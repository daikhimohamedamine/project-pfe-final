package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.employee.EmployeeEntity;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists / searches employees (= patients).
 * MEDECIN sees only their assigned patients (medecin_id = current user).
 * COORDINATRICE and ADMIN see everyone.
 */
@Component
public class ListEmployeesTool implements Tool {
    private final EmployeeRepository employees;

    public ListEmployeesTool(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public String name() { return "list_employees"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        String query = input.get("query") == null ? "" : input.get("query").toString().trim();
        int limit = toInt(input.get("limit"), 10);
        if (limit < 1) limit = 1;
        if (limit > 50) limit = 50;

        var pageable = PageRequest.of(0, limit);
        var nom = query;
        var prenom = query;

        List<EmployeeEntity> rows;
        long total;
        if (user.getRole() == Role.MEDECIN) {
            var page = employees.findByMedecinIdAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
                    user.getId(), nom, prenom, pageable);
            rows = page.getContent();
            total = page.getTotalElements();
        } else if (user.getRole() == Role.COORDINATRICE && user.getAssignedMedecinId() != null) {
            var page = employees.findByMedecinIdAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
                    user.getAssignedMedecinId(), nom, prenom, pageable);
            rows = page.getContent();
            total = page.getTotalElements();
        } else {
            var page = employees.findByNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
                    nom, prenom, pageable);
            rows = page.getContent();
            total = page.getTotalElements();
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (EmployeeEntity e : rows) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", e.getId());
            r.put("dossier_number", e.getDossierNumber());
            r.put("prenom", e.getPrenom());
            r.put("nom", e.getNom());
            r.put("poste_travail", e.getPosteTravail());
            r.put("departement", e.getDepartement());
            r.put("medecin_id", e.getMedecinId());
            r.put("statut", e.getStatut());
            out.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("query", query);
        data.put("scope", user.getRole() == Role.MEDECIN ? "assigned_to_me" : "all");
        data.put("count", out.size());
        data.put("total", total);
        data.put("results", out);
        return ToolResult.ok(data);
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
