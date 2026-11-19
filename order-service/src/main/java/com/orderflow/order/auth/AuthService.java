package com.orderflow.order.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {
    // TODO 1: UserRepository और PasswordEncoder inject करें
    // TODO 2: register(): email पहले से है? -> error. नहीं -> password hash करके save
    // TODO 3: login(): user ढूँढें, password match करें, JwtService से token बनाएँ
}
