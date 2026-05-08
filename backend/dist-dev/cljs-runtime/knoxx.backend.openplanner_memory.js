import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.stores.session_store_registry.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.quality_labels.js";
import "./knoxx.backend.runtime.actor_scope.js";
import "./knoxx.backend.util.time.js";
goog.provide('knoxx.backend.openplanner_memory');
knoxx.backend.openplanner_memory.js_array_seq = (function knoxx$backend$openplanner_memory$js_array_seq(arr){
if((!((arr == null)))){
var iter__5628__auto__ = (function knoxx$backend$openplanner_memory$js_array_seq_$_iter__50774(s__50775){
return (new cljs.core.LazySeq(null,(function (){
var s__50775__$1 = s__50775;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__50775__$1);
if(temp__5825__auto__){
var s__50775__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__50775__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__50775__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__50777 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__50776 = (0);
while(true){
if((i__50776 < size__5627__auto__)){
var i = cljs.core._nth(c__5626__auto__,i__50776);
cljs.core.chunk_append(b__50777,(arr[i]));

var G__51302 = (i__50776 + (1));
i__50776 = G__51302;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__50777),knoxx$backend$openplanner_memory$js_array_seq_$_iter__50774(cljs.core.chunk_rest(s__50775__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__50777),null);
}
} else {
var i = cljs.core.first(s__50775__$2);
return cljs.core.cons((arr[i]),knoxx$backend$openplanner_memory$js_array_seq_$_iter__50774(cljs.core.rest(s__50775__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.range.cljs$core$IFn$_invoke$arity$1(arr.length));
} else {
return null;
}
});
/**
 * Infer document kind from file extension for OpenPlanner indexing.
 */
knoxx.backend.openplanner_memory.guess_document_kind = (function knoxx$backend$openplanner_memory$guess_document_kind(rel_path){
var ext = (function (){var G__50783 = clojure.string.lower_case(rel_path);
var G__50783__$1 = (((G__50783 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__50783,/\./));
if((G__50783__$1 == null)){
return null;
} else {
return cljs.core.last(G__50783__$1);
}
})();
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 15, ["js",null,"cljc",null,"rs",null,"ts",null,"mjs",null,"py",null,"cljs",null,"cjs",null,"clj",null,"java",null,"jsx",null,"php",null,"go",null,"tsx",null,"rb",null], null), null),ext)){
return "code";
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["md",null,"rst",null,"txt",null,"mdx",null,"adoc",null], null), null),ext)){
return "docs";
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, ["json",null,"toml",null,"yml",null,"yaml",null,"conf",null,"env",null,"ini",null], null), null),ext)){
return "config";
} else {
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["xml",null,"csv",null,"tsv",null,"sql",null], null), null),ext)){
return "data";
} else {
return "docs";

}
}
}
}
});
/**
 * Send a document to OpenPlanner's /v1/documents endpoint for indexing.
 * Returns {:ok true, :document ...} on success, or {:ok false ...} on failure.
 */
knoxx.backend.openplanner_memory.upsert_openplanner_document_BANG_ = (function knoxx$backend$openplanner_memory$upsert_openplanner_document_BANG_(config,p__50790){
var map__50791 = p__50790;
var map__50791__$1 = cljs.core.__destructure_map(map__50791);
var domain = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"domain","domain",1847214937));
var rel_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"rel-path","rel-path",593215642));
var visibility = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"visibility","visibility",1338380893));
var content = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"content","content",15833224));
var extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"extra","extra",1612569067));
var title = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"title","title",636505583));
var source_path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"source-path","source-path",-1955873712));
var project = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"project","project",1124394579));
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50791__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
if(knoxx.backend.http.openplanner_enabled_QMARK_(config)){
} else {
throw (new Error("OpenPlanner is not configured"));
}

var doc_id = (function (){var or__5142__auto__ = id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"knoxx-doc:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto____$1 = rel_path;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return crypto.randomUUID();
}
})()));
}
})();
var doc_kind = (function (){var or__5142__auto__ = kind;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.openplanner_memory.guess_document_kind(rel_path);
}
})();
var doc_title = (function (){var or__5142__auto__ = title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__50795 = rel_path;
var G__50795__$1 = (((G__50795 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__50795,/\//));
if((G__50795__$1 == null)){
return null;
} else {
return cljs.core.last(G__50795__$1);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return doc_id;
}
}
})();
var doc_content = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = content;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var doc_project = (function (){var or__5142__auto__ = project;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "devel";
}
}
})();
var payload = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"document","document",-1329188687),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"createdBy","createdBy",-1784489851),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"sourcePath","sourcePath",-986600405),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"language","language",-1591107564),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"domain","domain",1847214937),new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"visibility","visibility",1338380893)],["knoxx-ingestion",doc_content,(function (){var or__5142__auto__ = source_path;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return rel_path;
}
})(),"knoxx-ingestion",doc_title,doc_project,"en",doc_id,doc_kind,(function (){var or__5142__auto__ = domain;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "general";
}
})(),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"indexed_from","indexed_from",218689940),"knoxx"], null),extra], 0)),(function (){var or__5142__auto__ = visibility;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "internal";
}
})()])], null);
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/documents",payload).then((function (resp){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"document","document",-1329188687),new cljs.core.Keyword(null,"document","document",-1329188687).cljs$core$IFn$_invoke$arity$1(resp),new cljs.core.Keyword(null,"indexed","indexed",390758624),new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(resp),new cljs.core.Keyword(null,"rel-path","rel-path",593215642),rel_path], null);
})).catch((function (err){
console.warn("[knoxx] failed to index document into OpenPlanner:",rel_path,err);

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"rel-path","rel-path",593215642),rel_path], null);
}));
});
/**
 * Ingest multiple documents into OpenPlanner with concurrency control.
 * Returns {:ok true, :indexed [...], :failed [...]} summary.
 */
