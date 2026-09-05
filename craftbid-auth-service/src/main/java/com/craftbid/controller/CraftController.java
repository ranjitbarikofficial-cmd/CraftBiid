package com.craftbid.controller;

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
    public ResponseEntity<Craft> uploadCraft(
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

        return ResponseEntity.ok(craft);
    }

    // ==========================================
    // GET ALL CRAFTS (PUBLIC)
    // ==========================================

    @GetMapping
    public ResponseEntity<List<Craft>> getAllCrafts() {
        return ResponseEntity.ok(craftService.getAllCrafts());
    }

    // ==========================================
    // GET MY CRAFTS (ARTISAN)
    // ==========================================

    @GetMapping("/my")
    public ResponseEntity<List<Craft>> getMyCrafts() {
        return ResponseEntity.ok(craftService.getMyCrafts());
    }

    // ==========================================
    // GET CRAFTS BY CATEGORY (PUBLIC)
    // ==========================================

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Craft>> getCraftsByCategory(
            @PathVariable @Positive(message = "Category ID must be positive") Long categoryId) {
        return ResponseEntity.ok(craftService.getCraftsByCategoryId(categoryId));
    }

    // ==========================================
    // SEARCH & FILTER CRAFTS (PUBLIC)
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<Craft>> searchCrafts(
            @RequestParam(required = false) @Size(max = 100, message = "Keyword cannot exceed 100 characters") String keyword,
            @RequestParam(required = false) @Positive(message = "Category ID must be positive") Long categoryId,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "Min price cannot be negative") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "Max price cannot be negative") BigDecimal maxPrice) {

        return ResponseEntity.ok(
                craftService.searchCrafts(keyword, categoryId, minPrice, maxPrice)
        );
    }

    // ==========================================
    // GET CRAFT BY ID (PUBLIC)
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Craft> getCraftById(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id) {

        return ResponseEntity.ok(craftService.getCraftById(id));
    }

    // ==========================================
    // UPDATE CRAFT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<Craft> updateCraft(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id,
            @Valid @RequestBody Craft craft) {

        return ResponseEntity.ok(craftService.updateCraft(id, craft));
    }

    // ==========================================
    // TOGGLE LIVE / OFFLINE STATUS
    // ==========================================

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Craft> toggleLiveStatus(
            @PathVariable @Positive(message = "Craft ID must be positive") Long id,
            @RequestParam(required = false) Boolean isLive) {

        return ResponseEntity.ok(craftService.toggleLiveStatus(id, isLive));
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