package com.deliveryplatform.driver.dto;

import java.util.List;

public record DriverEarningsSummaryResponse(
        Long driverId,
        Double totalEarnings,
        Double todayEarnings,
        long totalCompletedDeliveries,
        long todayCompletedDeliveries,
        List<DriverPayoutResponse> recentPayouts
) {}
