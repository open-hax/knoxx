import type { TranslationSegment } from "../../lib/types";

interface TranslationSegmentListProps {
  segments: TranslationSegment[];
  selectedId: string | null;
  onSelect: (segment: TranslationSegment) => void;
}

function statusClasses(status: TranslationSegment["status"]): string {
  switch (status) {
    case "approved":
      return "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300";
    case "rejected":
      return "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300";
    case "in_review":
      return "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300";
    default:
      return "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-200";
  }
}

export default function TranslationSegmentList({ segments, selectedId, onSelect }: TranslationSegmentListProps) {
  return (
    <div className="space-y-2">
      {segments.map((segment) => (
        <button
          key={segment.id}
          type="button"
          onClick={() => onSelect(segment)}
          className={`w-full rounded-lg border p-3 text-left transition ${selectedId === segment.id
            ? "border-blue-500 bg-blue-50 dark:border-blue-400 dark:bg-blue-500/10"
            : "border-slate-200 bg-white hover:border-slate-300 dark:border-slate-700 dark:bg-slate-900/50 dark:hover:border-slate-600"
          }`}
        >
          <div className="mb-2 flex items-center justify-between gap-2">
            <span className="truncate text-sm font-semibold text-slate-800 dark:text-slate-100">{segment.document_id}</span>
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusClasses(segment.status)}`}>{segment.status}</span>
          </div>
          <p className="line-clamp-2 text-sm text-slate-600 dark:text-slate-300">{segment.source_text}</p>
          <div className="mt-2 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span>{segment.source_lang} → {segment.target_lang}</span>
            <span>{segment.labels.length} review{segment.labels.length === 1 ? "" : "s"}</span>
          </div>
        </button>
      ))}
      {segments.length === 0 ? <p className="text-sm text-slate-500 dark:text-slate-400">No segments match the current filter.</p> : null}
    </div>
  );
}
