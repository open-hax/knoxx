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
        <p className="text-xs text-slate-500">Lounge hidden. Expand to chat with other users.</p>
      ) : (
        <>
          <div className="mb-2 flex items-center gap-2">
            <label className="text-xs text-slate-600">Alias</label>
            <input
              className="input max-w-48"
              value={alias}
              onChange={(e) => onAliasChange(e.target.value)}
              placeholder="your name"
            />
          </div>

          <div className="max-h-40 overflow-auto rounded-md border border-slate-200 bg-slate-50 p-2 text-sm">
            {messages.length === 0 ? (
              <p className="text-slate-500">No lounge messages yet.</p>
            ) : (
              messages.map((m) => (
                <div key={m.id} className="mb-1">
                  <span className="font-semibold text-slate-700">{m.alias}</span>
                  <span className="ml-2 text-xs text-slate-500">{new Date(m.timestamp).toLocaleTimeString()}</span>
                  <p className="whitespace-pre-wrap text-slate-800">{m.text}</p>
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
