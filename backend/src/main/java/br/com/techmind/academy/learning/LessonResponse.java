package br.com.techmind.academy.learning;

import java.util.List;

public record LessonResponse(
        Long id,
        String slug,
        String title,
        String summary,
        int position,
        int xpReward,
        List<LessonResourceResponse> resources
) {
    static LessonResponse from(Lesson lesson, List<LessonResource> resources) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getSlug(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getPosition(),
                lesson.getXpReward(),
                resources.stream().map(LessonResourceResponse::from).toList()
        );
    }
}
