import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_child_process.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
import "./shadow.esm.esm_import$node_util.js";
goog.provide('knoxx.backend.tools.music');
knoxx.backend.tools.music.exec_file_async = shadow.esm.esm_import$node_util.promisify(shadow.esm.esm_import$node_child_process.execFile);
/**
 * Identify a song from an audio file using AudD API.
 */
knoxx.backend.tools.music.music_audd_lookup_BANG_ = (function knoxx$backend$tools$music$music_audd_lookup_BANG_(runtime,config,source){
var audd_token = new cljs.core.Keyword(null,"audd-api-token","audd-api-token",-1668649966).cljs$core$IFn$_invoke$arity$1(config);
if(clojure.string.blank_QMARK_(audd_token)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"AUDD_API_TOKEN not configured",new cljs.core.Keyword(null,"hint","hint",439639918),"Set AUDD_API_TOKEN to enable music identification"], null));
} else {
return knoxx.backend.tools.media.materialize_media_source_BANG_(runtime,config,source,knoxx.backend.tools.media.audio_render_max_bytes).then((function (media){
var form = (new FormData());
form.append("api_token",audd_token);

form.append("return","apple_music,spotify,deezer");

form.append("file",(new Blob([new cljs.core.Keyword(null,"buffer","buffer",617295198).cljs$core$IFn$_invoke$arity$1(media)],({"type": new cljs.core.Keyword(null,"mime-type","mime-type",1058646439).cljs$core$IFn$_invoke$arity$1(media)}))),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(media));

return fetch("https://api.audd.io/",({"method": "POST", "body": form})).then((function (resp){
if(cljs.core.not(resp.ok)){
return resp.text().then((function (text){
throw (new Error((""+"AudD HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
} else {
return resp.json();
}
})).then((function (payload){
var status = (function (){var or__5142__auto__ = (payload["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})();
var result = (payload["result"]);
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"source","source",-433931539),source,new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(media),new cljs.core.Keyword(null,"result","result",1415092211),(cljs.core.truth_(result)?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):null)], null);
}));
}));
}
});
/**
 * Look up audio fingerprint via AcoustID API.
 */
knoxx.backend.tools.music.music_acoustid_lookup_BANG_ = (function knoxx$backend$tools$music$music_acoustid_lookup_BANG_(config,fingerprint,duration){
var acoustid_key = new cljs.core.Keyword(null,"acoustid-api-key","acoustid-api-key",-1190639229).cljs$core$IFn$_invoke$arity$1(config);
if(clojure.string.blank_QMARK_(acoustid_key)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"ACOUSTID_API_KEY not configured",new cljs.core.Keyword(null,"hint","hint",439639918),"Set acoustid-api-key in Knoxx config to enable AcoustID lookups"], null));
} else {
var url = (""+"https://api.acoustid.org/v2/lookup?client="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(acoustid_key)+"&duration="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = duration;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (25);
}
})())+"&fingerprint="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fingerprint)+"&meta=recordings+recordingids+releasegroups");
return fetch(url).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"AcoustID HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}),(function (result){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),"ok",new cljs.core.Keyword(null,"result","result",1415092211),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
}).then());
}
});
/**
 * Look up MusicBrainz recording by MBID.
 */
knoxx.backend.tools.music.music_musicbrainz_recording_BANG_ = (function knoxx$backend$tools$music$music_musicbrainz_recording_BANG_(mbid){
var url = (""+"https://musicbrainz.org/ws/2/recording/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mbid)+"?inc=isrcs+releases+release-groups&fmt=json");
return Promise.resolve(null).then((function (_){
return (new Promise((function (resolve){
return setTimeout(resolve,(1100));
})));
})).then((function (_){
return fetch(url,({"headers": ({"User-Agent": "Knoxx-Agent/1.0 (discord bot)"})}));
})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+"MusicBrainz HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status))], null);
}
})).then((function (result){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),"ok",new cljs.core.Keyword(null,"mbid","mbid",1421571116),mbid,new cljs.core.Keyword(null,"result","result",1415092211),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
}));
});
/**
 * Check if audio is likely copyrighted based on ISRC presence.
 */
