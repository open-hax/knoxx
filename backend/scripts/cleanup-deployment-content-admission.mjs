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
}) {
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
  if (scopedEventIds.length === 0) {
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

async function deleteScoped(collection, filter) {
  const result = await collection.deleteMany(filter);
  if (result?.acknowledged !== true
      || !Number.isSafeInteger(result.deletedCount)
      || result.deletedCount < 0) {
    throw new Error("cleanup delete was not durably acknowledged");
  }
  return result.deletedCount;
}

export async function cleanupDeploymentContentAdmission(db, requestedScope) {
  const scope = validateCleanupScope(requestedScope);
  const events = db.collection(scope.eventsCollection);
  const eventRows = await events.find(
    {
      "extra.document_id": { $in: scope.documents },
    },
    { projection: { _id: 0, id: 1 } },
  ).toArray();
  const eventIds = uniqueStrings(eventRows.map((row) => row.id));
  const ownedEventIds = new Set(eventIds);
  const unownedEventIds = scope.eventIds.filter((id) => !ownedEventIds.has(id));
  if (unownedEventIds.length > 0) {
    throw new Error(
      `cleanup event ids are not owned by the verifier documents: ${unownedEventIds.join(", ")}`,
    );
  }
  const graphEventIds = eventIds.map((id) => `graph.node:derive:${id}`);
  const graphIdentities = [...eventIds, ...graphEventIds];
  const documentKeywords = scope.documents.map((document) => `:${document}`);
  const turns = db.collection("knoxx_translation_turns");
  const turnRows = await turns.find(
    { document: { $in: documentKeywords } },
    { projection: { _id: 0, turn_id: 1 } },
  ).toArray();
  const turnIds = uniqueStrings(turnRows.map((row) => row.turn_id));
  const candidateSets = db.collection("knoxx_translation_candidate_sets");
  const candidateSetRows = await candidateSets.find(
    { turn_id: { $in: turnIds } },
    { projection: { _id: 0, candidate_set_id: 1 } },
  ).toArray();
  const candidateSetIds = uniqueStrings(
    candidateSetRows.map((row) => row.candidate_set_id),
  );

  const deleted = {};
  deleted.vectors = await deleteScoped(
    db.collection(scope.vectorCollection),
    { parent_id: { $in: eventIds } },
  );
  deleted.graphEdges = await deleteScoped(
    db.collection("graph_edges"),
    {
      $or: [
        { source_node_id: { $in: graphIdentities } },
        { target_node_id: { $in: graphIdentities } },
      ],
    },
  );
  deleted.dispatches = await deleteScoped(
    db.collection("knoxx_translation_dispatches"),
    { document_wire_id: { $in: scope.documents } },
  );
  deleted.reviews = await deleteScoped(
    db.collection("knoxx_translation_split_reviews"),
    { candidate_set_id: { $in: candidateSetIds } },
  );
  deleted.candidateSplits = await deleteScoped(
    db.collection("knoxx_translation_candidate_splits"),
    { turn_id: { $in: turnIds } },
  );
  deleted.candidateSets = await deleteScoped(
    candidateSets,
    { turn_id: { $in: turnIds } },
  );
  deleted.turns = await deleteScoped(
    turns,
    { turn_id: { $in: turnIds } },
  );
  deleted.receipts = await deleteScoped(
    db.collection("knoxx_translation_receipts"),
    { document: { $in: documentKeywords } },
  );
  deleted.approvals = await deleteScoped(
    db.collection("knoxx_translation_approvals"),
    { document: { $in: documentKeywords } },
  );
  deleted.graphNodeEmbeddings = await deleteScoped(
    db.collection(scope.graphNodeEmbeddingCollection),
    { source_event_id: { $in: eventIds } },
  );
  // Delete the ownership evidence last. If an earlier collection operation
  // fails, a retry can still re-authenticate every asserted event id before it
  // mutates anything else.
  deleted.events = await deleteScoped(events, { id: { $in: graphIdentities } });

  return {
    documents: scope.documents,
    eventIds,
    turnIds,
    candidateSetIds,
    deleted,
    deletedTotal: Object.values(deleted).reduce((sum, count) => sum + count, 0),
  };
}

async function main() {
  const { MongoClient } = await import("mongodb");
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
  });
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
    process.stdout.write(`${JSON.stringify(await cleanupDeploymentContentAdmission(db, scope))}\n`);
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
