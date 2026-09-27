package br.com.techmind.academy.settings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class PublicPlatformSettingsController {

    private final PlatformSettingsService service;

    public PublicPlatformSettingsController(PlatformSettingsService service) {
        this.service = service;
    }

    @GetMapping("/public")
    public PublicPlatformSettingsResponse get() {
        return service.publicView();
    }
}
