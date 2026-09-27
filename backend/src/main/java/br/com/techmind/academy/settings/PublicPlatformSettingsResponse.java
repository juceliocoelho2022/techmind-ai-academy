package br.com.techmind.academy.settings;

public record PublicPlatformSettingsResponse(
        String academyName,
        String tagline,
        String supportEmail,
        boolean registrationEnabled
) {
    static PublicPlatformSettingsResponse from(PlatformSettings settings) {
        return new PublicPlatformSettingsResponse(
                settings.getAcademyName(),
                settings.getTagline(),
                settings.getSupportEmail(),
                Boolean.TRUE.equals(settings.getRegistrationEnabled())
        );
    }
}
