package com.craftbid.service;

import com.craftbid.entity.ArtisanFollow;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.Craft;
import com.craftbid.entity.CraftReel;
import com.craftbid.entity.User;
import com.craftbid.repository.ArtisanFollowRepository;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.CraftReelRepository;
import com.craftbid.repository.CraftRepository;
import com.craftbid.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final ArtisanFollowRepository followRepository;
    private final UserRepository userRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final CraftReelRepository reelRepository;
    private final CraftRepository craftRepository;

    public FollowService(
            ArtisanFollowRepository followRepository,
            UserRepository userRepository,
            ArtisanProfileRepository artisanProfileRepository,
            CraftReelRepository reelRepository,
            CraftRepository craftRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.reelRepository = reelRepository;
        this.craftRepository = craftRepository;
    }

    // ==========================================
    // TOGGLE FOLLOW
    // ==========================================
    @Transactional
    public Map<String, Object> toggleFollow(String followerIdentifier, Long artisanUserId) {
        User follower = userRepository.findByEmailOrPhone(followerIdentifier, followerIdentifier)
                .orElseThrow(() -> new RuntimeException("Follower user not found: " + followerIdentifier));

        User artisan = userRepository.findById(artisanUserId)
                .orElseThrow(() -> new RuntimeException("Artisan user not found with id: " + artisanUserId));

        if (follower.getId().equals(artisan.getId())) {
            throw new RuntimeException("You cannot follow yourself!");
        }

        Optional<ArtisanFollow> existing = followRepository.findByFollowerAndArtisan(follower, artisan);
        boolean following;

        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            following = false;
        } else {
            followRepository.save(new ArtisanFollow(follower, artisan));
            following = true;
        }

        long followerCount = followRepository.countByArtisan(artisan);

        return Map.of(
                "following", following,
                "followerCount", followerCount,
                "artisanId", artisanUserId,
                "message", following ? "Now following " + artisan.getName() : "Unfollowed " + artisan.getName()
        );
    }

    // ==========================================
    // IS FOLLOWING STATUS
    // ==========================================
    public boolean isFollowing(String followerIdentifier, Long artisanUserId) {
        if (followerIdentifier == null || followerIdentifier.isBlank()) {
            return false;
        }
        Optional<User> follower = userRepository.findByEmailOrPhone(followerIdentifier, followerIdentifier);
        if (follower.isEmpty()) {
            return false;
        }
        Optional<User> artisan = userRepository.findById(artisanUserId);
        if (artisan.isEmpty()) {
            return false;
        }

        return followRepository.existsByFollowerAndArtisan(follower.get(), artisan.get());
    }

    // ==========================================
    // GET FOLLOWER COUNT
    // ==========================================
    public long getFollowerCount(Long artisanUserId) {
        return userRepository.findById(artisanUserId)
                .map(followRepository::countByArtisan)
                .orElse(0L);
    }

    // ==========================================
    // GET MY FOLLOWED ARTISANS
    // ==========================================
    public List<ArtisanProfile> getMyFollowedArtisans(String followerIdentifier) {
        User follower = userRepository.findByEmailOrPhone(followerIdentifier, followerIdentifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ArtisanFollow> follows = followRepository.findAllByFollower(follower);
        List<User> artisanUsers = follows.stream().map(ArtisanFollow::getArtisan).toList();

        if (artisanUsers.isEmpty()) {
            return Collections.emptyList();
        }

        return artisanUsers.stream()
                .map(artisanProfileRepository::findByUser)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    // ==========================================
    // GET REELS FROM FOLLOWED ARTISANS
    // ==========================================
    public List<CraftReel> getFollowingReels(String followerIdentifier) {
        User follower = userRepository.findByEmailOrPhone(followerIdentifier, followerIdentifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ArtisanFollow> follows = followRepository.findAllByFollower(follower);
        List<User> artisanUsers = follows.stream().map(ArtisanFollow::getArtisan).toList();

        if (artisanUsers.isEmpty()) {
            return Collections.emptyList();
        }

        return reelRepository.findByArtisanUsersAndStatusOrderByCreatedAtDesc(artisanUsers, "ACTIVE");
    }

    // ==========================================
    // GET NEW CRAFTS FROM FOLLOWED ARTISANS
    // ==========================================
    public List<Craft> getFollowingCrafts(String followerIdentifier) {
        User follower = userRepository.findByEmailOrPhone(followerIdentifier, followerIdentifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ArtisanFollow> follows = followRepository.findAllByFollower(follower);
        List<User> artisanUsers = follows.stream().map(ArtisanFollow::getArtisan).toList();

        if (artisanUsers.isEmpty()) {
            return Collections.emptyList();
        }

        return craftRepository.findBySellerInAndStatusOrderByCreatedAtDesc(artisanUsers, "ACTIVE");
    }
}
