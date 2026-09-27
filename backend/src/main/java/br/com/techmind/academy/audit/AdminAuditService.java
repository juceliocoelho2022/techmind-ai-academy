package br.com.techmind.academy.audit;

import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository repository;
    private final UserRepository userRepository;

    public AdminAuditService(
            AdminAuditLogRepository repository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(
            String actorEmail,
            String action,
            String entityType,
            Object entityId,
            String summary
    ) {
        repository.save(
                AdminAuditLog.builder()
                        .actorEmail(actorEmail)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId == null ? null : String.valueOf(entityId))
                        .summary(summary)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> recent(String email, int limit) {
        requireAdmin(email);

        int safeLimit = Math.max(1, Math.min(limit, 500));

        return repository.findAll(
                        PageRequest.of(
                                0,
                                safeLimit,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                        )
                )
                .stream()
                .map(AdminAuditLogResponse::from)
                .toList();
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
