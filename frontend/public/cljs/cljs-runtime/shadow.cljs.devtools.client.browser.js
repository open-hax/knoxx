goog.provide('shadow.cljs.devtools.client.browser');
shadow.cljs.devtools.client.browser.devtools_msg = (function shadow$cljs$devtools$client$browser$devtools_msg(var_args){
var args__5882__auto__ = [];
var len__5876__auto___19377 = arguments.length;
var i__5877__auto___19378 = (0);
while(true){
if((i__5877__auto___19378 < len__5876__auto___19377)){
args__5882__auto__.push((arguments[i__5877__auto___19378]));

var G__19379 = (i__5877__auto___19378 + (1));
i__5877__auto___19378 = G__19379;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic = (function (msg,args){
if(shadow.cljs.devtools.client.env.log){
if(cljs.core.seq(shadow.cljs.devtools.client.env.log_style)){
return console.log.apply(console,cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"%cshadow-cljs: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg)),shadow.cljs.devtools.client.env.log_style], null),args)));
} else {
return console.log.apply(console,cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"shadow-cljs: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg))], null),args)));
}
} else {
return null;
}
}));

(shadow.cljs.devtools.client.browser.devtools_msg.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(shadow.cljs.devtools.client.browser.devtools_msg.cljs$lang$applyTo = (function (seq19089){
var G__19090 = cljs.core.first(seq19089);
var seq19089__$1 = cljs.core.next(seq19089);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__19090,seq19089__$1);
}));

shadow.cljs.devtools.client.browser.script_eval = (function shadow$cljs$devtools$client$browser$script_eval(code){
return goog.globalEval(code);
});
shadow.cljs.devtools.client.browser.do_js_load = (function shadow$cljs$devtools$client$browser$do_js_load(sources){
var seq__19093 = cljs.core.seq(sources);
var chunk__19094 = null;
var count__19095 = (0);
var i__19096 = (0);
while(true){
if((i__19096 < count__19095)){
var map__19101 = chunk__19094.cljs$core$IIndexed$_nth$arity$2(null,i__19096);
var map__19101__$1 = cljs.core.__destructure_map(map__19101);
var src = map__19101__$1;
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19101__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
var output_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19101__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19101__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19101__$1,new cljs.core.Keyword(null,"js","js",1768080579));
$CLJS.SHADOW_ENV.setLoaded(output_name);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load JS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([resource_name], 0));

shadow.cljs.devtools.client.env.before_load_src(src);

try{shadow.cljs.devtools.client.browser.script_eval((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js)+"\n//# sourceURL="+cljs.core.str.cljs$core$IFn$_invoke$arity$1($CLJS.SHADOW_ENV.scriptBase)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(output_name)));
}catch (e19102){var e_19380 = e19102;
if(shadow.cljs.devtools.client.env.log){
console.error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)),e_19380);
} else {
}

throw (new Error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(e_19380.message))));
}

var G__19381 = seq__19093;
var G__19382 = chunk__19094;
var G__19383 = count__19095;
var G__19384 = (i__19096 + (1));
seq__19093 = G__19381;
chunk__19094 = G__19382;
count__19095 = G__19383;
i__19096 = G__19384;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__19093);
if(temp__5823__auto__){
var seq__19093__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__19093__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__19093__$1);
var G__19385 = cljs.core.chunk_rest(seq__19093__$1);
var G__19386 = c__5673__auto__;
var G__19387 = cljs.core.count(c__5673__auto__);
var G__19388 = (0);
seq__19093 = G__19385;
chunk__19094 = G__19386;
count__19095 = G__19387;
i__19096 = G__19388;
continue;
} else {
var map__19103 = cljs.core.first(seq__19093__$1);
var map__19103__$1 = cljs.core.__destructure_map(map__19103);
var src = map__19103__$1;
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19103__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
var output_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19103__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19103__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19103__$1,new cljs.core.Keyword(null,"js","js",1768080579));
$CLJS.SHADOW_ENV.setLoaded(output_name);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load JS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([resource_name], 0));

shadow.cljs.devtools.client.env.before_load_src(src);

try{shadow.cljs.devtools.client.browser.script_eval((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js)+"\n//# sourceURL="+cljs.core.str.cljs$core$IFn$_invoke$arity$1($CLJS.SHADOW_ENV.scriptBase)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(output_name)));
}catch (e19104){var e_19389 = e19104;
if(shadow.cljs.devtools.client.env.log){
console.error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)),e_19389);
} else {
}

throw (new Error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(e_19389.message))));
}

var G__19390 = cljs.core.next(seq__19093__$1);
var G__19391 = null;
var G__19392 = (0);
var G__19393 = (0);
seq__19093 = G__19390;
chunk__19094 = G__19391;
count__19095 = G__19392;
i__19096 = G__19393;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.cljs.devtools.client.browser.do_js_reload = (function shadow$cljs$devtools$client$browser$do_js_reload(msg,sources,complete_fn,failure_fn){
return shadow.cljs.devtools.client.env.do_js_reload.cljs$core$IFn$_invoke$arity$4(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(msg,new cljs.core.Keyword(null,"log-missing-fn","log-missing-fn",732676765),(function (fn_sym){
return null;
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"log-call-async","log-call-async",183826192),(function (fn_sym){
return shadow.cljs.devtools.client.browser.devtools_msg((""+"call async "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym)));
}),new cljs.core.Keyword(null,"log-call","log-call",412404391),(function (fn_sym){
return shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym)));
})], 0)),(function (next){
shadow.cljs.devtools.client.browser.do_js_load(sources);

return (next.cljs$core$IFn$_invoke$arity$0 ? next.cljs$core$IFn$_invoke$arity$0() : next.call(null));
}),complete_fn,failure_fn);
});
/**
 * when (require '["some-str" :as x]) is done at the REPL we need to manually call the shadow.js.require for it
 * since the file only adds the shadow$provide. only need to do this for shadow-js.
 */
