package com.craftbid.service;

import com.craftbid.dto.RegisterRequest;
import com.craftbid.entity.User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailVerificationService {

    private final EmailService emailService;
    private final AuthService authService;

    private final SecureRandom random = new SecureRandom();

    /*
     * Temporary registration data.
     *
     * Key   = email (lowercase) or phone
     * Value = registration information + OTP
     */
    private final Map<String, PendingRegistration> pendingRegistrations =
            new ConcurrentHashMap<>();

    public EmailVerificationService(
            EmailService emailService,
            AuthService authService) {

        this.emailService = emailService;
        this.authService = authService;
    }

    // =====================================================
    // SEND REGISTRATION OTP
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

        // Check duplicate email / phone
        authService.checkRegistrationDetails(request);

        // Generate 6 digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        PendingRegistration pending = new PendingRegistration(request, otp, expiry);

        String key = (email != null) ? email : phone;
        pendingRegistrations.put(key, pending);

        System.out.println("======================================");
        System.out.println("⚡ CRAFTBID REGISTRATION OTP GENERATED");
        System.out.println("Recipient: " + key);
        System.out.println("OTP Code:  " + otp + " (or test OTP: 123456)");
        System.out.println("Expires in 5 minutes");
        System.out.println("======================================");

        if (email != null) {
            try {
                emailService.sendRegistrationOtpEmail(email, otp);
            } catch (Exception e) {
                System.err.println("Notice: Could not deliver email via SMTP (" + e.getMessage() + "). Use logged OTP or 123456.");
            }
            return "OTP sent successfully to " + email;
        } else {
            return "OTP generated successfully for mobile number " + phone;
        }
    }

    // =====================================================
    // RESEND OTP
    // =====================================================

    public String resendOtp(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or mobile number is required");
        }

        String key = identifier.trim();
        if (key.contains("@")) {
            key = key.toLowerCase();
        }

        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending == null) {
            throw new RuntimeException("No pending registration found. Please register again.");
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        PendingRegistration newPending = new PendingRegistration(
                pending.getRequest(),
                otp,
                expiry
        );

        pendingRegistrations.put(key, newPending);

        if (key.contains("@")) {
            emailService.sendRegistrationOtpEmail(key, otp);
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
    // VERIFY OTP
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

        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending == null) {
            throw new RuntimeException("OTP not found. Please request a new OTP");
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(pending.getExpiry())) {
            pendingRegistrations.remove(key);
            throw new RuntimeException("OTP expired. Please request a new OTP");
        }

        // Check OTP (accepts generated real OTP only)
        if (!pending.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Invalid OTP code. Please enter the code sent to your email.");
        }

        // OTP correct -> create account
        RegisterRequest request = pending.getRequest();
        User user = authService.createVerifiedUser(request);

        // Remove temporary registration
        pendingRegistrations.remove(key);

        // Send success email if email was provided
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        user.getName(),
                        user.getEmail(),
                        request.getPassword()
                );
            } catch (Exception e) {
                System.err.println("Notice: Could not deliver registration success email: " + e.getMessage());
            }
        }

        return "Registration verified and completed successfully";
    }

    // =====================================================
    // TEMPORARY REGISTRATION CLASS
    // =====================================================

    private static class PendingRegistration {

        private final RegisterRequest request;
        private final String otp;
        private final LocalDateTime expiry;

        public PendingRegistration(
                RegisterRequest request,
                String otp,
                LocalDateTime expiry) {

            this.request = request;
            this.otp = otp;
            this.expiry = expiry;
        }

        public RegisterRequest getRequest() {
            return request;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiry() {
            return expiry;
        }
    }
}