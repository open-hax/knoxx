import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.multimodal');
knoxx.backend.tools.multimodal.upload_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Workspace path, URL, or data URL for the media to load."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional human-readable title for the media."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.multimodal.upload_execute = (function knoxx$backend$tools$multimodal$upload_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var source = (function (){var or__5142__auto__ = (params["source"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var title = knoxx.backend.tools.media.normalize_tool_path_arg((params["title"]));
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Loading multimodal media\u2026");

return knoxx.backend.tools.media.media_source__GT_content_part_BANG_(runtime,config,source,knoxx.backend.tools.media.multimodal_upload_max_bytes).then((function (p__58777){
var map__58778 = p__58777;
var map__58778__$1 = cljs.core.__destructure_map(map__58778);
var asset = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58778__$1,new cljs.core.Keyword(null,"source","source",-433931539));
var part = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58778__$1,new cljs.core.Keyword(null,"part","part",77757738));
var part_STAR_ = (function (){var G__58783 = part;
if(cljs.core.truth_(title)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58783,new cljs.core.Keyword(null,"filename","filename",-1428840783),title);
} else {
return G__58783;
}
})();
var label = (function (){var G__58786 = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part_STAR_);
switch (G__58786) {
case "image":
return "image";

break;
case "audio":
return "audio";

break;
case "video":
return "video";

break;
default:
return "document";

}
})();
return knoxx.backend.text.tool_text_result((""+"Loaded "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part_STAR_);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(asset);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return source;
}
}
}
})())+" for multimodal model context."),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"source","source",-433931539),source,new cljs.core.Keyword(null,"source_kind","source_kind",-1857411768),new cljs.core.Keyword(null,"source-kind","source-kind",-1955827566).cljs$core$IFn$_invoke$arity$1(asset),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(part_STAR_),new cljs.core.Keyword(null,"filename","filename",-1428840783),new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(part_STAR_),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [part_STAR_], null)], null));
}));
});
knoxx.backend.tools.multimodal.upload_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"multimodal.upload","Multimodal Upload","Load an image, audio file, video, or document from the workspace, a URL, or a data URL so the model can inspect it.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Load external or workspace media into the multimodal conversation context.",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use multimodal.upload when the user shares a media URL or asks you to inspect audio, images, video, or documents.","Prefer multimodal.upload for remote or inline media; prefer workspace_media.attach when the goal is to include a workspace file in the final reply.","If the model contract does not support a media type directly, explain that and consider audio.spectrogram for audio analysis via vision."], null),knoxx.backend.tools.multimodal.upload_params,knoxx.backend.tools.multimodal.upload_execute], 0));
knoxx.backend.tools.multimodal.create_multimodal_custom_tools = (function knoxx$backend$tools$multimodal$create_multimodal_custom_tools(var_args){
var G__58807 = arguments.length;
switch (G__58807) {
case 2:
return knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("multimodal.upload"))?knoxx.backend.tools.multimodal.upload_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.multimodal.create_multimodal_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.multimodal.js.map
