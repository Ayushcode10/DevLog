package com.devlog.devlog_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WHY @ManyToOne: many entries belong to one user.
    // WHY LAZY: don't load the full User object from DB unless we
    // explicitly ask for it. This prevents N+1 query problems.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private String title;

    // WHY @Lob / columnDefinition TEXT: journal entries can be long.
    // VARCHAR has a 255 char default limit. TEXT stores up to 65,535 chars.
    @Column(columnDefinition = "TEXT")
    private String body;

    private String mood; // "HAPPY", "FOCUSED", "TIRED", "FRUSTRATED", "NEUTRAL"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // WHY @ManyToMany: one entry has many tags, one tag is on many entries.
    // WHY @JoinTable: this is what creates the entry_tags join table in MySQL.
    // CascadeType.MERGE means if we save an entry with new tags, those tags
    // are also persisted automatically.
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "entry_tags",
            joinColumns = @JoinColumn(name = "entry_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}