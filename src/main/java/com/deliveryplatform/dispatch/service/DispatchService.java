package com.deliveryplatform.dispatch.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryEvent;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.repo.DeliveryEventRepo;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.dispatch.config.DispatchProperties;
import com.deliveryplatform.dispatch.lock.DistributedLockService;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.repo.DriverRepo;
import com.deliveryplatform.integration.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    public static final List<DeliveryStatus> ACTIVE_DELIVERY_STATUSES = List.of(
            DeliveryStatus.DRIVER_ASSIGNED,
            DeliveryStatus.DRIVER_GOING_TO_PICKUP,
            DeliveryStatus.ARRIVED_AT_PICKUP,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.OUT_FOR_DELIVERY,
            DeliveryStatus.ARRIVED_AT_DROP
    );

    private final DeliveryRepo deliveryRepo;
    private final DriverRepo driverRepo;
    private final DeliveryEventRepo deliveryEventRepo;
    private final DistributedLockService lockService;
    private final EtaService etaService;
    private final DispatchRejectionService rejectionService;
    private final DispatchProperties properties;
    private final WebhookService webhookService;
    private final com.deliveryplatform.driver.service.DriverNotificationService driverNotificationService;

    private final AtomicBoolean isDispatchSweepRunning = new AtomicBoolean(false);

    public DispatchService(
            DeliveryRepo deliveryRepo,
            DriverRepo driverRepo,
            DeliveryEventRepo deliveryEventRepo,
            DistributedLockService lockService,
            EtaService etaService,
            DispatchRejectionService rejectionService,
            DispatchProperties properties,
            WebhookService webhookService,
            com.deliveryplatform.driver.service.DriverNotificationService driverNotificationService) {
        this.deliveryRepo = deliveryRepo;
        this.driverRepo = driverRepo;
        this.deliveryEventRepo = deliveryEventRepo;
        this.lockService = lockService;
        this.etaService = etaService;
        this.rejectionService = rejectionService;
        this.properties = properties;
        this.webhookService = webhookService;
        this.driverNotificationService = driverNotificationService;
    }

    /**
     * Dispatch an available driver to a delivery with concurrency protection and ETA scoring.
     */
    @Transactional
    public Delivery dispatch(Delivery delivery) {
        if (delivery == null || delivery.getId() == null) {
            return delivery;
        }

        String deliveryLockKey = "lock:delivery:" + delivery.getId();
        String deliveryLockToken = lockService.acquireLock(deliveryLockKey, LOCK_TIMEOUT);
        if (deliveryLockToken == null) {
            log.warn("Delivery {} is currently being dispatched by another thread/process", delivery.getId());
            return delivery;
        }

        try {
            // Refresh from DB to guarantee latest status
            Delivery freshDelivery = deliveryRepo.findById(delivery.getId())
                    .orElse(delivery);

            if (freshDelivery.getStatus() != DeliveryStatus.SEARCHING_DRIVER) {
                log.info("Delivery {} is not in SEARCHING_DRIVER state (current: {})",
                        freshDelivery.getId(), freshDelivery.getStatus());
                return freshDelivery;
            }

            if (freshDelivery.getPickupLatitude() == null || freshDelivery.getPickupLongitude() == null) {
                throw new RuntimeException("Pickup coordinates are required for driver dispatch");
            }

            // 1. Fetch active available drivers
            List<Driver> availableDrivers = driverRepo.findByStatusAndActiveTrue(DriverStatus.AVAILABLE);
            if (availableDrivers.isEmpty()) {
                log.debug("No available drivers online for delivery {}", freshDelivery.getId());
                return freshDelivery;
            }

            // 2. Filter candidates:
            // - Valid coordinates
            // - Not already rejected this delivery
            // - Does NOT already have an active assigned delivery
            List<Driver> eligibleDrivers = availableDrivers.stream()
                    .filter(driver -> driver.getLatitude() != null && driver.getLongitude() != null)
                    .filter(driver -> !rejectionService.isDriverRejected(freshDelivery.getId(), driver.getId()))
                    .filter(driver -> !hasActiveDelivery(driver.getId()))
                    .toList();

            if (eligibleDrivers.isEmpty()) {
                log.debug("No eligible candidate drivers found for delivery {}", freshDelivery.getId());
                return freshDelivery;
            }

            // 3. Score and rank candidates by ETA (with straight-line distance as tie-breaker)
            List<EtaService.DriverCandidateScore> candidateScores = eligibleDrivers.stream()
                    .map(driver -> etaService.scoreDriver(
                            driver,
                            freshDelivery.getPickupLatitude(),
                            freshDelivery.getPickupLongitude()))
                    .filter(score -> score.distanceKm() <= properties.getMaxSearchRadiusKm())
                    .sorted()
                    .toList();

            if (candidateScores.isEmpty()) {
                log.debug("No candidate drivers within search radius ({} km) for delivery {}",
                        properties.getMaxSearchRadiusKm(), freshDelivery.getId());
                return freshDelivery;
            }

            // 4. Concurrently-safe assignment attempt across ranked candidates
            for (EtaService.DriverCandidateScore candidate : candidateScores) {
                Driver driver = candidate.driver();
                String driverLockKey = "lock:driver:" + driver.getId();
                String driverLockToken = lockService.acquireLock(driverLockKey, LOCK_TIMEOUT);

                if (driverLockToken == null) {
                    // Driver is being processed by another concurrent dispatch
                    log.debug("Driver {} is currently locked, trying next candidate", driver.getId());
                    continue;
                }

                try {
                    // Double check driver still has no active delivery
                    if (hasActiveDelivery(driver.getId())) {
                        log.debug("Driver {} acquired active delivery concurrently, skipping", driver.getId());
                        continue;
                    }

                    // Atomic CAS: Only transition driver to BUSY if currently AVAILABLE
                    int driverUpdated = driverRepo.updateStatusIfCurrentStatus(
                            driver.getId(),
                            DriverStatus.AVAILABLE,
                            DriverStatus.BUSY
                    );

                    if (driverUpdated == 0) {
                        log.debug("Driver {} status changed concurrently, trying next candidate", driver.getId());
                        continue;
                    }

                    LocalDateTime now = LocalDateTime.now();
                    double etaMinutes = candidate.etaMinutes();

                    // Atomic CAS: Only assign delivery if still SEARCHING_DRIVER
                    int deliveryUpdated = deliveryRepo.assignDriverAtomically(
                            freshDelivery.getId(),
                            driver,
                            DeliveryStatus.SEARCHING_DRIVER,
                            DeliveryStatus.DRIVER_ASSIGNED,
                            now,
                            etaMinutes,
                            now
                    );

                    if (deliveryUpdated == 0) {
                        log.warn("Delivery {} status changed concurrently during assignment. Rolling back driver {}",
                                freshDelivery.getId(), driver.getId());
                        driverRepo.updateStatusIfCurrentStatus(driver.getId(), DriverStatus.BUSY, DriverStatus.AVAILABLE);
                        return freshDelivery;
                    }

                    // Successful assignment! Update memory reference
                    freshDelivery.setDriver(driver);
                    freshDelivery.setStatus(DeliveryStatus.DRIVER_ASSIGNED);
                    freshDelivery.setAssignedAt(now);
                    freshDelivery.setEstimatedEtaMinutes(etaMinutes);
                    freshDelivery.setUpdatedAt(now);

                    driver.setStatus(DriverStatus.BUSY);

                    // Record event & trigger webhook
                    recordDeliveryEvent(
                            freshDelivery,
                            DeliveryStatus.DRIVER_ASSIGNED,
                            "SYSTEM",
                            driver.getId(),
                            String.format("Assigned driver %s (ETA: %.1f mins, Dist: %.2f km)",
                                    driver.getName(), etaMinutes, candidate.distanceKm())
                    );

                    webhookService.sendDeliveryStatusWebhook(freshDelivery);
                    driverNotificationService.notifyJobAssigned(driver, freshDelivery);

                    log.info("Successfully assigned driver {} ({}) to delivery {} with ETA: {} mins",
                            driver.getId(), driver.getName(), freshDelivery.getId(), etaMinutes);

                    return freshDelivery;

                } finally {
                    lockService.releaseLock(driverLockKey, driverLockToken);
                }
            }

            return freshDelivery;

        } finally {
            lockService.releaseLock(deliveryLockKey, deliveryLockToken);
        }
    }

    /**
     * Manually assign a driver to a delivery.
     */
    @Transactional
    public Delivery assignDriver(Long deliveryId) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        return dispatch(delivery);
    }

    /**
     * Handle driver rejection of an assigned delivery.
     */
    @Transactional
    public Delivery handleDriverRejection(Long deliveryId, Long driverId) {
        String lockKey = "lock:delivery:" + deliveryId;
        String token = lockService.acquireLock(lockKey, LOCK_TIMEOUT);
        if (token == null) {
            log.warn("Could not acquire lock for delivery rejection {}", deliveryId);
        }

        try {
            Delivery delivery = deliveryRepo.findById(deliveryId)
                    .orElseThrow(() -> new RuntimeException("Delivery not found"));

            if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driverId)) {
                throw new RuntimeException("This delivery is not assigned to you");
            }

            if (delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED) {
                throw new RuntimeException("Only an assigned delivery can be rejected");
            }

            Driver driver = delivery.getDriver();

            // 1. Record rejection in registry so this driver is excluded from re-dispatch
            rejectionService.recordRejection(deliveryId, driverId);

            // 2. Unassign driver and reset delivery to SEARCHING_DRIVER
            LocalDateTime now = LocalDateTime.now();
            deliveryRepo.unassignDriverAtomically(
                    deliveryId,
                    DeliveryStatus.DRIVER_ASSIGNED,
                    DeliveryStatus.SEARCHING_DRIVER,
                    now
            );

            // 3. Driver is returned to AVAILABLE for other orders
            driverRepo.updateStatusIfCurrentStatus(driverId, DriverStatus.BUSY, DriverStatus.AVAILABLE);

            delivery.setDriver(null);
            delivery.setStatus(DeliveryStatus.SEARCHING_DRIVER);
            delivery.setAssignedAt(null);
            delivery.setEstimatedEtaMinutes(null);
            delivery.setUpdatedAt(now);

            recordDeliveryEvent(
                    delivery,
                    DeliveryStatus.SEARCHING_DRIVER,
                    "DRIVER",
                    driverId,
                    "Driver rejected delivery assignment"
            );

            log.info("Driver {} rejected delivery {}. Re-dispatching to next candidate...", driverId, deliveryId);

            // 4. Immediately attempt re-dispatch to the next best driver
            Delivery redispatched = dispatch(delivery);

            // Trigger sweep so rejecting driver can receive other pending deliveries
            triggerPendingDispatchAsync();

            return redispatched;

        } finally {
            if (token != null) {
                lockService.releaseLock(lockKey, token);
            }
        }
    }

    /**
     * Handle driver assignment timeout (driver failed to accept within timeout window).
     */
    @Transactional
    public void handleAssignmentTimeout(Long deliveryId) {
        String lockKey = "lock:delivery:" + deliveryId;
        String token = lockService.acquireLock(lockKey, LOCK_TIMEOUT);
        if (token == null) {
            return;
        }

        try {
            Delivery delivery = deliveryRepo.findById(deliveryId).orElse(null);
            if (delivery == null || delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED || delivery.getDriver() == null) {
                return;
            }

            Long driverId = delivery.getDriver().getId();

            log.warn("Assignment timeout expired for delivery {} with driver {}. Unassigning and re-dispatching...",
                    deliveryId, driverId);

            // Record timeout as rejection so we try next driver
            rejectionService.recordRejection(deliveryId, driverId);

            LocalDateTime now = LocalDateTime.now();
            deliveryRepo.unassignDriverAtomically(
                    deliveryId,
                    DeliveryStatus.DRIVER_ASSIGNED,
                    DeliveryStatus.SEARCHING_DRIVER,
                    now
            );

            // Release driver back to AVAILABLE
            driverRepo.updateStatusIfCurrentStatus(driverId, DriverStatus.BUSY, DriverStatus.AVAILABLE);

            delivery.setDriver(null);
            delivery.setStatus(DeliveryStatus.SEARCHING_DRIVER);
            delivery.setAssignedAt(null);
            delivery.setEstimatedEtaMinutes(null);
            delivery.setUpdatedAt(now);

            Driver assignedDriver = delivery.getDriver();

            recordDeliveryEvent(
                    delivery,
                    DeliveryStatus.SEARCHING_DRIVER,
                    "SYSTEM",
                    driverId,
                    "Driver assignment timed out without acceptance"
            );

            if (assignedDriver != null) {
                driverNotificationService.notifyJobCancelled(assignedDriver, delivery);
            }

            // Re-dispatch
            dispatch(delivery);

        } finally {
            lockService.releaseLock(lockKey, token);
        }
    }

    /**
     * Handle driver becoming unavailable (e.g. going OFFLINE) while having an assigned delivery.
     */
    @Transactional
    public void handleDriverUnavailable(Long driverId, Long deliveryId) {
        String lockKey = "lock:delivery:" + deliveryId;
        String token = lockService.acquireLock(lockKey, LOCK_TIMEOUT);
        if (token == null) {
            return;
        }

        try {
            Delivery delivery = deliveryRepo.findById(deliveryId).orElse(null);
            if (delivery == null || delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED) {
                return;
            }

            rejectionService.recordRejection(deliveryId, driverId);

            LocalDateTime now = LocalDateTime.now();
            deliveryRepo.unassignDriverAtomically(
                    deliveryId,
                    DeliveryStatus.DRIVER_ASSIGNED,
                    DeliveryStatus.SEARCHING_DRIVER,
                    now
            );

            delivery.setDriver(null);
            delivery.setStatus(DeliveryStatus.SEARCHING_DRIVER);
            delivery.setAssignedAt(null);
            delivery.setEstimatedEtaMinutes(null);
            delivery.setUpdatedAt(now);

            recordDeliveryEvent(
                    delivery,
                    DeliveryStatus.SEARCHING_DRIVER,
                    "SYSTEM",
                    driverId,
                    "Driver became unavailable / went offline while assigned"
            );

            log.info("Driver {} went offline while assigned to delivery {}. Re-dispatching...", driverId, deliveryId);

            dispatch(delivery);

        } finally {
            lockService.releaseLock(lockKey, token);
        }
    }

    /**
     * Asynchronously trigger a dispatch sweep for all pending deliveries in SEARCHING_DRIVER.
     */
    @Async
    public void triggerPendingDispatchAsync() {
        dispatchPendingDeliveries();
    }

    /**
     * Sweep and dispatch all deliveries currently waiting in SEARCHING_DRIVER order by FIFO.
     */
    public void dispatchPendingDeliveries() {
        if (!isDispatchSweepRunning.compareAndSet(false, true)) {
            log.debug("Dispatch sweep already in progress, skipping redundant run");
            return;
        }

        try {
            List<Delivery> pendingDeliveries = deliveryRepo.findByStatusOrderByCreatedAtAsc(
                    DeliveryStatus.SEARCHING_DRIVER
            );

            if (!pendingDeliveries.isEmpty()) {
                log.info("Processing {} pending deliveries waiting for driver dispatch", pendingDeliveries.size());
                for (Delivery pending : pendingDeliveries) {
                    try {
                        dispatch(pending);
                    } catch (Exception e) {
                        log.error("Error dispatching pending delivery {}: {}", pending.getId(), e.getMessage());
                    }
                }
            }
        } finally {
            isDispatchSweepRunning.set(false);
        }
    }

    /**
     * Check if a driver currently has any active assigned or in-transit delivery.
     */
    public boolean hasActiveDelivery(Long driverId) {
        return deliveryRepo.existsByDriverIdAndStatusIn(driverId, ACTIVE_DELIVERY_STATUSES);
    }

    private void recordDeliveryEvent(
            Delivery delivery,
            DeliveryStatus status,
            String actorType,
            Long actorId,
            String note) {

        DeliveryEvent event = DeliveryEvent.builder()
                .delivery(delivery)
                .status(status)
                .actorType(actorType)
                .actorId(actorId)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();

        deliveryEventRepo.save(event);
    }
}