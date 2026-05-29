import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import BroadcastStudioPage from "./BroadcastStudioPage";

const chatController = vi.hoisted(() => ({
  activeAgentId: "",
  selectedModel: "",
  conversationId: "conversation-1",
  isSending: false,
  messages: [],
  unpinContextItem: vi.fn(),
  pinContextItem: vi.fn(),
  setSelectedModel: vi.fn(),
  setActiveActorId: vi.fn(),
  setActiveAgentId: vi.fn(),
  handleNewChat: vi.fn(),
  handleSend: vi.fn(),
}));

vi.mock("@open-hax/uxx", () => ({
  Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  Button: ({ children, ...props }: { children: ReactNode } & ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{children}</button>
  ),
  tokens: { fontSize: { xs: "12px", sm: "14px", lg: "18px" } },
}));

vi.mock("../components/studio/MusicPlayerView", () => ({
  AudioSpectrumVisualizer: () => <div data-testid="waveform-visualizer" />,
  PlaybackProgress: () => <div data-testid="playback-progress" />,
  MusicPlayerView: ({
    track,
    currentLabels,
    graphLabelControls,
    agentActions,
  }: {
    track: { name: string } | null;
    currentLabels?: ReactNode;
    graphLabelControls?: ReactNode;
    agentActions?: ReactNode;
  }) => (
    <section data-testid="music-player">
      <div>{track ? `Now playing: ${track.name}` : "No track selected"}</div>
      {currentLabels}
      {graphLabelControls}
      {agentActions}
    </section>
  ),
}));

vi.mock("../components/chat-page/ChatWorkspacePane", () => ({
  ChatWorkspacePane: () => <aside data-testid="studio-chat-pane" />,
}));

vi.mock("../components/chat-page/useChatWorkspaceController", () => ({
  useChatWorkspaceController: () => chatController,
}));

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

const audioFiles = [
  { name: "intro.wav", path: "intro.wav", size: 2048, ext: ".wav", modified: 1, mime: "audio/wav" },
  { name: "bumper.mp3", path: "bumper.mp3", size: 4096, ext: ".mp3", modified: 2, mime: "audio/mpeg" },
];

const graphLabels = [
  { label_id: "label-news", label: "News", emoji: "📰", description: "News beds", color: "#2563eb" },
  { label_id: "label-sfx", label: "SFX", emoji: "✨", description: "Sound effects", color: "#7c3aed" },
];

function installAudioDomMocks() {
  vi.spyOn(window.HTMLMediaElement.prototype, "load").mockImplementation(() => undefined);
  vi.spyOn(window.HTMLMediaElement.prototype, "pause").mockImplementation(() => undefined);
  vi.spyOn(window.HTMLMediaElement.prototype, "play").mockImplementation(() => Promise.resolve());

  class FakeAudio {
    duration = 42;
    preload = "";
    crossOrigin = "";
    private audioSrc = "";
    private listeners = new Map<string, Set<EventListenerOrEventListenerObject>>();

    set src(value: string) {
      this.audioSrc = value;
      window.setTimeout(() => this.dispatch("loadedmetadata"), 0);
    }

    get src() {
      return this.audioSrc;
    }

    addEventListener(type: string, listener: EventListenerOrEventListenerObject) {
      const listeners = this.listeners.get(type) ?? new Set<EventListenerOrEventListenerObject>();
      listeners.add(listener);
      this.listeners.set(type, listeners);
    }

    removeEventListener(type: string, listener: EventListenerOrEventListenerObject) {
      this.listeners.get(type)?.delete(listener);
    }

    pause() {}

    load() {}

    removeAttribute(name: string) {
      if (name === "src") this.audioSrc = "";
    }

    private dispatch(type: string) {
      const event = new Event(type);
      this.listeners.get(type)?.forEach((listener) => {
        if (typeof listener === "function") {
          listener.call(this, event);
        } else {
          listener.handleEvent(event);
        }
      });
    }
  }

  vi.stubGlobal("Audio", FakeAudio);
  vi.stubGlobal("AudioContext", class FakeAudioContext {
    destination = {};
    createAnalyser() {
      return {
        fftSize: 0,
        frequencyBinCount: 32,
        connect: vi.fn(),
        getByteTimeDomainData: vi.fn(),
        getByteFrequencyData: vi.fn(),
      };
    }
    createMediaElementSource() {
      return { connect: vi.fn() };
    }
    close() {
      return Promise.resolve();
    }
  });
}

