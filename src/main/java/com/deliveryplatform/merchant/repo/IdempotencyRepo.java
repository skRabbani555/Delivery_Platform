package com.deliveryplatform.merchant.repo;

import com.deliveryplatform.merchant.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepo extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);
}
