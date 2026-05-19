package com.devlog.devlog_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class EntryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String body;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    // WHY String mood: we validate it in the service, not with annotations.
    // This gives a friendlier error message than a generic @Pattern message.
    private String mood;

    // WHY Set<Long> tagIds: the frontend sends tag IDs, not full tag objects.
    // We look them up in the service. Never trust the client to send full objects.
    private Set<Long> tagIds;
}