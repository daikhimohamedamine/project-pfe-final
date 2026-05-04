package com.digitalassethub.medical.api.ai;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private List<Map<String, String>> history;
    private String sessionId;
}
