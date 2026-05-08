import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.util.time.js";
import "./knoxx.backend.text.js";
goog.provide('knoxx.backend.session_titles');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.session_titles !== 'undefined') && (typeof knoxx.backend.session_titles.session_titles_STAR_ !== 'undefined')){
} else {
knoxx.backend.session_titles.session_titles_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.session_titles !== 'undefined') && (typeof knoxx.backend.session_titles.session_title_promises_STAR_ !== 'undefined')){
} else {
knoxx.backend.session_titles.session_title_promises_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.session_titles !== 'undefined') && (typeof knoxx.backend.session_titles.session_title_generation_tail_STAR_ !== 'undefined')){
} else {
knoxx.backend.session_titles.session_title_generation_tail_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(Promise.resolve(null));
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.session_titles !== 'undefined') && (typeof knoxx.backend.session_titles.session_title_backfill_STAR_ !== 'undefined')){
} else {
knoxx.backend.session_titles.session_title_backfill_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"active","active",1895962068),false,new cljs.core.Keyword(null,"processed","processed",800622264),(0),new cljs.core.Keyword(null,"total","total",1916810418),(0),new cljs.core.Keyword(null,"failed","failed",-1397425762),(0),new cljs.core.Keyword(null,"force","force",781957286),false,new cljs.core.Keyword(null,"started_at","started_at",856896776),null,new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),null,new cljs.core.Keyword(null,"last_error","last_error",153231245),null], null));
}
knoxx.backend.session_titles.SESSION_TITLE_TTL_SECONDS = ((((60) * (60)) * (24)) * (7));
knoxx.backend.session_titles.SESSION_TITLES_CACHE_MAX = (512);
knoxx.backend.session_titles.session_title_key = (function knoxx$backend$session_titles$session_title_key(session_id){
return (""+"knoxx:session-title:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id))));
});
knoxx.backend.session_titles.resolved = (function knoxx$backend$session_titles$resolved(value){
return Promise.resolve(value);
});
/**
 * Serialize Proxx-backed title generation so cache misses cannot fan out into
 * a provider request storm. The returned promise preserves the task result;
 * the queue tail always recovers so one failed naming request does not stall
 * later titles.
 */
knoxx.backend.session_titles.enqueue_session_title_generation_BANG_ = (function knoxx$backend$session_titles$enqueue_session_title_generation_BANG_(task_fn){
var task = cljs.core.deref(knoxx.backend.session_titles.session_title_generation_tail_STAR_).catch((function (_){
return null;
})).then((function (){
return (task_fn.cljs$core$IFn$_invoke$arity$0 ? task_fn.cljs$core$IFn$_invoke$arity$0() : task_fn.call(null));
}));
cljs.core.reset_BANG_(knoxx.backend.session_titles.session_title_generation_tail_STAR_,task.catch((function (_){
return null;
})));

return task;
});
knoxx.backend.session_titles.sanitize_session_title = (function knoxx$backend$session_titles$sanitize_session_title(value){
var text = clojure.string.trim(clojure.string.replace(clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\s+/," ")),/^[`'\"“”‘’]+|[`'\"“”‘’]+$/,""));
var lowered = clojure.string.lower_case(text);
var text__$1 = ((clojure.string.starts_with_QMARK_(lowered,"title: "))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(text,(7)):((clojure.string.starts_with_QMARK_(lowered,"title-"))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(text,(6)):((clojure.string.starts_with_QMARK_(lowered,"title:"))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(text,(6)):text
)));
var text__$2 = clojure.string.trim(text__$1);
if(clojure.string.blank_QMARK_(text__$2)){
return null;
} else {
return cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text__$2,(0),cljs.core.min.cljs$core$IFn$_invoke$arity$2((160),((text__$2).length)));
}
});
knoxx.backend.session_titles.heuristic_session_title = (function knoxx$backend$session_titles$heuristic_session_title(seed_text){
var words = clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__522725_SHARP_){
return clojure.string.replace(p1__522725_SHARP_,/^[#>*\-\d.\s]+/,"");
}),cljs.core.take.cljs$core$IFn$_invoke$arity$2((2),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split_lines((function (){var or__5142__auto__ = seed_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))))))));
var cleaned = (function (){var G__522735 = words;
var G__522735__$1 = (((G__522735 == null))?null:clojure.string.lower_case(G__522735));
if((G__522735__$1 == null)){
return null;
} else {
return knoxx.backend.session_titles.sanitize_session_title(G__522735__$1);
}
})();
var or__5142__auto__ = cleaned;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Untitled session";
}
});
knoxx.backend.session_titles.acceptable_session_title_QMARK_ = (function knoxx$backend$session_titles$acceptable_session_title_QMARK_(value){
var title = knoxx.backend.session_titles.sanitize_session_title(value);
var lowered = (function (){var G__522742 = title;
if((G__522742 == null)){
return null;
} else {
return clojure.string.lower_case(G__522742);
}
})();
return cljs.core.boolean$((function (){var and__5140__auto__ = title;
if(cljs.core.truth_(and__5140__auto__)){
return (((cljs.core.count(title) >= (4))) && ((!(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, ["res",null,"untitled",null,"chat",null,"untitled session",null,"title",null,"session",null,"new chat",null], null), null),lowered)))));
} else {
return and__5140__auto__;
}
})());
});
knoxx.backend.session_titles.normalize_session_title = (function knoxx$backend$session_titles$normalize_session_title(var_args){
var G__522750 = arguments.length;
switch (G__522750) {
case 1:
return knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2(value,null);
}));

(knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2 = (function (value,fallback){
var title = knoxx.backend.session_titles.sanitize_session_title(value);
var fallback_title = knoxx.backend.session_titles.sanitize_session_title(fallback);
if(knoxx.backend.session_titles.acceptable_session_title_QMARK_(title)){
return title;
} else {
if(knoxx.backend.session_titles.acceptable_session_title_QMARK_(fallback_title)){
return fallback_title;
} else {
return null;

}
}
}));

(knoxx.backend.session_titles.normalize_session_title.cljs$lang$maxFixedArity = 2);

knoxx.backend.session_titles.session_title_seed_text = (function knoxx$backend$session_titles$session_title_seed_text(rows){
var user_texts = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__522757_SHARP_){
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(p1__522757_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__522756_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("user",new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__522756_SHARP_));
}),(function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
var substantive = cljs.core.first(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (text){
return (((cljs.core.count(text) >= (12))) || ((cljs.core.count(clojure.string.split.cljs$core$IFn$_invoke$arity$2(text,/\s+/)) >= (3))));
}),user_texts));
var combined = (function (){var G__522768 = user_texts;
var G__522768__$1 = (((G__522768 == null))?null:cljs.core.take.cljs$core$IFn$_invoke$arity$2((3),G__522768));
var G__522768__$2 = (((G__522768__$1 == null))?null:clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",G__522768__$1));
var G__522768__$3 = (((G__522768__$2 == null))?null:clojure.string.trim(G__522768__$2));
if((G__522768__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__522768__$3);
}
})();
var fallback = cljs.core.first(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__522759_SHARP_){
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(p1__522759_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
}),(function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
var or__5142__auto__ = substantive;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = combined;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = fallback;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
});
knoxx.backend.session_titles.title_from_reasoning_content = (function knoxx$backend$session_titles$title_from_reasoning_content(value){
var text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var or__5142__auto__ = (function (){var G__522811 = cljs.core.re_find(/(?:i(?:'|’)ll|i will) go with\s+[\"“]([^\"”]{4,80})[\"”]/i,text);
var G__522811__$1 = (((G__522811 == null))?null:cljs.core.second(G__522811));
if((G__522811__$1 == null)){
return null;
} else {
return knoxx.backend.session_titles.sanitize_session_title(G__522811__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__522813 = cljs.core.re_seq(/[\"“]([^\"”]{4,80})[\"”]/,text);
var G__522813__$1 = (((G__522813 == null))?null:cljs.core.last(G__522813));
var G__522813__$2 = (((G__522813__$1 == null))?null:cljs.core.second(G__522813__$1));
if((G__522813__$2 == null)){
return null;
} else {
return knoxx.backend.session_titles.sanitize_session_title(G__522813__$2);
}
}
});
knoxx.backend.session_titles.parse_json_object = (function knoxx$backend$session_titles$parse_json_object(value){
if(cljs.core.map_QMARK_(value)){
return value;
} else {
if(typeof value === 'string'){
try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(value),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e522816){var _ = e522816;
return null;
}} else {
return null;

}
}
});
knoxx.backend.session_titles.session_title_row_entry = (function knoxx$backend$session_titles$session_title_row_entry(row){
var extra = (function (){var or__5142__auto__ = knoxx.backend.session_titles.parse_json_object(new cljs.core.Keyword(null,"extra","extra",1612569067).cljs$core$IFn$_invoke$arity$1(row));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var kind = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"event_kind","event_kind",1009075217).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(extra,new cljs.core.Keyword(null,"kind","kind",-717265803));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(extra,new cljs.core.Keyword(null,"event_kind","event_kind",1009075217));
}
}
}
})();
var raw_title = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(extra,new cljs.core.Keyword(null,"title","title",636505583));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"knoxx.session_title")){
return new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(row);
} else {
return null;
}
}
}
})();
var title = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1(raw_title);
var title_model = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(extra,new cljs.core.Keyword(null,"title_model","title_model",501758950));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(extra,new cljs.core.Keyword(null,"titleModel","titleModel",-2089428163));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"titleModel","titleModel",-2089428163).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(row);
}
}
}
}
})();
var updated_at = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ts","ts",1617209904).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(row);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.util.time.now_iso();
}
}
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"knoxx.session_title");
if(and__5140__auto__){
return title;
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"title","title",636505583),title,new cljs.core.Keyword(null,"title_model","title_model",501758950),title_model,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),updated_at], null);
} else {
return null;
}
});
knoxx.backend.session_titles.stored_session_title_entry = (function knoxx$backend$session_titles$stored_session_title_entry(session_id,rows){
var temp__5825__auto__ = (function (){var G__522820 = (function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var G__522820__$1 = (((G__522820 == null))?null:cljs.core.reverse(G__522820));
var G__522820__$2 = (((G__522820__$1 == null))?null:cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.session_titles.session_title_row_entry,G__522820__$1));
if((G__522820__$2 == null)){
return null;
} else {
return cljs.core.first(G__522820__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var entry = temp__5825__auto__;
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(entry,new cljs.core.Keyword(null,"session","session",1008279103),session_id);
} else {
return null;
}
});
/**
 * When session-titles* exceeds SESSION_TITLES_CACHE_MAX, evict oldest entries.
 */
knoxx.backend.session_titles.evict_stale_titles_BANG_ = (function knoxx$backend$session_titles$evict_stale_titles_BANG_(){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.session_titles.session_titles_STAR_,(function (titles){
if((cljs.core.count(titles) <= knoxx.backend.session_titles.SESSION_TITLES_CACHE_MAX)){
return titles;
} else {
var sorted = cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2((function (p__522828){
var vec__522829 = p__522828;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522829,(0),null);
var entry = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__522829,(1),null);
var or__5142__auto__ = new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(entry);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
}),titles);
var drop_n = (cljs.core.count(sorted) - knoxx.backend.session_titles.SESSION_TITLES_CACHE_MAX);
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,cljs.core.drop.cljs$core$IFn$_invoke$arity$2(drop_n,sorted));
}
}));

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.session_titles.session_title_promises_STAR_,(function (promises){
if((cljs.core.count(promises) <= knoxx.backend.session_titles.SESSION_TITLES_CACHE_MAX)){
return promises;
} else {
var known = cljs.core.set(cljs.core.keys(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_)));
return cljs.core.select_keys(promises,known);
}
}));
});
knoxx.backend.session_titles.cache_session_title_entry_BANG_ = (function knoxx$backend$session_titles$cache_session_title_entry_BANG_(session_id,title,title_model,updated_at){
var resolved = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"title","title",636505583),(function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1(title);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Untitled session";
}
})(),new cljs.core.Keyword(null,"title_model","title_model",501758950),title_model,new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),(function (){var or__5142__auto__ = updated_at;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.time.now_iso();
}
})()], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_titles_STAR_,cljs.core.assoc,session_id,resolved);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_titles.session_title_promises_STAR_,cljs.core.dissoc,session_id);

