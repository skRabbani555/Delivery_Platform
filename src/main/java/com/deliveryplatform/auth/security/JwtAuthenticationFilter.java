package com.deliveryplatform.auth.security;

import com.deliveryplatform.auth.service.JwtService;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MerchantRepository merchantRepository;

    public JwtAuthenticationFilter(JwtService jwtService, MerchantRepository merchantRepository) {
        this.jwtService = jwtService;
        this.merchantRepository = merchantRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // ── 1. Try API key authentication first (X-API-Key header)
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            tryApiKeyAuth(apiKey);
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Fall back to Bearer JWT authentication
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            if (email != null
                    && role != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(token)) {

                    List<GrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role));

                    Long userId = jwtService.extractUserId(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, userId, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // Invalid/expired JWT — leave the request unauthenticated.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void tryApiKeyAuth(String apiKey) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            Optional<Merchant> merchant = merchantRepository.findByApiKey(apiKey);
            if (merchant.isPresent()
                    && merchant.get().getStatus() != null
                    && "ACTIVE".equals(merchant.get().getStatus().name())) {

                Merchant m = merchant.get();
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_MERCHANT"));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(m.getEmail(), m.getId(), authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Invalid API key — leave unauthenticated.
            SecurityContextHolder.clearContext();
        }
    }
}