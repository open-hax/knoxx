import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.http.js";
goog.provide('knoxx.backend.text');
/**
 * Repair SVG content corrupted by the <<tagtag duplication pattern.
 * Returns repaired content string, or original if no repair needed.
 */
knoxx.backend.text.sanitize_svg_content = (function knoxx$backend$text$sanitize_svg_content(content){
if(cljs.core.truth_((function (){var and__5140__auto__ = typeof content === 'string';
if(and__5140__auto__){
return cljs.core.re_find(/\<\<[a-z]/,content);
} else {
return and__5140__auto__;
}
})())){
return clojure.string.replace(content,/\<\<([a-z]+)((?:[A-Z][a-zA-Z]*)?)/,(function (p__517265){
var vec__517266 = p__517265;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517266,(0),null);
var prefix = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517266,(1),null);
var suffix = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517266,(2),null);
if(clojure.string.blank_QMARK_(suffix)){
var full = prefix;
var half_len = cljs.core.quot(cljs.core.count(full),(2));
var first_half = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(full,(0),half_len);
var second_half = cljs.core.subs.cljs$core$IFn$_invoke$arity$3(full,half_len,(half_len + half_len));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(first_half,second_half)){
return (""+"<"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(first_half));
} else {
return (""+"<"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(full));
}
} else {
var full = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));
var half_len = cljs.core.quot(cljs.core.count(prefix),(2));
if((((half_len > (0))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(prefix,(0),half_len),cljs.core.subs.cljs$core$IFn$_invoke$arity$3(prefix,half_len,(half_len + half_len)))))){
return (""+"<"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(prefix,(0),half_len))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));
} else {
return (""+"<"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(full));
}
}
}));
} else {
return content;
}
});
knoxx.backend.text.compact_whitespace = (function knoxx$backend$text$compact_whitespace(text){
return clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)),/\s+/," "));
});
knoxx.backend.text.search_tokens = (function knoxx$backend$text$search_tokens(text){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__517278_SHARP_){
return (cljs.core.count(p1__517278_SHARP_) <= (1));
}),cljs.core.re_seq(/[A-Za-z0-9][A-Za-z0-9_.\/:-]*/,clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)))))));
});
knoxx.backend.text.text_like_exts = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 20, [".json",null,".yaml",null,".tsx",null,".md",null,".log",null,".jsx",null,".js",null,".txt",null,".csv",null,".mdx",null,".ts",null,".htm",null,".html",null,".cljs",null,".clj",null,".org",null,".sql",null,".yml",null,".edn",null,".xml",null], null), null);
knoxx.backend.text.text_like_path_QMARK_ = (function knoxx$backend$text$text_like_path_QMARK_(path_str){
var lower = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_str)));
var idx = lower.lastIndexOf(".");
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(idx,(-1))) || (cljs.core.contains_QMARK_(knoxx.backend.text.text_like_exts,lower.slice(idx))));
});
knoxx.backend.text.count_occurrences = (function knoxx$backend$text$count_occurrences(text,needle){
var idx = (0);
var total = (0);
while(true){
var hit = text.indexOf(needle,idx);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(hit,(-1))){
return total;
} else {
var G__517828 = (hit + cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.count(needle)));
var G__517829 = (total + (1));
idx = G__517828;
total = G__517829;
continue;
}
break;
}
});
knoxx.backend.text.best_match_index = (function knoxx$backend$text$best_match_index(haystack,query,tokens){
var phrase_idx = ((clojure.string.blank_QMARK_(query))?(-1):haystack.indexOf(query));
if((phrase_idx >= (0))){
return phrase_idx;
} else {
var or__5142__auto__ = cljs.core.some((function (token){
var idx = haystack.indexOf(token);
if((idx >= (0))){
return idx;
} else {
return null;
}
}),tokens);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (-1);
}
}
});
knoxx.backend.text.snippet_around = (function knoxx$backend$text$snippet_around(text,query,tokens,max_chars){
var raw = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text));
var lowered = clojure.string.lower_case(raw);
var idx = knoxx.backend.text.best_match_index(lowered,query,tokens);
var radius = cljs.core.max.cljs$core$IFn$_invoke$arity$2((80),Math.floor((max_chars / (2))));
var start = cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),(idx - radius));
var end = cljs.core.min.cljs$core$IFn$_invoke$arity$2(((raw).length),(idx + radius));
var prefix = (((start > (0)))?"\u2026":"");
var suffix = (((end < ((raw).length)))?"\u2026":"");
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.text.compact_whitespace(raw.slice(start,end)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));
});
knoxx.backend.text.semantic_score = (function knoxx$backend$text$semantic_score(p__517296){
var map__517297 = p__517296;
var map__517297__$1 = cljs.core.__destructure_map(map__517297);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var tokens = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"tokens","tokens",-818939304));
var rel_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"rel-path","rel-path",593215642));
var name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"name","name",1843675177));
var text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var indexed = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517297__$1,new cljs.core.Keyword(null,"indexed","indexed",390758624));
var query__$1 = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)));
var rel_lower = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(rel_path)));
var name_lower = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)));
var text_lower = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)));
var phrase_score = (((((((!(clojure.string.blank_QMARK_(query__$1)))) && (clojure.string.includes_QMARK_(name_lower,query__$1))))?(10):(0)) + (((((!(clojure.string.blank_QMARK_(query__$1)))) && (clojure.string.includes_QMARK_(rel_lower,query__$1))))?(8):(0))) + (((((!(clojure.string.blank_QMARK_(query__$1)))) && (clojure.string.includes_QMARK_(text_lower,query__$1))))?(6):(0)));
var token_score = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (total,token){
return (((total + ((clojure.string.includes_QMARK_(name_lower,token))?(3):(0))) + ((clojure.string.includes_QMARK_(rel_lower,token))?(2):(0))) + cljs.core.min.cljs$core$IFn$_invoke$arity$2((3),(0.6 * knoxx.backend.text.count_occurrences(text_lower,token))));
}),(0),tokens);
var indexed_bonus = (cljs.core.truth_(indexed)?0.75:(0));
return ((phrase_score + token_score) + indexed_bonus);
});
knoxx.backend.text.tool_text_result = (function knoxx$backend$text$tool_text_result(text,details){
return ({"content": [({"type": "text", "text": text})], "details": cljs.core.clj__GT_js(details)});
});
knoxx.backend.text.preview_preferred_keys = new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"answer","answer",-742633163),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"result","result",1415092211),new cljs.core.Keyword(null,"output","output",-1105869043),new cljs.core.Keyword(null,"preview","preview",451279890),new cljs.core.Keyword(null,"summary","summary",380847952),new cljs.core.Keyword(null,"translated_text","translated_text",-108470289),new cljs.core.Keyword(null,"corrected_text","corrected_text",-1411081099),new cljs.core.Keyword(null,"snippet","snippet",953581994)], null);
knoxx.backend.text.preview_collection_keys = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"rows","rows",850049680),new cljs.core.Keyword(null,"hits","hits",-2120002930),new cljs.core.Keyword(null,"results","results",-1134170113),new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.Keyword(null,"items","items",1031954938),new cljs.core.Keyword(null,"documents","documents",-1582333455)], null);
knoxx.backend.text.safe_get = (function knoxx$backend$text$safe_get(m,k){
if(cljs.core.map_QMARK_(m)){
var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (((k instanceof cljs.core.Keyword))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,cljs.core.name(k)):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(typeof k === 'string'){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(m,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k));
} else {
return null;
}
}
}
} else {
return null;
}
});
knoxx.backend.text.format_preview_scalar = (function knoxx$backend$text$format_preview_scalar(value){
if((value == null)){
return "null";
} else {
if(typeof value === 'string'){
var trimmed = clojure.string.trim(value);
if(clojure.string.blank_QMARK_(trimmed)){
return "";
} else {
return trimmed;
}
} else {
if(typeof value === 'number'){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
} else {
if(cljs.core.boolean_QMARK_(value)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));

}
}
}
}
});
knoxx.backend.text.summarize_structured = (function knoxx$backend$text$summarize_structured(var_args){
var G__517333 = arguments.length;
switch (G__517333) {
case 1:
return knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2(value,(0));
}));

(knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2 = (function (value,depth){
if((depth < (4))){
if(((typeof value === 'string') || (((typeof value === 'number') || (((cljs.core.boolean_QMARK_(value)) || ((value == null)))))))){
var s = knoxx.backend.text.format_preview_scalar(value);
if(clojure.string.blank_QMARK_(s)){
return null;
} else {
return s;
}
} else {
if(cljs.core.map_QMARK_(value)){
var or__5142__auto__ = cljs.core.some((function (k){
var temp__5825__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2(knoxx.backend.text.safe_get(value,k),(depth + (1)));
if(cljs.core.truth_(temp__5825__auto__)){
var hit = temp__5825__auto__;
return hit;
} else {
return null;
}
}),knoxx.backend.text.preview_preferred_keys);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.some((function (k){
var temp__5825__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2(knoxx.backend.text.safe_get(value,k),(depth + (1)));
if(cljs.core.truth_(temp__5825__auto__)){
var hit = temp__5825__auto__;
return hit;
} else {
return null;
}
}),knoxx.backend.text.preview_collection_keys);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return null;
}
}
} else {
if(cljs.core.sequential_QMARK_(value)){
var items = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((8),value));
var looks_like_content_parts_QMARK_ = ((cljs.core.seq(items)) && (((cljs.core.every_QMARK_(cljs.core.map_QMARK_,items)) && (cljs.core.every_QMARK_((function (m){
var t = (function (){var or__5142__auto__ = knoxx.backend.text.safe_get(m,new cljs.core.Keyword(null,"text","text",-1790561697));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.text.safe_get(m,"text");
}
})();
var ty = (function (){var or__5142__auto__ = knoxx.backend.text.safe_get(m,new cljs.core.Keyword(null,"type","type",1174270348));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.text.safe_get(m,"type");
}
})();
return ((typeof t === 'string') && ((((ty == null)) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ty,"text")) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ty,"output_text")))))));
}),items)))));
if(looks_like_content_parts_QMARK_){
var joined = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__517327_SHARP_){
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__517327_SHARP_)));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (m){
var or__5142__auto__ = knoxx.backend.text.safe_get(m,new cljs.core.Keyword(null,"text","text",-1790561697));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.text.safe_get(m,"text");
}
}),items))));
if(clojure.string.blank_QMARK_(joined)){
return null;
} else {
return joined;
}
} else {
var summaries = cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__517329_SHARP_){
return (((p1__517329_SHARP_ == null)) || (clojure.string.blank_QMARK_(p1__517329_SHARP_)));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__517328_SHARP_){
return knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2(p1__517328_SHARP_,(depth + (1)));
}),items));
if(cljs.core.seq(summaries)){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (item){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item));
}),summaries));
} else {
return null;
}
}
} else {
return null;

}
}
}
} else {
return null;
}
}));

