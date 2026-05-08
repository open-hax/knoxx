import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.contracts.actor_scope.js";
import "./knoxx.backend.contracts.loader.js";
import "./knoxx.backend.contracts.roles.js";
import "./knoxx.backend.tools.registry.js";
goog.provide('knoxx.backend.contracts.resolve');
knoxx.backend.contracts.resolve.known_actor_keys = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 13, [new cljs.core.Keyword(null,"model-profile","model-profile",-1997108992),null,new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),null,new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),null,new cljs.core.Keyword(null,"capability-ids","capability-ids",-1477528817),null,new cljs.core.Keyword(null,"default-agent","default-agent",279723152),null,new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),null,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),null,new cljs.core.Keyword(null,"id","id",-1388402092),null,new cljs.core.Keyword(null,"kind","kind",-717265803),null,new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),null,new cljs.core.Keyword("ui","actions","ui/actions",-812652422),null,new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),null,new cljs.core.Keyword(null,"model","model",331153215),null], null), null);
knoxx.backend.contracts.resolve.known_role_keys = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword("role","permissions","role/permissions",54401385),null,new cljs.core.Keyword("role","capabilities","role/capabilities",208971087),null,new cljs.core.Keyword(null,"id","id",-1388402092),null,new cljs.core.Keyword("role","prompts","role/prompts",12162071),null], null), null);
knoxx.backend.contracts.resolve.known_capability_keys = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("capability","description","capability/description",2107615880),null,new cljs.core.Keyword("capability","tools","capability/tools",122081170),null,new cljs.core.Keyword(null,"id","id",-1388402092),null], null), null);
knoxx.backend.contracts.resolve.known_agent_keys = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 11, [new cljs.core.Keyword("contract","uses","contract/uses",715473218),null,new cljs.core.Keyword("contract","actors","contract/actors",-1138019932),null,new cljs.core.Keyword("agent","thinking","agent/thinking",1954119048),null,new cljs.core.Keyword("contract","actor","contract/actor",1959324173),null,new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783),null,new cljs.core.Keyword(null,"id","id",-1388402092),null,new cljs.core.Keyword("prompts","system","prompts/system",481662773),null,new cljs.core.Keyword("prompts","task","prompts/task",-1181813354),null,new cljs.core.Keyword("agent","model","agent/model",289028726),null,new cljs.core.Keyword("ui","actions","ui/actions",-812652422),null,new cljs.core.Keyword(null,"enabled","enabled",1195909756),null], null), null);
knoxx.backend.contracts.resolve.contract_extras = (function knoxx$backend$contracts$resolve$contract_extras(contract_data,known_set){
if(cljs.core.truth_(contract_data)){
var extras = cljs.core.apply.cljs$core$IFn$_invoke$arity$3(cljs.core.dissoc,contract_data,cljs.core.seq(known_set));
if(cljs.core.seq(extras)){
return extras;
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.contracts.resolve.actor_extras = (function knoxx$backend$contracts$resolve$actor_extras(actor_spec){
return knoxx.backend.contracts.resolve.contract_extras(actor_spec,knoxx.backend.contracts.resolve.known_actor_keys);
});
knoxx.backend.contracts.resolve.role_extras = (function knoxx$backend$contracts$resolve$role_extras(role_data){
return knoxx.backend.contracts.resolve.contract_extras(role_data,knoxx.backend.contracts.resolve.known_role_keys);
});
knoxx.backend.contracts.resolve.capability_extras = (function knoxx$backend$contracts$resolve$capability_extras(cap_data){
return knoxx.backend.contracts.resolve.contract_extras(cap_data,knoxx.backend.contracts.resolve.known_capability_keys);
});
knoxx.backend.contracts.resolve.agent_extras = (function knoxx$backend$contracts$resolve$agent_extras(agent_contract){
return knoxx.backend.contracts.resolve.contract_extras(agent_contract,knoxx.backend.contracts.resolve.known_agent_keys);
});
knoxx.backend.contracts.resolve.memory_hydration_from_contract = (function knoxx$backend$contracts$resolve$memory_hydration_from_contract(contract){
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
knoxx.backend.contracts.resolve.context_policy_from_contract = (function knoxx$backend$contracts$resolve$context_policy_from_contract(contract){
var or__5142__auto__ = new cljs.core.Keyword(null,"context","context",-830191113).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"context","context",-830191113)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557)], null));
}
}
}
}
});
knoxx.backend.contracts.resolve.all_contract_extras = (function knoxx$backend$contracts$resolve$all_contract_extras(config,actor_spec,role_slugs,capability_ids,agent_contract){
var actor_x = knoxx.backend.contracts.resolve.actor_extras(new cljs.core.Keyword(null,"actor","actor",-1830560481).cljs$core$IFn$_invoke$arity$1(actor_spec));
var role_x = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__33114_SHARP_){
return knoxx.backend.contracts.resolve.role_extras(knoxx.backend.contracts.roles.role_contract(config,p1__33114_SHARP_));
}),role_slugs);
var cap_x = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__33115_SHARP_){
return knoxx.backend.contracts.resolve.capability_extras(knoxx.backend.contracts.loader.contract_sync(config,"capabilities",p1__33115_SHARP_));
}),capability_ids);
var agent_x = knoxx.backend.contracts.resolve.agent_extras(agent_contract);
var merged = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core.into,cljs.core.PersistentArrayMap.EMPTY,cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.seq,role_x),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.seq,cap_x),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(cljs.core.truth_(agent_x)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [agent_x], null):null),(cljs.core.truth_(actor_x)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [actor_x], null):null)], 0)));
if(cljs.core.seq(merged)){
return merged;
} else {
return null;
}
});
knoxx.backend.contracts.resolve.keywordish__GT_role_slug = (function knoxx$backend$contracts$resolve$keywordish__GT_role_slug(value){
var raw = (((value instanceof cljs.core.Keyword))?cljs.core.name(value):((typeof value === 'string')?value:(((value == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value))
)));
var G__33118 = raw;
var G__33118__$1 = (((G__33118 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33118)));
var G__33118__$2 = (((G__33118__$1 == null))?null:clojure.string.trim(G__33118__$1));
if((G__33118__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33118__$2);
}
});
knoxx.backend.contracts.resolve.keywordish__GT_capability_ref = (function knoxx$backend$contracts$resolve$keywordish__GT_capability_ref(value){
if((value instanceof cljs.core.Keyword)){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.namespace(value))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(value)));
} else {
if(typeof value === 'string'){
var G__33124 = value;
var G__33124__$1 = (((G__33124 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33124)));
var G__33124__$2 = (((G__33124__$1 == null))?null:clojure.string.trim(G__33124__$1));
if((G__33124__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33124__$2);
}
} else {
if((value == null)){
return null;
} else {
var G__33130 = value;
var G__33130__$1 = (((G__33130 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33130)));
var G__33130__$2 = (((G__33130__$1 == null))?null:clojure.string.trim(G__33130__$1));
if((G__33130__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33130__$2);
}

}
}
}
});
knoxx.backend.contracts.resolve.keywordish__GT_wire = (function knoxx$backend$contracts$resolve$keywordish__GT_wire(value){
if((value instanceof cljs.core.Keyword)){
var temp__5823__auto__ = cljs.core.namespace(value);
if(cljs.core.truth_(temp__5823__auto__)){
var ns = temp__5823__auto__;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ns)+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.name(value)));
} else {
return cljs.core.name(value);
}
} else {
if(typeof value === 'string'){
var G__33137 = value;
var G__33137__$1 = (((G__33137 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33137)));
var G__33137__$2 = (((G__33137__$1 == null))?null:clojure.string.trim(G__33137__$1));
if((G__33137__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33137__$2);
}
} else {
if((value == null)){
return null;
} else {
var G__33138 = value;
var G__33138__$1 = (((G__33138 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33138)));
var G__33138__$2 = (((G__33138__$1 == null))?null:clojure.string.trim(G__33138__$1));
if((G__33138__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33138__$2);
}

}
}
}
});
knoxx.backend.contracts.resolve.ui_action_surfaces = (function knoxx$backend$contracts$resolve$ui_action_surfaces(action){
var single = knoxx.backend.contracts.resolve.keywordish__GT_wire(new cljs.core.Keyword(null,"surface","surface",699915646).cljs$core$IFn$_invoke$arity$1(action));
var many = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_wire,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"surfaces","surfaces",-2030326421).cljs$core$IFn$_invoke$arity$1(action);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((cljs.core.truth_(single)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [single], null):null),many)));
});
knoxx.backend.contracts.resolve.normalize_ui_action = (function knoxx$backend$contracts$resolve$normalize_ui_action(source,action){
var id = (function (){var G__33146 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(action);
var G__33146__$1 = (((G__33146 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33146)));
var G__33146__$2 = (((G__33146__$1 == null))?null:clojure.string.trim(G__33146__$1));
if((G__33146__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33146__$2);
}
})();
var label = (function (){var G__33147 = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(action);
var G__33147__$1 = (((G__33147 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33147)));
var G__33147__$2 = (((G__33147__$1 == null))?null:clojure.string.trim(G__33147__$1));
if((G__33147__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33147__$2);
}
})();
var surfaces = knoxx.backend.contracts.resolve.ui_action_surfaces(action);
if(cljs.core.truth_((function (){var and__5140__auto__ = id;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = label;
if(cljs.core.truth_(and__5140__auto____$1)){
return (!(new cljs.core.Keyword(null,"enabled?","enabled?",-1376075057).cljs$core$IFn$_invoke$arity$1(action) === false));
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var G__33148 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"confirm","confirm",-2004000608),new cljs.core.Keyword(null,"surfaces","surfaces",-2030326421),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"requires","requires",-1201390927),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"intent","intent",-390846953),new cljs.core.Keyword(null,"enabled","enabled",1195909756)],[cljs.core.boolean$(new cljs.core.Keyword(null,"confirm?","confirm?",-374341155).cljs$core$IFn$_invoke$arity$1(action)),surfaces,source,cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_wire,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"requires","requires",-1201390927).cljs$core$IFn$_invoke$arity$1(action);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))),label,id,(function (){var or__5142__auto__ = knoxx.backend.contracts.resolve.keywordish__GT_wire(new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(action));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "button";
}
})(),(function (){var or__5142__auto__ = knoxx.backend.contracts.resolve.keywordish__GT_wire(new cljs.core.Keyword(null,"intent","intent",-390846953).cljs$core$IFn$_invoke$arity$1(action));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "agent.run";
}
})(),true]);
var G__33148__$1 = ((cljs.core.seq(surfaces))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33148,new cljs.core.Keyword(null,"surface","surface",699915646),cljs.core.first(surfaces)):G__33148);
var G__33148__$2 = (cljs.core.truth_(new cljs.core.Keyword(null,"icon","icon",1679606541).cljs$core$IFn$_invoke$arity$1(action))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33148__$1,new cljs.core.Keyword(null,"icon","icon",1679606541),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"icon","icon",1679606541).cljs$core$IFn$_invoke$arity$1(action)))):G__33148__$1);
var G__33148__$3 = (cljs.core.truth_(new cljs.core.Keyword("agent","contract","agent/contract",-1980031674).cljs$core$IFn$_invoke$arity$1(action))?cljs.core.assoc_in(G__33148__$2,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"contractId","contractId",710260199)], null),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword("agent","contract","agent/contract",-1980031674).cljs$core$IFn$_invoke$arity$1(action)))):G__33148__$2);
var G__33148__$4 = (cljs.core.truth_(new cljs.core.Keyword("agent","actor","agent/actor",-1923440606).cljs$core$IFn$_invoke$arity$1(action))?cljs.core.assoc_in(G__33148__$3,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"actorId","actorId",989542370)], null),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword("agent","actor","agent/actor",-1923440606).cljs$core$IFn$_invoke$arity$1(action)))):G__33148__$3);
var G__33148__$5 = (cljs.core.truth_(new cljs.core.Keyword("tool","id","tool/id",-1375657828).cljs$core$IFn$_invoke$arity$1(action))?cljs.core.assoc_in(G__33148__$4,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tool","tool",-1298696470),new cljs.core.Keyword(null,"id","id",-1388402092)], null),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword("tool","id","tool/id",-1375657828).cljs$core$IFn$_invoke$arity$1(action)))):G__33148__$4);
var G__33148__$6 = (cljs.core.truth_(new cljs.core.Keyword("media","from","media/from",-1778213736).cljs$core$IFn$_invoke$arity$1(action))?cljs.core.assoc_in(G__33148__$5,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"media","media",-1066138403),new cljs.core.Keyword(null,"from","from",1815293044)], null),knoxx.backend.contracts.resolve.keywordish__GT_wire(new cljs.core.Keyword("media","from","media/from",-1778213736).cljs$core$IFn$_invoke$arity$1(action))):G__33148__$5);
if(cljs.core.truth_(new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(action))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__33148__$6,new cljs.core.Keyword(null,"mode","mode",654403691),knoxx.backend.contracts.resolve.keywordish__GT_wire(new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(action)));
} else {
return G__33148__$6;
}
} else {
return null;
}
});
knoxx.backend.contracts.resolve.action_matches_surface_QMARK_ = (function knoxx$backend$contracts$resolve$action_matches_surface_QMARK_(surface,action){
var wanted = (function (){var G__33160 = surface;
var G__33160__$1 = (((G__33160 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33160)));
var G__33160__$2 = (((G__33160__$1 == null))?null:clojure.string.trim(G__33160__$1));
if((G__33160__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33160__$2);
}
})();
var surfaces = new cljs.core.Keyword(null,"surfaces","surfaces",-2030326421).cljs$core$IFn$_invoke$arity$1(action);
var or__5142__auto__ = (wanted == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.empty_QMARK_(surfaces);
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
return cljs.core.some((function (p1__33155_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(wanted,p1__33155_SHARP_);
}),surfaces);
}
}
});
knoxx.backend.contracts.resolve.enrich_ui_action = (function knoxx$backend$contracts$resolve$enrich_ui_action(config,action){
var temp__5823__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(action,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"contractId","contractId",710260199)], null));
if(cljs.core.truth_(temp__5823__auto__)){
var contract_id = temp__5823__auto__;
var actor_id = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(action,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"actorId","actorId",989542370)], null));
var resolved = (knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,contract_id,actor_id) : knoxx.backend.contracts.resolve.resolve_agent_contract.call(null,config,contract_id,actor_id));
var G__33181 = action;
if(cljs.core.truth_(new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(resolved))){
return cljs.core.assoc_in(G__33181,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"model","model",331153215)], null),new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(resolved));
} else {
return G__33181;
}
} else {
return action;
}
});
knoxx.backend.contracts.resolve.resolve_actor = (function knoxx$backend$contracts$resolve$resolve_actor(config,actor_id){
var temp__5825__auto__ = (function (){var G__33188 = actor_id;
var G__33188__$1 = (((G__33188 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33188)));
var G__33188__$2 = (((G__33188__$1 == null))?null:clojure.string.trim(G__33188__$1));
if((G__33188__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33188__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var id = temp__5825__auto__;
var record = knoxx.backend.contracts.loader.find_contract_record_sync(config,"actors",id);
var actor = new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record);
if(cljs.core.truth_(actor)){
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record),new cljs.core.Keyword(null,"actor","actor",-1830560481),actor,new cljs.core.Keyword(null,"kind","kind",-717265803),(function (){var G__33190 = new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(actor);
if((G__33190 == null)){
return null;
} else {
return cljs.core.name(G__33190);
}
})(),new cljs.core.Keyword(null,"org","org",1495985),(function (){var G__33192 = new cljs.core.Keyword("actor","org","actor/org",175993262).cljs$core$IFn$_invoke$arity$1(actor);
var G__33192__$1 = (((G__33192 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33192)));
var G__33192__$2 = (((G__33192__$1 == null))?null:clojure.string.trim(G__33192__$1));
if((G__33192__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33192__$2);
}
})(),new cljs.core.Keyword(null,"default-agent","default-agent",279723152),(function (){var G__33196 = new cljs.core.Keyword("actor","default-agent","actor/default-agent",321319579).cljs$core$IFn$_invoke$arity$1(actor);
var G__33196__$1 = (((G__33196 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33196)));
var G__33196__$2 = (((G__33196__$1 == null))?null:clojure.string.trim(G__33196__$1));
if((G__33196__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33196__$2);
}
})(),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_role_slug,(function (){var or__5142__auto__ = new cljs.core.Keyword("actor","roles","actor/roles",186081855).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))),new cljs.core.Keyword(null,"capability-ids","capability-ids",-1477528817),cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_capability_ref,(function (){var or__5142__auto__ = new cljs.core.Keyword("actor","capabilities","actor/capabilities",-198939954).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),(function (){var G__33200 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(actor,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"system","system",-29381724)], null));
var G__33200__$1 = (((G__33200 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33200)));
var G__33200__$2 = (((G__33200__$1 == null))?null:clojure.string.trim(G__33200__$1));
if((G__33200__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33200__$2);
}
})()], null);
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.contracts.resolve.actor_catalog = (function knoxx$backend$contracts$resolve$actor_catalog(config){
return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (record){
return knoxx.backend.contracts.resolve.resolve_actor(config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33202_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("actors",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__33202_SHARP_));
}),knoxx.backend.contracts.loader.load_all_contracts_sync(config))))));
});
knoxx.backend.contracts.resolve.default_actor_id = (function knoxx$backend$contracts$resolve$default_actor_id(config){
var configured = (function (){var G__33211 = new cljs.core.Keyword(null,"knoxx-default-actor-id","knoxx-default-actor-id",1539819560).cljs$core$IFn$_invoke$arity$1(config);
var G__33211__$1 = (((G__33211 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33211)));
var G__33211__$2 = (((G__33211__$1 == null))?null:clojure.string.trim(G__33211__$1));
if((G__33211__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33211__$2);
}
})();
var configured_actor = (cljs.core.truth_(configured)?knoxx.backend.contracts.resolve.resolve_actor(config,configured):null);
if(cljs.core.truth_(configured_actor)){
return configured;
} else {
var or__5142__auto__ = (function (){var G__33216 = knoxx.backend.contracts.resolve.actor_catalog(config);
var G__33216__$1 = (((G__33216 == null))?null:cljs.core.first(G__33216));
if((G__33216__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__33216__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "chat_primary";
}

}
});
knoxx.backend.contracts.resolve.combine_system_prompts = (function knoxx$backend$contracts$resolve$combine_system_prompts(var_args){
var args__5882__auto__ = [];
var len__5876__auto___33549 = arguments.length;
var i__5877__auto___33550 = (0);
while(true){
if((i__5877__auto___33550 < len__5876__auto___33549)){
args__5882__auto__.push((arguments[i__5877__auto___33550]));

var G__33551 = (i__5877__auto___33550 + (1));
i__5877__auto___33550 = G__33551;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((0) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((0)),(0),null)):null);
return knoxx.backend.contracts.resolve.combine_system_prompts.cljs$core$IFn$_invoke$arity$variadic(argseq__5883__auto__);
});

(knoxx.backend.contracts.resolve.combine_system_prompts.cljs$core$IFn$_invoke$arity$variadic = (function (parts){
var segments = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (part){
var G__33224 = part;
var G__33224__$1 = (((G__33224 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33224)));
var G__33224__$2 = (((G__33224__$1 == null))?null:clojure.string.trim(G__33224__$1));
if((G__33224__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33224__$2);
}
}),parts));
if(cljs.core.seq(segments)){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",segments);
} else {
return null;
}
}));

