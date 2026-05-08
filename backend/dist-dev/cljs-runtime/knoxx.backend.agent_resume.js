import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_turns.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.turn_control.js";
import "./knoxx.backend.util.time.js";
goog.provide('knoxx.backend.agent_resume');
knoxx.backend.agent_resume.STALE_THRESHOLD_MS = (((10) * (60)) * (1000));
knoxx.backend.agent_resume.POST_DRAIN_GRACE_MS = (1000);
knoxx.backend.agent_resume.RECOVERY_INTERVAL_MS = (15000);
knoxx.backend.agent_resume.STARTUP_RESUME_CONCURRENCY = (2);
knoxx.backend.agent_resume.RECOVERY_COOLDOWN_MS = (60000);
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agent_resume !== 'undefined') && (typeof knoxx.backend.agent_resume.interval_handle_STAR_ !== 'undefined')){
} else {
knoxx.backend.agent_resume.interval_handle_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.agent_resume.log_info_BANG_ = (function knoxx$backend$agent_resume$log_info_BANG_(var_args){
var G__53947 = arguments.length;
switch (G__53947) {
case 2:
return knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (app,msg){
return knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,msg,null);
}));

(knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (app,msg,err){
var log = app.log;
if(cljs.core.truth_(err)){
return log.error(msg,err);
} else {
return log.info(msg);
}
}));

(knoxx.backend.agent_resume.log_info_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.agent_resume.log_warn_BANG_ = (function knoxx$backend$agent_resume$log_warn_BANG_(app,msg){
var log = app.log;
return log.warn(msg);
});
knoxx.backend.agent_resume.session_last_updated_ms = (function knoxx$backend$agent_resume$session_last_updated_ms(session){
var ts = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"shutdown_requested_at","shutdown_requested_at",1380231631).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return new cljs.core.Keyword(null,"recovered_at","recovered_at",-474781568).cljs$core$IFn$_invoke$arity$1(session);
}
}
}
})();
if(typeof ts === 'number'){
return ts;
} else {
if(typeof ts === 'string'){
try{return (new Date(ts)).getTime();
}catch (e53954){var _ = e53954;
return (0);
}} else {
return (0);

}
}
});
knoxx.backend.agent_resume.session_stale_QMARK_ = (function knoxx$backend$agent_resume$session_stale_QMARK_(session){
var last_ms = knoxx.backend.agent_resume.session_last_updated_ms(session);
return (((last_ms > (0))) && (((Date.now() - last_ms) >= knoxx.backend.agent_resume.STALE_THRESHOLD_MS)));
});
knoxx.backend.agent_resume.runtime_processing_session_QMARK_ = (function knoxx$backend$agent_resume$runtime_processing_session_QMARK_(conversation_id){
var active = knoxx.backend.agent_runtime.active_agent_session(conversation_id);
var streaming_QMARK_ = (function (){var and__5140__auto__ = active;
if(cljs.core.truth_(and__5140__auto__)){
return (active["isStreaming"]) === true;
} else {
return and__5140__auto__;
}
})();
var current_turn_QMARK_ = (function (){var and__5140__auto__ = active;
if(cljs.core.truth_(and__5140__auto__)){
try{return (!(((active["currentTurn"]) == null)));
}catch (e53961){if((e53961 instanceof Error)){
var _ = e53961;
return false;
} else {
throw e53961;

}
}} else {
return and__5140__auto__;
}
})();
var registered_turn_QMARK_ = (!((knoxx.backend.turn_control.active_turn(conversation_id) == null)));
var or__5142__auto__ = streaming_QMARK_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = current_turn_QMARK_;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return registered_turn_QMARK_;
}
}
});
/**
 * A session is 'hot' if it was updated very recently. Recovery skips hot
 * sessions so that in-flight runs (e.g. those orphaned by event-agents/reload!)
 * have time to finish naturally instead of being duplicated.
 */
