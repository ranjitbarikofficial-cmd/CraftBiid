package com.craftbid.service;

import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // SEND RESET OTP (DATABASE PERSISTENT)
    // =====================================================
    public String sendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        final String cleanEmail = email.trim().toLowerCase();

        // User must already exist
        User user = userRepository
                .findByEmail(cleanEmail)
                .orElseThrow(() ->
                        new RuntimeException("No account found with this email: " + cleanEmail)
                );

        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive. Please complete account verification.");
        }

        // Generate 6 digit OTP and persist in database
        String otp = String.format("%06d", random.nextInt(1_000_000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        System.out.println("======================================");
        System.out.println("⚡ CRAFTBID PASSWORD RESET OTP GENERATED");
        System.out.println("User Email:  " + cleanEmail);
        System.out.println("OTP Code:    " + otp);
        System.out.println("Expires in 5 minutes");
        System.out.println("======================================");

        // Send OTP email
        try {
            emailService.sendForgotPasswordOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            System.err.println("Notice: Forgot password email dispatch error: " + e.getMessage());
        }

        return "Password reset OTP sent successfully to " + cleanEmail;
    }

    // =====================================================
    // RESET PASSWORD (DATABASE PERSISTENT)
    // =====================================================
    public String resetPassword(
            String email,
            String otp,
            String newPassword) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP is required");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password is required");
        }

        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must contain at least 6 characters");
        }

        final String cleanEmail = email.trim().toLowerCase();

        // Find user
        User user = userRepository
                .findByEmail(cleanEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found for " + cleanEmail)
                );

        // Check database-persisted OTP
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new RuntimeException("OTP not found. Please click 'Send OTP' to request a new code.");
        }

        // Check OTP expiry
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("OTP expired. Please request a new verification code.");
        }

        // Check OTP (accepts generated real OTP only)
        if (!user.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Invalid OTP code. Please enter the 6-digit code sent to your email.");
        }

        // Encrypt new password & clear OTP
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Send password reset success email
        try {
            emailService.sendPasswordResetSuccessEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            System.err.println("Notice: Password reset confirmation email error: " + e.getMessage());
        }

        return "Password reset successfully. You can now login with your new password.";
    }
}