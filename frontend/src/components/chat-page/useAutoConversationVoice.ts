import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { API_BASE } from "../../lib/api";
import type { ChatMessage } from "../../lib/types";
import { ensureTrailingSpeechSpace, extractAutoConversationChunks } from "./auto-conversation-voice-utils";

type AutoConversationVoiceStatus = "idle" | "connecting" | "streaming" | "playing" | "error";

type VoiceSocketMessage =
  | { type: "ready" }
  | { type: "audio"; audio?: string }
  | { type: "final" }
  | { type: "error"; detail?: string }
  | { type: "upstream_closed"; reason?: string }
  | { type: string; [key: string]: unknown };

type UseAutoConversationVoiceParams = {
  enabled: boolean;
  available: boolean;
  messages: ChatMessage[];
  defaultVoiceId?: string;
  appendConsoleLine?: (line: string) => void;
};

type UseAutoConversationVoiceResult = {
  status: AutoConversationVoiceStatus;
  error: string | null;
};

function voiceStreamUrl(): string {
  if (!API_BASE) {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${window.location.host}/ws/voice/tts`;
  }

  const url = new URL(API_BASE, window.location.origin);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = "/ws/voice/tts";
  url.search = "";
  return url.toString();
}

function decodeBase64Audio(base64: string): Uint8Array {
  const binary = window.atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

export function useAutoConversationVoice({
  enabled,
  available,
  messages,
  defaultVoiceId,
  appendConsoleLine,
}: UseAutoConversationVoiceParams): UseAutoConversationVoiceResult {
  const [status, setStatus] = useState<AutoConversationVoiceStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const socketReadyRef = useRef(false);
  const currentMessageIdRef = useRef<string | null>(null);
  const previousEnabledRef = useRef(false);
  const lastObservedContentRef = useRef("");
  const pendingSpeechTextRef = useRef("");
  const flushIssuedRef = useRef(false);
  const latestAssistantRef = useRef<ChatMessage | null>(null);
  const mediaSourceRef = useRef<MediaSource | null>(null);
  const sourceBufferRef = useRef<SourceBuffer | null>(null);
  const appendQueueRef = useRef<Uint8Array[]>([]);
  const pendingEndOfStreamRef = useRef(false);
  const playbackUrlRef = useRef<string | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const latestAssistantMessage = useMemo(
    () => [...messages]
      .reverse()
      .find((message) => message.role === "assistant" && Boolean(message.content?.trim()) && ["streaming", "done"].includes(message.status ?? "")) ?? null,
    [messages],
  );

  latestAssistantRef.current = latestAssistantMessage;

  const log = useCallback((line: string) => {
    appendConsoleLine?.(line);
  }, [appendConsoleLine]);

  const appendNextChunk = useCallback(() => {
    const sourceBuffer = sourceBufferRef.current;
    if (!sourceBuffer || sourceBuffer.updating) return;

    const next = appendQueueRef.current.shift();
    if (next) {
      const copy = new Uint8Array(next.byteLength);
      copy.set(next);
      sourceBuffer.appendBuffer(copy);
      return;
    }

    if (pendingEndOfStreamRef.current && mediaSourceRef.current?.readyState === "open") {
      try {
        mediaSourceRef.current.endOfStream();
      } catch {
        // ignore
      }
      pendingEndOfStreamRef.current = false;
    }
  }, []);

  const teardownPlayback = useCallback(() => {
    appendQueueRef.current = [];
    pendingEndOfStreamRef.current = false;

    const audio = audioRef.current;
    audioRef.current = null;
    if (audio) {
      try {
        audio.pause();
      } catch {
        // ignore
      }
    }

    const sourceBuffer = sourceBufferRef.current;
    sourceBufferRef.current = null;
    if (sourceBuffer) {
      try {
        sourceBuffer.removeEventListener("updateend", appendNextChunk);
      } catch {
        // ignore
      }
    }

    mediaSourceRef.current = null;

    const playbackUrl = playbackUrlRef.current;
    playbackUrlRef.current = null;
    if (playbackUrl) {
      try {
        URL.revokeObjectURL(playbackUrl);
      } catch {
        // ignore
      }
    }
  }, [appendNextChunk]);

  const teardownSession = useCallback((closeMessage = true) => {
    const socket = socketRef.current;
    socketRef.current = null;
    socketReadyRef.current = false;
    if (socket) {
      try {
        if (closeMessage && socket.readyState === WebSocket.OPEN) {
          socket.send(JSON.stringify({ type: "close" }));
        }
        socket.close();
      } catch {
        // ignore
      }
    }
    currentMessageIdRef.current = null;
    lastObservedContentRef.current = "";
    pendingSpeechTextRef.current = "";
    flushIssuedRef.current = false;
    teardownPlayback();
  }, [teardownPlayback]);

  const ensurePlayback = useCallback(async () => {
    if (typeof window === "undefined" || typeof MediaSource === "undefined") {
      throw new Error("This browser does not support streaming audio playback.");
    }
    if (!MediaSource.isTypeSupported("audio/mpeg")) {
      throw new Error("This browser cannot play streaming MP3 audio.");
    }
    if (audioRef.current) return;

    const mediaSource = new MediaSource();
    const playbackUrl = URL.createObjectURL(mediaSource);
    const audio = new Audio(playbackUrl);
    audio.autoplay = true;

    mediaSourceRef.current = mediaSource;
    playbackUrlRef.current = playbackUrl;
    audioRef.current = audio;

    mediaSource.addEventListener("sourceopen", () => {
      if (!mediaSourceRef.current || mediaSourceRef.current.readyState !== "open") return;
      if (sourceBufferRef.current) return;
      const sourceBuffer = mediaSourceRef.current.addSourceBuffer("audio/mpeg");
      sourceBuffer.mode = "sequence";
      sourceBuffer.addEventListener("updateend", appendNextChunk);
      sourceBufferRef.current = sourceBuffer;
      appendNextChunk();
    }, { once: true });

    try {
      await audio.play();
    } catch (playbackError) {
      log(`[voice] autoplay blocked: ${playbackError instanceof Error ? playbackError.message : String(playbackError)}`);
    }
  }, [appendNextChunk, log]);

  const handleAudioChunk = useCallback(async (audioBase64: string) => {
    await ensurePlayback();
    appendQueueRef.current.push(decodeBase64Audio(audioBase64));
    appendNextChunk();
    setStatus((previous) => previous === "connecting" ? "streaming" : "playing");
  }, [appendNextChunk, ensurePlayback]);

  const sendSpeechChunks = useCallback((forceFlush: boolean) => {
    const socket = socketRef.current;
    const message = latestAssistantRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN || !socketReadyRef.current || !message) {
      return;
    }

    const { chunks, pending } = extractAutoConversationChunks(pendingSpeechTextRef.current, forceFlush);
    pendingSpeechTextRef.current = pending;

    for (const chunk of chunks) {
      const text = ensureTrailingSpeechSpace(chunk);
      if (!text) continue;
      socket.send(JSON.stringify({
        type: "text",
        text,
        try_trigger_generation: true,
      }));
      setStatus((previous) => previous === "idle" ? "streaming" : previous);
    }

    if (forceFlush && !flushIssuedRef.current) {
      socket.send(JSON.stringify({ type: "flush" }));
      flushIssuedRef.current = true;
    }
  }, []);

  const pumpAssistantMessage = useCallback(() => {
    const message = latestAssistantRef.current;
    if (!message || currentMessageIdRef.current !== message.id) return;

    const content = message.content ?? "";
    if (content.length > lastObservedContentRef.current.length) {
      pendingSpeechTextRef.current += content.slice(lastObservedContentRef.current.length);
      lastObservedContentRef.current = content;
      sendSpeechChunks(false);
    }

    if ((message.status === "done" || message.status === "error") && !flushIssuedRef.current) {
      sendSpeechChunks(true);
    }
  }, [sendSpeechChunks]);

  const startVoiceSession = useCallback((message: ChatMessage) => {
    if (typeof window === "undefined" || typeof WebSocket === "undefined") {
      setStatus("error");
      setError("This browser does not support voice streaming.");
      return;
    }

    teardownSession(false);
    currentMessageIdRef.current = message.id;
    lastObservedContentRef.current = "";
    pendingSpeechTextRef.current = "";
    flushIssuedRef.current = false;
    setError(null);
    setStatus("connecting");

    const socket = new WebSocket(voiceStreamUrl());
    socketRef.current = socket;

    socket.addEventListener("open", () => {
      socket.send(JSON.stringify({
        type: "start",
        voice_id: defaultVoiceId,
        output_format: "mp3_44100_128",
        auto_mode: true,
        generation_config: { chunk_length_schedule: [80, 120, 160, 220] },
        voice_settings: {
          stability: 0.45,
          similarity_boost: 0.75,
          use_speaker_boost: true,
          speed: 1,
        },
      }));
    });

    socket.addEventListener("message", (event) => {
      let payload: VoiceSocketMessage;
      try {
        payload = JSON.parse(event.data as string) as VoiceSocketMessage;
      } catch {
        log("[voice] malformed websocket packet");
        return;
      }

      if (payload.type === "ready") {
        socketReadyRef.current = true;
        setStatus("streaming");
        pumpAssistantMessage();
        return;
      }

      if (payload.type === "audio" && typeof payload.audio === "string") {
        void handleAudioChunk(payload.audio).catch((playbackError) => {
          const nextError = playbackError instanceof Error ? playbackError.message : String(playbackError);
          setError(nextError);
          setStatus("error");
          log(`[voice] playback failed: ${nextError}`);
        });
        return;
      }

      if (payload.type === "final") {
        pendingEndOfStreamRef.current = true;
        appendNextChunk();
        return;
      }

      if (payload.type === "error") {
        const detail = typeof payload.detail === "string" ? payload.detail : "Voice streaming failed.";
        setError(detail);
        setStatus("error");
        log(`[voice] ${detail}`);
        return;
      }

      if (payload.type === "upstream_closed") {
        pendingEndOfStreamRef.current = true;
        appendNextChunk();
      }
    });

    socket.addEventListener("close", () => {
      socketReadyRef.current = false;
      if (enabled) {
        setStatus((previous) => previous === "error" ? previous : "idle");
      }
    });

    socket.addEventListener("error", () => {
      setStatus("error");
      setError("Voice websocket connection failed.");
    });
  }, [appendNextChunk, defaultVoiceId, enabled, handleAudioChunk, log, pumpAssistantMessage, teardownSession]);

  useEffect(() => {
    if (enabled && !previousEnabledRef.current) {
      const latest = latestAssistantRef.current;
      if (latest?.status === "done") {
        currentMessageIdRef.current = latest.id;
        lastObservedContentRef.current = latest.content ?? "";
        pendingSpeechTextRef.current = "";
        flushIssuedRef.current = true;
      }
    }
    previousEnabledRef.current = enabled;

    if (!enabled || !available) {
      if (!available && enabled) {
        setStatus("error");
        setError("ElevenLabs voice streaming is not configured.");
      } else {
        setStatus("idle");
        setError(null);
      }
      teardownSession();
      return;
    }

    if (!latestAssistantMessage) return;

    if (currentMessageIdRef.current !== latestAssistantMessage.id) {
      startVoiceSession(latestAssistantMessage);
      return;
    }

    pumpAssistantMessage();
  }, [available, enabled, latestAssistantMessage, pumpAssistantMessage, startVoiceSession, teardownSession]);

  useEffect(() => () => teardownSession(), [teardownSession]);

  return { status, error };
}
