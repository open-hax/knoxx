import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.runtime.defaults.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
import "./knoxx.backend.tools.semantic.js";
import "./knoxx.backend.tools.discord.js";
import "./knoxx.backend.tools.discord_voice.js";
import "./knoxx.backend.tools.events.js";
import "./knoxx.backend.tools.actors.js";
import "./knoxx.backend.tools.openplanner.js";
import "./knoxx.backend.tools.music.js";
import "./knoxx.backend.tools.voice.js";
import "./knoxx.backend.tools.bluesky.js";
import "./knoxx.backend.tools.multimodal.js";
import "./knoxx.backend.tools.workspace_media.js";
import "./knoxx.backend.tools.mcp.js";
import "./knoxx.backend.tools.contracts.js";
import "./knoxx.backend.tools.nrepl.js";
import "./knoxx.backend.tools.session_mycology.js";
goog.provide('knoxx.backend.agent_hydration');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agent_hydration !== 'undefined') && (typeof knoxx.backend.agent_hydration.settings_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.agent_hydration.settings_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.agent_hydration.ensure_settings_BANG_ = (function knoxx$backend$agent_hydration$ensure_settings_BANG_(config){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_))){
} else {
cljs.core.reset_BANG_(knoxx.backend.agent_hydration.settings_state_STAR_,knoxx.backend.runtime.defaults.default_settings(config));
}

return cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_);
});
knoxx.backend.agent_hydration.passive_hydration_BANG_ = (function knoxx$backend$agent_hydration$passive_hydration_BANG_(var_args){
var G__34893 = arguments.length;
switch (G__34893) {
case 4:
return knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,mode,message){
return knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$5(runtime,config,mode,message,null);
}));

(knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (runtime,config,mode,message,auth_context){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mode,"rag")){
var started_ms = Date.now();
var top_k = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((4),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"retrievalTopK","retrievalTopK",1126924102).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (3);
}
})()));
return knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),message,new cljs.core.Keyword(null,"top-k","top-k",-1255881544),top_k,new cljs.core.Keyword(null,"max-snippet-chars","max-snippet-chars",785562463),(240)], null),auth_context).then((function (result){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(result,new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),(Date.now() - started_ms));
}));
} else {
return Promise.resolve(null);
}
}));

(knoxx.backend.agent_hydration.passive_hydration_BANG_.cljs$lang$maxFixedArity = 5);

knoxx.backend.agent_hydration.memory_hydration_trigger_QMARK_ = (function knoxx$backend$agent_hydration$memory_hydration_trigger_QMARK_(message){
return cljs.core.boolean$(cljs.core.re_find(/\b(previous|earlier|before|remember|last time|prior|session|you said|you did|we talked|we discussed)\b/i,(function (){var or__5142__auto__ = message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
});
knoxx.backend.agent_hydration.passive_memory_hydration_options = (function knoxx$backend$agent_hydration$passive_memory_hydration_options(agent_spec){
var or__5142__auto__ = new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passive-hydration","passive-hydration",-1337823895)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory_hydration","memory_hydration",-1458677455)], null));
}
}
}
}
});
knoxx.backend.agent_hydration.passive_memory_hydration_mode = (function knoxx$backend$agent_hydration$passive_memory_hydration_mode(opts){
var G__34932 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"source-mode","source-mode",725702471).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"sourceMode","sourceMode",1518870745).cljs$core$IFn$_invoke$arity$1(opts);
}
}
})();
var G__34932__$1 = (((G__34932 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34932)));
var G__34932__$2 = (((G__34932__$1 == null))?null:clojure.string.trim(G__34932__$1));
var G__34932__$3 = (((G__34932__$2 == null))?null:clojure.string.lower_case(G__34932__$2));
if((G__34932__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34932__$3);
}
});
knoxx.backend.agent_hydration.positive_int_or = (function knoxx$backend$agent_hydration$positive_int_or(value,fallback){
var parsed = parseInt(value,(10));
if(((typeof parsed === 'number') && (((cljs.core.not(isNaN(parsed))) && ((parsed > (0))))))){
return parsed;
} else {
return fallback;
}
});
knoxx.backend.agent_hydration.passive_memory_hydration_enabled_QMARK_ = (function knoxx$backend$agent_hydration$passive_memory_hydration_enabled_QMARK_(opts){
return (((!(new cljs.core.Keyword(null,"enabled?","enabled?",-1376075057).cljs$core$IFn$_invoke$arity$1(opts) === false))) && ((!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(opts) === false))));
});
knoxx.backend.agent_hydration.passive_memory_hydration_should_run_QMARK_ = (function knoxx$backend$agent_hydration$passive_memory_hydration_should_run_QMARK_(message,opts){
var mode = knoxx.backend.agent_hydration.passive_memory_hydration_mode(opts);
return ((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["eager",null,"always",null,"on",null], null), null),mode)) || (((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2("off",mode)) && (knoxx.backend.agent_hydration.memory_hydration_trigger_QMARK_(message)))));
});
knoxx.backend.agent_hydration.passive_memory_hydration_BANG_ = (function knoxx$backend$agent_hydration$passive_memory_hydration_BANG_(var_args){
var G__34971 = arguments.length;
switch (G__34971) {
case 3:
return knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (config,conversation_id,message){
return knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$5(config,conversation_id,message,null,null);
}));

(knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (config,conversation_id,message,auth_context){
return knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$5(config,conversation_id,message,auth_context,null);
}));

(knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (config,conversation_id,message,auth_context,agent_spec){
var opts = (function (){var or__5142__auto__ = knoxx.backend.agent_hydration.passive_memory_hydration_options(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
if(((knoxx.backend.http.openplanner_enabled_QMARK_(config)) && (((knoxx.backend.agent_hydration.passive_memory_hydration_enabled_QMARK_(opts)) && (knoxx.backend.agent_hydration.passive_memory_hydration_should_run_QMARK_(message,opts)))))){
var started_ms = Date.now();
var k = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((12),knoxx.backend.agent_hydration.positive_int_or((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"k","k",-2146297393).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"top-k","top-k",-1255881544).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"topK","topK",939681881).cljs$core$IFn$_invoke$arity$1(opts);
}
}
})(),(6))));
return knoxx.backend.openplanner_memory.openplanner_memory_search_BANG_(config,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"query","query",-1288509510),message,new cljs.core.Keyword(null,"k","k",-2146297393),k], null)).then((function (result){
return knoxx.backend.core_memory.filter_authorized_memory_hits_BANG_(config,auth_context,new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(result)).then((function (hits){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"hits","hits",-2120002930),hits,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"mode","mode",654403691),(function (){var or__5142__auto__ = knoxx.backend.agent_hydration.passive_memory_hydration_mode(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "triggered";
}
})(),new cljs.core.Keyword(null,"elapsedMs","elapsedMs",1350426486),(Date.now() - started_ms),new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),conversation_id], 0));
}));
}));
} else {
return Promise.resolve(null);
}
}));

(knoxx.backend.agent_hydration.passive_memory_hydration_BANG_.cljs$lang$maxFixedArity = 5);

knoxx.backend.agent_hydration.passive_hydration_text = (function knoxx$backend$agent_hydration$passive_hydration_text(hydration){
if(cljs.core.seq(new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(hydration))){
return (""+"Passive semantic hydration from the active Knoxx corpus follows. This context is automatic and may be incomplete. Use semantic_query or semantic_read if more grounding is needed.\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,result){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(result))+"\n   relevance: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Number(new cljs.core.Keyword(null,"score","score",-1963588780).cljs$core$IFn$_invoke$arity$1(result))).toFixed((2)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(result))?(""+", indexed chunks: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666).cljs$core$IFn$_invoke$arity$1(result))):null))+"\n   snippet: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(result)));
}),new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(hydration)))));
} else {
return null;
}
});
knoxx.backend.agent_hydration.passive_memory_hydration_text = (function knoxx$backend$agent_hydration$passive_memory_hydration_text(memory){
if(cljs.core.seq(new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(memory))){
return (""+"Passive conversational memory hydration from OpenPlanner follows. This is prior Knoxx session memory and action history; verify with memory_search or memory_session if precision matters.\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,hit){
var metadata = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"metadata","metadata",1799301597).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return hit;
}
})();
var session = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown-session";
}
}
})();
var role = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "memory";
}
}
})();
var snippet = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"document","document",-1329188687).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". session="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session)+", role="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(role)+"\n   snippet: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(snippet,(260));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}),new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(memory)))));
} else {
return null;
}
});
knoxx.backend.agent_hydration.build_agent_user_message = (function knoxx$backend$agent_hydration$build_agent_user_message(message,hydration,memory){
var parts = (function (){var G__35029 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"User request:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message))], null);
var G__35029__$1 = (cljs.core.truth_(knoxx.backend.agent_hydration.passive_hydration_text(hydration))?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__35029,knoxx.backend.agent_hydration.passive_hydration_text(hydration)):G__35029);
if(cljs.core.truth_(knoxx.backend.agent_hydration.passive_memory_hydration_text(memory))){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__35029__$1,knoxx.backend.agent_hydration.passive_memory_hydration_text(memory));
} else {
return G__35029__$1;
}
})();
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",parts);
});
/**
 * Build a multimodal message for models that support images, audio, video, and documents.
 * Returns a JavaScript array of content parts suitable for the agent SDK.
 */
