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
  const enabledTools = tools.filter((tool) => tool.enabled);
  const disabledTools = tools.filter((tool) => !tool.enabled);
  const capabilityIds = Array.isArray(toolCatalog?.capability_ids) ? toolCatalog.capability_ids : [];

  const panelStyle = {
    borderRadius: 8,
    border: "1px solid var(--token-colors-border-subtle)",
    background: "var(--token-colors-surface-input)",
  } as const;

  const labelStyle = {
    display: "block",
    fontSize: 11,
    fontWeight: 600,
    marginBottom: 6,
    color: "var(--token-colors-text-muted)",
    textTransform: "uppercase",
    letterSpacing: 0.4,
  } as const;

  return (
    <Card
      variant="default"
      padding="sm"
      style={{
        margin: 8,
        flexShrink: 0,
        maxHeight: "min(70vh, 40rem)",
        overflow: "hidden",
        minHeight: 0,
      }}
    >
      <div
        data-testid="chat-settings-scroll-region"
        style={{
          display: "grid",
          gap: 16,
          maxHeight: "min(70vh, 40rem)",
          overflowY: "auto",
          overflowX: "hidden",
          paddingRight: 4,
        }}
      >
        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontSize: 14, fontWeight: 700 }}>Runtime prompt</div>
          <div style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>
            The effective system prompt is shown as its own full-width section so you can actually read the contract the agent is running with.
          </div>
          <div style={{ ...panelStyle, padding: 12 }}>
            <div style={{ ...labelStyle, marginBottom: 8 }}>Effective system prompt</div>
            <pre
              style={{
                margin: 0,
                whiteSpace: "pre-wrap",
                wordBreak: "break-word",
                fontSize: 12,
                lineHeight: 1.5,
                fontFamily: "var(--token-fontFamily-mono)",
                maxHeight: 320,
                overflow: "auto",
              }}
            >
              {toolCatalog?.system_prompt ?? "No effective system prompt available for this runtime yet."}
            </pre>
          </div>
          {(toolCatalog?.actor_system_prompt || toolCatalog?.agent_system_prompt || toolCatalog?.task_prompt) ? (
            <div
              style={{
                display: "grid",
                gap: 8,
                gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              }}
            >
              {toolCatalog?.actor_system_prompt ? (
                <div style={{ ...panelStyle, padding: 10 }}>
                  <div style={labelStyle}>Actor prompt</div>
                  <div style={{ fontSize: 12, lineHeight: 1.4, wordBreak: "break-word" }}>{toolCatalog.actor_system_prompt}</div>
                </div>
              ) : null}
              {toolCatalog?.agent_system_prompt ? (
                <div style={{ ...panelStyle, padding: 10 }}>
                  <div style={labelStyle}>Agent prompt</div>
                  <div style={{ fontSize: 12, lineHeight: 1.4, wordBreak: "break-word" }}>{toolCatalog.agent_system_prompt}</div>
                </div>
              ) : null}
              {toolCatalog?.task_prompt ? (
                <div style={{ ...panelStyle, padding: 10 }}>
                  <div style={labelStyle}>Task prompt</div>
                  <div style={{ fontSize: 12, lineHeight: 1.4, wordBreak: "break-word" }}>{toolCatalog.task_prompt}</div>
                </div>
              ) : null}
            </div>
          ) : null}
        </div>

        <div
          style={{
            display: "grid",
            gap: 12,
            gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
            alignItems: "start",
          }}
        >
          <div>
            <label style={labelStyle}>Session steering note</label>
            <textarea
              value={systemPrompt}
              onChange={(event: ChangeEvent<HTMLTextAreaElement>) => onSystemPromptChange(event.target.value)}
              rows={5}
              style={{
                width: "100%",
                borderRadius: 8,
                border: "1px solid var(--token-colors-border-subtle)",
                padding: 10,
                fontSize: 13,
                lineHeight: 1.5,
                resize: "vertical",
                background: "var(--token-colors-surface-input)",
                color: "var(--token-colors-text-default)",
              }}
              placeholder="Optional: steer the agent toward a specific outcome for upcoming turns..."
            />
            <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
              Appended as a lightweight steering note for future turns.
            </div>
          </div>

          <div style={{ display: "grid", gap: 12 }}>
            <div>
              <label style={labelStyle}>Conversation</label>
              <div
                style={{
                  ...panelStyle,
                  padding: "10px 12px",
                  fontSize: 12,
                  minHeight: 40,
                  color: "var(--token-colors-text-default)",
                  fontFamily: "var(--token-fontFamily-mono)",
                  wordBreak: "break-word",
                }}
              >
                {conversationId ?? "new / not started"}
              </div>
              <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
                Multi-turn memory is preserved per conversation id.
              </div>
            </div>

            <div>
              <label style={labelStyle}>Active agent</label>
              <select
                value={activeAgentId}
                onChange={(event) => onActiveAgentChange(event.target.value)}
                style={{
                  width: "100%",
                  borderRadius: 8,
                  border: "1px solid var(--token-colors-border-subtle)",
                  padding: "8px 10px",
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
            </div>
          </div>
        </div>

        <div
          style={{
            display: "grid",
            gap: 12,
            gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
            alignItems: "start",
          }}
        >
          <div style={{ ...panelStyle, padding: 12 }}>
            <div style={labelStyle}>Capabilities</div>
            {capabilityIds.length > 0 ? (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, maxHeight: 124, overflow: "auto", paddingRight: 4 }}>
                {capabilityIds.map((capabilityId, idx) => (
                  <Badge key={`${capabilityId}:${idx}`} size="sm" variant="default">{capabilityId}</Badge>
                ))}
              </div>
            ) : (
              <div style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>No runtime capabilities reported.</div>
            )}
          </div>

          <div style={{ ...panelStyle, padding: 12 }}>
            <div style={labelStyle}>Tools</div>
            <div style={{ display: "grid", gap: 8 }}>
              <div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
                  Enabled ({enabledTools.length})
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6, maxHeight: 96, overflow: "auto", paddingRight: 4 }}>
                  {enabledTools.length > 0 ? enabledTools.map((tool, idx) => (
                    <Badge key={`${tool.id}:${idx}`} size="sm" variant="default">{tool.label}</Badge>
                  )) : <span style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>None</span>}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
                  Disabled ({disabledTools.length})
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 6, maxHeight: 96, overflow: "auto", paddingRight: 4 }}>
                  {disabledTools.length > 0 ? disabledTools.map((tool, idx) => (
                    <Badge key={`${tool.id}:${idx}`} size="sm" variant="warning">{tool.label}</Badge>
                  )) : <span style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>None</span>}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}
