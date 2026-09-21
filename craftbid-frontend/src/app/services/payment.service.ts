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
  razorpayOrderId?: string;
  razorpayPaymentId?: string;
  razorpaySignature?: string;
  amount: number;
  currency: string;
  type: string;
  paymentMethod: string;
  transactionRef: string;
  status: 'CREATED' | 'PENDING' | 'AUTHORIZED' | 'CAPTURED' | 'FAILED' | 'REFUNDED' | 'SUCCESS';
  receipt?: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface RefundItem {
  id: number;
  payment?: PaymentTransactionItem;
  user: {
    id: number;
    name: string;
    email: string;
  };
  auctionId?: number;
  razorpayPaymentId?: string;
  razorpayRefundId: string;
  amount: number;
  currency: string;
  status: 'INITIATED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  reason?: string;
  createdAt: string;
  updatedAt?: string;
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
  amountInInr?: number;
  currency: string;
  auctionId?: number;
  craftTitle?: string;
  userName?: string;
  userEmail?: string;
  userPhone?: string;
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
  paymentMethod?: string;
}

export interface PaymentStats {
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  totalRefunds: number;
  totalVolume: number;
  totalRefundedVolume: number;
  netVolume: number;
  currency: string;
}

export interface RazorpayCheckoutOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  theme?: {
    color?: string;
  };
  modal?: {
    ondismiss?: () => void;
  };
  handler?: (response: {
    razorpay_payment_id: string;
    razorpay_order_id: string;
    razorpay_signature: string;
  }) => void;
}

declare var Razorpay: any;

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private apiUrl = `${getApiBaseUrl()}/api/payments`;

  constructor(private http: HttpClient) {}

  /**
   * Create Razorpay Order on the backend
   */
  createRazorpayOrder(
    amount: number,
    auctionId?: number,
    craftId?: number,
    type = 'PARTICIPATION',
  ): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.apiUrl}/create-order`, {
      amount,
      auctionId,
      craftId,
      type,
    });
  }

  /**
   * Verify Razorpay Payment Signature and activate participation
   */
  verifyRazorpayPayment(payload: RazorpayVerifyPayload): Observable<PaymentTransactionItem> {
    return this.http.post<PaymentTransactionItem>(`${this.apiUrl}/verify`, payload);
  }

  /**
   * Launch Razorpay Standard Checkout popup
   */
  openRazorpayCheckout(
    order: RazorpayOrderResponse,
    onSuccess: (verifyPayload: RazorpayVerifyPayload) => void,
    onDismiss?: () => void,
    onError?: (err: any) => void,
  ): void {
    // If order is simulated/test or Razorpay client is unconfigured on backend
    if (
      order.simulated ||
      !order.orderId ||
      order.orderId.startsWith('order_test_') ||
      order.orderId.startsWith('order_sim_')
    ) {
      const payload: RazorpayVerifyPayload = {
        razorpayOrderId: order.orderId || `order_test_${Date.now()}`,
        razorpayPaymentId: `pay_test_${Date.now()}`,
        razorpaySignature: `sig_test_${Date.now()}`,
        auctionId: order.auctionId,
        amount: order.amountInInr || (order.amount / 100),
        type: 'PARTICIPATION',
        paymentMethod: 'RAZORPAY',
      };
      onSuccess(payload);
      return;
    }

    if (typeof Razorpay === 'undefined') {
      const err = new Error('Razorpay SDK failed to load. Please check your internet connection.');
      if (onError) onError(err);
      return;
    }

    try {
      const options: RazorpayCheckoutOptions = {
        key: order.keyId,
        amount: order.amount,
        currency: order.currency || 'INR',
        name: 'CraftBid Official',
        description: order.craftTitle ? `Join Auction: ${order.craftTitle}` : 'CraftBid Payment',
        order_id: order.orderId,
        prefill: {
          name: order.userName || '',
          email: order.userEmail || '',
          contact: order.userPhone || '',
        },
        theme: {
          color: '#ea580c', // CraftBid brand amber/orange
        },
        modal: {
          ondismiss: () => {
            if (onDismiss) onDismiss();
          },
        },
        handler: (response: any) => {
          const payload: RazorpayVerifyPayload = {
            razorpayOrderId: response.razorpay_order_id || order.orderId,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
            auctionId: order.auctionId,
            amount: order.amountInInr || (order.amount / 100),
            type: 'PARTICIPATION',
            paymentMethod: 'RAZORPAY',
          };
          onSuccess(payload);
        },
      };

      const rzp = new Razorpay(options);
      rzp.on('payment.failed', (response: any) => {
        if (onError) onError(response.error);
      });
      rzp.open();
    } catch (e) {
      if (onError) onError(e);
    }
  }

  processPayment(payload: ProcessPaymentPayload): Observable<PaymentTransactionItem> {
    return this.http.post<PaymentTransactionItem>(`${this.apiUrl}/process`, payload);
  }

  processRefund(id: number, amount?: number, reason?: string): Observable<RefundItem> {
    return this.http.post<RefundItem>(`${this.apiUrl}/${id}/refund`, { amount, reason });
  }

  getMyTransactions(): Observable<PaymentTransactionItem[]> {
    return this.http.get<PaymentTransactionItem[]>(`${this.apiUrl}/my-history`);
  }

  getMyRefunds(): Observable<RefundItem[]> {
    return this.http.get<RefundItem[]>(`${this.apiUrl}/my-refunds`);
  }

  getReceipt(ref: string): Observable<PaymentTransactionItem> {
    return this.http.get<PaymentTransactionItem>(`${this.apiUrl}/receipt/${ref}`);
  }

  getAdminPaymentStats(): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${this.apiUrl}/admin-stats`);
  }
}
