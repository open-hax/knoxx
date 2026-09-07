#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(scriptDir, "..");
const repoRoot = resolve(frontendRoot, "..");
const baselinePath = join(frontendRoot, "migration", "baseline.json");

function parseArgs(argv) {
  const args = { base: null, check: false, manifest: null, selfTest: false };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--check") args.check = true;
    else if (arg === "--self-test") args.selfTest = true;
    else if (arg === "--manifest") args.manifest = argv[++i];
    else if (arg === "--base") args.base = argv[++i];
    else throw new Error(`Unknown argument: ${arg}`);
  }
  return args;
}

function walkFiles(root) {
  const out = [];
  const visit = (dir) => {
    for (const name of readdirSync(dir).sort()) {
      const path = join(dir, name);
      const stat = statSync(path);
      if (stat.isDirectory()) visit(path);
      else if (stat.isFile()) out.push(path);
    }
  };
  visit(root);
  return out;
}

function repoPath(path) {
  return relative(repoRoot, path).split("\\").join("/");
}

function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|\s)\/\/.*$/gm, "$1");
}

function exportNames(source) {
  const names = [];
  const text = stripComments(source);
  const blocks = text.matchAll(
    /export\s*\{([\s\S]*?)\}\s*from\s*["'][^"']+["']/g,
  );
  for (const match of blocks) {
    for (const raw of match[1].split(",")) {
      const token = raw.trim();
      if (!token) continue;
      const alias = token.match(/(?:^|\s)as\s+([A-Za-z_$][\w$]*)$/);
      names.push(alias ? alias[1] : token.split(/\s+/).at(-1));
    }
  }
  return names.sort();
}

function bridgeRouteComponents(source) {
  return [...source.matchAll(/\(\$\s+app\/([A-Za-z0-9_.-]+)\)/g)]
    .map((match) => match[1])
    .sort();
}

function classifyIsland(path) {
  const lower = path.toLowerCase();
  if (lower.includes("chat-page") || lower.includes("chatpage")) return "chat";
  if (lower.includes("agent-audit") || lower.includes("agentspage")) return "agents";
  if (lower.includes("broadcast") || lower.includes("/studio/")) return "studio";
  if (lower.includes("cms") || lower.includes("editor")) return "cms";
  if (
    lower.includes("ingestion") ||
    lower.includes("datapage") ||
    lower.includes("documentspage") ||
    lower.includes("graph") ||
    lower.includes("vectors")
  ) {
    return "data";
  }
  if (
    lower.includes("ops") ||
    lower.includes("admin") ||
    lower.includes("settings") ||
    lower.includes("labels")
  ) {
    return "ops";
  }
  if (lower.includes("auth")) return "auth";
  return "shared";
}

function disposition(path) {
  const lower = path.toLowerCase();
  if (
    lower.includes("graphexplorer") ||
    lower.includes("visualeditor") ||
    lower.includes("edneditor") ||
    lower.includes("codemirror")
  ) {
    return "wrap";
  }
  if (
    lower.includes("settingspage.tsx") ||
    lower.includes("sourcedocpage.tsx") ||
    lower.includes("documentspage.tsx")
  ) {
    return "delete";
  }
  return "assess";
}

function ednString(value) {
  return JSON.stringify(value);
}

function ednKeyword(value) {
  return `:${value}`;
}

function recordToEdn(record) {
  const parts = Object.keys(record)
    .sort()
    .map((key) => {
      const value = record[key];
      let encoded;
      if (typeof value === "string" && value.startsWith(":")) encoded = value;
      else if (typeof value === "string") encoded = ednString(value);
      else if (typeof value === "boolean") encoded = value ? "true" : "false";
      else if (Number.isInteger(value)) encoded = String(value);
      else {
        throw new Error(
          `Unsupported manifest value for ${key}: ${String(value)}`,
        );
      }
      return `${ednKeyword(key)} ${encoded}`;
    });
  return `{${parts.join(" ")}}`;
}

