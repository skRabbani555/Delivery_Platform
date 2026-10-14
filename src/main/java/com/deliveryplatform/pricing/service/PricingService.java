package com.deliveryplatform.pricing.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.pricing.dto.PricingResponse;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private static final double BASE_FEE = 40.0;
    private static final double PER_KM_FEE = 10.0;
    private static final double DRIVER_PERCENTAGE = 0.80;

    public PricingResponse calculatePrice(Delivery delivery) {

        double distanceKm = calculateDistance(
                delivery.getPickupLatitude(),
                delivery.getPickupLongitude(),
                delivery.getDropLatitude(),
                delivery.getDropLongitude());

        double deliveryFee = BASE_FEE + (distanceKm * PER_KM_FEE);

        deliveryFee = Math.round(deliveryFee * 100.0) / 100.0;

        double driverPayout = Math.round(
                deliveryFee * DRIVER_PERCENTAGE * 100.0) / 100.0;

        double platformFee = Math.round(
                (deliveryFee - driverPayout) * 100.0) / 100.0;

        return new PricingResponse(
                Math.round(distanceKm * 100.0) / 100.0,
                deliveryFee,
                driverPayout,
                platformFee);
    }

    public void calculateAndApplyPricing(Delivery delivery) {
        PricingResponse pricing = calculatePrice(delivery);
        delivery.setDeliveryFee(pricing.deliveryFee());
        delivery.setDriverPayout(pricing.driverPayout());
        delivery.setPlatformFee(pricing.platformFee());
    }

    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double earthRadius = 6371.0;

        double latDistance = Math.toRadians(lat2 - lat1);

        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2)
                * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return earthRadius * c;
    }
}