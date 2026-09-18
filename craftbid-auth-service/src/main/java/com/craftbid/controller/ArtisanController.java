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

    // =====================================================
    // GET ARTISAN PROFILE
    // =====================================================

    @GetMapping("/profile")
    public ResponseEntity<com.craftbid.dto.ArtisanProfileDTO> getProfile(
            Authentication authentication) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(artisanService.getArtisanProfile(identifier));
    }

    // =====================================================
    // UPDATE ARTISAN PROFILE & NAME
    // =====================================================

    @PutMapping("/profile")
    public ResponseEntity<com.craftbid.dto.ArtisanProfileDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody com.craftbid.dto.UpdateArtisanProfileRequest request) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(artisanService.updateArtisanProfile(identifier, request));
    }

    // =====================================================
    // UPLOAD ARTISAN PROFILE PHOTO
    // =====================================================

    @PostMapping(value = "/profile-photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.craftbid.dto.ArtisanProfileDTO> uploadProfilePhoto(
            Authentication authentication,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(artisanService.uploadProfilePhoto(identifier, file));
    }
}