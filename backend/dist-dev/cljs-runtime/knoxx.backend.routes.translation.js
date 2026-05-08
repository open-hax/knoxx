import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.app_shapes.js";
goog.provide('knoxx.backend.routes.translation');
knoxx.backend.routes.translation.reply_header_BANG_ = (function knoxx$backend$routes$translation$reply_header_BANG_(reply,name,value){
return reply.header(name,value);
});
knoxx.backend.routes.translation.register_translation_routes_BANG_ = (function knoxx$backend$routes$translation$register_translation_routes_BANG_(app,runtime,config,p__69614){
var map__69615 = p__69614;
var map__69615__$1 = cljs.core.__destructure_map(map__69615);
var openplanner_enabled_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"openplanner-enabled?","openplanner-enabled?",-1180234471));
var openplanner_request_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"openplanner-request!","openplanner-request!",-1690277990));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var ctx_org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"ctx-org-id","ctx-org-id",949922116));
var openplanner_url = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"openplanner-url","openplanner-url",-1804248247));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var ctx_user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"ctx-user-id","ctx-user-id",-259951088));
var ctx_user_email = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"ctx-user-email","ctx-user-email",-64148717));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var openplanner_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69615__$1,new cljs.core.Keyword(null,"openplanner-headers","openplanner-headers",1561778839));
knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/segments",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69616 = reply;
var G__69617 = (503);
var G__69618 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69616,G__69617,G__69618) : json_response_BANG_.call(null,G__69616,G__69617,G__69618));
} else {
var G__69619 = runtime;
var G__69620 = request;
var G__69621 = reply;
var G__69622 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var project = (function (){var or__5142__auto__ = (query["project"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var status = (query["status"]);
var source_lang = (query["source_lang"]);
var target_lang = (query["target_lang"]);
var domain = (query["domain"]);
var limit = (function (){var or__5142__auto__ = (query["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "50";
}
})();
var offset = (function (){var or__5142__auto__ = (query["offset"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "0";
}
})();
var params = (""+"project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(project))+"&limit="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(limit)+"&offset="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(offset)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(status)?(""+"&status="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(status))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(source_lang)?(""+"&source_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(source_lang))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(target_lang)?(""+"&target_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(target_lang))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(domain)?(""+"&domain="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(domain))):null)));
return (function (){var G__69628 = config;
var G__69629 = "GET";
var G__69630 = (""+"/v1/translations/segments?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(params));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69628,G__69629,G__69630) : openplanner_request_BANG_.call(null,G__69628,G__69629,G__69630));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69619,G__69620,G__69621,G__69622) : with_request_context_BANG_.call(null,G__69619,G__69620,G__69621,G__69622));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/segments/:id",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69633 = reply;
var G__69634 = (503);
var G__69635 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69633,G__69634,G__69635) : json_response_BANG_.call(null,G__69633,G__69634,G__69635));
} else {
var G__69636 = runtime;
var G__69637 = request;
var G__69638 = reply;
var G__69639 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var segment_id = (request["params"]["id"]);
return (function (){var G__69642 = config;
var G__69643 = "GET";
var G__69644 = (""+"/v1/translations/segments/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment_id));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69642,G__69643,G__69644) : openplanner_request_BANG_.call(null,G__69642,G__69643,G__69644));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69636,G__69637,G__69638,G__69639) : with_request_context_BANG_.call(null,G__69636,G__69637,G__69638,G__69639));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/translations/segments/:id/labels",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69645 = reply;
var G__69646 = (503);
var G__69647 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69645,G__69646,G__69647) : json_response_BANG_.call(null,G__69645,G__69646,G__69647));
} else {
var G__69648 = runtime;
var G__69649 = request;
var G__69650 = reply;
var G__69651 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.review") : ensure_permission_BANG_.call(null,ctx,"org.translations.review"));
} else {
}

var segment_id = (request["params"]["id"]);
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var body_with_auth = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([body,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"labeler_id","labeler_id",-843194266),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_user_id.cljs$core$IFn$_invoke$arity$1 ? ctx_user_id.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_user_id.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())),new cljs.core.Keyword(null,"labeler_email","labeler_email",-1161180430),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_user_email.cljs$core$IFn$_invoke$arity$1 ? ctx_user_email.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_user_email.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())),new cljs.core.Keyword(null,"org_id","org_id",1380185385),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_org_id.cljs$core$IFn$_invoke$arity$1 ? ctx_org_id.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_org_id.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))], null)], 0));
return (function (){var G__69654 = config;
var G__69655 = "POST";
var G__69656 = (""+"/v1/translations/segments/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(segment_id)+"/labels");
var G__69657 = cljs.core.clj__GT_js(body_with_auth);
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(G__69654,G__69655,G__69656,G__69657) : openplanner_request_BANG_.call(null,G__69654,G__69655,G__69656,G__69657));
})().then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69648,G__69649,G__69650,G__69651) : with_request_context_BANG_.call(null,G__69648,G__69649,G__69650,G__69651));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/export/manifest",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69661 = reply;
var G__69662 = (503);
var G__69663 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69661,G__69662,G__69663) : json_response_BANG_.call(null,G__69661,G__69662,G__69663));
} else {
var G__69664 = runtime;
var G__69665 = request;
var G__69666 = reply;
var G__69667 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.export") : ensure_permission_BANG_.call(null,ctx,"org.translations.export"));
} else {
}