(knoxx.backend.text.summarize_structured.cljs$lang$maxFixedArity = 2);

knoxx.backend.text.markdown_bullets = (function knoxx$backend$text$markdown_bullets(var_args){
var G__517491 = arguments.length;
switch (G__517491) {
case 1:
return knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$2(value,(0));
}));

(knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$2 = (function (value,depth){
if((depth < (4))){
if(((typeof value === 'string') || (((typeof value === 'number') || (((cljs.core.boolean_QMARK_(value)) || ((value == null)))))))){
var s = knoxx.backend.text.format_preview_scalar(value);
if(clojure.string.blank_QMARK_(s)){
return null;
} else {
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s));
}
} else {
if(cljs.core.map_QMARK_(value)){
var keys = cljs.core.take.cljs$core$IFn$_invoke$arity$2((20),cljs.core.keys(value));
if(cljs.core.seq(keys)){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (k){
var child = cljs.core.get.cljs$core$IFn$_invoke$arity$2(value,k);
var label = (((k instanceof cljs.core.Keyword))?cljs.core.name(k):((typeof k === 'string')?k:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k))
));
var nested = knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$2(child,(depth + (1)));
if(cljs.core.truth_((function (){var and__5140__auto__ = nested;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_(child)) || (cljs.core.sequential_QMARK_(child)));
} else {
return and__5140__auto__;
}
})())){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+":\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.replace(nested,/^-/m,"  -")));
} else {
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$2(child,(depth + (1)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.text.format_preview_scalar(child);
}
})()));
}
}),keys));
} else {
return null;
}
} else {
if(cljs.core.sequential_QMARK_(value)){
var items = cljs.core.take.cljs$core$IFn$_invoke$arity$2((20),value);
var rendered = cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__517487_SHARP_){
return (((p1__517487_SHARP_ == null)) || (clojure.string.blank_QMARK_(p1__517487_SHARP_)));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__517485_SHARP_){
return knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$2(p1__517485_SHARP_,(depth + (1)));
}),items));
if(cljs.core.seq(rendered)){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",rendered);
} else {
return null;
}
} else {
return null;

}
}
}
} else {
return null;
}
}));

