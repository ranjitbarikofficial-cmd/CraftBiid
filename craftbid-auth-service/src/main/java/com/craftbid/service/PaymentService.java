package com.craftbid.service;

import com.craftbid.dto.PaymentRequest;
import com.craftbid.entity.PaymentTransaction;
import com.craftbid.entity.User;
import com.craftbid.repository.PaymentTransactionRepository;
import com.craftbid.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentTransactionRepository paymentRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    private User getUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }

    @Transactional
    public PaymentTransaction processPayment(String identifier, PaymentRequest request) {
        User user = getUserByIdentifier(identifier);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String txnRef = "CB-TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "UPI";
        String type = request.getType() != null ? request.getType().toUpperCase() : "DIRECT_PURCHASE";

        PaymentTransaction tx = new PaymentTransaction(
                user,
                request.getAuctionId(),
                request.getCraftId(),
                request.getAmount(),
                type,
                method,
                txnRef,
                "SUCCESS",
                request.getNotes() != null ? request.getNotes() : "Payment processed successfully via " + method
        );

        return paymentRepository.save(tx);
    }

    @Transactional
    public PaymentTransaction recordTransaction(User user, Long auctionId, Long craftId, BigDecimal amount, String type, String method, String notes) {
        String txnRef = (type.contains("REFUND") ? "CB-REF-" : "CB-TXN-") + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        PaymentTransaction tx = new PaymentTransaction(
                user,
                auctionId,
                craftId,
                amount,
                type,
                method != null ? method : "UPI",
                txnRef,
                type.contains("REFUND") ? "REFUNDED" : "SUCCESS",
                notes
        );

        return paymentRepository.save(tx);
    }

    public List<PaymentTransaction> getMyTransactions(String identifier) {
        User user = getUserByIdentifier(identifier);
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<PaymentTransaction> getByTransactionRef(String txnRef) {
        return paymentRepository.findByTransactionRef(txnRef);
    }
}
