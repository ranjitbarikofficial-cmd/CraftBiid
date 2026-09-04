package com.craftbid.service;

import com.craftbid.dto.LoginRequest;
import com.craftbid.dto.LoginResponse;
import com.craftbid.dto.RegisterRequest;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // =====================================================
    // =====================================================
    // CHECK EMAIL OR PHONE
    // =====================================================

    public void checkRegistrationDetails(RegisterRequest request) {

        String email = request.getEmail();
        String phone = request.getPhone();

        // At least one must be provided
        if ((email == null || email.isBlank()) &&
                (phone == null || phone.isBlank())) {

            throw new RuntimeException(
                    "Email or mobile number is required"
            );
        }

        // Check email
        if (email != null && !email.isBlank()) {
            email = email.trim().toLowerCase();
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (existing.isActive()) {
                    throw new RuntimeException("Email is already registered. Please login.");
                }
            });
        }

        // Check phone
        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();
            userRepository.findByPhone(phone).ifPresent(existing -> {
                if (existing.isActive()) {
                    throw new RuntimeException("Mobile number is already registered. Please login.");
                }
            });
        }
    }

    // =====================================================
    // SAVE PENDING REGISTRATION USER WITH OTP IN DATABASE
    // =====================================================
    public User savePendingUser(RegisterRequest request, String otp, java.time.LocalDateTime expiry) {
        String email = request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim().toLowerCase() : null;
        String phone = request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null;

        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null && phone != null) {
            user = userRepository.findByPhone(phone).orElse(null);
        }

        if (user == null) {
            user = new User();
            user.setRole(Role.CUSTOMER);
            user.setSellerEnabled(false);
        }

        user.setName(request.getName());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(false);
        user.setOtp(otp);
        user.setOtpExpiry(expiry);

        return userRepository.save(user);
    }

    // =====================================================
    // VERIFY PENDING USER AFTER OTP IN DATABASE
    // =====================================================
    public User verifyPendingUser(String identifier, String otp) {
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or mobile number is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("OTP code is required");
        }

        String cleanIdentifier = identifier.trim();
        User user = userRepository.findByIdentifier(cleanIdentifier)
                .orElseThrow(() -> new RuntimeException("Registration record not found for " + cleanIdentifier + ". Please register first."));

        if (user.isActive()) {
            return user;
        }

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new RuntimeException("OTP not found. Please click 'Resend OTP'.");
        }

        if (java.time.LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new RuntimeException("OTP expired. Please click 'Resend OTP' to get a new code.");
        }

        if (!user.getOtp().equals(otp.trim())) {
            throw new RuntimeException("Invalid OTP code. Please enter the 6-digit code sent to your email.");
        }

        user.setActive(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        return userRepository.save(user);
    }

    // =====================================================
    // CREATE USER AFTER OTP VERIFICATION (LEGACY COMPAT)
    // =====================================================

    public User createVerifiedUser(
            RegisterRequest request) {

        String email = request.getEmail();
        String phone = request.getPhone();

        if (email != null && !email.isBlank()) {
            email = email.trim().toLowerCase();
        } else {
            email = null;
        }

        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();
        } else {
            phone = null;
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setSellerEnabled(false);

        return userRepository.save(user);
    }

    // =====================================================
    // LOGIN USING EMAIL OR MOBILE
    // =====================================================

    public LoginResponse login(
            LoginRequest request) {

        if (request.getIdentifier() == null ||
                request.getIdentifier().isBlank()) {

            throw new RuntimeException(
                    "Email or mobile number is required"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new RuntimeException(
                    "Password is required"
            );
        }

        String identifier =
                request.getIdentifier().trim();

        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException(
                                identifier.contains("@")
                                        ? "Invalid email or password"
                                        : "Invalid mobile number or password"
                        )
                );

        // =================================================
        // ACCOUNT STATUS
        // =================================================

        if (!user.isActive()) {

            throw new RuntimeException(
                    "Please verify your account before login"
            );
        }

        // =================================================
        // PASSWORD
        // =================================================

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {

            throw new RuntimeException(
                    "Invalid email/mobile or password"
            );
        }

        // =================================================
        // JWT
        // =================================================

        String loginSubject =
                user.getEmail() != null
                        ? user.getEmail()
                        : user.getPhone();

        String token =
                jwtService.generateToken(
                        loginSubject,
                        user.getRole().name()
                );

        // =================================================
        // RESPONSE
        // =================================================

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail() != null ? user.getEmail() : user.getPhone(),
                user.getRole().name(),
                user.isSellerEnabled()
        );
    }

    // =====================================================
    // ENABLE CRAFTBID ARTISAN
    // =====================================================

    public String enableSeller(String identifier) {

        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is inactive"
            );
        }

        if (user.isSellerEnabled()) {
            return "CraftBid Artisan account is already enabled";
        }

        // Enable Artisan account
        user.setSellerEnabled(true);

        userRepository.save(user);

        // Send Artisan activation email if email exists
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendSellerEnabledEmail(
                    user.getEmail(),
                    user.getName()
            );
        }

        return "You are now a CraftBid Artisan. Confirmation email sent.";
    }
}