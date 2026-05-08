import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.actions.registry.js";
goog.provide('knoxx.backend.actions.invoke_agent');
knoxx.backend.actions.registry.run_action_BANG_.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("invoke","agent","invoke/agent",467801765),(function (p__523539,action){
var map__523540 = p__523539;
var map__523540__$1 = cljs.core.__destructure_map(map__523540);
var run_agent_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523540__$1,new cljs.core.Keyword(null,"run-agent!","run-agent!",-1490275853));
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523540__$1,new cljs.core.Keyword(null,"config","config",994861415));
var event = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523540__$1,new cljs.core.Keyword(null,"event","event",301435442));
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523540__$1,new cljs.core.Keyword(null,"job","job",850873087));
var job_agent_spec = new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$2(job,cljs.core.PersistentArrayMap.EMPTY);
var action_with = new cljs.core.Keyword("action","with","action/with",-243371526).cljs$core$IFn$_invoke$arity$2(action,cljs.core.PersistentArrayMap.EMPTY);
var merged_spec = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([job_agent_spec,action_with], 0));
var job_with_agent = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(job,new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),merged_spec);
return (run_agent_BANG_.cljs$core$IFn$_invoke$arity$3 ? run_agent_BANG_.cljs$core$IFn$_invoke$arity$3(config,job_with_agent,event) : run_agent_BANG_.call(null,config,job_with_agent,event));
}));

//# sourceMappingURL=knoxx.backend.actions.invoke_agent.js.map
