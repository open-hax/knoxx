/**
 * Pi Session Ingester
 *
 * Automatically ingests pi coding agent session files into Knoxx/OpenPlanner
 * as OpenPlanner event v1 envelopes. This makes pi session history searchable
 * via Knoxx's memory/search tools and graph queries.
 *
 * Architecture:
 *   pi JSONL sessions → parse → map to EventEnvelopeV1 → POST /v1/events
 *
 * Pi sessions are ingested under the same OpenPlanner project as Knoxx sessions
 * (knoxx-session) so they appear in the /api/memory/sessions and /api/memory/search
 * routes automatically.
 *
 * Scheduling:
 *   - Incremental scan: watches for new/modified session files via mtime tracking
 *   - Cron-driven via event-agent infrastructure (pi-session-ingest job)
 *   - Also exposed as POST /api/admin/pi-sessions/ingest for manual triggers
 */

import { readdir, readFile, stat, writeFile, mkdir } from "node:fs/promises";
import { join, basename } from "node:path";
import { createHash } from "node:crypto";

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

const PI_SESSIONS_ROOT = process.env.PI_SESSIONS_ROOT || "/home/err/.pi/agent/sessions";
const INGEST_STATE_DIR = process.env.INGEST_STATE_DIR || "/home/err/.knoxx/pi-ingest-state";
const INGEST_STATE_FILE = join(INGEST_STATE_DIR, "ingested-sessions.json");
const MAX_EVENTS_PER_BATCH = 200;
const MAX_TEXT_LENGTH = 12000; // Truncate long text to avoid oversized events
// The OpenPlanner project name for pi sessions — must match Knoxx's session-project-name
// so pi sessions appear in /api/memory/sessions
const PI_SESSION_PROJECT = process.env.PI_SESSION_PROJECT || "knoxx-session";
const SUPPORTED_EVENT_TYPES = new Set([
  "session", "message", "compaction", "model_change",
  "thinking_level_change", "custom_message", "branch_summary"
]);

// ---------------------------------------------------------------------------
// State tracking (which sessions have been ingested)
// ---------------------------------------------------------------------------

async function ensureStateDir() {
  await mkdir(INGEST_STATE_DIR, { recursive: true });
}

async function loadIngestState() {
  try {
    const raw = await readFile(INGEST_STATE_FILE, "utf-8");
    return JSON.parse(raw);
  } catch {
    return { sessions: {} }; // { sessionId: { mtime, eventCount, ingestedAt } }
  }
}

async function saveIngestState(state) {
  await ensureStateDir();
  await writeFile(INGEST_STATE_FILE, JSON.stringify(state, null, 2), "utf-8");
}

// ---------------------------------------------------------------------------
// Pi session file discovery
// ---------------------------------------------------------------------------

/**
 * Discover pi session directories and their JSONL files.
 * Returns [{ dir, path, sessionId, mtime }]
 */
async function discoverSessionFiles(sinceTs = 0) {
  const files = [];
  let dirs;
  try {
    dirs = await readdir(PI_SESSIONS_ROOT);
  } catch {
    return files;
  }

  for (const dir of dirs) {
    const dirPath = join(PI_SESSIONS_ROOT, dir);
    let dirEntries;
    try {
      dirEntries = await readdir(dirPath);
    } catch {
      continue;
    }

    for (const entry of dirEntries) {
      if (!entry.endsWith(".jsonl")) continue;
      const filePath = join(dirPath, entry);
      try {
        const s = await stat(filePath);
        if (s.mtimeMs > sinceTs) {
          // Extract session ID from filename: <timestamp>_<uuid>.jsonl
          const match = entry.match(/^[\dT:-]+_(.+)\.jsonl$/);
          const sessionId = match ? match[1] : entry.replace(/\.jsonl$/, "");
          files.push({
            dir,
            path: filePath,
            sessionId,
            mtime: s.mtimeMs,
            size: s.size,
          });
        }
      } catch {
        continue;
      }
    }
  }

  return files.sort((a, b) => a.mtime - b.mtime); // Oldest first
}

// ---------------------------------------------------------------------------
// Pi JSONL → OpenPlanner EventEnvelopeV1 mapping
// ---------------------------------------------------------------------------

