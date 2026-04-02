import { API_BASE } from "./api";
import type { WsMessage } from "./types";

function wsUrl(base: string, sessionId?: string): string {
  if (!base) {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const q = sessionId ? `?session_id=${encodeURIComponent(sessionId)}` : "";
    return `${protocol}://${window.location.host}/ws/stream${q}`;
  }
  const url = new URL(base);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = "/ws/stream";
  url.search = sessionId ? `session_id=${encodeURIComponent(sessionId)}` : "";
  return url.toString();
}

export interface StreamHandlers {
  onToken?: (token: string, runId?: string) => void;
  onStats?: (stats: Record<string, unknown>) => void;
  onConsole?: (line: string) => void;
  onEvent?: (event: Record<string, unknown>) => void;
  onLounge?: (message: Record<string, unknown>) => void;
  onStatus?: (status: "connected" | "closed" | "error") => void;
}

export function connectStream(handlers: StreamHandlers, sessionId?: string): () => void {
  const socket = new WebSocket(wsUrl(API_BASE, sessionId));

  socket.addEventListener("open", () => handlers.onStatus?.("connected"));
  socket.addEventListener("close", () => handlers.onStatus?.("closed"));
  socket.addEventListener("error", () => handlers.onStatus?.("error"));

  socket.addEventListener("message", (event) => {
    try {
      const message = JSON.parse(event.data as string) as WsMessage;
      const payload = message.payload ?? {};

      if (message.channel === "tokens") {
        handlers.onToken?.(String(payload.token ?? ""), payload.run_id as string | undefined);
      } else if (message.channel === "stats") {
        handlers.onStats?.(payload);
      } else if (message.channel === "console") {
        handlers.onConsole?.(`[${String(payload.stream ?? "log")}] ${String(payload.line ?? "")}`);
      } else if (message.channel === "events") {
        handlers.onEvent?.(payload);
      } else if (message.channel === "lounge") {
        handlers.onLounge?.(payload);
      }
    } catch {
      handlers.onConsole?.("Malformed websocket packet");
    }
  });

  return () => socket.close();
}
