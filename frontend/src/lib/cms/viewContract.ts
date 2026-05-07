/**
 * View Contract types for the folder-backed visual CMS.
 *
 * view-contract.edn is the source of truth; these TypeScript types
 * mirror its schema for frontend type safety.
 */

import type { PublicationBlock } from "@open-hax/garden-publication-components";

export type ViewStatus = "draft" | "review" | "public" | "archived";
export type ViewKind = "article-page" | "playlist-page" | "landing-page" | "collection-page";

export interface ViewSource {
  kind: "manual" | "markdown-import" | "broadcast-studio" | "ai-drafted";
  path?: string;
  imported_at?: string;
}

export interface ViewGarden {
  id: string;
  visibility: ViewStatus;
}

export interface ViewZone {
  id: string;
  label: string;
  accepts: string[];
  max_blocks?: number | null;
}

export interface ViewLayout {
  template: string;
  zones: ViewZone[];
}

export interface ViewBlockSource {
  kind: "file";
  path: string;
}

export interface ViewBlock {
  id: string;
  type: string;
  zone: string;
  props?: Record<string, unknown>;
  source?: ViewBlockSource;
  hidden?: boolean;
}

export interface ViewEditorPolicy {
  locked: boolean;
  allowed_actions: string[];
  default_panel: string;
}

export interface ViewSeo {
  title?: string | null;
  description?: string | null;
  og_image?: string | null;
}

export interface ViewPublishing {
  last_published_at?: string | null;
  defer_index: boolean;
  skip_translation: boolean;
  canonical_url?: string | null;
  seo?: ViewSeo;
}

export interface ViewSettings {
  language: string;
  allow_comments: boolean;
  chat_actor_id?: string | null;
}

export interface ViewContract {
  view_id: string;
  view_title: string;
  view_kind: ViewKind;
  view_schema_version: number;
  view_status: ViewStatus;
  source?: ViewSource;
  garden?: ViewGarden;
  layout: ViewLayout;
  blocks: ViewBlock[];
  editor: ViewEditorPolicy;
  publishing: ViewPublishing;
  settings: ViewSettings;
}

export interface BlockRegistryEntry {
  label: string;
  category: string;
  icon: string;
  props_schema: Record<string, PropSchema>;
}

export interface PropSchema {
  type: "string" | "markdown" | "boolean" | "enum" | "asset" | "audio" | "audio-list" | "track-list" | "url";
  required?: boolean;
  default?: unknown;
  values?: unknown[];
}

export interface CmsTemplate {
  label: string;
  zones: ViewZone[];
}

export type BlockRegistry = Record<string, BlockRegistryEntry>;
export type TemplateRegistry = Record<string, CmsTemplate>;
