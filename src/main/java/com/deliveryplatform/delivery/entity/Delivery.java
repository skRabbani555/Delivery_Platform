package com.deliveryplatform.delivery.entity;

import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.merchant.entity.Merchant;
import com.deliveryplatform.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_external_order", columnNames = { "merchant_id", "external_order_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(nullable = false)
    private String externalOrderId;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private String dropAddress;

    @Column(nullable = false)
    private Double dropLatitude;

    @Column(nullable = false)
    private Double dropLongitude;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    private Double packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double deliveryFee;

    @Column(nullable = false)
    private Double driverPayout;

    @Column(nullable = false)
    private Double platformFee;

    private LocalDateTime updatedAt;

    private LocalDateTime actualPickupTime;

    private LocalDateTime actualDeliveryTime;

    private LocalDateTime assignedAt;

    private Double estimatedEtaMinutes;

    @Column(name = "delivery_otp", length = 10)
    private String deliveryOtp;

    @Column(name = "otp_verified")
    private Boolean otpVerified;

    @Column(name = "pod_image_url")
    private String proofOfDeliveryImageUrl;

    @Column(name = "pod_notes", length = 500)
    private String proofOfDeliveryNotes;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "pod_drop_lat")
    private Double podDropLatitude;

    @Column(name = "pod_drop_lng")
    private Double podDropLongitude;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;

        if (status == null) {
            status = DeliveryStatus.CREATED;
        }

        if (deliveryOtp == null) {
            deliveryOtp = String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 10000));
        }

        if (otpVerified == null) {
            otpVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Double getDriverPayout() {
        return driverPayout;
    }

    public void setDriverPayout(Double driverPayout) {
        this.driverPayout = driverPayout;
    }

    public Double getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(Double platformFee) {
        this.platformFee = platformFee;
    }

}