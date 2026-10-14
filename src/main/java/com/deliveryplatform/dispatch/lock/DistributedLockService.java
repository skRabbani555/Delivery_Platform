package com.deliveryplatform.dispatch.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final StringRedisTemplate redisTemplate;

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    // In-memory fallback if Redis is down or in isolated test environments
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();
    private final Map<String, String> localTokens = new ConcurrentHashMap<>();

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Acquire a distributed lock on the specified key with an expiration lease.
     *
     * @param lockKey the lock identifier (e.g., "lock:delivery:123" or "lock:driver:456")
     * @param leaseDuration maximum time the lock remains valid if not released
     * @return unique lock token if acquired, or null if lock is currently held by another process
     */
    public String acquireLock(String lockKey, Duration leaseDuration) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, leaseDuration);
            if (Boolean.TRUE.equals(acquired)) {
                return token;
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis unavailable for lock {}, falling back to local lock: {}", lockKey, e.getMessage());
            return acquireLocalLock(lockKey, leaseDuration, token);
        }
    }

    /**
     * Release the distributed lock safely using token verification (Lua script).
     *
     * @param lockKey the lock identifier
     * @param token the lock token returned when acquiring
     * @return true if successfully released, false otherwise
     */
    public boolean releaseLock(String lockKey, String token) {
        if (token == null) {
            return false;
        }
        try {
            Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
            return result != null && result > 0;
        } catch (Exception e) {
            log.warn("Redis unavailable for unlocking {}, releasing local lock: {}", lockKey, e.getMessage());
            return releaseLocalLock(lockKey, token);
        }
    }

    /**
     * Executes a supplier function within a distributed lock.
     * Returns null if lock could not be acquired.
     */
    public <T> T executeWithLock(String lockKey, Duration leaseDuration, Supplier<T> task) {
        String token = acquireLock(lockKey, leaseDuration);
        if (token == null) {
            log.debug("Could not acquire lock for key: {}", lockKey);
            return null;
        }
        try {
            return task.get();
        } finally {
            releaseLock(lockKey, token);
        }
    }

    /**
     * Executes a runnable within a distributed lock.
     * Returns true if executed, false if lock could not be acquired.
     */
    public boolean executeWithLock(String lockKey, Duration leaseDuration, Runnable task) {
        String token = acquireLock(lockKey, leaseDuration);
        if (token == null) {
            log.debug("Could not acquire lock for key: {}", lockKey);
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            releaseLock(lockKey, token);
        }
    }

    private synchronized String acquireLocalLock(String lockKey, Duration leaseDuration, String token) {
        ReentrantLock lock = localLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if (acquired) {
                localTokens.put(lockKey, token);
                return token;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private synchronized boolean releaseLocalLock(String lockKey, String token) {
        ReentrantLock lock = localLocks.get(lockKey);
        String currentToken = localTokens.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread() && token.equals(currentToken)) {
            localTokens.remove(lockKey);
            lock.unlock();
            return true;
        }
        return false;
    }
}
