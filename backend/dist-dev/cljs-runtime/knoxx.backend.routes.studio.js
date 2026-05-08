import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.audio_labels.js";
import "./knoxx.backend.routes.studio.discord_scan.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.routes.studio');
knoxx.backend.routes.studio.audio_extensions = (function knoxx$backend$routes$studio$audio_extensions(){
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 8, [".flac",null,".mp3",null,".ogg",null,".m4a",null,".wma",null,".wav",null,".aac",null,".opus",null], null), null);
});
knoxx.backend.routes.studio.audio_mime_type = (function knoxx$backend$routes$studio$audio_mime_type(ext){
var G__52792 = ext;
switch (G__52792) {
case ".mp3":
return "audio/mpeg";

break;
case ".wav":
return "audio/wav";

break;
case ".ogg":
return "audio/ogg";

break;
case ".m4a":
return "audio/mp4";

break;
case ".flac":
return "audio/flac";

break;
case ".aac":
return "audio/aac";

break;
case ".opus":
return "audio/opus";

break;
default:
return "audio/mpeg";

}
});
knoxx.backend.routes.studio.process_entry = (function knoxx$backend$routes$studio$process_entry(node_fs,node_path,root_dir,base_relative,depth,max_depth,entry){
var nm = entry.name;
var abs = node_path.join(root_dir,nm);
var rel = ((clojure.string.blank_QMARK_(base_relative))?nm:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_relative)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(nm)));
if(clojure.string.starts_with_QMARK_(nm,".")){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
if(cljs.core.truth_(entry.isDirectory())){
var G__52796 = node_fs;
var G__52797 = node_path;
var G__52798 = abs;
var G__52799 = rel;
var G__52800 = (depth + (1));
var G__52801 = max_depth;
return (knoxx.backend.routes.studio.walk_audio_files_BANG_.cljs$core$IFn$_invoke$arity$6 ? knoxx.backend.routes.studio.walk_audio_files_BANG_.cljs$core$IFn$_invoke$arity$6(G__52796,G__52797,G__52798,G__52799,G__52800,G__52801) : knoxx.backend.routes.studio.walk_audio_files_BANG_.call(null,G__52796,G__52797,G__52798,G__52799,G__52800,G__52801));
} else {
var ext = clojure.string.lower_case((function (){var or__5142__auto__ = (function (){var G__52802 = node_path.extname(nm);
if((G__52802 == null)){
return null;
} else {
return clojure.string.trim(G__52802);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
if(cljs.core.contains_QMARK_(knoxx.backend.routes.studio.audio_extensions(),ext)){
return node_fs.stat(abs).then((function (s){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"name","name",1843675177),nm,new cljs.core.Keyword(null,"path","path",-188191168),rel,new cljs.core.Keyword(null,"ext","ext",-996964541),ext,new cljs.core.Keyword(null,"size","size",1098693007),s.size,new cljs.core.Keyword(null,"modified","modified",-2134587826),s.mtime.getTime(),new cljs.core.Keyword(null,"mime","mime",-1846414642),knoxx.backend.routes.studio.audio_mime_type(ext)], null)], null);
})).catch((function (_){
return cljs.core.PersistentVector.EMPTY;
}));
} else {
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
}

}
}
});
knoxx.backend.routes.studio.walk_audio_files_BANG_ = (function knoxx$backend$routes$studio$walk_audio_files_BANG_(node_fs,node_path,root_dir,base_relative,depth,max_depth){
if((depth > max_depth)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return node_fs.readdir(root_dir,({"withFileTypes": true})).then((function (entries){
var promises = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__52804_SHARP_){
return knoxx.backend.routes.studio.process_entry(node_fs,node_path,root_dir,base_relative,depth,max_depth,p1__52804_SHARP_);
}),cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(entries)));
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(promises)).then((function (r){
return cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.identity,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([r], 0)));
}));
})).catch((function (_){
return cljs.core.PersistentVector.EMPTY;
}));
}
});
knoxx.backend.routes.studio.studio_audio_library_BANG_ = (function knoxx$backend$routes$studio$studio_audio_library_BANG_(app,runtime,config,deps){
var map__52827 = deps;
var map__52827__$1 = cljs.core.__destructure_map(map__52827);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52827__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52828 = app;
var G__52829 = "GET";
var G__52830 = "/api/studio/audio-library";
var G__52831 = (function (request,reply){
var G__52832 = runtime;
var G__52833 = request;
var G__52834 = reply;
var G__52835 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var subpath = (function (){var or__5142__auto__ = (request["query"]["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ".";
}
})();
var max_depth = (function (){var d = parseInt((function (){var or__5142__auto__ = (request["query"]["depth"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "16";
}
})(),(10));
if(cljs.core.truth_(isNaN(d))){
return (16);
} else {
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),cljs.core.min.cljs$core$IFn$_invoke$arity$2(d,(64)));
}
})();
var is_root = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(subpath,".")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(subpath,"")) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(subpath,"/")))));
var absolute = ((is_root)?new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config):(function (){var normalized = knoxx.backend.tools.media.normalize_tool_path_arg(subpath);
var map__52836 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52836__$1 = cljs.core.__destructure_map(map__52836);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52836__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
return absolute;
})());
var base_relative = ((is_root)?"":subpath);
return knoxx.backend.routes.studio.walk_audio_files_BANG_(shadow.esm.esm_import$node_fs$promises,shadow.esm.esm_import$node_path,absolute,base_relative,(0),max_depth).then((function (files){
var sorted = cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"modified","modified",-2134587826),cljs.core._GT_,files));
var G__52837 = reply;
var G__52838 = (200);
var G__52839 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"root","root",-448657453),subpath,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(sorted),new cljs.core.Keyword(null,"files","files",-472457450),sorted], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52837,G__52838,G__52839) : json_response_BANG_.call(null,G__52837,G__52838,G__52839));
})).catch((function (err){
var G__52840 = reply;
var G__52841 = (500);
var G__52842 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Scan failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52840,G__52841,G__52842) : json_response_BANG_.call(null,G__52840,G__52841,G__52842));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52832,G__52833,G__52834,G__52835) : with_request_context_BANG_.call(null,G__52832,G__52833,G__52834,G__52835));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52828,G__52829,G__52830,G__52831) : route_BANG_.call(null,G__52828,G__52829,G__52830,G__52831));
});
knoxx.backend.routes.studio.studio_state_get_BANG_ = (function knoxx$backend$routes$studio$studio_state_get_BANG_(app,runtime,config,deps){
var map__52845 = deps;
var map__52845__$1 = cljs.core.__destructure_map(map__52845);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52845__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52846 = app;
var G__52847 = "GET";
var G__52848 = "/api/studio/state";
var G__52849 = (function (request,reply){
var G__52850 = runtime;
var G__52851 = request;
var G__52852 = reply;
var G__52853 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(db)){
var user_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"user-id","user-id",-206822291).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52854 = ctx;
var G__52854__$1 = (((G__52854 == null))?null:new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(G__52854));
if((G__52854__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52854__$1);
}
}
})();
var org_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-id","org-id",1485182668).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52855 = ctx;
var G__52855__$1 = (((G__52855 == null))?null:new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(G__52855));
if((G__52855__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52855__$1);
}
}
})();
var kind = (function (){var or__5142__auto__ = (request["query"]["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "player";
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = user_id;
if(cljs.core.truth_(and__5140__auto__)){
return org_id;
} else {
return and__5140__auto__;
}
})())){
return db.query("SELECT state_json FROM studio_state WHERE user_id = $1 AND org_id = $2 AND kind = $3",[user_id,org_id,kind]).then((function (res){
var row = (function (){var G__52856 = res;
var G__52856__$1 = (((G__52856 == null))?null:G__52856.rows);
if((G__52856__$1 == null)){
return null;
} else {
return (G__52856__$1[(0)]);
}
})();
var G__52857 = reply;
var G__52858 = (200);
var G__52859 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"state","state",-1988618099),(cljs.core.truth_(row)?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(row.state_json,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):cljs.core.PersistentArrayMap.EMPTY)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52857,G__52858,G__52859) : json_response_BANG_.call(null,G__52857,G__52858,G__52859));
})).catch((function (err){
var G__52860 = reply;
var G__52861 = (500);
var G__52862 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Load failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52860,G__52861,G__52862) : json_response_BANG_.call(null,G__52860,G__52861,G__52862));
}));
} else {
var G__52863 = reply;
var G__52864 = (200);
var G__52865 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"state","state",-1988618099),cljs.core.PersistentArrayMap.EMPTY], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52863,G__52864,G__52865) : json_response_BANG_.call(null,G__52863,G__52864,G__52865));
}
} else {
var G__52866 = reply;
var G__52867 = (200);
var G__52868 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"state","state",-1988618099),cljs.core.PersistentArrayMap.EMPTY], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52866,G__52867,G__52868) : json_response_BANG_.call(null,G__52866,G__52867,G__52868));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52850,G__52851,G__52852,G__52853) : with_request_context_BANG_.call(null,G__52850,G__52851,G__52852,G__52853));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52846,G__52847,G__52848,G__52849) : route_BANG_.call(null,G__52846,G__52847,G__52848,G__52849));
});
knoxx.backend.routes.studio.studio_state_put_BANG_ = (function knoxx$backend$routes$studio$studio_state_put_BANG_(app,runtime,config,deps){
var map__52870 = deps;
var map__52870__$1 = cljs.core.__destructure_map(map__52870);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52870__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52871 = app;
var G__52872 = "PUT";
var G__52873 = "/api/studio/state";
var G__52874 = (function (request,reply){
var G__52875 = runtime;
var G__52876 = request;
var G__52877 = reply;
var G__52878 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(db)){
var user_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"user-id","user-id",-206822291).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52879 = ctx;
var G__52879__$1 = (((G__52879 == null))?null:new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(G__52879));
if((G__52879__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52879__$1);
}
}
})();
var org_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-id","org-id",1485182668).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52880 = ctx;
var G__52880__$1 = (((G__52880 == null))?null:new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(G__52880));
if((G__52880__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52880__$1);
}
}
})();
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var kind = (function (){var or__5142__auto__ = (body["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "player";
}
})();
var state = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (body["state"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_((function (){var and__5140__auto__ = user_id;
if(cljs.core.truth_(and__5140__auto__)){
return org_id;
} else {
return and__5140__auto__;
}
})())){
return db.query("INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,$3,$4::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW() RETURNING *",[user_id,org_id,kind,JSON.stringify(cljs.core.clj__GT_js(state))]).then((function (_){
var G__52881 = reply;
var G__52882 = (200);
var G__52883 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"saved","saved",288760660),true], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52881,G__52882,G__52883) : json_response_BANG_.call(null,G__52881,G__52882,G__52883));
})).catch((function (err){
var G__52884 = reply;
var G__52885 = (500);
var G__52886 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Save failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52884,G__52885,G__52886) : json_response_BANG_.call(null,G__52884,G__52885,G__52886));
}));
} else {
var G__52887 = reply;
var G__52888 = (400);
var G__52889 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"User context required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52887,G__52888,G__52889) : json_response_BANG_.call(null,G__52887,G__52888,G__52889));
}
} else {
var G__52890 = reply;
var G__52891 = (503);
var G__52892 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52890,G__52891,G__52892) : json_response_BANG_.call(null,G__52890,G__52891,G__52892));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52875,G__52876,G__52877,G__52878) : with_request_context_BANG_.call(null,G__52875,G__52876,G__52877,G__52878));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52871,G__52872,G__52873,G__52874) : route_BANG_.call(null,G__52871,G__52872,G__52873,G__52874));
});
knoxx.backend.routes.studio.studio_playlist_get_BANG_ = (function knoxx$backend$routes$studio$studio_playlist_get_BANG_(app,runtime,config,deps){
var map__52893 = deps;
var map__52893__$1 = cljs.core.__destructure_map(map__52893);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52893__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52894 = app;
var G__52895 = "GET";
var G__52896 = "/api/studio/playlist";
var G__52897 = (function (request,reply){
var G__52898 = runtime;
var G__52899 = request;
var G__52900 = reply;
var G__52901 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(db)){
var user_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"user-id","user-id",-206822291).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52902 = ctx;
var G__52902__$1 = (((G__52902 == null))?null:new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(G__52902));
if((G__52902__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52902__$1);
}
}
})();
var org_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-id","org-id",1485182668).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52904 = ctx;
var G__52904__$1 = (((G__52904 == null))?null:new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(G__52904));
if((G__52904__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52904__$1);
}
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = user_id;
if(cljs.core.truth_(and__5140__auto__)){
return org_id;
} else {
return and__5140__auto__;
}
})())){
return db.query("SELECT state_json FROM studio_state WHERE user_id=$1 AND org_id=$2 AND kind='playlist'",[user_id,org_id]).then((function (res){
var row = (function (){var G__52907 = res;
var G__52907__$1 = (((G__52907 == null))?null:G__52907.rows);
if((G__52907__$1 == null)){
return null;
} else {
return (G__52907__$1[(0)]);
}
})();
var state = (cljs.core.truth_(row)?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(row.state_json,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):cljs.core.PersistentArrayMap.EMPTY);
var G__52908 = reply;
var G__52909 = (200);
var G__52910 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"playlist","playlist",1952276871),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"items","items",1031954938).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52908,G__52909,G__52910) : json_response_BANG_.call(null,G__52908,G__52909,G__52910));
})).catch((function (err){
var G__52912 = reply;
var G__52913 = (500);
var G__52914 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Load failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52912,G__52913,G__52914) : json_response_BANG_.call(null,G__52912,G__52913,G__52914));
}));
} else {
var G__52917 = reply;
var G__52918 = (200);
var G__52919 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"playlist","playlist",1952276871),cljs.core.PersistentVector.EMPTY], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52917,G__52918,G__52919) : json_response_BANG_.call(null,G__52917,G__52918,G__52919));
}
} else {
var G__52921 = reply;
var G__52922 = (200);
var G__52923 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"playlist","playlist",1952276871),cljs.core.PersistentVector.EMPTY], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52921,G__52922,G__52923) : json_response_BANG_.call(null,G__52921,G__52922,G__52923));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52898,G__52899,G__52900,G__52901) : with_request_context_BANG_.call(null,G__52898,G__52899,G__52900,G__52901));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52894,G__52895,G__52896,G__52897) : route_BANG_.call(null,G__52894,G__52895,G__52896,G__52897));
});
knoxx.backend.routes.studio.studio_playlist_put_BANG_ = (function knoxx$backend$routes$studio$studio_playlist_put_BANG_(app,runtime,config,deps){
var map__52927 = deps;
var map__52927__$1 = cljs.core.__destructure_map(map__52927);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52927__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52928 = app;
var G__52929 = "PUT";
var G__52930 = "/api/studio/playlist";
var G__52931 = (function (request,reply){
var G__52932 = runtime;
var G__52933 = request;
var G__52934 = reply;
var G__52935 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(db)){
var user_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"user-id","user-id",-206822291).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52936 = ctx;
var G__52936__$1 = (((G__52936 == null))?null:new cljs.core.Keyword(null,"user","user",1532431356).cljs$core$IFn$_invoke$arity$1(G__52936));
if((G__52936__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52936__$1);
}
}
})();
var org_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org-id","org-id",1485182668).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__52937 = ctx;
var G__52937__$1 = (((G__52937 == null))?null:new cljs.core.Keyword(null,"org","org",1495985).cljs$core$IFn$_invoke$arity$1(G__52937));
if((G__52937__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__52937__$1);
}
}
})();
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (body["items"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_((function (){var and__5140__auto__ = user_id;
if(cljs.core.truth_(and__5140__auto__)){
return org_id;
} else {
return and__5140__auto__;
}
})())){
return db.query("INSERT INTO studio_state (user_id,org_id,kind,state_json) VALUES ($1,$2,'playlist',$3::jsonb) ON CONFLICT (user_id,org_id,kind) DO UPDATE SET state_json=EXCLUDED.state_json, updated_at=NOW()",[user_id,org_id,JSON.stringify(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"items","items",1031954938),items], null)))]).then((function (_){
var G__52938 = reply;
var G__52939 = (200);
var G__52940 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"saved","saved",288760660),true,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(items)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52938,G__52939,G__52940) : json_response_BANG_.call(null,G__52938,G__52939,G__52940));
})).catch((function (err){
var G__52941 = reply;
var G__52942 = (500);
var G__52943 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Save failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52941,G__52942,G__52943) : json_response_BANG_.call(null,G__52941,G__52942,G__52943));
}));
} else {
var G__52944 = reply;
var G__52945 = (400);
var G__52946 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"User context required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52944,G__52945,G__52946) : json_response_BANG_.call(null,G__52944,G__52945,G__52946));
}
} else {
var G__52947 = reply;
var G__52948 = (503);
var G__52949 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Database not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52947,G__52948,G__52949) : json_response_BANG_.call(null,G__52947,G__52948,G__52949));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52932,G__52933,G__52934,G__52935) : with_request_context_BANG_.call(null,G__52932,G__52933,G__52934,G__52935));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52928,G__52929,G__52930,G__52931) : route_BANG_.call(null,G__52928,G__52929,G__52930,G__52931));
});
knoxx.backend.routes.studio.studio_stream_BANG_ = (function knoxx$backend$routes$studio$studio_stream_BANG_(app,runtime,config,deps){
var map__52950 = deps;
var map__52950__$1 = cljs.core.__destructure_map(map__52950);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52950__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52951 = app;
var G__52952 = "GET";
var G__52953 = "/api/studio/stream";
var G__52954 = (function (request,reply){
var G__52955 = runtime;
var G__52956 = request;
var G__52957 = reply;
var G__52958 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var raw_path = (function (){var or__5142__auto__ = (request["query"]["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var normalized = knoxx.backend.tools.media.normalize_tool_path_arg(raw_path);
var map__52959 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52959__$1 = cljs.core.__destructure_map(map__52959);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52959__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52959__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var mime_type = (function (){var or__5142__auto__ = knoxx.backend.tools.media.workspace_media_mime_type(relative);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "audio/mpeg";
}
})();
return knoxx.backend.tools.media.fs_stat_BANG_(shadow.esm.esm_import$node_fs$promises,absolute).then((function (stat){
if(cljs.core.truth_(stat.isFile())){
} else {
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" is not a file")));
}

var total_size = stat.size;
var filename = knoxx.backend.tools.media.path_basename(shadow.esm.esm_import$node_path,absolute);
var safe_QMARK_ = cljs.core.every_QMARK_((function (c){
var n = c.charCodeAt((0));
return (((n >= (32))) && ((n <= (126))));
}),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)));
var disp = ((safe_QMARK_)?(""+"inline; filename=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)+"\""):(""+"inline; filename*=UTF-8''"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(filename))));
reply.header("Content-Type",mime_type);

reply.header("Accept-Ranges","bytes");

reply.header("Cache-Control","private, max-age=0");

reply.header("Content-Disposition",disp);

reply.header("Content-Length",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(total_size)));

return reply.send(shadow.esm.esm_import$node_fs.createReadStream(absolute));
})).catch((function (err){
var G__52969 = reply;
var G__52970 = (404);
var G__52971 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52969,G__52970,G__52971) : json_response_BANG_.call(null,G__52969,G__52970,G__52971));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52955,G__52956,G__52957,G__52958) : with_request_context_BANG_.call(null,G__52955,G__52956,G__52957,G__52958));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52951,G__52952,G__52953,G__52954) : route_BANG_.call(null,G__52951,G__52952,G__52953,G__52954));
});
knoxx.backend.routes.studio.studio_save_m3u_BANG_ = (function knoxx$backend$routes$studio$studio_save_m3u_BANG_(app,runtime,config,deps){
var map__52978 = deps;
var map__52978__$1 = cljs.core.__destructure_map(map__52978);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52978__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52980 = app;
var G__52981 = "POST";
var G__52982 = "/api/studio/save-m3u";
var G__52983 = (function (request,reply){
var G__52984 = runtime;
var G__52985 = request;
var G__52986 = reply;
var G__52987 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (body["items"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var name = (function (){var or__5142__auto__ = (body["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"playlist-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Date()).toISOString()));
}
})();
var m3u_lines = cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["#EXTM3U"], null),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (item){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"#EXTINF:-1,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(item))),new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(item)], null);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([items], 0)));
var m3u_content = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",m3u_lines);
var normalized = knoxx.backend.tools.media.normalize_tool_path_arg("Music/playlists");
var map__52988 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52988__$1 = cljs.core.__destructure_map(map__52988);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52988__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var safe_name = clojure.string.replace(name,/[^a-zA-Z0-9_-]/,"_");
var file_path = shadow.esm.esm_import$node_path.join(absolute,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(safe_name)+".m3u"));
return shadow.esm.esm_import$node_fs$promises.mkdir(absolute,({"recursive": true})).then((function (_){
return shadow.esm.esm_import$node_fs$promises.writeFile(file_path,m3u_content,"utf8");
})).then((function (_){
var G__52992 = reply;
var G__52993 = (200);
var G__52994 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),(""+"Music/playlists/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(safe_name)+".m3u"),new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(items)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52992,G__52993,G__52994) : json_response_BANG_.call(null,G__52992,G__52993,G__52994));
})).catch((function (err){
var G__52995 = reply;
var G__52996 = (500);
var G__52997 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to save playlist: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52995,G__52996,G__52997) : json_response_BANG_.call(null,G__52995,G__52996,G__52997));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52984,G__52985,G__52986,G__52987) : with_request_context_BANG_.call(null,G__52984,G__52985,G__52986,G__52987));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52980,G__52981,G__52982,G__52983) : route_BANG_.call(null,G__52980,G__52981,G__52982,G__52983));
});
knoxx.backend.routes.studio.studio_save_m3u_download_BANG_ = (function knoxx$backend$routes$studio$studio_save_m3u_download_BANG_(app,runtime,config,deps){
var map__52998 = deps;
var map__52998__$1 = cljs.core.__destructure_map(map__52998);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52998__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__52999 = app;
var G__53000 = "POST";
var G__53001 = "/api/studio/download-m3u";
var G__53002 = (function (request,reply){
var G__53003 = runtime;
var G__53004 = request;
var G__53005 = reply;
var G__53006 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var items = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (body["items"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var name = (function (){var or__5142__auto__ = (body["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "playlist";
}
})();
var m3u_lines = cljs.core.concat.cljs$core$IFn$_invoke$arity$2(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["#EXTM3U"], null),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (item){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"#EXTINF:-1,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(item))),new cljs.core.Keyword(null,"path","path",-188191168).cljs$core$IFn$_invoke$arity$1(item)], null);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([items], 0)));
var m3u_content = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",m3u_lines);
var safe_name = clojure.string.replace(name,/[^a-zA-Z0-9_-]/,"_");
reply.header("Content-Type","audio/x-mpegurl");

reply.header("Content-Disposition",(""+"attachment; filename=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(safe_name)+".m3u\""));

return reply.send(m3u_content);
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53003,G__53004,G__53005,G__53006) : with_request_context_BANG_.call(null,G__53003,G__53004,G__53005,G__53006));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52999,G__53000,G__53001,G__53002) : route_BANG_.call(null,G__52999,G__53000,G__53001,G__53002));
});
knoxx.backend.routes.studio.studio_load_m3u_BANG_ = (function knoxx$backend$routes$studio$studio_load_m3u_BANG_(app,runtime,config,deps){
var map__53010 = deps;
var map__53010__$1 = cljs.core.__destructure_map(map__53010);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53010__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53011 = app;
var G__53012 = "GET";
var G__53013 = "/api/studio/load-m3u";
var G__53014 = (function (request,reply){
var G__53016 = runtime;
var G__53017 = request;
var G__53018 = reply;
var G__53019 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var file_path = (request["query"]["path"]);
if(cljs.core.not(file_path)){
var G__53020 = reply;
var G__53021 = (400);
var G__53022 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path parameter"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53020,G__53021,G__53022) : json_response_BANG_.call(null,G__53020,G__53021,G__53022));
} else {
return shadow.esm.esm_import$node_fs$promises.readFile(file_path,"utf8").then((function (content){
var lines = clojure.string.split_lines(content);
var items = (function (){var remaining = lines;
var result = cljs.core.PersistentVector.EMPTY;
var current_name = null;
while(true){
if(cljs.core.empty_QMARK_(remaining)){
return result;
} else {
var line = clojure.string.trim(cljs.core.first(remaining));
var rest_lines = cljs.core.rest(remaining);
if(((clojure.string.blank_QMARK_(line)) || (clojure.string.starts_with_QMARK_(line,"#EXTM3U")))){
var G__53384 = rest_lines;
var G__53385 = result;
var G__53386 = current_name;
remaining = G__53384;
result = G__53385;
current_name = G__53386;
continue;
} else {
if(clojure.string.starts_with_QMARK_(line,"#EXTINF:")){
var name_part = cljs.core.second(clojure.string.split.cljs$core$IFn$_invoke$arity$3(line,/,/,(2)));
var G__53387 = rest_lines;
var G__53388 = result;
var G__53389 = (function (){var or__5142__auto__ = name_part;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Unknown";
}
})();
remaining = G__53387;
result = G__53388;
current_name = G__53389;
continue;
} else {
var G__53390 = rest_lines;
var G__53391 = cljs.core.conj.cljs$core$IFn$_invoke$arity$2(result,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"path","path",-188191168),line,new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = current_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_path.basename(line);
}
})()], null));
var G__53392 = null;
remaining = G__53390;
result = G__53391;
current_name = G__53392;
continue;

}
}
}
break;
}
})();
var playlist_name = shadow.esm.esm_import$node_path.basename(file_path);
var clean_name = clojure.string.replace(playlist_name,/\.m3u$/,"");
var G__53030 = reply;
var G__53031 = (200);
var G__53032 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"name","name",1843675177),clean_name,new cljs.core.Keyword(null,"items","items",1031954938),cljs.core.vec(items)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53030,G__53031,G__53032) : json_response_BANG_.call(null,G__53030,G__53031,G__53032));
})).catch((function (err){
var G__53034 = reply;
var G__53035 = (500);
var G__53036 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to load M3U: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53034,G__53035,G__53036) : json_response_BANG_.call(null,G__53034,G__53035,G__53036));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53016,G__53017,G__53018,G__53019) : with_request_context_BANG_.call(null,G__53016,G__53017,G__53018,G__53019));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53011,G__53012,G__53013,G__53014) : route_BANG_.call(null,G__53011,G__53012,G__53013,G__53014));
});
knoxx.backend.routes.studio.studio_list_playlists_BANG_ = (function knoxx$backend$routes$studio$studio_list_playlists_BANG_(app,runtime,config,deps){
var map__53040 = deps;
var map__53040__$1 = cljs.core.__destructure_map(map__53040);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53040__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53042 = app;
var G__53043 = "GET";
var G__53044 = "/api/studio/playlists";
var G__53045 = (function (request,reply){
var G__53046 = runtime;
var G__53047 = request;
var G__53048 = reply;
var G__53049 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var playlists_dir = shadow.esm.esm_import$node_path.join(workspace_root,"Music","playlists");
return shadow.esm.esm_import$node_fs$promises.readdir(playlists_dir).then((function (files){
var m3u_files = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (filename){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),clojure.string.replace(filename,/\.m3u$/,""),new cljs.core.Keyword(null,"path","path",-188191168),shadow.esm.esm_import$node_path.join(playlists_dir,filename),new cljs.core.Keyword(null,"filename","filename",-1428840783),filename], null);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__53039_SHARP_){
return clojure.string.ends_with_QMARK_(p1__53039_SHARP_,".m3u");
}),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(files)));
var G__53051 = reply;
var G__53052 = (200);
var G__53053 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"playlists","playlists",861847789),cljs.core.vec(m3u_files)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53051,G__53052,G__53053) : json_response_BANG_.call(null,G__53051,G__53052,G__53053));
})).catch((function (_err){
var G__53054 = reply;
var G__53055 = (200);
var G__53056 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"playlists","playlists",861847789),cljs.core.PersistentVector.EMPTY], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53054,G__53055,G__53056) : json_response_BANG_.call(null,G__53054,G__53055,G__53056));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53046,G__53047,G__53048,G__53049) : with_request_context_BANG_.call(null,G__53046,G__53047,G__53048,G__53049));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53042,G__53043,G__53044,G__53045) : route_BANG_.call(null,G__53042,G__53043,G__53044,G__53045));
});
knoxx.backend.routes.studio.studio_labels_get_BANG_ = (function knoxx$backend$routes$studio$studio_labels_get_BANG_(app,runtime,config,deps){
var map__53061 = deps;
var map__53061__$1 = cljs.core.__destructure_map(map__53061);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53061__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53062 = app;
var G__53063 = "GET";
var G__53064 = "/api/studio/labels";
var G__53065 = (function (request,reply){
var G__53067 = runtime;
var G__53068 = request;
var G__53069 = reply;
var G__53070 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var file_path = (request["query"]["path"]);
var all_QMARK_ = (request["query"]["all"]);
if(cljs.core.truth_(all_QMARK_)){
return knoxx.backend.audio_labels.get_all_labels(shadow.esm.esm_import$node_fs$promises,workspace_root).then((function (all_labels){
var G__53075 = reply;
var G__53076 = (200);
var G__53077 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"labels","labels",-626734591),all_labels], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53075,G__53076,G__53077) : json_response_BANG_.call(null,G__53075,G__53076,G__53077));
})).catch((function (err){
var G__53078 = reply;
var G__53079 = (500);
var G__53080 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53078,G__53079,G__53080) : json_response_BANG_.call(null,G__53078,G__53079,G__53080));
}));
} else {
if(cljs.core.truth_(file_path)){
return knoxx.backend.audio_labels.get_labels(shadow.esm.esm_import$node_fs$promises,workspace_root,file_path).then((function (file_labels){
var G__53081 = reply;
var G__53082 = (200);
var G__53083 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),file_path,new cljs.core.Keyword(null,"labels","labels",-626734591),file_labels], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53081,G__53082,G__53083) : json_response_BANG_.call(null,G__53081,G__53082,G__53083));
})).catch((function (err){
var G__53084 = reply;
var G__53085 = (500);
var G__53086 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53084,G__53085,G__53086) : json_response_BANG_.call(null,G__53084,G__53085,G__53086));
}));
} else {
var G__53087 = reply;
var G__53088 = (400);
var G__53089 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path or all parameter"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53087,G__53088,G__53089) : json_response_BANG_.call(null,G__53087,G__53088,G__53089));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53067,G__53068,G__53069,G__53070) : with_request_context_BANG_.call(null,G__53067,G__53068,G__53069,G__53070));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53062,G__53063,G__53064,G__53065) : route_BANG_.call(null,G__53062,G__53063,G__53064,G__53065));
});
knoxx.backend.routes.studio.studio_labels_add_BANG_ = (function knoxx$backend$routes$studio$studio_labels_add_BANG_(app,runtime,config,deps){
var map__53090 = deps;
var map__53090__$1 = cljs.core.__destructure_map(map__53090);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53090__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53095 = app;
var G__53096 = "POST";
var G__53097 = "/api/studio/labels/add";
var G__53098 = (function (request,reply){
var G__53099 = runtime;
var G__53100 = request;
var G__53101 = reply;
var G__53102 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var file_path = (body["path"]);
var label = (body["label"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = file_path;
if(cljs.core.truth_(and__5140__auto__)){
return label;
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.audio_labels.add_label_BANG_(shadow.esm.esm_import$node_fs$promises,workspace_root,file_path,label).then((function (updated){
var G__53104 = reply;
var G__53105 = (200);
var G__53106 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),file_path,new cljs.core.Keyword(null,"labels","labels",-626734591),updated], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53104,G__53105,G__53106) : json_response_BANG_.call(null,G__53104,G__53105,G__53106));
})).catch((function (err){
var G__53107 = reply;
var G__53108 = (500);
var G__53109 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53107,G__53108,G__53109) : json_response_BANG_.call(null,G__53107,G__53108,G__53109));
}));
} else {
var G__53111 = reply;
var G__53112 = (400);
var G__53113 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path or label"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53111,G__53112,G__53113) : json_response_BANG_.call(null,G__53111,G__53112,G__53113));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53099,G__53100,G__53101,G__53102) : with_request_context_BANG_.call(null,G__53099,G__53100,G__53101,G__53102));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53095,G__53096,G__53097,G__53098) : route_BANG_.call(null,G__53095,G__53096,G__53097,G__53098));
});
knoxx.backend.routes.studio.studio_labels_remove_BANG_ = (function knoxx$backend$routes$studio$studio_labels_remove_BANG_(app,runtime,config,deps){
var map__53115 = deps;
var map__53115__$1 = cljs.core.__destructure_map(map__53115);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53115__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53118 = app;
var G__53119 = "POST";
var G__53120 = "/api/studio/labels/remove";
var G__53121 = (function (request,reply){
var G__53123 = runtime;
var G__53124 = request;
var G__53125 = reply;
var G__53126 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var file_path = (body["path"]);
var label = (body["label"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = file_path;
if(cljs.core.truth_(and__5140__auto__)){
return label;
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.audio_labels.remove_label_BANG_(shadow.esm.esm_import$node_fs$promises,workspace_root,file_path,label).then((function (updated){
var G__53131 = reply;
var G__53132 = (200);
var G__53133 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),file_path,new cljs.core.Keyword(null,"labels","labels",-626734591),updated], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53131,G__53132,G__53133) : json_response_BANG_.call(null,G__53131,G__53132,G__53133));
})).catch((function (err){
var G__53135 = reply;
var G__53136 = (500);
var G__53137 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53135,G__53136,G__53137) : json_response_BANG_.call(null,G__53135,G__53136,G__53137));
}));
} else {
var G__53138 = reply;
var G__53139 = (400);
var G__53140 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path or label"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53138,G__53139,G__53140) : json_response_BANG_.call(null,G__53138,G__53139,G__53140));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53123,G__53124,G__53125,G__53126) : with_request_context_BANG_.call(null,G__53123,G__53124,G__53125,G__53126));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53118,G__53119,G__53120,G__53121) : route_BANG_.call(null,G__53118,G__53119,G__53120,G__53121));
});
knoxx.backend.routes.studio.studio_labels_by_label_BANG_ = (function knoxx$backend$routes$studio$studio_labels_by_label_BANG_(app,runtime,config,deps){
var map__53141 = deps;
var map__53141__$1 = cljs.core.__destructure_map(map__53141);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53141__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53144 = app;
var G__53145 = "GET";
var G__53146 = "/api/studio/labels/by-label";
var G__53147 = (function (request,reply){
var G__53148 = runtime;
var G__53149 = request;
var G__53150 = reply;
var G__53151 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var label = (request["query"]["label"]);
if(cljs.core.truth_(label)){
return knoxx.backend.audio_labels.get_files_by_label(shadow.esm.esm_import$node_fs$promises,workspace_root,label).then((function (files){
var G__53154 = reply;
var G__53155 = (200);
var G__53156 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"label","label",1718410804),label,new cljs.core.Keyword(null,"files","files",-472457450),files], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53154,G__53155,G__53156) : json_response_BANG_.call(null,G__53154,G__53155,G__53156));
})).catch((function (err){
var G__53158 = reply;
var G__53159 = (500);
var G__53160 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53158,G__53159,G__53160) : json_response_BANG_.call(null,G__53158,G__53159,G__53160));
}));
} else {
var G__53161 = reply;
var G__53162 = (400);
var G__53163 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing label parameter"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53161,G__53162,G__53163) : json_response_BANG_.call(null,G__53161,G__53162,G__53163));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53148,G__53149,G__53150,G__53151) : with_request_context_BANG_.call(null,G__53148,G__53149,G__53150,G__53151));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53144,G__53145,G__53146,G__53147) : route_BANG_.call(null,G__53144,G__53145,G__53146,G__53147));
});
knoxx.backend.routes.studio.studio_sync_symlinks_BANG_ = (function knoxx$backend$routes$studio$studio_sync_symlinks_BANG_(app,runtime,config,deps){
var map__53174 = deps;
var map__53174__$1 = cljs.core.__destructure_map(map__53174);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53174__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53178 = app;
var G__53179 = "POST";
var G__53180 = "/api/studio/sync-symlinks";
var G__53181 = (function (request,reply){
var G__53183 = runtime;
var G__53184 = request;
var G__53185 = reply;
var G__53186 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
return knoxx.backend.audio_labels.sync_symlinks_BANG_(shadow.esm.esm_import$node_fs$promises,shadow.esm.esm_import$node_path,workspace_root).then((function (count){
var G__53190 = reply;
var G__53191 = (200);
var G__53192 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"symlinks","symlinks",2086981352),count], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53190,G__53191,G__53192) : json_response_BANG_.call(null,G__53190,G__53191,G__53192));
})).catch((function (err){
var G__53195 = reply;
var G__53197 = (500);
var G__53198 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to sync symlinks: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53195,G__53197,G__53198) : json_response_BANG_.call(null,G__53195,G__53197,G__53198));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53183,G__53184,G__53185,G__53186) : with_request_context_BANG_.call(null,G__53183,G__53184,G__53185,G__53186));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53178,G__53179,G__53180,G__53181) : route_BANG_.call(null,G__53178,G__53179,G__53180,G__53181));
});
knoxx.backend.routes.studio.studio_audio_asset_get_BANG_ = (function knoxx$backend$routes$studio$studio_audio_asset_get_BANG_(app,runtime,config,deps){
var map__53206 = deps;
var map__53206__$1 = cljs.core.__destructure_map(map__53206);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53206__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53213 = app;
var G__53214 = "GET";
var G__53215 = "/api/studio/audio-asset";
var G__53216 = (function (request,reply){
var G__53217 = runtime;
var G__53218 = request;
var G__53219 = reply;
var G__53220 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
var audio_path = (request["query"]["path"]);
var asset_type = (request["query"]["type"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = audio_path;
if(cljs.core.truth_(and__5140__auto__)){
return asset_type;
} else {
return and__5140__auto__;
}
})())){
return db.query("SELECT image_data, mime_type, width, height FROM studio_audio_assets WHERE audio_path = $1 AND asset_type = $2",[audio_path,asset_type]).then((function (res){
if(cljs.core.truth_(res.rows)){
var row = cljs.core.first(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(res.rows));
if(cljs.core.truth_(row)){
reply.header("Content-Type",cljs.core.get.cljs$core$IFn$_invoke$arity$3(row,"mime_type","image/png"));

reply.header("Cache-Control","public, max-age=86400");

return reply.send((row["image_data"]));
} else {
var G__53230 = reply;
var G__53231 = (404);
var G__53232 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Asset not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53230,G__53231,G__53232) : json_response_BANG_.call(null,G__53230,G__53231,G__53232));
}
} else {
var G__53233 = reply;
var G__53234 = (404);
var G__53235 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Asset not found"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53233,G__53234,G__53235) : json_response_BANG_.call(null,G__53233,G__53234,G__53235));
}
})).catch((function (err){
var G__53236 = reply;
var G__53237 = (500);
var G__53238 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53236,G__53237,G__53238) : json_response_BANG_.call(null,G__53236,G__53237,G__53238));
}));
} else {
var G__53239 = reply;
var G__53240 = (400);
var G__53241 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path or type"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53239,G__53240,G__53241) : json_response_BANG_.call(null,G__53239,G__53240,G__53241));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53217,G__53218,G__53219,G__53220) : with_request_context_BANG_.call(null,G__53217,G__53218,G__53219,G__53220));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53213,G__53214,G__53215,G__53216) : route_BANG_.call(null,G__53213,G__53214,G__53215,G__53216));
});
knoxx.backend.routes.studio.studio_audio_asset_save_BANG_ = (function knoxx$backend$routes$studio$studio_audio_asset_save_BANG_(app,runtime,config,deps){
var map__53247 = deps;
var map__53247__$1 = cljs.core.__destructure_map(map__53247);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__53247__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__53252 = app;
var G__53253 = "POST";
var G__53254 = "/api/studio/audio-asset";
var G__53255 = (function (request,reply){
var G__53256 = runtime;
var G__53257 = request;
var G__53258 = reply;
var G__53259 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.chat.use") : ensure_permission_BANG_.call(null,ctx,"agent.chat.use"));
} else {
}

var db = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var audio_path = (body["path"]);
var asset_type = (body["type"]);
var image_data = (body["imageData"]);
var mime_type = (function (){var or__5142__auto__ = (body["mimeType"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "image/png";
}
})();
var width = (body["width"]);
var height = (body["height"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = audio_path;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = asset_type;
if(cljs.core.truth_(and__5140__auto____$1)){
return image_data;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var buffer = Buffer.from(image_data,"base64");
return db.query("INSERT INTO studio_audio_assets (audio_path, asset_type, image_data, mime_type, width, height) VALUES ($1, $2, $3, $4, $5, $6) ON CONFLICT (audio_path, asset_type) DO UPDATE SET image_data = $3, mime_type = $4, width = $5, height = $6, created_at = NOW()",[audio_path,asset_type,buffer,mime_type,width,height]).then((function (_){
var G__53260 = reply;
var G__53261 = (200);
var G__53262 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),audio_path,new cljs.core.Keyword(null,"type","type",1174270348),asset_type], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53260,G__53261,G__53262) : json_response_BANG_.call(null,G__53260,G__53261,G__53262));
})).catch((function (err){
var G__53278 = reply;
var G__53279 = (500);
var G__53280 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53278,G__53279,G__53280) : json_response_BANG_.call(null,G__53278,G__53279,G__53280));
}));
} else {
var G__53282 = reply;
var G__53283 = (400);
var G__53284 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing path, type, or imageData"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__53282,G__53283,G__53284) : json_response_BANG_.call(null,G__53282,G__53283,G__53284));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__53256,G__53257,G__53258,G__53259) : with_request_context_BANG_.call(null,G__53256,G__53257,G__53258,G__53259));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__53252,G__53253,G__53254,G__53255) : route_BANG_.call(null,G__53252,G__53253,G__53254,G__53255));
});
knoxx.backend.routes.studio.register_studio_routes_BANG_ = (function knoxx$backend$routes$studio$register_studio_routes_BANG_(app,runtime,config,deps){
knoxx.backend.routes.studio.studio_audio_library_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_state_get_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_state_put_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_playlist_get_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_playlist_put_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_stream_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_save_m3u_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_save_m3u_download_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_load_m3u_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_list_playlists_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_labels_get_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_labels_add_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_labels_remove_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_labels_by_label_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_sync_symlinks_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_audio_asset_get_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.studio_audio_asset_save_BANG_(app,runtime,config,deps);

knoxx.backend.routes.studio.discord_scan.studio_discord_audio_scan_BANG_(app,runtime,config,deps);

return knoxx.backend.routes.studio.discord_scan.studio_discord_image_scan_BANG_(app,runtime,config,deps);
});

//# sourceMappingURL=knoxx.backend.routes.studio.js.map
