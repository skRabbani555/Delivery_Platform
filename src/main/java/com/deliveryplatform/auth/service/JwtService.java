package com.deliveryplatform.auth.service;

import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final MerchantRepository merchantRepository;
    private final SecretKey key;
    private final long expiration;

    public JwtService(
            MerchantRepository merchantRepository,
            @Value("${jwt.secret:delivery-platform-super-secret-key-change-this-in-production-123456789}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        this.merchantRepository = merchantRepository;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String email) {
        Long merchantId = null;
        if (merchantRepository != null) {
            Optional<Merchant> merchant = merchantRepository.findByEmail(email);
            if (merchant.isPresent()) {
                merchantId = merchant.get().getId();
            }
        }
        return generateToken(email, "MERCHANT", merchantId);
    }

    public String generateToken(String email, String role, Long userId) {
        var builder = Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .claim("id", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key);

        return builder.compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "MERCHANT";
    }

    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("id");
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
