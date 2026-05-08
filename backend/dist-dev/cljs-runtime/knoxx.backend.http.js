import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.http');
knoxx.backend.http.reply_already_sent_QMARK_ = (function knoxx$backend$http$reply_already_sent_QMARK_(reply){
var raw = (reply["raw"]);
return cljs.core.boolean$((function (){var or__5142__auto__ = (reply["sent"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = raw;
if(cljs.core.truth_(and__5140__auto__)){
return (raw["writableEnded"]);
} else {
return and__5140__auto__;
}
}
})());
});
knoxx.backend.http.json_response_BANG_ = (function knoxx$backend$http$json_response_BANG_(reply,status,body){
if(knoxx.backend.http.reply_already_sent_QMARK_(reply)){
return reply;
} else {
return reply.code(status).type("application/json").send(cljs.core.clj__GT_js(body));
}
});
knoxx.backend.http.request_hostname = (function knoxx$backend$http$request_hostname(request){
var forwarded = (function (){var G__50666 = (request["headers"]["x-forwarded-host"]);
var G__50666__$1 = (((G__50666 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__50666,/,/));
var G__50666__$2 = (((G__50666__$1 == null))?null:cljs.core.first(G__50666__$1));
if((G__50666__$2 == null)){
return null;
} else {
return clojure.string.trim(G__50666__$2);
}
})();
var raw_host = (function (){var or__5142__auto__ = forwarded;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["headers"]["host"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(raw_host)){
var or__5142__auto__ = (request["hostname"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "localhost";
}
} else {
return clojure.string.replace(raw_host,/:.*$/,"");
}
});
knoxx.backend.http.request_scheme = (function knoxx$backend$http$request_scheme(request){
var forwarded = (function (){var G__50667 = (request["headers"]["x-forwarded-proto"]);
var G__50667__$1 = (((G__50667 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__50667,/,/));
var G__50667__$2 = (((G__50667__$1 == null))?null:cljs.core.first(G__50667__$1));
if((G__50667__$2 == null)){
return null;
} else {
return clojure.string.trim(G__50667__$2);
}
})();
if(clojure.string.blank_QMARK_(forwarded)){
return "http";
} else {
return forwarded;
}
});
knoxx.backend.http.rewrite_localhost_url = (function knoxx$backend$http$rewrite_localhost_url(url,request){
try{var parsed = (new URL(url));
var host = parsed.hostname;
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, ["127.0.0.1",null,"localhost",null,"::1",null], null), null),host)){
var req_host = knoxx.backend.http.request_hostname(request);
var scheme = knoxx.backend.http.request_scheme(request);
(parsed.protocol = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(scheme)+":"));

(parsed.hostname = req_host);

return parsed.toString();
} else {
return url;
}
}catch (e50668){var _ = e50668;
return url;
}});
knoxx.backend.http.with_query_param = (function knoxx$backend$http$with_query_param(url,key,value){
try{var parsed = (new URL(url));
parsed.searchParams.set(key,value);

return parsed.toString();
}catch (e50671){var _ = e50671;
return url;
}});
knoxx.backend.http.bearer_headers = (function knoxx$backend$http$bearer_headers(token){
var headers = ({"Content-Type": "application/json"});
if(clojure.string.blank_QMARK_(token)){
} else {
(headers["Authorization"] = (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)));
}

return headers;
});
knoxx.backend.http.openai_auth_error = (function knoxx$backend$http$openai_auth_error(reply,status_code,message,code){
return knoxx.backend.http.json_response_BANG_(reply,status_code,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"message","message",-406056002),message,new cljs.core.Keyword(null,"type","type",1174270348),"invalid_request_error",new cljs.core.Keyword(null,"param","param",2013631823),null,new cljs.core.Keyword(null,"code","code",1586293142),code], null)], null));
});
knoxx.backend.http.require_openai_key_BANG_ = (function knoxx$backend$http$require_openai_key_BANG_(config,request,reply){
var expected = new cljs.core.Keyword(null,"model-lab-openai-api-key","model-lab-openai-api-key",1371814107).cljs$core$IFn$_invoke$arity$1(config);
var auth_header = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["headers"]["authorization"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(clojure.string.blank_QMARK_(expected)){
knoxx.backend.http.openai_auth_error(reply,(503),"MODEL_LAB_OPENAI_API_KEY is not configured","service_unavailable");

return false;
} else {
if((!(clojure.string.starts_with_QMARK_(clojure.string.lower_case(auth_header),"bearer ")))){
knoxx.backend.http.openai_auth_error(reply,(401),"Invalid API key","invalid_api_key");

return false;
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.subs.cljs$core$IFn$_invoke$arity$2(auth_header,(7)),expected)){
knoxx.backend.http.openai_auth_error(reply,(401),"Invalid API key","invalid_api_key");

return false;
} else {
return true;

}
}
}
});
knoxx.backend.http.fetch_json = (function knoxx$backend$http$fetch_json(url,opts){
return fetch(url,opts).then((function (resp){
return resp.text().then((function (text){
var body = ((clojure.string.blank_QMARK_(text))?({}):(function (){try{return JSON.parse(text);
}catch (e50673){var _ = e50673;
return ({"raw": text});
}})());
return ({"ok": resp.ok, "status": resp.status, "body": body, "headers": resp.headers});
}));
}));
});
knoxx.backend.http.trim_trailing_slashes = (function knoxx$backend$http$trim_trailing_slashes(s){
return clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = s;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\/+$/,"");
});
knoxx.backend.http.openplanner_enabled_QMARK_ = (function knoxx$backend$http$openplanner_enabled_QMARK_(config){
return (((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config))))));
});
knoxx.backend.http.openplanner_url = (function knoxx$backend$http$openplanner_url(config,suffix){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.trim_trailing_slashes(new cljs.core.Keyword(null,"openplanner-base-url","openplanner-base-url",2028278103).cljs$core$IFn$_invoke$arity$1(config)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(suffix));
});
knoxx.backend.http.openplanner_headers = (function knoxx$backend$http$openplanner_headers(config){
return ({"Content-Type": "application/json", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"openplanner-api-key","openplanner-api-key",5324020).cljs$core$IFn$_invoke$arity$1(config))), "X-Tenant-ID": (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-session";
}
})()});
});
knoxx.backend.http.openplanner_request_BANG_ = (function knoxx$backend$http$openplanner_request_BANG_(var_args){
var G__50675 = arguments.length;
switch (G__50675) {
case 3:
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (config,method,suffix){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4(config,method,suffix,null);
}));

(knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (config,method,suffix,body){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
return Promise.reject((new Error("OpenPlanner is not configured")));
} else {
var opts = ({"method": method, "headers": knoxx.backend.http.openplanner_headers(config)});
if(cljs.core.truth_(body)){
(opts["body"] = JSON.stringify(cljs.core.clj__GT_js(body)));
} else {
}

return knoxx.backend.http.fetch_json(knoxx.backend.http.openplanner_url(config,suffix),opts).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
throw (new Error((""+"OpenPlanner request failed ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1((resp["status"]))+"): "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], 0))))));
}
}));
}
}));

