package com.digitalassethub.medical.api.reminder;

import com.digitalassethub.medical.api.notification.NotificationLogRepository;
import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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

@Tag(name = "Reminders")
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {
    private final ReminderRepository repository;
    private final NotificationLogRepository notificationLogRepository;
    private final com.digitalassethub.medical.api.employee.EmployeeRepository employeeRepository;
    private final com.digitalassethub.medical.api.common.mail.EmailService emailService;

    public ReminderController(ReminderRepository repository, 
                              NotificationLogRepository notificationLogRepository,
                              com.digitalassethub.medical.api.employee.EmployeeRepository employeeRepository,
                              com.digitalassethub.medical.api.common.mail.EmailService emailService) {
        this.repository = repository;
        this.notificationLogRepository = notificationLogRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public List<java.util.Map<String, Object>> list(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long employeeId
    ) {
        if (employeeId != null) {
            return repository.findByEmployeeId(employeeId).stream().map(this::enrich).collect(Collectors.toList());
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now().plusDays(90);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ReminderEntity> list;
        
        if (principal instanceof SecurityUser securityUser) {
            UserEntity user = securityUser.user();
            Long medecinId = null;
            
            if (user.getRole() == Role.MEDECIN) {
                medecinId = user.getId();
            } else if (user.getRole() == Role.COORDINATRICE) {
                medecinId = user.getAssignedMedecinId();
            }
            
            if (medecinId != null) {
                list = repository.findByMedecinIdAndDateEcheanceBetween(medecinId, from, to);
            } else {
                list = repository.findByDateEcheanceBetween(from, to);
            }
        } else {
            list = repository.findByDateEcheanceBetween(from, to);
        }

        return list.stream().map(this::enrich).collect(Collectors.toList());
    }

    private java.util.Map<String, Object> enrich(ReminderEntity r) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", r.getId());
        map.put("employeeId", r.getEmployeeId());
        map.put("type", r.getType());
        map.put("message", r.getMessage());
        map.put("dateEcheance", r.getDateEcheance());
        map.put("dueDate", r.getDateEcheance()); // for frontend compatibility
        map.put("envoye", r.isEnvoye());
        
        // Add Employee Name
        employeeRepository.findById(r.getEmployeeId()).ifPresent(e -> {
            map.put("employeeName", e.getPrenom() + " " + e.getNom());
            map.put("employeeEmail", e.getEmail());
        });
        
        return map;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "CREATE_REMINDER", entityType = "Reminder")
    public ReminderEntity create(@RequestBody ReminderEntity reminder) {
        System.out.println("DEBUG: Creating reminder for employee " + reminder.getEmployeeId() + " on " + reminder.getDateEcheance() + " (sendNow=" + reminder.isSendEmailNow() + ")");
        reminder.setId(null);
        reminder.setEnvoye(false);
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            reminder.setCreatedBy(securityUser.user().getId());
        }
        
        ReminderEntity saved = repository.save(reminder);
        if (reminder.isSendEmailNow()) {
            return send(saved.getId());
        }
        return saved;
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public ReminderEntity createManual(@RequestBody ReminderEntity reminder) {
        if (reminder.getType() == null || reminder.getType().isBlank()) {
            reminder.setType("MANUEL");
        }
        return create(reminder);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "DELETE_REMINDER", entityType = "Reminder")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PutMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    @com.digitalassethub.medical.api.audit.Auditable(action = "SEND_REMINDER", entityType = "Reminder")
    public ReminderEntity send(@PathVariable Long id) {
        ReminderEntity reminder = repository.findById(id).orElseThrow();
        reminder.setEnvoye(true);
        reminder.setDateEnvoi(LocalDateTime.now());
        var log = new com.digitalassethub.medical.api.notification.NotificationLogEntity();
        log.setSujet("Rappel medical");
        
        String empEmail = "unknown@local";
        String empName = "Employé";
        String dateStr = reminder.getDateEcheance() != null ? reminder.getDateEcheance().toString() : "prochainement";
        
        try {
            var emp = employeeRepository.findById(reminder.getEmployeeId()).orElse(null);
            if (emp != null && emp.getEmail() != null) {
                empEmail = emp.getEmail();
                empName = emp.getPrenom() + " " + emp.getNom();
                if (reminder.getMessage() != null && !reminder.getMessage().isBlank()) {
                    emailService.sendCustomReminder(empEmail, reminder.getMessage());
                } else {
                    emailService.sendAppointmentReminder(empEmail, empName, dateStr, "Heure non précisée");
                }
            }
        } catch (Exception e) {}

        log.setDestinataireEmail(empEmail);
        log.setContenu("Reminder id " + reminder.getId() + " sent manually to " + empEmail);
        log.setStatut("SUCCESS");
        notificationLogRepository.save(log);
        return repository.save(reminder);
    }
}
