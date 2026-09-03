package com.craftbid.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "craft_id", nullable = false)
    private Craft craft;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal currentHighestBid;

    @Column(precision = 12, scale = 2)
    private BigDecimal reservePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minBidIncrement = BigDecimal.valueOf(50);

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "winning_bidder_id")
    private User winningBidder;

    private int totalBids = 0;

    // --- Dynamic 1-Minute Live Turn & Pay-to-Bid Fields ---

    private int maxParticipants = 10;

    private int currentParticipantsCount = 0;

    private int interestedCount = 0;

    private LocalDateTime lastBidTime;

    private LocalDateTime turnDeadline; // 60-second countdown timestamp

    private boolean liveTurnActive = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal adminFeeAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal artisanPayoutAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Auction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Craft getCraft() {
        return craft;
    }

    public void setCraft(Craft craft) {
        this.craft = craft;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public User getWinningBidder() {
        return winningBidder;
    }

    public void setWinningBidder(User winningBidder) {
        this.winningBidder = winningBidder;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getCurrentParticipantsCount() {
        return currentParticipantsCount;
    }

    public void setCurrentParticipantsCount(int currentParticipantsCount) {
        this.currentParticipantsCount = currentParticipantsCount;
    }

    public int getInterestedCount() {
        return interestedCount;
    }

    public void setInterestedCount(int interestedCount) {
        this.interestedCount = interestedCount;
    }

    public LocalDateTime getLastBidTime() {
        return lastBidTime;
    }

    public void setLastBidTime(LocalDateTime lastBidTime) {
        this.lastBidTime = lastBidTime;
    }

    public LocalDateTime getTurnDeadline() {
        return turnDeadline;
    }

    public void setTurnDeadline(LocalDateTime turnDeadline) {
        this.turnDeadline = turnDeadline;
    }

    public boolean isLiveTurnActive() {
        return liveTurnActive;
    }

    public void setLiveTurnActive(boolean liveTurnActive) {
        this.liveTurnActive = liveTurnActive;
    }

    public BigDecimal getAdminFeeAmount() {
        return adminFeeAmount;
    }

    public void setAdminFeeAmount(BigDecimal adminFeeAmount) {
        this.adminFeeAmount = adminFeeAmount;
    }

    public BigDecimal getArtisanPayoutAmount() {
        return artisanPayoutAmount;
    }

    public void setArtisanPayoutAmount(BigDecimal artisanPayoutAmount) {
        this.artisanPayoutAmount = artisanPayoutAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
