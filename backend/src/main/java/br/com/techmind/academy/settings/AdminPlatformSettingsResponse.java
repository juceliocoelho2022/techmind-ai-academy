package br.com.techmind.academy.settings;

import java.time.OffsetDateTime;

public record AdminPlatformSettingsResponse(
        String academyName,
        String tagline,
        String supportEmail,
        String whatsappNumber,
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
                settings.getWhatsappNumber(),
                Boolean.TRUE.equals(settings.getRegistrationEnabled()),
                settings.getDefaultLessonXp(),
                settings.getDefaultQuizPassingScore(),
                settings.getDefaultQuizXp(),
                settings.getUpdatedAt()
        );
    }
}
