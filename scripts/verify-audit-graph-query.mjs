#!/usr/bin/env node
// GPL-3.0-or-later. Read-only verification through the actor's registered tool.
import assert from 'node:assert/strict';
import { pathToFileURL } from 'node:url';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const modulePath=process.env.KNOXX_GRAPH_PROBE_MODULE??resolve(root,'backend/dist-graph-probe/probe.js');
assert.equal(process.env.KNOXX_OPENPLANNER_CLIENT_MODE,'mongo','Set direct-Mongo mode explicitly.');
assert.ok(process.env.MONGODB_URI,'Provide the existing application URI through protected environment configuration.');
assert.equal(process.env.MONGODB_DB,'openplanner','This verification expects the production audit graph database.');
const probe=await import(pathToFileURL(modulePath).href);
const ctx={actor:{id:'system_admin'},roleSlugs:['system_admin']};
const config=probe.config();
const tools=probe.createActorTools(null,config,ctx);
const graph=tools.find(tool=>tool.name==='graph_query');
assert.ok(graph,'The actor runtime must actually advertise graph_query.');
try{
  const result=await graph.execute('audit-graph-verification',{
    query:'codex',lake:'linux-audit',nodeType:'audit.harness',limit:8,edgeLimit:12});
  assert.equal(result.details.mode,'structural');
  assert.ok(result.details.nodes.some(n=>n.nodeType==='audit.harness'&&n.label==='codex'));
  assert.ok(result.details.edges.some(e=>e.edgeType==='mapped_to_harness'));
  assert.ok(result.details.nodes.every(n=>n.lake==='linux-audit'));
  console.log('PASS actor tool graph_query returned codex and its Linux principal relationship.');
  const observations=await graph.execute('audit-observation-verification',{
    query:'Audit ',lake:'linux-audit',nodeType:'audit.syscall',limit:1,edgeLimit:0});
  const observation=observations.details.nodes[0];
  assert.ok(observation?.data?.provenance?.evidence?.id,'Tool output must retain the exact raw-evidence pointer.');
  assert.equal(observations.details.edges.length,0);
  const neighborhood=await graph.execute('audit-neighborhood-verification',{
    query:observation.id,lake:'linux-audit',limit:8,edgeLimit:12});
  assert.ok(neighborhood.details.edges.some(e=>e.edgeType==='executed_as'));
  assert.ok(neighborhood.details.edges.some(e=>e.edgeType==='observed_on'));
  const empty=await graph.execute('audit-scope-verification',{
    query:observation.id,lake:'definitely-not-linux-audit',limit:8,edgeLimit:12});
  assert.equal(empty.details.nodes.length,0);
  const denied=probe.createActorTools(null,config,{actor:{id:'verification-denied'},toolPolicies:[]});
  assert.ok(!denied.some(tool=>tool.name==='graph_query'),'A context without permission must not receive the tool.');
  console.log('PASS actor tool returned an audit observation, root/host edges and raw-evidence provenance.');
  console.log('PASS edgeLimit=0, lake isolation and actor tool permission denial.');
  console.log(JSON.stringify({actorId:'system_admin',tool:'graph_query',mode:result.details.mode,
    observation:observation.id,evidence:observation.data.provenance.evidence,
    edgeTypes:neighborhood.details.edges.map(e=>e.edgeType)}));
  console.log('WARN this imports the candidate actor tool factory; it does not prove the production MCP server has been deployed.');
}finally{await probe.close();}
