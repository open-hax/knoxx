import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('malli.registry');
/**
 * @define {string}
 * @type {string}
 */
malli.registry.mode = goog.define("malli.registry.mode","default");
/**
 * @define {string}
 * @type {string}
 */
malli.registry.type = goog.define("malli.registry.type","default");

/**
 * @interface
 */
malli.registry.Registry = function(){};

var malli$registry$Registry$_schema$dyn_516885 = (function (this$,type){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.registry._schema[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(this$,type) : m__5499__auto__.call(null,this$,type));
} else {
var m__5497__auto__ = (malli.registry._schema["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(this$,type) : m__5497__auto__.call(null,this$,type));
} else {
throw cljs.core.missing_protocol("Registry.-schema",this$);
}
}
});
/**
 * returns the schema from a registry
 */
malli.registry._schema = (function malli$registry$_schema(this$,type){
if((((!((this$ == null)))) && ((!((this$.malli$registry$Registry$_schema$arity$2 == null)))))){
return this$.malli$registry$Registry$_schema$arity$2(this$,type);
} else {
return malli$registry$Registry$_schema$dyn_516885(this$,type);
}
});

var malli$registry$Registry$_schemas$dyn_516886 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.registry._schemas[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5499__auto__.call(null,this$));
} else {
var m__5497__auto__ = (malli.registry._schemas["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5497__auto__.call(null,this$));
} else {
throw cljs.core.missing_protocol("Registry.-schemas",this$);
}
}
});
/**
 * returns all schemas from a registry
 */
malli.registry._schemas = (function malli$registry$_schemas(this$){
if((((!((this$ == null)))) && ((!((this$.malli$registry$Registry$_schemas$arity$1 == null)))))){
return this$.malli$registry$Registry$_schemas$arity$1(this$);
} else {
return malli$registry$Registry$_schemas$dyn_516886(this$);
}
});

malli.registry.registry_QMARK_ = (function malli$registry$registry_QMARK_(x){
if((!((x == null)))){
if(((false) || ((cljs.core.PROTOCOL_SENTINEL === x.malli$registry$Registry$)))){
return true;
} else {
return false;
}
} else {
return false;
}
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516797 = (function (m,fm,meta516798){
this.m = m;
this.fm = fm;
this.meta516798 = meta516798;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516797.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516799,meta516798__$1){
var self__ = this;
var _516799__$1 = this;
return (new malli.registry.t_malli$registry516797(self__.m,self__.fm,meta516798__$1));
}));

(malli.registry.t_malli$registry516797.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516799){
var self__ = this;
var _516799__$1 = this;
return self__.meta516798;
}));

(malli.registry.t_malli$registry516797.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516797.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return self__.fm.get(type);
}));

(malli.registry.t_malli$registry516797.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.m;
}));

(malli.registry.t_malli$registry516797.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"m","m",-1021758608,null),new cljs.core.Symbol(null,"fm","fm",-1190690268,null),new cljs.core.Symbol(null,"meta516798","meta516798",58712443,null)], null);
}));

(malli.registry.t_malli$registry516797.cljs$lang$type = true);

(malli.registry.t_malli$registry516797.cljs$lang$ctorStr = "malli.registry/t_malli$registry516797");

(malli.registry.t_malli$registry516797.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516797");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516797.
 */
malli.registry.__GT_t_malli$registry516797 = (function malli$registry$__GT_t_malli$registry516797(m,fm,meta516798){
return (new malli.registry.t_malli$registry516797(m,fm,meta516798));
});


malli.registry.fast_registry = (function malli$registry$fast_registry(m){
var fm = m;
return (new malli.registry.t_malli$registry516797(m,fm,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516802 = (function (m,meta516803){
this.m = m;
this.meta516803 = meta516803;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516802.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516804,meta516803__$1){
var self__ = this;
var _516804__$1 = this;
return (new malli.registry.t_malli$registry516802(self__.m,meta516803__$1));
}));

(malli.registry.t_malli$registry516802.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516804){
var self__ = this;
var _516804__$1 = this;
return self__.meta516803;
}));

(malli.registry.t_malli$registry516802.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516802.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return (self__.m.cljs$core$IFn$_invoke$arity$1 ? self__.m.cljs$core$IFn$_invoke$arity$1(type) : self__.m.call(null,type));
}));

