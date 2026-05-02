package com.prephub.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findBySlug(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    List<Topic> findByFeaturedTrueOrderByQuestionCountDesc();
    List<Topic> findAllByOrderByQuestionCountDesc();
    List<Topic> findByParentId(UUID parentId);

    @Modifying
    @Query("UPDATE Topic t SET t.questionCount = t.questionCount + :delta WHERE t.id = :id")
    int adjustQuestionCount(@Param("id") UUID id, @Param("delta") int delta);
}
