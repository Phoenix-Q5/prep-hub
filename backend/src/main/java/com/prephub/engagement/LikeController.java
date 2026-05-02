package com.prephub.engagement;

import com.prephub.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions/{questionId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService service;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> like(@PathVariable UUID questionId) {
        return ResponseEntity.ok(Map.of("liked", service.like(currentUser.getRequired().id(), questionId)));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Boolean>> unlike(@PathVariable UUID questionId) {
        return ResponseEntity.ok(Map.of("unliked", service.unlike(currentUser.getRequired().id(), questionId)));
    }
}
