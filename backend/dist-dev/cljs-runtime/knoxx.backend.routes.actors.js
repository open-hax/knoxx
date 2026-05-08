import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.actor_mailbox.js";
import "./knoxx.backend.events.dispatch.js";
import "./knoxx.backend.macros.js";
goog.provide('knoxx.backend.routes.actors');
knoxx.backend.routes.actors.query_param = (function knoxx$backend$routes$actors$query_param(var_args){
var args__5882__auto__ = [];
var len__5876__auto___54613 = arguments.length;
var i__5877__auto___54614 = (0);
while(true){
if((i__5877__auto___54614 < len__5876__auto___54613)){
args__5882__auto__.push((arguments[i__5877__auto___54614]));

var G__54615 = (i__5877__auto___54614 + (1));
i__5877__auto___54614 = G__54615;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic = (function (request,names){
return cljs.core.some((function (name){
var value = (request["query"][name]);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
return null;
} else {
return value;
}
}),names);
}));

(knoxx.backend.routes.actors.query_param.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.backend.routes.actors.query_param.cljs$lang$applyTo = (function (seq54343){
var G__54344 = cljs.core.first(seq54343);
var seq54343__$1 = cljs.core.next(seq54343);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__54344,seq54343__$1);
}));

knoxx.backend.routes.actors.body_map = (function knoxx$backend$routes$actors$body_map(request){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
});
knoxx.backend.routes.actors.statuses_from_body = (function knoxx$backend$routes$actors$statuses_from_body(body){
var raw = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"statuses","statuses",710922046).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(body);
}
})();
if(cljs.core.sequential_QMARK_(raw)){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.actor_mailbox.normalize_status,raw);
} else {
if(typeof raw === 'string'){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.actor_mailbox.normalize_status(raw)], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["pending","failed"], null);

}
}
});
knoxx.backend.routes.actors.current_actor_id = (function knoxx$backend$routes$actors$current_actor_id(ctx){
var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"membership","membership",254556333),new cljs.core.Keyword(null,"actorId","actorId",989542370)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(ctx);
}
}
});
knoxx.backend.routes.actors.api_entry = (function knoxx$backend$routes$actors$api_entry(entry){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"expiresAt","expiresAt",1882778246),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"delivery","delivery",-1844470516),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"acknowledgedAt","acknowledgedAt",-2025454257),new cljs.core.Keyword(null,"preview","preview",451279890),new cljs.core.Keyword(null,"lastError","lastError",845794675),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"target","target",253001721),new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"deliveredAt","deliveredAt",510515582),new cljs.core.Keyword(null,"contentRef","contentRef",625680927)],[new cljs.core.Keyword("mailbox","updated-at","mailbox/updated-at",-779421100).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","expires-at","mailbox/expires-at",-1256489474).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","created-at","mailbox/created-at",-1406815032).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","source","mailbox/source",-1264954567).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","acknowledged-at","mailbox/acknowledged-at",1312417597).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","preview","mailbox/preview",-512838338).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","last-error","mailbox/last-error",997868945).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","kind","mailbox/kind",401992993).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","target","mailbox/target",1100093613).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","metadata","mailbox/metadata",-1698257615).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","delivered-at","mailbox/delivered-at",-1353109945).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","content-ref","mailbox/content-ref",877031624).cljs$core$IFn$_invoke$arity$1(entry)]);
});
knoxx.backend.routes.actors.api_result = (function knoxx$backend$routes$actors$api_result(result){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"entries","entries",-86943161),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.actors.api_entry,new cljs.core.Keyword(null,"entries","entries",-86943161).cljs$core$IFn$_invoke$arity$1(result)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"durable","durable",-216004834),cljs.core.boolean$(new cljs.core.Keyword(null,"durable?","durable?",2084525683).cljs$core$IFn$_invoke$arity$1(result))], 0)),new cljs.core.Keyword(null,"durable?","durable?",2084525683));
});
knoxx.backend.routes.actors.retry_dispatches_BANG_ = (function knoxx$backend$routes$actors$retry_dispatches_BANG_(entries){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (entry){
return knoxx.backend.events.dispatch.dispatch_BANG_(knoxx.backend.actor_mailbox.retry_request_event(entry));
}),entries)));
});
knoxx.backend.routes.actors.actor_mailbox_list_route_BANG_ = (function knoxx$backend$routes$actors$actor_mailbox_list_route_BANG_(app,runtime,config,deps){
var map__54378 = deps;
var map__54378__$1 = cljs.core.__destructure_map(map__54378);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54378__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54383 = app;
var G__54384 = "GET";
var G__54385 = "/api/admin/config/actors/mailbox";
var G__54386 = (function (){var obj54391 = ({"preHandler":(function (request,reply,done){
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

return knoxx.backend.actor_mailbox.list_entries_BANG_(runtime,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"status","status",-1997798413),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["status"], 0)),new cljs.core.Keyword(null,"target-actor-id","target-actor-id",1128799845),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["target_actor_id","targetActorId","actor_id","actorId"], 0)),new cljs.core.Keyword(null,"target-session-id","target-session-id",-1929186990),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["target_session_id","targetSessionId","session_id","sessionId"], 0)),new cljs.core.Keyword(null,"source-actor-id","source-actor-id",-1224551760),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["source_actor_id","sourceActorId"], 0)),new cljs.core.Keyword(null,"source-run-id","source-run-id",-2000058256),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["source_run_id","sourceRunId","run_id","runId"], 0)),new cljs.core.Keyword(null,"limit","limit",-1355822363),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["limit"], 0))], null)).then((function (result){
var G__54396 = reply;
var G__54397 = (200);
var G__54398 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(knoxx.backend.routes.actors.api_result(result),new cljs.core.Keyword(null,"ok","ok",967785236),true);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54396,G__54397,G__54398) : json_response_BANG_.call(null,G__54396,G__54397,G__54398));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e54395){var err = e54395;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54391;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54383,G__54384,G__54385,G__54386) : route_BANG_.call(null,G__54383,G__54384,G__54385,G__54386));
});
knoxx.backend.routes.actors.acknowledge_mailbox_BANG_ = (function knoxx$backend$routes$actors$acknowledge_mailbox_BANG_(var_args){
var G__54400 = arguments.length;
switch (G__54400) {
case 5:
return knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (runtime,reply,error_response_BANG_,json_response_BANG_,mailbox_id){
return knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,reply,error_response_BANG_,json_response_BANG_,mailbox_id,null);
}));

(knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$6 = (function (runtime,reply,error_response_BANG_,json_response_BANG_,mailbox_id,target_actor_id){
return knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,mailbox_id,target_actor_id).then((function (entry){
if(cljs.core.truth_(entry)){
var G__54403 = reply;
var G__54404 = (200);
var G__54405 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"entry","entry",505168823),knoxx.backend.routes.actors.api_entry(entry)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54403,G__54404,G__54405) : json_response_BANG_.call(null,G__54403,G__54404,G__54405));
} else {
var G__54406 = reply;
var G__54407 = (404);
var G__54408 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"mailbox entry not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54406,G__54407,G__54408) : json_response_BANG_.call(null,G__54406,G__54407,G__54408));
}
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}));

(knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$lang$maxFixedArity = 6);

knoxx.backend.routes.actors.actor_mailbox_ack_route_BANG_ = (function knoxx$backend$routes$actors$actor_mailbox_ack_route_BANG_(app,runtime,config,deps){
var map__54424 = deps;
var map__54424__$1 = cljs.core.__destructure_map(map__54424);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54424__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54428 = app;
var G__54429 = "POST";
var G__54430 = "/api/admin/config/actors/mailbox/:mailboxId/ack";
var G__54431 = (function (){var obj54433 = ({"preHandler":(function (request,reply,done){
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

return knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$5(runtime,reply,error_response_BANG_,json_response_BANG_,(request["params"]["mailboxId"]));
}catch (e54441){var err = e54441;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54433;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54428,G__54429,G__54430,G__54431) : route_BANG_.call(null,G__54428,G__54429,G__54430,G__54431));
});
knoxx.backend.routes.actors.actor_mailbox_retry_route_BANG_ = (function knoxx$backend$routes$actors$actor_mailbox_retry_route_BANG_(app,runtime,config,deps){
var map__54451 = deps;
var map__54451__$1 = cljs.core.__destructure_map(map__54451);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54451__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54460 = app;
var G__54461 = "POST";
var G__54462 = "/api/admin/config/actors/mailbox/retry";
var G__54463 = (function (){var obj54467 = ({"preHandler":(function (request,reply,done){
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

var body = knoxx.backend.routes.actors.body_map(request);
var dispatch_events_QMARK_ = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,new cljs.core.Keyword(null,"dispatch_events","dispatch_events",1219095071).cljs$core$IFn$_invoke$arity$1(body));
return knoxx.backend.actor_mailbox.retry_eligible_BANG_(runtime,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"mailbox-id","mailbox-id",796861681),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mailbox_id","mailbox_id",1368174469).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"mailboxId","mailboxId",-395830287).cljs$core$IFn$_invoke$arity$1(body);
}
})(),new cljs.core.Keyword(null,"statuses","statuses",710922046),knoxx.backend.routes.actors.statuses_from_body(body),new cljs.core.Keyword(null,"max-attempts","max-attempts",1686564297),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"max_attempts","max_attempts",541538771).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"maxAttempts","maxAttempts",250760336).cljs$core$IFn$_invoke$arity$1(body);
}
})(),new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.Keyword(null,"limit","limit",-1355822363).cljs$core$IFn$_invoke$arity$1(body),new cljs.core.Keyword(null,"delay-seconds","delay-seconds",-1391031133),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"delay_seconds","delay_seconds",2065814715).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"delaySeconds","delaySeconds",1562379005).cljs$core$IFn$_invoke$arity$1(body);
}
})()], null)).then((function (result){
var entries = new cljs.core.Keyword(null,"entries","entries",-86943161).cljs$core$IFn$_invoke$arity$1(result);
if(((dispatch_events_QMARK_) && (cljs.core.seq(entries)))){
return knoxx.backend.routes.actors.retry_dispatches_BANG_(entries).then((function (dispatch_results){
var G__54484 = reply;
var G__54485 = (202);
var G__54486 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.actors.api_result(result),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"retry_event_count","retry_event_count",-1329491888),cljs.core.count(entries),new cljs.core.Keyword(null,"dispatches","dispatches",-331249187),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(dispatch_results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54484,G__54485,G__54486) : json_response_BANG_.call(null,G__54484,G__54485,G__54486));
}));
} else {
var G__54487 = reply;
var G__54488 = (202);
var G__54489 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.actors.api_result(result),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"retry_event_count","retry_event_count",-1329491888),(0)], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54487,G__54488,G__54489) : json_response_BANG_.call(null,G__54487,G__54488,G__54489));
}
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}catch (e54478){var err = e54478;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54467;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54460,G__54461,G__54462,G__54463) : route_BANG_.call(null,G__54460,G__54461,G__54462,G__54463));
});
knoxx.backend.routes.actors.actor_mailbox_self_list_route_BANG_ = (function knoxx$backend$routes$actors$actor_mailbox_self_list_route_BANG_(app,runtime,config,deps){
var map__54502 = deps;
var map__54502__$1 = cljs.core.__destructure_map(map__54502);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54502__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54504 = app;
var G__54505 = "GET";
var G__54506 = "/api/actors/mailbox";
var G__54507 = (function (){var obj54510 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));

var actor_id = knoxx.backend.routes.actors.current_actor_id(ctx);
var box = (function (){var G__54524 = (function (){var or__5142__auto__ = knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["box"], 0));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "inbox";
}
})();
var G__54524__$1 = (((G__54524 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__54524)));
if((G__54524__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__54524__$1);
}
})();
var filters = (function (){var G__54526 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["status"], 0)),new cljs.core.Keyword(null,"limit","limit",-1355822363),knoxx.backend.routes.actors.query_param.cljs$core$IFn$_invoke$arity$variadic(request,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["limit"], 0))], null);
var G__54526__$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(box,"outbox"))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__54526,new cljs.core.Keyword(null,"source-actor-id","source-actor-id",-1224551760),actor_id):G__54526);
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(box,"outbox")){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__54526__$1,new cljs.core.Keyword(null,"target-actor-id","target-actor-id",1128799845),actor_id);
} else {
return G__54526__$1;
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))){
var G__54534 = reply;
var G__54535 = (403);
var G__54536 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"current actor is not available"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54534,G__54535,G__54536) : json_response_BANG_.call(null,G__54534,G__54535,G__54536));
} else {
return knoxx.backend.actor_mailbox.list_entries_BANG_(runtime,filters).then((function (result){
var G__54539 = reply;
var G__54540 = (200);
var G__54541 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.routes.actors.api_result(result),new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"box","box",1530920394),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(box,"outbox"))?"outbox":"inbox"),new cljs.core.Keyword(null,"actorId","actorId",989542370),actor_id], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54539,G__54540,G__54541) : json_response_BANG_.call(null,G__54539,G__54540,G__54541));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
}
}catch (e54521){var err = e54521;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54510;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54504,G__54505,G__54506,G__54507) : route_BANG_.call(null,G__54504,G__54505,G__54506,G__54507));
});
knoxx.backend.routes.actors.actor_mailbox_self_ack_route_BANG_ = (function knoxx$backend$routes$actors$actor_mailbox_self_ack_route_BANG_(app,runtime,config,deps){
var map__54552 = deps;
var map__54552__$1 = cljs.core.__destructure_map(map__54552);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54552__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__54555 = app;
var G__54556 = "POST";
var G__54557 = "/api/actors/mailbox/:mailboxId/ack";
var G__54558 = (function (){var obj54562 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [session_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
try{(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));

var actor_id = knoxx.backend.routes.actors.current_actor_id(ctx);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))){
var G__54569 = reply;
var G__54570 = (403);
var G__54571 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"detail","detail",-1545345025),"current actor is not available"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__54569,G__54570,G__54571) : json_response_BANG_.call(null,G__54569,G__54570,G__54571));
} else {
return knoxx.backend.routes.actors.acknowledge_mailbox_BANG_.cljs$core$IFn$_invoke$arity$6(runtime,reply,error_response_BANG_,json_response_BANG_,(request["params"]["mailboxId"]),actor_id);
}
}catch (e54566){var err = e54566;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}})});
return obj54562;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__54555,G__54556,G__54557,G__54558) : route_BANG_.call(null,G__54555,G__54556,G__54557,G__54558));
});
knoxx.backend.routes.actors.register_actor_routes_BANG_ = (function knoxx$backend$routes$actors$register_actor_routes_BANG_(app,runtime,config,deps){
knoxx.backend.routes.actors.actor_mailbox_list_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.actors.actor_mailbox_ack_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.actors.actor_mailbox_retry_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.actors.actor_mailbox_self_list_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.actors.actor_mailbox_self_ack_route_BANG_(app,runtime,config,deps);

return null;
});

//# sourceMappingURL=knoxx.backend.routes.actors.js.map
