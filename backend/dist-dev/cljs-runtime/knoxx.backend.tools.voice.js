import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./promesa.core.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.openutau.js";
import "./knoxx.backend.tools.shared.js";
import "./knoxx.backend.document_state.js";
goog.provide('knoxx.backend.tools.voice');
knoxx.backend.tools.voice.blank__GT_nil = (function knoxx$backend$tools$voice$blank__GT_nil(v){
var s = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = v;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(s)){
return null;
} else {
return s;
}
});
knoxx.backend.tools.voice.config_value = (function knoxx$backend$tools$voice$config_value(config,keyword_key,js_key,camel_key){
var or__5142__auto__ = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(config,keyword_key):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.map_QMARK_(config))?null:(config[js_key]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.map_QMARK_(config)){
return null;
} else {
return (config[camel_key]);
}
}
}
});
knoxx.backend.tools.voice.false_like_QMARK_ = (function knoxx$backend$tools$voice$false_like_QMARK_(v){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(false,v)) || (cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["off",null,"false",null,"0",null,"no",null], null), null),clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v)))))));
});
knoxx.backend.tools.voice.bool_value = (function knoxx$backend$tools$voice$bool_value(v,default$){
if((v == null)){
return default$;
} else {
return (!(knoxx.backend.tools.voice.false_like_QMARK_(v)));
}
});
knoxx.backend.tools.voice.config_bool_value = (function knoxx$backend$tools$voice$config_bool_value(config,keyword_key,js_key,camel_key,default$){
var v = ((cljs.core.map_QMARK_(config))?cljs.core.get.cljs$core$IFn$_invoke$arity$3(config,keyword_key,new cljs.core.Keyword("knoxx.backend.tools.voice","missing","knoxx.backend.tools.voice/missing",888295804)):(function (){var kebab = (config[js_key]);
var camel = (config[camel_key]);
if((!((kebab == null)))){
return kebab;
} else {
if((!((camel == null)))){
return camel;
} else {
return new cljs.core.Keyword("knoxx.backend.tools.voice","missing","knoxx.backend.tools.voice/missing",888295804);

}
}
})());
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword("knoxx.backend.tools.voice","missing","knoxx.backend.tools.voice/missing",888295804),v)){
return default$;
} else {
return knoxx.backend.tools.voice.bool_value(v,default$);
}
});
knoxx.backend.tools.voice.default_tts_speed = (function knoxx$backend$tools$voice$default_tts_speed(config){
var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-default-speed","voxx-default-speed",-370827943),"voxx-default-speed","voxxDefaultSpeed"));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__59208 = process;
var G__59208__$1 = (((G__59208 == null))?null:G__59208.env);
var G__59208__$2 = (((G__59208__$1 == null))?null:(G__59208__$1["KNOXX_VOXX_DEFAULT_SPEED"]));
if((G__59208__$2 == null)){
return null;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(G__59208__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__59209 = process;
var G__59209__$1 = (((G__59209 == null))?null:G__59209.env);
var G__59209__$2 = (((G__59209__$1 == null))?null:(G__59209__$1["VOICE_GATEWAY_TTS_DEFAULT_SPEED"]));
if((G__59209__$2 == null)){
return null;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(G__59209__$2);
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
knoxx.backend.tools.voice.resolve_voice_key = (function knoxx$backend$tools$voice$resolve_voice_key(config){
var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-api-key","voxx-api-key",2053708716),"voxx-api-key","voxxApiKey"));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__59218 = process;
var G__59218__$1 = (((G__59218 == null))?null:G__59218.env);
var G__59218__$2 = (((G__59218__$1 == null))?null:(G__59218__$1["VOICE_GATEWAY_API_KEY"]));
if((G__59218__$2 == null)){
return null;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(G__59218__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var G__59223 = process;
var G__59223__$1 = (((G__59223 == null))?null:G__59223.env);
var G__59223__$2 = (((G__59223__$1 == null))?null:(G__59223__$1["KNOXX_VOICE_GATEWAY_API_KEY"]));
if((G__59223__$2 == null)){
return null;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(G__59223__$2);
}
}
}
});
knoxx.backend.tools.voice.voice_gateway_url = (function knoxx$backend$tools$voice$voice_gateway_url(config){
var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-url","voxx-url",-1259052170),"voxx-url","voxxUrl"));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__59231 = process;
var G__59231__$1 = (((G__59231 == null))?null:G__59231.env);
var G__59231__$2 = (((G__59231__$1 == null))?null:(G__59231__$1["VOXX_URL"]));
if((G__59231__$2 == null)){
return null;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(G__59231__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "http://127.0.0.1:8787";
}
}
});
knoxx.backend.tools.voice.tts_url = (function knoxx$backend$tools$voice$tts_url(config){
var base = clojure.string.replace(knoxx.backend.tools.voice.voice_gateway_url(config),/\/+$/,"");
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
knoxx.backend.tools.voice.voice_settings_payload = (function knoxx$backend$tools$voice$voice_settings_payload(params){
var G__59246 = cljs.core.PersistentArrayMap.EMPTY;
var G__59246__$1 = (cljs.core.truth_((params["stability"]))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59246,new cljs.core.Keyword(null,"stability","stability",1733225509),(params["stability"])):G__59246);
if(cljs.core.truth_((params["similarity_boost"]))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59246__$1,new cljs.core.Keyword(null,"similarity_boost","similarity_boost",866821960),(params["similarity_boost"]));
} else {
return G__59246__$1;
}
});
knoxx.backend.tools.voice.tts_body = (function knoxx$backend$tools$voice$tts_body(text,voice_id,model_id,output_format,params,options){
var vs = knoxx.backend.tools.voice.voice_settings_payload(params);
var map__59259 = options;
var map__59259__$1 = cljs.core.__destructure_map(map__59259);
var speed = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59259__$1,new cljs.core.Keyword(null,"speed","speed",1257663751));
var postprocess_profile = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59259__$1,new cljs.core.Keyword(null,"postprocess-profile","postprocess-profile",-115988175));
var postprocess_enabled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59259__$1,new cljs.core.Keyword(null,"postprocess-enabled","postprocess-enabled",76184778));
var prompt_aware = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59259__$1,new cljs.core.Keyword(null,"prompt-aware","prompt-aware",464266766));
var prompt_aware_style = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59259__$1,new cljs.core.Keyword(null,"prompt-aware-style","prompt-aware-style",72282946));
var G__59265 = new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"input","input",556931961),text,new cljs.core.Keyword(null,"voice","voice",185716428),voice_id,new cljs.core.Keyword(null,"model","model",331153215),model_id,new cljs.core.Keyword(null,"response_format","response_format",1229973741),output_format,new cljs.core.Keyword(null,"speed","speed",1257663751),speed,new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),postprocess_enabled,new cljs.core.Keyword(null,"prompt_aware","prompt_aware",1309007496),prompt_aware], null);
var G__59265__$1 = (cljs.core.truth_(postprocess_profile)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59265,new cljs.core.Keyword(null,"postprocess_profile","postprocess_profile",-1254686835),postprocess_profile):G__59265);
var G__59265__$2 = (cljs.core.truth_(prompt_aware_style)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59265__$1,new cljs.core.Keyword(null,"prompt_aware_style","prompt_aware_style",1965441274),prompt_aware_style):G__59265__$1);
if(cljs.core.seq(vs)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59265__$2,new cljs.core.Keyword(null,"voice_settings","voice_settings",160567748),vs);
} else {
return G__59265__$2;
}
});
/**
 * Generate a default output path in Voice/ when none is provided.
 */
