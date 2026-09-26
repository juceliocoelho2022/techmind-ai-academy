package br.com.techmind.academy.interview;

import java.util.List;

public record InterviewFeedback(
        int score,
        String summary,
        List<String> strengths,
        List<String> improvements,
        String nextQuestion
) {}
