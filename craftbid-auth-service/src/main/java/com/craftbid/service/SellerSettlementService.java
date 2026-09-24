package com.craftbid.service;

import com.craftbid.dto.SellerSettlementDTO;
import com.craftbid.entity.ArtisanProfile;
import com.craftbid.entity.AuctionOrder;
import com.craftbid.entity.SellerSettlement;
import com.craftbid.entity.User;
import com.craftbid.repository.ArtisanProfileRepository;
import com.craftbid.repository.SellerSettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SellerSettlementService {

    private static final Logger logger = LoggerFactory.getLogger(SellerSettlementService.class);

    private final SellerSettlementRepository settlementRepository;
    private final ArtisanProfileRepository artisanProfileRepository;
    private final NotificationService notificationService;

    @Value("${craftbid.commission.platform-rate:10.00}")
    private BigDecimal platformCommissionRate = new BigDecimal("10.00");

    public SellerSettlementService(
            SellerSettlementRepository settlementRepository,
            ArtisanProfileRepository artisanProfileRepository,
            NotificationService notificationService) {
        this.settlementRepository = settlementRepository;
        this.artisanProfileRepository = artisanProfileRepository;
        this.notificationService = notificationService;
    }

    public BigDecimal getPlatformCommissionRate() {
        return platformCommissionRate != null ? platformCommissionRate : new BigDecimal("10.00");
    }

    @Transactional
    public SellerSettlement createPendingSettlement(AuctionOrder order, User artisan, BigDecimal winningAmount, BigDecimal platformFee, BigDecimal artisanPayout) {
        Optional<SellerSettlement> existing = settlementRepository.findByOrder(order);
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal rate = getPlatformCommissionRate();

        if (platformFee == null) {
            platformFee = winningAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (artisanPayout == null) {
            artisanPayout = winningAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        }

        SellerSettlement settlement = new SellerSettlement(order, artisan, winningAmount, platformFee, artisanPayout);
        settlement.setPlatformCommissionRate(rate);
        settlement.setStatus("PENDING");

        // Attach Artisan payout info snapshot if available
        populateArtisanBeneficiaryDetails(settlement, artisan);

        return settlementRepository.save(settlement);
    }

    /**
     * When an order is DELIVERED or COMPLETED, the settlement becomes ELIGIBLE / PENDING_PAYOUT.
     * STRICT RULE: DELIVERED does NOT mean PAID. Payout requires admin execution / gateway transfer.
     */
    @Transactional
    public SellerSettlement processDeliverySettlement(AuctionOrder order) {
        SellerSettlement settlement = order.getSellerSettlement();
        if (settlement == null) {
            settlement = settlementRepository.findByOrder(order).orElse(null);
        }
        if (settlement == null) {
            settlement = createPendingSettlement(order, order.getArtisan(), order.getWinningAmount(), order.getPlatformFee(), order.getArtisanPayout());
        }

        if ("PENDING".equalsIgnoreCase(settlement.getStatus())) {
            settlement.setStatus("PENDING_PAYOUT");
            settlement.setNotes("Order successfully delivered. Settlement is eligible for payout release.");
            populateArtisanBeneficiaryDetails(settlement, order.getArtisan());
            settlement = settlementRepository.save(settlement);

            try {
                notificationService.notifyArtisanCraftSold(
                    settlement.getArtisan(),
                    order.getAuction().getCraft().getTitle(),
                    settlement.getWinningAmount(),
                    settlement.getArtisanPayout(),
                    order.getAuction().getId()
                );
            } catch (Exception ignored) {}
        }
        return settlement;
    }

    /**
     * Admin executes payout to Artisan.
     * Enforces strict idempotency: cannot pay an already PAID settlement.
     */
    @Transactional
    public SellerSettlement markSettlementPaid(Long settlementId, String reference, String payoutMethod, String notes) {
        SellerSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        if ("PAID".equalsIgnoreCase(settlement.getStatus())) {
            throw new IllegalStateException("Settlement #" + settlementId + " is already PAID with reference " + settlement.getPayoutReference());
        }

        String actualRef = (reference != null && !reference.isBlank()) ? reference.trim() : "UTR-CB-" + System.currentTimeMillis() + "-" + settlementId;
        String actualMethod = (payoutMethod != null && !payoutMethod.isBlank()) ? payoutMethod.trim().toUpperCase() : settlement.getPayoutMethod();
        String actualNotes = (notes != null && !notes.isBlank()) ? notes.trim() : "Payout released by Admin";

        settlement.setStatus("PAID");
        settlement.setPayoutReference(actualRef);
        settlement.setPayoutMethod(actualMethod);
        settlement.setNotes(actualNotes);
        settlement.setSettledAt(LocalDateTime.now());

        // Ensure latest beneficiary snapshot is recorded
        populateArtisanBeneficiaryDetails(settlement, settlement.getArtisan());

        SellerSettlement saved = settlementRepository.save(settlement);

        try {
            String craftName = (saved.getOrder() != null && saved.getOrder().getAuction() != null && saved.getOrder().getAuction().getCraft() != null)
                    ? saved.getOrder().getAuction().getCraft().getTitle()
                    : "Handcrafted Art";
            Long orderId = saved.getOrder() != null ? saved.getOrder().getId() : null;

            notificationService.notifyArtisanPayoutCompleted(
                    saved.getArtisan(),
                    craftName,
                    saved.getArtisanPayout(),
                    saved.getPayoutReference(),
                    orderId
            );
        } catch (Exception e) {
            logger.warn("Could not send payout notification for settlement {}: {}", settlementId, e.getMessage());
        }

        return saved;
    }

    private void populateArtisanBeneficiaryDetails(SellerSettlement settlement, User artisan) {
        if (artisan == null) return;
        try {
            Optional<ArtisanProfile> profileOpt = artisanProfileRepository.findByUser(artisan);
            if (profileOpt.isPresent()) {
                ArtisanProfile p = profileOpt.get();
                if (p.getBankAccountNumber() != null && !p.getBankAccountNumber().isBlank()) {
                    settlement.setArtisanBankDetails("A/C: " + p.getBankAccountNumber() + " | IFSC: " + p.getBankIfscCode() + " | Name: " + p.getBankAccountName());
                }
                if (p.getUpiId() != null && !p.getUpiId().isBlank()) {
                    settlement.setArtisanUpiId(p.getUpiId());
                }
                if (p.getPayoutPreference() != null && !p.getPayoutPreference().isBlank()) {
                    settlement.setPayoutMethod(p.getPayoutPreference());
                }
            }
        } catch (Exception ignored) {}
    }

    public List<SellerSettlementDTO> getArtisanSettlements(User artisan) {
        return settlementRepository.findByArtisanOrderByCreatedAtDesc(artisan).stream()
                .map(SellerSettlementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SellerSettlementDTO> getAllSettlements() {
        return settlementRepository.findAll().stream()
                .map(SellerSettlementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<SellerSettlement> getSettlementByOrder(AuctionOrder order) {
        return settlementRepository.findByOrder(order);
    }
}
