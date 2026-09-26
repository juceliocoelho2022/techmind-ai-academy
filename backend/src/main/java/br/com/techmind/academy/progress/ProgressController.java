package br.com.techmind.academy.progress;

import br.com.techmind.academy.enrollment.EnrollmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {
    private final EnrollmentRepository enrollmentRepository;

    public ProgressController(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/me")
    public ProgressResponse myProgress(Authentication authentication) {
        var enrollments = enrollmentRepository.findByUserEmailOrderByStartedAtDesc(authentication.getName());
        long completed = enrollments.stream().mapToLong(e -> Math.min(e.getCompletedLessons(), e.getCourse().getTotalLessons())).sum();
        long total = enrollments.stream().mapToLong(e -> e.getCourse().getTotalLessons()).sum();
        int xp = enrollments.stream().mapToInt(e -> e.getXp()).sum();
        double percentage = total == 0 ? 0 : completed * 100.0 / total;
        return new ProgressResponse(completed, total, percentage, xp, levelFor(xp));
    }

    private String levelFor(int xp) {
        if (xp >= 3000) return "TechMind Pro";
        if (xp >= 1500) return "Backend Builder";
        if (xp >= 500) return "Code Explorer";
        return "Iniciando jornada";
    }
}
