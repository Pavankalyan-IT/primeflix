package com.netflixclone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Component
public class JwtUtil {

    private static final long EXPIRATION_TIME = 1000L * 60 * 60;
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");
    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String configuredSecret) {
        if (configuredSecret == null
                || configuredSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_TIME))
                .signWith(signingKey)
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            Object role = claims.get("role");
            Object tokenVersion = claims.get("tokenVersion");
            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && role instanceof String roleName
                    && ALLOWED_ROLES.contains(roleName)
                    && tokenVersion instanceof Number version
                    && version.intValue() >= 0
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public int extractTokenVersion(String token) {
        return extractClaims(token).get("tokenVersion", Integer.class);
    }
}
