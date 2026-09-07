import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  cleanupDeploymentContentAdmission,
  inspectDeploymentContentAdmission,
  validateCleanupScope,
} from "../../scripts/cleanup-deployment-content-admission.mjs";

const sourceDocument = "knoxx.verifyadmission/probe20260902010101123";
const generatedDocument = "knoxx.generated/post-0123456789abcdef01234567";
const verifier = readFileSync(
  new URL("../../../scripts/verify-deployment-content-admission.sh", import.meta.url),
  "utf8",
);

function fakeDb(rowsByCollection = {}) {
  const collections = new Map();
  const operations = [];
  return {
    collection(name) {
      if (!collections.has(name)) {
        collections.set(name, {
          deletes: [],
          finds: [],
          counts: [],
          deleteMany: async function deleteMany(filter) {
            this.deletes.push(filter);
            operations.push({ type: "delete", collection: name });
            return { acknowledged: true, deletedCount: 1 };
          },
          find: function find(filter, options) {
            this.finds.push({ filter, options });
            operations.push({ type: "find", collection: name });
            return { toArray: async () => rowsByCollection[name] ?? [] };
          },
          countDocuments: async function countDocuments(filter) {
            this.counts.push(filter);
            operations.push({ type: "count", collection: name });
            return 0;
          },
        });
      }
      return collections.get(name);
    },
    collections,
    operations,
  };
}

test("deployment verifier cleanup rejects scopes outside its run-owned namespaces", () => {
  const common = {
    eventIds: ["event-1"],
    eventsCollection: "events",
    vectorCollection: "event_chunks",
    graphNodeEmbeddingCollection: "graph_node_embeddings",
  };

  assert.throws(
    () => validateCleanupScope({ ...common, documents: ["production/real-document"] }),
    /outside the verifier namespaces/,
  );
  assert.throws(
    () => validateCleanupScope({ ...common, documents: [sourceDocument, "knoxx.generated/post-anything"] }),
    /outside the verifier namespaces/,
  );
  assert.throws(
    () => validateCleanupScope({ ...common, documents: [sourceDocument], eventIds: [] }),
    /at least one exact admission event id/,
  );
  assert.throws(
    () => validateCleanupScope({ ...common, documents: [sourceDocument], eventsCollection: "$events" }),
    /events collection is invalid/,
  );
  assert.deepEqual(
    validateCleanupScope({
      ...common,
      documents: [generatedDocument, sourceDocument],
    }).documents,
    [sourceDocument, generatedDocument],
  );
});

test("deployment verifier trap drains partial runs and retains only a green explicit demo", () => {
  const cleanupStart = verifier.indexOf("\ncleanup() {");
  const cleanupEnd = verifier.indexOf("trap cleanup EXIT", cleanupStart);
  const cleanup = verifier.slice(cleanupStart, cleanupEnd);
  const databaseFailureGuard = cleanup.indexOf('if [ "$cleanup_failed" -eq 1 ]; then');
  const translationFileCleanup = cleanup.indexOf('if [ "${#TRANSLATION_CONTENT_FILES[@]}" -gt 0 ]; then');
  const generatedFileCleanup = cleanup.indexOf('if [ -n "$GENERATED_MANIFEST_FILE" ]');
  const fixtureDirectoryCleanup = cleanup.indexOf(
    'if [ "$FIXTURE_OWNED" -eq 1 ]',
    generatedFileCleanup,
  );

  assert.match(verifier, /trap cleanup EXIT/);
  assert.match(verifier, /await_cleanup_quiescence/);
  assert.match(verifier, /ADMISSION_BARRIER_URL="\/api\/publications\/documents\/admission-barrier"/);
  assert.match(verifier, /quarantine_source_resources/);
  assert.match(verifier, /quarantine_generated_files/);
  assert.match(verifier, /first_canonical.*second_canonical/s);
  assert.doesNotMatch(verifier, /mv -- .*source-fixture.*"\$FIXTURE_DIR"/);
  assert.match(verifier, /cleanup_summary="\$\(cleanup_durable_fixtures\)"/);
  assert.match(verifier, /cleanup_failed=1/);
  assert.match(
    verifier,
    /\[ "\$GRAPH_NODE_EMBEDDINGS_SETTLED" -eq 1 \]/,
  );
  assert.match(
    verifier,
    /\[ "\$EVENT_TURNS_SETTLED" -eq 1 \]/,
  );
  assert.match(
    verifier,
    /EVENT_TURN_STATUS_URL="\/api\/publications\/translations\/event-turn-status"/,
  );
  assert.match(verifier, /wait_for_event_turn_release "\$settlement_owner_event_ids"/);
  assert.match(verifier, /wait_for_graph_node_embeddings "\$settlement_event_ids"/);
  assert.match(
    verifier,
    /\[ "\$REVIEW_DEMO_READY" -eq 1 \] && \[ "\$code" -eq 0 \]/,
  );
  assert.ok(databaseFailureGuard >= 0);
  assert.ok(databaseFailureGuard < translationFileCleanup);
  assert.match(
    cleanup.slice(databaseFailureGuard, translationFileCleanup),
    /exit "\$code"/,
  );
  assert.ok(generatedFileCleanup > translationFileCleanup);
  assert.ok(fixtureDirectoryCleanup > generatedFileCleanup);
  assert.doesNotMatch(verifier, /database fixtures were retained because agent work had not settled/);
  assert.doesNotMatch(verifier, /translation entries were retained because the run was partial/);
  assert.doesNotMatch(verifier, /generated agent work had not settled; exact files were left intact/);
  assert.doesNotMatch(verifier, /mongosh/);
});

