import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_resume.js";
import "./knoxx.backend.auth.session.js";
import "./knoxx.backend.core.js";
import "./knoxx.backend.discord_gateway.js";
import "./knoxx.backend.discord_reaction_labels.js";
import "./knoxx.backend.graceful_shutdown.js";
import "./knoxx.backend.http_server.js";
import "./knoxx.backend.lifecycle.js";
import "./knoxx.backend.policy_db.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.routes.auth.js";
import "./knoxx.backend.routes.mcp.js";
import "./knoxx.backend.routes.tools.proxy.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.agent_turns.js";
goog.provide('knoxx.backend.bootstrap');
knoxx.backend.bootstrap.env = (function knoxx$backend$bootstrap$env(k,default$){
var or__5142__auto__ = (process.env[k]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
});
knoxx.backend.bootstrap.truthy_QMARK_ = (function knoxx$backend$bootstrap$truthy_QMARK_(v){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["yes",null,"true",null,"on",null,"y",null,"1",null], null), null),clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = v;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))));
});
knoxx.backend.bootstrap.notify_ready_BANG_ = (function knoxx$backend$bootstrap$notify_ready_BANG_(){
var send_fn = (process["send"]);
var connected_QMARK_ = (process["connected"]);
if(cljs.core.fn_QMARK_(send_fn)){
try{send_fn.call(process,"ready");

console.log((""+"[knoxx-bootstrap] sent pm2 ready signal"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(connected_QMARK_)?null:" (process.connected was false)"))));

return true;
}catch (e65976){var err = e65976;
console.warn("[knoxx-bootstrap] failed to send pm2 ready signal",err);

return false;
}} else {
console.log("[knoxx-bootstrap] process.send unavailable; skipping pm2 ready signal");

return false;

}
});
knoxx.backend.bootstrap.policy_options = (function knoxx$backend$bootstrap$policy_options(){
return ({"connectionString": (function (){var or__5142__auto__ = (process.env["KNOXX_POLICY_DATABASE_URL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (process.env["DATABASE_URL"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(), "primaryOrgSlug": knoxx.backend.bootstrap.env("KNOXX_PRIMARY_ORG_SLUG","open-hax"), "primaryOrgName": knoxx.backend.bootstrap.env("KNOXX_PRIMARY_ORG_NAME","Open Hax"), "primaryOrgKind": knoxx.backend.bootstrap.env("KNOXX_PRIMARY_ORG_KIND","platform_owner"), "bootstrapSystemAdminEmail": knoxx.backend.bootstrap.env("KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL","system-admin@open-hax.local"), "bootstrapSystemAdminName": knoxx.backend.bootstrap.env("KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME","Knoxx System Admin"), "bootstrapAllowlistEmails": knoxx.backend.bootstrap.env("KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS",""), "bootstrapAllowlistRoleSlugs": knoxx.backend.bootstrap.env("KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS","")});
});
/**
 * Create a fresh Fastify app and bind HTTP routes around durable runtime state.
 */
knoxx.backend.bootstrap.start_http_BANG_ = (function knoxx$backend$bootstrap$start_http_BANG_(runtime,cfg,policyDb,cookie_hook_QMARK_){
knoxx.backend.runtime.state.remember_context_BANG_(runtime,cfg,policyDb);

var app = knoxx.backend.http_server.create_app_BANG_();
knoxx.backend.http_server.ensure_json_empty_body_parser_BANG_(app);

knoxx.backend.http_server.add_hook_BANG_(app,"onRequest",(function (req,reply,done){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(req.url,"/api/dev/hmr")){
reply.header("x-knoxx-hmr-probe","hmr-probe-2026-05-07-e");

console.log("[knoxx-hot-reload-probe] hmr-probe-2026-05-07-e");
} else {
}

var temp__5825__auto___65978 = (req.headers["content-length"]);
if(cljs.core.truth_(temp__5825__auto___65978)){
var len_65979 = temp__5825__auto___65978;
if((parseInt(len_65979,(10)) > ((900) * (1024)))){
console.warn("[knoxx] large request",req.url,len_65979,"bytes");
} else {
}
} else {
}

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));

return knoxx.backend.http_server.register_default_plugins_BANG_(app).then((function (){
return app.register((function (instance,_opts,done){
knoxx.backend.core.register_ws_routes_BANG_(runtime,instance);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));
})).then((function (){
if(cljs.core.truth_(cookie_hook_QMARK_)){
return knoxx.backend.http_server.add_hook_BANG_(app,"onRequest",knoxx.backend.auth.session.create_session_hook(policyDb));
} else {
return null;
}
})).then((function (){
return knoxx.backend.routes.auth.register_auth_routes(app,({"policyDb": policyDb, "runtime": runtime}));
})).then((function (){
return knoxx.backend.core.register_app_routes_BANG_(runtime,app,cfg,knoxx.backend.agent_turns.lounge_messages_STAR_);
})).then((function (){
return knoxx.backend.routes.tools.proxy.register_proxy_routes_BANG_(app,cfg);
})).then((function (){
return knoxx.backend.routes.mcp.register_mcp_http_routes_BANG_(app,runtime,cfg);
})).then((function (){
return knoxx.backend.http_server.listen_BANG_(app,new cljs.core.Keyword(null,"host","host",-1558485167).cljs$core$IFn$_invoke$arity$1(cfg),new cljs.core.Keyword(null,"port","port",1534937262).cljs$core$IFn$_invoke$arity$1(cfg));
})).then((function (_){
knoxx.backend.lifecycle.remember_app_BANG_(app);

knoxx.backend.graceful_shutdown.install_BANG_(app,cfg);

knoxx.backend.bootstrap.notify_ready_BANG_();

var log = app.log;
log.info((""+"Knoxx backend CLJS listening on "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"host","host",-1558485167).cljs$core$IFn$_invoke$arity$1(cfg))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"port","port",1534937262).cljs$core$IFn$_invoke$arity$1(cfg))));

knoxx.backend.redis_client.init_redis_BANG_(new cljs.core.Keyword(null,"redis-url","redis-url",1921993939).cljs$core$IFn$_invoke$arity$1(cfg)).then((function (client){
if(cljs.core.truth_(client)){
log.info("Redis connected for session persistence");

knoxx.backend.agent_resume.resume_on_startup_BANG_(runtime,app,cfg);

return knoxx.backend.agent_resume.start_periodic_recovery_BANG_(runtime,app,cfg);
} else {
return null;
}
})).catch((function (err){
return log.warn("Redis initialization failed",err);
}));

return app;
}));
});
/**
 * Main entrypoint called by shadow-cljs.
 */
knoxx.backend.bootstrap.bootstrap_BANG_ = (function knoxx$backend$bootstrap$bootstrap_BANG_(){
var cfg = knoxx.backend.runtime.models.enrich_config(knoxx.backend.runtime.config.cfg());
var cookie_hook_QMARK_ = knoxx.backend.bootstrap.truthy_QMARK_((process.env["KNOXX_ENABLE_SESSION_HOOK"]));
knoxx.backend.discord_gateway.createDiscordGatewayManager(({"log": console}));

knoxx.backend.discord_reaction_labels.bind_BANG_(cfg);

return knoxx.backend.policy_db.create_policy_db(knoxx.backend.bootstrap.policy_options()).then((function (policyDb){
var runtime = ({});
knoxx.backend.lifecycle.remember_context_BANG_(runtime,cfg,policyDb,cookie_hook_QMARK_);

return knoxx.backend.bootstrap.start_http_BANG_(runtime,cfg,policyDb,cookie_hook_QMARK_);
})).catch((function (err){
console.error("Knoxx policy DB failed to initialize",err);

return process.exit((1));
}));
});
knoxx.backend.bootstrap.stop_http_before_load_BANG_ = (function knoxx$backend$bootstrap$stop_http_before_load_BANG_(done){
console.log("[knoxx-hot-reload] before-load: closing HTTP server");

return knoxx.backend.lifecycle.close_current_http_BANG_().then((function (_){
return console.log("[knoxx-hot-reload] before-load: HTTP server closed");
})).catch((function (err){
return console.error("[knoxx-hot-reload] failed to close HTTP server",err);
})).finally(done);
});
knoxx.backend.bootstrap.start_http_after_load_BANG_ = (function knoxx$backend$bootstrap$start_http_after_load_BANG_(done){
console.log("[knoxx-hot-reload] after-load: starting HTTP server");

var map__65977 = knoxx.backend.lifecycle.context();
var map__65977__$1 = cljs.core.__destructure_map(map__65977);
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65977__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var config = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65977__$1,new cljs.core.Keyword(null,"config","config",994861415));
var policyDb = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65977__$1,new cljs.core.Keyword(null,"policyDb","policyDb",1076383858));
var cookie_hook_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65977__$1,new cljs.core.Keyword(null,"cookie-hook?","cookie-hook?",1025238582));
if(cljs.core.truth_((function (){var and__5140__auto__ = runtime;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = config;
if(cljs.core.truth_(and__5140__auto____$1)){
return policyDb;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.bootstrap.start_http_BANG_(runtime,config,policyDb,cookie_hook_QMARK_).then((function (_){
return console.log("[knoxx-hot-reload] after-load: HTTP server started");
})).catch((function (err){
return console.error("[knoxx-hot-reload] failed to restart HTTP server",err);
})).finally(done);
} else {
console.warn("[knoxx-hot-reload] no lifecycle context; skipping HTTP restart");

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}
});

//# sourceMappingURL=knoxx.backend.bootstrap.js.map
