import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.events.dispatch.js";
import "./knoxx.backend.events.runtime.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.macros.js";
import "./knoxx.backend.mcp_bridge.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.triggers.control_config.js";
import "./shadow.esm.esm_import$node_child_process.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
import "./shadow.esm.esm_import$node_util.js";
import "./shadow.esm.esm_import$nodemailer.js";
goog.provide('knoxx.backend.routes.tools');
knoxx.backend.routes.tools.exec_file_async = shadow.esm.esm_import$node_util.promisify(shadow.esm.esm_import$node_child_process.execFile);
/**
 * Send an email via Gmail SMTP using nodemailer.
 */
knoxx.backend.routes.tools.send_email_BANG_ = (function knoxx$backend$routes$tools$send_email_BANG_(_runtime,config,to,subject,text_body,cc,bcc){
var email = new cljs.core.Keyword(null,"gmail-app-email","gmail-app-email",-654288582).cljs$core$IFn$_invoke$arity$1(config);
var password = new cljs.core.Keyword(null,"gmail-app-password","gmail-app-password",-1448333374).cljs$core$IFn$_invoke$arity$1(config);
if(((clojure.string.blank_QMARK_(email)) || (clojure.string.blank_QMARK_(password)))){
return Promise.reject((new Error("Gmail credentials not configured")));
} else {
var transporter = shadow.esm.esm_import$nodemailer.default.createTransport(({"host": "smtp.gmail.com", "port": (587), "secure": false, "auth": ({"user": email, "pass": password})}));
return transporter.sendMail(({"from": email, "to": clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",to), "cc": ((cljs.core.seq(cc))?clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cc):null), "bcc": ((cljs.core.seq(bcc))?clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",bcc):null), "subject": subject, "text": text_body}));
}
});
knoxx.backend.routes.tools.masked_discord_token = (function knoxx$backend$routes$tools$masked_discord_token(token){
if(((typeof token === 'string') && ((cljs.core.count(token) > (8))))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(token,(0),(4)))+"***"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$2(token,(cljs.core.count(token) - (4)))));
} else {
return "";
}
});
knoxx.backend.routes.tools.event_agents_control_response = (function knoxx$backend$routes$tools$event_agents_control_response(config){
var live_config = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})();
var control = knoxx.backend.triggers.control_config.event_agent_control_config(live_config);
var runtime = knoxx.backend.events.runtime.status_snapshot(live_config);
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"configured","configured",-884777889),false,new cljs.core.Keyword(null,"tokenPreview","tokenPreview",-2066034955),"",new cljs.core.Keyword(null,"availableRoles","availableRoles",-1960366406),knoxx.backend.triggers.control_config.event_agent_role_options(live_config),new cljs.core.Keyword(null,"availableSourceKinds","availableSourceKinds",1521211447),knoxx.backend.triggers.control_config.event_agent_source_kind_options(),new cljs.core.Keyword(null,"availableTriggerKinds","availableTriggerKinds",-192509548),knoxx.backend.triggers.control_config.event_agent_trigger_kind_options(),new cljs.core.Keyword(null,"control","control",1892578036),control,new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime], null);
});
knoxx.backend.routes.tools.restart_discord_gateway_BANG_ = (function knoxx$backend$routes$tools$restart_discord_gateway_BANG_(token){
if(knoxx.backend.discord_gateway.started_QMARK_()){
return knoxx.backend.discord_gateway.restart_BANG_(token).catch((function (_){
return null;
}));
} else {
return null;
}
});
knoxx.backend.routes.tools.register_tool_catalog_route_BANG_ = (function knoxx$backend$routes$tools$register_tool_catalog_route_BANG_(app,runtime,config,deps){
var map__54334 = deps;
var map__54334__$1 = cljs.core.__destructure_map(map__54334);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var tool_catalog = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"tool-catalog","tool-catalog",899421286));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54334__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54337 = app;
var G__54338 = "GET";
var G__54339 = "/api/tools/catalog";
var G__54340 = (function (){var obj54342 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [optional_session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var role = (function (){var or__5142__auto__ = (request["query"]["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (request["query"]["agent"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["agentId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (request["query"]["agentContractId"]);
}
}
})();
var actor_id = (function (){var or__5142__auto__ = (request["query"]["actor"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (request["query"]["actorId"]);
}
})();
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var G__54349 = reply;
var G__54350 = (200);
var G__54351 = (tool_catalog.cljs$core$IFn$_invoke$arity$5 ? tool_catalog.cljs$core$IFn$_invoke$arity$5(config,role,ctx,agent_contract_id,actor_id) : tool_catalog.call(null,config,role,ctx,agent_contract_id,actor_id));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54349,G__54350,G__54351) : json_response_BANG_.call(null,G__54349,G__54350,G__54351));
})});
return obj54342;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54337,G__54338,G__54339,G__54340) : route_BANG_.call(null,G__54337,G__54338,G__54339,G__54340));
});
knoxx.backend.routes.tools.register_email_send_route_BANG_ = (function knoxx$backend$routes$tools$register_email_send_route_BANG_(app,runtime,config,deps){
var map__54353 = deps;
var map__54353__$1 = cljs.core.__destructure_map(map__54353);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54353__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54355 = app;
var G__54356 = "POST";
var G__54357 = "/api/tools/email/send";
var G__54358 = (function (){var obj54361 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var body_55151 = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id_55152 = (function (){var or__5142__auto__ = (body_55151["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body_55151["agent_contract_id"]);
}
})();
var role_55153 = (function (){var G__54364 = ctx;
var G__54365 = (function (){var or__5142__auto__ = (body_55151["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54366 = "email.send";
var G__54367 = agent_contract_id_55152;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54364,G__54365,G__54366,G__54367) : ensure_role_can_use_BANG_.call(null,G__54364,G__54365,G__54366,G__54367));
})();
var to_55154 = (function (){var or__5142__auto__ = (body_55151["to"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var cc_55155 = (function (){var or__5142__auto__ = (body_55151["cc"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var bcc_55156 = (function (){var or__5142__auto__ = (body_55151["bcc"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var subject_55157 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body_55151["subject"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "(no subject)";
}
})()));
var markdown_55158 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body_55151["markdown"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(cljs.core.empty_QMARK_(to_55154)){
var G__54372_55160 = reply;
var G__54373_55161 = (400);
var G__54374_55162 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing required field: to array"], null);
(json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54372_55160,G__54373_55161,G__54374_55162) : json_response_BANG_.call(null,G__54372_55160,G__54373_55161,G__54374_55162));
} else {
knoxx.backend.routes.tools.send_email_BANG_(runtime,config,to_55154,subject_55157,markdown_55158,cc_55155,bcc_55156).then((function (result){
var G__54375 = reply;
var G__54376 = (200);
var G__54377 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role_55153,new cljs.core.Keyword(null,"message_id","message_id",663757010),(result["messageId"])], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54375,G__54376,G__54377) : json_response_BANG_.call(null,G__54375,G__54376,G__54377));
})).catch((function (err){
var G__54387 = reply;
var G__54388 = (502);
var G__54389 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to send email: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (err["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54387,G__54388,G__54389) : json_response_BANG_.call(null,G__54387,G__54388,G__54389));
}));
}

var G__54392 = new cljs.core.Keyword(null,"default","default",-1987822328);
var G__54393 = knoxx.backend.routes.tools.err;
var G__54394 = (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,knoxx.backend.routes.tools.err) : error_response_BANG_.call(null,reply,knoxx.backend.routes.tools.err));
return (knoxx.backend.routes.tools.catch$.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.tools.catch$.cljs$core$IFn$_invoke$arity$3(G__54392,G__54393,G__54394) : knoxx.backend.routes.tools.catch$.call(null,G__54392,G__54393,G__54394));
})});
return obj54361;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54355,G__54356,G__54357,G__54358) : route_BANG_.call(null,G__54355,G__54356,G__54357,G__54358));
});
knoxx.backend.routes.tools.register_websearch_route_BANG_ = (function knoxx$backend$routes$tools$register_websearch_route_BANG_(app,runtime,config,deps){
var map__54402 = deps;
var map__54402__$1 = cljs.core.__destructure_map(map__54402);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54402__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54409 = app;
var G__54410 = "POST";
var G__54411 = "/api/tools/websearch";
var G__54412 = (function (){var obj54418 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var role = (function (){var G__54420 = ctx;
var G__54421 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54422 = "websearch";
var G__54423 = agent_contract_id;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54420,G__54421,G__54422,G__54423) : ensure_role_can_use_BANG_.call(null,G__54420,G__54421,G__54422,G__54423));
})();
var query = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var num_results = (function (){var or__5142__auto__ = (body["numResults"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8);
}
})();
var search_context_size = (body["searchContextSize"]);
var allowed_domains = (function (){var or__5142__auto__ = (body["allowedDomains"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var model = (body["model"]);
if(clojure.string.blank_QMARK_(query)){
var G__54425 = reply;
var G__54426 = (400);
var G__54427 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"query is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54425,G__54426,G__54427) : json_response_BANG_.call(null,G__54425,G__54426,G__54427));
} else {
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/api/tools/websearch"),({"method": "POST", "headers": (function (){var G__54434 = new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config);
return (bearer_headers.cljs$core$IFn$_invoke$arity$1 ? bearer_headers.cljs$core$IFn$_invoke$arity$1(G__54434) : bearer_headers.call(null,G__54434));
})(), "body": JSON.stringify(({"query": query, "numResults": num_results, "searchContextSize": search_context_size, "allowedDomains": allowed_domains, "model": model}))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
var G__54435 = reply;
var G__54436 = (200);
var G__54437 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),new cljs.core.Keyword(null,"role","role",-736691072),role);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54435,G__54436,G__54437) : json_response_BANG_.call(null,G__54435,G__54436,G__54437));
} else {
var G__54438 = reply;
var G__54439 = (function (){var or__5142__auto__ = (resp["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (502);
}
})();
var G__54440 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], 0))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54438,G__54439,G__54440) : json_response_BANG_.call(null,G__54438,G__54439,G__54440));
}
})).catch((function (err){
var G__54442 = reply;
var G__54443 = (502);
var G__54444 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54442,G__54443,G__54444) : json_response_BANG_.call(null,G__54442,G__54443,G__54444));
}));
}
}catch (e54419){var err = e54419;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54418;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54409,G__54410,G__54411,G__54412) : route_BANG_.call(null,G__54409,G__54410,G__54411,G__54412));
});
knoxx.backend.routes.tools.register_read_route_BANG_ = (function knoxx$backend$routes$tools$register_read_route_BANG_(app,runtime,config,deps){
var map__54450 = deps;
var map__54450__$1 = cljs.core.__destructure_map(map__54450);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var resolve_workspace_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"resolve-workspace-path","resolve-workspace-path",-1439207488));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54450__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54452 = app;
var G__54453 = "POST";
var G__54454 = "/api/tools/read";
var G__54455 = (function (){var obj54458 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var role = (function (){var G__54468 = ctx;
var G__54469 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54470 = "read";
var G__54471 = agent_contract_id;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54468,G__54469,G__54470,G__54471) : ensure_role_can_use_BANG_.call(null,G__54468,G__54469,G__54470,G__54471));
})();
var path_str = (function (){var G__54472 = runtime;
var G__54473 = config;
var G__54474 = (function (){var or__5142__auto__ = (body["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return (resolve_workspace_path.cljs$core$IFn$_invoke$arity$3 ? resolve_workspace_path.cljs$core$IFn$_invoke$arity$3(G__54472,G__54473,G__54474) : resolve_workspace_path.call(null,G__54472,G__54473,G__54474));
})();
var offset = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = (body["offset"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})());
var limit = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = (body["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (400);
}
})());
return shadow.esm.esm_import$node_fs$promises.stat(path_str).then((function (stat){
if(cljs.core.truth_(stat.isDirectory())){
return shadow.esm.esm_import$node_fs$promises.readdir(path_str,({"withFileTypes": true})).then((function (entries){
var content_lines = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (e){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((e["name"]))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(e.isDirectory())?"/":null)));
}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(entries));
var vec__54475 = (function (){var G__54479 = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",content_lines);
return (clip_text.cljs$core$IFn$_invoke$arity$1 ? clip_text.cljs$core$IFn$_invoke$arity$1(G__54479) : clip_text.call(null,G__54479));
})();
var content = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54475,(0),null);
var truncated = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54475,(1),null);
var G__54481 = reply;
var G__54482 = (200);
var G__54483 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"path","path",-188191168),path_str,new cljs.core.Keyword(null,"content","content",15833224),content,new cljs.core.Keyword(null,"truncated","truncated",298102102),truncated], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54481,G__54482,G__54483) : json_response_BANG_.call(null,G__54481,G__54482,G__54483));
}));
} else {
return shadow.esm.esm_import$node_fs$promises.readFile(path_str,"utf8").then((function (text){
var lines = clojure.string.split_lines(text);
var start = (offset - (1));
var stop = (start + limit);
var numbered = cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,line){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((start + idx) + (1)))+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line));
}),cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit,cljs.core.drop.cljs$core$IFn$_invoke$arity$2(start,lines)));
var vec__54490 = (function (){var G__54493 = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",numbered);
return (clip_text.cljs$core$IFn$_invoke$arity$1 ? clip_text.cljs$core$IFn$_invoke$arity$1(G__54493) : clip_text.call(null,G__54493));
})();
var content = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54490,(0),null);
var clipped_QMARK_ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54490,(1),null);
var G__54495 = reply;
var G__54496 = (200);
var G__54497 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"path","path",-188191168),path_str,new cljs.core.Keyword(null,"content","content",15833224),content,new cljs.core.Keyword(null,"truncated","truncated",298102102),(function (){var or__5142__auto__ = clipped_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (stop < cljs.core.count(lines));
}
})()], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54495,G__54496,G__54497) : json_response_BANG_.call(null,G__54495,G__54496,G__54497));
}));
}
})).catch((function (err){
var G__54499 = reply;
var G__54500 = (404);
var G__54501 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54499,G__54500,G__54501) : json_response_BANG_.call(null,G__54499,G__54500,G__54501));
}));
}catch (e54464){var err = e54464;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54458;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54452,G__54453,G__54454,G__54455) : route_BANG_.call(null,G__54452,G__54453,G__54454,G__54455));
});
knoxx.backend.routes.tools.register_write_route_BANG_ = (function knoxx$backend$routes$tools$register_write_route_BANG_(app,runtime,config,deps){
var map__54511 = deps;
var map__54511__$1 = cljs.core.__destructure_map(map__54511);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var resolve_workspace_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"resolve-workspace-path","resolve-workspace-path",-1439207488));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54511__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54513 = app;
var G__54514 = "POST";
var G__54515 = "/api/tools/write";
var G__54516 = (function (){var obj54518 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var role = (function (){var G__54527 = ctx;
var G__54528 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54529 = "write";
var G__54530 = agent_contract_id;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54527,G__54528,G__54529,G__54530) : ensure_role_can_use_BANG_.call(null,G__54527,G__54528,G__54529,G__54530));
})();
var path_str = (function (){var G__54531 = runtime;
var G__54532 = config;
var G__54533 = (function (){var or__5142__auto__ = (body["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return (resolve_workspace_path.cljs$core$IFn$_invoke$arity$3 ? resolve_workspace_path.cljs$core$IFn$_invoke$arity$3(G__54531,G__54532,G__54533) : resolve_workspace_path.call(null,G__54531,G__54532,G__54533));
})();
var raw_content = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var content = (cljs.core.truth_(cljs.core.re_find(/\.svg$/i,path_str))?knoxx.backend.text.sanitize_svg_content(raw_content):raw_content);
var overwrite = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,(body["overwrite"]));
var create_parents = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,(body["create_parents"]));
var parent = shadow.esm.esm_import$node_path.dirname(path_str);
var check_promise = ((overwrite)?Promise.resolve(null):shadow.esm.esm_import$node_fs$promises.stat(path_str).then((function (_){
return Promise.reject((new Error((""+"File exists and overwrite is false: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_str)))));
})).catch((function (_){
return Promise.resolve(null);
})));
return check_promise.then((function (){
if(create_parents){
return shadow.esm.esm_import$node_fs$promises.mkdir(parent,({"recursive": true}));
} else {
return Promise.resolve(null);
}
})).then((function (){
return shadow.esm.esm_import$node_fs$promises.writeFile(path_str,content,"utf8");
})).then((function (){
var G__54546 = reply;
var G__54547 = (200);
var G__54548 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"path","path",-188191168),path_str,new cljs.core.Keyword(null,"bytes_written","bytes_written",-1316873497),Buffer.from(content,"utf8").length], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54546,G__54547,G__54548) : json_response_BANG_.call(null,G__54546,G__54547,G__54548));
})).catch((function (err){
var G__54549 = reply;
var G__54550 = (409);
var G__54551 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54549,G__54550,G__54551) : json_response_BANG_.call(null,G__54549,G__54550,G__54551));
}));
}catch (e54525){var err = e54525;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54518;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54513,G__54514,G__54515,G__54516) : route_BANG_.call(null,G__54513,G__54514,G__54515,G__54516));
});
knoxx.backend.routes.tools.register_edit_route_BANG_ = (function knoxx$backend$routes$tools$register_edit_route_BANG_(app,runtime,config,deps){
var map__54572 = deps;
var map__54572__$1 = cljs.core.__destructure_map(map__54572);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var replace_first = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"replace-first","replace-first",1710901438));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var resolve_workspace_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"resolve-workspace-path","resolve-workspace-path",-1439207488));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var count_occurrences = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"count-occurrences","count-occurrences",1068095177));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54572__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54582 = app;
var G__54583 = "POST";
var G__54584 = "/api/tools/edit";
var G__54585 = (function (){var obj54591 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var role = (function (){var G__54618 = ctx;
var G__54619 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54620 = "edit";
var G__54621 = agent_contract_id;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54618,G__54619,G__54620,G__54621) : ensure_role_can_use_BANG_.call(null,G__54618,G__54619,G__54620,G__54621));
})();
var path_str = (function (){var G__54630 = runtime;
var G__54631 = config;
var G__54632 = (function (){var or__5142__auto__ = (body["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return (resolve_workspace_path.cljs$core$IFn$_invoke$arity$3 ? resolve_workspace_path.cljs$core$IFn$_invoke$arity$3(G__54630,G__54631,G__54632) : resolve_workspace_path.call(null,G__54630,G__54631,G__54632));
})();
var old_string = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["old_string"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var new_string = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["new_string"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var replace_all = (body["replace_all"]) === true;
return shadow.esm.esm_import$node_fs$promises.readFile(path_str,"utf8").then((function (current){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(current.indexOf(old_string),(-1))){
return Promise.reject((new Error("old_string not found in file")));
} else {
var replacements = ((replace_all)?(count_occurrences.cljs$core$IFn$_invoke$arity$2 ? count_occurrences.cljs$core$IFn$_invoke$arity$2(current,old_string) : count_occurrences.call(null,current,old_string)):(1));
var updated = ((replace_all)?clojure.string.replace(current,old_string,new_string):(replace_first.cljs$core$IFn$_invoke$arity$3 ? replace_first.cljs$core$IFn$_invoke$arity$3(current,old_string,new_string) : replace_first.call(null,current,old_string,new_string)));
return shadow.esm.esm_import$node_fs$promises.writeFile(path_str,updated,"utf8").then((function (){
var G__54643 = reply;
var G__54644 = (200);
var G__54645 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"path","path",-188191168),path_str,new cljs.core.Keyword(null,"replacements","replacements",1917839659),replacements], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54643,G__54644,G__54645) : json_response_BANG_.call(null,G__54643,G__54644,G__54645));
}));
}
})).catch((function (err){
var G__54651 = reply;
var G__54652 = (409);
var G__54653 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54651,G__54652,G__54653) : json_response_BANG_.call(null,G__54651,G__54652,G__54653));
}));
}catch (e54609){var err = e54609;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54591;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54582,G__54583,G__54584,G__54585) : route_BANG_.call(null,G__54582,G__54583,G__54584,G__54585));
});
knoxx.backend.routes.tools.register_bash_route_BANG_ = (function knoxx$backend$routes$tools$register_bash_route_BANG_(app,runtime,config,deps){
var map__54656 = deps;
var map__54656__$1 = cljs.core.__destructure_map(map__54656);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var resolve_workspace_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"resolve-workspace-path","resolve-workspace-path",-1439207488));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54656__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54666 = app;
var G__54667 = "POST";
var G__54668 = "/api/tools/bash";
var G__54669 = (function (){var obj54671 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var role = (function (){var G__54691 = ctx;
var G__54692 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54693 = "bash";
var G__54694 = agent_contract_id;
return (ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54691,G__54692,G__54693,G__54694) : ensure_role_can_use_BANG_.call(null,G__54691,G__54692,G__54693,G__54694));
})();
var timeout_ms = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = (body["timeout_ms"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (120000);
}
})(),(1000)),(600000));
var workdir = (function (){var temp__5823__auto__ = (body["workdir"]);
if(cljs.core.truth_(temp__5823__auto__)){
var raw_wd = temp__5823__auto__;
return (resolve_workspace_path.cljs$core$IFn$_invoke$arity$3 ? resolve_workspace_path.cljs$core$IFn$_invoke$arity$3(runtime,config,raw_wd) : resolve_workspace_path.call(null,runtime,config,raw_wd));
} else {
return shadow.esm.esm_import$node_path.resolve(new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config));
}
})();
return (function (){var G__54699 = "/bin/bash";
var G__54700 = ["-lc",(function (){var or__5142__auto__ = (body["command"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()];
var G__54701 = ({"cwd": workdir, "timeout": timeout_ms, "maxBuffer": (1048576)});
return (knoxx.backend.routes.tools.exec_file_async.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.tools.exec_file_async.cljs$core$IFn$_invoke$arity$3(G__54699,G__54700,G__54701) : knoxx.backend.routes.tools.exec_file_async.call(null,G__54699,G__54700,G__54701));
})().then((function (result){
var vec__54707 = (function (){var G__54713 = (function (){var or__5142__auto__ = (result["stdout"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__54714 = (24000);
return (clip_text.cljs$core$IFn$_invoke$arity$2 ? clip_text.cljs$core$IFn$_invoke$arity$2(G__54713,G__54714) : clip_text.call(null,G__54713,G__54714));
})();
var stdout = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54707,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54707,(1),null);
var vec__54710 = (function (){var G__54715 = (function (){var or__5142__auto__ = (result["stderr"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__54716 = (12000);
return (clip_text.cljs$core$IFn$_invoke$arity$2 ? clip_text.cljs$core$IFn$_invoke$arity$2(G__54715,G__54716) : clip_text.call(null,G__54715,G__54716));
})();
var stderr = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54710,(0),null);
var __ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54710,(1),null);
var G__54717 = reply;
var G__54718 = (200);
var G__54719 = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"command","command",-894540724),(function (){var or__5142__auto__ = (body["command"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"exit_code","exit_code",85578367),(0),new cljs.core.Keyword(null,"stdout","stdout",-531490018),stdout,new cljs.core.Keyword(null,"stderr","stderr",-1571650309),stderr], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54717,G__54718,G__54719) : json_response_BANG_.call(null,G__54717,G__54718,G__54719));
})).catch((function (err){
if(cljs.core.truth_((function (){var and__5140__auto__ = (err["killed"]);
if(cljs.core.truth_(and__5140__auto__)){
return (!(typeof (err["code"]) === 'number'));
} else {
return and__5140__auto__;
}
})())){
var G__54720 = reply;
var G__54721 = (408);
var G__54722 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Command timed out after "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((timeout_ms / (1000)))+"s")], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54720,G__54721,G__54722) : json_response_BANG_.call(null,G__54720,G__54721,G__54722));
} else {
var vec__54723 = (function (){var G__54729 = (function (){var or__5142__auto__ = (err["stdout"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__54730 = (24000);
return (clip_text.cljs$core$IFn$_invoke$arity$2 ? clip_text.cljs$core$IFn$_invoke$arity$2(G__54729,G__54730) : clip_text.call(null,G__54729,G__54730));
})();
var stdout = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54723,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54723,(1),null);
var vec__54726 = (function (){var G__54739 = (function (){var or__5142__auto__ = (err["stderr"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__54740 = (12000);
return (clip_text.cljs$core$IFn$_invoke$arity$2 ? clip_text.cljs$core$IFn$_invoke$arity$2(G__54739,G__54740) : clip_text.call(null,G__54739,G__54740));
})();
var stderr = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54726,(0),null);
var __ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__54726,(1),null);
var G__54742 = reply;
var G__54743 = (200);
var G__54744 = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"command","command",-894540724),(function (){var or__5142__auto__ = (body["command"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"exit_code","exit_code",85578367),((typeof (err["code"]) === 'number')?(err["code"]):(1)),new cljs.core.Keyword(null,"stdout","stdout",-531490018),stdout,new cljs.core.Keyword(null,"stderr","stderr",-1571650309),stderr], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54742,G__54743,G__54744) : json_response_BANG_.call(null,G__54742,G__54743,G__54744));
}
}));
}catch (e54681){var err = e54681;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54671;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54666,G__54667,G__54668,G__54669) : route_BANG_.call(null,G__54666,G__54667,G__54668,G__54669));
});
knoxx.backend.routes.tools.register_discord_publish_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_publish_route_BANG_(app,runtime,config,deps){
var map__54760 = deps;
var map__54760__$1 = cljs.core.__destructure_map(map__54760);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var ensure_role_can_use_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"ensure-role-can-use!","ensure-role-can-use!",-210679577));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54760__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54771 = app;
var G__54772 = "POST";
var G__54773 = "/api/tools/discord/publish";
var G__54774 = (function (){var obj54776 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var agent_contract_id = (function (){var or__5142__auto__ = (body["agentContractId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (body["agent_contract_id"]);
}
})();
var G__54792_55182 = ctx;
var G__54793_55183 = (function (){var or__5142__auto__ = (body["role"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var G__54794_55184 = "discord.publish";
var G__54795_55185 = agent_contract_id;
(ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4(G__54792_55182,G__54793_55183,G__54794_55184,G__54795_55185) : ensure_role_can_use_BANG_.call(null,G__54792_55182,G__54793_55183,G__54794_55184,G__54795_55185));

var G__54804 = reply;
var G__54805 = (410);
var G__54806 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"Global Discord publish is disabled. Use actor-owned Discord credentials via Admin \u2192 Actors and the discord.send tool."], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54804,G__54805,G__54806) : json_response_BANG_.call(null,G__54804,G__54805,G__54806));
}catch (e54785){var err = e54785;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54776;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54771,G__54772,G__54773,G__54774) : route_BANG_.call(null,G__54771,G__54772,G__54773,G__54774));
});
knoxx.backend.routes.tools.register_discord_token_get_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_token_get_route_BANG_(app,runtime,config,deps){
var map__54815 = deps;
var map__54815__$1 = cljs.core.__destructure_map(map__54815);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54815__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54827 = app;
var G__54828 = "GET";
var G__54829 = "/api/admin/config/discord";
var G__54830 = (function (){var obj54835 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__54839 = reply;
var G__54840 = (200);
var G__54841 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"configured","configured",-884777889),false,new cljs.core.Keyword(null,"tokenPreview","tokenPreview",-2066034955),"",new cljs.core.Keyword(null,"credentialSource","credentialSource",-355856243),"actor_credentials",new cljs.core.Keyword(null,"detail","detail",-1545345025),"Discord bot keys are configured per actor in Admin \u2192 Actors."], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54839,G__54840,G__54841) : json_response_BANG_.call(null,G__54839,G__54840,G__54841));
})});
return obj54835;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54827,G__54828,G__54829,G__54830) : route_BANG_.call(null,G__54827,G__54828,G__54829,G__54830));
});
knoxx.backend.routes.tools.register_discord_token_put_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_token_put_route_BANG_(app,runtime,config,deps){
var map__54842 = deps;
var map__54842__$1 = cljs.core.__destructure_map(map__54842);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54842__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54843 = app;
var G__54844 = "PUT";
var G__54845 = "/api/admin/config/discord";
var G__54846 = (function (){var obj54848 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__54851 = reply;
var G__54852 = (410);
var G__54853 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"configured","configured",-884777889),false,new cljs.core.Keyword(null,"credentialSource","credentialSource",-355856243),"actor_credentials",new cljs.core.Keyword(null,"detail","detail",-1545345025),"Global Discord token configuration has been migrated. Store Discord bot credentials on an actor in Admin \u2192 Actors."], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54851,G__54852,G__54853) : json_response_BANG_.call(null,G__54851,G__54852,G__54853));
}catch (e54849){var err = e54849;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54848;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54843,G__54844,G__54845,G__54846) : route_BANG_.call(null,G__54843,G__54844,G__54845,G__54846));
});
knoxx.backend.routes.tools.register_event_agents_get_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_get_route_BANG_(app,runtime,config,deps){
var map__54855 = deps;
var map__54855__$1 = cljs.core.__destructure_map(map__54855);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54855__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54857 = app;
var G__54858 = "GET";
var G__54859 = "/api/admin/config/event-agents";
var G__54860 = (function (){var obj54863 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__54864 = reply;
var G__54865 = (200);
var G__54866 = knoxx.backend.routes.tools.event_agents_control_response(config);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54864,G__54865,G__54866) : json_response_BANG_.call(null,G__54864,G__54865,G__54866));
})});
return obj54863;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54857,G__54858,G__54859,G__54860) : route_BANG_.call(null,G__54857,G__54858,G__54859,G__54860));
});
knoxx.backend.routes.tools.register_event_agents_put_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_put_route_BANG_(app,runtime,config,deps){
var map__54867 = deps;
var map__54867__$1 = cljs.core.__destructure_map(map__54867);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54867__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54869 = app;
var G__54870 = "PUT";
var G__54871 = "/api/admin/config/event-agents";
var G__54872 = (function (){var obj54874 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var live_config = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})();
var next_control = knoxx.backend.triggers.control_config.event_agent_control_config(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(live_config,new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),body));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.state.config_STAR_,(function (c){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = c;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})(),new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),next_control);
}));

