import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.runtime.contract_loader.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.runtime.roles.js";
import "./knoxx.backend.tooling.js";
import "./knoxx.backend.tools.registry.js";
import "./knoxx.backend.util.parse.js";
goog.provide('knoxx.backend.triggers.control_config');
knoxx.backend.triggers.control_config.env = (function knoxx$backend$triggers$control_config$env(k,default$){
var or__5142__auto__ = (process.env[k]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default$;
}
});
knoxx.backend.triggers.control_config.parse_string_list = (function knoxx$backend$triggers$control_config$parse_string_list(raw){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34225 = value;
var G__34225__$1 = (((G__34225 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34225)));
var G__34225__$2 = (((G__34225__$1 == null))?null:clojure.string.trim(G__34225__$1));
if((G__34225__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34225__$2);
}
}),clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = raw;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/,/))));
});


knoxx.backend.triggers.control_config.keywordish__GT_string = (function knoxx$backend$triggers$control_config$keywordish__GT_string(value){
if((value instanceof cljs.core.Keyword)){
return cljs.core.name(value);
} else {
if(typeof value === 'string'){
var G__34322 = value;
var G__34322__$1 = (((G__34322 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34322)));
var G__34322__$2 = (((G__34322__$1 == null))?null:clojure.string.trim(G__34322__$1));
if((G__34322__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34322__$2);
}
} else {
if((value == null)){
return null;
} else {
var G__34331 = value;
var G__34331__$1 = (((G__34331 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34331)));
var G__34331__$2 = (((G__34331__$1 == null))?null:clojure.string.trim(G__34331__$1));
if((G__34331__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34331__$2);
}

}
}
}
});
knoxx.backend.triggers.control_config.nonblank_str = (function knoxx$backend$triggers$control_config$nonblank_str(value){
var G__34353 = value;
var G__34353__$1 = (((G__34353 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34353)));
var G__34353__$2 = (((G__34353__$1 == null))?null:clojure.string.trim(G__34353__$1));
if((G__34353__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34353__$2);
}
});
knoxx.backend.triggers.control_config.keywordish__GT_event_kind = (function knoxx$backend$triggers$control_config$keywordish__GT_event_kind(value){
if((value instanceof cljs.core.Keyword)){
var ns = cljs.core.namespace(value);
var nm = cljs.core.name(value);
if(cljs.core.truth_((function (){var and__5140__auto__ = ns;
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(ns)));
} else {
return and__5140__auto__;
}
})())){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+"."+cljs.core.str.cljs$core$IFn$_invoke$arity$1(nm));
} else {
return nm;
}
} else {
if(typeof value === 'string'){
var G__34383 = value;
var G__34383__$1 = (((G__34383 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34383)));
var G__34383__$2 = (((G__34383__$1 == null))?null:clojure.string.trim(G__34383__$1));
var G__34383__$3 = (((G__34383__$2 == null))?null:cljs.core.not_empty(G__34383__$2));
if((G__34383__$3 == null)){
return null;
} else {
return clojure.string.replace(G__34383__$3,/\//,".");
}
} else {
return null;

}
}
});
knoxx.backend.triggers.control_config.explicit_tool_ids = (function knoxx$backend$triggers$control_config$explicit_tool_ids(contract){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"tools","tools",-1241731990)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.triggers.control_config.tool_policies_from_contract = (function knoxx$backend$triggers$control_config$tool_policies_from_contract(config,role,contract){
var explicit = knoxx.backend.triggers.control_config.explicit_tool_ids(contract);
var role_tool_ids = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = role;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))))?cljs.core.PersistentVector.EMPTY:(knoxx.backend.runtime.roles.role_tool_ids.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.roles.role_tool_ids.cljs$core$IFn$_invoke$arity$2(config,role) : knoxx.backend.runtime.roles.role_tool_ids.call(null,config,role)));
var tool_ids = ((cljs.core.seq(explicit))?explicit:role_tool_ids);
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null);
}),tool_ids);
});
knoxx.backend.triggers.control_config.filters_from_contract = (function knoxx$backend$triggers$control_config$filters_from_contract(contract){
var filters = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"filters","filters",974726919)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var G__34450 = cljs.core.PersistentArrayMap.EMPTY;
var G__34450__$1 = ((cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"channels","channels",1132759174).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450,new cljs.core.Keyword(null,"channels","channels",1132759174),cljs.core.vec(new cljs.core.Keyword(null,"channels","channels",1132759174).cljs$core$IFn$_invoke$arity$1(filters))):G__34450);
var G__34450__$2 = ((cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"keywords","keywords",1526959054).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$1,new cljs.core.Keyword(null,"keywords","keywords",1526959054),cljs.core.vec(new cljs.core.Keyword(null,"keywords","keywords",1526959054).cljs$core$IFn$_invoke$arity$1(filters))):G__34450__$1);
var G__34450__$3 = ((((new cljs.core.Keyword(null,"matchAll","matchAll",-601915638).cljs$core$IFn$_invoke$arity$1(filters) === true) || (new cljs.core.Keyword(null,"match_all","match_all",1325819031).cljs$core$IFn$_invoke$arity$1(filters) === true)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$2,new cljs.core.Keyword(null,"matchAll","matchAll",-601915638),true):G__34450__$2);
var G__34450__$4 = ((cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"publish_channels","publish_channels",2144419461).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})()))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$3,new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"publish_channels","publish_channels",2144419461).cljs$core$IFn$_invoke$arity$1(filters);
}
})())):G__34450__$3);
var G__34450__$5 = ((cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"guildIds","guildIds",785308327).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"guild_ids","guild_ids",-681975868).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})()))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$4,new cljs.core.Keyword(null,"guildIds","guildIds",785308327),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"guildIds","guildIds",785308327).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"guild_ids","guild_ids",-681975868).cljs$core$IFn$_invoke$arity$1(filters);
}
})())):G__34450__$4);
var G__34450__$6 = ((cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"authorIds","authorIds",1639016820).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"author_ids","author_ids",-1682078222).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"authors","authors",2063018172).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
}
})()))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$5,new cljs.core.Keyword(null,"authorIds","authorIds",1639016820),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"authorIds","authorIds",1639016820).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"author_ids","author_ids",-1682078222).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"authors","authors",2063018172).cljs$core$IFn$_invoke$arity$1(filters);
}
}
})())):G__34450__$5);
if(cljs.core.seq((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"repositories","repositories",1367837581).cljs$core$IFn$_invoke$arity$1(filters);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34450__$6,new cljs.core.Keyword(null,"repositories","repositories",1367837581),cljs.core.vec(new cljs.core.Keyword(null,"repositories","repositories",1367837581).cljs$core$IFn$_invoke$arity$1(filters)));
} else {
return G__34450__$6;
}
});
knoxx.backend.triggers.control_config.memory_hydration_from_contract = (function knoxx$backend$triggers$control_config$memory_hydration_from_contract(contract){
var or__5142__auto__ = new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passive-hydration","passive-hydration",-1337823895)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"memory","memory",-1449401430),new cljs.core.Keyword(null,"passiveHydration","passiveHydration",-884994907)], null));
}
}
}
});
knoxx.backend.triggers.control_config.contract__GT_event_agent_job = (function knoxx$backend$triggers$control_config$contract__GT_event_agent_job(config,contract_id,contract,contract_hash){
var trigger_kind = knoxx.backend.triggers.control_config.keywordish__GT_string(new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783).cljs$core$IFn$_invoke$arity$1(contract));
var resolved = knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2(config,contract_id);
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword("contract","kind","contract/kind",1929672067).cljs$core$IFn$_invoke$arity$1(contract))) && (cljs.core.contains_QMARK_(knoxx.backend.triggers.control_config.event_agent_trigger_kinds,trigger_kind)))){
var role = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(resolved);
var model = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(resolved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (knoxx.backend.triggers.control_config.default_discord_model.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.triggers.control_config.default_discord_model.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.triggers.control_config.default_discord_model.call(null,config));
}
})();
var thinking_level = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(resolved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__34492 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"thinking","thinking",2063777387)], null));
if((G__34492 == null)){
return null;
} else {
return knoxx.backend.triggers.control_config.keywordish__GT_string(G__34492);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})();
var source_kind = (function (){var or__5142__auto__ = knoxx.backend.triggers.control_config.keywordish__GT_string(new cljs.core.Keyword(null,"source-kind","source-kind",-1955827566).cljs$core$IFn$_invoke$arity$1(contract));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "manual";
}
})();
var source_mode = (function (){var or__5142__auto__ = knoxx.backend.triggers.control_config.keywordish__GT_string(new cljs.core.Keyword(null,"source-mode","source-mode",725702471).cljs$core$IFn$_invoke$arity$1(contract));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "respond";
}
})();
var cadence = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"cadence-min","cadence-min",508372506).cljs$core$IFn$_invoke$arity$1(contract));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (5);
}
})());
var always_kinds = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.triggers.control_config.keywordish__GT_event_kind,(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.Keyword(null,"always","always",-1772028770)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
var maybe_kinds = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.triggers.control_config.keywordish__GT_event_kind,(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.Keyword(null,"maybe","maybe",-314397560)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
var event_kinds = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(always_kinds,maybe_kinds)));
var event_weights = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.Keyword(null,"weights","weights",-1097626197)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var event_threshold = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"events","events",1792552201),new cljs.core.Keyword(null,"threshold","threshold",204221583)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var filters = knoxx.backend.triggers.control_config.filters_from_contract(contract);
var source_config = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"source","source",-433931539)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var memory_hydration = knoxx.backend.triggers.control_config.memory_hydration_from_contract(contract);
var sticky_session_QMARK_ = ((new cljs.core.Keyword(null,"stickySession","stickySession",1252676028).cljs$core$IFn$_invoke$arity$1(source_config) === true) || (new cljs.core.Keyword(null,"sticky_session","sticky_session",-495460458).cljs$core$IFn$_invoke$arity$1(source_config) === true));
var session_max_messages = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286).cljs$core$IFn$_invoke$arity$1(source_config));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"session_max_messages","session_max_messages",13587375).cljs$core$IFn$_invoke$arity$1(source_config));
}
})();
var streaming_behavior = (function (){var G__34546 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748).cljs$core$IFn$_invoke$arity$1(source_config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"streaming_behavior","streaming_behavior",1463087832).cljs$core$IFn$_invoke$arity$1(source_config);
}
})();
var G__34546__$1 = (((G__34546 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34546)));
var G__34546__$2 = (((G__34546__$1 == null))?null:clojure.string.trim(G__34546__$1));
if((G__34546__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34546__$2);
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"contractHash","contractHash",1130922241),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227),new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402),new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"enabled","enabled",1195909756)],[(function (){var or__5142__auto__ = (function (){var G__34557 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"task","task",-1476607993)], null));
var G__34557__$1 = (((G__34557 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34557)));
var G__34557__$2 = (((G__34557__$1 == null))?null:clojure.string.trim(G__34557__$1));
if((G__34557__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34557__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__34564 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"system","system",-29381724)], null));
var G__34564__$1 = (((G__34564 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34564)));
var G__34564__$2 = (((G__34564__$1 == null))?null:clojure.string.trim(G__34564__$1));
if((G__34564__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34564__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return contract_id;
}
}
})(),contract_hash,new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(resolved),contract_id,"agent",filters,contract_id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),source_kind,new cljs.core.Keyword(null,"mode","mode",654403691),source_mode,new cljs.core.Keyword(null,"config","config",994861415),(function (){var G__34570 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),(function (){var G__34572 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"max-messages","max-messages",-1089461657)], null));
var G__34573 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379)], null));
return (knoxx.backend.triggers.control_config.clamp_max_messages.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.triggers.control_config.clamp_max_messages.cljs$core$IFn$_invoke$arity$2(G__34572,G__34573) : knoxx.backend.triggers.control_config.clamp_max_messages.call(null,G__34572,G__34573));
})()], null);
var G__34570__$1 = ((sticky_session_QMARK_)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34570,new cljs.core.Keyword(null,"stickySession","stickySession",1252676028),true):G__34570);
var G__34570__$2 = (cljs.core.truth_(session_max_messages)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34570__$1,new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286),session_max_messages):G__34570__$1);
if(cljs.core.truth_(streaming_behavior)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34570__$2,new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748),streaming_behavior);
} else {
return G__34570__$2;
}
})()], null),(""+"agent:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id)),contract_id,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"contractId","contractId",710260199),new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"contractActors","contractActors",47284059),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = role;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))))?new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config):role),(function (){var or__5142__auto__ = (function (){var G__34582 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"task","task",-1476607993)], null));
var G__34582__$1 = (((G__34582 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34582)));
if((G__34582__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__34582__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(resolved),contract_id,memory_hydration,cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(resolved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.triggers.control_config.tool_policies_from_contract(config,role,contract);
}
})()),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(resolved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__34596 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"system","system",-29381724)], null));
var G__34596__$1 = (((G__34596 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34596)));
if((G__34596__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__34596__$1);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817).cljs$core$IFn$_invoke$arity$1(resolved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),thinking_level,model]),new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"kind","kind",-717265803),trigger_kind,new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),cadence,new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),event_kinds,new cljs.core.Keyword(null,"alwaysKinds","alwaysKinds",-464748381),always_kinds,new cljs.core.Keyword(null,"maybeKinds","maybeKinds",-549657535),maybe_kinds,new cljs.core.Keyword(null,"eventWeights","eventWeights",2010194476),event_weights,new cljs.core.Keyword(null,"eventThreshold","eventThreshold",-167593365),event_threshold], null),(!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(contract) === false))]);
} else {
return null;
}
});
knoxx.backend.triggers.control_config.contract_agent_jobs = (function knoxx$backend$triggers$control_config$contract_agent_jobs(config){
try{return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (record){
return knoxx.backend.triggers.control_config.contract__GT_event_agent_job(config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record),new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record),cljs.core.hash(new cljs.core.Keyword(null,"edn-text","edn-text",-2069322458).cljs$core$IFn$_invoke$arity$1(record)));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__34608_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("agents",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__34608_SHARP_));
}),(knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.contract_loader.load_all_contracts_sync.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.contract_loader.load_all_contracts_sync.call(null,config)))))));
}catch (e34616){var _ = e34616;
return cljs.core.PersistentVector.EMPTY;
}});
knoxx.backend.triggers.control_config.default_discord_channels = (function knoxx$backend$triggers$control_config$default_discord_channels(){
return knoxx.backend.triggers.control_config.parse_string_list(knoxx.backend.triggers.control_config.env("DISCORD_CHANNEL_IDS",""));
});
knoxx.backend.triggers.control_config.default_discord_keywords = (function knoxx$backend$triggers$control_config$default_discord_keywords(){
var keywords = knoxx.backend.triggers.control_config.parse_string_list(knoxx.backend.triggers.control_config.env("DISCORD_TARGET_KEYWORDS","knoxx,cephalon"));
if(cljs.core.seq(keywords)){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(clojure.string.lower_case,keywords);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["knoxx","cephalon"], null);
}
});
knoxx.backend.triggers.control_config.event_agent_control_redis_key = "event-agent:control-config";
knoxx.backend.triggers.control_config.event_agent_trigger_kinds = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["cron",null,"event",null], null), null);
knoxx.backend.triggers.control_config.max_messages_limit = (100);
knoxx.backend.triggers.control_config.clamp_max_messages = (function knoxx$backend$triggers$control_config$clamp_max_messages(value,fallback){
var n = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(value);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.util.parse.parse_positive_int(fallback);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (25);
}
}
})();
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2(knoxx.backend.triggers.control_config.max_messages_limit,n));
});
knoxx.backend.triggers.control_config.clamp_source_config = (function knoxx$backend$triggers$control_config$clamp_source_config(source_config,default_config){
var cfg = (function (){var or__5142__auto__ = source_config;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var G__34630 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(cfg,new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),knoxx.backend.triggers.control_config.clamp_max_messages(new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379).cljs$core$IFn$_invoke$arity$1(cfg),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379).cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = default_config;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})())));
var G__34630__$1 = ((cljs.core.contains_QMARK_(cfg,new cljs.core.Keyword(null,"stickySession","stickySession",1252676028)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34630,new cljs.core.Keyword(null,"stickySession","stickySession",1252676028),cljs.core.boolean$(new cljs.core.Keyword(null,"stickySession","stickySession",1252676028).cljs$core$IFn$_invoke$arity$1(cfg))):G__34630);
if(cljs.core.truth_(knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286).cljs$core$IFn$_invoke$arity$1(cfg)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34630__$1,new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286),knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286).cljs$core$IFn$_invoke$arity$1(cfg)));
} else {
return G__34630__$1;
}
});
/**
 * Persist the event-agent-control overrides to Redis so they survive restarts.
 */
