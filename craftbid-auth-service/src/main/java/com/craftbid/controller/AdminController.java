package com.craftbid.controller;

import com.craftbid.dto.AdminDashboardStatsDTO;
import com.craftbid.entity.Auction;
import com.craftbid.entity.Craft;
import com.craftbid.entity.User;
import com.craftbid.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<User> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }

    @GetMapping("/crafts")
    public ResponseEntity<List<Craft>> getAllCrafts() {
        return ResponseEntity.ok(adminService.getAllCrafts());
    }

    @GetMapping("/auctions")
    public ResponseEntity<List<Auction>> getAllAuctions() {
        return ResponseEntity.ok(adminService.getAllAuctions());
    }
}
