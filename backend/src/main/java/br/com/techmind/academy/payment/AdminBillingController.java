package br.com.techmind.academy.payment;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/billing")
public class AdminBillingController {

    private final AdminBillingService service;

    public AdminBillingController(AdminBillingService service) {
        this.service = service;
    }

    @GetMapping
    public AdminBillingResponse summary(Authentication authentication) {
        return service.summary(authentication.getName());
    }
}
