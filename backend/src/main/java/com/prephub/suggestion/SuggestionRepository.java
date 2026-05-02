package com.prephub.suggestion;

import com.prephub.common.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {
    Page<Suggestion> findByStatus(SuggestionStatus status, Pageable pageable);
    Page<Suggestion> findByUserId(UUID userId, Pageable pageable);
}
