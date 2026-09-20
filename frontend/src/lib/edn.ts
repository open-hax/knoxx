import type { Extension } from "@codemirror/state";
import { EditorView } from "@codemirror/view";
import { HighlightStyle } from "@codemirror/language";
import { linter, type Diagnostic } from "@codemirror/lint";
import { tags as t } from "@lezer/highlight";
import { radius, fontFamily, fontSize, withAlpha, type ThemePalette } from "@open-hax/uxx/tokens";

/**
 * Simple EDN parser for view-contract.edn files.
 *
 * This is a minimal parser sufficient for the view-contract schema.
 * It handles keywords, strings, maps, vectors, and basic values.
 */

export function parseEdn(edn: string): unknown {
  const tokens = tokenize(edn);
  const { value } = parseValue(tokens, 0);
  return value;
}

function tokenize(edn: string): string[] {
  const tokens: string[] = [];
  let i = 0;
  while (i < edn.length) {
    const ch = edn[i];
    if (whitespace(ch)) {
      i++;
    } else if (ch === ";" && edn[i + 1] === ";") {
      // Skip comment
      while (i < edn.length && edn[i] !== "\n") i++;
    } else if (ch === '"') {
      let str = '"';
      i++;
      while (i < edn.length && edn[i] !== '"') {
        if (edn[i] === "\\") {
          str += edn[i];
          i++;
        }
        str += edn[i];
        i++;
      }
      str += '"';
      tokens.push(str);
      i++;
    } else if (ch === ":") {
      let kw = ":";
      i++;
      while (i < edn.length && !terminator(edn[i])) {
        kw += edn[i];
        i++;
      }
      tokens.push(kw);
    } else if (ch === "{" || ch === "}" || ch === "[" || ch === "]") {
      tokens.push(ch);
      i++;
    } else {
      let atom = "";
      while (i < edn.length && !terminator(edn[i])) {
        atom += edn[i];
        i++;
      }
      if (atom) tokens.push(atom);
    }
  }
  return tokens;
}

function whitespace(ch: string): boolean {
  return ch === " " || ch === "\n" || ch === "\t" || ch === "," || ch === "\r";
}

function terminator(ch: string): boolean {
  return whitespace(ch) || ch === "{" || ch === "}" || ch === "[" || ch === "]" || ch === undefined;
}

function parseValue(tokens: string[], i: number): { value: unknown; nextIndex: number } {
  if (i >= tokens.length) return { value: null, nextIndex: i };
  const token = tokens[i];

  if (token === "{") {
    return parseMap(tokens, i + 1);
  }
  if (token === "[") {
    return parseVector(tokens, i + 1);
  }
  if (token.startsWith('"')) {
    return { value: token.slice(1, -1), nextIndex: i + 1 };
  }
  if (token.startsWith(":")) {
    return { value: token, nextIndex: i + 1 };
  }
  if (token === "true") return { value: true, nextIndex: i + 1 };
  if (token === "false") return { value: false, nextIndex: i + 1 };
  if (token === "nil") return { value: null, nextIndex: i + 1 };
  const num = Number(token);
  if (!Number.isNaN(num)) return { value: num, nextIndex: i + 1 };
  return { value: token, nextIndex: i + 1 };
}

function parseMap(tokens: string[], i: number): { value: Record<string, unknown>; nextIndex: number } {
  const map: Record<string, unknown> = {};
  while (i < tokens.length && tokens[i] !== "}") {
    const keyToken = tokens[i];
    if (!keyToken?.startsWith(":")) {
      // Skip invalid key
      i++;
      continue;
    }
    const key = keyToken.slice(1).replace(/-/g, "_"); // Convert :view-id to view_id
    i++;
    const { value, nextIndex } = parseValue(tokens, i);
    map[key] = value;
    i = nextIndex;
  }
  return { value: map, nextIndex: i + 1 };
}

function parseVector(tokens: string[], i: number): { value: unknown[]; nextIndex: number } {
  const vec: unknown[] = [];
  while (i < tokens.length && tokens[i] !== "]") {
    const { value, nextIndex } = parseValue(tokens, i);
    vec.push(value);
    i = nextIndex;
  }
  return { value: vec, nextIndex: i + 1 };
}

export function serializeEdn(value: unknown): string {
  if (value === null || value === undefined) return "nil";
  if (typeof value === "string") return `"${value}"`;
  if (typeof value === "number") return String(value);
  if (typeof value === "boolean") return String(value);
  if (Array.isArray(value)) {
    return `[${value.map(serializeEdn).join(" ")}]`;
  }
  if (typeof value === "object") {
    const entries = Object.entries(value).map(([k, v]) => {
      const key = k.replace(/_/g, "-");
      return `:${key} ${serializeEdn(v)}`;
    });
    return `{${entries.join(" ")}}`;
  }
  return String(value);
}

// ── EDN parse validation for CodeMirror ──────────────────────────────────────

export function tryParseEdn(text: string): { ok: true } | { ok: false; error: string; line?: number } {
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

export function ednLinter(): Extension {
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

export function monokaiHighlight(palette: ThemePalette): HighlightStyle {
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

export function uxxEditorTheme(palette: ThemePalette) {
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
      backgroundColor: withAlpha(palette.accent.cyan, 0.18),
    },
    ".cm-gutters": {
      backgroundColor: palette.bg.darker,
      color: palette.fg.muted,
      border: "none",
      paddingRight: "4px",
    },
    ".cm-activeLineGutter": {
      backgroundColor: withAlpha(palette.accent.cyan, 0.06),
      color: palette.fg.soft,
    },
    ".cm-activeLine": {
      backgroundColor: withAlpha(palette.accent.cyan, 0.04),
    },
    ".cm-matchingBracket": {
      backgroundColor: withAlpha(palette.accent.cyan, 0.25),
      outline: `1px solid ${withAlpha(palette.accent.cyan, 0.5)}`,
      color: palette.fg.bright,
    },
    ".cm-nonmatchingBracket": {
      backgroundColor: withAlpha(palette.accent.red, 0.25),
      outline: `1px solid ${withAlpha(palette.accent.red, 0.5)}`,
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
        backgroundColor: withAlpha(palette.accent.cyan, 0.15),
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
      backgroundColor: withAlpha(palette.accent.yellow, 0.25),
      outline: `1px solid ${withAlpha(palette.accent.yellow, 0.4)}`,
    },
    ".cm-searchMatch.cm-searchMatch-selected": {
      backgroundColor: withAlpha(palette.accent.orange, 0.35),
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
