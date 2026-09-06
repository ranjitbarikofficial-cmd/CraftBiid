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
  };
  auctionId?: number;
  craftId?: number;
  amount: number;
  type: 'BASE_DEPOSIT' | 'DIFFERENTIAL_BID' | 'AUTO_REFUND' | 'DIRECT_PURCHASE';
  paymentMethod: 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET';
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

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private apiUrl = `${getApiBaseUrl()}/api/payments`;

  constructor(private http: HttpClient) {}

  processPayment(payload: ProcessPaymentPayload): Observable<PaymentTransactionItem> {
    return this.http.post<PaymentTransactionItem>(`${this.apiUrl}/process`, payload);
  }

  getMyTransactions(): Observable<PaymentTransactionItem[]> {
    return this.http.get<PaymentTransactionItem[]>(`${this.apiUrl}/my-history`);
  }

  getReceipt(ref: string): Observable<PaymentTransactionItem> {
    return this.http.get<PaymentTransactionItem>(`${this.apiUrl}/receipt/${ref}`);
  }
}
