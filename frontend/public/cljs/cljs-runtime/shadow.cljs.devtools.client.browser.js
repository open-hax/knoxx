goog.provide('shadow.cljs.devtools.client.browser');
shadow.cljs.devtools.client.browser.devtools_msg = (function shadow$cljs$devtools$client$browser$devtools_msg(var_args){
var args__5882__auto__ = [];
var len__5876__auto___33826 = arguments.length;
var i__5877__auto___33827 = (0);
while(true){
if((i__5877__auto___33827 < len__5876__auto___33826)){
args__5882__auto__.push((arguments[i__5877__auto___33827]));

var G__33828 = (i__5877__auto___33827 + (1));
i__5877__auto___33827 = G__33828;
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
(shadow.cljs.devtools.client.browser.devtools_msg.cljs$lang$applyTo = (function (seq33537){
var G__33538 = cljs.core.first(seq33537);
var seq33537__$1 = cljs.core.next(seq33537);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__33538,seq33537__$1);
}));

shadow.cljs.devtools.client.browser.script_eval = (function shadow$cljs$devtools$client$browser$script_eval(code){
return goog.globalEval(code);
});
shadow.cljs.devtools.client.browser.do_js_load = (function shadow$cljs$devtools$client$browser$do_js_load(sources){
var seq__33540 = cljs.core.seq(sources);
var chunk__33541 = null;
var count__33542 = (0);
var i__33543 = (0);
while(true){
if((i__33543 < count__33542)){
var map__33548 = chunk__33541.cljs$core$IIndexed$_nth$arity$2(null,i__33543);
var map__33548__$1 = cljs.core.__destructure_map(map__33548);
var src = map__33548__$1;
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33548__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
var output_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33548__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33548__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33548__$1,new cljs.core.Keyword(null,"js","js",1768080579));
$CLJS.SHADOW_ENV.setLoaded(output_name);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load JS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([resource_name], 0));

shadow.cljs.devtools.client.env.before_load_src(src);

try{shadow.cljs.devtools.client.browser.script_eval((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js)+"\n//# sourceURL="+cljs.core.str.cljs$core$IFn$_invoke$arity$1($CLJS.SHADOW_ENV.scriptBase)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(output_name)));
}catch (e33549){var e_33829 = e33549;
if(shadow.cljs.devtools.client.env.log){
console.error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)),e_33829);
} else {
}

throw (new Error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(e_33829.message))));
}

var G__33830 = seq__33540;
var G__33831 = chunk__33541;
var G__33832 = count__33542;
var G__33833 = (i__33543 + (1));
seq__33540 = G__33830;
chunk__33541 = G__33831;
count__33542 = G__33832;
i__33543 = G__33833;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__33540);
if(temp__5823__auto__){
var seq__33540__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__33540__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__33540__$1);
var G__33834 = cljs.core.chunk_rest(seq__33540__$1);
var G__33835 = c__5673__auto__;
var G__33836 = cljs.core.count(c__5673__auto__);
var G__33837 = (0);
seq__33540 = G__33834;
chunk__33541 = G__33835;
count__33542 = G__33836;
i__33543 = G__33837;
continue;
} else {
var map__33550 = cljs.core.first(seq__33540__$1);
var map__33550__$1 = cljs.core.__destructure_map(map__33550);
var src = map__33550__$1;
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33550__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
var output_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33550__$1,new cljs.core.Keyword(null,"output-name","output-name",-1769107767));
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33550__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33550__$1,new cljs.core.Keyword(null,"js","js",1768080579));
$CLJS.SHADOW_ENV.setLoaded(output_name);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load JS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([resource_name], 0));

shadow.cljs.devtools.client.env.before_load_src(src);

try{shadow.cljs.devtools.client.browser.script_eval((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js)+"\n//# sourceURL="+cljs.core.str.cljs$core$IFn$_invoke$arity$1($CLJS.SHADOW_ENV.scriptBase)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(output_name)));
}catch (e33551){var e_33838 = e33551;
if(shadow.cljs.devtools.client.env.log){
console.error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)),e_33838);
} else {
}

throw (new Error((""+"Failed to load "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(e_33838.message))));
}

