package com.prephub.user;

import com.prephub.common.Role;

import java.time.Instant;
import java.util.UUID;

public class UserDtos {

    public record UserProfile(
            UUID id, String username, String email, String displayName, String bio,
            String avatarUrl, Role role, Instant joinedAt, PortfolioStats stats
    ) {}

    public record PortfolioStats(
            int posts, int suggestions, int acceptedSuggestions,
            int likesReceived, int reputation
    ) {
        public static PortfolioStats of(Portfolio p) {
            return new PortfolioStats(
                    p.getPostsCount(), p.getSuggestionsCount(),
                    p.getAcceptedSuggestionsCount(), p.getLikesReceived(), p.getReputation());
        }
    }

    public record UpdateProfileRequest(String displayName, String bio) {}
}
