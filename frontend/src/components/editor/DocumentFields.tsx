/**
 * Document Fields Component
 *
 * Structured fields panel for document metadata.
 */

import { useMemo } from "react";
import type {
  Collection,
  DocumentStatus,
  DocumentVisibility,
  STATUS_CONFIG,
  VISIBILITY_CONFIG,
} from "./editor-types";
import styles from "./DocumentFields.module.css";

interface DocumentFieldsProps {
  /** Selected collection ID */
  collectionId: string;
  /** Available collections */
  collections: Collection[];
  /** Current visibility */
  visibility: DocumentVisibility;
  /** Current status */
  status: DocumentStatus;
  /** Callback when collection changes */
  onCollectionChange: (collectionId: string) => void;
  /** Callback when visibility changes */
  onVisibilityChange: (visibility: DocumentVisibility) => void;
  /** Status config for display */
  statusConfig: typeof STATUS_CONFIG;
  /** Visibility config for display */
  visibilityConfig: typeof VISIBILITY_CONFIG;
}

export function DocumentFields({
  collectionId,
  collections,
  visibility,
  status,
  onCollectionChange,
  onVisibilityChange,
  statusConfig,
  visibilityConfig,
}: DocumentFieldsProps) {
  const currentStatus = statusConfig[status];
  const currentVisibility = visibilityConfig[visibility];
  const selectedCollection = useMemo(
    () => collections.find((c) => c.id === collectionId),
    [collections, collectionId]
  );

  return (
    <div className={styles.fieldsPanel}>
      <div className={styles.fieldGroup}>
        <label className={styles.fieldLabel}>Status</label>
        <div className={styles.statusBadge} style={{ color: currentStatus.color }}>
          {currentStatus.label}
        </div>
      </div>

      <div className={styles.fieldGroup}>
        <label className={styles.fieldLabel} htmlFor="collection-select">
          Collection
        </label>
        <select
          id="collection-select"
          className={styles.select}
          value={collectionId}
          onChange={(e) => onCollectionChange(e.target.value)}
        >
          {collections.map((collection) => (
            <option key={collection.id} value={collection.id}>
              {collection.name}
            </option>
          ))}
        </select>
        {selectedCollection?.description && (
          <span className={styles.fieldHint}>{selectedCollection.description}</span>
        )}
      </div>

      <div className={styles.fieldGroup}>
        <label className={styles.fieldLabel} htmlFor="visibility-select">
          Visibility
        </label>
        <select
          id="visibility-select"
          className={styles.select}
          value={visibility}
          onChange={(e) => onVisibilityChange(e.target.value as DocumentVisibility)}
        >
          {Object.entries(visibilityConfig).map(([key, config]) => (
            <option key={key} value={key}>
              {config.label}
            </option>
          ))}
        </select>
        <span className={styles.fieldHint}>{currentVisibility.description}</span>
      </div>
    </div>
  );
}
