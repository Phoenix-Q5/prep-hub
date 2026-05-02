package com.prephub.embedding;

import com.prephub.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates embedding generation + pgvector storage + similarity search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingClient client;
    private final EmbeddingRepository repo;

    /**
     * Embed a question asynchronously after creation/update.
     * Combines title + tags + truncated content for a rich representation.
     */
    @Async
    @Transactional
    public void embedQuestion(Question q) {
        try {
            String text = buildEmbeddingText(q);
            float[] vec = client.embedSingle(text);
            String pgVec = toPgVector(vec);
            repo.upsertEmbedding(q.getId(), pgVec, "BAAI/bge-small-en-v1.5");
            log.debug("Embedded question {} ({} dims)", q.getId(), vec.length);
        } catch (Exception e) {
            log.warn("Failed to embed question {}: {}", q.getId(), e.getMessage());
        }
    }

    /**
     * Embed a batch of questions (for backfill / re-indexing).
     */
    @Transactional
    public int embedBatch(List<Question> questions) {
        if (questions.isEmpty()) return 0;
        List<String> texts = questions.stream().map(this::buildEmbeddingText).toList();
        List<float[]> vecs = client.embed(texts);
        for (int i = 0; i < questions.size(); i++) {
            repo.upsertEmbedding(questions.get(i).getId(), toPgVector(vecs.get(i)), "BAAI/bge-small-en-v1.5");
        }
        return questions.size();
    }

    /**
     * Semantic search: embed the query text, then find nearest neighbors.
     */
    public List<SemanticHit> semanticSearch(String queryText, int limit) {
        float[] queryVec = client.embedSingle(queryText);
        String pgVec = toPgVector(queryVec);
        List<Object[]> rows = repo.findNearestNeighbors(pgVec, limit);
        return rows.stream().map(row -> new SemanticHit(
                (UUID) row[0],
                ((Number) row[1]).doubleValue()
        )).toList();
    }

    /**
     * Delete embedding when question is deleted.
     */
    @Async
    @Transactional
    public void deleteEmbedding(UUID questionId) {
        repo.deleteById(questionId);
    }

    public boolean isAvailable() {
        return client.isHealthy();
    }

    // ------ helpers ------

    private String buildEmbeddingText(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append(q.getTitle());
        if (q.getTags() != null && !q.getTags().isEmpty()) {
            sb.append(" [").append(String.join(", ", q.getTags())).append("]");
        }
        if (q.getTopic() != null) {
            sb.append(" (").append(q.getTopic().getName()).append(")");
        }
        // First ~500 chars of content
        if (q.getContent() != null) {
            sb.append(" ").append(q.getContent(), 0, Math.min(q.getContent().length(), 500));
        }
        return sb.toString();
    }

    private String toPgVector(float[] vec) {
        return "[" + java.util.stream.IntStream.range(0, vec.length)
                .mapToObj(i -> String.format("%.8f", vec[i]))
                .collect(Collectors.joining(",")) + "]";
    }

    // Convert float[] to pgvector string helper
    private String toPgVector(double[] vec) {
        return "[" + Arrays.stream(vec)
                .mapToObj(v -> String.format("%.8f", v))
                .collect(Collectors.joining(",")) + "]";
    }

    public record SemanticHit(UUID questionId, double similarity) {}
}
