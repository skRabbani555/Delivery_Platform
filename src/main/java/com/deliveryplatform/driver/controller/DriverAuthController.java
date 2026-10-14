package com.deliveryplatform.driver.controller;

import com.deliveryplatform.driver.dto.DriverLoginRequest;
import com.deliveryplatform.driver.dto.DriverLoginResponse;
import com.deliveryplatform.driver.service.DriverAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver/auth")
public class DriverAuthController {

    private final DriverAuthService driverAuthService;

    public DriverAuthController(
            DriverAuthService driverAuthService) {

        this.driverAuthService = driverAuthService;
    }

    @PostMapping("/login")
    public DriverLoginResponse login(
            @Valid @RequestBody DriverLoginRequest request) {

        return driverAuthService.login(request);
    }
}