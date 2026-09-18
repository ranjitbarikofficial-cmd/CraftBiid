package com.craftbid.controller;

import com.craftbid.entity.CraftReel;
import com.craftbid.service.CraftReelService;
import com.craftbid.dto.UpdateReelRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/craft-reels")
public class CraftReelController {

    private final CraftReelService craftReelService;

    public CraftReelController(CraftReelService craftReelService) {
        this.craftReelService = craftReelService;
    }

    // ==========================================
    // CREATE REEL
    // ==========================================

    @PostMapping
    public ResponseEntity<CraftReel> createReel(
            Authentication authentication,
            @RequestParam @NotNull(message = "Craft ID is required") @Positive(message = "Craft ID must be positive") Long craftId,
            @RequestParam @NotBlank(message = "Title is required") @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters") String title,
            @RequestParam(required = false) @Size(max = 1000, message = "Description cannot exceed 1000 characters") String description,
            @RequestParam @NotBlank(message = "Video URL is required") @Size(max = 500, message = "Video URL cannot exceed 500 characters") String videoUrl,
            @RequestParam(required = false) @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters") String thumbnailUrl) {

        String identifier = authentication.getName();

        CraftReel reel = craftReelService.createReel(
                identifier,
                craftId,
                title,
                description,
                videoUrl,
                thumbnailUrl
        );

        return ResponseEntity.ok(reel);
    }

    // ==========================================
    // UPDATE REEL
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<CraftReel> updateReel(
            Authentication authentication,
            @PathVariable @Positive(message = "Reel ID must be positive") Long id,
            @Valid @RequestBody UpdateReelRequest request) {

        String identifier = authentication.getName();

        CraftReel updated = craftReelService.updateReel(
                identifier,
                id,
                request
        );

        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // DELETE REEL
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReel(
            Authentication authentication,
            @PathVariable @Positive(message = "Reel ID must be positive") Long id) {

        String identifier = authentication.getName();

        craftReelService.deleteReel(identifier, id);

        return ResponseEntity.ok("Craft reel deleted successfully");
    }

    // ==========================================
    // MY REELS
    // ==========================================

    @GetMapping("/my")
    public ResponseEntity<List<CraftReel>> getMyReels(
            Authentication authentication) {

        String identifier = authentication.getName();

        return ResponseEntity.ok(
                craftReelService.getMyReels(identifier)
        );
    }

    // ==========================================
    // HOME FEED (PUBLIC)
    // ==========================================

    @GetMapping("/home")
    public ResponseEntity<List<CraftReel>> getHomeReels() {

        return ResponseEntity.ok(
                craftReelService.getHomeReels()
        );
    }

    // ==========================================
    // REELS FOR SPECIFIC CRAFT (PUBLIC)
    // ==========================================

    @GetMapping("/craft/{craftId}")
    public ResponseEntity<List<CraftReel>> getReelsByCraftId(
            @PathVariable @Positive(message = "Craft ID must be positive") Long craftId) {

        return ResponseEntity.ok(
                craftReelService.getReelsByCraftId(craftId)
        );
    }

    // ==========================================
    // VIEW (PUBLIC)
    // ==========================================

    @PostMapping("/{id}/view")
    public ResponseEntity<CraftReel> incrementViews(
            @PathVariable @Positive(message = "Reel ID must be positive") Long id) {

        return ResponseEntity.ok(
                craftReelService.incrementViews(id)
        );
    }

    // ==========================================
    // LIKE (PUBLIC/AUTH)
    // ==========================================

    @PostMapping("/{id}/like")
    public ResponseEntity<CraftReel> likeReel(
            @PathVariable @Positive(message = "Reel ID must be positive") Long id) {

        return ResponseEntity.ok(
                craftReelService.likeReel(id)
        );
    }
}