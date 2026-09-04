package com.craftbid.service;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminOtpService {

    // Primary authorized admin emails that can access admin login
    private static final Set<String> AUTHORIZED_ADMIN_EMAILS = Set.of(
            "craftbid.official@gmail.com",
            "ranjitbarik146@gmail.com",
            "rb650196@gmail.com",
            "ranjitbarik466@gmail.com"
    );

    // OTP validity = 5 minutes
    private static final long OTP_EXPIRY = 5 * 60 * 1000;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();

    public AdminOtpService(
            UserRepository userRepository,
            EmailService emailService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    private boolean isAuthorizedAdmin(String email) {
        if (email == null) return false;
        return AUTHORIZED_ADMIN_EMAILS.contains(email.trim().toLowerCase());
    }

    // ==========================================
    // SEND OTP
    // ==========================================
    public String sendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Administrator email is required");
        }

        String cleanEmail = email.trim().toLowerCase();

        // Find or auto-provision authorized administrator
        User admin = userRepository
                .findByEmail(cleanEmail)
                .orElseGet(() -> {
                    if (isAuthorizedAdmin(cleanEmail)) {
                        User newAdmin = new User();
                        newAdmin.setName("CraftBid Administrator");
                        newAdmin.setEmail(cleanEmail);
                        newAdmin.setPhone("9040408690");
                        newAdmin.setRole(Role.ADMIN);
                        newAdmin.setActive(true);
                        newAdmin.setSellerEnabled(true);
                        return userRepository.save(newAdmin);
                    }
                    throw new RuntimeException("Access denied. " + cleanEmail + " is not an authorized administrator.");
                });

        // Ensure role and active status
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            if (isAuthorizedAdmin(cleanEmail)) {
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin = userRepository.save(admin);
            } else {
                throw new RuntimeException("Access denied. Account is not configured as administrator.");
            }
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));
        long expiryTime = System.currentTimeMillis() + OTP_EXPIRY;

        // Store OTP keyed by clean email
        otpStore.put(cleanEmail, new OtpData(otp, expiryTime));

        System.out.println("======================================");
        System.out.println("⚡ CRAFTBID ADMIN LOGIN OTP GENERATED");
        System.out.println("Admin Email: " + cleanEmail);
        System.out.println("OTP Code:    " + otp);
        System.out.println("Expires in 5 minutes");
        System.out.println("======================================");

        try {
            emailService.sendAdminOtpEmail(cleanEmail, otp);
        } catch (Exception e) {
            System.err.println("Notice: Admin OTP email dispatch error: " + e.getMessage());
        }

        return otp;
    }

    // ==========================================
    // VERIFY OTP
    // ==========================================
    public LoginResponse verifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Admin email is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("Please enter the 6-digit OTP code");
        }

        String cleanEmail = email.trim().toLowerCase();

        // Find or auto-provision admin
        User admin = userRepository
                .findByEmail(cleanEmail)
                .orElseGet(() -> {
                    if (isAuthorizedAdmin(cleanEmail)) {
                        User newAdmin = new User();
                        newAdmin.setName("CraftBid Administrator");
                        newAdmin.setEmail(cleanEmail);
                        newAdmin.setPhone("9040408690");
                        newAdmin.setRole(Role.ADMIN);
                        newAdmin.setActive(true);
                        newAdmin.setSellerEnabled(true);
                        return userRepository.save(newAdmin);
                    }
                    throw new RuntimeException("Administrator account not found for " + cleanEmail);
                });

        // Check role and active
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            if (isAuthorizedAdmin(cleanEmail)) {
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin = userRepository.save(admin);
            } else {
                throw new RuntimeException("Access denied. Not an authorized administrator.");
            }
        }

        // Get stored OTP
        OtpData otpData = otpStore.get(cleanEmail);

        if (otpData == null) {
            throw new RuntimeException("OTP not found or expired. Please request a new security code.");
        }

        // Check expiry
        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpStore.remove(cleanEmail);
            throw new RuntimeException("Security code expired. Please request a new OTP.");
        }

        // Check OTP (accepts real generated OTP only)
        if (!otpData.otp.equals(otp.trim())) {
            throw new RuntimeException("Invalid security code. Please check your inbox and try again.");
        }

        // OTP used -> clear store
        otpStore.remove(cleanEmail);

        // Generate JWT
        String token = jwtService.generateToken(
                admin.getEmail(),
                admin.getRole().name()
        );

        return new LoginResponse(
                token,
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getRole().name(),
                admin.isSellerEnabled()
        );
    }

    // ==========================================
    // OTP DATA
    // ==========================================
    private static class OtpData {
        private final String otp;
        private final long expiryTime;

        public OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}