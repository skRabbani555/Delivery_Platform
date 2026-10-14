package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.dto.DeliveryCompletionRequest;
import com.deliveryplatform.delivery.dto.DeliveryFailureRequest;
import com.deliveryplatform.delivery.dto.DeliveryResponse;
import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.delivery.service.DeliveryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverDeliveryService {

    private final DeliveryRepo deliveryRepo;
    private final DeliveryService deliveryService;

    public DriverDeliveryService(
            DeliveryRepo deliveryRepo,
            DeliveryService deliveryService) {
        this.deliveryRepo = deliveryRepo;
        this.deliveryService = deliveryService;
    }

    // Get driver's active delivery
    public DeliveryResponse getActiveDelivery(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.toDriverResponse(delivery);
    }

    // Accept assigned delivery
    public DeliveryResponse acceptDelivery(Long driverId) {
        Delivery delivery = findAssignedDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                DeliveryStatus.DRIVER_GOING_TO_PICKUP,
                "DRIVER",
                driverId);
    }

    // Reject assigned delivery
    public DeliveryResponse rejectDelivery(Long driverId) {
        Delivery delivery = findAssignedDelivery(driverId);
        return deliveryService.rejectDelivery(
                delivery.getId(),
                driverId);
    }

    // Driver arrived at store for pickup
    public DeliveryResponse arrivedAtPickup(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                DeliveryStatus.ARRIVED_AT_PICKUP,
                "DRIVER",
                driverId);
    }

    // Driver picked up package from store
    public DeliveryResponse pickup(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                DeliveryStatus.PICKED_UP,
                "DRIVER",
                driverId);
    }

    // Driver out for delivery towards customer
    public DeliveryResponse outForDelivery(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                DeliveryStatus.OUT_FOR_DELIVERY,
                "DRIVER",
                driverId);
    }

    // Driver arrived at customer drop location
    public DeliveryResponse arrivedAtDrop(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                DeliveryStatus.ARRIVED_AT_DROP,
                "DRIVER",
                driverId);
    }

    // Complete delivery with customer OTP and proof-of-delivery verification
    public DeliveryResponse completeDelivery(Long driverId, DeliveryCompletionRequest request) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.verifyAndCompleteDelivery(
                delivery.getId(),
                driverId,
                request);
    }

    // Fail delivery with reason notes
    public DeliveryResponse failDelivery(Long driverId, DeliveryFailureRequest request) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.failDelivery(
                delivery.getId(),
                driverId,
                request);
    }

    // Update delivery status (generic fallback, validated against lifecycle)
    public DeliveryResponse updateStatus(
            Long driverId,
            DeliveryStatus newStatus) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.updateStatus(
                delivery.getId(),
                newStatus,
                "DRIVER",
                driverId);
    }

    // Cancel delivery (only permitted before pickup)
    public DeliveryResponse cancelDelivery(Long driverId) {
        Delivery delivery = findActiveDelivery(driverId);
        return deliveryService.cancelDelivery(
                delivery.getId(),
                "DRIVER",
                driverId);
    }

    // Get delivery history for driver
    public List<DeliveryResponse> getDeliveryHistory(Long driverId, DeliveryStatus status) {
        List<Delivery> list;
        if (status != null) {
            list = deliveryRepo.findByDriverIdAndStatusOrderByCreatedAtDesc(driverId, status);
        } else {
            list = deliveryRepo.findByDriverIdOrderByCreatedAtDesc(driverId);
        }

        return list.stream()
                .map(deliveryService::toDriverResponse)
                .toList();
    }

    // Get single delivery detail for driver
    public DeliveryResponse getDeliveryDetail(Long driverId, Long deliveryId) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("This delivery is not assigned to you");
        }

        return deliveryService.toDriverResponse(delivery);
    }

    // Find any active delivery assigned to driver
    private Delivery findActiveDelivery(Long driverId) {
        List<Delivery> deliveries =
                deliveryRepo.findByDriverIdAndStatusNotIn(
                        driverId,
                        List.of(
                                DeliveryStatus.DELIVERED,
                                DeliveryStatus.CANCELLED,
                                DeliveryStatus.FAILED));

        if (deliveries.isEmpty()) {
            throw new RuntimeException("No active delivery found");
        }

        return deliveries.get(0);
    }

    // Find delivery waiting for driver acceptance
    private Delivery findAssignedDelivery(Long driverId) {
        List<Delivery> deliveries =
                deliveryRepo.findByDriverIdAndStatusNotIn(
                        driverId,
                        List.of(
                                DeliveryStatus.DELIVERED,
                                DeliveryStatus.CANCELLED,
                                DeliveryStatus.FAILED));

        return deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.DRIVER_ASSIGNED)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No assigned delivery available"));
    }
}