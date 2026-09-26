package br.com.techmind.academy.learning;

public record LessonResponse(
        Long id,
        String slug,
        String title,
        String summary,
        int position,
        int xpReward
) {
    static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getSlug(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getPosition(),
                lesson.getXpReward()
        );
    }
}
