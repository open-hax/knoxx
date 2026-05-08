import "./cljs_env.js";
import "./cljs.core.js";
import "./shadow.cljs.modern.js";
import "./clojure.string.js";
import "./knoxx.backend.contracts.actor_scope.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.eta_mu_session_ingester.js";
goog.provide('knoxx.backend.routes.tools.proxy');
knoxx.backend.routes.tools.proxy.enrich_session_summary_BANG_ = (function knoxx$backend$routes$tools$proxy$enrich_session_summary_BANG_(config,summary){
var session_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(summary);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(summary,new cljs.core.Keyword(null,"session","session",1008279103));
}
})();
if(cljs.core.not(session_id)){
return Promise.resolve(summary);
} else {
return knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_(config,session_id).then((function (rows){
var contract_id = knoxx.backend.core_memory.session_contract_id_from_rows(rows);
var actor_id = knoxx.backend.core_memory.session_actor_id_from_rows(rows);
var contract_actors = knoxx.backend.core_memory.session_contract_actors_from_rows(rows);
var wire_actors = ((cljs.core.seq(contract_actors))?knoxx.backend.contracts.actor_scope.actor_claims__GT_wire(contract_actors):null);
var G__36042 = summary;
var G__36042__$1 = (cljs.core.truth_(contract_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__36042,new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),contract_id):G__36042);
var G__36042__$2 = (cljs.core.truth_(actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__36042__$1,new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),actor_id):G__36042__$1);
if(cljs.core.seq(wire_actors)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__36042__$2,new cljs.core.Keyword(null,"contract_actors","contract_actors",-1493360705),wire_actors);
} else {
return G__36042__$2;
}
})).catch((function (_){
return summary;
}));
}
});
knoxx.backend.routes.tools.proxy.now_iso = (function knoxx$backend$routes$tools$proxy$now_iso(){
return (new Date()).toISOString();
});
knoxx.backend.routes.tools.proxy.json_content_type = (function knoxx$backend$routes$tools$proxy$json_content_type(resp){
var or__5142__auto__ = (function (){var G__36055 = resp;
var G__36055__$1 = (((G__36055 == null))?null:(G__36055["headers"]));
if((G__36055__$1 == null)){
return null;
} else {
return G__36055__$1.get("content-type");
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "application/json";
}
});
knoxx.backend.routes.tools.proxy.safe_json = (function knoxx$backend$routes$tools$proxy$safe_json(resp){
return resp.json().catch((function (_){
return null;
}));
});
knoxx.backend.routes.tools.proxy.safe_text = (function knoxx$backend$routes$tools$proxy$safe_text(resp){
return resp.text().catch((function (_){
return "";
}));
});
knoxx.backend.routes.tools.proxy.timeout_signal = (function knoxx$backend$routes$tools$proxy$timeout_signal(ms){
return AbortSignal.timeout(ms);
});
knoxx.backend.routes.tools.proxy.reply_sent_QMARK_ = (function knoxx$backend$routes$tools$proxy$reply_sent_QMARK_(reply){
var raw = (reply["raw"]);
return cljs.core.boolean$((function (){var or__5142__auto__ = (reply["sent"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = raw;
if(cljs.core.truth_(and__5140__auto__)){
return (raw["writableEnded"]);
} else {
return and__5140__auto__;
}
}
})());
});
knoxx.backend.routes.tools.proxy.request_query_string = (function knoxx$backend$routes$tools$proxy$request_query_string(req){
var query = (function (){var or__5142__auto__ = (req["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var params = (new URLSearchParams());
var seq__36087_36206 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(Object.keys(query)));
var chunk__36088_36207 = null;
var count__36089_36208 = (0);
var i__36090_36209 = (0);
while(true){
if((i__36090_36209 < count__36089_36208)){
var key_36210 = chunk__36088_36207.cljs$core$IIndexed$_nth$arity$2(null,i__36090_36209);
var value_36211 = (query[key_36210]);
if((value_36211 == null)){
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(value_36211,undefined)){
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(value_36211))){
var seq__36146_36212 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value_36211));
var chunk__36147_36213 = null;
var count__36148_36214 = (0);
var i__36149_36215 = (0);
while(true){
if((i__36149_36215 < count__36148_36214)){
var item_36216 = chunk__36147_36213.cljs$core$IIndexed$_nth$arity$2(null,i__36149_36215);
params.append(key_36210,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_36216)));


var G__36217 = seq__36146_36212;
var G__36218 = chunk__36147_36213;
var G__36219 = count__36148_36214;
var G__36220 = (i__36149_36215 + (1));
seq__36146_36212 = G__36217;
chunk__36147_36213 = G__36218;
count__36148_36214 = G__36219;
i__36149_36215 = G__36220;
continue;
} else {
var temp__5825__auto___36221 = cljs.core.seq(seq__36146_36212);
if(temp__5825__auto___36221){
var seq__36146_36222__$1 = temp__5825__auto___36221;
if(cljs.core.chunked_seq_QMARK_(seq__36146_36222__$1)){
var c__5673__auto___36223 = cljs.core.chunk_first(seq__36146_36222__$1);
var G__36224 = cljs.core.chunk_rest(seq__36146_36222__$1);
var G__36225 = c__5673__auto___36223;
var G__36226 = cljs.core.count(c__5673__auto___36223);
var G__36227 = (0);
seq__36146_36212 = G__36224;
chunk__36147_36213 = G__36225;
count__36148_36214 = G__36226;
i__36149_36215 = G__36227;
continue;
} else {
var item_36228 = cljs.core.first(seq__36146_36222__$1);
params.append(key_36210,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_36228)));


var G__36229 = cljs.core.next(seq__36146_36222__$1);
var G__36230 = null;
var G__36231 = (0);
var G__36232 = (0);
seq__36146_36212 = G__36229;
chunk__36147_36213 = G__36230;
count__36148_36214 = G__36231;
i__36149_36215 = G__36232;
continue;
}
} else {
}
}
break;
}
} else {
params.append(key_36210,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_36211)));

}
}
}


