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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionEngineTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private CraftRepository craftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuctionParticipantRepository participantRepository;
    @Mock
    private AuctionOrderRepository orderRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuctionEventPublisher eventPublisher;
    @Mock
    private RazorpayService razorpayService;
    @Mock
    private OrderService orderService;
    @Mock
    private AuctionInterestRepository auctionInterestRepository;

    private AuctionService auctionService;

    private User artisan;
    private User buyer1;
    private User buyer2;
    private User buyer3;
    private User buyer4;
    private User buyer5;
    private Craft craft;
    private Auction auction;

    @BeforeEach
    void setUp() {
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
                razorpayService,
                orderService,
                auctionInterestRepository
        );

        artisan = new User();
        artisan.setId(1L);
        artisan.setName("Master Artisan");
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
    }

    // =========================================================================
    // 20 CANONICAL BUSINESS & WORKFLOW TEST CASES
    // =========================================================================

    @Test
    @DisplayName("Test 1: Artisan creates auction with max 5 participants and NULL participation deadline")
    void test1_ArtisanCreatesAuction() {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setCraftId(100L);
        req.setStartingPrice(BigDecimal.valueOf(500));
        req.setDurationHours(24);

        when(userRepository.findByIdentifier("artisan@craftbid.co.in")).thenReturn(Optional.of(artisan));
        when(craftRepository.findById(100L)).thenReturn(Optional.of(craft));
        when(auctionRepository.findByCraftAndStatus(craft, AuctionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        Auction created = auctionService.createAuction("artisan@craftbid.co.in", req);

        assertNotNull(created);
        assertEquals(BigDecimal.valueOf(500), created.getStartingPrice());
        assertEquals(5, created.getMaxParticipants());
        assertNull(created.getFirstDepositPaidAt());
        assertNull(created.getParticipationDeadline());
        assertEquals(0, created.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Test 2: Registering interest saves record without creating participant or starting timer")
    void test2_RegisterInterestWithoutStartingTimer() {
        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(auctionInterestRepository.existsByAuctionAndUser(auction, buyer1)).thenReturn(false);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        Auction updated = auctionService.registerInterest("alice@example.com", 10L);

        assertEquals(1, updated.getInterestedCount());
        assertNull(updated.getFirstDepositPaidAt());
        assertNull(updated.getParticipationDeadline());
        assertEquals(0, updated.getCurrentParticipantsCount());
        verify(auctionInterestRepository, times(1)).save(any(AuctionInterest.class));
    }

    @Test
    @DisplayName("Test 3: First base deposit starts 24h participation window and alerts interested users")
    void test3_FirstDepositStarts24hWindowAndAlertsInterestedUsers() {
        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionInterest interest = new AuctionInterest(auction, buyer2);
        when(auctionInterestRepository.findByAuction(auction)).thenReturn(List.of(interest));

        JoinAuctionRequest joinReq = new JoinAuctionRequest();
        joinReq.setPaymentMethod("UPI");

        AuctionParticipant p = auctionService.joinAuctionWithDeposit("alice@example.com", 10L, joinReq);

        assertNotNull(p);
        assertEquals(BigDecimal.valueOf(500), p.getBasePricePaid());
        assertEquals(1, auction.getCurrentParticipantsCount());
        assertNotNull(auction.getFirstDepositPaidAt());
        assertNotNull(auction.getParticipationDeadline());

        verify(notificationService, times(1)).notifyInterestedUsers(eq(auction), anyList());
        verify(notificationService, times(1)).notifyAuctionJoined(eq(buyer1), eq("Handmade Terracotta Vase"), eq(BigDecimal.valueOf(500)), eq(10L));
    }

    @Test
    @DisplayName("Test 4: Customers 2 to 4 join within 24h window without modifying original timer")
    void test4_Customers2To4JoinWithoutRestartingTimer() {
        LocalDateTime originalStart = LocalDateTime.now().minusHours(2);
        LocalDateTime originalDeadline = originalStart.plusHours(24);
        auction.setFirstDepositPaidAt(originalStart);
        auction.setParticipationDeadline(originalDeadline);
        auction.setCurrentParticipantsCount(1);

        when(userRepository.findByIdentifier("bob@example.com")).thenReturn(Optional.of(buyer2));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.empty());
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionParticipant p2 = auctionService.joinAuctionWithDeposit("bob@example.com", 10L, new JoinAuctionRequest());

        assertNotNull(p2);
        assertEquals(2, auction.getCurrentParticipantsCount());
        assertEquals(originalStart, auction.getFirstDepositPaidAt(), "firstDepositPaidAt must not change");
        assertEquals(originalDeadline, auction.getParticipationDeadline(), "participationDeadline must not change");
    }

    @Test
    @DisplayName("Test 5: 5th participant joining triggers 5-minute preparation countdown immediately")
    void test5_FifthParticipantJoinsStarts5MinutePreparation() {
        auction.setCurrentParticipantsCount(4);
        auction.setFirstDepositPaidAt(LocalDateTime.now().minusHours(2));
        auction.setParticipationDeadline(LocalDateTime.now().plusHours(22));

        when(userRepository.findByIdentifier("ethan@example.com")).thenReturn(Optional.of(buyer5));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer5)).thenReturn(Optional.empty());
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionParticipant p5 = auctionService.joinAuctionWithDeposit("ethan@example.com", 10L, new JoinAuctionRequest());

        assertNotNull(p5);
        assertEquals(5, auction.getCurrentParticipantsCount());
        assertEquals(AuctionStatus.PREPARATION, auction.getStatus());
        assertNotNull(auction.getPrepDeadline());
        assertTrue(auction.getPrepDeadline().isAfter(LocalDateTime.now().plusMinutes(4)));
    }

    @Test
    @DisplayName("Test 6: 5-minute preparation expiry transitions auction to LIVE bidding")
    void test6_FiveMinutePreparationExpiryStartsLiveAuction() {
        auction.setStatus(AuctionStatus.PREPARATION);
        auction.setPrepDeadline(LocalDateTime.now().minusSeconds(1)); // Expired prep timer
        auction.setCurrentParticipantsCount(5);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.LIVE, auction.getStatus());
        assertTrue(auction.isLiveTurnActive());
        assertNotNull(auction.getTurnDeadline());
        assertNotNull(auction.getInitialWaitDeadline());
    }

    @Test
    @DisplayName("Test 7: 24h participation window expiry with 0 participants marks CANCELLED")
    void test7_TwentyFourHourExpiryZeroParticipantsCancels() {
        auction.setCurrentParticipantsCount(0);
        auction.setParticipationDeadline(LocalDateTime.now().minusMinutes(5));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
    }

    @Test
    @DisplayName("Test 8: 24h expiry with 1 participant awards direct win at base price with zero shipping")
    void test8_TwentyFourHourExpirySingleParticipantDirectWin() {
        auction.setCurrentParticipantsCount(1);
        auction.setParticipationDeadline(LocalDateTime.now().minusMinutes(5));
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals(buyer1, auction.getWinningBidder());
        assertEquals("WON", p1.getStatus());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), auction.getAdminFeeAmount());
        assertEquals(BigDecimal.valueOf(450.00).setScale(2), auction.getArtisanPayoutAmount());

        verify(orderService, times(1)).createWinnerOrder(eq(auction), eq(buyer1), eq(BigDecimal.valueOf(500)), any(), any());
        verify(notificationService, times(1)).notifyAuctionWon(eq(buyer1), eq("Handmade Terracotta Vase"), eq(BigDecimal.valueOf(500)), eq(10L));
    }

    @Test
    @DisplayName("Test 9: 24h expiry with 2 to 4 participants starts LIVE auction")
    void test9_TwentyFourHourExpiryTwoToFourParticipantsStartsLive() {
        auction.setCurrentParticipantsCount(3);
        auction.setParticipationDeadline(LocalDateTime.now().minusMinutes(5));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        AuctionParticipant p3 = new AuctionParticipant(auction, buyer3, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2, p3));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.LIVE, auction.getStatus());
        assertTrue(auction.isLiveTurnActive());
        assertNotNull(auction.getTurnDeadline());
        verify(notificationService, times(3)).notifyAuctionLiveStarted(any(User.class), anyString(), eq(10L));
    }

    @Test
    @DisplayName("Test 10: Live auction starting state has current price equal to base price")
    void test10_LiveAuctionStartingState() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setCurrentHighestBid(BigDecimal.valueOf(500));
        auction.setStartingPrice(BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(500), auction.getCurrentHighestBid());
        assertEquals(BigDecimal.valueOf(500), auction.getStartingPrice());
    }

    @Test
    @DisplayName("Test 11: Differential bid payment calculates exact diff = newBid - totalAmountPaid")
    void test11_DifferentialBidCalculation() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setTurnDeadline(LocalDateTime.now().plusSeconds(40));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500)); // Committed ₹500

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Alice bids ₹650 (Delta to pay = ₹650 - ₹500 = ₹150)
        Bid bid = auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(650));

        assertNotNull(bid);
        assertEquals(BigDecimal.valueOf(650), bid.getAmount());
        assertEquals(BigDecimal.valueOf(650), p1.getTotalAmountPaid());
        assertEquals(BigDecimal.valueOf(650), auction.getCurrentHighestBid());
        assertEquals(buyer1, auction.getWinningBidder());

        verify(paymentService, times(1)).recordTransaction(
                eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(150)), eq("DIFFERENTIAL_BID"), eq("UPI"), anyString()
        );
    }

    @Test
    @DisplayName("Test 12: Differential bid rejects bid below minIncrement")
    void test12_DifferentialBidRejectsBelowMinIncrement() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setCurrentHighestBid(BigDecimal.valueOf(600));
        auction.setMinBidIncrement(BigDecimal.valueOf(50));
        auction.setTotalBids(1);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));

        // Required min is ₹650; bidding ₹620 must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(620))
        );
    }

    @Test
    @DisplayName("Test 13: Artisan cannot bid in own auction")
    void test13_ArtisanCannotBidInOwnAuction() {
        when(userRepository.findByIdentifier("artisan@craftbid.co.in")).thenReturn(Optional.of(artisan));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        assertThrows(AccessDeniedException.class, () ->
                auctionService.placeDifferentialBid("artisan@craftbid.co.in", 10L, BigDecimal.valueOf(600))
        );
    }

    @Test
    @DisplayName("Test 14: Valid differential bid resets 60s turn deadline")
    void test14_ValidBidResetsTurnDeadline() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setTurnDeadline(LocalDateTime.now().plusSeconds(10)); // 10s left

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(600));

        assertTrue(auction.getTurnDeadline().isAfter(LocalDateTime.now().plusSeconds(55)), "Turn timer must reset to ~60s");
    }

    @Test
    @DisplayName("Test 15: 60s turn timer expiry finalizes auction and declares highest bidder winner")
    void test15_TurnTimerExpiryFinalizesAuction() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setTurnDeadline(LocalDateTime.now().minusSeconds(5)); // Expired
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(800));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(800));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction mockRefundTx = new PaymentTransaction();
        mockRefundTx.setTransactionRef("CB-REF-LOSER");
        when(paymentService.refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(500)), anyString()))
                .thenReturn(mockRefundTx);

        Auction finalized = auctionService.checkAndFinalizeAuctionState(10L);

        assertEquals(AuctionStatus.ENDED, finalized.getStatus());
        assertFalse(finalized.isLiveTurnActive());
        assertEquals(buyer2, finalized.getWinningBidder());
    }

    @Test
    @DisplayName("Test 16: Winner is never charged again and receives zero shipping order")
    void test16_WinnerNeverChargedAgain() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(850));

        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(850));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.finalizeAuction(auction);

        // Winner status set to WON
        assertEquals("WON", p2.getStatus());
        verify(orderService, times(1)).createWinnerOrder(eq(auction), eq(buyer2), eq(BigDecimal.valueOf(850)), any(), any());
        // No refund for winner
        verify(paymentService, never()).refundAuctionParticipant(eq(buyer2), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Test 17: All non-winning participants receive 100% cumulative refund")
    void test17_NonWinnersReceive100PercentRefund() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(900));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(700)); // Alice paid base + differential = ₹700

        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        p2.setTotalAmountPaid(BigDecimal.valueOf(900)); // Bob won at ₹900

        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-ALICE");
        when(paymentService.refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(700)), anyString()))
                .thenReturn(refundTx);

        auctionService.finalizeAuction(auction);

        assertEquals("REFUNDED", p1.getStatus());
        assertEquals(BigDecimal.valueOf(700), p1.getRefundAmount());
        verify(paymentService, times(1)).refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(700)), anyString());
        verify(notificationService, times(1)).notifyRefundProcessed(eq(buyer1), anyString(), eq(BigDecimal.valueOf(700)), eq("CB-REF-ALICE"), eq(10L));
    }

    @Test
    @DisplayName("Test 18: Winner submits delivery address snapshots address and updates status to PAID")
    void test18_SubmitDeliveryAddress() {
        auction.setStatus(AuctionStatus.ENDED);
        auction.setWinningBidder(buyer1);

        AuctionOrder order = new AuctionOrder();
        order.setId(55L);
        order.setAuction(auction);
        order.setBuyer(buyer1);
        order.setArtisan(artisan);

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAddressRequest req = new SubmitAddressRequest();
        req.setFullName("Alice Sharma");
        req.setStreetAddress("123 Marine Lines");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPincode("400001");
        req.setPhone("9876543210");

        AuctionOrder updated = auctionService.submitDeliveryAddress("alice@example.com", 10L, req);

        assertEquals("PAID", updated.getStatus());
        assertEquals("123 Marine Lines", updated.getStreetAddress());
        assertEquals("400001", updated.getPincode());
    }

    @Test
    @DisplayName("Test 19: Non-winner cannot submit delivery address")
    void test19_NonWinnerCannotSubmitAddress() {
        auction.setStatus(AuctionStatus.ENDED);
        auction.setWinningBidder(buyer1);

        when(userRepository.findByIdentifier("bob@example.com")).thenReturn(Optional.of(buyer2));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        SubmitAddressRequest req = new SubmitAddressRequest();
        req.setFullName("Bob Verma");

        assertThrows(AccessDeniedException.class, () ->
                auctionService.submitDeliveryAddress("bob@example.com", 10L, req)
        );
    }

    @Test
    @DisplayName("Test 20: Cancelling active auction issues 100% full refund to all enrolled participants")
    void test20_CancelAuctionRefundsAllParticipants() {
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(650));

        when(userRepository.findByIdentifier("artisan@craftbid.co.in")).thenReturn(Optional.of(artisan));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-CANCEL");
        when(paymentService.refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(650)), anyString()))
                .thenReturn(refundTx);

        Auction cancelled = auctionService.cancelAuction("artisan@craftbid.co.in", 10L);

        assertEquals(AuctionStatus.CANCELLED, cancelled.getStatus());
        assertEquals("REFUNDED", p1.getStatus());
        assertEquals(BigDecimal.valueOf(650), p1.getRefundAmount());
        verify(paymentService, times(1)).refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(650)), anyString());
    }

    // =========================================================================
    // CUSTOMER VOLUNTARY CANCELLATION TEST CASES (TESTS 21 - 33)
    // =========================================================================

    @Test
    @DisplayName("Test 21: ₹500 participant cancels -> 5% fee (₹25), refund (₹475)")
    void test21_CancellationFeeAndRefund_500Paid() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(1);
        auction.setFirstDepositPaidAt(LocalDateTime.now());
        auction.setParticipationDeadline(LocalDateTime.now().plusHours(24));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.countByAuctionAndStatusNot(auction, "CANCELLED")).thenReturn(0L);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-475");
        when(paymentService.refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(475.00).setScale(2)), anyString()))
                .thenReturn(refundTx);

        CancelParticipationResponseDTO response = auctionService.cancelParticipation("alice@example.com", 10L);

        assertEquals(BigDecimal.valueOf(500), response.getTotalAmountPaid());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), response.getCancellationFee());
        assertEquals(BigDecimal.valueOf(475.00).setScale(2), response.getRefundAmount());
        assertEquals("CANCELLED", response.getStatus());
        assertEquals("CANCELLED", p1.getStatus());
        assertEquals(BigDecimal.valueOf(475.00).setScale(2), p1.getCancellationRefundAmount());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), p1.getCancellationFee());
        verify(paymentService, times(1)).refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(475.00).setScale(2)), anyString());
    }

    @Test
    @DisplayName("Test 22: ₹600 participant cancels -> 5% fee (₹30), refund (₹570)")
    void test22_CancellationFeeAndRefund_600Paid() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(2);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(600));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.countByAuctionAndStatusNot(auction, "CANCELLED")).thenReturn(1L);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-570");
        when(paymentService.refundAuctionParticipant(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(570.00).setScale(2)), anyString()))
                .thenReturn(refundTx);

        CancelParticipationResponseDTO response = auctionService.cancelParticipation("alice@example.com", 10L);

        assertEquals(BigDecimal.valueOf(600), response.getTotalAmountPaid());
        assertEquals(BigDecimal.valueOf(30.00).setScale(2), response.getCancellationFee());
        assertEquals(BigDecimal.valueOf(570.00).setScale(2), response.getRefundAmount());
        assertEquals(1, response.getCurrentParticipantsCount());
    }

    @Test
    @DisplayName("Test 23: First participant cancels -> count becomes 0, 24h window cleared, next deposit starts NEW 24h window")
    void test23_FirstParticipantCancels_Clears24HourWindow_NextStartsNewWindow() {
        auction.setStatus(AuctionStatus.ACTIVE);
        LocalDateTime firstDepositTime = LocalDateTime.now().minusHours(2);
        auction.setFirstDepositPaidAt(firstDepositTime);
        auction.setParticipationDeadline(firstDepositTime.plusHours(24));
        auction.setCurrentParticipantsCount(1);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.countByAuctionAndStatusNot(auction, "CANCELLED")).thenReturn(0L);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-RESET");
        when(paymentService.refundAuctionParticipant(any(), anyLong(), anyLong(), any(), any())).thenReturn(refundTx);

        CancelParticipationResponseDTO response = auctionService.cancelParticipation("alice@example.com", 10L);

        assertEquals(0, response.getCurrentParticipantsCount());
        assertNull(auction.getFirstDepositPaidAt());
        assertNull(auction.getParticipationDeadline());
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());

        // Now buyer2 joins as the new first participant -> starts a brand NEW 24h window
        when(userRepository.findByIdentifier("bob@example.com")).thenReturn(Optional.of(buyer2));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.empty());
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionParticipant newP2 = auctionService.joinAuctionWithDeposit("bob@example.com", 10L, new JoinAuctionRequest());

        assertNotNull(newP2);
        assertNotNull(auction.getFirstDepositPaidAt());
        assertNotNull(auction.getParticipationDeadline());
        assertEquals(1, auction.getCurrentParticipantsCount());
        // Verify deadline is set from now (new window)
        assertTrue(auction.getParticipationDeadline().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    @DisplayName("Test 24: 2 participants exist and one cancels -> remaining stays active, original 24h deadline does NOT restart")
    void test24_MultipleParticipants_OneCancels_PreservesOriginal24HourDeadline() {
        auction.setStatus(AuctionStatus.ACTIVE);
        LocalDateTime originalStart = LocalDateTime.now().minusHours(3);
        LocalDateTime originalDeadline = originalStart.plusHours(24);
        auction.setFirstDepositPaidAt(originalStart);
        auction.setParticipationDeadline(originalDeadline);
        auction.setCurrentParticipantsCount(2);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(participantRepository.countByAuctionAndStatusNot(auction, "CANCELLED")).thenReturn(1L);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setTransactionRef("CB-REF-PARTIAL");
        when(paymentService.refundAuctionParticipant(any(), anyLong(), anyLong(), any(), any())).thenReturn(refundTx);

        CancelParticipationResponseDTO response = auctionService.cancelParticipation("alice@example.com", 10L);

        assertEquals(1, response.getCurrentParticipantsCount());
        assertEquals(originalStart, auction.getFirstDepositPaidAt());
        assertEquals(originalDeadline, auction.getParticipationDeadline());
    }

    @Test
    @DisplayName("Test 25: 5/5 reached -> cancellation disabled")
    void test25_CancellationDisabled_WhenFiveParticipantsReached() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(5);
        auction.setMaxParticipants(5);

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("5/5"));
    }

    @Test
    @DisplayName("Test 26: 5-minute preparation started -> cancellation disabled")
    void test26_CancellationDisabled_WhenPreparationStarted() {
        auction.setStatus(AuctionStatus.PREPARATION);
        auction.setPrepDeadline(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("preparation phase"));
    }

    @Test
    @DisplayName("Test 27: LIVE auction started -> cancellation disabled")
    void test27_CancellationDisabled_WhenLiveAuctionStarted() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("live auction"));
    }

    @Test
    @DisplayName("Test 28: Auction ended -> cancellation disabled")
    void test28_CancellationDisabled_WhenAuctionEnded() {
        auction.setStatus(AuctionStatus.ENDED);

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("ended"));
    }

    @Test
    @DisplayName("Test 29: Winner -> normal cancellation disabled")
    void test29_CancellationDisabled_ForWinner() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(2);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setStatus("WON");

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("Winning participants"));
    }

    @Test
    @DisplayName("Test 30: Duplicate cancellation request -> idempotent, throws already cancelled")
    void test30_DuplicateCancellationRequest_Idempotent() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(1);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setStatus("CANCELLED");

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );
        assertTrue(ex.getMessage().contains("already been cancelled"));
        verify(paymentService, never()).refundAuctionParticipant(any(), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Test 31: Duplicate Cashfree refund / already refunded participant skipped in finalization")
    void test31_CancelledParticipantSkippedInFinalizationRefund() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(800));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setStatus("CANCELLED"); // Voluntarily cancelled earlier
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(800));
        p2.setStatus("ACTIVE");

        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.finalizeAuction(auction);

        // p1 is CANCELLED -> verify paymentService.refundAuctionParticipant is NEVER called for p1
        verify(paymentService, never()).refundAuctionParticipant(eq(buyer1), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Test 32: Customer A tries to cancel Customer B -> 403 Forbidden")
    void test32_CustomerATriesToCancelCustomerB_Forbidden() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(2);

        // Buyer2 tries to cancel Buyer1's record
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("bob@example.com")).thenReturn(Optional.of(buyer2));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p1)); // Mismatched user

        assertThrows(AccessDeniedException.class, () ->
                auctionService.cancelParticipation("bob@example.com", 10L)
        );
        verify(paymentService, never()).refundAuctionParticipant(any(), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Test 33: Failed Cashfree refund -> participant marked REFUND_FAILED and recoverable")
    void test33_FailedCashfreeRefund_MarksRefundFailed_Recoverable() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentParticipantsCount(2);

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(paymentService.refundAuctionParticipant(any(), anyLong(), anyLong(), any(), any()))
                .thenThrow(new RuntimeException("Cashfree Gateway Gateway Timeout 504"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auctionService.cancelParticipation("alice@example.com", 10L)
        );

        assertTrue(ex.getMessage().contains("Refund processing failed"));
        assertEquals("REFUND_FAILED", p1.getStatus());
        assertEquals("REFUND_FAILED", p1.getCancellationStatus());
    }
}