shadow.cljs.devtools.client.browser.do_js_requires = (function shadow$cljs$devtools$client$browser$do_js_requires(js_requires){
var seq__19107 = cljs.core.seq(js_requires);
var chunk__19108 = null;
var count__19109 = (0);
var i__19110 = (0);
while(true){
if((i__19110 < count__19109)){
var js_ns = chunk__19108.cljs$core$IIndexed$_nth$arity$2(null,i__19110);
var require_str_19394 = (""+"var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+" = shadow.js.require(\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+"\");");
shadow.cljs.devtools.client.browser.script_eval(require_str_19394);


var G__19395 = seq__19107;
var G__19396 = chunk__19108;
var G__19397 = count__19109;
var G__19398 = (i__19110 + (1));
seq__19107 = G__19395;
chunk__19108 = G__19396;
count__19109 = G__19397;
i__19110 = G__19398;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__19107);
if(temp__5823__auto__){
var seq__19107__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__19107__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__19107__$1);
var G__19399 = cljs.core.chunk_rest(seq__19107__$1);
var G__19400 = c__5673__auto__;
var G__19401 = cljs.core.count(c__5673__auto__);
var G__19402 = (0);
seq__19107 = G__19399;
chunk__19108 = G__19400;
count__19109 = G__19401;
i__19110 = G__19402;
continue;
} else {
var js_ns = cljs.core.first(seq__19107__$1);
var require_str_19403 = (""+"var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+" = shadow.js.require(\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+"\");");
shadow.cljs.devtools.client.browser.script_eval(require_str_19403);


var G__19404 = cljs.core.next(seq__19107__$1);
var G__19405 = null;
var G__19406 = (0);
var G__19407 = (0);
seq__19107 = G__19404;
chunk__19108 = G__19405;
count__19109 = G__19406;
i__19110 = G__19407;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.cljs.devtools.client.browser.handle_build_complete = (function shadow$cljs$devtools$client$browser$handle_build_complete(runtime,p__19112){
var map__19113 = p__19112;
var map__19113__$1 = cljs.core.__destructure_map(map__19113);
var msg = map__19113__$1;
var info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19113__$1,new cljs.core.Keyword(null,"info","info",-317069002));
var reload_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19113__$1,new cljs.core.Keyword(null,"reload-info","reload-info",1648088086));
var warnings = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.distinct.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__19114(s__19115){
return (new cljs.core.LazySeq(null,(function (){
var s__19115__$1 = s__19115;
while(true){
var temp__5823__auto__ = cljs.core.seq(s__19115__$1);
if(temp__5823__auto__){
var xs__6383__auto__ = temp__5823__auto__;
var map__19120 = cljs.core.first(xs__6383__auto__);
var map__19120__$1 = cljs.core.__destructure_map(map__19120);
var src = map__19120__$1;
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19120__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var warnings = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19120__$1,new cljs.core.Keyword(null,"warnings","warnings",-735437651));
if(cljs.core.not(new cljs.core.Keyword(null,"from-jar","from-jar",1050932827).cljs$core$IFn$_invoke$arity$1(src))){
var iterys__5624__auto__ = ((function (s__19115__$1,map__19120,map__19120__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__19113,map__19113__$1,msg,info,reload_info){
return (function shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__19114_$_iter__19116(s__19117){
return (new cljs.core.LazySeq(null,((function (s__19115__$1,map__19120,map__19120__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__19113,map__19113__$1,msg,info,reload_info){
return (function (){
var s__19117__$1 = s__19117;
while(true){
var temp__5823__auto____$1 = cljs.core.seq(s__19117__$1);
if(temp__5823__auto____$1){
var s__19117__$2 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_(s__19117__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__19117__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__19119 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__19118 = (0);
while(true){
if((i__19118 < size__5627__auto__)){
var warning = cljs.core._nth(c__5626__auto__,i__19118);
cljs.core.chunk_append(b__19119,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name));

var G__19408 = (i__19118 + (1));
i__19118 = G__19408;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__19119),shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__19114_$_iter__19116(cljs.core.chunk_rest(s__19117__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__19119),null);
}
} else {
var warning = cljs.core.first(s__19117__$2);
return cljs.core.cons(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name),shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__19114_$_iter__19116(cljs.core.rest(s__19117__$2)));
}
} else {
return null;
}
break;
}
});})(s__19115__$1,map__19120,map__19120__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__19113,map__19113__$1,msg,info,reload_info))
,null,null));
});})(s__19115__$1,map__19120,map__19120__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__19113,map__19113__$1,msg,info,reload_info))
;
var fs__5625__auto__ = cljs.core.seq(iterys__5624__auto__(warnings));
if(fs__5625__auto__){
return cljs.core.concat.cljs$core$IFn$_invoke$arity$2(fs__5625__auto__,shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__19114(cljs.core.rest(s__19115__$1)));
} else {
var G__19409 = cljs.core.rest(s__19115__$1);
s__19115__$1 = G__19409;
continue;
}
} else {
var G__19410 = cljs.core.rest(s__19115__$1);
s__19115__$1 = G__19410;
continue;
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(new cljs.core.Keyword(null,"sources","sources",-321166424).cljs$core$IFn$_invoke$arity$1(info));
})()));
if(shadow.cljs.devtools.client.env.log){
var seq__19121_19411 = cljs.core.seq(warnings);
var chunk__19122_19412 = null;
var count__19123_19413 = (0);
var i__19124_19414 = (0);
while(true){
if((i__19124_19414 < count__19123_19413)){
var map__19127_19415 = chunk__19122_19412.cljs$core$IIndexed$_nth$arity$2(null,i__19124_19414);
var map__19127_19416__$1 = cljs.core.__destructure_map(map__19127_19415);
var w_19417 = map__19127_19416__$1;
var msg_19418__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19127_19416__$1,new cljs.core.Keyword(null,"msg","msg",-1386103444));
var line_19419 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19127_19416__$1,new cljs.core.Keyword(null,"line","line",212345235));
var column_19420 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19127_19416__$1,new cljs.core.Keyword(null,"column","column",2078222095));
var resource_name_19421 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19127_19416__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
console.warn((""+"BUILD-WARNING in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name_19421)+" at ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line_19419)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column_19420)+"]\n\t"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg_19418__$1)));


var G__19422 = seq__19121_19411;
var G__19423 = chunk__19122_19412;
var G__19424 = count__19123_19413;
var G__19425 = (i__19124_19414 + (1));
seq__19121_19411 = G__19422;
chunk__19122_19412 = G__19423;
count__19123_19413 = G__19424;
i__19124_19414 = G__19425;
continue;
} else {
var temp__5823__auto___19426 = cljs.core.seq(seq__19121_19411);
if(temp__5823__auto___19426){
var seq__19121_19427__$1 = temp__5823__auto___19426;
if(cljs.core.chunked_seq_QMARK_(seq__19121_19427__$1)){
var c__5673__auto___19428 = cljs.core.chunk_first(seq__19121_19427__$1);
var G__19429 = cljs.core.chunk_rest(seq__19121_19427__$1);
var G__19430 = c__5673__auto___19428;
var G__19431 = cljs.core.count(c__5673__auto___19428);
var G__19432 = (0);
seq__19121_19411 = G__19429;
chunk__19122_19412 = G__19430;
count__19123_19413 = G__19431;
i__19124_19414 = G__19432;
continue;
} else {
var map__19128_19433 = cljs.core.first(seq__19121_19427__$1);
var map__19128_19434__$1 = cljs.core.__destructure_map(map__19128_19433);
var w_19435 = map__19128_19434__$1;
var msg_19436__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19128_19434__$1,new cljs.core.Keyword(null,"msg","msg",-1386103444));
var line_19437 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19128_19434__$1,new cljs.core.Keyword(null,"line","line",212345235));
var column_19438 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19128_19434__$1,new cljs.core.Keyword(null,"column","column",2078222095));
var resource_name_19439 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19128_19434__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
console.warn((""+"BUILD-WARNING in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name_19439)+" at ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line_19437)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column_19438)+"]\n\t"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg_19436__$1)));


