package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.appointment.AppointmentRepository;
import com.digitalassethub.medical.api.consultation.ConsultationRepository;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.reminder.ReminderRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns role-relevant high-level KPIs (employees, appointments today,
 * pending reminders, etc.). Useful for "what's my workload today?" or
 * admin overview questions.
 */
@Component
public class GetDashboardStatsTool implements Tool {
    private final EmployeeRepository employees;
    private final AppointmentRepository appointments;
    private final ReminderRepository reminders;
    private final ConsultationRepository consultations;
    private final UserRepository users;

    public GetDashboardStatsTool(EmployeeRepository employees,
                                  AppointmentRepository appointments,
                                  ReminderRepository reminders,
                                  ConsultationRepository consultations,
                                  UserRepository users) {
        this.employees = employees;
        this.appointments = appointments;
        this.reminders = reminders;
        this.consultations = consultations;
        this.users = users;
    }

    @Override
    public String name() { return "get_dashboard_stats"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", user.getRole().name());

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        LocalDate weekEnd = today.plusDays(7);

        try {
            switch (user.getRole()) {
                case MEDECIN -> {
                    long mine = employees.countByMedecinId(user.getId());
                    int todays = appointments
                            .findByMedecinIdAndDateDebutBetween(user.getId(), startOfDay, endOfDay).size();
                    int week = appointments
                            .findByMedecinIdAndDateDebutBetween(
                                    user.getId(), startOfDay, weekEnd.atTime(23, 59, 59)).size();
                    data.put("assigned_patients", mine);
                    data.put("appointments_today", todays);
                    data.put("appointments_next_7_days", week);
                }
                case COORDINATRICE -> {
                    long medecinId = user.getAssignedMedecinId() != null ? user.getAssignedMedecinId() : -1L;
                    long empCount;
                    int todays;
                    
                    if (medecinId != -1L) {
                        empCount = employees.countByMedecinId(medecinId);
                        todays = appointments.findByMedecinIdAndDateDebutBetween(medecinId, startOfDay, endOfDay).size();
                    } else {
                        empCount = employees.count();
                        todays = appointments.findByDateDebutBetween(startOfDay, endOfDay).size();
                    }
                    
                    int pendingReminders = reminders
                            .findByDateEcheanceBetweenAndEnvoyeFalseOrderByDateEcheanceAsc(
                                    today, weekEnd).size();
                                    
                    data.put("assigned_doctor_id", medecinId != -1L ? medecinId : "None");
                    data.put("total_employees", empCount);
                    data.put("appointments_today", todays);
                    data.put("pending_reminders_next_7_days", pendingReminders);
                }
                case ADMIN -> {
                    long allEmp = employees.count();
                    long allUsers = users.count();
                    long allConsults = consultations.count();
                    int todays = appointments.findByDateDebutBetween(startOfDay, endOfDay).size();
                    long medecins = users.findByRole(Role.MEDECIN).size();
                    long coords = users.findByRole(Role.COORDINATRICE).size();
                    data.put("total_employees", allEmp);
                    data.put("total_users", allUsers);
                    data.put("total_medecins", medecins);
                    data.put("total_coordinatrices", coords);
                    data.put("total_consultations", allConsults);
                    data.put("appointments_today", todays);
                }
            }
        } catch (Exception ex) {
            return ToolResult.fail("STATS_ERROR", ex.getMessage() == null ? "stats failed" : ex.getMessage());
        }
        return ToolResult.ok(data);
    }
}
