package com.craftbid.repository;

import com.craftbid.entity.PaymentTransaction;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<PaymentTransaction> findByUserAndTypeContainingIgnoreCaseOrderByCreatedAtDesc(User user, String type);

    List<PaymentTransaction> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<PaymentTransaction> findByAuctionIdAndUserAndStatus(Long auctionId, User user, String status);

    List<PaymentTransaction> findByAuctionIdAndUserAndStatusOrderByCreatedAtDesc(Long auctionId, User user, String status);

    long countByStatus(String status);
}
