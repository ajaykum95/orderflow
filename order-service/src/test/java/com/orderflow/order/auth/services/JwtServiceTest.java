package com.orderflow.order.auth.services;

import com.orderflow.order.auth.entities.Role;
import com.orderflow.order.auth.entities.User;
import com.orderflow.order.auth.services.impl.JwtServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private final User user = new User("a@test.com", "hash", Role.CUSTOMER);

    @Test
    void generatedTokenIsValidAndCarriesEmailAndRole() {
        JwtService jwt = new JwtServiceImpl(SECRET, 60);
        String token = jwt.generateToken(user);

        assertThat(jwt.isValid(token)).isTrue();
        assertThat(jwt.extractEmail(token)).isEqualTo("a@test.com");
        assertThat(jwt.parse(token).get("role", String.class)).isEqualTo("CUSTOMER");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtServiceImpl(SECRET, -1);
        assertThat(jwt.isValid(jwt.generateToken(user))).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtServiceImpl(SECRET, 60);
        String token = jwt.generateToken(user);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThat(jwt.isValid(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtServiceImpl("another-secret-another-secret-another-123", 60).generateToken(user);
        assertThat(new JwtServiceImpl(SECRET, 60).isValid(token)).isFalse();
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        JwtService jwt = new JwtServiceImpl(SECRET, 60);
        assertThat(jwt.isValid("not.a.jwt")).isFalse();
        assertThat(jwt.isValid("")).isFalse();
        assertThat(jwt.isValid(null)).isFalse();
    }

    @Test
    void tooShortSecretIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtServiceImpl("short", 60)).isInstanceOf(RuntimeException.class);
    }
}
