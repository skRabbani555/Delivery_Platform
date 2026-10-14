package com.deliveryplatform.driver.dto;

import com.deliveryplatform.driver.entity.PayoutStatus;

import java.time.LocalDateTime;

public record DriverPayoutResponse(
        Long id,
        Long driverId,
        Long deliveryId,
        Double amount,
        Double deliveryFee,
        Double platformFee,
        PayoutStatus status,
        LocalDateTime earnedAt,
        LocalDateTime paidAt,
        String referenceNumber
) {}
