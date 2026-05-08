import "./cljs_env.js";
import "./cljs.core.js";
import "./cljs.spec.alpha.js";
import "./goog.string.string.js";
import "./goog.string.stringformat.js";
goog.provide('cljs.repl');
cljs.repl.print_doc = (function cljs$repl$print_doc(p__24733){
var map__24734 = p__24733;
var map__24734__$1 = cljs.core.__destructure_map(map__24734);
var m = map__24734__$1;
var n = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24734__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var nm = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24734__$1,new cljs.core.Keyword(null,"name","name",1843675177));
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["-------------------------"], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"spec","spec",347520401).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__5825__auto__)){
var ns = temp__5825__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+"/");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m)));
}
})()], 0));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Protocol"], 0));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m))){
var seq__24739_25316 = cljs.core.seq(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__24740_25317 = null;
var count__24741_25318 = (0);
var i__24742_25319 = (0);
while(true){
if((i__24742_25319 < count__24741_25318)){
var f_25320 = chunk__24740_25317.cljs$core$IIndexed$_nth$arity$2(null,i__24742_25319);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_25320], 0));


var G__25325 = seq__24739_25316;
var G__25326 = chunk__24740_25317;
var G__25327 = count__24741_25318;
var G__25328 = (i__24742_25319 + (1));
seq__24739_25316 = G__25325;
chunk__24740_25317 = G__25326;
count__24741_25318 = G__25327;
i__24742_25319 = G__25328;
continue;
} else {
var temp__5825__auto___25329 = cljs.core.seq(seq__24739_25316);
if(temp__5825__auto___25329){
var seq__24739_25330__$1 = temp__5825__auto___25329;
if(cljs.core.chunked_seq_QMARK_(seq__24739_25330__$1)){
var c__5673__auto___25331 = cljs.core.chunk_first(seq__24739_25330__$1);
var G__25332 = cljs.core.chunk_rest(seq__24739_25330__$1);
var G__25333 = c__5673__auto___25331;
var G__25334 = cljs.core.count(c__5673__auto___25331);
var G__25335 = (0);
seq__24739_25316 = G__25332;
chunk__24740_25317 = G__25333;
count__24741_25318 = G__25334;
i__24742_25319 = G__25335;
continue;
} else {
var f_25337 = cljs.core.first(seq__24739_25330__$1);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_25337], 0));


var G__25341 = cljs.core.next(seq__24739_25330__$1);
var G__25342 = null;
var G__25343 = (0);
var G__25344 = (0);
seq__24739_25316 = G__25341;
chunk__24740_25317 = G__25342;
count__24741_25318 = G__25343;
i__24742_25319 = G__25344;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_25345 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([arglists_25345], 0));
} else {
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first(arglists_25345)))?cljs.core.second(arglists_25345):arglists_25345)], 0));
}
} else {
}
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"special-form","special-form",-1326536374).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Special Form"], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m)], 0));

if(cljs.core.contains_QMARK_(m,new cljs.core.Keyword(null,"url","url",276297046))){
if(cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n  Please see http://clojure.org/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m)))], 0));
} else {
return null;
}
} else {
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n  Please see http://clojure.org/special_forms#"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m)))], 0));
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Macro"], 0));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"spec","spec",347520401).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Spec"], 0));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["REPL Special Function"], 0));
} else {
}

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m)], 0));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
var seq__24797_25350 = cljs.core.seq(new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__24798_25351 = null;
var count__24799_25352 = (0);
var i__24800_25353 = (0);
while(true){
if((i__24800_25353 < count__24799_25352)){
var vec__24856_25354 = chunk__24798_25351.cljs$core$IIndexed$_nth$arity$2(null,i__24800_25353);
var name_25355 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__24856_25354,(0),null);
var map__24859_25356 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__24856_25354,(1),null);
var map__24859_25357__$1 = cljs.core.__destructure_map(map__24859_25356);
var doc_25358 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24859_25357__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_25359 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24859_25357__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_25355], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_25359], 0));

if(cljs.core.truth_(doc_25358)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_25358], 0));
} else {
}


