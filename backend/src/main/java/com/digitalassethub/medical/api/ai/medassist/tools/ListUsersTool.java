package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists staff users (medecins, coordinatrices, admins). Useful for
 * "who are our doctors?", "list coordinatrices", admin user-management
 * lookups, etc.
 */
@Component
public class ListUsersTool implements Tool {
    private final UserRepository users;

    public ListUsersTool(UserRepository users) {
        this.users = users;
    }

    @Override
    public String name() { return "list_users"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        String roleStr = input.get("role") == null ? null : input.get("role").toString().toUpperCase().trim();
        Role filter = null;
        if (roleStr != null && !roleStr.isEmpty() && !roleStr.equalsIgnoreCase("all")) {
            try { filter = Role.valueOf(roleStr); }
            catch (Exception ex) {
                return ToolResult.fail("BAD_INPUT",
                        "role must be one of: ADMIN, COORDINATRICE, MEDECIN, all");
            }
        }

        List<UserEntity> rows;
        if (filter != null) {
            rows = users.findByRole(filter);
        } else {
            rows = users.findAll();
        }

        int limit = toInt(input.get("limit"), 25);
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            UserEntity u = rows.get(i);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", u.getId());
            r.put("prenom", u.getPrenom());
            r.put("nom", u.getNom());
            // Email is sensitive — admins/coords see it; medecins only see colleague names.
            if (user.getRole() == Role.ADMIN || user.getRole() == Role.COORDINATRICE) {
                r.put("email", u.getEmail());
            }
            r.put("role", u.getRole().name());
            out.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filter_role", filter == null ? "all" : filter.name());
        data.put("count", out.size());
        data.put("total", rows.size());
        data.put("results", out);
        return ToolResult.ok(data);
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
