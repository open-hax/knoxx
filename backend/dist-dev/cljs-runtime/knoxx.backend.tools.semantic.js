import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.document_state.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.tools.semantic');
knoxx.backend.tools.semantic.query_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Natural-language semantic search query for the active Knoxx corpus."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"topK","topK",939681881),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum number of matches to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(10)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maxSnippetChars","maxSnippetChars",190771964),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum snippet length per hit."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(160),new cljs.core.Keyword(null,"max","max",61366548),(1200)], null)], null)], null)], null);
knoxx.backend.tools.semantic.read_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"path","path",-188191168),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Relative document path returned by semantic_query or visible in the active corpus."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maxChars","maxChars",-1468489647),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum characters of document content to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(500),new cljs.core.Keyword(null,"max","max",61366548),(20000)], null)], null)], null)], null);
knoxx.backend.tools.semantic.sanitize_svg_content = (function knoxx$backend$tools$semantic$sanitize_svg_content(content){
return clojure.string.replace(clojure.string.replace(content,/<script[^>]*>.*?<\/script>/is,""),/on[a-z]+\s*=\s*['\"].*?['\"]/i,"");
});
knoxx.backend.tools.semantic.hydrate_files = (function knoxx$backend$tools$semantic$hydrate_files(paths,node_path,docs_path,runtime,config,db_id,node_fs,query,tokens,max_snippet_chars){
return cljs.core.clj__GT_js((function (){var iter__5628__auto__ = (function knoxx$backend$tools$semantic$hydrate_files_$_iter__59187(s__59188){
return (new cljs.core.LazySeq(null,(function (){
var s__59188__$1 = s__59188;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__59188__$1);
if(temp__5825__auto__){
var s__59188__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__59188__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__59188__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__59190 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__59189 = (0);
while(true){
if((i__59189 < size__5627__auto__)){
var abs_path = cljs.core._nth(c__5626__auto__,i__59189);
cljs.core.chunk_append(b__59190,(function (){var rel_path = knoxx.backend.document_state.normalize_relative_path(knoxx.backend.tools.media.path_relative(node_path,docs_path,abs_path));
var name = knoxx.backend.tools.media.path_basename(node_path,abs_path);
var indexed_meta = knoxx.backend.document_state.indexed_meta(runtime,config,db_id,rel_path);
if(knoxx.backend.text.text_like_path_QMARK_(rel_path)){
return knoxx.backend.tools.media.fs_read_file_BANG_.cljs$core$IFn$_invoke$arity$3(node_fs,abs_path,"utf8").then(((function (i__59189,rel_path,name,indexed_meta,abs_path,c__5626__auto__,size__5627__auto__,b__59190,s__59188__$2,temp__5825__auto__){
return (function (content){
var cleaned = (cljs.core.truth_(cljs.core.re_find(/\.svg$/i,rel_path))?knoxx.backend.tools.semantic.sanitize_svg_content(content):content);
var vec__59195 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(cleaned,(20000));
var clipped = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59195,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59195,(1),null);
var score = knoxx.backend.text.semantic_score(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"tokens","tokens",-818939304),tokens,new cljs.core.Keyword(null,"rel-path","rel-path",593215642),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"text","text",-1790561697),clipped,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.boolean$(indexed_meta)], null));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),score,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.boolean$(indexed_meta),new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666).cljs$core$IFn$_invoke$arity$1(indexed_meta);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"snippet","snippet",953581994),knoxx.backend.text.snippet_around(clipped,clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query))),tokens,max_snippet_chars)], null);
});})(i__59189,rel_path,name,indexed_meta,abs_path,c__5626__auto__,size__5627__auto__,b__59190,s__59188__$2,temp__5825__auto__))
).catch(((function (i__59189,rel_path,name,indexed_meta,abs_path,c__5626__auto__,size__5627__auto__,b__59190,s__59188__$2,temp__5825__auto__){
return (function (_err){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),(0),new cljs.core.Keyword(null,"indexed","indexed",390758624),false,new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(0),new cljs.core.Keyword(null,"snippet","snippet",953581994),""], null);
});})(i__59189,rel_path,name,indexed_meta,abs_path,c__5626__auto__,size__5627__auto__,b__59190,s__59188__$2,temp__5825__auto__))
);
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),(0),new cljs.core.Keyword(null,"indexed","indexed",390758624),false,new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(0),new cljs.core.Keyword(null,"snippet","snippet",953581994),""], null));
}
})());

