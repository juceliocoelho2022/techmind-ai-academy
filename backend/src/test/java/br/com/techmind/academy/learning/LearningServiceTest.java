package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LearningServiceTest {

    @Test
    void shouldGroupCurriculumByModule() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var progressRepository = mock(LessonProgressRepository.class);
        var resourceRepository = mock(LessonResourceRepository.class);

        var course = Course.builder()
                .id(1L)
                .slug("java-backend")
                .title("Java Backend")
                .description("Java")
                .totalLessons(20)
                .build();

        var fundamentals = CourseModule.builder()
                .id(10L)
                .course(course)
                .title("Fundamentos")
                .description("Base Java")
                .position(1)
                .build();

        var api = CourseModule.builder()
                .id(11L)
                .course(course)
                .title("APIs")
                .description("REST")
                .position(2)
                .build();

        var lesson1 = Lesson.builder()
                .id(100L)
                .module(fundamentals)
                .slug("java-21")
                .title("Java 21")
                .summary("Java moderno")
                .position(1)
                .xpReward(10)
                .build();

        var lesson2 = Lesson.builder()
                .id(101L)
                .module(api)
                .slug("rest")
                .title("REST")
                .summary("APIs REST")
                .position(1)
                .xpReward(15)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(fundamentals, api));
        when(lessonRepository.findCurriculumByCourseId(1L)).thenReturn(List.of(lesson1, lesson2));

        var service = new LearningService(
                courseRepository,
                moduleRepository,
                userRepository,
                enrollmentRepository,
                lessonRepository,
                progressRepository,
                resourceRepository
        );

        var response = service.curriculum(1L);

        assertThat(response.modules()).hasSize(2);
        assertThat(response.availableLessons()).isEqualTo(2);
        assertThat(response.modules().get(0).lessons().get(0).title()).isEqualTo("Java 21");
    }

    @Test
    void shouldAwardXpOnlyOnFirstCompletion() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var progressRepository = mock(LessonProgressRepository.class);
        var resourceRepository = mock(LessonResourceRepository.class);

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
                .xpReward(25)
                .build();

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
                .completedLessons(3)
                .xp(100)
                .build();

        when(userRepository.findByEmail("aluno@techmind.dev")).thenReturn(Optional.of(user));
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findForUpdateByUserEmailAndCourseId("aluno@techmind.dev", 1L))
                .thenReturn(Optional.of(enrollment));
        when(progressRepository.findByUserIdAndLessonId(7L, 100L)).thenReturn(Optional.empty());
        when(progressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new LearningService(
                courseRepository,
                moduleRepository,
                userRepository,
                enrollmentRepository,
                lessonRepository,
                progressRepository,
                resourceRepository
        );

        var response = service.completeLesson("aluno@techmind.dev", 100L);

        assertThat(response.newlyCompleted()).isTrue();
        assertThat(response.xpAwarded()).isEqualTo(25);
        assertThat(response.courseCompletedLessons()).isEqualTo(4);
        assertThat(response.courseXp()).isEqualTo(125);

        verify(progressRepository).save(any(LessonProgress.class));
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void shouldBeIdempotentWhenLessonWasAlreadyCompleted() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var progressRepository = mock(LessonProgressRepository.class);
        var resourceRepository = mock(LessonResourceRepository.class);

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
                .xpReward(25)
                .build();

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
                .completedLessons(4)
                .xp(125)
                .build();

        var existingProgress = LessonProgress.builder()
                .id(70L)
                .user(user)
                .lesson(lesson)
                .xpAwarded(25)
                .build();

        when(userRepository.findByEmail("aluno@techmind.dev")).thenReturn(Optional.of(user));
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findForUpdateByUserEmailAndCourseId("aluno@techmind.dev", 1L))
                .thenReturn(Optional.of(enrollment));
        when(progressRepository.findByUserIdAndLessonId(7L, 100L))
                .thenReturn(Optional.of(existingProgress));

        var service = new LearningService(
                courseRepository,
                moduleRepository,
                userRepository,
                enrollmentRepository,
                lessonRepository,
                progressRepository,
                resourceRepository
        );

        var response = service.completeLesson("aluno@techmind.dev", 100L);

        assertThat(response.newlyCompleted()).isFalse();
        assertThat(response.xpAwarded()).isZero();
        assertThat(response.courseCompletedLessons()).isEqualTo(4);
        assertThat(response.courseXp()).isEqualTo(125);

        verify(progressRepository, never()).save(any());
        verify(enrollmentRepository, never()).save(any());
    }
}
