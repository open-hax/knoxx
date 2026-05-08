import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./promesa.core.js";
import "./knoxx.backend.tools.media.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.routes.workspace_media');
knoxx.backend.routes.workspace_media.reply_header_BANG_ = (function knoxx$backend$routes$workspace_media$reply_header_BANG_(reply,name,value){
return reply.header(name,value);
});
knoxx.backend.routes.workspace_media.request_header = (function knoxx$backend$routes$workspace_media$request_header(request,name){
var headers = (request["headers"]);
if(cljs.core.truth_(headers)){
return (headers[name]);
} else {
return null;
}
});
/**
 * Build a Content-Disposition header value that handles non-ASCII filenames.
 * Uses RFC 5987 encoding for filenames with special characters.
 */
knoxx.backend.routes.workspace_media.safe_content_disposition = (function knoxx$backend$routes$workspace_media$safe_content_disposition(filename){
var ascii_safe_QMARK_ = cljs.core.every_QMARK_((function (p1__52328_SHARP_){
var c = p1__52328_SHARP_.charCodeAt((0));
return (((c >= (32))) && ((((c <= (126))) && (((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(c,(34))) && (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(c,(92))))))));
}),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)));
if(ascii_safe_QMARK_){
return (""+"inline; filename=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)+"\"");
} else {
var encoded = encodeURIComponent(filename);
return (""+"inline; filename*=UTF-8''"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encoded));
}
});
knoxx.backend.routes.workspace_media.mime_type_for_path = (function knoxx$backend$routes$workspace_media$mime_type_for_path(relative,absolute){
var or__5142__auto__ = knoxx.backend.tools.media.workspace_media_mime_type(relative);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.media.workspace_media_mime_type(absolute);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "application/octet-stream";
}
}
});
/**
 * Parse a Range header of the form: bytes=start-end.
 * Returns {:start n :end n :length n} or nil when invalid/unsupported.
 */
knoxx.backend.routes.workspace_media.parse_range_header = (function knoxx$backend$routes$workspace_media$parse_range_header(range_header,total_size){
var raw = (function (){var G__52343 = range_header;
var G__52343__$1 = (((G__52343 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__52343)));
if((G__52343__$1 == null)){
return null;
} else {
return clojure.string.trim(G__52343__$1);
}
})();
var match = (cljs.core.truth_((function (){var and__5140__auto__ = raw;
if(cljs.core.truth_(and__5140__auto__)){
return clojure.string.starts_with_QMARK_(raw,"bytes=");
} else {
return and__5140__auto__;
}
})())?raw.match(/^bytes=(\\d*)-(\\d*)$/):null);
if(cljs.core.truth_(match)){
var start_str = (match[(1)]);
var end_str = (match[(2)]);
var start = (cljs.core.truth_((function (){var and__5140__auto__ = start_str;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(start_str)));
} else {
return and__5140__auto__;
}
})())?parseInt(start_str,(10)):null);
var end = (cljs.core.truth_((function (){var and__5140__auto__ = end_str;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(end_str)));
} else {
return and__5140__auto__;
}
})())?parseInt(end_str,(10)):null);
var start_STAR_ = ((((typeof start === 'number') && (cljs.core.not(isNaN(start)))))?start:(((((start == null)) && (((typeof end === 'number') && (cljs.core.not(isNaN(end)))))))?cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),(total_size - end)):null
));
var end_STAR_ = ((((typeof end === 'number') && (cljs.core.not(isNaN(end)))))?cljs.core.min.cljs$core$IFn$_invoke$arity$2((total_size - (1)),end):(total_size - (1))
);
if(((typeof start_STAR_ === 'number') && (((((0) <= start_STAR_)) && ((((start_STAR_ < total_size)) && ((start_STAR_ <= end_STAR_)))))))){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"start","start",-355208981),start_STAR_,new cljs.core.Keyword(null,"end","end",-268185958),end_STAR_,new cljs.core.Keyword(null,"length","length",588987862),((end_STAR_ - start_STAR_) + (1))], null);
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.routes.workspace_media.register_workspace_media_routes_BANG_ = (function knoxx$backend$routes$workspace_media$register_workspace_media_routes_BANG_(app,runtime,config,p__52364){
var map__52365 = p__52364;
var map__52365__$1 = cljs.core.__destructure_map(map__52365);
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52365__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52365__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52365__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52365__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var ensure_tool_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52365__$1,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334));
var G__52367 = app;
var G__52368 = "GET";
var G__52369 = "/api/workspace-media/raw";
var G__52370 = (function (request,reply){
var G__52371 = runtime;
var G__52372 = request;
var G__52373 = reply;
var G__52374 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"read") : ensure_tool_BANG_.call(null,ctx,"read"));
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
var map__52376 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52376__$1 = cljs.core.__destructure_map(map__52376);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52376__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52376__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var mime_type = knoxx.backend.routes.workspace_media.mime_type_for_path(relative,absolute);
var range_header = (function (){var or__5142__auto__ = knoxx.backend.routes.workspace_media.request_header(request,"range");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.routes.workspace_media.request_header(request,"Range");
}
})();
return knoxx.backend.tools.media.fs_stat_BANG_(shadow.esm.esm_import$node_fs$promises,absolute).then((function (stat){
if(cljs.core.truth_(stat.isFile())){
} else {
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" is not a file")));
}

