import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.http.js";
goog.provide('knoxx.backend.routes.voice');
knoxx.backend.routes.voice.default_voxx_voice_id = "af_jessica";
knoxx.backend.routes.voice.default_voxx_model_id = "kokoro";
knoxx.backend.routes.voice.default_voxx_speed = "1.15";
knoxx.backend.routes.voice.default_voxx_output_format = "mp3";
knoxx.backend.routes.voice.default_voxx_postprocess_profile = "sports-commentator-v1";
knoxx.backend.routes.voice.trim_trailing_slashes = (function knoxx$backend$routes$voice$trim_trailing_slashes(s){
return clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = s;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\/+$/,"");
});
knoxx.backend.routes.voice.stt_base_url = (function knoxx$backend$routes$voice$stt_base_url(config){
return knoxx.backend.routes.voice.trim_trailing_slashes(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"stt-base-url","stt-base-url",-12292445).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
});
knoxx.backend.routes.voice.fetch_stt_json = (function knoxx$backend$routes$voice$fetch_stt_json(base_url,suffix,opts){
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_url)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix)),opts);
});
knoxx.backend.routes.voice.trim_or_empty = (function knoxx$backend$routes$voice$trim_or_empty(value){
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
});
knoxx.backend.routes.voice.false_like_QMARK_ = (function knoxx$backend$routes$voice$false_like_QMARK_(value){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(false,value)) || (cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, ["none",null,"off",null,"false",null,"disable",null,"0",null,"no",null,"disabled",null], null), null),clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto____$1 = value;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
})())))))));
});
knoxx.backend.routes.voice.bool_value = (function knoxx$backend$routes$voice$bool_value(value,default$){
if((value == null)){
return default$;
} else {
return (!(knoxx.backend.routes.voice.false_like_QMARK_(value)));
}
});
knoxx.backend.routes.voice.first_body_value = (function knoxx$backend$routes$voice$first_body_value(body,names){
return cljs.core.some((function (name){
var value = (body[name]);
if((value == null)){
return null;
} else {
return value;
}
}),names);
});
knoxx.backend.routes.voice.voice_gateway_url = (function knoxx$backend$routes$voice$voice_gateway_url(config){
var configured = knoxx.backend.routes.voice.trim_or_empty(new cljs.core.Keyword(null,"voxx-url","voxx-url",-1259052170).cljs$core$IFn$_invoke$arity$1(config));
if(clojure.string.blank_QMARK_(configured)){
return "http://127.0.0.1:8787";
} else {
return knoxx.backend.routes.voice.trim_trailing_slashes(configured);
}
});
knoxx.backend.routes.voice.voxx_v1_url = (function knoxx$backend$routes$voice$voxx_v1_url(config,suffix){
var base = knoxx.backend.routes.voice.voice_gateway_url(config);
if(clojure.string.ends_with_QMARK_(base,"/v1/audio/speech")){
return clojure.string.replace(base,/\/audio\/speech$/,suffix);
} else {
if(clojure.string.ends_with_QMARK_(base,"/v1")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/v1"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));

}
}
});
knoxx.backend.routes.voice.voice_gateway_ws_url = (function knoxx$backend$routes$voice$voice_gateway_ws_url(config){
var url = knoxx.backend.routes.voice.voice_gateway_url(config);
return clojure.string.replace(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)),"^https://","wss://"),"^http://","ws://");
});
knoxx.backend.routes.voice.voice_gateway_api_key = (function knoxx$backend$routes$voice$voice_gateway_api_key(config){
return knoxx.backend.routes.voice.trim_or_empty(new cljs.core.Keyword(null,"voxx-api-key","voxx-api-key",2053708716).cljs$core$IFn$_invoke$arity$1(config));
});
knoxx.backend.routes.voice.voxx_default_voice_id = (function knoxx$backend$routes$voice$voxx_default_voice_id(config){
var configured = knoxx.backend.routes.voice.trim_or_empty(new cljs.core.Keyword(null,"voxx-voice-id","voxx-voice-id",-652120125).cljs$core$IFn$_invoke$arity$1(config));
if(clojure.string.blank_QMARK_(configured)){
return knoxx.backend.routes.voice.default_voxx_voice_id;
} else {
return configured;
}
});
knoxx.backend.routes.voice.voxx_default_model_id = (function knoxx$backend$routes$voice$voxx_default_model_id(config){
var configured = knoxx.backend.routes.voice.trim_or_empty(new cljs.core.Keyword(null,"voxx-model-id","voxx-model-id",2106305693).cljs$core$IFn$_invoke$arity$1(config));
if(clojure.string.blank_QMARK_(configured)){
return knoxx.backend.routes.voice.default_voxx_model_id;
} else {
return configured;
}
});
knoxx.backend.routes.voice.voxx_default_speed = (function knoxx$backend$routes$voice$voxx_default_speed(config){
var configured = knoxx.backend.routes.voice.trim_or_empty(new cljs.core.Keyword(null,"voxx-default-speed","voxx-default-speed",-370827943).cljs$core$IFn$_invoke$arity$1(config));
if(clojure.string.blank_QMARK_(configured)){
return knoxx.backend.routes.voice.default_voxx_speed;
} else {
return configured;
}
});
knoxx.backend.routes.voice.voxx_headers = (function knoxx$backend$routes$voice$voxx_headers(api_key){
return ({"Content-Type": "application/json", "Accept": "audio/mpeg", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key))});
});
knoxx.backend.routes.voice.voxx_health_headers = (function knoxx$backend$routes$voice$voxx_health_headers(api_key){
return ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key))});
});
knoxx.backend.routes.voice.voxx_tts_url = (function knoxx$backend$routes$voice$voxx_tts_url(config){
return knoxx.backend.routes.voice.voxx_v1_url(config,"/audio/speech");
});

