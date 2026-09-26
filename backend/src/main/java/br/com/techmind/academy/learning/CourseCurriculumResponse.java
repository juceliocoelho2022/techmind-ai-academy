package br.com.techmind.academy.learning;

import java.util.List;

public record CourseCurriculumResponse(
        Long courseId,
        String courseSlug,
        String courseTitle,
        int plannedLessons,
        int availableLessons,
        List<LearningModuleResponse> modules
) {
}
