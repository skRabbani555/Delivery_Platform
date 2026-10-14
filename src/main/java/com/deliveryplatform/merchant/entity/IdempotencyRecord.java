package com.deliveryplatform.merchant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted idempotency record.
 * When a merchant creates a delivery, we record (merchantId + idempotencyKey) → deliveryId.
 * Any duplicate POST with the same key within TTL returns the original delivery response without
 * re-creating or re-billing.
 */
@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_merchant_idempotency",
                columnNames = {"merchant_id", "idempotency_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    /** The delivery that was created by this request, or null if creation failed. */
    @Column(name = "delivery_id")
    private Long deliveryId;

    /**
     * JSON snapshot of the DeliveryResponse at time of creation.
     * Stored so we can replay it without touching the database.
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
