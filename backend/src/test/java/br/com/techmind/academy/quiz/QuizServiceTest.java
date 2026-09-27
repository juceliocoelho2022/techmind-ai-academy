package br.com.techmind.academy.quiz;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.learning.CourseModule;
import br.com.techmind.academy.learning.Lesson;
import br.com.techmind.academy.subscription.CourseEntitlementService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    @Test
    void shouldAwardXpOnFirstPassingAttempt() {
        var quizRepository = mock(LessonQuizRepository.class);
        var attemptRepository = mock(QuizAttemptRepository.class);
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var entitlementService = mock(CourseEntitlementService.class);

        var fixture = fixture();

        when(userRepository.findByEmail("aluno@techmind.dev"))
                .thenReturn(Optional.of(fixture.user));
        when(quizRepository.findByLessonId(100L))
                .thenReturn(Optional.of(fixture.quiz));
        when(enrollmentRepository.findForUpdateByUserEmailAndCourseId("aluno@techmind.dev", 1L))
                .thenReturn(Optional.of(fixture.enrollment));
        when(attemptRepository.existsByQuizIdAndUserIdAndPassedTrue(900L, 7L))
                .thenReturn(false);
        when(attemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new QuizService(
                quizRepository,
                attemptRepository,
                userRepository,
                enrollmentRepository,
                entitlementService
        );

        var response = service.submit(
                "aluno@techmind.dev",
                100L,
                new SubmitQuizRequest(List.of(
                        new SubmitQuizRequest.Answer(201L, 301L),
                        new SubmitQuizRequest.Answer(202L, 304L)
                ))
        );

        assertThat(response.passed()).isTrue();
        assertThat(response.score()).isEqualByComparingTo("100.00");
        assertThat(response.correctAnswers()).isEqualTo(2);
        assertThat(response.xpAwarded()).isEqualTo(50);
        assertThat(fixture.enrollment.getXp()).isEqualTo(150);

        verify(enrollmentRepository).save(fixture.enrollment);
        verify(attemptRepository).save(any(QuizAttempt.class));
    }

    @Test
    void shouldNotAwardXpAgainAfterPreviousApproval() {
        var quizRepository = mock(LessonQuizRepository.class);
        var attemptRepository = mock(QuizAttemptRepository.class);
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var entitlementService = mock(CourseEntitlementService.class);

        var fixture = fixture();

        when(userRepository.findByEmail("aluno@techmind.dev"))
                .thenReturn(Optional.of(fixture.user));
        when(quizRepository.findByLessonId(100L))
                .thenReturn(Optional.of(fixture.quiz));
        when(enrollmentRepository.findForUpdateByUserEmailAndCourseId("aluno@techmind.dev", 1L))
                .thenReturn(Optional.of(fixture.enrollment));
        when(attemptRepository.existsByQuizIdAndUserIdAndPassedTrue(900L, 7L))
                .thenReturn(true);
        when(attemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new QuizService(
                quizRepository,
                attemptRepository,
                userRepository,
                enrollmentRepository,
                entitlementService
        );

        var response = service.submit(
                "aluno@techmind.dev",
                100L,
                new SubmitQuizRequest(List.of(
                        new SubmitQuizRequest.Answer(201L, 301L),
                        new SubmitQuizRequest.Answer(202L, 304L)
                ))
        );

        assertThat(response.passed()).isTrue();
        assertThat(response.xpAwarded()).isZero();
        assertThat(fixture.enrollment.getXp()).isEqualTo(100);

        verify(enrollmentRepository, never()).save(any());
        verify(attemptRepository).save(any(QuizAttempt.class));
    }

    private Fixture fixture() {
        var course = Course.builder()
                .id(1L)
                .slug("java-backend")
                .title("Java Backend")
                .description("Java")
                .totalLessons(20)
                .build();

        var module = CourseModule.builder()
                .id(10L)
                .course(course)
                .title("Fundamentos")
                .description("Base Java")
                .position(1)
                .build();

        var lesson = Lesson.builder()
                .id(100L)
                .module(module)
                .slug("java-21")
                .title("Java 21")
                .summary("Java moderno")
                .position(1)
                .xpReward(10)
                .build();

        var question1 = QuizQuestion.builder()
                .id(201L)
                .prompt("Qual versão?")
                .position(1)
                .build();
        question1.replaceOptions(List.of(
                QuizOption.builder().id(301L).text("Java 21").position(1).correct(true).build(),
                QuizOption.builder().id(302L).text("Java 8").position(2).correct(false).build()
        ));

        var question2 = QuizQuestion.builder()
                .id(202L)
                .prompt("Qual coleção não aceita duplicidade?")
                .position(2)
                .build();
        question2.replaceOptions(List.of(
                QuizOption.builder().id(303L).text("List").position(1).correct(false).build(),
                QuizOption.builder().id(304L).text("Set").position(2).correct(true).build()
        ));

        var quiz = LessonQuiz.builder()
                .id(900L)
                .lesson(lesson)
                .title("Quiz Java")
                .description("Fundamentos")
                .passingScore(70)
                .xpReward(50)
                .active(true)
                .build();
        quiz.replaceQuestions(List.of(question1, question2));

        var user = User.builder()
                .id(7L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();

        var enrollment = Enrollment.builder()
                .id(50L)
                .user(user)
                .course(course)
                .completedLessons(0)
                .xp(100)
                .build();

        return new Fixture(quiz, user, enrollment);
    }

    private record Fixture(LessonQuiz quiz, User user, Enrollment enrollment) {}
}
