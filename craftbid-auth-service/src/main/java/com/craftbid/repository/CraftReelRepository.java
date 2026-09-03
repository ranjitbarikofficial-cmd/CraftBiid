package com.craftbid.repository;

import com.craftbid.entity.CraftReel;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CraftReelRepository extends JpaRepository<CraftReel, Long> {

    List<CraftReel> findByArtisanId(Long artisanId);

    List<CraftReel> findByCraftId(Long craftId);

    List<CraftReel> findByStatus(String status);

    List<CraftReel> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT r FROM CraftReel r WHERE r.artisan.user IN :artisanUsers AND r.status = :status ORDER BY r.createdAt DESC")
    List<CraftReel> findByArtisanUsersAndStatusOrderByCreatedAtDesc(
            @Param("artisanUsers") List<User> artisanUsers,
            @Param("status") String status
    );
}