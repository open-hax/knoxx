import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agents.content.js";
import "./knoxx.backend.agents.tools.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.turn_control.js";
import "./knoxx.backend.util.time.js";
goog.provide('knoxx.backend.agents.stream');
knoxx.backend.agents.stream.DEATH_SPIRAL_STREAK_LIMIT = (6);
knoxx.backend.agents.stream.DEATH_SPIRAL_TOTAL_LIMIT = (12);
knoxx.backend.agents.stream.make_stream_state = (function knoxx$backend$agents$stream$make_stream_state(run_id,conversation_id,session_id,started_at,started_ms,node_crypto){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"tool-loop*","tool-loop*",1394428066),new cljs.core.Keyword(null,"think-tag-mode*","think-tag-mode*",1825166790),new cljs.core.Keyword(null,"abort-reason*","abort-reason*",-962330650),new cljs.core.Keyword(null,"node-crypto","node-crypto",1686415591),new cljs.core.Keyword(null,"started-at","started-at",1318767912),new cljs.core.Keyword(null,"started-ms","started-ms",1106122505),new cljs.core.Keyword(null,"seen-tool-lifecycle-events*","seen-tool-lifecycle-events*",83846058),new cljs.core.Keyword(null,"chunks","chunks",83720431),new cljs.core.Keyword(null,"replay-suppression*","replay-suppression*",192344501),new cljs.core.Keyword(null,"reasoning-chunks","reasoning-chunks",-526618091),new cljs.core.Keyword(null,"aborting?","aborting?",690860697),new cljs.core.Keyword(null,"ttft-recorded?","ttft-recorded?",-1916399622),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770),new cljs.core.Keyword(null,"last-reasoning-text*","last-reasoning-text*",-1619356993)],[conversation_id,session_id,cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"last","last",1105735132),null,new cljs.core.Keyword(null,"streak","streak",1229213332),(0),new cljs.core.Keyword(null,"counts","counts",234305892),cljs.core.PersistentArrayMap.EMPTY], null)),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"off","off",606440789)),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null),node_crypto,started_at,started_ms,cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentHashSet.EMPTY),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false),cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false),run_id,cljs.core.atom.cljs$core$IFn$_invoke$arity$1(""),cljs.core.atom.cljs$core$IFn$_invoke$arity$1("")]);
});
/**
 * Emit appended delta for a *cumulative* text value.
 * 
 * Some providers misuse `*_delta` to carry the full message-so-far (cumulative)
 * instead of an incremental token. If we treat that as an incremental delta we
 * get duplicated leading tokens. This helper diffs against our last seen text,
 * emits only the appended portion, and then resets our last-text atom to the
 * provided cumulative value.
 */
knoxx.backend.agents.stream.emit_progress_text_BANG_ = (function knoxx$backend$agents$stream$emit_progress_text_BANG_(state,kind,full_text){
var full_text__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = full_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var last_STAR_ = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201)))?new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770).cljs$core$IFn$_invoke$arity$1(state):new cljs.core.Keyword(null,"last-reasoning-text*","last-reasoning-text*",-1619356993).cljs$core$IFn$_invoke$arity$1(state));
var delta = knoxx.backend.agents.content.diff_appended_text(cljs.core.deref(last_STAR_),full_text__$1);
if(cljs.core.seq(delta)){
(knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,kind,delta) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,kind,delta));
} else {
}

return cljs.core.reset_BANG_(last_STAR_,full_text__$1);
});
/**
 * Routes text deltas that contain <think>...</think> blocks into the reasoning
 * stream, leaving the assistant message stream clean.
 * 
 * This is a pragmatic fix for Gemma-family models that sometimes emit thinking
 * traces inline even when the provider does not supply structured reasoning
 * events.
 */
