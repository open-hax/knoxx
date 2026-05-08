import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.actions.registry.js";
import "./knoxx.backend.contracts.loader.js";
import "./knoxx.backend.contracts.roles.js";
import "./knoxx.backend.tools.registry.js";
goog.provide('knoxx.backend.actions.invoke_sub_agent');
knoxx.backend.actions.invoke_sub_agent.id__GT_string = (function knoxx$backend$actions$invoke_sub_agent$id__GT_string(value){
var G__34468 = (((value instanceof cljs.core.Keyword))?cljs.core.name(value):(((!((value == null))))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)):null
));
var G__34468__$1 = (((G__34468 == null))?null:clojure.string.trim(G__34468));
if((G__34468__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__34468__$1);
}
});
knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword = (function knoxx$backend$actions$invoke_sub_agent$mode__GT_keyword(value){
if((value instanceof cljs.core.Keyword)){
return value;
} else {
if((!((value == null)))){
return cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(clojure.string.replace(clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)))),/_/,"-"));
} else {
return null;

}
}
});
/**
 * Load a sub-agent contract by ID. Resolves to the parsed contract map or nil.
 */
knoxx.backend.actions.invoke_sub_agent.load_sub_agent_contract_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$load_sub_agent_contract_BANG_(config,sub_agent_id){
var temp__5823__auto__ = knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id);
if(cljs.core.truth_(temp__5823__auto__)){
var id = temp__5823__auto__;
return knoxx.backend.contracts.loader.load_contract_BANG_.cljs$core$IFn$_invoke$arity$3(config,"sub_agents",id).then((function (p__34486){
var map__34487 = p__34486;
var map__34487__$1 = cljs.core.__destructure_map(map__34487);
var ok_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34487__$1,new cljs.core.Keyword(null,"ok?","ok?",447310304));
var contract = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34487__$1,new cljs.core.Keyword(null,"contract","contract",798152745));
var validation = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34487__$1,new cljs.core.Keyword(null,"validation","validation",-2141396518));
if(cljs.core.truth_(ok_QMARK_)){
return contract;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("Contract not found",(function (){var G__34493 = validation;
var G__34493__$1 = (((G__34493 == null))?null:new cljs.core.Keyword(null,"errors","errors",-908790718).cljs$core$IFn$_invoke$arity$1(G__34493));
var G__34493__$2 = (((G__34493__$1 == null))?null:cljs.core.first(G__34493__$1));
if((G__34493__$2 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(G__34493__$2);
}
})())){
console.warn("[sub-agent] contract not found:",id);

return null;
} else {
console.warn("[sub-agent] invalid contract",id,cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([validation], 0)));

return null;

}
}
}));
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actions.invoke_sub_agent.contract_from_action = (function knoxx$backend$actions$invoke_sub_agent$contract_from_action(action_with,sub_agent_id){
var id = knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id);
var contracts_by_id = new cljs.core.Keyword(null,"sub-agent-contracts","sub-agent-contracts",1911159075).cljs$core$IFn$_invoke$arity$1(action_with);
var or__5142__auto__ = new cljs.core.Keyword(null,"sub-agent-contract","sub-agent-contract",-742371462).cljs$core$IFn$_invoke$arity$1(action_with);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(contracts_by_id,sub_agent_id);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(contracts_by_id,id);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(contracts_by_id,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(id));
}
}
}
});
knoxx.backend.actions.invoke_sub_agent.resolve_sub_agent_contract_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$resolve_sub_agent_contract_BANG_(config,action_with,sub_agent_id){
var temp__5823__auto__ = knoxx.backend.actions.invoke_sub_agent.contract_from_action(action_with,sub_agent_id);
if(cljs.core.truth_(temp__5823__auto__)){
var contract = temp__5823__auto__;
return Promise.resolve(contract);
} else {
return knoxx.backend.actions.invoke_sub_agent.load_sub_agent_contract_BANG_(config,sub_agent_id);
}
});
knoxx.backend.actions.invoke_sub_agent.fallback_contract = (function knoxx$backend$actions$invoke_sub_agent$fallback_contract(sub_agent_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword("contract","id","contract/id",-872298206),(function (){var or__5142__auto__ = knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-sub-agent";
}
})(),new cljs.core.Keyword("contract","kind","contract/kind",1929672067),new cljs.core.Keyword(null,"sub-agent","sub-agent",-111773131)], null);
});
knoxx.backend.actions.invoke_sub_agent.enabled_contract_QMARK_ = (function knoxx$backend$actions$invoke_sub_agent$enabled_contract_QMARK_(contract){
return (!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(contract) === false));
});
/**
 * Build a context preamble for the sub-agent describing its parent lineage.
 */
