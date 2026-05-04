package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.appointment.AppointmentEntity;
import com.digitalassethub.medical.api.appointment.AppointmentRepository;
import com.digitalassethub.medical.api.consultation.ConsultationEntity;
import com.digitalassethub.medical.api.consultation.ConsultationRepository;
import com.digitalassethub.medical.api.employee.EmployeeEntity;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class GetPatientHistoryTool implements Tool {
    private final EmployeeRepository employees;
    private final ConsultationRepository consultations;
    private final AppointmentRepository appointments;

    public GetPatientHistoryTool(EmployeeRepository employees,
                                 ConsultationRepository consultations,
                                 AppointmentRepository appointments) {
        this.employees = employees;
        this.consultations = consultations;
        this.appointments = appointments;
    }

    @Override
    public String name() { return "get_patient_history"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        Object pid = input.get("patient_id");
        if (pid == null) {
            return ToolResult.fail("BAD_INPUT", "patient_id is required");
        }

        Optional<EmployeeEntity> opt = resolveEmployee(pid);
        if (opt.isEmpty()) {
            return ToolResult.fail("NOT_FOUND", "Patient (employee) not found for id: " + pid);
        }
        EmployeeEntity emp = opt.get();

        // Authorization: medecin can only access patients assigned to them when an assignment exists.
        if (user.getRole() == Role.MEDECIN) {
            Long assigned = emp.getMedecinId();
            if (assigned != null && !assigned.equals(user.getId())) {
                return ToolResult.fail("FORBIDDEN",
                        "This patient is not assigned to you.");
            }
        }
        // COORDINATRICE and ADMIN have full read access.

        @SuppressWarnings("unchecked")
        List<String> sections = input.get("sections") instanceof List<?> l
                ? (List<String>) (List<?>) l
                : List.of("profile", "records", "antecedents", "vitals", "appointments");

        int limitRecords = toInt(input.get("limit_records"), 10);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patient_id", emp.getId());
        data.put("dossier_number", emp.getDossierNumber());

        if (sections.contains("profile")) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("nom", emp.getNom());
            profile.put("prenom", emp.getPrenom());
            profile.put("date_naissance", emp.getDateNaissance());
            profile.put("lieu_naissance", emp.getLieuNaissance());
            profile.put("telephone", emp.getTelephone());
            profile.put("email", emp.getEmail());
            profile.put("gouvernorat", emp.getGouvernorat());
            profile.put("situation_familiale", emp.getSituationFamiliale());
            profile.put("nb_enfants", emp.getNombreEnfants());
            profile.put("departement", emp.getDepartement());
            profile.put("poste_travail", emp.getPosteTravail());
            profile.put("date_embauche", emp.getDateEmbauche());
            profile.put("statut", emp.getStatut());
            profile.put("medecin_id", emp.getMedecinId());
            data.put("profile", profile);
        }

        if (sections.contains("antecedents")) {
            Map<String, Object> ant = new LinkedHashMap<>();
            ant.put("chirurgicaux", emp.getAntecedentsChirurgicaux());
            ant.put("medicaux", emp.getAntecedentsMedicaux());
            ant.put("gynecologiques", emp.getAntecedentsGynecologiques());
            ant.put("hereditaires", emp.getAntecedentsHereditaires());
            ant.put("tabac", emp.getTabac());
            ant.put("alcool", emp.getAlcool());
            ant.put("automedication", emp.getAutomedication());
            data.put("antecedents", ant);
        }

        List<ConsultationEntity> recent = consultations.findTop10ByEmployeeIdOrderByDateConsultationDesc(emp.getId());
        if (recent.size() > limitRecords) recent = recent.subList(0, limitRecords);

        if (sections.contains("records")) {
            List<Map<String, Object>> records = new ArrayList<>();
            for (ConsultationEntity c : recent) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", c.getId());
                r.put("type", c.getType());
                r.put("date", c.getDateConsultation());
                r.put("medecin_id", c.getMedecinId());
                r.put("poids_kg", c.getPoids());
                r.put("taille_cm", c.getTaille());
                r.put("details", c.getDetails());
                records.add(r);
            }
            data.put("records", records);
        }

        if (sections.contains("vitals")) {
            List<Map<String, Object>> vitals = new ArrayList<>();
            for (ConsultationEntity c : recent) {
                if (c.getPoids() != null || c.getTaille() != null) {
                    Map<String, Object> v = new LinkedHashMap<>();
                    v.put("date", c.getDateConsultation());
                    v.put("poids_kg", c.getPoids());
                    v.put("taille_cm", c.getTaille());
                    if (c.getPoids() != null && c.getTaille() != null && c.getTaille() > 0) {
                        double m = c.getTaille() / 100.0;
                        v.put("imc", Math.round((c.getPoids() / (m * m)) * 10.0) / 10.0);
                    }
                    vitals.add(v);
                }
            }
            data.put("vitals", vitals);
        }

        if (sections.contains("appointments")) {
            LocalDateTime now = LocalDateTime.now();
            List<AppointmentEntity> all = appointments.findByDateDebutBetween(now.minusMonths(6), now.plusMonths(6));
            List<Map<String, Object>> appts = new ArrayList<>();
            for (AppointmentEntity a : all) {
                if (!Objects.equals(a.getEmployeeId(), emp.getId())) continue;
                Map<String, Object> ap = new LinkedHashMap<>();
                ap.put("id", a.getId());
                ap.put("date_debut", a.getDateDebut());
                ap.put("date_fin", a.getDateFin());
                ap.put("type_visite", a.getTypeVisite());
                ap.put("statut", a.getStatut());
                ap.put("medecin_id", a.getMedecinId());
                appts.add(ap);
            }
            data.put("appointments", appts);
        }

        return ToolResult.ok(data);
    }

    private Optional<EmployeeEntity> resolveEmployee(Object pid) {
        if (pid instanceof Number n) {
            return employees.findById(n.longValue());
        }
        String s = pid.toString().trim();
        if (s.isEmpty()) return Optional.empty();
        try {
            return employees.findById(Long.parseLong(s));
        } catch (NumberFormatException ignored) {
            return employees.findByDossierNumber(s);
        }
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return def;
    }
}
