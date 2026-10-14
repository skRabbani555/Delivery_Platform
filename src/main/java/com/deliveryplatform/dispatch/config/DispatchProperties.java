package com.deliveryplatform.dispatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dispatch")
public class DispatchProperties {

    /**
     * Maximum straight-line search radius for candidate drivers in kilometers.
     */
    private double maxSearchRadiusKm = 20.0;

    /**
     * Timeout in seconds for an assigned driver to accept the delivery before auto-unassigning and re-dispatching.
     */
    private long assignmentTimeoutSeconds = 45;

    /**
     * Background periodic sweep interval in milliseconds for deliveries searching for drivers.
     */
    private long retryIntervalMs = 5000;

    /**
     * Base urban speed in km/h for delivery vehicles (motorbikes / city cars).
     */
    private double urbanBaseSpeedKmh = 28.0;

    /**
     * Road detour / winding multiplier (straight-line distance to actual road distance ratio).
     */
    private double roadDetourFactor = 1.35;

    /**
     * Prep and arrival buffer time in minutes (parking, walk-in, vehicle start).
     */
    private double pickupBufferMinutes = 2.0;

    /**
     * Optional external OSRM routing server URL (e.g. http://router.project-osrm.org).
     */
    private String osrmUrl = "";

    /**
     * Enable external OSRM routing service instead of local dynamic traffic model.
     */
    private boolean osrmEnabled = false;

    public double getMaxSearchRadiusKm() {
        return maxSearchRadiusKm;
    }

    public void setMaxSearchRadiusKm(double maxSearchRadiusKm) {
        this.maxSearchRadiusKm = maxSearchRadiusKm;
    }

    public long getAssignmentTimeoutSeconds() {
        return assignmentTimeoutSeconds;
    }

    public void setAssignmentTimeoutSeconds(long assignmentTimeoutSeconds) {
        this.assignmentTimeoutSeconds = assignmentTimeoutSeconds;
    }

    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public double getUrbanBaseSpeedKmh() {
        return urbanBaseSpeedKmh;
    }

    public void setUrbanBaseSpeedKmh(double urbanBaseSpeedKmh) {
        this.urbanBaseSpeedKmh = urbanBaseSpeedKmh;
    }

    public double getRoadDetourFactor() {
        return roadDetourFactor;
    }

    public void setRoadDetourFactor(double roadDetourFactor) {
        this.roadDetourFactor = roadDetourFactor;
    }

    public double getPickupBufferMinutes() {
        return pickupBufferMinutes;
    }

    public void setPickupBufferMinutes(double pickupBufferMinutes) {
        this.pickupBufferMinutes = pickupBufferMinutes;
    }

    public String getOsrmUrl() {
        return osrmUrl;
    }

    public void setOsrmUrl(String osrmUrl) {
        this.osrmUrl = osrmUrl;
    }

    public boolean isOsrmEnabled() {
        return osrmEnabled;
    }

    public void setOsrmEnabled(boolean osrmEnabled) {
        this.osrmEnabled = osrmEnabled;
    }
}
