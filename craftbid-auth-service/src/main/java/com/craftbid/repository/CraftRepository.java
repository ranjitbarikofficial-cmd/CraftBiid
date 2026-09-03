package com.craftbid.repository;

import com.craftbid.entity.Craft;
import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CraftRepository extends JpaRepository<Craft, Long> {

    List<Craft> findBySeller(User seller);

    List<Craft> findBySellerOrderByCreatedAtDesc(User seller);

    List<Craft> findBySellerInAndStatusOrderByCreatedAtDesc(List<User> sellers, String status);

    List<Craft> findByStatus(String status);

    List<Craft> findByStatusOrderByCreatedAtDesc(String status);

    List<Craft> findByCategoryId(Long categoryId);

    List<Craft> findByCategoryIdAndStatus(Long categoryId, String status);

    long countBySeller(User seller);

    @Query("SELECT c FROM Craft c WHERE c.status = 'ACTIVE' " +
           "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR c.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR c.basePrice <= :maxPrice) " +
           "ORDER BY c.createdAt DESC")
    List<Craft> searchCrafts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}