package com.prephub.question;

import com.prephub.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService service;
    private final CurrentUser currentUser;

    @GetMapping
    public Page<QuestionDtos.QuestionSummary> list(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(topicId, page, size);
    }

    @GetMapping("/hot")
    public List<QuestionDtos.QuestionSummary> hot(@RequestParam(defaultValue = "10") int limit) {
        return service.hot(limit);
    }

    @GetMapping("/{id}")
    public QuestionDtos.QuestionDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<QuestionDtos.QuestionDetail> create(@Valid @RequestBody QuestionDtos.CreateQuestionRequest req) {
        var u = currentUser.getRequired();
        return ResponseEntity.ok(service.create(u.id(), req));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<QuestionDtos.QuestionDetail> update(
            @PathVariable UUID id, @Valid @RequestBody QuestionDtos.UpdateQuestionRequest req) {
        var u = currentUser.getRequired();
        return ResponseEntity.ok(service.update(id, u.id(), u.role(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        var u = currentUser.getRequired();
        service.delete(id, u.id(), u.role());
        return ResponseEntity.noContent().build();
    }
}
