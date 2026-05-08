import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_context.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.discord_voice');
knoxx.backend.tools.discord_voice.gw = (function knoxx$backend$tools$discord_voice$gw(){
return knoxx.backend.discord_gateway.gateway_manager.cljs$core$IFn$_invoke$arity$0();
});
knoxx.backend.tools.discord_voice.resolve_voice_key = (function knoxx$backend$tools$discord_voice$resolve_voice_key(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"voxx-api-key","voxx-api-key",2053708716)):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58761 = process;
var G__58761__$1 = (((G__58761 == null))?null:G__58761.env);
if((G__58761__$1 == null)){
return null;
} else {
return (G__58761__$1["VOICE_GATEWAY_API_KEY"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__58764 = process;
var G__58764__$1 = (((G__58764 == null))?null:G__58764.env);
if((G__58764__$1 == null)){
return null;
} else {
return (G__58764__$1["KNOXX_VOICE_GATEWAY_API_KEY"]);
}
}
}
});
knoxx.backend.tools.discord_voice.voice_gateway_url = (function knoxx$backend$tools$discord_voice$voice_gateway_url(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"voxx-url","voxx-url",-1259052170)):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58767 = process;
var G__58767__$1 = (((G__58767 == null))?null:G__58767.env);
if((G__58767__$1 == null)){
return null;
} else {
return (G__58767__$1["VOXX_URL"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "http://127.0.0.1:8787";
}
}
});
knoxx.backend.tools.discord_voice.tts_url = (function knoxx$backend$tools$discord_voice$tts_url(config){
var base = clojure.string.replace(knoxx.backend.tools.discord_voice.voice_gateway_url(config),/\/+$/,"");
if(clojure.string.ends_with_QMARK_(base,"/v1/audio/speech")){
return base;
} else {
if(clojure.string.ends_with_QMARK_(base,"/v1")){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/audio/speech");
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"/v1/audio/speech");

}
}
});
knoxx.backend.tools.discord_voice.default_tts_speed = (function knoxx$backend$tools$discord_voice$default_tts_speed(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"voxx-default-speed","voxx-default-speed",-370827943)):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58792 = process;
var G__58792__$1 = (((G__58792 == null))?null:G__58792.env);
if((G__58792__$1 == null)){
return null;
} else {
return (G__58792__$1["KNOXX_VOXX_DEFAULT_SPEED"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__58794 = process;
var G__58794__$1 = (((G__58794 == null))?null:G__58794.env);
if((G__58794__$1 == null)){
return null;
} else {
return (G__58794__$1["VOICE_GATEWAY_TTS_DEFAULT_SPEED"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "1.15";
}
}
}
});
knoxx.backend.tools.discord_voice.stt_url = (function knoxx$backend$tools$discord_voice$stt_url(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"stt-url","stt-url",-2119897950));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"stt-base-url","stt-base-url",-12292445));
}
})():null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58804 = process;
var G__58804__$1 = (((G__58804 == null))?null:G__58804.env);
if((G__58804__$1 == null)){
return null;
} else {
return (G__58804__$1["KNOXX_STT_URL"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__58811 = process;
var G__58811__$1 = (((G__58811 == null))?null:G__58811.env);
if((G__58811__$1 == null)){
return null;
} else {
return (G__58811__$1["KNOXX_STT_BASE_URL"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "http://127.0.0.1:8010";
}
}
}
});
knoxx.backend.tools.discord_voice.knoxx_base = (function knoxx$backend$tools$discord_voice$knoxx_base(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143)):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58825 = process;
var G__58825__$1 = (((G__58825 == null))?null:G__58825.env);
if((G__58825__$1 == null)){
return null;
} else {
return (G__58825__$1["KNOXX_BASE_URL"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "http://127.0.0.1:8000";
}
}
});
knoxx.backend.tools.discord_voice.knoxx_key = (function knoxx$backend$tools$discord_voice$knoxx_key(config){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154)):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__58831 = process;
var G__58831__$1 = (((G__58831 == null))?null:G__58831.env);
if((G__58831__$1 == null)){
return null;
} else {
return (G__58831__$1["KNOXX_API_KEY"]);
}
}
});
knoxx.backend.tools.discord_voice.knoxx_control_headers = (function knoxx$backend$tools$discord_voice$knoxx_control_headers(config){
var api_key = knoxx.backend.tools.discord_voice.knoxx_key(config);
var headers = ({"Content-Type": "application/json", "x-knoxx-user-email": "system-admin@open-hax.local"});
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key)))){
} else {
(headers["X-API-Key"] = api_key);
}

return headers;
});
knoxx.backend.tools.discord_voice.param_int = (function knoxx$backend$tools$discord_voice$param_int(params,key,camel,default$){
var raw = (function (){var or__5142__auto__ = (params[key]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params[camel]);
}
})();
var n = Number(raw);
if(cljs.core.truth_((function (){var or__5142__auto__ = (raw == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return isNaN(n);
}
})())){
return default$;
} else {
return n;
}
});
knoxx.backend.tools.discord_voice.fetch_tts_BANG_ = (function knoxx$backend$tools$discord_voice$fetch_tts_BANG_(config,text,voice_id,model_id){
var api_key = (function (){var or__5142__auto__ = knoxx.backend.tools.discord_voice.resolve_voice_key(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw (new Error("VOICE_GATEWAY_API_KEY not configured"));
}
})();
var body = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"input","input",556931961),text,new cljs.core.Keyword(null,"voice","voice",185716428),(function (){var or__5142__auto__ = voice_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "af_jessica";
}
})(),new cljs.core.Keyword(null,"model","model",331153215),(function (){var or__5142__auto__ = model_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "kokoro";
}
})(),new cljs.core.Keyword(null,"response_format","response_format",1229973741),"mp3",new cljs.core.Keyword(null,"speed","speed",1257663751),knoxx.backend.tools.discord_voice.default_tts_speed(config),new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),false], null);
return fetch(knoxx.backend.tools.discord_voice.tts_url(config),({"method": "POST", "headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key)), "Content-Type": "application/json", "Accept": "audio/mpeg"}), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (r){
if(cljs.core.truth_(r.ok)){
return r.arrayBuffer();
} else {
throw (new Error((""+"TTS "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(r.status))));
}
})).then((function (b){
return Buffer.from((new Uint8Array(b)));
}));
});
knoxx.backend.tools.discord_voice.parse_stt_json_text = (function knoxx$backend$tools$discord_voice$parse_stt_json_text(raw){
var s = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw)));
if(clojure.string.includes_QMARK_(s,"data:")){
var lines = cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (line){
var t = clojure.string.trim(line);
if(clojure.string.starts_with_QMARK_(t,"data:")){
return clojure.string.trim(cljs.core.subs.cljs$core$IFn$_invoke$arity$2(t,(5)));
} else {
return null;
}
}),clojure.string.split_lines(s));
var segments = cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (text){
if(cljs.core.seq(text)){
try{var j = JSON.parse(text);
var txt = (function (){var or__5142__auto__ = j.text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = j.transcription;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.seq(txt)){
return txt;
} else {
return null;
}
}catch (e58923){if((e58923 instanceof Error)){
var _ = e58923;
return null;
} else {
throw e58923;

}
}} else {
return null;
}
}),lines);
var final_segment = cljs.core.last(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (text){
if(cljs.core.seq(text)){
try{return JSON.parse(text);
}catch (e58936){if((e58936 instanceof Error)){
var _ = e58936;
return null;
} else {
throw e58936;

}
}} else {
return null;
}
}),lines));
var merged_text = clojure.string.trim(clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",segments));
return ({"text": merged_text, "final": (function (){var or__5142__auto__ = (cljs.core.truth_(final_segment)?final_segment.final:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return true;
}
})()});
} else {
return JSON.parse(s);
}
});
/**
 * Detect repetitive/garbage STT output (e.g. NPU KV-cache stuck).
 */
