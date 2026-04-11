export interface Source {
  source_id: string;
  tenant_id: string;
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  state: Record<string, unknown>;
  collections?: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
  last_scan_at: string | null;
  last_error: string | null;
  enabled: boolean;
  created_at: string;
  updated_at: string;
}

export interface SourceAudit {
  source_id: string;
  tenant_id: string;
  driver_type: string;
  root_path: string | null;
  collections: string[];
  file_types: string[];
  include_patterns: string[];
  exclude_patterns: string[];
  matching_files: number;
  new_files: number;
  changed_files: number;
  unchanged_files: number;
  skipped_files: number;
  state_ingested_files: number;
  state_failed_files: number;
  openplanner_documents: number;
  coverage_delta: number;
}

export interface Job {
  job_id: string;
  source_id: string;
  tenant_id: string;
  status: "pending" | "running" | "completed" | "failed" | "cancelled";
  total_files: number;
  processed_files: number;
  failed_files: number;
  skipped_files: number;
  chunks_created: number;
  started_at: string | null;
  completed_at: string | null;
  error_message: string | null;
  created_at: string;
}

export interface ProgressEvent {
  type: string;
  job_id: string;
  timestamp: string;
  total_files?: number;
  processed_files?: number;
  failed_files?: number;
  chunks_created?: number;
  percent_complete?: number;
  file_id?: string;
  file_path?: string;
  file_status?: "success" | "failed" | "skipped";
  file_chunks?: number;
  file_error?: string;
  status?: string;
  error_message?: string;
  duration_seconds?: number;
}

export interface CreateSourceForm {
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  collections: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
}
