import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.util.time.js";
import "./knoxx.backend.redis_client.js";
goog.provide('knoxx.backend.run_state');
knoxx.backend.run_state.RUN_EVENTS_KEY_PREFIX = "knoxx:run_events:";
knoxx.backend.run_state.RUN_EVENTS_MAX = (1000);
knoxx.backend.run_state.RUN_EVENTS_TTL = (7200);
knoxx.backend.run_state.run_events_key = (function knoxx$backend$run_state$run_events_key(run_id){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.run_state.RUN_EVENTS_KEY_PREFIX)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id));
});
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.run_state !== 'undefined') && (typeof knoxx.backend.run_state.runs_STAR_ !== 'undefined')){
} else {
knoxx.backend.run_state.runs_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.run_state !== 'undefined') && (typeof knoxx.backend.run_state.run_order_STAR_ !== 'undefined')){
} else {
knoxx.backend.run_state.run_order_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.run_state !== 'undefined') && (typeof knoxx.backend.run_state.retrieval_stats_STAR_ !== 'undefined')){
} else {
knoxx.backend.run_state.retrieval_stats_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"samples","samples",635504833),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"avgRetrievalMs","avgRetrievalMs",1824844412),(0),new cljs.core.Keyword(null,"p95RetrievalMs","p95RetrievalMs",2076738360),(0),new cljs.core.Keyword(null,"recentSamples","recentSamples",-1864023603),(0),new cljs.core.Keyword(null,"modeCounts","modeCounts",1131129372),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"dense","dense",-1352835783),(0),new cljs.core.Keyword(null,"hybrid","hybrid",-1594229919),(0),new cljs.core.Keyword(null,"hybrid_rerank","hybrid_rerank",1983364251),(0)], null)], null));
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.run_state !== 'undefined') && (typeof knoxx.backend.run_state.event_stream_sink_STAR_ !== 'undefined')){
} else {
knoxx.backend.run_state.event_stream_sink_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
/**
 * Register a 1-arity fire-and-forget fn called with each event as it is appended.
 * Intended for streaming events to OpenPlanner during an active run.
 */
knoxx.backend.run_state.set_event_stream_sink_BANG_ = (function knoxx$backend$run_state$set_event_stream_sink_BANG_(f){
return cljs.core.reset_BANG_(knoxx.backend.run_state.event_stream_sink_STAR_,f);
});
knoxx.backend.run_state.clear_event_stream_sink_BANG_ = (function knoxx$backend$run_state$clear_event_stream_sink_BANG_(){
return cljs.core.reset_BANG_(knoxx.backend.run_state.event_stream_sink_STAR_,null);
});
knoxx.backend.run_state.latest_assistant_message = (function knoxx$backend$run_state$latest_assistant_message(session){
var messages = (cljs.core.truth_(cljs.core.array_QMARK_((session["messages"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((session["messages"])):cljs.core.PersistentVector.EMPTY);
return cljs.core.last(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__51144_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((p1__51144_SHARP_["role"]),"assistant");
}),messages));
});
knoxx.backend.run_state.usage_map = (function knoxx$backend$run_state$usage_map(usage){
if(cljs.core.truth_(usage)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322),(function (){var or__5142__auto__ = (usage["input"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498),(function (){var or__5142__auto__ = (usage["output"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()], null);
} else {
return null;
}
});
/**
 * Defensive: coerce raw JS arrays/objects back to CLJS data before
 * storing in the in-memory run atom.  Redis round-trips and SDK
 * interop can leak raw #js [] / #js {} into fields.
 */
knoxx.backend.run_state.ensure_clj = (function knoxx$backend$run_state$ensure_clj(value){
if(cljs.core.truth_(cljs.core.array_QMARK_(value))){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
return value;

}
});
knoxx.backend.run_state.MAX_RUNS = (200);
knoxx.backend.run_state.store_run_BANG_ = (function knoxx$backend$run_state$store_run_BANG_(run_id,run){
var clean = (function (){var G__51189 = run;
var G__51189__$1 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189,new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067),knoxx.backend.run_state.ensure_clj):G__51189);
var G__51189__$2 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$1,new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),knoxx.backend.run_state.ensure_clj):G__51189__$1);
var G__51189__$3 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$2,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),knoxx.backend.run_state.ensure_clj):G__51189__$2);
var G__51189__$4 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"events","events",1792552201).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$3,new cljs.core.Keyword(null,"events","events",1792552201),knoxx.backend.run_state.ensure_clj):G__51189__$3);
var G__51189__$5 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$4,new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565),knoxx.backend.run_state.ensure_clj):G__51189__$4);
var G__51189__$6 = (cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"resources","resources",1632806811).cljs$core$IFn$_invoke$arity$1(run)))?cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$5,new cljs.core.Keyword(null,"resources","resources",1632806811),knoxx.backend.run_state.ensure_clj):G__51189__$5);
if(cljs.core.truth_(cljs.core.array_QMARK_(new cljs.core.Keyword(null,"settings","settings",1556144875).cljs$core$IFn$_invoke$arity$1(run)))){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51189__$6,new cljs.core.Keyword(null,"settings","settings",1556144875),knoxx.backend.run_state.ensure_clj);
} else {
return G__51189__$6;
}
})();
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.run_state.runs_STAR_,cljs.core.assoc,run_id,clean);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.run_state.run_order_STAR_,(function (order){
var new_order = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(knoxx.backend.run_state.MAX_RUNS,cljs.core.cons(run_id,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentHashSet.createAsIfByAssoc([run_id]),order))));
if((cljs.core.count(order) > knoxx.backend.run_state.MAX_RUNS)){
var stale_ids_51475 = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.set(new_order),cljs.core.keys(cljs.core.deref(knoxx.backend.run_state.runs_STAR_)));
if(cljs.core.seq(stale_ids_51475)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.run_state.runs_STAR_,(function (p1__51168_SHARP_){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$3(cljs.core.dissoc,p1__51168_SHARP_,stale_ids_51475);
}));
} else {
}
} else {
}