(malli.registry.t_malli$registry516802.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.m;
}));

(malli.registry.t_malli$registry516802.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"m","m",-1021758608,null),new cljs.core.Symbol(null,"meta516803","meta516803",1165205608,null)], null);
}));

(malli.registry.t_malli$registry516802.cljs$lang$type = true);

(malli.registry.t_malli$registry516802.cljs$lang$ctorStr = "malli.registry/t_malli$registry516802");

(malli.registry.t_malli$registry516802.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516802");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516802.
 */
malli.registry.__GT_t_malli$registry516802 = (function malli$registry$__GT_t_malli$registry516802(m,meta516803){
return (new malli.registry.t_malli$registry516802(m,meta516803));
});


malli.registry.simple_registry = (function malli$registry$simple_registry(m){
return (new malli.registry.t_malli$registry516802(m,cljs.core.PersistentArrayMap.EMPTY));
});
malli.registry.registry = (function malli$registry$registry(_QMARK_registry){
if((_QMARK_registry == null)){
return null;
} else {
if(malli.registry.registry_QMARK_(_QMARK_registry)){
return _QMARK_registry;
} else {
if(cljs.core.map_QMARK_(_QMARK_registry)){
return malli.registry.simple_registry(_QMARK_registry);
} else {
if((((!((_QMARK_registry == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === _QMARK_registry.malli$registry$Registry$))))?true:(((!_QMARK_registry.cljs$lang$protocol_mask$partition$))?cljs.core.native_satisfies_QMARK_(malli.registry.Registry,_QMARK_registry):false)):cljs.core.native_satisfies_QMARK_(malli.registry.Registry,_QMARK_registry))){
return _QMARK_registry;
} else {
return null;
}
}
}
}
});
malli.registry.registry_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(malli.registry.simple_registry(cljs.core.PersistentArrayMap.EMPTY));
malli.registry.set_default_registry_BANG_ = (function malli$registry$set_default_registry_BANG_(_QMARK_registry){
if((!((malli.registry.mode === "strict")))){
return cljs.core.reset_BANG_(malli.registry.registry_STAR_,malli.registry.registry(_QMARK_registry));
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("can't set default registry, invalid mode",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"mode","mode",654403691),malli.registry.mode,new cljs.core.Keyword(null,"type","type",1174270348),malli.registry.type], null));
}
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516811 = (function (meta516812){
this.meta516812 = meta516812;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516811.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516813,meta516812__$1){
var self__ = this;
var _516813__$1 = this;
return (new malli.registry.t_malli$registry516811(meta516812__$1));
}));

(malli.registry.t_malli$registry516811.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516813){
var self__ = this;
var _516813__$1 = this;
return self__.meta516812;
}));

(malli.registry.t_malli$registry516811.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516811.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema(cljs.core.deref(malli.registry.registry_STAR_),type);
}));

(malli.registry.t_malli$registry516811.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas(cljs.core.deref(malli.registry.registry_STAR_));
}));

(malli.registry.t_malli$registry516811.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"meta516812","meta516812",1636670674,null)], null);
}));

(malli.registry.t_malli$registry516811.cljs$lang$type = true);

(malli.registry.t_malli$registry516811.cljs$lang$ctorStr = "malli.registry/t_malli$registry516811");

