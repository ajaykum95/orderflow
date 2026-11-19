package com.orderflow.order.auth;

import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record TokenResponse(String token) {}
}
