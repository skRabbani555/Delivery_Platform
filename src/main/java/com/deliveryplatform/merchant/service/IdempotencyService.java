package com.deliveryplatform.merchant.service;

import com.deliveryplatform.merchant.entity.IdempotencyRecord;
import com.deliveryplatform.merchant.repo.IdempotencyRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRepo idempotencyRepo;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepo idempotencyRepo) {
        this.idempotencyRepo = idempotencyRepo;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Execute the given supplier exactly once per (merchantId, idempotencyKey) combination.
     *
     * <p>If the same key is submitted again within the record's lifetime, the original
     * response body is replayed without re-executing the business logic.
     *
     * @param merchantId       the authenticated merchant
     * @param idempotencyKey   the key from the Idempotency-Key HTTP header
     * @param responseType     the Java type to deserialise the stored response into
     * @param action           the real business action to run on first call
     * @return the original or replayed response
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T executeIdempotently(
            Long merchantId,
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> action) {

        Optional<IdempotencyRecord> existing =
                idempotencyRepo.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            log.info("Replaying idempotent response for merchant {} key {}", merchantId, idempotencyKey);
            try {
                return objectMapper.readValue(record.getResponseBody(), responseType);
            } catch (Exception e) {
                log.warn("Failed to replay idempotent response: {}", e.getMessage());
                throw new RuntimeException("Duplicate request replay failed: " + e.getMessage());
            }
        }

        // First call – run the action and persist the result
        T result = action.get();

        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .merchantId(merchantId)
                    .idempotencyKey(idempotencyKey)
                    .responseBody(objectMapper.writeValueAsString(result))
                    .httpStatus(201)
                    .build();

            idempotencyRepo.save(record);
        } catch (Exception e) {
            log.warn("Failed to persist idempotency record for key {}: {}", idempotencyKey, e.getMessage());
        }

        return result;
    }
}
