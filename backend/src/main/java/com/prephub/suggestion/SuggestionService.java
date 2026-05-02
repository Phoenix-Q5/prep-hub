package com.prephub.suggestion;

import com.prephub.common.*;
import com.prephub.question.Question;
import com.prephub.question.QuestionDtos;
import com.prephub.question.QuestionRepository;
import com.prephub.question.QuestionService;
import com.prephub.search.SearchIndexer;
import com.prephub.topic.Topic;
import com.prephub.topic.TopicRepository;
import com.prephub.user.PortfolioRepository;
import com.prephub.user.User;
import com.prephub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestions;
    private final UserRepository users;
    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final PortfolioRepository portfolios;
    private final SearchIndexer indexer;

    @Transactional
    public SuggestionDtos.SuggestionResponse create(UUID userId, SuggestionDtos.CreateSuggestionRequest req) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Question q = req.questionId() != null
                ? questions.findById(req.questionId()).orElseThrow(() -> new NotFoundException("Question not found"))
                : null;

        Suggestion s = Suggestion.builder()
                .type(req.type()).user(user).question(q)
                .payload(req.payload()).rationale(req.rationale())
                .status(SuggestionStatus.PENDING).build();
        s = suggestions.save(s);
        portfolios.incrementSuggestions(userId);
        return SuggestionDtos.SuggestionResponse.of(s);
    }

    @Transactional(readOnly = true)
    public Page<SuggestionDtos.SuggestionResponse> listPending(int page, int size) {
        return suggestions.findByStatus(SuggestionStatus.PENDING,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))
                .map(SuggestionDtos.SuggestionResponse::of);
    }

    @Transactional(readOnly = true)
    public Page<SuggestionDtos.SuggestionResponse> listMine(UUID userId, int page, int size) {
        return suggestions.findByUserId(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(SuggestionDtos.SuggestionResponse::of);
    }

    @Transactional
    public SuggestionDtos.SuggestionResponse review(UUID id, UUID adminId, SuggestionDtos.ReviewSuggestionRequest req) {
        Suggestion s = suggestions.findById(id).orElseThrow(() -> new NotFoundException("Suggestion not found"));
        if (s.getStatus() != SuggestionStatus.PENDING)
            throw new ConflictException("Suggestion already reviewed");

        User admin = users.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found"));
        s.setStatus(req.decision());
        s.setReviewedBy(admin);
        s.setReviewedAt(Instant.now());
        s.setReviewNotes(req.reviewNotes());

        if (req.decision() == SuggestionStatus.APPROVED) {
            applyApproved(s);
            portfolios.incrementAcceptedSuggestions(s.getUser().getId());
        }

        return SuggestionDtos.SuggestionResponse.of(suggestions.save(s));
    }

    /**
     * Apply approved suggestion to the actual content.
     * For NEW_QUESTION: create the question. For EDIT_QUESTION: patch fields.
     */
    @SuppressWarnings("unchecked")
    private void applyApproved(Suggestion s) {
        switch (s.getType()) {
            case NEW_QUESTION -> {
                var p = s.getPayload();
                String title = (String) p.get("title");
                String content = (String) p.get("content");
                String topicSlug = (String) p.get("topicSlug");
                Topic topic = topics.findBySlug(topicSlug)
                        .orElseThrow(() -> new NotFoundException("Topic not found: " + topicSlug));
                Set<String> tags = p.get("tags") instanceof java.util.Collection<?> col
                        ? new HashSet<>((java.util.Collection<String>) col) : new HashSet<>();
                String diff = (String) p.getOrDefault("difficulty", "MEDIUM");

                Question q = Question.builder()
                        .title(title).content(content).topic(topic).author(s.getUser())
                        .difficulty(Difficulty.valueOf(diff))
                        .status(QuestionStatus.PUBLISHED)
                        .tags(tags).build();
                q = questions.save(q);
                topics.adjustQuestionCount(topic.getId(), 1);
                indexer.indexQuestion(q);
            }
            case EDIT_QUESTION -> {
                if (s.getQuestion() == null) return;
                Question q = s.getQuestion();
                var p = s.getPayload();
                if (p.get("title") instanceof String t) q.setTitle(t);
                if (p.get("content") instanceof String c) q.setContent(c);
                if (p.get("difficulty") instanceof String d) q.setDifficulty(Difficulty.valueOf(d));
                if (p.get("tags") instanceof java.util.Collection<?> tags)
                    q.setTags(new HashSet<>((java.util.Collection<String>) tags));
                Question saved = questions.save(q);
                indexer.indexQuestion(saved);
            }
            case NEW_TOPIC -> {
                var p = s.getPayload();
                Topic t = Topic.builder()
                        .name((String) p.get("name"))
                        .slug(((String) p.get("slug")).toLowerCase())
                        .description((String) p.get("description"))
                        .colorHex((String) p.get("colorHex"))
                        .build();
                topics.save(t);
            }
            case NEW_ANSWER -> {  }
        }
    }
}