test("deployment verifier cleanup uses exact identities for all durable fixture stores", async () => {
  const db = fakeDb({
    events: [
      { id: "source-1" },
      { id: "index-1" },
      { id: "translation-1" },
      { id: "translation-2" },
    ],
    knoxx_translation_turns: [{ turn_id: "turn-1" }, { turn_id: "turn-2" }],
    knoxx_translation_candidate_sets: [
      { candidate_set_id: "set-1", turn_id: "turn-1", candidate_revision: "revision-1" },
      { candidate_set_id: "set-2", turn_id: "turn-2", candidate_revision: "revision-2" },
    ],
    knoxx_translation_dispatches: [
      { document_wire_id: sourceDocument, outcome: "dispatch/completed", batch_id: "run-1" },
      { document_wire_id: generatedDocument, outcome: "dispatch/failed", batch_id: "run-2" },
    ],
  });
  const result = await cleanupDeploymentContentAdmission(db, {
    documents: [sourceDocument, generatedDocument],
    eventIds: ["source-1", "index-1"],
    eventsCollection: "events",
    vectorCollection: "event_chunks",
    graphNodeEmbeddingCollection: "custom_graph_embeddings",
  });

  assert.deepEqual(result.eventIds, ["index-1", "source-1", "translation-1", "translation-2"]);
  assert.equal(result.deletedTotal, 11);
  assert.deepEqual(db.collections.get("events").finds, [{
    filter: {
      "extra.document_id": { $in: [sourceDocument, generatedDocument] },
    },
    options: { projection: { _id: 0, id: 1, kind: 1, "extra.document_id": 1 } },
  }]);
  assert.deepEqual(db.collections.get("events").deletes, [{
    id: {
      $in: [
        "index-1",
        "source-1",
        "translation-1",
        "translation-2",
        "graph.node:derive:index-1",
        "graph.node:derive:source-1",
        "graph.node:derive:translation-1",
        "graph.node:derive:translation-2",
      ],
    },
  }]);
  assert.deepEqual(db.collections.get("event_chunks").deletes, [{
    parent_id: { $in: result.eventIds },
  }]);
  assert.deepEqual(db.collections.get("custom_graph_embeddings").deletes, [{
    source_event_id: { $in: result.eventIds },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_dispatches").deletes, [{
    document_wire_id: { $in: [sourceDocument, generatedDocument] },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_receipts").deletes, [{
    document: { $in: [`:${sourceDocument}`, `:${generatedDocument}`] },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_turns").finds, [{
    filter: { document: { $in: [`:${sourceDocument}`, `:${generatedDocument}`] } },
    options: { projection: { _id: 0, turn_id: 1, document: 1, candidate_revision: 1 } },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_candidate_sets").finds, [{
    filter: { turn_id: { $in: ["turn-1", "turn-2"] } },
    options: {
      projection: {
        _id: 0,
        candidate_set_id: 1,
        turn_id: 1,
        candidate_revision: 1,
      },
    },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_split_reviews").deletes, [{
    candidate_set_id: { $in: ["set-1", "set-2"] },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_candidate_splits").deletes, [{
    turn_id: { $in: ["turn-1", "turn-2"] },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_candidate_sets").deletes, [{
    turn_id: { $in: ["turn-1", "turn-2"] },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_turns").deletes, [{
    document: { $in: [`:${sourceDocument}`, `:${generatedDocument}`] },
  }]);
  assert.deepEqual(
    db.operations.filter((operation) => operation.type === "delete").at(-1),
    { type: "delete", collection: "events" },
  );
  assert.deepEqual(result.candidateRevisions, ["revision-1", "revision-2"]);
});

