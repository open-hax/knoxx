import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.app_shapes.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.util.time.js";
import "./shadow.cljs.modern.js";
goog.provide('knoxx.backend.routes.models');
knoxx.backend.routes.models.proxx_configured_QMARK_ = (function knoxx$backend$routes$models$proxx_configured_QMARK_(config){
return (((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))))));
});
/**
 * Best-effort extraction of a stable Knoxx session key from request headers.
 * 
 * Knoxx frontend always sends x-knoxx-session-id. We map that to Proxx's
 * prompt_cache_key for provider+account session affinity.
 */
knoxx.backend.routes.models.request_session_key = (function knoxx$backend$routes$models$request_session_key(request){
var headers = (function (){var or__5142__auto__ = (request["headers"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var v = (function (){var or__5142__auto__ = (headers["x-knoxx-session-id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (headers["x-session-id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (headers["x-open-hax-session-id"]);
}
}
})();
if(typeof v === 'string'){
var trimmed = clojure.string.trim(v);
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
 * Mutates payload in-place, adding prompt_cache_key when missing.
 * 
 * Proxx uses prompt_cache_key as the affinity key in routing + analytics.
 */
knoxx.backend.routes.models.ensure_prompt_cache_key_BANG_ = (function knoxx$backend$routes$models$ensure_prompt_cache_key_BANG_(request,payload){
var session_key_69797 = knoxx.backend.routes.models.request_session_key(request);
var existing_69798 = (function (){var or__5142__auto__ = (payload["prompt_cache_key"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["promptCacheKey"]);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = session_key_69797;
if(cljs.core.truth_(and__5140__auto__)){
return (((existing_69798 == null)) || (clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(existing_69798)))));
} else {
return and__5140__auto__;
}
})())){
(payload["prompt_cache_key"] = session_key_69797);
} else {
}

return payload;
});
knoxx.backend.routes.models.model_policy_allowed_ids = (function knoxx$backend$routes$models$model_policy_allowed_ids(ctx){
var constraints = knoxx.backend.authz.ctx_tool_constraints(ctx,"agent.chat");
var raw = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"allowedModels","allowedModels",-660080636).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"allowed-models","allowed-models",2019027926).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"models","models",-1985455662).cljs$core$IFn$_invoke$arity$1(constraints);
}
}
})();
return cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (value){
if(typeof value === 'string'){
var trimmed = clojure.string.trim(value);
if(clojure.string.blank_QMARK_(trimmed)){
return null;
} else {
return trimmed;
}
} else {
return null;
}
}),((cljs.core.sequential_QMARK_(raw))?raw:(cljs.core.truth_(cljs.core.array_QMARK_(raw))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw):cljs.core.PersistentVector.EMPTY
))));
});
knoxx.backend.routes.models.filter_model_items_for_ctx = (function knoxx$backend$routes$models$filter_model_items_for_ctx(ctx,items,config){
var allowed_by_policy = knoxx.backend.routes.models.model_policy_allowed_ids(ctx);
return cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (item){
var model_id = (item["id"]);
return ((knoxx.backend.runtime.models.allowlisted_model_id_QMARK_(config,model_id)) && (((cljs.core.empty_QMARK_(allowed_by_policy)) || (cljs.core.contains_QMARK_(allowed_by_policy,model_id)))));
}),items);
});
knoxx.backend.routes.models.proxx_health_ctx = (function knoxx$backend$routes$models$proxx_health_ctx(config,request,reply){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"request","request",1772954723),request,new cljs.core.Keyword(null,"reply","reply",1144328671),reply,new cljs.core.Keyword(null,"configured","configured",-884777889),knoxx.backend.routes.models.proxx_configured_QMARK_(config),new cljs.core.Keyword(null,"default-model","default-model",-1201018527),new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_))], null);
});
knoxx.backend.routes.models.send_proxx_health_unconfigured_BANG_ = (function knoxx$backend$routes$models$send_proxx_health_unconfigured_BANG_(p__69626){
var map__69627 = p__69626;
var map__69627__$1 = cljs.core.__destructure_map(map__69627);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69627__$1,new cljs.core.Keyword(null,"config","config",994861415));
var reply = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69627__$1,new cljs.core.Keyword(null,"reply","reply",1144328671));
var default_model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69627__$1,new cljs.core.Keyword(null,"default-model","default-model",-1201018527));
knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"reachable","reachable",-1495191549),false,new cljs.core.Keyword(null,"configured","configured",-884777889),false,new cljs.core.Keyword(null,"base_url","base_url",-1764155256),new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(503),new cljs.core.Keyword(null,"default_model","default_model",516528790),default_model], null));

