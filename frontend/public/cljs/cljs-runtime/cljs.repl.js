goog.provide('cljs.repl');
cljs.repl.print_doc = (function cljs$repl$print_doc(p__30997){
var map__30998 = p__30997;
var map__30998__$1 = cljs.core.__destructure_map(map__30998);
var m = map__30998__$1;
var n = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__30998__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var nm = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__30998__$1,new cljs.core.Keyword(null,"name","name",1843675177));
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
var seq__31055_31633 = cljs.core.seq(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31056_31634 = null;
var count__31057_31635 = (0);
var i__31058_31636 = (0);
while(true){
if((i__31058_31636 < count__31057_31635)){
var f_31638 = chunk__31056_31634.cljs$core$IIndexed$_nth$arity$2(null,i__31058_31636);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_31638], 0));


var G__31643 = seq__31055_31633;
var G__31644 = chunk__31056_31634;
var G__31645 = count__31057_31635;
var G__31646 = (i__31058_31636 + (1));
seq__31055_31633 = G__31643;
chunk__31056_31634 = G__31644;
count__31057_31635 = G__31645;
i__31058_31636 = G__31646;
continue;
} else {
var temp__5823__auto___31647 = cljs.core.seq(seq__31055_31633);
if(temp__5823__auto___31647){
var seq__31055_31648__$1 = temp__5823__auto___31647;
if(cljs.core.chunked_seq_QMARK_(seq__31055_31648__$1)){
var c__5673__auto___31650 = cljs.core.chunk_first(seq__31055_31648__$1);
var G__31652 = cljs.core.chunk_rest(seq__31055_31648__$1);
var G__31653 = c__5673__auto___31650;
var G__31654 = cljs.core.count(c__5673__auto___31650);
var G__31655 = (0);
seq__31055_31633 = G__31652;
chunk__31056_31634 = G__31653;
count__31057_31635 = G__31654;
i__31058_31636 = G__31655;
continue;
} else {
var f_31657 = cljs.core.first(seq__31055_31648__$1);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["  ",f_31657], 0));


var G__31660 = cljs.core.next(seq__31055_31648__$1);
var G__31661 = null;
var G__31662 = (0);
var G__31663 = (0);
seq__31055_31633 = G__31660;
chunk__31056_31634 = G__31661;
count__31057_31635 = G__31662;
i__31058_31636 = G__31663;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_31665 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([arglists_31665], 0));
} else {
cljs.core.prn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first(arglists_31665)))?cljs.core.second(arglists_31665):arglists_31665)], 0));
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
var seq__31127_31672 = cljs.core.seq(new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31128_31673 = null;
var count__31129_31674 = (0);
var i__31130_31675 = (0);
while(true){
if((i__31130_31675 < count__31129_31674)){
var vec__31148_31678 = chunk__31128_31673.cljs$core$IIndexed$_nth$arity$2(null,i__31130_31675);
var name_31681 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31148_31678,(0),null);
var map__31151_31683 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31148_31678,(1),null);
var map__31151_31684__$1 = cljs.core.__destructure_map(map__31151_31683);
var doc_31685 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31151_31684__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_31686 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31151_31684__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_31681], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_31686], 0));

if(cljs.core.truth_(doc_31685)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_31685], 0));
} else {
}


var G__31689 = seq__31127_31672;
var G__31690 = chunk__31128_31673;
var G__31691 = count__31129_31674;
var G__31692 = (i__31130_31675 + (1));
seq__31127_31672 = G__31689;
chunk__31128_31673 = G__31690;
count__31129_31674 = G__31691;
i__31130_31675 = G__31692;
continue;
} else {
var temp__5823__auto___31693 = cljs.core.seq(seq__31127_31672);
if(temp__5823__auto___31693){
var seq__31127_31694__$1 = temp__5823__auto___31693;
if(cljs.core.chunked_seq_QMARK_(seq__31127_31694__$1)){
var c__5673__auto___31696 = cljs.core.chunk_first(seq__31127_31694__$1);
var G__31697 = cljs.core.chunk_rest(seq__31127_31694__$1);
var G__31698 = c__5673__auto___31696;
var G__31699 = cljs.core.count(c__5673__auto___31696);
var G__31700 = (0);
seq__31127_31672 = G__31697;
chunk__31128_31673 = G__31698;
count__31129_31674 = G__31699;
i__31130_31675 = G__31700;
continue;
} else {
var vec__31168_31701 = cljs.core.first(seq__31127_31694__$1);
var name_31702 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31168_31701,(0),null);
var map__31171_31703 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31168_31701,(1),null);
var map__31171_31704__$1 = cljs.core.__destructure_map(map__31171_31703);
var doc_31705 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31171_31704__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists_31706 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31171_31704__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",name_31702], 0));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",arglists_31706], 0));

