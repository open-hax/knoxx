import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agents.content.js";
import "./knoxx.backend.agents.policy.js";
import "./knoxx.backend.agents.stream.js";
import "./knoxx.backend.agents.transcript.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.session_titles.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.turn_control.js";
import "./knoxx.backend.agent_context.js";
import "./knoxx.backend.util.time.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
goog.provide('knoxx.backend.agents.turn');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agents !== 'undefined') && (typeof knoxx.backend.agents.turn !== 'undefined') && (typeof knoxx.backend.agents.turn.conversation_access_STAR_ !== 'undefined')){
} else {
knoxx.backend.agents.turn.conversation_access_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agents !== 'undefined') && (typeof knoxx.backend.agents.turn !== 'undefined') && (typeof knoxx.backend.agents.turn.lounge_messages_STAR_ !== 'undefined')){
} else {
knoxx.backend.agents.turn.lounge_messages_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY);
}
knoxx.backend.agents.turn.flatten_template_values = (function knoxx$backend$agents$turn$flatten_template_values(var_args){
var G__65981 = arguments.length;
switch (G__65981) {
case 1:
return knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$2(null,value);
}));

(knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$2 = (function (prefix,value){
if(cljs.core.map_QMARK_(value)){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$1((function (p__65982){
var vec__65983 = p__65982;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65983,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65983,(1),null);
var key_name = (((k instanceof cljs.core.Keyword))?cljs.core.name(k):((typeof k === 'string')?k:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k))
));
var next_prefix = (cljs.core.truth_(prefix)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key_name)):key_name);
return knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$2(next_prefix,v);
})),value);
} else {
if(cljs.core.sequential_QMARK_(value)){
if(cljs.core.truth_(prefix)){
return cljs.core.PersistentArrayMap.createAsIfByAssoc([prefix,clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,value))]);
} else {
return null;
}
} else {
if((!((value == null)))){
if(cljs.core.truth_(prefix)){
return cljs.core.PersistentArrayMap.createAsIfByAssoc([prefix,value]);
} else {
return null;
}
} else {
return cljs.core.PersistentArrayMap.EMPTY;

}
}
}
}));

(knoxx.backend.agents.turn.flatten_template_values.cljs$lang$maxFixedArity = 2);

knoxx.backend.agents.turn.auth_context_template_values = (function knoxx$backend$agents$turn$auth_context_template_values(auth_context){
var user = new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(auth_context);
var org = new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(auth_context);
var membership = new cljs.core.Keyword(null,"membership","membership",254556333).cljs$core$IFn$_invoke$arity$1(auth_context);
var email = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"user-email","user-email",2126479881).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"user-email","user-email",2126479881).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(user);
}
}
}
}
}
})();
var display_name = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"display-name","display-name",694513143).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"displayName","displayName",-809144601).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"display-name","display-name",694513143).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = new cljs.core.Keyword(null,"displayName","displayName",-809144601).cljs$core$IFn$_invoke$arity$1(user);
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
return email;
}
}
}
}
}
}
})();
var org_slug = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-slug","org-slug",-726595051).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"slug","slug",2029314850).cljs$core$IFn$_invoke$arity$1(org);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"org-slug","org-slug",-726595051).cljs$core$IFn$_invoke$arity$1(org);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998).cljs$core$IFn$_invoke$arity$1(org);
}
}
}
}
})();
return new cljs.core.PersistentArrayMap(null, 6, ["name",display_name,"email",email,"user.email",email,"user.name",display_name,"org.slug",org_slug,"membership.id",(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"membership-id","membership-id",-723542492).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"membershipId","membershipId",2026001076).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(membership);
}
}
})()], null);
});
knoxx.backend.agents.turn.render_task_prompt = (function knoxx$backend$agents$turn$render_task_prompt(var_args){
var G__65987 = arguments.length;
switch (G__65987) {
case 2:
return knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$2 = (function (task_prompt,auth_context){
return knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$3(task_prompt,auth_context,null);
}));

(knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$3 = (function (task_prompt,auth_context,template_context){
var temp__5825__auto__ = knoxx.backend.agents.content.nonblank(task_prompt);
if(cljs.core.truth_(temp__5825__auto__)){
var template = temp__5825__auto__;
var values = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.agents.turn.auth_context_template_values(auth_context),knoxx.backend.agents.turn.flatten_template_values.cljs$core$IFn$_invoke$arity$1(template_context)], 0));
return clojure.string.replace(template,/\{ctx\.([A-Za-z0-9_.-]+)\}/,(function (match){
var parts = ((cljs.core.vector_QMARK_(match))?match:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [match], null));
var full = cljs.core.first(parts);
var key = cljs.core.second(parts);
var or__5142__auto__ = (function (){var G__65988 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(values,key);
if((G__65988 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65988));
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return full;
}
}));
} else {
return null;
}
}));

(knoxx.backend.agents.turn.render_task_prompt.cljs$lang$maxFixedArity = 3);

knoxx.backend.agents.turn.ensure_conversation_access_BANG_ = (function knoxx$backend$agents$turn$ensure_conversation_access_BANG_(ctx,conversation_id){
return knoxx.backend.authz.ensure_conversation_access_BANG_(knoxx.backend.agents.turn.conversation_access_STAR_,ctx,conversation_id);
});
knoxx.backend.agents.turn.remember_conversation_access_BANG_ = (function knoxx$backend$agents$turn$remember_conversation_access_BANG_(ctx,conversation_id){
return knoxx.backend.authz.remember_conversation_access_BANG_(knoxx.backend.agents.turn.conversation_access_STAR_,ctx,conversation_id);
});
knoxx.backend.agents.turn.auth_context_for_agent_turn = (function knoxx$backend$agents$turn$auth_context_for_agent_turn(auth_context,agent_spec){
var agent_actor_id = (function (){var G__65989 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__65989__$1 = (((G__65989 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65989)));
var G__65989__$2 = (((G__65989__$1 == null))?null:clojure.string.trim(G__65989__$1));
if((G__65989__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65989__$2);
}
})();
var needs_context_QMARK_ = (function (){var or__5142__auto__ = auth_context;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = agent_actor_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.seq(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(or__5142__auto____$2){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
}
}
})();
if(cljs.core.truth_(needs_context_QMARK_)){
var G__65990 = (function (){var or__5142__auto__ = auth_context;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var G__65990__$1 = (cljs.core.truth_(agent_actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65990,new cljs.core.Keyword(null,"actorId","actorId",989542370),agent_actor_id):G__65990);
var G__65990__$2 = (((((auth_context == null)) && (cljs.core.seq(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec)))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65990__$1,new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),cljs.core.vec(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec))):G__65990__$1);
if(cljs.core.truth_((function (){var and__5140__auto__ = (auth_context == null);
if(and__5140__auto__){
return new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec);
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65990__$2,new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec)], null));
} else {
return G__65990__$2;
}
} else {
return null;
}
});
knoxx.backend.agents.turn.ensure_session_id = (function knoxx$backend$agents$turn$ensure_session_id(node_crypto,session_id){
var or__5142__auto__ = knoxx.backend.agents.content.nonblank(session_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return node_crypto.randomUUID();
}
});
/**
 * Extract a leading <think>...</think> block from assistant text.
 * 
 * Some Gemma-family models emit thinking traces inline instead of as structured
 * reasoning parts. This keeps the assistant answer clean while preserving
 * the trace in :reasoning.
 */
