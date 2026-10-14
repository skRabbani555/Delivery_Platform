package com.deliveryplatform.driver.dto;

import com.deliveryplatform.driver.entity.DriverStatus;

public record DriverResponse(
        Long id,
        String name,
        String email,
        String phone,
        DriverStatus status,
        Double latitude,
        Double longitude,
        Boolean active) {
}