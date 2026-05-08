import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.redis_client.js";
goog.provide('knoxx.backend.session_store');
knoxx.backend.session_store.SESSION_TTL_SECONDS = (3600);
knoxx.backend.session_store.SESSION_KEY_PREFIX = "knoxx:session:";
knoxx.backend.session_store.CONVERSATION_SESSION_KEY = "knoxx:conversation_to_session:";
knoxx.backend.session_store.ACTIVE_SESSIONS_SET = "knoxx:active_sessions";
knoxx.backend.session_store.session_key = (function knoxx$backend$session_store$session_key(session_id){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.session_store.SESSION_KEY_PREFIX)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id));
});
knoxx.backend.session_store.conversation_session_key = (function knoxx$backend$session_store$conversation_session_key(conversation_id){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.session_store.CONVERSATION_SESSION_KEY)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id));
});
knoxx.backend.session_store.resolved = (function knoxx$backend$session_store$resolved(value){
return Promise.resolve(value);
});
knoxx.backend.session_store.now_iso = (function knoxx$backend$session_store$now_iso(){
return (new Date()).toISOString();
});
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.session_store !== 'undefined') && (typeof knoxx.backend.session_store.session_cache_STAR_ !== 'undefined')){
} else {
knoxx.backend.session_store.session_cache_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
/**
 * Get session state, checking cache first then Redis.
 * Always resolves a promise for call-site consistency.
 */
knoxx.backend.session_store.get_session = (function knoxx$backend$session_store$get_session(redis_client,session_id){
var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_store.session_cache_STAR_),session_id);
if(cljs.core.truth_(temp__5823__auto__)){
var cached = temp__5823__auto__;
return knoxx.backend.session_store.resolved(cached);
} else {
if(cljs.core.truth_(redis_client)){
return knoxx.backend.redis_client.get_json(redis_client,knoxx.backend.session_store.session_key(session_id)).then((function (session){
if(cljs.core.truth_(session)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_store.session_cache_STAR_,cljs.core.assoc,session_id,session);
} else {
}

return session;
}));
} else {
return knoxx.backend.session_store.resolved(null);
}
}
});
/**
 * Synchronous session lookup from cache only. Use get-session for full lookup.
 */
knoxx.backend.session_store.get_session_sync = (function knoxx$backend$session_store$get_session_sync(session_id){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_store.session_cache_STAR_),session_id);
});
/**
 * Get the active session ID for a conversation.
 */
knoxx.backend.session_store.get_conversation_active_session = (function knoxx$backend$session_store$get_conversation_active_session(redis_client,conversation_id){
if(cljs.core.truth_(redis_client)){
return knoxx.backend.redis_client.get_key(redis_client,knoxx.backend.session_store.conversation_session_key(conversation_id));
} else {
return knoxx.backend.session_store.resolved(null);
}
});
/**
 * Store session state in cache and Redis.
 * Always resolves a promise with the stored session.
 */
knoxx.backend.session_store.put_session_BANG_ = (function knoxx$backend$session_store$put_session_BANG_(redis_client,session){
var session_id = (function (){var G__519226 = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session);
if((G__519226 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__519226));
}
})();
var conversation_id = (function (){var G__519227 = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(session);
if((G__519227 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__519227));
}
})();
var session__$1 = (function (){var G__519228 = session;
var G__519228__$1 = (cljs.core.truth_(session_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__519228,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id):G__519228);
if(cljs.core.truth_(conversation_id)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__519228__$1,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id);
} else {
return G__519228__$1;
}
})();
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_store.session_cache_STAR_,cljs.core.assoc,session_id,session__$1);

if(cljs.core.truth_(redis_client)){
return knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$4(redis_client,knoxx.backend.session_store.session_key(session_id),session__$1,knoxx.backend.session_store.SESSION_TTL_SECONDS).then((function (){
if(cljs.core.truth_(conversation_id)){
return knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$4(redis_client,knoxx.backend.session_store.conversation_session_key(conversation_id),session_id,knoxx.backend.session_store.SESSION_TTL_SECONDS);
} else {
return knoxx.backend.session_store.resolved(null);
}
})).then((function (){
if(cljs.core.truth_(session_id)){
return knoxx.backend.redis_client.sadd(redis_client,knoxx.backend.session_store.ACTIVE_SESSIONS_SET,session_id);
} else {
console.error("[session-store] put-session! reached sadd with nil session-id; session keys:",cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.keys(session__$1)], 0)));

return Promise.resolve(null);
}
})).then((function (){
return session__$1;
})).catch((function (err){
console.error("Failed to persist session to Redis:",err);

return session__$1;
}));
} else {
return knoxx.backend.session_store.resolved(session__$1);
}
});
/**
 * Update session state, merging with existing. Always resolves the updated session.
 */
