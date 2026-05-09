goog.provide('shadow.remote.runtime.shared');
shadow.remote.runtime.shared.init_state = (function shadow$remote$runtime$shared$init_state(client_info){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"extensions","extensions",-1103629196),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info,new cljs.core.Keyword(null,"call-id-seq","call-id-seq",-1679248218),(0),new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),cljs.core.PersistentArrayMap.EMPTY], null);
});
shadow.remote.runtime.shared.now = (function shadow$remote$runtime$shared$now(){
return Date.now();
});
shadow.remote.runtime.shared.get_client_id = (function shadow$remote$runtime$shared$get_client_id(p__10329){
var map__10332 = p__10329;
var map__10332__$1 = cljs.core.__destructure_map(map__10332);
var runtime = map__10332__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10332__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var or__5142__auto__ = new cljs.core.Keyword(null,"client-id","client-id",-464622140).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("runtime has no assigned runtime-id",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime], null));
}
});
shadow.remote.runtime.shared.relay_msg = (function shadow$remote$runtime$shared$relay_msg(runtime,msg){
var self_id_10947 = shadow.remote.runtime.shared.get_client_id(runtime);
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(msg),self_id_10947)){
shadow.remote.runtime.api.relay_msg(runtime,msg);
} else {
Promise.resolve((1)).then((function (){
var G__10356 = runtime;
var G__10357 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"from","from",1815293044),self_id_10947);
return (shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2 ? shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2(G__10356,G__10357) : shadow.remote.runtime.shared.process.call(null,G__10356,G__10357));
}));
}

