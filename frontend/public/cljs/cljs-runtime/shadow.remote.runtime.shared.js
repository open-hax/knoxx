goog.provide('shadow.remote.runtime.shared');
shadow.remote.runtime.shared.init_state = (function shadow$remote$runtime$shared$init_state(client_info){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"extensions","extensions",-1103629196),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info,new cljs.core.Keyword(null,"call-id-seq","call-id-seq",-1679248218),(0),new cljs.core.Keyword(null,"call-handlers","call-handlers",386605551),cljs.core.PersistentArrayMap.EMPTY], null);
});
shadow.remote.runtime.shared.now = (function shadow$remote$runtime$shared$now(){
return Date.now();
});
shadow.remote.runtime.shared.get_client_id = (function shadow$remote$runtime$shared$get_client_id(p__25241){
var map__25242 = p__25241;
var map__25242__$1 = cljs.core.__destructure_map(map__25242);
var runtime = map__25242__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25242__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var or__5142__auto__ = new cljs.core.Keyword(null,"client-id","client-id",-464622140).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("runtime has no assigned runtime-id",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime], null));
}
});
shadow.remote.runtime.shared.relay_msg = (function shadow$remote$runtime$shared$relay_msg(runtime,msg){
var self_id_25450 = shadow.remote.runtime.shared.get_client_id(runtime);
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"to","to",192099007).cljs$core$IFn$_invoke$arity$1(msg),self_id_25450)){
shadow.remote.runtime.api.relay_msg(runtime,msg);
} else {
Promise.resolve((1)).then((function (){
var G__25261 = runtime;
var G__25262 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"from","from",1815293044),self_id_25450);
return (shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2 ? shadow.remote.runtime.shared.process.cljs$core$IFn$_invoke$arity$2(G__25261,G__25262) : shadow.remote.runtime.shared.process.call(null,G__25261,G__25262));
}));
}

