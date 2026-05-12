package com.devlog.devlog_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// WHY DTOs: we NEVER accept a raw @Entity as a request body.
// Someone could send extra fields (like isAdmin=true) that Hibernate
// would silently persist. DTOs are an explicit whitelist of accepted fields.
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Must be a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}