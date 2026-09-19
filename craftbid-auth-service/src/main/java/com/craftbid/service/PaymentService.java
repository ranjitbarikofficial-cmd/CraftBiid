package com.craftbid.service;

import com.craftbid.dto.PaymentRequest;
import com.craftbid.dto.PaymentStatsDTO;
import com.craftbid.dto.RazorpayVerifyRequest;
import com.craftbid.entity.*;
import com.craftbid.exception.AccessDeniedException;
import com.craftbid.repository.AuctionParticipantRepository;
import com.craftbid.repository.AuctionRepository;
import com.craftbid.repository.PaymentTransactionRepository;
import com.craftbid.repository.RefundRepository;
import com.craftbid.repository.UserRepository;
import com.craftbid.websocket.AuctionEventPublisher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionParticipantRepository participantRepository;
    private final RazorpayService razorpayService;
    private final AuctionEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentTransactionRepository paymentRepository,
            RefundRepository refundRepository,
            UserRepository userRepository,
            AuctionRepository auctionRepository,
            AuctionParticipantRepository participantRepository,
            RazorpayService razorpayService,
            AuctionEventPublisher eventPublisher,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.participantRepository = participantRepository;
        this.razorpayService = razorpayService;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    private User getUserByIdentifier(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));
    }

    /**
     * Create Razorpay Order specifically for joining an auction room.
     * Enforces starting price server-side, 10-participant ceiling, and non-seller check.
     */
    @Transactional
    public Map<String, Object> createParticipationOrder(String identifier, Long auctionId) {
        User buyer = getUserByIdentifier(identifier);
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));

        if (auction.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("Artisans cannot join their own auctions");
        }

        if (auction.getStatus() != AuctionStatus.ACTIVE && auction.getStatus() != AuctionStatus.SCHEDULED && auction.getStatus() != AuctionStatus.LIVE) {
            throw new RuntimeException("This auction is not open for joining (Status: " + auction.getStatus() + ")");
        }

        Optional<AuctionParticipant> existing = participantRepository.findByAuctionAndUser(auction, buyer);
        if (existing.isPresent()) {
            throw new RuntimeException("You have already joined this auction room");
        }

        int maxLimit = auction.getMaxParticipants() > 0 ? auction.getMaxParticipants() : 10;
        if (auction.getCurrentParticipantsCount() >= maxLimit) {
            throw new RuntimeException("Auction room is full! Maximum " + maxLimit + " participants reached.");
        }

        BigDecimal startingPrice = auction.getStartingPrice();
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid starting price for auction");
        }

        String receipt = "rcpt_auc_" + auction.getId() + "_" + System.currentTimeMillis();
        String notes = "Auction #" + auction.getId() + " Participation Deposit for " + auction.getCraft().getTitle() + " by " + buyer.getEmail();

        Map<String, Object> rzpOrder = razorpayService.createOrder(startingPrice, receipt, notes);

        String orderId = (String) rzpOrder.get("orderId");
        String txnRef = "CB-ORD-" + (orderId != null ? orderId : System.currentTimeMillis());

        PaymentTransaction tx = new PaymentTransaction(
                buyer,
                auction.getId(),
                auction.getCraft().getId(),
                startingPrice,
                "PARTICIPATION",
                "RAZORPAY",
                txnRef,
                "CREATED",
                "Created Razorpay order: " + orderId
        );
        tx.setRazorpayOrderId(orderId);
        tx.setReceipt(receipt);
        paymentRepository.save(tx);

        rzpOrder.put("auctionId", auction.getId());
        rzpOrder.put("craftTitle", auction.getCraft().getTitle());
        rzpOrder.put("userEmail", buyer.getEmail());
        rzpOrder.put("userName", buyer.getName());
        rzpOrder.put("userPhone", buyer.getPhone() != null ? buyer.getPhone() : "");
        rzpOrder.put("startingPrice", startingPrice);

        return rzpOrder;
    }

    /**
     * Create generic Razorpay Order (direct purchase, custom order, or auction deposit).
     */
    @Transactional
    public Map<String, Object> createRazorpayOrder(String identifier, BigDecimal amount, Long auctionId, Long craftId, String type) {
        if (auctionId != null && ("PARTICIPATION".equalsIgnoreCase(type) || "BASE_DEPOSIT".equalsIgnoreCase(type))) {
            return createParticipationOrder(identifier, auctionId);
        }

        User user = getUserByIdentifier(identifier);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String receipt = "rcpt_" + System.currentTimeMillis() + "_" + (auctionId != null ? auctionId : (craftId != null ? craftId : 0));
        String notes = (type != null ? type : "PAYMENT") + " by " + user.getEmail();

        Map<String, Object> rzpOrder = razorpayService.createOrder(amount, receipt, notes);
        String orderId = (String) rzpOrder.get("orderId");
        String txnRef = "CB-ORD-" + (orderId != null ? orderId : System.currentTimeMillis());

        PaymentTransaction tx = new PaymentTransaction(
                user,
                auctionId,
                craftId,
                amount,
                type != null ? type.toUpperCase() : "DIRECT_PURCHASE",
                "RAZORPAY",
                txnRef,
                "CREATED",
                "Created Razorpay order: " + orderId
        );
        tx.setRazorpayOrderId(orderId);
        tx.setReceipt(receipt);
        paymentRepository.save(tx);

        rzpOrder.put("auctionId", auctionId);
        rzpOrder.put("craftId", craftId);
        rzpOrder.put("userEmail", user.getEmail());
        rzpOrder.put("userName", user.getName());
        rzpOrder.put("userPhone", user.getPhone() != null ? user.getPhone() : "");

        return rzpOrder;
    }

    /**
     * Verify Razorpay Signature, transition status to CAPTURED, and activate Auction Participant.
     */
    @Transactional
    public PaymentTransaction verifyAndRecordRazorpayPayment(
            String identifier,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            Long auctionId,
            Long craftId,
            BigDecimal amount,
            String type,
            String method) {

        User user = getUserByIdentifier(identifier);

        boolean isValid = razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if (!isValid) {
            logger.error("Signature verification failed for orderId: {}, paymentId: {}", razorpayOrderId, razorpayPaymentId);
            throw new RuntimeException("Invalid Razorpay payment signature. Payment cannot be verified.");
        }

        Optional<PaymentTransaction> optTx = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        PaymentTransaction tx;

        if (optTx.isPresent()) {
            tx = optTx.get();
            if ("CAPTURED".equalsIgnoreCase(tx.getStatus()) || "SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                return tx;
            }
            tx.setRazorpayPaymentId(razorpayPaymentId);
            tx.setRazorpaySignature(razorpaySignature);
            tx.setStatus("CAPTURED");
            if (method != null && !method.isBlank()) {
                tx.setPaymentMethod(method.toUpperCase());
            }
            tx.setNotes("Payment captured via Razorpay. Order: " + razorpayOrderId + ", Payment: " + razorpayPaymentId);
        } else {
            String txnRef = "CB-RZP-" + (razorpayPaymentId != null ? razorpayPaymentId : System.currentTimeMillis());
            String paymentType = type != null ? type.toUpperCase() : "PARTICIPATION";
            String payMethod = method != null ? method.toUpperCase() : "RAZORPAY";

            tx = new PaymentTransaction(
                    user,
                    auctionId,
                    craftId,
                    amount != null ? amount : BigDecimal.ZERO,
                    paymentType,
                    payMethod,
                    txnRef,
                    "CAPTURED",
                    "Razorpay payment verified. Order ID: " + razorpayOrderId + ", Payment ID: " + razorpayPaymentId
            );
            tx.setRazorpayOrderId(razorpayOrderId);
            tx.setRazorpayPaymentId(razorpayPaymentId);
            tx.setRazorpaySignature(razorpaySignature);
        }

        PaymentTransaction savedTx = paymentRepository.save(tx);

        // If transaction is for an auction participation, enroll user as AuctionParticipant
        Long targetAuctionId = auctionId != null ? auctionId : savedTx.getAuctionId();
        if (targetAuctionId != null && ("PARTICIPATION".equalsIgnoreCase(savedTx.getType()) || "BASE_DEPOSIT".equalsIgnoreCase(savedTx.getType()))) {
            try {
                auctionRepository.findById(targetAuctionId).ifPresent(auction -> {
                    Optional<AuctionParticipant> existing = participantRepository.findByAuctionAndUser(auction, user);
                    if (existing.isEmpty()) {
                        AuctionParticipant participant = new AuctionParticipant(auction, user, savedTx.getAmount());
                        participant.setStatus("ACTIVE");
                        participantRepository.save(participant);

                        auction.setCurrentParticipantsCount(auction.getCurrentParticipantsCount() + 1);
                        auctionRepository.save(auction);

                        // Send transactional notifications
                        try {
                            notificationService.notifyAuctionJoined(user, auction.getCraft().getTitle(), savedTx.getAmount(), auction.getId());
                        } catch (Exception e) {
                            logger.warn("Notification dispatch failed on join: {}", e.getMessage());
                        }

                        // Broadcast real-time WebSocket event
                        try {
                            Map<String, Object> joinData = new HashMap<>();
                            joinData.put("auctionId", auction.getId());
                            joinData.put("currentParticipants", auction.getCurrentParticipantsCount());
                            joinData.put("maxParticipants", auction.getMaxParticipants());
                            Map<String, String> pInfo = new HashMap<>();
                            pInfo.put("name", user.getName());
                            pInfo.put("city", user.getCity());
                            joinData.put("participant", pInfo);

                            eventPublisher.publishAuctionEvent(auction.getId(), "auction:joined", joinData);
                        } catch (Exception e) {
                            logger.warn("WebSocket publish failed on join: {}", e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Error activating auction participant after payment verification: {}", e.getMessage(), e);
            }
        }

        return savedTx;
    }

    /**
     * Process incoming Razorpay Server Webhooks idempotently.
     */
    @Transactional
    public Map<String, Object> processWebhook(String rawPayload, String signatureHeader) {
        Map<String, Object> result = new HashMap<>();

        boolean isValid = razorpayService.verifyWebhookSignature(rawPayload, signatureHeader);
        if (!isValid) {
            logger.error("Invalid webhook signature received from Razorpay");
            result.put("status", "error");
            result.put("message", "Invalid signature");
            return result;
        }

        try {
            JSONObject payloadObj = new JSONObject(rawPayload);
            String event = payloadObj.optString("event");
            JSONObject payload = payloadObj.optJSONObject("payload");

            if (payload != null) {
                if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                    JSONObject paymentEntity = payload.optJSONObject("payment");
                    if (paymentEntity != null) {
                        JSONObject entity = paymentEntity.optJSONObject("entity");
                        if (entity != null) {
                            String orderId = entity.optString("order_id");
                            String paymentId = entity.optString("id");
                            String method = entity.optString("method");

                            if (orderId != null && !orderId.isBlank()) {
                                paymentRepository.findByRazorpayOrderId(orderId).ifPresent(tx -> {
                                    if (!"CAPTURED".equalsIgnoreCase(tx.getStatus()) && !"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                                        tx.setRazorpayPaymentId(paymentId);
                                        tx.setStatus("CAPTURED");
                                        if (method != null && !method.isBlank()) {
                                            tx.setPaymentMethod(method.toUpperCase());
                                        }
                                        paymentRepository.save(tx);
                                        logger.info("Webhook updated transaction to CAPTURED for order: {}", orderId);
                                    }
                                });
                            }
                        }
                    }
                } else if ("payment.failed".equals(event)) {
                    JSONObject paymentEntity = payload.optJSONObject("payment");
                    if (paymentEntity != null) {
                        JSONObject entity = paymentEntity.optJSONObject("entity");
                        if (entity != null) {
                            String orderId = entity.optString("order_id");
                            String paymentId = entity.optString("id");
                            String errorDesc = entity.optString("error_description");

                            if (orderId != null && !orderId.isBlank()) {
                                paymentRepository.findByRazorpayOrderId(orderId).ifPresent(tx -> {
                                    tx.setRazorpayPaymentId(paymentId);
                                    tx.setStatus("FAILED");
                                    tx.setNotes("Payment failed: " + errorDesc);
                                    paymentRepository.save(tx);
                                    logger.info("Webhook updated transaction to FAILED for order: {}", orderId);
                                });
                            }
                        }
                    }
                } else if ("refund.processed".equals(event)) {
                    JSONObject refundEntity = payload.optJSONObject("refund");
                    if (refundEntity != null) {
                        JSONObject entity = refundEntity.optJSONObject("entity");
                        if (entity != null) {
                            String refundId = entity.optString("id");
                            String paymentId = entity.optString("payment_id");

                            refundRepository.findByRazorpayRefundId(refundId).ifPresent(ref -> {
                                ref.setStatus("COMPLETED");
                                refundRepository.save(ref);
                            });
                            paymentRepository.findByRazorpayPaymentId(paymentId).ifPresent(tx -> {
                                tx.setStatus("REFUNDED");
                                paymentRepository.save(tx);
                            });
                        }
                    }
                }
            }

            result.put("status", "processed");
            result.put("event", event);
            return result;
        } catch (Exception e) {
            logger.error("Error processing Razorpay webhook payload: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Process Gateway and Ledger Refunds for losing bidders or cancellations.
     */
    @Transactional
    public Refund processRefund(Long paymentTransactionId, BigDecimal amount, String reason, String identifier) {
        User caller = getUserByIdentifier(identifier);
        if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied. Only administrators can initiate manual refunds.");
        }

        PaymentTransaction tx = paymentRepository.findById(paymentTransactionId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + paymentTransactionId));

        if ("REFUNDED".equalsIgnoreCase(tx.getStatus())) {
            throw new RuntimeException("Transaction has already been fully refunded");
        }

        BigDecimal refundAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? amount : tx.getAmount();

        Map<String, Object> rzpRefund = razorpayService.processRefund(tx.getRazorpayPaymentId(), refundAmount, reason);
        String refundId = (String) rzpRefund.get("refundId");
        String refundStatus = (String) rzpRefund.getOrDefault("status", "COMPLETED");

        Refund refund = new Refund(
                tx,
                tx.getUser(),
                tx.getAuctionId(),
                tx.getRazorpayPaymentId(),
                refundId,
                refundAmount,
                refundStatus,
                reason != null ? reason : "Payment refund initiated"
        );
        Refund savedRefund = refundRepository.save(refund);

        tx.setStatus("REFUNDED");
        tx.setNotes((tx.getNotes() != null ? tx.getNotes() + " | " : "") + "Refund processed: " + refundId + " (" + refundAmount + " INR)");
        paymentRepository.save(tx);

        // Update participant record if associated
        if (tx.getAuctionId() != null) {
            auctionRepository.findById(tx.getAuctionId()).ifPresent(auction -> {
                participantRepository.findByAuctionAndUser(auction, tx.getUser()).ifPresent(p -> {
                    p.setStatus("REFUNDED");
                    p.setRefundAmount(refundAmount);
                    participantRepository.save(p);
                });
                try {
                    notificationService.notifyRefundProcessed(tx.getUser(), auction.getCraft().getTitle(), refundAmount, refundId, auction.getId());
                } catch (Exception e) {
                    logger.warn("Could not dispatch refund notification: {}", e.getMessage());
                }
            });
        }

        return savedRefund;
    }

    @Transactional
    public PaymentTransaction refundAuctionParticipant(User user, Long auctionId, Long craftId, BigDecimal amount, String reason) {
        Optional<PaymentTransaction> capturedTx = paymentRepository.findByAuctionIdAndUserAndStatus(auctionId, user, "CAPTURED");
        String rzpPaymentId = capturedTx.map(PaymentTransaction::getRazorpayPaymentId).orElse(null);

        Map<String, Object> rzpRefund = razorpayService.processRefund(rzpPaymentId, amount, reason);
        String refundId = (String) rzpRefund.get("refundId");

        PaymentTransaction refundTx = recordTransaction(
                user,
                auctionId,
                craftId,
                amount,
                "AUTO_REFUND",
                "RAZORPAY",
                (reason != null ? reason : "100% Automated Refund") + " (Gateway Ref: " + refundId + ")"
        );

        Refund refund = new Refund(
                capturedTx.orElse(refundTx),
                user,
                auctionId,
                rzpPaymentId,
                refundId,
                amount,
                "COMPLETED",
                reason
        );
        refundRepository.save(refund);

        return refundTx;
    }

    /**
     * Process direct non-gateway mock/local payment request.
     */
    @Transactional
    public PaymentTransaction processPayment(String identifier, PaymentRequest request) {
        User user = getUserByIdentifier(identifier);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String txnRef = "CB-TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "UPI";
        String type = request.getType() != null ? request.getType().toUpperCase() : "DIRECT_PURCHASE";

        PaymentTransaction tx = new PaymentTransaction(
                user,
                request.getAuctionId(),
                request.getCraftId(),
                request.getAmount(),
                type,
                method,
                txnRef,
                "SUCCESS",
                request.getNotes() != null ? request.getNotes() : "Payment processed successfully via " + method
        );

        return paymentRepository.save(tx);
    }

    @Transactional
    public PaymentTransaction recordTransaction(User user, Long auctionId, Long craftId, BigDecimal amount, String type, String method, String notes) {
        String txnRef = (type.contains("REFUND") ? "CB-REF-" : "CB-TXN-") + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        PaymentTransaction tx = new PaymentTransaction(
                user,
                auctionId,
                craftId,
                amount,
                type,
                method != null ? method : "UPI",
                txnRef,
                type.contains("REFUND") ? "REFUNDED" : "SUCCESS",
                notes
        );

        return paymentRepository.save(tx);
    }

    public List<PaymentTransaction> getMyTransactions(String identifier) {
        User user = getUserByIdentifier(identifier);
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Refund> getMyRefunds(String identifier) {
        User user = getUserByIdentifier(identifier);
        return refundRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<PaymentTransaction> getByTransactionRef(String txnRef, String identifier) {
        User user = getUserByIdentifier(identifier);
        Optional<PaymentTransaction> txOpt = paymentRepository.findByTransactionRef(txnRef);
        if (txOpt.isPresent() && user.getRole() != Role.ADMIN) {
            PaymentTransaction tx = txOpt.get();
            if (tx.getUser() != null && !tx.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Access denied. You do not have permission to view this transaction.");
            }
        }
        return txOpt;
    }

    public Optional<PaymentTransaction> getByTransactionRef(String txnRef) {
        return paymentRepository.findByTransactionRef(txnRef);
    }

    public Optional<PaymentTransaction> getById(Long id, String identifier) {
        User user = getUserByIdentifier(identifier);
        Optional<PaymentTransaction> txOpt = paymentRepository.findById(id);
        if (txOpt.isPresent() && user.getRole() != Role.ADMIN) {
            PaymentTransaction tx = txOpt.get();
            if (tx.getUser() != null && !tx.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Access denied. You do not have permission to view this transaction.");
            }
        }
        return txOpt;
    }

    public Optional<PaymentTransaction> getById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * Compute platform-wide payment and refund stats for Admin Dashboard.
     */
    public PaymentStatsDTO getAdminPaymentStats(String identifier) {
        User user = getUserByIdentifier(identifier);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied. Admin role required to view payment statistics.");
        }
        return getAdminPaymentStats();
    }

    public PaymentStatsDTO getAdminPaymentStats() {
        List<PaymentTransaction> allTx = paymentRepository.findAll();
        List<Refund> allRefunds = refundRepository.findAll();

        long totalTx = allTx.size();
        long successTx = allTx.stream().filter(t -> "CAPTURED".equalsIgnoreCase(t.getStatus()) || "SUCCESS".equalsIgnoreCase(t.getStatus())).count();
        long failedTx = allTx.stream().filter(t -> "FAILED".equalsIgnoreCase(t.getStatus())).count();
        long totalRef = allRefunds.size();

        BigDecimal totalVolume = allTx.stream()
                .filter(t -> "CAPTURED".equalsIgnoreCase(t.getStatus()) || "SUCCESS".equalsIgnoreCase(t.getStatus()) || "REFUNDED".equalsIgnoreCase(t.getStatus()))
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunded = allRefunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netVolume = totalVolume.subtract(totalRefunded);

        return new PaymentStatsDTO(
                totalTx,
                successTx,
                failedTx,
                totalRef,
                totalVolume,
                totalRefunded,
                netVolume.compareTo(BigDecimal.ZERO) >= 0 ? netVolume : BigDecimal.ZERO,
                "INR"
        );
    }
}
