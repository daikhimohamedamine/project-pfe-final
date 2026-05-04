package com.digitalassethub.medical.api.ai;

import com.digitalassethub.medical.api.ai.medassist.MedAssistEngine;
import com.digitalassethub.medical.api.user.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final MedAssistEngine medAssistEngine;

    public AiChatService(MedAssistEngine medAssistEngine) {
        this.medAssistEngine = medAssistEngine;
    }

    public Map<String, Object> chat(UserEntity user, ChatRequest request) {
        log.info("--- AGENTIC AI MODE (NVIDIA/OPENROUTER) ---");
        
        try {
            log.info("Starting AI chat for user: {}", user != null ? user.getEmail() : "NULL");
            var reply = medAssistEngine.chat(user, request.getMessage(), request.getSessionId(), request.getHistory());
            
            Map<String, Object> responseMap = new java.util.HashMap<>();
            responseMap.put("response", reply.response() != null ? reply.response() : "Désolé, je n'ai pas pu générer de réponse.");
            responseMap.put("thinking", reply.thinking() != null ? reply.thinking() : "");
            responseMap.put("toolCalls", reply.toolCalls() != null ? reply.toolCalls() : java.util.List.of());
            
            log.info("Successfully generated response for user: {}", user != null ? user.getEmail() : "NULL");
            return responseMap;
        } catch (Exception e) {
            log.error("CRITICAL AI ERROR in AiChatService: {}", e.getMessage(), e);
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("response", "Erreur technique critique : " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            err.put("error", true);
            return err;
        }
    }
}
