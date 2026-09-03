package com.craftbid.service;

import com.craftbid.dto.ArtisanRegistrationRequest;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.User;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ArtisanService {

    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final EmailService emailService;

    public ArtisanService(
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.emailService = emailService;
    }

    // =====================================================
    // ENABLE ARTISAN ACCOUNT
    // =====================================================

    public String enableArtisan(
            String identifier,
            ArtisanRegistrationRequest request) {

        // Find user by email or phone
        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        // Check account
        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is inactive"
            );
        }

        // Check already enabled
        if (user.isSellerEnabled()) {
            throw new RuntimeException(
                    "Artisan account is already enabled"
            );
        }

        // Check if profile already exists
        if (artisanProfileRepository.existsByUser(user)) {
            throw new RuntimeException(
                    "Artisan profile already exists"
            );
        }

        // =================================================
        // CREATE ARTISAN PROFILE
        // =================================================

        ArtisanProfile profile =
                new ArtisanProfile();

        profile.setUser(user);

        profile.setShopName(
                request.getShopName().trim()
        );

        profile.setCraftType(
                request.getCraftType().trim()
        );

        profile.setCity(
                request.getCity().trim()
        );

        artisanProfileRepository.save(profile);

        // =================================================
        // ENABLE SELLER
        // =================================================

        user.setSellerEnabled(true);

        userRepository.save(user);

        // =================================================
        // SEND EMAIL IF EMAIL AVAILABLE
        // =================================================

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendSellerEnabledEmail(
                    user.getEmail(),
                    user.getName()
            );
        }

        return "Congratulations! You are now a CraftBid Artisan. " +
                "Your Artisan account has been activated.";
    }
}