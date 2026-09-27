package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.CourseLevel;
import br.com.techmind.academy.subscription.SubscriptionPlan;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCourseRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug deve usar letras minúsculas, números e hífens")
        @Size(max = 120)
        String slug,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotBlank
        @Size(max = 80)
        String category,

        @NotBlank
        @Size(max = 80)
        String technology,

        @NotNull
        CourseLevel level,

        @NotNull
        SubscriptionPlan requiredPlan,

        @Min(0)
        Integer totalLessons
) {
}
