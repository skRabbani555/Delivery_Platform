package com.deliveryplatform.delivery.controller;

import com.deliveryplatform.delivery.dto.DeliveryRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.service.DeliveryService;
import com.deliveryplatform.merchant.service.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final IdempotencyService idempotencyService;

    public DeliveryController(
            DeliveryService deliveryService,
            IdempotencyService idempotencyService) {
        this.deliveryService = deliveryService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Create a new delivery.
     *
     * <p><b>Idempotency:</b> Supply a unique {@code Idempotency-Key} header on every request.
     * If the exact same key is re-submitted (e.g. after a network timeout), the original
     * response is returned safely without creating a duplicate order.
     *
     * <p>Authenticate either via {@code Authorization: Bearer <jwt>} or {@code X-API-Key: dp_live_...}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse createDelivery(
            @Valid @RequestBody DeliveryRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyService.executeIdempotently(
                    merchantId,
                    idempotencyKey,
                    DeliveryResponse.class,
                    () -> deliveryService.createDelivery(merchantId, request));
        }

        return deliveryService.createDelivery(merchantId, request);
    }

    /** Get delivery by ID. */
    @GetMapping("/{deliveryId}")
    public DeliveryResponse getDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();
        DeliveryResponse response = deliveryService.getDelivery(deliveryId);

        // Ensure merchant can only see their own deliveries
        if (!response.merchantId().equals(merchantId)) {
            throw new RuntimeException("Delivery not found");
        }

        return response;
    }

    /** Manually trigger dispatch for a delivery stuck in SEARCHING_DRIVER. */
    @PostMapping("/{deliveryId}/dispatch")
    public DeliveryResponse dispatchDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();

        // Ownership check
        DeliveryResponse existing = deliveryService.getDelivery(deliveryId);
        if (!existing.merchantId().equals(merchantId)) {
            throw new RuntimeException("Delivery not found");
        }

        return deliveryService.dispatchDelivery(deliveryId);
    }

    /** Update delivery status. */
    @PatchMapping("/{deliveryId}/status")
    public DeliveryResponse updateStatus(
            @PathVariable Long deliveryId,
            @RequestParam DeliveryStatus status,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();

        return deliveryService.updateStatus(
                deliveryId,
                status,
                "MERCHANT",
                merchantId);
    }

    /** Cancel delivery. */
    @PostMapping("/{deliveryId}/cancel")
    public DeliveryResponse cancelDelivery(
            @PathVariable Long deliveryId,
            Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();

        return deliveryService.cancelDelivery(
                deliveryId,
                "MERCHANT",
                merchantId);
    }

    /** List all deliveries for the authenticated merchant. */
    @GetMapping
    public List<DeliveryResponse> getMyDeliveries(Authentication authentication) {

        Long merchantId = (Long) authentication.getCredentials();
        return deliveryService.getMerchantDeliveries(merchantId);
    }
}