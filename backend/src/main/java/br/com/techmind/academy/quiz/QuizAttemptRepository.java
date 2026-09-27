package br.com.techmind.academy.quiz;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    boolean existsByQuizIdAndUserIdAndPassedTrue(Long quizId, Long userId);
    boolean existsByQuizId(Long quizId);
    List<QuizAttempt> findByQuizIdAndUserEmailOrderBySubmittedAtDesc(Long quizId, String email);

    @EntityGraph(attributePaths = {
            "quiz",
            "quiz.lesson",
            "quiz.lesson.module",
            "quiz.lesson.module.course"
    })
    @Query("select a from QuizAttempt a")
    List<QuizAttempt> findAllForAnalytics();
}
