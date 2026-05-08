import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.eta_mu_session_ingester');
knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSIONS_ROOT = (function (){var or__5142__auto__ = (process.env["ETA_MU_SESSIONS_ROOT"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/home/err/.\u03B7\u03BC/agent/sessions";
}
})();
knoxx.backend.eta_mu_session_ingester.INGEST_STATE_DIR = (function (){var or__5142__auto__ = (process.env["INGEST_STATE_DIR"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/home/err/.knoxx/eta-mu-ingest-state";
}
})();
knoxx.backend.eta_mu_session_ingester.INGEST_STATE_FILE = shadow.esm.esm_import$node_path.join(knoxx.backend.eta_mu_session_ingester.INGEST_STATE_DIR,"ingested-sessions.json");
knoxx.backend.eta_mu_session_ingester.MAX_EVENTS_PER_BATCH = (200);
knoxx.backend.eta_mu_session_ingester.MAX_TEXT_LENGTH = (12000);
knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSION_PROJECT = (function (){var or__5142__auto__ = (process.env["ETA_MU_SESSION_PROJECT"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})();
knoxx.backend.eta_mu_session_ingester.SUPPORTED_EVENT_TYPES = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, ["message",null,"thinking_level_change",null,"branch_summary",null,"model_change",null,"compaction",null,"session",null,"custom_message",null], null), null);
knoxx.backend.eta_mu_session_ingester.obj_get = (function knoxx$backend$eta_mu_session_ingester$obj_get(obj,key){
return (obj[key]);
});
knoxx.backend.eta_mu_session_ingester.ensure_state_dir = (function knoxx$backend$eta_mu_session_ingester$ensure_state_dir(){
return shadow.esm.esm_import$node_fs$promises.mkdir(knoxx.backend.eta_mu_session_ingester.INGEST_STATE_DIR,({"recursive": true}));
});
knoxx.backend.eta_mu_session_ingester.load_ingest_state = (function knoxx$backend$eta_mu_session_ingester$load_ingest_state(){
return shadow.esm.esm_import$node_fs$promises.readFile(knoxx.backend.eta_mu_session_ingester.INGEST_STATE_FILE,"utf-8").then((function (raw){
return JSON.parse(raw);
})).catch((function (_){
return ({"sessions": Object.create(null)});
}));
});
knoxx.backend.eta_mu_session_ingester.save_ingest_state = (function knoxx$backend$eta_mu_session_ingester$save_ingest_state(state){
return knoxx.backend.eta_mu_session_ingester.ensure_state_dir().then((function (_){
return shadow.esm.esm_import$node_fs$promises.writeFile(knoxx.backend.eta_mu_session_ingester.INGEST_STATE_FILE,JSON.stringify(state,null,(2)),"utf-8");
}));
});
knoxx.backend.eta_mu_session_ingester.discover_session_files = (function knoxx$backend$eta_mu_session_ingester$discover_session_files(since_ts){
var since_ts__$1 = (function (){var or__5142__auto__ = since_ts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
return shadow.esm.esm_import$node_fs$promises.readdir(knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSIONS_ROOT).then((function (dirs){
return Promise.all(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (dir){
var dir_path = shadow.esm.esm_import$node_path.join(knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSIONS_ROOT,dir);
return shadow.esm.esm_import$node_fs$promises.readdir(dir_path).then((function (entries){
return Promise.all(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (entry){
if(cljs.core.truth_(entry.endsWith(".jsonl"))){
var file_path = shadow.esm.esm_import$node_path.join(dir_path,entry);
return shadow.esm.esm_import$node_fs$promises.stat(file_path).then((function (s){
if((knoxx.backend.eta_mu_session_ingester.obj_get(s,"mtimeMs") > since_ts__$1)){
var match = entry.match(/^[\dT:-]+_(.+)\.jsonl$/);
var session_id = (cljs.core.truth_(match)?(match[(1)]):entry.replace(/\.jsonl$/,""));
return ({"dir": dir, "path": file_path, "sessionId": session_id, "mtime": knoxx.backend.eta_mu_session_ingester.obj_get(s,"mtimeMs"), "size": knoxx.backend.eta_mu_session_ingester.obj_get(s,"size")});
} else {
return null;
}
})).catch((function (_){
return null;
}));
} else {
return null;
}
}),entries));
})).catch((function (_){
return [];
}));
}),dirs));
})).then((function (dir_results){
var flat = cljs.core.filterv(cljs.core.some_QMARK_,cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,r){
if(cljs.core.truth_(cljs.core.array_QMARK_(r))){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(acc,Array.from(r));
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,r);
}
}),cljs.core.PersistentVector.EMPTY,Array.from(dir_results)));
return flat.sort((function (a,b){
return (knoxx.backend.eta_mu_session_ingester.obj_get(a,"mtime") - knoxx.backend.eta_mu_session_ingester.obj_get(b,"mtime"));
}));
})).catch((function (_){
return [];
}));
});
knoxx.backend.eta_mu_session_ingester.truncate_text = (function knoxx$backend$eta_mu_session_ingester$truncate_text(text,max_len){
var max_len__$1 = (function (){var or__5142__auto__ = max_len;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.eta_mu_session_ingester.MAX_TEXT_LENGTH;
}
})();
if(((cljs.core.not(text)) || ((text.length <= max_len__$1)))){
return text;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text.slice((0),max_len__$1))+"\n... [truncated "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((text.length - max_len__$1))+" chars]");
}
});
knoxx.backend.eta_mu_session_ingester.extract_text_from_content = (function knoxx$backend$eta_mu_session_ingester$extract_text_from_content(content){
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (c){
return knoxx.backend.eta_mu_session_ingester.obj_get(c,"text");
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (c){
var and__5140__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.eta_mu_session_ingester.obj_get(c,"type"),"text");
if(and__5140__auto__){
return knoxx.backend.eta_mu_session_ingester.obj_get(c,"text");
} else {
return and__5140__auto__;
}
}),Array.from(content))));
} else {
return null;
}
});
knoxx.backend.eta_mu_session_ingester.extract_tool_calls = (function knoxx$backend$eta_mu_session_ingester$extract_tool_calls(content){
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (c){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.eta_mu_session_ingester.obj_get(c,"type"),"toolCall");
}),Array.from(content));
} else {
return null;
}
});
knoxx.backend.eta_mu_session_ingester.extract_thinking = (function knoxx$backend$eta_mu_session_ingester$extract_thinking(content){
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (c){
return knoxx.backend.eta_mu_session_ingester.obj_get(c,"text");
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (c){
var and__5140__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.eta_mu_session_ingester.obj_get(c,"type"),"thinking");
if(and__5140__auto__){
return knoxx.backend.eta_mu_session_ingester.obj_get(c,"text");
} else {
return and__5140__auto__;
}
}),Array.from(content))));
} else {
return null;
}
});
knoxx.backend.eta_mu_session_ingester.cwd_to_project = (function knoxx$backend$eta_mu_session_ingester$cwd_to_project(cwd){
if(cljs.core.not(cwd)){
return "eta-mu";
} else {
var normalized = cwd.replace(/^\/home\/[^\/]+\/devel\//,"").replace(/^\/home\/[^\/]+\//,"").replace(/^\//,"");
if(clojure.string.blank_QMARK_(normalized)){
return "eta-mu";
} else {
return normalized;
}
}
});
knoxx.backend.eta_mu_session_ingester.make_event = (function knoxx$backend$eta_mu_session_ingester$make_event(session_id,kind,ts,text,meta,extra){
var evt = ({"schema": "openplanner.event.v1", "meta": meta, "extra": extra, "source": "eta-mu-session-ingester", "ts": ts, "source_ref": ({"project": knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSION_PROJECT, "session": session_id}), "id": (""+"eta-mu:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kind)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(extra.id)), "kind": (""+"eta-mu."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kind)), "text": knoxx.backend.eta_mu_session_ingester.truncate_text(text,null)});
return evt;
});
knoxx.backend.eta_mu_session_ingester.map_session_event = (function knoxx$backend$eta_mu_session_ingester$map_session_event(eta_mu_event,session_id,cwd){
var id = (function (){var or__5142__auto__ = eta_mu_event.id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})();
return [knoxx.backend.eta_mu_session_ingester.make_event(session_id,"session_start",eta_mu_event.timestamp,(""+"eta-mu session started in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cwd)),({"role": "system", "author": "eta-mu", "eta_mu_session_version": eta_mu_event.version, "eta_mu_cwd": cwd}),({"id": id, "eta_mu_session_id": eta_mu_event.id, "eta_mu_version": eta_mu_event.version, "workspace": cwd, "eta_mu_workspace_project": knoxx.backend.eta_mu_session_ingester.cwd_to_project(cwd)}))];
});
knoxx.backend.eta_mu_session_ingester.map_model_change_event = (function knoxx$backend$eta_mu_session_ingester$map_model_change_event(eta_mu_event,session_id){
return [knoxx.backend.eta_mu_session_ingester.make_event(session_id,"model_change",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),(""+"Model: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"provider");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"modelId");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),({"role": "system", "author": "eta-mu"}),({"id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "provider": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"provider"), "model_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"modelId")}))];
});
knoxx.backend.eta_mu_session_ingester.map_compaction_event = (function knoxx$backend$eta_mu_session_ingester$map_compaction_event(eta_mu_event,session_id,cwd){
var summary = (function (){var or__5142__auto__ = eta_mu_event.summary;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(summary)){
return [];
} else {
return [knoxx.backend.eta_mu_session_ingester.make_event(session_id,"compaction",eta_mu_event.timestamp,summary,({"role": "system", "author": "eta-mu"}),({"id": eta_mu_event.id, "compaction": true, "eta_mu_workspace_project": knoxx.backend.eta_mu_session_ingester.cwd_to_project(cwd)}))];
}
});
knoxx.backend.eta_mu_session_ingester.map_custom_message_event = (function knoxx$backend$eta_mu_session_ingester$map_custom_message_event(eta_mu_event,session_id,cwd){
var content = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"content");
if(((cljs.core.not(content)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"display"),false)))){
return [];
} else {
return [knoxx.backend.eta_mu_session_ingester.make_event(session_id,(""+"custom."+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"customType");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())),knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),((typeof content === 'string')?content:JSON.stringify(content)),({"role": "system", "author": "eta-mu"}),({"id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "custom_type": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"customType"), "eta_mu_workspace_project": knoxx.backend.eta_mu_session_ingester.cwd_to_project(cwd)}))];
}
});
knoxx.backend.eta_mu_session_ingester.map_user_message = (function knoxx$backend$eta_mu_session_ingester$map_user_message(eta_mu_event,session_id,msg,cwd){
var text = knoxx.backend.eta_mu_session_ingester.extract_text_from_content(knoxx.backend.eta_mu_session_ingester.obj_get(msg,"content"));
if(clojure.string.blank_QMARK_(text)){
return [];
} else {
return [knoxx.backend.eta_mu_session_ingester.make_event(session_id,"message",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),text,({"role": "user", "author": "user"}),({"id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "eta_mu_message_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "eta_mu_parent_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"parentId"), "eta_mu_workspace_project": knoxx.backend.eta_mu_session_ingester.cwd_to_project(cwd)}))];
}
});
knoxx.backend.eta_mu_session_ingester.map_assistant_message = (function knoxx$backend$eta_mu_session_ingester$map_assistant_message(eta_mu_event,session_id,msg,cwd){
var text = knoxx.backend.eta_mu_session_ingester.extract_text_from_content(knoxx.backend.eta_mu_session_ingester.obj_get(msg,"content"));
var thinking = knoxx.backend.eta_mu_session_ingester.extract_thinking(knoxx.backend.eta_mu_session_ingester.obj_get(msg,"content"));
var tool_calls = knoxx.backend.eta_mu_session_ingester.extract_tool_calls(knoxx.backend.eta_mu_session_ingester.obj_get(msg,"content"));
var model = (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(msg,"model");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.eta_mu_session_ingester.obj_get(msg,"provider");
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown";
}
}
})();
var events = [];
if((!(clojure.string.blank_QMARK_(text)))){
events.push(knoxx.backend.eta_mu_session_ingester.make_event(session_id,"message",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),text,({"role": "assistant", "author": "eta-mu", "model": model}),({"id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "eta_mu_message_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "eta_mu_parent_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"parentId"), "provider": knoxx.backend.eta_mu_session_ingester.obj_get(msg,"provider"), "model": knoxx.backend.eta_mu_session_ingester.obj_get(msg,"model"), "usage": (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(msg,"usage");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "stop_reason": (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(msg,"stopReason");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "eta_mu_workspace_project": knoxx.backend.eta_mu_session_ingester.cwd_to_project(cwd)})));
} else {
}

if((!(clojure.string.blank_QMARK_(thinking)))){
events.push(knoxx.backend.eta_mu_session_ingester.make_event(session_id,"reasoning",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),thinking,({"role": "system", "author": "eta-mu", "model": model}),({"id": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"))+"-thinking"), "eta_mu_message_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id")})));
} else {
}

var seq__53337_53491 = cljs.core.seq(Array.from(tool_calls));
var chunk__53338_53492 = null;
var count__53339_53493 = (0);
var i__53340_53494 = (0);
while(true){
if((i__53340_53494 < count__53339_53493)){
var tc_53495 = chunk__53338_53492.cljs$core$IIndexed$_nth$arity$2(null,i__53340_53494);
var tool_name_53496 = (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(tc_53495,"name");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})();
var args_preview_53497 = (cljs.core.truth_(knoxx.backend.eta_mu_session_ingester.obj_get(tc_53495,"arguments"))?knoxx.backend.eta_mu_session_ingester.truncate_text(JSON.stringify(knoxx.backend.eta_mu_session_ingester.obj_get(tc_53495,"arguments")),(2000)):null);
events.push(knoxx.backend.eta_mu_session_ingester.make_event(session_id,"tool_call",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),knoxx.backend.eta_mu_session_ingester.truncate_text((""+"Tool: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name_53496)+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = args_preview_53497;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),null),({"role": "system", "author": "eta-mu", "model": model}),({"id": (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(tc_53495,"id");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id");
}
})(), "eta_mu_message_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "tool_name": tool_name_53496, "tool_call_id": knoxx.backend.eta_mu_session_ingester.obj_get(tc_53495,"id"), "tool_arguments_preview": args_preview_53497})));


var G__53507 = seq__53337_53491;
var G__53508 = chunk__53338_53492;
var G__53509 = count__53339_53493;
var G__53510 = (i__53340_53494 + (1));
seq__53337_53491 = G__53507;
chunk__53338_53492 = G__53508;
count__53339_53493 = G__53509;
i__53340_53494 = G__53510;
continue;
} else {
var temp__5825__auto___53511 = cljs.core.seq(seq__53337_53491);
if(temp__5825__auto___53511){
var seq__53337_53512__$1 = temp__5825__auto___53511;
if(cljs.core.chunked_seq_QMARK_(seq__53337_53512__$1)){
var c__5673__auto___53513 = cljs.core.chunk_first(seq__53337_53512__$1);
var G__53514 = cljs.core.chunk_rest(seq__53337_53512__$1);
var G__53515 = c__5673__auto___53513;
var G__53516 = cljs.core.count(c__5673__auto___53513);
var G__53517 = (0);
seq__53337_53491 = G__53514;
chunk__53338_53492 = G__53515;
count__53339_53493 = G__53516;
i__53340_53494 = G__53517;
continue;
} else {
var tc_53521 = cljs.core.first(seq__53337_53512__$1);
var tool_name_53522 = (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(tc_53521,"name");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})();
var args_preview_53523 = (cljs.core.truth_(knoxx.backend.eta_mu_session_ingester.obj_get(tc_53521,"arguments"))?knoxx.backend.eta_mu_session_ingester.truncate_text(JSON.stringify(knoxx.backend.eta_mu_session_ingester.obj_get(tc_53521,"arguments")),(2000)):null);
events.push(knoxx.backend.eta_mu_session_ingester.make_event(session_id,"tool_call",knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"timestamp"),knoxx.backend.eta_mu_session_ingester.truncate_text((""+"Tool: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name_53522)+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = args_preview_53523;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),null),({"role": "system", "author": "eta-mu", "model": model}),({"id": (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(tc_53521,"id");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id");
}
})(), "eta_mu_message_id": knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"id"), "tool_name": tool_name_53522, "tool_call_id": knoxx.backend.eta_mu_session_ingester.obj_get(tc_53521,"id"), "tool_arguments_preview": args_preview_53523})));


var G__53525 = cljs.core.next(seq__53337_53512__$1);
var G__53526 = null;
var G__53527 = (0);
var G__53528 = (0);
seq__53337_53491 = G__53525;
chunk__53338_53492 = G__53526;
count__53339_53493 = G__53527;
i__53340_53494 = G__53528;
continue;
}
} else {
}
}
break;
}

return events;
});
knoxx.backend.eta_mu_session_ingester.map_eta_mu_event_to_events = (function knoxx$backend$eta_mu_session_ingester$map_eta_mu_event_to_events(eta_mu_event,session_meta){
var event_type = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"type");
var session_id = session_meta.sessionId;
var cwd = knoxx.backend.eta_mu_session_ingester.obj_get(session_meta,"cwd");
if((!(cljs.core.contains_QMARK_(knoxx.backend.eta_mu_session_ingester.SUPPORTED_EVENT_TYPES,event_type)))){
return [];
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"session")){
return knoxx.backend.eta_mu_session_ingester.map_session_event(eta_mu_event,session_id,cwd);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"model_change")){
return knoxx.backend.eta_mu_session_ingester.map_model_change_event(eta_mu_event,session_id);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"thinking_level_change")){
return [];
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"compaction")){
return knoxx.backend.eta_mu_session_ingester.map_compaction_event(eta_mu_event,session_id,cwd);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"custom_message")){
return knoxx.backend.eta_mu_session_ingester.map_custom_message_event(eta_mu_event,session_id,cwd);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"message")){
var msg = (function (){var or__5142__auto__ = knoxx.backend.eta_mu_session_ingester.obj_get(eta_mu_event,"message");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var role = knoxx.backend.eta_mu_session_ingester.obj_get(msg,"role");
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"user")){
return knoxx.backend.eta_mu_session_ingester.map_user_message(eta_mu_event,session_id,msg,cwd);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"assistant")){
return knoxx.backend.eta_mu_session_ingester.map_assistant_message(eta_mu_event,session_id,msg,cwd);
} else {
return [];

}
}
} else {
return [];

}
}
}
}
}
}
}
});
knoxx.backend.eta_mu_session_ingester.parse_session_file = (function knoxx$backend$eta_mu_session_ingester$parse_session_file(file_path){
return shadow.esm.esm_import$node_fs$promises.readFile(file_path,"utf-8").then((function (raw){
var lines = raw.split("\n");
var events = [];
var session_meta = ({"sessionId": "unknown", "cwd": "/unknown"});
var seq__53360_53535 = cljs.core.seq(Array.from(lines));
var chunk__53361_53536 = null;
var count__53362_53537 = (0);
var i__53363_53538 = (0);
while(true){
if((i__53363_53538 < count__53362_53537)){
var line_53539 = chunk__53361_53536.cljs$core$IIndexed$_nth$arity$2(null,i__53363_53538);
var trimmed_53553 = line_53539.trim();
if((!(clojure.string.blank_QMARK_(trimmed_53553)))){
try{var parsed_53554 = JSON.parse(trimmed_53553);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(parsed_53554.type,"session")){
(session_meta["sessionId"] = (function (){var or__5142__auto__ = parsed_53554.id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})());

(session_meta["cwd"] = (function (){var or__5142__auto__ = parsed_53554.cwd;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/unknown";
}
})());
} else {
}

events.push(parsed_53554);
}catch (e53371){var __53555 = e53371;
}} else {
}


var G__53556 = seq__53360_53535;
var G__53557 = chunk__53361_53536;
var G__53558 = count__53362_53537;
var G__53559 = (i__53363_53538 + (1));
seq__53360_53535 = G__53556;
chunk__53361_53536 = G__53557;
count__53362_53537 = G__53558;
i__53363_53538 = G__53559;
continue;
} else {
var temp__5825__auto___53560 = cljs.core.seq(seq__53360_53535);
if(temp__5825__auto___53560){
var seq__53360_53561__$1 = temp__5825__auto___53560;
if(cljs.core.chunked_seq_QMARK_(seq__53360_53561__$1)){
var c__5673__auto___53562 = cljs.core.chunk_first(seq__53360_53561__$1);
var G__53563 = cljs.core.chunk_rest(seq__53360_53561__$1);
var G__53564 = c__5673__auto___53562;
var G__53565 = cljs.core.count(c__5673__auto___53562);
var G__53566 = (0);
seq__53360_53535 = G__53563;
chunk__53361_53536 = G__53564;
count__53362_53537 = G__53565;
i__53363_53538 = G__53566;
continue;
} else {
var line_53567 = cljs.core.first(seq__53360_53561__$1);
var trimmed_53568 = line_53567.trim();
if((!(clojure.string.blank_QMARK_(trimmed_53568)))){
try{var parsed_53570 = JSON.parse(trimmed_53568);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(parsed_53570.type,"session")){
(session_meta["sessionId"] = (function (){var or__5142__auto__ = parsed_53570.id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})());

(session_meta["cwd"] = (function (){var or__5142__auto__ = parsed_53570.cwd;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/unknown";
}
})());
} else {
}

events.push(parsed_53570);
}catch (e53373){var __53577 = e53373;
}} else {
}


var G__53578 = cljs.core.next(seq__53360_53561__$1);
var G__53579 = null;
var G__53580 = (0);
var G__53581 = (0);
seq__53360_53535 = G__53578;
chunk__53361_53536 = G__53579;
count__53362_53537 = G__53580;
i__53363_53538 = G__53581;
continue;
}
} else {
}
}
break;
}

return ({"events": events, "sessionMeta": session_meta});
}));
});
knoxx.backend.eta_mu_session_ingester.ingest_session_file = (function knoxx$backend$eta_mu_session_ingester$ingest_session_file(file_path,session_file_meta,openplanner_request_fn){
return knoxx.backend.eta_mu_session_ingester.parse_session_file(file_path).then((function (p__53382){
var map__53383 = p__53382;
var map__53383__$1 = cljs.core.__destructure_map(map__53383);
var events = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53383__$1,new cljs.core.Keyword(null,"events","events",1792552201));
var session_meta = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53383__$1,new cljs.core.Keyword(null,"session-meta","session-meta",2135624586));
var all_op_events = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,eta_mu_event){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(Array.from(acc),Array.from(knoxx.backend.eta_mu_session_ingester.map_eta_mu_event_to_events(eta_mu_event,session_meta)));
}),[],Array.from(events));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(all_op_events.length,(0))){
return ({"sessionId": session_meta.sessionId, "eventsIngested": (0), "batches": (0)});
} else {
var promises = [];
var events_ingested = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((0));
var batches = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((0));
var i_53595 = (0);
while(true){
if((i_53595 < all_op_events.length)){
var batch_53596 = all_op_events.slice(i_53595,(i_53595 + knoxx.backend.eta_mu_session_ingester.MAX_EVENTS_PER_BATCH));
promises.push((function (){var G__53393 = "POST";
var G__53394 = "/v1/events";
var G__53395 = ({"events": batch_53596});
return (openplanner_request_fn.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_fn.cljs$core$IFn$_invoke$arity$3(G__53393,G__53394,G__53395) : openplanner_request_fn.call(null,G__53393,G__53394,G__53395));
})().then(((function (i_53595,batch_53596,promises,events_ingested,batches,all_op_events,map__53383,map__53383__$1,events,session_meta){
return (function (_){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(events_ingested,cljs.core._PLUS_,batch_53596.length);

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(batches,cljs.core.inc);
});})(i_53595,batch_53596,promises,events_ingested,batches,all_op_events,map__53383,map__53383__$1,events,session_meta))
).catch(((function (i_53595,batch_53596,promises,events_ingested,batches,all_op_events,map__53383,map__53383__$1,events,session_meta){
return (function (err){
return console.error((""+"[eta-mu-ingester] Batch failed for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_meta.sessionId)+":"),err.message);
});})(i_53595,batch_53596,promises,events_ingested,batches,all_op_events,map__53383,map__53383__$1,events,session_meta))
));

var G__53598 = (i_53595 + knoxx.backend.eta_mu_session_ingester.MAX_EVENTS_PER_BATCH);
i_53595 = G__53598;
continue;
} else {
}
break;
}

return Promise.all(promises).then((function (_){
return ({"sessionId": session_meta.sessionId, "eventsIngested": cljs.core.deref(events_ingested), "batches": cljs.core.deref(batches)});
}));
}
}));
});
knoxx.backend.eta_mu_session_ingester.run_eta_mu_session_ingest = (function knoxx$backend$eta_mu_session_ingester$run_eta_mu_session_ingest(p__53399){
var map__53400 = p__53399;
var map__53400__$1 = cljs.core.__destructure_map(map__53400);
var openplanner_request_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53400__$1,new cljs.core.Keyword(null,"openplanner-request-fn","openplanner-request-fn",-63455595));
var force = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__53400__$1,new cljs.core.Keyword(null,"force","force",781957286),false);
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__53400__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363),(50));
var session_dirs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53400__$1,new cljs.core.Keyword(null,"session-dirs","session-dirs",-866597807));
if(cljs.core.not(openplanner_request_fn)){
throw (new Error("openplannerRequestFn is required"));
} else {
}

