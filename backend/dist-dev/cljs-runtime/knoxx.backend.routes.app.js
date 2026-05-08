import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.routes.admin.js";
import "./knoxx.backend.routes.actors.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agent_turns.js";
import "./knoxx.backend.app_shapes.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.routes.contracts.js";
import "./knoxx.backend.routes.documents.js";
import "./knoxx.backend.guards.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.routes.memory.js";
import "./knoxx.backend.routes.models.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.util.parse.js";
import "./knoxx.backend.util.time.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.session_titles.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.routes.tools.js";
import "./knoxx.backend.tooling.js";
import "./knoxx.backend.turn_control.js";
import "./knoxx.backend.routes.voice.js";
import "./knoxx.backend.routes.workspace_media.js";
import "./knoxx.backend.routes.studio.js";
import "./knoxx.backend.routes.translation.js";
import "./shadow.cljs.modern.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.routes.app');
knoxx.backend.routes.app.queue_chat_start_BANG_ = (function knoxx$backend$routes$app$queue_chat_start_BANG_(runtime,config,reply,agent_ctx,policy_model,body,accepted_response){
return knoxx.backend.agent_turns.validate_chat_policy_BANG_(agent_ctx,policy_model).then((function (validated){
return knoxx.backend.agent_turns.send_agent_turn_BANG_(runtime,config,body).then((function (sent){
return knoxx.backend.http.json_response_BANG_(reply,(202),accepted_response);
}));
}));
});
knoxx.backend.routes.app.compact_agent_spec_overrides = (function knoxx$backend$routes$app$compact_agent_spec_overrides(agent_spec){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$1((function (p__70729){
var vec__70730 = p__70729;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70730,(0),null);
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70730,(1),null);
return (((value == null)) || (((((typeof value === 'string') && (clojure.string.blank_QMARK_(value)))) || (((cljs.core.sequential_QMARK_(value)) && (cljs.core.empty_QMARK_(value)))))));
})),agent_spec);
});
knoxx.backend.routes.app.merged_agent_spec = (function knoxx$backend$routes$app$merged_agent_spec(config,parsed){
var requested = knoxx.backend.routes.app.compact_agent_spec_overrides((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
var requested_actor_id = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(requested,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tooling.default_actor_id(config);
}
})();
var requested_contract_id = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(requested,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,requested_actor_id);
}
})();
var resolved = knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,requested_contract_id,requested_actor_id);
var resolved_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(resolved);
var G__70733 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.select_keys(resolved,new cljs.core.PersistentVector(null, 9, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817),new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557)], null)),requested], 0));
var G__70733__$1 = (cljs.core.truth_(requested_actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70733,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),requested_actor_id):G__70733);
var G__70733__$2 = ((cljs.core.seq(new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817).cljs$core$IFn$_invoke$arity$1(resolved)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70733__$1,new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049),new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817).cljs$core$IFn$_invoke$arity$1(resolved)):G__70733__$1);
if(cljs.core.truth_(resolved_id)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70733__$2,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),resolved_id);
} else {
return G__70733__$2;
}
});
knoxx.backend.routes.app.requested_role = (function knoxx$backend$routes$app$requested_role(parsed){
var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"role","role",-736691072)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__70734 = new cljs.core.Keyword(null,"auth-context","auth-context",320032325).cljs$core$IFn$_invoke$arity$1(parsed);
var G__70734__$1 = (((G__70734 == null))?null:new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(G__70734));
var G__70734__$2 = (((G__70734__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70734__$1)));
var G__70734__$3 = (((G__70734__$2 == null))?null:clojure.string.trim(G__70734__$2));
if((G__70734__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__70734__$3);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__70735 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270)], null));
var G__70735__$1 = (((G__70735 == null))?null:cljs.core.seq(G__70735));
var G__70735__$2 = (((G__70735__$1 == null))?null:cljs.core.first(G__70735__$1));
var G__70735__$3 = (((G__70735__$2 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70735__$2)));
var G__70735__$4 = (((G__70735__$3 == null))?null:clojure.string.trim(G__70735__$3));
if((G__70735__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__70735__$4);
}
}
}
});
knoxx.backend.routes.app.allow_policy_QMARK_ = (function knoxx$backend$routes$app$allow_policy_QMARK_(policy){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("allow",(function (){var G__70736 = new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy);
var G__70736__$1 = (((G__70736 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70736)));
if((G__70736__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__70736__$1);
}
})());
});
knoxx.backend.routes.app.requested_tool_policies = (function knoxx$backend$routes$app$requested_tool_policies(parsed){
var from_spec = cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var from_auth = cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
if(cljs.core.seq(from_spec)){
return from_spec;
} else {
if(cljs.core.seq(from_auth)){
return from_auth;
} else {
return cljs.core.PersistentVector.EMPTY;

}
}
});
knoxx.backend.routes.app.effective_tool_policies = (function knoxx$backend$routes$app$effective_tool_policies(ctx,parsed){
var requested = knoxx.backend.routes.app.requested_tool_policies(parsed);
if((((ctx == null)) && (cljs.core.seq(requested)))){
return requested;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = (ctx == null);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"auth-context","auth-context",320032325).cljs$core$IFn$_invoke$arity$1(parsed);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
} else {
if(cljs.core.empty_QMARK_(requested)){
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
} else {
if(knoxx.backend.authz.system_admin_QMARK_(ctx)){
return requested;
} else {
var allowed = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.allow_policy_QMARK_,new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx))));
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70737_SHARP_){
return cljs.core.contains_QMARK_(allowed,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(p1__70737_SHARP_));
}),requested));

}
}
}
}
});
knoxx.backend.routes.app.effective_auth_context = (function knoxx$backend$routes$app$effective_auth_context(ctx,parsed){
var base = (function (){var or__5142__auto__ = ctx;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"auth-context","auth-context",320032325).cljs$core$IFn$_invoke$arity$1(parsed);
}
})();
var requested_actor_id = (function (){var G__70738 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"actor-id","actor-id",897721067)], null));
var G__70738__$1 = (((G__70738 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70738)));
var G__70738__$2 = (((G__70738__$1 == null))?null:clojure.string.trim(G__70738__$1));
if((G__70738__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70738__$2);
}
})();
var requested_role_slug = knoxx.backend.routes.app.requested_role(parsed);
var role_slugs = (cljs.core.truth_((function (){var and__5140__auto__ = (base == null);
if(and__5140__auto__){
return requested_role_slug;
} else {
return and__5140__auto__;
}
})())?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [requested_role_slug], null):(cljs.core.truth_((function (){var and__5140__auto__ = requested_role_slug;
if(cljs.core.truth_(and__5140__auto__)){
return ((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (cljs.core.contains_QMARK_(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentHashSet.EMPTY,(function (){var or__5142__auto____$1 = new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(base);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),requested_role_slug)));
} else {
return and__5140__auto__;
}
})())?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [requested_role_slug], null):cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(base);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())
));
var tool_policies = knoxx.backend.routes.app.effective_tool_policies(ctx,parsed);
var resource_policies = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364).cljs$core$IFn$_invoke$arity$1(base);
}
}
})();
if(cljs.core.truth_((function (){var or__5142__auto__ = base;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = requested_actor_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = requested_role_slug;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.seq(tool_policies);
if(or__5142__auto____$3){
return or__5142__auto____$3;
} else {
return resource_policies;
}
}
}
}
})())){
var G__70739 = (function (){var or__5142__auto__ = base;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var G__70739__$1 = (cljs.core.truth_(requested_actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70739,new cljs.core.Keyword(null,"actorId","actorId",989542370),requested_actor_id):G__70739);
var G__70739__$2 = ((cljs.core.seq(role_slugs))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70739__$1,new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),role_slugs):G__70739__$1);
var G__70739__$3 = ((((cljs.core.seq(tool_policies)) || ((!((base == null))))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70739__$2,new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),tool_policies):G__70739__$2);
if(cljs.core.truth_(resource_policies)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70739__$3,new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364),resource_policies);
} else {
return G__70739__$3;
}
} else {
return null;
}
});
knoxx.backend.routes.app.auth_context_with_actor = (function knoxx$backend$routes$app$auth_context_with_actor(ctx,actor_id){
var temp__5823__auto__ = (function (){var G__70740 = actor_id;
var G__70740__$1 = (((G__70740 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70740)));
var G__70740__$2 = (((G__70740__$1 == null))?null:clojure.string.trim(G__70740__$1));
if((G__70740__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70740__$2);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var actor_id_STAR_ = temp__5823__auto__;
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = ctx;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword(null,"actorId","actorId",989542370),actor_id_STAR_);
} else {
return ctx;
}
});
knoxx.backend.routes.app.active_run_summary = (function knoxx$backend$routes$app$active_run_summary(run,session){
var messages = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var user_msg = cljs.core.some((function (p1__70741_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("user",(function (){var G__70742 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__70741_SHARP_);
if((G__70742 == null)){
return null;
} else {
return clojure.string.lower_case(G__70742);
}
})())){
return p1__70741_SHARP_;
} else {
return null;
}
}),cljs.core.reverse(messages));
var conversation_id = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(run);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322),new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067),new cljs.core.Keyword(null,"resource_policies","resource_policies",-1190579829),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"latest_user_message","latest_user_message",278994764),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),new cljs.core.Keyword(null,"tokens_per_s","tokens_per_s",1005457231),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"event_count","event_count",-1889732422),new cljs.core.Keyword(null,"latest_event","latest_event",-573333605),new cljs.core.Keyword(null,"active_turn_registered","active_turn_registered",-892675300),new cljs.core.Keyword(null,"tool_receipt_count","tool_receipt_count",-628689028),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498),new cljs.core.Keyword(null,"model","model",331153215)],[cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (b){
return cljs.core.select_keys(b,new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"toolName","toolName",869440778),new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"at","at",1476951349),new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474),new cljs.core.Keyword(null,"outputPreview","outputPreview",-747507208),new cljs.core.Keyword(null,"isError","isError",-1727958473)], null));
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114).cljs$core$IFn$_invoke$arity$1(run),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p){
return cljs.core.select_keys(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p,new cljs.core.Keyword(null,"data","data",-232669377)),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"url","url",276297046),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"text","text",-1790561697)], null));
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content-parts","content-parts",684529019).cljs$core$IFn$_invoke$arity$1(user_msg);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322).cljs$core$IFn$_invoke$arity$1(run),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (r){
return cljs.core.select_keys(r,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"input_preview","input_preview",2048529734),new cljs.core.Keyword(null,"result_preview","result_preview",215554859),new cljs.core.Keyword(null,"started_at","started_at",856896776),new cljs.core.Keyword(null,"ended_at","ended_at",1150683059)], null));
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(run,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"resources","resources",1632806811),new cljs.core.Keyword(null,"agentResourcePolicies","agentResourcePolicies",-1357376229)], null)),new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(user_msg),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(run),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(run,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050)], null)),new cljs.core.Keyword(null,"tokens_per_s","tokens_per_s",1005457231).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run),cljs.core.boolean$(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(session)),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(run),cljs.core.count((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"events","events",1792552201).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),(function (){var G__70743 = new cljs.core.Keyword(null,"events","events",1792552201).cljs$core$IFn$_invoke$arity$1(run);
var G__70743__$1 = (((G__70743 == null))?null:cljs.core.last(G__70743));
if((G__70743__$1 == null)){
return null;
} else {
return cljs.core.select_keys(G__70743__$1,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),new cljs.core.Keyword(null,"preview","preview",451279890),new cljs.core.Keyword(null,"at","at",1476951349)], null));
}
})(),cljs.core.boolean$((function (){var and__5140__auto__ = conversation_id;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.turn_control.active_turn(conversation_id);
} else {
return and__5140__auto__;
}
})()),cljs.core.count((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]);
});
knoxx.backend.routes.app.active_session_summary = (function knoxx$backend$routes$app$active_session_summary(session){
var messages = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var user_msg = cljs.core.some((function (p1__70744_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("user",(function (){var G__70745 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__70744_SHARP_);
if((G__70745 == null)){
return null;
} else {
return clojure.string.lower_case(G__70745);
}
})())){
return p1__70744_SHARP_;
} else {
return null;
}
}),cljs.core.reverse(messages));
var conversation_id = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"resource_policies","resource_policies",-1190579829),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"latest_user_message","latest_user_message",278994764),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"event_count","event_count",-1889732422),new cljs.core.Keyword(null,"latest_event","latest_event",-573333605),new cljs.core.Keyword(null,"active_turn_registered","active_turn_registered",-892675300),new cljs.core.Keyword(null,"tool_receipt_count","tool_receipt_count",-628689028),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"model","model",331153215)],[new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"resource_policies","resource_policies",-1190579829).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(user_msg),conversation_id,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(session);
}
})(),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session),cljs.core.boolean$(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(session)),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(session),(0),null,cljs.core.boolean$((function (){var and__5140__auto__ = conversation_id;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.turn_control.active_turn(conversation_id);
} else {
return and__5140__auto__;
}
})()),(0),new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(session)]);
});
knoxx.backend.routes.app.live_active_agent_summaries_BANG_ = (function knoxx$backend$routes$app$live_active_agent_summaries_BANG_(limit,include_all_QMARK_){
var limit__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (25);
}
})());
var redis_client = knoxx.backend.redis_client.get_client();
var sessions_by_id = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (session){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session),session], null);
})),knoxx.backend.session_store.active_session_snapshots());
var run_items = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (run){
return knoxx.backend.routes.app.active_run_summary(run,cljs.core.get.cljs$core$IFn$_invoke$arity$2(sessions_by_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(run)));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70747_SHARP_){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["waiting_input",null,"running",null,"queued",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__70747_SHARP_));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70746_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),p1__70746_SHARP_);
}),cljs.core.deref(knoxx.backend.run_state.run_order_STAR_))))));
if(cljs.core.not(redis_client)){
return Promise.resolve(cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit__$1,run_items));
} else {
return knoxx.backend.session_store.list_active_sessions(redis_client).then((function (ids){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__70748_SHARP_){
return knoxx.backend.session_store.get_session(redis_client,p1__70748_SHARP_);
}),cljs.core.vec(ids)))).then((function (sessions_js){
var run_session_ids = cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session_id","session_id",1584799627),run_items));
var session_items = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.active_session_summary,cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__70750_SHARP_){
return cljs.core.contains_QMARK_(run_session_ids,new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(p1__70750_SHARP_));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70749_SHARP_){
var or__5142__auto__ = include_all_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["waiting_input",null,"running",null,"queued",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__70749_SHARP_));
}
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(sessions_js,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))))))));
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit__$1,cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3((function (p1__70751_SHARP_){
var or__5142__auto__ = new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(p1__70751_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(p1__70751_SHARP_);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
}),(function (p1__70753_SHARP_,p2__70752_SHARP_){
return cljs.core.compare(p2__70752_SHARP_,p1__70753_SHARP_);
}),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(run_items,session_items))));
}));
}));
}
});
knoxx.backend.routes.app.SESSION_RECOVERY_STALE_MS = (60000);
/**
 * If turn-control has an entry for conversation-id but the underlying Proxx
 * session shows no active streaming or current turn, the entry is a ghost
 * from a previous hung run. Unregister it so zombie recovery can proceed.
 */
