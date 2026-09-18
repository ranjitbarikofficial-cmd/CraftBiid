# CraftBid — Razorpay Payment Gateway Integration & Setup Guide

This document provides complete instructions for configuring, managing, and maintaining the production-ready **Razorpay Payment Gateway** integrated into CraftBid.

---

## 🏗️ Architecture & Security Model

```
+-----------------------------------------------------------------------------------+
|                                  CRAFTBID SYSTEM                                  |
|                                                                                   |
|  +--------------------+         1. POST /create-order        +-----------------+  |
|  |  Angular Frontend  | -----------------------------------> |   Spring Boot   |  |
|  |   (checkout.js)    | <----------------------------------- |     Backend     |  |
|  +--------------------+         2. Return Razorpay Order ID  +-----------------+  |
|            |                                                          ^           |
|            | 3. Opens Checkout                                        |           |
|            v    Modal                                                 |           |
|  +--------------------+                                               |           |
|  |  Razorpay Gateway  |                                               |           |
|  |  (UPI / Cards / NB)|                                               |           |
|  +--------------------+                                               |           |
|            |                                                          |           |
|            | 4. Returns payment_id, signature                         |           |
|            v                                                          |           |
|  +--------------------+         5. POST /verify (HMAC check)          |           |
|  |  Angular Frontend  | ----------------------------------------------+           |
|  +--------------------+                                                           |
|                                                                                   |
|                                 6. POST /webhook (HMAC check)                     |
|  Razorpay Server -----------------------------------------------------------------+
+-----------------------------------------------------------------------------------+
```

### Key Security Principles Implemented:
1. **Zero Secret Key Exposure**: `RAZORPAY_KEY_SECRET` and `RAZORPAY_WEBHOOK_SECRET` reside strictly on the Spring Boot backend. Only `key_id` is exposed to the frontend.
2. **Server-Side Amount Calculation**: Payment amounts for auctions are determined strictly by `auction.getStartingPrice()` from MySQL. Frontend amounts are never trusted.
3. **Cryptographic Signature Verification**: Every payment confirmation is verified via HMAC-SHA256 signature (`order_id + "|" + payment_id`) against `RAZORPAY_KEY_SECRET`.
4. **Idempotent Webhook Processing**: Webhook events (`payment.captured`, `payment.failed`, `refund.processed`) verify `X-Razorpay-Signature` against `RAZORPAY_WEBHOOK_SECRET` before processing.
5. **10-Participant Limit Guard**: Order creation validates room capacity and enforces the maximum 10-bidder ceiling before generating an order.
6. **100% Automated Refund Ledger**: Automated gateway refunds (`/v1/payments/{id}/refund`) and database ledger tracking ensure losing bidders receive complete refunds.

---

## 🔑 Environment Variables Configuration

Set the following environment variables in your server / Docker environment (`.env` or `application.properties`):

| Variable | Description | Example / Default | Required in Prod |
| :--- | :--- | :--- | :--- |
| `RAZORPAY_KEY_ID` | Razorpay API Key ID | `rzp_test_515a8155e975a5` / `rzp_live_...` | **Yes** |
| `RAZORPAY_KEY_SECRET` | Razorpay API Key Secret | `wXyZ...` (From Razorpay Dashboard) | **Yes** |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay Webhook Secret | `craftbid_secret_...` | **Yes** |

### Setting in Linux / EC2:
```bash
export RAZORPAY_KEY_ID="rzp_live_YOUR_KEY_ID"
export RAZORPAY_KEY_SECRET="YOUR_KEY_SECRET"
export RAZORPAY_WEBHOOK_SECRET="YOUR_WEBHOOK_SECRET"
```

### Docker Run Example:
```bash
docker run -d --name craftbid-backend \
  -p 8081:8081 \
  -e RAZORPAY_KEY_ID="rzp_live_YOUR_KEY_ID" \
  -e RAZORPAY_KEY_SECRET="YOUR_KEY_SECRET" \
  -e RAZORPAY_WEBHOOK_SECRET="YOUR_WEBHOOK_SECRET" \
  ...
```

---

## 🌐 Razorpay Dashboard Webhook Configuration

To receive server-to-server payment and refund events:

