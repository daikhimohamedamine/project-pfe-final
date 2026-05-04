package com.digitalassethub.medical.api.ai.medassist;

import com.digitalassethub.medical.api.user.UserEntity;
import java.util.Map;

/**
 * A single MedAssist tool. Each implementation is a Spring component
 * registered in {@link ToolRegistry}.
 */
public interface Tool {

    /** Stable name used in the model's `tool_call` JSON. */
    String name();

    /** Execute the tool against the live database. */
    ToolResult execute(Map<String, Object> input, UserEntity user);
}
