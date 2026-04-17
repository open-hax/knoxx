import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { EditorState, Extension, Compartment } from "@codemirror/state";
import { EditorView, keymap, placeholder as cmPlaceholder, lineNumbers, highlightActiveLineGutter, highlightActiveLine, drawSelection, rectangularSelection, highlightSpecialChars } from "@codemirror/view";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { syntaxHighlighting, bracketMatching, indentOnInput, foldKeymap, foldGutter, HighlightStyle } from "@codemirror/language";
import { clojure } from "@nextjournal/lang-clojure";
import { closeBrackets, closeBracketsKeymap } from "@codemirror/autocomplete";
import { linter, Diagnostic } from "@codemirror/lint";
import { searchKeymap, highlightSelectionMatches } from "@codemirror/search";
import { tags as t } from "@lezer/highlight";
import { monokai, type ThemePalette, tokens, fontFamily, fontSize, spacing } from "@open-hax/uxx/tokens";

// Radius values matching the Monokai theme pack
const radius = { xs: "2px", sm: "4px", md: "6px", lg: "8px" } as const;

// ── EDN parse validation for CodeMirror ──────────────────────────────────────

function tryParseEdn(text: string): { ok: true } | { ok: false; error: string; line?: number } {
  try {
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

// ── Monokai-based Clojure/EDN highlight style using uxx palette ──────────────

function monokaiHighlight(palette: ThemePalette): HighlightStyle {
  return HighlightStyle.define([
    // Comments — muted gold
    { tag: t.comment, color: palette.accent.yellow, fontStyle: "italic" },
    { tag: t.lineComment, color: palette.accent.yellow, fontStyle: "italic" },
    { tag: t.blockComment, color: palette.accent.yellow, fontStyle: "italic" },
    { tag: t.docComment, color: palette.accent.yellow, fontStyle: "italic" },

    // Strings — warm yellow
    { tag: t.string, color: palette.accent.yellow },
    { tag: t.special(t.string), color: palette.accent.orange },

    // Keywords — clojure keywords (:foo) as magenta/purple
    { tag: t.keyword, color: palette.accent.magenta },
    { tag: t.controlKeyword, color: palette.accent.magenta },
    { tag: t.operatorKeyword, color: palette.accent.magenta },
    { tag: t.special(t.keyword), color: palette.accent.red },

    // Numbers — orange
    { tag: t.number, color: palette.accent.orange },
    { tag: t.integer, color: palette.accent.orange },
    { tag: t.float, color: palette.accent.orange },

    // Booleans, null — red/magenta
    { tag: t.bool, color: palette.accent.red },
    { tag: t.null, color: palette.accent.magenta },

    // Operators — red
    { tag: t.operator, color: palette.accent.red },
    { tag: t.definitionOperator, color: palette.accent.red },
    { tag: t.typeOperator, color: palette.accent.magenta },

    // Function names — green
    { tag: t.function(t.variableName), color: palette.accent.green },
    { tag: t.definition(t.function(t.variableName)), color: palette.accent.green },

    // Variables — bright
    { tag: t.variableName, color: palette.fg.bright },
    { tag: t.definition(t.variableName), color: palette.accent.green },
    { tag: t.special(t.variableName), color: palette.accent.cyan },

    // Types / class names — cyan
    { tag: t.typeName, color: palette.accent.cyan },
    { tag: t.className, color: palette.accent.cyan },
    { tag: t.labelName, color: palette.accent.cyan },
    { tag: t.namespace, color: palette.accent.cyan },

    // Property names — cyan
    { tag: t.propertyName, color: palette.accent.cyan },
    { tag: t.attributeName, color: palette.accent.cyan },

    // Punctuation — soft
    { tag: t.punctuation, color: palette.fg.soft },
    { tag: t.separator, color: palette.fg.soft },
    { tag: t.bracket, color: palette.fg.soft },
    { tag: t.angleBracket, color: palette.fg.soft },

    // Special — red for meta, macros
    { tag: t.meta, color: palette.accent.red },
    { tag: t.processingInstruction, color: palette.accent.red },

    // Tags (EDN #inst, #uuid etc.) — red
    { tag: t.special(t.string), color: palette.accent.red },

    // Standard defaults
    { tag: t.content, color: palette.fg.default },
    { tag: t.heading, color: palette.accent.green, fontWeight: "bold" },
    { tag: t.link, color: palette.accent.cyan, textDecoration: "underline" },
    { tag: t.emphasis, fontStyle: "italic" },
    { tag: t.strong, fontWeight: "bold" },
    { tag: t.strikethrough, textDecoration: "line-through" },
    { tag: t.inserted, color: palette.accent.green },
    { tag: t.deleted, color: palette.accent.red },
    { tag: t.changed, color: palette.accent.orange },
  ]);
}

// ── Editor theme from uxx tokens ─────────────────────────────────────────────

function uxxEditorTheme(palette: ThemePalette) {
  return EditorView.theme({
    "&": {
      backgroundColor: palette.bg.default,
      color: palette.fg.default,
      fontSize: fontSize.sm,
      height: "100%",
    },
    ".cm-content": {
      fontFamily: fontFamily.mono,
      caretColor: palette.accent.cyan,
      padding: "8px 0",
    },
    ".cm-cursor": {
      borderLeftColor: palette.accent.cyan,
      borderLeftWidth: "2px",
    },
    ".cm-selectionBackground, &.cm-focused .cm-selectionBackground": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.cyan)}, 0.18)`,
    },
    ".cm-gutters": {
      backgroundColor: palette.bg.darker,
      color: palette.fg.muted,
      border: "none",
      paddingRight: "4px",
    },
    ".cm-activeLineGutter": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.cyan)}, 0.06)`,
      color: palette.fg.soft,
    },
    ".cm-activeLine": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.cyan)}, 0.04)`,
    },
    ".cm-matchingBracket": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.cyan)}, 0.25)`,
      outline: `1px solid rgba(${hexToRgb(palette.accent.cyan)}, 0.5)`,
      color: palette.fg.bright,
    },
    ".cm-nonmatchingBracket": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.red)}, 0.25)`,
      outline: `1px solid rgba(${hexToRgb(palette.accent.red)}, 0.5)`,
    },
    ".cm-lintRange-error": {
      backgroundImage: "none",
      borderBottom: `2px wavy ${palette.accent.red}`,
    },
    ".cm-lintRange-warning": {
      backgroundImage: "none",
      borderBottom: `2px wavy ${palette.accent.orange}`,
    },
    ".cm-tooltip": {
      backgroundColor: palette.bg.darker,
      border: `1px solid ${palette.fg.subtle}`,
      color: palette.fg.default,
      borderRadius: radius.md,
    },
    ".cm-tooltip.cm-tooltip-lint": {
      borderRadius: radius.md,
    },
    ".cm-tooltip.cm-tooltip-autocomplete": {
      borderRadius: radius.md,
      "& > ul > li": {
        padding: "4px 8px",
      },
      "& > ul > li[aria-selected]": {
        backgroundColor: `rgba(${hexToRgb(palette.accent.cyan)}, 0.15)`,
        color: palette.fg.bright,
      },
    },
    ".cm-foldGutter": {
      width: "16px",
    },
    ".cm-foldPlaceholder": {
      backgroundColor: palette.bg.lighter,
      border: `1px solid ${palette.fg.subtle}`,
      color: palette.fg.muted,
      padding: "0 4px",
      borderRadius: radius.xs,
    },
    ".cm-scroller": {
      overflow: "auto",
    },
    ".cm-searchMatch": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.yellow)}, 0.25)`,
      outline: `1px solid rgba(${hexToRgb(palette.accent.yellow)}, 0.4)`,
    },
    ".cm-searchMatch.cm-searchMatch-selected": {
      backgroundColor: `rgba(${hexToRgb(palette.accent.orange)}, 0.35)`,
    },
    ".cm-panels": {
      backgroundColor: palette.bg.darker,
      borderBottom: `1px solid ${palette.fg.subtle}`,
      color: palette.fg.default,
    },
    ".cm-panels input, .cm-panels button": {
      fontFamily: fontFamily.sans,
      fontSize: fontSize.xs,
    },
  }, { dark: true });
}

