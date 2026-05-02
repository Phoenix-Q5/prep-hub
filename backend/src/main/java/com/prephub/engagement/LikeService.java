package com.prephub.engagement;

import com.prephub.common.NotFoundException;
import com.prephub.question.Question;
import com.prephub.question.QuestionRepository;
import com.prephub.user.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final QuestionLikeRepository likes;
    private final QuestionRepository questions;
    private final PortfolioRepository portfolios;

    @Transactional
    @CacheEvict(value = "hotTopics", allEntries = true)
    public boolean like(UUID userId, UUID questionId) {
        if (likes.existsByUserIdAndQuestionId(userId, questionId)) return false;
        Question q = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Question not found"));
        likes.save(QuestionLike.builder().userId(userId).questionId(questionId).createdAt(Instant.now()).build());
        questions.adjustLikeCount(questionId, 1);
        portfolios.adjustLikes(q.getAuthor().getId(), 1);
        return true;
    }

    @Transactional
    @CacheEvict(value = "hotTopics", allEntries = true)
    public boolean unlike(UUID userId, UUID questionId) {
        long deleted = likes.deleteByUserIdAndQuestionId(userId, questionId);
        if (deleted == 0) return false;
        Question q = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Question not found"));
        questions.adjustLikeCount(questionId, -1);
        portfolios.adjustLikes(q.getAuthor().getId(), -1);
        return true;
    }
}
