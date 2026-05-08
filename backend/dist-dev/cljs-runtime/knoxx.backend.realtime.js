import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.util.time.js";
import "./shadow.esm.esm_import$node_child_process.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$node_os.js";
import "./shadow.esm.esm_import$node_util.js";
goog.provide('knoxx.backend.realtime');
knoxx.backend.realtime.exec_file_async = shadow.esm.esm_import$node_util.promisify(shadow.esm.esm_import$node_child_process.execFile);
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.realtime !== 'undefined') && (typeof knoxx.backend.realtime.ws_clients_STAR_ !== 'undefined')){
} else {
knoxx.backend.realtime.ws_clients_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.realtime !== 'undefined') && (typeof knoxx.backend.realtime.ws_stats_interval_STAR_ !== 'undefined')){
} else {
knoxx.backend.realtime.ws_stats_interval_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.realtime.ws_envelope = (function knoxx$backend$realtime$ws_envelope(channel,payload){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"channel","channel",734187692),channel,new cljs.core.Keyword(null,"timestamp","timestamp",579478971),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"payload","payload",-383036092),payload], null);
});
knoxx.backend.realtime.safe_ws_send_BANG_ = (function knoxx$backend$realtime$safe_ws_send_BANG_(socket,payload){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((socket["readyState"]),(1))){
return socket.send(JSON.stringify(cljs.core.clj__GT_js(payload)));
} else {
return null;
}
});
knoxx.backend.realtime.nvidia_smi_query_args = ["--query-gpu=index,name,utilization.gpu,utilization.memory,memory.used,memory.total,temperature.gpu,power.draw","--format=csv,noheader,nounits"];
knoxx.backend.realtime.parse_float_safe = (function knoxx$backend$realtime$parse_float_safe(value){
var parsed = parseFloat((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
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
knoxx.backend.realtime.mib__GT_bytes = (function knoxx$backend$realtime$mib__GT_bytes(value){
var temp__5825__auto__ = knoxx.backend.realtime.parse_float_safe(value);
if(cljs.core.truth_(temp__5825__auto__)){
var parsed = temp__5825__auto__;
return ((parsed * (1024)) * (1024));
} else {
return null;
}
});
knoxx.backend.realtime.parse_nvidia_smi_line = (function knoxx$backend$realtime$parse_nvidia_smi_line(line){
var vec__51136 = cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2(line,/,/));
var index = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(0),null);
var name = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(1),null);
var util_gpu = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(2),null);
var util_mem = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(3),null);
var mem_used = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(4),null);
var mem_total = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(5),null);
var temp_c = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(6),null);
var power_w = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51136,(7),null);
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"index","index",-1531685915),(function (){var or__5142__auto__ = (function (){var G__51143 = index;
if((G__51143 == null)){
return null;
} else {
return parseInt(G__51143);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "NVIDIA GPU";
}
})(),new cljs.core.Keyword(null,"util_gpu","util_gpu",1856082322),(function (){var or__5142__auto__ = knoxx.backend.realtime.parse_float_safe(util_gpu);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"util_mem","util_mem",563992700),(function (){var or__5142__auto__ = knoxx.backend.realtime.parse_float_safe(util_mem);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"mem_used_bytes","mem_used_bytes",-1607680465),(function (){var or__5142__auto__ = knoxx.backend.realtime.mib__GT_bytes(mem_used);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"mem_total_bytes","mem_total_bytes",-1633418275),(function (){var or__5142__auto__ = knoxx.backend.realtime.mib__GT_bytes(mem_total);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"temp_c","temp_c",-1409598289),knoxx.backend.realtime.parse_float_safe(temp_c),new cljs.core.Keyword(null,"power_w","power_w",1448927329),knoxx.backend.realtime.parse_float_safe(power_w)], null);
});
knoxx.backend.realtime.collect_nvidia_gpu_stats_BANG_ = (function knoxx$backend$realtime$collect_nvidia_gpu_stats_BANG_(_runtime){
return (function (){var G__51157 = "nvidia-smi";
var G__51158 = knoxx.backend.realtime.nvidia_smi_query_args;
var G__51159 = ({"timeout": (1200)});
return (knoxx.backend.realtime.exec_file_async.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.realtime.exec_file_async.cljs$core$IFn$_invoke$arity$3(G__51157,G__51158,G__51159) : knoxx.backend.realtime.exec_file_async.call(null,G__51157,G__51158,G__51159));
})().then((function (result){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.realtime.parse_nvidia_smi_line,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split_lines((function (){var or__5142__auto__ = (result["stdout"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
})).catch((function (_){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}));
});
knoxx.backend.realtime.system_stats_BANG_ = (function knoxx$backend$realtime$system_stats_BANG_(runtime,active_runs_count){
var cpu_count = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),shadow.esm.esm_import$node_os.availableParallelism());
var load1 = (function (){var or__5142__auto__ = (shadow.esm.esm_import$node_os.loadavg()[(0)]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var total_mem = (function (){var or__5142__auto__ = shadow.esm.esm_import$node_os.totalmem();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var free_mem = (function (){var or__5142__auto__ = shadow.esm.esm_import$node_os.freemem();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var used_mem = cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),(total_mem - free_mem));
var cpu_percent = cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),((100) * (load1 / cpu_count)));
var mem_percent = cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),((100) * ((1) - (free_mem / total_mem))));
return knoxx.backend.realtime.collect_nvidia_gpu_stats_BANG_(runtime).then((function (gpu){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"memory_used_bytes","memory_used_bytes",-1404301599),new cljs.core.Keyword(null,"memory_total_bytes","memory_total_bytes",-446657821),new cljs.core.Keyword(null,"active_runs","active_runs",-1033118107),new cljs.core.Keyword(null,"gpu","gpu",437691081),new cljs.core.Keyword(null,"cpu_percent","cpu_percent",-932287404),new cljs.core.Keyword(null,"active_clients","active_clients",-1749706924),new cljs.core.Keyword(null,"network","network",2050004697),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),new cljs.core.Keyword(null,"memory_percent","memory_percent",-61905313)],[used_mem,total_mem,(active_runs_count.cljs$core$IFn$_invoke$arity$0 ? active_runs_count.cljs$core$IFn$_invoke$arity$0() : active_runs_count.call(null)),gpu,cpu_percent,cljs.core.count(cljs.core.deref(knoxx.backend.realtime.ws_clients_STAR_)),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"total_bytes_per_sec","total_bytes_per_sec",438678542),(0),new cljs.core.Keyword(null,"rx_bytes_per_sec","rx_bytes_per_sec",-1256495652),(0),new cljs.core.Keyword(null,"tx_bytes_per_sec","tx_bytes_per_sec",-320326868),(0)], null),knoxx.backend.util.time.now_iso(),mem_percent]);
}));
});
knoxx.backend.realtime.broadcast_ws_BANG_ = (function knoxx$backend$realtime$broadcast_ws_BANG_(channel,payload){
var seq__51214 = cljs.core.seq(cljs.core.deref(knoxx.backend.realtime.ws_clients_STAR_));
var chunk__51215 = null;
var count__51216 = (0);
var i__51217 = (0);
while(true){
if((i__51217 < count__51216)){
var vec__51251 = chunk__51215.cljs$core$IIndexed$_nth$arity$2(null,i__51217);
var client_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51251,(0),null);
var client = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51251,(1),null);
try{knoxx.backend.realtime.safe_ws_send_BANG_((client["socket"]),knoxx.backend.realtime.ws_envelope(channel,payload));
}catch (e51254){var __51450 = e51254;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}

var G__51453 = seq__51214;
var G__51454 = chunk__51215;
var G__51455 = count__51216;
var G__51456 = (i__51217 + (1));
seq__51214 = G__51453;
chunk__51215 = G__51454;
count__51216 = G__51455;
i__51217 = G__51456;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__51214);
if(temp__5825__auto__){
var seq__51214__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__51214__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__51214__$1);
var G__51457 = cljs.core.chunk_rest(seq__51214__$1);
var G__51458 = c__5673__auto__;
var G__51459 = cljs.core.count(c__5673__auto__);
var G__51460 = (0);
seq__51214 = G__51457;
chunk__51215 = G__51458;
count__51216 = G__51459;
i__51217 = G__51460;
continue;
} else {
var vec__51256 = cljs.core.first(seq__51214__$1);
var client_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51256,(0),null);
var client = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51256,(1),null);
try{knoxx.backend.realtime.safe_ws_send_BANG_((client["socket"]),knoxx.backend.realtime.ws_envelope(channel,payload));
}catch (e51260){var __51461 = e51260;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}

