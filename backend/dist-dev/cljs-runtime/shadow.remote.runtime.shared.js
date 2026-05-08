import "./cljs_env.js";
import "./cljs.core.js";
import "./shadow.remote.runtime.api.js";
goog.provide('shadow.remote.runtime.shared');
shadow.remote.runtime.shared.init_state = (function shadow$remote$runtime$shared$init_state(client_info){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"extensions","extensions",-1103629196),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info,new cljs.core.Keyword(null,"call-id-seq","call-id-seq",-1679248218),(0),new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),cljs.core.PersistentArrayMap.EMPTY], null);
});
shadow.remote.runtime.shared.now = (function shadow$remote$runtime$shared$now(){
return Date.now();
});
shadow.remote.runtime.shared.get_client_id = (function shadow$remote$runtime$shared$get_client_id(p__15399){
var map__15401 = p__15399;
var map__15401__$1 = cljs.core.__destructure_map(map__15401);
var runtime = map__15401__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15401__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var or__5142__auto__ = new cljs.core.Keyword(null,"client-id","client-id",-464622140).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("runtime has no assigned runtime-id",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime], null));
}
});
shadow.remote.runtime.shared.relay_msg = (function shadow$remote$runtime$shared$relay_msg(runtime,msg){
var self_id_15710 = shadow.remote.runtime.shared.get_client_id(runtime);
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(msg),self_id_15710)){
shadow.remote.runtime.api.relay_msg(runtime,msg);
} else {
Promise.resolve((1)).then((function (){
var G__15431 = runtime;
var G__15432 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"from","from",1815293044),self_id_15710);
return (shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2 ? shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2(G__15431,G__15432) : shadow.remote.runtime.shared.process.call(null,G__15431,G__15432));
}));
}

return msg;
});
shadow.remote.runtime.shared.reply = (function shadow$remote$runtime$shared$reply(runtime,p__15435,res){
var map__15436 = p__15435;
var map__15436__$1 = cljs.core.__destructure_map(map__15436);
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15436__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15436__$1,new cljs.core.Keyword(null,"from","from",1815293044));
var res__$1 = (function (){var G__15438 = res;
var G__15438__$1 = (cljs.core.truth_(call_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__15438,new cljs.core.Keyword(null,"call-id","call-id",1043012968),call_id):G__15438);
if(cljs.core.truth_(from)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__15438__$1,new cljs.core.Keyword(null,"to","to",192099007),from);
} else {
return G__15438__$1;
}
})();
return shadow.remote.runtime.api.relay_msg(runtime,res__$1);
});
shadow.remote.runtime.shared.call = (function shadow$remote$runtime$shared$call(var_args){
var G__15445 = arguments.length;
switch (G__15445) {
case 3:
return shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$3 = (function (runtime,msg,handlers){
return shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$4(runtime,msg,handlers,(0));
}));

(shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$4 = (function (p__15448,msg,handlers,timeout_after_ms){
var map__15449 = p__15448;
var map__15449__$1 = cljs.core.__destructure_map(map__15449);
var runtime = map__15449__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15449__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
if(cljs.core.map_QMARK_(msg)){
} else {
throw (new Error("Assert failed: (map? msg)"));
}

if(cljs.core.map_QMARK_(handlers)){
} else {
throw (new Error("Assert failed: (map? handlers)"));
}

if(cljs.core.nat_int_QMARK_(timeout_after_ms)){
} else {
throw (new Error("Assert failed: (nat-int? timeout-after-ms)"));
}

var call_id = new cljs.core.Keyword(null,"call-id-seq","call-id-seq",-1679248218).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(state_ref,cljs.core.update,new cljs.core.Keyword(null,"call-id-seq","call-id-seq",-1679248218),cljs.core.inc);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(state_ref,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),call_id], null),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"handlers","handlers",79528781),handlers,new cljs.core.Keyword(null,"called-at","called-at",607081160),shadow.remote.runtime.shared.now(),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg,new cljs.core.Keyword(null,"timeout","timeout",-318625318),timeout_after_ms], null));

return shadow.remote.runtime.api.relay_msg(runtime,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"call-id","call-id",1043012968),call_id));
}));

(shadow.remote.runtime.shared.call.cljs$lang$maxFixedArity = 4);

