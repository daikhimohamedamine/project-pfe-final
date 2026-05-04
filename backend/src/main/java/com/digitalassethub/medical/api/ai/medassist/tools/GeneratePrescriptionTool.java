package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.consultation.ConsultationEntity;
import com.digitalassethub.medical.api.consultation.ConsultationRepository;
import com.digitalassethub.medical.api.employee.EmployeeEntity;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Creates a formal prescription. Persisted as a {@link ConsultationEntity}
 * with type = "ORDONNANCE" and a JSON payload in the encrypted details column.
 *
 * Doctor-only. Always runs a drug-interaction check first.
 */
@Component
public class GeneratePrescriptionTool implements Tool {

    private final EmployeeRepository employees;
    private final ConsultationRepository consultations;
    private final CheckDrugInteractionsTool interactionTool;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeneratePrescriptionTool(EmployeeRepository employees,
                                    ConsultationRepository consultations,
                                    CheckDrugInteractionsTool interactionTool) {
        this.employees = employees;
        this.consultations = consultations;
        this.interactionTool = interactionTool;
    }

    @Override
    public String name() { return "generate_prescription"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        if (user.getRole() != Role.MEDECIN) {
            return ToolResult.fail("FORBIDDEN",
                    "Prescription generation requires the MEDECIN (doctor) role.");
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
        if (emp == null) {
            return ToolResult.fail("NOT_FOUND", "Patient not found for id: " + pid);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meds = input.get("medications") instanceof List<?> l
                ? (List<Map<String, Object>>) (List<?>) l
                : null;
        if (meds == null || meds.isEmpty()) {
            return ToolResult.fail("BAD_INPUT", "medications must be a non-empty array");
        }

        // Drug interaction safety gate
        List<String> drugNames = new ArrayList<>();
        for (Map<String, Object> m : meds) {
            Object n = m.get("name");
            if (n != null) drugNames.add(n.toString());
        }
        Map<String, Object> checkInput = new LinkedHashMap<>();
        checkInput.put("new_drugs", drugNames);
        checkInput.put("patient_id", emp.getId());
        ToolResult check = interactionTool.execute(checkInput, user);
        if (!check.success()) return check;

        @SuppressWarnings("unchecked")
        Map<String, Object> checkData = (Map<String, Object>) check.data();
        boolean safe = checkData.get("safe_to_prescribe") instanceof Boolean b && b;
        if (!safe) {
            return ToolResult.fail("INTERACTION_BLOCK",
                    "Prescription blocked due to major drug interactions or allergy conflicts.",
                    checkData);
        }

        int validDays = toInt(input.get("valid_days"), 30);
        String diagnosis = input.get("diagnosis_summary") == null ? "" : input.get("diagnosis_summary").toString();
        String notes = input.get("clinical_notes") == null ? "" : input.get("clinical_notes").toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "prescription");
        payload.put("diagnosis_summary", diagnosis);
        payload.put("clinical_notes", notes);
        payload.put("medications", meds);
        payload.put("valid_until", LocalDate.now().plusDays(validDays).toString());
        payload.put("interaction_warnings", checkData.get("interactions"));

        String detailsJson;
        try {
            detailsJson = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return ToolResult.fail("SERIALIZATION_ERROR", ex.getMessage());
        }

        ConsultationEntity c = new ConsultationEntity();
        c.setEmployeeId(emp.getId());
        c.setMedecinId(user.getId());
        c.setType("ORDONNANCE");
        c.setDateConsultation(LocalDate.now());
        c.setDetails(detailsJson);
        ConsultationEntity saved = consultations.save(c);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("prescription_id", saved.getId());
        data.put("patient_id", emp.getId());
        data.put("dossier_number", emp.getDossierNumber());
        data.put("medecin_id", user.getId());
        data.put("issued_on", saved.getDateConsultation());
        data.put("valid_until", payload.get("valid_until"));
        data.put("medications", meds);
        data.put("diagnosis_summary", diagnosis);
        data.put("interaction_warnings", checkData.get("interactions"));
        return ToolResult.ok(data, "Ordonnance générée et enregistrée.");
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
