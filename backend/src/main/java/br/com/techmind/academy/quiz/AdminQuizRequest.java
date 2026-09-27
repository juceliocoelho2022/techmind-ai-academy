package br.com.techmind.academy.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record AdminQuizRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String description,
        @Min(0) @Max(100) Integer passingScore,
        @Min(0) Integer xpReward,
        @NotNull Boolean active,
        @NotEmpty List<@Valid AdminQuizQuestionRequest> questions
) {
    public record AdminQuizQuestionRequest(
            @NotBlank @Size(max = 1000) String prompt,
            @NotEmpty List<@Valid AdminQuizOptionRequest> options
    ) {}

    public record AdminQuizOptionRequest(
            @NotBlank @Size(max = 500) String text,
            @NotNull Boolean correct
    ) {}
}
