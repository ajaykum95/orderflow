package com.orderflow.order.auth;

import jakarta.persistence.*;

// @Entity = यह class database की एक table बन जाएगी
@Entity
@Table(name = "users")   // "user" शब्द Postgres में reserved है, इसलिए "users"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id अपने-आप बढ़ेगी: 1, 2, 3...
    private Long id;

    @Column(unique = true, nullable = false)   // एक email से दो user नहीं
    private String email;

    @Column(nullable = false)
    private String passwordHash;   // हमेशा hash रखते हैं, असली password नहीं

    @Enumerated(EnumType.STRING)   // database में "CUSTOMER" / "ADMIN" लिखा जाएगा
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    // JPA को खाली constructor चाहिए
    protected User() {}

    public User(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
