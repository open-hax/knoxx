goog.provide('cljs.repl');
cljs.repl.print_doc = (function cljs$repl$print_doc(p__16884){
var map__16886 = p__16884;
var map__16886__$1 = cljs.core.__destructure_map(map__16886);
var m = map__16886__$1;
var n = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16886__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var nm = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16886__$1,new cljs.core.Keyword(null,"name","name",1843675177));
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["-------------------------"], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"spec","spec",347520401).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5823__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__5823__auto__)){
var ns = temp__5823__auto__;
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
var seq__16898_17343 = cljs.core.seq(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__16899_17344 = null;
var count__16900_17345 = (0);
var i__16901_17346 = (0);
while(true){
if((i__16901_17346 < count__16900_17345)){
var f_17347 = chunk__16899_17344.cljs$core$IIndexed$_nth$arity$2(null,i__16901_17346);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_17347], 0));


var G__17348 = seq__16898_17343;
var G__17349 = chunk__16899_17344;
var G__17350 = count__16900_17345;
var G__17351 = (i__16901_17346 + (1));
seq__16898_17343 = G__17348;
chunk__16899_17344 = G__17349;
count__16900_17345 = G__17350;
i__16901_17346 = G__17351;
continue;
} else {
var temp__5823__auto___17352 = cljs.core.seq(seq__16898_17343);
if(temp__5823__auto___17352){
var seq__16898_17353__$1 = temp__5823__auto___17352;
if(cljs.core.chunked_seq_QMARK_(seq__16898_17353__$1)){
var c__5673__auto___17354 = cljs.core.chunk_first(seq__16898_17353__$1);
var G__17355 = cljs.core.chunk_rest(seq__16898_17353__$1);
var G__17356 = c__5673__auto___17354;
var G__17357 = cljs.core.count(c__5673__auto___17354);
var G__17358 = (0);
seq__16898_17343 = G__17355;
chunk__16899_17344 = G__17356;
count__16900_17345 = G__17357;
i__16901_17346 = G__17358;
continue;
} else {
var f_17359 = cljs.core.first(seq__16898_17353__$1);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_17359], 0));


var G__17360 = cljs.core.next(seq__16898_17353__$1);
var G__17361 = null;
var G__17362 = (0);
var G__17363 = (0);
seq__16898_17343 = G__17360;
chunk__16899_17344 = G__17361;
count__16900_17345 = G__17362;
i__16901_17346 = G__17363;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_17365 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([arglists_17365], 0));
} else {
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first(arglists_17365)))?cljs.core.second(arglists_17365):arglists_17365)], 0));
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
var seq__16909_17368 = cljs.core.seq(new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__16910_17369 = null;
var count__16911_17370 = (0);
var i__16912_17371 = (0);
while(true){
if((i__16912_17371 < count__16911_17370)){
var vec__16961_17372 = chunk__16910_17369.cljs$core$IIndexed$_nth$arity$2(null,i__16912_17371);
var name_17373 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__16961_17372,(0),null);
var map__16964_17374 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__16961_17372,(1),null);
var map__16964_17375__$1 = cljs.core.__destructure_map(map__16964_17374);
var doc_17376 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16964_17375__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_17377 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16964_17375__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_17373], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_17377], 0));

if(cljs.core.truth_(doc_17376)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_17376], 0));
} else {
}


