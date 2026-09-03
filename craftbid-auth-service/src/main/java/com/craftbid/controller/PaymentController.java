package com.craftbid.controller;

import com.craftbid.dto.PaymentRequest;
import com.craftbid.entity.PaymentTransaction;
import com.craftbid.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentTransaction> processPayment(
            Authentication authentication,
            @RequestBody PaymentRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.processPayment(identifier, request));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<PaymentTransaction>> getMyTransactions(Authentication authentication) {
        String identifier = authentication.getName();
        return ResponseEntity.ok(paymentService.getMyTransactions(identifier));
    }

    @GetMapping("/receipt/{ref}")
    public ResponseEntity<PaymentTransaction> getReceipt(@PathVariable String ref) {
        return ResponseEntity.of(paymentService.getByTransactionRef(ref));
    }
}