knoxx.backend.routes.voice.message_data__GT_string = (function knoxx$backend$routes$voice$message_data__GT_string(value){
if(typeof value === 'string'){
return value;
} else {
if((value instanceof Buffer)){
return value.toString("utf8");
} else {
if((value instanceof Uint8Array)){
return Buffer.from(value).toString("utf8");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));

}
}
}
});
knoxx.backend.routes.voice.ws_send_json_BANG_ = (function knoxx$backend$routes$voice$ws_send_json_BANG_(socket,payload){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((1),(socket["readyState"]))){
return socket.send(JSON.stringify(cljs.core.clj__GT_js(payload)));
} else {
return null;
}
});
knoxx.backend.routes.voice.ws_close_BANG_ = (function knoxx$backend$routes$voice$ws_close_BANG_(var_args){
var G__524104 = arguments.length;
switch (G__524104) {
case 1:
return knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (socket){
return knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$3(socket,(1000),"");
}));

(knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (socket,code,reason){
if(cljs.core.truth_(socket)){
try{return socket.close(code,reason);
}catch (e524114){var _ = e524114;
return null;
}} else {
return null;
}
}));

(knoxx.backend.routes.voice.ws_close_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.routes.voice.normalize_voice_stream_text = (function knoxx$backend$routes$voice$normalize_voice_stream_text(value){
var text = clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\s+/," "));
if(clojure.string.blank_QMARK_(text)){
return "";
} else {
if(clojure.string.ends_with_QMARK_(text," ")){
return text;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)+" ");

}
}
});
knoxx.backend.routes.voice.relay_voice_stream_BANG_ = (function knoxx$backend$routes$voice$relay_voice_stream_BANG_(client,payload){
var audio = (payload["audio"]);
var is_final = (payload["isFinal"]) === true;
if(typeof audio === 'string'){
return knoxx.backend.routes.voice.ws_send_json_BANG_(client,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),"audio",new cljs.core.Keyword(null,"audio","audio",1819127321),audio,new cljs.core.Keyword(null,"alignment","alignment",1040093386),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((payload["alignment"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),new cljs.core.Keyword(null,"normalized_alignment","normalized_alignment",-162749980),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((payload["normalizedAlignment"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
} else {
if(is_final){
return knoxx.backend.routes.voice.ws_send_json_BANG_(client,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"final",new cljs.core.Keyword(null,"isFinal","isFinal",150442431),true], null));
} else {
return knoxx.backend.routes.voice.ws_send_json_BANG_(client,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"event",new cljs.core.Keyword(null,"payload","payload",-383036092),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(payload,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));

}
}
});
knoxx.backend.routes.voice.register_voice_ws_route_BANG_ = (function knoxx$backend$routes$voice$register_voice_ws_route_BANG_(app,_config){
var G__524138 = app;
var G__524139 = ({"method": "GET", "url": "/ws/voice/tts", "handler": (function (_request,reply){
return reply.code((426)).type("application/json").send(({"error": "WebSocket upgrade required"}));
}), "wsHandler": (function (socket,_request){
var client = (function (){var or__5142__auto__ = (socket["socket"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return socket;
}
})();
knoxx.backend.routes.voice.ws_send_json_BANG_(client,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"error",new cljs.core.Keyword(null,"detail","detail",-1545345025),"Voxx streaming TTS is not exposed by this Knoxx bridge yet. Use voice.tts or POST /api/voice/tts for Voxx /v1/audio/speech."], null));

return knoxx.backend.routes.voice.ws_close_BANG_.cljs$core$IFn$_invoke$arity$3(client,(1000),"voxx_streaming_tts_unavailable");
})});
return (knoxx.backend.routes.voice.app_route_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.routes.voice.app_route_BANG_.cljs$core$IFn$_invoke$arity$2(G__524138,G__524139) : knoxx.backend.routes.voice.app_route_BANG_.call(null,G__524138,G__524139));
});
knoxx.backend.routes.voice.request_parts_promise = (function knoxx$backend$routes$voice$request_parts_promise(request){
return Array.fromAsync(request.parts());
});
knoxx.backend.routes.voice.reply_header_BANG_ = (function knoxx$backend$routes$voice$reply_header_BANG_(reply,name,value){
return reply.header(name,value);
});
knoxx.backend.routes.voice.ws_on_BANG_ = (function knoxx$backend$routes$voice$ws_on_BANG_(socket,event_name,handler){
return socket.on(event_name,handler);
});
knoxx.backend.routes.voice.app_route_BANG_ = (function knoxx$backend$routes$voice$app_route_BANG_(app,opts){
return app.route(opts);
});
knoxx.backend.routes.voice.register_voice_routes_BANG_ = (function knoxx$backend$routes$voice$register_voice_routes_BANG_(app,runtime,config,handlers){
var map__524167 = handlers;
var map__524167__$1 = cljs.core.__destructure_map(map__524167);
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__524167__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__524167__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__524167__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var ensure_tool_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__524167__$1,new cljs.core.Keyword(null,"ensure-tool!","ensure-tool!",-869161334));
knoxx.backend.routes.voice.register_voice_ws_route_BANG_(app,config);

var G__524169_524411 = app;
var G__524170_524412 = "GET";
var G__524171_524413 = "/api/voice/stt/health";
var G__524172_524414 = (function (request,reply){
var G__524173 = runtime;
var G__524174 = request;
var G__524175 = reply;
var G__524176 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"multimodal.upload") : ensure_tool_BANG_.call(null,ctx,"multimodal.upload"));
} else {
}

var base = knoxx.backend.routes.voice.stt_base_url(config);
if(clojure.string.blank_QMARK_(base)){
var G__524193 = reply;
var G__524194 = (503);
var G__524195 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"KNOXX_STT_BASE_URL is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524193,G__524194,G__524195) : json_response_BANG_.call(null,G__524193,G__524194,G__524195));
} else {
return knoxx.backend.routes.voice.fetch_stt_json(base,"/health",({"method": "GET"})).then((function (resp){
var G__524199 = reply;
var G__524200 = (cljs.core.truth_((resp["ok"]))?(200):(502));
var G__524201 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524199,G__524200,G__524201) : json_response_BANG_.call(null,G__524199,G__524200,G__524201));
})).catch((function (err){
var G__524203 = reply;
var G__524204 = (502);
var G__524205 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"STT health failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524203,G__524204,G__524205) : json_response_BANG_.call(null,G__524203,G__524204,G__524205));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__524173,G__524174,G__524175,G__524176) : with_request_context_BANG_.call(null,G__524173,G__524174,G__524175,G__524176));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__524169_524411,G__524170_524412,G__524171_524413,G__524172_524414) : route_BANG_.call(null,G__524169_524411,G__524170_524412,G__524171_524413,G__524172_524414));

var G__524206_524420 = app;
var G__524207_524421 = "POST";
var G__524208_524422 = "/api/voice/stt";
var G__524209_524423 = (function (request,reply){
var G__524211 = runtime;
var G__524212 = request;
var G__524213 = reply;
var G__524214 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"multimodal.upload") : ensure_tool_BANG_.call(null,ctx,"multimodal.upload"));
} else {
}

var base = knoxx.backend.routes.voice.stt_base_url(config);
if(clojure.string.blank_QMARK_(base)){
var G__524216 = reply;
var G__524217 = (503);
var G__524218 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"KNOXX_STT_BASE_URL is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524216,G__524217,G__524218) : json_response_BANG_.call(null,G__524216,G__524217,G__524218));
} else {
var promise = knoxx.backend.routes.voice.request_parts_promise(request).then((function (parts){
var part_seq = knoxx.backend.http.js_array_seq(parts);
var file_part = cljs.core.first(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__524155_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((p1__524155_SHARP_["type"]),"file");
}),part_seq));
if(cljs.core.not(file_part)){
return ({"error": ({"status": (400), "detail": "No file uploaded. Send multipart/form-data with a file part."})});
} else {
return (new Response((file_part["file"]))).arrayBuffer().then((function (buf){
var mime = (function (){var or__5142__auto__ = (file_part["mimetype"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (file_part["type"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "application/octet-stream";
}
}
})();
var headers = ({"Content-Type": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime))});
var body = Buffer.from(buf);
return knoxx.backend.routes.voice.fetch_stt_json(base,"/transcribe",({"method": "POST", "headers": headers, "body": body}));
}));
}
})).then((function (resp){
if(cljs.core.truth_((function (){var and__5140__auto__ = resp;
if(cljs.core.truth_(and__5140__auto__)){
return (resp["error"]);
} else {
return and__5140__auto__;
}
})())){
var err = (resp["error"]);
var G__524247 = reply;
var G__524248 = (err["status"]);
var G__524249 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(err,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524247,G__524248,G__524249) : json_response_BANG_.call(null,G__524247,G__524248,G__524249));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = resp;
if(cljs.core.truth_(and__5140__auto__)){
return (resp["ok"]);
} else {
return and__5140__auto__;
}
})())){
var G__524253 = reply;
var G__524254 = (200);
var G__524255 = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524253,G__524254,G__524255) : json_response_BANG_.call(null,G__524253,G__524254,G__524255));
} else {
var G__524256 = reply;
var G__524257 = (502);
var G__524258 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"STT service error",new cljs.core.Keyword(null,"status","status",-1997798413),(resp["status"]),new cljs.core.Keyword(null,"body","body",-2049205669),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524256,G__524257,G__524258) : json_response_BANG_.call(null,G__524256,G__524257,G__524258));

}
}
})).catch((function (err){
var G__524263 = reply;
var G__524264 = (500);
var G__524265 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"STT request failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524263,G__524264,G__524265) : json_response_BANG_.call(null,G__524263,G__524264,G__524265));
}));
return promise;
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__524211,G__524212,G__524213,G__524214) : with_request_context_BANG_.call(null,G__524211,G__524212,G__524213,G__524214));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__524206_524420,G__524207_524421,G__524208_524422,G__524209_524423) : route_BANG_.call(null,G__524206_524420,G__524207_524421,G__524208_524422,G__524209_524423));

