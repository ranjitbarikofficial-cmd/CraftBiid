package com.craftbid.security;

import com.craftbid.dto.LoginResponse;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.*;
import com.craftbid.service.*;
import com.craftbid.websocket.AuctionEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Production Security Hardening & IDOR Protection Tests")
public class SecurityHardeningTest {

    private ObjectMapper objectMapper;
    private UserRepository userRepository;
    private AuctionRepository auctionRepository;
    private AuctionOrderRepository orderRepository;
    private PaymentTransactionRepository paymentRepository;
    private RefundRepository refundRepository;
    private AuctionService auctionService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        userRepository = mock(UserRepository.class);
        auctionRepository = mock(AuctionRepository.class);
        orderRepository = mock(AuctionOrderRepository.class);
        paymentRepository = mock(PaymentTransactionRepository.class);
        refundRepository = mock(RefundRepository.class);

        CraftRepository craftRepository = mock(CraftRepository.class);
        BidRepository bidRepository = mock(BidRepository.class);
        AuctionParticipantRepository participantRepository = mock(AuctionParticipantRepository.class);
        RazorpayService razorpayService = mock(RazorpayService.class);
        AuctionEventPublisher eventPublisher = mock(AuctionEventPublisher.class);
        NotificationService notificationService = mock(NotificationService.class);

        paymentService = new PaymentService(
                paymentRepository,
                refundRepository,
                userRepository,
                auctionRepository,
                participantRepository,
                razorpayService,
                eventPublisher,
                notificationService
        );

