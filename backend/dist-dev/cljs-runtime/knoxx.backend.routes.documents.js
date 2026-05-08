import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.cljs.modern.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.document_state.js";
import "./knoxx.backend.util.time.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.routes.documents');
knoxx.backend.routes.documents.path_is_absolute_QMARK_ = (function knoxx$backend$routes$documents$path_is_absolute_QMARK_(node_path,value){
return node_path.isAbsolute(value);
});
knoxx.backend.routes.documents.path_relative = (function knoxx$backend$routes$documents$path_relative(node_path,from,to){
return node_path.relative(from,to);
});
knoxx.backend.routes.documents.fs_rm_BANG_ = (function knoxx$backend$routes$documents$fs_rm_BANG_(node_fs,path,opts){
return node_fs.rm(path,opts);
});
knoxx.backend.routes.documents.fs_write_buffer_BANG_ = (function knoxx$backend$routes$documents$fs_write_buffer_BANG_(node_fs,path,content){
return node_fs.writeFile(path,content);
});
knoxx.backend.routes.documents.fs_read_file_BANG_ = (function knoxx$backend$routes$documents$fs_read_file_BANG_(node_fs,path,encoding){
return node_fs.readFile(path,encoding);
});
knoxx.backend.routes.documents.parse_auto_ingest_QMARK_ = (function knoxx$backend$routes$documents$parse_auto_ingest_QMARK_(part_seq){
return cljs.core.boolean$(cljs.core.some((function (part){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["type"]),"field")) && (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((part["fieldname"]),"autoIngest")) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((part["value"])))),"true")))));
}),part_seq));
});
knoxx.backend.routes.documents.parse_file_parts = (function knoxx$backend$routes$documents$parse_file_parts(part_seq){
return cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59194_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((p1__59194_SHARP_["type"]),"file");
}),part_seq);
});
knoxx.backend.routes.documents.request_parts_promise = (function knoxx$backend$routes$documents$request_parts_promise(request){
return Array.fromAsync(request.parts());
});
knoxx.backend.routes.documents.process_file_part = (function knoxx$backend$routes$documents$process_file_part(docs_path,part){
var safe_name = knoxx.backend.document_state.sanitize_upload_name((function (){var or__5142__auto__ = (part["filename"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "upload.bin";
}
})());
var abs_path = shadow.esm.esm_import$node_path.join(docs_path,safe_name);
var rel_path = knoxx.backend.document_state.normalize_relative_path(knoxx.backend.routes.documents.path_relative(shadow.esm.esm_import$node_path,docs_path,abs_path));
return (new Response((part["file"]))).arrayBuffer().then((function (buf){
knoxx.backend.routes.documents.fs_write_buffer_BANG_(shadow.esm.esm_import$node_fs$promises,abs_path,Buffer.from(buf));

return rel_path;
}));
});
knoxx.backend.routes.documents.upload_files = (function knoxx$backend$routes$documents$upload_files(docs_path,file_parts){
var promises = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__59201_SHARP_){
return knoxx.backend.routes.documents.process_file_part(docs_path,p1__59201_SHARP_);
}),file_parts);
return Promise.all(cljs.core.clj__GT_js(promises)).then((function (written){
return cljs.core.vec(knoxx.backend.document_state.js_array_seq(written));
}));
});
knoxx.backend.routes.documents.api_documents_list_BANG_ = (function knoxx$backend$routes$documents$api_documents_list_BANG_(app,runtime,config,deps){
var map__59215 = deps;
var map__59215__$1 = cljs.core.__destructure_map(map__59215);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59215__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59219 = app;
var G__59220 = "GET";
var G__59221 = "/api/documents";
var G__59222 = (function (request,reply){
var G__59224 = runtime;
var G__59225 = request;
var G__59226 = reply;
var G__59227 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.read") : ensure_permission_BANG_.call(null,ctx,"datalake.read"));
} else {
}

return knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx).then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59224,G__59225,G__59226,G__59227) : with_request_context_BANG_.call(null,G__59224,G__59225,G__59226,G__59227));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59219,G__59220,G__59221,G__59222) : route_BANG_.call(null,G__59219,G__59220,G__59221,G__59222));
});
knoxx.backend.routes.documents.api_documents_content_BANG_ = (function knoxx$backend$routes$documents$api_documents_content_BANG_(app,runtime,config,deps){
var map__59233 = deps;
var map__59233__$1 = cljs.core.__destructure_map(map__59233);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59233__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59236 = app;
var G__59237 = "GET";
var G__59238 = "/api/documents/content/*";
var G__59239 = (function (request,reply){
var G__59241 = runtime;
var G__59242 = request;
var G__59243 = reply;
var G__59244 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.read") : ensure_permission_BANG_.call(null,ctx,"datalake.read"));
} else {
}

var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var rel_path = knoxx.backend.document_state.normalize_relative_path((request["params"]["*"]));
var abs_path = shadow.esm.esm_import$node_path.resolve(new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),rel_path);
var rel_to_root = shadow.esm.esm_import$node_path.relative(new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),abs_path);
if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_(rel_to_root,"..");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.routes.documents.path_is_absolute_QMARK_(shadow.esm.esm_import$node_path,rel_to_root);
}
})())){
var G__59247 = reply;
var G__59248 = (403);
var G__59249 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Path escapes active docs root"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59247,G__59248,G__59249) : json_response_BANG_.call(null,G__59247,G__59248,G__59249));
} else {
return knoxx.backend.routes.documents.fs_read_file_BANG_(shadow.esm.esm_import$node_fs$promises,abs_path,"utf8").then((function (content){
var G__59252 = reply;
var G__59253 = (200);
var G__59254 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"content","content",15833224),content], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59252,G__59253,G__59254) : json_response_BANG_.call(null,G__59252,G__59253,G__59254));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59241,G__59242,G__59243,G__59244) : with_request_context_BANG_.call(null,G__59241,G__59242,G__59243,G__59244));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59236,G__59237,G__59238,G__59239) : route_BANG_.call(null,G__59236,G__59237,G__59238,G__59239));
});
knoxx.backend.routes.documents.api_documents_delete_BANG_ = (function knoxx$backend$routes$documents$api_documents_delete_BANG_(app,runtime,config,deps){
var map__59267 = deps;
var map__59267__$1 = cljs.core.__destructure_map(map__59267);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var openplanner_graph_export_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"openplanner-graph-export!","openplanner-graph-export!",-1726254887));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59267__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59271 = app;
var G__59272 = "DELETE";
var G__59273 = "/api/documents/*";
var G__59274 = (function (request,reply){
var G__59277 = runtime;
var G__59278 = request;
var G__59279 = reply;
var G__59280 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.write") : ensure_permission_BANG_.call(null,ctx,"datalake.write"));
} else {
}

var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var rel_path = knoxx.backend.document_state.normalize_relative_path((request["params"]["*"]));
var abs_path = shadow.esm.esm_import$node_path.resolve(new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),rel_path);
var rel_to_root = knoxx.backend.routes.documents.path_relative(shadow.esm.esm_import$node_path,new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile),abs_path);
var db_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile);
if(cljs.core.truth_((function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_(rel_to_root,"..");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.routes.documents.path_is_absolute_QMARK_(shadow.esm.esm_import$node_path,rel_to_root);
}
})())){
var G__59286 = reply;
var G__59287 = (403);
var G__59288 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Path escapes active docs root"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59286,G__59287,G__59288) : json_response_BANG_.call(null,G__59286,G__59287,G__59288));
} else {
return knoxx.backend.routes.documents.fs_rm_BANG_(shadow.esm.esm_import$node_fs$promises,abs_path,({"force": true})).then((function (){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.document_state.database_state_STAR_,cljs.core.update_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"indexed","indexed",390758624)], null),cljs.core.dissoc,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([rel_path], 0));