knoxx.backend.agents.turn.split_think_tags = (function knoxx$backend$agents$turn$split_think_tags(text){
var text__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var open_idx = text__$1.indexOf("<think>");
var close_idx = text__$1.indexOf("</think>");
if((((open_idx >= (0))) && ((((close_idx >= (0))) && ((((open_idx < (64))) && ((close_idx > open_idx)))))))){
var thinking = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text__$1,(open_idx + (("<think>").length)),close_idx);
var after = cljs.core.subs.cljs$core$IFn$_invoke$arity$2(text__$1,(close_idx + (("</think>").length)));
var before = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text__$1,(0),open_idx);
var answer = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = before;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = after;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),clojure.string.trim(thinking),new cljs.core.Keyword(null,"answer","answer",-742633163),clojure.string.trim(answer),new cljs.core.Keyword(null,"hadThinkTags","hadThinkTags",-1568024818),true], null);
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),"",new cljs.core.Keyword(null,"answer","answer",-742633163),text__$1,new cljs.core.Keyword(null,"hadThinkTags","hadThinkTags",-1568024818),false], null);
}
});
knoxx.backend.agents.turn.agent_spec_summary = (function knoxx$backend$agents$turn$agent_spec_summary(agent_spec){
if(cljs.core.truth_(agent_spec)){
var G__65991 = cljs.core.PersistentArrayMap.EMPTY;
var G__65991__$1 = (cljs.core.truth_(new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991,new cljs.core.Keyword(null,"contractId","contractId",710260199),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991);
var G__65991__$2 = (cljs.core.truth_(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$1,new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$1);
var G__65991__$3 = ((cljs.core.seq(new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(agent_spec)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$2,new cljs.core.Keyword(null,"contractActors","contractActors",47284059),cljs.core.vec(new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(agent_spec))):G__65991__$2);
var G__65991__$4 = (cljs.core.truth_(new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$3,new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$3);
var G__65991__$5 = (cljs.core.truth_(new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$4,new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$4);
var G__65991__$6 = (cljs.core.truth_(new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$5,new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$5);
var G__65991__$7 = (cljs.core.truth_(new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$6,new cljs.core.Keyword(null,"hasSystemPrompt","hasSystemPrompt",1356421777),true):G__65991__$6);
var G__65991__$8 = (cljs.core.truth_(new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$7,new cljs.core.Keyword(null,"hasTaskPrompt","hasTaskPrompt",-1607821637),true):G__65991__$7);
var G__65991__$9 = ((cljs.core.seq(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$8,new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),cljs.core.vec(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec))):G__65991__$8);
var G__65991__$10 = (cljs.core.truth_(new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$9,new cljs.core.Keyword(null,"resourcePolicies","resourcePolicies",-1399026364),new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$9);
var G__65991__$11 = (cljs.core.truth_(new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$10,new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$10);
var G__65991__$12 = (cljs.core.truth_(new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$11,new cljs.core.Keyword(null,"subAgentId","subAgentId",538139792),new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$11);
var G__65991__$13 = (cljs.core.truth_(new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$12,new cljs.core.Keyword(null,"parentAgentId","parentAgentId",1686278200),new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$12);
var G__65991__$14 = (cljs.core.truth_(new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(agent_spec))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$13,new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271),new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(agent_spec)):G__65991__$13);
if(cljs.core.truth_(new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959).cljs$core$IFn$_invoke$arity$1(agent_spec))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65991__$14,new cljs.core.Keyword(null,"spawnKind","spawnKind",1648184297),new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959).cljs$core$IFn$_invoke$arity$1(agent_spec));
} else {
return G__65991__$14;
}
} else {
return null;
}
});
knoxx.backend.agents.turn.create_initial_run_BANG_ = (function knoxx$backend$agents$turn$create_initial_run_BANG_(run_id,session_id,conversation_id,started_at,model_id,mode,thinking_level,agent_spec,auth_extra,request_messages,config){
var base_run = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322),new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067),new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"tokens_per_s","tokens_per_s",1005457231),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"answer","answer",-742633163),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565),new cljs.core.Keyword(null,"resources","resources",1632806811),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498),new cljs.core.Keyword(null,"model","model",331153215)],[cljs.core.PersistentVector.EMPTY,null,cljs.core.PersistentVector.EMPTY,run_id,cljs.core.PersistentVector.EMPTY,null,cljs.core.PersistentVector.EMPTY,(function (){var G__65992 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),session_id,new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),conversation_id,new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),thinking_level,new cljs.core.Keyword(null,"workspaceRoot","workspaceRoot",493714538),new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config)], null);
if(cljs.core.truth_(agent_spec)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65992,new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),knoxx.backend.agents.turn.agent_spec_summary(agent_spec));
} else {
return G__65992;
}
})(),session_id,conversation_id,null,null,started_at,"running",null,null,request_messages,(function (){var G__65993 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"provider","provider",-302056900),"proxx",new cljs.core.Keyword(null,"collection","collection",-683361892),new cljs.core.Keyword(null,"collection-name","collection-name",600435477).cljs$core$IFn$_invoke$arity$1(config)], null);
if(cljs.core.truth_(cljs.core.get.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65993,new cljs.core.Keyword(null,"agentResourcePolicies","agentResourcePolicies",-1357376229),cljs.core.get.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874)));
} else {
return G__65993;
}
})(),started_at,null,model_id]),auth_extra], 0));
knoxx.backend.run_state.store_run_BANG_(run_id,base_run);

knoxx.backend.run_state.set_event_stream_sink_BANG_((function (event){
if(knoxx.backend.http.openplanner_enabled_QMARK_(config)){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/events",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.openplanner_memory.openplanner_event(config,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(event))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(event))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(event))),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(event),new cljs.core.Keyword(null,"kind","kind",-717265803),(""+"knoxx."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(event))),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(event),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(event),new cljs.core.Keyword(null,"role","role",-736691072),"system",new cljs.core.Keyword(null,"text","text",-1790561697),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(event))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(event))?(""+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(event))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"preview","preview",451279890).cljs$core$IFn$_invoke$arity$1(event))?(""+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"preview","preview",451279890).cljs$core$IFn$_invoke$arity$1(event))):null))),new cljs.core.Keyword(null,"extra","extra",1612569067),event], null))], null)], null)).catch((function (_){
return null;
}));
} else {
return null;
}
}));

