package com.prephub.suggestion;

import com.prephub.common.SuggestionStatus;
import com.prephub.common.SuggestionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SuggestionDtos {

    public record SuggestionResponse(
            UUID id, SuggestionType type, UUID userId, String username,
            UUID questionId, Map<String, Object> payload, String rationale,
            SuggestionStatus status, String reviewedByUsername, Instant reviewedAt,
            String reviewNotes, Instant createdAt
    ) {
        public static SuggestionResponse of(Suggestion s) {
            return new SuggestionResponse(
                    s.getId(), s.getType(),
                    s.getUser() != null ? s.getUser().getId() : null,
                    s.getUser() != null ? s.getUser().getUsername() : null,
                    s.getQuestion() != null ? s.getQuestion().getId() : null,
                    s.getPayload(), s.getRationale(), s.getStatus(),
                    s.getReviewedBy() != null ? s.getReviewedBy().getUsername() : null,
                    s.getReviewedAt(), s.getReviewNotes(), s.getCreatedAt());
        }
    }

    public record CreateSuggestionRequest(
            @NotNull SuggestionType type,
            UUID questionId,
            @NotNull Map<String, Object> payload,
            @Size(max = 500) String rationale
    ) {}

    public record ReviewSuggestionRequest(
            @NotNull SuggestionStatus decision,
            @Size(max = 500) String reviewNotes
    ) {}
}
