package com.craftbid.service;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AdminOtpService {

    // Authorized admin emails that can access admin login
    private static final Set<String> AUTHORIZED_ADMIN_EMAILS = Set.of(
            "craftbid.official@gmail.com",
            "ranjitbarik146@gmail.com",
            "ranjitbarik.official@gmail.com",
            "rb650196@gmail.com",
            "ranjitbarik466@gmail.com"
    );

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

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
    // SEND OTP (DATABASE PERSISTENT)
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
            } else {
                throw new RuntimeException("Access denied. Account is not configured as administrator.");
            }
        }

        // Generate 6 digit OTP and persist in MySQL database
        String otp = String.format("%06d", random.nextInt(1_000_000));
        admin.setOtp(otp);
        admin.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(admin);

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
    // VERIFY OTP (DATABASE PERSISTENT)
    // ==========================================
    public LoginResponse verifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Admin email is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("Please enter the 6-digit OTP code");
        }

        String cleanEmail = email.trim().toLowerCase();

        // Find admin
        User admin = userRepository
                .findByEmail(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Administrator account not found for " + cleanEmail));

        // Check role and active
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            throw new RuntimeException("Access denied. Not an authorized administrator.");
        }

        // Check database-persisted OTP
        if (admin.getOtp() == null || admin.getOtpExpiry() == null) {
            throw new RuntimeException("Security code not found. Please click 'Send OTP Code'.");
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(admin.getOtpExpiry())) {
            admin.setOtp(null);
            admin.setOtpExpiry(null);
            userRepository.save(admin);
            throw new RuntimeException("Security code expired. Please request a new OTP.");
        }

        // Check OTP
        if (!admin.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Invalid security code. Please check your inbox and try again.");
        }

        // Clear used OTP in database
        admin.setOtp(null);
        admin.setOtpExpiry(null);
        userRepository.save(admin);

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
}