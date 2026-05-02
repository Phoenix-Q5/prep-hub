package com.prephub.question;

import com.prephub.common.Difficulty;
import com.prephub.common.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class QuestionDtos {

    public record QuestionSummary(
            UUID id, String title, UUID topicId, String topicName,
            String authorUsername, Difficulty difficulty, int likeCount,
            int answerCount, long viewCount, Set<String> tags, Instant createdAt
    ) {
        public static QuestionSummary of(Question q) {
            return new QuestionSummary(
                    q.getId(), q.getTitle(),
                    q.getTopic() != null ? q.getTopic().getId() : null,
                    q.getTopic() != null ? q.getTopic().getName() : null,
                    q.getAuthor() != null ? q.getAuthor().getUsername() : null,
                    q.getDifficulty(), q.getLikeCount(),
                    q.getAnswerCount(), q.getViewCount(), q.getTags(), q.getCreatedAt());
        }
    }

    public record QuestionDetail(
            UUID id, String title, String content, UUID topicId, String topicName,
            String authorUsername, Difficulty difficulty, QuestionStatus status,
            int likeCount, int answerCount, long viewCount, Set<String> tags,
            Instant createdAt, Instant updatedAt
    ) {
        public static QuestionDetail of(Question q) {
            return new QuestionDetail(
                    q.getId(), q.getTitle(), q.getContent(),
                    q.getTopic() != null ? q.getTopic().getId() : null,
                    q.getTopic() != null ? q.getTopic().getName() : null,
                    q.getAuthor() != null ? q.getAuthor().getUsername() : null,
                    q.getDifficulty(), q.getStatus(),
                    q.getLikeCount(), q.getAnswerCount(), q.getViewCount(),
                    q.getTags(), q.getCreatedAt(), q.getUpdatedAt());
        }
    }

    public record CreateQuestionRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String content,
            @NotNull UUID topicId,
            Difficulty difficulty,
            Set<String> tags
    ) {}

    public record UpdateQuestionRequest(
            @Size(max = 300) String title,
            String content,
            Difficulty difficulty,
            QuestionStatus status,
            Set<String> tags
    ) {}
}
