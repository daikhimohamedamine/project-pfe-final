package com.digitalassethub.medical.api.ai.medassist;

import com.digitalassethub.medical.api.user.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agentic loop driving the Nemotron reasoning model on OpenRouter.
 *
 * Loop:
 *   1. Build messages: [system, ...history, {user: input}]
 *   2. Call OpenRouter
 *   3. Strip `<think>` blocks
 *   4. If response contains a ```tool_call``` block:
 *        execute tool → append assistant turn + user turn with `[TOOL RESULT: name]`
 *        loop again (max {@code MAX_TOOL_ITERATIONS} times)
 *      else: stop, return cleaned text.
 *
 * History is persisted in {@link MemoryManager} keyed by sessionId.
 */
@Service
public class MedAssistEngine {
    private static final Logger log = LoggerFactory.getLogger(MedAssistEngine.class);

    private final OpenRouterClient client;
    private final MedAssistSystemPrompt systemPrompt;
    private final ToolParser parser;
    private final ToolRegistry tools;
    private final MemoryManager memory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${medassist.max-tool-iterations:6}")
    private int maxToolIterations;

    public MedAssistEngine(OpenRouterClient client,
                           MedAssistSystemPrompt systemPrompt,
                           ToolParser parser,
                           ToolRegistry tools,
                           MemoryManager memory) {
        this.client = client;
        this.systemPrompt = systemPrompt;
        this.parser = parser;
        this.tools = tools;
        this.memory = memory;
    }

    public Reply chat(UserEntity user, String userMessage, String sessionId,
                      List<Map<String, String>> clientHistory) {

        // Resolve effective conversation history: server-side memory wins; fall back to
        // client-provided history when no sessionId is supplied.
        List<Map<String, Object>> history;
        boolean useServerMemory = sessionId != null && !sessionId.isBlank();
        if (useServerMemory) {
            history = memory.load(sessionId);
        } else {
            history = new ArrayList<>();
            if (clientHistory != null) {
                for (Map<String, String> h : clientHistory) {
                    if (h == null) continue;
                    String role = "assistant".equalsIgnoreCase(h.get("role")) ? "assistant" : "user";
                    String content = h.get("content");
                    if (content == null || content.isBlank()) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("role", role);
                    m.put("content", content);
                    history.add(m);
                }
            }
        }

        // Append the new user turn
        Map<String, Object> userTurn = new LinkedHashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        history.add(userTurn);

        // Summarize if history grew too long
        history = memory.summarizeIfNeeded(history);

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt.build(user));

        List<ToolCallTrace> trace = new ArrayList<>();
        String thinking = null;
        String finalText = null;

        for (int iteration = 0; iteration < maxToolIterations; iteration++) {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(system);
            messages.addAll(history);

            OpenRouterClient.ChatCompletionResult result;
            try {
                result = client.complete(messages);
            } catch (Exception ex) {
                log.error("OpenRouter call failed: {}", ex.getMessage(), ex);
                Map<String, Object> errTurn = new LinkedHashMap<>();
                errTurn.put("role", "assistant");
                errTurn.put("content", "Erreur du service IA: " + ex.getMessage());
                history.add(errTurn);
                if (useServerMemory) memory.save(sessionId, history);
                return new Reply("Erreur du service IA: " + ex.getMessage(), null, trace, true);
            }

            String raw = result.content() == null ? "" : result.content();
            if (thinking == null) {
                thinking = result.reasoning();
                if (thinking == null || thinking.isBlank()) {
                    thinking = parser.extractThinkingBlock(raw);
                }
            }

            ToolParser.ParsedToolCall call = parser.extractToolCall(raw);

            if (call == null) {
                String clean = parser.stripToolCallBlock(raw);
                finalText = clean.isBlank() ? "..." : clean;

                Map<String, Object> assistantTurn = new LinkedHashMap<>();
                assistantTurn.put("role", "assistant");
                assistantTurn.put("content", finalText);
                history.add(assistantTurn);
                break;
            }

            // Persist the assistant's tool-call turn verbatim so the model sees its own history
            Map<String, Object> assistantToolTurn = new LinkedHashMap<>();
            assistantToolTurn.put("role", "assistant");
            assistantToolTurn.put("content", "```tool_call\n" + call.rawJson() + "\n```");
            history.add(assistantToolTurn);

            ToolResult toolResult = tools.execute(call.tool(), call.input(), user);
            trace.add(new ToolCallTrace(call.tool(), call.input(), toolResult));

            String resultJson;
            try {
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("success", toolResult.success());
                if (toolResult.success()) {
                    envelope.put("data", toolResult.data());
                    if (toolResult.message() != null) envelope.put("message", toolResult.message());
                } else {
                    envelope.put("error", toolResult.error());
                    envelope.put("message", toolResult.message());
                    if (toolResult.data() != null) envelope.put("data", toolResult.data());
                }
                resultJson = mapper.writeValueAsString(envelope);
            } catch (JsonProcessingException ex) {
                resultJson = "{\"success\":false,\"error\":\"SERIALIZATION_ERROR\",\"message\":\""
                        + ex.getMessage() + "\"}";
            }

            Map<String, Object> toolTurn = new LinkedHashMap<>();
            toolTurn.put("role", "user");
            toolTurn.put("content", "[TOOL RESULT: " + call.tool() + "]\n" + resultJson);
            history.add(toolTurn);
        }

        if (finalText == null) {
            finalText = "Désolé, j'ai atteint la limite d'itérations sans pouvoir conclure. "
                    + "Veuillez reformuler votre demande.";
            Map<String, Object> assistantTurn = new LinkedHashMap<>();
            assistantTurn.put("role", "assistant");
            assistantTurn.put("content", finalText);
            history.add(assistantTurn);
        }

        if (useServerMemory) memory.save(sessionId, history);

        return new Reply(finalText, thinking, trace, false);
    }

    public record ToolCallTrace(String tool, Map<String, Object> input, ToolResult result) {}

    public record Reply(String response, String thinking, List<ToolCallTrace> toolCalls, boolean error) {}
}
