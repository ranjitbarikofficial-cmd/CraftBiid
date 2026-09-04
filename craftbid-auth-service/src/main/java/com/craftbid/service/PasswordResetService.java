package com.craftbid.service;

import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom random =
            new SecureRandom();

    private final Map<String, ResetOtpData> otpStore =
            new ConcurrentHashMap<>();

    public PasswordResetService(
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // SEND RESET OTP
    // =====================================================

    public String sendOtp(String email) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email is required"
            );
        }

        email = email.trim().toLowerCase();

        // User must already exist
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No account found with this email"
                        )
                );

        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is inactive"
            );
        }

        // Generate 6 digit OTP
        String otp = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );

        // OTP expires after 5 minutes
        LocalDateTime expiry =
                LocalDateTime.now()
                        .plusMinutes(5);

        // Store OTP
        otpStore.put(
                email,
                new ResetOtpData(
                        otp,
                        expiry
                )
        );

        // Send OTP email
        emailService.sendForgotPasswordOtpEmail(
                email,
                otp
        );

        return "Password reset OTP sent successfully";
    }

    // =====================================================
    // RESET PASSWORD
    // =====================================================

    public String resetPassword(
            String email,
            String otp,
            String newPassword) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email is required"
            );
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException(
                    "OTP is required"
            );
        }

        if (newPassword == null ||
                newPassword.isBlank()) {

            throw new RuntimeException(
                    "New password is required"
            );
        }

        if (newPassword.length() < 6) {
            throw new RuntimeException(
                    "Password must contain at least 6 characters"
            );
        }

        email = email.trim().toLowerCase();

        // Get stored OTP
        ResetOtpData otpData =
                otpStore.get(email);

        if (otpData == null) {
            throw new RuntimeException(
                    "OTP not found. Please request a new OTP"
            );
        }

        // Check OTP expiry
        if (LocalDateTime.now()
                .isAfter(otpData.expiry)) {

            otpStore.remove(email);

            throw new RuntimeException(
                    "OTP expired. Please request a new OTP"
            );
        }

        // Check OTP (accepts generated real OTP only)
        if (!otpData.otp.equals(otp.trim())) {
            throw new RuntimeException("Invalid OTP code. Please enter the code sent to your email.");
        }

        // Find user
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        // Encrypt new password
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        // Save new password
        userRepository.save(user);

        // Send password reset success email
        emailService.sendPasswordResetSuccessEmail(
                user.getEmail(),
                user.getName()
        );

        // OTP can only be used once
        otpStore.remove(email);

        return "Password reset successfully. You can now login with your new password.";
    }

    // =====================================================
    // OTP DATA
    // =====================================================

    private static class ResetOtpData {

        private final String otp;
        private final LocalDateTime expiry;

        public ResetOtpData(
                String otp,
                LocalDateTime expiry) {

            this.otp = otp;
            this.expiry = expiry;
        }
    }
}