import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Button, Card, Input } from '@open-hax/uxx';

type PresetsResponse = {
  presets: Record<string, string[]>;
};

type QueryRow = {
  id: string;
  ts?: string;
  source?: string;
  kind?: string;
  project?: string;
  session?: string;
  message?: string;
  snippet?: string;
  text?: string;
  tier?: string;
};

type SearchResponse = {
  projects: string[];
  count: number;
  rows: QueryRow[];
};

type AnswerResponse = SearchResponse & {
  answer: string;
};

const DEFAULT_ROLE = "workspace";

function parseProjects(raw: string): string[] {
  return raw
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
}

function QueryPage() {
  const [presets, setPresets] = useState<Record<string, string[]>>({});
  const [role, setRole] = useState(DEFAULT_ROLE);
  const [projectsInput, setProjectsInput] = useState("");
  const [query, setQuery] = useState("What knowledge lakes exist in this system?");
  const [limit, setLimit] = useState(5);
  const [loadingPresets, setLoadingPresets] = useState(true);
  const [searchLoading, setSearchLoading] = useState(false);
  const [answerLoading, setAnswerLoading] = useState(false);
  const [searchResult, setSearchResult] = useState<SearchResponse | null>(null);
  const [answerResult, setAnswerResult] = useState<AnswerResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/query/presets")
      .then((resp) => {
        if (!resp.ok) throw new Error(`Failed to load presets: ${resp.status}`);
        return resp.json() as Promise<PresetsResponse>;
      })
      .then((data) => {
        if (cancelled) return;
        setPresets(data.presets || {});
      })
      .catch((err) => {
        if (cancelled) return;
        setError(String(err));
      })
      .finally(() => {
        if (!cancelled) setLoadingPresets(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const activePresetProjects = useMemo(() => presets[role] || [], [presets, role]);
  const effectiveProjects = useMemo(
    () => (projectsInput.trim() ? parseProjects(projectsInput) : activePresetProjects),
    [projectsInput, activePresetProjects]
  );

  async function runSearch() {
    setSearchLoading(true);
    setError(null);
    setAnswerResult(null);
    try {
      const resp = await fetch("/api/query/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          q: query,
          role,
          projects: projectsInput.trim() ? parseProjects(projectsInput) : undefined,
          limit,
          tenant_id: "devel",
        }),
      });
      if (!resp.ok) throw new Error(`Search failed: ${resp.status}`);
      const data = (await resp.json()) as SearchResponse;
      setSearchResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSearchLoading(false);
    }
  }

  async function runAnswer() {
    setAnswerLoading(true);
    setError(null);
    try {
      const resp = await fetch("/api/query/answer", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          q: query,
          role,
          projects: projectsInput.trim() ? parseProjects(projectsInput) : undefined,
          limit,
          tenant_id: "devel",
        }),
      });
      if (!resp.ok) throw new Error(`Answer failed: ${resp.status}`);
      const data = (await resp.json()) as AnswerResponse;
      setAnswerResult(data);
      setSearchResult({ projects: data.projects, count: data.count, rows: data.rows });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setAnswerLoading(false);
    }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[360px,minmax(0,1fr)]">
      <Card variant="elevated" title="Federated Lake Query">
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Query `devel`, `cephalon-hive`, and `sintel` through one Clojure surface.
        </p>

        <div className="mt-4 space-y-4">
          <div>
            <label htmlFor="query-role" className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              Role Preset
            </label>
            <select
              id="query-role"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700"
            >
              {loadingPresets ? <option>Loading...</option> : null}
              {Object.keys(presets).map((preset) => (
                <option key={preset} value={preset}>
                  {preset}
                </option>
              ))}
            </select>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              Default lakes: {activePresetProjects.join(", ") || "none"}
            </p>
          </div>

          <div>
            <label htmlFor="query-projects" className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              Override Lakes
            </label>
            <Input
              id="query-projects"
              type="text"
              value={projectsInput}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setProjectsInput(e.target.value)}
              placeholder="devel-docs,cephalon-hive,sintel"
            />
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              Leave blank to use the selected role preset.
            </p>
          </div>

          <div>
            <label htmlFor="query-text" className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              Query
            </label>
            <Input
              id="query-text"
              value={query}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
              placeholder="Ask across the selected lakes..."
            />
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              Multi-line prompt editing is still a legacy surface and remains tracked in `knowledge-ops-legacy-ui-inventory.md`.
            </p>
          </div>

          <div>
            <label htmlFor="query-limit" className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              Limit
            </label>
            <Input
              id="query-limit"
              type="number"
              value={String(limit)}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setLimit(Number(e.target.value) || 5)}
            />
          </div>

          <div className="flex gap-2">
            <Button
              variant="primary"
              onClick={runSearch}
              disabled={searchLoading || answerLoading || !query.trim()}
            >
              {searchLoading ? "Searching..." : "Search"}
            </Button>
            <Button
              variant="secondary"
              onClick={runAnswer}
              disabled={searchLoading || answerLoading || !query.trim()}
            >
              {answerLoading ? "Thinking..." : "Synthesize"}
            </Button>
          </div>

          <div className="rounded-md bg-slate-50 p-3 text-xs text-slate-600 dark:bg-slate-900 dark:text-slate-300">
            <div className="font-medium">Effective lakes</div>
            <div className="mt-1 break-words">{effectiveProjects.join(", ") || "none"}</div>
          </div>
        </div>
      </Card>

      <section className="space-y-4">
        {error ? (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/40 dark:bg-red-950/30 dark:text-red-300">
            {error}
          </div>
        ) : null}

        <Card variant="default" title="Synthesized Answer">
          <div className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700 dark:text-slate-200">
            {answerResult?.answer || "Run “Synthesize” to produce an answer from the selected lakes."}
          </div>
        </Card>

        <Card
          variant="default"
          title="Raw Hits"
          extra={
            <span className="text-sm text-slate-500 dark:text-slate-400">
              {searchResult ? `${searchResult.count} result(s)` : "No search yet"}
            </span>
          }
        >

          <div className="mt-4 space-y-3">
            {searchResult?.rows?.length ? (
              searchResult.rows.map((row) => (
                <Card key={row.id} variant="outlined" padding="sm">
                  <div className="flex flex-wrap gap-2 text-xs">
                    <span className="rounded bg-slate-100 px-2 py-0.5 dark:bg-slate-700">{row.project || "unknown-project"}</span>
                    <span className="rounded bg-slate-100 px-2 py-0.5 dark:bg-slate-700">{row.kind || "unknown-kind"}</span>
                    <span className="rounded bg-slate-100 px-2 py-0.5 dark:bg-slate-700">{row.source || "unknown-source"}</span>
                    {row.tier ? <span className="rounded bg-blue-100 px-2 py-0.5 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">{row.tier}</span> : null}
                  </div>
                  <div className="mt-2 whitespace-pre-wrap text-sm text-slate-700 dark:text-slate-200">
                    {row.snippet || row.text || "(no snippet)"}
                  </div>
                  <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                    {row.ts || "no timestamp"}
                  </div>
                </Card>
              ))
            ) : (
              <div className="rounded-md bg-slate-50 px-4 py-6 text-sm text-slate-500 dark:bg-slate-900 dark:text-slate-400">
                No results yet.
              </div>
            )}
          </div>
        </Card>
      </section>
    </div>
  );
}

export default QueryPage;