var G__19440 = cljs.core.next(seq__19121_19427__$1);
var G__19441 = null;
var G__19442 = (0);
var G__19443 = (0);
seq__19121_19411 = G__19440;
chunk__19122_19412 = G__19441;
count__19123_19413 = G__19442;
i__19124_19414 = G__19443;
continue;
}
} else {
}
}
break;
}
} else {
}

if((!(shadow.cljs.devtools.client.env.autoload))){
return shadow.cljs.devtools.client.hud.load_end_success();
} else {
if(((cljs.core.empty_QMARK_(warnings)) || (shadow.cljs.devtools.client.env.ignore_warnings))){
var sources_to_get = shadow.cljs.devtools.client.env.filter_reload_sources(info,reload_info);
if(cljs.core.not(cljs.core.seq(sources_to_get))){
return shadow.cljs.devtools.client.hud.load_end_success();
} else {
if(cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"after-load","after-load",-1278503285)], null)))){
} else {
shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("reloading code but no :after-load hooks are configured!",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["https://shadow-cljs.github.io/docs/UsersGuide.html#_lifecycle_hooks"], 0));
}

return shadow.cljs.devtools.client.shared.load_sources(runtime,sources_to_get,(function (p1__19111_SHARP_){
return shadow.cljs.devtools.client.browser.do_js_reload(msg,p1__19111_SHARP_,shadow.cljs.devtools.client.hud.load_end_success,shadow.cljs.devtools.client.hud.load_failure);
}));
}
} else {
return null;
}
}
});
shadow.cljs.devtools.client.browser.page_load_uri = (cljs.core.truth_(goog.global.document)?goog.Uri.parse(document.location.href):null);
shadow.cljs.devtools.client.browser.match_paths = (function shadow$cljs$devtools$client$browser$match_paths(old,new$){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("file",shadow.cljs.devtools.client.browser.page_load_uri.getScheme())){
var rel_new = cljs.core.subs.cljs$core$IFn$_invoke$arity$2(new$,(1));
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(old,rel_new)) || (clojure.string.starts_with_QMARK_(old,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(rel_new)+"?"))))){
return rel_new;
} else {
return null;
}
} else {
var node_uri = goog.Uri.parse(old);
var node_uri_resolved = shadow.cljs.devtools.client.browser.page_load_uri.resolve(node_uri);
var node_abs = node_uri_resolved.getPath();
var and__5140__auto__ = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$1(shadow.cljs.devtools.client.browser.page_load_uri.hasSameDomainAs(node_uri))) || (cljs.core.not(node_uri.hasDomain())));
if(and__5140__auto__){
var and__5140__auto____$1 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(node_abs,new$);
if(and__5140__auto____$1){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var G__19130 = node_uri;
G__19130.setQuery(null);

G__19130.setPath(new$);

return G__19130;
})()));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
}
});
shadow.cljs.devtools.client.browser.handle_asset_update = (function shadow$cljs$devtools$client$browser$handle_asset_update(p__19131){
var map__19132 = p__19131;
var map__19132__$1 = cljs.core.__destructure_map(map__19132);
var msg = map__19132__$1;
var updates = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19132__$1,new cljs.core.Keyword(null,"updates","updates",2013983452));
var reload_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19132__$1,new cljs.core.Keyword(null,"reload-info","reload-info",1648088086));
var seq__19133 = cljs.core.seq(updates);
var chunk__19135 = null;
var count__19136 = (0);
var i__19137 = (0);
while(true){
if((i__19137 < count__19136)){
var path = chunk__19135.cljs$core$IIndexed$_nth$arity$2(null,i__19137);
if(clojure.string.ends_with_QMARK_(path,"css")){
var seq__19247_19444 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(document.querySelectorAll("link[rel=\"stylesheet\"]")));
var chunk__19251_19445 = null;
var count__19252_19446 = (0);
var i__19253_19447 = (0);
while(true){
if((i__19253_19447 < count__19252_19446)){
var node_19448 = chunk__19251_19445.cljs$core$IIndexed$_nth$arity$2(null,i__19253_19447);
if(cljs.core.not(node_19448.shadow$old)){
var path_match_19449 = shadow.cljs.devtools.client.browser.match_paths(node_19448.getAttribute("href"),path);
if(cljs.core.truth_(path_match_19449)){
var new_link_19450 = (function (){var G__19279 = node_19448.cloneNode(true);
G__19279.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_19449)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__19279;
})();
(node_19448.shadow$old = true);

(new_link_19450.onload = ((function (seq__19247_19444,chunk__19251_19445,count__19252_19446,i__19253_19447,seq__19133,chunk__19135,count__19136,i__19137,new_link_19450,path_match_19449,node_19448,path,map__19132,map__19132__$1,msg,updates,reload_info){
return (function (e){
var seq__19280_19451 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__19282_19452 = null;
var count__19283_19453 = (0);
var i__19284_19454 = (0);
while(true){
if((i__19284_19454 < count__19283_19453)){
var map__19288_19455 = chunk__19282_19452.cljs$core$IIndexed$_nth$arity$2(null,i__19284_19454);
var map__19288_19456__$1 = cljs.core.__destructure_map(map__19288_19455);
var task_19457 = map__19288_19456__$1;
var fn_str_19458 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19288_19456__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19459 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19288_19456__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19460 = goog.getObjectByName(fn_str_19458,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19459)));

(fn_obj_19460.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19460.cljs$core$IFn$_invoke$arity$2(path,new_link_19450) : fn_obj_19460.call(null,path,new_link_19450));


var G__19461 = seq__19280_19451;
var G__19462 = chunk__19282_19452;
var G__19463 = count__19283_19453;
var G__19464 = (i__19284_19454 + (1));
seq__19280_19451 = G__19461;
chunk__19282_19452 = G__19462;
count__19283_19453 = G__19463;
i__19284_19454 = G__19464;
continue;
} else {
var temp__5823__auto___19465 = cljs.core.seq(seq__19280_19451);
if(temp__5823__auto___19465){
var seq__19280_19466__$1 = temp__5823__auto___19465;
if(cljs.core.chunked_seq_QMARK_(seq__19280_19466__$1)){
var c__5673__auto___19467 = cljs.core.chunk_first(seq__19280_19466__$1);
var G__19468 = cljs.core.chunk_rest(seq__19280_19466__$1);
var G__19469 = c__5673__auto___19467;
var G__19470 = cljs.core.count(c__5673__auto___19467);
var G__19471 = (0);
seq__19280_19451 = G__19468;
chunk__19282_19452 = G__19469;
count__19283_19453 = G__19470;
i__19284_19454 = G__19471;
continue;
} else {
var map__19289_19472 = cljs.core.first(seq__19280_19466__$1);
var map__19289_19473__$1 = cljs.core.__destructure_map(map__19289_19472);
var task_19474 = map__19289_19473__$1;
var fn_str_19475 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19289_19473__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19476 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19289_19473__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19477 = goog.getObjectByName(fn_str_19475,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19476)));

(fn_obj_19477.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19477.cljs$core$IFn$_invoke$arity$2(path,new_link_19450) : fn_obj_19477.call(null,path,new_link_19450));


var G__19478 = cljs.core.next(seq__19280_19466__$1);
var G__19479 = null;
var G__19480 = (0);
var G__19481 = (0);
seq__19280_19451 = G__19478;
chunk__19282_19452 = G__19479;
count__19283_19453 = G__19480;
i__19284_19454 = G__19481;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_19448);
});})(seq__19247_19444,chunk__19251_19445,count__19252_19446,i__19253_19447,seq__19133,chunk__19135,count__19136,i__19137,new_link_19450,path_match_19449,node_19448,path,map__19132,map__19132__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_19449], 0));

goog.dom.insertSiblingAfter(new_link_19450,node_19448);


var G__19482 = seq__19247_19444;
var G__19483 = chunk__19251_19445;
var G__19484 = count__19252_19446;
var G__19485 = (i__19253_19447 + (1));
seq__19247_19444 = G__19482;
chunk__19251_19445 = G__19483;
count__19252_19446 = G__19484;
i__19253_19447 = G__19485;
continue;
} else {
var G__19486 = seq__19247_19444;
var G__19487 = chunk__19251_19445;
var G__19488 = count__19252_19446;
var G__19489 = (i__19253_19447 + (1));
seq__19247_19444 = G__19486;
chunk__19251_19445 = G__19487;
count__19252_19446 = G__19488;
i__19253_19447 = G__19489;
continue;
}
} else {
var G__19490 = seq__19247_19444;
var G__19491 = chunk__19251_19445;
var G__19492 = count__19252_19446;
var G__19493 = (i__19253_19447 + (1));
seq__19247_19444 = G__19490;
chunk__19251_19445 = G__19491;
count__19252_19446 = G__19492;
i__19253_19447 = G__19493;
continue;
}
} else {
var temp__5823__auto___19494 = cljs.core.seq(seq__19247_19444);
if(temp__5823__auto___19494){
var seq__19247_19495__$1 = temp__5823__auto___19494;
if(cljs.core.chunked_seq_QMARK_(seq__19247_19495__$1)){
var c__5673__auto___19496 = cljs.core.chunk_first(seq__19247_19495__$1);
var G__19497 = cljs.core.chunk_rest(seq__19247_19495__$1);
var G__19498 = c__5673__auto___19496;
var G__19499 = cljs.core.count(c__5673__auto___19496);
var G__19500 = (0);
seq__19247_19444 = G__19497;
chunk__19251_19445 = G__19498;
count__19252_19446 = G__19499;
i__19253_19447 = G__19500;
continue;
} else {
var node_19501 = cljs.core.first(seq__19247_19495__$1);
if(cljs.core.not(node_19501.shadow$old)){
var path_match_19502 = shadow.cljs.devtools.client.browser.match_paths(node_19501.getAttribute("href"),path);
if(cljs.core.truth_(path_match_19502)){
var new_link_19503 = (function (){var G__19290 = node_19501.cloneNode(true);
G__19290.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_19502)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__19290;
})();
(node_19501.shadow$old = true);

(new_link_19503.onload = ((function (seq__19247_19444,chunk__19251_19445,count__19252_19446,i__19253_19447,seq__19133,chunk__19135,count__19136,i__19137,new_link_19503,path_match_19502,node_19501,seq__19247_19495__$1,temp__5823__auto___19494,path,map__19132,map__19132__$1,msg,updates,reload_info){
return (function (e){
var seq__19291_19504 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__19293_19505 = null;
var count__19294_19506 = (0);
var i__19295_19507 = (0);
while(true){
if((i__19295_19507 < count__19294_19506)){
var map__19299_19508 = chunk__19293_19505.cljs$core$IIndexed$_nth$arity$2(null,i__19295_19507);
var map__19299_19509__$1 = cljs.core.__destructure_map(map__19299_19508);
var task_19510 = map__19299_19509__$1;
var fn_str_19511 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19299_19509__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19512 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19299_19509__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19513 = goog.getObjectByName(fn_str_19511,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19512)));

(fn_obj_19513.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19513.cljs$core$IFn$_invoke$arity$2(path,new_link_19503) : fn_obj_19513.call(null,path,new_link_19503));


var G__19514 = seq__19291_19504;
var G__19515 = chunk__19293_19505;
var G__19516 = count__19294_19506;
var G__19517 = (i__19295_19507 + (1));
seq__19291_19504 = G__19514;
chunk__19293_19505 = G__19515;
count__19294_19506 = G__19516;
i__19295_19507 = G__19517;
continue;
} else {
var temp__5823__auto___19518__$1 = cljs.core.seq(seq__19291_19504);
if(temp__5823__auto___19518__$1){
var seq__19291_19519__$1 = temp__5823__auto___19518__$1;
if(cljs.core.chunked_seq_QMARK_(seq__19291_19519__$1)){
var c__5673__auto___19520 = cljs.core.chunk_first(seq__19291_19519__$1);
var G__19521 = cljs.core.chunk_rest(seq__19291_19519__$1);
var G__19522 = c__5673__auto___19520;
var G__19523 = cljs.core.count(c__5673__auto___19520);
var G__19524 = (0);
seq__19291_19504 = G__19521;
chunk__19293_19505 = G__19522;
count__19294_19506 = G__19523;
i__19295_19507 = G__19524;
continue;
} else {
var map__19300_19525 = cljs.core.first(seq__19291_19519__$1);
var map__19300_19526__$1 = cljs.core.__destructure_map(map__19300_19525);
var task_19527 = map__19300_19526__$1;
var fn_str_19528 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19300_19526__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19529 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19300_19526__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19530 = goog.getObjectByName(fn_str_19528,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19529)));

(fn_obj_19530.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19530.cljs$core$IFn$_invoke$arity$2(path,new_link_19503) : fn_obj_19530.call(null,path,new_link_19503));


var G__19531 = cljs.core.next(seq__19291_19519__$1);
var G__19532 = null;
var G__19533 = (0);
var G__19534 = (0);
seq__19291_19504 = G__19531;
chunk__19293_19505 = G__19532;
count__19294_19506 = G__19533;
i__19295_19507 = G__19534;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_19501);
});})(seq__19247_19444,chunk__19251_19445,count__19252_19446,i__19253_19447,seq__19133,chunk__19135,count__19136,i__19137,new_link_19503,path_match_19502,node_19501,seq__19247_19495__$1,temp__5823__auto___19494,path,map__19132,map__19132__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_19502], 0));

goog.dom.insertSiblingAfter(new_link_19503,node_19501);


var G__19535 = cljs.core.next(seq__19247_19495__$1);
var G__19536 = null;
var G__19537 = (0);
var G__19538 = (0);
seq__19247_19444 = G__19535;
chunk__19251_19445 = G__19536;
count__19252_19446 = G__19537;
i__19253_19447 = G__19538;
continue;
} else {
var G__19539 = cljs.core.next(seq__19247_19495__$1);
var G__19540 = null;
var G__19541 = (0);
var G__19542 = (0);
seq__19247_19444 = G__19539;
chunk__19251_19445 = G__19540;
count__19252_19446 = G__19541;
i__19253_19447 = G__19542;
continue;
}
} else {
var G__19543 = cljs.core.next(seq__19247_19495__$1);
var G__19544 = null;
var G__19545 = (0);
var G__19546 = (0);
seq__19247_19444 = G__19543;
chunk__19251_19445 = G__19544;
count__19252_19446 = G__19545;
i__19253_19447 = G__19546;
continue;
}
}
} else {
}
}
break;
}