var G__59400 = (i__59189 + (1));
i__59189 = G__59400;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__59190),knoxx$backend$tools$semantic$hydrate_files_$_iter__59187(cljs.core.chunk_rest(s__59188__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__59190),null);
}
} else {
var abs_path = cljs.core.first(s__59188__$2);
return cljs.core.cons((function (){var rel_path = knoxx.backend.document_state.normalize_relative_path(knoxx.backend.tools.media.path_relative(node_path,docs_path,abs_path));
var name = knoxx.backend.tools.media.path_basename(node_path,abs_path);
var indexed_meta = knoxx.backend.document_state.indexed_meta(runtime,config,db_id,rel_path);
if(knoxx.backend.text.text_like_path_QMARK_(rel_path)){
return knoxx.backend.tools.media.fs_read_file_BANG_.cljs$core$IFn$_invoke$arity$3(node_fs,abs_path,"utf8").then(((function (rel_path,name,indexed_meta,abs_path,s__59188__$2,temp__5825__auto__){
return (function (content){
var cleaned = (cljs.core.truth_(cljs.core.re_find(/\.svg$/i,rel_path))?knoxx.backend.tools.semantic.sanitize_svg_content(content):content);
var vec__59205 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(cleaned,(20000));
var clipped = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59205,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59205,(1),null);
var score = knoxx.backend.text.semantic_score(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"tokens","tokens",-818939304),tokens,new cljs.core.Keyword(null,"rel-path","rel-path",593215642),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"text","text",-1790561697),clipped,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.boolean$(indexed_meta)], null));
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),score,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.boolean$(indexed_meta),new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666).cljs$core$IFn$_invoke$arity$1(indexed_meta);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"snippet","snippet",953581994),knoxx.backend.text.snippet_around(clipped,clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query))),tokens,max_snippet_chars)], null);
});})(rel_path,name,indexed_meta,abs_path,s__59188__$2,temp__5825__auto__))
).catch(((function (rel_path,name,indexed_meta,abs_path,s__59188__$2,temp__5825__auto__){
return (function (_err){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),(0),new cljs.core.Keyword(null,"indexed","indexed",390758624),false,new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(0),new cljs.core.Keyword(null,"snippet","snippet",953581994),""], null);
});})(rel_path,name,indexed_meta,abs_path,s__59188__$2,temp__5825__auto__))
);
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"score","score",-1963588780),(0),new cljs.core.Keyword(null,"indexed","indexed",390758624),false,new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(0),new cljs.core.Keyword(null,"snippet","snippet",953581994),""], null));
}
})(),knoxx$backend$tools$semantic$hydrate_files_$_iter__59187(cljs.core.rest(s__59188__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(paths);
})());
});
knoxx.backend.tools.semantic.rank_results = (function knoxx$backend$tools$semantic$rank_results(results,top_k,profile,docs_path,query,tokens){
var ranked = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(top_k,cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(cljs.core.juxt.cljs$core$IFn$_invoke$arity$2(cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core._,new cljs.core.Keyword(null,"score","score",-1963588780)),new cljs.core.Keyword(null,"path","path",-188191168)),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59213_SHARP_){
return (new cljs.core.Keyword(null,"score","score",-1963588780).cljs$core$IFn$_invoke$arity$1(p1__59213_SHARP_) > (0));
}),knoxx.backend.http.js_array_seq(results)))));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"database","database",1849087575),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(profile),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882),docs_path], null),new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"tokens","tokens",-818939304),tokens,new cljs.core.Keyword(null,"results","results",-1134170113),ranked], null);
});
knoxx.backend.tools.semantic.semantic_search_documents_BANG_ = (function knoxx$backend$tools$semantic$semantic_search_documents_BANG_(var_args){
var G__59229 = arguments.length;
switch (G__59229) {
case 3:
return knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,opts){
return knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,opts,null);
}));

(knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,p__59234,auth_context){
var map__59235 = p__59234;
var map__59235__$1 = cljs.core.__destructure_map(map__59235);
var query = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59235__$1,new cljs.core.Keyword(null,"query","query",-1288509510));
var top_k = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59235__$1,new cljs.core.Keyword(null,"top-k","top-k",-1255881544));
var max_snippet_chars = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59235__$1,new cljs.core.Keyword(null,"max-snippet-chars","max-snippet-chars",785562463));
var profile = knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var db_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile);
var docs_path = new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile);
var tokens = knoxx.backend.text.search_tokens(query);
var top_k__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((10),(function (){var or__5142__auto__ = top_k;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (5);
}
})()));
var max_snippet_chars__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((160),cljs.core.min.cljs$core$IFn$_invoke$arity$2((1200),(function (){var or__5142__auto__ = max_snippet_chars;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (320);
}
})()));
return knoxx.backend.document_state.ensure_dir_BANG_(runtime,docs_path).then((function (){
return knoxx.backend.document_state.list_files_recursive_BANG_(runtime,docs_path);
})).then((function (paths){
return Promise.all(knoxx.backend.tools.semantic.hydrate_files(paths,shadow.esm.esm_import$node_path,docs_path,runtime,config,db_id,shadow.esm.esm_import$node_fs$promises,query,tokens,max_snippet_chars__$1));
})).then((function (results){
return knoxx.backend.tools.semantic.rank_results(results,top_k__$1,profile,docs_path,query,tokens);
}));
}));

(knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.tools.semantic.semantic_read_document_BANG_ = (function knoxx$backend$tools$semantic$semantic_read_document_BANG_(var_args){
var G__59302 = arguments.length;
switch (G__59302) {
case 3:
return knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,opts){
return knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,opts,null);
}));

(knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,p__59304,auth_context){
var map__59306 = p__59304;
var map__59306__$1 = cljs.core.__destructure_map(map__59306);
var path = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59306__$1,new cljs.core.Keyword(null,"path","path",-188191168));
var max_chars = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59306__$1,new cljs.core.Keyword(null,"max-chars","max-chars",899663888));
var profile = knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var rel_path = knoxx.backend.document_state.normalize_relative_path(path);
var abs_path = knoxx.backend.tools.media.path_resolve.cljs$core$IFn$_invoke$arity$variadic(shadow.esm.esm_import$node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),rel_path], 0));
var rel_to_root = knoxx.backend.tools.media.path_relative(shadow.esm.esm_import$node_path,new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),abs_path);
var max_chars__$1 = cljs.core.max.cljs$core$IFn$_invoke$arity$2((500),cljs.core.min.cljs$core$IFn$_invoke$arity$2((20000),(function (){var or__5142__auto__ = max_chars;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (6000);
}
})()));
if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_(rel_to_root,"..");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.tools.media.path_is_absolute_QMARK_(shadow.esm.esm_import$node_path,rel_to_root);
}
})())){
return Promise.reject((new Error("Path escapes active docs root")));
} else {
return knoxx.backend.tools.media.fs_read_file_BANG_.cljs$core$IFn$_invoke$arity$3(shadow.esm.esm_import$node_fs$promises,abs_path,"utf8").then((function (content){
var cleaned = (cljs.core.truth_(cljs.core.re_find(/\.svg$/i,rel_path))?knoxx.backend.tools.semantic.sanitize_svg_content(content):content);
var vec__59318 = knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2(cleaned,max_chars__$1);
var clipped = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59318,(0),null);
var truncated_QMARK_ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__59318,(1),null);
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"database","database",1849087575),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(profile)], null),new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"truncated","truncated",298102102),truncated_QMARK_,new cljs.core.Keyword(null,"content","content",15833224),clipped], null);
}));
}
}));

(knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.tools.semantic.semantic_query_execute = (function knoxx$backend$tools$semantic$semantic_query_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var top_k = (function (){var or__5142__auto__ = (params["topK"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["top_k"]);
}
})();
var max_snippet_chars = (function (){var or__5142__auto__ = (params["maxSnippetChars"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["max_snippet_chars"]);
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Searching corpus via OpenPlanner\u2026");

if(knoxx.backend.http.openplanner_enabled_QMARK_(config)){
return knoxx.backend.openplanner_memory.openplanner_semantic_search_BANG_(config,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"k","k",-2146297393),(function (){var or__5142__auto__ = top_k;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (5);
}
})()], null)).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.text.openplanner_semantic_search_text(result),result);
}));
} else {
return knoxx.backend.tools.semantic.semantic_search_documents_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"top-k","top-k",-1255881544),top_k,new cljs.core.Keyword(null,"max-snippet-chars","max-snippet-chars",785562463),max_snippet_chars], null)).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.text.semantic_search_result_text(result),result);
}));
}
});
knoxx.backend.tools.semantic.semantic_read_execute = (function knoxx$backend$tools$semantic$semantic_read_execute(runtime,config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var path = (function (){var or__5142__auto__ = (params["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var max_chars = (function (){var or__5142__auto__ = (params["maxChars"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["max_chars"]);
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Reading corpus document\u2026");

return knoxx.backend.tools.semantic.semantic_read_document_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),path,new cljs.core.Keyword(null,"max-chars","max-chars",899663888),max_chars], null)).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.text.semantic_read_result_text(result),result);
}));
});
knoxx.backend.tools.semantic.semantic_query_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"semantic_query","Semantic Query","Search the active Knoxx knowledge corpus for semantically relevant documents and snippets.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search the active Knoxx corpus by meaning and retrieve the most relevant documents/snippets.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use semantic_query when you need grounded workspace knowledge beyond what passive hydration already exposed.","Prefer semantic_query over guessing when the answer may live in notes, uploaded documents, or indexed corpus files.","Follow semantic_query with semantic_read when one result looks promising and you need exact source text."], null),knoxx.backend.tools.semantic.query_params,knoxx.backend.tools.semantic.semantic_query_execute], 0));
knoxx.backend.tools.semantic.semantic_read_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"semantic_read","Read Document","Read a document by relative path from the active Knoxx corpus.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read a specific Knoxx corpus document by relative path after semantic_query identifies a likely hit.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use semantic_read after semantic_query when you need exact source text instead of summaries or snippets.","Pass a relative document path from semantic_query results."], null),knoxx.backend.tools.semantic.read_params,knoxx.backend.tools.semantic.semantic_read_execute], 0));
knoxx.backend.tools.semantic.create_semantic_custom_tools = (function knoxx$backend$tools$semantic$create_semantic_custom_tools(var_args){
var G__59363 = arguments.length;
switch (G__59363) {
case 2:
return knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(((((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"semantic_query"))))?knoxx.backend.tools.semantic.semantic_query_tool(runtime,config):null),(((((auth_context == null)) || (((knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"read")) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,"semantic_query"))))))?knoxx.backend.tools.semantic.semantic_read_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.semantic.create_semantic_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.semantic.js.map