return msg;
});
shadow.remote.runtime.shared.reply = (function shadow$remote$runtime$shared$reply(runtime,p__25268,res){
var map__25269 = p__25268;
var map__25269__$1 = cljs.core.__destructure_map(map__25269);
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25269__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25269__$1,new cljs.core.Keyword(null,"from","from",1815293044));
var res__$1 = (function (){var G__25270 = res;
var G__25270__$1 = (cljs.core.truth_(call_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25270,new cljs.core.Keyword(null,"call-id","call-id",1043012968),call_id):G__25270);
if(cljs.core.truth_(from)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25270__$1,new cljs.core.Keyword(null,"to","to",192099007),from);
} else {
return G__25270__$1;
}
})();
return shadow.remote.runtime.api.relay_msg(runtime,res__$1);
});
shadow.remote.runtime.shared.call = (function shadow$remote$runtime$shared$call(var_args){
var G__25330 = arguments.length;
switch (G__25330) {
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

(shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$4 = (function (p__25335,msg,handlers,timeout_after_ms){
var map__25336 = p__25335;
var map__25336__$1 = cljs.core.__destructure_map(map__25336);
var runtime = map__25336__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25336__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
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
var len__5876__auto___25459 = arguments.length;
var i__5877__auto___25460 = (0);
while(true){
if((i__5877__auto___25460 < len__5876__auto___25459)){
args__5882__auto__.push((arguments[i__5877__auto___25460]));

var G__25462 = (i__5877__auto___25460 + (1));
i__5877__auto___25460 = G__25462;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (p__25340,ev,args){
var map__25341 = p__25340;
var map__25341__$1 = cljs.core.__destructure_map(map__25341);
var runtime = map__25341__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25341__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var seq__25342 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__25345 = null;
var count__25346 = (0);
var i__25347 = (0);
while(true){
if((i__25347 < count__25346)){
var ext = chunk__25345.cljs$core$IIndexed$_nth$arity$2(null,i__25347);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__25463 = seq__25342;
var G__25464 = chunk__25345;
var G__25465 = count__25346;
var G__25466 = (i__25347 + (1));
seq__25342 = G__25463;
chunk__25345 = G__25464;
count__25346 = G__25465;
i__25347 = G__25466;
continue;
} else {
var G__25467 = seq__25342;
var G__25468 = chunk__25345;
var G__25469 = count__25346;
var G__25470 = (i__25347 + (1));
seq__25342 = G__25467;
chunk__25345 = G__25468;
count__25346 = G__25469;
i__25347 = G__25470;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__25342);
if(temp__5823__auto__){
var seq__25342__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__25342__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__25342__$1);
var G__25475 = cljs.core.chunk_rest(seq__25342__$1);
var G__25476 = c__5673__auto__;
var G__25477 = cljs.core.count(c__5673__auto__);
var G__25478 = (0);
seq__25342 = G__25475;
chunk__25345 = G__25476;
count__25346 = G__25477;
i__25347 = G__25478;
continue;
} else {
var ext = cljs.core.first(seq__25342__$1);
var ev_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(ext,ev);
if(cljs.core.truth_(ev_fn)){
cljs.core.apply.cljs$core$IFn$_invoke$arity$2(ev_fn,args);


var G__25479 = cljs.core.next(seq__25342__$1);
var G__25480 = null;
var G__25481 = (0);
var G__25482 = (0);
seq__25342 = G__25479;
chunk__25345 = G__25480;
count__25346 = G__25481;
i__25347 = G__25482;
continue;
} else {
var G__25483 = cljs.core.next(seq__25342__$1);
var G__25484 = null;
var G__25485 = (0);
var G__25486 = (0);
seq__25342 = G__25483;
chunk__25345 = G__25484;
count__25346 = G__25485;
i__25347 = G__25486;
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
(shadow.remote.runtime.shared.trigger_BANG_.cljs$lang$applyTo = (function (seq25337){
var G__25338 = cljs.core.first(seq25337);
var seq25337__$1 = cljs.core.next(seq25337);
var G__25339 = cljs.core.first(seq25337__$1);
var seq25337__$2 = cljs.core.next(seq25337__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__25338,G__25339,seq25337__$2);
}));

shadow.remote.runtime.shared.welcome = (function shadow$remote$runtime$shared$welcome(p__25353,p__25354){
var map__25355 = p__25353;
var map__25355__$1 = cljs.core.__destructure_map(map__25355);
var runtime = map__25355__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25355__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__25356 = p__25354;
var map__25356__$1 = cljs.core.__destructure_map(map__25356);
var msg = map__25356__$1;
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25356__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(state_ref,cljs.core.assoc,new cljs.core.Keyword(null,"client-id","client-id",-464622140),client_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"welcome","welcome",-578152123),true], 0));

var map__25357 = cljs.core.deref(state_ref);
var map__25357__$1 = cljs.core.__destructure_map(map__25357);
var client_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25357__$1,new cljs.core.Keyword(null,"client-info","client-info",1958982504));
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25357__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
shadow.remote.runtime.shared.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"hello","hello",-245025397),new cljs.core.Keyword(null,"client-info","client-info",1958982504),client_info], null));

return shadow.remote.runtime.shared.trigger_BANG_(runtime,new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125));
});
shadow.remote.runtime.shared.ping = (function shadow$remote$runtime$shared$ping(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"pong","pong",-172484958)], null));
});
shadow.remote.runtime.shared.request_supported_ops = (function shadow$remote$runtime$shared$request_supported_ops(p__25358,msg){
var map__25359 = p__25358;
var map__25359__$1 = cljs.core.__destructure_map(map__25359);
var runtime = map__25359__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25359__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"supported-ops","supported-ops",337914702),new cljs.core.Keyword(null,"ops","ops",1237330063),cljs.core.disj.cljs$core$IFn$_invoke$arity$variadic(cljs.core.set(cljs.core.keys(new cljs.core.Keyword(null,"ops","ops",1237330063).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))),new cljs.core.Keyword(null,"welcome","welcome",-578152123),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),new cljs.core.Keyword(null,"tool-disconnect","tool-disconnect",189103996)], 0))], null));
});
shadow.remote.runtime.shared.unknown_relay_op = (function shadow$remote$runtime$shared$unknown_relay_op(msg){
return console.warn("unknown-relay-op",msg);
});
shadow.remote.runtime.shared.unknown_op = (function shadow$remote$runtime$shared$unknown_op(msg){
return console.warn("unknown-op",msg);
});
shadow.remote.runtime.shared.add_extension_STAR_ = (function shadow$remote$runtime$shared$add_extension_STAR_(p__25372,key,p__25373){
var map__25374 = p__25372;
var map__25374__$1 = cljs.core.__destructure_map(map__25374);
var state = map__25374__$1;
var extensions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25374__$1,new cljs.core.Keyword(null,"extensions","extensions",-1103629196));
var map__25375 = p__25373;
var map__25375__$1 = cljs.core.__destructure_map(map__25375);
var spec = map__25375__$1;
var ops = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25375__$1,new cljs.core.Keyword(null,"ops","ops",1237330063));
var transit_write_handlers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25375__$1,new cljs.core.Keyword(null,"transit-write-handlers","transit-write-handlers",1886308716));
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
shadow.remote.runtime.shared.add_extension = (function shadow$remote$runtime$shared$add_extension(p__25376,key,spec){
var map__25380 = p__25376;
var map__25380__$1 = cljs.core.__destructure_map(map__25380);
var runtime = map__25380__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25380__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(state_ref,shadow.remote.runtime.shared.add_extension_STAR_,key,spec);

var temp__5827__auto___25495 = new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125).cljs$core$IFn$_invoke$arity$1(spec);
if((temp__5827__auto___25495 == null)){
} else {
var on_welcome_25496 = temp__5827__auto___25495;
if(cljs.core.truth_(new cljs.core.Keyword(null,"welcome","welcome",-578152123).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref)))){
(on_welcome_25496.cljs$core$IFn$_invoke$arity$0 ? on_welcome_25496.cljs$core$IFn$_invoke$arity$0() : on_welcome_25496.call(null));
} else {
}
}