knoxx.backend.openplanner_memory.batch_upsert_openplanner_documents_BANG_ = (function knoxx$backend$openplanner_memory$batch_upsert_openplanner_documents_BANG_(config,documents,p__50809){
var map__50810 = p__50809;
var map__50810__$1 = cljs.core.__destructure_map(map__50810);
var concurrency = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__50810__$1,new cljs.core.Keyword(null,"concurrency","concurrency",595096228),(3));
var project = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50810__$1,new cljs.core.Keyword(null,"project","project",1124394579));
var visibility = cljs.core.get.cljs$core$IFn$_invoke$arity$3(map__50810__$1,new cljs.core.Keyword(null,"visibility","visibility",1338380893),"internal");
var extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50810__$1,new cljs.core.Keyword(null,"extra","extra",1612569067));
if(cljs.core.empty_QMARK_(documents)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"total","total",1916810418),(0),new cljs.core.Keyword(null,"indexed-count","indexed-count",822648260),(0),new cljs.core.Keyword(null,"failed-count","failed-count",-366647954),(0)], null));
} else {
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"total","total",1916810418),cljs.core.count(documents),new cljs.core.Keyword(null,"indexed-count","indexed-count",822648260),(0),new cljs.core.Keyword(null,"failed-count","failed-count",-366647954),cljs.core.count(documents),new cljs.core.Keyword(null,"error","error",-978969032),"OpenPlanner is not configured"], null));
} else {
var chunks = cljs.core.vec(cljs.core.partition_all.cljs$core$IFn$_invoke$arity$2(concurrency,documents));
var results = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.PersistentVector.EMPTY], null));
var process_chunk_BANG_ = (function (chunk){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (doc){
return knoxx.backend.openplanner_memory.upsert_openplanner_document_BANG_(config,cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"project","project",1124394579),project,new cljs.core.Keyword(null,"visibility","visibility",1338380893),visibility,new cljs.core.Keyword(null,"extra","extra",1612569067),extra], null),doc], 0)));
}),chunk))).then((function (chunk_results){
var seq__50855_51346 = cljs.core.seq(knoxx.backend.openplanner_memory.js_array_seq(chunk_results));
var chunk__50856_51347 = null;
var count__50857_51348 = (0);
var i__50858_51349 = (0);
while(true){
if((i__50858_51349 < count__50857_51348)){
var result_51350 = chunk__50856_51347.cljs$core$IIndexed$_nth$arity$2(null,i__50858_51349);
if(cljs.core.truth_(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(result_51350))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.update,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.conj,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([result_51350], 0));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.update,new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.conj,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([result_51350], 0));
}


var G__51351 = seq__50855_51346;
var G__51352 = chunk__50856_51347;
var G__51353 = count__50857_51348;
var G__51354 = (i__50858_51349 + (1));
seq__50855_51346 = G__51351;
chunk__50856_51347 = G__51352;
count__50857_51348 = G__51353;
i__50858_51349 = G__51354;
continue;
} else {
var temp__5825__auto___51355 = cljs.core.seq(seq__50855_51346);
if(temp__5825__auto___51355){
var seq__50855_51356__$1 = temp__5825__auto___51355;
if(cljs.core.chunked_seq_QMARK_(seq__50855_51356__$1)){
var c__5673__auto___51357 = cljs.core.chunk_first(seq__50855_51356__$1);
var G__51359 = cljs.core.chunk_rest(seq__50855_51356__$1);
var G__51360 = c__5673__auto___51357;
var G__51361 = cljs.core.count(c__5673__auto___51357);
var G__51362 = (0);
seq__50855_51346 = G__51359;
chunk__50856_51347 = G__51360;
count__50857_51348 = G__51361;
i__50858_51349 = G__51362;
continue;
} else {
var result_51363 = cljs.core.first(seq__50855_51356__$1);
if(cljs.core.truth_(new cljs.core.Keyword(null,"ok","ok",967785236).cljs$core$IFn$_invoke$arity$1(result_51363))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.update,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.conj,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([result_51363], 0));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.update,new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.conj,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([result_51363], 0));
}


var G__51369 = cljs.core.next(seq__50855_51356__$1);
var G__51370 = null;
var G__51371 = (0);
var G__51372 = (0);
seq__50855_51346 = G__51369;
chunk__50856_51347 = G__51370;
count__50857_51348 = G__51371;
i__50858_51349 = G__51372;
continue;
}
} else {
}
}
break;
}

