package br.com.techmind.academy.settings;

import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlatformSettingsService {

    private static final short SETTINGS_ID = 1;

    private final PlatformSettingsRepository repository;
    private final UserRepository userRepository;

    public PlatformSettingsService(
            PlatformSettingsRepository repository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminPlatformSettingsResponse adminView(String email) {
        requireAdmin(email);
        return AdminPlatformSettingsResponse.from(current());
    }

    @Transactional(readOnly = true)
    public PublicPlatformSettingsResponse publicView() {
        return PublicPlatformSettingsResponse.from(current());
    }

    @Transactional
    public AdminPlatformSettingsResponse update(
            String email,
            UpdatePlatformSettingsRequest request
    ) {
        requireAdmin(email);

        var settings = current();
        settings.setAcademyName(request.academyName().trim());
        settings.setTagline(request.tagline().trim());
        settings.setSupportEmail(normalizeNullable(request.supportEmail()));
        settings.setRegistrationEnabled(request.registrationEnabled());
        settings.setDefaultLessonXp(request.defaultLessonXp());
        settings.setDefaultQuizPassingScore(request.defaultQuizPassingScore());
        settings.setDefaultQuizXp(request.defaultQuizXp());

        return AdminPlatformSettingsResponse.from(repository.save(settings));
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        return Boolean.TRUE.equals(current().getRegistrationEnabled());
    }

    @Transactional(readOnly = true)
    public int defaultLessonXp() {
        return current().getDefaultLessonXp();
    }

    @Transactional(readOnly = true)
    public int defaultQuizPassingScore() {
        return current().getDefaultQuizPassingScore();
    }

    @Transactional(readOnly = true)
    public int defaultQuizXp() {
        return current().getDefaultQuizXp();
    }

    private PlatformSettings current() {
        return repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Configurações da plataforma não foram inicializadas"
                ));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