return reply;
});
knoxx.backend.routes.models.fetch_proxx_health = (function knoxx$backend$routes$models$fetch_proxx_health(p__69631){
var map__69632 = p__69631;
var map__69632__$1 = cljs.core.__destructure_map(map__69632);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69632__$1,new cljs.core.Keyword(null,"config","config",994861415));
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/health"),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))}));
});
knoxx.backend.routes.models.send_proxx_health_success_BANG_ = (function knoxx$backend$routes$models$send_proxx_health_success_BANG_(p__69640,resp){
var map__69641 = p__69640;
var map__69641__$1 = cljs.core.__destructure_map(map__69641);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69641__$1,new cljs.core.Keyword(null,"config","config",994861415));
var reply = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69641__$1,new cljs.core.Keyword(null,"reply","reply",1144328671));
var default_model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69641__$1,new cljs.core.Keyword(null,"default-model","default-model",-1201018527));
var body = (resp["body"]);
var key_pool = (body["keyPool"]);
knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"reachable","reachable",-1495191549),cljs.core.boolean$((resp["ok"])),new cljs.core.Keyword(null,"configured","configured",-884777889),true,new cljs.core.Keyword(null,"base_url","base_url",-1764155256),new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(resp["status"]),new cljs.core.Keyword(null,"model_count","model_count",-986279688),((typeof (body["modelCount"]) === 'number')?(body["modelCount"]):((typeof (key_pool["totalKeys"]) === 'number')?(key_pool["totalKeys"]):null
)),new cljs.core.Keyword(null,"default_model","default_model",516528790),default_model], null));

return reply;
});
knoxx.backend.routes.models.send_proxx_health_failure_BANG_ = (function knoxx$backend$routes$models$send_proxx_health_failure_BANG_(p__69652){
var map__69653 = p__69652;
var map__69653__$1 = cljs.core.__destructure_map(map__69653);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69653__$1,new cljs.core.Keyword(null,"config","config",994861415));
var reply = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69653__$1,new cljs.core.Keyword(null,"reply","reply",1144328671));
var default_model = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69653__$1,new cljs.core.Keyword(null,"default-model","default-model",-1201018527));
knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"reachable","reachable",-1495191549),false,new cljs.core.Keyword(null,"configured","configured",-884777889),true,new cljs.core.Keyword(null,"base_url","base_url",-1764155256),new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"status_code","status_code",-572644263),(502),new cljs.core.Keyword(null,"default_model","default_model",516528790),default_model], null));