knoxx.backend.routes.app.clear_ghost_turn_BANG_ = (function knoxx$backend$routes$app$clear_ghost_turn_BANG_(conversation_id){
var agent_session = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
var streaming_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
return (agent_session["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
var current_turn_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
try{return (!(((agent_session["currentTurn"]) == null)));
}catch (e70754){if((e70754 instanceof Error)){
var _ = e70754;
return false;
} else {
throw e70754;

}
}} else {
return and__5140__auto__;
}
})();
if(((cljs.core.not(streaming_QMARK_)) && (cljs.core.not(current_turn_QMARK_)))){
return knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$1(conversation_id);
} else {
return null;
}
});
knoxx.backend.routes.app.runtime_processing_session_QMARK_ = (function knoxx$backend$routes$app$runtime_processing_session_QMARK_(conversation_id){
var agent_session = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
var streaming_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
return (agent_session["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
var current_turn_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
try{return (!(((agent_session["currentTurn"]) == null)));
}catch (e70755){if((e70755 instanceof Error)){
var _ = e70755;
return false;
} else {
throw e70755;

}
}} else {
return and__5140__auto__;
}
})();
var registered_turn_QMARK_ = (!((knoxx.backend.turn_control.active_turn(conversation_id) == null)));
var or__5142__auto__ = streaming_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = current_turn_QMARK_;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return registered_turn_QMARK_;
}
}
});
knoxx.backend.routes.app.parse_iso_ms = (function knoxx$backend$routes$app$parse_iso_ms(value){
var parsed = Date.parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(cljs.core.truth_(isNaN(parsed))){
return null;
} else {
return parsed;
}
});
knoxx.backend.routes.app.latest_run_event_BANG_ = (function knoxx$backend$routes$app$latest_run_event_BANG_(run_id){
var run_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(run_id__$1)){
return Promise.resolve(null);
} else {
if(cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [run_id__$1,new cljs.core.Keyword(null,"events","events",1792552201)], null)))){
return Promise.resolve(cljs.core.last(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [run_id__$1,new cljs.core.Keyword(null,"events","events",1792552201)], null))));
} else {
if((knoxx.backend.redis_client.get_client() == null)){
return Promise.resolve(null);
} else {
return knoxx.backend.redis_client.lrange_json(knoxx.backend.redis_client.get_client(),knoxx.backend.run_state.run_events_key(run_id__$1),(0),(0)).then(cljs.core.first).catch(cljs.core.constantly(null));

}
}
}
});
knoxx.backend.routes.app.stale_running_session_QMARK_ = (function knoxx$backend$routes$app$stale_running_session_QMARK_(session,latest_event){
var stamp = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(latest_event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(session);
}
}
})();
var stamp_ms = knoxx.backend.routes.app.parse_iso_ms(stamp);
return (((stamp_ms == null)) || (((Date.now() - stamp_ms) > knoxx.backend.routes.app.SESSION_RECOVERY_STALE_MS)));
});
knoxx.backend.routes.app.dev_hmr_response = (function knoxx$backend$routes$app$dev_hmr_response(){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"version","version",425292698),"v2",new cljs.core.Keyword(null,"at","at",1476951349),knoxx.backend.util.time.now_iso()], null);
});
knoxx.backend.routes.app.proxy_ok = (function knoxx$backend$routes$app$proxy_ok(reply,resp){
return knoxx.backend.http.send_fetch_response_BANG_(reply,resp);
});
knoxx.backend.routes.app.proxy_err = (function knoxx$backend$routes$app$proxy_err(reply,prefix,err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.fetch_json_ok = (function knoxx$backend$routes$app$fetch_json_ok(reply,resp){
return knoxx.backend.http.json_response_BANG_(reply,(function (){var or__5142__auto__ = (resp["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (200);
}
})(),(resp["body"]));
});
knoxx.backend.routes.app.fetch_json_err = (function knoxx$backend$routes$app$fetch_json_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),err.message], null));
});
knoxx.backend.routes.app.fetch_json_err_detail = (function knoxx$backend$routes$app$fetch_json_err_detail(reply,prefix,err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.pg_query_ok = (function knoxx$backend$routes$app$pg_query_ok(reply,result){
var rows = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((result["rows"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"rows","rows",850049680),rows,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(rows)], null));
});
knoxx.backend.routes.app.pg_query_table_ok = (function knoxx$backend$routes$app$pg_query_table_ok(reply,table,result){
var rows = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((result["rows"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"table","table",-564943036),table,new cljs.core.Keyword(null,"rows","rows",850049680),rows,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(rows)], null));
});
knoxx.backend.routes.app.pg_query_err = (function knoxx$backend$routes$app$pg_query_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(400),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),err.message], null));
});
knoxx.backend.routes.app.zombie_recovered = (function knoxx$backend$routes$app$zombie_recovered(queue_turn_BANG_,_){
return (queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1 ? queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1("Async direct agent chat failed (recovered from zombie)") : queue_turn_BANG_.call(null,"Async direct agent chat failed (recovered from zombie)"));
});
knoxx.backend.routes.app.zombie_recovery_failed = (function knoxx$backend$routes$app$zombie_recovery_failed(reply,err){
console.error("Failed to abort zombie session",err);

return knoxx.backend.http.json_response_BANG_(reply,(409),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+"Agent is already processing. Zombie recovery failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"code","code",1586293142),"agent_already_processing",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"can_send","can_send",534936371),false], null));
});
knoxx.backend.routes.app.session_check_failed = (function knoxx$backend$routes$app$session_check_failed(queue_turn_BANG_,err){
console.error("Session status check failed",err);

return (queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1 ? queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1("Async agent chat failed") : queue_turn_BANG_.call(null,"Async agent chat failed"));
});
knoxx.backend.routes.app.health_deps_ok = (function knoxx$backend$routes$app$health_deps_ok(reply,proxx_configured,openplanner_configured,parts){
var proxx_res = (parts[(0)]);
var openplanner_res = (parts[(1)]);
var proxx_ok = (function (){var and__5140__auto__ = proxx_configured;
if(cljs.core.truth_(and__5140__auto__)){
return (proxx_res["ok"]);
} else {
return and__5140__auto__;
}
})();
var openplanner_ok = (function (){var and__5140__auto__ = openplanner_configured;
if(cljs.core.truth_(and__5140__auto__)){
return (openplanner_res["ok"]);
} else {
return and__5140__auto__;
}
})();
var healthy = (function (){var and__5140__auto__ = proxx_ok;
if(cljs.core.truth_(and__5140__auto__)){
return openplanner_ok;
} else {
return and__5140__auto__;
}
})();
return knoxx.backend.http.json_response_BANG_(reply,(cljs.core.truth_(healthy)?(200):(503)),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),(cljs.core.truth_(healthy)?"ok":"unhealthy"),new cljs.core.Keyword(null,"service","service",-1963054559),"knoxx-backend-cljs",new cljs.core.Keyword(null,"dependencies","dependencies",1108064605),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"proxx","proxx",289303663),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"configured","configured",-884777889),proxx_configured,new cljs.core.Keyword(null,"reachable","reachable",-1495191549),cljs.core.boolean$(proxx_ok),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(proxx_res["status"]),new cljs.core.Keyword(null,"detail","detail",-1545345025),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((proxx_res["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null),new cljs.core.Keyword(null,"openplanner","openplanner",-175854128),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"configured","configured",-884777889),openplanner_configured,new cljs.core.Keyword(null,"reachable","reachable",-1495191549),cljs.core.boolean$(openplanner_ok),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(openplanner_res["status"]),new cljs.core.Keyword(null,"detail","detail",-1545345025),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((openplanner_res["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null)], null)], null));
});
knoxx.backend.routes.app.health_deps_err = (function knoxx$backend$routes$app$health_deps_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(503),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"unhealthy",new cljs.core.Keyword(null,"service","service",-1963054559),"knoxx-backend-cljs",new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.data_health_ok = (function knoxx$backend$routes$app$data_health_ok(reply,results){
var r = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"services","services",970478783),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"openplanner","openplanner",-175854128),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(0)),new cljs.core.Keyword(null,"proxx","proxx",289303663),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(1)),new cljs.core.Keyword(null,"ingestion","ingestion",1555117680),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(2)),new cljs.core.Keyword(null,"graph-weaver","graph-weaver",1931242055),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(3)),new cljs.core.Keyword(null,"shuvcrawl","shuvcrawl",1133487479),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(4)),new cljs.core.Keyword(null,"vexx","vexx",1931567209),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(5)),new cljs.core.Keyword(null,"eros-eris-field-app","eros-eris-field-app",-55973265),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(6)),new cljs.core.Keyword(null,"myrmex","myrmex",-95374765),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(7))], null)], null));
});
knoxx.backend.routes.app.data_health_err = (function knoxx$backend$routes$app$data_health_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(500),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),err.message], null));
});
knoxx.backend.routes.app.deps = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848),new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000),new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966),new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310),new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686)],[knoxx.backend.http.request_query_string,knoxx.backend.app_shapes.route_BANG_,null,knoxx.backend.http.json_response_BANG_,knoxx.backend.authz.with_request_context_BANG_,knoxx.backend.http.send_fetch_response_BANG_,null,knoxx.backend.http.error_response_BANG_,knoxx.backend.http.bearer_headers,knoxx.backend.text.clip_text,knoxx.backend.authz.ensure_permission_BANG_,knoxx.backend.http.fetch_json]);
knoxx.backend.routes.app.mongo_collections_ok = (function knoxx$backend$routes$app$mongo_collections_ok(reply,results){
var r = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"documents","documents",-1582333455),(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(0))["body"]),new cljs.core.Keyword(null,"graph","graph",1558099509),(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(r,(1))["body"])], null));
});
knoxx.backend.routes.app.undo_session_ok = (function knoxx$backend$routes$app$undo_session_ok(reply,session_id,conversation_id,removed_count,rewound_messages){
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"removed_count","removed_count",-1925224714),removed_count,new cljs.core.Keyword(null,"remaining_messages","remaining_messages",134744241),cljs.core.count(rewound_messages)], null));
});
knoxx.backend.routes.app.undo_session_err = (function knoxx$backend$routes$app$undo_session_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(500),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.agents_active_ok = (function knoxx$backend$routes$app$agents_active_ok(reply,items){
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"runs","runs",-1553997798),items,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(items)], null));
});
knoxx.backend.routes.app.agents_active_err = (function knoxx$backend$routes$app$agents_active_err(reply,err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502));
});
knoxx.backend.routes.app.abort_admin_ok = (function knoxx$backend$routes$app$abort_admin_ok(reply,abort_result,conversation_id,resolved_session_id,resolved_run_id){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(abort_result,new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),resolved_session_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),resolved_run_id,new cljs.core.Keyword(null,"marked_aborted","marked_aborted",-489318151),true], 0)));
});
knoxx.backend.routes.app.run_events_ok = (function knoxx$backend$routes$app$run_events_ok(reply,run_id,events){
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"events","events",1792552201),events,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(events)], null));
});
knoxx.backend.routes.app.run_events_err = (function knoxx$backend$routes$app$run_events_err(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(500),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.shibboleth_ok = (function knoxx$backend$routes$app$shibboleth_ok(reply,request,body,data){
var session = (function (){var or__5142__auto__ = (data["session"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (session["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var ui_url = (((((!(clojure.string.blank_QMARK_(session_id)))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"shibboleth-ui-url","shibboleth-ui-url",358926658).cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.app.config)))))))?knoxx.backend.http.with_query_param(knoxx.backend.http.rewrite_localhost_url(new cljs.core.Keyword(null,"shibboleth-ui-url","shibboleth-ui-url",358926658).cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.app.config),request),"session",session_id):"");
if(clojure.string.blank_QMARK_(session_id)){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Shibboleth import did not return a session id"], null));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"ui_url","ui_url",1034204910),ui_url,new cljs.core.Keyword(null,"imported_item_count","imported_item_count",1631884122),cljs.core.count(knoxx.backend.http.js_array_seq((body["items"])))], null));
}
});
knoxx.backend.routes.app.shibboleth_import_failed = (function knoxx$backend$routes$app$shibboleth_import_failed(reply,resp){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Shibboleth import failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = ((resp["body"])["raw"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return JSON.stringify((resp["body"]));
}
})()))], null));
});
knoxx.backend.routes.app.shibboleth_unreachable = (function knoxx$backend$routes$app$shibboleth_unreachable(reply,err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Shibboleth is unreachable: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
});
knoxx.backend.routes.app.detect_zombies = (function knoxx$backend$routes$app$detect_zombies(conversation_id,session,session_id,queue_turn_BANG_,can_send_result,reply,latest_event){
knoxx.backend.routes.app.clear_ghost_turn_BANG_(conversation_id);

var stalled_QMARK_ = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))) && (((cljs.core.not(knoxx.backend.routes.app.runtime_processing_session_QMARK_(conversation_id))) && (knoxx.backend.routes.app.stale_running_session_QMARK_(session,latest_event)))));
if(stalled_QMARK_){
return knoxx.backend.session_store.complete_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,conversation_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),"Session was stale/zombie; auto-aborted before new turn.",new cljs.core.Keyword(null,"messages","messages",345434482),new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session)], null)).then((function (_){
return (queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1 ? queue_turn_BANG_.cljs$core$IFn$_invoke$arity$1("Async direct agent chat failed (recovered from zombie)") : queue_turn_BANG_.call(null,"Async direct agent chat failed (recovered from zombie)"));
})).catch((function (err){
console.error("Failed to abort zombie session",err);

return knoxx.backend.http.json_response_BANG_(reply,(409),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+"Agent is already processing. Zombie recovery failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"code","code",1586293142),"agent_already_processing",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"can_send","can_send",534936371),false], null));
}));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(409),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+"Agent is already processing. "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"reason","reason",-2070751759).cljs$core$IFn$_invoke$arity$1(can_send_result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"code","code",1586293142),"agent_already_processing",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),cljs.core.boolean$(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(session)),new cljs.core.Keyword(null,"can_send","can_send",534936371),false], null));
}
});
knoxx.backend.routes.app.handle_chat_start = (function knoxx$backend$routes$app$handle_chat_start(runtime,config,reply,ctx,request){
var node_crypto = shadow.esm.esm_import$node_crypto;
var parsed0 = knoxx.backend.app_shapes.normalize_chat_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
var parsed = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(parsed0,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),knoxx.backend.routes.app.merged_agent_spec(config,parsed0));
var agent_ctx = knoxx.backend.routes.app.effective_auth_context(ctx,parsed);
var policy_model = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
}
}
})();
var provided_session_id = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(parsed);
var session_id = knoxx.backend.agent_turns.ensure_session_id(node_crypto,provided_session_id);
var conversation_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return node_crypto.randomUUID();
}
})();
var run_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return node_crypto.randomUUID();
}
})();
var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(parsed,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),run_id,new cljs.core.Keyword(null,"mode","mode",654403691),"rag",new cljs.core.Keyword(null,"auth-context","auth-context",320032325),agent_ctx], 0));
var accepted_response = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"queued","queued",1701634607),new cljs.core.Keyword(null,"runId","runId",505587730),new cljs.core.Keyword(null,"ok","ok",967785236),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),new cljs.core.Keyword(null,"model","model",331153215)],[session_id,run_id,session_id,conversation_id,true,run_id,true,conversation_id,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
}
}
})()]);
var queue_turn_BANG_ = (function (_log_label){
return knoxx.backend.routes.app.queue_chat_start_BANG_(runtime,config,reply,agent_ctx,policy_model,body,accepted_response);
});
if(cljs.core.not(provided_session_id)){
return queue_turn_BANG_("Async agent chat failed");
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
var can_send_result = knoxx.backend.session_store.session_can_send_QMARK_(session);
if(cljs.core.truth_(new cljs.core.Keyword(null,"can-send","can-send",-704220819).cljs$core$IFn$_invoke$arity$1(can_send_result))){
var agent_session = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
var actively_streaming_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
return (agent_session["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(actively_streaming_QMARK_)){
return knoxx.backend.http.json_response_BANG_(reply,(409),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Agent is already processing. Specify streamingBehavior steer or followUp to queue the message.",new cljs.core.Keyword(null,"code","code",1586293142),"agent-already-processing",new cljs.core.Keyword(null,"has-active-stream","has-active-stream",1912435974),true,new cljs.core.Keyword(null,"can-send","can-send",-704220819),false], null));
} else {
return queue_turn_BANG_("Async agent chat failed");
}
} else {
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session)))?knoxx.backend.routes.app.latest_run_event_BANG_(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session)):Promise.resolve(null)).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.app.detect_zombies,conversation_id,session,session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([queue_turn_BANG_,can_send_result,reply], 0)));
}
})).catch((function (err){
console.error("Session status check failed",err);

return queue_turn_BANG_("Async agent chat failed");
}));
}
});
knoxx.backend.routes.app.handle_direct_start = (function knoxx$backend$routes$app$handle_direct_start(runtime,config,reply,ctx,request){
var node_crypto = shadow.esm.esm_import$node_crypto;
var parsed0 = knoxx.backend.app_shapes.normalize_chat_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
var parsed = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(parsed0,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),knoxx.backend.routes.app.merged_agent_spec(config,parsed0));
var agent_ctx = knoxx.backend.routes.app.effective_auth_context(ctx,parsed);
var policy_model = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(parsed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
}
}
})();
var provided_session_id = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(parsed);
var session_id = knoxx.backend.agent_turns.ensure_session_id(node_crypto,provided_session_id);
var conversation_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return node_crypto.randomUUID();
}
})();
var run_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return node_crypto.randomUUID();
}
})();
var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(parsed,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),run_id,new cljs.core.Keyword(null,"mode","mode",654403691),"direct",new cljs.core.Keyword(null,"auth-context","auth-context",320032325),agent_ctx], 0));
var accepted_response = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"queued","queued",1701634607),true,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(body),new cljs.core.Keyword(null,"model","model",331153215),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"model","model",331153215)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
}
}
})()], null);
var queue_turn_BANG_ = (function (log_label){
return knoxx.backend.agent_turns.validate_chat_policy_BANG_(agent_ctx,policy_model).then((function (_){
knoxx.backend.agent_turns.send_agent_turn_BANG_(runtime,config,body).then((function (___$1){
return null;
})).catch((function (err){
return console.error(log_label,err);
}));

return knoxx.backend.http.json_response_BANG_(reply,(202),accepted_response);
})).catch((function (err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(429));
}));
});
if(cljs.core.not(provided_session_id)){
return queue_turn_BANG_("Async direct agent chat failed");
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
var can_send_result = knoxx.backend.session_store.session_can_send_QMARK_(session);
if(cljs.core.truth_(new cljs.core.Keyword(null,"can-send","can-send",-704220819).cljs$core$IFn$_invoke$arity$1(can_send_result))){
var agent_session = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
var actively_streaming_QMARK_ = (function (){var and__5140__auto__ = agent_session;
if(cljs.core.truth_(and__5140__auto__)){
return (agent_session["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(actively_streaming_QMARK_)){
return knoxx.backend.http.json_response_BANG_(reply,(409),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Agent is already processing. Specify streamingBehavior ('steer' or 'followUp') to queue the message.",new cljs.core.Keyword(null,"code","code",1586293142),"agent_already_processing",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),true,new cljs.core.Keyword(null,"can_send","can_send",534936371),false], null));
} else {
return queue_turn_BANG_("Async direct agent chat failed");
}
} else {
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session)))?knoxx.backend.routes.app.latest_run_event_BANG_(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session)):Promise.resolve(null)).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.app.detect_zombies,conversation_id,session,session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([queue_turn_BANG_,can_send_result,reply], 0)));
}
}),(function (err){
console.error("Session status check failed",err);

return queue_turn_BANG_("Async direct agent chat failed");
}));
}
});
knoxx.backend.routes.app.handle_admin_abort = (function knoxx$backend$routes$app$handle_admin_abort(reply,ctx,request){
var raw = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var requested_conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["conversationId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var requested_session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["session_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["sessionId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var requested_run_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["run_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["runId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var reason = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["reason"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "operator_abort";
}
})()));
var redis_client = knoxx.backend.redis_client.get_client();
var run = ((clojure.string.blank_QMARK_(requested_run_id))?null:cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),requested_run_id));
var session_id = (function (){var or__5142__auto__ = (function (){var G__70756 = requested_session_id;
if((G__70756 == null)){
return null;
} else {
return cljs.core.not_empty(G__70756);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(run);
}
})();
return (cljs.core.truth_((function (){var and__5140__auto__ = redis_client;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))));
} else {
return and__5140__auto__;
}
})())?knoxx.backend.session_store.get_session(redis_client,session_id):Promise.resolve(null)).then((function (session){
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__70758 = requested_conversation_id;
if((G__70758 == null)){
return null;
} else {
return cljs.core.not_empty(G__70758);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})()));
var resolved_session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var resolved_run_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = requested_run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})()));
if(clojure.string.blank_QMARK_(conversation_id)){
return knoxx.backend.http.json_response_BANG_(reply,(400),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"conversation_id, session_id, or run_id is required"], null));
} else {
return knoxx.backend.turn_control.abort_active_turn_BANG_(conversation_id,reason).then((function (abort_result){
if(clojure.string.blank_QMARK_(resolved_run_id)){
} else {
knoxx.backend.run_state.update_run_BANG_(resolved_run_id,(function (r){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(r,new cljs.core.Keyword(null,"status","status",-1997798413),"aborted",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"error","error",-978969032),reason,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso()], 0));
}));
}

return (cljs.core.truth_((function (){var and__5140__auto__ = redis_client;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(resolved_session_id)));
} else {
return and__5140__auto__;
}
})())?knoxx.backend.session_store.update_session_BANG_(redis_client,resolved_session_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"aborted",new cljs.core.Keyword(null,"error","error",-978969032),reason,new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false], null)):Promise.resolve(null)).then((function (_){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(abort_result,new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),resolved_session_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),resolved_run_id,new cljs.core.Keyword(null,"marked_aborted","marked_aborted",-489318151),true], 0)));
}));
}));
}
})).catch((function (err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(409));
}));
});
knoxx.backend.routes.app.handle_session_status = (function knoxx$backend$routes$app$handle_session_status(runtime,config,reply,request){
var session_id = (function (){var or__5142__auto__ = (request["query"]["session_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["sessionId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var conversation_id = (function (){var or__5142__auto__ = (request["query"]["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["conversationId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(session_id)){
return knoxx.backend.http.json_response_BANG_(reply,(400),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"session_id is required"], null));
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
if(cljs.core.truth_(session)){
var conversation_id_SINGLEQUOTE_ = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = conversation_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var runtime_active_QMARK_ = knoxx.backend.routes.app.runtime_processing_session_QMARK_(conversation_id_SINGLEQUOTE_);
var can_send = knoxx.backend.session_store.session_can_send_QMARK_(session);
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session)))?knoxx.backend.routes.app.latest_run_event_BANG_(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session)):Promise.resolve(null)).then((function (latest_event){
var stalled_QMARK_ = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))) && (((cljs.core.not(runtime_active_QMARK_)) && (knoxx.backend.routes.app.stale_running_session_QMARK_(session,latest_event)))));
if(stalled_QMARK_){
knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,session).catch((function (err){
return console.error("On-demand session recovery failed",err);
}));
} else {
}

return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"reason","reason",-2070751759),new cljs.core.Keyword(null,"can_send","can_send",534936371),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"recovery_requested","recovery_requested",-1570831052),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),new cljs.core.Keyword(null,"latest_event_at","latest_event_at",1047694684),new cljs.core.Keyword(null,"model","model",331153215)],[new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(session),((stalled_QMARK_)?"Session looked stalled after restart; recovery requested.":(cljs.core.truth_(runtime_active_QMARK_)?"Session is already processing. Use steer, follow-up, abort, or wait.":new cljs.core.Keyword(null,"reason","reason",-2070751759).cljs$core$IFn$_invoke$arity$1(can_send)
)),((stalled_QMARK_)?false:new cljs.core.Keyword(null,"can-send","can-send",-704220819).cljs$core$IFn$_invoke$arity$1(can_send)),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session),stalled_QMARK_,cljs.core.boolean$((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return runtime_active_QMARK_;
}
})()),new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(latest_event),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(session)]));
}));
} else {
if(cljs.core.truth_(knoxx.backend.routes.app.runtime_processing_session_QMARK_(conversation_id))){
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),true,new cljs.core.Keyword(null,"can_send","can_send",534936371),false,new cljs.core.Keyword(null,"reason","reason",-2070751759),"Session is already processing. Use steer, follow-up, abort, or wait."], null));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"status","status",-1997798413),"not_found",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"can_send","can_send",534936371),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"No session state found. Ready for new turn."], null));
}
}
})).catch((function (err){
console.error("Session status check failed",err);

return knoxx.backend.http.json_response_BANG_(reply,(500),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));

}
});
knoxx.backend.routes.app.health_BANG_ = (function knoxx$backend$routes$app$health_BANG_(app,runtime,config,deps){
var map__70759 = deps;
var map__70759__$1 = cljs.core.__destructure_map(map__70759);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70759__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70760 = app;
var G__70761 = "GET";
var G__70762 = "/health";
var G__70763 = (function (request,reply){
var G__70764 = runtime;
var G__70765 = request;
var G__70766 = reply;
var G__70767 = (function (ctx){
var proxx_configured = (((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))))));
var openplanner_configured = knoxx.backend.http.openplanner_enabled_QMARK_(config);
var proxx_promise = ((proxx_configured)?(function (){var G__70768 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/health");
var G__70769 = ({"headers": (function (){var G__70770 = new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config);
return (bearer_headers.cljs$core$IFn$_invoke$arity$1 ? bearer_headers.cljs$core$IFn$_invoke$arity$1(G__70770) : bearer_headers.call(null,G__70770));
})()});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70768,G__70769) : fetch_json.call(null,G__70768,G__70769));
})():Promise.resolve(({"ok": false, "status": (503), "body": ({"detail": "Proxx is not configured"})})));
var openplanner_promise = ((openplanner_configured)?(function (){var G__70771 = knoxx.backend.http.openplanner_url(config,"/v1/health");
var G__70772 = ({"headers": knoxx.backend.http.openplanner_headers(config)});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70771,G__70772) : fetch_json.call(null,G__70771,G__70772));
})():Promise.resolve(({"ok": false, "status": (503), "body": ({"detail": "OpenPlanner is not configured"})})));
return Promise.all([proxx_promise,openplanner_promise]).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$4(knoxx.backend.routes.app.health_deps_ok,reply,proxx_configured,openplanner_configured)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.health_deps_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70764,G__70765,G__70766,G__70767) : with_request_context_BANG_.call(null,G__70764,G__70765,G__70766,G__70767));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70760,G__70761,G__70762,G__70763) : route_BANG_.call(null,G__70760,G__70761,G__70762,G__70763));
});
knoxx.backend.routes.app.dev_hmr_BANG_ = (function knoxx$backend$routes$app$dev_hmr_BANG_(app,runtime,config,deps){
var map__70773 = deps;
var map__70773__$1 = cljs.core.__destructure_map(map__70773);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70773__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70774 = app;
var G__70775 = "GET";
var G__70776 = "/api/dev/hmr";
var G__70777 = (function (request,reply){
var G__70778 = runtime;
var G__70779 = request;
var G__70780 = reply;
var G__70781 = (function (ctx){
var G__70782 = reply;
var G__70783 = (200);
var G__70784 = knoxx.backend.routes.app.dev_hmr_response();
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__70782,G__70783,G__70784) : json_response_BANG_.call(null,G__70782,G__70783,G__70784));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70778,G__70779,G__70780,G__70781) : with_request_context_BANG_.call(null,G__70778,G__70779,G__70780,G__70781));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70774,G__70775,G__70776,G__70777) : route_BANG_.call(null,G__70774,G__70775,G__70776,G__70777));
});
knoxx.backend.routes.app.config_BANG_ = (function knoxx$backend$routes$app$config_BANG_(app,runtime,config,deps){
var map__70785 = deps;
var map__70785__$1 = cljs.core.__destructure_map(map__70785);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70785__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70786 = app;
var G__70787 = "GET";
var G__70788 = "/api/config";
var G__70789 = (function (request,reply){
var G__70790 = runtime;
var G__70791 = request;
var G__70792 = reply;
var G__70793 = (function (ctx){
var G__70794 = reply;
var G__70795 = (200);
var G__70796 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"tts_provider","tts_provider",1848673731),new cljs.core.Keyword(null,"default_actor_id","default_actor_id",-1072651996),new cljs.core.Keyword(null,"knoxx_enabled","knoxx_enabled",786305029),new cljs.core.Keyword(null,"email_enabled","email_enabled",-1965651127),new cljs.core.Keyword(null,"shibboleth_ui_url","shibboleth_ui_url",-1682504182),new cljs.core.Keyword(null,"default_role","default_role",-536700245),new cljs.core.Keyword(null,"tts_default_speed","tts_default_speed",-1897181429),new cljs.core.Keyword(null,"knoxx_base_url","knoxx_base_url",-804222133),new cljs.core.Keyword(null,"tts_default_postprocess_enabled","tts_default_postprocess_enabled",-353300629),new cljs.core.Keyword(null,"tts_default_voice_id","tts_default_voice_id",138593804),new cljs.core.Keyword(null,"tts_default_model_id","tts_default_model_id",-141776244),new cljs.core.Keyword(null,"shibboleth_enabled","shibboleth_enabled",-1795847955),new cljs.core.Keyword(null,"stt_enabled","stt_enabled",-1918729424),new cljs.core.Keyword(null,"proxx_default_model","proxx_default_model",-1345936300),new cljs.core.Keyword(null,"tts_enabled","tts_enabled",-895456908),new cljs.core.Keyword(null,"proxx_enabled","proxx_enabled",-344912362),new cljs.core.Keyword(null,"default_agent_contract","default_agent_contract",1944367159),new cljs.core.Keyword(null,"stt_base_url","stt_base_url",-364531688),new cljs.core.Keyword(null,"rbac_enabled","rbac_enabled",1179348187),new cljs.core.Keyword(null,"tts_default_postprocess_profile","tts_default_postprocess_profile",497692541),new cljs.core.Keyword(null,"knoxx_admin_url","knoxx_admin_url",457353310)],[(((!(clojure.string.blank_QMARK_(clojure.string.trim((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"voxx-api-key","voxx-api-key",2053708716).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))))?"voxx":""),knoxx.backend.tooling.default_actor_id(config),true,knoxx.backend.tooling.email_enabled_QMARK_(config),((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"shibboleth-ui-url","shibboleth-ui-url",358926658).cljs$core$IFn$_invoke$arity$1(config)))?"":knoxx.backend.http.rewrite_localhost_url(new cljs.core.Keyword(null,"shibboleth-ui-url","shibboleth-ui-url",358926658).cljs$core$IFn$_invoke$arity$1(config),request)),new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"voxx-default-speed","voxx-default-speed",-370827943).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "1.15";
}
})(),knoxx.backend.http.rewrite_localhost_url(new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config),request),true,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"voxx-voice-id","voxx-voice-id",-652120125).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "af_jessica";
}
})(),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"voxx-model-id","voxx-model-id",2106305693).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "kokoro";
}
})(),(((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"shibboleth-base-url","shibboleth-base-url",-351013125).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"shibboleth-ui-url","shibboleth-ui-url",358926658).cljs$core$IFn$_invoke$arity$1(config)))))),(!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"stt-base-url","stt-base-url",-12292445).cljs$core$IFn$_invoke$arity$1(config)))),new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_)),(!(clojure.string.blank_QMARK_(clojure.string.trim((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"voxx-api-key","voxx-api-key",2053708716).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))),(((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)))))),knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,knoxx.backend.tooling.default_actor_id(config)),((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"stt-base-url","stt-base-url",-12292445).cljs$core$IFn$_invoke$arity$1(config)))?"":knoxx.backend.http.rewrite_localhost_url(new cljs.core.Keyword(null,"stt-base-url","stt-base-url",-12292445).cljs$core$IFn$_invoke$arity$1(config),request)),knoxx.backend.authz.policy_db_enabled_QMARK_(runtime),"sports-commentator-v1",knoxx.backend.http.rewrite_localhost_url(new cljs.core.Keyword(null,"knoxx-admin-url","knoxx-admin-url",238625622).cljs$core$IFn$_invoke$arity$1(config),request)]);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__70794,G__70795,G__70796) : json_response_BANG_.call(null,G__70794,G__70795,G__70796));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70790,G__70791,G__70792,G__70793) : with_request_context_BANG_.call(null,G__70790,G__70791,G__70792,G__70793));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70786,G__70787,G__70788,G__70789) : route_BANG_.call(null,G__70786,G__70787,G__70788,G__70789));
});
knoxx.backend.routes.app.api_knoxx_agents_catalog_BANG_ = (function knoxx$backend$routes$app$api_knoxx_agents_catalog_BANG_(app,runtime,config,deps){
var map__70798 = deps;
var map__70798__$1 = cljs.core.__destructure_map(map__70798);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70798__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70799 = app;
var G__70800 = "GET";
var G__70801 = "/api/knoxx/agents/catalog";
var G__70802 = (function (request,reply){
var G__70803 = runtime;
var G__70804 = request;
var G__70805 = reply;
var G__70806 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var actor_id = (function (){var G__70807 = (function (){var or__5142__auto__ = (request["query"]["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (request["query"]["actor"]);
}
})();
var G__70807__$1 = (((G__70807 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70807)));
var G__70807__$2 = (((G__70807__$1 == null))?null:clojure.string.trim(G__70807__$1));
if((G__70807__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70807__$2);
}
})();
var effective_actor_id = (function (){var or__5142__auto__ = actor_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tooling.default_actor_id(config);
}
})();
var agents = knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2(config,effective_actor_id);
var default_agent_id = knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,effective_actor_id);
var default_agent = (cljs.core.truth_(default_agent_id)?knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,default_agent_id,effective_actor_id):null);
var catalog = (function (){var G__70808 = agents;
if(cljs.core.truth_((function (){var and__5140__auto__ = default_agent;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not(cljs.core.some((function (p1__70797_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__70797_SHARP_),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(default_agent));
}),agents));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__70808,default_agent);
} else {
return G__70808;
}
})();
var G__70809 = reply;
var G__70810 = (200);
var G__70811 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),effective_actor_id,new cljs.core.Keyword(null,"actors","actors",-1845636398),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (actor){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(actor),new cljs.core.Keyword(null,"defaultAgent","defaultAgent",-2024015469),new cljs.core.Keyword(null,"default-agent","default-agent",279723152).cljs$core$IFn$_invoke$arity$1(actor),new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null);
}),knoxx.backend.tooling.actor_catalog(config)),new cljs.core.Keyword(null,"agents","agents",-1112413700),cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),catalog)),new cljs.core.Keyword(null,"default_actor_id","default_actor_id",-1072651996),knoxx.backend.tooling.default_actor_id(config),new cljs.core.Keyword(null,"default_agent_contract","default_agent_contract",1944367159),default_agent_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__70809,G__70810,G__70811) : json_response_BANG_.call(null,G__70809,G__70810,G__70811));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70803,G__70804,G__70805,G__70806) : with_request_context_BANG_.call(null,G__70803,G__70804,G__70805,G__70806));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70799,G__70800,G__70801,G__70802) : route_BANG_.call(null,G__70799,G__70800,G__70801,G__70802));
});
knoxx.backend.routes.app.api_auth_context_BANG_ = (function knoxx$backend$routes$app$api_auth_context_BANG_(app,runtime,config,deps){
var map__70812 = deps;
var map__70812__$1 = cljs.core.__destructure_map(map__70812);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70812__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70813 = app;
var G__70814 = "GET";
var G__70815 = "/api/auth/context";
var G__70816 = (function (request,reply){
var G__70817 = runtime;
var G__70818 = request;
var G__70819 = reply;
var G__70820 = (function (ctx){
if((!(knoxx.backend.authz.policy_db_enabled_QMARK_(runtime)))){
var G__70821 = reply;
var G__70822 = (503);
var G__70823 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__70821,G__70822,G__70823) : json_response_BANG_.call(null,G__70821,G__70822,G__70823));
} else {
var G__70824 = reply;
var G__70825 = (200);
var G__70826 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"isSystemAdmin","isSystemAdmin",679314438),new cljs.core.Keyword(null,"roles","roles",143379530),new cljs.core.Keyword(null,"membership","membership",254556333),new cljs.core.Keyword(null,"membershipToolPolicies","membershipToolPolicies",-954353456),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"org","org",1495985),new cljs.core.Keyword(null,"primaryRole","primaryRole",-1016391334),new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),new cljs.core.Keyword(null,"actor","actor",-1830560481)],[cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"permissions","permissions",67803075).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),cljs.core.boolean$(new cljs.core.Keyword(null,"isSystemAdmin","isSystemAdmin",679314438).cljs$core$IFn$_invoke$arity$1(ctx)),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"roles","roles",143379530).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"membership","membership",254556333).cljs$core$IFn$_invoke$arity$1(ctx),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"membershipToolPolicies","membershipToolPolicies",-954353456).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(ctx),knoxx.backend.authz.primary_context_role(ctx),new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(ctx),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"actor","actor",-1830560481).cljs$core$IFn$_invoke$arity$1(ctx)]);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__70824,G__70825,G__70826) : json_response_BANG_.call(null,G__70824,G__70825,G__70826));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70817,G__70818,G__70819,G__70820) : with_request_context_BANG_.call(null,G__70817,G__70818,G__70819,G__70820));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70813,G__70814,G__70815,G__70816) : route_BANG_.call(null,G__70813,G__70814,G__70815,G__70816));
});
knoxx.backend.routes.app.api_knoxx_proxy_get_BANG_ = (function knoxx$backend$routes$app$api_knoxx_proxy_get_BANG_(app,runtime,config,deps){
var map__70827 = deps;
var map__70827__$1 = cljs.core.__destructure_map(map__70827);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70827__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70828 = app;
var G__70829 = "GET";
var G__70830 = "/api/knoxx/proxy/*";
var G__70831 = (function (request,reply){
var G__70832 = runtime;
var G__70833 = request;
var G__70834 = reply;
var G__70835 = (function (ctx){
var path = (request["params"]["*"]);
return knoxx.backend.agent_runtime.forward_knoxx_request_BANG_(config,request,"GET",path,null).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.proxy_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.proxy_err,reply,"Proxy request failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70832,G__70833,G__70834,G__70835) : with_request_context_BANG_.call(null,G__70832,G__70833,G__70834,G__70835));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70828,G__70829,G__70830,G__70831) : route_BANG_.call(null,G__70828,G__70829,G__70830,G__70831));
});
knoxx.backend.routes.app.api_knoxx_proxy_post_BANG_ = (function knoxx$backend$routes$app$api_knoxx_proxy_post_BANG_(app,runtime,config,deps){
var map__70836 = deps;
var map__70836__$1 = cljs.core.__destructure_map(map__70836);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70836__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70837 = app;
var G__70838 = "POST";
var G__70839 = "/api/knoxx/proxy/*";
var G__70840 = (function (request,reply){
var G__70841 = runtime;
var G__70842 = request;
var G__70843 = reply;
var G__70844 = (function (ctx){
var path = (request["params"]["*"]);
return knoxx.backend.agent_runtime.forward_knoxx_request_BANG_(config,request,"POST",path,null).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.proxy_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.proxy_err,reply,"Proxy request failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70841,G__70842,G__70843,G__70844) : with_request_context_BANG_.call(null,G__70841,G__70842,G__70843,G__70844));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70837,G__70838,G__70839,G__70840) : route_BANG_.call(null,G__70837,G__70838,G__70839,G__70840));
});
knoxx.backend.routes.app.api_knoxx_proxy_put_BANG_ = (function knoxx$backend$routes$app$api_knoxx_proxy_put_BANG_(app,runtime,config,deps){
var map__70845 = deps;
var map__70845__$1 = cljs.core.__destructure_map(map__70845);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70845__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70846 = app;
var G__70847 = "PUT";
var G__70848 = "/api/knoxx/proxy/*";
var G__70849 = (function (request,reply){
var G__70850 = runtime;
var G__70851 = request;
var G__70852 = reply;
var G__70853 = (function (ctx){
var path = (request["params"]["*"]);
return knoxx.backend.agent_runtime.forward_knoxx_request_BANG_(config,request,"PUT",path,null).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.proxy_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.proxy_err,reply,"Proxy request failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70850,G__70851,G__70852,G__70853) : with_request_context_BANG_.call(null,G__70850,G__70851,G__70852,G__70853));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70846,G__70847,G__70848,G__70849) : route_BANG_.call(null,G__70846,G__70847,G__70848,G__70849));
});
knoxx.backend.routes.app.api_knoxx_proxy_patch_BANG_ = (function knoxx$backend$routes$app$api_knoxx_proxy_patch_BANG_(app,runtime,config,deps){
var map__70854 = deps;
var map__70854__$1 = cljs.core.__destructure_map(map__70854);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70854__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70855 = app;
var G__70856 = "PATCH";
var G__70857 = "/api/knoxx/proxy/*";
var G__70858 = (function (request,reply){
var G__70859 = runtime;
var G__70860 = request;
var G__70861 = reply;
var G__70862 = (function (ctx){
var path = (request["params"]["*"]);
return knoxx.backend.agent_runtime.forward_knoxx_request_BANG_(config,request,"PATCH",path,null).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.proxy_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.proxy_err,reply,"Proxy request failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70859,G__70860,G__70861,G__70862) : with_request_context_BANG_.call(null,G__70859,G__70860,G__70861,G__70862));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70855,G__70856,G__70857,G__70858) : route_BANG_.call(null,G__70855,G__70856,G__70857,G__70858));
});
knoxx.backend.routes.app.api_knoxx_proxy_delete_BANG_ = (function knoxx$backend$routes$app$api_knoxx_proxy_delete_BANG_(app,runtime,config,deps){
var map__70863 = deps;
var map__70863__$1 = cljs.core.__destructure_map(map__70863);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70863__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70864 = app;
var G__70865 = "DELETE";
var G__70866 = "/api/knoxx/proxy/*";
var G__70867 = (function (request,reply){
var G__70868 = runtime;
var G__70869 = request;
var G__70870 = reply;
var G__70871 = (function (ctx){
var path = (request["params"]["*"]);
return knoxx.backend.agent_runtime.forward_knoxx_request_BANG_(config,request,"DELETE",path,null).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.proxy_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.proxy_err,reply,"Proxy request failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70868,G__70869,G__70870,G__70871) : with_request_context_BANG_.call(null,G__70868,G__70869,G__70870,G__70871));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70864,G__70865,G__70866,G__70867) : route_BANG_.call(null,G__70864,G__70865,G__70866,G__70867));
});
knoxx.backend.routes.app.api_ingestion_browse_BANG_ = (function knoxx$backend$routes$app$api_ingestion_browse_BANG_(app,runtime,config,deps){
var map__70872 = deps;
var map__70872__$1 = cljs.core.__destructure_map(map__70872);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70872__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70873 = app;
var G__70874 = "GET";
var G__70875 = "/api/ingestion/browse";
var G__70876 = (function (request,reply){
var G__70877 = runtime;
var G__70878 = request;
var G__70879 = reply;
var G__70880 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var qs = (request_query_string.cljs$core$IFn$_invoke$arity$1 ? request_query_string.cljs$core$IFn$_invoke$arity$1(request) : request_query_string.call(null,request));
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/browse"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qs));
return (function (){var G__70881 = target_url;
var G__70882 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70881,G__70882) : fetch_json.call(null,G__70881,G__70882));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70877,G__70878,G__70879,G__70880) : with_request_context_BANG_.call(null,G__70877,G__70878,G__70879,G__70880));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70873,G__70874,G__70875,G__70876) : route_BANG_.call(null,G__70873,G__70874,G__70875,G__70876));
});
knoxx.backend.routes.app.api_ingestion_file_BANG_ = (function knoxx$backend$routes$app$api_ingestion_file_BANG_(app,runtime,config,deps){
var map__70883 = deps;
var map__70883__$1 = cljs.core.__destructure_map(map__70883);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70883__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70884 = app;
var G__70885 = "GET";
var G__70886 = "/api/ingestion/file";
var G__70887 = (function (request,reply){
var G__70888 = runtime;
var G__70889 = request;
var G__70890 = reply;
var G__70891 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var qs = (request_query_string.cljs$core$IFn$_invoke$arity$1 ? request_query_string.cljs$core$IFn$_invoke$arity$1(request) : request_query_string.call(null,request));
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/file"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qs));
return (function (){var G__70892 = target_url;
var G__70893 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70892,G__70893) : fetch_json.call(null,G__70892,G__70893));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70888,G__70889,G__70890,G__70891) : with_request_context_BANG_.call(null,G__70888,G__70889,G__70890,G__70891));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70884,G__70885,G__70886,G__70887) : route_BANG_.call(null,G__70884,G__70885,G__70886,G__70887));
});
knoxx.backend.routes.app.api_ingestion_sources_BANG_ = (function knoxx$backend$routes$app$api_ingestion_sources_BANG_(app,runtime,config,deps){
var map__70894 = deps;
var map__70894__$1 = cljs.core.__destructure_map(map__70894);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70894__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70895 = app;
var G__70896 = "GET";
var G__70897 = "/api/ingestion/sources";
var G__70898 = (function (request,reply){
var G__70899 = runtime;
var G__70900 = request;
var G__70901 = reply;
var G__70902 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
return (function (){var G__70903 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/sources");
var G__70904 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70903,G__70904) : fetch_json.call(null,G__70903,G__70904));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70899,G__70900,G__70901,G__70902) : with_request_context_BANG_.call(null,G__70899,G__70900,G__70901,G__70902));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70895,G__70896,G__70897,G__70898) : route_BANG_.call(null,G__70895,G__70896,G__70897,G__70898));
});
knoxx.backend.routes.app.api_ingestion_jobs_get_BANG_ = (function knoxx$backend$routes$app$api_ingestion_jobs_get_BANG_(app,runtime,config,deps){
var map__70905 = deps;
var map__70905__$1 = cljs.core.__destructure_map(map__70905);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70905__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70906 = app;
var G__70907 = "GET";
var G__70908 = "/api/ingestion/jobs";
var G__70909 = (function (request,reply){
var G__70910 = runtime;
var G__70911 = request;
var G__70912 = reply;
var G__70913 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var qs = (request_query_string.cljs$core$IFn$_invoke$arity$1 ? request_query_string.cljs$core$IFn$_invoke$arity$1(request) : request_query_string.call(null,request));
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/jobs"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qs));
return (function (){var G__70914 = target_url;
var G__70915 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70914,G__70915) : fetch_json.call(null,G__70914,G__70915));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70910,G__70911,G__70912,G__70913) : with_request_context_BANG_.call(null,G__70910,G__70911,G__70912,G__70913));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70906,G__70907,G__70908,G__70909) : route_BANG_.call(null,G__70906,G__70907,G__70908,G__70909));
});
knoxx.backend.routes.app.api_ingestion_jobs_post_BANG_ = (function knoxx$backend$routes$app$api_ingestion_jobs_post_BANG_(app,runtime,config,deps){
var map__70916 = deps;
var map__70916__$1 = cljs.core.__destructure_map(map__70916);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70916__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70917 = app;
var G__70918 = "POST";
var G__70919 = "/api/ingestion/jobs";
var G__70920 = (function (request,reply){
var G__70921 = runtime;
var G__70922 = request;
var G__70923 = reply;
var G__70924 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var body = (request["body"]);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/jobs");
return (function (){var G__70925 = target_url;
var G__70926 = ({"method": "POST", "headers": ({"Content-Type": "application/json"}), "body": JSON.stringify((function (){var or__5142__auto__ = body;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})())});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70925,G__70926) : fetch_json.call(null,G__70925,G__70926));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70921,G__70922,G__70923,G__70924) : with_request_context_BANG_.call(null,G__70921,G__70922,G__70923,G__70924));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70917,G__70918,G__70919,G__70920) : route_BANG_.call(null,G__70917,G__70918,G__70919,G__70920));
});
knoxx.backend.routes.app.api_ingestion_proxy_get_BANG_ = (function knoxx$backend$routes$app$api_ingestion_proxy_get_BANG_(app,runtime,config,deps){
var map__70927 = deps;
var map__70927__$1 = cljs.core.__destructure_map(map__70927);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70927__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70928 = app;
var G__70929 = "GET";
var G__70930 = "/api/ingestion-proxy/*";
var G__70931 = (function (request,reply){
var G__70932 = runtime;
var G__70933 = request;
var G__70934 = reply;
var G__70935 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var path = (request["params"]["*"]);
var qs = (request_query_string.cljs$core$IFn$_invoke$arity$1 ? request_query_string.cljs$core$IFn$_invoke$arity$1(request) : request_query_string.call(null,request));
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qs));
return (function (){var G__70936 = target_url;
var G__70937 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70936,G__70937) : fetch_json.call(null,G__70936,G__70937));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.fetch_json_err_detail,reply,"Ingestion proxy failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70932,G__70933,G__70934,G__70935) : with_request_context_BANG_.call(null,G__70932,G__70933,G__70934,G__70935));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70928,G__70929,G__70930,G__70931) : route_BANG_.call(null,G__70928,G__70929,G__70930,G__70931));
});
knoxx.backend.routes.app.api_ingestion_proxy_post_BANG_ = (function knoxx$backend$routes$app$api_ingestion_proxy_post_BANG_(app,runtime,config,deps){
var map__70938 = deps;
var map__70938__$1 = cljs.core.__destructure_map(map__70938);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70938__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70939 = app;
var G__70940 = "POST";
var G__70941 = "/api/ingestion-proxy/*";
var G__70942 = (function (request,reply){
var G__70943 = runtime;
var G__70944 = request;
var G__70945 = reply;
var G__70946 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var path = (request["params"]["*"]);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
var body = (request["body"]);
return (function (){var G__70947 = target_url;
var G__70948 = ({"method": "POST", "headers": ({"Content-Type": "application/json"}), "body": JSON.stringify((function (){var or__5142__auto__ = body;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})())});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70947,G__70948) : fetch_json.call(null,G__70947,G__70948));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.fetch_json_err_detail,reply,"Ingestion proxy failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70943,G__70944,G__70945,G__70946) : with_request_context_BANG_.call(null,G__70943,G__70944,G__70945,G__70946));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70939,G__70940,G__70941,G__70942) : route_BANG_.call(null,G__70939,G__70940,G__70941,G__70942));
});
knoxx.backend.routes.app.api_ingestion_proxy_delete_BANG_ = (function knoxx$backend$routes$app$api_ingestion_proxy_delete_BANG_(app,runtime,config,deps){
var map__70949 = deps;
var map__70949__$1 = cljs.core.__destructure_map(map__70949);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70949__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70950 = app;
var G__70951 = "DELETE";
var G__70952 = "/api/ingestion-proxy/*";
var G__70953 = (function (request,reply){
var G__70954 = runtime;
var G__70955 = request;
var G__70956 = reply;
var G__70957 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var path = (request["params"]["*"]);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
return (function (){var G__70958 = target_url;
var G__70959 = ({"method": "DELETE"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70958,G__70959) : fetch_json.call(null,G__70958,G__70959));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.fetch_json_err_detail,reply,"Ingestion proxy failed: "));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70954,G__70955,G__70956,G__70957) : with_request_context_BANG_.call(null,G__70954,G__70955,G__70956,G__70957));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70950,G__70951,G__70952,G__70953) : route_BANG_.call(null,G__70950,G__70951,G__70952,G__70953));
});
knoxx.backend.routes.app.api_data_op_get_BANG_ = (function knoxx$backend$routes$app$api_data_op_get_BANG_(app,runtime,config,deps){
var map__70960 = deps;
var map__70960__$1 = cljs.core.__destructure_map(map__70960);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70960__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70961 = app;
var G__70962 = "GET";
var G__70963 = "/api/data/op/*";
var G__70964 = (function (request,reply){
var G__70965 = runtime;
var G__70966 = request;
var G__70967 = reply;
var G__70968 = (function (ctx){
var path = (request["params"]["*"]);
var raw_url = (request["raw"]["url"]);
var query_idx = raw_url.indexOf("?");
var qs = (((query_idx >= (0)))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(raw_url,query_idx):"");
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__70969 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(qs));
var G__70970 = ({"headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id})});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70969,G__70970) : fetch_json.call(null,G__70969,G__70970));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70965,G__70966,G__70967,G__70968) : with_request_context_BANG_.call(null,G__70965,G__70966,G__70967,G__70968));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70961,G__70962,G__70963,G__70964) : route_BANG_.call(null,G__70961,G__70962,G__70963,G__70964));
});
knoxx.backend.routes.app.api_data_op_post_BANG_ = (function knoxx$backend$routes$app$api_data_op_post_BANG_(app,runtime,config,deps){
var map__70971 = deps;
var map__70971__$1 = cljs.core.__destructure_map(map__70971);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70971__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70972 = app;
var G__70973 = "POST";
var G__70974 = "/api/data/op/*";
var G__70975 = (function (request,reply){
var G__70976 = runtime;
var G__70977 = request;
var G__70978 = reply;
var G__70979 = (function (ctx){
var path = (request["params"]["*"]);
var body = (request["body"]);
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__70980 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
var G__70981 = ({"method": "POST", "headers": ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id}), "body": JSON.stringify((function (){var or__5142__auto__ = body;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})())});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70980,G__70981) : fetch_json.call(null,G__70980,G__70981));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70976,G__70977,G__70978,G__70979) : with_request_context_BANG_.call(null,G__70976,G__70977,G__70978,G__70979));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70972,G__70973,G__70974,G__70975) : route_BANG_.call(null,G__70972,G__70973,G__70974,G__70975));
});
knoxx.backend.routes.app.api_data_op_delete_BANG_ = (function knoxx$backend$routes$app$api_data_op_delete_BANG_(app,runtime,config,deps){
var map__70982 = deps;
var map__70982__$1 = cljs.core.__destructure_map(map__70982);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70982__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70983 = app;
var G__70984 = "DELETE";
var G__70985 = "/api/data/op/*";
var G__70986 = (function (request,reply){
var G__70987 = runtime;
var G__70988 = request;
var G__70989 = reply;
var G__70990 = (function (ctx){
var path = (request["params"]["*"]);
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__70991 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
var G__70992 = ({"method": "DELETE", "headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id})});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__70991,G__70992) : fetch_json.call(null,G__70991,G__70992));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70987,G__70988,G__70989,G__70990) : with_request_context_BANG_.call(null,G__70987,G__70988,G__70989,G__70990));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70983,G__70984,G__70985,G__70986) : route_BANG_.call(null,G__70983,G__70984,G__70985,G__70986));
});
knoxx.backend.routes.app.api_data_op_patch_BANG_ = (function knoxx$backend$routes$app$api_data_op_patch_BANG_(app,runtime,config,deps){
var map__70993 = deps;
var map__70993__$1 = cljs.core.__destructure_map(map__70993);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70993__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__70994 = app;
var G__70995 = "PATCH";
var G__70996 = "/api/data/op/*";
var G__70997 = (function (request,reply){
var G__70998 = runtime;
var G__70999 = request;
var G__71000 = reply;
var G__71001 = (function (ctx){
var path = (request["params"]["*"]);
var body = (request["body"]);
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__71002 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
var G__71003 = ({"method": "PATCH", "headers": ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id}), "body": JSON.stringify((function (){var or__5142__auto__ = body;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})())});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71002,G__71003) : fetch_json.call(null,G__71002,G__71003));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__70998,G__70999,G__71000,G__71001) : with_request_context_BANG_.call(null,G__70998,G__70999,G__71000,G__71001));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__70994,G__70995,G__70996,G__70997) : route_BANG_.call(null,G__70994,G__70995,G__70996,G__70997));
});
knoxx.backend.routes.app.api_data_health_BANG_ = (function knoxx$backend$routes$app$api_data_health_BANG_(app,runtime,config,deps){
var map__71004 = deps;
var map__71004__$1 = cljs.core.__destructure_map(map__71004);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71004__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71005 = app;
var G__71006 = "GET";
var G__71007 = "/api/data/health";
var G__71008 = (function (request,reply){
var G__71009 = runtime;
var G__71010 = request;
var G__71011 = reply;
var G__71012 = (function (ctx){
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var proxx_base = new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config);
var proxx_key = new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config);
var check = (function (url,headers){
return (function (){var G__71013 = url;
var G__71014 = ({"headers": (function (){var or__5142__auto__ = headers;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(), "method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71013,G__71014) : fetch_json.call(null,G__71013,G__71014));
})().then((function (resp){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),(resp["ok"]),new cljs.core.Keyword(null,"status","status",-1997798413),(resp["status"]),new cljs.core.Keyword(null,"url","url",276297046),url,new cljs.core.Keyword(null,"detail","detail",-1545345025),(resp["body"])], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),err.message,new cljs.core.Keyword(null,"url","url",276297046),url], null);
}));
});
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [check((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/health"),({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})()})),check((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(proxx_base)+"/health"),({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(proxx_key))})),check((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/health"),null),check("http://127.0.0.1:8796/api/status",null),check("http://127.0.0.1:3777/health",null),check("http://127.0.0.1:8787/v1/health",null),check("http://127.0.0.1:8786/health",null),check("http://127.0.0.1:8801/health",null)], null))).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.data_health_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.data_health_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71009,G__71010,G__71011,G__71012) : with_request_context_BANG_.call(null,G__71009,G__71010,G__71011,G__71012));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71005,G__71006,G__71007,G__71008) : route_BANG_.call(null,G__71005,G__71006,G__71007,G__71008));
});
knoxx.backend.routes.app.api_data_mongo_collections_BANG_ = (function knoxx$backend$routes$app$api_data_mongo_collections_BANG_(app,runtime,config,deps){
var map__71015 = deps;
var map__71015__$1 = cljs.core.__destructure_map(map__71015);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71015__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71016 = app;
var G__71017 = "GET";
var G__71018 = "/api/data/mongo/collections";
var G__71019 = (function (request,reply){
var G__71020 = runtime;
var G__71021 = request;
var G__71022 = reply;
var G__71023 = (function (ctx){
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var G__71024 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/documents/stats");
var G__71025 = ({"headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id})});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71024,G__71025) : fetch_json.call(null,G__71024,G__71025));
})(),(function (){var G__71026 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/graph/monitoring");
var G__71027 = ({"headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id})});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71026,G__71027) : fetch_json.call(null,G__71026,G__71027));
})()], null))).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.mongo_collections_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71020,G__71021,G__71022,G__71023) : with_request_context_BANG_.call(null,G__71020,G__71021,G__71022,G__71023));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71016,G__71017,G__71018,G__71019) : route_BANG_.call(null,G__71016,G__71017,G__71018,G__71019));
});
knoxx.backend.routes.app.api_data_mongo_list_BANG_ = (function knoxx$backend$routes$app$api_data_mongo_list_BANG_(app,runtime,config,deps){
var map__71028 = deps;
var map__71028__$1 = cljs.core.__destructure_map(map__71028);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71028__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71029 = app;
var G__71030 = "GET";
var G__71031 = "/api/data/mongo/list";
var G__71032 = (function (request,reply){
var G__71033 = runtime;
var G__71034 = request;
var G__71035 = reply;
var G__71036 = (function (ctx){
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__71037 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/mongo/collections");
var G__71038 = ({"headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id})});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71037,G__71038) : fetch_json.call(null,G__71037,G__71038));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71033,G__71034,G__71035,G__71036) : with_request_context_BANG_.call(null,G__71033,G__71034,G__71035,G__71036));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71029,G__71030,G__71031,G__71032) : route_BANG_.call(null,G__71029,G__71030,G__71031,G__71032));
});
knoxx.backend.routes.app.api_data_mongo_query_BANG_ = (function knoxx$backend$routes$app$api_data_mongo_query_BANG_(app,runtime,config,deps){
var map__71039 = deps;
var map__71039__$1 = cljs.core.__destructure_map(map__71039);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71039__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71040 = app;
var G__71041 = "POST";
var G__71042 = "/api/data/mongo/query";
var G__71043 = (function (request,reply){
var G__71044 = runtime;
var G__71045 = request;
var G__71046 = reply;
var G__71047 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__71048 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/mongo/query");
var G__71049 = ({"method": "POST", "headers": ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id}), "body": JSON.stringify(body)});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71048,G__71049) : fetch_json.call(null,G__71048,G__71049));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71044,G__71045,G__71046,G__71047) : with_request_context_BANG_.call(null,G__71044,G__71045,G__71046,G__71047));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71040,G__71041,G__71042,G__71043) : route_BANG_.call(null,G__71040,G__71041,G__71042,G__71043));
});
knoxx.backend.routes.app.api_data_pg_tables_BANG_ = (function knoxx$backend$routes$app$api_data_pg_tables_BANG_(app,runtime,config,deps){
var map__71050 = deps;
var map__71050__$1 = cljs.core.__destructure_map(map__71050);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71050__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71051 = app;
var G__71052 = "GET";
var G__71053 = "/api/data/pg/tables";
var G__71054 = (function (request,reply){
var G__71055 = runtime;
var G__71056 = request;
var G__71057 = reply;
var G__71058 = (function (ctx){
var G__71059 = reply;
var G__71060 = (200);
var G__71061 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"tables","tables",1334623052),new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, ["ingestion_sources","ingestion_jobs","ingestion_file_state","orgs","users","roles","memberships","data_lakes","sessions","audit_events","permissions"], null)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71059,G__71060,G__71061) : json_response_BANG_.call(null,G__71059,G__71060,G__71061));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71055,G__71056,G__71057,G__71058) : with_request_context_BANG_.call(null,G__71055,G__71056,G__71057,G__71058));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71051,G__71052,G__71053,G__71054) : route_BANG_.call(null,G__71051,G__71052,G__71053,G__71054));
});
knoxx.backend.routes.app.api_data_jobs_build_semantic_edges_BANG_ = (function knoxx$backend$routes$app$api_data_jobs_build_semantic_edges_BANG_(app,runtime,config,deps){
var map__71062 = deps;
var map__71062__$1 = cljs.core.__destructure_map(map__71062);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71062__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71063 = app;
var G__71064 = "POST";
var G__71065 = "/api/data/jobs/build-semantic-edges";
var G__71066 = (function (request,reply){
var G__71067 = runtime;
var G__71068 = request;
var G__71069 = reply;
var G__71070 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var k = (function (){var or__5142__auto__ = (body["k"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8);
}
})();
var min_sim = (function (){var or__5142__auto__ = (body["minSimilarity"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return 0.3;
}
})();
var op_base = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
var op_key = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
var tenant_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
return (function (){var G__71071 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_base)+"/v1/jobs/build-semantic-edges");
var G__71072 = ({"method": "POST", "headers": ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(op_key)), "X-Tenant-ID": tenant_id}), "body": JSON.stringify(({"k": k, "minSimilarity": min_sim}))});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71071,G__71072) : fetch_json.call(null,G__71071,G__71072));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71067,G__71068,G__71069,G__71070) : with_request_context_BANG_.call(null,G__71067,G__71068,G__71069,G__71070));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71063,G__71064,G__71065,G__71066) : route_BANG_.call(null,G__71063,G__71064,G__71065,G__71066));
});
knoxx.backend.routes.app.api_data_pg_query_BANG_ = (function knoxx$backend$routes$app$api_data_pg_query_BANG_(app,runtime,config,deps){
var map__71073 = deps;
var map__71073__$1 = cljs.core.__destructure_map(map__71073);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71073__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71074 = app;
var G__71075 = "POST";
var G__71076 = "/api/data/pg/query";
var G__71077 = (function (request,reply){
var G__71078 = runtime;
var G__71079 = request;
var G__71080 = reply;
var G__71081 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var raw_sql = (function (){var or__5142__auto__ = (body["sql"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var table = (function (){var or__5142__auto__ = (body["table"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var limit = (function (){var or__5142__auto__ = (body["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})();
var db = knoxx.backend.authz.policy_db(runtime);
var query_fn = (cljs.core.truth_(db)?(db["query"]):null);
if((db == null)){
var G__71082 = reply;
var G__71083 = (503);
var G__71084 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Policy database not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71082,G__71083,G__71084) : json_response_BANG_.call(null,G__71082,G__71083,G__71084));
} else {
if((!(clojure.string.blank_QMARK_(raw_sql)))){
var trimmed = clojure.string.trim(raw_sql);
if((!(clojure.string.starts_with_QMARK_(clojure.string.upper_case(trimmed),"SELECT")))){
var G__71085 = reply;
var G__71086 = (400);
var G__71087 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Only SELECT queries are allowed"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71085,G__71086,G__71087) : json_response_BANG_.call(null,G__71085,G__71086,G__71087));
} else {
var enforced_limit = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2(parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(limit)),(10)),(1)),(500));
var has_limit = cljs.core.re_find(/\bLIMIT\b/i,trimmed);
var final_sql = (cljs.core.truth_(has_limit)?trimmed:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(trimmed)+" LIMIT "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(enforced_limit)));
return (function (){var G__71088 = final_sql;
var G__71089 = [];
return (query_fn.cljs$core$IFn$_invoke$arity$2 ? query_fn.cljs$core$IFn$_invoke$arity$2(G__71088,G__71089) : query_fn.call(null,G__71088,G__71089));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.pg_query_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.pg_query_err,reply));
}
} else {
if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.blank_QMARK_(table);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return cljs.core.re_find(/[^a-zA-Z0-9_]/,table);
}
})())){
var G__71090 = reply;
var G__71091 = (400);
var G__71092 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Invalid table name"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71090,G__71091,G__71092) : json_response_BANG_.call(null,G__71090,G__71091,G__71092));
} else {
var enforced_limit = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2(parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(limit)),(10)),(1)),(500));
var sql_str = (""+"SELECT * FROM "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(table)+" LIMIT "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(enforced_limit));
return (function (){var G__71093 = sql_str;
var G__71094 = [];
return (query_fn.cljs$core$IFn$_invoke$arity$2 ? query_fn.cljs$core$IFn$_invoke$arity$2(G__71093,G__71094) : query_fn.call(null,G__71093,G__71094));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.pg_query_table_ok,reply,table)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.pg_query_err,reply));

}
}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71078,G__71079,G__71080,G__71081) : with_request_context_BANG_.call(null,G__71078,G__71079,G__71080,G__71081));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71074,G__71075,G__71076,G__71077) : route_BANG_.call(null,G__71074,G__71075,G__71076,G__71077));
});
knoxx.backend.routes.app.api_data_browse_BANG_ = (function knoxx$backend$routes$app$api_data_browse_BANG_(app,runtime,config,deps){
var map__71095 = deps;
var map__71095__$1 = cljs.core.__destructure_map(map__71095);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71095__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71096 = app;
var G__71097 = "GET";
var G__71098 = "/api/data/browse";
var G__71099 = (function (request,reply){
var G__71100 = runtime;
var G__71101 = request;
var G__71102 = reply;
var G__71103 = (function (ctx){
var qs = (request["query"]);
var path = (function (){var or__5142__auto__ = (qs["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/browse"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(path))?"":(""+"?path="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(path))))));
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(target_url,null) : fetch_json.call(null,target_url,null)).then((function (resp){
var G__71104 = reply;
var G__71105 = (function (){var or__5142__auto__ = (resp["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (200);
}
})();
var G__71106 = (resp["body"]);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71104,G__71105,G__71106) : json_response_BANG_.call(null,G__71104,G__71105,G__71106));
})).catch((function (err){
var G__71107 = reply;
var G__71108 = (502);
var G__71109 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),err.message], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71107,G__71108,G__71109) : json_response_BANG_.call(null,G__71107,G__71108,G__71109));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71100,G__71101,G__71102,G__71103) : with_request_context_BANG_.call(null,G__71100,G__71101,G__71102,G__71103));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71096,G__71097,G__71098,G__71099) : route_BANG_.call(null,G__71096,G__71097,G__71098,G__71099));
});
knoxx.backend.routes.app.api_data_file_BANG_ = (function knoxx$backend$routes$app$api_data_file_BANG_(app,runtime,config,deps){
var map__71110 = deps;
var map__71110__$1 = cljs.core.__destructure_map(map__71110);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71110__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71111 = app;
var G__71112 = "GET";
var G__71113 = "/api/data/file";
var G__71114 = (function (request,reply){
var G__71115 = runtime;
var G__71116 = request;
var G__71117 = reply;
var G__71118 = (function (ctx){
var qs = (request["query"]);
var path = (function (){var or__5142__auto__ = (qs["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var ingestion_base = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
return (function (){var G__71119 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ingestion_base)+"/api/ingestion/file?path="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(path)));
var G__71120 = null;
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71119,G__71120) : fetch_json.call(null,G__71119,G__71120));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71115,G__71116,G__71117,G__71118) : with_request_context_BANG_.call(null,G__71115,G__71116,G__71117,G__71118));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71111,G__71112,G__71113,G__71114) : route_BANG_.call(null,G__71111,G__71112,G__71113,G__71114));
});
knoxx.backend.routes.app.api_data_graphql_BANG_ = (function knoxx$backend$routes$app$api_data_graphql_BANG_(app,runtime,config,deps){
var map__71121 = deps;
var map__71121__$1 = cljs.core.__destructure_map(map__71121);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71121__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71122 = app;
var G__71123 = "POST";
var G__71124 = "/api/data/graphql";
var G__71125 = (function (request,reply){
var G__71126 = runtime;
var G__71127 = request;
var G__71128 = reply;
var G__71129 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var gw_url = "http://127.0.0.1:8796/graphql";
return (function (){var G__71130 = gw_url;
var G__71131 = ({"method": "POST", "headers": ({"Content-Type": "application/json"}), "body": JSON.stringify(body)});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71130,G__71131) : fetch_json.call(null,G__71130,G__71131));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71126,G__71127,G__71128,G__71129) : with_request_context_BANG_.call(null,G__71126,G__71127,G__71128,G__71129));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71122,G__71123,G__71124,G__71125) : route_BANG_.call(null,G__71122,G__71123,G__71124,G__71125));
});
knoxx.backend.routes.app.api_data_graph_status_BANG_ = (function knoxx$backend$routes$app$api_data_graph_status_BANG_(app,runtime,config,deps){
var map__71132 = deps;
var map__71132__$1 = cljs.core.__destructure_map(map__71132);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71132__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71133 = app;
var G__71134 = "GET";
var G__71135 = "/api/data/graph/status";
var G__71136 = (function (request,reply){
var G__71137 = runtime;
var G__71138 = request;
var G__71139 = reply;
var G__71140 = (function (ctx){
return (function (){var G__71141 = "http://127.0.0.1:8796/api/status";
var G__71142 = ({"method": "GET"});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71141,G__71142) : fetch_json.call(null,G__71141,G__71142));
})().then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.fetch_json_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71137,G__71138,G__71139,G__71140) : with_request_context_BANG_.call(null,G__71137,G__71138,G__71139,G__71140));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71133,G__71134,G__71135,G__71136) : route_BANG_.call(null,G__71133,G__71134,G__71135,G__71136));
});
knoxx.backend.routes.app.api_data_graph_view_url_BANG_ = (function knoxx$backend$routes$app$api_data_graph_view_url_BANG_(app,runtime,config,deps){
var map__71143 = deps;
var map__71143__$1 = cljs.core.__destructure_map(map__71143);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71143__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71144 = app;
var G__71145 = "GET";
var G__71146 = "/api/data/graph/view-url";
var G__71147 = (function (request,reply){
var G__71148 = runtime;
var G__71149 = request;
var G__71150 = reply;
var G__71151 = (function (ctx){
var G__71152 = reply;
var G__71153 = (200);
var G__71154 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"url","url",276297046),"http://127.0.0.1:8796"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71152,G__71153,G__71154) : json_response_BANG_.call(null,G__71152,G__71153,G__71154));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71148,G__71149,G__71150,G__71151) : with_request_context_BANG_.call(null,G__71148,G__71149,G__71150,G__71151));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71144,G__71145,G__71146,G__71147) : route_BANG_.call(null,G__71144,G__71145,G__71146,G__71147));
});
knoxx.backend.routes.app.api_knoxx_health_BANG_ = (function knoxx$backend$routes$app$api_knoxx_health_BANG_(app,runtime,config,deps){
var map__71155 = deps;
var map__71155__$1 = cljs.core.__destructure_map(map__71155);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71155__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71156 = app;
var G__71157 = "GET";
var G__71158 = "/api/knoxx/health";
var G__71159 = (function (request,reply){
var G__71160 = runtime;
var G__71161 = request;
var G__71162 = reply;
var G__71163 = (function (ctx){
var G__71164 = reply;
var G__71165 = (200);
var G__71166 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"reachable","reachable",-1495191549),true,new cljs.core.Keyword(null,"configured","configured",-884777889),true,new cljs.core.Keyword(null,"base_url","base_url",-1764155256),new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(200),new cljs.core.Keyword(null,"details","details",1956795411),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"mode","mode",654403691),"shadow-cljs-eta-mu-sdk",new cljs.core.Keyword(null,"status","status",-1997798413),"ok",new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"collection","collection",-683361892),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"collection-name","collection-name",600435477).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"pointsCount","pointsCount",363767063),null], null)], null)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71164,G__71165,G__71166) : json_response_BANG_.call(null,G__71164,G__71165,G__71166));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71160,G__71161,G__71162,G__71163) : with_request_context_BANG_.call(null,G__71160,G__71161,G__71162,G__71163));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71156,G__71157,G__71158,G__71159) : route_BANG_.call(null,G__71156,G__71157,G__71158,G__71159));
});
knoxx.backend.routes.app.chat_turn_ok = (function knoxx$backend$routes$app$chat_turn_ok(reply,resp){
return knoxx.backend.http.json_response_BANG_(reply,(200),resp);
});
knoxx.backend.routes.app.chat_turn_err = (function knoxx$backend$routes$app$chat_turn_err(reply,err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502));
});
knoxx.backend.routes.app.api_knoxx_chat_BANG_ = (function knoxx$backend$routes$app$api_knoxx_chat_BANG_(app,runtime,config,deps){
var map__71167 = deps;
var map__71167__$1 = cljs.core.__destructure_map(map__71167);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71167__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71168 = app;
var G__71169 = "POST";
var G__71170 = "/api/knoxx/chat";
var G__71171 = (function (request,reply){
var G__71172 = runtime;
var G__71173 = request;
var G__71174 = reply;
var G__71175 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var parsed0 = knoxx.backend.app_shapes.normalize_chat_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
var parsed = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(parsed0,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),knoxx.backend.routes.app.merged_agent_spec(config,parsed0));
var agent_ctx = knoxx.backend.routes.app.effective_auth_context(ctx,parsed);
var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(parsed,new cljs.core.Keyword(null,"mode","mode",654403691),"rag",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"auth-context","auth-context",320032325),agent_ctx], 0));
return knoxx.backend.agent_turns.send_agent_turn_BANG_(runtime,config,body).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.chat_turn_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.chat_turn_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71172,G__71173,G__71174,G__71175) : with_request_context_BANG_.call(null,G__71172,G__71173,G__71174,G__71175));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71168,G__71169,G__71170,G__71171) : route_BANG_.call(null,G__71168,G__71169,G__71170,G__71171));
});
knoxx.backend.routes.app.api_knoxx_chat_start_BANG_ = (function knoxx$backend$routes$app$api_knoxx_chat_start_BANG_(app,runtime,config,deps){
var map__71176 = deps;
var map__71176__$1 = cljs.core.__destructure_map(map__71176);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71176__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71177 = app;
var G__71178 = "POST";
var G__71179 = "/api/knoxx/chat/start";
var G__71180 = (function (request,reply){
var G__71181 = runtime;
var G__71182 = request;
var G__71183 = reply;
var G__71184 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

return knoxx.backend.routes.app.handle_chat_start(runtime,config,reply,ctx,request);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71181,G__71182,G__71183,G__71184) : with_request_context_BANG_.call(null,G__71181,G__71182,G__71183,G__71184));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71177,G__71178,G__71179,G__71180) : route_BANG_.call(null,G__71177,G__71178,G__71179,G__71180));
});
knoxx.backend.routes.app.api_knoxx_direct_BANG_ = (function knoxx$backend$routes$app$api_knoxx_direct_BANG_(app,runtime,config,deps){
var map__71185 = deps;
var map__71185__$1 = cljs.core.__destructure_map(map__71185);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71185__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71186 = app;
var G__71187 = "POST";
var G__71188 = "/api/knoxx/direct";
var G__71189 = (function (request,reply){
var G__71190 = runtime;
var G__71191 = request;
var G__71192 = reply;
var G__71193 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var parsed0 = knoxx.backend.app_shapes.normalize_chat_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
var parsed = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(parsed0,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),knoxx.backend.routes.app.merged_agent_spec(config,parsed0));
var agent_ctx = knoxx.backend.routes.app.effective_auth_context(ctx,parsed);
var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(parsed,new cljs.core.Keyword(null,"mode","mode",654403691),"direct",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"auth-context","auth-context",320032325),agent_ctx], 0));
return knoxx.backend.agent_turns.send_agent_turn_BANG_(runtime,config,body).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.chat_turn_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.chat_turn_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71190,G__71191,G__71192,G__71193) : with_request_context_BANG_.call(null,G__71190,G__71191,G__71192,G__71193));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71186,G__71187,G__71188,G__71189) : route_BANG_.call(null,G__71186,G__71187,G__71188,G__71189));
});
knoxx.backend.routes.app.api_knoxx_direct_start_BANG_ = (function knoxx$backend$routes$app$api_knoxx_direct_start_BANG_(app,runtime,config,deps){
var map__71194 = deps;
var map__71194__$1 = cljs.core.__destructure_map(map__71194);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71194__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71195 = app;
var G__71196 = "POST";
var G__71197 = "/api/knoxx/direct/start";
var G__71198 = (function (request,reply){
var G__71199 = runtime;
var G__71200 = request;
var G__71201 = reply;
var G__71202 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

return knoxx.backend.routes.app.handle_direct_start(runtime,config,reply,ctx,request);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71199,G__71200,G__71201,G__71202) : with_request_context_BANG_.call(null,G__71199,G__71200,G__71201,G__71202));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71195,G__71196,G__71197,G__71198) : route_BANG_.call(null,G__71195,G__71196,G__71197,G__71198));
});
knoxx.backend.routes.app.steer_ok = (function knoxx$backend$routes$app$steer_ok(reply,resp){
return knoxx.backend.http.json_response_BANG_(reply,(200),resp);
});
knoxx.backend.routes.app.steer_err = (function knoxx$backend$routes$app$steer_err(reply,err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(409));
});
knoxx.backend.routes.app.api_knoxx_steer_BANG_ = (function knoxx$backend$routes$app$api_knoxx_steer_BANG_(app,runtime,config,deps){
var map__71203 = deps;
var map__71203__$1 = cljs.core.__destructure_map(map__71203);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71203__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71204 = app;
var G__71205 = "POST";
var G__71206 = "/api/knoxx/steer";
var G__71207 = (function (request,reply){
var G__71208 = runtime;
var G__71209 = request;
var G__71210 = reply;
var G__71211 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.controls.steer") : ensure_permission_BANG_.call(null,ctx,"agent.controls.steer"));
} else {
}

var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.app_shapes.normalize_control_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})()),new cljs.core.Keyword(null,"kind","kind",-717265803),"steer");
var actor_ctx = knoxx.backend.routes.app.auth_context_with_actor(ctx,new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(body));
knoxx.backend.agent_turns.ensure_conversation_access_BANG_(actor_ctx,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(body));