(malli.registry.t_malli$registry516811.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516811");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516811.
 */
malli.registry.__GT_t_malli$registry516811 = (function malli$registry$__GT_t_malli$registry516811(meta516812){
return (new malli.registry.t_malli$registry516811(meta516812));
});


malli.registry.custom_default_registry = (function malli$registry$custom_default_registry(){
return (new malli.registry.t_malli$registry516811(cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516820 = (function (_QMARK_registries,registries,meta516821){
this._QMARK_registries = _QMARK_registries;
this.registries = registries;
this.meta516821 = meta516821;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516820.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516822,meta516821__$1){
var self__ = this;
var _516822__$1 = this;
return (new malli.registry.t_malli$registry516820(self__._QMARK_registries,self__.registries,meta516821__$1));
}));

(malli.registry.t_malli$registry516820.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516822){
var self__ = this;
var _516822__$1 = this;
return self__.meta516821;
}));

(malli.registry.t_malli$registry516820.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516820.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return cljs.core.some((function (p1__516816_SHARP_){
return malli.registry._schema(p1__516816_SHARP_,type);
}),self__.registries);
}));

(malli.registry.t_malli$registry516820.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$2(cljs.core.merge,cljs.core.map.cljs$core$IFn$_invoke$arity$2(malli.registry._schemas,cljs.core.reverse(self__.registries)));
}));

(malli.registry.t_malli$registry516820.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"?registries","?registries",2135368100,null),new cljs.core.Symbol(null,"registries","registries",-1366064418,null),new cljs.core.Symbol(null,"meta516821","meta516821",1286396900,null)], null);
}));

(malli.registry.t_malli$registry516820.cljs$lang$type = true);

(malli.registry.t_malli$registry516820.cljs$lang$ctorStr = "malli.registry/t_malli$registry516820");

(malli.registry.t_malli$registry516820.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516820");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516820.
 */
malli.registry.__GT_t_malli$registry516820 = (function malli$registry$__GT_t_malli$registry516820(_QMARK_registries,registries,meta516821){
return (new malli.registry.t_malli$registry516820(_QMARK_registries,registries,meta516821));
});


malli.registry.composite_registry = (function malli$registry$composite_registry(var_args){
var args__5882__auto__ = [];
var len__5876__auto___516927 = arguments.length;
var i__5877__auto___516928 = (0);
while(true){
if((i__5877__auto___516928 < len__5876__auto___516927)){
args__5882__auto__.push((arguments[i__5877__auto___516928]));

var G__516929 = (i__5877__auto___516928 + (1));
i__5877__auto___516928 = G__516929;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return malli.registry.composite_registry.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(malli.registry.composite_registry.cljs$core$IFn$_invoke$arity$variadic = (function (_QMARK_registries){
var registries = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(malli.registry.registry,_QMARK_registries);
return (new malli.registry.t_malli$registry516820(_QMARK_registries,registries,cljs.core.PersistentArrayMap.EMPTY));
}));

(malli.registry.composite_registry.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(malli.registry.composite_registry.cljs$lang$applyTo = (function (seq516817){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq516817));
}));


/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516827 = (function (db,meta516828){
this.db = db;
this.meta516828 = meta516828;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516827.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516829,meta516828__$1){
var self__ = this;
var _516829__$1 = this;
return (new malli.registry.t_malli$registry516827(self__.db,meta516828__$1));
}));

(malli.registry.t_malli$registry516827.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516829){
var self__ = this;
var _516829__$1 = this;
return self__.meta516828;
}));

(malli.registry.t_malli$registry516827.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516827.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema(malli.registry.registry(cljs.core.deref(self__.db)),type);
}));

(malli.registry.t_malli$registry516827.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas(malli.registry.registry(cljs.core.deref(self__.db)));
}));

(malli.registry.t_malli$registry516827.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"db","db",-1661185010,null),new cljs.core.Symbol(null,"meta516828","meta516828",-2066613933,null)], null);
}));

(malli.registry.t_malli$registry516827.cljs$lang$type = true);

(malli.registry.t_malli$registry516827.cljs$lang$ctorStr = "malli.registry/t_malli$registry516827");

