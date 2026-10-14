package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.driver.dto.DriverEarningsSummaryResponse;
import com.deliveryplatform.driver.dto.DriverPayoutResponse;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverPayout;
import com.deliveryplatform.driver.entity.PayoutStatus;
import com.deliveryplatform.driver.repo.DriverPayoutRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DriverPayoutService {

    private static final Logger log = LoggerFactory.getLogger(DriverPayoutService.class);

    private final DriverPayoutRepo driverPayoutRepo;

    public DriverPayoutService(DriverPayoutRepo driverPayoutRepo) {
        this.driverPayoutRepo = driverPayoutRepo;
    }

    /**
     * Record a payout record for a driver upon successful delivery completion.
     */
    @Transactional
    public DriverPayout recordDeliveryPayout(Delivery delivery, Driver driver) {
        if (delivery == null || driver == null) {
            throw new IllegalArgumentException("Delivery and Driver must not be null");
        }

        // Check if already recorded (idempotent)
        Optional<DriverPayout> existing = driverPayoutRepo.findByDeliveryId(delivery.getId());
        if (existing.isPresent()) {
            log.info("Payout record already exists for delivery {}", delivery.getId());
            return existing.get();
        }

        Double amount = delivery.getDriverPayout() != null ? delivery.getDriverPayout() : 0.0;
        Double deliveryFee = delivery.getDeliveryFee() != null ? delivery.getDeliveryFee() : 0.0;
        Double platformFee = delivery.getPlatformFee() != null ? delivery.getPlatformFee() : 0.0;

        DriverPayout payout = DriverPayout.builder()
                .driver(driver)
                .delivery(delivery)
                .amount(amount)
                .deliveryFee(deliveryFee)
                .platformFee(platformFee)
                .status(PayoutStatus.PENDING)
                .earnedAt(LocalDateTime.now())
                .referenceNumber("PAYOUT-" + delivery.getId() + "-" + System.currentTimeMillis())
                .build();

        DriverPayout saved = driverPayoutRepo.save(payout);
        log.info("Created payout record #{} of amount {} for driver {} on delivery {}",
                saved.getId(), saved.getAmount(), driver.getId(), delivery.getId());

        return saved;
    }

    /**
     * Calculate and return earnings summary for a driver.
     */
    @Transactional(readOnly = true)
    public DriverEarningsSummaryResponse getDriverEarningsSummary(Long driverId) {
        Double totalEarnings = driverPayoutRepo.sumEarningsByDriverId(driverId);
        if (totalEarnings == null) {
            totalEarnings = 0.0;
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        Double todayEarnings = driverPayoutRepo.sumEarningsByDriverIdAndEarnedAtAfter(driverId, startOfToday);
        if (todayEarnings == null) {
            todayEarnings = 0.0;
        }

        long totalCompleted = driverPayoutRepo.countByDriverId(driverId);
        long todayCompleted = driverPayoutRepo.countByDriverIdAndEarnedAtAfter(driverId, startOfToday);

        List<DriverPayoutResponse> recentPayouts = driverPayoutRepo.findByDriverIdOrderByEarnedAtDesc(driverId)
                .stream()
                .limit(10)
                .map(this::toResponse)
                .toList();

        return new DriverEarningsSummaryResponse(
                driverId,
                Math.round(totalEarnings * 100.0) / 100.0,
                Math.round(todayEarnings * 100.0) / 100.0,
                totalCompleted,
                todayCompleted,
                recentPayouts
        );
    }

    /**
     * List all payout records for a driver.
     */
    @Transactional(readOnly = true)
    public List<DriverPayoutResponse> getDriverPayouts(Long driverId) {
        return driverPayoutRepo.findByDriverIdOrderByEarnedAtDesc(driverId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DriverPayoutResponse toResponse(DriverPayout payout) {
        return new DriverPayoutResponse(
                payout.getId(),
                payout.getDriver().getId(),
                payout.getDelivery().getId(),
                payout.getAmount(),
                payout.getDeliveryFee(),
                payout.getPlatformFee(),
                payout.getStatus(),
                payout.getEarnedAt(),
                payout.getPaidAt(),
                payout.getReferenceNumber()
        );
    }
}
