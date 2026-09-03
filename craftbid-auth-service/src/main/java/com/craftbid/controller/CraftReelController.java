package com.craftbid.controller;

import com.craftbid.entity.CraftReel;
import com.craftbid.service.CraftReelService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam Long craftId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String videoUrl,
            @RequestParam(required = false) String thumbnailUrl) {

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
            @PathVariable Long craftId) {

        return ResponseEntity.ok(
                craftReelService.getReelsByCraftId(craftId)
        );
    }

    // ==========================================
    // VIEW (PUBLIC)
    // ==========================================

    @PostMapping("/{id}/view")
    public ResponseEntity<CraftReel> incrementViews(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                craftReelService.incrementViews(id)
        );
    }

    // ==========================================
    // LIKE (PUBLIC/AUTH)
    // ==========================================

    @PostMapping("/{id}/like")
    public ResponseEntity<CraftReel> likeReel(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                craftReelService.likeReel(id)
        );
    }
}