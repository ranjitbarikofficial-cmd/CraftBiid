package com.craftbid.repository;

import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionInterest;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionInterestRepository extends JpaRepository<AuctionInterest, Long> {
    List<AuctionInterest> findByAuction(Auction auction);
    Optional<AuctionInterest> findByAuctionAndUser(Auction auction, User user);
    boolean existsByAuctionAndUser(Auction auction, User user);
    long countByAuction(Auction auction);
}
