package com.craftbid.service;

import com.craftbid.dsa.LRUCache;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.Craft;
import com.craftbid.entity.CraftReel;
import com.craftbid.entity.User;

import com.craftbid.dto.UpdateReelRequest;
import com.craftbid.entity.Role;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.CraftReelRepository;
import com.craftbid.repository.CraftRepository;
import com.craftbid.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CraftReelService {

    private final CraftReelRepository craftReelRepository;
    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final CraftRepository craftRepository;

    // DSA: In-Memory LRU Cache for Home Reels Feed (60s TTL) and Individual Reels (5m TTL)
    private final LRUCache<String, List<CraftReel>> feedCache = new LRUCache<>(20, 60_000);
    private final LRUCache<Long, CraftReel> reelCache = new LRUCache<>(200, 300_000);

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

        CraftReel saved = craftReelRepository.save(reel);
        reelCache.put(saved.getId(), saved);
        feedCache.clear(); // Invalidate feed cache on new reel
        return saved;
    }

    // ==========================================
    // GET MY REELS
    // ==========================================

    private boolean isVideoUrl(String url) {
        if (url == null || url.trim().isBlank()) return false;
        String lower = url.trim().toLowerCase();
        return !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") &&
               !lower.endsWith(".png") && !lower.endsWith(".webp") &&
               !lower.endsWith(".gif") && !lower.endsWith(".svg");
    }

    public List<CraftReel> getMyReels(String identifier) {

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        ArtisanProfile artisan =
                artisanProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException("Artisan profile not found"));

        return craftReelRepository
                .findByArtisanId(artisan.getId())
                .stream()
                .filter(r -> isVideoUrl(r.getVideoUrl()))
                .toList();
    }

    // ==========================================
    // HOME REELS (FEED) - O(1) via LRU Cache
    // ==========================================

    public List<CraftReel> getHomeReels() {
        List<CraftReel> cached = feedCache.get("HOME_REELS");
        if (cached != null) {
            return cached;
        }

        List<CraftReel> list = craftReelRepository
                .findByStatusOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .filter(r -> isVideoUrl(r.getVideoUrl()))
                .toList();
        feedCache.put("HOME_REELS", list);
        return list;
    }

    // ==========================================
    // GET REELS BY CRAFT ID
    // ==========================================

    public List<CraftReel> getReelsByCraftId(Long craftId) {

        return craftReelRepository.findByCraftId(craftId)
                .stream()
                .filter(r -> isVideoUrl(r.getVideoUrl()))
                .toList();
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
        CraftReel saved = craftReelRepository.save(reel);
        reelCache.put(saved.getId(), saved);
        return saved;
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
        CraftReel saved = craftReelRepository.save(reel);
        reelCache.put(saved.getId(), saved);
        return saved;
    }

    // ==========================================
    // UPDATE REEL
    // ==========================================

    @Transactional
    public CraftReel updateReel(String identifier, Long reelId, UpdateReelRequest request) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CraftReel reel = craftReelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found with id: " + reelId));

        if (user.getRole() != Role.ADMIN) {
            if (reel.getArtisan() == null || reel.getArtisan().getUser() == null ||
                    !reel.getArtisan().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You are not allowed to modify this craft reel");
            }
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            reel.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            reel.setDescription(request.getDescription().trim());
        }

        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            reel.setVideoUrl(request.getVideoUrl().trim());
        }

        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isBlank()) {
            reel.setThumbnailUrl(request.getThumbnailUrl().trim());
        }

        CraftReel saved = craftReelRepository.save(reel);
        reelCache.put(saved.getId(), saved);
        feedCache.clear();
        return saved;
    }

    // ==========================================
    // DELETE REEL
    // ==========================================

    @Transactional
    public void deleteReel(String identifier, Long reelId) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CraftReel reel = craftReelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found with id: " + reelId));

        if (user.getRole() != Role.ADMIN) {
            if (reel.getArtisan() == null || reel.getArtisan().getUser() == null ||
                    !reel.getArtisan().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You are not allowed to delete this craft reel");
            }
        }

        craftReelRepository.delete(reel);
        reelCache.remove(reelId);
        feedCache.clear();
    }
}