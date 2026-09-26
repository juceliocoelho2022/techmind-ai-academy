package br.com.techmind.academy.learning;

import java.util.List;

public record LearningModuleResponse(
        Long id,
        String title,
        String description,
        int position,
        List<LessonResponse> lessons
) {
}
