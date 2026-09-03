package com.craftbid.repository;

import com.craftbid.entity.ArtisanFollow;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtisanFollowRepository extends JpaRepository<ArtisanFollow, Long> {

    Optional<ArtisanFollow> findByFollowerAndArtisan(User follower, User artisan);

    boolean existsByFollowerAndArtisan(User follower, User artisan);

    void deleteByFollowerAndArtisan(User follower, User artisan);

    long countByArtisan(User artisan);

    List<ArtisanFollow> findAllByFollower(User follower);

    List<ArtisanFollow> findAllByArtisan(User artisan);
}
