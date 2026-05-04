package com.digitalassethub.medical.api.ai.medassist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts ```tool_call``` JSON blocks and `<think>` reasoning blocks from
 * Nemotron output. Mirrors the TypeScript reference implementation in the spec.
 */
@Component
public class ToolParser {
    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("```tool_call\\s*([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern THINK_PATTERN =
            Pattern.compile("<think>([\\s\\S]*?)</think>", Pattern.MULTILINE);

    private final ObjectMapper mapper = new ObjectMapper();

    public ParsedToolCall extractToolCall(String text) {
        if (text == null || text.isBlank()) return null;
        String stripped = THINK_PATTERN.matcher(text).replaceAll("").trim();
        Matcher m = TOOL_CALL_PATTERN.matcher(stripped);
        if (!m.find()) return null;
        String json = m.group(1).trim();
        try {
            JsonNode node = mapper.readTree(json);
            String tool = node.path("tool").asText(null);
            JsonNode inputNode = node.path("input");
            if (tool == null || tool.isBlank() || inputNode.isMissingNode() || inputNode.isNull()) {
                return null;
            }
            Map<String, Object> input = new HashMap<>();
            if (inputNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = inputNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    input.put(e.getKey(), nodeToValue(e.getValue()));
                }
            }
            return new ParsedToolCall(tool, input, json);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String stripToolCallBlock(String text) {
        if (text == null) return "";
        String s = THINK_PATTERN.matcher(text).replaceAll("");
        s = TOOL_CALL_PATTERN.matcher(s).replaceAll("");
        return s.trim();
    }

    public String extractThinkingBlock(String text) {
        if (text == null) return null;
        Matcher m = THINK_PATTERN.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private Object nodeToValue(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isTextual()) return n.asText();
        if (n.isInt()) return n.asInt();
        if (n.isLong()) return n.asLong();
        if (n.isDouble() || n.isFloat()) return n.asDouble();
        if (n.isBoolean()) return n.asBoolean();
        if (n.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (JsonNode child : n) list.add(nodeToValue(child));
            return list;
        }
        if (n.isObject()) {
            Map<String, Object> map = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = n.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                map.put(e.getKey(), nodeToValue(e.getValue()));
            }
            return map;
        }
        return n.asText();
    }

    public record ParsedToolCall(String tool, Map<String, Object> input, String rawJson) {}
}
