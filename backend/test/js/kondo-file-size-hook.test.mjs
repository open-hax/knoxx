import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, copyFileSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import test from 'node:test';

const repository = fileURLToPath(new URL('../../../', import.meta.url));

for (const side of ['backend', 'frontend']) {
  test(`${side} size hook diagnoses the owning file including the final file`, () => {
    const directory = mkdtempSync(path.join(tmpdir(), 'knoxx-kondo-size-'));
    try {
      const config = path.join(directory, 'config');
      mkdirSync(path.join(config, 'hooks'), {recursive:true});
      copyFileSync(path.join(repository,side,'.clj-kondo/hooks/promise_chain.clj'),
        path.join(config,'hooks/promise_chain.clj'));
      writeFileSync(path.join(config,'config.edn'), `{:hooks {:analyze-call
        {cljs.core/ns hooks.promise-chain/check-ns
         cljs.core/defn hooks.promise-chain/check-defn}}}`);
      const large = path.join(directory,'large.cljs'), tiny = path.join(directory,'tiny.cljs');
      writeFileSync(large, `(ns large)\n${'\n'.repeat(849)}(defn value [] 1)\n`);
      writeFileSync(tiny, '(ns tiny)\n(defn value [] 1)\n');
      for (const files of [[large],[large,tiny],[tiny,large]]) {
        const result = spawnSync('clj-kondo', ['--config-dir',config,'--lint',...files,
          '--config','{:output {:format :json}}'], {encoding:'utf8',timeout:10_000});
        assert.ifError(result.error);
        assert.equal(result.signal,null);
        const output = JSON.parse(result.stdout);
        const findings = output.findings.filter(finding => finding.type.startsWith('file-length/'));
        assert(findings.some(finding => finding.filename === large && finding.level === 'error'),
          `Large file must fail in every input order: ${result.stdout}`);
        assert(!findings.some(finding => finding.filename === tiny),
          `Another file cannot inherit a previous file's length: ${result.stdout}`);
      }
    } finally {
      rmSync(directory,{recursive:true,force:true});
    }
  });
}
