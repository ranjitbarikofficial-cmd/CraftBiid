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

    boolean existsByAuction(Auction auction);

    Optional<AuctionOrder> findByOrderNumber(String orderNumber);

    List<AuctionOrder> findByArtisanOrderByCreatedAtDesc(User artisan);

    List<AuctionOrder> findByBuyerOrderByCreatedAtDesc(User buyer);

    List<AuctionOrder> findAllByOrderByCreatedAtDesc();

    List<AuctionOrder> findByStatusOrderByCreatedAtDesc(String status);
}