knoxx.backend.tools.discord_voice.stt_text_garbage_QMARK_ = (function knoxx$backend$tools$discord_voice$stt_text_garbage_QMARK_(text){
if(cljs.core.seq(text)){
var t = clojure.string.trim(text);
var and__5140__auto__ = (((t).length) > (10));
if(and__5140__auto__){
var and__5140__auto____$1 = (function (){var chars = cljs.core.set(cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p1__58940_SHARP_){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__58940_SHARP_," ")) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__58940_SHARP_,"\n")));
}),t));
return (cljs.core.count(chars) <= (2));
})();
if(and__5140__auto____$1){
return cljs.core.not(cljs.core.re_find(/[a-zA-Z0-9]/,t));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
} else {
return null;
}
});
knoxx.backend.tools.discord_voice.transcribe_BANG_ = (function knoxx$backend$tools$discord_voice$transcribe_BANG_(config,audio_buffer){
console.log("[voice:stt] === TRANSCRIBE START ===",audio_buffer.length,"bytes from",knoxx.backend.tools.discord_voice.stt_url(config));

console.log("[voice:stt] sending POST to",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.discord_voice.stt_url(config))+"/transcribe"));

return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.discord_voice.stt_url(config))+"/transcribe"),({"method": "POST", "headers": ({"Content-Type": "audio/wav", "Accept": "application/json, text/plain, text/event-stream"}), "body": audio_buffer})).then((function (r){
console.log("[voice:stt] response received, status:",r.status,"ok:",r.ok);

if(cljs.core.truth_(r.ok)){
console.log("[voice:stt] parsing response body");

return r.text();
} else {
console.error("[voice:stt] HTTP FAILED:",r.status);

throw (new Error((""+"STT "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(r.status))));
}
})).then((function (raw){
console.log("[voice:stt] raw body prefix:",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw)).slice((0),(80)));

var j = knoxx.backend.tools.discord_voice.parse_stt_json_text(raw);
console.log("[voice:stt] JSON parsed:",JSON.stringify(j));

var text = (function (){var or__5142__auto__ = j.text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = j.transcription;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.truth_(knoxx.backend.tools.discord_voice.stt_text_garbage_QMARK_(text))){
console.warn("[voice:stt] GARBAGE detected, discarding:",text.slice((0),(60)));

return "";
} else {
console.log("[voice:stt] extracted text:",((clojure.string.blank_QMARK_(text))?"[EMPTY]":text));

return text;
}
})).catch((function (err){
console.error("[voice:stt] === TRANSCRIBE ERROR ===",err.message);

throw err;
}));
});
knoxx.backend.tools.discord_voice.steer_BANG_ = (function knoxx$backend$tools$discord_voice$steer_BANG_(config,session_id,conversation_id,text){
console.log("[voice:steer] injecting into session:",session_id,"conv:",conversation_id,"text:",text.slice((0),(60)));

var body = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"message","message",-406056002),(""+"[Voice] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null);
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.discord_voice.knoxx_base(config))+"/api/knoxx/steer"),({"method": "POST", "headers": knoxx.backend.tools.discord_voice.knoxx_control_headers(config), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (r){
return r.text().then((function (raw){
if(cljs.core.truth_(r.ok)){
console.log("[voice:steer] ok");

return raw;
} else {
console.error("[voice:steer] failed:",r.status,raw);

throw (new Error((""+"Steer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(r.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw))));
}
}));
}));
});
knoxx.backend.tools.discord_voice.session_agent_spec = (function knoxx$backend$tools$discord_voice$session_agent_spec(session){
var or__5142__auto__ = new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(session);
}
});
knoxx.backend.tools.discord_voice.direct_start_voice_turn_BANG_ = (function knoxx$backend$tools$discord_voice$direct_start_voice_turn_BANG_(config,body){
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.discord_voice.knoxx_base(config))+"/api/knoxx/direct/start"),({"method": "POST", "headers": knoxx.backend.tools.discord_voice.knoxx_control_headers(config), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (r){
return r.text().then((function (raw){
if(cljs.core.truth_(r.ok)){
console.log("[voice:direct-start] ok");

return raw;
} else {
console.error("[voice:direct-start] failed:",r.status,raw);

throw (new Error((""+"Direct start "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(r.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw))));
}
}));
}));
});
knoxx.backend.tools.discord_voice.start_voice_turn_BANG_ = (function knoxx$backend$tools$discord_voice$start_voice_turn_BANG_(config,session_id,conversation_id,text){
console.log("[voice:direct-start] starting idle session:",session_id,"conv:",conversation_id);

return knoxx.backend.session_store.get_session(knoxx.backend.redis_client.get_client(),session_id).then((function (session){
var agent_spec = knoxx.backend.tools.discord_voice.session_agent_spec(session);
var body = (function (){var G__58991 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"message","message",-406056002),(""+"[Voice] "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text)),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null);
if(cljs.core.truth_(agent_spec)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58991,new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),agent_spec);
} else {
return G__58991;
}
})();
if(cljs.core.truth_(agent_spec)){
console.log("[voice:direct-start] resuming agent spec:",JSON.stringify(cljs.core.clj__GT_js(cljs.core.select_keys(agent_spec,new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contractId","contractId",710260199),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"model","model",331153215)], null)))));
} else {
}

return knoxx.backend.tools.discord_voice.direct_start_voice_turn_BANG_(config,body);
}));
});
knoxx.backend.tools.discord_voice.inactive_steer_error_QMARK_ = (function knoxx$backend$tools$discord_voice$inactive_steer_error_QMARK_(err){
var message = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message)));
return ((clojure.string.includes_QMARK_(message,"no active running turn")) || (((clojure.string.includes_QMARK_(message,"conversation is not active")) || (clojure.string.includes_QMARK_(message,"not active in the agent runtime")))));
});
knoxx.backend.tools.discord_voice.deliver_voice_text_BANG_ = (function knoxx$backend$tools$discord_voice$deliver_voice_text_BANG_(config,sid,cid,text){
return knoxx.backend.tools.discord_voice.steer_BANG_(config,sid,cid,text).catch((function (err){
if(knoxx.backend.tools.discord_voice.inactive_steer_error_QMARK_(err)){
console.log("[voice:deliver] steer target idle; starting a normal voice turn");

return knoxx.backend.tools.discord_voice.start_voice_turn_BANG_(config,sid,cid,text);
} else {
throw err;
}
}));
});

