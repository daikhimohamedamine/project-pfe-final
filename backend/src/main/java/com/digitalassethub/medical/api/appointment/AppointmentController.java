package com.digitalassethub.medical.api.appointment;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.common.mail.EmailService;
import java.time.format.DateTimeFormatter;
import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointments")
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
    private final AppointmentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    public AppointmentController(AppointmentRepository repository, 
                               EmployeeRepository employeeRepository, 
                               EmailService emailService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public List<AppointmentEntity> list(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now().plusDays(90);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            
            if (user.getRole() == Role.MEDECIN) {
                return repository.findByMedecinIdAndDateDebutBetween(user.getId(), from, to);
            } else if (user.getRole() == Role.COORDINATRICE) {
                Long medecinId = user.getAssignedMedecinId();
                if (medecinId != null) {
                    return repository.findByMedecinIdAndDateDebutBetween(medecinId, from, to);
                }
                // Si pas de médecin assigné, la coordinatrice voit tout par défaut pour éviter les listes vides
            }
        }
        
        return repository.findByDateDebutBetween(from, to);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public AppointmentEntity create(@RequestBody AppointmentEntity payload) {
        payload.setId(null);
        
        if (payload.getDateFin() == null && payload.getDateDebut() != null) {
            payload.setDateFin(payload.getDateDebut().plusMinutes(30));
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            if (user.getRole() == Role.COORDINATRICE) {
                if (user.getAssignedMedecinId() != null) {
                    payload.setMedecinId(user.getAssignedMedecinId());
                } else {
                    // Security fallback if coordinator has no assigned doctor
                    throw new IllegalArgumentException("Erreur de sécurité : Aucun médecin n'est assigné à cette coordinatrice.");
                }
            } else if (user.getRole() == Role.MEDECIN) {
                payload.setMedecinId(user.getId());
            }
            payload.setCreatedBy(user.getId());
        }

        if (payload.getStatut() == null || payload.getStatut().isBlank()) {
            payload.setStatut("PLANIFIE");
        }
        AppointmentEntity saved = repository.save(payload);
        
        // Envoi du rappel par email si l'employé existe
        employeeRepository.findById(saved.getEmployeeId()).ifPresent(emp -> {
            if (emp.getEmail() != null && !emp.getEmail().isBlank()) {
                String dateStr = saved.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String timeStr = saved.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm"));
                emailService.sendAppointmentReminder(emp.getEmail(), emp.getPrenom() + " " + emp.getNom(), dateStr, timeStr);
            }
        });
        
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public AppointmentEntity update(@PathVariable Long id, @RequestBody AppointmentEntity payload) {
        AppointmentEntity entity = repository.findById(id).orElseThrow();
        entity.setDateDebut(payload.getDateDebut());
        entity.setDateFin(payload.getDateFin());
        entity.setTypeVisite(payload.getTypeVisite());
        
        // Validation de sécurité pour la modification
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            if (user.getRole() == Role.COORDINATRICE && user.getAssignedMedecinId() != null) {
                entity.setMedecinId(user.getAssignedMedecinId()); // Forcé au médecin assigné
            } else if (user.getRole() == Role.MEDECIN) {
                entity.setMedecinId(user.getId()); // Forcé à lui-même
            } else {
                entity.setMedecinId(payload.getMedecinId()); // Admin peut modifier librement
            }
        } else {
            entity.setMedecinId(payload.getMedecinId());
        }
        
        entity.setNotes(payload.getNotes());
        entity.setStatut(payload.getStatut());
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public org.springframework.http.ResponseEntity<Void> cancel(@PathVariable Long id) {
        AppointmentEntity entity = repository.findById(id).orElseThrow();
        repository.delete(entity);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