knoxx.backend.session_store.update_session_BANG_ = (function knoxx$backend$session_store$update_session_BANG_(redis_client,session_id,updates){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
console.error("[session-store] update-session! called with nil/blank session-id; updates:",cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([updates], 0)));

return Promise.resolve(null);
} else {
var raw = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_store.session_cache_STAR_),session_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var current = (cljs.core.truth_(cljs.core.array_QMARK_(raw))?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(raw,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):raw);
var updated = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([current,updates,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),Date.now()], null)], 0));
return knoxx.backend.session_store.put_session_BANG_(redis_client,updated);
}
});
/**
 * Remove the last N user turns plus everything that followed them.
 * Preserves any leading system messages that predate the removed turn(s).
 */
knoxx.backend.session_store.rewind_messages = (function knoxx$backend$session_store$rewind_messages(messages,turns){
var remaining = cljs.core.vec((function (){var or__5142__auto__ = messages;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var turns_left = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = turns;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})());
while(true){
if((((turns_left === (0))) || (cljs.core.empty_QMARK_(remaining)))){
return remaining;
} else {
var temp__5823__auto__ = cljs.core.last(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2(((function (remaining,turns_left){
return (function (index,message){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("user",new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(message))){
return index;
} else {
return null;
}
});})(remaining,turns_left))
,remaining));
if(cljs.core.truth_(temp__5823__auto__)){
var last_user_index = temp__5823__auto__;
var G__519372 = cljs.core.subvec.cljs$core$IFn$_invoke$arity$3(remaining,(0),last_user_index);
var G__519373 = (turns_left - (1));
remaining = G__519372;
turns_left = G__519373;
continue;
} else {
return remaining;
}
}
break;
}
});
/**
 * Rewind the session by removing the last N user turns.
 * Resolves nil when no session exists, or the updated session when successful.
 */
knoxx.backend.session_store.undo_session_turns_BANG_ = (function knoxx$backend$session_store$undo_session_turns_BANG_(redis_client,session_id,turns){
return knoxx.backend.session_store.get_session(redis_client,session_id).then((function (session){
if(cljs.core.not(session)){
return null;
} else {
var current_messages = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var rewound_messages = knoxx.backend.session_store.rewind_messages(current_messages,turns);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(rewound_messages,current_messages)){
return session;
} else {
return knoxx.backend.session_store.put_session_BANG_(redis_client,cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(session,new cljs.core.Keyword(null,"messages","messages",345434482),rewound_messages,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"status","status",-1997798413),"waiting_input",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.session_store.now_iso(),new cljs.core.Keyword(null,"answer","answer",-742633163),null,new cljs.core.Keyword(null,"error","error",-978969032),null], 0)));
}
}
}));
});
/**
 * Remove session from cache and Redis.
 */
knoxx.backend.session_store.remove_session_BANG_ = (function knoxx$backend$session_store$remove_session_BANG_(redis_client,session_id,conversation_id){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_store.session_cache_STAR_,cljs.core.dissoc,session_id);

if(cljs.core.truth_(redis_client)){
return knoxx.backend.redis_client.del(redis_client,knoxx.backend.session_store.session_key(session_id)).then((function (){
if(cljs.core.truth_(conversation_id)){
return knoxx.backend.redis_client.del(redis_client,knoxx.backend.session_store.conversation_session_key(conversation_id));
} else {
return knoxx.backend.session_store.resolved(null);
}
})).then((function (){
return knoxx.backend.redis_client.srem(redis_client,knoxx.backend.session_store.ACTIVE_SESSIONS_SET,session_id);
})).then((function (){
return true;
})).catch((function (err){
console.error("Failed to remove session from Redis:",err);

return false;
}));
} else {
return knoxx.backend.session_store.resolved(true);
}
});
/**
 * List all active session IDs from Redis.
 */
knoxx.backend.session_store.list_active_sessions = (function knoxx$backend$session_store$list_active_sessions(redis_client){
if(cljs.core.truth_(redis_client)){
return knoxx.backend.redis_client.smembers(redis_client,knoxx.backend.session_store.ACTIVE_SESSIONS_SET);
} else {
return knoxx.backend.session_store.resolved(cljs.core.PersistentVector.EMPTY);
}
});
/**
 * Recover sessions from Redis on startup. Returns the session records that were still running.
 */