if(cljs.core.truth_(doc_31705)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([" ",doc_31705], 0));
} else {
}


var G__31707 = cljs.core.next(seq__31127_31694__$1);
var G__31708 = null;
var G__31709 = (0);
var G__31710 = (0);
seq__31127_31672 = G__31707;
chunk__31128_31673 = G__31708;
count__31129_31674 = G__31709;
i__31130_31675 = G__31710;
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

var seq__31188 = cljs.core.seq(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"args","args",1315556576),new cljs.core.Keyword(null,"ret","ret",-468222814),new cljs.core.Keyword(null,"fn","fn",-1175266204)], null));
var chunk__31189 = null;
var count__31190 = (0);
var i__31191 = (0);
while(true){
if((i__31191 < count__31190)){
var role = chunk__31189.cljs$core$IIndexed$_nth$arity$2(null,i__31191);
var temp__5823__auto___31714__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5823__auto___31714__$1)){
var spec_31715 = temp__5823__auto___31714__$1;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_31715)], 0));
} else {
}


var G__31716 = seq__31188;
var G__31717 = chunk__31189;
var G__31718 = count__31190;
var G__31719 = (i__31191 + (1));
seq__31188 = G__31716;
chunk__31189 = G__31717;
count__31190 = G__31718;
i__31191 = G__31719;
continue;
} else {
var temp__5823__auto____$1 = cljs.core.seq(seq__31188);
if(temp__5823__auto____$1){
var seq__31188__$1 = temp__5823__auto____$1;
if(cljs.core.chunked_seq_QMARK_(seq__31188__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__31188__$1);
var G__31726 = cljs.core.chunk_rest(seq__31188__$1);
var G__31727 = c__5673__auto__;
var G__31728 = cljs.core.count(c__5673__auto__);
var G__31729 = (0);
seq__31188 = G__31726;
chunk__31189 = G__31727;
count__31190 = G__31728;
i__31191 = G__31729;
continue;
} else {
var role = cljs.core.first(seq__31188__$1);
var temp__5823__auto___31734__$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(fnspec,role);
if(cljs.core.truth_(temp__5823__auto___31734__$2)){
var spec_31736 = temp__5823__auto___31734__$2;
cljs.core.print.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(""+"\n "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(role))+":"),cljs.spec.alpha.describe(spec_31736)], 0));
} else {
}


var G__31737 = cljs.core.next(seq__31188__$1);
var G__31738 = null;
var G__31739 = (0);
var G__31740 = (0);
seq__31188 = G__31737;
chunk__31189 = G__31738;
count__31190 = G__31739;
i__31191 = G__31740;
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
var map__31319 = datafied_throwable;
var map__31319__$1 = cljs.core.__destructure_map(map__31319);
var via = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31319__$1,new cljs.core.Keyword(null,"via","via",-1904457336));
var trace = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31319__$1,new cljs.core.Keyword(null,"trace","trace",-1082747415));
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__31319__$1,new cljs.core.Keyword(null,"phase","phase",575722892),new cljs.core.Keyword(null,"execution","execution",253283524));
var map__31320 = cljs.core.last(via);
var map__31320__$1 = cljs.core.__destructure_map(map__31320);
var type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31320__$1,new cljs.core.Keyword(null,"type","type",1174270348));
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31320__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var data = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31320__$1,new cljs.core.Keyword(null,"data","data",-232669377));
var map__31321 = data;
var map__31321__$1 = cljs.core.__destructure_map(map__31321);
var problems = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31321__$1,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814));
var fn = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31321__$1,new cljs.core.Keyword("cljs.spec.alpha","fn","cljs.spec.alpha/fn",408600443));
var caller = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31321__$1,new cljs.core.Keyword("cljs.spec.test.alpha","caller","cljs.spec.test.alpha/caller",-398302390));
var map__31322 = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.first(via));
var map__31322__$1 = cljs.core.__destructure_map(map__31322);
var top_data = map__31322__$1;
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31322__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var G__31327 = phase;
var G__31327__$1 = (((G__31327 instanceof cljs.core.Keyword))?G__31327.fqn:null);
switch (G__31327__$1) {
case "read-source":
var map__31329 = data;
var map__31329__$1 = cljs.core.__destructure_map(map__31329);
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31329__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31329__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var G__31331 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(cljs.core.second(via)),top_data], 0));
var G__31331__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31331,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__31331);
var G__31331__$2 = (cljs.core.truth_((function (){var fexpr__31334 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__31334.cljs$core$IFn$_invoke$arity$1 ? fexpr__31334.cljs$core$IFn$_invoke$arity$1(source) : fexpr__31334.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__31331__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__31331__$1);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31331__$2,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__31331__$2;
}

break;
case "compile-syntax-check":
case "compilation":
case "macro-syntax-check":
case "macroexpansion":
var G__31337 = top_data;
var G__31337__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31337,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),source):G__31337);
var G__31337__$2 = (cljs.core.truth_((function (){var fexpr__31339 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__31339.cljs$core$IFn$_invoke$arity$1 ? fexpr__31339.cljs$core$IFn$_invoke$arity$1(source) : fexpr__31339.call(null,source));
})())?cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(G__31337__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397)):G__31337__$1);
var G__31337__$3 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31337__$2,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__31337__$2);
var G__31337__$4 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31337__$3,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__31337__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31337__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__31337__$4;
}

