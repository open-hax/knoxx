import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.actor_mailbox.js";
import "./knoxx.backend.agent_context.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.extension_runtime.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.tooling.js";
import "./shadow.esm.esm_import$$open_hax$eta_mu.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.agent_runtime');
knoxx.backend.extension_runtime.init_BANG_();
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agent_runtime !== 'undefined') && (typeof knoxx.backend.agent_runtime.sdk_runtime_STAR_ !== 'undefined')){
} else {
knoxx.backend.agent_runtime.sdk_runtime_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agent_runtime !== 'undefined') && (typeof knoxx.backend.agent_runtime.agent_sessions_STAR_ !== 'undefined')){
} else {
knoxx.backend.agent_runtime.agent_sessions_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
knoxx.backend.agent_runtime.proxx_session_affinity_extension_code = (""+"import type { ExtensionAPI } from \"@open-hax/eta-mu\";\n\n"+"function isRecord(value: unknown): value is Record<string, unknown> {\n"+"  return typeof value === 'object' && value !== null;\n"+"}\n\n"+"export default function (etaMu: ExtensionAPI) {\n"+"  etaMu.on('before_provider_request', (event, ctx) => {\n"+"    // Only touch Knoxx\u2192Proxx traffic.\n"+"    if (ctx.model?.provider !== 'proxx') return;\n"+"\n"+"    const sessionKey = typeof ctx.sessionManager?.getSessionId === 'function'\n"+"      ? String(ctx.sessionManager.getSessionId() ?? '').trim()\n"+"      : '';\n"+"    if (!sessionKey) return;\n"+"\n"+"    const payload = event.payload;\n"+"    if (payload === null || typeof payload !== 'object') return;\n"+"    const record = payload as Record<string, unknown>;\n"+"\n"+"    const existing = typeof record.prompt_cache_key === 'string'\n"+"      ? record.prompt_cache_key.trim()\n"+"      : typeof record.promptCacheKey === 'string'\n"+"        ? record.promptCacheKey.trim()\n"+"        : '';\n"+"\n"+"    if (existing) return record;\n"+"\n"+"    // Proxx extracts prompt_cache_key from request bodies to enforce affinity.\n"+"    return { ...record, prompt_cache_key: sessionKey };\n"+"  });\n"+"}\n");
knoxx.backend.agent_runtime.js_array_seq = (function knoxx$backend$agent_runtime$js_array_seq(value){
if(cljs.core.truth_(cljs.core.array_QMARK_(value))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value);
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.agent_runtime.proxx_models_url = (function knoxx$backend$agent_runtime$proxx_models_url(config){
var base = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.ends_with_QMARK_(base,"/v1")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/models");
} else {
if(clojure.string.ends_with_QMARK_(base,"/v1/")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"models");
} else {
if(clojure.string.ends_with_QMARK_(base,"/")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"v1/models");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/v1/models");

}
}
}
});
/**
 * Fetch available model ids from Proxx /v1/models so Knoxx's eta-mu model registry includes
 * local Ollama (gemma4, qwen, etc) as well as upstream hosted models.
 * 
 * Returns a Promise of vector of strings.
 */
knoxx.backend.agent_runtime.fetch_proxx_model_ids_BANG_ = (function knoxx$backend$agent_runtime$fetch_proxx_model_ids_BANG_(config){
var token = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var url = knoxx.backend.agent_runtime.proxx_models_url(config);
if(clojure.string.blank_QMARK_(token)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return fetch(url,({"headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)), "Accept": "application/json"})})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return resp.json();
} else {
return Promise.reject((new Error((""+"Proxx /v1/models failed with status "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((resp["status"]))))));
}
})).then((function (payload){
var items = knoxx.backend.agent_runtime.js_array_seq((function (){var or__5142__auto__ = (payload["data"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
var ids = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (model_id){
return knoxx.backend.runtime.models.allowlisted_model_id_QMARK_(config,model_id);
}),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (item){
var raw = (item["id"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = raw;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw)))));
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw));
} else {
return null;
}
}),items)))));
return ids;
})).catch((function (_err){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}));
}
});
knoxx.backend.agent_runtime.mime__GT_audio_format = (function knoxx$backend$agent_runtime$mime__GT_audio_format(mime_type){
var mime = (function (){var G__52903 = mime_type;
var G__52903__$1 = (((G__52903 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52903)));
if((G__52903__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52903__$1);
}
})();
var G__52905 = mime;
switch (G__52905) {
case "audio/mpeg":
return "mp3";

break;
case "audio/mp4":
return "mp4";

break;
case "audio/wav":
return "wav";

break;
case "audio/x-wav":
return "wav";

break;
case "audio/ogg":
return "ogg";

break;
case "audio/flac":
return "flac";

break;
case "audio/aac":
return "aac";

break;
default:
var G__52906 = mime;
var G__52906__$1 = (((G__52906 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__52906,/\//));
if((G__52906__$1 == null)){
return null;
} else {
return cljs.core.second(G__52906__$1);
}

}
});
knoxx.backend.agent_runtime.stored_content_part__GT_agent_part = (function knoxx$backend$agent_runtime$stored_content_part__GT_agent_part(part){
var part_type = (function (){var G__52911 = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part);
var G__52911__$1 = (((G__52911 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52911)));
if((G__52911__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__52911__$1);
}
})();
var text = (function (){var G__52915 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(part);
if((G__52915 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52915));
}
})();
var url = (function (){var G__52916 = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part);
if((G__52916 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52916));
}
})();
var data = (function (){var G__52920 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(part);
if((G__52920 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52920));
}
})();
var mime_type = (function (){var G__52924 = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(part);
if((G__52924 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52924));
}
})();
var filename = (function (){var G__52925 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part);
if((G__52925 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52925));
}
})();
var G__52926 = part_type;
switch (G__52926) {
case "text":
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)))))){
return ({"type": "text", "text": text});
} else {
return null;
}

break;
case "image":
if(((typeof data === 'string') && ((((!(clojure.string.blank_QMARK_(data)))) && (clojure.string.starts_with_QMARK_(data,"data:")))))){
var comma = data.indexOf(",");
var raw = (((comma >= (0)))?data.slice((comma + (1))):data);
var mime = (function (){var or__5142__auto__ = mime_type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.second(cljs.core.re_find(/data:([^;,]+)/,data));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "image/png";
}
}
})();
return ({"type": "image", "data": raw, "mimeType": mime});
} else {
if(((typeof data === 'string') && ((!(clojure.string.blank_QMARK_(data)))))){
return ({"type": "image", "data": data, "mimeType": (function (){var or__5142__auto__ = mime_type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "image/png";
}
})()});
} else {
if(((typeof url === 'string') && ((!(clojure.string.blank_QMARK_(url)))))){
return ({"type": "image_url", "image_url": ({"url": url})});
} else {
return null;

}
}
}

break;
case "audio":
if(((typeof data === 'string') && ((((!(clojure.string.blank_QMARK_(data)))) && (clojure.string.starts_with_QMARK_(data,"data:")))))){
var comma = data.indexOf(",");
var raw = (((comma >= (0)))?data.slice((comma + (1))):data);
var mime = (function (){var or__5142__auto__ = mime_type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.second(cljs.core.re_find(/data:([^;,]+)/,data));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "audio/mpeg";
}
}
})();
return ({"type": "audio", "data": raw, "mimeType": mime, "format": (function (){var or__5142__auto__ = knoxx.backend.agent_runtime.mime__GT_audio_format(mime);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mp3";
}
})()});
} else {
if(((typeof data === 'string') && ((!(clojure.string.blank_QMARK_(data)))))){
return ({"type": "audio", "data": data, "mimeType": mime_type, "format": (function (){var or__5142__auto__ = knoxx.backend.agent_runtime.mime__GT_audio_format(mime_type);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mp3";
}
})()});
} else {
if(((typeof url === 'string') && ((!(clojure.string.blank_QMARK_(url)))))){
return ({"type": "audio", "data": url, "mimeType": mime_type, "format": (function (){var or__5142__auto__ = knoxx.backend.agent_runtime.mime__GT_audio_format(mime_type);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mp3";
}
})()});
} else {
return null;

}
}
}