knoxx.backend.agents.stream.emit_text_delta_with_think_tags_BANG_ = (function knoxx$backend$agents$stream$emit_text_delta_with_think_tags_BANG_(state,delta){
var delta__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = delta;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var mode_STAR_ = new cljs.core.Keyword(null,"think-tag-mode*","think-tag-mode*",1825166790).cljs$core$IFn$_invoke$arity$1(state);
var last_text = cljs.core.deref(new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770).cljs$core$IFn$_invoke$arity$1(state));
if(clojure.string.blank_QMARK_(delta__$1)){
return null;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(mode_STAR_),new cljs.core.Keyword(null,"off","off",606440789))){
var idx = delta__$1.indexOf("<think>");
if((((idx >= (0))) && (((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(last_text)))) && ((idx < (64))))))){
cljs.core.reset_BANG_(mode_STAR_,new cljs.core.Keyword(null,"thinking","thinking",2063777387));

var before = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(delta__$1,(0),idx);
var after = cljs.core.subs.cljs$core$IFn$_invoke$arity$2(delta__$1,(idx + (("<think>").length)));
if(cljs.core.seq(clojure.string.trim(before))){
(knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),before) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),before));
} else {
}

if(cljs.core.seq(after)){
return (knoxx.backend.agents.stream.emit_text_delta_with_think_tags_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.agents.stream.emit_text_delta_with_think_tags_BANG_.cljs$core$IFn$_invoke$arity$2(state,after) : knoxx.backend.agents.stream.emit_text_delta_with_think_tags_BANG_.call(null,state,after));
} else {
return null;
}
} else {
return (knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),delta__$1) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),delta__$1));
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(mode_STAR_),new cljs.core.Keyword(null,"thinking","thinking",2063777387))){
var idx = delta__$1.indexOf("</think>");
if((idx >= (0))){
var thinking = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(delta__$1,(0),idx);
var after = cljs.core.subs.cljs$core$IFn$_invoke$arity$2(delta__$1,(idx + (("</think>").length)));
if(cljs.core.seq(thinking)){
(knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),thinking) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),thinking));
} else {
}

cljs.core.reset_BANG_(mode_STAR_,new cljs.core.Keyword(null,"done","done",-889844188));

if(cljs.core.seq(after)){
return (knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),after) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),after));
} else {
return null;
}
} else {
return (knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),delta__$1) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),delta__$1));
}
} else {
return (knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.agents.stream.emit_streaming_delta_BANG_.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),delta__$1) : knoxx.backend.agents.stream.emit_streaming_delta_BANG_.call(null,state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),delta__$1));

}
}
}
});
knoxx.backend.agents.stream.first_lifecycle_event_QMARK_ = (function knoxx$backend$agents$stream$first_lifecycle_event_QMARK_(state,type,tool_call_id){
if((!(((typeof tool_call_id === 'string') && (cljs.core.seq(tool_call_id)))))){
return true;
} else {
var event_key = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(type)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_call_id));
var seen_QMARK_ = cljs.core.contains_QMARK_(cljs.core.deref(new cljs.core.Keyword(null,"seen-tool-lifecycle-events*","seen-tool-lifecycle-events*",83846058).cljs$core$IFn$_invoke$arity$1(state)),event_key);
if(seen_QMARK_){
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"seen-tool-lifecycle-events*","seen-tool-lifecycle-events*",83846058).cljs$core$IFn$_invoke$arity$1(state),cljs.core.conj,event_key);
}

