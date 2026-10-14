package com.deliveryplatform.pricing.controller;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.pricing.dto.PricingResponse;
import com.deliveryplatform.pricing.service.PricingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    private final PricingService pricingService;
    private final DeliveryRepo deliveryRepo;

    public PricingController(
            PricingService pricingService,
            DeliveryRepo deliveryRepo) {

        this.pricingService = pricingService;
        this.deliveryRepo = deliveryRepo;
    }

    @GetMapping("/delivery/{deliveryId}")
    public PricingResponse calculatePrice(
            @PathVariable Long deliveryId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        return pricingService.calculatePrice(delivery);
    }
}