break;
case "read-eval-result":
case "print-eval-result":
var vec__31344 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31344,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31344,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31344,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31344,(3),null);
var G__31351 = top_data;
var G__31351__$1 = (cljs.core.truth_(line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31351,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),line):G__31351);
var G__31351__$2 = (cljs.core.truth_(file)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31351__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file):G__31351__$1);
var G__31351__$3 = (cljs.core.truth_((function (){var and__5140__auto__ = source__$1;
if(cljs.core.truth_(and__5140__auto__)){
return method;
} else {
return and__5140__auto__;
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31351__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null))):G__31351__$2);
var G__31351__$4 = (cljs.core.truth_(type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31351__$3,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type):G__31351__$3);
if(cljs.core.truth_(message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31351__$4,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message);
} else {
return G__31351__$4;
}

break;
case "execution":
var vec__31367 = cljs.core.first(trace);
var source__$1 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31367,(0),null);
var method = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31367,(1),null);
var file = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31367,(2),null);
var line = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__31367,(3),null);
var file__$1 = cljs.core.first(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__31314_SHARP_){
var or__5142__auto__ = (p1__31314_SHARP_ == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var fexpr__31371 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["NO_SOURCE_PATH",null,"NO_SOURCE_FILE",null], null), null);
return (fexpr__31371.cljs$core$IFn$_invoke$arity$1 ? fexpr__31371.cljs$core$IFn$_invoke$arity$1(p1__31314_SHARP_) : fexpr__31371.call(null,p1__31314_SHARP_));
}
}),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(caller),file], null)));
var err_line = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(caller);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return line;
}
})();
var G__31375 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890),type], null);
var G__31375__$1 = (cljs.core.truth_(err_line)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31375,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471),err_line):G__31375);
var G__31375__$2 = (cljs.core.truth_(message)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31375__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742),message):G__31375__$1);
var G__31375__$3 = (cljs.core.truth_((function (){var or__5142__auto__ = fn;
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
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31375__$2,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994),(function (){var or__5142__auto__ = fn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[source__$1,method],null));
}
})()):G__31375__$2);
var G__31375__$4 = (cljs.core.truth_(file__$1)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31375__$3,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397),file__$1):G__31375__$3);
if(cljs.core.truth_(problems)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__31375__$4,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595),data);
} else {
return G__31375__$4;
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__31327__$1))));

}
})(),new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358),phase);
});
/**
 * Returns a string from exception data, as produced by ex-triage.
 *   The first line summarizes the exception phase and location.
 *   The subsequent lines describe the cause.
 */
cljs.repl.ex_str = (function cljs$repl$ex_str(p__31494){
var map__31498 = p__31494;
var map__31498__$1 = cljs.core.__destructure_map(map__31498);
var triage_data = map__31498__$1;
var phase = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","phase","clojure.error/phase",275140358));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","source","clojure.error/source",-2011936397));
var line = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","line","clojure.error/line",-1816287471));
var column = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","column","clojure.error/column",304721553));
var symbol = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","symbol","clojure.error/symbol",1544821994));
var class$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","class","clojure.error/class",278435890));
var cause = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","cause","clojure.error/cause",-1879175742));
var spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__31498__$1,new cljs.core.Keyword("clojure.error","spec","clojure.error/spec",2055032595));
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
var G__31526 = phase;
var G__31526__$1 = (((G__31526 instanceof cljs.core.Keyword))?G__31526.fqn:null);
switch (G__31526__$1) {
case "read-source":
return (format.cljs$core$IFn$_invoke$arity$3 ? format.cljs$core$IFn$_invoke$arity$3("Syntax error reading source at (%s).\n%s\n",loc,cause) : format.call(null,"Syntax error reading source at (%s).\n%s\n",loc,cause));

break;
case "macro-syntax-check":
var G__31549 = "Syntax error macroexpanding %sat (%s).\n%s";
var G__31550 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__31551 = loc;
var G__31552 = (cljs.core.truth_(spec)?(function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__31553_31822 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__31554_31823 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__31555_31824 = true;
var _STAR_print_fn_STAR__temp_val__31556_31825 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__31555_31824);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__31556_31825);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__31485_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__31485_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__31554_31823);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__31553_31822);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})():(format.cljs$core$IFn$_invoke$arity$2 ? format.cljs$core$IFn$_invoke$arity$2("%s\n",cause) : format.call(null,"%s\n",cause)));
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__31549,G__31550,G__31551,G__31552) : format.call(null,G__31549,G__31550,G__31551,G__31552));

