package com.budgettracker.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_TOKEN_VERSION = "rtv";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), expirationMs, -1);
    }

    /** Generates a refresh token embedding the user's current refreshTokenVersion. */
    public String generateRefreshToken(UserDetails userDetails, int tokenVersion) {
        return buildToken(userDetails.getUsername(), refreshExpirationMs, tokenVersion);
    }

    /** @deprecated Use generateRefreshToken(UserDetails, int) */
    @Deprecated
    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails, 0);
    }

    private String buildToken(String subject, long ttl, int tokenVersion) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(key);
        if (tokenVersion >= 0) {
            builder.claim(CLAIM_TOKEN_VERSION, tokenVersion);
        }
        return builder.compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a refresh token: checks signature, expiry, email, and that the
     * embedded version matches the user's current refreshTokenVersion.
     * Returns false (rather than throwing) so callers can return a clean 401.
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails, int currentVersion) {
        try {
            Claims claims = parseClaims(token);
            String email = claims.getSubject();
            if (!email.equals(userDetails.getUsername())) return false;
            if (isTokenExpired(token)) return false;

            Integer tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
            // Tokens issued before versioning was introduced have no claim — treat as version 0
            int version = tokenVersion == null ? 0 : tokenVersion;
            return version == currentVersion;
        } catch (JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}