knoxx.backend.session_store.put_session_BANG_(knoxx.backend.redis_client.get_client(),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var G__65994 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"messages","messages",345434482),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"model","model",331153215)],[run_id,mode,session_id,conversation_id,thinking_level,started_at,request_messages,"running",false,started_at,model_id]);
if(cljs.core.truth_(agent_spec)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65994,new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),knoxx.backend.agents.turn.agent_spec_summary(agent_spec));
} else {
return G__65994;
}
})(),auth_extra], 0)));

var initial_event = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,"run_started",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"model","model",331153215),model_id,new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069),thinking_level], null));
knoxx.backend.run_state.append_run_event_BANG_(run_id,initial_event);

return knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",initial_event);
});
knoxx.backend.agents.turn.finalize_turn_success_BANG_ = (function knoxx$backend$agents$turn$finalize_turn_success_BANG_(config,state,session,run_id,conversation_id,session_id,started_ms,model_id,mode,hydration,memory_hydration,persisted_request_messages,agent_spec){
var assistant_message = knoxx.backend.run_state.latest_assistant_message(session);
var answer = (function (){var chunked = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.deref(new cljs.core.Keyword(null,"chunks","chunks",83720431).cljs$core$IFn$_invoke$arity$1(state)));
if(clojure.string.blank_QMARK_(chunked)){
return knoxx.backend.text.assistant_message_text(assistant_message);
} else {
return chunked;
}
})();
var assistant_content_parts = knoxx.backend.agents.content.assistant_content_parts(assistant_message);
var usage = (function (){var or__5142__auto__ = (assistant_message["usage"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var reasoning_text = (function (){var streamed = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.deref(new cljs.core.Keyword(null,"reasoning-chunks","reasoning-chunks",-526618091).cljs$core$IFn$_invoke$arity$1(state)));
var final_reasoning = knoxx.backend.text.assistant_message_reasoning_text(assistant_message);
if(((clojure.string.blank_QMARK_(streamed)) && ((!(clojure.string.blank_QMARK_(final_reasoning)))))){
return final_reasoning;
} else {
if((((!(clojure.string.blank_QMARK_(final_reasoning)))) && ((cljs.core.count(final_reasoning) > cljs.core.count(streamed))))){
return final_reasoning;
} else {
return streamed;

}
}
})();
var think_split = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(reasoning_text))))?knoxx.backend.agents.turn.split_think_tags(answer):null);
var answer__$1 = (cljs.core.truth_((function (){var and__5140__auto__ = think_split;
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"hadThinkTags","hadThinkTags",-1568024818).cljs$core$IFn$_invoke$arity$1(think_split);
} else {
return and__5140__auto__;
}
})())?new cljs.core.Keyword(null,"answer","answer",-742633163).cljs$core$IFn$_invoke$arity$1(think_split):answer);
var reasoning_text__$1 = (cljs.core.truth_((function (){var and__5140__auto__ = think_split;
if(cljs.core.truth_(and__5140__auto__)){
return new cljs.core.Keyword(null,"hadThinkTags","hadThinkTags",-1568024818).cljs$core$IFn$_invoke$arity$1(think_split);
} else {
return and__5140__auto__;
}
})())?new cljs.core.Keyword(null,"reasoning","reasoning",1956143595).cljs$core$IFn$_invoke$arity$1(think_split):reasoning_text);
var elapsed = (Date.now() - started_ms);
var output_tokens = (function (){var or__5142__auto__ = (usage["output"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var tokens_per_second = (((((output_tokens > (0))) && ((elapsed > (0)))))?((1000) * (output_tokens / elapsed)):null);
var sources = knoxx.backend.agent_hydration.hydration_sources(hydration);
var message_parts = (function (){var G__65995 = cljs.core.PersistentVector.EMPTY;
var G__65995__$1 = (((!(clojure.string.blank_QMARK_(reasoning_text__$1))))?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__65995,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"role","role",-736691072),"thinking",new cljs.core.Keyword(null,"content","content",15833224),reasoning_text__$1,new cljs.core.Keyword(null,"reasoningType","reasoningType",-1978480536),"reasoning_summary"], null)):G__65995);
if((!(clojure.string.blank_QMARK_(answer__$1)))){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__65995__$1,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"assistant",new cljs.core.Keyword(null,"content","content",15833224),answer__$1], null));
} else {
return G__65995__$1;
}
})();
var completed_event = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,"run_completed",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"completed",new cljs.core.Keyword(null,"model","model",331153215),model_id,new cljs.core.Keyword(null,"sources_count","sources_count",723026405),cljs.core.count(sources)], null));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mode,"rag")){
knoxx.backend.run_state.record_retrieval_sample_BANG_(new cljs.core.Keyword(null,"retrievalMode","retrievalMode",-1090540764).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_)),elapsed);
} else {
}

knoxx.backend.run_state.finalize_run_trace_blocks_BANG_(run_id,"done");

var completed_run = knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
var resource_patch = (function (){var G__65996 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"sources","sources",-321166424),sources], null);
var G__65996__$1 = (cljs.core.truth_(hydration)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65996,new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907),cljs.core.select_keys(hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"tokens","tokens",-818939304),new cljs.core.Keyword(null,"database","database",1849087575),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"results","results",-1134170113)], null))):G__65996);
if(cljs.core.truth_(memory_hydration)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65996__$1,new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759),cljs.core.select_keys(memory_hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"hits","hits",-2120002930),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996)], null)));
} else {
return G__65996__$1;
}
})();
var merged_content_parts = knoxx.backend.agents.content.merge_content_parts.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([assistant_content_parts,knoxx.backend.agents.content.reply_attachment_content_parts(new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run))], 0));
return cljs.core.update.cljs$core$IFn$_invoke$arity$4(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(run,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"status","status",-1997798413),"completed",new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),elapsed,new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322),(function (){var or__5142__auto__ = (usage["input"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498),output_tokens,new cljs.core.Keyword(null,"tokens_per_s","tokens_per_s",1005457231),tokens_per_second,new cljs.core.Keyword(null,"answer","answer",-742633163),answer__$1,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),merged_content_parts,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),reasoning_text__$1,new cljs.core.Keyword(null,"sources","sources",-321166424),sources], 0)),new cljs.core.Keyword(null,"resources","resources",1632806811),cljs.core.merge,resource_patch);
}));
var merged_content_parts = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(completed_run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return assistant_content_parts;
}
})());
var response = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.Keyword(null,"runId","runId",505587730),new cljs.core.Keyword(null,"answer","answer",-742633163),new cljs.core.Keyword(null,"compare","compare",-530677770),new cljs.core.Keyword(null,"message_parts","message_parts",-1030507719),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),new cljs.core.Keyword(null,"model","model",331153215)],[merged_content_parts,run_id,sources,session_id,conversation_id,run_id,answer__$1,null,message_parts,conversation_id,model_id]);
var _ = (cljs.core.truth_(completed_run)?knoxx.backend.openplanner_memory.index_run_memory_BANG_(config,completed_run,knoxx.backend.core_memory.extract_mentioned_devel_paths,knoxx.backend.core_memory.extract_mentioned_urls):null);
knoxx.backend.run_state.append_run_event_BANG_(run_id,completed_event);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",completed_event);