function truncateText(text, maxLen = MAX_TEXT_LENGTH) {
  if (!text || text.length <= maxLen) return text;
  return text.slice(0, maxLen) + `\n... [truncated ${text.length - maxLen} chars]`;
}

function extractTextFromContent(content) {
  if (!Array.isArray(content)) return "";
  return content
    .filter((c) => c.type === "text" && c.text)
    .map((c) => c.text)
    .join("\n");
}

function extractToolCalls(content) {
  if (!Array.isArray(content)) return [];
  return content.filter((c) => c.type === "toolCall");
}

function extractThinking(content) {
  if (!Array.isArray(content)) return "";
  return content
    .filter((c) => c.type === "thinking" && c.text)
    .map((c) => c.text)
    .join("\n");
}

/**
 * Map a pi session event to one or more OpenPlanner EventEnvelopeV1 events.
 * Returns an array of events.
 */
function mapPiEventToOpenPlannerEvents(piEvent, sessionMeta) {
  const { sessionId, cwd } = sessionMeta;
  const events = [];
  const eventType = piEvent.type;

  // Skip non-substantive event types
  if (!SUPPORTED_EVENT_TYPES.has(eventType)) return events;

  // --- Session start event ---
  if (eventType === "session") {
    events.push({
      schema: "openplanner.event.v1",
      id: `pi:${sessionId}:session`,
      ts: piEvent.timestamp,
      source: "pi-session-ingester",
      kind: "pi.session_start",
      source_ref: {
        project: PI_SESSION_PROJECT,
        session: sessionId,
      },
      text: `Pi session started in ${cwd}`,
      meta: {
        role: "system",
        author: "pi",
        pi_session_version: piEvent.version,
        pi_cwd: cwd,
      },
      extra: {
        pi_session_id: piEvent.id,
        pi_version: piEvent.version,
        workspace: cwd,
        pi_workspace_project: cwdToProject(cwd),
      },
    });
    return events;
  }

  // --- Model change ---
  if (eventType === "model_change") {
    events.push({
      schema: "openplanner.event.v1",
      id: `pi:${sessionId}:model:${piEvent.id}`,
      ts: piEvent.timestamp,
      source: "pi-session-ingester",
      kind: "pi.model_change",
      source_ref: {
        project: PI_SESSION_PROJECT,
        session: sessionId,
      },
      text: `Model: ${piEvent.provider}/${piEvent.modelId}`,
      meta: {
        role: "system",
        author: "pi",
      },
      extra: {
        provider: piEvent.provider,
        model_id: piEvent.modelId,
      },
    });
    return events;
  }

  // --- Thinking level change ---
  if (eventType === "thinking_level_change") {
    // Low-signal, skip unless needed
    return events;
  }

  // --- Compaction (summary of prior context) ---
  if (eventType === "compaction") {
    const summary = piEvent.summary || "";
    if (!summary) return events;
    events.push({
      schema: "openplanner.event.v1",
      id: `pi:${sessionId}:compaction:${piEvent.id}`,
      ts: piEvent.timestamp,
      source: "pi-session-ingester",
      kind: "pi.compaction",
      source_ref: {
        project: PI_SESSION_PROJECT,
        session: sessionId,
      },
      text: truncateText(summary),
      meta: {
        role: "system",
        author: "pi",
      },
      extra: {
        compaction: true,
        pi_workspace_project: cwdToProject(cwd),
      },
    });
    return events;
  }

  // --- Custom message ---
  if (eventType === "custom_message") {
    const content = piEvent.content || "";
    if (!content || piEvent.display === false) return events;
    events.push({
      schema: "openplanner.event.v1",
      id: `pi:${sessionId}:custom:${piEvent.id}`,
      ts: piEvent.timestamp,
      source: "pi-session-ingester",
      kind: `pi.custom.${piEvent.customType || "unknown"}`,
      source_ref: {
        project: PI_SESSION_PROJECT,
        session: sessionId,
      },
      text: truncateText(typeof content === "string" ? content : JSON.stringify(content)),
      meta: {
        role: "system",
        author: "pi",
      },
      extra: {
        custom_type: piEvent.customType,
        pi_workspace_project: cwdToProject(cwd),
      },
    });
    return events;
  }

  // --- Messages (user, assistant, tool results) ---
  if (eventType === "message") {
    const msg = piEvent.message || {};
    const role = msg.role;
    const text = extractTextFromContent(msg.content);
    const thinking = extractThinking(msg.content);
    const toolCalls = extractToolCalls(msg.content);

    // User message
    if (role === "user" && text) {
      events.push({
        schema: "openplanner.event.v1",
        id: `pi:${sessionId}:msg:${piEvent.id}`,
        ts: piEvent.timestamp,
        source: "pi-session-ingester",
        kind: "pi.message",
        source_ref: {
          project: PI_SESSION_PROJECT,
          session: sessionId,
          message: piEvent.id,
        },
        text: truncateText(text),
        meta: {
          role: "user",
          author: "user",
        },
        extra: {
          pi_message_id: piEvent.id,
          pi_parent_id: piEvent.parentId,
          pi_workspace_project: cwdToProject(cwd),
        },
      });
    }

    // Assistant message with text
    if (role === "assistant") {
      if (text) {
        events.push({
          schema: "openplanner.event.v1",
          id: `pi:${sessionId}:msg:${piEvent.id}`,
          ts: piEvent.timestamp,
          source: "pi-session-ingester",
          kind: "pi.message",
          source_ref: {
            project: PI_SESSION_PROJECT,
            session: sessionId,
            message: piEvent.id,
          },
          text: truncateText(text),
          meta: {
            role: "assistant",
            author: "pi",
            model: msg.model || msg.provider || "unknown",
          },
          extra: {
            pi_message_id: piEvent.id,
            pi_parent_id: piEvent.parentId,
            provider: msg.provider,
            model: msg.model,
            usage: msg.usage || null,
            stop_reason: msg.stopReason || null,
            pi_workspace_project: cwdToProject(cwd),
          },
        });
      }

      // Thinking/reasoning
      if (thinking) {
        events.push({
          schema: "openplanner.event.v1",
          id: `pi:${sessionId}:thinking:${piEvent.id}`,
          ts: piEvent.timestamp,
          source: "pi-session-ingester",
          kind: "pi.reasoning",
          source_ref: {
            project: PI_SESSION_PROJECT,
            session: sessionId,
          },
          text: truncateText(thinking),
          meta: {
            role: "system",
            author: "pi",
            model: msg.model || msg.provider || "unknown",
          },
          extra: {
            pi_message_id: piEvent.id,
          },
        });
      }

      // Tool calls
      for (const tc of toolCalls) {
        const toolName = tc.name || "unknown";
        const argsPreview = tc.arguments
          ? truncateText(JSON.stringify(tc.arguments), 2000)
          : "";

        events.push({
          schema: "openplanner.event.v1",
          id: `pi:${sessionId}:tool:${tc.id || piEvent.id}`,
          ts: piEvent.timestamp,
          source: "pi-session-ingester",
          kind: "pi.tool_call",
          source_ref: {
            project: PI_SESSION_PROJECT,
            session: sessionId,
            message: piEvent.id,
          },
          text: truncateText(`Tool: ${toolName}\n${argsPreview}`),
          meta: {
            role: "system",
            author: "pi",
            model: msg.model || "unknown",
          },
          extra: {
            pi_message_id: piEvent.id,
            tool_name: toolName,
            tool_call_id: tc.id,
            tool_arguments_preview: argsPreview,
          },
        });
      }
    }
  }

  return events;
}

