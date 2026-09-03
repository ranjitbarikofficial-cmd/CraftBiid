package com.craftbid.service;

import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.Craft;
import com.craftbid.entity.CraftReel;
import com.craftbid.entity.User;

import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.CraftReelRepository;
import com.craftbid.repository.CraftRepository;
import com.craftbid.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CraftReelService {

    private final CraftReelRepository craftReelRepository;
    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final CraftRepository craftRepository;

    public CraftReelService(
            CraftReelRepository craftReelRepository,
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            CraftRepository craftRepository) {

        this.craftReelRepository = craftReelRepository;
        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.craftRepository = craftRepository;
    }

    // ==========================================
    // CREATE CRAFT REEL
    // ==========================================

    public CraftReel createReel(
            String identifier,
            Long craftId,
            String title,
            String description,
            String videoUrl,
            String thumbnailUrl) {

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (!user.isSellerEnabled()) {
            throw new RuntimeException(
                    "Please enable your Artisan account first");
        }

        ArtisanProfile artisan =
                artisanProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException("Artisan profile not found"));

        Craft craft = craftRepository.findById(craftId)
                .orElseThrow(() ->
                        new RuntimeException("Craft not found"));

        // Make sure this craft belongs to the logged-in artisan
        if (!craft.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You can only create reels for your own crafts");
        }

        CraftReel reel = new CraftReel();

        reel.setArtisan(artisan);
        reel.setCraft(craft);
        reel.setTitle(title);
        reel.setDescription(description);
        reel.setVideoUrl(videoUrl);
        reel.setThumbnailUrl(thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : craft.getImageUrl());
        reel.setViews(0L);
        reel.setLikes(0L);
        reel.setStatus("ACTIVE");

        return craftReelRepository.save(reel);
    }

    // ==========================================
    // GET MY REELS
    // ==========================================

    public List<CraftReel> getMyReels(String identifier) {

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        ArtisanProfile artisan =
                artisanProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException("Artisan profile not found"));

        return craftReelRepository
                .findByArtisanId(artisan.getId());
    }

    // ==========================================
    // HOME REELS (FEED)
    // ==========================================

    public List<CraftReel> getHomeReels() {

        return craftReelRepository
                .findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    // ==========================================
    // GET REELS BY CRAFT ID
    // ==========================================

    public List<CraftReel> getReelsByCraftId(Long craftId) {

        return craftReelRepository.findByCraftId(craftId);
    }

    // ==========================================
    // INCREMENT VIEWS
    // ==========================================

    public CraftReel incrementViews(Long reelId) {

        CraftReel reel = craftReelRepository
                .findById(reelId)
                .orElseThrow(() ->
                        new RuntimeException("Reel not found"));

        reel.setViews(reel.getViews() + 1);

        return craftReelRepository.save(reel);
    }

    // ==========================================
    // LIKE REEL
    // ==========================================

    public CraftReel likeReel(Long reelId) {

        CraftReel reel = craftReelRepository
                .findById(reelId)
                .orElseThrow(() ->
                        new RuntimeException("Reel not found"));

        reel.setLikes(reel.getLikes() + 1);

        return craftReelRepository.save(reel);
    }
}