var G__33839 = cljs.core.next(seq__33540__$1);
var G__33840 = null;
var G__33841 = (0);
var G__33842 = (0);
seq__33540 = G__33839;
chunk__33541 = G__33840;
count__33542 = G__33841;
i__33543 = G__33842;
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
var seq__33552 = cljs.core.seq(js_requires);
var chunk__33553 = null;
var count__33554 = (0);
var i__33555 = (0);
while(true){
if((i__33555 < count__33554)){
var js_ns = chunk__33553.cljs$core$IIndexed$_nth$arity$2(null,i__33555);
var require_str_33843 = (""+"var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+" = shadow.js.require(\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+"\");");
shadow.cljs.devtools.client.browser.script_eval(require_str_33843);


var G__33844 = seq__33552;
var G__33845 = chunk__33553;
var G__33846 = count__33554;
var G__33847 = (i__33555 + (1));
seq__33552 = G__33844;
chunk__33553 = G__33845;
count__33554 = G__33846;
i__33555 = G__33847;
continue;
} else {
var temp__5823__auto__ = cljs.core.seq(seq__33552);
if(temp__5823__auto__){
var seq__33552__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__33552__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__33552__$1);
var G__33848 = cljs.core.chunk_rest(seq__33552__$1);
var G__33849 = c__5673__auto__;
var G__33850 = cljs.core.count(c__5673__auto__);
var G__33851 = (0);
seq__33552 = G__33848;
chunk__33553 = G__33849;
count__33554 = G__33850;
i__33555 = G__33851;
continue;
} else {
var js_ns = cljs.core.first(seq__33552__$1);
var require_str_33852 = (""+"var "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+" = shadow.js.require(\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(js_ns)+"\");");
shadow.cljs.devtools.client.browser.script_eval(require_str_33852);


var G__33853 = cljs.core.next(seq__33552__$1);
var G__33854 = null;
var G__33855 = (0);
var G__33856 = (0);
seq__33552 = G__33853;
chunk__33553 = G__33854;
count__33554 = G__33855;
i__33555 = G__33856;
continue;
}
} else {
return null;
}
}
break;
}
});
shadow.cljs.devtools.client.browser.handle_build_complete = (function shadow$cljs$devtools$client$browser$handle_build_complete(runtime,p__33557){
var map__33558 = p__33557;
var map__33558__$1 = cljs.core.__destructure_map(map__33558);
var msg = map__33558__$1;
var info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33558__$1,new cljs.core.Keyword(null,"info","info",-317069002));
var reload_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33558__$1,new cljs.core.Keyword(null,"reload-info","reload-info",1648088086));
var warnings = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.distinct.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__33559(s__33560){
return (new cljs.core.LazySeq(null,(function (){
var s__33560__$1 = s__33560;
while(true){
var temp__5823__auto__ = cljs.core.seq(s__33560__$1);
if(temp__5823__auto__){
var xs__6383__auto__ = temp__5823__auto__;
var map__33565 = cljs.core.first(xs__6383__auto__);
var map__33565__$1 = cljs.core.__destructure_map(map__33565);
var src = map__33565__$1;
var resource_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33565__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
var warnings = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33565__$1,new cljs.core.Keyword(null,"warnings","warnings",-735437651));
if(cljs.core.not(new cljs.core.Keyword(null,"from-jar","from-jar",1050932827).cljs$core$IFn$_invoke$arity$1(src))){
var iterys__5624__auto__ = ((function (s__33560__$1,map__33565,map__33565__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__33558,map__33558__$1,msg,info,reload_info){
return (function shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__33559_$_iter__33561(s__33562){
return (new cljs.core.LazySeq(null,((function (s__33560__$1,map__33565,map__33565__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__33558,map__33558__$1,msg,info,reload_info){
return (function (){
var s__33562__$1 = s__33562;
while(true){
var temp__5823__auto____$1 = cljs.core.seq(s__33562__$1);
if(temp__5823__auto____$1){
var s__33562__$2 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_(s__33562__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__33562__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__33564 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__33563 = (0);
while(true){
if((i__33563 < size__5627__auto__)){
var warning = cljs.core._nth(c__5626__auto__,i__33563);
cljs.core.chunk_append(b__33564,cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name));

var G__33857 = (i__33563 + (1));
i__33563 = G__33857;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__33564),shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__33559_$_iter__33561(cljs.core.chunk_rest(s__33562__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__33564),null);
}
} else {
var warning = cljs.core.first(s__33562__$2);
return cljs.core.cons(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(warning,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100),resource_name),shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__33559_$_iter__33561(cljs.core.rest(s__33562__$2)));
}
} else {
return null;
}
break;
}
});})(s__33560__$1,map__33565,map__33565__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__33558,map__33558__$1,msg,info,reload_info))
,null,null));
});})(s__33560__$1,map__33565,map__33565__$1,src,resource_name,warnings,xs__6383__auto__,temp__5823__auto__,map__33558,map__33558__$1,msg,info,reload_info))
;
var fs__5625__auto__ = cljs.core.seq(iterys__5624__auto__(warnings));
if(fs__5625__auto__){
return cljs.core.concat.cljs$core$IFn$_invoke$arity$2(fs__5625__auto__,shadow$cljs$devtools$client$browser$handle_build_complete_$_iter__33559(cljs.core.rest(s__33560__$1)));
} else {
var G__33858 = cljs.core.rest(s__33560__$1);
s__33560__$1 = G__33858;
continue;
}
} else {
var G__33859 = cljs.core.rest(s__33560__$1);
s__33560__$1 = G__33859;
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
var seq__33566_33860 = cljs.core.seq(warnings);
var chunk__33567_33861 = null;
var count__33568_33862 = (0);
var i__33569_33863 = (0);
while(true){
if((i__33569_33863 < count__33568_33862)){
var map__33572_33864 = chunk__33567_33861.cljs$core$IIndexed$_nth$arity$2(null,i__33569_33863);
var map__33572_33865__$1 = cljs.core.__destructure_map(map__33572_33864);
var w_33866 = map__33572_33865__$1;
var msg_33867__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33572_33865__$1,new cljs.core.Keyword(null,"msg","msg",-1386103444));
var line_33868 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33572_33865__$1,new cljs.core.Keyword(null,"line","line",212345235));
var column_33869 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33572_33865__$1,new cljs.core.Keyword(null,"column","column",2078222095));
var resource_name_33870 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33572_33865__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
console.warn((""+"BUILD-WARNING in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name_33870)+" at ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line_33868)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column_33869)+"]\n\t"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg_33867__$1)));


var G__33871 = seq__33566_33860;
var G__33872 = chunk__33567_33861;
var G__33873 = count__33568_33862;
var G__33874 = (i__33569_33863 + (1));
seq__33566_33860 = G__33871;
chunk__33567_33861 = G__33872;
count__33568_33862 = G__33873;
i__33569_33863 = G__33874;
continue;
} else {
var temp__5823__auto___33875 = cljs.core.seq(seq__33566_33860);
if(temp__5823__auto___33875){
var seq__33566_33876__$1 = temp__5823__auto___33875;
if(cljs.core.chunked_seq_QMARK_(seq__33566_33876__$1)){
var c__5673__auto___33877 = cljs.core.chunk_first(seq__33566_33876__$1);
var G__33878 = cljs.core.chunk_rest(seq__33566_33876__$1);
var G__33879 = c__5673__auto___33877;
var G__33880 = cljs.core.count(c__5673__auto___33877);
var G__33881 = (0);
seq__33566_33860 = G__33878;
chunk__33567_33861 = G__33879;
count__33568_33862 = G__33880;
i__33569_33863 = G__33881;
continue;
} else {
var map__33573_33882 = cljs.core.first(seq__33566_33876__$1);
var map__33573_33883__$1 = cljs.core.__destructure_map(map__33573_33882);
var w_33884 = map__33573_33883__$1;
var msg_33885__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33573_33883__$1,new cljs.core.Keyword(null,"msg","msg",-1386103444));
var line_33886 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33573_33883__$1,new cljs.core.Keyword(null,"line","line",212345235));
var column_33887 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33573_33883__$1,new cljs.core.Keyword(null,"column","column",2078222095));
var resource_name_33888 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33573_33883__$1,new cljs.core.Keyword(null,"resource-name","resource-name",2001617100));
console.warn((""+"BUILD-WARNING in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resource_name_33888)+" at ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(line_33886)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column_33887)+"]\n\t"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg_33885__$1)));