var G__36233 = seq__36087_36206;
var G__36234 = chunk__36088_36207;
var G__36235 = count__36089_36208;
var G__36236 = (i__36090_36209 + (1));
seq__36087_36206 = G__36233;
chunk__36088_36207 = G__36234;
count__36089_36208 = G__36235;
i__36090_36209 = G__36236;
continue;
} else {
var temp__5825__auto___36237 = cljs.core.seq(seq__36087_36206);
if(temp__5825__auto___36237){
var seq__36087_36238__$1 = temp__5825__auto___36237;
if(cljs.core.chunked_seq_QMARK_(seq__36087_36238__$1)){
var c__5673__auto___36239 = cljs.core.chunk_first(seq__36087_36238__$1);
var G__36240 = cljs.core.chunk_rest(seq__36087_36238__$1);
var G__36241 = c__5673__auto___36239;
var G__36242 = cljs.core.count(c__5673__auto___36239);
var G__36243 = (0);
seq__36087_36206 = G__36240;
chunk__36088_36207 = G__36241;
count__36089_36208 = G__36242;
i__36090_36209 = G__36243;
continue;
} else {
var key_36244 = cljs.core.first(seq__36087_36238__$1);
var value_36245 = (query[key_36244]);
if((value_36245 == null)){
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(value_36245,undefined)){
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(value_36245))){
var seq__36158_36250 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value_36245));
var chunk__36159_36251 = null;
var count__36160_36252 = (0);
var i__36161_36253 = (0);
while(true){
if((i__36161_36253 < count__36160_36252)){
var item_36255 = chunk__36159_36251.cljs$core$IIndexed$_nth$arity$2(null,i__36161_36253);
params.append(key_36244,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_36255)));


var G__36256 = seq__36158_36250;
var G__36257 = chunk__36159_36251;
var G__36258 = count__36160_36252;
var G__36259 = (i__36161_36253 + (1));
seq__36158_36250 = G__36256;
chunk__36159_36251 = G__36257;
count__36160_36252 = G__36258;
i__36161_36253 = G__36259;
continue;
} else {
var temp__5825__auto___36260__$1 = cljs.core.seq(seq__36158_36250);
if(temp__5825__auto___36260__$1){
var seq__36158_36261__$1 = temp__5825__auto___36260__$1;
if(cljs.core.chunked_seq_QMARK_(seq__36158_36261__$1)){
var c__5673__auto___36262 = cljs.core.chunk_first(seq__36158_36261__$1);
var G__36263 = cljs.core.chunk_rest(seq__36158_36261__$1);
var G__36264 = c__5673__auto___36262;
var G__36265 = cljs.core.count(c__5673__auto___36262);
var G__36266 = (0);
seq__36158_36250 = G__36263;
chunk__36159_36251 = G__36264;
count__36160_36252 = G__36265;
i__36161_36253 = G__36266;
continue;
} else {
var item_36267 = cljs.core.first(seq__36158_36261__$1);
params.append(key_36244,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_36267)));


var G__36269 = cljs.core.next(seq__36158_36261__$1);
var G__36270 = null;
var G__36271 = (0);
var G__36272 = (0);
seq__36158_36250 = G__36269;
chunk__36159_36251 = G__36270;
count__36160_36252 = G__36271;
i__36161_36253 = G__36272;
continue;
}
} else {
}
}
break;
}
} else {
params.append(key_36244,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_36245)));

}
}
}


