import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CraftItem } from './craft.service';
import { UserAuth } from './auth';
import { getApiBaseUrl } from './api-config';

export interface AuctionItem {
  id: number;
  craft: CraftItem;
  seller: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
  startingPrice: number;
  currentHighestBid: number;
  reservePrice?: number;
  minBidIncrement: number;
  startTime: string;
  endTime: string;
  status: 'DRAFT' | 'ACTIVE' | 'ENDED' | 'CANCELLED';
  winningBidder?: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
  totalBids: number;
  maxParticipants: number;
  currentParticipantsCount: number;
  interestedCount: number;
  lastBidTime?: string;
  turnDeadline?: string;
  liveTurnActive?: boolean;
  adminFeeAmount?: number;
  artisanPayoutAmount?: number;
  createdAt: string;
}

export interface BidItem {
  id: number;
  auction: AuctionItem;
  bidder: {
    id: number;
    name: string;
    email: string;
  };
  amount: number;
  bidTime: string;
  status: string;
}

export interface AuctionParticipantItem {
  id: number;
  auction: AuctionItem;
  user: {
    id: number;
    name: string;
    email: string;
  };
  basePricePaid: number;
  totalAmountPaid: number;
  status: 'JOINED' | 'ACTIVE' | 'WON' | 'REFUNDED';
  refundAmount?: number;
  joinedAt: string;
}

export interface AuctionOrderItem {
  id: number;
  auction: AuctionItem;
  buyer: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
  artisan: {
    id: number;
    name: string;
    email: string;
    phone?: string;
  };
  winningAmount: number;
  platformFee: number;
  artisanPayout: number;
  fullName: string;
  streetAddress: string;
  city: string;
  state?: string;
  pincode: string;
  phone: string;
  status: string;
  createdAt: string;
}

export interface CreateAuctionPayload {
  craftId: number;
  startingPrice?: number;
  minBidIncrement?: number;
  durationHours?: number;
  reservePrice?: number;
}

export interface SubmitAddressPayload {
  fullName: string;
  streetAddress: string;
  city: string;
  state?: string;
  pincode: string;
  phone: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuctionService {
  private apiUrl = `${getApiBaseUrl()}/api/auctions`;

  constructor(private http: HttpClient) {}

  createAuction(payload: CreateAuctionPayload): Observable<AuctionItem> {
    return this.http.post<AuctionItem>(this.apiUrl, payload);
  }

  getActiveAuctions(): Observable<AuctionItem[]> {
    return this.http.get<AuctionItem[]>(this.apiUrl);
  }

  getAuctionById(id: number): Observable<AuctionItem> {
    return this.http.get<AuctionItem>(`${this.apiUrl}/${id}`);
  }

  getMyAuctions(): Observable<AuctionItem[]> {
    return this.http.get<AuctionItem[]>(`${this.apiUrl}/my-auctions`);
  }

  getAuctionsByCraft(craftId: number): Observable<AuctionItem[]> {
    return this.http.get<AuctionItem[]>(`${this.apiUrl}/craft/${craftId}`);
  }

  joinAuctionWithDeposit(
    auctionId: number,
    paymentMethod = 'UPI',
  ): Observable<AuctionParticipantItem> {
    return this.http.post<AuctionParticipantItem>(`${this.apiUrl}/${auctionId}/join`, {
      paymentMethod,
    });
  }

  placeDifferentialBid(auctionId: number, amount: number): Observable<BidItem> {
    return this.http.post<BidItem>(`${this.apiUrl}/${auctionId}/differential-bid`, { amount });
  }

  placeBid(auctionId: number, amount: number): Observable<BidItem> {
    return this.placeDifferentialBid(auctionId, amount);
  }

  getParticipants(auctionId: number): Observable<AuctionParticipantItem[]> {
    return this.http.get<AuctionParticipantItem[]>(`${this.apiUrl}/${auctionId}/participants`);
  }

  submitDeliveryAddress(
    auctionId: number,
    payload: SubmitAddressPayload,
  ): Observable<AuctionOrderItem> {
    return this.http.post<AuctionOrderItem>(`${this.apiUrl}/${auctionId}/address`, payload);
  }

  getAuctionOrder(auctionId: number): Observable<AuctionOrderItem> {
    return this.http.get<AuctionOrderItem>(`${this.apiUrl}/${auctionId}/order`);
  }

  getArtisanOrders(): Observable<AuctionOrderItem[]> {
    return this.http.get<AuctionOrderItem[]>(`${this.apiUrl}/artisan-orders`);
  }

  getBuyerOrders(): Observable<AuctionOrderItem[]> {
    return this.http.get<AuctionOrderItem[]>(`${this.apiUrl}/buyer-orders`);
  }

  getAuctionBids(auctionId: number): Observable<BidItem[]> {
    return this.http.get<BidItem[]>(`${this.apiUrl}/${auctionId}/bids`);
  }

  getMyBids(): Observable<BidItem[]> {
    return this.http.get<BidItem[]>(`${this.apiUrl}/my-bids`);
  }

  cancelAuction(id: number): Observable<AuctionItem> {
    return this.http.post<AuctionItem>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
