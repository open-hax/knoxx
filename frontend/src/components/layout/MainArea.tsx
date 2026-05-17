export type MainAreaProps = {
  children: React.ReactNode;
  bottomPanel?: React.ReactNode;
  className?: string;
};

export function MainArea({ children, bottomPanel, className = "" }: MainAreaProps) {
  return (
    <div
      style={{
        flex: 1,
        minWidth: 0,
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
      }}
      className={className}
    >
      <div style={{ flex: 1, minHeight: 0, overflow: "hidden", display: "flex", flexDirection: "column" }}>
        {children}
      </div>
      {bottomPanel}
    </div>
  );
}
