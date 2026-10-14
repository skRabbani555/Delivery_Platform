package com.deliveryplatform.merchant.service;

import com.deliveryplatform.merchant.dto.MerchantRequest;
import com.deliveryplatform.merchant.dto.MerchantResponse;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public MerchantService(
            MerchantRepository merchantRepository,
            PasswordEncoder passwordEncoder) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MerchantResponse createMerchant(MerchantRequest request) {

        if (merchantRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Merchant with this email already exists");
        }

        Merchant merchant = Merchant.builder()
                .businessName(request.businessName())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .webhookUrl(request.webhookUrl())
                .apiKey(generateApiKey())
                .webhookSecret(generateWebhookSecret())
                .build();

        Merchant savedMerchant = merchantRepository.save(merchant);

        return toResponse(savedMerchant);
    }

    @Transactional
    public MerchantResponse rotateApiKey(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        merchant.setApiKey(generateApiKey());
        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    @Transactional
    public MerchantResponse updateWebhook(Long merchantId, String webhookUrl) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        merchant.setWebhookUrl(webhookUrl);
        return toResponse(merchantRepository.save(merchant));
    }

    public MerchantResponse getMerchant(Long merchantId) {
        return toResponse(merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "dp_live_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getBusinessName(),
                merchant.getEmail(),
                merchant.getPhone(),
                merchant.getStatus().name(),
                merchant.getApiKey(),
                merchant.getWebhookSecret(),
                merchant.getWebhookUrl());
    }
}