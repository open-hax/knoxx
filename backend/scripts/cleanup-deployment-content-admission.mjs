import { pathToFileURL } from "node:url";

const SOURCE_DOCUMENT_PATTERN = /^knoxx\.verifyadmission\/probe[0-9]+$/;
const GENERATED_DOCUMENT_PATTERN = /^knoxx\.generated\/post-[0-9a-f]{24}$/;
const COLLECTION_PATTERN = /^[A-Za-z][A-Za-z0-9_.-]{0,127}$/;

function uniqueStrings(values) {
  return [...new Set(values.filter((value) => typeof value === "string" && value.length > 0))];
}

function parseJsonArray(name, raw) {
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    throw new Error(`${name} must be valid JSON: ${error.message}`);
  }
  if (!Array.isArray(parsed)) {
    throw new Error(`${name} must be a JSON array`);
  }
  return parsed;
}

export function validateCleanupScope({
  documents,
  eventIds,
  eventsCollection,
  vectorCollection,
  graphNodeEmbeddingCollection,
}, { requireEventIds = true } = {}) {
  if (!Array.isArray(documents) || !Array.isArray(eventIds)) {
    throw new Error("cleanup documents and event ids must be arrays");
  }
  const scopedDocuments = uniqueStrings(documents);
  const scopedEventIds = uniqueStrings(eventIds);

  if (scopedDocuments.length !== documents.length || scopedEventIds.length !== eventIds.length) {
    throw new Error("cleanup identities must be unique non-empty strings");
  }
  if (scopedDocuments.length < 1 || scopedDocuments.length > 2) {
    throw new Error("cleanup requires one source document and at most one generated document");
  }
  const sourceDocuments = scopedDocuments.filter((document) => SOURCE_DOCUMENT_PATTERN.test(document));
  const generatedDocuments = scopedDocuments.filter((document) => GENERATED_DOCUMENT_PATTERN.test(document));
  if (sourceDocuments.length !== 1 || sourceDocuments.length + generatedDocuments.length !== scopedDocuments.length) {
    throw new Error("cleanup documents are outside the verifier namespaces");
  }
  if (generatedDocuments.length > 1) {
    throw new Error("cleanup accepts at most one generated verifier document");
  }
  if (requireEventIds && scopedEventIds.length === 0) {
    throw new Error("cleanup requires at least one exact admission event id");
  }
  for (const [name, value] of [
    ["events collection", eventsCollection],
    ["vector collection", vectorCollection],
    ["graph-node embedding collection", graphNodeEmbeddingCollection],
  ]) {
    if (typeof value !== "string" || !COLLECTION_PATTERN.test(value)) {
      throw new Error(`${name} is invalid`);
    }
  }

  return {
    documents: [...sourceDocuments, ...generatedDocuments],
    eventIds: scopedEventIds,
    eventsCollection,
    vectorCollection,
    graphNodeEmbeddingCollection,
  };
}

function sortedUniqueStrings(values) {
  return uniqueStrings(values).sort();
}

export async function inspectDeploymentContentAdmission(db, requestedScope) {
  const scope = validateCleanupScope(requestedScope, { requireEventIds: false });
  const events = db.collection(scope.eventsCollection);
  const eventRows = await events.find(
    {
      "extra.document_id": { $in: scope.documents },
    },
    { projection: { _id: 0, id: 1, kind: 1, "extra.document_id": 1 } },
  ).toArray();
  if (eventRows.some((row) => typeof row.id !== "string" || row.id.length === 0)) {
    throw new Error("cleanup discovered a verifier event without an exact id");
  }
  const eventIds = sortedUniqueStrings(eventRows.map((row) => row.id));
  const ownedEventIds = new Set(eventIds);
  const unownedEventIds = scope.eventIds.filter((id) => !ownedEventIds.has(id));
  if (unownedEventIds.length > 0) {
    throw new Error(
      `cleanup event ids are not owned by the verifier documents: ${unownedEventIds.join(", ")}`,
    );
  }

  const documentKeywords = scope.documents.map((document) => `:${document}`);
  const turns = db.collection("knoxx_translation_turns");
  const turnRows = await turns.find(
    { document: { $in: documentKeywords } },
    { projection: { _id: 0, turn_id: 1, document: 1, candidate_revision: 1 } },
  ).toArray();
  const turnIds = sortedUniqueStrings(turnRows.map((row) => row.turn_id));
  const candidateSets = db.collection("knoxx_translation_candidate_sets");
  const candidateSetRows = await candidateSets.find(
    { turn_id: { $in: turnIds } },
    { projection: { _id: 0, candidate_set_id: 1, turn_id: 1, candidate_revision: 1 } },
  ).toArray();
  const candidateSetIds = sortedUniqueStrings(
    candidateSetRows.map((row) => row.candidate_set_id),
  );
  const dispatchRows = await db.collection("knoxx_translation_dispatches").find(
    { document_wire_id: { $in: scope.documents } },
    { projection: { _id: 0, document_wire_id: 1, outcome: 1, batch_id: 1 } },
  ).toArray();
  if (dispatchRows.some((row) => !scope.documents.includes(row.document_wire_id))) {
    throw new Error("cleanup discovered a translation dispatch outside its documents");
  }
  const dispatches = dispatchRows.map((row) => ({
    document: row.document_wire_id,
    outcome: row.outcome,
    batchId: row.batch_id ?? null,
  })).sort((left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right)));
  const ownerEventIds = sortedUniqueStrings([
    ...eventRows
      .filter((row) => row.kind === "publication.document.indexed")
      .map((row) => row.id),
    ...dispatchRows
      .filter((row) => typeof row.batch_id === "string" && row.batch_id.length > 0)
      .map((row) => `translation-needed-${row.batch_id}`),
  ]);
  const candidateRevisions = sortedUniqueStrings([
    ...turnRows.map((row) => row.candidate_revision),
    ...candidateSetRows.map((row) => row.candidate_revision),
  ]);

  return {
    documents: scope.documents,
    eventIds,
    ownerEventIds,
    dispatches,
    turnIds,
    candidateSetIds,
    candidateRevisions,
  };
}

