package com.digitalassethub.medical.api.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-2fa")
    public LoginResponse verify2fa(@RequestBody java.util.Map<String, String> payload) {
        return authService.verify2fa(payload.get("email"), payload.get("code"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/change-password")
    public void changePassword(@RequestBody java.util.Map<String, String> payload, java.security.Principal principal) {
        if (principal == null) throw new org.springframework.security.access.AccessDeniedException("Must be authenticated");
        authService.changePassword(principal.getName(), payload.get("currentPassword"), payload.get("newPassword"));
    }
}
