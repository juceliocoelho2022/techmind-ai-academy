package br.com.techmind.academy.learning;

public record LessonResourceResponse(
        Long id,
        LessonResourceType type,
        String title,
        String description,
        String fileName,
        String contentType,
        long sizeBytes,
        int position
) {
    static LessonResourceResponse from(LessonResource resource) {
        return new LessonResourceResponse(
                resource.getId(),
                resource.getType(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getOriginalFileName(),
                resource.getContentType(),
                resource.getSizeBytes(),
                resource.getPosition()
        );
    }
}
