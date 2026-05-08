import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.app_shapes');
knoxx.backend.app_shapes.media_extension_pattern = /.*\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mp3|wav|ogg|m4a|flac|pdf)(?:\?.*)?$/;
/**
 * Extract media URLs from text content.
 */
knoxx.backend.app_shapes.extract_media_urls = (function knoxx$backend$app_shapes$extract_media_urls(text){
if(typeof text === 'string'){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (token){
var lower = clojure.string.lower_case(token);
if(cljs.core.truth_((function (){var or__5142__auto__ = cljs.core.re_matches(knoxx.backend.app_shapes.media_extension_pattern,lower);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.some((function (p1__69583_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__69583_SHARP_);
}),new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [".png",".jpg",".jpeg",".gif",".webp",".mp4",".webm",".mp3",".wav",".ogg",".m4a",".flac",".pdf"], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return clojure.string.includes_QMARK_(token,"cdn.discordapp.com");
}
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"url","url",276297046),token,new cljs.core.Keyword(null,"type","type",1174270348),(cljs.core.truth_(cljs.core.some((function (p1__69584_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__69584_SHARP_);
}),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [".png",".jpg",".jpeg",".gif",".webp"], null)))?"image":(cljs.core.truth_(cljs.core.some((function (p1__69585_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__69585_SHARP_);
}),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [".mp4",".webm",".mov"], null)))?"video":(cljs.core.truth_(cljs.core.some((function (p1__69586_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__69586_SHARP_);
}),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [".mp3",".wav",".ogg",".m4a",".flac"], null)))?"audio":(cljs.core.truth_(cljs.core.some((function (p1__69587_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__69587_SHARP_);
}),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [".pdf"], null)))?"document":"image"
))))], null);
} else {
return null;
}
}),clojure.string.split.cljs$core$IFn$_invoke$arity$2(text,/\s+/)));
} else {
return null;
}
});
/**
 * Auto-detect media URLs in message text and add them as content parts.
 */
