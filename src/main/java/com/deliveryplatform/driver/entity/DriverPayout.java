package com.deliveryplatform.driver.entity;

import com.deliveryplatform.delivery.entity.Delivery;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double deliveryFee;

    @Column(nullable = false)
    private Double platformFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    private LocalDateTime paidAt;

    private String referenceNumber;

    @PrePersist
    public void prePersist() {
        if (earnedAt == null) {
            earnedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PayoutStatus.PENDING;
        }
    }
}