var G__59295 = reply;
var G__59296 = (200);
var G__59297 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),rel_path], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59295,G__59296,G__59297) : json_response_BANG_.call(null,G__59295,G__59296,G__59297));
})).catch((function (err){
var G__59299 = reply;
var G__59300 = (500);
var G__59301 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Delete failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59299,G__59300,G__59301) : json_response_BANG_.call(null,G__59299,G__59300,G__59301));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59277,G__59278,G__59279,G__59280) : with_request_context_BANG_.call(null,G__59277,G__59278,G__59279,G__59280));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59271,G__59272,G__59273,G__59274) : route_BANG_.call(null,G__59271,G__59272,G__59273,G__59274));
});
knoxx.backend.routes.documents.api_documents_ingest_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingest_BANG_(app,runtime,config,deps){
var map__59305 = deps;
var map__59305__$1 = cljs.core.__destructure_map(map__59305);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59305__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59310 = app;
var G__59311 = "POST";
var G__59312 = "/api/documents/ingest";
var G__59313 = (function (request,reply){
var G__59314 = runtime;
var G__59315 = request;
var G__59316 = reply;
var G__59317 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.ingest") : ensure_permission_BANG_.call(null,ctx,"datalake.ingest"));
} else {
}

var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return knoxx.backend.document_state.start_document_ingestion_BANG_(runtime,config,profile,body).then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
var G__59321 = reply;
var G__59322 = (500);
var G__59323 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Ingestion failed to start: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59321,G__59322,G__59323) : json_response_BANG_.call(null,G__59321,G__59322,G__59323));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59314,G__59315,G__59316,G__59317) : with_request_context_BANG_.call(null,G__59314,G__59315,G__59316,G__59317));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59310,G__59311,G__59312,G__59313) : route_BANG_.call(null,G__59310,G__59311,G__59312,G__59313));
});
knoxx.backend.routes.documents.api_documents_ingest_priority_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingest_priority_BANG_(app,runtime,config,deps){
var map__59324 = deps;
var map__59324__$1 = cljs.core.__destructure_map(map__59324);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59324__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59325 = app;
var G__59326 = "POST";
var G__59327 = "/api/documents/ingest/priority";
var G__59328 = (function (request,reply){
var G__59330 = runtime;
var G__59331 = request;
var G__59332 = reply;
var G__59333 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.ingest") : ensure_permission_BANG_.call(null,ctx,"datalake.ingest"));
} else {
}

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var paths = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"paths","paths",-1807389588).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"files","files",-472457450).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})());
var project = new cljs.core.Keyword(null,"project","project",1124394579).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.empty_QMARK_(paths)){
var G__59334 = reply;
var G__59335 = (400);
var G__59336 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"paths (array of workspace-relative file paths) is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59334,G__59335,G__59336) : json_response_BANG_.call(null,G__59334,G__59335,G__59336));
} else {
return knoxx.backend.document_state.priority_ingest_workspace_files_BANG_(runtime,config,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"paths","paths",-1807389588),paths,new cljs.core.Keyword(null,"project","project",1124394579),project], null)).then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
var G__59338 = reply;
var G__59339 = (500);
var G__59340 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Priority ingestion failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59338,G__59339,G__59340) : json_response_BANG_.call(null,G__59338,G__59339,G__59340));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59330,G__59331,G__59332,G__59333) : with_request_context_BANG_.call(null,G__59330,G__59331,G__59332,G__59333));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59325,G__59326,G__59327,G__59328) : route_BANG_.call(null,G__59325,G__59326,G__59327,G__59328));
});
knoxx.backend.routes.documents.api_documents_ingest_restart_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingest_restart_BANG_(app,runtime,config,deps){
var map__59343 = deps;
var map__59343__$1 = cljs.core.__destructure_map(map__59343);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59343__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59344 = app;
var G__59345 = "POST";
var G__59346 = "/api/documents/ingest/restart";
var G__59347 = (function (request,reply){
var G__59348 = runtime;
var G__59349 = request;
var G__59350 = reply;
var G__59351 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.ingest") : ensure_permission_BANG_.call(null,ctx,"datalake.ingest"));
} else {
}

var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var db_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile);
var last_request = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,ctx),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"lastRequest","lastRequest",-738015741)], null));
if((last_request == null)){
var G__59352 = reply;
var G__59353 = (400);
var G__59354 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"No active ingestion to restart",new cljs.core.Keyword(null,"resumed","resumed",897761340),false], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59352,G__59353,G__59354) : json_response_BANG_.call(null,G__59352,G__59353,G__59354));
} else {
return knoxx.backend.document_state.start_document_ingestion_BANG_(runtime,config,profile,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"full","full",436801220),new cljs.core.Keyword(null,"full","full",436801220).cljs$core$IFn$_invoke$arity$1(last_request),new cljs.core.Keyword(null,"selected-files","selected-files",1045525459),new cljs.core.Keyword(null,"selectedFiles","selectedFiles",-2058493306).cljs$core$IFn$_invoke$arity$1(last_request)], null)).then((function (resp){
var G__59356 = reply;
var G__59357 = (200);
var G__59358 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(resp,new cljs.core.Keyword(null,"resumed","resumed",897761340),true);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59356,G__59357,G__59358) : json_response_BANG_.call(null,G__59356,G__59357,G__59358));
})).catch((function (err){
var G__59359 = reply;
var G__59360 = (500);
var G__59361 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Restart failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),new cljs.core.Keyword(null,"resumed","resumed",897761340),false], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59359,G__59360,G__59361) : json_response_BANG_.call(null,G__59359,G__59360,G__59361));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59348,G__59349,G__59350,G__59351) : with_request_context_BANG_.call(null,G__59348,G__59349,G__59350,G__59351));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59344,G__59345,G__59346,G__59347) : route_BANG_.call(null,G__59344,G__59345,G__59346,G__59347));
});
knoxx.backend.routes.documents.api_documents_ingestion_status_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingestion_status_BANG_(app,runtime,config,deps){
var map__59364 = deps;
var map__59364__$1 = cljs.core.__destructure_map(map__59364);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59364__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59365 = app;
var G__59366 = "GET";
var G__59367 = "/api/documents/ingestion-status";
var G__59368 = (function (request,reply){
var G__59369 = runtime;
var G__59370 = request;
var G__59371 = reply;
var G__59372 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.read") : ensure_permission_BANG_.call(null,ctx,"datalake.read"));
} else {
}

var record = knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var progress = new cljs.core.Keyword(null,"progress","progress",244323547).cljs$core$IFn$_invoke$arity$1(record);
var G__59373 = reply;
var G__59374 = (200);
var G__59375 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"active","active",1895962068),cljs.core.boolean$(new cljs.core.Keyword(null,"active","active",1895962068).cljs$core$IFn$_invoke$arity$1(progress)),new cljs.core.Keyword(null,"progress","progress",244323547),progress,new cljs.core.Keyword(null,"canResumeForum","canResumeForum",-218600603),false,new cljs.core.Keyword(null,"stale","stale",395586896),false], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59373,G__59374,G__59375) : json_response_BANG_.call(null,G__59373,G__59374,G__59375));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59369,G__59370,G__59371,G__59372) : with_request_context_BANG_.call(null,G__59369,G__59370,G__59371,G__59372));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59365,G__59366,G__59367,G__59368) : route_BANG_.call(null,G__59365,G__59366,G__59367,G__59368));
});
knoxx.backend.routes.documents.api_documents_ingestion_progress_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingestion_progress_BANG_(app,runtime,config,deps){
var map__59376 = deps;
var map__59376__$1 = cljs.core.__destructure_map(map__59376);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59376__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59377 = app;
var G__59378 = "GET";
var G__59379 = "/api/documents/ingestion-progress";
var G__59380 = (function (request,reply){
var G__59381 = runtime;
var G__59382 = request;
var G__59383 = reply;
var G__59384 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.read") : ensure_permission_BANG_.call(null,ctx,"datalake.read"));
} else {
}

var record = knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var progress = new cljs.core.Keyword(null,"progress","progress",244323547).cljs$core$IFn$_invoke$arity$1(record);
var G__59385 = reply;
var G__59386 = (200);
var G__59387 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"active","active",1895962068),cljs.core.boolean$(new cljs.core.Keyword(null,"active","active",1895962068).cljs$core$IFn$_invoke$arity$1(progress)),new cljs.core.Keyword(null,"progress","progress",244323547),progress,new cljs.core.Keyword(null,"canResumeForum","canResumeForum",-218600603),false,new cljs.core.Keyword(null,"stale","stale",395586896),false], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59385,G__59386,G__59387) : json_response_BANG_.call(null,G__59385,G__59386,G__59387));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59381,G__59382,G__59383,G__59384) : with_request_context_BANG_.call(null,G__59381,G__59382,G__59383,G__59384));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59377,G__59378,G__59379,G__59380) : route_BANG_.call(null,G__59377,G__59378,G__59379,G__59380));
});
knoxx.backend.routes.documents.api_documents_ingestion_history_BANG_ = (function knoxx$backend$routes$documents$api_documents_ingestion_history_BANG_(app,runtime,config,deps){
var map__59388 = deps;
var map__59388__$1 = cljs.core.__destructure_map(map__59388);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59388__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59389 = app;
var G__59390 = "GET";
var G__59391 = "/api/documents/ingestion-history";
var G__59392 = (function (request,reply){
var G__59393 = runtime;
var G__59394 = request;
var G__59395 = reply;
var G__59396 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.read") : ensure_permission_BANG_.call(null,ctx,"datalake.read"));
} else {
}

var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var record = knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var G__59397 = reply;
var G__59398 = (200);
var G__59399 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"collection","collection",-683361892),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371).cljs$core$IFn$_invoke$arity$1(profile),new cljs.core.Keyword(null,"items","items",1031954938),new cljs.core.Keyword(null,"history","history",-247395220).cljs$core$IFn$_invoke$arity$1(record)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59397,G__59398,G__59399) : json_response_BANG_.call(null,G__59397,G__59398,G__59399));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59393,G__59394,G__59395,G__59396) : with_request_context_BANG_.call(null,G__59393,G__59394,G__59395,G__59396));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59389,G__59390,G__59391,G__59392) : route_BANG_.call(null,G__59389,G__59390,G__59391,G__59392));
});
knoxx.backend.routes.documents.api_chat_retrieval_debug_BANG_ = (function knoxx$backend$routes$documents$api_chat_retrieval_debug_BANG_(app,runtime,config,deps){
var map__59404 = deps;
var map__59404__$1 = cljs.core.__destructure_map(map__59404);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59404__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59406 = app;
var G__59407 = "POST";
var G__59408 = "/api/chat/retrieval-debug";
var G__59409 = (function (request,reply){
var G__59411 = runtime;
var G__59412 = request;
var G__59413 = reply;
var G__59414 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.query") : ensure_permission_BANG_.call(null,ctx,"datalake.query"));
} else {
}

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var query = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(body))));
var top_k = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"topK","topK",939681881).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (5);
}
})();
if(clojure.string.blank_QMARK_(query)){
var G__59416 = reply;
var G__59417 = (400);
var G__59418 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"message is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59416,G__59417,G__59418) : json_response_BANG_.call(null,G__59416,G__59417,G__59418));
} else {
return knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx).then((function (resp){
var documents = new cljs.core.Keyword(null,"documents","documents",-1582333455).cljs$core$IFn$_invoke$arity$1(resp);
var lowered = clojure.string.lower_case(query);
var results = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(top_k,cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"score","score",-1963588780),cljs.core._GT_,cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59401_SHARP_){
return (new cljs.core.Keyword(null,"score","score",-1963588780).cljs$core$IFn$_invoke$arity$1(p1__59401_SHARP_) > (0));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (doc){
var path = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"relativePath","relativePath",-608773918).cljs$core$IFn$_invoke$arity$1(doc)));
var name = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(doc)));
var hay = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)));
var score = ((clojure.string.includes_QMARK_(hay,lowered))?(1):((clojure.string.includes_QMARK_(lowered,clojure.string.lower_case(name)))?0.5:(0)
));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(doc,new cljs.core.Keyword(null,"score","score",-1963588780),score);
}),documents)))));
var G__59427 = reply;
var G__59428 = (200);
var G__59429 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"topK","topK",939681881),top_k,new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59427,G__59428,G__59429) : json_response_BANG_.call(null,G__59427,G__59428,G__59429));
})).catch((function (err){
var G__59430 = reply;
var G__59431 = (500);
var G__59432 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Retrieval debug failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59430,G__59431,G__59432) : json_response_BANG_.call(null,G__59430,G__59431,G__59432));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59411,G__59412,G__59413,G__59414) : with_request_context_BANG_.call(null,G__59411,G__59412,G__59413,G__59414));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59406,G__59407,G__59408,G__59409) : route_BANG_.call(null,G__59406,G__59407,G__59408,G__59409));
});
knoxx.backend.routes.documents.api_graph_export_BANG_ = (function knoxx$backend$routes$documents$api_graph_export_BANG_(app,runtime,config,deps){
var map__59435 = deps;
var map__59435__$1 = cljs.core.__destructure_map(map__59435);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var openplanner_graph_export_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"openplanner-graph-export!","openplanner-graph-export!",-1726254887));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59435__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59438 = app;
var G__59439 = "GET";
var G__59440 = "/api/graph/export";
var G__59441 = (function (request,reply){
var G__59446 = runtime;
var G__59447 = request;
var G__59448 = reply;
var G__59449 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"datalake.query") : ensure_permission_BANG_.call(null,ctx,"datalake.query"));
} else {
}

