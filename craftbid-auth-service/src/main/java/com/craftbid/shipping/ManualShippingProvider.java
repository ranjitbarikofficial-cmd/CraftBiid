package com.craftbid.shipping;

import com.craftbid.dto.CreateShipmentDTO;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Shipment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("manualShippingProvider")
public class ManualShippingProvider implements ShippingProvider {

    @Override
    public String getProviderName() {
        return "MANUAL";
    }

    @Override
    public Shipment createShipment(AuctionOrder order, CreateShipmentDTO request) {
        Shipment shipment = order.getShipment();
        if (shipment == null) {
            shipment = new Shipment(order);
        }

        shipment.setProvider("MANUAL");
        shipment.setCourierName(request.getCourierName());
        shipment.setTrackingNumber(request.getTrackingNumber());
        if (request.getShippingCost() != null) {
            shipment.setShippingCost(request.getShippingCost());
        }
        if (request.getPickupDate() != null) {
            shipment.setPickupDate(request.getPickupDate());
        } else {
            shipment.setPickupDate(LocalDateTime.now());
        }
        if (request.getEstimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        } else {
            shipment.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(4));
        }
        if (request.getTrackingNotes() != null) {
            shipment.setTrackingNotes(request.getTrackingNotes());
        }

        shipment.setStatus("SHIPMENT_CREATED");
        return shipment;
    }

    @Override
    public Shipment trackShipment(Shipment shipment) {
        // For manual provider, tracking status is maintained in database
        return shipment;
    }

    @Override
    public Shipment updateStatus(Shipment shipment, String newStatus, String notes) {
        shipment.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            shipment.setTrackingNotes(notes);
        }
        if ("PICKED_UP".equalsIgnoreCase(newStatus) && shipment.getPickedUpAt() == null) {
            shipment.setPickedUpAt(LocalDateTime.now());
        } else if ("DELIVERED".equalsIgnoreCase(newStatus) && shipment.getDeliveredAt() == null) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }
        return shipment;
    }
}