return knoxx.backend.agent_runtime.queue_agent_control_BANG_(runtime,config,body).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.steer_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.steer_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71208,G__71209,G__71210,G__71211) : with_request_context_BANG_.call(null,G__71208,G__71209,G__71210,G__71211));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71204,G__71205,G__71206,G__71207) : route_BANG_.call(null,G__71204,G__71205,G__71206,G__71207));
});
knoxx.backend.routes.app.api_knoxx_follow_up_BANG_ = (function knoxx$backend$routes$app$api_knoxx_follow_up_BANG_(app,runtime,config,deps){
var map__71212 = deps;
var map__71212__$1 = cljs.core.__destructure_map(map__71212);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71212__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71213 = app;
var G__71214 = "POST";
var G__71215 = "/api/knoxx/follow-up";
var G__71216 = (function (request,reply){
var G__71217 = runtime;
var G__71218 = request;
var G__71219 = reply;
var G__71220 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.controls.follow_up") : ensure_permission_BANG_.call(null,ctx,"agent.controls.follow_up"));
} else {
}

var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.app_shapes.normalize_control_body((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})()),new cljs.core.Keyword(null,"kind","kind",-717265803),"follow_up");
var actor_ctx = knoxx.backend.routes.app.auth_context_with_actor(ctx,new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(body));
knoxx.backend.agent_turns.ensure_conversation_access_BANG_(actor_ctx,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(body));

return knoxx.backend.agent_runtime.queue_agent_control_BANG_(runtime,config,body).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.steer_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.steer_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71217,G__71218,G__71219,G__71220) : with_request_context_BANG_.call(null,G__71217,G__71218,G__71219,G__71220));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71213,G__71214,G__71215,G__71216) : route_BANG_.call(null,G__71213,G__71214,G__71215,G__71216));
});
knoxx.backend.routes.app.abort_ok = (function knoxx$backend$routes$app$abort_ok(reply,resp){
return knoxx.backend.http.json_response_BANG_(reply,(cljs.core.truth_(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(resp))?(200):(409)),resp);
});
knoxx.backend.routes.app.api_knoxx_abort_BANG_ = (function knoxx$backend$routes$app$api_knoxx_abort_BANG_(app,runtime,config,deps){
var map__71221 = deps;
var map__71221__$1 = cljs.core.__destructure_map(map__71221);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71221__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71222 = app;
var G__71223 = "POST";
var G__71224 = "/api/knoxx/abort";
var G__71225 = (function (request,reply){
var G__71226 = runtime;
var G__71227 = request;
var G__71228 = reply;
var G__71229 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.controls.steer") : ensure_permission_BANG_.call(null,ctx,"agent.controls.steer"));
} else {
}

var raw = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var conversation_id = (function (){var or__5142__auto__ = (raw["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["conversationId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var actor_id = (function (){var or__5142__auto__ = (raw["actor_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["actorId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (raw["actor-id"]);
}
}
})();
var actor_ctx = knoxx.backend.routes.app.auth_context_with_actor(ctx,actor_id);
var reason = (function (){var or__5142__auto__ = (raw["reason"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "aborted_by_user";
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)))){
var G__71230 = reply;
var G__71231 = (400);
var G__71232 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"conversation_id is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71230,G__71231,G__71232) : json_response_BANG_.call(null,G__71230,G__71231,G__71232));
} else {
knoxx.backend.agent_turns.ensure_conversation_access_BANG_(actor_ctx,conversation_id);

return knoxx.backend.turn_control.abort_active_turn_BANG_(conversation_id,reason).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.abort_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.steer_err,reply));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71226,G__71227,G__71228,G__71229) : with_request_context_BANG_.call(null,G__71226,G__71227,G__71228,G__71229));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71222,G__71223,G__71224,G__71225) : route_BANG_.call(null,G__71222,G__71223,G__71224,G__71225));
});
knoxx.backend.routes.app.api_knoxx_session_undo_BANG_ = (function knoxx$backend$routes$app$api_knoxx_session_undo_BANG_(app,runtime,config,deps){
var map__71233 = deps;
var map__71233__$1 = cljs.core.__destructure_map(map__71233);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71233__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71234 = app;
var G__71235 = "POST";
var G__71236 = "/api/knoxx/session/undo";
var G__71237 = (function (request,reply){
var G__71238 = runtime;
var G__71239 = request;
var G__71240 = reply;
var G__71241 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var raw = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["session_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["sessionId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var provided_conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (raw["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["conversationId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var actor_id = (function (){var or__5142__auto__ = (raw["actor_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (raw["actorId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (raw["actor-id"]);
}
}
})();
var actor_ctx = knoxx.backend.routes.app.auth_context_with_actor(ctx,actor_id);
var turns_raw = (function (){var or__5142__auto__ = (raw["turns"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var turns = (function (){var parsed = parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(turns_raw)),(10));
if(cljs.core.truth_(isNaN(parsed))){
return (1);
} else {
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),parsed);
}
})();
if(clojure.string.blank_QMARK_(session_id)){
var G__71242 = reply;
var G__71243 = (400);
var G__71244 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"session_id is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71242,G__71243,G__71244) : json_response_BANG_.call(null,G__71242,G__71243,G__71244));
} else {
return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
if((session == null)){
var G__71245 = reply;
var G__71246 = (404);
var G__71247 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Session not found or expired"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71245,G__71246,G__71247) : json_response_BANG_.call(null,G__71245,G__71246,G__71247));
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))){
var G__71248 = reply;
var G__71249 = (409);
var G__71250 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Cannot undo while a turn is still running"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71248,G__71249,G__71250) : json_response_BANG_.call(null,G__71248,G__71249,G__71250));
} else {
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = provided_conversation_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var current_messages = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var rewound_messages = knoxx.backend.session_store.rewind_messages(current_messages,turns);
var removed_count = (cljs.core.count(current_messages) - cljs.core.count(rewound_messages));
if(cljs.core.truth_((function (){var and__5140__auto__ = actor_ctx;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(conversation_id)));
} else {
return and__5140__auto__;
}
})())){
knoxx.backend.agent_turns.ensure_conversation_access_BANG_(actor_ctx,conversation_id);
} else {
}

if((removed_count === (0))){
var G__71251 = reply;
var G__71252 = (409);
var G__71253 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"No user turns available to undo"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71251,G__71252,G__71253) : json_response_BANG_.call(null,G__71251,G__71252,G__71253));
} else {
return knoxx.backend.session_store.undo_session_turns_BANG_(knoxx.backend.redis_client.get_client(),session_id,turns).then((function (_){
var G__71254 = reply;
var G__71255 = (200);
var G__71256 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"removed_count","removed_count",-1925224714),removed_count,new cljs.core.Keyword(null,"remaining_messages","remaining_messages",134744241),cljs.core.count(rewound_messages)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71254,G__71255,G__71256) : json_response_BANG_.call(null,G__71254,G__71255,G__71256));
}));
}

}
}
})).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.undo_session_err,reply));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71238,G__71239,G__71240,G__71241) : with_request_context_BANG_.call(null,G__71238,G__71239,G__71240,G__71241));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71234,G__71235,G__71236,G__71237) : route_BANG_.call(null,G__71234,G__71235,G__71236,G__71237));
});
knoxx.backend.routes.app.build_active_runs = (function knoxx$backend$routes$app$build_active_runs(ctx,limit){
var sessions_by_id = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (session){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session),session], null);
})),knoxx.backend.session_store.active_session_snapshots());
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (run){
return knoxx.backend.routes.app.active_run_summary(run,cljs.core.get.cljs$core$IFn$_invoke$arity$2(sessions_by_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(run)));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__71259_SHARP_){
return knoxx.backend.authz.run_visible_QMARK_(ctx,p1__71259_SHARP_);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__71258_SHARP_){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["waiting_input",null,"running",null,"queued",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__71258_SHARP_));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__71257_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),p1__71257_SHARP_);
}),cljs.core.deref(knoxx.backend.run_state.run_order_STAR_))))))));
});
knoxx.backend.routes.app.api_knoxx_agents_active_BANG_ = (function knoxx$backend$routes$app$api_knoxx_agents_active_BANG_(app,runtime,config,deps){
var map__71260 = deps;
var map__71260__$1 = cljs.core.__destructure_map(map__71260);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71260__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71261 = app;
var G__71262 = "GET";
var G__71263 = "/api/knoxx/agents/active";
var G__71264 = (function (request,reply){
var G__71265 = runtime;
var G__71266 = request;
var G__71267 = reply;
var G__71268 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var limit_raw = (request["query"]["limit"]);
var limit = ((typeof limit_raw === 'string')?cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),parseInt(limit_raw,(10))):(25));
var items = knoxx.backend.routes.app.build_active_runs(ctx,limit);
return knoxx.backend.routes.app.agents_active_ok(reply,items);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71265,G__71266,G__71267,G__71268) : with_request_context_BANG_.call(null,G__71265,G__71266,G__71267,G__71268));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71261,G__71262,G__71263,G__71264) : route_BANG_.call(null,G__71261,G__71262,G__71263,G__71264));
});
knoxx.backend.routes.app.api_admin_agents_active_BANG_ = (function knoxx$backend$routes$app$api_admin_agents_active_BANG_(app,runtime,config,deps){
var map__71269 = deps;
var map__71269__$1 = cljs.core.__destructure_map(map__71269);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71269__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71270 = app;
var G__71271 = "GET";
var G__71272 = "/api/admin/agents/active";
var G__71273 = (function (request,reply){
var G__71274 = runtime;
var G__71275 = request;
var G__71276 = reply;
var G__71277 = (function (ctx){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var limit_raw = (request["query"]["limit"]);
var limit = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(limit_raw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (200);
}
})();
return knoxx.backend.routes.app.live_active_agent_summaries_BANG_(limit,false).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.agents_active_ok,reply)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.agents_active_err,reply));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71274,G__71275,G__71276,G__71277) : with_request_context_BANG_.call(null,G__71274,G__71275,G__71276,G__71277));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71270,G__71271,G__71272,G__71273) : route_BANG_.call(null,G__71270,G__71271,G__71272,G__71273));
});
knoxx.backend.routes.app.api_admin_agents_abort_BANG_ = (function knoxx$backend$routes$app$api_admin_agents_abort_BANG_(app,runtime,config,deps){
var map__71278 = deps;
var map__71278__$1 = cljs.core.__destructure_map(map__71278);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71278__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71279 = app;
var G__71280 = "POST";
var G__71281 = "/api/admin/agents/abort";
var G__71282 = (function (request,reply){
var G__71283 = runtime;
var G__71284 = request;
var G__71285 = reply;
var G__71286 = (function (ctx){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

return knoxx.backend.routes.app.handle_admin_abort(reply,ctx,request);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71283,G__71284,G__71285,G__71286) : with_request_context_BANG_.call(null,G__71283,G__71284,G__71285,G__71286));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71279,G__71280,G__71281,G__71282) : route_BANG_.call(null,G__71279,G__71280,G__71281,G__71282));
});
knoxx.backend.routes.app.api_knoxx_session_status_BANG_ = (function knoxx$backend$routes$app$api_knoxx_session_status_BANG_(app,runtime,config,deps){
var map__71287 = deps;
var map__71287__$1 = cljs.core.__destructure_map(map__71287);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71287__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71288 = app;
var G__71289 = "GET";
var G__71290 = "/api/knoxx/session/status";
var G__71291 = (function (request,reply){
var G__71292 = runtime;
var G__71293 = request;
var G__71294 = reply;
var G__71295 = (function (ctx){
return knoxx.backend.routes.app.handle_session_status(runtime,config,reply,request);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71292,G__71293,G__71294,G__71295) : with_request_context_BANG_.call(null,G__71292,G__71293,G__71294,G__71295));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71288,G__71289,G__71290,G__71291) : route_BANG_.call(null,G__71288,G__71289,G__71290,G__71291));
});
knoxx.backend.routes.app.api_knoxx_run_events_BANG_ = (function knoxx$backend$routes$app$api_knoxx_run_events_BANG_(app,runtime,config,deps){
var map__71296 = deps;
var map__71296__$1 = cljs.core.__destructure_map(map__71296);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71296__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71297 = app;
var G__71298 = "GET";
var G__71299 = "/api/knoxx/run/:runId/events";
var G__71300 = (function (request,reply){
var G__71301 = runtime;
var G__71302 = request;
var G__71303 = reply;
var G__71304 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var run_id = (request["params"]["runId"]);
var since = (function (){var or__5142__auto__ = (request["query"]["since"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(run_id)){
var G__71305 = reply;
var G__71306 = (400);
var G__71307 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"runId is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71305,G__71306,G__71307) : json_response_BANG_.call(null,G__71305,G__71306,G__71307));
} else {
return knoxx.backend.run_state.get_run_events_since(knoxx.backend.redis_client.get_client(),run_id,since).then(cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.app.run_events_ok,reply,run_id)).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.run_events_err,reply));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71301,G__71302,G__71303,G__71304) : with_request_context_BANG_.call(null,G__71301,G__71302,G__71303,G__71304));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71297,G__71298,G__71299,G__71300) : route_BANG_.call(null,G__71297,G__71298,G__71299,G__71300));
});
knoxx.backend.routes.app.api_knoxx_run_get_BANG_ = (function knoxx$backend$routes$app$api_knoxx_run_get_BANG_(app,runtime,config,deps){
var map__71308 = deps;
var map__71308__$1 = cljs.core.__destructure_map(map__71308);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71308__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71309 = app;
var G__71310 = "GET";
var G__71311 = "/api/knoxx/runs/:runId";
var G__71312 = (function (request,reply){
var G__71313 = runtime;
var G__71314 = request;
var G__71315 = reply;
var G__71316 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var run_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["params"]["runId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(run_id)){
var G__71317 = reply;
var G__71318 = (400);
var G__71319 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"runId required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71317,G__71318,G__71319) : json_response_BANG_.call(null,G__71317,G__71318,G__71319));
} else {
if((!((cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),run_id) == null)))){
var temp__5823__auto__ = knoxx.backend.authz.run_visible_QMARK_(ctx,cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),run_id));
if(temp__5823__auto__){
var filtered = temp__5823__auto__;
var G__71320 = reply;
var G__71321 = (200);
var G__71322 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"source","source",-433931539),"memory",new cljs.core.Keyword(null,"run","run",-1821166653),filtered], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71320,G__71321,G__71322) : json_response_BANG_.call(null,G__71320,G__71321,G__71322));
} else {
var G__71323 = reply;
var G__71324 = (403);
var G__71325 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Access denied"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71323,G__71324,G__71325) : json_response_BANG_.call(null,G__71323,G__71324,G__71325));
}
} else {
var G__71326 = reply;
var G__71327 = (404);
var G__71328 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"Run not found",new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71326,G__71327,G__71328) : json_response_BANG_.call(null,G__71326,G__71327,G__71328));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71313,G__71314,G__71315,G__71316) : with_request_context_BANG_.call(null,G__71313,G__71314,G__71315,G__71316));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71309,G__71310,G__71311,G__71312) : route_BANG_.call(null,G__71309,G__71310,G__71311,G__71312));
});
knoxx.backend.routes.app.api_shibboleth_handoff_BANG_ = (function knoxx$backend$routes$app$api_shibboleth_handoff_BANG_(app,runtime,config,deps){
var map__71329 = deps;
var map__71329__$1 = cljs.core.__destructure_map(map__71329);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__71329__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__71330 = app;
var G__71331 = "POST";
var G__71332 = "/api/shibboleth/handoff";
var G__71333 = (function (request,reply){
var G__71334 = runtime;
var G__71335 = request;
var G__71336 = reply;
var G__71337 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
if(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"shibboleth-base-url","shibboleth-base-url",-351013125).cljs$core$IFn$_invoke$arity$1(config))){
var G__71338 = reply;
var G__71339 = (503);
var G__71340 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"SHIBBOLETH_BASE_URL is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__71338,G__71339,G__71340) : json_response_BANG_.call(null,G__71338,G__71339,G__71340));
} else {
var payload = ({"source_app": "knoxx", "model": (body["model"]), "system_prompt": (body["system_prompt"]), "provider": (body["provider"]), "conversation_id": (body["conversation_id"]), "fake_tools_enabled": cljs.core.boolean$((body["fake_tools_enabled"])), "items": (function (){var or__5142__auto__ = (body["items"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()});
return (function (){var G__71341 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"shibboleth-base-url","shibboleth-base-url",-351013125).cljs$core$IFn$_invoke$arity$1(config))+"/api/chat/import");
var G__71342 = ({"method": "POST", "headers": ({"Content-Type": "application/json"}), "body": JSON.stringify(payload)});
return (fetch_json.cljs$core$IFn$_invoke$arity$2 ? fetch_json.cljs$core$IFn$_invoke$arity$2(G__71341,G__71342) : fetch_json.call(null,G__71341,G__71342));
})().then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return knoxx.backend.routes.app.shibboleth_ok(reply,request,body,(resp["body"]));
} else {
return knoxx.backend.routes.app.shibboleth_import_failed(reply,resp);
}
})).catch(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.app.shibboleth_unreachable,reply));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__71334,G__71335,G__71336,G__71337) : with_request_context_BANG_.call(null,G__71334,G__71335,G__71336,G__71337));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__71330,G__71331,G__71332,G__71333) : route_BANG_.call(null,G__71330,G__71331,G__71332,G__71333));
});
knoxx.backend.routes.app.register_routes_BANG_ = (function knoxx$backend$routes$app$register_routes_BANG_(runtime,app,config,lounge_messages_STAR_){
knoxx.backend.agent_hydration.ensure_settings_BANG_(config);

knoxx.backend.routes.app.health_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.dev_hmr_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.config_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_agents_catalog_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_auth_context_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.admin.register_admin_routes_BANG_(app,runtime,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183),new cljs.core.Keyword(null,"ensure-any-permission!","ensure-any-permission!",1999271593),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"http-error","http-error",-1040049553),new cljs.core.Keyword(null,"policy-db-promise","policy-db-promise",-584929935),new cljs.core.Keyword(null,"ensure-org-scope!","ensure-org-scope!",-1115734566),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163)],[knoxx.backend.app_shapes.route_BANG_,knoxx.backend.authz.policy_db,knoxx.backend.authz.ensure_any_permission_BANG_,knoxx.backend.http.json_response_BANG_,knoxx.backend.authz.with_request_context_BANG_,knoxx.backend.http.http_error,knoxx.backend.authz.policy_db_promise,knoxx.backend.authz.ensure_org_scope_BANG_,knoxx.backend.authz.ensure_permission_BANG_]));

