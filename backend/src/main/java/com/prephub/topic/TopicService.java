package com.prephub.topic;

import com.prephub.common.ConflictException;
import com.prephub.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topics;

    @Cacheable(value = "topicTree", key = "'all'")
    public List<TopicDtos.TopicResponse> listAll() {
        return topics.findAllByOrderByQuestionCountDesc().stream().map(TopicDtos.TopicResponse::of).toList();
    }

    @Cacheable(value = "topicTree", key = "'featured'")
    public List<TopicDtos.TopicResponse> listFeatured() {
        return topics.findByFeaturedTrueOrderByQuestionCountDesc().stream().map(TopicDtos.TopicResponse::of).toList();
    }

    public TopicDtos.TopicResponse getBySlug(String slug) {
        return topics.findBySlug(slug).map(TopicDtos.TopicResponse::of)
                .orElseThrow(() -> new NotFoundException("Topic not found: " + slug));
    }

    @Transactional
    @CacheEvict(value = "topicTree", allEntries = true)
    public TopicDtos.TopicResponse create(TopicDtos.CreateTopicRequest req) {
        if (topics.existsBySlugIgnoreCase(req.slug()))
            throw new ConflictException("Topic slug already exists: " + req.slug());

        Topic t = Topic.builder()
                .name(req.name()).slug(req.slug().toLowerCase())
                .description(req.description()).colorHex(req.colorHex())
                .parentId(req.parentId()).featured(req.featured())
                .build();
        return TopicDtos.TopicResponse.of(topics.save(t));
    }

    @Transactional
    @CacheEvict(value = "topicTree", allEntries = true)
    public void delete(UUID id) {
        if (!topics.existsById(id)) throw new NotFoundException("Topic not found");
        topics.deleteById(id);
    }
}
