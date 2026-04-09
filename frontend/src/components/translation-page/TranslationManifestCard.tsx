import { Card } from "@open-hax/uxx";
import type { TranslationManifest } from "../../lib/types";

interface TranslationManifestCardProps {
  manifest: TranslationManifest | null;
  loading: boolean;
}

export default function TranslationManifestCard({ manifest, loading }: TranslationManifestCardProps) {
  return (
    <Card variant="elevated" title="Translation Ops">
      {loading ? <p className="text-sm text-slate-500 dark:text-slate-400">Loading translation stats…</p> : null}
      {!loading && !manifest ? <p className="text-sm text-slate-500 dark:text-slate-400">No manifest loaded yet.</p> : null}
      {manifest ? (
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            {Object.entries(manifest.languages).map(([lang, stats]) => (
              <div key={lang} className="rounded-lg border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900/40">
                <div className="mb-2 flex items-center justify-between">
                  <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-700 dark:text-slate-200">{lang}</h3>
                  <span className="text-xs text-slate-500 dark:text-slate-400">{stats.total_segments} segments</span>
                </div>
                <dl className="grid grid-cols-2 gap-x-3 gap-y-1 text-sm">
                  <div><dt className="text-slate-500 dark:text-slate-400">Approved</dt><dd>{stats.approved}</dd></div>
                  <div><dt className="text-slate-500 dark:text-slate-400">Pending</dt><dd>{stats.pending}</dd></div>
                  <div><dt className="text-slate-500 dark:text-slate-400">Rejected</dt><dd>{stats.rejected}</dd></div>
                  <div><dt className="text-slate-500 dark:text-slate-400">Corrections</dt><dd>{stats.with_corrections}</dd></div>
                </dl>
              </div>
            ))}
          </div>
          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-700 dark:text-slate-200">Reviewers</h3>
            <ul className="space-y-1 text-sm text-slate-600 dark:text-slate-300">
              {manifest.labelers.length > 0 ? manifest.labelers.map((labeler) => (
                <li key={labeler.email} className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-1 dark:bg-slate-800/70">
                  <span>{labeler.email}</span>
                  <span>{labeler.segments_labeled}</span>
                </li>
              )) : <li className="text-slate-500 dark:text-slate-400">No reviewer activity yet.</li>}
            </ul>
          </div>
        </div>
      ) : null}
    </Card>
  );
}
