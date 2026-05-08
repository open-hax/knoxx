import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.redis_client.js";
goog.provide('knoxx.backend.agents.policy');
knoxx.backend.agents.policy.chat_policy_constraints = (function knoxx$backend$agents$policy$chat_policy_constraints(auth_context){
var constraints = knoxx.backend.authz.ctx_tool_constraints(auth_context,"agent.chat");
if(cljs.core.map_QMARK_(constraints)){
return constraints;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = constraints;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("object",goog.typeOf(constraints));
} else {
return and__5140__auto__;
}
})())){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(constraints,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
return cljs.core.PersistentArrayMap.EMPTY;

}
}
});
knoxx.backend.agents.policy.positive_int = (function knoxx$backend$agents$policy$positive_int(value){
if(typeof value === 'number'){
var n = (value | 0);
if((n > (0))){
return n;
} else {
return null;
}
} else {
if(typeof value === 'string'){
var parsed = parseInt(value,(10));
if(((cljs.core.not(isNaN(parsed))) && ((parsed > (0))))){
return parsed;
} else {
return null;
}
} else {
return null;

}
}
});
knoxx.backend.agents.policy.allowed_models = (function knoxx$backend$agents$policy$allowed_models(constraints){
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
return cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__58773_SHARP_){
if(typeof p1__58773_SHARP_ === 'string'){
var t = clojure.string.trim(p1__58773_SHARP_);
if(clojure.string.blank_QMARK_(t)){
return null;
} else {
return t;
}
} else {
return null;
}
}),((cljs.core.sequential_QMARK_(raw))?raw:(cljs.core.truth_(cljs.core.array_QMARK_(raw))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(raw):cljs.core.PersistentVector.EMPTY
))));
});
knoxx.backend.agents.policy.chat_rate_limit_principal = (function knoxx$backend$agents$policy$chat_rate_limit_principal(auth_context){
var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(auth_context,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"membership","membership",254556333),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"membershipId","membershipId",2026001076).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(auth_context,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(auth_context);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(auth_context,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"email","email",1415816706)], null));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(auth_context);
}
}
}
}
}
});
knoxx.backend.agents.policy.rate_limit_error = (function knoxx$backend$agents$policy$rate_limit_error(max_requests,window_seconds){
var G__58803 = (new Error((""+"Chat rate limit exceeded: more than "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(max_requests)+" requests in "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(window_seconds)+" seconds")));
(G__58803["statusCode"] = (429));

(G__58803["code"] = "chat_rate_limited");

return G__58803;
});
knoxx.backend.agents.policy.model_policy_error = (function knoxx$backend$agents$policy$model_policy_error(model_id,allowed){
var G__58812 = (new Error((""+"Model '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(model_id)+"' is not allowed for this account. Allowed models: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",cljs.core.sort.cljs$core$IFn$_invoke$arity$1(allowed))))));
(G__58812["statusCode"] = (403));

(G__58812["code"] = "model_not_allowed");

return G__58812;
});
knoxx.backend.agents.policy.enforce_chat_policy_BANG_ = (function knoxx$backend$agents$policy$enforce_chat_policy_BANG_(auth_context,model_id){
var constraints = knoxx.backend.agents.policy.chat_policy_constraints(auth_context);
var permitted_models = knoxx.backend.agents.policy.allowed_models(constraints);
var max_requests = knoxx.backend.agents.policy.positive_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"maxRequests","maxRequests",967196018).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"max-requests","max-requests",229153272).cljs$core$IFn$_invoke$arity$1(constraints);
}
})());
var window_seconds = knoxx.backend.agents.policy.positive_int((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"windowSeconds","windowSeconds",-573461410).cljs$core$IFn$_invoke$arity$1(constraints);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"window-seconds","window-seconds",1922216946).cljs$core$IFn$_invoke$arity$1(constraints);
}
})());
var principal = (function (){var G__58829 = knoxx.backend.agents.policy.chat_rate_limit_principal(auth_context);
var G__58829__$1 = (((G__58829 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58829)));
if((G__58829__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__58829__$1);
}
})();
var redis_client = knoxx.backend.redis_client.get_client();
if(((cljs.core.seq(permitted_models)) && ((!(cljs.core.contains_QMARK_(permitted_models,model_id)))))){
return Promise.reject(knoxx.backend.agents.policy.model_policy_error(model_id,permitted_models));
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = principal;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = redis_client;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = max_requests;
if(cljs.core.truth_(and__5140__auto____$2)){
return window_seconds;
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var key = (""+"knoxx:chat-rate:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(principal)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(window_seconds));
return redis_client.incr(key).then((function (count){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(count,(1))){
return redis_client.expire(key,window_seconds).then((function (_){
return count;
}));
} else {
return Promise.resolve(count);
}
})).then((function (count){
if((count > max_requests)){
return Promise.reject(knoxx.backend.agents.policy.rate_limit_error(max_requests,window_seconds));
} else {
return Promise.resolve(null);
}
})).catch((function (err){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((err["code"]),"chat_rate_limited")){
return Promise.reject(err);
} else {
return Promise.resolve(null);
}
}));
} else {
return Promise.resolve(null);

}
}
});
knoxx.backend.agents.policy.validate_chat_policy_BANG_ = (function knoxx$backend$agents$policy$validate_chat_policy_BANG_(auth_context,model_id){
return knoxx.backend.agents.policy.enforce_chat_policy_BANG_(auth_context,model_id);
});

//# sourceMappingURL=knoxx.backend.agents.policy.js.map