knoxx.backend.actions.invoke_sub_agent.parent_context_preamble = (function knoxx$backend$actions$invoke_sub_agent$parent_context_preamble(parent_job,parent_event){
var parent_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(parent_job);
var event_kind = new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(parent_event);
var source_kind = new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(parent_event);
return (""+"[Sub-Agent Context]\n"+"You are a sub-agent spawned by parent agent: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(parent_id)+"\n"+"Trigger event: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)+" from "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(source_kind)+"\n"+"Complete only the delegated task and report your result clearly.\n\n");
});
knoxx.backend.actions.invoke_sub_agent.capability__GT_tool_ids = (function knoxx$backend$actions$invoke_sub_agent$capability__GT_tool_ids(config,cap){
var from_contract = knoxx.backend.contracts.roles.capability_tool_ids(config,cap);
var direct_tool = (function (){var G__34533 = (((cap instanceof cljs.core.Keyword))?cljs.core.name(cap):cap);
var G__34533__$1 = (((G__34533 == null))?null:knoxx.backend.tools.registry.normalize_tool_id(G__34533));
var G__34533__$2 = (((G__34533__$1 == null))?null:clojure.string.trim(G__34533__$1));
if((G__34533__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34533__$2);
}
})();
if(cljs.core.seq(from_contract)){
return from_contract;
} else {
var G__34544 = cljs.core.PersistentVector.EMPTY;
if(cljs.core.truth_(direct_tool)){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__34544,direct_tool);
} else {
return G__34544;
}
}
});
knoxx.backend.actions.invoke_sub_agent.capabilities__GT_tool_policies = (function knoxx$backend$actions$invoke_sub_agent$capabilities__GT_tool_policies(config,caps){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null);
}),cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__34547_SHARP_){
return knoxx.backend.actions.invoke_sub_agent.capability__GT_tool_ids(config,p1__34547_SHARP_);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = caps;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()], 0))))));
});
knoxx.backend.actions.invoke_sub_agent.parent_tool_policies = (function knoxx$backend$actions$invoke_sub_agent$parent_tool_policies(parent_spec){
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(parent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(parent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})());
});
knoxx.backend.actions.invoke_sub_agent.resolve_tool_policies = (function knoxx$backend$actions$invoke_sub_agent$resolve_tool_policies(config,parent_spec,sub_agent_config,sub_agent_contract){
var parent_mode = (function (){var or__5142__auto__ = knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword(new cljs.core.Keyword("sub-agent","parent-capabilities","sub-agent/parent-capabilities",-2116028020).cljs$core$IFn$_invoke$arity$1(sub_agent_contract));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"inherit","inherit",-1840815422);
}
})();
var configured_caps = (function (){var or__5142__auto__ = new cljs.core.Keyword("sub-agent","capabilities","sub-agent/capabilities",1435665929).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"restrict-capabilities","restrict-capabilities",-406350349).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
}
})();
var sub_policies = knoxx.backend.actions.invoke_sub_agent.capabilities__GT_tool_policies(config,configured_caps);
var parent_policies = knoxx.backend.actions.invoke_sub_agent.parent_tool_policies(parent_spec);
var G__34579 = parent_mode;
var G__34579__$1 = (((G__34579 instanceof cljs.core.Keyword))?G__34579.fqn:null);
switch (G__34579__$1) {
case "none":
return sub_policies;

break;
case "restrict":
return sub_policies;

break;
case "inherit":
if(cljs.core.seq(sub_policies)){
return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.first,cljs.core.vals(cljs.core.group_by(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(parent_policies,sub_policies))))));
} else {
return parent_policies;
}

break;
default:
if(cljs.core.seq(sub_policies)){
return sub_policies;
} else {
return parent_policies;
}

}
});
knoxx.backend.actions.invoke_sub_agent.compact_spec = (function knoxx$backend$actions$invoke_sub_agent$compact_spec(spec){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.filter.cljs$core$IFn$_invoke$arity$1((function (p__34590){
var vec__34591 = p__34590;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__34591,(0),null);
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__34591,(1),null);
return (!((((value == null)) || (((((typeof value === 'string') && (clojure.string.blank_QMARK_(value)))) || (((cljs.core.sequential_QMARK_(value)) && (cljs.core.empty_QMARK_(value)))))))));
})),spec);
});
/**
 * Merge parent agent spec with sub-agent contract to create the spawn spec.
 */
