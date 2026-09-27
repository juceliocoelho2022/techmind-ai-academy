package br.com.techmind.academy.learning;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/api/v1/courses/{courseId}/curriculum")
    public CourseCurriculumResponse curriculum(
            Authentication authentication,
            @PathVariable Long courseId
    ) {
        var email = authentication == null ? null : authentication.getName();
        return learningService.curriculum(courseId, email);
    }

    @GetMapping("/api/v1/learning/courses/{courseId}/progress")
    public LearningProgressResponse progress(
            Authentication authentication,
            @PathVariable Long courseId
    ) {
        return learningService.progress(authentication.getName(), courseId);
    }

    @PostMapping("/api/v1/learning/lessons/{lessonId}/complete")
    public CompleteLessonResponse completeLesson(
            Authentication authentication,
            @PathVariable Long lessonId
    ) {
        return learningService.completeLesson(authentication.getName(), lessonId);
    }
}
