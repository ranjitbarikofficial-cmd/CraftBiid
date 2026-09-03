package com.craftbid.controller;

import com.craftbid.entity.Craft;
import com.craftbid.service.CraftService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

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
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal basePrice,
            @RequestParam("image") MultipartFile image,
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
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(craftService.getCraftsByCategoryId(categoryId));
    }

    // ==========================================
    // SEARCH & FILTER CRAFTS (PUBLIC)
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<Craft>> searchCrafts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        return ResponseEntity.ok(
                craftService.searchCrafts(keyword, categoryId, minPrice, maxPrice)
        );
    }

    // ==========================================
    // GET CRAFT BY ID (PUBLIC)
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Craft> getCraftById(
            @PathVariable Long id) {

        return ResponseEntity.ok(craftService.getCraftById(id));
    }

    // ==========================================
    // UPDATE CRAFT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<Craft> updateCraft(
            @PathVariable Long id,
            @RequestBody Craft craft) {

        return ResponseEntity.ok(craftService.updateCraft(id, craft));
    }

    // ==========================================
    // TOGGLE LIVE / OFFLINE STATUS
    // ==========================================

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Craft> toggleLiveStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isLive) {

        return ResponseEntity.ok(craftService.toggleLiveStatus(id, isLive));
    }

    // ==========================================
    // DELETE CRAFT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCraft(
            @PathVariable Long id) {

        craftService.deleteCraft(id);
        return ResponseEntity.ok("Craft deleted successfully");
    }
}