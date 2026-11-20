package com.orderflow.order.auth.services.impl;

import com.orderflow.order.auth.dtos.AuthDtos;
import com.orderflow.order.auth.services.AuthService;
import com.orderflow.order.auth.entities.Role;
import com.orderflow.order.auth.entities.User;
import com.orderflow.order.auth.repositories.UserRepository;
import com.orderflow.order.auth.services.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public void register(AuthDtos.RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String hash = passwordEncoder.encode(request.password());

        userRepository.save(new User(request.email(), hash, Role.CUSTOMER));
    }

    @Override
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new AuthDtos.TokenResponse(jwtService.generateToken(user));
    }
}