knoxx.backend.app_shapes.auto_detect_content_parts = (function knoxx$backend$app_shapes$auto_detect_content_parts(message,content_parts){
var extracted = ((typeof message === 'string')?knoxx.backend.app_shapes.extract_media_urls(message):null);
var existing_urls = cljs.core.set(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"url","url",276297046),(function (){var or__5142__auto__ = content_parts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
var new_parts = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__69588_SHARP_){
var G__69589 = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p1__69588_SHARP_);
return (existing_urls.cljs$core$IFn$_invoke$arity$1 ? existing_urls.cljs$core$IFn$_invoke$arity$1(G__69589) : existing_urls.call(null,G__69589));
}),extracted));
if(cljs.core.seq(new_parts)){
return cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(content_parts,new_parts));
} else {
return content_parts;
}
});
knoxx.backend.app_shapes.normalize_tool_policy = (function knoxx$backend$app_shapes$normalize_tool_policy(policy){
var tool_id = (function (){var G__69590 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-id","tool-id",-290456894).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"tool_id","tool_id",1550520216).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})();
var G__69590__$1 = (((G__69590 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69590)));
var G__69590__$2 = (((G__69590__$1 == null))?null:clojure.string.trim(G__69590__$1));
if((G__69590__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69590__$2);
}
})();
var effect = (function (){var G__69591 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "allow";
}
})();
var G__69591__$1 = (((G__69591 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69591)));
var G__69591__$2 = (((G__69591__$1 == null))?null:clojure.string.trim(G__69591__$1));
var G__69591__$3 = (((G__69591__$2 == null))?null:clojure.string.lower_case(G__69591__$2));
if((G__69591__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__69591__$3);
}
})();
if(cljs.core.truth_(tool_id)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),(cljs.core.truth_((function (){var fexpr__69592 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["allow",null,"deny",null], null), null);
return (fexpr__69592.cljs$core$IFn$_invoke$arity$1 ? fexpr__69592.cljs$core$IFn$_invoke$arity$1(effect) : fexpr__69592.call(null,effect));
})())?effect:"allow")], null);
} else {
return null;
}
});
knoxx.backend.app_shapes.normalize_tool_policies = (function knoxx$backend$app_shapes$normalize_tool_policies(policies){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.app_shapes.normalize_tool_policy,(function (){var or__5142__auto__ = policies;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
});
knoxx.backend.app_shapes.memory_hydration_spec = (function knoxx$backend$app_shapes$memory_hydration_spec(spec){
var or__5142__auto__ = new cljs.core.Keyword(null,"memory_hydration","memory_hydration",-1458677455).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passive-hydration","passive-hydration",-1337823895)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907)], null));
}
}
}
}
});
knoxx.backend.app_shapes.context_policy_spec = (function knoxx$backend$app_shapes$context_policy_spec(spec){
var or__5142__auto__ = new cljs.core.Keyword(null,"context_policy","context_policy",1230169154).cljs$core$IFn$_invoke$arity$1(spec);
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
});
knoxx.backend.app_shapes.normalize_agent_spec = (function knoxx$backend$app_shapes$normalize_agent_spec(value){
var spec = (function (){var G__69593 = value;
if((G__69593 == null)){
return null;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(G__69593,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}
})();
var contract_id = (function (){var G__69594 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193).cljs$core$IFn$_invoke$arity$1(spec);
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
var G__69594__$1 = (((G__69594 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69594)));
var G__69594__$2 = (((G__69594__$1 == null))?null:clojure.string.trim(G__69594__$1));
if((G__69594__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69594__$2);
}
})();
var actor_id = (function (){var G__69595 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(spec);
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
var G__69595__$1 = (((G__69595 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69595)));
var G__69595__$2 = (((G__69595__$1 == null))?null:clojure.string.trim(G__69595__$1));
if((G__69595__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69595__$2);
}
})();
var role = (function (){var G__69596 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role_slug","role_slug",219656703).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"role-slug","role-slug",-617706766).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})();
var G__69596__$1 = (((G__69596 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69596)));
var G__69596__$2 = (((G__69596__$1 == null))?null:clojure.string.trim(G__69596__$1));
if((G__69596__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69596__$2);
}
})();
var system_prompt = (function (){var G__69597 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"system_prompt","system_prompt",-655033954).cljs$core$IFn$_invoke$arity$1(spec);
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
var G__69597__$1 = (((G__69597 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69597)));
if((G__69597__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__69597__$1);
}
})();
var task_prompt = (function (){var G__69598 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"task_prompt","task_prompt",1276696196).cljs$core$IFn$_invoke$arity$1(spec);
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
var G__69598__$1 = (((G__69598 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69598)));
if((G__69598__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__69598__$1);
}
})();
var model = (function (){var G__69599 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(spec);
var G__69599__$1 = (((G__69599 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69599)));
var G__69599__$2 = (((G__69599__$1 == null))?null:clojure.string.trim(G__69599__$1));
if((G__69599__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69599__$2);
}
})();
var thinking_level = (function (){var G__69600 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069).cljs$core$IFn$_invoke$arity$1(spec);
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
var G__69600__$1 = (((G__69600 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69600)));
var G__69600__$2 = (((G__69600__$1 == null))?null:clojure.string.trim(G__69600__$1));
if((G__69600__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69600__$2);
}
})();
var tool_policies = knoxx.backend.app_shapes.normalize_tool_policies((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(spec);
}
}
})());
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
var memory_hydration = knoxx.backend.app_shapes.memory_hydration_spec(spec);
var context_policy = knoxx.backend.app_shapes.context_policy_spec(spec);
if(cljs.core.truth_((function (){var or__5142__auto__ = contract_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = actor_id;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = role;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = system_prompt;
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = task_prompt;
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = model;
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = thinking_level;
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var or__5142__auto____$7 = cljs.core.seq(tool_policies);
if(or__5142__auto____$7){
return or__5142__auto____$7;
} else {
var or__5142__auto____$8 = resource_policies;
if(cljs.core.truth_(or__5142__auto____$8)){
return or__5142__auto____$8;
} else {
var or__5142__auto____$9 = memory_hydration;
if(cljs.core.truth_(or__5142__auto____$9)){
return or__5142__auto____$9;
} else {
return context_policy;
}
}
}
}
}
}
}
}
}
}
})())){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082),new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),new cljs.core.Keyword(null,"resource-policies","resource-policies",-1726016874),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.Keyword(null,"model","model",331153215)],[role,memory_hydration,task_prompt,context_policy,actor_id,thinking_level,contract_id,system_prompt,resource_policies,tool_policies,model]);
} else {
return null;
}
});
knoxx.backend.app_shapes.normalize_content_part_type = (function knoxx$backend$app_shapes$normalize_content_part_type(value){
var G__69601 = (function (){var G__69602 = value;
var G__69602__$1 = (((G__69602 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69602)));
var G__69602__$2 = (((G__69602__$1 == null))?null:clojure.string.trim(G__69602__$1));
if((G__69602__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__69602__$2);
}
})();
switch (G__69601) {
case "text":
case "input_text":
case "output_text":
case "refusal":
return new cljs.core.Keyword(null,"text","text",-1790561697);

break;
case "image":
case "image_url":
case "input_image":
case "output_image":
return new cljs.core.Keyword(null,"image","image",-58725096);

break;
case "audio":
case "audio_url":
case "input_audio":
case "output_audio":
return new cljs.core.Keyword(null,"audio","audio",1819127321);

break;
case "video":
case "video_url":
case "input_video":
case "output_video":
return new cljs.core.Keyword(null,"video","video",156888130);

break;
case "document":
case "file":
case "input_file":
case "output_file":
return new cljs.core.Keyword(null,"document","document",-1329188687);

break;
default:
return null;

}
});
knoxx.backend.app_shapes.normalize_content_part = (function knoxx$backend$app_shapes$normalize_content_part(part){
if(typeof part === 'string'){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"text","text",-1790561697),part], null);
} else {
if(cljs.core.map_QMARK_(part)){
var type = knoxx.backend.app_shapes.normalize_content_part_type((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"partType","partType",-2014749732).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"part-type","part-type",631022337).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"part_type","part_type",129170086).cljs$core$IFn$_invoke$arity$1(part);
}
}
}
})());
var text = (function (){var G__69603 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"refusal","refusal",-44985842).cljs$core$IFn$_invoke$arity$1(part);
}
})();
if((G__69603 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69603));
}
})();
var url = (function (){var G__69604 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"file_url","file_url",314758761).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"file-url","file-url",-863738963).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"fileUrl","fileUrl",1401098371).cljs$core$IFn$_invoke$arity$1(part);
}
}
}
})();
if((G__69604 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69604));
}
})();
var data = (function (){var G__69605 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part);
if((G__69605 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69605));
}
})();
var mime_type = (function (){var G__69606 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"mime_type","mime_type",1613436611).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"mediaType","mediaType",272273903).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"media_type","media_type",-696536767).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"media-type","media-type",-764436129).cljs$core$IFn$_invoke$arity$1(part);
}
}
}
}
}
})();
if((G__69606 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69606));
}
})();
var filename = (function (){var G__69607 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part);
if((G__69607 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69607));
}
})();
var size = (function (){var value = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"size","size",1098693007).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"bytes","bytes",1175866680).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"byteSize","byteSize",737211841).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"byte_size","byte_size",626949575).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return new cljs.core.Keyword(null,"byte-size","byte-size",452288254).cljs$core$IFn$_invoke$arity$1(part);
}
}
}
}
})();
if(typeof value === 'number'){
return value;
} else {
return null;
}
})();
if(cljs.core.truth_((function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = text;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = url;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = data;
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return filename;
}
}
}
}
})())){
var G__69608 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),(function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);
}
})()], null);
var G__69608__$1 = ((((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"text","text",-1790561697);
}
})(),new cljs.core.Keyword(null,"text","text",-1790561697))) && ((!((text == null))))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608,new cljs.core.Keyword(null,"text","text",-1790561697),text):G__69608);
var G__69608__$2 = (cljs.core.truth_(url)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608__$1,new cljs.core.Keyword(null,"url","url",276297046),url):G__69608__$1);
var G__69608__$3 = (cljs.core.truth_(data)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608__$2,new cljs.core.Keyword(null,"data","data",-232669377),data):G__69608__$2);
var G__69608__$4 = (cljs.core.truth_(mime_type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608__$3,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type):G__69608__$3);
var G__69608__$5 = (cljs.core.truth_(filename)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608__$4,new cljs.core.Keyword(null,"filename","filename",-1428840783),filename):G__69608__$4);
if(cljs.core.truth_(size)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__69608__$5,new cljs.core.Keyword(null,"size","size",1098693007),size);
} else {
return G__69608__$5;
}
} else {
return null;
}
} else {
return null;

}
}
});
knoxx.backend.app_shapes.normalize_content_parts = (function knoxx$backend$app_shapes$normalize_content_parts(value){
var parts = (function (){var G__69609 = value;
if((G__69609 == null)){
return null;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(G__69609,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}
})();
if(cljs.core.sequential_QMARK_(parts)){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.app_shapes.normalize_content_part,parts));
} else {
if(cljs.core.map_QMARK_(parts)){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.app_shapes.normalize_content_part,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [parts], null)));
} else {
return cljs.core.PersistentVector.EMPTY;

}
}
});
knoxx.backend.app_shapes.normalize_chat_body = (function knoxx$backend$app_shapes$normalize_chat_body(body){
var message = (function (){var or__5142__auto__ = (body["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var raw_content_parts = knoxx.backend.app_shapes.normalize_content_parts((function (){var or__5142__auto__ = (body["contentParts"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["content_parts"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (body["content-parts"]);
}
}
})());
var content_parts = raw_content_parts;
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"template-context","template-context",-946500342),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"content-parts","content-parts",684529019),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"model","model",331153215)],[(function (){var or__5142__auto__ = (body["conversationId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["conversation_id"]);
}
})(),(function (){var or__5142__auto__ = (body["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["session_id"]);
}
})(),(function (){var G__69610 = (function (){var or__5142__auto__ = (body["authContext"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["auth_context"]);
}
})();
if((G__69610 == null)){
return null;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(G__69610,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}
})(),(function (){var G__69611 = (function (){var or__5142__auto__ = (body["templateContext"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["template_context"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (body["template-context"]);
}
}
})();
if((G__69611 == null)){
return null;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(G__69611,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}
})(),(function (){var or__5142__auto__ = (body["mode"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "direct";
}
})(),(function (){var or__5142__auto__ = (body["thinkingLevel"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["thinking_level"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (body["reasoningEffort"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (body["reasoning_effort"]);
}
}
}
})(),knoxx.backend.app_shapes.normalize_agent_spec((function (){var or__5142__auto__ = (body["agentSpec"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_spec"]);
}
})()),content_parts,(function (){var or__5142__auto__ = (body["runId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["run_id"]);
}
})(),message,(function (){var or__5142__auto__ = (body["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()]);
});
knoxx.backend.app_shapes.normalize_control_body = (function knoxx$backend$app_shapes$normalize_control_body(body){
var metadata = (function (){var or__5142__auto__ = (body["metadata"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["lineage"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return ({});
}
}
})();
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"message","message",-406056002),(function (){var or__5142__auto__ = (body["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(function (){var or__5142__auto__ = (body["conversationId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["conversation_id"]);
}
})(),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(function (){var or__5142__auto__ = (body["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["session_id"]);
}
})(),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(function (){var or__5142__auto__ = (body["runId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["run_id"]);
}
})(),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),(function (){var G__69612 = (function (){var or__5142__auto__ = (body["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["actor_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (body["actor-id"]);
}
}
})();
var G__69612__$1 = (((G__69612 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69612)));
var G__69612__$2 = (((G__69612__$1 == null))?null:clojure.string.trim(G__69612__$1));
if((G__69612__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69612__$2);
}
})(),new cljs.core.Keyword(null,"metadata","metadata",1799301597),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(metadata,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
});
/**
 * Register a Fastify route. handler-or-opts may be either:
 * - a plain function  → classic-mode (no preHandlers)
 * - a JS options obj  → preHandler-mode from defroute macro;
 *   its keys are merged into the base route options so that
 *   @fastify/websocket receives a proper :handler fn, not a
 *   nested object (which causes `handler.call is not a function`).
 */
knoxx.backend.app_shapes.route_BANG_ = (function knoxx$backend$app_shapes$route_BANG_(app,method,url,handler_or_opts){
if(cljs.core.fn_QMARK_(handler_or_opts)){
return app.route(({"method": method, "url": url, "handler": handler_or_opts}));
} else {
return app.route(Object.assign(({"method": method, "url": url}),handler_or_opts));
}
});

//# sourceMappingURL=knoxx.backend.app_shapes.js.map
