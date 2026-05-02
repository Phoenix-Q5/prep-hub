package com.prephub.search;

public class SearchDtos {
    public record SearchHit(
            String id, String title, String topicName, String authorUsername,
            String difficulty, int likeCount, long viewCount, double score
    ) {}
}
