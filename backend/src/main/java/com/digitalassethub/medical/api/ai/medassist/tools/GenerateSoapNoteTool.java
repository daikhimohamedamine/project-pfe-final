package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.consultation.ConsultationEntity;
import com.digitalassethub.medical.api.consultation.ConsultationRepository;
import com.digitalassethub.medical.api.employee.EmployeeEntity;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Builds a SOAP-formatted clinical note and persists it as a consultation
 * with type = "SOAP". Doctor-only.
 */
@Component
public class GenerateSoapNoteTool implements Tool {

    private final EmployeeRepository employees;
    private final ConsultationRepository consultations;

    public GenerateSoapNoteTool(EmployeeRepository employees, ConsultationRepository consultations) {
        this.employees = employees;
        this.consultations = consultations;
    }

    @Override
    public String name() { return "generate_soap_note"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        if (user.getRole() != Role.MEDECIN) {
            return ToolResult.fail("FORBIDDEN",
                    "SOAP note generation requires the MEDECIN (doctor) role.");
        }

        Object pid = input.get("patient_id");
        if (pid == null) return ToolResult.fail("BAD_INPUT", "patient_id is required");

        EmployeeEntity emp;
        try {
            Long id = pid instanceof Number n ? n.longValue() : Long.parseLong(pid.toString());
            emp = employees.findById(id).orElse(null);
        } catch (NumberFormatException nfe) {
            emp = employees.findByDossierNumber(pid.toString()).orElse(null);
        }
        if (emp == null) return ToolResult.fail("NOT_FOUND", "Patient not found for id: " + pid);

        String chief = input.get("chief_complaint") == null ? "" : input.get("chief_complaint").toString();

        @SuppressWarnings("unchecked")
        List<String> symptoms = input.get("symptoms") instanceof List<?> l
                ? toStringList((List<?>) l) : List.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> vitals = input.get("vital_signs") instanceof Map<?, ?> vm
                ? (Map<String, Object>) vm : Map.of();
        String physical = input.get("physical_exam") == null ? "Non documenté." : input.get("physical_exam").toString();
        String labs = input.get("lab_results") == null ? "En attente." : input.get("lab_results").toString();
        @SuppressWarnings("unchecked")
        List<String> differentials = input.get("differentials") instanceof List<?> dl
                ? toStringList((List<?>) dl) : List.of();
        String plan = input.get("plan") == null ? "À déterminer." : input.get("plan").toString();

        Map<String, Object> soap = new LinkedHashMap<>();

        Map<String, Object> subjective = new LinkedHashMap<>();
        subjective.put("chief_complaint", chief);
        subjective.put("symptoms", symptoms);
        subjective.put("history_of_present_illness",
                "Patient présente: " + String.join(", ", symptoms) + ".");
        soap.put("subjective", subjective);

        Map<String, Object> objective = new LinkedHashMap<>();
        objective.put("vital_signs", vitals);
        objective.put("physical_exam", physical);
        objective.put("lab_results", labs);
        soap.put("objective", objective);

        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("differential_diagnoses", differentials);
        assessment.put("primary_diagnosis", differentials.isEmpty() ? "À déterminer." : differentials.get(0));
        assessment.put("icd10_codes", List.of());
        soap.put("assessment", assessment);

        Map<String, Object> planMap = new LinkedHashMap<>();
        planMap.put("treatment", plan);
        planMap.put("follow_up", "1-2 semaines ou selon nécessité.");
        planMap.put("referrals", List.of());
        planMap.put("patient_education", List.of());
        soap.put("plan", planMap);

        String today = LocalDate.now().toString();
        StringBuilder md = new StringBuilder();
        md.append("## SOAP Note — ").append(today).append("\n");
        md.append("**Patient**: ").append(emp.getPrenom()).append(" ").append(emp.getNom())
          .append(" (dossier ").append(emp.getDossierNumber()).append(", id ").append(emp.getId()).append(")\n\n");
        md.append("### S — Subjectif\n");
        md.append("**Motif principal**: ").append(chief).append("\n");
        md.append("**Symptômes**: ").append(String.join(", ", symptoms)).append("\n\n");
        md.append("### O — Objectif\n");
        md.append("**Constantes**: ")
          .append("TA ").append(vitals.getOrDefault("bp", "N/A")).append(" | ")
          .append("FC ").append(vitals.getOrDefault("hr", "N/A")).append(" bpm | ")
          .append("Temp ").append(vitals.getOrDefault("temp_celsius", "N/A")).append(" °C | ")
          .append("FR ").append(vitals.getOrDefault("rr", "N/A")).append(" | ")
          .append("SpO2 ").append(vitals.getOrDefault("spo2", "N/A")).append(" %")
          .append(vitals.containsKey("weight_kg") ? " | Poids " + vitals.get("weight_kg") + " kg" : "")
          .append("\n");
        md.append("**Examen**: ").append(physical).append("\n");
        md.append("**Labos**: ").append(labs).append("\n\n");
        md.append("### A — Évaluation\n");
        md.append("**Diagnostic principal**: ").append(assessment.get("primary_diagnosis")).append("\n");
        md.append("**Différentiels**: ").append(String.join(", ", differentials)).append("\n\n");
        md.append("### P — Plan\n");
        md.append(plan).append("\n");
        md.append("**Suivi**: 1-2 semaines ou selon nécessité.\n");

        ConsultationEntity c = new ConsultationEntity();
        c.setEmployeeId(emp.getId());
        c.setMedecinId(user.getId());
        c.setType("SOAP");
        c.setDateConsultation(LocalDate.now());
        if (vitals.get("weight_kg") instanceof Number wn) c.setPoids(wn.doubleValue());
        c.setDetails(md.toString());
        ConsultationEntity saved = consultations.save(c);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record_id", saved.getId());
        data.put("patient_id", emp.getId());
        data.put("medecin_id", user.getId());
        data.put("date", saved.getDateConsultation());
        data.put("soap_json", soap);
        data.put("soap_markdown", md.toString());
        return ToolResult.ok(data, "Note SOAP générée et enregistrée.");
    }

    private static List<String> toStringList(List<?> in) {
        List<String> out = new ArrayList<>(in.size());
        for (Object o : in) if (o != null) out.add(o.toString());
        return out;
    }
}
