package com.orderflow.order.auth.services;

import com.orderflow.order.auth.entities.User;
import io.jsonwebtoken.Claims;

public interface JwtService {
    String generateToken(User user);
    Claims parse(String token);
    String extractEmail(String token);
    boolean isValid(String token);
}
