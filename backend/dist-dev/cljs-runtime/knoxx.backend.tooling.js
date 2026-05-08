import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.contracts.resolve.js";
import "./knoxx.backend.contracts.roles.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.mcp_bridge.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.tools.registry.js";
goog.provide('knoxx.backend.tooling');
knoxx.backend.tooling.current_config = (function knoxx$backend$tooling$current_config(){
var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.runtime.config.cfg();
}
});
knoxx.backend.tooling.normalize_role = (function knoxx$backend$tooling$normalize_role(role){
return knoxx.backend.contracts.roles.normalize_role(knoxx.backend.tooling.current_config(),role);
});
/**
 * Normalize role slug strings to kebab-case and deduplicate.
 * Collapses mixed forms like 'knowledge_worker' / 'knowledge-worker' into one.
 */
knoxx.backend.tooling.normalize_slugs = (function knoxx$backend$tooling$normalize_slugs(slugs){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__58762_SHARP_){
return clojure.string.replace(cljs.core.name(p1__58762_SHARP_),"_","-");
}),(function (){var or__5142__auto__ = slugs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.tooling.email_enabled_QMARK_ = (function knoxx$backend$tooling$email_enabled_QMARK_(config){
return (((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"gmail-app-email","gmail-app-email",-654288582).cljs$core$IFn$_invoke$arity$1(config))))) && ((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"gmail-app-password","gmail-app-password",-1448333374).cljs$core$IFn$_invoke$arity$1(config))))));
});
knoxx.backend.tooling.discord_enabled_QMARK_ = (function knoxx$backend$tooling$discord_enabled_QMARK_(config){
return (!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"discord-bot-token","discord-bot-token",1224757550).cljs$core$IFn$_invoke$arity$1(config))));
});
knoxx.backend.tooling.auth_tool_ids = (function knoxx$backend$tooling$auth_tool_ids(auth_context){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.filter.cljs$core$IFn$_invoke$arity$1((function (p1__58770_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("allow",new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(p1__58770_SHARP_));
})),cljs.core.map.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"toolId","toolId",-1935596543))),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(auth_context));
});
knoxx.backend.tooling.resolve_actor = (function knoxx$backend$tooling$resolve_actor(config,actor_id){
return knoxx.backend.contracts.resolve.resolve_actor(config,actor_id);
});
knoxx.backend.tooling.actor_catalog = (function knoxx$backend$tooling$actor_catalog(config){
return knoxx.backend.contracts.resolve.actor_catalog(config);
});
knoxx.backend.tooling.default_actor_id = (function knoxx$backend$tooling$default_actor_id(config){
return knoxx.backend.contracts.resolve.default_actor_id(config);
});
knoxx.backend.tooling.resolve_agent_contract = (function knoxx$backend$tooling$resolve_agent_contract(var_args){
var G__58787 = arguments.length;
switch (G__58787) {
case 2:
return knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2 = (function (config,contract_id){
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,contract_id,null);
}));

(knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3 = (function (config,contract_id,actor_id){
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,contract_id,actor_id);
}));

(knoxx.backend.tooling.resolve_agent_contract.cljs$lang$maxFixedArity = 3);

knoxx.backend.tooling.agent_contract_catalog = (function knoxx$backend$tooling$agent_contract_catalog(var_args){
var G__58816 = arguments.length;
switch (G__58816) {
case 1:
return knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2(config,null);
}));

(knoxx.backend.tooling.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2 = (function (config,actor_id){
return knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2(config,actor_id);
}));

(knoxx.backend.tooling.agent_contract_catalog.cljs$lang$maxFixedArity = 2);

knoxx.backend.tooling.default_agent_contract_id = (function knoxx$backend$tooling$default_agent_contract_id(var_args){
var G__58827 = arguments.length;
switch (G__58827) {
case 1:
return knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,null);
}));

(knoxx.backend.tooling.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2 = (function (config,actor_id){
return knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,actor_id);
}));

(knoxx.backend.tooling.default_agent_contract_id.cljs$lang$maxFixedArity = 2);

knoxx.backend.tooling.effective_agent_contract = (function knoxx$backend$tooling$effective_agent_contract(var_args){
var G__58834 = arguments.length;
switch (G__58834) {
case 2:
return knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$2 = (function (config,requested_contract_id){
return knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,requested_contract_id,null);
}));

(knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3 = (function (config,requested_contract_id,actor_id){
return knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,requested_contract_id,actor_id);
}));

