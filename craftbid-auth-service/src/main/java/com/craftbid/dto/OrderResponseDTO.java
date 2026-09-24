package com.craftbid.dto;

import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Craft;
import com.craftbid.entity.Shipment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponseDTO {

    private Long id;
    private String orderNumber;
    private Long auctionId;
    private Long craftId;
    private String craftTitle;
    private String craftImageUrl;
    private String craftDescription;
    private BigDecimal craftStartingPrice;

    private Long buyerId;
    private String buyerName;
    private String buyerEmail;

    private Long artisanId;
    private String artisanName;
    private String artisanEmail;

    private BigDecimal winningAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal artisanPayout;

    private String status;
    private String statusDisplay;

    private AddressDTO shippingAddress;
    private String fullName;
    private String streetAddress;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String landmark;

    // Shipment info
    private String shippingProvider;
    private String courierName;
    private String trackingNumber;
    private Double packageWeight;
    private Double packageLength;
    private Double packageWidth;
    private Double packageHeight;
    private BigDecimal shippingCost;
    private LocalDateTime pickupDate;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private String shipmentStatus;
    private String trackingNotes;

    // Settlement info
    private SellerSettlementDTO sellerSettlement;

    // Timeline
    private List<TrackingTimelineDTO> timeline = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponseDTO() {
    }

    public static OrderResponseDTO fromEntity(AuctionOrder order) {
        if (order == null) return null;
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        if (order.getAuction() != null) {
            dto.setAuctionId(order.getAuction().getId());
            Craft craft = order.getAuction().getCraft();
            if (craft != null) {
                dto.setCraftId(craft.getId());
                dto.setCraftTitle(craft.getTitle());
                dto.setCraftImageUrl(craft.getImageUrl());
                dto.setCraftDescription(craft.getDescription());
                dto.setCraftStartingPrice(craft.getBasePrice());
            }
        }
        if (order.getBuyer() != null) {
            dto.setBuyerId(order.getBuyer().getId());
            dto.setBuyerName(order.getBuyer().getName());
            dto.setBuyerEmail(order.getBuyer().getEmail());
        }
        if (order.getArtisan() != null) {
            dto.setArtisanId(order.getArtisan().getId());
            dto.setArtisanName(order.getArtisan().getName());
            dto.setArtisanEmail(order.getArtisan().getEmail());
        }
        dto.setWinningAmount(order.getWinningAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : order.getWinningAmount());
        dto.setPlatformFee(order.getPlatformFee());
        dto.setArtisanPayout(order.getArtisanPayout());
        dto.setStatus(order.getStatus());
        dto.setStatusDisplay(formatStatusDisplay(order.getStatus()));

        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(AddressDTO.fromEntity(order.getShippingAddress()));
        }
        dto.setFullName(order.getFullName());
        dto.setStreetAddress(order.getStreetAddress());
        dto.setCity(order.getCity());
        dto.setState(order.getState());
        dto.setPincode(order.getPincode());
        dto.setPhone(order.getPhone());
        dto.setLandmark(order.getLandmark());

        Shipment shipment = order.getShipment();
        if (shipment != null) {
            dto.setShippingProvider(shipment.getProvider());
            dto.setCourierName(shipment.getCourierName());
            dto.setTrackingNumber(shipment.getTrackingNumber());
            dto.setPackageWeight(shipment.getWeight());
            dto.setPackageLength(shipment.getLength());
            dto.setPackageWidth(shipment.getWidth());
            dto.setPackageHeight(shipment.getHeight());
            dto.setShippingCost(shipment.getShippingCost());
            dto.setPickupDate(shipment.getPickupDate());
            dto.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
            dto.setPickedUpAt(shipment.getPickedUpAt());
            dto.setDeliveredAt(shipment.getDeliveredAt());
            dto.setShipmentStatus(shipment.getStatus());
            dto.setTrackingNotes(shipment.getTrackingNotes());
        }

        if (order.getSellerSettlement() != null) {
            dto.setSellerSettlement(SellerSettlementDTO.fromEntity(order.getSellerSettlement()));
        }

        dto.setTimeline(buildTimeline(order));
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        return dto;
    }

    private static String formatStatusDisplay(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "ADDRESS_REQUIRED" -> "Address Required";
            case "ADDRESS_CONFIRMED" -> "Address Confirmed";
            case "SELLER_PREPARING" -> "Seller Preparing Package";
            case "READY_TO_SHIP" -> "Ready for Shipping";
            case "SHIPMENT_CREATED" -> "Shipment Booked";
            case "PICKUP_SCHEDULED" -> "Pickup Scheduled";
            case "PICKED_UP" -> "Picked Up";
            case "IN_TRANSIT" -> "In Transit";
            case "OUT_FOR_DELIVERY" -> "Out for Delivery";
            case "DELIVERED" -> "Delivered";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            case "RTO" -> "Returned to Origin";
            case "DELIVERY_FAILED" -> "Delivery Failed";
            default -> status.replace('_', ' ');
        };
    }

    public static List<TrackingTimelineDTO> buildTimeline(AuctionOrder order) {
        List<TrackingTimelineDTO> list = new ArrayList<>();
        String status = order.getStatus() != null ? order.getStatus() : "ADDRESS_REQUIRED";

        String[] stages = {
            "ADDRESS_REQUIRED",
            "ADDRESS_CONFIRMED",
            "SELLER_PREPARING",
            "READY_TO_SHIP",
            "SHIPMENT_CREATED",
            "PICKUP_SCHEDULED",
            "PICKED_UP",
            "IN_TRANSIT",
            "OUT_FOR_DELIVERY",
            "DELIVERED"
        };

        String[] titles = {
            "Auction Won",
            "Delivery Address Provided",
            "Package Prepared",
            "Ready to Ship",
            "Shipment Booked",
            "Pickup Scheduled",
            "Package Picked Up",
            "In Transit",
            "Out for Delivery",
            "Delivered"
        };

        String[] descriptions = {
            "Winner finalized. Please confirm delivery address.",
            "Delivery address verified and shared with artisan.",
            "Artisan packed the craft with dimensions & weight.",
            "Shipment label ready for courier dispatch.",
            "Courier assigned with AWB tracking number.",
            "Courier pickup scheduled from artisan.",
            "Package in courier network.",
            "Package in transit towards destination hub.",
            "Courier executive is out for delivery.",
            "Craft safely delivered to winner."
        };

        int currentStageIdx = 0;
        for (int i = 0; i < stages.length; i++) {
            if (stages[i].equalsIgnoreCase(status)) {
                currentStageIdx = i;
                break;
            }
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            currentStageIdx = stages.length - 1;
        }

        for (int i = 0; i < stages.length; i++) {
            boolean completed = i < currentStageIdx || "DELIVERED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
            boolean current = i == currentStageIdx && !"COMPLETED".equalsIgnoreCase(status);
            LocalDateTime ts = null;
            if (i == 0) ts = order.getCreatedAt();
            else if (completed || current) {
                if (order.getShipment() != null) {
                    if (stages[i].equals("PICKED_UP")) ts = order.getShipment().getPickedUpAt();
                    else if (stages[i].equals("DELIVERED")) ts = order.getShipment().getDeliveredAt();
                    else ts = order.getUpdatedAt();
                } else {
                    ts = order.getUpdatedAt();
                }
            }
            list.add(new TrackingTimelineDTO(stages[i], titles[i], descriptions[i], ts, completed, current));
        }

        return list;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getCraftId() {
        return craftId;
    }

    public void setCraftId(Long craftId) {
        this.craftId = craftId;
    }

    public String getCraftTitle() {
        return craftTitle;
    }

    public void setCraftTitle(String craftTitle) {
        this.craftTitle = craftTitle;
    }

    public String getCraftImageUrl() {
        return craftImageUrl;
    }

    public void setCraftImageUrl(String craftImageUrl) {
        this.craftImageUrl = craftImageUrl;
    }

    public String getCraftDescription() {
        return craftDescription;
    }

    public void setCraftDescription(String craftDescription) {
        this.craftDescription = craftDescription;
    }

    public BigDecimal getCraftStartingPrice() {
        return craftStartingPrice;
    }

    public void setCraftStartingPrice(BigDecimal craftStartingPrice) {
        this.craftStartingPrice = craftStartingPrice;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public Long getArtisanId() {
        return artisanId;
    }

    public void setArtisanId(Long artisanId) {
        this.artisanId = artisanId;
    }

    public String getArtisanName() {
        return artisanName;
    }

    public void setArtisanName(String artisanName) {
        this.artisanName = artisanName;
    }

    public String getArtisanEmail() {
        return artisanEmail;
    }

    public void setArtisanEmail(String artisanEmail) {
        this.artisanEmail = artisanEmail;
    }

    public BigDecimal getWinningAmount() {
        return winningAmount;
    }

    public void setWinningAmount(BigDecimal winningAmount) {
        this.winningAmount = winningAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getArtisanPayout() {
        return artisanPayout;
    }

    public void setArtisanPayout(BigDecimal artisanPayout) {
        this.artisanPayout = artisanPayout;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }

    public AddressDTO getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressDTO shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(String shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Double getPackageWeight() {
        return packageWeight;
    }

    public void setPackageWeight(Double packageWeight) {
        this.packageWeight = packageWeight;
    }

    public Double getPackageLength() {
        return packageLength;
    }

    public void setPackageLength(Double packageLength) {
        this.packageLength = packageLength;
    }

    public Double getPackageWidth() {
        return packageWidth;
    }

    public void setPackageWidth(Double packageWidth) {
        this.packageWidth = packageWidth;
    }

    public Double getPackageHeight() {
        return packageHeight;
    }

    public void setPackageHeight(Double packageHeight) {
        this.packageHeight = packageHeight;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public LocalDateTime getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDateTime pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getTrackingNotes() {
        return trackingNotes;
    }

    public void setTrackingNotes(String trackingNotes) {
        this.trackingNotes = trackingNotes;
    }

    public SellerSettlementDTO getSellerSettlement() {
        return sellerSettlement;
    }

    public void setSellerSettlement(SellerSettlementDTO sellerSettlement) {
        this.sellerSettlement = sellerSettlement;
    }

    public List<TrackingTimelineDTO> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TrackingTimelineDTO> timeline) {
        this.timeline = timeline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