// ── Hex to RGB helper ────────────────────────────────────────────────────────

function hexToRgb(hex: string): string {
  const raw = hex.replace("#", "");
  const r = parseInt(raw.slice(0, 2), 16);
  const g = parseInt(raw.slice(2, 4), 16);
  const b = parseInt(raw.slice(4, 6), 16);
  return `${r}, ${g}, ${b}`;
}

// ── Inline IDE chrome (toolbar + status bar) ─────────────────────────────────
// These use uxx tokens directly. When EditorToolbar/EditorStatusBar are
// available from @open-hax/uxx/primitives, replace these.

interface ToolbarAction {
  key: string;
  label: string;
  title?: string;
  icon?: string;
  disabled?: boolean;
  onClick?: () => void;
}

function EditorToolbarInline({ actions, onValidate }: {
  actions: ToolbarAction[];
  onValidate?: () => void;
}) {
  const palette = monokai;
  return (
    <div
      data-component="edn-editor-toolbar"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 2,
        padding: "4px 8px",
        background: palette.bg.darker,
        borderBottom: `1px solid ${palette.fg.subtle}`,
        fontSize: fontSize.xs,
      }}
    >
      <span style={{ color: palette.accent.cyan, fontWeight: 600, marginRight: 8, fontFamily: fontFamily.mono }}>
        EDN
      </span>
      {actions.map((action) => (
        <button
          key={action.key}
          type="button"
          title={action.title}
          disabled={action.disabled}
          onClick={action.onClick}
          style={{
            background: "none",
            border: "none",
            padding: "3px 6px",
            borderRadius: radius.xs,
            color: palette.fg.soft,
            cursor: action.disabled ? "not-allowed" : "pointer",
            fontFamily: fontFamily.mono,
            fontSize: fontSize.xs,
            opacity: action.disabled ? 0.4 : 0.8,
            transition: "opacity 0.15s, color 0.15s",
          }}
          onMouseEnter={(e) => {
            if (!action.disabled) {
              (e.target as HTMLElement).style.opacity = "1";
              (e.target as HTMLElement).style.color = palette.accent.cyan;
            }
          }}
          onMouseLeave={(e) => {
            (e.target as HTMLElement).style.opacity = action.disabled ? "0.4" : "0.8";
            (e.target as HTMLElement).style.color = palette.fg.soft;
          }}
        >
          {action.icon ? `${action.icon} ` : ""}{action.label}
        </button>
      ))}
      {onValidate && (
        <button
          type="button"
          title="Validate EDN (Ctrl+Shift+V)"
          onClick={onValidate}
          style={{
            background: "none",
            border: `1px solid ${palette.fg.subtle}`,
            padding: "2px 8px",
            borderRadius: radius.xs,
            color: palette.accent.green,
            cursor: "pointer",
            fontFamily: fontFamily.mono,
            fontSize: fontSize.xs,
            marginLeft: "auto",
          }}
        >
          ✓ Validate
        </button>
      )}
    </div>
  );
}

