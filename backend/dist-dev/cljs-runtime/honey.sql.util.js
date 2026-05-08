import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('honey.sql.util');
/**
 * More efficient implementation of `clojure.core/str` because it has more
 *   non-variadic arities. Optimization is Clojure-only, on other platforms it
 *   reverts back to `clojure.core/str`.
 */
honey.sql.util.str = (function honey$sql$util$str(var_args){
var G__524923 = arguments.length;
switch (G__524923) {
case 0:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
var args_arr__5901__auto__ = [];
var len__5876__auto___524986 = arguments.length;
var i__5877__auto___524987 = (0);
while(true){
if((i__5877__auto___524987 < len__5876__auto___524986)){
args_arr__5901__auto__.push((arguments[i__5877__auto___524987]));

var G__524988 = (i__5877__auto___524987 + (1));
i__5877__auto___524987 = G__524988;
continue;
} else {
}
break;
}

var argseq__5902__auto__ = ((((5) < args_arr__5901__auto__.length))?(new cljs.core.IndexedSeq(args_arr__5901__auto__.slice((5)),(0),null)):null);
return honey.sql.util.str.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),argseq__5902__auto__);

}
});

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$0 = (function (){
return "";
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$1 = (function (a){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(a));
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(a)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b));
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$3 = (function (a,b,c){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(a)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(c));
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$4 = (function (a,b,c,d){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(a)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(c)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(d));
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$5 = (function (a,b,c,d,e){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(a)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(b)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(c)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(d)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(e));
}));

(honey.sql.util.str.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,c,d,e,more){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$variadic(cljs.core.str,a,b,c,d,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([e,more], 0));
}));

/** @this {Function} */
(honey.sql.util.str.cljs$lang$applyTo = (function (seq524917){
var G__524918 = cljs.core.first(seq524917);
var seq524917__$1 = cljs.core.next(seq524917);
var G__524919 = cljs.core.first(seq524917__$1);
var seq524917__$2 = cljs.core.next(seq524917__$1);
var G__524920 = cljs.core.first(seq524917__$2);
var seq524917__$3 = cljs.core.next(seq524917__$2);
var G__524921 = cljs.core.first(seq524917__$3);
var seq524917__$4 = cljs.core.next(seq524917__$3);
var G__524922 = cljs.core.first(seq524917__$4);
var seq524917__$5 = cljs.core.next(seq524917__$4);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__524918,G__524919,G__524920,G__524921,G__524922,seq524917__$5);
}));

(honey.sql.util.str.cljs$lang$maxFixedArity = (5));

/**
 * More efficient implementation of `clojure.string/join`. May accept a transducer
 *   `xform` to perform operations on each element before combining them together
 *   into a string. Clojure-only, delegates to `clojure.string/join` on other
 *   platforms.
 */
honey.sql.util.join = (function honey$sql$util$join(var_args){
var G__524953 = arguments.length;
switch (G__524953) {
case 2:
return honey.sql.util.join.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return honey.sql.util.join.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(honey.sql.util.join.cljs$core$IFn$_invoke$arity$2 = (function (separator,coll){
return honey.sql.util.join.cljs$core$IFn$_invoke$arity$3(separator,cljs.core.identity,coll);
}));

(honey.sql.util.join.cljs$core$IFn$_invoke$arity$3 = (function (separator,xform,coll){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2(separator,cljs.core.transduce.cljs$core$IFn$_invoke$arity$4(xform,cljs.core.conj,cljs.core.PersistentVector.EMPTY,coll));
}));

(honey.sql.util.join.cljs$lang$maxFixedArity = 3);

/**
 * More efficient implementation of `clojure.string/split` for cases when a
 *   literal string (not regex) is used as a separator, and for cases where the
 *   separator is not present in the haystack at all.
 */
honey.sql.util.split_by_separator = (function honey$sql$util$split_by_separator(s,sep){
var start = (0);
var res = cljs.core.PersistentVector.EMPTY;
while(true){
var temp__5827__auto__ = clojure.string.index_of.cljs$core$IFn$_invoke$arity$3(s,sep,start);
if((temp__5827__auto__ == null)){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(start,(0))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [s], null);
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(res,cljs.core.subs.cljs$core$IFn$_invoke$arity$2(s,start));
}
} else {
var sep_idx = temp__5827__auto__;
var sep_idx__$1 = cljs.core.long$(sep_idx);
var G__525003 = (sep_idx__$1 + (1));
var G__525004 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(res,cljs.core.subs.cljs$core$IFn$_invoke$arity$3(s,start,sep_idx__$1));
start = G__525003;
res = G__525004;
continue;
}
break;
}
});
/**
 * An extension of `clojure.core/into` that accepts multiple "from" arguments.
 *   Doesn't support `xform`.
 */
honey.sql.util.into_STAR_ = (function honey$sql$util$into_STAR_(var_args){
var G__524970 = arguments.length;
switch (G__524970) {
case 2:
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (to,from1){
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$5(to,from1,null,null,null);
}));

(honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$3 = (function (to,from1,from2){
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$5(to,from1,from2,null,null);
}));

(honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$4 = (function (to,from1,from2,from3){
return honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$5(to,from1,from2,from3,null);
}));

(honey.sql.util.into_STAR_.cljs$core$IFn$_invoke$arity$5 = (function (to,from1,from2,from3,from4){
if(cljs.core.truth_((function (){var or__5142__auto__ = from1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = from2;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = from3;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return from4;
}
}
}
})())){
var to_SINGLEQUOTE_ = cljs.core.transient$(to);
var to_SINGLEQUOTE___$1 = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core.conj_BANG_,to_SINGLEQUOTE_,from1);
var to_SINGLEQUOTE___$2 = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core.conj_BANG_,to_SINGLEQUOTE___$1,from2);
var to_SINGLEQUOTE___$3 = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core.conj_BANG_,to_SINGLEQUOTE___$2,from3);
var to_SINGLEQUOTE___$4 = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core.conj_BANG_,to_SINGLEQUOTE___$3,from4);
return cljs.core.persistent_BANG_(to_SINGLEQUOTE___$4);
} else {
return to;
}
}));

(honey.sql.util.into_STAR_.cljs$lang$maxFixedArity = 5);


//# sourceMappingURL=honey.sql.util.js.map