knoxx.backend.triggers.control_config.persist_event_agent_control_BANG_ = (function knoxx$backend$triggers$control_config$persist_event_agent_control_BANG_(control){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3(client,knoxx.backend.triggers.control_config.event_agent_control_redis_key,control).then((function (_){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[control-config] persisted event-agent-control to Redis"], 0));

return control;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[control-config] failed to persist event-agent-control to Redis:",err.message], 0));

return control;
}));
} else {
return Promise.resolve(control);
}
});
/**
 * Load event-agent-control overrides from Redis. Returns nil if not found.
 */
knoxx.backend.triggers.control_config.load_event_agent_control = (function knoxx$backend$triggers$control_config$load_event_agent_control(){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return knoxx.backend.redis_client.get_json(client,knoxx.backend.triggers.control_config.event_agent_control_redis_key).then((function (saved){
if(cljs.core.truth_(saved)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[control-config] loaded event-agent-control from Redis"], 0));
} else {
}

return saved;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[control-config] failed to load event-agent-control from Redis:",err.message], 0));

return null;
}));
} else {
return Promise.resolve(null);
}
});
/**
 * Roles that can be used for scheduled Discord jobs.
 * 
 * Source of truth lives in contracts/roles/*.edn.
 */
knoxx.backend.triggers.control_config.discord_agent_role_options = (function knoxx$backend$triggers$control_config$discord_agent_role_options(config){
return (knoxx.backend.runtime.roles.list_role_slugs.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.roles.list_role_slugs.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.roles.list_role_slugs.call(null,config));
});
knoxx.backend.triggers.control_config.default_discord_agent_jobs = (function knoxx$backend$triggers$control_config$default_discord_agent_jobs(config){
var default_model = (knoxx.backend.triggers.control_config.default_discord_model.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.triggers.control_config.default_discord_model.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.triggers.control_config.default_discord_model.call(null,config));
var known_roles = cljs.core.set((knoxx.backend.runtime.roles.list_role_slugs.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.roles.list_role_slugs.cljs$core$IFn$_invoke$arity$1(config) : knoxx.backend.runtime.roles.list_role_slugs.call(null,config)));
var configured_role = (function (){var G__34664 = config;
var G__34665 = new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
return (knoxx.backend.runtime.roles.normalize_role.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.roles.normalize_role.cljs$core$IFn$_invoke$arity$2(G__34664,G__34665) : knoxx.backend.runtime.roles.normalize_role.call(null,G__34664,G__34665));
})();
var default_role = (function (){var or__5142__auto__ = ((cljs.core.contains_QMARK_(known_roles,configured_role))?configured_role:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.first(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(known_roles));
}
})();
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),new cljs.core.Keyword(null,"channels","channels",1132759174),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"keywords","keywords",1526959054),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[default_role,"Read recent channel messages, update freshness state, and queue human messages that mention the bot or contain target keywords.","Poll configured Discord channels, remember fresh messages, and queue signals for follow-up jobs.",(25),knoxx.backend.triggers.control_config.default_discord_channels(),"Channel patrol",knoxx.backend.triggers.control_config.default_discord_keywords(),"Observe configured channels, detect fresh human signals, and queue them without speaking publicly.",(5),"patrol","observer",true,"off",default_model]),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),new cljs.core.Keyword(null,"channels","channels",1132759174),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"keywords","keywords",1526959054),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[default_role,"A queued Discord message needs triage. Use discord.read or discord.search if needed, then either stay silent or reply with discord.publish.","Process queued mentions and keyword hits, then decide whether Knoxx should answer.",(12),knoxx.backend.triggers.control_config.default_discord_channels(),"Mention response",knoxx.backend.triggers.control_config.default_discord_keywords(),"You are Knoxx's targeted Discord responder. Read the room before replying, stay useful, and prefer silence over filler.",(1),"mentions","response",true,"off",default_model]),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),new cljs.core.Keyword(null,"channels","channels",1132759174),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"keywords","keywords",1526959054),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[default_role,"Summarize recent cross-channel activity, identify important opportunities or risks, and decide whether to publish a concise proactive message.","Periodically synthesize cross-channel activity and decide whether a proactive intervention is warranted.",(12),knoxx.backend.triggers.control_config.default_discord_channels(),"Deep synthesis",knoxx.backend.triggers.control_config.default_discord_keywords(),"You are Knoxx's strategic Discord synthesizer. Look across channels, find meaningful patterns, and only speak when synthesis helps humans.",(120),"deep-synthesis","synthesis",true,"minimal",default_model])], null);
});
knoxx.backend.triggers.control_config.normalize_discord_job = (function knoxx$backend$triggers$control_config$normalize_discord_job(config,default_job,raw_job){
var allowed_roles = cljs.core.set(knoxx.backend.triggers.control_config.discord_agent_role_options(config));
var source = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([default_job,(function (){var or__5142__auto__ = raw_job;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
var cadence = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (5);
}
}
})();
var role = (function (){var candidate = (function (){var G__34668 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(source);
var G__34668__$1 = (((G__34668 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34668)));
var G__34668__$2 = (((G__34668__$1 == null))?null:clojure.string.trim(G__34668__$1));
if((G__34668__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34668__$2);
}
})();
if(cljs.core.contains_QMARK_(allowed_roles,candidate)){
return candidate;
} else {
return new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(default_job);
}
})();
var thinking_level = (function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_thinking_level(new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "off";
}
}
}
})();
var channels = (function (){var candidate = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34670 = value;
var G__34670__$1 = (((G__34670 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34670)));
var G__34670__$2 = (((G__34670__$1 == null))?null:clojure.string.trim(G__34670__$1));
if((G__34670__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34670__$2);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"channels","channels",1132759174).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
if(cljs.core.seq(candidate)){
return candidate;
} else {
return new cljs.core.Keyword(null,"channels","channels",1132759174).cljs$core$IFn$_invoke$arity$1(default_job);
}
})();
var keywords = (function (){var candidate = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34674 = value;
var G__34674__$1 = (((G__34674 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34674)));
var G__34674__$2 = (((G__34674__$1 == null))?null:clojure.string.trim(G__34674__$1));
var G__34674__$3 = (((G__34674__$2 == null))?null:clojure.string.lower_case(G__34674__$2));
if((G__34674__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34674__$3);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"keywords","keywords",1526959054).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.seq(candidate)){
return candidate;
} else {
return new cljs.core.Keyword(null,"keywords","keywords",1526959054).cljs$core$IFn$_invoke$arity$1(default_job);
}
})();
var max_messages = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (25);
}
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379),new cljs.core.Keyword(null,"channels","channels",1132759174),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"keywords","keywords",1526959054),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[role,(function (){var or__5142__auto__ = (function (){var G__34681 = new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(source);
var G__34681__$1 = (((G__34681 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34681)));
var G__34681__$2 = (((G__34681__$1 == null))?null:clojure.string.trim(G__34681__$1));
if((G__34681__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34681__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),(function (){var or__5142__auto__ = (function (){var G__34682 = new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(source);
var G__34682__$1 = (((G__34682 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34682)));
var G__34682__$2 = (((G__34682__$1 == null))?null:clojure.string.trim(G__34682__$1));
if((G__34682__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34682__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((100),max_messages)),channels,(function (){var or__5142__auto__ = (function (){var G__34683 = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(source);
var G__34683__$1 = (((G__34683 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34683)));
var G__34683__$2 = (((G__34683__$1 == null))?null:clojure.string.trim(G__34683__$1));
if((G__34683__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34683__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),keywords,(function (){var or__5142__auto__ = (function (){var G__34684 = new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(source);
var G__34684__$1 = (((G__34684 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34684)));
var G__34684__$2 = (((G__34684__$1 == null))?null:clojure.string.trim(G__34684__$1));
if((G__34684__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34684__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((10080),cadence)),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(default_job),new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(default_job),(!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(source) === false)),thinking_level,(function (){var or__5142__auto__ = (function (){var G__34689 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(source);
var G__34689__$1 = (((G__34689 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34689)));
var G__34689__$2 = (((G__34689__$1 == null))?null:clojure.string.trim(G__34689__$1));
if((G__34689__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34689__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"proxx-default-model","proxx-default-model",-927829764).cljs$core$IFn$_invoke$arity$1(config);
}
}
})()]);
});
knoxx.backend.triggers.control_config.discord_agent_control_config = (function knoxx$backend$triggers$control_config$discord_agent_control_config(config){
var saved = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"discord-agent-control","discord-agent-control",-1228454123).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var defaults = knoxx.backend.triggers.control_config.default_discord_agent_jobs(config);
var saved_by_id = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (job){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),job], null);
})),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(saved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var merged_jobs = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (default_job){
return knoxx.backend.triggers.control_config.normalize_discord_job(config,default_job,cljs.core.get.cljs$core$IFn$_invoke$arity$2(saved_by_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(default_job)));
}),defaults);
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"botUserId","botUserId",971606494),(function (){var or__5142__auto__ = (function (){var G__34694 = new cljs.core.Keyword(null,"botUserId","botUserId",971606494).cljs$core$IFn$_invoke$arity$1(saved);
var G__34694__$1 = (((G__34694 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34694)));
var G__34694__$2 = (((G__34694__$1 == null))?null:clojure.string.trim(G__34694__$1));
if((G__34694__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34694__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__34695 = knoxx.backend.triggers.control_config.env("DISCORD_BOT_USER_ID","");
var G__34695__$1 = (((G__34695 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34695)));
var G__34695__$2 = (((G__34695__$1 == null))?null:clojure.string.trim(G__34695__$1));
if((G__34695__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34695__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685),(function (){var saved_channels = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34697 = value;
var G__34697__$1 = (((G__34697 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34697)));
var G__34697__$2 = (((G__34697__$1 == null))?null:clojure.string.trim(G__34697__$1));
if((G__34697__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34697__$2);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685).cljs$core$IFn$_invoke$arity$1(saved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
if(cljs.core.seq(saved_channels)){
return saved_channels;
} else {
return knoxx.backend.triggers.control_config.default_discord_channels();
}
})(),new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449),(function (){var saved_keywords = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34698 = value;
var G__34698__$1 = (((G__34698 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34698)));
var G__34698__$2 = (((G__34698__$1 == null))?null:clojure.string.trim(G__34698__$1));
var G__34698__$3 = (((G__34698__$2 == null))?null:clojure.string.lower_case(G__34698__$2));
if((G__34698__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34698__$3);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449).cljs$core$IFn$_invoke$arity$1(saved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.seq(saved_keywords)){
return saved_keywords;
} else {
return knoxx.backend.triggers.control_config.default_discord_keywords();
}
})(),new cljs.core.Keyword(null,"jobs","jobs",-313607120),merged_jobs], null);
});
knoxx.backend.triggers.control_config.event_agent_role_options = (function knoxx$backend$triggers$control_config$event_agent_role_options(config){
return knoxx.backend.triggers.control_config.discord_agent_role_options(config);
});
knoxx.backend.triggers.control_config.event_agent_source_kind_options = (function knoxx$backend$triggers$control_config$event_agent_source_kind_options(){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord","github","cron","manual"], null);
});
knoxx.backend.triggers.control_config.event_agent_trigger_kind_options = (function knoxx$backend$triggers$control_config$event_agent_trigger_kind_options(){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["cron","event"], null);
});
knoxx.backend.triggers.control_config.normalize_tool_policy_entry = (function knoxx$backend$triggers$control_config$normalize_tool_policy_entry(policy){
var tool_id = (function (){var G__34699 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool-id","tool-id",-290456894).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"tool_id","tool_id",1550520216).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})();
var G__34699__$1 = (((G__34699 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34699)));
var G__34699__$2 = (((G__34699__$1 == null))?null:clojure.string.trim(G__34699__$1));
if((G__34699__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34699__$2);
}
})();
var effect = (function (){var G__34702 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "allow";
}
})();
var G__34702__$1 = (((G__34702 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34702)));
var G__34702__$2 = (((G__34702__$1 == null))?null:clojure.string.trim(G__34702__$1));
var G__34702__$3 = (((G__34702__$2 == null))?null:clojure.string.lower_case(G__34702__$2));
if((G__34702__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34702__$3);
}
})();
if(cljs.core.truth_(tool_id)){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),(cljs.core.truth_((function (){var fexpr__34703 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["allow",null,"deny",null], null), null);
return (fexpr__34703.cljs$core$IFn$_invoke$arity$1 ? fexpr__34703.cljs$core$IFn$_invoke$arity$1(effect) : fexpr__34703.call(null,effect));
})())?effect:"allow")], null);
} else {
return null;
}
});
knoxx.backend.triggers.control_config.normalize_tool_policy_list = (function knoxx$backend$triggers$control_config$normalize_tool_policy_list(policies){
return cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.triggers.control_config.normalize_tool_policy_entry,(function (){var or__5142__auto__ = policies;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
});
knoxx.backend.triggers.control_config.default_discord_tool_policies = (function knoxx$backend$triggers$control_config$default_discord_tool_policies(){
return new cljs.core.PersistentVector(null, 15, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channel.messages",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channel.scroll",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.dm.messages",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.publish",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.send",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.guilds",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.channels",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.list.servers",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"discord.list.channels",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"websearch",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"web.read",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"memory_search",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),"graph_query",new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null)], null);
});
knoxx.backend.triggers.control_config.derive_default_discord_channels = (function knoxx$backend$triggers$control_config$derive_default_discord_channels(jobs){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34719 = value;
var G__34719__$1 = (((G__34719 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34719)));
var G__34719__$2 = (((G__34719__$1 == null))?null:clojure.string.trim(G__34719__$1));
if((G__34719__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34719__$2);
}
}),cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (job){
return cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"channels","channels",1132759174)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"publish_channels","publish_channels",2144419461)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()], 0));
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var or__5142__auto__ = jobs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()], 0))))));
});
knoxx.backend.triggers.control_config.default_discord_model = (function knoxx$backend$triggers$control_config$default_discord_model(_config){
return "gemma4:31b";
});
knoxx.backend.triggers.control_config.default_event_agent_control = (function knoxx$backend$triggers$control_config$default_event_agent_control(config){
var jobs = knoxx.backend.triggers.control_config.contract_agent_jobs(config);
var inferred_default_channels = knoxx.backend.triggers.control_config.derive_default_discord_channels(jobs);
var configured_default_channels = knoxx.backend.triggers.control_config.default_discord_channels();
var default_discord_source = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"botUserId","botUserId",971606494),(function (){var or__5142__auto__ = (function (){var G__34723 = knoxx.backend.triggers.control_config.env("DISCORD_BOT_USER_ID","");
var G__34723__$1 = (((G__34723 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34723)));
var G__34723__$2 = (((G__34723__$1 == null))?null:clojure.string.trim(G__34723__$1));
if((G__34723__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34723__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685),((cljs.core.seq(configured_default_channels))?configured_default_channels:inferred_default_channels),new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449),knoxx.backend.triggers.control_config.default_discord_keywords()], null);
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"discord","discord",480262077),default_discord_source,new cljs.core.Keyword(null,"github","github",567794498),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"webhookSecretConfigured","webhookSecretConfigured",2037841273),cljs.core.boolean$((function (){var G__34724 = knoxx.backend.triggers.control_config.env("GITHUB_WEBHOOK_SECRET","");
var G__34724__$1 = (((G__34724 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34724)));
var G__34724__$2 = (((G__34724__$1 == null))?null:clojure.string.trim(G__34724__$1));
if((G__34724__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34724__$2);
}
})())], null),new cljs.core.Keyword(null,"cron","cron",-1939354481),cljs.core.PersistentArrayMap.EMPTY], null),new cljs.core.Keyword(null,"jobs","jobs",-313607120),jobs], null);
});
knoxx.backend.triggers.control_config.normalize_event_agent_job = (function knoxx$backend$triggers$control_config$normalize_event_agent_job(config,default_job,raw_job){
var allowed_roles = cljs.core.set(knoxx.backend.triggers.control_config.event_agent_role_options(config));
var contract_sourced_QMARK_ = (function (){var G__34732 = new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(default_job);
var G__34732__$1 = (((G__34732 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34732)));
var G__34732__$2 = (((G__34732__$1 == null))?null:clojure.string.trim(G__34732__$1));
if((G__34732__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34732__$2);
}
})();
var saved_contract_hash = new cljs.core.Keyword(null,"contractHash","contractHash",1130922241).cljs$core$IFn$_invoke$arity$1(raw_job);
var current_contract_hash = new cljs.core.Keyword(null,"contractHash","contractHash",1130922241).cljs$core$IFn$_invoke$arity$1(default_job);
var saved_job_current_QMARK_ = (function (){var or__5142__auto__ = cljs.core.not(contract_sourced_QMARK_);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var and__5140__auto__ = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(raw_job),new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(default_job));
if(and__5140__auto__){
var and__5140__auto____$1 = current_contract_hash;
if(cljs.core.truth_(and__5140__auto____$1)){
var and__5140__auto____$2 = saved_contract_hash;
if(cljs.core.truth_(and__5140__auto____$2)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(saved_contract_hash,current_contract_hash);
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
}
})();
var source = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([default_job,(cljs.core.truth_(saved_job_current_QMARK_)?(function (){var or__5142__auto__ = raw_job;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})():cljs.core.PersistentArrayMap.EMPTY)], 0));
var trigger_source = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(default_job),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
var source_config = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(default_job),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
var agent_source = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
var trigger_kind = (function (){var candidate = (function (){var G__34735 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(trigger_source);
var G__34735__$1 = (((G__34735 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34735)));
var G__34735__$2 = (((G__34735__$1 == null))?null:clojure.string.trim(G__34735__$1));
var G__34735__$3 = (((G__34735__$2 == null))?null:clojure.string.lower_case(G__34735__$2));
if((G__34735__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34735__$3);
}
})();
if(cljs.core.truth_((function (){var fexpr__34736 = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["cron",null,"event",null], null), null);
return (fexpr__34736.cljs$core$IFn$_invoke$arity$1 ? fexpr__34736.cljs$core$IFn$_invoke$arity$1(candidate) : fexpr__34736.call(null,candidate));
})())){
return candidate;
} else {
return new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(default_job));
}
})();
var cadence = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405).cljs$core$IFn$_invoke$arity$1(trigger_source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (5);
}
}
})();
var event_kinds = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34739 = value;
var G__34739__$1 = (((G__34739 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34739)));
var G__34739__$2 = (((G__34739__$1 == null))?null:clojure.string.trim(G__34739__$1));
if((G__34739__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34739__$2);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289).cljs$core$IFn$_invoke$arity$1(trigger_source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
var source_kind = (function (){var candidate = (function (){var G__34742 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(source_config);
var G__34742__$1 = (((G__34742 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34742)));
var G__34742__$2 = (((G__34742__$1 == null))?null:clojure.string.trim(G__34742__$1));
var G__34742__$3 = (((G__34742__$2 == null))?null:clojure.string.lower_case(G__34742__$2));
if((G__34742__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34742__$3);
}
})();
if(cljs.core.truth_(cljs.core.some((function (p1__34725_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(candidate,p1__34725_SHARP_);
}),knoxx.backend.triggers.control_config.event_agent_source_kind_options()))){
return candidate;
} else {
return new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(default_job));
}
})();
var source_mode = (function (){var or__5142__auto__ = (function (){var G__34746 = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(source_config);
var G__34746__$1 = (((G__34746 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34746)));
var G__34746__$2 = (((G__34746__$1 == null))?null:clojure.string.trim(G__34746__$1));
if((G__34746__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34746__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "observe";
}
}
})();
var role = (function (){var candidate = (function (){var G__34750 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_source);
var G__34750__$1 = (((G__34750 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34750)));
var G__34750__$2 = (((G__34750__$1 == null))?null:clojure.string.trim(G__34750__$1));
if((G__34750__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34750__$2);
}
})();
if(cljs.core.contains_QMARK_(allowed_roles,candidate)){
return candidate;
} else {
return new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
}
})();
var thinking_level = (function (){var or__5142__auto__ = knoxx.backend.runtime.models.normalize_thinking_level(new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(agent_source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"agent-thinking-level","agent-thinking-level",1959324030).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "off";
}
}
}
})();
var actor_id = (function (){var or__5142__auto__ = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(agent_source));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_source));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(agent_source));
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var or__5142__auto____$7 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job)));
if(cljs.core.truth_(or__5142__auto____$7)){
return or__5142__auto____$7;
} else {
return knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job)));
}
}
}
}
}
}
}
}
})();
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"description","description",-1428560544),new cljs.core.Keyword(null,"contractHash","contractHash",1130922241),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227),new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402),new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"enabled","enabled",1195909756)],[(function (){var or__5142__auto__ = (function (){var G__34751 = new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(source);
var G__34751__$1 = (((G__34751 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34751)));
var G__34751__$2 = (((G__34751__$1 == null))?null:clojure.string.trim(G__34751__$1));
if((G__34751__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34751__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractHash","contractHash",1130922241).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contractHash","contractHash",1130922241).cljs$core$IFn$_invoke$arity$1(source);
}
})(),actor_id,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"filters","filters",974726919).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"filters","filters",974726919).cljs$core$IFn$_invoke$arity$1(default_job);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
})(),(function (){var or__5142__auto__ = (function (){var G__34752 = new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(source);
var G__34752__$1 = (((G__34752 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34752)));
var G__34752__$2 = (((G__34752__$1 == null))?null:clojure.string.trim(G__34752__$1));
if((G__34752__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34752__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),source_kind,new cljs.core.Keyword(null,"mode","mode",654403691),source_mode,new cljs.core.Keyword(null,"config","config",994861415),knoxx.backend.triggers.control_config.clamp_source_config(new cljs.core.Keyword(null,"config","config",994861415).cljs$core$IFn$_invoke$arity$1(source_config),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(default_job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415)], null)))], null),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630).cljs$core$IFn$_invoke$arity$1(source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630).cljs$core$IFn$_invoke$arity$1(default_job);
}
})(),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(default_job),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"contractId","contractId",710260199),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),new cljs.core.Keyword(null,"contractActors","contractActors",47284059),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),new cljs.core.Keyword(null,"model","model",331153215)],[role,(function (){var or__5142__auto__ = (function (){var G__34753 = new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(agent_source);
var G__34753__$1 = (((G__34753 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34753)));
if((G__34753__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__34753__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),actor_id,(function (){var or__5142__auto__ = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"contractId","contractId",710260199).cljs$core$IFn$_invoke$arity$1(agent_source));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(source));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.triggers.control_config.nonblank_str(new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(default_job));
}
}
})(),(function (){var normalized = knoxx.backend.triggers.control_config.normalize_tool_policy_list(new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(agent_source));
if(((cljs.core.seq(normalized)) && (cljs.core.every_QMARK_(cljs.core.some_QMARK_,normalized)))){
return normalized;
} else {
return knoxx.backend.triggers.control_config.normalize_tool_policy_list(new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job)));
}
})(),(function (){var or__5142__auto__ = (function (){var G__34754 = new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(agent_source);
var G__34754__$1 = (((G__34754 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34754)));
if((G__34754__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__34754__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractActors","contractActors",47284059).cljs$core$IFn$_invoke$arity$1(agent_source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contractActors","contractActors",47284059).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})()),thinking_level,(function (){var or__5142__auto__ = (function (){var G__34755 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(agent_source);
var G__34755__$1 = (((G__34755 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34755)));
var G__34755__$2 = (((G__34755__$1 == null))?null:clojure.string.trim(G__34755__$1));
if((G__34755__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34755__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(default_job));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.triggers.control_config.default_discord_model(config);
}
}
})()]),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),trigger_kind,new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((10080),cadence)),new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),event_kinds], null),(!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(source) === false))]);
});
knoxx.backend.triggers.control_config.event_agent_control_config = (function knoxx$backend$triggers$control_config$event_agent_control_config(config){
var saved = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var defaults = knoxx.backend.triggers.control_config.default_event_agent_control(config);
var default_sources = new cljs.core.Keyword(null,"sources","sources",-321166424).cljs$core$IFn$_invoke$arity$1(defaults);
var saved_sources = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"sources","sources",-321166424).cljs$core$IFn$_invoke$arity$1(saved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var saved_discord_source = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"discord","discord",480262077).cljs$core$IFn$_invoke$arity$1(saved_sources);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var saved_discord_channels = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34757 = value;
var G__34757__$1 = (((G__34757 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34757)));
var G__34757__$2 = (((G__34757__$1 == null))?null:clojure.string.trim(G__34757__$1));
if((G__34757__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34757__$2);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685).cljs$core$IFn$_invoke$arity$1(saved_discord_source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
var saved_discord_keywords = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__34758 = value;
var G__34758__$1 = (((G__34758 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34758)));
var G__34758__$2 = (((G__34758__$1 == null))?null:clojure.string.trim(G__34758__$1));
var G__34758__$3 = (((G__34758__$2 == null))?null:clojure.string.lower_case(G__34758__$2));
if((G__34758__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__34758__$3);
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449).cljs$core$IFn$_invoke$arity$1(saved_discord_source);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
var github_webhook_secret_configured = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(default_sources,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"github","github",567794498),new cljs.core.Keyword(null,"webhookSecretConfigured","webhookSecretConfigured",2037841273)], null));
var default_jobs = new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(defaults);
var saved_jobs = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(saved);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var default_job_ids = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092)),default_jobs);
var saved_jobs_by_id = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (job){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),job], null);
})),saved_jobs);
var custom_jobs = cljs.core.vec(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (job){
var job_id = (function (){var G__34759 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
var G__34759__$1 = (((G__34759 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__34759)));
var G__34759__$2 = (((G__34759__$1 == null))?null:clojure.string.trim(G__34759__$1));
if((G__34759__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__34759__$2);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = job_id;
if(cljs.core.truth_(and__5140__auto__)){
return (!(cljs.core.contains_QMARK_(default_job_ids,job_id)));
} else {
return and__5140__auto__;
}
})())){
return knoxx.backend.triggers.control_config.normalize_event_agent_job(config,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),job_id,new cljs.core.Keyword(null,"name","name",1843675177),(function (){var or__5142__auto__ = job_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "custom-job";
}
})(),new cljs.core.Keyword(null,"enabled","enabled",1195909756),true,new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"event",new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),(5),new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),cljs.core.PersistentVector.EMPTY], null),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"manual",new cljs.core.Keyword(null,"mode","mode",654403691),"respond",new cljs.core.Keyword(null,"config","config",994861415),cljs.core.PersistentArrayMap.EMPTY], null),new cljs.core.Keyword(null,"filters","filters",974726919),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config),new cljs.core.Keyword(null,"model","model",331153215),knoxx.backend.triggers.control_config.default_discord_model(config),new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429),"off",new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886),"You are Knoxx's scheduled event agent. Respond to dispatched events, use Discord tools when needed, and emit useful actions without filler.",new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720),"A structured event matched this job. Read context, decide what action is useful, and use available tools deliberately.",new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),knoxx.backend.triggers.control_config.default_discord_tool_policies()], null),new cljs.core.Keyword(null,"description","description",-1428560544),"Custom scheduled event-agent job"], null),job);
} else {
return null;
}
}),saved_jobs));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"sources","sources",-321166424),cljs.core.assoc_in(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"discord","discord",480262077),(function (){var G__34762 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"discord","discord",480262077).cljs$core$IFn$_invoke$arity$1(default_sources),saved_discord_source], 0));
var G__34762__$1 = ((cljs.core.empty_QMARK_(saved_discord_channels))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34762,new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(default_sources,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685)], null))):G__34762);
var G__34762__$2 = ((cljs.core.seq(saved_discord_channels))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34762__$1,new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685),saved_discord_channels):G__34762__$1);
var G__34762__$3 = ((cljs.core.empty_QMARK_(saved_discord_keywords))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34762__$2,new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(default_sources,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449)], null))):G__34762__$2);
if(cljs.core.seq(saved_discord_keywords)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__34762__$3,new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449),saved_discord_keywords);
} else {
return G__34762__$3;
}
})(),new cljs.core.Keyword(null,"github","github",567794498),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"github","github",567794498).cljs$core$IFn$_invoke$arity$1(default_sources),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"github","github",567794498).cljs$core$IFn$_invoke$arity$1(saved_sources);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0)),new cljs.core.Keyword(null,"cron","cron",-1939354481),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"cron","cron",-1939354481).cljs$core$IFn$_invoke$arity$1(default_sources),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"cron","cron",-1939354481).cljs$core$IFn$_invoke$arity$1(saved_sources);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0))], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"github","github",567794498),new cljs.core.Keyword(null,"webhookSecretConfigured","webhookSecretConfigured",2037841273)], null),cljs.core.boolean$(github_webhook_secret_configured)),new cljs.core.Keyword(null,"jobs","jobs",-313607120),cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (default_job){
return knoxx.backend.triggers.control_config.normalize_event_agent_job(config,default_job,cljs.core.get.cljs$core$IFn$_invoke$arity$2(saved_jobs_by_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(default_job)));
}),default_jobs),custom_jobs))], null);
});

//# sourceMappingURL=knoxx.backend.triggers.control_config.js.map
