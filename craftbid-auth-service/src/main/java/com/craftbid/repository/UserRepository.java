package com.craftbid.repository;

import com.craftbid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<User> findByEmailOrPhone(
            String email,
            String phone
    );

    default Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return findByEmail(trimmed.toLowerCase());
        }
        return findByPhone(trimmed).or(() -> findByEmail(trimmed.toLowerCase()));
    }
}