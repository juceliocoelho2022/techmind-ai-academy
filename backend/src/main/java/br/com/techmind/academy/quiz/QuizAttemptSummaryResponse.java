package br.com.techmind.academy.quiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record QuizAttemptSummaryResponse(
        Long attemptId,
        BigDecimal score,
        boolean passed,
        int xpAwarded,
        OffsetDateTime submittedAt
) {
    static QuizAttemptSummaryResponse from(QuizAttempt attempt) {
        return new QuizAttemptSummaryResponse(
                attempt.getId(),
                attempt.getScore(),
                attempt.getPassed(),
                attempt.getXpAwarded(),
                attempt.getSubmittedAt()
        );
    }
}
