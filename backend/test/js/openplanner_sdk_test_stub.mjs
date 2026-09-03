// Hermetic stub for @open-hax/openplanner-sdk in test builds.
// Mirrors the SDK surface used by knoxx.backend.extern.openplanner-sdk with
// canned data so extern conversion tests never touch MongoDB. ESM with named
// exports, matching the real package's module shape.
export const __calls = [];
const calls = __calls;
const ingestedEvents = [];
const hotVectorRows = [];
let eventVectorMode = "valid";

// Production requires this setting for fail-closed read-your-writes checks.
// Give the hermetic SDK stub a deliberately tiny vector contract unless the
// test process supplied its own dimensions.
if (process.env.EMBED_PROVIDER_DIMENSIONS === undefined) {
  process.env.EMBED_PROVIDER_DIMENSIONS = "3";
}

function record(name, args) {
  calls.push({ name, args });
}

function persistedEvent(ev) {
  const sourceRef = ev.source_ref ?? {};
  const meta = ev.meta ?? {};
  return {
    _id: ev.id,
    id: ev.id,
    ts: ev.ts,
    source: ev.source,
    kind: ev.kind,
    project: sourceRef.project ?? null,
    session: sourceRef.session ?? null,
    message: sourceRef.message ?? null,
    role: meta.role ?? null,
    author: meta.author ?? null,
    model: meta.model ?? null,
    tags: meta.tags ?? null,
    text: ev.text ?? "",
    attachments: ev.attachments ?? null,
    extra: ev.extra ?? null,
  };
}

function replaceHotVector(eventId, text, embedding, metadata = {}) {
  for (let index = hotVectorRows.length - 1; index >= 0; index -= 1) {
    if (hotVectorRows[index].parent_id === eventId) hotVectorRows.splice(index, 1);
  }
  hotVectorRows.push({
    _id: eventId,
    parent_id: eventId,
    text,
    embedding,
    embedding_dimensions: embedding.length,
    embedding_model: metadata.embedding_model ?? null,
  });
}

function materializeEventVectors(events) {
  const configured = Number(process.env.EMBED_PROVIDER_DIMENSIONS);
  for (const event of events) {
    if (typeof event.text !== "string" || event.text.trim().length === 0) continue;
    if (event.kind === "graph.node" || event.kind === "graph.edge") continue;

    if (eventVectorMode === "missing") {
      for (let index = hotVectorRows.length - 1; index >= 0; index -= 1) {
        if (hotVectorRows[index].parent_id === event.id) hotVectorRows.splice(index, 1);
      }
      continue;
    }

    const dimensions = eventVectorMode === "wrong-dimensions"
      ? Math.max(1, configured - 1)
      : configured;
    replaceHotVector(
      event.id,
      event.text,
      Array.from({ length: dimensions }, (_unused, index) => index + 0.25),
    );
  }
}

function idMatches(row, selector) {
  if (selector === undefined) return true;
  if (typeof selector === "string") return row.id === selector;
  if (Array.isArray(selector?.$in)) return selector.$in.includes(row.id);
  return false;
}

function applyEventSet(row, fields) {
  for (const [path, value] of Object.entries(fields ?? {})) {
    if (path === "extra") {
      row.extra = value;
    } else if (path.startsWith("extra.")) {
      if (!row.extra || typeof row.extra !== "object" || Array.isArray(row.extra)) {
        row.extra = {};
      }
      row.extra[path.slice("extra.".length)] = value;
    } else {
      row[path] = value;
    }
  }
}

export function __setEventVectorMode(mode) {
  eventVectorMode = mode;
}

export function __resetOpenPlannerStub() {
  calls.splice(0, calls.length);
  ingestedEvents.splice(0, ingestedEvents.length);
  hotVectorRows.splice(0, hotVectorRows.length);
  eventVectorMode = "valid";
}

export function shouldIndexEventHotVectors(event) {
  if (typeof event?.text !== "string" || event.text.trim().length === 0) return false;
  return event.kind !== "graph.node" && event.kind !== "graph.edge";
}