knoxx.backend.session_store.recover_sessions_BANG_ = (function knoxx$backend$session_store$recover_sessions_BANG_(redis_client){
if(cljs.core.not(redis_client)){
return knoxx.backend.session_store.resolved(cljs.core.PersistentVector.EMPTY);
} else {
return knoxx.backend.session_store.list_active_sessions(redis_client).then((function (session_ids){
var ids = cljs.core.vec(session_ids);
if(cljs.core.seq(ids)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__519266_SHARP_){
return knoxx.backend.session_store.get_session(redis_client,p1__519266_SHARP_);
}),ids))).then((function (results){
var sessions = cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
var pairs = cljs.core.map.cljs$core$IFn$_invoke$arity$3(cljs.core.vector,ids,sessions);
var stale_ids = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.first,cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__519276){
var vec__519277 = p__519276;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__519277,(0),null);
var session = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__519277,(1),null);
return (session == null);
}),pairs)));
var cacheable = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p__519280){
var vec__519283 = p__519280;
var session_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__519283,(0),null);
var session = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__519283,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = session;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))) || (clojure.string.includes_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)),"-sticky")));
} else {
return and__5140__auto__;
}
})())){
return session;
} else {
return null;
}
}),pairs));
var running = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__519268_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__519268_SHARP_));
}),cacheable));
var seq__519290_519385 = cljs.core.seq(stale_ids);
var chunk__519291_519386 = null;
var count__519292_519387 = (0);
var i__519293_519388 = (0);
while(true){
if((i__519293_519388 < count__519292_519387)){
var stale_id_519389 = chunk__519291_519386.cljs$core$IIndexed$_nth$arity$2(null,i__519293_519388);
knoxx.backend.redis_client.srem(redis_client,knoxx.backend.session_store.ACTIVE_SESSIONS_SET,stale_id_519389);


var G__519391 = seq__519290_519385;
var G__519392 = chunk__519291_519386;
var G__519393 = count__519292_519387;
var G__519394 = (i__519293_519388 + (1));
seq__519290_519385 = G__519391;
chunk__519291_519386 = G__519392;
count__519292_519387 = G__519393;
i__519293_519388 = G__519394;
continue;
} else {
var temp__5825__auto___519395 = cljs.core.seq(seq__519290_519385);
if(temp__5825__auto___519395){
var seq__519290_519398__$1 = temp__5825__auto___519395;
if(cljs.core.chunked_seq_QMARK_(seq__519290_519398__$1)){
var c__5673__auto___519400 = cljs.core.chunk_first(seq__519290_519398__$1);
var G__519401 = cljs.core.chunk_rest(seq__519290_519398__$1);
var G__519402 = c__5673__auto___519400;
var G__519403 = cljs.core.count(c__5673__auto___519400);
var G__519404 = (0);
seq__519290_519385 = G__519401;
chunk__519291_519386 = G__519402;
count__519292_519387 = G__519403;
i__519293_519388 = G__519404;
continue;
} else {
var stale_id_519405 = cljs.core.first(seq__519290_519398__$1);
knoxx.backend.redis_client.srem(redis_client,knoxx.backend.session_store.ACTIVE_SESSIONS_SET,stale_id_519405);


var G__519406 = cljs.core.next(seq__519290_519398__$1);
var G__519407 = null;
var G__519408 = (0);
var G__519409 = (0);
seq__519290_519385 = G__519406;
chunk__519291_519386 = G__519407;
count__519292_519387 = G__519408;
i__519293_519388 = G__519409;
continue;
}
} else {
}
}
break;
}

var seq__519301_519410 = cljs.core.seq(cacheable);
var chunk__519302_519411 = null;
var count__519303_519412 = (0);
var i__519304_519413 = (0);
while(true){
if((i__519304_519413 < count__519303_519412)){
var session_519419 = chunk__519302_519411.cljs$core$IIndexed$_nth$arity$2(null,i__519304_519413);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_store.session_cache_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session_519419),session_519419);


