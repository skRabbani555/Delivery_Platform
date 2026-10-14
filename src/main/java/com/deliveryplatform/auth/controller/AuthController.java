package com.deliveryplatform.auth.controller;

import com.deliveryplatform.auth.dto.LoginRequest;
import com.deliveryplatform.auth.dto.LoginResponse;
import com.deliveryplatform.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}