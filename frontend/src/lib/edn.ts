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