return new_order;
}));

return clean;
});
knoxx.backend.run_state.summarize_run = (function knoxx$backend$run_state$summarize_run(run){
return cljs.core.select_keys(run,new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),new cljs.core.Keyword(null,"created_at","created_at",1484050750),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"model","model",331153215),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),new cljs.core.Keyword(null,"input_tokens","input_tokens",490797322),new cljs.core.Keyword(null,"output_tokens","output_tokens",-1339146498),new cljs.core.Keyword(null,"tokens_per_s","tokens_per_s",1005457231),new cljs.core.Keyword(null,"error","error",-978969032)], null));
});
knoxx.backend.run_state.append_limited = (function knoxx$backend$run_state$append_limited(items,item,limit){
var v = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(items),item);
if((cljs.core.count(v) > limit)){
return cljs.core.subvec.cljs$core$IFn$_invoke$arity$2(v,(cljs.core.count(v) - limit));
} else {
return v;
}
});
knoxx.backend.run_state.update_run_BANG_ = (function knoxx$backend$run_state$update_run_BANG_(run_id,f){
var state = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.run_state.runs_STAR_,cljs.core.update,run_id,(function (run){
if(cljs.core.truth_(run)){
return (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(run) : f.call(null,run));
} else {
return null;
}
}));
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(state,run_id);
});
knoxx.backend.run_state.append_run_event_BANG_ = (function knoxx$backend$run_state$append_run_event_BANG_(run_id,event){
knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(run,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso()),new cljs.core.Keyword(null,"events","events",1792552201),(function (p1__51232_SHARP_){
return knoxx.backend.run_state.append_limited(p1__51232_SHARP_,event,(200));
}));
}));

var temp__5825__auto___51491 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___51491)){
var redis_client_51492 = temp__5825__auto___51491;
knoxx.backend.redis_client.lpush_json(redis_client_51492,knoxx.backend.run_state.run_events_key(run_id),event);

try{redis_client_51492.lTrim(knoxx.backend.run_state.run_events_key(run_id),(0),(knoxx.backend.run_state.RUN_EVENTS_MAX - (1)));

redis_client_51492.expire(knoxx.backend.run_state.run_events_key(run_id),knoxx.backend.run_state.RUN_EVENTS_TTL);
}catch (e51243){var __51493 = e51243;
}} else {
}

