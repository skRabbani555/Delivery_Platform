package com.deliveryplatform.tracking.dto;

public record DriverLocationMessage(
        Long driverId,
        Double latitude,
        Double longitude,
        String timestamp) {
}