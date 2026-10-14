package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.driver.entity.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DriverNotificationServiceImpl implements DriverNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DriverNotificationServiceImpl.class);

    @Override
    public void notifyJobAssigned(Driver driver, Delivery delivery) {
        String token = driver.getDeviceToken();
        log.info("[PUSH NOTIFICATION] Job Assigned - Driver: #{} ({}) | Token: {} | Delivery: #{} | Store: {} | Fee Payout: ${}",
                driver.getId(), driver.getName(),
                token != null ? token : "NO_TOKEN_REGISTERED",
                delivery.getId(),
                delivery.getPickupAddress(),
                delivery.getDriverPayout());

        if (token != null && !token.isBlank()) {
            // Push notification payload is pre-formatted for FCM/APNs provider
            dispatchPush(token, "New Delivery Offer!",
                    "You have a new delivery request from " + delivery.getPickupAddress() + ". Tap to accept.",
                    delivery.getId());
        }
    }

    @Override
    public void notifyJobCancelled(Driver driver, Delivery delivery) {
        String token = driver.getDeviceToken();
        log.info("[PUSH NOTIFICATION] Job Cancelled - Driver: #{} | Delivery: #{}",
                driver.getId(), delivery.getId());

        if (token != null && !token.isBlank()) {
            dispatchPush(token, "Delivery Cancelled",
                    "Delivery #" + delivery.getId() + " has been cancelled.",
                    delivery.getId());
        }
    }

    @Override
    public void notifyGeneral(Long driverId, String title, String message) {
        log.info("[PUSH NOTIFICATION] General - Driver: #{} | Title: {} | Message: {}",
                driverId, title, message);
    }

    private void dispatchPush(String token, String title, String body, Long deliveryId) {
        // Ready for Firebase Cloud Messaging / OneSignal SDK integration
        log.debug("Dispatched push message to device token [{}]: {} - {} (Delivery ID: {})",
                token, title, body, deliveryId);
    }
}
