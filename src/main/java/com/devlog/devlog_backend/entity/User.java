package com.devlog.devlog_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

// WHY @Entity: tells Hibernate "this class = a database table"
// WHY implements UserDetails: Spring Security requires this interface
// to understand how to load and authenticate a user.
@Entity
@Table(name = "users")
@Data                    // Lombok: generates getters, setters, toString, equals, hashCode
@Builder                 // Lombok: lets you do User.builder().email("...").build()
@NoArgsConstructor       // Lombok: generates empty constructor (required by JPA)
@AllArgsConstructor      // Lombok: generates constructor with all fields (used by @Builder)
public class User implements UserDetails {

    // WHY @GeneratedValue IDENTITY: MySQL auto-increments the ID for us
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WHY unique=true: the database enforces that no two users share an email
    @Column(nullable = false, unique = true)
    private String email;

    // WHY 'passwordHash' not 'password': we never store plain text.
    // BCrypt will hash it before saving.
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private String timezone = "UTC";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // WHY @PrePersist: this method runs automatically just before
    // Hibernate INSERTs this entity. It sets createdAt without us
    // having to remember to set it every time.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── UserDetails methods ────────────────────────────────────────────
    // WHY: Spring Security calls these to check if a user is valid.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // For now every user is ROLE_USER. Later you can add ROLE_ADMIN.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email; // We use email as the unique identifier
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}