var G__33889 = cljs.core.next(seq__33566_33876__$1);
var G__33890 = null;
var G__33891 = (0);
var G__33892 = (0);
seq__33566_33860 = G__33889;
chunk__33567_33861 = G__33890;
count__33568_33862 = G__33891;
i__33569_33863 = G__33892;
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

return shadow.cljs.devtools.client.shared.load_sources(runtime,sources_to_get,(function (p1__33556_SHARP_){
return shadow.cljs.devtools.client.browser.do_js_reload(msg,p1__33556_SHARP_,shadow.cljs.devtools.client.hud.load_end_success,shadow.cljs.devtools.client.hud.load_failure);
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
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var G__33575 = node_uri;
G__33575.setQuery(null);

G__33575.setPath(new$);

return G__33575;
})()));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
}
});
shadow.cljs.devtools.client.browser.handle_asset_update = (function shadow$cljs$devtools$client$browser$handle_asset_update(p__33576){
var map__33577 = p__33576;
var map__33577__$1 = cljs.core.__destructure_map(map__33577);
var msg = map__33577__$1;
var updates = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33577__$1,new cljs.core.Keyword(null,"updates","updates",2013983452));
var reload_info = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33577__$1,new cljs.core.Keyword(null,"reload-info","reload-info",1648088086));
var seq__33578 = cljs.core.seq(updates);
var chunk__33580 = null;
var count__33581 = (0);
var i__33582 = (0);
while(true){
if((i__33582 < count__33581)){
var path = chunk__33580.cljs$core$IIndexed$_nth$arity$2(null,i__33582);
if(clojure.string.ends_with_QMARK_(path,"css")){
var seq__33692_33893 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(document.querySelectorAll("link[rel=\"stylesheet\"]")));
var chunk__33696_33894 = null;
var count__33697_33895 = (0);
var i__33698_33896 = (0);
while(true){
if((i__33698_33896 < count__33697_33895)){
var node_33897 = chunk__33696_33894.cljs$core$IIndexed$_nth$arity$2(null,i__33698_33896);
if(cljs.core.not(node_33897.shadow$old)){
var path_match_33898 = shadow.cljs.devtools.client.browser.match_paths(node_33897.getAttribute("href"),path);
if(cljs.core.truth_(path_match_33898)){
var new_link_33899 = (function (){var G__33724 = node_33897.cloneNode(true);
G__33724.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_33898)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__33724;
})();
(node_33897.shadow$old = true);

(new_link_33899.onload = ((function (seq__33692_33893,chunk__33696_33894,count__33697_33895,i__33698_33896,seq__33578,chunk__33580,count__33581,i__33582,new_link_33899,path_match_33898,node_33897,path,map__33577,map__33577__$1,msg,updates,reload_info){
return (function (e){
var seq__33725_33900 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__33727_33901 = null;
var count__33728_33902 = (0);
var i__33729_33903 = (0);
while(true){
if((i__33729_33903 < count__33728_33902)){
var map__33733_33904 = chunk__33727_33901.cljs$core$IIndexed$_nth$arity$2(null,i__33729_33903);
var map__33733_33905__$1 = cljs.core.__destructure_map(map__33733_33904);
var task_33906 = map__33733_33905__$1;
var fn_str_33907 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33733_33905__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_33908 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33733_33905__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_33909 = goog.getObjectByName(fn_str_33907,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_33908)));

(fn_obj_33909.cljs$core$IFn$_invoke$arity$2 ? fn_obj_33909.cljs$core$IFn$_invoke$arity$2(path,new_link_33899) : fn_obj_33909.call(null,path,new_link_33899));


var G__33910 = seq__33725_33900;
var G__33911 = chunk__33727_33901;
var G__33912 = count__33728_33902;
var G__33913 = (i__33729_33903 + (1));
seq__33725_33900 = G__33910;
chunk__33727_33901 = G__33911;
count__33728_33902 = G__33912;
i__33729_33903 = G__33913;
continue;
} else {
var temp__5823__auto___33914 = cljs.core.seq(seq__33725_33900);
if(temp__5823__auto___33914){
var seq__33725_33915__$1 = temp__5823__auto___33914;
if(cljs.core.chunked_seq_QMARK_(seq__33725_33915__$1)){
var c__5673__auto___33916 = cljs.core.chunk_first(seq__33725_33915__$1);
var G__33917 = cljs.core.chunk_rest(seq__33725_33915__$1);
var G__33918 = c__5673__auto___33916;
var G__33919 = cljs.core.count(c__5673__auto___33916);
var G__33920 = (0);
seq__33725_33900 = G__33917;
chunk__33727_33901 = G__33918;
count__33728_33902 = G__33919;
i__33729_33903 = G__33920;
continue;
} else {
var map__33734_33921 = cljs.core.first(seq__33725_33915__$1);
var map__33734_33922__$1 = cljs.core.__destructure_map(map__33734_33921);
var task_33923 = map__33734_33922__$1;
var fn_str_33924 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33734_33922__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_33925 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33734_33922__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_33926 = goog.getObjectByName(fn_str_33924,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_33925)));

(fn_obj_33926.cljs$core$IFn$_invoke$arity$2 ? fn_obj_33926.cljs$core$IFn$_invoke$arity$2(path,new_link_33899) : fn_obj_33926.call(null,path,new_link_33899));


var G__33927 = cljs.core.next(seq__33725_33915__$1);
var G__33928 = null;
var G__33929 = (0);
var G__33930 = (0);
seq__33725_33900 = G__33927;
chunk__33727_33901 = G__33928;
count__33728_33902 = G__33929;
i__33729_33903 = G__33930;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_33897);
});})(seq__33692_33893,chunk__33696_33894,count__33697_33895,i__33698_33896,seq__33578,chunk__33580,count__33581,i__33582,new_link_33899,path_match_33898,node_33897,path,map__33577,map__33577__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_33898], 0));

goog.dom.insertSiblingAfter(new_link_33899,node_33897);


var G__33931 = seq__33692_33893;
var G__33932 = chunk__33696_33894;
var G__33933 = count__33697_33895;
var G__33934 = (i__33698_33896 + (1));
seq__33692_33893 = G__33931;
chunk__33696_33894 = G__33932;
count__33697_33895 = G__33933;
i__33698_33896 = G__33934;
continue;
} else {
var G__33935 = seq__33692_33893;
var G__33936 = chunk__33696_33894;
var G__33937 = count__33697_33895;
var G__33938 = (i__33698_33896 + (1));
seq__33692_33893 = G__33935;
chunk__33696_33894 = G__33936;
count__33697_33895 = G__33937;
i__33698_33896 = G__33938;
continue;
}
} else {
var G__33939 = seq__33692_33893;
var G__33940 = chunk__33696_33894;
var G__33941 = count__33697_33895;
var G__33942 = (i__33698_33896 + (1));
seq__33692_33893 = G__33939;
chunk__33696_33894 = G__33940;
count__33697_33895 = G__33941;
i__33698_33896 = G__33942;
continue;
}
} else {
var temp__5823__auto___33943 = cljs.core.seq(seq__33692_33893);
if(temp__5823__auto___33943){
var seq__33692_33944__$1 = temp__5823__auto___33943;
if(cljs.core.chunked_seq_QMARK_(seq__33692_33944__$1)){
var c__5673__auto___33945 = cljs.core.chunk_first(seq__33692_33944__$1);
var G__33946 = cljs.core.chunk_rest(seq__33692_33944__$1);
var G__33947 = c__5673__auto___33945;
var G__33948 = cljs.core.count(c__5673__auto___33945);
var G__33949 = (0);
seq__33692_33893 = G__33946;
chunk__33696_33894 = G__33947;
count__33697_33895 = G__33948;
i__33698_33896 = G__33949;
continue;
} else {
var node_33950 = cljs.core.first(seq__33692_33944__$1);
if(cljs.core.not(node_33950.shadow$old)){
var path_match_33951 = shadow.cljs.devtools.client.browser.match_paths(node_33950.getAttribute("href"),path);
if(cljs.core.truth_(path_match_33951)){
var new_link_33952 = (function (){var G__33735 = node_33950.cloneNode(true);
G__33735.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_33951)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__33735;
})();
(node_33950.shadow$old = true);

(new_link_33952.onload = ((function (seq__33692_33893,chunk__33696_33894,count__33697_33895,i__33698_33896,seq__33578,chunk__33580,count__33581,i__33582,new_link_33952,path_match_33951,node_33950,seq__33692_33944__$1,temp__5823__auto___33943,path,map__33577,map__33577__$1,msg,updates,reload_info){
return (function (e){
var seq__33736_33953 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__33738_33954 = null;
var count__33739_33955 = (0);
var i__33740_33956 = (0);
while(true){
if((i__33740_33956 < count__33739_33955)){
var map__33744_33957 = chunk__33738_33954.cljs$core$IIndexed$_nth$arity$2(null,i__33740_33956);
var map__33744_33958__$1 = cljs.core.__destructure_map(map__33744_33957);
var task_33959 = map__33744_33958__$1;
var fn_str_33960 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33744_33958__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_33961 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33744_33958__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_33962 = goog.getObjectByName(fn_str_33960,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_33961)));

(fn_obj_33962.cljs$core$IFn$_invoke$arity$2 ? fn_obj_33962.cljs$core$IFn$_invoke$arity$2(path,new_link_33952) : fn_obj_33962.call(null,path,new_link_33952));


var G__33963 = seq__33736_33953;
var G__33964 = chunk__33738_33954;
var G__33965 = count__33739_33955;
var G__33966 = (i__33740_33956 + (1));
seq__33736_33953 = G__33963;
chunk__33738_33954 = G__33964;
count__33739_33955 = G__33965;
i__33740_33956 = G__33966;
continue;
} else {
var temp__5823__auto___33967__$1 = cljs.core.seq(seq__33736_33953);
if(temp__5823__auto___33967__$1){
var seq__33736_33968__$1 = temp__5823__auto___33967__$1;
if(cljs.core.chunked_seq_QMARK_(seq__33736_33968__$1)){
var c__5673__auto___33969 = cljs.core.chunk_first(seq__33736_33968__$1);
var G__33970 = cljs.core.chunk_rest(seq__33736_33968__$1);
var G__33971 = c__5673__auto___33969;
var G__33972 = cljs.core.count(c__5673__auto___33969);
var G__33973 = (0);
seq__33736_33953 = G__33970;
chunk__33738_33954 = G__33971;
count__33739_33955 = G__33972;
i__33740_33956 = G__33973;
continue;
} else {
var map__33745_33974 = cljs.core.first(seq__33736_33968__$1);
var map__33745_33975__$1 = cljs.core.__destructure_map(map__33745_33974);
var task_33976 = map__33745_33975__$1;
var fn_str_33977 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33745_33975__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_33978 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33745_33975__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_33979 = goog.getObjectByName(fn_str_33977,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_33978)));

(fn_obj_33979.cljs$core$IFn$_invoke$arity$2 ? fn_obj_33979.cljs$core$IFn$_invoke$arity$2(path,new_link_33952) : fn_obj_33979.call(null,path,new_link_33952));


var G__33980 = cljs.core.next(seq__33736_33968__$1);
var G__33981 = null;
var G__33982 = (0);
var G__33983 = (0);
seq__33736_33953 = G__33980;
chunk__33738_33954 = G__33981;
count__33739_33955 = G__33982;
i__33740_33956 = G__33983;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_33950);
});})(seq__33692_33893,chunk__33696_33894,count__33697_33895,i__33698_33896,seq__33578,chunk__33580,count__33581,i__33582,new_link_33952,path_match_33951,node_33950,seq__33692_33944__$1,temp__5823__auto___33943,path,map__33577,map__33577__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_33951], 0));

goog.dom.insertSiblingAfter(new_link_33952,node_33950);


var G__33984 = cljs.core.next(seq__33692_33944__$1);
var G__33985 = null;
var G__33986 = (0);
var G__33987 = (0);
seq__33692_33893 = G__33984;
chunk__33696_33894 = G__33985;
count__33697_33895 = G__33986;
i__33698_33896 = G__33987;
continue;
} else {
var G__33988 = cljs.core.next(seq__33692_33944__$1);
var G__33989 = null;
var G__33990 = (0);
var G__33991 = (0);
seq__33692_33893 = G__33988;
chunk__33696_33894 = G__33989;
count__33697_33895 = G__33990;
i__33698_33896 = G__33991;
continue;
}
} else {
var G__33992 = cljs.core.next(seq__33692_33944__$1);
var G__33993 = null;
var G__33994 = (0);
var G__33995 = (0);
seq__33692_33893 = G__33992;
chunk__33696_33894 = G__33993;
count__33697_33895 = G__33994;
i__33698_33896 = G__33995;
continue;
}
}
} else {
}
}
break;
}


