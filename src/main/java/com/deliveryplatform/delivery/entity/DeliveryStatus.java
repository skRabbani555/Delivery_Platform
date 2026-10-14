package com.deliveryplatform.delivery.entity;

public enum DeliveryStatus {
    CREATED,
    SEARCHING_DRIVER,
    DRIVER_ASSIGNED,
    DRIVER_GOING_TO_PICKUP,
    ARRIVED_AT_PICKUP,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    ARRIVED_AT_DROP,
    DELIVERED,
    CANCELLED,
    FAILED
}