knoxx.backend.tools.discord_voice.voice_join_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord voice channel ID to join."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_join_execute = (function knoxx$backend$tools$discord_voice$voice_join_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var ch = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var map__59023 = (knoxx.backend.tools.discord_voice.resolve_session_context_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.discord_voice.resolve_session_context_BANG_.cljs$core$IFn$_invoke$arity$1(params) : knoxx.backend.tools.discord_voice.resolve_session_context_BANG_.call(null,params));
var map__59023__$1 = cljs.core.__destructure_map(map__59023);
var sid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59023__$1,new cljs.core.Keyword(null,"sid","sid",1815016414));
var cid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59023__$1,new cljs.core.Keyword(null,"cid","cid",-1940591320));
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(ch)){
throw (new Error("channel_id required"));
} else {
}

console.log("[voice:tool] discord.voice.join channel:",ch);

if(cljs.core.truth_((function (){var and__5140__auto__ = m;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.seq(sid)) && (cljs.core.seq(cid)));
} else {
return and__5140__auto__;
}
})())){
(m["__voiceSessionContext"] = ({"sessionId": sid, "conversationId": cid}));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Joining voice "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+"\u2026"));

return m.joinVoice(ch).then((function (r){
console.log("[voice:tool] joined voice, result:",JSON.stringify(r));

return knoxx.backend.text.tool_text_result((""+"Joined voice "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+" in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((r["guildId"]))),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(r,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
}));
});
knoxx.backend.tools.discord_voice.voice_join_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.join","Join Voice","Join a Discord voice channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Join a voice channel to enable voice features.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.join to connect to a voice channel.","Provide channel_id from discord.list.channels."], null)),knoxx.backend.tools.discord_voice.voice_join_params,knoxx.backend.tools.discord_voice.voice_join_execute], 0));
knoxx.backend.tools.discord_voice.voice_leave_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_leave_execute = (function knoxx$backend$tools$discord_voice$voice_leave_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(g)){
throw (new Error("guild_id required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Leaving voice "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+"\u2026"));

if(cljs.core.truth_(m)){
(knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_.cljs$core$IFn$_invoke$arity$1(m) : knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_.call(null,m));

var temp__5825__auto___59501 = (m["__voiceListener"]);
if(cljs.core.truth_(temp__5825__auto___59501)){
var sf_59502 = temp__5825__auto___59501;
try{(sf_59502.cljs$core$IFn$_invoke$arity$0 ? sf_59502.cljs$core$IFn$_invoke$arity$0() : sf_59502.call(null));
}catch (e59060){if((e59060 instanceof Error)){
var __59503 = e59060;
} else {
throw e59060;

}
}} else {
}

(m["__voiceListener"] = null);

(m["__voiceSessionContext"] = null);
} else {
}

return m.leaveVoice(g).then((function (r){
return knoxx.backend.text.tool_text_result((""+"Left voice in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(r,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
}));
});
knoxx.backend.tools.discord_voice.voice_leave_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.leave","Leave Voice","Leave a Discord voice channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Disconnect from a voice channel.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.leave to disconnect from a voice channel.","Provide the guild_id of the active connection."], null)),knoxx.backend.tools.discord_voice.voice_leave_params,knoxx.backend.tools.discord_voice.voice_leave_execute], 0));
knoxx.backend.tools.discord_voice.voice_say_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Text to synthesize and play in the voice channel."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"voice_id","voice_id",-725801774),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx/Kokoro voice ID. Default: af_jessica."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"model_id","model_id",-2010580717),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx model ID. Default: kokoro."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_say_execute = (function knoxx$backend$tools$discord_voice$voice_say_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var text = (function (){var or__5142__auto__ = (params["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var vi = (params["voice_id"]);
var mi = (params["model_id"]);
var listening_QMARK_ = cljs.core.boolean$((cljs.core.truth_(m)?(m["__voiceListener"]):null));
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(g)){
throw (new Error("guild_id required"));
} else {
}

if(clojure.string.blank_QMARK_(text)){
throw (new Error("text required"));
} else {
}

if(listening_QMARK_){
} else {
throw (new Error("Voice listener is not running. Use discord.voice.connect (preferred) before discord.voice.say."));
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"TTS: \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text.slice((0),(40)))+"\"\u2026"));

