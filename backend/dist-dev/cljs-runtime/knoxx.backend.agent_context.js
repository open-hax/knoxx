import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.agent_context');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.agent_context !== 'undefined') && (typeof knoxx.backend.agent_context.current_context_STAR_ !== 'undefined')){
} else {
knoxx.backend.agent_context.current_context_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
/**
 * Set the current agent turn context.
 */
knoxx.backend.agent_context.set_context_BANG_ = (function knoxx$backend$agent_context$set_context_BANG_(p__517531){
var map__517532 = p__517531;
var map__517532__$1 = cljs.core.__destructure_map(map__517532);
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517532__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517532__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517532__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var agent_spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517532__$1,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541));
return cljs.core.reset_BANG_(knoxx.backend.agent_context.current_context_STAR_,(cljs.core.truth_((function (){var and__5140__auto__ = session_id;
if(cljs.core.truth_(and__5140__auto__)){
return conversation_id;
} else {
return and__5140__auto__;
}
})())?(function (){var G__517533 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),conversation_id,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),run_id], null);
if(cljs.core.truth_(agent_spec)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517533,new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541),agent_spec);
} else {
return G__517533;
}
})():null));
});
/**
 * Clear the current agent turn context.
 */
knoxx.backend.agent_context.clear_context_BANG_ = (function knoxx$backend$agent_context$clear_context_BANG_(){
return cljs.core.reset_BANG_(knoxx.backend.agent_context.current_context_STAR_,null);
});
/**
 * Get the current agent turn context, or nil if none is active.
 */
knoxx.backend.agent_context.get_context = (function knoxx$backend$agent_context$get_context(){
return cljs.core.deref(knoxx.backend.agent_context.current_context_STAR_);
});

//# sourceMappingURL=knoxx.backend.agent_context.js.map