knoxx.backend.routes.memory.register_memory_routes_BANG_(app,runtime,config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"openplanner-memory-search!","openplanner-memory-search!",1781688896),new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"ctx-permitted?","ctx-permitted?",-1842773024),new cljs.core.Keyword(null,"filter-authorized-memory-hits!","filter-authorized-memory-hits!",-1951695933),new cljs.core.Keyword(null,"session-matches-page-actor-filter?","session-matches-page-actor-filter?",2088135972),new cljs.core.Keyword(null,"cache-session-title-entry!","cache-session-title-entry!",-1970978492),new cljs.core.Keyword(null,"heuristic-session-title","heuristic-session-title",1513408292),new cljs.core.Keyword(null,"authorized-session-ids!","authorized-session-ids!",999199653),new cljs.core.Keyword(null,"system-admin?","system-admin?",-148862842),new cljs.core.Keyword(null,"fetch-openplanner-session-rows!","fetch-openplanner-session-rows!",1014940648),new cljs.core.Keyword(null,"now-iso","now-iso",74414857),new cljs.core.Keyword(null,"get-cached-session-title!","get-cached-session-title!",1808522986),new cljs.core.Keyword(null,"parse-positive-int","parse-positive-int",728793034),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"session-title-backfill*","session-title-backfill*",-1810746770),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"normalize-session-title","normalize-session-title",-1159928850),new cljs.core.Keyword(null,"http-error","http-error",-1040049553),new cljs.core.Keyword(null,"lounge-messages*","lounge-messages*",-1382832656),new cljs.core.Keyword(null,"session-titles*","session-titles*",1985458162),new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),new cljs.core.Keyword(null,"truthy-param?","truthy-param?",-219040013),new cljs.core.Keyword(null,"stored-session-title-entry","stored-session-title-entry",803389171),new cljs.core.Keyword(null,"cache-session-title!","cache-session-title!",1861418325),new cljs.core.Keyword(null,"openplanner-enabled?","openplanner-enabled?",-1180234471),new cljs.core.Keyword(null,"session-visible?","session-visible?",-1647736199),new cljs.core.Keyword(null,"openplanner-request!","openplanner-request!",-1690277990),new cljs.core.Keyword(null,"session-title-seed-text","session-title-seed-text",1029264443),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),new cljs.core.Keyword(null,"broadcast-ws!","broadcast-ws!",-1433906309),new cljs.core.Keyword(null,"start-session-title-backfill!","start-session-title-backfill!",1417456380),new cljs.core.Keyword(null,"resolve-session-title!","resolve-session-title!",-281505699)],[knoxx.backend.openplanner_memory.openplanner_memory_search_BANG_,knoxx.backend.app_shapes.route_BANG_,knoxx.backend.authz.ctx_permitted_QMARK_,knoxx.backend.core_memory.filter_authorized_memory_hits_BANG_,knoxx.backend.core_memory.session_matches_page_actor_filter_QMARK_,knoxx.backend.session_titles.cache_session_title_entry_BANG_,knoxx.backend.session_titles.heuristic_session_title,knoxx.backend.core_memory.authorized_session_ids_BANG_,knoxx.backend.authz.system_admin_QMARK_,knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_,knoxx.backend.util.time.now_iso,knoxx.backend.session_titles.get_cached_session_title_BANG_,knoxx.backend.util.parse.parse_positive_int,knoxx.backend.http.json_response_BANG_,knoxx.backend.session_titles.session_title_backfill_STAR_,knoxx.backend.authz.with_request_context_BANG_,knoxx.backend.session_titles.normalize_session_title,knoxx.backend.http.http_error,lounge_messages_STAR_,knoxx.backend.session_titles.session_titles_STAR_,knoxx.backend.http.error_response_BANG_,knoxx.backend.util.parse.truthy_param_QMARK_,knoxx.backend.session_titles.stored_session_title_entry,knoxx.backend.session_titles.cache_session_title_BANG_,knoxx.backend.http.openplanner_enabled_QMARK_,knoxx.backend.core_memory.session_visible_QMARK_,knoxx.backend.http.openplanner_request_BANG_,knoxx.backend.session_titles.session_title_seed_text,knoxx.backend.authz.ensure_permission_BANG_,knoxx.backend.realtime.broadcast_ws_BANG_,knoxx.backend.session_titles.start_session_title_backfill_BANG_,knoxx.backend.session_titles.resolve_session_title_BANG_]));

