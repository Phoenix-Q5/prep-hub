package com.prephub.search;

import com.prephub.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexer {

    private final QuestionSearchRepository repo;

    @Async
    public void indexQuestion(Question q) {
        try {
            QuestionDocument doc = QuestionDocument.builder()
                    .id(q.getId().toString())
                    .title(q.getTitle())
                    .content(q.getContent())
                    .topicId(q.getTopic() != null ? q.getTopic().getId().toString() : null)
                    .topicName(q.getTopic() != null ? q.getTopic().getName() : null)
                    .authorUsername(q.getAuthor() != null ? q.getAuthor().getUsername() : null)
                    .difficulty(q.getDifficulty())
                    .status(q.getStatus())
                    .tags(q.getTags())
                    .likeCount(q.getLikeCount())
                    .viewCount(q.getViewCount())
                    .createdAt(q.getCreatedAt())
                    .build();
            repo.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index question {}: {}", q.getId(), e.getMessage());
        }
    }

    @Async
    public void deleteQuestion(UUID id) {
        try { repo.deleteById(id.toString()); }
        catch (Exception e) { log.warn("Failed to remove question {} from index: {}", id, e.getMessage()); }
    }
}
