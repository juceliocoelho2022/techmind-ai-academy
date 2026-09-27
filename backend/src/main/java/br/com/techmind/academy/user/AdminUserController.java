package br.com.techmind.academy.user;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUserResponse> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @PatchMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleRequest request
    ) {
        return service.updateRole(authentication.getName(), userId, request);
    }
}