async function deleteScoped(collection, filter) {
  const result = await collection.deleteMany(filter);
  if (result?.acknowledged !== true
      || !Number.isSafeInteger(result.deletedCount)
      || result.deletedCount < 0) {
    throw new Error("cleanup delete was not durably acknowledged");
  }
  return result.deletedCount;
}

async function countScoped(collection, filter) {
  const count = await collection.countDocuments(filter);
  if (!Number.isSafeInteger(count) || count < 0) {
    throw new Error("cleanup residue count was not a safe integer");
  }
  return count;
}

async function deleteScopedAndVerify(collection, deleteFilter, residueFilter, label) {
  const deleted = await deleteScoped(collection, deleteFilter);
  const residue = await countScoped(collection, residueFilter);
  if (residue !== 0) {
    throw new Error(`cleanup left run-owned residue: ${label}=${residue}`);
  }
  return { deleted, residue };
}

export async function cleanupDeploymentContentAdmission(db, requestedScope) {
  const scope = validateCleanupScope(requestedScope);
  const inspection = await inspectDeploymentContentAdmission(db, scope);
  const events = db.collection(scope.eventsCollection);
  const eventIds = inspection.eventIds;
  const graphEventIds = eventIds.map((id) => `graph.node:derive:${id}`);
  const graphIdentities = [...eventIds, ...graphEventIds];
  const documentKeywords = scope.documents.map((document) => `:${document}`);
  const turns = db.collection("knoxx_translation_turns");
  const turnIds = inspection.turnIds;
  const candidateSets = db.collection("knoxx_translation_candidate_sets");
  const candidateSetIds = inspection.candidateSetIds;

  const deleted = {};
  const residue = {};
  let result = await deleteScopedAndVerify(
    db.collection(scope.vectorCollection),
    { parent_id: { $in: eventIds } },
    { parent_id: { $in: eventIds } },
    "vectors",
  );
  deleted.vectors = result.deleted;
  residue.vectors = result.residue;

  const graphEdgeFilter = {
    $or: [
      { source_node_id: { $in: graphIdentities } },
      { target_node_id: { $in: graphIdentities } },
    ],
  };
  result = await deleteScopedAndVerify(
    db.collection("graph_edges"),
    graphEdgeFilter,
    graphEdgeFilter,
    "graphEdges",
  );
  deleted.graphEdges = result.deleted;
  residue.graphEdges = result.residue;

  const dispatchFilter = { document_wire_id: { $in: scope.documents } };
  result = await deleteScopedAndVerify(
    db.collection("knoxx_translation_dispatches"),
    dispatchFilter,
    dispatchFilter,
    "dispatches",
  );
  deleted.dispatches = result.deleted;
  residue.dispatches = result.residue;

  // Verify each child collection before deleting the rows that authenticate
  // its derived selector. That keeps a legacy/schema-mismatched child
  // discoverable on the next exact retry.
  const reviewFilter = { candidate_set_id: { $in: candidateSetIds } };
  result = await deleteScopedAndVerify(
    db.collection("knoxx_translation_split_reviews"),
    reviewFilter,
    reviewFilter,
    "reviews",
  );
  deleted.reviews = result.deleted;
  residue.reviews = result.residue;

  const candidateSplitFilter = { turn_id: { $in: turnIds } };
  result = await deleteScopedAndVerify(
    db.collection("knoxx_translation_candidate_splits"),
    candidateSplitFilter,
    candidateSplitFilter,
    "candidateSplits",
  );
  deleted.candidateSplits = result.deleted;
  residue.candidateSplits = result.residue;

  const candidateSetFilter = { turn_id: { $in: turnIds } };
  result = await deleteScopedAndVerify(
    candidateSets,
    candidateSetFilter,
    candidateSetFilter,
    "candidateSets",
  );
  deleted.candidateSets = result.deleted;
  residue.candidateSets = result.residue;

  const turnFilter = { document: { $in: documentKeywords } };
  result = await deleteScopedAndVerify(
    turns,
    turnFilter,
    turnFilter,
    "turns",
  );
  deleted.turns = result.deleted;
  residue.turns = result.residue;

  const receiptFilter = { document: { $in: documentKeywords } };
  result = await deleteScopedAndVerify(
    db.collection("knoxx_translation_receipts"),
    receiptFilter,
    receiptFilter,
    "receipts",
  );
  deleted.receipts = result.deleted;
  residue.receipts = result.residue;

  const approvalFilter = { document: { $in: documentKeywords } };
  result = await deleteScopedAndVerify(
    db.collection("knoxx_translation_approvals"),
    approvalFilter,
    approvalFilter,
    "approvals",
  );
  deleted.approvals = result.deleted;
  residue.approvals = result.residue;

  const graphNodeEmbeddingFilter = { source_event_id: { $in: eventIds } };
  result = await deleteScopedAndVerify(
    db.collection(scope.graphNodeEmbeddingCollection),
    graphNodeEmbeddingFilter,
    graphNodeEmbeddingFilter,
    "graphNodeEmbeddings",
  );
  deleted.graphNodeEmbeddings = result.deleted;
  residue.graphNodeEmbeddings = result.residue;

  // Delete the ownership evidence last. Every dependent delete was already
  // acknowledged and verified empty, so no derived selector is lost early.
  const eventDeleteFilter = { id: { $in: graphIdentities } };
  const eventResidueFilter = {
    $or: [
      { "extra.document_id": { $in: scope.documents } },
      { id: { $in: graphIdentities } },
    ],
  };
  result = await deleteScopedAndVerify(
    events,
    eventDeleteFilter,
    eventResidueFilter,
    "events",
  );
  deleted.events = result.deleted;
  residue.events = result.residue;

  return {
    ...inspection,
    deleted,
    residue,
    deletedTotal: Object.values(deleted).reduce((sum, count) => sum + count, 0),
  };
}

