import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./malli.core.js";
import "./malli.error.js";
import "./knoxx.backend.app_shapes.js";
import "./knoxx.backend.auth.session.js";
import "./knoxx.backend.mcp_expose.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.runtime.state.js";
import "./shadow.esm.esm_import$$modelcontextprotocol$sdk$server$mcp.js";
import "./shadow.esm.esm_import$$modelcontextprotocol$sdk$server$streamableHttp.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$zod.js";
goog.provide('knoxx.backend.routes.mcp');

if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.routes !== 'undefined') && (typeof knoxx.backend.routes.mcp !== 'undefined') && (typeof knoxx.backend.routes.mcp.mcp_sessions_STAR_ !== 'undefined')){
} else {
knoxx.backend.routes.mcp.mcp_sessions_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
knoxx.backend.routes.mcp.RegisterClientBody = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"redirect-uris","redirect-uris",778927369),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),cljs.core.string_QMARK_], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"client-name","client-name",1843891115),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),cljs.core.string_QMARK_], null)], null);
knoxx.backend.routes.mcp.AuthorizeQuery = new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"client-id","client-id",-464622140),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"state","state",-1988618099),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maybe","maybe",-314397560),cljs.core.string_QMARK_], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"scope","scope",-439358418),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maybe","maybe",-314397560),cljs.core.string_QMARK_], null)], null)], null);
knoxx.backend.routes.mcp.AuthorizeConfirmQuery = new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"client-id","client-id",-464622140),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"state","state",-1988618099),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maybe","maybe",-314397560),cljs.core.string_QMARK_], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"scope","scope",-439358418),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"optional","optional",2053951509),true], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"maybe","maybe",-314397560),cljs.core.string_QMARK_], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"selected-tools","selected-tools",700501530),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"vector","vector",1902966158),cljs.core.string_QMARK_], null)], null)], null);
knoxx.backend.routes.mcp.TokenExchangeBody = new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"grant-type","grant-type",-1751533246),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code","code",1586293142),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"code-verifier","code-verifier",-848846001),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"client-id","client-id",-464622140),cljs.core.string_QMARK_], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),cljs.core.string_QMARK_], null)], null);
knoxx.backend.routes.mcp.RevokeTokenParams = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"token-id","token-id",-764089526),cljs.core.string_QMARK_], null)], null);
knoxx.backend.routes.mcp.env = (function knoxx$backend$routes$mcp$env(k,default$){
var or__5142__auto__ = (process.env[k]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
});
knoxx.backend.routes.mcp.public_base_url = (function knoxx$backend$routes$mcp$public_base_url(config){
try{return (new URL((function (){var or__5142__auto__ = (process.env["KNOXX_PUBLIC_BASE_URL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["RENDER_EXTERNAL_URL"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "http://localhost";
}
}
}
})()));
}catch (e69613){var _ = e69613;
return (new URL("http://localhost"));
}});
knoxx.backend.routes.mcp.base64url = (function knoxx$backend$routes$mcp$base64url(buf){
return Buffer.from(buf).toString("base64url");
});
knoxx.backend.routes.mcp.pkce_challenge = (function knoxx$backend$routes$mcp$pkce_challenge(crypto,verifier){
return knoxx.backend.routes.mcp.base64url(crypto.createHash("sha256").update((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(verifier))).digest());
});
knoxx.backend.routes.mcp.bearer_token = (function knoxx$backend$routes$mcp$bearer_token(req){
var raw = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__69624 = req;
var G__69624__$1 = (((G__69624 == null))?null:(G__69624["headers"]));
if((G__69624__$1 == null)){
return null;
} else {
return (G__69624__$1["authorization"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var m = raw.match((new RegExp("^Bearer\\s+(.+)$","i")));
if(cljs.core.truth_(m)){
return clojure.string.trim((m[(1)]));
} else {
return null;
}
});
knoxx.backend.routes.mcp.resolve_session_id = (function knoxx$backend$routes$mcp$resolve_session_id(req){
var headers = (function (){var or__5142__auto__ = (req["headers"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var header_id = (headers["mcp-session-id"]);
var q = (function (){var or__5142__auto__ = (req["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var query_id = (q["sessionId"]);
if(((typeof header_id === 'string') && ((!(clojure.string.blank_QMARK_(header_id)))))){
return header_id;
} else {
if(((typeof query_id === 'string') && ((!(clojure.string.blank_QMARK_(query_id)))))){
return query_id;
} else {
return null;

}
}
});
knoxx.backend.routes.mcp.safe = (function knoxx$backend$routes$mcp$safe(s){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = s;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"","&quot;");
});
knoxx.backend.routes.mcp.normalize_tool_selection = (function knoxx$backend$routes$mcp$normalize_tool_selection(raw){
if((raw == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(raw))){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw));
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw))], null);

}
}
});
knoxx.backend.routes.mcp.json_send_BANG_ = (function knoxx$backend$routes$mcp$json_send_BANG_(reply,status,payload){
return reply.code(status).send(cljs.core.clj__GT_js(payload));
});
knoxx.backend.routes.mcp.text_send_BANG_ = (function knoxx$backend$routes$mcp$text_send_BANG_(reply,status,body){
return reply.code(status).send(body);
});
knoxx.backend.routes.mcp.protected_resource_metadata_url = (function knoxx$backend$routes$mcp$protected_resource_metadata_url(base){
return (new URL("/.well-known/oauth-protected-resource",base)).toString();
});
knoxx.backend.routes.mcp.www_authenticate_challenge = (function knoxx$backend$routes$mcp$www_authenticate_challenge(base){
return (""+"Bearer realm=\"mcp\", resource_metadata=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.protected_resource_metadata_url(base))+"\"");
});
knoxx.backend.routes.mcp.challenge_unauthorized_BANG_ = (function knoxx$backend$routes$mcp$challenge_unauthorized_BANG_(reply,base){
return (function (){var G__69658 = reply;
var G__69659 = "WWW-Authenticate";
var G__69660 = knoxx.backend.routes.mcp.www_authenticate_challenge(base);
return (knoxx.backend.routes.mcp.reply_header_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.routes.mcp.reply_header_BANG_.cljs$core$IFn$_invoke$arity$3(G__69658,G__69659,G__69660) : knoxx.backend.routes.mcp.reply_header_BANG_.call(null,G__69658,G__69659,G__69660));
})().code((401)).send("Unauthorized");
});
knoxx.backend.routes.mcp.redis_set_BANG_ = (function knoxx$backend$routes$mcp$redis_set_BANG_(c,k,v,o){
return c.set(k,v,o);
});
knoxx.backend.routes.mcp.redis_get_BANG_ = (function knoxx$backend$routes$mcp$redis_get_BANG_(c,k){
return c.get(k);
});
knoxx.backend.routes.mcp.redis_del_BANG_ = (function knoxx$backend$routes$mcp$redis_del_BANG_(c,k){
return c.del(k);
});
knoxx.backend.routes.mcp.redis_sadd_BANG_ = (function knoxx$backend$routes$mcp$redis_sadd_BANG_(c,k,v){
return c.sAdd(k,v);
});
knoxx.backend.routes.mcp.redis_smembers_BANG_ = (function knoxx$backend$routes$mcp$redis_smembers_BANG_(c,k){
return c.sMembers(k);
});
knoxx.backend.routes.mcp.redis_srem_BANG_ = (function knoxx$backend$routes$mcp$redis_srem_BANG_(c,k,v){
return c.sRem(k,v);
});
knoxx.backend.routes.mcp.transport_handle_request_BANG_ = (function knoxx$backend$routes$mcp$transport_handle_request_BANG_(var_args){
var G__69690 = arguments.length;
switch (G__69690) {
case 3:
return knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (t,req,reply){
return t.handleRequest(req,reply);
}));

(knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (t,req,reply,body){
return t.handleRequest(req,reply,body);
}));

(knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.routes.mcp.tool_execute_BANG_ = (function knoxx$backend$routes$mcp$tool_execute_BANG_(tool,params){
return tool.execute("mcp",params,null,null,null);
});
knoxx.backend.routes.mcp.reply_header_BANG_ = (function knoxx$backend$routes$mcp$reply_header_BANG_(reply,name,value){
return reply.header(name,value);
});
knoxx.backend.routes.mcp.ensure_streamable_accept_BANG_ = (function knoxx$backend$routes$mcp$ensure_streamable_accept_BANG_(req){
var raw = (req["raw"]);
var headers = (function (){var or__5142__auto__ = (raw["headers"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var raw_headers = (function (){var or__5142__auto__ = (raw["rawHeaders"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var accept_value = "application/json, text/event-stream";
var accept = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (headers["accept"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var has_json_QMARK_ = clojure.string.includes_QMARK_(accept,"application/json");
var has_sse_QMARK_ = clojure.string.includes_QMARK_(accept,"text/event-stream");
if(((clojure.string.blank_QMARK_(accept)) || ((((!(has_json_QMARK_))) || ((!(has_sse_QMARK_))))))){
(headers["accept"] = accept_value);

(raw["headers"] = headers);

var filtered_70004 = cljs.core.vec(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.identity,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.remove.cljs$core$IFn$_invoke$arity$2((function (p__69712){
var vec__69713 = p__69712;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__69713,(0),null);
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__69713,(1),null);
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("accept",clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(k))));
}),cljs.core.partition.cljs$core$IFn$_invoke$arity$2((2),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw_headers)))], 0)));
(raw["rawHeaders"] = cljs.core.clj__GT_js(cljs.core.conj.cljs$core$IFn$_invoke$arity$variadic(filtered_70004,"accept",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([accept_value], 0))));
} else {
}

return req;
});
knoxx.backend.routes.mcp.http_error = (function knoxx$backend$routes$mcp$http_error(var_args){
var G__69727 = arguments.length;
switch (G__69727) {
case 3:
return knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3 = (function (status,error,detail){
return cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2(detail,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"detail","detail",-1545345025),detail], null));
}));

(knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$4 = (function (status,error,detail,data){
return cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2(detail,cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"detail","detail",-1545345025),detail], null),data], 0)));
}));