return knoxx.backend.tools.discord_voice.fetch_tts_BANG_(config,text,vi,mi).then((function (buf){
return m.playAudio(g,buf);
})).then((function (_){
return knoxx.backend.text.tool_text_result((""+"Playing in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+": \""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text.slice((0),(60)))+"\""),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"guildId","guildId",-559818490),g,new cljs.core.Keyword(null,"text","text",-1790561697),text,new cljs.core.Keyword(null,"played","played",-1713723590),true,new cljs.core.Keyword(null,"listening","listening",1028216980),true], null));
}));
});
knoxx.backend.tools.discord_voice.voice_say_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.say","Voice Say","Synthesize speech and play in a voice channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Speak text aloud in a connected voice channel.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.say to speak in a voice channel.","Must be connected via discord.voice.join first.","Provide guild_id and text. Optionally set voice_id and model_id."], null)),knoxx.backend.tools.discord_voice.voice_say_params,knoxx.backend.tools.discord_voice.voice_say_execute], 0));
knoxx.backend.tools.discord_voice.voice_status_params = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461)], null);
knoxx.backend.tools.discord_voice.voice_status_execute = (function knoxx$backend$tools$discord_voice$voice_status_execute(runtime,config,_tool_call_id,_params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Checking voice\u2026");

var c__$1 = m.getVoiceConnection();
var agent_loop = (m["__voiceAgentEventLoop"]);
return knoxx.backend.text.tool_text_result((cljs.core.truth_(c__$1)?(""+"Connected to guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = c__$1.__guildId;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return c__$1.guildId;
}
})())):"Not connected"),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"connected","connected",-169833045),(!((c__$1 == null))),new cljs.core.Keyword(null,"agentEventLoop","agentEventLoop",985359234),(!((agent_loop == null))),new cljs.core.Keyword(null,"queuedAudioWindows","queuedAudioWindows",-367314727),(cljs.core.truth_(agent_loop)?(function (){var or__5142__auto__ = (agent_loop["audioWindows"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})().length:null)], null));
});
knoxx.backend.tools.discord_voice.voice_status_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.status","Voice Status","Check voice connection status.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Check whether the bot is connected to a voice channel.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.status to check if the bot is in a voice channel.","No parameters required."], null)),knoxx.backend.tools.discord_voice.voice_status_params,knoxx.backend.tools.discord_voice.voice_status_execute], 0));
knoxx.backend.tools.discord_voice.voice_connect_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord voice channel ID to join."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Agent session ID to inject transcriptions into. Auto-detected if omitted."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Agent conversation ID for the session. Auto-detected if omitted."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_listen_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"session_id","session_id",1584799627),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Agent session ID to inject transcriptions into. Auto-detected if omitted."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Agent conversation ID for the session. Auto-detected if omitted."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
/**
 * Resolve (session-id, conversation-id) either from explicit params, the current agent context,
 * or the last known voice session context stored on the gateway manager.
 */