var total_size = stat.size;
var range = (cljs.core.truth_((function (){var and__5140__auto__ = range_header;
if(cljs.core.truth_(and__5140__auto__)){
return (total_size > (0));
} else {
return and__5140__auto__;
}
})())?knoxx.backend.routes.workspace_media.parse_range_header(range_header,total_size):null);
var filename = knoxx.backend.tools.media.path_basename(shadow.esm.esm_import$node_path,absolute);
knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Content-Type",mime_type);

knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Accept-Ranges","bytes");

knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Cache-Control","private, max-age=0");

knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Content-Disposition",knoxx.backend.routes.workspace_media.safe_content_disposition(filename));

if(cljs.core.truth_(range)){
var map__52379 = range;
var map__52379__$1 = cljs.core.__destructure_map(map__52379);
var start = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52379__$1,new cljs.core.Keyword(null,"start","start",-355208981));
var end = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52379__$1,new cljs.core.Keyword(null,"end","end",-268185958));
var length = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52379__$1,new cljs.core.Keyword(null,"length","length",588987862));
var stream = shadow.esm.esm_import$node_fs.createReadStream(absolute,({"start": start, "end": end}));
knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Content-Range",(""+"bytes "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(start)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(end)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(total_size)));

knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Content-Length",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(length)));

reply.code((206));

return reply.send(stream);
} else {
var stream = shadow.esm.esm_import$node_fs.createReadStream(absolute);
knoxx.backend.routes.workspace_media.reply_header_BANG_(reply,"Content-Length",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(total_size)));

reply.code((200));

return reply.send(stream);
}
})).catch((function (err){
var G__52381 = reply;
var G__52382 = (404);
var G__52383 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52381,G__52382,G__52383) : json_response_BANG_.call(null,G__52381,G__52382,G__52383));
}));
}catch (e52375){var err = e52375;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52371,G__52372,G__52373,G__52374) : with_request_context_BANG_.call(null,G__52371,G__52372,G__52373,G__52374));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52367,G__52368,G__52369,G__52370) : route_BANG_.call(null,G__52367,G__52368,G__52369,G__52370));
});
/**
 * Set of recognized audio file extensions.
 */
