package com.digitalassethub.medical.api.ai.medassist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process per-session message store with TTL and threshold-based summarization.
 *
 * Replaces the spec's Redis-backed store. Suitable for a single-instance
 * Spring Boot deployment; swap to Redis if/when horizontal scaling is needed.
 */
@Component
public class MemoryManager {
    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    private final OpenRouterClient openRouterClient;

    @Value("${medassist.session-ttl-seconds:7200}")
    private long ttlSeconds;

    @Value("${medassist.max-history-messages:30}")
    private int maxHistoryMessages;

    @Value("${medassist.summary-threshold:20}")
    private int summaryThreshold;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public MemoryManager(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public List<Map<String, Object>> load(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return new ArrayList<>();
        Entry e = store.get(sessionId);
        if (e == null) return new ArrayList<>();
        if (System.currentTimeMillis() > e.expiry) {
            store.remove(sessionId);
            return new ArrayList<>();
        }
        return new ArrayList<>(e.messages);
    }

    public void save(String sessionId, List<Map<String, Object>> messages) {
        if (sessionId == null || sessionId.isBlank()) return;
        // hard cap to avoid unbounded growth even before summarization kicks in
        List<Map<String, Object>> trimmed = messages.size() > maxHistoryMessages
                ? new ArrayList<>(messages.subList(messages.size() - maxHistoryMessages, messages.size()))
                : new ArrayList<>(messages);
        store.put(sessionId, new Entry(trimmed, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    public void clear(String sessionId) {
        if (sessionId != null) store.remove(sessionId);
    }

    /**
     * Summarize the oldest half of the history when length exceeds {@link #summaryThreshold}.
     * Falls back to simple truncation if the model call fails.
     */
    public List<Map<String, Object>> summarizeIfNeeded(List<Map<String, Object>> messages) {
        if (messages.size() <= summaryThreshold) return messages;

        int splitAt = summaryThreshold / 2;
        List<Map<String, Object>> toSummarize = messages.subList(0, splitAt);
        List<Map<String, Object>> toKeep = new ArrayList<>(messages.subList(splitAt, messages.size()));

        StringBuilder convo = new StringBuilder();
        for (Map<String, Object> m : toSummarize) {
            convo.append(m.getOrDefault("role", "user"))
                 .append(": ")
                 .append(m.getOrDefault("content", ""))
                 .append("\n");
        }

        try {
            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content",
                    "Summarize this medical conversation. Preserve: all clinical data mentioned, "
                            + "medications, diagnoses, decisions made, and patient concerns. "
                            + "Be concise but clinically complete.\n\n" + convo);

            OpenRouterClient.ChatCompletionResult result =
                    openRouterClient.complete(List.of(userMsg), 600, 0.3);
            String summary = result.content() == null ? "" : result.content()
                    .replaceAll("(?s)<think>.*?</think>", "")
                    .trim();

            List<Map<String, Object>> out = new ArrayList<>();
            Map<String, Object> sum = new LinkedHashMap<>();
            sum.put("role", "user");
            sum.put("content", "[Previous conversation summary]: " + summary);
            out.add(sum);

            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("role", "assistant");
            ack.put("content", "Compris. J'ai le contexte de notre discussion précédente.");
            out.add(ack);

            out.addAll(toKeep);
            return out;

        } catch (Exception ex) {
            log.warn("Summarization failed, falling back to truncation: {}", ex.getMessage());
            return new ArrayList<>(messages.subList(messages.size() - summaryThreshold, messages.size()));
        }
    }

    private record Entry(List<Map<String, Object>> messages, long expiry) {}
}
