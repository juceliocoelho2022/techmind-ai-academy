package br.com.techmind.academy.quiz;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "quiz_questions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_question_position",
                columnNames = {"quiz_id", "position"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private LessonQuiz quiz;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(nullable = false)
    private Integer position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();

    public void replaceOptions(List<QuizOption> newOptions) {
        options.clear();
        newOptions.forEach(option -> {
            option.setQuestion(this);
            options.add(option);
        });
    }
}
