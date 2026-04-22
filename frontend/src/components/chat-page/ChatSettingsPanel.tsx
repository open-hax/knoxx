import type { ChangeEvent } from "react";
import { Badge, Card } from "@open-hax/uxx";
import type { AgentContractCatalogItem, ToolCatalogResponse } from "../../lib/types";

type ChatSettingsPanelProps = {
  systemPrompt: string;
  onSystemPromptChange: (value: string) => void;
  conversationId: string | null;
  activeRole: string;
  activeActorId: string;
  activeAgentId: string;
  availableAgents: AgentContractCatalogItem[];
  onActiveAgentChange: (value: string) => void;
  toolCatalog: ToolCatalogResponse | null;
};

export function ChatSettingsPanel({
  systemPrompt,
  onSystemPromptChange,
  conversationId,
  activeRole,
  activeActorId,
  activeAgentId,
  availableAgents,
  onActiveAgentChange,
  toolCatalog,
}: ChatSettingsPanelProps) {
  const tools = Array.isArray(toolCatalog?.tools) ? toolCatalog.tools : [];

  return (
    <Card variant="default" padding="sm" style={{ margin: 8, flexShrink: 0 }}>
      <div style={{ display: "grid", gap: 12, gridTemplateColumns: "minmax(0,1fr) 140px 220px" }}>
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
          <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Active agent</label>
          <select
            value={activeAgentId}
            onChange={(event) => onActiveAgentChange(event.target.value)}
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
            {availableAgents.length === 0 ? <option value="">No agents available</option> : null}
            {availableAgents.map((agent) => (
              <option key={agent.id} value={agent.id}>
                {agent.id}
              </option>
            ))}
          </select>
          <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Actor: {activeActorId || "(default)"}
          </div>
          <div style={{ marginTop: 4, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Role: {activeRole}
          </div>
          {toolCatalog?.capability_ids && toolCatalog.capability_ids.length > 0 ? (
            <div style={{ marginTop: 6 }}>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", marginBottom: 4 }}>Capabilities</div>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                {toolCatalog.capability_ids.map((capabilityId) => (
                  <Badge key={capabilityId} size="sm" variant="default">{capabilityId}</Badge>
                ))}
              </div>
            </div>
          ) : null}
          <div style={{ marginTop: 6, display: "flex", flexWrap: "wrap", gap: 6 }}>
            {tools.map((tool) => (
              <Badge key={tool.id} size="sm" variant={tool.enabled ? "default" : "warning"}>
                {tool.label}
              </Badge>
            ))}
          </div>
          {toolCatalog?.system_prompt ? (
            <div style={{ marginTop: 8, display: "grid", gap: 6 }}>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Effective system prompt</div>
              <pre style={{ whiteSpace: "pre-wrap", fontSize: 11, padding: 8, borderRadius: 6, background: "var(--token-colors-surface-input)", border: "1px solid var(--token-colors-border-subtle)", maxHeight: 180, overflow: "auto" }}>{toolCatalog.system_prompt}</pre>
              {toolCatalog.actor_system_prompt ? <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Actor prompt: {toolCatalog.actor_system_prompt}</div> : null}
              {toolCatalog.agent_system_prompt ? <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Agent prompt: {toolCatalog.agent_system_prompt}</div> : null}
              {toolCatalog.task_prompt ? <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Task prompt: {toolCatalog.task_prompt}</div> : null}
            </div>
          ) : null}
        </div>
      </div>
    </Card>
  );
}
