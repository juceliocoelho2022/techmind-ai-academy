package br.com.techmind.academy.user;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {

    @Test
    void shouldListUsersWithEnrollmentActivity() {
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var student = User.builder()
                .id(2L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();

        var course = Course.builder()
                .id(10L)
                .slug("java-backend")
                .title("Java Backend")
                .description("Java")
                .totalLessons(20)
                .build();

        var enrollment = Enrollment.builder()
                .id(100L)
                .user(student)
                .course(course)
                .completedLessons(4)
                .xp(90)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(userRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(student, admin));
        when(enrollmentRepository.findByUserIdOrderByStartedAtDesc(2L))
                .thenReturn(List.of(enrollment));
        when(enrollmentRepository.findByUserIdOrderByStartedAtDesc(1L))
                .thenReturn(List.of());

        var service = new AdminUserService(userRepository, enrollmentRepository);

        var users = service.list("admin@techmind.dev");

        assertThat(users).hasSize(2);

        var studentResponse = users.get(0);
        assertThat(studentResponse.email()).isEqualTo("aluno@techmind.dev");
        assertThat(studentResponse.enrollments()).isEqualTo(1);
        assertThat(studentResponse.completedLessons()).isEqualTo(4);
        assertThat(studentResponse.xp()).isEqualTo(90);
        assertThat(studentResponse.currentUser()).isFalse();

        var adminResponse = users.get(1);
        assertThat(adminResponse.currentUser()).isTrue();
        assertThat(adminResponse.role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldPromoteStudentToAdmin() {
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var student = User.builder()
                .id(2L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(student));
        when(userRepository.save(student))
                .thenReturn(student);
        when(enrollmentRepository.findByUserIdOrderByStartedAtDesc(2L))
                .thenReturn(List.of());

        var service = new AdminUserService(userRepository, enrollmentRepository);

        var response = service.updateRole(
                "admin@techmind.dev",
                2L,
                new AdminUserRoleRequest(UserRole.ADMIN)
        );

        assertThat(student.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.role()).isEqualTo("ADMIN");
        verify(userRepository).save(student);
    }

    @Test
    void shouldPreventAdminFromDemotingOwnAccount() {
        var userRepository = mock(UserRepository.class);
        var enrollmentRepository = mock(EnrollmentRepository.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        var service = new AdminUserService(userRepository, enrollmentRepository);

        assertThatThrownBy(() ->
                service.updateRole(
                        "admin@techmind.dev",
                        1L,
                        new AdminUserRoleRequest(UserRole.STUDENT)
                )
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("própria conta");

        verify(userRepository, never()).save(any());
    }
}
