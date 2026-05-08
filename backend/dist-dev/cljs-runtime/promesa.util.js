import "./cljs_env.js";
import "./cljs.core.js";
import "./promesa.protocols.js";
goog.provide('promesa.util');
/**
 * Create a handler, mainly for combine two separate functions
 *   into a single callbale.
 */
promesa.util.handler = (function promesa$util$handler(fv,fc){
return (function (v,c){
if(cljs.core.truth_(c)){
return (fc.cljs$core$IFn$_invoke$arity$1 ? fc.cljs$core$IFn$_invoke$arity$1(c) : fc.call(null,c));
} else {
return (fv.cljs$core$IFn$_invoke$arity$1 ? fv.cljs$core$IFn$_invoke$arity$1(v) : fv.call(null,v));
}
});
});
promesa.util.has_method_QMARK_ = (function promesa$util$has_method_QMARK_(klass,name){
var methods$ = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (method){
return method.getName();
})),klass.getDeclaredMethods());
return cljs.core.contains_QMARK_(methods$,name);
});
promesa.util.maybe_deref = (function promesa$util$maybe_deref(o){
if(cljs.core.delay_QMARK_(o)){
return cljs.core.deref(o);
} else {
return o;
}
});

/**
* @constructor
 * @implements {promesa.protocols.ILock}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
promesa.util.t_promesa$util521766 = (function (meta521767){
this.meta521767 = meta521767;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(promesa.util.t_promesa$util521766.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_521771,meta521767__$1){
var self__ = this;
var _521771__$1 = this;
return (new promesa.util.t_promesa$util521766(meta521767__$1));
}));

(promesa.util.t_promesa$util521766.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_521771){
var self__ = this;
var _521771__$1 = this;
return self__.meta521767;
}));

(promesa.util.t_promesa$util521766.prototype.promesa$protocols$ILock$ = cljs.core.PROTOCOL_SENTINEL);

(promesa.util.t_promesa$util521766.prototype.promesa$protocols$ILock$_lock_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(promesa.util.t_promesa$util521766.prototype.promesa$protocols$ILock$_unlock_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(promesa.util.t_promesa$util521766.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"meta521767","meta521767",2057350387,null)], null);
}));

(promesa.util.t_promesa$util521766.cljs$lang$type = true);

(promesa.util.t_promesa$util521766.cljs$lang$ctorStr = "promesa.util/t_promesa$util521766");

(promesa.util.t_promesa$util521766.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"promesa.util/t_promesa$util521766");
}));

/**
 * Positional factory function for promesa.util/t_promesa$util521766.
 */
promesa.util.__GT_t_promesa$util521766 = (function promesa$util$__GT_t_promesa$util521766(meta521767){
return (new promesa.util.t_promesa$util521766(meta521767));
});


promesa.util.mutex = (function promesa$util$mutex(){
return (new promesa.util.t_promesa$util521766(cljs.core.PersistentArrayMap.EMPTY));
});
promesa.util.try_STAR_ = (function promesa$util$try_STAR_(f,on_error){
try{return (f.cljs$core$IFn$_invoke$arity$0 ? f.cljs$core$IFn$_invoke$arity$0() : f.call(null));
}catch (e521814){var e = e521814;
return (on_error.cljs$core$IFn$_invoke$arity$1 ? on_error.cljs$core$IFn$_invoke$arity$1(e) : on_error.call(null,e));
}});

//# sourceMappingURL=promesa.util.js.map