knoxx.backend.tools.discord_voice.resolve_session_context_BANG_ = (function knoxx$backend$tools$discord_voice$resolve_session_context_BANG_(params){
var explicit_sid = (function (){var or__5142__auto__ = ((clojure.string.blank_QMARK_((params["session_id"])))?null:(params["session_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(clojure.string.blank_QMARK_((params["sessionId"]))){
return null;
} else {
return (params["sessionId"]);
}
}
})();
var explicit_cid = (function (){var or__5142__auto__ = ((clojure.string.blank_QMARK_((params["conversation_id"])))?null:(params["conversation_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(clojure.string.blank_QMARK_((params["conversationId"]))){
return null;
} else {
return (params["conversationId"]);
}
}
})();
var agent_context = knoxx.backend.agent_context.get_context();
var stored_ctx = (function (){var G__59095 = knoxx.backend.tools.discord_voice.gw();
if((G__59095 == null)){
return null;
} else {
return (G__59095["__voiceSessionContext"]);
}
})();
var sid = (function (){var or__5142__auto__ = explicit_sid;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(agent_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (cljs.core.truth_(stored_ctx)?(stored_ctx["sessionId"]):null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})();
var cid = (function (){var or__5142__auto__ = explicit_cid;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(agent_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (cljs.core.truth_(stored_ctx)?(stored_ctx["conversationId"]):null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})();
console.log("[voice:tool] resolve-session-context explicit-sid:",explicit_sid,"agent-context:",(cljs.core.truth_(agent_context)?JSON.stringify(cljs.core.clj__GT_js(agent_context)):null),"stored-sid:",(cljs.core.truth_(stored_ctx)?(stored_ctx["sessionId"]):null),"resolved-sid:",sid);

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"sid","sid",1815016414),sid,new cljs.core.Keyword(null,"cid","cid",-1940591320),cid,new cljs.core.Keyword(null,"auto?","auto?",1295579150),((clojure.string.blank_QMARK_(explicit_sid)) && (clojure.string.blank_QMARK_(explicit_cid)))], null);
});
knoxx.backend.tools.discord_voice.ensure_session_context_BANG_ = (function knoxx$backend$tools$discord_voice$ensure_session_context_BANG_(sid,cid){
if(clojure.string.blank_QMARK_(sid)){
throw (new Error((""+"session_id required (auto-detect failed; no active agent turn context). "+"If calling manually, provide session_id and conversation_id explicitly.")));
} else {
}

if(clojure.string.blank_QMARK_(cid)){
throw (new Error((""+"conversation_id required (auto-detect failed; no active agent turn context). "+"If calling manually, provide session_id and conversation_id explicitly.")));
} else {
return null;
}
});
/**
 * Send accumulated transcription text for a user as a single steer.
 */
knoxx.backend.tools.discord_voice.flush_voice_buffer_BANG_ = (function knoxx$backend$tools$discord_voice$flush_voice_buffer_BANG_(config,sid,cid,uid){
var m = knoxx.backend.tools.discord_voice.gw();
var buf_obj = (cljs.core.truth_(m)?(m["__voiceTranscriptionBuffer"]):null);
var user_buf = (cljs.core.truth_(buf_obj)?(buf_obj[uid]):null);
if(cljs.core.truth_(user_buf)){
var texts = (user_buf["texts"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = texts;
if(cljs.core.truth_(and__5140__auto__)){
return texts.length;
} else {
return and__5140__auto__;
}
})())){
var merged_59556 = clojure.string.trim(clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",Array.from(texts)));
console.log("[voice:tool] >>> FLUSHING buffer for",uid,"concatenated:",((clojure.string.blank_QMARK_(merged_59556))?"[EMPTY]":merged_59556));

if(clojure.string.blank_QMARK_(merged_59556)){
} else {
knoxx.backend.tools.discord_voice.deliver_voice_text_BANG_(config,sid,cid,merged_59556).catch((function (e){
return console.error("[voice:tool] voice delivery FAILED for",uid,":",e.message);
}));
}
} else {
}

(user_buf["texts"] = []);

return (user_buf["timer"] = null);
} else {
return null;
}
});
knoxx.backend.tools.discord_voice.audio_window_content_part = (function knoxx$backend$tools$discord_voice$audio_window_content_part(uid,audio_buffer){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),"audio",new cljs.core.Keyword(null,"data","data",-232669377),audio_buffer.toString("base64"),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),"audio/wav",new cljs.core.Keyword(null,"filename","filename",-1428840783),(""+"discord-voice-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(uid)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())+".wav"),new cljs.core.Keyword(null,"bytes","bytes",1175866680),audio_buffer.length], null);
});
knoxx.backend.tools.discord_voice.requeue_front_BANG_ = (function knoxx$backend$tools$discord_voice$requeue_front_BANG_(queue,windows){
var seq__59121 = cljs.core.seq(cljs.core.reverse(windows));
var chunk__59122 = null;
var count__59123 = (0);
var i__59124 = (0);
while(true){
if((i__59124 < count__59123)){
var window__$1 = chunk__59122.cljs$core$IIndexed$_nth$arity$2(null,i__59124);
queue.unshift(window__$1);


var G__59563 = seq__59121;
var G__59564 = chunk__59122;
var G__59565 = count__59123;
var G__59566 = (i__59124 + (1));
seq__59121 = G__59563;
chunk__59122 = G__59564;
count__59123 = G__59565;
i__59124 = G__59566;
continue;
} else {
var temp__5825__auto__ = cljs.core.seq(seq__59121);
if(temp__5825__auto__){
var seq__59121__$1 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(seq__59121__$1)){
var c__5673__auto__ = cljs.core.chunk_first(seq__59121__$1);
var G__59567 = cljs.core.chunk_rest(seq__59121__$1);
var G__59568 = c__5673__auto__;
var G__59569 = cljs.core.count(c__5673__auto__);
var G__59570 = (0);
seq__59121 = G__59567;
chunk__59122 = G__59568;
count__59123 = G__59569;
i__59124 = G__59570;
continue;
} else {
var window__$1 = cljs.core.first(seq__59121__$1);
queue.unshift(window__$1);


var G__59572 = cljs.core.next(seq__59121__$1);
var G__59573 = null;
var G__59574 = (0);
var G__59575 = (0);
seq__59121 = G__59572;
chunk__59122 = G__59573;
count__59123 = G__59574;
i__59124 = G__59575;
continue;
}
} else {
return null;
}
}
break;
}
});
knoxx.backend.tools.discord_voice.trigger_agent_voice_event_BANG_ = (function knoxx$backend$tools$discord_voice$trigger_agent_voice_event_BANG_(config,loop_state,windows){
var guild_id = (loop_state["guildId"]);
var channel_id = (loop_state["channelId"]);
var event_id = (""+"discord-voice-audio-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now()));
var body = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),event_id,new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"discord",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"discord.voice.audio.window",new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord.voice.audio.window"], null),new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"guildId","guildId",-559818490),guild_id,new cljs.core.Keyword(null,"channelId","channelId",2082229448),channel_id,new cljs.core.Keyword(null,"authorId","authorId",-1664154012),"discord-voice-room",new cljs.core.Keyword(null,"content","content",15833224),"Raw Discord voice audio window(s) are attached. Do not require ASR; perceive the audio directly if the model supports it.",new cljs.core.Keyword(null,"summary","summary",380847952),(""+"Discord voice audio event with "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(windows))+" window(s)."),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),windows], null)], null);
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.discord_voice.knoxx_base(config))+"/api/admin/config/events/dispatch"),({"method": "POST", "headers": knoxx.backend.tools.discord_voice.knoxx_control_headers(config), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
})).then((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"event_id","event_id",-767275570),event_id,new cljs.core.Keyword(null,"windows","windows",2068861701),cljs.core.count(windows)], null);
}));
});
knoxx.backend.tools.discord_voice.run_agent_event_loop_step_BANG_ = (function knoxx$backend$tools$discord_voice$run_agent_event_loop_step_BANG_(config,loop_state){
var queue = (function (){var or__5142__auto__ = (loop_state["audioWindows"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
if(cljs.core.truth_((function (){var or__5142__auto__ = (loop_state["running"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (queue.length === (0));
}
})())){
return Promise.resolve(null);
} else {
var max_windows = (loop_state["maxWindowsPerTurn"]);
var n = cljs.core.min.cljs$core$IFn$_invoke$arity$2(queue.length,max_windows);
var windows = cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(queue.splice((0),n)));
(loop_state["running"] = true);

return knoxx.backend.tools.discord_voice.trigger_agent_voice_event_BANG_(config,loop_state,windows).catch((function (err){
var message = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message));
var busy_QMARK_ = ((clojure.string.includes_QMARK_(message,"agent_already_processing")) || (clojure.string.includes_QMARK_(message,"already processing")));
if(busy_QMARK_){
knoxx.backend.tools.discord_voice.requeue_front_BANG_(queue,windows);
} else {
}

return console.log("[voice:agent-event] trigger not accepted:",message);
})).finally((function (){
return (loop_state["running"] = false);
}));
}
});
knoxx.backend.tools.discord_voice.schedule_agent_event_loop_BANG_ = (function knoxx$backend$tools$discord_voice$schedule_agent_event_loop_BANG_(config,m,loop_state){
if(cljs.core.truth_((loop_state["stopped"]))){
return null;
} else {
return (loop_state["timer"] = setTimeout((function (){
return knoxx.backend.tools.discord_voice.run_agent_event_loop_step_BANG_(config,loop_state).finally((function (){
return (knoxx.backend.tools.discord_voice.schedule_agent_event_loop_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.tools.discord_voice.schedule_agent_event_loop_BANG_.cljs$core$IFn$_invoke$arity$3(config,m,loop_state) : knoxx.backend.tools.discord_voice.schedule_agent_event_loop_BANG_.call(null,config,m,loop_state));
}));
}),(loop_state["tickMs"])));
}
});
knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_ = (function knoxx$backend$tools$discord_voice$stop_agent_event_loop_BANG_(m){
var temp__5825__auto__ = (cljs.core.truth_(m)?(m["__voiceAgentEventLoop"]):null);
if(cljs.core.truth_(temp__5825__auto__)){
var loop_state = temp__5825__auto__;
(loop_state["stopped"] = true);

var temp__5825__auto___59606__$1 = (loop_state["timer"]);
if(cljs.core.truth_(temp__5825__auto___59606__$1)){
var timer_59607 = temp__5825__auto___59606__$1;
clearTimeout(timer_59607);
} else {
}

var temp__5825__auto___59608__$1 = (loop_state["listenerStop"]);
if(cljs.core.truth_(temp__5825__auto___59608__$1)){
var stop_59609 = temp__5825__auto___59608__$1;
try{(stop_59609.cljs$core$IFn$_invoke$arity$0 ? stop_59609.cljs$core$IFn$_invoke$arity$0() : stop_59609.call(null));
}catch (e59185){if((e59185 instanceof Error)){
var __59610 = e59185;
} else {
throw e59185;

}
}} else {
}

(m["__voiceAgentEventLoop"] = null);

return (m["__voiceListener"] = null);
} else {
return null;
}
});
knoxx.backend.tools.discord_voice.start_agent_event_voice_listener_BANG_ = (function knoxx$backend$tools$discord_voice$start_agent_event_voice_listener_BANG_(config,m,g,ch,sid,cid,auto_QMARK_,params,on_update){
var tick_ms = cljs.core.max.cljs$core$IFn$_invoke$arity$2((250),knoxx.backend.tools.discord_voice.param_int(params,"tick_ms","tickMs",(1000)));
var max_windows = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = knoxx.backend.tools.discord_voice.param_int(params,"max_windows_per_event","maxWindowsPerEvent",null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.discord_voice.param_int(params,"max_windows_per_turn","maxWindowsPerTurn",(3));
}
})());
var model_id = (function (){var or__5142__auto__ = (params["model_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["modelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_(m);

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Listening in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+" as agent-event voice trigger\u2026"));

console.log("[voice:tool] discord.voice.listen agent-event guild:",g,"session:",sid,"conv:",cid,"auto-detect?",auto_QMARK_);

return m.startVoiceListener(g,(function (uid){
return console.log("[voice:agent-event] speaker start:",uid);
}),(function (uid,buf){
console.log("[voice:agent-event] audio window:",uid,"bytes:",buf.length);

var temp__5825__auto__ = (m["__voiceAgentEventLoop"]);
if(cljs.core.truth_(temp__5825__auto__)){
var loop_state = temp__5825__auto__;
return (loop_state["audioWindows"]).push(knoxx.backend.tools.discord_voice.audio_window_content_part(uid,buf));
} else {
return null;
}
})).then((function (stop){
var loop_state = ({"listenerStop": stop, "sessionId": sid, "guildId": g, "channelId": ch, "modelId": model_id, "audioWindows": [], "running": false, "stopped": false, "tickMs": tick_ms, "maxWindowsPerTurn": max_windows, "conversationId": cid});
(m["__voiceAgentEventLoop"] = loop_state);

(m["__voiceListener"] = stop);

knoxx.backend.tools.discord_voice.schedule_agent_event_loop_BANG_(config,m,loop_state);

return knoxx.backend.text.tool_text_result((""+"Listening in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+" as agent-event trigger for session "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sid)),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"guildId","guildId",-559818490),g,new cljs.core.Keyword(null,"listening","listening",1028216980),true,new cljs.core.Keyword(null,"mode","mode",654403691),"agent_event",new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),sid,new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),cid,new cljs.core.Keyword(null,"tickMs","tickMs",-947024718),tick_ms], null));
}));
});
knoxx.backend.tools.discord_voice.start_asr_steer_voice_listener_BANG_ = (function knoxx$backend$tools$discord_voice$start_asr_steer_voice_listener_BANG_(config,m,g,sid,auto_QMARK_,steer_debounce_ms,on_update){
knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_(m);

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Listening in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+"\u2026"));

console.log("[voice:tool] discord.voice.listen guild:",g,"session:",sid,"auto-detect?",auto_QMARK_);

return m.startVoiceListener(g,(function (uid){
console.log("[voice:tool] >>> on-start callback fired for user:",uid);

var buf_obj = (cljs.core.truth_(m)?(m["__voiceTranscriptionBuffer"]):null);
var user_buf = (cljs.core.truth_(buf_obj)?(buf_obj[uid]):null);
if(cljs.core.truth_(user_buf)){
var temp__5825__auto__ = (user_buf["timer"]);
if(cljs.core.truth_(temp__5825__auto__)){
var t = temp__5825__auto__;
clearTimeout(t);

return (user_buf["timer"] = null);
} else {
return null;
}
} else {
return null;
}
}),(function (uid,buf){
console.log("[voice:tool] >>> on-audio callback fired for user:",uid,"buffer length:",buf.length,"bytes");

return knoxx.backend.tools.discord_voice.transcribe_BANG_(config,buf).then((function (t){
console.log("[voice:tool] transcription result for",uid,":",((clojure.string.blank_QMARK_(t))?"[EMPTY]":t));

if(clojure.string.blank_QMARK_(t)){
return null;
} else {
var buf_obj = (cljs.core.truth_(m)?(m["__voiceTranscriptionBuffer"]):null);
var _ = (cljs.core.truth_((function (){var and__5140__auto__ = m;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not(buf_obj);
} else {
return and__5140__auto__;
}
})())?(m["__voiceTranscriptionBuffer"] = ({})):null);
var buf_obj__$1 = (function (){var or__5142__auto__ = buf_obj;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (m["__voiceTranscriptionBuffer"]);
}
})();
var user_buf = (function (){var or__5142__auto__ = (buf_obj__$1[uid]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({"texts": [], "timer": null});
}
})();
(user_buf["texts"]).push(t);

(buf_obj__$1[uid] = user_buf);

var temp__5825__auto___59622 = (user_buf["timer"]);
if(cljs.core.truth_(temp__5825__auto___59622)){
var old_timer_59623 = temp__5825__auto___59622;
clearTimeout(old_timer_59623);
} else {
}

var new_timer = setTimeout((function (){
return knoxx.backend.tools.discord_voice.flush_voice_buffer_BANG_(config,sid,((m["__voiceSessionContext"])["conversationId"]),uid);
}),steer_debounce_ms);
return (user_buf["timer"] = new_timer);
}
})).catch((function (e){
return console.error("[voice:tool] transcription/steering pipeline FAILED for",uid,":",e.message);
}));
})).then((function (stop){
(m["__voiceListener"] = stop);

return knoxx.backend.text.tool_text_result((""+"Listening in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)+". Transcriptions \u2192 session "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(sid)),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"guildId","guildId",-559818490),g,new cljs.core.Keyword(null,"listening","listening",1028216980),true,new cljs.core.Keyword(null,"mode","mode",654403691),"asr_steer"], null));
}));
});
knoxx.backend.tools.discord_voice.voice_listen_execute = (function knoxx$backend$tools$discord_voice$voice_listen_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var map__59263 = knoxx.backend.tools.discord_voice.resolve_session_context_BANG_(params);
var map__59263__$1 = cljs.core.__destructure_map(map__59263);
var sid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59263__$1,new cljs.core.Keyword(null,"sid","sid",1815016414));
var cid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59263__$1,new cljs.core.Keyword(null,"cid","cid",-1940591320));
var auto_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59263__$1,new cljs.core.Keyword(null,"auto?","auto?",1295579150));
var steer_debounce_ms = (1500);
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(g)){
throw (new Error("guild_id required"));
} else {
}