var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var project = (function (){var or__5142__auto__ = (query["project"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
}
})();
return (function (){var G__69672 = config;
var G__69673 = "GET";
var G__69674 = (""+"/v1/translations/export/manifest?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(project)));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69672,G__69673,G__69674) : openplanner_request_BANG_.call(null,G__69672,G__69673,G__69674));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69664,G__69665,G__69666,G__69667) : with_request_context_BANG_.call(null,G__69664,G__69665,G__69666,G__69667));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/export/sft",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69677 = reply;
var G__69678 = (503);
var G__69679 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69677,G__69678,G__69679) : json_response_BANG_.call(null,G__69677,G__69678,G__69679));
} else {
var G__69680 = runtime;
var G__69681 = request;
var G__69682 = reply;
var G__69683 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.export") : ensure_permission_BANG_.call(null,ctx,"org.translations.export"));
} else {
}

var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var project = (function (){var or__5142__auto__ = (query["project"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var target_lang = (query["target_lang"]);
var include_corrected = (query["include_corrected"]);
var suffix = (""+"/v1/translations/export/sft?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(project))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(target_lang)?(""+"&target_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(target_lang))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(include_corrected)?(""+"&include_corrected="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(include_corrected))):null)));
return fetch((openplanner_url.cljs$core$IFn$_invoke$arity$2 ? openplanner_url.cljs$core$IFn$_invoke$arity$2(config,suffix) : openplanner_url.call(null,config,suffix)),({"method": "GET", "headers": (openplanner_headers.cljs$core$IFn$_invoke$arity$1 ? openplanner_headers.cljs$core$IFn$_invoke$arity$1(config) : openplanner_headers.call(null,config))})).then((function (resp){
return resp.text().then((function (text){
if(cljs.core.truth_((resp["ok"]))){
knoxx.backend.routes.translation.reply_header_BANG_(reply,"Content-Type","application/x-ndjson");

return reply.send(text);
} else {
var G__69686 = reply;
var G__69687 = (function (){var or__5142__auto__ = (resp["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (502);
}
})();
var G__69688 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),text], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69686,G__69687,G__69688) : json_response_BANG_.call(null,G__69686,G__69687,G__69688));
}
}));
}),(function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}).catch());
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69680,G__69681,G__69682,G__69683) : with_request_context_BANG_.call(null,G__69680,G__69681,G__69682,G__69683));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/translations/segments/batch",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69691 = reply;
var G__69692 = (503);
var G__69693 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69691,G__69692,G__69693) : json_response_BANG_.call(null,G__69691,G__69692,G__69693));
} else {
var G__69694 = runtime;
var G__69695 = request;
var G__69696 = reply;
var G__69697 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.manage") : ensure_permission_BANG_.call(null,ctx,"org.translations.manage"));
} else {
}