(knoxx.backend.http.openplanner_request_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.http.http_error = (function knoxx$backend$http$http_error(status,code,message){
var G__50679 = cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(status)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"code","code",1586293142),code], null));
(G__50679["statusCode"] = status);

(G__50679["code"] = code);

return G__50679;
});
knoxx.backend.http.error_status = (function knoxx$backend$http$error_status(err,default_status){
var or__5142__auto__ = (err["statusCode"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (err["status"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return default_status;
}
}
});
knoxx.backend.http.error_message = (function knoxx$backend$http$error_message(err){
var or__5142__auto__ = (err["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
});
knoxx.backend.http.error_response_BANG_ = (function knoxx$backend$http$error_response_BANG_(var_args){
var G__50682 = arguments.length;
switch (G__50682) {
case 2:
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (reply,err){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(500));
}));

(knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (reply,err,default_status){
return knoxx.backend.http.json_response_BANG_(reply,knoxx.backend.http.error_status(err,default_status),(function (){var G__50683 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),knoxx.backend.http.error_message(err)], null);
if(cljs.core.truth_((err["code"]))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__50683,new cljs.core.Keyword(null,"error_code","error_code",1077063696),(err["code"]));
} else {
return G__50683;
}
})());
}));

(knoxx.backend.http.error_response_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.http.no_content_QMARK_ = (function knoxx$backend$http$no_content_QMARK_(x){
return (((x == null)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(undefined,x)));
});
knoxx.backend.http.js_array_seq = (function knoxx$backend$http$js_array_seq(value){
if(cljs.core.truth_(cljs.core.array_QMARK_(value))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value);
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.http.copy_response_headers_BANG_ = (function knoxx$backend$http$copy_response_headers_BANG_(reply,headers){
return headers.forEach((function (value,key){
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["content-encoding",null,"content-length",null,"connection",null,"transfer-encoding",null], null), null),clojure.string.lower_case(key))){
return null;
} else {
return reply.header(key,value);
}
}));
});
knoxx.backend.http.send_fetch_response_BANG_ = (function knoxx$backend$http$send_fetch_response_BANG_(reply,resp){
knoxx.backend.http.copy_response_headers_BANG_(reply,resp.headers);

return resp.arrayBuffer().then((function (buf){
return reply.code(resp.status).send(Buffer.from(buf));
}));
});
knoxx.backend.http.request_query_string = (function knoxx$backend$http$request_query_string(request){
var params = (new URLSearchParams());
var query = (function (){var or__5142__auto__ = (request["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var seq__50685_50803 = cljs.core.seq(knoxx.backend.http.js_array_seq(Object.keys(query)));
var chunk__50686_50804 = null;
var count__50687_50805 = (0);
var i__50688_50806 = (0);
while(true){
if((i__50688_50806 < count__50687_50805)){
var key_50811 = chunk__50686_50804.cljs$core$IIndexed$_nth$arity$2(null,i__50688_50806);
var value_50812 = (query[key_50811]);
if(knoxx.backend.http.no_content_QMARK_(value_50812)){
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(value_50812))){
var seq__50707_50813 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value_50812));
var chunk__50708_50814 = null;
var count__50709_50815 = (0);
var i__50710_50816 = (0);
while(true){
if((i__50710_50816 < count__50709_50815)){
var item_50817 = chunk__50708_50814.cljs$core$IIndexed$_nth$arity$2(null,i__50710_50816);
params.append(key_50811,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_50817)));


var G__50819 = seq__50707_50813;
var G__50820 = chunk__50708_50814;
var G__50821 = count__50709_50815;
var G__50822 = (i__50710_50816 + (1));
seq__50707_50813 = G__50819;
chunk__50708_50814 = G__50820;
count__50709_50815 = G__50821;
i__50710_50816 = G__50822;
continue;
} else {
var temp__5825__auto___50823 = cljs.core.seq(seq__50707_50813);
if(temp__5825__auto___50823){
var seq__50707_50824__$1 = temp__5825__auto___50823;
if(cljs.core.chunked_seq_QMARK_(seq__50707_50824__$1)){
var c__5673__auto___50825 = cljs.core.chunk_first(seq__50707_50824__$1);
var G__50826 = cljs.core.chunk_rest(seq__50707_50824__$1);
var G__50827 = c__5673__auto___50825;
var G__50828 = cljs.core.count(c__5673__auto___50825);
var G__50829 = (0);
seq__50707_50813 = G__50826;
chunk__50708_50814 = G__50827;
count__50709_50815 = G__50828;
i__50710_50816 = G__50829;
continue;
} else {
var item_50830 = cljs.core.first(seq__50707_50824__$1);
params.append(key_50811,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_50830)));


var G__50832 = cljs.core.next(seq__50707_50824__$1);
var G__50833 = null;
var G__50834 = (0);
var G__50835 = (0);
seq__50707_50813 = G__50832;
chunk__50708_50814 = G__50833;
count__50709_50815 = G__50834;
i__50710_50816 = G__50835;
continue;
}
} else {
}
}
break;
}
} else {
params.append(key_50811,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50812)));

}
}