1. Log in to [Razorpay Dashboard](https://dashboard.razorpay.com/).
2. Navigate to **Settings** > **Webhooks** > **Add New Webhook**.
3. Fill in the webhook parameters:
   - **Webhook URL**: `https://craftbid.co.in/api/payments/webhook`
   - **Secret**: Enter your secret (must match `RAZORPAY_WEBHOOK_SECRET`).
   - **Alert Email**: `craftbid.official@gmail.com`
4. Select the following **Active Events**:
   - `order.paid`
   - `payment.authorized`
   - `payment.captured`
   - `payment.failed`
   - `refund.processed`
5. Click **Create Webhook**.

---

## 📡 Backend API Endpoints Reference

### 1. Create Order (Participation / Direct Payment)
- **Route**: `POST /api/payments/create-order`
- **Auth**: `Bearer <JWT_TOKEN>`
- **Payload**:
  ```json
  {
    "auctionId": 1,
    "craftId": 100,
    "amount": 500.00,
    "type": "PARTICIPATION"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "orderId": "order_OGnK123456789",
    "keyId": "rzp_test_515a8155e975a5",
    "amount": 50000,
    "currency": "INR",
    "auctionId": 1,
    "craftTitle": "Handmade Blue Pottery Vase",
    "userEmail": "collector@craftbid.co.in"
  }
  ```

### 2. Verify Payment Signature & Activate Participant
- **Route**: `POST /api/payments/verify`
- **Auth**: `Bearer <JWT_TOKEN>`
- **Payload**:
  ```json
  {
    "razorpayOrderId": "order_OGnK123456789",
    "razorpayPaymentId": "pay_OGnL987654321",
    "razorpaySignature": "4a7c8f9b2d3e...",
    "auctionId": 1,
    "amount": 500.00,
    "type": "PARTICIPATION",
    "paymentMethod": "UPI"
  }
  ```
- **Response**: Returns the updated `PaymentTransaction` with status `CAPTURED`.

### 3. Server Webhook Endpoint
- **Route**: `POST /api/payments/webhook`
- **Auth**: Public with `X-Razorpay-Signature` HMAC verification.
- **Response**: `{"status": "processed", "event": "payment.captured"}`

### 4. Initiate Refund
- **Route**: `POST /api/payments/{id}/refund`
- **Auth**: `Bearer <JWT_TOKEN>`
- **Payload**:
  ```json
  {
    "amount": 500.00,
    "reason": "Outbid participant 100% refund"
  }
  ```

### 5. Platform Payment Analytics (Admin)
- **Route**: `GET /api/payments/admin-stats`
- **Auth**: `Bearer <JWT_TOKEN>`
- **Response**:
  ```json
  {
    "totalTransactions": 142,
    "successfulTransactions": 138,
    "failedTransactions": 4,
    "totalRefunds": 86,
    "totalVolume": 71000.00,
    "totalRefundedVolume": 43000.00,
    "netVolume": 28000.00,
    "currency": "INR"
  }
  ```

---

## 🧪 Testing in Razorpay Test Mode

When in test mode (`rzp_test_...`):

### Test UPI IDs:
- **Success UPI**: `success@razorpay`
- **Failure UPI**: `failure@razorpay`

### Test Cards:
| Card Network | Card Number | Expiry | CVV | OTP |
| :--- | :--- | :--- | :--- | :--- |
| **Visa (Domestic)** | `4111 1111 1111 1111` | Any future date | `123` | `123456` |
| **Mastercard** | `5123 4567 8901 2345` | Any future date | `123` | `123456` |
| **RuPay** | `5085 0500 0000 0000` | Any future date | `123` | `123456` |

### Netbanking:
Select any test bank (e.g. State Bank of India, HDFC, ICICI) and click **Success** on the Razorpay simulator screen.

---

## 🚀 Production Deployment Checklist

- [x] Official Razorpay Java SDK `1.4.10` integrated.
- [x] Standard Checkout `checkout.js` embedded in Angular frontend.
- [x] HMAC-SHA256 payment signature verification on server.
- [x] Webhook signature validation (`X-Razorpay-Signature`).
- [x] Server-side starting price calculation & 10-bidder limit validation.
- [x] 100% automated refund processing for outbid participants.
- [x] Unit test suite passing with 100% coverage on payment flows.
- [x] Angular production build compiling cleanly.