var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var body_with_auth = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(body,new cljs.core.Keyword(null,"org_id","org_id",1380185385),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_org_id.cljs$core$IFn$_invoke$arity$1 ? ctx_org_id.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_org_id.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
return (function (){var G__69698 = config;
var G__69699 = "POST";
var G__69700 = "/v1/translations/segments/batch";
var G__69701 = cljs.core.clj__GT_js(body_with_auth);
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(G__69698,G__69699,G__69700,G__69701) : openplanner_request_BANG_.call(null,G__69698,G__69699,G__69700,G__69701));
})().then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69694,G__69695,G__69696,G__69697) : with_request_context_BANG_.call(null,G__69694,G__69695,G__69696,G__69697));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/documents",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69702 = reply;
var G__69703 = (503);
var G__69704 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69702,G__69703,G__69704) : json_response_BANG_.call(null,G__69702,G__69703,G__69704));
} else {
var G__69705 = runtime;
var G__69706 = request;
var G__69707 = reply;
var G__69708 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var project = (function (){var or__5142__auto__ = (query["project"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
}
})();
var target_lang = (query["target_lang"]);
var source_lang = (query["source_lang"]);
var garden_id = (query["garden_id"]);
var params = (""+"project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(project))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(target_lang)?(""+"&target_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(target_lang))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(source_lang)?(""+"&source_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(source_lang))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(garden_id)?(""+"&garden_id="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(garden_id))):null)));
return (function (){var G__69709 = config;
var G__69710 = "GET";
var G__69711 = (""+"/v1/translations/documents?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(params));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69709,G__69710,G__69711) : openplanner_request_BANG_.call(null,G__69709,G__69710,G__69711));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69705,G__69706,G__69707,G__69708) : with_request_context_BANG_.call(null,G__69705,G__69706,G__69707,G__69708));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/documents/:documentId/:targetLang",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69716 = reply;
var G__69717 = (503);
var G__69718 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69716,G__69717,G__69718) : json_response_BANG_.call(null,G__69716,G__69717,G__69718));
} else {
var G__69719 = runtime;
var G__69720 = request;
var G__69721 = reply;
var G__69722 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var doc_id = (request["params"]["documentId"]);
var target_lang = (request["params"]["targetLang"]);
return (function (){var G__69724 = config;
var G__69725 = "GET";
var G__69726 = (""+"/v1/translations/documents/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(doc_id)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_lang));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69724,G__69725,G__69726) : openplanner_request_BANG_.call(null,G__69724,G__69725,G__69726));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69719,G__69720,G__69721,G__69722) : with_request_context_BANG_.call(null,G__69719,G__69720,G__69721,G__69722));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/translations/documents/:documentId/:targetLang/review",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69728 = reply;
var G__69729 = (503);
var G__69730 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69728,G__69729,G__69730) : json_response_BANG_.call(null,G__69728,G__69729,G__69730));
} else {
var G__69731 = runtime;
var G__69732 = request;
var G__69733 = reply;
var G__69734 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.review") : ensure_permission_BANG_.call(null,ctx,"org.translations.review"));
} else {
}

var doc_id = (request["params"]["documentId"]);
var target_lang = (request["params"]["targetLang"]);
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
var body_with_auth = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([body,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"labeler_id","labeler_id",-843194266),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_user_id.cljs$core$IFn$_invoke$arity$1 ? ctx_user_id.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_user_id.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})())),new cljs.core.Keyword(null,"labeler_email","labeler_email",-1161180430),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (ctx_user_email.cljs$core$IFn$_invoke$arity$1 ? ctx_user_email.cljs$core$IFn$_invoke$arity$1(ctx) : ctx_user_email.call(null,ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})()))], null)], 0));
return (function (){var G__69738 = config;
var G__69739 = "POST";
var G__69740 = (""+"/v1/translations/documents/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(doc_id)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(target_lang)+"/review");
var G__69741 = cljs.core.clj__GT_js(body_with_auth);
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(G__69738,G__69739,G__69740,G__69741) : openplanner_request_BANG_.call(null,G__69738,G__69739,G__69740,G__69741));
})().then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69731,G__69732,G__69733,G__69734) : with_request_context_BANG_.call(null,G__69731,G__69732,G__69733,G__69734));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/translations/batches",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69744 = reply;
var G__69745 = (503);
var G__69746 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69744,G__69745,G__69746) : json_response_BANG_.call(null,G__69744,G__69745,G__69746));
} else {
var G__69747 = runtime;
var G__69748 = request;
var G__69749 = reply;
var G__69750 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.manage") : ensure_permission_BANG_.call(null,ctx,"org.translations.manage"));
} else {
}

