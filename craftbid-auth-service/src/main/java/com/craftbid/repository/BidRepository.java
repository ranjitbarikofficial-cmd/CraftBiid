package com.craftbid.repository;

import com.craftbid.entity.Auction;
import com.craftbid.entity.Bid;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByAuctionOrderByBidTimeDesc(Auction auction);

    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    List<Bid> findByBidderOrderByBidTimeDesc(User bidder);

    long countByAuction(Auction auction);
}
