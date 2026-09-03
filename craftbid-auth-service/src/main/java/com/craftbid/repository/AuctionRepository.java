package com.craftbid.repository;

import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionStatus;
import com.craftbid.entity.Craft;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatusOrderByEndTimeAsc(AuctionStatus status);

    List<Auction> findBySellerOrderByCreatedAtDesc(User seller);

    Optional<Auction> findByCraftAndStatus(Craft craft, AuctionStatus status);

    List<Auction> findByCraftId(Long craftId);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(
            @Param("status") AuctionStatus status,
            @Param("now") LocalDateTime now
    );
}
