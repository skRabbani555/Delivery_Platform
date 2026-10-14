package com.deliveryplatform.tracking.dto;

public record DeliveryTrackingResponse(
        Long deliveryId,
        String status,
        Long driverId,
        DriverLocationMessage driverLocation) {
}