package br.com.techmind.academy.analytics;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.learning.CourseModule;
import br.com.techmind.academy.learning.Lesson;
import br.com.techmind.academy.learning.LessonProgress;
import br.com.techmind.academy.learning.LessonProgressRepository;
import br.com.techmind.academy.quiz.LessonQuiz;
import br.com.techmind.academy.quiz.LessonQuizRepository;
import br.com.techmind.academy.quiz.QuizAttempt;
import br.com.techmind.academy.quiz.QuizAttemptRepository;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAnalyticsServiceTest {

    @Test
    void shouldCalculatePlatformAndCourseAnalyticsFromRealActivity() {
        var userRepository = mock(UserRepository.class);
        var courseRepository = mock(CourseRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var progressRepository = mock(LessonProgressRepository.class);
        var quizRepository = mock(LessonQuizRepository.class);
        var attemptRepository = mock(QuizAttemptRepository.class);

        var admin = user(1L, "Admin", "admin@techmind.dev", UserRole.ADMIN);
        var studentA = user(2L, "Aluno A", "a@techmind.dev", UserRole.STUDENT);
        var studentB = user(3L, "Aluno B", "b@techmind.dev", UserRole.STUDENT);

        var java = Course.builder()
                .id(10L)
                .slug("java")
                .title("Java")
                .description("Java")
                .category("Programação")
                .technology("Java")
                .level(CourseLevel.BEGINNER)
                .totalLessons(4)
                .build();

        var react = Course.builder()
                .id(20L)
                .slug("react")
                .title("React")
                .description("React")
                .category("Frontend")
                .technology("React.js")
                .level(CourseLevel.BEGINNER)
                .totalLessons(2)
                .build();

        var javaModule = module(100L, java, "Java");
        var reactModule = module(200L, react, "React");
        var javaLesson = lesson(1000L, javaModule, "java-1");
        var reactLesson = lesson(2000L, reactModule, "react-1");

        var enrollments = List.of(
                enrollment(1L, studentA, java, 100),
                enrollment(2L, studentB, java, 50),
                enrollment(3L, studentA, react, 20)
        );

        var progress = List.of(
                progress(1L, studentA, javaLesson),
                progress(2L, studentA, javaLesson),
                progress(3L, studentB, javaLesson),
                progress(4L, studentA, reactLesson)
        );

        var javaQuiz = quiz(500L, javaLesson);
        var reactQuiz = quiz(600L, reactLesson);

        var attempts = List.of(
                attempt(1L, javaQuiz, studentA, "80.00", true),
                attempt(2L, javaQuiz, studentB, "60.00", false),
                attempt(3L, reactQuiz, studentA, "100.00", true)
        );

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(userRepository.count()).thenReturn(4L);
        when(userRepository.countByRole(UserRole.STUDENT)).thenReturn(3L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);
        when(courseRepository.findAll()).thenReturn(List.of(java, react));
        when(enrollmentRepository.findAllForAnalytics()).thenReturn(enrollments);
        when(progressRepository.findAllForAnalytics()).thenReturn(progress);
        when(quizRepository.findAllForAnalytics()).thenReturn(List.of(javaQuiz, reactQuiz));
        when(attemptRepository.findAllForAnalytics()).thenReturn(attempts);

        var service = new AdminAnalyticsService(
                userRepository,
                courseRepository,
                enrollmentRepository,
                progressRepository,
                quizRepository,
                attemptRepository
        );

        var response = service.snapshot("admin@techmind.dev");

        assertThat(response.summary().totalUsers()).isEqualTo(4);
        assertThat(response.summary().students()).isEqualTo(3);
        assertThat(response.summary().admins()).isEqualTo(1);
        assertThat(response.summary().courses()).isEqualTo(2);
        assertThat(response.summary().enrollments()).isEqualTo(3);
        assertThat(response.summary().completedLessons()).isEqualTo(4);
        assertThat(response.summary().totalXp()).isEqualTo(170);
        assertThat(response.summary().quizzes()).isEqualTo(2);
        assertThat(response.summary().quizAttempts()).isEqualTo(3);
        assertThat(response.summary().averageQuizScore()).isEqualTo(80.0);
        assertThat(response.summary().quizApprovalRate()).isEqualTo(66.67);

        var javaAnalytics = response.courses().stream()
                .filter(item -> item.courseId().equals(10L))
                .findFirst()
                .orElseThrow();

        assertThat(javaAnalytics.enrollments()).isEqualTo(2);
        assertThat(javaAnalytics.completedLessons()).isEqualTo(3);
        assertThat(javaAnalytics.completionRate()).isEqualTo(37.5);
        assertThat(javaAnalytics.totalXp()).isEqualTo(150);
        assertThat(javaAnalytics.quizzes()).isEqualTo(1);
        assertThat(javaAnalytics.quizAttempts()).isEqualTo(2);
        assertThat(javaAnalytics.averageQuizScore()).isEqualTo(70.0);
        assertThat(javaAnalytics.quizApprovalRate()).isEqualTo(50.0);
    }

    private User user(Long id, String name, String email, UserRole role) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .passwordHash("hash")
                .role(role)
                .build();
    }

    private CourseModule module(Long id, Course course, String title) {
        return CourseModule.builder()
                .id(id)
                .course(course)
                .title(title)
                .description(title)
                .position(1)
                .build();
    }

    private Lesson lesson(Long id, CourseModule module, String slug) {
        return Lesson.builder()
                .id(id)
                .module(module)
                .slug(slug)
                .title(slug)
                .summary(slug)
                .position(1)
                .xpReward(10)
                .build();
    }

    private Enrollment enrollment(Long id, User user, Course course, int xp) {
        return Enrollment.builder()
                .id(id)
                .user(user)
                .course(course)
                .completedLessons(0)
                .xp(xp)
                .build();
    }

    private LessonProgress progress(Long id, User user, Lesson lesson) {
        return LessonProgress.builder()
                .id(id)
                .user(user)
                .lesson(lesson)
                .xpAwarded(10)
                .build();
    }

    private LessonQuiz quiz(Long id, Lesson lesson) {
        return LessonQuiz.builder()
                .id(id)
                .lesson(lesson)
                .title("Quiz")
                .passingScore(70)
                .xpReward(50)
                .active(true)
                .build();
    }

    private QuizAttempt attempt(
            Long id,
            LessonQuiz quiz,
            User user,
            String score,
            boolean passed
    ) {
        return QuizAttempt.builder()
                .id(id)
                .quiz(quiz)
                .user(user)
                .score(new BigDecimal(score))
                .correctAnswers(passed ? 1 : 0)
                .totalQuestions(1)
                .passed(passed)
                .xpAwarded(passed ? 50 : 0)
                .build();
    }
}
