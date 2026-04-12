/**
 * Content Editor Page
 *
 * Document authoring surface with title, body editor, and metadata fields.
 */

import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { DocumentFields } from "../components/editor/DocumentFields";
import { EmptyState } from "../components/EmptyState";
import {
  type EditorDocument,
  type DocumentStatus,
  type DocumentVisibility,
  STATUS_CONFIG,
  VISIBILITY_CONFIG,
  MOCK_COLLECTIONS,
  MOCK_DOCUMENT,
} from "../components/editor/editor-types";
import styles from "./ContentEditorPage.module.css";

export function ContentEditorPage() {
  const navigate = useNavigate();
  const [document, setDocument] = useState<EditorDocument>(MOCK_DOCUMENT);
  const [isDirty, setIsDirty] = useState(false);

  const handleTitleChange = useCallback((title: string) => {
    setDocument((prev) => ({ ...prev, title }));
    setIsDirty(true);
  }, []);

  const handleBodyChange = useCallback((body: string) => {
    setDocument((prev) => ({ ...prev, body }));
    setIsDirty(true);
  }, []);

  const handleCollectionChange = useCallback((collectionId: string) => {
    setDocument((prev) => ({ ...prev, collection_id: collectionId }));
    setIsDirty(true);
  }, []);

  const handleVisibilityChange = useCallback((visibility: DocumentVisibility) => {
    setDocument((prev) => ({ ...prev, visibility }));
    setIsDirty(true);
  }, []);

  const handleSave = useCallback(() => {
    // TODO: Implement save to API
    setIsDirty(false);
    console.log("Saving document:", document);
  }, [document]);

  const handlePublish = useCallback(() => {
    // TODO: Implement publish workflow
    if (document.status === "draft") {
      setDocument((prev) => ({ ...prev, status: "review" }));
    } else if (document.status === "review") {
      setDocument((prev) => ({ ...prev, status: "published" }));
    }
  }, [document.status]);

  // Show empty state if no document loaded
  if (!document) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="No document selected"
          message="Select a document from the content list to start editing."
          primaryAction={{
            label: "Browse content",
            onClick: () => navigate("/workbench/content"),
          }}
        />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.titleRow}>
          <input
            type="text"
            className={styles.titleInput}
            value={document.title}
            onChange={(e) => handleTitleChange(e.target.value)}
            placeholder="Untitled document"
          />
          {isDirty && <span className={styles.dirtyIndicator}>Unsaved changes</span>}
        </div>
        <div className={styles.actions}>
          <button
            className={styles.saveButton}
            onClick={handleSave}
            disabled={!isDirty}
          >
            Save
          </button>
          <button
            className={styles.publishButton}
            onClick={handlePublish}
            disabled={document.status === "published"}
          >
            {document.status === "draft"
              ? "Submit for Review"
              : document.status === "review"
                ? "Publish"
                : "Published"}
          </button>
        </div>
      </header>

      <div className={styles.editorLayout}>
        <main className={styles.bodyEditor}>
          <textarea
            className={styles.bodyTextarea}
            value={document.body}
            onChange={(e) => handleBodyChange(e.target.value)}
            placeholder="Start writing..."
          />
        </main>

        <aside className={styles.fieldsSidebar}>
          <DocumentFields
            collectionId={document.collection_id}
            collections={MOCK_COLLECTIONS}
            visibility={document.visibility}
            status={document.status}
            onCollectionChange={handleCollectionChange}
            onVisibilityChange={handleVisibilityChange}
            statusConfig={STATUS_CONFIG}
            visibilityConfig={VISIBILITY_CONFIG}
          />
        </aside>
      </div>
    </div>
  );
}
