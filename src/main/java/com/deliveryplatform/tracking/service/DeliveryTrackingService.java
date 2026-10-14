package com.deliveryplatform.tracking.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.tracking.dto.DeliveryTrackingResponse;
import com.deliveryplatform.tracking.dto.DriverLocationMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DeliveryTrackingService {

    private final DeliveryRepo deliveryRepo;
    private final StringRedisTemplate redisTemplate;

    public DeliveryTrackingService(
            DeliveryRepo deliveryRepo,
            StringRedisTemplate redisTemplate) {

        this.deliveryRepo = deliveryRepo;
        this.redisTemplate = redisTemplate;
    }

    public DeliveryTrackingResponse getTracking(Long deliveryId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException(
                        "Delivery not found: " + deliveryId));

        DriverLocationMessage location = null;

        if (delivery.getDriver() != null) {

            String key = "driver:" + delivery.getDriver().getId() + ":location";

            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (!data.isEmpty()) {
                location = new DriverLocationMessage(
                        delivery.getDriver().getId(),
                        Double.valueOf(data.get("latitude").toString()),
                        Double.valueOf(data.get("longitude").toString()),
                        data.get("timestamp").toString());
            }
        }

        return new DeliveryTrackingResponse(
                delivery.getId(),
                delivery.getStatus().name(),
                delivery.getDriver() != null
                        ? delivery.getDriver().getId()
                        : null,
                location);
    }
}