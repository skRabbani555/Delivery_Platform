package com.deliveryplatform.driver.service;

import com.deliveryplatform.auth.service.JwtService;
import com.deliveryplatform.driver.dto.DriverLoginRequest;
import com.deliveryplatform.driver.dto.DriverLoginResponse;
import com.deliveryplatform.driver.entity.Driver;
import com.deliveryplatform.driver.repo.DriverRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DriverAuthService {

    private final DriverRepo driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public DriverAuthService(
            DriverRepo driverRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public DriverLoginResponse login(DriverLoginRequest request) {

        Driver driver = driverRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!driver.getActive()) {
            throw new RuntimeException("Driver account is inactive");
        }

        if (!passwordEncoder.matches(
                request.password(),
                driver.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                driver.getEmail(),
                "DRIVER",
                driver.getId());

        return new DriverLoginResponse(token, "Bearer");
    }
}