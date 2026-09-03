import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  cleanupDeploymentContentAdmission,
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

test("deployment verifier trap cleans settled runs and retains only a green explicit demo", () => {
  const databaseFailureGuard = verifier.indexOf('if [ "$cleanup_failed" -eq 1 ]; then');
  const translationFileCleanup = verifier.indexOf('if [ "${#TRANSLATION_CONTENT_FILES[@]}" -gt 0 ]; then');
  const generatedFileCleanup = verifier.indexOf('if [ -n "$GENERATED_MANIFEST_FILE" ]');
  const fixtureDirectoryCleanup = verifier.indexOf('if [ "$FIXTURE_OWNED" -eq 1 ]');

  assert.match(verifier, /trap cleanup EXIT/);
  assert.match(verifier, /elif durable_agent_work_settled; then/);
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
    verifier.slice(databaseFailureGuard, translationFileCleanup),
    /exit "\$code"/,
  );
  assert.ok(generatedFileCleanup > translationFileCleanup);
  assert.ok(fixtureDirectoryCleanup > generatedFileCleanup);
  assert.match(
    verifier.slice(translationFileCleanup, generatedFileCleanup),
    /elif durable_agent_work_settled; then/,
  );
  assert.match(
    verifier.slice(generatedFileCleanup, fixtureDirectoryCleanup),
    /elif durable_agent_work_settled; then/,
  );
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
      { candidate_set_id: "set-1" },
      { candidate_set_id: "set-2" },
    ],
  });
  const result = await cleanupDeploymentContentAdmission(db, {
    documents: [sourceDocument, generatedDocument],
    eventIds: ["source-1", "index-1"],
    eventsCollection: "events",
    vectorCollection: "event_chunks",
    graphNodeEmbeddingCollection: "custom_graph_embeddings",
  });

  assert.deepEqual(result.eventIds, ["source-1", "index-1", "translation-1", "translation-2"]);
  assert.equal(result.deletedTotal, 11);
  assert.deepEqual(db.collections.get("events").finds, [{
    filter: {
      "extra.document_id": { $in: [sourceDocument, generatedDocument] },
    },
    options: { projection: { _id: 0, id: 1 } },
  }]);
  assert.deepEqual(db.collections.get("events").deletes, [{
    id: {
      $in: [
        "source-1",
        "index-1",
        "translation-1",
        "translation-2",
        "graph.node:derive:source-1",
        "graph.node:derive:index-1",
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
    options: { projection: { _id: 0, turn_id: 1 } },
  }]);
  assert.deepEqual(db.collections.get("knoxx_translation_candidate_sets").finds, [{
    filter: { turn_id: { $in: ["turn-1", "turn-2"] } },
    options: { projection: { _id: 0, candidate_set_id: 1 } },
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
    turn_id: { $in: ["turn-1", "turn-2"] },
  }]);
  assert.deepEqual(db.operations.at(-1), { type: "delete", collection: "events" });
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
