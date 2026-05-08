import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.text.js";
goog.provide('knoxx.backend.agents.content');
/**
 * Return s when it is a non-blank string (after trim).
 */
knoxx.backend.agents.content.nonblank = (function knoxx$backend$agents$content$nonblank(s){
if(typeof s === 'string'){
var trimmed = clojure.string.trim(s);
if(clojure.string.blank_QMARK_(trimmed)){
return null;
} else {
return trimmed;
}
} else {
return null;
}
});
/**
 * Like value->preview-text, but returns nil for blank previews so OR chains keep searching.
 */
knoxx.backend.agents.content.preview_text_nonblank = (function knoxx$backend$agents$content$preview_text_nonblank(value,max_chars){
var preview = (function (){var G__33501 = knoxx.backend.text.value__GT_preview_text.cljs$core$IFn$_invoke$arity$2(value,max_chars);
if((G__33501 == null)){
return null;
} else {
return knoxx.backend.agents.content.nonblank(G__33501);
}
})();
var lowered = (function (){var G__33502 = preview;
if((G__33502 == null)){
return null;
} else {
return clojure.string.lower_case(G__33502);
}
})();
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["null",null,"undefined",null], null), null),lowered)){
return null;
} else {
return preview;
}
});
knoxx.backend.agents.content.fenced = (function knoxx$backend$agents$content$fenced(lang,text){
return (""+"```"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(lang)+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())+"\n```");
});
knoxx.backend.agents.content.json_preview_nonblank = (function knoxx$backend$agents$content$json_preview_nonblank(value,max_chars){
if(cljs.core.truth_((function (){var and__5140__auto__ = value;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(value,undefined);
} else {
return and__5140__auto__;
}
})())){
try{var json = JSON.stringify(value,null,(2));
if(typeof json === 'string'){
return knoxx.backend.agents.content.preview_text_nonblank(json,max_chars);
} else {
return null;
}
}catch (e33534){var _ = e33534;
return null;
}} else {
return null;
}
});
knoxx.backend.agents.content.duplicate_normalized_text = (function knoxx$backend$agents$content$duplicate_normalized_text(s){
return clojure.string.lower_case(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s)),/[\s\W_]+/,""));
});
knoxx.backend.agents.content.boundary_ended_QMARK_ = (function knoxx$backend$agents$content$boundary_ended_QMARK_(s){
return cljs.core.boolean$(cljs.core.re_find(/[\s\W_]$/,s));
});
knoxx.backend.agents.content.duplicated_prefix_QMARK_ = (function knoxx$backend$agents$content$duplicated_prefix_QMARK_(previous,appended){
var or__5142__auto__ = (function (){var and__5140__auto__ = clojure.string.starts_with_QMARK_(appended,previous);
if(and__5140__auto__){
var remaining = appended.slice(cljs.core.count(previous));
return (((cljs.core.count(remaining) > (0))) && (cljs.core.boolean$(cljs.core.re_find(/^[\s\W_]/,remaining))));
} else {
return and__5140__auto__;
}
})();
if(or__5142__auto__){
return or__5142__auto__;
} else {
return ((knoxx.backend.agents.content.boundary_ended_QMARK_(previous)) && (((cljs.core.seq(knoxx.backend.agents.content.duplicate_normalized_text(previous))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.content.duplicate_normalized_text(appended),knoxx.backend.agents.content.duplicate_normalized_text(previous))))));
}
});
knoxx.backend.agents.content.max_overlap = (function knoxx$backend$agents$content$max_overlap(left,right){
var n = cljs.core.min.cljs$core$IFn$_invoke$arity$2(cljs.core.count(left),cljs.core.count(right));
while(true){
if((n === (0))){
return (0);
} else {
if(clojure.string.ends_with_QMARK_(left,right.slice((0),n))){
return n;
} else {
var G__33664 = (n - (1));
n = G__33664;
continue;

}
}
break;
}
});
knoxx.backend.agents.content.diff_appended_text = (function knoxx$backend$agents$content$diff_appended_text(previous,current){
var previous__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = previous;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var current__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(current__$1)){
return "";
} else {
if(clojure.string.blank_QMARK_(previous__$1)){
return current__$1;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(current__$1,previous__$1)){
return "";
} else {
if(clojure.string.starts_with_QMARK_(current__$1,previous__$1)){
var appended = current__$1.slice(((previous__$1).length));
if(knoxx.backend.agents.content.duplicated_prefix_QMARK_(previous__$1,appended)){
return appended.slice(((previous__$1).length));
} else {
return appended;
}
} else {
return current__$1.slice(knoxx.backend.agents.content.max_overlap(previous__$1,current__$1));

}
}
}
}
});
knoxx.backend.agents.content.media_part_url = (function knoxx$backend$agents$content$media_part_url(part){
var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["url"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["file_url"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.nonblank((part["fileUrl"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (function (){var image_url = (part["image_url"]);
if(typeof image_url === 'string'){
return knoxx.backend.agents.content.nonblank(image_url);
} else {
if(cljs.core.truth_(image_url)){
return knoxx.backend.agents.content.nonblank((image_url["url"]));
} else {
return null;

}
}
})();
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = (function (){var video_url = (part["video_url"]);
if(typeof video_url === 'string'){
return knoxx.backend.agents.content.nonblank(video_url);
} else {
if(cljs.core.truth_(video_url)){
return knoxx.backend.agents.content.nonblank((video_url["url"]));
} else {
return null;

}
}
})();
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = (function (){var audio_url = (part["audio_url"]);
if(typeof audio_url === 'string'){
return knoxx.backend.agents.content.nonblank(audio_url);
} else {
if(cljs.core.truth_(audio_url)){
return knoxx.backend.agents.content.nonblank((audio_url["url"]));
} else {
return null;

}
}
})();
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var source = (part["source"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = source;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("url",(function (){var G__33581 = (source["type"]);
var G__33581__$1 = (((G__33581 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33581)));
if((G__33581__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__33581__$1);
}
})());
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.agents.content.nonblank((source["url"]));
} else {
return null;
}
}
}
}
}
}
}
});
knoxx.backend.agents.content.media_part_data = (function knoxx$backend$agents$content$media_part_data(part){
var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["data"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["b64_json"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.nonblank((part["result"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (function (){var input_audio = (part["input_audio"]);
if(cljs.core.truth_(input_audio)){
return knoxx.backend.agents.content.nonblank((input_audio["data"]));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = (function (){var output_audio = (part["output_audio"]);
if(cljs.core.truth_(output_audio)){
return knoxx.backend.agents.content.nonblank((output_audio["data"]));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var source = (part["source"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = source;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("base64",(function (){var G__33593 = (source["type"]);
var G__33593__$1 = (((G__33593 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33593)));
if((G__33593__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__33593__$1);
}
})());
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.agents.content.nonblank((source["data"]));
} else {
return null;
}
}
}
}
}
}
});
knoxx.backend.agents.content.media_part_mime_type = (function knoxx$backend$agents$content$media_part_mime_type(part,media_kind){
var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["mimeType"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["mime_type"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.nonblank((part["mediaType"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.agents.content.nonblank((part["media_type"]));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = (function (){var source = (part["source"]);
if(cljs.core.truth_(source)){
var or__5142__auto____$4 = knoxx.backend.agents.content.nonblank((source["media_type"]));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return knoxx.backend.agents.content.nonblank((source["mime_type"]));
}
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = (function (){var input_audio = (part["input_audio"]);
var format = (cljs.core.truth_(input_audio)?knoxx.backend.agents.content.nonblank((input_audio["format"])):null);
if(cljs.core.truth_(format)){
return (""+"audio/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(format));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = (function (){var output_audio = (part["output_audio"]);
var format = (cljs.core.truth_(output_audio)?knoxx.backend.agents.content.nonblank((output_audio["format"])):null);
if(cljs.core.truth_(format)){
return (""+"audio/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(format));
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var G__33605 = media_kind;
switch (G__33605) {
case "image":
return "image/png";

break;
case "audio":
return "audio/wav";

break;
case "video":
return "video/mp4";

break;
case "document":
return "application/octet-stream";

break;
default:
return null;

}
}
}
}
}
}
}
}
});
knoxx.backend.agents.content.media_part_filename = (function knoxx$backend$agents$content$media_part_filename(part){
var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["filename"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["file_name"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.nonblank((part["fileName"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.agents.content.nonblank((part["name"]));
}
}
}
});
knoxx.backend.agents.content.media_part_size = (function knoxx$backend$agents$content$media_part_size(part){
var value = (function (){var or__5142__auto__ = (part["size"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (part["bytes"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (part["byte_size"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (part["byteSize"]);
}
}
}
})();
if(typeof value === 'number'){
return value;
} else {
return null;
}
});
knoxx.backend.agents.content.assistant_media_part = (function knoxx$backend$agents$content$assistant_media_part(part){
var raw_type = (function (){var G__33618 = (part["type"]);
var G__33618__$1 = (((G__33618 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33618)));
if((G__33618__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__33618__$1);
}
})();
var media_kind = ((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["image",null,"input_image",null,"output_image",null,"image_url",null], null), null),raw_type))?"image":((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["input_audio",null,"output_audio",null,"audio_url",null,"audio",null], null), null),raw_type))?"audio":((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["output_video",null,"video_url",null,"video",null,"input_video",null], null), null),raw_type))?"video":((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["input_file",null,"document",null,"output_file",null,"file",null], null), null),raw_type))?"document":null
))));
var url = knoxx.backend.agents.content.media_part_url(part);
var raw_data = knoxx.backend.agents.content.media_part_data(part);
var mime_type = knoxx.backend.agents.content.media_part_mime_type(part,media_kind);
var data = (cljs.core.truth_(raw_data)?((clojure.string.starts_with_QMARK_(raw_data,"data:"))?raw_data:(""+"data:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime_type)+";base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_data))):null);
var filename = knoxx.backend.agents.content.media_part_filename(part);
var size = knoxx.backend.agents.content.media_part_size(part);
if(cljs.core.truth_((function (){var and__5140__auto__ = media_kind;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = url;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return data;
}
} else {
return and__5140__auto__;
}
})())){
var G__33621 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),media_kind], null);
var G__33621__$1 = (cljs.core.truth_(url)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33621,new cljs.core.Keyword(null,"url","url",276297046),url):G__33621);
var G__33621__$2 = (cljs.core.truth_(data)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33621__$1,new cljs.core.Keyword(null,"data","data",-232669377),data):G__33621__$1);
var G__33621__$3 = (cljs.core.truth_(mime_type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33621__$2,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type):G__33621__$2);
var G__33621__$4 = (cljs.core.truth_(filename)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33621__$3,new cljs.core.Keyword(null,"filename","filename",-1428840783),filename):G__33621__$3);
if(cljs.core.truth_(size)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33621__$4,new cljs.core.Keyword(null,"size","size",1098693007),size);
} else {
return G__33621__$4;
}
} else {
return null;
}
});
knoxx.backend.agents.content.assistant_content_parts = (function knoxx$backend$agents$content$assistant_content_parts(assistant_message){
var content = (cljs.core.truth_(assistant_message)?(assistant_message["content"]):null);
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.content.assistant_media_part,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content)));
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.agents.content.session_message_text = (function knoxx$backend$agents$content$session_message_text(message){
var content = (message["content"]);
if(typeof content === 'string'){
return content;
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(content))){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.text.content_part_text,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(content))));
} else {
if(typeof (message["text"]) === 'string'){
return (message["text"]);
} else {
return "";

}
}
}
});
knoxx.backend.agents.content.content_part_label = (function knoxx$backend$agents$content$content_part_label(part){
var part_type = (((new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) instanceof cljs.core.Keyword))?cljs.core.name(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part)):((typeof new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part) === 'string')?new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part):null
));
var G__33636 = part_type;
switch (G__33636) {
case "image":
return "image";

break;
case "audio":
return "audio file";

break;
case "video":
return "video";

break;
case "document":
return "document";

break;
default:
return "attachment";

}
});
knoxx.backend.agents.content.content_part_name = (function knoxx$backend$agents$content$content_part_name(part){
var or__5142__auto__ = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.agents.content.content_part_label(part);
}
}
});
knoxx.backend.agents.content.tool_result_media_type = (function knoxx$backend$agents$content$tool_result_media_type(value){
var G__33637 = (function (){var G__33638 = value;
var G__33638__$1 = (((G__33638 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33638)));
if((G__33638__$1 == null)){
return null;
} else {
return clojure.string.lower_case(G__33638__$1);
}
})();
switch (G__33637) {
case "image":
case "image_url":
case "output_image":
return "image";

break;
case "audio":
case "audio_url":
case "output_audio":
return "audio";

break;
case "video":
case "video_url":
case "output_video":
return "video";

break;
case "document":
case "file":
case "output_file":
return "document";

break;
default:
return null;

}
});
knoxx.backend.agents.content.tool_result_content_part = (function knoxx$backend$agents$content$tool_result_content_part(part){
var media_type = knoxx.backend.agents.content.tool_result_media_type((part["type"]));
var data = knoxx.backend.agents.content.nonblank((part["data"]));
var url = knoxx.backend.agents.content.nonblank((part["url"]));
var mime_type = (function (){var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["mimeType"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["mime_type"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.agents.content.nonblank((part["mediaType"]));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.agents.content.nonblank((part["media_type"]));
}
}
}
})();
var filename = (function (){var or__5142__auto__ = knoxx.backend.agents.content.nonblank((part["filename"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.agents.content.nonblank((part["fileName"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.agents.content.nonblank((part["name"]));
}
}
})();
var size = (function (){var value = (function (){var or__5142__auto__ = (part["size"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (part["bytes"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (part["byteSize"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (part["byte_size"]);
}
}
}
})();
if(typeof value === 'number'){
return value;
} else {
return null;
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = media_type;
if(cljs.core.truth_(and__5140__auto__)){
var or__5142__auto__ = data;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return url;
}
} else {
return and__5140__auto__;
}
})())){
var G__33643 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"type","type",1174270348),media_type], null);
var G__33643__$1 = (cljs.core.truth_(data)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33643,new cljs.core.Keyword(null,"data","data",-232669377),data):G__33643);
var G__33643__$2 = (cljs.core.truth_(url)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33643__$1,new cljs.core.Keyword(null,"url","url",276297046),url):G__33643__$1);
var G__33643__$3 = (cljs.core.truth_(mime_type)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33643__$2,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type):G__33643__$2);
var G__33643__$4 = (cljs.core.truth_(filename)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33643__$3,new cljs.core.Keyword(null,"filename","filename",-1428840783),filename):G__33643__$3);
if(cljs.core.truth_(size)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33643__$4,new cljs.core.Keyword(null,"size","size",1098693007),size);
} else {
return G__33643__$4;
}
} else {
return null;
}
});
knoxx.backend.agents.content.tool_result_content_parts = (function knoxx$backend$agents$content$tool_result_content_parts(tool_result){
var details = (cljs.core.truth_(tool_result)?(tool_result["details"]):null);
var raw_parts = (function (){var or__5142__auto__ = (cljs.core.truth_(tool_result)?(tool_result["content_parts"]):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (cljs.core.truth_(tool_result)?(tool_result["contentParts"]):null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (cljs.core.truth_(details)?(details["content_parts"]):null);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = (cljs.core.truth_(details)?(details["contentParts"]):null);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
if(cljs.core.truth_(details)){
return (details["attachments"]);
} else {
return null;
}
}
}
}
}
})();
if(cljs.core.truth_(cljs.core.array_QMARK_(raw_parts))){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.agents.content.tool_result_content_part,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw_parts)));
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.agents.content.merge_content_parts = (function knoxx$backend$agents$content$merge_content_parts(var_args){
var args__5882__auto__ = [];
var len__5876__auto___33718 = arguments.length;
var i__5877__auto___33719 = (0);
while(true){
if((i__5877__auto___33719 < len__5876__auto___33718)){
args__5882__auto__.push((arguments[i__5877__auto___33719]));

var G__33720 = (i__5877__auto___33719 + (1));
i__5877__auto___33719 = G__33720;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return knoxx.backend.agents.content.merge_content_parts.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(knoxx.backend.agents.content.merge_content_parts.cljs$core$IFn$_invoke$arity$variadic = (function (groups){
return cljs.core.vec(cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,part){
if(cljs.core.truth_(cljs.core.some((function (p1__33651_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__33651_SHARP_,part);
}),acc))){
return acc;
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,part);
}
}),cljs.core.PersistentVector.EMPTY,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__33650_SHARP_){
var or__5142__auto__ = p1__33650_SHARP_;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([groups], 0))));
}));

