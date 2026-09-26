package br.com.techmind.academy.interview;

import jakarta.validation.constraints.NotBlank;

public record InterviewRequest(
        @NotBlank String targetRole,
        @NotBlank String answer
) {}
