package com.deliveryplatform.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryCompletionRequest(

        @NotBlank(message = "Delivery OTP is required for completion verification")
        String otp,

        String recipientName,
        String notes,
        String imageUrl,
        Double dropLatitude,
        Double dropLongitude

) {}
