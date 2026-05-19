package com.devlog.devlog_backend.controller;

import com.devlog.devlog_backend.dto.request.LoginRequest;
import com.devlog.devlog_backend.dto.request.RegisterRequest;
import com.devlog.devlog_backend.dto.response.ApiResponse;
import com.devlog.devlog_backend.dto.response.AuthResponse;
import com.devlog.devlog_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// WHY @RequestMapping("/api/auth"): groups all auth endpoints under this prefix.
// SecurityConfig permits all requests to /api/auth/** without a token.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // WHY @Valid: triggers validation on RegisterRequest fields
    // (@NotBlank, @Email, @Size). Without this, annotations are ignored.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}