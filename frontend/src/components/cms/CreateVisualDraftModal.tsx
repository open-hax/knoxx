import { useState, useCallback } from "react";
import {
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Input,
  Select,
} from "@open-hax/uxx";

export type CmsTemplateOption = {
  value: string;
  label: string;
};

interface CreateVisualDraftModalProps {
  open: boolean;
  onClose: () => void;
  onCreate: (params: { slug: string; title: string; template: string }) => void;
  templates: CmsTemplateOption[];
  isCreating?: boolean;
}

export function CreateVisualDraftModal({
  open,
  onClose,
  onCreate,
  templates,
  isCreating = false,
}: CreateVisualDraftModalProps) {
  const [slug, setSlug] = useState("");
  const [title, setTitle] = useState("");
  const [template, setTemplate] = useState(templates[0]?.value ?? "");
  const [error, setError] = useState<string | null>(null);

  const handleSlugChange = useCallback((value: string) => {
    // Auto-generate kebab-case slug from title if slug is empty
    setSlug(value);
    setError(null);
  }, []);

  const handleTitleChange = useCallback((value: string) => {
    setTitle(value);
    if (!slug) {
      setSlug(
        value
          .toLowerCase()
          .replace(/[^a-z0-9\s]/g, "")
          .replace(/\s+/g, "-")
          .slice(0, 60)
      );
    }
    setError(null);
  }, [slug]);

  const handleSubmit = useCallback(() => {
    const trimmedSlug = slug.trim();
    const trimmedTitle = title.trim();

    if (!trimmedSlug) {
      setError("Slug is required");
      return;
    }
    if (!/^[a-z0-9-]+$/.test(trimmedSlug)) {
      setError("Slug must be lowercase letters, numbers, and hyphens only");
      return;
    }
    if (!trimmedTitle) {
      setError("Title is required");
      return;
    }
    if (!template) {
      setError("Template is required");
      return;
    }

    onCreate({ slug: trimmedSlug, title: trimmedTitle, template });
  }, [slug, title, template, onCreate]);

  const handleClose = useCallback(() => {
    setSlug("");
    setTitle("");
    setTemplate(templates[0]?.value ?? "");
    setError(null);
    onClose();
  }, [onClose, templates]);

  return (
    <Modal open={open} onClose={handleClose} size="md">
      <ModalHeader>Create Visual Draft</ModalHeader>
      <ModalBody>
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div>
            <label
              style={{
                display: "block",
                fontSize: "13px",
                fontWeight: 500,
                marginBottom: "6px",
                color: "var(--token-colors-text-default)",
              }}
            >
              Page Title
            </label>
            <Input
              value={title}
              onChange={(e) => handleTitleChange(e.target.value)}
              placeholder="e.g., Error Coded Radio"
            />
          </div>

          <div>
            <label
              style={{
                display: "block",
                fontSize: "13px",
                fontWeight: 500,
                marginBottom: "6px",
                color: "var(--token-colors-text-default)",
              }}
            >
              URL Slug
            </label>
            <Input
              value={slug}
              onChange={(e) => handleSlugChange(e.target.value)}
              placeholder="e.g., error-coded-radio"
            />
            <div
              style={{
                fontSize: "11px",
                color: "var(--token-colors-text-muted)",
                marginTop: "4px",
              }}
            >
              Lowercase letters, numbers, and hyphens only.
            </div>
          </div>

          <div>
            <label
              style={{
                display: "block",
                fontSize: "13px",
                fontWeight: 500,
                marginBottom: "6px",
                color: "var(--token-colors-text-default)",
              }}
            >
              Template
            </label>
            <Select
              value={template}
              onChange={(e) => setTemplate(e.target.value)}
              options={templates}
            />
          </div>

          {error && (
            <div
              style={{
                padding: "8px 12px",
                background: "rgba(239, 68, 68, 0.1)",
                borderLeft: "3px solid #ef4444",
                borderRadius: "0 4px 4px 0",
                fontSize: "13px",
                color: "#fca5a5",
              }}
            >
              {error}
            </div>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button variant="ghost" onClick={handleClose} disabled={isCreating}>
          Cancel
        </Button>
        <Button variant="primary" onClick={handleSubmit} disabled={isCreating}>
          {isCreating ? "Creating…" : "Create Draft"}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