return (openplanner_graph_export_BANG_.cljs$core$IFn$_invoke$arity$2 ? openplanner_graph_export_BANG_.cljs$core$IFn$_invoke$arity$2(config,request) : openplanner_graph_export_BANG_.call(null,config,request)).then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59446,G__59447,G__59448,G__59449) : with_request_context_BANG_.call(null,G__59446,G__59447,G__59448,G__59449));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59438,G__59439,G__59440,G__59441) : route_BANG_.call(null,G__59438,G__59439,G__59440,G__59441));
});
knoxx.backend.routes.documents.api_settings_databases_list_BANG_ = (function knoxx$backend$routes$documents$api_settings_databases_list_BANG_(app,runtime,config,deps){
var map__59454 = deps;
var map__59454__$1 = cljs.core.__destructure_map(map__59454);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59454__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59458 = app;
var G__59459 = "GET";
var G__59460 = "/api/settings/databases";
var G__59461 = (function (request,reply){
var G__59462 = runtime;
var G__59463 = request;
var G__59464 = reply;
var G__59465 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.datalakes.read") : ensure_permission_BANG_.call(null,ctx,"org.datalakes.read"));
} else {
}

var state = knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,ctx);
var session_id = knoxx.backend.document_state.request_session_id(request);
var active_id = knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4(runtime,config,request,ctx);
var active_profile = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),active_id], null));
var profiles = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (profile){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(profile,new cljs.core.Keyword(null,"canAccess","canAccess",-1259964654),knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,ctx,session_id));
}),cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"createdAt","createdAt",-936788),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59453_SHARP_){
return knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(p1__59453_SHARP_,ctx,session_id);
}),cljs.core.vals(new cljs.core.Keyword(null,"profiles","profiles",507634713).cljs$core$IFn$_invoke$arity$1(state)))));
var G__59469 = reply;
var G__59470 = (200);
var G__59471 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"activeDatabaseId","activeDatabaseId",-1115597611),active_id,new cljs.core.Keyword(null,"databases","databases",2040134125),profiles,new cljs.core.Keyword(null,"activeRuntime","activeRuntime",438512110),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"projectName","projectName",295421548),new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(active_profile),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371).cljs$core$IFn$_invoke$arity$1(active_profile)], null)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59469,G__59470,G__59471) : json_response_BANG_.call(null,G__59469,G__59470,G__59471));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59462,G__59463,G__59464,G__59465) : with_request_context_BANG_.call(null,G__59462,G__59463,G__59464,G__59465));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59458,G__59459,G__59460,G__59461) : route_BANG_.call(null,G__59458,G__59459,G__59460,G__59461));
});
knoxx.backend.routes.documents.api_settings_databases_create_BANG_ = (function knoxx$backend$routes$documents$api_settings_databases_create_BANG_(app,runtime,config,deps){
var map__59480 = deps;
var map__59480__$1 = cljs.core.__destructure_map(map__59480);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59480__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59485 = app;
var G__59486 = "POST";
var G__59487 = "/api/settings/databases";
var G__59488 = (function (request,reply){
var G__59489 = runtime;
var G__59490 = request;
var G__59491 = reply;
var G__59492 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.datalakes.create") : ensure_permission_BANG_.call(null,ctx,"org.datalakes.create"));
} else {
}

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var name = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(body))));
var session_id = knoxx.backend.document_state.request_session_id(request);
if(clojure.string.blank_QMARK_(name)){
var G__59493 = reply;
var G__59494 = (400);
var G__59495 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"name is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59493,G__59494,G__59495) : json_response_BANG_.call(null,G__59493,G__59494,G__59495));
} else {
var db_id = knoxx.backend.document_state.create_db_id(runtime,name);
var docs_path = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.document_state.database_docs_dir(runtime,config,db_id);
}
})();
var profile = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"privateToSession","privateToSession",-73446717),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"forumMode","forumMode",2078997894),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882),new cljs.core.Keyword(null,"ownerSessionId","ownerSessionId",1073095462),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"useLocalDocsBaseUrl","useLocalDocsBaseUrl",-1109521974),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"ownerUserId","ownerUserId",-1250504308),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371),new cljs.core.Keyword(null,"ownerMembershipId","ownerMembershipId",2136804692),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"publicDocsBaseUrl","publicDocsBaseUrl",-1708554755)],[cljs.core.boolean$(new cljs.core.Keyword(null,"privateToSession","privateToSession",-73446717).cljs$core$IFn$_invoke$arity$1(body)),knoxx.backend.authz.ctx_org_id(ctx),cljs.core.boolean$(new cljs.core.Keyword(null,"forumMode","forumMode",2078997894).cljs$core$IFn$_invoke$arity$1(body)),docs_path,(cljs.core.truth_(new cljs.core.Keyword(null,"privateToSession","privateToSession",-73446717).cljs$core$IFn$_invoke$arity$1(body))?session_id:null),name,knoxx.backend.authz.ctx_org_slug(ctx),cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,new cljs.core.Keyword(null,"useLocalDocsBaseUrl","useLocalDocsBaseUrl",-1109521974).cljs$core$IFn$_invoke$arity$1(body)),knoxx.backend.util.time.now_iso(),knoxx.backend.authz.ctx_user_id(ctx),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"collection-name","collection-name",600435477).cljs$core$IFn$_invoke$arity$1(config))+"_"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(db_id));
}
})(),knoxx.backend.authz.ctx_membership_id(ctx),db_id,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"publicDocsBaseUrl","publicDocsBaseUrl",-1708554755).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()]);
return knoxx.backend.document_state.ensure_dir_BANG_(runtime,docs_path).then((function (){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.database_state_STAR_,(function (state){
var owner_key = knoxx.backend.document_state.database_owner_key(ctx);
return (function (s){
if(cljs.core.truth_(new cljs.core.Keyword(null,"activate","activate",441219614).cljs$core$IFn$_invoke$arity$1(body))){
return cljs.core.assoc_in(s,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),owner_key], null),db_id);
} else {
return s;
}
})(cljs.core.assoc_in(cljs.core.assoc_in(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null),profile),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id], null),knoxx.backend.document_state.default_database_record()));
}));

