import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.turn_control');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.turn_control !== 'undefined') && (typeof knoxx.backend.turn_control.active_turns_STAR_ !== 'undefined')){
} else {
knoxx.backend.turn_control.active_turns_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
/**
 * Register an active turn control entry.
 * 
 * entry keys:
 * - :run_id
 * - :session_id
 * - :abort! (fn [reason] => Promise)
 * - :started_at
 *   
 */
knoxx.backend.turn_control.register_active_turn_BANG_ = (function knoxx$backend$turn_control$register_active_turn_BANG_(conversation_id,entry){
if(cljs.core.truth_((function (){var and__5140__auto__ = conversation_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)));
} else {
return and__5140__auto__;
}
})())){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.turn_control.active_turns_STAR_,cljs.core.assoc,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)),entry);
} else {
}

return entry;
});
/**
 * Remove the active turn entry if it matches the run-id (when provided).
 */
knoxx.backend.turn_control.unregister_active_turn_BANG_ = (function knoxx$backend$turn_control$unregister_active_turn_BANG_(var_args){
var G__522622 = arguments.length;
switch (G__522622) {
case 1:
return knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (conversation_id){
return knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2(conversation_id,null);
}));

(knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (conversation_id,run_id){
if(cljs.core.truth_((function (){var and__5140__auto__ = conversation_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)));
} else {
return and__5140__auto__;
}
})())){
var cid_522654 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.turn_control.active_turns_STAR_,(function (m){
var entry = cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,cid_522654);
if((entry == null)){
return m;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = run_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(entry))));
} else {
return and__5140__auto__;
}
})())){
return m;
} else {
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(m,cid_522654);

}
}
}));
} else {
}

return true;
}));

(knoxx.backend.turn_control.unregister_active_turn_BANG_.cljs$lang$maxFixedArity = 2);

knoxx.backend.turn_control.active_turn = (function knoxx$backend$turn_control$active_turn(conversation_id){
if(cljs.core.truth_((function (){var and__5140__auto__ = conversation_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.turn_control.active_turns_STAR_),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)));
} else {
return null;
}
});
knoxx.backend.turn_control.active_turn_count = (function knoxx$backend$turn_control$active_turn_count(){
return cljs.core.count(cljs.core.deref(knoxx.backend.turn_control.active_turns_STAR_));
});
knoxx.backend.turn_control.active_turn_entries = (function knoxx$backend$turn_control$active_turn_entries(){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p__522632){
var vec__522634 = p__522632;
var conversation_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522634,(0),null);
var entry = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522634,(1),null);
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(entry,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id);
}),cljs.core.deref(knoxx.backend.turn_control.active_turns_STAR_));
});
/**
 * Abort the currently registered turn for conversation-id.
 * 
 * Returns a Promise resolving to {:ok boolean, ...}.
 */
knoxx.backend.turn_control.abort_active_turn_BANG_ = (function knoxx$backend$turn_control$abort_active_turn_BANG_(conversation_id,reason){
var entry = knoxx.backend.turn_control.active_turn(conversation_id);
var abort_BANG_ = new cljs.core.Keyword(null,"abort!","abort!",-220883953).cljs$core$IFn$_invoke$arity$1(entry);
if((entry == null)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"no_active_turn"], null));
} else {
if((!(cljs.core.fn_QMARK_(abort_BANG_)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"no_abort_handler"], null));
} else {
return (function (){var G__522645 = (function (){var or__5142__auto__ = reason;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "aborted";
}
})();
return (abort_BANG_.cljs$core$IFn$_invoke$arity$1 ? abort_BANG_.cljs$core$IFn$_invoke$arity$1(G__522645) : abort_BANG_.call(null,G__522645));
})().then((function (_){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(entry)], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)),new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));

}
}
});

//# sourceMappingURL=knoxx.backend.turn_control.js.map
