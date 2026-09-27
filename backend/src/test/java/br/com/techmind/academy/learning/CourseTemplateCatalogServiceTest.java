package br.com.techmind.academy.learning;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseTemplateCatalogServiceTest {

    @Test
    void shouldInstantiateJavaTemplateWithStarterStructure() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var userRepository = mock(UserRepository.class);
        var auditService = mock(AdminAuditService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(courseRepository.findBySlug("java-backend-pro"))
                .thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class)))
                .thenAnswer(invocation -> {
                    var course = invocation.getArgument(0, Course.class);
                    course.setId(100L);
                    return course;
                });
        when(moduleRepository.save(any(CourseModule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new CourseTemplateCatalogService(
                courseRepository,
                moduleRepository,
                lessonRepository,
                auditService,
                userRepository
        );

        var response = service.instantiate(
                "admin@techmind.dev",
                "java-backend",
                new CourseTemplateInstantiateRequest(
                        "Java Backend Profissional",
                        "java-backend-pro",
                        "Trilha completa de Java Backend.",
                        CourseLevel.ADVANCED,
                        true
                )
        );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Java Backend Profissional");
        assertThat(response.category()).isEqualTo("Backend");
        assertThat(response.technology()).isEqualTo("Java");
        assertThat(response.level()).isEqualTo(CourseLevel.ADVANCED);
        assertThat(response.totalLessons()).isEqualTo(8);

        verify(moduleRepository, times(4)).save(any(CourseModule.class));
        verify(lessonRepository, times(4)).saveAll(any());
        verify(auditService).record(
                eq("admin@techmind.dev"),
                eq("TEMPLATE_INSTANTIATE"),
                eq("COURSE"),
                eq(100L),
                contains("java-backend")
        );
    }

    @Test
    void shouldExposeJavaInProgrammingCategory() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var userRepository = mock(UserRepository.class);
        var auditService = mock(AdminAuditService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));

        var service = new CourseTemplateCatalogService(
                courseRepository,
                moduleRepository,
                lessonRepository,
                auditService,
                userRepository
        );

        var java = service.catalog("admin@techmind.dev").stream()
                .filter(template -> template.key().equals("java-programming"))
                .findFirst()
                .orElseThrow();

        assertThat(java.category()).isEqualTo("Programação");
        assertThat(java.technology()).isEqualTo("Java");
        assertThat(java.level()).isEqualTo(CourseLevel.BEGINNER);
        assertThat(java.modules()).hasSize(4);
        assertThat(java.totalLessons()).isEqualTo(8);
    }

    @Test
    void shouldCreateCourseWithoutStarterStructureWhenDisabled() {
        var courseRepository = mock(CourseRepository.class);
        var moduleRepository = mock(CourseModuleRepository.class);
        var lessonRepository = mock(LessonRepository.class);
        var userRepository = mock(UserRepository.class);
        var auditService = mock(AdminAuditService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(courseRepository.findBySlug("react-custom"))
                .thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new CourseTemplateCatalogService(
                courseRepository,
                moduleRepository,
                lessonRepository,
                auditService,
                userRepository
        );

        service.instantiate(
                "admin@techmind.dev",
                "react-js",
                new CourseTemplateInstantiateRequest(
                        "React Custom",
                        "react-custom",
                        "React para um público específico.",
                        CourseLevel.BEGINNER,
                        false
                )
        );

        verify(moduleRepository, never()).save(any());
        verify(lessonRepository, never()).saveAll(any());
    }
}