var session_guard_71344 = knoxx.backend.guards.make_session_guard(runtime);
var optional_session_guard_71345 = knoxx.backend.guards.make_optional_session_guard(runtime);
knoxx.backend.routes.tools.register_tool_routes_BANG_(app,runtime,config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"resolve-workspace-path","resolve-workspace-path",-1439207488),new cljs.core.Keyword(null,"tool-catalog","tool-catalog",899421286),new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954),new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577),new cljs.core.Keyword(null,"count-occurrences","count-occurrences",1068095177),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966),new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),new cljs.core.Keyword(null,"replace-first","replace-first",1710901438)],[knoxx.backend.app_shapes.route_BANG_,knoxx.backend.agent_runtime.resolve_workspace_path,knoxx.backend.tooling.tool_catalog,session_guard_71344,knoxx.backend.tooling.ensure_role_can_use_BANG_,knoxx.backend.text.count_occurrences,knoxx.backend.http.json_response_BANG_,knoxx.backend.authz.with_request_context_BANG_,optional_session_guard_71345,knoxx.backend.http.error_response_BANG_,knoxx.backend.text.clip_text,knoxx.backend.authz.ensure_permission_BANG_,knoxx.backend.text.replace_first]));

knoxx.backend.routes.actors.register_actor_routes_BANG_(app,runtime,config,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"route!","route!",-1286958144),knoxx.backend.app_shapes.route_BANG_,new cljs.core.Keyword(null,"json-response!","json-response!",103570476),knoxx.backend.http.json_response_BANG_,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),knoxx.backend.http.error_response_BANG_,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),knoxx.backend.authz.with_request_context_BANG_,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),knoxx.backend.authz.ensure_permission_BANG_,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954),session_guard_71344], null));

