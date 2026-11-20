// GPL-3.0-or-later. Isolated fixture for the read-only verifier.
export const config=()=>({});
export function createActorTools(_unused,_config,ctx){
  if(ctx.toolPolicies) return [];
  return [{name:'graph_query',execute:async (id)=>{
    if(id==='audit-graph-verification') return {details:{mode:'structural',nodes:[{nodeType:'audit.harness',label:'codex',lake:'linux-audit'}],edges:[{edgeType:'mapped_to_harness'}]}};
    if(id==='audit-observation-verification') return {details:{nodes:[{id:'observation',data:{provenance:{evidence:{id:'evidence'}}}}],edges:[]}};
    if(id==='audit-scope-verification') return {details:{nodes:[]}};
    const disconnected=process.env.DISCONNECTED_EDGE;
    return {details:{edges:['executed_as','observed_on'].map((edgeType,i)=>({edgeType,source:edgeType===disconnected?'other':i?'host':'observation',target:edgeType===disconnected?'elsewhere':i?'observation':'principal'}))}};
  }}];
}
export const close=async()=>{};
