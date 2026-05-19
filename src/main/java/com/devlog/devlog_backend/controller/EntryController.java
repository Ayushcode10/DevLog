package com.devlog.devlog_backend.controller;

import com.devlog.devlog_backend.dto.request.EntryRequest;
import com.devlog.devlog_backend.dto.response.ApiResponse;
import com.devlog.devlog_backend.dto.response.EntryResponse;
import com.devlog.devlog_backend.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// WHY controllers are thin: a controller's ONLY job is
// 1. receive HTTP  2. call service  3. return HTTP
// If you find yourself writing IF statements or DB logic in a
// controller, stop — move it to the service layer.
@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EntryResponse>>> getEntries(
            // WHY @RequestParam with defaults: these are optional query params.
            // GET /api/entries              → page 0, size 10, no search
            // GET /api/entries?search=java  → filtered results
            // GET /api/entries?page=1       → second page
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<EntryResponse> entries = entryService.getEntries(search, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Entries fetched", entries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntryResponse>> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Entry fetched", entryService.getEntryById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EntryResponse>> createEntry(
            @Valid @RequestBody EntryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Entry created", entryService.createEntry(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EntryResponse>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody EntryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Entry updated", entryService.updateEntry(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        entryService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.success("Entry deleted", null));
    }
}