knoxx.backend.tools.music.music_copyright_check_BANG_ = (function knoxx$backend$tools$music$music_copyright_check_BANG_(_config,audio_data){
var isrc = cljs.core.get.cljs$core$IFn$_invoke$arity$2(audio_data,new cljs.core.Keyword(null,"isrc","isrc",-1965534627));
var apple_music_isrc = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(audio_data,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"apple_music","apple_music",-1398875611),new cljs.core.Keyword(null,"isrc","isrc",-1965534627)], null));
var spotify_isrc = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(audio_data,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"spotify","spotify",312979316),new cljs.core.Keyword(null,"external_ids","external_ids",-547120114),new cljs.core.Keyword(null,"isrc","isrc",-1965534627)], null));
var has_isrc = (((!(clojure.string.blank_QMARK_(isrc)))) || ((((!(clojure.string.blank_QMARK_(apple_music_isrc)))) || ((!(clojure.string.blank_QMARK_(spotify_isrc)))))));
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"decision","decision",820953053),((has_isrc)?"BLOCK":"UNKNOWN"),new cljs.core.Keyword(null,"reason","reason",-2070751759),((has_isrc)?"ISRC found - recording is commercially released":"No ISRC found - copyright status unclear"),new cljs.core.Keyword(null,"isrcs","isrcs",-692461665),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"primary","primary",817773892),isrc,new cljs.core.Keyword(null,"apple_music","apple_music",-1398875611),apple_music_isrc,new cljs.core.Keyword(null,"spotify","spotify",312979316),spotify_isrc], null)], null);
});
/**
 * Generate a WAV file from a JSON music spec using the native Node.js synthesis engine.
 */
knoxx.backend.tools.music.music_generate_BANG_ = (function knoxx$backend$tools$music$music_generate_BANG_(runtime,config,spec_json,output_path){
var script_path = knoxx.backend.tools.media.path_resolve.cljs$core$IFn$_invoke$arity$variadic(shadow.esm.esm_import$node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = process.cwd();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/";
}
})(),"scripts","synthesize-music.mjs"], 0));
return knoxx.backend.tools.media.temp_file_path_BANG_(runtime,"music-specs",".json").then((function (spec_path){
return knoxx.backend.tools.media.fs_write_file_BANG_.cljs$core$IFn$_invoke$arity$3(shadow.esm.esm_import$node_fs$promises,spec_path,spec_json).then((function (){
var out_path = (function (){var or__5142__auto__ = output_path;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+"Music/generated/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(shadow.esm.esm_import$node_crypto.randomUUID())+".wav");
}
})();
var map__59008 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,out_path);
var map__59008__$1 = cljs.core.__destructure_map(map__59008);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59008__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59008__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
return knoxx.backend.tools.media.fs_mkdir_BANG_(shadow.esm.esm_import$node_fs$promises,knoxx.backend.tools.media.path_resolve.cljs$core$IFn$_invoke$arity$variadic(shadow.esm.esm_import$node_path,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([absolute,".."], 0)),({"recursive": true})).then((function (){
return (function (){var G__59015 = "node";
var G__59016 = [script_path,spec_path,absolute];
var G__59017 = ({"timeout": (120000), "maxBuffer": (1048576)});
return (knoxx.backend.tools.music.exec_file_async.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.tools.music.exec_file_async.cljs$core$IFn$_invoke$arity$3(G__59015,G__59016,G__59017) : knoxx.backend.tools.music.exec_file_async.call(null,G__59015,G__59016,G__59017));
})().then((function (stdout,_stderr){
var result = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(stdout),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"workspace-path","workspace-path",918930789),relative,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"absolute-path","absolute-path",-1285195449),absolute], 0));
}));
}));
}));
}));
});
knoxx.backend.tools.music.identify_file_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file_path","file_path",-1069511467),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Workspace path, URL, or data URL for the audio file to identify."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.music.acoustid_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"fingerprint","fingerprint",598613022),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"AcoustID fingerprint string."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"duration","duration",1444101068),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Duration in seconds (default 25)."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null);
knoxx.backend.tools.music.musicbrainz_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"mbid","mbid",1421571116),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"MusicBrainz recording ID (MBID)."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.music.copyright_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"audio_data_json","audio_data_json",566868786),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"JSON object containing audio metadata with ISRC fields from AudD or similar."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.music.audio_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Path or URL to the audio file."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"width","width",-384071477),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Output width in pixels."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"height","height",1025178622),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Output height in pixels."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional filename/title for the rendered image."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.music.generate_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"spec_json","spec_json",-360102652),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"JSON music specification describing BPM, tracks, instruments, patterns, and notes."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"output_path","output_path",-1715585288),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional workspace-relative output path for the WAV file. Defaults to Music/generated/<uuid>.wav"], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.music.identify_file_execute = (function knoxx$backend$tools$music$identify_file_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var file_path = (params["file_path"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Identifying song from file: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(file_path)+"\u2026"));