var G__19547 = seq__19133;
var G__19548 = chunk__19135;
var G__19549 = count__19136;
var G__19550 = (i__19137 + (1));
seq__19133 = G__19547;
chunk__19135 = G__19548;
count__19136 = G__19549;
i__19137 = G__19550;
continue;
} else {
var G__19551 = seq__19133;
var G__19552 = chunk__19135;
var G__19553 = count__19136;
var G__19554 = (i__19137 + (1));
seq__19133 = G__19551;
chunk__19135 = G__19552;
count__19136 = G__19553;
i__19137 = G__19554;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__19133);
if(temp__5823__auto__){
var seq__19133__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__19133__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__19133__$1);
var G__19555 = cljs.core.chunk_rest(seq__19133__$1);
var G__19556 = c__5673__auto__;
var G__19557 = cljs.core.count(c__5673__auto__);
var G__19558 = (0);
seq__19133 = G__19555;
chunk__19135 = G__19556;
count__19136 = G__19557;
i__19137 = G__19558;
continue;
} else {
var path = cljs.core.first(seq__19133__$1);
if(clojure.string.ends_with_QMARK_(path,"css")){
var seq__19301_19559 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(document.querySelectorAll("link[rel=\"stylesheet\"]")));
var chunk__19305_19560 = null;
var count__19306_19561 = (0);
var i__19307_19562 = (0);
while(true){
if((i__19307_19562 < count__19306_19561)){
var node_19563 = chunk__19305_19560.cljs$core$IIndexed$_nth$arity$2(null,i__19307_19562);
if(cljs.core.not(node_19563.shadow$old)){
var path_match_19564 = shadow.cljs.devtools.client.browser.match_paths(node_19563.getAttribute("href"),path);
if(cljs.core.truth_(path_match_19564)){
var new_link_19565 = (function (){var G__19333 = node_19563.cloneNode(true);
G__19333.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_19564)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__19333;
})();
(node_19563.shadow$old = true);

(new_link_19565.onload = ((function (seq__19301_19559,chunk__19305_19560,count__19306_19561,i__19307_19562,seq__19133,chunk__19135,count__19136,i__19137,new_link_19565,path_match_19564,node_19563,path,seq__19133__$1,temp__5823__auto__,map__19132,map__19132__$1,msg,updates,reload_info){
return (function (e){
var seq__19334_19566 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__19336_19567 = null;
var count__19337_19568 = (0);
var i__19338_19569 = (0);
while(true){
if((i__19338_19569 < count__19337_19568)){
var map__19342_19570 = chunk__19336_19567.cljs$core$IIndexed$_nth$arity$2(null,i__19338_19569);
var map__19342_19571__$1 = cljs.core.__destructure_map(map__19342_19570);
var task_19572 = map__19342_19571__$1;
var fn_str_19573 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19342_19571__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19574 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19342_19571__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19575 = goog.getObjectByName(fn_str_19573,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19574)));

(fn_obj_19575.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19575.cljs$core$IFn$_invoke$arity$2(path,new_link_19565) : fn_obj_19575.call(null,path,new_link_19565));


var G__19576 = seq__19334_19566;
var G__19577 = chunk__19336_19567;
var G__19578 = count__19337_19568;
var G__19579 = (i__19338_19569 + (1));
seq__19334_19566 = G__19576;
chunk__19336_19567 = G__19577;
count__19337_19568 = G__19578;
i__19338_19569 = G__19579;
continue;
} else {
var temp__5823__auto___19580__$1 = cljs.core.seq(seq__19334_19566);
if(temp__5823__auto___19580__$1){
var seq__19334_19581__$1 = temp__5823__auto___19580__$1;
if(cljs.core.chunked_seq_QMARK_(seq__19334_19581__$1)){
var c__5673__auto___19582 = cljs.core.chunk_first(seq__19334_19581__$1);
var G__19583 = cljs.core.chunk_rest(seq__19334_19581__$1);
var G__19584 = c__5673__auto___19582;
var G__19585 = cljs.core.count(c__5673__auto___19582);
var G__19586 = (0);
seq__19334_19566 = G__19583;
chunk__19336_19567 = G__19584;
count__19337_19568 = G__19585;
i__19338_19569 = G__19586;
continue;
} else {
var map__19343_19587 = cljs.core.first(seq__19334_19581__$1);
var map__19343_19588__$1 = cljs.core.__destructure_map(map__19343_19587);
var task_19589 = map__19343_19588__$1;
var fn_str_19590 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19343_19588__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19591 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19343_19588__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19592 = goog.getObjectByName(fn_str_19590,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19591)));

(fn_obj_19592.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19592.cljs$core$IFn$_invoke$arity$2(path,new_link_19565) : fn_obj_19592.call(null,path,new_link_19565));


var G__19593 = cljs.core.next(seq__19334_19581__$1);
var G__19594 = null;
var G__19595 = (0);
var G__19596 = (0);
seq__19334_19566 = G__19593;
chunk__19336_19567 = G__19594;
count__19337_19568 = G__19595;
i__19338_19569 = G__19596;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_19563);
});})(seq__19301_19559,chunk__19305_19560,count__19306_19561,i__19307_19562,seq__19133,chunk__19135,count__19136,i__19137,new_link_19565,path_match_19564,node_19563,path,seq__19133__$1,temp__5823__auto__,map__19132,map__19132__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_19564], 0));

goog.dom.insertSiblingAfter(new_link_19565,node_19563);


var G__19597 = seq__19301_19559;
var G__19598 = chunk__19305_19560;
var G__19599 = count__19306_19561;
var G__19600 = (i__19307_19562 + (1));
seq__19301_19559 = G__19597;
chunk__19305_19560 = G__19598;
count__19306_19561 = G__19599;
i__19307_19562 = G__19600;
continue;
} else {
var G__19601 = seq__19301_19559;
var G__19602 = chunk__19305_19560;
var G__19603 = count__19306_19561;
var G__19604 = (i__19307_19562 + (1));
seq__19301_19559 = G__19601;
chunk__19305_19560 = G__19602;
count__19306_19561 = G__19603;
i__19307_19562 = G__19604;
continue;
}
} else {
var G__19605 = seq__19301_19559;
var G__19606 = chunk__19305_19560;
var G__19607 = count__19306_19561;
var G__19608 = (i__19307_19562 + (1));
seq__19301_19559 = G__19605;
chunk__19305_19560 = G__19606;
count__19306_19561 = G__19607;
i__19307_19562 = G__19608;
continue;
}
} else {
var temp__5823__auto___19609__$1 = cljs.core.seq(seq__19301_19559);
if(temp__5823__auto___19609__$1){
var seq__19301_19610__$1 = temp__5823__auto___19609__$1;
if(cljs.core.chunked_seq_QMARK_(seq__19301_19610__$1)){
var c__5673__auto___19611 = cljs.core.chunk_first(seq__19301_19610__$1);
var G__19612 = cljs.core.chunk_rest(seq__19301_19610__$1);
var G__19613 = c__5673__auto___19611;
var G__19614 = cljs.core.count(c__5673__auto___19611);
var G__19615 = (0);
seq__19301_19559 = G__19612;
chunk__19305_19560 = G__19613;
count__19306_19561 = G__19614;
i__19307_19562 = G__19615;
continue;
} else {
var node_19616 = cljs.core.first(seq__19301_19610__$1);
if(cljs.core.not(node_19616.shadow$old)){
var path_match_19617 = shadow.cljs.devtools.client.browser.match_paths(node_19616.getAttribute("href"),path);
if(cljs.core.truth_(path_match_19617)){
var new_link_19618 = (function (){var G__19344 = node_19616.cloneNode(true);
G__19344.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_19617)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__19344;
})();
(node_19616.shadow$old = true);

(new_link_19618.onload = ((function (seq__19301_19559,chunk__19305_19560,count__19306_19561,i__19307_19562,seq__19133,chunk__19135,count__19136,i__19137,new_link_19618,path_match_19617,node_19616,seq__19301_19610__$1,temp__5823__auto___19609__$1,path,seq__19133__$1,temp__5823__auto__,map__19132,map__19132__$1,msg,updates,reload_info){
return (function (e){
var seq__19345_19619 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__19347_19620 = null;
var count__19348_19621 = (0);
var i__19349_19622 = (0);
while(true){
if((i__19349_19622 < count__19348_19621)){
var map__19353_19623 = chunk__19347_19620.cljs$core$IIndexed$_nth$arity$2(null,i__19349_19622);
var map__19353_19624__$1 = cljs.core.__destructure_map(map__19353_19623);
var task_19625 = map__19353_19624__$1;
var fn_str_19626 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19353_19624__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19627 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19353_19624__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19628 = goog.getObjectByName(fn_str_19626,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19627)));

(fn_obj_19628.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19628.cljs$core$IFn$_invoke$arity$2(path,new_link_19618) : fn_obj_19628.call(null,path,new_link_19618));


var G__19629 = seq__19345_19619;
var G__19630 = chunk__19347_19620;
var G__19631 = count__19348_19621;
var G__19632 = (i__19349_19622 + (1));
seq__19345_19619 = G__19629;
chunk__19347_19620 = G__19630;
count__19348_19621 = G__19631;
i__19349_19622 = G__19632;
continue;
} else {
var temp__5823__auto___19633__$2 = cljs.core.seq(seq__19345_19619);
if(temp__5823__auto___19633__$2){
var seq__19345_19634__$1 = temp__5823__auto___19633__$2;
if(cljs.core.chunked_seq_QMARK_(seq__19345_19634__$1)){
var c__5673__auto___19635 = cljs.core.chunk_first(seq__19345_19634__$1);
var G__19636 = cljs.core.chunk_rest(seq__19345_19634__$1);
var G__19637 = c__5673__auto___19635;
var G__19638 = cljs.core.count(c__5673__auto___19635);
var G__19639 = (0);
seq__19345_19619 = G__19636;
chunk__19347_19620 = G__19637;
count__19348_19621 = G__19638;
i__19349_19622 = G__19639;
continue;
} else {
var map__19354_19640 = cljs.core.first(seq__19345_19634__$1);
var map__19354_19641__$1 = cljs.core.__destructure_map(map__19354_19640);
var task_19642 = map__19354_19641__$1;
var fn_str_19643 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19354_19641__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_19644 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19354_19641__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_19645 = goog.getObjectByName(fn_str_19643,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_19644)));

(fn_obj_19645.cljs$core$IFn$_invoke$arity$2 ? fn_obj_19645.cljs$core$IFn$_invoke$arity$2(path,new_link_19618) : fn_obj_19645.call(null,path,new_link_19618));


var G__19646 = cljs.core.next(seq__19345_19634__$1);
var G__19647 = null;
var G__19648 = (0);
var G__19649 = (0);
seq__19345_19619 = G__19646;
chunk__19347_19620 = G__19647;
count__19348_19621 = G__19648;
i__19349_19622 = G__19649;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_19616);
});})(seq__19301_19559,chunk__19305_19560,count__19306_19561,i__19307_19562,seq__19133,chunk__19135,count__19136,i__19137,new_link_19618,path_match_19617,node_19616,seq__19301_19610__$1,temp__5823__auto___19609__$1,path,seq__19133__$1,temp__5823__auto__,map__19132,map__19132__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_19617], 0));

goog.dom.insertSiblingAfter(new_link_19618,node_19616);


var G__19650 = cljs.core.next(seq__19301_19610__$1);
var G__19651 = null;
var G__19652 = (0);
var G__19653 = (0);
seq__19301_19559 = G__19650;
chunk__19305_19560 = G__19651;
count__19306_19561 = G__19652;
i__19307_19562 = G__19653;
continue;
} else {
var G__19654 = cljs.core.next(seq__19301_19610__$1);
var G__19655 = null;
var G__19656 = (0);
var G__19657 = (0);
seq__19301_19559 = G__19654;
chunk__19305_19560 = G__19655;
count__19306_19561 = G__19656;
i__19307_19562 = G__19657;
continue;
}
} else {
var G__19658 = cljs.core.next(seq__19301_19610__$1);
var G__19659 = null;
var G__19660 = (0);
var G__19661 = (0);
seq__19301_19559 = G__19658;
chunk__19305_19560 = G__19659;
count__19306_19561 = G__19660;
i__19307_19562 = G__19661;
continue;
}
}
} else {
}
}
break;
}


