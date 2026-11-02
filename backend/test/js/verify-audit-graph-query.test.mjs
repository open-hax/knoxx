// GPL-3.0-or-later.
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const verifier=fileURLToPath(new URL('../../../scripts/verify-audit-graph-query.mjs',import.meta.url));
const fixture=fileURLToPath(new URL('./fixtures/audit-graph-probe.mjs',import.meta.url));
function run(disconnected=''){
  return spawnSync(process.execPath,[verifier],{encoding:'utf8',env:{...process.env,
    KNOXX_GRAPH_PROBE_MODULE:fixture,KNOXX_OPENPLANNER_CLIENT_MODE:'mongo',
    MONGODB_URI:'fixture-only',MONGODB_DB:'openplanner',DISCONNECTED_EDGE:disconnected}});
}
test('accepts both relationships incident to the selected observation in either direction',()=>{
  const result=run();
  assert.equal(result.status,0,result.stderr);
});
for(const edgeType of ['executed_as','observed_on']){
  test(`rejects a disconnected ${edgeType} relationship`,()=>{
    const result=run(edgeType);
    assert.notEqual(result.status,0,result.stdout);
    assert.match(result.stderr,new RegExp(edgeType));
  });
}
