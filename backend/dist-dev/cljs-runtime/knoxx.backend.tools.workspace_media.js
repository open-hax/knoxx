import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.tools.shared.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.tools.workspace_media');
knoxx.backend.tools.workspace_media.attach_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"path","path",-188191168),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Workspace-relative path to the image, audio file, video, or document to attach."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional human-readable label for the attachment."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.workspace_media.attach_execute = (function knoxx$backend$tools$workspace_media$attach_execute(runtime,config,_tool_call_id,params,a,b,c){
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
var raw_path = (function (){var or__5142__auto__ = (params["path"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var title = knoxx.backend.tools.media.normalize_tool_path_arg((params["title"]));
var map__58784 = knoxx.backend.tools.media.resolve_workspace_media_path(runtime,config,raw_path);
var map__58784__$1 = cljs.core.__destructure_map(map__58784);
var absolute = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58784__$1,new cljs.core.Keyword(null,"absolute","absolute",1655386478));
var relative = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58784__$1,new cljs.core.Keyword(null,"relative","relative",22796862));
var mime_type = knoxx.backend.tools.media.workspace_media_mime_type(relative);
var filename = knoxx.backend.tools.media.path_basename(shadow.esm.esm_import$node_path,absolute);
if(cljs.core.truth_(mime_type)){
} else {
throw (new Error((""+"Unsupported workspace media type for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+". Supported formats: images, audio, video, pdf, txt, md, csv, json.")));
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Attaching workspace file "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+"\u2026"));

return knoxx.backend.tools.media.fs_stat_BANG_(shadow.esm.esm_import$node_fs$promises,absolute).then((function (stat){
if(cljs.core.truth_(stat.isFile())){
} else {
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" is not a file")));
}

if((stat.size > knoxx.backend.tools.media.workspace_media_max_bytes)){
throw (new Error((""+"File exceeds "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.media.workspace_media_max_bytes)+" bytes. Choose a smaller file or summarize it instead.")));
} else {
}

return knoxx.backend.tools.media.fs_read_file_BANG_.cljs$core$IFn$_invoke$arity$2(shadow.esm.esm_import$node_fs$promises,absolute);
})).then((function (buffer){
var size = buffer.length;
var data_url = (""+"data:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mime_type)+";base64,"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(buffer.toString("base64")));
var part = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"type","type",1174270348),knoxx.backend.tools.media.workspace_media_type(mime_type),new cljs.core.Keyword(null,"data","data",-232669377),data_url,new cljs.core.Keyword(null,"mimeType","mimeType",-995071690),mime_type,new cljs.core.Keyword(null,"filename","filename",-1428840783),(function (){var or__5142__auto__ = title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return filename;
}
})(),new cljs.core.Keyword(null,"size","size",1098693007),size], null);
var label = (function (){var G__58822 = new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part);
switch (G__58822) {
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
return "file";

}
})();
return ({"content": [({"type": "text", "text": (""+"Attached workspace "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(relative)+" for the final reply.")})], "details": ({"path": relative, "title": (function (){var or__5142__auto__ = title;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return filename;
}
})(), "content_parts": cljs.core.clj__GT_js(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [part], null))})});
}));
});
knoxx.backend.tools.workspace_media.attach_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"workspace_media.attach","Attach Workspace Media","Attach an image, audio file, video, or document from the workspace so the user can see or play it inline.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Attach a workspace image, audio file, video, or document directly into the reply when the user asks to show or play a file.",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Use workspace_media.attach when the user wants to show, display, render, play, or attach a workspace file.","Prefer this tool over replying with only a path when the user explicitly wants the media itself.","Pass a workspace-relative path when possible.","If the file is too large or unsupported, explain that clearly and offer the path instead."], null),knoxx.backend.tools.workspace_media.attach_params,knoxx.backend.tools.workspace_media.attach_execute], 0));
knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools = (function knoxx$backend$tools$workspace_media$create_workspace_media_custom_tools(var_args){
var G__58830 = arguments.length;
switch (G__58830) {
case 2:
return knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("workspace_media.attach"))?knoxx.backend.tools.workspace_media.attach_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.workspace_media.create_workspace_media_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.workspace_media.js.map