knoxx.backend.routes.contracts.register_contracts_routes_BANG_(app,runtime,config,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"route!","route!",-1286958144),knoxx.backend.app_shapes.route_BANG_,new cljs.core.Keyword(null,"json-response!","json-response!",103570476),knoxx.backend.http.json_response_BANG_,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),knoxx.backend.http.error_response_BANG_,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),knoxx.backend.authz.with_request_context_BANG_,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),knoxx.backend.authz.ensure_permission_BANG_], null));

knoxx.backend.routes.models.register_model_routes_BANG_(app,runtime,config);

knoxx.backend.routes.voice.register_voice_routes_BANG_(app,runtime,config,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"route!","route!",-1286958144),knoxx.backend.app_shapes.route_BANG_,new cljs.core.Keyword(null,"json-response!","json-response!",103570476),knoxx.backend.http.json_response_BANG_,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),knoxx.backend.authz.with_request_context_BANG_,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334),knoxx.backend.authz.ensure_tool_BANG_], null));

knoxx.backend.routes.documents.register_document_routes_BANG_(app,runtime,config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848),new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"openai-auth-error","openai-auth-error",-466046941),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000),new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310),new cljs.core.Keyword(null,"openplanner-graph-export!","openplanner-graph-export!",-1726254887),new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686)],[knoxx.backend.http.request_query_string,knoxx.backend.app_shapes.route_BANG_,knoxx.backend.http.openai_auth_error,knoxx.backend.http.json_response_BANG_,knoxx.backend.authz.with_request_context_BANG_,knoxx.backend.http.send_fetch_response_BANG_,knoxx.backend.http.error_response_BANG_,knoxx.backend.http.bearer_headers,knoxx.backend.openplanner_memory.openplanner_graph_export_BANG_,knoxx.backend.text.clip_text,knoxx.backend.authz.ensure_permission_BANG_,knoxx.backend.http.fetch_json]));