return (!(seen_QMARK_));
}
});
knoxx.backend.agents.stream.suppress_replayed_prefix_delta_BANG_ = (function knoxx$backend$agents$stream$suppress_replayed_prefix_delta_BANG_(p__39065,kind,delta){
var map__39066 = p__39065;
var map__39066__$1 = cljs.core.__destructure_map(map__39066);
var last_assistant_text_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39066__$1,new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770));
var last_reasoning_text_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39066__$1,new cljs.core.Keyword(null,"last-reasoning-text*","last-reasoning-text*",-1619356993));
var replay_suppression_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39066__$1,new cljs.core.Keyword(null,"replay-suppression*","replay-suppression*",192344501));
var previous = cljs.core.deref(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201)))?last_assistant_text_STAR_:last_reasoning_text_STAR_));
var offset = cljs.core.long$(cljs.core.get.cljs$core$IFn$_invoke$arity$3(cljs.core.deref(replay_suppression_STAR_),kind,(0)));
var delta_len = cljs.core.count(delta);
var previous_len = cljs.core.count(previous);
var prior_has_boundary_QMARK_ = cljs.core.boolean$(cljs.core.re_find(/[\s\W_]/,previous));
var advance_BANG_ = (function (next_offset){
if((next_offset < previous_len)){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(replay_suppression_STAR_,cljs.core.assoc,kind,next_offset);
} else {
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(replay_suppression_STAR_,cljs.core.dissoc,kind);
}
});
if(clojure.string.blank_QMARK_(delta)){
return "";
} else {
if((offset > (0))){
var expected = previous.slice(offset,cljs.core.min.cljs$core$IFn$_invoke$arity$2(previous_len,(offset + delta_len)));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(delta,expected)){
advance_BANG_((offset + delta_len));

return "";
} else {
if(clojure.string.starts_with_QMARK_(delta,expected)){
advance_BANG_(previous_len);

return delta.slice(cljs.core.count(expected));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(replay_suppression_STAR_,cljs.core.dissoc,kind);

return delta;

}
}
} else {
if(((prior_has_boundary_QMARK_) && ((((!(cljs.core.boolean$(cljs.core.re_find(/\s$/,previous))))) && (((clojure.string.starts_with_QMARK_(previous,delta)) && ((delta_len < previous_len)))))))){
advance_BANG_(delta_len);

return "";
} else {
return delta;

}
}
}
});
knoxx.backend.agents.stream.emit_streaming_delta_BANG_ = (function knoxx$backend$agents$stream$emit_streaming_delta_BANG_(p__39069,kind,delta){
var map__39070 = p__39069;
var map__39070__$1 = cljs.core.__destructure_map(map__39070);
var state = map__39070__$1;
var ttft_recorded_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"ttft-recorded?","ttft-recorded?",-1916399622));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var last_assistant_text_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770));
var last_reasoning_text_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"last-reasoning-text*","last-reasoning-text*",-1619356993));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var started_ms = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"started-ms","started-ms",1106122505));
var chunks = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"chunks","chunks",83720431));
var reasoning_chunks = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39070__$1,new cljs.core.Keyword(null,"reasoning-chunks","reasoning-chunks",-526618091));
var delta__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = delta;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var delta__$2 = knoxx.backend.agents.stream.suppress_replayed_prefix_delta_BANG_(state,kind,delta__$1);
var last_STAR_ = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201)))?last_assistant_text_STAR_:last_reasoning_text_STAR_);
var delta__$3 = knoxx.backend.agents.content.diff_appended_text(cljs.core.deref(last_STAR_),delta__$2);
if(cljs.core.seq(delta__$3)){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201))) && (cljs.core.not(cljs.core.deref(ttft_recorded_QMARK_))))){
cljs.core.reset_BANG_(ttft_recorded_QMARK_,true);

var ttft_ms_39277 = (Date.now() - started_ms);
var ttft_event_39278 = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,"assistant_first_token",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),ttft_ms_39277], null));
knoxx.backend.run_state.update_run_BANG_(run_id,(function (p1__39068_SHARP_){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__39068_SHARP_,new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),ttft_ms_39277);
}));

knoxx.backend.run_state.append_run_event_BANG_(run_id,ttft_event_39278);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",ttft_event_39278);

knoxx.backend.session_store.mark_session_streaming_BANG_(knoxx.backend.redis_client.get_client(),session_id,true);
} else {
}

if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(chunks,cljs.core.conj,delta__$3);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(last_assistant_text_STAR_,cljs.core.str,delta__$3);
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(reasoning_chunks,cljs.core.conj,delta__$3);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(last_reasoning_text_STAR_,cljs.core.str,delta__$3);
}

knoxx.backend.run_state.append_run_trace_text_BANG_(run_id,kind,delta__$3,knoxx.backend.util.time.now_iso());

return knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"tokens",new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"kind","kind",-717265803),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201)))?"assistant_message":"reasoning"),new cljs.core.Keyword(null,"token","token",-1211463215),delta__$3], null));
} else {
return null;
}
});
knoxx.backend.agents.stream.sync_assistant_message_BANG_ = (function knoxx$backend$agents$stream$sync_assistant_message_BANG_(state,assistant_message){
if(cljs.core.truth_((function (){var and__5140__auto__ = assistant_message;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((assistant_message["role"]),"assistant");
} else {
return and__5140__auto__;
}
})())){
var full_text = knoxx.backend.text.assistant_message_text(assistant_message);
var full_reasoning = knoxx.backend.text.assistant_message_reasoning_text(assistant_message);
var tool_previews = knoxx.backend.agents.tools.assistant_tool_call_previews(assistant_message);
var seq__39072_39279 = cljs.core.seq(tool_previews);
var chunk__39073_39280 = null;
var count__39074_39281 = (0);
var i__39075_39282 = (0);
while(true){
if((i__39075_39282 < count__39074_39281)){
var map__39078_39283 = chunk__39073_39280.cljs$core$IIndexed$_nth$arity$2(null,i__39075_39282);
var map__39078_39284__$1 = cljs.core.__destructure_map(map__39078_39283);
var tool_call_id_39285 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39078_39284__$1,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517));
var tool_name_39286 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39078_39284__$1,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484));
var input_preview_39287 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39078_39284__$1,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734));
knoxx.backend.run_state.backfill_run_tool_input_preview_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_call_id_39285,tool_name_39286,input_preview_39287);


var G__39288 = seq__39072_39279;
var G__39289 = chunk__39073_39280;
var G__39290 = count__39074_39281;
var G__39291 = (i__39075_39282 + (1));
seq__39072_39279 = G__39288;
chunk__39073_39280 = G__39289;
count__39074_39281 = G__39290;
i__39075_39282 = G__39291;
continue;
} else {
var temp__5825__auto___39292 = cljs.core.seq(seq__39072_39279);
if(temp__5825__auto___39292){
var seq__39072_39293__$1 = temp__5825__auto___39292;
if(cljs.core.chunked_seq_QMARK_(seq__39072_39293__$1)){
var c__5673__auto___39294 = cljs.core.chunk_first(seq__39072_39293__$1);
var G__39295 = cljs.core.chunk_rest(seq__39072_39293__$1);
var G__39296 = c__5673__auto___39294;
var G__39297 = cljs.core.count(c__5673__auto___39294);
var G__39298 = (0);
seq__39072_39279 = G__39295;
chunk__39073_39280 = G__39296;
count__39074_39281 = G__39297;
i__39075_39282 = G__39298;
continue;
} else {
var map__39083_39299 = cljs.core.first(seq__39072_39293__$1);
var map__39083_39300__$1 = cljs.core.__destructure_map(map__39083_39299);
var tool_call_id_39301 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39083_39300__$1,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517));
var tool_name_39302 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39083_39300__$1,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484));
var input_preview_39303 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39083_39300__$1,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734));
knoxx.backend.run_state.backfill_run_tool_input_preview_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_call_id_39301,tool_name_39302,input_preview_39303);


var G__39304 = cljs.core.next(seq__39072_39293__$1);
var G__39305 = null;
var G__39306 = (0);
var G__39307 = (0);
seq__39072_39279 = G__39304;
chunk__39073_39280 = G__39305;
count__39074_39281 = G__39306;
i__39075_39282 = G__39307;
continue;
}
} else {
}
}
break;
}

knoxx.backend.agents.stream.emit_progress_text_BANG_(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),full_text);

return knoxx.backend.agents.stream.emit_progress_text_BANG_(state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),full_reasoning);
} else {
return null;
}
});
knoxx.backend.agents.stream.request_abort_BANG_ = (function knoxx$backend$agents$stream$request_abort_BANG_(p__39087,session,reason){
var map__39088 = p__39087;
var map__39088__$1 = cljs.core.__destructure_map(map__39088);
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39088__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39088__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39088__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var aborting_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39088__$1,new cljs.core.Keyword(null,"aborting?","aborting?",690860697));
var abort_reason_STAR_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39088__$1,new cljs.core.Keyword(null,"abort-reason*","abort-reason*",-962330650));
var reason__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = reason;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "aborted";
}
})()));
if(cljs.core.truth_(cljs.core.deref(aborting_QMARK_))){
return Promise.resolve(null);
} else {
cljs.core.reset_BANG_(aborting_QMARK_,true);

cljs.core.reset_BANG_(abort_reason_STAR_,reason__$1);

knoxx.backend.session_store.mark_session_streaming_BANG_(knoxx.backend.redis_client.get_client(),session_id,false);

var abort_event_39308 = knoxx.backend.run_state.tool_event_payload(run_id,conversation_id,session_id,"abort_requested",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),"aborting",new cljs.core.Keyword(null,"reason","reason",-2070751759),reason__$1], null));
knoxx.backend.run_state.append_run_event_BANG_(run_id,abort_event_39308);

