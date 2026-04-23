import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import type { RunDetail, RunEvent, ToolReceipt } from "../../lib/types";
import type { HydrationSource } from "./types";
import { asMarkdownPreview, formatMaybeDate, truncateText } from "./utils";

type ChatRuntimePanelProps = {
  wsStatus: "connected" | "closed" | "error" | "connecting";
  isRecovering: boolean;
  latestRun: RunDetail | null;
  isSending: boolean;
  selectedModel: string;
  conversationId: string | null;
  activeRunId: string | null;
  hydrationSources: HydrationSource[];
  runtimeEvents: RunEvent[];
  latestToolReceipts: ToolReceipt[];
  assistantSurfaceBackground: string;
  assistantSurfaceBorder: string;
  assistantSurfaceText: string;
  onOpenHydrationSource: (source: HydrationSource) => void | Promise<void>;
  onPinHydrationSource: (source: HydrationSource) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
};

export function ChatRuntimePanel({
  wsStatus,
  isRecovering,
  latestRun,
  isSending,
  selectedModel,
  conversationId,
  activeRunId,
  hydrationSources,
  runtimeEvents,
  latestToolReceipts,
  assistantSurfaceBackground,
  assistantSurfaceBorder,
  assistantSurfaceText,
  onOpenHydrationSource,
  onPinHydrationSource,
  onAppendToScratchpad,
}: ChatRuntimePanelProps) {
  return (
    <Card variant="outlined" padding="sm" style={{ marginBottom: 12, background: "var(--token-colors-alpha-bg-_08)" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 10, flexWrap: "wrap" }}>
        <div>
          <div style={{ fontSize: 12, fontWeight: 700 }}>Agent Runtime</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Presence, witness thread, and receipt river for the active Knoxx turn.
          </div>
        </div>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          <Badge size="sm" variant={wsStatus === "connected" ? "success" : wsStatus === "connecting" ? "warning" : "error"}>
            {wsStatus}
          </Badge>
          {isRecovering && <Badge size="sm" variant="warning">recovering</Badge>}
          <Badge size="sm" variant={latestRun?.status === "completed" ? "success" : latestRun?.status === "failed" ? "error" : isSending ? "warning" : "default"}>
            {latestRun?.status ?? (isSending ? "running" : "idle")}
          </Badge>
          <Badge size="sm" variant="info">{selectedModel || "no-model"}</Badge>
        </div>
      </div>

      <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <div style={{ display: "grid", gap: 6 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Presence</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Conversation</div>
          <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
            {conversationId ?? "new / not started"}
          </div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Latest run</div>
          <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
            {latestRun?.run_id ?? activeRunId ?? "waiting for first turn"}
          </div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            {latestRun?.ttft_ms != null ? `TTFT ${Math.round(latestRun.ttft_ms)}ms` : "No token timing yet"}
            {latestRun?.total_time_ms != null ? ` • total ${Math.round(latestRun.total_time_ms)}ms` : ""}
          </div>
        </div>

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Witness Thread</div>
          {hydrationSources.length === 0 ? (
            <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
              Passive hydration has not surfaced corpus witnesses for the latest run yet.
            </div>
          ) : (
            hydrationSources.slice(0, 3).map((source) => (
              <div key={source.path} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{source.title}</div>
                <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{source.path}</div>
                {source.section ? (
                  <div style={{ marginTop: 6 }}>
                    <Markdown content={asMarkdownPreview(truncateText(source.section, 180))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                  </div>
                ) : null}
                <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                  <Button variant="ghost" size="sm" onClick={() => void onOpenHydrationSource(source)}>Open</Button>
                  <Button variant="ghost" size="sm" onClick={() => onPinHydrationSource(source)}>Pin</Button>
                  <Button variant="ghost" size="sm" onClick={() => onAppendToScratchpad(source.section || source.path, source.title)}>Insert</Button>
                </div>
              </div>
            ))
          )}
        </div>

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Receipt River</div>
          {runtimeEvents.length === 0 ? (
            <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>No live runtime events yet.</div>
          ) : (
            runtimeEvents.slice(-6).reverse().map((event, index) => (
              <div key={`${event.type ?? "event"}:${event.at ?? index}`} style={{ borderLeft: "2px solid var(--token-colors-accent-cyan)", padding: "8px 10px", borderRadius: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{event.type ?? "event"}{event.tool_name ? ` • ${event.tool_name}` : ""}</div>
                <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84 }}>{formatMaybeDate(event.at as string | undefined) ?? event.at ?? "just now"}</div>
                {typeof event.preview === "string" && event.preview.trim().length > 0 ? (
                  <div style={{ marginTop: 6 }}>
                    <Markdown content={asMarkdownPreview(truncateText(event.preview, 300))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                  </div>
                ) : null}
              </div>
            ))
          )}
        </div>
      </div>

      {latestToolReceipts.length ? (
        <div style={{ marginTop: 12, paddingTop: 12, borderTop: "1px solid var(--token-colors-border-default)", display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Tool Receipts</div>
          {latestToolReceipts.slice(0, 4).map((receipt) => (
            <div key={receipt.id} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{receipt.tool_name ?? receipt.id}</div>
                <Badge size="sm" variant={receipt.status === "completed" ? "success" : receipt.status === "failed" ? "error" : "warning"}>{receipt.status ?? "running"}</Badge>
              </div>
              {typeof receipt.input_preview === "string" && receipt.input_preview.trim().length > 0 ? (
                <div style={{ marginTop: 6 }}>
                  <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>input</div>
                  <Markdown content={asMarkdownPreview(truncateText(receipt.input_preview, 500))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                </div>
              ) : null}
              {typeof receipt.result_preview === "string" && receipt.result_preview.trim().length > 0 ? (
                <div style={{ marginTop: 6 }}>
                  <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>output</div>
                  <Markdown content={asMarkdownPreview(truncateText(receipt.result_preview, 500))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                </div>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
    </Card>
  );
}
