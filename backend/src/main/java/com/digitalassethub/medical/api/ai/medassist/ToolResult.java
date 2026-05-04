package com.digitalassethub.medical.api.ai.medassist;

/**
 * Standard envelope returned by every tool handler.
 * Mirrors the spec contract `{ success, data?, error?, message? }`.
 */
public record ToolResult(boolean success, Object data, String error, String message) {

    public static ToolResult ok(Object data) {
        return new ToolResult(true, data, null, null);
    }

    public static ToolResult ok(Object data, String message) {
        return new ToolResult(true, data, null, message);
    }

    public static ToolResult fail(String error, String message) {
        return new ToolResult(false, null, error, message);
    }

    public static ToolResult fail(String error, String message, Object data) {
        return new ToolResult(false, data, error, message);
    }
}
