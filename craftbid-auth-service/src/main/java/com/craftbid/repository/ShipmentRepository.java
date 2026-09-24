package com.craftbid.repository;

import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrder(AuctionOrder order);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
