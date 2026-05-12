package com.devlog.devlog_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

// WHY a generic wrapper: instead of returning raw objects or strings,
// every endpoint returns { "success": true, "message": "...", "data": {...} }
// This consistency makes the frontend much easier to work with.
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    // Convenience factory methods
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}