package com.craftbid.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "artisan_follows", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "artisan_id"})
})
public class ArtisanFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who is following
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    // The artisan (seller) who is being followed
    @ManyToOne
    @JoinColumn(name = "artisan_id", nullable = false)
    private User artisan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ArtisanFollow() {
    }

    public ArtisanFollow(User follower, User artisan) {
        this.follower = follower;
        this.artisan = artisan;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public User getArtisan() {
        return artisan;
    }

    public void setArtisan(User artisan) {
        this.artisan = artisan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
