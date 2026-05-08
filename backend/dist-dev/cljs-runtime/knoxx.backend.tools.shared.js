import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.runtime.state.js";
import "./malli.json_schema.js";
import "./knoxx.backend.http.js";
goog.provide('knoxx.backend.tools.shared');
/**
 * Convert a Malli schema to a Pi tool :parameters JS object.
 */
knoxx.backend.tools.shared.__GT_params = (function knoxx$backend$tools$shared$__GT_params(schema){
return cljs.core.clj__GT_js(malli.json_schema.transform.cljs$core$IFn$_invoke$arity$1(schema));
});
knoxx.backend.tools.shared.create_tool_obj = (function knoxx$backend$tools$shared$create_tool_obj(name,label,description,prompt,prompt_guidelines,params,execute,runtime,config){
return ({"name": name, "label": label, "description": description, "promptSnippet": prompt, "promptGuidelines": cljs.core.clj__GT_js(prompt_guidelines), "parameters": knoxx.backend.tools.shared.__GT_params(params), "execute": cljs.core.partial.cljs$core$IFn$_invoke$arity$3(execute,runtime,config)});
});
/**
 * Call an on-update callback with a text status update.
 */
knoxx.backend.tools.shared.maybe_tool_update_BANG_ = (function knoxx$backend$tools$shared$maybe_tool_update_BANG_(f,text){
if(cljs.core.fn_QMARK_(f)){
var G__521275 = ({"content": [({"type": "text", "text": text})]});
return (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(G__521275) : f.call(null,G__521275));
} else {
return null;
}
});
knoxx.backend.tools.shared.type_optional = (function knoxx$backend$tools$shared$type_optional(Type,schema){
return Type.Optional(schema);
});
knoxx.backend.tools.shared.sanitize_custom_tool_name = (function knoxx$backend$tools$shared$sanitize_custom_tool_name(tool){
var name = (function (){var G__521276 = (tool["name"]);
if((G__521276 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521276));
}
})();
var sanitized = (function (){var G__521277 = name;
var G__521277__$1 = (((G__521277 == null))?null:clojure.string.replace(G__521277,/[^A-Za-z0-9_-]/,"_"));
if((G__521277__$1 == null)){
return null;
} else {
return clojure.string.replace(G__521277__$1,/_+/,"_");
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = sanitized;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(sanitized,name);
} else {
return and__5140__auto__;
}
})())){
(tool["name"] = sanitized);

(tool["originalName"] = name);

var temp__5825__auto___521300 = (function (){var G__521279 = (tool["description"]);
if((G__521279 == null)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521279));
}
})();
if(cljs.core.truth_(temp__5825__auto___521300)){
var description_521301 = temp__5825__auto___521300;
(tool["description"] = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(description_521301)+" Original tool id: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)+"."));
} else {
}
} else {
}

return tool;
});
knoxx.backend.tools.shared.sanitize_custom_tools = (function knoxx$backend$tools$shared$sanitize_custom_tools(tools){
var items = (cljs.core.truth_(cljs.core.array_QMARK_(tools))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(tools):cljs.core.PersistentVector.EMPTY);
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.shared.sanitize_custom_tool_name,items));
});
/**
 * Filter a collection of tool objects to only those whose name (or originalName)
 * appears in allowed-tool-ids.
 */
knoxx.backend.tools.shared.filter_custom_tools_by_allow_set = (function knoxx$backend$tools$shared$filter_custom_tools_by_allow_set(tools,allowed_tool_ids){
if((allowed_tool_ids == null)){
return tools;
} else {
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (tool){
var runtime_id = (function (){var G__521281 = tool;
var G__521281__$1 = (((G__521281 == null))?null:(G__521281["name"]));
var G__521281__$2 = (((G__521281__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521281__$1)));
var G__521281__$3 = (((G__521281__$2 == null))?null:clojure.string.trim(G__521281__$2));
if((G__521281__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__521281__$3);
}
})();
var original_id = (function (){var G__521283 = tool;
var G__521283__$1 = (((G__521283 == null))?null:(G__521283["originalName"]));
var G__521283__$2 = (((G__521283__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521283__$1)));
var G__521283__$3 = (((G__521283__$2 == null))?null:clojure.string.trim(G__521283__$2));
if((G__521283__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__521283__$3);
}
})();
var or__5142__auto__ = (function (){var and__5140__auto__ = runtime_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(allowed_tool_ids,runtime_id);
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var and__5140__auto__ = original_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(allowed_tool_ids,original_id);
} else {
return and__5140__auto__;
}
}
}),knoxx.backend.http.js_array_seq(tools)));
}
});
/**
 * Parse JSON string to Clojure data.
 */
knoxx.backend.tools.shared.json_parse = (function knoxx$backend$tools$shared$json_parse(text){
try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(text),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e521286){var err = e521286;
throw (new Error((""+"Invalid JSON: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))));
}});
/**
 * Resolve live config, preferring the runtime atom.
 */
knoxx.backend.tools.shared.live_config = (function knoxx$backend$tools$shared$live_config(config){
var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
});
/**
 * Classify an agent spec into a tool-suite keyword.
 */
knoxx.backend.tools.shared.agent_custom_tool_suite = (function knoxx$backend$tools$shared$agent_custom_tool_suite(agent_spec){
var role = (function (){var G__521288 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__521288__$1 = (((G__521288 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521288)));
var G__521288__$2 = (((G__521288__$1 == null))?null:clojure.string.trim(G__521288__$1));
if((G__521288__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__521288__$2);
}
})();
var contract_id = (function (){var G__521289 = new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec);
var G__521289__$1 = (((G__521289 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__521289)));
var G__521289__$2 = (((G__521289__$1 == null))?null:clojure.string.trim(G__521289__$1));
if((G__521289__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__521289__$2);
}
})();
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"contract_librarian")) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(contract_id,"contract_librarian")))){
return new cljs.core.Keyword(null,"contract-librarian","contract-librarian",638098080);
} else {
return new cljs.core.Keyword(null,"knoxx","knoxx",-1928448572);
}
});

//# sourceMappingURL=knoxx.backend.tools.shared.js.map
