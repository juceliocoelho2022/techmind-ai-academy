package br.com.techmind.academy.quiz;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LessonQuizRepository extends JpaRepository<LessonQuiz, Long> {

    @EntityGraph(attributePaths = {"lesson", "lesson.module", "lesson.module.course", "questions"})
    Optional<LessonQuiz> findByLessonId(Long lessonId);

    @EntityGraph(attributePaths = {"lesson", "lesson.module", "lesson.module.course"})
    @Query("select q from LessonQuiz q")
    List<LessonQuiz> findAllForAnalytics();
}
