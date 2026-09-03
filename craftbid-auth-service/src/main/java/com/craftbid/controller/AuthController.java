package com.craftbid.controller;

import com.craftbid.dto.AdminOtpRequest;
import com.craftbid.dto.AdminOtpVerifyRequest;
import com.craftbid.dto.EmailVerificationRequest;
import com.craftbid.dto.ForgotPasswordRequest;
import com.craftbid.dto.LoginRequest;
import com.craftbid.dto.LoginResponse;
import com.craftbid.dto.RegisterRequest;
import com.craftbid.dto.ResetPasswordRequest;

import com.craftbid.service.AdminOtpService;
import com.craftbid.service.AuthService;
import com.craftbid.service.EmailVerificationService;
import com.craftbid.service.PasswordResetService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminOtpService adminOtpService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            AdminOtpService adminOtpService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService) {

        this.authService = authService;
        this.adminOtpService = adminOtpService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    // ==========================================
    // CUSTOMER / SELLER REGISTER
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                emailVerificationService.sendOtp(request)
        );
    }

    // ==========================================
    // CUSTOMER / SELLER VERIFY REGISTRATION / EMAIL
    // ==========================================

    @PostMapping({"/verify-registration", "/verify-email"})
    public ResponseEntity<String> verifyRegistration(
            @RequestBody EmailVerificationRequest request) {

        return ResponseEntity.ok(
                emailVerificationService.verifyOtp(
                        request.getEffectiveIdentifier(),
                        request.getOtp()
                )
        );
    }

    // ==========================================
    // RESEND CUSTOMER / SELLER OTP
    // ==========================================

    @PostMapping("/resend-verification-otp")
    public ResponseEntity<String> resendVerificationOtp(
            @RequestBody EmailVerificationRequest request) {

        return ResponseEntity.ok(
                emailVerificationService.resendOtp(
                        request.getEffectiveIdentifier()
                )
        );
    }

    // ==========================================
    // CUSTOMER / SELLER LOGIN
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    // ==========================================
    // ADMIN SEND OTP
    // ==========================================

    @PostMapping("/admin/send-otp")
    public ResponseEntity<java.util.Map<String, String>> sendAdminOtp(
            @RequestBody AdminOtpRequest request) {

        String otp = adminOtpService.sendOtp(
                request.getEmail()
        );
        return ResponseEntity.ok(
                java.util.Map.of(
                        "message", "Admin login OTP sent successfully to " + request.getEmail(),
                        "otp", otp
                )
        );
    }

    // ==========================================
    // ADMIN VERIFY OTP
    // ==========================================

    @PostMapping("/admin/verify-otp")
    public ResponseEntity<LoginResponse> verifyAdminOtp(
            @RequestBody AdminOtpVerifyRequest request) {

        return ResponseEntity.ok(
                adminOtpService.verifyOtp(
                        request.getEmail(),
                        request.getOtp()
                )
        );
    }

    // ==========================================
    // FORGOT PASSWORD - SEND OTP
    // ==========================================

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        return ResponseEntity.ok(
                passwordResetService.sendOtp(
                        request.getEmail()
                )
        );
    }

    // ==========================================
    // RESET PASSWORD
    // ==========================================

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        return ResponseEntity.ok(
                passwordResetService.resetPassword(
                        request.getEmail(),
                        request.getOtp(),
                        request.getNewPassword()
                )
        );
    }

    // ==========================================
    // ENABLE ARTISAN / SELLER MODE
    // ==========================================

    @PostMapping("/enable-seller")
    public ResponseEntity<String> enableSeller(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Please login first");
        }

        String identifier = authentication.getName();

        String result = authService.enableSeller(identifier);

        return ResponseEntity.ok(result);
    }
}