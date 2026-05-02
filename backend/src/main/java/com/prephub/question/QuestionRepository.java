package com.prephub.question;

import com.prephub.common.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Page<Question> findByStatus(QuestionStatus status, Pageable pageable);
    Page<Question> findByTopicIdAndStatus(UUID topicId, QuestionStatus status, Pageable pageable);
    Page<Question> findByAuthorId(UUID authorId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.status = 'PUBLISHED' AND q.createdAt >= :since ORDER BY q.likeCount DESC, q.viewCount DESC")
    List<Question> findHotSince(@Param("since") Instant since, Pageable pageable);

    @Modifying
    @Query("UPDATE Question q SET q.viewCount = q.viewCount + 1 WHERE q.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Question q SET q.likeCount = q.likeCount + :delta WHERE q.id = :id")
    void adjustLikeCount(@Param("id") UUID id, @Param("delta") int delta);
}
