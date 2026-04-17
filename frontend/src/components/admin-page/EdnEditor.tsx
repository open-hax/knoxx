import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { EditorState, Extension, Compartment } from "@codemirror/state";
import { EditorView, keymap, placeholder as cmPlaceholder } from "@codemirror/view";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching } from "@codemirror/language";
import { clojure } from "@nextjournal/lang-clojure";
import { closeBrackets, closeBracketsKeymap } from "@codemirror/autocomplete";
import { linter, Diagnostic } from "@codemirror/lint";
import { searchKeymap } from "@codemirror/search";
import { classNames } from "./common";

// ── EDN parse validation for CodeMirror ──────────────────────────────────────

function tryParseEdn(text: string): { ok: true } | { ok: false; error: string; line?: number } {
  try {
    // Basic bracket matching as a first pass
    let depth = 0;
    let inString = false;
    let inComment = false;
    let line = 1;
    for (let i = 0; i < text.length; i++) {
      const ch = text[i];
      if (ch === "\n") { line++; inComment = false; continue; }
      if (inComment) continue;
      if (inString) {
        if (ch === "\\") { i++; continue; }
        if (ch === "\"") inString = false;
        continue;
      }
      if (ch === ";") { inComment = true; continue; }
      if (ch === "\"") { inString = true; continue; }
      if (ch === "{" || ch === "(" || ch === "[") depth++;
      if (ch === "}" || ch === ")" || ch === "]") depth--;
      if (depth < 0) return { ok: false, error: "Unexpected closing bracket", line };
    }
    if (depth > 0) return { ok: false, error: `Unclosed bracket(s): depth ${depth}`, line };
    if (inString) return { ok: false, error: "Unclosed string", line };
    return { ok: true };
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) };
  }
}

function ednLinter(): Extension {
  return linter((view): Diagnostic[] => {
    const text = view.state.doc.toString();
    const result = tryParseEdn(text);
    if (result.ok) return [];
    const line = result.line ?? 1;
    const lineInfo = view.state.doc.line(Math.min(line, view.state.doc.lines));
    return [{
      from: lineInfo.from,
      to: lineInfo.to,
      severity: "error",
      message: result.error,
    }];
  });
}

// ── Dark theme for CodeMirror ────────────────────────────────────────────────

const darkTheme = EditorView.theme({
  "&": {
    backgroundColor: "transparent",
    color: "#e2e8f0",
    fontSize: "13px",
  },
  ".cm-content": {
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace",
    caretColor: "#38bdf8",
    padding: "8px 0",
  },
  ".cm-cursor": {
    borderLeftColor: "#38bdf8",
  },
  ".cm-selectionBackground, &.cm-focused .cm-selectionBackground": {
    backgroundColor: "rgba(56, 189, 248, 0.15)",
  },
  ".cm-gutters": {
    backgroundColor: "rgba(15, 23, 42, 0.5)",
    color: "#64748b",
    border: "none",
    paddingRight: "4px",
  },
  ".cm-activeLineGutter": {
    backgroundColor: "rgba(56, 189, 248, 0.08)",
    color: "#94a3b8",
  },
  ".cm-activeLine": {
    backgroundColor: "rgba(56, 189, 248, 0.04)",
  },
  ".cm-matchingBracket": {
    backgroundColor: "rgba(56, 189, 248, 0.25)",
    outline: "1px solid rgba(56, 189, 248, 0.5)",
  },
  ".cm-lintRange-error": {
    backgroundImage: "none",
    borderBottom: "2px wavy #f43f5e",
  },
  ".cm-lintRange-warning": {
    backgroundImage: "none",
    borderBottom: "2px wavy #f59e0b",
  },
  ".cm-tooltip": {
    backgroundColor: "#1e293b",
    border: "1px solid #334155",
    color: "#e2e8f0",
  },
  ".cm-tooltip.cm-tooltip-lint": {
    borderRadius: "8px",
  },
}, { dark: true });

// ── Component ────────────────────────────────────────────────────────────────

interface EdnEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  className?: string;
  height?: string;
  externalErrors?: Array<{ line?: number; message: string }>;
}

export function EdnEditor({
  value,
  onChange,
  placeholder,
  readOnly = false,
  className,
  height = "400px",
  externalErrors = [],
}: EdnEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  const readOnlyCompartment = useRef(new Compartment());
  const externalLintsCompartment = useRef(new Compartment());

  // Keep onChange ref current
  useEffect(() => { onChangeRef.current = onChange; }, [onChange]);

  // External lint diagnostics compartment
  const externalLinter = useMemo(() => {
    return linter((): Diagnostic[] => {
      return externalErrors.map((err) => {
        const line = err.line ?? 1;
        const doc = viewRef.current?.state.doc;
        const lineInfo = doc ? doc.line(Math.min(line, doc.lines)) : null;
        return {
          from: lineInfo?.from ?? 0,
          to: lineInfo?.to ?? 1,
          severity: "error" as const,
          message: err.message,
        };
      });
    });
  }, [externalErrors]);

  // Create editor
  useEffect(() => {
    if (!containerRef.current) return;

    const extensions: Extension[] = [
      darkTheme,
      history(),
      clojure(),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      bracketMatching(),
      closeBrackets(),
      ednLinter(),
      keymap.of([
        ...closeBracketsKeymap,
        ...defaultKeymap,
        ...searchKeymap,
        ...historyKeymap,
        indentWithTab,
      ]),
      readOnlyCompartment.current.of(EditorState.readOnly.of(readOnly)),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          onChangeRef.current(update.state.doc.toString());
        }
      }),
    ];

    if (placeholder) {
      extensions.push(cmPlaceholder(placeholder));
    }

    const state = EditorState.create({
      doc: value,
      extensions,
    });

    const view = new EditorView({
      state,
      parent: containerRef.current,
    });

    viewRef.current = view;
    return () => {
      view.destroy();
      viewRef.current = null;
    };
    // Only re-create on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync external value changes (not from user typing)
  const lastExternalValue = useRef(value);
  useEffect(() => {
    if (!viewRef.current) return;
    if (value === lastExternalValue.current) return;
    lastExternalValue.current = value;
    viewRef.current.dispatch({
      changes: {
        from: 0,
        to: viewRef.current.state.doc.length,
        insert: value,
      },
    });
  }, [value]);

  // Update read-only
  useEffect(() => {
    if (!viewRef.current) return;
    viewRef.current.dispatch({
      effects: readOnlyCompartment.current.reconfigure(
        EditorState.readOnly.of(readOnly),
      ),
    });
  }, [readOnly]);

  return (
    <div
      ref={containerRef}
      className={classNames("overflow-hidden rounded-lg border border-slate-800", className)}
      style={{ height }}
    />
  );
}
