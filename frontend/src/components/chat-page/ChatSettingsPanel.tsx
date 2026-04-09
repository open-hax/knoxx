import type { ChangeEvent } from "react";
import { Badge, Card } from "@open-hax/uxx";
import type { ToolCatalogResponse } from "../../lib/types";

type ChatSettingsPanelProps = {
  systemPrompt: string;
  onSystemPromptChange: (value: string) => void;
  conversationId: string | null;
  activeRole: string;
  onActiveRoleChange: (value: string) => void;
  toolCatalog: ToolCatalogResponse | null;
};

export function ChatSettingsPanel({
  systemPrompt,
  onSystemPromptChange,
  conversationId,
  activeRole,
  onActiveRoleChange,
  toolCatalog,
}: ChatSettingsPanelProps) {
  return (
    <Card variant="default" padding="sm" style={{ margin: 8, flexShrink: 0 }}>
      <div style={{ display: "grid", gap: 12, gridTemplateColumns: "minmax(0,1fr) 140px 180px" }}>
        <div>
          <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Session Steering Note</label>
          <textarea
            value={systemPrompt}
            onChange={(event: ChangeEvent<HTMLTextAreaElement>) => onSystemPromptChange(event.target.value)}
            rows={3}
            style={{
              width: "100%",
              borderRadius: 6,
              border: "1px solid var(--token-colors-border-subtle)",
              padding: 8,
              fontSize: 13,
              resize: "vertical",
              background: "var(--token-colors-surface-input)",
              color: "var(--token-colors-text-default)",
            }}
            placeholder="Optional: steer the agent toward a specific outcome for upcoming turns..."
          />
          <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            This is appended as a lightweight steering note to future turns.
          </div>
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Conversation</label>
          <div
            style={{
              borderRadius: 6,
              border: "1px solid var(--token-colors-border-subtle)",
              padding: "8px 10px",
              fontSize: 11,
              minHeight: 34,
              background: "var(--token-colors-surface-input)",
              color: "var(--token-colors-text-default)",
              fontFamily: "var(--token-fontFamily-mono)",
            }}
          >
            {conversationId ?? "new / not started"}
          </div>
          <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Multi-turn memory is preserved per conversation id.
          </div>
        </div>
        <div>
          <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Role</label>
          <select
            value={activeRole}
            onChange={(event) => onActiveRoleChange(event.target.value)}
            style={{
              width: "100%",
              borderRadius: 6,
              border: "1px solid var(--token-colors-border-subtle)",
              padding: "6px 8px",
              fontSize: 12,
              background: "var(--token-colors-surface-input)",
              color: "var(--token-colors-text-default)",
            }}
          >
            <option value="executive">Executive</option>
            <option value="principal_architect">Principal Architect</option>
            <option value="junior_dev">Junior Dev</option>
          </select>
          <div style={{ marginTop: 6, display: "flex", flexWrap: "wrap", gap: 6 }}>
            {toolCatalog?.tools.map((tool) => (
              <Badge key={tool.id} size="sm" variant={tool.enabled ? "default" : "warning"}>
                {tool.label}
              </Badge>
            ))}
          </div>
        </div>
      </div>
    </Card>
  );
}
