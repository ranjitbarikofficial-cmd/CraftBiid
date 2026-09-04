package com.craftbid.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Value("${craftbid.mail.from:${spring.mail.username:ranjitbarik146@gmail.com}}")
    private String fromEmail;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    private final JavaMailSender mailSender;
    private final HttpClient httpClient;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // =====================================================
    // CORE EMAIL DISPATCHER (HTTP REST API + SMTP FALLBACK)
    // =====================================================
    private void dispatchEmail(String toEmail, String subject, String bodyText, String htmlContent) {
        CompletableFuture.runAsync(() -> {
            boolean sentViaRest = false;

            // 1. Try Brevo HTTPS REST API if API key exists (Fastest, bypasses SMTP port blocking)
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                try {
                    String jsonBody = String.format(
                            "{\"sender\":{\"name\":\"CraftBid\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                            escapeJson(fromEmail),
                            escapeJson(toEmail),
                            escapeJson(subject),
                            escapeJson(htmlContent)
                    );

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                            .header("api-key", brevoApiKey.trim())
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .timeout(Duration.ofSeconds(6))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        System.out.println("✅ Email delivered via Brevo REST API to " + toEmail);
                        sentViaRest = true;
                    } else {
                        System.err.println("⚠️ Brevo REST API responded with status " + response.statusCode() + ": " + response.body());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Brevo REST API failed (" + e.getMessage() + "), falling back to SMTP...");
                }
            }

            // 2. Fallback to Spring JavaMailSender (SMTP)
            if (!sentViaRest && mailSender != null) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromEmail);
                    message.setTo(toEmail);
                    message.setSubject(subject);
                    message.setText(bodyText);
                    mailSender.send(message);
                    System.out.println("✅ Email delivered via SMTP to " + toEmail);
                } catch (Exception e) {
                    System.err.println("❌ SMTP delivery failed for " + toEmail + ": " + e.getMessage());
                }
            }
        });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "<br/>")
                    .replace("\r", "");
    }

    // =====================================================
    // REGISTRATION OTP
    // =====================================================
    public void sendRegistrationOtpEmail(String email, String otp) {
        String subject = "CraftBid Verification Code: " + otp;
        String body = "Hello,\n\nWelcome to CraftBid!\n\nYour verification code is: " + otp + "\n\nThis code is valid for 5 minutes.\n\nRegards,\nCraftBid Team";
        String html = "<div style='font-family:sans-serif;padding:20px;color:#18181b;'><h2 style='color:#ea580c;'>Welcome to CraftBid!</h2><p>Your verification code is:</p><div style='font-size:28px;font-weight:bold;letter-spacing:4px;color:#c2410c;padding:12px;background:#fff7ed;border:1px solid #fdba74;border-radius:8px;display:inline-block;'>" + otp + "</div><p>Valid for 5 minutes. If you did not request this, please ignore.</p></div>";
        dispatchEmail(email, subject, body, html);
    }

    // =====================================================
    // REGISTRATION SUCCESS EMAIL
    // =====================================================
    public void sendRegistrationSuccessEmail(String email, String name, String loginId, String password) {
        String subject = "🎉 Welcome to CraftBid - Account Created Successfully!";
        String body = "Hello " + name + ",\n\nYour CraftBid account is now active.\nLogin: " + loginId + "\nPassword: " + password + "\n\nRegards,\nCraftBid Team";
        String html = "<div style='font-family:sans-serif;padding:20px;'><h2 style='color:#10b981;'>Registration Successful!</h2><p>Hello " + name + ",</p><p>Your account is ready. Login: <strong>" + loginId + "</strong></p></div>";
        dispatchEmail(email, subject, body, html);
    }

    // =====================================================
    // ADMIN OTP EMAIL
    // =====================================================
    public void sendAdminOtpEmail(String email, String otp) {
        String subject = "🛡️ CraftBid Admin Security Code: " + otp;
        String body = "Admin Security Code: " + otp + " (Valid for 5 minutes).";
        String html = "<div><h2>Admin Security Code</h2><p style='font-size:24px;color:#ea580c;font-weight:bold;'>" + otp + "</p></div>";
        dispatchEmail(email, subject, body, html);
    }

    // =====================================================
    // FORGOT PASSWORD OTP
    // =====================================================
    public void sendForgotPasswordOtpEmail(String email, String otp) {
        String subject = "CraftBid Password Reset Code: " + otp;
        String body = "Hello,\n\nYour password reset code is: " + otp + "\n\nValid for 5 minutes.\n\nRegards,\nCraftBid Team";
        String html = "<div style='font-family:sans-serif;padding:20px;'><h2 style='color:#ea580c;'>Reset Your Password</h2><p>Your password reset code is:</p><div style='font-size:28px;font-weight:bold;letter-spacing:4px;color:#c2410c;padding:12px;background:#fff7ed;border:1px solid #fdba74;border-radius:8px;display:inline-block;'>" + otp + "</div><p>Valid for 5 minutes.</p></div>";
        dispatchEmail(email, subject, body, html);
    }

    // =====================================================
    // PASSWORD RESET SUCCESS
    // =====================================================
    public void sendPasswordResetSuccessEmail(String email, String name) {
        String subject = "CraftBid Password Reset Confirmation";
        String body = "Hello " + name + ",\n\nYour password has been reset successfully.\n\nRegards,\nCraftBid Team";
        String html = "<p>Your CraftBid password has been updated successfully.</p>";
        dispatchEmail(email, subject, body, html);
    }

    // =====================================================
    // SELLER ENABLED EMAIL
    // =====================================================
    public void sendSellerEnabledEmail(String email, String name) {
        String subject = "🎨 Welcome to CraftBid Artisan Studio!";
        String body = "Hello " + name + ",\n\nYour Artisan Studio profile has been activated.\n\nRegards,\nCraftBid Team";
        String html = "<p>Your Artisan Studio profile is now active on CraftBid.</p>";
        dispatchEmail(email, subject, body, html);
    }
}