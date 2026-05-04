package com.digitalassethub.medical.api.notification;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationLogRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_ADMIN','ROLE_MEDECIN')")
    public List<NotificationLogEntity> list() {
        return repository.findAll();
    }
}