var G__36273 = cljs.core.next(seq__36087_36238__$1);
var G__36274 = null;
var G__36275 = (0);
var G__36276 = (0);
seq__36087_36206 = G__36273;
chunk__36088_36207 = G__36274;
count__36089_36208 = G__36275;
i__36090_36209 = G__36276;
continue;
}
} else {
}
}
break;
}

var encoded = params.toString();
if(clojure.string.blank_QMARK_(encoded)){
return "";
} else {
return (""+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encoded));
}
});
knoxx.backend.routes.tools.proxy.reply_send_with_content_type_BANG_ = (function knoxx$backend$routes$tools$proxy$reply_send_with_content_type_BANG_(reply,status,content_type,body){
if(knoxx.backend.routes.tools.proxy.reply_sent_QMARK_(reply)){
return null;
} else {
var reply_STAR_ = reply.code(status);
reply_STAR_.header("content-type",content_type);

return reply_STAR_.send(body);
}
});
knoxx.backend.routes.tools.proxy.send_proxy_error_BANG_ = (function knoxx$backend$routes$tools$proxy$send_proxy_error_BANG_(reply,prefix,err){
if(knoxx.backend.routes.tools.proxy.reply_sent_QMARK_(reply)){
return null;
} else {
return reply.code((502)).send(({"ok": false, "error": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (err["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()))}));
}
});
knoxx.backend.routes.tools.proxy.request_body = (function knoxx$backend$routes$tools$proxy$request_body(req){
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["HEAD",null,"GET",null], null), null),(req["method"]))){
return undefined;
} else {
return JSON.stringify((req["body"]));
}
});
knoxx.backend.routes.tools.proxy.proxy_fetch_BANG_ = (function knoxx$backend$routes$tools$proxy$proxy_fetch_BANG_(target_url,req,reply,headers,error_prefix){
var fetch_promise = fetch(target_url,({"method": (req["method"]), "headers": headers, "body": knoxx.backend.routes.tools.proxy.request_body(req), "signal": knoxx.backend.routes.tools.proxy.timeout_signal((60000))}));
return fetch_promise.then((function (resp){
var content_type = knoxx.backend.routes.tools.proxy.json_content_type(resp);
var body_promise = ((clojure.string.includes_QMARK_(content_type,"application/json"))?knoxx.backend.routes.tools.proxy.safe_json(resp):knoxx.backend.routes.tools.proxy.safe_text(resp));
return body_promise.then((function (body){
return knoxx.backend.routes.tools.proxy.reply_send_with_content_type_BANG_(reply,resp.status,content_type,body);
}),(function (err){
return knoxx.backend.routes.tools.proxy.send_proxy_error_BANG_(reply,error_prefix,err);
}));
}),(function (err){
return knoxx.backend.routes.tools.proxy.send_proxy_error_BANG_(reply,error_prefix,err);
}));
});
/**
 * Register all proxy endpoints on the fastify app.
 */
knoxx.backend.routes.tools.proxy.register_proxy_routes_BANG_ = (function knoxx$backend$routes$tools$proxy$register_proxy_routes_BANG_(app,config){
app.get("/api/admin/eta-mu-sessions/status",(function (_req,reply){
var kms_base = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost:3003";
}
})();
var kms_headers = ({"x-knoxx-user-email": "system-admin@open-hax.local", "x-knoxx-org-slug": "open-hax"});
var legacy_p = knoxx.backend.eta_mu_session_ingester.get_eta_mu_ingest_status().catch((function (err){
return ({"ok": false, "error": err.message});
}));
var kms_p = fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kms_base)+"/api/ingestion/sources?tenant_id=knoxx-session"),({"headers": kms_headers, "signal": knoxx.backend.routes.tools.proxy.timeout_signal((15000))})).then((function (r){
if(cljs.core.truth_(r.ok)){
return r.json();
} else {
return Promise.resolve([]);
}
})).catch((function (_){
return [];
})).then((function (sources){
var sources__$1 = (cljs.core.truth_(cljs.core.array_QMARK_(sources))?sources:[]);
var eta_mu_source = sources__$1.find((function (s){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((s["driver_type"]),"eta-mu-sessions");
}));
if(cljs.core.not(eta_mu_source)){
return ({"ok": false, "error": "eta-mu-sessions source not found", "sources": sources__$1});
} else {
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kms_base)+"/api/ingestion/jobs?tenant_id=knoxx-session&source_id="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((eta_mu_source["source_id"]))),({"headers": kms_headers, "signal": knoxx.backend.routes.tools.proxy.timeout_signal((15000))})).then((function (r){
if(cljs.core.truth_(r.ok)){
return r.json();
} else {
return Promise.resolve([]);
}
})).catch((function (_){
return [];
})).then((function (jobs){
return ({"ok": true, "source": eta_mu_source, "jobs": jobs});
}));
}
}));
return Promise.all([legacy_p,kms_p]).then((function (parts){
var legacy = (parts[(0)]);
var kms = (parts[(1)]);
return reply.send(({"ok": true, "legacy": legacy, "kms_ingestion": kms, "time": knoxx.backend.routes.tools.proxy.now_iso()}));
})).catch((function (err){
reply.code((500));

return reply.send(({"ok": false, "error": err.message}));
}));
}));