break;
case "video":
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = data;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
})())))))){
return ({"type": "video", "data": (function (){var or__5142__auto__ = data;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
})(), "mimeType": mime_type});
} else {
return null;
}

break;
case "document":
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = data;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
})())))))){
return ({"type": "document", "data": (function (){var or__5142__auto__ = data;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
})(), "mimeType": mime_type, "filename": filename});
} else {
return null;
}

break;
default:
return null;

}
});
knoxx.backend.agent_runtime.stored_session_message__GT_agent_message = (function knoxx$backend$agent_runtime$stored_session_message__GT_agent_message(message){
var role = (function (){var G__52962 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(message);
if((G__52962 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52962));
}
})();
var content = (function (){var G__52964 = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if((G__52964 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52964));
}
})();
var content_parts = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.stored_content_part__GT_agent_part,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content-parts","content-parts",684529019).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
var payload = ((cljs.core.seq(content_parts))?cljs.core.clj__GT_js(content_parts):(((!(clojure.string.blank_QMARK_(content))))?[({"type": "text", "text": content})]:null
));
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["user",null,"assistant",null,"system",null], null), null),role);
if(and__5140__auto__){
return payload;
} else {
return and__5140__auto__;
}
})())){
return ({"role": role, "content": payload, "timestamp": Date.now()});
} else {
return null;
}
});
knoxx.backend.agent_runtime.planner_row__GT_stored_session_message = (function knoxx$backend$agent_runtime$planner_row__GT_stored_session_message(row){
var role = (function (){var G__52990 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(row);
if((G__52990 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52990));
}
})();
var text = (function (){var G__52991 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(row);
if((G__52991 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52991));
}
})();
if(((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["user",null,"assistant",null,"system",null], null), null),role)) && ((!(clojure.string.blank_QMARK_(text)))))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"content","content",15833224),text], null);
} else {
return null;
}
});
knoxx.backend.agent_runtime.fetch_openplanner_session_messages_BANG_ = (function knoxx$backend$agent_runtime$fetch_openplanner_session_messages_BANG_(config,conversation_id){
if(((clojure.string.blank_QMARK_(conversation_id)) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id))).then((function (body){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.planner_row__GT_stored_session_message,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
})).catch((function (err){
console.warn("[knoxx] failed to fetch OpenPlanner session transcript",err);

return cljs.core.PersistentVector.EMPTY;
}));
}
});
knoxx.backend.agent_runtime.comparable_session_message = (function knoxx$backend$agent_runtime$comparable_session_message(message){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),(function (){var G__53007 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(message);
if((G__53007 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53007));
}
})(),new cljs.core.Keyword(null,"content","content",15833224),(function (){var G__53008 = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if((G__53008 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53008));
}
})()], null);
});
knoxx.backend.agent_runtime.merge_restored_session_messages = (function knoxx$backend$agent_runtime$merge_restored_session_messages(base_messages,overlay_messages){
var base = cljs.core.vec((function (){var or__5142__auto__ = base_messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var overlay = cljs.core.vec((function (){var or__5142__auto__ = overlay_messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var base_STAR_ = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.comparable_session_message,base);
var overlay_STAR_ = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.comparable_session_message,overlay);
var overlap = (function (){var n = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.count(base_STAR_),cljs.core.count(overlay_STAR_));
while(true){
if((n === (0))){
return (0);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.subvec.cljs$core$IFn$_invoke$arity$2(base_STAR_,(cljs.core.count(base_STAR_) - n)),cljs.core.subvec.cljs$core$IFn$_invoke$arity$3(overlay_STAR_,(0),n))){
return n;
} else {
var G__53488 = (n - (1));
n = G__53488;
continue;

}
}
break;
}
})();
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(base,cljs.core.subvec.cljs$core$IFn$_invoke$arity$2(overlay,overlap));
});
knoxx.backend.agent_runtime.sync_system_message = (function knoxx$backend$agent_runtime$sync_system_message(messages,system_prompt){
var items = cljs.core.vec((function (){var or__5142__auto__ = messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var prompt = (function (){var G__53015 = system_prompt;
var G__53015__$1 = (((G__53015 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53015)));
var G__53015__$2 = (((G__53015__$1 == null))?null:clojure.string.trim(G__53015__$1));
if((G__53015__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53015__$2);
}
})();
if(cljs.core.not(prompt)){
return items;
} else {
var system_index = cljs.core.reduce_kv((function (_,idx,entry){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("system",(function (){var G__53023 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(entry);
var G__53023__$1 = (((G__53023 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53023)));
if((G__53023__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__53023__$1);
}
})())){
return cljs.core.reduced(idx);
} else {
return null;
}
}),null,items);
if((!((system_index == null)))){
var updated = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,system_index,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"system",new cljs.core.Keyword(null,"content","content",15833224),prompt], null));
return cljs.core.vec(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,entry){
if(((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2("system",(function (){var G__53026 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(entry);
var G__53026__$1 = (((G__53026 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53026)));
if((G__53026__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__53026__$1);
}
})())) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(idx,system_index)))){
return entry;
} else {
return null;
}
}),updated));
} else {
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"system",new cljs.core.Keyword(null,"content","content",15833224),prompt], null)], null),items);
}
}
});
knoxx.backend.agent_runtime.context_policy = (function knoxx$backend$agent_runtime$context_policy(agent_spec){
var or__5142__auto__ = new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"context","context",-830191113).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extras","extras",-1110348066),new cljs.core.Keyword(null,"context","context",-830191113)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extras","extras",-1110348066),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557)], null));
}
}
}
}
});
knoxx.backend.agent_runtime.positive_int_value = (function knoxx$backend$agent_runtime$positive_int_value(value){
var n = Number(value);
if(((cljs.core.not(isNaN(n))) && ((n > (0))))){
return Math.floor(n);
} else {
return null;
}
});
knoxx.backend.agent_runtime.message_text_size = (function knoxx$backend$agent_runtime$message_text_size(message){
return ((((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))).length) + cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._PLUS_,(0),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__53029_SHARP_){
return (((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(p1__53029_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(p1__53029_SHARP_);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p1__53029_SHARP_);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})()))).length);
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content-parts","content-parts",684529019).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contentParts","contentParts",1395809695).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})())));
});
/**
 * Apply an agent contract context policy to stored transcript messages.
 * 
 * Supported contract shape:
 * :context {:max-messages 40
 *           :max-chars 80000
 *           :preserve-system true}
 * 
 * This is intentionally a deterministic sliding-window prune. Summary-based
 * compression can be layered later, but this prevents unbounded sticky sessions.
 */
