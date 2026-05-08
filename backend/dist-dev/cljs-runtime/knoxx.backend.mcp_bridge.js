import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.mcp_bridge');
knoxx.backend.mcp_bridge.PROTOCOL_VERSION = "2024-11-05";
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.mcp_bridge !== 'undefined') && (typeof knoxx.backend.mcp_bridge.servers_STAR_ !== 'undefined')){
} else {
knoxx.backend.mcp_bridge.servers_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.mcp_bridge !== 'undefined') && (typeof knoxx.backend.mcp_bridge.request_counter_STAR_ !== 'undefined')){
} else {
knoxx.backend.mcp_bridge.request_counter_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1((0));
}
/**
 * Parse MCP_SERVERS env: "id:url:transport,id:command:args:transport"
 */
knoxx.backend.mcp_bridge.parse_mcp_servers_env = (function knoxx$backend$mcp_bridge$parse_mcp_servers_env(env_value){
if(clojure.string.blank_QMARK_(env_value)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
var entries = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2(env_value,/,/)));
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (entry){
var parts = clojure.string.split.cljs$core$IFn$_invoke$arity$2(entry,/:/);
if((cljs.core.count(parts) >= (3))){
var id = cljs.core.first(parts);
var rest_parts = cljs.core.rest(parts);
var transport = cljs.core.last(rest_parts);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(transport,"http")){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [id,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"url","url",276297046),clojure.string.join.cljs$core$IFn$_invoke$arity$2(":",cljs.core.butlast(rest_parts)),new cljs.core.Keyword(null,"transport","transport",-649001056),"http"], null)], null);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(transport,"stdio")){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"command","command",-894540724),cljs.core.first(rest_parts),new cljs.core.Keyword(null,"args","args",1315556576),cljs.core.vec(cljs.core.rest(cljs.core.butlast(rest_parts))),new cljs.core.Keyword(null,"transport","transport",-649001056),"stdio"], null)], null);
} else {
return null;

}
}
} else {
return null;
}
})),entries);
}
});
knoxx.backend.mcp_bridge.get_mcp_servers_from_env = (function knoxx$backend$mcp_bridge$get_mcp_servers_from_env(){
return knoxx.backend.mcp_bridge.parse_mcp_servers_env((function (){var or__5142__auto__ = (process.env["MCP_SERVERS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
});
knoxx.backend.mcp_bridge.parse_sse_response = (function knoxx$backend$mcp_bridge$parse_sse_response(text,expected_id){
var lines = clojure.string.split.cljs$core$IFn$_invoke$arity$2(text,/\n/);
var data_line = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (_,line){
if(clojure.string.starts_with_QMARK_(clojure.string.trim(line),"data:")){
return cljs.core.reduced(clojure.string.trim(cljs.core.subs.cljs$core$IFn$_invoke$arity$2(clojure.string.trim(line),(5))));
} else {
return null;
}
}),null,lines);
if(cljs.core.truth_(data_line)){
var result = JSON.parse(data_line);
if(cljs.core.truth_((result["error"]))){
throw (new Error((""+"MCP error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((result["error"])["message"])))));
} else {
return (result["result"]);
}
} else {
try{var result = JSON.parse(text);
if(cljs.core.truth_((result["error"]))){
throw (new Error((""+"MCP error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((result["error"])["message"])))));
} else {
return (result["result"]);
}
}catch (e517834){var _ = e517834;
throw (new Error((""+"Failed to parse MCP response: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text,(0),(200))))));
}}
});
knoxx.backend.mcp_bridge.create_http_client = (function knoxx$backend$mcp_bridge$create_http_client(config){
var base_url = clojure.string.replace((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),/\/$/,"");
return (function (method,params){
var id = cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.mcp_bridge.request_counter_STAR_,cljs.core.inc);
var body = JSON.stringify(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"jsonrpc","jsonrpc",1483657187),"2.0",new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"method","method",55703592),method,new cljs.core.Keyword(null,"params","params",710516235),(function (){var or__5142__auto__ = params;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], null)));
var headers = (function (){var G__517841 = (new Object());
(G__517841["Content-Type"] = "application/json");

(G__517841["Accept"] = "application/json, text/event-stream");

return G__517841;
})();
if(cljs.core.truth_(new cljs.core.Keyword(null,"shared-secret","shared-secret",284397677).cljs$core$IFn$_invoke$arity$1(config))){
(headers["Authorization"] = (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"shared-secret","shared-secret",284397677).cljs$core$IFn$_invoke$arity$1(config))));
} else {
}

return fetch(base_url,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),Object.entries(headers),new cljs.core.Keyword(null,"body","body",-2049205669),body], null))).then((function (response){
if(cljs.core.not(response.ok)){
throw (new Error((""+"MCP HTTP error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(response.status)+" "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(response.statusText))));
} else {
return response.text().then((function (text){
return knoxx.backend.mcp_bridge.parse_sse_response(text,id);
}));
}
}));
});
});
knoxx.backend.mcp_bridge.initialize_http_server_BANG_ = (function knoxx$backend$mcp_bridge$initialize_http_server_BANG_(server){
var client_fn = new cljs.core.Keyword(null,"client","client",-1323448117).cljs$core$IFn$_invoke$arity$1(server);
return (function (){var G__517847 = "initialize";
var G__517848 = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"protocolVersion","protocolVersion",370363372),knoxx.backend.mcp_bridge.PROTOCOL_VERSION,new cljs.core.Keyword(null,"capabilities","capabilities",212739361),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"clientInfo","clientInfo",-802887449),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1843675177),"knoxx",new cljs.core.Keyword(null,"version","version",425292698),"1.0.0"], null)], null));
return (client_fn.cljs$core$IFn$_invoke$arity$2 ? client_fn.cljs$core$IFn$_invoke$arity$2(G__517847,G__517848) : client_fn.call(null,G__517847,G__517848));
})().then((function (init_result){
console.log("[mcp-gateway]",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(server),"initialized:",(function (){var or__5142__auto__ = (init_result["serverInfo"]["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown";
}
})());

return (client_fn.cljs$core$IFn$_invoke$arity$2 ? client_fn.cljs$core$IFn$_invoke$arity$2("tools/list",null) : client_fn.call(null,"tools/list",null));
})).then((function (tools_result){
var js_tools = (function (){var or__5142__auto__ = (tools_result["tools"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var tools = cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$initialize_http_server_BANG__$_iter__517852(s__517853){
return (new cljs.core.LazySeq(null,(function (){
var s__517853__$1 = s__517853;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__517853__$1);
if(temp__5825__auto__){
var s__517853__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__517853__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__517853__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__517855 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__517854 = (0);
while(true){
if((i__517854 < size__5627__auto__)){
var i = cljs.core._nth(c__5626__auto__,i__517854);
cljs.core.chunk_append(b__517855,(function (){var tool = (js_tools[i]);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = (tool["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = (tool["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"input-schema","input-schema",-266884346),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (tool["inputSchema"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
})());

var G__518157 = (i__517854 + (1));
i__517854 = G__518157;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__517855),knoxx$backend$mcp_bridge$initialize_http_server_BANG__$_iter__517852(cljs.core.chunk_rest(s__517853__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__517855),null);
}
} else {
var i = cljs.core.first(s__517853__$2);
return cljs.core.cons((function (){var tool = (js_tools[i]);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = (tool["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = (tool["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"input-schema","input-schema",-266884346),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (tool["inputSchema"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
})(),knoxx$backend$mcp_bridge$initialize_http_server_BANG__$_iter__517852(cljs.core.rest(s__517853__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.range.cljs$core$IFn$_invoke$arity$1(js_tools.length));
})());
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.mcp_bridge.servers_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(server),new cljs.core.Keyword(null,"tools","tools",-1241731990)], null),tools);

return console.log("[mcp-gateway] Connected to",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(server),", found",cljs.core.count(tools),"tools");
}));
});
knoxx.backend.mcp_bridge.connect_server_BANG_ = (function knoxx$backend$mcp_bridge$connect_server_BANG_(id,config){
console.log("[mcp-gateway] Connecting to",id,"(",new cljs.core.Keyword(null,"transport","transport",-649001056).cljs$core$IFn$_invoke$arity$1(config),")...");

if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"transport","transport",-649001056).cljs$core$IFn$_invoke$arity$1(config),"http")){
var client_fn = knoxx.backend.mcp_bridge.create_http_client(config);
var server = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"tools","tools",-1241731990),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"connected","connected",-169833045),true,new cljs.core.Keyword(null,"client","client",-1323448117),client_fn], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.mcp_bridge.servers_STAR_,cljs.core.assoc,id,server);

return knoxx.backend.mcp_bridge.initialize_http_server_BANG_(server);
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"transport","transport",-649001056).cljs$core$IFn$_invoke$arity$1(config),"stdio")){
console.log("[mcp-gateway] stdio transport not yet implemented");

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.mcp_bridge.servers_STAR_,cljs.core.assoc,id,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"tools","tools",-1241731990),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"connected","connected",-169833045),false,new cljs.core.Keyword(null,"client","client",-1323448117),null], null));
} else {
return Promise.reject((new Error((""+"Unknown transport: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"transport","transport",-649001056).cljs$core$IFn$_invoke$arity$1(config))))));

}
}
});
/**
 * Returns true if any MCP servers are connected.
 */
knoxx.backend.mcp_bridge.available_QMARK_ = (function knoxx$backend$mcp_bridge$available_QMARK_(){
return (!((cljs.core.seq(cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_)) == null)));
});
/**
 * Initialize the MCP gateway with configured servers.
 * Returns a Promise that resolves when all servers are connected.
 */
