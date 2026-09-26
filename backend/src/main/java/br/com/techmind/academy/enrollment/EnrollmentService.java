package br.com.techmind.academy.enrollment;

import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, UserRepository userRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EnrollmentResponse enroll(String email, Long courseId) {
        var existing = enrollmentRepository.findByUserEmailAndCourseId(email, courseId);
        if (existing.isPresent()) return EnrollmentResponse.from(existing.get());

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trilha não encontrada"));

        var enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .completedLessons(0)
                .xp(0)
                .build();

        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listMine(String email) {
        return enrollmentRepository.findByUserEmailOrderByStartedAtDesc(email)
                .stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional
    public EnrollmentResponse updateProgress(String email, Long courseId, UpdateProgressRequest request) {
        var enrollment = enrollmentRepository.findByUserEmailAndCourseId(email, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));

        int cappedCompleted = Math.min(request.completedLessons(), enrollment.getCourse().getTotalLessons());
        enrollment.setCompletedLessons(cappedCompleted);
        enrollment.setXp(request.xp());
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }
}