knoxx.backend.agent_runtime.prune_session_messages = (function knoxx$backend$agent_runtime$prune_session_messages(agent_spec,messages){
var items = cljs.core.vec((function (){var or__5142__auto__ = messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var policy = knoxx.backend.agent_runtime.context_policy(agent_spec);
if(cljs.core.not(policy)){
return items;
} else {
var max_messages = knoxx.backend.agent_runtime.positive_int_value((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"max-messages","max-messages",-1089461657).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"max_messages","max_messages",-755082145).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})());
var max_chars = knoxx.backend.agent_runtime.positive_int_value((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"max-chars","max-chars",899663888).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"maxChars","maxChars",-1468489647).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"max_chars","max_chars",667525949).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})());
var preserve_system_QMARK_ = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"preserve-system","preserve-system",1239455246).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"preserveSystem","preserveSystem",-2026748027).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"preserve_system","preserve_system",-966670117).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})());
var system_messages = ((preserve_system_QMARK_)?cljs.core.filterv((function (p1__53037_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("system",(function (){var G__53041 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__53037_SHARP_);
var G__53041__$1 = (((G__53041 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53041)));
if((G__53041__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__53041__$1);
}
})());
}),items):cljs.core.PersistentVector.EMPTY);
var body_messages = ((preserve_system_QMARK_)?cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__53038_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("system",(function (){var G__53050 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__53038_SHARP_);
var G__53050__$1 = (((G__53050 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53050)));
if((G__53050__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__53050__$1);
}
})());
}),items):items);
var by_count = (cljs.core.truth_(max_messages)?cljs.core.take_last(max_messages,cljs.core.vec(body_messages)):cljs.core.vec(body_messages));
var by_chars = (cljs.core.truth_(max_chars)?(function (){var remaining = cljs.core.reverse(by_count);
var total = (0);
var kept = cljs.core.List.EMPTY;
while(true){
var temp__5823__auto__ = cljs.core.first(remaining);
if(cljs.core.truth_(temp__5823__auto__)){
var message = temp__5823__auto__;
var size = knoxx.backend.agent_runtime.message_text_size(message);
if(((cljs.core.seq(kept)) && (((total + size) > max_chars)))){
return cljs.core.vec(kept);
} else {
var G__53518 = cljs.core.rest(remaining);
var G__53519 = (total + size);
var G__53520 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(kept,message);
remaining = G__53518;
total = G__53519;
kept = G__53520;
continue;
}
} else {
return cljs.core.vec(kept);
}
break;
}
})():cljs.core.vec(by_count));
return cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(system_messages,by_chars));
}
});
knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_ = (function knoxx$backend$agent_runtime$rehydrate_session_manager_from_redis_BANG_(var_args){
var G__53059 = arguments.length;
switch (G__53059) {
case 3:
return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (config,session_manager,conversation_id){
return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$5(config,session_manager,conversation_id,null,null);
}));

(knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (config,session_manager,conversation_id,agent_spec){
return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$5(config,session_manager,conversation_id,null,agent_spec);
}));

(knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (config,session_manager,conversation_id,session_id,agent_spec){
var redis_client = knoxx.backend.redis_client.get_client();
var preferred_session_id = (function (){var G__53074 = session_id;
var G__53074__$1 = (((G__53074 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53074)));
var G__53074__$2 = (((G__53074__$1 == null))?null:clojure.string.trim(G__53074__$1));
if((G__53074__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53074__$2);
}
})();
var fetch_session_messages_BANG_ = (function (target_session_id){
if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = target_session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))) || ((redis_client == null)))){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return knoxx.backend.session_store.get_session(redis_client,target_session_id).then((function (session){
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
}));
}
});
return Promise.all([knoxx.backend.agent_runtime.fetch_openplanner_session_messages_BANG_(config,conversation_id),(cljs.core.truth_(preferred_session_id)?fetch_session_messages_BANG_(preferred_session_id):((((clojure.string.blank_QMARK_(conversation_id)) || ((redis_client == null))))?Promise.resolve(cljs.core.PersistentVector.EMPTY):knoxx.backend.session_store.get_conversation_active_session(redis_client,conversation_id).then((function (active_session_id){
return fetch_session_messages_BANG_(active_session_id);
}))))]).then((function (parts){
var openplanner_messages = cljs.core.vec((function (){var or__5142__auto__ = (parts[(0)]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var redis_messages = cljs.core.vec((function (){var or__5142__auto__ = (parts[(1)]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var merged_messages = (function (p1__53057_SHARP_){
return knoxx.backend.agent_runtime.prune_session_messages(agent_spec,p1__53057_SHARP_);
})(knoxx.backend.agent_runtime.sync_system_message(knoxx.backend.agent_runtime.merge_restored_session_messages(openplanner_messages,redis_messages),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(agent_spec)));
var seq__53091_53543 = cljs.core.seq(merged_messages);
var chunk__53092_53544 = null;
var count__53093_53545 = (0);
var i__53094_53546 = (0);
while(true){
if((i__53094_53546 < count__53093_53545)){
var message_53551 = chunk__53092_53544.cljs$core$IIndexed$_nth$arity$2(null,i__53094_53546);
var temp__5825__auto___53552 = knoxx.backend.agent_runtime.stored_session_message__GT_agent_message(message_53551);
if(cljs.core.truth_(temp__5825__auto___53552)){
var agent_message_53569 = temp__5825__auto___53552;
session_manager.appendMessage(agent_message_53569);
} else {
}


var G__53571 = seq__53091_53543;
var G__53572 = chunk__53092_53544;
var G__53573 = count__53093_53545;
var G__53574 = (i__53094_53546 + (1));
seq__53091_53543 = G__53571;
chunk__53092_53544 = G__53572;
count__53093_53545 = G__53573;
i__53094_53546 = G__53574;
continue;
} else {
var temp__5825__auto___53575 = cljs.core.seq(seq__53091_53543);
if(temp__5825__auto___53575){
var seq__53091_53582__$1 = temp__5825__auto___53575;
if(cljs.core.chunked_seq_QMARK_(seq__53091_53582__$1)){
var c__5673__auto___53583 = cljs.core.chunk_first(seq__53091_53582__$1);
var G__53584 = cljs.core.chunk_rest(seq__53091_53582__$1);
var G__53585 = c__5673__auto___53583;
var G__53586 = cljs.core.count(c__5673__auto___53583);
var G__53587 = (0);
seq__53091_53543 = G__53584;
chunk__53092_53544 = G__53585;
count__53093_53545 = G__53586;
i__53094_53546 = G__53587;
continue;
} else {
var message_53588 = cljs.core.first(seq__53091_53582__$1);
var temp__5825__auto___53589__$1 = knoxx.backend.agent_runtime.stored_session_message__GT_agent_message(message_53588);
if(cljs.core.truth_(temp__5825__auto___53589__$1)){
var agent_message_53590 = temp__5825__auto___53589__$1;
session_manager.appendMessage(agent_message_53590);
} else {
}


var G__53591 = cljs.core.next(seq__53091_53582__$1);
var G__53592 = null;
var G__53593 = (0);
var G__53594 = (0);
seq__53091_53543 = G__53591;
chunk__53092_53544 = G__53592;
count__53093_53545 = G__53593;
i__53094_53546 = G__53594;
continue;
}
} else {
}
}
break;
}

return ({"sessionManager": session_manager, "restored": cljs.core.boolean$(cljs.core.seq(merged_messages))});
}));
}));

(knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$lang$maxFixedArity = 5);

knoxx.backend.agent_runtime.request_stream_body = (function knoxx$backend$agent_runtime$request_stream_body(request){
var method = clojure.string.upper_case((function (){var or__5142__auto__ = (request["method"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "GET";
}
})());
var body = knoxx.backend.http.request_forward_body(request);
var content_type = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["headers"]["content-type"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["HEAD",null,"GET",null], null), null),method)){
return ({});
} else {
if((!((body == null)))){
return ({"body": body});
} else {
if(clojure.string.includes_QMARK_(content_type,"multipart/form-data")){
return ({"body": (request["raw"]), "duplex": "half"});
} else {
return ({});

}
}
}
});
knoxx.backend.agent_runtime.forward_knoxx_request_BANG_ = (function knoxx$backend$agent_runtime$forward_knoxx_request_BANG_(config,request,method,path,extra){
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config))+"/api/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.request_query_string(request)));
var base = ({"method": method, "headers": knoxx.backend.http.request_forward_headers(request,new cljs.core.PersistentArrayMap(null, 1, ["x-api-key",((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154).cljs$core$IFn$_invoke$arity$1(config)))?null:new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154).cljs$core$IFn$_invoke$arity$1(config))], null))});
var stream_opts = knoxx.backend.agent_runtime.request_stream_body(request);
return fetch(target_url,Object.assign(base,stream_opts,cljs.core.clj__GT_js(extra)));
});
knoxx.backend.agent_runtime.effective_tool_auth_context = (function knoxx$backend$agent_runtime$effective_tool_auth_context(auth_context,allowed_tool_ids){
if(cljs.core.not(auth_context)){
return null;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(auth_context,new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null);
}),cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.vec(allowed_tool_ids))));
}
});
knoxx.backend.agent_runtime.restore_agent_context_BANG_ = (function knoxx$backend$agent_runtime$restore_agent_context_BANG_(previous){
if(cljs.core.truth_(previous)){
return knoxx.backend.agent_context.set_context_BANG_(previous);
} else {
return knoxx.backend.agent_context.clear_context_BANG_();
}
});
knoxx.backend.agent_runtime.wrap_tool_execute_with_agent_context_BANG_ = (function knoxx$backend$agent_runtime$wrap_tool_execute_with_agent_context_BANG_(tool,context){
var execute_53608 = (function (){var and__5140__auto__ = tool;
if(cljs.core.truth_(and__5140__auto__)){
return (tool["execute"]);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.fn_QMARK_(execute_53608)){
(tool["execute"] = (function() { 
var G__53609__delegate = function (args){
var previous = knoxx.backend.agent_context.get_context();
knoxx.backend.agent_context.set_context_BANG_(context);

try{var result = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(execute_53608,args);
if(cljs.core.truth_((function (){var and__5140__auto__ = result;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.fn_QMARK_((result["finally"]));
} else {
return and__5140__auto__;
}
})())){
return result.finally((function (){
return knoxx.backend.agent_runtime.restore_agent_context_BANG_(previous);
}));
} else {
knoxx.backend.agent_runtime.restore_agent_context_BANG_(previous);

return result;
}
}catch (e53164){var err = e53164;
knoxx.backend.agent_runtime.restore_agent_context_BANG_(previous);

throw err;
}};
var G__53609 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__53612__i = 0, G__53612__a = new Array(arguments.length -  0);
while (G__53612__i < G__53612__a.length) {G__53612__a[G__53612__i] = arguments[G__53612__i + 0]; ++G__53612__i;}
  args = new cljs.core.IndexedSeq(G__53612__a,0,null);
} 
return G__53609__delegate.call(this,args);};
G__53609.cljs$lang$maxFixedArity = 0;
G__53609.cljs$lang$applyTo = (function (arglist__53613){
var args = cljs.core.seq(arglist__53613);
return G__53609__delegate(args);
});
G__53609.cljs$core$IFn$_invoke$arity$variadic = G__53609__delegate;
return G__53609;
})()
);
} else {
}

return tool;
});
knoxx.backend.agent_runtime.wrap_custom_tools_with_agent_context_BANG_ = (function knoxx$backend$agent_runtime$wrap_custom_tools_with_agent_context_BANG_(custom_tools,context){
if(cljs.core.truth_(custom_tools)){
var seq__53168_53614 = cljs.core.seq((cljs.core.truth_(cljs.core.array_QMARK_(custom_tools))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(custom_tools):cljs.core.PersistentVector.EMPTY));
var chunk__53169_53615 = null;
var count__53170_53616 = (0);
var i__53171_53617 = (0);
while(true){
if((i__53171_53617 < count__53170_53616)){
var tool_53618 = chunk__53169_53615.cljs$core$IIndexed$_nth$arity$2(null,i__53171_53617);
knoxx.backend.agent_runtime.wrap_tool_execute_with_agent_context_BANG_(tool_53618,context);


var G__53619 = seq__53168_53614;
var G__53620 = chunk__53169_53615;
var G__53621 = count__53170_53616;
var G__53622 = (i__53171_53617 + (1));
seq__53168_53614 = G__53619;
chunk__53169_53615 = G__53620;
count__53170_53616 = G__53621;
i__53171_53617 = G__53622;
continue;
} else {
var temp__5825__auto___53623 = cljs.core.seq(seq__53168_53614);
if(temp__5825__auto___53623){
var seq__53168_53624__$1 = temp__5825__auto___53623;
if(cljs.core.chunked_seq_QMARK_(seq__53168_53624__$1)){
var c__5673__auto___53625 = cljs.core.chunk_first(seq__53168_53624__$1);
var G__53626 = cljs.core.chunk_rest(seq__53168_53624__$1);
var G__53627 = c__5673__auto___53625;
var G__53628 = cljs.core.count(c__5673__auto___53625);
var G__53629 = (0);
seq__53168_53614 = G__53626;
chunk__53169_53615 = G__53627;
count__53170_53616 = G__53628;
i__53171_53617 = G__53629;
continue;
} else {
var tool_53630 = cljs.core.first(seq__53168_53624__$1);
knoxx.backend.agent_runtime.wrap_tool_execute_with_agent_context_BANG_(tool_53630,context);


var G__53631 = cljs.core.next(seq__53168_53624__$1);
var G__53632 = null;
var G__53633 = (0);
var G__53634 = (0);
seq__53168_53614 = G__53631;
chunk__53169_53615 = G__53632;
count__53170_53616 = G__53633;
i__53171_53617 = G__53634;
continue;
}
} else {
}
}
break;
}
} else {
}

return custom_tools;
});
/**
 * Return the eta-mu runtime name for a built-in/custom tool entry.
 * Built-ins are strings after eta-mu 0.70; custom tools are JS objects and may
 * have sanitized names such as discord_send with originalName=discord.send.
 */
