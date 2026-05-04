import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Badge, tokens } from "@open-hax/uxx";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import {
  getAudioLibrary,
  getAudioStreamUrl,
  savePlaylistAsM3U,
  loadM3UPlaylist,
  listPlaylists,
  getAudioLabels,
  addAudioLabel,
  removeAudioLabel,
  getAllLabels,
  syncAudioSymlinks,
  saveAudioAsset,
  scanDiscordAudio,
  scanDiscordImages,
  type AudioFileEntry,
  type AudioLibraryResponse,
  type DiscordAudioScanResponse,
  type DiscordImageScanResponse,
} from "../lib/api/runtime";
import { request } from "../lib/api/core";

// ── Persistence keys ─────────────────────────────────────────────────

const STUDIO_SESSION_KEY = "knoxx_studio_session_id";
const STUDIO_SCRATCHPAD_KEY = "knoxx_studio_scratchpad";
const STUDIO_PINNED_KEY = "knoxx_studio_pinned_context";
const STUDIO_SESSION_STATE_KEY = "knoxx_studio_session_state";
const STUDIO_SIDEBAR_WIDTH_KEY = "knoxx_studio_sidebar_width";

// ── Types ────────────────────────────────────────────────────────────

interface PlaylistItem {
  id: string;
  path: string;
  name: string;
  addedAt: number;
  duration?: number;
  labels?: string[];
}

type PlayerState = "idle" | "loading" | "playing" | "paused";

interface StudioPlayerState {
  currentPath: string | null;
  currentTime: number;
  volume: number;
  shuffleOn: boolean;
  repeatOn: boolean;
}

interface AudioChatContextResponse {
  ok: boolean;
  path: string;
  source_id: string;
  song_title: string;
  description: string;
  content: string;
}

// ── API helpers ──────────────────────────────────────────────────────

async function loadStudioState(kind: string): Promise<Record<string, unknown>> {
  try {
    const res = await request<{ ok: boolean; state: Record<string, unknown> }>(
      `/api/studio/state?kind=${encodeURIComponent(kind)}`
    );
    return res.state ?? {};
  } catch {
    return {};
  }
}

async function saveStudioState(kind: string, state: Record<string, unknown>): Promise<void> {
  try {
    await request("/api/studio/state", {
      method: "PUT",
      body: JSON.stringify({ kind, state }),
    });
  } catch {
    // Silent fail for state persistence
  }
}

async function loadPlaylist(): Promise<PlaylistItem[]> {
  try {
    const res = await request<{ ok: boolean; playlist: PlaylistItem[] }>(
      "/api/studio/playlist"
    );
    return res.playlist ?? [];
  } catch {
    return [];
  }
}

async function savePlaylist(items: PlaylistItem[]): Promise<void> {
  try {
    await request("/api/studio/playlist", {
      method: "PUT",
      body: JSON.stringify({ items }),
    });
  } catch {
    // Silent fail
  }
}

// ── Helpers ──────────────────────────────────────────────────────────

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDuration(seconds: number): string {
  if (!isFinite(seconds)) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function getAudioDuration(path: string): Promise<number> {
  return new Promise((resolve) => {
    const audio = new Audio();
    audio.addEventListener("loadedmetadata", () => resolve(audio.duration));
    audio.addEventListener("error", () => resolve(0));
    audio.src = getAudioStreamUrl(path);
  });
}

function fileIcon(ext: string): string {
  switch (ext) {
    case ".mp3": return "🎵";
    case ".wav": return "🔊";
    case ".ogg": return "🎶";
    case ".flac": return "🎼";
    case ".m4a": return "🎤";
    default: return "🎧";
  }
}

// ── Waveform Visualizer (audioMotion) ───────────────────────────────

import AudioMotionAnalyzer from "audiomotion-analyzer";

function WaveformVisualizer({ analyserRef, isPlaying }: { analyserRef: React.RefObject<AnalyserNode | null>; isPlaying: boolean }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const audioMotionRef = useRef<AudioMotionAnalyzer | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    const analyser = analyserRef.current;
    if (!container || !analyser || !isPlaying) return;

    // Create audioMotion instance
    const audioMotion = new AudioMotionAnalyzer(container, {
      source: analyser,
      width: container.clientWidth,
      height: 50,
      mode: 2, // Oscilloscope mode
      gradient: 'classic',
      showScaleX: false,
      showScaleY: false,
      smoothing: 0.7,
    });
    audioMotionRef.current = audioMotion;

    return () => {
      audioMotion.destroy();
      audioMotionRef.current = null;
    };
  }, [analyserRef, isPlaying]);

  return (
    <div
      ref={containerRef}
      style={{ width: "100%", height: 50, borderRadius: 8, overflow: "hidden", background: "rgba(0,0,0,0.2)" }}
    />
  );
}

// ── Main Component ───────────────────────────────────────────────────