knoxx.backend.triggers.control_config.persist_event_agent_control_BANG_(next_control);

knoxx.backend.events.runtime.reload_BANG_();

var G__54876 = reply;
var G__54877 = (200);
var G__54878 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54876,G__54877,G__54878) : json_response_BANG_.call(null,G__54876,G__54877,G__54878));
}catch (e54875){var err = e54875;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54874;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54869,G__54870,G__54871,G__54872) : route_BANG_.call(null,G__54869,G__54870,G__54871,G__54872));
});
knoxx.backend.routes.tools.register_event_agents_job_run_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_job_run_route_BANG_(app,runtime,config,deps){
var map__54879 = deps;
var map__54879__$1 = cljs.core.__destructure_map(map__54879);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54879__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54880 = app;
var G__54881 = "POST";
var G__54882 = "/api/admin/config/event-agents/jobs/:jobId/run";
var G__54883 = (function (){var obj54885 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var job_id = (function (){var or__5142__auto__ = (request["params"]["jobId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(job_id)){
var G__54889 = reply;
var G__54890 = (400);
var G__54891 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"jobId is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54889,G__54890,G__54891) : json_response_BANG_.call(null,G__54889,G__54890,G__54891));
} else {
return knoxx.backend.events.runtime.run_job_BANG_(job_id).then((function (_){
var G__54892 = reply;
var G__54893 = (202);
var G__54894 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"jobId","jobId",1965699355),job_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54892,G__54893,G__54894) : json_response_BANG_.call(null,G__54892,G__54893,G__54894));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}
}catch (e54887){var err = e54887;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54885;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54880,G__54881,G__54882,G__54883) : route_BANG_.call(null,G__54880,G__54881,G__54882,G__54883));
});
knoxx.backend.routes.tools.register_event_agents_dispatch_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_dispatch_route_BANG_(app,runtime,config,deps){
var map__54895 = deps;
var map__54895__$1 = cljs.core.__destructure_map(map__54895);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54895__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54896 = app;
var G__54897 = "POST";
var G__54898 = "/api/admin/config/event-agents/events/dispatch";
var G__54899 = (function (){var obj54901 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.events.dispatch.dispatch_BANG_(body).then((function (result){
var G__54926 = reply;
var G__54927 = (202);
var G__54928 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822),new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"event","event",301435442),new cljs.core.Keyword(null,"event","event",301435442).cljs$core$IFn$_invoke$arity$1(result)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54926,G__54927,G__54928) : json_response_BANG_.call(null,G__54926,G__54927,G__54928));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e54925){var err = e54925;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54901;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54896,G__54897,G__54898,G__54899) : route_BANG_.call(null,G__54896,G__54897,G__54898,G__54899));
});
knoxx.backend.routes.tools.register_event_agents_runtime_stop_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_runtime_stop_route_BANG_(app,runtime,config,deps){
var map__54929 = deps;
var map__54929__$1 = cljs.core.__destructure_map(map__54929);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54929__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54930 = app;
var G__54931 = "POST";
var G__54932 = "/api/admin/config/event-agents/runtime/stop";
var G__54933 = (function (){var obj54935 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

knoxx.backend.events.runtime.stop_BANG_();

var G__54936 = reply;
var G__54937 = (200);
var G__54938 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"action","action",-811238024),"stopped"], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54936,G__54937,G__54938) : json_response_BANG_.call(null,G__54936,G__54937,G__54938));
})});
return obj54935;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54930,G__54931,G__54932,G__54933) : route_BANG_.call(null,G__54930,G__54931,G__54932,G__54933));
});
knoxx.backend.routes.tools.register_event_agents_runtime_start_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_runtime_start_route_BANG_(app,runtime,config,deps){
var map__54939 = deps;
var map__54939__$1 = cljs.core.__destructure_map(map__54939);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54939__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54940 = app;
var G__54941 = "POST";
var G__54942 = "/api/admin/config/event-agents/runtime/start";
var G__54943 = (function (){var obj54945 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

knoxx.backend.events.runtime.start_BANG_(config);

var G__54946 = reply;
var G__54947 = (200);
var G__54948 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"action","action",-811238024),"started"], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54946,G__54947,G__54948) : json_response_BANG_.call(null,G__54946,G__54947,G__54948));
})});
return obj54945;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54940,G__54941,G__54942,G__54943) : route_BANG_.call(null,G__54940,G__54941,G__54942,G__54943));
});
knoxx.backend.routes.tools.register_event_agents_runtime_reset_route_BANG_ = (function knoxx$backend$routes$tools$register_event_agents_runtime_reset_route_BANG_(app,runtime,config,deps){
var map__54949 = deps;
var map__54949__$1 = cljs.core.__destructure_map(map__54949);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54949__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54950 = app;
var G__54951 = "POST";
var G__54952 = "/api/admin/config/event-agents/runtime/reset";
var G__54953 = (function (){var obj54955 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

return knoxx.backend.events.runtime.reset_runtime_BANG_(config).then((function (summary){
var G__54957 = reply;
var G__54958 = (200);
var G__54959 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"action","action",-811238024),"reset",new cljs.core.Keyword(null,"reset","reset",-800929946),summary], null)], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54957,G__54958,G__54959) : json_response_BANG_.call(null,G__54957,G__54958,G__54959));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e54956){var err = e54956;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54955;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54950,G__54951,G__54952,G__54953) : route_BANG_.call(null,G__54950,G__54951,G__54952,G__54953));
});
knoxx.backend.routes.tools.register_events_get_route_BANG_ = (function knoxx$backend$routes$tools$register_events_get_route_BANG_(app,runtime,config,deps){
var map__54960 = deps;
var map__54960__$1 = cljs.core.__destructure_map(map__54960);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54960__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54965 = app;
var G__54966 = "GET";
var G__54967 = "/api/admin/config/events";
var G__54968 = (function (){var obj54970 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__54979 = reply;
var G__54980 = (200);
var G__54981 = knoxx.backend.routes.tools.event_agents_control_response(config);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54979,G__54980,G__54981) : json_response_BANG_.call(null,G__54979,G__54980,G__54981));
})});
return obj54970;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54965,G__54966,G__54967,G__54968) : route_BANG_.call(null,G__54965,G__54966,G__54967,G__54968));
});
knoxx.backend.routes.tools.register_events_put_route_BANG_ = (function knoxx$backend$routes$tools$register_events_put_route_BANG_(app,runtime,config,deps){
var map__54982 = deps;
var map__54982__$1 = cljs.core.__destructure_map(map__54982);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54982__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54983 = app;
var G__54984 = "PUT";
var G__54985 = "/api/admin/config/events";
var G__54986 = (function (){var obj54988 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var live_config = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})();
var next_control = knoxx.backend.triggers.control_config.event_agent_control_config(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(live_config,new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),body));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.state.config_STAR_,(function (c){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = c;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})(),new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),next_control);
}));

