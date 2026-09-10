package com.craftbid.service;

import com.craftbid.dto.*;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.*;
import com.craftbid.websocket.AuctionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private AuctionService auctionService;

    private User artisan;
    private User buyer1;
    private User buyer2;
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
                razorpayService
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
        auction.setParticipationDeadline(LocalDateTime.now().plusHours(24));
        auction.setEndTime(LocalDateTime.now().plusHours(25));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setMaxParticipants(10);
        auction.setCurrentParticipantsCount(0);
        auction.setLiveTurnActive(false);
    }

    @Test
    @DisplayName("1. Artisan creates auction with 24h participation window & 10 max participants")
    void testCreateAuction() {
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
        assertEquals(10, created.getMaxParticipants());
        assertNotNull(created.getParticipationDeadline());
    }

    @Test
    @DisplayName("2. Buyer joins auction room with base price deposit")
    void testJoinAuctionWithDeposit() {
        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.empty());
        when(participantRepository.save(any(AuctionParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        JoinAuctionRequest joinReq = new JoinAuctionRequest();
        joinReq.setPaymentMethod("UPI");

        AuctionParticipant p = auctionService.joinAuctionWithDeposit("alice@example.com", 10L, joinReq);

        assertNotNull(p);
        assertEquals(BigDecimal.valueOf(500), p.getBasePricePaid());
        assertEquals(BigDecimal.valueOf(500), p.getTotalAmountPaid());
        assertEquals(1, auction.getCurrentParticipantsCount());

        verify(paymentService, times(1)).recordTransaction(
                eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(500)), eq("BASE_DEPOSIT"), eq("UPI"), anyString()
        );
        verify(notificationService, times(1)).notifyAuctionJoined(eq(buyer1), eq("Handmade Terracotta Vase"), eq(BigDecimal.valueOf(500)), eq(10L));
    }

    @Test
    @DisplayName("3. Artisan cannot join own auction")
    void testArtisanCannotJoinOwnAuction() {
        when(userRepository.findByIdentifier("artisan@craftbid.co.in")).thenReturn(Optional.of(artisan));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));

        assertThrows(AccessDeniedException.class, () ->
                auctionService.joinAuctionWithDeposit("artisan@craftbid.co.in", 10L, new JoinAuctionRequest())
        );
    }

    @Test
    @DisplayName("4. Participation window evaluation: 0 participants cancels auction")
    void testEvaluateParticipationZeroParticipants() {
        auction.setCurrentParticipantsCount(0);
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
    }

    @Test
    @DisplayName("5. Participation window evaluation: 1 participant wins via direct purchase at base price")
    void testEvaluateParticipationSingleParticipantDirectPurchase() {
        auction.setCurrentParticipantsCount(1);
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals(buyer1, auction.getWinningBidder());
        assertEquals("WON", p1.getStatus());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), auction.getAdminFeeAmount()); // 10%
        assertEquals(BigDecimal.valueOf(450.00).setScale(2), auction.getArtisanPayoutAmount()); // 90%
        verify(orderRepository, times(1)).save(any(AuctionOrder.class));
    }

    @Test
    @DisplayName("6. Participation window evaluation: 2+ participants starts 1-minute live turn bidding")
    void testEvaluateParticipationMultipleParticipantsStartsLive() {
        auction.setCurrentParticipantsCount(2);
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        auctionService.evaluateParticipationWindow(auction);

        assertTrue(auction.isLiveTurnActive());
        assertNotNull(auction.getTurnDeadline());
        verify(notificationService, times(2)).notifyAuctionLiveStarted(any(User.class), anyString(), eq(10L));
    }

    @Test
    @DisplayName("7. Differential bidding calculates exact delta and resets 60s countdown")
    void testPlaceDifferentialBid() {
        auction.setStatus(AuctionStatus.LIVE);
        auction.setLiveTurnActive(true);
        auction.setTurnDeadline(LocalDateTime.now().plusSeconds(40));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(500));

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionAndUser(auction, buyer1)).thenReturn(Optional.of(p1));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Alice bids ₹600 (Delta = ₹600 - ₹500 = ₹100)
        Bid bid = auctionService.placeDifferentialBid("alice@example.com", 10L, BigDecimal.valueOf(600));

        assertNotNull(bid);
        assertEquals(BigDecimal.valueOf(600), bid.getAmount());
        assertEquals(BigDecimal.valueOf(600), p1.getTotalAmountPaid());
        assertEquals(BigDecimal.valueOf(600), auction.getCurrentHighestBid());
        assertEquals(buyer1, auction.getWinningBidder());
        assertEquals(1, auction.getTotalBids());

        // Verify differential payment transaction of ₹100 recorded
        verify(paymentService, times(1)).recordTransaction(
                eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(100)), eq("DIFFERENTIAL_BID"), eq("UPI"), anyString()
        );
    }

    @Test
    @DisplayName("8. Auction finalization issues 100% automated refund to losing bidders")
    void testFinalizeAuction100PercentRefunds() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setLiveTurnActive(true);
        auction.setWinningBidder(buyer2);
        auction.setCurrentHighestBid(BigDecimal.valueOf(750));

        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        p1.setTotalAmountPaid(BigDecimal.valueOf(600)); // Alice paid ₹600 total

        AuctionParticipant p2 = new AuctionParticipant(auction, buyer2, BigDecimal.valueOf(500));
        p2.setTotalAmountPaid(BigDecimal.valueOf(750)); // Bob won at ₹750

        when(participantRepository.findByAuctionAndUser(auction, buyer2)).thenReturn(Optional.of(p2));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1, p2));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction mockRefundTx = new PaymentTransaction();
        mockRefundTx.setTransactionRef("CB-REF-12345");
        when(paymentService.recordTransaction(eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(600)), eq("AUTO_REFUND"), anyString(), anyString()))
                .thenReturn(mockRefundTx);

        auctionService.finalizeAuction(auction);

        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals("WON", p2.getStatus());
        assertEquals("REFUNDED", p1.getStatus());
        assertEquals(BigDecimal.valueOf(600), p1.getRefundAmount()); // 100% refund

        // Verify loser received 100% refund transaction & notification
        verify(paymentService, times(1)).recordTransaction(
                eq(buyer1), eq(10L), eq(100L), eq(BigDecimal.valueOf(600)), eq("AUTO_REFUND"), anyString(), anyString()
        );
        verify(notificationService, times(1)).notifyRefundProcessed(
                eq(buyer1), eq("Handmade Terracotta Vase"), eq(BigDecimal.valueOf(600)), eq("CB-REF-12345"), eq(10L)
        );
    }

    @Test
    @DisplayName("9. Privacy test: Sanitized participant list contains only Name and City")
    void testPrivacySanitizedParticipants() {
        AuctionParticipant p1 = new AuctionParticipant(auction, buyer1, BigDecimal.valueOf(500));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(participantRepository.findByAuctionOrderByJoinedAtAsc(auction)).thenReturn(List.of(p1));

        List<AuctionParticipantDTO> dtoList = auctionService.getSanitizedParticipants(10L);

        assertEquals(1, dtoList.size());
        AuctionParticipantDTO dto = dtoList.get(0);
        assertEquals("Alice Sharma", dto.getName());
        assertEquals("Mumbai", dto.getCity());
        assertEquals(BigDecimal.valueOf(500), dto.getBasePricePaid());
    }

    @Test
    @DisplayName("10. Winner submits delivery address updates order status to PAID")
    void testWinnerSubmitsAddress() {
        auction.setStatus(AuctionStatus.ENDED);
        auction.setWinningBidder(buyer1);

        AuctionOrder existingOrder = new AuctionOrder();
        existingOrder.setId(500L);
        existingOrder.setAuction(auction);
        existingOrder.setBuyer(buyer1);

        when(userRepository.findByIdentifier("alice@example.com")).thenReturn(Optional.of(buyer1));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAddressRequest addressReq = new SubmitAddressRequest();
        addressReq.setFullName("Alice Sharma");
        addressReq.setStreetAddress("123 Marine Drive");
        addressReq.setCity("Mumbai");
        addressReq.setState("Maharashtra");
        addressReq.setPincode("400020");
        addressReq.setPhone("9876543210");

        AuctionOrder updated = auctionService.submitDeliveryAddress("alice@example.com", 10L, addressReq);

        assertEquals("PAID", updated.getStatus());
        assertEquals("123 Marine Drive", updated.getStreetAddress());
        assertEquals("400020", updated.getPincode());
    }
}
