import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { getApiBaseUrl } from './api-config';

export interface PaymentTransactionItem {
  id: number;
  user: {
    id: number;
    name: string;
    email: string;
    city?: string;
  };
  auctionId?: number;
  craftId?: number;
  amount: number;
  type: 'BASE_DEPOSIT' | 'DIFFERENTIAL_BID' | 'AUTO_REFUND' | 'DIRECT_PURCHASE';
  paymentMethod: 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET' | 'RAZORPAY';
  transactionRef: string;
  status: 'SUCCESS' | 'REFUNDED' | 'FAILED';
  notes?: string;
  createdAt: string;
}

export interface ProcessPaymentPayload {
  auctionId?: number;
  craftId?: number;
  amount: number;
  type: string;
  paymentMethod: string;
  notes?: string;
}

export interface RazorpayOrderResponse {
  success: boolean;
  orderId: string;
  keyId: string;
  amount: number;
  currency: string;
  simulated?: boolean;
}

export interface RazorpayVerifyPayload {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  auctionId?: number;
  craftId?: number;
  amount: number;
  type: string;
  paymentMethod: string;
}

declare var Razorpay: any;

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private apiUrl = `${getApiBaseUrl()}/api/payments`;

  constructor(private http: HttpClient) {}

  createRazorpayOrder(
    amount: number,
    auctionId?: number,
    craftId?: number,
    type = 'BASE_DEPOSIT',
  ): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.apiUrl}/create-order`, {
      amount,
      auctionId,
      craftId,
      type,
    });
  }

  verifyRazorpayPayment(payload: RazorpayVerifyPayload): Observable<PaymentTransactionItem> {
    return this.http.post<PaymentTransactionItem>(`${this.apiUrl}/verify`, payload);
  }

  processPayment(payload: ProcessPaymentPayload): Observable<PaymentTransactionItem> {
    return this.http.post<PaymentTransactionItem>(`${this.apiUrl}/process`, payload);
  }

  getMyTransactions(): Observable<PaymentTransactionItem[]> {
    return this.http.get<PaymentTransactionItem[]>(`${this.apiUrl}/my-history`);
  }

  getMyRefunds(): Observable<PaymentTransactionItem[]> {
    return this.http.get<PaymentTransactionItem[]>(`${this.apiUrl}/my-refunds`);
  }

  getReceipt(ref: string): Observable<PaymentTransactionItem> {
    return this.http.get<PaymentTransactionItem>(`${this.apiUrl}/receipt/${ref}`);
  }
}
