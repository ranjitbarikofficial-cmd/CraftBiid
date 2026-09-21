package com.craftbid.dto;

import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicAuctionResponse {

    private Long id;
    private PublicCraftResponse craft;
    private PublicSellerResponse seller;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestBid;
    private BigDecimal reservePrice;
    private BigDecimal minBidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private PublicSellerResponse winningBidder;
    private int totalBids;
    private int maxParticipants;
    private int currentParticipantsCount;
    private int interestedCount;
    private LocalDateTime lastBidTime;
    private LocalDateTime participationDeadline;
    private LocalDateTime turnDeadline;
    private boolean liveTurnActive;
    private LocalDateTime createdAt;

    public PublicAuctionResponse() {
    }

    public static PublicAuctionResponse fromEntity(Auction auction) {
        if (auction == null) {
            return null;
        }
        PublicAuctionResponse res = new PublicAuctionResponse();
        res.setId(auction.getId());
        res.setCraft(PublicCraftResponse.fromEntity(auction.getCraft()));
        res.setSeller(PublicSellerResponse.fromUser(auction.getSeller()));
        res.setStartingPrice(auction.getStartingPrice());
        res.setCurrentHighestBid(auction.getCurrentHighestBid());
        res.setReservePrice(auction.getReservePrice());
        res.setMinBidIncrement(auction.getMinBidIncrement());
        res.setStartTime(auction.getStartTime());
        res.setEndTime(auction.getEndTime());
        res.setStatus(auction.getStatus());
        res.setWinningBidder(PublicSellerResponse.fromUser(auction.getWinningBidder()));
        res.setTotalBids(auction.getTotalBids());
        res.setMaxParticipants(auction.getMaxParticipants());
        res.setCurrentParticipantsCount(auction.getCurrentParticipantsCount());
        res.setInterestedCount(auction.getInterestedCount());
        res.setLastBidTime(auction.getLastBidTime());
        res.setParticipationDeadline(auction.getParticipationDeadline());
        res.setTurnDeadline(auction.getTurnDeadline());
        res.setLiveTurnActive(auction.isLiveTurnActive());
        res.setCreatedAt(auction.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PublicCraftResponse getCraft() {
        return craft;
    }

    public void setCraft(PublicCraftResponse craft) {
        this.craft = craft;
    }

    public PublicSellerResponse getSeller() {
        return seller;
    }

    public void setSeller(PublicSellerResponse seller) {
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

    public PublicSellerResponse getWinningBidder() {
        return winningBidder;
    }

    public void setWinningBidder(PublicSellerResponse winningBidder) {
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

    public LocalDateTime getParticipationDeadline() {
        return participationDeadline;
    }

    public void setParticipationDeadline(LocalDateTime participationDeadline) {
        this.participationDeadline = participationDeadline;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