var G__50837 = seq__50685_50803;
var G__50838 = chunk__50686_50804;
var G__50839 = count__50687_50805;
var G__50840 = (i__50688_50806 + (1));
seq__50685_50803 = G__50837;
chunk__50686_50804 = G__50838;
count__50687_50805 = G__50839;
i__50688_50806 = G__50840;
continue;
} else {
var temp__5825__auto___50841 = cljs.core.seq(seq__50685_50803);
if(temp__5825__auto___50841){
var seq__50685_50842__$1 = temp__5825__auto___50841;
if(cljs.core.chunked_seq_QMARK_(seq__50685_50842__$1)){
var c__5673__auto___50844 = cljs.core.chunk_first(seq__50685_50842__$1);
var G__50845 = cljs.core.chunk_rest(seq__50685_50842__$1);
var G__50846 = c__5673__auto___50844;
var G__50847 = cljs.core.count(c__5673__auto___50844);
var G__50848 = (0);
seq__50685_50803 = G__50845;
chunk__50686_50804 = G__50846;
count__50687_50805 = G__50847;
i__50688_50806 = G__50848;
continue;
} else {
var key_50849 = cljs.core.first(seq__50685_50842__$1);
var value_50850 = (query[key_50849]);
if(knoxx.backend.http.no_content_QMARK_(value_50850)){
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(value_50850))){
var seq__50722_50851 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(value_50850));
var chunk__50723_50852 = null;
var count__50724_50853 = (0);
var i__50725_50854 = (0);
while(true){
if((i__50725_50854 < count__50724_50853)){
var item_50859 = chunk__50723_50852.cljs$core$IIndexed$_nth$arity$2(null,i__50725_50854);
params.append(key_50849,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_50859)));


var G__50861 = seq__50722_50851;
var G__50862 = chunk__50723_50852;
var G__50863 = count__50724_50853;
var G__50864 = (i__50725_50854 + (1));
seq__50722_50851 = G__50861;
chunk__50723_50852 = G__50862;
count__50724_50853 = G__50863;
i__50725_50854 = G__50864;
continue;
} else {
var temp__5825__auto___50865__$1 = cljs.core.seq(seq__50722_50851);
if(temp__5825__auto___50865__$1){
var seq__50722_50866__$1 = temp__5825__auto___50865__$1;
if(cljs.core.chunked_seq_QMARK_(seq__50722_50866__$1)){
var c__5673__auto___50868 = cljs.core.chunk_first(seq__50722_50866__$1);
var G__50869 = cljs.core.chunk_rest(seq__50722_50866__$1);
var G__50870 = c__5673__auto___50868;
var G__50871 = cljs.core.count(c__5673__auto___50868);
var G__50872 = (0);
seq__50722_50851 = G__50869;
chunk__50723_50852 = G__50870;
count__50724_50853 = G__50871;
i__50725_50854 = G__50872;
continue;
} else {
var item_50873 = cljs.core.first(seq__50722_50866__$1);
params.append(key_50849,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(item_50873)));


var G__50874 = cljs.core.next(seq__50722_50866__$1);
var G__50875 = null;
var G__50876 = (0);
var G__50877 = (0);
seq__50722_50851 = G__50874;
chunk__50723_50852 = G__50875;
count__50724_50853 = G__50876;
i__50725_50854 = G__50877;
continue;
}
} else {
}
}
break;
}
} else {
params.append(key_50849,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50850)));

}
}


