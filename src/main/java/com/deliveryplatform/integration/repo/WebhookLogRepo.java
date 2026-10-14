package com.deliveryplatform.integration.repo;

import com.deliveryplatform.integration.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WebhookLogRepo extends JpaRepository<WebhookLog, Long> {

    List<WebhookLog> findByDeliveryIdOrderByCreatedAtDesc(Long deliveryId);

    List<WebhookLog> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<WebhookLog> findBySuccessFalseAndNextRetryAtBefore(LocalDateTime now);
}
