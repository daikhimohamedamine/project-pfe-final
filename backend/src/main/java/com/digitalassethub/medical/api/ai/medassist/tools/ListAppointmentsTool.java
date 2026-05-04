package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.appointment.AppointmentEntity;
import com.digitalassethub.medical.api.appointment.AppointmentRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListAppointmentsTool implements Tool {
    private final AppointmentRepository appointments;
    private final com.digitalassethub.medical.api.employee.EmployeeRepository employees;

    public ListAppointmentsTool(AppointmentRepository appointments, 
                                com.digitalassethub.medical.api.employee.EmployeeRepository employees) {
        this.appointments = appointments;
        this.employees = employees;
    }

    @Override
    public String name() { return "list_appointments"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        LocalDate from = parseDate(input.get("from"), LocalDate.now());
        LocalDate to   = parseDate(input.get("to"),   from.plusDays(7));
        if (to.isBefore(from)) {
            return ToolResult.fail("BAD_INPUT", "'to' must be on or after 'from'");
        }
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        List<AppointmentEntity> rows;
        if (user.getRole() == Role.MEDECIN) {
            rows = appointments.findByMedecinIdAndDateDebutBetween(user.getId(), fromDt, toDt);
        } else if (user.getRole() == Role.COORDINATRICE && user.getAssignedMedecinId() != null) {
            rows = appointments.findByMedecinIdAndDateDebutBetween(user.getAssignedMedecinId(), fromDt, toDt);
        } else {
            rows = appointments.findByDateDebutBetween(fromDt, toDt);
        }

        int limit = toInt(input.get("limit"), 25);
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            AppointmentEntity a = rows.get(i);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", a.getId());
            r.put("date_debut", a.getDateDebut() == null ? null : a.getDateDebut().toString());
            r.put("date_fin",   a.getDateFin()   == null ? null : a.getDateFin().toString());
            r.put("employee_id", a.getEmployeeId());
            
            if (a.getEmployeeId() != null) {
                employees.findById(a.getEmployeeId()).ifPresent(e -> {
                    r.put("patient_name", e.getNom() + " " + e.getPrenom());
                });
            }
            
            r.put("medecin_id", a.getMedecinId());
            r.put("type_visite", a.getTypeVisite());
            r.put("statut", a.getStatut());
            r.put("notes", a.getNotes());
            out.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from.toString());
        data.put("to", to.toString());
        data.put("count", out.size());
        data.put("results", out);
        return ToolResult.ok(data);
    }

    private static LocalDate parseDate(Object o, LocalDate fallback) {
        if (o == null) return fallback;
        try { return LocalDate.parse(o.toString()); }
        catch (DateTimeParseException ex) { return fallback; }
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