shadow.remote.runtime.shared.trigger_BANG_ = (function shadow$remote$runtime$shared$trigger_BANG_(var_args){
var args__5882__auto__ = [];
var len__5876__auto___15726 = arguments.length;
var i__5877__auto___15727 = (0);
while(true){
if((i__5877__auto___15727 < len__5876__auto___15726)){
args__5882__auto__.push((arguments[i__5877__auto___15727]));

var G__15729 = (i__5877__auto___15727 + (1));
i__5877__auto___15727 = G__15729;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (p__15461,ev,args){
var map__15462 = p__15461;
var map__15462__$1 = cljs.core.__destructure_map(map__15462);
var runtime = map__15462__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15462__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var seq__15463 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__15466 = null;
var count__15467 = (0);
var i__15468 = (0);
while(true){
if((i__15468 < count__15467)){
var ext = chunk__15466.cljs$core$IIndexed$_nth$arity$2(null,i__15468);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__15735 = seq__15463;
var G__15736 = chunk__15466;
var G__15737 = count__15467;
var G__15738 = (i__15468 + (1));
seq__15463 = G__15735;
chunk__15466 = G__15736;
count__15467 = G__15737;
i__15468 = G__15738;
continue;
} else {
var G__15739 = seq__15463;
var G__15740 = chunk__15466;
var G__15741 = count__15467;
var G__15742 = (i__15468 + (1));
seq__15463 = G__15739;
chunk__15466 = G__15740;
count__15467 = G__15741;
i__15468 = G__15742;
continue;
}
} else {
var temp__5825__auto__ = cljs.core.seq(seq__15463);
if(temp__5825__auto__){
var seq__15463__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__15463__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__15463__$1);
var G__15743 = cljs.core.chunk_rest(seq__15463__$1);
var G__15744 = c__5673__auto__;
var G__15745 = cljs.core.count(c__5673__auto__);
var G__15746 = (0);
seq__15463 = G__15743;
chunk__15466 = G__15744;
count__15467 = G__15745;
i__15468 = G__15746;
continue;
} else {
var ext = cljs.core.first(seq__15463__$1);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__15747 = cljs.core.next(seq__15463__$1);
var G__15748 = null;
var G__15749 = (0);
var G__15750 = (0);
seq__15463 = G__15747;
chunk__15466 = G__15748;
count__15467 = G__15749;
i__15468 = G__15750;
continue;
} else {
var G__15751 = cljs.core.next(seq__15463__$1);
var G__15752 = null;
var G__15753 = (0);
var G__15754 = (0);
seq__15463 = G__15751;
chunk__15466 = G__15752;
count__15467 = G__15753;
i__15468 = G__15754;
continue;
}
}
} else {
return null;
}
}
break;
}
}));

(shadow.remote.runtime.shared.trigger_BANG_.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(shadow.remote.runtime.shared.trigger_BANG_.cljs$lang$applyTo = (function (seq15458){
var G__15459 = cljs.core.first(seq15458);
var seq15458__$1 = cljs.core.next(seq15458);
var G__15460 = cljs.core.first(seq15458__$1);
var seq15458__$2 = cljs.core.next(seq15458__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__15459,G__15460,seq15458__$2);
}));

shadow.remote.runtime.shared.welcome = (function shadow$remote$runtime$shared$welcome(p__15494,p__15495){
var map__15496 = p__15494;
var map__15496__$1 = cljs.core.__destructure_map(map__15496);
var runtime = map__15496__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15496__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__15497 = p__15495;
var map__15497__$1 = cljs.core.__destructure_map(map__15497);
var msg = map__15497__$1;
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15497__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state_ref,cljs.core.assoc,new cljs.core.Keyword(null,"client-id","client-id",-464622140),client_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"welcome","welcome",-578152123),true], 0));

var map__15503 = cljs.core.deref(state_ref);
var map__15503__$1 = cljs.core.__destructure_map(map__15503);
var client_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15503__$1,new cljs.core.Keyword(null,"client-info","client-info",1958982504));
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15503__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
shadow.remote.runtime.shared.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"hello","hello",-245025397),new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info], null));

return shadow.remote.runtime.shared.trigger_BANG_(runtime,new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125));
});
shadow.remote.runtime.shared.ping = (function shadow$remote$runtime$shared$ping(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"pong","pong",-172484958)], null));
});
shadow.remote.runtime.shared.request_supported_ops = (function shadow$remote$runtime$shared$request_supported_ops(p__15532,msg){
var map__15534 = p__15532;
var map__15534__$1 = cljs.core.__destructure_map(map__15534);
var runtime = map__15534__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15534__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"supported-ops","supported-ops",337914702),new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.disj.cljs$core$IFn$_invoke$arity$variadic(cljs.core.set(cljs.core.keys(new cljs.core.Keyword(null,"ops","ops",1237330063).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))),new cljs.core.Keyword(null,"welcome","welcome",-578152123),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),new cljs.core.Keyword(null,"tool-disconnect","tool-disconnect",189103996)], 0))], null));
});
shadow.remote.runtime.shared.unknown_relay_op = (function shadow$remote$runtime$shared$unknown_relay_op(msg){
return console.warn("unknown-relay-op",msg);
});
shadow.remote.runtime.shared.unknown_op = (function shadow$remote$runtime$shared$unknown_op(msg){
return console.warn("unknown-op",msg);
});
shadow.remote.runtime.shared.add_extension_STAR_ = (function shadow$remote$runtime$shared$add_extension_STAR_(p__15557,key,p__15558){
var map__15565 = p__15557;
var map__15565__$1 = cljs.core.__destructure_map(map__15565);
var state = map__15565__$1;
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15565__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
var map__15566 = p__15558;
var map__15566__$1 = cljs.core.__destructure_map(map__15566);
var spec = map__15566__$1;
var ops = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15566__$1,new cljs.core.Keyword(null,"ops","ops",1237330063));
var transit_write_handlers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15566__$1,new cljs.core.Keyword(null,"transit-write-handlers","transit-write-handlers",1886308716));
if(cljs.core.contains_QMARK_(extensions,key)){
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("extension already registered",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"key","key",-1516042587),key,new cljs.core.Keyword(null,"spec","spec",347520401),spec], null));
} else {
}