return reply;
});
knoxx.backend.routes.models.send_proxx_health_BANG_ = (function knoxx$backend$routes$models$send_proxx_health_BANG_(ctx){
if(cljs.core.not(new cljs.core.Keyword(null,"configured","configured",-884777889).cljs$core$IFn$_invoke$arity$1(ctx))){
return knoxx.backend.routes.models.send_proxx_health_unconfigured_BANG_(ctx);
} else {
return knoxx.backend.routes.models.fetch_proxx_health(ctx).then((function (resp){
return knoxx.backend.routes.models.send_proxx_health_success_BANG_(ctx,resp);
})).catch((function (_err){
return knoxx.backend.routes.models.send_proxx_health_failure_BANG_(ctx);
}));
}
});
knoxx.backend.routes.models.proxx_models_ctx = (function knoxx$backend$routes$models$proxx_models_ctx(config,request,reply,auth_ctx){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"request","request",1772954723),request,new cljs.core.Keyword(null,"reply","reply",1144328671),reply,new cljs.core.Keyword(null,"auth","auth",1389754926),auth_ctx,new cljs.core.Keyword(null,"fetch-json!","fetch-json!",868607614),knoxx.backend.http.fetch_json], null);
});
knoxx.backend.routes.models.fetch_proxx_models = (function knoxx$backend$routes$models$fetch_proxx_models(p__69668){
var map__69669 = p__69668;
var map__69669__$1 = cljs.core.__destructure_map(map__69669);
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69669__$1,new cljs.core.Keyword(null,"config","config",994861415));
var fetch_json_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69669__$1,new cljs.core.Keyword(null,"fetch-json!","fetch-json!",868607614));
var G__69670 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/models");
var G__69671 = ({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))});
return (fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_json_BANG_.cljs$core$IFn$_invoke$arity$2(G__69670,G__69671) : fetch_json_BANG_.call(null,G__69670,G__69671));
});
knoxx.backend.routes.models.send_proxx_models_success_BANG_ = (function knoxx$backend$routes$models$send_proxx_models_success_BANG_(p__69675,resp){
var map__69676 = p__69675;
var map__69676__$1 = cljs.core.__destructure_map(map__69676);
var auth = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69676__$1,new cljs.core.Keyword(null,"auth","auth",1389754926));
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69676__$1,new cljs.core.Keyword(null,"config","config",994861415));
var reply = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69676__$1,new cljs.core.Keyword(null,"reply","reply",1144328671));
if(cljs.core.truth_((resp["ok"]))){
var items_69801 = knoxx.backend.http.js_array_seq((function (){var or__5142__auto__ = ((resp["body"])["data"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
var filtered_69802 = cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(knoxx.backend.routes.models.filter_model_items_for_ctx(auth,items_69801,config));
knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"models","models",-1985455662),filtered_69802], null));
} else {
knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx model list failed",new cljs.core.Keyword(null,"details","details",1956795411),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
}

return reply;
});
knoxx.backend.routes.models.send_proxx_models_failure_BANG_ = (function knoxx$backend$routes$models$send_proxx_models_failure_BANG_(p__69684,err){
var map__69685 = p__69684;
var map__69685__$1 = cljs.core.__destructure_map(map__69685);
var reply = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__69685__$1,new cljs.core.Keyword(null,"reply","reply",1144328671));
knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));

return reply;
});
knoxx.backend.routes.models.send_proxx_models_BANG_ = (function knoxx$backend$routes$models$send_proxx_models_BANG_(ctx){
return knoxx.backend.routes.models.fetch_proxx_models(ctx).then((function (resp){
return knoxx.backend.routes.models.send_proxx_models_success_BANG_(ctx,resp);
})).catch((function (err){
return knoxx.backend.routes.models.send_proxx_models_failure_BANG_(ctx,err);
}));
});
knoxx.backend.routes.models.register_model_routes_BANG_ = (function knoxx$backend$routes$models$register_model_routes_BANG_(app,runtime,config){
knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/proxx/health",(function (request,reply){
return knoxx.backend.routes.models.send_proxx_health_BANG_(knoxx.backend.routes.models.proxx_health_ctx(config,request,reply));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/proxx/observability/request-logs",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
try{knoxx.backend.authz.ensure_permission_BANG_(ctx,"org.proxx.observability.read");

if((!(knoxx.backend.routes.models.proxx_configured_QMARK_(config)))){
return knoxx.backend.http.json_response_BANG_(reply,(503),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx is not configured"], null));
} else {
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/api/v1/request-logs"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.request_query_string(request))),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx request logs failed",new cljs.core.Keyword(null,"details","details",1956795411),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
}
})).catch((function (err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));
}
}catch (e69751){var err = e69751;
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err);
}}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/proxx/observability/dashboard/overview",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
try{knoxx.backend.authz.ensure_permission_BANG_(ctx,"org.proxx.observability.read");

if((!(knoxx.backend.routes.models.proxx_configured_QMARK_(config)))){
return knoxx.backend.http.json_response_BANG_(reply,(503),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx is not configured"], null));
} else {
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/api/v1/dashboard/overview"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.request_query_string(request))),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx dashboard overview failed",new cljs.core.Keyword(null,"details","details",1956795411),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
}
})).catch((function (err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));
}
}catch (e69760){var err = e69760;
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err);
}}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/proxx/observability/analytics/provider-model",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
try{knoxx.backend.authz.ensure_permission_BANG_(ctx,"org.proxx.observability.read");

if((!(knoxx.backend.routes.models.proxx_configured_QMARK_(config)))){
return knoxx.backend.http.json_response_BANG_(reply,(503),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx is not configured"], null));
} else {
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/api/v1/analytics/provider-model"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.http.request_query_string(request))),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx provider-model analytics failed",new cljs.core.Keyword(null,"details","details",1956795411),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
}
})).catch((function (err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));
}
}catch (e69761){var err = e69761;
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err);
}}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/proxx/models",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
if(cljs.core.truth_(ctx)){
knoxx.backend.authz.ensure_permission_BANG_(ctx,"agent.chat.use");
} else {
}