(knoxx.backend.tooling.effective_agent_contract.cljs$lang$maxFixedArity = 3);

knoxx.backend.tooling.ensure_role_can_use_BANG_ = (function knoxx$backend$tooling$ensure_role_can_use_BANG_(var_args){
var G__58847 = arguments.length;
switch (G__58847) {
case 2:
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (role,tool_id){
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$5(null,role,tool_id,null,null);
}));

(knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (auth_context,role,tool_id){
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$5(auth_context,role,tool_id,null,null);
}));

(knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (auth_context,role,tool_id,agent_contract_id){
return knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$5(auth_context,role,tool_id,agent_contract_id,null);
}));

(knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$core$IFn$_invoke$arity$5 = (function (auth_context,role,tool_id,agent_contract_id,actor_id){
var config = knoxx.backend.tooling.current_config();
var contract_spec = knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,agent_contract_id,actor_id);
var normalized = knoxx.backend.contracts.roles.normalize_role(config,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(contract_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return role;
}
})());
var contract_tool_ids = (function (){var G__58868 = contract_spec;
var G__58868__$1 = (((G__58868 == null))?null:new cljs.core.Keyword(null,"tool-ids","tool-ids",-1358371034).cljs$core$IFn$_invoke$arity$1(G__58868));
if((G__58868__$1 == null)){
return null;
} else {
return cljs.core.set(G__58868__$1);
}
})();
var role_tool_ids = cljs.core.set(knoxx.backend.contracts.roles.role_tool_ids(config,normalized));
var allowed = (cljs.core.truth_(contract_tool_ids)?contract_tool_ids:(cljs.core.truth_(auth_context)?knoxx.backend.tooling.auth_tool_ids(auth_context):role_tool_ids
));
if(cljs.core.contains_QMARK_(allowed,tool_id)){
} else {
if(cljs.core.truth_(auth_context)){
throw knoxx.backend.http.http_error((403),"tool_denied",(""+"Current Knoxx policy does not allow tool '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)+"'"));
} else {
throw (new Error((""+"Role '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(normalized)+"' cannot use tool '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tool_id)+"'")));
}
}

return normalized;
}));

(knoxx.backend.tooling.ensure_role_can_use_BANG_.cljs$lang$maxFixedArity = 5);

knoxx.backend.tooling.resolve_tool_context = (function knoxx$backend$tooling$resolve_tool_context(config,role,auth_context,agent_contract_id,actor_id){
var contract_spec = knoxx.backend.tooling.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,agent_contract_id,actor_id);
var actor_spec = (function (){var or__5142__auto__ = (cljs.core.truth_(actor_id)?knoxx.backend.tooling.resolve_actor(config,actor_id):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(contract_spec);
if(cljs.core.truth_(temp__5825__auto__)){
var resolved_actor_id = temp__5825__auto__;
return knoxx.backend.tooling.resolve_actor(config,resolved_actor_id);
} else {
return null;
}
}
})();
var normalized = knoxx.backend.contracts.roles.normalize_role(config,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(contract_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return role;
}
})());
var role_tool_ids = cljs.core.set(knoxx.backend.contracts.roles.role_tool_ids(config,normalized));
var _ = console.log("[tooling/resolve-tool-context]",cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),agent_contract_id,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id,new cljs.core.Keyword(null,"contract-spec-id","contract-spec-id",657356173),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract_spec),new cljs.core.Keyword(null,"tool-ids-from-contract","tool-ids-from-contract",2067884743),cljs.core.vec(new cljs.core.Keyword(null,"tool-ids","tool-ids",-1358371034).cljs$core$IFn$_invoke$arity$1(contract_spec)),new cljs.core.Keyword(null,"role-slugs-from-contract","role-slugs-from-contract",1926274832),knoxx.backend.tooling.normalize_slugs(new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(contract_spec)),new cljs.core.Keyword(null,"actor-role-slugs","actor-role-slugs",1597094544),knoxx.backend.tooling.normalize_slugs(new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(actor_spec))], null)));
var allowed_tool_ids = (cljs.core.truth_(contract_spec)?cljs.core.set(new cljs.core.Keyword(null,"tool-ids","tool-ids",-1358371034).cljs$core$IFn$_invoke$arity$1(contract_spec)):(cljs.core.truth_(auth_context)?knoxx.backend.tooling.auth_tool_ids(auth_context):role_tool_ids
));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"contract-spec","contract-spec",-1213908296),contract_spec,new cljs.core.Keyword(null,"actor-spec","actor-spec",-1363690060),actor_spec,new cljs.core.Keyword(null,"normalized-role","normalized-role",804748660),normalized,new cljs.core.Keyword(null,"allowed-tool-ids","allowed-tool-ids",1814920623),allowed_tool_ids], null);
});
knoxx.backend.tooling.allowed_tool_id_set = (function knoxx$backend$tooling$allowed_tool_id_set(var_args){
var G__58910 = arguments.length;
switch (G__58910) {
case 2:
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$2 = (function (config,role){
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,role,null,null,null);
}));

(knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$3 = (function (config,role,auth_context){
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,role,auth_context,null,null);
}));

(knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$4 = (function (config,role,auth_context,agent_contract_id){
return knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,role,auth_context,agent_contract_id,null);
}));

(knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5 = (function (config,role,auth_context,agent_contract_id,actor_id){
return new cljs.core.Keyword(null,"allowed-tool-ids","allowed-tool-ids",1814920623).cljs$core$IFn$_invoke$arity$1(knoxx.backend.tooling.resolve_tool_context(config,role,auth_context,agent_contract_id,actor_id));
}));

(knoxx.backend.tooling.allowed_tool_id_set.cljs$lang$maxFixedArity = 5);

knoxx.backend.tooling.tool_catalog = (function knoxx$backend$tooling$tool_catalog(var_args){
var G__58938 = arguments.length;
switch (G__58938) {
case 2:
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$2 = (function (config,role){
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$5(config,role,null,null,null);
}));

(knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$3 = (function (config,role,auth_context){
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$5(config,role,auth_context,null,null);
}));

(knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$4 = (function (config,role,auth_context,agent_contract_id){
return knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$5(config,role,auth_context,agent_contract_id,null);
}));

(knoxx.backend.tooling.tool_catalog.cljs$core$IFn$_invoke$arity$5 = (function (config,role,auth_context,agent_contract_id,actor_id){
var email_QMARK_ = knoxx.backend.tooling.email_enabled_QMARK_(config);
var discord_QMARK_ = knoxx.backend.tooling.discord_enabled_QMARK_(config);
var live_mcp_tool_ids = (cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"mcp-enabled","mcp-enabled",-2146653267).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(and__5140__auto__)){
return ((knoxx.backend.mcp_bridge.available_QMARK_()) && (knoxx.backend.mcp_bridge.enabled_QMARK_()));
} else {
return and__5140__auto__;
}
})())?cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092)),knoxx.backend.mcp_bridge.catalog()):cljs.core.PersistentHashSet.EMPTY);
var map__58964 = knoxx.backend.tooling.resolve_tool_context(config,role,auth_context,agent_contract_id,actor_id);
var map__58964__$1 = cljs.core.__destructure_map(map__58964);
var contract_spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58964__$1,new cljs.core.Keyword(null,"contract-spec","contract-spec",-1213908296));
var actor_spec = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58964__$1,new cljs.core.Keyword(null,"actor-spec","actor-spec",-1363690060));
var normalized_role = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58964__$1,new cljs.core.Keyword(null,"normalized-role","normalized-role",804748660));
var allowed_tool_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58964__$1,new cljs.core.Keyword(null,"allowed-tool-ids","allowed-tool-ids",1814920623));
var base_tools = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
var map__58970 = knoxx.backend.tools.registry.get_tool(tool_id);
var map__58970__$1 = cljs.core.__destructure_map(map__58970);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58970__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58970__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),tool_id,new cljs.core.Keyword(null,"label","label",1718410804),label,new cljs.core.Keyword(null,"description","description",-1428560544),description,new cljs.core.Keyword(null,"enabled","enabled",1195909756),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(tool_id,"email.send"))?email_QMARK_:((clojure.string.starts_with_QMARK_(tool_id,"discord."))?discord_QMARK_:((clojure.string.starts_with_QMARK_(tool_id,"mcp."))?cljs.core.contains_QMARK_(live_mcp_tool_ids,tool_id):true
)))], null);
}),cljs.core.sort.cljs$core$IFn$_invoke$arity$1(allowed_tool_ids));
var tools = (function (){var G__58984 = base_tools;
if(cljs.core.contains_QMARK_(allowed_tool_ids,"semantic_query")){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__58984,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),"graph_query",new cljs.core.Keyword(null,"label","label",1718410804),"Graph Query",new cljs.core.Keyword(null,"description","description",-1428560544),"Query the canonical knowledge graph across devel/web/bluesky/knoxx-session lakes",new cljs.core.Keyword(null,"enabled","enabled",1195909756),true], null));
} else {
return G__58984;
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"task_prompt","task_prompt",1276696196),new cljs.core.Keyword(null,"role_slugs","role_slugs",2101192325),new cljs.core.Keyword(null,"email_enabled","email_enabled",-1965651127),new cljs.core.Keyword(null,"tools","tools",-1241731990),new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"capability_ids","capability_ids",-706143570),new cljs.core.Keyword(null,"agent_system_prompt","agent_system_prompt",-1671550769),new cljs.core.Keyword(null,"actor_system_prompt","actor_system_prompt",-2141052778),new cljs.core.Keyword(null,"agent_label","agent_label",803211513),new cljs.core.Keyword(null,"agent_id","agent_id",-1820880197),new cljs.core.Keyword(null,"agent_trigger_kind","agent_trigger_kind",-2083901605),new cljs.core.Keyword(null,"system_prompt","system_prompt",-655033954)],[(cljs.core.truth_(auth_context)?(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(contract_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.authz.primary_context_role(auth_context);
}
})():normalized_role),(((((auth_context == null)) || (knoxx.backend.authz.system_admin_QMARK_(auth_context))))?new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(contract_spec):null),knoxx.backend.tooling.normalize_slugs(new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(contract_spec)),email_QMARK_,tools,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"capability-ids","capability-ids",-1477528817).cljs$core$IFn$_invoke$arity$1(contract_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),(((((auth_context == null)) || (knoxx.backend.authz.system_admin_QMARK_(auth_context))))?new cljs.core.Keyword(null,"agent-system-prompt","agent-system-prompt",-1576864491).cljs$core$IFn$_invoke$arity$1(contract_spec):null),(((((auth_context == null)) || (knoxx.backend.authz.system_admin_QMARK_(auth_context))))?new cljs.core.Keyword(null,"actor-system-prompt","actor-system-prompt",-1563106020).cljs$core$IFn$_invoke$arity$1(contract_spec):null),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract_spec),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(contract_spec),new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783).cljs$core$IFn$_invoke$arity$1(contract_spec),(((((auth_context == null)) || (knoxx.backend.authz.system_admin_QMARK_(auth_context))))?new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(contract_spec):null)]);
}));