        auctionService = new AuctionService(
                auctionRepository,
                bidRepository,
                craftRepository,
                userRepository,
                participantRepository,
                orderRepository,
                paymentService,
                notificationService,
                eventPublisher,
                razorpayService
        );
    }

    // =========================================================================
    // 1. CREDENTIAL & SECRET SUPPRESSION ON JSON SERIALIZATION
    // =========================================================================

    @Test
    @DisplayName("User entity JSON serialization must NEVER expose password hash or active OTP")
    void shouldNeverSerializePasswordOrOtpInUserEntity() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setName("Ranjit Barik");
        user.setEmail("user@example.com");
        user.setPhone("9876543210");
        user.setPassword("$2a$10$SecretBCryptHashedPassword1234567890abcdef");
        user.setOtp("$2a$10$HashedOtpSecretCode987654");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);

        String json = objectMapper.writeValueAsString(user);

        assertNotNull(json);
        assertFalse(json.contains("password"), "User JSON must not contain 'password' field");
        assertFalse(json.contains("SecretBCryptHashedPassword"), "User JSON must not contain raw/hashed password");
        assertFalse(json.contains("otp"), "User JSON must not contain 'otp' field");
        assertFalse(json.contains("HashedOtpSecretCode"), "User JSON must not contain raw/hashed OTP");
        assertFalse(json.contains("otpExpiry"), "User JSON must not contain 'otpExpiry' field");
        assertTrue(json.contains("user@example.com"));
        assertTrue(json.contains("Ranjit Barik"));
    }

    @Test
    @DisplayName("PaymentTransaction JSON serialization must NEVER expose raw razorpaySignature")
    void shouldNeverSerializeRazorpaySignature() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Bidder One");

        PaymentTransaction tx = new PaymentTransaction(
                user,
                55L,
                12L,
                new BigDecimal("1500.00"),
                "PARTICIPATION",
                "RAZORPAY",
                "CB-ORD-12345",
                "CAPTURED",
                "Participation deposit"
        );
        tx.setRazorpaySignature("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        String json = objectMapper.writeValueAsString(tx);

        assertNotNull(json);
        assertFalse(json.contains("razorpaySignature"), "PaymentTransaction JSON must not expose razorpaySignature");
        assertFalse(json.contains("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    // =========================================================================
    // 2. IDOR PROTECTION ON AUCTION ORDERS
    // =========================================================================

    @Test
    @DisplayName("getAuctionOrder must reject non-owner and non-artisan attackers with AccessDeniedException")
    void shouldBlockUnauthorizedAccessToAuctionOrder() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@example.com");

        User artisan = new User();
        artisan.setId(2L);
        artisan.setEmail("artisan@example.com");

        User attacker = new User();
        attacker.setId(99L);
        attacker.setEmail("attacker@example.com");
        attacker.setRole(Role.CUSTOMER);

        Auction auction = new Auction();
        auction.setId(10L);
        auction.setSeller(artisan);
        auction.setStatus(AuctionStatus.ENDED);

        AuctionOrder order = new AuctionOrder();
        order.setId(100L);
        order.setAuction(auction);
        order.setBuyer(buyer);
        order.setArtisan(artisan);
        order.setStreetAddress("123 Private Street, Secret Colony");
        order.setPhone("9999999999");

        when(userRepository.findByIdentifier("attacker@example.com")).thenReturn(Optional.of(attacker));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.of(order));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> auctionService.getAuctionOrder("attacker@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("getAuctionOrder should allow winning buyer, artisan, and platform admin")
    void shouldAllowAuthorizedRolesToViewAuctionOrder() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@example.com");
        buyer.setRole(Role.CUSTOMER);

        User artisan = new User();
        artisan.setId(2L);
        artisan.setEmail("artisan@example.com");
        artisan.setRole(Role.CUSTOMER);

        User admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@craftbid.co.in");
        admin.setRole(Role.ADMIN);

        Auction auction = new Auction();
        auction.setId(10L);
        auction.setSeller(artisan);
        auction.setStatus(AuctionStatus.ENDED);

        AuctionOrder order = new AuctionOrder();
        order.setId(100L);
        order.setAuction(auction);
        order.setBuyer(buyer);
        order.setArtisan(artisan);

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.of(order));

        when(userRepository.findByIdentifier("buyer@example.com")).thenReturn(Optional.of(buyer));
        assertTrue(auctionService.getAuctionOrder("buyer@example.com", 10L).isPresent());

        when(userRepository.findByIdentifier("artisan@example.com")).thenReturn(Optional.of(artisan));
        assertTrue(auctionService.getAuctionOrder("artisan@example.com", 10L).isPresent());

        when(userRepository.findByIdentifier("admin@craftbid.co.in")).thenReturn(Optional.of(admin));
        assertTrue(auctionService.getAuctionOrder("admin@craftbid.co.in", 10L).isPresent());
    }

    // =========================================================================
    // 3. IDOR & RBAC PROTECTION ON PAYMENTS AND REFUNDS
    // =========================================================================

    @Test
    @DisplayName("processRefund must reject non-admin users with AccessDeniedException")
    void shouldRejectNonAdminRefundInitiation() {
        User customer = new User();
        customer.setId(5L);
        customer.setEmail("customer@example.com");
        customer.setRole(Role.CUSTOMER);

        when(userRepository.findByIdentifier("customer@example.com")).thenReturn(Optional.of(customer));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.processRefund(100L, new BigDecimal("500"), "User requested refund", "customer@example.com")
        );
        assertTrue(ex.getMessage().contains("Only administrators"));
    }

    @Test
    @DisplayName("getById on payment transactions must reject other users with AccessDeniedException")
    void shouldRejectOtherUsersFromViewingPaymentTransactions() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        User eavesdropper = new User();
        eavesdropper.setId(2L);
        eavesdropper.setEmail("eavesdropper@example.com");
        eavesdropper.setRole(Role.CUSTOMER);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(77L);
        tx.setUser(owner);
        tx.setAmount(new BigDecimal("2000.00"));

        when(userRepository.findByIdentifier("eavesdropper@example.com")).thenReturn(Optional.of(eavesdropper));
        when(paymentRepository.findById(77L)).thenReturn(Optional.of(tx));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getById(77L, "eavesdropper@example.com")
        );
        assertTrue(ex.getMessage().contains("Access denied"));
    }
}