knoxx.backend.session_titles.evict_stale_titles_BANG_();

var temp__5825__auto___523092 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___523092)){
var redis_client_523093 = temp__5825__auto___523092;
knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$4(redis_client_523093,knoxx.backend.session_titles.session_title_key(session_id),resolved,knoxx.backend.session_titles.SESSION_TITLE_TTL_SECONDS).catch((function (err){
console.warn("Failed to persist session title cache into Redis",err);

return null;
}));
} else {
}

return resolved;
});
knoxx.backend.session_titles.clear_session_title_entry_BANG_ = (function knoxx$backend$session_titles$clear_session_title_entry_BANG_(session_id){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_titles.session_titles_STAR_,cljs.core.dissoc,session_id);

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_titles.session_title_promises_STAR_,cljs.core.dissoc,session_id);

var temp__5825__auto___523094 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___523094)){
var redis_client_523095 = temp__5825__auto___523094;
knoxx.backend.redis_client.del(redis_client_523095,knoxx.backend.session_titles.session_title_key(session_id)).catch((function (err){
console.warn("Failed to clear session title cache from Redis",err);

return null;
}));
} else {
}

return null;
});
knoxx.backend.session_titles.get_cached_session_title_BANG_ = (function knoxx$backend$session_titles$get_cached_session_title_BANG_(session_id){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(session_id__$1)){
return knoxx.backend.session_titles.resolved(null);
} else {
if(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_),session_id__$1)){
return knoxx.backend.session_titles.resolved(cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_),session_id__$1));
} else {
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var redis_client = temp__5823__auto__;
return knoxx.backend.redis_client.get_json(redis_client,knoxx.backend.session_titles.session_title_key(session_id__$1)).then((function (entry){
if(cljs.core.truth_(entry)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_titles_STAR_,cljs.core.assoc,session_id__$1,entry);
} else {
}

return entry;
}));
} else {
return knoxx.backend.session_titles.resolved(null);
}

}
}
});
knoxx.backend.session_titles.session_title_event = (function knoxx$backend$session_titles$session_title_event(config,session_id,title,title_model){
var event_id = (""+"knoxx:session-title:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id));
var ts = knoxx.backend.util.time.now_iso();
var normalized_title = (function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1(title);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Untitled session";
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"schema","schema",-1582001791),new cljs.core.Keyword(null,"meta","meta",1499536964),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"source_ref","source_ref",-1854699662),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"text","text",-1790561697)],["openplanner.event.v1",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"role","role",-736691072),"system",new cljs.core.Keyword(null,"author","author",2111686192),"knoxx",new cljs.core.Keyword(null,"model","model",331153215),title_model,new cljs.core.Keyword(null,"tags","tags",1771418977),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["knoxx","session_title","metadata"], null)], null),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"kind","kind",-717265803),"knoxx.session_title",new cljs.core.Keyword(null,"title","title",636505583),normalized_title,new cljs.core.Keyword(null,"title_model","title_model",501758950),title_model,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null),"knoxx",ts,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"message","message",-406056002),event_id], null),event_id,"knoxx.session_title",normalized_title]);
});
knoxx.backend.session_titles.persist_session_title_BANG_ = (function knoxx$backend$session_titles$persist_session_title_BANG_(config,session_id,title,title_model){
if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))){
return Promise.resolve(null);
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/events",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.session_titles.session_title_event(config,session_id,title,title_model)], null)], null)).catch((function (err){
console.warn("Failed to persist session title into OpenPlanner",err);

return null;
}));
}
});
knoxx.backend.session_titles.cache_session_title_BANG_ = (function knoxx$backend$session_titles$cache_session_title_BANG_(_runtime,_config,session_id,title,title_model){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var resolved = knoxx.backend.session_titles.cache_session_title_entry_BANG_(session_id__$1,title,title_model,null);
return resolved;
});
knoxx.backend.session_titles.preload_session_title_entry_BANG_ = (function knoxx$backend$session_titles$preload_session_title_entry_BANG_(config,session_id){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(session_id))+"?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config))))).then((function (body){
var temp__5825__auto__ = knoxx.backend.session_titles.stored_session_title_entry(session_id,new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body));
if(cljs.core.truth_(temp__5825__auto__)){
var entry = temp__5825__auto__;
return knoxx.backend.session_titles.cache_session_title_entry_BANG_(session_id,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(entry));
} else {
return null;
}
})).catch((function (_){
return null;
}));
});
knoxx.backend.session_titles.load_session_titles_BANG_ = (function knoxx$backend$session_titles$load_session_titles_BANG_(_runtime,config){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
return Promise.resolve(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_));
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config))))).then((function (body){
var session_ids = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((64),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session","session",1008279103),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))))));
if(cljs.core.empty_QMARK_(session_ids)){
return cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_);
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$3(knoxx.backend.session_titles.preload_session_title_entry_BANG_,cljs.core.repeat.cljs$core$IFn$_invoke$arity$1(config),session_ids))).then((function (_){
return cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_);
}));
}
})).catch((function (err){
console.warn("Failed to preload session titles from OpenPlanner",err);

return Promise.resolve(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_));
}));
}
});
knoxx.backend.session_titles.resolve_session_title_BANG_ = (function knoxx$backend$session_titles$resolve_session_title_BANG_(config,seed_text){
var fallback = knoxx.backend.session_titles.heuristic_session_title(seed_text);
return knoxx.backend.session_titles.enqueue_session_title_generation_BANG_((function (){
return (knoxx.backend.session_titles.generate_session_title_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.session_titles.generate_session_title_BANG_.cljs$core$IFn$_invoke$arity$2(config,seed_text) : knoxx.backend.session_titles.generate_session_title_BANG_.call(null,config,seed_text));
})).then((function (entry){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),(function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),fallback);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return fallback;
}
})(),new cljs.core.Keyword(null,"title_model","title_model",501758950),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry)], null);
})).catch((function (_){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),fallback,new cljs.core.Keyword(null,"title_model","title_model",501758950),null], null));
}));
});
knoxx.backend.session_titles.generate_session_title_BANG_ = (function knoxx$backend$session_titles$generate_session_title_BANG_(config,seed_text){
var fallback = knoxx.backend.session_titles.heuristic_session_title(seed_text);
if(((clojure.string.blank_QMARK_(seed_text)) || (((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))) || (clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))))))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),fallback,new cljs.core.Keyword(null,"title_model","title_model",501758950),null], null));
} else {
var payload = ({"model": "auto:cheapest", "messages": cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"system",new cljs.core.Keyword(null,"content","content",15833224),"You create very short, useful session titles. Return only the title text, 2 to 6 words, with no quotes, no markdown, and no explanation."], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"role","role",-736691072),"user",new cljs.core.Keyword(null,"content","content",15833224),(""+"Create a concise title for this Knoxx session based on the opening request.\n\nRequest:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(seed_text,(900));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))], null)], null)), "temperature": 0.1, "max_tokens": (24), "stream": false});
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/chat/completions"),({"method": "POST", "headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)), "body": JSON.stringify(payload)})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
var body = (resp["body"]);
var choices = (function (){var or__5142__auto__ = (body["choices"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var first_choice = (choices[(0)]);
var message = (function (){var or__5142__auto__ = (first_choice["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var content = (function (){var or__5142__auto__ = (message["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (first_choice["text"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var reasoning_content = (function (){var or__5142__auto__ = (message["reasoning_content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (message["reasoningContent"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var title_candidate = (function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1(content);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.session_titles.title_from_reasoning_content(reasoning_content);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return fallback;
}
}
})();
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),(function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2(title_candidate,fallback);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return fallback;
}
})(),new cljs.core.Keyword(null,"title_model","title_model",501758950),(function (){var or__5142__auto__ = (body["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "auto:cheapest";
}
})()], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),fallback,new cljs.core.Keyword(null,"title_model","title_model",501758950),null], null);
}
})).catch((function (_){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),fallback,new cljs.core.Keyword(null,"title_model","title_model",501758950),null], null));
}));
}
});
knoxx.backend.session_titles.resolve_session_title_from_rows_BANG_ = (function knoxx$backend$session_titles$resolve_session_title_from_rows_BANG_(config,session_id,rows){
var temp__5823__auto__ = knoxx.backend.session_titles.stored_session_title_entry(session_id,rows);
if(cljs.core.truth_(temp__5823__auto__)){
var stored = temp__5823__auto__;
return Promise.resolve(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(stored,new cljs.core.Keyword(null,"stored","stored",-892842620),true));
} else {
var seed_text = knoxx.backend.session_titles.session_title_seed_text(cljs.core.vec((function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
return knoxx.backend.session_titles.resolve_session_title_BANG_(config,seed_text).then((function (entry){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"stored","stored",-892842620),false], null);
}));
}
});
knoxx.backend.session_titles.ensure_session_title_BANG_ = (function knoxx$backend$session_titles$ensure_session_title_BANG_(runtime,config,session_id,rows,force_QMARK_,fetch_session_rows_BANG_){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(cljs.core.truth_(force_QMARK_)){
knoxx.backend.session_titles.clear_session_title_entry_BANG_(session_id__$1);
} else {
}

if(clojure.string.blank_QMARK_(session_id__$1)){
return knoxx.backend.session_titles.resolved(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),"Untitled session",new cljs.core.Keyword(null,"title_model","title_model",501758950),null], null));
} else {
if(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.session_titles.session_title_promises_STAR_),session_id__$1)){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_titles.session_title_promises_STAR_),session_id__$1);
} else {
var title_promise = knoxx.backend.session_titles.get_cached_session_title_BANG_(session_id__$1).then((function (cached){
if(cljs.core.truth_(cached)){
return cached;
} else {
return ((cljs.core.seq(rows))?knoxx.backend.session_titles.resolved(rows):(fetch_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2(config,session_id__$1) : fetch_session_rows_BANG_.call(null,config,session_id__$1))).then((function (resolved_rows){
return knoxx.backend.session_titles.resolve_session_title_from_rows_BANG_(config,session_id__$1,resolved_rows);
})).then((function (entry){
if(cljs.core.truth_(new cljs.core.Keyword(null,"stored","stored",-892842620).cljs$core$IFn$_invoke$arity$1(entry))){
return knoxx.backend.session_titles.cache_session_title_entry_BANG_(session_id__$1,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(entry));
} else {
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry));
}
})).catch((function (_){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,"Untitled session",null);
}));
}
}));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_title_promises_STAR_,cljs.core.assoc,session_id__$1,title_promise);