return null;
}));
});
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (promise,chunk){
return promise.then((function (){
return process_chunk_BANG_(chunk);
}));
}),Promise.resolve(null),chunks).then((function (){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"indexed","indexed",390758624),new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(results)),new cljs.core.Keyword(null,"failed","failed",-1397425762),new cljs.core.Keyword(null,"failed","failed",-1397425762).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(results)),new cljs.core.Keyword(null,"total","total",1916810418),cljs.core.count(documents),new cljs.core.Keyword(null,"indexed-count","indexed-count",822648260),cljs.core.count(new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(results))),new cljs.core.Keyword(null,"failed-count","failed-count",-366647954),cljs.core.count(new cljs.core.Keyword(null,"failed","failed",-1397425762).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(results)))], null);
}));

}
}
});
knoxx.backend.openplanner_memory.planner_row_timestamp_ms = (function knoxx$backend$openplanner_memory$planner_row_timestamp_ms(row){
var ts = new cljs.core.Keyword(null,"ts","ts",1617209904).cljs$core$IFn$_invoke$arity$1(row);
if(typeof ts === 'number'){
return ts;
} else {
if(typeof ts === 'string'){
var parsed = Date.parse(ts);
if(cljs.core.truth_(isNaN(parsed))){
return Date.now();
} else {
return parsed;
}
} else {
return Date.now();

}
}
});
knoxx.backend.openplanner_memory.planner_row__GT_agent_message = (function knoxx$backend$openplanner_memory$planner_row__GT_agent_message(row){
var role = (function (){var G__50916 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(row);
if((G__50916 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50916));
}
})();
var text = (function (){var G__50921 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(row);
if((G__50921 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50921));
}
})();
var parts = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(row,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(row,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667)], null));
}
})();
if(((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["user",null,"assistant",null,"system",null], null), null),role)) && ((!(clojure.string.blank_QMARK_(text)))))){
var text_block = ({"type": "text", "text": text});
var content_arr = ((cljs.core.seq(parts))?cljs.core.clj__GT_js(cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [text_block], null),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("image",new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(p))){
return ({"type": "image_url", "image_url": ({"url": (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"data:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(p))+";base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(p)));
}
})()})});
} else {
return ({"type": "text", "text": (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()});
}
}),parts))):[text_block]);
return ({"role": role, "content": content_arr, "timestamp": knoxx.backend.openplanner_memory.planner_row_timestamp_ms(row)});
} else {
return null;
}
});
knoxx.backend.openplanner_memory.rehydrate_session_manager_BANG_ = (function knoxx$backend$openplanner_memory$rehydrate_session_manager_BANG_(config,session_manager,conversation_id,_model_id){
if(((clojure.string.blank_QMARK_(conversation_id)) || ((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))))){
return Promise.resolve(session_manager);
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id))).then((function (body){
var seq__50947_51386 = cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var chunk__50948_51387 = null;
var count__50949_51388 = (0);
var i__50950_51389 = (0);
while(true){
if((i__50950_51389 < count__50949_51388)){
var row_51390 = chunk__50948_51387.cljs$core$IIndexed$_nth$arity$2(null,i__50950_51389);
var temp__5825__auto___51391 = knoxx.backend.openplanner_memory.planner_row__GT_agent_message(row_51390);
if(cljs.core.truth_(temp__5825__auto___51391)){
var message_51392 = temp__5825__auto___51391;
session_manager.appendMessage(message_51392);
} else {
}


var G__51393 = seq__50947_51386;
var G__51394 = chunk__50948_51387;
var G__51395 = count__50949_51388;
var G__51396 = (i__50950_51389 + (1));
seq__50947_51386 = G__51393;
chunk__50948_51387 = G__51394;
count__50949_51388 = G__51395;
i__50950_51389 = G__51396;
continue;
} else {
var temp__5825__auto___51397 = cljs.core.seq(seq__50947_51386);
if(temp__5825__auto___51397){
var seq__50947_51398__$1 = temp__5825__auto___51397;
if(cljs.core.chunked_seq_QMARK_(seq__50947_51398__$1)){
var c__5673__auto___51399 = cljs.core.chunk_first(seq__50947_51398__$1);
var G__51400 = cljs.core.chunk_rest(seq__50947_51398__$1);
var G__51401 = c__5673__auto___51399;
var G__51402 = cljs.core.count(c__5673__auto___51399);
var G__51403 = (0);
seq__50947_51386 = G__51400;
chunk__50948_51387 = G__51401;
count__50949_51388 = G__51402;
i__50950_51389 = G__51403;
continue;
} else {
var row_51404 = cljs.core.first(seq__50947_51398__$1);
var temp__5825__auto___51405__$1 = knoxx.backend.openplanner_memory.planner_row__GT_agent_message(row_51404);
if(cljs.core.truth_(temp__5825__auto___51405__$1)){
var message_51406 = temp__5825__auto___51405__$1;
session_manager.appendMessage(message_51406);
} else {
}


var G__51407 = cljs.core.next(seq__50947_51398__$1);
var G__51408 = null;
var G__51409 = (0);
var G__51410 = (0);
seq__50947_51386 = G__51407;
chunk__50948_51387 = G__51408;
count__50949_51388 = G__51409;
i__50950_51389 = G__51410;
continue;
}
} else {
}
}
break;
}

return session_manager;
})).catch((function (err){
console.warn("[knoxx] failed to rehydrate session from OpenPlanner",err);

return session_manager;
}));
}
});
knoxx.backend.openplanner_memory.first_result_array = (function knoxx$backend$openplanner_memory$first_result_array(value){
var items = (function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var first_item = cljs.core.first(items);
if(cljs.core.sequential_QMARK_(first_item)){
return cljs.core.vec(first_item);
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.openplanner_memory.vector_result_hits = (function knoxx$backend$openplanner_memory$vector_result_hits(result){
var ids = knoxx.backend.openplanner_memory.first_result_array(new cljs.core.Keyword(null,"ids","ids",-998535796).cljs$core$IFn$_invoke$arity$1(result));
var docs = knoxx.backend.openplanner_memory.first_result_array(new cljs.core.Keyword(null,"documents","documents",-1582333455).cljs$core$IFn$_invoke$arity$1(result));
var metas = knoxx.backend.openplanner_memory.first_result_array(new cljs.core.Keyword(null,"metadatas","metadatas",-1319371457).cljs$core$IFn$_invoke$arity$1(result));
var distances = knoxx.backend.openplanner_memory.first_result_array(new cljs.core.Keyword(null,"distances","distances",-1026444268).cljs$core$IFn$_invoke$arity$1(result));
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$3((function (idx,id){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"document","document",-1329188687),cljs.core.nth.cljs$core$IFn$_invoke$arity$3(docs,idx,""),new cljs.core.Keyword(null,"metadata","metadata",1799301597),cljs.core.nth.cljs$core$IFn$_invoke$arity$3(metas,idx,cljs.core.PersistentArrayMap.EMPTY),new cljs.core.Keyword(null,"distance","distance",-1671893894),cljs.core.nth.cljs$core$IFn$_invoke$arity$3(distances,idx,null)], null);
}),cljs.core.range.cljs$core$IFn$_invoke$arity$1(cljs.core.count(ids)),ids);
});
knoxx.backend.openplanner_memory.hit_metadata = (function knoxx$backend$openplanner_memory$hit_metadata(hit){
var or__5142__auto__ = new cljs.core.Keyword(null,"metadata","metadata",1799301597).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = hit;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
});
knoxx.backend.openplanner_memory.hit_text = (function knoxx$backend$openplanner_memory$hit_text(hit){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(hit);
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
})()));
});
knoxx.backend.openplanner_memory.reasoning_memory_hit_QMARK_ = (function knoxx$backend$openplanner_memory$reasoning_memory_hit_QMARK_(hit){
var metadata = knoxx.backend.openplanner_memory.hit_metadata(hit);
var kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var role = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"parent_id","parent_id",-1999171020).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})()));
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"knoxx.reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"node_type","node_type",-1629889898).cljs$core$IFn$_invoke$arity$1(metadata),"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"node-type","node-type",129492462).cljs$core$IFn$_invoke$arity$1(metadata),"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"reasoning")) || (clojure.string.includes_QMARK_(id,":reasoning")))))))))));
});
knoxx.backend.openplanner_memory.operational_failure_memory_hit_QMARK_ = (function knoxx$backend$openplanner_memory$operational_failure_memory_hit_QMARK_(hit){
var text = knoxx.backend.openplanner_memory.hit_text(hit);
return cljs.core.boolean$((function (){var or__5142__auto__ = cljs.core.re_find(/\b403\s+No upstream providers are allowed\b/i,text);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.re_find(/\bNo upstream providers are allowed for this tenant and request\b/i,text);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.re_find(/\bprovider_not_allowed\b/i,text);
}
}
})());
});
knoxx.backend.openplanner_memory.default_memory_hit_QMARK_ = (function knoxx$backend$openplanner_memory$default_memory_hit_QMARK_(hit){
return (((!(knoxx.backend.openplanner_memory.reasoning_memory_hit_QMARK_(hit)))) && ((((!(knoxx.backend.openplanner_memory.operational_failure_memory_hit_QMARK_(hit)))) && (knoxx.backend.quality_labels.not_bad_QMARK_(hit)))));
});
knoxx.backend.openplanner_memory.default_memory_hits = (function knoxx$backend$openplanner_memory$default_memory_hits(hits,limit){
return cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(limit,knoxx.backend.quality_labels.good_first_then_not_bad(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.openplanner_memory.default_memory_hit_QMARK_,(function (){var or__5142__auto__ = hits;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.openplanner_memory.openplanner_recent_session_summaries_BANG_ = (function knoxx$backend$openplanner_memory$openplanner_recent_session_summaries_BANG_(config){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config))))).then((function (body){
var session_ids = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2((4),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session","session",1008279103),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))));
if(cljs.core.seq(session_ids)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (session_id){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id))).then((function (session_body){
var rows = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(session_body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var row = (function (){var or__5142__auto__ = cljs.core.last(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__50994_SHARP_){
return ((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["assistant",null,"system",null], null), null),new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(p1__50994_SHARP_))) && (knoxx.backend.openplanner_memory.default_memory_hit_QMARK_(p1__50994_SHARP_)));
}),rows));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.last(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.openplanner_memory.default_memory_hit_QMARK_,rows));
}
})();
if(cljs.core.truth_(row)){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(row),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(row)], null);
} else {
return null;
}
})).catch((function (_){
return null;
}));
}),session_ids))).then((function (results){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))));
}));
} else {
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}
})).catch((function (_){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}));
});
knoxx.backend.openplanner_memory.openplanner_memory_search_BANG_ = (function knoxx$backend$openplanner_memory$openplanner_memory_search_BANG_(config,p__51007){
var map__51008 = p__51007;
var map__51008__$1 = cljs.core.__destructure_map(map__51008);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51008__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var k = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51008__$1,new cljs.core.Keyword(null,"k","k",-2146297393));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51008__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var query__$1 = clojure.string.trim((function (){var or__5142__auto__ = query;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var k__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((12),(function (){var or__5142__auto__ = k;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (7);
}
})()));
var fetch_k = cljs.core.max.cljs$core$IFn$_invoke$arity$2(k__$1,cljs.core.min.cljs$core$IFn$_invoke$arity$2((36),(k__$1 * (3))));
if(clojure.string.blank_QMARK_(query__$1)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),"",new cljs.core.Keyword(null,"hits","hits",-2120002930),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"none","none",1333468478)], null));
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/search/vector",(function (){var G__51013 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"q","q",689001697),query__$1,new cljs.core.Keyword(null,"k","k",-2146297393),fetch_k,new cljs.core.Keyword(null,"source","source",-433931539),"knoxx",new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config)], null);
if((!(clojure.string.blank_QMARK_(session_id)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51013,new cljs.core.Keyword(null,"session","session",1008279103),session_id);
} else {
return G__51013;
}
})()).then((function (body){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query__$1,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"hits","hits",-2120002930),knoxx.backend.openplanner_memory.default_memory_hits(knoxx.backend.openplanner_memory.vector_result_hits(new cljs.core.Keyword(null,"result","result",1415092211).cljs$core$IFn$_invoke$arity$1(body)),k__$1)], null);
})).catch((function (_){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/search/fts",(function (){var G__51014 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"q","q",689001697),query__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363),fetch_k,new cljs.core.Keyword(null,"source","source",-433931539),"knoxx",new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config)], null);
if((!(clojure.string.blank_QMARK_(session_id)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51014,new cljs.core.Keyword(null,"session","session",1008279103),session_id);
} else {
return G__51014;
}
})()).then((function (body){
var hits = knoxx.backend.openplanner_memory.default_memory_hits((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),k__$1);
if(cljs.core.seq(hits)){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query__$1,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"fts","fts",-1557144410),new cljs.core.Keyword(null,"hits","hits",-2120002930),hits], null);
} else {
return knoxx.backend.openplanner_memory.openplanner_recent_session_summaries_BANG_(config).then((function (recent){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query__$1,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"recent","recent",449517715),new cljs.core.Keyword(null,"hits","hits",-2120002930),knoxx.backend.openplanner_memory.default_memory_hits(recent,k__$1)], null);
}));
}
}));
}));
}
});
knoxx.backend.openplanner_memory.openplanner_graph_query_BANG_ = (function knoxx$backend$openplanner_memory$openplanner_graph_query_BANG_(config,p__51016){
var map__51017 = p__51016;
var map__51017__$1 = cljs.core.__destructure_map(map__51017);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51017__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var lake = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51017__$1,new cljs.core.Keyword(null,"lake","lake",805586599));
var node_type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51017__$1,new cljs.core.Keyword(null,"node-type","node-type",129492462));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51017__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var edge_limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51017__$1,new cljs.core.Keyword(null,"edge-limit","edge-limit",4816756));
var k = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((60),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (15);
}
})()));
var node_types = ((clojure.string.blank_QMARK_((function (){var or__5142__auto__ = node_type;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))?null:JSON.parse(JSON.stringify(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(node_type,/,/)))));
var lakes = ((clojure.string.blank_QMARK_((function (){var or__5142__auto__ = lake;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))?null:JSON.parse(JSON.stringify(cljs.core.clj__GT_js(clojure.string.split.cljs$core$IFn$_invoke$arity$2(lake,/,/)))));
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/graph/memory",(function (){var G__51018 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"q","q",689001697),(function (){var or__5142__auto__ = query;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"k","k",-2146297393),k,new cljs.core.Keyword(null,"includeText","includeText",-1104106275),true], null);
var G__51018__$1 = (cljs.core.truth_(node_types)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51018,new cljs.core.Keyword(null,"nodeTypes","nodeTypes",1617837637),node_types):G__51018);
var G__51018__$2 = (cljs.core.truth_(lakes)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51018__$1,new cljs.core.Keyword(null,"lakes","lakes",-1032595818),lakes):G__51018__$1);
if(cljs.core.truth_(edge_limit)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51018__$2,new cljs.core.Keyword(null,"maxCost","maxCost",-1281687611),(1.0 / cljs.core.max.cljs$core$IFn$_invoke$arity$2(0.01,(function (){var or__5142__auto__ = edge_limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (16);
}
})())));
} else {
return G__51018__$2;
}
})());
});
/**
 * Search OpenPlanner's indexed document corpus via vector similarity.
 * Returns {:query, :mode, :hits} where each hit has :id, :document, :metadata, :distance.
 * Falls back to FTS if vector search fails.
 */
