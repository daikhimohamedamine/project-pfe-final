package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.consultation.ConsultationEntity;
import com.digitalassethub.medical.api.consultation.ConsultationRepository;
import com.digitalassethub.medical.api.employee.EmployeeEntity;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Bundled drug-drug interaction and patient-allergy/antecedent checker.
 * Pure logic — no external API call. Extend KNOWN_INTERACTIONS over time.
 */
@Component
public class CheckDrugInteractionsTool implements Tool {

    private final EmployeeRepository employees;
    private final ConsultationRepository consultations;

    public CheckDrugInteractionsTool(EmployeeRepository employees, ConsultationRepository consultations) {
        this.employees = employees;
        this.consultations = consultations;
    }

    @Override
    public String name() { return "check_drug_interactions"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        @SuppressWarnings("unchecked")
        List<String> newDrugs = input.get("new_drugs") instanceof List<?> l
                ? toStringList((List<?>) l) : List.of();
        @SuppressWarnings("unchecked")
        List<String> additional = input.get("additional_drugs") instanceof List<?> l2
                ? toStringList((List<?>) l2) : List.of();

        if (newDrugs.isEmpty()) {
            return ToolResult.fail("BAD_INPUT", "new_drugs must be a non-empty array");
        }

        EmployeeEntity emp = null;
        Object pid = input.get("patient_id");
        if (pid != null) {
            try {
                Long id = pid instanceof Number n ? n.longValue() : Long.parseLong(pid.toString());
                emp = employees.findById(id).orElse(null);
            } catch (NumberFormatException nfe) {
                emp = employees.findByDossierNumber(pid.toString()).orElse(null);
            }
        }

        // Build full drug set: new + additional + drugs mentioned in recent consultations
        List<String> all = new ArrayList<>();
        for (String d : newDrugs) all.add(d.toLowerCase().trim());
        for (String d : additional) all.add(d.toLowerCase().trim());

        if (emp != null) {
            List<ConsultationEntity> recent = consultations.findTop10ByEmployeeIdOrderByDateConsultationDesc(emp.getId());
            for (ConsultationEntity c : recent) {
                if (c.getDetails() == null) continue;
                for (Interaction known : KNOWN_INTERACTIONS) {
                    String lc = c.getDetails().toLowerCase();
                    if (lc.contains(known.a)) all.add(known.a);
                    if (lc.contains(known.b)) all.add(known.b);
                }
            }
        }

        // Scan pairs
        List<Map<String, Object>> interactions = new ArrayList<>();
        for (Interaction k : KNOWN_INTERACTIONS) {
            boolean aP = all.stream().anyMatch(d -> d.contains(k.a));
            boolean bP = all.stream().anyMatch(d -> d.contains(k.b));
            if (aP && bP) {
                Map<String, Object> i = new LinkedHashMap<>();
                i.put("drug_a", k.a);
                i.put("drug_b", k.b);
                i.put("severity", k.severity);
                i.put("description", k.description);
                i.put("recommendation", k.recommendation);
                interactions.add(i);
            }
        }

        // Allergy / antecedent conflicts derived from the patient's free-text antecedents
        List<Map<String, Object>> allergyConflicts = new ArrayList<>();
        if (emp != null) {
            String allergyText = String.join(" | ",
                    safe(emp.getAntecedentsMedicaux()),
                    safe(emp.getAntecedentsChirurgicaux()),
                    safe(emp.getAntecedentsHereditaires())
            ).toLowerCase();

            for (String drug : newDrugs) {
                String lc = drug.toLowerCase();
                if (allergyText.contains("penicillin") || allergyText.contains("pénicilline") || allergyText.contains("allergie pénicilline")) {
                    if (lc.contains("penicillin") || lc.contains("amoxicillin") || lc.contains("ampicillin") || lc.contains("amox")) {
                        allergyConflicts.add(conflict(drug, "MAJOR",
                                "Patient documented penicillin allergy — direct beta-lactam exposure."));
                    } else if (lc.contains("cephalosporin") || lc.contains("cefa") || lc.contains("ceft")) {
                        allergyConflicts.add(conflict(drug, "MODERATE",
                                "Penicillin allergy: ~5% cephalosporin cross-reactivity. Choose a non-beta-lactam alternative if reaction was severe."));
                    }
                }
                if ((allergyText.contains("aspirin") || allergyText.contains("aspirine"))
                        && (lc.contains("aspirin") || lc.contains("aspirine"))) {
                    allergyConflicts.add(conflict(drug, "MAJOR",
                            "Patient documented aspirin allergy."));
                }
                if ((allergyText.contains("nsaid") || allergyText.contains("ains"))
                        && (lc.contains("ibuprofen") || lc.contains("naproxen") || lc.contains("diclofenac") || lc.contains("ains"))) {
                    allergyConflicts.add(conflict(drug, "MAJOR",
                            "Patient documented NSAID intolerance."));
                }
            }
        }

        boolean hasMajor = interactions.stream().anyMatch(i -> "MAJOR".equals(i.get("severity")))
                || allergyConflicts.stream().anyMatch(c -> "MAJOR".equals(c.get("severity")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("interactions", interactions);
        data.put("allergy_conflicts", allergyConflicts);
        data.put("safe_to_prescribe", !hasMajor);
        data.put("summary", hasMajor
                ? "UNSAFE: Major interactions or allergy conflicts detected."
                : "No major interactions found.");
        return ToolResult.ok(data);
    }

    private Map<String, Object> conflict(String drug, String sev, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("drug", drug);
        m.put("severity", sev);
        m.put("description", desc);
        return m;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static List<String> toStringList(List<?> in) {
        List<String> out = new ArrayList<>(in.size());
        for (Object o : in) if (o != null) out.add(o.toString());
        return out;
    }

    /** Minimal hardcoded interaction table — extend as needed. */
    private static final List<Interaction> KNOWN_INTERACTIONS = List.of(
            new Interaction("warfarin", "aspirin", "MAJOR",
                    "Increased bleeding risk.",
                    "Avoid combination or monitor INR closely."),
            new Interaction("warfarin", "ibuprofen", "MAJOR",
                    "Increased bleeding risk via platelet inhibition + INR rise.",
                    "Avoid; use paracetamol for analgesia."),
            new Interaction("warfarin", "amiodarone", "MAJOR",
                    "Amiodarone potentiates warfarin; INR rises within days.",
                    "Reduce warfarin dose ~30-50% and monitor INR weekly."),
            new Interaction("ssri", "maoi", "MAJOR",
                    "Risk of serotonin syndrome.",
                    "Contraindicated. Allow 14-day washout."),
            new Interaction("tramadol", "ssri", "MAJOR",
                    "Serotonin syndrome and lowered seizure threshold.",
                    "Avoid combination if possible."),
            new Interaction("metformin", "contrast", "MAJOR",
                    "Risk of lactic acidosis with iodinated contrast.",
                    "Hold metformin 48h before and after contrast."),
            new Interaction("digoxin", "amiodarone", "MAJOR",
                    "Digoxin toxicity risk.",
                    "Reduce digoxin dose by 50%, monitor levels."),
            new Interaction("sildenafil", "nitrate", "MAJOR",
                    "Severe hypotension.",
                    "Absolutely contraindicated."),
            new Interaction("statin", "clarithromycin", "MAJOR",
                    "CYP3A4 inhibition → rhabdomyolysis risk.",
                    "Hold statin during course or use azithromycin."),
            new Interaction("lithium", "nsaid", "MODERATE",
                    "NSAIDs reduce lithium clearance.",
                    "Monitor lithium levels closely."),
            new Interaction("lithium", "ibuprofen", "MODERATE",
                    "NSAIDs reduce lithium clearance.",
                    "Monitor lithium levels closely."),
            new Interaction("clopidogrel", "omeprazole", "MODERATE",
                    "Reduced antiplatelet effect via CYP2C19 inhibition.",
                    "Use pantoprazole instead."),
            new Interaction("ace inhibitor", "potassium", "MODERATE",
                    "Hyperkalemia risk.",
                    "Monitor potassium periodically."),
            new Interaction("ace", "spironolactone", "MODERATE",
                    "Hyperkalemia risk.",
                    "Monitor potassium periodically."),
            new Interaction("amoxicillin", "methotrexate", "MODERATE",
                    "Penicillins reduce methotrexate clearance.",
                    "Monitor methotrexate level and toxicity."),
            new Interaction("ciprofloxacin", "tizanidine", "MAJOR",
                    "Massive tizanidine exposure increase.",
                    "Contraindicated."),
            new Interaction("ciprofloxacin", "theophylline", "MODERATE",
                    "Increases theophylline levels.",
                    "Reduce theophylline dose; monitor levels.")
    );

    private record Interaction(String a, String b, String severity, String description, String recommendation) {}
}
