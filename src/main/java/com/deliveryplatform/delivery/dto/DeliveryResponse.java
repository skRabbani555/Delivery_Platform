package com.deliveryplatform.delivery.dto;

import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.entity.DeliveryType;

import java.time.LocalDateTime;

public record DeliveryResponse(
        Long id,
        Long merchantId,
        Long storeId,
        Long driverId,
        String externalOrderId,
        String pickupAddress,
        String dropAddress,
        String customerName,
        String customerPhone,
        Double packageWeight,
        DeliveryType deliveryType,
        DeliveryStatus status,
        Double deliveryFee,
        Double driverPayout,
        Double platformFee,
        Double estimatedEtaMinutes,
        LocalDateTime assignedAt,
        String deliveryOtp,
        Boolean otpVerified,
        String proofOfDeliveryNotes,
        String proofOfDeliveryImageUrl,
        String recipientName,
        LocalDateTime actualPickupTime,
        LocalDateTime actualDeliveryTime,
        String failureReason) {

    public DeliveryResponse(
            Long id,
            Long merchantId,
            Long storeId,
            Long driverId,
            String externalOrderId,
            String pickupAddress,
            String dropAddress,
            String customerName,
            String customerPhone,
            Double packageWeight,
            DeliveryType deliveryType,
            DeliveryStatus status,
            Double deliveryFee,
            Double driverPayout,
            Double platformFee,
            Double estimatedEtaMinutes,
            LocalDateTime assignedAt) {
        this(id, merchantId, storeId, driverId, externalOrderId, pickupAddress, dropAddress,
                customerName, customerPhone, packageWeight, deliveryType, status,
                deliveryFee, driverPayout, platformFee, estimatedEtaMinutes, assignedAt,
                null, null, null, null, null, null, null, null);
    }

    public DeliveryResponse(
            Long id,
            Long merchantId,
            Long storeId,
            Long driverId,
            String externalOrderId,
            String pickupAddress,
            String dropAddress,
            String customerName,
            String customerPhone,
            Double packageWeight,
            DeliveryType deliveryType,
            DeliveryStatus status,
            Double deliveryFee,
            Double driverPayout,
            Double platformFee) {
        this(id, merchantId, storeId, driverId, externalOrderId, pickupAddress, dropAddress,
                customerName, customerPhone, packageWeight, deliveryType, status,
                deliveryFee, driverPayout, platformFee, null, null,
                null, null, null, null, null, null, null, null);
    }
}