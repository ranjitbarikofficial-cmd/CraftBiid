package com.craftbid.dto;

import com.craftbid.entity.Bid;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PublicBidResponse {

    private Long id;
    private Long auctionId;
    private PublicSellerResponse bidder;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private String status;

    public PublicBidResponse() {
    }

    public static PublicBidResponse fromEntity(Bid bid) {
        if (bid == null) {
            return null;
        }
        PublicBidResponse res = new PublicBidResponse();
        res.setId(bid.getId());
        res.setAuctionId(bid.getAuction() != null ? bid.getAuction().getId() : null);
        res.setBidder(PublicSellerResponse.fromUser(bid.getBidder()));
        res.setAmount(bid.getAmount());
        res.setBidTime(bid.getBidTime());
        res.setStatus(bid.getStatus());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public PublicSellerResponse getBidder() {
        return bidder;
    }

    public void setBidder(PublicSellerResponse bidder) {
        this.bidder = bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
