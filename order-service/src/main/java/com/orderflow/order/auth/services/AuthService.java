package com.orderflow.order.auth.services;

import com.orderflow.order.auth.dtos.AuthDtos;
import jakarta.validation.Valid;

public interface AuthService {
    void register(AuthDtos.@Valid RegisterRequest req);
    AuthDtos.TokenResponse login(AuthDtos.LoginRequest request);
}