knoxx.backend.openplanner_memory.openplanner_semantic_search_BANG_ = (function knoxx$backend$openplanner_memory$openplanner_semantic_search_BANG_(config,p__51024){
var map__51025 = p__51024;
var map__51025__$1 = cljs.core.__destructure_map(map__51025);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var k = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"k","k",-2146297393));
var project = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"project","project",1124394579));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"source","source",-433931539));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
var visibility = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51025__$1,new cljs.core.Keyword(null,"visibility","visibility",1338380893));
var query__$1 = clojure.string.trim((function (){var or__5142__auto__ = query;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var k__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((20),(function (){var or__5142__auto__ = k;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (10);
}
})()));
if(clojure.string.blank_QMARK_(query__$1)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),"",new cljs.core.Keyword(null,"hits","hits",-2120002930),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"none","none",1333468478)], null));
} else {
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/search/vector",(function (){var G__51034 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"q","q",689001697),query__$1,new cljs.core.Keyword(null,"k","k",-2146297393),k__$1,new cljs.core.Keyword(null,"project","project",1124394579),(function (){var or__5142__auto__ = project;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "devel";
}
}
})()], null);
var G__51034__$1 = (cljs.core.truth_(source)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51034,new cljs.core.Keyword(null,"source","source",-433931539),source):G__51034);
var G__51034__$2 = (cljs.core.truth_(kind)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51034__$1,new cljs.core.Keyword(null,"kind","kind",-717265803),kind):G__51034__$1);
if(cljs.core.truth_(visibility)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51034__$2,new cljs.core.Keyword(null,"visibility","visibility",1338380893),visibility);
} else {
return G__51034__$2;
}
})()).then((function (body){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query__$1,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.Keyword(null,"hits","hits",-2120002930),knoxx.backend.openplanner_memory.vector_result_hits(new cljs.core.Keyword(null,"result","result",1415092211).cljs$core$IFn$_invoke$arity$1(body))], null);
})).catch((function (_){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/search/fts",(function (){var G__51035 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"q","q",689001697),query__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363),k__$1,new cljs.core.Keyword(null,"project","project",1124394579),(function (){var or__5142__auto__ = project;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "devel";
}
}
})()], null);
if(cljs.core.truth_(source)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51035,new cljs.core.Keyword(null,"source","source",-433931539),source);
} else {
return G__51035;
}
})()).then((function (body){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query__$1,new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"fts","fts",-1557144410),new cljs.core.Keyword(null,"hits","hits",-2120002930),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null);
}));
}));
}
});
knoxx.backend.openplanner_memory.openplanner_graph_export_BANG_ = (function knoxx$backend$openplanner_memory$openplanner_graph_export_BANG_(config,request){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/graph/export"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.request_query_string(request))));
});
knoxx.backend.openplanner_memory.operational_failure_text_QMARK_ = (function knoxx$backend$openplanner_memory$operational_failure_text_QMARK_(text){
var text__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text));
return cljs.core.boolean$((function (){var or__5142__auto__ = cljs.core.re_find(/\b403\s+No upstream providers are allowed\b/i,text__$1);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.re_find(/\bNo upstream providers are allowed for this tenant and request\b/i,text__$1);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.re_find(/\bprovider_not_allowed\b/i,text__$1);
}
}
})());
});
knoxx.backend.openplanner_memory.quality_label_extra = (function knoxx$backend$openplanner_memory$quality_label_extra(quality,explicit_meaning){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"openplanner_labels","openplanner_labels",-669573727),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"claim_system","claim_system",-320128383),"knoxx-auto-quality-v1",new cljs.core.Keyword(null,"quality","quality",147850199),quality,new cljs.core.Keyword(null,"explicit_meaning","explicit_meaning",1062627523),explicit_meaning,new cljs.core.Keyword(null,"labels","labels",-626734591),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"quality:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(quality))], null),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592),knoxx.backend.util.time.now_iso()], null)], null);
});
knoxx.backend.openplanner_memory.output_quality_extra = (function knoxx$backend$openplanner_memory$output_quality_extra(text){
if(knoxx.backend.openplanner_memory.operational_failure_text_QMARK_(text)){
return knoxx.backend.openplanner_memory.quality_label_extra("bad","operational provider error, not useful assistant output");
} else {
return null;
}
});
knoxx.backend.openplanner_memory.openplanner_event = (function knoxx$backend$openplanner_memory$openplanner_event(config,p__51041){
var map__51042 = p__51041;
var map__51042__$1 = cljs.core.__destructure_map(map__51042);
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var session = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"session","session",1008279103));
var text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"model","model",331153215));
var role = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"role","role",-736691072));
var extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"extra","extra",1612569067));
var ts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"ts","ts",1617209904));
var project = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"project","project",1124394579));
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51042__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"schema","schema",-1582001791),new cljs.core.Keyword(null,"meta","meta",1499536964),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"source_ref","source_ref",-1854699662),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"text","text",-1790561697)],["openplanner.event.v1",new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"role","role",-736691072),role,new cljs.core.Keyword(null,"author","author",2111686192),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"user"))?"user":"knoxx"),new cljs.core.Keyword(null,"model","model",331153215),model,new cljs.core.Keyword(null,"tags","tags",1771418977),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["knoxx",kind,role], null)], null),extra,"knoxx",(function (){var or__5142__auto__ = ts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.time.now_iso();
}
})(),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"project","project",1124394579),(function (){var or__5142__auto__ = project;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
}
})(),new cljs.core.Keyword(null,"session","session",1008279103),session,new cljs.core.Keyword(null,"message","message",-406056002),message], null),id,kind,text]);
});
knoxx.backend.openplanner_memory.tool_receipt_summary_text = (function knoxx$backend$openplanner_memory$tool_receipt_summary_text(receipt){
return (""+"Tool: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "tool";
}
}
})())+"\nStatus: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"input_preview","input_preview",2048529734).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(temp__5825__auto__)){
var input = temp__5825__auto__;
return (""+"\nInput:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(input));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"result_preview","result_preview",215554859).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(temp__5825__auto__)){
var result = temp__5825__auto__;
return (""+"\nOutput:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(result));
} else {
return null;
}
})()));
});
knoxx.backend.openplanner_memory.sanitize_tool_receipt_for_indexing = (function knoxx$backend$openplanner_memory$sanitize_tool_receipt_for_indexing(receipt){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(receipt,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667)),new cljs.core.Keyword(null,"contentParts","contentParts",1395809695)),new cljs.core.Keyword(null,"attachments","attachments",-1535547830));
});
knoxx.backend.openplanner_memory.run_summary_text = (function knoxx$backend$openplanner_memory$run_summary_text(run){
return (""+"Run "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(run))+"\nMode: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(run,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"mode","mode",654403691)], null)))+"\nModel: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run))+"\nStatus: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"answer","answer",-742633163).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(temp__5825__auto__)){
var answer = temp__5825__auto__;
return (""+"\nAnswer:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(answer));
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(temp__5825__auto__)){
var error = temp__5825__auto__;
return (""+"\nError:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error));
} else {
return null;
}
})()));
});
knoxx.backend.openplanner_memory.run_scope_extra = (function knoxx$backend$openplanner_memory$run_scope_extra(run){
var base = cljs.core.select_keys(run,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770),new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"user_email","user_email",-926613652),new cljs.core.Keyword(null,"membership_id","membership_id",-171302674)], null));
var agent_spec = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(run,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365).cljs$core$IFn$_invoke$arity$1(run);
}
})();
var contract_id = (function (){var G__51049 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractId","contractId",710260199).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51049__$1 = (((G__51049 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51049)));
var G__51049__$2 = (((G__51049__$1 == null))?null:clojure.string.trim(G__51049__$1));
if((G__51049__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51049__$2);
}
})();
var actor_id = (function (){var G__51050 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51050__$1 = (((G__51050 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51050)));
var G__51050__$2 = (((G__51050__$1 == null))?null:clojure.string.trim(G__51050__$1));
if((G__51050__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51050__$2);
}
})();
var contract_actors = (function (){var G__51051 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractActors","contractActors",47284059).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
return (knoxx.backend.runtime.actor_scope.actor_claims__GT_wire.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.actor_scope.actor_claims__GT_wire.cljs$core$IFn$_invoke$arity$1(G__51051) : knoxx.backend.runtime.actor_scope.actor_claims__GT_wire.call(null,G__51051));
})();
var sub_agent_id = (function (){var G__51052 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"subAgentId","subAgentId",538139792).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"sub-agent-id","sub-agent-id",1379404479).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51052__$1 = (((G__51052 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51052)));
var G__51052__$2 = (((G__51052__$1 == null))?null:clojure.string.trim(G__51052__$1));
if((G__51052__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51052__$2);
}
})();
var parent_agent_id = (function (){var G__51053 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"parentAgentId","parentAgentId",1686278200).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"parent-agent-id","parent-agent-id",1884761925).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51053__$1 = (((G__51053 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51053)));
var G__51053__$2 = (((G__51053__$1 == null))?null:clojure.string.trim(G__51053__$1));
if((G__51053__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51053__$2);
}
})();
var parent_run_id = (function (){var G__51054 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"parentRunId","parentRunId",938716271).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"parent-run-id","parent-run-id",662820367).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51054__$1 = (((G__51054 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51054)));
var G__51054__$2 = (((G__51054__$1 == null))?null:clojure.string.trim(G__51054__$1));
if((G__51054__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51054__$2);
}
})();
var spawn_kind = (function (){var G__51055 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"spawnKind","spawnKind",1648184297).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"spawn-kind","spawn-kind",-1330963959).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var G__51055__$1 = (((G__51055 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__51055)));
var G__51055__$2 = (((G__51055__$1 == null))?null:clojure.string.trim(G__51055__$1));
if((G__51055__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__51055__$2);
}
})();
var G__51056 = base;
var G__51056__$1 = (cljs.core.truth_(contract_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056,new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),contract_id):G__51056);
var G__51056__$2 = (cljs.core.truth_(actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$1,new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),actor_id):G__51056__$1);
var G__51056__$3 = ((cljs.core.seq(contract_actors))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$2,new cljs.core.Keyword(null,"contract_actors","contract_actors",-1493360705),contract_actors):G__51056__$2);
var G__51056__$4 = (cljs.core.truth_(sub_agent_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$3,new cljs.core.Keyword(null,"sub_agent_id","sub_agent_id",320149773),sub_agent_id):G__51056__$3);
var G__51056__$5 = (cljs.core.truth_(parent_agent_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$4,new cljs.core.Keyword(null,"parent_agent_id","parent_agent_id",-252488900),parent_agent_id):G__51056__$4);
var G__51056__$6 = (cljs.core.truth_(parent_run_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$5,new cljs.core.Keyword(null,"parent_run_id","parent_run_id",-2071531014),parent_run_id):G__51056__$5);
if(cljs.core.truth_(spawn_kind)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__51056__$6,new cljs.core.Keyword(null,"spawn_kind","spawn_kind",1611229473),spawn_kind);
} else {
return G__51056__$6;
}
});
knoxx.backend.openplanner_memory.session_node_kind = (function knoxx$backend$openplanner_memory$session_node_kind(node_type){
var G__51057 = node_type;
switch (G__51057) {
case "tool_call":
return "tool_call";

break;
case "tool_result":
return "tool_result";

break;
case "reasoning":
return "reasoning";

break;
default:
return "message";

}
});
knoxx.backend.openplanner_memory.session_graph_node_event = (function knoxx$backend$openplanner_memory$session_graph_node_event(config,p__51058){
var map__51059 = p__51058;
var map__51059__$1 = cljs.core.__destructure_map(map__51059);
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var session = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"session","session",1008279103));
var text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"model","model",331153215));
var event_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"event-id","event-id",2130210178));
var extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"extra","extra",1612569067));
var node_type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"node-type","node-type",129492462));
var ts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"ts","ts",1617209904));
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51059__$1,new cljs.core.Keyword(null,"node-id","node-id",779482292));
return knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"lake","lake",805586599),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"node_id","node_id",-1781409174),node_id,new cljs.core.Keyword(null,"node_type","node_type",-1629889898),node_type,new cljs.core.Keyword(null,"node_kind","node_kind",-1510972914),knoxx.backend.openplanner_memory.session_node_kind(node_type),new cljs.core.Keyword(null,"label","label",1718410804),label,new cljs.core.Keyword(null,"entity_key","entity_key",-535942535),node_id], null),extra], 0)),ts,new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),event_id,"graph.node",message,session,text,model]));
});
knoxx.backend.openplanner_memory.session_graph_edge_event = (function knoxx$backend$openplanner_memory$session_graph_edge_event(config,p__51061){
var map__51062 = p__51061;
var map__51062__$1 = cljs.core.__destructure_map(map__51062);
var message = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var session = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"session","session",1008279103));
var event_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"event-id","event-id",2130210178));
var target_node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"target-node-id","target-node-id",1474740067));
var edge_type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"edge-type","edge-type",1113487045));
var target_lake = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"target-lake","target-lake",-1039226));
var extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"extra","extra",1612569067));
var source_lake = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"source-lake","source-lake",2133547533));
var source_node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"source-node-id","source-node-id",823076653));
var ts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51062__$1,new cljs.core.Keyword(null,"ts","ts",1617209904));
return knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"lake","lake",805586599),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"edge_id","edge_id",629853454),event_id,new cljs.core.Keyword(null,"edge_type","edge_type",1140519228),edge_type,new cljs.core.Keyword(null,"source_node_id","source_node_id",-525446610),source_node_id,new cljs.core.Keyword(null,"target_node_id","target_node_id",-988690835),target_node_id,new cljs.core.Keyword(null,"source_lake","source_lake",-657789362),source_lake,new cljs.core.Keyword(null,"target_lake","target_lake",656087470),target_lake], null),extra], 0)),ts,new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),event_id,"graph.edge",message,session,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(source_node_id)+" -> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_node_id))]));
});
knoxx.backend.openplanner_memory.session_text_graph_events = (function knoxx$backend$openplanner_memory$session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,p__51063){
var map__51064 = p__51063;
var map__51064__$1 = cljs.core.__destructure_map(map__51064);
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"model","model",331153215));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var scope_extra = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052));
var node_type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"node-type","node-type",129492462));
var ts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"ts","ts",1617209904));
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51064__$1,new cljs.core.Keyword(null,"node-id","node-id",779482292));
var safe_text = (function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var node_event = knoxx.backend.openplanner_memory.session_graph_node_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_id)+":node"),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null),scope_extra,knoxx.backend.openplanner_memory.output_quality_extra(safe_text)], 0)),node_type,ts,node_id,label,node_id,conversation_id,safe_text,model]));
var devel_edges = (function (){var iter__5628__auto__ = (function knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51065(s__51066){
return (new cljs.core.LazySeq(null,(function (){
var s__51066__$1 = s__51066;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__51066__$1);
if(temp__5825__auto__){
var s__51066__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__51066__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__51066__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__51068 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__51067 = (0);
while(true){
if((i__51067 < size__5627__auto__)){
var map__51069 = cljs.core._nth(c__5626__auto__,i__51067);
var map__51069__$1 = cljs.core.__destructure_map(map__51069);
var path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51069__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var target_node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51069__$1,new cljs.core.Keyword(null,"target_node_id","target_node_id",-988690835));
var target_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51069__$1,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164));
cljs.core.chunk_append(b__51068,knoxx.backend.openplanner_memory.session_graph_edge_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword(null,"target-node-id","target-node-id",1474740067),new cljs.core.Keyword(null,"edge-type","edge-type",1113487045),new cljs.core.Keyword(null,"target-lake","target-lake",-1039226),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source-lake","source-lake",2133547533),new cljs.core.Keyword(null,"source-node-id","source-node-id",823076653),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_id)+":mentions_devel:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_node_id)),target_node_id,"mentions_devel_path","devel",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"path","path",-188191168),path,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164),target_kind], null),scope_extra], 0)),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),node_id,ts,node_id,conversation_id])));

