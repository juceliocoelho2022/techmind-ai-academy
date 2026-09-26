package br.com.techmind.academy.learning;

public record CompleteLessonResponse(
        Long lessonId,
        boolean newlyCompleted,
        int xpAwarded,
        int courseCompletedLessons,
        int courseXp,
        double percentage
) {
}
