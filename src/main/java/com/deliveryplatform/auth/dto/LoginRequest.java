package com.deliveryplatform.auth.dto;

public record LoginRequest(
        String email,
        String password) {
}