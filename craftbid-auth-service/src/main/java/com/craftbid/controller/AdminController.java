package com.craftbid.controller;

import com.craftbid.dto.AdminDashboardStatsDTO;
import com.craftbid.dto.OrderResponseDTO;
import com.craftbid.dto.SellerSettlementDTO;
import com.craftbid.entity.Auction;
import com.craftbid.entity.Craft;
import com.craftbid.entity.User;
import com.craftbid.service.AdminService;
import com.craftbid.service.EmailService;
import com.craftbid.service.OrderService;
import com.craftbid.service.SellerSettlementService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final EmailService emailService;
    private final OrderService orderService;
    private final SellerSettlementService settlementService;

    public AdminController(AdminService adminService,
                           EmailService emailService,
                           OrderService orderService,
                           SellerSettlementService settlementService) {
        this.adminService = adminService;
        this.emailService = emailService;
        this.orderService = orderService;
        this.settlementService = settlementService;
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
    public ResponseEntity<User> toggleUserStatus(
            @PathVariable @Positive(message = "User ID must be positive") Long id) {
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

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersAdmin());
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SellerSettlementDTO>> getAllSettlements() {
        return ResponseEntity.ok(settlementService.getAllSettlements());
    }

    @PostMapping("/settlements/{id}/pay")
    public ResponseEntity<SellerSettlementDTO> markSettlementPaid(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String ref = payload != null ? payload.get("reference") : null;
        String method = payload != null ? payload.get("payoutMethod") : "BANK_TRANSFER";
        String notes = payload != null ? payload.get("notes") : "Paid by Admin";
        return ResponseEntity.ok(SellerSettlementDTO.fromEntity(
                settlementService.markSettlementPaid(id, ref, method, notes)
        ));
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(
            @RequestParam(defaultValue = "ranjitbarik.official@gmail.com") String to) {
        return ResponseEntity.ok(emailService.sendDiagnosticTestEmail(to));
    }
}