knoxx.backend.actions.invoke_sub_agent.merge_sub_agent_spec = (function knoxx$backend$actions$invoke_sub_agent$merge_sub_agent_spec(config,parent_spec,sub_agent_config,sub_agent_contract,full_system_prompt,task_prompt){
var contract_agent = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agent","agent",-766455027).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var model = (function (){var or__5142__auto__ = new cljs.core.Keyword("sub-agent","model","sub-agent/model",1269193863).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(contract_agent);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(parent_spec);
}
}
}
})();
var thinking = (function (){var or__5142__auto__ = new cljs.core.Keyword("sub-agent","thinking","sub-agent/thinking",372087731).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinking","thinking",2063777387).cljs$core$IFn$_invoke$arity$1(contract_agent);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"thinking","thinking",2063777387).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(parent_spec);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(parent_spec);
}
}
}
}
})();
var role = (function (){var or__5142__auto__ = new cljs.core.Keyword("sub-agent","role","sub-agent/role",819618552).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(contract_agent);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"inherit-role","inherit-role",-543857755).cljs$core$IFn$_invoke$arity$1(sub_agent_config))){
return new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(parent_spec);
} else {
return null;
}
}
}
})();
var context_policy = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"context","context",-830191113).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
}
})();
var tool_policies = knoxx.backend.actions.invoke_sub_agent.resolve_tool_policies(config,parent_spec,sub_agent_config,sub_agent_contract);
return knoxx.backend.actions.invoke_sub_agent.compact_spec(cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353),new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925),new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959),new cljs.core.Keyword(null,"spawnKind","spawnKind",1648184297),new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106),new cljs.core.Keyword(null,"parentConversationId","parentConversationId",-64718550),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557),new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367),new cljs.core.Keyword(null,"subAgentId","subAgentId",538139792),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"parentCapabilitiesMode","parentCapabilitiesMode",-1578789741),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426),new cljs.core.Keyword(null,"parentAgentId","parentAgentId",1686278200),new cljs.core.Keyword(null,"parentSessionId","parentSessionId",1674230329),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479),new cljs.core.Keyword(null,"model","model",331153215)],[role,task_prompt,context_policy,task_prompt,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"parent-job","parent-job",-416788546).cljs$core$IFn$_invoke$arity$1(sub_agent_config)),"sub-agent","sub-agent",new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106).cljs$core$IFn$_invoke$arity$1(sub_agent_config),new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426).cljs$core$IFn$_invoke$arity$1(sub_agent_config),context_policy,new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(sub_agent_config),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(sub_agent_config),new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(sub_agent_contract),tool_policies,thinking,full_system_prompt,new cljs.core.Keyword("sub-agent","parent-capabilities","sub-agent/parent-capabilities",-2116028020).cljs$core$IFn$_invoke$arity$1(sub_agent_contract),full_system_prompt,new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426).cljs$core$IFn$_invoke$arity$1(sub_agent_config),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"parent-job","parent-job",-416788546).cljs$core$IFn$_invoke$arity$1(sub_agent_config)),new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106).cljs$core$IFn$_invoke$arity$1(sub_agent_config),tool_policies,thinking,new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(sub_agent_contract),model]));
});
/**
 * Build the spawn payload for a sub-agent run.
 */