return knoxx.backend.routes.models.send_proxx_models_BANG_(knoxx.backend.routes.models.proxx_models_ctx(config,request,reply,ctx));
}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/api/proxx/chat",(function (request,reply){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var payload = knoxx.backend.routes.models.ensure_prompt_cache_key_BANG_(request,({"model": (function (){var or__5142__auto__ = (body["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"llmModel","llmModel",-1399114982).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
}
})(), "messages": (function (){var or__5142__auto__ = (body["messages"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(), "temperature": (body["temperature"]), "top_p": (body["top_p"]), "max_tokens": (body["max_tokens"]), "stop": (body["stop"]), "stream": false}));
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/chat/completions"),({"method": "POST", "headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)), "body": JSON.stringify(payload)})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
var data = (resp["body"]);
var choices = (function (){var or__5142__auto__ = (data["choices"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var first_choice = (choices[(0)]);
var message = (function (){var or__5142__auto__ = (first_choice["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var content = (function (){var or__5142__auto__ = (message["content"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (first_choice["text"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"answer","answer",-742633163),content,new cljs.core.Keyword(null,"model","model",331153215),(function (){var or__5142__auto__ = (data["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["model"]);
}
})(),new cljs.core.Keyword(null,"rag_context","rag_context",-368956766),null], null));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"error","error",-978969032),"Proxx chat failed",new cljs.core.Keyword(null,"details","details",1956795411),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null));
}
})).catch((function (err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/models",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
if(cljs.core.truth_(ctx)){
knoxx.backend.authz.ensure_permission_BANG_(ctx,"agent.chat.use");
} else {
}

return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/models"),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
var items = knoxx.backend.http.js_array_seq((function (){var or__5142__auto__ = ((resp["body"])["data"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
var models = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (item){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (item["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"name","name",1843675177),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (item["id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),new cljs.core.Keyword(null,"path","path",-188191168),"",new cljs.core.Keyword(null,"size_bytes","size_bytes",2073761055),(0),new cljs.core.Keyword(null,"modified_at","modified_at",-1569634080),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"hash16mb","hash16mb",-86345561),"",new cljs.core.Keyword(null,"suggested_ctx","suggested_ctx",-2040144573),(128000)], null);
}),knoxx.backend.routes.models.filter_model_items_for_ctx(ctx,items,config));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"models","models",-1985455662),models], null));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Model list failed"], null));
}
})).catch((function (err){
return knoxx.backend.http.json_response_BANG_(reply,(502),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null));
}));
}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/runs",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
var limit_raw = (request["query"]["limit"]);
var limit = ((typeof limit_raw === 'string')?parseInt(limit_raw,(10)):(100));
var items = cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.run_state.summarize_run,cljs.core.take.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = limit;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (100);
}
})()),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__69736_SHARP_){
return knoxx.backend.authz.run_visible_QMARK_(ctx,p1__69736_SHARP_);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__69735_SHARP_){
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),p1__69735_SHARP_);
}),cljs.core.deref(knoxx.backend.run_state.run_order_STAR_)))))));
return knoxx.backend.http.json_response_BANG_(reply,(200),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"runs","runs",-1553997798),items], null));
}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/api/runs/:runId",(function (request,reply){
return knoxx.backend.authz.with_request_context_BANG_(runtime,request,reply,(function (ctx){
var run_id = (request["params"]["runId"]);
var run = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.run_state.runs_STAR_),run_id);
if((run == null)){
return knoxx.backend.http.json_response_BANG_(reply,(404),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Run not found"], null));
} else {
if((!(knoxx.backend.authz.run_visible_QMARK_(ctx,run)))){
return knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,knoxx.backend.http.http_error((403),"run_scope_denied","Run is outside the current Knoxx scope"));
} else {
return knoxx.backend.http.json_response_BANG_(reply,(200),run);

}
}
}));
}));

