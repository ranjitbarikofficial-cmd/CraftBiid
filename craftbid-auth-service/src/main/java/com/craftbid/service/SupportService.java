package com.craftbid.service;

import com.craftbid.dto.SupportTicketRequest;
import com.craftbid.entity.SupportTicket;
import com.craftbid.entity.User;
import com.craftbid.repository.SupportTicketRepository;
import com.craftbid.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;

    public SupportService(SupportTicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SupportTicket createTicket(String identifier, SupportTicketRequest request) {
        User user = null;
        if (identifier != null && !identifier.equalsIgnoreCase("anonymousUser")) {
            user = userRepository.findByIdentifier(identifier).orElse(null);
        }

        String ticketRef = "CB-SUP-" + System.currentTimeMillis() % 1000000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        String name = request.getName();
        String email = request.getEmail();
        String phone = request.getPhone();

        if (user != null) {
            if (name == null || name.isBlank()) name = user.getName();
            if (email == null || email.isBlank()) email = user.getEmail();
            if (phone == null || phone.isBlank()) phone = user.getPhone();
        }

        if (name == null || name.isBlank()) name = "Valued Customer";
        if (email == null || email.isBlank()) email = "guest@craftbid.in";

        String category = request.getCategory() != null ? request.getCategory() : "GENERAL_SUPPORT";
        String subject = request.getSubject() != null ? request.getSubject() : "Support Inquiry";
        String message = request.getMessage() != null ? request.getMessage() : "Customer assistance requested.";

        SupportTicket ticket = new SupportTicket(
                user,
                name,
                email,
                phone,
                category,
                subject,
                message,
                ticketRef
        );

        return ticketRepository.save(ticket);
    }

    public List<SupportTicket> getMyTickets(String identifier) {
        if (identifier == null || identifier.equalsIgnoreCase("anonymousUser")) {
            return List.of();
        }
        User user = userRepository.findByIdentifier(identifier).orElse(null);
        if (user == null) return List.of();
        return ticketRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<SupportTicket> getTicketByRef(String ticketRef) {
        return ticketRepository.findByTicketRef(ticketRef);
    }
}