var G__519420 = seq__519301_519410;
var G__519421 = chunk__519302_519411;
var G__519422 = count__519303_519412;
var G__519423 = (i__519304_519413 + (1));
seq__519301_519410 = G__519420;
chunk__519302_519411 = G__519421;
count__519303_519412 = G__519422;
i__519304_519413 = G__519423;
continue;
} else {
var temp__5825__auto___519424 = cljs.core.seq(seq__519301_519410);
if(temp__5825__auto___519424){
var seq__519301_519425__$1 = temp__5825__auto___519424;
if(cljs.core.chunked_seq_QMARK_(seq__519301_519425__$1)){
var c__5673__auto___519426 = cljs.core.chunk_first(seq__519301_519425__$1);
var G__519427 = cljs.core.chunk_rest(seq__519301_519425__$1);
var G__519428 = c__5673__auto___519426;
var G__519429 = cljs.core.count(c__5673__auto___519426);
var G__519430 = (0);
seq__519301_519410 = G__519427;
chunk__519302_519411 = G__519428;
count__519303_519412 = G__519429;
i__519304_519413 = G__519430;
continue;
} else {
var session_519431 = cljs.core.first(seq__519301_519425__$1);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_store.session_cache_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(session_519431),session_519431);


var G__519433 = cljs.core.next(seq__519301_519425__$1);
var G__519434 = null;
var G__519435 = (0);
var G__519436 = (0);
seq__519301_519410 = G__519433;
chunk__519302_519411 = G__519434;
count__519303_519412 = G__519435;
i__519304_519413 = G__519436;
continue;
}
} else {
}
}
break;
}

return running;
}));
} else {
return knoxx.backend.session_store.resolved(cljs.core.PersistentVector.EMPTY);
}
}));
}
});
/**
 * Mark session as actively streaming.
 */
knoxx.backend.session_store.mark_session_streaming_BANG_ = (function knoxx$backend$session_store$mark_session_streaming_BANG_(redis_client,session_id,is_streaming){
return knoxx.backend.session_store.update_session_BANG_(redis_client,session_id,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),is_streaming], null));
});
/**
 * Mark session as completed and remove from active set.
 * Optionally archive to OpenPlanner for long-term memory.
 */
knoxx.backend.session_store.complete_session_BANG_ = (function knoxx$backend$session_store$complete_session_BANG_(redis_client,session_id,conversation_id,opts){
var map__519319 = opts;
var map__519319__$1 = cljs.core.__destructure_map(map__519319);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__519319__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var answer = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__519319__$1,new cljs.core.Keyword(null,"answer","answer",-742633163));
var error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__519319__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var messages = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__519319__$1,new cljs.core.Keyword(null,"messages","messages",345434482));
return knoxx.backend.session_store.update_session_BANG_(redis_client,session_id,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"status","status",-1997798413),(function (){var or__5142__auto__ = status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "completed";
}
})(),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false,new cljs.core.Keyword(null,"answer","answer",-742633163),answer,new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"messages","messages",345434482),messages], null)).then((function (session){
var sticky_QMARK__519439 = clojure.string.includes_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)),"-sticky");
if(sticky_QMARK__519439){
} else {
setTimeout((function (){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_store.session_cache_STAR_,cljs.core.dissoc,session_id);

return knoxx.backend.session_store.remove_session_BANG_(redis_client,session_id,conversation_id);
}),(60000));
}

return session;
}));
});
/**
 * Check if session can accept new messages.
 * Returns {:can-send true|false :reason <string-or-nil>}.
 */
knoxx.backend.session_store.session_can_send_QMARK_ = (function knoxx$backend$session_store$session_can_send_QMARK_(session){
if((session == null)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"No existing session. Ready for new conversation."], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("running",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),false,new cljs.core.Keyword(null,"reason","reason",-2070751759),(cljs.core.truth_(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(session))?"Session is actively streaming. Use steer or wait.":"Session is already processing. Use steer, follow-up, abort, or wait.")], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("waiting_input",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),null], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("completed",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"Previous session completed. Starting new turn."], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("failed",new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(session))){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"Previous session failed. Starting new turn."], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"can-send","can-send",-704220819),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),null], null);

}
}
}
}
}
});
knoxx.backend.session_store.active_session_snapshots = (function knoxx$backend$session_store$active_session_snapshots(){
return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),(function (p1__519335_SHARP_,p2__519334_SHARP_){
return cljs.core.compare(p2__519334_SHARP_,p1__519335_SHARP_);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__519333_SHARP_){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["waiting_input",null,"running",null,"queued",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__519333_SHARP_));
}),cljs.core.vals(cljs.core.deref(knoxx.backend.session_store.session_cache_STAR_)))));
});
knoxx.backend.session_store.debug_dump_cache = (function knoxx$backend$session_store$debug_dump_cache(){
return console.log("Session cache:",cljs.core.clj__GT_js(cljs.core.deref(knoxx.backend.session_store.session_cache_STAR_)));
});

//# sourceMappingURL=knoxx.backend.session_store.js.map
