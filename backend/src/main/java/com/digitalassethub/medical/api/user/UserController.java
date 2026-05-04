package com.digitalassethub.medical.api.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users Profile")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserEntity updateProfile(@RequestBody UserEntity payload, java.security.Principal principal) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Must be authenticated");
        }
        String email = principal.getName();
        UserEntity user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        
        if (payload.getNom() != null) user.setNom(payload.getNom());
        if (payload.getPrenom() != null) user.setPrenom(payload.getPrenom());
        if (payload.getTelephone() != null) user.setTelephone(payload.getTelephone());
        
        // Only allow updating non-sensitive fields from this endpoint
        return userRepository.save(user);
    }

    @GetMapping("/medecins")
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_ADMIN')")
    public java.util.List<UserEntity> listMedecins() {
        return userRepository.findByRole(Role.MEDECIN);
    }
}
