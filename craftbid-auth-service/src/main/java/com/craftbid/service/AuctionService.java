package com.craftbid.service;

import com.craftbid.dto.AuctionParticipantDTO;
import com.craftbid.dto.CreateAuctionRequest;
import com.craftbid.dto.JoinAuctionRequest;
import com.craftbid.dto.SubmitAddressRequest;
import com.craftbid.dto.UpdateOrderStatusRequest;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.*;
import com.craftbid.websocket.AuctionEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final CraftRepository craftRepository;
    private final UserRepository userRepository;
    private final AuctionParticipantRepository participantRepository;
    private final AuctionOrderRepository orderRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final AuctionEventPublisher eventPublisher;
    private final RazorpayService razorpayService;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            CraftRepository craftRepository,
            UserRepository userRepository,
            AuctionParticipantRepository participantRepository,
            AuctionOrderRepository orderRepository,
            PaymentService paymentService,
            NotificationService notificationService,
            AuctionEventPublisher eventPublisher,
            RazorpayService razorpayService) {

        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.craftRepository = craftRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.razorpayService = razorpayService;
    }

    private User getUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }

    // ==========================================
    // 1. CREATE AUCTION (Artisan)
    // ==========================================

    @Transactional
    public Auction createAuction(String identifier, CreateAuctionRequest request) {
        User seller = getUserByIdentifier(identifier);

        if (!seller.isSellerEnabled() && seller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only enabled artisans can start auctions");
        }

        Craft craft = craftRepository.findById(request.getCraftId())
                .orElseThrow(() -> new RuntimeException("Craft not found"));

        if (!craft.getSeller().getId().equals(seller.getId()) && seller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You can only create auctions for your own crafts");
        }

        auctionRepository.findByCraftAndStatus(craft, AuctionStatus.ACTIVE)
                .ifPresent(a -> {
                    throw new RuntimeException("An active auction already exists for this craft");
                });

        BigDecimal startingPrice = request.getStartingPrice() != null
                ? request.getStartingPrice()
                : craft.getBasePrice();

        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than zero");
        }

        BigDecimal minIncrement = request.getMinBidIncrement() != null && request.getMinBidIncrement().compareTo(BigDecimal.ZERO) > 0
                ? request.getMinBidIncrement()
                : BigDecimal.valueOf(50);

        LocalDateTime startTime = request.getStartTime() != null
                ? request.getStartTime()
                : LocalDateTime.now();

        int hours = (request.getDurationHours() != null && request.getDurationHours() > 0)
                ? request.getDurationHours()
                : 24;

        LocalDateTime participationDeadline = startTime.plusHours(hours);
        LocalDateTime endTime = participationDeadline.plusMinutes(10); // buffer

        Auction auction = new Auction();
        auction.setCraft(craft);
        auction.setSeller(seller);
        auction.setStartingPrice(startingPrice);
        auction.setCurrentHighestBid(startingPrice);
        auction.setReservePrice(request.getReservePrice());
        auction.setMinBidIncrement(minIncrement);
        auction.setStartTime(startTime);
        auction.setParticipationDeadline(participationDeadline);
        auction.setEndTime(endTime);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setTotalBids(0);
        auction.setMaxParticipants(10);
        auction.setCurrentParticipantsCount(0);
        auction.setLiveTurnActive(false);

        Auction savedAuction = auctionRepository.save(auction);

        // Broadcast creation event
        try {
            eventPublisher.publishAuctionEvent(savedAuction.getId(), "auction:created", Map.of(
                    "auctionId", savedAuction.getId(),
                    "craftName", craft.getTitle(),
                    "basePrice", startingPrice,
                    "participationDeadline", participationDeadline.toString()
            ));
        } catch (Exception ignored) {}

        return savedAuction;
    }

    // ==========================================
    // 2. JOIN AUCTION WITH BASE DEPOSIT (Buyer)
    // ==========================================

    @Transactional
    public synchronized AuctionParticipant joinAuctionWithDeposit(String identifier, Long auctionId, JoinAuctionRequest request) {
        User buyer = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        if (auction.getSeller().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Artisans cannot join their own auctions");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE && auction.getStatus() != AuctionStatus.SCHEDULED && auction.getStatus() != AuctionStatus.LIVE) {
            throw new RuntimeException("This auction is not open for joining (Status: " + auction.getStatus() + ")");
        }

        // Check if user already joined
        Optional<AuctionParticipant> existing = participantRepository.findByAuctionAndUser(auction, buyer);
        if (existing.isPresent()) {
            return existing.get();
        }

        int maxLimit = auction.getMaxParticipants() > 0 ? auction.getMaxParticipants() : 10;
        if (auction.getCurrentParticipantsCount() >= maxLimit) {
            throw new RuntimeException("Auction room is full! Maximum " + maxLimit + " participants reached.");
        }

        BigDecimal basePrice = auction.getStartingPrice();
        AuctionParticipant participant = new AuctionParticipant(auction, buyer, basePrice);
        AuctionParticipant saved = participantRepository.save(participant);

        auction.setCurrentParticipantsCount(auction.getCurrentParticipantsCount() + 1);

        // Record payment ledger transaction
        String method = (request != null && request.getPaymentMethod() != null) ? request.getPaymentMethod() : "UPI";
        paymentService.recordTransaction(
                buyer,
                auction.getId(),
                auction.getCraft().getId(),
                basePrice,
                "BASE_DEPOSIT",
                method,
                "Paid base deposit of ₹" + basePrice + " for auction #" + auction.getId() + " (" + auction.getCraft().getTitle() + ")"
        );

        // Save auction
        Auction updatedAuction = auctionRepository.save(auction);

        // Dispatch in-app notification & transactional email to buyer
        try {
            notificationService.notifyAuctionJoined(buyer, auction.getCraft().getTitle(), basePrice, auction.getId());
        } catch (Exception e) {
            System.err.println("⚠️ Notification error on join: " + e.getMessage());
        }

        // Broadcast real-time WebSocket event (sanitized Name • City)
        try {
            Map<String, Object> joinData = new HashMap<>();
            joinData.put("auctionId", auction.getId());
            joinData.put("currentParticipants", updatedAuction.getCurrentParticipantsCount());
            joinData.put("maxParticipants", updatedAuction.getMaxParticipants());
            Map<String, String> pInfo = new HashMap<>();
            pInfo.put("name", buyer.getName());
            pInfo.put("city", buyer.getCity());
            joinData.put("participant", pInfo);

            eventPublisher.publishAuctionEvent(auction.getId(), "auction:joined", joinData);
        } catch (Exception ignored) {}

        return saved;
    }

    // ==========================================
    // 3. PLACE DIFFERENTIAL BID (Buyer)
    // ==========================================

    @Transactional
    public synchronized Bid placeDifferentialBid(String identifier, Long auctionId, BigDecimal targetBidAmount) {
        if (targetBidAmount == null || targetBidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than zero");
        }

        User bidder = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        // Check if 1-minute turn timer expired
        if (checkTurnExpiry(auction)) {
            finalizeAuction(auction);
            throw new RuntimeException("The 1-minute turn timer has expired! The auction has ended.");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE && auction.getStatus() != AuctionStatus.LIVE) {
            throw new RuntimeException("This auction is no longer active (Status: " + auction.getStatus() + ")");
        }

        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new AccessDeniedException("Artisans cannot bid on their own auctions");
        }

        // Verify bidder has joined by paying base deposit
        AuctionParticipant participant = participantRepository.findByAuctionAndUser(auction, bidder)
                .orElseThrow(() -> new AccessDeniedException("You must pay the Base Price deposit of ₹" + auction.getStartingPrice() + " to join this auction before bidding"));

        BigDecimal minRequiredBid;
        if (auction.getTotalBids() == 0) {
            minRequiredBid = auction.getStartingPrice();
        } else {
            minRequiredBid = auction.getCurrentHighestBid().add(auction.getMinBidIncrement());
        }

        if (targetBidAmount.compareTo(minRequiredBid) < 0) {
            throw new IllegalArgumentException(
                    "Bid must be at least ₹" + minRequiredBid + " (Current price: ₹" + auction.getCurrentHighestBid() + " + Min increment: ₹" + auction.getMinBidIncrement() + ")"
            );
        }

        User previousWinningBidder = auction.getWinningBidder();

        // Calculate differential payment required: diff = targetBid - amountAlreadyPaid
        BigDecimal diffToPay = targetBidAmount.subtract(participant.getTotalAmountPaid());
        if (diffToPay.compareTo(BigDecimal.ZERO) < 0) {
            diffToPay = BigDecimal.ZERO;
        }

        // Record Bid
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(targetBidAmount);
        bid.setBidTime(LocalDateTime.now());
        bid.setStatus("ACCEPTED (Diff: ₹" + diffToPay + ")");
        Bid savedBid = bidRepository.save(bid);

        // Update participant's total committed paid amount
        participant.setTotalAmountPaid(targetBidAmount);
        participant.setStatus("ACTIVE");
        participantRepository.save(participant);

        // Record payment ledger transaction if differential > 0
        if (diffToPay.compareTo(BigDecimal.ZERO) > 0) {
            paymentService.recordTransaction(
                    bidder,
                    auction.getId(),
                    auction.getCraft().getId(),
                    diffToPay,
                    "DIFFERENTIAL_BID",
                    "UPI",
                    "Paid differential increment of ₹" + diffToPay + " (New Bid: ₹" + targetBidAmount + ")"
            );
        }

        // Update auction state & reset 1-minute countdown timer
        auction.setCurrentHighestBid(targetBidAmount);
        auction.setWinningBidder(bidder);
        auction.setTotalBids(auction.getTotalBids() + 1);
        auction.setLastBidTime(LocalDateTime.now());
        auction.setTurnDeadline(LocalDateTime.now().plusSeconds(60)); // Reset 1-minute turn timer
        auction.setLiveTurnActive(true);
        auction.setStatus(AuctionStatus.ACTIVE);
        Auction savedAuction = auctionRepository.save(auction);

        // Notify previous leader that they were outbid
        if (previousWinningBidder != null && !previousWinningBidder.getId().equals(bidder.getId())) {
            try {
                notificationService.notifyOutbid(previousWinningBidder, auction.getCraft().getTitle(), targetBidAmount, auction.getId());
            } catch (Exception ignored) {}
        }

        // Broadcast real-time WebSocket bid event
        try {
            Map<String, Object> bidData = new HashMap<>();
            bidData.put("auctionId", auction.getId());
            bidData.put("amount", targetBidAmount);
            bidData.put("diffPaid", diffToPay);
            bidData.put("bidderName", bidder.getName());
            bidData.put("bidderCity", bidder.getCity());
            bidData.put("totalBids", savedAuction.getTotalBids());
            bidData.put("turnDeadline", savedAuction.getTurnDeadline().toString());
            bidData.put("secondsRemaining", 60);

            eventPublisher.publishAuctionEvent(auction.getId(), "auction:bid", bidData);
        } catch (Exception ignored) {}

        return savedBid;
    }

    // ==========================================
    // 4. PARTICIPATION WINDOW EVALUATION & SCHEDULER
    // ==========================================

    @Transactional
    public synchronized void evaluateParticipationWindow(Auction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE && auction.getStatus() != AuctionStatus.SCHEDULED) {
            return;
        }

        if (auction.isLiveTurnActive()) {
            return;
        }

        int count = auction.getCurrentParticipantsCount();

        if (count == 0) {
            // Cancel auction
            auction.setStatus(AuctionStatus.CANCELLED);
            auctionRepository.save(auction);
            try {
                eventPublisher.publishAuctionEvent(auction.getId(), "auction:cancelled", Map.of("auctionId", auction.getId(), "reason", "No participants joined within 24 hours"));
            } catch (Exception ignored) {}
        } else if (count == 1) {
            // Direct purchase at base price
            auction.setStatus(AuctionStatus.DIRECT_PURCHASE);
            List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
            if (!participants.isEmpty()) {
                AuctionParticipant winnerPart = participants.get(0);
                User winner = winnerPart.getUser();
                auction.setWinningBidder(winner);
                auction.setCurrentHighestBid(auction.getStartingPrice());

                winnerPart.setStatus("WON");
                participantRepository.save(winnerPart);

                // Financials: 10% fee, 90% payout
                BigDecimal amount = auction.getStartingPrice();
                BigDecimal platformFee = amount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal artisanPayout = amount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
                auction.setAdminFeeAmount(platformFee);
                auction.setArtisanPayoutAmount(artisanPayout);

                // Create initial order
                createAuctionOrderRecord(auction, winner, amount, platformFee, artisanPayout);

                notificationService.notifyAuctionWon(winner, auction.getCraft().getTitle(), amount, auction.getId());
                notificationService.notifyArtisanCraftSold(auction.getSeller(), auction.getCraft().getTitle(), amount, artisanPayout, auction.getId());
            }
            auction.setStatus(AuctionStatus.ENDED);
            auctionRepository.save(auction);

            try {
                eventPublisher.publishAuctionEvent(auction.getId(), "auction:winner", Map.of(
                        "auctionId", auction.getId(),
                        "winningAmount", auction.getStartingPrice(),
                        "directPurchase", true
                ));
            } catch (Exception ignored) {}
        } else {
            // 2 to 10 participants -> Start 1-Minute Live Auction
            auction.setLiveTurnActive(true);
            auction.setTurnDeadline(LocalDateTime.now().plusSeconds(60));
            auction.setLastBidTime(LocalDateTime.now());
            auctionRepository.save(auction);

            // Notify all participants that live bidding started
            List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
            for (AuctionParticipant p : participants) {
                try {
                    notificationService.notifyAuctionLiveStarted(p.getUser(), auction.getCraft().getTitle(), auction.getId());
                } catch (Exception ignored) {}
            }

            try {
                eventPublisher.publishAuctionEvent(auction.getId(), "auction:started", Map.of(
                        "auctionId", auction.getId(),
                        "currentPrice", auction.getCurrentHighestBid(),
                        "turnDeadline", auction.getTurnDeadline().toString(),
                        "secondsRemaining", 60
                ));
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // 5. CHECK TURN TIMER & FINALIZE (Winner & 100% Refunds)
    // ==========================================

    @Transactional
    public synchronized Auction checkAndFinalizeAuctionState(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        if ((auction.getStatus() == AuctionStatus.ACTIVE || auction.getStatus() == AuctionStatus.LIVE) && checkTurnExpiry(auction)) {
            finalizeAuction(auction);
        }

        return auction;
    }

    private boolean checkTurnExpiry(Auction auction) {
        if (!auction.isLiveTurnActive()) {
            // Check 24-hour participation window
            if (auction.getParticipationDeadline() != null && LocalDateTime.now().isAfter(auction.getParticipationDeadline())) {
                evaluateParticipationWindow(auction);
                return false;
            }
            return false;
        }

        if (auction.getTurnDeadline() != null && LocalDateTime.now().isAfter(auction.getTurnDeadline())) {
            return true;
        }
        if (auction.getEndTime() != null && LocalDateTime.now().isAfter(auction.getEndTime())) {
            return true;
        }
        return false;
    }

    @Transactional
    public synchronized void finalizeAuction(Auction auction) {
        if (auction.getStatus() == AuctionStatus.ENDED || auction.getStatus() == AuctionStatus.CANCELLED) {
            return;
        }

        auction.setStatus(AuctionStatus.ENDED);
        auction.setLiveTurnActive(false);

        User winner = auction.getWinningBidder();
        BigDecimal finalWinningAmount = auction.getCurrentHighestBid();

        if (winner != null && finalWinningAmount != null) {
            // 10% platform fee & 90% artisan payout
            BigDecimal adminFee = finalWinningAmount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal artisanPayout = finalWinningAmount.subtract(adminFee).setScale(2, RoundingMode.HALF_UP);

            auction.setAdminFeeAmount(adminFee);
            auction.setArtisanPayoutAmount(artisanPayout);

            // Mark winner participant
            participantRepository.findByAuctionAndUser(auction, winner).ifPresent(p -> {
                p.setStatus("WON");
                participantRepository.save(p);
            });

            // Create or update AuctionOrder
            createAuctionOrderRecord(auction, winner, finalWinningAmount, adminFee, artisanPayout);

            // Notify winner & artisan
            try {
                notificationService.notifyAuctionWon(winner, auction.getCraft().getTitle(), finalWinningAmount, auction.getId());
                notificationService.notifyArtisanCraftSold(auction.getSeller(), auction.getCraft().getTitle(), finalWinningAmount, artisanPayout, auction.getId());
            } catch (Exception ignored) {}

            // 100% AUTOMATED REFUND FOR ALL OTHER PARTICIPANTS
            List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
            for (AuctionParticipant p : participants) {
                if (!p.getUser().getId().equals(winner.getId())) {
                    BigDecimal refundAmt = p.getTotalAmountPaid();
                    p.setStatus("REFUNDED");
                    p.setRefundAmount(refundAmt);
                    participantRepository.save(p);

                    // Record auto-refund in payment ledger
                    PaymentTransaction refundTx = paymentService.recordTransaction(
                            p.getUser(),
                            auction.getId(),
                            auction.getCraft().getId(),
                            refundAmt,
                            "AUTO_REFUND",
                            "UPI",
                            "100% Automated refund of ₹" + refundAmt + " for outbid participation in auction #" + auction.getId()
                    );

                    // Execute Razorpay refund (if gateway active/simulated)
                    try {
                        razorpayService.processRefund(refundTx.getTransactionRef(), refundAmt, "100% Outbid Refund - Auction #" + auction.getId());
                    } catch (Exception ignored) {}

                    // Notify losing participant with exact refund reference
                    try {
                        notificationService.notifyRefundProcessed(
                                p.getUser(),
                                auction.getCraft().getTitle(),
                                refundAmt,
                                refundTx.getTransactionRef(),
                                auction.getId()
                        );
                    } catch (Exception ignored) {}
                }
            }

            // Broadcast real-time conclusion over WebSocket
            try {
                Map<String, Object> endData = new HashMap<>();
                endData.put("auctionId", auction.getId());
                endData.put("winnerName", winner.getName());
                endData.put("winnerCity", winner.getCity());
                endData.put("winningAmount", finalWinningAmount);
                endData.put("status", "ENDED");

                eventPublisher.publishAuctionEvent(auction.getId(), "auction:ended", endData);
                eventPublisher.publishAuctionEvent(auction.getId(), "auction:winner", endData);
            } catch (Exception ignored) {}
        } else {
            // No winner -> check participation evaluation
            evaluateParticipationWindow(auction);
        }

        auctionRepository.save(auction);
    }

    private void createAuctionOrderRecord(Auction auction, User buyer, BigDecimal winningAmount, BigDecimal platformFee, BigDecimal artisanPayout) {
        Optional<AuctionOrder> existing = orderRepository.findByAuction(auction);
        if (existing.isEmpty()) {
            AuctionOrder order = new AuctionOrder();
            order.setAuction(auction);
            order.setBuyer(buyer);
            order.setArtisan(auction.getSeller());
            order.setWinningAmount(winningAmount);
            order.setPlatformFee(platformFee);
            order.setArtisanPayout(artisanPayout);
            order.setFullName(buyer.getName());
            order.setStreetAddress("Pending Buyer Address Submission");
            order.setCity(buyer.getCity());
            order.setState("India");
            order.setPincode("000000");
            order.setPhone(buyer.getPhone() != null ? buyer.getPhone() : "Pending");
            order.setStatus("PENDING_ADDRESS");
            orderRepository.save(order);
        }
    }

    // ==========================================
    // 6. SUBMIT DELIVERY ADDRESS (Winning Buyer)
    // ==========================================

    @Transactional
    public AuctionOrder submitDeliveryAddress(String identifier, Long auctionId, SubmitAddressRequest request) {
        User buyer = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        if (auction.getStatus() != AuctionStatus.ENDED && auction.getStatus() != AuctionStatus.DIRECT_PURCHASE) {
            throw new RuntimeException("Cannot submit address for an auction that is still active");
        }

        if (auction.getWinningBidder() == null || !auction.getWinningBidder().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Only the winning bidder can submit the delivery address");
        }

        AuctionOrder order = orderRepository.findByAuction(auction)
                .orElseGet(() -> {
                    BigDecimal winningAmount = auction.getCurrentHighestBid();
                    BigDecimal platformFee = winningAmount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal artisanPayout = winningAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

                    AuctionOrder newOrder = new AuctionOrder();
                    newOrder.setAuction(auction);
                    newOrder.setBuyer(buyer);
                    newOrder.setArtisan(auction.getSeller());
                    newOrder.setWinningAmount(winningAmount);
                    newOrder.setPlatformFee(platformFee);
                    newOrder.setArtisanPayout(artisanPayout);
                    return newOrder;
                });

        order.setFullName(request.getFullName());
        order.setStreetAddress(request.getStreetAddress());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPincode(request.getPincode());
        order.setPhone(request.getPhone());
        order.setStatus("PAID"); // Marked PAID / PENDING_DISPATCH

        AuctionOrder savedOrder = orderRepository.save(order);

        // Notify artisan that buyer provided address
        try {
            notificationService.createNotification(
                    auction.getSeller(),
                    "📦 Delivery Address Provided",
                    "Buyer " + request.getFullName() + " has submitted the delivery address for \"" + auction.getCraft().getTitle() + "\". You may now pack and ship the item.",
                    "ADDRESS_SUBMITTED",
                    "/artisan-dashboard"
            );
        } catch (Exception ignored) {}

        return savedOrder;
    }

    // ==========================================
    // 7. ORDER FULFILLMENT (Artisan)
    // ==========================================

    @Transactional
    public AuctionOrder updateOrderStatus(String identifier, Long orderId, UpdateOrderStatusRequest request) {
        User user = getUserByIdentifier(identifier);
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getArtisan().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the craft artisan or admin can update order status");
        }

        String newStatus = request.getStatus().toUpperCase();
        order.setStatus(newStatus);
        AuctionOrder updated = orderRepository.save(order);

        if ("SHIPPED".equalsIgnoreCase(newStatus) || "DISPATCHED".equalsIgnoreCase(newStatus)) {
            try {
                notificationService.notifyOrderShipped(
                        order.getBuyer(),
                        order.getAuction().getCraft().getTitle(),
                        request.getTrackingNotes(),
                        request.getCarrier(),
                        order.getId()
                );
            } catch (Exception ignored) {}
        }

        return updated;
    }

    // ==========================================
    // 8. PRIVACY-SANITIZED PARTICIPANTS & QUERIES
    // ==========================================

    public List<AuctionParticipantDTO> getSanitizedParticipants(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);

        return participants.stream()
                .map(p -> new AuctionParticipantDTO(
                        p.getId(),
                        p.getUser().getId(),
                        p.getUser().getName(),
                        p.getUser().getCity(),
                        p.getBasePricePaid(),
                        p.getTotalAmountPaid(),
                        p.getStatus(),
                        p.getJoinedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<AuctionParticipant> getAuctionParticipants(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        return participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
    }

    public List<AuctionOrder> getArtisanOrders(String identifier) {
        User artisan = getUserByIdentifier(identifier);
        return orderRepository.findByArtisanOrderByCreatedAtDesc(artisan);
    }

    public List<AuctionOrder> getBuyerOrders(String identifier) {
        User buyer = getUserByIdentifier(identifier);
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public Optional<AuctionOrder> getAuctionOrder(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        return orderRepository.findByAuction(auction);
    }

    public List<Auction> getActiveAuctions() {
        List<Auction> active = auctionRepository.findByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE);
        for (Auction a : active) {
            checkAndFinalizeAuctionState(a.getId());
        }
        return auctionRepository.findByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE);
    }

    public Auction getAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found with id: " + id));

        if ((auction.getStatus() == AuctionStatus.ACTIVE || auction.getStatus() == AuctionStatus.LIVE) && checkTurnExpiry(auction)) {
            finalizeAuction(auction);
        }

        return auction;
    }

    public List<Auction> getMyAuctions(String identifier) {
        User seller = getUserByIdentifier(identifier);
        return auctionRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<Auction> getAuctionsByCraftId(Long craftId) {
        return auctionRepository.findByCraftId(craftId);
    }

    @Transactional
    public Auction cancelAuction(String identifier, Long auctionId) {
        User user = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        if (!auction.getSeller().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You are not authorized to cancel this auction");
        }

        if (auction.getStatus() == AuctionStatus.ENDED || auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel an auction that has already ended");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        auction.setLiveTurnActive(false);

        // Refund any participants and record ledger transactions
        List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
        for (AuctionParticipant p : participants) {
            BigDecimal refundAmt = p.getTotalAmountPaid();
            p.setStatus("REFUNDED");
            p.setRefundAmount(refundAmt);
            participantRepository.save(p);

            PaymentTransaction refundTx = paymentService.recordTransaction(
                    p.getUser(),
                    auction.getId(),
                    auction.getCraft().getId(),
                    refundAmt,
                    "AUTO_REFUND",
                    "UPI",
                    "100% Refund of ₹" + refundAmt + " due to auction cancellation #" + auction.getId()
            );

            try {
                razorpayService.processRefund(refundTx.getTransactionRef(), refundAmt, "Auction Cancellation Refund");
                notificationService.notifyRefundProcessed(p.getUser(), auction.getCraft().getTitle(), refundAmt, refundTx.getTransactionRef(), auction.getId());
            } catch (Exception ignored) {}
        }

        return auctionRepository.save(auction);
    }

    public List<Bid> getAuctionBids(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
    }

    public List<Bid> getMyBids(String identifier) {
        User bidder = getUserByIdentifier(identifier);
        return bidRepository.findByBidderOrderByBidTimeDesc(bidder);
    }
}