return title_promise;

}
}
});
knoxx.backend.session_titles.maybe_prime_session_title_BANG_ = (function knoxx$backend$session_titles$maybe_prime_session_title_BANG_(runtime,config,session_id,seed_text){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var seed_text__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = seed_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if((((!(clojure.string.blank_QMARK_(session_id__$1)))) && ((((!(clojure.string.blank_QMARK_(seed_text__$1)))) && ((!(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.session_titles.session_title_promises_STAR_),session_id__$1)))))))){
var title_promise = knoxx.backend.session_titles.get_cached_session_title_BANG_(session_id__$1).then((function (cached){
if(cljs.core.truth_(cached)){
return cached;
} else {
return knoxx.backend.session_titles.resolve_session_title_BANG_(config,seed_text__$1).then((function (entry){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry));
})).catch((function (_){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,knoxx.backend.session_titles.heuristic_session_title(seed_text__$1),null);
}));
}
}));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_title_promises_STAR_,cljs.core.assoc,session_id__$1,title_promise);

return title_promise;
} else {
return null;
}
});
knoxx.backend.session_titles.start_session_title_backfill_BANG_ = (function knoxx$backend$session_titles$start_session_title_backfill_BANG_(runtime,config,p__522963,fetch_session_rows_BANG_){
var map__522964 = p__522963;
var map__522964__$1 = cljs.core.__destructure_map(map__522964);
var force = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522964__$1,new cljs.core.Keyword(null,"force","force",781957286));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__522964__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
if(cljs.core.truth_(new cljs.core.Keyword(null,"active","active",1895962068).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_)))){
return Promise.resolve(cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_));
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config))))).then((function (body){
var session_ids = (function (){var G__522974 = cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session","session",1008279103),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.truth_(limit)){
return cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit,G__522974);
} else {
return G__522974;
}
})();
var session_ids__$1 = cljs.core.vec(session_ids);
cljs.core.reset_BANG_(knoxx.backend.session_titles.session_title_backfill_STAR_,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"active","active",1895962068),true,new cljs.core.Keyword(null,"processed","processed",800622264),(0),new cljs.core.Keyword(null,"total","total",1916810418),cljs.core.count(session_ids__$1),new cljs.core.Keyword(null,"failed","failed",-1397425762),(0),new cljs.core.Keyword(null,"force","force",781957286),cljs.core.boolean$(force),new cljs.core.Keyword(null,"started_at","started_at",856896776),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),null,new cljs.core.Keyword(null,"last_error","last_error",153231245),null], null));

