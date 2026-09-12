import { buildKnoxxAuthHeaders, request } from "./core";

/**
 * Translation pipeline configuration, resolved from Knoxx resources.
 *
 * `model` is a catalog model id spelled exactly as contracts/models/*.edn
 * spells it. `updated_at` is deliberately gone: it was an operational fact
 * from the legacy endpoint, and desired-state configuration does not carry
 * runtime timestamps.
 */
export type TranslationPipelineConfig = {
  model: string;
  "source-locale": string;
  "default-review": "required" | "none";
};

export async function getTranslationPipelineConfig(): Promise<TranslationPipelineConfig> {
  return await request<TranslationPipelineConfig>("/api/translations/config");
}

export async function updateTranslationPipelineConfig(model: string): Promise<TranslationPipelineConfig> {
  return await request<TranslationPipelineConfig>("/api/translations/config", {
    method: "PATCH",
    body: JSON.stringify({ model }),
  });
}

// Translation review wire contracts shared by the pipeline and review UI.
export type TranslationAdequacy = "excellent" | "good" | "adequate" | "poor" | "unusable";
export type TranslationFluency = "excellent" | "good" | "adequate" | "poor" | "unusable";
export type TranslationTerminology = "correct" | "minor_errors" | "major_errors";
export type TranslationRisk = "safe" | "sensitive" | "policy_violation";
export type TranslationOverall = "approve" | "needs_edit" | "reject";
export type TranslationStatus = "pending" | "in_review" | "approved" | "rejected";

export interface TranslationLabel {
  id: string;
  segment_id: string;
  labeler_id: string;
  labeler_email: string;
  adequacy: TranslationAdequacy;
  fluency: TranslationFluency;
  terminology: TranslationTerminology;
  risk: TranslationRisk;
  overall: TranslationOverall;
  corrected_text?: string | null;
  editor_notes?: string | null;
  ts: string;
}

export interface TranslationSegment {
  id: string;
  source_text: string;
  translated_text: string;
  source_lang: string;
  target_lang: string;
  status: TranslationStatus;
  confidence?: number | null;
  mt_model?: string | null;
  document_id: string;
  segment_index: number;
  domain?: string | null;
  garden_id?: string | null;
  tenant_id?: string | null;
  org_id?: string | null;
  labels?: TranslationLabel[];
  label_count?: number;
  ts?: string | null;
}

export interface TranslationSegmentListResponse {
  segments: TranslationSegment[];
  total: number;
  has_more: boolean;
}

export interface TranslationLabelPayload {
  adequacy: TranslationAdequacy;
  fluency: TranslationFluency;
  terminology: TranslationTerminology;
  risk: TranslationRisk;
  overall: TranslationOverall;
  project?: string;
  garden_id?: string;
  corrected_text?: string;
  editor_notes?: string;
}

export interface TranslationDocumentSummary {
  document_id: string;
  target_lang: string;
  source_lang: string;
  garden_id: string | null;
  project: string | null;
  title: string;
  document_status: string;
  total_segments: number;
  approved: number;
  pending: number;
  rejected: number;
  in_review: number;
  overall_status: "fully_approved" | "fully_rejected" | "pending_review" | "partial_review" | "mixed";
}

export interface TranslationDocumentDetail {
  document: {
    id: string;
    title: string;
    content: string;
    source_lang: string;
    visibility: string;
    source_path: string | null;
  };
  segments: TranslationSegment[];
  summary: {
    total_segments: number;
    approved: number;
    pending: number;
    rejected: number;
    in_review: number;
    overall_status: string;
  };
}

export interface TranslationBatchSummary {
  id: string;
  batch_id: string;
  garden_id: string;
  target_lang: string;
  source_lang: string;
  project: string;
  status: "queued" | "processing" | "complete" | "partial" | "failed";
  document_ids: string[];
  completed_documents: string[];
  failed_documents: { document_id: string; error: string }[];
  agent_session_id?: string;
  agent_conversation_id?: string;
  agent_run_id?: string;
  created_at: string;
  started_at?: string;
  completed_at?: string;
  error?: string;
}

export interface TranslationDocumentReviewPayload {
  overall: "approve" | "needs_edit" | "reject";
  project?: string;
  garden_id?: string;
  editor_notes?: string;
  segment_overrides?: Record<string, {
    overall: "approve" | "needs_edit" | "reject";
    corrected_text?: string;
    editor_notes?: string;
  }>;
}

export interface TranslationManifestLanguageStats {
  total_segments: number;
  approved: number;
  rejected: number;
  pending: number;
  in_review: number;
  avg_labels_per_segment: number;
  with_corrections: number;
}