/**
 * Convert a pi CWD path to an OpenPlanner project name.
 * /home/err/devel → devel
 * /home/err/devel/orgs/open-hax/proxx → orgs/open-hax/proxx
 * /home/err → home
 */
function cwdToProject(cwd) {
  if (!cwd) return "pi";
  // Strip common prefixes
  const normalized = cwd
    .replace(/^\/home\/[^/]+\/devel\//, "")
    .replace(/^\/home\/[^/]+\//, "")
    .replace(/^\//, "");
  return normalized || "pi";
}

// ---------------------------------------------------------------------------
// JSONL parsing
// ---------------------------------------------------------------------------

async function parseSessionFile(filePath) {
  const raw = await readFile(filePath, "utf-8");
  const lines = raw.split("\n");
  const events = [];
  let sessionMeta = { sessionId: "unknown", cwd: "/unknown" };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    try {
      const parsed = JSON.parse(trimmed);
      if (parsed.type === "session") {
        sessionMeta = {
          sessionId: parsed.id || "unknown",
          cwd: parsed.cwd || "/unknown",
        };
      }
      events.push(parsed);
    } catch {
      // Skip malformed lines
    }
  }

  return { events, sessionMeta };
}

// ---------------------------------------------------------------------------
// Ingestion pipeline
// ---------------------------------------------------------------------------

/**
 * Ingest a single pi session file into OpenPlanner.
 * Returns { sessionId, eventsIngested, batches }
 */
async function ingestSessionFile(filePath, sessionFileMeta, openplannerRequestFn) {
  const { events: piEvents, sessionMeta } = await parseSessionFile(filePath);

  // Map all pi events to OpenPlanner events
  const allOpEvents = [];
  for (const piEvent of piEvents) {
    const opEvents = mapPiEventToOpenPlannerEvents(piEvent, sessionMeta);
    allOpEvents.push(...opEvents);
  }

  if (allOpEvents.length === 0) {
    return { sessionId: sessionMeta.sessionId, eventsIngested: 0, batches: 0 };
  }

  // Batch and send
  let eventsIngested = 0;
  let batches = 0;
  for (let i = 0; i < allOpEvents.length; i += MAX_EVENTS_PER_BATCH) {
    const batch = allOpEvents.slice(i, i + MAX_EVENTS_PER_BATCH);
    try {
      await openplannerRequestFn("POST", "/v1/events", { events: batch });
      eventsIngested += batch.length;
      batches++;
    } catch (err) {
      console.error(`[pi-ingester] Failed to ingest batch for session ${sessionMeta.sessionId}:`, err.message);
      // Continue with next batch rather than failing entirely
    }
  }

  return { sessionId: sessionMeta.sessionId, eventsIngested, batches };
}

/**
 * Main ingestion entry point. Scans for new/modified pi sessions and ingests them.
 *
 * @param {object} options
 * @param {function} options.openplannerRequestFn - Function to call OpenPlanner API
 * @param {boolean} options.force - Re-ingest all sessions (ignore state)
 * @param {number} options.limit - Max sessions to ingest in this run
 * @param {string[]} options.sessionDirs - Only ingest from these workspace dirs
 * @returns {object} Ingestion summary
 */
export async function runPiSessionIngest({
  openplannerRequestFn,
  force = false,
  limit = 50,
  sessionDirs = null,
} = {}) {
  if (!openplannerRequestFn) {
    throw new Error("openplannerRequestFn is required");
  }

  const state = force ? { sessions: {} } : await loadIngestState();
  // When state is empty, ingest everything (sinceTs = 0)
  const ingestedMtimes = Object.values(state.sessions).map((s) => s.mtime || 0);
  const minMtime = ingestedMtimes.length > 0 ? Math.min(...ingestedMtimes) : 0;
  const allFiles = await discoverSessionFiles(force ? 0 : minMtime - 60000); // 60s overlap for safety

  // Filter by session dirs if specified
  const files = sessionDirs
    ? allFiles.filter((f) => sessionDirs.some((d) => f.dir.includes(d)))
    : allFiles;

  // Filter out already-ingested sessions (by sessionId + mtime match)
  const newFiles = force
    ? files
    : files.filter((f) => {
        const existing = state.sessions[f.sessionId];
        return !existing || existing.mtime < f.mtime;
      });

  // Apply limit
  const toIngest = newFiles.slice(0, limit);

  if (toIngest.length === 0) {
    return {
      ok: true,
      scanned: files.length,
      newSessions: 0,
      ingested: 0,
      totalEvents: 0,
      skipped: files.length - toIngest.length,
    };
  }

  console.log(`[pi-ingester] Found ${toIngest.length} sessions to ingest (scanned ${files.length})`);

  const results = [];
  for (const file of toIngest) {
    try {
      const result = await ingestSessionFile(file.path, file, openplannerRequestFn);
      state.sessions[file.sessionId] = {
        mtime: file.mtime,
        eventCount: result.eventsIngested,
        ingestedAt: new Date().toISOString(),
        dir: file.dir,
        size: file.size,
      };
      results.push(result);
    } catch (err) {
      console.error(`[pi-ingester] Failed to ingest session ${file.sessionId}:`, err.message);
      results.push({ sessionId: file.sessionId, error: err.message, eventsIngested: 0, batches: 0 });
    }
  }

  await saveIngestState(state);

  const totalEvents = results.reduce((sum, r) => sum + (r.eventsIngested || 0), 0);
  const errors = results.filter((r) => r.error);

  return {
    ok: true,
    scanned: files.length,
    newSessions: toIngest.length,
    ingested: results.filter((r) => !r.error).length,
    totalEvents,
    errors: errors.length,
    details: results,
  };
}

/**
 * Get the status of pi session ingestion (what's been ingested, what's pending).
 */
export async function getPiIngestStatus() {
  const state = await loadIngestState();
  const allFiles = await discoverSessionFiles(0);

  const ingestedIds = new Set(Object.keys(state.sessions));
  const pendingFiles = allFiles.filter((f) => !ingestedIds.has(f.sessionId));
  const staleFiles = allFiles.filter((f) => {
    const existing = state.sessions[f.sessionId];
    return existing && existing.mtime < f.mtime;
  });

  const totalIngestedEvents = Object.values(state.sessions).reduce((sum, s) => sum + (s.eventCount || 0), 0);

  return {
    ok: true,
    piSessionsRoot: PI_SESSIONS_ROOT,
    totalSessionFiles: allFiles.length,
    ingestedSessions: ingestedIds.size,
    pendingSessions: pendingFiles.length,
    staleSessions: staleFiles.length,
    totalIngestedEvents,
    lastIngestedAt: Object.values(state.sessions).reduce(
      (max, s) => (s.ingestedAt > max ? s.ingestedAt : max),
      ""
    ),
    recentIngested: Object.entries(state.sessions)
      .sort((a, b) => (b[1].ingestedAt || "").localeCompare(a[1].ingestedAt || ""))
      .slice(0, 10)
      .map(([id, s]) => ({
        sessionId: id,
        eventCount: s.eventCount,
        ingestedAt: s.ingestedAt,
        dir: s.dir,
      })),
  };
}

/**
 * Get a list of all pi sessions that can be browsed/searched.
 * Returns metadata about each session (id, workspace, message count, time range).
 */
export async function listPiSessions({ limit = 50, offset = 0, workspace = null } = {}) {
  const allFiles = await discoverSessionFiles(0);

  let filtered = workspace
    ? allFiles.filter((f) => f.dir.includes(workspace))
    : allFiles;

  // Sort by mtime descending (most recent first)
  filtered.sort((a, b) => b.mtime - a.mtime);

  const total = filtered.length;
  const page = filtered.slice(offset, offset + limit);

  // Parse minimal metadata from each session file
  const sessions = await Promise.all(
    page.map(async (f) => {
      try {
        const raw = await readFile(f.path, "utf-8");
        const firstLine = raw.split("\n").find((l) => l.trim());
        if (!firstLine) return null;
        const header = JSON.parse(firstLine);
        // Count messages quickly
        const msgCount = (raw.match(/"type":"message"/g) || []).length;
        const toolCount = (raw.match(/"type":"toolCall"/g) || []).length;
        return {
          sessionId: header.id || f.sessionId,
          workspace: header.cwd || f.dir,
          startTime: header.timestamp,
          lastModified: new Date(f.mtime).toISOString(),
          messageCount: msgCount,
          toolCallCount: toolCount,
          fileSize: f.size,
          dir: f.dir,
        };
      } catch {
        return {
          sessionId: f.sessionId,
          workspace: f.dir,
          lastModified: new Date(f.mtime).toISOString(),
          fileSize: f.size,
          dir: f.dir,
        };
      }
    })
  );

  return {
    ok: true,
    sessions: sessions.filter(Boolean),
    total,
    offset,
    limit,
    has_more: offset + sessions.length < total,
  };
}