var G__33996 = seq__33578;
var G__33997 = chunk__33580;
var G__33998 = count__33581;
var G__33999 = (i__33582 + (1));
seq__33578 = G__33996;
chunk__33580 = G__33997;
count__33581 = G__33998;
i__33582 = G__33999;
continue;
} else {
var G__34000 = seq__33578;
var G__34001 = chunk__33580;
var G__34002 = count__33581;
var G__34003 = (i__33582 + (1));
seq__33578 = G__34000;
chunk__33580 = G__34001;
count__33581 = G__34002;
i__33582 = G__34003;
continue;
}
} else {
var temp__5823__auto__ = cljs.core.seq(seq__33578);
if(temp__5823__auto__){
var seq__33578__$1 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__33578__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__33578__$1);
var G__34004 = cljs.core.chunk_rest(seq__33578__$1);
var G__34005 = c__5673__auto__;
var G__34006 = cljs.core.count(c__5673__auto__);
var G__34007 = (0);
seq__33578 = G__34004;
chunk__33580 = G__34005;
count__33581 = G__34006;
i__33582 = G__34007;
continue;
} else {
var path = cljs.core.first(seq__33578__$1);
if(clojure.string.ends_with_QMARK_(path,"css")){
var seq__33746_34008 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(document.querySelectorAll("link[rel=\"stylesheet\"]")));
var chunk__33750_34009 = null;
var count__33751_34010 = (0);
var i__33752_34011 = (0);
while(true){
if((i__33752_34011 < count__33751_34010)){
var node_34012 = chunk__33750_34009.cljs$core$IIndexed$_nth$arity$2(null,i__33752_34011);
if(cljs.core.not(node_34012.shadow$old)){
var path_match_34013 = shadow.cljs.devtools.client.browser.match_paths(node_34012.getAttribute("href"),path);
if(cljs.core.truth_(path_match_34013)){
var new_link_34014 = (function (){var G__33780 = node_34012.cloneNode(true);
G__33780.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_34013)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__33780;
})();
(node_34012.shadow$old = true);

(new_link_34014.onload = ((function (seq__33746_34008,chunk__33750_34009,count__33751_34010,i__33752_34011,seq__33578,chunk__33580,count__33581,i__33582,new_link_34014,path_match_34013,node_34012,path,seq__33578__$1,temp__5823__auto__,map__33577,map__33577__$1,msg,updates,reload_info){
return (function (e){
var seq__33781_34015 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__33783_34016 = null;
var count__33784_34017 = (0);
var i__33785_34018 = (0);
while(true){
if((i__33785_34018 < count__33784_34017)){
var map__33789_34019 = chunk__33783_34016.cljs$core$IIndexed$_nth$arity$2(null,i__33785_34018);
var map__33789_34020__$1 = cljs.core.__destructure_map(map__33789_34019);
var task_34021 = map__33789_34020__$1;
var fn_str_34022 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33789_34020__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_34023 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33789_34020__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_34024 = goog.getObjectByName(fn_str_34022,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_34023)));

(fn_obj_34024.cljs$core$IFn$_invoke$arity$2 ? fn_obj_34024.cljs$core$IFn$_invoke$arity$2(path,new_link_34014) : fn_obj_34024.call(null,path,new_link_34014));


var G__34025 = seq__33781_34015;
var G__34026 = chunk__33783_34016;
var G__34027 = count__33784_34017;
var G__34028 = (i__33785_34018 + (1));
seq__33781_34015 = G__34025;
chunk__33783_34016 = G__34026;
count__33784_34017 = G__34027;
i__33785_34018 = G__34028;
continue;
} else {
var temp__5823__auto___34029__$1 = cljs.core.seq(seq__33781_34015);
if(temp__5823__auto___34029__$1){
var seq__33781_34030__$1 = temp__5823__auto___34029__$1;
if(cljs.core.chunked_seq_QMARK_(seq__33781_34030__$1)){
var c__5673__auto___34031 = cljs.core.chunk_first(seq__33781_34030__$1);
var G__34032 = cljs.core.chunk_rest(seq__33781_34030__$1);
var G__34033 = c__5673__auto___34031;
var G__34034 = cljs.core.count(c__5673__auto___34031);
var G__34035 = (0);
seq__33781_34015 = G__34032;
chunk__33783_34016 = G__34033;
count__33784_34017 = G__34034;
i__33785_34018 = G__34035;
continue;
} else {
var map__33790_34036 = cljs.core.first(seq__33781_34030__$1);
var map__33790_34037__$1 = cljs.core.__destructure_map(map__33790_34036);
var task_34038 = map__33790_34037__$1;
var fn_str_34039 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33790_34037__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_34040 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33790_34037__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_34041 = goog.getObjectByName(fn_str_34039,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_34040)));

(fn_obj_34041.cljs$core$IFn$_invoke$arity$2 ? fn_obj_34041.cljs$core$IFn$_invoke$arity$2(path,new_link_34014) : fn_obj_34041.call(null,path,new_link_34014));


var G__34042 = cljs.core.next(seq__33781_34030__$1);
var G__34043 = null;
var G__34044 = (0);
var G__34045 = (0);
seq__33781_34015 = G__34042;
chunk__33783_34016 = G__34043;
count__33784_34017 = G__34044;
i__33785_34018 = G__34045;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_34012);
});})(seq__33746_34008,chunk__33750_34009,count__33751_34010,i__33752_34011,seq__33578,chunk__33580,count__33581,i__33582,new_link_34014,path_match_34013,node_34012,path,seq__33578__$1,temp__5823__auto__,map__33577,map__33577__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_34013], 0));

goog.dom.insertSiblingAfter(new_link_34014,node_34012);


var G__34046 = seq__33746_34008;
var G__34047 = chunk__33750_34009;
var G__34048 = count__33751_34010;
var G__34049 = (i__33752_34011 + (1));
seq__33746_34008 = G__34046;
chunk__33750_34009 = G__34047;
count__33751_34010 = G__34048;
i__33752_34011 = G__34049;
continue;
} else {
var G__34050 = seq__33746_34008;
var G__34051 = chunk__33750_34009;
var G__34052 = count__33751_34010;
var G__34053 = (i__33752_34011 + (1));
seq__33746_34008 = G__34050;
chunk__33750_34009 = G__34051;
count__33751_34010 = G__34052;
i__33752_34011 = G__34053;
continue;
}
} else {
var G__34054 = seq__33746_34008;
var G__34055 = chunk__33750_34009;
var G__34056 = count__33751_34010;
var G__34057 = (i__33752_34011 + (1));
seq__33746_34008 = G__34054;
chunk__33750_34009 = G__34055;
count__33751_34010 = G__34056;
i__33752_34011 = G__34057;
continue;
}
} else {
var temp__5823__auto___34058__$1 = cljs.core.seq(seq__33746_34008);
if(temp__5823__auto___34058__$1){
var seq__33746_34059__$1 = temp__5823__auto___34058__$1;
if(cljs.core.chunked_seq_QMARK_(seq__33746_34059__$1)){
var c__5673__auto___34060 = cljs.core.chunk_first(seq__33746_34059__$1);
var G__34061 = cljs.core.chunk_rest(seq__33746_34059__$1);
var G__34062 = c__5673__auto___34060;
var G__34063 = cljs.core.count(c__5673__auto___34060);
var G__34064 = (0);
seq__33746_34008 = G__34061;
chunk__33750_34009 = G__34062;
count__33751_34010 = G__34063;
i__33752_34011 = G__34064;
continue;
} else {
var node_34065 = cljs.core.first(seq__33746_34059__$1);
if(cljs.core.not(node_34065.shadow$old)){
var path_match_34066 = shadow.cljs.devtools.client.browser.match_paths(node_34065.getAttribute("href"),path);
if(cljs.core.truth_(path_match_34066)){
var new_link_34067 = (function (){var G__33791 = node_34065.cloneNode(true);
G__33791.setAttribute("href",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_match_34066)+"?r="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.rand.cljs$core$IFn$_invoke$arity$0())));

return G__33791;
})();
(node_34065.shadow$old = true);

(new_link_34067.onload = ((function (seq__33746_34008,chunk__33750_34009,count__33751_34010,i__33752_34011,seq__33578,chunk__33580,count__33581,i__33582,new_link_34067,path_match_34066,node_34065,seq__33746_34059__$1,temp__5823__auto___34058__$1,path,seq__33578__$1,temp__5823__auto__,map__33577,map__33577__$1,msg,updates,reload_info){
return (function (e){
var seq__33792_34068 = cljs.core.seq(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(msg,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"reload-info","reload-info",1648088086),new cljs.core.Keyword(null,"asset-load","asset-load",-1925902322)], null)));
var chunk__33794_34069 = null;
var count__33795_34070 = (0);
var i__33796_34071 = (0);
while(true){
if((i__33796_34071 < count__33795_34070)){
var map__33800_34072 = chunk__33794_34069.cljs$core$IIndexed$_nth$arity$2(null,i__33796_34071);
var map__33800_34073__$1 = cljs.core.__destructure_map(map__33800_34072);
var task_34074 = map__33800_34073__$1;
var fn_str_34075 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33800_34073__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_34076 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33800_34073__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_34077 = goog.getObjectByName(fn_str_34075,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_34076)));

(fn_obj_34077.cljs$core$IFn$_invoke$arity$2 ? fn_obj_34077.cljs$core$IFn$_invoke$arity$2(path,new_link_34067) : fn_obj_34077.call(null,path,new_link_34067));


var G__34078 = seq__33792_34068;
var G__34079 = chunk__33794_34069;
var G__34080 = count__33795_34070;
var G__34081 = (i__33796_34071 + (1));
seq__33792_34068 = G__34078;
chunk__33794_34069 = G__34079;
count__33795_34070 = G__34080;
i__33796_34071 = G__34081;
continue;
} else {
var temp__5823__auto___34082__$2 = cljs.core.seq(seq__33792_34068);
if(temp__5823__auto___34082__$2){
var seq__33792_34083__$1 = temp__5823__auto___34082__$2;
if(cljs.core.chunked_seq_QMARK_(seq__33792_34083__$1)){
var c__5673__auto___34084 = cljs.core.chunk_first(seq__33792_34083__$1);
var G__34085 = cljs.core.chunk_rest(seq__33792_34083__$1);
var G__34086 = c__5673__auto___34084;
var G__34087 = cljs.core.count(c__5673__auto___34084);
var G__34088 = (0);
seq__33792_34068 = G__34085;
chunk__33794_34069 = G__34086;
count__33795_34070 = G__34087;
i__33796_34071 = G__34088;
continue;
} else {
var map__33803_34089 = cljs.core.first(seq__33792_34083__$1);
var map__33803_34090__$1 = cljs.core.__destructure_map(map__33803_34089);
var task_34091 = map__33803_34090__$1;
var fn_str_34092 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33803_34090__$1,new cljs.core.Keyword(null,"fn-str","fn-str",-1348506402));
var fn_sym_34093 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33803_34090__$1,new cljs.core.Keyword(null,"fn-sym","fn-sym",1423988510));
var fn_obj_34094 = goog.getObjectByName(fn_str_34092,$CLJS);
shadow.cljs.devtools.client.browser.devtools_msg((""+"call "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fn_sym_34093)));

(fn_obj_34094.cljs$core$IFn$_invoke$arity$2 ? fn_obj_34094.cljs$core$IFn$_invoke$arity$2(path,new_link_34067) : fn_obj_34094.call(null,path,new_link_34067));


var G__34095 = cljs.core.next(seq__33792_34083__$1);
var G__34096 = null;
var G__34097 = (0);
var G__34098 = (0);
seq__33792_34068 = G__34095;
chunk__33794_34069 = G__34096;
count__33795_34070 = G__34097;
i__33796_34071 = G__34098;
continue;
}
} else {
}
}
break;
}