(knoxx.backend.tooling.tool_catalog.cljs$lang$maxFixedArity = 5);

/**
 * Return eta-mu built-in tool names enabled for this runtime.
 * 
 * eta-mu 0.70 changed createAgentSession :tools from cwd-bound Tool objects to
 * a string allowlist. Passing createReadTool/createBashTool objects now causes
 * tool registration/selection failures, so Knoxx must pass names and let eta-mu
 * bind built-ins to :cwd itself.
 */
knoxx.backend.tooling.create_runtime_tools = (function knoxx$backend$tooling$create_runtime_tools(var_args){
var G__58993 = arguments.length;
switch (G__58993) {
case 3:
return knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 6:
return knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
return knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$6(runtime,config,auth_context,null,null,null);
}));

(knoxx.backend.tooling.create_runtime_tools.cljs$core$IFn$_invoke$arity$6 = (function (_runtime,config,auth_context,role,agent_contract_id,actor_id){
var allowed_tool_ids = knoxx.backend.tooling.allowed_tool_id_set.cljs$core$IFn$_invoke$arity$5(config,role,auth_context,agent_contract_id,actor_id);
var allowed_QMARK_ = (function (tool_id){
return cljs.core.contains_QMARK_(allowed_tool_ids,tool_id);
});
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("read"))?"read":null),((allowed_QMARK_("write"))?"write":null),((allowed_QMARK_("edit"))?"edit":null),((allowed_QMARK_("bash"))?"bash":null)], null)));
}));

(knoxx.backend.tooling.create_runtime_tools.cljs$lang$maxFixedArity = 6);


//# sourceMappingURL=knoxx.backend.tooling.js.map
