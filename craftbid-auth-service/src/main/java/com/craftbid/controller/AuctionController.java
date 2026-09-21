package com.craftbid.controller;

import com.craftbid.dto.*;
import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.AuctionParticipant;
import com.craftbid.entity.Bid;
import com.craftbid.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<PublicAuctionResponse> createAuction(
            Authentication authentication,
            @Valid @RequestBody CreateAuctionRequest request) {

        String identifier = authentication.getName();
        Auction auction = auctionService.createAuction(identifier, request);
        return ResponseEntity.ok(PublicAuctionResponse.fromEntity(auction));
    }

    @GetMapping
    public ResponseEntity<List<PublicAuctionResponse>> getActiveAuctions() {
        List<Auction> auctions = auctionService.getActiveAuctions();
        List<PublicAuctionResponse> response = auctions.stream()
                .map(PublicAuctionResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicAuctionResponse> getAuctionById(@PathVariable Long id) {
        Auction auction = auctionService.getAuctionById(id);
        return ResponseEntity.ok(PublicAuctionResponse.fromEntity(auction));
    }

    @GetMapping("/my-auctions")
    public ResponseEntity<List<PublicAuctionResponse>> getMyAuctions(Authentication authentication) {
        String identifier = authentication.getName();
        List<Auction> auctions = auctionService.getMyAuctions(identifier);
        List<PublicAuctionResponse> response = auctions.stream()
                .map(PublicAuctionResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/craft/{craftId}")
    public ResponseEntity<List<PublicAuctionResponse>> getAuctionsByCraftId(@PathVariable Long craftId) {
        List<Auction> auctions = auctionService.getAuctionsByCraftId(craftId);
        List<PublicAuctionResponse> response = auctions.stream()
                .map(PublicAuctionResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Join room with Base Deposit
    @PostMapping("/{id}/join")
    public ResponseEntity<AuctionParticipant> joinAuction(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) JoinAuctionRequest request) {

        String identifier = authentication.getName();
        if (request == null) request = new JoinAuctionRequest();
        return ResponseEntity.ok(auctionService.joinAuctionWithDeposit(identifier, id, request));
    }

    // Register Interest (Does NOT start timer; registers for notifications)
    @PostMapping("/{id}/interest")
    public ResponseEntity<PublicAuctionResponse> registerInterest(
            Authentication authentication,
            @PathVariable Long id) {

        String identifier = authentication.getName();
        Auction auction = auctionService.registerInterest(identifier, id);
        return ResponseEntity.ok(PublicAuctionResponse.fromEntity(auction));
    }

    // Place differential bid & reset 1-minute timer
    @PostMapping("/{id}/differential-bid")
    public ResponseEntity<PublicBidResponse> placeDifferentialBid(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PlaceBidRequest request) {

        String identifier = authentication.getName();
        Bid bid = auctionService.placeDifferentialBid(identifier, id, request.getAmount());
        return ResponseEntity.ok(PublicBidResponse.fromEntity(bid));
    }

    // Fallback standard bid
    @PostMapping("/{id}/bids")
    public ResponseEntity<PublicBidResponse> placeBid(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PlaceBidRequest request) {

        String identifier = authentication.getName();
        Bid bid = auctionService.placeDifferentialBid(identifier, id, request.getAmount());
        return ResponseEntity.ok(PublicBidResponse.fromEntity(bid));
    }

    // Privacy-Safe sanitized participants (Name • City only)
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<AuctionParticipantDTO>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getSanitizedParticipants(id));
    }

    // DSA: In-Memory Max-Heap Live Leaderboard (O(K log K))
    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<com.craftbid.dsa.LiveAuctionHeap.BidNode>> getLiveLeaderboard(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(auctionService.getLiveLeaderboard(id, limit));
    }

    // DSA: In-Memory Circular Ring Buffer Live Event Stream (O(1))
    @GetMapping("/{id}/live-events")
    public ResponseEntity<List<java.util.Map<String, Object>>> getRecentLiveEvents(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(auctionService.getRecentLiveEvents(id, limit));
    }

    @PostMapping(value = {"/{id}/address", "/{id}/submit-address"})
    public ResponseEntity<AuctionOrder> submitDeliveryAddress(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SubmitAddressRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.submitDeliveryAddress(identifier, id, request));
    }

    @GetMapping("/{id}/order")
    public ResponseEntity<AuctionOrder> getAuctionOrder(
            Authentication authentication,
            @PathVariable Long id) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getAuctionOrder(identifier, id).orElse(null));
    }

    @GetMapping(value = {"/artisan-orders", "/artisan/orders"})
    public ResponseEntity<List<AuctionOrder>> getArtisanOrders(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getArtisanOrders(identifier));
    }

    @GetMapping(value = {"/buyer-orders", "/customer/orders"})
    public ResponseEntity<List<AuctionOrder>> getBuyerOrders(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getBuyerOrders(identifier));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<AuctionOrder> updateOrderStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.updateOrderStatus(identifier, orderId, request));
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<PublicBidResponse>> getAuctionBids(@PathVariable Long id) {
        List<Bid> bids = auctionService.getAuctionBids(id);
        List<PublicBidResponse> response = bids.stream()
                .map(PublicBidResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-bids")
    public ResponseEntity<List<PublicBidResponse>> getMyBids(Authentication authentication) {
        String identifier = authentication.getName();
        List<Bid> bids = auctionService.getMyBids(identifier);
        List<PublicBidResponse> response = bids.stream()
                .map(PublicBidResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PublicAuctionResponse> cancelAuction(
            Authentication authentication,
            @PathVariable Long id) {

        String identifier = authentication.getName();
        Auction auction = auctionService.cancelAuction(identifier, id);
        return ResponseEntity.ok(PublicAuctionResponse.fromEntity(auction));
    }
}
