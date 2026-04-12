import { Button, Card } from "@open-hax/uxx";
import type {
  TranslationAdequacy,
  TranslationFluency,
  TranslationLabelPayload,
  TranslationOverall,
  TranslationRisk,
  TranslationSegment,
  TranslationTerminology,
} from "../../lib/types";

const adequacyOptions: TranslationAdequacy[] = ["excellent", "good", "adequate", "poor", "unusable"];
const fluencyOptions: TranslationFluency[] = ["excellent", "good", "adequate", "poor", "unusable"];
const terminologyOptions: TranslationTerminology[] = ["correct", "minor_errors", "major_errors"];
const riskOptions: TranslationRisk[] = ["safe", "sensitive", "policy_violation"];

interface TranslationReviewCardProps {
  segment: TranslationSegment | null;
  form: TranslationLabelPayload;
  saving: boolean;
  onChange: (next: TranslationLabelPayload) => void;
  onSubmit: (overall: TranslationOverall) => void;
}

function SelectField<T extends string>({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: T;
  options: readonly T[];
  onChange: (value: T) => void;
}) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value as T)}
        className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
      >
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
  );
}

export default function TranslationReviewCard({ segment, form, saving, onChange, onSubmit }: TranslationReviewCardProps) {
  return (
    <Card variant="elevated" title="Review Segment">
      {!segment ? <p className="text-sm text-slate-500 dark:text-slate-400">Select a segment to begin review.</p> : null}
      {segment ? (
        <div className="space-y-4">
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-lg border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900/40">
              <h3 className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-200">Source ({segment.source_lang})</h3>
              <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">{segment.source_text}</pre>
            </div>
            <div className="rounded-lg border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900/40">
              <h3 className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-200">Translation ({segment.target_lang})</h3>
              <pre className="whitespace-pre-wrap break-words text-sm text-slate-700 dark:text-slate-200">{segment.translated_text}</pre>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <SelectField label="Adequacy" value={form.adequacy} options={adequacyOptions} onChange={(value) => onChange({ ...form, adequacy: value })} />
            <SelectField label="Fluency" value={form.fluency} options={fluencyOptions} onChange={(value) => onChange({ ...form, fluency: value })} />
            <SelectField label="Terminology" value={form.terminology} options={terminologyOptions} onChange={(value) => onChange({ ...form, terminology: value })} />
            <SelectField label="Risk" value={form.risk} options={riskOptions} onChange={(value) => onChange({ ...form, risk: value })} />
          </div>

          <label className="block text-sm">
            <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Corrected translation</span>
            <textarea
              value={form.corrected_text ?? ""}
              onChange={(event) => onChange({ ...form, corrected_text: event.target.value })}
              rows={6}
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
              placeholder="Only fill this in if you want the approved target text to differ from the MT output."
            />
          </label>

          <label className="block text-sm">
            <span className="mb-1 block font-medium text-slate-700 dark:text-slate-200">Editor notes</span>
            <textarea
              value={form.editor_notes ?? ""}
              onChange={(event) => onChange({ ...form, editor_notes: event.target.value })}
              rows={3}
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800"
              placeholder="Record terminology caveats, tone issues, or why a correction was needed."
            />
          </label>

          <div className="flex flex-wrap gap-2">
            <Button disabled={saving} onClick={() => onSubmit("approve")}>Approve</Button>
            <Button variant="secondary" disabled={saving} onClick={() => onSubmit("needs_edit")}>Needs Edit</Button>
            <Button variant="ghost" disabled={saving} onClick={() => onSubmit("reject")}>Reject</Button>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 dark:border-slate-700 dark:bg-slate-900/30">
            <h3 className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-200">Existing labels</h3>
            <div className="space-y-2">
              {(segment.labels?.length ?? 0) > 0 ? segment.labels?.map((label) => (
                <div key={label.id} className="rounded-md bg-white px-3 py-2 text-sm dark:bg-slate-800">
                  <div className="mb-1 flex items-center justify-between gap-2">
                    <span className="font-medium text-slate-700 dark:text-slate-200">{label.labeler_email}</span>
                    <span className="text-xs text-slate-500 dark:text-slate-400">{new Date(label.ts).toLocaleString()}</span>
                  </div>
                  <p className="text-slate-600 dark:text-slate-300">{label.overall} · {label.adequacy} adequacy · {label.fluency} fluency</p>
                  {label.corrected_text ? <pre className="mt-2 whitespace-pre-wrap break-words text-xs text-slate-600 dark:text-slate-300">{label.corrected_text}</pre> : null}
                </div>
              )) : <p className="text-sm text-slate-500 dark:text-slate-400">No labels yet.</p>}
            </div>
          </div>
        </div>
      ) : null}
    </Card>
  );
}
