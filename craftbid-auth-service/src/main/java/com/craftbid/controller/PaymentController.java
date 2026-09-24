package com.craftbid.controller;

import com.craftbid.dto.*;
import com.craftbid.entity.PaymentTransaction;
import com.craftbid.entity.Refund;
import com.craftbid.service.CashfreeService;
import com.craftbid.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    private final CashfreeService cashfreeService;

    public PaymentController(PaymentService paymentService, CashfreeService cashfreeService) {
        this.paymentService = paymentService;
        this.cashfreeService = cashfreeService;
    }

    // ==========================================
    // CASHFREE PAYMENT GATEWAY ENDPOINTS
    // ==========================================

    /**
     * Create Cashfree PG Order (v2023-08-01) for Auction Deposit or Direct Purchase.
     * Generates Cashfree order and returns payment_session_id for Cashfree.js checkout modal.
     */
    @PostMapping("/cashfree/create-order")
    public ResponseEntity<CashfreeOrderResponse> createCashfreeOrder(
            Authentication authentication,
            @Valid @RequestBody CashfreeOrderRequest request) {

        String identifier = authentication.getName();
        CashfreeOrderResponse response = cashfreeService.createOrder(
                identifier,
                request.getAmount(),
                request.getAuctionId(),
                request.getCraftId(),
                request.getType()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Strictly verify Cashfree Order payment status with Cashfree Gateway and enroll Auction Participant.
     */
    @PostMapping("/cashfree/verify")
    public ResponseEntity<PaymentTransaction> verifyCashfreePayment(
            Authentication authentication,
            @Valid @RequestBody CashfreeVerifyRequest request) {

        String identifier = authentication.getName();
        PaymentTransaction tx = cashfreeService.verifyAndRecordCashfreePayment(
                identifier,
                request.getOrderId(),
                request.getAuctionId(),
                request.getCraftId(),
                request.getAmount(),
                request.getType(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(tx);
    }

    /**
     * Cashfree Server Webhook with HMAC-SHA256 Signature Verification.
     */
    @PostMapping("/cashfree/webhook")
    public ResponseEntity<Map<String, Object>> handleCashfreeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp) {

        Map<String, Object> response = cashfreeService.processWebhook(rawBody, signature, timestamp);
        return ResponseEntity.ok(response);
    }

    /**
     * Create Razorpay Order for Auction Participation or Direct Payment.
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            Authentication authentication,
            @Valid @RequestBody RazorpayOrderRequest request) {

        String identifier = authentication.getName();
        Map<String, Object> orderData = paymentService.createRazorpayOrder(
                identifier,
                request.getAmount(),
                request.getAuctionId(),
                request.getCraftId(),
                request.getType()
        );
        return ResponseEntity.ok(orderData);
    }

    /**
     * Verify Razorpay Payment Signature, mark transaction CAPTURED, and activate Auction Participant.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentTransaction> verifyPayment(
            Authentication authentication,
            @Valid @RequestBody RazorpayVerifyRequest request) {

        String identifier = authentication.getName();
        PaymentTransaction tx = paymentService.verifyAndRecordRazorpayPayment(
                identifier,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                request.getAuctionId(),
                request.getCraftId(),
                request.getAmount(),
                request.getType(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(tx);
    }

    /**
     * Process Direct / Non-Gateway Payment.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentTransaction> processPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.processPayment(identifier, request));
    }

    /**
     * Process Gateway and Ledger Refund.
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<Refund> processRefund(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) RefundRequest request) {

        String identifier = authentication.getName();
        Refund refund = paymentService.processRefund(
                id,
                request != null ? request.getAmount() : null,
                request != null ? request.getReason() : "Admin initiated refund",
                identifier
        );
        return ResponseEntity.ok(refund);
    }

    /**
     * Razorpay Server Webhook Endpoint.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        Map<String, Object> response = paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok(response);
    }

    /**
     * Get Current User's Payment Transactions.
     */
    @GetMapping({"/my", "/my-history"})
    public ResponseEntity<List<PaymentTransaction>> getMyTransactions(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.getMyTransactions(identifier));
    }

    /**
     * Get Current User's Refunds.
     */
    @GetMapping({"/my-refunds", "/refunds"})
    public ResponseEntity<List<Refund>> getMyRefunds(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.getMyRefunds(identifier));
    }

    /**
     * Get Transaction by Reference.
     */
    @GetMapping("/receipt/{ref}")
    public ResponseEntity<PaymentTransaction> getReceipt(
            Authentication authentication,
            @PathVariable String ref) {
        String identifier = authentication != null ? authentication.getName() : null;
        return ResponseEntity.of(paymentService.getByTransactionRef(ref, identifier));
    }

    /**
     * Get Transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentTransaction> getById(
            Authentication authentication,
            @PathVariable Long id) {
        String identifier = authentication != null ? authentication.getName() : null;
        return ResponseEntity.of(paymentService.getById(id, identifier));
    }

    /**
     * Get Platform Payment Analytics for Admin Dashboard.
     */
    @GetMapping("/admin-stats")
    public ResponseEntity<PaymentStatsDTO> getAdminPaymentStats(Authentication authentication) {
        String identifier = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(paymentService.getAdminPaymentStats(identifier));
    }
}
