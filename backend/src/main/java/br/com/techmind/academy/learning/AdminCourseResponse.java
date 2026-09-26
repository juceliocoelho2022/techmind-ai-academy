package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;

public record AdminCourseResponse(
        Long id,
        String slug,
        String title,
        String description,
        int totalLessons
) {
    static AdminCourseResponse from(Course course) {
        return new AdminCourseResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getDescription(),
                course.getTotalLessons()
        );
    }
}