knoxx.backend.realtime.broadcast_ws_session_BANG_(session_id,"events",abort_event_39308);

return session.abort();
}
});
knoxx.backend.agents.stream.register_active_turn_BANG_ = (function knoxx$backend$agents$stream$register_active_turn_BANG_(var_args){
var G__39093 = arguments.length;
switch (G__39093) {
case 2:
return knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (state,abort_BANG_){
return knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$3(state,abort_BANG_,null);
}));

(knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (p__39123,abort_BANG_,agent_spec){
var map__39124 = p__39123;
var map__39124__$1 = cljs.core.__destructure_map(map__39124);
var state = map__39124__$1;
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39124__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39124__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var started_at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39124__$1,new cljs.core.Keyword(null,"started-at","started-at",1318767912));
return knoxx.backend.turn_control.register_active_turn_BANG_(conversation_id,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"started_at","started_at",856896776),started_at,new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),agent_spec,new cljs.core.Keyword(null,"abort!","abort!",-220883953),abort_BANG_], null));
}));

(knoxx.backend.agents.stream.register_active_turn_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.agents.stream.handle_message_update_BANG_ = (function knoxx$backend$agents$stream$handle_message_update_BANG_(state,event){
var assistant_event = (event["assistantMessageEvent"]);
var assistant_event_type = (assistant_event["type"]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(assistant_event_type,"text_delta")){
var delta = (function (){var or__5142__auto__ = (assistant_event["delta"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var delta__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = delta;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var last_text = cljs.core.deref(new cljs.core.Keyword(null,"last-assistant-text*","last-assistant-text*",-872016770).cljs$core$IFn$_invoke$arity$1(state));
if((((!(clojure.string.blank_QMARK_(last_text)))) && (clojure.string.starts_with_QMARK_(delta__$1,last_text)))){
return knoxx.backend.agents.stream.emit_progress_text_BANG_(state,new cljs.core.Keyword(null,"agent_message","agent_message",-522809201),delta__$1);
} else {
return knoxx.backend.agents.stream.emit_text_delta_with_think_tags_BANG_(state,delta__$1);
}
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["reasoning",null,"thinking_delta",null,"reasoning_content_delta",null,"reasoning_delta",null,"thinking",null], null), null),assistant_event_type)){
var delta = (function (){var or__5142__auto__ = (assistant_event["delta"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (assistant_event["text"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (assistant_event["reasoning"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (assistant_event["thinking"]);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return "";
}
}
}
}
})();
var delta__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = delta;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var last_reasoning = cljs.core.deref(new cljs.core.Keyword(null,"last-reasoning-text*","last-reasoning-text*",-1619356993).cljs$core$IFn$_invoke$arity$1(state));
if((((!(clojure.string.blank_QMARK_(last_reasoning)))) && (clojure.string.starts_with_QMARK_(delta__$1,last_reasoning)))){
return knoxx.backend.agents.stream.emit_progress_text_BANG_(state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),delta__$1);
} else {
return knoxx.backend.agents.stream.emit_streaming_delta_BANG_(state,new cljs.core.Keyword(null,"reasoning","reasoning",1956143595),delta__$1);
}
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["toolcall_delta",null,"tool_call_delta",null], null), null),assistant_event_type)){
return knoxx.backend.agents.stream.sync_assistant_message_BANG_(state,(function (){var or__5142__auto__ = (assistant_event["partial"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (event["message"]);
}
})());
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["tool_call_end",null,"toolcall_end",null], null), null),assistant_event_type)){
var temp__5825__auto___39310 = knoxx.backend.agents.tools.tool_call_preview_from_part((assistant_event["toolCall"]));
if(cljs.core.truth_(temp__5825__auto___39310)){
var preview_39311 = temp__5825__auto___39310;
knoxx.backend.run_state.backfill_run_tool_input_preview_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517).cljs$core$IFn$_invoke$arity$1(preview_39311),new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(preview_39311),new cljs.core.Keyword(null,"input_preview","input_preview",2048529734).cljs$core$IFn$_invoke$arity$1(preview_39311));
} else {
}

