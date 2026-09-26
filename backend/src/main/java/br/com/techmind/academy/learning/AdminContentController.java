package br.com.techmind.academy.learning;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/content")
public class AdminContentController {

    private final AdminContentService service;

    public AdminContentController(AdminContentService service) {
        this.service = service;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCourseResponse createCourse(
            Authentication authentication,
            @Valid @RequestBody AdminCourseRequest request
    ) {
        return service.createCourse(authentication.getName(), request);
    }

    @PutMapping("/courses/{courseId}")
    public AdminCourseResponse updateCourse(
            Authentication authentication,
            @PathVariable Long courseId,
            @Valid @RequestBody AdminCourseRequest request
    ) {
        return service.updateCourse(authentication.getName(), courseId, request);
    }

    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            Authentication authentication,
            @PathVariable Long courseId
    ) {
        service.deleteCourse(authentication.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminModuleResponse createModule(
            Authentication authentication,
            @PathVariable Long courseId,
            @Valid @RequestBody AdminModuleRequest request
    ) {
        return service.createModule(authentication.getName(), courseId, request);
    }

    @PutMapping("/modules/{moduleId}")
    public AdminModuleResponse updateModule(
            Authentication authentication,
            @PathVariable Long moduleId,
            @Valid @RequestBody AdminModuleRequest request
    ) {
        return service.updateModule(authentication.getName(), moduleId, request);
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(
            Authentication authentication,
            @PathVariable Long moduleId
    ) {
        service.deleteModule(authentication.getName(), moduleId);
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminLessonResponse createLesson(
            Authentication authentication,
            @PathVariable Long moduleId,
            @Valid @RequestBody AdminLessonRequest request
    ) {
        return service.createLesson(authentication.getName(), moduleId, request);
    }

    @PutMapping("/lessons/{lessonId}")
    public AdminLessonResponse updateLesson(
            Authentication authentication,
            @PathVariable Long lessonId,
            @Valid @RequestBody AdminLessonRequest request
    ) {
        return service.updateLesson(authentication.getName(), lessonId, request);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(
            Authentication authentication,
            @PathVariable Long lessonId
    ) {
        service.deleteLesson(authentication.getName(), lessonId);
    }
}
