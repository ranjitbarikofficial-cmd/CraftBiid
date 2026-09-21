package com.craftbid.controller;

import com.craftbid.dto.PublicCraftResponse;
import com.craftbid.entity.Craft;
import com.craftbid.service.CraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/crafts")
public class CraftController {

    private final CraftService craftService;

    public CraftController(CraftService craftService) {
        this.craftService = craftService;
    }

    // ==========================================
    // UPLOAD CRAFT + IMAGE + CRAFT REEL VIDEO
    // ==========================================

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PublicCraftResponse> uploadCraft(
            @RequestParam @NotBlank(message = "Title is required") @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters") String title,
            @RequestParam @NotBlank(message = "Category is required") @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters") String category,
            @RequestParam(required = false) @Size(max = 2000, message = "Description cannot exceed 2000 characters") String description,
            @RequestParam @NotNull(message = "Base price is required") @DecimalMin(value = "1.00", message = "Base price must be at least 1.00") BigDecimal basePrice,
            @RequestParam("image") @NotNull(message = "Image file is required") MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(required = false, defaultValue = "true") Boolean isLiveForAuction) {

        Craft craft = craftService.uploadCraft(
                title,
                category,
                description,
                basePrice,
                image,
                video,
                isLiveForAuction
        );

        return ResponseEntity.ok(PublicCraftResponse.fromEntity(craft));
    }

    // ==========================================
    // GET ALL CRAFTS (PUBLIC)
    // ==========================================

    @GetMapping
    public ResponseEntity<List<PublicCraftResponse>> getAllCrafts() {
        List<Craft> crafts = craftService.getAllCrafts();
        List<PublicCraftResponse> response = crafts.stream()
                .map(PublicCraftResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET MY CRAFTS (ARTISAN)
    // ==========================================

    @GetMapping("/my")
    public ResponseEntity<List<PublicCraftResponse>> getMyCrafts() {
        List<Craft> crafts = craftService.getMyCrafts();
        List<PublicCraftResponse> response = crafts.stream()
                .map(PublicCraftResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET CRAFTS BY CATEGORY (PUBLIC)
    // ==========================================

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<PublicCraftResponse>> getCraftsByCategory(
            @PathVariable @Positive(message = "Category ID must be positive") Long categoryId) {
        List<Craft> crafts = craftService.getCraftsByCategoryId(categoryId);
        List<PublicCraftResponse> response = crafts.stream()
                .map(PublicCraftResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // SEARCH & FILTER CRAFTS (PUBLIC)
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<PublicCraftResponse>> searchCrafts(
            @RequestParam(required = false) @Size(max = 100, message = "Keyword cannot exceed 100 characters") String keyword,
            @RequestParam(required = false) @Positive(message = "Category ID must be positive") Long categoryId,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "Min price cannot be negative") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "Max price cannot be negative") BigDecimal maxPrice) {

        List<Craft> crafts = craftService.searchCrafts(keyword, categoryId, minPrice, maxPrice);
        List<PublicCraftResponse> response = crafts.stream()
                .map(PublicCraftResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // TRIE AUTOCOMPLETE SUGGESTIONS (PUBLIC)
    // ==========================================

    @GetMapping("/autocomplete")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> autocomplete(
            @RequestParam @NotBlank(message = "Query prefix is required") @Size(min = 1, max = 50) String q,
            @RequestParam(required = false, defaultValue = "8") int limit) {

        return ResponseEntity.ok(craftService.autocomplete(q, limit));
    }

    // ==========================================
    // GET CRAFT BY ID (PUBLIC)
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<PublicCraftResponse> getCraftById(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id) {

        Craft craft = craftService.getCraftById(id);
        return ResponseEntity.ok(PublicCraftResponse.fromEntity(craft));
    }

    // ==========================================
    // UPDATE CRAFT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<PublicCraftResponse> updateCraft(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id,
            @Valid @RequestBody Craft craft) {

        Craft updated = craftService.updateCraft(id, craft);
        return ResponseEntity.ok(PublicCraftResponse.fromEntity(updated));
    }

    // ==========================================
    // UPDATE CRAFT (MULTIPART WITH OPTIONAL IMAGE REPLACEMENT)
    // ==========================================

    @PostMapping(
            value = "/{id}/update-media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PublicCraftResponse> updateCraftWithMedia(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id,
            @RequestParam(required = false) @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters") String title,
            @RequestParam(required = false) @Size(min = 2, max = 100, message = "Category must be between 2 and 100 characters") String category,
            @RequestParam(required = false) @Size(max = 2000, message = "Description cannot exceed 2000 characters") String description,
            @RequestParam(required = false) @DecimalMin(value = "1.00", message = "Base price must be at least 1.00") BigDecimal basePrice,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(required = false) Boolean isLiveForAuction) {

        Craft updated = craftService.updateCraftWithMedia(
                id,
                title,
                category,
                description,
                basePrice,
                image,
                isLiveForAuction
        );

        return ResponseEntity.ok(PublicCraftResponse.fromEntity(updated));
    }

    // ==========================================
    // TOGGLE LIVE / OFFLINE STATUS
    // ==========================================

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PublicCraftResponse> toggleLiveStatus(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id,
            @RequestParam(required = false) Boolean isLive) {

        Craft updated = craftService.toggleLiveStatus(id, isLive);
        return ResponseEntity.ok(PublicCraftResponse.fromEntity(updated));
    }

    // ==========================================
    // DELETE CRAFT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCraft(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id) {

        craftService.deleteCraft(id);
        return ResponseEntity.ok("Craft deleted successfully");
    }
}