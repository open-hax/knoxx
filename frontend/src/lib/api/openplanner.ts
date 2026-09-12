import { request } from "./core";

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