(knoxx.backend.routes.mcp.http_error.cljs$lang$maxFixedArity = 4);

knoxx.backend.routes.mcp.validation_detail = (function knoxx$backend$routes$mcp$validation_detail(schema,value){
var G__69737 = malli.core.explain.cljs$core$IFn$_invoke$arity$2(schema,value);
var G__69737__$1 = (((G__69737 == null))?null:malli.error.humanize.cljs$core$IFn$_invoke$arity$1(G__69737));
if((G__69737__$1 == null)){
return null;
} else {
return cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([G__69737__$1], 0));
}
});
knoxx.backend.routes.mcp.validate_BANG_ = (function knoxx$backend$routes$mcp$validate_BANG_(schema,value,p__69742){
var map__69743 = p__69742;
var map__69743__$1 = cljs.core.__destructure_map(map__69743);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69743__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69743__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var detail = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69743__$1,new cljs.core.Keyword(null,"detail","detail",-1545345025));
if(cljs.core.truth_(malli.core.validate.cljs$core$IFn$_invoke$arity$2(schema,value))){
return value;
} else {
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (400);
}
})(),(function (){var or__5142__auto__ = error;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "invalid_request";
}
})(),(function (){var or__5142__auto__ = detail;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.routes.mcp.validation_detail(schema,value);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "Invalid request";
}
}
})());
}
});
knoxx.backend.routes.mcp.handle_route_error_BANG_ = (function knoxx$backend$routes$mcp$handle_route_error_BANG_(reply,err){
if(cljs.core.truth_((reply["sent"]))){
return null;
} else {
var data = cljs.core.ex_data(err);
var status = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(data);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (500);
}
})();
var error = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"error","error",-978969032).cljs$core$IFn$_invoke$arity$1(data);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "internal_error";
}
})();
var detail = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"detail","detail",-1545345025).cljs$core$IFn$_invoke$arity$1(data);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__69759 = err;
if((G__69759 == null)){
return null;
} else {
return G__69759.message;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "Unexpected error";
}
}
}
})();
if((status >= (500))){
console.error("[knoxx-mcp] route failed",err);
} else {
}

return knoxx.backend.routes.mcp.json_send_BANG_(reply,status,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),error,new cljs.core.Keyword(null,"detail","detail",-1545345025),detail], null));
}
});
knoxx.backend.routes.mcp.as_promise = (function knoxx$backend$routes$mcp$as_promise(value){
if((value instanceof Promise)){
return value.then((function (r){
if((r == null)){
return undefined;
} else {
return r;
}
}));
} else {
if((value == null)){
return Promise.resolve(undefined);
} else {
return Promise.resolve(value);

}
}
});
/**
 * Returns a Fastify preHandler hook that attaches redis client to request.redis.
 * Derefs the global redis-client atom at request time, not at route-registration time,
 * so connections established after startup are visible to all handlers.
 */
knoxx.backend.routes.mcp.require_redis_BANG_ = (function knoxx$backend$routes$mcp$require_redis_BANG_(_ignored){
return (function (req,_reply,done){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
(req["redis"] = client);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
} else {
var G__69765 = cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("Redis unavailable",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),(503),new cljs.core.Keyword(null,"error","error",-978969032),"redis_unavailable"], null));
return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(G__69765) : done.call(null,G__69765));
}
});
});
/**
 * Returns a Fastify preHandler hook that resolves browser auth context onto request.authContext.
 */