var final_messages_66041 = knoxx.backend.agent_runtime.prune_session_messages(agent_spec,knoxx.backend.agents.transcript.transcript_after_turn(session,cljs.core.conj.cljs$core$IFn$_invoke$arity$2(persisted_request_messages,(function (){var G__65997 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"assistant",new cljs.core.Keyword(null,"content","content",15833224),answer__$1], null);
if(cljs.core.seq(merged_content_parts)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65997,new cljs.core.Keyword(null,"content-parts","content-parts",684529019),merged_content_parts);
} else {
return G__65997;
}
})())));
knoxx.backend.session_store.complete_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,conversation_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"completed",new cljs.core.Keyword(null,"answer","answer",-742633163),answer__$1,new cljs.core.Keyword(null,"messages","messages",345434482),final_messages_66041], null));

knoxx.backend.run_state.clear_event_stream_sink_BANG_();

knoxx.backend.agent_runtime.remove_agent_session_BANG_(conversation_id);

return response;
});
knoxx.backend.agents.turn.finalize_turn_failure_BANG_ = (function knoxx$backend$agents$turn$finalize_turn_failure_BANG_(config,state,session,run_id,conversation_id,session_id,started_ms,hydration,memory_hydration,persisted_request_messages,agent_spec,err){
var err_text = (function (){var or__5142__auto__ = cljs.core.deref(new cljs.core.Keyword(null,"abort-reason*","abort-reason*",-962330650).cljs$core$IFn$_invoke$arity$1(state));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})();
var error_event = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,"run_failed",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),err_text], null));
knoxx.backend.run_state.finalize_run_trace_blocks_BANG_(run_id,"error");

var failed_run_66042 = knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
var resource_patch = (function (){var G__65998 = cljs.core.PersistentArrayMap.EMPTY;
var G__65998__$1 = (cljs.core.truth_(hydration)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65998,new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907),cljs.core.select_keys(hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"tokens","tokens",-818939304),new cljs.core.Keyword(null,"database","database",1849087575),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"results","results",-1134170113)], null))):G__65998);
if(cljs.core.truth_(memory_hydration)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__65998__$1,new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759),cljs.core.select_keys(memory_hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"hits","hits",-2120002930),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996)], null)));
} else {
return G__65998__$1;
}
})();
return cljs.core.update.cljs$core$IFn$_invoke$arity$4(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(run,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),(Date.now() - started_ms),new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.deref(new cljs.core.Keyword(null,"reasoning-chunks","reasoning-chunks",-526618091).cljs$core$IFn$_invoke$arity$1(state))),new cljs.core.Keyword(null,"error","error",-978969032),err_text], 0)),new cljs.core.Keyword(null,"resources","resources",1632806811),cljs.core.merge,resource_patch);
}));
var __66043 = (cljs.core.truth_(failed_run_66042)?knoxx.backend.openplanner_memory.index_run_memory_BANG_(config,failed_run_66042,knoxx.backend.core_memory.extract_mentioned_devel_paths,knoxx.backend.core_memory.extract_mentioned_urls):null);
knoxx.backend.run_state.append_run_event_BANG_(run_id,error_event);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",error_event);

var final_messages_66045 = knoxx.backend.agent_runtime.prune_session_messages(agent_spec,knoxx.backend.agents.transcript.transcript_after_turn(session,persisted_request_messages));
knoxx.backend.session_store.complete_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,conversation_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),err_text,new cljs.core.Keyword(null,"messages","messages",345434482),final_messages_66045], null));

knoxx.backend.run_state.clear_event_stream_sink_BANG_();

knoxx.backend.agent_runtime.remove_agent_session_BANG_(conversation_id);