var G__51462 = cljs.core.next(seq__51214__$1);
var G__51463 = null;
var G__51464 = (0);
var G__51465 = (0);
seq__51214 = G__51462;
chunk__51215 = G__51463;
count__51216 = G__51464;
i__51217 = G__51465;
continue;
}
} else {
return null;
}
}
break;
}
});
/**
 * True when a realtime client should receive a scoped payload.
 * Conversation id is authoritative when the client already knows it. A blank
 * client conversation id may still match by session id so the first async
 * /chat/start response cannot strand the live stream before the frontend learns
 * the server-generated conversation id.
 */
knoxx.backend.realtime.ws_client_matches_payload_QMARK_ = (function knoxx$backend$realtime$ws_client_matches_payload_QMARK_(client,session_id,payload){
var payload_conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["conversation_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var client_session_id = (function (){var or__5142__auto__ = (client["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var client_conversation_id = (function (){var or__5142__auto__ = (client["conversationId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if((((!(clojure.string.blank_QMARK_(payload_conversation_id)))) && ((!(clojure.string.blank_QMARK_(client_conversation_id)))))){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(payload_conversation_id,client_conversation_id);
} else {
if((!(clojure.string.blank_QMARK_(session_id)))){
return (((!(clojure.string.blank_QMARK_(client_session_id)))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(session_id,client_session_id)));
} else {
return false;

}
}
});
/**
 * Broadcast to clients scoped by conversation-id for isolation.
 * Falls back to session-id matching for clients that have not learned the
 * conversation-id yet. Never broadcasts to all clients.
 */
knoxx.backend.realtime.broadcast_ws_session_BANG_ = (function knoxx$backend$realtime$broadcast_ws_session_BANG_(session_id,channel,payload){
var seq__51266 = cljs.core.seq(cljs.core.deref(knoxx.backend.realtime.ws_clients_STAR_));
var chunk__51267 = null;
var count__51268 = (0);
var i__51269 = (0);
while(true){
if((i__51269 < count__51268)){
var vec__51280 = chunk__51267.cljs$core$IIndexed$_nth$arity$2(null,i__51269);
var client_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51280,(0),null);
var client = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51280,(1),null);
if(knoxx.backend.realtime.ws_client_matches_payload_QMARK_(client,session_id,payload)){
try{knoxx.backend.realtime.safe_ws_send_BANG_((client["socket"]),knoxx.backend.realtime.ws_envelope(channel,payload));
}catch (e51284){var __51466 = e51284;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}} else {
}


var G__51467 = seq__51266;
var G__51468 = chunk__51267;
var G__51469 = count__51268;
var G__51470 = (i__51269 + (1));
seq__51266 = G__51467;
chunk__51267 = G__51468;
count__51268 = G__51469;
i__51269 = G__51470;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__51266);
if(temp__5825__auto__){
var seq__51266__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__51266__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__51266__$1);
var G__51471 = cljs.core.chunk_rest(seq__51266__$1);
var G__51472 = c__5673__auto__;
var G__51473 = cljs.core.count(c__5673__auto__);
var G__51474 = (0);
seq__51266 = G__51471;
chunk__51267 = G__51472;
count__51268 = G__51473;
i__51269 = G__51474;
continue;
} else {
var vec__51288 = cljs.core.first(seq__51266__$1);
var client_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51288,(0),null);
var client = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51288,(1),null);
if(knoxx.backend.realtime.ws_client_matches_payload_QMARK_(client,session_id,payload)){
try{knoxx.backend.realtime.safe_ws_send_BANG_((client["socket"]),knoxx.backend.realtime.ws_envelope(channel,payload));
}catch (e51293){var __51476 = e51293;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}} else {
}


var G__51477 = cljs.core.next(seq__51266__$1);
var G__51478 = null;
var G__51479 = (0);
var G__51480 = (0);
seq__51266 = G__51477;
chunk__51267 = G__51478;
count__51268 = G__51479;
i__51269 = G__51480;
continue;
}
} else {
return null;
}
}
break;
}
});
knoxx.backend.realtime.ensure_ws_stats_loop_BANG_ = (function knoxx$backend$realtime$ensure_ws_stats_loop_BANG_(runtime,active_runs_count){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.realtime.ws_stats_interval_STAR_))){
return null;
} else {
return cljs.core.reset_BANG_(knoxx.backend.realtime.ws_stats_interval_STAR_,setInterval((function (){
if(cljs.core.seq(cljs.core.deref(knoxx.backend.realtime.ws_clients_STAR_))){
return knoxx.backend.realtime.system_stats_BANG_(runtime,active_runs_count).then((function (stats){
return knoxx.backend.realtime.broadcast_ws_BANG_("stats",stats);
})).catch((function (_){
return null;
}));
} else {
return null;
}
}),(5000)));
}
});
knoxx.backend.realtime.stop_BANG_ = (function knoxx$backend$realtime$stop_BANG_(){
var temp__5825__auto___51481 = cljs.core.deref(knoxx.backend.realtime.ws_stats_interval_STAR_);
if(cljs.core.truth_(temp__5825__auto___51481)){
var interval_id_51482 = temp__5825__auto___51481;
clearInterval(interval_id_51482);

cljs.core.reset_BANG_(knoxx.backend.realtime.ws_stats_interval_STAR_,null);
} else {
}

var seq__51297_51483 = cljs.core.seq(cljs.core.deref(knoxx.backend.realtime.ws_clients_STAR_));
var chunk__51298_51484 = null;
var count__51299_51485 = (0);
var i__51300_51486 = (0);
while(true){
if((i__51300_51486 < count__51299_51485)){
var vec__51328_51487 = chunk__51298_51484.cljs$core$IIndexed$_nth$arity$2(null,i__51300_51486);
var client_id_51488 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51328_51487,(0),null);
var client_51489 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51328_51487,(1),null);
var socket_51490 = (client_51489["socket"]);
try{if(cljs.core.truth_(socket_51490)){
socket_51490.close((1001),"server_shutdown");
} else {
}
}catch (e51331){var __51494 = e51331;
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id_51488);


var G__51495 = seq__51297_51483;
var G__51496 = chunk__51298_51484;
var G__51497 = count__51299_51485;
var G__51498 = (i__51300_51486 + (1));
seq__51297_51483 = G__51495;
chunk__51298_51484 = G__51496;
count__51299_51485 = G__51497;
i__51300_51486 = G__51498;
continue;
} else {
var temp__5825__auto___51499 = cljs.core.seq(seq__51297_51483);
if(temp__5825__auto___51499){
var seq__51297_51500__$1 = temp__5825__auto___51499;
if(cljs.core.chunked_seq_QMARK_(seq__51297_51500__$1)){
var c__5673__auto___51501 = cljs.core.chunk_first(seq__51297_51500__$1);
var G__51502 = cljs.core.chunk_rest(seq__51297_51500__$1);
var G__51503 = c__5673__auto___51501;
var G__51504 = cljs.core.count(c__5673__auto___51501);
var G__51505 = (0);
seq__51297_51483 = G__51502;
chunk__51298_51484 = G__51503;
count__51299_51485 = G__51504;
i__51300_51486 = G__51505;
continue;
} else {
var vec__51338_51506 = cljs.core.first(seq__51297_51500__$1);
var client_id_51507 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51338_51506,(0),null);
var client_51508 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__51338_51506,(1),null);
var socket_51510 = (client_51508["socket"]);
try{if(cljs.core.truth_(socket_51510)){
socket_51510.close((1001),"server_shutdown");
} else {
}
}catch (e51345){var __51512 = e51345;
}
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id_51507);


var G__51513 = cljs.core.next(seq__51297_51500__$1);
var G__51514 = null;
var G__51515 = (0);
var G__51516 = (0);
seq__51297_51483 = G__51513;
chunk__51298_51484 = G__51514;
count__51299_51485 = G__51515;
i__51300_51486 = G__51516;
continue;
}
} else {
}
}
break;
}