knoxx.backend.routes.workspace_media.audio_extensions = (function knoxx$backend$routes$workspace_media$audio_extensions(){
return new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 8, [".flac",null,".mp3",null,".ogg",null,".m4a",null,".wma",null,".wav",null,".aac",null,".opus",null], null), null);
});
knoxx.backend.routes.workspace_media.audio_mime_type = (function knoxx$backend$routes$workspace_media$audio_mime_type(ext){
var G__52384 = ext;
switch (G__52384) {
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
/**
 * Recursively walk a directory and collect audio file metadata.
 */
knoxx.backend.routes.workspace_media.walk_audio_files_BANG_ = (function knoxx$backend$routes$workspace_media$walk_audio_files_BANG_(root_dir,base_relative,depth,max_depth){
if((depth > max_depth)){
return promesa.core.resolved(cljs.core.PersistentVector.EMPTY);
} else {
return shadow.esm.esm_import$node_fs$promises.readdir(root_dir,({"withFileTypes": true})).then((function (entries){
var entries_arr = cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(entries));
var promises = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (entry){
var name = entry.name;
var abs_path = shadow.esm.esm_import$node_path.join(root_dir,name);
var rel_path = ((clojure.string.blank_QMARK_(base_relative))?name:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_relative)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)));
if(clojure.string.starts_with_QMARK_(name,".")){
return promesa.core.resolved(cljs.core.PersistentVector.EMPTY);
} else {
if(cljs.core.truth_(entry.isDirectory())){
var G__52396 = abs_path;
var G__52397 = rel_path;
var G__52398 = (depth + (1));
var G__52399 = max_depth;
return (knoxx.backend.routes.workspace_media.walk_audio_files_BANG_.cljs$core$IFn$_invoke$arity$4 ? knoxx.backend.routes.workspace_media.walk_audio_files_BANG_.cljs$core$IFn$_invoke$arity$4(G__52396,G__52397,G__52398,G__52399) : knoxx.backend.routes.workspace_media.walk_audio_files_BANG_.call(null,G__52396,G__52397,G__52398,G__52399));
} else {
var ext = clojure.string.lower_case((function (){var or__5142__auto__ = (function (){var G__52401 = shadow.esm.esm_import$node_path.extname(name);
if((G__52401 == null)){
return null;
} else {
return clojure.string.trim(G__52401);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
if(cljs.core.contains_QMARK_(knoxx.backend.routes.workspace_media.audio_extensions(),ext)){
return shadow.esm.esm_import$node_fs$promises.stat(abs_path).then((function (stat){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"path","path",-188191168),rel_path,new cljs.core.Keyword(null,"ext","ext",-996964541),ext,new cljs.core.Keyword(null,"size","size",1098693007),stat.size,new cljs.core.Keyword(null,"modified","modified",-2134587826),stat.mtime.getTime(),new cljs.core.Keyword(null,"mime","mime",-1846414642),knoxx.backend.routes.workspace_media.audio_mime_type(ext)], null)], null);
})).catch((function (_){
return cljs.core.PersistentVector.EMPTY;
}));
} else {
return promesa.core.resolved(cljs.core.PersistentVector.EMPTY);
}

}
}
}),entries_arr);
return promesa.core.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(promises)).then((function (results){
return cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.identity,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([results], 0)));
}));
})).catch((function (_){
return cljs.core.PersistentVector.EMPTY;
}));
}
});
knoxx.backend.routes.workspace_media.register_workspace_media_routes = (function knoxx$backend$routes$workspace_media$register_workspace_media_routes(app,runtime,config,handlers){
return knoxx.backend.routes.workspace_media.register_workspace_media_routes_BANG_(app,runtime,config,handlers);
});
/**
 * Routes for the broadcast studio audio library.
 */
knoxx.backend.routes.workspace_media.register_audio_library_routes_BANG_ = (function knoxx$backend$routes$workspace_media$register_audio_library_routes_BANG_(app,runtime,config,p__52423){
var map__52424 = p__52423;
var map__52424__$1 = cljs.core.__destructure_map(map__52424);
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52424__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52424__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52424__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52424__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var ensure_tool_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52424__$1,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334));
var G__52426_52642 = app;
var G__52427_52643 = "GET";
var G__52428_52644 = "/api/workspace-media/audio-library";
var G__52429_52645 = (function (request,reply){
var G__52430 = runtime;
var G__52431 = request;
var G__52432 = reply;
var G__52433 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"read") : ensure_tool_BANG_.call(null,ctx,"read"));
} else {
}

var subpath = (function (){var or__5142__auto__ = (request["query"]["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var max_depth = (function (){var d = parseInt((function (){var or__5142__auto__ = (request["query"]["depth"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "3";
}
})(),(10));
if(cljs.core.truth_(isNaN(d))){
return (3);
} else {
return cljs.core.min.cljs$core$IFn$_invoke$arity$2(d,(8));
}
})();
var scan_root = ((clojure.string.blank_QMARK_(subpath))?new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config):(function (){var normalized = knoxx.backend.tools.media.normalize_tool_path_arg(subpath);
var map__52435 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52435__$1 = cljs.core.__destructure_map(map__52435);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52435__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
return absolute;
})());
var rel_base = clojure.string.trim((function (){var or__5142__auto__ = subpath;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
return knoxx.backend.routes.workspace_media.walk_audio_files_BANG_(scan_root,rel_base,(0),max_depth).then((function (files){
var sorted = cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$3(new cljs.core.Keyword(null,"modified","modified",-2134587826),cljs.core._GT_,files));
var G__52439 = reply;
var G__52440 = (200);
var G__52441 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"root","root",-448657453),rel_base,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(sorted),new cljs.core.Keyword(null,"files","files",-472457450),sorted], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52439,G__52440,G__52441) : json_response_BANG_.call(null,G__52439,G__52440,G__52441));
})).catch((function (err){
var G__52443 = reply;
var G__52444 = (500);
var G__52445 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to scan audio library: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52443,G__52444,G__52445) : json_response_BANG_.call(null,G__52443,G__52444,G__52445));
}));
}catch (e52434){var err = e52434;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52430,G__52431,G__52432,G__52433) : with_request_context_BANG_.call(null,G__52430,G__52431,G__52432,G__52433));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52426_52642,G__52427_52643,G__52428_52644,G__52429_52645) : route_BANG_.call(null,G__52426_52642,G__52427_52643,G__52428_52644,G__52429_52645));