knoxx.backend.mcp_bridge.initialize_BANG_ = (function knoxx$backend$mcp_bridge$initialize_BANG_(var_args){
var G__517916 = arguments.length;
switch (G__517916) {
case 0:
return knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$0 = (function (){
return knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}));

(knoxx.backend.mcp_bridge.initialize_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (config){
var server_configs = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"servers","servers",1881102005).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.mcp_bridge.get_mcp_servers_from_env();
}
})();
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$iter__517924(s__517925){
return (new cljs.core.LazySeq(null,(function (){
var s__517925__$1 = s__517925;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__517925__$1);
if(temp__5825__auto__){
var s__517925__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__517925__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__517925__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__517927 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__517926 = (0);
while(true){
if((i__517926 < size__5627__auto__)){
var vec__517930 = cljs.core._nth(c__5626__auto__,i__517926);
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517930,(0),null);
var server_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517930,(1),null);
cljs.core.chunk_append(b__517927,Promise.resolve(knoxx.backend.mcp_bridge.connect_server_BANG_(id,server_config)).catch(((function (i__517926,vec__517930,id,server_config,c__5626__auto__,size__5627__auto__,b__517927,s__517925__$2,temp__5825__auto__,server_configs){
return (function (err){
return console.error("[mcp-gateway] Failed to connect to",id,":",(err["message"]));
});})(i__517926,vec__517930,id,server_config,c__5626__auto__,size__5627__auto__,b__517927,s__517925__$2,temp__5825__auto__,server_configs))
));

var G__518170 = (i__517926 + (1));
i__517926 = G__518170;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__517927),knoxx$backend$mcp_bridge$iter__517924(cljs.core.chunk_rest(s__517925__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__517927),null);
}
} else {
var vec__517935 = cljs.core.first(s__517925__$2);
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517935,(0),null);
var server_config = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517935,(1),null);
return cljs.core.cons(Promise.resolve(knoxx.backend.mcp_bridge.connect_server_BANG_(id,server_config)).catch(((function (vec__517935,id,server_config,s__517925__$2,temp__5825__auto__,server_configs){
return (function (err){
return console.error("[mcp-gateway] Failed to connect to",id,":",(err["message"]));
});})(vec__517935,id,server_config,s__517925__$2,temp__5825__auto__,server_configs))
),knoxx$backend$mcp_bridge$iter__517924(cljs.core.rest(s__517925__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(server_configs);
})())).then((function (){
return cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_);
}));
}));