knoxx.backend.agent_runtime.tool_runtime_name = (function knoxx$backend$agent_runtime$tool_runtime_name(tool){
if(typeof tool === 'string'){
var G__53189 = tool;
var G__53189__$1 = (((G__53189 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53189)));
var G__53189__$2 = (((G__53189__$1 == null))?null:clojure.string.trim(G__53189__$1));
if((G__53189__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53189__$2);
}
} else {
var or__5142__auto__ = (function (){var G__53194 = tool;
var G__53194__$1 = (((G__53194 == null))?null:(G__53194["name"]));
var G__53194__$2 = (((G__53194__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53194__$1)));
var G__53194__$3 = (((G__53194__$2 == null))?null:clojure.string.trim(G__53194__$2));
if((G__53194__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__53194__$3);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__53199 = tool;
var G__53199__$1 = (((G__53199 == null))?null:(G__53199["id"]));
var G__53199__$2 = (((G__53199__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53199__$1)));
var G__53199__$3 = (((G__53199__$2 == null))?null:clojure.string.trim(G__53199__$2));
if((G__53199__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__53199__$3);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__53202 = tool;
var G__53202__$1 = (((G__53202 == null))?null:(G__53202["label"]));
var G__53202__$2 = (((G__53202__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53202__$1)));
var G__53202__$3 = (((G__53202__$2 == null))?null:clojure.string.trim(G__53202__$2));
if((G__53202__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__53202__$3);
}
}
}

}
});
knoxx.backend.agent_runtime.enabled_tool_name_allowlist = (function knoxx$backend$agent_runtime$enabled_tool_name_allowlist(builtin_tools,custom_tools){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.tool_runtime_name,cljs.core.concat.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = builtin_tools;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(cljs.core.truth_(cljs.core.array_QMARK_(custom_tools))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(custom_tools):cljs.core.PersistentVector.EMPTY)))));
});
knoxx.backend.agent_runtime.path_resolve = (function knoxx$backend$agent_runtime$path_resolve(var_args){
var args__5882__auto__ = [];
var len__5876__auto___53637 = arguments.length;
var i__5877__auto___53638 = (0);
while(true){
if((i__5877__auto___53638 < len__5876__auto___53637)){
args__5882__auto__.push((arguments[i__5877__auto___53638]));

var G__53639 = (i__5877__auto___53638 + (1));
i__5877__auto___53638 = G__53639;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic = (function (node_path,parts){
var G__53212 = cljs.core.count(parts);
switch (G__53212) {
case (0):
return node_path.resolve();

break;
case (1):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)));

break;
case (2):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)));

break;
case (3):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)));

break;
case (4):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)));

break;
case (5):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)));

break;
case (6):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)));

break;
case (7):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(6)));

break;
default:
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(6)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(7)));

}
}));

(knoxx.backend.agent_runtime.path_resolve.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.backend.agent_runtime.path_resolve.cljs$lang$applyTo = (function (seq53204){
var G__53205 = cljs.core.first(seq53204);
var seq53204__$1 = cljs.core.next(seq53204);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__53205,seq53204__$1);
}));

