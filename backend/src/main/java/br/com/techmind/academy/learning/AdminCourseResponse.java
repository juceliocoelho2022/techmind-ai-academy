package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.subscription.SubscriptionPlan;

public record AdminCourseResponse(
        Long id,
        String slug,
        String title,
        String description,
        String category,
        String technology,
        CourseLevel level,
        SubscriptionPlan requiredPlan,
        int totalLessons
) {
    static AdminCourseResponse from(Course course) {
        return new AdminCourseResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory(),
                course.getTechnology(),
                course.getLevel(),
                course.getRequiredPlan(),
                course.getTotalLessons()
        );
    }
}
