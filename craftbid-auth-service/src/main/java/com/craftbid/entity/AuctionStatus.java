package com.craftbid.entity;

public enum AuctionStatus {
    DRAFT,
    SCHEDULED,       // 24-hour participation window open
    ACTIVE,          // Active / Live
    LIVE,            // 1-minute live turn bidding
    DIRECT_PURCHASE, // 1 participant won at base price
    ENDED,           // Concluded
    CANCELLED        // Cancelled / 0 participants
}
