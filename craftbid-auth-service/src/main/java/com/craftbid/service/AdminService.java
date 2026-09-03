package com.craftbid.service;

import com.craftbid.dto.AdminDashboardStatsDTO;
import com.craftbid.entity.*;
import com.craftbid.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CraftRepository craftRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AdminService(
            UserRepository userRepository,
            CraftRepository craftRepository,
            AuctionRepository auctionRepository,
            BidRepository bidRepository) {

        this.userRepository = userRepository;
        this.craftRepository = craftRepository;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    public AdminDashboardStatsDTO getStats() {
        long totalUsers = userRepository.count();
        long totalArtisans = userRepository.findAll().stream().filter(User::isSellerEnabled).count();
        long totalCrafts = craftRepository.count();
        long totalAuctions = auctionRepository.count();
        long totalActiveAuctions = auctionRepository.findByStatusOrderByEndTimeAsc(AuctionStatus.ACTIVE).size();
        long totalBids = bidRepository.count();

        return new AdminDashboardStatsDTO(
                totalUsers,
                totalArtisans,
                totalCrafts,
                totalAuctions,
                totalActiveAuctions,
                totalBids
        );
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    public List<Craft> getAllCrafts() {
        return craftRepository.findAll();
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }
}