var body = (request["body"]);
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,"POST","/v1/translations/batches",body) : openplanner_request_BANG_.call(null,config,"POST","/v1/translations/batches",body)).then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69747,G__69748,G__69749,G__69750) : with_request_context_BANG_.call(null,G__69747,G__69748,G__69749,G__69750));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/batches",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69752 = reply;
var G__69753 = (503);
var G__69754 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69752,G__69753,G__69754) : json_response_BANG_.call(null,G__69752,G__69753,G__69754));
} else {
var G__69755 = runtime;
var G__69756 = request;
var G__69757 = reply;
var G__69758 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var status = (query["status"]);
var garden_id = (query["garden_id"]);
var target_lang = (query["target_lang"]);
var params = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(status)?(""+"status="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(status))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(garden_id)?(""+"&garden_id="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(garden_id))):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(target_lang)?(""+"&target_lang="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(target_lang))):null)));
return (function (){var G__69762 = config;
var G__69763 = "GET";
var G__69764 = (""+"/v1/translations/batches?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(params));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69762,G__69763,G__69764) : openplanner_request_BANG_.call(null,G__69762,G__69763,G__69764));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69755,G__69756,G__69757,G__69758) : with_request_context_BANG_.call(null,G__69755,G__69756,G__69757,G__69758));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/batches/next",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69766 = reply;
var G__69767 = (503);
var G__69768 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69766,G__69767,G__69768) : json_response_BANG_.call(null,G__69766,G__69767,G__69768));
} else {
var G__69769 = runtime;
var G__69770 = request;
var G__69771 = reply;
var G__69772 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.manage") : ensure_permission_BANG_.call(null,ctx,"org.translations.manage"));
} else {
}

return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET","/v1/translations/batches/next") : openplanner_request_BANG_.call(null,config,"GET","/v1/translations/batches/next")).then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69769,G__69770,G__69771,G__69772) : with_request_context_BANG_.call(null,G__69769,G__69770,G__69771,G__69772));
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/translations/batches/:id",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69773 = reply;
var G__69774 = (503);
var G__69775 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69773,G__69774,G__69775) : json_response_BANG_.call(null,G__69773,G__69774,G__69775));
} else {
var G__69777 = runtime;
var G__69778 = request;
var G__69779 = reply;
var G__69780 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.read") : ensure_permission_BANG_.call(null,ctx,"org.translations.read"));
} else {
}

var batch_id = (request["params"]["id"]);
return (function (){var G__69781 = config;
var G__69782 = "GET";
var G__69783 = (""+"/v1/translations/batches/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(batch_id));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__69781,G__69782,G__69783) : openplanner_request_BANG_.call(null,G__69781,G__69782,G__69783));
})().then((function (body){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),body) : json_response_BANG_.call(null,reply,(200),body));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69777,G__69778,G__69779,G__69780) : with_request_context_BANG_.call(null,G__69777,G__69778,G__69779,G__69780));
}
}));

return knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/translations/batches/:id/status",(function (request,reply){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__69784 = reply;
var G__69785 = (503);
var G__69786 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__69784,G__69785,G__69786) : json_response_BANG_.call(null,G__69784,G__69785,G__69786));
} else {
var G__69787 = runtime;
var G__69788 = request;
var G__69789 = reply;
var G__69790 = (function (ctx){
if(cljs.core.truth_(ctx)){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"org.translations.manage") : ensure_permission_BANG_.call(null,ctx,"org.translations.manage"));
} else {
}

var batch_id = (request["params"]["id"]);
var body = (request["body"]);
return (function (){var G__69791 = config;
var G__69792 = "POST";
var G__69793 = (""+"/v1/translations/batches/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(batch_id)+"/status");
var G__69794 = body;
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(G__69791,G__69792,G__69793,G__69794) : openplanner_request_BANG_.call(null,G__69791,G__69792,G__69793,G__69794));
})().then((function (resp){
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,(200),resp) : json_response_BANG_.call(null,reply,(200),resp));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err) : error_response_BANG_.call(null,reply,err));
}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69787,G__69788,G__69789,G__69790) : with_request_context_BANG_.call(null,G__69787,G__69788,G__69789,G__69790));
}
}));
});

//# sourceMappingURL=knoxx.backend.routes.translation.js.map
