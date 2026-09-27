package br.com.techmind.academy.settings;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlatformSettingsServiceTest {

    @Test
    void shouldReturnPublicSettings() {
        var repository = mock(PlatformSettingsRepository.class);
        var userRepository = mock(UserRepository.class);
        var auditService = mock(AdminAuditService.class);

        when(repository.findById((short) 1))
                .thenReturn(Optional.of(settings()));

        var service = new PlatformSettingsService(repository, auditService, userRepository);

        var response = service.publicView();

        assertThat(response.academyName()).isEqualTo("TechMind AI Academy");
        assertThat(response.tagline()).isEqualTo("Do conteúdo ao projeto real.");
        assertThat(response.registrationEnabled()).isTrue();
    }

    @Test
    void shouldAllowAdminToUpdateSettings() {
        var repository = mock(PlatformSettingsRepository.class);
        var userRepository = mock(UserRepository.class);
        var auditService = mock(AdminAuditService.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var settings = settings();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(repository.findById((short) 1))
                .thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);

        var service = new PlatformSettingsService(repository, auditService, userRepository);

        var response = service.update(
                "admin@techmind.dev",
                new UpdatePlatformSettingsRequest(
                        "Nova Academy",
                        "Aprenda construindo.",
                        "suporte@example.com",
                        false,
                        20,
                        80,
                        100
                )
        );

        assertThat(response.academyName()).isEqualTo("Nova Academy");
        assertThat(response.registrationEnabled()).isFalse();
        assertThat(response.defaultLessonXp()).isEqualTo(20);
        assertThat(response.defaultQuizPassingScore()).isEqualTo(80);
        assertThat(response.defaultQuizXp()).isEqualTo(100);
        verify(repository).save(settings);
        verify(auditService).record(
                eq("admin@techmind.dev"),
                eq("SETTINGS_UPDATE"),
                eq("PLATFORM_SETTINGS"),
                eq((short) 1),
                contains("atualizadas")
        );
    }

    private PlatformSettings settings() {
        return PlatformSettings.builder()
                .id((short) 1)
                .academyName("TechMind AI Academy")
                .tagline("Do conteúdo ao projeto real.")
                .supportEmail(null)
                .registrationEnabled(true)
                .defaultLessonXp(10)
                .defaultQuizPassingScore(70)
                .defaultQuizXp(50)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
