package com.craftbid.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Value("${craftbid.mail.from:craftbid.official@gmail.com}")
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
    // CORE EMAIL DISPATCHER (HTML MIME + REST API FALLBACK)
    // =====================================================
    private void dispatchEmail(String toEmail, String subject, String htmlContent) {
        CompletableFuture.runAsync(() -> {
            boolean sent = false;

            // 1. Send via Google SMTP (JavaMailSender with HTML MimeMessage)
            if (mailSender != null) {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                    helper.setFrom(fromEmail, "CraftBid Official");
                    helper.setTo(toEmail);
                    helper.setSubject(subject);
                    helper.setText(htmlContent, true);
                    mailSender.send(mimeMessage);
                    System.out.println("✅ Email delivered successfully via Google SMTP to " + toEmail);
                    sent = true;
                } catch (Exception e) {
                    System.err.println("⚠️ SMTP failed: " + e.getMessage() + ". Trying REST fallback...");
                }
            }

            // 2. Fallback to Brevo REST API if API Key is configured
            if (!sent && brevoApiKey != null && !brevoApiKey.isBlank()) {
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
                    } else {
                        System.err.println("❌ Brevo REST API returned " + response.statusCode() + ": " + response.body());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Brevo REST API error: " + e.getMessage());
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
        String subject = "CraftBid Email Verification OTP: " + otp;
        String html = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'><span style='font-size: 32px;'>🏺</span><h2 style='color: #ea580c; margin: 4px 0;'>CraftBid Verification</h2></div>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>Thank you for joining CraftBid. Your 6-digit account verification code is:</p>"
                + "<div style='text-align: center; margin: 24px 0;'><span style='display: inline-block; background: #fff7ed; border: 2px dashed #f97316; color: #c2410c; font-size: 32px; font-weight: bold; letter-spacing: 6px; padding: 12px 28px; border-radius: 8px;'>" + otp + "</span></div>"
                + "<p style='color: #71717a; font-size: 12px; text-align: center;'>This code is valid for 5 minutes. Please do not share it with anyone.</p>"
                + "<hr style='border: none; border-top: 1px solid #e8e2d9; margin: 20px 0;'/>"
                + "<p style='color: #a1a1aa; font-size: 11px; text-align: center;'>CraftBid Platform © 2026</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // REGISTRATION SUCCESS EMAIL
    // =====================================================
    public void sendRegistrationSuccessEmail(String email, String name, String loginId, String password) {
        String subject = "🎉 Welcome to CraftBid - Account Created Successfully!";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<h2 style='color: #10b981;'>🎉 Welcome to CraftBid!</h2>"
                + "<p>Hello <strong>" + name + "</strong>,</p>"
                + "<p>Your account is active. Login ID: <strong>" + loginId + "</strong></p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // ADMIN OTP EMAIL
    // =====================================================
    public void sendAdminOtpEmail(String email, String otp) {
        String subject = "🛡️ CraftBid Admin Login Security Code: " + otp;
        String html = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: auto; padding: 24px; background: #18181b; color: #fff; border-radius: 12px;'>"
                + "<h2 style='color: #f97316;'>🛡️ Admin Console Login</h2>"
                + "<p>Your admin security code is:</p>"
                + "<div style='font-size: 32px; font-weight: bold; color: #f97316; letter-spacing: 6px; text-align: center; padding: 16px; background: #27272a; border-radius: 8px;'>" + otp + "</div>"
                + "<p style='font-size: 12px; color: #a1a1aa;'>Valid for 5 minutes.</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // FORGOT PASSWORD OTP
    // =====================================================
    public void sendForgotPasswordOtpEmail(String email, String otp) {
        String subject = "CraftBid Password Reset Code: " + otp;
        String html = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'><span style='font-size: 32px;'>🔐</span><h2 style='color: #ea580c; margin: 4px 0;'>Reset Your Password</h2></div>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>We received a request to reset your CraftBid account password. Your verification code is:</p>"
                + "<div style='text-align: center; margin: 24px 0;'><span style='display: inline-block; background: #fff7ed; border: 2px dashed #f97316; color: #c2410c; font-size: 32px; font-weight: bold; letter-spacing: 6px; padding: 12px 28px; border-radius: 8px;'>" + otp + "</span></div>"
                + "<p style='color: #71717a; font-size: 12px; text-align: center;'>This code is valid for 5 minutes. If you did not request this, please ignore this email.</p>"
                + "<hr style='border: none; border-top: 1px solid #e8e2d9; margin: 20px 0;'/>"
                + "<p style='color: #a1a1aa; font-size: 11px; text-align: center;'>CraftBid Platform © 2026</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // PASSWORD RESET SUCCESS
    // =====================================================
    public void sendPasswordResetSuccessEmail(String email, String name) {
        String subject = "CraftBid Password Reset Confirmation";
        String html = "<p>Hello " + name + ",</p><p>Your password has been reset successfully.</p>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // SELLER ENABLED EMAIL
    // =====================================================
    public void sendSellerEnabledEmail(String email, String name) {
        String subject = "🎨 Welcome to CraftBid Artisan Studio!";
        String html = "<p>Hello " + name + ",</p><p>Your Artisan Studio profile has been activated.</p>";
        dispatchEmail(email, subject, html);
    }
}