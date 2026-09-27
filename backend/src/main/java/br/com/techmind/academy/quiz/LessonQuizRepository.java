package br.com.techmind.academy.quiz;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonQuizRepository extends JpaRepository<LessonQuiz, Long> {

    @EntityGraph(attributePaths = {"lesson", "lesson.module", "lesson.module.course", "questions", "questions.options"})
    Optional<LessonQuiz> findByLessonId(Long lessonId);
}
