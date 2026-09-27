package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.subscription.SubscriptionPlan;

import java.util.List;

public record CourseTemplateResponse(
        String key,
        String category,
        String technology,
        CourseLevel level,
        SubscriptionPlan requiredPlan,
        String title,
        String slug,
        String description,
        int totalLessons,
        List<ModuleTemplate> modules
) {
    public record ModuleTemplate(
            String title,
            String description,
            int position,
            List<LessonTemplate> lessons
    ) {}

    public record LessonTemplate(
            String slug,
            String title,
            String summary,
            int position,
            int xpReward
    ) {}
}
