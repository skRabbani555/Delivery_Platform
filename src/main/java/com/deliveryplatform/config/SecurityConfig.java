package com.deliveryplatform.config;

import com.deliveryplatform.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints (no auth required)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/driver/auth/**",
                                "/api/v1/merchants",          // merchant registration
                                "/api/v1/drivers",            // driver registration
                                "/ws/**")
                        .permitAll()

                        // ── Driver-only endpoints
                        .requestMatchers(
                                "/api/v1/drivers/me/**",
                                "/api/v1/drivers/*/location",
                                "/api/v1/drivers/*/status")
                        .hasAuthority("ROLE_DRIVER")

                        // ── Merchant-only endpoints
                        .requestMatchers(
                                "/api/v1/merchants/me",
                                "/api/v1/merchants/me/**",
                                "/api/v1/deliveries/**",
                                "/api/v1/webhook-logs/**")
                        .hasAuthority("ROLE_MERCHANT")

                        // ── Everything else requires authentication
                        .anyRequest().authenticated())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}