var G__50880 = cljs.core.next(seq__50685_50842__$1);
var G__50881 = null;
var G__50882 = (0);
var G__50883 = (0);
seq__50685_50803 = G__50880;
chunk__50686_50804 = G__50881;
count__50687_50805 = G__50882;
i__50688_50806 = G__50883;
continue;
}
} else {
}
}
break;
}

var encoded = params.toString();
if(clojure.string.blank_QMARK_(encoded)){
return "";
} else {
return (""+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encoded));
}
});
knoxx.backend.http.request_forward_headers = (function knoxx$backend$http$request_forward_headers(request,extra){
var headers = (new Headers());
var source = (function (){var or__5142__auto__ = (request["headers"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var seq__50737_50884 = cljs.core.seq(knoxx.backend.http.js_array_seq(Object.keys(source)));
var chunk__50738_50885 = null;
var count__50739_50886 = (0);
var i__50740_50887 = (0);
while(true){
if((i__50740_50887 < count__50739_50886)){
var key_50888 = chunk__50738_50885.cljs$core$IIndexed$_nth$arity$2(null,i__50740_50887);
var lower_50889 = clojure.string.lower_case(key_50888);
var value_50890 = (source[key_50888]);
if((((!(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["host",null,"content-length",null,"connection",null,"transfer-encoding",null], null), null),lower_50889)))) && ((!(knoxx.backend.http.no_content_QMARK_(value_50890)))))){
headers.set(key_50888,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50890)));
} else {
}


var G__50892 = seq__50737_50884;
var G__50893 = chunk__50738_50885;
var G__50894 = count__50739_50886;
var G__50895 = (i__50740_50887 + (1));
seq__50737_50884 = G__50892;
chunk__50738_50885 = G__50893;
count__50739_50886 = G__50894;
i__50740_50887 = G__50895;
continue;
} else {
var temp__5825__auto___50896 = cljs.core.seq(seq__50737_50884);
if(temp__5825__auto___50896){
var seq__50737_50898__$1 = temp__5825__auto___50896;
if(cljs.core.chunked_seq_QMARK_(seq__50737_50898__$1)){
var c__5673__auto___50899 = cljs.core.chunk_first(seq__50737_50898__$1);
var G__50900 = cljs.core.chunk_rest(seq__50737_50898__$1);
var G__50901 = c__5673__auto___50899;
var G__50902 = cljs.core.count(c__5673__auto___50899);
var G__50903 = (0);
seq__50737_50884 = G__50900;
chunk__50738_50885 = G__50901;
count__50739_50886 = G__50902;
i__50740_50887 = G__50903;
continue;
} else {
var key_50904 = cljs.core.first(seq__50737_50898__$1);
var lower_50905 = clojure.string.lower_case(key_50904);
var value_50906 = (source[key_50904]);
if((((!(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, ["host",null,"content-length",null,"connection",null,"transfer-encoding",null], null), null),lower_50905)))) && ((!(knoxx.backend.http.no_content_QMARK_(value_50906)))))){
headers.set(key_50904,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50906)));
} else {
}


