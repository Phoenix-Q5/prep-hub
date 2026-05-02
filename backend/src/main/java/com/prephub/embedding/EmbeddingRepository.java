package com.prephub.embedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingRepository extends JpaRepository<QuestionEmbedding, UUID> {

    /**
     * Upsert embedding using native pgvector cast.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO question_embeddings (question_id, embedding, model_name, created_at, updated_at)
            VALUES (:questionId, cast(:embedding AS vector), :modelName, NOW(), NOW())
            ON CONFLICT (question_id)
            DO UPDATE SET embedding = cast(:embedding AS vector), updated_at = NOW()
            """)
    void upsertEmbedding(
            @Param("questionId") UUID questionId,
            @Param("embedding") String embedding,
            @Param("modelName") String modelName
    );

    /**
     * Nearest-neighbor search using cosine distance.
     * Returns question IDs ordered by similarity (most similar first).
     */
    @Query(nativeQuery = true, value = """
            SELECT qe.question_id,
                   1 - (qe.embedding <=> cast(:queryVec AS vector)) AS similarity
            FROM question_embeddings qe
            JOIN questions q ON q.id = qe.question_id
            WHERE q.status = 'PUBLISHED'
            ORDER BY qe.embedding <=> cast(:queryVec AS vector)
            LIMIT :limit
            """)
    List<Object[]> findNearestNeighbors(
            @Param("queryVec") String queryVec,
            @Param("limit") int limit
    );
}