knoxx.backend.routes.workspace_media.register_workspace_media_routes_BANG_(app,runtime,config,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"route!","route!",-1286958144),knoxx.backend.app_shapes.route_BANG_,new cljs.core.Keyword(null,"json-response!","json-response!",103570476),knoxx.backend.http.json_response_BANG_,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),knoxx.backend.http.error_response_BANG_,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),knoxx.backend.authz.with_request_context_BANG_,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334),knoxx.backend.authz.ensure_tool_BANG_], null));

knoxx.backend.routes.studio.register_studio_routes_BANG_(app,runtime,config,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"route!","route!",-1286958144),knoxx.backend.app_shapes.route_BANG_,new cljs.core.Keyword(null,"json-response!","json-response!",103570476),knoxx.backend.http.json_response_BANG_,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),knoxx.backend.http.error_response_BANG_,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),knoxx.backend.authz.with_request_context_BANG_,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334),knoxx.backend.authz.ensure_tool_BANG_,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163),knoxx.backend.authz.ensure_permission_BANG_,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183),knoxx.backend.authz.policy_db,new cljs.core.Keyword(null,"policy-db-promise","policy-db-promise",-584929935),knoxx.backend.authz.policy_db_promise], null));

knoxx.backend.routes.app.api_knoxx_proxy_get_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_proxy_post_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_proxy_put_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_proxy_patch_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_proxy_delete_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_browse_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_file_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_sources_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_jobs_get_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_jobs_post_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_proxy_get_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_proxy_post_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_ingestion_proxy_delete_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_op_get_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_op_post_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_op_patch_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_op_delete_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_health_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_mongo_collections_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_mongo_list_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_mongo_query_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_pg_tables_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_jobs_build_semantic_edges_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_pg_query_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_browse_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_file_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_graphql_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_graph_status_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_data_graph_view_url_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_health_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_chat_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_chat_start_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_direct_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_direct_start_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_steer_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_follow_up_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_abort_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_session_undo_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_agents_active_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_admin_agents_active_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_admin_agents_abort_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_session_status_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_run_events_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_knoxx_run_get_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

