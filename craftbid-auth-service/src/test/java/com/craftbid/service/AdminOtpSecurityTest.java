package com.craftbid.service;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.repository.UserRepository;
import com.craftbid.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminOtpSecurityTest {

    private UserRepository userRepository;
    private EmailService emailService;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AdminOtpService adminOtpService;

    private final String adminEmail = "craftbid.official@gmail.com";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailService = mock(EmailService.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        adminOtpService = new AdminOtpService(
                userRepository,
                emailService,
                jwtService,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Send OTP should hash the OTP and send raw OTP solely via email")
    void testSendOtp_StoresHashedOtpAndDispatchesEmail() {
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        adminOtpService.sendOtp(adminEmail);

        // Verify email was dispatched with a 6-digit numeric OTP
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendAdminOtpEmail(eq(adminEmail), otpCaptor.capture());
        String dispatchedOtp = otpCaptor.getValue();
        assertNotNull(dispatchedOtp);
        assertEquals(6, dispatchedOtp.length());
        assertTrue(dispatchedOtp.matches("^[0-9]{6}$"));

        // Verify the database-stored OTP is hashed and NOT the raw OTP string
        assertNotNull(adminUser.getOtp());
        assertNotEquals(dispatchedOtp, adminUser.getOtp());
        assertTrue(passwordEncoder.matches(dispatchedOtp, adminUser.getOtp()));
        assertNotNull(adminUser.getOtpExpiry());
        assertTrue(adminUser.getOtpExpiry().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Send OTP should enforce minimum 60s cooldown between resends")
    void testSendOtp_EnforcesCooldown() {
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // First send succeeds
        adminOtpService.sendOtp(adminEmail);

        // Immediate second send fails with cooldown message
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminOtpService.sendOtp(adminEmail));
        assertTrue(ex.getMessage().contains("Please wait") && ex.getMessage().contains("second(s) before requesting another OTP code"));
    }

    @Test
    @DisplayName("Verify OTP with valid code should succeed, return JWT, and clear OTP")
    void testVerifyOtp_Success() {
        String rawOtp = "654321";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setName("CraftBid Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser.setOtp(passwordEncoder.encode(rawOtp));
        adminUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(eq(adminEmail), eq("ADMIN"))).thenReturn("mock-admin-jwt-token");

        LoginResponse response = adminOtpService.verifyOtp(adminEmail, rawOtp);

        assertNotNull(response);
        assertEquals("mock-admin-jwt-token", response.getToken());
        assertEquals(adminEmail, response.getEmail());
        assertEquals("ADMIN", response.getRole());

        // Verify OTP was cleared in database
        assertNull(adminUser.getOtp());
        assertNull(adminUser.getOtpExpiry());
        verify(userRepository).save(adminUser);
    }

    @Test
    @DisplayName("Verify OTP with invalid code should fail and track attempt count")
    void testVerifyOtp_InvalidCode() {
        String rawOtp = "654321";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser.setOtp(passwordEncoder.encode(rawOtp));
        adminUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminOtpService.verifyOtp(adminEmail, "000000"));
        assertTrue(ex.getMessage().contains("Invalid security code") && ex.getMessage().contains("attempt(s) remaining"));
    }

    @Test
    @DisplayName("Verify OTP after 5 failed attempts should invalidate OTP and lock account")
    void testVerifyOtp_LockoutAfter5Attempts() {
        String rawOtp = "654321";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser.setOtp(passwordEncoder.encode(rawOtp));
        adminUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        for (int i = 1; i <= 4; i++) {
            final String badOtp = "11111" + i;
            assertThrows(RuntimeException.class, () -> adminOtpService.verifyOtp(adminEmail, badOtp));
        }

        // 5th attempt invalidates OTP and locks
        RuntimeException ex5 = assertThrows(RuntimeException.class, () -> adminOtpService.verifyOtp(adminEmail, "999999"));
        assertTrue(ex5.getMessage().contains("Maximum invalid attempts reached"));
        assertNull(adminUser.getOtp());

        // Subsequent attempt is blocked by lockout
        RuntimeException lockedEx = assertThrows(RuntimeException.class, () -> adminOtpService.verifyOtp(adminEmail, rawOtp));
        assertTrue(lockedEx.getMessage().contains("Security lockout active"));
    }

    @Test
    @DisplayName("Verify OTP with expired code should fail and invalidate expired OTP")
    void testVerifyOtp_Expired() {
        String rawOtp = "654321";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser.setOtp(passwordEncoder.encode(rawOtp));
        adminUser.setOtpExpiry(LocalDateTime.now().minusMinutes(1)); // Expired

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminOtpService.verifyOtp(adminEmail, rawOtp));
        assertTrue(ex.getMessage().contains("expired"));
        assertNull(adminUser.getOtp());
    }
}
