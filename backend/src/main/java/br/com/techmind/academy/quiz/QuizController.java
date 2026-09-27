package br.com.techmind.academy.quiz;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/lessons/{lessonId}/quiz")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @GetMapping
    public StudentQuizResponse get(Authentication authentication, @PathVariable Long lessonId) {
        return service.getQuiz(authentication.getName(), lessonId);
    }

    @GetMapping("/attempts")
    public List<QuizAttemptSummaryResponse> history(
            Authentication authentication,
            @PathVariable Long lessonId
    ) {
        return service.history(authentication.getName(), lessonId);
    }

    @PostMapping("/attempts")
    public QuizAttemptResponse submit(
            Authentication authentication,
            @PathVariable Long lessonId,
            @Valid @RequestBody SubmitQuizRequest request
    ) {
        return service.submit(authentication.getName(), lessonId, request);
    }
}
