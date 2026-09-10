package com.craftbid.service;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AdminOtpService {

    // Authorized admin emails that can access admin login
    private static final Set<String> AUTHORIZED_ADMIN_EMAILS = Set.of(
            "craftbid.official@gmail.com",
            "ranjitbarik146@gmail.com",
            "ranjitbarik.official@gmail.com",
            "rb650196@gmail.com",
            "ranjitbarik466@gmail.com"
    );

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long RESEND_COOLDOWN_MILLIS = 60_000L; // 60 seconds
    private static final long LOCKOUT_DURATION_MILLIS = 15 * 60_000L; // 15 minutes
    private static final int MAX_SENDS_PER_WINDOW = 5;
    private static final long SEND_WINDOW_MILLIS = 15 * 60_000L; // 15 minutes

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    // In-memory rate limiting and brute-force protection maps
    private final Map<String, Long> lastSendTimeMap = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> sendTimestampsMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttemptsMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lockoutUntilMap = new ConcurrentHashMap<>();

    public AdminOtpService(
            UserRepository userRepository,
            EmailService emailService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean isAuthorizedAdmin(String email) {
        if (email == null) return false;
        return AUTHORIZED_ADMIN_EMAILS.contains(email.trim().toLowerCase());
    }

    // ==========================================
    // SEND OTP (SECURE SERVER-SIDE GENERATION)
    // ==========================================
    public void sendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Administrator email is required");
        }

        String cleanEmail = email.trim().toLowerCase();
        long now = System.currentTimeMillis();

        // Check active brute-force lockout
        Long lockoutUntil = lockoutUntilMap.get(cleanEmail);
        if (lockoutUntil != null && lockoutUntil > now) {
            long remainingMinutes = Math.max(1, (lockoutUntil - now + 59_999) / 60_000);
            throw new RuntimeException("Too many failed security attempts. Account temporarily locked for " + remainingMinutes + " minute(s).");
        }

        // Check resend cooldown (minimum 60 seconds)
        Long lastSend = lastSendTimeMap.get(cleanEmail);
        if (lastSend != null && (now - lastSend) < RESEND_COOLDOWN_MILLIS) {
            long remainingSec = Math.max(1, (RESEND_COOLDOWN_MILLIS - (now - lastSend) + 999) / 1000);
            throw new RuntimeException("Please wait " + remainingSec + " second(s) before requesting another OTP code.");
        }

        // Check maximum send requests in sliding window
        List<Long> sendTimestamps = sendTimestampsMap.computeIfAbsent(cleanEmail, k -> new CopyOnWriteArrayList<>());
        sendTimestamps.removeIf(ts -> (now - ts) > SEND_WINDOW_MILLIS);
        if (sendTimestamps.size() >= MAX_SENDS_PER_WINDOW) {
            throw new RuntimeException("OTP request limit reached (5 requests per 15 minutes). Please try again later.");
        }

        // Find or auto-provision authorized administrator
        User admin = userRepository
                .findByEmail(cleanEmail)
                .orElseGet(() -> {
                    if (isAuthorizedAdmin(cleanEmail)) {
                        User newAdmin = new User();
                        newAdmin.setName("CraftBid Administrator");
                        newAdmin.setEmail(cleanEmail);
                        newAdmin.setPhone("9040408690");
                        newAdmin.setRole(Role.ADMIN);
                        newAdmin.setActive(true);
                        newAdmin.setSellerEnabled(true);
                        return userRepository.save(newAdmin);
                    }
                    throw new RuntimeException("Access denied. " + cleanEmail + " is not an authorized administrator.");
                });

        // Ensure role and active status
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            if (isAuthorizedAdmin(cleanEmail)) {
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
            } else {
                throw new RuntimeException("Access denied. Account is not configured as administrator.");
            }
        }

        // Generate 6 digit numeric OTP (server-only)
        String rawOtp = String.format("%06d", random.nextInt(1_000_000));

        // Store hashed OTP in database with 5-minute expiration
        admin.setOtp(passwordEncoder.encode(rawOtp));
        admin.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(admin);

        // Record rate-limiting metrics
        lastSendTimeMap.put(cleanEmail, now);
        sendTimestamps.add(now);
        failedAttemptsMap.remove(cleanEmail); // Reset failed attempts counter for new OTP

        // Dispatch OTP strictly via email to authorized administrator
        try {
            emailService.sendAdminOtpEmail(cleanEmail, rawOtp);
        } catch (Exception e) {
            System.err.println("Notice: Admin OTP email dispatch error: " + e.getMessage());
        }
    }

    // ==========================================
    // VERIFY OTP (BRUTE-FORCE PROTECTED)
    // ==========================================
    public LoginResponse verifyOtp(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Admin email is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException("Please enter the 6-digit OTP code");
        }

        String cleanEmail = email.trim().toLowerCase();
        long now = System.currentTimeMillis();

        // Check active brute-force lockout
        Long lockoutUntil = lockoutUntilMap.get(cleanEmail);
        if (lockoutUntil != null && lockoutUntil > now) {
            long remainingMinutes = Math.max(1, (lockoutUntil - now + 59_999) / 60_000);
            throw new RuntimeException("Security lockout active. Please wait " + remainingMinutes + " minute(s) before retrying.");
        }

        // Find admin
        User admin = userRepository
                .findByEmail(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Administrator account not found for " + cleanEmail));

        // Check role and active
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            throw new RuntimeException("Access denied. Not an authorized administrator.");
        }

        // Check database-persisted OTP
        if (admin.getOtp() == null || admin.getOtpExpiry() == null) {
            throw new RuntimeException("Security code not found. Please click 'Send OTP Code'.");
        }

        // Check expiry (5 minutes)
        if (LocalDateTime.now().isAfter(admin.getOtpExpiry())) {
            admin.setOtp(null);
            admin.setOtpExpiry(null);
            userRepository.save(admin);
            failedAttemptsMap.remove(cleanEmail);
            throw new RuntimeException("Security code expired. Please request a new OTP.");
        }

        // Verify hashed OTP (with fallback for BCrypt / plain migration safety)
        String storedOtp = admin.getOtp();
        boolean matches = passwordEncoder.matches(otp.trim(), storedOtp) || storedOtp.equals(otp.trim());

        if (!matches) {
            int attempts = failedAttemptsMap.getOrDefault(cleanEmail, 0) + 1;
            failedAttemptsMap.put(cleanEmail, attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Invalidate OTP in DB and trigger lockout
                admin.setOtp(null);
                admin.setOtpExpiry(null);
                userRepository.save(admin);
                failedAttemptsMap.remove(cleanEmail);
                lockoutUntilMap.put(cleanEmail, now + LOCKOUT_DURATION_MILLIS);
                throw new RuntimeException("Maximum invalid attempts reached (5/5). Security code invalidated and account locked for 15 minutes.");
            }

            int remainingAttempts = MAX_FAILED_ATTEMPTS - attempts;
            throw new RuntimeException("Invalid security code. " + remainingAttempts + " attempt(s) remaining.");
        }

        // Clear used OTP in database immediately upon successful verification
        admin.setOtp(null);
        admin.setOtpExpiry(null);
        userRepository.save(admin);

        // Clear tracking state
        failedAttemptsMap.remove(cleanEmail);
        lockoutUntilMap.remove(cleanEmail);

        // Generate JWT
        String token = jwtService.generateToken(
                admin.getEmail(),
                admin.getRole().name()
        );

        return new LoginResponse(
                token,
                admin.getId(),
                admin.getName(),
                admin.getEmail(),
                admin.getRole().name(),
                admin.isSellerEnabled()
        );
    }
}