return goog.dom.removeNode(node_34065);
});})(seq__33746_34008,chunk__33750_34009,count__33751_34010,i__33752_34011,seq__33578,chunk__33580,count__33581,i__33582,new_link_34067,path_match_34066,node_34065,seq__33746_34059__$1,temp__5823__auto___34058__$1,path,seq__33578__$1,temp__5823__auto__,map__33577,map__33577__$1,msg,updates,reload_info))
);

shadow.cljs.devtools.client.browser.devtools_msg.cljs$core$IFn$_invoke$arity$variadic("load CSS",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([path_match_34066], 0));

goog.dom.insertSiblingAfter(new_link_34067,node_34065);


var G__34099 = cljs.core.next(seq__33746_34059__$1);
var G__34100 = null;
var G__34101 = (0);
var G__34102 = (0);
seq__33746_34008 = G__34099;
chunk__33750_34009 = G__34100;
count__33751_34010 = G__34101;
i__33752_34011 = G__34102;
continue;
} else {
var G__34103 = cljs.core.next(seq__33746_34059__$1);
var G__34104 = null;
var G__34105 = (0);
var G__34106 = (0);
seq__33746_34008 = G__34103;
chunk__33750_34009 = G__34104;
count__33751_34010 = G__34105;
i__33752_34011 = G__34106;
continue;
}
} else {
var G__34107 = cljs.core.next(seq__33746_34059__$1);
var G__34108 = null;
var G__34109 = (0);
var G__34110 = (0);
seq__33746_34008 = G__34107;
chunk__33750_34009 = G__34108;
count__33751_34010 = G__34109;
i__33752_34011 = G__34110;
continue;
}
}
} else {
}
}
break;
}