knoxx.backend.agent_runtime.path_relative = (function knoxx$backend$agent_runtime$path_relative(node_path,from,to){
return node_path.relative(from,to);
});
knoxx.backend.agent_runtime.path_is_absolute_QMARK_ = (function knoxx$backend$agent_runtime$path_is_absolute_QMARK_(node_path,value){
return node_path.isAbsolute(value);
});
knoxx.backend.agent_runtime.configured_extra_root_records = (function knoxx$backend$agent_runtime$configured_extra_root_records(node_path,config){
var music_root = (function (){var G__53227 = new cljs.core.Keyword(null,"music-library-root","music-library-root",1834434652).cljs$core$IFn$_invoke$arity$1(config);
var G__53227__$1 = (((G__53227 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53227)));
var G__53227__$2 = (((G__53227__$1 == null))?null:clojure.string.trim(G__53227__$1));
if((G__53227__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53227__$2);
}
})();
var extra_roots = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (raw){
var G__53228 = raw;
var G__53228__$1 = (((G__53228 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53228)));
var G__53228__$2 = (((G__53228__$1 == null))?null:clojure.string.trim(G__53228__$1));
if((G__53228__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53228__$2);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"extra-workspace-roots","extra-workspace-roots",-21056439).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
return cljs.core.vec(cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,entry){
if(cljs.core.truth_(cljs.core.some((function (p1__53226_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"root","root",-448657453).cljs$core$IFn$_invoke$arity$1(p1__53226_SHARP_),new cljs.core.Keyword(null,"root","root",-448657453).cljs$core$IFn$_invoke$arity$1(entry));
}),acc))){
return acc;
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,entry);
}
}),cljs.core.PersistentVector.EMPTY,cljs.core.concat.cljs$core$IFn$_invoke$arity$2((cljs.core.truth_(music_root)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"alias","alias",-2039751630),"Music",new cljs.core.Keyword(null,"root","root",-448657453),knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([music_root], 0))], null)], null):null),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (raw_root){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"alias","alias",-2039751630),null,new cljs.core.Keyword(null,"root","root",-448657453),knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([raw_root], 0))], null);
}),extra_roots))));
});
knoxx.backend.agent_runtime.path_resolve = (function knoxx$backend$agent_runtime$path_resolve(var_args){
var args__5882__auto__ = [];
var len__5876__auto___53649 = arguments.length;
var i__5877__auto___53650 = (0);
while(true){
if((i__5877__auto___53650 < len__5876__auto___53649)){
args__5882__auto__.push((arguments[i__5877__auto___53650]));

var G__53651 = (i__5877__auto___53650 + (1));
i__5877__auto___53650 = G__53651;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic = (function (node_path,parts){
var G__53246 = cljs.core.count(parts);
switch (G__53246) {
case (0):
return node_path.resolve();

break;
case (1):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)));

break;
case (2):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)));

break;
case (3):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)));

break;
case (4):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)));

break;
case (5):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)));

break;
case (6):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)));

break;
case (7):
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(6)));

break;
default:
return node_path.resolve(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(0)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(1)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(2)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(3)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(4)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(5)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(6)),cljs.core.nth.cljs$core$IFn$_invoke$arity$2(parts,(7)));

}
}));

(knoxx.backend.agent_runtime.path_resolve.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.backend.agent_runtime.path_resolve.cljs$lang$applyTo = (function (seq53242){
var G__53243 = cljs.core.first(seq53242);
var seq53242__$1 = cljs.core.next(seq53242);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__53243,seq53242__$1);
}));

knoxx.backend.agent_runtime.path_relative = (function knoxx$backend$agent_runtime$path_relative(node_path,from,to){
return node_path.relative(from,to);
});
knoxx.backend.agent_runtime.path_is_absolute_QMARK_ = (function knoxx$backend$agent_runtime$path_is_absolute_QMARK_(node_path,value){
return node_path.isAbsolute(value);
});
knoxx.backend.agent_runtime.allowed_root_records = (function knoxx$backend$agent_runtime$allowed_root_records(node_path,config){
return cljs.core.vec(cljs.core.cons(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"alias","alias",-2039751630),null,new cljs.core.Keyword(null,"root","root",-448657453),knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config)], 0))], null),knoxx.backend.agent_runtime.configured_extra_root_records(node_path,config)));
});
knoxx.backend.agent_runtime.root_relative_path = (function knoxx$backend$agent_runtime$root_relative_path(node_path,root,candidate){
var rel = knoxx.backend.agent_runtime.path_relative(node_path,root,candidate);
if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_(rel,"..");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.agent_runtime.path_is_absolute_QMARK_(node_path,rel);
}
})())){
return null;
} else {
return rel;
}
});
knoxx.backend.agent_runtime.resolve_workspace_path = (function knoxx$backend$agent_runtime$resolve_workspace_path(_runtime,config,raw_path){
var node_path = shadow.esm.esm_import$node_path;
var requested = (function (){var G__53251 = raw_path;
var G__53251__$1 = (((G__53251 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53251)));
var G__53251__$2 = (((G__53251__$1 == null))?null:clojure.string.trim(G__53251__$1));
if((G__53251__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53251__$2);
}
})();
var roots = knoxx.backend.agent_runtime.allowed_root_records(node_path,config);
var music_root = cljs.core.some((function (p1__53249_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("Music",new cljs.core.Keyword(null,"alias","alias",-2039751630).cljs$core$IFn$_invoke$arity$1(p1__53249_SHARP_))){
return p1__53249_SHARP_;
} else {
return null;
}
}),roots);
var candidate = (cljs.core.truth_(knoxx.backend.agent_runtime.path_is_absolute_QMARK_(node_path,(function (){var or__5142__auto__ = requested;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))?knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([requested], 0)):(cljs.core.truth_((function (){var and__5140__auto__ = requested;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = music_root;
if(cljs.core.truth_(and__5140__auto____$1)){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(requested,"Music")) || (clojure.string.starts_with_QMARK_(requested,"Music/")));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())?(function (){var suffix = cljs.core.subs.cljs$core$IFn$_invoke$arity$2(requested,cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.count(requested),(("Music/").length)));
return knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"root","root",-448657453).cljs$core$IFn$_invoke$arity$1(music_root),suffix], 0));
})():knoxx.backend.agent_runtime.path_resolve.cljs$core$IFn$_invoke$arity$variadic(node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config),(function (){var or__5142__auto__ = requested;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()], 0))
));
var matched_root = cljs.core.some((function (root_record){
if(cljs.core.truth_(knoxx.backend.agent_runtime.root_relative_path(node_path,new cljs.core.Keyword(null,"root","root",-448657453).cljs$core$IFn$_invoke$arity$1(root_record),candidate))){
return root_record;
} else {
return null;
}
}),roots);
if(cljs.core.truth_(matched_root)){
} else {
throw (new Error("Path escapes allowed workspace roots"));
}

