package com.deliveryplatform.tracking.controller;

import com.deliveryplatform.tracking.dto.DeliveryTrackingResponse;
import com.deliveryplatform.tracking.service.DeliveryTrackingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryTrackingController {

    private final DeliveryTrackingService trackingService;

    public DeliveryTrackingController(
            DeliveryTrackingService trackingService) {

        this.trackingService = trackingService;
    }

    @GetMapping("/{deliveryId}/tracking")
    public DeliveryTrackingResponse getTracking(
            @PathVariable Long deliveryId) {

        return trackingService.getTracking(deliveryId);
    }
}