var G__25364 = seq__24797_25350;
var G__25365 = chunk__24798_25351;
var G__25366 = count__24799_25352;
var G__25367 = (i__24800_25353 + (1));
seq__24797_25350 = G__25364;
chunk__24798_25351 = G__25365;
count__24799_25352 = G__25366;
i__24800_25353 = G__25367;
continue;
} else {
var temp__5825__auto___25368 = cljs.core.seq(seq__24797_25350);
if(temp__5825__auto___25368){
var seq__24797_25369__$1 = temp__5825__auto___25368;
if(cljs.core.chunked_seq_QMARK_(seq__24797_25369__$1)){
var c__5673__auto___25374 = cljs.core.chunk_first(seq__24797_25369__$1);
var G__25375 = cljs.core.chunk_rest(seq__24797_25369__$1);
var G__25376 = c__5673__auto___25374;
var G__25377 = cljs.core.count(c__5673__auto___25374);
var G__25378 = (0);
seq__24797_25350 = G__25375;
chunk__24798_25351 = G__25376;
count__24799_25352 = G__25377;
i__24800_25353 = G__25378;
continue;
} else {
var vec__24871_25379 = cljs.core.first(seq__24797_25369__$1);
var name_25380 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__24871_25379,(0),null);
var map__24874_25381 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__24871_25379,(1),null);
var map__24874_25382__$1 = cljs.core.__destructure_map(map__24874_25381);
var doc_25383 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24874_25382__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_25384 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__24874_25382__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_25380], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_25384], 0));

if(cljs.core.truth_(doc_25383)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_25383], 0));
} else {
}


var G__25389 = cljs.core.next(seq__24797_25369__$1);
var G__25390 = null;
var G__25391 = (0);
var G__25392 = (0);
seq__24797_25350 = G__25389;
chunk__24798_25351 = G__25390;
count__24799_25352 = G__25391;
i__24800_25353 = G__25392;
continue;
}
} else {
}
}
break;
}
} else {
}

if(cljs.core.truth_(n)){
var temp__5825__auto__ = cljs.spec.alpha.get_spec(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.ns_name(n))),cljs.core.name(nm)));
if(cljs.core.truth_(temp__5825__auto__)){
var fnspec = temp__5825__auto__;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Spec"], 0));

var seq__24888 = cljs.core.seq(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.Keyword(null,"ret","ret",-468222814),new cljs.core.Keyword(null,"fn","fn",-1175266204)], null));
var chunk__24889 = null;
var count__24890 = (0);
var i__24891 = (0);
while(true){
if((i__24891 < count__24890)){
var role = chunk__24889.cljs$core$IIndexed$_nth$arity$2(null,i__24891);
var temp__5825__auto___25397__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5825__auto___25397__$1)){
var spec_25401 = temp__5825__auto___25397__$1;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_25401)], 0));
} else {
}


var G__25403 = seq__24888;
var G__25404 = chunk__24889;
var G__25405 = count__24890;
var G__25406 = (i__24891 + (1));
seq__24888 = G__25403;
chunk__24889 = G__25404;
count__24890 = G__25405;
i__24891 = G__25406;
continue;
} else {
var temp__5825__auto____$1 = cljs.core.seq(seq__24888);
if(temp__5825__auto____$1){
var seq__24888__$1 = temp__5825__auto____$1;
if(cljs.core.chunked_seq_QMARK_(seq__24888__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__24888__$1);
var G__25411 = cljs.core.chunk_rest(seq__24888__$1);
var G__25412 = c__5673__auto__;
var G__25413 = cljs.core.count(c__5673__auto__);
var G__25414 = (0);
seq__24888 = G__25411;
chunk__24889 = G__25412;
count__24890 = G__25413;
i__24891 = G__25414;
continue;
} else {
var role = cljs.core.first(seq__24888__$1);
var temp__5825__auto___25419__$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5825__auto___25419__$2)){
var spec_25432 = temp__5825__auto___25419__$2;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_25432)], 0));
} else {
}


var G__25434 = cljs.core.next(seq__24888__$1);
var G__25435 = null;
var G__25436 = (0);
var G__25437 = (0);
seq__24888 = G__25434;
chunk__24889 = G__25435;
count__24890 = G__25436;
i__24891 = G__25437;
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
} else {
return null;
}
}
});
/**
 * Constructs a data representation for a Error with keys:
 *  :cause - root cause message
 *  :phase - error phase
 *  :via - cause chain, with cause keys:
 *           :type - exception class symbol
 *           :message - exception message
 *           :data - ex-data
 *           :at - top stack element
 *  :trace - root cause stack elements
 */
cljs.repl.Error__GT_map = (function cljs$repl$Error__GT_map(o){
return cljs.core.Throwable__GT_map(o);
});
/**
 * Returns an analysis of the phase, error, cause, and location of an error that occurred
 *   based on Throwable data, as returned by Throwable->map. All attributes other than phase
 *   are optional:
 *  :clojure.error/phase - keyword phase indicator, one of:
 *    :read-source :compile-syntax-check :compilation :macro-syntax-check :macroexpansion
 *    :execution :read-eval-result :print-eval-result
 *  :clojure.error/source - file name (no path)
 *  :clojure.error/line - integer line number
 *  :clojure.error/column - integer column number
 *  :clojure.error/symbol - symbol being expanded/compiled/invoked
 *  :clojure.error/class - cause exception class symbol
 *  :clojure.error/cause - cause exception message
 *  :clojure.error/spec - explain-data for spec error
 */
