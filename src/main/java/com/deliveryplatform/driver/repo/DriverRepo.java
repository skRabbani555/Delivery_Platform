package com.deliveryplatform.driver.repo;

import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepo extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmail(String email);

    List<Driver> findByStatusAndActiveTrue(DriverStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Driver d
            SET d.status = :newStatus
            WHERE d.id = :driverId
              AND d.status = :expectedStatus
            """)
    int updateStatusIfCurrentStatus(
            @Param("driverId") Long driverId,
            @Param("expectedStatus") DriverStatus expectedStatus,
            @Param("newStatus") DriverStatus newStatus);
}