export async function indexTextInMongoVectors(params) {
  record("indexTextInMongoVectors", params);
  const embeddings = await params.embeddingFunction.generate([params.text]);
  const embedding = embeddings?.[0];
  if (!Array.isArray(embedding) || embedding.length === 0) {
    throw new Error("stub embedding function returned no vector");
  }
  replaceHotVector(params.parentId, params.text, embedding, params.metadata);
}

const stubSdk = {
  mongo: {
    db: {
      admin() {
        return { command: async () => ({ ok: 1 }) };
      },
    },
    hotVectors: {
      find(filter) {
        const ids = filter?.parent_id?.$in;
        const rows = Array.isArray(ids)
          ? hotVectorRows.filter((row) => ids.includes(row.parent_id))
          : hotVectorRows;
        return { toArray: async () => rows };
      },
    },
    events: {
      find(filter) {
        const ids = filter?.id?.$in;
        const rows = Array.isArray(ids)
          ? ingestedEvents.filter((row) => ids.includes(row.id))
          : ingestedEvents;
        return { toArray: async () => rows };
      },
      async findOne(filter) {
        return ingestedEvents.find((row) => row.id === filter?.id) ?? null;
      },
      async updateOne(filter, update) {
        const row = ingestedEvents.find((candidate) => candidate.id === filter?.id);
        if (!row) return { acknowledged: true, matchedCount: 0, modifiedCount: 0 };
        applyEventSet(row, update?.$set);
        return { acknowledged: true, matchedCount: 1, modifiedCount: 1 };
      },
    },
  },
  embeddingRuntime: {
    hot: {
      getModel() {
        return "stub-embedding-model";
      },
      getBackgroundEmbeddingFunction() {
        return {
          async generate(texts) {
            const dimensions = Number(process.env.EMBED_PROVIDER_DIMENSIONS);
            return texts.map((_text) => Array.from(
              { length: dimensions },
              (_unused, index) => index + 0.5,
            ));
          },
        };
      },
    },
  },
  async ingestEvents(events) {
    record("ingestEvents", events);
    ingestedEvents.push(...events.map(persistedEvent));
    const backgroundIndexing = Promise.resolve().then(() => materializeEventVectors(events));
    return {
      ok: true,
      count: events.length,
      ids: events.map((ev) => ev.id),
      projectedGraphEdges: 0,
      ftsEnabled: true,
      storageBackend: "mongodb",
      indexed: true,
      indexing: events.length > 0 ? "queued" : "skipped",
      queuedEventVectors: events.length,
      queuedGraphNodeEmbeddings: 0,
      acceptedEvents: events,
      backgroundIndexing,
    };
  },
  async searchVector(payload) {
    record("searchVector", payload);
    return {
      ok: true,
      result: { ids: [["stub-1"]], documents: [["stub doc"]], metadatas: [[{ kind: "stub" }]], distances: [[0.1]] },
      tier: payload.tier ?? "both",
      qualityMode: "good_then_not_bad",
      storageBackend: "mongodb",
    };
  },
  async close() {},
};

export async function createOpenPlannerSdk() {
  return stubSdk;
}
export async function listSessionsResponse(_ctx, query) {
  record("listSessionsResponse", query);
  return { ok: true, rows: [{ project: "p", session: "s", last_ts: "2026-01-01T00:00:00Z", event_count: 1 }], total: 1, offset: 0, limit: 50, has_more: false, storageBackend: "mongodb" };
}
export async function getSessionResponse(_ctx, sessionId, query) {
  record("getSessionResponse", { sessionId, query });
  return { ok: true, session: sessionId, rows: [], storageBackend: "mongodb" };
}
export async function listCollectionsResponse() {
  record("listCollectionsResponse", null);
  return { ok: true, collections: [{ name: "events", count: 1, type: "collection" }] };
}
export async function queryCollectionResponse(_ctx, body) {
  record("queryCollectionResponse", body);
  const rows = body.collection === "events"
    ? ingestedEvents.filter((event) => idMatches(event, body.filter?.id))
    : [];
  const limit = Number(body.limit ?? 50);
  return {
    ok: true,
    collection: body.collection,
    count: rows.slice(0, limit).length,
    total: rows.length,
    skip: 0,
    limit,
    rows: rows.slice(0, limit),
  };
}