var G__50908 = cljs.core.next(seq__50737_50898__$1);
var G__50909 = null;
var G__50910 = (0);
var G__50911 = (0);
seq__50737_50884 = G__50908;
chunk__50738_50885 = G__50909;
count__50739_50886 = G__50910;
i__50740_50887 = G__50911;
continue;
}
} else {
}
}
break;
}

var seq__50746_50912 = cljs.core.seq(extra);
var chunk__50747_50913 = null;
var count__50748_50914 = (0);
var i__50749_50915 = (0);
while(true){
if((i__50749_50915 < count__50748_50914)){
var vec__50762_50917 = chunk__50747_50913.cljs$core$IIndexed$_nth$arity$2(null,i__50749_50915);
var key_50918 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50762_50917,(0),null);
var value_50919 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50762_50917,(1),null);
if((value_50919 == null)){
headers.delete(key_50918);
} else {
headers.set(key_50918,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50919)));
}


var G__50923 = seq__50746_50912;
var G__50924 = chunk__50747_50913;
var G__50925 = count__50748_50914;
var G__50926 = (i__50749_50915 + (1));
seq__50746_50912 = G__50923;
chunk__50747_50913 = G__50924;
count__50748_50914 = G__50925;
i__50749_50915 = G__50926;
continue;
} else {
var temp__5825__auto___50927 = cljs.core.seq(seq__50746_50912);
if(temp__5825__auto___50927){
var seq__50746_50929__$1 = temp__5825__auto___50927;
if(cljs.core.chunked_seq_QMARK_(seq__50746_50929__$1)){
var c__5673__auto___50930 = cljs.core.chunk_first(seq__50746_50929__$1);
var G__50931 = cljs.core.chunk_rest(seq__50746_50929__$1);
var G__50932 = c__5673__auto___50930;
var G__50933 = cljs.core.count(c__5673__auto___50930);
var G__50934 = (0);
seq__50746_50912 = G__50931;
chunk__50747_50913 = G__50932;
count__50748_50914 = G__50933;
i__50749_50915 = G__50934;
continue;
} else {
var vec__50765_50935 = cljs.core.first(seq__50746_50929__$1);
var key_50936 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50765_50935,(0),null);
var value_50937 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50765_50935,(1),null);
if((value_50937 == null)){
headers.delete(key_50936);
} else {
headers.set(key_50936,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value_50937)));
}


var G__50939 = cljs.core.next(seq__50746_50929__$1);
var G__50940 = null;
var G__50941 = (0);
var G__50942 = (0);
seq__50746_50912 = G__50939;
chunk__50747_50913 = G__50940;
count__50748_50914 = G__50941;
i__50749_50915 = G__50942;
continue;
}
} else {
}
}
break;
}

return headers;
});
knoxx.backend.http.request_forward_body = (function knoxx$backend$http$request_forward_body(request){
var method = clojure.string.upper_case((function (){var or__5142__auto__ = (request["method"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "GET";
}
})());
var body = (request["body"]);
if(cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["HEAD",null,"GET",null], null), null),method)){
return null;
} else {
if(((typeof body === 'string') || ((((body instanceof Uint8Array)) || ((((body instanceof ArrayBuffer)) || ((body instanceof Buffer)))))))){
return body;
} else {
if(knoxx.backend.http.no_content_QMARK_(body)){
return null;
} else {
return JSON.stringify(body);

}
}
}
});

//# sourceMappingURL=knoxx.backend.http.js.map
