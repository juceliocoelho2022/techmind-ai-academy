package br.com.techmind.academy.user;

import jakarta.validation.constraints.NotNull;

public record AdminUserRoleRequest(
        @NotNull UserRole role
) {
}
