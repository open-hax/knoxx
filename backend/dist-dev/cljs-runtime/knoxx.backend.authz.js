import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.auth.session.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.runtime.state.js";
goog.provide('knoxx.backend.authz');
knoxx.backend.authz.policy_db = (function knoxx$backend$authz$policy_db(_runtime){
return knoxx.backend.runtime.state.current_policy_db();
});
knoxx.backend.authz.policy_db_enabled_QMARK_ = (function knoxx$backend$authz$policy_db_enabled_QMARK_(runtime){
return (!((knoxx.backend.authz.policy_db(runtime) == null)));
});
/**
 * Fastify treats a resolved promise value as a response payload.
 * 
 * CLJS `nil` becomes JS `null`, which makes Fastify try to send a second
 * response after we've already used reply.send/reply.code in async handlers.
 * Normalize `nil` to `undefined` so promise-returning handlers can safely do
 * side-effectful replies and resolve to 'no payload'.
 */
knoxx.backend.authz.fastify_handler_result = (function knoxx$backend$authz$fastify_handler_result(value){
if((value instanceof Promise)){
return value.then((function (resolved){
if((resolved == null)){
return undefined;
} else {
return resolved;
}
}));
} else {
if((value == null)){
return undefined;
} else {
return value;

}
}
});
knoxx.backend.authz.policy_db_promise = (function knoxx$backend$authz$policy_db_promise(runtime,reply,status,promise){
if((!(knoxx.backend.authz.policy_db_enabled_QMARK_(runtime)))){
return knoxx.backend.authz.fastify_handler_result(knoxx.backend.http.json_response_BANG_(reply,(503),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null)));
} else {
return promise.then((function (result){
knoxx.backend.http.json_response_BANG_(reply,status,cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(result,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));

return undefined;
})).catch((function (err){
knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err);

return undefined;
}));
}
});
knoxx.backend.authz.resolve_request_context_BANG_ = (function knoxx$backend$authz$resolve_request_context_BANG_(runtime,request){
if((!(knoxx.backend.authz.policy_db_enabled_QMARK_(runtime)))){
return Promise.resolve(null);
} else {
var temp__5823__auto__ = (request["__knoxxRequestContext"]);
if(cljs.core.truth_(temp__5823__auto__)){
var cached = temp__5823__auto__;
return Promise.resolve(cached);
} else {
var headers = (function (){var or__5142__auto__ = (request["headers"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var header_email = clojure.string.trim((function (){var or__5142__auto__ = (headers["x-knoxx-user-email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var header_mid = clojure.string.trim((function (){var or__5142__auto__ = (headers["x-knoxx-membership-id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var ctx_promise = (((((!(clojure.string.blank_QMARK_(header_email)))) || ((!(clojure.string.blank_QMARK_(header_mid))))))?knoxx.backend.authz.policy_db(runtime).resolveRequestContext(headers):knoxx.backend.auth.session.resolve_auth_context(request,knoxx.backend.authz.policy_db(runtime)));
return ctx_promise.then((function (ctx){
var clj_ctx = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(ctx,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
(request["__knoxxRequestContext"] = clj_ctx);

return clj_ctx;
}));
}
}
});
knoxx.backend.authz.with_request_context_BANG_ = (function knoxx$backend$authz$with_request_context_BANG_(runtime,request,reply,f){
if((!(knoxx.backend.authz.policy_db_enabled_QMARK_(runtime)))){
return knoxx.backend.authz.fastify_handler_result((f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(null) : f.call(null,null)));
} else {
return knoxx.backend.authz.resolve_request_context_BANG_(runtime,request).then((function (ctx){
return knoxx.backend.authz.fastify_handler_result((f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(ctx) : f.call(null,ctx)));
})).catch((function (err){
knoxx.backend.http.error_response_BANG_.cljs$core$IFn$_invoke$arity$2(reply,err);

return undefined;
}));
}
});
knoxx.backend.authz.ctx_org_id = (function knoxx$backend$authz$ctx_org_id(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"org","org",1495985),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["orgId"]);
} else {
return null;
}
}
}
});
knoxx.backend.authz.ctx_org_slug = (function knoxx$backend$authz$ctx_org_slug(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"org","org",1495985),new cljs.core.Keyword(null,"slug","slug",2029314850)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["orgSlug"]);
} else {
return null;
}
}
}
});
knoxx.backend.authz.ctx_user_id = (function knoxx$backend$authz$ctx_user_id(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["userId"]);
} else {
return null;
}
}
}
});
knoxx.backend.authz.ctx_user_email = (function knoxx$backend$authz$ctx_user_email(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"email","email",1415816706)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["userEmail"]);
} else {
return null;
}
}
}
});
knoxx.backend.authz.ctx_membership_id = (function knoxx$backend$authz$ctx_membership_id(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"membershipId","membershipId",2026001076).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"membership","membership",254556333),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["membershipId"]);
} else {
return null;
}
}
}
});
knoxx.backend.authz.ctx_actor_id = (function knoxx$backend$authz$ctx_actor_id(ctx){
var or__5142__auto__ = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(ctx,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.Keyword(null,"id","id",-1388402092)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
if(cljs.core.truth_(ctx)){
return (ctx["actorId"]);
} else {
return null;
}
}
}
}
}
});
knoxx.backend.authz.ctx_role_slugs = (function knoxx$backend$authz$ctx_role_slugs(ctx){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentHashSet.EMPTY,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.keep.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"slug","slug",2029314850),new cljs.core.Keyword(null,"roles","roles",143379530).cljs$core$IFn$_invoke$arity$1(ctx));
}
})());
});
knoxx.backend.authz.primary_context_role = (function knoxx$backend$authz$primary_context_role(ctx){
var or__5142__auto__ = cljs.core.first(new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__58758 = new cljs.core.Keyword(null,"roles","roles",143379530).cljs$core$IFn$_invoke$arity$1(ctx);
var G__58758__$1 = (((G__58758 == null))?null:cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"slug","slug",2029314850),G__58758));
if((G__58758__$1 == null)){
return null;
} else {
return cljs.core.first(G__58758__$1);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "knowledge_worker";
}
}
});
knoxx.backend.authz.system_admin_QMARK_ = (function knoxx$backend$authz$system_admin_QMARK_(ctx){
return cljs.core.contains_QMARK_(knoxx.backend.authz.ctx_role_slugs(ctx),"system_admin");
});
knoxx.backend.authz.ctx_permissions = (function knoxx$backend$authz$ctx_permissions(ctx){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentHashSet.EMPTY,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"permissions","permissions",67803075).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
});
knoxx.backend.authz.ctx_permitted_QMARK_ = (function knoxx$backend$authz$ctx_permitted_QMARK_(ctx,permission){
return cljs.core.contains_QMARK_(knoxx.backend.authz.ctx_permissions(ctx),permission);
});
knoxx.backend.authz.ctx_any_permission_QMARK_ = (function knoxx$backend$authz$ctx_any_permission_QMARK_(ctx,permissions){
return cljs.core.boolean$(cljs.core.some((function (p1__58759_SHARP_){
return knoxx.backend.authz.ctx_permitted_QMARK_(ctx,p1__58759_SHARP_);
}),permissions));
});
knoxx.backend.authz.ctx_tool_effect = (function knoxx$backend$authz$ctx_tool_effect(ctx,tool_id){
return cljs.core.some((function (policy){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)))){
return new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy);
} else {
return null;
}
}),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx));
});
knoxx.backend.authz.ctx_tool_policy = (function knoxx$backend$authz$ctx_tool_policy(ctx,tool_id){
return cljs.core.some((function (policy){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)))){
return policy;
} else {
return null;
}
}),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx));
});
knoxx.backend.authz.ctx_tool_constraints = (function knoxx$backend$authz$ctx_tool_constraints(ctx,tool_id){
var or__5142__auto__ = new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_tool_policy(ctx,tool_id));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.authz.ctx_tool_allowed_QMARK_ = (function knoxx$backend$authz$ctx_tool_allowed_QMARK_(ctx,tool_id){
return ((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("allow",knoxx.backend.authz.ctx_tool_effect(ctx,tool_id))));
});
knoxx.backend.authz.ensure_permission_BANG_ = (function knoxx$backend$authz$ensure_permission_BANG_(ctx,permission){
if(((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (knoxx.backend.authz.ctx_permitted_QMARK_(ctx,permission)))){
} else {
throw knoxx.backend.http.http_error((403),"permission_denied",(""+"Permission '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(permission)+"' is required"));
}

return ctx;
});
/**
 * Enforce tool access for request-scoped endpoints.
 * 
 * Prefer this over ensure-permission! for endpoints that gate on tool ids
 * like `multimodal.upload`.
 * 
 * NOTE: system_admin bypasses tool policy checks.
 */
knoxx.backend.authz.ensure_tool_BANG_ = (function knoxx$backend$authz$ensure_tool_BANG_(ctx,tool_id){
if(((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(ctx,tool_id)))){
} else {
throw knoxx.backend.http.http_error((403),"tool_denied",(""+"Tool '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)+"' is required"));
}

return ctx;
});
knoxx.backend.authz.ensure_any_permission_BANG_ = (function knoxx$backend$authz$ensure_any_permission_BANG_(ctx,permissions,code,message){
if(((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (knoxx.backend.authz.ctx_any_permission_QMARK_(ctx,permissions)))){
} else {
throw knoxx.backend.http.http_error((403),code,message);
}

return ctx;
});
knoxx.backend.authz.ensure_org_scope_BANG_ = (function knoxx$backend$authz$ensure_org_scope_BANG_(ctx,org_id,permission){
knoxx.backend.authz.ensure_permission_BANG_(ctx,permission);

if(((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_org_id(ctx))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))))){
} else {
throw knoxx.backend.http.http_error((403),"org_scope_denied","Requested org is outside the current Knoxx scope");
}

return ctx;
});
knoxx.backend.authz.record_org_id = (function knoxx$backend$authz$record_org_id(record){
var or__5142__auto__ = new cljs.core.Keyword(null,"org_id","org_id",1380185385).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(record);
}
});
knoxx.backend.authz.record_user_id = (function knoxx$backend$authz$record_user_id(record){
var or__5142__auto__ = new cljs.core.Keyword(null,"user_id","user_id",993497112).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(record);
}
});
knoxx.backend.authz.record_user_email = (function knoxx$backend$authz$record_user_email(record){
var or__5142__auto__ = new cljs.core.Keyword(null,"user_email","user_email",-926613652).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"userEmail","userEmail",-1838879618).cljs$core$IFn$_invoke$arity$1(record);
}
});
knoxx.backend.authz.record_membership_id = (function knoxx$backend$authz$record_membership_id(record){
var or__5142__auto__ = new cljs.core.Keyword(null,"membership_id","membership_id",-171302674).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"membershipId","membershipId",2026001076).cljs$core$IFn$_invoke$arity$1(record);
}
});
knoxx.backend.authz.record_actor_id = (function knoxx$backend$authz$record_actor_id(record){
var or__5142__auto__ = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(record);
}
}
});
knoxx.backend.authz.principal_match_QMARK_ = (function knoxx$backend$authz$principal_match_QMARK_(ctx,record){
var ctx_membership = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.ctx_membership_id(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var record_membership = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.record_membership_id(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var ctx_user = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.ctx_user_id(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var record_user = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.record_user_id(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var ctx_email = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.ctx_user_email(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var record_email = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.record_user_email(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var ctx_actor = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.ctx_actor_id(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var record_actor = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.authz.record_actor_id(record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var actor_bound_QMARK_ = (!(clojure.string.blank_QMARK_(record_actor)));
var actor_match_QMARK_ = (((!(actor_bound_QMARK_))) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ctx_actor,record_actor)));
var user_bound_QMARK_ = (((!(clojure.string.blank_QMARK_(record_membership)))) || ((((!(clojure.string.blank_QMARK_(record_user)))) || ((!(clojure.string.blank_QMARK_(record_email)))))));
if((!(actor_match_QMARK_))){
return false;
} else {
if(knoxx.backend.authz.system_admin_QMARK_(ctx)){
return true;
} else {
if((((!(clojure.string.blank_QMARK_(ctx_membership)))) && ((!(clojure.string.blank_QMARK_(record_membership)))))){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ctx_membership,record_membership);
} else {
if((((!(clojure.string.blank_QMARK_(ctx_user)))) && ((!(clojure.string.blank_QMARK_(record_user)))))){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ctx_user,record_user);
} else {
if((((!(clojure.string.blank_QMARK_(ctx_email)))) && ((!(clojure.string.blank_QMARK_(record_email)))))){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ctx_email,record_email);
} else {
return ((actor_bound_QMARK_) && ((((!(user_bound_QMARK_))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(ctx_actor,record_actor)))));

}
}
}
}
}
});
knoxx.backend.authz.auth_snapshot = (function knoxx$backend$authz$auth_snapshot(ctx){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"role_slugs","role_slugs",2101192325),new cljs.core.Keyword(null,"org_slug","org_slug",-322631770),new cljs.core.Keyword(null,"org_id","org_id",1380185385),new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"user_email","user_email",-926613652),new cljs.core.Keyword(null,"membership_id","membership_id",-171302674),new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177),new cljs.core.Keyword(null,"user_id","user_id",993497112),new cljs.core.Keyword(null,"is_system_admin","is_system_admin",-723489128),new cljs.core.Keyword(null,"membership_tool_policies","membership_tool_policies",2116037883)],[cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"permissions","permissions",67803075).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),knoxx.backend.authz.ctx_org_slug(ctx),knoxx.backend.authz.ctx_org_id(ctx),knoxx.backend.authz.ctx_actor_id(ctx),knoxx.backend.authz.ctx_user_email(ctx),knoxx.backend.authz.ctx_membership_id(ctx),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),knoxx.backend.authz.ctx_user_id(ctx),cljs.core.boolean$(new cljs.core.Keyword(null,"isSystemAdmin","isSystemAdmin",679314438).cljs$core$IFn$_invoke$arity$1(ctx)),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"membershipToolPolicies","membershipToolPolicies",-954353456).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())]);
});
knoxx.backend.authz.auth_snapshot_has_principal_QMARK_ = (function knoxx$backend$authz$auth_snapshot_has_principal_QMARK_(snapshot){
return cljs.core.boolean$((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"org_id","org_id",1380185385).cljs$core$IFn$_invoke$arity$1(snapshot);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"user_id","user_id",993497112).cljs$core$IFn$_invoke$arity$1(snapshot);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"user_email","user_email",-926613652).cljs$core$IFn$_invoke$arity$1(snapshot);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"membership_id","membership_id",-171302674).cljs$core$IFn$_invoke$arity$1(snapshot);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(snapshot);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"is_system_admin","is_system_admin",-723489128).cljs$core$IFn$_invoke$arity$1(snapshot);
}
}
}
}
}
})());
});
knoxx.backend.authz.ensure_conversation_access_BANG_ = (function knoxx$backend$authz$ensure_conversation_access_BANG_(conversation_access_STAR_,ctx,conversation_id){
if(cljs.core.truth_((function (){var and__5140__auto__ = ctx;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)))));
} else {
return and__5140__auto__;
}
})())){
var temp__5825__auto___58870 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(conversation_access_STAR_),conversation_id);
if(cljs.core.truth_(temp__5825__auto___58870)){
var existing_58877 = temp__5825__auto___58870;
if(knoxx.backend.authz.principal_match_QMARK_(ctx,existing_58877)){
} else {
throw knoxx.backend.http.http_error((403),"conversation_scope_denied","Conversation belongs to another Knoxx user");
}
} else {
}
} else {
}

