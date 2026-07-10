// Hermetic stub for @open-hax/openplanner-sdk in test builds.
// Mirrors the SDK surface used by knoxx.backend.extern.openplanner-sdk with
// canned data so extern conversion tests never touch MongoDB. ESM with named
// exports, matching the real package's module shape.
export const __calls = [];
const calls = __calls;

function record(name, args) {
  calls.push({ name, args });
}

const stubSdk = {
  mongo: {
    db: {
      admin() {
        return { command: async () => ({ ok: 1 }) };
      },
    },
  },
  async ingestEvents(events) {
    record("ingestEvents", events);
    return {
      ok: true,
      count: events.length,
      ids: events.map((ev) => ev.id),
      projectedGraphEdges: 0,
      ftsEnabled: true,
      storageBackend: "mongodb",
      indexed: true,
      indexing: "skipped",
      queuedEventVectors: 0,
      queuedGraphNodeEmbeddings: 0,
      acceptedEvents: events,
      backgroundIndexing: Promise.resolve(),
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
    return { ok: true, collection: body.collection, count: 0, total: 0, skip: 0, limit: 50, rows: [] };
}
