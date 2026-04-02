import { useEffect, useState } from "react";
import { Button, Card } from '@devel/ui-react';

type Garden = {
  garden_id: string;
  title: string;
  purpose: string;
  lakes: string[];
  views: string[];
  actions: string[];
  outputs: string[];
};

type GardensResponse = {
  ok: boolean;
  count: number;
  gardens: Garden[];
};

const gardenLinks: Record<string, string> = {
  "devel-deps-garden": "http://127.0.0.1:8798/",
  "truth-workbench": "http://127.0.0.1:8790/workbench",
  query: "/query",
  ingestion: "/ingestion",
};

export default function GardensPage() {
  const [data, setData] = useState<GardensResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    fetch("/api/query/gardens")
      .then((resp) => {
        if (!resp.ok) throw new Error(`Failed to load gardens: ${resp.status}`);
        return resp.json() as Promise<GardensResponse>;
      })
      .then((body) => {
        if (!cancelled) setData(body);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Gardens</h1>
        <p className="mt-1 text-sm text-slate-500">
          Publishable operator views over the same OpenPlanner runtime.
        </p>
      </div>

      {loading ? (
        <Card variant="default" padding="md">
          <div className="text-sm text-slate-500">Loading gardens...</div>
        </Card>
      ) : null}

      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/40 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2">
        {data?.gardens?.map((garden) => {
          const href = gardenLinks[garden.garden_id];
          return (
            <Card
              key={garden.garden_id}
              variant="elevated"
              padding="md"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">{garden.title}</h2>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                    {garden.purpose}
                  </p>
                </div>
                <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-slate-700 dark:text-slate-200">
                  {garden.garden_id}
                </span>
              </div>

              <div className="mt-4 space-y-3 text-sm">
                <div>
                  <div className="mb-1 font-medium text-slate-700 dark:text-slate-200">Lakes</div>
                  <div className="flex flex-wrap gap-2">
                    {garden.lakes.map((lake) => (
                      <span key={lake} className="rounded bg-slate-100 px-2 py-0.5 text-xs dark:bg-slate-700">
                        {lake}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="mb-1 font-medium text-slate-700 dark:text-slate-200">Views</div>
                  <div className="text-slate-600 dark:text-slate-300">{garden.views.join(", ")}</div>
                </div>

                <div>
                  <div className="mb-1 font-medium text-slate-700 dark:text-slate-200">Actions</div>
                  <div className="text-slate-600 dark:text-slate-300">{garden.actions.join(", ")}</div>
                </div>
              </div>

              <div className="mt-5 flex gap-2">
                {href ? (
                  <Button
                    onClick={() => {
                      if (href.startsWith('http')) {
                        window.open(href, '_blank', 'noopener,noreferrer');
                      } else {
                        window.location.assign(href);
                      }
                    }}
                    variant="primary"
                  >
                    Open Garden
                  </Button>
                ) : null}
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
