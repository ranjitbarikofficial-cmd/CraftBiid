package com.craftbid.service;

import com.craftbid.dto.ArtisanProfileDTO;
import com.craftbid.dto.ArtisanRegistrationRequest;
import com.craftbid.dto.UpdateArtisanProfileRequest;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.User;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ArtisanService {

    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    public ArtisanService(
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            EmailService emailService,
            FileStorageService fileStorageService) {

        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
    }

    // =====================================================
    // ENABLE ARTISAN ACCOUNT
    // =====================================================

    public String enableArtisan(
            String identifier,
            ArtisanRegistrationRequest request) {

        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        if (user.isSellerEnabled()) {
            throw new RuntimeException("Artisan account is already enabled");
        }

        if (artisanProfileRepository.existsByUser(user)) {
            throw new RuntimeException("Artisan profile already exists");
        }

        ArtisanProfile profile = new ArtisanProfile();
        profile.setUser(user);
        profile.setShopName(request.getShopName().trim());
        profile.setCraftType(request.getCraftType().trim());
        profile.setCity(request.getCity().trim());
        profile.setProfileImageUrl(user.getProfileImageUrl());

        artisanProfileRepository.save(profile);

        user.setSellerEnabled(true);
        userRepository.save(user);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendSellerEnabledEmail(
                    user.getEmail(),
                    user.getName()
            );
        }

        return "Congratulations! You are now a CraftBid Artisan. Your Artisan account has been activated.";
    }

    // =====================================================
    // GET ARTISAN PROFILE
    // =====================================================

    public ArtisanProfileDTO getArtisanProfile(String identifier) {
        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));

        ArtisanProfile profile = artisanProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    ArtisanProfile newP = new ArtisanProfile();
                    newP.setUser(user);
                    newP.setShopName(user.getName() + "'s Studio");
                    newP.setCraftType("Handmade Crafts");
                    newP.setCity(user.getCity());
                    newP.setProfileImageUrl(user.getProfileImageUrl());
                    return artisanProfileRepository.save(newP);
                });

        return new ArtisanProfileDTO(
                profile.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                profile.getCity(),
                profile.getShopName(),
                profile.getCraftType(),
                user.getProfileImageUrl() != null ? user.getProfileImageUrl() : profile.getProfileImageUrl(),
                profile.getCreatedAt()
        );
    }

    // =====================================================
    // UPDATE ARTISAN PROFILE & USER NAME
    // =====================================================

    @Transactional
    public ArtisanProfileDTO updateArtisanProfile(String identifier, UpdateArtisanProfileRequest request) {
        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));

        ArtisanProfile profile = artisanProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    ArtisanProfile newP = new ArtisanProfile();
                    newP.setUser(user);
                    newP.setShopName(user.getName() + "'s Studio");
                    newP.setCraftType("Handmade Crafts");
                    newP.setCity(user.getCity());
                    return artisanProfileRepository.save(newP);
                });

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getCity() != null && !request.getCity().isBlank()) {
            user.setCity(request.getCity().trim());
            profile.setCity(request.getCity().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getShopName() != null && !request.getShopName().isBlank()) {
            profile.setShopName(request.getShopName().trim());
        }

        if (request.getCraftType() != null && !request.getCraftType().isBlank()) {
            profile.setCraftType(request.getCraftType().trim());
        }

        userRepository.save(user);
        artisanProfileRepository.save(profile);

        return new ArtisanProfileDTO(
                profile.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                profile.getCity(),
                profile.getShopName(),
                profile.getCraftType(),
                user.getProfileImageUrl() != null ? user.getProfileImageUrl() : profile.getProfileImageUrl(),
                profile.getCreatedAt()
        );
    }

    // =====================================================
    // UPLOAD ARTISAN PROFILE PHOTO
    // =====================================================

    @Transactional
    public ArtisanProfileDTO uploadProfilePhoto(String identifier, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo file is required");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile photo size must not exceed 10 MB");
        }

        User user = userRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));

        ArtisanProfile profile = artisanProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    ArtisanProfile newP = new ArtisanProfile();
                    newP.setUser(user);
                    newP.setShopName(user.getName() + "'s Studio");
                    newP.setCraftType("Handmade Crafts");
                    newP.setCity(user.getCity());
                    return artisanProfileRepository.save(newP);
                });

        try {
            String photoUrl = fileStorageService.saveFile(file, "avatars");
            user.setProfileImageUrl(photoUrl);
            profile.setProfileImageUrl(photoUrl);

            userRepository.save(user);
            artisanProfileRepository.save(profile);

            return new ArtisanProfileDTO(
                    profile.getId(),
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    profile.getCity(),
                    profile.getShopName(),
                    profile.getCraftType(),
                    photoUrl,
                    profile.getCreatedAt()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile photo. Please try again.", e);
        }
    }
}