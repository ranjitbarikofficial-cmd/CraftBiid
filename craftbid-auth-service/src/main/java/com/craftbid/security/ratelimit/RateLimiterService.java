package com.craftbid.security.ratelimit;

import com.craftbid.config.RateLimitProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private final RateLimitProperties properties;

    // Sliding window request logs: Key -> Queue of timestamp millis
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestWindows = new ConcurrentHashMap<>();

    // Exponential backoff state for auth routes: Key -> BackoffState
    private final ConcurrentHashMap<String, BackoffState> authBackoffStates = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    // =========================================================
    // TIER 1: AUTH ROUTES RATE LIMITING (DUAL-KEY + EXPONENTIAL BACKOFF)
    // =========================================================
    public RateLimitResult checkAuthRateLimit(String clientIp, String accountIdentifier) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed();
        }

        RateLimitProperties.Auth authConfig = properties.getAuth();

        // 1. Check IP auth rate limit with exponential backoff
        String ipKey = "AUTH_IP:" + (clientIp != null ? clientIp.trim() : "unknown");
        RateLimitResult ipResult = evaluateAuthKey(
                ipKey,
                authConfig.getIpLimit(),
                authConfig.getIpWindowSeconds(),
                authConfig.getBaseBackoffSeconds(),
                authConfig.getMaxBackoffSeconds(),
                authConfig.getBackoffResetSeconds(),
                "AUTH_PER_IP",
                "Too many authentication attempts from this IP address."
        );

        if (!ipResult.isAllowed()) {
            return ipResult;
        }

        // 2. If an account identifier (email/phone/username) is present, check per-account limit
        if (accountIdentifier != null && !accountIdentifier.isBlank()) {
            String accKey = "AUTH_ACC:" + accountIdentifier.trim().toLowerCase();
            RateLimitResult accResult = evaluateAuthKey(
                    accKey,
                    authConfig.getAccountLimit(),
                    authConfig.getAccountWindowSeconds(),
                    authConfig.getBaseBackoffSeconds(),
                    authConfig.getMaxBackoffSeconds(),
                    authConfig.getBackoffResetSeconds(),
                    "AUTH_PER_ACCOUNT",
                    "Too many authentication attempts for this account (" + accountIdentifier + ")."
            );

            if (!accResult.isAllowed()) {
                return accResult;
            }
        }

        return RateLimitResult.allowed();
    }

    private RateLimitResult evaluateAuthKey(
            String key,
            int limit,
            int windowSeconds,
            int baseBackoffSeconds,
            int maxBackoffSeconds,
            int backoffResetSeconds,
            String limitType,
            String baseMessage) {

        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        long resetMillis = backoffResetSeconds * 1000L;

        BackoffState backoff = authBackoffStates.computeIfAbsent(key, k -> new BackoffState());

        synchronized (backoff) {
            // Check if currently under penalty
            if (backoff.penaltyUntilTime > now) {
                long remainingSeconds = Math.max(1, (backoff.penaltyUntilTime - now + 999) / 1000);
                return RateLimitResult.denied(
                        remainingSeconds,
                        limitType,
                        baseMessage + " Please wait " + remainingSeconds + "s before retrying (Exponential Backoff Level " + backoff.violationCount.get() + ")."
                );
            }

            // Check if violation counter should reset due to sustained good behavior
            if (backoff.lastViolationTime > 0 && (now - backoff.lastViolationTime) > resetMillis) {
                backoff.violationCount.set(0);
            }

            // Evaluate sliding window
            ConcurrentLinkedQueue<Long> window = requestWindows.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
            pruneWindow(window, now - windowMillis);

            if (window.size() >= limit) {
                // Trigger next level of exponential backoff
                int currentViolations = backoff.violationCount.incrementAndGet();
                long multiplier = 1L << Math.min(20, currentViolations - 1); // 2^(v-1)
                long penaltySeconds = Math.min((long) maxBackoffSeconds, (long) baseBackoffSeconds * multiplier);

                backoff.lastViolationTime = now;
                backoff.penaltyUntilTime = now + (penaltySeconds * 1000L);

                return RateLimitResult.denied(
                        penaltySeconds,
                        limitType,
                        baseMessage + " Rate limit exceeded. Please wait " + penaltySeconds + "s before retrying (Exponential Backoff Level " + currentViolations + ")."
                );
            }

            // Request is allowed: record timestamp
            window.add(now);
            return RateLimitResult.allowed();
        }
    }

    // =========================================================
    // TIER 2: PUBLIC ENDPOINTS RATE LIMITING (PER-IP SLIDING WINDOW)
    // =========================================================
    public RateLimitResult checkPublicRateLimit(String clientIp) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed();
        }

        RateLimitProperties.PublicConfig pubConfig = properties.getPublic();
        String key = "PUB_IP:" + (clientIp != null ? clientIp.trim() : "unknown");

        return evaluateSlidingWindow(
                key,
                pubConfig.getIpLimit(),
                pubConfig.getIpWindowSeconds(),
                "PUBLIC_PER_IP",
                "Public endpoint request limit exceeded."
        );
    }

    // =========================================================
    // TIER 3: AUTHENTICATED USER ACTIONS (PER-USER SLIDING WINDOW)
    // =========================================================
    public RateLimitResult checkAuthenticatedUserRateLimit(String userIdentifier) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed();
        }

        RateLimitProperties.Authenticated authConfig = properties.getAuthenticated();
        String key = "AUTH_USER:" + (userIdentifier != null ? userIdentifier.trim() : "anonymous");

        return evaluateSlidingWindow(
                key,
                authConfig.getUserLimit(),
                authConfig.getUserWindowSeconds(),
                "AUTHENTICATED_USER",
                "User action request limit exceeded."
        );
    }

    private RateLimitResult evaluateSlidingWindow(
            String key,
            int limit,
            int windowSeconds,
            String limitType,
            String baseMessage) {

        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;

        ConcurrentLinkedQueue<Long> window = requestWindows.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        synchronized (window) {
            pruneWindow(window, now - windowMillis);

            if (window.size() >= limit) {
                Long oldest = window.peek();
                long retryAfter = oldest != null
                        ? Math.max(1, (windowMillis - (now - oldest) + 999) / 1000)
                        : (long) windowSeconds;

                return RateLimitResult.denied(
                        retryAfter,
                        limitType,
                        baseMessage + " Please wait " + retryAfter + "s before retrying."
                );
            }

            window.add(now);
            return RateLimitResult.allowed();
        }
    }

    private void pruneWindow(ConcurrentLinkedQueue<Long> window, long cutoffTime) {
        while (!window.isEmpty()) {
            Long head = window.peek();
            if (head != null && head < cutoffTime) {
                window.poll();
            } else {
                break;
            }
        }
    }

    // =========================================================
    // PERIODIC CLEANUP (EVICT EXPIRED BUCKETS EVERY 5 MINUTES)
    // =========================================================
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        long maxRetention = 1800000L; // 30 minutes

        // Prune request windows
        requestWindows.entrySet().removeIf(entry -> {
            ConcurrentLinkedQueue<Long> queue = entry.getValue();
            pruneWindow(queue, now - maxRetention);
            return queue.isEmpty();
        });

        // Prune backoff states that have fully recovered
        authBackoffStates.entrySet().removeIf(entry -> {
            BackoffState state = entry.getValue();
            return state.penaltyUntilTime < now && (now - state.lastViolationTime) > maxRetention;
        });
    }

    // Reset helper for unit testing
    public void resetAll() {
        requestWindows.clear();
        authBackoffStates.clear();
    }

    // =========================================================
    // INNER CLASSES & DATA STRUCTURES
    // =========================================================
    private static class BackoffState {
        final AtomicInteger violationCount = new AtomicInteger(0);
        volatile long lastViolationTime = 0;
        volatile long penaltyUntilTime = 0;
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final long retryAfterSeconds;
        private final String limitType;
        private final String message;

        private RateLimitResult(boolean allowed, long retryAfterSeconds, String limitType, String message) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
            this.limitType = limitType;
            this.message = message;
        }

        public static RateLimitResult allowed() {
            return new RateLimitResult(true, 0, null, null);
        }

        public static RateLimitResult denied(long retryAfterSeconds, String limitType, String message) {
            return new RateLimitResult(false, retryAfterSeconds, limitType, message);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public String getLimitType() {
            return limitType;
        }

        public String getMessage() {
            return message;
        }
    }
}