function installBroadcastStudioFetchMock() {
  const requests: Array<{ url: string; init?: RequestInit }> = [];
  const appliedLabelIds = new Set<string>();

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    requests.push({ url, init });

    if (url.startsWith("/api/contracts/ui-actions")) {
      return jsonResponse({ actions: [] });
    }
    if (url.startsWith("/api/studio/audio-library")) {
      return jsonResponse({ ok: true, root: ".", count: audioFiles.length, files: audioFiles });
    }
    if (url === "/api/studio/state?kind=player") {
      return jsonResponse({ ok: true, state: {} });
    }
    if (url === "/api/studio/state" && method === "PUT") {
      return jsonResponse({ ok: true });
    }
    if (url === "/api/studio/playlist" && method === "GET") {
      return jsonResponse({ ok: true, playlist: [] });
    }
    if (url === "/api/studio/playlist" && method === "PUT") {
      return jsonResponse({ ok: true });
    }
    if (url === "/api/studio/playlists") {
      return jsonResponse({ ok: true, playlists: [{ name: "Late Night", path: "playlists/late.m3u", filename: "late.m3u" }] });
    }
    if (url === `/api/studio/load-m3u?path=${encodeURIComponent("playlists/late.m3u")}`) {
      return jsonResponse({
        ok: true,
        name: "Late Night",
        items: [
          { path: "intro.wav", name: "intro.wav" },
          { path: "bumper.mp3", name: "bumper.mp3" },
        ],
      });
    }
    if (url === "/api/studio/save-m3u" && method === "POST") {
      const body = JSON.parse(String(init?.body));
      return jsonResponse({ ok: true, path: `playlists/${body.name}.m3u`, count: body.items?.length ?? 0 });
    }
    if (url === "/api/openplanner/v1/graph/labels?search=&limit=200") {
      return jsonResponse({ ok: true, labels: graphLabels });
    }
    if (url === "/api/openplanner/v1/graph/edges/query" && method === "POST") {
      const body = JSON.parse(String(init?.body)) as { nodeIds?: string[] };
      const source = body.nodeIds?.[0] ?? "workspace:file:intro.wav";
      return jsonResponse({
        edges: Array.from(appliedLabelIds).map((labelId) => ({ source, target: labelId, edgeKind: "has_label" })),
      });
    }
    if (url.startsWith("/api/openplanner/v1/graph/labels/") && method === "POST") {
      const match = url.match(/\/api\/openplanner\/v1\/graph\/labels\/([^/]+)\/(apply|remove)$/);
      if (match?.[1] && match[2] === "apply") appliedLabelIds.add(decodeURIComponent(match[1]));
      if (match?.[1] && match[2] === "remove") appliedLabelIds.delete(decodeURIComponent(match[1]));
      return jsonResponse({ ok: true });
    }
    if (url.startsWith("/api/ingestion/audio-context")) {
      const parsed = new URL(url, "http://localhost");
      const path = parsed.searchParams.get("path") ?? "intro.wav";
      return jsonResponse({
        ok: true,
        path,
        source_id: "workspace-audio",
        song_title: path.replace(/\.[^.]+$/, ""),
        description: "Bright news bed. Labels: news, stinger",
        content: "Bright news bed.",
      });
    }
    if (url === "/api/openplanner/v1/gardens") {
      return jsonResponse({ ok: true, gardens: [] });
    }

    return jsonResponse({ error: `Unexpected ${method} ${url}` }, { status: 404 });
  });

  vi.stubGlobal("fetch", fetchMock);
  return { requests, appliedLabelIds };
}

function renderBroadcastStudioPage() {
  return render(
    <MemoryRouter>
      <BroadcastStudioPage />
    </MemoryRouter>,
  );
}

describe("BroadcastStudioPage audio library, playlists, and labels", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
    localStorage.clear();
    installAudioDomMocks();
  });

  it("loads audio library entries and plays the selected file through the stream URL", async () => {
    installBroadcastStudioFetchMock();
    const { container } = renderBroadcastStudioPage();

    fireEvent.click(await screen.findByText("intro.wav"));

    await screen.findByText("Now playing: intro.wav");
    const audio = container.querySelector("audio");
    expect(audio?.src).toContain("/api/studio/stream?path=intro.wav");
  });

  it("loads a saved playlist into the queue and saves the queue order back to M3U", async () => {
    const { requests } = installBroadcastStudioFetchMock();
    renderBroadcastStudioPage();

    fireEvent.click(await screen.findByText("Late Night"));
    expect(await screen.findByText("Preview: Late Night")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Load" }));

    await waitFor(() => expect(screen.getByPlaceholderText("Playlist name...")).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText("Playlist name..."), { target: { value: "Roundtrip" } });
    fireEvent.click(screen.getByRole("button", { name: /Save/ }));

    await waitFor(() => {
      const saveRequest = requests.find((request) => request.url === "/api/studio/save-m3u" && request.init?.method === "POST");
      expect(saveRequest).toBeTruthy();
      expect(JSON.parse(String(saveRequest?.init?.body))).toMatchObject({
        name: "Roundtrip",
        items: [
          { path: "intro.wav", name: "intro.wav" },
          { path: "bumper.mp3", name: "bumper.mp3" },
        ],
      });
    });
  });

  it("applies and removes graph labels for the selected audio file", async () => {
    const { requests } = installBroadcastStudioFetchMock();
    renderBroadcastStudioPage();

    fireEvent.click((await screen.findAllByTitle("Labels"))[0]!);
    expect(await screen.findByText("Graph labels for: intro.wav")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /News/ }));
    await waitFor(() => expect(requests.some((request) => (
      request.url === "/api/openplanner/v1/graph/labels/label-news/apply"
      && request.init?.method === "POST"
      && JSON.parse(String(request.init.body)).node_id === "workspace:file:intro.wav"
    ))).toBe(true));
    fireEvent.click(await screen.findByRole("button", { name: /News ×/ }));

    await waitFor(() => expect(requests.some((request) => (
      request.url === "/api/openplanner/v1/graph/labels/label-news/remove"
      && request.init?.method === "POST"
      && JSON.parse(String(request.init.body)).node_id === "workspace:file:intro.wav"
    ))).toBe(true));
    expect(await screen.findByText("No graph labels applied yet")).toBeInTheDocument();
  });
});
