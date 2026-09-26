package br.com.techmind.academy.enrollment;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/me")
    public List<EnrollmentResponse> mine(Authentication authentication) {
        return enrollmentService.listMine(authentication.getName());
    }

    @PostMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(
            Authentication authentication,
            @PathVariable Long courseId
    ) {
        return enrollmentService.enroll(authentication.getName(), courseId);
    }
}
