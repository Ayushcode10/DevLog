package com.devlog.devlog_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WHY onDelete CASCADE: if an Entry is deleted, all its attachments
    // are automatically deleted too. Without this, you'd have orphaned rows.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private Entry entry;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}