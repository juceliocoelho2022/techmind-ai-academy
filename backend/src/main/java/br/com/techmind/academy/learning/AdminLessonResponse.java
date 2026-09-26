package br.com.techmind.academy.learning;

public record AdminLessonResponse(
        Long id,
        Long moduleId,
        String slug,
        String title,
        String summary,
        int position,
        int xpReward
) {
    static AdminLessonResponse from(Lesson lesson) {
        return new AdminLessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getSlug(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getPosition(),
                lesson.getXpReward()
        );
    }
}
