package com.craftbid.repository;

import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionParticipant;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionParticipantRepository extends JpaRepository<AuctionParticipant, Long> {

    List<AuctionParticipant> findByAuctionOrderByJoinedAtAsc(Auction auction);

    List<AuctionParticipant> findByAuctionId(Long auctionId);

    Optional<AuctionParticipant> findByAuctionAndUser(Auction auction, User user);

    List<AuctionParticipant> findByUserOrderByJoinedAtDesc(User user);

    long countByAuction(Auction auction);
}
