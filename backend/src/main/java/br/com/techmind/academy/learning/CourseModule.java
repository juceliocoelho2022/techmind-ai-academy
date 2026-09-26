package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "course_modules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_module_position",
                columnNames = {"course_id", "position"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Integer position;
}
