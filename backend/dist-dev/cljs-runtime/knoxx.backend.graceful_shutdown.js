import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.agent_resume.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.events.runtime.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.turn_control.js";
goog.provide('knoxx.backend.graceful_shutdown');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.graceful_shutdown !== 'undefined') && (typeof knoxx.backend.graceful_shutdown.shutdown_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.graceful_shutdown.shutdown_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"installed?","installed?",-345006478),false,new cljs.core.Keyword(null,"in-progress?","in-progress?",-689790546),false,new cljs.core.Keyword(null,"promise","promise",1767129287),null,new cljs.core.Keyword(null,"signal","signal",-1984951589),null], null));
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.graceful_shutdown !== 'undefined') && (typeof knoxx.backend.graceful_shutdown.shutdown_target_STAR_ !== 'undefined')){
} else {
knoxx.backend.graceful_shutdown.shutdown_target_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"app","app",-560961707),null,new cljs.core.Keyword(null,"config","config",994861415),null], null));
}
knoxx.backend.graceful_shutdown.log_info_BANG_ = (function knoxx$backend$graceful_shutdown$log_info_BANG_(app,message){
var temp__5823__auto__ = (function (){var G__65971 = app;
if((G__65971 == null)){
return null;
} else {
return G__65971.log;
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var logger = temp__5823__auto__;
return logger.info(message);
} else {
return console.log(message);
}
});
knoxx.backend.graceful_shutdown.log_warn_BANG_ = (function knoxx$backend$graceful_shutdown$log_warn_BANG_(app,message){
var temp__5823__auto__ = (function (){var G__65972 = app;
if((G__65972 == null)){
return null;
} else {
return G__65972.log;
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var logger = temp__5823__auto__;
return logger.warn(message);
} else {
return console.warn(message);
}
});
knoxx.backend.graceful_shutdown.log_error_BANG_ = (function knoxx$backend$graceful_shutdown$log_error_BANG_(app,message,err){
var temp__5823__auto__ = (function (){var G__65973 = app;
if((G__65973 == null)){
return null;
} else {
return G__65973.log;
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var logger = temp__5823__auto__;
return logger.error(message,err);
} else {
return console.error(message,err);
}
});
knoxx.backend.graceful_shutdown.begin_shutdown_BANG_ = (function knoxx$backend$graceful_shutdown$begin_shutdown_BANG_(app,config,signal){
var temp__5823__auto__ = new cljs.core.Keyword(null,"promise","promise",1767129287).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.graceful_shutdown.shutdown_state_STAR_));
if(cljs.core.truth_(temp__5823__auto__)){
var existing = temp__5823__auto__;
return existing;
} else {
var signal__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = signal;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "shutdown";
}
})()));
var close_server_BANG_ = (function (){
try{var result = app.close();
if((!((result == null)))){
return result;
} else {
return Promise.resolve(true);
}
}catch (e65974){var err = e65974;
knoxx.backend.graceful_shutdown.log_error_BANG_(app,"[shutdown] failed to close Fastify cleanly",err);

return Promise.resolve(false);
}});
var shutdown_promise = Promise.resolve(null).then((function (_){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.graceful_shutdown.shutdown_state_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"in-progress?","in-progress?",-689790546),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"signal","signal",-1984951589),signal__$1], 0));

knoxx.backend.graceful_shutdown.log_info_BANG_(app,(""+"[shutdown] received "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(signal__$1)+"; draining Knoxx"));

knoxx.backend.agent_resume.stop_periodic_recovery_BANG_();

knoxx.backend.events.runtime.stop_BANG_();

knoxx.backend.discord_gateway.stop_BANG_();

