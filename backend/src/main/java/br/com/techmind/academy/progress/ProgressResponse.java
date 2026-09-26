package br.com.techmind.academy.progress;

public record ProgressResponse(
        long completedLessons,
        long totalLessons,
        double percentage,
        int xp,
        String level
) {}
