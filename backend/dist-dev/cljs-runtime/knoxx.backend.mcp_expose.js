import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.agent_hydration.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.state.js";
goog.provide('knoxx.backend.mcp_expose');
/**
 * Resolve the effective CLJS config map.
 * 
 * server.mjs currently holds a JS object (from core/config-js), but the tool
 * factories expect a CLJS map with keyword keys. We prefer the live in-memory
 * config (runtime.state/config*) because it includes enrich-config and any
 * admin overrides, falling back to runtime-config/cfg if needed.
 */
knoxx.backend.mcp_expose.resolve_config = (function knoxx$backend$mcp_expose$resolve_config(config){
if(cljs.core.map_QMARK_(config)){
return config;
} else {
var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.runtime.config.cfg();
}

}
});
/**
 * Return the same JS tool objects the Knoxx agent runtime uses.
 * 
 * Parameters:
 * - runtime: JS runtime bundle passed from server.mjs
 * - config: Knoxx config map
 * - ctx-js: a JS request context object from policyDb.resolveRequestContext
 * 
 * Returns: a JS array of tool objects.
 * Each tool has at least:
 * - name (string)
 * - description (string)
 * - parameters (TypeBox schema)
 * - execute (fn)
 * 
 *   NOTE: We intentionally accept JS contexts here because the JS bootstrap owns
 * the policyDb instance. CLJS code expects keyword keys, so we keywordize.
 */
knoxx.backend.mcp_expose.create_knoxx_custom_tools_js = (function knoxx$backend$mcp_expose$create_knoxx_custom_tools_js(runtime,config,ctx_js){
var ctx = (cljs.core.truth_(ctx_js)?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(ctx_js,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)):null);
var cfg = knoxx.backend.mcp_expose.resolve_config(config);
return knoxx.backend.agent_hydration.create_knoxx_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,cfg,ctx);
});

//# sourceMappingURL=knoxx.backend.mcp_expose.js.map
