package com.craftbid.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
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
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Value("${craftbid.mail.from:${SPRING_MAIL_FROM:craftbid.official@gmail.com}}")
    private String fromEmail;

    @Value("${spring.mail.username:craftbid.official@gmail.com}")
    private String mailUsername;

    @Value("${spring.mail.password:toyekvrmhrmunicr}")
    private String mailPassword;

    @Value("${craftbid.brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${craftbid.resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${craftbid.email.webhook-url:${CRAFTBID_EMAIL_WEBHOOK_URL:}}")
    private String webhookUrl;

    private final JavaMailSender mailSender;
    private final HttpClient httpClient;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    // =====================================================
    // CORE EMAIL DISPATCHER (NON-BLOCKING ASYNC)
    // =====================================================
    private void dispatchEmail(String toEmail, String subject, String htmlContent) {
        CompletableFuture.runAsync(() -> {
            boolean sent = false;

            String whUrl = getEffectiveWebhookUrl();
            String brevoKey = getFormattedBrevoKey();
            String resendKey = getEffectiveResendKey();

            // 1. If Webhook URL is configured (HTTPS Port 443)
            if (!whUrl.isBlank()) {
                sent = trySendViaWebhook(toEmail, subject, htmlContent);
            }

            // 2. If Brevo REST API key is configured, use it (HTTPS Port 443)
            if (!sent && !brevoKey.isBlank()) {
                sent = trySendViaBrevoRest(toEmail, subject, htmlContent);
            }

            // 3. If Resend REST API key is configured (HTTPS Port 443)
            if (!sent && !resendKey.isBlank()) {
                sent = trySendViaResendRest(toEmail, subject, htmlContent);
            }

            // 4. Try Google SMTP via JavaMailSender (Port 587)
            if (!sent) {
                sent = trySendViaGoogleSmtp(toEmail, subject, htmlContent);
            }

            // 5. Try Direct SMTPS (Port 465 SSL)
            if (!sent) {
                trySendViaDirectSmtps(toEmail, subject, htmlContent);
            }
        });
    }

    // =====================================================
    // SYNCHRONOUS DIAGNOSTIC EMAIL TEST
    // =====================================================
    public String sendDiagnosticTestEmail(String toEmail) {
        long start = System.currentTimeMillis();
        String subject = "CraftBid Diagnostic Email Test - " + System.currentTimeMillis();
        String html = "<div style='font-family:sans-serif;padding:20px;background:#f0fdf4;border:1px solid #86efac;border-radius:8px;'>"
                + "<h2 style='color:#15803d;'>✅ CraftBid Email Dispatcher is Operational!</h2>"
                + "<p>This is a test email sent to verify active delivery from the CraftBid cloud infrastructure.</p>"
                + "<p><strong>Timestamp:</strong> " + java.time.LocalDateTime.now() + "</p>"
                + "</div>";

        String whUrl = getEffectiveWebhookUrl();
        String brevoKey = getFormattedBrevoKey();
        String resendKey = getEffectiveResendKey();

        StringBuilder log = new StringBuilder();
        log.append("Attempting email dispatch to: ").append(toEmail).append("\n");
        log.append("Configured WEBHOOK_URL: ").append(!whUrl.isBlank() ? "YES (" + whUrl.substring(0, Math.min(30, whUrl.length())) + "...)" : "NO").append("\n");
        log.append("Configured BREVO_API_KEY: ").append(!brevoKey.isBlank() ? "YES (" + brevoKey.substring(0, Math.min(16, brevoKey.length())) + "...)" : "NO (Empty)").append("\n");
        log.append("Configured RESEND_API_KEY: ").append(!resendKey.isBlank() ? "YES" : "NO").append("\n");
        log.append("Configured Sender FROM: ").append(fromEmail).append("\n");

        boolean sent = false;

        // 1. Try Webhook if configured
        if (!whUrl.isBlank()) {
            try {
                String jsonBody = String.format(
                        "{\"to\":\"%s\",\"subject\":\"%s\",\"html\":\"%s\"}",
                        escapeJson(toEmail),
                        escapeJson(subject),
                        escapeJson(html)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(whUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(6))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.append("✅ Google Webhook (HTTPS Port 443) succeeded in ").append(System.currentTimeMillis() - start).append("ms! Response: ").append(response.body()).append("\n");
                    sent = true;
                } else {
                    log.append("⚠️ Google Webhook returned ").append(response.statusCode()).append(": ").append(response.body()).append("\n");
                }
            } catch (Exception e) {
                log.append("⚠️ Google Webhook exception: ").append(e.getMessage()).append("\n");
            }
        }

        // 1. Try Brevo REST if key present
        if (!brevoKey.isBlank()) {
            try {
                String jsonBody = String.format(
                        "{\"sender\":{\"name\":\"CraftBid\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                        escapeJson(fromEmail),
                        escapeJson(toEmail),
                        escapeJson(subject),
                        escapeJson(html)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("api-key", brevoKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(6))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.append("✅ Brevo REST API (HTTPS Port 443) succeeded in ").append(System.currentTimeMillis() - start).append("ms! Response: ").append(response.body()).append("\n");
                    sent = true;
                } else {
                    log.append("⚠️ Brevo REST API returned ").append(response.statusCode()).append(": ").append(response.body()).append("\n");
                }
            } catch (Exception e) {
                log.append("⚠️ Brevo REST API exception: ").append(e.getMessage()).append("\n");
            }
        }

        // 2. Try Resend REST
        if (!sent && !resendKey.isBlank()) {
            try {
                String jsonBody = String.format(
                        "{\"from\":\"CraftBid <onboarding@resend.dev>\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                        escapeJson(toEmail),
                        escapeJson(subject),
                        escapeJson(html)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.resend.com/emails"))
                        .header("Authorization", "Bearer " + resendKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(6))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.append("✅ Resend REST API (HTTPS Port 443) succeeded in ").append(System.currentTimeMillis() - start).append("ms! Response: ").append(response.body()).append("\n");
                    sent = true;
                } else {
                    log.append("⚠️ Resend REST API returned ").append(response.statusCode()).append(": ").append(response.body()).append("\n");
                }
            } catch (Exception e) {
                log.append("⚠️ Resend REST API exception: ").append(e.getMessage()).append("\n");
            }
        }

        if (!sent) {
            try {
                if (mailSender != null) {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
                    helper.setFrom(fromEmail, "CraftBid Official");
                    helper.setTo(toEmail);
                    helper.setSubject(subject);
                    helper.setText(html, true);
                    mailSender.send(mimeMessage);
                    log.append("✅ Google SMTP (JavaMailSender) succeeded in ").append(System.currentTimeMillis() - start).append("ms\n");
                    sent = true;
                }
            } catch (Exception e) {
                log.append("⚠️ JavaMailSender failed: ").append(e.getMessage()).append("\n");
            }
        }

        if (!sent) {
            try {
                Properties prop = new Properties();
                prop.put("mail.smtp.host", "smtp.gmail.com");
                prop.put("mail.smtp.port", "465");
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.socketFactory.port", "465");
                prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                prop.put("mail.smtp.ssl.enable", "true");
                prop.put("mail.smtp.ssl.trust", "*");
                prop.put("mail.smtp.connectiontimeout", "5000");
                prop.put("mail.smtp.timeout", "5000");

                Session session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mailUsername, mailPassword);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail, "CraftBid Official"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setContent(html, "text/html; charset=utf-8");

                Transport.send(message);
                log.append("✅ Direct SMTPS (Port 465 SSL) succeeded in ").append(System.currentTimeMillis() - start).append("ms\n");
                sent = true;
            } catch (Exception e) {
                log.append("⚠️ Direct SMTPS failed: ").append(e.getMessage()).append("\n");
            }
        }

        return log.toString();
    }

    private boolean trySendViaGoogleSmtp(String toEmail, String subject, String htmlContent) {
        if (mailSender == null) return false;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail, "CraftBid Official");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            System.out.println("✅ Email delivered successfully via Google SMTP to " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Google SMTP failed for " + toEmail + ": " + e.getMessage() + ". Trying SMTPS fallback...");
            return false;
        }
    }

    private boolean trySendViaDirectSmtps(String toEmail, String subject, String htmlContent) {
        try {
            Properties prop = new Properties();
            prop.put("mail.smtp.host", "smtp.gmail.com");
            prop.put("mail.smtp.port", "465");
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.socketFactory.port", "465");
            prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            prop.put("mail.smtp.ssl.enable", "true");
            prop.put("mail.smtp.ssl.trust", "*");
            prop.put("mail.smtp.connectiontimeout", "5000");
            prop.put("mail.smtp.timeout", "5000");

            Session session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "CraftBid Official"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email delivered via SMTPS Port 465 to " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Direct SMTPS failed for " + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    private String getEffectiveWebhookUrl() {
        String url = webhookUrl;
        if (url == null || url.isBlank()) {
            url = System.getenv("CRAFTBID_EMAIL_WEBHOOK_URL");
        }
        return url != null ? url.trim() : "";
    }

    private String getFormattedBrevoKey() {
        String key = brevoApiKey;
        if (key == null || key.isBlank()) {
            key = System.getenv("BREVO_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("CRAFTBID_BREVO_API_KEY");
        }
        if (key == null || key.isBlank()) {
            return "";
        }
        String clean = key.trim();
        if (!clean.startsWith("xkeysib-") && !clean.isBlank()) {
            return "xkeysib-" + clean;
        }
        return clean;
    }

    private String getEffectiveResendKey() {
        String key = resendApiKey;
        if (key == null || key.isBlank()) {
            key = System.getenv("RESEND_API_KEY");
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("CRAFTBID_RESEND_API_KEY");
        }
        return key != null ? key.trim() : "";
    }

    private boolean trySendViaWebhook(String toEmail, String subject, String htmlContent) {
        String url = getEffectiveWebhookUrl();
        if (url.isBlank()) return false;
        try {
            String jsonBody = String.format(
                    "{\"to\":\"%s\",\"subject\":\"%s\",\"html\":\"%s\"}",
                    escapeJson(toEmail),
                    escapeJson(subject),
                    escapeJson(htmlContent)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Email delivered via Webhook to " + toEmail);
                return true;
            } else {
                System.err.println("❌ Webhook returned " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Webhook error: " + e.getMessage());
            return false;
        }
    }

    private boolean trySendViaBrevoRest(String toEmail, String subject, String htmlContent) {
        try {
            String cleanKey = getFormattedBrevoKey();
            String jsonBody = String.format(
                    "{\"sender\":{\"name\":\"CraftBid\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                    escapeJson(fromEmail),
                    escapeJson(toEmail),
                    escapeJson(subject),
                    escapeJson(htmlContent)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", cleanKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Email delivered via Brevo REST API to " + toEmail);
                return true;
            } else {
                System.err.println("❌ Brevo REST API returned " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Brevo REST API error: " + e.getMessage());
            return false;
        }
    }

    private boolean trySendViaResendRest(String toEmail, String subject, String htmlContent) {
        try {
            String jsonBody = String.format(
                    "{\"from\":\"CraftBid <onboarding@resend.dev>\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    escapeJson(toEmail),
                    escapeJson(subject),
                    escapeJson(htmlContent)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Email delivered via Resend REST API to " + toEmail);
                return true;
            } else {
                System.err.println("❌ Resend REST API returned " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Resend REST API error: " + e.getMessage());
            return false;
        }
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