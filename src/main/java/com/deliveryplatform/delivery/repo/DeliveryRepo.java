package com.deliveryplatform.delivery.repo;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepo extends JpaRepository<Delivery, Long> {

    List<Delivery> findByMerchantId(Long merchantId);

    List<Delivery> findByDriverId(Long driverId);

    List<Delivery> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    List<Delivery> findByDriverIdAndStatusOrderByCreatedAtDesc(Long driverId, DeliveryStatus status);

    List<Delivery> findByStatus(DeliveryStatus status);

    List<Delivery> findByStatusOrderByCreatedAtAsc(DeliveryStatus status);

    List<Delivery> findByStatusAndAssignedAtBefore(DeliveryStatus status, LocalDateTime beforeTime);

    List<Delivery> findByDriverIdAndStatus(Long driverId, DeliveryStatus status);

    boolean existsByDriverIdAndStatusIn(Long driverId, Collection<DeliveryStatus> statuses);

    Optional<Delivery> findByMerchantIdAndExternalOrderId(
            Long merchantId,
            String externalOrderId);

    List<Delivery> findByDriverIdAndStatusNotIn(
            Long driverId,
            List<DeliveryStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Delivery d
            SET d.driver = :driver,
                d.status = :newStatus,
                d.assignedAt = :assignedAt,
                d.estimatedEtaMinutes = :eta,
                d.updatedAt = :updatedAt
            WHERE d.id = :deliveryId
              AND d.status = :expectedStatus
            """)
    int assignDriverAtomically(
            @Param("deliveryId") Long deliveryId,
            @Param("driver") Driver driver,
            @Param("expectedStatus") DeliveryStatus expectedStatus,
            @Param("newStatus") DeliveryStatus newStatus,
            @Param("assignedAt") LocalDateTime assignedAt,
            @Param("eta") Double eta,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Delivery d
            SET d.driver = NULL,
                d.status = :newStatus,
                d.assignedAt = NULL,
                d.estimatedEtaMinutes = NULL,
                d.updatedAt = :updatedAt
            WHERE d.id = :deliveryId
              AND d.status = :expectedStatus
            """)
    int unassignDriverAtomically(
            @Param("deliveryId") Long deliveryId,
            @Param("expectedStatus") DeliveryStatus expectedStatus,
            @Param("newStatus") DeliveryStatus newStatus,
            @Param("updatedAt") LocalDateTime updatedAt);
}