package br.com.techmind.academy.course;

import br.com.techmind.academy.subscription.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 80)
    private String technology;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_plan", nullable = false, length = 20)
    private SubscriptionPlan requiredPlan;

    @Column(nullable = false)
    private Integer totalLessons;

    @PrePersist
    void prePersist() {
        if (category == null || category.isBlank()) category = "Geral";
        if (technology == null || technology.isBlank()) technology = "Geral";
        if (level == null) level = CourseLevel.INTERMEDIATE;
        if (requiredPlan == null) requiredPlan = SubscriptionPlan.FREE;
        if (totalLessons == null) totalLessons = 0;
    }
}
