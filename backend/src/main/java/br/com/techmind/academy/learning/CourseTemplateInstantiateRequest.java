package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseTemplateInstantiateRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "slug deve usar letras minúsculas, números e hífens"
        )
        @Size(max = 120)
        String slug,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        CourseLevel level,

        @NotNull
        Boolean createStructure
) {
}