var G__17378 = seq__16909_17368;
var G__17379 = chunk__16910_17369;
var G__17380 = count__16911_17370;
var G__17381 = (i__16912_17371 + (1));
seq__16909_17368 = G__17378;
chunk__16910_17369 = G__17379;
count__16911_17370 = G__17380;
i__16912_17371 = G__17381;
continue;
} else {
var temp__5823__auto___17384 = cljs.core.seq(seq__16909_17368);
if(temp__5823__auto___17384){
var seq__16909_17385__$1 = temp__5823__auto___17384;
if(cljs.core.chunked_seq_QMARK_(seq__16909_17385__$1)){
var c__5673__auto___17386 = cljs.core.chunk_first(seq__16909_17385__$1);
var G__17387 = cljs.core.chunk_rest(seq__16909_17385__$1);
var G__17388 = c__5673__auto___17386;
var G__17389 = cljs.core.count(c__5673__auto___17386);
var G__17390 = (0);
seq__16909_17368 = G__17387;
chunk__16910_17369 = G__17388;
count__16911_17370 = G__17389;
i__16912_17371 = G__17390;
continue;
} else {
var vec__16971_17391 = cljs.core.first(seq__16909_17385__$1);
var name_17392 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__16971_17391,(0),null);
var map__16974_17393 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__16971_17391,(1),null);
var map__16974_17394__$1 = cljs.core.__destructure_map(map__16974_17393);
var doc_17395 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16974_17394__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_17396 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__16974_17394__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_17392], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_17396], 0));

if(cljs.core.truth_(doc_17395)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_17395], 0));
} else {
}


var G__17401 = cljs.core.next(seq__16909_17385__$1);
var G__17402 = null;
var G__17403 = (0);
var G__17404 = (0);
seq__16909_17368 = G__17401;
chunk__16910_17369 = G__17402;
count__16911_17370 = G__17403;
i__16912_17371 = G__17404;
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
var temp__5823__auto__ = cljs.spec.alpha.get_spec(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.ns_name(n))),cljs.core.name(nm)));
if(cljs.core.truth_(temp__5823__auto__)){
var fnspec = temp__5823__auto__;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Spec"], 0));

var seq__16993 = cljs.core.seq(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.Keyword(null,"ret","ret",-468222814),new cljs.core.Keyword(null,"fn","fn",-1175266204)], null));
var chunk__17003 = null;
var count__17004 = (0);
var i__17005 = (0);
while(true){
if((i__17005 < count__17004)){
var role = chunk__17003.cljs$core$IIndexed$_nth$arity$2(null,i__17005);
var temp__5823__auto___17405__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5823__auto___17405__$1)){
var spec_17406 = temp__5823__auto___17405__$1;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_17406)], 0));
} else {
}


var G__17407 = seq__16993;
var G__17408 = chunk__17003;
var G__17409 = count__17004;
var G__17410 = (i__17005 + (1));
seq__16993 = G__17407;
chunk__17003 = G__17408;
count__17004 = G__17409;
i__17005 = G__17410;
continue;
} else {
var temp__5823__auto____$1 = cljs.core.seq(seq__16993);
if(temp__5823__auto____$1){
var seq__16993__$1 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_(seq__16993__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__16993__$1);
var G__17411 = cljs.core.chunk_rest(seq__16993__$1);
var G__17412 = c__5673__auto__;
var G__17413 = cljs.core.count(c__5673__auto__);
var G__17414 = (0);
seq__16993 = G__17411;
chunk__17003 = G__17412;
count__17004 = G__17413;
i__17005 = G__17414;
continue;
} else {
var role = cljs.core.first(seq__16993__$1);
var temp__5823__auto___17416__$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5823__auto___17416__$2)){
var spec_17417 = temp__5823__auto___17416__$2;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_17417)], 0));
} else {
}