(knoxx.backend.mcp_bridge.initialize_BANG_.cljs$lang$maxFixedArity = 1);

/**
 * Check if MCP is enabled and has connected servers.
 */
knoxx.backend.mcp_bridge.enabled_QMARK_ = (function knoxx$backend$mcp_bridge$enabled_QMARK_(){
return ((cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((process.env["MCP_ENABLED"]),"false")) && ((!((cljs.core.seq(cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_)) == null)))));
});
/**
 * Get MCP gateway status as a CLJS map.
 */
knoxx.backend.mcp_bridge.status = (function knoxx$backend$mcp_bridge$status(){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"enabled","enabled",1195909756),knoxx.backend.mcp_bridge.enabled_QMARK_(),new cljs.core.Keyword(null,"servers","servers",1881102005),(function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$status_$_iter__517952(s__517953){
return (new cljs.core.LazySeq(null,(function (){
var s__517953__$1 = s__517953;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__517953__$1);
if(temp__5825__auto__){
var s__517953__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__517953__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__517953__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__517955 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__517954 = (0);
while(true){
if((i__517954 < size__5627__auto__)){
var vec__517960 = cljs.core._nth(c__5626__auto__,i__517954);
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517960,(0),null);
var server = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517960,(1),null);
cljs.core.chunk_append(b__517955,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"transport","transport",-649001056),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(server,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"transport","transport",-649001056)], null)),new cljs.core.Keyword(null,"connected","connected",-169833045),new cljs.core.Keyword(null,"connected","connected",-169833045).cljs$core$IFn$_invoke$arity$1(server),new cljs.core.Keyword(null,"tool-count","tool-count",600749873),cljs.core.count(new cljs.core.Keyword(null,"tools","tools",-1241731990).cljs$core$IFn$_invoke$arity$1(server)),new cljs.core.Keyword(null,"tools","tools",-1241731990),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"tools","tools",-1241731990).cljs$core$IFn$_invoke$arity$1(server))], null));

