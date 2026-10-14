package com.deliveryplatform.integration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Webhook delivery log – every attempt to POST a status event to a merchant is recorded here.
 * This enables retry, auditing, and debugging.
 */
@Entity
@Table(
        name = "webhook_logs",
        indexes = {
                @Index(name = "idx_webhook_delivery_id", columnList = "delivery_id"),
                @Index(name = "idx_webhook_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    /** JSON body that was posted (or was going to be posted). */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /** HTTP status code returned by the merchant endpoint (null if never reached). */
    @Column(name = "http_status")
    private Integer httpStatus;

    /** Whether the webhook was ultimately delivered successfully. */
    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    /** Error message or response body on failure. */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