return candidate;
});
knoxx.backend.agent_runtime.ensure_sdk_runtime_BANG_ = (function knoxx$backend$agent_runtime$ensure_sdk_runtime_BANG_(_runtime,config){
var temp__5823__auto__ = cljs.core.deref(knoxx.backend.agent_runtime.sdk_runtime_STAR_);
if(cljs.core.truth_(temp__5823__auto__)){
var p = temp__5823__auto__;
return p;
} else {
var node_fs = shadow.esm.esm_import$node_fs$promises;
var node_path = shadow.esm.esm_import$node_path;
var runtime_dir = new cljs.core.Keyword(null,"agent-dir","agent-dir",-1644183343).cljs$core$IFn$_invoke$arity$1(config);
var models_file = node_path.join(runtime_dir,"models.json");
var auth_file = node_path.join(runtime_dir,"auth.json");
var extensions_dir = node_path.join(runtime_dir,"extensions");
var affinity_extension_file = node_path.join(extensions_dir,"proxx-session-affinity.ts");
var SettingsManager = (shadow.esm.esm_import$$open_hax$eta_mu["SettingsManager"]);
var AuthStorage = (shadow.esm.esm_import$$open_hax$eta_mu["AuthStorage"]);
var ModelRegistry = (shadow.esm.esm_import$$open_hax$eta_mu["ModelRegistry"]);
var DefaultResourceLoader = (shadow.esm.esm_import$$open_hax$eta_mu["DefaultResourceLoader"]);
var settings_manager = SettingsManager.inMemory(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"compaction","compaction",468381181),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"enabled","enabled",1195909756),false], null),new cljs.core.Keyword(null,"retry","retry",-614012896),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"enabled","enabled",1195909756),true,new cljs.core.Keyword(null,"maxRetries","maxRetries",1482254096),(1)], null)], null)));
var p = node_fs.mkdir(runtime_dir,({"recursive": true})).then((function (){
return knoxx.backend.agent_runtime.fetch_proxx_model_ids_BANG_(config).then((function (model_ids){
return node_fs.writeFile(models_file,JSON.stringify(cljs.core.clj__GT_js(knoxx.backend.runtime.models.models_config.cljs$core$IFn$_invoke$arity$2(config,model_ids)),null,(2)),"utf8").then((function (){
return null;
}));
}));
})).then((function (){
return node_fs.mkdir(extensions_dir,({"recursive": true})).then((function (){
return node_fs.writeFile(affinity_extension_file,knoxx.backend.agent_runtime.proxx_session_affinity_extension_code,"utf8");
}));
})).then((function (){
var auth_storage = AuthStorage.create(auth_file);
var _ = ((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)))?null:auth_storage.setRuntimeApiKey("proxx",new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)));
var ___$1 = (function (){var seq__53269 = cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"provider-auth-tokens","provider-auth-tokens",1365293080).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})());
var chunk__53270 = null;
var count__53271 = (0);
var i__53272 = (0);
while(true){
if((i__53272 < count__53271)){
var vec__53294 = chunk__53270.cljs$core$IIndexed$_nth$arity$2(null,i__53272);
var provider_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53294,(0),null);
var env_var = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53294,(1),null);
var provider_id_53660__$1 = (function (){var G__53297 = provider_id;
var G__53297__$1 = (((G__53297 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53297)));
var G__53297__$2 = (((G__53297__$1 == null))?null:clojure.string.trim(G__53297__$1));
if((G__53297__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53297__$2);
}
})();
var env_var_53661__$1 = (function (){var G__53299 = env_var;
var G__53299__$1 = (((G__53299 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53299)));
var G__53299__$2 = (((G__53299__$1 == null))?null:clojure.string.trim(G__53299__$1));
if((G__53299__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53299__$2);
}
})();
var token_53662 = (cljs.core.truth_(env_var_53661__$1)?(process.env[env_var_53661__$1]):null);
if(cljs.core.truth_((function (){var and__5140__auto__ = provider_id_53660__$1;
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof token_53662 === 'string') && ((!(clojure.string.blank_QMARK_(token_53662)))));
} else {
return and__5140__auto__;
}
})())){
auth_storage.setRuntimeApiKey(provider_id_53660__$1,token_53662);
} else {
}


var G__53663 = seq__53269;
var G__53664 = chunk__53270;
var G__53665 = count__53271;
var G__53666 = (i__53272 + (1));
seq__53269 = G__53663;
chunk__53270 = G__53664;
count__53271 = G__53665;
i__53272 = G__53666;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__53269);
if(temp__5825__auto__){
var seq__53269__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__53269__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__53269__$1);
var G__53667 = cljs.core.chunk_rest(seq__53269__$1);
var G__53668 = c__5673__auto__;
var G__53669 = cljs.core.count(c__5673__auto__);
var G__53670 = (0);
seq__53269 = G__53667;
chunk__53270 = G__53668;
count__53271 = G__53669;
i__53272 = G__53670;
continue;
} else {
var vec__53304 = cljs.core.first(seq__53269__$1);
var provider_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53304,(0),null);
var env_var = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53304,(1),null);
var provider_id_53671__$1 = (function (){var G__53307 = provider_id;
var G__53307__$1 = (((G__53307 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53307)));
var G__53307__$2 = (((G__53307__$1 == null))?null:clojure.string.trim(G__53307__$1));
if((G__53307__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53307__$2);
}
})();
var env_var_53672__$1 = (function (){var G__53308 = env_var;
var G__53308__$1 = (((G__53308 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53308)));
var G__53308__$2 = (((G__53308__$1 == null))?null:clojure.string.trim(G__53308__$1));
if((G__53308__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53308__$2);
}
})();
var token_53673 = (cljs.core.truth_(env_var_53672__$1)?(process.env[env_var_53672__$1]):null);
if(cljs.core.truth_((function (){var and__5140__auto__ = provider_id_53671__$1;
if(cljs.core.truth_(and__5140__auto__)){
return ((typeof token_53673 === 'string') && ((!(clojure.string.blank_QMARK_(token_53673)))));
} else {
return and__5140__auto__;
}
})())){
auth_storage.setRuntimeApiKey(provider_id_53671__$1,token_53673);
} else {
}


var G__53675 = cljs.core.next(seq__53269__$1);
var G__53676 = null;
var G__53677 = (0);
var G__53678 = (0);
seq__53269 = G__53675;
chunk__53270 = G__53676;
count__53271 = G__53677;
i__53272 = G__53678;
continue;
}
} else {
return null;
}
}
break;
}
})();
var model_registry = (new ModelRegistry(auth_storage,models_file));
var loader = (new DefaultResourceLoader(({"cwd": new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config), "agentDir": runtime_dir, "settingsManager": settings_manager})));
return loader.reload().then((function (){
return ({"authStorage": auth_storage, "modelRegistry": model_registry, "settingsManager": settings_manager, "loader": loader, "runtimeDir": runtime_dir});
}));
})).catch((function (err){
cljs.core.reset_BANG_(knoxx.backend.agent_runtime.sdk_runtime_STAR_,null);

return Promise.reject(err);
}));
cljs.core.reset_BANG_(knoxx.backend.agent_runtime.sdk_runtime_STAR_,p);

return p;
}
});
knoxx.backend.agent_runtime.create_agent_session_BANG_ = (function knoxx$backend$agent_runtime$create_agent_session_BANG_(var_args){
var G__53314 = arguments.length;
switch (G__53314) {
case 4:
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
case 7:
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]));

