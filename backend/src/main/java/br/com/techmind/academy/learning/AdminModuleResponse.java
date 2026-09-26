package br.com.techmind.academy.learning;

public record AdminModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        int position
) {
    static AdminModuleResponse from(CourseModule module) {
        return new AdminModuleResponse(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getPosition()
        );
    }
}
