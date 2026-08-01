// Hermetic stub for @open-hax/openplanner-sdk in test builds.
// Mirrors the SDK surface used by knoxx.backend.extern.openplanner-sdk with
// canned data so extern conversion tests never touch MongoDB. ESM with named
// exports, matching the real package's module shape.
export const __calls = [];
const calls = __calls;

function record(name, args) {
  calls.push({ name, args });
}

const manifestLanguage = {
  total_segments: 1,
  approved: 1,
  rejected: 0,
  pending: 0,
  in_review: 0,
  with_corrections: 0,
  avg_labels_per_segment: 0,
};

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
  translation: {
    async listSegments(opts) {
      record("translation.listSegments", opts);
      return { segments: [], total: 0, has_more: false };
    },
    async segment(id) {
      record("translation.segment", id);
      return {
        id,
        source_text: "Hello",
        translated_text: "Hola",
        source_lang: "en",
        target_lang: "es",
        document_id: "doc-1",
        segment_index: 0,
        status: "approved",
        labels: [],
      };
    },
    async createSegment(payload) {
      record("translation.createSegment", payload);
      return { ok: true, id: "segment-1", status: "pending", upserted: true, modified: false };
    },
    async labelSegment(id, payload) {
      record("translation.labelSegment", { id, payload });
      return { ok: true, label: { id: "label-1", segment_id: id }, new_status: "approved", graph_memory: { success: true } };
    },
    async manifest(input) {
      record("translation.manifest", input);
      const project = typeof input === "string" ? input : input?.project ?? "all";
      return {
        project,
        languages: { es: manifestLanguage },
        labelers: [],
        export_sizes: { sft_es: { rows: 1, bytes_estimate: 500 } },
        generated_at: "2026-08-01T00:00:00.000Z",
      };
    },
    async exportSft(opts) {
      record("translation.exportSft", opts);
      return `${JSON.stringify({ prompt: "Translate Hello", target: "Hola" })}\n`;
    },
    async createSegmentsBatch(payload) {
      record("translation.createSegmentsBatch", payload);
      return { ok: true, imported: 1, errors: 0, results: [{ index: 0, id: "segment-1", status: "pending" }] };
    },
    async documents(opts) {
      record("translation.documents", opts);
      return {
        documents: [{
          document_id: "doc-1",
          target_lang: "es",
          title: "Document",
          document_status: "internal",
          total_segments: 1,
          approved: 1,
          pending: 0,
          rejected: 0,
          in_review: 0,
          overall_status: "fully_approved",
        }],
        total: 1,
      };
    },
    async document(documentId, targetLang) {
      record("translation.document", { documentId, targetLang });
      return {
        document: { id: documentId, title: "Document", source_lang: "en" },
        segments: [{ id: "segment-1", document_id: documentId, target_lang: targetLang, status: "approved", labels: [] }],
        summary: { total_segments: 1, approved: 1, pending: 0, rejected: 0, in_review: 0, overall_status: "fully_approved" },
      };
    },
    async reviewDocument(documentId, targetLang, payload) {
      record("translation.reviewDocument", { documentId, targetLang, payload });
      return { ok: true, document_id: documentId, target_lang: targetLang, segments_reviewed: 1, failed_segments: [], overall: payload.overall };
    },
    async createBatch(payload) {
      record("translation.createBatch", payload);
      return { ok: true, batch_id: "batch-1", id: "mongo-batch-1", status: "queued", document_ids: payload.document_ids };
    },
    async listBatches(opts) {
      record("translation.listBatches", opts);
      return { batches: [{ id: "mongo-batch-1", batch_id: "batch-1", status: "queued" }] };
    },
    async nextBatch() {
      record("translation.nextBatch", null);
      return { batch: { id: "mongo-batch-1", batch_id: "batch-1", status: "processing" } };
    },
    async batch(id) {
      record("translation.batch", id);
      return { id, batch_id: "batch-1", status: "processing" };
    },
    async updateBatch(id, payload) {
      record("translation.updateBatch", { id, payload });
      return { ok: true, batch_id: id, status: payload.status };
    },
  },
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