knoxx.backend.routes.mcp.require_browser_auth_BANG_ = (function knoxx$backend$routes$mcp$require_browser_auth_BANG_(policy_db,config){
return (function (req,reply,done){
return knoxx.backend.auth.session.resolve_auth_context(req,policy_db).then((function (auth_ctx){
(req["authContext"] = auth_ctx);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
})).catch((function (_){
var base = knoxx.backend.routes.mcp.public_base_url(config);
var current_path = (function (){var or__5142__auto__ = (function (){var G__69776 = req;
var G__69776__$1 = (((G__69776 == null))?null:(G__69776["raw"]));
if((G__69776__$1 == null)){
return null;
} else {
return (G__69776__$1["url"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/api/mcp/oauth/authorize";
}
})();
var login_url = (new URL("/api/auth/login",base));
login_url.searchParams.set("redirect",current_path);

return reply.redirect(login_url.toString(),(302));
}));
});
});
/**
 * Returns a Fastify preHandler hook that extracts bearer token onto request.bearerToken.
 */
knoxx.backend.routes.mcp.require_bearer_token_BANG_ = (function knoxx$backend$routes$mcp$require_bearer_token_BANG_(base){
return (function (req,reply,done){
var token = knoxx.backend.routes.mcp.bearer_token(req);
if(clojure.string.blank_QMARK_(token)){
return knoxx.backend.routes.mcp.challenge_unauthorized_BANG_(reply,base);
} else {
(req["bearerToken"] = token);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}
});
});
knoxx.backend.routes.mcp.parse_register_client_body = (function knoxx$backend$routes$mcp$parse_register_client_body(req){
var body = (function (){var or__5142__auto__ = (req["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var value = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"redirect-uris","redirect-uris",778927369),(cljs.core.truth_(cljs.core.array_QMARK_((body["redirect_uris"])))?cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((body["redirect_uris"]))):cljs.core.PersistentVector.EMPTY),new cljs.core.Keyword(null,"client-name","client-name",1843891115),(function (){var G__69795 = (body["client_name"]);
if((G__69795 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69795));
}
})()], null);
var parsed = knoxx.backend.routes.mcp.validate_BANG_(knoxx.backend.routes.mcp.RegisterClientBody,value,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),(400),new cljs.core.Keyword(null,"error","error",-978969032),"invalid_client_metadata",new cljs.core.Keyword(null,"detail","detail",-1545345025),"redirect_uris is required"], null));
if(cljs.core.empty_QMARK_(new cljs.core.Keyword(null,"redirect-uris","redirect-uris",778927369).cljs$core$IFn$_invoke$arity$1(parsed))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_client_metadata","redirect_uris is required");
} else {
}

return parsed;
});
knoxx.backend.routes.mcp.parse_authorize_query = (function knoxx$backend$routes$mcp$parse_authorize_query(req){
var q = (function (){var or__5142__auto__ = (req["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
return knoxx.backend.routes.mcp.validate_BANG_(knoxx.backend.routes.mcp.AuthorizeQuery,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"client-id","client-id",-464622140),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["client_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["redirect_uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"state","state",-1988618099),(function (){var temp__5825__auto__ = (q["state"]);
if(cljs.core.truth_(temp__5825__auto__)){
var s = temp__5825__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s));
} else {
return null;
}
})(),new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["code_challenge"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["code_challenge_method"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "S256";
}
})())),new cljs.core.Keyword(null,"scope","scope",-439358418),(function (){var temp__5825__auto__ = (q["scope"]);
if(cljs.core.truth_(temp__5825__auto__)){
var scope = temp__5825__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(scope));
} else {
return null;
}
})()], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),(400),new cljs.core.Keyword(null,"error","error",-978969032),"invalid_request"], null));
});
knoxx.backend.routes.mcp.parse_authorize_confirm_query = (function knoxx$backend$routes$mcp$parse_authorize_confirm_query(req){
var q = (function (){var or__5142__auto__ = (req["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
return knoxx.backend.routes.mcp.validate_BANG_(knoxx.backend.routes.mcp.AuthorizeConfirmQuery,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"client-id","client-id",-464622140),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["client_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["redirect_uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"state","state",-1988618099),(function (){var temp__5825__auto__ = (q["state"]);
if(cljs.core.truth_(temp__5825__auto__)){
var s = temp__5825__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(s));
} else {
return null;
}
})(),new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["code_challenge"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (q["code_challenge_method"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "S256";
}
})())),new cljs.core.Keyword(null,"scope","scope",-439358418),(function (){var temp__5825__auto__ = (q["scope"]);
if(cljs.core.truth_(temp__5825__auto__)){
var scope = temp__5825__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(scope));
} else {
return null;
}
})(),new cljs.core.Keyword(null,"selected-tools","selected-tools",700501530),knoxx.backend.routes.mcp.normalize_tool_selection((q["tool"]))], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),(400),new cljs.core.Keyword(null,"error","error",-978969032),"invalid_request"], null));
});
knoxx.backend.routes.mcp.parse_token_exchange_body = (function knoxx$backend$routes$mcp$parse_token_exchange_body(req){
var body = (function (){var or__5142__auto__ = (req["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
return knoxx.backend.routes.mcp.validate_BANG_(knoxx.backend.routes.mcp.TokenExchangeBody,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"grant-type","grant-type",-1751533246),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["grant_type"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["grantType"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())),new cljs.core.Keyword(null,"code","code",1586293142),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["code"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"code-verifier","code-verifier",-848846001),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["code_verifier"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["codeVerifier"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())),new cljs.core.Keyword(null,"client-id","client-id",-464622140),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["client_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["clientId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())),new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["redirect_uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["redirectUri"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()))], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),(400),new cljs.core.Keyword(null,"error","error",-978969032),"invalid_request"], null));
});
knoxx.backend.routes.mcp.parse_revoke_token_params = (function knoxx$backend$routes$mcp$parse_revoke_token_params(req){
var params = (function (){var or__5142__auto__ = (req["params"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
return knoxx.backend.routes.mcp.validate_BANG_(knoxx.backend.routes.mcp.RevokeTokenParams,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"token-id","token-id",-764089526),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (params["tokenId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),(400),new cljs.core.Keyword(null,"error","error",-978969032),"invalid_request"], null));
});
knoxx.backend.routes.mcp.ensure_oauth_request_BANG_ = (function knoxx$backend$routes$mcp$ensure_oauth_request_BANG_(p__69799){
var map__69800 = p__69799;
var map__69800__$1 = cljs.core.__destructure_map(map__69800);
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69800__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69800__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
var code_challenge = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69800__$1,new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507));
var code_challenge_method = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69800__$1,new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712));
if(((clojure.string.blank_QMARK_(client_id)) || (((clojure.string.blank_QMARK_(redirect_uri)) || (((clojure.string.blank_QMARK_(code_challenge)) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(code_challenge_method,"S256")))))))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_request","Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)");
} else {
return null;
}
});
knoxx.backend.routes.mcp.ensure_oauth_confirm_request_BANG_ = (function knoxx$backend$routes$mcp$ensure_oauth_confirm_request_BANG_(p__69803){
var map__69804 = p__69803;
var map__69804__$1 = cljs.core.__destructure_map(map__69804);
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69804__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69804__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
var code_challenge = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69804__$1,new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507));
var code_challenge_method = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69804__$1,new cljs.core.Keyword(null,"code-challenge-method","code-challenge-method",-705359712));
if(((clojure.string.blank_QMARK_(client_id)) || (((clojure.string.blank_QMARK_(redirect_uri)) || (((clojure.string.blank_QMARK_(code_challenge)) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(code_challenge_method,"S256")))))))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_request","Missing required OAuth parameters");
} else {
return null;
}
});
knoxx.backend.routes.mcp.get_registered_client = (function knoxx$backend$routes$mcp$get_registered_client(redis_client,client_id){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(client_id)))){
return Promise.resolve(null);
} else {
return knoxx.backend.routes.mcp.redis_get_BANG_(redis_client,(""+"knoxx:mcp:client:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(client_id))).then((function (raw){
if(cljs.core.truth_(raw)){
try{return JSON.parse(raw);
}catch (e69805){var _ = e69805;
return null;
}} else {
return null;
}
})).catch((function (_){
return null;
}));
}
});
knoxx.backend.routes.mcp.redirect_uri_allowed_QMARK_ = (function knoxx$backend$routes$mcp$redirect_uri_allowed_QMARK_(client,redirect_uri){
if(cljs.core.not(client)){
return true;
} else {
return cljs.core.boolean$(Array.from((function (){var or__5142__auto__ = (client["redirect_uris"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()).includes(redirect_uri));
}
});
knoxx.backend.routes.mcp.ensure_redirect_uri_allowed_BANG_ = (function knoxx$backend$routes$mcp$ensure_redirect_uri_allowed_BANG_(client,redirect_uri,error_code){
if(cljs.core.truth_((function (){var and__5140__auto__ = client;
if(cljs.core.truth_(and__5140__auto__)){
return (!(knoxx.backend.routes.mcp.redirect_uri_allowed_QMARK_(client,redirect_uri)));
} else {
return and__5140__auto__;
}
})())){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),error_code,"redirect_uri not allowed for registered client");
} else {
return null;
}
});
knoxx.backend.routes.mcp.available_tools = (function knoxx$backend$routes$mcp$available_tools(runtime,config,auth_context){
var or__5142__auto__ = knoxx.backend.mcp_expose.create_knoxx_custom_tools_js(runtime,config,auth_context);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
});
knoxx.backend.routes.mcp.tool_name_set = (function knoxx$backend$routes$mcp$tool_name_set(tools){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (t){
var G__69812 = (t["name"]);
var G__69812__$1 = (((G__69812 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69812)));
var G__69812__$2 = (((G__69812__$1 == null))?null:clojure.string.trim(G__69812__$1));
if((G__69812__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69812__$2);
}
})),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(tools));
});
knoxx.backend.routes.mcp.selected_tools_from_scope = (function knoxx$backend$routes$mcp$selected_tools_from_scope(tools,requested_scope){
var requested = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.map.cljs$core$IFn$_invoke$arity$1(clojure.string.trim),cljs.core.remove.cljs$core$IFn$_invoke$arity$1(clojure.string.blank_QMARK_)),clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = requested_scope;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\s+/));
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (t){
var n = (function (){var G__69814 = (t["name"]);
var G__69814__$1 = (((G__69814 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69814)));
var G__69814__$2 = (((G__69814__$1 == null))?null:clojure.string.trim(G__69814__$1));
if((G__69814__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69814__$2);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = n;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.contains_QMARK_(requested,"all")) || (cljs.core.contains_QMARK_(requested,n)));
} else {
return and__5140__auto__;
}
})())){
return n;
} else {
return null;
}
})),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(tools));
});
knoxx.backend.routes.mcp.default_selected_tools = (function knoxx$backend$routes$mcp$default_selected_tools(tool_names){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.filter.cljs$core$IFn$_invoke$arity$1((function (p1__69816_SHARP_){
return cljs.core.contains_QMARK_(tool_names,p1__69816_SHARP_);
})),new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, ["semantic_query","semantic_read","memory_search","memory_session","graph_query","websearch","read"], null));
});
knoxx.backend.routes.mcp.requested_tools = (function knoxx$backend$routes$mcp$requested_tools(runtime,config,auth_context,selected_tools){
var tools = knoxx.backend.routes.mcp.available_tools(runtime,config,auth_context);
var available = knoxx.backend.routes.mcp.tool_name_set(tools);
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__69817_SHARP_){
return cljs.core.contains_QMARK_(available,p1__69817_SHARP_);
}),cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.comp.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,cljs.core.str),selected_tools)))));
});
knoxx.backend.routes.mcp.tool_checkbox_html = (function knoxx$backend$routes$mcp$tool_checkbox_html(tools,selected){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (tool){
var n = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (tool["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var label = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (tool["label"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (tool["name"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (tool["description"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return n;
}
}
}
})()));
var desc = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (tool["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var checked = ((cljs.core.contains_QMARK_(selected,n))?"checked":"");
return (""+"\n        <label style=\"display:block; margin: 6px 0;\">\n"+"          <input type=\"checkbox\" name=\"tool\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(n))+"\" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(checked)+" />\n"+"          <span style=\"font-weight:600;\">"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(label))+"</span>\n"+"          <span style=\"color:#666;\">("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(n))+")</span>\n"+"          <div style=\"color:#444; margin-left: 22px;\">"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(desc))+"</div>\n"+"        </label>\n");
}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(tools)));
});
knoxx.backend.routes.mcp.authorization_consent_html = (function knoxx$backend$routes$mcp$authorization_consent_html(p__69820){
var map__69821 = p__69820;
var map__69821__$1 = cljs.core.__destructure_map(map__69821);
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"base","base",185279322));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var selected = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"selected","selected",574897764));
var code_challenge = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507));
var auth_context = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"auth-context","auth-context",320032325));
var tools = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"tools","tools",-1241731990));
var requested_scope = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"requested-scope","requested-scope",-712807637));
var state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69821__$1,new cljs.core.Keyword(null,"state","state",-1988618099));
var confirm_url = (new URL("/api/mcp/oauth/authorize/confirm",base));
var user_email = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["user"]["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["userEmail"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var org_slug = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["org"]["slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["orgSlug"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
confirm_url.searchParams.set("client_id",client_id);

confirm_url.searchParams.set("redirect_uri",redirect_uri);

if(cljs.core.truth_(state)){
confirm_url.searchParams.set("state",state);
} else {
}

confirm_url.searchParams.set("code_challenge",code_challenge);

confirm_url.searchParams.set("code_challenge_method","S256");

if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = requested_scope;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
} else {
confirm_url.searchParams.set("scope",requested_scope);
}

return (""+"<!doctype html>\n<html><head><meta charset=\"utf-8\" />\n"+"<title>Authorize MCP Client</title>\n"+"<style>body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;margin:24px;}"+".box{max-width:920px;} .meta{color:#555;margin-bottom:12px;}"+".tools{border:1px solid #ddd;border-radius:8px;padding:12px 16px;}"+".actions{margin-top:18px;display:flex;gap:12px;}"+"button{padding:8px 14px;border-radius:8px;border:1px solid #333;background:#111;color:#fff;cursor:pointer;}"+"a{color:#0b67d0;}"+"</style></head><body><div class=\"box\">\n"+"<h1>Authorize MCP Client</h1>\n"+"<div class=\"meta\">\n"+"<div><strong>Client:</strong> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(client_id))+"</div>\n"+"<div><strong>Redirect URI:</strong> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(redirect_uri))+"</div>\n"+"<div><strong>User:</strong> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(user_email))+"</div>\n"+"<div><strong>Org:</strong> "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(org_slug))+"</div>\n"+"</div>\n"+"<form method=\"GET\" action=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(confirm_url.pathname))+"\">\n"+"<input type=\"hidden\" name=\"client_id\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(client_id))+"\" />\n"+"<input type=\"hidden\" name=\"redirect_uri\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(redirect_uri))+"\" />\n"+"<input type=\"hidden\" name=\"state\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe((function (){var or__5142__auto__ = state;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))+"\" />\n"+"<input type=\"hidden\" name=\"code_challenge\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(code_challenge))+"\" />\n"+"<input type=\"hidden\" name=\"code_challenge_method\" value=\"S256\" />\n"+"<input type=\"hidden\" name=\"scope\" value=\""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.safe(requested_scope))+"\" />\n"+"<h2>Capabilities</h2>\n"+"<p>Select exactly which Knoxx tools this client can call. You can always revoke tokens later.</p>\n"+"<div class=\"tools\">\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.mcp.tool_checkbox_html(tools,selected))+"</div>\n"+"<div class=\"actions\">\n"+"<button type=\"submit\">Authorize</button>\n"+"<a href=\"/\">Cancel</a>\n"+"</div></form></div></body></html>");
});
knoxx.backend.routes.mcp.load_token_record_BANG_ = (function knoxx$backend$routes$mcp$load_token_record_BANG_(redis_client,access_token){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(access_token)))){
return Promise.resolve(null);
} else {
return knoxx.backend.routes.mcp.redis_get_BANG_(redis_client,(""+"knoxx:mcp:token:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(access_token))).then((function (raw){
if(cljs.core.truth_(raw)){
try{return JSON.parse(raw);
}catch (e69824){var _ = e69824;
return null;
}} else {
return null;
}
})).catch((function (_){
return null;
}));
}
});
knoxx.backend.routes.mcp.resolve_token_context_BANG_ = (function knoxx$backend$routes$mcp$resolve_token_context_BANG_(policy_db,token_record){
var headers_like = ({});
var resolver = (policy_db["resolveRequestContext"]);
var temp__5825__auto___70057 = (token_record["membershipId"]);
if(cljs.core.truth_(temp__5825__auto___70057)){
var mid_70058 = temp__5825__auto___70057;
(headers_like["x-knoxx-membership-id"] = mid_70058);
} else {
}

var temp__5825__auto___70059 = (token_record["userEmail"]);
if(cljs.core.truth_(temp__5825__auto___70059)){
var ue_70060 = temp__5825__auto___70059;
(headers_like["x-knoxx-user-email"] = ue_70060);
} else {
}

var temp__5825__auto___70061 = (token_record["orgSlug"]);
if(cljs.core.truth_(temp__5825__auto___70061)){
var os_70062 = temp__5825__auto___70061;
(headers_like["x-knoxx-org-slug"] = os_70062);
} else {
}

return resolver.call(policy_db,headers_like);
});
knoxx.backend.routes.mcp.apply_zod_description = (function knoxx$backend$routes$mcp$apply_zod_description(schema_node,schema_json){
var description = (function (){var G__69826 = (schema_json["description"]);
var G__69826__$1 = (((G__69826 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69826)));
var G__69826__$2 = (((G__69826__$1 == null))?null:clojure.string.trim(G__69826__$1));
if((G__69826__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69826__$2);
}
})();
if(cljs.core.truth_(description)){
return schema_node.describe(description);
} else {
return schema_node;
}
});
knoxx.backend.routes.mcp.typebox__GT_zod_node = (function knoxx$backend$routes$mcp$typebox__GT_zod_node(z,schema_json){
var schema_type = (schema_json["type"]);
var node = (function (){var G__69829 = schema_type;
switch (G__69829) {
case "string":
return z.string();

break;
case "number":
return z.number();

break;
case "integer":
return z.number().int();

break;
case "boolean":
return z.boolean();

break;
case "array":
return z.array((function (){var or__5142__auto__ = (function (){var G__69831 = z;
var G__69832 = (schema_json["items"]);
return (knoxx.backend.routes.mcp.typebox__GT_zod_node.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.routes.mcp.typebox__GT_zod_node.cljs$core$IFn$_invoke$arity$2(G__69831,G__69832) : knoxx.backend.routes.mcp.typebox__GT_zod_node.call(null,G__69831,G__69832));
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return z.any();
}
})());

break;
case "object":
var or__5142__auto__ = (knoxx.backend.routes.mcp.typebox__GT_zod_shape.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.routes.mcp.typebox__GT_zod_shape.cljs$core$IFn$_invoke$arity$2(z,schema_json) : knoxx.backend.routes.mcp.typebox__GT_zod_shape.call(null,z,schema_json));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return z.object(({}));
}

break;
default:
return z.any();

}
})();
return (function (n){
var temp__5823__auto__ = (schema_json["maximum"]);
if(cljs.core.truth_(temp__5823__auto__)){
var max = temp__5823__auto__;
return n.max(max);
} else {
return n;
}
})((function (n){
var temp__5823__auto__ = (schema_json["minimum"]);
if(cljs.core.truth_(temp__5823__auto__)){
var min = temp__5823__auto__;
return n.min(min);
} else {
return n;
}
})(knoxx.backend.routes.mcp.apply_zod_description(node,schema_json)));
});
knoxx.backend.routes.mcp.typebox__GT_zod_shape = (function knoxx$backend$routes$mcp$typebox__GT_zod_shape(z,schema_json){
var properties = (function (){var or__5142__auto__ = (schema_json["properties"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var required_set = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(cljs.core.str),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (schema_json["required"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()));
var entries = Object.entries(properties);
if(cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(entries))){
return cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (shape,entry){
var fname = (entry[(0)]);
var fschema = knoxx.backend.routes.mcp.typebox__GT_zod_node(z,(entry[(1)]));
var final$ = ((cljs.core.contains_QMARK_(required_set,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(fname))))?fschema:fschema.optional());
(shape[fname] = final$);

return shape;
}),({}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(entries));
} else {
return null;
}
});
knoxx.backend.routes.mcp.mcp_discovery_metadata_BANG_ = (function knoxx$backend$routes$mcp$mcp_discovery_metadata_BANG_(app,runtime,config,deps){
var map__69844 = deps;
var map__69844__$1 = cljs.core.__destructure_map(map__69844);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"base","base",185279322));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69844__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69845 = app;
var G__69846 = "GET";
var G__69847 = "/.well-known/oauth-authorization-server";
var G__69848 = (function (request,reply){
var G__69849 = runtime;
var G__69850 = request;
var G__69851 = reply;
var G__69852 = (function (ctx){
var issuer = (new URL(base.toString()));
return reply.send(({"issuer": issuer.toString().replace((new RegExp("/$")),""), "authorization_endpoint": (new URL("/api/mcp/oauth/authorize",issuer)).toString(), "token_endpoint": (new URL("/api/mcp/oauth/token",issuer)).toString(), "registration_endpoint": (new URL("/api/mcp/oauth/register",issuer)).toString(), "response_types_supported": ["code"], "grant_types_supported": ["authorization_code"], "code_challenge_methods_supported": ["S256"], "token_endpoint_auth_methods_supported": ["none"]}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69849,G__69850,G__69851,G__69852) : with_request_context_BANG_.call(null,G__69849,G__69850,G__69851,G__69852));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69845,G__69846,G__69847,G__69848) : route_BANG_.call(null,G__69845,G__69846,G__69847,G__69848));
});
knoxx.backend.routes.mcp.mcp_protected_resource_metadata_BANG_ = (function knoxx$backend$routes$mcp$mcp_protected_resource_metadata_BANG_(app,runtime,config,deps){
var map__69854 = deps;
var map__69854__$1 = cljs.core.__destructure_map(map__69854);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"base","base",185279322));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69854__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69855 = app;
var G__69856 = "GET";
var G__69857 = "/.well-known/oauth-protected-resource";
var G__69858 = (function (request,reply){
var G__69859 = runtime;
var G__69860 = request;
var G__69861 = reply;
var G__69862 = (function (ctx){
var issuer = (new URL(base.toString())).toString().replace((new RegExp("/$")),"");
return reply.send(({"resource": (new URL("/mcp",base)).toString(), "authorization_servers": [issuer], "scopes_supported": ["mcp:tools"], "bearer_methods_supported": ["header"]}));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__69859,G__69860,G__69861,G__69862) : with_request_context_BANG_.call(null,G__69859,G__69860,G__69861,G__69862));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69855,G__69856,G__69857,G__69858) : route_BANG_.call(null,G__69855,G__69856,G__69857,G__69858));
});
knoxx.backend.routes.mcp.mcp_register_client_BANG_ = (function knoxx$backend$routes$mcp$mcp_register_client_BANG_(app,runtime,config,deps){
var map__69863 = deps;
var map__69863__$1 = cljs.core.__destructure_map(map__69863);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var crypto = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69863__$1,new cljs.core.Keyword(null,"crypto","crypto",1772582615));
var G__69864 = app;
var G__69865 = "POST";
var G__69866 = "/api/mcp/oauth/register";
var G__69867 = (function (){var obj69869 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var map__69870 = knoxx.backend.routes.mcp.parse_register_client_body(request);
var map__69870__$1 = cljs.core.__destructure_map(map__69870);
var redirect_uris = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69870__$1,new cljs.core.Keyword(null,"redirect-uris","redirect-uris",778927369));
var client_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69870__$1,new cljs.core.Keyword(null,"client-name","client-name",1843891115));
var client_id = crypto.randomUUID();
var client = ({"client_id": client_id, "client_name": (function (){var or__5142__auto__ = client_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "mcp-client";
}
})(), "redirect_uris": cljs.core.clj__GT_js(redirect_uris), "token_endpoint_auth_method": "none", "grant_types": ["authorization_code"], "response_types": ["code"], "created_at": (new Date()).toISOString()});
return knoxx.backend.routes.mcp.redis_set_BANG_(redis,(""+"knoxx:mcp:client:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(client_id)),JSON.stringify(client),undefined).then((function (_){
return reply.code((201)).send(client);
})).catch((function (err){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((500),"registration_failed",(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})());
}));
})});
return obj69869;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69864,G__69865,G__69866,G__69867) : route_BANG_.call(null,G__69864,G__69865,G__69866,G__69867));
});
knoxx.backend.routes.mcp.mcp_authorize_client_BANG_ = (function knoxx$backend$routes$mcp$mcp_authorize_client_BANG_(app,runtime,config,deps){
var map__69871 = deps;
var map__69871__$1 = cljs.core.__destructure_map(map__69871);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"base","base",185279322));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var config__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"config","config",994861415));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var browser_auth_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"browser-auth-guard","browser-auth-guard",-1183678191));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var runtime__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69871__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69872 = app;
var G__69873 = "GET";
var G__69874 = "/api/mcp/oauth/authorize";
var G__69875 = (function (){var obj69877 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard,browser_auth_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var auth_context = (request["authContext"]);
var map__69878 = knoxx.backend.routes.mcp.parse_authorize_query(request);
var map__69878__$1 = cljs.core.__destructure_map(map__69878);
var params = map__69878__$1;
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69878__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69878__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
var state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69878__$1,new cljs.core.Keyword(null,"state","state",-1988618099));
var code_challenge = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69878__$1,new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507));
var scope = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69878__$1,new cljs.core.Keyword(null,"scope","scope",-439358418));
knoxx.backend.routes.mcp.ensure_oauth_request_BANG_(params);

