package br.com.techmind.academy.enrollment;

import jakarta.validation.constraints.Min;

public record UpdateProgressRequest(
        @Min(0) int completedLessons,
        @Min(0) int xp
) {}
