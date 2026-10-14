package com.deliveryplatform.driver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DriverRequest(

        @NotBlank String name,

        @NotBlank @Email String email,

        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$") String phone,

        @NotBlank @Size(min = 8) String password

) {
}