cljs.repl.ex_triage = (function cljs$repl$ex_triage(datafied_throwable){
var map__25006 = datafied_throwable;
var map__25006__$1 = cljs.core.__destructure_map(map__25006);
var via = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25006__$1,new cljs.core.Keyword(null,"via","via",-1904457336));
var trace = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25006__$1,new cljs.core.Keyword(null,"trace","trace",-1082747415));
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__25006__$1,new cljs.core.Keyword(null,"phase","phase",575722892),new cljs.core.Keyword(null,"execution","execution",253283524));
var map__25007 = cljs.core.last(via);
var map__25007__$1 = cljs.core.__destructure_map(map__25007);
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25007__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25007__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var data = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25007__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var map__25009 = data;
var map__25009__$1 = cljs.core.__destructure_map(map__25009);
var problems = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25009__$1,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814));
var fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25009__$1,new cljs.core.Keyword("cljs.spec.alpha","fn","cljs.spec.alpha/fn",408600443));
var caller = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25009__$1,new cljs.core.Keyword("cljs.spec.test.alpha","caller","cljs.spec.test.alpha/caller",-398302390));
var map__25012 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.first(via));
var map__25012__$1 = cljs.core.__destructure_map(map__25012);
var top_data = map__25012__$1;
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25012__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var G__25027 = phase;
var G__25027__$1 = (((G__25027 instanceof cljs.core.Keyword))?G__25027.fqn:null);
switch (G__25027__$1) {
case "read-source":
var map__25038 = data;
var map__25038__$1 = cljs.core.__destructure_map(map__25038);
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25038__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25038__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var G__25046 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.second(via)),top_data], 0));
var G__25046__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25046,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__25046);
var G__25046__$2 = (cljs.core.truth_((function (){var fexpr__25051 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__25051.cljs$core$IFn$_invoke$arity$1 ? fexpr__25051.cljs$core$IFn$_invoke$arity$1(source) : fexpr__25051.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__25046__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__25046__$1);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25046__$2,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__25046__$2;
}

break;
case "compile-syntax-check":
case "compilation":
case "macro-syntax-check":
case "macroexpansion":
var G__25056 = top_data;
var G__25056__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25056,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__25056);
var G__25056__$2 = (cljs.core.truth_((function (){var fexpr__25059 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__25059.cljs$core$IFn$_invoke$arity$1 ? fexpr__25059.cljs$core$IFn$_invoke$arity$1(source) : fexpr__25059.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__25056__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__25056__$1);
var G__25056__$3 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25056__$2,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__25056__$2);
var G__25056__$4 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25056__$3,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__25056__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25056__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__25056__$4;
}

break;
case "read-eval-result":
case "print-eval-result":
var vec__25071 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25071,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25071,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25071,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25071,(3),null);
var G__25078 = top_data;
var G__25078__$1 = (cljs.core.truth_(line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25078,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),line):G__25078);
var G__25078__$2 = (cljs.core.truth_(file)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25078__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file):G__25078__$1);
var G__25078__$3 = (cljs.core.truth_((function (){var and__5140__auto__ = source__$1;
if(cljs.core.truth_(and__5140__auto__)){
return method;
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25078__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null))):G__25078__$2);
var G__25078__$4 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25078__$3,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__25078__$3);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25078__$4,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__25078__$4;
}

break;
case "execution":
var vec__25096 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25096,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25096,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25096,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__25096,(3),null);
var file__$1 = cljs.core.first(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__24995_SHARP_){
var or__5142__auto__ = (p1__24995_SHARP_ == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var fexpr__25114 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__25114.cljs$core$IFn$_invoke$arity$1 ? fexpr__25114.cljs$core$IFn$_invoke$arity$1(p1__24995_SHARP_) : fexpr__25114.call(null,p1__24995_SHARP_));
}
}),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(caller),file], null)));
var err_line = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(caller);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return line;
}
})();
var G__25122 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type], null);
var G__25122__$1 = (cljs.core.truth_(err_line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25122,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),err_line):G__25122);
var G__25122__$2 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25122__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__25122__$1);
var G__25122__$3 = (cljs.core.truth_((function (){var or__5142__auto__ = fn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = source__$1;
if(cljs.core.truth_(and__5140__auto__)){
return method;
} else {
return and__5140__auto__;
}
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25122__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(function (){var or__5142__auto__ = fn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null));
}
})()):G__25122__$2);
var G__25122__$4 = (cljs.core.truth_(file__$1)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25122__$3,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file__$1):G__25122__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__25122__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__25122__$4;
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__25027__$1))));

}
})(),new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358),phase);
});
/**
 * Returns a string from exception data, as produced by ex-triage.
 *   The first line summarizes the exception phase and location.
 *   The subsequent lines describe the cause.
 */
