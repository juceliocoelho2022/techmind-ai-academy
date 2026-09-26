package br.com.techmind.academy.learning;

import br.com.techmind.academy.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_progress_user_lesson",
                columnNames = {"user_id", "lesson_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(name = "xp_awarded", nullable = false)
    private Integer xpAwarded;

    @PrePersist
    void prePersist() {
        if (completedAt == null) completedAt = OffsetDateTime.now();
        if (xpAwarded == null) xpAwarded = 0;
    }
}
