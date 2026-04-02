interface ConsolePanelProps {
  lines: string[];
}

function ConsolePanel({ lines }: ConsolePanelProps) {
  return (
    <section className="panel flex h-full min-h-0 flex-col">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="panel-title">Console Stream</h2>
        <span className="text-xs text-slate-500">{lines.length} lines</span>
      </div>
      <div className="min-h-0 flex-1 overflow-auto rounded-md bg-slate-950 p-2 font-mono text-xs text-emerald-200">
        {lines.length === 0 ? (
          <p className="text-emerald-400/70">Waiting for logs...</p>
        ) : (
          lines.map((line, idx) => <div key={`${idx}-${line}`}>{line}</div>)
        )}
      </div>
    </section>
  );
}

export default ConsolePanel;