knoxx.backend.actions.invoke_sub_agent.build_sub_agent_payload = (function knoxx$backend$actions$invoke_sub_agent$build_sub_agent_payload(config,parent_job,parent_event,sub_agent_id,sub_agent_config,sub_agent_contract){
var parent_spec = new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$2(parent_job,cljs.core.PersistentArrayMap.EMPTY);
var system_prompt = cljs.core.get_in.cljs$core$IFn$_invoke$arity$3(sub_agent_contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"system","system",-29381724)], null),"");
var task_prompt = cljs.core.get_in.cljs$core$IFn$_invoke$arity$3(sub_agent_contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"task","task",-1476607993)], null),"");
var shared_context = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"shared-context","shared-context",-627628786).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(sub_agent_config,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"shared-context","shared-context",-627628786)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(sub_agent_contract,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"context","context",-830191113)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
}
})();
var context_preamble = knoxx.backend.actions.invoke_sub_agent.parent_context_preamble(parent_job,parent_event);
var full_system_prompt = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(context_preamble)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(system_prompt));
var result_key = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(sub_agent_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id);
}
})();
var sub_agent_config__$1 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(sub_agent_config,new cljs.core.Keyword(null,"parent-job","parent-job",-416788546),parent_job,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"run-id","run-id",-1745267908)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"run_id","run_id",-556768024)], null));
}
})(),new cljs.core.Keyword(null,"parent-conversation-id","parent-conversation-id",-1886944426),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980)], null));
}
}
}
})(),new cljs.core.Keyword(null,"parent-session-id","parent-session-id",975696106),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"session-id","session-id",-1147060351)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"session_id","session_id",1584799627)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"sessionId","sessionId",1640410629)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"session_id","session_id",1584799627)], null));
}
}
}
})()], 0));
var agent_spec = knoxx.backend.actions.invoke_sub_agent.merge_sub_agent_spec(config,parent_spec,sub_agent_config__$1,sub_agent_contract,full_system_prompt,task_prompt);
var spawn_id = (""+"sub-agent-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(parent_job))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now()));
var timeout_ms = (function (){var or__5142__auto__ = new cljs.core.Keyword("sub-agent","timeout-ms","sub-agent/timeout-ms",-942662874).cljs$core$IFn$_invoke$arity$1(sub_agent_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406).cljs$core$IFn$_invoke$arity$1(sub_agent_config__$1);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (30000);
}
}
})();
var mode = (function (){var or__5142__auto__ = knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword(new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(sub_agent_config__$1));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword(new cljs.core.Keyword("sub-agent","mode","sub-agent/mode",-974519373).cljs$core$IFn$_invoke$arity$1(sub_agent_contract));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"fire-and-forget","fire-and-forget",-940372776);
}
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),new cljs.core.Keyword(null,"job-id","job-id",651349542),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"shared-context","shared-context",-627628786),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131),new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"result-key","result-key",-521043082),new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479)],[task_prompt,spawn_id,mode,shared_context,cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parent_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"run-id","run-id",-1745267908)], null)),full_system_prompt,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(parent_job),agent_spec,result_key,timeout_ms,knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_agent_id)]);
});
knoxx.backend.actions.invoke_sub_agent.enriched_content = (function knoxx$backend$actions$invoke_sub_agent$enriched_content(payload,event_payload){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(payload),((cljs.core.seq(new cljs.core.Keyword(null,"shared-context","shared-context",-627628786).cljs$core$IFn$_invoke$arity$1(payload)))?(""+"Shared context:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"shared-context","shared-context",-627628786).cljs$core$IFn$_invoke$arity$1(payload)], 0)))):null),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(event_payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"summary","summary",380847952).cljs$core$IFn$_invoke$arity$1(event_payload);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"payloadPreview","payloadPreview",874931409).cljs$core$IFn$_invoke$arity$1(event_payload);
}
}
})()], null)));
});
knoxx.backend.actions.invoke_sub_agent.sub_agent_job = (function knoxx$backend$actions$invoke_sub_agent$sub_agent_job(job,payload){
return cljs.core.assoc_in(cljs.core.assoc_in(cljs.core.assoc_in(cljs.core.assoc_in(cljs.core.assoc_in(cljs.core.assoc_in(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(job,new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"job-id","job-id",651349542).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.Keyword(null,"name","name",1843675177),(""+"Sub-agent: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload)))),new cljs.core.Keyword(null,"description","description",-1428560544),(""+"Sub-agent spawned by "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(payload)))),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227),null),new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402),"sub-agent"),new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630),(""+"sub-agent:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload)))),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"stickySession","stickySession",1252676028)], null),false),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131)], null),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367)], null),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479)], null),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"result-key","result-key",-521043082)], null),new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(payload)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"shared-context","shared-context",-627628786)], null),new cljs.core.Keyword(null,"shared-context","shared-context",-627628786).cljs$core$IFn$_invoke$arity$1(payload));
});
knoxx.backend.actions.invoke_sub_agent.sub_agent_event = (function knoxx$backend$actions$invoke_sub_agent$sub_agent_event(event,payload){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(event,new cljs.core.Keyword(null,"payload","payload",-383036092),(function (event_payload){
var event_payload__$1 = (function (){var or__5142__auto__ = event_payload;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(event_payload__$1,new cljs.core.Keyword(null,"content","content",15833224),knoxx.backend.actions.invoke_sub_agent.enriched_content(payload,event_payload__$1));
}));
});
knoxx.backend.actions.invoke_sub_agent.timeout_promise = (function knoxx$backend$actions$invoke_sub_agent$timeout_promise(payload){
return (new Promise((function (_resolve,reject){
return setTimeout((function (){
var G__34669 = (new Error((""+"Sub-agent timeout: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload)))));
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(G__34669) : reject.call(null,G__34669));
}),new cljs.core.Keyword(null,"timeout-ms","timeout-ms",754221406).cljs$core$IFn$_invoke$arity$2(payload,(30000)));
})));
});
knoxx.backend.actions.invoke_sub_agent.spawn_once_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$spawn_once_BANG_(ctx,payload){
var map__34673 = ctx;
var map__34673__$1 = cljs.core.__destructure_map(map__34673);
var run_agent_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34673__$1,new cljs.core.Keyword(null,"run-agent!","run-agent!",-1490275853));
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34673__$1,new cljs.core.Keyword(null,"config","config",994861415));
var event = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34673__$1,new cljs.core.Keyword(null,"event","event",301435442));
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34673__$1,new cljs.core.Keyword(null,"job","job",850873087));
var G__34675 = config;
var G__34676 = knoxx.backend.actions.invoke_sub_agent.sub_agent_job(job,payload);
var G__34677 = knoxx.backend.actions.invoke_sub_agent.sub_agent_event(event,payload);
return (run_agent_BANG_.cljs$core$IFn$_invoke$arity$3 ? run_agent_BANG_.cljs$core$IFn$_invoke$arity$3(G__34675,G__34676,G__34677) : run_agent_BANG_.call(null,G__34675,G__34676,G__34677));
});
knoxx.backend.actions.invoke_sub_agent.fire_and_forget_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$fire_and_forget_BANG_(ctx,payload){
return knoxx.backend.actions.invoke_sub_agent.spawn_once_BANG_(ctx,payload).then((function (result){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"fire-and-forget","fire-and-forget",-940372776),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"result-key","result-key",-521043082),new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"spawn-result","spawn-result",-523764589),result], null);
}));
});
knoxx.backend.actions.invoke_sub_agent.await_result_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$await_result_BANG_(ctx,payload){
return Promise.race(cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.actions.invoke_sub_agent.spawn_once_BANG_(ctx,payload),knoxx.backend.actions.invoke_sub_agent.timeout_promise(payload)], null))).then((function (result){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"await","await",-298732162),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"result-key","result-key",-521043082),new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"result","result",1415092211),result], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"await","await",-298732162),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131),new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"result-key","result-key",-521043082),new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(payload),new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()], null);
}));
});
knoxx.backend.actions.invoke_sub_agent.run_payload_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$run_payload_BANG_(ctx,payload){
var G__34693 = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(payload);
var G__34693__$1 = (((G__34693 instanceof cljs.core.Keyword))?G__34693.fqn:null);
switch (G__34693__$1) {
case "fire-and-forget":
return knoxx.backend.actions.invoke_sub_agent.fire_and_forget_BANG_(ctx,payload);

break;
case "await":
return knoxx.backend.actions.invoke_sub_agent.await_result_BANG_(ctx,payload);

break;
case "collect":
return knoxx.backend.actions.invoke_sub_agent.await_result_BANG_(ctx,payload);

break;
default:
return knoxx.backend.actions.invoke_sub_agent.fire_and_forget_BANG_(ctx,payload);

}
});
knoxx.backend.actions.invoke_sub_agent.collect_results_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$collect_results_BANG_(ctx,payloads){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__34696_SHARP_){
return knoxx.backend.actions.invoke_sub_agent.run_payload_BANG_(ctx,p1__34696_SHARP_);
}),payloads))).then((function (results){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),cljs.core.every_QMARK_(new cljs.core.Keyword(null,"ok","ok",967785236),results),new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"collect","collect",-284321549),new cljs.core.Keyword(null,"results","results",-1134170113),cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,result){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(acc,new cljs.core.Keyword(null,"result-key","result-key",-521043082).cljs$core$IFn$_invoke$arity$1(result),result);
}),cljs.core.PersistentArrayMap.EMPTY,results),new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(results)], null);
}));
});
knoxx.backend.actions.invoke_sub_agent.payload_for_id_BANG_ = (function knoxx$backend$actions$invoke_sub_agent$payload_for_id_BANG_(ctx,action_with,shared_config,sub_id){
var map__34700 = ctx;
var map__34700__$1 = cljs.core.__destructure_map(map__34700);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34700__$1,new cljs.core.Keyword(null,"config","config",994861415));
var event = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34700__$1,new cljs.core.Keyword(null,"event","event",301435442));
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34700__$1,new cljs.core.Keyword(null,"job","job",850873087));
var id = knoxx.backend.actions.invoke_sub_agent.id__GT_string(sub_id);
return knoxx.backend.actions.invoke_sub_agent.resolve_sub_agent_contract_BANG_(config,action_with,id).then((function (loaded_contract){
var sub_agent_contract = (function (){var or__5142__auto__ = loaded_contract;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.actions.invoke_sub_agent.fallback_contract(id);
}
})();
if((!(knoxx.backend.actions.invoke_sub_agent.enabled_contract_QMARK_(sub_agent_contract)))){
throw (new Error((""+"Sub-agent disabled: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(id))));
} else {
return knoxx.backend.actions.invoke_sub_agent.build_sub_agent_payload(config,job,event,id,shared_config,sub_agent_contract);
}
}));
});
knoxx.backend.actions.registry.run_action_BANG_.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),(function (p__34720,action){
var map__34721 = p__34720;
var map__34721__$1 = cljs.core.__destructure_map(map__34721);
var ctx = map__34721__$1;
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34721__$1,new cljs.core.Keyword(null,"config","config",994861415));
var action_with = new cljs.core.Keyword("action","with","action/with",-243371526).cljs$core$IFn$_invoke$arity$2(action,cljs.core.PersistentArrayMap.EMPTY);
var sub_agent_ids = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"sub-agents","sub-agents",-1939042605).cljs$core$IFn$_invoke$arity$1(action_with);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"sub-agent-ids","sub-agent-ids",-1693661147).cljs$core$IFn$_invoke$arity$1(action_with);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})();
var explicit_mode = (function (){var or__5142__auto__ = knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword(new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(action_with));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.actions.invoke_sub_agent.mode__GT_keyword(new cljs.core.Keyword("sub-agent","mode","sub-agent/mode",-974519373).cljs$core$IFn$_invoke$arity$1(action_with));
}
})();
var shared_config = (function (){var G__34731 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"sub-agent-config","sub-agent-config",-1509915250).cljs$core$IFn$_invoke$arity$1(action_with);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
if(cljs.core.truth_(explicit_mode)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34731,new cljs.core.Keyword(null,"mode","mode",654403691),explicit_mode);
} else {
return G__34731;
}
})();
if(cljs.core.truth_(new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(action_with))){
return knoxx.backend.actions.invoke_sub_agent.payload_for_id_BANG_(ctx,action_with,shared_config,new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(action_with)).then((function (p1__34708_SHARP_){
return knoxx.backend.actions.invoke_sub_agent.run_payload_BANG_(ctx,p1__34708_SHARP_);
}));
} else {
if(cljs.core.seq(sub_agent_ids)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__34713_SHARP_){
return knoxx.backend.actions.invoke_sub_agent.payload_for_id_BANG_(ctx,action_with,shared_config,p1__34713_SHARP_);
}),sub_agent_ids))).then((function (payloads){
var payloads__$1 = cljs.core.vec(payloads);
var mode = (function (){var or__5142__auto__ = explicit_mode;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(cljs.core.first(payloads__$1));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"fire-and-forget","fire-and-forget",-940372776);
}
}
})();
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"fire-and-forget","fire-and-forget",-940372776),mode)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__34714_SHARP_){
return knoxx.backend.actions.invoke_sub_agent.fire_and_forget_BANG_(ctx,p1__34714_SHARP_);
}),payloads__$1))).then((function (results){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),cljs.core.every_QMARK_(new cljs.core.Keyword(null,"ok","ok",967785236),results),new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"fire-and-forget","fire-and-forget",-940372776),new cljs.core.Keyword(null,"results","results",-1134170113),cljs.core.vec(results),new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(results)], null);
}));
} else {
return knoxx.backend.actions.invoke_sub_agent.collect_results_BANG_(ctx,payloads__$1);
}
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword(null,"error","error",-978969032),"No sub-agent-id or sub-agents specified in action/with"], null));

}
}
}));

//# sourceMappingURL=knoxx.backend.actions.invoke_sub_agent.js.map