(knoxx.backend.text.markdown_bullets.cljs$lang$maxFixedArity = 2);

knoxx.backend.text.extract_json_keys = (function knoxx$backend$text$extract_json_keys(text){
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((16),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.second,cljs.core.re_seq(/\"([A-Za-z0-9_\-]+)\"\s*:/,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)))))));
});
knoxx.backend.text.unescape_json_fragment = (function knoxx$backend$text$unescape_json_fragment(text){
return clojure.string.replace(clojure.string.replace(clojure.string.replace(clojure.string.replace(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)),/\\\\n/,"\n"),/\\\\t/,"\t"),/\\\\r/,"\r"),/\\\\\"/,"\""),/\\\\\\\\/,"\\");
});
/**
 * Best-effort extraction for truncated/invalid JSON: pulls the string value of a field like "text".
 */
knoxx.backend.text.extract_json_string_field = (function knoxx$backend$text$extract_json_string_field(text,field){
var s = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text));
var needle = (""+"\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(field)+"\"");
var idx = s.indexOf(needle);
if((idx >= (0))){
var colon = s.indexOf(":",(idx + ((needle).length)));
var quote = s.indexOf("\"",(((colon >= (0)))?(colon + (1)):(idx + ((needle).length))));
if((quote >= (0))){
var i = (quote + (1));
var escaped_QMARK_ = false;
var acc = cljs.core.transient$(cljs.core.PersistentVector.EMPTY);
while(true){
if((i >= ((s).length))){
return knoxx.backend.text.unescape_json_fragment(cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.persistent_BANG_(acc)));
} else {
var ch = s.charAt(i);
if(escaped_QMARK_){
var G__517881 = (i + (1));
var G__517882 = false;
var G__517883 = cljs.core.conj_BANG_.cljs$core$IFn$_invoke$arity$2(acc,(""+"\\"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)));
i = G__517881;
escaped_QMARK_ = G__517882;
acc = G__517883;
continue;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ch,"\\")){
var G__517884 = (i + (1));
var G__517885 = true;
var G__517886 = acc;
i = G__517884;
escaped_QMARK_ = G__517885;
acc = G__517886;
continue;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ch,"\"")){
return knoxx.backend.text.unescape_json_fragment(cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.persistent_BANG_(acc)));
} else {
var G__517888 = (i + (1));
var G__517889 = false;
var G__517890 = cljs.core.conj_BANG_.cljs$core$IFn$_invoke$arity$2(acc,ch);
i = G__517888;
escaped_QMARK_ = G__517889;
acc = G__517890;
continue;

}
}
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
});
knoxx.backend.text.summarize_json_string = (function knoxx$backend$text$summarize_json_string(text){
var trimmed = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)));
if((((!(clojure.string.blank_QMARK_(trimmed)))) && (((clojure.string.starts_with_QMARK_(trimmed,"{")) || (clojure.string.starts_with_QMARK_(trimmed,"[")))))){
try{var parsed = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(trimmed),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var summarized = (function (){var or__5142__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$1(parsed);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$1(parsed);
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summarized)))){
return null;
} else {
return summarized;
}
}catch (e517547){var _ = e517547;
var or__5142__auto__ = (function (){var G__517548 = knoxx.backend.text.extract_json_string_field(trimmed,"text");
var G__517548__$1 = (((G__517548 == null))?null:clojure.string.trim(G__517548));
if((G__517548__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__517548__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = cljs.core.seq(knoxx.backend.text.extract_json_keys(trimmed));
if(temp__5825__auto__){
var keys = temp__5825__auto__;
return (""+"- JSON (truncated/invalid preview) keys: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",keys)));
} else {
return null;
}
}
}} else {
return null;
}
});
knoxx.backend.text.clip_text = (function knoxx$backend$text$clip_text(var_args){
var G__517564 = arguments.length;
switch (G__517564) {
case 1:
return knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$1 = (function (text){
return knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(text,(20000));
}));

(knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2 = (function (text,limit){
if((cljs.core.count(text) <= limit)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [text,false], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text,(0),limit),true], null);
}
}));

