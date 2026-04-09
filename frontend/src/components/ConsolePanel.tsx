interface ConsolePanelProps {
  lines: string[];
}

function ConsolePanel({ lines }: ConsolePanelProps) {
  return (
    <section className="panel flex h-full min-h-0 flex-col">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="panel-title">Console Stream</h2>
        <span style={{ fontSize: 12, color: 'var(--token-colors-text-muted)' }}>{lines.length} lines</span>
      </div>
      <div
        style={{
          minHeight: 0,
          flex: 1,
          overflow: 'auto',
          borderRadius: 8,
          background: 'var(--token-colors-alpha-bg-_88b)',
          border: '1px solid var(--token-colors-border-default)',
          padding: 8,
          fontFamily: 'var(--token-fontFamily-mono)',
          fontSize: 12,
          color: 'var(--token-colors-accent-green)',
        }}
      >
        {lines.length === 0 ? (
          <p style={{ color: 'var(--token-colors-alpha-green-_55)' }}>Waiting for logs...</p>
        ) : (
          lines.map((line, idx) => <div key={`${idx}-${line}`}>{line}</div>)
        )}
      </div>
    </section>
  );
}

export default ConsolePanel;
