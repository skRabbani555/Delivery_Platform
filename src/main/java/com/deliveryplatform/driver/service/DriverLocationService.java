package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.driver.dto.UpdateLocationRequest;
import com.deliveryplatform.tracking.service.DriverLocationWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DriverLocationService {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private final StringRedisTemplate redisTemplate;
    private final DriverLocationWebSocketService webSocketService;
    private final DeliveryRepo deliveryRepo;

    public DriverLocationService(
            StringRedisTemplate redisTemplate,
            DriverLocationWebSocketService webSocketService,
            DeliveryRepo deliveryRepo) {

        this.redisTemplate = redisTemplate;
        this.webSocketService = webSocketService;
        this.deliveryRepo = deliveryRepo;
    }

    public void updateLocation(
            Long driverId,
            Double latitude,
            Double longitude) {
        updateLocation(driverId, new UpdateLocationRequest(latitude, longitude));
    }

    public void updateLocation(
            Long driverId,
            UpdateLocationRequest request) {

        if (driverId == null || request == null) {
            return;
        }

        Double latitude = request.latitude();
        Double longitude = request.longitude();

        if (latitude == null || longitude == null
                || latitude < -90.0 || latitude > 90.0
                || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Invalid GPS coordinates: [" + latitude + ", " + longitude + "]");
        }

        String key = "driver:" + driverId + ":location";

        Map<String, String> location = new HashMap<>();
        location.put("latitude", latitude.toString());
        location.put("longitude", longitude.toString());
        location.put("timestamp", Instant.now().toString());

        if (request.heading() != null) {
            location.put("heading", request.heading().toString());
        }
        if (request.speed() != null) {
            location.put("speed", request.speed().toString());
        }
        if (request.accuracy() != null) {
            location.put("accuracy", request.accuracy().toString());
        }

        redisTemplate.opsForHash().putAll(key, location);

        // Geospatial indexing in Redis for fast radial/proximity searches
        try {
            redisTemplate.opsForGeo().add("drivers:geo", new Point(longitude, latitude), driverId.toString());
        } catch (Exception e) {
            log.warn("Could not index driver {} geo position in Redis: {}", driverId, e.getMessage());
        }

        // Broadcast to WebSocket driver channel
        webSocketService.broadcastDriverLocation(
                driverId,
                latitude,
                longitude);

        // Broadcast to active delivery channel
        deliveryRepo.findByDriverId(driverId)
                .stream()
                .filter(delivery -> delivery.getStatus() != null)
                .filter(delivery -> !delivery.getStatus().name().equals("DELIVERED"))
                .filter(delivery -> !delivery.getStatus().name().equals("CANCELLED"))
                .filter(delivery -> !delivery.getStatus().name().equals("FAILED"))
                .findFirst()
                .ifPresent(delivery -> webSocketService.broadcastDeliveryLocation(
                        delivery.getId(),
                        driverId,
                        latitude,
                        longitude));
    }

    public Map<Object, Object> getLocation(Long driverId) {
        String key = "driver:" + driverId + ":location";
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Compute Haversine distance in meters between two lat/lon points.
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Check if two points are within maxDistanceMeters (geofence check).
     */
    public static boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double maxDistanceMeters) {
        return calculateDistanceMeters(lat1, lon1, lat2, lon2) <= maxDistanceMeters;
    }
}