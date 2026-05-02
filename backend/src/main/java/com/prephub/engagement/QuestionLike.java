package com.prephub.engagement;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "question_likes")
@IdClass(QuestionLike.QuestionLikeId.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionLike {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "created_at", nullable = false) @Builder.Default
    private Instant createdAt = Instant.now();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class QuestionLikeId implements Serializable {
        private UUID userId;
        private UUID questionId;
    }
}
