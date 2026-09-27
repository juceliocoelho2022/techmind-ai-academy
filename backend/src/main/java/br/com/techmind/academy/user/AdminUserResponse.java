package br.com.techmind.academy.user;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String role,
        OffsetDateTime createdAt,
        int enrollments,
        int completedLessons,
        int xp,
        boolean currentUser
) {
}
