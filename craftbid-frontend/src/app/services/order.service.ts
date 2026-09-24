import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-config';

export interface AddressDTO {
  id?: number;
  userId?: number;
  fullName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  streetAddress?: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  isDefault?: boolean;
  createdAt?: string;
}

export interface TrackingTimelineDTO {
  stepKey: string;
  title: string;
  description: string;
  timestamp?: string;
  completed: boolean;
  current: boolean;
}

export interface SellerSettlementDTO {
  id: number;
  orderId: number;
  orderNumber: string;
  artisanId: number;
  artisanName: string;
  winningAmount: number;
  platformCommissionRate: number;
  platformFee: number;
  artisanPayout: number;
  status: string; // PENDING, PAID
  payoutReference?: string;
  payoutMethod?: string;
  settledAt?: string;
  notes?: string;
  createdAt: string;
}

export interface OrderResponseDTO {
  id: number;
  orderNumber: string;
  auctionId: number;
  craftId: number;
  craftTitle: string;
  craftImageUrl: string;
  craftDescription: string;
  craftStartingPrice: number;
  buyerId: number;
  buyerName: string;
  buyerEmail: string;
  artisanId: number;
  artisanName: string;
  artisanEmail: string;
  winningAmount: number;
  shippingFee: number;
  totalAmount: number;
  platformFee: number;
  artisanPayout: number;
  status: string;
  statusDisplay: string;
  shippingAddress?: AddressDTO;
  fullName?: string;
  streetAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  phone?: string;
  landmark?: string;
  shippingProvider?: string;
  courierName?: string;
  trackingNumber?: string;
  packageWeight?: number;
  packageLength?: number;
  packageWidth?: number;
  packageHeight?: number;
  shippingCost?: number;
  pickupDate?: string;
  estimatedDeliveryDate?: string;
  pickedUpAt?: string;
  deliveredAt?: string;
  shipmentStatus?: string;
  trackingNotes?: string;
  sellerSettlement?: SellerSettlementDTO;
  timeline: TrackingTimelineDTO[];
  createdAt: string;
  updatedAt?: string;
}

export interface SubmitAddressRequest {
  addressId?: number;
  fullName?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  streetAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  landmark?: string;
  saveAsDefault?: boolean;
}

export interface PackageDetailsRequest {
  weight: number;
  length: number;
  width: number;
  height: number;
  notes?: string;
}

export interface CreateShipmentRequest {
  courierName: string;
  trackingNumber: string;
  shippingCost?: number;
  pickupDate?: string;
  estimatedDeliveryDate?: string;
  trackingNotes?: string;
}

export interface UpdateOrderStatusRequest {
  status: string;
  trackingNotes?: string;
  carrier?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(private http: HttpClient) {}

  private get baseUrl(): string {
    return `${getApiBaseUrl()}/api/orders`;
  }

  private get addressUrl(): string {
    return `${getApiBaseUrl()}/api/addresses`;
  }

  getMyOrders(): Observable<OrderResponseDTO[]> {
    return this.http.get<OrderResponseDTO[]>(`${this.baseUrl}/my-orders`);
  }

  getArtisanOrders(): Observable<OrderResponseDTO[]> {
    return this.http.get<OrderResponseDTO[]>(`${this.baseUrl}/artisan-orders`);
  }

  getOrderById(orderId: number): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.baseUrl}/${orderId}`);
  }

  getOrderByAuctionId(auctionId: number): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.baseUrl}/auction/${auctionId}`);
  }

  getOrderByNumber(orderNumber: string): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.baseUrl}/number/${orderNumber}`);
  }

  submitDeliveryAddress(orderId: number, data: SubmitAddressRequest): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.baseUrl}/${orderId}/address`, data);
  }

  updatePackageDetails(orderId: number, data: PackageDetailsRequest): Observable<OrderResponseDTO> {
    return this.http.put<OrderResponseDTO>(`${this.baseUrl}/${orderId}/package`, data);
  }

  markReadyToShip(orderId: number): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.baseUrl}/${orderId}/ready-to-ship`, {});
  }

  createShipment(orderId: number, data: CreateShipmentRequest): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.baseUrl}/${orderId}/shipment`, data);
  }

  updateOrderStatus(orderId: number, data: UpdateOrderStatusRequest): Observable<OrderResponseDTO> {
    return this.http.patch<OrderResponseDTO>(`${this.baseUrl}/${orderId}/status`, data);
  }

  getOrderTimeline(orderId: number): Observable<TrackingTimelineDTO[]> {
    return this.http.get<TrackingTimelineDTO[]>(`${this.baseUrl}/${orderId}/timeline`);
  }

  // Address book management
  getUserAddresses(): Observable<AddressDTO[]> {
    return this.http.get<AddressDTO[]>(this.addressUrl);
  }

  createAddress(data: AddressDTO): Observable<AddressDTO> {
    return this.http.post<AddressDTO>(this.addressUrl, data);
  }

  updateAddress(id: number, data: AddressDTO): Observable<AddressDTO> {
    return this.http.put<AddressDTO>(`${this.addressUrl}/${id}`, data);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`${this.addressUrl}/${id}`);
  }

  setDefaultAddress(id: number): Observable<AddressDTO> {
    return this.http.put<AddressDTO>(`${this.addressUrl}/${id}/default`, {});
  }
}
