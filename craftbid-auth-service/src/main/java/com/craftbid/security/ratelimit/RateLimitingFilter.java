package com.craftbid.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // If rate limiting is globally disabled, continue
        if (!rateLimiterService.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = extractClientIp(request);
        HttpServletRequest requestToPass = request;

        // =========================================================
        // 1. TIER 1: AUTHENTICATION ROUTES (STRICTEST + EXPONENTIAL BACKOFF)
        // =========================================================
        if (isAuthRoute(path, request)) {
            String accountIdentifier = null;

            // Cache request body if POST/PUT with JSON payload
            if (hasJsonBody(request)) {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
                requestToPass = cachedRequest;
                accountIdentifier = extractAccountIdentifierFromBody(cachedRequest.getBodyAsString());
            }

            // Fallback: check query parameters (e.g. ?email=... or ?to=...)
            if (accountIdentifier == null || accountIdentifier.isBlank()) {
                accountIdentifier = extractAccountIdentifierFromParams(request);
            }

            RateLimiterService.RateLimitResult result =
                    rateLimiterService.checkAuthRateLimit(clientIp, accountIdentifier);

            if (!result.isAllowed()) {
                sendRateLimitError(response, result);
                return;
            }

            filterChain.doFilter(requestToPass, response);
            return;
        }

        // =========================================================
        // 2. TIER 3: AUTHENTICATED USER ACTIONS (LOOSEST / HIGH THROUGHPUT)
        // =========================================================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            String userIdentifier = auth.getName();
            RateLimiterService.RateLimitResult result =
                    rateLimiterService.checkAuthenticatedUserRateLimit(userIdentifier);

            if (!result.isAllowed()) {
                sendRateLimitError(response, result);
                return;
            }

            filterChain.doFilter(requestToPass, response);
            return;
        }

        // =========================================================
        // 3. TIER 2: PUBLIC BROWSING ROUTES (MODERATE PER-IP)
        // =========================================================
        RateLimiterService.RateLimitResult result =
                rateLimiterService.checkPublicRateLimit(clientIp);

        if (!result.isAllowed()) {
            sendRateLimitError(response, result);
            return;
        }

        filterChain.doFilter(requestToPass, response);
    }

    private boolean isAuthRoute(String path, HttpServletRequest request) {
        if (path == null) return false;
        // /api/auth/enable-seller requires an authenticated user, not treated as public auth endpoint
        if (path.contains("/api/auth/enable-seller")) {
            return false;
        }
        return path.startsWith("/api/auth/") || path.equals("/api/auth");
    }

    private boolean hasJsonBody(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    private String extractAccountIdentifierFromBody(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("email")) {
                return node.get("email").asText();
            }
            if (node.hasNonNull("identifier")) {
                return node.get("identifier").asText();
            }
            if (node.hasNonNull("phone")) {
                return node.get("phone").asText();
            }
            if (node.hasNonNull("username")) {
                return node.get("username").asText();
            }
        } catch (Exception ignored) {
            // Unparseable JSON, ignore body account extraction
        }
        return null;
    }

    private String extractAccountIdentifierFromParams(HttpServletRequest request) {
        String email = request.getParameter("email");
        if (email != null && !email.isBlank()) return email;
        String to = request.getParameter("to");
        if (to != null && !to.isBlank()) return to;
        String identifier = request.getParameter("identifier");
        if (identifier != null && !identifier.isBlank()) return identifier;
        String phone = request.getParameter("phone");
        if (phone != null && !phone.isBlank()) return phone;
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {
                "CF-Connecting-IP",
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip.trim())) {
                // If X-Forwarded-For contains multiple IPs, the first one is the client IP
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void sendRateLimitError(
            HttpServletResponse response,
            RateLimiterService.RateLimitResult result) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));

        Map<String, Object> errorPayload = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", result.getMessage(),
                "retryAfterSeconds", result.getRetryAfterSeconds(),
                "limitType", result.getLimitType() != null ? result.getLimitType() : "RATE_LIMIT_EXCEEDED",
                "timestamp", Instant.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorPayload));
    }
}