return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),profile) : json_response_BANG_.call(null,reply,(200),profile));
})).catch((function (err){
var G__59498 = reply;
var G__59499 = (500);
var G__59500 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to create database profile: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59498,G__59499,G__59500) : json_response_BANG_.call(null,G__59498,G__59499,G__59500));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59489,G__59490,G__59491,G__59492) : with_request_context_BANG_.call(null,G__59489,G__59490,G__59491,G__59492));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59485,G__59486,G__59487,G__59488) : route_BANG_.call(null,G__59485,G__59486,G__59487,G__59488));
});
knoxx.backend.routes.documents.api_settings_databases_activate_BANG_ = (function knoxx$backend$routes$documents$api_settings_databases_activate_BANG_(app,runtime,config,deps){
var map__59504 = deps;
var map__59504__$1 = cljs.core.__destructure_map(map__59504);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59504__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59505 = app;
var G__59506 = "POST";
var G__59507 = "/api/settings/databases/activate";
var G__59508 = (function (request,reply){
var G__59509 = runtime;
var G__59510 = request;
var G__59511 = reply;
var G__59512 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.datalakes.read") : ensure_permission_BANG_.call(null,ctx,"org.datalakes.read"));
} else {
}

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var db_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(body)));
var session_id = knoxx.backend.document_state.request_session_id(request);
var profile = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,ctx),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null));
if((profile == null)){
var G__59514 = reply;
var G__59515 = (404);
var G__59516 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59514,G__59515,G__59516) : json_response_BANG_.call(null,G__59514,G__59515,G__59516));
} else {
if((!(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,ctx,session_id)))){
var G__59517 = reply;
var G__59518 = (403);
var G__59519 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile is outside the current Knoxx scope"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59517,G__59518,G__59519) : json_response_BANG_.call(null,G__59517,G__59518,G__59519));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.document_state.database_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),knoxx.backend.document_state.database_owner_key(ctx)], null),db_id);

var G__59520 = reply;
var G__59521 = (200);
var G__59522 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"activeDatabaseId","activeDatabaseId",-1115597611),db_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59520,G__59521,G__59522) : json_response_BANG_.call(null,G__59520,G__59521,G__59522));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59509,G__59510,G__59511,G__59512) : with_request_context_BANG_.call(null,G__59509,G__59510,G__59511,G__59512));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59505,G__59506,G__59507,G__59508) : route_BANG_.call(null,G__59505,G__59506,G__59507,G__59508));
});
knoxx.backend.routes.documents.api_settings_databases_update_BANG_ = (function knoxx$backend$routes$documents$api_settings_databases_update_BANG_(app,runtime,config,deps){
var map__59525 = deps;
var map__59525__$1 = cljs.core.__destructure_map(map__59525);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59525__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59529 = app;
var G__59530 = "PATCH";
var G__59531 = "/api/settings/databases/:id";
var G__59532 = (function (request,reply){
var G__59534 = runtime;
var G__59535 = request;
var G__59536 = reply;
var G__59537 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.datalakes.update") : ensure_permission_BANG_.call(null,ctx,"org.datalakes.update"));
} else {
}

var db_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((request["params"]["id"])));
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var session_id = knoxx.backend.document_state.request_session_id(request);
var profile = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,ctx),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null));
if((profile == null)){
var G__59553 = reply;
var G__59554 = (404);
var G__59555 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59553,G__59554,G__59555) : json_response_BANG_.call(null,G__59553,G__59554,G__59555));
} else {
if((!(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,ctx,session_id)))){
var G__59557 = reply;
var G__59558 = (403);
var G__59559 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile is outside the current Knoxx scope"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59557,G__59558,G__59559) : json_response_BANG_.call(null,G__59557,G__59558,G__59559));
} else {
var updated = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([profile,cljs.core.select_keys(body,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"publicDocsBaseUrl","publicDocsBaseUrl",-1708554755),new cljs.core.Keyword(null,"useLocalDocsBaseUrl","useLocalDocsBaseUrl",-1109521974),new cljs.core.Keyword(null,"forumMode","forumMode",2078997894)], null))], 0));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.document_state.database_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null),updated);

