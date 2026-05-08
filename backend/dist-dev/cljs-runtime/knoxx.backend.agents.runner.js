import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agent_turns.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.session_store.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.agents.runner');
knoxx.backend.agents.runner.current_runtime = (function knoxx$backend$agents$runner$current_runtime(){
return cljs.core.deref(knoxx.backend.runtime.state.runtime_STAR_);
});
knoxx.backend.agents.runner.normalize_tool_policy = (function knoxx$backend$agents$runner$normalize_tool_policy(policy){
var policy__$1 = ((cljs.core.map_QMARK_(policy))?policy:(((!((policy == null))))?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(policy,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):cljs.core.PersistentArrayMap.EMPTY
));
var tool_id = (function (){var G__53946 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-id","tool-id",-290456894).cljs$core$IFn$_invoke$arity$1(policy__$1);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"tool_id","tool_id",1550520216).cljs$core$IFn$_invoke$arity$1(policy__$1);
}
}
})();
var G__53946__$1 = (((G__53946 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53946)));
if((G__53946__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53946__$1);
}
})();
var effect = (function (){var G__53948 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "allow";
}
})();
var G__53948__$1 = (((G__53948 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53948)));
if((G__53948__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53948__$1);
}
})();
if(cljs.core.truth_(tool_id)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),effect], null);
} else {
return null;
}
});
knoxx.backend.agents.runner.normalize_agent_spec = (function knoxx$backend$agents$runner$normalize_agent_spec(value){
var spec = ((cljs.core.map_QMARK_(value))?value:(((!((value == null))))?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):cljs.core.PersistentArrayMap.EMPTY
));
var contract_id = (function (){var G__53949 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"contractId","contractId",710260199).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"agent_id","agent_id",-1820880197).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"agent-id","agent-id",1570348870).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"agentId","agentId",2025355078).cljs$core$IFn$_invoke$arity$1(spec);
}
}
}
}
}
})();
var G__53949__$1 = (((G__53949 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53949)));
if((G__53949__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53949__$1);
}
})();
var actor_id = (function (){var G__53950 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__53950__$1 = (((G__53950 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53950)));
if((G__53950__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53950__$1);
}
})();
var role = (function (){var G__53951 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role_slug","role_slug",219656703).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"role-slug","role-slug",-617706766).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"roleSlug","roleSlug",-867274708).cljs$core$IFn$_invoke$arity$1(spec);
}
}
}
})();
var G__53951__$1 = (((G__53951 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53951)));
if((G__53951__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53951__$1);
}
})();
var system_prompt = (function (){var G__53953 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"system_prompt","system_prompt",-655033954).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
if((G__53953 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53953));
}
})();
var task_prompt = (function (){var G__53955 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"task_prompt","task_prompt",1276696196).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
if((G__53955 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53955));
}
})();
var model = (function (){var G__53956 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(spec);
var G__53956__$1 = (((G__53956 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53956)));
if((G__53956__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53956__$1);
}
})();
var thinking_level = (function (){var G__53957 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"reasoning_effort","reasoning_effort",-375529027).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"reasoning-effort","reasoning-effort",-1891634506).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"reasoningEffort","reasoningEffort",1501429170).cljs$core$IFn$_invoke$arity$1(spec);
}
}
}
}
}
})();
var G__53957__$1 = (((G__53957 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53957)));
if((G__53957__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53957__$1);
}
})();
var tool_policies = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.runner.normalize_tool_policy,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
}
})()));
var resource_policies = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"resource_policies","resource_policies",-1190579829).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var memory_hydration = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"memory_hydration","memory_hydration",-1458677455).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var context_policy = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"context_policy","context_policy",1230169154).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"context","context",-830191113).cljs$core$IFn$_invoke$arity$1(spec);
}
}
}
})();
var sub_agent_id = (function (){var G__53958 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"sub_agent_id","sub_agent_id",320149773).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"subAgentId","subAgentId",538139792).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__53958__$1 = (((G__53958 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53958)));
if((G__53958__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53958__$1);
}
})();
var parent_agent_id = (function (){var G__53959 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"parent_agent_id","parent_agent_id",-252488900).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"parentAgentId","parentAgentId",1686278200).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__53959__$1 = (((G__53959 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53959)));
if((G__53959__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53959__$1);
}
})();
var parent_run_id = (function (){var G__53960 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"parent_run_id","parent_run_id",-2071531014).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__53960__$1 = (((G__53960 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53960)));
if((G__53960__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53960__$1);
}
})();
var spawn_kind = (function (){var G__53962 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"spawn_kind","spawn_kind",1611229473).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"spawnKind","spawnKind",1648184297).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__53962__$1 = (((G__53962 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53962)));
if((G__53962__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53962__$1);
}
})();
var G__53963 = cljs.core.PersistentArrayMap.EMPTY;
var G__53963__$1 = (cljs.core.truth_(contract_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),contract_id):G__53963);
var G__53963__$2 = (cljs.core.truth_(actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$1,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id):G__53963__$1);
var G__53963__$3 = (cljs.core.truth_(role)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$2,new cljs.core.Keyword(null,"role","role",-736691072),role):G__53963__$2);
var G__53963__$4 = (((!((system_prompt == null))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$3,new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),system_prompt):G__53963__$3);
var G__53963__$5 = (((!((task_prompt == null))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$4,new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),task_prompt):G__53963__$4);
var G__53963__$6 = (cljs.core.truth_(model)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$5,new cljs.core.Keyword(null,"model","model",331153215),model):G__53963__$5);
var G__53963__$7 = (cljs.core.truth_(thinking_level)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$6,new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),thinking_level):G__53963__$6);
var G__53963__$8 = ((cljs.core.seq(tool_policies))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$7,new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),tool_policies):G__53963__$7);
var G__53963__$9 = (cljs.core.truth_(resource_policies)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$8,new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874),resource_policies):G__53963__$8);
var G__53963__$10 = (cljs.core.truth_(memory_hydration)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$9,new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082),memory_hydration):G__53963__$9);
var G__53963__$11 = (cljs.core.truth_(context_policy)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$10,new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557),context_policy):G__53963__$10);
var G__53963__$12 = (cljs.core.truth_(sub_agent_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$11,new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479),sub_agent_id):G__53963__$11);
var G__53963__$13 = (cljs.core.truth_(parent_agent_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$12,new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925),parent_agent_id):G__53963__$12);
var G__53963__$14 = (cljs.core.truth_(parent_run_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$13,new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367),parent_run_id):G__53963__$13);
if(cljs.core.truth_(spawn_kind)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53963__$14,new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959),spawn_kind);
} else {
return G__53963__$14;
}
});
knoxx.backend.agents.runner.direct_start_payload__GT_turn_params = (function knoxx$backend$agents$runner$direct_start_payload__GT_turn_params(payload){
var auth_context = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"auth_context","auth_context",-1323760790).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"auth-context","auth-context",320032325).cljs$core$IFn$_invoke$arity$1(payload);
}
})();
var G__53964 = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(payload);
}
})(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(payload);
}
})(),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(payload);
}
})(),new cljs.core.Keyword(null,"message","message",-406056002),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"content-parts","content-parts",684529019),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"content-parts","content-parts",684529019).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})(),new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"mode","mode",654403691),"direct",new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),knoxx.backend.agents.runner.normalize_agent_spec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(payload);
}
})())], null);
if(cljs.core.truth_(auth_context)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53964,new cljs.core.Keyword(null,"auth-context","auth-context",320032325),auth_context);
} else {
return G__53964;
}
});
knoxx.backend.agents.runner.policy_model = (function knoxx$backend$agents$runner$policy_model(config,body){
var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config);
}
}
}
});
knoxx.backend.agents.runner.accepted_response = (function knoxx$backend$agents$runner$accepted_response(body){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"queued","queued",1701634607),true,new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(body),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(body),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(body),new cljs.core.Keyword(null,"model","model",331153215),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
}
})()], null);
});
knoxx.backend.agents.runner.queue_turn_BANG_ = (function knoxx$backend$agents$runner$queue_turn_BANG_(runtime,config,body){
return knoxx.backend.agent_turns.validate_chat_policy_BANG_(new cljs.core.Keyword(null,"auth-context","auth-context",320032325).cljs$core$IFn$_invoke$arity$1(body),knoxx.backend.agents.runner.policy_model(config,body)).then((function (_){
knoxx.backend.agent_turns.send_agent_turn_BANG_(runtime,config,body).then((function (___$1){
return null;
})).catch((function (err){
return console.error("[agents.runner] async direct spawn failed",err);
}));

return knoxx.backend.agents.runner.accepted_response(body);
}));
});
knoxx.backend.agents.runner.busy_error = (function knoxx$backend$agents$runner$busy_error(message){
return Promise.reject((new Error(message)));
});
knoxx.backend.agents.runner.normalize_body = (function knoxx$backend$agents$runner$normalize_body(_runtime,payload){
var params = knoxx.backend.agents.runner.direct_start_payload__GT_turn_params(payload);
var provided_session_id = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(params);
var session_id = knoxx.backend.agent_turns.ensure_session_id(shadow.esm.esm_import$node_crypto,provided_session_id);
var conversation_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(params);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_crypto.randomUUID();
}
})();
var run_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(params);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_crypto.randomUUID();
}
})();
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(params,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),run_id,new cljs.core.Keyword(null,"mode","mode",654403691),"direct"], 0));
});
knoxx.backend.agents.runner.spawn_direct_BANG_ = (function knoxx$backend$agents$runner$spawn_direct_BANG_(var_args){
var G__53967 = arguments.length;
switch (G__53967) {
case 2:
return knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (config,payload){
return knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.agents.runner.current_runtime(),config,payload);
}));

(knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,payload){
if(cljs.core.not(runtime)){
return knoxx.backend.agents.runner.busy_error("Knoxx runtime unavailable for direct agent spawn");
} else {
var body = knoxx.backend.agents.runner.normalize_body(runtime,payload);
var provided_session_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(payload);
}
})();
if(cljs.core.not(provided_session_id)){
return knoxx.backend.agents.runner.queue_turn_BANG_(runtime,config,body);
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(body)).then((function (session){
var can_send_result = knoxx.backend.session_store.session_can_send_QMARK_(session);
if(cljs.core.not(new cljs.core.Keyword(null,"can-send","can-send",-704220819).cljs$core$IFn$_invoke$arity$1(can_send_result))){
return knoxx.backend.agents.runner.busy_error((""+"agent_already_processing: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"reason","reason",-2070751759).cljs$core$IFn$_invoke$arity$1(can_send_result))));
} else {
var agent_session = knoxx.backend.agent_runtime.active_agent_session(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(body));
var actively_streaming_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
return (agent_session["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(actively_streaming_QMARK_)){
return knoxx.backend.agents.runner.busy_error("agent_already_processing: active stream");
} else {
return knoxx.backend.agents.runner.queue_turn_BANG_(runtime,config,body);
}
}
}));
}
}
}));

(knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.agents.runner.js.map
