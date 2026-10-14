package com.deliveryplatform.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType) {
}