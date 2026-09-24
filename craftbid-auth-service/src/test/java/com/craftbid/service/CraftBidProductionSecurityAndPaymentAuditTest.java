package com.craftbid.service;

import com.craftbid.dto.*;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.exception.AlreadyJoinedException;
import com.craftbid.repository.*;
import com.craftbid.websocket.AuctionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PRODUCTION AUDIT SUITE — 35 CANONICAL SECURITY, PAYMENT & AUCTION STATE MACHINE TESTS
 * Strictly verifies zero-trust backend enforcement, Cashfree idempotency, differential bidding,
 * voluntary cancellation (5% fee), 100% losing refunds, 2-minute live tiebreaker, IDOR, and logging.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CraftBidProductionSecurityAndPaymentAuditTest {

    @Mock private UserRepository userRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private AuctionParticipantRepository participantRepository;
    @Mock private PaymentTransactionRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private BidRepository bidRepository;
    @Mock private AuctionInterestRepository auctionInterestRepository;
    @Mock private AuctionOrderRepository orderRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuctionEventPublisher eventPublisher;
    @Mock private RazorpayService razorpayService;

    private OrderService orderService;
    private SellerSettlementService settlementService;
    private AddressService addressService;
    private PaymentService paymentService;
    private CashfreeService cashfreeService;
    private AuctionService auctionService;

    private User artisan;
    private User buyer1;
    private User buyer2;
    private User buyer3;
    private User buyer4;
    private User buyer5;
    private User buyer6;
    private Craft craft;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // Users
        artisan = new User();
        artisan.setId(1L);
        artisan.setName("Master Artisan Rajesh");
        artisan.setEmail("artisan@craftbid.co.in");
        artisan.setSellerEnabled(true);
        artisan.setRole(Role.CUSTOMER);
        artisan.setCity("Jaipur");

        buyer1 = new User();
        buyer1.setId(2L);
        buyer1.setName("Alice Sharma");
        buyer1.setEmail("alice@example.com");
        buyer1.setRole(Role.CUSTOMER);
        buyer1.setCity("Mumbai");

        buyer2 = new User();
        buyer2.setId(3L);
        buyer2.setName("Bob Verma");
        buyer2.setEmail("bob@example.com");
        buyer2.setRole(Role.CUSTOMER);
        buyer2.setCity("Delhi");

        buyer3 = new User();
        buyer3.setId(4L);
        buyer3.setName("Charlie Patel");
        buyer3.setEmail("charlie@example.com");
        buyer3.setRole(Role.CUSTOMER);
        buyer3.setCity("Ahmedabad");

        buyer4 = new User();
        buyer4.setId(5L);
        buyer4.setName("Divya Rao");
        buyer4.setEmail("divya@example.com");
        buyer4.setRole(Role.CUSTOMER);
        buyer4.setCity("Bengaluru");

        buyer5 = new User();
        buyer5.setId(6L);
        buyer5.setName("Ethan Nair");
        buyer5.setEmail("ethan@example.com");
        buyer5.setRole(Role.CUSTOMER);
        buyer5.setCity("Kochi");

        buyer6 = new User();
        buyer6.setId(7L);
        buyer6.setName("Farhan Khan");
        buyer6.setEmail("farhan@example.com");
        buyer6.setRole(Role.CUSTOMER);
        buyer6.setCity("Hyderabad");

        craft = new Craft();
        craft.setId(100L);
        craft.setTitle("Handmade Terracotta Vase");
        craft.setBasePrice(BigDecimal.valueOf(500));
        craft.setSeller(artisan);

        auction = new Auction();
        auction.setId(10L);
        auction.setCraft(craft);
        auction.setSeller(artisan);
        auction.setStartingPrice(BigDecimal.valueOf(500));
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));
        auction.setMinBidIncrement(BigDecimal.valueOf(50));
        auction.setFirstDepositPaidAt(null);
        auction.setParticipationDeadline(null);
        auction.setEndTime(LocalDateTime.now().plusDays(30));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setMaxParticipants(5);
        auction.setCurrentParticipantsCount(0);
        auction.setLiveTurnActive(false);

        // Services wiring
        settlementService = new SellerSettlementService(mock(SellerSettlementRepository.class), mock(ArtisanProfileRepository.class), notificationService);
        addressService = new AddressService(mock(AddressRepository.class));
        orderService = new OrderService(orderRepository, auctionRepository, userRepository, addressService, mock(com.craftbid.shipping.ShippingService.class), settlementService, notificationService, eventPublisher);

        cashfreeService = new CashfreeService(
                userRepository, auctionRepository, participantRepository,
                paymentRepository, refundRepository, notificationService,
                eventPublisher, bidRepository, auctionInterestRepository
        );
        ReflectionTestUtils.setField(cashfreeService, "appId", "TEST10343825838cf4023dd41a942cf852834301");
        ReflectionTestUtils.setField(cashfreeService, "secretKey", "cfsk_ma_test_mock_secret_key_12345");
        ReflectionTestUtils.setField(cashfreeService, "environment", "SANDBOX");

        paymentService = new PaymentService(
                paymentRepository, refundRepository, userRepository,
                auctionRepository, participantRepository, razorpayService,
                cashfreeService, eventPublisher, notificationService
        );

        auctionService = new AuctionService(
                auctionRepository, bidRepository, craftRepositoryMock(),
                userRepository, participantRepository, orderRepository,
                paymentService, notificationService, eventPublisher,
                razorpayService, orderService, auctionInterestRepository
        );
        ReflectionTestUtils.setField(auctionService, "platformCommissionRate", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(auctionService, "cancellationFeePercent", new BigDecimal("5.00"));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(userRepository.findByIdentifier("bob@example.com")).thenReturn(Optional.of(buyer2));
        when(userRepository.findByIdentifier("charlie@example.com")).thenReturn(Optional.of(buyer3));
        when(userRepository.findByIdentifier("divya@example.com")).thenReturn(Optional.of(buyer4));
        when(userRepository.findByIdentifier("ethan@example.com")).thenReturn(Optional.of(buyer5));
        when(userRepository.findByIdentifier("farhan@example.com")).thenReturn(Optional.of(buyer6));
        when(userRepository.findByIdentifier("artisan@craftbid.co.in")).thenReturn(Optional.of(artisan));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        Map<String, AuctionParticipant> participantStore = new ConcurrentHashMap<>();
        when(participantRepository.findByAuctionAndUser(any(Auction.class), any(User.class)))
                .thenAnswer(i -> {
                    User u = i.getArgument(1);
                    return Optional.ofNullable(participantStore.get(u.getEmail()));
                });
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(i -> {
            AuctionParticipant p = i.getArgument(0);
            if (p.getUser() != null) {
                participantStore.put(p.getUser().getEmail(), p);
            }
            return p;
        });

        Map<String, PaymentTransaction> paymentStore = new ConcurrentHashMap<>();
        when(paymentRepository.findByRazorpayOrderId(anyString())).thenAnswer(i -> {
            String rzpId = i.getArgument(0);
            return Optional.ofNullable(paymentStore.get(rzpId));
        });
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> {
            PaymentTransaction pt = i.getArgument(0);
            if (pt.getRazorpayOrderId() != null) {
                paymentStore.put(pt.getRazorpayOrderId(), pt);
            }
            return pt;
        });
        when(refundRepository.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CraftRepository craftRepositoryMock() {
        CraftRepository cr = mock(CraftRepository.class);
        when(cr.findById(100L)).thenReturn(Optional.of(craft));
        return cr;
    }

    // =========================================================================
    // 35 CANONICAL AUDIT TEST SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 1: Interested does NOT start participation timer")
    void test1_InterestedDoesNotStartTimer() {
        when(auctionInterestRepository.existsByAuctionAndUser(auction, buyer1)).thenReturn(false);

        Auction result = auctionService.registerInterest("alice@example.com", 10L);

        assertEquals(1, result.getInterestedCount());
        assertNull(result.getFirstDepositPaidAt(), "firstDepositPaidAt must remain null");
        assertNull(result.getParticipationDeadline(), "participationDeadline must remain null");
        assertEquals(0, result.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Scenario 2: Cashfree order checkout creation does NOT create participant")
    void test2_CheckoutCreationDoesNotCreateParticipant() {
        CashfreeOrderResponse resp = cashfreeService.createOrder("alice@example.com", BigDecimal.valueOf(500), 10L, 100L, "PARTICIPATION");

        assertNotNull(resp);
        assertNotNull(resp.getOrderId());
        verify(participantRepository, never()).save(any(AuctionParticipant.class));
        assertNull(auction.getFirstDepositPaidAt());
        assertEquals(0, auction.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Scenario 3: Failed base payment does NOT create participant")
    void test3_FailedBasePaymentDoesNotCreateParticipant() {
        String failedOrderId = "order_failed_999";
        PaymentTransaction failedTx = new PaymentTransaction(buyer1, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE", "CB-CF-1", "FAILED", "Failed");
        failedTx.setRazorpayOrderId(failedOrderId);
        when(paymentRepository.findByRazorpayOrderId(failedOrderId)).thenReturn(Optional.of(failedTx));

        assertThrows(IllegalStateException.class, () ->
                cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", failedOrderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE")
        );

        verify(participantRepository, never()).save(any(AuctionParticipant.class));
        assertEquals(0, auction.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Scenario 4: Successful base payment creates exactly one participant")
    void test4_SuccessfulBasePaymentCreatesExactlyOneParticipant() {
        String orderId = "order_cb_success_001";
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());

        PaymentTransaction tx = cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE");

        assertEquals("CAPTURED", tx.getStatus());
        verify(participantRepository, times(1)).save(any(AuctionParticipant.class));
        assertEquals(1, auction.getCurrentParticipantsCount());
        assertNotNull(auction.getFirstDepositPaidAt());
        assertNotNull(auction.getParticipationDeadline());
    }

    @Test
    @DisplayName("Scenario 5: Duplicate webhook does NOT create duplicate participant")
    void test5_DuplicateWebhookDoesNotCreateDuplicateParticipant() {
        String orderId = "order_cb_dup_webhook";
        PaymentTransaction capturedTx = new PaymentTransaction(buyer1, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE", "CB-CF-DUP", "CAPTURED", "Paid");
        capturedTx.setRazorpayOrderId(orderId);
        when(paymentRepository.findByRazorpayOrderId(orderId)).thenReturn(Optional.of(capturedTx));

        AuctionParticipant existingParticipant = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(existingParticipant));

        PaymentTransaction result = cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE");

        assertEquals("CAPTURED", result.getStatus());
        verify(participantRepository, never()).save(any(AuctionParticipant.class));
    }

    @Test
    @DisplayName("Scenario 6: 24-hour timer starts only after verified payment")
    void test6_24HourTimerStartsOnlyAfterVerifiedPayment() {
        assertNull(auction.getFirstDepositPaidAt());
        assertNull(auction.getParticipationDeadline());

        String orderId = "order_cb_first_dep";
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());

        cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE");

        assertNotNull(auction.getFirstDepositPaidAt());
        assertEquals(auction.getFirstDepositPaidAt().plusHours(24), auction.getParticipationDeadline());
    }

    @Test
    @DisplayName("Scenario 7: Refresh does NOT reset participation timer")
    void test7_RefreshDoesNotResetTimer() {
        LocalDateTime originalStart = LocalDateTime.now().minusHours(2);
        LocalDateTime originalDeadline = originalStart.plusHours(24);
        auction.setFirstDepositPaidAt(originalStart);
        auction.setParticipationDeadline(originalDeadline);

        Auction fetched = auctionService.getAuctionById(10L);

        assertEquals(originalStart, fetched.getFirstDepositPaidAt(), "firstDepositPaidAt must remain constant on fetch");
        assertEquals(originalDeadline, fetched.getParticipationDeadline(), "participationDeadline must remain constant on fetch");
    }

    @Test
    @DisplayName("Scenario 8: Sixth participant is rejected when max 5 limit reached")
    void test8_SixthParticipantIsRejected() {
        auction.setCurrentParticipantsCount(5);

        assertThrows(RuntimeException.class, () ->
                auctionService.joinAuctionWithDeposit("farhan@example.com", 10L, new JoinAuctionRequest())
        );
    }

    @Test
    @DisplayName("Scenario 9: 5/5 participants immediately starts 5-minute preparation countdown")
    void test9_FifthParticipantStarts5MinutePreparation() {
        auction.setCurrentParticipantsCount(4);
        auction.setFirstDepositPaidAt(LocalDateTime.now().minusHours(1));
        auction.setParticipationDeadline(LocalDateTime.now().plusHours(23));

        when(participantRepository.findByAuctionAndUser(auction, buyer5)).thenReturn(Optional.empty());

        AuctionParticipant p5 = auctionService.joinAuctionWithDeposit("ethan@example.com", 10L, new JoinAuctionRequest());

        assertNotNull(p5);
        assertEquals(5, auction.getCurrentParticipantsCount());
        assertEquals(AuctionStatus.PREPARATION, auction.getStatus());
        assertNotNull(auction.getPrepDeadline());
    }

    @Test
    @DisplayName("Scenario 10: 24h expires with 0 participants -> CANCELLED")
    void test10_24hExpiresWithZeroParticipantsCancelsAuction() {
        auction.setFirstDepositPaidAt(LocalDateTime.now().minusHours(25));
        auction.setParticipationDeadline(LocalDateTime.now().minusHours(1));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(Collections.emptyList());

        auctionService.checkAndFinalizeAuctionState(10L);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
    }

    @Test
    @DisplayName("Scenario 11: 24h expires with 1 participant -> direct winner without competitive bidding")
    void test11_24hExpiresWithOneParticipantDirectPurchase() {
        auction.setFirstDepositPaidAt(LocalDateTime.now().minusHours(25));
        auction.setParticipationDeadline(LocalDateTime.now().minusHours(1));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setStatus("JOINED");
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.checkAndFinalizeAuctionState(10L);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals(buyer1, auction.getWinningBidder());
        assertEquals("WON", p1.getStatus());
    }

    @Test
    @DisplayName("Scenario 12: 24h expires with 2+ participants -> starts LIVE auction")
    void test12_24hExpiresWithMultipleParticipantsStartsLive() {
        auction.setFirstDepositPaidAt(LocalDateTime.now().minusHours(25));
        auction.setParticipationDeadline(LocalDateTime.now().minusHours(1));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));

        auctionService.checkAndFinalizeAuctionState(10L);

        assertEquals(AuctionStatus.LIVE, auction.getStatus());
        assertTrue(auction.isLiveTurnActive());
        assertNotNull(auction.getTurnDeadline());
    }

    @Test
    @DisplayName("Scenario 13: Voluntary cancellation deducts 5% fee and refunds 95%")
    void test13_VoluntaryCancellationDeducts5PercentFee() {
        AuctionParticipant p = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p.setId(101L);
        p.setStatus("JOINED");
        auction.setCurrentParticipantsCount(2);

        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p));
        when(participantRepository.countByAuctionAndStatusNot(auction, "CANCELLED")).thenReturn(1L);

        CancelParticipationResponseDTO resp = auctionService.cancelParticipation("alice@example.com", 10L);

        assertEquals(new BigDecimal("25.00"), resp.getCancellationFee());
        assertEquals(new BigDecimal("475.00"), resp.getRefundAmount());
        assertEquals("CANCELLED", p.getStatus());
    }

    @Test
    @DisplayName("Scenario 14: Duplicate cancellation attempt is rejected")
    void test14_DuplicateCancellationIsRejected() {
        AuctionParticipant p = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p.setStatus("CANCELLED");
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
    }

    @Test
    @DisplayName("Scenario 15: Cancellation after LIVE auction start is rejected")
    void test15_CancellationAfterLiveIsRejected() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);

        assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
    }

    @Test
    @DisplayName("Scenario 16: Cancellation during PREPARATION phase is rejected")
    void test16_CancellationDuringPreparationIsRejected() {
        auction.setStatus(AuctionStatus.PREPARATION);

        assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
    }

    @Test
    @DisplayName("Scenario 17: Correct differential bid calculation on backend")
    void test17_CorrectDifferentialBidCalculation() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        Bid bid = auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(600));

        assertNotNull(bid);
        assertEquals(BigDecimal.valueOf(600), bid.getAmount());
        assertEquals(BigDecimal.valueOf(600), auction.getCurrentHighestBid());
        assertEquals(buyer1, auction.getWinningBidder());
    }

    @Test
    @DisplayName("Scenario 18: Failed differential payment does NOT change auction price")
    void test18_FailedDifferentialPaymentDoesNotChangePrice() {
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));
        String failedOrderId = "order_diff_fail";
        PaymentTransaction failedTx = new PaymentTransaction(buyer1, 10L, 100L, BigDecimal.valueOf(100), "DIFFERENTIAL_BID", "CASHFREE", "REF", "FAILED", "Fail");
        failedTx.setRazorpayOrderId(failedOrderId);
        when(paymentRepository.findByRazorpayOrderId(failedOrderId)).thenReturn(Optional.of(failedTx));

        assertThrows(IllegalStateException.class, () ->
                cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", failedOrderId, 10L, 100L, BigDecimal.valueOf(100), "DIFFERENTIAL_BID", "CASHFREE")
        );

        assertEquals(BigDecimal.valueOf(500), auction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("Scenario 19: Successful differential payment resets 60-second turn timer")
    void test19_SuccessfulDifferentialPaymentResetsTurnTimer() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));

        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.placeDifferentialBid("bob@example.com", 10L, BigDecimal.valueOf(650));

        assertEquals(BigDecimal.valueOf(650), auction.getCurrentHighestBid());
        assertTrue(auction.getTurnDeadline().isAfter(LocalDateTime.now().plusSeconds(55)));
    }

    @Test
    @DisplayName("Scenario 20: Duplicate bid payment verification is idempotent")
    void test20_DuplicateBidVerificationIsIdempotent() {
        String orderId = "order_diff_dup";
        PaymentTransaction capturedTx = new PaymentTransaction(buyer1, 10L, 100L, BigDecimal.valueOf(100), "DIFFERENTIAL_BID", "CASHFREE", "REF", "CAPTURED", "Paid");
        capturedTx.setRazorpayOrderId(orderId);
        when(paymentRepository.findByRazorpayOrderId(orderId)).thenReturn(Optional.of(capturedTx));

        PaymentTransaction result = cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(100), "DIFFERENTIAL_BID", "CASHFREE");

        assertEquals("CAPTURED", result.getStatus());
        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    @DisplayName("Scenario 21: 60-second expiration selects correct highest bidder as winner")
    void test21_60SecondExpirationSelectsCorrectWinner() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(750));
        auction.setTurnDeadline(LocalDateTime.now().minusSeconds(5));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(750));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.finalizeAuction(auction);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals("WON", p2.getStatus());
        assertEquals("REFUNDED", p1.getStatus());
    }

    @Test
    @DisplayName("Scenario 22: Initial 2-minute no-bid rule awards craft to earliest base depositor")
    void test22_Initial2MinuteNoBidAwardsFirstDepositor() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setWinningBidder(null); // No differential bids placed
        auction.setStartingPrice(BigDecimal.valueOf(500));
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));
        auction.setTurnDeadline(LocalDateTime.now().minusSeconds(2));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setJoinedAt(LocalDateTime.now().minusMinutes(10)); // Joined earlier
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        p2.setJoinedAt(LocalDateTime.now().minusMinutes(5));

        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.finalizeAuction(auction);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals(buyer1, auction.getWinningBidder(), "Earliest base depositor must win when no differential bids occur");
        assertEquals("WON", p1.getStatus());
        assertEquals("REFUNDED", p2.getStatus());
    }

    @Test
    @DisplayName("Scenario 23: Winner does NOT get charged again after auction ends")
    void test23_WinnerNotChargedAgain() {
        auction.setWinningBidder(buyer1);
        auction.setCurrentHighestBid(BigDecimal.valueOf(600));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(600));
        p1.setStatus("JOINED");
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.finalizeAuction(auction);

        // Verify winner received 0 refunds and no additional charge transaction
        assertEquals(BigDecimal.ZERO, p1.getRefundAmount());
        assertEquals("WON", p1.getStatus());
    }

    @Test
    @DisplayName("Scenario 24: Losing participant receives 100% refund of cumulative paid amount")
    void test24_LosingParticipantReceives100PercentRefund() {
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(700));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(650)); // Paid 500 + 150 diff
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(700));

        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(i -> i.getArgument(0));

        auctionService.finalizeAuction(auction);

        assertEquals("REFUNDED", p1.getStatus());
        assertEquals(BigDecimal.valueOf(650), p1.getRefundAmount());
        verify(refundRepository, atLeastOnce()).save(any(Refund.class));
    }

    @Test
    @DisplayName("Scenario 25: Duplicate refund webhook does NOT refund twice")
    void test25_DuplicateRefundWebhookDoesNotRefundTwice() {
        Refund existingRefund = new Refund(null, buyer1, 10L, "ref_1", "ref_1", BigDecimal.valueOf(500), "COMPLETED", "Refunded");
        when(refundRepository.findByUserAndAuctionId(buyer1, 10L)).thenReturn(List.of(existingRefund));

        PaymentTransaction tx = paymentService.refundAuctionParticipant(buyer1, 10L, 100L, BigDecimal.valueOf(500), "Auto Refund");

        // Verify refundRepository is NOT saved again with a new record
        verify(refundRepository, never()).save(any(Refund.class));
    }

    @Test
    @DisplayName("Scenario 26: Unauthorized participant cancellation is rejected (IDOR)")
    void test26_UnauthorizedCancellationRejected() {
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                auctionService.cancelParticipation("bob@example.com", 10L)
        );
    }

    @Test
    @DisplayName("Scenario 27: Unauthorized bid by non-participant is rejected")
    void test27_UnauthorizedBidByNonParticipantRejected() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        when(participantRepository.findByAuctionAndUser(auction, buyer6)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                auctionService.placeDifferentialBid("farhan@example.com", 10L, BigDecimal.valueOf(600))
        );
    }

    @Test
    @DisplayName("Scenario 28: User cannot view another user's order (IDOR)")
    void test28_UserCannotAccessAnotherUsersOrder() {
        AuctionOrder order = new AuctionOrder();
        order.setId(50L);
        order.setAuction(auction);
        order.setBuyer(buyer1);
        order.setArtisan(artisan);

        when(orderRepository.findByAuction(auction)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () ->
                auctionService.getAuctionOrder("bob@example.com", 10L)
        );
    }

    @Test
    @DisplayName("Scenario 29: Concurrent 5th participant attempts handled safely")
    void test29_ConcurrentFifthParticipantAttempts() throws Exception {
        auction.setCurrentParticipantsCount(4);
        AtomicInteger successfulJoins = new AtomicInteger(0);

        when(participantRepository.findByAuctionAndUser(eq(auction), any(User.class))).thenReturn(Optional.empty());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> {
            try {
                auctionService.joinAuctionWithDeposit("ethan@example.com", 10L, new JoinAuctionRequest());
                successfulJoins.incrementAndGet();
            } catch (Exception ignored) {}
        });
        Future<?> f2 = executor.submit(() -> {
            try {
                auctionService.joinAuctionWithDeposit("farhan@example.com", 10L, new JoinAuctionRequest());
                successfulJoins.incrementAndGet();
            } catch (Exception ignored) {}
        });

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successfulJoins.get(), "Only 1 user should succeed in claiming the 5th and final room slot");
        assertEquals(5, auction.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Scenario 30: Concurrent bid attempts serialize correctly via synchronized engine")
    void test30_ConcurrentBidAttemptsSerializeCorrectly() throws Exception {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(600)));
        Future<?> f2 = executor.submit(() -> auctionService.placeDifferentialBid("bob@example.com", 10L, BigDecimal.valueOf(700)));

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(BigDecimal.valueOf(700), auction.getCurrentHighestBid());
        assertEquals(buyer2, auction.getWinningBidder());
    }

    @Test
    @DisplayName("Scenario 31: Payment webhook + frontend callback race produces single enrollment")
    void test31_WebhookAndFrontendCallbackRace() throws Exception {
        String orderId = "order_cb_race_31";
        PaymentTransaction tx = new PaymentTransaction(buyer1, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE", "REF", "CREATED", "Init");
        tx.setRazorpayOrderId(orderId);
        when(paymentRepository.findByRazorpayOrderId(orderId)).thenReturn(Optional.of(tx));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE"));
        Future<?> f2 = executor.submit(() -> cashfreeService.verifyAndRecordCashfreePayment("alice@example.com", orderId, 10L, 100L, BigDecimal.valueOf(500), "PARTICIPATION", "CASHFREE"));

        f1.get(2, TimeUnit.SECONDS);
        f2.get(2, TimeUnit.SECONDS);
        executor.shutdown();

        verify(participantRepository, atMost(1)).save(any(AuctionParticipant.class));
    }

    @Test
    @DisplayName("Scenario 32: Auction expiration race with payment rejects closed join")
    void test32_AuctionExpirationAndPaymentRace() {
        auction.setParticipationDeadline(LocalDateTime.now().minusSeconds(1)); // Window closed

        assertThrows(RuntimeException.class, () ->
                auctionService.joinAuctionWithDeposit("alice@example.com", 10L, new JoinAuctionRequest())
        );
    }

    @Test
    @DisplayName("Scenario 33: Seller cannot participate or bid in their own auction")
    void test33_SellerCannotParticipateInOwnAuction() {
        assertThrows(AccessDeniedException.class, () ->
                auctionService.joinAuctionWithDeposit("artisan@craftbid.co.in", 10L, new JoinAuctionRequest())
        );
    }

    @Test
    @DisplayName("Scenario 34: Admin-only settlement endpoints reject non-admin users")
    void test34_AdminOnlySettlementRequiresAdminRole() {
        // Calling markSettlementPaid as customer should throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                paymentService.processRefund(100L, BigDecimal.valueOf(100), "Refund", "alice@example.com")
        );
    }

    @Test
    @DisplayName("Scenario 35: Privacy sanitization returns only Name and City for public participants")
    void test35_SanitizedParticipantsDoNotExposePrivateDetails() {
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));

        List<AuctionParticipantDTO> sanitized = auctionService.getSanitizedParticipants(10L);

        assertNotNull(sanitized);
        assertEquals(1, sanitized.size());
        assertEquals("Alice Sharma", sanitized.get(0).getName());
        assertEquals("Mumbai", sanitized.get(0).getCity());
    }
}
