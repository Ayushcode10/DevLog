package com.devlog.devlog_backend.service;

import com.devlog.devlog_backend.dto.request.TagRequest;
import com.devlog.devlog_backend.dto.response.TagResponse;
import com.devlog.devlog_backend.entity.Tag;
import com.devlog.devlog_backend.entity.User;
import com.devlog.devlog_backend.exception.ResourceNotFoundException;
import com.devlog.devlog_backend.exception.UnauthorizedException;
import com.devlog.devlog_backend.repository.TagRepository;
import com.devlog.devlog_backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final SecurityUtils securityUtils;

    public List<TagResponse> getAllTags() {
        User currentUser = securityUtils.getCurrentUser();
        // WHY filter by userId: users should only see THEIR own tags.
        // Never do findAll() on user-owned data.
        return tagRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TagResponse createTag(TagRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        if (tagRepository.existsByNameAndUserId(request.getName(), currentUser.getId())) {
            throw new RuntimeException("Tag '" + request.getName() + "' already exists");
        }

        Tag tag = Tag.builder()
                .user(currentUser)
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : "#6366f1")
                .build();

        return toResponse(tagRepository.save(tag));
    }

    public void deleteTag(Long tagId) {
        User currentUser = securityUtils.getCurrentUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        // WHY this check: without it, user A could delete user B's tags
        // just by knowing the ID. Always verify ownership.
        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this tag");
        }

        tagRepository.delete(tag);
    }

    // WHY a private mapper method: keeps toResponse logic in one place.
    // If TagResponse changes, we update one method, not 10 places.
    public TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }
}