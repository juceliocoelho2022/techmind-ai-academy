package br.com.techmind.academy.interview;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {
    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public InterviewFeedback evaluate(@Valid @RequestBody InterviewRequest request) {
        return service.evaluate(request);
    }
}
