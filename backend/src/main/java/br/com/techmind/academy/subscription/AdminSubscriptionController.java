package br.com.techmind.academy.subscription;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionService service;

    public AdminSubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/upgrade-requests")
    public List<AdminUpgradeRequestResponse> requests(
            Authentication authentication,
            @RequestParam(required = false) UpgradeRequestStatus status
    ) {
        return service.adminRequests(authentication.getName(), status);
    }

    @PostMapping("/upgrade-requests/{requestId}/approve")
    public AdminUpgradeRequestResponse approve(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        return service.approve(authentication.getName(), requestId);
    }

    @PostMapping("/upgrade-requests/{requestId}/reject")
    public AdminUpgradeRequestResponse reject(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        return service.reject(authentication.getName(), requestId);
    }
}
