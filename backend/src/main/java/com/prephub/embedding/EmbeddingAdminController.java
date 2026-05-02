package com.prephub.embedding;

import com.prephub.common.QuestionStatus;
import com.prephub.question.Question;
import com.prephub.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoint to backfill embeddings for existing questions.
 * POST /api/admin/embeddings/backfill?batchSize=50
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/embeddings")
@RequiredArgsConstructor
public class EmbeddingAdminController {

    private final EmbeddingService embeddingService;
    private final QuestionRepository questions;

    @PostMapping("/backfill")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> backfill(@RequestParam(defaultValue = "50") int batchSize) {
        int total = 0;
        int pageNum = 0;

        while (true) {
            Page<Question> page = questions.findByStatus(
                    QuestionStatus.PUBLISHED,
                    PageRequest.of(pageNum, batchSize)
            );
            if (page.isEmpty()) break;
            int embedded = embeddingService.embedBatch(page.getContent());
            total += embedded;
            pageNum++;
            log.info("Backfilled embeddings: page={}, embedded={}, total={}", pageNum, embedded, total);
        }

        return Map.of(
                "status", "completed",
                "totalEmbedded", total,
                "pages", pageNum
        );
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> status() {
        return Map.of(
                "embeddingServiceAvailable", embeddingService.isAvailable(),
                "totalEmbeddings", "query question_embeddings table for count"
        );
    }
}