knoxx.backend.tools.discord_voice.ensure_session_context_BANG_(sid,cid);

(m["__voiceSessionContext"] = ({"sessionId": sid, "conversationId": cid}));

if(cljs.core.truth_((m["__voiceTranscriptionBuffer"]))){
} else {
(m["__voiceTranscriptionBuffer"] = ({}));
}

return knoxx.backend.tools.discord_voice.start_asr_steer_voice_listener_BANG_(config,m,g,sid,auto_QMARK_,steer_debounce_ms,on_update);
});
knoxx.backend.tools.discord_voice.voice_connect_execute = (function knoxx$backend$tools$discord_voice$voice_connect_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var ch = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var map__59290 = knoxx.backend.tools.discord_voice.resolve_session_context_BANG_(params);
var map__59290__$1 = cljs.core.__destructure_map(map__59290);
var sid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59290__$1,new cljs.core.Keyword(null,"sid","sid",1815016414));
var cid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59290__$1,new cljs.core.Keyword(null,"cid","cid",-1940591320));
var auto_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59290__$1,new cljs.core.Keyword(null,"auto?","auto?",1295579150));
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(ch)){
throw (new Error("channel_id required"));
} else {
}

knoxx.backend.tools.discord_voice.ensure_session_context_BANG_(sid,cid);

