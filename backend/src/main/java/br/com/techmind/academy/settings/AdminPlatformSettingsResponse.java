package br.com.techmind.academy.settings;

import java.time.OffsetDateTime;

public record AdminPlatformSettingsResponse(
        String academyName,
        String tagline,
        String supportEmail,
        boolean registrationEnabled,
        int defaultLessonXp,
        int defaultQuizPassingScore,
        int defaultQuizXp,
        OffsetDateTime updatedAt
) {
    static AdminPlatformSettingsResponse from(PlatformSettings settings) {
        return new AdminPlatformSettingsResponse(
                settings.getAcademyName(),
                settings.getTagline(),
                settings.getSupportEmail(),
                Boolean.TRUE.equals(settings.getRegistrationEnabled()),
                settings.getDefaultLessonXp(),
                settings.getDefaultQuizPassingScore(),
                settings.getDefaultQuizXp(),
                settings.getUpdatedAt()
        );
    }
}
