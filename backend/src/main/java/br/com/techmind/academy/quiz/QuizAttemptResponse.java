package br.com.techmind.academy.quiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record QuizAttemptResponse(
        Long attemptId,
        BigDecimal score,
        int correctAnswers,
        int totalQuestions,
        boolean passed,
        int xpAwarded,
        OffsetDateTime submittedAt,
        List<QuestionResult> results
) {
    public record QuestionResult(
            Long questionId,
            Long selectedOptionId,
            Long correctOptionId,
            boolean correct
    ) {}
}
