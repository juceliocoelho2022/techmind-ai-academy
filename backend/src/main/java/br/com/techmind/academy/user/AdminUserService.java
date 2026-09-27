package br.com.techmind.academy.user;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.subscription.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubscriptionService subscriptionService;
    private final AdminAuditService auditService;

    public AdminUserService(
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            SubscriptionService subscriptionService,
            AdminAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subscriptionService = subscriptionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list(String currentEmail) {
        requireAdmin(currentEmail);

        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(user -> toResponse(user, currentEmail))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateRole(
            String currentEmail,
            Long userId,
            AdminUserRoleRequest request
    ) {
        var currentAdmin = requireAdmin(currentEmail);

        var target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));

        if (target.getId().equals(currentAdmin.getId()) && request.role() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Você não pode remover o papel ADMIN da própria conta"
            );
        }

        if (
                target.getRole() == UserRole.ADMIN &&
                request.role() != UserRole.ADMIN &&
                userRepository.countByRole(UserRole.ADMIN) <= 1
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A plataforma precisa manter pelo menos um administrador"
            );
        }

        var previousRole = target.getRole();
        target.setRole(request.role());
        var saved = userRepository.save(target);

        auditService.record(
                currentEmail,
                "ROLE_CHANGE",
                "USER",
                saved.getId(),
                "Papel alterado de " + previousRole.name() + " para " + saved.getRole().name() + " em " + saved.getEmail()
        );

        return toResponse(saved, currentEmail);
    }

    private AdminUserResponse toResponse(User user, String currentEmail) {
        var enrollments = enrollmentRepository.findByUserIdOrderByStartedAtDesc(user.getId());

        int completedLessons = enrollments.stream()
                .mapToInt(enrollment -> enrollment.getCompletedLessons() == null ? 0 : enrollment.getCompletedLessons())
                .sum();

        int xp = enrollments.stream()
                .mapToInt(enrollment -> enrollment.getXp() == null ? 0 : enrollment.getXp())
                .sum();

        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                subscriptionService.currentPlanForUser(user.getId()).name(),
                user.getCreatedAt(),
                enrollments.size(),
                completedLessons,
                xp,
                user.getEmail().equalsIgnoreCase(currentEmail)
        );
    }

    private User requireAdmin(String email) {
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

        return user;
    }
}
