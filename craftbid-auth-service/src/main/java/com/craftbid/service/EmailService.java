package com.craftbid.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final String FROM_EMAIL = "craftbid.official@gmail.com";
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =====================================================
    // REGISTRATION OTP (NON-BLOCKING ASYNC)
    // =====================================================
    public void sendRegistrationOtpEmail(String email, String otp) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("CraftBid Email Verification OTP: " + otp);
                message.setText(
                        "Hello,\n\n" +
                        "Welcome to CraftBid!\n\n" +
                        "Your email verification OTP is:\n\n" +
                        "       " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n\n" +
                        "Please do not share this OTP with anyone.\n\n" +
                        "Regards,\nCraftBid Team"
                );
                mailSender.send(message);
                System.out.println("✅ Registration OTP email sent successfully to " + email);
            } catch (Exception e) {
                System.err.println("❌ Email dispatch failed for " + email + ": " + e.getMessage());
            }
        });
    }

    // =====================================================
    // REGISTRATION SUCCESS EMAIL (NON-BLOCKING ASYYNC)
    // =====================================================
    public void sendRegistrationSuccessEmail(String email, String name, String loginId, String password) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("🎉 Welcome to CraftBid - Account Created Successfully!");
                message.setText(
                        "Hello " + name + ",\n\n" +
                        "Congratulations! Your CraftBid registration has been completed successfully.\n\n" +
                        "Your login details are:\n" +
                        "Login ID: " + loginId + "\n" +
                        "Password: " + password + "\n\n" +
                        "You can now explore handcrafted items, participate in 1-minute live auctions, and follow master artisans.\n\n" +
                        "Regards,\nCraftBid Team"
                );
                mailSender.send(message);
                System.out.println("✅ Registration success email sent to " + email);
            } catch (Exception e) {
                System.err.println("❌ Registration success email failed: " + e.getMessage());
            }
        });
    }

    // =====================================================
    // ADMIN OTP EMAIL (NON-BLOCKING ASYNC)
    // =====================================================
    public void sendAdminOtpEmail(String email, String otp) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("🛡️ CraftBid Admin Login Security Code: " + otp);
                message.setText(
                        "Hello CraftBid Admin,\n\n" +
                        "Your single-use security code for Admin Console Login is:\n\n" +
                        "       " + otp + "\n\n" +
                        "This code is valid for 5 minutes.\n\n" +
                        "If you did not initiate this login request, please verify your credentials immediately.\n\n" +
                        "Regards,\n" +
                        "CraftBid Security Team"
                );
                mailSender.send(message);
                System.out.println("✅ Admin OTP email delivered to " + email);
            } catch (Exception e) {
                System.err.println("❌ Admin OTP email failed to send: " + e.getMessage());
            }
        });
    }

    // =====================================================
    // FORGOT PASSWORD OTP (NON-BLOCKING ASYNC)
    // =====================================================
    public void sendForgotPasswordOtpEmail(String email, String otp) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("CraftBid Password Reset OTP: " + otp);
                message.setText(
                        "Hello,\n\n" +
                        "We received a request to reset your CraftBid password.\n\n" +
                        "Your password reset OTP is:\n\n" +
                        "       " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n\n" +
                        "Regards,\nCraftBid Team"
                );
                mailSender.send(message);
                System.out.println("✅ Password reset OTP email delivered to " + email);
            } catch (Exception e) {
                System.err.println("❌ Forgot password email failed: " + e.getMessage());
            }
        });
    }

    // =====================================================
    // PASSWORD RESET SUCCESS (NON-BLOCKING ASYNC)
    // =====================================================
    public void sendPasswordResetSuccessEmail(String email, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("CraftBid Password Reset Confirmation");
                message.setText(
                        "Hello " + name + ",\n\n" +
                        "Your CraftBid account password has been reset successfully.\n\n" +
                        "Regards,\nCraftBid Team"
                );
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("❌ Reset confirmation email failed: " + e.getMessage());
            }
        });
    }

    // =====================================================
    // SELLER ENABLED EMAIL (NON-BLOCKING ASYNC)
    // =====================================================
    public void sendSellerEnabledEmail(String email, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_EMAIL);
                message.setTo(email);
                message.setSubject("🎨 Welcome to CraftBid Artisan Studio!");
                message.setText(
                        "Hello " + name + ",\n\n" +
                        "Congratulations! Your Artisan Studio profile has been activated on CraftBid.\n\n" +
                        "You can now upload handcrafted products, launch live auctions, and manage your craft orders.\n\n" +
                        "Regards,\nCraftBid Team"
                );
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("❌ Seller enabled email failed: " + e.getMessage());
            }
        });
    }
}