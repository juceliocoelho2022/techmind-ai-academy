package br.com.techmind.academy.settings;

import jakarta.validation.constraints.*;

public record UpdatePlatformSettingsRequest(
        @NotBlank @Size(max = 120) String academyName,
        @NotBlank @Size(max = 240) String tagline,
        @Email @Size(max = 200) String supportEmail,
        @NotNull Boolean registrationEnabled,
        @NotNull @Min(0) Integer defaultLessonXp,
        @NotNull @Min(0) @Max(100) Integer defaultQuizPassingScore,
        @NotNull @Min(0) Integer defaultQuizXp
) {
}
