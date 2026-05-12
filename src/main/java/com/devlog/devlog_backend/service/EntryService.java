package com.devlog.devlog_backend.service;

import com.devlog.devlog_backend.dto.request.EntryRequest;
import com.devlog.devlog_backend.dto.response.ApiResponse;
import com.devlog.devlog_backend.dto.response.EntryResponse;
import com.devlog.devlog_backend.dto.response.TagResponse;
import com.devlog.devlog_backend.entity.Entry;
import com.devlog.devlog_backend.entity.Tag;
import com.devlog.devlog_backend.entity.User;
import com.devlog.devlog_backend.exception.ResourceNotFoundException;
import com.devlog.devlog_backend.exception.UnauthorizedException;
import com.devlog.devlog_backend.repository.EntryRepository;
import com.devlog.devlog_backend.repository.TagRepository;
import com.devlog.devlog_backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final TagRepository tagRepository;
    private final SecurityUtils securityUtils;
    private final TagService tagService;

    // WHY @Transactional(readOnly=true): tells Hibernate this is a read operation.
    // Hibernate skips dirty-checking (comparing snapshots) which is a big
    // performance win on large result sets. Always use readOnly on GET methods.
    @Transactional(readOnly = true)
    public Page<EntryResponse> getEntries(String search, int page, int size) {
        User currentUser = securityUtils.getCurrentUser();

        // WHY Pageable: instead of loading all entries at once, we load
        // a "page" of results. page=0, size=10 returns the first 10 entries.
        // The frontend can request page=1 for the next 10, and so on.
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());

        Page<Entry> entries;
        if (search != null && !search.isBlank()) {
            entries = entryRepository.searchEntries(currentUser.getId(), search, pageable);
        } else {
            entries = entryRepository.findByUserIdOrderByEntryDateDesc(
                    currentUser.getId(), pageable);
        }

        // WHY .map(): Page<Entry> → Page<EntryResponse>
        // We convert every entity in the page to a safe DTO.
        return entries.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EntryResponse getEntryById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Entry entry = findEntryAndVerifyOwnership(id, currentUser);
        return toResponse(entry);
    }

    // WHY @Transactional (without readOnly): this method writes to the DB.
    // @Transactional means: if anything inside throws an exception,
    // ALL database changes in this method are rolled back automatically.
    // This prevents partial saves (e.g. entry saved but tags not linked).
    @Transactional
    public EntryResponse createEntry(EntryRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Entry entry = Entry.builder()
                .user(currentUser)
                .title(request.getTitle())
                .body(request.getBody())
                .entryDate(request.getEntryDate())
                .mood(request.getMood())
                .build();

        // Resolve tag IDs to Tag entities
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = resolveTags(request.getTagIds(), currentUser.getId());
            entry.setTags(tags);
        }

        return toResponse(entryRepository.save(entry));
    }

    @Transactional
    public EntryResponse updateEntry(Long id, EntryRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Entry entry = findEntryAndVerifyOwnership(id, currentUser);

        // WHY update fields individually (not create a new entity):
        // we preserve createdAt, id, and the user relationship.
        entry.setTitle(request.getTitle());
        entry.setBody(request.getBody());
        entry.setEntryDate(request.getEntryDate());
        entry.setMood(request.getMood());

        if (request.getTagIds() != null) {
            entry.setTags(resolveTags(request.getTagIds(), currentUser.getId()));
        }

        // WHY no explicit save() call: because we're inside @Transactional,
        // Hibernate tracks changes to managed entities automatically.
        // When the transaction commits, it flushes the changes to the DB.
        // This is called "dirty checking" — a core Hibernate concept.
        return toResponse(entry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Entry entry = findEntryAndVerifyOwnership(id, currentUser);
        entryRepository.delete(entry);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private Entry findEntryAndVerifyOwnership(Long id, User currentUser) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this entry");
        }
        return entry;
    }

    private Set<Tag> resolveTags(Set<Long> tagIds, Long userId) {
        Set<Tag> tags = new HashSet<>();
        for (Long tagId : tagIds) {
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Tag not found: " + tagId));
            // Security: verify the tag belongs to the current user
            if (!tag.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("Tag does not belong to you");
            }
            tags.add(tag);
        }
        return tags;
    }

    private EntryResponse toResponse(Entry entry) {
        Set<TagResponse> tagResponses = entry.getTags()
                .stream()
                .map(tagService::toResponse)
                .collect(Collectors.toSet());

        return EntryResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .body(entry.getBody())
                .entryDate(entry.getEntryDate())
                .mood(entry.getMood())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .tags(tagResponses)
                .build();
    }
}