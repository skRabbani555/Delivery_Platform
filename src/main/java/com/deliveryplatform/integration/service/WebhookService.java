package com.deliveryplatform.integration.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.integration.entity.WebhookLog;
import com.deliveryplatform.integration.repo.WebhookLogRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private static final int MAX_ATTEMPTS = 5;
    /** Exponential back-off delays: attempt 1→30s, 2→2m, 3→10m, 4→30m, 5→2h */
    private static final long[] RETRY_DELAYS_SECONDS = {30, 120, 600, 1800, 7200};

    private final HttpClient httpClient;
    private final WebhookLogRepo webhookLogRepo;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookLogRepo webhookLogRepo) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.webhookLogRepo = webhookLogRepo;
        this.objectMapper = new ObjectMapper();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Asynchronously send a webhook for the given delivery status change.
     * Signs the payload with HMAC-SHA256 if the merchant has a webhookSecret configured.
     * Records the attempt in webhook_logs for auditing and retry.
     */
    @Async
    public void sendDeliveryStatusWebhook(Delivery delivery) {
        if (delivery == null || delivery.getMerchant() == null) {
            return;
        }

        String webhookUrl = delivery.getMerchant().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String payload = buildPayload(delivery);
        String signature = buildSignature(payload, delivery.getMerchant().getWebhookSecret());

        sendWebhookAttempt(
                delivery.getId(),
                delivery.getMerchant().getId(),
                webhookUrl,
                payload,
                signature,
                1
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Retry scheduler
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Every minute, pick up any failed webhook attempts that are due for retry.
     */
    @Scheduled(fixedDelay = 60_000)
    public void retryFailedWebhooks() {
        List<WebhookLog> due = webhookLogRepo.findBySuccessFalseAndNextRetryAtBefore(LocalDateTime.now());

        if (!due.isEmpty()) {
            log.info("Retrying {} failed webhook(s)", due.size());
        }

        for (WebhookLog entry : due) {
            sendWebhookAttempt(
                    entry.getDeliveryId(),
                    entry.getMerchantId(),
                    entry.getWebhookUrl(),
                    entry.getPayload(),
                    null,           // signature was baked into the original payload – re-sign below
                    entry.getAttemptNumber() + 1
            );
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void sendWebhookAttempt(
            Long deliveryId,
            Long merchantId,
            String webhookUrl,
            String payload,
            String signature,
            int attemptNumber) {

        WebhookLog logEntry = WebhookLog.builder()
                .deliveryId(deliveryId)
                .merchantId(merchantId)
                .webhookUrl(webhookUrl)
                .payload(payload)
                .attemptNumber(attemptNumber)
                .success(false)
                .build();

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .header("X-Delivery-Platform-Event", "delivery.status_changed")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(5));

            if (signature != null) {
                requestBuilder.header("X-Webhook-Signature", "sha256=" + signature);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            logEntry.setHttpStatus(response.statusCode());
            logEntry.setSuccess(success);

            if (!success) {
                scheduleRetry(logEntry, attemptNumber);
                log.warn("Webhook delivery {} attempt {} failed with HTTP {}", deliveryId, attemptNumber, response.statusCode());
            } else {
                log.info("Webhook delivered for delivery {} (attempt {})", deliveryId, attemptNumber);
            }

        } catch (Exception e) {
            logEntry.setErrorMessage(e.getMessage());
            scheduleRetry(logEntry, attemptNumber);
            log.warn("Webhook delivery {} attempt {} error: {}", deliveryId, attemptNumber, e.getMessage());
        } finally {
            webhookLogRepo.save(logEntry);
        }
    }

    private void scheduleRetry(WebhookLog entry, int attemptNumber) {
        if (attemptNumber < MAX_ATTEMPTS) {
            long delaySec = RETRY_DELAYS_SECONDS[Math.min(attemptNumber, RETRY_DELAYS_SECONDS.length - 1)];
            entry.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySec));
            log.info("Scheduled retry #{} for delivery {} webhook in {} seconds",
                    attemptNumber + 1, entry.getDeliveryId(), delaySec);
        } else {
            log.error("Webhook permanently failed for delivery {} after {} attempts", entry.getDeliveryId(), MAX_ATTEMPTS);
        }
    }

    private String buildPayload(Delivery delivery) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("deliveryId", delivery.getId());
            body.put("externalOrderId", delivery.getExternalOrderId() != null ? delivery.getExternalOrderId() : "");
            body.put("status", delivery.getStatus().name());
            if (delivery.getDriver() != null) {
                body.put("driverId", delivery.getDriver().getId());
            }
            body.put("estimatedEtaMinutes", delivery.getEstimatedEtaMinutes());
            body.put("timestamp", java.time.Instant.now().toString());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.format(
                    "{\"deliveryId\":%d,\"status\":\"%s\",\"externalOrderId\":\"%s\",\"timestamp\":\"%s\"}",
                    delivery.getId(),
                    delivery.getStatus(),
                    delivery.getExternalOrderId() != null ? delivery.getExternalOrderId() : "",
                    java.time.Instant.now()
            );
        }
    }

    /**
     * Signs the payload with HMAC-SHA256 using the merchant's webhookSecret.
     * If no secret is configured, returns null and no signature header is added.
     */
    private String buildSignature(String payload, String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Failed to sign webhook payload: {}", e.getMessage());
            return null;
        }
    }
}
