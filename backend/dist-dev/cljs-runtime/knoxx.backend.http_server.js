import "./cljs_env.js";
import "./cljs.core.js";
import "./shadow.esm.esm_import$fastify.js";
import "./shadow.esm.esm_import$$fastify$cors.js";
import "./shadow.esm.esm_import$$fastify$websocket.js";
import "./shadow.esm.esm_import$$fastify$multipart.js";
import "./shadow.esm.esm_import$$fastify$cookie.js";
import "./shadow.esm.esm_import$$fastify$formbody.js";
goog.provide('knoxx.backend.http_server');
knoxx.backend.http_server.create_app_BANG_ = (function knoxx$backend$http_server$create_app_BANG_(){
return shadow.esm.esm_import$fastify.default(({"logger": true, "bodyLimit": (((50) * (1024)) * (1024)), "requestTimeout": (600000), "connectionTimeout": (600000), "forceCloseConnections": true}));
});
/**
 * Allow Content-Type: application/json with empty bodies.
 * 
 * Fastify's default parser throws FST_ERR_CTP_EMPTY_JSON_BODY, but some
 * endpoints are intentionally POST-without-body.
 */
knoxx.backend.http_server.ensure_json_empty_body_parser_BANG_ = (function knoxx$backend$http_server$ensure_json_empty_body_parser_BANG_(app){
return app.addContentTypeParser("application/json",({"parseAs": "string"}),(function (_req,body,done){
try{var G__524817 = null;
var G__524818 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(body,""))?({}):JSON.parse(body));
return (done.cljs$core$IFn$_invoke$arity$2 ? done.cljs$core$IFn$_invoke$arity$2(G__524817,G__524818) : done.call(null,G__524817,G__524818));
}catch (e524816){var err = e524816;
return (done.cljs$core$IFn$_invoke$arity$1 ? done.cljs$core$IFn$_invoke$arity$1(err) : done.call(null,err));
}}));
});
knoxx.backend.http_server.add_hook_BANG_ = (function knoxx$backend$http_server$add_hook_BANG_(app,hook_name,handler){
return app.addHook(hook_name,handler);
});
knoxx.backend.http_server.register_default_plugins_BANG_ = (function knoxx$backend$http_server$register_default_plugins_BANG_(app){
return app.register(shadow.esm.esm_import$$fastify$cors.default,({"origin": true})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$cookie.default);
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$formbody.default);
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$multipart.default,({"limits": ({"fileSize": (((50) * (1024)) * (1024)), "fieldSize": (((1) * (1024)) * (1024)), "files": (10)})}));
})).then((function (){
return app.register(shadow.esm.esm_import$$fastify$websocket.default);
}));
});
knoxx.backend.http_server.listen_BANG_ = (function knoxx$backend$http_server$listen_BANG_(app,host,port){
return app.listen(({"host": host, "port": port}));
});
knoxx.backend.http_server.close_BANG_ = (function knoxx$backend$http_server$close_BANG_(app){
try{var result = app.close();
if((!((result == null)))){
return result;
} else {
return Promise.resolve(true);
}
}catch (e524829){var err = e524829;
return Promise.reject(err);
}});

//# sourceMappingURL=knoxx.backend.http_server.js.map