knoxx.backend.agent_resume.session_hot_QMARK_ = (function knoxx$backend$agent_resume$session_hot_QMARK_(session){
var last_ms = knoxx.backend.agent_resume.session_last_updated_ms(session);
return (((last_ms > (0))) && (((Date.now() - last_ms) < knoxx.backend.agent_resume.RECOVERY_COOLDOWN_MS)));
});
knoxx.backend.agent_resume.session_resumable_QMARK_ = (function knoxx$backend$agent_resume$session_resumable_QMARK_(session){
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
return (((!(knoxx.backend.agent_resume.session_hot_QMARK_(session)))) && (cljs.core.not(knoxx.backend.agent_resume.runtime_processing_session_QMARK_(conversation_id))));
});
knoxx.backend.agent_resume.abort_stale_session_BANG_ = (function knoxx$backend$agent_resume$abort_stale_session_BANG_(session){
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var client = knoxx.backend.redis_client.get_client();
if(((clojure.string.blank_QMARK_(session_id)) || ((client == null)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"action","action",-811238024),"abort_skipped",new cljs.core.Keyword(null,"reason","reason",-2070751759),"missing session_id or redis"], null));
} else {
return knoxx.backend.session_store.complete_session_BANG_(client,session_id,conversation_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),"Session aborted automatically: stale (> 10 min)",new cljs.core.Keyword(null,"messages","messages",345434482),new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session)], null)).then((function (_){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"action","action",-811238024),"aborted",new cljs.core.Keyword(null,"reason","reason",-2070751759),"stale"], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"action","action",-811238024),"abort_failed",new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
}
});
knoxx.backend.agent_resume.resume_session_BANG_ = (function knoxx$backend$agent_resume$resume_session_BANG_(runtime,config,session){
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(knoxx.backend.agent_resume.session_resumable_QMARK_(session)){
return knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,session,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"wait-for","wait-for",603509654),new cljs.core.Keyword(null,"kickoff","kickoff",-1736115645)], null)).then((function (result){
if(cljs.core.truth_(new cljs.core.Keyword(null,"resumed","resumed",897761340).cljs$core$IFn$_invoke$arity$1(result))){
} else {
console.warn("[agent-resume] session did not resume",({"sessionId": session_id, "conversationId": conversation_id, "reason": new cljs.core.Keyword(null,"reason","reason",-2070751759).cljs$core$IFn$_invoke$arity$1(result)}));
}

return result;
})).catch((function (err){
console.error("[agent-resume] resume failed",({"sessionId": session_id, "conversationId": conversation_id, "error": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))}));

return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),false,new cljs.core.Keyword(null,"reason","reason",-2070751759),"already_processing"], null));
}
});
/**
 * Run promise-returning item-fn over items with bounded concurrency.
 * This keeps startup recovery active without launching hundreds of agent turns at once.
 */
knoxx.backend.agent_resume.run_limited_BANG_ = (function knoxx$backend$agent_resume$run_limited_BANG_(items,limit,item_fn){
var queue = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.vec(items));
var results = [];
var worker = (function knoxx$backend$agent_resume$run_limited_BANG__$_worker(){
var temp__5823__auto__ = cljs.core.first(cljs.core.deref(queue));
if(cljs.core.truth_(temp__5823__auto__)){
var item = temp__5823__auto__;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(queue,cljs.core.subvec,(1));

return (item_fn.cljs$core$IFn$_invoke$arity$1 ? item_fn.cljs$core$IFn$_invoke$arity$1(item) : item_fn.call(null,item)).then((function (result){
results.push(result);

return knoxx$backend$agent_resume$run_limited_BANG__$_worker();
})).catch((function (err){
results.push(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));

return knoxx$backend$agent_resume$run_limited_BANG__$_worker();
}));
} else {
return Promise.resolve(results);
}
});
var worker_count = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),limit),cljs.core.count(items));
if((worker_count === (0))){
return Promise.resolve(results);
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.repeatedly.cljs$core$IFn$_invoke$arity$2(worker_count,worker))).then((function (_){
return results;
}));
}
});
knoxx.backend.agent_resume.process_sessions_BANG_ = (function knoxx$backend$agent_resume$process_sessions_BANG_(runtime,app,config,sessions){
var map__53968 = cljs.core.group_by(knoxx.backend.agent_resume.session_stale_QMARK_,sessions);
var map__53968__$1 = cljs.core.__destructure_map(map__53968);
var stale = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53968__$1,true);
var recent = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53968__$1,false);
var resumable_recent = cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_resume.session_resumable_QMARK_,recent);
if(cljs.core.seq(stale)){
knoxx.backend.agent_resume.log_warn_BANG_(app,(""+"[agent-resume] aborting "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(stale))+" stale session(s)"));
} else {
}

if(cljs.core.seq(resumable_recent)){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2(app,(""+"[agent-resume] resuming "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(resumable_recent))+" recent session(s)"));
} else {
}

