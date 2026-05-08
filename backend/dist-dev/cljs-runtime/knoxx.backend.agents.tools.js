import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agents.content.js";
import "./knoxx.backend.text.js";
goog.provide('knoxx.backend.agents.tools');
/**
 * Tool-specific input previews that are always human readable (no raw JSON).
 * 
 * This is intentionally conservative: we only special-case tools where we know
 * the user expectation is strict (bash/read). Everything else falls back to
 * value->preview-text rendering (which the frontend formats into bullets).
 */
knoxx.backend.agents.tools.tool_args__GT_markdown_preview = (function knoxx$backend$agents$tools$tool_args__GT_markdown_preview(tool_name,raw_args){
var tool_name__$1 = clojure.string.lower_case(cljs.core.last(clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = tool_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/[.\/:]/)));
var args = ((cljs.core.map_QMARK_(raw_args))?raw_args:(cljs.core.truth_((function (){var and__5140__auto__ = raw_args;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(raw_args,undefined);
} else {
return and__5140__auto__;
}
})())?(function (){try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(raw_args,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e33797){var _ = e33797;
return null;
}})():null
));
var arg_value = (function() { 
var G__34085__delegate = function (keys){
var or__5142__auto__ = cljs.core.some((function (k){
if(((cljs.core.map_QMARK_(args)) && ((k instanceof cljs.core.Keyword)))){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(args,k);
} else {
if(((cljs.core.map_QMARK_(args)) && (typeof k === 'string'))){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(args,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(args,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k));
}
} else {
return null;

}
}
}),keys);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.some((function (k){
if(((cljs.core.map_QMARK_(raw_args)) && ((k instanceof cljs.core.Keyword)))){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(raw_args,k);
} else {
if(((cljs.core.map_QMARK_(raw_args)) && (typeof k === 'string'))){
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(raw_args,k);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(raw_args,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k));
}
} else {
return null;

}
}
}),keys);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.some((function (k){
if(cljs.core.truth_((function (){var and__5140__auto__ = raw_args;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(raw_args,undefined)) && (((cljs.core.object_QMARK_(raw_args)) || (cljs.core.fn_QMARK_(raw_args)))));
} else {
return and__5140__auto__;
}
})())){
return (raw_args[(((k instanceof cljs.core.Keyword))?cljs.core.name(k):(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k)))]);
} else {
return null;
}
}),keys);
}
}
};
var G__34085 = function (var_args){
var keys = null;
if (arguments.length > 0) {
var G__34091__i = 0, G__34091__a = new Array(arguments.length -  0);
while (G__34091__i < G__34091__a.length) {G__34091__a[G__34091__i] = arguments[G__34091__i + 0]; ++G__34091__i;}
  keys = new cljs.core.IndexedSeq(G__34091__a,0,null);
} 
return G__34085__delegate.call(this,keys);};
G__34085.cljs$lang$maxFixedArity = 0;
G__34085.cljs$lang$applyTo = (function (arglist__34092){
var keys = cljs.core.seq(arglist__34092);
return G__34085__delegate(keys);
});
G__34085.cljs$core$IFn$_invoke$arity$variadic = G__34085__delegate;
return G__34085;
})()
;
var G__33890 = tool_name__$1;
switch (G__33890) {
case "bash":
if(cljs.core.map_QMARK_(args)){
var cmd = arg_value(new cljs.core.Keyword(null,"command","command",-894540724),new cljs.core.Keyword(null,"cmd","cmd",-302931143));
var timeout = arg_value(new cljs.core.Keyword(null,"timeout","timeout",-318625318),new cljs.core.Keyword(null,"timeoutSeconds","timeoutSeconds",-815612016),new cljs.core.Keyword(null,"timeoutMs","timeoutMs",-716622575));
if(((typeof cmd === 'string') && ((!(clojure.string.blank_QMARK_(cmd)))))){
var vec__33919 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(cmd,(20000));
var clipped_cmd = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__33919,(0),null);
var clipped_QMARK_ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__33919,(1),null);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agents.content.fenced("bash",(cljs.core.truth_(clipped_QMARK_)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clipped_cmd)+"\u2026"):clipped_cmd)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(clipped_QMARK_)?"\n\n_(truncated)_":null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((!((timeout == null))))?(""+"\n\n- timeout: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(timeout)):null)));
} else {
return null;
}
} else {
return null;
}

break;
case "read":
var path = arg_value(new cljs.core.Keyword(null,"path","path",-188191168),"path");
var offset = arg_value(new cljs.core.Keyword(null,"offset","offset",296498311),"offset");
var limit = arg_value(new cljs.core.Keyword(null,"limit","limit",-1355822363),"limit");
if(((typeof path === 'string') && ((!(clojure.string.blank_QMARK_(path)))))){
return knoxx.backend.agents.content.fenced("yaml",(""+"path: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+"\noffset: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((!((offset == null))))?offset:"(default)"))+"\nlimit: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((!((limit == null))))?limit:"(default)"))));
} else {
return null;
}

