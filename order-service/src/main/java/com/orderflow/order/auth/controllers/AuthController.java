package com.orderflow.order.auth.controllers;

import com.orderflow.order.auth.dtos.AuthDtos;
import com.orderflow.order.auth.services.AuthService;
import com.orderflow.order.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(201, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.TokenResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthDtos.TokenResponse token = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(200, "Login successful", token));
    }
}