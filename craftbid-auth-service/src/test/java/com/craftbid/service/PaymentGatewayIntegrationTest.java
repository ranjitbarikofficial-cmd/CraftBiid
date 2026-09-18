package com.craftbid.service;

import com.craftbid.dto.PaymentStatsDTO;
import com.craftbid.dto.RazorpayVerifyRequest;
import com.craftbid.entity.*;
import com.craftbid.repository.AuctionParticipantRepository;
import com.craftbid.repository.AuctionRepository;
import com.craftbid.repository.PaymentTransactionRepository;
import com.craftbid.repository.RefundRepository;
import com.craftbid.repository.UserRepository;
import com.craftbid.websocket.AuctionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentGatewayIntegrationTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionParticipantRepository participantRepository;

    @Mock
    private RazorpayService razorpayService;

    @Mock
    private AuctionEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    private PaymentService paymentService;

    private User buyer;
    private User seller;
    private Craft craft;
    private Auction auction;

    @BeforeEach
    void setUp() {
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

        buyer = new User();
        buyer.setId(10L);
        buyer.setName("Rahul Sharma");
        buyer.setEmail("rahul@example.com");
        buyer.setCity("Jaipur");

        seller = new User();
        seller.setId(20L);
        seller.setName("Artisan Ramesh");
        seller.setEmail("ramesh@artisan.com");

        craft = new Craft();
        craft.setId(100L);
        craft.setTitle("Handmade Blue Pottery Vase");
        craft.setBasePrice(new BigDecimal("500.00"));

        auction = new Auction();
        auction.setId(1L);
        auction.setCraft(craft);
        auction.setSeller(seller);
        auction.setStartingPrice(new BigDecimal("500.00"));
        auction.setCurrentHighestBid(new BigDecimal("500.00"));
        auction.setMaxParticipants(10);
        auction.setCurrentParticipantsCount(2);
        auction.setStatus(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create Razorpay order for auction participation using server-side starting price")
    void testCreateParticipationOrderSuccess() {
        when(userRepository.findByIdentifier("rahul@example.com")).thenReturn(Optional.of(buyer));
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer)).thenReturn(Optional.empty());

        Map<String, Object> rzpMockResponse = new HashMap<>();
        rzpMockResponse.put("orderId", "order_test_12345");
        rzpMockResponse.put("amount", 50000L);
        rzpMockResponse.put("currency", "INR");
        rzpMockResponse.put("keyId", "rzp_test_515a8155e975a5");

        when(razorpayService.createOrder(eq(new BigDecimal("500.00")), anyString(), anyString()))
                .thenReturn(rzpMockResponse);

        Map<String, Object> result = paymentService.createParticipationOrder("rahul@example.com", 1L);

        assertNotNull(result);
        assertEquals("order_test_12345", result.get("orderId"));
        assertEquals(50000L, result.get("amount"));
        assertEquals("INR", result.get("currency"));
        assertEquals(1L, result.get("auctionId"));
        assertEquals("Handmade Blue Pottery Vase", result.get("craftTitle"));

        // Verify transaction saved with CREATED status
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(txCaptor.capture());
        PaymentTransaction savedTx = txCaptor.getValue();
        assertEquals("CREATED", savedTx.getStatus());
        assertEquals("order_test_12345", savedTx.getRazorpayOrderId());
        assertEquals("PARTICIPATION", savedTx.getType());
        assertEquals(new BigDecimal("500.00"), savedTx.getAmount());
    }

    @Test
    @DisplayName("Should reject order creation if auction has reached 10 participant maximum limit")
    void testCreateOrderParticipantLimitExceeded() {
        auction.setCurrentParticipantsCount(10);
        when(userRepository.findByIdentifier("rahul@example.com")).thenReturn(Optional.of(buyer));
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.createParticipationOrder("rahul@example.com", 1L));

        assertTrue(ex.getMessage().contains("Auction room is full"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify valid signature and transition payment to CAPTURED and activate participant")
    void testVerifyAndRecordPaymentSuccess() {
        when(userRepository.findByIdentifier("rahul@example.com")).thenReturn(Optional.of(buyer));
        when(razorpayService.verifySignature("order_test_12345", "pay_test_9999", "sig_valid_hmac"))
                .thenReturn(true);

        PaymentTransaction existingTx = new PaymentTransaction(
                buyer, 1L, 100L, new BigDecimal("500.00"), "PARTICIPATION", "RAZORPAY",
                "CB-ORD-order_test_12345", "CREATED", "Created Razorpay order"
        );
        existingTx.setRazorpayOrderId("order_test_12345");

        when(paymentRepository.findByRazorpayOrderId("order_test_12345")).thenReturn(Optional.of(existingTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer)).thenReturn(Optional.empty());

        PaymentTransaction result = paymentService.verifyAndRecordRazorpayPayment(
                "rahul@example.com",
                "order_test_12345",
                "pay_test_9999",
                "sig_valid_hmac",
                1L,
                100L,
                new BigDecimal("500.00"),
                "PARTICIPATION",
                "UPI"
        );

        assertNotNull(result);
        assertEquals("CAPTURED", result.getStatus());
        assertEquals("pay_test_9999", result.getRazorpayPaymentId());
        assertEquals("sig_valid_hmac", result.getRazorpaySignature());

        // Verify participant created and incremented
        verify(participantRepository).save(any(AuctionParticipant.class));
        assertEquals(3, auction.getCurrentParticipantsCount());
        verify(eventPublisher).publishAuctionEvent(eq(1L), eq("auction:joined"), any());
        verify(notificationService).notifyAuctionJoined(eq(buyer), eq("Handmade Blue Pottery Vase"), eq(new BigDecimal("500.00")), eq(1L));
    }

    @Test
    @DisplayName("Should throw exception when payment signature is invalid")
    void testVerifyPaymentInvalidSignature() {
        when(userRepository.findByIdentifier("rahul@example.com")).thenReturn(Optional.of(buyer));
        when(razorpayService.verifySignature("order_test_12345", "pay_test_9999", "sig_invalid"))
                .thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.verifyAndRecordRazorpayPayment(
                        "rahul@example.com",
                        "order_test_12345",
                        "pay_test_9999",
                        "sig_invalid",
                        1L,
                        100L,
                        new BigDecimal("500.00"),
                        "PARTICIPATION",
                        "UPI"
                ));

        assertTrue(ex.getMessage().contains("Invalid Razorpay payment signature"));
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process automated refund for losing bidder and record in ledger and refund tables")
    void testProcessAutomatedRefund() {
        PaymentTransaction capturedTx = new PaymentTransaction(
                buyer, 1L, 100L, new BigDecimal("500.00"), "PARTICIPATION", "RAZORPAY",
                "CB-ORD-order_test_12345", "CAPTURED", "Captured"
        );
        capturedTx.setRazorpayPaymentId("pay_test_9999");

        when(paymentRepository.findByAuctionIdAndUserAndStatus(1L, buyer, "CAPTURED")).thenReturn(Optional.of(capturedTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> rzpRefundResult = new HashMap<>();
        rzpRefundResult.put("refundId", "rfnd_test_777");
        rzpRefundResult.put("status", "COMPLETED");

        when(razorpayService.processRefund(eq("pay_test_9999"), eq(new BigDecimal("500.00")), anyString()))
                .thenReturn(rzpRefundResult);

        PaymentTransaction refundTx = paymentService.refundAuctionParticipant(
                buyer, 1L, 100L, new BigDecimal("500.00"), "100% Outbid Refund"
        );

        assertNotNull(refundTx);
        assertEquals("REFUNDED", refundTx.getStatus());
        assertEquals("AUTO_REFUND", refundTx.getType());

        // Verify Refund entity saved
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund savedRefund = refundCaptor.getValue();
        assertEquals("rfnd_test_777", savedRefund.getRazorpayRefundId());
        assertEquals("COMPLETED", savedRefund.getStatus());
        assertEquals(new BigDecimal("500.00"), savedRefund.getAmount());
    }

    @Test
    @DisplayName("Should calculate admin payment stats accurately")
    void testGetAdminPaymentStats() {
        PaymentTransaction tx1 = new PaymentTransaction(buyer, 1L, 100L, new BigDecimal("500.00"), "PARTICIPATION", "RAZORPAY", "ref1", "CAPTURED", null);
        PaymentTransaction tx2 = new PaymentTransaction(buyer, 2L, 101L, new BigDecimal("1000.00"), "PARTICIPATION", "RAZORPAY", "ref2", "CAPTURED", null);
        PaymentTransaction tx3 = new PaymentTransaction(buyer, 3L, 102L, new BigDecimal("300.00"), "PARTICIPATION", "RAZORPAY", "ref3", "FAILED", null);

        when(paymentRepository.findAll()).thenReturn(List.of(tx1, tx2, tx3));

        Refund ref1 = new Refund(tx1, buyer, 1L, "pay_1", "rfnd_1", new BigDecimal("500.00"), "COMPLETED", "Outbid refund");
        when(refundRepository.findAll()).thenReturn(List.of(ref1));

        PaymentStatsDTO stats = paymentService.getAdminPaymentStats();

        assertEquals(3, stats.getTotalTransactions());
        assertEquals(2, stats.getSuccessfulTransactions());
        assertEquals(1, stats.getFailedTransactions());
        assertEquals(1, stats.getTotalRefunds());
        assertEquals(new BigDecimal("1500.00"), stats.getTotalVolume());
        assertEquals(new BigDecimal("500.00"), stats.getTotalRefundedVolume());
        assertEquals(new BigDecimal("1000.00"), stats.getNetVolume());
        assertEquals("INR", stats.getCurrency());
    }
}
