package br.com.techmind.academy.quiz;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "quiz_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_option_position",
                columnNames = {"question_id", "position"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_text", nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private Boolean correct;
}