return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),updated) : json_response_BANG_.call(null,reply,(200),updated));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59534,G__59535,G__59536,G__59537) : with_request_context_BANG_.call(null,G__59534,G__59535,G__59536,G__59537));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59529,G__59530,G__59531,G__59532) : route_BANG_.call(null,G__59529,G__59530,G__59531,G__59532));
});
knoxx.backend.routes.documents.api_settings_databases_delete_BANG_ = (function knoxx$backend$routes$documents$api_settings_databases_delete_BANG_(app,runtime,config,deps){
var map__59576 = deps;
var map__59576__$1 = cljs.core.__destructure_map(map__59576);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59576__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59581 = app;
var G__59582 = "DELETE";
var G__59583 = "/api/settings/databases/:id";
var G__59584 = (function (request,reply){
var G__59586 = runtime;
var G__59587 = request;
var G__59588 = reply;
var G__59589 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.datalakes.delete") : ensure_permission_BANG_.call(null,ctx,"org.datalakes.delete"));
} else {
}

var db_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((request["params"]["id"])));
var session_id = knoxx.backend.document_state.request_session_id(request);
var profile = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,ctx),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null));
if((profile == null)){
var G__59594 = reply;
var G__59595 = (404);
var G__59596 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59594,G__59595,G__59596) : json_response_BANG_.call(null,G__59594,G__59595,G__59596));
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(db_id,knoxx.backend.document_state.default_database_id(ctx))){
var G__59597 = reply;
var G__59598 = (400);
var G__59599 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Default database cannot be deleted"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59597,G__59598,G__59599) : json_response_BANG_.call(null,G__59597,G__59598,G__59599));
} else {
if((!(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,ctx,session_id)))){
var G__59600 = reply;
var G__59601 = (403);
var G__59602 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database profile is outside the current Knoxx scope"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59600,G__59601,G__59602) : json_response_BANG_.call(null,G__59600,G__59601,G__59602));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.database_state_STAR_,(function (state){
var owner_key = knoxx.backend.document_state.database_owner_key(ctx);
return (function (s){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(s,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),owner_key], null)),db_id)){
return cljs.core.assoc_in(s,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),owner_key], null),knoxx.backend.document_state.default_database_id(ctx));
} else {
return s;
}
})(cljs.core.update.cljs$core$IFn$_invoke$arity$4(cljs.core.update.cljs$core$IFn$_invoke$arity$4(state,new cljs.core.Keyword(null,"profiles","profiles",507634713),cljs.core.dissoc,db_id),new cljs.core.Keyword(null,"records","records",1326822832),cljs.core.dissoc,db_id));
}));

