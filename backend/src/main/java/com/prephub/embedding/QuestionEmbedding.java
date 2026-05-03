package com.prephub.embedding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the question_embeddings table.
 * The actual vector is stored/queried via native SQL (pgvector doesn't have
 * a Hibernate type yet), so we store it as a String for read convenience.
 */
@Entity
@Table(name = "question_embeddings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionEmbedding {

    @Id
    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "model_name", length = 100)
    @Builder.Default
    private String modelName = "BAAI/bge-small-en-v1.5";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // Note: 'embedding' column is vector(384) — read/written via native SQL
}
