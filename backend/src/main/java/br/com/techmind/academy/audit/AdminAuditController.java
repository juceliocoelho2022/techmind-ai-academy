package br.com.techmind.academy.audit;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AdminAuditController {

    private final AdminAuditService service;

    public AdminAuditController(AdminAuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminAuditLogResponse> recent(
            Authentication authentication,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return service.recent(authentication.getName(), limit);
    }
}