return knoxx.backend.routes.mcp.get_registered_client(redis,client_id).then((function (client){
knoxx.backend.routes.mcp.ensure_redirect_uri_allowed_BANG_(client,redirect_uri,"invalid_request");

var tools = knoxx.backend.routes.mcp.available_tools(runtime__$1,config__$1,auth_context);
var selected = (function (){var explicit = knoxx.backend.routes.mcp.selected_tools_from_scope(tools,scope);
if(cljs.core.seq(explicit)){
return explicit;
} else {
return knoxx.backend.routes.mcp.default_selected_tools(knoxx.backend.routes.mcp.tool_name_set(tools));
}
})();
var html = knoxx.backend.routes.mcp.authorization_consent_html(cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842),new cljs.core.Keyword(null,"client-id","client-id",-464622140),new cljs.core.Keyword(null,"selected","selected",574897764),new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507),new cljs.core.Keyword(null,"auth-context","auth-context",320032325),new cljs.core.Keyword(null,"tools","tools",-1241731990),new cljs.core.Keyword(null,"requested-scope","requested-scope",-712807637),new cljs.core.Keyword(null,"state","state",-1988618099),new cljs.core.Keyword(null,"base","base",185279322)],[redirect_uri,client_id,selected,code_challenge,auth_context,tools,(function (){var or__5142__auto__ = scope;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),state,base]));
return knoxx.backend.routes.mcp.reply_header_BANG_(reply,"content-type","text/html; charset=utf-8").send(html);
}));
})});
return obj69877;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69872,G__69873,G__69874,G__69875) : route_BANG_.call(null,G__69872,G__69873,G__69874,G__69875));
});
knoxx.backend.routes.mcp.mcp_authorize_confirm_BANG_ = (function knoxx$backend$routes$mcp$mcp_authorize_confirm_BANG_(app,runtime,config,deps){
var map__69879 = deps;
var map__69879__$1 = cljs.core.__destructure_map(map__69879);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"base","base",185279322));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var code_ttl = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"code-ttl","code-ttl",-1627471037));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var config__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"config","config",994861415));
var token_ttl = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"token-ttl","token-ttl",-103977687));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var browser_auth_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"browser-auth-guard","browser-auth-guard",-1183678191));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var runtime__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var crypto = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69879__$1,new cljs.core.Keyword(null,"crypto","crypto",1772582615));
var G__69880 = app;
var G__69881 = "GET";
var G__69882 = "/api/mcp/oauth/authorize/confirm";
var G__69883 = (function (){var obj69885 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard,browser_auth_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var auth_context = (request["authContext"]);
var map__69886 = knoxx.backend.routes.mcp.parse_authorize_confirm_query(request);
var map__69886__$1 = cljs.core.__destructure_map(map__69886);
var params = map__69886__$1;
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69886__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69886__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
var state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69886__$1,new cljs.core.Keyword(null,"state","state",-1988618099));
var code_challenge = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69886__$1,new cljs.core.Keyword(null,"code-challenge","code-challenge",-46720507));
var selected_tools = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69886__$1,new cljs.core.Keyword(null,"selected-tools","selected-tools",700501530));
knoxx.backend.routes.mcp.ensure_oauth_confirm_request_BANG_(params);

