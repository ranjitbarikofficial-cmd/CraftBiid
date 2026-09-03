package com.craftbid.controller;

import com.craftbid.dto.ArtisanRegistrationRequest;
import com.craftbid.service.ArtisanService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artisan")
public class ArtisanController {

    private final ArtisanService artisanService;

    public ArtisanController(
            ArtisanService artisanService) {

        this.artisanService = artisanService;
    }

    // =====================================================
    // ENABLE ARTISAN ACCOUNT
    // =====================================================

    @PostMapping("/enable")
    public ResponseEntity<String> enableArtisan(
            @Valid @RequestBody ArtisanRegistrationRequest request,
            Authentication authentication) {

        // Email comes from JWT
        String email = authentication.getName();

        String message =
                artisanService.enableArtisan(
                        email,
                        request
                );

        return ResponseEntity.ok(message);
    }
}