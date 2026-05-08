import "./cljs_env.js";
import "./cljs.core.js";
import "./shadow.remote.runtime.api.js";
import "./shadow.remote.runtime.shared.js";
import "./shadow.remote.runtime.obj_support.js";
goog.provide('shadow.remote.runtime.tap_support');
shadow.remote.runtime.tap_support.tap_subscribe = (function shadow$remote$runtime$tap_support$tap_subscribe(p__26058,p__26059){
var map__26060 = p__26058;
var map__26060__$1 = cljs.core.__destructure_map(map__26060);
var svc = map__26060__$1;
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26060__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
var obj_support = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26060__$1,new cljs.core.Keyword(null,"obj-support","obj-support",1522559229));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26060__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var map__26061 = p__26059;
var map__26061__$1 = cljs.core.__destructure_map(map__26061);
var msg = map__26061__$1;
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26061__$1,new cljs.core.Keyword(null,"from","from",1815293044));
var summary = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26061__$1,new cljs.core.Keyword(null,"summary","summary",380847952));
var history__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26061__$1,new cljs.core.Keyword(null,"history","history",-247395220));
var num = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__26061__$1,new cljs.core.Keyword(null,"num","num",1985240673),(10));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(subs_ref,cljs.core.assoc,from,msg);

if(cljs.core.truth_(history__$1)){
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap-subscribed","tap-subscribed",-1882247432),new cljs.core.Keyword(null,"history","history",-247395220),cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (oid){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"oid","oid",-768692334),oid,new cljs.core.Keyword(null,"summary","summary",380847952),shadow.remote.runtime.obj_support.obj_describe_STAR_(obj_support,oid)], null);
}),shadow.remote.runtime.obj_support.get_tap_history(obj_support,num)))], null));
} else {
return null;
}
});
shadow.remote.runtime.tap_support.tap_unsubscribe = (function shadow$remote$runtime$tap_support$tap_unsubscribe(p__26063,p__26064){
var map__26065 = p__26063;
var map__26065__$1 = cljs.core.__destructure_map(map__26065);
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26065__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
var map__26066 = p__26064;
var map__26066__$1 = cljs.core.__destructure_map(map__26066);
var from = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26066__$1,new cljs.core.Keyword(null,"from","from",1815293044));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(subs_ref,cljs.core.dissoc,from);
});
shadow.remote.runtime.tap_support.request_tap_history = (function shadow$remote$runtime$tap_support$request_tap_history(p__26073,p__26074){
var map__26075 = p__26073;
var map__26075__$1 = cljs.core.__destructure_map(map__26075);
var obj_support = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26075__$1,new cljs.core.Keyword(null,"obj-support","obj-support",1522559229));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26075__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var map__26077 = p__26074;
var map__26077__$1 = cljs.core.__destructure_map(map__26077);
var msg = map__26077__$1;
var num = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__26077__$1,new cljs.core.Keyword(null,"num","num",1985240673),(10));
var tap_ids = shadow.remote.runtime.obj_support.get_tap_history(obj_support,num);
return shadow.remote.runtime.shared.reply(runtime,msg,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap-history","tap-history",-282803347),new cljs.core.Keyword(null,"oids","oids",-1580877688),tap_ids], null));
});
shadow.remote.runtime.tap_support.tool_disconnect = (function shadow$remote$runtime$tap_support$tool_disconnect(p__26081,tid){
var map__26082 = p__26081;
var map__26082__$1 = cljs.core.__destructure_map(map__26082);
var svc = map__26082__$1;
var subs_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26082__$1,new cljs.core.Keyword(null,"subs-ref","subs-ref",-1355989911));
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(subs_ref,cljs.core.dissoc,tid);
});
shadow.remote.runtime.tap_support.start = (function shadow$remote$runtime$tap_support$start(runtime,obj_support){
var subs_ref = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var tap_fn = (function shadow$remote$runtime$tap_support$start_$_runtime_tap(obj){
if((!((obj == null)))){
var oid = shadow.remote.runtime.obj_support.register(obj_support,obj,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"from","from",1815293044),new cljs.core.Keyword(null,"tap","tap",-1086702463)], null));
var seq__26090 = cljs.core.seq(cljs.core.deref(subs_ref));
var chunk__26091 = null;
var count__26092 = (0);
var i__26093 = (0);
while(true){
if((i__26093 < count__26092)){
var vec__26107 = chunk__26091.cljs$core$IIndexed$_nth$arity$2(null,i__26093);
var tid = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26107,(0),null);
var tap_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26107,(1),null);
shadow.remote.runtime.api.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap","tap",-1086702463),new cljs.core.Keyword(null,"to","to",192099007),tid,new cljs.core.Keyword(null,"oid","oid",-768692334),oid], null));


var G__26150 = seq__26090;
var G__26151 = chunk__26091;
var G__26152 = count__26092;
var G__26153 = (i__26093 + (1));
seq__26090 = G__26150;
chunk__26091 = G__26151;
count__26092 = G__26152;
i__26093 = G__26153;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__26090);
if(temp__5825__auto__){
var seq__26090__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__26090__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__26090__$1);
var G__26154 = cljs.core.chunk_rest(seq__26090__$1);
var G__26155 = c__5673__auto__;
var G__26156 = cljs.core.count(c__5673__auto__);
var G__26157 = (0);
seq__26090 = G__26154;
chunk__26091 = G__26155;
count__26092 = G__26156;
i__26093 = G__26157;
continue;
} else {
var vec__26110 = cljs.core.first(seq__26090__$1);
var tid = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26110,(0),null);
var tap_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__26110,(1),null);
shadow.remote.runtime.api.relay_msg(runtime,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"tap","tap",-1086702463),new cljs.core.Keyword(null,"to","to",192099007),tid,new cljs.core.Keyword(null,"oid","oid",-768692334),oid], null));


