package br.com.techmind.academy.subscription;

import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseEntitlementServiceTest {

    @Test
    void shouldDenyFreeUserOnProCourse() {
        var userRepository = mock(UserRepository.class);
        var subscriptionService = mock(SubscriptionService.class);

        var user = student();
        var course = course(SubscriptionPlan.PRO);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(subscriptionService.currentPlanForUser(user.getId()))
                .thenReturn(SubscriptionPlan.FREE);

        var service = new CourseEntitlementService(userRepository, subscriptionService);

        assertThatThrownBy(() -> service.requireAccess(user, course))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("requer o plano PRO")
                .hasMessageContaining("FREE");
    }

    @Test
    void shouldAllowCareerUserOnProCourse() {
        var userRepository = mock(UserRepository.class);
        var subscriptionService = mock(SubscriptionService.class);

        var user = student();
        var course = course(SubscriptionPlan.PRO);

        when(subscriptionService.currentPlanForUser(user.getId()))
                .thenReturn(SubscriptionPlan.CAREER);

        var service = new CourseEntitlementService(userRepository, subscriptionService);

        assertThat(service.hasAccess(user, course)).isTrue();
    }

    @Test
    void shouldAllowAdminRegardlessOfPlan() {
        var userRepository = mock(UserRepository.class);
        var subscriptionService = mock(SubscriptionService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var service = new CourseEntitlementService(userRepository, subscriptionService);

        assertThat(service.hasAccess(admin, course(SubscriptionPlan.CAREER))).isTrue();
    }

    private User student() {
        return User.builder()
                .id(7L)
                .name("Aluno")
                .email("aluno@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.STUDENT)
                .build();
    }

    private Course course(SubscriptionPlan requiredPlan) {
        return Course.builder()
                .id(10L)
                .slug("premium")
                .title("Premium")
                .description("Curso premium")
                .requiredPlan(requiredPlan)
                .totalLessons(8)
                .build();
    }
}
