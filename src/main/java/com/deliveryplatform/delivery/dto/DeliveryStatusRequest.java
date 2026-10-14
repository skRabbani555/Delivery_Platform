package com.deliveryplatform.delivery.dto;

import com.deliveryplatform.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record DeliveryStatusRequest(

        @NotNull DeliveryStatus status,

        String actorType,

        Long actorId

) {
}