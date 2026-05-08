import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.agent_runtime.js";
import "./knoxx.backend.agent_turns.js";
import "./knoxx.backend.routes.app.js";
import "./knoxx.backend.routes.contracts.js";
import "./knoxx.backend.events.runtime.js";
import "./knoxx.backend.mcp_bridge.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.agent_resume.js";
import "./knoxx.backend.run_state.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.session_titles.js";
import "./shadow.esm.esm_import$fastify.js";
import "./shadow.esm.esm_import$$fastify$cors.js";
import "./shadow.esm.esm_import$$fastify$websocket.js";
import "./shadow.esm.esm_import$$fastify$multipart.js";
goog.provide('knoxx.backend.core');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.core !== 'undefined') && (typeof knoxx.backend.core.server_STAR_ !== 'undefined')){
} else {
knoxx.backend.core.server_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.core.app_log_info_BANG_ = (function knoxx$backend$core$app_log_info_BANG_(app,message){
var log = app.log;
return log.info(message);
});
knoxx.backend.core.app_log_error_BANG_ = (function knoxx$backend$core$app_log_error_BANG_(app,message,err){
var log = app.log;
if(cljs.core.truth_(err)){
return log.error(err,message);
} else {
return log.error(message);
}
});
knoxx.backend.core.app_listen_BANG_ = (function knoxx$backend$core$app_listen_BANG_(app,host,port){
return app.listen(({"host": host, "port": port}));
});
knoxx.backend.core.register_ws_routes_BANG_ = (function knoxx$backend$core$register_ws_routes_BANG_(runtime,app){
return knoxx.backend.realtime.register_ws_routes_BANG_(runtime,app,knoxx.backend.run_state.active_runs_count,knoxx.backend.agent_turns.lounge_messages_STAR_);
});
knoxx.backend.core.prewarm_sdk_runtime_BANG_ = (function knoxx$backend$core$prewarm_sdk_runtime_BANG_(runtime,app,resolved_config){
return knoxx.backend.agent_runtime.ensure_sdk_runtime_BANG_(runtime,resolved_config).then((function (_){
return knoxx.backend.core.app_log_info_BANG_(app,"Knoxx SDK runtime prewarmed");
})).catch((function (err){
knoxx.backend.core.app_log_error_BANG_(app,"Knoxx SDK runtime prewarm failed",err);

return Promise.reject(err);
}));
});
knoxx.backend.core.config_js = (function knoxx$backend$core$config_js(){
return cljs.core.clj__GT_js(knoxx.backend.runtime.models.enrich_config(knoxx.backend.runtime.config.cfg()));
});
knoxx.backend.core.initialize_mcp_gateway_BANG_ = (function knoxx$backend$core$initialize_mcp_gateway_BANG_(app,resolved_config){
if(cljs.core.not(new cljs.core.Keyword(null,"mcp-enabled","mcp-enabled",-2146653267).cljs$core$IFn$_invoke$arity$1(resolved_config))){
return Promise.resolve(null);
} else {
var existing_servers = knoxx.backend.mcp_bridge.parse_mcp_servers_env((function (){var or__5142__auto__ = (process.env["MCP_SERVERS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var openplanner_url = new cljs.core.Keyword(null,"openplanner-mcp-base-url","openplanner-mcp-base-url",1563571366).cljs$core$IFn$_invoke$arity$1(resolved_config);
var openplanner_name = new cljs.core.Keyword(null,"openplanner-mcp-tool-name","openplanner-mcp-tool-name",1459761280).cljs$core$IFn$_invoke$arity$2(resolved_config,"openplanner");
var shoedelussy_url = new cljs.core.Keyword(null,"shoedelussy-mcp-base-url","shoedelussy-mcp-base-url",1454013907).cljs$core$IFn$_invoke$arity$1(resolved_config);
var shoedelussy_name = new cljs.core.Keyword(null,"shoedelussy-mcp-tool-name","shoedelussy-mcp-tool-name",2046051346).cljs$core$IFn$_invoke$arity$2(resolved_config,"shoedelussy");
var shoedelussy_secret = new cljs.core.Keyword(null,"shoedelussy-mcp-shared-secret","shoedelussy-mcp-shared-secret",386556861).cljs$core$IFn$_invoke$arity$1(resolved_config);
var merged_servers = (function (){var G__71343 = existing_servers;
var G__71343__$1 = (((((!(cljs.core.contains_QMARK_(existing_servers,openplanner_name)))) && ((((!((openplanner_url == null)))) && (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2("",openplanner_url))))))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__71343,openplanner_name,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"url","url",276297046),openplanner_url,new cljs.core.Keyword(null,"transport","transport",-649001056),"http"], null)):G__71343);
if((((!(cljs.core.contains_QMARK_(existing_servers,shoedelussy_name)))) && ((((!((shoedelussy_url == null)))) && (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2("",shoedelussy_url)))))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__71343__$1,shoedelussy_name,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"url","url",276297046),shoedelussy_url,new cljs.core.Keyword(null,"transport","transport",-649001056),"http",new cljs.core.Keyword(null,"shared-secret","shared-secret",284397677),shoedelussy_secret], null));
} else {
return G__71343__$1;
}
})();
return knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"servers","servers",1881102005),merged_servers], null)).then((function (_){
return knoxx.backend.core.app_log_info_BANG_(app,(""+"MCP gateway initialized: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(knoxx.backend.mcp_bridge.catalog()))+" tools available"));
})).catch((function (err){
return knoxx.backend.core.app_log_error_BANG_(app,"MCP gateway initialization failed",err);
}));
}
});
knoxx.backend.core.start_background_services_BANG_ = (function knoxx$backend$core$start_background_services_BANG_(app,resolved_config){
return knoxx.backend.redis_client.init_redis_BANG_(new cljs.core.Keyword(null,"redis-url","redis-url",1921993939).cljs$core$IFn$_invoke$arity$1(resolved_config)).then((function (_){
return knoxx.backend.events.runtime.start_BANG_(resolved_config);
})).then((function (_){
return knoxx.backend.routes.contracts.start_contract_watcher_BANG_(resolved_config);
})).then((function (_){
return knoxx.backend.core.initialize_mcp_gateway_BANG_(app,resolved_config);
})).catch((function (err){
return knoxx.backend.core.app_log_error_BANG_(app,"Background startup services failed",err);
}));
});
knoxx.backend.core.register_app_routes_BANG_ = (function knoxx$backend$core$register_app_routes_BANG_(runtime,app,config,lounge_messages_STAR_){
var resolved_config = knoxx.backend.runtime.models.enrich_config(((cljs.core.map_QMARK_(config))?config:knoxx.backend.runtime.config.cfg()));
knoxx.backend.agent_hydration.ensure_settings_BANG_(resolved_config);

cljs.core.reset_BANG_(knoxx.backend.runtime.state.config_STAR_,resolved_config);

cljs.core.reset_BANG_(knoxx.backend.runtime.state.runtime_STAR_,runtime);

knoxx.backend.routes.app.register_routes_BANG_(runtime,app,resolved_config,lounge_messages_STAR_);

setTimeout((function (){
return knoxx.backend.core.prewarm_sdk_runtime_BANG_(runtime,app,resolved_config).catch((function (err){
knoxx.backend.core.app_log_error_BANG_(app,"Knoxx SDK runtime prewarm failed; startup continuing",err);

return null;
}));
}),(1000));

setTimeout((function (){
return knoxx.backend.core.start_background_services_BANG_(app,resolved_config);
}),(1500));

return Promise.resolve(cljs.core.clj__GT_js(resolved_config));
});
knoxx.backend.core.start_BANG_ = (function knoxx$backend$core$start_BANG_(runtime){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.core.server_STAR_))){
return null;
} else {
var config = knoxx.backend.runtime.models.enrich_config(knoxx.backend.runtime.config.cfg());
var app = shadow.esm.esm_import$fastify.default(({"logger": ({"stream": process.stderr})}));
cljs.core.reset_BANG_(knoxx.backend.runtime.state.config_STAR_,config);

cljs.core.reset_BANG_(knoxx.backend.runtime.state.runtime_STAR_,runtime);

knoxx.backend.agent_hydration.ensure_settings_BANG_(config);

return Promise.resolve(null).then((function (){
return knoxx.backend.session_titles.load_session_titles_BANG_(runtime,config);
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$cors.default,({"origin": true}));
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$multipart.default);
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$websocket.default);
})).then((function (){
return app.register((function (instance,_opts,done){
knoxx.backend.core.register_ws_routes_BANG_(runtime,instance);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));
})).then((function (){
knoxx.backend.routes.app.register_routes_BANG_(runtime,app,config,knoxx.backend.agent_turns.lounge_messages_STAR_);

return knoxx.backend.redis_client.init_redis_BANG_(new cljs.core.Keyword(null,"redis-url","redis-url",1921993939).cljs$core$IFn$_invoke$arity$1(config)).then((function (redis_client){
if(cljs.core.truth_(redis_client)){
knoxx.backend.core.app_log_info_BANG_(app,"Redis client initialized");

knoxx.backend.routes.contracts.sync_contract_index_BANG_(config);
} else {
}

return null;
})).catch((function (err){
knoxx.backend.core.app_log_error_BANG_(app,"Failed to initialize Redis",err);

return null;
})).then((function (_){
knoxx.backend.events.runtime.start_BANG_(config);

knoxx.backend.routes.contracts.start_contract_watcher_BANG_(config);

return knoxx.backend.core.app_listen_BANG_(app,new cljs.core.Keyword(null,"host","host",-1558485167).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"port","port",1534937262).cljs$core$IFn$_invoke$arity$1(config));
}));
})).then((function (_){
cljs.core.reset_BANG_(knoxx.backend.core.server_STAR_,app);

knoxx.backend.core.app_log_info_BANG_(app,(""+"Knoxx backend CLJS listening on "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"host","host",-1558485167).cljs$core$IFn$_invoke$arity$1(config))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"port","port",1534937262).cljs$core$IFn$_invoke$arity$1(config))));

return knoxx.backend.agent_resume.resume_on_startup_BANG_(runtime,app,config).catch((function (err){
return knoxx.backend.core.app_log_error_BANG_(app,"agent-resume failed",err);
}));
})).catch((function (err){
console.error("Knoxx backend CLJS failed to start",err);

return process.exit((1));
}));
}
});

//# sourceMappingURL=knoxx.backend.core.js.map