var G__34111 = cljs.core.next(seq__33578__$1);
var G__34112 = null;
var G__34113 = (0);
var G__34114 = (0);
seq__33578 = G__34111;
chunk__33580 = G__34112;
count__33581 = G__34113;
i__33582 = G__34114;
continue;
} else {
var G__34115 = cljs.core.next(seq__33578__$1);
var G__34116 = null;
var G__34117 = (0);
var G__34118 = (0);
seq__33578 = G__34115;
chunk__33580 = G__34116;
count__33581 = G__34117;
i__33582 = G__34118;
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
try{var G__33805 = shadow.cljs.devtools.client.browser.global_eval(code);
return (success.cljs$core$IFn$_invoke$arity$1 ? success.cljs$core$IFn$_invoke$arity$1(G__33805) : success.call(null,G__33805));
}catch (e33804){var e = e33804;
return (fail.cljs$core$IFn$_invoke$arity$1 ? fail.cljs$core$IFn$_invoke$arity$1(e) : fail.call(null,e));
}}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$ = cljs.core.PROTOCOL_SENTINEL);

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_invoke$arity$5 = (function (this$,ns,p__33806,success,fail){
var map__33807 = p__33806;
var map__33807__$1 = cljs.core.__destructure_map(map__33807);
var js = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33807__$1,new cljs.core.Keyword(null,"js","js",1768080579));
var this$__$1 = this;
try{var G__33809 = shadow.cljs.devtools.client.browser.global_eval(js);
return (success.cljs$core$IFn$_invoke$arity$1 ? success.cljs$core$IFn$_invoke$arity$1(G__33809) : success.call(null,G__33809));
}catch (e33808){var e = e33808;
return (fail.cljs$core$IFn$_invoke$arity$1 ? fail.cljs$core$IFn$_invoke$arity$1(e) : fail.call(null,e));
}}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_repl_init$arity$4 = (function (runtime,p__33810,done,error){
var map__33811 = p__33810;
var map__33811__$1 = cljs.core.__destructure_map(map__33811);
var repl_sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33811__$1,new cljs.core.Keyword(null,"repl-sources","repl-sources",723867535));
var runtime__$1 = this;
return shadow.cljs.devtools.client.shared.load_sources(runtime__$1,cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(shadow.cljs.devtools.client.env.src_is_loaded_QMARK_,repl_sources)),(function (sources){
shadow.cljs.devtools.client.browser.do_js_load(sources);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));
}));

