import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.defaults.js";
goog.provide('knoxx.backend.agent_templates');
knoxx.backend.agent_templates.model_profiles = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"local-fast","local-fast",-1899378251),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"gemma4:e4b",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"local-mid","local-mid",1783247256),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"gemma4:31b",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"local-heavy","local-heavy",-855646526),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"gemma4:31b",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"minimal"], null),new cljs.core.Keyword(null,"cloud-heavy","cloud-heavy",507323846),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"glm-5",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"high"], null),new cljs.core.Keyword(null,"cloud-fast","cloud-fast",-1978516418),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"glm-5-fast",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"cloud-balanced","cloud-balanced",179139458),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),"glm-5",new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"minimal"], null)], null);
knoxx.backend.agent_templates.resolve_model_profile = (function knoxx$backend$agent_templates$resolve_model_profile(profile_id){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_templates.model_profiles,profile_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"model","model",331153215),knoxx.backend.runtime.defaults.default_model(knoxx.backend.runtime.config.cfg()),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null);
}
});
knoxx.backend.agent_templates.all_model_profiles = (function knoxx$backend$agent_templates$all_model_profiles(){
return cljs.core.vec(cljs.core.keys(knoxx.backend.agent_templates.model_profiles));
});
knoxx.backend.agent_templates.templates = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"yap-bot","yap-bot",1264882118),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"role","role",-736691072),"creative_catalyst",new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),"You are the Frankie-Infinite-Yap Bot. Be creatively chaotic, musically inclined, and a catalyst for lyrics and rhythms. Prefer vivid, surreal imagery over dry facts.",new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),new cljs.core.Keyword(null,"local-fast","local-fast",-1899378251),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"sentinel","sentinel",908650593),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"role","role",-736691072),"security_monitor",new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),"You are a system sentinel. Monitor logs for anomalies, security drift, and critical failures. Be concise and urgent.",new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),new cljs.core.Keyword(null,"cloud-heavy","cloud-heavy",507323846),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"high"], null),new cljs.core.Keyword(null,"summarizer","summarizer",1296998125),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"role","role",-736691072),"knowledge_synthesizer",new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),"You are a professional synthesizer. Extract key themes, action items, and critical signal from noisy conversations.",new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),new cljs.core.Keyword(null,"local-mid","local-mid",1783247256),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"memory_search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"patrol-observer","patrol-observer",-664829155),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"role","role",-736691072),"knowledge_worker",new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),"Observe configured channels, detect fresh human signals, and queue structured events without speaking publicly.",new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),new cljs.core.Keyword(null,"local-fast","local-fast",-1899378251),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channel.messages",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null),new cljs.core.Keyword(null,"mention-responder","mention-responder",-1426945499),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"role","role",-736691072),"executive",new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),"You are Knoxx's targeted event-driven Discord responder. Read the room, use tools when needed, and prefer silence over filler.",new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),new cljs.core.Keyword(null,"local-fast","local-fast",-1899378251),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"memory_search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"graph_query",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),"off"], null)], null);
knoxx.backend.agent_templates.get_template = (function knoxx$backend$agent_templates$get_template(template_id){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_templates.templates,template_id);
});
knoxx.backend.agent_templates.all_templates = (function knoxx$backend$agent_templates$all_templates(){
return cljs.core.vec(cljs.core.keys(knoxx.backend.agent_templates.templates));
});
/**
 * Resolve a template into a concrete agent spec.
 * Merges template defaults + model profile + user overrides.
 * Returns a map suitable for :agentSpec in event-agent jobs.
 */
