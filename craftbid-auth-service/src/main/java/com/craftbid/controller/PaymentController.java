package com.craftbid.controller;

import com.craftbid.dto.PaymentRequest;
import com.craftbid.dto.RazorpayOrderRequest;
import com.craftbid.dto.RazorpayVerifyRequest;
import com.craftbid.entity.PaymentTransaction;
import com.craftbid.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

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

    @PostMapping("/process")
    public ResponseEntity<PaymentTransaction> processPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.processPayment(identifier, request));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<PaymentTransaction>> getMyTransactions(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.getMyTransactions(identifier));
    }

    @GetMapping(value = {"/my-refunds", "/refunds"})
    public ResponseEntity<List<PaymentTransaction>> getMyRefunds(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.getMyRefunds(identifier));
    }

    @GetMapping("/receipt/{ref}")
    public ResponseEntity<PaymentTransaction> getReceipt(@PathVariable String ref) {
        return ResponseEntity.of(paymentService.getByTransactionRef(ref));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody String payload, @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        // Webhook handler for external gateway event updates
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
