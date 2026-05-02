package com.prephub.topic;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService service;

    @GetMapping
    public List<TopicDtos.TopicResponse> list(@RequestParam(defaultValue = "false") boolean featured) {
        return featured ? service.listFeatured() : service.listAll();
    }

    @GetMapping("/{slug}")
    public TopicDtos.TopicResponse getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }
}

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
class AdminTopicController {

    private final TopicService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicDtos.TopicResponse> create(@Valid @RequestBody TopicDtos.CreateTopicRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
