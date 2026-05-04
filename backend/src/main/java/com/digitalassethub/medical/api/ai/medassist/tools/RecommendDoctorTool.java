package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Match symptoms → specialty → in-house MEDECIN users.
 * The current schema does not store doctor specialty/rating/fee, so we return
 * the matched specialty alongside the available physicians.
 */
@Component
public class RecommendDoctorTool implements Tool {

    private final UserRepository users;

    public RecommendDoctorTool(UserRepository users) {
        this.users = users;
    }

    @Override
    public String name() { return "recommend_doctor"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        @SuppressWarnings("unchecked")
        List<String> symptoms = input.get("symptoms") instanceof List<?> l
                ? toStringList((List<?>) l) : List.of();
        String requested = input.get("specialty") == null ? null : input.get("specialty").toString().toLowerCase();
        int limit = toInt(input.get("limit"), 3);
        if (limit < 1) limit = 1;
        if (limit > 10) limit = 10;

        String specialty = requested;
        if (specialty == null || specialty.isBlank()) {
            for (String sym : symptoms) {
                String lc = sym.toLowerCase();
                for (Map.Entry<String, String> e : SYMPTOM_SPECIALTY_MAP.entrySet()) {
                    if (lc.contains(e.getKey())) { specialty = e.getValue(); break; }
                }
                if (specialty != null) break;
            }
        }
        if (specialty == null || specialty.isBlank()) specialty = "internal medicine";

        List<UserEntity> medecins = users.findByRole(Role.MEDECIN);
        List<Map<String, Object>> doctors = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, medecins.size()); i++) {
            UserEntity m = medecins.get(i);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", m.getId());
            d.put("nom", m.getNom());
            d.put("prenom", m.getPrenom());
            d.put("email", m.getEmail());
            d.put("telephone", m.getTelephone());
            doctors.add(d);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("specialty_matched", specialty);
        data.put("note", "Specialty/rating/fee filters are not stored on the user schema; "
                + "returning available MEDECIN users.");
        data.put("doctors", doctors);
        return ToolResult.ok(data);
    }

    private static List<String> toStringList(List<?> in) {
        List<String> out = new ArrayList<>(in.size());
        for (Object o : in) if (o != null) out.add(o.toString());
        return out;
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }

    /** Symptom → specialty mapping (extended). */
    private static final Map<String, String> SYMPTOM_SPECIALTY_MAP = new LinkedHashMap<>();
    static {
        SYMPTOM_SPECIALTY_MAP.put("chest pain", "cardiology");
        SYMPTOM_SPECIALTY_MAP.put("douleur thoracique", "cardiology");
        SYMPTOM_SPECIALTY_MAP.put("palpitations", "cardiology");
        SYMPTOM_SPECIALTY_MAP.put("hypertension", "cardiology");
        SYMPTOM_SPECIALTY_MAP.put("shortness of breath", "pulmonology");
        SYMPTOM_SPECIALTY_MAP.put("dyspnea", "pulmonology");
        SYMPTOM_SPECIALTY_MAP.put("cough", "pulmonology");
        SYMPTOM_SPECIALTY_MAP.put("toux", "pulmonology");
        SYMPTOM_SPECIALTY_MAP.put("asthma", "pulmonology");
        SYMPTOM_SPECIALTY_MAP.put("rash", "dermatology");
        SYMPTOM_SPECIALTY_MAP.put("acne", "dermatology");
        SYMPTOM_SPECIALTY_MAP.put("eczema", "dermatology");
        SYMPTOM_SPECIALTY_MAP.put("psoriasis", "dermatology");
        SYMPTOM_SPECIALTY_MAP.put("headache", "neurology");
        SYMPTOM_SPECIALTY_MAP.put("céphalée", "neurology");
        SYMPTOM_SPECIALTY_MAP.put("seizure", "neurology");
        SYMPTOM_SPECIALTY_MAP.put("memory loss", "neurology");
        SYMPTOM_SPECIALTY_MAP.put("migraine", "neurology");
        SYMPTOM_SPECIALTY_MAP.put("abdominal pain", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("douleur abdominale", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("nausea", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("vomiting", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("diarrhea", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("constipation", "gastroenterology");
        SYMPTOM_SPECIALTY_MAP.put("joint pain", "rheumatology");
        SYMPTOM_SPECIALTY_MAP.put("arthralgia", "rheumatology");
        SYMPTOM_SPECIALTY_MAP.put("back pain", "orthopedics");
        SYMPTOM_SPECIALTY_MAP.put("dorsalgie", "orthopedics");
        SYMPTOM_SPECIALTY_MAP.put("fracture", "orthopedics");
        SYMPTOM_SPECIALTY_MAP.put("sprain", "orthopedics");
        SYMPTOM_SPECIALTY_MAP.put("fever", "internal medicine");
        SYMPTOM_SPECIALTY_MAP.put("fièvre", "internal medicine");
        SYMPTOM_SPECIALTY_MAP.put("fatigue", "internal medicine");
        SYMPTOM_SPECIALTY_MAP.put("weight loss", "internal medicine");
        SYMPTOM_SPECIALTY_MAP.put("depression", "psychiatry");
        SYMPTOM_SPECIALTY_MAP.put("anxiety", "psychiatry");
        SYMPTOM_SPECIALTY_MAP.put("insomnia", "psychiatry");
        SYMPTOM_SPECIALTY_MAP.put("vision loss", "ophthalmology");
        SYMPTOM_SPECIALTY_MAP.put("eye pain", "ophthalmology");
        SYMPTOM_SPECIALTY_MAP.put("ear pain", "otolaryngology");
        SYMPTOM_SPECIALTY_MAP.put("sore throat", "otolaryngology");
        SYMPTOM_SPECIALTY_MAP.put("hearing loss", "otolaryngology");
        SYMPTOM_SPECIALTY_MAP.put("diabetes", "endocrinology");
        SYMPTOM_SPECIALTY_MAP.put("thyroid", "endocrinology");
        SYMPTOM_SPECIALTY_MAP.put("obesity", "endocrinology");
        SYMPTOM_SPECIALTY_MAP.put("pregnancy", "obstetrics");
        SYMPTOM_SPECIALTY_MAP.put("menstrual", "gynecology");
        SYMPTOM_SPECIALTY_MAP.put("infertility", "gynecology");
        SYMPTOM_SPECIALTY_MAP.put("child", "pediatrics");
        SYMPTOM_SPECIALTY_MAP.put("infant", "pediatrics");
        SYMPTOM_SPECIALTY_MAP.put("vaccination", "pediatrics");
        SYMPTOM_SPECIALTY_MAP.put("kidney", "nephrology");
        SYMPTOM_SPECIALTY_MAP.put("urination", "urology");
        SYMPTOM_SPECIALTY_MAP.put("prostate", "urology");
        SYMPTOM_SPECIALTY_MAP.put("cancer", "oncology");
        SYMPTOM_SPECIALTY_MAP.put("tumor", "oncology");
        SYMPTOM_SPECIALTY_MAP.put("allergy", "allergy and immunology");
        SYMPTOM_SPECIALTY_MAP.put("workplace injury", "occupational medicine");
        SYMPTOM_SPECIALTY_MAP.put("burnout", "occupational medicine");
        SYMPTOM_SPECIALTY_MAP.put("noise exposure", "occupational medicine");
    }
}