var G__518192 = (i__517954 + (1));
i__517954 = G__518192;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__517955),knoxx$backend$mcp_bridge$status_$_iter__517952(cljs.core.chunk_rest(s__517953__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__517955),null);
}
} else {
var vec__517964 = cljs.core.first(s__517953__$2);
var id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517964,(0),null);
var server = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517964,(1),null);
return cljs.core.cons(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),id,new cljs.core.Keyword(null,"transport","transport",-649001056),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(server,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"transport","transport",-649001056)], null)),new cljs.core.Keyword(null,"connected","connected",-169833045),new cljs.core.Keyword(null,"connected","connected",-169833045).cljs$core$IFn$_invoke$arity$1(server),new cljs.core.Keyword(null,"tool-count","tool-count",600749873),cljs.core.count(new cljs.core.Keyword(null,"tools","tools",-1241731990).cljs$core$IFn$_invoke$arity$1(server)),new cljs.core.Keyword(null,"tools","tools",-1241731990),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"tools","tools",-1241731990).cljs$core$IFn$_invoke$arity$1(server))], null),knoxx$backend$mcp_bridge$status_$_iter__517952(cljs.core.rest(s__517953__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_));
})()], null);
});
/**
 * Get the MCP tool catalog as a vector of tool maps.
 */
knoxx.backend.mcp_bridge.catalog = (function knoxx$backend$mcp_bridge$catalog(){
return cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$catalog_$_iter__517977(s__517978){
return (new cljs.core.LazySeq(null,(function (){
var s__517978__$1 = s__517978;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__517978__$1);
if(temp__5825__auto__){
var xs__6385__auto__ = temp__5825__auto__;
var vec__517987 = cljs.core.first(xs__6385__auto__);
var server_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517987,(0),null);
var server = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517987,(1),null);
var iterys__5624__auto__ = ((function (s__517978__$1,vec__517987,server_id,server,xs__6385__auto__,temp__5825__auto__){
return (function knoxx$backend$mcp_bridge$catalog_$_iter__517977_$_iter__517979(s__517980){
return (new cljs.core.LazySeq(null,((function (s__517978__$1,vec__517987,server_id,server,xs__6385__auto__,temp__5825__auto__){
return (function (){
var s__517980__$1 = s__517980;
while(true){
var temp__5825__auto____$1 = cljs.core.seq(s__517980__$1);
if(temp__5825__auto____$1){
var s__517980__$2 = temp__5825__auto____$1;
if(cljs.core.chunked_seq_QMARK_(s__517980__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__517980__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__517982 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__517981 = (0);
while(true){
if((i__517981 < size__5627__auto__)){
var tool = cljs.core._nth(c__5626__auto__,i__517981);
cljs.core.chunk_append(b__517982,cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(tool,new cljs.core.Keyword(null,"id","id",-1388402092),(""+"mcp."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tool))),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"serverId","serverId",-264111159),server_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(""+"mcp."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tool)))], 0)));

var G__518223 = (i__517981 + (1));
i__517981 = G__518223;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__517982),knoxx$backend$mcp_bridge$catalog_$_iter__517977_$_iter__517979(cljs.core.chunk_rest(s__517980__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__517982),null);
}
} else {
var tool = cljs.core.first(s__517980__$2);
return cljs.core.cons(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(tool,new cljs.core.Keyword(null,"id","id",-1388402092),(""+"mcp."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tool))),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"serverId","serverId",-264111159),server_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(""+"mcp."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tool)))], 0)),knoxx$backend$mcp_bridge$catalog_$_iter__517977_$_iter__517979(cljs.core.rest(s__517980__$2)));
}
} else {
return null;
}
break;
}
});})(s__517978__$1,vec__517987,server_id,server,xs__6385__auto__,temp__5825__auto__))
,null,null));
});})(s__517978__$1,vec__517987,server_id,server,xs__6385__auto__,temp__5825__auto__))
;
var fs__5625__auto__ = cljs.core.seq(iterys__5624__auto__(new cljs.core.Keyword(null,"tools","tools",-1241731990).cljs$core$IFn$_invoke$arity$1(server)));
if(fs__5625__auto__){
return cljs.core.concat.cljs$core$IFn$_invoke$arity$2(fs__5625__auto__,knoxx$backend$mcp_bridge$catalog_$_iter__517977(cljs.core.rest(s__517978__$1)));
} else {
var G__518226 = cljs.core.rest(s__517978__$1);
s__517978__$1 = G__518226;
continue;
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_));
})());
});
/**
 * Get all MCP tools as a map keyed by tool ID.
 */
