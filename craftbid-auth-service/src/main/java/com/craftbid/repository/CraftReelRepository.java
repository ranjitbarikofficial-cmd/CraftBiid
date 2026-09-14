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

    @Query("SELECT r FROM CraftReel r LEFT JOIN FETCH r.artisan a LEFT JOIN FETCH a.user LEFT JOIN FETCH r.craft c LEFT JOIN FETCH c.category LEFT JOIN FETCH c.seller WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<CraftReel> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT r FROM CraftReel r LEFT JOIN FETCH r.artisan a LEFT JOIN FETCH a.user LEFT JOIN FETCH r.craft c LEFT JOIN FETCH c.category LEFT JOIN FETCH c.seller WHERE r.artisan.user IN :artisanUsers AND r.status = :status ORDER BY r.createdAt DESC")
    List<CraftReel> findByArtisanUsersAndStatusOrderByCreatedAtDesc(
            @Param("artisanUsers") List<User> artisanUsers,
            @Param("status") String status
    );
}