function collect(root = frontendRoot) {
  const srcRoot = join(root, "src");
  const files = walkFiles(srcRoot);
  const tsx = files.filter((path) => path.endsWith(".tsx"));
  const ts = files.filter(
    (path) => path.endsWith(".ts") && !path.endsWith(".d.ts"),
  );
  const declarations = files.filter((path) => path.endsWith(".d.ts"));
  const cljs = files.filter((path) => path.endsWith(".cljs"));
  const cljc = files.filter((path) => path.endsWith(".cljc"));
  const legacyTests = [...tsx, ...ts].filter((path) =>
    /\.(?:test|spec)\.tsx?$/.test(path),
  );

  const appBridgePath = join(srcRoot, "bridge", "app.ts");
  const frontendBridgePath = join(srcRoot, "bridge", "index.ts");
  const appPath = join(srcRoot, "cljs", "knoxx", "frontend", "app.cljs");
  const appBridgeExports = exportNames(readFileSync(appBridgePath, "utf8"));
  const frontendBridgeExports = exportNames(
    readFileSync(frontendBridgePath, "utf8"),
  );
  const bridgedRoutes = bridgeRouteComponents(readFileSync(appPath, "utf8"));

  const sourceRecords = [...tsx, ...ts, ...declarations]
    .sort()
    .map((path) => ({
      "migration/disposition": `:${disposition(repoPath(path))}`,
      "migration/island": `:${classifyIsland(repoPath(path))}`,
      "migration/kind": ":legacy-source",
      "migration/language": path.endsWith(".tsx")
        ? ":tsx"
        : path.endsWith(".d.ts")
          ? ":declaration"
          : ":ts",
      "migration/path": repoPath(path),
      "migration/test?": /\.(?:test|spec)\.tsx?$/.test(path),
    }));

  const testRecords = legacyTests.sort().map((path) => ({
    "migration/island": `:${classifyIsland(repoPath(path))}`,
    "migration/kind": ":legacy-test",
    "migration/path": repoPath(path),
  }));

  const bridgeRecords = [
    ...appBridgeExports.map((name) => ({
      "migration/bridge": ":app",
      "migration/export": name,
      "migration/kind": ":bridge-export",
      "migration/path": "frontend/src/bridge/app.ts",
    })),
    ...frontendBridgeExports.map((name) => ({
      "migration/bridge": ":frontend",
      "migration/export": name,
      "migration/kind": ":bridge-export",
      "migration/path": "frontend/src/bridge/index.ts",
    })),
  ];

  const routeRecords = bridgedRoutes.map((component, index) => ({
    "migration/component": component,
    "migration/index": index,
    "migration/kind": ":bridge-route",
    "migration/path": "frontend/src/cljs/knoxx/frontend/app.cljs",
  }));

  return {
    bridgedRoutes,
    exports: { app: appBridgeExports, frontend: frontendBridgeExports },
    records: [
      ...sourceRecords,
      ...testRecords,
      ...bridgeRecords,
      ...routeRecords,
    ],
    summary: {
      appBridgeExports: appBridgeExports.length,
      appBridgeRouteRegistrations: bridgedRoutes.length,
      cljcFiles: cljc.length,
      cljsFiles: cljs.length,
      frontendBridgeExports: frontendBridgeExports.length,
      legacyTestFiles: legacyTests.length,
      tsFiles: ts.length + declarations.length,
      tsxFiles: tsx.length,
    },
  };
}

function gitText(ref, path) {
  return execFileSync("git", ["show", `${ref}:${path}`], {
    cwd: repoRoot,
    encoding: "utf8",
  });
}

function newTypeScriptPaths(base) {
  const output = execFileSync(
    "git",
    [
      "diff",
      "--name-status",
      "--find-renames",
      `${base}...HEAD`,
      "--",
      "frontend/src",
    ],
    { cwd: repoRoot, encoding: "utf8" },
  );
  const bad = [];
  for (const line of output.split("\n")) {
    if (!line.trim()) continue;
    const [status, ...paths] = line.split("\t");
    const path = paths.at(-1);
    if (
      (status.startsWith("A") || status.startsWith("C")) &&
      /\.tsx?$/.test(path) &&
      !path.endsWith(".d.ts")
    ) {
      bad.push(path);
    }
  }
  return bad.sort();
}

function addedNames(current, previous) {
  const oldSet = new Set(previous);
  return current.filter((name) => !oldSet.has(name));
}

