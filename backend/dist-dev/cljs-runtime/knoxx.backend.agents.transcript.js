import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agents.content.js";
import "./knoxx.backend.text.js";
goog.provide('knoxx.backend.agents.transcript');
/**
 * Exported simplified variant (no content-parts).  Used by tests and recovery.
 */
knoxx.backend.agents.transcript.session__GT_stored_messages = (function knoxx$backend$agents$transcript$session__GT_stored_messages(session){
var messages = (cljs.core.truth_((function (){var and__5140__auto__ = session;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.array_QMARK_((session["messages"]));
} else {
return and__5140__auto__;
}
})())?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((session["messages"])):null);
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (message){
var role = (function (){var G__53444 = (message["role"]);
if((G__53444 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53444));
}
})();
var content = (message["content"]);
var text = ((typeof content === 'string')?knoxx.backend.agents.content.nonblank(content):(cljs.core.truth_(cljs.core.array_QMARK_(content))?knoxx.backend.agents.content.nonblank(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.text.content_part_text,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content))))):(function (){var G__53446 = (message["text"]);
if((G__53446 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__53446);
}
})()
));
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["user",null,"assistant",null,"system",null], null), null),role);
if(and__5140__auto__){
return text;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"content","content",15833224),text], null);
} else {
return null;
}
}),messages));
});
goog.exportSymbol('knoxx.backend.agents.transcript.session__GT_stored_messages', knoxx.backend.agents.transcript.session__GT_stored_messages);
/**
 * Internal richer variant that preserves assistant content-parts.
 */
knoxx.backend.agents.transcript.transcript_messages = (function knoxx$backend$agents$transcript$transcript_messages(session){
var messages = (cljs.core.truth_((function (){var and__5140__auto__ = session;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.array_QMARK_((session["messages"]));
} else {
return and__5140__auto__;
}
})())?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((session["messages"])):null);
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (message){
var role = (function (){var G__53450 = (message["role"]);
if((G__53450 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53450));
}
})();
var text = (function (){var G__53451 = knoxx.backend.agents.content.session_message_text(message);
if((G__53451 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__53451);
}
})();
var parts = knoxx.backend.agents.content.assistant_content_parts(message);
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["user",null,"assistant",null,"system",null], null), null),role);
if(and__5140__auto__){
var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.seq(parts);
}
} else {
return and__5140__auto__;
}
})())){
var G__53452 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"role","role",-736691072),role], null);
var G__53452__$1 = (cljs.core.truth_(text)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53452,new cljs.core.Keyword(null,"content","content",15833224),text):G__53452);
if(cljs.core.seq(parts)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53452__$1,new cljs.core.Keyword(null,"content-parts","content-parts",684529019),parts);
} else {
return G__53452__$1;
}
} else {
return null;
}
}),messages));
});
knoxx.backend.agents.transcript.append_message_if_novel = (function knoxx$backend$agents$transcript$append_message_if_novel(messages,message){
var items = cljs.core.vec((function (){var or__5142__auto__ = messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var last_message = cljs.core.peek(items);
var comparable = (function (entry){
return cljs.core.select_keys(entry,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content-parts","content-parts",684529019)], null));
});
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(comparable(last_message),comparable(message))){
return items;
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(items,message);
}
});
knoxx.backend.agents.transcript.requested_system_prompt = (function knoxx$backend$agents$transcript$requested_system_prompt(agent_spec){
var G__53455 = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__53455__$1 = (((G__53455 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__53455)));
var G__53455__$2 = (((G__53455__$1 == null))?null:clojure.string.trim(G__53455__$1));
if((G__53455__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__53455__$2);
}
});
knoxx.backend.agents.transcript.ensure_system_message = (function knoxx$backend$agents$transcript$ensure_system_message(messages,agent_spec){
return knoxx.backend.agent_runtime.sync_system_message(messages,knoxx.backend.agents.transcript.requested_system_prompt(agent_spec));
});
knoxx.backend.agents.transcript.transcript_before_prompt = (function knoxx$backend$agents$transcript$transcript_before_prompt(session,user_message,agent_spec){
return knoxx.backend.agents.transcript.append_message_if_novel(knoxx.backend.agents.transcript.ensure_system_message(knoxx.backend.agents.transcript.transcript_messages(session),agent_spec),user_message);
});
knoxx.backend.agents.transcript.transcript_after_turn = (function knoxx$backend$agents$transcript$transcript_after_turn(session,fallback_messages){
var snapshot = knoxx.backend.agents.transcript.transcript_messages(session);
if(cljs.core.seq(snapshot)){
return snapshot;
} else {
return cljs.core.vec(fallback_messages);
}
});

//# sourceMappingURL=knoxx.backend.agents.transcript.js.map
