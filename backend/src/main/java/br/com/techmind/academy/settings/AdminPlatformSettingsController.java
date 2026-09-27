package br.com.techmind.academy.settings;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminPlatformSettingsController {

    private final PlatformSettingsService service;

    public AdminPlatformSettingsController(PlatformSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminPlatformSettingsResponse get(Authentication authentication) {
        return service.adminView(authentication.getName());
    }

    @PutMapping
    public AdminPlatformSettingsResponse update(
            Authentication authentication,
            @Valid @RequestBody UpdatePlatformSettingsRequest request
    ) {
        return service.update(authentication.getName(), request);
    }
}
