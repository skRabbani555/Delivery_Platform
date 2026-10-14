package com.deliveryplatform.driver.repo;

import com.deliveryplatform.driver.entity.DriverPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DriverPayoutRepo extends JpaRepository<DriverPayout, Long> {

    List<DriverPayout> findByDriverIdOrderByEarnedAtDesc(Long driverId);

    List<DriverPayout> findByDriverIdAndEarnedAtBetweenOrderByEarnedAtDesc(
            Long driverId,
            LocalDateTime start,
            LocalDateTime end);

    Optional<DriverPayout> findByDeliveryId(Long deliveryId);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM DriverPayout p WHERE p.driver.id = :driverId")
    Double sumEarningsByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM DriverPayout p WHERE p.driver.id = :driverId AND p.earnedAt >= :after")
    Double sumEarningsByDriverIdAndEarnedAtAfter(
            @Param("driverId") Long driverId,
            @Param("after") LocalDateTime after);

    long countByDriverId(Long driverId);

    long countByDriverIdAndEarnedAtAfter(Long driverId, LocalDateTime after);
}