knoxx.backend.tools.voice.tts_default_output_path = (function knoxx$backend$tools$voice$tts_default_output_path(){
var ts = (new Date()).toISOString();
var safe_ts = clojure.string.replace(ts,/[:.]/,"-");
return (""+"Voice/tts-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(safe_ts)+".mp3");
});
knoxx.backend.tools.voice.tts_rest_params = new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Plain text. Strip markdown first."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"output_path","output_path",-1715585288),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Workspace-relative output path. Defaults to Voice/tts-<timestamp>.mp3. Use Voice/ for spoken output, Audio/ for clips and effects, Music/ for musical content."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"voice_id","voice_id",-725801774),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx/Kokoro voice ID. Default: af_jessica."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"model_id","model_id",-2010580717),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx backend hint/model. Default: kokoro. Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER: xiaomi_mimo, kokoro; eSpeak is opt-in only."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"output_format","output_format",1390326421),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Audio format. Default mp3."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"speed","speed",1257663751),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Speech speed multiplier. Default 1.15 for the af_jessica workspace voice."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),0.25,new cljs.core.Keyword(null,"max","max",61366548),4.0], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"postprocess_profile","postprocess_profile",-1254686835),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports/commentator, broadcast/warm, narrator/polish, radio/crisp, soft/studio; off/none disables."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Enable final Voxx postprocess. Default true; set false for dry capture."], null),new cljs.core.Keyword(null,"boolean","boolean",-1919418404)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompt_aware","prompt_aware",1309007496),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Prompt-aware performance mode. Default true. Voxx consumes tags like [excited], [whisper], [pause], [dramatic], [laugh], and <break time=\"500ms\" /> as segment-level postprocessing directions, not spoken words."], null),new cljs.core.Keyword(null,"boolean","boolean",-1919418404)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompt_aware_style","prompt_aware_style",1965441274),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional custom instruction for how Voxx should interpret performance tags."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"stability","stability",1733225509),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Stability 0-1 for compatible providers."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),(1)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"similarity_boost","similarity_boost",866821960),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Similarity boost 0-1 for compatible providers."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(0),new cljs.core.Keyword(null,"max","max",61366548),(1)], null)], null)], null)], null);
knoxx.backend.tools.voice.fetch_tts_audio_BANG_ = (function knoxx$backend$tools$voice$fetch_tts_audio_BANG_(url,api_key,body){
return promesa.protocols._mcat(promesa.protocols._promise(null),(function (___21692__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(fetch(url,({"method": "POST", "headers": ({"Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_key)), "Content-Type": "application/json", "Accept": "audio/mpeg"}), "body": JSON.stringify(cljs.core.clj__GT_js(body))}))),(function (res){
return promesa.protocols._mcat(promesa.protocols._promise((cljs.core.truth_(res.ok)?null:promesa.protocols._mcat(promesa.protocols._promise(null),(function (___21692__auto____$1){
return promesa.protocols._mcat(promesa.protocols._promise(res.text()),(function (msg){
return promesa.protocols._promise((function(){throw (new Error((""+"Voice Gateway "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(res.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(msg))))})());
}));
})))),(function (_){
return promesa.protocols._mcat(promesa.protocols._promise(res.arrayBuffer()),(function (arr){
return promesa.protocols._promise(Buffer.from((new Uint8Array(arr))));
}));
}));
}));
}));
});
knoxx.backend.tools.voice.write_audio_file_BANG_ = (function knoxx$backend$tools$voice$write_audio_file_BANG_(node_path,buf,absolute,relative,voice_id,model_id,fmt){
return promesa.protocols._mcat(promesa.protocols._promise(null),(function (___21670__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(shadow.esm.esm_import$node_fs$promises.mkdir(node_path.dirname(absolute),({"recursive": true}))),(function (___21660__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(shadow.esm.esm_import$node_fs$promises.writeFile(absolute,buf)),(function (___21660__auto____$1){
return promesa.protocols._promise(knoxx.backend.text.tool_text_result((""+"Wrote "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(buf.length)+" bytes). Use workspace_media.attach to embed."),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"path","path",-188191168),relative,new cljs.core.Keyword(null,"bytes","bytes",1175866680),buf.length,new cljs.core.Keyword(null,"voice_id","voice_id",-725801774),voice_id,new cljs.core.Keyword(null,"model_id","model_id",-2010580717),model_id,new cljs.core.Keyword(null,"format","format",-1306924766),fmt], null)));
}));
}));
}));
});
knoxx.backend.tools.voice.tts_rest_execute = (function knoxx$backend$tools$voice$tts_rest_execute(runtime,config){
return (function() { 
var G__59527__delegate = function (_call_id,params,on_update,_){
var text = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["text"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw (new Error("voice.tts: text required"));
}
})();
var api_key = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.resolve_voice_key(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
throw (new Error("voice.tts: VOICE_GATEWAY_API_KEY not configured"));
}
})();
var voice_id = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["voice_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-voice-id","voxx-voice-id",-652120125),"voxx-voice-id","voxxVoiceId"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "af_jessica";
}
}
})();
var model_id = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["model_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-model-id","voxx-model-id",2106305693),"voxx-model-id","voxxModelId"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "kokoro";
}
}
})();
var speed = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["speed"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.voice.default_tts_speed(config);
}
})();
var out_fmt = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["output_format"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mp3";
}
})();
var postprocess_profile = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["postprocess_profile"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-postprocess-profile","voxx-postprocess-profile",-1094633966),"voxx-postprocess-profile","voxxPostprocessProfile"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "sports-commentator-v1";
}
}
})();
var postprocess_enabled = (((!(((params["postprocess_enabled"]) == null))))?knoxx.backend.tools.voice.bool_value((params["postprocess_enabled"]),true):knoxx.backend.tools.voice.config_bool_value(config,new cljs.core.Keyword(null,"voxx-postprocess-enabled","voxx-postprocess-enabled",-1494110237),"voxx-postprocess-enabled","voxxPostprocessEnabled",true));
var prompt_aware = (((!(((params["prompt_aware"]) == null))))?knoxx.backend.tools.voice.bool_value((params["prompt_aware"]),true):knoxx.backend.tools.voice.config_bool_value(config,new cljs.core.Keyword(null,"voxx-prompt-aware","voxx-prompt-aware",-648801777),"voxx-prompt-aware","voxxPromptAware",true));
var prompt_aware_style = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["prompt_aware_style"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-prompt-aware-style","voxx-prompt-aware-style",-1200845338),"voxx-prompt-aware-style","voxxPromptAwareStyle"));
}
})();
var out_path = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["output_path"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.voice.tts_default_output_path();
}
})();
var map__59355 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,out_path);
var map__59355__$1 = cljs.core.__destructure_map(map__59355);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59355__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59355__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var options = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"speed","speed",1257663751),speed,new cljs.core.Keyword(null,"postprocess-profile","postprocess-profile",-115988175),postprocess_profile,new cljs.core.Keyword(null,"postprocess-enabled","postprocess-enabled",76184778),postprocess_enabled,new cljs.core.Keyword(null,"prompt-aware","prompt-aware",464266766),prompt_aware,new cljs.core.Keyword(null,"prompt-aware-style","prompt-aware-style",72282946),prompt_aware_style], null);
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"TTS: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((text).length))+" chars -> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" via "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+", voice="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(voice_id)+", speed="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(speed)+", postprocess="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(postprocess_enabled)?postprocess_profile:"off"))+", prompt-aware="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prompt_aware)+"..."));

