package br.com.techmind.academy.quiz;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/lessons/{lessonId}/quiz")
public class AdminQuizController {

    private final AdminQuizService service;

    public AdminQuizController(AdminQuizService service) {
        this.service = service;
    }

    @GetMapping
    public AdminQuizResponse get(Authentication authentication, @PathVariable Long lessonId) {
        return service.findByLesson(authentication.getName(), lessonId);
    }

    @PutMapping
    public AdminQuizResponse save(
            Authentication authentication,
            @PathVariable Long lessonId,
            @Valid @RequestBody AdminQuizRequest request
    ) {
        return service.save(authentication.getName(), lessonId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long lessonId) {
        service.delete(authentication.getName(), lessonId);
    }
}
