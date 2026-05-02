package com.prephub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    @Modifying
    @Query("UPDATE Portfolio p SET p.postsCount = p.postsCount + :delta, p.reputation = p.reputation + :repDelta WHERE p.userId = :userId")
    int incrementPosts(@Param("userId") UUID userId, @Param("delta") int delta, @Param("repDelta") int repDelta);

    @Modifying
    @Query("UPDATE Portfolio p SET p.suggestionsCount = p.suggestionsCount + 1 WHERE p.userId = :userId")
    int incrementSuggestions(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Portfolio p SET p.acceptedSuggestionsCount = p.acceptedSuggestionsCount + 1, p.reputation = p.reputation + 5 WHERE p.userId = :userId")
    int incrementAcceptedSuggestions(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Portfolio p SET p.likesReceived = p.likesReceived + :delta, p.reputation = p.reputation + :delta WHERE p.userId = :userId")
    int adjustLikes(@Param("userId") UUID userId, @Param("delta") int delta);
}
