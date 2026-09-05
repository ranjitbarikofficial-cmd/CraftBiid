package com.craftbid.controller;

import com.craftbid.dto.CreateAuctionRequest;
import com.craftbid.dto.JoinAuctionRequest;
import com.craftbid.dto.PlaceBidRequest;
import com.craftbid.dto.SubmitAddressRequest;
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

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<AuctionParticipant>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getAuctionParticipants(id));
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
    public ResponseEntity<AuctionOrder> getAuctionOrder(@PathVariable Long id) {
        return ResponseEntity.of(auctionService.getAuctionOrder(id));
    }

    @GetMapping("/artisan-orders")
    public ResponseEntity<List<AuctionOrder>> getArtisanOrders(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getArtisanOrders(identifier));
    }

    @GetMapping("/buyer-orders")
    public ResponseEntity<List<AuctionOrder>> getBuyerOrders(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(auctionService.getBuyerOrders(identifier));
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
