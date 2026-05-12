package com.devlog.devlog_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

// WHY a separate Response DTO: the Entry entity has a User field
// (with password hash!). If we returned the entity directly, we'd
// leak sensitive data. The DTO is our explicit "safe to expose" contract.
@Data
@Builder
public class EntryResponse {
    private Long id;
    private String title;
    private String body;
    private LocalDate entryDate;
    private String mood;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TagResponse> tags;
}