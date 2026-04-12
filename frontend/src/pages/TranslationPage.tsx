import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Button, Card, Input } from "@open-hax/uxx";
import {
  getTranslationManifest,
  getTranslationSegment,
  getTranslationSftExport,
  listTranslationSegments,
  submitTranslationLabel,
} from "../lib/api";
import type {
  TranslationLabelPayload,
  TranslationManifest,
  TranslationOverall,
  TranslationSegment,
  TranslationStatus,
} from "../lib/types";
import TranslationManifestCard from "../components/translation-page/TranslationManifestCard";
import TranslationReviewCard from "../components/translation-page/TranslationReviewCard";
import TranslationSegmentList from "../components/translation-page/TranslationSegmentList";

const defaultForm: TranslationLabelPayload = {
  adequacy: "good",
  fluency: "good",
  terminology: "correct",
  risk: "safe",
  overall: "approve",
  corrected_text: "",
  editor_notes: "",
};

export default function TranslationPage() {
  const [project, setProject] = useState("devel");
  const [status, setStatus] = useState<TranslationStatus | "all">("pending");
  const [targetLang, setTargetLang] = useState("es");
  const [segments, setSegments] = useState<TranslationSegment[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedSegment, setSelectedSegment] = useState<TranslationSegment | null>(null);
  const [manifest, setManifest] = useState<TranslationManifest | null>(null);
  const [form, setForm] = useState<TranslationLabelPayload>(defaultForm);
  const [loading, setLoading] = useState(true);
  const [segmentLoading, setSegmentLoading] = useState(false);
  const [manifestLoading, setManifestLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Load segment detail when selection changes
  useEffect(() => {
    if (!selectedId) {
      setSelectedSegment(null);
      return;
    }

    setSegmentLoading(true);
    getTranslationSegment(selectedId)
      .then((segment) => {
        setSelectedSegment(segment);
        setForm(defaultForm);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : String(err));
        setSelectedSegment(null);
      })
      .finally(() => {
        setSegmentLoading(false);
      });
  }, [selectedId]);

  async function loadSegments() {
    setLoading(true);
    setError(null);
    try {
      const response = await listTranslationSegments({
        project,
        status,
        target_lang: targetLang,
        limit: 100,
      });
      setSegments(response.segments);
      setSelectedId((prev) => prev && response.segments.some((segment) => segment.id === prev)
        ? prev
        : response.segments[0]?.id ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setSegments([]);
      setSelectedId(null);
    } finally {
      setLoading(false);
    }
  }

  async function loadManifest() {
    setManifestLoading(true);
    try {
      const nextManifest = await getTranslationManifest(project);
      setManifest(nextManifest);
    } catch (err) {
      setManifest(null);
      setError((prev) => prev ?? (err instanceof Error ? err.message : String(err)));
    } finally {
      setManifestLoading(false);
    }
  }

  useEffect(() => {
    void loadSegments();
    void loadManifest();
  }, [project, status, targetLang]);

  async function handleSubmit(overall: TranslationOverall) {
    if (!selectedSegment) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      await submitTranslationLabel(selectedSegment.id, {
        ...form,
        overall,
        corrected_text: form.corrected_text?.trim() || undefined,
        editor_notes: form.editor_notes?.trim() || undefined,
      });
      setNotice(`Saved ${overall} for ${selectedSegment.document_id}#${selectedSegment.segment_index}.`);
      // Reload segments list and selected segment detail
      await Promise.all([loadSegments(), loadManifest()]);
      // Re-fetch selected segment to get updated labels
      if (selectedId) {
        const updatedSegment = await getTranslationSegment(selectedId);
        setSelectedSegment(updatedSegment);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleDownloadExport() {
    setError(null);
    try {
      const text = await getTranslationSftExport({ project, targetLang, includeCorrected: true });
      const blob = new Blob([text], { type: "application/x-ndjson" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${project}-${targetLang}-translations.jsonl`;
      anchor.click();
      URL.revokeObjectURL(url);
      setNotice(`Downloaded ${project}/${targetLang} SFT export.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[320px,minmax(0,1fr)]">
      <div className="space-y-4">
        <Card variant="elevated" title="Translation Review">
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Review GLM-5 translations, correct them, and turn accepted corrections into training data grounded in OpenPlanner.
          </p>
          <div className="mt-4 space-y-3">
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Project</span>
              <Input value={project} onChange={(event: ChangeEvent<HTMLInputElement>) => setProject(event.target.value)} placeholder="devel" />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Target language</span>
              <select value={targetLang} onChange={(event) => setTargetLang(event.target.value)} className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800">
                <option value="es">es</option>
                <option value="de">de</option>
              </select>
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Status</span>
              <select value={status} onChange={(event) => setStatus(event.target.value as TranslationStatus | "all")} className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800">
                <option value="pending">pending</option>
                <option value="in_review">in review</option>
                <option value="approved">approved</option>
                <option value="rejected">rejected</option>
                <option value="all">all</option>
              </select>
            </label>
            <div className="flex flex-wrap gap-2">
              <Button variant="secondary" onClick={() => void loadSegments()} disabled={loading}>Refresh queue</Button>
              <Button variant="ghost" onClick={() => void handleDownloadExport()}>Download SFT</Button>
            </div>
          </div>
        </Card>

        <TranslationManifestCard manifest={manifest} loading={manifestLoading} />

        <Card variant="elevated" title={`Segments (${segments.length})`}>
          {loading ? <p className="text-sm text-slate-500 dark:text-slate-400">Loading translation queue…</p> : null}
          {!loading ? (
            <TranslationSegmentList
              segments={segments}
              selectedId={selectedId}
              onSelect={(segment) => setSelectedId(segment.id)}
            />
          ) : null}
        </Card>
      </div>

      <div className="space-y-4">
        {notice ? <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">{notice}</div> : null}
        {error ? <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">{error}</div> : null}
        <TranslationReviewCard
          segment={selectedSegment}
          form={form}
          saving={saving}
          onChange={setForm}
          onSubmit={(overall) => void handleSubmit(overall)}
        />
      </div>
    </div>
  );
}
