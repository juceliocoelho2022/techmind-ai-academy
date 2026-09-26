package br.com.techmind.academy.learning;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_module_slug", columnNames = {"module_id", "slug"}),
                @UniqueConstraint(name = "uk_lesson_module_position", columnNames = {"module_id", "position"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward;
}