knoxx.backend.triggers.control_config.persist_event_agent_control_BANG_(next_control);

knoxx.backend.events.runtime.reload_BANG_();

var G__54994 = reply;
var G__54995 = (200);
var G__54996 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54994,G__54995,G__54996) : json_response_BANG_.call(null,G__54994,G__54995,G__54996));
}catch (e54993){var err = e54993;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54988;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54983,G__54984,G__54985,G__54986) : route_BANG_.call(null,G__54983,G__54984,G__54985,G__54986));
});
knoxx.backend.routes.tools.register_events_job_run_route_BANG_ = (function knoxx$backend$routes$tools$register_events_job_run_route_BANG_(app,runtime,config,deps){
var map__55005 = deps;
var map__55005__$1 = cljs.core.__destructure_map(map__55005);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55005__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55006 = app;
var G__55007 = "POST";
var G__55008 = "/api/admin/config/events/jobs/:jobId/run";
var G__55009 = (function (){var obj55011 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var job_id = (function (){var or__5142__auto__ = (request["params"]["jobId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(job_id)){
var G__55017 = reply;
var G__55018 = (400);
var G__55019 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"jobId is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55017,G__55018,G__55019) : json_response_BANG_.call(null,G__55017,G__55018,G__55019));
} else {
return knoxx.backend.events.runtime.run_job_BANG_(job_id).then((function (_){
var G__55020 = reply;
var G__55021 = (202);
var G__55022 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"jobId","jobId",1965699355),job_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55020,G__55021,G__55022) : json_response_BANG_.call(null,G__55020,G__55021,G__55022));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}
}catch (e55016){var err = e55016;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj55011;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55006,G__55007,G__55008,G__55009) : route_BANG_.call(null,G__55006,G__55007,G__55008,G__55009));
});
knoxx.backend.routes.tools.register_events_dispatch_route_BANG_ = (function knoxx$backend$routes$tools$register_events_dispatch_route_BANG_(app,runtime,config,deps){
var map__55023 = deps;
var map__55023__$1 = cljs.core.__destructure_map(map__55023);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55023__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55024 = app;
var G__55025 = "POST";
var G__55026 = "/api/admin/config/events/dispatch";
var G__55027 = (function (){var obj55029 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.events.dispatch.dispatch_BANG_(body).then((function (result){
var G__55031 = reply;
var G__55032 = (202);
var G__55033 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822),new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822).cljs$core$IFn$_invoke$arity$1(result),new cljs.core.Keyword(null,"event","event",301435442),new cljs.core.Keyword(null,"event","event",301435442).cljs$core$IFn$_invoke$arity$1(result)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55031,G__55032,G__55033) : json_response_BANG_.call(null,G__55031,G__55032,G__55033));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e55030){var err = e55030;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj55029;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55024,G__55025,G__55026,G__55027) : route_BANG_.call(null,G__55024,G__55025,G__55026,G__55027));
});
knoxx.backend.routes.tools.register_events_runtime_stop_route_BANG_ = (function knoxx$backend$routes$tools$register_events_runtime_stop_route_BANG_(app,runtime,config,deps){
var map__55034 = deps;
var map__55034__$1 = cljs.core.__destructure_map(map__55034);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55034__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55035 = app;
var G__55036 = "POST";
var G__55037 = "/api/admin/config/events/runtime/stop";
var G__55038 = (function (){var obj55040 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

knoxx.backend.events.runtime.stop_BANG_();

var G__55041 = reply;
var G__55042 = (200);
var G__55043 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"action","action",-811238024),"stopped"], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55041,G__55042,G__55043) : json_response_BANG_.call(null,G__55041,G__55042,G__55043));
})});
return obj55040;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55035,G__55036,G__55037,G__55038) : route_BANG_.call(null,G__55035,G__55036,G__55037,G__55038));
});
knoxx.backend.routes.tools.register_events_runtime_start_route_BANG_ = (function knoxx$backend$routes$tools$register_events_runtime_start_route_BANG_(app,runtime,config,deps){
var map__55044 = deps;
var map__55044__$1 = cljs.core.__destructure_map(map__55044);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55044__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55045 = app;
var G__55046 = "POST";
var G__55047 = "/api/admin/config/events/runtime/start";
var G__55048 = (function (){var obj55050 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

knoxx.backend.events.runtime.start_BANG_(config);

var G__55051 = reply;
var G__55052 = (200);
var G__55053 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"action","action",-811238024),"started"], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55051,G__55052,G__55053) : json_response_BANG_.call(null,G__55051,G__55052,G__55053));
})});
return obj55050;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55045,G__55046,G__55047,G__55048) : route_BANG_.call(null,G__55045,G__55046,G__55047,G__55048));
});
knoxx.backend.routes.tools.register_events_runtime_reset_route_BANG_ = (function knoxx$backend$routes$tools$register_events_runtime_reset_route_BANG_(app,runtime,config,deps){
var map__55054 = deps;
var map__55054__$1 = cljs.core.__destructure_map(map__55054);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55054__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55055 = app;
var G__55056 = "POST";
var G__55057 = "/api/admin/config/events/runtime/reset";
var G__55058 = (function (){var obj55060 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

return knoxx.backend.events.runtime.reset_runtime_BANG_(config).then((function (summary){
var G__55062 = reply;
var G__55063 = (200);
var G__55064 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"action","action",-811238024),"reset",new cljs.core.Keyword(null,"reset","reset",-800929946),summary], null)], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55062,G__55063,G__55064) : json_response_BANG_.call(null,G__55062,G__55063,G__55064));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e55061){var err = e55061;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj55060;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55055,G__55056,G__55057,G__55058) : route_BANG_.call(null,G__55055,G__55056,G__55057,G__55058));
});
knoxx.backend.routes.tools.register_discord_control_get_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_control_get_route_BANG_(app,runtime,config,deps){
var map__55065 = deps;
var map__55065__$1 = cljs.core.__destructure_map(map__55065);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55065__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55066 = app;
var G__55067 = "GET";
var G__55068 = "/api/admin/config/discord/control";
var G__55069 = (function (){var obj55071 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__55072 = reply;
var G__55073 = (200);
var G__55074 = knoxx.backend.routes.tools.event_agents_control_response(config);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55072,G__55073,G__55074) : json_response_BANG_.call(null,G__55072,G__55073,G__55074));
})});
return obj55071;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55066,G__55067,G__55068,G__55069) : route_BANG_.call(null,G__55066,G__55067,G__55068,G__55069));
});
knoxx.backend.routes.tools.register_discord_control_put_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_control_put_route_BANG_(app,runtime,config,deps){
var map__55075 = deps;
var map__55075__$1 = cljs.core.__destructure_map(map__55075);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55075__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55076 = app;
var G__55077 = "PUT";
var G__55078 = "/api/admin/config/discord/control";
var G__55079 = (function (){var obj55081 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var live_config = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})();
var next_control = knoxx.backend.triggers.control_config.event_agent_control_config(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(live_config,new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),body));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.state.config_STAR_,(function (c){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = c;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})(),new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),next_control);
}));