break;
case "macroexpansion":
var G__31570 = "Unexpected error%s macroexpanding %sat (%s).\n%s\n";
var G__31571 = cause_type;
var G__31572 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__31573 = loc;
var G__31574 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__31570,G__31571,G__31572,G__31573,G__31574) : format.call(null,G__31570,G__31571,G__31572,G__31573,G__31574));

break;
case "compile-syntax-check":
var G__31577 = "Syntax error%s compiling %sat (%s).\n%s\n";
var G__31578 = cause_type;
var G__31579 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__31580 = loc;
var G__31581 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__31577,G__31578,G__31579,G__31580,G__31581) : format.call(null,G__31577,G__31578,G__31579,G__31580,G__31581));

break;
case "compilation":
var G__31582 = "Unexpected error%s compiling %sat (%s).\n%s\n";
var G__31583 = cause_type;
var G__31584 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__31585 = loc;
var G__31586 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__31582,G__31583,G__31584,G__31585,G__31586) : format.call(null,G__31582,G__31583,G__31584,G__31585,G__31586));

break;
case "read-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error reading eval result%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "print-eval-result":
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5("Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause) : format.call(null,"Error printing return value%s at %s (%s).\n%s\n",cause_type,symbol,loc,cause));

break;
case "execution":
if(cljs.core.truth_(spec)){
var G__31588 = "Execution error - invalid arguments to %s at (%s).\n%s";
var G__31589 = symbol;
var G__31590 = loc;
var G__31591 = (function (){var sb__5795__auto__ = (new goog.string.StringBuffer());
var _STAR_print_newline_STAR__orig_val__31593_31834 = cljs.core._STAR_print_newline_STAR_;
var _STAR_print_fn_STAR__orig_val__31596_31835 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR__temp_val__31597_31836 = true;
var _STAR_print_fn_STAR__temp_val__31598_31837 = (function (x__5796__auto__){
return sb__5795__auto__.append(x__5796__auto__);
});
(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__temp_val__31597_31836);

(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__temp_val__31598_31837);

try{cljs.spec.alpha.explain_out(cljs.core.update.cljs$core$IFn$_invoke$arity$3(spec,new cljs.core.Keyword("cljs.spec.alpha","problems","cljs.spec.alpha/problems",447400814),(function (probs){
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__31491_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(p1__31491_SHARP_,new cljs.core.Keyword(null,"in","in",-1531184865));
}),probs);
}))
);
}finally {(cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR__orig_val__31596_31835);

(cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR__orig_val__31593_31834);
}
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sb__5795__auto__));
})();
return (format.cljs$core$IFn$_invoke$arity$4 ? format.cljs$core$IFn$_invoke$arity$4(G__31588,G__31589,G__31590,G__31591) : format.call(null,G__31588,G__31589,G__31590,G__31591));
} else {
var G__31606 = "Execution error%s at %s(%s).\n%s\n";
var G__31607 = cause_type;
var G__31608 = (cljs.core.truth_(symbol)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(symbol)+" "):"");
var G__31609 = loc;
var G__31610 = cause;
return (format.cljs$core$IFn$_invoke$arity$5 ? format.cljs$core$IFn$_invoke$arity$5(G__31606,G__31607,G__31608,G__31609,G__31610) : format.call(null,G__31606,G__31607,G__31608,G__31609,G__31610));
}

break;
default:
throw (new Error((""+"No matching clause: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__31526__$1))));

}
});
cljs.repl.error__GT_str = (function cljs$repl$error__GT_str(error){
return cljs.repl.ex_str(cljs.repl.ex_triage(cljs.repl.Error__GT_map(error)));
});

//# sourceMappingURL=cljs.repl.js.map