(shadow.cljs.devtools.client.shared.Runtime.prototype.shadow$cljs$devtools$client$shared$IHostSpecific$do_repl_require$arity$4 = (function (runtime,p__33812,done,error){
var map__33813 = p__33812;
var map__33813__$1 = cljs.core.__destructure_map(map__33813);
var msg = map__33813__$1;
var sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33813__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var reload_namespaces = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33813__$1,new cljs.core.Keyword(null,"reload-namespaces","reload-namespaces",250210134));
var js_requires = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33813__$1,new cljs.core.Keyword(null,"js-requires","js-requires",-1311472051));
var runtime__$1 = this;
var sources_to_load = cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentVector.EMPTY,cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p__33814){
var map__33815 = p__33814;
var map__33815__$1 = cljs.core.__destructure_map(map__33815);
var src = map__33815__$1;
var provides = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33815__$1,new cljs.core.Keyword(null,"provides","provides",-1634397992));
var and__5140__auto__ = shadow.cljs.devtools.client.env.src_is_loaded_QMARK_(src);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not(cljs.core.some(reload_namespaces,provides));
} else {
return and__5140__auto__;
}
}),sources));
if(cljs.core.not(cljs.core.seq(sources_to_load))){
var G__33816 = cljs.core.PersistentVector.EMPTY;
return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(G__33816) : done.call(null,G__33816));
} else {
return shadow.remote.runtime.shared.call.cljs$core$IFn$_invoke$arity$3(runtime__$1,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"op","op",-1882987955),new cljs.core.Keyword(null,"cljs-load-sources","cljs-load-sources",-1458295962),new cljs.core.Keyword(null,"to","to",192099007),shadow.cljs.devtools.client.env.worker_client_id,new cljs.core.Keyword(null,"sources","sources",-321166424),cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentVector.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582)),sources_to_load)], null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"cljs-sources","cljs-sources",31121610),(function (p__33817){
var map__33818 = p__33817;
var map__33818__$1 = cljs.core.__destructure_map(map__33818);
var msg__$1 = map__33818__$1;
var sources__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33818__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
try{shadow.cljs.devtools.client.browser.do_js_load(sources__$1);

if(cljs.core.seq(js_requires)){
shadow.cljs.devtools.client.browser.do_js_requires(js_requires);
} else {
}

return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(sources_to_load) : done.call(null,sources_to_load));
}catch (e33819){var ex = e33819;
return (error.cljs$core$IFn$_invoke$arity$1 ? error.cljs$core$IFn$_invoke$arity$1(ex) : error.call(null,ex));
}})], null));
}
}));

