import { request } from "./core";

/**
 * Resource-backed CMS publication surface.
 *
 * The wire is already normalized JSON scalars, so TypeScript works with the wire
 * values directly — there is no second decoding layer here to drift from the
 * backend contract. Qualified ids arrive as `"namespace/name"` with no leading
 * colon, so two documents differing only by namespace never collapse.
 *
 * `desired` is contract intent, from the resource graph. `observed` is runtime
 * evidence and may legitimately disagree — that disagreement is drift, and the
 * UI must be able to show both rather than pretending one is the truth.
 */

export type PublicationDesiredState = "published" | "withheld" | "archived";

export type PublicationWire = {
  id: string;
  document: string;
  garden: string;
  locale: string;
  revision: string;
  path: string;
  desired: PublicationDesiredState;
  observed: string | null;
  blockers: string[];
};

export type DocumentWire = {
  id: string;
  title: string;
  "source-locale": string;
  source: { path: string };
};

export type GardenWire = {
  id: string;
  title: string;
  status: "active" | "archived";
};

export type CmsDocumentWire = {
  document: DocumentWire;
  publications: PublicationWire[];
};

export type CmsListWire = {
  documents: CmsDocumentWire[];
  gardens: GardenWire[];
};

export async function listPublicationTopology(): Promise<CmsListWire> {
  return await request<CmsListWire>("/api/cms/publications/documents");
}

/**
 * Change ONLY a publication's desired state.
 *
 * The body key is `state`, unqualified, matching the backend's
 * `PublicationStatePatchJson`. Publication identity — document, garden, locale,
 * revision — cannot move through this endpoint.
 */
export async function setPublicationState(
  publicationId: string,
  state: PublicationDesiredState,
): Promise<PublicationWire> {
  return await request<PublicationWire>(
    `/api/cms/publications/intents/${encodeURIComponent(publicationId)}`,
    { method: "PATCH", body: JSON.stringify({ state }) },
  );
}

/**
 * Gardens a document is published to, derived from `desired` state.
 *
 * Deliberately a derivation rather than stored state: a second client-side
 * authority is exactly what let the old CMS disagree with the resource graph.
 */
export function publishedGardenIdsFor(document: CmsDocumentWire | null): string[] {
  if (!document) return [];
  return document.publications
    .filter((publication) => publication.desired === "published")
    .map((publication) => publication.garden);
}

/** The publication targeting one garden for a document, if any. */
export function publicationForGarden(
  document: CmsDocumentWire | null,
  gardenId: string,
): PublicationWire | null {
  if (!document) return null;
  return document.publications.find((publication) => publication.garden === gardenId) ?? null;
}

/** Desired publication is not reflected by observed evidence. */
export function isDrifted(publication: PublicationWire): boolean {
  return publication.desired === "published"
    ? publication.observed === null
    : publication.observed !== null;
}

export function findDocumentBySourcePath(
  topology: CmsListWire | null,
  sourcePath: string,
): CmsDocumentWire | null {
  if (!topology) return null;
  const normalize = (value: string) => value.replace(/^\/+/, "");
  const wanted = normalize(sourcePath);
  return (
    topology.documents.find(
      (candidate) => normalize(candidate.document.source.path) === wanted,
    ) ?? null
  );
}
