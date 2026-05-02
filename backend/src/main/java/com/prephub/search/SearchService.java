package com.prephub.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations operations;

    @Cacheable(value = "search", key = "#q + '|' + #topicId + '|' + #limit")
    public List<SearchDtos.SearchHit> typeAhead(String q, String topicId, int limit) {
        if (q == null || q.isBlank()) return List.of();

        Query bool = Query.of(b -> b.bool(bq -> {
            bq.must(m -> m.multiMatch(mm -> mm
                    .query(q)
                    .fields("title^3", "title.fuzzy^1.5", "topicName^2", "tags^2", "content")
                    .fuzziness("AUTO")
                    .prefixLength(1)));
            if (topicId != null && !topicId.isBlank())
                bq.filter(f -> f.term(t -> t.field("topicId").value(topicId)));
            bq.filter(f -> f.term(t -> t.field("status").value("PUBLISHED")));
            return bq;
        }));

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool)
                .withSort(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))))
                .withPageable(org.springframework.data.domain.PageRequest.of(0, limit))
                .build();

        SearchHits<QuestionDocument> hits = operations.search(query, QuestionDocument.class);
        return hits.stream().map(h -> {
            QuestionDocument d = h.getContent();
            return new SearchDtos.SearchHit(
                    d.getId(), d.getTitle(), d.getTopicName(), d.getAuthorUsername(),
                    d.getDifficulty() != null ? d.getDifficulty().name() : null,
                    d.getLikeCount(), d.getViewCount(),
                    h.getScore());
        }).toList();
    }
}
