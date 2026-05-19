package com.devlog.devlog_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

// This is what we send back after a successful login/register.
// The frontend stores this token and sends it with every future request.
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String name;
}