export interface TranslationManifest {
  project: string;
  generated_at: string;
  languages: Record<string, TranslationManifestLanguageStats>;
  labelers: Array<{ email: string; segments_labeled: number }>;
  export_sizes: Record<string, { rows: number; bytes_estimate: number }>;
}

export async function listTranslationSegments(params: {
  project: string;
  status?: TranslationStatus | "all";
  target_lang?: string;
  source_lang?: string;
  domain?: string;
  limit?: number;
  offset?: number;
}): Promise<TranslationSegmentListResponse> {
  const query = new URLSearchParams({ project: params.project });
  if (params.status && params.status !== "all") query.set("status", params.status);
  if (params.target_lang) query.set("target_lang", params.target_lang);
  if (params.source_lang) query.set("source_lang", params.source_lang);
  if (params.domain) query.set("domain", params.domain);
  if (typeof params.limit === "number") query.set("limit", String(params.limit));
  if (typeof params.offset === "number") query.set("offset", String(params.offset));
  return request<TranslationSegmentListResponse>(`/api/translations/segments?${query.toString()}`);
}

export async function getTranslationSegment(segmentId: string): Promise<TranslationSegment> {
  return request<TranslationSegment>(`/api/translations/segments/${encodeURIComponent(segmentId)}`);
}

export async function submitTranslationLabel(segmentId: string, payload: TranslationLabelPayload): Promise<{ ok: boolean; label_id: string; new_status: TranslationStatus }> {
  return request<{ ok: boolean; label_id: string; new_status: TranslationStatus }>(`/api/translations/segments/${encodeURIComponent(segmentId)}/labels`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getTranslationManifest(project: string): Promise<TranslationManifest> {
  return request<TranslationManifest>(`/api/translations/export/manifest?project=${encodeURIComponent(project)}`);
}

export async function getTranslationSftExport(params: {
  project: string;
  targetLang?: string;
  includeCorrected?: boolean;
}): Promise<string> {
  const query = new URLSearchParams({ project: params.project });
  if (params.targetLang) query.set("target_lang", params.targetLang);
  if (typeof params.includeCorrected === "boolean") {
    query.set("include_corrected", String(params.includeCorrected));
  }
  const res = await fetch(`/api/translations/export/sft?${query.toString()}`, {
    headers: buildKnoxxAuthHeaders(),
  });
  if (!res.ok) {
    throw new Error(await res.text() || `Failed to export SFT: ${res.status}`);
  }
  return res.text();
}

export async function listTranslationDocuments(params: {
  project: string;
  target_lang?: string;
  source_lang?: string;
  garden_id?: string;
}): Promise<{ documents: TranslationDocumentSummary[]; total: number }> {
  const query = new URLSearchParams({ project: params.project });
  if (params.target_lang) query.set("target_lang", params.target_lang);
  if (params.source_lang) query.set("source_lang", params.source_lang);
  if (params.garden_id) query.set("garden_id", params.garden_id);
  return request<{ documents: TranslationDocumentSummary[]; total: number }>(`/api/translations/documents?${query.toString()}`);
}

export async function getTranslationDocument(documentId: string, targetLang: string): Promise<TranslationDocumentDetail> {
  return request<TranslationDocumentDetail>(`/api/translations/documents/${encodeURIComponent(documentId)}/${encodeURIComponent(targetLang)}`);
}

export async function reviewTranslationDocument(
  documentId: string,
  targetLang: string,
  payload: TranslationDocumentReviewPayload,
): Promise<{ ok: boolean; segments_reviewed: number; overall: string; overrides_applied: number }> {
  return request<{ ok: boolean; segments_reviewed: number; overall: string; overrides_applied: number }>(
    `/api/translations/documents/${encodeURIComponent(documentId)}/${encodeURIComponent(targetLang)}/review`,
    { method: "POST", body: JSON.stringify(payload) },
  );
}

export async function listTranslationBatches(params?: {
  status?: string;
  garden_id?: string;
  target_lang?: string;
}): Promise<{ batches: TranslationBatchSummary[] }> {
  const query = new URLSearchParams();
  if (params?.status) query.set("status", params.status);
  if (params?.garden_id) query.set("garden_id", params.garden_id);
  if (params?.target_lang) query.set("target_lang", params.target_lang);
  const qs = query.toString();
  return request<{ batches: TranslationBatchSummary[] }>(`/api/translations/batches${qs ? `?${qs}` : ""}`);
}

export async function createTranslationBatch(payload: {
  garden_id: string;
  target_lang: string;
  document_ids: string[];
  source_lang?: string;
  project?: string;
}): Promise<{ ok: boolean; batch_id: string; status: string; document_ids: string[] }> {
  return request<{ ok: boolean; batch_id: string; status: string; document_ids: string[] }>("/api/translations/batches", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