return msg;
});
shadow.remote.runtime.shared.reply = (function shadow$remote$runtime$shared$reply(runtime,p__10397,res){
var map__10429 = p__10397;
var map__10429__$1 = cljs.core.__destructure_map(map__10429);
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10429__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10429__$1,new cljs.core.Keyword(null,"from","from",1815293044));
var res__$1 = (function (){var G__10445 = res;
var G__10445__$1 = (cljs.core.truth_(call_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__10445,new cljs.core.Keyword(null,"call-id","call-id",1043012968),call_id):G__10445);
if(cljs.core.truth_(from)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__10445__$1,new cljs.core.Keyword(null,"to","to",192099007),from);
} else {
return G__10445__$1;
}
})();
return shadow.remote.runtime.api.relay_msg(runtime,res__$1);
});
shadow.remote.runtime.shared.call = (function shadow$remote$runtime$shared$call(var_args){
var G__10652 = arguments.length;
switch (G__10652) {
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

(shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$4 = (function (p__10653,msg,handlers,timeout_after_ms){
var map__10656 = p__10653;
var map__10656__$1 = cljs.core.__destructure_map(map__10656);
var runtime = map__10656__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10656__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
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
var len__5876__auto___10980 = arguments.length;
var i__5877__auto___10981 = (0);
while(true){
if((i__5877__auto___10981 < len__5876__auto___10980)){
args__5882__auto__.push((arguments[i__5877__auto___10981]));

var G__10986 = (i__5877__auto___10981 + (1));
i__5877__auto___10981 = G__10986;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (p__10695,ev,args){
var map__10696 = p__10695;
var map__10696__$1 = cljs.core.__destructure_map(map__10696);
var runtime = map__10696__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10696__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var seq__10704 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__10707 = null;
var count__10708 = (0);
var i__10709 = (0);
while(true){
if((i__10709 < count__10708)){
var ext = chunk__10707.cljs$core$IIndexed$_nth$arity$2(null,i__10709);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__10994 = seq__10704;
var G__10995 = chunk__10707;
var G__10996 = count__10708;
var G__10997 = (i__10709 + (1));
seq__10704 = G__10994;
chunk__10707 = G__10995;
count__10708 = G__10996;
i__10709 = G__10997;
continue;
} else {
var G__10999 = seq__10704;
var G__11000 = chunk__10707;
var G__11001 = count__10708;
var G__11002 = (i__10709 + (1));
seq__10704 = G__10999;
chunk__10707 = G__11000;
count__10708 = G__11001;
i__10709 = G__11002;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__10704);
if(temp__5823__auto__){
var seq__10704__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__10704__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__10704__$1);
var G__11003 = cljs.core.chunk_rest(seq__10704__$1);
var G__11004 = c__5673__auto__;
var G__11005 = cljs.core.count(c__5673__auto__);
var G__11006 = (0);
seq__10704 = G__11003;
chunk__10707 = G__11004;
count__10708 = G__11005;
i__10709 = G__11006;
continue;
} else {
var ext = cljs.core.first(seq__10704__$1);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__11007 = cljs.core.next(seq__10704__$1);
var G__11008 = null;
var G__11009 = (0);
var G__11010 = (0);
seq__10704 = G__11007;
chunk__10707 = G__11008;
count__10708 = G__11009;
i__10709 = G__11010;
continue;
} else {
var G__11011 = cljs.core.next(seq__10704__$1);
var G__11012 = null;
var G__11013 = (0);
var G__11014 = (0);
seq__10704 = G__11011;
chunk__10707 = G__11012;
count__10708 = G__11013;
i__10709 = G__11014;
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
(shadow.remote.runtime.shared.trigger_BANG_.cljs$lang$applyTo = (function (seq10676){
var G__10677 = cljs.core.first(seq10676);
var seq10676__$1 = cljs.core.next(seq10676);
var G__10678 = cljs.core.first(seq10676__$1);
var seq10676__$2 = cljs.core.next(seq10676__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__10677,G__10678,seq10676__$2);
}));

shadow.remote.runtime.shared.welcome = (function shadow$remote$runtime$shared$welcome(p__10782,p__10783){
var map__10784 = p__10782;
var map__10784__$1 = cljs.core.__destructure_map(map__10784);
var runtime = map__10784__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10784__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__10785 = p__10783;
var map__10785__$1 = cljs.core.__destructure_map(map__10785);
var msg = map__10785__$1;
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10785__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state_ref,cljs.core.assoc,new cljs.core.Keyword(null,"client-id","client-id",-464622140),client_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"welcome","welcome",-578152123),true], 0));

var map__10788 = cljs.core.deref(state_ref);
var map__10788__$1 = cljs.core.__destructure_map(map__10788);
var client_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10788__$1,new cljs.core.Keyword(null,"client-info","client-info",1958982504));
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10788__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
shadow.remote.runtime.shared.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"hello","hello",-245025397),new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info], null));

return shadow.remote.runtime.shared.trigger_BANG_(runtime,new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125));
});
shadow.remote.runtime.shared.ping = (function shadow$remote$runtime$shared$ping(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"pong","pong",-172484958)], null));
});
shadow.remote.runtime.shared.request_supported_ops = (function shadow$remote$runtime$shared$request_supported_ops(p__10799,msg){
var map__10803 = p__10799;
var map__10803__$1 = cljs.core.__destructure_map(map__10803);
var runtime = map__10803__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10803__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"supported-ops","supported-ops",337914702),new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.disj.cljs$core$IFn$_invoke$arity$variadic(cljs.core.set(cljs.core.keys(new cljs.core.Keyword(null,"ops","ops",1237330063).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))),new cljs.core.Keyword(null,"welcome","welcome",-578152123),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),new cljs.core.Keyword(null,"tool-disconnect","tool-disconnect",189103996)], 0))], null));
});
shadow.remote.runtime.shared.unknown_relay_op = (function shadow$remote$runtime$shared$unknown_relay_op(msg){
return console.warn("unknown-relay-op",msg);
});
shadow.remote.runtime.shared.unknown_op = (function shadow$remote$runtime$shared$unknown_op(msg){
return console.warn("unknown-op",msg);
});
shadow.remote.runtime.shared.add_extension_STAR_ = (function shadow$remote$runtime$shared$add_extension_STAR_(p__10813,key,p__10814){
var map__10819 = p__10813;
var map__10819__$1 = cljs.core.__destructure_map(map__10819);
var state = map__10819__$1;
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10819__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
var map__10821 = p__10814;
var map__10821__$1 = cljs.core.__destructure_map(map__10821);
var spec = map__10821__$1;
var ops = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10821__$1,new cljs.core.Keyword(null,"ops","ops",1237330063));
var transit_write_handlers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10821__$1,new cljs.core.Keyword(null,"transit-write-handlers","transit-write-handlers",1886308716));
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
shadow.remote.runtime.shared.add_extension = (function shadow$remote$runtime$shared$add_extension(p__10825,key,spec){
var map__10826 = p__10825;
var map__10826__$1 = cljs.core.__destructure_map(map__10826);
var runtime = map__10826__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10826__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(state_ref,shadow.remote.runtime.shared.add_extension_STAR_,key,spec);

var temp__5827__auto___11044 = new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125).cljs$core$IFn$_invoke$arity$1(spec);
if((temp__5827__auto___11044 == null)){
} else {
var on_welcome_11045 = temp__5827__auto___11044;
if(cljs.core.truth_(new cljs.core.Keyword(null,"welcome","welcome",-578152123).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))){
(on_welcome_11045.cljs$core$IFn$_invoke$arity$0 ? on_welcome_11045.cljs$core$IFn$_invoke$arity$0() : on_welcome_11045.call(null));
} else {
}
}