(knoxx.backend.text.clip_text.cljs$lang$maxFixedArity = 2);

knoxx.backend.text.replace_first = (function knoxx$backend$text$replace_first(text,old,new$){
var idx = text.indexOf(old);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(idx,(-1))){
return text;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text.slice((0),idx))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new$)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text.slice((idx + cljs.core.count(old)))));
}
});
knoxx.backend.text.content_part_text = (function knoxx$backend$text$content_part_text(part){
if((part == null)){
return "";
} else {
if(typeof part === 'string'){
return part;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"text")){
var or__5142__auto__ = (part["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"output_text")){
var or__5142__auto__ = (part["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
} else {
return "";

}
}
}
}
});
knoxx.backend.text.reasoning_part_text = (function knoxx$backend$text$reasoning_part_text(part){
if(typeof part === 'string'){
return "";
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"reasoning")){
var or__5142__auto__ = (part["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"reasoning_text")){
var or__5142__auto__ = (part["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"thinking")){
var or__5142__auto__ = (part["thinking"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (part["text"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
} else {
return "";

}
}
}
}
});
knoxx.backend.text.overlap_length = (function knoxx$backend$text$overlap_length(left,right){
var left__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = left;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var right__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = right;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var limit = cljs.core.min.cljs$core$IFn$_invoke$arity$2(((left__$1).length),((right__$1).length));
var n = limit;
while(true){
if((n === (0))){
return (0);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(left__$1.slice((((left__$1).length) - n)),right__$1.slice((0),n))){
return n;
} else {
var G__517905 = (n - (1));
n = G__517905;
continue;

}
}
break;
}
});
knoxx.backend.text.merge_monotonic_text = (function knoxx$backend$text$merge_monotonic_text(acc,fragment){
var acc__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = acc;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var fragment__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = fragment;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(fragment__$1)){
return acc__$1;
} else {
if(clojure.string.blank_QMARK_(acc__$1)){
return fragment__$1;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(acc__$1,fragment__$1)){
return acc__$1;
} else {
if(clojure.string.starts_with_QMARK_(fragment__$1,acc__$1)){
return fragment__$1;
} else {
if(clojure.string.starts_with_QMARK_(acc__$1,fragment__$1)){
return acc__$1;
} else {
var overlap = knoxx.backend.text.overlap_length(acc__$1,fragment__$1);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(acc__$1)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fragment__$1.slice(overlap)));

}
}
}
}
}
});
knoxx.backend.text.merge_part_texts = (function knoxx$backend$text$merge_part_texts(parts,part__GT_text){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,part){
return knoxx.backend.text.merge_monotonic_text(acc,(part__GT_text.cljs$core$IFn$_invoke$arity$1 ? part__GT_text.cljs$core$IFn$_invoke$arity$1(part) : part__GT_text.call(null,part)));
}),"",parts);
});
knoxx.backend.text.assistant_message_text = (function knoxx$backend$text$assistant_message_text(message){
var content = (message["content"]);
var merged = ((typeof content === 'string')?content:(cljs.core.truth_(cljs.core.array_QMARK_(content))?knoxx.backend.text.merge_part_texts(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content),knoxx.backend.text.content_part_text):""
));
if((!(clojure.string.blank_QMARK_(merged)))){
return merged;
} else {
if(typeof (message["text"]) === 'string'){
return (message["text"]);
} else {
if(typeof (message["errorMessage"]) === 'string'){
return (message["errorMessage"]);
} else {
return "";

}
}
}
});
knoxx.backend.text.assistant_message_reasoning_text = (function knoxx$backend$text$assistant_message_reasoning_text(message){
var content = (message["content"]);
var merged = (cljs.core.truth_(cljs.core.array_QMARK_(content))?knoxx.backend.text.merge_part_texts(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content),knoxx.backend.text.reasoning_part_text):""
);
if((!(clojure.string.blank_QMARK_(merged)))){
return merged;
} else {
if(typeof (message["reasoning_content"]) === 'string'){
return (message["reasoning_content"]);
} else {
if(typeof (message["reasoningContent"]) === 'string'){
return (message["reasoningContent"]);
} else {
if(typeof (message["reasoning_text"]) === 'string'){
return (message["reasoning_text"]);
} else {
if(typeof (message["reasoning"]) === 'string'){
return (message["reasoning"]);
} else {
if(typeof (message["thinking"]) === 'string'){
return (message["thinking"]);
} else {
return "";

}
}
}
}
}
}
});
knoxx.backend.text.semantic_search_result_text = (function knoxx$backend$text$semantic_search_result_text(p__517650){
var map__517651 = p__517650;
var map__517651__$1 = cljs.core.__destructure_map(map__517651);
var database = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517651__$1,new cljs.core.Keyword(null,"database","database",1849087575));
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517651__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var results = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517651__$1,new cljs.core.Keyword(null,"results","results",-1134170113));
if(cljs.core.seq(results)){
return (""+"Active corpus: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(database))+"\n"+"Query: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,result){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(result))+"\n   score: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Number(new cljs.core.Keyword(null,"score","score",-1963588780).cljs$core$IFn$_invoke$arity$1(result))).toFixed((2)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(result))?(""+", indexed chunks: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666).cljs$core$IFn$_invoke$arity$1(result))):null))+"\n   snippet: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(result)));
}),results))));
} else {
return (""+"Active corpus: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(database))+"\n"+"Query: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\n\nNo strong semantic matches found.");
}
});
knoxx.backend.text.openplanner_semantic_search_text = (function knoxx$backend$text$openplanner_semantic_search_text(result){
var query = new cljs.core.Keyword(null,"query","query",-1288509510).cljs$core$IFn$_invoke$arity$1(result);
var mode = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(result);
var hits = cljs.core.seq(new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(result));
if(hits){
return (""+"OpenPlanner semantic search for: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\nMode: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(mode))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,hit){
var metadata = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"metadata","metadata",1799301597).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var doc = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"document","document",-1329188687).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var source_path = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"sourcePath","sourcePath",-986600405).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var distance = new cljs.core.Keyword(null,"distance","distance",-1671893894).cljs$core$IFn$_invoke$arity$1(hit);
var kind = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = kind;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "doc";
}
})())+"]"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((typeof distance === 'number')?(""+" distance="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Number(distance)).toFixed((3)))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(source_path))?null:(""+"\n   path: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(source_path))))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(temp__5825__auto__)){
var title = temp__5825__auto__;
return (""+"\n   title: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(title));
} else {
return null;
}
})())+"\n   "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(doc,(320)) : knoxx.backend.text.value__GT_preview_text.call(null,doc,(320)));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}),hits))));
} else {
return (""+"OpenPlanner semantic search for: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\nNo indexed documents matched.");
}
});
knoxx.backend.text.semantic_read_result_text = (function knoxx$backend$text$semantic_read_result_text(p__517717){
var map__517718 = p__517717;
var map__517718__$1 = cljs.core.__destructure_map(map__517718);
var database = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517718__$1,new cljs.core.Keyword(null,"database","database",1849087575));
var path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517718__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var content = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517718__$1,new cljs.core.Keyword(null,"content","content",15833224));
var truncated = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517718__$1,new cljs.core.Keyword(null,"truncated","truncated",298102102));
return (""+"Active corpus: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(database))+"\n"+"Path: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(truncated)?"\nNote: content truncated for tool safety.":null))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content));
});
knoxx.backend.text.value__GT_preview_text = (function knoxx$backend$text$value__GT_preview_text(var_args){
var G__517723 = arguments.length;
switch (G__517723) {
case 1:
return knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(value,(800));
}));

(knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2 = (function (value,max_chars){
var raw = ((knoxx.backend.http.no_content_QMARK_(value))?"":((typeof value === 'string')?(function (){var trimmed = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)));
var lowered = clojure.string.lower_case(trimmed);
if(clojure.string.blank_QMARK_(trimmed)){
return "";
} else {
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(lowered,"null")) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(lowered,"undefined")))){
return "";
} else {
var or__5142__auto__ = knoxx.backend.text.summarize_json_string(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return trimmed;
}

}
}
})():((((cljs.core.map_QMARK_(value)) || (((cljs.core.vector_QMARK_(value)) || (cljs.core.seq_QMARK_(value))))))?(function (){var or__5142__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$1(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$1(value);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([value], 0));
}
}
})():(function (){try{var clj_value = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var or__5142__auto__ = knoxx.backend.text.summarize_structured.cljs$core$IFn$_invoke$arity$1(clj_value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.text.markdown_bullets.cljs$core$IFn$_invoke$arity$1(clj_value);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var json = JSON.stringify(value,null,(2));
if(typeof json === 'string'){
return json;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
}
}
}
}catch (e517737){var _ = e517737;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
}})()
)));
var vec__517728 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(raw,max_chars);
var text = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517728,(0),null);
var truncated = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517728,(1),null);
if(clojure.string.blank_QMARK_(text)){
if((((!((value == null)))) && (((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(value,undefined)) && ((((!(typeof value === 'string'))) && ((((!(typeof value === 'number'))) && ((!(cljs.core.boolean_QMARK_(value)))))))))))){
try{var json = JSON.stringify(value);
if(((typeof json === 'string') && ((!(clojure.string.blank_QMARK_(json)))))){
var vec__517745 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(json,max_chars);
var jt = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517745,(0),null);
var jtrunc = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517745,(1),null);
if(cljs.core.truth_(jtrunc)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(jt)+"\u2026");
} else {
return jt;
}
} else {
return null;
}
}catch (e517743){var _ = e517743;
return null;
}} else {
return null;
}
} else {
if(cljs.core.truth_(truncated)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)+"\u2026");
} else {
return text;
}
}
}));

