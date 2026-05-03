package com.prephub.suggestion;

import com.prephub.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService service;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<SuggestionDtos.SuggestionResponse> create(
            @Valid @RequestBody SuggestionDtos.CreateSuggestionRequest req) {
        return ResponseEntity.ok(service.create(currentUser.getRequired().id(), req));
    }

    @GetMapping("/me")
    public Page<SuggestionDtos.SuggestionResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listMine(currentUser.getRequired().id(), page, size);
    }
}

@RestController
@RequestMapping("/api/admin/suggestions")
@RequiredArgsConstructor
class AdminSuggestionController {

    private final SuggestionService service;
    private final CurrentUser currentUser;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SuggestionDtos.SuggestionResponse> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPending(page, size);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuggestionDtos.SuggestionResponse> review(
            @PathVariable UUID id, @Valid @RequestBody SuggestionDtos.ReviewSuggestionRequest req) {
        return ResponseEntity.ok(service.review(id, currentUser.getRequired().id(), req));
    }
}