return ctx;
});
knoxx.backend.authz.remember_conversation_access_BANG_ = (function knoxx$backend$authz$remember_conversation_access_BANG_(conversation_access_STAR_,ctx,conversation_id){
if(cljs.core.truth_((function (){var and__5140__auto__ = ctx;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conversation_id)))));
} else {
return and__5140__auto__;
}
})())){
var snapshot = knoxx.backend.authz.auth_snapshot(ctx);
if(knoxx.backend.authz.auth_snapshot_has_principal_QMARK_(snapshot)){
knoxx.backend.authz.ensure_conversation_access_BANG_(conversation_access_STAR_,ctx,conversation_id);

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(conversation_access_STAR_,cljs.core.assoc,conversation_id,snapshot);
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.authz.run_visible_QMARK_ = (function knoxx$backend$authz$run_visible_QMARK_(ctx,run){
if((ctx == null)){
return true;
} else {
if(knoxx.backend.authz.system_admin_QMARK_(ctx)){
return true;
} else {
if(knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.runs.read_all")){
return true;
} else {
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_org_id(ctx))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.record_org_id(run))))) && (knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.runs.read_org")))){
return true;
} else {
if(((knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.runs.read_own")) && (knoxx.backend.authz.principal_match_QMARK_(ctx,run)))){
return true;
} else {
return false;

}
}
}
}
}
});

//# sourceMappingURL=knoxx.backend.authz.js.map