(knoxx.backend.contracts.resolve.combine_system_prompts.cljs$lang$maxFixedArity = (0));

/** @this {Function} */
(knoxx.backend.contracts.resolve.combine_system_prompts.cljs$lang$applyTo = (function (seq33219){
var self__5862__auto__ = this;
return self__5862__auto__.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq(seq33219));
}));

knoxx.backend.contracts.resolve.collect_role_tool_ids = (function knoxx$backend$contracts$resolve$collect_role_tool_ids(config,role_slugs){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (role_slug){
return knoxx.backend.contracts.roles.role_tool_ids(config,role_slug);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_role_slug,role_slugs)))], 0)))));
});
knoxx.backend.contracts.resolve.collect_capability_tool_ids = (function knoxx$backend$contracts$resolve$collect_capability_tool_ids(config,capability_ids){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (cap_id){
return knoxx.backend.contracts.roles.capability_tool_ids(config,cap_id);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_capability_ref,capability_ids)))], 0)))));
});
knoxx.backend.contracts.resolve.legacy_explicit_tool_ids = (function knoxx$backend$contracts$resolve$legacy_explicit_tool_ids(contract){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"tools","tools",-1241731990)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))));
});
knoxx.backend.contracts.resolve.contract_actor_capability_claims = (function knoxx$backend$contracts$resolve$contract_actor_capability_claims(contract){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = new cljs.core.Keyword("actor","capabilities","actor/capabilities",-198939954).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.Keyword(null,"capabilities","capabilities",212739361)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.contracts.resolve.resolve_agent_contract = (function knoxx$backend$contracts$resolve$resolve_agent_contract(var_args){
var G__33286 = arguments.length;
switch (G__33286) {
case 2:
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2 = (function (config,contract_id){
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,contract_id,null);
}));

(knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3 = (function (config,contract_id,actor_id){
var temp__5825__auto__ = (function (){var G__33297 = contract_id;
var G__33297__$1 = (((G__33297 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33297)));
var G__33297__$2 = (((G__33297__$1 == null))?null:clojure.string.trim(G__33297__$1));
if((G__33297__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33297__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var id = temp__5825__auto__;
var record = knoxx.backend.contracts.loader.find_contract_record_sync(config,"agents",id);
var contract0 = new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record);
var contract = (function (){var G__33302 = contract0;
if((G__33302 == null)){
return null;
} else {
return knoxx.backend.contracts.actor_scope.normalize_agent_contract(G__33302);
}
})();
if(cljs.core.truth_(contract)){
var enabled_QMARK_ = (!(new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(contract) === false));
var contract_actors = knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$1(contract);
var requested_actor_id = (function (){var G__33304 = actor_id;
var G__33304__$1 = (((G__33304 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33304)));
var G__33304__$2 = (((G__33304__$1 == null))?null:clojure.string.trim(G__33304__$1));
if((G__33304__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33304__$2);
}
})();
var allowed_for_request_QMARK_ = (function (){var or__5142__auto__ = (requested_actor_id == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.contracts.actor_scope.actor_allowed_QMARK_(contract_actors,requested_actor_id);
}
})();
var effective_actor_id = knoxx.backend.contracts.actor_scope.effective_actor_id(contract_actors,requested_actor_id,knoxx.backend.contracts.resolve.default_actor_id(config));
if(cljs.core.truth_(allowed_for_request_QMARK_)){
var actor_spec = (function (){var or__5142__auto__ = (cljs.core.truth_(effective_actor_id)?knoxx.backend.contracts.resolve.resolve_actor(config,effective_actor_id):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto____$1 = knoxx.backend.contracts.resolve.default_actor_id(config);
if(cljs.core.truth_(temp__5825__auto____$1)){
var default_id = temp__5825__auto____$1;
return knoxx.backend.contracts.resolve.resolve_actor(config,default_id);
} else {
return null;
}
}
})();
var contract_role_slugs = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_role_slug,knoxx.backend.contracts.actor_scope.agent_role_claims(contract)))));
var actor_role_slugs = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(actor_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var role_slugs = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(actor_role_slugs,contract_role_slugs)));
var actor_capability_ids = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"capability-ids","capability-ids",-1477528817).cljs$core$IFn$_invoke$arity$1(actor_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var contract_capability_ids = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.keywordish__GT_capability_ref,knoxx.backend.contracts.resolve.contract_actor_capability_claims(contract)))));
var capability_ids = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(actor_capability_ids,contract_capability_ids)));
var role_tool_ids = knoxx.backend.contracts.resolve.collect_role_tool_ids(config,role_slugs);
var capability_tool_ids = knoxx.backend.contracts.resolve.collect_capability_tool_ids(config,capability_ids);
var explicit_tool_ids = knoxx.backend.contracts.resolve.legacy_explicit_tool_ids(contract);
var tool_ids = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(role_tool_ids,capability_tool_ids,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([explicit_tool_ids], 0)))));
var tool_policies = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null);
}),tool_ids);
var primary_role = (function (){var or__5142__auto__ = cljs.core.first(contract_role_slugs);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.first(actor_role_slugs);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"knoxx-default-role","knoxx-default-role",1668482524).cljs$core$IFn$_invoke$arity$1(config);
}
}
})();
var role_system_prompt_text = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__33276_SHARP_){
var G__33347 = knoxx.backend.contracts.roles.role_system_prompt(config,p1__33276_SHARP_);
var G__33347__$1 = (((G__33347 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33347)));
var G__33347__$2 = (((G__33347__$1 == null))?null:clojure.string.trim(G__33347__$1));
if((G__33347__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33347__$2);
}
}),role_slugs)));
var agent_system_prompt = (function (){var G__33355 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"system","system",-29381724)], null));
var G__33355__$1 = (((G__33355 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33355)));
var G__33355__$2 = (((G__33355__$1 == null))?null:clojure.string.trim(G__33355__$1));
if((G__33355__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33355__$2);
}
})();
var system_prompt = knoxx.backend.contracts.resolve.combine_system_prompts.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([role_system_prompt_text,new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(actor_spec),agent_system_prompt], 0));
var all_extras = knoxx.backend.contracts.resolve.all_contract_extras(config,actor_spec,role_slugs,capability_ids,contract);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"role","role",-736691072),new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817),new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082),new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716),new cljs.core.Keyword(null,"tool-ids","tool-ids",-1358371034),new cljs.core.Keyword(null,"contract","contract",798152745),new cljs.core.Keyword(null,"actor-kind","actor-kind",42051561),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557),new cljs.core.Keyword(null,"actor-id","actor-id",897721067),new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049),new cljs.core.Keyword(null,"capability-ids","capability-ids",-1477528817),new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783),new cljs.core.Keyword(null,"actor-role-slugs","actor-role-slugs",1597094544),new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429),new cljs.core.Keyword(null,"agent-system-prompt","agent-system-prompt",-1576864491),new cljs.core.Keyword(null,"role-system-prompt","role-system-prompt",-1946535526),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),new cljs.core.Keyword(null,"actor-system-prompt","actor-system-prompt",-1563106020),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"extras","extras",-1110348066),new cljs.core.Keyword(null,"model","model",331153215)],[primary_role,knoxx.backend.contracts.actor_scope.actor_claims__GT_wire(contract_actors),knoxx.backend.contracts.resolve.memory_hydration_from_contract(contract),(function (){var G__33369 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"prompts","prompts",15471501),new cljs.core.Keyword(null,"task","task",-1476607993)], null));
var G__33369__$1 = (((G__33369 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33369)));
var G__33369__$2 = (((G__33369__$1 == null))?null:clojure.string.trim(G__33369__$1));
if((G__33369__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33369__$2);
}
})(),tool_ids,contract,new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(actor_spec),role_slugs,knoxx.backend.contracts.resolve.context_policy_from_contract(contract),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec),contract_actors,capability_ids,(function (){var G__33375 = new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783).cljs$core$IFn$_invoke$arity$1(contract);
if((G__33375 == null)){
return null;
} else {
return knoxx.backend.contracts.resolve.keywordish__GT_role_slug(G__33375);
}
})(),actor_role_slugs,(function (){var G__33376 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"thinking","thinking",2063777387)], null));
if((G__33376 == null)){
return null;
} else {
return knoxx.backend.contracts.resolve.keywordish__GT_role_slug(G__33376);
}
})(),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record),system_prompt,agent_system_prompt,role_system_prompt_text,tool_policies,new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(actor_spec),enabled_QMARK_,all_extras,(function (){var G__33381 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"model","model",331153215)], null));
var G__33381__$1 = (((G__33381 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33381)));
var G__33381__$2 = (((G__33381__$1 == null))?null:clojure.string.trim(G__33381__$1));
if((G__33381__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33381__$2);
}
})()]);
} else {
return null;
}
} else {
return null;
}
} else {
return null;
}
}));

(knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$lang$maxFixedArity = 3);

knoxx.backend.contracts.resolve.manual_agent_contract_QMARK_ = (function knoxx$backend$contracts$resolve$manual_agent_contract_QMARK_(entry){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("manual",(function (){var G__33386 = new cljs.core.Keyword(null,"trigger-kind","trigger-kind",1773988783).cljs$core$IFn$_invoke$arity$1(entry);
var G__33386__$1 = (((G__33386 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33386)));
var G__33386__$2 = (((G__33386__$1 == null))?null:clojure.string.trim(G__33386__$1));
if((G__33386__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__33386__$2);
}
})());
});
knoxx.backend.contracts.resolve.agent_contract_catalog = (function knoxx$backend$contracts$resolve$agent_contract_catalog(var_args){
var G__33397 = arguments.length;
switch (G__33397) {
case 1:
return knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2(config,null);
}));

(knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2 = (function (config,actor_id){
var wanted_actor_id = (function (){var G__33400 = actor_id;
var G__33400__$1 = (((G__33400 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33400)));
var G__33400__$2 = (((G__33400__$1 == null))?null:clojure.string.trim(G__33400__$1));
if((G__33400__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33400__$2);
}
})();
return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(knoxx.backend.contracts.resolve.manual_agent_contract_QMARK_,cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"enabled","enabled",1195909756),cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (record){
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record),wanted_actor_id);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33388_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("agents",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__33388_SHARP_));
}),knoxx.backend.contracts.loader.load_all_contracts_sync(config))))))));
}));

(knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$lang$maxFixedArity = 2);

knoxx.backend.contracts.resolve.default_agent_contract_id = (function knoxx$backend$contracts$resolve$default_agent_contract_id(var_args){
var G__33425 = arguments.length;
switch (G__33425) {
case 1:
return knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$1 = (function (config){
return knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,null);
}));

(knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2 = (function (config,actor_id){
var actor_spec = (function (){var or__5142__auto__ = (cljs.core.truth_(actor_id)?knoxx.backend.contracts.resolve.resolve_actor(config,actor_id):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = knoxx.backend.contracts.resolve.default_actor_id(config);
if(cljs.core.truth_(temp__5825__auto__)){
var default_id = temp__5825__auto__;
return knoxx.backend.contracts.resolve.resolve_actor(config,default_id);
} else {
return null;
}
}
})();
var actor_default = (function (){var G__33431 = new cljs.core.Keyword(null,"default-agent","default-agent",279723152).cljs$core$IFn$_invoke$arity$1(actor_spec);
var G__33431__$1 = (((G__33431 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33431)));
var G__33431__$2 = (((G__33431__$1 == null))?null:clojure.string.trim(G__33431__$1));
if((G__33431__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33431__$2);
}
})();
var configured = (function (){var G__33432 = new cljs.core.Keyword(null,"knoxx-default-agent-contract","knoxx-default-agent-contract",-620088071).cljs$core$IFn$_invoke$arity$1(config);
var G__33432__$1 = (((G__33432 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33432)));
var G__33432__$2 = (((G__33432__$1 == null))?null:clojure.string.trim(G__33432__$1));
if((G__33432__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33432__$2);
}
})();
var configured_manual = (cljs.core.truth_(configured)?knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,configured,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec)):null);
var actor_default_manual = (cljs.core.truth_(actor_default)?knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,actor_default,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec)):null);
var actor_catalog = knoxx.backend.contracts.resolve.agent_contract_catalog.cljs$core$IFn$_invoke$arity$2(config,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec));
if(cljs.core.truth_((function (){var and__5140__auto__ = actor_default_manual;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.contracts.resolve.manual_agent_contract_QMARK_(actor_default_manual);
} else {
return and__5140__auto__;
}
})())){
return actor_default;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = configured_manual;
if(cljs.core.truth_(and__5140__auto__)){
return ((knoxx.backend.contracts.resolve.manual_agent_contract_QMARK_(configured_manual)) && ((((actor_spec == null)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(actor_spec),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(configured_manual))))));
} else {
return and__5140__auto__;
}
})())){
return configured;
} else {
var G__33433 = actor_catalog;
var G__33433__$1 = (((G__33433 == null))?null:cljs.core.first(G__33433));
if((G__33433__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__33433__$1);
}

}
}
}));

(knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$lang$maxFixedArity = 2);

knoxx.backend.contracts.resolve.effective_agent_contract = (function knoxx$backend$contracts$resolve$effective_agent_contract(var_args){
var G__33435 = arguments.length;
switch (G__33435) {
case 2:
return knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$2 = (function (config,requested_contract_id){
return knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$3(config,requested_contract_id,null);
}));

(knoxx.backend.contracts.resolve.effective_agent_contract.cljs$core$IFn$_invoke$arity$3 = (function (config,requested_contract_id,actor_id){
var or__5142__auto__ = (cljs.core.truth_(requested_contract_id)?knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,requested_contract_id,actor_id):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var temp__5825__auto__ = knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,actor_id);
if(cljs.core.truth_(temp__5825__auto__)){
var actor_default_id = temp__5825__auto__;
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,actor_default_id,actor_id);
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var temp__5825__auto__ = knoxx.backend.contracts.resolve.default_agent_contract_id.cljs$core$IFn$_invoke$arity$2(config,null);
if(cljs.core.truth_(temp__5825__auto__)){
var global_default_id = temp__5825__auto__;
return knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,global_default_id,actor_id);
} else {
return null;
}
}
}
}));

(knoxx.backend.contracts.resolve.effective_agent_contract.cljs$lang$maxFixedArity = 3);