return knoxx.backend.agents.stream.sync_assistant_message_BANG_(state,(function (){var or__5142__auto__ = (assistant_event["partial"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (event["message"]);
}
})());
} else {
return knoxx.backend.agents.stream.sync_assistant_message_BANG_(state,(event["message"]));

}
}
}
}
});
knoxx.backend.agents.stream.handle_message_end_BANG_ = (function knoxx$backend$agents$stream$handle_message_end_BANG_(state,event){
return knoxx.backend.agents.stream.sync_assistant_message_BANG_(state,(event["message"]));
});
knoxx.backend.agents.stream.handle_tool_execution_start_BANG_ = (function knoxx$backend$agents$stream$handle_tool_execution_start_BANG_(state,_session,event){
var tool_name = (function (){var or__5142__auto__ = (event["toolName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "tool";
}
})();
var tool_call_id = (function (){var or__5142__auto__ = (event["toolCallId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"node-crypto","node-crypto",1686415591).cljs$core$IFn$_invoke$arity$1(state).randomUUID();
}
})();
var raw_args = (function (){var or__5142__auto__ = (event["params"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (event["toolArgs"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (event["args"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (event["arguments"]);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = (event["input"]);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return (event["parameters"]);
}
}
}
}
}
})();
var input_raw = (((raw_args == null))?null:((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(raw_args,undefined))?null:((typeof raw_args === 'string')?raw_args:(function (){try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(raw_args,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e39138){var _ = e39138;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_args));
}})()
)));
var input_preview = (function (){var or__5142__auto__ = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["params"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["toolArgs"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["args"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["arguments"]));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["input"]));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,(event["parameters"]));
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
return knoxx.backend.agents.tools.tool_call_input_preview(tool_name,raw_args);
}
}
}
}
}
}
})();
var signature = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name)+"::"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = input_preview;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var map__39132 = cljs.core.deref(new cljs.core.Keyword(null,"tool-loop*","tool-loop*",1394428066).cljs$core$IFn$_invoke$arity$1(state));
var map__39132__$1 = cljs.core.__destructure_map(map__39132);
var last = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39132__$1,new cljs.core.Keyword(null,"last","last",1105735132));
var streak = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39132__$1,new cljs.core.Keyword(null,"streak","streak",1229213332));
var counts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__39132__$1,new cljs.core.Keyword(null,"counts","counts",234305892));
var next_total = (cljs.core.get.cljs$core$IFn$_invoke$arity$3(counts,signature,(0)) + (1));
var next_counts = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(counts,signature,next_total);
var next_streak = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(signature,last))?(streak + (1)):(1));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(new cljs.core.Keyword(null,"tool-loop*","tool-loop*",1394428066).cljs$core$IFn$_invoke$arity$1(state),cljs.core.assoc,new cljs.core.Keyword(null,"last","last",1105735132),signature,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"streak","streak",1229213332),next_streak,new cljs.core.Keyword(null,"counts","counts",234305892),next_counts], 0));

if(((cljs.core.not(cljs.core.deref(new cljs.core.Keyword(null,"aborting?","aborting?",690860697).cljs$core$IFn$_invoke$arity$1(state)))) && ((((next_streak >= knoxx.backend.agents.stream.DEATH_SPIRAL_STREAK_LIMIT)) || ((next_total >= knoxx.backend.agents.stream.DEATH_SPIRAL_TOTAL_LIMIT)))))){
var reason_39313 = (""+"death_spiral_detected: tool '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name)+"' repeated "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(next_total)+"x (streak "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(next_streak)+")");
var spiral_event_39314 = knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"death_spiral_detected",new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"status","status",-1997798413),"failed",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"count","count",2139924085),next_total,new cljs.core.Keyword(null,"streak","streak",1229213332),next_streak], null));
knoxx.backend.run_state.append_run_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),spiral_event_39314);

knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",spiral_event_39314);

