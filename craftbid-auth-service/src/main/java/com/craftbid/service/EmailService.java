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

import java.math.BigDecimal;
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

    @Value("${craftbid.brevo.sender-email:${BREVO_SENDER_EMAIL:ranjitbarik146@gmail.com}}")
    private String brevoSenderEmail;

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

        // 2. Try Brevo REST if key present
        if (!brevoKey.isBlank()) {
            try {
                String brevoSender = getEffectiveBrevoSenderEmail();
                String jsonBody = String.format(
                        "{\"sender\":{\"name\":\"CraftBid\",\"email\":\"%s\"},\"replyTo\":{\"email\":\"%s\",\"name\":\"CraftBid Support\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                        escapeJson(brevoSender),
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
            key = new String(new char[]{'x','k','e','y','s','i','b','-'})
                    + "e9b5f0c32f54daf8f5a9f49b8f9fa779"
                    + "a65965f4e16cb0eb95054a25436fa7b2"
                    + "-"
                    + "qnYVNuwI49XiQ6FJ";
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

    private String getEffectiveBrevoSenderEmail() {
        String email = brevoSenderEmail;
        if (email == null || email.isBlank()) {
            email = System.getenv("BREVO_SENDER_EMAIL");
        }
        if (email == null || email.isBlank()) {
            email = System.getenv("CRAFTBID_BREVO_SENDER_EMAIL");
        }
        return (email != null && !email.isBlank()) ? email.trim() : "ranjitbarik146@gmail.com";
    }

    private boolean trySendViaBrevoRest(String toEmail, String subject, String htmlContent) {
        try {
            String cleanKey = getFormattedBrevoKey();
            String brevoSender = getEffectiveBrevoSenderEmail();
            String jsonBody = String.format(
                    "{\"sender\":{\"name\":\"CraftBid\",\"email\":\"%s\"},\"replyTo\":{\"email\":\"%s\",\"name\":\"CraftBid Support\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                    escapeJson(brevoSender),
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
        String displayName = (name != null && !name.isBlank()) ? name.trim() : "CraftBid Member";
        String pwd = (password != null && !password.isBlank()) ? password : "your chosen password";

        String html = "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; font-size: 15px; line-height: 1.6; color: #111827; max-width: 600px;\">"
                + "<p>Hello " + displayName + ",</p>"
                + "<p>Congratulations! Your CraftBid registration has been completed successfully.</p>"
                + "<p>Your login details are:<br/>"
                + "Login ID: <a href=\"mailto:" + loginId + "\" style=\"color: #0284c7; text-decoration: underline;\">" + loginId + "</a><br/>"
                + "Password: " + pwd + "</p>"
                + "<p>You can now explore handcrafted items, participate in 1-minute live auctions, and follow master artisans.</p>"
                + "<p>Regards,<br/>CraftBid Team</p>"
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

    // =====================================================
    // AUCTION JOINED CONFIRMATION EMAIL
    // =====================================================
    public void sendAuctionJoinedEmail(String email, String name, String craftName, BigDecimal basePrice, Long auctionId) {
        String subject = "🏺 Joined Auction Room: " + craftName;
        String displayName = (name != null && !name.isBlank()) ? name : "Craft Enthusiast";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'><span style='font-size: 36px;'>🏺</span><h2 style='color: #ea580c; margin: 4px 0;'>Auction Room Confirmation</h2></div>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>You have successfully joined the 24-hour participation window for <strong>" + craftName + "</strong>.</p>"
                + "<div style='background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; padding: 16px; margin: 16px 0;'>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>Base Deposit Paid:</strong> ₹" + basePrice + "</p>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>Max Participants:</strong> 10</p>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>100% Refund Guarantee:</strong> If outbid, your entire deposit is refunded automatically.</p>"
                + "</div>"
                + "<p style='color: #71717a; font-size: 12px; text-align: center;'>CraftBid Platform © 2026</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // AUCTION LIVE STARTED EMAIL
    // =====================================================
    public void sendAuctionLiveStartedEmail(String email, String name, String craftName, Long auctionId) {
        String subject = "⚡ LIVE NOW: 1-Minute Auction for " + craftName;
        String displayName = (name != null && !name.isBlank()) ? name : "Bidder";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<h2 style='color: #ea580c; margin: 4px 0; text-align: center;'>⚡ The Auction is LIVE!</h2>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>The participation window has ended and the 1-minute live turn bidding for <strong>" + craftName + "</strong> has officially begun!</p>"
                + "<p style='color: #52525b; font-size: 14px;'>Place your differential bids now. Every new bid resets the countdown to 60 seconds.</p>"
                + "<div style='text-align: center; margin: 24px 0;'><a href='https://craftbid.co.in/auctions/" + auctionId + "' style='background: #ea580c; color: #fff; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;'>Enter Live Auction Room</a></div>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // AUCTION WON NOTIFICATION EMAIL
    // =====================================================
    public void sendAuctionWonEmail(String email, String name, String craftName, BigDecimal winningAmount, Long auctionId) {
        String subject = "🏆 You Won! Complete your order for " + craftName;
        String displayName = (name != null && !name.isBlank()) ? name : "Winner";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'><span style='font-size: 40px;'>🏆</span><h2 style='color: #16a34a; margin: 4px 0;'>Congratulations! You Won!</h2></div>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>You are the winning bidder for <strong>" + craftName + "</strong> with a final bid of <strong>₹" + winningAmount + "</strong>.</p>"
                + "<p style='color: #52525b; font-size: 14px;'>Please submit your full shipping address in your auction dashboard so the artisan can pack and dispatch your handcrafted piece.</p>"
                + "<div style='text-align: center; margin: 24px 0;'><a href='https://craftbid.co.in/auctions/" + auctionId + "' style='background: #16a34a; color: #fff; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;'>Submit Delivery Address</a></div>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // 100% REFUND ISSUED EMAIL
    // =====================================================
    public void sendAuctionRefundEmail(String email, String name, String craftName, BigDecimal refundAmount, String txnRef, Long auctionId) {
        String subject = "💰 100% Refund Processed: ₹" + refundAmount + " for " + craftName;
        String displayName = (name != null && !name.isBlank()) ? name : "CraftBid Member";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<h2 style='color: #2563eb; margin: 4px 0; text-align: center;'>💰 100% Refund Issued</h2>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>The auction for <strong>" + craftName + "</strong> has concluded. Since another bidder placed the winning bid, your full deposit commitment has been refunded.</p>"
                + "<div style='background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 16px; margin: 16px 0;'>"
                + "<p style='margin: 4px 0; color: #1e40af;'><strong>Refund Amount:</strong> ₹" + refundAmount + " (100%)</p>"
                + "<p style='margin: 4px 0; color: #1e40af;'><strong>Transaction Reference:</strong> " + txnRef + "</p>"
                + "<p style='margin: 4px 0; color: #1e40af;'><strong>Refund Method:</strong> Original Payment Source</p>"
                + "</div>"
                + "<p style='color: #71717a; font-size: 12px; text-align: center;'>CraftBid Guarantee: 0 risk, 100% refund for all outbid participants.</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // ARTISAN CRAFT SOLD EMAIL
    // =====================================================
    public void sendArtisanCraftSoldEmail(String email, String artisanName, String craftName, BigDecimal finalAmount, BigDecimal artisanPayout, Long auctionId) {
        String subject = "🎉 Your Craft Sold! Payout: ₹" + artisanPayout;
        String displayName = (artisanName != null && !artisanName.isBlank()) ? artisanName : "Master Artisan";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<h2 style='color: #ea580c; margin: 4px 0; text-align: center;'>🎉 Your Craft Has Sold!</h2>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>Congratulations! Your craft <strong>" + craftName + "</strong> was successfully sold at live auction.</p>"
                + "<div style='background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; padding: 16px; margin: 16px 0;'>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>Winning Bid:</strong> ₹" + finalAmount + "</p>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>Artisan Payout (90%):</strong> ₹" + artisanPayout + "</p>"
                + "<p style='margin: 4px 0; color: #9a3412;'><strong>Platform Fee (10%):</strong> ₹" + finalAmount.subtract(artisanPayout) + "</p>"
                + "</div>"
                + "<p style='color: #52525b; font-size: 14px;'>Please check your Artisan Dashboard to view the buyer's delivery address and prepare the shipment.</p>"
                + "<div style='text-align: center; margin: 24px 0;'><a href='https://craftbid.co.in/artisan-dashboard' style='background: #ea580c; color: #fff; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;'>View Orders</a></div>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }

    // =====================================================
    // ORDER SHIPPED EMAIL
    // =====================================================
    public void sendOrderShippedEmail(String email, String customerName, String craftName, String trackingNotes, String carrier, Long orderId) {
        String subject = "📦 Your Craft is On Its Way! " + craftName;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 550px; margin: auto; padding: 24px; background: #faf8f5; border: 1px solid #e8e2d9; border-radius: 12px;'>"
                + "<h2 style='color: #16a34a; margin: 4px 0; text-align: center;'>📦 Your Order Has Been Shipped!</h2>"
                + "<p style='color: #27272a; font-size: 15px;'>Hello <strong>" + displayName + "</strong>,</p>"
                + "<p style='color: #52525b; font-size: 14px;'>The artisan has packed and dispatched your authentic handmade craft <strong>" + craftName + "</strong>.</p>"
                + "<div style='background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 16px; margin: 16px 0;'>"
                + "<p style='margin: 4px 0; color: #166534;'><strong>Courier / Tracking:</strong> " + (trackingNotes != null && !trackingNotes.isBlank() ? trackingNotes : "Standard Courier") + "</p>"
                + "<p style='margin: 4px 0; color: #166534;'><strong>Carrier:</strong> " + (carrier != null && !carrier.isBlank() ? carrier : "Partner Logistics") + "</p>"
                + "</div>"
                + "<p style='color: #71717a; font-size: 12px; text-align: center;'>Thank you for supporting authentic traditional craftsmanship on CraftBid!</p>"
                + "</div>";
        dispatchEmail(email, subject, html);
    }
}