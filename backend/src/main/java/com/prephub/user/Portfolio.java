package com.prephub.user;

import com.prephub.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio extends Auditable {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "posts_count", nullable = false)
    @Builder.Default
    private int postsCount = 0;

    @Column(name = "suggestions_count", nullable = false)
    @Builder.Default
    private int suggestionsCount = 0;

    @Column(name = "accepted_suggestions_count", nullable = false)
    @Builder.Default
    private int acceptedSuggestionsCount = 0;

    @Column(name = "likes_received", nullable = false)
    @Builder.Default
    private int likesReceived = 0;

    @Column(name = "reputation", nullable = false)
    @Builder.Default
    private int reputation = 0;
}
