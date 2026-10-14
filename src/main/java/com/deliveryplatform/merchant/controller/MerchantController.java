package com.deliveryplatform.merchant.controller;

import com.deliveryplatform.merchant.dto.MerchantRequest;
import com.deliveryplatform.merchant.dto.MerchantResponse;
import com.deliveryplatform.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * Register a new merchant.
     * Returns the generated API key and webhook secret (only shown once).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse createMerchant(
            @Valid @RequestBody MerchantRequest request) {
        return merchantService.createMerchant(request);
    }

    /** Get current merchant profile. */
    @GetMapping("/me")
    public MerchantResponse getMe(Authentication authentication) {
        Long merchantId = (Long) authentication.getCredentials();
        return merchantService.getMerchant(merchantId);
    }

    /**
     * Rotate the API key – invalidates the old one and returns a new one.
     * Treat it like a password; store it securely.
     */
    @PostMapping("/me/rotate-api-key")
    public MerchantResponse rotateApiKey(Authentication authentication) {
        Long merchantId = (Long) authentication.getCredentials();
        return merchantService.rotateApiKey(merchantId);
    }

    /**
     * Update webhook URL.
     */
    @PatchMapping("/me/webhook")
    public MerchantResponse updateWebhook(
            @RequestParam String webhookUrl,
            Authentication authentication) {
        Long merchantId = (Long) authentication.getCredentials();
        return merchantService.updateWebhook(merchantId, webhookUrl);
    }
}