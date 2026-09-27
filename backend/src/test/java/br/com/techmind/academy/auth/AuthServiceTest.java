package br.com.techmind.academy.auth;

import br.com.techmind.academy.security.JwtService;
import br.com.techmind.academy.settings.PlatformSettingsService;
import br.com.techmind.academy.user.User;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void shouldRegisterUserAndReturnToken() {
        var repository = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var jwt = mock(JwtService.class);
        var settings = mock(PlatformSettingsService.class);

        when(settings.isRegistrationEnabled()).thenReturn(true);
        when(repository.existsByEmail("aluno@techmind.dev")).thenReturn(false);
        when(encoder.encode("senha123")).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwt.generate(any(User.class))).thenReturn(new JwtService.TokenResult("token-jwt", 7200));

        var service = new AuthService(repository, encoder, jwt, settings);
        var response = service.register(new RegisterRequest("Aluno Tech", "ALUNO@TECHMIND.DEV", "senha123"));

        assertThat(response.accessToken()).isEqualTo("token-jwt");
        assertThat(response.user().email()).isEqualTo("aluno@techmind.dev");
        assertThat(response.user().role()).isEqualTo(UserRole.STUDENT.name());
        verify(repository).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenDisabledByPlatformSettings() {
        var repository = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var jwt = mock(JwtService.class);
        var settings = mock(PlatformSettingsService.class);

        when(settings.isRegistrationEnabled()).thenReturn(false);

        var service = new AuthService(repository, encoder, jwt, settings);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.register(
                        new RegisterRequest(
                                "Aluno Tech",
                                "aluno@techmind.dev",
                                "senha123"
                        )
                )
        )
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("temporariamente desabilitados");

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginWithValidCredentials() {
        var repository = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var jwt = mock(JwtService.class);
        var settings = mock(PlatformSettingsService.class);
        var user = User.builder().id(7L).name("Aluno").email("aluno@techmind.dev").passwordHash("hash").role(UserRole.STUDENT).build();

        when(repository.findByEmail("aluno@techmind.dev")).thenReturn(Optional.of(user));
        when(encoder.matches("senha123", "hash")).thenReturn(true);
        when(jwt.generate(user)).thenReturn(new JwtService.TokenResult("token-login", 7200));

        var service = new AuthService(repository, encoder, jwt, settings);
        var response = service.login(new LoginRequest("aluno@techmind.dev", "senha123"));

        assertThat(response.accessToken()).isEqualTo("token-login");
        assertThat(response.user().id()).isEqualTo(7L);
    }
}
