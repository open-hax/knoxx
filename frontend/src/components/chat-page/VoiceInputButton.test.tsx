import type { FormEvent } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { VoiceInputButton } from "./VoiceInputButton";

class MockMediaRecorder {
  static isTypeSupported(): boolean {
    return true;
  }

  public mimeType = "audio/webm";

  public state = "recording";

  public ondataavailable: ((event: { data: Blob; size?: number }) => void) | null = null;

  public onstop: (() => void) | null = null;

  public onerror: ((event: { error?: Error }) => void) | null = null;

  constructor(_stream: MediaStream, _options?: MediaRecorderOptions) {}

  start(): void {
    this.state = "recording";
  }

  stop(): void {
    this.state = "inactive";
    this.onstop?.();
  }
}

class MockAudioContext {
  public sampleRate = 44_100;

  public destination = {};

  createMediaStreamSource(): MediaStreamAudioSourceNode {
    return { connect: vi.fn() } as unknown as MediaStreamAudioSourceNode;
  }

  createScriptProcessor(): ScriptProcessorNode {
    return {
      connect: vi.fn(),
      disconnect: vi.fn(),
      onaudioprocess: null,
    } as unknown as ScriptProcessorNode;
  }

  createAnalyser(): AnalyserNode {
    return {
      connect: vi.fn(),
      fftSize: 512,
      smoothingTimeConstant: 0.3,
      getByteTimeDomainData: vi.fn(),
    } as unknown as AnalyserNode;
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}

describe("VoiceInputButton", () => {
  const originalMediaDevices = navigator.mediaDevices;

  beforeEach(() => {
    vi.stubGlobal("MediaRecorder", MockMediaRecorder);
    vi.stubGlobal("AudioContext", MockAudioContext);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: originalMediaDevices,
    });
  });

  it("starts microphone capture without submitting the surrounding form", async () => {
    const submitSpy = vi.fn();
    const preventSubmit = (event: FormEvent<HTMLFormElement>) => {
      submitSpy();
      event.preventDefault();
    };
    const getUserMedia = vi.fn().mockResolvedValue({
      getTracks: () => [{ stop: vi.fn() }],
    } as unknown as MediaStream);

    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: { getUserMedia },
    });

    render(
      <form onSubmit={preventSubmit}>
        <VoiceInputButton onTranscript={vi.fn()} idleLabel="Speak" />
      </form>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Speak" }));

    await waitFor(() => {
      expect(getUserMedia).toHaveBeenCalledTimes(1);
    });
    expect(submitSpy).not.toHaveBeenCalled();
    expect(screen.getByText("recording")).toBeInTheDocument();
  });
});