break;
case 8:
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]),(arguments[(7)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,conversation_id,model_id){
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,config,conversation_id,model_id,null,new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config));
}));

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (runtime,config,conversation_id,model_id,auth_context){
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,config,conversation_id,model_id,auth_context,new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config));
}));

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level){
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7(runtime,config,conversation_id,model_id,auth_context,thinking_level,null);
}));

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id){
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id,null);
}));

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id,agent_spec){
return knoxx.backend.agent_runtime.ensure_sdk_runtime_BANG_(runtime,config).then((function (sdk_runtime){
var SessionManager = (shadow.esm.esm_import$$open_hax$eta_mu["SessionManager"]);
var createAgentSession = (shadow.esm.esm_import$$open_hax$eta_mu["createAgentSession"]);
var model_registry = (sdk_runtime["modelRegistry"]);
var auth_storage = (sdk_runtime["authStorage"]);
var loader = (sdk_runtime["loader"]);
var settings_manager = (sdk_runtime["settingsManager"]);
var thinking_level__$1 = knoxx.backend.runtime.models.effective_thinking_level(config,model_id,(function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_thinking_level(thinking_level);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = thinking_level;
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
var model_provider_id = (function (){var or__5142__auto__ = (function (){var G__53323 = knoxx.backend.runtime.models.resolve_model_contract(config,model_id);
if((G__53323 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"provider","provider",-302056900).cljs$core$IFn$_invoke$arity$1(G__53323);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "proxx";
}
})();
var model = (function (){var or__5142__auto__ = model_registry.find((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_provider_id)),model_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = model_registry.find("proxx",model_id);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return model_registry.find("proxx",new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config));
}
}
})();
var allowed_tool_ids = knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec),auth_context,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
var tool_auth_context = knoxx.backend.agent_runtime.effective_tool_auth_context(auth_context,allowed_tool_ids);
var builtin_tools = knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$6(runtime,config,tool_auth_context,new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
var custom_tools = knoxx.backend.agent_runtime.wrap_custom_tools_with_agent_context_BANG_(knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5(runtime,config,tool_auth_context,agent_spec,allowed_tool_ids),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),agent_spec], null));
var tool_name_allowlist = knoxx.backend.agent_runtime.enabled_tool_name_allowlist(builtin_tools,custom_tools);
var create_session = (function (session_manager){
return (function (){var G__53327 = ({"resourceLoader": loader, "sessionManager": session_manager, "agentDir": (sdk_runtime["runtimeDir"]), "tools": cljs.core.clj__GT_js(tool_name_allowlist), "authStorage": auth_storage, "cwd": new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config), "customTools": custom_tools, "settingsManager": settings_manager, "modelRegistry": model_registry, "thinkingLevel": thinking_level__$1, "model": model});
return (createAgentSession.cljs$core$IFn$_invoke$arity$1 ? createAgentSession.cljs$core$IFn$_invoke$arity$1(G__53327) : createAgentSession.call(null,G__53327));
})().then((function (result){
var session = (result["session"]);
session.setThinkingLevel(thinking_level__$1);

if(cljs.core.fn_QMARK_((function (){var G__53329 = session;
var G__53329__$1 = (((G__53329 == null))?null:(G__53329["agent"]));
if((G__53329__$1 == null)){
return null;
} else {
return (G__53329__$1["setAfterToolCall"]);
}
})())){
(session["agent"]).setAfterToolCall((function (ctx,_signal){
var result__$1 = (ctx["result"]);
var details = (cljs.core.truth_(result__$1)?(result__$1["details"]):null);
var raw_parts = (function (){var or__5142__auto__ = (cljs.core.truth_(details)?(details["content_parts"]):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (cljs.core.truth_(details)?(details["contentParts"]):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return [];
}
}
})();
var img_parts = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53310_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("image",(function (){var G__53330 = (p1__53310_SHARP_["type"]);
var G__53330__$1 = (((G__53330 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53330)));
if((G__53330__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__53330__$1);
}
})());
}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw_parts)));
var fetch_b64_BANG_ = (function (url){
return fetch(url).then((function (r){
if(cljs.core.truth_(r.ok)){
} else {
throw (new Error((""+"img fetch failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(r.status))));
}

return r.arrayBuffer;
})).then((function (ab){
var buf = Buffer.from(ab);
return (""+"data:image/png;base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(buf.toString("base64")));
}));
});
var materialize_BANG_ = (function (part){
var url = (function (){var G__53331 = (part["url"]);
var G__53331__$1 = (((G__53331 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53331)));
if((G__53331__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53331__$1);
}
})();
var data = (function (){var G__53332 = (part["data"]);
var G__53332__$1 = (((G__53332 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53332)));
if((G__53332__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53332__$1);
}
})();
var mime = (function (){var or__5142__auto__ = (function (){var G__53341 = (part["mimeType"]);
var G__53341__$1 = (((G__53341 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53341)));
if((G__53341__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__53341__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "image/png";
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = data;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_(data,"data:");
} else {
return and__5140__auto__;
}
})())){
return Promise.resolve((function (){var comma = data.indexOf(",");
return ({"type": "image", "data": (((comma >= (0)))?data.slice((comma + (1))):data), "mimeType": mime});
})());
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = data;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.starts_with_QMARK_(data,"http")));
} else {
return and__5140__auto__;
}
})())){
return Promise.resolve(({"type": "image", "data": data, "mimeType": mime}));
} else {
if(cljs.core.truth_(url)){
return fetch_b64_BANG_(url).then((function (data_url){
var comma = data_url.indexOf(",");
return ({"type": "image", "data": (((comma >= (0)))?data_url.slice((comma + (1))):data_url), "mimeType": mime});
}));
} else {
return Promise.resolve(null);

}
}
}
});
if(cljs.core.seq(img_parts)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(materialize_BANG_,img_parts))).then((function (materialized){
var good = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(materialized)));
if(cljs.core.seq(good)){
var existing = (function (){var or__5142__auto__ = (function (){var G__53352 = result__$1;
if((G__53352 == null)){
return null;
} else {
return (G__53352["content"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var merged = cljs.core.clj__GT_js(cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(existing)),good));
return ({"content": merged});
} else {
return null;
}
})).catch((function (_){
return null;
}));
} else {
return Promise.resolve(null);
}
}));
} else {
}

return session;
}));
});
if(knoxx.backend.http.no_content_QMARK_(model)){
return Promise.reject((new Error((""+"No eta-mu model configured for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)))));
} else {
var session_manager = SessionManager.inMemory(new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config));
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
} else {
session_manager.newSession(({"id": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id))}));
}

session_manager.appendModelChange((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_provider_id)),model_id);

session_manager.appendThinkingLevelChange(thinking_level__$1);

return knoxx.backend.agent_runtime.rehydrate_session_manager_from_redis_BANG_.cljs$core$IFn$_invoke$arity$5(config,session_manager,conversation_id,session_id,agent_spec).then((function (result){
return create_session((result["sessionManager"]));
}));
}
}));
}));

(knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$lang$maxFixedArity = 8);

knoxx.backend.agent_runtime.visible_session_signature = (function knoxx$backend$agent_runtime$visible_session_signature(runtime,config,auth_context,agent_spec){
var allowed_tool_ids = knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec),auth_context,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
var tool_auth_context = knoxx.backend.agent_runtime.effective_tool_auth_context(auth_context,allowed_tool_ids);
var builtin_tools = (function (){var or__5142__auto__ = knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$6(runtime,config,tool_auth_context,new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var custom_tools = (function (){var temp__5823__auto__ = knoxx.backend.agent_hydration.create_agent_custom_tools.cljs$core$IFn$_invoke$arity$5(runtime,config,tool_auth_context,agent_spec,allowed_tool_ids);
if(cljs.core.truth_(temp__5823__auto__)){
var tools = temp__5823__auto__;
if(cljs.core.truth_(cljs.core.array_QMARK_(tools))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(tools);
} else {
return cljs.core.PersistentVector.EMPTY;
}
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
return cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"tools","tools",-1241731990),cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_runtime.tool_runtime_name,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(builtin_tools,custom_tools))))),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),(function (){var G__53365 = new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53365__$1 = (((G__53365 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53365)));
var G__53365__$2 = (((G__53365__$1 == null))?null:clojure.string.trim(G__53365__$1));
if((G__53365__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53365__$2);
}
})(),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),(function (){var G__53366 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53366__$1 = (((G__53366 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53366)));
var G__53366__$2 = (((G__53366__$1 == null))?null:clojure.string.trim(G__53366__$1));
if((G__53366__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53366__$2);
}
})(),new cljs.core.Keyword(null,"role","role",-736691072),(function (){var G__53367 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53367__$1 = (((G__53367 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53367)));
var G__53367__$2 = (((G__53367__$1 == null))?null:clojure.string.trim(G__53367__$1));
if((G__53367__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53367__$2);
}
})(),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),(function (){var G__53369 = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53369__$1 = (((G__53369 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53369)));
var G__53369__$2 = (((G__53369__$1 == null))?null:clojure.string.trim(G__53369__$1));
if((G__53369__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53369__$2);
}
})(),new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),(function (){var G__53370 = new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53370__$1 = (((G__53370 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53370)));
var G__53370__$2 = (((G__53370__$1 == null))?null:clojure.string.trim(G__53370__$1));
if((G__53370__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53370__$2);
}
})()], null)], 0));
});
knoxx.backend.agent_runtime.register_actor_live_route_BANG_ = (function knoxx$backend$agent_runtime$register_actor_live_route_BANG_(runtime,conversation_id,session_id,agent_spec){
var temp__5825__auto__ = (function (){var G__53372 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53372__$1 = (((G__53372 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53372)));
var G__53372__$2 = (((G__53372__$1 == null))?null:clojure.string.trim(G__53372__$1));
if((G__53372__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53372__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var actor_id = temp__5825__auto__;
return knoxx.backend.actor_mailbox.register_live_session_BANG_(runtime,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),(function (){var G__53374 = new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53374__$1 = (((G__53374 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53374)));
var G__53374__$2 = (((G__53374__$1 == null))?null:clojure.string.trim(G__53374__$1));
if((G__53374__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53374__$2);
}
})(),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"registeredBy","registeredBy",1921868146),"agent-runtime",new cljs.core.Keyword(null,"contractId","contractId",710260199),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec)], null)], null)).catch((function (err){
return console.warn("[actor-mailbox] failed to register live actor route",err.message);
}));
} else {
return null;
}
});
knoxx.backend.agent_runtime.ensure_agent_session_BANG_ = (function knoxx$backend$agent_runtime$ensure_agent_session_BANG_(var_args){
var G__53381 = arguments.length;
switch (G__53381) {
case 4:
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
case 7:
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]));

