package com.craftbid.repository;

import com.craftbid.entity.SupportTicket;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

    Optional<SupportTicket> findByTicketRef(String ticketRef);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
