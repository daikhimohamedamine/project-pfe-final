package com.digitalassethub.medical.api.employee;

import com.digitalassethub.medical.api.audit.Auditable;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag(name = "Employees")
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_COORDINATRICE','ROLE_MEDECIN','ROLE_ADMIN')")
    public Page<EmployeeEntity> list(
            @RequestParam(defaultValue = "") String nom,
            @RequestParam(defaultValue = "") String prenom,
            Pageable pageable
    ) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            
            if (user.getRole() == Role.MEDECIN) {
                return employeeRepository.findByMedecinIdAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(user.getId(), nom, prenom, pageable);
            } else if (user.getRole() == Role.COORDINATRICE) {
                Long medecinId = user.getAssignedMedecinId();
                if (medecinId != null) {
                    return employeeRepository.findByMedecinIdAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(medecinId, nom, prenom, pageable);
                }
            }
        }
        
        return employeeRepository.findByNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(nom, prenom, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    @Auditable(action = "VIEW_EMPLOYEE", entityType = "Employee")
    public EmployeeEntity get(@PathVariable Long id) {
        return employeeRepository.findById(id).orElseThrow();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINATRICE')")
    @Auditable(action = "CREATE_EMPLOYEE", entityType = "Employee")
    public EmployeeEntity create(@Valid @RequestBody EmployeeEntity employee) {
        employee.setId(null);
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            if (user.getRole() == Role.COORDINATRICE) {
                employee.setMedecinId(user.getAssignedMedecinId());
            } else if (user.getRole() == Role.MEDECIN) {
                employee.setMedecinId(user.getId());
            }
        }
        
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATRICE')")
    @Auditable(action = "UPDATE_EMPLOYEE", entityType = "Employee")
    @Transactional
    public EmployeeEntity update(@PathVariable Long id, @RequestBody EmployeeEntity payload) {
        EmployeeEntity employee = employeeRepository.findById(id).orElseThrow();
        
        // Basic Info
        employee.setNom(payload.getNom());
        employee.setPrenom(payload.getPrenom());
        employee.setDateNaissance(payload.getDateNaissance());
        employee.setLieuNaissance(payload.getLieuNaissance());
        employee.setTelephone(payload.getTelephone());
        employee.setGouvernorat(payload.getGouvernorat());
        employee.setSituationFamiliale(payload.getSituationFamiliale());
        employee.setNombreEnfants(payload.getNombreEnfants());
        employee.setAdresse(payload.getAdresse());
        employee.setCodePostal(payload.getCodePostal());
        
        // Professional Info
        employee.setDepartement(payload.getDepartement());
        employee.setPosteTravail(payload.getPosteTravail());
        employee.setDateEmbauche(payload.getDateEmbauche());
        employee.setMatriculeCaisse(payload.getMatriculeCaisse());
        employee.setDossierNumber(payload.getDossierNumber());
        employee.setEmail(payload.getEmail());
        
        // Medical History
        employee.setAntecedentsChirurgicaux(payload.getAntecedentsChirurgicaux());
        employee.setAntecedentsMedicaux(payload.getAntecedentsMedicaux());
        employee.setAntecedentsGynecologiques(payload.getAntecedentsGynecologiques());
        employee.setAntecedentsHereditaires(payload.getAntecedentsHereditaires());
        
        // Habits
        employee.setTabac(payload.getTabac());
        employee.setAlcool(payload.getAlcool());
        employee.setAutomedication(payload.getAutomedication());
        
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}/medical")
    @PreAuthorize("hasRole('MEDECIN')")
    public EmployeeEntity updateMedical(@PathVariable Long id, @RequestBody EmployeeEntity payload) {
        EmployeeEntity employee = employeeRepository.findById(id).orElseThrow();
        employee.setStatut(payload.getStatut());
        return employeeRepository.save(employee);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('COORDINATRICE')")
    public EmployeeEntity archive(@PathVariable Long id) {
        EmployeeEntity employee = employeeRepository.findById(id).orElseThrow();
        employee.setStatut("ARCHIVE");
        employee.setArchivedAt(LocalDateTime.now());
        return employeeRepository.save(employee);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDINATRICE')")
    @Auditable(action = "DELETE_EMPLOYEE", entityType = "Employee")
    public void delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
    }
}
