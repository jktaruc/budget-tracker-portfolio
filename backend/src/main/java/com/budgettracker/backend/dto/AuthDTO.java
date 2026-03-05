package com.budgettracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTO {

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password
    ) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String name
    ) {}

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {}
}
