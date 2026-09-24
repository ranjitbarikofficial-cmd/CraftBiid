package com.craftbid.shipping;

import com.craftbid.dto.CreateShipmentDTO;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.Shipment;

public interface ShippingProvider {

    String getProviderName();

    Shipment createShipment(AuctionOrder order, CreateShipmentDTO request);

    Shipment trackShipment(Shipment shipment);

    Shipment updateStatus(Shipment shipment, String newStatus, String notes);
}