return knoxx.backend.routes.mcp.get_registered_client(redis,client_id).then((function (client){
knoxx.backend.routes.mcp.ensure_redirect_uri_allowed_BANG_(client,redirect_uri,"invalid_request");

var requested = knoxx.backend.routes.mcp.requested_tools(runtime__$1,config__$1,auth_context,selected_tools);
if(cljs.core.empty_QMARK_(requested)){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_scope","No valid tools selected");
} else {
}

var membership_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["membership"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["membershipId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var user_email = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["user"]["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["userEmail"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var org_slug = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["org"]["slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["orgSlug"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var code = crypto.randomUUID();
var payload = ({"redirectUri": redirect_uri, "clientId": client_id, "codeChallengeMethod": "S256", "tools": cljs.core.clj__GT_js(requested), "orgSlug": org_slug, "codeChallenge": code_challenge, "createdAt": (new Date()).toISOString(), "membershipId": membership_id, "code": code, "userEmail": user_email});
return knoxx.backend.routes.mcp.redis_set_BANG_(redis,(""+"knoxx:mcp:code:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code)),JSON.stringify(payload),({"EX": code_ttl})).then((function (_){
var redir = (new URL(redirect_uri));
redir.searchParams.set("code",code);

if(cljs.core.truth_(state)){
redir.searchParams.set("state",state);
} else {
}

return reply.redirect(redir.toString(),(302));
}));
}));
})});
return obj69885;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69880,G__69881,G__69882,G__69883) : route_BANG_.call(null,G__69880,G__69881,G__69882,G__69883));
});
knoxx.backend.routes.mcp.persist_access_token_BANG_ = (function knoxx$backend$routes$mcp$persist_access_token_BANG_(redis,crypto,token_ttl,client_id,record){
var access_token = crypto.randomUUID();
var token_value = ({"accessToken": access_token, "clientId": client_id, "membershipId": (record["membershipId"]), "userEmail": (record["userEmail"]), "orgSlug": (record["orgSlug"]), "tools": (record["tools"]), "createdAt": (new Date()).toISOString(), "expiresAt": (new Date((Date.now() + (token_ttl * (1000))))).toISOString()});
return knoxx.backend.routes.mcp.redis_set_BANG_(redis,(""+"knoxx:mcp:token:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(access_token)),JSON.stringify(token_value),({"EX": token_ttl})).then((function (_){
var temp__5823__auto__ = (record["membershipId"]);
if(cljs.core.truth_(temp__5823__auto__)){
var mid = temp__5823__auto__;
return knoxx.backend.routes.mcp.redis_sadd_BANG_(redis,(""+"knoxx:mcp:user:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mid)+":tokens"),access_token);
} else {
return Promise.resolve(null);
}
})).then((function (_){
return ({"access_token": access_token, "token_type": "Bearer", "scope": clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (record["tools"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})())), "expires_in": token_ttl});
}));
});
knoxx.backend.routes.mcp.mcp_exchange_token_BANG_ = (function knoxx$backend$routes$mcp$mcp_exchange_token_BANG_(app,runtime,config,deps){
var map__69887 = deps;
var map__69887__$1 = cljs.core.__destructure_map(map__69887);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var token_ttl = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"token-ttl","token-ttl",-103977687));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var crypto = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69887__$1,new cljs.core.Keyword(null,"crypto","crypto",1772582615));
var G__69888 = app;
var G__69889 = "POST";
var G__69890 = "/api/mcp/oauth/token";
var G__69891 = (function (){var obj69893 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var map__69894 = knoxx.backend.routes.mcp.parse_token_exchange_body(request);
var map__69894__$1 = cljs.core.__destructure_map(map__69894);
var grant_type = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69894__$1,new cljs.core.Keyword(null,"grant-type","grant-type",-1751533246));
var code = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69894__$1,new cljs.core.Keyword(null,"code","code",1586293142));
var code_verifier = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69894__$1,new cljs.core.Keyword(null,"code-verifier","code-verifier",-848846001));
var client_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69894__$1,new cljs.core.Keyword(null,"client-id","client-id",-464622140));
var redirect_uri = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69894__$1,new cljs.core.Keyword(null,"redirect-uri","redirect-uri",374475842));
if(((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(grant_type,"authorization_code")) || (((clojure.string.blank_QMARK_(code)) || (((clojure.string.blank_QMARK_(code_verifier)) || (((clojure.string.blank_QMARK_(client_id)) || (clojure.string.blank_QMARK_(redirect_uri)))))))))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_request","Missing required token exchange parameters");
} else {
}

