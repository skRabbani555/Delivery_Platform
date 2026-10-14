package com.deliveryplatform.delivery.repo;

import com.deliveryplatform.delivery.entity.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepo extends JpaRepository<DeliveryEvent, Long> {

    List<DeliveryEvent> findByDeliveryIdOrderByCreatedAtAsc(Long deliveryId);
}