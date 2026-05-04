package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.audit.AuditLogEntity;
import com.digitalassethub.medical.api.audit.AuditLogRepository;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns recent audit log entries. ADMIN-only.
 */
@Component
public class ListAuditLogsTool implements Tool {
    private final AuditLogRepository auditLogs;

    public ListAuditLogsTool(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @Override
    public String name() { return "list_audit_logs"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        if (user.getRole() != Role.ADMIN) {
            return ToolResult.fail("FORBIDDEN",
                    "Le journal d'audit est réservé aux administrateurs.");
        }

        int limit = toInt(input.get("limit"), 20);
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        var page = auditLogs.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp")));

        String actionFilter = input.get("action") == null ? null : input.get("action").toString().toUpperCase();
        String entityFilter = input.get("entity_type") == null ? null : input.get("entity_type").toString();

        List<Map<String, Object>> out = new ArrayList<>();
        for (AuditLogEntity log : page.getContent()) {
            if (actionFilter != null && (log.getAction() == null
                    || !log.getAction().toUpperCase().contains(actionFilter))) continue;
            if (entityFilter != null && (log.getEntityType() == null
                    || !log.getEntityType().equalsIgnoreCase(entityFilter))) continue;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", log.getId());
            r.put("timestamp", log.getTimestamp() == null ? null : log.getTimestamp().toString());
            r.put("user_id", log.getUserId());
            r.put("user_role", log.getUserRole());
            r.put("action", log.getAction());
            r.put("entity_type", log.getEntityType());
            r.put("entity_id", log.getEntityId());
            r.put("ip_address", log.getIpAddress());
            r.put("details", log.getDetails());
            out.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", out.size());
        data.put("total_recent", page.getContent().size());
        data.put("results", out);
        return ToolResult.ok(data);
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return def;
    }
}