return cljs.core.reduce_kv((function (state__$1,op_kw,op_handler){
if(cljs.core.truth_(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state__$1,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ops","ops",1237330063),op_kw], null)))){
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("op already registered",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"key","key",-1516042587),key,new cljs.core.Keyword(null,"op","op",-1882987955),op_kw], null));
} else {
}

return cljs.core.assoc_in(state__$1,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ops","ops",1237330063),op_kw], null),op_handler);
}),cljs.core.assoc_in(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extensions","extensions",-1103629196),key], null),spec),ops);
});
shadow.remote.runtime.shared.add_extension = (function shadow$remote$runtime$shared$add_extension(p__15594,key,spec){
var map__15597 = p__15594;
var map__15597__$1 = cljs.core.__destructure_map(map__15597);
var runtime = map__15597__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15597__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(state_ref,shadow.remote.runtime.shared.add_extension_STAR_,key,spec);

var temp__5829__auto___15793 = new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125).cljs$core$IFn$_invoke$arity$1(spec);
if((temp__5829__auto___15793 == null)){
} else {
var on_welcome_15796 = temp__5829__auto___15793;
if(cljs.core.truth_(new cljs.core.Keyword(null,"welcome","welcome",-578152123).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))){
(on_welcome_15796.cljs$core$IFn$_invoke$arity$0 ? on_welcome_15796.cljs$core$IFn$_invoke$arity$0() : on_welcome_15796.call(null));
} else {
}
}

return runtime;
});
shadow.remote.runtime.shared.add_defaults = (function shadow$remote$runtime$shared$add_defaults(runtime){
return shadow.remote.runtime.shared.add_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.shared","defaults","shadow.remote.runtime.shared/defaults",-1821257543),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"welcome","welcome",-578152123),(function (p1__15612_SHARP_){
return shadow.remote.runtime.shared.welcome(runtime,p1__15612_SHARP_);
}),new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),(function (p1__15613_SHARP_){
return shadow.remote.runtime.shared.unknown_relay_op(p1__15613_SHARP_);
}),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),(function (p1__15614_SHARP_){
return shadow.remote.runtime.shared.unknown_op(p1__15614_SHARP_);
}),new cljs.core.Keyword(null,"ping","ping",-1670114784),(function (p1__15615_SHARP_){
return shadow.remote.runtime.shared.ping(runtime,p1__15615_SHARP_);
}),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),(function (p1__15616_SHARP_){
return shadow.remote.runtime.shared.request_supported_ops(runtime,p1__15616_SHARP_);
})], null)], null));
});
shadow.remote.runtime.shared.del_extension_STAR_ = (function shadow$remote$runtime$shared$del_extension_STAR_(state,key){
var ext = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extensions","extensions",-1103629196),key], null));
if(cljs.core.not(ext)){
return state;
} else {
return cljs.core.reduce_kv((function (state__$1,op_kw,op_handler){
return cljs.core.update_in.cljs$core$IFn$_invoke$arity$4(state__$1,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ops","ops",1237330063)], null),cljs.core.dissoc,op_kw);
}),cljs.core.update.cljs$core$IFn$_invoke$arity$4(state,new cljs.core.Keyword(null,"extensions","extensions",-1103629196),cljs.core.dissoc,key),new cljs.core.Keyword(null,"ops","ops",1237330063).cljs$core$IFn$_invoke$arity$1(ext));
}
});
shadow.remote.runtime.shared.del_extension = (function shadow$remote$runtime$shared$del_extension(p__15639,key){
var map__15641 = p__15639;
var map__15641__$1 = cljs.core.__destructure_map(map__15641);
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15641__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(state_ref,shadow.remote.runtime.shared.del_extension_STAR_,key);
});
shadow.remote.runtime.shared.unhandled_call_result = (function shadow$remote$runtime$shared$unhandled_call_result(call_config,msg){
return console.warn("unhandled call result",msg,call_config);
});
shadow.remote.runtime.shared.unhandled_client_not_found = (function shadow$remote$runtime$shared$unhandled_client_not_found(p__15649,msg){
var map__15652 = p__15649;
var map__15652__$1 = cljs.core.__destructure_map(map__15652);
var runtime = map__15652__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15652__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic(runtime,new cljs.core.Keyword(null,"on-client-not-found","on-client-not-found",-642452849),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msg], 0));
});
shadow.remote.runtime.shared.reply_unknown_op = (function shadow$remote$runtime$shared$reply_unknown_op(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg], null));
});
shadow.remote.runtime.shared.process = (function shadow$remote$runtime$shared$process(p__15661,p__15662){
var map__15663 = p__15661;
var map__15663__$1 = cljs.core.__destructure_map(map__15663);
var runtime = map__15663__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15663__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__15664 = p__15662;
var map__15664__$1 = cljs.core.__destructure_map(map__15664);
var msg = map__15664__$1;
var op = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15664__$1,new cljs.core.Keyword(null,"op","op",-1882987955));
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15664__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
var state = cljs.core.deref(state_ref);
var op_handler = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"ops","ops",1237330063),op], null));
if(cljs.core.truth_(call_id)){
var cfg = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),call_id], null));
var call_handler = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cfg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"handlers","handlers",79528781),op], null));
if(cljs.core.truth_(call_handler)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state_ref,cljs.core.update,new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),cljs.core.dissoc,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([call_id], 0));

