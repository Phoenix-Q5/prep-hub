package com.prephub.embedding;

import com.prephub.question.Question;
import com.prephub.question.QuestionRepository;
import com.prephub.search.SearchDtos;
import com.prephub.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hybrid search: combines Elasticsearch keyword results with pgvector semantic results
 * using Reciprocal Rank Fusion (RRF).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final SearchService keywordSearch;
    private final EmbeddingService embeddingService;
    private final QuestionRepository questions;

    private static final double RRF_K = 60.0; // standard RRF constant

    @Cacheable(value = "search", key = "'hybrid:' + #query + '|' + #limit")
    public List<SearchDtos.SearchHit> hybridSearch(String query, int limit) {
        // 1. Keyword results from ES
        List<SearchDtos.SearchHit> kwHits = keywordSearch.typeAhead(query, null, limit * 2);

        // 2. Semantic results from pgvector
        List<EmbeddingService.SemanticHit> semHits;
        try {
            semHits = embeddingService.semanticSearch(query, limit * 2);
        } catch (Exception e) {
            log.warn("Semantic search unavailable, falling back to keyword-only: {}", e.getMessage());
            return kwHits.stream().limit(limit).toList();
        }

        // 3. RRF fusion
        Map<String, Double> scores = new LinkedHashMap<>();

        for (int i = 0; i < kwHits.size(); i++) {
            String id = kwHits.get(i).id();
            scores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
        }

        for (int i = 0; i < semHits.size(); i++) {
            String id = semHits.get(i).questionId().toString();
            scores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
        }

        // 4. Sort by fused score, build response
        List<String> rankedIds = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        // Build a lookup of keyword hits for fast retrieval
        Map<String, SearchDtos.SearchHit> kwLookup = kwHits.stream()
                .collect(Collectors.toMap(SearchDtos.SearchHit::id, h -> h, (a, b) -> a));

        List<SearchDtos.SearchHit> result = new ArrayList<>();
        for (String id : rankedIds) {
            SearchDtos.SearchHit kwHit = kwLookup.get(id);
            if (kwHit != null) {
                result.add(new SearchDtos.SearchHit(
                        kwHit.id(), kwHit.title(), kwHit.topicName(), kwHit.authorUsername(),
                        kwHit.difficulty(), kwHit.likeCount(), kwHit.viewCount(),
                        scores.get(id)
                ));
            } else {
                // Came from semantic only — fetch from DB
                try {
                    Question q = questions.findById(UUID.fromString(id))
                            .orElse(null);
                    if (q != null) {
                        result.add(new SearchDtos.SearchHit(
                                q.getId().toString(), q.getTitle(),
                                q.getTopic() != null ? q.getTopic().getName() : null,
                                q.getAuthor() != null ? q.getAuthor().getUsername() : null,
                                q.getDifficulty() != null ? q.getDifficulty().name() : null,
                                q.getLikeCount(), q.getViewCount(),
                                scores.get(id)
                        ));
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch question {}: {}", id, e.getMessage());
                }
            }
        }

        return result;
    }
}
