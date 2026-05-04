package com.digitalassethub.medical.api.ai.medassist;

import com.digitalassethub.medical.api.user.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {
    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private final Map<String, Tool> tools = new HashMap<>();

    public ToolRegistry(List<Tool> all) {
        for (Tool t : all) tools.put(t.name(), t);
        log.info("MedAssist tools registered: {}", tools.keySet());
    }

    public ToolResult execute(String name, Map<String, Object> input, UserEntity user) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.fail("UNKNOWN_TOOL", "Tool '" + name + "' does not exist.");
        }
        try {
            return tool.execute(input == null ? Map.of() : input, user);
        } catch (Exception ex) {
            log.error("Tool '{}' failed: {}", name, ex.getMessage(), ex);
            return ToolResult.fail("HANDLER_ERROR",
                    ex.getMessage() == null ? "Tool execution failed." : ex.getMessage());
        }
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }
}