var G__524271_524435 = app;
var G__524272_524436 = "GET";
var G__524273_524437 = "/api/voice/tts/health";
var G__524274_524438 = (function (request,reply){
var G__524278 = runtime;
var G__524279 = request;
var G__524280 = reply;
var G__524281 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"multimodal.upload") : ensure_tool_BANG_.call(null,ctx,"multimodal.upload"));
} else {
}

var api_key = knoxx.backend.routes.voice.voice_gateway_api_key(config);
if(clojure.string.blank_QMARK_(api_key)){
var G__524286 = reply;
var G__524287 = (503);
var G__524288 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"VOICE_GATEWAY_API_KEY is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524286,G__524287,G__524288) : json_response_BANG_.call(null,G__524286,G__524287,G__524288));
} else {
return knoxx.backend.http.fetch_json(knoxx.backend.routes.voice.voxx_v1_url(config,"/voices"),({"method": "GET", "headers": knoxx.backend.routes.voice.voxx_health_headers(api_key)})).then((function (resp){
var G__524291 = reply;
var G__524292 = (cljs.core.truth_((resp["ok"]))?(200):(502));
var G__524293 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"reachable","reachable",-1495191549),new cljs.core.Keyword(null,"default_speed","default_speed",-854483932),new cljs.core.Keyword(null,"default_postprocess_enabled","default_postprocess_enabled",526743684),new cljs.core.Keyword(null,"default_prompt_aware","default_prompt_aware",-1135936336),new cljs.core.Keyword(null,"default_voice_id","default_voice_id",-896667018),new cljs.core.Keyword(null,"status_code","status_code",-572644263),new cljs.core.Keyword(null,"default_model_id","default_model_id",631932827),new cljs.core.Keyword(null,"provider","provider",-302056900),new cljs.core.Keyword(null,"default_postprocess_profile","default_postprocess_profile",1969594013),new cljs.core.Keyword(null,"configured","configured",-884777889)],[cljs.core.boolean$((resp["ok"])),knoxx.backend.routes.voice.voxx_default_speed(config),true,true,knoxx.backend.routes.voice.voxx_default_voice_id(config),(resp["status"]),knoxx.backend.routes.voice.voxx_default_model_id(config),"voxx",knoxx.backend.routes.voice.default_voxx_postprocess_profile,true]);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524291,G__524292,G__524293) : json_response_BANG_.call(null,G__524291,G__524292,G__524293));
})).catch((function (err){
var G__524297 = reply;
var G__524298 = (502);
var G__524299 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Voice Gateway health failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524297,G__524298,G__524299) : json_response_BANG_.call(null,G__524297,G__524298,G__524299));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__524278,G__524279,G__524280,G__524281) : with_request_context_BANG_.call(null,G__524278,G__524279,G__524280,G__524281));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__524271_524435,G__524272_524436,G__524273_524437,G__524274_524438) : route_BANG_.call(null,G__524271_524435,G__524272_524436,G__524273_524437,G__524274_524438));