var G__26158 = cljs.core.next(seq__26090__$1);
var G__26159 = null;
var G__26160 = (0);
var G__26161 = (0);
seq__26090 = G__26158;
chunk__26091 = G__26159;
count__26092 = G__26160;
i__26093 = G__26161;
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
shadow.remote.runtime.api.add_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.tap-support","ext","shadow.remote.runtime.tap-support/ext",1019069674),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"tap-subscribe","tap-subscribe",411179050),(function (p1__26085_SHARP_){
return shadow.remote.runtime.tap_support.tap_subscribe(svc,p1__26085_SHARP_);
}),new cljs.core.Keyword(null,"tap-unsubscribe","tap-unsubscribe",1183890755),(function (p1__26086_SHARP_){
return shadow.remote.runtime.tap_support.tap_unsubscribe(svc,p1__26086_SHARP_);
}),new cljs.core.Keyword(null,"request-tap-history","request-tap-history",-670837812),(function (p1__26087_SHARP_){
return shadow.remote.runtime.tap_support.request_tap_history(svc,p1__26087_SHARP_);
})], null),new cljs.core.Keyword(null,"on-tool-disconnect","on-tool-disconnect",693464366),(function (p1__26088_SHARP_){
return shadow.remote.runtime.tap_support.tool_disconnect(svc,p1__26088_SHARP_);
})], null));

cljs.core.add_tap(tap_fn);

return svc;
});
shadow.remote.runtime.tap_support.stop = (function shadow$remote$runtime$tap_support$stop(p__26124){
var map__26125 = p__26124;
var map__26125__$1 = cljs.core.__destructure_map(map__26125);
var svc = map__26125__$1;
var tap_fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26125__$1,new cljs.core.Keyword(null,"tap-fn","tap-fn",1573556461));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__26125__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
cljs.core.remove_tap(tap_fn);

return shadow.remote.runtime.api.del_extension(runtime,new cljs.core.Keyword("shadow.remote.runtime.tap-support","ext","shadow.remote.runtime.tap-support/ext",1019069674));
});

//# sourceMappingURL=shadow.remote.runtime.tap_support.js.map
