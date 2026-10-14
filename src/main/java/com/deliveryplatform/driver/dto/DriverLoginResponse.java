package com.deliveryplatform.driver.dto;

public record DriverLoginResponse(
        String accessToken,
        String tokenType) {
}