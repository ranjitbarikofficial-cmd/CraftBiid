package com.craftbid.service;

import com.craftbid.dto.RegisterRequest;
import com.craftbid.entity.User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailVerificationService {

    private final EmailService emailService;
    private final AuthService authService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailService emailService,
            AuthService authService) {

        this.emailService = emailService;
        this.authService = authService;
    }

    // =====================================================
    // SEND REGISTRATION OTP (DATABASE PERSISTENT)
    // =====================================================
    public String sendOtp(RegisterRequest request) {

        String email = request.getEmail();
        String phone = request.getPhone();

        // Normalize
        if (email != null && !email.isBlank()) {
            email = email.trim().toLowerCase();
            request.setEmail(email);
        } else {
            request.setEmail(null);
            email = null;
        }

        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();
            request.setPhone(phone);
        } else {
            request.setPhone(null);
            phone = null;
        }

        if (email == null && phone == null) {
            throw new RuntimeException("Email or mobile number is required");
        }

        // Check active duplicates
        authService.checkRegistrationDetails(request);

        // Generate 6 digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Persist pending registration in database
        authService.savePendingUser(request, otp, expiry);

        String recipient = (email != null) ? email : phone;
        System.out.println("======================================");
        System.out.println("⚡ CRAFTBID REGISTRATION OTP GENERATED");
        System.out.println("Recipient: " + recipient);
        System.out.println("OTP Code:  " + otp);
        System.out.println("Expires in 5 minutes");
        System.out.println("======================================");

        if (email != null) {
            try {
                emailService.sendRegistrationOtpEmail(email, otp);
            } catch (Exception e) {
                System.err.println("Notice: Could not deliver registration OTP email: " + e.getMessage());
            }
            return "OTP sent successfully to " + email;
        } else {
            return "OTP generated successfully for mobile number " + phone;
        }
    }

    // =====================================================
    // RESEND OTP (DATABASE PERSISTENT)
    // =====================================================
    public String resendOtp(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or mobile number is required");
        }

        String key = identifier.trim();
        if (key.contains("@")) {
            key = key.toLowerCase();
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        RegisterRequest req = new RegisterRequest();
        if (key.contains("@")) {
            req.setEmail(key);
            req.setName("CraftBid Member");
            req.setPassword("craftbid123");
        } else {
            req.setPhone(key);
            req.setName("CraftBid Member");
            req.setPassword("craftbid123");
        }

        authService.savePendingUser(req, otp, expiry);

        if (key.contains("@")) {
            try {
                emailService.sendRegistrationOtpEmail(key, otp);
            } catch (Exception e) {
                System.err.println("Notice: Could not deliver resend OTP email: " + e.getMessage());
            }
            return "New OTP sent successfully to " + key;
        } else {
            System.out.println("======================================");
            System.out.println("CRAFTBID RESEND MOBILE OTP");
            System.out.println("Mobile: " + key);
            System.out.println("OTP: " + otp);
            System.out.println("======================================");
            return "New OTP generated successfully for " + key;
        }
    }

    // =====================================================
    // VERIFY OTP (DATABASE PERSISTENT)
    // =====================================================
    public String verifyOtp(String identifier, String otp) {

        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or mobile number is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP is required");
        }

        String key = identifier.trim();
        if (key.contains("@")) {
            key = key.toLowerCase();
        }

        // Verify user in MySQL
        User user = authService.verifyPendingUser(key, otp);

        // Send welcome email if email is provided
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        user.getName(),
                        user.getEmail(),
                        "********"
                );
            } catch (Exception e) {
                System.err.println("Notice: Registration success email error: " + e.getMessage());
            }
        }

        return "Registration verified and completed successfully";
    }
}