return knoxx.backend.tools.music.music_audd_lookup_BANG_(runtime,config,file_path).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Music identification result: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(result))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(result,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"result","result",1415092211),new cljs.core.Keyword(null,"title","title",636505583)], null));
if(cljs.core.truth_(temp__5825__auto__)){
var song = temp__5825__auto__;
return (""+" - "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(song)+" by "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(result,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"result","result",1415092211),new cljs.core.Keyword(null,"artist","artist",152869709)], null))));
} else {
return null;
}
})())),result);
}));
});
knoxx.backend.tools.music.acoustid_execute = (function knoxx$backend$tools$music$acoustid_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var fingerprint = (params["fingerprint"]);
var duration = (params["duration"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Looking up AcoustID fingerprint\u2026");

return knoxx.backend.tools.music.music_acoustid_lookup_BANG_(config,fingerprint,duration).then((function (result){
return knoxx.backend.text.tool_text_result((""+"AcoustID lookup: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(result))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(result,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"result","result",1415092211),new cljs.core.Keyword(null,"results","results",-1134170113)], null));
if(cljs.core.truth_(temp__5825__auto__)){
var results = temp__5825__auto__;
return (""+" - found "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(results))+" matches");
} else {
return null;
}
})())),result);
}));
});
knoxx.backend.tools.music.musicbrainz_execute = (function knoxx$backend$tools$music$musicbrainz_execute(_runtime,_config,_tool_call_id,params,a,b,c){
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
var mbid = (params["mbid"]);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Looking up MusicBrainz recording "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mbid)+"\u2026"));

return knoxx.backend.tools.music.music_musicbrainz_recording_BANG_(mbid).then((function (result){
return knoxx.backend.text.tool_text_result((""+"MusicBrainz recording: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mbid)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(result,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"result","result",1415092211),new cljs.core.Keyword(null,"title","title",636505583)], null));
if(cljs.core.truth_(temp__5825__auto__)){
var title = temp__5825__auto__;
return (""+" - "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(title));
} else {
return null;
}
})())),result);
}));
});
knoxx.backend.tools.music.copyright_check_execute = (function knoxx$backend$tools$music$copyright_check_execute(_runtime,config,_tool_call_id,params,a,b,c){
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
var audio_data_json = (params["audio_data_json"]);
var audio_data = (function (){try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(audio_data_json),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e59080){var _ = e59080;
return null;
}})();
if((audio_data == null)){
return knoxx.backend.text.tool_text_result("Invalid audio_data_json",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Invalid JSON"], null));
} else {
var result = knoxx.backend.tools.music.music_copyright_check_BANG_(config,audio_data);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Checking copyright status\u2026");

return knoxx.backend.text.tool_text_result((""+"Copyright decision: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"decision","decision",820953053).cljs$core$IFn$_invoke$arity$1(result))+" - "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"reason","reason",-2070751759).cljs$core$IFn$_invoke$arity$1(result))),result);
}
});
knoxx.backend.tools.music.spectrogram_execute = (function knoxx$backend$tools$music$spectrogram_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var source = (params["source"]);
var width = (params["width"]);
var height = (params["height"]);
var title = knoxx.backend.tools.media.normalize_tool_path_arg((params["title"]));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Audio spectrogram generation\u2026");

return knoxx.backend.tools.media.audio_visualization_result_BANG_(runtime,config,source,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"spectrogram","spectrogram",950500999),new cljs.core.Keyword(null,"width","width",-384071477),width,new cljs.core.Keyword(null,"height","height",1025178622),height,new cljs.core.Keyword(null,"title","title",636505583),title], null));
});
knoxx.backend.tools.music.waveform_execute = (function knoxx$backend$tools$music$waveform_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var source = (params["source"]);
var width = (params["width"]);
var height = (params["height"]);
var title = knoxx.backend.tools.media.normalize_tool_path_arg((params["title"]));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Audio waveform generation\u2026");

return knoxx.backend.tools.media.audio_visualization_result_BANG_(runtime,config,source,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"waveform","waveform",1355199399),new cljs.core.Keyword(null,"width","width",-384071477),width,new cljs.core.Keyword(null,"height","height",1025178622),height,new cljs.core.Keyword(null,"title","title",636505583),title], null));
});
knoxx.backend.tools.music.generate_execute = (function knoxx$backend$tools$music$generate_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var spec_json = (params["spec_json"]);
var output_path = knoxx.backend.tools.media.normalize_tool_path_arg((params["output_path"]));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Generating music from spec\u2026");

return knoxx.backend.tools.music.music_generate_BANG_(runtime,config,spec_json,output_path).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Generated WAV: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"workspace-path","workspace-path",918930789).cljs$core$IFn$_invoke$arity$1(result))+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"durationSec","durationSec",385007278).cljs$core$IFn$_invoke$arity$1(result))+"s, "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sampleRate","sampleRate",-541273751).cljs$core$IFn$_invoke$arity$1(result))+"Hz)"),result);
}));
});
knoxx.backend.tools.music.identify_file_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"music.identify_file","Music Identify File","Identify a song from an audio file using AudD API.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Identify songs from audio files when you need to know what music is playing.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use music.identify_file when you have an audio file and need to identify the song.","Returns song title, artist, album, and ISRC when found."], null),knoxx.backend.tools.music.identify_file_params,knoxx.backend.tools.music.identify_file_execute], 0));
knoxx.backend.tools.music.acoustid_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"music.acoustid_lookup","AcoustID Lookup","Look up audio fingerprint via AcoustID API to identify recordings.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Look up AcoustID fingerprints to identify music by its acoustic signature.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use music.acoustid_lookup when you have a fingerprint from chromaprint or similar.","Requires AcoustID API key in config."], null),knoxx.backend.tools.music.acoustid_params,knoxx.backend.tools.music.acoustid_execute], 0));
knoxx.backend.tools.music.musicbrainz_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"music.musicbrainz_recording","MusicBrainz Recording","Look up MusicBrainz recording metadata by MBID.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Fetch detailed recording metadata from MusicBrainz.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use music.musicbrainz_recording when you have a MusicBrainz ID (MBID).","Returns ISRCs, releases, release groups, and other metadata.","Rate-limited to 1 request per second."], null),knoxx.backend.tools.music.musicbrainz_params,knoxx.backend.tools.music.musicbrainz_execute], 0));
knoxx.backend.tools.music.copyright_check_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"music.copyright_check","Music Copyright Check","Check if audio is likely copyrighted based on ISRC presence.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Check copyright status of identified music before using it.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use music.copyright_check after music identification to assess copyright risk.","Returns BLOCK if ISRC found (commercially released), UNKNOWN otherwise.","Pass audio_data_json from AudD or similar identification result."], null),knoxx.backend.tools.music.copyright_params,knoxx.backend.tools.music.copyright_check_execute], 0));
knoxx.backend.tools.music.spectrogram_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"audio.spectrogram","Audio Spectrogram","Generate a spectrogram image from an audio file using ffmpeg.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Visualize audio as a spectrogram to see frequencies over time.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use audio.spectrogram to visualize audio frequency content.","This is especially useful when the active model can see images but cannot directly accept audio input."], null),knoxx.backend.tools.music.audio_params,knoxx.backend.tools.music.spectrogram_execute], 0));
knoxx.backend.tools.music.waveform_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"audio.waveform","Audio Waveform","Generate a waveform image from an audio file using ffmpeg.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Visualize audio as a waveform to see amplitude over time.",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use audio.waveform to visualize audio amplitude.","Pair with audio.spectrogram when you want both amplitude and frequency views."], null),knoxx.backend.tools.music.audio_params,knoxx.backend.tools.music.waveform_execute], 0));
knoxx.backend.tools.music.generate_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"music.generate","Generate Music","Synthesize a WAV file from a JSON music spec using the native Node.js audio engine.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Generate original music and render it to a WAV file directly on the server.",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use music.generate when the user wants original synthesized music, beats, loops, or melodies.","Construct a JSON spec with bpm, tracks, instruments, and patterns.","Supported instruments: synth, bass, lead, pad, drum (kick, snare, hihat, clap, tom).","After generation, use workspace_media.attach to embed the WAV in the reply with a player.","Default output path is Music/generated/<uuid>.wav."], null),knoxx.backend.tools.music.generate_params,knoxx.backend.tools.music.generate_execute], 0));
/**
 * Create music/audio identification and analysis tools.
 */
knoxx.backend.tools.music.create_music_custom_tools = (function knoxx$backend$tools$music$create_music_custom_tools(var_args){
var G__59108 = arguments.length;
switch (G__59108) {
case 2:
return knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.music.create_music_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("music.identify_file"))?knoxx.backend.tools.music.identify_file_tool(runtime,config):null),((allowed_QMARK_("music.acoustid_lookup"))?knoxx.backend.tools.music.acoustid_tool(runtime,config):null),((allowed_QMARK_("music.musicbrainz_recording"))?knoxx.backend.tools.music.musicbrainz_tool(runtime,config):null),((allowed_QMARK_("music.copyright_check"))?knoxx.backend.tools.music.copyright_check_tool(runtime,config):null),((allowed_QMARK_("audio.spectrogram"))?knoxx.backend.tools.music.spectrogram_tool(runtime,config):null),((allowed_QMARK_("audio.waveform"))?knoxx.backend.tools.music.waveform_tool(runtime,config):null),((allowed_QMARK_("music.generate"))?knoxx.backend.tools.music.generate_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.music.create_music_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.music.js.map
