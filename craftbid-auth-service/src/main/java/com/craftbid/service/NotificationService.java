package com.craftbid.service;

import com.craftbid.entity.Notification;
import com.craftbid.entity.User;
import com.craftbid.repository.NotificationRepository;
import com.craftbid.repository.UserRepository;
import com.craftbid.websocket.AuctionEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuctionEventPublisher eventPublisher;
    private final EmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            AuctionEventPublisher eventPublisher,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
    }

    private User getUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }

    @Transactional
    public Notification createNotification(User user, String title, String message, String type, String link) {
        Notification notification = new Notification(user, title, message, type, link);
        Notification saved = notificationRepository.save(notification);

        // Push real-time event to the user's private WebSocket queue
        try {
            eventPublisher.publishUserEvent(user.getId(), "NOTIFICATION_RECEIVED", saved);
        } catch (Exception ignored) {}

        return saved;
    }

    public List<Notification> getMyNotifications(String identifier) {
        User user = getUserByIdentifier(identifier);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String identifier) {
        User user = getUserByIdentifier(identifier);
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public Notification markAsRead(String identifier, Long notificationId) {
        User user = getUserByIdentifier(identifier);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to modify this notification");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String identifier) {
        User user = getUserByIdentifier(identifier);
        notificationRepository.markAllAsReadForUser(user);
    }

    // =========================================================================
    // HIGH-LEVEL NOTIFICATION + EMAIL DISPATCH HELPERS
    // =========================================================================

    public void notifyAuctionJoined(User user, String craftName, BigDecimal basePrice, Long auctionId) {
        String title = "Auction Room Joined";
        String message = "You have successfully joined the 24-hour participation window for \"" + craftName + "\" by paying ₹" + basePrice + " base deposit.";
        String link = "/auctions/" + auctionId;
        createNotification(user, title, message, "AUCTION_JOINED", link);
        emailService.sendAuctionJoinedEmail(user.getEmail(), user.getName(), craftName, basePrice, auctionId);
    }

    public void notifyAuctionLiveStarted(User user, String craftName, Long auctionId) {
        String title = "⚡ Live Auction Started!";
        String message = "The live 1-minute auction for \"" + craftName + "\" is now LIVE! Place your differential bids now.";
        String link = "/auctions/" + auctionId;
        createNotification(user, title, message, "AUCTION_LIVE", link);
        emailService.sendAuctionLiveStartedEmail(user.getEmail(), user.getName(), craftName, auctionId);
    }

    public void notifyOutbid(User user, String craftName, BigDecimal newHighestBid, Long auctionId) {
        String title = "You were outbid!";
        String message = "Another bidder placed a higher bid of ₹" + newHighestBid + " on \"" + craftName + "\". Bid now before the 60s timer expires!";
        String link = "/auctions/" + auctionId;
        createNotification(user, title, message, "OUTBID", link);
    }

    public void notifyAuctionWon(User user, String craftName, BigDecimal winningAmount, Long auctionId) {
        String title = "🏆 Congratulations! You Won the Auction!";
        String message = "You won \"" + craftName + "\" for ₹" + winningAmount + "! Please provide your delivery address to dispatch your craft.";
        String link = "/auctions/" + auctionId;
        createNotification(user, title, message, "AUCTION_WON", link);
        emailService.sendAuctionWonEmail(user.getEmail(), user.getName(), craftName, winningAmount, auctionId);
    }

    public void notifyRefundProcessed(User user, String craftName, BigDecimal refundAmount, String txnRef, Long auctionId) {
        String title = "💰 100% Refund Processed";
        String message = "Your ₹" + refundAmount + " deposit for \"" + craftName + "\" has been 100% refunded to your original payment method. Ref: " + txnRef;
        String link = "/profile";
        createNotification(user, title, message, "REFUND_PROCESSED", link);
        emailService.sendAuctionRefundEmail(user.getEmail(), user.getName(), craftName, refundAmount, txnRef, auctionId);
    }

    public void notifyArtisanCraftSold(User artisan, String craftName, BigDecimal finalAmount, BigDecimal payoutAmount, Long auctionId) {
        String title = "🎉 Craft Sold!";
        String message = "Your craft \"" + craftName + "\" sold at auction for ₹" + finalAmount + "! Your payout is ₹" + payoutAmount + " (after 10% platform fee).";
        String link = "/artisan-dashboard";
        createNotification(artisan, title, message, "CRAFT_SOLD", link);
        emailService.sendArtisanCraftSoldEmail(artisan.getEmail(), artisan.getName(), craftName, finalAmount, payoutAmount, auctionId);
    }

    public void notifyOrderShipped(User buyer, String craftName, String trackingNotes, String carrier, Long orderId) {
        String title = "📦 Order Shipped!";
        String message = "Your craft \"" + craftName + "\" has been shipped by the artisan! " + (trackingNotes != null ? "Tracking: " + trackingNotes : "");
        String link = "/profile";
        createNotification(buyer, title, message, "ORDER_SHIPPED", link);
        emailService.sendOrderShippedEmail(buyer.getEmail(), buyer.getName(), craftName, trackingNotes, carrier, orderId);
    }
}
