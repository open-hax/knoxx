import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { copyFileSync, mkdirSync, mkdtempSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { test } from "node:test";

test("each CLJS gain becomes the base floor without freezing file names", () => {
  const root = mkdtempSync(join(tmpdir(), "knoxx-migration-"));
  const put = (path, text = "") => {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    writeFileSync(join(root, path), text);
  };
  const git = (...args) => execFileSync("git", args, { cwd: root, encoding: "utf8" });
  const check = (...args) => spawnSync(process.execPath,
    ["frontend/scripts/check-cljs-migration-ratchet.mjs", "--check", ...args],
    { cwd: root, encoding: "utf8" });
  try {
    put("frontend/scripts/check-cljs-migration-ratchet.mjs");
    copyFileSync(new URL("./check-cljs-migration-ratchet.mjs", import.meta.url),
      join(root, "frontend/scripts/check-cljs-migration-ratchet.mjs"));
    put("frontend/migration/baseline.json", JSON.stringify({ sourceRef: "fixture", limits: {
      tsxFiles: 0, tsFiles: 2, cljsFilesMinimum: 65,
      appBridgeExports: 0, frontendBridgeExports: 0, appBridgeRouteRegistrations: 0,
    } }));
    put("frontend/src/bridge/app.ts");
    put("frontend/src/bridge/index.ts");
    put("frontend/src/cljs/knoxx/frontend/app.cljs");
    for (let i = 1; i <= 65; i += 1) put(`frontend/src/cljs/file${i}.cljs`);
    // Neither out-of-scope source nor a similarly suffixed directory is a CLJS file.
    put("backend/src/ignored.cljs");
    put("frontend/src/folder.cljs/readme.txt");
    git("init", "-q");
    git("add", ".");
    git("-c", "user.name=Ratchet Test", "-c", "user.email=ratchet@example.invalid",
      "commit", "-qm", "66 native files");
    const base = git("rev-parse", "HEAD").trim();
    let result = check("--base", base);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    rmSync(join(root, "frontend/src/cljs/file65.cljs"));
    result = check();
    assert.equal(result.status, 0, "65 still satisfies the historical floor");
    result = check("--base", base);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /cljsFiles regressed against base .*: 65 < 66/);
    put("frontend/src/cljs/replacement.cljs");
    renameSync(join(root, "frontend/src/cljs/file1.cljs"), join(root, "frontend/src/cljs/renamed.cljs"));
    result = check("--base", base);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    put("frontend/src/cljs/gain.cljs");
    result = check("--base", base);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    result = check("--base", "missing-revision");
    assert.notEqual(result.status, 0, "unreadable base must fail closed");
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
