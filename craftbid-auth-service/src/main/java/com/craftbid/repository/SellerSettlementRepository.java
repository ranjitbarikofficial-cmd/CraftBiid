package com.craftbid.repository;

import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.SellerSettlement;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerSettlementRepository extends JpaRepository<SellerSettlement, Long> {

    Optional<SellerSettlement> findByOrder(AuctionOrder order);

    List<SellerSettlement> findByArtisanOrderByCreatedAtDesc(User artisan);

    List<SellerSettlement> findByStatusOrderByCreatedAtDesc(String status);
}