return (call_handler.cljs$core$IFn$_invoke$arity$1 ? call_handler.cljs$core$IFn$_invoke$arity$1(msg) : call_handler.call(null,msg));
} else {
if(cljs.core.truth_(op_handler)){
return (op_handler.cljs$core$IFn$_invoke$arity$1 ? op_handler.cljs$core$IFn$_invoke$arity$1(msg) : op_handler.call(null,msg));
} else {
return shadow.remote.runtime.shared.unhandled_call_result(cfg,msg);

}
}
} else {
if(cljs.core.truth_(op_handler)){
return (op_handler.cljs$core$IFn$_invoke$arity$1 ? op_handler.cljs$core$IFn$_invoke$arity$1(msg) : op_handler.call(null,msg));
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"client-not-found","client-not-found",-1754042614),op)){
return shadow.remote.runtime.shared.unhandled_client_not_found(runtime,msg);
} else {
return shadow.remote.runtime.shared.reply_unknown_op(runtime,msg);

}
}
}
});
shadow.remote.runtime.shared.run_on_idle = (function shadow$remote$runtime$shared$run_on_idle(state_ref){
var seq__15670 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__15672 = null;
var count__15673 = (0);
var i__15674 = (0);
while(true){
if((i__15674 < count__15673)){
var map__15691 = chunk__15672.cljs$core$IIndexed$_nth$arity$2(null,i__15674);
var map__15691__$1 = cljs.core.__destructure_map(map__15691);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15691__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__15818 = seq__15670;
var G__15819 = chunk__15672;
var G__15820 = count__15673;
var G__15821 = (i__15674 + (1));
seq__15670 = G__15818;
chunk__15672 = G__15819;
count__15673 = G__15820;
i__15674 = G__15821;
continue;
} else {
var G__15824 = seq__15670;
var G__15825 = chunk__15672;
var G__15826 = count__15673;
var G__15827 = (i__15674 + (1));
seq__15670 = G__15824;
chunk__15672 = G__15825;
count__15673 = G__15826;
i__15674 = G__15827;
continue;
}
} else {
var temp__5825__auto__ = cljs.core.seq(seq__15670);
if(temp__5825__auto__){
var seq__15670__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__15670__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__15670__$1);
var G__15837 = cljs.core.chunk_rest(seq__15670__$1);
var G__15838 = c__5673__auto__;
var G__15839 = cljs.core.count(c__5673__auto__);
var G__15840 = (0);
seq__15670 = G__15837;
chunk__15672 = G__15838;
count__15673 = G__15839;
i__15674 = G__15840;
continue;
} else {
var map__15701 = cljs.core.first(seq__15670__$1);
var map__15701__$1 = cljs.core.__destructure_map(map__15701);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__15701__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__15841 = cljs.core.next(seq__15670__$1);
var G__15842 = null;
var G__15843 = (0);
var G__15844 = (0);
seq__15670 = G__15841;
chunk__15672 = G__15842;
count__15673 = G__15843;
i__15674 = G__15844;
continue;
} else {
var G__15845 = cljs.core.next(seq__15670__$1);
var G__15846 = null;
var G__15847 = (0);
var G__15848 = (0);
seq__15670 = G__15845;
chunk__15672 = G__15846;
count__15673 = G__15847;
i__15674 = G__15848;
continue;
}
}
} else {
return null;
}
}
break;
}
});

//# sourceMappingURL=shadow.remote.runtime.shared.js.map
