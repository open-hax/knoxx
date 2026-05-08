import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agents.turn.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.turn_control.js";
import "./knoxx.backend.util.time.js";
goog.provide('knoxx.backend.agents.recovery');
knoxx.backend.agents.recovery.RECOVERED_SESSION_KICKOFF_TIMEOUT_MS = (5000);
knoxx.backend.agents.recovery.RECOVERED_SESSION_KICKOFF_POLL_MS = (25);
knoxx.backend.agents.recovery.recovered_auth_context = (function knoxx$backend$agents$recovery$recovered_auth_context(session){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"isSystemAdmin","isSystemAdmin",679314438),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"membershipToolPolicies","membershipToolPolicies",-954353456),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"membershipId","membershipId",2026001076),new cljs.core.Keyword(null,"userId","userId",575594135),new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618),new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270)],[new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(session),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"permissions","permissions",67803075).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"org_id","org_id",1380185385).cljs$core$IFn$_invoke$arity$1(session),cljs.core.boolean$(new cljs.core.Keyword(null,"is_system_admin","is_system_admin",-723489128).cljs$core$IFn$_invoke$arity$1(session)),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770).cljs$core$IFn$_invoke$arity$1(session),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"membership_tool_policies","membership_tool_policies",2116037883).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"membership_id","membership_id",-171302674).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"user_id","user_id",993497112).cljs$core$IFn$_invoke$arity$1(session),new cljs.core.Keyword(null,"user_email","user_email",-926613652).cljs$core$IFn$_invoke$arity$1(session),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role_slugs","role_slugs",2101192325).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())]);
});
knoxx.backend.agents.recovery.recovered_agent_spec = (function knoxx$backend$agents$recovery$recovered_agent_spec(session){
return new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365).cljs$core$IFn$_invoke$arity$1(session);
});
knoxx.backend.agents.recovery.restored_conversation_access_BANG_ = (function knoxx$backend$agents$recovery$restored_conversation_access_BANG_(session){
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var snapshot = cljs.core.select_keys(session,new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770),new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"user_email","user_email",-926613652),new cljs.core.Keyword(null,"membership_id","membership_id",-171302674),new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"role_slugs","role_slugs",2101192325),new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177),new cljs.core.Keyword(null,"membership_tool_policies","membership_tool_policies",2116037883),new cljs.core.Keyword(null,"is_system_admin","is_system_admin",-723489128)], null));
if((((!(clojure.string.blank_QMARK_(conversation_id)))) && (knoxx.backend.authz.auth_snapshot_has_principal_QMARK_(snapshot)))){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.agents.turn.conversation_access_STAR_,cljs.core.assoc,conversation_id,snapshot);
} else {
return null;
}
});
knoxx.backend.agents.recovery.last_session_user_message = (function knoxx$backend$agents$recovery$last_session_user_message(session){
return cljs.core.some((function (message){
var role = (function (){var G__66036 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(message);
var G__66036__$1 = (((G__66036 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66036)));
if((G__66036__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__66036__$1);
}
})();
var content = (function (){var G__66037 = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if((G__66037 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__66037));
}
})();
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"user")) && ((!(clojure.string.blank_QMARK_(content)))))){
return content;
} else {
return null;
}
}),cljs.core.reverse(cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.agents.recovery.wait_for_recovered_turn_kickoff_BANG_ = (function knoxx$backend$agents$recovery$wait_for_recovered_turn_kickoff_BANG_(conversation_id,launch_promise){
if((!((knoxx.backend.turn_control.active_turn(conversation_id) == null)))){
return Promise.resolve(true);
} else {
return (new Promise((function (resolve,reject){
var done_QMARK_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false);
var started_ms = Date.now();
var check_BANG_ = (function knoxx$backend$agents$recovery$wait_for_recovered_turn_kickoff_BANG__$_check_BANG_(){
if(cljs.core.truth_(cljs.core.deref(done_QMARK_))){
return null;
} else {
if((!((knoxx.backend.turn_control.active_turn(conversation_id) == null)))){
cljs.core.reset_BANG_(done_QMARK_,true);

return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(true) : resolve.call(null,true));
} else {
if(((Date.now() - started_ms) > knoxx.backend.agents.recovery.RECOVERED_SESSION_KICKOFF_TIMEOUT_MS)){
cljs.core.reset_BANG_(done_QMARK_,true);

var G__66038 = (new Error((""+"Timed out waiting for recovered session kickoff: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id))));
return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(G__66038) : reject.call(null,G__66038));
} else {
return setTimeout(knoxx$backend$agents$recovery$wait_for_recovered_turn_kickoff_BANG__$_check_BANG_,knoxx.backend.agents.recovery.RECOVERED_SESSION_KICKOFF_POLL_MS);

}
}
}
});
launch_promise.catch((function (err){
if(cljs.core.truth_(cljs.core.deref(done_QMARK_))){
return null;
} else {
cljs.core.reset_BANG_(done_QMARK_,true);

return (reject.cljs$core$IFn$_invoke$arity$1 ? reject.cljs$core$IFn$_invoke$arity$1(err) : reject.call(null,err));
}
}));

return check_BANG_();
})));
}
});
knoxx.backend.agents.recovery.resume_recovered_session_BANG_ = (function knoxx$backend$agents$recovery$resume_recovered_session_BANG_(var_args){
var G__66040 = arguments.length;
switch (G__66040) {
case 3:
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,session){
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,session,null);
}));

(knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,session,opts){
var conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var run_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})();
var model_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})();
var mode = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "direct";
}
})();
var wait_for = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"wait-for","wait-for",603509654).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"completion","completion",-731716930);
}
})();
var thinking_level = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})();
var auth_context = knoxx.backend.agents.recovery.recovered_auth_context(session);
var agent_spec = knoxx.backend.agents.recovery.recovered_agent_spec(session);
var message = knoxx.backend.agents.recovery.last_session_user_message(session);
var resume_failed_BANG_ = (function (err){
console.error("[knoxx] failed to resume recovered session",({"sessionId": session_id, "conversationId": conversation_id, "error": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))}));

return knoxx.backend.session_store.complete_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,conversation_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"error","error",-978969032),(""+"Session recovery failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"messages","messages",345434482),new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session)], null)).then((function (_){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
});
knoxx.backend.agents.recovery.restored_conversation_access_BANG_(session);

if(((clojure.string.blank_QMARK_(conversation_id)) || (clojure.string.blank_QMARK_(session_id)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),false,new cljs.core.Keyword(null,"reason","reason",-2070751759),"missing session or conversation id"], null));
} else {
if(clojure.string.blank_QMARK_(message)){
return knoxx.backend.agent_runtime.ensure_agent_session_BANG_.cljs$core$IFn$_invoke$arity$8(runtime,config,conversation_id,model_id,auth_context,thinking_level,session_id,agent_spec).then((function (_){
return knoxx.backend.session_store.update_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"waiting_input",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"recovered_at","recovered_at",-474781568),knoxx.backend.util.time.now_iso()], null)).then((function (___$1){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),false,new cljs.core.Keyword(null,"reason","reason",-2070751759),"no pending user message to resume"], null);
}));
}));
} else {
return knoxx.backend.session_store.update_session_BANG_(knoxx.backend.redis_client.get_client(),session_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"recovered_at","recovered_at",-474781568),knoxx.backend.util.time.now_iso()], null)).then((function (_){
var send_promise = knoxx.backend.agents.turn.send_agent_turn_BANG_(runtime,config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,auth_context,mode,thinking_level,agent_spec,run_id,message,model_id]));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wait_for,new cljs.core.Keyword(null,"kickoff","kickoff",-1736115645))){
send_promise.catch((function (err){
console.error("[knoxx] recovered session failed after kickoff",({"sessionId": session_id, "conversationId": conversation_id, "error": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))}));

return null;
}));

return knoxx.backend.agents.recovery.wait_for_recovered_turn_kickoff_BANG_(conversation_id,send_promise).then((function (___$1){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),true,new cljs.core.Keyword(null,"wait_for","wait_for",-1748516157),"kickoff"], null);
})).catch(resume_failed_BANG_);
} else {
return send_promise.then((function (___$1){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"resumed","resumed",897761340),true], null);
})).catch(resume_failed_BANG_);
}
})).catch(resume_failed_BANG_);

}
}
}));

(knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.agents.recovery.recover_active_agent_sessions_BANG_ = (function knoxx$backend$agents$recovery$recover_active_agent_sessions_BANG_(runtime,config,redis_client){
return knoxx.backend.session_store.recover_sessions_BANG_(redis_client).then((function (sessions){
var items = cljs.core.vec(sessions);
if(cljs.core.seq(items)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__66044_SHARP_){
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,p1__66044_SHARP_);
}),items))).then((function (results){
return cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
}));
} else {
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}
}));
});

//# sourceMappingURL=knoxx.backend.agents.recovery.js.map
