package com.craftbid.repository;

import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtisanProfileRepository
        extends JpaRepository<ArtisanProfile, Long> {

    Optional<ArtisanProfile> findByUser(User user);

    boolean existsByUser(User user);
}