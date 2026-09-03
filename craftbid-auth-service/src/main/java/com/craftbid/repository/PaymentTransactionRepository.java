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

    List<PaymentTransaction> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
}
