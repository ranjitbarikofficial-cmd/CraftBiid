package com.craftbid.controller;

import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.Craft;
import com.craftbid.entity.CraftReel;
import com.craftbid.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    // ==========================================
    // TOGGLE FOLLOW / UNFOLLOW
    // ==========================================
    @PostMapping("/toggle/{artisanUserId}")
    public ResponseEntity<Map<String, Object>> toggleFollow(
            Authentication authentication,
            @PathVariable Long artisanUserId) {

        String identifier = authentication.getName();
        Map<String, Object> result = followService.toggleFollow(identifier, artisanUserId);
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // CHECK IS FOLLOWING STATUS
    // ==========================================
    @GetMapping("/status/{artisanUserId}")
    public ResponseEntity<Map<String, Object>> getFollowStatus(
            Authentication authentication,
            @PathVariable Long artisanUserId) {

        String identifier = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        boolean isFollowing = followService.isFollowing(identifier, artisanUserId);
        long followerCount = followService.getFollowerCount(artisanUserId);

        return ResponseEntity.ok(Map.of(
                "following", isFollowing,
                "followerCount", followerCount,
                "artisanId", artisanUserId
        ));
    }

    // ==========================================
    // GET FOLLOWER COUNT (PUBLIC)
    // ==========================================
    @GetMapping("/count/{artisanUserId}")
    public ResponseEntity<Map<String, Object>> getFollowerCount(
            @PathVariable Long artisanUserId) {

        long count = followService.getFollowerCount(artisanUserId);
        return ResponseEntity.ok(Map.of(
                "artisanId", artisanUserId,
                "followerCount", count
        ));
    }

    // ==========================================
    // GET MY FOLLOWED ARTISANS
    // ==========================================
    @GetMapping("/my-artisans")
    public ResponseEntity<List<ArtisanProfile>> getMyFollowedArtisans(
            Authentication authentication) {

        String identifier = authentication.getName();
        return ResponseEntity.ok(
                followService.getMyFollowedArtisans(identifier)
        );
    }

    // ==========================================
    // GET REELS FROM FOLLOWED ARTISANS
    // ==========================================
    @GetMapping("/following-reels")
    public ResponseEntity<List<CraftReel>> getFollowingReels(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String identifier = authentication.getName();
        return ResponseEntity.ok(
                followService.getFollowingReels(identifier)
        );
    }

    // ==========================================
    // GET CRAFTS FROM FOLLOWED ARTISANS
    // ==========================================
    @GetMapping("/following-crafts")
    public ResponseEntity<List<Craft>> getFollowingCrafts(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String identifier = authentication.getName();
        return ResponseEntity.ok(
                followService.getFollowingCrafts(identifier)
        );
    }
}