return knoxx.backend.realtime.stop_BANG_();
})).then((function (_){
return Promise.all([close_server_BANG_(),knoxx.backend.agent_resume.wait_for_turns_and_flush_BANG_(app,config)]);
})).then((function (parts){
var drain_result = (parts[(1)]);
if(cljs.core.truth_((drain_result["timed_out"]))){
var active_turns = knoxx.backend.turn_control.active_turn_entries();
return knoxx.backend.agent_resume.mark_sessions_resumable_BANG_(knoxx.backend.redis_client.get_client(),active_turns,signal__$1).then((function (count){
knoxx.backend.graceful_shutdown.log_warn_BANG_(app,(""+"[shutdown] marked "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(count)+" active session(s) resumable for restart"));

return ({"count": count});
}));
} else {
return Promise.resolve(({"count": (0)}));
}
})).then((function (_){
return Promise.all(cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var temp__5825__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto__)){
var client = temp__5825__auto__;
return knoxx.backend.redis_client.quit(client);
} else {
return null;
}
})(),(function (){var temp__5825__auto__ = knoxx.backend.runtime.state.current_policy_db();
if(cljs.core.truth_(temp__5825__auto__)){
var policy_db = temp__5825__auto__;
var temp__5825__auto____$1 = (policy_db["close"]);
if(cljs.core.truth_(temp__5825__auto____$1)){
var close_fn = temp__5825__auto____$1;
return (close_fn.cljs$core$IFn$_invoke$arity$0 ? close_fn.cljs$core$IFn$_invoke$arity$0() : close_fn.call(null));
} else {
return null;
}
} else {
return null;
}
})()], null)));
})).then((function (_){
knoxx.backend.graceful_shutdown.log_info_BANG_(app,"[shutdown] graceful shutdown complete");

return process.exit((0));
})).catch((function (err){
knoxx.backend.graceful_shutdown.log_error_BANG_(app,"[shutdown] graceful shutdown failed",err);

return process.exit((1));
}));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.graceful_shutdown.shutdown_state_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"promise","promise",1767129287),shutdown_promise);

return shutdown_promise;
}
});
knoxx.backend.graceful_shutdown.begin_current_shutdown_BANG_ = (function knoxx$backend$graceful_shutdown$begin_current_shutdown_BANG_(signal){
var map__65975 = cljs.core.deref(knoxx.backend.graceful_shutdown.shutdown_target_STAR_);
var map__65975__$1 = cljs.core.__destructure_map(map__65975);
var app = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65975__$1,new cljs.core.Keyword(null,"app","app",-560961707));
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65975__$1,new cljs.core.Keyword(null,"config","config",994861415));
if(cljs.core.truth_(app)){
return knoxx.backend.graceful_shutdown.begin_shutdown_BANG_(app,config,signal);
} else {
console.warn("[shutdown] no active HTTP app; exiting");

return process.exit((0));
}
});
knoxx.backend.graceful_shutdown.install_BANG_ = (function knoxx$backend$graceful_shutdown$install_BANG_(app,config){
cljs.core.reset_BANG_(knoxx.backend.graceful_shutdown.shutdown_target_STAR_,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"app","app",-560961707),app,new cljs.core.Keyword(null,"config","config",994861415),config], null));

if(cljs.core.truth_(new cljs.core.Keyword(null,"installed?","installed?",-345006478).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.graceful_shutdown.shutdown_state_STAR_)))){
return null;
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.graceful_shutdown.shutdown_state_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"installed?","installed?",-345006478),true);

process.on("SIGINT",(function (){
return knoxx.backend.graceful_shutdown.begin_current_shutdown_BANG_("SIGINT");
}));

process.on("SIGTERM",(function (){
return knoxx.backend.graceful_shutdown.begin_current_shutdown_BANG_("SIGTERM");
}));

process.on("message",(function (message){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message)),"shutdown")){
return knoxx.backend.graceful_shutdown.begin_current_shutdown_BANG_("pm2:shutdown");
} else {
return null;
}
}));

return true;
}
});

//# sourceMappingURL=knoxx.backend.graceful_shutdown.js.map
