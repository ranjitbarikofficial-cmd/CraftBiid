package com.craftbid.service;

import com.craftbid.dto.CashfreeOrderResponse;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.exception.AlreadyJoinedException;
import com.craftbid.repository.*;
import com.craftbid.websocket.AuctionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CashfreeGatewayIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionParticipantRepository participantRepository;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuctionEventPublisher eventPublisher;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionInterestRepository auctionInterestRepository;

    private CashfreeService cashfreeService;

    private User buyer1;
    private User buyer2;
    private User seller;
    private Craft craft;
    private Auction auction;

    @BeforeEach
    void setUp() {
        cashfreeService = new CashfreeService(
                userRepository,
                auctionRepository,
                participantRepository,
                paymentRepository,
                refundRepository,
                notificationService,
                eventPublisher,
                bidRepository,
                auctionInterestRepository
        );

        ReflectionTestUtils.setField(cashfreeService, "appId", "TEST10343825838cf4023dd41a942cf852834301");
        ReflectionTestUtils.setField(cashfreeService, "secretKey", "cfsk_ma_test_c9b4df16bb4b9ee58c42b66236b28096_176e0ffc");
        ReflectionTestUtils.setField(cashfreeService, "apiVersion", "2023-08-01");
        ReflectionTestUtils.setField(cashfreeService, "environment", "SANDBOX");

        buyer1 = new User();
        buyer1.setId(10L);
        buyer1.setName("Rohan Das");
        buyer1.setEmail("rohan@example.com");
        buyer1.setCity("Mumbai");

        buyer2 = new User();
        buyer2.setId(20L);
        buyer2.setName("Pooja Iyer");
        buyer2.setEmail("pooja@example.com");
        buyer2.setCity("Bengaluru");

        seller = new User();
        seller.setId(30L);
        seller.setName("Artisan Shyam");
        seller.setEmail("shyam@artisan.com");

        craft = new Craft();
        craft.setId(100L);
        craft.setTitle("Handmade Terracotta Lamp");
        craft.setBasePrice(new BigDecimal("350.00"));
        craft.setSeller(seller);

        auction = new Auction();
        auction.setId(5L);
        auction.setCraft(craft);
        auction.setSeller(seller);
        auction.setStartingPrice(new BigDecimal("350.00"));
        auction.setCurrentHighestBid(new BigDecimal("350.00"));
        auction.setMaxParticipants(10);
        auction.setCurrentParticipantsCount(0);
        auction.setFirstDepositPaidAt(null);
        auction.setParticipationDeadline(null);
        auction.setStatus(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("1. Order Creation MUST NOT create AuctionParticipant or set timer")
    void testOrderCreationDoesNotCreateParticipantOrSetTimer() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());

        CashfreeOrderResponse orderRes = cashfreeService.createOrder(
                "rohan@example.com",
                new BigDecimal("10.00"), // Tampered amount should be overridden by server-side startingPrice
                5L,
                100L,
                "PARTICIPATION"
        );

        assertNotNull(orderRes);
        assertNotNull(orderRes.getOrderId());
        assertEquals(new BigDecimal("350.00"), orderRes.getOrderAmount()); // Enforces 350.00 server-side

        // Verify Auction state is unmodified
        assertNull(auction.getFirstDepositPaidAt(), "firstDepositPaidAt MUST remain NULL on order creation");
        assertNull(auction.getParticipationDeadline(), "participationDeadline MUST remain NULL on order creation");
        assertEquals(0, auction.getCurrentParticipantsCount(), "currentParticipantsCount MUST remain 0 on order creation");

        // Verify participantRepository.save() was NEVER called
        verify(participantRepository, never()).save(any());

        // Verify transaction saved with CREATED status
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(txCaptor.capture());
        PaymentTransaction savedTx = txCaptor.getValue();
        assertEquals("CREATED", savedTx.getStatus());
        assertEquals(new BigDecimal("350.00"), savedTx.getAmount());
    }

    @Test
    @DisplayName("2. Artisan cannot create participation order in own auction")
    void testArtisanCannotParticipateInOwnAuction() {
        when(userRepository.findByIdentifier("shyam@artisan.com")).thenReturn(Optional.of(seller));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));

        assertThrows(AccessDeniedException.class, () ->
                cashfreeService.createOrder("shyam@artisan.com", new BigDecimal("350.00"), 5L, 100L, "PARTICIPATION"));

        verify(paymentRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. First verified Cashfree payment sets firstDepositPaidAt, 24h deadline, and creates participant")
    void testFirstDepositSetsTimerAndCreatesParticipant() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction createdTx = new PaymentTransaction(
                buyer1, 5L, 100L, new BigDecimal("350.00"), "PARTICIPATION", "CASHFREE",
                "CB-CF-order_cb_123", "CREATED", "Created"
        );
        createdTx.setRazorpayOrderId("order_cb_123");
        when(paymentRepository.findByRazorpayOrderId("order_cb_123")).thenReturn(Optional.of(createdTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction verifiedTx = cashfreeService.verifyAndRecordCashfreePayment(
                "rohan@example.com",
                "order_cb_123",
                5L,
                100L,
                new BigDecimal("350.00"),
                "PARTICIPATION",
                "CASHFREE"
        );

        assertNotNull(verifiedTx);
        assertEquals("CAPTURED", verifiedTx.getStatus());

        // Verify AuctionParticipant created
        ArgumentCaptor<AuctionParticipant> pCaptor = ArgumentCaptor.forClass(AuctionParticipant.class);
        verify(participantRepository).save(pCaptor.capture());
        AuctionParticipant savedParticipant = pCaptor.getValue();
        assertEquals("JOINED", savedParticipant.getStatus());
        assertEquals(buyer1, savedParticipant.getUser());
        assertEquals(new BigDecimal("350.00"), savedParticipant.getBasePricePaid());

        // Verify 24-hour timer strictly activated
        assertNotNull(auction.getFirstDepositPaidAt(), "firstDepositPaidAt MUST be set on first verified payment");
        assertNotNull(auction.getParticipationDeadline(), "participationDeadline MUST be set on first verified payment");
        assertEquals(1, auction.getCurrentParticipantsCount());

        LocalDateTime expectedDeadline = auction.getFirstDepositPaidAt().plusHours(24);
        assertEquals(expectedDeadline, auction.getParticipationDeadline());

        // Verify WebSocket & notifications
        verify(eventPublisher, atLeastOnce()).publishAuctionEvent(eq(5L), eq("auction:participant_joined"), any());
        verify(notificationService).notifyAuctionJoined(eq(buyer1), eq("Handmade Terracotta Lamp"), eq(new BigDecimal("350.00")), eq(5L));
    }

    @Test
    @DisplayName("4. Duplicate payment verification MUST NOT duplicate participant or restart timer")
    void testDuplicatePaymentVerificationIdempotency() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));

        // Already captured transaction
        PaymentTransaction capturedTx = new PaymentTransaction(
                buyer1, 5L, 100L, new BigDecimal("350.00"), "PARTICIPATION", "CASHFREE",
                "CB-CF-order_cb_123", "CAPTURED", "Payment verified via Cashfree"
        );
        capturedTx.setRazorpayOrderId("order_cb_123");
        when(paymentRepository.findByRazorpayOrderId("order_cb_123")).thenReturn(Optional.of(capturedTx));

        LocalDateTime originalFirstDeposit = LocalDateTime.now().minusHours(2);
        LocalDateTime originalDeadline = originalFirstDeposit.plusHours(24);
        auction.setFirstDepositPaidAt(originalFirstDeposit);
        auction.setParticipationDeadline(originalDeadline);
        auction.setCurrentParticipantsCount(1);

        PaymentTransaction result = cashfreeService.verifyAndRecordCashfreePayment(
                "rohan@example.com",
                "order_cb_123",
                5L,
                100L,
                new BigDecimal("350.00"),
                "PARTICIPATION",
                "CASHFREE"
        );

        assertEquals("CAPTURED", result.getStatus());
        // Verify participantRepository.save() NEVER called again
        verify(participantRepository, never()).save(any());
        // Verify timer was NOT altered
        assertEquals(originalFirstDeposit, auction.getFirstDepositPaidAt());
        assertEquals(originalDeadline, auction.getParticipationDeadline());
        assertEquals(1, auction.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("5. Second customer joining in 24h window increments count WITHOUT restarting timer")
    void testSecondParticipantDoesNotRestartTimer() {
        LocalDateTime originalFirstDeposit = LocalDateTime.now().minusHours(2);
        LocalDateTime originalDeadline = originalFirstDeposit.plusHours(24);
        auction.setFirstDepositPaidAt(originalFirstDeposit);
        auction.setParticipationDeadline(originalDeadline);
        auction.setCurrentParticipantsCount(1);

        when(userRepository.findByIdentifier("pooja@example.com")).thenReturn(Optional.of(buyer2));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction tx2 = new PaymentTransaction(
                buyer2, 5L, 100L, new BigDecimal("350.00"), "PARTICIPATION", "CASHFREE",
                "CB-CF-order_cb_456", "CREATED", "Created"
        );
        tx2.setRazorpayOrderId("order_cb_456");
        when(paymentRepository.findByRazorpayOrderId("order_cb_456")).thenReturn(Optional.of(tx2));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction verifiedTx = cashfreeService.verifyAndRecordCashfreePayment(
                "pooja@example.com",
                "order_cb_456",
                5L,
                100L,
                new BigDecimal("350.00"),
                "PARTICIPATION",
                "CASHFREE"
        );

        assertNotNull(verifiedTx);
        assertEquals("CAPTURED", verifiedTx.getStatus());

        // Verify 2nd participant saved
        verify(participantRepository).save(any(AuctionParticipant.class));
        assertEquals(2, auction.getCurrentParticipantsCount());

        // Verify timer remained UNCHANGED
        assertEquals(originalFirstDeposit, auction.getFirstDepositPaidAt(), "firstDepositPaidAt MUST NOT change for 2nd participant");
        assertEquals(originalDeadline, auction.getParticipationDeadline(), "participationDeadline MUST NOT change for 2nd participant");
    }

    @Test
    @DisplayName("6. Customer already joined cannot join again")
    void testUserCannotJoinTwice() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
        AuctionParticipant existingParticipant = new AuctionParticipant(auction, buyer1, new BigDecimal("350.00"));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(existingParticipant));

        assertThrows(AlreadyJoinedException.class, () ->
                cashfreeService.createOrder("rohan@example.com", new BigDecimal("350.00"), 5L, 100L, "PARTICIPATION"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Webhook signature verification succeeds with valid HMAC-SHA256 and fails on tampered payload")
    void testWebhookSignatureVerification() {
        String secret = "cfsk_ma_test_c9b4df16bb4b9ee58c42b66236b28096_176e0ffc";
        String timestamp = "1727050000";
        String rawBody = "{\"type\":\"PAYMENT_SUCCESS_WEBHOOK\",\"data\":{\"order\":{\"order_id\":\"order_cb_999\",\"order_status\":\"PAID\"},\"payment\":{\"cf_payment_id\":\"cf_pay_123\"}}}";

        // Compute valid signature
        String validSignature = (String) ReflectionTestUtils.invokeMethod(cashfreeService, "computeHmacSha256", timestamp + rawBody, secret);
        assertNotNull(validSignature);

        PaymentTransaction tx = new PaymentTransaction(buyer1, 5L, 100L, new BigDecimal("350.00"), "PARTICIPATION", "CASHFREE", "ref999", "CREATED", null);
        tx.setRazorpayOrderId("order_cb_999");
        when(paymentRepository.findByRazorpayOrderId("order_cb_999")).thenReturn(Optional.of(tx));

        // Test with VALID signature
        Map<String, Object> successResult = cashfreeService.processWebhook(rawBody, validSignature, timestamp);
        assertEquals("processed", successResult.get("status"));
        assertEquals("CAPTURED", tx.getStatus());

        // Test with INVALID signature
        Map<String, Object> failedResult = cashfreeService.processWebhook(rawBody, "invalid_signature_hash", timestamp);
        assertEquals("error", failedResult.get("status"));
        assertEquals("Invalid signature", failedResult.get("message"));
    }

    @Test
    @DisplayName("8. Differential Bid Order creates order for ONLY the difference (targetBid - totalPaid)")
    void testDifferentialBidOrderCalculatesDifferenceFromTotalPaid() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));

        // Buyer already paid 350 base deposit
        AuctionParticipant participant = new AuctionParticipant(auction, buyer1, new BigDecimal("350.00"));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(participant));

        auction.setCurrentHighestBid(new BigDecimal("350.00"));
        auction.setTotalBids(1);
        auction.setMinBidIncrement(new BigDecimal("50.00"));

        // User bids 450. Difference to pay = 450 - 350 = 100.
        CashfreeOrderResponse orderRes = cashfreeService.createOrder(
                "rohan@example.com",
                new BigDecimal("450.00"),
                5L,
                100L,
                "DIFFERENTIAL_BID"
        );

        assertNotNull(orderRes);
        assertEquals(new BigDecimal("100.00"), orderRes.getOrderAmount(), "Order amount MUST be exactly differential (₹100)");
        assertEquals("DIFFERENTIAL_BID", orderRes.getType());

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(txCaptor.capture());
        PaymentTransaction savedTx = txCaptor.getValue();
        assertEquals("CREATED", savedTx.getStatus());
        assertEquals(new BigDecimal("100.00"), savedTx.getAmount());
        assertEquals("DIFFERENTIAL_BID", savedTx.getType());
    }

    @Test
    @DisplayName("9. Differential Bid rejected if user has not joined by paying base deposit")
    void testDifferentialBidRequiresPriorParticipation() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                cashfreeService.createOrder("rohan@example.com", new BigDecimal("450.00"), 5L, 100L, "DIFFERENTIAL_BID"));
    }

    @Test
    @DisplayName("10. Differential Bid rejected if target bid is below minimum required increment")
    void testDifferentialBidEnforcesMinimumIncrement() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));

        AuctionParticipant participant = new AuctionParticipant(auction, buyer1, new BigDecimal("350.00"));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(participant));

        auction.setCurrentHighestBid(new BigDecimal("350.00"));
        auction.setTotalBids(1);
        auction.setMinBidIncrement(new BigDecimal("50.00")); // Min next bid = 400.00

        // User attempts to bid 380 (below 400 minimum)
        assertThrows(IllegalArgumentException.class, () ->
                cashfreeService.createOrder("rohan@example.com", new BigDecimal("380.00"), 5L, 100L, "DIFFERENTIAL_BID"));
    }

    @Test
    @DisplayName("11. Differential Bid verification atomically updates totalAmountPaid, saves Bid, and resets 60s turn timer")
    void testDifferentialBidVerificationRecordsBidAndResetsTurnTimer() {
        when(userRepository.findByIdentifier("rohan@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));

        AuctionParticipant participant = new AuctionParticipant(auction, buyer1, new BigDecimal("350.00"));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(participant));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction diffTx = new PaymentTransaction(
                buyer1, 5L, 100L, new BigDecimal("100.00"), "DIFFERENTIAL_BID", "CASHFREE",
                "CB-CF-order_cb_diff1", "CREATED", "Created"
        );
        diffTx.setRazorpayOrderId("order_cb_diff1");
        when(paymentRepository.findByRazorpayOrderId("order_cb_diff1")).thenReturn(Optional.of(diffTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentTransaction verifiedTx = cashfreeService.verifyAndRecordCashfreePayment(
                "rohan@example.com",
                "order_cb_diff1",
                5L,
                100L,
                new BigDecimal("100.00"),
                "DIFFERENTIAL_BID",
                "CASHFREE"
        );

        assertNotNull(verifiedTx);
        assertEquals("CAPTURED", verifiedTx.getStatus());

        // Verify participant updated: 350 + 100 = 450 total committed
        assertEquals(new BigDecimal("450.00"), participant.getTotalAmountPaid());
        verify(participantRepository).save(participant);

        // Verify Bid record created
        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(bidCaptor.capture());
        Bid savedBid = bidCaptor.getValue();
        assertEquals(new BigDecimal("450.00"), savedBid.getAmount());
        assertEquals(buyer1, savedBid.getBidder());
        assertEquals(auction, savedBid.getAuction());

        // Verify Auction updated & 60s turn timer reset
        assertEquals(new BigDecimal("450.00"), auction.getCurrentHighestBid());
        assertEquals(buyer1, auction.getWinningBidder());
        assertEquals(1, auction.getTotalBids());
        assertTrue(auction.isLiveTurnActive());
        assertNotNull(auction.getTurnDeadline());
        assertTrue(auction.getTurnDeadline().isAfter(LocalDateTime.now().plusSeconds(50)));

        // Verify WebSocket broadcast
        verify(eventPublisher).publishAuctionEvent(eq(5L), eq("auction:bid"), any());
    }

    @Test
    @DisplayName("12. Initiate Cashfree 100% refund returns successful refund response")
    void testInitiateCashfreeRefund() {
        Map<String, Object> refundRes = cashfreeService.initiateRefund("order_cb_123", new BigDecimal("350.00"), "100% Outbid Refund");
        assertNotNull(refundRes);
        assertEquals("SUCCESS", refundRes.get("status"));
        assertNotNull(refundRes.get("refundId"));
    }
}
