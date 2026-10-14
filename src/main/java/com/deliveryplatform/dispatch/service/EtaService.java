package com.deliveryplatform.dispatch.service;

import com.deliveryplatform.dispatch.config.DispatchProperties;
import com.deliveryplatform.driver.entity.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Service
public class EtaService {

    private static final Logger log = LoggerFactory.getLogger(EtaService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DispatchProperties properties;
    private final RestClient restClient;

    public EtaService(DispatchProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * Represents a driver candidate scored by ETA and straight-line distance.
     */
    public record DriverCandidateScore(
            Driver driver,
            double etaMinutes,
            double distanceKm
    ) implements Comparable<DriverCandidateScore> {
        @Override
        public int compareTo(DriverCandidateScore other) {
            int etaCmp = Double.compare(this.etaMinutes, other.etaMinutes);
            if (etaCmp != 0) {
                return etaCmp;
            }
            return Double.compare(this.distanceKm, other.distanceKm);
        }
    }

    /**
     * Score a driver candidate based on pickup coordinates and current traffic conditions.
     */
    public DriverCandidateScore scoreDriver(Driver driver, Double pickupLat, Double pickupLon) {
        double distanceKm = calculateDistanceKm(
                driver.getLatitude(),
                driver.getLongitude(),
                pickupLat,
                pickupLon
        );

        double etaMinutes = calculateEtaMinutes(
                driver.getLatitude(),
                driver.getLongitude(),
                pickupLat,
                pickupLon,
                LocalDateTime.now()
        );

        return new DriverCandidateScore(driver, etaMinutes, distanceKm);
    }

    /**
     * Calculate estimated time of arrival (in minutes) from driver location to pickup.
     */
    public double calculateEtaMinutes(
            double driverLat,
            double driverLon,
            double pickupLat,
            double pickupLon,
            LocalDateTime dispatchTime) {

        // Try external OSRM routing if enabled
        if (properties.isOsrmEnabled() && properties.getOsrmUrl() != null && !properties.getOsrmUrl().isBlank()) {
            Double osrmEta = fetchOsrmEta(driverLat, driverLon, pickupLat, pickupLon);
            if (osrmEta != null) {
                return roundToOneDecimal(osrmEta + properties.getPickupBufferMinutes());
            }
        }

        // Local high-fidelity traffic & road detour model
        double straightLineKm = calculateDistanceKm(driverLat, driverLon, pickupLat, pickupLon);
        double roadDistanceKm = straightLineKm * properties.getRoadDetourFactor();

        double trafficMultiplier = calculateTrafficMultiplier(dispatchTime.toLocalTime());
        double effectiveSpeedKmh = properties.getUrbanBaseSpeedKmh() / trafficMultiplier;

        if (effectiveSpeedKmh <= 0) {
            effectiveSpeedKmh = 10.0;
        }

        double travelTimeMinutes = (roadDistanceKm / effectiveSpeedKmh) * 60.0;
        double totalEtaMinutes = travelTimeMinutes + properties.getPickupBufferMinutes();

        return Math.max(1.0, roundToOneDecimal(totalEtaMinutes));
    }

    /**
     * Calculate straight-line Haversine distance between two coordinates in kilometers.
     */
    public double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return roundToOneDecimal(EARTH_RADIUS_KM * c);
    }

    /**
     * Dynamic traffic multiplier based on hour of the day.
     * Higher multiplier = heavier traffic = lower effective vehicle speed.
     */
    public double calculateTrafficMultiplier(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        // Morning rush hour (08:00 - 10:30)
        if ((hour >= 8 && hour < 10) || (hour == 10 && minute <= 30)) {
            return 1.40;
        }

        // Lunch rush hour (12:00 - 14:00)
        if (hour >= 12 && hour < 14) {
            return 1.25;
        }

        // Evening rush hour (17:00 - 20:30)
        if ((hour >= 17 && hour < 20) || (hour == 20 && minute <= 30)) {
            return 1.50;
        }

        // Late night light traffic (23:00 - 06:00)
        if (hour >= 23 || hour < 6) {
            return 0.85;
        }

        // Normal regular daytime
        return 1.00;
    }

    /**
     * Optional external routing query to an OSRM instance.
     */
    private Double fetchOsrmEta(double driverLat, double driverLon, double pickupLat, double pickupLon) {
        try {
            String uri = String.format("%s/route/v1/driving/%f,%f;%f,%f?overview=false",
                    properties.getOsrmUrl().replaceAll("/+$", ""),
                    driverLon, driverLat,
                    pickupLon, pickupLat);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response != null && "Ok".equals(response.get("code"))) {
                @SuppressWarnings("unchecked")
                var routes = (java.util.List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Number durationSeconds = (Number) routes.get(0).get("duration");
                    if (durationSeconds != null) {
                        return durationSeconds.doubleValue() / 60.0;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("OSRM routing request failed: {}, falling back to local ETA model", e.getMessage());
        }
        return null;
    }

    private double roundToOneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