test("deployment cleanup inspection reconstructs owners and revisions before shell ids exist", async () => {
  const db = fakeDb({
    events: [
      { id: "knoxx-publication-document-indexed-a", kind: "publication.document.indexed" },
      { id: "candidate-event", kind: "translation.segment" },
    ],
    knoxx_translation_turns: [
      { turn_id: "turn-1", document: `:${sourceDocument}`, candidate_revision: "revision-from-turn" },
    ],
    knoxx_translation_candidate_sets: [],
    knoxx_translation_dispatches: [
      { document_wire_id: sourceDocument, outcome: "dispatch/accepted", batch_id: "run-1" },
    ],
  });

  const result = await inspectDeploymentContentAdmission(db, {
    documents: [sourceDocument],
    eventIds: [],
    eventsCollection: "events",
    vectorCollection: "event_chunks",
    graphNodeEmbeddingCollection: "graph_node_embeddings",
  });

  assert.deepEqual(result.eventIds, ["candidate-event", "knoxx-publication-document-indexed-a"]);
  assert.deepEqual(result.ownerEventIds, [
    "knoxx-publication-document-indexed-a",
    "translation-needed-run-1",
  ]);
  assert.deepEqual(result.candidateRevisions, ["revision-from-turn"]);
  assert.deepEqual(result.dispatches, [{
    document: sourceDocument,
    outcome: "dispatch/accepted",
    batchId: "run-1",
  }]);
});

test("deployment verifier cleanup refuses an event id not owned by its documents", async () => {
  const db = fakeDb({ events: [{ id: "source-1" }] });

  await assert.rejects(
    cleanupDeploymentContentAdmission(db, {
      documents: [sourceDocument],
      eventIds: ["production-event"],
      eventsCollection: "events",
      vectorCollection: "event_chunks",
      graphNodeEmbeddingCollection: "graph_node_embeddings",
    }),
    /event ids are not owned by the verifier documents/,
  );
  assert.deepEqual(db.collections.get("events").deletes, []);
});

test("deployment verifier cleanup refuses an unacknowledged delete", async () => {
  const db = fakeDb({ events: [{ id: "source-1" }] });
  db.collection("event_chunks").deleteMany = async function deleteMany(filter) {
    this.deletes.push(filter);
    return { acknowledged: false };
  };

  await assert.rejects(
    cleanupDeploymentContentAdmission(db, {
      documents: [sourceDocument],
      eventIds: ["source-1"],
      eventsCollection: "events",
      vectorCollection: "event_chunks",
      graphNodeEmbeddingCollection: "graph_node_embeddings",
    }),
    /delete was not durably acknowledged/,
  );
  assert.deepEqual(db.collections.get("events").deletes, []);
});

test("deployment verifier retains ownership events when dependent residue remains", async () => {
  const db = fakeDb({
    events: [{ id: "source-1" }],
    knoxx_translation_turns: [
      { document: `:${sourceDocument}`, candidate_revision: "legacy-revision" },
    ],
  });
  db.collection("knoxx_translation_turns").countDocuments = async function countDocuments(filter) {
    this.counts.push(filter);
    return 1;
  };

  await assert.rejects(
    cleanupDeploymentContentAdmission(db, {
      documents: [sourceDocument],
      eventIds: ["source-1"],
      eventsCollection: "events",
      vectorCollection: "event_chunks",
      graphNodeEmbeddingCollection: "graph_node_embeddings",
    }),
    /cleanup left run-owned residue: turns=1/,
  );
  assert.deepEqual(
    db.collections.get("events").deletes,
    [],
    "the exact ownership evidence remains available for a corrected retry",
  );
});

test("deployment verifier cleanup rejects run-owned residue after deletion", async () => {
  const db = fakeDb({ events: [{ id: "source-1" }] });
  db.collection("events").countDocuments = async function countDocuments(filter) {
    this.counts.push(filter);
    return 1;
  };

  await assert.rejects(
    cleanupDeploymentContentAdmission(db, {
      documents: [sourceDocument],
      eventIds: ["source-1"],
      eventsCollection: "events",
      vectorCollection: "event_chunks",
      graphNodeEmbeddingCollection: "graph_node_embeddings",
    }),
    /cleanup left run-owned residue: events=1/,
  );
  assert.deepEqual(db.collections.get("events").counts, [{
    $or: [
      { "extra.document_id": { $in: [sourceDocument] } },
      {
        id: {
          $in: ["source-1", "graph.node:derive:source-1"],
        },
      },
    ],
  }]);
});