/**
 * Resolve contract-declared UI actions for an actor and optional surface.
 * Actor actions are listed before default-agent actions; disabled actions are
 * omitted. This is intentionally a render contract, not an execution contract.
 */
knoxx.backend.contracts.resolve.ui_actions_for_actor = (function knoxx$backend$contracts$resolve$ui_actions_for_actor(config,actor_id,surface){
var effective_actor_id = (function (){var or__5142__auto__ = (function (){var G__33448 = actor_id;
var G__33448__$1 = (((G__33448 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33448)));
var G__33448__$2 = (((G__33448__$1 == null))?null:clojure.string.trim(G__33448__$1));
if((G__33448__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33448__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.contracts.resolve.default_actor_id(config);
}
})();
var actor_spec = knoxx.backend.contracts.resolve.resolve_actor(config,effective_actor_id);
var actor_actions = cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__33440_SHARP_){
return knoxx.backend.contracts.resolve.normalize_ui_action("actor",p1__33440_SHARP_);
}),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(actor_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.Keyword("ui","actions","ui/actions",-812652422)], null)));
var default_agent_id = (function (){var G__33452 = new cljs.core.Keyword(null,"default-agent","default-agent",279723152).cljs$core$IFn$_invoke$arity$1(actor_spec);
var G__33452__$1 = (((G__33452 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33452)));
var G__33452__$2 = (((G__33452__$1 == null))?null:clojure.string.trim(G__33452__$1));
if((G__33452__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33452__$2);
}
})();
var agent_spec = (cljs.core.truth_(default_agent_id)?knoxx.backend.contracts.resolve.resolve_agent_contract.cljs$core$IFn$_invoke$arity$3(config,default_agent_id,effective_actor_id):null);
var agent_actions = cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__33442_SHARP_){
return knoxx.backend.contracts.resolve.normalize_ui_action("agent",p1__33442_SHARP_);
}),cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(agent_spec,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contract","contract",798152745),new cljs.core.Keyword("ui","actions","ui/actions",-812652422)], null)));
var actions = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__33446_SHARP_){
return knoxx.backend.contracts.resolve.action_matches_surface_QMARK_(surface,p1__33446_SHARP_);
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__33445_SHARP_){
return knoxx.backend.contracts.resolve.enrich_ui_action(config,p1__33445_SHARP_);
}),cljs.core.concat.cljs$core$IFn$_invoke$arity$2(actor_actions,agent_actions))));
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),effective_actor_id,new cljs.core.Keyword(null,"surface","surface",699915646),(function (){var G__33453 = surface;
var G__33453__$1 = (((G__33453 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__33453)));
var G__33453__$2 = (((G__33453__$1 == null))?null:clojure.string.trim(G__33453__$1));
if((G__33453__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__33453__$2);
}
})(),new cljs.core.Keyword(null,"default-agent-id","default-agent-id",-2135472358),default_agent_id,new cljs.core.Keyword(null,"actions","actions",-812656882),actions], null);
});

//# sourceMappingURL=knoxx.backend.contracts.resolve.js.map
