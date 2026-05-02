package com.prephub.question;

import com.prephub.common.*;
import com.prephub.search.SearchIndexer;
import com.prephub.topic.Topic;
import com.prephub.topic.TopicRepository;
import com.prephub.user.PortfolioRepository;
import com.prephub.user.User;
import com.prephub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final SearchIndexer indexer;

    @Transactional(readOnly = true)
    public Page<QuestionDtos.QuestionSummary> list(UUID topicId, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Question> p = topicId != null
                ? questions.findByTopicIdAndStatus(topicId, QuestionStatus.PUBLISHED, pg)
                : questions.findByStatus(QuestionStatus.PUBLISHED, pg);
        return p.map(QuestionDtos.QuestionSummary::of);
    }

    @Transactional
    public QuestionDtos.QuestionDetail get(UUID id) {
        Question q = questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found"));
        questions.incrementViewCount(id);
        return QuestionDtos.QuestionDetail.of(q);
    }

    @Cacheable(value = "hotTopics", key = "'7d-' + #limit")
    @Transactional(readOnly = true)
    public List<QuestionDtos.QuestionSummary> hot(int limit) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return questions.findHotSince(since, PageRequest.of(0, limit))
                .stream().map(QuestionDtos.QuestionSummary::of).toList();
    }

    @Transactional
    @CacheEvict(value = "hotTopics", allEntries = true)
    public QuestionDtos.QuestionDetail create(UUID authorId, QuestionDtos.CreateQuestionRequest req) {
        User author = users.findById(authorId).orElseThrow(() -> new NotFoundException("User not found"));
        Topic topic = topics.findById(req.topicId()).orElseThrow(() -> new NotFoundException("Topic not found"));

        Question q = Question.builder()
                .title(req.title()).content(req.content())
                .topic(topic).author(author)
                .difficulty(req.difficulty() != null ? req.difficulty() : Difficulty.MEDIUM)
                .status(QuestionStatus.PUBLISHED)
                .tags(req.tags() != null ? new HashSet<>(req.tags()) : new HashSet<>())
                .build();
        q = questions.save(q);

        topics.adjustQuestionCount(topic.getId(), 1);
        portfolios.incrementPosts(authorId, 1, 2);
        indexer.indexQuestion(q);

        return QuestionDtos.QuestionDetail.of(q);
    }

    @Transactional
    @CacheEvict(value = "hotTopics", allEntries = true)
    public QuestionDtos.QuestionDetail update(UUID id, UUID actorId, String actorRole,
                                              QuestionDtos.UpdateQuestionRequest req) {
        Question q = questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found"));
        if (!q.getAuthor().getId().equals(actorId) && !"ADMIN".equals(actorRole))
            throw new ForbiddenException("Cannot edit this question");

        if (req.title() != null) q.setTitle(req.title());
        if (req.content() != null) q.setContent(req.content());
        if (req.difficulty() != null) q.setDifficulty(req.difficulty());
        if (req.status() != null) q.setStatus(req.status());
        if (req.tags() != null) q.setTags(new HashSet<>(req.tags()));

        Question saved = questions.save(q);
        indexer.indexQuestion(saved);
        return QuestionDtos.QuestionDetail.of(saved);
    }

    @Transactional
    @CacheEvict(value = "hotTopics", allEntries = true)
    public void delete(UUID id, UUID actorId, String actorRole) {
        Question q = questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found"));
        if (!q.getAuthor().getId().equals(actorId) && !"ADMIN".equals(actorRole))
            throw new ForbiddenException("Cannot delete this question");
        topics.adjustQuestionCount(q.getTopic().getId(), -1);
        questions.deleteById(id);
        indexer.deleteQuestion(id);
    }
}
