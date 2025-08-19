package com.ecommerce.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // Prefer environment variable JWT_SECRET, otherwise fallback to jwt.secret from properties
    @Value("${JWT_SECRET:${jwt.secret:Ue7d9Kj4QpLmZ8sR2xvTgH5yVbNc1fR0wYz3AaPqS6tHjK9L}}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}") // por defecto 1 día
    private long expirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        try {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } catch (io.jsonwebtoken.security.WeakKeyException wke) {
            org.slf4j.LoggerFactory.getLogger(JwtUtil.class).warn("Configured JWT secret is too weak, generating a secure random key for this run. Set a longer JWT_SECRET to avoid this.");
            this.secretKey = io.jsonwebtoken.security.Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}
