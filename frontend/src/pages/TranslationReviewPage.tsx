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
  return (
    <div
      className={`group relative cursor-pointer rounded-md border-l-4 px-3 py-2 transition ${
        isSelected
          ? "border-l-blue-500 bg-blue-50/50 dark:border-l-blue-400 dark:bg-blue-500/10"
          : segment.status === "approved"
            ? "border-l-emerald-400 bg-emerald-50/30 hover:bg-emerald-50/60 dark:border-l-emerald-600 dark:bg-emerald-500/5 dark:hover:bg-emerald-500/10"
            : segment.status === "rejected"
              ? "border-l-rose-400 bg-rose-50/30 hover:bg-rose-50/60 dark:border-l-rose-600 dark:bg-rose-500/5 dark:hover:bg-rose-500/10"
              : "border-l-amber-400 bg-amber-50/30 hover:bg-amber-50/60 dark:border-l-amber-600 dark:bg-amber-500/5 dark:hover:bg-amber-500/10"
      }`}
      onClick={onSelect}
    >
      <div className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-200 whitespace-pre-wrap">
        <span className="shrink-0 text-xs">{statusIcon[segment.status] ?? "⏳"}</span>
        <span className="line-clamp-3">{segment.source_text}</span>
      </div>
      <div className="mt-1 flex items-center gap-2">
        <span className="text-xs text-slate-400 dark:text-slate-500">seg {segment.segment_index}</span>
        <span className={`text-xs ${segment.status === "approved" ? "text-emerald-600 dark:text-emerald-400" : segment.status === "rejected" ? "text-rose-600 dark:text-rose-400" : "text-amber-600 dark:text-amber-400"}`}>
          {segment.status}
        </span>
        {segment.label_count != null && segment.label_count > 0 && (
          <span className="text-xs text-slate-400">{segment.label_count} review{segment.label_count === 1 ? "" : "s"}</span>
        )}
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
      <div className="rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/40">
        <p className="text-sm text-slate-500 dark:text-slate-400">Click a segment annotation to review it.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4 rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/40">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200">
          Segment {segment.segment_index}
        </h3>
        <StatusBadge status={segment.status} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/50">
          <h4 className="mb-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Source ({langName(segment.source_lang)})
          </h4>
          <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">
            {segment.source_text}
          </pre>
        </div>
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-800/50">
          <h4 className="mb-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Translation ({langName(segment.target_lang)})
          </h4>
          <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">
            {segment.translated_text}
          </pre>
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
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
          <h4 className="mb-2 text-xs font-semibold text-slate-600 dark:text-slate-300">Previous labels</h4>
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
    <div className="flex h-full flex-col overflow-hidden">
      {/* Header */}
      <div className="shrink-0 border-b border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-800">
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-bold text-slate-800 dark:text-slate-100">Translation Review</h1>
          <Button variant="ghost" onClick={() => void handleExport()}>Export SFT</Button>
        </div>
        <div className="mt-2 flex gap-3">
          <label className="block text-sm">
            <span className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Project</span>
            <Input value={project} onChange={(e) => setProject(e.target.value)} placeholder="devel" className="w-28" />
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
        <div className="shrink-0 rounded-b border-b border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
          {notice}
          <button className="ml-2 underline" onClick={() => setNotice(null)}>dismiss</button>
        </div>
      )}
      {error && (
        <div className="shrink-0 rounded-b border-b border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
          {error}
          <button className="ml-2 underline" onClick={() => setError(null)}>dismiss</button>
        </div>
      )}

      {/* Main layout: document list | document annotation */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left rail: document list */}
        <aside className="w-80 shrink-0 overflow-y-auto border-r border-slate-200 bg-slate-50/50 p-3 dark:border-slate-700 dark:bg-slate-900/50">
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
        </aside>

        {/* Main content: document annotation view */}
        <main className="flex flex-1 flex-col overflow-y-auto">
          {!selectedDoc ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-400 dark:text-slate-500">
              Select a document to review
            </div>
          ) : detailLoading ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-400">Loading…</div>
          ) : !docDetail ? (
            <div className="flex flex-1 items-center justify-center text-sm text-rose-400">Failed to load document</div>
          ) : (
            <div className="flex flex-1 flex-col">
              {/* Document header + actions */}
              <div className="shrink-0 border-b border-slate-200 bg-white px-4 py-3 dark:border-slate-700 dark:bg-slate-800">
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

              {/* Segment annotations */}
              <div className="flex-1 overflow-y-auto p-4">
                <div className="mx-auto max-w-3xl space-y-2">
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

              {/* Segment detail panel (sticky bottom) */}
              {selectedSegment && (
                <div className="shrink-0 border-t border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800 max-h-[50vh] overflow-y-auto">
                  <SegmentDetailPanel
                    segment={selectedSegment}
                    form={form}
                    saving={saving}
                    onChange={setForm}
                    onSubmit={(overall) => void handleSegmentSubmit(overall)}
                  />
                </div>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
