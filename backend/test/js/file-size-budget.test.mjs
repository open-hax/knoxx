import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const repository = fileURLToPath(new URL('../../../', import.meta.url));
const linter = path.join(repository, 'scripts/lint-file-sizes.mjs');

for (const extension of ['clj', 'cljs', 'cljc', 'js', 'mjs', 'cjs', 'ts', 'tsx']) {
  test(`repository ${extension} size gate enforces documented 350/500 boundaries`, () => {
    const directory = mkdtempSync(path.join(tmpdir(), 'knoxx-size-budget-'));
    try {
      const fixture = path.join(directory, `fixture.${extension}`);
      for (const [lines, warning, error] of [
        [349, false, false], [350, true, false], [499, true, false], [500, false, true],
      ]) {
        writeFileSync(fixture, `${'source line\n'.repeat(lines)}`);
        const result = spawnSync(process.execPath, [linter, fixture],
          { encoding: 'utf8', timeout: 10_000 });
        assert.ifError(result.error);
        assert.equal(result.signal, null);
        assert.equal(result.status, error ? 1 : 0, result.stdout + result.stderr);
        assert.equal(/WARN\s+\d+ lines \(warn 350\)/.test(result.stdout), warning, result.stdout);
        assert.equal(/ERROR \d+ lines \(error 500\)/.test(result.stdout), error, result.stdout);
      }
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
}