knoxx.backend.triggers.control_config.persist_event_agent_control_BANG_(next_control);

knoxx.backend.events.runtime.reload_BANG_();

var G__55083 = reply;
var G__55084 = (200);
var G__55085 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.tools.event_agents_control_response(config),new cljs.core.Keyword(null,"ok","ok",967785236),true);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55083,G__55084,G__55085) : json_response_BANG_.call(null,G__55083,G__55084,G__55085));
}catch (e55082){var err = e55082;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj55081;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55076,G__55077,G__55078,G__55079) : route_BANG_.call(null,G__55076,G__55077,G__55078,G__55079));
});
knoxx.backend.routes.tools.register_discord_control_job_run_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_control_job_run_route_BANG_(app,runtime,config,deps){
var map__55086 = deps;
var map__55086__$1 = cljs.core.__destructure_map(map__55086);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55086__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55087 = app;
var G__55088 = "POST";
var G__55089 = "/api/admin/config/discord/control/jobs/:jobId/run";
var G__55090 = (function (){var obj55092 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var job_id = (function (){var or__5142__auto__ = (request["params"]["jobId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(job_id)){
var G__55094 = reply;
var G__55095 = (400);
var G__55096 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"jobId is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55094,G__55095,G__55096) : json_response_BANG_.call(null,G__55094,G__55095,G__55096));
} else {
return knoxx.backend.events.runtime.run_job_BANG_(job_id).then((function (_){
var G__55097 = reply;
var G__55098 = (202);
var G__55099 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"jobId","jobId",1965699355),job_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55097,G__55098,G__55099) : json_response_BANG_.call(null,G__55097,G__55098,G__55099));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}
}catch (e55093){var err = e55093;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj55092;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55087,G__55088,G__55089,G__55090) : route_BANG_.call(null,G__55087,G__55088,G__55089,G__55090));
});
knoxx.backend.routes.tools.register_discord_cron_get_route_BANG_ = (function knoxx$backend$routes$tools$register_discord_cron_get_route_BANG_(app,runtime,config,deps){
var map__55100 = deps;
var map__55100__$1 = cljs.core.__destructure_map(map__55100);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55100__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55101 = app;
var G__55102 = "GET";
var G__55103 = "/api/admin/config/discord/cron";
var G__55104 = (function (){var obj55106 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.event_agents.control") : ensure_permission_BANG_.call(null,ctx,"org.event_agents.control"));

var G__55107 = reply;
var G__55108 = (200);
var G__55109 = new cljs.core.Keyword(null,"runtime","runtime",-1331573996).cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.tools.event_agents_control_response(config));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55107,G__55108,G__55109) : json_response_BANG_.call(null,G__55107,G__55108,G__55109));
})});
return obj55106;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55101,G__55102,G__55103,G__55104) : route_BANG_.call(null,G__55101,G__55102,G__55103,G__55104));
});
knoxx.backend.routes.tools.register_mcp_status_route_BANG_ = (function knoxx$backend$routes$tools$register_mcp_status_route_BANG_(app,runtime,config,deps){
var map__55110 = deps;
var map__55110__$1 = cljs.core.__destructure_map(map__55110);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55110__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55111 = app;
var G__55112 = "GET";
var G__55113 = "/api/mcp/status";
var G__55114 = (function (){var obj55116 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [optional_session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var G__55117 = reply;
var G__55118 = (200);
var G__55119 = knoxx.backend.mcp_bridge.status();
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55117,G__55118,G__55119) : json_response_BANG_.call(null,G__55117,G__55118,G__55119));
})});
return obj55116;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55111,G__55112,G__55113,G__55114) : route_BANG_.call(null,G__55111,G__55112,G__55113,G__55114));
});
knoxx.backend.routes.tools.register_mcp_catalog_route_BANG_ = (function knoxx$backend$routes$tools$register_mcp_catalog_route_BANG_(app,runtime,config,deps){
var map__55120 = deps;
var map__55120__$1 = cljs.core.__destructure_map(map__55120);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55120__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55121 = app;
var G__55122 = "GET";
var G__55123 = "/api/mcp/catalog";
var G__55124 = (function (){var obj55126 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [optional_session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var G__55127 = reply;
var G__55128 = (200);
var G__55129 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tools","tools",-1241731990),knoxx.backend.mcp_bridge.catalog(),new cljs.core.Keyword(null,"enabled","enabled",1195909756),knoxx.backend.mcp_bridge.enabled_QMARK_()], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55127,G__55128,G__55129) : json_response_BANG_.call(null,G__55127,G__55128,G__55129));
})});
return obj55126;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55121,G__55122,G__55123,G__55124) : route_BANG_.call(null,G__55121,G__55122,G__55123,G__55124));
});
knoxx.backend.routes.tools.register_mcp_call_route_BANG_ = (function knoxx$backend$routes$tools$register_mcp_call_route_BANG_(app,runtime,config,deps){
var map__55130 = deps;
var map__55130__$1 = cljs.core.__destructure_map(map__55130);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__55130__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__55131 = app;
var G__55132 = "POST";
var G__55133 = "/api/mcp/call";
var G__55134 = (function (){var obj55136 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));

var body_55186 = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var tool_id_55187 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body_55186["toolId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var args_55188 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (body_55186["arguments"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(clojure.string.blank_QMARK_(tool_id_55187)){
var G__55137_55189 = reply;
var G__55138_55190 = (400);
var G__55139_55191 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"toolId is required"], null);
(json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55137_55189,G__55138_55190,G__55139_55191) : json_response_BANG_.call(null,G__55137_55189,G__55138_55190,G__55139_55191));
} else {
knoxx.backend.mcp_bridge.call_tool_BANG_(tool_id_55187,args_55188).then((function (result){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),result) : json_response_BANG_.call(null,reply,(200),result));
})).catch((function (err){
var G__55140 = reply;
var G__55141 = (502);
var G__55142 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"MCP tool call failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (err["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__55140,G__55141,G__55142) : json_response_BANG_.call(null,G__55140,G__55141,G__55142));
}));
}

