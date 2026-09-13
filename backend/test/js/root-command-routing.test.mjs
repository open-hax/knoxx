import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

const rootManifest = JSON.parse(readFileSync(new URL('../../../package.json', import.meta.url)));
const routes = {
  dev: ['frontend:dev'],
  build: ['backend:build', 'frontend:build'],
  test: ['backend:test', 'frontend:test:cljs', 'frontend:test'],
  lint: ['backend:lint', 'frontend:lint', 'root:lint:size'],
  typecheck: ['backend:typecheck', 'frontend:typecheck'],
};

function fixture(t) {
  const directory = mkdtempSync(join(tmpdir(), 'knoxx-root-commands-'));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  const root = join(directory, 'knoxx');
  const ledger = join(directory, 'commands.jsonl');
  mkdirSync(root);
  mkdirSync(join(directory, 'unrelated'));
  writeFileSync(join(directory, 'pnpm-workspace.yaml'), 'packages:\n  - unrelated\n');
  writeFileSync(join(directory, 'package.json'), JSON.stringify({ private: true }));
  writeFileSync(join(directory, 'unrelated/package.json'), JSON.stringify({ name: 'unrelated', private: true }));
  writeFileSync(join(root, 'package.json'), JSON.stringify({
    name: rootManifest.name,
    private: true,
    scripts: rootManifest.scripts,
  }));
  writeFileSync(join(root, 'record.mjs'), [
    "import { appendFileSync } from 'node:fs';",
    'const stage = process.argv[2];',
    'appendFileSync(process.env.COMMAND_LEDGER, JSON.stringify({ stage, cwd: process.cwd() }) + "\\n");',
    'if (stage === process.env.FAIL_STAGE) process.exit(23);',
  ].join('\n'));
  mkdirSync(join(root, 'scripts'));
  writeFileSync(join(root, 'scripts/lint-file-sizes.mjs'),
    "process.argv[2] = 'root:lint:size'; await import('../record.mjs');\n");
  for (const child of ['backend', 'frontend']) {
    mkdirSync(join(root, child));
    writeFileSync(join(root, child, 'package.json'), JSON.stringify({
      name: child === 'backend' ? '@open-hax/knoxx-backend-cljs' : '@open-hax/knoxx-frontend',
      private: true,
      scripts: Object.fromEntries(['dev', 'build', 'test', 'test:cljs', 'lint', 'typecheck']
        .map(action => [action, `node ../record.mjs ${child}:${action}`])),
    }));
  }
  return {
    root,
    run(action, failStage = '') {
      const result = spawnSync('pnpm', ['run', action], {
        cwd: root,
        env: { ...process.env, COMMAND_LEDGER: ledger, FAIL_STAGE: failStage },
        encoding: 'utf8',
        timeout: 30_000,
      });
      assert.ifError(result.error);
      let records = [];
      try {
        records = readFileSync(ledger, 'utf8').trim().split('\n').filter(Boolean).map(JSON.parse);
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
      return { ...result, records };
    },
  };
}

for (const [action, stages] of Object.entries(routes)) {
  test(`root ${action} reaches its children inside an enclosing workspace`, t => {
    const { root, run } = fixture(t);
    const result = run(action);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.deepEqual(result.records, stages.map(stage => ({
      stage, cwd: stage.startsWith('root:') ? root : join(root, stage.split(':')[0]),
    })));
  });

  test(`root ${action} propagates failure and stops the remaining children`, t => {
    const { root, run } = fixture(t);
    const result = run(action, stages[0]);
    assert.notEqual(result.status, 0, result.stdout + result.stderr);
    assert.deepEqual(result.records, [{ stage: stages[0], cwd: join(root, stages[0].split(':')[0]) }]);
  });
}

test('root lint preserves a failing repository file-size gate', t => {
  const { run } = fixture(t);
  const result = run('lint', 'root:lint:size');
  assert.notEqual(result.status, 0, result.stdout + result.stderr);
  assert.deepEqual(result.records.map(record => record.stage), routes.lint);
});

test('a missing frontend cannot turn the root build into a successful no-op', t => {
  const { root, run } = fixture(t);
  rmSync(join(root, 'frontend'), { recursive: true });
  const result = run('build');
  assert.notEqual(result.status, 0, result.stdout + result.stderr);
  assert.deepEqual(result.records, [{ stage: 'backend:build', cwd: join(root, 'backend') }]);
});