return promesa.protocols._mcat(promesa.protocols._promise(null),(function (___21692__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(knoxx.backend.tools.voice.fetch_tts_audio_BANG_(knoxx.backend.tools.voice.tts_url(config),api_key,knoxx.backend.tools.voice.tts_body(text,voice_id,model_id,out_fmt,params,options))),(function (buf){
return promesa.protocols._promise(knoxx.backend.tools.voice.write_audio_file_BANG_(shadow.esm.esm_import$node_path,buf,absolute,relative,voice_id,model_id,out_fmt));
}));
}));
};
var G__59527 = function (_call_id,params,on_update,var_args){
var _ = null;
if (arguments.length > 3) {
var G__59550__i = 0, G__59550__a = new Array(arguments.length -  3);
while (G__59550__i < G__59550__a.length) {G__59550__a[G__59550__i] = arguments[G__59550__i + 3]; ++G__59550__i;}
  _ = new cljs.core.IndexedSeq(G__59550__a,0,null);
} 
return G__59527__delegate.call(this,_call_id,params,on_update,_);};
G__59527.cljs$lang$maxFixedArity = 3;
G__59527.cljs$lang$applyTo = (function (arglist__59551){
var _call_id = cljs.core.first(arglist__59551);
arglist__59551 = cljs.core.next(arglist__59551);
var params = cljs.core.first(arglist__59551);
arglist__59551 = cljs.core.next(arglist__59551);
var on_update = cljs.core.first(arglist__59551);
var _ = cljs.core.rest(arglist__59551);
return G__59527__delegate(_call_id,params,on_update,_);
});
G__59527.cljs$core$IFn$_invoke$arity$variadic = G__59527__delegate;
return G__59527;
})()
;
});
knoxx.backend.tools.voice.tts_stream_params = new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Text to synthesize via /ws/voice/tts."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"voice_id","voice_id",-725801774),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx/Kokoro voice ID. Default: af_jessica."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"model_id","model_id",-2010580717),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voxx backend hint/model. Default: kokoro; fallback order is controlled by Voxx. eSpeak is opt-in only in the workspace default."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"output_format","output_format",1390326421),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Format. Default: mp3."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"speed","speed",1257663751),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Speech speed multiplier. Default 1.15."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"double","double",884886883),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),0.25,new cljs.core.Keyword(null,"max","max",61366548),4.0], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"postprocess_profile","postprocess_profile",-1254686835),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Final Voxx mastering profile. Default sports-commentator-v1. Aliases: sports, broadcast, narrator, radio, soft; off disables."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Enable final Voxx postprocess. Default true."], null),new cljs.core.Keyword(null,"boolean","boolean",-1919418404)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompt_aware","prompt_aware",1309007496),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Prompt-aware tag mode. Default true; Voxx consumes tags as segment-level postprocessing directions."], null),new cljs.core.Keyword(null,"boolean","boolean",-1919418404)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompt_aware_style","prompt_aware_style",1965441274),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional custom instruction for tag interpretation."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"auto_mode","auto_mode",-1224005487),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"auto_mode. Default true."], null),new cljs.core.Keyword(null,"boolean","boolean",-1919418404)], null)], null);
knoxx.backend.tools.voice.openutau_project_params = new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"project_name","project_name",-1535411620),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Project name."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"notes","notes",-1039600523),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Ordered note plan."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"lyric","lyric",164436415),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Lyric. Use + or +~ for slurs."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"phonetic_hint","phonetic_hint",1425882362),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Phonetic hint without brackets."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"MIDI note number. C4 = 60."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"duration","duration",1444101068),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Duration in ticks. 480 = 1 quarter note."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"position","position",-2011731912),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Start tick. Sequential if omitted."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tempo","tempo",-1555208453),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"BPM. Default 120."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"beat_per_bar","beat_per_bar",-752938484),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Numerator."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"beat_unit","beat_unit",1360431781),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Denominator."], null),new cljs.core.Keyword(null,"int","int",-1741416922)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"singer_id","singer_id",1456162645),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Singer/voicebank folder."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"phonemizer","phonemizer",-1364007211),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Phonemizer class/tag."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"track_name","track_name",1331132230),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Vocal track name."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"part_name","part_name",-334556537),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Voice part name."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"output_path","output_path",-1715585288),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Output .ustx path."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"comment","comment",532206069),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Project comment."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.voice.tts_stream_execute = (function knoxx$backend$tools$voice$tts_stream_execute(config){
return (function() { 
var G__59560__delegate = function (_call_id,params,on_update,_){
var voice_id = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["voice_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-voice-id","voxx-voice-id",-652120125),"voxx-voice-id","voxxVoiceId"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "af_jessica";
}
}
})();
var model_id = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["model_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-model-id","voxx-model-id",2106305693),"voxx-model-id","voxxModelId"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "kokoro";
}
}
})();
var speed = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["speed"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.voice.default_tts_speed(config);
}
})();
var out_fmt = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["output_format"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mp3";
}
})();
var postprocess_profile = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["postprocess_profile"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-postprocess-profile","voxx-postprocess-profile",-1094633966),"voxx-postprocess-profile","voxxPostprocessProfile"));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "sports-commentator-v1";
}
}
})();
var postprocess_enabled = (((!(((params["postprocess_enabled"]) == null))))?knoxx.backend.tools.voice.bool_value((params["postprocess_enabled"]),true):knoxx.backend.tools.voice.config_bool_value(config,new cljs.core.Keyword(null,"voxx-postprocess-enabled","voxx-postprocess-enabled",-1494110237),"voxx-postprocess-enabled","voxxPostprocessEnabled",true));
var prompt_aware = (((!(((params["prompt_aware"]) == null))))?knoxx.backend.tools.voice.bool_value((params["prompt_aware"]),true):knoxx.backend.tools.voice.config_bool_value(config,new cljs.core.Keyword(null,"voxx-prompt-aware","voxx-prompt-aware",-648801777),"voxx-prompt-aware","voxxPromptAware",true));
var prompt_aware_style = (function (){var or__5142__auto__ = knoxx.backend.tools.voice.blank__GT_nil((params["prompt_aware_style"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.voice.blank__GT_nil(knoxx.backend.tools.voice.config_value(config,new cljs.core.Keyword(null,"voxx-prompt-aware-style","voxx-prompt-aware-style",-1200845338),"voxx-prompt-aware-style","voxxPromptAwareStyle"));
}
})();
var auto_mode = cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(false,(params["auto_mode"]));
var key_ok_QMARK_ = cljs.core.boolean$(knoxx.backend.tools.voice.resolve_voice_key(config));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"voice.tts_stream: returning WS params...");

return promesa.core.resolved(knoxx.backend.text.tool_text_result((function (){var G__59405 = "Connect to /ws/voice/tts. Send {type:start,...}, then {type:text,text:...} chunks, then {type:flush}. Include postprocess_profile/postprocess_enabled/prompt_aware in the start message or query. Receive {type:audio,audio:<base64>} chunks.";
if((!(key_ok_QMARK_))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59405)+" WARNING: VOICE_GATEWAY_API_KEY is not configured.");
} else {
return G__59405;
}
})(),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"speed","speed",1257663751),new cljs.core.Keyword(null,"postprocess_enabled","postprocess_enabled",-648946072),new cljs.core.Keyword(null,"prompt_aware","prompt_aware",1309007496),new cljs.core.Keyword(null,"postprocess_profile","postprocess_profile",-1254686835),new cljs.core.Keyword(null,"auto-mode","auto-mode",-692726543),new cljs.core.Keyword(null,"voice_id","voice_id",-725801774),new cljs.core.Keyword(null,"model_id","model_id",-2010580717),new cljs.core.Keyword(null,"output_format","output_format",1390326421),new cljs.core.Keyword(null,"prompt_aware_style","prompt_aware_style",1965441274),new cljs.core.Keyword(null,"api_key_configured","api_key_configured",437685374),new cljs.core.Keyword(null,"ws_endpoint","ws_endpoint",-365315745)],[speed,postprocess_enabled,prompt_aware,(cljs.core.truth_(postprocess_enabled)?postprocess_profile:"none"),auto_mode,voice_id,model_id,out_fmt,prompt_aware_style,key_ok_QMARK_,"/ws/voice/tts"])));
};
var G__59560 = function (_call_id,params,on_update,var_args){
var _ = null;
if (arguments.length > 3) {
var G__59577__i = 0, G__59577__a = new Array(arguments.length -  3);
while (G__59577__i < G__59577__a.length) {G__59577__a[G__59577__i] = arguments[G__59577__i + 3]; ++G__59577__i;}
  _ = new cljs.core.IndexedSeq(G__59577__a,0,null);
} 
return G__59560__delegate.call(this,_call_id,params,on_update,_);};
G__59560.cljs$lang$maxFixedArity = 3;
G__59560.cljs$lang$applyTo = (function (arglist__59578){
var _call_id = cljs.core.first(arglist__59578);
arglist__59578 = cljs.core.next(arglist__59578);
var params = cljs.core.first(arglist__59578);
arglist__59578 = cljs.core.next(arglist__59578);
var on_update = cljs.core.first(arglist__59578);
var _ = cljs.core.rest(arglist__59578);
return G__59560__delegate(_call_id,params,on_update,_);
});
G__59560.cljs$core$IFn$_invoke$arity$variadic = G__59560__delegate;
return G__59560;
})()
;
});
knoxx.backend.tools.voice.openutau_project_execute = (function knoxx$backend$tools$voice$openutau_project_execute(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59579 = arguments.length;
var i__5877__auto___59580 = (0);
while(true){
if((i__5877__auto___59580 < len__5876__auto___59579)){
args__5882__auto__.push((arguments[i__5877__auto___59580]));

var G__59585 = (i__5877__auto___59580 + (1));
i__5877__auto___59580 = G__59585;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((5) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((5)),(0),null)):null);
return knoxx.backend.tools.voice.openutau_project_execute.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),argseq__5883__auto__);
});

