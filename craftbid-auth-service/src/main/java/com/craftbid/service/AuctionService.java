package com.craftbid.service;

import com.craftbid.dto.CreateAuctionRequest;
import com.craftbid.dto.JoinAuctionRequest;
import com.craftbid.dto.SubmitAddressRequest;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final CraftRepository craftRepository;
    private final UserRepository userRepository;
    private final AuctionParticipantRepository participantRepository;
    private final AuctionOrderRepository orderRepository;
    private final PaymentService paymentService;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            CraftRepository craftRepository,
            UserRepository userRepository,
            AuctionParticipantRepository participantRepository,
            AuctionOrderRepository orderRepository,
            PaymentService paymentService) {

        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.craftRepository = craftRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
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

        LocalDateTime endTime = request.getEndTime() != null
                ? request.getEndTime()
                : startTime.plusHours(hours);

        Auction auction = new Auction();
        auction.setCraft(craft);
        auction.setSeller(seller);
        auction.setStartingPrice(startingPrice);
        auction.setCurrentHighestBid(startingPrice);
        auction.setReservePrice(request.getReservePrice());
        auction.setMinBidIncrement(minIncrement);
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setTotalBids(0);
        auction.setMaxParticipants(10);
        auction.setCurrentParticipantsCount(0);
        auction.setLiveTurnActive(false);

        return auctionRepository.save(auction);
    }

    // ==========================================
    // 2. JOIN AUCTION WITH BASE DEPOSIT (Buyer)
    // ==========================================

    @Transactional
    public AuctionParticipant joinAuctionWithDeposit(String identifier, Long auctionId, JoinAuctionRequest request) {
        User buyer = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        if (auction.getSeller().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Artisans cannot join their own auctions");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("This auction is not open for joining");
        }

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
        paymentService.recordTransaction(
                buyer,
                auction.getId(),
                auction.getCraft().getId(),
                basePrice,
                "BASE_DEPOSIT",
                request != null ? request.getPaymentMethod() : "UPI",
                "Paid base deposit of ₹" + basePrice + " for auction #" + auction.getId()
        );

        // If first participant or beginning turn
        if (!auction.isLiveTurnActive()) {
            auction.setLiveTurnActive(true);
            auction.setLastBidTime(LocalDateTime.now());
            auction.setTurnDeadline(LocalDateTime.now().plusSeconds(60));
        }

        auctionRepository.save(auction);
        return saved;
    }

    // ==========================================
    // 3. PLACE DIFFERENTIAL BID (Buyer)
    // ==========================================

    @Transactional
    public Bid placeDifferentialBid(String identifier, Long auctionId, BigDecimal targetBidAmount) {
        if (targetBidAmount == null || targetBidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than zero");
        }

        User bidder = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        // Check if 1-minute turn timer expired
        if (checkTurnExpiry(auction)) {
            throw new RuntimeException("The 1-minute turn timer has expired! The auction is ended.");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("This auction is no longer active");
        }

        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new AccessDeniedException("Artisans cannot bid on their own auctions");
        }

        // Verify bidder has joined by paying base deposit
        AuctionParticipant participant = participantRepository.findByAuctionAndUser(auction, bidder)
                .orElseThrow(() -> new AccessDeniedException("You must pay the Base Price deposit to join this auction before bidding"));

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

        // Calculate differential payment required
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
        bid.setStatus("ACCEPTED (Diff Paid: ₹" + diffToPay + ")");
        Bid savedBid = bidRepository.save(bid);

        // Update participant's total paid amount
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

        // Update auction state & reset 1-minute timer
        auction.setCurrentHighestBid(targetBidAmount);
        auction.setWinningBidder(bidder);
        auction.setTotalBids(auction.getTotalBids() + 1);
        auction.setLastBidTime(LocalDateTime.now());
        auction.setTurnDeadline(LocalDateTime.now().plusSeconds(60)); // Reset 1-minute turn timer
        auction.setLiveTurnActive(true);
        auctionRepository.save(auction);

        return savedBid;
    }

    // ==========================================
    // 4. CHECK TURN TIMER & FINALIZE (Winner & Auto-Refunds)
    // ==========================================

    @Transactional
    public Auction checkAndFinalizeAuctionState(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        if (auction.getStatus() == AuctionStatus.ACTIVE && checkTurnExpiry(auction)) {
            finalizeAuction(auction);
        }

        return auction;
    }

    private boolean checkTurnExpiry(Auction auction) {
        if (auction.getTurnDeadline() != null && LocalDateTime.now().isAfter(auction.getTurnDeadline())) {
            return true;
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            return true;
        }
        return false;
    }

    @Transactional
    public void finalizeAuction(Auction auction) {
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

            // Refund all other participants 100% of their total amounts paid
            List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
            for (AuctionParticipant p : participants) {
                if (!p.getUser().getId().equals(winner.getId())) {
                    p.setStatus("REFUNDED");
                    p.setRefundAmount(p.getTotalAmountPaid());
                    participantRepository.save(p);

                    // Record auto-refund in payment ledger
                    paymentService.recordTransaction(
                            p.getUser(),
                            auction.getId(),
                            auction.getCraft().getId(),
                            p.getTotalAmountPaid(),
                            "AUTO_REFUND",
                            "UPI",
                            "100% Automated refund of ₹" + p.getTotalAmountPaid() + " for non-winning participation in auction #" + auction.getId()
                    );
                }
            }
        }

        auctionRepository.save(auction);
    }

    // ==========================================
    // 5. SUBMIT DELIVERY ADDRESS (Winning Buyer)
    // ==========================================

    @Transactional
    public AuctionOrder submitDeliveryAddress(String identifier, Long auctionId, SubmitAddressRequest request) {
        User buyer = getUserByIdentifier(identifier);
        Auction auction = getAuctionById(auctionId);

        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new RuntimeException("Cannot submit address for an auction that is still active");
        }

        if (auction.getWinningBidder() == null || !auction.getWinningBidder().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Only the winning bidder can submit the delivery address");
        }

        Optional<AuctionOrder> existing = orderRepository.findByAuction(auction);
        if (existing.isPresent()) {
            AuctionOrder order = existing.get();
            order.setFullName(request.getFullName());
            order.setStreetAddress(request.getStreetAddress());
            order.setCity(request.getCity());
            order.setState(request.getState());
            order.setPincode(request.getPincode());
            order.setPhone(request.getPhone());
            return orderRepository.save(order);
        }

        BigDecimal winningAmount = auction.getCurrentHighestBid();
        BigDecimal platformFee = winningAmount.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal artisanPayout = winningAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        AuctionOrder order = new AuctionOrder();
        order.setAuction(auction);
        order.setBuyer(buyer);
        order.setArtisan(auction.getSeller());
        order.setWinningAmount(winningAmount);
        order.setPlatformFee(platformFee);
        order.setArtisanPayout(artisanPayout);
        order.setFullName(request.getFullName());
        order.setStreetAddress(request.getStreetAddress());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPincode(request.getPincode());
        order.setPhone(request.getPhone());
        order.setStatus("PENDING_DISPATCH");

        return orderRepository.save(order);
    }

    // ==========================================
    // 6. QUERIES & PARTICIPANTS LIST
    // ==========================================

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

        if (auction.getStatus() == AuctionStatus.ACTIVE && checkTurnExpiry(auction)) {
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

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new RuntimeException("Cannot cancel an auction that is not active");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        auction.setLiveTurnActive(false);

        // Refund any participants and record ledger transactions
        List<AuctionParticipant> participants = participantRepository.findByAuctionOrderByJoinedAtAsc(auction);
        for (AuctionParticipant p : participants) {
            p.setStatus("REFUNDED");
            p.setRefundAmount(p.getTotalAmountPaid());
            participantRepository.save(p);

            paymentService.recordTransaction(
                    p.getUser(),
                    auction.getId(),
                    auction.getCraft().getId(),
                    p.getTotalAmountPaid(),
                    "AUTO_REFUND",
                    "UPI",
                    "Refund of ₹" + p.getTotalAmountPaid() + " due to auction cancellation #" + auction.getId()
            );
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
