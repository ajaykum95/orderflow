package com.orderflow.order.auth;

import org.springframework.stereotype.Service;

@Service
public class JwtService {
    // TODO 1: application.yml से secret पढ़ें (@Value)
    // TODO 2: generateToken(User) -> subject = email, claim "role"
    // TODO 3: extractEmail(token) और isValid(token)
}