if(cljs.core.truth_(m)){
(m["__voiceSessionContext"] = ({"sessionId": sid, "conversationId": cid}));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Connecting voice + listener for channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+"\u2026"));

return m.joinVoice(ch).then((function (r){
var guild_id = (function (){var or__5142__auto__ = (r["guildId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(guild_id)){
throw (new Error("joinVoice did not return guildId"));
} else {
}

console.log("[voice:tool] discord.voice.connect joined",ch,"guild",guild_id,"auto-detect?",auto_QMARK_);

return knoxx.backend.tools.discord_voice.voice_listen_execute(runtime,config,_tool_call_id,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),guild_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),sid,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),cid], null)),a,b,c);
})).then((function (_){
return knoxx.backend.text.tool_text_result((""+"Connected to voice "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+" and listening"),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),ch,new cljs.core.Keyword(null,"listening","listening",1028216980),true,new cljs.core.Keyword(null,"sessionId","sessionId",1640410629),sid,new cljs.core.Keyword(null,"conversationId","conversationId",-981028996),cid], null));
}));
});
knoxx.backend.tools.discord_voice.voice_connect_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.connect","Voice Connect","Join a Discord voice channel and start listening/transcription.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Join voice + enable voice-to-text transcription in one operation.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.connect as the default voice entrypoint.","Provide channel_id. session_id and conversation_id are auto-detected when called during an agent run.","This will join the channel, then start listening in the resulting guild."], null)),knoxx.backend.tools.discord_voice.voice_connect_params,knoxx.backend.tools.discord_voice.voice_connect_execute], 0));
knoxx.backend.tools.discord_voice.voice_listen_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.listen","Voice Listen","Listen for user speech and transcribe into agent session.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Start listening for voice input and transcribe speech to text.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.listen only when already connected via discord.voice.join.","Provide guild_id. session_id and conversation_id are auto-detected.","Transcriptions are steered into the agent session automatically."], null)),knoxx.backend.tools.discord_voice.voice_listen_params,knoxx.backend.tools.discord_voice.voice_listen_execute], 0));
knoxx.backend.tools.discord_voice.voice_agent_event_connect_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Discord voice channel ID to join and emit raw-audio events from."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tick_ms","tick_ms",999628236),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Mechanical event-dispatch cadence in ms. Default 1000."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"max_windows_per_event","max_windows_per_event",-947356916),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum queued WAV windows per emitted event. Default 3."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null);
knoxx.backend.tools.discord_voice.voice_agent_event_listen_params = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voice channel ID used as the event owner/filter key."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tick_ms","tick_ms",999628236),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Mechanical event-dispatch cadence in ms. Default 1000."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"max_windows_per_event","max_windows_per_event",-947356916),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum queued WAV windows per emitted event. Default 3."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null);
knoxx.backend.tools.discord_voice.voice_agent_event_listen_execute = (function knoxx$backend$tools$discord_voice$voice_agent_event_listen_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var ch = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var map__59329 = knoxx.backend.tools.discord_voice.resolve_session_context_BANG_(params);
var map__59329__$1 = cljs.core.__destructure_map(map__59329);
var sid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59329__$1,new cljs.core.Keyword(null,"sid","sid",1815016414));
var cid = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59329__$1,new cljs.core.Keyword(null,"cid","cid",-1940591320));
var auto_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59329__$1,new cljs.core.Keyword(null,"auto?","auto?",1295579150));
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(g)){
throw (new Error("guild_id required"));
} else {
}

