package br.com.techmind.academy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    boolean existsByQuizIdAndUserIdAndPassedTrue(Long quizId, Long userId);
    boolean existsByQuizId(Long quizId);
    List<QuizAttempt> findByQuizIdAndUserEmailOrderBySubmittedAtDesc(Long quizId, String email);
}
