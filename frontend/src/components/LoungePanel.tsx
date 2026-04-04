import { FormEvent, useState } from "react";
import type { LoungeMessage } from "../lib/types";

interface LoungePanelProps {
  collapsed: boolean;
  onToggle: () => void;
  messages: LoungeMessage[];
  alias: string;
  onAliasChange: (v: string) => void;
  onSend: (text: string) => void;
}

function LoungePanel({ collapsed, onToggle, messages, alias, onAliasChange, onSend }: LoungePanelProps) {
  const [text, setText] = useState("");

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText("");
  };

  return (
    <section className="panel min-h-0">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="panel-title">Team Lounge</h2>
        <button className="btn-ghost" onClick={onToggle}>{collapsed ? "Expand" : "Collapse"}</button>
      </div>

      {collapsed ? (
        <p style={{ fontSize: 12, color: 'var(--token-colors-text-muted)' }}>Lounge hidden. Expand to chat with other users.</p>
      ) : (
        <>
          <div className="mb-2 flex items-center gap-2">
            <label style={{ fontSize: 12, color: 'var(--token-colors-text-soft)' }}>Alias</label>
            <input
              className="input max-w-48"
              value={alias}
              onChange={(e) => onAliasChange(e.target.value)}
              placeholder="your name"
            />
          </div>

          <div
            style={{
              maxHeight: 160,
              overflow: 'auto',
              borderRadius: 8,
              border: '1px solid var(--token-colors-border-default)',
              background: 'var(--token-colors-alpha-bg-_12)',
              padding: 8,
              fontSize: 14,
            }}
          >
            {messages.length === 0 ? (
              <p style={{ color: 'var(--token-colors-text-muted)' }}>No lounge messages yet.</p>
            ) : (
              messages.map((m) => (
                <div key={m.id} className="mb-1">
                  <span style={{ fontWeight: 600, color: 'var(--token-colors-text-default)' }}>{m.alias}</span>
                  <span style={{ marginLeft: 8, fontSize: 12, color: 'var(--token-colors-text-muted)' }}>{new Date(m.timestamp).toLocaleTimeString()}</span>
                  <p style={{ whiteSpace: 'pre-wrap', color: 'var(--token-colors-text-panel)' }}>{m.text}</p>
                </div>
              ))
            )}
          </div>

          <form className="mt-2 flex gap-2" onSubmit={handleSubmit}>
            <input
              className="input flex-1"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Message everyone while runs are in progress..."
            />
            <button className="btn-primary" type="submit">Send</button>
          </form>
        </>
      )}
    </section>
  );
}

export default LoungePanel;
