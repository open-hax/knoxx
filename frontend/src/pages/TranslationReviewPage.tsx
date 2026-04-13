import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Input } from "@open-hax/uxx";
import {
  getTranslationDocument,
  listTranslationDocuments,
  reviewTranslationDocument,
  getTranslationManifest,
  getTranslationSftExport,
  submitTranslationLabel,
} from "../lib/api";
import type {
  TranslationDocumentSummary,
  TranslationDocumentDetail,
  TranslationLabelPayload,
  TranslationManifest,
  TranslationSegment,
  TranslationStatus,
} from "../lib/types";

const LANG_NAMES: Record<string, string> = {
  en: "English", es: "Español", fr: "Français", de: "Deutsch",
  ja: "日本語", zh: "中文", ko: "한국어", pt: "Português",
  ru: "Русский", it: "Italiano",
};

function langName(code: string): string {
  return LANG_NAMES[code] ?? code;
}

const defaultLabel: TranslationLabelPayload = {
  adequacy: "good",
  fluency: "good",
  terminology: "correct",
  risk: "safe",
  overall: "approve",
  corrected_text: "",
  editor_notes: "",
};

// ─── Status Badge ────────────────────────────────────────────────────

function StatusBadge({ status }: { status: string }) {
  const cls: Record<string, string> = {
    approved: "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300",
    rejected: "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300",
    in_review: "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300",
    pending: "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300",
    fully_approved: "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300",
    pending_review: "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300",
    partial_review: "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300",
    fully_rejected: "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300",
    mixed: "bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300",
  };
  const icons: Record<string, string> = {
    approved: "✅", rejected: "❌", in_review: "📝", pending: "⏳",
    fully_approved: "✅", pending_review: "⏳", partial_review: "🔄",
    fully_rejected: "❌", mixed: "🔀",
  };
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${cls[status] ?? cls.pending}`}>
      {icons[status] ?? "⏳"} {status.replace(/_/g, " ")}
    </span>
  );
}

// ─── Progress Bar ─────────────────────────────────────────────────────

function ProgressBar({ approved, total }: { approved: number; total: number }) {
  const pct = total > 0 ? (approved / total) * 100 : 0;
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 flex-1 rounded-full bg-slate-200 dark:bg-slate-700">
        <div
          className="h-1.5 rounded-full bg-emerald-500 transition-all"
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-xs text-slate-500 dark:text-slate-400">{approved}/{total}</span>
    </div>
  );
}

// ─── Document Card ────────────────────────────────────────────────────

function DocumentCard({
  doc,
  isSelected,
  onSelect,
}: {
  doc: TranslationDocumentSummary;
  isSelected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full rounded-lg border p-3 text-left transition ${
        isSelected
          ? "border-blue-500 bg-blue-50 dark:border-blue-400 dark:bg-blue-500/10"
          : "border-slate-200 bg-white hover:border-slate-300 dark:border-slate-700 dark:bg-slate-900/50 dark:hover:border-slate-600"
      }`}
    >
      <div className="mb-1.5 flex items-start justify-between gap-2">
        <span className="text-sm font-semibold text-slate-800 dark:text-slate-100 line-clamp-1">
          {doc.title}
        </span>
        <StatusBadge status={doc.overall_status} />
      </div>
      <div className="mb-2 text-xs text-slate-500 dark:text-slate-400">
        {langName(doc.source_lang)} → {langName(doc.target_lang)}
        {doc.garden_id && <span className="ml-2">· {doc.garden_id}</span>}
      </div>
      <ProgressBar approved={doc.approved} total={doc.total_segments} />
    </button>
  );
}

// ─── Segment Annotation (inline in document) ─────────────────────────

