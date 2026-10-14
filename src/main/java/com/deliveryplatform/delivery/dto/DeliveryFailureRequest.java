package com.deliveryplatform.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryFailureRequest(

        @NotBlank(message = "Failure reason is required")
        String reason,

        String notes,
        Double latitude,
        Double longitude

) {}
