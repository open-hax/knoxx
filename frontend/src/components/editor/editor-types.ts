/**
 * Content Editor Types
 *
 * Types for document editing, status, and visibility.
 */

/** Document status states */
export type DocumentStatus = "draft" | "review" | "published";

/** Document visibility levels */
export type DocumentVisibility = "private" | "internal" | "public";

/** Collection for grouping documents */
export interface Collection {
  id: string;
  name: string;
  description?: string;
}

/** Document being edited */
export interface EditorDocument {
  id: string;
  title: string;
  body: string;
  collection_id: string;
  visibility: DocumentVisibility;
  status: DocumentStatus;
  created_at: string;
  updated_at: string;
}

/** Editor state for tracking dirty/saving */
export interface EditorState {
  isDirty: boolean;
  isSaving: boolean;
  lastSaved?: string;
  error?: string;
}

/** Status configuration */
export const STATUS_CONFIG: Record<DocumentStatus, { label: string; color: string }> = {
  draft: { label: "Draft", color: "var(--token-colors-text-muted)" },
  review: { label: "In Review", color: "var(--token-colors-accent-amber)" },
  published: { label: "Published", color: "var(--token-colors-accent-green)" },
};

/** Visibility configuration */
export const VISIBILITY_CONFIG: Record<DocumentVisibility, { label: string; description: string }> = {
  private: { label: "Private", description: "Only you can see this" },
  internal: { label: "Internal", description: "Team members only" },
  public: { label: "Public", description: "Anyone with the link" },
};

/** Mock collections for development */
export const MOCK_COLLECTIONS: Collection[] = [
  { id: "col-1", name: "Documentation", description: "Technical docs and guides" },
  { id: "col-2", name: "Blog Posts", description: "Public-facing articles" },
  { id: "col-3", name: "Internal Notes", description: "Team knowledge base" },
];

/** Mock document for development */
export const MOCK_DOCUMENT: EditorDocument = {
  id: "doc-1",
  title: "Getting Started with Knowledge Ops",
  body: `# Getting Started with Knowledge Ops

This guide will help you understand the core concepts of the Knowledge Ops workbench.

## Key Concepts

- **Gardens**: Organized collections of documents
- **Documents**: Content items with metadata and provenance
- **Review Queue**: Items awaiting human validation
- **Memory**: Stigmergic signals for coordination

## Next Steps

1. Create a garden
2. Add documents
3. Configure review policies
`,
  collection_id: "col-1",
  visibility: "internal",
  status: "draft",
  created_at: "2024-01-15T10:00:00Z",
  updated_at: "2024-01-15T12:30:00Z",
};