knoxx.backend.app_shapes.route_BANG_(app,"GET","/v1/models",(function (request,reply){
if(knoxx.backend.http.require_openai_key_BANG_(config,request,reply)){
return knoxx.backend.http.fetch_json((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/models"),({"headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config))})).then((function (resp){
if(cljs.core.truth_((resp["ok"]))){
return knoxx.backend.http.json_response_BANG_(reply,(200),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((resp["body"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
} else {
return knoxx.backend.http.openai_auth_error(reply,(502),"Upstream model list failed","upstream_error");
}
})).catch((function (err){
return knoxx.backend.http.openai_auth_error(reply,(502),(""+"Upstream model list failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),"upstream_error");
}));
} else {
return null;
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/v1/chat/completions",(function (request,reply){
if(knoxx.backend.http.require_openai_key_BANG_(config,request,reply)){
var payload = knoxx.backend.routes.models.ensure_prompt_cache_key_BANG_(request,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/chat/completions"),({"method": "POST", "headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)), "body": JSON.stringify(payload)})).then((function (resp){
return knoxx.backend.http.send_fetch_response_BANG_(reply,resp);
})).catch((function (err){
return knoxx.backend.http.openai_auth_error(reply,(502),(""+"Upstream chat request failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),"upstream_error");
}));
} else {
return null;
}
}));

knoxx.backend.app_shapes.route_BANG_(app,"POST","/v1/embeddings",(function (request,reply){
if(knoxx.backend.http.require_openai_key_BANG_(config,request,reply)){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var payload = knoxx.backend.routes.models.ensure_prompt_cache_key_BANG_(request,(function (){var G__69796 = Object.assign(({}),body);
(G__69796["model"] = (function (){var or__5142__auto__ = (body["model"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"embedModel","embedModel",1987630417).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.agent_hydration.settings_state_STAR_));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"proxx-embed-model","proxx-embed-model",-289269914).cljs$core$IFn$_invoke$arity$1(config);
}
}
})());

return G__69796;
})());
return fetch((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"proxx-base-url","proxx-base-url",-322145978).cljs$core$IFn$_invoke$arity$1(config))+"/v1/embeddings"),({"method": "POST", "headers": knoxx.backend.http.bearer_headers(new cljs.core.Keyword(null,"proxx-auth-token","proxx-auth-token",-621179676).cljs$core$IFn$_invoke$arity$1(config)), "body": JSON.stringify(payload)})).then((function (resp){
return knoxx.backend.http.send_fetch_response_BANG_(reply,resp);
})).catch((function (err){
return knoxx.backend.http.openai_auth_error(reply,(502),(""+"Embedding generation failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err)),"upstream_error");
}));
} else {
return null;
}
}));

return null;
});

//# sourceMappingURL=knoxx.backend.routes.models.js.map