var G__59603 = reply;
var G__59604 = (200);
var G__59605 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"deleted","deleted",-510100639),db_id], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59603,G__59604,G__59605) : json_response_BANG_.call(null,G__59603,G__59604,G__59605));

}
}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59586,G__59587,G__59588,G__59589) : with_request_context_BANG_.call(null,G__59586,G__59587,G__59588,G__59589));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59581,G__59582,G__59583,G__59584) : route_BANG_.call(null,G__59581,G__59582,G__59583,G__59584));
});
knoxx.backend.routes.documents.register_document_routes_BANG_ = (function knoxx$backend$routes$documents$register_document_routes_BANG_(app,runtime,config,deps){
knoxx.backend.routes.documents.api_documents_list_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_content_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_delete_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_ingest_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_ingest_priority_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_ingest_restart_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_ingestion_progress_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_documents_ingestion_history_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_chat_retrieval_debug_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_graph_export_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_settings_databases_list_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_settings_databases_create_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_settings_databases_activate_BANG_(app,runtime,config,deps);

knoxx.backend.routes.documents.api_settings_databases_update_BANG_(app,runtime,config,deps);

return knoxx.backend.routes.documents.api_settings_databases_delete_BANG_(app,runtime,config,deps);
});

//# sourceMappingURL=knoxx.backend.routes.documents.js.map
