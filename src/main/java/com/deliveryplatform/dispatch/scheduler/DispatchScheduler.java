package com.deliveryplatform.dispatch.scheduler;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.dispatch.config.DispatchProperties;
import com.deliveryplatform.dispatch.service.DispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DispatchScheduler.class);

    private final DispatchService dispatchService;
    private final DeliveryRepo deliveryRepo;
    private final DispatchProperties properties;

    public DispatchScheduler(
            DispatchService dispatchService,
            DeliveryRepo deliveryRepo,
            DispatchProperties properties) {
        this.dispatchService = dispatchService;
        this.deliveryRepo = deliveryRepo;
        this.properties = properties;
    }

    /**
     * Periodic background worker: retries dispatch for deliveries in SEARCHING_DRIVER.
     * Ensures deliveries created when no drivers were available automatically get assigned when drivers appear.
     */
    @Scheduled(fixedDelayString = "${dispatch.retry-interval-ms:5000}")
    public void sweepPendingDeliveries() {
        try {
            dispatchService.dispatchPendingDeliveries();
        } catch (Exception e) {
            log.error("Error running pending delivery dispatch sweep: {}", e.getMessage(), e);
        }
    }

    /**
     * Periodic assignment timeout monitor:
     * Checks if any assigned delivery has exceeded the driver acceptance window.
     * If so, unassigns the unresponsive driver, releases them, and re-dispatches to next candidate.
     */
    @Scheduled(fixedDelay = 5000)
    public void monitorAssignmentTimeouts() {
        try {
            long timeoutSec = properties.getAssignmentTimeoutSeconds();
            LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(timeoutSec);

            List<Delivery> expiredDeliveries = deliveryRepo.findByStatusAndAssignedAtBefore(
                    DeliveryStatus.DRIVER_ASSIGNED,
                    cutoffTime
            );

            if (!expiredDeliveries.isEmpty()) {
                log.info("Found {} expired driver assignments. Processing timeouts...", expiredDeliveries.size());
                for (Delivery delivery : expiredDeliveries) {
                    try {
                        dispatchService.handleAssignmentTimeout(delivery.getId());
                    } catch (Exception e) {
                        log.error("Error handling timeout for delivery {}: {}", delivery.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error monitoring driver assignment timeouts: {}", e.getMessage(), e);
        }
    }
}
