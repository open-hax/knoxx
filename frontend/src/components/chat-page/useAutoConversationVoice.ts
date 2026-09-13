import { useVoiceRecorder } from "./useVoiceRecorder";
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

type ConversationVoiceControlsOptions = {
  messages: ChatMessage[];
  ttsEnabled: boolean;
  sttEnabled: boolean;
  ttsDefaultVoiceId: string;
  isSending: boolean;
  onSend: (text: string) => void;
};

/** Coordinate the existing automatic reply playback and silence-triggered recording. */
export function useConversationVoiceControls({
  messages, ttsEnabled, sttEnabled, ttsDefaultVoiceId, isSending, onSend,
}: ConversationVoiceControlsOptions) {
  const [autoConversationEnabled, setAutoConversationEnabled] = useState(false);
  const [autoRecording, setAutoRecording] = useState(false);
  const [voiceThreshold, setVoiceThreshold] = useState(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('knoxx_voice_threshold');
      if (saved) {
        const parsed = parseFloat(saved);
        if (!isNaN(parsed)) return Math.max(0.001, Math.min(0.1, parsed));
      }
    }
    return 0.015;
  });

  useEffect(() => {
    localStorage.setItem('knoxx_voice_threshold', String(voiceThreshold));
  }, [voiceThreshold]);

  const autoConversationVoice = useAutoConversationVoice({
    enabled: autoConversationEnabled,
    available: ttsEnabled,
    messages,
    defaultVoiceId: ttsDefaultVoiceId,
    onPlaybackEnded: () => {
      if (sttEnabled) {
        setAutoRecording(true);
      }
    },
  });

  useEffect(() => {
    if (!ttsEnabled && autoConversationEnabled) {
      setAutoConversationEnabled(false);
    }
  }, [autoConversationEnabled, ttsEnabled]);

  useEffect(() => {
    if (!autoConversationEnabled && autoRecording) {
      setAutoRecording(false);
    }
  }, [autoConversationEnabled, autoRecording]);

  const prevAutoConversationEnabledRef = useRef(false);

  const { state: autoRecorderState, startRecording: startAutoRecording, stopRecording: stopAutoRecording, audioLevelRef } = useVoiceRecorder({
    onTranscript: (text) => {
      setAutoRecording(false);
      onSend(text);
    },
    conversationMode: true,
    silenceThreshold: voiceThreshold,
  });

  // Start recording immediately when user toggles auto-conversation ON
  useEffect(() => {
    if (!prevAutoConversationEnabledRef.current && autoConversationEnabled && sttEnabled && !isSending) {
      setAutoRecording(true);
    }
    prevAutoConversationEnabledRef.current = autoConversationEnabled;
  }, [autoConversationEnabled, sttEnabled, isSending]);

  // Stop recording when assistant starts generating
  useEffect(() => {
    if (isSending && autoRecording) {
      setAutoRecording(false);
    }
  }, [isSending, autoRecording]);

  // Start/stop the actual recorder based on autoRecording state
  useEffect(() => {
    if (autoRecording && autoRecorderState.status === "idle") {
      void startAutoRecording();
    }
  }, [autoRecording, autoRecorderState.status, startAutoRecording]);

  useEffect(() => {
    if (!autoRecording && autoRecorderState.status === "recording") {
      stopAutoRecording();
    }
  }, [autoRecording, autoRecorderState.status, stopAutoRecording]);

  return {
    autoConversationEnabled, setAutoConversationEnabled, autoRecording,
    voiceThreshold, setVoiceThreshold, autoConversationVoice,
    autoRecorderState, audioLevelRef,
  };
}
