package com.orderflow.order.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Password को hash करने वाला tool (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // "कौन सा URL किसके लिए खुला है" का नियम
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF cookie वाले login के लिए होता है; हम JWT इस्तेमाल करेंगे, इसलिए बंद
            .csrf(AbstractHttpConfigurer::disable)

            // Server कोई session याद नहीं रखेगा; हर request में token आएगा
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()          // register/login सबके लिए
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll() // products कोई भी देख सके
                .requestMatchers("/api/admin/**").hasRole("ADMIN")    // सिर्फ़ ADMIN
                .anyRequest().authenticated()                         // बाकी सब पर login ज़रूरी
            )
            .httpBasic(Customizer.withDefaults());

        // TODO (JwtService के बाद): यहाँ JWT filter जोड़ेंगे जो token जाँचे
        return http.build();
    }
}
