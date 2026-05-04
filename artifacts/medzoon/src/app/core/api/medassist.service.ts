import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { API_BASE_URL } from './api.config';

const TOKEN_KEY = 'medzoon.auth.token';
const SESSION_KEY = 'medzoon.medassist.session';

export type ChatMessageDto = { role: 'user' | 'assistant'; content: string };

export type StreamEvent =
  | { type: 'tool_call'; tool: string; input: Record<string, unknown> }
  | { type: 'tool_result'; tool: string; success: boolean; data: unknown; error?: string }
  | { type: 'thinking'; text: string }
  | { type: 'response'; text: string }
  | { type: 'done'; ok: boolean }
  | { type: 'error'; message: string };

/**
 * Streaming client for the MedAssist agentic engine.
 * Connects to POST /api/v1/ai/stream (Server-Sent Events) and emits typed events.
 */
@Injectable({ providedIn: 'root' })
export class MedAssistService {
  private readonly streamUrl = `${API_BASE_URL}/ai/stream`;
  private readonly fallbackUrl = `${API_BASE_URL}/ai/chat`;
  private readonly sessionUrl = `${API_BASE_URL}/ai/sessions`;

  /** Get or lazily mint a stable per-browser session id. */
  sessionId(): string {
    let id = localStorage.getItem(SESSION_KEY);
    if (!id) {
      id = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
      localStorage.setItem(SESSION_KEY, id);
    }
    return id;
  }

  resetSession(): void {
    const id = localStorage.getItem(SESSION_KEY);
    if (!id) return;
    localStorage.removeItem(SESSION_KEY);
    // Best-effort server-side wipe — fire and forget.
    fetch(`${this.sessionUrl}/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: this.authHeaders(),
    }).catch(() => undefined);
  }

  /**
   * Stream a chat turn. Falls back to the non-streaming JSON endpoint if the
   * server doesn't support SSE for this client (e.g. older proxy in front).
   */
  stream(message: string, history: ChatMessageDto[]): Observable<StreamEvent> {
    const subject = new Subject<StreamEvent>();
    const controller = new AbortController();

    fetch(this.streamUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...this.authHeaders(),
      },
      body: JSON.stringify({ message, history, sessionId: this.sessionId() }),
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok || !res.body) {
          await this.fallbackToJson(message, history, subject);
          return;
        }
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          let idx: number;
          while ((idx = buffer.indexOf('\n\n')) !== -1) {
            const chunk = buffer.slice(0, idx);
            buffer = buffer.slice(idx + 2);
            const evt = this.parseSseChunk(chunk);
            if (evt) subject.next(evt);
          }
        }
        if (buffer.trim().length > 0) {
          const evt = this.parseSseChunk(buffer);
          if (evt) subject.next(evt);
        }
        subject.complete();
      })
      .catch(async (err) => {
        if (controller.signal.aborted) {
          subject.complete();
          return;
        }
        try {
          await this.fallbackToJson(message, history, subject);
        } catch {
          subject.next({ type: 'error', message: err?.message ?? 'Erreur réseau' });
          subject.complete();
        }
      });

    return new Observable<StreamEvent>((sub) => {
      const inner = subject.subscribe(sub);
      return () => {
        inner.unsubscribe();
        controller.abort();
      };
    });
  }

  private parseSseChunk(chunk: string): StreamEvent | null {
    const lines = chunk.split('\n');
    let event: string | null = null;
    let data = '';
    for (const line of lines) {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) data += line.slice(5).trim();
    }
    if (!event) return null;
    let payload: any = {};
    try { payload = data ? JSON.parse(data) : {}; } catch { payload = { raw: data }; }
    switch (event) {
      case 'tool_call':
        return { type: 'tool_call', tool: payload.tool, input: payload.input ?? {} };
      case 'tool_result':
        return {
          type: 'tool_result',
          tool: payload.tool,
          success: !!payload.success,
          data: payload.data,
          error: payload.error || undefined,
        };
      case 'thinking':
        return { type: 'thinking', text: payload.text ?? '' };
      case 'response':
        return { type: 'response', text: payload.text ?? '' };
      case 'done':
        return { type: 'done', ok: payload.ok !== false };
      case 'error':
        return { type: 'error', message: payload.message ?? 'Erreur' };
      default:
        return null;
    }
  }

  /**
   * Fallback to the buffered JSON endpoint, then synthesize a stream of
   * {tool_call, tool_result, thinking, response, done} events from the reply.
   */
  private async fallbackToJson(
    message: string,
    history: ChatMessageDto[],
    subject: Subject<StreamEvent>,
  ): Promise<void> {
    const res = await fetch(this.fallbackUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...this.authHeaders() },
      body: JSON.stringify({ message, history, sessionId: this.sessionId() }),
    });
    if (!res.ok) {
      const text = await res.text().catch(() => res.statusText);
      subject.next({ type: 'error', message: `${res.status}: ${text || res.statusText}` });
      subject.complete();
      return;
    }
    const body = await res.json().catch(() => ({} as any));
    const calls: Array<{ tool: string; input: any; result: any }> = body.toolCalls || [];
    for (const c of calls) {
      subject.next({ type: 'tool_call', tool: c.tool, input: c.input ?? {} });
      subject.next({
        type: 'tool_result',
        tool: c.tool,
        success: !!c.result?.success,
        data: c.result?.data,
        error: c.result?.error || undefined,
      });
    }
    if (body.thinking) subject.next({ type: 'thinking', text: body.thinking });
    if (body.response) subject.next({ type: 'response', text: body.response });
    subject.next({ type: 'done', ok: !body.error });
    subject.complete();
  }

  private authHeaders(): Record<string, string> {
    const token = localStorage.getItem(TOKEN_KEY);
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
}
