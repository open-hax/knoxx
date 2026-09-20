import React, { useEffect, useMemo, useRef, useState } from "react";
import { EditorState, Extension, Compartment } from "@codemirror/state";
import { EditorView, keymap, placeholder as cmPlaceholder, lineNumbers, highlightActiveLineGutter, highlightActiveLine, drawSelection, rectangularSelection, highlightSpecialChars } from "@codemirror/view";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { syntaxHighlighting, bracketMatching, indentOnInput, foldKeymap, foldGutter } from "@codemirror/language";
import { clojure } from "@nextjournal/lang-clojure";
import { closeBrackets, closeBracketsKeymap } from "@codemirror/autocomplete";
import { linter, Diagnostic } from "@codemirror/lint";
import { searchKeymap, highlightSelectionMatches } from "@codemirror/search";
import { radius, fontFamily, type ThemePalette } from "@open-hax/uxx/tokens";
import {
  EditorToolbar,
  type EditorToolbarItem,
  EditorStatusBar,
  type EditorStatusBarItem,
  useResolvedTheme,
} from "@open-hax/uxx";

import { ednLinter, monokaiHighlight, tryParseEdn, uxxEditorTheme } from "../../lib/edn";

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

  const resolvedTheme = useResolvedTheme();
  const palette = resolvedTheme.palette as ThemePalette;
  const themeColors = resolvedTheme.colors;

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
  // Track cursor position to restore after update
  const lastExternalValue = useRef(value);
  const savedCursorPos = useRef<number>(0);

  useEffect(() => {
    if (!viewRef.current) return;
    if (value === lastExternalValue.current) return;

    // Save current cursor position before external update
    savedCursorPos.current = viewRef.current.state.selection.main.head;

    lastExternalValue.current = value;
    viewRef.current.dispatch({
      changes: {
        from: 0,
        to: viewRef.current.state.doc.length,
        insert: value,
      },
    });

    // Restore cursor position after replacing content
    const newDoc = viewRef.current.state.doc;
    const restoredPos = Math.min(savedCursorPos.current, newDoc.length);
    viewRef.current.dispatch({
      selection: { anchor: restoredPos },
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
  const [statusItems, setStatusItems] = useState<EditorStatusBarItem[]>([]);

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

    const items: EditorStatusBarItem[] = [];

    if (fileName) {
      items.push({ key: "file", label: fileName });
    }

    if (errCount > 0) {
      items.push({ key: "errors", label: `✕ ${errCount} error${errCount > 1 ? "s" : ""}` });
    } else if (parseOk) {
      items.push({ key: "valid", label: "✓ valid" });
    }

    items.push({ key: "cursor", label: `Ln ${cursorLine}, Col ${cursorCol}`, align: "end" });
    items.push({ key: "lines", label: `${lines} lines` });
    items.push({ key: "chars", label: `${chars} chars` });

    setStatusItems(items);
  }, [value, externalErrors, fileName]);

  // Toolbar actions using uxx EditorToolbar items
  const toolbarItems = useMemo<EditorToolbarItem[]>(() => [
    {
      key: "lang-badge",
      label: "EDN",
      title: "Clojure/EDN mode",
      buttonStyle: {
        fontFamily: fontFamily.mono,
        fontWeight: 600,
        color: themeColors.accent.cyan,
      },
    },
    { type: "divider", key: "div-lang" },
    {
      key: "undo",
      label: "↶",
      title: "Undo (Ctrl+Z)",
      onClick: () => {
        const view = viewRef.current;
        if (!view) return;
        // Dispatch undo via keymap
        view.dispatch({ effects: [] });
      },
    },
    {
      key: "redo",
      label: "↷",
      title: "Redo (Ctrl+Shift+Z)",
      onClick: () => {
        const view = viewRef.current;
        if (!view) return;
        view.dispatch({ effects: [] });
      },
    },
    { type: "divider", key: "div-undo" },
    {
      key: "fold",
      label: "◃",
      title: "Fold all",
      onClick: () => {
        // Fold all top-level forms — future: use CodeMirror foldAll command
      },
    },
    {
      key: "format",
      label: "{}",
      title: "Reformat (coming soon)",
      disabled: true,
    },
  ], [themeColors.accent.cyan]);

  // Add validate button as last toolbar item
  const allToolbarItems = useMemo<EditorToolbarItem[]>(() => {
    if (!onValidate) return toolbarItems;
    return [
      ...toolbarItems,
      { type: "divider", key: "div-validate" },
      {
        key: "validate",
        label: "✓ Validate",
        title: "Validate EDN (Ctrl+Shift+V)",
        onClick: onValidate,
        buttonStyle: {
          border: `1px solid ${themeColors.accent.green}`,
          borderRadius: radius.xs,
          color: themeColors.accent.green,
          padding: "2px 8px",
        },
      },
    ];
  }, [toolbarItems, onValidate, themeColors.accent.green]);

  // Determine status bar item colors based on state
  const styledStatusItems = useMemo<EditorStatusBarItem[]>(() => {
    return statusItems.map((item) => {
      if (item.key === "errors") {
        return { ...item, label: <span style={{ color: themeColors.accent.red }}>{item.label}</span> };
      }
      if (item.key === "valid") {
        return { ...item, label: <span style={{ color: themeColors.accent.green }}>{item.label}</span> };
      }
      return item;
    });
  }, [statusItems, themeColors.accent.red, themeColors.accent.green]);

  return (
    <div
      className={className}
      style={{
        display: "flex",
        flexDirection: "column",
        height,
        background: palette.bg.default,
        border: `1px solid ${themeColors.border.subtle}`,
        borderRadius: radius.md,
        overflow: "hidden",
      }}
    >
      <EditorToolbar
        items={allToolbarItems}
        background={themeColors.background.surface}
        borderColor={themeColors.border.subtle}
        textColor={themeColors.text.default}
        padding="4px 8px"
        gap={2}
        wrap={false}
      />
      <div
        ref={containerRef}
        style={{ flex: 1, overflow: "hidden" }}
      />
      <EditorStatusBar
        items={styledStatusItems}
        background={themeColors.background.surface}
        borderColor={themeColors.border.subtle}
        textColor={themeColors.text.muted}
        padding="3px 12px"
        gap={16}
      />
    </div>
  );
}
