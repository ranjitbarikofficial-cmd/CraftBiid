package com.craftbid.repository;

import com.craftbid.entity.PaymentTransaction;
import com.craftbid.entity.Refund;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByUserOrderByCreatedAtDesc(User user);

    List<Refund> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    List<Refund> findByPaymentOrderByCreatedAtDesc(PaymentTransaction payment);

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    List<Refund> findByUserAndAuctionId(User user, Long auctionId);

    long countByStatus(String status);
}
