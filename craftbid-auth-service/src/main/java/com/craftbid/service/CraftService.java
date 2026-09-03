package com.craftbid.service;

import com.craftbid.entity.Category;
import com.craftbid.entity.Craft;
import com.craftbid.entity.Role;
import com.craftbid.entity.User;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.CraftRepository;
import com.craftbid.repository.UserRepository;
import com.craftbid.repository.CategoryRepository;
import com.craftbid.repository.CraftReelRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.CraftReel;
import com.craftbid.repository.ArtisanProfileRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class CraftService {

    private final CraftRepository craftRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final CraftReelRepository craftReelRepository;
    private final FileStorageService fileStorageService;

    public CraftService(
            CraftRepository craftRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ArtisanProfileRepository artisanProfileRepository,
            CraftReelRepository craftReelRepository,
            FileStorageService fileStorageService) {

        this.craftRepository = craftRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.craftReelRepository = craftReelRepository;
        this.fileStorageService = fileStorageService;
    }

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String identifier = authentication.getName();
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    private void checkSellerEnabled(User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (!user.isSellerEnabled()) {
            throw new AccessDeniedException("Please enable Artisan mode before uploading crafts");
        }
    }

    public Craft uploadCraft(
            String title,
            String categoryIdentifier,
            String description,
            BigDecimal basePrice,
            MultipartFile image,
            MultipartFile video,
            Boolean isLiveForAuction) {

        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Craft title is required");
        }

        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be greater than zero");
        }

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Craft image is required");
        }

        // Validate file sizes (10MB for image, 100MB for video)
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image file size must not exceed 10 MB");
        }

        if (video != null && !video.isEmpty() && video.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("Video file size must not exceed 100 MB");
        }

        try {
            User seller = getLoggedInUser();
            checkSellerEnabled(seller);

            // Resolve or create Artisan Profile if needed
            ArtisanProfile artisan = artisanProfileRepository.findByUser(seller)
                    .orElseGet(() -> {
                        ArtisanProfile newProfile = new ArtisanProfile();
                        newProfile.setUser(seller);
                        newProfile.setShopName(seller.getName() + "'s Craft Studio");
                        newProfile.setCraftType("Handmade Crafts");
                        newProfile.setCity("Local");
                        return artisanProfileRepository.save(newProfile);
                    });

            // Resolve Category
            Category category = resolveCategory(categoryIdentifier);

            // Store craft image
            String imageUrl = fileStorageService.saveFile(image, "crafts");

            // Save Craft
            Craft craft = new Craft();
            craft.setTitle(title.trim());
            craft.setDescription(description != null ? description.trim() : "");
            craft.setBasePrice(basePrice);
            craft.setImageUrl(imageUrl);
            craft.setCategory(category);
            craft.setSeller(seller);
            craft.setStatus((isLiveForAuction != null && !isLiveForAuction) ? "OFFLINE" : "ACTIVE");

            Craft savedCraft = craftRepository.save(craft);

            // Create Craft Reel if video is provided
            if (video != null && !video.isEmpty()) {
                String videoUrl = fileStorageService.saveFile(video, "reels");
                CraftReel reel = new CraftReel();
                reel.setArtisan(artisan);
                reel.setCraft(savedCraft);
                reel.setTitle(title.trim());
                reel.setDescription(description != null ? description.trim() : "");
                reel.setVideoUrl(videoUrl);
                reel.setThumbnailUrl(imageUrl);
                reel.setViews(0L);
                reel.setLikes(0L);
                reel.setStatus("ACTIVE");

                craftReelRepository.save(reel);
            }

            return savedCraft;

        } catch (IOException e) {
            throw new RuntimeException("Unable to upload craft media files: " + e.getMessage(), e);
        }
    }

    private Category resolveCategory(String categoryIdentifier) {
        if (categoryIdentifier == null || categoryIdentifier.trim().isBlank()) {
            return categoryRepository.findAll().stream().findFirst()
                    .orElseGet(() -> {
                        Category def = new Category();
                        def.setName("Handmade Crafts");
                        def.setDescription("Unique handcrafted items");
                        return categoryRepository.save(def);
                    });
        }

        String trimmed = categoryIdentifier.trim();

        // Try numeric ID lookup
        try {
            Long catId = Long.parseLong(trimmed);
            return categoryRepository.findById(catId)
                    .orElseGet(() -> resolveByNameOrCreate(trimmed));
        } catch (NumberFormatException ignored) {
            return resolveByNameOrCreate(trimmed);
        }
    }

    private Category resolveByNameOrCreate(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName(name);
                    newCat.setDescription("Handcrafted items in " + name);
                    return categoryRepository.save(newCat);
                });
    }

    public List<Craft> getAllCrafts() {
        return craftRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    public List<Craft> getMyCrafts() {
        User seller = getLoggedInUser();
        return craftRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<Craft> getCraftsByCategoryId(Long categoryId) {
        return craftRepository.findByCategoryIdAndStatus(categoryId, "ACTIVE");
    }

    public List<Craft> searchCrafts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        String cleanKeyword = (keyword != null && !keyword.trim().isBlank()) ? keyword.trim() : null;
        return craftRepository.searchCrafts(cleanKeyword, categoryId, minPrice, maxPrice);
    }

    public Craft getCraftById(Long id) {
        return craftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Craft not found with id: " + id));
    }

    public Craft updateCraft(Long id, Craft updatedCraft) {
        Craft existingCraft = getCraftById(id);
        User loggedInUser = getLoggedInUser();

        if (loggedInUser.getRole() != Role.ADMIN) {
            checkSellerEnabled(loggedInUser);
            checkOwnership(existingCraft, loggedInUser);
        }

        if (updatedCraft.getTitle() != null && !updatedCraft.getTitle().isBlank()) {
            existingCraft.setTitle(updatedCraft.getTitle().trim());
        }

        if (updatedCraft.getDescription() != null) {
            existingCraft.setDescription(updatedCraft.getDescription().trim());
        }

        if (updatedCraft.getBasePrice() != null && updatedCraft.getBasePrice().compareTo(BigDecimal.ZERO) > 0) {
            existingCraft.setBasePrice(updatedCraft.getBasePrice());
        }

        if (updatedCraft.getImageUrl() != null && !updatedCraft.getImageUrl().isBlank()) {
            existingCraft.setImageUrl(updatedCraft.getImageUrl());
        }

        if (updatedCraft.getCategory() != null) {
            existingCraft.setCategory(updatedCraft.getCategory());
        }

        if (updatedCraft.getStatus() != null && !updatedCraft.getStatus().isBlank()) {
            existingCraft.setStatus(updatedCraft.getStatus());
        }

        return craftRepository.save(existingCraft);
    }

    public Craft toggleLiveStatus(Long id, Boolean isLive) {
        Craft existingCraft = getCraftById(id);
        User loggedInUser = getLoggedInUser();

        if (loggedInUser.getRole() != Role.ADMIN) {
            checkSellerEnabled(loggedInUser);
            checkOwnership(existingCraft, loggedInUser);
        }

        if (isLive != null) {
            existingCraft.setStatus(isLive ? "ACTIVE" : "OFFLINE");
        } else {
            existingCraft.setStatus("ACTIVE".equalsIgnoreCase(existingCraft.getStatus()) ? "OFFLINE" : "ACTIVE");
        }

        return craftRepository.save(existingCraft);
    }

    public void deleteCraft(Long id) {
        Craft existingCraft = getCraftById(id);
        User loggedInUser = getLoggedInUser();

        if (loggedInUser.getRole() != Role.ADMIN) {
            checkSellerEnabled(loggedInUser);
            checkOwnership(existingCraft, loggedInUser);
        }

        craftRepository.delete(existingCraft);
    }

    private void checkOwnership(Craft craft, User loggedInUser) {
        if (loggedInUser.getRole() == Role.ADMIN) {
            return;
        }

        if (craft.getSeller() == null || !craft.getSeller().getId().equals(loggedInUser.getId())) {
            throw new AccessDeniedException("You are not allowed to modify this craft");
        }
    }
}