var fexpr__39148_39319 = new cljs.core.Keyword(null,"abort!","abort!",-220883953).cljs$core$IFn$_invoke$arity$1(state);
(fexpr__39148_39319.cljs$core$IFn$_invoke$arity$1 ? fexpr__39148_39319.cljs$core$IFn$_invoke$arity$1(reason_39313) : fexpr__39148_39319.call(null,reason_39313));
} else {
}

var first_event_QMARK_ = knoxx.backend.agents.stream.first_lifecycle_event_QMARK_(state,"tool_start",tool_call_id);
var tool_event = knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"tool_start",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"preview","preview",451279890),input_preview], null));
knoxx.backend.run_state.update_run_tool_receipt_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_call_id,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name], null),(function (receipt){
var G__39150 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([receipt,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"started_at","started_at",856896776),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"started_at","started_at",856896776).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.time.now_iso();
}
})()], null)], 0));
var G__39150__$1 = (((!((input_raw == null))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__39150,new cljs.core.Keyword(null,"input","input",556931961),input_raw):G__39150);
if(cljs.core.truth_(input_preview)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__39150__$1,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734),input_preview);
} else {
return G__39150__$1;
}
}));

if(first_event_QMARK_){
knoxx.backend.run_state.apply_run_tool_trace_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),"tool_start",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"preview","preview",451279890),input_preview,new cljs.core.Keyword(null,"at","at",1476951349),knoxx.backend.util.time.now_iso()], null));

knoxx.backend.run_state.append_run_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_event);

return knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",tool_event);
} else {
return null;
}
});
knoxx.backend.agents.stream.handle_tool_execution_update_BANG_ = (function knoxx$backend$agents$stream$handle_tool_execution_update_BANG_(state,event){
var tool_name = (function (){var or__5142__auto__ = (event["toolName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "tool";
}
})();
var tool_call_id = (function (){var or__5142__auto__ = (event["toolCallId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name)+"-update");
}
})();
var preview = (function (){var or__5142__auto__ = knoxx.backend.agents.content.preview_text_nonblank((event["delta"]),(400));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.preview_text_nonblank((event["update"]),(400));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.preview_text_nonblank((event["message"]),(400));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.agents.content.preview_text_nonblank((event["statusMessage"]),(400));
}
}
}
})();
knoxx.backend.run_state.update_run_tool_receipt_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_call_id,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name], null),(function (receipt){
var G__39181 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([receipt,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"status","status",-1997798413),"running"], null)], 0));
if(cljs.core.truth_(preview)){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__39181,new cljs.core.Keyword(null,"updates","updates",2013983452),(function (p1__39166_SHARP_){
return knoxx.backend.run_state.append_limited(p1__39166_SHARP_,preview,(8));
}));
} else {
return G__39181;
}
}));

knoxx.backend.run_state.apply_run_tool_trace_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),"tool_update",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"preview","preview",451279890),preview,new cljs.core.Keyword(null,"at","at",1476951349),knoxx.backend.util.time.now_iso()], null));

if(cljs.core.truth_(preview)){
var tool_event = knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"tool_update",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"preview","preview",451279890),preview], null));
knoxx.backend.run_state.append_run_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_event);

return knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",tool_event);
} else {
return null;
}
});
knoxx.backend.agents.stream.handle_tool_execution_end_BANG_ = (function knoxx$backend$agents$stream$handle_tool_execution_end_BANG_(state,event){
var tool_name = (function (){var or__5142__auto__ = (event["toolName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "tool";
}
})();
var tool_call_id = (function (){var or__5142__auto__ = (event["toolCallId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"node-crypto","node-crypto",1686415591).cljs$core$IFn$_invoke$arity$1(state).randomUUID();
}
})();
var is_error = cljs.core.boolean$((event["isError"]));
var raw_result = (function (){var or__5142__auto__ = (event["result"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (event["toolResult"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (event["output"]);
}
}
})();
var result_raw = (((raw_result == null))?null:((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(raw_result,undefined))?null:((typeof raw_result === 'string')?raw_result:(function (){try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(raw_result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e39198){var _ = e39198;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_result));
}})()
)));
var content_parts = knoxx.backend.agents.content.tool_result_content_parts(raw_result);
var result_preview = (function (){var or__5142__auto__ = knoxx.backend.agents.content.preview_text_nonblank((event["result"]),(20000));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.preview_text_nonblank((event["toolResult"]),(20000));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.agents.content.preview_text_nonblank((event["output"]),(20000));
}
}
})();
var first_event_QMARK_ = knoxx.backend.agents.stream.first_lifecycle_event_QMARK_(state,"tool_end",tool_call_id);
var tool_event = knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"tool_end",new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"status","status",-1997798413),((is_error)?"failed":"completed"),new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"is_error","is_error",-700121398),is_error,new cljs.core.Keyword(null,"preview","preview",451279890),result_preview], null));
knoxx.backend.run_state.update_run_tool_receipt_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_call_id,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name], null),(function (receipt){
var G__39202 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([receipt,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"status","status",-1997798413),((is_error)?"failed":"completed"),new cljs.core.Keyword(null,"ended_at","ended_at",1150683059),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"is_error","is_error",-700121398),is_error], null)], 0));
var G__39202__$1 = (((!((result_raw == null))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__39202,new cljs.core.Keyword(null,"result","result",1415092211),result_raw):G__39202);
var G__39202__$2 = (cljs.core.truth_(result_preview)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__39202__$1,new cljs.core.Keyword(null,"result_preview","result_preview",215554859),result_preview):G__39202__$1);
if(cljs.core.seq(content_parts)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__39202__$2,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),content_parts);
} else {
return G__39202__$2;
}
}));

if(first_event_QMARK_){
knoxx.backend.run_state.apply_run_tool_trace_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"type","type",1174270348),"tool_end",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"preview","preview",451279890),result_preview,new cljs.core.Keyword(null,"is_error","is_error",-700121398),is_error,new cljs.core.Keyword(null,"at","at",1476951349),knoxx.backend.util.time.now_iso()], null));

knoxx.backend.run_state.append_run_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),tool_event);

return knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",tool_event);
} else {
return null;
}
});
knoxx.backend.agents.stream.handle_turn_end_BANG_ = (function knoxx$backend$agents$stream$handle_turn_end_BANG_(state,event){
var tool_results = (function (){var or__5142__auto__ = (event["toolResults"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var turn_event = knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"turn_end",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),"completed",new cljs.core.Keyword(null,"tool_result_count","tool_result_count",-1860451143),(function (){var or__5142__auto__ = tool_results.length;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()], null));
knoxx.backend.run_state.append_run_event_BANG_(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),turn_event);

return knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",turn_event);
});
knoxx.backend.agents.stream.handle_agent_end_BANG_ = (function knoxx$backend$agents$stream$handle_agent_end_BANG_(state,_event){
return knoxx.backend.realtime.broadcast_ws_session_BANG_(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"events",knoxx.backend.run_state.tool_event_payload(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(state),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(state),"agent_end",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"status","status",-1997798413),"completed"], null)));
});
knoxx.backend.agents.stream.build_subscribe_handler = (function knoxx$backend$agents$stream$build_subscribe_handler(state,session){
var abort_BANG_ = (function (reason){
return knoxx.backend.agents.stream.request_abort_BANG_(state,session,reason);
});
var state__$1 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"abort!","abort!",-220883953),abort_BANG_);
return (function (event){
var event_type = (event["type"]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"message_update")){
return knoxx.backend.agents.stream.handle_message_update_BANG_(state__$1,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"message_end")){
return knoxx.backend.agents.stream.handle_message_end_BANG_(state__$1,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"tool_execution_start")){
return knoxx.backend.agents.stream.handle_tool_execution_start_BANG_(state__$1,session,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"tool_execution_update")){
return knoxx.backend.agents.stream.handle_tool_execution_update_BANG_(state__$1,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"tool_execution_end")){
return knoxx.backend.agents.stream.handle_tool_execution_end_BANG_(state__$1,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"turn_end")){
return knoxx.backend.agents.stream.handle_turn_end_BANG_(state__$1,event);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(event_type,"agent_end")){
return knoxx.backend.agents.stream.handle_agent_end_BANG_(state__$1,event);
} else {
return null;
}
}
}
}
}
}
}
});
});

//# sourceMappingURL=knoxx.backend.agents.stream.js.map
