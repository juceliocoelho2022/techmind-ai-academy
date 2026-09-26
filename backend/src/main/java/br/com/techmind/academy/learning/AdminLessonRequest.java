package br.com.techmind.academy.learning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminLessonRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve usar letras minúsculas, números e hífens")
        @Size(max = 120)
        String slug,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 500)
        String summary,

        @Min(1)
        Integer position,

        @Min(0)
        Integer xpReward
) {
}
