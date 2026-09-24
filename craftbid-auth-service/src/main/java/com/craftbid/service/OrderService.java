package com.craftbid.service;

import com.craftbid.dto.*;
import com.craftbid.entity.*;
import com.craftbid.repository.AuctionOrderRepository;
import com.craftbid.repository.AuctionRepository;
import com.craftbid.repository.UserRepository;
import com.craftbid.shipping.ShippingService;
import com.craftbid.websocket.AuctionEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final AuctionOrderRepository orderRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final ShippingService shippingService;
    private final SellerSettlementService settlementService;
    private final NotificationService notificationService;
    private final AuctionEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${craftbid.commission.platform-rate:10.00}")
    private BigDecimal platformCommissionRate = new BigDecimal("10.00");

    public OrderService(
            AuctionOrderRepository orderRepository,
            AuctionRepository auctionRepository,
            UserRepository userRepository,
            AddressService addressService,
            ShippingService shippingService,
            SellerSettlementService settlementService,
            NotificationService notificationService,
            AuctionEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.addressService = addressService;
        this.shippingService = shippingService;
        this.settlementService = settlementService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    public User getUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }

    /**
     * Atomically and idempotently creates the Winner AuctionOrder record when an auction ends.
     */
    @Transactional
    public synchronized AuctionOrder createWinnerOrder(Auction auction, User winner, BigDecimal winningAmount, BigDecimal platformFee, BigDecimal artisanPayout) {
        Optional<AuctionOrder> existing = orderRepository.findByAuction(auction);
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal rate = platformCommissionRate != null ? platformCommissionRate : new BigDecimal("10.00");
        if (platformFee == null) {
            platformFee = winningAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (artisanPayout == null) {
            artisanPayout = winningAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        }

        AuctionOrder order = new AuctionOrder();
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(auction.getSeller());
        order.setWinningAmount(winningAmount);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(winningAmount);
        order.setPlatformFee(platformFee);
        order.setArtisanPayout(artisanPayout);
        order.setStatus("ADDRESS_REQUIRED");
        order.setOrderNumber("CB-ORD-" + System.currentTimeMillis() + "-" + auction.getId());

        // Check if winner already has a default address
        try {
            List<AddressDTO> userAddresses = addressService.getUserAddresses(winner);
            if (!userAddresses.isEmpty()) {
                AddressDTO defaultAddr = userAddresses.stream().filter(AddressDTO::isDefault).findFirst().orElse(userAddresses.get(0));
                order.setFullName(defaultAddr.getFullName());
                order.setStreetAddress(defaultAddr.getAddressLine1() + (defaultAddr.getAddressLine2() != null ? ", " + defaultAddr.getAddressLine2() : ""));
                order.setCity(defaultAddr.getCity());
                order.setState(defaultAddr.getState());
                order.setPincode(defaultAddr.getPincode());
                order.setPhone(defaultAddr.getPhone());
                order.setLandmark(defaultAddr.getLandmark());
            } else {
                order.setFullName(winner.getName());
                order.setStreetAddress("Pending Buyer Address Submission");
                order.setCity(winner.getCity() != null ? winner.getCity() : "Pending");
                order.setState("India");
                order.setPincode("000000");
                order.setPhone(winner.getPhone() != null ? winner.getPhone() : "Pending");
            }
        } catch (Exception ignored) {}

        AuctionOrder savedOrder = orderRepository.save(order);

        // Initialize Shipment in PENDING status
        Shipment shipment = new Shipment(savedOrder);
        savedOrder.setShipment(shipment);

        // Initialize Seller Settlement in PENDING status
        settlementService.createPendingSettlement(savedOrder, auction.getSeller(), winningAmount, platformFee, artisanPayout);

        // Broadcast order created event
        try {
            eventPublisher.publishOrderEvent(savedOrder.getId(), "order:created", OrderResponseDTO.fromEntity(savedOrder));
        } catch (Exception ignored) {}

        return savedOrder;
    }

    /**
     * Winner submits or selects their delivery address
     */
    @Transactional
    public OrderResponseDTO submitDeliveryAddress(Long orderId, SubmitAddressRequest request, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        validateOrderBuyerOrAdmin(order, currentUser);

        if (!"ADDRESS_REQUIRED".equalsIgnoreCase(order.getStatus()) && !"ADDRESS_CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Cannot update address when order is in status: " + order.getStatus());
        }

        Address address = null;
        if (request.getAddressId() != null) {
            address = addressService.getAddressEntity(request.getAddressId(), currentUser)
                    .orElseThrow(() -> new RuntimeException("Selected address not found or unauthorized"));
        } else {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setFullName(request.getFullName() != null ? request.getFullName() : currentUser.getName());
            addressDTO.setPhone(request.getPhone() != null ? request.getPhone() : currentUser.getPhone());
            addressDTO.setAddressLine1(request.getAddressLine1());
            addressDTO.setAddressLine2(request.getAddressLine2());
            addressDTO.setCity(request.getCity());
            addressDTO.setState(request.getState());
            addressDTO.setPincode(request.getPincode());
            addressDTO.setLandmark(request.getLandmark());
            addressDTO.setDefault(Boolean.TRUE.equals(request.getSaveAsDefault()));

            address = addressService.createAddress(currentUser, addressDTO);
        }

        // Immutable snapshot into order record
        order.setShippingAddress(address);
        order.setFullName(address.getFullName());
        order.setPhone(address.getPhone());
        order.setStreetAddress(address.getAddressLine1() + (address.getAddressLine2() != null ? ", " + address.getAddressLine2() : ""));
        order.setCity(address.getCity());
        order.setState(address.getState());
        order.setPincode(address.getPincode());
        order.setLandmark(address.getLandmark());

        order.setStatus("ADDRESS_CONFIRMED");
        AuctionOrder saved = orderRepository.save(order);

        // Notify Artisan
        try {
            notificationService.notifyOrderAddressSubmitted(order.getArtisan(), order.getAuction().getCraft().getTitle(), order.getId());
        } catch (Exception ignored) {}

        // Publish WS Event
        OrderResponseDTO response = OrderResponseDTO.fromEntity(saved);
        try {
            eventPublisher.publishOrderEvent(saved.getId(), "order:address_confirmed", response);
        } catch (Exception ignored) {}

        return response;
    }

    /**
     * Artisan specifies package dimensions & weight
     */
    @Transactional
    public OrderResponseDTO updatePackageDetails(Long orderId, PackageDetailsDTO request, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        validateOrderArtisanOrAdmin(order, currentUser);

        Shipment shipment = shippingService.savePackageDetails(order, request);
        order.setShipment(shipment);
        if ("ADDRESS_CONFIRMED".equalsIgnoreCase(order.getStatus()) || "ADDRESS_REQUIRED".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("SELLER_PREPARING");
        }
        AuctionOrder saved = orderRepository.save(order);

        OrderResponseDTO response = OrderResponseDTO.fromEntity(saved);
        try {
            eventPublisher.publishOrderEvent(saved.getId(), "order:package_prepared", response);
        } catch (Exception ignored) {}

        return response;
    }

    /**
     * Artisan marks the order as READY_TO_SHIP
     */
    @Transactional
    public OrderResponseDTO markReadyToShip(Long orderId, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        validateOrderArtisanOrAdmin(order, currentUser);

        order.setStatus("READY_TO_SHIP");
        if (order.getShipment() != null) {
            order.getShipment().setStatus("READY_TO_SHIP");
        }
        AuctionOrder saved = orderRepository.save(order);

        try {
            notificationService.notifyOrderReadyToShip(order.getBuyer(), order.getAuction().getCraft().getTitle(), order.getId());
        } catch (Exception ignored) {}

        OrderResponseDTO response = OrderResponseDTO.fromEntity(saved);
        try {
            eventPublisher.publishOrderEvent(saved.getId(), "order:ready_to_ship", response);
        } catch (Exception ignored) {}

        return response;
    }

    /**
     * Artisan creates / books shipment with courier AWB details
     */
    @Transactional
    public OrderResponseDTO createShipment(Long orderId, CreateShipmentDTO request, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        validateOrderArtisanOrAdmin(order, currentUser);

        Shipment shipment = shippingService.createShipment(order, request, "MANUAL");
        order.setShipment(shipment);
        order.setStatus("SHIPMENT_CREATED");
        AuctionOrder saved = orderRepository.save(order);

        try {
            notificationService.notifyOrderShipmentCreated(
                    order.getBuyer(),
                    order.getAuction().getCraft().getTitle(),
                    request.getCourierName(),
                    request.getTrackingNumber(),
                    order.getId()
            );
        } catch (Exception ignored) {}

        OrderResponseDTO response = OrderResponseDTO.fromEntity(saved);
        try {
            eventPublisher.publishOrderEvent(saved.getId(), "order:shipment_created", response);
        } catch (Exception ignored) {}

        return response;
    }

    /**
     * Update order & shipment status in state machine (PICKUP_SCHEDULED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, COMPLETED, etc.)
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String newStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : "";

        // Buyer can mark COMPLETED after DELIVERED. Artisan/Admin can update transit states.
        if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            validateOrderBuyerOrAdmin(order, currentUser);
        } else {
            validateOrderArtisanOrAdmin(order, currentUser);
        }

        order.setStatus(newStatus);

        Shipment shipment = order.getShipment();
        if (shipment != null) {
            shipment = shippingService.updateShipmentStatus(shipment, newStatus, request.getTrackingNotes());
            if (request.getCarrier() != null && !request.getCarrier().isBlank()) {
                shipment.setCourierName(request.getCarrier());
            }
            order.setShipment(shipment);
        }

        // If DELIVERED, automatically release Seller Settlement!
        if ("DELIVERED".equalsIgnoreCase(newStatus) || "COMPLETED".equalsIgnoreCase(newStatus)) {
            settlementService.processDeliverySettlement(order);
            try {
                notificationService.notifyOrderDelivered(order.getBuyer(), order.getAuction().getCraft().getTitle(), order.getId());
            } catch (Exception ignored) {}
        } else if ("PICKED_UP".equalsIgnoreCase(newStatus)) {
            try {
                notificationService.notifyOrderShipped(
                        order.getBuyer(),
                        order.getAuction().getCraft().getTitle(),
                        request.getTrackingNotes(),
                        request.getCarrier() != null ? request.getCarrier() : (shipment != null ? shipment.getCourierName() : "Courier"),
                        order.getId()
                );
            } catch (Exception ignored) {}
        }

        AuctionOrder saved = orderRepository.save(order);
        OrderResponseDTO response = OrderResponseDTO.fromEntity(saved);

        try {
            eventPublisher.publishOrderEvent(saved.getId(), "order:status_updated", response);
        } catch (Exception ignored) {}

        return response;
    }

    public OrderResponseDTO getOrderById(Long orderId, User currentUser) {
        AuctionOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        validateOrderPartyOrAdmin(order, currentUser);
        return OrderResponseDTO.fromEntity(order);
    }

    public OrderResponseDTO getOrderByAuctionId(Long auctionId, User currentUser) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        AuctionOrder order = orderRepository.findByAuction(auction)
                .orElseThrow(() -> new RuntimeException("Order not yet created for auction: " + auctionId));

        validateOrderPartyOrAdmin(order, currentUser);
        return OrderResponseDTO.fromEntity(order);
    }

    public OrderResponseDTO getOrderByOrderNumber(String orderNumber, User currentUser) {
        AuctionOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));

        validateOrderPartyOrAdmin(order, currentUser);
        return OrderResponseDTO.fromEntity(order);
    }

    public List<OrderResponseDTO> getMyWonOrders(User currentUser) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(currentUser).stream()
                .map(OrderResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getArtisanOrders(User currentUser) {
        return orderRepository.findByArtisanOrderByCreatedAtDesc(currentUser).stream()
                .map(OrderResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getAllOrdersAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OrderResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // IDOR Security Validations
    private void validateOrderBuyerOrAdmin(AuctionOrder order, User user) {
        if (isAdmin(user)) return;
        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You are not the winner/buyer of this order");
        }
    }

    private void validateOrderArtisanOrAdmin(AuctionOrder order, User user) {
        if (isAdmin(user)) return;
        if (!order.getArtisan().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You are not the artisan/seller of this order");
        }
    }

    private void validateOrderPartyOrAdmin(AuctionOrder order, User user) {
        if (isAdmin(user)) return;
        if (!order.getBuyer().getId().equals(user.getId()) && !order.getArtisan().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not have permission to view this order");
        }
    }

    private boolean isAdmin(User user) {
        if (user == null) return false;
        return user.getRole() == Role.ADMIN;
    }
}
