package com.digitalassethub.medical.api.admin;

import com.digitalassethub.medical.api.admin.importing.DrugCsvImportService;
import com.digitalassethub.medical.api.audit.AuditLogEntity;
import com.digitalassethub.medical.api.audit.AuditLogRepository;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;
import com.digitalassethub.medical.api.common.mail.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Administration")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;
    private final DrugCsvImportService drugCsvImportService;
    private final EmailService emailService;
    private final com.digitalassethub.medical.api.employee.EmployeeRepository employeeRepository;
    private final com.digitalassethub.medical.api.consultation.ConsultationRepository consultationRepository;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuditLogRepository auditLogRepository, DrugCsvImportService drugCsvImportService,
                           EmailService emailService, com.digitalassethub.medical.api.employee.EmployeeRepository employeeRepository,
                           com.digitalassethub.medical.api.consultation.ConsultationRepository consultationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
        this.drugCsvImportService = drugCsvImportService;
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
        this.consultationRepository = consultationRepository;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Object> getStats() {
        long activeUsers = userRepository.findAll().stream().filter(UserEntity::isEnabled).count();
        long totalUsers = userRepository.count();
        long totalRecords = employeeRepository.count();
        long openAuditItems = auditLogRepository.count();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeUsers", activeUsers);
        stats.put("totalUsers", totalUsers);
        stats.put("recordsStored", totalRecords);
        stats.put("openAuditItems", openAuditItems);
        stats.put("storageUsed", "14.2 GB"); // Mocked for now

        // Role Distribution
        java.util.List<UserEntity> users = userRepository.findAll();
        long doctors = users.stream().filter(u -> u.getRole() == com.digitalassethub.medical.api.user.Role.MEDECIN).count();
        long coords = users.stream().filter(u -> u.getRole() == com.digitalassethub.medical.api.user.Role.COORDINATRICE).count();
        long admins = users.stream().filter(u -> u.getRole() == com.digitalassethub.medical.api.user.Role.ADMIN).count();
        
        long total = users.isEmpty() ? 1 : users.size();
        
        stats.put("roleDistDoctors", Math.round((doctors * 100.0) / total));
        stats.put("roleDistCoords", Math.round((coords * 100.0) / total));
        stats.put("roleDistAdmins", Math.round((admins * 100.0) / total));

        return stats;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COORDINATRICE')")
    public List<UserEntity> users() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "CREATE_USER", entityType = "User")
    public UserEntity createUser(@RequestBody UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Cet e-mail est déjà utilisé par un autre compte."
            );
        }
        String rawPassword = user.getPasswordHash(); // Contient le mot de passe en clair du front
        user.setId(null);
        user.setEnabled(true);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        UserEntity saved = userRepository.save(user);
        
        // Envoi de l'email de bienvenue
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getPrenom() + " " + saved.getNom(), rawPassword);
        
        return saved;
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "UPDATE_USER", entityType = "User")
    public UserEntity updateUser(@PathVariable Long id, @RequestBody UserEntity payload) {
        UserEntity user = userRepository.findById(id).orElseThrow();
        user.setNom(payload.getNom());
        user.setPrenom(payload.getPrenom());
        user.setRole(payload.getRole());
        user.setEnabled(payload.isEnabled());
        user.setAssignedMedecinId(payload.getAssignedMedecinId());
        if (payload.getPasswordHash() != null && !payload.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(payload.getPasswordHash()));
        }
        return userRepository.save(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "DELETE_USER", entityType = "User")
    public void deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<java.util.Map<String, Object>> auditLogs() {
        List<AuditLogEntity> logs = auditLogRepository.findAll();
        List<UserEntity> users = userRepository.findAll();
        List<com.digitalassethub.medical.api.employee.EmployeeEntity> employees = employeeRepository.findAll();
        
        return logs.stream().map(log -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", log.getId());
            map.put("timestamp", log.getTimestamp());
            map.put("action", log.getAction());
            map.put("entityType", log.getEntityType());
            map.put("entityId", log.getEntityId());
            map.put("ipAddress", log.getIpAddress());
            map.put("userRole", log.getUserRole());
            map.put("userId", log.getUserId());
            
            // Trouver le nom de l'acteur
            if (log.getUserId() != null) {
                users.stream()
                    .filter(u -> u.getId().equals(log.getUserId()))
                    .findFirst()
                    .ifPresent(u -> map.put("actorName", u.getPrenom() + " " + u.getNom()));
            } else {
                map.put("actorName", "Système");
            }
                
            // Trouver le nom de la cible (Entity Name)
            if (log.getEntityId() != null && log.getEntityType() != null) {
                if ("Employee".equalsIgnoreCase(log.getEntityType())) {
                    employees.stream()
                        .filter(e -> e.getId().equals(log.getEntityId()))
                        .findFirst()
                        .ifPresent(e -> map.put("targetName", e.getPrenom() + " " + e.getNom()));
                } else if ("User".equalsIgnoreCase(log.getEntityType())) {
                    users.stream()
                        .filter(u -> u.getId().equals(log.getEntityId()))
                        .findFirst()
                        .ifPresent(u -> map.put("targetName", u.getPrenom() + " " + u.getNom()));
                }
            }

            // Trouver le contexte du médecin si applicable
            if (log.getMedecinContextId() != null) {
                users.stream()
                    .filter(u -> u.getId().equals(log.getMedecinContextId()))
                    .findFirst()
                    .ifPresent(u -> map.put("medecinContextName", "Dr. " + u.getPrenom() + " " + u.getNom()));
            }
            
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/drugs/import")
    @PreAuthorize("hasRole('ADMIN')")
    public DrugCsvImportService.ImportResult importDrugs(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean clearBefore
    ) throws IOException {
        return drugCsvImportService.importCsv(file.getInputStream(), clearBefore);
    }

    @GetMapping("/users/{id}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Object> getUserActivity(@PathVariable Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow();
        java.util.List<AuditLogEntity> logs = auditLogRepository.findAll().stream()
            .filter(l -> l.getUserId().equals(id))
            .collect(java.util.stream.Collectors.toList());
            
        long totalConsultations = consultationRepository.countByMedecinId(id);
        
        java.util.Map<String, Object> activity = new java.util.HashMap<>();
        activity.put("user", user);
        activity.put("logs", logs);
        activity.put("totalConsultations", totalConsultations);
        
        return activity;
    }
}