return knoxx.backend.tools.discord_voice.start_agent_event_voice_listener_BANG_(config,m,g,ch,sid,cid,auto_QMARK_,params,on_update);
});
knoxx.backend.tools.discord_voice.voice_agent_event_connect_execute = (function knoxx$backend$tools$discord_voice$voice_agent_event_connect_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var ch = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(ch)){
throw (new Error("channel_id required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Connecting voice event source for channel "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+"\u2026"));

return m.joinVoice(ch).then((function (r){
var guild_id = (function (){var or__5142__auto__ = (r["guildId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(guild_id)){
throw (new Error("joinVoice did not return guildId"));
} else {
}

return knoxx.backend.tools.discord_voice.voice_agent_event_listen_execute(runtime,config,_tool_call_id,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),guild_id,new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),ch,new cljs.core.Keyword(null,"tick_ms","tick_ms",999628236),(function (){var or__5142__auto__ = (params["tick_ms"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["tickMs"]);
}
})(),new cljs.core.Keyword(null,"max_windows_per_event","max_windows_per_event",-947356916),(function (){var or__5142__auto__ = (params["max_windows_per_event"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (params["maxWindowsPerEvent"]);
}
})()], null)),a,b,c);
}));
});
knoxx.backend.tools.discord_voice.voice_agent_event_connect_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.agent_event.connect","Voice Agent Event Connect","Join voice and emit raw Discord audio as real Knoxx event-agent events.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Contract-only voice event source for agents that perceive raw audio through event-agent runs; no ASR and no direct provider loop.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["This is for contracts that explicitly grant :cap/voice-audio-event.","It emits discord.voice.audio.window events; matching event-agent contracts decide what to do.","Do not use this as a general replacement for discord.voice.connect."], null)),knoxx.backend.tools.discord_voice.voice_agent_event_connect_params,knoxx.backend.tools.discord_voice.voice_agent_event_connect_execute], 0));
knoxx.backend.tools.discord_voice.voice_agent_event_listen_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.agent_event.listen","Voice Agent Event Listen","Emit raw Discord audio as real Knoxx event-agent events for an existing voice connection.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Contract-only voice event source; no ASR and no direct provider loop.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use only after joining voice.","Emits discord.voice.audio.window events for matching event-agent contracts."], null)),knoxx.backend.tools.discord_voice.voice_agent_event_listen_params,knoxx.backend.tools.discord_voice.voice_agent_event_listen_execute], 0));
knoxx.backend.tools.discord_voice.voice_stop_listen_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_stop_listen_execute = (function knoxx$backend$tools$discord_voice$voice_stop_listen_execute(runtime,config,_tool_call_id,params,a,b,c){
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.truth_(m)){
knoxx.backend.tools.discord_voice.stop_agent_event_loop_BANG_(m);

var temp__5825__auto___59645 = (m["__voiceListener"]);
if(cljs.core.truth_(temp__5825__auto___59645)){
var sf_59646 = temp__5825__auto___59645;
(sf_59646.cljs$core$IFn$_invoke$arity$0 ? sf_59646.cljs$core$IFn$_invoke$arity$0() : sf_59646.call(null));
} else {
}

(m["__voiceListener"] = null);

(m["__voiceSessionContext"] = null);
} else {
}

return knoxx.backend.text.tool_text_result((""+"Stopped listening in guild "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(g)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"guildId","guildId",-559818490),g,new cljs.core.Keyword(null,"listening","listening",1028216980),false], null));
});
knoxx.backend.tools.discord_voice.voice_stop_listen_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.stop_listen","Stop Voice Listen","Stop listening for voice input.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Stop the active voice listener in a guild.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.stop_listen to stop voice transcription.","Provide the guild_id of the active listener."], null)),knoxx.backend.tools.discord_voice.voice_stop_listen_params,knoxx.backend.tools.discord_voice.voice_stop_listen_execute], 0));
knoxx.backend.tools.discord_voice.voice_list_members_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"guild_id","guild_id",-2139504959),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Guild ID with an active voice connection."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"channel_id","channel_id",1180018383),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Voice channel ID to list members of."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.discord_voice.voice_list_members_execute = (function knoxx$backend$tools$discord_voice$voice_list_members_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var m = knoxx.backend.tools.discord_voice.gw();
var g = (function (){var or__5142__auto__ = (params["guild_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["guildId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var ch = (function (){var or__5142__auto__ = (params["channel_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (params["channelId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(cljs.core.truth_(m)){
} else {
throw (new Error("Gateway not started"));
}

if(clojure.string.blank_QMARK_(g)){
throw (new Error("guild_id required"));
} else {
}

if(clojure.string.blank_QMARK_(ch)){
throw (new Error("channel_id required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Listing voice members in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+"\u2026"));

return m.listVoiceMembers(g,ch).then((function (members){
var ms = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(members,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var lines = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (m__$1){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"isBot","isBot",-56412981).cljs$core$IFn$_invoke$arity$1(m__$1))?"[bot] ":""))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"displayName","displayName",-809144601).cljs$core$IFn$_invoke$arity$1(m__$1))+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(m__$1))+")");
}),ms);
return knoxx.backend.text.tool_text_result((""+"Voice members in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ch)+":\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",lines))),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),ch,new cljs.core.Keyword(null,"members","members",159001018),ms,new cljs.core.Keyword(null,"count","count",2139924085),cljs.core.count(ms)], null));
}));
});
knoxx.backend.tools.discord_voice.voice_list_members_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"discord.voice.list_members","List Voice Members","List members currently in a voice channel.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["List who is in a voice channel.",cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use discord.voice.list_members to see who is in a voice channel.","Provide guild_id and channel_id."], null)),knoxx.backend.tools.discord_voice.voice_list_members_params,knoxx.backend.tools.discord_voice.voice_list_members_execute], 0));
knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools = (function knoxx$backend$tools$discord_voice$create_discord_voice_custom_tools(var_args){
var G__59403 = arguments.length;
switch (G__59403) {
case 2:
return knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var ok_QMARK_ = (function (id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [((ok_QMARK_("discord.voice.join"))?knoxx.backend.tools.discord_voice.voice_join_tool(runtime,config):null),((ok_QMARK_("discord.voice.leave"))?knoxx.backend.tools.discord_voice.voice_leave_tool(runtime,config):null),((ok_QMARK_("discord.voice.say"))?knoxx.backend.tools.discord_voice.voice_say_tool(runtime,config):null),((ok_QMARK_("discord.voice.status"))?knoxx.backend.tools.discord_voice.voice_status_tool(runtime,config):null),((ok_QMARK_("discord.voice.connect"))?knoxx.backend.tools.discord_voice.voice_connect_tool(runtime,config):null),((ok_QMARK_("discord.voice.listen"))?knoxx.backend.tools.discord_voice.voice_listen_tool(runtime,config):null),((ok_QMARK_("discord.voice.agent_event.connect"))?knoxx.backend.tools.discord_voice.voice_agent_event_connect_tool(runtime,config):null),((ok_QMARK_("discord.voice.agent_event.listen"))?knoxx.backend.tools.discord_voice.voice_agent_event_listen_tool(runtime,config):null),((ok_QMARK_("discord.voice.stop_listen"))?knoxx.backend.tools.discord_voice.voice_stop_listen_tool(runtime,config):null),((ok_QMARK_("discord.voice.list_members"))?knoxx.backend.tools.discord_voice.voice_list_members_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.discord_voice.create_discord_voice_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.discord_voice.js.map
