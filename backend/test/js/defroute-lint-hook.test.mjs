import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const backend = fileURLToPath(new URL('../../', import.meta.url));

function lint(body) {
  const directory = mkdtempSync(join(tmpdir(), 'knoxx-defroute-hook-'));
  const fixture = join(directory, 'knoxx', 'backend', 'hook_probe.cljs');
  try {
    mkdirSync(join(directory, 'knoxx', 'backend'), { recursive: true });
    writeFileSync(fixture, `(ns knoxx.backend.hook-probe
  (:require [knoxx.backend.macros :refer [defroute]]))
(defroute probe [token-ttl helper pending*] "POST" "/probe" []
  ${body})\n`);
    const result = spawnSync('clj-kondo', [
      '--config-dir', resolve(backend, '.clj-kondo'),
      '--config', '{:output {:format :json} :linters {:shadowed-var {:level :warning}}}', '--lint', fixture,
    ], { cwd: backend, encoding: 'utf8', timeout: 30000 });
    assert.ifError(result.error);
    assert.equal(result.signal, null, result.stderr);
    return JSON.parse(result.stdout).findings;
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

test('defroute models injected types as unknown and retains unused-local checks', () => {
  const findings = lint(`(let [unused-local 1
                               timeout (+ token-ttl 1)]
                           (swap! pending* assoc :timeout timeout)
                           (helper (aget request "method") reply timeout)
                           (await (helper timeout)))`);
  assert.deepEqual(findings.map(({ type }) => type), ['unused-binding']);
  assert.match(findings[0].message, /unused-local/);
});

test('defroute still reports an actual local type mismatch', () => {
  const findings = lint('(helper (inc "not-a-number"))');
  assert.equal(findings.length, 1);
  assert.equal(findings[0].type, 'type-mismatch');
  assert.equal(findings[0].level, 'error');
});

test('defroute retains real local shadow checks while native await stays unbound', () => {
  const findings = lint('(let [map (helper)] (await (helper map)))');
  assert.equal(findings.length, 1);
  assert.equal(findings[0].type, 'shadowed-var');
  assert.match(findings[0].message, /cljs.core\/map/);
});