function checkBaseline(actual, baseline, base) {
  const failures = [];
  const limits = baseline.limits;
  const compareMax = (key) => {
    if (actual.summary[key] > limits[key]) {
      failures.push(`${key} grew: ${actual.summary[key]} > ${limits[key]}`);
    }
  };

  compareMax("tsxFiles");
  compareMax("tsFiles");
  compareMax("appBridgeExports");
  compareMax("frontendBridgeExports");
  compareMax("appBridgeRouteRegistrations");

  if (actual.summary.cljsFiles < limits.cljsFilesMinimum) {
    failures.push(
      `cljsFiles regressed: ${actual.summary.cljsFiles} < ${limits.cljsFilesMinimum}`,
    );
  }

  if (base) {
    const priorCljsFiles = execFileSync(
      "git",
      ["ls-tree", "-r", "--name-only", "-z", base, "--", "frontend/src/"],
      { cwd: repoRoot, encoding: "utf8" },
    ).split("\0").filter((path) => path.endsWith(".cljs")).length;
    if (actual.summary.cljsFiles < priorCljsFiles) {
      failures.push(
        `cljsFiles regressed against base ${base}: ${actual.summary.cljsFiles} < ${priorCljsFiles}`,
      );
    }

    const newTs = newTypeScriptPaths(base);
    if (newTs.length) {
      failures.push(`new production TypeScript paths: ${newTs.join(", ")}`);
    }

    const priorAppExports = exportNames(
      gitText(base, "frontend/src/bridge/app.ts"),
    );
    const priorFrontendExports = exportNames(
      gitText(base, "frontend/src/bridge/index.ts"),
    );
    const priorBridgedRoutes = bridgeRouteComponents(
      gitText(base, "frontend/src/cljs/knoxx/frontend/app.cljs"),
    );
    const addedApp = addedNames(actual.exports.app, priorAppExports);
    const addedFrontend = addedNames(
      actual.exports.frontend,
      priorFrontendExports,
    );
    const addedRoutes = addedNames(actual.bridgedRoutes, priorBridgedRoutes);
    if (addedApp.length) {
      failures.push(`new app-bridge exports: ${addedApp.join(", ")}`);
    }
    if (addedFrontend.length) {
      failures.push(`new frontend-bridge exports: ${addedFrontend.join(", ")}`);
    }
    if (addedRoutes.length) {
      failures.push(`new bridge-owned routes: ${addedRoutes.join(", ")}`);
    }
  }

  return failures;
}

function writeManifest(path, records) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${records.map(recordToEdn).join("\n")}\n`, "utf8");
}

function runSelfTest() {
  const source = [
    'export { default as A } from "./a";',
    'export { B, C as D } from "./b";',
  ].join("\n");
  const exports = exportNames(source);
  if (JSON.stringify(exports) !== JSON.stringify(["A", "B", "D"])) {
    throw new Error(`export parser self-test failed: ${JSON.stringify(exports)}`);
  }

  const routes = bridgeRouteComponents(
    "($ app/A) ($ Protected {:children ($ app/B)})",
  );
  if (JSON.stringify(routes) !== JSON.stringify(["A", "B"])) {
    throw new Error(`route parser self-test failed: ${JSON.stringify(routes)}`);
  }

  const edn = recordToEdn({
    "migration/kind": ":legacy-source",
    "migration/path": 'a"b.ts',
    "migration/test?": false,
  });
  if (!edn.includes('\\"')) {
    throw new Error(`EDN escaping self-test failed: ${edn}`);
  }
  console.log("frontend migration ratchet self-test: PASS");
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.selfTest) runSelfTest();
  if (args.selfTest && !args.check && !args.manifest) return;

  if (!existsSync(baselinePath)) {
    throw new Error(`Missing baseline: ${baselinePath}`);
  }
  const baseline = JSON.parse(readFileSync(baselinePath, "utf8"));
  const actual = collect();
  const manifestPath = args.manifest
    ? resolve(frontendRoot, args.manifest)
    : null;
  if (manifestPath) writeManifest(manifestPath, actual.records);

  if (args.check) {
    const failures = checkBaseline(actual, baseline, args.base);
    console.log(`Frontend migration baseline: ${baseline.sourceRef}`);
    console.log(
      `TSX ${actual.summary.tsxFiles}/${baseline.limits.tsxFiles} max; ` +
        `TS ${actual.summary.tsFiles}/${baseline.limits.tsFiles} max; ` +
        `CLJS ${actual.summary.cljsFiles}/${baseline.limits.cljsFilesMinimum} min`,
    );
    console.log(
      `Bridges app ${actual.summary.appBridgeExports}/${baseline.limits.appBridgeExports} max, ` +
        `frontend ${actual.summary.frontendBridgeExports}/${baseline.limits.frontendBridgeExports} max, ` +
        `bridged routes ${actual.summary.appBridgeRouteRegistrations}/${baseline.limits.appBridgeRouteRegistrations} max`,
    );
    if (failures.length) {
      for (const failure of failures) console.error(`FAIL ${failure}`);
      process.exitCode = 1;
    } else {
      console.log("Frontend CLJS migration ratchet: PASS");
    }
  }
}

main();
