package com.deliveryplatform.driver.controller;

import com.deliveryplatform.driver.dto.DriverRequest;
import com.deliveryplatform.driver.dto.DriverResponse;
import com.deliveryplatform.driver.dto.UpdateLocationRequest;
import com.deliveryplatform.driver.entity.DriverStatus;
import com.deliveryplatform.driver.service.DriverLocationService;
import com.deliveryplatform.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;
    private final DriverLocationService driverLocationService;

    public DriverController(
            DriverService driverService,
            DriverLocationService driverLocationService) {
        this.driverService = driverService;
        this.driverLocationService = driverLocationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse createDriver(
            @Valid @RequestBody DriverRequest request) {

        return driverService.createDriver(request);
    }

    @GetMapping("/{id}")
    public DriverResponse getDriver(@PathVariable Long id) {
        return driverService.getDriver(id);
    }

    @GetMapping
    public List<DriverResponse> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @PatchMapping("/{id}/status")
    public DriverResponse updateStatus(
            @PathVariable Long id,
            @RequestParam DriverStatus status) {

        return driverService.updateStatus(id, status);
    }

    @PatchMapping("/{id}/location")
    public DriverResponse updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLocationRequest request) {

        return driverService.updateLocation(id, request);
    }

    @GetMapping("/{id}/location")
    public Map<Object, Object> getDriverLocation(@PathVariable Long id) {
        return driverLocationService.getLocation(id);
    }
}