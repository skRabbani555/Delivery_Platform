package com.deliveryplatform.delivery.dto;

import com.deliveryplatform.delivery.entity.DeliveryType;
import jakarta.validation.constraints.*;

public record DeliveryRequest(

        @NotNull(message = "Store ID is required")
        Long storeId,

        @NotBlank(message = "External order ID is required")
        @Size(max = 100, message = "External order ID must not exceed 100 characters")
        String externalOrderId,

        @NotBlank(message = "Pickup address is required")
        @Size(max = 500, message = "Pickup address must not exceed 500 characters")
        String pickupAddress,

        @NotNull(message = "Pickup latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double pickupLatitude,

        @NotNull(message = "Pickup longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double pickupLongitude,

        @NotBlank(message = "Drop address is required")
        @Size(max = 500, message = "Drop address must not exceed 500 characters")
        String dropAddress,

        @NotNull(message = "Drop latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double dropLatitude,

        @NotNull(message = "Drop longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double dropLongitude,

        @NotBlank(message = "Customer name is required")
        @Size(max = 100, message = "Customer name must not exceed 100 characters")
        String customerName,

        @NotBlank(message = "Customer phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number format")
        String customerPhone,

        @DecimalMin(value = "0.0", inclusive = false, message = "Package weight must be positive")
        @DecimalMax(value = "50.0", message = "Package weight must not exceed 50 kg")
        Double packageWeight,

        @NotNull(message = "Delivery type is required")
        DeliveryType deliveryType) {
}