knoxx.backend.mcp_bridge.tools_map = (function knoxx$backend$mcp_bridge$tools_map(){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,(function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$tools_map_$_iter__517998(s__517999){
return (new cljs.core.LazySeq(null,(function (){
var s__517999__$1 = s__517999;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__517999__$1);
if(temp__5825__auto__){
var s__517999__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__517999__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__517999__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__518001 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__518000 = (0);
while(true){
if((i__518000 < size__5627__auto__)){
var tool = cljs.core._nth(c__5626__auto__,i__518000);
cljs.core.chunk_append(b__518001,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(tool)),tool], null));

var G__518230 = (i__518000 + (1));
i__518000 = G__518230;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__518001),knoxx$backend$mcp_bridge$tools_map_$_iter__517998(cljs.core.chunk_rest(s__517999__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__518001),null);
}
} else {
var tool = cljs.core.first(s__517999__$2);
return cljs.core.cons(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(tool)),tool], null),knoxx$backend$mcp_bridge$tools_map_$_iter__517998(cljs.core.rest(s__517999__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(knoxx.backend.mcp_bridge.catalog());
})());
});
knoxx.backend.mcp_bridge.format_mcp_result = (function knoxx$backend$mcp_bridge$format_mcp_result(result){
if(cljs.core.not(result)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content","content",15833224),"",new cljs.core.Keyword(null,"isError","isError",-1727958473),false], null);
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = (result["content"]);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.array_QMARK_((result["content"]));
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content","content",15833224),clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.keep.cljs$core$IFn$_invoke$arity$2(cljs.core.identity,(function (){var iter__5628__auto__ = (function knoxx$backend$mcp_bridge$format_mcp_result_$_iter__518013(s__518014){
return (new cljs.core.LazySeq(null,(function (){
var s__518014__$1 = s__518014;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__518014__$1);
if(temp__5825__auto__){
var s__518014__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__518014__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__518014__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__518016 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__518015 = (0);
while(true){
if((i__518015 < size__5627__auto__)){
var i = cljs.core._nth(c__5626__auto__,i__518015);
cljs.core.chunk_append(b__518016,(function (){var block = ((result["content"])[i]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((block["type"]),"text")){
return (block["text"]);
} else {
return null;
}
})());

var G__518235 = (i__518015 + (1));
i__518015 = G__518235;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__518016),knoxx$backend$mcp_bridge$format_mcp_result_$_iter__518013(cljs.core.chunk_rest(s__518014__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__518016),null);
}
} else {
var i = cljs.core.first(s__518014__$2);
return cljs.core.cons((function (){var block = ((result["content"])[i]);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((block["type"]),"text")){
return (block["text"]);
} else {
return null;
}
})(),knoxx$backend$mcp_bridge$format_mcp_result_$_iter__518013(cljs.core.rest(s__518014__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.range.cljs$core$IFn$_invoke$arity$1((result["content"]).length));
})())),new cljs.core.Keyword(null,"isError","isError",-1727958473),cljs.core.boolean$((result["isError"]))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"content","content",15833224),JSON.stringify(result,null,(2)),new cljs.core.Keyword(null,"isError","isError",-1727958473),false], null);
}
}
});
/**
 * Call an MCP tool by its full ID (e.g. "mcp.grep.searchGitHub").
 * Returns a Promise that resolves with {:content "..." :isError bool}.
 */