return true;
});
knoxx.backend.realtime.register_ws_routes_BANG_ = (function knoxx$backend$realtime$register_ws_routes_BANG_(runtime,app,active_runs_count,lounge_messages_STAR_){
knoxx.backend.realtime.ensure_ws_stats_loop_BANG_(runtime,active_runs_count);

return app.route(({"method": "GET", "url": "/ws/stream", "handler": (function (_request,reply){
return reply.code((426)).type("application/json").send(({"error": "WebSocket upgrade required"}));
}), "wsHandler": (function (socket,request){
var ws = (function (){var or__5142__auto__ = (socket["socket"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return socket;
}
})();
var client_id = shadow.esm.esm_import$node_crypto.randomUUID();
var url_params = (function (){try{return (new URL((""+"http://localhost"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["url"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/ws/stream";
}
})()))));
}catch (e51375){var _ = e51375;
return null;
}})();
var session_id = (function (){try{var or__5142__auto__ = url_params.searchParams.get("session_id");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}catch (e51376){var _ = e51376;
return "";
}})();
var conversation_id = (function (){try{var or__5142__auto__ = url_params.searchParams.get("conversation_id");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}catch (e51377){var _ = e51377;
return "";
}})();
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.assoc,client_id,({"socket": ws, "sessionId": session_id, "conversationId": conversation_id}));

ws.on("close",(function (){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}));

ws.on("error",(function (){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.dissoc,client_id);
}));

ws.on("message",(function (data){
try{var msg = JSON.parse((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(data)));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((msg["type"]),"set_conversation")){
var new_cid = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (msg["conversation_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.realtime.ws_clients_STAR_,cljs.core.update,client_id,(function (c){
if(cljs.core.truth_(c)){
return Object.assign(({}),c,({"conversationId": new_cid}));
} else {
return null;
}
}));
} else {
return null;
}
}catch (e51378){var _ = e51378;
return null;
}}));

knoxx.backend.realtime.system_stats_BANG_(runtime,active_runs_count).then((function (stats){
return knoxx.backend.realtime.safe_ws_send_BANG_(ws,knoxx.backend.realtime.ws_envelope("stats",stats));
})).catch((function (_){
return null;
}));

var seq__51414 = cljs.core.seq(cljs.core.take_last((20),cljs.core.deref(lounge_messages_STAR_)));
var chunk__51415 = null;
var count__51416 = (0);
var i__51417 = (0);
while(true){
if((i__51417 < count__51416)){
var msg = chunk__51415.cljs$core$IIndexed$_nth$arity$2(null,i__51417);
knoxx.backend.realtime.safe_ws_send_BANG_(ws,knoxx.backend.realtime.ws_envelope("lounge",msg));


var G__51523 = seq__51414;
var G__51524 = chunk__51415;
var G__51525 = count__51416;
var G__51526 = (i__51417 + (1));
seq__51414 = G__51523;
chunk__51415 = G__51524;
count__51416 = G__51525;
i__51417 = G__51526;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__51414);
if(temp__5825__auto__){
var seq__51414__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__51414__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__51414__$1);
var G__51527 = cljs.core.chunk_rest(seq__51414__$1);
var G__51528 = c__5673__auto__;
var G__51529 = cljs.core.count(c__5673__auto__);
var G__51530 = (0);
seq__51414 = G__51527;
chunk__51415 = G__51528;
count__51416 = G__51529;
i__51417 = G__51530;
continue;
} else {
var msg = cljs.core.first(seq__51414__$1);
knoxx.backend.realtime.safe_ws_send_BANG_(ws,knoxx.backend.realtime.ws_envelope("lounge",msg));


var G__51531 = cljs.core.next(seq__51414__$1);
var G__51532 = null;
var G__51533 = (0);
var G__51534 = (0);
seq__51414 = G__51531;
chunk__51415 = G__51532;
count__51416 = G__51533;
i__51417 = G__51534;
continue;
}
} else {
return null;
}
}
break;
}
})}));
});

//# sourceMappingURL=knoxx.backend.realtime.js.map
