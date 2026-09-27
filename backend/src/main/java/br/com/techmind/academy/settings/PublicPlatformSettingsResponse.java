package br.com.techmind.academy.settings;

public record PublicPlatformSettingsResponse(
        String academyName,
        String tagline,
        String supportEmail,
        String whatsappNumber,
        boolean registrationEnabled
) {
    static PublicPlatformSettingsResponse from(PlatformSettings settings) {
        return new PublicPlatformSettingsResponse(
                settings.getAcademyName(),
                settings.getTagline(),
                settings.getSupportEmail(),
                settings.getWhatsappNumber(),
                Boolean.TRUE.equals(settings.getRegistrationEnabled())
        );
    }
}