knoxx.backend.mcp_bridge.call_tool_BANG_ = (function knoxx$backend$mcp_bridge$call_tool_BANG_(tool_id,args){
var match = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)).match(/^mcp\.([^.]+)\.(.+)$/);
if(cljs.core.truth_(match)){
} else {
throw (new Error((""+"Invalid MCP tool ID: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id))));
}

var server_id = (match[(1)]);
var tool_name = (match[(2)]);
var server = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.mcp_bridge.servers_STAR_),server_id);
if(cljs.core.truth_(server)){
} else {
throw (new Error((""+"MCP server not found: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id))));
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"connected","connected",-169833045).cljs$core$IFn$_invoke$arity$1(server))){
} else {
throw (new Error((""+"MCP server not connected: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(server_id))));
}

console.log("[mcp-gateway] Calling",server_id,".",tool_name,"with args:",cljs.core.subs.cljs$core$IFn$_invoke$arity$3(JSON.stringify(cljs.core.clj__GT_js((function (){var or__5142__auto__ = args;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})())),(0),(200)));

if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(server,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"transport","transport",-649001056)], null)),"http")){
var client_fn = new cljs.core.Keyword(null,"client","client",-1323448117).cljs$core$IFn$_invoke$arity$1(server);
return (function (){var G__518057 = "tools/call";
var G__518058 = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"name","name",1843675177),tool_name,new cljs.core.Keyword(null,"arguments","arguments",-1182834456),(function (){var or__5142__auto__ = args;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], null));
return (client_fn.cljs$core$IFn$_invoke$arity$2 ? client_fn.cljs$core$IFn$_invoke$arity$2(G__518057,G__518058) : client_fn.call(null,G__518057,G__518058));
})().then(knoxx.backend.mcp_bridge.format_mcp_result);
} else {
throw (new Error((""+"Transport not supported: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(server,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"transport","transport",-649001056)], null))))));
}
});
/**
 * Return MCP tools formatted as agent SDK custom tools (JavaScript array).
 */
knoxx.backend.mcp_bridge.mcp_tools_for_agent = (function knoxx$backend$mcp_bridge$mcp_tools_for_agent(){
if(knoxx.backend.mcp_bridge.enabled_QMARK_()){
var tools = knoxx.backend.mcp_bridge.catalog();
return cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool){
var tool_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(tool);
var input_schema = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"input-schema","input-schema",-266884346).cljs$core$IFn$_invoke$arity$1(tool);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var execute_fn = (function (_tool_call_id,tool_args,a,b,c){
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
var args = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(tool_args,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.fn_QMARK_(on_update)){
var G__518063_518237 = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Calling MCP tool "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)+"...")], null)], null)], null));
(on_update.cljs$core$IFn$_invoke$arity$1 ? on_update.cljs$core$IFn$_invoke$arity$1(G__518063_518237) : on_update.call(null,G__518063_518237));
} else {
}

return knoxx.backend.mcp_bridge.call_tool_BANG_(tool_id,args).then((function (result){
return cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()], null)], null)], null));
})).catch((function (err){
return cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),"text",new cljs.core.Keyword(null,"text","text",-1790561697),(""+"MCP tool error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (err["message"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err));
}
})()))], null)], null)], null));
}));
});
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"name","name",1843675177),tool_id,new cljs.core.Keyword(null,"label","label",1718410804),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(tool);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(tool);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"promptSnippet","promptSnippet",-592322820),(""+"Call MCP tool "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)),new cljs.core.Keyword(null,"promptGuidelines","promptGuidelines",2131626915),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(""+"Use "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)+" when the task requires capabilities from the "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"serverId","serverId",-264111159).cljs$core$IFn$_invoke$arity$1(tool))+" MCP server.")], null),new cljs.core.Keyword(null,"parameters","parameters",-1229919748),cljs.core.clj__GT_js((function (){var or__5142__auto__ = input_schema;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword(null,"execute","execute",-129499188),execute_fn], null);
}),tools));
} else {
return null;
}
});

//# sourceMappingURL=knoxx.backend.mcp_bridge.js.map
