package com.craftbid.security.ratelimit;

import com.craftbid.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimitProperties properties;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);

        // Configure predictable test thresholds
        RateLimitProperties.Auth auth = properties.getAuth();
        auth.setIpLimit(3);
        auth.setIpWindowSeconds(60);
        auth.setAccountLimit(2);
        auth.setAccountWindowSeconds(60);
        auth.setBaseBackoffSeconds(10);
        auth.setMaxBackoffSeconds(100);
        auth.setBackoffResetSeconds(600);

        RateLimitProperties.PublicConfig pub = properties.getPublic();
        pub.setIpLimit(5);
        pub.setIpWindowSeconds(60);

        RateLimitProperties.Authenticated authenticated = properties.getAuthenticated();
        authenticated.setUserLimit(10);
        authenticated.setUserWindowSeconds(60);

        rateLimiterService = new RateLimiterService(properties);
    }

    @Test
    void testAuthRateLimit_PerIp_EnforcedWithExponentialBackoff() {
        String ip = "192.168.1.100";

        // First 3 requests should be allowed
        for (int i = 0; i < 3; i++) {
            RateLimiterService.RateLimitResult result = rateLimiterService.checkAuthRateLimit(ip, null);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }

        // 4th request exceeds limit -> Level 1 exponential backoff (10s)
        RateLimiterService.RateLimitResult violation1 = rateLimiterService.checkAuthRateLimit(ip, null);
        assertFalse(violation1.isAllowed(), "4th request should be denied");
        assertEquals("AUTH_PER_IP", violation1.getLimitType());
        assertEquals(10, violation1.getRetryAfterSeconds());
        assertTrue(violation1.getMessage().contains("Exponential Backoff Level 1"));

        // Subsequent immediate request during penalty is still denied
        RateLimiterService.RateLimitResult stillInPenalty = rateLimiterService.checkAuthRateLimit(ip, null);
        assertFalse(stillInPenalty.isAllowed());
        assertTrue(stillInPenalty.getRetryAfterSeconds() > 0);
    }

    @Test
    void testAuthRateLimit_PerAccount_ProtectsAgainstDistributedAttack() {
        String targetAccount = "victim@craftbid.com";

        // Attacker from IP 1
        RateLimiterService.RateLimitResult r1 = rateLimiterService.checkAuthRateLimit("10.0.0.1", targetAccount);
        assertTrue(r1.isAllowed());

        // Attacker from IP 2 targeting the same account
        RateLimiterService.RateLimitResult r2 = rateLimiterService.checkAuthRateLimit("10.0.0.2", targetAccount);
        assertTrue(r2.isAllowed());

        // Attacker from IP 3 targeting the same account -> breaches account limit of 2
        RateLimiterService.RateLimitResult r3 = rateLimiterService.checkAuthRateLimit("10.0.0.3", targetAccount);
        assertFalse(r3.isAllowed(), "Should be blocked by per-account rate limit despite different IP");
        assertEquals("AUTH_PER_ACCOUNT", r3.getLimitType());
        assertTrue(r3.getMessage().contains(targetAccount));
    }

    @Test
    void testPublicRateLimit_PerIp() {
        String ip = "198.51.100.1";

        // First 5 requests allowed
        for (int i = 0; i < 5; i++) {
            RateLimiterService.RateLimitResult result = rateLimiterService.checkPublicRateLimit(ip);
            assertTrue(result.isAllowed(), "Public request " + (i + 1) + " should be allowed");
        }

        // 6th request denied
        RateLimiterService.RateLimitResult result = rateLimiterService.checkPublicRateLimit(ip);
        assertFalse(result.isAllowed(), "6th public request should be denied");
        assertEquals("PUBLIC_PER_IP", result.getLimitType());
        assertTrue(result.getRetryAfterSeconds() > 0);
    }

    @Test
    void testAuthenticatedUserRateLimit() {
        String user = "artisan_rahul";

        // First 10 requests allowed
        for (int i = 0; i < 10; i++) {
            RateLimiterService.RateLimitResult result = rateLimiterService.checkAuthenticatedUserRateLimit(user);
            assertTrue(result.isAllowed(), "Authenticated request " + (i + 1) + " should be allowed");
        }

        // 11th request denied
        RateLimiterService.RateLimitResult result = rateLimiterService.checkAuthenticatedUserRateLimit(user);
        assertFalse(result.isAllowed(), "11th authenticated request should be denied");
        assertEquals("AUTHENTICATED_USER", result.getLimitType());
        assertTrue(result.getRetryAfterSeconds() > 0);
    }

    @Test
    void testRateLimitDisabled() {
        properties.setEnabled(false);
        String ip = "192.168.1.50";

        for (int i = 0; i < 20; i++) {
            RateLimiterService.RateLimitResult authRes = rateLimiterService.checkAuthRateLimit(ip, "user@example.com");
            RateLimiterService.RateLimitResult pubRes = rateLimiterService.checkPublicRateLimit(ip);
            RateLimiterService.RateLimitResult userRes = rateLimiterService.checkAuthenticatedUserRateLimit("user123");

            assertTrue(authRes.isAllowed());
            assertTrue(pubRes.isAllowed());
            assertTrue(userRes.isAllowed());
        }
    }
}
