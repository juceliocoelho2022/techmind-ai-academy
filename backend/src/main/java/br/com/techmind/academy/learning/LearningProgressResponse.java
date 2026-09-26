package br.com.techmind.academy.learning;

import java.util.List;

public record LearningProgressResponse(
        Long courseId,
        List<Long> completedLessonIds,
        int completedLessons,
        int plannedLessons,
        int xp,
        double percentage
) {
}
