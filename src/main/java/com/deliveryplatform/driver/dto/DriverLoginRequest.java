package com.deliveryplatform.driver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DriverLoginRequest(

        @Email @NotBlank String email,

        @NotBlank String password) {
}