knoxx.backend.agent_templates.resolve_template_spec = (function knoxx$backend$agent_templates$resolve_template_spec(template_id,overrides){
var template = knoxx.backend.agent_templates.get_template(template_id);
if(cljs.core.not(template)){
throw (new Error((""+"Unknown agent template: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(template_id))));
} else {
var model_cfg = knoxx.backend.agent_templates.resolve_model_profile(new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992).cljs$core$IFn$_invoke$arity$1(template));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([template,model_cfg], 0)),overrides], 0)),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(overrides);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(template);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(model_cfg);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "off";
}
}
}
})());
}
});
knoxx.backend.agent_templates.default_tool_policies = (function knoxx$backend$agent_templates$default_tool_policies(){
return new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channel.messages",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channel.scroll",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.dm.messages",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"websearch",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"memory_search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"graph_query",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null);
});
/**
 * Create a concrete event-agent job from a template.
 * 
 * Args:
 * - template-id: Keyword identifying the template (e.g., :yap-bot)
 * - job-id: Unique string ID for this job instance
 * - trigger: Map with :kind ("cron" or "event"), :cadenceMinutes, :eventKinds
 * - source: Map with :kind, :mode, :config
 * - filters: Map with :channels, :keywords, :repositories
 * - overrides: Optional map to override template defaults
 * 
 * Returns a complete job spec ready for persistence.
 */
knoxx.backend.agent_templates.instantiate_job = (function knoxx$backend$agent_templates$instantiate_job(var_args){
var args__5882__auto__ = [];
var len__5876__auto___33646 = arguments.length;
var i__5877__auto___33647 = (0);
while(true){
if((i__5877__auto___33647 < len__5876__auto___33646)){
args__5882__auto__.push((arguments[i__5877__auto___33647]));

var G__33649 = (i__5877__auto___33647 + (1));
i__5877__auto___33647 = G__33649;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((5) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((5)),(0),null)):null);
return knoxx.backend.agent_templates.instantiate_job.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),argseq__5883__auto__);
});

(knoxx.backend.agent_templates.instantiate_job.cljs$core$IFn$_invoke$arity$variadic = (function (template_id,job_id,trigger,source,filters,p__33632){
var vec__33633 = p__33632;
var overrides = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__33633,(0),null);
var agent_spec = knoxx.backend.agent_templates.resolve_template_spec(template_id,overrides);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"templateId","templateId",613248985),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"enabled","enabled",1195909756)],[(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(overrides);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Instance of "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(template_id))+" template");
}
})(),filters,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(overrides);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return job_id;
}
})(),source,job_id,cljs.core.name(template_id),agent_spec,trigger,true]);
}));

(knoxx.backend.agent_templates.instantiate_job.cljs$lang$maxFixedArity = (5));

/** @this {Function} */
(knoxx.backend.agent_templates.instantiate_job.cljs$lang$applyTo = (function (seq33624){
var G__33625 = cljs.core.first(seq33624);
var seq33624__$1 = cljs.core.next(seq33624);
var G__33626 = cljs.core.first(seq33624__$1);
var seq33624__$2 = cljs.core.next(seq33624__$1);
var G__33627 = cljs.core.first(seq33624__$2);
var seq33624__$3 = cljs.core.next(seq33624__$2);
var G__33628 = cljs.core.first(seq33624__$3);
var seq33624__$4 = cljs.core.next(seq33624__$3);
var G__33629 = cljs.core.first(seq33624__$4);
var seq33624__$5 = cljs.core.next(seq33624__$4);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__33625,G__33626,G__33627,G__33628,G__33629,seq33624__$5);
}));

/**
 * Ensure a job spec has all required fields for durable storage.
 * Adds timestamps and validates structure.
 */
knoxx.backend.agent_templates.normalize_job_for_persistence = (function knoxx$backend$agent_templates$normalize_job_for_persistence(job){
var now = (new Date()).toISOString();
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc_in(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(job,new cljs.core.Keyword(null,"createdAt","createdAt",-936788),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"createdAt","createdAt",-936788).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return now;
}
})()),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),now),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429)], null),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})()),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),(function (p1__33639_SHARP_){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__33639_SHARP_,new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953)),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(p1__33639_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(p1__33639_SHARP_);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})());
}));
});

//# sourceMappingURL=knoxx.backend.agent_templates.js.map