export default function BroadcastStudioPage() {
  // Chat workspace controller (right pane)
  const chat = useChatWorkspaceController({
    initialShowCanvas: false,
    defaultActorId: "broadcast_studio",
    sessionIdKey: STUDIO_SESSION_KEY,
    scratchpadStorageKey: STUDIO_SCRATCHPAD_KEY,
    pinnedContextStorageKey: STUDIO_PINNED_KEY,
    sessionStateKey: STUDIO_SESSION_STATE_KEY,
    sidebarWidthKey: STUDIO_SIDEBAR_WIDTH_KEY,
  });

  // Ref to hold latest chat object — prevents unstable reference from
  // triggering syncAudioChatContext effect on every render.
  const chatRef = useRef(chat);
  chatRef.current = chat;

  // Audio library state
  const [library, setLibrary] = useState<AudioLibraryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [scanPath, setScanPath] = useState(".");
  const [filterText, setFilterText] = useState("");

  // Player state
  const [currentFile, setCurrentFile] = useState<AudioFileEntry | null>(null);
  const [playerState, setPlayerState] = useState<PlayerState>("idle");
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [playingFrom, setPlayingFrom] = useState<"library" | "playlist">("library");
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);

  // Playlist
  const [playlist, setPlaylist] = useState<PlaylistItem[]>([]);
  const playlistIndexRef = useRef<number>(-1);
  const pinnedAudioContextPathsRef = useRef<string[]>([]);

  // Sidebar state
  const [showSidebar, setShowSidebar] = useState(true);
  const [sidebarWidth, setSidebarWidth] = useState(() => {
    const stored = localStorage.getItem(STUDIO_SIDEBAR_WIDTH_KEY);
    return stored ? parseInt(stored, 10) : 280;
  });

  // Panel sizes
  const [bottomPanelHeight, setBottomPanelHeight] = useState(() => {
    const stored = localStorage.getItem("knoxx_studio_bottom_height");
    return stored ? parseInt(stored, 10) : 300;
  });
  const [chatPanelWidth, setChatPanelWidth] = useState(() => {
    const stored = localStorage.getItem("knoxx_studio_chat_width");
    return stored ? parseInt(stored, 10) : 460;
  });

  // State restore on mount
  const [stateRestored, setStateRestored] = useState(false);
  const [discordScanStatus, setDiscordScanStatus] = useState<DiscordAudioScanResponse | null>(null);
  const [discordImageScanStatus, setDiscordImageScanStatus] = useState<DiscordImageScanResponse | null>(null);
  const [scanningDiscordAudio, setScanningDiscordAudio] = useState(false);
  const [scanningDiscordImages, setScanningDiscordImages] = useState(false);

  // ── Load audio library ───────────────────────────────────────────
  const refreshLibrary = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getAudioLibrary({ path: scanPath, depth: 3 });
      setLibrary(data);
    } catch {
      // Silent fail
    } finally {
      setLoading(false);
    }
  }, [scanPath]);

  useEffect(() => {
    void refreshLibrary();
  }, [refreshLibrary]);

  const handleDiscordAudioScan = useCallback(async () => {
    try {
      setScanningDiscordAudio(true);
      const result = await scanDiscordAudio({
        since_hours: 24 * 14,
        pages_per_channel: 2,
        limit_per_page: 100,
        max_channels: 50,
        import_root: "discord/audio",
      });
      setDiscordScanStatus(result);
      await refreshLibrary();
    } catch (error) {
      console.error("Failed to scan Discord audio:", error);
    } finally {
      setScanningDiscordAudio(false);
    }
  }, [refreshLibrary]);

  const handleDiscordImageScan = useCallback(async () => {
    try {
      setScanningDiscordImages(true);
      const result = await scanDiscordImages({
        since_hours: 24 * 14,
        pages_per_channel: 2,
        limit_per_page: 100,
        max_channels: 50,
        import_root: "discord/images",
      });
      setDiscordImageScanStatus(result);
    } catch (error) {
      console.error("Failed to scan Discord images:", error);
    } finally {
      setScanningDiscordImages(false);
    }
  }, []);

  const syncAudioChatContext = useCallback(async (file: AudioFileEntry | null) => {
    const c = chatRef.current;
    const previousPaths = pinnedAudioContextPathsRef.current;
    if (previousPaths.length > 0) {
      previousPaths.forEach((path) => c.unpinContextItem(path));
      pinnedAudioContextPathsRef.current = [];
    }

    if (!file) return;

    try {
      const context = await request<AudioChatContextResponse>(
        `/api/ingestion/audio-context?source_id=${encodeURIComponent("workspace-audio")}&path=${encodeURIComponent(file.path)}`
      );

      const titlePath = `${file.path}#song-title`;
      const descriptionPath = `${file.path}#song-description`;

      c.pinContextItem({
        id: titlePath,
        title: `Song: ${context.song_title}`,
        path: titlePath,
        snippet: context.song_title,
        kind: "message",
      });

      c.pinContextItem({
        id: descriptionPath,
        title: `Description: ${file.name}`,
        path: descriptionPath,
        snippet: context.description,
        kind: "message",
      });

      pinnedAudioContextPathsRef.current = [titlePath, descriptionPath];
    } catch (error) {
      console.error("Failed to pin audio chat context:", error);
    }
  }, []);

  useEffect(() => {
    void syncAudioChatContext(currentFile);
  }, [currentFile, syncAudioChatContext]);

  // ── Restore persisted state ──────────────────────────────────────
  useEffect(() => {
    const restore = async () => {
      try {
        const [playerSaved, playlistSaved] = await Promise.all([
          loadStudioState("player"),
          loadPlaylist(),
        ]);

        if (playerSaved?.volume != null) setVolume(playerSaved.volume as number);

        if (playlistSaved.length > 0) {
          setPlaylist(playlistSaved);
        }

        // Restore current track if it exists in library
        if (playerSaved?.currentPath && library?.files) {
          const found = library.files.find(f => f.path === playerSaved.currentPath);
          if (found) {
            setCurrentFile(found);
            if (playerSaved.currentTime) setCurrentTime(playerSaved.currentTime as number);
          }
        }
      } catch {
        // Silent fail on restore
      }
      setStateRestored(true);
    };

    if (library && !stateRestored) {
      void restore();
    }
  }, [library, stateRestored]);

  // ── Persist player state (debounced) ─────────────────────────────
  useEffect(() => {
    if (!stateRestored) return;
    const timeout = setTimeout(() => {
      void saveStudioState("player", {
        currentPath: currentFile?.path ?? null,
        currentTime,
        volume,
      });
    }, 1000);
    return () => clearTimeout(timeout);
  }, [currentFile, currentTime, volume, stateRestored]);

  // ── Persist playlist (debounced) ─────────────────────────────────
  useEffect(() => {
    if (!stateRestored) return;
    const timeout = setTimeout(() => {
      void savePlaylist(playlist);
    }, 500);
    return () => clearTimeout(timeout);
  }, [playlist, stateRestored]);

  // ── Audio context setup ──────────────────────────────────────────
  const sourceRef = useRef<MediaElementAudioSourceNode | null>(null);

  const setupAudioContext = useCallback(() => {
    if (audioContextRef.current) return;
    const audio = audioRef.current;
    if (!audio) return;

    const ctx = new AudioContext();
    const analyser = ctx.createAnalyser();
    analyser.fftSize = 128;

    const source = ctx.createMediaElementSource(audio);
    source.connect(analyser);
    analyser.connect(ctx.destination);

    audioContextRef.current = ctx;
    analyserRef.current = analyser;
    sourceRef.current = source;
  }, []);

  // Cleanup AudioContext on unmount
  useEffect(() => {
    return () => {
      if (audioContextRef.current) {
        void audioContextRef.current.close();
        audioContextRef.current = null;
        analyserRef.current = null;
        sourceRef.current = null;
      }
    };
  }, []);

  // ── Playback controls ────────────────────────────────────────────
  const playFile = useCallback((file: AudioFileEntry, source: "library" | "playlist" = "library") => {
    const audio = audioRef.current;
    if (!audio) return;

    setupAudioContext();
    audio.src = getAudioStreamUrl(file.path);
    audio.load();

    setCurrentFile(file);
    setPlayerState("loading");
    setPlayingFrom(source);

    audio.play()
      .then(() => setPlayerState("playing"))
      .catch(() => setPlayerState("idle"));
  }, [setupAudioContext]);

  const togglePlayPause = useCallback(() => {
    const audio = audioRef.current;
    if (!audio || !currentFile) return;

    if (playerState === "playing") {
      audio.pause();
      setPlayerState("paused");
    } else {
      audio.play().then(() => setPlayerState("playing")).catch(() => {});
    }
  }, [currentFile, playerState]);

  // Play a playlist item directly (doesn't require library lookup)
  const playPlaylistItem = useCallback((index: number) => {
    if (index < 0 || index >= playlist.length) return;
    playlistIndexRef.current = index;
    const item = playlist[index];
    const audio = audioRef.current;
    if (!audio) return;

    setupAudioContext();
    audio.src = getAudioStreamUrl(item.path);
    audio.load();

    // Create a minimal file entry for display
    const fileEntry: AudioFileEntry = {
      name: item.name,
      path: item.path,
      size: 0,
      ext: item.path.split('.').pop() ?? '',
      modified: 0,
      mime: "audio/mpeg",
    };
    setCurrentFile(fileEntry);
    setPlayerState("loading");
    setPlayingFrom("playlist");

    audio.play()
      .then(() => setPlayerState("playing"))
      .catch(() => setPlayerState("idle"));
  }, [playlist, setupAudioContext]);

  const playNext = useCallback(() => {
    if (playlist.length === 0) {
      setPlayerState("idle");
      setCurrentFile(null);
      setPlayingFrom("library");
      return;
    }

    const nextIndex = (playlistIndexRef.current + 1) % playlist.length;
    playPlaylistItem(nextIndex);
  }, [playlist, playPlaylistItem]);

  const playPrevious = useCallback(() => {
    if (playlist.length === 0) return;

    const prevIndex = playlistIndexRef.current <= 0
      ? playlist.length - 1
      : playlistIndexRef.current - 1;
    playPlaylistItem(prevIndex);
  }, [playlist, playPlaylistItem]);

  // ── Audio element events ─────────────────────────────────────────
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    let lastTimeUpdate = 0;
    const onTimeUpdate = () => {
      const now = Date.now();
      if (now - lastTimeUpdate < 250) return; // Throttle to 4 updates/sec
      lastTimeUpdate = now;
      setCurrentTime(audio.currentTime);
    };
    const onLoadedMetadata = () => setDuration(audio.duration);
    const onEnded = () => playNext();
    const onVolumeChange = () => setVolume(audio.volume);

    audio.addEventListener("timeupdate", onTimeUpdate);
    audio.addEventListener("loadedmetadata", onLoadedMetadata);
    audio.addEventListener("ended", onEnded);
    audio.addEventListener("volumechange", onVolumeChange);

    return () => {
      audio.removeEventListener("timeupdate", onTimeUpdate);
      audio.removeEventListener("loadedmetadata", onLoadedMetadata);
      audio.removeEventListener("ended", onEnded);
      audio.removeEventListener("volumechange", onVolumeChange);
    };
  }, [playNext]);

  // ── Volume control ───────────────────────────────────────────────
  useEffect(() => {
    if (audioRef.current) audioRef.current.volume = volume;
  }, [volume]);

  // ── Playlist management ──────────────────────────────────────────
  const addToPlaylist = useCallback(async (file: AudioFileEntry) => {
    const [duration, labelsRes] = await Promise.all([
      getAudioDuration(file.path),
      getAudioLabels(file.path).catch(() => ({ labels: [] })),
    ]);
    setPlaylist(prev => [
      ...prev,
      { id: `${file.path}-${Date.now()}`, path: file.path, name: file.name, addedAt: Date.now(), duration, labels: labelsRes.labels },
    ]);
  }, []);

  const removeFromPlaylist = useCallback((id: string) => {
    setPlaylist(prev => prev.filter(item => item.id !== id));
  }, []);

  const clearPlaylist = useCallback(() => {
    setPlaylist([]);
    playlistIndexRef.current = -1;
  }, []);

  const shufflePlaylist = useCallback(() => {
    setPlaylist(prev => {
      const next = [...prev];
      // Fisher-Yates shuffle
      for (let i = next.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [next[i], next[j]] = [next[j], next[i]];
      }
      return next;
    });
    playlistIndexRef.current = -1;
  }, []);

  const moveInPlaylist = useCallback((fromIndex: number, toIndex: number) => {
    setPlaylist(prev => {
      const next = [...prev];
      const [item] = next.splice(fromIndex, 1);
      next.splice(toIndex, 0, item);
      return next;
    });
  }, []);

  const [savingPlaylist, setSavingPlaylist] = useState(false);
  const [playlistName, setPlaylistName] = useState("");

  // ── Label state ──────────────────────────────────────────────────
  const [centerTab, setCenterTab] = useState<"queue" | "labels">("queue");
  const [selectedFileLabels, setSelectedFileLabels] = useState<string[]>([]);
  const [allLabels, setAllLabels] = useState<string[]>([]);
  const [newLabel, setNewLabel] = useState("");
  const [labelFilter, setLabelFilter] = useState<string | null>(null);
  const [labelingFile, setLabelingFile] = useState<AudioFileEntry | null>(null);
  const [savedPlaylists, setSavedPlaylists] = useState<Array<{ name: string; path: string; filename: string }>>([]);
  const [previewPlaylist, setPreviewPlaylist] = useState<{ name: string; items: Array<{ path: string; name: string }> } | null>(null);

  // Load all labels and playlists on mount
  useEffect(() => {
    void getAllLabels().then(res => setAllLabels(res.labels)).catch(() => {});
    void listPlaylists().then(res => setSavedPlaylists(res.playlists)).catch(() => {});
  }, []);

  // Load labels when selecting a file for labeling
  const openLabelEditor = useCallback(async (file: AudioFileEntry) => {
    setLabelingFile(file);
    setCenterTab("labels");
    try {
      const res = await getAudioLabels(file.path);
      setSelectedFileLabels(res.labels);
    } catch {
      setSelectedFileLabels([]);
    }
  }, []);

  const handleAddLabel = useCallback(async () => {
    if (!labelingFile || !newLabel.trim()) return;
    try {
      const res = await addAudioLabel(labelingFile.path, newLabel.trim());
      setSelectedFileLabels(res.labels);
      setNewLabel("");
      // Refresh all labels
      const allRes = await getAllLabels();
      setAllLabels(allRes.labels);
    } catch (err) {
      console.error("Failed to add label:", err);
    }
  }, [labelingFile, newLabel]);

  const handleRemoveLabel = useCallback(async (label: string) => {
    if (!labelingFile) return;
    try {
      const res = await removeAudioLabel(labelingFile.path, label);
      setSelectedFileLabels(res.labels);
      // Refresh all labels
      const allRes = await getAllLabels();
      setAllLabels(allRes.labels);
    } catch (err) {
      console.error("Failed to remove label:", err);
    }
  }, [labelingFile]);

  const handleSyncSymlinks = useCallback(async () => {
    try {
      const res = await syncAudioSymlinks();
      alert(`Synced ${res.symlinks} symlinks to ./audio/`);
    } catch (err) {
      console.error("Failed to sync symlinks:", err);
    }
  }, []);

  const generateVisualization = useCallback(async (audioPath: string, type: "waveform" | "spectrogram") => {
    try {
      // Create offscreen canvas
      const canvas = document.createElement("canvas");
      canvas.width = 800;
      canvas.height = 200;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      // Load audio
      const audio = new Audio();
      audio.crossOrigin = "anonymous";
      audio.src = getAudioStreamUrl(audioPath);

      await new Promise<void>((resolve, reject) => {
        audio.addEventListener("loadeddata", () => resolve());
        audio.addEventListener("error", reject);
      });

      // Create audio context
      const audioCtx = new AudioContext();
      const source = audioCtx.createMediaElementSource(audio);
      const analyser = audioCtx.createAnalyser();
      analyser.fftSize = 2048;
      source.connect(analyser);
      analyser.connect(audioCtx.destination);

      if (type === "waveform") {
        // Generate waveform
        const bufferLength = analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        analyser.getByteTimeDomainData(dataArray);

        ctx.fillStyle = "#1a1a2e";
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.lineWidth = 2;
        ctx.strokeStyle = "#6382ff";
        ctx.beginPath();

        const sliceWidth = canvas.width / bufferLength;
        let x = 0;

        for (let i = 0; i < bufferLength; i++) {
          const v = dataArray[i] / 128.0;
          const y = (v * canvas.height) / 2;

          if (i === 0) {
            ctx.moveTo(x, y);
          } else {
            ctx.lineTo(x, y);
          }

          x += sliceWidth;
        }

        ctx.lineTo(canvas.width, canvas.height / 2);
        ctx.stroke();
      } else {
        // Generate spectrogram
        const bufferLength = analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);

        ctx.fillStyle = "#1a1a2e";
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        // Simple spectrogram visualization
        const barWidth = canvas.width / bufferLength * 2.5;
        let x = 0;

        for (let i = 0; i < bufferLength; i++) {
          analyser.getByteFrequencyData(dataArray);
          const barHeight = (dataArray[i] / 255) * canvas.height;

          const hue = (i / bufferLength) * 60 + 200;
          ctx.fillStyle = `hsla(${hue}, 80%, 60%, 0.8)`;
          ctx.fillRect(x, canvas.height - barHeight, barWidth, barHeight);

          x += barWidth + 1;
          if (x > canvas.width) break;
        }
      }

      // Convert to base64
      const imageData = canvas.toDataURL("image/png").split(",")[1];

      // Save to backend
      await saveAudioAsset(audioPath, type, imageData, "image/png", canvas.width, canvas.height);

      // Cleanup
      audioCtx.close();

      alert(`${type === "waveform" ? "Waveform" : "Spectrogram"} generated and saved!`);
    } catch (err) {
      console.error(`Failed to generate ${type}:`, err);
      alert(`Failed to generate ${type}`);
    }
  }, []);

  const handlePreviewM3U = useCallback(async (filePath: string, name: string) => {
    try {
      const res = await loadM3UPlaylist(filePath);
      if (res.ok) {
        // Fetch durations for all items
        const itemsWithDuration = await Promise.all(
          res.items.map(async (item) => {
            const duration = await getAudioDuration(item.path);
            return { ...item, duration };
          })
        );
        setPreviewPlaylist({ name, items: itemsWithDuration });
      }
    } catch (err) {
      console.error("Failed to load M3U:", err);
    }
  }, []);

  const handleLoadPlaylist = useCallback(() => {
    if (!previewPlaylist) return;
    const newItems = previewPlaylist.items.map(item => ({
      id: `${item.path}-${Date.now()}-${Math.random()}`,
      path: item.path,
      name: item.name,
      addedAt: Date.now(),
      duration: (item as any).duration,
    }));
    setPlaylist(newItems);
    setPreviewPlaylist(null);
  }, [previewPlaylist]);

  const handleAppendPlaylist = useCallback(() => {
    if (!previewPlaylist) return;
    const newItems = previewPlaylist.items.map(item => ({
      id: `${item.path}-${Date.now()}-${Math.random()}`,
      path: item.path,
      name: item.name,
      addedAt: Date.now(),
      duration: (item as any).duration,
    }));
    setPlaylist(prev => [...prev, ...newItems]);
    setPreviewPlaylist(null);
  }, [previewPlaylist]);

  // Filter files by label
  const filesFilteredByLabel = labelFilter
    ? library?.files.filter(f => {
        // This is a client-side filter; we'd need to track labels per file
        // For now, just show all files when filtering
        return true;
      }) ?? []
    : [];

  const savePlaylistToM3U = useCallback(async () => {
    if (playlist.length === 0) return;
    try {
      setSavingPlaylist(true);
      const name = playlistName || `playlist-${new Date().toISOString().slice(0, 10)}`;
      await savePlaylistAsM3U(name, playlist.map(item => ({ path: item.path, name: item.name })));
      setPlaylistName("");
      // Refresh playlist list
      const res = await listPlaylists();
      setSavedPlaylists(res.playlists);
    } catch (err) {
      console.error("Failed to save playlist:", err);
    } finally {
      setSavingPlaylist(false);
    }
  }, [playlist, playlistName]);

  // ── Seek ─────────────────────────────────────────────────────────
  const handleSeek = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = audioRef.current;
    if (!audio) return;
    const time = parseFloat(e.target.value);
    audio.currentTime = time;
    setCurrentTime(time);
  }, []);

  // ── Directory structure ──────────────────────────────────────────
  // Extract unique directories from files
  const allDirectories = library?.files.reduce((dirs, file) => {
    const parts = file.path.split('/');
    // Add all parent directories
    for (let i = 1; i < parts.length; i++) {
      const dir = parts.slice(0, i).join('/');
      if (dir && !dirs.includes(dir)) dirs.push(dir);
    }
    return dirs;
  }, [] as string[]).sort() ?? [];

  // Current directory level - files directly in scanPath
  const currentDirPrefix = scanPath === '.' ? '' : `${scanPath}/`;
  const directFiles = library?.files.filter(f => {
    // File must be directly in current directory (no subdirectory)
    if (!f.path.startsWith(currentDirPrefix)) return false;
    const relative = f.path.slice(currentDirPrefix.length);
    return !relative.includes('/');
  }) ?? [];

  // Subdirectories in current directory
  const subdirectories = allDirectories
    .filter(dir => {
      if (scanPath === '.') return !dir.includes('/');
      return dir.startsWith(scanPath + '/') && dir.slice(scanPath.length + 1).split('/').length === 1;
    })
    .map(dir => dir.split('/').pop()!);

  // Filtered files (search applies to all files)
  const filteredFiles = filterText
    ? library?.files.filter(f => f.name.toLowerCase().includes(filterText.toLowerCase())) ?? []
    : directFiles;

  // ── Sidebar resize ───────────────────────────────────────────────
  const handleSidebarResize = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    const startX = e.clientX;
    const startWidth = sidebarWidth;

    const onMouseMove = (moveEvt: MouseEvent) => {
      const newWidth = Math.max(200, Math.min(500, startWidth + (moveEvt.clientX - startX)));
      setSidebarWidth(newWidth);
    };

    const onMouseUp = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
      localStorage.setItem(STUDIO_SIDEBAR_WIDTH_KEY, String(sidebarWidth));
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  }, [sidebarWidth]);

  // ── Bottom panel resize ──────────────────────────────────────────
  const handleBottomResize = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    const startY = e.clientY;
    const startHeight = bottomPanelHeight;

    const onMouseMove = (moveEvt: MouseEvent) => {
      const newHeight = Math.max(150, Math.min(600, startHeight - (moveEvt.clientY - startY)));
      setBottomPanelHeight(newHeight);
    };

    const onMouseUp = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
      localStorage.setItem("knoxx_studio_bottom_height", String(bottomPanelHeight));
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  }, [bottomPanelHeight]);

  // ── Chat panel resize ────────────────────────────────────────────
  const handleChatResize = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    const startX = e.clientX;
    const startWidth = chatPanelWidth;

    const onMouseMove = (moveEvt: MouseEvent) => {
      const newWidth = Math.max(300, Math.min(800, startWidth - (moveEvt.clientX - startX)));
      setChatPanelWidth(newWidth);
    };

    const onMouseUp = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
      localStorage.setItem("knoxx_studio_chat_width", String(chatPanelWidth));
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  }, [chatPanelWidth]);

  // ── Render ───────────────────────────────────────────────────────
  return (
    <div style={{ display: "flex", height: "100%", overflow: "hidden" }}>
      {/* Hidden audio element */}
      <audio ref={audioRef} preload="auto" crossOrigin="anonymous" />

      {/* Left: Audio Library Sidebar */}
      {showSidebar && (
        <aside
          style={{
            width: sidebarWidth,
            minWidth: 200,
            maxWidth: 500,
            borderRight: `1px solid var(--token-colors-border-default)`,
            display: "flex",
            flexDirection: "column",
            background: "var(--token-colors-background-surface)",
            overflow: "hidden",
          }}
        >
          {/* Sidebar header */}
          <div style={{ padding: "12px 16px", borderBottom: `1px solid var(--token-colors-border-default)`, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ fontSize: tokens.fontSize.sm, fontWeight: 600 }}>Audio Library</div>
            <Button variant="ghost" size="sm" onClick={() => setShowSidebar(false)}>✕</Button>
          </div>

          {/* Path selector */}
          <div style={{ padding: "8px 16px", borderBottom: `1px solid var(--token-colors-border-subtle)` }}>
            <input
              type="text"
              value={scanPath}
              onChange={e => setScanPath(e.target.value)}
              placeholder="Scan path..."
              style={{
                width: "100%",
                padding: "4px 8px",
                fontSize: tokens.fontSize.xs,
                borderRadius: 4,
                border: `1px solid var(--token-colors-border-default)`,
                background: "var(--token-colors-background-input)",
              }}
            />
          </div>

          {/* Search + Label Filter */}
          <div style={{ padding: "8px 16px", borderBottom: `1px solid var(--token-colors-border-subtle)`, display: "flex", gap: 6 }}>
            <input
              type="text"
              value={filterText}
              onChange={e => setFilterText(e.target.value)}
              placeholder="🔍 Search files..."
              style={{
                flex: 1,
                padding: "4px 8px",
                fontSize: tokens.fontSize.xs,
                borderRadius: 4,
                border: `1px solid var(--token-colors-border-default)`,
                background: "var(--token-colors-background-input)",
              }}
            />
            <div style={{ position: "relative" }}>
              <Button
                variant={labelFilter ? "primary" : "ghost"}
                size="sm"
                onClick={() => {
                  const dropdown = document.getElementById("label-filter-dropdown");
                  if (dropdown) dropdown.style.display = dropdown.style.display === "none" ? "block" : "none";
                }}
                title="Filter by label"
              >
                🏷
              </Button>
              <div
                id="label-filter-dropdown"
                style={{
                  display: "none",
                  position: "absolute",
                  top: "100%",
                  right: 0,
                  zIndex: 100,
                  background: "var(--token-colors-background-surface)",
                  border: `1px solid var(--token-colors-border-default)`,
                  borderRadius: 6,
                  padding: 8,
                  minWidth: 150,
                  maxHeight: 200,
                  overflowY: "auto",
                  boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                }}
              >
                <div
                  onClick={() => {
                    setLabelFilter(null);
                    document.getElementById("label-filter-dropdown")!.style.display = "none";
                  }}
                  style={{
                    padding: "4px 8px",
                    fontSize: tokens.fontSize.xs,
                    cursor: "pointer",
                    borderRadius: 4,
                    background: !labelFilter ? "var(--token-colors-alpha-blue-10)" : "transparent",
                  }}
                >
                  All files
                </div>
                {allLabels.map(label => (
                  <div
                    key={label}
                    onClick={() => {
                      setLabelFilter(label);
                      document.getElementById("label-filter-dropdown")!.style.display = "none";
                    }}
                    style={{
                      padding: "4px 8px",
                      fontSize: tokens.fontSize.xs,
                      cursor: "pointer",
                      borderRadius: 4,
                      background: labelFilter === label ? "var(--token-colors-alpha-blue-10)" : "transparent",
                    }}
                  >
                    {label}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Breadcrumb */}
          <div style={{ padding: "6px 16px", borderBottom: `1px solid var(--token-colors-border-subtle)`, display: "flex", alignItems: "center", gap: 4, fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)", flexWrap: "wrap" }}>
            <button
              onClick={() => setScanPath('.')}
              style={{ background: "none", border: "none", cursor: "pointer", color: scanPath === '.' ? "var(--token-colors-text-default)" : "var(--token-colors-accent-blue)", padding: 0, fontSize: tokens.fontSize.xs }}
            >
              ~
            </button>
            {scanPath !== '.' && scanPath.split('/').map((part, i, arr) => (
              <span key={i} style={{ display: "flex", alignItems: "center", gap: 4 }}>
                <span>/</span>
                <button
                  onClick={() => setScanPath(arr.slice(0, i + 1).join('/'))}
                  style={{ background: "none", border: "none", cursor: "pointer", color: i === arr.length - 1 ? "var(--token-colors-text-default)" : "var(--token-colors-accent-blue)", padding: 0, fontSize: tokens.fontSize.xs }}
                >
                  {part}
                </button>
              </span>
            ))}
          </div>

          {/* File list */}
          <div style={{ flex: 1, overflowY: "auto", padding: "8px" }}>
            {loading ? (
              <div style={{ padding: 16, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.sm }}>Loading...</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                {/* Parent directory */}
                {scanPath !== '.' && (
                  <div
                    onClick={() => {
                      const parts = scanPath.split('/');
                      if (parts.length > 1) setScanPath(parts.slice(0, -1).join('/'));
                      else setScanPath('.');
                    }}
                    style={{
                      display: "flex", alignItems: "center", gap: 8, padding: "6px 8px",
                      borderRadius: 6, cursor: "pointer", transition: "all 0.15s",
                    }}
                    onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-alpha-white-05)"}
                    onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                  >
                    <span style={{ fontSize: 16 }}>📁</span>
                    <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 500 }}>..</div>
                  </div>
                )}

                {/* Subdirectories */}
                {!filterText && subdirectories.map(dir => (
                  <div
                    key={dir}
                    onClick={() => setScanPath(scanPath + '/' + dir)}
                    style={{
                      display: "flex", alignItems: "center", gap: 8, padding: "6px 8px",
                      borderRadius: 6, cursor: "pointer", transition: "all 0.15s",
                    }}
                    onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-alpha-white-05)"}
                    onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                  >
                    <span style={{ fontSize: 16 }}>📁</span>
                    <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 500 }}>{dir}</div>
                  </div>
                ))}

                {/* Files */}
                {filteredFiles.length === 0 && subdirectories.length === 0 && (
                  <div style={{ padding: 16, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.sm }}>
                    {filterText ? `No files matching "${filterText}"` : `No audio files in "${scanPath}"`}
                  </div>
                )}
                {filteredFiles.map(file => (
                  <div
                    key={file.path}
                    onClick={() => playFile(file)}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                      padding: "6px 8px",
                      borderRadius: 6,
                      cursor: "pointer",
                      background: currentFile?.path === file.path ? "var(--token-colors-alpha-blue-10)" : "transparent",
                      border: currentFile?.path === file.path ? `1px solid var(--token-colors-accent-blue)` : "1px solid transparent",
                      transition: "all 0.15s",
                    }}
                    onMouseEnter={e => {
                      if (currentFile?.path !== file.path) {
                        e.currentTarget.style.background = "var(--token-colors-alpha-white-05)";
                      }
                    }}
                    onMouseLeave={e => {
                      if (currentFile?.path !== file.path) {
                        e.currentTarget.style.background = "transparent";
                      }
                    }}
                  >
                    <span style={{ fontSize: 16 }}>{fileIcon(file.ext)}</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                        {file.name}
                      </div>
                      <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>
                        {formatBytes(file.size)}
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={e => { e.stopPropagation(); void openLabelEditor(file); }}
                      title="Labels"
                    >
                      🏷
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={e => { e.stopPropagation(); addToPlaylist(file); }}
                      title="Add to queue"
                    >
                      +
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Saved Playlists */}
          <div style={{ borderTop: `1px solid var(--token-colors-border-default)`, display: "flex", flexDirection: "column", flex: 1, minHeight: 0 }}>
            <div style={{ padding: "8px 16px", display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid var(--token-colors-border-subtle)` }}>
              <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600 }}>
                Playlists <Badge size="sm">{savedPlaylists.length}</Badge>
              </div>
              <Button variant="ghost" size="sm" onClick={() => void listPlaylists().then(res => setSavedPlaylists(res.playlists)).catch(() => {})}>↻</Button>
            </div>
            <div style={{ flex: 1, overflowY: "auto", padding: "4px 8px" }}>
              {savedPlaylists.length === 0 ? (
                <div style={{ padding: 8, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.xs }}>
                  No saved playlists
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 1 }}>
                  {savedPlaylists.map(pl => (
                    <div
                      key={pl.path}
                      onClick={() => void handlePreviewM3U(pl.path, pl.name)}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 6,
                        padding: "4px 8px",
                        borderRadius: 4,
                        fontSize: tokens.fontSize.xs,
                        cursor: "pointer",
                        background: previewPlaylist?.name === pl.name ? "var(--token-colors-alpha-blue-10)" : "transparent",
                      }}
                      onMouseEnter={e => {
                        if (previewPlaylist?.name !== pl.name) e.currentTarget.style.background = "var(--token-colors-alpha-white-05)";
                      }}
                      onMouseLeave={e => {
                        if (previewPlaylist?.name !== pl.name) e.currentTarget.style.background = "transparent";
                      }}
                    >
                      <span>🎵</span>
                      <span style={{ flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{pl.name}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Playlist Preview */}
          {previewPlaylist && (
            <div style={{ borderTop: `1px solid var(--token-colors-border-default)`, display: "flex", flexDirection: "column", maxHeight: "40%" }}>
              <div style={{ padding: "8px 16px", display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid var(--token-colors-border-subtle)` }}>
                <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600 }}>
                  Preview: {previewPlaylist.name}
                </div>
                <div style={{ display: "flex", gap: 4 }}>
                  <Button size="sm" onClick={handleLoadPlaylist}>Load</Button>
                  <Button variant="ghost" size="sm" onClick={handleAppendPlaylist}>Append</Button>
                  <Button variant="ghost" size="sm" onClick={() => setPreviewPlaylist(null)}>✕</Button>
                </div>
              </div>
              <div style={{ flex: 1, overflowY: "auto", padding: "4px 8px" }}>
                <div style={{ display: "flex", flexDirection: "column", gap: 1 }}>
                  {previewPlaylist.items.map((item, index) => (
                    <div
                      key={`${item.path}-${index}`}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 6,
                        padding: "3px 6px",
                        borderRadius: 4,
                        fontSize: tokens.fontSize.xs,
                      }}
                      onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-alpha-white-05)"}
                      onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                    >
                      <span style={{ width: 16, textAlign: "center", color: "var(--token-colors-text-muted)" }}>{index + 1}</span>
                      <span style={{ flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.name}</span>
                      {(item as any).duration !== undefined && (item as any).duration > 0 && (
                        <span style={{ color: "var(--token-colors-text-muted)", fontSize: 10, minWidth: 28, textAlign: "right" }}>
                          {formatDuration((item as any).duration)}
                        </span>
                      )}
                      <button
                        onClick={() => {
                          const newItems = [item].map(i => ({
                            id: `${i.path}-${Date.now()}-${Math.random()}`,
                            path: i.path,
                            name: i.name,
                            addedAt: Date.now(),
                            duration: (i as any).duration,
                          }));
                          setPlaylist(prev => [...prev, ...newItems]);
                        }}
                        style={{ background: "none", border: "none", cursor: "pointer", padding: 2, fontSize: 12 }}
                        title="Add to queue"
                      >
                        +
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </aside>
      )}

      {/* Resize handle */}
      {showSidebar && (
        <div
          onMouseDown={handleSidebarResize}
          style={{
            width: 4,
            cursor: "col-resize",
            background: "var(--token-colors-border-default)",
            transition: "background 0.15s",
          }}
          onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-accent-blue)"}
          onMouseLeave={e => e.currentTarget.style.background = "var(--token-colors-border-default)"}
        />
      )}

      {/* Center: Player + Tabs */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 400, overflow: "hidden", position: "relative" }}>
        {/* Top bar */}
        <header style={{
          padding: "8px 16px",
          borderBottom: `1px solid var(--token-colors-border-default)`,
          display: "flex",
          alignItems: "center",
          gap: 12,
          background: "var(--token-colors-background-surface)",
          flexWrap: "wrap",
        }}>
          {!showSidebar && (
            <Button variant="ghost" size="sm" onClick={() => setShowSidebar(true)}>
              ☰
            </Button>
          )}
          <div style={{ fontSize: tokens.fontSize.lg, fontWeight: 600 }}>Broadcast Studio</div>
          <Badge variant="default">{library?.count ?? 0} files</Badge>
          <Button variant="ghost" size="sm" onClick={() => void handleDiscordAudioScan()} disabled={scanningDiscordAudio}>
            {scanningDiscordAudio ? "Scanning Discord audio…" : "Scan Discord audio"}
          </Button>
          <Button variant="ghost" size="sm" onClick={() => void handleDiscordImageScan()} disabled={scanningDiscordImages}>
            {scanningDiscordImages ? "Scanning Discord images…" : "Scan Discord images"}
          </Button>
          {discordScanStatus && (
            <Badge variant="default">
              Audio {discordScanStatus.imported_count} imported / {discordScanStatus.attachments_found} found
            </Badge>
          )}
          {discordImageScanStatus && (
            <Badge variant="default">
              Images {discordImageScanStatus.imported_count} imported / {discordImageScanStatus.attachments_found} found
            </Badge>
          )}
          {currentFile && (
            <Badge variant={playerState === "playing" ? "success" : "default"}>
              {playerState === "playing" ? "● Live" : playerState === "paused" ? "❚❚ Paused" : "Ready"}
            </Badge>
          )}
        </header>

        {(discordScanStatus || discordImageScanStatus) && (
          <div style={{
            padding: "8px 16px",
            borderBottom: `1px solid var(--token-colors-border-subtle)`,
            background: "var(--token-colors-background-surface)",
            fontSize: tokens.fontSize.xs,
            color: "var(--token-colors-text-muted)",
            display: "flex",
            gap: 12,
            flexWrap: "wrap",
          }}>
            {discordScanStatus && (
              <>
                <span>Audio channels: {discordScanStatus.channels_scanned}</span>
                <span>Audio messages: {discordScanStatus.messages_scanned}</span>
                <span>Audio imported: {discordScanStatus.imported_count}</span>
                <span>Audio skipped: {discordScanStatus.skipped_count}</span>
                <span>Audio failed: {discordScanStatus.failed_count}</span>
                {discordScanStatus.manifest_path && <span>Audio manifest: {discordScanStatus.manifest_path}</span>}
              </>
            )}
            {discordImageScanStatus && (
              <>
                <span>Image channels: {discordImageScanStatus.channels_scanned}</span>
                <span>Image messages: {discordImageScanStatus.messages_scanned}</span>
                <span>Image imported: {discordImageScanStatus.imported_count}</span>
                <span>Image skipped: {discordImageScanStatus.skipped_count}</span>
                <span>Image failed: {discordImageScanStatus.failed_count}</span>
                {discordImageScanStatus.manifest_path && <span>Image manifest: {discordImageScanStatus.manifest_path}</span>}
              </>
            )}
          </div>
        )}

        {/* Player area (top half) */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: 24, background: "var(--token-colors-background-base)" }}>
          {currentFile ? (
            <div style={{ width: "100%", maxWidth: 600, display: "flex", flexDirection: "column", alignItems: "center", gap: 16 }}>
              {/* Album art + track info side by side */}
              <div style={{ display: "flex", alignItems: "center", gap: 24, width: "100%" }}>
                <div style={{
                  width: 120,
                  height: 120,
                  borderRadius: 12,
                  background: "linear-gradient(135deg, var(--token-colors-accent-blue), var(--token-colors-accent-purple))",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 48,
                  boxShadow: "0 4px 16px rgba(0,0,0,0.2)",
                  flexShrink: 0,
                }}>
                  {fileIcon(currentFile.ext)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: tokens.fontSize.lg, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{currentFile.name}</div>
                  <div style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)", marginTop: 4, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {currentFile.path}
                  </div>
                  <div style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-accent-blue)", marginTop: 4 }}>
                    {playingFrom === "playlist" ? "🎵 Playing from queue" : "📁 Playing from library"}
                  </div>
                </div>
              </div>

              {/* Waveform */}
              <WaveformVisualizer analyserRef={analyserRef} isPlaying={playerState === "playing"} />

              {/* Progress */}
              <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 4 }}>
                <input
                  type="range"
                  min={0}
                  max={duration || 0}
                  value={currentTime}
                  onChange={handleSeek}
                  style={{ width: "100%", accentColor: "var(--token-colors-accent-blue)" }}
                />
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)" }}>
                  <span>{formatDuration(currentTime)}</span>
                  <span>{formatDuration(duration)}</span>
                </div>
              </div>

              {/* Controls */}
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <Button variant="ghost" onClick={playPrevious}>⏮</Button>
                <button
                  onClick={togglePlayPause}
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: "50%",
                    background: "var(--token-colors-accent-blue)",
                    border: "none",
                    cursor: "pointer",
                    fontSize: 20,
                    color: "white",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
                  }}
                >
                  {playerState === "playing" ? "❚❚" : "▶"}
                </button>
                <Button variant="ghost" onClick={playNext}>⏭</Button>
                <Button variant="ghost" onClick={() => currentFile && void openLabelEditor(currentFile)} title="Labels">🏷</Button>
              </div>

              {/* Current file labels */}
              {currentFile && selectedFileLabels.length > 0 && labelingFile?.path === currentFile.path && (
                <div style={{ display: "flex", flexWrap: "wrap", gap: 4, justifyContent: "center" }}>
                  {selectedFileLabels.map(label => (
                    <span
                      key={label}
                      style={{
                        padding: "2px 8px",
                        borderRadius: 10,
                        fontSize: tokens.fontSize.xs,
                        background: "var(--token-colors-alpha-blue-10)",
                        border: `1px solid var(--token-colors-alpha-blue-20)`,
                      }}
                    >
                      {label}
                    </span>
                  ))}
                </div>
              )}

              {/* Volume */}
              <div style={{ display: "flex", alignItems: "center", gap: 8, width: 200 }}>
                <span style={{ fontSize: 14 }}>{volume === 0 ? "🔇" : volume < 0.5 ? "🔉" : "🔊"}</span>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.01}
                  value={volume}
                  onChange={e => setVolume(parseFloat(e.target.value))}
                  style={{ flex: 1, accentColor: "var(--token-colors-accent-blue)" }}
                />
              </div>

              {/* Generate buttons */}
              <div style={{ display: "flex", gap: 8 }}>
                <Button variant="ghost" size="sm" onClick={() => {
                  if (!currentFile) return;
                  generateVisualization(currentFile.path, "spectrogram");
                }}>
                  📊 Spectrogram
                </Button>
                <Button variant="ghost" size="sm" onClick={() => {
                  if (!currentFile) return;
                  generateVisualization(currentFile.path, "waveform");
                }}>
                  📈 Waveform
                </Button>
              </div>

              {/* Label suggestions */}
              {allLabels.length > 0 && currentFile && (
                <div style={{ width: "100%", maxWidth: 500 }}>
                  <div style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)", marginBottom: 6, textAlign: "center" }}>
                    Suggested labels (click to add):
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 6, justifyContent: "center" }}>
                    {allLabels.slice(0, 10).map(label => (
                      <button
                        key={label}
                        onClick={() => {
                          setNewLabel(label);
                          void handleAddLabel();
                        }}
                        style={{
                          padding: "4px 10px",
                          borderRadius: 12,
                          fontSize: tokens.fontSize.xs,
                          background: selectedFileLabels.includes(label) ? "var(--token-colors-alpha-blue-20)" : "var(--token-colors-alpha-white-05)",
                          border: `1px solid ${selectedFileLabels.includes(label) ? "var(--token-colors-accent-blue)" : "var(--token-colors-border-default)"}`,
                          cursor: "pointer",
                          color: selectedFileLabels.includes(label) ? "var(--token-colors-accent-blue)" : "inherit",
                        }}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div style={{ textAlign: "center", color: "var(--token-colors-text-muted)" }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>🎧</div>
              <div style={{ fontSize: tokens.fontSize.lg }}>Select a file to play</div>
              <div style={{ fontSize: tokens.fontSize.sm, marginTop: 8 }}>
                Browse the audio library on the left, or ask the studio agent
              </div>
            </div>
          )}
        </div>

        {/* Bottom tabs (Queue + Labels) */}
        <div style={{ borderTop: `1px solid var(--token-colors-border-default)`, display: "flex", flexDirection: "column", height: bottomPanelHeight, minHeight: 150 }}>
          {/* Resize handle */}
          <div
            onMouseDown={handleBottomResize}
            style={{
              height: 4,
              cursor: "row-resize",
              background: "var(--token-colors-border-default)",
              transition: "background 0.15s",
              position: "absolute",
              top: 0,
              left: 0,
              right: 0,
              zIndex: 10,
            }}
            onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-accent-blue)"}
            onMouseLeave={e => e.currentTarget.style.background = "var(--token-colors-border-default)"}
          />
          {/* Tab bar */}
          <div style={{ display: "flex", borderBottom: `1px solid var(--token-colors-border-subtle)`, background: "var(--token-colors-background-surface)" }}>
            <button
              onClick={() => setCenterTab("queue")}
              style={{
                padding: "8px 16px",
                fontSize: tokens.fontSize.xs,
                fontWeight: 600,
                background: centerTab === "queue" ? "var(--token-colors-alpha-blue-10)" : "transparent",
                border: "none",
                borderBottom: centerTab === "queue" ? `2px solid var(--token-colors-accent-blue)` : "2px solid transparent",
                cursor: "pointer",
                color: centerTab === "queue" ? "var(--token-colors-text-default)" : "var(--token-colors-text-muted)",
              }}
            >
              Now Playing <Badge size="sm">{playlist.length}</Badge>
            </button>
            <button
              onClick={() => setCenterTab("labels")}
              style={{
                padding: "8px 16px",
                fontSize: tokens.fontSize.xs,
                fontWeight: 600,
                background: centerTab === "labels" ? "var(--token-colors-alpha-blue-10)" : "transparent",
                border: "none",
                borderBottom: centerTab === "labels" ? `2px solid var(--token-colors-accent-blue)` : "2px solid transparent",
                cursor: "pointer",
                color: centerTab === "labels" ? "var(--token-colors-text-default)" : "var(--token-colors-text-muted)",
              }}
            >
              🏷 Labels
            </button>
          </div>

          {/* Tab content */}
          <div style={{ flex: 1, overflowY: "auto", display: "flex" }}>
            {centerTab === "queue" ? (
              /* ── Queue Tab ── */
              <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
                <div style={{ padding: "8px 16px", display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: `1px solid var(--token-colors-border-subtle)` }}>
                  <div style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)" }}>
                    {playlist.length === 0 ? "Add files from the library" : "Click ▶ to play from queue"}
                  </div>
                  <div style={{ display: "flex", gap: 4 }}>
                    {playlist.length > 0 && (
                      <>
                        <input
                          type="text"
                          value={playlistName}
                          onChange={e => setPlaylistName(e.target.value)}
                          placeholder="Playlist name..."
                          style={{ width: 120, padding: "2px 6px", fontSize: tokens.fontSize.xs, borderRadius: 4, border: `1px solid var(--token-colors-border-default)`, background: "var(--token-colors-background-input)" }}
                        />
                        <Button variant="ghost" size="sm" onClick={() => void savePlaylistToM3U()} disabled={savingPlaylist}>
                          {savingPlaylist ? "..." : "💾 Save"}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={shufflePlaylist}>🔀</Button>
                        <Button variant="ghost" size="sm" onClick={clearPlaylist}>Clear</Button>
                      </>
                    )}
                  </div>
                </div>
                <div style={{ flex: 1, overflowY: "auto", padding: "8px" }}>
                  {playlist.length === 0 ? (
                    <div style={{ padding: 24, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.sm }}>
                      Click + on any file to add it to the queue
                    </div>
                  ) : (
                    <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
                      {playlist.map((item, index) => (
                        <div
                          key={item.id}
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 4,
                            padding: "6px 8px",
                            borderRadius: 6,
                            fontSize: tokens.fontSize.sm,
                            background: playlistIndexRef.current === index ? "var(--token-colors-alpha-blue-10)" : "transparent",
                            border: playlistIndexRef.current === index ? "1px solid var(--token-colors-accent-blue)" : "1px solid transparent",
                          }}
                        >
                          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                            <span style={{ color: "var(--token-colors-text-muted)", width: 20, textAlign: "center", fontSize: tokens.fontSize.xs }}>{index + 1}</span>
                            <span style={{ flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.name}</span>
                            {item.duration !== undefined && item.duration > 0 && (
                              <span style={{ color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.xs, minWidth: 32, textAlign: "right" }}>
                                {formatDuration(item.duration)}
                              </span>
                            )}
                            <button
                              onClick={() => playPlaylistItem(index)}
                              style={{ background: "none", border: "none", cursor: "pointer", padding: 4, fontSize: 14 }}
                              title="Play now"
                            >
                              ▶
                            </button>
                            <button
                              onClick={() => removeFromPlaylist(item.id)}
                              style={{ background: "none", border: "none", cursor: "pointer", padding: 4, fontSize: 14, color: "var(--token-colors-accent-red)" }}
                              title="Remove"
                            >
                              ×
                            </button>
                          </div>
                          {item.labels && item.labels.length > 0 && (
                            <div style={{ display: "flex", flexWrap: "wrap", gap: 3, paddingLeft: 28 }}>
                              {item.labels.map(label => (
                                <span
                                  key={label}
                                  style={{
                                    padding: "1px 6px",
                                    borderRadius: 8,
                                    fontSize: 10,
                                    background: "var(--token-colors-alpha-blue-10)",
                                    color: "var(--token-colors-text-muted)",
                                  }}
                                >
                                  {label}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              /* ── Labels Tab ── */
              <div style={{ flex: 1, padding: "16px", overflowY: "auto" }}>
                {labelingFile ? (
                  <>
                    <div style={{ fontSize: tokens.fontSize.sm, fontWeight: 600, marginBottom: 12 }}>
                      Labels for: {labelingFile.name}
                    </div>
                    {/* Current labels */}
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 16 }}>
                      {selectedFileLabels.length === 0 ? (
                        <span style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)", fontStyle: "italic" }}>No labels yet</span>
                      ) : (
                        selectedFileLabels.map(label => (
                          <span
                            key={label}
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4,
                              padding: "4px 10px",
                              borderRadius: 12,
                              fontSize: tokens.fontSize.xs,
                              background: "var(--token-colors-alpha-blue-10)",
                              border: `1px solid var(--token-colors-alpha-blue-20)`,
                            }}
                          >
                            {label}
                            <button
                              onClick={() => void handleRemoveLabel(label)}
                              style={{ background: "none", border: "none", cursor: "pointer", padding: 0, fontSize: 12, color: "var(--token-colors-accent-red)" }}
                            >
                              ×
                            </button>
                          </span>
                        ))
                      )}
                    </div>
                    {/* Add label */}
                    <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
                      <input
                        type="text"
                        value={newLabel}
                        onChange={e => setNewLabel(e.target.value)}
                        onKeyDown={e => { if (e.key === "Enter") void handleAddLabel(); }}
                        placeholder="Add label..."
                        style={{ flex: 1, padding: "6px 10px", fontSize: tokens.fontSize.sm, borderRadius: 6, border: `1px solid var(--token-colors-border-default)`, background: "var(--token-colors-background-input)" }}
                      />
                      <Button size="sm" onClick={() => void handleAddLabel()}>Add</Button>
                    </div>
                    {/* Quick labels */}
                    {allLabels.length > 0 && (
                      <>
                        <div style={{ fontSize: tokens.fontSize.xs, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>Quick labels ({Math.min(allLabels.length, 20)}):</div>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                          {allLabels.slice(0, 20).map(label => (
                            <button
                              key={label}
                              onClick={() => { setNewLabel(label); }}
                              style={{
                                padding: "4px 8px",
                                borderRadius: 10,
                                fontSize: tokens.fontSize.xs,
                                background: "var(--token-colors-alpha-white-05)",
                                border: `1px solid var(--token-colors-border-default)`,
                                cursor: "pointer",
                              }}
                            >
                              {label}
                            </button>
                          ))}
                        </div>
                      </>
                    )}
                    {/* Sync symlinks button */}
                    <div style={{ marginTop: 16, borderTop: `1px solid var(--token-colors-border-subtle)`, paddingTop: 12 }}>
                      <Button size="sm" onClick={() => void handleSyncSymlinks()}>
                        📁 Sync symlinks to ./audio/
                      </Button>
                    </div>
                  </>
                ) : (
                  <div style={{ padding: 24, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: tokens.fontSize.sm }}>
                    Click 🏷 on a file in the library to add labels
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Chat resize handle */}
      <div
        onMouseDown={handleChatResize}
        style={{
          width: 4,
          cursor: "col-resize",
          background: "var(--token-colors-border-default)",
          transition: "background 0.15s",
        }}
        onMouseEnter={e => e.currentTarget.style.background = "var(--token-colors-accent-blue)"}
        onMouseLeave={e => e.currentTarget.style.background = "var(--token-colors-border-default)"}
      />

      {/* Right: Chat */}
      <aside style={{
        width: chatPanelWidth,
        minWidth: 300,
        maxWidth: 800,
        display: "flex",
        minHeight: 0,
        overflow: "hidden",
        background: "var(--token-colors-background-surface)",
      }}>
        <ChatWorkspacePane
          controller={chat}
          showFiles={false}
          showCanvasToggle={false}
          onShowFiles={() => setShowSidebar(true)}
        />
      </aside>
    </div>
  );
}
