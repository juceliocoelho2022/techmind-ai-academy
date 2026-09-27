package br.com.techmind.academy.analytics;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.learning.LessonProgress;
import br.com.techmind.academy.learning.LessonProgressRepository;
import br.com.techmind.academy.quiz.LessonQuiz;
import br.com.techmind.academy.quiz.LessonQuizRepository;
import br.com.techmind.academy.quiz.QuizAttempt;
import br.com.techmind.academy.quiz.QuizAttemptRepository;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonQuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonQuizRepository quizRepository,
            QuizAttemptRepository attemptRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse snapshot(String email) {
        requireAdmin(email);

        var courses = courseRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        var enrollments = enrollmentRepository.findAllForAnalytics();
        var progress = lessonProgressRepository.findAllForAnalytics();
        var quizzes = quizRepository.findAllForAnalytics();
        var attempts = attemptRepository.findAllForAnalytics();

        Map<Long, List<Enrollment>> enrollmentsByCourse = enrollments.stream()
                .collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getId()));

        Map<Long, List<LessonProgress>> progressByCourse = progress.stream()
                .collect(Collectors.groupingBy(item -> item.getLesson().getModule().getCourse().getId()));

        Map<Long, List<LessonQuiz>> quizzesByCourse = quizzes.stream()
                .collect(Collectors.groupingBy(quiz -> quiz.getLesson().getModule().getCourse().getId()));

        Map<Long, List<QuizAttempt>> attemptsByCourse = attempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getQuiz().getLesson().getModule().getCourse().getId()
                ));

        var coursePerformance = courses.stream()
                .map(course -> coursePerformance(
                        course,
                        enrollmentsByCourse.getOrDefault(course.getId(), List.of()),
                        progressByCourse.getOrDefault(course.getId(), List.of()),
                        quizzesByCourse.getOrDefault(course.getId(), List.of()),
                        attemptsByCourse.getOrDefault(course.getId(), List.of())
                ))
                .toList();

        long totalXp = enrollments.stream()
                .mapToLong(enrollment -> value(enrollment.getXp()))
                .sum();

        long passedAttempts = attempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getPassed()))
                .count();

        var summary = new AdminAnalyticsResponse.PlatformSummary(
                userRepository.count(),
                userRepository.countByRole(UserRole.STUDENT),
                userRepository.countByRole(UserRole.ADMIN),
                courses.size(),
                enrollments.size(),
                progress.size(),
                totalXp,
                quizzes.size(),
                attempts.size(),
                averageScore(attempts),
                percentage(passedAttempts, attempts.size())
        );

        return new AdminAnalyticsResponse(
                OffsetDateTime.now(),
                summary,
                coursePerformance
        );
    }

    private AdminAnalyticsResponse.CoursePerformance coursePerformance(
            Course course,
            List<Enrollment> enrollments,
            List<LessonProgress> progress,
            List<LessonQuiz> quizzes,
            List<QuizAttempt> attempts
    ) {
        long totalXp = enrollments.stream()
                .mapToLong(enrollment -> value(enrollment.getXp()))
                .sum();

        long passedAttempts = attempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getPassed()))
                .count();

        long expectedLessonCompletions =
                (long) enrollments.size() * Math.max(value(course.getTotalLessons()), 0);

        return new AdminAnalyticsResponse.CoursePerformance(
                course.getId(),
                course.getTitle(),
                course.getCategory(),
                course.getTechnology(),
                course.getLevel().name(),
                value(course.getTotalLessons()),
                enrollments.size(),
                progress.size(),
                percentage(progress.size(), expectedLessonCompletions),
                totalXp,
                quizzes.size(),
                attempts.size(),
                averageScore(attempts),
                percentage(passedAttempts, attempts.size())
        );
    }

    private double averageScore(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;

        double average = attempts.stream()
                .map(QuizAttempt::getScore)
                .filter(score -> score != null)
                .mapToDouble(score -> score.doubleValue())
                .average()
                .orElse(0);

        return round(average);
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito ao administrador"
            );
        }
    }
}
