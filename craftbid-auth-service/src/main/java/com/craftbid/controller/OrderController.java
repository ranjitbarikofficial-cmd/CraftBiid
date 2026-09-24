package com.craftbid.controller;

import com.craftbid.dto.*;
import com.craftbid.entity.User;
import com.craftbid.service.OrderService;
import com.craftbid.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication required");
        }
        return userRepository.findByIdentifier(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyWonOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.getMyWonOrders(user));
    }

    @GetMapping("/artisan-orders")
    public ResponseEntity<List<OrderResponseDTO>> getArtisanOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.getArtisanOrders(user));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(Authentication authentication, @PathVariable Long orderId) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.getOrderById(orderId, user));
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<OrderResponseDTO> getOrderByAuctionId(Authentication authentication, @PathVariable Long auctionId) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.getOrderByAuctionId(auctionId, user));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponseDTO> getOrderByOrderNumber(Authentication authentication, @PathVariable String orderNumber) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber, user));
    }

    @PostMapping("/{orderId}/address")
    public ResponseEntity<OrderResponseDTO> submitDeliveryAddress(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody SubmitAddressRequest request) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.submitDeliveryAddress(orderId, request, user));
    }

    @PutMapping("/{orderId}/package")
    public ResponseEntity<OrderResponseDTO> updatePackageDetails(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody PackageDetailsDTO request) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.updatePackageDetails(orderId, request, user));
    }

    @PostMapping("/{orderId}/ready-to-ship")
    public ResponseEntity<OrderResponseDTO> markReadyToShip(
            Authentication authentication,
            @PathVariable Long orderId) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.markReadyToShip(orderId, user));
    }

    @PostMapping("/{orderId}/shipment")
    public ResponseEntity<OrderResponseDTO> createShipment(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateShipmentDTO request) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.createShipment(orderId, request, user));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request, user));
    }

    @GetMapping("/{orderId}/timeline")
    public ResponseEntity<List<TrackingTimelineDTO>> getOrderTimeline(
            Authentication authentication,
            @PathVariable Long orderId) {
        User user = getAuthenticatedUser(authentication);
        OrderResponseDTO order = orderService.getOrderById(orderId, user);
        return ResponseEntity.ok(order.getTimeline());
    }
}
