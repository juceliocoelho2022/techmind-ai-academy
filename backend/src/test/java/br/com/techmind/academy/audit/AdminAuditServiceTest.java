package br.com.techmind.academy.audit;

import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuditServiceTest {

    @Test
    void shouldRecordAdministrativeActionWithoutSensitivePayload() {
        var repository = mock(AdminAuditLogRepository.class);
        var userRepository = mock(UserRepository.class);

        when(repository.save(any(AdminAuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new AdminAuditService(repository, userRepository);

        service.record(
                "admin@techmind.dev",
                "UPDATE",
                "COURSE",
                10L,
                "Trilha atualizada: Java"
        );

        verify(repository).save(argThat(log ->
                log.getActorEmail().equals("admin@techmind.dev") &&
                log.getAction().equals("UPDATE") &&
                log.getEntityType().equals("COURSE") &&
                log.getEntityId().equals("10") &&
                log.getSummary().equals("Trilha atualizada: Java")
        ));
    }

    @Test
    void shouldReturnRecentLogsToAdmin() {
        var repository = mock(AdminAuditLogRepository.class);
        var userRepository = mock(UserRepository.class);

        var admin = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@techmind.dev")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .build();

        var log = AdminAuditLog.builder()
                .id(99L)
                .actorEmail("admin@techmind.dev")
                .action("CREATE")
                .entityType("LESSON")
                .entityId("123")
                .summary("Aula criada: Streams")
                .createdAt(OffsetDateTime.now())
                .build();

        when(userRepository.findByEmail("admin@techmind.dev"))
                .thenReturn(Optional.of(admin));
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(log)));

        var service = new AdminAuditService(repository, userRepository);

        var result = service.recent("admin@techmind.dev", 20);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().action()).isEqualTo("CREATE");
        assertThat(result.getFirst().entityType()).isEqualTo("LESSON");
        assertThat(result.getFirst().entityId()).isEqualTo("123");
    }
}
