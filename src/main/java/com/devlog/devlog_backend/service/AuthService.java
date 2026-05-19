package com.devlog.devlog_backend.service;

import com.devlog.devlog_backend.dto.request.LoginRequest;
import com.devlog.devlog_backend.dto.request.RegisterRequest;
import com.devlog.devlog_backend.dto.response.AuthResponse;
import com.devlog.devlog_backend.entity.User;
import com.devlog.devlog_backend.repository.UserRepository;
import com.devlog.devlog_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                // WHY encode here, not in the entity: the service layer
                // owns business rules. The entity is just data.
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        // Generate token immediately so the user is "logged in" after registering
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }

    public AuthResponse login(LoginRequest request) {
        // WHY authenticationManager.authenticate(): this is Spring Security's
        // standard way to verify credentials. It calls our UserDetailsService
        // to load the user, then compares BCrypt hashes automatically.
        // If wrong, it throws BadCredentialsException — caught by GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here, credentials are valid
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }
}