var G__52446_52668 = app;
var G__52447_52669 = "POST";
var G__52448_52670 = "/api/workspace-media/audio-library/ensure-dir";
var G__52449_52671 = (function (request,reply){
var G__52450 = runtime;
var G__52451 = request;
var G__52452 = reply;
var G__52453 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"write") : ensure_tool_BANG_.call(null,ctx,"write"));
} else {
}

var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var dir_path = (function (){var or__5142__auto__ = (body["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(dir_path)){
var G__52460 = reply;
var G__52461 = (400);
var G__52462 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"path is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52460,G__52461,G__52462) : json_response_BANG_.call(null,G__52460,G__52461,G__52462));
} else {
var normalized = knoxx.backend.tools.media.normalize_tool_path_arg(dir_path);
var map__52463 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,normalized);
var map__52463__$1 = cljs.core.__destructure_map(map__52463);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52463__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52463__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
return shadow.esm.esm_import$node_fs$promises.mkdir(absolute,({"recursive": true})).then((function (){
var G__52464 = reply;
var G__52465 = (200);
var G__52466 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"path","path",-188191168),relative], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52464,G__52465,G__52466) : json_response_BANG_.call(null,G__52464,G__52465,G__52466));
})).catch((function (err){
var G__52471 = reply;
var G__52472 = (500);
var G__52473 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Failed to create directory: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52471,G__52472,G__52473) : json_response_BANG_.call(null,G__52471,G__52472,G__52473));
}));
}
}catch (e52458){var err = e52458;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52450,G__52451,G__52452,G__52453) : with_request_context_BANG_.call(null,G__52450,G__52451,G__52452,G__52453));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52446_52668,G__52447_52669,G__52448_52670,G__52449_52671) : route_BANG_.call(null,G__52446_52668,G__52447_52669,G__52448_52670,G__52449_52671));

var G__52474 = app;
var G__52475 = "POST";
var G__52476 = "/api/workspace-media/audio-library/rename";
var G__52477 = (function (request,reply){
var G__52478 = runtime;
var G__52479 = request;
var G__52480 = reply;
var G__52481 = (function (ctx){
try{if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"write") : ensure_tool_BANG_.call(null,ctx,"write"));
} else {
}

var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var from_path = (function (){var or__5142__auto__ = (body["from"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var to_path = (function (){var or__5142__auto__ = (body["to"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(from_path)){
var G__52484 = reply;
var G__52485 = (400);
var G__52486 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"from is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52484,G__52485,G__52486) : json_response_BANG_.call(null,G__52484,G__52485,G__52486));
} else {
if(clojure.string.blank_QMARK_(to_path)){
var G__52487 = reply;
var G__52488 = (400);
var G__52489 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"to is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52487,G__52488,G__52489) : json_response_BANG_.call(null,G__52487,G__52488,G__52489));
} else {
var from_norm = knoxx.backend.tools.media.normalize_tool_path_arg(from_path);
var to_norm = knoxx.backend.tools.media.normalize_tool_path_arg(to_path);
var map__52494 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,from_norm);
var map__52494__$1 = cljs.core.__destructure_map(map__52494);
var from_abs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52494__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var map__52495 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,to_norm);
var map__52495__$1 = cljs.core.__destructure_map(map__52495);
var to_abs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__52495__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
return shadow.esm.esm_import$node_fs$promises.rename(from_abs,to_abs).then((function (){
var G__52503 = reply;
var G__52504 = (200);
var G__52505 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"from","from",1815293044),from_norm,new cljs.core.Keyword(null,"to","to",192099007),to_norm], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52503,G__52504,G__52505) : json_response_BANG_.call(null,G__52503,G__52504,G__52505));
})).catch((function (err){
var G__52509 = reply;
var G__52510 = (500);
var G__52511 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Rename failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52509,G__52510,G__52511) : json_response_BANG_.call(null,G__52509,G__52510,G__52511));
}));

}
}
}catch (e52482){var err = e52482;
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52478,G__52479,G__52480,G__52481) : with_request_context_BANG_.call(null,G__52478,G__52479,G__52480,G__52481));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52474,G__52475,G__52476,G__52477) : route_BANG_.call(null,G__52474,G__52475,G__52476,G__52477));
});

//# sourceMappingURL=knoxx.backend.routes.workspace_media.js.map
