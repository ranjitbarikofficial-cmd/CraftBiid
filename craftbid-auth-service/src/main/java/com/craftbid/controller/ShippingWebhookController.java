package com.craftbid.controller;

import com.craftbid.dto.OrderResponseDTO;
import com.craftbid.dto.UpdateOrderStatusRequest;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Shipment;
import com.craftbid.repository.ShipmentRepository;
import com.craftbid.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
public class ShippingWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(ShippingWebhookController.class);

    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;

    public ShippingWebhookController(ShipmentRepository shipmentRepository, OrderService orderService) {
        this.shipmentRepository = shipmentRepository;
        this.orderService = orderService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleShippingWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("📦 Received Shipping Webhook: {}", payload);

        String trackingNumber = (String) payload.get("trackingNumber");
        if (trackingNumber == null || trackingNumber.isBlank()) {
            trackingNumber = (String) payload.get("awb");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Tracking number / AWB is required"));
        }

        String status = (String) payload.get("status");
        String notes = (String) payload.get("notes");
        String carrier = (String) payload.get("carrier");

        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber).orElse(null);
        if (shipment == null) {
            logger.warn("Shipment not found for tracking number: {}", trackingNumber);
            return ResponseEntity.ok(Map.of("status", "ignored", "message", "Shipment not found"));
        }

        AuctionOrder order = shipment.getOrder();
        if (order != null && status != null) {
            UpdateOrderStatusRequest updateReq = new UpdateOrderStatusRequest();
            updateReq.setStatus(status);
            updateReq.setTrackingNotes(notes != null ? notes : "Webhook auto update");
            updateReq.setCarrier(carrier);

            OrderResponseDTO updated = orderService.updateOrderStatus(order.getId(), updateReq, order.getArtisan());
            return ResponseEntity.ok(Map.of("status", "success", "orderId", updated.getId(), "orderStatus", updated.getStatus()));
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
