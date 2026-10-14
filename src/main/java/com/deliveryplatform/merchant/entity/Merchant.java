package com.deliveryplatform.merchant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants",
        indexes = @Index(name = "idx_merchant_api_key", columnList = "apiKey", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Webhook URL the platform POSTs delivery status updates to. */
    private String webhookUrl;

    /**
     * Stable API key used instead of (or alongside) JWT for machine-to-machine calls.
     * Generated once at merchant registration; can be rotated via admin endpoint.
     */
    @Column(unique = true)
    private String apiKey;

    /**
     * HMAC-SHA256 signing secret appended to every outbound webhook payload.
     * Allows merchants to verify that the event came from this platform.
     */
    private String webhookSecret;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = MerchantStatus.ACTIVE;
        }
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}