var limit_53599__$1 = (function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (50);
}
})();
(cljs.core.truth_(force)?Promise.resolve(({"sessions": Object.create(null)})):knoxx.backend.eta_mu_session_ingester.load_ingest_state()).then((function (state){
return knoxx.backend.eta_mu_session_ingester.discover_session_files((0)).then((function (all_files){
var files = (cljs.core.truth_(session_dirs)?all_files.filter((function (f){
return cljs.core.some((function (d){
return f.dir.includes(d);
}),session_dirs);
})):all_files);
var new_files = (cljs.core.truth_(force)?files:files.filter((function (f){
var existing = (state.sessions[f.sessionId]);
return ((cljs.core.not(existing)) || (((function (){var or__5142__auto____$1 = existing.mtime;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (0);
}
})() < f.mtime)));
})));
var to_ingest = new_files.slice((0),limit_53599__$1);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(to_ingest.length,(0))){
return ({"ok": true, "scanned": files.length, "newSessions": (0), "ingested": (0), "totalEvents": (0), "skipped": (files.length - to_ingest.length)});
} else {
var results_atom = cljs.core.atom.cljs$core$IFn$_invoke$arity$1([]);
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (promise_chain,file){
return promise_chain.then((function (_){
return knoxx.backend.eta_mu_session_ingester.ingest_session_file(file.path,file,openplanner_request_fn).then((function (result){
(state.sessions[file.sessionId] = ({"mtime": file.mtime, "eventCount": result.eventsIngested, "ingestedAt": (new Date()).toISOString(), "dir": file.dir, "size": file.size}));

return cljs.core.deref(results_atom).push(result);
})).catch((function (err){
console.error((""+"[eta-mu-ingester] Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file.sessionId)+":"),err.message);

return cljs.core.deref(results_atom).push(({"sessionId": file.sessionId, "error": err.message, "eventsIngested": (0), "batches": (0)}));
}));
}));
}),Promise.resolve(null),Array.from(to_ingest)).then((function (_){
return knoxx.backend.eta_mu_session_ingester.save_ingest_state(state).then((function (___$1){
var results = cljs.core.deref(results_atom);
var total_events = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (sum,r){
return (sum + (function (){var or__5142__auto__ = r.eventsIngested;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})());
}),(0),Array.from(results));
var errors = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (cnt,r){
if(cljs.core.truth_(r.error)){
return (cnt + (1));
} else {
return cnt;
}
}),(0),Array.from(results));
return ({"ok": true, "scanned": files.length, "newSessions": to_ingest.length, "ingested": (to_ingest.length - errors), "totalEvents": total_events, "errors": errors, "details": results});
}));
}));
}
}));
}));

