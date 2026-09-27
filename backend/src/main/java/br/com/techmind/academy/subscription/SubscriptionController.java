package br.com.techmind.academy.subscription;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public MySubscriptionResponse me(Authentication authentication) {
        return service.current(authentication.getName());
    }

    @PostMapping("/upgrade-requests")
    public MySubscriptionResponse requestUpgrade(
            Authentication authentication,
            @Valid @RequestBody UpgradeSubscriptionRequest request
    ) {
        return service.requestUpgrade(authentication.getName(), request);
    }
}
