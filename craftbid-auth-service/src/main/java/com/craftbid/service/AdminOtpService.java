package com.craftbid.service;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminOtpService {

    // ONLY this email can access admin login
    public static final String ADMIN_EMAIL = "craftbid.official@gmail.com";

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

    // ==========================================
    // SEND OTP
    // ==========================================

    public String sendOtp(String email) {

        // Check fixed admin email
        if (!ADMIN_EMAIL.equalsIgnoreCase(email)) {
            throw new RuntimeException(
                    "Invalid admin email. Only the official CraftBid admin (" + ADMIN_EMAIL + ") can login."
            );
        }

        // Find admin from database (or auto-provision official admin if first time)
        User admin = userRepository
                .findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> {
                    User newAdmin = new User();
                    newAdmin.setName("CraftBid Official Admin");
                    newAdmin.setEmail(ADMIN_EMAIL);
                    newAdmin.setPhone("9040408690");
                    newAdmin.setRole(Role.ADMIN);
                    newAdmin.setActive(true);
                    newAdmin.setSellerEnabled(true);
                    return userRepository.save(newAdmin);
                });

        // Ensure role and active status
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin = userRepository.save(admin);
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));

        // Expire after 5 minutes
        long expiryTime = System.currentTimeMillis() + OTP_EXPIRY;

        // Store OTP
        otpStore.put(ADMIN_EMAIL, new OtpData(otp, expiryTime));

        System.out.println("======================================");
        System.out.println("⚡ CRAFTBID ADMIN LOGIN OTP GENERATED");
        System.out.println("Admin Email: " + ADMIN_EMAIL);
        System.out.println("OTP Code:    " + otp + " (or test OTP: 123456)");
        System.out.println("Expires in 5 minutes");
        System.out.println("======================================");

        try {
            emailService.sendAdminOtpEmail(ADMIN_EMAIL, otp);
        } catch (Exception e) {
            System.err.println("Notice: Could not deliver admin OTP email via SMTP (" + e.getMessage() + "). Use logged OTP or 123456.");
        }

        return otp;
    }

    // ==========================================
    // VERIFY OTP
    // ==========================================

    public LoginResponse verifyOtp(String email, String otp) {

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("Please enter the 6-digit OTP");
        }

        // Only official email
        if (!ADMIN_EMAIL.equalsIgnoreCase(email)) {
            throw new RuntimeException(
                    "Invalid admin email. Only " + ADMIN_EMAIL + " is authorized."
            );
        }

        // Find admin (or auto-provision)
        User admin = userRepository
                .findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> {
                    User newAdmin = new User();
                    newAdmin.setName("CraftBid Official Admin");
                    newAdmin.setEmail(ADMIN_EMAIL);
                    newAdmin.setPhone("9040408690");
                    newAdmin.setRole(Role.ADMIN);
                    newAdmin.setActive(true);
                    newAdmin.setSellerEnabled(true);
                    return userRepository.save(newAdmin);
                });

        // Check role and active
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin = userRepository.save(admin);
        }

        // Get stored OTP
        OtpData otpData = otpStore.get(ADMIN_EMAIL);

        if (otpData == null) {
            throw new RuntimeException("OTP not found or expired. Please request a new OTP.");
        } else {
            // Check expiry
            if (System.currentTimeMillis() > otpData.expiryTime) {
                otpStore.remove(ADMIN_EMAIL);
                throw new RuntimeException("OTP expired. Please request a new OTP");
            }

            // Check OTP (accepts generated real OTP only)
            if (!otpData.otp.equals(otp.trim())) {
                throw new RuntimeException("Invalid OTP code. Please check and try again.");
            }
        }

        // OTP used -> clear store
        otpStore.remove(ADMIN_EMAIL);

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

        public OtpData(
                String otp,
                long expiryTime) {

            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}