throw err;
});
knoxx.backend.agents.turn.prompt_and_await_BANG_ = (function knoxx$backend$agents$turn$prompt_and_await_BANG_(runtime,config,session_id,run_id,conversation_id,started_ms,model_id,mode,session,message,prompt_content_parts,hydration,memory_hydration,persisted_request_messages,agent_spec){
var content_part_type = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_content_part_type(part){
if((new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) instanceof cljs.core.Keyword)){
return cljs.core.name(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part));
} else {
if(typeof new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) === 'string'){
return new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part);
} else {
return null;

}
}
});
var data_url__GT_image_attachment = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_data_url__GT_image_attachment(raw){
if(((typeof raw === 'string') && (clojure.string.starts_with_QMARK_(raw,"data:")))){
var vec__66006 = clojure.string.split.cljs$core$IFn$_invoke$arity$3(raw,/,/,(2));
var meta = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__66006,(0),null);
var b64 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__66006,(1),null);
var meta__$1 = (function (){var or__5142__auto__ = meta;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var mime = (function (){var G__66009 = meta__$1;
var G__66009__$1 = (((G__66009 == null))?null:clojure.string.replace_first(G__66009,/^data:/,""));
var G__66009__$2 = (((G__66009__$1 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$3(G__66009__$1,/;/,(2)));
var G__66009__$3 = (((G__66009__$2 == null))?null:cljs.core.first(G__66009__$2));
var G__66009__$4 = (((G__66009__$3 == null))?null:clojure.string.trim(G__66009__$3));
if((G__66009__$4 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__66009__$4);
}
})();
var b64__$1 = knoxx.backend.agents.content.nonblank(b64);
if(cljs.core.truth_(b64__$1)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"data","data",-232669377),b64__$1,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime], null);
} else {
return null;
}
} else {
return null;
}
});
var strip_data_url = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_strip_data_url(raw_data){
var temp__5825__auto__ = knoxx.backend.agents.content.nonblank(raw_data);
if(cljs.core.truth_(temp__5825__auto__)){
var data = temp__5825__auto__;
var comma = data.indexOf(",");
if(((clojure.string.starts_with_QMARK_(data,"data:")) && ((comma >= (0))))){
return data.slice((comma + (1)));
} else {
return data;
}
} else {
return null;
}
});
var base64_bytes = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_base64_bytes(b64){
var temp__5825__auto__ = knoxx.backend.agents.content.nonblank(b64);
if(cljs.core.truth_(temp__5825__auto__)){
var b64__$1 = temp__5825__auto__;
var len = ((b64__$1).length);
var padding = ((clojure.string.ends_with_QMARK_(b64__$1,"=="))?(2):((clojure.string.ends_with_QMARK_(b64__$1,"="))?(1):(0)
));
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),(Math.floor(((3) * (len / (4)))) - padding));
} else {
return null;
}
});
var media_part__GT_eta_mu_attachment = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_media_part__GT_eta_mu_attachment(part){
var part_type = content_part_type(part);
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["image",null,"video",null,"document",null,"audio",null], null), null),part_type)){
var raw_data = knoxx.backend.agents.content.nonblank(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part));
var parsed = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("image",part_type))?data_url__GT_image_attachment(raw_data):null);
var data = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return strip_data_url(raw_data);
}
})();
var mime_type = knoxx.backend.agents.content.nonblank((function (){var G__66010 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(parsed);
}
})();
if((G__66010 == null)){
return null;
} else {
return cljs.core.name(G__66010);
}
})());
var filename = knoxx.backend.agents.content.nonblank(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part));
if(cljs.core.truth_(data)){
var G__66011 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),part_type,new cljs.core.Keyword(null,"data","data",-232669377),data], null);
var G__66011__$1 = (cljs.core.truth_(mime_type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__66011,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type):G__66011);
if(cljs.core.truth_((function (){var and__5140__auto__ = filename;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(part_type,"audio");
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__66011__$1,new cljs.core.Keyword(null,"filename","filename",-1428840783),filename);
} else {
return G__66011__$1;
}
} else {
return null;
}
} else {
return null;
}
});
var file_processor_style_marker = (function knoxx$backend$agents$turn$prompt_and_await_BANG__$_file_processor_style_marker(media_part){
var t = content_part_type(media_part);
var mime = (function (){var or__5142__auto__ = knoxx.backend.agents.content.nonblank(new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(media_part));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"audio"))?"audio/mpeg":null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"image")){
return "image/png";
} else {
return null;
}
}
}
})();
var name = (function (){var G__66012 = t;
switch (G__66012) {
case "audio":
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mime,"audio/wav")){
return "attached-audio.wav";
} else {
return "attached-audio.mp3";
}

break;
case "image":
return "attached-image";

break;
case "video":
return "attached-video";

break;
case "document":
return "attached-document";

break;
default:
return (""+"attached-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(t));

}
})();
var bytes = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"size","size",1098693007).cljs$core$IFn$_invoke$arity$1(media_part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return base64_bytes(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(media_part));
}
})();
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"audio")){
return (""+"<file name=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"\">[Audio attached: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime)+", "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = bytes;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "?";
}
})())+" bytes.]</file>\n");
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"image")){
return (""+"<file name=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"\"></file>\n");
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"video")){
return (""+"<file name=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"\">[Video attached: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime)+", "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = bytes;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "?";
}
})())+" bytes.]</file>\n");
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(t,"document")){
return (""+"<file name=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"\">[Document attached: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime)+", "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = bytes;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "?";
}
})())+" bytes.]</file>\n");
} else {
return "";

}
}
}
}
});
var state = knoxx.backend.agents.stream.make_stream_state(run_id,conversation_id,session_id,knoxx.backend.util.time.now_iso(),started_ms,shadow.esm.esm_import$node_crypto);
var abort_BANG_ = (function (reason){
return knoxx.backend.agents.stream.request_abort_BANG_(state,session,reason);
});
var _registered = knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$3(state,abort_BANG_,agent_spec);
var unsubscribe = session.subscribe(knoxx.backend.agents.stream.build_subscribe_handler(state,session));
var parts = (function (){var or__5142__auto__ = prompt_content_parts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var media_parts = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(media_part__GT_eta_mu_attachment,parts));
var omitted_count = cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),(cljs.core.count(parts) - cljs.core.count(media_parts)));
var turn_message = (function (){var or__5142__auto__ = knoxx.backend.agents.content.nonblank(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank(new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var attachment_markers = ((cljs.core.seq(media_parts))?cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.map.cljs$core$IFn$_invoke$arity$2(file_processor_style_marker,media_parts)):null);
var base_text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = attachment_markers;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agent_hydration.build_agent_user_message(turn_message,hydration,memory_hydration)));
var final_text = (function (){var G__66013 = base_text;
if((omitted_count > (0))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66013)+"\n\n"+"[Note: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(omitted_count)+" unsupported attachment(s) were omitted for this model/runtime.]");
} else {
return G__66013;
}
})();
var content = ((cljs.core.seq(media_parts))?cljs.core.clj__GT_js(cljs.core.conj.cljs$core$IFn$_invoke$arity$2(media_parts,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),final_text], null))):final_text);
var hash_data_66049 = (function (s){
return (""+"[img:sha="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Buffer.from(s,"base64").toString("hex").slice((0),(12)))+" len="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(s))+"]");
});
var safe_part_66050 = (function (p){
var m = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(p,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.count(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(m)) > (64));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.clj__GT_js(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(m,new cljs.core.Keyword(null,"data","data",-232669377),hash_data_66049(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(m))));
} else {
return p;
}
});
var log_content_66051 = (cljs.core.truth_(cljs.core.array_QMARK_(content))?cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(safe_part_66050,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content))):content);
console.log("[prompt-and-await!]",JSON.stringify(({"parts_count": cljs.core.count(parts), "content": log_content_66051, "run_id": run_id, "mode": mode, "session_id": session_id, "media_parts_count": cljs.core.count(media_parts), "conversation_id": conversation_id, "content_type": (cljs.core.truth_(cljs.core.array_QMARK_(content))?"multipart":"text"), "omitted_count": omitted_count, "model_id": model_id})));

knoxx.backend.agent_context.set_context_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),run_id,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),agent_spec], null));