if(cljs.core.empty_QMARK_(session_ids__$1)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.session_titles.session_title_backfill_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"active","active",1895962068),false,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),knoxx.backend.util.time.now_iso()], 0));

return cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_);
} else {
var step = (function knoxx$backend$session_titles$start_session_title_backfill_BANG__$_step(remaining){
var temp__5823__auto__ = cljs.core.first(remaining);
if(cljs.core.truth_(temp__5823__auto__)){
var session_id = temp__5823__auto__;
if(cljs.core.truth_(force)){
knoxx.backend.session_titles.clear_session_title_entry_BANG_(session_id);
} else {
}

return (fetch_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2(config,session_id) : fetch_session_rows_BANG_.call(null,config,session_id)).then((function (title_rows){
return knoxx.backend.session_titles.resolve_session_title_from_rows_BANG_(config,session_id,title_rows).then((function (entry){
if(cljs.core.truth_(new cljs.core.Keyword(null,"stored","stored",-892842620).cljs$core$IFn$_invoke$arity$1(entry))){
return knoxx.backend.session_titles.cache_session_title_entry_BANG_(session_id,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(entry));
} else {
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id,new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry));
}
}));
})).catch((function (_){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id,"Untitled session",null);
})).then((function (_){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_title_backfill_STAR_,cljs.core.update,new cljs.core.Keyword(null,"processed","processed",800622264),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.inc,(0)));
})).catch((function (err){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.session_titles.session_title_backfill_STAR_,(function (state){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(state,new cljs.core.Keyword(null,"processed","processed",800622264),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.inc,(0))),new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.inc,(0))),new cljs.core.Keyword(null,"last_error","last_error",153231245),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)));
}));

return null;
})).then((function (_){
return knoxx$backend$session_titles$start_session_title_backfill_BANG__$_step(cljs.core.rest(remaining));
}));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.session_titles.session_title_backfill_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"active","active",1895962068),false,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),knoxx.backend.util.time.now_iso()], 0));

return Promise.resolve(cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_));
}
});
step(session_ids__$1).catch((function (err){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.session_titles.session_title_backfill_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"active","active",1895962068),false,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"last_error","last_error",153231245),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], 0));

return null;
}));

return cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_);
}
})).catch((function (err){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.session_titles.session_title_backfill_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"active","active",1895962068),false,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"completed_at","completed_at",1756837256),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"last_error","last_error",153231245),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], 0));

return Promise.resolve(cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_));
}));
}
});

//# sourceMappingURL=knoxx.backend.session_titles.js.map
