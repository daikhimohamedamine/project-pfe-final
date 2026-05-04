package com.digitalassethub.medical.api.reminder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    List<ReminderEntity> findByDateEcheanceBetween(LocalDate from, LocalDate to);
    
    @org.springframework.data.jpa.repository.Query("SELECT r FROM ReminderEntity r, com.digitalassethub.medical.api.employee.EmployeeEntity e WHERE r.employeeId = e.id AND e.medecinId = :medecinId AND r.dateEcheance BETWEEN :from AND :to")
    List<ReminderEntity> findByMedecinIdAndDateEcheanceBetween(Long medecinId, LocalDate from, LocalDate to);

    Optional<ReminderEntity> findTopByEmployeeIdAndTypeAndEnvoyeFalse(Long employeeId, String type);
    
    List<ReminderEntity> findByEmployeeId(Long employeeId);

    List<ReminderEntity> findByDateEcheanceBetweenAndEnvoyeFalseOrderByDateEcheanceAsc(LocalDate from, LocalDate to);
}
