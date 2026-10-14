package com.deliveryplatform.integration.controller;

import com.deliveryplatform.integration.entity.WebhookLog;
import com.deliveryplatform.integration.repo.WebhookLogRepo;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/webhook-logs")
public class WebhookLogController {

    private final WebhookLogRepo webhookLogRepo;

    public WebhookLogController(WebhookLogRepo webhookLogRepo) {
        this.webhookLogRepo = webhookLogRepo;
    }

    /** Get all webhook attempts for a specific delivery. */
    @GetMapping("/delivery/{deliveryId}")
    public List<WebhookLog> getByDelivery(@PathVariable Long deliveryId) {
        return webhookLogRepo.findByDeliveryIdOrderByCreatedAtDesc(deliveryId);
    }

    /** Get all webhook attempts for the authenticated merchant. */
    @GetMapping("/me")
    public List<WebhookLog> getMyWebhookLogs(Authentication authentication) {
        Long merchantId = (Long) authentication.getCredentials();
        return webhookLogRepo.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }
}