return session.sendUserMessage(content).then((function (_){
knoxx.backend.agent_context.clear_context_BANG_();

(unsubscribe.cljs$core$IFn$_invoke$arity$0 ? unsubscribe.cljs$core$IFn$_invoke$arity$0() : unsubscribe.call(null));

return knoxx.backend.agents.turn.finalize_turn_success_BANG_(config,state,session,run_id,conversation_id,session_id,started_ms,model_id,mode,hydration,memory_hydration,persisted_request_messages,agent_spec);
})).catch((function (err){
knoxx.backend.agent_context.clear_context_BANG_();

(unsubscribe.cljs$core$IFn$_invoke$arity$0 ? unsubscribe.cljs$core$IFn$_invoke$arity$0() : unsubscribe.call(null));

knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2(conversation_id,run_id);

return knoxx.backend.agents.turn.finalize_turn_failure_BANG_(config,state,session,run_id,conversation_id,session_id,started_ms,hydration,memory_hydration,persisted_request_messages,agent_spec,err);
}));
});
knoxx.backend.agents.turn.send_agent_turn_BANG_ = (function knoxx$backend$agents$turn$send_agent_turn_BANG_(runtime,config,p__66015){
var map__66016 = p__66015;
var map__66016__$1 = cljs.core.__destructure_map(map__66016);
var content_parts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"content-parts","content-parts",684529019));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"model","model",331153215));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var auth_context = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"auth-context","auth-context",320032325));
var template_context = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"template-context","template-context",-946500342));
var mode = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"mode","mode",654403691));
var thinking_level = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953));
var agent_spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66016__$1,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541));
return (new Promise((function (resolve,reject){
try{var G__66018 = (function (){var conversation_id__$1 = (function (){var or__5142__auto__ = conversation_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_crypto.randomUUID();
}
})();
var session_id__$1 = knoxx.backend.agents.turn.ensure_session_id(shadow.esm.esm_import$node_crypto,session_id);
var auth_context__$1 = knoxx.backend.agents.turn.auth_context_for_agent_turn(auth_context,agent_spec);
var _ = knoxx.backend.agents.turn.ensure_conversation_access_BANG_(auth_context__$1,conversation_id__$1);
var ___$1 = knoxx.backend.agents.turn.remember_conversation_access_BANG_(auth_context__$1,conversation_id__$1);
var mode__$1 = (function (){var or__5142__auto__ = mode;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "direct";
}
})();
var requested_model = (function (){var or__5142__auto__ = model;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var model_id = (function (){var or__5142__auto__ = requested_model;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(knoxx.backend.agent_hydration.ensure_settings_BANG_(config));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config);
}
}
})();
var thinking_level_raw = (function (){var or__5142__auto__ = thinking_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var parsed_thinking_level = (cljs.core.truth_(thinking_level_raw)?knoxx.backend.runtime.models.normalize_thinking_level(thinking_level_raw):null);
var thinking_level__$1 = knoxx.backend.runtime.models.effective_thinking_level(config,model_id,(function (){var or__5142__auto__ = parsed_thinking_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = thinking_level_raw;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "off";
}
}
}
})());
var run_id__$1 = (function (){var or__5142__auto__ = run_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_crypto.randomUUID();
}
})();
var started_at = knoxx.backend.util.time.now_iso();
var started_ms = Date.now();
var existing_messages = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(knoxx.backend.session_store.get_session_sync(session_id__$1));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var seeded_messages = knoxx.backend.agent_runtime.prune_session_messages(agent_spec,cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__66014_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__66014_SHARP_,new cljs.core.Keyword(null,"content-parts","content-parts",684529019));
}),knoxx.backend.agents.transcript.ensure_system_message(existing_messages,agent_spec)));
var auth_extra = knoxx.backend.authz.auth_snapshot(auth_context__$1);
if(cljs.core.truth_((function (){var and__5140__auto__ = thinking_level_raw;
if(cljs.core.truth_(and__5140__auto__)){
return (parsed_thinking_level == null);
} else {
return and__5140__auto__;
}
})())){
return Promise.reject((new Error((""+"Unsupported thinking level: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(thinking_level_raw)+". Expected one of off, minimal, low, medium, high, xhigh."))));
} else {
return knoxx.backend.agents.policy.enforce_chat_policy_BANG_(auth_context__$1,model_id).then((function (___$2){
knoxx.backend.session_titles.maybe_prime_session_title_BANG_(runtime,config,conversation_id__$1,message);

var max_bytes = (32000000);
var looks_like_url_QMARK_ = (function (value){
return ((typeof value === 'string') && (((clojure.string.starts_with_QMARK_(value,"http://")) || (clojure.string.starts_with_QMARK_(value,"https://")))));
});
var media_url_QMARK_ = (function (value){
return ((typeof value === 'string') && (((looks_like_url_QMARK_(value)) || (clojure.string.starts_with_QMARK_(value,"/")))));
});
var resolve_media_url = (function (value){
if((!(typeof value === 'string'))){
return value;
} else {
if(clojure.string.starts_with_QMARK_(value,"/")){
return (""+"http://127.0.0.1:8000"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
} else {
return value;

}
}
});
var studio_stream_path = (function (value){
try{var u = (new URL(resolve_media_url(value)));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(u.pathname,"/api/studio/stream")){
var G__66020 = u.searchParams;
var G__66020__$1 = (((G__66020 == null))?null:G__66020.get("path"));
var G__66020__$2 = (((G__66020__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66020__$1)));
var G__66020__$3 = (((G__66020__$2 == null))?null:clojure.string.trim(G__66020__$2));
if((G__66020__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66020__$3);
}
} else {
return null;
}
}catch (e66019){var ___$3 = e66019;
return null;
}});
var read_workspace_media_data_url_BANG_ = (function (raw_path,fallback_mime,label){
var normalized = knoxx.backend.tools.media.normalize_tool_path_arg(raw_path);
var map__66021 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__66021__$1 = cljs.core.__destructure_map(map__66021);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66021__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__66021__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var mime = (function (){var or__5142__auto__ = knoxx.backend.tools.media.workspace_media_mime_type(relative);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return fallback_mime;
}
})();
return knoxx.backend.tools.media.fs_stat_BANG_(shadow.esm.esm_import$node_fs$promises,absolute).then((function (stat){
if(cljs.core.truth_(stat.isFile())){
} else {
throw (new Error((""+"Attached "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" is not a file")));
}

var size = stat.size;
if((size > max_bytes)){
throw (new Error((""+"Attached "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" exceeds max bytes: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(size)+"; max="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max_bytes))));
} else {
}

return shadow.esm.esm_import$node_fs$promises.readFile(absolute);
})).then((function (buf){
return (""+"data:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime)+";base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(buf.toString("base64")));
}));
});
var data_url_QMARK_ = (function (value){
return ((typeof value === 'string') && (clojure.string.starts_with_QMARK_(value,"data:")));
});
var content_part_type = (function (part){
if((new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) instanceof cljs.core.Keyword)){
return cljs.core.name(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part));
} else {
if(typeof new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) === 'string'){
return new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part);
} else {
return null;

}
}
});
var fetch_media_data_url_BANG_ = (function (url,fallback_mime,label){
var temp__5823__auto__ = studio_stream_path(url);
if(cljs.core.truth_(temp__5823__auto__)){
var stream_path = temp__5823__auto__;
return read_workspace_media_data_url_BANG_(stream_path,fallback_mime,label);
} else {
var url__$1 = resolve_media_url(url);
var headers = ({});
var auth_email = (function (){var or__5142__auto__ = (function (){var G__66022 = auth_context__$1;
var G__66022__$1 = (((G__66022 == null))?null:new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(G__66022));
var G__66022__$2 = (((G__66022__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66022__$1)));
var G__66022__$3 = (((G__66022__$2 == null))?null:clojure.string.trim(G__66022__$2));
if((G__66022__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66022__$3);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__66023 = auth_context__$1;
var G__66023__$1 = (((G__66023 == null))?null:new cljs.core.Keyword(null,"user-email","user-email",2126479881).cljs$core$IFn$_invoke$arity$1(G__66023));
var G__66023__$2 = (((G__66023__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66023__$1)));
var G__66023__$3 = (((G__66023__$2 == null))?null:clojure.string.trim(G__66023__$2));
if((G__66023__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66023__$3);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__66024 = auth_context__$1;
var G__66024__$1 = (((G__66024 == null))?null:new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(G__66024));
var G__66024__$2 = (((G__66024__$1 == null))?null:new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(G__66024__$1));
var G__66024__$3 = (((G__66024__$2 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66024__$2)));
var G__66024__$4 = (((G__66024__$3 == null))?null:clojure.string.trim(G__66024__$3));
if((G__66024__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__66024__$4);
}
}
}
})();
var auth_org_slug = (function (){var or__5142__auto__ = (function (){var G__66025 = auth_context__$1;
var G__66025__$1 = (((G__66025 == null))?null:new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998).cljs$core$IFn$_invoke$arity$1(G__66025));
var G__66025__$2 = (((G__66025__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66025__$1)));
var G__66025__$3 = (((G__66025__$2 == null))?null:clojure.string.trim(G__66025__$2));
if((G__66025__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66025__$3);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__66026 = auth_context__$1;
var G__66026__$1 = (((G__66026 == null))?null:new cljs.core.Keyword(null,"org-slug","org-slug",-726595051).cljs$core$IFn$_invoke$arity$1(G__66026));
var G__66026__$2 = (((G__66026__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66026__$1)));
var G__66026__$3 = (((G__66026__$2 == null))?null:clojure.string.trim(G__66026__$2));
if((G__66026__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66026__$3);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__66027 = auth_context__$1;
var G__66027__$1 = (((G__66027 == null))?null:new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(G__66027));
var G__66027__$2 = (((G__66027__$1 == null))?null:new cljs.core.Keyword(null,"slug","slug",2029314850).cljs$core$IFn$_invoke$arity$1(G__66027__$1));
var G__66027__$3 = (((G__66027__$2 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66027__$2)));
var G__66027__$4 = (((G__66027__$3 == null))?null:clojure.string.trim(G__66027__$3));
if((G__66027__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__66027__$4);
}
}
}
})();
var auth_membership_id = (function (){var or__5142__auto__ = (function (){var G__66028 = auth_context__$1;
var G__66028__$1 = (((G__66028 == null))?null:new cljs.core.Keyword(null,"membershipId","membershipId",2026001076).cljs$core$IFn$_invoke$arity$1(G__66028));
var G__66028__$2 = (((G__66028__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66028__$1)));
var G__66028__$3 = (((G__66028__$2 == null))?null:clojure.string.trim(G__66028__$2));
if((G__66028__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66028__$3);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__66029 = auth_context__$1;
var G__66029__$1 = (((G__66029 == null))?null:new cljs.core.Keyword(null,"membership-id","membership-id",-723542492).cljs$core$IFn$_invoke$arity$1(G__66029));
var G__66029__$2 = (((G__66029__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66029__$1)));
var G__66029__$3 = (((G__66029__$2 == null))?null:clojure.string.trim(G__66029__$2));
if((G__66029__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__66029__$3);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__66030 = auth_context__$1;
var G__66030__$1 = (((G__66030 == null))?null:new cljs.core.Keyword(null,"membership","membership",254556333).cljs$core$IFn$_invoke$arity$1(G__66030));
var G__66030__$2 = (((G__66030__$1 == null))?null:new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__66030__$1));
var G__66030__$3 = (((G__66030__$2 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66030__$2)));
var G__66030__$4 = (((G__66030__$3 == null))?null:clojure.string.trim(G__66030__$3));
if((G__66030__$4 == null)){
return null;
} else {
return cljs.core.not_empty(G__66030__$4);
}
}
}
})();
var local_knoxx_url_QMARK_ = ((typeof url__$1 === 'string') && (((clojure.string.starts_with_QMARK_(url__$1,"http://127.0.0.1:8000/")) || (((clojure.string.starts_with_QMARK_(url__$1,"http://localhost:8000/")) || (((clojure.string.starts_with_QMARK_(url__$1,"http://0.0.0.0:8000/")) || (((clojure.string.starts_with_QMARK_(url__$1,"http://knoxx.promethean.rest/")) || (clojure.string.starts_with_QMARK_(url__$1,"https://knoxx.promethean.rest/")))))))))));
if(cljs.core.truth_((function (){var and__5140__auto__ = local_knoxx_url_QMARK_;
if(and__5140__auto__){
return auth_email;
} else {
return and__5140__auto__;
}
})())){
(headers["x-knoxx-user-email"] = auth_email);
} else {
}

if(cljs.core.truth_((function (){var and__5140__auto__ = local_knoxx_url_QMARK_;
if(and__5140__auto__){
return auth_org_slug;
} else {
return and__5140__auto__;
}
})())){
(headers["x-knoxx-org-slug"] = auth_org_slug);
} else {
}

if(cljs.core.truth_((function (){var and__5140__auto__ = local_knoxx_url_QMARK_;
if(and__5140__auto__){
return auth_membership_id;
} else {
return and__5140__auto__;
}
})())){
(headers["x-knoxx-membership-id"] = auth_membership_id);
} else {
}

return fetch(url__$1,({"method": "GET", "headers": headers})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
} else {
throw (new Error((""+"Failed to fetch "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+": HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status))));
}

var len_h = (function (){var G__66031 = resp;
var G__66031__$1 = (((G__66031 == null))?null:G__66031.headers);
if((G__66031__$1 == null)){
return null;
} else {
return G__66031__$1.get("content-length");
}
})();
var len = (cljs.core.truth_(len_h)?parseInt(len_h,(10)):null);
if(((typeof len === 'number') && ((((len > (0))) && ((len > max_bytes)))))){
throw (new Error((""+"Remote "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" exceeds max bytes: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(len))));
} else {
}

return resp.arrayBuffer().then((function (ab){
var buf = Buffer.from(ab);
var size = buf.length;
var ___$3 = (((size > max_bytes))?(function(){throw (new Error((""+"Remote "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" exceeds max bytes: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(size))))})():null);
var mime = (function (){var or__5142__auto__ = (function (){var G__66032 = resp;
var G__66032__$1 = (((G__66032 == null))?null:G__66032.headers);
if((G__66032__$1 == null)){
return null;
} else {
return G__66032__$1.get("content-type");
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return fallback_mime;
}
})();
var b64 = buf.toString("base64");
return (""+"data:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime)+";base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b64));
}));
}));
}
});
var materialize_part_BANG_ = (function (part){
var part_type = content_part_type(part);
if(cljs.core.not((function (){var fexpr__66033 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["image",null,"audio",null], null), null);
return (fexpr__66033.cljs$core$IFn$_invoke$arity$1 ? fexpr__66033.cljs$core$IFn$_invoke$arity$1(part_type) : fexpr__66033.call(null,part_type));
})())){
return Promise.resolve(part);
} else {
if(data_url_QMARK_(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part))){
return Promise.resolve(part);
} else {
if(media_url_QMARK_(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part))){
return fetch_media_data_url_BANG_(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(part_type,"audio"))?"audio/mpeg":"image/png"),part_type).then((function (data_url){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(part,new cljs.core.Keyword(null,"url","url",276297046)),new cljs.core.Keyword(null,"data","data",-232669377),data_url);
}));
} else {
if(media_url_QMARK_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part))){
return fetch_media_data_url_BANG_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(part_type,"audio"))?"audio/mpeg":"image/png"),part_type).then((function (data_url){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(part,new cljs.core.Keyword(null,"url","url",276297046)),new cljs.core.Keyword(null,"data","data",-232669377),data_url);
}));
} else {
return Promise.resolve(part);

}
}
}
}
});
var materialize_content_parts_BANG_ = (function (parts){
var parts__$1 = cljs.core.vec((function (){var or__5142__auto__ = parts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var should_materialize_QMARK_ = cljs.core.some((function (part){
var part_type = content_part_type(part);
var and__5140__auto__ = part_type;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.runtime.models.model_supports_input_QMARK_(config,model_id,part_type);
} else {
return and__5140__auto__;
}
}),parts__$1);
if(cljs.core.not((function (){var and__5140__auto__ = cljs.core.seq(parts__$1);
if(and__5140__auto__){
return should_materialize_QMARK_;
} else {
return and__5140__auto__;
}
})())){
return Promise.resolve(parts__$1);
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2(materialize_part_BANG_,parts__$1))).then((function (items){
return cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(items));
}));
}
});
return Promise.all([knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$5(runtime,config,mode__$1,message,auth_context__$1),knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$5(config,conversation_id__$1,message,auth_context__$1,agent_spec),materialize_content_parts_BANG_(content_parts),knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id__$1,model_id,auth_context__$1,thinking_level__$1,session_id__$1,agent_spec)]).then((function (results){
var hydration = (results[(0)]);
var memory_hydration = (results[(1)]);
var materialized_content_parts = cljs.core.vec((function (){var or__5142__auto__ = (results[(2)]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var session = (results[(3)]);
var turn_message = (function (){var or__5142__auto__ = knoxx.backend.agents.content.nonblank(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.turn.render_task_prompt.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(agent_spec),auth_context__$1,template_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var user_message = ((cljs.core.seq(materialized_content_parts))?new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"role","role",-736691072),"user",new cljs.core.Keyword(null,"content","content",15833224),turn_message,new cljs.core.Keyword(null,"content-parts","content-parts",684529019),materialized_content_parts], null):new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"user",new cljs.core.Keyword(null,"content","content",15833224),turn_message], null));
var prompt_content_parts = knoxx.backend.agents.content.model_ready_content_parts(config,model_id,materialized_content_parts);
var request_messages = knoxx.backend.agent_runtime.prune_session_messages(agent_spec,cljs.core.conj.cljs$core$IFn$_invoke$arity$2(seeded_messages,user_message));
knoxx.backend.agents.turn.create_initial_run_BANG_(run_id__$1,session_id__$1,conversation_id__$1,started_at,model_id,mode__$1,thinking_level__$1,agent_spec,auth_extra,request_messages,config);

if(cljs.core.truth_(hydration)){
var hydration_event_66058 = knoxx.backend.run_state.tool_event_payload(run_id__$1,conversation_id__$1,session_id__$1,"passive_hydration",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"ok",new cljs.core.Keyword(null,"hits","hits",-2120002930),cljs.core.count(new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(hydration)),new cljs.core.Keyword(null,"elapsed_ms","elapsed_ms",-325114493),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486).cljs$core$IFn$_invoke$arity$1(hydration)], null));
knoxx.backend.run_state.update_run_BANG_(run_id__$1,(function (run){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$4(run,new cljs.core.Keyword(null,"resources","resources",1632806811),cljs.core.merge,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907),cljs.core.select_keys(hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"tokens","tokens",-818939304),new cljs.core.Keyword(null,"database","database",1849087575),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"results","results",-1134170113)], null))], null)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso());
}));

knoxx.backend.run_state.append_run_event_BANG_(run_id__$1,hydration_event_66058);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id__$1,"events",hydration_event_66058);
} else {
}