return knoxx.backend.routes.mcp.get_registered_client(redis,client_id).then((function (client){
knoxx.backend.routes.mcp.ensure_redirect_uri_allowed_BANG_(client,redirect_uri,"invalid_grant");

return knoxx.backend.routes.mcp.redis_get_BANG_(redis,(""+"knoxx:mcp:code:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code)));
})).then((function (raw){
if(cljs.core.truth_(raw)){
} else {
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_grant","Unknown or expired code");
}

var record = JSON.parse(raw);
var expected = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (record["codeChallenge"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var actual = knoxx.backend.routes.mcp.pkce_challenge(crypto,code_verifier);
if(((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((record["clientId"]),client_id)) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((record["redirectUri"]),redirect_uri)))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_grant","Client/redirect mismatch");
} else {
}

if(((clojure.string.blank_QMARK_(expected)) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(expected,actual)))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_grant","PKCE verification failed");
} else {
}

return knoxx.backend.routes.mcp.redis_del_BANG_(redis,(""+"knoxx:mcp:code:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code))).then((function (_){
return knoxx.backend.routes.mcp.persist_access_token_BANG_(redis,crypto,token_ttl,client_id,record);
}));
})).then((function (token_response){
return reply.send(token_response);
}));
})});
return obj69893;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69888,G__69889,G__69890,G__69891) : route_BANG_.call(null,G__69888,G__69889,G__69890,G__69891));
});
knoxx.backend.routes.mcp.mcp_list_user_tokens_BANG_ = (function knoxx$backend$routes$mcp$mcp_list_user_tokens_BANG_(app,runtime,config,deps){
var map__69895 = deps;
var map__69895__$1 = cljs.core.__destructure_map(map__69895);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var browser_auth_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"browser-auth-guard","browser-auth-guard",-1183678191));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69895__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69896 = app;
var G__69897 = "GET";
var G__69898 = "/api/mcp/tokens";
var G__69899 = (function (){var obj69901 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard,browser_auth_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var auth_context = (request["authContext"]);
var membership_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["membership"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["membershipId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
if(clojure.string.blank_QMARK_(membership_id)){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"missing_membership","No membership available for this session");
} else {
}

return knoxx.backend.routes.mcp.redis_smembers_BANG_(redis,(""+"knoxx:mcp:user:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(membership_id)+":tokens")).then((function (token_ids){
return Promise.all(cljs.core.clj__GT_js((function (){var iter__5628__auto__ = (function knoxx$backend$routes$mcp$mcp_list_user_tokens_BANG__$_iter__69902(s__69903){
return (new cljs.core.LazySeq(null,(function (){
var s__69903__$1 = s__69903;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__69903__$1);
if(temp__5825__auto__){
var s__69903__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__69903__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__69903__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__69905 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__69904 = (0);
while(true){
if((i__69904 < size__5627__auto__)){
var tid = cljs.core._nth(c__5626__auto__,i__69904);
cljs.core.chunk_append(b__69905,knoxx.backend.routes.mcp.redis_get_BANG_(redis,(""+"knoxx:mcp:token:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tid))).then(((function (i__69904,tid,c__5626__auto__,size__5627__auto__,b__69905,s__69903__$2,temp__5825__auto__,redis,auth_context,membership_id,ctx,G__69896,G__69897,G__69898,map__69895,map__69895__$1,clip_text,redis_guard,ensure_permission_BANG_,fetch_json,route_BANG_,request_query_string,session_guard,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,browser_auth_guard,optional_session_guard,error_response_BANG_,bearer_headers){
return (function (raw){
if(cljs.core.truth_(raw)){
try{return JSON.parse(raw);
}catch (e69906){var _ = e69906;
return null;
}} else {
return null;
}
});})(i__69904,tid,c__5626__auto__,size__5627__auto__,b__69905,s__69903__$2,temp__5825__auto__,redis,auth_context,membership_id,ctx,G__69896,G__69897,G__69898,map__69895,map__69895__$1,clip_text,redis_guard,ensure_permission_BANG_,fetch_json,route_BANG_,request_query_string,session_guard,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,browser_auth_guard,optional_session_guard,error_response_BANG_,bearer_headers))
));

var G__70139 = (i__69904 + (1));
i__69904 = G__70139;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__69905),knoxx$backend$routes$mcp$mcp_list_user_tokens_BANG__$_iter__69902(cljs.core.chunk_rest(s__69903__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__69905),null);
}
} else {
var tid = cljs.core.first(s__69903__$2);
return cljs.core.cons(knoxx.backend.routes.mcp.redis_get_BANG_(redis,(""+"knoxx:mcp:token:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tid))).then(((function (tid,s__69903__$2,temp__5825__auto__,redis,auth_context,membership_id,ctx,G__69896,G__69897,G__69898,map__69895,map__69895__$1,clip_text,redis_guard,ensure_permission_BANG_,fetch_json,route_BANG_,request_query_string,session_guard,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,browser_auth_guard,optional_session_guard,error_response_BANG_,bearer_headers){
return (function (raw){
if(cljs.core.truth_(raw)){
try{return JSON.parse(raw);
}catch (e69907){var _ = e69907;
return null;
}} else {
return null;
}
});})(tid,s__69903__$2,temp__5825__auto__,redis,auth_context,membership_id,ctx,G__69896,G__69897,G__69898,map__69895,map__69895__$1,clip_text,redis_guard,ensure_permission_BANG_,fetch_json,route_BANG_,request_query_string,session_guard,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,browser_auth_guard,optional_session_guard,error_response_BANG_,bearer_headers))
),knoxx$backend$routes$mcp$mcp_list_user_tokens_BANG__$_iter__69902(cljs.core.rest(s__69903__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(token_ids));
})()));
})).then((function (records){
return reply.send(({"ok": true, "tokens": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(records)))}));
}));
})});
return obj69901;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69896,G__69897,G__69898,G__69899) : route_BANG_.call(null,G__69896,G__69897,G__69898,G__69899));
});
knoxx.backend.routes.mcp.mcp_revoke_user_token_BANG_ = (function knoxx$backend$routes$mcp$mcp_revoke_user_token_BANG_(app,runtime,config,deps){
var map__69908 = deps;
var map__69908__$1 = cljs.core.__destructure_map(map__69908);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var redis_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var browser_auth_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"browser-auth-guard","browser-auth-guard",-1183678191));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69908__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69909 = app;
var G__69910 = "DELETE";
var G__69911 = "/api/mcp/tokens/:tokenId";
var G__69912 = (function (){var obj69914 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [redis_guard,browser_auth_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var redis = (request["redis"]);
var auth_context = (request["authContext"]);
var map__69915 = knoxx.backend.routes.mcp.parse_revoke_token_params(request);
var map__69915__$1 = cljs.core.__destructure_map(map__69915);
var token_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69915__$1,new cljs.core.Keyword(null,"token-id","token-id",-764089526));
var membership_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (auth_context["membership"]["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (auth_context["membershipId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
if(((clojure.string.blank_QMARK_(membership_id)) || (clojure.string.blank_QMARK_(token_id)))){
throw knoxx.backend.routes.mcp.http_error.cljs$core$IFn$_invoke$arity$3((400),"invalid_request","membership and tokenId are required");
} else {
}

return knoxx.backend.routes.mcp.redis_del_BANG_(redis,(""+"knoxx:mcp:token:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token_id))).then((function (_){
return knoxx.backend.routes.mcp.redis_srem_BANG_(redis,(""+"knoxx:mcp:user:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(membership_id)+":tokens"),token_id);
})).then((function (_){
return reply.send(({"ok": true}));
}));
})});
return obj69914;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69909,G__69910,G__69911,G__69912) : route_BANG_.call(null,G__69909,G__69910,G__69911,G__69912));
});
knoxx.backend.routes.mcp.mcp_handle_session_BANG_ = (function knoxx$backend$routes$mcp$mcp_handle_session_BANG_(app,runtime,config,deps){
var map__69916 = deps;
var map__69916__$1 = cljs.core.__destructure_map(map__69916);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"base","base",185279322));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_token_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"bearer-token-guard","bearer-token-guard",1833348950));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69916__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69917 = app;
var G__69918 = "GET";
var G__69919 = "/mcp";
var G__69920 = (function (){var obj69922 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [bearer_token_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var bearer = (request["bearerToken"]);
var session_id = knoxx.backend.routes.mcp.resolve_session_id(request);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))){
return knoxx.backend.routes.mcp.text_send_BANG_(reply,(400),"Missing mcp-session-id");
} else {
var map__69923 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.routes.mcp.mcp_sessions_STAR_),session_id);
var map__69923__$1 = cljs.core.__destructure_map(map__69923);
var transport = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69923__$1,new cljs.core.Keyword(null,"transport","transport",-649001056));
var token = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69923__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
if((transport == null)){
return knoxx.backend.routes.mcp.text_send_BANG_(reply,(404),(""+"Invalid mcp-session-id: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)));
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bearer)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)))){
return knoxx.backend.routes.mcp.challenge_unauthorized_BANG_(reply,base);
} else {
knoxx.backend.routes.mcp.ensure_streamable_accept_BANG_(request);

return knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$3(transport,(request["raw"]),(reply["raw"]));

}
}

}
})});
return obj69922;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69917,G__69918,G__69919,G__69920) : route_BANG_.call(null,G__69917,G__69918,G__69919,G__69920));
});
knoxx.backend.routes.mcp.mcp_handle_delete_session_BANG_ = (function knoxx$backend$routes$mcp$mcp_handle_delete_session_BANG_(app,runtime,config,deps){
var map__69924 = deps;
var map__69924__$1 = cljs.core.__destructure_map(map__69924);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"base","base",185279322));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_token_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"bearer-token-guard","bearer-token-guard",1833348950));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69924__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69925 = app;
var G__69926 = "DELETE";
var G__69927 = "/mcp";
var G__69928 = (function (){var obj69930 = ({"preHandler":(function (request,reply,done){
var guards = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [bearer_token_guard], null);
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
var bearer = (request["bearerToken"]);
var session_id = knoxx.backend.routes.mcp.resolve_session_id(request);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))){
return knoxx.backend.routes.mcp.text_send_BANG_(reply,(400),"Missing mcp-session-id");
} else {
var map__69931 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.routes.mcp.mcp_sessions_STAR_),session_id);
var map__69931__$1 = cljs.core.__destructure_map(map__69931);
var transport = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69931__$1,new cljs.core.Keyword(null,"transport","transport",-649001056));
var token = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69931__$1,new cljs.core.Keyword(null,"token","token",-1211463215));
if((transport == null)){
return knoxx.backend.routes.mcp.text_send_BANG_(reply,(404),(""+"Invalid mcp-session-id: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)));
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bearer)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)))){
return knoxx.backend.routes.mcp.challenge_unauthorized_BANG_(reply,base);
} else {
knoxx.backend.routes.mcp.ensure_streamable_accept_BANG_(request);

return knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$3(transport,(request["raw"]),(reply["raw"]));

}
}

}
})});
return obj69930;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69925,G__69926,G__69927,G__69928) : route_BANG_.call(null,G__69925,G__69926,G__69927,G__69928));
});
knoxx.backend.routes.mcp.mcp_handle_post_BANG_ = (function knoxx$backend$routes$mcp$mcp_handle_post_BANG_(app,runtime,config,deps){
var map__69934 = deps;
var map__69934__$1 = cljs.core.__destructure_map(map__69934);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var base = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"base","base",185279322));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var StreamableHTTPServerTransport = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"StreamableHTTPServerTransport","StreamableHTTPServerTransport",1299523549));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var McpServer = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"McpServer","McpServer",251219330));
var code_ttl = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"code-ttl","code-ttl",-1627471037));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var config__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"config","config",994861415));
var token_ttl = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"token-ttl","token-ttl",-103977687));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var z = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"z","z",-789527183));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var runtime__$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69934__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__69936 = app;
var G__69937 = "POST";
var G__69938 = "/mcp";
var G__69939 = (function (){var obj69941 = ({"preHandler":(function (request,reply,done){
var guards = cljs.core.PersistentVector.EMPTY;
var chain = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (next,guard){
return (function (){
return (guard.cljs$core$IFn$_invoke$arity$3 ? guard.cljs$core$IFn$_invoke$arity$3(request,reply,next) : guard.call(null,request,reply,next));
});
}),done,cljs.core.reverse(guards));
return (chain.cljs$core$IFn$_invoke$arity$0 ? chain.cljs$core$IFn$_invoke$arity$0() : chain.call(null));
}),"handler":(function (request,reply){
var ctx = (request["ctx"]);
reply.hijack();

var raw_req = (request["raw"]);
var raw_res = (reply["raw"]);
var redis = knoxx.backend.redis_client.get_client();
var bearer = knoxx.backend.routes.mcp.bearer_token(request);
if(clojure.string.blank_QMARK_(bearer)){
raw_res.writeHead((401),({"WWW-Authenticate": knoxx.backend.routes.mcp.www_authenticate_challenge(base), "Content-Type": "text/plain"}));

return raw_res.end("Unauthorized");
} else {
return knoxx.backend.routes.mcp.load_token_record_BANG_(redis,bearer).then((function (token_record){
if(cljs.core.not(token_record)){
raw_res.writeHead((401),({"WWW-Authenticate": knoxx.backend.routes.mcp.www_authenticate_challenge(base), "Content-Type": "text/plain"}));

return raw_res.end("Unauthorized");
} else {
return knoxx.backend.routes.mcp.resolve_token_context_BANG_(policy_db,token_record).then((function (token_ctx){
var all_tools = knoxx.backend.routes.mcp.available_tools(runtime__$1,config__$1,token_ctx);
var allowed = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(cljs.core.str),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (token_record["tools"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()));
var effective = cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (t){
return cljs.core.contains_QMARK_(allowed,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((t["name"]))));
}),cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(all_tools)));
var server = (new McpServer(({"name": "knoxx", "version": "0.1.0"})));
var transport = (new StreamableHTTPServerTransport(({"sessionIdGenerator": undefined})));
var seq__69942_70173 = cljs.core.seq(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(effective));
var chunk__69943_70174 = null;
var count__69944_70175 = (0);
var i__69945_70176 = (0);
while(true){
if((i__69945_70176 < count__69944_70175)){
var tool_70177 = chunk__69943_70174.cljs$core$IIndexed$_nth$arity$2(null,i__69945_70176);
var n_70178 = (function (){var G__69948 = (tool_70177["name"]);
var G__69948__$1 = (((G__69948 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69948)));
var G__69948__$2 = (((G__69948__$1 == null))?null:clojure.string.trim(G__69948__$1));
if((G__69948__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69948__$2);
}
})();
var s_70179 = (function (){var or__5142__auto__ = (cljs.core.truth_(z)?knoxx.backend.routes.mcp.typebox__GT_zod_shape(z,(function (){var or__5142__auto__ = (tool_70177["parameters"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})()):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
if(cljs.core.truth_(n_70178)){
server.registerTool(n_70178,({"description": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (tool_70177["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (tool_70177["label"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return n_70178;
}
}
})())), "inputSchema": s_70179}),((function (seq__69942_70173,chunk__69943_70174,count__69944_70175,i__69945_70176,n_70178,s_70179,tool_70177,all_tools,allowed,effective,server,transport,raw_req,raw_res,redis,bearer,ctx,G__69936,G__69937,G__69938,map__69934,map__69934__$1,clip_text,base,ensure_permission_BANG_,StreamableHTTPServerTransport,fetch_json,route_BANG_,request_query_string,policy_db,McpServer,code_ttl,session_guard,config__$1,token_ttl,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,z,optional_session_guard,error_response_BANG_,runtime__$1,bearer_headers){
return (function (params){
return knoxx.backend.routes.mcp.tool_execute_BANG_(tool_70177,params);
});})(seq__69942_70173,chunk__69943_70174,count__69944_70175,i__69945_70176,n_70178,s_70179,tool_70177,all_tools,allowed,effective,server,transport,raw_req,raw_res,redis,bearer,ctx,G__69936,G__69937,G__69938,map__69934,map__69934__$1,clip_text,base,ensure_permission_BANG_,StreamableHTTPServerTransport,fetch_json,route_BANG_,request_query_string,policy_db,McpServer,code_ttl,session_guard,config__$1,token_ttl,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,z,optional_session_guard,error_response_BANG_,runtime__$1,bearer_headers))
);
} else {
}


var G__70185 = seq__69942_70173;
var G__70186 = chunk__69943_70174;
var G__70187 = count__69944_70175;
var G__70188 = (i__69945_70176 + (1));
seq__69942_70173 = G__70185;
chunk__69943_70174 = G__70186;
count__69944_70175 = G__70187;
i__69945_70176 = G__70188;
continue;
} else {
var temp__5825__auto___70193 = cljs.core.seq(seq__69942_70173);
if(temp__5825__auto___70193){
var seq__69942_70194__$1 = temp__5825__auto___70193;
if(cljs.core.chunked_seq_QMARK_(seq__69942_70194__$1)){
var c__5673__auto___70195 = cljs.core.chunk_first(seq__69942_70194__$1);
var G__70196 = cljs.core.chunk_rest(seq__69942_70194__$1);
var G__70197 = c__5673__auto___70195;
var G__70198 = cljs.core.count(c__5673__auto___70195);
var G__70199 = (0);
seq__69942_70173 = G__70196;
chunk__69943_70174 = G__70197;
count__69944_70175 = G__70198;
i__69945_70176 = G__70199;
continue;
} else {
var tool_70200 = cljs.core.first(seq__69942_70194__$1);
var n_70201 = (function (){var G__69949 = (tool_70200["name"]);
var G__69949__$1 = (((G__69949 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__69949)));
var G__69949__$2 = (((G__69949__$1 == null))?null:clojure.string.trim(G__69949__$1));
if((G__69949__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__69949__$2);
}
})();
var s_70202 = (function (){var or__5142__auto__ = (cljs.core.truth_(z)?knoxx.backend.routes.mcp.typebox__GT_zod_shape(z,(function (){var or__5142__auto__ = (tool_70200["parameters"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})()):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
if(cljs.core.truth_(n_70201)){
server.registerTool(n_70201,({"description": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (tool_70200["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (tool_70200["label"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return n_70201;
}
}
})())), "inputSchema": s_70202}),((function (seq__69942_70173,chunk__69943_70174,count__69944_70175,i__69945_70176,n_70201,s_70202,tool_70200,seq__69942_70194__$1,temp__5825__auto___70193,all_tools,allowed,effective,server,transport,raw_req,raw_res,redis,bearer,ctx,G__69936,G__69937,G__69938,map__69934,map__69934__$1,clip_text,base,ensure_permission_BANG_,StreamableHTTPServerTransport,fetch_json,route_BANG_,request_query_string,policy_db,McpServer,code_ttl,session_guard,config__$1,token_ttl,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,z,optional_session_guard,error_response_BANG_,runtime__$1,bearer_headers){
return (function (params){
return knoxx.backend.routes.mcp.tool_execute_BANG_(tool_70200,params);
});})(seq__69942_70173,chunk__69943_70174,count__69944_70175,i__69945_70176,n_70201,s_70202,tool_70200,seq__69942_70194__$1,temp__5825__auto___70193,all_tools,allowed,effective,server,transport,raw_req,raw_res,redis,bearer,ctx,G__69936,G__69937,G__69938,map__69934,map__69934__$1,clip_text,base,ensure_permission_BANG_,StreamableHTTPServerTransport,fetch_json,route_BANG_,request_query_string,policy_db,McpServer,code_ttl,session_guard,config__$1,token_ttl,json_response_BANG_,with_request_context_BANG_,send_fetch_response_BANG_,z,optional_session_guard,error_response_BANG_,runtime__$1,bearer_headers))
);
} else {
}


var G__70205 = cljs.core.next(seq__69942_70194__$1);
var G__70206 = null;
var G__70207 = (0);
var G__70208 = (0);
seq__69942_70173 = G__70205;
chunk__69943_70174 = G__70206;
count__69944_70175 = G__70207;
i__69945_70176 = G__70208;
continue;
}
} else {
}
}
break;
}

return server.connect(transport).then((function (_){
knoxx.backend.routes.mcp.ensure_streamable_accept_BANG_(request);

return knoxx.backend.routes.mcp.transport_handle_request_BANG_.cljs$core$IFn$_invoke$arity$4(transport,raw_req,raw_res,(request["body"]));
}));
}));
}
})).catch((function (err){
console.error("[knoxx-mcp] post failed",err);

if(cljs.core.truth_(raw_res.headersSent)){
return null;
} else {
raw_res.writeHead((500),({"Content-Type": "application/json"}));

return raw_res.end(JSON.stringify(({"error": "mcp_post_failed", "detail": (function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()})));
}
}));
}
})});
return obj69941;
})();
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__69936,G__69937,G__69938,G__69939) : route_BANG_.call(null,G__69936,G__69937,G__69938,G__69939));
});
knoxx.backend.routes.mcp.register_mcp_http_routes_BANG_ = (function knoxx$backend$routes$mcp$register_mcp_http_routes_BANG_(app,runtime,config){
var redis_client = knoxx.backend.redis_client.get_client();
var base = knoxx.backend.routes.mcp.public_base_url(config);
var policy_db = knoxx.backend.runtime.state.current_policy_db();
var code_ttl = parseInt(knoxx.backend.routes.mcp.env("KNOXX_MCP_CODE_TTL_SECONDS","300"),(10));
var token_ttl = parseInt(knoxx.backend.routes.mcp.env("KNOXX_MCP_TOKEN_TTL_SECONDS",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((((60) * (60)) * (24)) * (30))))),(10));
var deps = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"route!","route!",-1286958144),new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183),new cljs.core.Keyword(null,"McpServer","McpServer",251219330),new cljs.core.Keyword(null,"code-ttl","code-ttl",-1627471037),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"token-ttl","token-ttl",-103977687),new cljs.core.Keyword(null,"browser-auth-guard","browser-auth-guard",-1183678191),new cljs.core.Keyword(null,"z","z",-789527183),new cljs.core.Keyword(null,"runtime","runtime",-1331573996),new cljs.core.Keyword(null,"bearer-token-guard","bearer-token-guard",1833348950),new cljs.core.Keyword(null,"crypto","crypto",1772582615),new cljs.core.Keyword(null,"redis-guard","redis-guard",-1977872070),new cljs.core.Keyword(null,"base","base",185279322),new cljs.core.Keyword(null,"StreamableHTTPServerTransport","StreamableHTTPServerTransport",1299523549)],[knoxx.backend.app_shapes.route_BANG_,policy_db,shadow.esm.esm_import$$modelcontextprotocol$sdk$server$mcp.McpServer,code_ttl,config,token_ttl,knoxx.backend.routes.mcp.require_browser_auth_BANG_(policy_db,config),shadow.esm.esm_import$zod.z,runtime,knoxx.backend.routes.mcp.require_bearer_token_BANG_(base),shadow.esm.esm_import$node_crypto,knoxx.backend.routes.mcp.require_redis_BANG_(redis_client),base,shadow.esm.esm_import$$modelcontextprotocol$sdk$server$streamableHttp.StreamableHTTPServerTransport]);
knoxx.backend.routes.mcp.mcp_discovery_metadata_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_protected_resource_metadata_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_register_client_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_authorize_client_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_authorize_confirm_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_exchange_token_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_list_user_tokens_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_revoke_user_token_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_handle_post_BANG_(app,runtime,config,deps);

knoxx.backend.routes.mcp.mcp_handle_session_BANG_(app,runtime,config,deps);

return knoxx.backend.routes.mcp.mcp_handle_delete_session_BANG_(app,runtime,config,deps);
});

//# sourceMappingURL=knoxx.backend.routes.mcp.js.map
