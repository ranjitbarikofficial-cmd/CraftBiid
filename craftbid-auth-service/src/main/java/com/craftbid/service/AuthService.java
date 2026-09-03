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

            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException(
                        "Email already registered"
                );
            }
        }

        // Check phone
        if (phone != null && !phone.isBlank()) {

            phone = phone.trim();

            if (userRepository.existsByPhone(phone)) {
                throw new RuntimeException(
                        "Mobile number already registered"
                );
            }
        }
    }

    // =====================================================
    // CREATE USER AFTER OTP VERIFICATION
    // =====================================================

    public User createVerifiedUser(
            RegisterRequest request) {

        String email = request.getEmail();
        String phone = request.getPhone();

        // -------------------------------------------------
        // NORMALIZE EMAIL
        // -------------------------------------------------

        if (email != null && !email.isBlank()) {

            email = email.trim().toLowerCase();

            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException(
                        "Email already registered"
                );
            }

        } else {
            email = null;
        }

        // -------------------------------------------------
        // NORMALIZE PHONE
        // -------------------------------------------------

        if (phone != null && !phone.isBlank()) {

            phone = phone.trim();

            if (userRepository.existsByPhone(phone)) {
                throw new RuntimeException(
                        "Mobile number already registered"
                );
            }

        } else {
            phone = null;
        }

        // -------------------------------------------------
        // AT LEAST ONE LOGIN METHOD
        // -------------------------------------------------

        if (email == null && phone == null) {

            throw new RuntimeException(
                    "Email or mobile number is required"
            );
        }

        // =================================================
        // CREATE NORMAL USER
        // =================================================

        User user = new User();

        user.setName(request.getName());

        user.setEmail(email);

        user.setPhone(phone);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Every normal registered user is CUSTOMER
        user.setRole(Role.CUSTOMER);

        // Account verified
        user.setActive(true);

        // Seller capability disabled initially
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