var G__19662 = cljs.core.next(seq__19133__$1);
var G__19663 = null;
var G__19664 = (0);
var G__19665 = (0);
seq__19133 = G__19662;
chunk__19135 = G__19663;
count__19136 = G__19664;
i__19137 = G__19665;
continue;
} else {
var G__19666 = cljs.core.next(seq__19133__$1);
var G__19667 = null;
var G__19668 = (0);
var G__19669 = (0);
seq__19133 = G__19666;
chunk__19135 = G__19667;
count__19136 = G__19668;
i__19137 = G__19669;
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
shadow.cljs.devtools.client.browser.global_eval = (function shadow$cljs$devtools$client$browser$global_eval(js){
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2("undefined",typeof(module))){
return eval(js);
} else {
return (0,eval)(js);;
}
});
shadow.cljs.devtools.client.browser.runtime_info = (((typeof SHADOW_CONFIG !== 'undefined'))?shadow.json.to_clj.cljs$core$IFn$_invoke$arity$1(SHADOW_CONFIG):null);
shadow.cljs.devtools.client.browser.client_info = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([shadow.cljs.devtools.client.browser.runtime_info,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"host","host",-1558485167),(cljs.core.truth_(goog.global.document)?new cljs.core.Keyword(null,"browser","browser",828191719):new cljs.core.Keyword(null,"browser-worker","browser-worker",1638998282)),new cljs.core.Keyword(null,"user-agent","user-agent",1220426212),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(goog.userAgent.OPERA)?"Opera":(cljs.core.truth_(goog.userAgent.product.CHROME)?"Chrome":(cljs.core.truth_(goog.userAgent.IE)?"MSIE":(cljs.core.truth_(goog.userAgent.EDGE)?"Edge":(cljs.core.truth_(goog.userAgent.GECKO)?"Firefox":(cljs.core.truth_(goog.userAgent.SAFARI)?"Safari":(cljs.core.truth_(goog.userAgent.WEBKIT)?"Webkit":null))))))))+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(goog.userAgent.VERSION)+" ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(goog.userAgent.PLATFORM)+"]"),new cljs.core.Keyword(null,"dom","dom",-1236537922),(!((goog.global.document == null)))], null)], 0));
if((typeof shadow !== 'undefined') && (typeof shadow.cljs !== 'undefined') && (typeof shadow.cljs.devtools !== 'undefined') && (typeof shadow.cljs.devtools.client !== 'undefined') && (typeof shadow.cljs.devtools.client.browser !== 'undefined') && (typeof shadow.cljs.devtools.client.browser.ws_was_welcome_ref !== 'undefined')){
} else {
shadow.cljs.devtools.client.browser.ws_was_welcome_ref = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false);
}
if(((shadow.cljs.devtools.client.env.enabled) && ((shadow.cljs.devtools.client.env.worker_client_id > (0))))){
(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$remote$runtime$api$IEvalJS$ = cljs.core.PROTOCOL_SENTINEL);

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$remote$runtime$api$IEvalJS$_js_eval$arity$4 = (function (this$,code,success,fail){
var this$__$1 = this;
try{var G__19356 = shadow.cljs.devtools.client.browser.global_eval(code);
return (success.cljs$core$IFn$_invoke$arity$1 ? success.cljs$core$IFn$_invoke$arity$1(G__19356) : success.call(null,G__19356));
}catch (e19355){var e = e19355;
return (fail.cljs$core$IFn$_invoke$arity$1 ? fail.cljs$core$IFn$_invoke$arity$1(e) : fail.call(null,e));
}}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$ = cljs.core.PROTOCOL_SENTINEL);

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_invoke$arity$5 = (function (this$,ns,p__19357,success,fail){
var map__19358 = p__19357;
var map__19358__$1 = cljs.core.__destructure_map(map__19358);
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19358__$1,new cljs.core.Keyword(null,"js","js",1768080579));
var this$__$1 = this;
try{var G__19360 = shadow.cljs.devtools.client.browser.global_eval(js);
return (success.cljs$core$IFn$_invoke$arity$1 ? success.cljs$core$IFn$_invoke$arity$1(G__19360) : success.call(null,G__19360));
}catch (e19359){var e = e19359;
return (fail.cljs$core$IFn$_invoke$arity$1 ? fail.cljs$core$IFn$_invoke$arity$1(e) : fail.call(null,e));
}}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_repl_init$arity$4 = (function (runtime,p__19361,done,error){
var map__19362 = p__19361;
var map__19362__$1 = cljs.core.__destructure_map(map__19362);
var repl_sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19362__$1,new cljs.core.Keyword(null,"repl-sources","repl-sources",723867535));
var runtime__$1 = this;
return shadow.cljs.devtools.client.shared.load_sources(runtime__$1,cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(shadow.cljs.devtools.client.env.src_is_loaded_QMARK_,repl_sources)),(function (sources){
shadow.cljs.devtools.client.browser.do_js_load(sources);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));
}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_repl_require$arity$4 = (function (runtime,p__19363,done,error){
var map__19364 = p__19363;
var map__19364__$1 = cljs.core.__destructure_map(map__19364);
var msg = map__19364__$1;
var sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19364__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var reload_namespaces = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19364__$1,new cljs.core.Keyword(null,"reload-namespaces","reload-namespaces",250210134));
var js_requires = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19364__$1,new cljs.core.Keyword(null,"js-requires","js-requires",-1311472051));
var runtime__$1 = this;
var sources_to_load = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p__19365){
var map__19366 = p__19365;
var map__19366__$1 = cljs.core.__destructure_map(map__19366);
var src = map__19366__$1;
var provides = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19366__$1,new cljs.core.Keyword(null,"provides","provides",-1634397992));
var and__5140__auto__ = shadow.cljs.devtools.client.env.src_is_loaded_QMARK_(src);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not(cljs.core.some(reload_namespaces,provides));
} else {
return and__5140__auto__;
}
}),sources));
if(cljs.core.not(cljs.core.seq(sources_to_load))){
var G__19367 = cljs.core.PersistentVector.EMPTY;
return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(G__19367) : done.call(null,G__19367));
} else {
return shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$3(runtime__$1,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"cljs-load-sources","cljs-load-sources",-1458295962),new cljs.core.Keyword(null,"to","to",192099007),shadow.cljs.devtools.client.env.worker_client_id,new cljs.core.Keyword(null,"sources","sources",-321166424),cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentVector.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582)),sources_to_load)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"cljs-sources","cljs-sources",31121610),(function (p__19368){
var map__19369 = p__19368;
var map__19369__$1 = cljs.core.__destructure_map(map__19369);
var msg__$1 = map__19369__$1;
var sources__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19369__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
try{shadow.cljs.devtools.client.browser.do_js_load(sources__$1);

if(cljs.core.seq(js_requires)){
shadow.cljs.devtools.client.browser.do_js_requires(js_requires);
} else {
}

return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(sources_to_load) : done.call(null,sources_to_load));
}catch (e19370){var ex = e19370;
return (error.cljs$core$IFn$_invoke$arity$1 ? error.cljs$core$IFn$_invoke$arity$1(ex) : error.call(null,ex));
}})], null));
}
}));

