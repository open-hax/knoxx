import { useCallback, useEffect, useRef, useState } from "react";

import { voiceTtsSynthesize } from "../../lib/api";
import type { ChatMessage } from "../../lib/types";

type AutoConversationVoiceStatus = "idle" | "loading" | "playing" | "error";

type UseAutoConversationVoiceParams = {
  enabled: boolean;
  available: boolean;
  messages: ChatMessage[];
  defaultVoiceId?: string;
  onPlaybackEnded?: () => void;
};

type UseAutoConversationVoiceResult = {
  status: AutoConversationVoiceStatus;
  error: string | null;
};

export function useAutoConversationVoice({
  enabled,
  available,
  messages,
  defaultVoiceId,
  onPlaybackEnded,
}: UseAutoConversationVoiceParams): UseAutoConversationVoiceResult {
  const [status, setStatus] = useState<AutoConversationVoiceStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const urlRef = useRef<string | null>(null);
  const spokenMessageIdsRef = useRef<Set<string>>(new Set());
  const wasEnabledRef = useRef(false);

  const cleanup = useCallback(() => {
    const audio = audioRef.current;
    audioRef.current = null;
    if (audio) {
      try {
        audio.pause();
      } catch {
        // ignore
      }
    }

    const url = urlRef.current;
    urlRef.current = null;
    if (url) {
      try {
        URL.revokeObjectURL(url);
      } catch {
        // ignore
      }
    }
  }, []);

  useEffect(() => cleanup, [cleanup]);

  useEffect(() => {
    if (!enabled || !available) {
      cleanup();
      if (!available && enabled) {
        setStatus("error");
        setError("TTS is not configured.");
      } else {
        setStatus("idle");
        setError(null);
      }
      return;
    }

    // Seed spoken IDs on first enable so we don't replay history.
    if (enabled && !wasEnabledRef.current) {
      for (const message of messages) {
        if (message.role === "assistant" && message.status === "done" && message.id) {
          spokenMessageIdsRef.current.add(message.id);
        }
      }
    }
    wasEnabledRef.current = enabled;

    const latestAssistant = [...messages]
      .reverse()
      .find(
        (message) =>
          message.role === "assistant" &&
          message.status === "done" &&
          Boolean(message.content?.trim()) &&
          message.id,
      );

    if (!latestAssistant) return;
    if (spokenMessageIdsRef.current.has(latestAssistant.id)) return;

    // New assistant reply to speak.
    spokenMessageIdsRef.current.add(latestAssistant.id);
    cleanup();
    setStatus("loading");
    setError(null);

    const text = latestAssistant.content!.trim();

    void voiceTtsSynthesize({ text, voice_id: defaultVoiceId || undefined })
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        urlRef.current = url;

        const audio = new Audio(url);
        audioRef.current = audio;

        audio.onended = () => {
          if (audioRef.current === audio) {
            cleanup();
            setStatus("idle");
            onPlaybackEnded?.();
          }
        };
        audio.onerror = () => {
          if (audioRef.current === audio) {
            cleanup();
            setStatus("error");
            setError("Audio playback failed.");
          }
        };

        return audio.play();
      })
      .then(() => {
        setStatus("playing");
      })
      .catch((err) => {
        cleanup();
        setStatus("error");
        setError(err instanceof Error ? err.message : String(err));
      });
  }, [available, enabled, messages, defaultVoiceId, cleanup]);

  return { status, error };
}
