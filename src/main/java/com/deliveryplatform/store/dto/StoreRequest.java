package com.deliveryplatform.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreRequest(

        @NotBlank String name,

        @NotBlank String phone,

        @NotBlank String address,

        @NotNull Double latitude,

        @NotNull Double longitude

) {
}