async function main() {
  const { MongoClient } = await import("mongodb");
  const mode = process.env.KNOXX_VERIFY_CLEANUP_MODE ?? "delete";
  if (!new Set(["inspect", "delete"]).has(mode)) {
    throw new Error("KNOXX_VERIFY_CLEANUP_MODE must be inspect or delete");
  }
  const documents = parseJsonArray(
    "KNOXX_VERIFY_CLEANUP_DOCUMENTS_JSON",
    process.env.KNOXX_VERIFY_CLEANUP_DOCUMENTS_JSON ?? "",
  );
  const eventIds = parseJsonArray(
    "KNOXX_VERIFY_CLEANUP_EVENT_IDS_JSON",
    process.env.KNOXX_VERIFY_CLEANUP_EVENT_IDS_JSON ?? "",
  );
  const scope = validateCleanupScope({
    documents,
    eventIds,
    eventsCollection: process.env.MONGODB_EVENTS_COLLECTION ?? "events",
    vectorCollection: process.env.MONGODB_VECTOR_HOT_COLLECTION ?? "event_chunks",
    graphNodeEmbeddingCollection:
      process.env.MONGODB_GRAPH_NODE_EMBEDDING_COLLECTION ?? "graph_node_embeddings",
  }, { requireEventIds: mode === "delete" });
  const client = new MongoClient(
    process.env.MONGODB_URI
      || process.env.OPENPLANNER_MONGODB_URI
      || "mongodb://localhost:27017",
    { serverSelectionTimeoutMS: 5000 },
  );

  try {
    await client.connect();
    const db = client.db(
      process.env.MONGODB_DB
        || process.env.OPENPLANNER_MONGODB_DB
        || "openplanner",
    );
    const result = mode === "inspect"
      ? await inspectDeploymentContentAdmission(db, scope)
      : await cleanupDeploymentContentAdmission(db, scope);
    process.stdout.write(`${JSON.stringify(result)}\n`);
  } finally {
    await client.close();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((error) => {
    process.stderr.write(`${error.stack ?? error.message}\n`);
    process.exitCode = 1;
  });
}
