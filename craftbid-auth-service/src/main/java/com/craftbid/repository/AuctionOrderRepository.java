package com.craftbid.repository;

import com.craftbid.entity.Auction;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {

    Optional<AuctionOrder> findByAuction(Auction auction);

    List<AuctionOrder> findByArtisanOrderByCreatedAtDesc(User artisan);

    List<AuctionOrder> findByBuyerOrderByCreatedAtDesc(User buyer);
}
