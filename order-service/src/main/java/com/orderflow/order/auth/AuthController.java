package com.orderflow.order.auth;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public void register(@RequestBody AuthDtos.RegisterRequest registerRequest) {
        System.out.println("Request received : " + registerRequest);
    }
    // TODO: POST /api/auth/register  -> RegisterRequest
    // TODO: POST /api/auth/login     -> LoginRequest, TokenResponse लौटाएँ
}
