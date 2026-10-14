package com.deliveryplatform.delivery.service;

import com.deliveryplatform.delivery.dto.DeliveryCompletionRequest;
import com.deliveryplatform.delivery.dto.DeliveryFailureRequest;
import com.deliveryplatform.delivery.dto.DeliveryRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryEvent;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.repo.DeliveryEventRepo;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.dispatch.service.DispatchService;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.repo.DriverRepo;
import com.deliveryplatform.driver.service.DriverPayoutService;
import com.deliveryplatform.integration.service.WebhookService;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.merchant.repo.MerchantRepository;
import com.deliveryplatform.pricing.service.PricingService;
import com.deliveryplatform.store.entity.Store;
import com.deliveryplatform.store.repo.StoreRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DeliveryService {

    private static final Set<DeliveryStatus> IN_TRANSIT_STATUSES = Set.of(
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.OUT_FOR_DELIVERY,
            DeliveryStatus.ARRIVED_AT_DROP
    );

    private static final Set<DeliveryStatus> DRIVER_ALLOWED_STATUSES = Set.of(
            DeliveryStatus.DRIVER_GOING_TO_PICKUP,
            DeliveryStatus.ARRIVED_AT_PICKUP,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.OUT_FOR_DELIVERY,
            DeliveryStatus.ARRIVED_AT_DROP,
            DeliveryStatus.DELIVERED,
            DeliveryStatus.FAILED
    );

    private final DeliveryRepo deliveryRepo;
    private final MerchantRepository merchantRepository;
    private final StoreRepo storeRepo;
    private final DispatchService dispatchService;
    private final DeliveryEventRepo deliveryEventRepo;
    private final WebhookService webhookService;
    private final PricingService pricingService;
    private final DriverRepo driverRepo;
    private final DriverPayoutService driverPayoutService;

    public DeliveryService(
            DeliveryRepo deliveryRepo,
            MerchantRepository merchantRepository,
            StoreRepo storeRepo,
            DispatchService dispatchService,
            DeliveryEventRepo deliveryEventRepo,
            WebhookService webhookService,
            PricingService pricingService,
            DriverRepo driverRepo,
            DriverPayoutService driverPayoutService) {
        this.deliveryRepo = deliveryRepo;
        this.merchantRepository = merchantRepository;
        this.storeRepo = storeRepo;
        this.dispatchService = dispatchService;
        this.deliveryEventRepo = deliveryEventRepo;
        this.webhookService = webhookService;
        this.pricingService = pricingService;
        this.driverRepo = driverRepo;
        this.driverPayoutService = driverPayoutService;
    }

    @Transactional
    public DeliveryResponse createDelivery(
            Long merchantId,
            DeliveryRequest request) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        Store store = storeRepo.findById(request.storeId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        if (!store.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Store does not belong to merchant");
        }

        deliveryRepo.findByMerchantIdAndExternalOrderId(
                merchantId,
                request.externalOrderId()).ifPresent(existing -> {
            throw new RuntimeException(
                    "Delivery already exists for external order: "
                            + request.externalOrderId());
        });

        Delivery delivery = Delivery.builder()
                .merchant(merchant)
                .store(store)
                .externalOrderId(request.externalOrderId())
                .pickupAddress(request.pickupAddress())
                .pickupLatitude(request.pickupLatitude())
                .pickupLongitude(request.pickupLongitude())
                .dropAddress(request.dropAddress())
                .dropLatitude(request.dropLatitude())
                .dropLongitude(request.dropLongitude())
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .packageWeight(request.packageWeight())
                .deliveryType(request.deliveryType())
                .status(DeliveryStatus.CREATED)
                .build();

        Delivery saved = deliveryRepo.save(delivery);

        /*
         * Calculate delivery pricing.
         */
        pricingService.calculateAndApplyPricing(saved);

        saved.setStatus(DeliveryStatus.SEARCHING_DRIVER);
        saved = deliveryRepo.save(saved);

        recordEvent(
                saved,
                DeliveryStatus.SEARCHING_DRIVER,
                "MERCHANT",
                merchantId,
                "Delivery created and started automatic dispatch");

        /*
         * Automatic dispatch finds an available driver using ETA and concurrency locks.
         * If no driver is currently available, the delivery remains in SEARCHING_DRIVER
         * and the background scheduler / event dispatcher will automatically match it.
         */
        Delivery dispatched = dispatchService.dispatch(saved);

        return toResponse(dispatched);
    }

    public DeliveryResponse getDelivery(Long deliveryId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        return toResponse(delivery);
    }

    public List<DeliveryResponse> getMerchantDeliveries(Long merchantId) {

        return deliveryRepo.findByMerchantId(merchantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DeliveryResponse updateStatus(
            Long deliveryId,
            DeliveryStatus newStatus,
            String actorType,
            Long actorId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (!isValidTransition(delivery.getStatus(), newStatus)) {
            throw new RuntimeException(
                    "Invalid delivery status transition: "
                            + delivery.getStatus()
                            + " -> "
                            + newStatus);
        }

        if ("MERCHANT".equals(actorType)) {
            if (!delivery.getMerchant().getId().equals(actorId)) {
                throw new RuntimeException("You do not own this delivery");
            }
            if (DRIVER_ALLOWED_STATUSES.contains(newStatus)) {
                throw new RuntimeException("Merchants cannot execute driver operational status updates");
            }
        }

        if ("DRIVER".equals(actorType)) {
            if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(actorId)) {
                throw new RuntimeException("This delivery is not assigned to you");
            }
            if (!DRIVER_ALLOWED_STATUSES.contains(newStatus)) {
                throw new RuntimeException("Drivers cannot transition delivery to " + newStatus);
            }
            if (newStatus == DeliveryStatus.DELIVERED) {
                throw new RuntimeException(
                        "Delivery cannot be marked DELIVERED directly. Please call the /complete endpoint with customer verification OTP.");
            }
        }

        delivery.setStatus(newStatus);

        if (newStatus == DeliveryStatus.PICKED_UP) {
            delivery.setActualPickupTime(LocalDateTime.now());
        }

        Delivery saved = deliveryRepo.save(delivery);

        recordEvent(
                saved,
                newStatus,
                actorType,
                actorId,
                "Status transitioned to " + newStatus);

        webhookService.sendDeliveryStatusWebhook(saved);

        return "DRIVER".equals(actorType) ? toDriverResponse(saved) : toResponse(saved);
    }

    /**
     * Complete delivery with customer OTP and proof-of-delivery verification.
     */
    @Transactional
    public DeliveryResponse verifyAndCompleteDelivery(
            Long deliveryId,
            Long driverId,
            DeliveryCompletionRequest request) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("This delivery is not assigned to you");
        }

        if (delivery.getStatus() != DeliveryStatus.ARRIVED_AT_DROP
                && delivery.getStatus() != DeliveryStatus.OUT_FOR_DELIVERY) {
            throw new RuntimeException(
                    "Delivery must be at drop location before completion. Current status: " + delivery.getStatus());
        }

        // Strict OTP verification
        if (delivery.getDeliveryOtp() == null || !delivery.getDeliveryOtp().trim().equalsIgnoreCase(request.otp().trim())) {
            throw new RuntimeException("Invalid delivery verification OTP. Delivery completion rejected.");
        }

        delivery.setOtpVerified(true);
        delivery.setProofOfDeliveryNotes(request.notes());
        delivery.setProofOfDeliveryImageUrl(request.imageUrl());
        delivery.setRecipientName(request.recipientName());
        delivery.setPodDropLatitude(request.dropLatitude());
        delivery.setPodDropLongitude(request.dropLongitude());
        delivery.setActualDeliveryTime(LocalDateTime.now());
        delivery.setStatus(DeliveryStatus.DELIVERED);

        Driver driver = delivery.getDriver();
        driver.setStatus(DriverStatus.AVAILABLE);
        driverRepo.save(driver);

        Delivery saved = deliveryRepo.save(delivery);

        // Record driver earning/payout record
        driverPayoutService.recordDeliveryPayout(saved, driver);

        recordEvent(
                saved,
                DeliveryStatus.DELIVERED,
                "DRIVER",
                driverId,
                "Delivery completed with OTP verification. Handed to: "
                        + (request.recipientName() != null ? request.recipientName() : "recipient"));

        webhookService.sendDeliveryStatusWebhook(saved);

        // Driver is free now; trigger pending orders sweep
        dispatchService.triggerPendingDispatchAsync();

        return toDriverResponse(saved);
    }

    /**
     * Mark delivery as failed at drop location (e.g. customer unavailable, address incorrect, refused).
     */
    @Transactional
    public DeliveryResponse failDelivery(
            Long deliveryId,
            Long driverId,
            DeliveryFailureRequest request) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("This delivery is not assigned to you");
        }

        if (delivery.getStatus() == DeliveryStatus.DELIVERED
                || delivery.getStatus() == DeliveryStatus.CANCELLED
                || delivery.getStatus() == DeliveryStatus.FAILED) {
            throw new RuntimeException("Cannot fail a delivery in terminal state: " + delivery.getStatus());
        }

        String fullReason = request.reason() + (request.notes() != null && !request.notes().isBlank()
                ? " - " + request.notes()
                : "");

        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(fullReason);
        delivery.setPodDropLatitude(request.latitude());
        delivery.setPodDropLongitude(request.longitude());

        Driver driver = delivery.getDriver();
        driver.setStatus(DriverStatus.AVAILABLE);
        driverRepo.save(driver);

        Delivery saved = deliveryRepo.save(delivery);

        recordEvent(
                saved,
                DeliveryStatus.FAILED,
                "DRIVER",
                driverId,
                "Delivery failed: " + fullReason);

        webhookService.sendDeliveryStatusWebhook(saved);

        dispatchService.triggerPendingDispatchAsync();

        return toDriverResponse(saved);
    }

    @Transactional
    public DeliveryResponse cancelDelivery(
            Long deliveryId,
            String actorType,
            Long actorId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new RuntimeException("Delivered delivery cannot be cancelled");
        }

        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new RuntimeException("Delivery is already cancelled");
        }

        if (delivery.getStatus() == DeliveryStatus.FAILED) {
            throw new RuntimeException("Failed delivery cannot be cancelled");
        }

        // Prevent cancellation once goods are picked up / in-transit
        if (IN_TRANSIT_STATUSES.contains(delivery.getStatus())) {
            throw new RuntimeException(
                    "Package has already been picked up. In-transit delivery cannot be cancelled; must be completed or marked failed.");
        }

        if ("MERCHANT".equals(actorType)
                && !delivery.getMerchant().getId().equals(actorId)) {
            throw new RuntimeException("You do not own this delivery");
        }

        if ("DRIVER".equals(actorType)
                && (delivery.getDriver() == null || !delivery.getDriver().getId().equals(actorId))) {
            throw new RuntimeException("This delivery is not assigned to you");
        }

        Driver driver = delivery.getDriver();

        delivery.setStatus(DeliveryStatus.CANCELLED);
        delivery.setDriver(null);
        delivery.setAssignedAt(null);

        boolean driverFreed = false;
        if (driver != null) {
            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepo.save(driver);
            driverFreed = true;
        }

        Delivery saved = deliveryRepo.save(delivery);

        recordEvent(
                saved,
                DeliveryStatus.CANCELLED,
                actorType,
                actorId,
                "Delivery cancelled");

        webhookService.sendDeliveryStatusWebhook(saved);

        if (driverFreed) {
            dispatchService.triggerPendingDispatchAsync();
        }

        return toResponse(saved);
    }

    @Transactional
    public DeliveryResponse rejectDelivery(Long deliveryId, Long driverId) {
        Delivery redispatched = dispatchService.handleDriverRejection(deliveryId, driverId);
        return toDriverResponse(redispatched);
    }

    private boolean isValidTransition(
            DeliveryStatus current,
            DeliveryStatus next) {

        return switch (current) {

            case CREATED ->
                    next == DeliveryStatus.SEARCHING_DRIVER;

            case SEARCHING_DRIVER ->
                    next == DeliveryStatus.DRIVER_ASSIGNED
                            || next == DeliveryStatus.CANCELLED
                            || next == DeliveryStatus.FAILED;

            case DRIVER_ASSIGNED ->
                    next == DeliveryStatus.DRIVER_GOING_TO_PICKUP
                            || next == DeliveryStatus.CANCELLED
                            || next == DeliveryStatus.SEARCHING_DRIVER;

            case DRIVER_GOING_TO_PICKUP ->
                    next == DeliveryStatus.ARRIVED_AT_PICKUP
                            || next == DeliveryStatus.CANCELLED;

            case ARRIVED_AT_PICKUP ->
                    next == DeliveryStatus.PICKED_UP
                            || next == DeliveryStatus.CANCELLED;

            case PICKED_UP ->
                    next == DeliveryStatus.OUT_FOR_DELIVERY;

            case OUT_FOR_DELIVERY ->
                    next == DeliveryStatus.ARRIVED_AT_DROP
                            || next == DeliveryStatus.DELIVERED
                            || next == DeliveryStatus.FAILED;

            case ARRIVED_AT_DROP ->
                    next == DeliveryStatus.DELIVERED
                            || next == DeliveryStatus.FAILED;

            case DELIVERED,
                 CANCELLED,
                 FAILED ->
                    false;
        };
    }

    private void recordEvent(
            Delivery delivery,
            DeliveryStatus status,
            String actorType,
            Long actorId,
            String note) {

        var event = new DeliveryEvent();

        event.setDelivery(delivery);
        event.setStatus(status);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setNote(note);
        event.setCreatedAt(LocalDateTime.now());

        deliveryEventRepo.save(event);
    }

    public List<DeliveryEvent> getDeliveryEvents(Long deliveryId) {

        deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        return deliveryEventRepo.findByDeliveryIdOrderByCreatedAtAsc(
                deliveryId);
    }

    public DeliveryResponse toResponse(Delivery delivery) {

        return new DeliveryResponse(
                delivery.getId(),
                delivery.getMerchant().getId(),
                delivery.getStore().getId(),
                delivery.getDriver() != null
                        ? delivery.getDriver().getId()
                        : null,
                delivery.getExternalOrderId(),
                delivery.getPickupAddress(),
                delivery.getDropAddress(),
                delivery.getCustomerName(),
                delivery.getCustomerPhone(),
                delivery.getPackageWeight(),
                delivery.getDeliveryType(),
                delivery.getStatus(),
                delivery.getDeliveryFee(),
                delivery.getDriverPayout(),
                delivery.getPlatformFee(),
                delivery.getEstimatedEtaMinutes(),
                delivery.getAssignedAt(),
                delivery.getDeliveryOtp(),
                delivery.getOtpVerified(),
                delivery.getProofOfDeliveryNotes(),
                delivery.getProofOfDeliveryImageUrl(),
                delivery.getRecipientName(),
                delivery.getActualPickupTime(),
                delivery.getActualDeliveryTime(),
                delivery.getFailureReason());
    }

    public DeliveryResponse toDriverResponse(Delivery delivery) {
        // Driver only sees OTP after verification (or null beforehand to prevent fraudulent bypass)
        String otpForDriver = Boolean.TRUE.equals(delivery.getOtpVerified()) ? delivery.getDeliveryOtp() : null;

        return new DeliveryResponse(
                delivery.getId(),
                delivery.getMerchant().getId(),
                delivery.getStore().getId(),
                delivery.getDriver() != null
                        ? delivery.getDriver().getId()
                        : null,
                delivery.getExternalOrderId(),
                delivery.getPickupAddress(),
                delivery.getDropAddress(),
                delivery.getCustomerName(),
                delivery.getCustomerPhone(),
                delivery.getPackageWeight(),
                delivery.getDeliveryType(),
                delivery.getStatus(),
                delivery.getDeliveryFee(),
                delivery.getDriverPayout(),
                delivery.getPlatformFee(),
                delivery.getEstimatedEtaMinutes(),
                delivery.getAssignedAt(),
                otpForDriver,
                delivery.getOtpVerified(),
                delivery.getProofOfDeliveryNotes(),
                delivery.getProofOfDeliveryImageUrl(),
                delivery.getRecipientName(),
                delivery.getActualPickupTime(),
                delivery.getActualDeliveryTime(),
                delivery.getFailureReason());
    }

    @Transactional
    public DeliveryResponse dispatchDelivery(Long deliveryId) {

        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        Delivery dispatched = dispatchService.dispatch(delivery);

        return toResponse(dispatched);
    }
}