shadow.cljs.devtools.client.shared.add_plugin_BANG_(new cljs.core.Keyword("shadow.cljs.devtools.client.browser","client","shadow.cljs.devtools.client.browser/client",-1461019282),cljs.core.PersistentHashSet.EMPTY,(function (p__19371){
var map__19372 = p__19371;
var map__19372__$1 = cljs.core.__destructure_map(map__19372);
var env = map__19372__$1;
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19372__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var svc = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"runtime","runtime",-1331573996),runtime], null);
shadow.remote.runtime.api.add_extension(runtime,new cljs.core.Keyword("shadow.cljs.devtools.client.browser","client","shadow.cljs.devtools.client.browser/client",-1461019282),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"on-welcome","on-welcome",1895317125),(function (){
cljs.core.reset_BANG_(shadow.cljs.devtools.client.browser.ws_was_welcome_ref,true);

shadow.cljs.devtools.client.hud.connection_error_clear_BANG_();

shadow.cljs.devtools.client.env.patch_goog_BANG_();

return shadow.cljs.devtools.client.browser.devtools_msg((""+"#"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"client-id","client-id",-464622140).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(new cljs.core.Keyword(null,"state-ref","state-ref",2127874952).cljs$core$IFn$_invoke$arity$1(runtime))))+" ready!"));
}),new cljs.core.Keyword(null,"on-disconnect","on-disconnect",-809021814),(function (e){
if(cljs.core.truth_(cljs.core.deref(shadow.cljs.devtools.client.browser.ws_was_welcome_ref))){
shadow.cljs.devtools.client.hud.connection_error("The Websocket connection was closed!");

return cljs.core.reset_BANG_(shadow.cljs.devtools.client.browser.ws_was_welcome_ref,false);
} else {
return null;
}
}),new cljs.core.Keyword(null,"on-reconnect","on-reconnect",1239988702),(function (e){
return shadow.cljs.devtools.client.hud.connection_error("Reconnecting ...");
}),new cljs.core.Keyword(null,"ops","ops",1237330063),new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"access-denied","access-denied",959449406),(function (msg){
cljs.core.reset_BANG_(shadow.cljs.devtools.client.browser.ws_was_welcome_ref,false);

return shadow.cljs.devtools.client.hud.connection_error((""+"Stale Output! Your loaded JS was not produced by the running shadow-cljs instance."+" Is the watch for this build running?"));
}),new cljs.core.Keyword(null,"cljs-asset-update","cljs-asset-update",1224093028),(function (msg){
return shadow.cljs.devtools.client.browser.handle_asset_update(msg);
}),new cljs.core.Keyword(null,"cljs-build-configure","cljs-build-configure",-2089891268),(function (msg){
return null;
}),new cljs.core.Keyword(null,"cljs-build-start","cljs-build-start",-725781241),(function (msg){
shadow.cljs.devtools.client.hud.hud_hide();

shadow.cljs.devtools.client.hud.load_start();

return shadow.cljs.devtools.client.env.run_custom_notify_BANG_(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"build-start","build-start",-959649480)));
}),new cljs.core.Keyword(null,"cljs-build-complete","cljs-build-complete",273626153),(function (msg){
var msg__$1 = shadow.cljs.devtools.client.env.add_warnings_to_info(msg);
shadow.cljs.devtools.client.hud.connection_error_clear_BANG_();

shadow.cljs.devtools.client.hud.hud_warnings(msg__$1);

shadow.cljs.devtools.client.browser.handle_build_complete(runtime,msg__$1);

return shadow.cljs.devtools.client.env.run_custom_notify_BANG_(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg__$1,new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"build-complete","build-complete",-501868472)));
}),new cljs.core.Keyword(null,"cljs-build-failure","cljs-build-failure",1718154990),(function (msg){
shadow.cljs.devtools.client.hud.load_end();

shadow.cljs.devtools.client.hud.hud_error(msg);

return shadow.cljs.devtools.client.env.run_custom_notify_BANG_(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(msg,new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"build-failure","build-failure",-2107487466)));
}),new cljs.core.Keyword("shadow.cljs.devtools.client.env","worker-notify","shadow.cljs.devtools.client.env/worker-notify",-1456820670),(function (p__19373){
var map__19374 = p__19373;
var map__19374__$1 = cljs.core.__destructure_map(map__19374);
var event_op = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19374__$1,new cljs.core.Keyword(null,"event-op","event-op",200358057));
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19374__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"client-disconnect","client-disconnect",640227957),event_op)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(client_id,shadow.cljs.devtools.client.env.worker_client_id)))){
shadow.cljs.devtools.client.hud.connection_error_clear_BANG_();

return shadow.cljs.devtools.client.hud.connection_error("The watch for this build was stopped!");
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"client-connect","client-connect",-1113973888),event_op)){
shadow.cljs.devtools.client.hud.connection_error_clear_BANG_();

return shadow.cljs.devtools.client.hud.connection_error("The watch for this build was restarted. Reload required!");
} else {
return null;
}
}
})], null)], null));

return svc;
}),(function (p__19375){
var map__19376 = p__19375;
var map__19376__$1 = cljs.core.__destructure_map(map__19376);
var svc = map__19376__$1;
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__19376__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
return shadow.remote.runtime.api.del_extension(runtime,new cljs.core.Keyword("shadow.cljs.devtools.client.browser","client","shadow.cljs.devtools.client.browser/client",-1461019282));
}));

shadow.cljs.devtools.client.shared.init_runtime_BANG_(shadow.cljs.devtools.client.browser.client_info,shadow.cljs.devtools.client.websocket.start,shadow.cljs.devtools.client.websocket.send,shadow.cljs.devtools.client.websocket.stop);
} else {
}

//# sourceMappingURL=shadow.cljs.devtools.client.browser.js.map