var G__17418 = cljs.core.next(seq__16993__$1);
var G__17419 = null;
var G__17420 = (0);
var G__17421 = (0);
seq__16993 = G__17418;
chunk__17003 = G__17419;
count__17004 = G__17420;
i__17005 = G__17421;
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
var map__17097 = datafied_throwable;
var map__17097__$1 = cljs.core.__destructure_map(map__17097);
var via = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17097__$1,new cljs.core.Keyword(null,"via","via",-1904457336));
var trace = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17097__$1,new cljs.core.Keyword(null,"trace","trace",-1082747415));
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__17097__$1,new cljs.core.Keyword(null,"phase","phase",575722892),new cljs.core.Keyword(null,"execution","execution",253283524));
var map__17101 = cljs.core.last(via);
var map__17101__$1 = cljs.core.__destructure_map(map__17101);
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17101__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17101__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var data = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17101__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var map__17103 = data;
var map__17103__$1 = cljs.core.__destructure_map(map__17103);
var problems = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17103__$1,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814));
var fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17103__$1,new cljs.core.Keyword("cljs.spec.alpha","fn","cljs.spec.alpha/fn",408600443));
var caller = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17103__$1,new cljs.core.Keyword("cljs.spec.test.alpha","caller","cljs.spec.test.alpha/caller",-398302390));
var map__17104 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.first(via));
var map__17104__$1 = cljs.core.__destructure_map(map__17104);
var top_data = map__17104__$1;
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17104__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var G__17130 = phase;
var G__17130__$1 = (((G__17130 instanceof cljs.core.Keyword))?G__17130.fqn:null);
switch (G__17130__$1) {
case "read-source":
var map__17153 = data;
var map__17153__$1 = cljs.core.__destructure_map(map__17153);
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17153__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17153__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var G__17156 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.second(via)),top_data], 0));
var G__17156__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17156,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__17156);
var G__17156__$2 = (cljs.core.truth_((function (){var fexpr__17162 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__17162.cljs$core$IFn$_invoke$arity$1 ? fexpr__17162.cljs$core$IFn$_invoke$arity$1(source) : fexpr__17162.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__17156__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__17156__$1);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17156__$2,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__17156__$2;
}

break;
case "compile-syntax-check":
case "compilation":
case "macro-syntax-check":
case "macroexpansion":
var G__17166 = top_data;
var G__17166__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17166,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__17166);
var G__17166__$2 = (cljs.core.truth_((function (){var fexpr__17169 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__17169.cljs$core$IFn$_invoke$arity$1 ? fexpr__17169.cljs$core$IFn$_invoke$arity$1(source) : fexpr__17169.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__17166__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__17166__$1);
var G__17166__$3 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17166__$2,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__17166__$2);
var G__17166__$4 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17166__$3,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__17166__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17166__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__17166__$4;
}

break;
case "read-eval-result":
case "print-eval-result":
var vec__17174 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17174,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17174,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17174,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17174,(3),null);
var G__17181 = top_data;
var G__17181__$1 = (cljs.core.truth_(line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17181,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),line):G__17181);
var G__17181__$2 = (cljs.core.truth_(file)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17181__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file):G__17181__$1);
var G__17181__$3 = (cljs.core.truth_((function (){var and__5140__auto__ = source__$1;
if(cljs.core.truth_(and__5140__auto__)){
return method;
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17181__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null))):G__17181__$2);
var G__17181__$4 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17181__$3,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__17181__$3);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17181__$4,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__17181__$4;
}

break;
case "execution":
var vec__17185 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17185,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17185,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17185,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__17185,(3),null);
var file__$1 = cljs.core.first(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__17088_SHARP_){
var or__5142__auto__ = (p1__17088_SHARP_ == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var fexpr__17194 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__17194.cljs$core$IFn$_invoke$arity$1 ? fexpr__17194.cljs$core$IFn$_invoke$arity$1(p1__17088_SHARP_) : fexpr__17194.call(null,p1__17088_SHARP_));
}
}),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(caller),file], null)));
var err_line = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(caller);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return line;
}
})();
var G__17198 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type], null);
var G__17198__$1 = (cljs.core.truth_(err_line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17198,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),err_line):G__17198);
var G__17198__$2 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17198__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__17198__$1);
var G__17198__$3 = (cljs.core.truth_((function (){var or__5142__auto__ = fn;
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
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17198__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(function (){var or__5142__auto__ = fn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null));
}
})()):G__17198__$2);
var G__17198__$4 = (cljs.core.truth_(file__$1)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17198__$3,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file__$1):G__17198__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__17198__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__17198__$4;
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__17130__$1))));

}
})(),new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358),phase);
});
/**
 * Returns a string from exception data, as produced by ex-triage.
 *   The first line summarizes the exception phase and location.
 *   The subsequent lines describe the cause.
 */