var G__51547 = (i__51067 + (1));
i__51067 = G__51547;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__51068),knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51065(cljs.core.chunk_rest(s__51066__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__51068),null);
}
} else {
var map__51070 = cljs.core.first(s__51066__$2);
var map__51070__$1 = cljs.core.__destructure_map(map__51070);
var path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51070__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var target_node_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51070__$1,new cljs.core.Keyword(null,"target_node_id","target_node_id",-988690835));
var target_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51070__$1,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164));
return cljs.core.cons(knoxx.backend.openplanner_memory.session_graph_edge_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword(null,"target-node-id","target-node-id",1474740067),new cljs.core.Keyword(null,"edge-type","edge-type",1113487045),new cljs.core.Keyword(null,"target-lake","target-lake",-1039226),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source-lake","source-lake",2133547533),new cljs.core.Keyword(null,"source-node-id","source-node-id",823076653),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_id)+":mentions_devel:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_node_id)),target_node_id,"mentions_devel_path","devel",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"path","path",-188191168),path,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164),target_kind], null),scope_extra], 0)),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),node_id,ts,node_id,conversation_id])),knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51065(cljs.core.rest(s__51066__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__((extract_mentioned_devel_paths.cljs$core$IFn$_invoke$arity$1 ? extract_mentioned_devel_paths.cljs$core$IFn$_invoke$arity$1(safe_text) : extract_mentioned_devel_paths.call(null,safe_text)));
})();
var web_edges = (function (){var iter__5628__auto__ = (function knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51071(s__51072){
return (new cljs.core.LazySeq(null,(function (){
var s__51072__$1 = s__51072;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__51072__$1);
if(temp__5825__auto__){
var s__51072__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__51072__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__51072__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__51074 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__51073 = (0);
while(true){
if((i__51073 < size__5627__auto__)){
var url = cljs.core._nth(c__5626__auto__,i__51073);
cljs.core.chunk_append(b__51074,knoxx.backend.openplanner_memory.session_graph_edge_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword(null,"target-node-id","target-node-id",1474740067),new cljs.core.Keyword(null,"edge-type","edge-type",1113487045),new cljs.core.Keyword(null,"target-lake","target-lake",-1039226),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source-lake","source-lake",2133547533),new cljs.core.Keyword(null,"source-node-id","source-node-id",823076653),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_id)+":mentions_web:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),(""+"web:url:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),"mentions_web_url","web",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"url","url",276297046),url], null),scope_extra], 0)),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),node_id,ts,node_id,conversation_id])));

var G__51549 = (i__51073 + (1));
i__51073 = G__51549;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__51074),knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51071(cljs.core.chunk_rest(s__51072__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__51074),null);
}
} else {
var url = cljs.core.first(s__51072__$2);
return cljs.core.cons(knoxx.backend.openplanner_memory.session_graph_edge_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"event-id","event-id",2130210178),new cljs.core.Keyword(null,"target-node-id","target-node-id",1474740067),new cljs.core.Keyword(null,"edge-type","edge-type",1113487045),new cljs.core.Keyword(null,"target-lake","target-lake",-1039226),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source-lake","source-lake",2133547533),new cljs.core.Keyword(null,"source-node-id","source-node-id",823076653),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103)],[(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(node_id)+":mentions_web:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),(""+"web:url:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),"mentions_web_url","web",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"url","url",276297046),url], null),scope_extra], 0)),new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config),node_id,ts,node_id,conversation_id])),knoxx$backend$openplanner_memory$session_text_graph_events_$_iter__51071(cljs.core.rest(s__51072__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__((extract_mentioned_urls.cljs$core$IFn$_invoke$arity$1 ? extract_mentioned_urls.cljs$core$IFn$_invoke$arity$1(safe_text) : extract_mentioned_urls.call(null,safe_text)));
})();
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [node_event], null),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(devel_edges,web_edges));
});
knoxx.backend.openplanner_memory.index_run_memory_BANG_ = (function knoxx$backend$openplanner_memory$index_run_memory_BANG_(config,run,extract_mentioned_devel_paths,extract_mentioned_urls){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
return Promise.resolve(null);
} else {
var temp__5823__auto__ = cljs.core.deref(knoxx.backend.stores.session_store_registry.session_store_STAR_);
if(cljs.core.truth_(temp__5823__auto__)){
var store = temp__5823__auto__;
return store.complete_run_BANG_(new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"answer","answer",-742633163),new cljs.core.Keyword(null,"answer","answer",-742633163).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"messages","messages",345434482),new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(run)], null));
} else {
return (knoxx.backend.openplanner_memory.index_run_memory_legacy_BANG_.cljs$core$IFn$_invoke$arity$4 ? knoxx.backend.openplanner_memory.index_run_memory_legacy_BANG_.cljs$core$IFn$_invoke$arity$4(config,run,extract_mentioned_devel_paths,extract_mentioned_urls) : knoxx.backend.openplanner_memory.index_run_memory_legacy_BANG_.call(null,config,run,extract_mentioned_devel_paths,extract_mentioned_urls));
}
}
});
knoxx.backend.openplanner_memory.index_run_memory_legacy_BANG_ = (function knoxx$backend$openplanner_memory$index_run_memory_legacy_BANG_(config,run,extract_mentioned_devel_paths,extract_mentioned_urls){
var conversation_id = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(run);
var session_id = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(run);
var scope_extra = knoxx.backend.openplanner_memory.run_scope_extra(run);
var session_project = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
var request_text = (function (){var or__5142__auto__ = (function (){var G__51088 = new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565).cljs$core$IFn$_invoke$arity$1(run);
var G__51088__$1 = (((G__51088 == null))?null:cljs.core.first(G__51088));
if((G__51088__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(G__51088__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var answer = new cljs.core.Keyword(null,"answer","answer",-742633163).cljs$core$IFn$_invoke$arity$1(run);
var reasoning_text = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"reasoning","reasoning",1956143595).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var trace_blocks = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var run_id = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(run);
var common_extra = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"mode","mode",654403691),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(run,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"settings","settings",1556144875),new cljs.core.Keyword(null,"mode","mode",654403691)], null))], null),scope_extra], 0));
var content_parts = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p){
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("image",new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(p))) && (((clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(p))))))))){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),"image",new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(p),new cljs.core.Keyword(null,"data","data",-232669377),cljs.core.subs.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(p),(0),cljs.core.min.cljs$core$IFn$_invoke$arity$2((2048),cljs.core.count(new cljs.core.Keyword(null,"data","data",-232669377).cljs$core$IFn$_invoke$arity$1(p)))),new cljs.core.Keyword(null,"truncated","truncated",298102102),true], null);
} else {
return cljs.core.select_keys(p,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"url","url",276297046),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"text","text",-1790561697)], null));
}
}),(function (){var or__5142__auto__ = (function (){var G__51094 = new cljs.core.Keyword(null,"request_messages","request_messages",-1334174565).cljs$core$IFn$_invoke$arity$1(run);
var G__51094__$1 = (((G__51094 == null))?null:cljs.core.first(G__51094));
if((G__51094__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"content-parts","content-parts",684529019).cljs$core$IFn$_invoke$arity$1(G__51094__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var base_events = (function (){var G__51095 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["user",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),content_parts], null)], 0)),new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(run),session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":user"),"knoxx.message",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":user"),conversation_id,request_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])),knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832),new cljs.core.Keyword(null,"ttft_ms","ttft_ms",-630990832).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114),new cljs.core.Keyword(null,"total_time_ms","total_time_ms",390390114).cljs$core$IFn$_invoke$arity$1(run)], null),knoxx.backend.openplanner_memory.output_quality_extra(knoxx.backend.openplanner_memory.run_summary_text(run))], 0)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":summary"),"knoxx.run",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":summary"),conversation_id,knoxx.backend.openplanner_memory.run_summary_text(run),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]))], null);
var G__51095__$1 = (((!(clojure.string.blank_QMARK_((function (){var or__5142__auto__ = answer;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))))?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__51095,knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["assistant",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run),new cljs.core.Keyword(null,"trace_blocks","trace_blocks",1856523872),trace_blocks], null),knoxx.backend.openplanner_memory.output_quality_extra(answer)], 0)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":assistant"),"knoxx.message",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":assistant"),conversation_id,answer,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]))):G__51095);
if((!(clojure.string.blank_QMARK_(reasoning_text)))){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__51095__$1,knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(run)], null)], 0)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":reasoning"),"knoxx.reasoning",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":reasoning"),conversation_id,reasoning_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])));
} else {
return G__51095__$1;
}
})();
var tool_events = cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (receipt){
var tool_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "tool";
}
})();
var tool_ts = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"ended_at","ended_at",1150683059).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"started_at","started_at",856896776).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run);
}
}
})();
var summary_text = knoxx.backend.openplanner_memory.tool_receipt_summary_text(receipt);
var call_text = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"input_preview","input_preview",2048529734).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return summary_text;
}
})();
var result_text = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"result_preview","result_preview",215554859).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return summary_text;
}
})();
var G__51231 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"receipt","receipt",1871350913),knoxx.backend.openplanner_memory.sanitize_tool_receipt_for_indexing(receipt)], null)], 0)),tool_ts,session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)),"knoxx.tool_receipt",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":tool:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)),conversation_id,summary_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]))], null);
var G__51231__$1 = cljs.core.into.cljs$core$IFn$_invoke$arity$2(G__51231,knoxx.backend.openplanner_memory.session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,scope_extra,"tool_call",tool_ts,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_project)+":run:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":tool-call:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)),(""+"Tool call \u00B7 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})())),run_id,call_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])))
;
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(G__51231__$1,knoxx.backend.openplanner_memory.session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,scope_extra,"tool_result",tool_ts,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_project)+":run:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":tool-result:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)),(""+"Tool result \u00B7 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(receipt);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})())),run_id,result_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])));

}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"tool_receipts","tool_receipts",1763489067).cljs$core$IFn$_invoke$arity$1(run)], 0));
var graph_events = cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.openplanner_memory.session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,scope_extra,"user_message",new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(run),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_project)+":run:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":user"),"User message",run_id,request_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])),((clojure.string.blank_QMARK_((function (){var or__5142__auto__ = answer;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))?null:knoxx.backend.openplanner_memory.session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,scope_extra,"assistant_message",new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_project)+":run:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":assistant"),"Assistant message",run_id,answer,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]))),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([((clojure.string.blank_QMARK_(reasoning_text))?null:knoxx.backend.openplanner_memory.session_text_graph_events(config,extract_mentioned_devel_paths,extract_mentioned_urls,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),new cljs.core.Keyword(null,"scope-extra","scope-extra",-1362977052),new cljs.core.Keyword(null,"node-type","node-type",129492462),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"node-id","node-id",779482292),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],[conversation_id,session_id,scope_extra,"reasoning",new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(run),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_project)+":run:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":reasoning"),"Reasoning",run_id,reasoning_text,new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)])))], 0));
var content_parts__$1 = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(run);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var media_events = ((cljs.core.seq(content_parts__$1))?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.openplanner_memory.openplanner_event(config,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"ts","ts",1617209904),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"session","session",1008279103),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"model","model",331153215)],["system",cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([common_extra,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content_parts_count","content_parts_count",648994127),cljs.core.count(content_parts__$1),new cljs.core.Keyword(null,"content_parts_summary","content_parts_summary",-1938191944),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__51087_SHARP_){
return cljs.core.select_keys(p1__51087_SHARP_,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"url","url",276297046)], null));
}),content_parts__$1)], null)], 0)),new cljs.core.Keyword(null,"created_at","created_at",1484050750).cljs$core$IFn$_invoke$arity$1(run),session_project,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":media"),"knoxx.run.media",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(run_id)+":media"),conversation_id,(""+"Media context: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(content_parts__$1))+" part(s)"+" \u2014 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "?";
}
})())+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "?";
}
})())+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(p);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "(inline)";
}
}
})()));
}),content_parts__$1)))),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(run)]))], null):null);
var all_events = cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(base_events,graph_events,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([tool_events,media_events], 0)));
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/events",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"events","events",1792552201),all_events], null)).catch((function (err){
console.warn("[knoxx] failed to index run memory into OpenPlanner",err);

return null;
}));
});

//# sourceMappingURL=knoxx.backend.openplanner_memory.js.map
