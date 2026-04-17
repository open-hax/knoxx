import { API_BASE } from "./api";
import type { WsMessage } from "./types";

function wsUrl(base: string, sessionId?: string, conversationId?: string | null): string {
  const params = new URLSearchParams();
  if (sessionId) params.set("session_id", sessionId);
  if (conversationId) params.set("conversation_id", conversationId);
  const q = params.toString() ? `?${params.toString()}` : "";

  if (!base) {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${window.location.host}/ws/stream${q}`;
  }
  const url = new URL(base);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = "/ws/stream";
  url.search = q;
  return url.toString();
}

export interface StreamHandlers {
  onToken?: (token: string, meta?: { runId?: string; kind?: string }) => void;
  onStats?: (stats: Record<string, unknown>) => void;
  onConsole?: (line: string) => void;
  onEvent?: (event: Record<string, unknown>) => void;
  onLounge?: (message: Record<string, unknown>) => void;
  onStatus?: (status: "connected" | "closed" | "error") => void;
}

export interface StreamConnection {
  (): void; // Callable as cleanup function
  disconnect: () => void;
  setConversationId: (conversationId: string | null) => void;
}

export function connectStream(
  handlers: StreamHandlers,
  sessionId?: string,
  initialConversationId?: string | null,
): StreamConnection {
  let socket: WebSocket | null = null;
  let currentConversationId = initialConversationId ?? null;
  let disposed = false;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let reconnectDelay = 1000;
  const MAX_RECONNECT_DELAY = 30000;

  const connectSocket = () => {
    if (disposed) return;
    socket = new WebSocket(wsUrl(API_BASE, sessionId, currentConversationId));

    socket.addEventListener("open", () => {
      reconnectDelay = 1000; // reset on successful connect
      handlers.onStatus?.("connected");
    });
    socket.addEventListener("close", () => {
      handlers.onStatus?.("closed");
      // Auto-reconnect unless explicitly disconnected
      if (!disposed) {
        reconnectTimer = setTimeout(connectSocket, reconnectDelay);
        reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
      }
    });
    socket.addEventListener("error", () => {
      handlers.onStatus?.("error");
    });

    socket.addEventListener("message", (event) => {
      try {
        const message = JSON.parse(event.data as string) as WsMessage;
        const payload = message.payload ?? {};

        if (message.channel === "tokens") {
          handlers.onToken?.(String(payload.token ?? ""), {
            runId: payload.run_id as string | undefined,
            kind: typeof payload.kind === "string" ? payload.kind : undefined,
          });
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
  };

  connectSocket();

  const disconnect = () => {
    disposed = true;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    socket?.close();
  };
  const setConversationId = (conversationId: string | null) => {
    if (conversationId !== currentConversationId) {
      currentConversationId = conversationId;
      if (!socket) {
        connectSocket();
        return;
      }
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "set_conversation", conversation_id: conversationId }));
      } else if (socket.readyState === WebSocket.CLOSED) {
        connectSocket();
      }
    }
  };

  // Return a callable function with additional methods
  const connection = Object.assign(disconnect, { disconnect, setConversationId });
  return connection as StreamConnection;
}
