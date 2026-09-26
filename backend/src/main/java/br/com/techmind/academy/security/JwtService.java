package br.com.techmind.academy.security;

import br.com.techmind.academy.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.security.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public TokenResult generate(User user) {
        var now = Instant.now();
        var expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        var claims = JwtClaimsSet.builder()
                .issuer("techmind-ai-academy")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResult(token, expirationMinutes * 60);
    }

    public record TokenResult(String value, long expiresInSeconds) {}
}
