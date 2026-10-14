package com.deliveryplatform.tracking.service;

import com.deliveryplatform.tracking.dto.DriverLocationMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DriverLocationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public DriverLocationWebSocketService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastDriverLocation(
            Long driverId,
            Double latitude,
            Double longitude) {

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Instant.now().toString());

        messagingTemplate.convertAndSend(
                "/topic/drivers/" + driverId + "/location",
                message);
    }

    public void broadcastDeliveryLocation(
            Long deliveryId,
            Long driverId,
            Double latitude,
            Double longitude) {

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Instant.now().toString());

        messagingTemplate.convertAndSend(
                "/topic/deliveries/" + deliveryId + "/location",
                message);
    }
}