(knoxx.backend.text.value__GT_preview_text.cljs$lang$maxFixedArity = 2);

knoxx.backend.text.float_format = (function knoxx$backend$text$float_format(x){
if(typeof x === 'number'){
return Number(x).toFixed((3));
} else {
return null;
}
});
knoxx.backend.text.openplanner_memory_search_text = (function knoxx$backend$text$openplanner_memory_search_text(p__517761){
var map__517762 = p__517761;
var map__517762__$1 = cljs.core.__destructure_map(map__517762);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517762__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var mode = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517762__$1,new cljs.core.Keyword(null,"mode","mode",654403691));
var hits = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517762__$1,new cljs.core.Keyword(null,"hits","hits",-2120002930));
if(cljs.core.seq(hits)){
return (""+"OpenPlanner memory search for: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\nMode: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(mode))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,hit){
var metadata = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"metadata","metadata",1799301597).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return hit;
}
})();
var session = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown-session";
}
}
})();
var role = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "memory";
}
}
})();
var snippet = knoxx.backend.text.sanitize_svg_content((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"document","document",-1329188687).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})());
var distance = new cljs.core.Keyword(null,"distance","distance",-1671893894).cljs$core$IFn$_invoke$arity$1(hit);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". session="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session)+", role="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(role)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((typeof distance === 'number')?(""+", distance="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Number(distance)).toFixed((3)))):null))+"\n   "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(snippet,(280));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}),hits))));
} else {
return (""+"OpenPlanner memory search for: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query)+"\nNo prior Knoxx memory hits found.");
}
});
knoxx.backend.text.websearch_result_text = (function knoxx$backend$text$websearch_result_text(p__517788){
var map__517789 = p__517788;
var map__517789__$1 = cljs.core.__destructure_map(map__517789);
var output = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517789__$1,new cljs.core.Keyword(null,"output","output",-1105869043));
var sources = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517789__$1,new cljs.core.Keyword(null,"sources","sources",-321166424));
var model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517789__$1,new cljs.core.Keyword(null,"model","model",331153215));
return (""+"Web search"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(model)?(""+" via "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model)):null))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = output;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.seq(sources))?(""+"\n\nSources:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (source){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(source);
}
})())+" \u2014 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(source)));
}),sources)))):null)));
});
knoxx.backend.text.openplanner_session_text = (function knoxx$backend$text$openplanner_session_text(session_id,rows){
if(cljs.core.seq(rows)){
return (""+"OpenPlanner session "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,row){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "event";
}
})())+"] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(knoxx.backend.text.sanitize_svg_content((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()),(320));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}),rows))));
} else {
return (""+"OpenPlanner session "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)+" is empty or missing.");
}
});
knoxx.backend.text.graph_query_result_text = (function knoxx$backend$text$graph_query_result_text(result){
var nodes = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"nodes","nodes",-2099585805).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var edges = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"edges","edges",-694791395).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var stats = new cljs.core.Keyword(null,"stats","stats",-85643011).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.seq(nodes)){
var node_text = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (idx,node){
var data = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(node);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var label = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(node);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(node);
}
})();
var text = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(node);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"preview","preview",451279890).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(node));
}
})();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((idx + (1)))+". ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"lake","lake",805586599).cljs$core$IFn$_invoke$arity$1(node))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"nodeType","nodeType",-639803451).cljs$core$IFn$_invoke$arity$1(node))+"] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"score","score",-1963588780).cljs$core$IFn$_invoke$arity$1(node);
if(cljs.core.truth_(temp__5825__auto__)){
var s = temp__5825__auto__;
return (""+" (score="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.text.float_format(s))+")");
} else {
return null;
}
})())+"\n   id="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(node))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(data);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__517811 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(node);
if((G__517811 == null)){
return null;
} else {
return clojure.string.replace(G__517811,/^[^:]+:/,"");
}
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var path = temp__5825__auto__;
return (""+"\n   path="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(data);
if(cljs.core.truth_(temp__5825__auto__)){
var url = temp__5825__auto__;
return (""+"\n   url="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(text)?(""+"\n   text="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(text,(280));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())):null)));
}),nodes));
var edge_text = ((cljs.core.seq(edges))?clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (edge){
var etype = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"edgeType","edgeType",-1635019622).cljs$core$IFn$_invoke$arity$1(edge);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = new cljs.core.Keyword(null,"similarity","similarity",1871163855).cljs$core$IFn$_invoke$arity$1(edge);
if(cljs.core.truth_(temp__5825__auto__)){
var sim = temp__5825__auto__;
return (""+"sim="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.text.float_format(sim)));
} else {
return null;
}
}
})();
return (""+"- ["+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = etype;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "link";
}
})())+"] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(edge))+" -> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(edge)));
}),edges)):null);
var clusters_text = (function (){var temp__5825__auto__ = cljs.core.seq(new cljs.core.Keyword(null,"clusters","clusters",273881275).cljs$core$IFn$_invoke$arity$1(result));
if(temp__5825__auto__){
var clusters = temp__5825__auto__;
return (""+"\n\nClusters:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (c){
return (""+"  "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"lake","lake",805586599).cljs$core$IFn$_invoke$arity$1(c))+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"count","count",2139924085).cljs$core$IFn$_invoke$arity$1(c))+" nodes"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto____$1 = cljs.core.seq(new cljs.core.Keyword(null,"topNodes","topNodes",1188708685).cljs$core$IFn$_invoke$arity$1(c));
if(temp__5825__auto____$1){
var top = temp__5825__auto____$1;
return (""+"\n    top: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),top))));
} else {
return null;
}
})()));
}),clusters))));
} else {
return null;
}
})();
return (""+"Knowledge graph query"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"query","query",-1288509510).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(temp__5825__auto__)){
var q = temp__5825__auto__;
return (""+"\nQuery: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(q));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = cljs.core.seq(new cljs.core.Keyword(null,"projects","projects",-364845983).cljs$core$IFn$_invoke$arity$1(result));
if(temp__5825__auto__){
var projects = temp__5825__auto__;
return (""+"\nProjects: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",projects)));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(stats)?(""+"\nStats: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([stats], 0)))):null))+"\n\nNodes:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_text)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(edge_text)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clusters_text));
} else {
return (""+"Knowledge graph query returned no matching nodes."+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(stats)?(""+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([stats], 0)))):null)));
}
});

//# sourceMappingURL=knoxx.backend.text.js.map
