package com.craftbid.scheduler;

import com.craftbid.entity.Auction;
import com.craftbid.repository.AuctionRepository;
import com.craftbid.service.AuctionService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    public AuctionScheduler(AuctionRepository auctionRepository, AuctionService auctionService) {
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    /**
     * Runs every 1 second to authoritatively advance expired participation windows
     * and conclude active 60-second live turn bidding countdowns.
     */
    @Scheduled(fixedDelay = 1000)
    public void processAuctionLifeCycles() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Evaluate expired 24-hour participation windows
        try {
            List<Auction> expiredParticipation = auctionRepository.findExpiredParticipationAuctions(now);
            for (Auction auction : expiredParticipation) {
                auctionService.evaluateParticipationWindow(auction);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Scheduler error in participation evaluation: " + e.getMessage());
        }

        // 2. Conclude expired 60-second live turn bidding sessions
        try {
            List<Auction> expiredTurns = auctionRepository.findExpiredTurnAuctions(now);
            for (Auction auction : expiredTurns) {
                auctionService.finalizeAuction(auction);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Scheduler error in turn finalization: " + e.getMessage());
        }
    }
}