shadow.cljs.devtools.client.shared.add_plugin_BANG_(new cljs.core.Keyword("shadow.cljs.devtools.client.browser","client","shadow.cljs.devtools.client.browser/client",-1461019282),cljs.core.PersistentHashSet.EMPTY,(function (p__33820){
var map__33821 = p__33820;
var map__33821__$1 = cljs.core.__destructure_map(map__33821);
var env = map__33821__$1;
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33821__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
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
}),new cljs.core.Keyword("shadow.cljs.devtools.client.env","worker-notify","shadow.cljs.devtools.client.env/worker-notify",-1456820670),(function (p__33822){
var map__33823 = p__33822;
var map__33823__$1 = cljs.core.__destructure_map(map__33823);
var event_op = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33823__$1,new cljs.core.Keyword(null,"event-op","event-op",200358057));
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33823__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
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
}),(function (p__33824){
var map__33825 = p__33824;
var map__33825__$1 = cljs.core.__destructure_map(map__33825);
var svc = map__33825__$1;
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__33825__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
return shadow.remote.runtime.api.del_extension(runtime,new cljs.core.Keyword("shadow.cljs.devtools.client.browser","client","shadow.cljs.devtools.client.browser/client",-1461019282));
}));

shadow.cljs.devtools.client.shared.init_runtime_BANG_(shadow.cljs.devtools.client.browser.client_info,shadow.cljs.devtools.client.websocket.start,shadow.cljs.devtools.client.websocket.send,shadow.cljs.devtools.client.websocket.stop);
} else {
}

//# sourceMappingURL=shadow.cljs.devtools.client.browser.js.map
