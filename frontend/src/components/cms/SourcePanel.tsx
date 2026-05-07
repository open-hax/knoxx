import { useState } from "react";
import type { ViewContract } from "../../lib/cms/viewContract";

interface SourcePanelProps {
  contract: ViewContract;
  contentMd?: string;
  assets?: string[];
  validationErrors?: string[];
}

export function SourcePanel({ contract, contentMd, assets, validationErrors }: SourcePanelProps) {
  const [activeTab, setActiveTab] = useState<"content" | "contract" | "assets" | "validation">("contract");

  const formatEdn = (obj: unknown): string => {
    return JSON.stringify(obj, null, 2)
      .replace(/"([^"]+)":/g, ":$1")
      .replace(/true/g, "true")
      .replace(/false/g, "false")
      .replace(/null/g, "nil");
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%", background: "var(--token-colors-background-surface)", borderTop: "1px solid var(--token-colors-border-default)" }}>
      <div style={{ display: "flex", gap: 0, borderBottom: "1px solid var(--token-colors-border-default)" }}>
        {(["contract", "content", "assets", "validation"] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              padding: "8px 16px",
              background: activeTab === tab ? "var(--token-colors-background-surface)" : "transparent",
              border: "none",
              borderBottom: activeTab === tab ? "2px solid #3b82f6" : "2px solid transparent",
              color: activeTab === tab ? "#fff" : "var(--token-colors-text-muted)",
              cursor: "pointer",
              fontSize: "13px",
              fontWeight: 500,
              textTransform: "capitalize",
            }}
          >
            {tab}
            {tab === "validation" && validationErrors && validationErrors.length > 0 && (
              <span style={{ marginLeft: "4px", color: "#ef4444" }}>({validationErrors.length})</span>
            )}
          </button>
        ))}
      </div>

      <div style={{ flex: 1, overflow: "auto", padding: "12px" }}>
        {activeTab === "contract" && (
          <pre style={{ margin: 0, fontSize: "12px", lineHeight: 1.5, color: "#d0d0d0", whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
            {formatEdn(contract)}
          </pre>
        )}

        {activeTab === "content" && (
          <pre style={{ margin: 0, fontSize: "12px", lineHeight: 1.5, color: "#d0d0d0", whiteSpace: "pre-wrap" }}>
            {contentMd || "No content.md file"}
          </pre>
        )}

        {activeTab === "assets" && (
          <div>
            {assets && assets.length > 0 ? (
              <ul style={{ margin: 0, padding: "0 0 0 16px", fontSize: "12px", color: "#d0d0d0" }}>
                {assets.map((asset) => (
                  <li key={asset} style={{ padding: "4px 0" }}>{asset}</li>
                ))}
              </ul>
            ) : (
              <div style={{ color: "var(--token-colors-text-muted)", fontSize: "12px" }}>No assets in this draft.</div>
            )}
          </div>
        )}

        {activeTab === "validation" && (
          <div>
            {validationErrors && validationErrors.length > 0 ? (
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {validationErrors.map((err, i) => (
                  <div key={i} style={{ padding: "8px 12px", background: "rgba(239, 68, 68, 0.1)", borderLeft: "3px solid #ef4444", borderRadius: "0 4px 4px 0", fontSize: "12px", color: "#fca5a5" }}>
                    {err}
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: "#22c55e", fontSize: "12px" }}>✓ Contract is valid</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
