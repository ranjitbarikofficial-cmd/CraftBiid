package com.craftbid.shipping;

import com.craftbid.dto.CreateShipmentDTO;
import com.craftbid.dto.PackageDetailsDTO;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Shipment;
import com.craftbid.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class ShippingService {

    private final Map<String, ShippingProvider> shippingProviders;
    private final ShipmentRepository shipmentRepository;

    public ShippingService(Map<String, ShippingProvider> shippingProviders,
                           ShipmentRepository shipmentRepository) {
        this.shippingProviders = shippingProviders;
        this.shipmentRepository = shipmentRepository;
    }

    public ShippingProvider getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            providerName = "manualShippingProvider";
        }
        if (!providerName.endsWith("ShippingProvider")) {
            providerName = providerName.toLowerCase() + "ShippingProvider";
        }
        ShippingProvider provider = shippingProviders.get(providerName);
        if (provider == null) {
            provider = shippingProviders.get("manualShippingProvider");
        }
        return provider;
    }

    @Transactional
    public Shipment savePackageDetails(AuctionOrder order, PackageDetailsDTO dto) {
        Shipment shipment = order.getShipment();
        if (shipment == null) {
            shipment = new Shipment(order);
        }
        shipment.setWeight(dto.getWeight());
        shipment.setLength(dto.getLength());
        shipment.setWidth(dto.getWidth());
        shipment.setHeight(dto.getHeight());
        if (dto.getNotes() != null) {
            shipment.setTrackingNotes(dto.getNotes());
        }
        shipment.setStatus("SELLER_PREPARING");
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment createShipment(AuctionOrder order, CreateShipmentDTO dto, String providerName) {
        ShippingProvider provider = getProvider(providerName);
        Shipment shipment = provider.createShipment(order, dto);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment updateShipmentStatus(Shipment shipment, String newStatus, String notes) {
        ShippingProvider provider = getProvider(shipment.getProvider());
        Shipment updated = provider.updateStatus(shipment, newStatus, notes);
        return shipmentRepository.save(updated);
    }

    public Optional<Shipment> getShipmentByOrder(AuctionOrder order) {
        return shipmentRepository.findByOrder(order);
    }

    public Optional<Shipment> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }
}
