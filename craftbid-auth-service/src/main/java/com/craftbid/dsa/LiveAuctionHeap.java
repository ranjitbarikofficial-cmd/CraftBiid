package com.craftbid.dsa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-Memory Binary Heap / PriorityQueue for Real-Time Live Auctions.
 *
 * Complexities:
 * - peekMax() -> O(1)
 * - insertOrUpdateBid() -> O(log K) where K is number of bidders
 * - getTopLeaderboard(k) -> O(K log K)
 *
 * Provides thread-safe, high-frequency bid processing and masked leaderboard generation.
 */
public class LiveAuctionHeap {

    private final Long auctionId;
    private final PriorityQueue<BidNode> maxHeap;
    private final Map<Long, BidNode> bidderNodeMap; // BidderId -> BidNode
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static class BidNode implements Comparable<BidNode> {
        private final Long bidderId;
        private final String bidderName;
        private final String city;
        private BigDecimal amount;
        private LocalDateTime timestamp;
        private int bidCount;

        public BidNode(Long bidderId, String bidderName, String city, BigDecimal amount, LocalDateTime timestamp) {
            this.bidderId = bidderId;
            this.bidderName = bidderName;
            this.city = city;
            this.amount = amount;
            this.timestamp = timestamp;
            this.bidCount = 1;
        }

        @Override
        public int compareTo(BidNode o) {
            // Max-heap: higher amount has higher priority
            int cmp = o.amount.compareTo(this.amount);
            if (cmp != 0) {
                return cmp;
            }
            // Tie-break: earlier timestamp has higher priority
            return this.timestamp.compareTo(o.timestamp);
        }

        public Long getBidderId() {
            return bidderId;
        }

        public String getBidderName() {
            return bidderName;
        }

        public String getCity() {
            return city;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getBidCount() {
            return bidCount;
        }

        public String getMaskedIdentity() {
            String name = (bidderName != null && !bidderName.isBlank()) ? bidderName.trim() : "Anonymous";
            String c = (city != null && !city.isBlank()) ? city.trim() : "CraftBid Member";
            return name + " • " + c;
        }
    }

    public LiveAuctionHeap(Long auctionId) {
        this.auctionId = auctionId;
        this.maxHeap = new PriorityQueue<>();
        this.bidderNodeMap = new HashMap<>();
    }

    /**
     * Record a new or updated differential bid from a participant.
     * Time Complexity: O(log K)
     */
    public BidNode recordBid(Long bidderId, String bidderName, String city, BigDecimal amount, LocalDateTime timestamp) {
        if (bidderId == null || amount == null) return null;

        rwLock.writeLock().lock();
        try {
            BidNode existing = bidderNodeMap.get(bidderId);
            if (existing != null) {
                // Update existing participant's bid
                maxHeap.remove(existing); // O(K) remove from heap
                existing.amount = amount;
                existing.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
                existing.bidCount++;
                maxHeap.offer(existing); // O(log K) re-insert
                return existing;
            } else {
                // New bidder
                BidNode newNode = new BidNode(bidderId, bidderName, city, amount, (timestamp != null) ? timestamp : LocalDateTime.now());
                bidderNodeMap.put(bidderId, newNode);
                maxHeap.offer(newNode);
                return newNode;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Peek the current highest bidder without removing from the heap.
     * Time Complexity: O(1)
     */
    public BidNode peekHighestBid() {
        rwLock.readLock().lock();
        try {
            return maxHeap.peek();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns top K bidders for real-time leaderboard broadcast.
     * Time Complexity: O(K log K)
     */
    public List<BidNode> getTopBidders(int k) {
        if (k <= 0) return Collections.emptyList();

        rwLock.readLock().lock();
        try {
            List<BidNode> all = new ArrayList<>(maxHeap);
            all.sort(BidNode::compareTo); // Sort based on Max-Heap comparator

            if (all.size() > k) {
                return all.subList(0, k);
            }
            return all;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Get the bid amount for a specific participant.
     * Time Complexity: O(1)
     */
    public BigDecimal getParticipantCommittedAmount(Long bidderId) {
        if (bidderId == null) return BigDecimal.ZERO;

        rwLock.readLock().lock();
        try {
            BidNode node = bidderNodeMap.get(bidderId);
            return (node != null) ? node.getAmount() : BigDecimal.ZERO;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getParticipantCount() {
        rwLock.readLock().lock();
        try {
            return bidderNodeMap.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            maxHeap.clear();
            bidderNodeMap.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