(malli.registry.t_malli$registry516827.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516827");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516827.
 */
malli.registry.__GT_t_malli$registry516827 = (function malli$registry$__GT_t_malli$registry516827(db,meta516828){
return (new malli.registry.t_malli$registry516827(db,meta516828));
});


malli.registry.mutable_registry = (function malli$registry$mutable_registry(db){
return (new malli.registry.t_malli$registry516827(db,cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516839 = (function (meta516840){
this.meta516840 = meta516840;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516839.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516841,meta516840__$1){
var self__ = this;
var _516841__$1 = this;
return (new malli.registry.t_malli$registry516839(meta516840__$1));
}));

(malli.registry.t_malli$registry516839.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516841){
var self__ = this;
var _516841__$1 = this;
return self__.meta516840;
}));

(malli.registry.t_malli$registry516839.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516839.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
if(cljs.core.var_QMARK_(type)){
return cljs.core.deref(type);
} else {
return null;
}
}));

(malli.registry.t_malli$registry516839.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return null;
}));

(malli.registry.t_malli$registry516839.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"meta516840","meta516840",-2045234715,null)], null);
}));

(malli.registry.t_malli$registry516839.cljs$lang$type = true);

(malli.registry.t_malli$registry516839.cljs$lang$ctorStr = "malli.registry/t_malli$registry516839");

(malli.registry.t_malli$registry516839.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516839");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516839.
 */
malli.registry.__GT_t_malli$registry516839 = (function malli$registry$__GT_t_malli$registry516839(meta516840){
return (new malli.registry.t_malli$registry516839(meta516840));
});


malli.registry.var_registry = (function malli$registry$var_registry(){
return (new malli.registry.t_malli$registry516839(cljs.core.PersistentArrayMap.EMPTY));
});
malli.registry._STAR_registry_STAR_ = cljs.core.PersistentArrayMap.EMPTY;

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516853 = (function (meta516854){
this.meta516854 = meta516854;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516853.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516855,meta516854__$1){
var self__ = this;
var _516855__$1 = this;
return (new malli.registry.t_malli$registry516853(meta516854__$1));
}));

(malli.registry.t_malli$registry516853.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516855){
var self__ = this;
var _516855__$1 = this;
return self__.meta516854;
}));

(malli.registry.t_malli$registry516853.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516853.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,type){
var self__ = this;
var ___$1 = this;
return malli.registry._schema(malli.registry.registry(malli.registry._STAR_registry_STAR_),type);
}));

(malli.registry.t_malli$registry516853.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return malli.registry._schemas(malli.registry.registry(malli.registry._STAR_registry_STAR_));
}));

(malli.registry.t_malli$registry516853.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"meta516854","meta516854",-874515901,null)], null);
}));

(malli.registry.t_malli$registry516853.cljs$lang$type = true);

(malli.registry.t_malli$registry516853.cljs$lang$ctorStr = "malli.registry/t_malli$registry516853");

(malli.registry.t_malli$registry516853.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516853");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516853.
 */
malli.registry.__GT_t_malli$registry516853 = (function malli$registry$__GT_t_malli$registry516853(meta516854){
return (new malli.registry.t_malli$registry516853(meta516854));
});


malli.registry.dynamic_registry = (function malli$registry$dynamic_registry(){
return (new malli.registry.t_malli$registry516853(cljs.core.PersistentArrayMap.EMPTY));
});

/**
* @constructor
 * @implements {malli.registry.Registry}
 * @implements {cljs.core.IMeta}
 * @implements {cljs.core.IWithMeta}
*/
malli.registry.t_malli$registry516860 = (function (default_registry,provider,cache_STAR_,registry_STAR_,meta516861){
this.default_registry = default_registry;
this.provider = provider;
this.cache_STAR_ = cache_STAR_;
this.registry_STAR_ = registry_STAR_;
this.meta516861 = meta516861;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
});
(malli.registry.t_malli$registry516860.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_516862,meta516861__$1){
var self__ = this;
var _516862__$1 = this;
return (new malli.registry.t_malli$registry516860(self__.default_registry,self__.provider,self__.cache_STAR_,self__.registry_STAR_,meta516861__$1));
}));

