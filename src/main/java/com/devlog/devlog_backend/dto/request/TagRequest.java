package com.devlog.devlog_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;

    private String color; // hex color, e.g. "#6366f1"
}