knoxx.backend.routes.app.api_shibboleth_handoff_BANG_(app,runtime,config,knoxx.backend.routes.app.deps);

return knoxx.backend.routes.translation.register_translation_routes_BANG_(app,runtime,config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ctx-org-id","ctx-org-id",949922116),new cljs.core.Keyword(null,"openplanner-url","openplanner-url",-1804248247),new cljs.core.Keyword(null,"json-response!","json-response!",103570476),new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046),new cljs.core.Keyword(null,"ctx-user-id","ctx-user-id",-259951088),new cljs.core.Keyword(null,"error-response!","error-response!",-856339341),new cljs.core.Keyword(null,"ctx-user-email","ctx-user-email",-64148717),new cljs.core.Keyword(null,"openplanner-headers","openplanner-headers",1561778839),new cljs.core.Keyword(null,"openplanner-enabled?","openplanner-enabled?",-1180234471),new cljs.core.Keyword(null,"openplanner-request!","openplanner-request!",-1690277990),new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163)],[knoxx.backend.authz.ctx_org_id,knoxx.backend.http.openplanner_url,knoxx.backend.http.json_response_BANG_,knoxx.backend.authz.with_request_context_BANG_,knoxx.backend.authz.ctx_user_id,knoxx.backend.http.error_response_BANG_,knoxx.backend.authz.ctx_user_email,knoxx.backend.http.openplanner_headers,knoxx.backend.http.openplanner_enabled_QMARK_,knoxx.backend.http.openplanner_request_BANG_,knoxx.backend.authz.ensure_permission_BANG_]));
});

//# sourceMappingURL=knoxx.backend.routes.app.js.map
