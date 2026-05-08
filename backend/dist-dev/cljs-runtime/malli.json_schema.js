import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.set.js";
import "./clojure.string.js";
import "./malli.core.js";
goog.provide('malli.json_schema');

/**
 * @interface
 */
malli.json_schema.JsonSchema = function(){};

var malli$json_schema$JsonSchema$_accept$dyn_521267 = (function (this$,children,options){
var x__5498__auto__ = (((this$ == null))?null:this$);
var m__5499__auto__ = (malli.json_schema._accept[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(this$,children,options) : m__5499__auto__.call(null,this$,children,options));
} else {
var m__5497__auto__ = (malli.json_schema._accept["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(this$,children,options) : m__5497__auto__.call(null,this$,children,options));
} else {
throw cljs.core.missing_protocol("JsonSchema.-accept",this$);
}
}
});
/**
 * transforms schema to JSON Schema
 */
malli.json_schema._accept = (function malli$json_schema$_accept(this$,children,options){
if((((!((this$ == null)))) && ((!((this$.malli$json_schema$JsonSchema$_accept$arity$3 == null)))))){
return this$.malli$json_schema$JsonSchema$_accept$arity$3(this$,children,options);
} else {
return malli$json_schema$JsonSchema$_accept$dyn_521267(this$,children,options);
}
});

malli.json_schema._ref = (function malli$json_schema$_ref(schema,p__521069){
var map__521070 = p__521069;
var map__521070__$1 = cljs.core.__destructure_map(map__521070);
var options = map__521070__$1;
var transform = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521070__$1,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521));
var definitions = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521070__$1,new cljs.core.Keyword("malli.json-schema","definitions","malli.json-schema/definitions",1666750607));
var definitions_path = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__521070__$1,new cljs.core.Keyword("malli.json-schema","definitions-path","malli.json-schema/definitions-path",10213173),"#/definitions/");
var ref = (function (){var $ = malli.core._ref(schema);
if(cljs.core.var_QMARK_($)){
var map__521075 = cljs.core.meta($);
var map__521075__$1 = cljs.core.__destructure_map(map__521075);
var ns = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521075__$1,new cljs.core.Keyword(null,"ns","ns",441598760));
var name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521075__$1,new cljs.core.Keyword(null,"name","name",1843675177));
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)))));
} else {
if(cljs.core.qualified_ident_QMARK_($)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.namespace($))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name($)));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1($));

}
}
})();
if(cljs.core.contains_QMARK_(cljs.core.deref(definitions),ref)){
} else {
var child_521268 = malli.core.deref.cljs$core$IFn$_invoke$arity$1(schema);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(definitions,cljs.core.assoc,ref,new cljs.core.Keyword("malli.json-schema","recursion-stopper","malli.json-schema/recursion-stopper",1157242039));

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(definitions,cljs.core.assoc,ref,(transform.cljs$core$IFn$_invoke$arity$2 ? transform.cljs$core$IFn$_invoke$arity$2(child_521268,options) : transform.call(null,child_521268,options)));
}

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"$ref","$ref",841290683),cljs.core.apply.cljs$core$IFn$_invoke$arity$3(cljs.core.str,definitions_path,clojure.string.replace(ref,/\//,"~1"))], null);
});
malli.json_schema._schema = (function malli$json_schema$_schema(schema,p__521082){
var map__521084 = p__521082;
var map__521084__$1 = cljs.core.__destructure_map(map__521084);
var options = map__521084__$1;
var transform = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521084__$1,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521));
if(cljs.core.truth_(malli.core._ref(schema))){
return malli.json_schema._ref(schema,options);
} else {
var G__521085 = malli.core.deref.cljs$core$IFn$_invoke$arity$1(schema);
var G__521086 = options;
return (transform.cljs$core$IFn$_invoke$arity$2 ? transform.cljs$core$IFn$_invoke$arity$2(G__521085,G__521086) : transform.call(null,G__521085,G__521086));
}
});
malli.json_schema.select = (function malli$json_schema$select(m){
return cljs.core.select_keys(m,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"default","default",-1987822328)], null));
});
if((typeof malli !== 'undefined') && (typeof malli.json_schema !== 'undefined') && (typeof malli.json_schema.accept !== 'undefined')){
} else {
malli.json_schema.accept = (function (){var method_table__5747__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__5748__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var method_cache__5749__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__5750__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__5751__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$3(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),new cljs.core.Keyword("malli.json-schema","default","malli.json-schema/default",-421449099)], null),new cljs.core.Keyword(null,"hierarchy","hierarchy",-1053470341),(function (){var fexpr__521113 = cljs.core.get_global_hierarchy;
return (fexpr__521113.cljs$core$IFn$_invoke$arity$0 ? fexpr__521113.cljs$core$IFn$_invoke$arity$0() : fexpr__521113.call(null));
})());
return (new cljs.core.MultiFn(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2("malli.json-schema","accept"),(function (name,_schema,_children,_options){
return name;
}),new cljs.core.Keyword("malli.json-schema","default","malli.json-schema/default",-421449099),hierarchy__5751__auto__,method_table__5747__auto__,prefer_table__5748__auto__,method_cache__5749__auto__,cached_hierarchy__5750__auto__));
})();
}
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("malli.json-schema","default","malli.json-schema/default",-421449099),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"any?","any?",-318999933,null),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"some?","some?",234752293,null),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"number?","number?",-1747282210,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"integer?","integer?",1303791671,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"integer"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"int?","int?",1799729645,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"integer"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"pos-int?","pos-int?",-1205815015,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"integer",new cljs.core.Keyword(null,"minimum","minimum",-1621006059),(1)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"neg-int?","neg-int?",-1610409390,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"integer",new cljs.core.Keyword(null,"maximum","maximum",573880714),(-1)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"nat-int?","nat-int?",-1879663400,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"integer",new cljs.core.Keyword(null,"minimum","minimum",-1621006059),(0)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"float?","float?",673884616,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"double?","double?",-2146564276,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"float?","float?",673884616,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"pos?","pos?",-244377722,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"exclusiveMinimum","exclusiveMinimum",-869557322),(0)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"neg?","neg?",-1902175577,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"exclusiveMaximum","exclusiveMaximum",1883434466),(0)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"boolean?","boolean?",1790940868,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"boolean"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"string?","string?",-1129175764,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"ident?","ident?",-2061359468,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"simple-ident?","simple-ident?",194189851,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"qualified-ident?","qualified-ident?",-928894763,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"keyword?","keyword?",1917797069,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"simple-keyword?","simple-keyword?",-367134735,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"qualified-keyword?","qualified-keyword?",375456001,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"symbol?","symbol?",1820680511,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"simple-symbol?","simple-symbol?",1408454822,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"qualified-symbol?","qualified-symbol?",98763807,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"uuid?","uuid?",400077689,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"format","format",-1306924766),"uuid"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"uri?","uri?",2029475116,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"format","format",-1306924766),"uri"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"decimal?","decimal?",687666240,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"inst?","inst?",1614698981,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"format","format",-1306924766),"date-time"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"seqable?","seqable?",72462495,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"indexed?","indexed?",1234610384,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"map?","map?",-1780568534,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"object"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"vector?","vector?",-61367869,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"list?","list?",-1494629,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"seq?","seq?",-1951934719,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"char?","char?",-1072221244,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"set?","set?",1636014792,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"uniqueItems","uniqueItems",-826722268),true], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"null"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"false?","false?",-1522377573,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"boolean"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"true?","true?",-1600332395,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"boolean"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"zero?","zero?",325758897,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"integer"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"coll?","coll?",-1874821441,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"object"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"empty?","empty?",76408555,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"maxItems","maxItems",576652798),(0),new cljs.core.Keyword(null,"minItems","minItems",1950622069),(0)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"associative?","associative?",-141666771,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"object"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"sequential?","sequential?",1102351463,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"array"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"bytes?","bytes?",-1745721485,null),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"format","format",-1306924766),"byte"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"ifn?","ifn?",-2106461064,null),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Symbol(null,"fn?","fn?",1820990818,null),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,">",">",-555517146),(function (_,___$1,p__521161,___$2){
var vec__521162 = p__521161;
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521162,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"exclusiveMinimum","exclusiveMinimum",-869557322),value], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,">=",">=",-623615505),(function (_,___$1,p__521169,___$2){
var vec__521171 = p__521169;
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521171,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"minimum","minimum",-1621006059),value], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"<","<",-646864291),(function (_,___$1,p__521175,___$2){
var vec__521179 = p__521175;
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521179,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"exclusiveMaximum","exclusiveMaximum",1883434466),value], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"<=","<=",-395636158),(function (_,___$1,p__521182,___$2){
var vec__521183 = p__521182;
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521183,(0),null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"number",new cljs.core.Keyword(null,"maximum","maximum",573880714),value], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"=","=",1152933628),(function (_,___$1,p__521186,___$2){
var vec__521187 = p__521186;
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521187,(0),null);
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"const","const",1709929842),value], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"not=","not=",-173995323),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"not","not",-595976884),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"not","not",-595976884),cljs.core.last(children)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"and","and",-971899817),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"allOf","allOf",857821143),children], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"or","or",235744169),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"anyOf","anyOf",-1046092155),children], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"orn","orn",738436484),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"anyOf","anyOf",-1046092155),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.last,children)], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("malli.core","val","malli.core/val",39501268),(function (_,___$1,children,___$2){
return cljs.core.first(children);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"map","map",1371690461),(function (_,schema,children,___$1){
var ks = cljs.core.set(malli.core.explicit_keys.cljs$core$IFn$_invoke$arity$1(schema));
var default$ = (function (){var G__521195 = children;
var G__521195__$1 = (((G__521195 == null))?null:cljs.core.remove.cljs$core$IFn$_invoke$arity$2(malli.core._comp.cljs$core$IFn$_invoke$arity$2(ks,cljs.core.first),G__521195));
var G__521195__$2 = (((G__521195__$1 == null))?null:cljs.core.first(G__521195__$1));
if((G__521195__$2 == null)){
return null;
} else {
return cljs.core.last(G__521195__$2);
}
})();
var map__521194 = default$;
var map__521194__$1 = cljs.core.__destructure_map(map__521194);
var additionalProperties_SINGLEQUOTE_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521194__$1,new cljs.core.Keyword(null,"additionalProperties","additionalProperties",-1203767392));
var properties_SINGLEQUOTE_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521194__$1,new cljs.core.Keyword(null,"properties","properties",685819552));
var required_SINGLEQUOTE_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521194__$1,new cljs.core.Keyword(null,"required","required",1807647006));
var children__$1 = cljs.core.filter.cljs$core$IFn$_invoke$arity$2(malli.core._comp.cljs$core$IFn$_invoke$arity$2(ks,cljs.core.first),children);
var required = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.first,cljs.core.filter.cljs$core$IFn$_invoke$arity$2(malli.core._comp.cljs$core$IFn$_invoke$arity$3(cljs.core.not,new cljs.core.Keyword(null,"optional","optional",2053951509),cljs.core.second),children__$1));
var closed = new cljs.core.Keyword(null,"closed","closed",-919675359).cljs$core$IFn$_invoke$arity$1(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema));
var object = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"object",new cljs.core.Keyword(null,"properties","properties",685819552),cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.array_map,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p__521199){
var vec__521201 = p__521199;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521201,(0),null);
var ___$2 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521201,(1),null);
var s = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__521201,(2),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,s], null);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([children__$1], 0)))], null);
var G__521207 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([default$,object], 0));
var G__521207__$1 = ((cljs.core.seq(required))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__521207,new cljs.core.Keyword(null,"required","required",1807647006),required):G__521207);
var G__521207__$2 = (cljs.core.truth_(closed)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__521207__$1,new cljs.core.Keyword(null,"additionalProperties","additionalProperties",-1203767392),false):G__521207__$1);
if(cljs.core.truth_(default$)){
var G__521209 = G__521207__$2;
var G__521209__$1 = (cljs.core.truth_(additionalProperties_SINGLEQUOTE_)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__521209,new cljs.core.Keyword(null,"additionalProperties","additionalProperties",-1203767392),additionalProperties_SINGLEQUOTE_):G__521209);
var G__521209__$2 = (cljs.core.truth_(properties_SINGLEQUOTE_)?cljs.core.update.cljs$core$IFn$_invoke$arity$4(G__521209__$1,new cljs.core.Keyword(null,"properties","properties",685819552),cljs.core.merge,properties_SINGLEQUOTE_):G__521209__$1);
if(cljs.core.truth_(required_SINGLEQUOTE_)){
return cljs.core.update.cljs$core$IFn$_invoke$arity$4(G__521209__$2,new cljs.core.Keyword(null,"required","required",1807647006),cljs.core.comp.cljs$core$IFn$_invoke$arity$3(cljs.core.vec,cljs.core.distinct,cljs.core.into),required_SINGLEQUOTE_);
} else {
return G__521209__$2;
}
} else {
return G__521207__$2;
}
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"multi","multi",-190293005),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"oneOf","oneOf",1209080187),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.last,children)], null);
}));
malli.json_schema.minmax_properties = (function malli$json_schema$minmax_properties(m,schema,kmin,kmax){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([m,clojure.set.rename_keys(cljs.core.select_keys(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"max","max",61366548)], null)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),kmin,new cljs.core.Keyword(null,"max","max",61366548),kmax], null))], 0));
});
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"map-of","map-of",1189682355),(function (_,schema,children,___$1){
return malli.json_schema.minmax_properties(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"object",new cljs.core.Keyword(null,"additionalProperties","additionalProperties",-1203767392),cljs.core.second(children)], null),schema,new cljs.core.Keyword(null,"minProperties","minProperties",100355152),new cljs.core.Keyword(null,"maxProperties","maxProperties",1289793027));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"vector","vector",1902966158),(function (_,schema,children,___$1){
return malli.json_schema.minmax_properties(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.first(children)], null),schema,new cljs.core.Keyword(null,"minItems","minItems",1950622069),new cljs.core.Keyword(null,"maxItems","maxItems",576652798));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"sequential","sequential",-1082983960),(function (_,schema,children,___$1){
return malli.json_schema.minmax_properties(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.first(children)], null),schema,new cljs.core.Keyword(null,"minItems","minItems",1950622069),new cljs.core.Keyword(null,"maxItems","maxItems",576652798));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"set","set",304602554),(function (_,schema,children,___$1){
return malli.json_schema.minmax_properties(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.first(children),new cljs.core.Keyword(null,"uniqueItems","uniqueItems",-826722268),true], null),schema,new cljs.core.Keyword(null,"minItems","minItems",1950622069),new cljs.core.Keyword(null,"maxItems","maxItems",576652798));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"enum","enum",1679018432),(function (_,___$1,children,options){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var G__521211 = malli.core._infer(children);
if((G__521211 == null)){
return null;
} else {
return (malli.json_schema._transform.cljs$core$IFn$_invoke$arity$2 ? malli.json_schema._transform.cljs$core$IFn$_invoke$arity$2(G__521211,options) : malli.json_schema._transform.call(null,G__521211,options));
}
})(),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"enum","enum",1679018432),children], null)], 0));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"maybe","maybe",-314397560),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"oneOf","oneOf",1209080187),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(children,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"null"], null))], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"tuple","tuple",-472667284),(function (_,___$1,children,___$2){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),"array",new cljs.core.Keyword(null,"items","items",1031954938),children,new cljs.core.Keyword(null,"additionalItems","additionalItems",630706986),false], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"re","re",228676202),(function (_,schema,___$1,options){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"pattern","pattern",242135423),cljs.core.first(malli.core.children.cljs$core$IFn$_invoke$arity$2(schema,options))], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"fn","fn",-1175266204),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"any","any",1705907423),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"some","some",-1951079573),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"nil","nil",99600501),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"null"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"string","string",-1989541586),(function (_,schema,___$1,___$2){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null),clojure.set.rename_keys(cljs.core.select_keys(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"max","max",61366548)], null)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"minLength","minLength",-1538722770),new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"maxLength","maxLength",-1633020073)], null))], 0));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"int","int",-1741416922),(function (_,schema,___$1,___$2){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"integer"], null),clojure.set.rename_keys(cljs.core.select_keys(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"max","max",61366548)], null)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"minimum","minimum",-1621006059),new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"maximum","maximum",573880714)], null))], 0));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"float","float",-1732389368),(function (_,schema,___$1,___$2){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null),clojure.set.rename_keys(cljs.core.select_keys(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"max","max",61366548)], null)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"minimum","minimum",-1621006059),new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"maximum","maximum",573880714)], null))], 0));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"double","double",884886883),(function (_,schema,___$1,___$2){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"number"], null),clojure.set.rename_keys(cljs.core.select_keys(malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"max","max",61366548)], null)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),new cljs.core.Keyword(null,"minimum","minimum",-1621006059),new cljs.core.Keyword(null,"max","max",61366548),new cljs.core.Keyword(null,"maximum","maximum",573880714)], null))], 0));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"boolean","boolean",-1919418404),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"boolean"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"keyword","keyword",811389747),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"qualified-keyword","qualified-keyword",736041675),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"symbol","symbol",-1038572696),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"qualified-symbol","qualified-symbol",-665513695),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),"string"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"uuid","uuid",-2145095719),(function (_,___$1,___$2,___$3){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"string",new cljs.core.Keyword(null,"format","format",-1306924766),"uuid"], null);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"=>","=>",1841166128),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"function","function",-2127255473),(function (_,___$1,___$2,___$3){
return cljs.core.PersistentArrayMap.EMPTY;
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"ref","ref",1289896967),(function (_,schema,___$1,options){
return malli.json_schema._ref(schema,options);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"schema","schema",-1582001791),(function (_,schema,___$1,options){
return malli.json_schema._schema(schema,options);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("malli.core","schema","malli.core/schema",-1780373863),(function (_,schema,___$1,options){
return malli.json_schema._schema(schema,options);
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"merge","merge",-1804319409),(function (_,schema,___$1,p__521235){
var map__521236 = p__521235;
var map__521236__$1 = cljs.core.__destructure_map(map__521236);
var options = map__521236__$1;
var transform = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521236__$1,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521));
var G__521237 = malli.core.deref.cljs$core$IFn$_invoke$arity$1(schema);
var G__521238 = options;
return (transform.cljs$core$IFn$_invoke$arity$2 ? transform.cljs$core$IFn$_invoke$arity$2(G__521237,G__521238) : transform.call(null,G__521237,G__521238));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"union","union",2142937499),(function (_,schema,___$1,p__521239){
var map__521240 = p__521239;
var map__521240__$1 = cljs.core.__destructure_map(map__521240);
var options = map__521240__$1;
var transform = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521240__$1,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521));
var G__521242 = malli.core.deref.cljs$core$IFn$_invoke$arity$1(schema);
var G__521243 = options;
return (transform.cljs$core$IFn$_invoke$arity$2 ? transform.cljs$core$IFn$_invoke$arity$2(G__521242,G__521243) : transform.call(null,G__521242,G__521243));
}));
malli.json_schema.accept.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"select-keys","select-keys",1945879180),(function (_,schema,___$1,p__521244){
var map__521245 = p__521244;
var map__521245__$1 = cljs.core.__destructure_map(map__521245);
var options = map__521245__$1;
var transform = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__521245__$1,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521));
var G__521246 = malli.core.deref.cljs$core$IFn$_invoke$arity$1(schema);
var G__521247 = options;
return (transform.cljs$core$IFn$_invoke$arity$2 ? transform.cljs$core$IFn$_invoke$arity$2(G__521246,G__521247) : transform.call(null,G__521246,G__521247));
}));
malli.json_schema._json_schema_walker = (function malli$json_schema$_json_schema_walker(schema,_,children,options){
var p = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([malli.core.type_properties.cljs$core$IFn$_invoke$arity$1(schema),malli.core.properties.cljs$core$IFn$_invoke$arity$1(schema)], 0));
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(p,new cljs.core.Keyword(null,"json-schema","json-schema",389191695));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([malli.json_schema.select(p),(((((!((schema == null))))?((((false) || ((cljs.core.PROTOCOL_SENTINEL === schema.malli$json_schema$JsonSchema$))))?true:(((!schema.cljs$lang$protocol_mask$partition$))?cljs.core.native_satisfies_QMARK_(malli.json_schema.JsonSchema,schema):false)):cljs.core.native_satisfies_QMARK_(malli.json_schema.JsonSchema,schema)))?malli.json_schema._accept(schema,children,options):malli.json_schema.accept.cljs$core$IFn$_invoke$arity$4(malli.core.type.cljs$core$IFn$_invoke$arity$1(schema),schema,children,options)),malli.core._unlift_keys(p,new cljs.core.Keyword(null,"json-schema","json-schema",389191695))], 0));
}
});
malli.json_schema._transform = (function malli$json_schema$_transform(_QMARK_schema,options){
return malli.core.walk.cljs$core$IFn$_invoke$arity$3(_QMARK_schema,malli.json_schema._json_schema_walker,options);
});
malli.json_schema.transform = (function malli$json_schema$transform(var_args){
var G__521252 = arguments.length;
switch (G__521252) {
case 1:
return malli.json_schema.transform.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return malli.json_schema.transform.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(malli.json_schema.transform.cljs$core$IFn$_invoke$arity$1 = (function (_QMARK_schema){
return malli.json_schema.transform.cljs$core$IFn$_invoke$arity$2(_QMARK_schema,null);
}));

(malli.json_schema.transform.cljs$core$IFn$_invoke$arity$2 = (function (_QMARK_schema,options){
var definitions = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var options__$1 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([options,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("malli.core","walk-entry-vals","malli.core/walk-entry-vals",-64238340),true,new cljs.core.Keyword("malli.json-schema","definitions","malli.json-schema/definitions",1666750607),definitions,new cljs.core.Keyword("malli.json-schema","transform","malli.json-schema/transform",-1255228521),malli.json_schema._transform], null)], 0));
var G__521257 = malli.json_schema._transform(_QMARK_schema,options__$1);
if(cljs.core.seq(cljs.core.deref(definitions))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__521257,new cljs.core.Keyword(null,"definitions","definitions",167529986),cljs.core.deref(definitions));
} else {
return G__521257;
}
}));

(malli.json_schema.transform.cljs$lang$maxFixedArity = 2);


//# sourceMappingURL=malli.json_schema.js.map