return runtime;
});
shadow.remote.runtime.shared.add_defaults = (function shadow$remote$runtime$shared$add_defaults(runtime){
return shadow.remote.runtime.shared.add_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.shared","defaults","shadow.remote.runtime.shared/defaults",-1821257543),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"welcome","welcome",-578152123),(function (p1__10829_SHARP_){
return shadow.remote.runtime.shared.welcome(runtime,p1__10829_SHARP_);
}),new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),(function (p1__10830_SHARP_){
return shadow.remote.runtime.shared.unknown_relay_op(p1__10830_SHARP_);
}),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),(function (p1__10831_SHARP_){
return shadow.remote.runtime.shared.unknown_op(p1__10831_SHARP_);
}),new cljs.core.Keyword(null,"ping","ping",-1670114784),(function (p1__10832_SHARP_){
return shadow.remote.runtime.shared.ping(runtime,p1__10832_SHARP_);
}),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),(function (p1__10833_SHARP_){
return shadow.remote.runtime.shared.request_supported_ops(runtime,p1__10833_SHARP_);
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
shadow.remote.runtime.shared.del_extension = (function shadow$remote$runtime$shared$del_extension(p__10839,key){
var map__10840 = p__10839;
var map__10840__$1 = cljs.core.__destructure_map(map__10840);
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10840__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(state_ref,shadow.remote.runtime.shared.del_extension_STAR_,key);
});
shadow.remote.runtime.shared.unhandled_call_result = (function shadow$remote$runtime$shared$unhandled_call_result(call_config,msg){
return console.warn("unhandled call result",msg,call_config);
});
shadow.remote.runtime.shared.unhandled_client_not_found = (function shadow$remote$runtime$shared$unhandled_client_not_found(p__10850,msg){
var map__10854 = p__10850;
var map__10854__$1 = cljs.core.__destructure_map(map__10854);
var runtime = map__10854__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10854__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic(runtime,new cljs.core.Keyword(null,"on-client-not-found","on-client-not-found",-642452849),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msg], 0));
});
shadow.remote.runtime.shared.reply_unknown_op = (function shadow$remote$runtime$shared$reply_unknown_op(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg], null));
});
shadow.remote.runtime.shared.process = (function shadow$remote$runtime$shared$process(p__10866,p__10867){
var map__10868 = p__10866;
var map__10868__$1 = cljs.core.__destructure_map(map__10868);
var runtime = map__10868__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10868__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__10871 = p__10867;
var map__10871__$1 = cljs.core.__destructure_map(map__10871);
var msg = map__10871__$1;
var op = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10871__$1,new cljs.core.Keyword(null,"op","op",-1882987955));
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10871__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
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
var seq__10894 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__10896 = null;
var count__10897 = (0);
var i__10898 = (0);
while(true){
if((i__10898 < count__10897)){
var map__10931 = chunk__10896.cljs$core$IIndexed$_nth$arity$2(null,i__10898);
var map__10931__$1 = cljs.core.__destructure_map(map__10931);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10931__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__11076 = seq__10894;
var G__11077 = chunk__10896;
var G__11078 = count__10897;
var G__11079 = (i__10898 + (1));
seq__10894 = G__11076;
chunk__10896 = G__11077;
count__10897 = G__11078;
i__10898 = G__11079;
continue;
} else {
var G__11080 = seq__10894;
var G__11081 = chunk__10896;
var G__11082 = count__10897;
var G__11083 = (i__10898 + (1));
seq__10894 = G__11080;
chunk__10896 = G__11081;
count__10897 = G__11082;
i__10898 = G__11083;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__10894);
if(temp__5823__auto__){
var seq__10894__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__10894__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__10894__$1);
var G__11084 = cljs.core.chunk_rest(seq__10894__$1);
var G__11085 = c__5673__auto__;
var G__11086 = cljs.core.count(c__5673__auto__);
var G__11087 = (0);
seq__10894 = G__11084;
chunk__10896 = G__11085;
count__10897 = G__11086;
i__10898 = G__11087;
continue;
} else {
var map__10935 = cljs.core.first(seq__10894__$1);
var map__10935__$1 = cljs.core.__destructure_map(map__10935);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__10935__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__11089 = cljs.core.next(seq__10894__$1);
var G__11090 = null;
var G__11091 = (0);
var G__11092 = (0);
seq__10894 = G__11089;
chunk__10896 = G__11090;
count__10897 = G__11091;
i__10898 = G__11092;
continue;
} else {
var G__11094 = cljs.core.next(seq__10894__$1);
var G__11095 = null;
var G__11096 = (0);
var G__11097 = (0);
seq__10894 = G__11094;
chunk__10896 = G__11095;
count__10897 = G__11096;
i__10898 = G__11097;
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
