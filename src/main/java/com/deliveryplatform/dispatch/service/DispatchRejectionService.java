package com.deliveryplatform.dispatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DispatchRejectionService {

    private static final Logger log = LoggerFactory.getLogger(DispatchRejectionService.class);
    private static final Duration REJECTION_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final Map<Long, Set<Long>> localRejectionCache = new ConcurrentHashMap<>();

    public DispatchRejectionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Record that a driver rejected or timed out on a delivery offer.
     */
    public void recordRejection(Long deliveryId, Long driverId) {
        if (deliveryId == null || driverId == null) {
            return;
        }

        // Always update local cache
        localRejectionCache.computeIfAbsent(deliveryId, k -> ConcurrentHashMap.newKeySet()).add(driverId);

        // Update Redis with TTL
        try {
            String key = formatKey(deliveryId);
            redisTemplate.opsForSet().add(key, driverId.toString());
            redisTemplate.expire(key, REJECTION_TTL);
            log.info("Recorded rejection: driver {} rejected delivery {}", driverId, deliveryId);
        } catch (Exception e) {
            log.warn("Redis unavailable for rejection recording: {}", e.getMessage());
        }
    }

    /**
     * Check whether a driver has already rejected the specified delivery.
     */
    public boolean isDriverRejected(Long deliveryId, Long driverId) {
        if (deliveryId == null || driverId == null) {
            return false;
        }

        // Check local cache first
        Set<Long> localSet = localRejectionCache.get(deliveryId);
        if (localSet != null && localSet.contains(driverId)) {
            return true;
        }

        // Check Redis
        try {
            String key = formatKey(deliveryId);
            Boolean isMember = redisTemplate.opsForSet().isMember(key, driverId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.warn("Redis unavailable for rejection check: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get all driver IDs who have rejected the specified delivery.
     */
    public Set<Long> getRejectedDriverIds(Long deliveryId) {
        if (deliveryId == null) {
            return Collections.emptySet();
        }

        Set<Long> result = ConcurrentHashMap.newKeySet();

        Set<Long> localSet = localRejectionCache.get(deliveryId);
        if (localSet != null) {
            result.addAll(localSet);
        }

        try {
            String key = formatKey(deliveryId);
            Set<String> redisMembers = redisTemplate.opsForSet().members(key);
            if (redisMembers != null) {
                result.addAll(redisMembers.stream().map(Long::valueOf).collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for retrieving rejected drivers: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Clear rejections for a delivery (e.g., if re-opened or reset).
     */
    public void clearRejections(Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        localRejectionCache.remove(deliveryId);
        try {
            redisTemplate.delete(formatKey(deliveryId));
        } catch (Exception e) {
            log.warn("Redis unavailable for clearing rejections: {}", e.getMessage());
        }
    }

    private String formatKey(Long deliveryId) {
        return "delivery:" + deliveryId + ":rejected_drivers";
    }
}
