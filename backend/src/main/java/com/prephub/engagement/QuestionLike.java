package com.prephub.engagement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