knoxx.backend.agent_hydration.build_agent_multimodal_message = (function knoxx$backend$agent_hydration$build_agent_multimodal_message(message,content_parts,hydration,memory){
var text_content = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",(function (){var G__35030 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"User request:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message))], null);
var G__35030__$1 = (cljs.core.truth_(knoxx.backend.agent_hydration.passive_hydration_text(hydration))?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__35030,knoxx.backend.agent_hydration.passive_hydration_text(hydration)):G__35030);
if(cljs.core.truth_(knoxx.backend.agent_hydration.passive_memory_hydration_text(memory))){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__35030__$1,knoxx.backend.agent_hydration.passive_memory_hydration_text(memory));
} else {
return G__35030__$1;
}
})());
var base_parts = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),text_content], null)], null);
if(cljs.core.seq(content_parts)){
return cljs.core.clj__GT_js(cljs.core.into.cljs$core$IFn$_invoke$arity$2(base_parts,cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (part){
var p = ((cljs.core.map_QMARK_(part))?part:cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(part,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
var raw = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p);
}
})();
var strip_fn = (function (s){
if(((typeof s === 'string') && (clojure.string.starts_with_QMARK_(s,"data:")))){
var i = s.indexOf(",");
if((i >= (0))){
return s.slice((i + (1)));
} else {
return s;
}
} else {
return s;
}
});
var data = strip_fn(raw);
var mime = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(((typeof raw === 'string') && (clojure.string.starts_with_QMARK_(raw,"data:")))){
return cljs.core.second(cljs.core.re_find(/data:([^;,]+)/,raw));
} else {
return null;
}
}
})();
var ptype = (function (){var G__35040 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (p["type"]);
}
})();
if((G__35040 == null)){
return null;
} else {
return cljs.core.name(G__35040);
}
})();
var G__35041 = ptype;
switch (G__35041) {
case "text":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(p)))], null);

break;
case "image":
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"image",new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime], null);

break;
case "audio":
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"audio",new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime], null);

break;
case "video":
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"video",new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime], null);

break;
case "document":
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),"document",new cljs.core.Keyword(null,"data","data",-232669377),data,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime,new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(p)], null);

break;
default:
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),(""+"[Unknown: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ptype)+"]")], null);

}
}),content_parts)));
} else {
return cljs.core.clj__GT_js(base_parts);
}
});
knoxx.backend.agent_hydration.hydration_sources = (function knoxx$backend$agent_hydration$hydration_sources(hydration){
if(cljs.core.seq(new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(hydration))){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (result){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"url","url",276297046),new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"section","section",-300141526),new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(result)], null);
}),new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(hydration));
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
/**
 * Compose the full Knoxx tool suite from vertical domain slices.
 */
knoxx.backend.agent_hydration.create_knoxx_custom_tools = (function knoxx$backend$agent_hydration$create_knoxx_custom_tools(var_args){
var G__35043 = arguments.length;
switch (G__35043) {
case 2:
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$4(runtime,config,auth_context,null);
}));

(knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,auth_context,allowed_tool_ids){
return knoxx.backend.tools.shared.filter_custom_tools_by_allow_set(knoxx.backend.tools.shared.sanitize_custom_tools(knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context).concat(knoxx.backend.tools.discord.create_discord_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.events.create_events_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.actors.create_actors_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.openplanner.create_openplanner_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.session_mycology.create_session_mycology_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context)).concat(knoxx.backend.tools.nrepl.create_nrepl_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context))),allowed_tool_ids);
}));

(knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$lang$maxFixedArity = 4);

/**
 * Compatibility wrapper for tests and older call sites.
 */
knoxx.backend.agent_hydration.agent_custom_tool_suite = (function knoxx$backend$agent_hydration$agent_custom_tool_suite(agent_spec){
return knoxx.backend.tools.shared.agent_custom_tool_suite(agent_spec);
});
/**
 * Dispatch to the appropriate tool suite for the given agent spec.
 */
knoxx.backend.agent_hydration.create_agent_custom_tools = (function knoxx$backend$agent_hydration$create_agent_custom_tools(var_args){
var G__35046 = arguments.length;
switch (G__35046) {
case 2:
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5(runtime,config,null,null,null);
}));

(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5(runtime,config,auth_context,null,null);
}));

(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,auth_context,agent_spec){
return knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5(runtime,config,auth_context,agent_spec,null);
}));

(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5 = (function (runtime,config,auth_context,agent_spec,allowed_tool_ids){
var G__35055 = knoxx.backend.tools.shared.agent_custom_tool_suite(agent_spec);
var G__35055__$1 = (((G__35055 instanceof cljs.core.Keyword))?G__35055.fqn:null);
switch (G__35055__$1) {
case "contract-librarian":
return knoxx.backend.tools.contracts.create_contract_librarian_tools.cljs$core$IFn$_invoke$arity$4(runtime,config,auth_context,allowed_tool_ids);

break;
default:
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$4(runtime,config,auth_context,allowed_tool_ids);

}
}));

(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$lang$maxFixedArity = 5);


//# sourceMappingURL=knoxx.backend.agent_hydration.js.map
