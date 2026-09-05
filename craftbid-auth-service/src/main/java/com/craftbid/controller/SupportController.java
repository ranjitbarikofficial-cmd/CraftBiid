package com.craftbid.controller;

import com.craftbid.dto.SupportTicketRequest;
import com.craftbid.entity.SupportTicket;
import com.craftbid.service.SupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/ticket")
    public ResponseEntity<SupportTicket> createTicket(
            Authentication authentication,
            @Valid @RequestBody SupportTicketRequest request) {

        String identifier = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(supportService.createTicket(identifier, request));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<SupportTicket>> getMyTickets(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(supportService.getMyTickets(authentication.getName()));
    }

    @GetMapping("/ticket/{ref}")
    public ResponseEntity<SupportTicket> getTicketByRef(
            @PathVariable @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Invalid reference format") String ref) {
        return ResponseEntity.of(supportService.getTicketByRef(ref));
    }
}

