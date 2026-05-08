import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.mcp_bridge.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.mcp');
/**
 * Create agent SDK custom tools for all connected MCP servers.
 * Returns a JS array of tool objects, or an empty array if MCP is disabled.
 */
knoxx.backend.tools.mcp.create_mcp_custom_tools = (function knoxx$backend$tools$mcp$create_mcp_custom_tools(var_args){
var G__58768 = arguments.length;
switch (G__58768) {
case 2:
return knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (_runtime,config,auth_context){
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"mcp-enabled","mcp-enabled",-2146653267).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(and__5140__auto__)){
return ((knoxx.backend.mcp_bridge.available_QMARK_()) && (knoxx.backend.mcp_bridge.enabled_QMARK_()));
} else {
return and__5140__auto__;
}
})())){
var tools = (function (){var temp__5823__auto__ = knoxx.backend.mcp_bridge.mcp_tools_for_agent();
if(cljs.core.truth_(temp__5823__auto__)){
var items = temp__5823__auto__;
if(cljs.core.truth_(cljs.core.array_QMARK_(items))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(items);
} else {
return cljs.core.PersistentVector.EMPTY;
}
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((((auth_context == null))?tools:cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (tool){
var temp__5825__auto__ = (function (){var G__58824 = tool;
var G__58824__$1 = (((G__58824 == null))?null:(G__58824["name"]));
var G__58824__$2 = (((G__58824__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58824__$1)));
var G__58824__$3 = (((G__58824__$2 == null))?null:clojure.string.trim(G__58824__$2));
if((G__58824__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__58824__$3);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var tool_id = temp__5825__auto__;
return knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id);
} else {
return null;
}
}),tools)));
} else {
return [];
}
}));

(knoxx.backend.tools.mcp.create_mcp_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.mcp.js.map
