package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.drug.DrugEntity;
import com.digitalassethub.medical.api.drug.DrugRepository;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchMedicalLibraryTool implements Tool {
    private final DrugRepository drugs;

    public SearchMedicalLibraryTool(DrugRepository drugs) {
        this.drugs = drugs;
    }

    @Override
    public String name() { return "search_medical_library"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        String query = input.get("query") == null ? "" : input.get("query").toString().trim();
        if (query.isEmpty()) {
            return ToolResult.fail("BAD_INPUT", "query is required");
        }
        int limit = toInt(input.get("limit"), 5);
        if (limit < 1) limit = 1;
        if (limit > 20) limit = 20;

        // The platform's "medical library" is currently the drug catalogue (with
        // sicknesses/indications metadata) — see DrugEntity.
        List<DrugEntity> hits;
        try {
            hits = drugs.searchByKeyword(query);
        } catch (Exception ex) {
            // Fallback to JPA derived query if the native LIKE query fails for any reason.
            hits = drugs.findByDrugNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
                    query, query, PageRequest.of(0, limit)
            ).getContent();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, hits.size()); i++) {
            DrugEntity d = hits.get(i);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", d.getId());
            r.put("type", "drug_monograph");
            r.put("drug_name", d.getDrugName());
            r.put("generic_name", d.getGenericName());
            r.put("dosage", d.getDosage());
            r.put("indications", d.getIndications());
            r.put("sicknesses", d.getSicknesses());
            results.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", query);
        data.put("count", results.size());
        data.put("results", results);
        return ToolResult.ok(data);
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