function SegmentAnnotation({
  segment,
  isSelected,
  onSelect,
}: {
  segment: TranslationSegment;
  isSelected: boolean;
  onSelect: () => void;
}) {
  const statusIcon: Record<string, string> = {
    approved: "✅", rejected: "❌", in_review: "📝", pending: "⏳",
  };

  const borderColor = isSelected
    ? "var(--token-colors-border-focus, #3b82f6)"
    : segment.status === "approved"
      ? "var(--token-colors-border-success, #10b981)"
      : segment.status === "rejected"
        ? "var(--token-colors-border-danger, #ef4444)"
        : "var(--token-colors-border-warning, #f59e0b)";

  const bgColor = isSelected
    ? "var(--token-colors-alpha-blue-_10, rgba(59, 130, 246, 0.1))"
    : segment.status === "approved"
      ? "var(--token-colors-alpha-green-_05, rgba(16, 185, 129, 0.05))"
      : segment.status === "rejected"
        ? "var(--token-colors-alpha-red-_05, rgba(239, 68, 68, 0.05))"
        : "var(--token-colors-alpha-orange-_05, rgba(245, 158, 11, 0.05))";

  return (
    <div
      style={{
        cursor: "pointer",
        borderRadius: 6,
        borderLeft: `4px solid ${borderColor}`,
        background: bgColor,
        padding: "8px 12px",
        transition: "background 0.15s",
      }}
      onClick={onSelect}
    >
      {/* Header row: segment index, status, reviews */}
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
        <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
          seg {segment.segment_index}
        </span>
        <span style={{ fontSize: 12 }}>{statusIcon[segment.status] ?? "⏳"}</span>
        <span style={{
          fontSize: 11,
          color: segment.status === "approved"
            ? "var(--token-colors-text-success)"
            : segment.status === "rejected"
              ? "var(--token-colors-text-danger)"
              : "var(--token-colors-text-warning)",
        }}>
          {segment.status}
        </span>
        {segment.label_count != null && segment.label_count > 0 && (
          <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            {segment.label_count} review{segment.label_count === 1 ? "" : "s"}
          </span>
        )}
      </div>

      {/* Two-column layout: source | translation */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
        <div>
          <div style={{ fontSize: 10, fontWeight: 500, color: "var(--token-colors-text-muted)", marginBottom: 4 }}>
            Source ({langName(segment.source_lang)})
          </div>
          <div style={{
            fontSize: 13,
            lineHeight: 1.5,
            color: "var(--token-colors-text-primary)",
            whiteSpace: "pre-wrap",
            wordBreak: "break-word",
          }}>
            {segment.source_text}
          </div>
        </div>
        <div>
          <div style={{ fontSize: 10, fontWeight: 500, color: "var(--token-colors-text-muted)", marginBottom: 4 }}>
            Translation ({langName(segment.target_lang)})
          </div>
          <div style={{
            fontSize: 13,
            lineHeight: 1.5,
            color: "var(--token-colors-text-primary)",
            whiteSpace: "pre-wrap",
            wordBreak: "break-word",
          }}>
            {segment.translated_text}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Segment Detail Panel ─────────────────────────────────────────────

function SegmentDetailPanel({
  segment,
  form,
  saving,
  onChange,
  onSubmit,
}: {
  segment: TranslationSegment | null;
  form: TranslationLabelPayload;
  saving: boolean;
  onChange: (f: TranslationLabelPayload) => void;
  onSubmit: (overall: "approve" | "needs_edit" | "reject") => void;
}) {
  if (!segment) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">Click a segment annotation to review it.</p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
          Segment {segment.segment_index}
        </h4>
        <StatusBadge status={segment.status} />
      </div>

      <div className="space-y-3">
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/50">
          <h5 className="mb-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Source ({langName(segment.source_lang)})
          </h5>
          <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">
            {segment.source_text}
          </pre>
        </div>
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/50">
          <h5 className="mb-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Translation ({langName(segment.target_lang)})
          </h5>
          <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">
            {segment.translated_text}
          </pre>
        </div>
      </div>

      <div className="grid gap-3">
        {(["adequacy", "fluency", "terminology", "risk"] as const).map((field) => (
          <label key={field} className="block text-sm">
            <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200 capitalize">{field}</span>
            <select
              value={form[field]}
              onChange={(e) => onChange({ ...form, [field]: e.target.value })}
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
            >
              {field === "risk" ? (
                ["safe", "sensitive", "policy_violation"].map((v) => <option key={v} value={v}>{v}</option>)
              ) : field === "terminology" ? (
                ["correct", "minor_errors", "major_errors"].map((v) => <option key={v} value={v}>{v}</option>)
              ) : (
                ["excellent", "good", "adequate", "poor", "unusable"].map((v) => <option key={v} value={v}>{v}</option>)
              )}
            </select>
          </label>
        ))}
      </div>

      <label className="block text-sm">
        <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Corrected translation</span>
        <textarea
          value={form.corrected_text ?? ""}
          onChange={(e) => onChange({ ...form, corrected_text: e.target.value })}
          rows={4}
          className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
          placeholder="Only if the approved text should differ from MT output."
        />
      </label>

      <label className="block text-sm">
        <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Editor notes</span>
        <textarea
          value={form.editor_notes ?? ""}
          onChange={(e) => onChange({ ...form, editor_notes: e.target.value })}
          rows={2}
          className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
          placeholder="Terminology caveats, tone issues, etc."
        />
      </label>

      <div className="flex gap-2">
        <Button disabled={saving} onClick={() => onSubmit("approve")}>Approve</Button>
        <Button variant="secondary" disabled={saving} onClick={() => onSubmit("needs_edit")}>Needs Edit</Button>
        <Button variant="ghost" disabled={saving} onClick={() => onSubmit("reject")}>Reject</Button>
      </div>

      {/* Existing labels */}
      {(segment.labels?.length ?? 0) > 0 && (
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/30">
          <h5 className="mb-2 text-xs font-semibold text-slate-600 dark:text-slate-300">Previous labels</h5>
          <div className="space-y-1">
            {segment.labels!.map((label) => (
              <div key={label.id} className="text-xs text-slate-500 dark:text-slate-400">
                <span className="font-medium">{label.labeler_email}</span>{" "}
                · {label.overall} · {label.adequacy}/{label.fluency}
                {label.corrected_text && <span className="ml-1">· corrected</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────

export default function TranslationReviewPage() {
  const [project, setProject] = useState("devel");
  const [targetLang, setTargetLang] = useState<string>("");
  const [documents, setDocuments] = useState<TranslationDocumentSummary[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<TranslationDocumentSummary | null>(null);
  const [docDetail, setDocDetail] = useState<TranslationDocumentDetail | null>(null);
  const [selectedSegIdx, setSelectedSegIdx] = useState<number | null>(null);
  const [form, setForm] = useState<TranslationLabelPayload>(defaultLabel);
  const [manifest, setManifest] = useState<TranslationManifest | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Load document list
  const loadDocuments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await listTranslationDocuments({ project, target_lang: targetLang || undefined });
      setDocuments(res.documents);
      // Preserve selection if still in list
      if (selectedDoc) {
        const still = res.documents.find(
          (d) => d.document_id === selectedDoc.document_id && d.target_lang === selectedDoc.target_lang,
        );
        if (!still) {
          setSelectedDoc(null);
          setDocDetail(null);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  }, [project, targetLang, selectedDoc]);

  // Load manifest
  const loadManifest = useCallback(async () => {
    try {
      const m = await getTranslationManifest(project);
      setManifest(m);
    } catch {
      setManifest(null);
    }
  }, [project]);

  useEffect(() => {
    void loadDocuments();
    void loadManifest();
  }, [loadDocuments, loadManifest]);

  // Load document detail when selection changes
  useEffect(() => {
    if (!selectedDoc) {
      setDocDetail(null);
      setSelectedSegIdx(null);
      return;
    }
    setDetailLoading(true);
    getTranslationDocument(selectedDoc.document_id, selectedDoc.target_lang)
      .then((detail) => {
        setDocDetail(detail);
        setSelectedSegIdx(null);
        setForm(defaultLabel);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : String(err));
        setDocDetail(null);
      })
      .finally(() => setDetailLoading(false));
  }, [selectedDoc]);

  const selectedSegment = useMemo(() => {
    if (!docDetail || selectedSegIdx == null) return null;
    return docDetail.segments.find((s) => s.segment_index === selectedSegIdx) ?? null;
  }, [docDetail, selectedSegIdx]);

  // Segment-level label submit
  async function handleSegmentSubmit(overall: "approve" | "needs_edit" | "reject") {
    if (!selectedSegment) return;
    setSaving(true);
    setError(null);
    try {
      await submitTranslationLabel(selectedSegment.id, {
        ...form,
        overall,
        corrected_text: form.corrected_text?.trim() || undefined,
        editor_notes: form.editor_notes?.trim() || undefined,
      });
      setNotice(`Segment ${selectedSegment.segment_index}: ${overall}`);
      // Reload detail
      if (selectedDoc) {
        const detail = await getTranslationDocument(selectedDoc.document_id, selectedDoc.target_lang);
        setDocDetail(detail);
      }
      setForm(defaultLabel);
      await loadDocuments();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  // Document-level review
  async function handleDocumentReview(overall: "approve" | "needs_edit" | "reject") {
    if (!selectedDoc) return;
    setSaving(true);
    setError(null);
    try {
      const result = await reviewTranslationDocument(
        selectedDoc.document_id,
        selectedDoc.target_lang,
        { overall },
      );
      setNotice(`Document review: ${overall} (${result.segments_reviewed} segments)`);
      await loadDocuments();
      // Reload detail
      const detail = await getTranslationDocument(selectedDoc.document_id, selectedDoc.target_lang);
      setDocDetail(detail);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  // SFT export
  async function handleExport() {
    try {
      const text = await getTranslationSftExport({ project, targetLang: targetLang || undefined, includeCorrected: true });
      const blob = new Blob([text], { type: "application/x-ndjson" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${project}-${targetLang || "all"}-translations.jsonl`;
      a.click();
      URL.revokeObjectURL(url);
      setNotice("SFT export downloaded.");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  // Available target langs from manifest
  const availableLangs = useMemo(() => {
    if (!manifest) return ["es", "de", "ko", "fr", "ja", "zh", "it", "pt", "ru"];
    return Object.keys(manifest.languages);
  }, [manifest]);

  return (
    <div
      style={{
        display: "flex",
        height: "calc(100vh - 96px)",
        flexDirection: "column",
        minHeight: 0,
        overflow: "hidden",
      }}
    >
      {/* Header */}
      <div
        style={{
          flexShrink: 0,
          borderBottom: "1px solid var(--token-colors-border-default)",
          background: "var(--token-colors-background-surface)",
          padding: "12px 16px",
        }}
      >
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-bold text-slate-800 dark:text-slate-100">Translation Review</h1>
          <Button variant="ghost" onClick={() => void handleExport()}>Export SFT</Button>
        </div>
        <div className="mt-2 flex gap-3">
          <label className="block text-sm">
            <span className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Project</span>
            <div className="w-28">
              <Input value={project} onChange={(e) => setProject(e.target.value)} placeholder="devel" />
            </div>
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Target Lang</span>
            <select
              value={targetLang}
              onChange={(e) => setTargetLang(e.target.value)}
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
            >
              <option value="">All</option>
              {availableLangs.map((l) => (
                <option key={l} value={l}>{langName(l)}</option>
              ))}
            </select>
          </label>
          {/* Compact manifest stats */}
          {manifest && (
            <div className="flex items-end gap-3 text-xs text-slate-500 dark:text-slate-400">
              {Object.entries(manifest.languages).map(([lang, stats]) => (
                <span key={lang}>
                  {langName(lang)}: {stats.approved}/{stats.total_segments} approved
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Notices */}
      {notice && (
        <div
          style={{
            flexShrink: 0,
            borderBottom: "1px solid var(--token-colors-border-success, #10b981)",
            background: "var(--token-colors-alpha-green-_10, rgba(16, 185, 129, 0.1))",
            padding: "8px 16px",
            fontSize: 14,
            color: "var(--token-colors-text-success, #10b981)",
          }}
        >
          {notice}
          <button style={{ marginLeft: 8, textDecoration: "underline" }} onClick={() => setNotice(null)}>dismiss</button>
        </div>
      )}
      {error && (
        <div
          style={{
            flexShrink: 0,
            borderBottom: "1px solid var(--token-colors-border-danger, #ef4444)",
            background: "var(--token-colors-alpha-red-_10, rgba(239, 68, 68, 0.1))",
            padding: "8px 16px",
            fontSize: 14,
            color: "var(--token-colors-text-danger, #ef4444)",
          }}
        >
          {error}
          <button style={{ marginLeft: 8, textDecoration: "underline" }} onClick={() => setError(null)}>dismiss</button>
        </div>
      )}

      {/* Main layout: document list | document chunks | segment editor */}
      <div style={{ display: "flex", flex: 1, minHeight: 0, overflow: "hidden" }}>
        {/* Left rail: document list - full height, scrollable */}
        <aside
          style={{
            width: 288,
            minWidth: 0,
            display: "flex",
            flexDirection: "column",
            borderRight: "1px solid var(--token-colors-border-default)",
            background: "var(--token-colors-surface-nav)",
            minHeight: 0,
            overflow: "hidden",
          }}
        >
          <div style={{ flex: 1, minHeight: 0, overflow: "auto", padding: 12 }}>
            {loading ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">Loading documents…</p>
            ) : documents.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">No translated documents found.</p>
            ) : (
              <div className="space-y-2">
                {documents.map((doc) => (
                  <DocumentCard
                    key={`${doc.document_id}-${doc.target_lang}`}
                    doc={doc}
                    isSelected={selectedDoc?.document_id === doc.document_id && selectedDoc?.target_lang === doc.target_lang}
                    onSelect={() => setSelectedDoc(doc)}
                  />
                ))}
              </div>
            )}
          </div>
        </aside>

        {/* Center: document chunks - scrollable */}
        <main
          style={{
            display: "flex",
            flex: 1,
            flexDirection: "column",
            minWidth: 0,
            borderRight: "1px solid var(--token-colors-border-default)",
            minHeight: 0,
            overflow: "hidden",
          }}
        >
          {!selectedDoc ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-400 dark:text-slate-500">
              Select a document to review
            </div>
          ) : detailLoading ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-400">Loading…</div>
          ) : !docDetail ? (
            <div className="flex flex-1 items-center justify-center text-sm text-rose-400">Failed to load document</div>
          ) : (
            <>
              {/* Document header - fixed */}
              <div
                style={{
                  flexShrink: 0,
                  borderBottom: "1px solid var(--token-colors-border-default)",
                  background: "var(--token-colors-background-surface)",
                  padding: "12px 16px",
                }}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-base font-semibold text-slate-800 dark:text-slate-100">
                      {docDetail.document.title}
                    </h2>
                    <div className="mt-0.5 flex items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                      <span>{langName(docDetail.document.source_lang)} → {langName(selectedDoc.target_lang)}</span>
                      <span>· {docDetail.summary.total_segments} segments</span>
                      <StatusBadge status={docDetail.summary.overall_status} />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button size="sm" disabled={saving} onClick={() => void handleDocumentReview("approve")}>
                      Approve All
                    </Button>
                    <Button size="sm" variant="secondary" disabled={saving} onClick={() => void handleDocumentReview("needs_edit")}>
                      Needs Edit
                    </Button>
                    <Button size="sm" variant="ghost" disabled={saving} onClick={() => void handleDocumentReview("reject")}>
                      Reject All
                    </Button>
                  </div>
                </div>
                <ProgressBar approved={docDetail.summary.approved} total={docDetail.summary.total_segments} />
              </div>

              {/* Segment annotations - scrollable */}
              <div style={{ flex: 1, minHeight: 0, overflow: "auto", padding: 16 }}>
                <div className="space-y-2">
                  {docDetail.segments.map((seg) => (
                    <SegmentAnnotation
                      key={seg.id}
                      segment={seg}
                      isSelected={selectedSegIdx === seg.segment_index}
                      onSelect={() => setSelectedSegIdx(seg.segment_index)}
                    />
                  ))}
                </div>
              </div>
            </>
          )}
        </main>

        {/* Right rail: segment editor - full height, scrollable */}
        <aside
          style={{
            width: 440,
            minWidth: 0,
            display: "flex",
            flexDirection: "column",
            background: "var(--token-colors-background-surface)",
            minHeight: 0,
            overflow: "hidden",
          }}
        >
          <div
            style={{
              flexShrink: 0,
              borderBottom: "1px solid var(--token-colors-border-default)",
              padding: "12px 16px",
            }}
          >
            <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">Segment Review</h3>
          </div>
          <div style={{ flex: 1, minHeight: 0, overflow: "auto", padding: 16 }}>
            {selectedSegment ? (
              <SegmentDetailPanel
                segment={selectedSegment}
                form={form}
                saving={saving}
                onChange={setForm}
                onSubmit={(overall) => void handleSegmentSubmit(overall)}
              />
            ) : (
              <p className="text-sm text-slate-500 dark:text-slate-400">Click a segment annotation to review it.</p>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
