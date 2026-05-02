package com.prephub.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class TopicDtos {

    public record TopicResponse(
            UUID id, String name, String slug, String description,
            String colorHex, UUID parentId, int questionCount, boolean featured
    ) {
        public static TopicResponse of(Topic t) {
            return new TopicResponse(t.getId(), t.getName(), t.getSlug(), t.getDescription(),
                    t.getColorHex(), t.getParentId(), t.getQuestionCount(), t.isFeatured());
        }
    }

    public record CreateTopicRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 120) String slug,
            @Size(max = 500) String description,
            @Size(max = 7) String colorHex,
            UUID parentId,
            boolean featured
    ) {}
}