var G__55143 = new cljs.core.Keyword(null,"default","default",-1987822328);
var G__55144 = knoxx.backend.routes.tools.err;
var G__55145 = (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,knoxx.backend.routes.tools.err) : error_response_BANG_.call(null,reply,knoxx.backend.routes.tools.err));
return (knoxx.backend.routes.tools.catch$.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.tools.catch$.cljs$core$IFn$_invoke$arity$3(G__55143,G__55144,G__55145) : knoxx.backend.routes.tools.catch$.call(null,G__55143,G__55144,G__55145));
})});
return obj55136;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__55131,G__55132,G__55133,G__55134) : route_BANG_.call(null,G__55131,G__55132,G__55133,G__55134));
});
knoxx.backend.routes.tools.register_tool_routes_BANG_ = (function knoxx$backend$routes$tools$register_tool_routes_BANG_(app,runtime,config,deps){
knoxx.backend.routes.tools.register_tool_catalog_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_email_send_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_websearch_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_read_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_write_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_edit_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_bash_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_publish_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_token_get_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_token_put_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_get_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_put_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_job_run_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_dispatch_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_runtime_stop_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_runtime_start_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_event_agents_runtime_reset_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_get_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_put_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_job_run_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_dispatch_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_runtime_stop_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_runtime_start_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_events_runtime_reset_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_control_get_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_control_put_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_control_job_run_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_discord_cron_get_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_mcp_status_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_mcp_catalog_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.tools.register_mcp_call_route_BANG_(app,runtime,config,deps);

return null;
});

//# sourceMappingURL=knoxx.backend.routes.tools.js.map
