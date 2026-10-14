package com.deliveryplatform.pricing.dto;

public record PricingResponse(
        Double distanceKm,
        Double deliveryFee,
        Double driverPayout,
        Double platformFee) {
}