(knoxx.backend.agents.content.merge_content_parts.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(knoxx.backend.agents.content.merge_content_parts.cljs$lang$applyTo = (function (seq33652){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq33652));
}));

knoxx.backend.agents.content.reply_attachment_content_parts = (function knoxx$backend$agents$content$reply_attachment_content_parts(tool_receipts){
return cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__33656_SHARP_){
var or__5142__auto__ = new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(p1__33656_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contentParts","contentParts",1395809695).cljs$core$IFn$_invoke$arity$1(p1__33656_SHARP_);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33655_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("workspace_media.attach",new cljs.core.Keyword(null,"tool_name","tool_name",-42168484).cljs$core$IFn$_invoke$arity$1(p1__33655_SHARP_));
}),(function (){var or__5142__auto__ = tool_receipts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], 0)));
});
knoxx.backend.agents.content.model_ready_content_parts = (function knoxx$backend$agents$content$model_ready_content_parts(config,model_id,content_parts){
return cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (part){
var part__$1 = ((cljs.core.map_QMARK_(part))?part:cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(part,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
var part_type = (((new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part__$1) instanceof cljs.core.Keyword))?cljs.core.name(new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part__$1)):((typeof new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part__$1) === 'string')?new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part__$1):null
));
if((((part_type == null)) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(part_type,"text")) || (knoxx.backend.runtime.models.model_supports_input_QMARK_(config,model_id,part_type)))))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [part__$1], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(part_type,"audio")){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Uploaded audio source '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agents.content.content_part_name(part__$1))+"' is available, but model "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+" does not declare audio input. Use audio.spectrogram if you need an image-friendly audio view.")], null)], null);
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Uploaded "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agents.content.content_part_label(part__$1))+" '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.agents.content.content_part_name(part__$1))+"' is available, but model "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+" does not declare "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(part_type)+" input.")], null)], null);

}
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = content_parts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()], 0)));
});

//# sourceMappingURL=knoxx.backend.agents.content.js.map