(malli.registry.t_malli$registry516860.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_516862){
var self__ = this;
var _516862__$1 = this;
return self__.meta516861;
}));

(malli.registry.t_malli$registry516860.prototype.malli$registry$Registry$ = cljs.core.PROTOCOL_SENTINEL);

(malli.registry.t_malli$registry516860.prototype.malli$registry$Registry$_schema$arity$2 = (function (_,name){
var self__ = this;
var ___$1 = this;
var or__5142__auto__ = (function (){var fexpr__516866 = cljs.core.deref(self__.cache_STAR_);
return (fexpr__516866.cljs$core$IFn$_invoke$arity$1 ? fexpr__516866.cljs$core$IFn$_invoke$arity$1(name) : fexpr__516866.call(null,name));
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = (function (){var G__516872 = name;
var G__516873 = cljs.core.deref(self__.registry_STAR_);
return (self__.provider.cljs$core$IFn$_invoke$arity$2 ? self__.provider.cljs$core$IFn$_invoke$arity$2(G__516872,G__516873) : self__.provider.call(null,G__516872,G__516873));
})();
if(cljs.core.truth_(temp__5825__auto__)){
var schema = temp__5825__auto__;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(self__.cache_STAR_,cljs.core.assoc,name,schema);

return schema;
} else {
return null;
}
}
}));

(malli.registry.t_malli$registry516860.prototype.malli$registry$Registry$_schemas$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref(self__.cache_STAR_);
}));

(malli.registry.t_malli$registry516860.getBasis = (function (){
return new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"default-registry","default-registry",732204441,null),new cljs.core.Symbol(null,"provider","provider",1338474627,null),new cljs.core.Symbol(null,"cache*","cache*",-548597526,null),new cljs.core.Symbol(null,"registry*","registry*",-268031273,null),new cljs.core.Symbol(null,"meta516861","meta516861",-1819513391,null)], null);
}));

(malli.registry.t_malli$registry516860.cljs$lang$type = true);

(malli.registry.t_malli$registry516860.cljs$lang$ctorStr = "malli.registry/t_malli$registry516860");

(malli.registry.t_malli$registry516860.cljs$lang$ctorPrWriter = (function (this__5434__auto__,writer__5435__auto__,opt__5436__auto__){
return cljs.core._write(writer__5435__auto__,"malli.registry/t_malli$registry516860");
}));

/**
 * Positional factory function for malli.registry/t_malli$registry516860.
 */
malli.registry.__GT_t_malli$registry516860 = (function malli$registry$__GT_t_malli$registry516860(default_registry,provider,cache_STAR_,registry_STAR_,meta516861){
return (new malli.registry.t_malli$registry516860(default_registry,provider,cache_STAR_,registry_STAR_,meta516861));
});


malli.registry.lazy_registry = (function malli$registry$lazy_registry(default_registry,provider){
var cache_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var registry_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(default_registry);
return cljs.core.reset_BANG_(registry_STAR_,malli.registry.composite_registry.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([default_registry,(new malli.registry.t_malli$registry516860(default_registry,provider,cache_STAR_,registry_STAR_,cljs.core.PersistentArrayMap.EMPTY))], 0)));
});
/**
 * finds a schema from a registry
 */
malli.registry.schema = (function malli$registry$schema(registry,type){
return malli.registry._schema(registry,type);
});
/**
 * finds all schemas from a registry
 */
malli.registry.schemas = (function malli$registry$schemas(registry){
return malli.registry._schemas(registry);
});

//# sourceMappingURL=malli.registry.js.map