(knoxx.backend.tools.voice.openutau_project_execute.cljs$core$IFn$_invoke$arity$variadic = (function (runtime,config,_call_id,params,on_update,_){
var project_name = (function (){var or__5142__auto__ = knoxx.backend.tools.media.normalize_tool_path_arg((params["project_name"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Knoxx OpenUtau Project";
}
})();
var out_path = (function (){var or__5142__auto__ = knoxx.backend.tools.media.normalize_tool_path_arg((params["output_path"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.tools.openutau.default_project_relative_path(project_name);
}
})();
var map__59426 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,out_path);
var map__59426__$1 = cljs.core.__destructure_map(map__59426);
var workspace_root = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59426__$1,new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547));
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59426__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59426__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var output_dir = shadow.esm.esm_import$node_path.dirname(absolute);
var filename = knoxx.backend.tools.media.path_basename(shadow.esm.esm_import$node_path,absolute);
var readme_abs = shadow.esm.esm_import$node_path.join(output_dir,"README.md");
var readme_rel = knoxx.backend.document_state.normalize_relative_path(knoxx.backend.tools.media.path_relative(shadow.esm.esm_import$node_path,workspace_root,readme_abs));
var notes = knoxx.backend.tools.openutau.normalize_notes(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (params["notes"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
var project = knoxx.backend.tools.openutau.build_project(new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"project_name","project_name",-1535411620),(params["project_name"]),new cljs.core.Keyword(null,"tempo","tempo",-1555208453),(params["tempo"]),new cljs.core.Keyword(null,"time_signature","time_signature",-98519217),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (params["time_signature"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)),new cljs.core.Keyword(null,"singer_id","singer_id",1456162645),(params["singer_id"]),new cljs.core.Keyword(null,"phonemizer","phonemizer",-1364007211),(params["phonemizer"]),new cljs.core.Keyword(null,"track_name","track_name",1331132230),(params["track_name"]),new cljs.core.Keyword(null,"part_name","part_name",-334556537),(params["part_name"]),new cljs.core.Keyword(null,"comment","comment",532206069),(params["comment"])], null),notes);
var ustx_yaml = knoxx.backend.tools.openutau.project__GT_ustx_yaml(project);
var readme_text = knoxx.backend.tools.openutau.readme_markdown(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"project-name","project-name",1486861539),project_name,new cljs.core.Keyword(null,"ustx-path","ustx-path",242803323),relative,new cljs.core.Keyword(null,"readme-path","readme-path",205242972),readme_rel,new cljs.core.Keyword(null,"note-count","note-count",-2010784834),cljs.core.count(notes),new cljs.core.Keyword(null,"tempo","tempo",-1555208453),(function (){var or__5142__auto__ = (params["tempo"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (120);
}
})(),new cljs.core.Keyword(null,"singer-id","singer-id",705189264),(function (){var or__5142__auto__ = (params["singer_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"phonemizer","phonemizer",-1364007211),(function (){var or__5142__auto__ = (params["phonemizer"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()], null));
var data_url = (""+"data:text/yaml;base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Buffer.from(ustx_yaml,"utf8").toString("base64")));
if(cljs.core.seq(notes)){
} else {
throw (new Error("notes must contain at least one note"));
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Writing OpenUtau project "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+"..."));

return promesa.protocols._mcat(promesa.protocols._promise(null),(function (___21670__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(shadow.esm.esm_import$node_fs$promises.mkdir(output_dir,({"recursive": true}))),(function (___21660__auto__){
return promesa.protocols._mcat(promesa.protocols._promise(shadow.esm.esm_import$node_fs$promises.writeFile(absolute,ustx_yaml,"utf8")),(function (___21660__auto____$1){
return promesa.protocols._mcat(promesa.protocols._promise(shadow.esm.esm_import$node_fs$promises.writeFile(readme_abs,readme_text,"utf8")),(function (___21660__auto____$2){
return promesa.protocols._promise(knoxx.backend.text.tool_text_result((""+"Created OpenUtau project at "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" with "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(notes))+" notes."),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"path","path",-188191168),relative,new cljs.core.Keyword(null,"readme_path","readme_path",501154428),readme_rel,new cljs.core.Keyword(null,"project_name","project_name",-1535411620),project_name,new cljs.core.Keyword(null,"note_count","note_count",693479986),cljs.core.count(notes),new cljs.core.Keyword(null,"tempo","tempo",-1555208453),(function (){var or__5142__auto__ = (params["tempo"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (120);
}
})(),new cljs.core.Keyword(null,"renderer","renderer",336841071),knoxx.backend.tools.openutau.default_renderer,new cljs.core.Keyword(null,"headless_render_supported","headless_render_supported",1252467375),false,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),"document",new cljs.core.Keyword(null,"data","data",-232669377),data_url,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),"text/yaml",new cljs.core.Keyword(null,"filename","filename",-1428840783),filename], null)], null)], null)));
}));
}));
}));
}));
}));

