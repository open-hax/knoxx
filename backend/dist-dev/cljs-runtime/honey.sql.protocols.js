import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('honey.sql.protocols');

/**
 * @interface
 */
honey.sql.protocols.InlineValue = function(){};

var honey$sql$protocols$InlineValue$sqlize$dyn_524912 = (function (this$){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (honey.sql.protocols.sqlize[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5499__auto__.call(null,this$));
} else {
var m__5497__auto__ = (honey.sql.protocols.sqlize["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(this$) : m__5497__auto__.call(null,this$));
} else {
throw cljs.core.missing_protocol("InlineValue.sqlize",this$);
}
}
});
/**
 * Render value inline in a SQL string.
 */
honey.sql.protocols.sqlize = (function honey$sql$protocols$sqlize(this$){
var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.meta(this$),cljs.core.with_meta(new cljs.core.Symbol("honey.sql.protocols","sqlize","honey.sql.protocols/sqlize",160108832,null),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword("cljs.analyzer","no-resolve","cljs.analyzer/no-resolve",-1872351017),true], null)));
if(temp__5823__auto__){
var meta_impl__5500__auto__ = temp__5823__auto__;
return (meta_impl__5500__auto__.cljs$core$IFn$_invoke$arity$1 ? meta_impl__5500__auto__.cljs$core$IFn$_invoke$arity$1(this$) : meta_impl__5500__auto__.call(null,this$));
} else {
if((((!((this$ == null)))) && ((!((this$.honey$sql$protocols$InlineValue$sqlize$arity$1 == null)))))){
return this$.honey$sql$protocols$InlineValue$sqlize$arity$1(this$);
} else {
return honey$sql$protocols$InlineValue$sqlize$dyn_524912(this$);
}
}
});


//# sourceMappingURL=honey.sql.protocols.js.map
