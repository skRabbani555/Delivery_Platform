package com.deliveryplatform.driver.service;

import com.deliveryplatform.delivery.entity.Delivery;
import com.deliveryplatform.delivery.entity.DeliveryStatus;
import com.deliveryplatform.delivery.repo.DeliveryRepo;
import com.deliveryplatform.dispatch.service.DispatchService;
import com.deliveryplatform.driver.dto.DriverRequest;
import com.deliveryplatform.driver.dto.DriverResponse;
import com.deliveryplatform.driver.dto.UpdateLocationRequest;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.repo.DriverRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepo driverRepo;
    private final DriverLocationService driverLocationService;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryRepo deliveryRepo;
    private final DispatchService dispatchService;

    public DriverService(
            DriverRepo driverRepo,
            DriverLocationService driverLocationService,
            PasswordEncoder passwordEncoder,
            DeliveryRepo deliveryRepo,
            DispatchService dispatchService) {
        this.driverRepo = driverRepo;
        this.driverLocationService = driverLocationService;
        this.passwordEncoder = passwordEncoder;
        this.deliveryRepo = deliveryRepo;
        this.dispatchService = dispatchService;
    }

    public DriverResponse createDriver(DriverRequest request) {

        if (driverRepo.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Driver email already exists");
        }

        Driver driver = new Driver();

        driver.setName(request.name());
        driver.setEmail(request.email());
        driver.setPhone(request.phone());

        // Temporary for MVP.
        driver.setPassword(passwordEncoder.encode(request.password()));

        driver.setStatus(DriverStatus.OFFLINE);
        driver.setActive(true);

        return toResponse(driverRepo.save(driver));
    }

    public DriverResponse getDriver(Long id) {

        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        return toResponse(driver);
    }

    public List<DriverResponse> getAllDrivers() {

        return driverRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DriverResponse updateStatus(Long id, DriverStatus status) {

        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new RuntimeException("Driver account is deactivated");
        }

        if (status == DriverStatus.AVAILABLE) {
            // Prevent going AVAILABLE if driver currently has an active delivery
            if (dispatchService.hasActiveDelivery(id)) {
                throw new RuntimeException(
                        "Cannot switch status to AVAILABLE while assigned to an active delivery. Please finish your active delivery first.");
            }
        } else if (status == DriverStatus.OFFLINE) {
            // Check if driver has an active delivery in transit
            List<Delivery> activeInTransit = deliveryRepo.findByDriverIdAndStatusNotIn(
                    id,
                    List.of(
                            DeliveryStatus.DELIVERED,
                            DeliveryStatus.CANCELLED,
                            DeliveryStatus.FAILED,
                            DeliveryStatus.DRIVER_ASSIGNED,
                            DeliveryStatus.CREATED,
                            DeliveryStatus.SEARCHING_DRIVER
                    )
            );

            if (!activeInTransit.isEmpty()) {
                throw new RuntimeException(
                        "Cannot go OFFLINE while in-transit for active delivery #"
                                + activeInTransit.get(0).getId());
            }

            // If driver was assigned a delivery awaiting acceptance, handle becoming unavailable
            List<Delivery> assignedDeliveries = deliveryRepo.findByDriverIdAndStatus(
                    id,
                    DeliveryStatus.DRIVER_ASSIGNED
            );

            for (Delivery assigned : assignedDeliveries) {
                dispatchService.handleDriverUnavailable(id, assigned.getId());
            }
        }

        driver.setStatus(status);
        Driver saved = driverRepo.save(driver);

        // If driver became AVAILABLE, trigger pending delivery sweep immediately
        if (status == DriverStatus.AVAILABLE) {
            dispatchService.triggerPendingDispatchAsync();
        }

        return toResponse(saved);
    }

    @Transactional
    public void updateDeviceToken(Long id, String deviceToken) {
        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setDeviceToken(deviceToken);
        driverRepo.save(driver);
    }

    public DriverResponse updateLocation(
            Long id,
            Double latitude,
            Double longitude) {
        return updateLocation(id, new UpdateLocationRequest(latitude, longitude));
    }

    @Transactional
    public DriverResponse updateLocation(
            Long id,
            UpdateLocationRequest request) {

        Driver driver = driverRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        driverLocationService.updateLocation(id, request);

        return toResponse(driverRepo.save(driver));
    }

    private DriverResponse toResponse(Driver driver) {

        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getStatus(),
                driver.getLatitude(),
                driver.getLongitude(),
                driver.getActive());
    }
}