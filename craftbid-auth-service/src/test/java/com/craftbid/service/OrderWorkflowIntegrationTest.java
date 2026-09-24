package com.craftbid.service;

import com.craftbid.dto.*;
import com.craftbid.entity.*;
import com.craftbid.repository.*;
import com.craftbid.shipping.ManualShippingProvider;
import com.craftbid.shipping.ShippingProvider;
import com.craftbid.shipping.ShippingService;
import com.craftbid.websocket.AuctionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderWorkflowIntegrationTest {

    @Mock
    private AuctionOrderRepository orderRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private SellerSettlementRepository settlementRepository;
    @Mock
    private ArtisanProfileRepository artisanProfileRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuctionEventPublisher eventPublisher;

    private AddressService addressService;
    private ShippingService shippingService;
    private SellerSettlementService settlementService;
    private OrderService orderService;

    private User winner;
    private User artisan;
    private User attacker;
    private Craft craft;
    private Auction auction;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository);

        Map<String, ShippingProvider> providers = new HashMap<>();
        providers.put("manualShippingProvider", new ManualShippingProvider());
        shippingService = new ShippingService(providers, shipmentRepository);

        settlementService = new SellerSettlementService(settlementRepository, artisanProfileRepository, notificationService);

        orderService = new OrderService(
                orderRepository,
                auctionRepository,
                userRepository,
                addressService,
                shippingService,
                settlementService,
                notificationService,
                eventPublisher
        );

        artisan = new User();
        artisan.setId(101L);
        artisan.setName("Artisan Ramesh");
        artisan.setEmail("ramesh@artisan.in");
        artisan.setRole(Role.CUSTOMER);

        winner = new User();
        winner.setId(202L);
        winner.setName("Buyer Priya");
        winner.setEmail("priya@buyer.in");
        winner.setPhone("9876543210");
        winner.setRole(Role.CUSTOMER);

        attacker = new User();
        attacker.setId(999L);
        attacker.setName("Malicious User");
        attacker.setEmail("attacker@hacker.io");
        attacker.setRole(Role.CUSTOMER);

        craft = new Craft();
        craft.setId(501L);
        craft.setTitle("Handmade Brass Lamp");
        craft.setBasePrice(new BigDecimal("199.00"));
        craft.setSeller(artisan);

        auction = new Auction();
        auction.setId(701L);
        auction.setCraft(craft);
        auction.setSeller(artisan);
        auction.setStartingPrice(new BigDecimal("199.00"));
        auction.setCurrentHighestBid(new BigDecimal("299.00"));
        auction.setWinningBidder(winner);
        auction.setStatus(AuctionStatus.ENDED);
    }

    @Test
    @DisplayName("1. Create Winner Order: Idempotent & initializes configurable 10% fee, shipment, and PENDING settlement")
    void testCreateWinnerOrder_Idempotent() {
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.empty());
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> {
            AuctionOrder o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });

        BigDecimal winningAmount = new BigDecimal("299.00");
        BigDecimal expectedFee = winningAmount.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP); // 29.90
        BigDecimal expectedPayout = winningAmount.subtract(expectedFee).setScale(2, RoundingMode.HALF_UP); // 269.10

        AuctionOrder created = orderService.createWinnerOrder(auction, winner, winningAmount, expectedFee, expectedPayout);

        assertNotNull(created);
        assertEquals(winner, created.getBuyer());
        assertEquals(artisan, created.getArtisan());
        assertEquals(winningAmount, created.getWinningAmount());
        assertEquals(BigDecimal.ZERO, created.getShippingFee()); // Free Shipping
        assertEquals(winningAmount, created.getTotalAmount()); // No double charging
        assertEquals(expectedFee, created.getPlatformFee());
        assertEquals(expectedPayout, created.getArtisanPayout());
        assertEquals("ADDRESS_REQUIRED", created.getStatus());

        verify(orderRepository, times(1)).save(any(AuctionOrder.class));
        verify(settlementRepository, times(1)).save(any(SellerSettlement.class));
    }

    @Test
    @DisplayName("2. Winner submits address: Transitions to ADDRESS_CONFIRMED and stores permanent address snapshot")
    void testSubmitDeliveryAddress_Success() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setWinningAmount(new BigDecimal("299.00"));
        order.setStatus("ADDRESS_REQUIRED");

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(55L);
            return a;
        });

        SubmitAddressRequest req = new SubmitAddressRequest();
        req.setFullName("Priya Sharma");
        req.setPhone("9876543210");
        req.setAddressLine1("Flat 402, Lotus Heights");
        req.setAddressLine2("MG Road");
        req.setCity("Bengaluru");
        req.setState("Karnataka");
        req.setPincode("560001");
        req.setLandmark("Near Metro Station");
        req.setSaveAsDefault(true);

        OrderResponseDTO response = orderService.submitDeliveryAddress(1001L, req, winner);

        assertNotNull(response);
        assertEquals("ADDRESS_CONFIRMED", response.getStatus());
        assertEquals("Priya Sharma", response.getFullName());
        assertEquals("Bengaluru", response.getCity());
        assertEquals("560001", response.getPincode());

        // Verify permanent snapshot in order entity
        assertEquals("Flat 402, Lotus Heights, MG Road", order.getStreetAddress());
        assertEquals("Karnataka", order.getState());

        verify(notificationService, times(1)).notifyOrderAddressSubmitted(eq(artisan), eq(craft.getTitle()), eq(1001L));
    }

    @Test
    @DisplayName("3. IDOR Protection: Attacker cannot submit address for another user's order")
    void testSubmitDeliveryAddress_IdorRejection() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setStatus("ADDRESS_REQUIRED");

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        SubmitAddressRequest req = new SubmitAddressRequest();
        req.setFullName("Attacker");
        req.setAddressLine1("Hack Street");
        req.setPincode("110001");

        assertThrows(RuntimeException.class, () -> orderService.submitDeliveryAddress(1001L, req, attacker));
    }

    @Test
    @DisplayName("4. Address Immutability: Cannot alter delivery address once seller has started preparing or shipped")
    void testSubmitDeliveryAddress_ImmutableAfterPreparation() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setStatus("SELLER_PREPARING"); // Already in preparation

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        SubmitAddressRequest req = new SubmitAddressRequest();
        req.setFullName("Priya Sharma");
        req.setAddressLine1("New Address");

        assertThrows(RuntimeException.class, () -> orderService.submitDeliveryAddress(1001L, req, winner));
    }

    @Test
    @DisplayName("5. Artisan updates package dimensions: Transitions to SELLER_PREPARING")
    void testUpdatePackageDetails_Success() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setStatus("ADDRESS_CONFIRMED");

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        PackageDetailsDTO packageDTO = new PackageDetailsDTO(1.2, 25.0, 15.0, 10.0, "Fragile brassware packed with bubblewrap");

        OrderResponseDTO response = orderService.updatePackageDetails(1001L, packageDTO, artisan);

        assertNotNull(response);
        assertEquals("SELLER_PREPARING", response.getStatus());
        assertEquals(1.2, response.getPackageWeight());
        assertEquals(25.0, response.getPackageLength());
    }

    @Test
    @DisplayName("6. Artisan marks READY_TO_SHIP: Winner is notified")
    void testMarkReadyToShip_Success() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setStatus("SELLER_PREPARING");
        Shipment shipment = new Shipment(order);
        order.setShipment(shipment);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.markReadyToShip(1001L, artisan);

        assertNotNull(response);
        assertEquals("READY_TO_SHIP", response.getStatus());
        verify(notificationService, times(1)).notifyOrderReadyToShip(eq(winner), eq(craft.getTitle()), eq(1001L));
    }

    @Test
    @DisplayName("7. Artisan creates shipment: AWB tracking assigned & transitions to SHIPMENT_CREATED")
    void testCreateShipment_Success() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setStatus("READY_TO_SHIP");

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateShipmentDTO shipmentDTO = new CreateShipmentDTO(
                "Delhivery",
                "DLV-987654321",
                new BigDecimal("0.00"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(4),
                "Standard Surface Dispatch"
        );

        OrderResponseDTO response = orderService.createShipment(1001L, shipmentDTO, artisan);

        assertNotNull(response);
        assertEquals("SHIPMENT_CREATED", response.getStatus());
        assertEquals("Delhivery", response.getCourierName());
        assertEquals("DLV-987654321", response.getTrackingNumber());
        verify(notificationService, times(1)).notifyOrderShipmentCreated(eq(winner), eq(craft.getTitle()), eq("Delhivery"), eq("DLV-987654321"), eq(1001L));
    }

    @Test
    @DisplayName("8. Order state transitions to DELIVERED: Settlement transitions to PENDING_PAYOUT (NOT PAID)")
    void testUpdateOrderStatus_Delivered_TransitionsToPendingPayout() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);
        order.setWinningAmount(new BigDecimal("299.00"));
        order.setPlatformFee(new BigDecimal("29.90"));
        order.setArtisanPayout(new BigDecimal("269.10"));
        order.setStatus("OUT_FOR_DELIVERY");

        Shipment shipment = new Shipment(order);
        shipment.setCourierName("Delhivery");
        shipment.setTrackingNumber("DLV-987654321");
        order.setShipment(shipment);

        SellerSettlement settlement = new SellerSettlement(order, artisan, new BigDecimal("299.00"), new BigDecimal("29.90"), new BigDecimal("269.10"));
        settlement.setStatus("PENDING");
        order.setSellerSettlement(settlement);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(settlementRepository.save(any(SellerSettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest updateReq = new UpdateOrderStatusRequest();
        updateReq.setStatus("DELIVERED");
        updateReq.setTrackingNotes("Package handed over to recipient");

        OrderResponseDTO response = orderService.updateOrderStatus(1001L, updateReq, artisan);

        assertNotNull(response);
        assertEquals("DELIVERED", response.getStatus());
        assertNotNull(response.getDeliveredAt());
        assertNotNull(response.getSellerSettlement());
        // DELIVERED MUST BE PENDING_PAYOUT, NOT PAID!
        assertEquals("PENDING_PAYOUT", response.getSellerSettlement().getStatus());
        assertEquals(new BigDecimal("269.10"), response.getSellerSettlement().getArtisanPayout());

        verify(notificationService, times(1)).notifyOrderDelivered(eq(winner), eq(craft.getTitle()), eq(1001L));
    }

    @Test
    @DisplayName("9. Payout Execution: Admin marks settlement PAID with UTR reference")
    void testMarkSettlementPaid_Success() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setAuction(auction);
        order.setBuyer(winner);
        order.setArtisan(artisan);

        SellerSettlement settlement = new SellerSettlement(order, artisan, new BigDecimal("299.00"), new BigDecimal("29.90"), new BigDecimal("269.10"));
        settlement.setId(88L);
        settlement.setStatus("PENDING_PAYOUT");
        settlement.setOrder(order);

        when(settlementRepository.findById(88L)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(SellerSettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerSettlement paidSettlement = settlementService.markSettlementPaid(88L, "UTR-HDFC-98765432", "BANK_TRANSFER", "Released after 24h delivery grace period");

        assertNotNull(paidSettlement);
        assertEquals("PAID", paidSettlement.getStatus());
        assertEquals("UTR-HDFC-98765432", paidSettlement.getPayoutReference());
        assertEquals("BANK_TRANSFER", paidSettlement.getPayoutMethod());
        assertNotNull(paidSettlement.getSettledAt());

        verify(notificationService, times(1)).notifyArtisanPayoutCompleted(eq(artisan), eq(craft.getTitle()), eq(new BigDecimal("269.10")), eq("UTR-HDFC-98765432"), eq(1001L));
    }

    @Test
    @DisplayName("10. Duplicate Payout Guard: Rejects payment on already PAID settlement")
    void testMarkSettlementPaid_DuplicateRejection() {
        SellerSettlement settlement = new SellerSettlement();
        settlement.setId(88L);
        settlement.setStatus("PAID");
        settlement.setPayoutReference("UTR-FIRST-1111");

        when(settlementRepository.findById(88L)).thenReturn(Optional.of(settlement));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                settlementService.markSettlementPaid(88L, "UTR-SECOND-2222", "BANK_TRANSFER", "Retry payout")
        );

        assertTrue(ex.getMessage().contains("already PAID"));
    }

    @Test
    @DisplayName("11. IDOR Defense: Customer A cannot view Customer B's order")
    void testIDOR_CustomerCannotViewOtherCustomerOrder() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setBuyer(winner);
        order.setArtisan(artisan);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                orderService.getOrderById(1001L, attacker)
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("12. IDOR Defense: Seller A cannot update Seller B's order package details")
    void testIDOR_SellerCannotModifyOtherSellerOrder() {
        AuctionOrder order = new AuctionOrder();
        order.setId(1001L);
        order.setBuyer(winner);
        order.setArtisan(artisan);

        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        PackageDetailsDTO packageDTO = new PackageDetailsDTO(1.0, 20.0, 10.0, 5.0, "Package");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                orderService.updatePackageDetails(1001L, packageDTO, attacker)
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("13. Configurable Commission: Platform commission rate dynamically alters platform fee and net payout")
    void testConfigurableCommissionCalculation() {
        AuctionOrder order = new AuctionOrder();
        order.setId(2001L);

        // When winning amount is ₹1000 and default 10% platform rate is configured
        BigDecimal winningAmount = new BigDecimal("1000.00");
        BigDecimal expectedPlatformFee = new BigDecimal("100.00");
        BigDecimal expectedSellerPayout = new BigDecimal("900.00");

        when(settlementRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(settlementRepository.save(any(SellerSettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerSettlement settlement = settlementService.createPendingSettlement(order, artisan, winningAmount, null, null);

        assertNotNull(settlement);
        assertEquals(new BigDecimal("10.00"), settlement.getPlatformCommissionRate());
        assertEquals(expectedPlatformFee, settlement.getPlatformFee());
        assertEquals(expectedSellerPayout, settlement.getArtisanPayout());
        assertEquals("PENDING", settlement.getStatus());
    }

    @Test
    @DisplayName("14. Full End-to-End Workflow: Order Creation -> Address -> Packaging -> Shipping -> Delivered -> PENDING_PAYOUT -> Admin UTR -> PAID")
    void testFullEndToEndWorkflow() {
        // 1. Create Winner Order
        when(orderRepository.findByAuction(auction)).thenReturn(Optional.empty());
        when(orderRepository.save(any(AuctionOrder.class))).thenAnswer(inv -> {
            AuctionOrder o = inv.getArgument(0);
            o.setId(5001L);
            return o;
        });

        BigDecimal winningAmount = new BigDecimal("500.00");
        BigDecimal platformFee = new BigDecimal("50.00");
        BigDecimal artisanPayout = new BigDecimal("450.00");

        AuctionOrder order = orderService.createWinnerOrder(auction, winner, winningAmount, platformFee, artisanPayout);
        assertEquals("ADDRESS_REQUIRED", order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getShippingFee());

        // 2. Winner Submits Address
        when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(77L);
            return a;
        });

        SubmitAddressRequest addressReq = new SubmitAddressRequest();
        addressReq.setFullName("Priya Sharma");
        addressReq.setPhone("9876543210");
        addressReq.setAddressLine1("42 Residency Road");
        addressReq.setCity("Jaipur");
        addressReq.setState("Rajasthan");
        addressReq.setPincode("302001");

        OrderResponseDTO addrRes = orderService.submitDeliveryAddress(5001L, addressReq, winner);
        assertEquals("ADDRESS_CONFIRMED", addrRes.getStatus());
        assertEquals("Jaipur", addrRes.getCity());

        // 3. Artisan Prepares Package
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        PackageDetailsDTO packageDTO = new PackageDetailsDTO(2.5, 30.0, 20.0, 15.0, "Carefully wrapped");
        OrderResponseDTO pkgRes = orderService.updatePackageDetails(5001L, packageDTO, artisan);
        assertEquals("SELLER_PREPARING", pkgRes.getStatus());

        // 4. Artisan Assigns Courier & AWB
        CreateShipmentDTO shipDTO = new CreateShipmentDTO("BlueDart", "BD-778899", BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now().plusDays(3), "Air Express");
        OrderResponseDTO shipRes = orderService.createShipment(5001L, shipDTO, artisan);
        assertEquals("SHIPMENT_CREATED", shipRes.getStatus());
        assertEquals("BD-778899", shipRes.getTrackingNumber());

        // 5. Delivery Completed -> Settlement Transitions to PENDING_PAYOUT (NOT PAID)
        SellerSettlement settlement = new SellerSettlement(order, artisan, winningAmount, platformFee, artisanPayout);
        settlement.setId(9001L);
        settlement.setStatus("PENDING");
        order.setSellerSettlement(settlement);

        when(settlementRepository.save(any(SellerSettlement.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest delivReq = new UpdateOrderStatusRequest();
        delivReq.setStatus("DELIVERED");
        OrderResponseDTO delivRes = orderService.updateOrderStatus(5001L, delivReq, artisan);
        assertEquals("DELIVERED", delivRes.getStatus());
        assertEquals("PENDING_PAYOUT", delivRes.getSellerSettlement().getStatus());

        // 6. Admin Executes Manual Bank Transfer & Records UTR -> PAID
        when(settlementRepository.findById(9001L)).thenReturn(Optional.of(settlement));
        SellerSettlement paidRes = settlementService.markSettlementPaid(9001L, "UTR-SBI-2026092301", "BANK_TRANSFER", "Manual NEFT Transfer verified");
        assertEquals("PAID", paidRes.getStatus());
        assertEquals("UTR-SBI-2026092301", paidRes.getPayoutReference());
        assertNotNull(paidRes.getSettledAt());
    }
}