(knoxx.backend.tools.voice.openutau_project_execute.cljs$lang$maxFixedArity = (5));

/** @this {Function} */
(knoxx.backend.tools.voice.openutau_project_execute.cljs$lang$applyTo = (function (seq59419){
var G__59420 = cljs.core.first(seq59419);
var seq59419__$1 = cljs.core.next(seq59419);
var G__59421 = cljs.core.first(seq59419__$1);
var seq59419__$2 = cljs.core.next(seq59419__$1);
var G__59422 = cljs.core.first(seq59419__$2);
var seq59419__$3 = cljs.core.next(seq59419__$2);
var G__59423 = cljs.core.first(seq59419__$3);
var seq59419__$4 = cljs.core.next(seq59419__$3);
var G__59424 = cljs.core.first(seq59419__$4);
var seq59419__$5 = cljs.core.next(seq59419__$4);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59420,G__59421,G__59422,G__59423,G__59424,seq59419__$5);
}));

knoxx.backend.tools.voice.voice_openutau_project_execute = (function knoxx$backend$tools$voice$voice_openutau_project_execute(runtime,config,call_id,params,a,b,c){
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
return knoxx.backend.tools.voice.openutau_project_execute(runtime,config,call_id,params,on_update);
});
knoxx.backend.tools.voice.voice_tts_execute = (function knoxx$backend$tools$voice$voice_tts_execute(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59611 = arguments.length;
var i__5877__auto___59612 = (0);
while(true){
if((i__5877__auto___59612 < len__5876__auto___59611)){
args__5882__auto__.push((arguments[i__5877__auto___59612]));

var G__59613 = (i__5877__auto___59612 + (1));
i__5877__auto___59612 = G__59613;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return knoxx.backend.tools.voice.voice_tts_execute.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(knoxx.backend.tools.voice.voice_tts_execute.cljs$core$IFn$_invoke$arity$variadic = (function (runtime,config,args){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.voice.tts_rest_execute(runtime,config),args);
}));

(knoxx.backend.tools.voice.voice_tts_execute.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(knoxx.backend.tools.voice.voice_tts_execute.cljs$lang$applyTo = (function (seq59472){
var G__59473 = cljs.core.first(seq59472);
var seq59472__$1 = cljs.core.next(seq59472);
var G__59474 = cljs.core.first(seq59472__$1);
var seq59472__$2 = cljs.core.next(seq59472__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59473,G__59474,seq59472__$2);
}));

knoxx.backend.tools.voice.voice_tts_stream_execute = (function knoxx$backend$tools$voice$voice_tts_stream_execute(var_args){
var args__5882__auto__ = [];
var len__5876__auto___59614 = arguments.length;
var i__5877__auto___59615 = (0);
while(true){
if((i__5877__auto___59615 < len__5876__auto___59614)){
args__5882__auto__.push((arguments[i__5877__auto___59615]));

var G__59616 = (i__5877__auto___59615 + (1));
i__5877__auto___59615 = G__59616;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((2) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((2)),(0),null)):null);
return knoxx.backend.tools.voice.voice_tts_stream_execute.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__5883__auto__);
});

(knoxx.backend.tools.voice.voice_tts_stream_execute.cljs$core$IFn$_invoke$arity$variadic = (function (runtime,config,args){
return cljs.core.apply.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.voice.tts_stream_execute(config),args);
}));

(knoxx.backend.tools.voice.voice_tts_stream_execute.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(knoxx.backend.tools.voice.voice_tts_stream_execute.cljs$lang$applyTo = (function (seq59481){
var G__59482 = cljs.core.first(seq59481);
var seq59481__$1 = cljs.core.next(seq59481);
var G__59483 = cljs.core.first(seq59481__$1);
var seq59481__$2 = cljs.core.next(seq59481__$1);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__59482,G__59483,seq59481__$2);
}));

knoxx.backend.tools.voice.voice_openutau_project_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"voice.openutau_project","OpenUtau Project","Create an OpenUtau .ustx singing project.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Use for lyric-timed vocal synthesis via OpenUtau.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Provide notes with lyric, tone, duration.","Export is UI-driven; do not claim audio rendered without a real file.","WORLDLINE-R is the default safe renderer."], null),knoxx.backend.tools.voice.openutau_project_params,knoxx.backend.tools.voice.voice_openutau_project_execute], 0));
knoxx.backend.tools.voice.voice_tts_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"voice.tts","Text-to-Speech","Synthesize spoken audio via Voxx Gateway. Defaults to prompt-aware mode plus lively final postprocess, then writes MP3 to workspace.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Use voice.tts for spoken audio. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=kokoro, voice_id=af_jessica, speed=1.15.",new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Pass clean spoken copy; strip markdown formatting, but keep intentional performance tags.","Default mode is prompt-aware: [excited], [whisper], [laugh], [pause], [dramatic], and <break time=\"500ms\" /> are Voxx-owned performance directions, not words to speak and not markup to pass through to the provider.","Use tags sparingly at phrase boundaries. Bracket tags set Voxx segment-level emotion/energy filters, [pause] and <break time=\"...ms\" /> insert silence, and [laugh] inserts a short nonverbal effect.","Voxx consumes known performance tags, sends clean segment text to the chosen backend, stitches the segments together, then applies tag-driven inflection postprocessing plus the final mastering profile.","Use postprocess_profile to choose Voxx's final mastering: sports/commentator (default high energy), broadcast/warm, narrator/polish, radio/crisp, soft/studio, or off/none for dry capture.","eSpeak is not in the default Voxx backend order; if a voice sounds robotic, inspect x-openhax-tts-backend before assuming postprocess is the cause.","Use model_id as a backend hint: kokoro, xiaomi_mimo, requesty, openai, melo, or espeak; Voxx may fall back by VOICE_GATEWAY_TTS_BACKEND_ORDER.","Default output_format is mp3. When output_path is omitted, files save to Voice/tts-<timestamp>.mp3 automatically.","Use Voice/ for spoken TTS output, Audio/ for sound clips and effects, Music/ for musical or sung content.","Follow with workspace_media.attach to embed audio.","If debugging, inspect Voxx headers/logs: x-openhax-tts-backend, x-openhax-tts-postprocess-profile, and x-openhax-tts-prompt-aware."], null),knoxx.backend.tools.voice.tts_rest_params,knoxx.backend.tools.voice.voice_tts_execute], 0));
knoxx.backend.tools.voice.voice_tts_stream_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"voice.tts_stream","TTS Stream","WS streaming TTS session params for /ws/voice/tts with Voxx prompt-aware and postprocess defaults.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Use voice.tts_stream for WS TTS connection params. Default: prompt_aware=true, postprocess_profile=sports-commentator-v1, model_id=kokoro, voice_id=af_jessica, speed=1.15.",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Returns WS protocol spec, default postprocess/prompt-aware settings, and API key status.","Send prompt_aware, prompt_aware_style, postprocess_profile, and postprocess_enabled in the start message or query when overriding defaults.","Use the same tag rules as voice.tts: bracket/XML-like tags are Voxx-owned postprocessing directions, not spoken text.","Use voice.tts when you need a persisted MP3 file."], null),knoxx.backend.tools.voice.tts_stream_params,knoxx.backend.tools.voice.voice_tts_stream_execute], 0));
knoxx.backend.tools.voice.create_voice_synth_custom_tools = (function knoxx$backend$tools$voice$create_voice_synth_custom_tools(var_args){
var G__59497 = arguments.length;
switch (G__59497) {
case 2:
return knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("voice.openutau_project"))?knoxx.backend.tools.voice.voice_openutau_project_tool(runtime,config):null),((allowed_QMARK_("voice.tts"))?knoxx.backend.tools.voice.voice_tts_tool(runtime,config):null),((allowed_QMARK_("voice.tts_stream"))?knoxx.backend.tools.voice.voice_tts_stream_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.voice.create_voice_synth_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.voice.js.map
