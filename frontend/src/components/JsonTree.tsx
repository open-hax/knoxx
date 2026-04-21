import React, { useMemo } from 'react';

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function previewPrimitive(value: unknown): string {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') {
    const trimmed = value.length > 120 ? `${value.slice(0, 117)}…` : value;
    return JSON.stringify(trimmed);
  }
  if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') return String(value);
  return '';
}

function previewValue(value: unknown): string {
  if (Array.isArray(value)) return `Array(${value.length})`;
  if (isPlainObject(value)) return `Object(${Object.keys(value).length})`;
  return previewPrimitive(value);
}

export function JsonTree(props: {
  label?: string;
  value: unknown;
  defaultExpandDepth: number;
  depth?: number;
  path?: string;
}) {
  const depth = props.depth ?? 0;
  const path = props.path ?? props.label ?? '';

  const shouldOpen = depth < props.defaultExpandDepth;
  const summaryLabel = props.label ? `${props.label}: ` : '';

  const rendered = useMemo(() => {
    const value = props.value;

    if (Array.isArray(value)) {
      return (
        <details className="pl-2" open={shouldOpen}>
          <summary className="cursor-pointer select-none text-xs text-slate-200">
            <span className="text-slate-400">{summaryLabel}</span>
            <span className="text-slate-300">{previewValue(value)}</span>
          </summary>
          <ol className="pl-5 mt-1 space-y-1 list-decimal">
            {value.map((item, idx) => (
              <li key={`${path}[${idx}]`} className="text-xs text-slate-300">
                <JsonTree
                  value={item}
                  label={String(idx)}
                  defaultExpandDepth={props.defaultExpandDepth}
                  depth={depth + 1}
                  path={`${path}[${idx}]`}
                />
              </li>
            ))}
          </ol>
        </details>
      );
    }

    if (isPlainObject(value)) {
      const entries = Object.entries(value);
      entries.sort(([a], [b]) => a.localeCompare(b));
      return (
        <details className="pl-2" open={shouldOpen}>
          <summary className="cursor-pointer select-none text-xs text-slate-200">
            <span className="text-slate-400">{summaryLabel}</span>
            <span className="text-slate-300">{previewValue(value)}</span>
          </summary>
          <div className="mt-1 space-y-1">
            {entries.map(([k, v]) => (
              <div key={`${path}.${k}`} className="border-l border-slate-700/30">
                <JsonTree
                  label={k}
                  value={v}
                  defaultExpandDepth={props.defaultExpandDepth}
                  depth={depth + 1}
                  path={`${path}.${k}`}
                />
              </div>
            ))}
          </div>
        </details>
      );
    }

    return (
      <details className="pl-2" open={shouldOpen}>
        <summary className="cursor-pointer select-none text-xs text-slate-200">
          <span className="text-slate-400">{summaryLabel}</span>
          <span className="text-slate-300">{previewValue(value)}</span>
        </summary>
        <div className="mt-1 text-xs text-slate-300 font-mono whitespace-pre-wrap">
          {previewPrimitive(value) || String(value)}
        </div>
      </details>
    );
  }, [props.value, props.defaultExpandDepth, depth, path, shouldOpen, summaryLabel]);

  return rendered;
}