break;
case 8:
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]),(arguments[(7)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,conversation_id,model_id){
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,config,conversation_id,model_id,null,new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config));
}));

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (runtime,config,conversation_id,model_id,auth_context){
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,config,conversation_id,model_id,auth_context,new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config));
}));

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$6 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level){
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7(runtime,config,conversation_id,model_id,auth_context,thinking_level,null);
}));

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$7 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id){
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id,null);
}));

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8 = (function (runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id,agent_spec){
var thinking_level__$1 = knoxx.backend.runtime.models.effective_thinking_level(config,model_id,(function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_thinking_level(thinking_level);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = thinking_level;
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
var current_tool_signature = knoxx.backend.agent_runtime.visible_session_signature(runtime,config,auth_context,agent_spec);
var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.agent_runtime.agent_sessions_STAR_),conversation_id);
if(cljs.core.truth_(temp__5823__auto__)){
var entry = temp__5823__auto__;
var session = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(entry);
var active_model = new cljs.core.Keyword(null,"model-id","model-id",-467101728).cljs$core$IFn$_invoke$arity$1(entry);
var active_tool_signature = new cljs.core.Keyword(null,"tool-signature","tool-signature",1310639957).cljs$core$IFn$_invoke$arity$1(entry);
if((((!((session == null)))) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(active_model)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = active_tool_signature;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = current_tool_signature;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))))))){
session.setThinkingLevel(thinking_level__$1);

knoxx.backend.agent_runtime.register_actor_live_route_BANG_(runtime,conversation_id,session_id,agent_spec);

return Promise.resolve(session);
} else {
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id,model_id,auth_context,thinking_level__$1,session_id,agent_spec).then((function (next_session){
var ctx_53722 = knoxx.backend.extension_runtime.build_extension_ctx.cljs$core$IFn$_invoke$arity$variadic(runtime,config,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"model-id","model-id",-467101728),model_id,new cljs.core.Keyword(null,"auth-context","auth-context",320032325),auth_context], 0));
knoxx.backend.extension_runtime.dispatch_event("session_switch",({"conversationId": conversation_id, "sessionId": session_id}),ctx_53722);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.agent_runtime.agent_sessions_STAR_,cljs.core.assoc,conversation_id,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"session","session",1008279103),next_session,new cljs.core.Keyword(null,"model-id","model-id",-467101728),model_id,new cljs.core.Keyword(null,"tool-signature","tool-signature",1310639957),current_tool_signature,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec)], null));

knoxx.backend.agent_runtime.register_actor_live_route_BANG_(runtime,conversation_id,session_id,agent_spec);

return next_session;
}));
}
} else {
return knoxx.backend.agent_runtime.create_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id,model_id,auth_context,thinking_level__$1,session_id,agent_spec).then((function (session){
var ctx_53723 = knoxx.backend.extension_runtime.build_extension_ctx.cljs$core$IFn$_invoke$arity$variadic(runtime,config,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"model-id","model-id",-467101728),model_id,new cljs.core.Keyword(null,"auth-context","auth-context",320032325),auth_context], 0));
knoxx.backend.extension_runtime.dispatch_event("session_start",({"conversationId": conversation_id, "sessionId": session_id}),ctx_53723);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.agent_runtime.agent_sessions_STAR_,cljs.core.assoc,conversation_id,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"session","session",1008279103),session,new cljs.core.Keyword(null,"model-id","model-id",-467101728),model_id,new cljs.core.Keyword(null,"tool-signature","tool-signature",1310639957),current_tool_signature,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec)], null));

knoxx.backend.agent_runtime.register_actor_live_route_BANG_(runtime,conversation_id,session_id,agent_spec);

return session;
}));
}
}));

(knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$lang$maxFixedArity = 8);

knoxx.backend.agent_runtime.active_agent_session = (function knoxx$backend$agent_runtime$active_agent_session(conversation_id){
return new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.agent_runtime.agent_sessions_STAR_),conversation_id));
});
/**
 * Dispatch session_shutdown to extensions, then release the in-process session entry.
 * Redis/OpenPlanner rehydration remains the fallback path across restarts or instance changes.
 */
knoxx.backend.agent_runtime.remove_agent_session_BANG_ = (function knoxx$backend$agent_runtime$remove_agent_session_BANG_(conversation_id){
var temp__5825__auto___53724 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.agent_runtime.agent_sessions_STAR_),conversation_id);
if(cljs.core.truth_(temp__5825__auto___53724)){
var entry_53725 = temp__5825__auto___53724;
var ctx_53726 = knoxx.backend.extension_runtime.build_extension_ctx.cljs$core$IFn$_invoke$arity$variadic(({}),cljs.core.PersistentArrayMap.EMPTY,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(entry_53725)], 0));
knoxx.backend.extension_runtime.dispatch_event("session_shutdown",({"conversationId": conversation_id}),ctx_53726);
} else {
}

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.agent_runtime.agent_sessions_STAR_,cljs.core.dissoc,conversation_id);

return null;
});
knoxx.backend.agent_runtime.queue_agent_control_BANG_ = (function knoxx$backend$agent_runtime$queue_agent_control_BANG_(_runtime,_config,p__53403){
var map__53404 = p__53403;
var map__53404__$1 = cljs.core.__destructure_map(map__53404);
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
var metadata = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53404__$1,new cljs.core.Keyword(null,"metadata","metadata",1799301597));
if(clojure.string.blank_QMARK_(conversation_id)){
return Promise.reject((new Error("conversation_id is required for live controls")));
} else {
if(clojure.string.blank_QMARK_(message)){
return Promise.reject((new Error("message is required for live controls")));
} else {
var temp__5823__auto__ = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
if(cljs.core.truth_(temp__5823__auto__)){
var session = temp__5823__auto__;
if((!((session["isStreaming"]) === true))){
return Promise.reject((new Error("No active running turn is available for live controls")));
} else {
var preview = (((cljs.core.count(message) > (240)))?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(message,(0),(240)))+"\u2026"):message);
var event_type = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"follow_up"))?"follow_up_queued":"steer_queued");
var failure_type = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"follow_up"))?"follow_up_failed":"steer_failed");
var metadata__$1 = (function (){var or__5142__auto__ = metadata;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var invoke = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"follow_up"))?(function (){
return session.followUp(message);
}):(function (){
return session.steer(message);
}));
return invoke().then((function (){
var event = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,event_type,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"queued",new cljs.core.Keyword(null,"preview","preview",451279890),preview,new cljs.core.Keyword(null,"metadata","metadata",1799301597),metadata__$1], null));
if(cljs.core.truth_(run_id)){
knoxx.backend.run_state.append_run_event_BANG_(run_id,event);
} else {
}

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",event);

return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"kind","kind",-717265803),kind], null);
})).catch((function (err){
var event_53735 = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,failure_type,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"preview","preview",451279890),preview,new cljs.core.Keyword(null,"metadata","metadata",1799301597),metadata__$1], null));
if(cljs.core.truth_(run_id)){
knoxx.backend.run_state.append_run_event_BANG_(run_id,event_53735);
} else {
}

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",event_53735);

throw err;
}));
}
} else {
return Promise.reject((new Error("Conversation is not active in the agent runtime")));
}

}
}
});

//# sourceMappingURL=knoxx.backend.agent_runtime.js.map
