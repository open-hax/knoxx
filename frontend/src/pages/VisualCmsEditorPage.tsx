import { useState, useCallback, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { VisualEditor } from "../components/cms/VisualEditor";
import { SourcePanel } from "../components/cms/SourcePanel";
import type { ViewContract, ViewSource } from "../lib/cms/viewContract";
import { validateContract } from "../lib/cms/validateContract";
import { parseEdn, serializeEdn } from "../lib/edn";
import styles from "./VisualCmsEditorPage.module.css";

// Fallback contract for loading state / errors
const FALLBACK_CONTRACT: ViewContract = {
  view_id: "untitled",
  view_title: "Untitled Draft",
  view_kind: "article-page",
  view_schema_version: 1,
  view_status: "draft",
  layout: {
    template: "article-page",
    zones: [
      { id: "header", label: "Header", accepts: ["hero", "image"], max_blocks: 1 },
      { id: "body", label: "Body", accepts: ["rich-text", "heading", "callout", "image", "cta", "divider"] },
    ],
  },
  blocks: [],
  editor: {
    locked: false,
    allowed_actions: ["drag", "drop", "duplicate", "hide", "delete", "edit-props"],
    default_panel: "layout",
  },
  publishing: {
    defer_index: false,
    skip_translation: true,
  },
  settings: {
    language: "en",
    allow_comments: true,
  },
};

async function loadDraftContract(draftPath: string): Promise<{ contract: ViewContract; contentMd?: string; assets?: string[] }> {
  // Fetch view-contract.edn from the backend
  const contractResp = await fetch(`/api/ingestion/file?path=${encodeURIComponent(`${draftPath}/view-contract.edn`)}`);
  if (!contractResp.ok) {
    throw new Error(`Failed to load view-contract.edn: ${contractResp.status}`);
  }

  const contractText = (await contractResp.json()).content as string;
  // Parse EDN-like structure (simplified: we expect the backend to eventually serve JSON)
  // For now, we'll use a simple heuristic or require the backend to convert
  const contract = parseViewContract(contractText);

  // Try to load content.md
  let contentMd: string | undefined;
  try {
    const contentResp = await fetch(`/api/ingestion/file?path=${encodeURIComponent(`${draftPath}/content.md`)}`);
    if (contentResp.ok) {
      contentMd = (await contentResp.json()).content as string;
    }
  } catch {
    // content.md is optional
  }

  return { contract, contentMd };
}

// Parse view-contract.edn into ViewContract
function parseViewContract(ednText: string): ViewContract {
  const raw = parseEdn(ednText) as Record<string, unknown>;
  
  const extractKeyword = (val: unknown): string => {
    if (typeof val === "string" && val.startsWith(":")) return val.slice(1);
    return String(val);
  };

  const parseBlocks = (blocks: unknown[]): ViewContract["blocks"] => {
    return blocks.map((b) => {
      const block = b as Record<string, unknown>;
      return {
        id: String(block.id ?? ""),
        type: extractKeyword(block.type),
        zone: extractKeyword(block.zone),
        props: (block.props as Record<string, unknown>) ?? {},
        source: block.source ? {
          kind: extractKeyword((block.source as Record<string, unknown>).kind) as "file",
          path: String((block.source as Record<string, unknown>).path ?? ""),
        } : undefined,
        hidden: block.hidden === true,
      };
    }).filter((b) => b.id && b.type);
  };

  const parseZones = (zones: unknown[]): ViewContract["layout"]["zones"] => {
    return zones.map((z) => {
      const zone = z as Record<string, unknown>;
      return {
        id: extractKeyword(zone.id),
        label: String(zone.label ?? ""),
        accepts: Array.isArray(zone.accepts) 
          ? zone.accepts.map((a) => extractKeyword(a))
          : [],
        max_blocks: zone.max_blocks === null ? undefined : (zone.max_blocks as number | undefined),
      };
    });
  };

  return {
    view_id: String(raw.view_id ?? raw["view/id"] ?? "untitled"),
    view_title: String(raw.view_title ?? raw["view/title"] ?? "Untitled"),
    view_kind: (extractKeyword(raw.view_kind ?? raw["view/kind"]) ?? "article-page") as ViewContract["view_kind"],
    view_schema_version: Number(raw.view_schema_version ?? raw["view/schema_version"] ?? 1),
    view_status: (extractKeyword(raw.view_status ?? raw["view/status"]) ?? "draft") as ViewContract["view_status"],
    source: raw.source ? (() => {
      const src = raw.source! as Record<string, unknown>;
      return {
        kind: extractKeyword(src.kind) as ViewSource["kind"],
        path: String(src.path ?? ""),
        imported_at: String(src.imported_at ?? ""),
      };
    })() : undefined,
    garden: raw.garden ? {
      id: String((raw.garden as Record<string, unknown>).id ?? ""),
      visibility: extractKeyword((raw.garden as Record<string, unknown>).visibility) as ViewContract["view_status"],
    } : undefined,
    layout: {
      template: extractKeyword((raw.layout as Record<string, unknown>)?.template ?? "article-page"),
      zones: parseZones(((raw.layout as Record<string, unknown>)?.zones as unknown[]) ?? []),
    },
    blocks: parseBlocks((raw.blocks as unknown[]) ?? []),
    editor: {
      locked: (raw.editor as Record<string, unknown>)?.locked === true,
      allowed_actions: Array.isArray((raw.editor as Record<string, unknown>)?.allowed_actions)
        ? ((raw.editor as Record<string, unknown>).allowed_actions as unknown[]).map(String)
        : ["drag", "drop", "duplicate", "hide", "delete", "edit-props"],
      default_panel: String((raw.editor as Record<string, unknown>)?.default_panel ?? "layout"),
    },
    publishing: {
      defer_index: ((raw.publishing as Record<string, unknown>)?.defer_index ?? false) === true,
      skip_translation: ((raw.publishing as Record<string, unknown>)?.skip_translation ?? true) === true,
      last_published_at: (raw.publishing as Record<string, unknown>)?.last_published_at as string | null | undefined,
      canonical_url: (raw.publishing as Record<string, unknown>)?.canonical_url as string | null | undefined,
    },
    settings: {
      language: String((raw.settings as Record<string, unknown>)?.language ?? "en"),
      allow_comments: ((raw.settings as Record<string, unknown>)?.allow_comments ?? true) === true,
      chat_actor_id: (raw.settings as Record<string, unknown>)?.chat_actor_id as string | null | undefined,
    },
  };
}

export function VisualCmsEditorPage() {
  const { '*': draftPath } = useParams();
  const navigate = useNavigate();
  const [contract, setContract] = useState<ViewContract>(FALLBACK_CONTRACT);
  const [contentMd, setContentMd] = useState<string | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!draftPath) {
      setLoading(false);
      setError("No draft path specified");
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    loadDraftContract(draftPath)
      .then(({ contract, contentMd }) => {
        if (cancelled) return;
        setContract(contract);
        setContentMd(contentMd);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [draftPath]);

  const handleContractChange = useCallback((next: ViewContract) => {
    setContract(next);
    setIsDirty(true);
  }, []);

  const handleSave = useCallback(async () => {
    if (!draftPath || !isDirty) return;
    setSaving(true);
    try {
      // Save view-contract.edn
      const contractPath = `${draftPath}/view-contract.edn`;
      // Convert contract to EDN string
      const ednBody = serializeViewContract(contract);
      const resp = await fetch("/api/ingestion/file", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path: contractPath, content: ednBody }),
      });
      if (!resp.ok) throw new Error(await resp.text());
      setIsDirty(false);
    } catch (err) {
      console.error("Save failed:", err);
    } finally {
      setSaving(false);
    }
  }, [draftPath, contract, isDirty]);

  const validationErrors = validateContract(contract);

  if (loading) {
    return (
      <div className={styles.page}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100%", color: "var(--token-colors-text-muted)" }}>
          Loading draft…
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.page}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100%", color: "#ef4444" }}>
          Error: {error}
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.titleRow}>
          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <button
              onClick={() => navigate("/cms")}
              style={{ background: "none", border: "none", color: "var(--token-colors-text-muted)", cursor: "pointer", fontSize: "18px" }}
              title="Back to CMS"
            >
              ←
            </button>
            <span style={{ fontWeight: 600, fontSize: "16px" }}>{contract.view_title}</span>
            <span style={{ fontSize: "12px", color: "var(--token-colors-text-muted)", padding: "2px 8px", background: "rgba(255,255,255,0.05)", borderRadius: "4px" }}>
              {contract.view_status}
            </span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            {isDirty && <span style={{ fontSize: "12px", color: "#f59e0b" }}>Unsaved changes</span>}
            <button
              className={styles.saveButton}
              onClick={() => void handleSave()}
              disabled={saving || !isDirty}
            >
              {saving ? "Saving…" : "Save"}
            </button>
          </div>
        </div>
      </header>

      <div className={styles.splitPanel}>
        <div className={styles.canvasPanel}>
          <VisualEditor contract={contract} onChange={handleContractChange} />
        </div>
        <div className={styles.sourcePanel}>
          <SourcePanel
            contract={contract}
            contentMd={contentMd}
            validationErrors={validationErrors}
          />
        </div>
      </div>
    </div>
  );
}

function serializeViewContract(contract: ViewContract): string {
  // Convert snake_case keys to kebab-case for EDN output
  const toEdnKey = (k: string): string => k.replace(/_/g, "-");
  
  const transformKeys = (obj: unknown): unknown => {
    if (obj === null || obj === undefined) return obj;
    if (typeof obj !== "object") return obj;
    if (Array.isArray(obj)) return obj.map(transformKeys);
    const result: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(obj)) {
      result[toEdnKey(k)] = transformKeys(v);
    }
    return result;
  };

  return serializeEdn(transformKeys(contract));
}
