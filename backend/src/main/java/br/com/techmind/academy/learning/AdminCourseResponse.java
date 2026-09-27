package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseLevel;

public record AdminCourseResponse(
        Long id,
        String slug,
        String title,
        String description,
        String category,
        String technology,
        CourseLevel level,
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
                course.getTotalLessons()
        );
    }
}
