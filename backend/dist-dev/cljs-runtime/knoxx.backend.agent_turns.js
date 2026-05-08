import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.agents.content.js";
import "./knoxx.backend.agents.policy.js";
import "./knoxx.backend.agents.recovery.js";
import "./knoxx.backend.agents.tools.js";
import "./knoxx.backend.agents.transcript.js";
import "./knoxx.backend.agents.turn.js";
goog.provide('knoxx.backend.agent_turns');
knoxx.backend.agent_turns.lounge_messages_STAR_ = knoxx.backend.agents.turn.lounge_messages_STAR_;
knoxx.backend.agent_turns.validate_chat_policy_BANG_ = (function knoxx$backend$agent_turns$validate_chat_policy_BANG_(auth_context,model_id){
return knoxx.backend.agents.policy.validate_chat_policy_BANG_(auth_context,model_id);
});
knoxx.backend.agent_turns.ensure_session_id = (function knoxx$backend$agent_turns$ensure_session_id(node_crypto,session_id){
return knoxx.backend.agents.turn.ensure_session_id(node_crypto,session_id);
});
knoxx.backend.agent_turns.send_agent_turn_BANG_ = (function knoxx$backend$agent_turns$send_agent_turn_BANG_(runtime,config,params){
return knoxx.backend.agents.turn.send_agent_turn_BANG_(runtime,config,params);
});
knoxx.backend.agent_turns.ensure_conversation_access_BANG_ = (function knoxx$backend$agent_turns$ensure_conversation_access_BANG_(ctx,conversation_id){
return knoxx.backend.agents.turn.ensure_conversation_access_BANG_(ctx,conversation_id);
});
knoxx.backend.agent_turns.remember_conversation_access_BANG_ = (function knoxx$backend$agent_turns$remember_conversation_access_BANG_(ctx,conversation_id){
return knoxx.backend.agents.turn.remember_conversation_access_BANG_(ctx,conversation_id);
});
knoxx.backend.agent_turns.session__GT_stored_messages = (function knoxx$backend$agent_turns$session__GT_stored_messages(session){
return knoxx.backend.agents.transcript.session__GT_stored_messages(session);
});
goog.exportSymbol('knoxx.backend.agent_turns.session__GT_stored_messages', knoxx.backend.agent_turns.session__GT_stored_messages);
knoxx.backend.agent_turns.model_ready_content_parts = (function knoxx$backend$agent_turns$model_ready_content_parts(config,model_id,content_parts){
return knoxx.backend.agents.content.model_ready_content_parts(config,model_id,content_parts);
});
goog.exportSymbol('knoxx.backend.agent_turns.model_ready_content_parts', knoxx.backend.agent_turns.model_ready_content_parts);
knoxx.backend.agent_turns.assistant_tool_call_previews = (function knoxx$backend$agent_turns$assistant_tool_call_previews(assistant_message){
return knoxx.backend.agents.tools.assistant_tool_call_previews(assistant_message);
});
goog.exportSymbol('knoxx.backend.agent_turns.assistant_tool_call_previews', knoxx.backend.agent_turns.assistant_tool_call_previews);
knoxx.backend.agent_turns.reply_attachment_content_parts = (function knoxx$backend$agent_turns$reply_attachment_content_parts(tool_receipts){
return knoxx.backend.agents.content.reply_attachment_content_parts(tool_receipts);
});
goog.exportSymbol('knoxx.backend.agent_turns.reply_attachment_content_parts', knoxx.backend.agent_turns.reply_attachment_content_parts);
knoxx.backend.agent_turns.merge_content_parts = (function knoxx$backend$agent_turns$merge_content_parts(var_args){
var args__5882__auto__ = [];
var len__5876__auto___66054 = arguments.length;
var i__5877__auto___66055 = (0);
while(true){
if((i__5877__auto___66055 < len__5876__auto___66054)){
args__5882__auto__.push((arguments[i__5877__auto___66055]));

var G__66056 = (i__5877__auto___66055 + (1));
i__5877__auto___66055 = G__66056;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return knoxx.backend.agent_turns.merge_content_parts.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});
goog.exportSymbol('knoxx.backend.agent_turns.merge_content_parts', knoxx.backend.agent_turns.merge_content_parts);

(knoxx.backend.agent_turns.merge_content_parts.cljs$core$IFn$_invoke$arity$variadic = (function (groups){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.content.merge_content_parts,groups);
}));

(knoxx.backend.agent_turns.merge_content_parts.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(knoxx.backend.agent_turns.merge_content_parts.cljs$lang$applyTo = (function (seq66048){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq66048));
}));

knoxx.backend.agent_turns.recovered_auth_context = (function knoxx$backend$agent_turns$recovered_auth_context(session){
return knoxx.backend.agents.recovery.recovered_auth_context(session);
});
knoxx.backend.agent_turns.recovered_agent_spec = (function knoxx$backend$agent_turns$recovered_agent_spec(session){
return knoxx.backend.agents.recovery.recovered_agent_spec(session);
});
knoxx.backend.agent_turns.restore_recovered_conversation_access_BANG_ = (function knoxx$backend$agent_turns$restore_recovered_conversation_access_BANG_(session){
return knoxx.backend.agents.recovery.restored_conversation_access_BANG_(session);
});
knoxx.backend.agent_turns.last_session_user_message = (function knoxx$backend$agent_turns$last_session_user_message(session){
return knoxx.backend.agents.recovery.last_session_user_message(session);
});
knoxx.backend.agent_turns.wait_for_recovered_turn_kickoff_BANG_ = (function knoxx$backend$agent_turns$wait_for_recovered_turn_kickoff_BANG_(conversation_id,launch_promise){
return knoxx.backend.agents.recovery.wait_for_recovered_turn_kickoff_BANG_(conversation_id,launch_promise);
});
knoxx.backend.agent_turns.resume_recovered_session_BANG_ = (function knoxx$backend$agent_turns$resume_recovered_session_BANG_(var_args){
var G__66053 = arguments.length;
switch (G__66053) {
case 3:
return knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,session){
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,session);
}));

(knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,session,opts){
return knoxx.backend.agents.recovery.resume_recovered_session_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,session,opts);
}));

(knoxx.backend.agent_turns.resume_recovered_session_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.agent_turns.recover_active_agent_sessions_BANG_ = (function knoxx$backend$agent_turns$recover_active_agent_sessions_BANG_(runtime,config,redis_client){
return knoxx.backend.agents.recovery.recover_active_agent_sessions_BANG_(runtime,config,redis_client);
});

//# sourceMappingURL=knoxx.backend.agent_turns.js.map
