package com.prephub.engagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, QuestionLike.QuestionLikeId> {
    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);
    long deleteByUserIdAndQuestionId(UUID userId, UUID questionId);
    long countByQuestionId(UUID questionId);
}
