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
    public ResponseEntity<Auction> createAuction(
            Authentication authentication,
            @Valid @RequestBody CreateAuctionRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.createAuction(identifier, request));
    }

    @GetMapping
    public ResponseEntity<List<Auction>> getActiveAuctions() {
        return ResponseEntity.ok(auctionService.getActiveAuctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auction> getAuctionById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @GetMapping("/my-auctions")
    public ResponseEntity<List<Auction>> getMyAuctions(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getMyAuctions(identifier));
    }

    @GetMapping("/craft/{craftId}")
    public ResponseEntity<List<Auction>> getAuctionsByCraftId(@PathVariable Long craftId) {
        return ResponseEntity.ok(auctionService.getAuctionsByCraftId(craftId));
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

    // Place differential bid & reset 1-minute timer
    @PostMapping("/{id}/differential-bid")
    public ResponseEntity<Bid> placeDifferentialBid(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PlaceBidRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.placeDifferentialBid(identifier, id, request.getAmount()));
    }

    // Fallback standard bid
    @PostMapping("/{id}/bids")
    public ResponseEntity<Bid> placeBid(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PlaceBidRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.placeDifferentialBid(identifier, id, request.getAmount()));
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
    public ResponseEntity<List<Bid>> getAuctionBids(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionBids(id));
    }

    @GetMapping("/my-bids")
    public ResponseEntity<List<Bid>> getMyBids(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getMyBids(identifier));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Auction> cancelAuction(
            Authentication authentication,
            @PathVariable Long id) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.cancelAuction(identifier, id));
    }
}
