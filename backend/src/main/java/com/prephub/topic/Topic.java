package com.prephub.topic;

import com.prephub.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "topics",
        uniqueConstraints = @UniqueConstraint(name = "uk_topics_slug", columnNames = "slug"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private int questionCount = 0;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;
}
