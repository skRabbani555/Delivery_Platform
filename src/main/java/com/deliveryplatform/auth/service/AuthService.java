package com.deliveryplatform.auth.service;

import com.deliveryplatform.auth.dto.LoginRequest;
import com.deliveryplatform.auth.dto.LoginResponse;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        Merchant merchant = merchantRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(),
                merchant.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(merchant.getEmail());

        return new LoginResponse(token, "Bearer");
    }
}