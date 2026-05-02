package com.prephub.answer;

import com.prephub.common.Auditable;
import com.prephub.question.Question;
import com.prephub.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "answers", indexes = @Index(name = "idx_answers_question", columnList = "question_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private boolean official = false;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;
}
