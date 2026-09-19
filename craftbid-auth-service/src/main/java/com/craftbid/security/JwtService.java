package com.craftbid.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret:${craftbid.jwt.secret:${JWT_SECRET:CraftBidSecretKeyForJwtAuthentication2026VerySecurePlatformKey}}}")
    private String jwtSecret;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour

    public SecretKey getSigningKey() {
        String secret = (jwtSecret != null && !jwtSecret.isBlank())
                ? jwtSecret.trim()
                : "CraftBidSecretKeyForJwtAuthentication2026VerySecurePlatformKey";

        // Ensure key is at least 256 bits (32 bytes) for HMAC-SHA256
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            secret = String.format("%-32s", secret).replace(' ', '0');
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }
}