cljs.repl.ex_str = (function cljs$repl$ex_str(p__25163){
var map__25165 = p__25163;
var map__25165__$1 = cljs.core.__destructure_map(map__25165);
var triage_data = map__25165__$1;
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var symbol = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994));
var class$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890));
var cause = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742));
var spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__25165__$1,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595));
var loc = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = source;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "<cljs repl>";
}
})())+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = line;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(column)?(""+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(column)):"")));
var class_name = cljs.core.name((function (){var or__5142__auto__ = class$;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var simple_class = class_name;
var cause_type = ((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["RuntimeException",null,"Exception",null], null), null),simple_class))?"":(""+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(simple_class)+")"));
var format = goog.string.format;
var G__25230 = phase;
var G__25230__$1 = (((G__25230 instanceof cljs.core.Keyword))?G__25230.fqn:null);
switch (G__25230__$1) {
case "read-source":
return (format.cljs$core$IFn$_invoke$arity$3 ? format.cljs$core$IFn$_invoke$arity$3("Syntax error reading source at (%s).\n%s\n",loc,cause) : format.call(null,"Syntax error reading source at (%s).\n%s\n",loc,cause));

break;
case "macro-syntax-check":
var G__25232 = "Syntax error macroexpanding %sat (%s).\n%s";
var G__25233 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__25234 = loc;
var G__25235 = (cljs.core.truth_(spec)?(function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__25241_25551 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__25242_25552 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__25243_25553 = true;
var _STAR_print_fn_STAR__temp_val__25244_25554 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__25243_25553);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__25244_25554);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__25153_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__25153_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__25242_25552);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__25241_25551);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})():(format.cljs$core$IFn$_invoke$arity$2 ? format.cljs$core$IFn$_invoke$arity$2("%s\n",cause) : format.call(null,"%s\n",cause)));
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__25232,G__25233,G__25234,G__25235) : format.call(null,G__25232,G__25233,G__25234,G__25235));

break;
case "macroexpansion":
var G__25249 = "Unexpected error%s macroexpanding %sat (%s).\n%s\n";
var G__25250 = cause_type;
var G__25251 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__25252 = loc;
var G__25253 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__25249,G__25250,G__25251,G__25252,G__25253) : format.call(null,G__25249,G__25250,G__25251,G__25252,G__25253));

break;
case "compile-syntax-check":
var G__25256 = "Syntax error%s compiling %sat (%s).\n%s\n";
var G__25257 = cause_type;
var G__25258 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__25259 = loc;
var G__25260 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__25256,G__25257,G__25258,G__25259,G__25260) : format.call(null,G__25256,G__25257,G__25258,G__25259,G__25260));

break;
case "compilation":
var G__25261 = "Unexpected error%s compiling %sat (%s).\n%s\n";
var G__25262 = cause_type;
var G__25263 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__25264 = loc;
var G__25265 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__25261,G__25262,G__25263,G__25264,G__25265) : format.call(null,G__25261,G__25262,G__25263,G__25264,G__25265));

break;
case "read-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "print-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "execution":
if(cljs.core.truth_(spec)){
var G__25271 = "Execution error - invalid arguments to %s at (%s).\n%s";
var G__25272 = symbol;
var G__25273 = loc;
var G__25274 = (function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__25277_25593 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__25278_25594 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__25279_25595 = true;
var _STAR_print_fn_STAR__temp_val__25280_25596 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__25279_25595);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__25280_25596);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__25155_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__25155_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__25278_25594);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__25277_25593);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})();
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__25271,G__25272,G__25273,G__25274) : format.call(null,G__25271,G__25272,G__25273,G__25274));
} else {
var G__25288 = "Execution error%s at %s(%s).\n%s\n";
var G__25289 = cause_type;
var G__25290 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__25291 = loc;
var G__25292 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__25288,G__25289,G__25290,G__25291,G__25292) : format.call(null,G__25288,G__25289,G__25290,G__25291,G__25292));
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__25230__$1))));

}
});
cljs.repl.error__GT_str = (function cljs$repl$error__GT_str(error){
return cljs.repl.ex_str(cljs.repl.ex_triage(cljs.repl.Error__GT_map(error)));
});

//# sourceMappingURL=cljs.repl.js.map
