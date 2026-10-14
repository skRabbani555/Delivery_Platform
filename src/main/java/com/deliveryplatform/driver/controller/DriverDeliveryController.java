package com.deliveryplatform.driver.controller;

import com.deliveryplatform.delivery.dto.DeliveryCompletionRequest;
import com.deliveryplatform.delivery.dto.DeliveryFailureRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.driver.dto.DriverEarningsSummaryResponse;
import com.deliveryplatform.driver.dto.DriverPayoutResponse;
import com.deliveryplatform.driver.dto.DriverResponse;
import com.deliveryplatform.driver.dto.UpdateLocationRequest;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.service.DriverDeliveryService;
import com.deliveryplatform.driver.service.DriverLocationService;
import com.deliveryplatform.driver.service.DriverPayoutService;
import com.deliveryplatform.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers/me")
public class DriverDeliveryController {

    private final DriverDeliveryService driverDeliveryService;
    private final DriverPayoutService driverPayoutService;
    private final DriverService driverService;
    private final DriverLocationService driverLocationService;

    public DriverDeliveryController(
            DriverDeliveryService driverDeliveryService,
            DriverPayoutService driverPayoutService,
            DriverService driverService,
            DriverLocationService driverLocationService) {
        this.driverDeliveryService = driverDeliveryService;
        this.driverPayoutService = driverPayoutService;
        this.driverService = driverService;
        this.driverLocationService = driverLocationService;
    }

    // ── Delivery Lifecycle Endpoints

    // Get current active delivery
    @GetMapping("/delivery")
    public DeliveryResponse getActiveDelivery(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.getActiveDelivery(driverId);
    }

    // Accept assigned delivery
    @PostMapping("/delivery/accept")
    public DeliveryResponse acceptDelivery(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.acceptDelivery(driverId);
    }

    // Reject assigned delivery (triggers re-dispatch to next best driver)
    @PostMapping("/delivery/reject")
    public DeliveryResponse rejectDelivery(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.rejectDelivery(driverId);
    }

    // Driver arrived at store for pickup
    @PostMapping("/delivery/arrived-pickup")
    public DeliveryResponse arrivedAtPickup(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.arrivedAtPickup(driverId);
    }

    // Driver picked up order from store
    @PostMapping("/delivery/pickup")
    public DeliveryResponse pickup(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.pickup(driverId);
    }

    // Driver out for delivery towards customer
    @PostMapping("/delivery/out-for-delivery")
    public DeliveryResponse outForDelivery(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.outForDelivery(driverId);
    }

    // Driver arrived at customer drop location
    @PostMapping("/delivery/arrived-drop")
    public DeliveryResponse arrivedAtDrop(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.arrivedAtDrop(driverId);
    }

    // Verify customer OTP and complete delivery (Proof of Delivery)
    @PostMapping("/delivery/complete")
    public DeliveryResponse completeDelivery(
            @Valid @RequestBody DeliveryCompletionRequest request,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.completeDelivery(driverId, request);
    }

    // Mark delivery as failed at drop (e.g. customer unreachable, refused)
    @PostMapping("/delivery/fail")
    public DeliveryResponse failDelivery(
            @Valid @RequestBody DeliveryFailureRequest request,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.failDelivery(driverId, request);
    }

    // Update delivery status (generic fallback)
    @PatchMapping("/delivery/status")
    public DeliveryResponse updateDeliveryStatus(
            @RequestParam DeliveryStatus status,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.updateStatus(driverId, status);
    }

    // Cancel delivery (before pickup only)
    @PostMapping("/delivery/cancel")
    public DeliveryResponse cancelDelivery(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.cancelDelivery(driverId);
    }

    // ── Delivery History Endpoints

    // List deliveries (optionally filtered by status)
    @GetMapping("/deliveries")
    public List<DeliveryResponse> getDeliveries(
            @RequestParam(required = false) DeliveryStatus status,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.getDeliveryHistory(driverId, status);
    }

    // Get specific delivery detail
    @GetMapping("/deliveries/{deliveryId}")
    public DeliveryResponse getDeliveryDetail(
            @PathVariable Long deliveryId,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverDeliveryService.getDeliveryDetail(driverId, deliveryId);
    }

    // ── Driver Earnings & Payout Endpoints

    // Get earnings summary (total earnings, today earnings, delivery counts, recent payouts)
    @GetMapping("/earnings")
    public DriverEarningsSummaryResponse getEarningsSummary(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverPayoutService.getDriverEarningsSummary(driverId);
    }

    // List all payout records
    @GetMapping("/payouts")
    public List<DriverPayoutResponse> getPayouts(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverPayoutService.getDriverPayouts(driverId);
    }

    // ── Driver Profile & Availability Endpoints

    // Get current driver profile and status
    @GetMapping("/status")
    public DriverResponse getStatus(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverService.getDriver(driverId);
    }

    // Update driver availability status (AVAILABLE, OFFLINE, BUSY)
    @PatchMapping("/status")
    public DriverResponse updateStatus(
            @RequestParam DriverStatus status,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverService.updateStatus(driverId, status);
    }

    // ── Location & Telemetry Endpoints

    // Push GPS location update
    @PostMapping("/location")
    public DriverResponse updateLocation(
            @Valid @RequestBody UpdateLocationRequest request,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverService.updateLocation(driverId, request);
    }

    // Read current registered location
    @GetMapping("/location")
    public Map<Object, Object> getLocation(Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        return driverLocationService.getLocation(driverId);
    }

    // ── Push Notification Infrastructure

    // Register FCM/APNs push notification device token
    @PostMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        Long driverId = (Long) authentication.getCredentials();
        String deviceToken = payload.get("deviceToken");
        if (deviceToken != null && !deviceToken.isBlank()) {
            driverService.updateDeviceToken(driverId, deviceToken);
        }
    }
}