app.get("/api/admin/eta-mu-sessions",(function (req,reply){
try{var q = (function (){var or__5142__auto__ = (req["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var limit = cljs.core.min.cljs$core$IFn$_invoke$arity$2(parseInt((function (){var or__5142__auto__ = (q["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "50";
}
})(),(10)),(200));
var offset = parseInt((function (){var or__5142__auto__ = (q["offset"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "0";
}
})(),(10));
var workspace = (q["workspace"]);
return knoxx.backend.eta_mu_session_ingester.list_eta_mu_sessions(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"offset","offset",296498311),offset,new cljs.core.Keyword(null,"workspace","workspace",-1096735709),workspace], null)).then((function (result){
return reply.send(result);
})).catch((function (err){
reply.code((500));

return reply.send(({"ok": false, "error": err.message}));
}));
}catch (e36193){var err = e36193;
reply.code((500));

return reply.send(({"ok": false, "error": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))}));
}}));

app.post("/api/admin/eta-mu-sessions/ingest",(function (req,reply){
var kms_base = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost:3003";
}
})();
var kms_headers = ({"content-type": "application/json", "x-knoxx-user-email": "system-admin@open-hax.local", "x-knoxx-org-slug": "open-hax"});
var body = (function (){var or__5142__auto__ = (req["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var force_QMARK_ = cljs.core.boolean$((body["force"]));
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kms_base)+"/api/ingestion/sources?tenant_id=knoxx-session"),({"headers": kms_headers, "signal": knoxx.backend.routes.tools.proxy.timeout_signal((20000))})).then((function (r){
if(cljs.core.truth_(r.ok)){
return r.json();
} else {
return Promise.resolve([]);
}
})).catch((function (_){
return [];
})).then((function (sources){
var sources__$1 = (cljs.core.truth_(cljs.core.array_QMARK_(sources))?sources:[]);
var eta_mu_source = sources__$1.find((function (s){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((s["driver_type"]),"eta-mu-sessions");
}));
if(cljs.core.not(eta_mu_source)){
reply.code((404));

return reply.send(({"ok": false, "error": "eta-mu-sessions source not found in ingestion service"}));
} else {
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kms_base)+"/api/ingestion/jobs"),({"method": "POST", "headers": kms_headers, "body": JSON.stringify(({"source_id": (eta_mu_source["source_id"]), "full_scan": force_QMARK_})), "signal": knoxx.backend.routes.tools.proxy.timeout_signal((20000))})).then((function (r){
if(cljs.core.truth_(r.ok)){
return r.json();
} else {
return knoxx.backend.routes.tools.proxy.safe_json(r);
}
})).then((function (job){
return reply.send(({"ok": true, "job": job}));
})).catch((function (err){
reply.code((500));

return reply.send(({"ok": false, "error": err.message}));
}));
}
}));
}));

