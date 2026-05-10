goog.provide('shadow.remote.runtime.tap_support');
shadow.remote.runtime.tap_support.tap_subscribe = (function shadow$remote$runtime$tap_support$tap_subscribe(p__32586,p__32587){
var map__32588 = p__32586;
var map__32588__$1 = cljs.core.__destructure_map(map__32588);
var svc = map__32588__$1;
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32588__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
var obj_support = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32588__$1,new cljs.core.Keyword(null,"obj-support","obj-support",1522559229));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32588__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var map__32589 = p__32587;
var map__32589__$1 = cljs.core.__destructure_map(map__32589);
var msg = map__32589__$1;
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32589__$1,new cljs.core.Keyword(null,"from","from",1815293044));
var summary = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32589__$1,new cljs.core.Keyword(null,"summary","summary",380847952));
var history__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32589__$1,new cljs.core.Keyword(null,"history","history",-247395220));
var num = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__32589__$1,new cljs.core.Keyword(null,"num","num",1985240673),(10));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(subs_ref,cljs.core.assoc,from,msg);

if(cljs.core.truth_(history__$1)){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap-subscribed","tap-subscribed",-1882247432),new cljs.core.Keyword(null,"history","history",-247395220),cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (oid){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"oid","oid",-768692334),oid,new cljs.core.Keyword(null,"summary","summary",380847952),shadow.remote.runtime.obj_support.obj_describe_STAR_(obj_support,oid)], null);
}),shadow.remote.runtime.obj_support.get_tap_history(obj_support,num)))], null));
} else {
return null;
}
});
shadow.remote.runtime.tap_support.tap_unsubscribe = (function shadow$remote$runtime$tap_support$tap_unsubscribe(p__32597,p__32598){
var map__32600 = p__32597;
var map__32600__$1 = cljs.core.__destructure_map(map__32600);
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32600__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
var map__32601 = p__32598;
var map__32601__$1 = cljs.core.__destructure_map(map__32601);
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32601__$1,new cljs.core.Keyword(null,"from","from",1815293044));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(subs_ref,cljs.core.dissoc,from);
});
shadow.remote.runtime.tap_support.request_tap_history = (function shadow$remote$runtime$tap_support$request_tap_history(p__32603,p__32604){
var map__32605 = p__32603;
var map__32605__$1 = cljs.core.__destructure_map(map__32605);
var obj_support = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32605__$1,new cljs.core.Keyword(null,"obj-support","obj-support",1522559229));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32605__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var map__32606 = p__32604;
var map__32606__$1 = cljs.core.__destructure_map(map__32606);
var msg = map__32606__$1;
var num = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__32606__$1,new cljs.core.Keyword(null,"num","num",1985240673),(10));
var tap_ids = shadow.remote.runtime.obj_support.get_tap_history(obj_support,num);
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap-history","tap-history",-282803347),new cljs.core.Keyword(null,"oids","oids",-1580877688),tap_ids], null));
});
shadow.remote.runtime.tap_support.tool_disconnect = (function shadow$remote$runtime$tap_support$tool_disconnect(p__32627,tid){
var map__32628 = p__32627;
var map__32628__$1 = cljs.core.__destructure_map(map__32628);
var svc = map__32628__$1;
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32628__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(subs_ref,cljs.core.dissoc,tid);
});
shadow.remote.runtime.tap_support.start = (function shadow$remote$runtime$tap_support$start(runtime,obj_support){
var subs_ref = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var tap_fn = (function shadow$remote$runtime$tap_support$start_$_runtime_tap(obj){
if((!((obj == null)))){
var oid = shadow.remote.runtime.obj_support.register(obj_support,obj,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.Keyword(null,"tap","tap",-1086702463)], null));
var seq__32640 = cljs.core.seq(cljs.core.deref(subs_ref));
var chunk__32641 = null;
var count__32642 = (0);
var i__32643 = (0);
while(true){
if((i__32643 < count__32642)){
var vec__32672 = chunk__32641.cljs$core$IIndexed$_nth$arity$2(null,i__32643);
var tid = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__32672,(0),null);
var tap_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__32672,(1),null);
shadow.remote.runtime.api.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap","tap",-1086702463),new cljs.core.Keyword(null,"to","to",192099007),tid,new cljs.core.Keyword(null,"oid","oid",-768692334),oid], null));


var G__32809 = seq__32640;
var G__32810 = chunk__32641;
var G__32811 = count__32642;
var G__32812 = (i__32643 + (1));
seq__32640 = G__32809;
chunk__32641 = G__32810;
count__32642 = G__32811;
i__32643 = G__32812;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__32640);
if(temp__5823__auto__){
var seq__32640__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__32640__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__32640__$1);
var G__32813 = cljs.core.chunk_rest(seq__32640__$1);
var G__32814 = c__5673__auto__;
var G__32815 = cljs.core.count(c__5673__auto__);
var G__32816 = (0);
seq__32640 = G__32813;
chunk__32641 = G__32814;
count__32642 = G__32815;
i__32643 = G__32816;
continue;
} else {
var vec__32751 = cljs.core.first(seq__32640__$1);
var tid = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__32751,(0),null);
var tap_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__32751,(1),null);
shadow.remote.runtime.api.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap","tap",-1086702463),new cljs.core.Keyword(null,"to","to",192099007),tid,new cljs.core.Keyword(null,"oid","oid",-768692334),oid], null));


var G__32817 = cljs.core.next(seq__32640__$1);
var G__32818 = null;
var G__32819 = (0);
var G__32820 = (0);
seq__32640 = G__32817;
chunk__32641 = G__32818;
count__32642 = G__32819;
i__32643 = G__32820;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
});
var svc = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime,new cljs.core.Keyword(null,"obj-support","obj-support",1522559229),obj_support,new cljs.core.Keyword(null,"tap-fn","tap-fn",1573556461),tap_fn,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911),subs_ref], null);
shadow.remote.runtime.api.add_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.tap-support","ext","shadow.remote.runtime.tap-support/ext",1019069674),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"tap-subscribe","tap-subscribe",411179050),(function (p1__32629_SHARP_){
return shadow.remote.runtime.tap_support.tap_subscribe(svc,p1__32629_SHARP_);
}),new cljs.core.Keyword(null,"tap-unsubscribe","tap-unsubscribe",1183890755),(function (p1__32630_SHARP_){
return shadow.remote.runtime.tap_support.tap_unsubscribe(svc,p1__32630_SHARP_);
}),new cljs.core.Keyword(null,"request-tap-history","request-tap-history",-670837812),(function (p1__32631_SHARP_){
return shadow.remote.runtime.tap_support.request_tap_history(svc,p1__32631_SHARP_);
})], null),new cljs.core.Keyword(null,"on-tool-disconnect","on-tool-disconnect",693464366),(function (p1__32632_SHARP_){
return shadow.remote.runtime.tap_support.tool_disconnect(svc,p1__32632_SHARP_);
})], null));

cljs.core.add_tap(tap_fn);

return svc;
});
shadow.remote.runtime.tap_support.stop = (function shadow$remote$runtime$tap_support$stop(p__32785){
var map__32786 = p__32785;
var map__32786__$1 = cljs.core.__destructure_map(map__32786);
var svc = map__32786__$1;
var tap_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32786__$1,new cljs.core.Keyword(null,"tap-fn","tap-fn",1573556461));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__32786__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
cljs.core.remove_tap(tap_fn);

return shadow.remote.runtime.api.del_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.tap-support","ext","shadow.remote.runtime.tap-support/ext",1019069674));
});

//# sourceMappingURL=shadow.remote.runtime.tap_support.js.map
