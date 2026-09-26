package br.com.techmind.academy.auth;

import br.com.techmind.academy.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