var temp__5825__auto__ = cljs.core.deref(knoxx.backend.run_state.event_stream_sink_STAR_);
if(cljs.core.truth_(temp__5825__auto__)){
var sink = temp__5825__auto__;
try{return (sink.cljs$core$IFn$_invoke$arity$1 ? sink.cljs$core$IFn$_invoke$arity$1(event) : sink.call(null,event));
}catch (e51247){var _ = e51247;
return null;
}} else {
return null;
}
});
knoxx.backend.run_state.trace_tool_block_id = (function knoxx$backend$run_state$trace_tool_block_id(p__51249){
var map__51250 = p__51249;
var map__51250__$1 = cljs.core.__destructure_map(map__51250);
var tool_call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51250__$1,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517));
var tool_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51250__$1,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484));
var at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51250__$1,new cljs.core.Keyword(null,"at","at",1476951349));
if(((typeof tool_call_id === 'string') && (cljs.core.seq(tool_call_id)))){
return (""+"tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_call_id));
} else {
if(((typeof tool_name === 'string') && (cljs.core.seq(tool_name)))){
return (""+"tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_name)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = at;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
} else {
return (""+"tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = at;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));

}
}
});
knoxx.backend.run_state.append_run_trace_text_BANG_ = (function knoxx$backend$run_state$append_run_trace_text_BANG_(run_id,kind,delta,at){
if(cljs.core.seq((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(delta)))){
return knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(run,new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),(function (blocks){
var items = cljs.core.vec(blocks);
var last_block = cljs.core.peek(items);
if(cljs.core.truth_((function (){var and__5140__auto__ = last_block;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(last_block),kind)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(last_block),"streaming")));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,(cljs.core.count(items) - (1)),cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(last_block,new cljs.core.Keyword(null,"content","content",15833224),(function (p1__51261_SHARP_){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = p1__51261_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(delta));
})),new cljs.core.Keyword(null,"at","at",1476951349),(function (){var or__5142__auto__ = at;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(last_block);
}
})()));
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(items,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(kind))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(items))),new cljs.core.Keyword(null,"kind","kind",-717265803),kind,new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",new cljs.core.Keyword(null,"content","content",15833224),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(delta)),new cljs.core.Keyword(null,"at","at",1476951349),at], null));
}
}));
}));
} else {
return null;
}
});
knoxx.backend.run_state.apply_run_tool_trace_event_BANG_ = (function knoxx$backend$run_state$apply_run_tool_trace_event_BANG_(run_id,p__51292){
var map__51294 = p__51292;
var map__51294__$1 = cljs.core.__destructure_map(map__51294);
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var tool_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484));
var tool_call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517));
var preview = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"preview","preview",451279890));
var is_error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"is_error","is_error",-700121398));
var at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51294__$1,new cljs.core.Keyword(null,"at","at",1476951349));
return knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(run,new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),(function (blocks){
var items = cljs.core.vec(blocks);
var block_id = knoxx.backend.run_state.trace_tool_block_id(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"at","at",1476951349),at], null));
var idx = cljs.core.first(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2((function (i,item){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(item),block_id)){
return i;
} else {
return null;
}
}),items));
var existing = ((typeof idx === 'number')?cljs.core.nth.cljs$core$IFn$_invoke$arity$2(items,idx):null);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(type,"tool_start")){
var block = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),block_id,new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908),new cljs.core.Keyword(null,"toolName","toolName",869440778),tool_name,new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580),tool_call_id,new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474),preview,new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",new cljs.core.Keyword(null,"at","at",1476951349),at,new cljs.core.Keyword(null,"updates","updates",2013983452),cljs.core.PersistentVector.EMPTY], null);
if(typeof idx === 'number'){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,idx,cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([existing,block], 0)));
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(items,block);
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(type,"tool_update")){
if(typeof idx === 'number'){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,idx,(function (){var G__51296 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(existing,new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"at","at",1476951349),(function (){var or__5142__auto__ = at;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(existing);
}
})()], 0));
if(cljs.core.seq(preview)){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__51296,new cljs.core.Keyword(null,"updates","updates",2013983452),(function (p1__51286_SHARP_){
return knoxx.backend.run_state.append_limited(p1__51286_SHARP_,preview,(8));
}));
} else {
return G__51296;
}
})());
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(items,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"id","id",-1388402092),block_id,new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908),new cljs.core.Keyword(null,"toolName","toolName",869440778),tool_name,new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580),tool_call_id,new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",new cljs.core.Keyword(null,"at","at",1476951349),at,new cljs.core.Keyword(null,"updates","updates",2013983452),(function (){var G__51301 = cljs.core.PersistentVector.EMPTY;
if(cljs.core.seq(preview)){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__51301,preview);
} else {
return G__51301;
}
})()], null));
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(type,"tool_end")){
var block = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),block_id,new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908),new cljs.core.Keyword(null,"toolName","toolName",869440778),tool_name,new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580),tool_call_id,new cljs.core.Keyword(null,"status","status",-1997798413),(cljs.core.truth_(is_error)?"error":"done"),new cljs.core.Keyword(null,"outputPreview","outputPreview",-747507208),preview,new cljs.core.Keyword(null,"isError","isError",-1727958473),cljs.core.boolean$(is_error),new cljs.core.Keyword(null,"at","at",1476951349),at], null);
if(typeof idx === 'number'){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,idx,cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([existing,block,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"updates","updates",2013983452),new cljs.core.Keyword(null,"updates","updates",2013983452).cljs$core$IFn$_invoke$arity$1(existing),new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474),new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474).cljs$core$IFn$_invoke$arity$1(existing)], null)], 0)));
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(items,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(block,new cljs.core.Keyword(null,"updates","updates",2013983452),cljs.core.PersistentVector.EMPTY));
}
} else {
return items;

}
}
}
}));
}));
});
knoxx.backend.run_state.finalize_run_trace_blocks_BANG_ = (function knoxx$backend$run_state$finalize_run_trace_blocks_BANG_(run_id,status){
return knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(run,new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),(function (blocks){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (block){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(block),"streaming")){
var G__51306 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(block,new cljs.core.Keyword(null,"status","status",-1997798413),status);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(status,"error")){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51306,new cljs.core.Keyword(null,"isError","isError",-1727958473),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"isError","isError",-1727958473).cljs$core$IFn$_invoke$arity$1(block);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(block),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908));
}
})());
} else {
return G__51306;
}
} else {
return block;
}
}),cljs.core.vec(blocks));
}));
}));
});
knoxx.backend.run_state.update_run_tool_receipt_BANG_ = (function knoxx$backend$run_state$update_run_tool_receipt_BANG_(run_id,receipt_id,default_receipt,f){
return knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(run,new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067),(function (receipts){
var items = cljs.core.vec(receipts);
var idx = cljs.core.first(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2((function (i,item){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(item),receipt_id)){
return i;
} else {
return null;
}
}),items));
var base = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),receipt_id], null),default_receipt], 0));
if((idx == null)){
return knoxx.backend.run_state.append_limited(items,(f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(base) : f.call(null,base)),(40));
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(items,idx,(function (){var G__51319 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([base,cljs.core.nth.cljs$core$IFn$_invoke$arity$2(items,idx)], 0));
return (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(G__51319) : f.call(null,G__51319));
})());
}
}));
}));
});
knoxx.backend.run_state.preview_present_QMARK_ = (function knoxx$backend$run_state$preview_present_QMARK_(value){
if(typeof value === 'string'){
var trimmed = clojure.string.trim(value);
var lowered = clojure.string.lower_case(trimmed);
return (((!(clojure.string.blank_QMARK_(trimmed)))) && (((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(lowered,"null")) && (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(lowered,"undefined")))));
} else {
return null;
}
});
knoxx.backend.run_state.backfill_run_tool_input_preview_BANG_ = (function knoxx$backend$run_state$backfill_run_tool_input_preview_BANG_(run_id,receipt_id,tool_name,input_preview){
if(cljs.core.truth_((function (){var and__5140__auto__ = typeof receipt_id === 'string';
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core.seq(receipt_id);
if(and__5140__auto____$1){
return knoxx.backend.run_state.preview_present_QMARK_(input_preview);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.run_state.update_run_BANG_(run_id,(function (run){
var receipt_items = cljs.core.vec(new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run));
var receipt_idx = cljs.core.first(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2((function (i,receipt){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(receipt),receipt_id)){
return i;
} else {
return null;
}
}),receipt_items));
var next_receipt = ((typeof receipt_idx === 'number')?(function (){var G__51358 = cljs.core.nth.cljs$core$IFn$_invoke$arity$2(receipt_items,receipt_idx);
var G__51358__$1 = ((cljs.core.not(knoxx.backend.run_state.preview_present_QMARK_(new cljs.core.Keyword(null,"input_preview","input_preview",2048529734).cljs$core$IFn$_invoke$arity$1(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(receipt_items,receipt_idx)))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51358,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734),input_preview):G__51358);
var G__51358__$2 = (((new cljs.core.Keyword(null,"input","input",556931961).cljs$core$IFn$_invoke$arity$1(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(receipt_items,receipt_idx)) == null))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51358__$1,new cljs.core.Keyword(null,"input","input",556931961),input_preview):G__51358__$1);
if(((cljs.core.not(cljs.core.seq(new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(receipt_items,receipt_idx))))) && (cljs.core.seq(tool_name)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51358__$2,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name);
} else {
return G__51358__$2;
}
})():new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),receipt_id,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"status","status",-1997798413),"running",new cljs.core.Keyword(null,"input","input",556931961),input_preview,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734),input_preview], null));
var next_receipts = ((typeof receipt_idx === 'number')?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(receipt_items,receipt_idx,next_receipt):knoxx.backend.run_state.append_limited(receipt_items,next_receipt,(40)));
var block_items = cljs.core.vec(new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872).cljs$core$IFn$_invoke$arity$1(run));
var block_idx = cljs.core.first(cljs.core.keep_indexed.cljs$core$IFn$_invoke$arity$2((function (i,block){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(block),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908))) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580).cljs$core$IFn$_invoke$arity$1(block),receipt_id)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(block),(""+"tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(receipt_id)))))))){
return i;
} else {
return null;
}
}),block_items));
var next_block = ((typeof block_idx === 'number')?(function (){var G__51385 = cljs.core.nth.cljs$core$IFn$_invoke$arity$2(block_items,block_idx);
var G__51385__$1 = ((cljs.core.not(knoxx.backend.run_state.preview_present_QMARK_(new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474).cljs$core$IFn$_invoke$arity$1(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(block_items,block_idx)))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51385,new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474),input_preview):G__51385);
if(((cljs.core.not(cljs.core.seq(new cljs.core.Keyword(null,"toolName","toolName",869440778).cljs$core$IFn$_invoke$arity$1(cljs.core.nth.cljs$core$IFn$_invoke$arity$2(block_items,block_idx))))) && (cljs.core.seq(tool_name)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51385__$1,new cljs.core.Keyword(null,"toolName","toolName",869440778),tool_name);
} else {
return G__51385__$1;
}
})():new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+"tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(receipt_id)),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"tool_call","tool_call",1265726908),new cljs.core.Keyword(null,"toolName","toolName",869440778),tool_name,new cljs.core.Keyword(null,"toolCallId","toolCallId",58445580),receipt_id,new cljs.core.Keyword(null,"status","status",-1997798413),"streaming",new cljs.core.Keyword(null,"inputPreview","inputPreview",-809122474),input_preview,new cljs.core.Keyword(null,"updates","updates",2013983452),cljs.core.PersistentVector.EMPTY], null));
var next_blocks = ((typeof block_idx === 'number')?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(block_items,block_idx,next_block):cljs.core.conj.cljs$core$IFn$_invoke$arity$2(block_items,next_block));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(run,new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067),next_receipts,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),next_blocks], 0));
}));
} else {
return null;
}
});
knoxx.backend.run_state.tool_event_payload = (function knoxx$backend$run_state$tool_event_payload(run_id,conversation_id,session_id,type,extra){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"type","type",1174270348),type,new cljs.core.Keyword(null,"at","at",1476951349),knoxx.backend.util.time.now_iso()], null),extra], 0));
});
knoxx.backend.run_state.percentile_95 = (function knoxx$backend$run_state$percentile_95(values){
if(cljs.core.seq(values)){
var sorted = cljs.core.sort.cljs$core$IFn$_invoke$arity$1(values);
var idx = Math.floor((0.95 * (cljs.core.count(sorted) - (1))));
return cljs.core.nth.cljs$core$IFn$_invoke$arity$3(sorted,idx,(0));
} else {
return (0);
}
});
knoxx.backend.run_state.record_retrieval_sample_BANG_ = (function knoxx$backend$run_state$record_retrieval_sample_BANG_(mode,elapsed_ms){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.run_state.retrieval_stats_STAR_,(function (stats){
var samples = cljs.core.vec(cljs.core.take_last((100),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(new cljs.core.Keyword(null,"samples","samples",635504833).cljs$core$IFn$_invoke$arity$1(stats)),elapsed_ms)));
var count_samples = cljs.core.count(samples);
var avg = (((count_samples > (0)))?(cljs.core.reduce.cljs$core$IFn$_invoke$arity$2(cljs.core._PLUS_,samples) / count_samples):(0));
var current_modes = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"modeCounts","modeCounts",1131129372).cljs$core$IFn$_invoke$arity$1(stats);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"dense","dense",-1352835783),(0),new cljs.core.Keyword(null,"hybrid","hybrid",-1594229919),(0),new cljs.core.Keyword(null,"hybrid_rerank","hybrid_rerank",1983364251),(0)], null);
}
})();
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"samples","samples",635504833),samples,new cljs.core.Keyword(null,"avgRetrievalMs","avgRetrievalMs",1824844412),Math.round(avg),new cljs.core.Keyword(null,"p95RetrievalMs","p95RetrievalMs",2076738360),Math.round(knoxx.backend.run_state.percentile_95(samples)),new cljs.core.Keyword(null,"recentSamples","recentSamples",-1864023603),count_samples,new cljs.core.Keyword(null,"modeCounts","modeCounts",1131129372),cljs.core.update.cljs$core$IFn$_invoke$arity$3(current_modes,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = mode;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "dense";
}
})()),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.inc,(0)))], null);
}));
});
knoxx.backend.run_state.active_runs_count = (function knoxx$backend$run_state$active_runs_count(){
return cljs.core.count(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__51439_SHARP_){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["running",null,"queued",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__51439_SHARP_));
}),cljs.core.vals(cljs.core.deref(knoxx.backend.run_state.runs_STAR_))));
});
/**
 * Get run events from Redis that occurred after the given timestamp.
 * Returns a promise resolving to a vector of events.
 * Events are stored newest-first in Redis (LPUSH), so we reverse for chronological order.
 */
knoxx.backend.run_state.get_run_events_since = (function knoxx$backend$run_state$get_run_events_since(redis_client,run_id,since_timestamp){
if(cljs.core.not(redis_client)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
var key = knoxx.backend.run_state.run_events_key(run_id);
return knoxx.backend.redis_client.lrange_json(redis_client,key,(0),(knoxx.backend.run_state.RUN_EVENTS_MAX - (1))).then((function (events){
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (event){
var at = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"at","at",1476951349).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (event["at"]);
}
})();
var and__5140__auto__ = at;
if(cljs.core.truth_(and__5140__auto__)){
return (cljs.core.compare(at,since_timestamp) > (0));
} else {
return and__5140__auto__;
}
}),cljs.core.reverse(events)));
})).catch((function (_){
return cljs.core.PersistentVector.EMPTY;
}));
}
});

//# sourceMappingURL=knoxx.backend.run_state.js.map