app.all("/api/ingestion/*",(function (req,reply){
var kms_base = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ingestion-base-url","ingestion-base-url",-1760674811).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost:3003";
}
})();
var sub_path = ((req["params"])["*"]);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(kms_base)+"/api/ingestion/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sub_path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.tools.proxy.request_query_string(req)));
var headers = Object.assign(({}),(req["headers"]));
Reflect.deleteProperty(headers,"host");

Reflect.deleteProperty(headers,"connection");

Reflect.deleteProperty(headers,"content-length");

return knoxx.backend.routes.tools.proxy.proxy_fetch_BANG_(target_url,req,reply,headers,"Ingestion proxy error");
}));

app.get("/api/openplanner/v1/sessions",(function (req,reply){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.tools.proxy.request_query_string(req)))).then((function (body){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__36184_SHARP_){
return knoxx.backend.routes.tools.proxy.enrich_session_summary_BANG_(config,p1__36184_SHARP_);
}),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))).then((function (enriched){
return reply.send(cljs.core.clj__GT_js(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(body,new cljs.core.Keyword(null,"rows","rows",850049680),cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(enriched)))));
}));
}));
}));

return app.all("/api/openplanner/*",(function (req,reply){
var base = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost:7777";
}
})();
var key = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "change-me";
}
})();
var sub_path = ((req["params"])["*"]);
var target_url = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sub_path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.tools.proxy.request_query_string(req)));
var fwd_headers = ({"content-type": "application/json", "authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(key)), "x-knoxx-user-email": (function (){var or__5142__auto__ = ((req["headers"])["x-knoxx-user-email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "x-knoxx-org-slug": (function (){var or__5142__auto__ = ((req["headers"])["x-knoxx-org-slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()});
return knoxx.backend.routes.tools.proxy.proxy_fetch_BANG_(target_url,req,reply,fwd_headers,"OpenPlanner proxy error");
}));
});

//# sourceMappingURL=knoxx.backend.routes.tools.proxy.js.map