break;
default:
return null;

}
});
knoxx.backend.agents.tools.copy_js_object = (function knoxx$backend$agents$tools$copy_js_object(value){
if(cljs.core.truth_((function (){var and__5140__auto__ = value;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(value,undefined)) && (((cljs.core.not(cljs.core.array_QMARK_(value))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("object",goog.typeOf(value))))));
} else {
return and__5140__auto__;
}
})())){
var copy = ({});
var own_keys = cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(Object.keys(value)),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(Object.getOwnPropertyNames(value))));
var seq__33953_34116 = cljs.core.seq(own_keys);
var chunk__33954_34117 = null;
var count__33955_34118 = (0);
var i__33956_34119 = (0);
while(true){
if((i__33956_34119 < count__33955_34118)){
var k_34122 = chunk__33954_34117.cljs$core$IIndexed$_nth$arity$2(null,i__33956_34119);
(copy[k_34122] = (value[k_34122]));


var G__34124 = seq__33953_34116;
var G__34125 = chunk__33954_34117;
var G__34126 = count__33955_34118;
var G__34127 = (i__33956_34119 + (1));
seq__33953_34116 = G__34124;
chunk__33954_34117 = G__34125;
count__33955_34118 = G__34126;
i__33956_34119 = G__34127;
continue;
} else {
var temp__5825__auto___34128 = cljs.core.seq(seq__33953_34116);
if(temp__5825__auto___34128){
var seq__33953_34129__$1 = temp__5825__auto___34128;
if(cljs.core.chunked_seq_QMARK_(seq__33953_34129__$1)){
var c__5673__auto___34130 = cljs.core.chunk_first(seq__33953_34129__$1);
var G__34131 = cljs.core.chunk_rest(seq__33953_34129__$1);
var G__34132 = c__5673__auto___34130;
var G__34133 = cljs.core.count(c__5673__auto___34130);
var G__34134 = (0);
seq__33953_34116 = G__34131;
chunk__33954_34117 = G__34132;
count__33955_34118 = G__34133;
i__33956_34119 = G__34134;
continue;
} else {
var k_34135 = cljs.core.first(seq__33953_34129__$1);
(copy[k_34135] = (value[k_34135]));


var G__34140 = cljs.core.next(seq__33953_34129__$1);
var G__34141 = null;
var G__34142 = (0);
var G__34143 = (0);
seq__33953_34116 = G__34140;
chunk__33954_34117 = G__34141;
count__33955_34118 = G__34142;
i__33956_34119 = G__34143;
continue;
}
} else {
}
}
break;
}

return copy;
} else {
return null;
}
});
knoxx.backend.agents.tools.tool_call_input_preview = (function knoxx$backend$agents$tools$tool_call_input_preview(tool_name,raw_args){
var or__5142__auto__ = knoxx.backend.agents.tools.tool_args__GT_markdown_preview(tool_name,raw_args);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.preview_text_nonblank(raw_args,(20000));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.json_preview_nonblank(raw_args,(20000));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var copied = knoxx.backend.agents.tools.copy_js_object(raw_args);
var or__5142__auto____$3 = knoxx.backend.agents.tools.tool_args__GT_markdown_preview(tool_name,copied);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.agents.content.preview_text_nonblank(copied,(20000));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return knoxx.backend.agents.content.json_preview_nonblank(copied,(20000));
}
}
}
}
}
});
knoxx.backend.agents.tools.tool_call_preview_from_part = (function knoxx$backend$agents$tools$tool_call_preview_from_part(part){
var part_type = (function (){var G__34043 = (part["type"]);
var G__34043__$1 = (((G__34043 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34043)));
if((G__34043__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__34043__$1);
}
})();
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["tool_call",null,"toolcall",null], null), null),part_type)){
var tool_call_id = (function (){var G__34048 = (part["id"]);
var G__34048__$1 = (((G__34048 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34048)));
if((G__34048__$1 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__34048__$1);
}
})();
var tool_name = (function (){var G__34049 = (part["name"]);
var G__34049__$1 = (((G__34049 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34049)));
if((G__34049__$1 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__34049__$1);
}
})();
var arguments$ = (part["arguments"]);
var input_preview = knoxx.backend.agents.tools.tool_call_input_preview(tool_name,arguments$);
if(cljs.core.truth_((function (){var and__5140__auto__ = tool_call_id;
if(cljs.core.truth_(and__5140__auto__)){
return input_preview;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"tool_call_id","tool_call_id",-1531015517),tool_call_id,new cljs.core.Keyword(null,"tool_name","tool_name",-42168484),tool_name,new cljs.core.Keyword(null,"input_preview","input_preview",2048529734),input_preview], null);
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.agents.tools.assistant_tool_call_previews = (function knoxx$backend$agents$tools$assistant_tool_call_previews(assistant_message){
var content = (cljs.core.truth_(assistant_message)?(assistant_message["content"]):null);
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.tools.tool_call_preview_from_part,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content)));
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
goog.exportSymbol('knoxx.backend.agents.tools.assistant_tool_call_previews', knoxx.backend.agents.tools.assistant_tool_call_previews);

//# sourceMappingURL=knoxx.backend.agents.tools.js.map