return Promise.all([Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_resume.abort_stale_session_BANG_,stale))).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] abort batch error",err);

return null;
})),knoxx.backend.agent_resume.run_limited_BANG_(resumable_recent,knoxx.backend.agent_resume.STARTUP_RESUME_CONCURRENCY,(function (p1__53966_SHARP_){
return knoxx.backend.agent_resume.resume_session_BANG_(runtime,config,p1__53966_SHARP_);
})).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] resume batch error",err);

return null;
}))]).then((function (_){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"stale","stale",395586896),cljs.core.count(stale),new cljs.core.Keyword(null,"resumed","resumed",897761340),cljs.core.count(resumable_recent),new cljs.core.Keyword(null,"skipped","skipped",-1144887090),(cljs.core.count(recent) - cljs.core.count(resumable_recent))], null);
}));
});
/**
 * Fire-and-forget scan of Redis active sessions on startup.
 * Returns a promise for testability, but callers should not await it
 * on the critical startup path.
 */
knoxx.backend.agent_resume.resume_on_startup_BANG_ = (function knoxx$backend$agent_resume$resume_on_startup_BANG_(runtime,app,config){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return knoxx.backend.session_store.recover_sessions_BANG_(client).then((function (sessions){
var running = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53969_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__53969_SHARP_));
}),sessions));
if(cljs.core.seq(running)){
return knoxx.backend.agent_resume.process_sessions_BANG_(runtime,app,config,running);
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"stale","stale",395586896),(0),new cljs.core.Keyword(null,"resumed","resumed",897761340),(0),new cljs.core.Keyword(null,"skipped","skipped",-1144887090),(0)], null);
}
})).then((function (result){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2(app,(""+"[agent-resume] startup scan complete: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([result], 0)))));

return result;
})).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] startup scan failed",err);

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
} else {
knoxx.backend.agent_resume.log_warn_BANG_(app,"[agent-resume] Redis unavailable; skipping startup scan");

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"skipped","skipped",-1144887090),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"redis_not_connected"], null));
}
});
knoxx.backend.agent_resume.attempt_recovery_BANG_ = (function knoxx$backend$agent_resume$attempt_recovery_BANG_(runtime,app,config){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return knoxx.backend.session_store.recover_sessions_BANG_(client).then((function (sessions){
var running = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53970_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__53970_SHARP_));
}),sessions));
var map__53972 = cljs.core.group_by(knoxx.backend.agent_resume.session_stale_QMARK_,running);
var map__53972__$1 = cljs.core.__destructure_map(map__53972);
var stale = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53972__$1,true);
var recent = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53972__$1,false);
var resumable = cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_resume.session_resumable_QMARK_,recent);
if(cljs.core.seq(stale)){
knoxx.backend.agent_resume.log_warn_BANG_(app,(""+"[agent-resume] aborting "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(stale))+" stale session(s)"));
} else {
}

if(cljs.core.seq(resumable)){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2(app,(""+"[agent-resume] resuming "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(resumable))+" recent session(s)"));
} else {
}

return Promise.all([Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agent_resume.abort_stale_session_BANG_,stale))).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] abort batch error",err);

return null;
})),Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__53971_SHARP_){
return knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,p1__53971_SHARP_,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"wait-for","wait-for",603509654),new cljs.core.Keyword(null,"kickoff","kickoff",-1736115645)], null));
}),resumable))).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] resume batch error",err);

