package com.orderflow.order.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record TokenResponse(String token) {}
}
