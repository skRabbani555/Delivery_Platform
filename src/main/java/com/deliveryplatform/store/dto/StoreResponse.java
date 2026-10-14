package com.deliveryplatform.store.dto;

public record StoreResponse(
        Long id,
        String name,
        String phone,
        String address,
        Double latitude,
        Double longitude,
        Boolean active) {
}