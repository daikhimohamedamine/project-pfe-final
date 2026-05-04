package com.digitalassethub.medical.api.consultation;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;

import com.digitalassethub.medical.api.user.UserRepository;

@Tag(name = "Consultations")
@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {
    private final ConsultationRepository repository;
    private final UserRepository userRepository;

    public ConsultationController(ConsultationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public Page<java.util.Map<String, Object>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "") String type,
            Pageable pageable) {
            
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<ConsultationEntity> page;
        
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            Long medecinId = null;
            
            if (user.getRole() == Role.MEDECIN) {
                medecinId = user.getId();
            } else if (user.getRole() == Role.COORDINATRICE) {
                medecinId = user.getAssignedMedecinId();
            }
            
            if (medecinId != null) {
                if (employeeId != null) {
                    page = repository.findByMedecinIdAndEmployeeIdAndTypeContainingIgnoreCase(medecinId, employeeId, type, pageable);
                } else {
                    page = repository.findByMedecinIdAndTypeContainingIgnoreCase(medecinId, type, pageable);
                }
            } else {
                page = (employeeId != null) ? repository.findByEmployeeIdAndTypeContainingIgnoreCase(employeeId, type, pageable) : repository.findByTypeContainingIgnoreCase(type, pageable);
            }
        } else {
            page = (employeeId != null) ? repository.findByEmployeeIdAndTypeContainingIgnoreCase(employeeId, type, pageable) : repository.findByTypeContainingIgnoreCase(type, pageable);
        }

        return page.map(this::enrich);
    }

    private java.util.Map<String, Object> enrich(ConsultationEntity c) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", c.getId());
        map.put("employeeId", c.getEmployeeId());
        map.put("medecinId", c.getMedecinId());
        map.put("type", c.getType());
        map.put("dateConsultation", c.getDateConsultation());
        map.put("poids", c.getPoids());
        map.put("taille", c.getTaille());
        map.put("details", c.getDetails());
        map.put("createdAt", c.getCreatedAt());

        // Add Doctor Name
        if (c.getMedecinId() != null) {
            userRepository.findById(c.getMedecinId())
                .ifPresent(u -> map.put("medecinName", "Dr. " + u.getPrenom() + " " + u.getNom()));
        }
        return map;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public java.util.Map<String, Object> get(@PathVariable Long id) {
        return enrich(repository.findById(id).orElseThrow());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "CREATE_CONSULTATION", entityType = "Consultation")
    public ConsultationEntity create(@RequestBody ConsultationEntity consultation) {
        consultation.setId(null);
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            if (user.getRole() == Role.MEDECIN) {
                consultation.setMedecinId(user.getId());
            } else if (user.getRole() == Role.COORDINATRICE && consultation.getMedecinId() == null) {
                consultation.setMedecinId(user.getAssignedMedecinId());
            }
            // If ADMIN or COORDINATRICE provided a medecinId, it's already in consultation object from @RequestBody
        }
        
        return repository.save(consultation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "UPDATE_CONSULTATION", entityType = "Consultation")
    public ConsultationEntity update(@PathVariable Long id, @RequestBody ConsultationEntity payload) {
        ConsultationEntity consultation = repository.findById(id).orElseThrow();
        consultation.setType(payload.getType());
        consultation.setDateConsultation(payload.getDateConsultation());
        consultation.setPoids(payload.getPoids());
        consultation.setTaille(payload.getTaille());
        consultation.setDetails(payload.getDetails());
        return repository.save(consultation);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "DELETE_CONSULTATION", entityType = "Consultation")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
