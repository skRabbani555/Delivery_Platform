package com.deliveryplatform.merchant.dto;

/**
 * Merchant registration response – returned once at creation and on rotateApiKey.
 * The apiKey and webhookSecret are shown only in these responses; they are never exposed again.
 */
public record MerchantResponse(
        Long id,
        String businessName,
        String email,
        String phone,
        String status,
        String apiKey,
        String webhookSecret,
        String webhookUrl) {
}