return runtime;
});
shadow.remote.runtime.shared.add_defaults = (function shadow$remote$runtime$shared$add_defaults(runtime){
return shadow.remote.runtime.shared.add_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.shared","defaults","shadow.remote.runtime.shared/defaults",-1821257543),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"welcome","welcome",-578152123),(function (p1__25382_SHARP_){
return shadow.remote.runtime.shared.welcome(runtime,p1__25382_SHARP_);
}),new cljs.core.Keyword(null,"unknown-relay-op","unknown-relay-op",170832753),(function (p1__25383_SHARP_){
return shadow.remote.runtime.shared.unknown_relay_op(p1__25383_SHARP_);
}),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),(function (p1__25384_SHARP_){
return shadow.remote.runtime.shared.unknown_op(p1__25384_SHARP_);
}),new cljs.core.Keyword(null,"ping","ping",-1670114784),(function (p1__25385_SHARP_){
return shadow.remote.runtime.shared.ping(runtime,p1__25385_SHARP_);
}),new cljs.core.Keyword(null,"request-supported-ops","request-supported-ops",-1034994502),(function (p1__25386_SHARP_){
return shadow.remote.runtime.shared.request_supported_ops(runtime,p1__25386_SHARP_);
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
shadow.remote.runtime.shared.del_extension = (function shadow$remote$runtime$shared$del_extension(p__25391,key){
var map__25392 = p__25391;
var map__25392__$1 = cljs.core.__destructure_map(map__25392);
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25392__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(state_ref,shadow.remote.runtime.shared.del_extension_STAR_,key);
});
shadow.remote.runtime.shared.unhandled_call_result = (function shadow$remote$runtime$shared$unhandled_call_result(call_config,msg){
return console.warn("unhandled call result",msg,call_config);
});
shadow.remote.runtime.shared.unhandled_client_not_found = (function shadow$remote$runtime$shared$unhandled_client_not_found(p__25394,msg){
var map__25398 = p__25394;
var map__25398__$1 = cljs.core.__destructure_map(map__25398);
var runtime = map__25398__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25398__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
return shadow.remote.runtime.shared.trigger_BANG_.cljs$core$IFn$_invoke$arity$variadic(runtime,new cljs.core.Keyword(null,"on-client-not-found","on-client-not-found",-642452849),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([msg], 0));
});
shadow.remote.runtime.shared.reply_unknown_op = (function shadow$remote$runtime$shared$reply_unknown_op(runtime,msg){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"unknown-op","unknown-op",1900385996),new cljs.core.Keyword(null,"msg","msg",-1386103444),msg], null));
});
shadow.remote.runtime.shared.process = (function shadow$remote$runtime$shared$process(p__25403,p__25404){
var map__25406 = p__25403;
var map__25406__$1 = cljs.core.__destructure_map(map__25406);
var runtime = map__25406__$1;
var state_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25406__$1,new cljs.core.Keyword(null,"state-ref","state-ref",2127874952));
var map__25407 = p__25404;
var map__25407__$1 = cljs.core.__destructure_map(map__25407);
var msg = map__25407__$1;
var op = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25407__$1,new cljs.core.Keyword(null,"op","op",-1882987955));
var call_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25407__$1,new cljs.core.Keyword(null,"call-id","call-id",1043012968));
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
var seq__25419 = cljs.core.seq(cljs.core.vals(new cljs.core.Keyword(null,"extensions","extensions",-1103629196).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(state_ref))));
var chunk__25421 = null;
var count__25422 = (0);
var i__25423 = (0);
while(true){
if((i__25423 < count__25422)){
var map__25434 = chunk__25421.cljs$core$IIndexed$_nth$arity$2(null,i__25423);
var map__25434__$1 = cljs.core.__destructure_map(map__25434);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25434__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__25504 = seq__25419;
var G__25505 = chunk__25421;
var G__25506 = count__25422;
var G__25507 = (i__25423 + (1));
seq__25419 = G__25504;
chunk__25421 = G__25505;
count__25422 = G__25506;
i__25423 = G__25507;
continue;
} else {
var G__25508 = seq__25419;
var G__25509 = chunk__25421;
var G__25510 = count__25422;
var G__25511 = (i__25423 + (1));
seq__25419 = G__25508;
chunk__25421 = G__25509;
count__25422 = G__25510;
i__25423 = G__25511;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__25419);
if(temp__5823__auto__){
var seq__25419__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__25419__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__25419__$1);
var G__25513 = cljs.core.chunk_rest(seq__25419__$1);
var G__25514 = c__5673__auto__;
var G__25515 = cljs.core.count(c__5673__auto__);
var G__25516 = (0);
seq__25419 = G__25513;
chunk__25421 = G__25514;
count__25422 = G__25515;
i__25423 = G__25516;
continue;
} else {
var map__25439 = cljs.core.first(seq__25419__$1);
var map__25439__$1 = cljs.core.__destructure_map(map__25439);
var on_idle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25439__$1,new cljs.core.Keyword(null,"on-idle","on-idle",2044706602));
if(cljs.core.truth_(on_idle)){
(on_idle.cljs$core$IFn$_invoke$arity$0 ? on_idle.cljs$core$IFn$_invoke$arity$0() : on_idle.call(null));


var G__25518 = cljs.core.next(seq__25419__$1);
var G__25519 = null;
var G__25520 = (0);
var G__25521 = (0);
seq__25419 = G__25518;
chunk__25421 = G__25519;
count__25422 = G__25520;
i__25423 = G__25521;
continue;
} else {
var G__25522 = cljs.core.next(seq__25419__$1);
var G__25523 = null;
var G__25524 = (0);
var G__25525 = (0);
seq__25419 = G__25522;
chunk__25421 = G__25523;
count__25422 = G__25524;
i__25423 = G__25525;
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