cljs.repl.ex_str = (function cljs$repl$ex_str(p__17240){
var map__17256 = p__17240;
var map__17256__$1 = cljs.core.__destructure_map(map__17256);
var triage_data = map__17256__$1;
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var symbol = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994));
var class$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890));
var cause = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742));
var spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__17256__$1,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595));
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
var G__17274 = phase;
var G__17274__$1 = (((G__17274 instanceof cljs.core.Keyword))?G__17274.fqn:null);
switch (G__17274__$1) {
case "read-source":
return (format.cljs$core$IFn$_invoke$arity$3 ? format.cljs$core$IFn$_invoke$arity$3("Syntax error reading source at (%s).\n%s\n",loc,cause) : format.call(null,"Syntax error reading source at (%s).\n%s\n",loc,cause));

break;
case "macro-syntax-check":
var G__17277 = "Syntax error macroexpanding %sat (%s).\n%s";
var G__17278 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__17279 = loc;
var G__17280 = (cljs.core.truth_(spec)?(function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__17285_17432 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__17286_17433 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__17287_17434 = true;
var _STAR_print_fn_STAR__temp_val__17288_17435 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__17287_17434);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__17288_17435);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__17220_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__17220_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__17286_17433);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__17285_17432);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})():(format.cljs$core$IFn$_invoke$arity$2 ? format.cljs$core$IFn$_invoke$arity$2("%s\n",cause) : format.call(null,"%s\n",cause)));
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__17277,G__17278,G__17279,G__17280) : format.call(null,G__17277,G__17278,G__17279,G__17280));

break;
case "macroexpansion":
var G__17305 = "Unexpected error%s macroexpanding %sat (%s).\n%s\n";
var G__17306 = cause_type;
var G__17307 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__17308 = loc;
var G__17309 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__17305,G__17306,G__17307,G__17308,G__17309) : format.call(null,G__17305,G__17306,G__17307,G__17308,G__17309));

break;
case "compile-syntax-check":
var G__17311 = "Syntax error%s compiling %sat (%s).\n%s\n";
var G__17312 = cause_type;
var G__17313 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__17314 = loc;
var G__17315 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__17311,G__17312,G__17313,G__17314,G__17315) : format.call(null,G__17311,G__17312,G__17313,G__17314,G__17315));

break;
case "compilation":
var G__17317 = "Unexpected error%s compiling %sat (%s).\n%s\n";
var G__17318 = cause_type;
var G__17319 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__17320 = loc;
var G__17321 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__17317,G__17318,G__17319,G__17320,G__17321) : format.call(null,G__17317,G__17318,G__17319,G__17320,G__17321));

break;
case "read-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "print-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "execution":
if(cljs.core.truth_(spec)){
var G__17323 = "Execution error - invalid arguments to %s at (%s).\n%s";
var G__17324 = symbol;
var G__17325 = loc;
var G__17326 = (function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__17327_17441 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__17328_17442 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__17329_17443 = true;
var _STAR_print_fn_STAR__temp_val__17330_17444 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__17329_17443);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__17330_17444);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__17229_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__17229_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__17328_17442);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__17327_17441);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})();
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__17323,G__17324,G__17325,G__17326) : format.call(null,G__17323,G__17324,G__17325,G__17326));
} else {
var G__17334 = "Execution error%s at %s(%s).\n%s\n";
var G__17335 = cause_type;
var G__17336 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__17337 = loc;
var G__17338 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__17334,G__17335,G__17336,G__17337,G__17338) : format.call(null,G__17334,G__17335,G__17336,G__17337,G__17338));
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__17274__$1))));

}
});
cljs.repl.error__GT_str = (function cljs$repl$error__GT_str(error){
return cljs.repl.ex_str(cljs.repl.ex_triage(cljs.repl.Error__GT_map(error)));
});

//# sourceMappingURL=cljs.repl.js.map