var G__524303_524458 = app;
var G__524304_524459 = "POST";
var G__524305_524460 = "/api/voice/tts";
var G__524306_524461 = (function (request,reply){
var G__524307 = runtime;
var G__524308 = request;
var G__524309 = reply;
var G__524310 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_tool_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"multimodal.upload") : ensure_tool_BANG_.call(null,ctx,"multimodal.upload"));
} else {
}

var api_key = knoxx.backend.routes.voice.voice_gateway_api_key(config);
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var voice_id_raw = knoxx.backend.routes.voice.trim_or_empty((function (){var or__5142__auto__ = (body["voice_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["voiceId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})());
var voice_id = ((clojure.string.blank_QMARK_(voice_id_raw))?knoxx.backend.routes.voice.voxx_default_voice_id(config):voice_id_raw);
var model_id_raw = knoxx.backend.routes.voice.trim_or_empty((function (){var or__5142__auto__ = (body["model_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["modelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (body["model"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})());
var model_id = ((clojure.string.blank_QMARK_(model_id_raw))?knoxx.backend.routes.voice.voxx_default_model_id(config):model_id_raw);
var output_format_raw = knoxx.backend.routes.voice.trim_or_empty((function (){var or__5142__auto__ = (body["output_format"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["outputFormat"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (body["response_format"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (body["responseFormat"]);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return "";
}
}
}
}
})());
var output_format = ((clojure.string.blank_QMARK_(output_format_raw))?knoxx.backend.routes.voice.default_voxx_output_format:output_format_raw);
var speed_raw = knoxx.backend.routes.voice.trim_or_empty(knoxx.backend.routes.voice.first_body_value(body,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["speed"], null)));
var speed = ((clojure.string.blank_QMARK_(speed_raw))?knoxx.backend.routes.voice.voxx_default_speed(config):speed_raw);
var postprocess_profile_raw = knoxx.backend.routes.voice.trim_or_empty(knoxx.backend.routes.voice.first_body_value(body,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["postprocess_profile","postprocessProfile","postprocess"], null)));
var postprocess_profile = ((clojure.string.blank_QMARK_(postprocess_profile_raw))?knoxx.backend.routes.voice.default_voxx_postprocess_profile:postprocess_profile_raw);
var postprocess_enabled = knoxx.backend.routes.voice.bool_value(knoxx.backend.routes.voice.first_body_value(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["postprocess_enabled","postprocessEnabled"], null)),true);
var prompt_aware = knoxx.backend.routes.voice.bool_value(knoxx.backend.routes.voice.first_body_value(body,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["prompt_aware","promptAware","prompt-aware"], null)),true);
var prompt_aware_style = knoxx.backend.routes.voice.trim_or_empty(knoxx.backend.routes.voice.first_body_value(body,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["prompt_aware_style","promptAwareStyle"], null)));
var voice_settings = (body["voice_settings"]);
if(clojure.string.blank_QMARK_(api_key)){
var G__524342 = reply;
var G__524345 = (503);
var G__524346 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"VOICE_GATEWAY_API_KEY is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524342,G__524345,G__524346) : json_response_BANG_.call(null,G__524342,G__524345,G__524346));
} else {
if(clojure.string.blank_QMARK_(clojure.string.trim(text))){
var G__524347 = reply;
var G__524348 = (400);
var G__524349 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Missing required field: text"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524347,G__524348,G__524349) : json_response_BANG_.call(null,G__524347,G__524348,G__524349));
} else {
var payload = cljs.core.clj__GT_js((function (){var G__524353 = new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"input","input",556931961),text,new cljs.core.Keyword(null,"voice","voice",185716428),voice_id,new cljs.core.Keyword(null,"model","model",331153215),model_id,new cljs.core.Keyword(null,"response_format","response_format",1229973741),output_format,new cljs.core.Keyword(null,"speed","speed",1257663751),speed,new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),postprocess_enabled,new cljs.core.Keyword(null,"prompt_aware","prompt_aware",1309007496),prompt_aware], null);
var G__524353__$1 = (cljs.core.truth_(postprocess_profile)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__524353,new cljs.core.Keyword(null,"postprocess_profile","postprocess_profile",-1254686835),postprocess_profile):G__524353);
var G__524353__$2 = (((!(clojure.string.blank_QMARK_(prompt_aware_style))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__524353__$1,new cljs.core.Keyword(null,"prompt_aware_style","prompt_aware_style",1965441274),prompt_aware_style):G__524353__$1);
if(cljs.core.truth_((function (){var and__5140__auto__ = voice_settings;
if(cljs.core.truth_(and__5140__auto__)){
return (!((voice_settings == null)));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__524353__$2,new cljs.core.Keyword(null,"voice_settings","voice_settings",160567748),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(voice_settings));
} else {
return G__524353__$2;
}
})());
var url = knoxx.backend.routes.voice.voxx_tts_url(config);
var opts = ({"method": "POST", "headers": knoxx.backend.routes.voice.voxx_headers(api_key), "body": JSON.stringify(payload)});
return fetch(url,opts).then((function (resp){
if(cljs.core.truth_(resp.ok)){
knoxx.backend.routes.voice.reply_header_BANG_(reply,"Cache-Control","no-store");

return knoxx.backend.http.send_fetch_response_BANG_(reply,resp);
} else {
return resp.text().then((function (detail){
var G__524360 = reply;
var G__524361 = resp.status;
var G__524362 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Voice Gateway TTS failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(detail)),new cljs.core.Keyword(null,"status_code","status_code",-572644263),resp.status], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524360,G__524361,G__524362) : json_response_BANG_.call(null,G__524360,G__524361,G__524362));
}));
}
})).catch((function (err){
var G__524366 = reply;
var G__524367 = (502);
var G__524368 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+"Voice Gateway TTS request failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__524366,G__524367,G__524368) : json_response_BANG_.call(null,G__524366,G__524367,G__524368));
}));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__524307,G__524308,G__524309,G__524310) : with_request_context_BANG_.call(null,G__524307,G__524308,G__524309,G__524310));
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__524303_524458,G__524304_524459,G__524305_524460,G__524306_524461) : route_BANG_.call(null,G__524303_524458,G__524304_524459,G__524305_524460,G__524306_524461));

return null;
});
knoxx.backend.routes.voice.register_voice_routes = (function knoxx$backend$routes$voice$register_voice_routes(app,runtime,config,handlers){
return knoxx.backend.routes.voice.register_voice_routes_BANG_(app,runtime,config,handlers);
});

//# sourceMappingURL=knoxx.backend.routes.voice.js.map