return (function (err){
return ({"ok": false, "error": err.message});
}).catch();
});
knoxx.backend.eta_mu_session_ingester.get_eta_mu_ingest_status = (function knoxx$backend$eta_mu_session_ingester$get_eta_mu_ingest_status(){
return Promise.all([knoxx.backend.eta_mu_session_ingester.load_ingest_state(),knoxx.backend.eta_mu_session_ingester.discover_session_files((0))]).then((function (p__53413){
var vec__53415 = p__53413;
var state = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53415,(0),null);
var all_files = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53415,(1),null);
var ingested_ids = (new Set(Object.keys(state.sessions)));
var pending = all_files.filter((function (f){
return cljs.core.not(ingested_ids.has(f.sessionId));
}));
var stale = all_files.filter((function (f){
var existing = (state.sessions[f.sessionId]);
var and__5140__auto__ = existing;
if(cljs.core.truth_(and__5140__auto__)){
return ((function (){var or__5142__auto__ = existing.mtime;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})() < f.mtime);
} else {
return and__5140__auto__;
}
}));
var total_ingested = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (sum,s){
return (sum + (function (){var or__5142__auto__ = (s["eventCount"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})());
}),(0),Object.values(state.sessions));
var last_ingested = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (max_val,s){
var at = (s["ingestedAt"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = at;
if(cljs.core.truth_(and__5140__auto__)){
return (at.length() > max_val.length());
} else {
return and__5140__auto__;
}
})())){
return at;
} else {
return max_val;
}
}),"",Object.values(state.sessions));
var recent = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__53426){
var vec__53427 = p__53426;
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53427,(0),null);
var s = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53427,(1),null);
return ({"sessionId": id, "eventCount": (s["eventCount"]), "ingestedAt": (s["ingestedAt"]), "dir": (s["dir"])});
}),cljs.core.take.cljs$core$IFn$_invoke$arity$2((10),cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3((function (p__53430){
var vec__53431 = p__53430;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53431,(0),null);
var s = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__53431,(1),null);
return (s["ingestedAt"]);
}),(function (p1__53411_SHARP_,p2__53412_SHARP_){
return (p1__53411_SHARP_ > p2__53412_SHARP_);
}),Object.entries(state.sessions))));
return ({"recentIngested": cljs.core.clj__GT_js(recent), "staleSessions": stale.length, "totalIngestedEvents": total_ingested, "totalSessionFiles": all_files.length, "etaMuSessionsRoot": knoxx.backend.eta_mu_session_ingester.ETA_MU_SESSIONS_ROOT, "lastIngestedAt": last_ingested, "ingestedSessions": ingested_ids.size, "ok": true, "pendingSessions": pending.length});
}));
});
knoxx.backend.eta_mu_session_ingester.list_eta_mu_sessions = (function knoxx$backend$eta_mu_session_ingester$list_eta_mu_sessions(p__53435){
var map__53440 = p__53435;
var map__53440__$1 = cljs.core.__destructure_map(map__53440);
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__53440__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363),(50));
var offset = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__53440__$1,new cljs.core.Keyword(null,"offset","offset",296498311),(0));
var workspace = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53440__$1,new cljs.core.Keyword(null,"workspace","workspace",-1096735709));
return knoxx.backend.eta_mu_session_ingester.discover_session_files((0)).then((function (all_files){
var filtered = (cljs.core.truth_(workspace)?all_files.filter((function (f){
return f.dir.includes(workspace);
})):all_files);
var sorted = filtered.sort((function (a,b){
return (b.mtime - a.mtime);
}));
var total = sorted.length;
var page = sorted.slice(offset,(offset + limit));
return Promise.all(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (f){
return shadow.esm.esm_import$node_fs$promises.readFile(f.path,"utf-8").then((function (raw){
var lines = raw.split("\n");
var first_line = cljs.core.some((function (l){
if((!(clojure.string.blank_QMARK_(l.trim())))){
return l;
} else {
return null;
}
}),Array.from(lines));
if(cljs.core.not(first_line)){
return ({"sessionId": f.sessionId, "workspace": f.dir, "lastModified": (new Date(f.mtime)).toISOString(), "fileSize": f.size, "dir": f.dir});
} else {
try{var header = JSON.parse(first_line.trim());
var msg_count = (function (){var or__5142__auto__ = raw.match((new RegExp("\"type\":\"message\"","g"))).length();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var tool_count = (function (){var or__5142__auto__ = raw.match((new RegExp("\"type\":\"toolCall\"","g"))).length();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
return ({"sessionId": (function (){var or__5142__auto__ = header.id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return f.sessionId;
}
})(), "workspace": (function (){var or__5142__auto__ = header.cwd;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return f.dir;
}
})(), "startTime": header.timestamp, "lastModified": (new Date(f.mtime)).toISOString(), "messageCount": msg_count, "toolCallCount": tool_count, "fileSize": f.size, "dir": f.dir});
}catch (e53449){var _ = e53449;
return ({"sessionId": f.sessionId, "workspace": f.dir, "lastModified": (new Date(f.mtime)).toISOString(), "fileSize": f.size, "dir": f.dir});
}}
})).catch((function (_){
return ({"sessionId": f.sessionId, "workspace": f.dir, "lastModified": (new Date(f.mtime)).toISOString(), "fileSize": f.size, "dir": f.dir});
}));
}),Array.from(page))).then((function (sessions){
var valid = sessions.filter(cljs.core.some_QMARK_);
return ({"ok": true, "sessions": valid, "total": total, "offset": offset, "limit": limit, "has_more": ((offset + valid.length) < total)});
}));
}));
});

//# sourceMappingURL=knoxx.backend.eta_mu_session_ingester.js.map