if(cljs.core.seq(new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(memory_hydration))){
var memory_event_66059 = knoxx.backend.run_state.tool_event_payload(run_id__$1,conversation_id__$1,session_id__$1,"memory_hydration",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"ok",new cljs.core.Keyword(null,"hits","hits",-2120002930),cljs.core.count(new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(memory_hydration)),new cljs.core.Keyword(null,"elapsed_ms","elapsed_ms",-325114493),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486).cljs$core$IFn$_invoke$arity$1(memory_hydration)], null));
knoxx.backend.run_state.update_run_BANG_(run_id__$1,(function (run){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$4(run,new cljs.core.Keyword(null,"resources","resources",1632806811),cljs.core.merge,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759),cljs.core.select_keys(memory_hydration,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"hits","hits",-2120002930),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996)], null))], null)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso());
}));

knoxx.backend.run_state.append_run_event_BANG_(run_id__$1,memory_event_66059);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id__$1,"events",memory_event_66059);
} else {
}

var persisted_request_messages = knoxx.backend.agent_runtime.prune_session_messages(agent_spec,knoxx.backend.agents.transcript.transcript_before_prompt(session,user_message,agent_spec));
knoxx.backend.session_store.update_session_BANG_(knoxx.backend.redis_client.get_client(),session_id__$1,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"messages","messages",345434482),persisted_request_messages,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id__$1,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id__$1], null));

return knoxx.backend.agents.turn.prompt_and_await_BANG_(runtime,config,session_id__$1,run_id__$1,conversation_id__$1,started_ms,model_id,mode__$1,session,turn_message,prompt_content_parts,hydration,memory_hydration,persisted_request_messages,agent_spec);
}));
}));

}
})();
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__66018) : resolve.call(null,G__66018));
}catch (e66017){var e_SHARP_ = e66017;
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(e_SHARP_) : reject.call(null,e_SHARP_));
}})));
});

//# sourceMappingURL=knoxx.backend.agents.turn.js.map