interface StatusItem {
  key: string;
  label: string;
  color?: string;
  align?: "start" | "end";
}

function EditorStatusBarInline({ items }: { items: StatusItem[] }) {
  const palette = monokai;
  return (
    <div
      data-component="edn-editor-statusbar"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 16,
        padding: "3px 12px",
        background: palette.bg.darker,
        borderTop: `1px solid ${palette.fg.subtle}`,
        fontSize: fontSize.xs,
        color: palette.fg.muted,
        fontFamily: fontFamily.mono,
      }}
    >
      {items.map((item, index) => {
        const isFirstEnd = item.align === "end" && items.slice(0, index).every((i) => i.align !== "end");
        return (
          <span
            key={item.key}
            style={{
              color: item.color ?? palette.fg.muted,
              marginLeft: isFirstEnd ? "auto" : undefined,
            }}
          >
            {item.label}
          </span>
        );
      })}
    </div>
  );
}

// ── Component ────────────────────────────────────────────────────────────────

interface EdnEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  className?: string;
  height?: string;
  externalErrors?: Array<{ line?: number; message: string }>;
  onValidate?: () => void;
  fileName?: string;
}

export function EdnEditor({
  value,
  onChange,
  placeholder,
  readOnly = false,
  className,
  height = "400px",
  externalErrors = [],
  onValidate,
  fileName,
}: EdnEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  const readOnlyCompartment = useRef(new Compartment());

  const palette = monokai;

  // Keep onChange ref current
  useEffect(() => { onChangeRef.current = onChange; }, [onChange]);

  // External lint diagnostics
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
      uxxEditorTheme(palette),
      syntaxHighlighting(monokaiHighlight(palette)),
      history(),
      clojure(),
      lineNumbers(),
      highlightActiveLineGutter(),
      highlightActiveLine(),
      highlightSpecialChars(),
      drawSelection(),
      rectangularSelection(),
      indentOnInput(),
      bracketMatching(),
      closeBrackets(),
      highlightSelectionMatches(),
      foldGutter(),
      ednLinter(),
      externalLinter,
      keymap.of([
        ...closeBracketsKeymap,
        ...defaultKeymap,
        ...searchKeymap,
        ...historyKeymap,
        ...foldKeymap,
        indentWithTab,
        {
          key: "Mod-Shift-v",
          run: () => { onValidate?.(); return true; },
        },
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

  // Status bar info
  const [statusItems, setStatusItems] = useState<StatusItem[]>([]);

  useEffect(() => {
    const view = viewRef.current;
    if (!view) return;

    const doc = view.state.doc;
    const lines = doc.lines;
    const chars = doc.length;
    const cursor = view.state.selection.main.head;
    const cursorLine = doc.lineAt(cursor).number;
    const cursorCol = cursor - doc.lineAt(cursor).from + 1;

    const errCount = externalErrors.length;
    const lintResult = tryParseEdn(doc.toString());
    const parseOk = lintResult.ok;

    const items: StatusItem[] = [
      { key: "cursor", label: `Ln ${cursorLine}, Col ${cursorCol}`, align: "end" as const },
      { key: "lines", label: `${lines} lines` },
      { key: "chars", label: `${chars} chars` },
    ];

    if (errCount > 0) {
      items.unshift({ key: "errors", label: `✕ ${errCount} error${errCount > 1 ? "s" : ""}`, color: palette.accent.red });
    } else if (parseOk) {
      items.unshift({ key: "valid", label: "✓ valid", color: palette.accent.green });
    }

    if (fileName) {
      items.unshift({ key: "file", label: fileName });
    }

    setStatusItems(items);
  }, [value, externalErrors, fileName, palette]);

  // Toolbar actions
  const toolbarActions: ToolbarAction[] = useMemo(() => [
    { key: "undo", label: "↶", title: "Undo (Ctrl+Z)", onClick: () => viewRef.current?.dispatch({ effects: [] }) },
    { key: "redo", label: "↷", title: "Redo (Ctrl+Shift+Z)", onClick: () => viewRef.current?.dispatch({ effects: [] }) },
    { key: "fold", label: "◃", title: "Fold all", onClick: () => {
      // Fold all top-level forms
      const view = viewRef.current;
      if (!view) return;
      const cmd = keymap.of([{ key: "Ctrl-Alt-[", run: () => true }]); // placeholder
    }},
    { key: "format", label: "{}", title: "Reformat (coming soon)", disabled: true },
  ], []);

  return (
    <div
      className={className}
      style={{
        display: "flex",
        flexDirection: "column",
        height,
        background: palette.bg.default,
        border: `1px solid ${palette.fg.subtle}`,
        borderRadius: radius.md,
        overflow: "hidden",
      }}
    >
      <EditorToolbarInline actions={toolbarActions} onValidate={onValidate} />
      <div
        ref={containerRef}
        style={{ flex: 1, overflow: "hidden" }}
      />
      <EditorStatusBarInline items={statusItems} />
    </div>
  );
}