return null;
}))]).then((function (_){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"stale","stale",395586896),cljs.core.count(stale),new cljs.core.Keyword(null,"resumed","resumed",897761340),cljs.core.count(resumable),new cljs.core.Keyword(null,"skipped","skipped",-1144887090),(cljs.core.count(recent) - cljs.core.count(resumable))], null);
}));
})).catch((function (err){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] recovery tick error",err);

knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$3(app,"[agent-resume] redis:",(knoxx.backend.redis_client.get_client() == null));

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"skipped","skipped",-1144887090),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"redis_not_connected"], null));
}
});
knoxx.backend.agent_resume.start_periodic_recovery_BANG_ = (function knoxx$backend$agent_resume$start_periodic_recovery_BANG_(runtime,app,config){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.agent_resume.interval_handle_STAR_))){
return null;
} else {
return cljs.core.reset_BANG_(knoxx.backend.agent_resume.interval_handle_STAR_,setInterval((function (){
if((knoxx.backend.redis_client.get_client() == null)){
knoxx.backend.redis_client.init_redis_BANG_(new cljs.core.Keyword(null,"redis-url","redis-url",1921993939).cljs$core$IFn$_invoke$arity$1(config)).catch((function (_){
return null;
}));
} else {
}

return knoxx.backend.agent_resume.attempt_recovery_BANG_(runtime,app,config).catch((function (_){
return null;
}));
}),knoxx.backend.agent_resume.RECOVERY_INTERVAL_MS));
}
});
knoxx.backend.agent_resume.stop_periodic_recovery_BANG_ = (function knoxx$backend$agent_resume$stop_periodic_recovery_BANG_(){
var temp__5825__auto__ = cljs.core.deref(knoxx.backend.agent_resume.interval_handle_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var id = temp__5825__auto__;
clearInterval(id);

return cljs.core.reset_BANG_(knoxx.backend.agent_resume.interval_handle_STAR_,null);
} else {
return null;
}
});
/**
 * Called by graceful-shutdown when active turns time out.
 */
knoxx.backend.agent_resume.mark_sessions_resumable_BANG_ = (function knoxx$backend$agent_resume$mark_sessions_resumable_BANG_(client,active_turns,signal){
var stamp = knoxx.backend.util.time.now_iso();
if(cljs.core.empty_QMARK_(active_turns)){
return Promise.resolve((0));
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__54030){
var map__54031 = p__54030;
var map__54031__$1 = cljs.core.__destructure_map(map__54031);
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54031__$1,new cljs.core.Keyword(null,"session_id","session_id",1584799627));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54031__$1,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__54031__$1,new cljs.core.Keyword(null,"run_id","run_id",-556768024));
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
return Promise.resolve(null);
} else {
return knoxx.backend.session_store.update_session_BANG_(client,session_id,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"shutdown_requested_at","shutdown_requested_at",1380231631),stamp,new cljs.core.Keyword(null,"shutdown_signal","shutdown_signal",-2065919667),signal], null));
}
}),active_turns))).then((function (_){
return cljs.core.count(active_turns);
}));
}
});
/**
 * Wait for turn-control to drain, then give Redis a grace window to persist.
 * Returns a promise.
 */
knoxx.backend.agent_resume.wait_for_turns_and_flush_BANG_ = (function knoxx$backend$agent_resume$wait_for_turns_and_flush_BANG_(app,config){
var grace_ms = (function (){var v = new cljs.core.Keyword(null,"shutdown-grace-ms","shutdown-grace-ms",-1671726656).cljs$core$IFn$_invoke$arity$1(config);
if(((typeof v === 'number') && ((v > (0))))){
return v;
} else {
return (25000);
}
})();
var poll_ms = (function (){var v = new cljs.core.Keyword(null,"shutdown-poll-ms","shutdown-poll-ms",1512160015).cljs$core$IFn$_invoke$arity$1(config);
if(((typeof v === 'number') && ((v > (0))))){
return v;
} else {
return (250);
}
})();
var deadline = (Date.now() + grace_ms);
return (new Promise((function (resolve){
var poll = (function knoxx$backend$agent_resume$wait_for_turns_and_flush_BANG__$_poll(){
var remaining = knoxx.backend.turn_control.active_turn_count();
if((remaining === (0))){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2(app,(""+"[agent-resume] turns drained; waiting "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agent_resume.POST_DRAIN_GRACE_MS)+"ms for Redis flush"));

return setTimeout((function (){
var G__54038 = ({"timed_out": false, "remaining": (0)});
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__54038) : resolve.call(null,G__54038));
}),knoxx.backend.agent_resume.POST_DRAIN_GRACE_MS);
} else {
if((Date.now() >= deadline)){
var G__54039 = ({"timed_out": true, "remaining": remaining});
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__54039) : resolve.call(null,G__54039));
} else {
return setTimeout(knoxx$backend$agent_resume$wait_for_turns_and_flush_BANG__$_poll,poll_ms);

}
}
});
var initial = knoxx.backend.turn_control.active_turn_count();
if((initial > (0))){
knoxx.backend.agent_resume.log_info_BANG_.cljs$core$IFn$_invoke$arity$2(app,(""+"[agent-resume] waiting for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(initial)+" active turn(s)"));
} else {
}

return poll();
})));
});

//# sourceMappingURL=knoxx.backend.agent_resume.js.map
