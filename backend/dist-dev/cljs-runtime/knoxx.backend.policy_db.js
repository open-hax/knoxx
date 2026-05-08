import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./honey.sql.js";
import "./knoxx.backend.contracts.loader.js";
import "./knoxx.backend.contracts.roles.js";
import "./knoxx.backend.policy_db.actors.js";
import "./knoxx.backend.policy.protocol.js";
import "./knoxx.backend.policy.sql_adapter.js";
import "./knoxx.backend.tools.registry.js";
import "./shadow.esm.esm_import$pg.js";
import "./shadow.esm.esm_import$node_fs.js";
import "./shadow.esm.esm_import$node_path.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.policy_db');






knoxx.backend.policy_db.default_contracts_dir = (function knoxx$backend$policy_db$default_contracts_dir(){
var configured = (function (){var G__65551 = (process.env["CONTRACTS_DIR"]);
var G__65551__$1 = (((G__65551 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65551)));
var G__65551__$2 = (((G__65551__$1 == null))?null:clojure.string.trim(G__65551__$1));
if((G__65551__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65551__$2);
}
})();
var cwd = process.cwd();
var candidates = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__65550_SHARP_){
return shadow.esm.esm_import$node_path.resolve(cwd,p1__65550_SHARP_);
}),cljs.core.keep.cljs$core$IFn$_invoke$arity$2(cljs.core.identity,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [configured,"contracts","../contracts","packages/agents/knoxx/contracts","orgs/open-hax/openplanner/packages/agents/knoxx/contracts"], null)))));
var or__5142__auto__ = cljs.core.some((function (candidate){
if(cljs.core.truth_(shadow.esm.esm_import$node_fs.existsSync(candidate))){
return candidate;
} else {
return null;
}
}),candidates);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return shadow.esm.esm_import$node_path.resolve(cwd,(function (){var or__5142__auto____$1 = configured;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "contracts";
}
})());
}
});
knoxx.backend.policy_db.contracts_dir = (function knoxx$backend$policy_db$contracts_dir(){
return knoxx.backend.policy_db.default_contracts_dir();
});
knoxx.backend.policy_db.normalize_email = (function knoxx$backend$policy_db$normalize_email(value){
var G__65552 = value;
var G__65552__$1 = (((G__65552 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65552)));
var G__65552__$2 = (((G__65552__$1 == null))?null:clojure.string.trim(G__65552__$1));
var G__65552__$3 = (((G__65552__$2 == null))?null:clojure.string.lower_case(G__65552__$2));
if((G__65552__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__65552__$3);
}
});
knoxx.backend.policy_db.user_actor_id_from_email = (function knoxx$backend$policy_db$user_actor_id_from_email(email){
return knoxx.backend.policy_db.actors.user_actor_id_from_email(email);
});
knoxx.backend.policy_db.actor_email_from_id = (function knoxx$backend$policy_db$actor_email_from_id(actor_id){
return knoxx.backend.policy_db.actors.actor_email_from_id(actor_id);
});
knoxx.backend.policy_db.upsert_actor_contract_BANG_ = (function knoxx$backend$policy_db$upsert_actor_contract_BANG_(payload){
return knoxx.backend.policy_db.actors.upsert_actor_contract_BANG_(knoxx.backend.policy_db.contracts_dir(),payload);
});
knoxx.backend.policy_db.find_user_actor_contract_by_email = (function knoxx$backend$policy_db$find_user_actor_contract_by_email(email){
return knoxx.backend.policy_db.actors.find_user_actor_contract_by_email(knoxx.backend.policy_db.contracts_dir(),email);
});
knoxx.backend.policy_db.find_actor_contract_by_id = (function knoxx$backend$policy_db$find_actor_contract_by_id(actor_id){
return knoxx.backend.policy_db.actors.find_actor_contract_by_id(knoxx.backend.policy_db.contracts_dir(),actor_id);
});
knoxx.backend.policy_db.list_actor_contracts = (function knoxx$backend$policy_db$list_actor_contracts(){
return knoxx.backend.policy_db.actors.list_actor_contracts(knoxx.backend.policy_db.contracts_dir());
});
knoxx.backend.policy_db.find_org_by_id = (function knoxx$backend$policy_db$find_org_by_id(pool,org_id){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
return Promise.resolve(null);
} else {
var G__65553 = pool;
var G__65554 = "SELECT * FROM orgs WHERE id = $1::uuid";
var G__65555 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null);
return (knoxx.backend.policy_db.query_one_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_one_BANG_.cljs$core$IFn$_invoke$arity$3(G__65553,G__65554,G__65555) : knoxx.backend.policy_db.query_one_BANG_.call(null,G__65553,G__65554,G__65555));
}
});
knoxx.backend.policy_db.sql_policy_store = (function knoxx$backend$policy_db$sql_policy_store(pool,primary_org){
return knoxx.backend.policy.sql_adapter.create_store(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"query-one!","query-one!",373666417),(function (sql_str,params){
return (knoxx.backend.policy_db.query_one_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_one_BANG_.cljs$core$IFn$_invoke$arity$3(pool,sql_str,params) : knoxx.backend.policy_db.query_one_BANG_.call(null,pool,sql_str,params));
}),new cljs.core.Keyword(null,"query!","query!",1326722454),(function (sql_str,params){
return (knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3(pool,sql_str,params) : knoxx.backend.policy_db.query_BANG_.call(null,pool,sql_str,params));
}),new cljs.core.Keyword(null,"find-org-by-slug!","find-org-by-slug!",2122494329),(function (org_slug){
return (knoxx.backend.policy_db.find_org_by_slug.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.policy_db.find_org_by_slug.cljs$core$IFn$_invoke$arity$2(pool,org_slug) : knoxx.backend.policy_db.find_org_by_slug.call(null,pool,org_slug));
}),new cljs.core.Keyword(null,"set-membership-roles!","set-membership-roles!",668277417),(function (membership_id,opts){
return (knoxx.backend.policy_db.set_membership_roles_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.set_membership_roles_BANG_.cljs$core$IFn$_invoke$arity$3(pool,membership_id,opts) : knoxx.backend.policy_db.set_membership_roles_BANG_.call(null,pool,membership_id,opts));
}),new cljs.core.Keyword(null,"primary-org","primary-org",-717687488),primary_org], null));
});
knoxx.backend.policy_db.contract_tool_ids = (function knoxx$backend$policy_db$contract_tool_ids(){
return knoxx.backend.policy_db.actors.contract_tool_ids(knoxx.backend.policy_db.contracts_dir());
});
knoxx.backend.policy_db.contracts_config = (function knoxx$backend$policy_db$contracts_config(){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"contracts-dir","contracts-dir",220735735),knoxx.backend.policy_db.default_contracts_dir()], null);
});
knoxx.backend.policy_db.basic_user_chat_policy_constraints = (function knoxx$backend$policy_db$basic_user_chat_policy_constraints(){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"allowedModels","allowedModels",-660080636),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["gemma4:31b"], null),new cljs.core.Keyword(null,"maxRequests","maxRequests",967196018),(20),new cljs.core.Keyword(null,"windowSeconds","windowSeconds",-573461410),(600)], null);
});
knoxx.backend.policy_db.slugify = (function knoxx$backend$policy_db$slugify(value,fallback){
var slug = clojure.string.replace(clojure.string.replace(clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)+""))),/[^a-z0-9]+/,"-"),/^[-]+|[-]+$/,"");
if(clojure.string.blank_QMARK_(slug)){
return fallback;
} else {
return slug;
}
});
knoxx.backend.policy_db.unique = (function knoxx$backend$policy_db$unique(values){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,values)));
});
knoxx.backend.policy_db.normalize_actor_id = (function knoxx$backend$policy_db$normalize_actor_id(value){
var G__65556 = value;
var G__65556__$1 = (((G__65556 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65556)));
var G__65556__$2 = (((G__65556__$1 == null))?null:clojure.string.trim(G__65556__$1));
if((G__65556__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65556__$2);
}
});
knoxx.backend.policy_db.default_membership_actor_id = (function knoxx$backend$policy_db$default_membership_actor_id(role_slugs){
var normalized = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.comp.cljs$core$IFn$_invoke$arity$3(cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (p1__65557_SHARP_){
var G__65559 = p1__65557_SHARP_;
var G__65559__$1 = (((G__65559 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65559)));
var G__65559__$2 = (((G__65559__$1 == null))?null:clojure.string.trim(G__65559__$1));
if((G__65559__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65559__$2);
}
})),cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (p1__65558_SHARP_){
if(cljs.core.truth_(p1__65558_SHARP_)){
return knoxx.backend.contracts.roles.normalize_role(knoxx.backend.policy_db.contracts_config(),p1__65558_SHARP_);
} else {
return null;
}
})),cljs.core.remove.cljs$core$IFn$_invoke$arity$1(cljs.core.nil_QMARK_)),(function (){var or__5142__auto__ = role_slugs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
if(cljs.core.contains_QMARK_(normalized,"system_admin")){
return "system_admin";
} else {
return "workspace_user";
}
});
knoxx.backend.policy_db.set_membership_actor_id_BANG_ = (function knoxx$backend$policy_db$set_membership_actor_id_BANG_(pool,membership_id,actor_id){
var resolved = (function (){var or__5142__auto__ = knoxx.backend.policy_db.normalize_actor_id(actor_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "workspace_user";
}
})();
return (function (){var G__65560 = pool;
var G__65561 = "UPDATE memberships SET actor_id = $2, updated_at = NOW() WHERE id = $1::uuid";
var G__65562 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id,resolved], null);
return (knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3(G__65560,G__65561,G__65562) : knoxx.backend.policy_db.query_BANG_.call(null,G__65560,G__65561,G__65562));
})().then((function (_){
return resolved;
}));
});
knoxx.backend.policy_db.backfill_membership_actors_BANG_ = (function knoxx$backend$policy_db$backfill_membership_actors_BANG_(pool){
return (function (){var G__65563 = pool;
var G__65564 = "UPDATE memberships m\n               SET actor_id = CASE\n                 WHEN EXISTS (\n                   SELECT 1\n                   FROM membership_roles mr\n                   JOIN roles r ON r.id = mr.role_id\n                   WHERE mr.membership_id = m.id\n                     AND r.slug = 'system_admin'\n                 ) THEN 'system_admin'\n                 ELSE 'workspace_user'\n               END,\n               updated_at = NOW()\n               WHERE COALESCE(NULLIF(trim(actor_id), ''), '') = ''";
var G__65565 = cljs.core.PersistentVector.EMPTY;
return (knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3(G__65563,G__65564,G__65565) : knoxx.backend.policy_db.query_BANG_.call(null,G__65563,G__65564,G__65565));
})().then((function (_){
return null;
}));
});
knoxx.backend.policy_db.sync_user_from_actor_contract_BANG_ = (function knoxx$backend$policy_db$sync_user_from_actor_contract_BANG_(pool,primary_org,payload){
var requested_actor_id = knoxx.backend.policy_db.normalize_actor_id((function (){var or__5142__auto__ = (payload["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["actor_id"]);
}
})());
var email = knoxx.backend.policy_db.normalize_email((payload["email"]));
if(cljs.core.not((function (){var or__5142__auto__ = email;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return requested_actor_id;
}
})())){
return Promise.resolve(null);
} else {
var temp__5823__auto__ = (function (){var or__5142__auto__ = knoxx.backend.policy_db.find_actor_contract_by_id(requested_actor_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.find_user_actor_contract_by_email(email);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var actor_contract = temp__5823__auto__;
return knoxx.backend.policy.protocol.sync_actor_projections_BANG_(knoxx.backend.policy_db.sql_policy_store(pool,primary_org),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481).cljs$core$IFn$_invoke$arity$1(actor_contract)], null));
} else {
return Promise.resolve(null);
}
}
});
knoxx.backend.policy_db.sync_user_actors_from_contracts_BANG_ = (function knoxx$backend$policy_db$sync_user_actors_from_contracts_BANG_(pool,primary_org){
return knoxx.backend.policy.protocol.sync_actor_projections_BANG_(knoxx.backend.policy_db.sql_policy_store(pool,primary_org),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"actor","actor",-1830560481),knoxx.backend.policy_db.list_actor_contracts()));
});
knoxx.backend.policy_db.normalize_tool_policy = (function knoxx$backend$policy_db$normalize_tool_policy(policy){
if(typeof policy === 'string'){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),policy,new cljs.core.Keyword(null,"effect","effect",347343289),"allow",new cljs.core.Keyword(null,"constraints","constraints",422775616),cljs.core.PersistentArrayMap.EMPTY], null);
} else {
var tool_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"tool_id","tool_id",1550520216).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(policy);
}
}
})();
if(cljs.core.truth_(tool_id)){
} else {
throw (new Error("toolId is required for tool policy"));
}

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),knoxx.backend.tools.registry.normalize_tool_id(tool_id),new cljs.core.Keyword(null,"effect","effect",347343289),((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy),"deny"))?"deny":"allow"),new cljs.core.Keyword(null,"constraints","constraints",422775616),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"constraints_json","constraints_json",1549946864).cljs$core$IFn$_invoke$arity$1(policy);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
})()], null);

}
});
/**
 * Return a stable, sorted list of tool ids that should exist in tool_definitions.
 * 
 * We seed from the runtime registry AND from contract capabilities so that
 * contract-driven role policies can't fail on FK constraints when a tool id
 * appears in contracts before it is added to the runtime tool registry (or
 * when different nodes deploy slightly different registries).
 */
knoxx.backend.policy_db.seed_tool_ids = (function knoxx$backend$policy_db$seed_tool_ids(){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,cljs.core.concat.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.known_tool_ids(),cljs.core.seq(knoxx.backend.policy_db.contract_tool_ids()))))));
});
/**
 * Upsert tool definitions so FK constraints on *_tool_policies can't fail.
 * 
 * Accepts a seq of tool-id strings (or keywords/symbols).
 * This intentionally tolerates unknown tools by inserting a placeholder.
 * Unknown tool ids may originate from contracts, UI experiments, or mixed-version
 * deployments.
 */
knoxx.backend.policy_db.ensure_tool_definitions_BANG_ = (function knoxx$backend$policy_db$ensure_tool_definitions_BANG_(pool,tool_ids){
var ids = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,tool_ids)));
if(cljs.core.empty_QMARK_(ids)){
return Promise.resolve(null);
} else {
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$ensure_tool_definitions_BANG__$_iter__65566(s__65567){
return (new cljs.core.LazySeq(null,(function (){
var s__65567__$1 = s__65567;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65567__$1);
if(temp__5825__auto__){
var s__65567__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65567__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65567__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65569 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65568 = (0);
while(true){
if((i__65568 < size__5627__auto__)){
var tool_id = cljs.core._nth(c__5626__auto__,i__65568);
cljs.core.chunk_append(b__65569,(function (){var map__65570 = knoxx.backend.tools.registry.get_tool(tool_id);
var map__65570__$1 = cljs.core.__destructure_map(map__65570);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65570__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65570__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
var risk_level = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65570__$1,new cljs.core.Keyword(null,"risk-level","risk-level",658496607));
var G__65571 = pool;
var G__65572 = "INSERT INTO tool_definitions (id, label, description, risk_level)\n                         VALUES ($1, $2, $3, $4)\n                         ON CONFLICT (id) DO UPDATE\n                         SET label = EXCLUDED.label,\n                             description = EXCLUDED.description,\n                             risk_level = EXCLUDED.risk_level";
var G__65573 = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [tool_id,(function (){var or__5142__auto__ = label;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})(),(function (){var or__5142__auto__ = description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = risk_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "low";
}
})()], null);
return (knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3(G__65571,G__65572,G__65573) : knoxx.backend.policy_db.query_BANG_.call(null,G__65571,G__65572,G__65573));
})());

var G__65734 = (i__65568 + (1));
i__65568 = G__65734;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65569),knoxx$backend$policy_db$ensure_tool_definitions_BANG__$_iter__65566(cljs.core.chunk_rest(s__65567__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65569),null);
}
} else {
var tool_id = cljs.core.first(s__65567__$2);
return cljs.core.cons((function (){var map__65574 = knoxx.backend.tools.registry.get_tool(tool_id);
var map__65574__$1 = cljs.core.__destructure_map(map__65574);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65574__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65574__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
var risk_level = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65574__$1,new cljs.core.Keyword(null,"risk-level","risk-level",658496607));
var G__65575 = pool;
var G__65576 = "INSERT INTO tool_definitions (id, label, description, risk_level)\n                         VALUES ($1, $2, $3, $4)\n                         ON CONFLICT (id) DO UPDATE\n                         SET label = EXCLUDED.label,\n                             description = EXCLUDED.description,\n                             risk_level = EXCLUDED.risk_level";
var G__65577 = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [tool_id,(function (){var or__5142__auto__ = label;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})(),(function (){var or__5142__auto__ = description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = risk_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "low";
}
})()], null);
return (knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3 ? knoxx.backend.policy_db.query_BANG_.cljs$core$IFn$_invoke$arity$3(G__65575,G__65576,G__65577) : knoxx.backend.policy_db.query_BANG_.call(null,G__65575,G__65576,G__65577));
})(),knoxx$backend$policy_db$ensure_tool_definitions_BANG__$_iter__65566(cljs.core.rest(s__65567__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(ids);
})())).then((function (_){
return null;
}));
}
});
knoxx.backend.policy_db.normalize_lake_config = (function knoxx$backend$policy_db$normalize_lake_config(config){
if(cljs.core.truth_((function (){var or__5142__auto__ = (config == null);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (!(cljs.core.object_QMARK_(config)));
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
return cljs.core.array_QMARK_(config);
}
}
})())){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(config,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}
});
knoxx.backend.policy_db.http_error = (function knoxx$backend$policy_db$http_error(status_code,message,code){
var err = (new Error(message));
(err["statusCode"] = status_code);

(err["code"] = code);

return err;
});
knoxx.backend.policy_db.header_value = (function knoxx$backend$policy_db$header_value(headers_like,name){
if(cljs.core.truth_(headers_like)){
if(cljs.core.fn_QMARK_((headers_like["get"]))){
return clojure.string.trim((function (){var or__5142__auto__ = headers_like.get(name);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = headers_like.get(clojure.string.lower_case(name));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})());
} else {
return clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (headers_like[name]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (headers_like[clojure.string.lower_case(name)]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())));

}
} else {
return null;
}
});
knoxx.backend.policy_db.merge_toolPolicies = (function knoxx$backend$policy_db$merge_toolPolicies(role_policies,membership_policies){
var merged = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var seq__65578_65735 = cljs.core.seq(role_policies);
var chunk__65579_65736 = null;
var count__65580_65737 = (0);
var i__65581_65738 = (0);
while(true){
if((i__65581_65738 < count__65580_65737)){
var policy_65739 = chunk__65579_65736.cljs$core$IIndexed$_nth$arity$2(null,i__65581_65738);
var normalized_65741 = knoxx.backend.policy_db.normalize_tool_policy(policy_65739);
var tool_id_65742 = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(normalized_65741);
var existing_65743 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(merged),tool_id_65742);
if((((existing_65743 == null)) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(normalized_65741),"deny")) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(existing_65743),"deny")))))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(merged,cljs.core.assoc,tool_id_65742,normalized_65741);
} else {
}


var G__65744 = seq__65578_65735;
var G__65745 = chunk__65579_65736;
var G__65746 = count__65580_65737;
var G__65747 = (i__65581_65738 + (1));
seq__65578_65735 = G__65744;
chunk__65579_65736 = G__65745;
count__65580_65737 = G__65746;
i__65581_65738 = G__65747;
continue;
} else {
var temp__5825__auto___65748 = cljs.core.seq(seq__65578_65735);
if(temp__5825__auto___65748){
var seq__65578_65749__$1 = temp__5825__auto___65748;
if(cljs.core.chunked_seq_QMARK_(seq__65578_65749__$1)){
var c__5673__auto___65750 = cljs.core.chunk_first(seq__65578_65749__$1);
var G__65751 = cljs.core.chunk_rest(seq__65578_65749__$1);
var G__65752 = c__5673__auto___65750;
var G__65753 = cljs.core.count(c__5673__auto___65750);
var G__65754 = (0);
seq__65578_65735 = G__65751;
chunk__65579_65736 = G__65752;
count__65580_65737 = G__65753;
i__65581_65738 = G__65754;
continue;
} else {
var policy_65755 = cljs.core.first(seq__65578_65749__$1);
var normalized_65756 = knoxx.backend.policy_db.normalize_tool_policy(policy_65755);
var tool_id_65757 = new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(normalized_65756);
var existing_65758 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(merged),tool_id_65757);
if((((existing_65758 == null)) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(normalized_65756),"deny")) || (cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(existing_65758),"deny")))))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(merged,cljs.core.assoc,tool_id_65757,normalized_65756);
} else {
}


var G__65759 = cljs.core.next(seq__65578_65749__$1);
var G__65760 = null;
var G__65761 = (0);
var G__65762 = (0);
seq__65578_65735 = G__65759;
chunk__65579_65736 = G__65760;
count__65580_65737 = G__65761;
i__65581_65738 = G__65762;
continue;
}
} else {
}
}
break;
}

var seq__65582_65763 = cljs.core.seq(membership_policies);
var chunk__65583_65764 = null;
var count__65584_65765 = (0);
var i__65585_65766 = (0);
while(true){
if((i__65585_65766 < count__65584_65765)){
var policy_65767 = chunk__65583_65764.cljs$core$IIndexed$_nth$arity$2(null,i__65585_65766);
var normalized_65768 = knoxx.backend.policy_db.normalize_tool_policy(policy_65767);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(merged,cljs.core.assoc,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(normalized_65768),normalized_65768);


var G__65769 = seq__65582_65763;
var G__65770 = chunk__65583_65764;
var G__65771 = count__65584_65765;
var G__65772 = (i__65585_65766 + (1));
seq__65582_65763 = G__65769;
chunk__65583_65764 = G__65770;
count__65584_65765 = G__65771;
i__65585_65766 = G__65772;
continue;
} else {
var temp__5825__auto___65773 = cljs.core.seq(seq__65582_65763);
if(temp__5825__auto___65773){
var seq__65582_65774__$1 = temp__5825__auto___65773;
if(cljs.core.chunked_seq_QMARK_(seq__65582_65774__$1)){
var c__5673__auto___65775 = cljs.core.chunk_first(seq__65582_65774__$1);
var G__65776 = cljs.core.chunk_rest(seq__65582_65774__$1);
var G__65777 = c__5673__auto___65775;
var G__65778 = cljs.core.count(c__5673__auto___65775);
var G__65779 = (0);
seq__65582_65763 = G__65776;
chunk__65583_65764 = G__65777;
count__65584_65765 = G__65778;
i__65585_65766 = G__65779;
continue;
} else {
var policy_65780 = cljs.core.first(seq__65582_65774__$1);
var normalized_65781 = knoxx.backend.policy_db.normalize_tool_policy(policy_65780);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(merged,cljs.core.assoc,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(normalized_65781),normalized_65781);


var G__65782 = cljs.core.next(seq__65582_65774__$1);
var G__65783 = null;
var G__65784 = (0);
var G__65785 = (0);
seq__65582_65763 = G__65782;
chunk__65583_65764 = G__65783;
count__65584_65765 = G__65784;
i__65585_65766 = G__65785;
continue;
}
} else {
}
}
break;
}

return cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),cljs.core.vals(cljs.core.deref(merged))));
});
knoxx.backend.policy_db.rolePriority = (function knoxx$backend$policy_db$rolePriority(slug){
var G__65586 = slug;
switch (G__65586) {
case "system_admin":
return (100);

break;
case "org_admin":
return (90);

break;
case "developer":
return (80);

break;
case "data_analyst":
return (70);

break;
case "knowledge_worker":
return (60);

break;
default:
return (0);

}
});
knoxx.backend.policy_db.permission_row_shape = (function knoxx$backend$policy_db$permission_row_shape(code){
var parts = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code)),/\./)));
var action = (function (){var or__5142__auto__ = cljs.core.peek(parts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "use";
}
})();
var resource_kind = (function (){var or__5142__auto__ = (function (){var G__65587 = cljs.core.butlast(parts);
var G__65587__$1 = (((G__65587 == null))?null:cljs.core.seq(G__65587));
if((G__65587__$1 == null)){
return null;
} else {
return clojure.string.join.cljs$core$IFn$_invoke$arity$2("_",G__65587__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "permission";
}
})();
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"code","code",1586293142),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code)),new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),resource_kind,new cljs.core.Keyword(null,"action","action",-811238024),action,new cljs.core.Keyword(null,"description","description",-1428560544),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(code))], null);
});
/**
 * Execute a parameterized SQL query. Returns Promise resolving to {:rows [...]}.
 */
knoxx.backend.policy_db.query_BANG_ = (function knoxx$backend$policy_db$query_BANG_(pool,sql_str,params){
var params_arr = ((cljs.core.seq(params))?cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(params):undefined);
return pool.query(sql_str,params_arr);
});
/**
 * Execute query and return first row.
 */
knoxx.backend.policy_db.query_one_BANG_ = (function knoxx$backend$policy_db$query_one_BANG_(pool,sql_str,params){
return knoxx.backend.policy_db.query_BANG_(pool,sql_str,params).then((function (result){
var rows = (result["rows"]);
if(cljs.core.truth_((function (){var and__5140__auto__ = rows;
if(cljs.core.truth_(and__5140__auto__)){
return (rows.length > (0));
} else {
return and__5140__auto__;
}
})())){
return (rows[(0)]);
} else {
return null;
}
}));
});
knoxx.backend.policy_db.role_permissions_uses_legacy_ids_QMARK_ = (function knoxx$backend$policy_db$role_permissions_uses_legacy_ids_QMARK_(pool){
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'role_permissions'",cljs.core.PersistentVector.EMPTY).then((function (result){
var rows = cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((result["rows"]));
var columns = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__65588_SHARP_){
return (p1__65588_SHARP_["column_name"]);
}),rows));
return cljs.core.contains_QMARK_(columns,"permission_id");
}));
});
knoxx.backend.policy_db.ensure_permission_records_BANG_ = (function knoxx$backend$policy_db$ensure_permission_records_BANG_(pool,permission_codes){
var codes = knoxx.backend.policy_db.unique(permission_codes);
if(cljs.core.empty_QMARK_(codes)){
return Promise.resolve(null);
} else {
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$ensure_permission_records_BANG__$_iter__65589(s__65590){
return (new cljs.core.LazySeq(null,(function (){
var s__65590__$1 = s__65590;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65590__$1);
if(temp__5825__auto__){
var s__65590__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65590__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65590__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65592 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65591 = (0);
while(true){
if((i__65591 < size__5627__auto__)){
var code = cljs.core._nth(c__5626__auto__,i__65591);
cljs.core.chunk_append(b__65592,(function (){var map__65593 = knoxx.backend.policy_db.permission_row_shape(code);
var map__65593__$1 = cljs.core.__destructure_map(map__65593);
var resource_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65593__$1,new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299));
var action = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65593__$1,new cljs.core.Keyword(null,"action","action",-811238024));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65593__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO permissions (code, resource_kind, action, description)\n                         VALUES ($1, $2, $3, $4)\n                         ON CONFLICT (code) DO UPDATE\n                         SET resource_kind = EXCLUDED.resource_kind,\n                             action = EXCLUDED.action,\n                             description = EXCLUDED.description",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [code,resource_kind,action,description], null));
})());

var G__65789 = (i__65591 + (1));
i__65591 = G__65789;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65592),knoxx$backend$policy_db$ensure_permission_records_BANG__$_iter__65589(cljs.core.chunk_rest(s__65590__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65592),null);
}
} else {
var code = cljs.core.first(s__65590__$2);
return cljs.core.cons((function (){var map__65594 = knoxx.backend.policy_db.permission_row_shape(code);
var map__65594__$1 = cljs.core.__destructure_map(map__65594);
var resource_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65594__$1,new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299));
var action = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65594__$1,new cljs.core.Keyword(null,"action","action",-811238024));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65594__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO permissions (code, resource_kind, action, description)\n                         VALUES ($1, $2, $3, $4)\n                         ON CONFLICT (code) DO UPDATE\n                         SET resource_kind = EXCLUDED.resource_kind,\n                             action = EXCLUDED.action,\n                             description = EXCLUDED.description",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [code,resource_kind,action,description], null));
})(),knoxx$backend$policy_db$ensure_permission_records_BANG__$_iter__65589(cljs.core.rest(s__65590__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(codes);
})())).then((function (_){
return null;
}));
}
});
knoxx.backend.policy_db.fetch_role_permission_rows_BANG_ = (function knoxx$backend$policy_db$fetch_role_permission_rows_BANG_(pool,role_ids){
return knoxx.backend.policy_db.role_permissions_uses_legacy_ids_QMARK_(pool).then((function (legacy_QMARK_){
if(cljs.core.truth_(legacy_QMARK_)){
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT rp.role_id, p.code AS code FROM role_permissions rp JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = ANY($1::uuid[]) ORDER BY p.code ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_ids)], null));
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT role_id, permission_code AS code FROM role_permissions WHERE role_id = ANY($1::uuid[]) ORDER BY permission_code ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_ids)], null));
}
}));
});
/**
 * Convert HoneySQL map to [sql-string & params].
 */
knoxx.backend.policy_db.honey__GT_sql = (function knoxx$backend$policy_db$honey__GT_sql(honey_map){
return honey.sql.format.cljs$core$IFn$_invoke$arity$2(honey_map,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"numbered","numbered",-2119856269),true], null));
});
/**
 * Execute HoneySQL query map.
 */
knoxx.backend.policy_db.honey_query_BANG_ = (function knoxx$backend$policy_db$honey_query_BANG_(pool,honey_map){
var vec__65595 = knoxx.backend.policy_db.honey__GT_sql(honey_map);
var seq__65596 = cljs.core.seq(vec__65595);
var first__65597 = cljs.core.first(seq__65596);
var seq__65596__$1 = cljs.core.next(seq__65596);
var sql_str = first__65597;
var params = seq__65596__$1;
return knoxx.backend.policy_db.query_BANG_(pool,sql_str,params);
});
/**
 * Execute HoneySQL query and return first row.
 */
knoxx.backend.policy_db.honey_query_one_BANG_ = (function knoxx$backend$policy_db$honey_query_one_BANG_(pool,honey_map){
var vec__65598 = knoxx.backend.policy_db.honey__GT_sql(honey_map);
var seq__65599 = cljs.core.seq(vec__65598);
var first__65600 = cljs.core.first(seq__65599);
var seq__65599__$1 = cljs.core.next(seq__65599);
var sql_str = first__65600;
var params = seq__65599__$1;
return knoxx.backend.policy_db.query_one_BANG_(pool,sql_str,params);
});
knoxx.backend.policy_db.ensure_schema_BANG_ = (function knoxx$backend$policy_db$ensure_schema_BANG_(pool){
return knoxx.backend.policy_db.query_BANG_(pool,"\n    CREATE EXTENSION IF NOT EXISTS pgcrypto;\n\n    CREATE TABLE IF NOT EXISTS orgs (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      slug TEXT NOT NULL UNIQUE,\n      name TEXT NOT NULL,\n      kind TEXT NOT NULL DEFAULT 'customer',\n      is_primary BOOLEAN NOT NULL DEFAULT FALSE,\n      status TEXT NOT NULL DEFAULT 'active',\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    CREATE TABLE IF NOT EXISTS users (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      email TEXT NOT NULL UNIQUE,\n      display_name TEXT NOT NULL,\n      auth_provider TEXT NOT NULL DEFAULT 'bootstrap',\n      external_subject TEXT,\n      status TEXT NOT NULL DEFAULT 'active',\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    CREATE TABLE IF NOT EXISTS memberships (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      actor_id TEXT,\n      status TEXT NOT NULL DEFAULT 'active',\n      is_default BOOLEAN NOT NULL DEFAULT FALSE,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      UNIQUE (user_id, org_id)\n    );\n\n    CREATE TABLE IF NOT EXISTS roles (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      org_id UUID REFERENCES orgs(id) ON DELETE CASCADE,\n      name TEXT NOT NULL,\n      slug TEXT NOT NULL,\n      scope_kind TEXT NOT NULL DEFAULT 'org',\n      built_in BOOLEAN NOT NULL DEFAULT FALSE,\n      system_managed BOOLEAN NOT NULL DEFAULT FALSE,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      CHECK (scope_kind IN ('platform', 'org'))\n    );\n\n    CREATE UNIQUE INDEX IF NOT EXISTS roles_platform_slug_uniq\n      ON roles (slug)\n      WHERE org_id IS NULL;\n\n    CREATE UNIQUE INDEX IF NOT EXISTS roles_org_slug_uniq\n      ON roles (org_id, slug)\n      WHERE org_id IS NOT NULL;\n\n    CREATE TABLE IF NOT EXISTS role_permissions (\n      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,\n      permission_code TEXT NOT NULL,\n      effect TEXT NOT NULL DEFAULT 'allow',\n      PRIMARY KEY (role_id, permission_code),\n      CHECK (effect IN ('allow', 'deny'))\n    );\n\n    -- Legacy compatibility: older Knoxx builds stored role permissions via\n    -- permission_id -> permissions(id). Newer builds store permission_code\n    -- directly so contracts can drive the catalog without a separate table.\n    ALTER TABLE role_permissions\n      ADD COLUMN IF NOT EXISTS permission_code TEXT;\n\n    DO $$\n    BEGIN\n      IF EXISTS (\n        SELECT 1\n        FROM information_schema.tables\n        WHERE table_schema = 'public'\n          AND table_name = 'permissions'\n      ) AND EXISTS (\n        SELECT 1\n        FROM information_schema.columns\n        WHERE table_schema = 'public'\n          AND table_name = 'role_permissions'\n          AND column_name = 'permission_id'\n      ) THEN\n        UPDATE role_permissions rp\n        SET permission_code = p.code\n        FROM permissions p\n        WHERE rp.permission_code IS NULL\n          AND rp.permission_id = p.id;\n      END IF;\n    END $$;\n\n    CREATE UNIQUE INDEX IF NOT EXISTS role_permissions_role_code_uniq\n      ON role_permissions (role_id, permission_code)\n      WHERE permission_code IS NOT NULL;\n\n    CREATE TABLE IF NOT EXISTS membership_roles (\n      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,\n      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,\n      PRIMARY KEY (membership_id, role_id)\n    );\n\n    CREATE TABLE IF NOT EXISTS tool_definitions (\n      id TEXT PRIMARY KEY,\n      label TEXT NOT NULL,\n      description TEXT NOT NULL,\n      risk_level TEXT NOT NULL DEFAULT 'medium'\n    );\n\n    CREATE TABLE IF NOT EXISTS role_tool_policies (\n      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,\n      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,\n      effect TEXT NOT NULL DEFAULT 'allow',\n      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      PRIMARY KEY (role_id, tool_id),\n      CHECK (effect IN ('allow', 'deny'))\n    );\n\n    CREATE TABLE IF NOT EXISTS user_tool_policies (\n      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,\n      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,\n      effect TEXT NOT NULL DEFAULT 'allow',\n      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      PRIMARY KEY (membership_id, tool_id),\n      CHECK (effect IN ('allow', 'deny'))\n    );\n\n    CREATE TABLE IF NOT EXISTS actor_credentials (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      provider TEXT NOT NULL,\n      kind TEXT NOT NULL DEFAULT 'credential',\n      account_identifier TEXT,\n      secret_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      status TEXT NOT NULL DEFAULT 'active',\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      UNIQUE (user_id, org_id, provider, kind)\n    );\n\n    CREATE INDEX IF NOT EXISTS actor_credentials_user_org_idx\n      ON actor_credentials (user_id, org_id, provider);\n\n    CREATE TABLE IF NOT EXISTS data_lakes (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      name TEXT NOT NULL,\n      slug TEXT NOT NULL,\n      kind TEXT NOT NULL DEFAULT 'workspace_docs',\n      config_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      status TEXT NOT NULL DEFAULT 'active',\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      UNIQUE (org_id, slug)\n    );\n\n    CREATE TABLE IF NOT EXISTS audit_events (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,\n      actor_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,\n      org_id UUID REFERENCES orgs(id) ON DELETE SET NULL,\n      action TEXT NOT NULL,\n      resource_kind TEXT NOT NULL,\n      resource_id TEXT,\n      before_json JSONB,\n      after_json JSONB,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    CREATE TABLE IF NOT EXISTS sessions (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,\n      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      token_hash TEXT NOT NULL,\n      token_prefix TEXT NOT NULL DEFAULT '',\n      salt TEXT NOT NULL,\n      email TEXT NOT NULL,\n      display_name TEXT NOT NULL,\n      auth_provider TEXT NOT NULL DEFAULT 'github',\n      external_subject TEXT,\n      ip_address TEXT,\n      user_agent TEXT,\n      expires_at TIMESTAMPTZ NOT NULL,\n      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    -- Backfill for installations that created `sessions` before `token_prefix` existed.\n    ALTER TABLE sessions\n      ADD COLUMN IF NOT EXISTS token_prefix TEXT NOT NULL DEFAULT '';\n\n    ALTER TABLE memberships\n      ADD COLUMN IF NOT EXISTS actor_id TEXT;\n\n    CREATE INDEX IF NOT EXISTS sessions_user_idx ON sessions (user_id);\n    CREATE INDEX IF NOT EXISTS sessions_membership_idx ON sessions (membership_id);\n    CREATE INDEX IF NOT EXISTS sessions_token_prefix_idx ON sessions (token_prefix);\n    CREATE INDEX IF NOT EXISTS sessions_expires_at_idx ON sessions (expires_at);\n\n    CREATE TABLE IF NOT EXISTS invites (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      code TEXT NOT NULL UNIQUE,\n      email TEXT NOT NULL,\n      inviter_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,\n      role_slugs JSONB NOT NULL DEFAULT '[]'::jsonb,\n      status TEXT NOT NULL DEFAULT 'pending',\n      redeemed_by UUID REFERENCES users(id) ON DELETE SET NULL,\n      redeemed_at TIMESTAMPTZ,\n      expires_at TIMESTAMPTZ NOT NULL,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    CREATE INDEX IF NOT EXISTS invites_org_idx ON invites (org_id);\n    CREATE INDEX IF NOT EXISTS invites_code_idx ON invites (code);\n    CREATE INDEX IF NOT EXISTS invites_status_idx ON invites (status);\n\n    CREATE TABLE IF NOT EXISTS knoxx_config (\n      key TEXT PRIMARY KEY,\n      value TEXT NOT NULL,\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()\n    );\n\n    -- Actor mailbox delivery projection. Canonical message/event content stays\n    -- in OpenPlanner/session/run logs; this table tracks addressing, delivery\n    -- state, retries, and audit pointers.\n    CREATE TABLE IF NOT EXISTS actor_mailbox_entries (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      kind TEXT NOT NULL DEFAULT 'actor-message',\n      status TEXT NOT NULL DEFAULT 'pending',\n      source_actor_id TEXT,\n      source_session_id TEXT,\n      source_conversation_id TEXT,\n      source_run_id TEXT,\n      source_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      target_kind TEXT NOT NULL DEFAULT 'unknown',\n      target_actor_id TEXT,\n      target_session_id TEXT,\n      target_conversation_id TEXT,\n      target_run_id TEXT,\n      target_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      delivery_mode TEXT NOT NULL DEFAULT 'follow-up',\n      attempts INT NOT NULL DEFAULT 0,\n      next_at TIMESTAMPTZ,\n      expires_at TIMESTAMPTZ,\n      delivered_at TIMESTAMPTZ,\n      acknowledged_at TIMESTAMPTZ,\n      content_ref_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      preview TEXT,\n      last_error TEXT,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      CHECK (status IN ('pending', 'delivered', 'failed', 'expired', 'superseded', 'acknowledged')),\n      CHECK (delivery_mode IN ('steer', 'follow-up', 'event', 'inbox-only', 'direct-run'))\n    );\n\n    CREATE INDEX IF NOT EXISTS actor_mailbox_status_next_idx ON actor_mailbox_entries (status, next_at);\n    CREATE INDEX IF NOT EXISTS actor_mailbox_target_actor_idx ON actor_mailbox_entries (target_actor_id, status, created_at DESC);\n    CREATE INDEX IF NOT EXISTS actor_mailbox_target_session_idx ON actor_mailbox_entries (target_session_id, status, created_at DESC);\n    CREATE INDEX IF NOT EXISTS actor_mailbox_source_run_idx ON actor_mailbox_entries (source_run_id, created_at DESC);\n\n    -- Latest live-route projection for actor-id -> conversation/session delivery.\n    CREATE TABLE IF NOT EXISTS actor_mailbox_routes (\n      actor_id TEXT PRIMARY KEY,\n      conversation_id TEXT,\n      session_id TEXT,\n      run_id TEXT,\n      contract_id TEXT,\n      status TEXT NOT NULL DEFAULT 'active',\n      source_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      expires_at TIMESTAMPTZ,\n      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      CHECK (status IN ('active', 'inactive'))\n    );\n\n    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_session_idx ON actor_mailbox_routes (session_id);\n    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_conversation_idx ON actor_mailbox_routes (conversation_id);\n    CREATE INDEX IF NOT EXISTS actor_mailbox_routes_status_seen_idx ON actor_mailbox_routes (status, last_seen_at DESC);\n\n    -- Broadcast studio persistent state\n    CREATE TABLE IF NOT EXISTS studio_state (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,\n      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,\n      kind TEXT NOT NULL DEFAULT 'player',\n      state_json JSONB NOT NULL DEFAULT '{}'::jsonb,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      UNIQUE (user_id, org_id, kind)\n    );\n\n    CREATE INDEX IF NOT EXISTS studio_state_user_idx ON studio_state (user_id);\n    CREATE INDEX IF NOT EXISTS studio_state_org_idx ON studio_state (org_id);\n\n    CREATE TABLE IF NOT EXISTS studio_audio_assets (\n      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n      audio_path TEXT NOT NULL,\n      asset_type TEXT NOT NULL CHECK (asset_type IN ('waveform', 'spectrogram')),\n      image_data BYTEA NOT NULL,\n      mime_type TEXT NOT NULL DEFAULT 'image/png',\n      width INT,\n      height INT,\n      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n      UNIQUE (audio_path, asset_type)\n    );\n\n    CREATE INDEX IF NOT EXISTS studio_audio_assets_path_idx ON studio_audio_assets (audio_path);\n    CREATE INDEX IF NOT EXISTS studio_audio_assets_type_idx ON studio_audio_assets (asset_type);\n  ",null);
});
/**
 * Permissions are now contract-driven via role EDN files. No-op for backwards compat.
 */
knoxx.backend.policy_db.insert_permission_seeds_BANG_ = (function knoxx$backend$policy_db$insert_permission_seeds_BANG_(pool){
return Promise.resolve(null);
});
knoxx.backend.policy_db.insert_tool_seeds_BANG_ = (function knoxx$backend$policy_db$insert_tool_seeds_BANG_(pool){
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$insert_tool_seeds_BANG__$_iter__65601(s__65602){
return (new cljs.core.LazySeq(null,(function (){
var s__65602__$1 = s__65602;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65602__$1);
if(temp__5825__auto__){
var s__65602__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65602__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65602__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65604 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65603 = (0);
while(true){
if((i__65603 < size__5627__auto__)){
var tool_id = cljs.core._nth(c__5626__auto__,i__65603);
cljs.core.chunk_append(b__65604,(function (){var map__65605 = knoxx.backend.tools.registry.get_tool(tool_id);
var map__65605__$1 = cljs.core.__destructure_map(map__65605);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65605__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65605__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
var risk_level = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65605__$1,new cljs.core.Keyword(null,"risk-level","risk-level",658496607));
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO tool_definitions (id, label, description, risk_level)\n                     VALUES ($1, $2, $3, $4)\n                     ON CONFLICT (id) DO UPDATE\n                     SET label = EXCLUDED.label,\n                         description = EXCLUDED.description,\n                         risk_level = EXCLUDED.risk_level",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [tool_id,(function (){var or__5142__auto__ = label;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})(),(function (){var or__5142__auto__ = description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = risk_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "low";
}
})()], null));
})());

var G__65790 = (i__65603 + (1));
i__65603 = G__65790;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65604),knoxx$backend$policy_db$insert_tool_seeds_BANG__$_iter__65601(cljs.core.chunk_rest(s__65602__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65604),null);
}
} else {
var tool_id = cljs.core.first(s__65602__$2);
return cljs.core.cons((function (){var map__65606 = knoxx.backend.tools.registry.get_tool(tool_id);
var map__65606__$1 = cljs.core.__destructure_map(map__65606);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65606__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65606__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
var risk_level = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65606__$1,new cljs.core.Keyword(null,"risk-level","risk-level",658496607));
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO tool_definitions (id, label, description, risk_level)\n                     VALUES ($1, $2, $3, $4)\n                     ON CONFLICT (id) DO UPDATE\n                     SET label = EXCLUDED.label,\n                         description = EXCLUDED.description,\n                         risk_level = EXCLUDED.risk_level",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [tool_id,(function (){var or__5142__auto__ = label;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return tool_id;
}
})(),(function (){var or__5142__auto__ = description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = risk_level;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "low";
}
})()], null));
})(),knoxx$backend$policy_db$insert_tool_seeds_BANG__$_iter__65601(cljs.core.rest(s__65602__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(knoxx.backend.policy_db.seed_tool_ids());
})())).then((function (_){
return null;
}));
});
knoxx.backend.policy_db.ensure_primary_org_BANG_ = (function knoxx$backend$policy_db$ensure_primary_org_BANG_(pool,options){
var slug = knoxx.backend.policy_db.slugify((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"primaryOrgSlug","primaryOrgSlug",-1593178745).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "open-hax";
}
})(),"open-hax");
var name = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"primaryOrgName","primaryOrgName",-778206708).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Open Hax";
}
})()));
var kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"primaryOrgKind","primaryOrgKind",1238921763).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "platform_owner";
}
})()));
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO orgs (slug, name, kind, is_primary, status)\n                     VALUES ($1, $2, $3, TRUE, 'active')\n                     ON CONFLICT (slug) DO UPDATE\n                     SET name = EXCLUDED.name,\n                         kind = EXCLUDED.kind,\n                         is_primary = TRUE,\n                         updated_at = NOW()\n                     RETURNING *",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [slug,name,kind], null)).then((function (org){
return knoxx.backend.policy_db.query_BANG_(pool,"UPDATE orgs SET is_primary = CASE WHEN slug = $1 THEN TRUE ELSE FALSE END",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [slug], null)).then((function (_){
return org;
}));
}));
});
knoxx.backend.policy_db.find_org_by_slug = (function knoxx$backend$policy_db$find_org_by_slug(pool,slug){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)))){
return Promise.resolve(null);
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM orgs WHERE lower(slug) = lower($1) LIMIT 1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [slug], null));
}
});
knoxx.backend.policy_db.find_role = (function knoxx$backend$policy_db$find_role(pool,p__65607){
var map__65608 = p__65607;
var map__65608__$1 = cljs.core.__destructure_map(map__65608);
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65608__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var slug = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65608__$1,new cljs.core.Keyword(null,"slug","slug",2029314850));
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM roles WHERE slug = $1 AND (($2::uuid IS NULL AND org_id IS NULL) OR org_id = $2::uuid) LIMIT 1",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [slug,org_id], null));
});
knoxx.backend.policy_db.ensure_role_BANG_ = (function knoxx$backend$policy_db$ensure_role_BANG_(pool,p__65609){
var map__65610 = p__65609;
var map__65610__$1 = cljs.core.__destructure_map(map__65610);
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"name","name",1843675177));
var slug = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"slug","slug",2029314850));
var scope_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"scope-kind","scope-kind",-2016316465));
var built_in = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"built-in","built-in",1245067766));
var system_managed = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65610__$1,new cljs.core.Keyword(null,"system-managed","system-managed",-191362489));
return knoxx.backend.policy_db.find_role(pool,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"slug","slug",2029314850),slug], null)).then((function (existing){
if(cljs.core.truth_(existing)){
return knoxx.backend.policy_db.query_one_BANG_(pool,"UPDATE roles SET name = $2, scope_kind = $3, built_in = $4, system_managed = $5, updated_at = NOW() WHERE id = $1 RETURNING *",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(existing["id"]),name,scope_kind,built_in,system_managed], null));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO roles (org_id, name, slug, scope_kind, built_in, system_managed) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *",new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id,name,slug,scope_kind,built_in,system_managed], null));
}
}));
});
knoxx.backend.policy_db.set_role_permissions_BANG_ = (function knoxx$backend$policy_db$set_role_permissions_BANG_(pool,role_id,permission_codes){
var codes = knoxx.backend.policy_db.unique(permission_codes);
return knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM role_permissions WHERE role_id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id], null)).then((function (_){
return knoxx.backend.policy_db.role_permissions_uses_legacy_ids_QMARK_(pool);
})).then((function (legacy_QMARK_){
if(cljs.core.empty_QMARK_(codes)){
return null;
} else {
if(cljs.core.truth_(legacy_QMARK_)){
return knoxx.backend.policy_db.ensure_permission_records_BANG_(pool,codes).then((function (_){
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65611(s__65612){
return (new cljs.core.LazySeq(null,(function (){
var s__65612__$1 = s__65612;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65612__$1);
if(temp__5825__auto__){
var s__65612__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65612__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65612__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65614 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65613 = (0);
while(true){
if((i__65613 < size__5627__auto__)){
var code = cljs.core._nth(c__5626__auto__,i__65613);
cljs.core.chunk_append(b__65614,knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_permissions (role_id, permission_id, effect) SELECT $1, id, 'allow' FROM permissions WHERE code = $2 ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,code], null)));

var G__65791 = (i__65613 + (1));
i__65613 = G__65791;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65614),knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65611(cljs.core.chunk_rest(s__65612__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65614),null);
}
} else {
var code = cljs.core.first(s__65612__$2);
return cljs.core.cons(knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_permissions (role_id, permission_id, effect) SELECT $1, id, 'allow' FROM permissions WHERE code = $2 ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,code], null)),knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65611(cljs.core.rest(s__65612__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(codes);
})()));
}));
} else {
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65615(s__65616){
return (new cljs.core.LazySeq(null,(function (){
var s__65616__$1 = s__65616;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65616__$1);
if(temp__5825__auto__){
var s__65616__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65616__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65616__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65618 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65617 = (0);
while(true){
if((i__65617 < size__5627__auto__)){
var code = cljs.core._nth(c__5626__auto__,i__65617);
cljs.core.chunk_append(b__65618,knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_permissions (role_id, permission_code, effect) VALUES ($1, $2, 'allow') ON CONFLICT (role_id, permission_code) DO UPDATE SET effect = EXCLUDED.effect",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,code], null)));

var G__65792 = (i__65617 + (1));
i__65617 = G__65792;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65618),knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65615(cljs.core.chunk_rest(s__65616__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65618),null);
}
} else {
var code = cljs.core.first(s__65616__$2);
return cljs.core.cons(knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_permissions (role_id, permission_code, effect) VALUES ($1, $2, 'allow') ON CONFLICT (role_id, permission_code) DO UPDATE SET effect = EXCLUDED.effect",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,code], null)),knoxx$backend$policy_db$set_role_permissions_BANG__$_iter__65615(cljs.core.rest(s__65616__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(codes);
})()));
}
}
}));
});
knoxx.backend.policy_db.set_role_tool_policies_BANG_ = (function knoxx$backend$policy_db$set_role_tool_policies_BANG_(pool,role_id,tool_policies){
var normalized = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.normalize_tool_policy,tool_policies);
var tool_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),normalized);
return knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM role_tool_policies WHERE role_id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id], null)).then((function (_){
return knoxx.backend.policy_db.ensure_tool_definitions_BANG_(pool,tool_ids);
})).then((function (_){
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$set_role_tool_policies_BANG__$_iter__65619(s__65620){
return (new cljs.core.LazySeq(null,(function (){
var s__65620__$1 = s__65620;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65620__$1);
if(temp__5825__auto__){
var s__65620__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65620__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65620__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65622 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65621 = (0);
while(true){
if((i__65621 < size__5627__auto__)){
var policy = cljs.core._nth(c__5626__auto__,i__65621);
cljs.core.chunk_append(b__65622,knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_tool_policies (role_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (role_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy),JSON.stringify(cljs.core.clj__GT_js(new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(policy)))], null)));

var G__65793 = (i__65621 + (1));
i__65621 = G__65793;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65622),knoxx$backend$policy_db$set_role_tool_policies_BANG__$_iter__65619(cljs.core.chunk_rest(s__65620__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65622),null);
}
} else {
var policy = cljs.core.first(s__65620__$2);
return cljs.core.cons(knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO role_tool_policies (role_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (role_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy),JSON.stringify(cljs.core.clj__GT_js(new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(policy)))], null)),knoxx$backend$policy_db$set_role_tool_policies_BANG__$_iter__65619(cljs.core.rest(s__65620__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(normalized);
})()));
})).then((function (_){
return null;
}));
});
knoxx.backend.policy_db.set_membership_tool_policies_BANG_ = (function knoxx$backend$policy_db$set_membership_tool_policies_BANG_(pool,membership_id,tool_policies){
var normalized = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.normalize_tool_policy,tool_policies);
var tool_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"toolId","toolId",-1935596543),normalized);
return knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM user_tool_policies WHERE membership_id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)).then((function (_){
return knoxx.backend.policy_db.ensure_tool_definitions_BANG_(pool,tool_ids);
})).then((function (_){
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$set_membership_tool_policies_BANG__$_iter__65623(s__65624){
return (new cljs.core.LazySeq(null,(function (){
var s__65624__$1 = s__65624;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65624__$1);
if(temp__5825__auto__){
var s__65624__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65624__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65624__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65626 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65625 = (0);
while(true){
if((i__65625 < size__5627__auto__)){
var policy = cljs.core._nth(c__5626__auto__,i__65625);
cljs.core.chunk_append(b__65626,knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO user_tool_policies (membership_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (membership_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy),JSON.stringify(cljs.core.clj__GT_js(new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(policy)))], null)));

var G__65794 = (i__65625 + (1));
i__65625 = G__65794;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65626),knoxx$backend$policy_db$set_membership_tool_policies_BANG__$_iter__65623(cljs.core.chunk_rest(s__65624__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65626),null);
}
} else {
var policy = cljs.core.first(s__65624__$2);
return cljs.core.cons(knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO user_tool_policies (membership_id, tool_id, effect, constraints_json) VALUES ($1, $2, $3, $4::jsonb) ON CONFLICT (membership_id, tool_id) DO UPDATE SET effect = EXCLUDED.effect, constraints_json = EXCLUDED.constraints_json",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id,new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy),JSON.stringify(cljs.core.clj__GT_js(new cljs.core.Keyword(null,"constraints","constraints",422775616).cljs$core$IFn$_invoke$arity$1(policy)))], null)),knoxx$backend$policy_db$set_membership_tool_policies_BANG__$_iter__65623(cljs.core.rest(s__65624__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(normalized);
})()));
})).then((function (_){
return null;
}));
});
knoxx.backend.policy_db.ensure_builtin_org_roles_BANG_ = (function knoxx$backend$policy_db$ensure_builtin_org_roles_BANG_(pool,_org){
return (knoxx.backend.policy_db.sync_contract_role_projections_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.policy_db.sync_contract_role_projections_BANG_.cljs$core$IFn$_invoke$arity$1(pool) : knoxx.backend.policy_db.sync_contract_role_projections_BANG_.call(null,pool));
});
knoxx.backend.policy_db.ensure_builtin_platform_roles_BANG_ = (function knoxx$backend$policy_db$ensure_builtin_platform_roles_BANG_(pool){
return (knoxx.backend.policy_db.sync_contract_role_projections_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.policy_db.sync_contract_role_projections_BANG_.cljs$core$IFn$_invoke$arity$1(pool) : knoxx.backend.policy_db.sync_contract_role_projections_BANG_.call(null,pool));
});
knoxx.backend.policy_db.keywordish_id = (function knoxx$backend$policy_db$keywordish_id(value){
if((value instanceof cljs.core.Keyword)){
return cljs.core.name(value);
} else {
if(typeof value === 'string'){
var G__65627 = value;
var G__65627__$1 = (((G__65627 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65627)));
var G__65627__$2 = (((G__65627__$1 == null))?null:clojure.string.trim(G__65627__$1));
if((G__65627__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65627__$2);
}
} else {
return null;

}
}
});
knoxx.backend.policy_db.contract_role_slug = (function knoxx$backend$policy_db$contract_role_slug(role_record){
var G__65628 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(role_record);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.keywordish_id(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(role_record,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"contract","contract",798152745),new cljs.core.Keyword("role","id","role/id",-1375589954)], null)));
}
})();
var G__65628__$1 = (((G__65628 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65628)));
var G__65628__$2 = (((G__65628__$1 == null))?null:clojure.string.trim(G__65628__$1));
if((G__65628__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65628__$2);
}
});
knoxx.backend.policy_db.titleize_contract_id = (function knoxx$backend$policy_db$titleize_contract_id(contract_id){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.capitalize,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(contract_id)),/[-_]+/))));
});
knoxx.backend.policy_db.contract_permission_codes = (function knoxx$backend$policy_db$contract_permission_codes(role_contract){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,(function (){var or__5142__auto__ = new cljs.core.Keyword("role","permissions","role/permissions",54401385).cljs$core$IFn$_invoke$arity$1(role_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
});
knoxx.backend.policy_db.capability_records_by_id = (function knoxx$backend$policy_db$capability_records_by_id(records){
return cljs.core.into.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (record){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(record),new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(record)], null);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__65629_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("capabilities",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__65629_SHARP_));
}),records)));
});
knoxx.backend.policy_db.contract_capability_id = (function knoxx$backend$policy_db$contract_capability_id(value){
return knoxx.backend.policy_db.keywordish_id(value);
});
knoxx.backend.policy_db.contract_tool_policies_for_role = (function knoxx$backend$policy_db$contract_tool_policies_for_role(capabilities_by_id,role_contract){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (tool_id){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),tool_id,new cljs.core.Keyword(null,"effect","effect",347343289),"allow"], null);
}),cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.tools.registry.normalize_tool_id,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__65630_SHARP_){
var or__5142__auto__ = new cljs.core.Keyword("cap","tools","cap/tools",-1241568196).cljs$core$IFn$_invoke$arity$1(p1__65630_SHARP_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.keep.cljs$core$IFn$_invoke$arity$2(capabilities_by_id,cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.contract_capability_id,(function (){var or__5142__auto__ = new cljs.core.Keyword("role","capabilities","role/capabilities",208971087).cljs$core$IFn$_invoke$arity$1(role_contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))], 0))))));
});
/**
 * Mirror canonical role contracts into SQL role rows.
 * 
 * SQL is a projection only: role identity comes from parsed/deduped contract
 * records, not from filenames, hard-coded role lists, or whatever rows happen
 * to already exist in Postgres.
 */
knoxx.backend.policy_db.sync_contract_role_projections_BANG_ = (function knoxx$backend$policy_db$sync_contract_role_projections_BANG_(pool){
return knoxx.backend.contracts.loader.load_all_contracts_BANG_(knoxx.backend.policy_db.contracts_config()).then((function (records){
var capabilities_by_id = knoxx.backend.policy_db.capability_records_by_id(records);
var role_records = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__65631_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("roles",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__65631_SHARP_));
}),records));
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (role_record){
var temp__5825__auto__ = knoxx.backend.policy_db.contract_role_slug(role_record);
if(cljs.core.truth_(temp__5825__auto__)){
var slug = temp__5825__auto__;
var role_contract = new cljs.core.Keyword(null,"contract","contract",798152745).cljs$core$IFn$_invoke$arity$1(role_record);
var name = (function (){var or__5142__auto__ = (function (){var G__65632 = new cljs.core.Keyword("role","label","role/label",1746427558).cljs$core$IFn$_invoke$arity$1(role_contract);
var G__65632__$1 = (((G__65632 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65632)));
var G__65632__$2 = (((G__65632__$1 == null))?null:clojure.string.trim(G__65632__$1));
if((G__65632__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65632__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__65633 = new cljs.core.Keyword("role","name","role/name",1848754355).cljs$core$IFn$_invoke$arity$1(role_contract);
var G__65633__$1 = (((G__65633 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65633)));
var G__65633__$2 = (((G__65633__$1 == null))?null:clojure.string.trim(G__65633__$1));
if((G__65633__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65633__$2);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.policy_db.titleize_contract_id(slug);
}
}
})();
return knoxx.backend.policy_db.ensure_role_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),null,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"slug","slug",2029314850),slug,new cljs.core.Keyword(null,"scope-kind","scope-kind",-2016316465),"platform",new cljs.core.Keyword(null,"built-in","built-in",1245067766),false,new cljs.core.Keyword(null,"system-managed","system-managed",-191362489),true], null)).then((function (role){
return knoxx.backend.policy_db.set_role_permissions_BANG_(pool,(role["id"]),knoxx.backend.policy_db.contract_permission_codes(role_contract)).then((function (_){
return knoxx.backend.policy_db.set_role_tool_policies_BANG_(pool,(role["id"]),knoxx.backend.policy_db.contract_tool_policies_for_role(capabilities_by_id,role_contract));
}));
}));
} else {
return null;
}
}),role_records))).then((function (_){
return null;
}));
}));
});
knoxx.backend.policy_db.contract_role_slugs_from_records = (function knoxx$backend$policy_db$contract_role_slugs_from_records(records){
return cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.contract_role_slug,cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__65634_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("roles",new cljs.core.Keyword(null,"contractClass","contractClass",-918904694).cljs$core$IFn$_invoke$arity$1(p1__65634_SHARP_));
}),records)));
});
/**
 * Keep only role claims that resolve to canonical role contracts.
 * 
 * This is used only while projecting actor contracts into SQL. Missing role
 * contracts are contract-layer drift, not a reason for SQL to invent roles or
 * to be treated as the source of truth.
 */
knoxx.backend.policy_db.canonicalize_contract_role_slugs_BANG_ = (function knoxx$backend$policy_db$canonicalize_contract_role_slugs_BANG_(role_slugs){
return knoxx.backend.contracts.loader.load_all_contracts_BANG_(knoxx.backend.policy_db.contracts_config()).then((function (records){
var known = knoxx.backend.policy_db.contract_role_slugs_from_records(records);
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (raw){
var raw_slug = (function (){var G__65635 = raw;
var G__65635__$1 = (((G__65635 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65635)));
var G__65635__$2 = (((G__65635__$1 == null))?null:clojure.string.trim(G__65635__$1));
if((G__65635__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65635__$2);
}
})();
var normalized = (cljs.core.truth_(raw_slug)?knoxx.backend.policy_db.slugify(raw_slug,raw_slug):null);
if(cljs.core.truth_((function (){var and__5140__auto__ = raw_slug;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(known,raw_slug);
} else {
return and__5140__auto__;
}
})())){
return raw_slug;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = normalized;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(known,normalized);
} else {
return and__5140__auto__;
}
})())){
return normalized;
} else {
if(cljs.core.truth_(raw_slug)){
console.warn("[policy-db] actor role claim has no role contract; skipping SQL projection role",raw_slug);

return null;
} else {
return null;

}
}
}
}),(function (){var or__5142__auto__ = role_slugs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
}));
});
knoxx.backend.policy_db.tool_allowed = (function knoxx$backend$policy_db$tool_allowed(context,tool_id){
var policies = (function (){var or__5142__auto__ = (function (){var G__65636 = context;
if((G__65636 == null)){
return null;
} else {
return (G__65636["toolPolicies"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})();
var match = cljs.core.some((function (entry){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((entry["toolId"]),tool_id)){
return entry;
} else {
return null;
}
}),policies);
return (!(((function (){var and__5140__auto__ = match;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((match["effect"]),"allow");
} else {
return and__5140__auto__;
}
})() == null)));
});
knoxx.backend.policy_db.resolve_role_ids = (function knoxx$backend$policy_db$resolve_role_ids(pool,p__65637){
var map__65638 = p__65637;
var map__65638__$1 = cljs.core.__destructure_map(map__65638);
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65638__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var role_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65638__$1,new cljs.core.Keyword(null,"role-ids","role-ids",652985101));
var role_slugs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65638__$1,new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158));
var resolved_ids = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,(function (){var or__5142__auto__ = role_ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})())));
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$resolve_role_ids_$_iter__65639(s__65640){
return (new cljs.core.LazySeq(null,(function (){
var s__65640__$1 = s__65640;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65640__$1);
if(temp__5825__auto__){
var s__65640__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65640__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65640__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65642 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65641 = (0);
while(true){
if((i__65641 < size__5627__auto__)){
var slug = cljs.core._nth(c__5626__auto__,i__65641);
cljs.core.chunk_append(b__65642,(function (){var raw_slug = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)));
var normalized = knoxx.backend.policy_db.slugify(raw_slug,raw_slug);
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT id FROM roles WHERE (slug = $1 OR slug = $2) AND (org_id = $3::uuid OR org_id IS NULL) ORDER BY CASE WHEN org_id IS NULL THEN 1 ELSE 0 END, created_at ASC LIMIT 1",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [raw_slug,normalized,org_id], null)).then(((function (i__65641,raw_slug,normalized,slug,c__5626__auto__,size__5627__auto__,b__65642,s__65640__$2,temp__5825__auto__,resolved_ids,map__65638,map__65638__$1,org_id,role_ids,role_slugs){
return (function (row){
if(cljs.core.truth_(row)){
} else {
throw (new Error((""+"Role not found for slug '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_slug)+"'")));
}

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(resolved_ids,cljs.core.conj,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((row["id"]))));
});})(i__65641,raw_slug,normalized,slug,c__5626__auto__,size__5627__auto__,b__65642,s__65640__$2,temp__5825__auto__,resolved_ids,map__65638,map__65638__$1,org_id,role_ids,role_slugs))
);
})());

var G__65795 = (i__65641 + (1));
i__65641 = G__65795;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65642),knoxx$backend$policy_db$resolve_role_ids_$_iter__65639(cljs.core.chunk_rest(s__65640__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65642),null);
}
} else {
var slug = cljs.core.first(s__65640__$2);
return cljs.core.cons((function (){var raw_slug = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)));
var normalized = knoxx.backend.policy_db.slugify(raw_slug,raw_slug);
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT id FROM roles WHERE (slug = $1 OR slug = $2) AND (org_id = $3::uuid OR org_id IS NULL) ORDER BY CASE WHEN org_id IS NULL THEN 1 ELSE 0 END, created_at ASC LIMIT 1",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [raw_slug,normalized,org_id], null)).then(((function (raw_slug,normalized,slug,s__65640__$2,temp__5825__auto__,resolved_ids,map__65638,map__65638__$1,org_id,role_ids,role_slugs){
return (function (row){
if(cljs.core.truth_(row)){
} else {
throw (new Error((""+"Role not found for slug '"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw_slug)+"'")));
}

return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(resolved_ids,cljs.core.conj,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((row["id"]))));
});})(raw_slug,normalized,slug,s__65640__$2,temp__5825__auto__,resolved_ids,map__65638,map__65638__$1,org_id,role_ids,role_slugs))
);
})(),knoxx$backend$policy_db$resolve_role_ids_$_iter__65639(cljs.core.rest(s__65640__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.some_QMARK_,(function (){var or__5142__auto__ = role_slugs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()));
})())).then((function (_){
return cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.deref(resolved_ids));
}));
});
knoxx.backend.policy_db.set_membership_roles_BANG_ = (function knoxx$backend$policy_db$set_membership_roles_BANG_(pool,membership_id,p__65643){
var map__65644 = p__65643;
var map__65644__$1 = cljs.core.__destructure_map(map__65644);
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65644__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var role_ids = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65644__$1,new cljs.core.Keyword(null,"role-ids","role-ids",652985101));
var role_slugs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65644__$1,new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158));
var replace = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65644__$1,new cljs.core.Keyword(null,"replace","replace",-786587770));
var contract_projection = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65644__$1,new cljs.core.Keyword(null,"contract-projection","contract-projection",-1495437365));
var role_slugs_promise = (cljs.core.truth_(contract_projection)?knoxx.backend.policy_db.canonicalize_contract_role_slugs_BANG_(role_slugs):Promise.resolve((function (){var or__5142__auto__ = role_slugs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()));
return role_slugs_promise.then((function (resolved_role_slugs){
return knoxx.backend.policy_db.resolve_role_ids(pool,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"role-ids","role-ids",652985101),(function (){var or__5142__auto__ = role_ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),resolved_role_slugs], null));
})).then((function (resolved_ids){
return (cljs.core.truth_(replace)?knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM membership_roles WHERE membership_id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)):Promise.resolve(null)).then((function (_){
return Promise.all(cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$set_membership_roles_BANG__$_iter__65645(s__65646){
return (new cljs.core.LazySeq(null,(function (){
var s__65646__$1 = s__65646;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65646__$1);
if(temp__5825__auto__){
var s__65646__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65646__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65646__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65648 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65647 = (0);
while(true){
if((i__65647 < size__5627__auto__)){
var role_id = cljs.core._nth(c__5626__auto__,i__65647);
cljs.core.chunk_append(b__65648,knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id,role_id], null)));

var G__65796 = (i__65647 + (1));
i__65647 = G__65796;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65648),knoxx$backend$policy_db$set_membership_roles_BANG__$_iter__65645(cljs.core.chunk_rest(s__65646__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65648),null);
}
} else {
var role_id = cljs.core.first(s__65646__$2);
return cljs.core.cons(knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id,role_id], null)),knoxx$backend$policy_db$set_membership_roles_BANG__$_iter__65645(cljs.core.rest(s__65646__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(resolved_ids);
})()));
})).then((function (_){
return resolved_ids;
}));
}));
});
knoxx.backend.policy_db.hydrate_role_maps = (function knoxx$backend$policy_db$hydrate_role_maps(pool,roles){
if(cljs.core.empty_QMARK_(roles)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
var role_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__65649_SHARP_){
return (p1__65649_SHARP_["id"]);
}),roles);
return Promise.all(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.policy_db.fetch_role_permission_rows_BANG_(pool,role_ids),knoxx.backend.policy_db.query_BANG_(pool,"SELECT rtp.role_id, rtp.tool_id, rtp.effect, rtp.constraints_json FROM role_tool_policies rtp WHERE rtp.role_id = ANY($1::uuid[]) ORDER BY rtp.tool_id ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_ids)], null))], null)).then((function (p__65650){
var vec__65651 = p__65650;
var perm_result = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65651,(0),null);
var tool_result = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65651,(1),null);
var perm_rows = (perm_result["rows"]);
var tool_rows = (tool_result["rows"]);
var perm_map = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var tool_map = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var seq__65654_65797 = cljs.core.seq(cljs.core.range.cljs$core$IFn$_invoke$arity$1(perm_rows.length));
var chunk__65655_65798 = null;
var count__65656_65799 = (0);
var i__65657_65800 = (0);
while(true){
if((i__65657_65800 < count__65656_65799)){
var i_65801 = chunk__65655_65798.cljs$core$IIndexed$_nth$arity$2(null,i__65657_65800);
var row_65802 = (perm_rows[i_65801]);
var rid_65803 = (row_65802["role_id"]);
var code_65804 = (row_65802["code"]);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(perm_map,cljs.core.update,rid_65803,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([code_65804], 0));


var G__65805 = seq__65654_65797;
var G__65806 = chunk__65655_65798;
var G__65807 = count__65656_65799;
var G__65808 = (i__65657_65800 + (1));
seq__65654_65797 = G__65805;
chunk__65655_65798 = G__65806;
count__65656_65799 = G__65807;
i__65657_65800 = G__65808;
continue;
} else {
var temp__5825__auto___65809 = cljs.core.seq(seq__65654_65797);
if(temp__5825__auto___65809){
var seq__65654_65810__$1 = temp__5825__auto___65809;
if(cljs.core.chunked_seq_QMARK_(seq__65654_65810__$1)){
var c__5673__auto___65811 = cljs.core.chunk_first(seq__65654_65810__$1);
var G__65812 = cljs.core.chunk_rest(seq__65654_65810__$1);
var G__65813 = c__5673__auto___65811;
var G__65814 = cljs.core.count(c__5673__auto___65811);
var G__65815 = (0);
seq__65654_65797 = G__65812;
chunk__65655_65798 = G__65813;
count__65656_65799 = G__65814;
i__65657_65800 = G__65815;
continue;
} else {
var i_65816 = cljs.core.first(seq__65654_65810__$1);
var row_65817 = (perm_rows[i_65816]);
var rid_65818 = (row_65817["role_id"]);
var code_65819 = (row_65817["code"]);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(perm_map,cljs.core.update,rid_65818,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([code_65819], 0));


var G__65820 = cljs.core.next(seq__65654_65810__$1);
var G__65821 = null;
var G__65822 = (0);
var G__65823 = (0);
seq__65654_65797 = G__65820;
chunk__65655_65798 = G__65821;
count__65656_65799 = G__65822;
i__65657_65800 = G__65823;
continue;
}
} else {
}
}
break;
}

var seq__65658_65824 = cljs.core.seq(cljs.core.range.cljs$core$IFn$_invoke$arity$1(tool_rows.length));
var chunk__65659_65825 = null;
var count__65660_65826 = (0);
var i__65661_65827 = (0);
while(true){
if((i__65661_65827 < count__65660_65826)){
var i_65828 = chunk__65659_65825.cljs$core$IIndexed$_nth$arity$2(null,i__65661_65827);
var row_65829 = (tool_rows[i_65828]);
var rid_65830 = (row_65829["role_id"]);
var policy_65831 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(row_65829["tool_id"]),new cljs.core.Keyword(null,"effect","effect",347343289),(row_65829["effect"]),new cljs.core.Keyword(null,"constraints","constraints",422775616),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (row_65829["constraints_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(tool_map,cljs.core.update,rid_65830,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([policy_65831], 0));


var G__65832 = seq__65658_65824;
var G__65833 = chunk__65659_65825;
var G__65834 = count__65660_65826;
var G__65835 = (i__65661_65827 + (1));
seq__65658_65824 = G__65832;
chunk__65659_65825 = G__65833;
count__65660_65826 = G__65834;
i__65661_65827 = G__65835;
continue;
} else {
var temp__5825__auto___65836 = cljs.core.seq(seq__65658_65824);
if(temp__5825__auto___65836){
var seq__65658_65837__$1 = temp__5825__auto___65836;
if(cljs.core.chunked_seq_QMARK_(seq__65658_65837__$1)){
var c__5673__auto___65838 = cljs.core.chunk_first(seq__65658_65837__$1);
var G__65839 = cljs.core.chunk_rest(seq__65658_65837__$1);
var G__65840 = c__5673__auto___65838;
var G__65841 = cljs.core.count(c__5673__auto___65838);
var G__65842 = (0);
seq__65658_65824 = G__65839;
chunk__65659_65825 = G__65840;
count__65660_65826 = G__65841;
i__65661_65827 = G__65842;
continue;
} else {
var i_65843 = cljs.core.first(seq__65658_65837__$1);
var row_65844 = (tool_rows[i_65843]);
var rid_65845 = (row_65844["role_id"]);
var policy_65846 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(row_65844["tool_id"]),new cljs.core.Keyword(null,"effect","effect",347343289),(row_65844["effect"]),new cljs.core.Keyword(null,"constraints","constraints",422775616),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (row_65844["constraints_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(tool_map,cljs.core.update,rid_65845,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([policy_65846], 0));


var G__65847 = cljs.core.next(seq__65658_65837__$1);
var G__65848 = null;
var G__65849 = (0);
var G__65850 = (0);
seq__65658_65824 = G__65847;
chunk__65659_65825 = G__65848;
count__65660_65826 = G__65849;
i__65661_65827 = G__65850;
continue;
}
} else {
}
}
break;
}

return cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$hydrate_role_maps_$_iter__65662(s__65663){
return (new cljs.core.LazySeq(null,(function (){
var s__65663__$1 = s__65663;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65663__$1);
if(temp__5825__auto__){
var s__65663__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65663__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65663__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65665 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65664 = (0);
while(true){
if((i__65664 < size__5627__auto__)){
var role = cljs.core._nth(c__5626__auto__,i__65664);
cljs.core.chunk_append(b__65665,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"slug","slug",2029314850),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"builtIn","builtIn",-1253686643),new cljs.core.Keyword(null,"scopeKind","scopeKind",994904174),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"systemManaged","systemManaged",-1020093673)],[(role["slug"]),(role["updated_at"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(perm_map),(role["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(role["org_id"]),(role["name"]),(role["created_at"]),(role["built_in"]),(role["scope_kind"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(tool_map),(role["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(role["id"]),(role["system_managed"])]));

var G__65851 = (i__65664 + (1));
i__65664 = G__65851;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65665),knoxx$backend$policy_db$hydrate_role_maps_$_iter__65662(cljs.core.chunk_rest(s__65663__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65665),null);
}
} else {
var role = cljs.core.first(s__65663__$2);
return cljs.core.cons(cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"slug","slug",2029314850),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"builtIn","builtIn",-1253686643),new cljs.core.Keyword(null,"scopeKind","scopeKind",994904174),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"systemManaged","systemManaged",-1020093673)],[(role["slug"]),(role["updated_at"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(perm_map),(role["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(role["org_id"]),(role["name"]),(role["created_at"]),(role["built_in"]),(role["scope_kind"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(tool_map),(role["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(role["id"]),(role["system_managed"])]),knoxx$backend$policy_db$hydrate_role_maps_$_iter__65662(cljs.core.rest(s__65663__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(roles);
})());
}));
}
});
knoxx.backend.policy_db.hydrate_memberships = (function knoxx$backend$policy_db$hydrate_memberships(pool,memberships){
if(cljs.core.empty_QMARK_(memberships)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
var membership_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__65666_SHARP_){
return (p1__65666_SHARP_["id"]);
}),memberships);
return Promise.all(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.policy_db.query_BANG_(pool,"SELECT mr.membership_id, r.id AS role_id, r.slug, r.name, r.scope_kind, r.org_id FROM membership_roles mr JOIN roles r ON r.id = mr.role_id WHERE mr.membership_id = ANY($1::uuid[]) ORDER BY r.name ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(membership_ids)], null)),knoxx.backend.policy_db.query_BANG_(pool,"SELECT membership_id, tool_id, effect, constraints_json FROM user_tool_policies WHERE membership_id = ANY($1::uuid[]) ORDER BY tool_id ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(membership_ids)], null))], null)).then((function (p__65667){
var vec__65668 = p__65667;
var role_result = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65668,(0),null);
var tool_result = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65668,(1),null);
var role_rows = (role_result["rows"]);
var tool_rows = (tool_result["rows"]);
var roles_by_m = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var tools_by_m = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var seq__65671_65852 = cljs.core.seq(cljs.core.range.cljs$core$IFn$_invoke$arity$1(role_rows.length));
var chunk__65672_65853 = null;
var count__65673_65854 = (0);
var i__65674_65855 = (0);
while(true){
if((i__65674_65855 < count__65673_65854)){
var i_65856 = chunk__65672_65853.cljs$core$IIndexed$_nth$arity$2(null,i__65674_65855);
var row_65857 = (role_rows[i_65856]);
var m_id_65858 = (row_65857["membership_id"]);
var role_65859 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(row_65857["role_id"]),new cljs.core.Keyword(null,"slug","slug",2029314850),(row_65857["slug"]),new cljs.core.Keyword(null,"name","name",1843675177),(row_65857["name"]),new cljs.core.Keyword(null,"scopeKind","scopeKind",994904174),(row_65857["scope_kind"]),new cljs.core.Keyword(null,"orgId","orgId",-73585595),(row_65857["org_id"])], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(roles_by_m,cljs.core.update,m_id_65858,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([role_65859], 0));


var G__65860 = seq__65671_65852;
var G__65861 = chunk__65672_65853;
var G__65862 = count__65673_65854;
var G__65863 = (i__65674_65855 + (1));
seq__65671_65852 = G__65860;
chunk__65672_65853 = G__65861;
count__65673_65854 = G__65862;
i__65674_65855 = G__65863;
continue;
} else {
var temp__5825__auto___65864 = cljs.core.seq(seq__65671_65852);
if(temp__5825__auto___65864){
var seq__65671_65865__$1 = temp__5825__auto___65864;
if(cljs.core.chunked_seq_QMARK_(seq__65671_65865__$1)){
var c__5673__auto___65866 = cljs.core.chunk_first(seq__65671_65865__$1);
var G__65867 = cljs.core.chunk_rest(seq__65671_65865__$1);
var G__65868 = c__5673__auto___65866;
var G__65869 = cljs.core.count(c__5673__auto___65866);
var G__65870 = (0);
seq__65671_65852 = G__65867;
chunk__65672_65853 = G__65868;
count__65673_65854 = G__65869;
i__65674_65855 = G__65870;
continue;
} else {
var i_65871 = cljs.core.first(seq__65671_65865__$1);
var row_65872 = (role_rows[i_65871]);
var m_id_65873 = (row_65872["membership_id"]);
var role_65874 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(row_65872["role_id"]),new cljs.core.Keyword(null,"slug","slug",2029314850),(row_65872["slug"]),new cljs.core.Keyword(null,"name","name",1843675177),(row_65872["name"]),new cljs.core.Keyword(null,"scopeKind","scopeKind",994904174),(row_65872["scope_kind"]),new cljs.core.Keyword(null,"orgId","orgId",-73585595),(row_65872["org_id"])], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(roles_by_m,cljs.core.update,m_id_65873,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([role_65874], 0));


var G__65875 = cljs.core.next(seq__65671_65865__$1);
var G__65876 = null;
var G__65877 = (0);
var G__65878 = (0);
seq__65671_65852 = G__65875;
chunk__65672_65853 = G__65876;
count__65673_65854 = G__65877;
i__65674_65855 = G__65878;
continue;
}
} else {
}
}
break;
}

var seq__65675_65879 = cljs.core.seq(cljs.core.range.cljs$core$IFn$_invoke$arity$1(tool_rows.length));
var chunk__65676_65880 = null;
var count__65677_65881 = (0);
var i__65678_65882 = (0);
while(true){
if((i__65678_65882 < count__65677_65881)){
var i_65883 = chunk__65676_65880.cljs$core$IIndexed$_nth$arity$2(null,i__65678_65882);
var row_65884 = (tool_rows[i_65883]);
var m_id_65885 = (row_65884["membership_id"]);
var policy_65886 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(row_65884["tool_id"]),new cljs.core.Keyword(null,"effect","effect",347343289),(row_65884["effect"]),new cljs.core.Keyword(null,"constraints","constraints",422775616),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (row_65884["constraints_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(tools_by_m,cljs.core.update,m_id_65885,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([policy_65886], 0));


var G__65887 = seq__65675_65879;
var G__65888 = chunk__65676_65880;
var G__65889 = count__65677_65881;
var G__65890 = (i__65678_65882 + (1));
seq__65675_65879 = G__65887;
chunk__65676_65880 = G__65888;
count__65677_65881 = G__65889;
i__65678_65882 = G__65890;
continue;
} else {
var temp__5825__auto___65891 = cljs.core.seq(seq__65675_65879);
if(temp__5825__auto___65891){
var seq__65675_65892__$1 = temp__5825__auto___65891;
if(cljs.core.chunked_seq_QMARK_(seq__65675_65892__$1)){
var c__5673__auto___65893 = cljs.core.chunk_first(seq__65675_65892__$1);
var G__65894 = cljs.core.chunk_rest(seq__65675_65892__$1);
var G__65895 = c__5673__auto___65893;
var G__65896 = cljs.core.count(c__5673__auto___65893);
var G__65897 = (0);
seq__65675_65879 = G__65894;
chunk__65676_65880 = G__65895;
count__65677_65881 = G__65896;
i__65678_65882 = G__65897;
continue;
} else {
var i_65898 = cljs.core.first(seq__65675_65892__$1);
var row_65899 = (tool_rows[i_65898]);
var m_id_65900 = (row_65899["membership_id"]);
var policy_65901 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),(row_65899["tool_id"]),new cljs.core.Keyword(null,"effect","effect",347343289),(row_65899["effect"]),new cljs.core.Keyword(null,"constraints","constraints",422775616),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = (row_65899["constraints_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(tools_by_m,cljs.core.update,m_id_65900,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([policy_65901], 0));


var G__65902 = cljs.core.next(seq__65675_65892__$1);
var G__65903 = null;
var G__65904 = (0);
var G__65905 = (0);
seq__65675_65879 = G__65902;
chunk__65676_65880 = G__65903;
count__65677_65881 = G__65904;
i__65678_65882 = G__65905;
continue;
}
} else {
}
}
break;
}

return cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$hydrate_memberships_$_iter__65679(s__65680){
return (new cljs.core.LazySeq(null,(function (){
var s__65680__$1 = s__65680;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65680__$1);
if(temp__5825__auto__){
var s__65680__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65680__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65680__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65682 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65681 = (0);
while(true){
if((i__65681 < size__5627__auto__)){
var membership = cljs.core._nth(c__5626__auto__,i__65681);
cljs.core.chunk_append(b__65682,cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"isDefault","isDefault",809666532),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"orgName","orgName",-751297303),new cljs.core.Keyword(null,"roles","roles",143379530),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"userId","userId",575594135)],[(function (){var or__5142__auto__ = knoxx.backend.policy_db.normalize_actor_id((membership["actor_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.default_membership_actor_id(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"slug","slug",2029314850),(function (){var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(roles_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
}
})(),(membership["updated_at"]),(membership["is_default"]),(membership["org_id"]),(membership["org_name"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(roles_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(membership["org_slug"]),(membership["created_at"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(tools_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(membership["status"]),(membership["id"]),(membership["user_id"])]));

var G__65906 = (i__65681 + (1));
i__65681 = G__65906;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65682),knoxx$backend$policy_db$hydrate_memberships_$_iter__65679(cljs.core.chunk_rest(s__65680__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65682),null);
}
} else {
var membership = cljs.core.first(s__65680__$2);
return cljs.core.cons(cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"actorId","actorId",989542370),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"isDefault","isDefault",809666532),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"orgName","orgName",-751297303),new cljs.core.Keyword(null,"roles","roles",143379530),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"userId","userId",575594135)],[(function (){var or__5142__auto__ = knoxx.backend.policy_db.normalize_actor_id((membership["actor_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.default_membership_actor_id(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"slug","slug",2029314850),(function (){var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(roles_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
}
})(),(membership["updated_at"]),(membership["is_default"]),(membership["org_id"]),(membership["org_name"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(roles_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(membership["org_slug"]),(membership["created_at"]),(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(tools_by_m),(membership["id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})(),(membership["status"]),(membership["id"]),(membership["user_id"])]),knoxx$backend$policy_db$hydrate_memberships_$_iter__65679(cljs.core.rest(s__65680__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(memberships);
})());
}));
}
});
knoxx.backend.policy_db.js_json_map = (function knoxx$backend$policy_db$js_json_map(value){
if((value == null)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
if(cljs.core.map_QMARK_(value)){
return value;
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));

}
}
});
knoxx.backend.policy_db.credential_provider_label = (function knoxx$backend$policy_db$credential_provider_label(provider){
var G__65683 = provider;
switch (G__65683) {
case "bluesky":
return "Bluesky";

break;
case "twitch":
return "Twitch";

break;
case "discord_bot":
return "Discord bot";

break;
case "discord_oauth":
return "Discord OAuth";

break;
default:
return provider;

}
});
knoxx.backend.policy_db.redact_credential_row = (function knoxx$backend$policy_db$redact_credential_row(row){
var secrets = knoxx.backend.policy_db.js_json_map((row["secret_json"]));
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"configuredFields","configuredFields",523282029),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"provider","provider",-302056900)],[(row["account_identifier"]),(row["updated_at"]),(row["created_at"]),cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p__65684){
var vec__65685 = p__65684;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65685,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65685,(1),null);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = v;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
return null;
} else {
return cljs.core.name(k);
}
}),secrets))),(row["status"]),knoxx.backend.policy_db.credential_provider_label((row["provider"])),(row["id"]),(row["kind"]),(row["provider"])]);
});
knoxx.backend.policy_db.internal_credential_row = (function knoxx$backend$policy_db$internal_credential_row(row){
if(cljs.core.truth_(row)){
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),(row["id"]),new cljs.core.Keyword(null,"provider","provider",-302056900),(row["provider"]),new cljs.core.Keyword(null,"kind","kind",-717265803),(row["kind"]),new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),(row["account_identifier"]),new cljs.core.Keyword(null,"status","status",-1997798413),(row["status"]),new cljs.core.Keyword(null,"secretJson","secretJson",1807839704),knoxx.backend.policy_db.js_json_map((row["secret_json"])),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),(row["created_at"]),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),(row["updated_at"])], null);
} else {
return null;
}
});
knoxx.backend.policy_db.fetch_credentials_by_user_BANG_ = (function knoxx$backend$policy_db$fetch_credentials_by_user_BANG_(pool,user_ids,org_id){
if(cljs.core.empty_QMARK_(user_ids)){
return Promise.resolve(cljs.core.PersistentArrayMap.EMPTY);
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM actor_credentials WHERE user_id = ANY($1::uuid[]) AND org_id = $2::uuid ORDER BY provider ASC, kind ASC",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(user_ids),org_id], null)).then((function (result){
var rows = (result["rows"]);
var by_user = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var seq__65688_65908 = cljs.core.seq(cljs.core.range.cljs$core$IFn$_invoke$arity$1(rows.length));
var chunk__65689_65909 = null;
var count__65690_65910 = (0);
var i__65691_65911 = (0);
while(true){
if((i__65691_65911 < count__65690_65910)){
var i_65912 = chunk__65689_65909.cljs$core$IIndexed$_nth$arity$2(null,i__65691_65911);
var row_65913 = (rows[i_65912]);
var user_id_65914 = (row_65913["user_id"]);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(by_user,cljs.core.update,user_id_65914,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.policy_db.redact_credential_row(row_65913)], 0));


var G__65915 = seq__65688_65908;
var G__65916 = chunk__65689_65909;
var G__65917 = count__65690_65910;
var G__65918 = (i__65691_65911 + (1));
seq__65688_65908 = G__65915;
chunk__65689_65909 = G__65916;
count__65690_65910 = G__65917;
i__65691_65911 = G__65918;
continue;
} else {
var temp__5825__auto___65919 = cljs.core.seq(seq__65688_65908);
if(temp__5825__auto___65919){
var seq__65688_65920__$1 = temp__5825__auto___65919;
if(cljs.core.chunked_seq_QMARK_(seq__65688_65920__$1)){
var c__5673__auto___65921 = cljs.core.chunk_first(seq__65688_65920__$1);
var G__65922 = cljs.core.chunk_rest(seq__65688_65920__$1);
var G__65923 = c__5673__auto___65921;
var G__65924 = cljs.core.count(c__5673__auto___65921);
var G__65925 = (0);
seq__65688_65908 = G__65922;
chunk__65689_65909 = G__65923;
count__65690_65910 = G__65924;
i__65691_65911 = G__65925;
continue;
} else {
var i_65926 = cljs.core.first(seq__65688_65920__$1);
var row_65927 = (rows[i_65926]);
var user_id_65928 = (row_65927["user_id"]);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(by_user,cljs.core.update,user_id_65928,cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.policy_db.redact_credential_row(row_65927)], 0));


var G__65929 = cljs.core.next(seq__65688_65920__$1);
var G__65930 = null;
var G__65931 = (0);
var G__65932 = (0);
seq__65688_65908 = G__65929;
chunk__65689_65909 = G__65930;
count__65690_65910 = G__65931;
i__65691_65911 = G__65932;
continue;
}
} else {
}
}
break;
}

return cljs.core.deref(by_user);
}));
}
});
knoxx.backend.policy_db.actor_email_from_id = (function knoxx$backend$policy_db$actor_email_from_id(actor_id){
var slug = clojure.string.replace(clojure.string.replace(clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))),/[^a-z0-9._+-]+/,"-"),/^[-.]+|[-.]+$/,"");
if(clojure.string.blank_QMARK_(slug)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)+"@actors.local");
}
});
knoxx.backend.policy_db.nonblank_secret_map = (function knoxx$backend$policy_db$nonblank_secret_map(value){
return cljs.core.reduce_kv((function (acc,k,v){
var s = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = v;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(s)){
return acc;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(acc,cljs.core.name(k),s);
}
}),cljs.core.PersistentArrayMap.EMPTY,knoxx.backend.policy_db.js_json_map(value));
});
knoxx.backend.policy_db.factory_list_actor_credentials = (function knoxx$backend$policy_db$factory_list_actor_credentials(pool,provider){
return knoxx.backend.policy.protocol.list_actor_credentials(knoxx.backend.policy_db.sql_policy_store(pool,null),provider).then((function (credentials){
return ({"credentials": cljs.core.clj__GT_js(credentials)});
}));
});
knoxx.backend.policy_db.factory_get_actor_credential = (function knoxx$backend$policy_db$factory_get_actor_credential(pool,actor_id,provider){
return knoxx.backend.policy.protocol.get_actor_credential(knoxx.backend.policy_db.sql_policy_store(pool,null),actor_id,provider).then((function (credential){
return ({"credential": cljs.core.clj__GT_js(credential)});
}));
});
knoxx.backend.policy_db.load_detailed_roles = (function knoxx$backend$policy_db$load_detailed_roles(pool,role_ids){
if(cljs.core.empty_QMARK_(role_ids)){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM roles WHERE id = ANY($1::uuid[]) ORDER BY name ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_ids)], null)).then((function (result){
return knoxx.backend.policy_db.hydrate_role_maps(pool,(result["rows"]));
}));
}
});
knoxx.backend.policy_db.find_request_membership_row = (function knoxx$backend$policy_db$find_request_membership_row(pool,headers_like){
var membership_id = knoxx.backend.policy_db.header_value(headers_like,"x-knoxx-membership-id");
var user_email = clojure.string.lower_case(knoxx.backend.policy_db.header_value(headers_like,"x-knoxx-user-email"));
var org_id = knoxx.backend.policy_db.header_value(headers_like,"x-knoxx-org-id");
var org_slug = clojure.string.lower_case(knoxx.backend.policy_db.header_value(headers_like,"x-knoxx-org-slug"));
if(((clojure.string.blank_QMARK_(membership_id)) && (clojure.string.blank_QMARK_(user_email)))){
return Promise.reject(knoxx.backend.policy_db.http_error((401),"Knoxx request context is missing x-knoxx-user-email or x-knoxx-membership-id","request_context_missing"));
} else {
if((!(clojure.string.blank_QMARK_(membership_id)))){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null));
} else {
if((((!(clojure.string.blank_QMARK_(user_email)))) && ((((!(clojure.string.blank_QMARK_(org_id)))) || ((!(clojure.string.blank_QMARK_(org_slug)))))))){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE lower(u.email) = $1 AND (($2 <> '' AND o.id = $2::uuid) OR ($3 <> '' AND lower(o.slug) = $3)) ORDER BY m.is_default DESC, m.created_at ASC LIMIT 1",new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [user_email,org_id,org_slug], null));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.*, u.email, u.display_name, u.status AS user_status, o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE lower(u.email) = $1 ORDER BY m.is_default DESC, o.is_primary DESC, m.created_at ASC LIMIT 1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [user_email], null));

}
}
}
});
knoxx.backend.policy_db.build_request_context = (function knoxx$backend$policy_db$build_request_context(pool,membership_row){
if(cljs.core.not(membership_row)){
return Promise.reject(knoxx.backend.policy_db.http_error((401),"Knoxx request context did not resolve to a membership","request_context_unresolved"));
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((membership_row["user_status"]),"active")){
return Promise.reject(knoxx.backend.policy_db.http_error((403),"Knoxx user is not active","user_inactive"));
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((membership_row["status"]),"active")){
return Promise.reject(knoxx.backend.policy_db.http_error((403),"Knoxx membership is not active","membership_inactive"));
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((membership_row["org_status"]),"active")){
return Promise.reject(knoxx.backend.policy_db.http_error((403),"Knoxx org is not active","org_inactive"));
} else {
return knoxx.backend.policy_db.hydrate_memberships(pool,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_row], null)).then((function (memberships){
var membership = cljs.core.first(memberships);
var role_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"roles","roles",143379530).cljs$core$IFn$_invoke$arity$1(membership));
return knoxx.backend.policy_db.load_detailed_roles(pool,role_ids).then((function (detailed_roles){
var permissions = cljs.core.sort.cljs$core$IFn$_invoke$arity$1(knoxx.backend.policy_db.unique(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(new cljs.core.Keyword(null,"permissions","permissions",67803075),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([detailed_roles], 0))));
var effective_tool_policies = knoxx.backend.policy_db.merge_toolPolicies(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic(new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([detailed_roles], 0)),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(membership));
var role_slugs = cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2((function (p1__65692_SHARP_){
return (- knoxx.backend.policy_db.rolePriority(p1__65692_SHARP_));
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"slug","slug",2029314850),detailed_roles));
var actor_id = (function (){var or__5142__auto__ = knoxx.backend.policy_db.normalize_actor_id((membership_row["actor_id"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.default_membership_actor_id(role_slugs);
}
})();
return cljs.core.clj__GT_js(cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"permissions","permissions",67803075),new cljs.core.Keyword(null,"isSystemAdmin","isSystemAdmin",679314438),new cljs.core.Keyword(null,"roles","roles",143379530),new cljs.core.Keyword(null,"membership","membership",254556333),new cljs.core.Keyword(null,"membershipToolPolicies","membershipToolPolicies",-954353456),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976),new cljs.core.Keyword(null,"org","org",1495985),new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),new cljs.core.Keyword(null,"actor","actor",-1830560481)],[permissions,cljs.core.contains_QMARK_(cljs.core.set(role_slugs),"system_admin"),detailed_roles,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(membership),new cljs.core.Keyword(null,"actorId","actorId",989542370),actor_id,new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(membership),new cljs.core.Keyword(null,"isDefault","isDefault",809666532),new cljs.core.Keyword(null,"isDefault","isDefault",809666532).cljs$core$IFn$_invoke$arity$1(membership),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"createdAt","createdAt",-936788).cljs$core$IFn$_invoke$arity$1(membership),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523).cljs$core$IFn$_invoke$arity$1(membership)], null),new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(membership),effective_tool_policies,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"id","id",-1388402092),(membership_row["org_id"]),new cljs.core.Keyword(null,"slug","slug",2029314850),(membership_row["org_slug"]),new cljs.core.Keyword(null,"name","name",1843675177),(membership_row["org_name"]),new cljs.core.Keyword(null,"status","status",-1997798413),(membership_row["org_status"]),new cljs.core.Keyword(null,"isPrimary","isPrimary",821290856),(membership_row["is_primary"]),new cljs.core.Keyword(null,"kind","kind",-717265803),(membership_row["org_kind"])], null),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(membership_row["user_id"]),new cljs.core.Keyword(null,"email","email",1415816706),(membership_row["email"]),new cljs.core.Keyword(null,"username","username",1605666410),(membership_row["email"]),new cljs.core.Keyword(null,"displayName","displayName",-809144601),(membership_row["display_name"]),new cljs.core.Keyword(null,"status","status",-1997798413),(membership_row["user_status"])], null),role_slugs,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),actor_id], null)]));
}));
}));

}
}
}
}
});
knoxx.backend.policy_db.ensure_bootstrap_user_BANG_ = (function knoxx$backend$policy_db$ensure_bootstrap_user_BANG_(pool,primary_org,options){
var email = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"bootstrapSystemAdminEmail","bootstrapSystemAdminEmail",-461724198).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "system-admin@open-hax.local";
}
})())));
var display_name = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"bootstrapSystemAdminName","bootstrapSystemAdminName",-2104021931).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Knoxx System Admin";
}
})()));
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO users (email, display_name, auth_provider, status) VALUES ($1, $2, 'bootstrap', 'active') ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name, updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [email,display_name], null)).then((function (user){
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, 'active', TRUE) ON CONFLICT (user_id, org_id) DO UPDATE SET is_default = TRUE, updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(user["id"]),(primary_org["id"])], null)).then((function (membership){
return knoxx.backend.policy_db.find_role(pool,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"slug","slug",2029314850),"system_admin",new cljs.core.Keyword(null,"org-id","org-id",1485182668),null], null)).then((function (system_admin){
var insert_role_promise = (cljs.core.truth_(system_admin)?knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(membership["id"]),(system_admin["id"])], null)):Promise.resolve(null));
return insert_role_promise.then((function (_){
return knoxx.backend.policy_db.set_membership_actor_id_BANG_(pool,(membership["id"]),"system_admin");
})).then((function (_){
return ({"user": user, "membership": membership});
}));
}));
}));
}));
});
knoxx.backend.policy_db.parse_bootstrap_allowlist_emails = (function knoxx$backend$policy_db$parse_bootstrap_allowlist_emails(options){
var raw = (function (){var or__5142__auto__ = (options["bootstrapAllowlistEmails"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"bootstrapAllowlistEmails","bootstrapAllowlistEmails",-1359191424).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var raw__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw));
var parts = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.lower_case,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2(raw__$1,/[\s,]+/))))));
return parts;
});
knoxx.backend.policy_db.parse_bootstrap_allowlist_role_slugs = (function knoxx$backend$policy_db$parse_bootstrap_allowlist_role_slugs(options){
var raw = (function (){var or__5142__auto__ = (options["bootstrapAllowlistRoleSlugs"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"bootstrapAllowlistRoleSlugs","bootstrapAllowlistRoleSlugs",1281999922).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var raw__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw));
var parts = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2(raw__$1,/[\s,]+/)))));
if(cljs.core.seq(parts)){
return parts;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["knowledge_worker"], null);
}
});
knoxx.backend.policy_db.ensure_bootstrap_allowlist_users_BANG_ = (function knoxx$backend$policy_db$ensure_bootstrap_allowlist_users_BANG_(pool,primary_org,options){
var emails = knoxx.backend.policy_db.parse_bootstrap_allowlist_emails(options);
var role_slugs = knoxx.backend.policy_db.parse_bootstrap_allowlist_role_slugs(options);
var org_id = (primary_org["id"]);
if(cljs.core.empty_QMARK_(emails)){
return Promise.resolve(null);
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (email){
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO users (email, display_name, auth_provider, status) VALUES ($1, $2, 'bootstrap', 'active') ON CONFLICT (email) DO UPDATE SET updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [email,email], null)).then((function (user){
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, 'active', FALSE) ON CONFLICT (user_id, org_id) DO UPDATE SET updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(user["id"]),org_id], null));
})).then((function (membership){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (slug){
return knoxx.backend.policy_db.find_role(pool,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"slug","slug",2029314850),slug,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id], null)).then((function (role){
if(cljs.core.truth_(role)){
return role;
} else {
return knoxx.backend.policy_db.find_role(pool,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"slug","slug",2029314850),slug,new cljs.core.Keyword(null,"org-id","org-id",1485182668),null], null));
}
})).then((function (role){
if(cljs.core.truth_(role)){
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO membership_roles (membership_id, role_id) VALUES ($1, $2) ON CONFLICT (membership_id, role_id) DO NOTHING",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(membership["id"]),(role["id"])], null));
} else {
return null;
}
}));
}),role_slugs))).then((function (_){
return knoxx.backend.policy_db.set_membership_actor_id_BANG_(pool,(membership["id"]),knoxx.backend.policy_db.default_membership_actor_id(role_slugs));
})).then((function (_){
return ({"email": email, "ok": true});
}));
}));
}),emails))).then((function (_){
return null;
}));
}
});
knoxx.backend.policy_db.append_audit_BANG_ = (function knoxx$backend$policy_db$append_audit_BANG_(pool,p__65693){
var map__65694 = p__65693;
var map__65694__$1 = cljs.core.__destructure_map(map__65694);
var actor_user_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995));
var actor_membership_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239));
var org_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"org-id","org-id",1485182668));
var action = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"action","action",-811238024));
var resource_kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299));
var resource_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582));
var before = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"before","before",-1633692388));
var after = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__65694__$1,new cljs.core.Keyword(null,"after","after",594996914));
return knoxx.backend.policy_db.query_BANG_(pool,"INSERT INTO audit_events (actor_user_id, actor_membership_id, org_id, action, resource_kind, resource_id, before_json, after_json) VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8::jsonb)",new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [actor_user_id,actor_membership_id,org_id,action,resource_kind,resource_id,(cljs.core.truth_(before)?JSON.stringify(cljs.core.clj__GT_js(before)):null),(cljs.core.truth_(after)?JSON.stringify(cljs.core.clj__GT_js(after)):null)], null));
});
knoxx.backend.policy_db.__GT_js_permission = (function knoxx$backend$policy_db$__GT_js_permission(code){
return ({"code": code});
});
knoxx.backend.policy_db.__GT_js_tool = (function knoxx$backend$policy_db$__GT_js_tool(row){
return ({"id": (row["id"]), "label": (row["label"]), "description": (row["description"]), "riskLevel": (row["risk_level"])});
});
knoxx.backend.policy_db.__GT_js_org = (function knoxx$backend$policy_db$__GT_js_org(row){
return ({"id": (row["id"]), "slug": (row["slug"]), "name": (row["name"]), "kind": (row["kind"]), "isPrimary": (row["is_primary"]), "status": (row["status"])});
});
knoxx.backend.policy_db.__GT_js_org_with_counts = (function knoxx$backend$policy_db$__GT_js_org_with_counts(row){
return ({"memberCount": Number((function (){var or__5142__auto__ = (row["member_count"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()), "slug": (row["slug"]), "updatedAt": (row["updated_at"]), "isPrimary": (row["is_primary"]), "name": (row["name"]), "createdAt": (row["created_at"]), "roleCount": Number((function (){var or__5142__auto__ = (row["role_count"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()), "status": (row["status"]), "id": (row["id"]), "kind": (row["kind"]), "dataLakeCount": Number((function (){var or__5142__auto__ = (row["data_lake_count"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})())});
});
knoxx.backend.policy_db.__GT_js_data_lake = (function knoxx$backend$policy_db$__GT_js_data_lake(row){
return ({"slug": (row["slug"]), "updatedAt": (row["updated_at"]), "orgId": (row["org_id"]), "config": (function (){var or__5142__auto__ = (row["config_json"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(), "name": (row["name"]), "createdAt": (row["created_at"]), "status": (row["status"]), "id": (row["id"]), "kind": (row["kind"])});
});
knoxx.backend.policy_db.factory_resolve_request_context = (function knoxx$backend$policy_db$factory_resolve_request_context(pool,headers_like){
return knoxx.backend.policy_db.find_request_membership_row(pool,headers_like).then((function (row){
return knoxx.backend.policy_db.build_request_context(pool,row);
}));
});
knoxx.backend.policy_db.factory_evaluate_tool_access = (function knoxx$backend$policy_db$factory_evaluate_tool_access(pool,headers_like,tool_id){
return knoxx.backend.policy_db.find_request_membership_row(pool,headers_like).then((function (row){
return knoxx.backend.policy_db.build_request_context(pool,row);
})).then((function (ctx){
return ({"context": ctx, "toolId": tool_id, "allowed": knoxx.backend.policy_db.tool_allowed(ctx,tool_id)});
}));
});
knoxx.backend.policy_db.factory_list_permissions = (function knoxx$backend$policy_db$factory_list_permissions(_pool){
var all_codes = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__65695_SHARP_){
return knoxx.backend.contracts.roles.role_permissions(knoxx.backend.policy_db.contracts_config(),p1__65695_SHARP_);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.contracts.roles.list_role_slugs(knoxx.backend.policy_db.contracts_config())], 0)))));
return Promise.resolve(({"permissions": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.__GT_js_permission,all_codes))}));
});
knoxx.backend.policy_db.factory_list_tools = (function knoxx$backend$policy_db$factory_list_tools(pool){
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC",cljs.core.PersistentVector.EMPTY).then((function (r){
return ({"tools": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.__GT_js_tool,(r["rows"])))});
}));
});
knoxx.backend.policy_db.factory_get_bootstrap_context = (function knoxx$backend$policy_db$factory_get_bootstrap_context(pool,primary_org,bootstrap){
var uid = (bootstrap["user"]["id"]);
var mid = (bootstrap["membership"]["id"]);
return Promise.resolve(({"primaryOrg": knoxx.backend.policy_db.__GT_js_org(primary_org), "bootstrapUser": ({"id": uid, "email": (bootstrap["user"]["email"]), "displayName": (bootstrap["user"]["display_name"]), "membershipId": mid})}));
});
knoxx.backend.policy_db.factory_list_orgs = (function knoxx$backend$policy_db$factory_list_orgs(pool){
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT o.*, COUNT(DISTINCT m.id) AS member_count, COUNT(DISTINCT r.id) FILTER (WHERE r.org_id = o.id) AS role_count, COUNT(DISTINCT d.id) AS data_lake_count FROM orgs o LEFT JOIN memberships m ON m.org_id = o.id LEFT JOIN roles r ON r.org_id = o.id LEFT JOIN data_lakes d ON d.org_id = o.id GROUP BY o.id ORDER BY o.is_primary DESC, o.name ASC",cljs.core.PersistentVector.EMPTY).then((function (r){
return ({"orgs": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.__GT_js_org_with_counts,(r["rows"])))});
}));
});
knoxx.backend.policy_db.factory_create_org = (function knoxx$backend$policy_db$factory_create_org(pool,uid,mid,payload){
var name = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(name)){
return Promise.reject((new Error("name is required")));
} else {
var slug = knoxx.backend.policy_db.slugify((function (){var or__5142__auto__ = (payload["slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return name;
}
})(),"org");
var kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "customer";
}
})()));
var status = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()));
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO orgs (slug, name, kind, is_primary, status) VALUES ($1, $2, $3, FALSE, $4) RETURNING *",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [slug,name,kind,status], null)).then((function (org){
return knoxx.backend.policy_db.ensure_builtin_org_roles_BANG_(pool,org).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),(org["id"]),new cljs.core.Keyword(null,"action","action",-811238024),"org.create",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"org",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),(org["id"])], null));
})).then((function (_){
return ({"org": knoxx.backend.policy_db.__GT_js_org(org)});
}));
}));
}
});
knoxx.backend.policy_db.factory_list_roles = (function knoxx$backend$policy_db$factory_list_roles(pool,opts){
var org_id = (function (){var G__65696 = opts;
if((G__65696 == null)){
return null;
} else {
return (G__65696["orgId"]);
}
})();
return (cljs.core.truth_(org_id)?knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM roles WHERE org_id = $1 ORDER BY built_in DESC, name ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null)):knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM roles ORDER BY built_in DESC, name ASC",cljs.core.PersistentVector.EMPTY)).then((function (r){
return knoxx.backend.policy_db.hydrate_role_maps(pool,(r["rows"]));
})).then((function (roles){
return ({"roles": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(roles)});
}));
});
knoxx.backend.policy_db.factory_get_role = (function knoxx$backend$policy_db$factory_get_role(pool,role_id){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM roles WHERE id = $1::uuid",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id], null)).then((function (row){
if(cljs.core.truth_(row)){
return knoxx.backend.policy_db.hydrate_role_maps(pool,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [row], null)).then((function (h){
return ({"role": cljs.core.first(h)});
}));
} else {
return ({"role": null});
}
}));
});
knoxx.backend.policy_db.factory_create_role = (function knoxx$backend$policy_db$factory_create_role(pool,uid,mid,payload){
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var name = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
if(clojure.string.blank_QMARK_(name)){
return Promise.reject((new Error("name is required")));
} else {
var slug = knoxx.backend.policy_db.slugify((function (){var or__5142__auto__ = (payload["slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return name;
}
})(),"role");
return knoxx.backend.policy_db.ensure_role_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"name","name",1843675177),name,new cljs.core.Keyword(null,"slug","slug",2029314850),slug,new cljs.core.Keyword(null,"scope-kind","scope-kind",-2016316465),"org",new cljs.core.Keyword(null,"built-in","built-in",1245067766),false,new cljs.core.Keyword(null,"system-managed","system-managed",-191362489),false], null)).then((function (role){
var rid = (role["id"]);
return knoxx.backend.policy_db.set_role_permissions_BANG_(pool,rid,(function (){var or__5142__auto__ = (payload["permissionCodes"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()).then((function (_){
return knoxx.backend.policy_db.set_role_tool_policies_BANG_(pool,rid,(function (){var or__5142__auto__ = (payload["toolPolicies"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
})).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"role.create",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"role",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),rid], null));
})).then((function (_){
return knoxx.backend.policy_db.hydrate_role_maps(pool,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role], null));
})).then((function (h){
return ({"role": cljs.core.first(h)});
}));
}));

}
}
});
knoxx.backend.policy_db.factory_set_role_tool_policies = (function knoxx$backend$policy_db$factory_set_role_tool_policies(pool,uid,mid,role_id,payload){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM roles WHERE id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role_id], null)).then((function (role){
if(cljs.core.not(role)){
return Promise.reject((new Error("role not found")));
} else {
return knoxx.backend.policy_db.set_role_tool_policies_BANG_(pool,role_id,(function (){var or__5142__auto__ = (payload["toolPolicies"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),(role["org_id"]),new cljs.core.Keyword(null,"action","action",-811238024),"role.tool_policy.update",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"role",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),role_id], null));
})).then((function (_){
return knoxx.backend.policy_db.hydrate_role_maps(pool,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [role], null));
})).then((function (h){
return ({"role": cljs.core.first(h)});
}));
}
}));
});
knoxx.backend.policy_db.factory_list_users = (function knoxx$backend$policy_db$factory_list_users(pool,opts){
var org_id = (function (){var G__65698 = opts;
if((G__65698 == null)){
return null;
} else {
return (G__65698["orgId"]);
}
})();
return (cljs.core.truth_(org_id)?knoxx.backend.policy_db.query_BANG_(pool,"SELECT DISTINCT u.* FROM users u JOIN memberships m ON m.user_id = u.id WHERE m.org_id = $1::uuid ORDER BY u.display_name ASC, u.email ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null)):knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM users ORDER BY display_name ASC, email ASC",cljs.core.PersistentVector.EMPTY)).then((function (user_result){
var users = cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((user_result["rows"])));
var user_ids = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__65697_SHARP_){
return (p1__65697_SHARP_["id"]);
}),users);
var memberships_promise = (cljs.core.truth_(org_id)?knoxx.backend.policy_db.query_BANG_(pool,"SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.user_id = ANY($1::uuid[]) AND m.org_id = $2::uuid ORDER BY m.created_at ASC",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(user_ids),org_id], null)):knoxx.backend.policy_db.query_BANG_(pool,"SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.user_id = ANY($1::uuid[]) ORDER BY o.name ASC, m.created_at ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(user_ids)], null)));
var credentials_promise = (cljs.core.truth_(org_id)?knoxx.backend.policy_db.fetch_credentials_by_user_BANG_(pool,user_ids,org_id):Promise.resolve(cljs.core.PersistentArrayMap.EMPTY));
return Promise.all(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [memberships_promise,credentials_promise], null)).then((function (p__65699){
var vec__65700 = p__65699;
var membership_result = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65700,(0),null);
var credentials_by_user = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65700,(1),null);
return knoxx.backend.policy_db.hydrate_memberships(pool,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((membership_result["rows"]))).then((function (memberships){
var memberships_by_user = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var seq__65703_65933 = cljs.core.seq(memberships);
var chunk__65704_65934 = null;
var count__65705_65935 = (0);
var i__65706_65936 = (0);
while(true){
if((i__65706_65936 < count__65705_65935)){
var membership_65937 = chunk__65704_65934.cljs$core$IIndexed$_nth$arity$2(null,i__65706_65936);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(memberships_by_user,cljs.core.update,new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(membership_65937),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([membership_65937], 0));


var G__65938 = seq__65703_65933;
var G__65939 = chunk__65704_65934;
var G__65940 = count__65705_65935;
var G__65941 = (i__65706_65936 + (1));
seq__65703_65933 = G__65938;
chunk__65704_65934 = G__65939;
count__65705_65935 = G__65940;
i__65706_65936 = G__65941;
continue;
} else {
var temp__5825__auto___65942 = cljs.core.seq(seq__65703_65933);
if(temp__5825__auto___65942){
var seq__65703_65943__$1 = temp__5825__auto___65942;
if(cljs.core.chunked_seq_QMARK_(seq__65703_65943__$1)){
var c__5673__auto___65944 = cljs.core.chunk_first(seq__65703_65943__$1);
var G__65945 = cljs.core.chunk_rest(seq__65703_65943__$1);
var G__65946 = c__5673__auto___65944;
var G__65947 = cljs.core.count(c__5673__auto___65944);
var G__65948 = (0);
seq__65703_65933 = G__65945;
chunk__65704_65934 = G__65946;
count__65705_65935 = G__65947;
i__65706_65936 = G__65948;
continue;
} else {
var membership_65949 = cljs.core.first(seq__65703_65943__$1);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$variadic(memberships_by_user,cljs.core.update,new cljs.core.Keyword(null,"userId","userId",575594135).cljs$core$IFn$_invoke$arity$1(membership_65949),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.conj,cljs.core.PersistentVector.EMPTY),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([membership_65949], 0));


var G__65950 = cljs.core.next(seq__65703_65943__$1);
var G__65951 = null;
var G__65952 = (0);
var G__65953 = (0);
seq__65703_65933 = G__65950;
chunk__65704_65934 = G__65951;
count__65705_65935 = G__65952;
i__65706_65936 = G__65953;
continue;
}
} else {
}
}
break;
}

return ({"users": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$factory_list_users_$_iter__65707(s__65708){
return (new cljs.core.LazySeq(null,(function (){
var s__65708__$1 = s__65708;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65708__$1);
if(temp__5825__auto__){
var s__65708__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65708__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65708__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65710 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65709 = (0);
while(true){
if((i__65709 < size__5627__auto__)){
var u = cljs.core._nth(c__5626__auto__,i__65709);
cljs.core.chunk_append(b__65710,(function (){var user_id = (u["id"]);
return ({"email": (u["email"]), "updatedAt": (u["updated_at"]), "credentials": cljs.core.clj__GT_js((function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(credentials_by_user,user_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()), "displayName": (u["display_name"]), "createdAt": (u["created_at"]), "authProvider": (u["auth_provider"]), "status": (u["status"]), "id": user_id, "memberships": cljs.core.clj__GT_js((function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(memberships_by_user),user_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()), "externalSubject": (u["external_subject"])});
})());

var G__65954 = (i__65709 + (1));
i__65709 = G__65954;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65710),knoxx$backend$policy_db$factory_list_users_$_iter__65707(cljs.core.chunk_rest(s__65708__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65710),null);
}
} else {
var u = cljs.core.first(s__65708__$2);
return cljs.core.cons((function (){var user_id = (u["id"]);
return ({"email": (u["email"]), "updatedAt": (u["updated_at"]), "credentials": cljs.core.clj__GT_js((function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(credentials_by_user,user_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()), "displayName": (u["display_name"]), "createdAt": (u["created_at"]), "authProvider": (u["auth_provider"]), "status": (u["status"]), "id": user_id, "memberships": cljs.core.clj__GT_js((function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(memberships_by_user),user_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()), "externalSubject": (u["external_subject"])});
})(),knoxx$backend$policy_db$factory_list_users_$_iter__65707(cljs.core.rest(s__65708__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(users);
})())});
}));
}));
}));
});
knoxx.backend.policy_db.factory_create_user = (function knoxx$backend$policy_db$factory_create_user(pool,uid,mid,payload){
var requested_payload_actor_id = knoxx.backend.policy_db.normalize_actor_id((payload["actorId"]));
var existing_actor_contract = knoxx.backend.policy_db.find_actor_contract_by_id(requested_payload_actor_id);
var provided_email = knoxx.backend.policy_db.normalize_email((payload["email"]));
var email = (function (){var or__5142__auto__ = provided_email;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(existing_actor_contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.policy_db.actor_email_from_id(requested_payload_actor_id);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})();
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())));
if(clojure.string.blank_QMARK_(email)){
return Promise.reject((new Error("email is required")));
} else {
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
var dn = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["display_name"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"label","label",1718410804).cljs$core$IFn$_invoke$arity$1(existing_actor_contract);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = requested_payload_actor_id;
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return email;
}
}
}
}
})())));
var requested_role_slugs = cljs.core.vec((function (){var or__5142__auto__ = (payload["roleSlugs"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158).cljs$core$IFn$_invoke$arity$1(existing_actor_contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return ["knowledge_worker"];
}
}
})());
var requested_actor_id = (function (){var or__5142__auto__ = requested_payload_actor_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(existing_actor_contract);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__65711 = knoxx.backend.policy_db.find_user_actor_contract_by_email(email);
if((G__65711 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(G__65711);
}
})();
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.policy_db.user_actor_id_from_email(email);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return knoxx.backend.policy_db.default_membership_actor_id(requested_role_slugs);
}
}
}
}
})();
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO users (email, display_name, auth_provider, external_subject, status) VALUES ($1, $2, $3, $4, $5) ON CONFLICT (email) DO UPDATE SET display_name = EXCLUDED.display_name, auth_provider = EXCLUDED.auth_provider, external_subject = EXCLUDED.external_subject, status = EXCLUDED.status, updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [email,(function (){var or__5142__auto__ = dn;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return email;
}
})(),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["authProvider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "local";
}
})())),(function (){var or__5142__auto__ = (payload["externalSubject"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()))], null)).then((function (user){
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO memberships (user_id, org_id, status, is_default) VALUES ($1, $2, $3, $4) ON CONFLICT (user_id, org_id) DO UPDATE SET status = EXCLUDED.status, is_default = EXCLUDED.is_default, updated_at = NOW() RETURNING *",new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [(user["id"]),org_id,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["membershipStatus"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})())),cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((payload["isDefault"]),false)], null)).then((function (ms){
return knoxx.backend.policy_db.set_membership_roles_BANG_(pool,(ms["id"]),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"role-ids","role-ids",652985101),(function (){var or__5142__auto__ = (payload["roleIds"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),requested_role_slugs,new cljs.core.Keyword(null,"replace","replace",-786587770),true], null));
})).then((function (_){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT id FROM memberships WHERE user_id = $1::uuid AND org_id = $2::uuid",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(user["id"]),org_id], null));
})).then((function (membership_row){
return knoxx.backend.policy_db.set_membership_actor_id_BANG_(pool,(membership_row["id"]),requested_actor_id).then((function (_){
return knoxx.backend.policy_db.find_org_by_id(pool,org_id);
})).then((function (org_row){
return knoxx.backend.policy_db.upsert_actor_contract_BANG_(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),requested_actor_id,new cljs.core.Keyword(null,"email","email",1415816706),provided_email,new cljs.core.Keyword(null,"display-name","display-name",694513143),dn,new cljs.core.Keyword(null,"org-slug","org-slug",-726595051),(org_row["slug"]),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),requested_role_slugs,new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"agent","agent",-766455027)], null));
}));
})).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"user.create_or_update",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"user",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),(user["id"])], null));
})).then((function (_){
return ({"user": null});
}));
}));

}
}
});
knoxx.backend.policy_db.membership_role_slugs = (function knoxx$backend$policy_db$membership_role_slugs(pool,membership_id){
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT r.slug FROM membership_roles mr JOIN roles r ON r.id = mr.role_id WHERE mr.membership_id = $1::uuid ORDER BY r.slug ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)).then((function (result){
return cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$policy_db$membership_role_slugs_$_iter__65712(s__65713){
return (new cljs.core.LazySeq(null,(function (){
var s__65713__$1 = s__65713;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__65713__$1);
if(temp__5825__auto__){
var s__65713__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__65713__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__65713__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__65715 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__65714 = (0);
while(true){
if((i__65714 < size__5627__auto__)){
var row = cljs.core._nth(c__5626__auto__,i__65714);
cljs.core.chunk_append(b__65715,(row["slug"]));

var G__65955 = (i__65714 + (1));
i__65714 = G__65955;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__65715),knoxx$backend$policy_db$membership_role_slugs_$_iter__65712(cljs.core.chunk_rest(s__65713__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__65715),null);
}
} else {
var row = cljs.core.first(s__65713__$2);
return cljs.core.cons((row["slug"]),knoxx$backend$policy_db$membership_role_slugs_$_iter__65712(cljs.core.rest(s__65713__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((result["rows"])));
})());
}));
});
knoxx.backend.policy_db.factory_update_user_actor = (function knoxx$backend$policy_db$factory_update_user_actor(pool,uid,mid,user_id,payload){
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())));
var actor_id = knoxx.backend.policy_db.normalize_actor_id((function (){var or__5142__auto__ = (payload["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["actor_id"]);
}
})());
var display_name = (function (){var G__65716 = (function (){var or__5142__auto__ = (payload["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["display_name"]);
}
})();
var G__65716__$1 = (((G__65716 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65716)));
var G__65716__$2 = (((G__65716__$1 == null))?null:clojure.string.trim(G__65716__$1));
if((G__65716__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65716__$2);
}
})();
var email = knoxx.backend.policy_db.normalize_email((payload["email"]));
var status = (function (){var G__65717 = (payload["status"]);
var G__65717__$1 = (((G__65717 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65717)));
var G__65717__$2 = (((G__65717__$1 == null))?null:clojure.string.trim(G__65717__$1));
if((G__65717__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65717__$2);
}
})();
var auth_provider = (function (){var G__65718 = (function (){var or__5142__auto__ = (payload["authProvider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["auth_provider"]);
}
})();
var G__65718__$1 = (((G__65718 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65718)));
var G__65718__$2 = (((G__65718__$1 == null))?null:clojure.string.trim(G__65718__$1));
if((G__65718__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65718__$2);
}
})();
var external_subject = (function (){var G__65719 = (function (){var or__5142__auto__ = (payload["externalSubject"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["external_subject"]);
}
})();
var G__65719__$1 = (((G__65719 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65719)));
var G__65719__$2 = (((G__65719__$1 == null))?null:clojure.string.trim(G__65719__$1));
if((G__65719__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65719__$2);
}
})();
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id)))){
return Promise.reject((new Error("userId is required")));
} else {
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.*, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.user_id = $1::uuid AND m.org_id = $2::uuid",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [user_id,org_id], null)).then((function (membership){
if(cljs.core.not(membership)){
return Promise.reject((new Error("actor membership not found")));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"UPDATE users SET email = COALESCE($2, email), display_name = COALESCE($3, display_name), status = COALESCE($4, status), auth_provider = COALESCE($5, auth_provider), external_subject = COALESCE($6, external_subject), updated_at = NOW() WHERE id = $1::uuid RETURNING *",new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [user_id,email,display_name,status,auth_provider,external_subject], null)).then((function (user){
var resolved_actor_id = (function (){var or__5142__auto__ = actor_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.policy_db.normalize_actor_id((membership["actor_id"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return knoxx.backend.policy_db.user_actor_id_from_email((user["email"]));
}
}
})();
return knoxx.backend.policy_db.set_membership_actor_id_BANG_(pool,(membership["id"]),resolved_actor_id).then((function (_){
return knoxx.backend.policy_db.membership_role_slugs(pool,(membership["id"]));
})).then((function (role_slugs){
return knoxx.backend.policy_db.upsert_actor_contract_BANG_(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),resolved_actor_id,new cljs.core.Keyword(null,"email","email",1415816706),(user["email"]),new cljs.core.Keyword(null,"display-name","display-name",694513143),(user["display_name"]),new cljs.core.Keyword(null,"org-slug","org-slug",-726595051),(membership["org_slug"]),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),role_slugs], null));
})).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"actor.update",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"user",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),user_id], null));
})).then((function (_){
return ({"user": null});
}));
}));
}
}));

}
}
});
knoxx.backend.policy_db.credential_response = (function knoxx$backend$policy_db$credential_response(credential){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"configuredFields","configuredFields",523282029),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"label","label",1718410804),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"provider","provider",-302056900)],[new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"createdAt","createdAt",-936788).cljs$core$IFn$_invoke$arity$1(credential),cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p__65720){
var vec__65721 = p__65720;
var k = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65721,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__65721,(1),null);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = v;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))){
return null;
} else {
return cljs.core.name(k);
}
}),new cljs.core.Keyword(null,"secretJson","secretJson",1807839704).cljs$core$IFn$_invoke$arity$1(credential)))),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(credential),knoxx.backend.policy_db.credential_provider_label(new cljs.core.Keyword(null,"provider","provider",-302056900).cljs$core$IFn$_invoke$arity$1(credential)),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"provider","provider",-302056900).cljs$core$IFn$_invoke$arity$1(credential)]);
});
knoxx.backend.policy_db.factory_upsert_actor_credential = (function knoxx$backend$policy_db$factory_upsert_actor_credential(pool,uid,mid,user_id,payload){
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())));
var provider = (function (){var G__65724 = (function (){var or__5142__auto__ = (payload["provider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["credentialProvider"]);
}
})();
var G__65724__$1 = (((G__65724 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65724)));
var G__65724__$2 = (((G__65724__$1 == null))?null:clojure.string.trim(G__65724__$1));
if((G__65724__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65724__$2);
}
})();
var kind = (function (){var or__5142__auto__ = (function (){var G__65725 = (payload["kind"]);
var G__65725__$1 = (((G__65725 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65725)));
var G__65725__$2 = (((G__65725__$1 == null))?null:clojure.string.trim(G__65725__$1));
if((G__65725__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65725__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "credential";
}
})();
var account_identifier = (function (){var G__65726 = (function (){var or__5142__auto__ = (payload["accountIdentifier"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["account_identifier"]);
}
})();
var G__65726__$1 = (((G__65726 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65726)));
var G__65726__$2 = (((G__65726__$1 == null))?null:clojure.string.trim(G__65726__$1));
if((G__65726__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65726__$2);
}
})();
var status = (function (){var or__5142__auto__ = (function (){var G__65727 = (payload["status"]);
var G__65727__$1 = (((G__65727 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__65727)));
var G__65727__$2 = (((G__65727__$1 == null))?null:clojure.string.trim(G__65727__$1));
if((G__65727__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__65727__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})();
var submitted_secrets = knoxx.backend.policy_db.nonblank_secret_map((function (){var or__5142__auto__ = (payload["credentials"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["secretJson"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (payload["secret_json"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return ({});
}
}
}
})());
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id)))){
return Promise.reject((new Error("userId is required")));
} else {
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider)))){
return Promise.reject((new Error("provider is required")));
} else {
var vec__65728 = knoxx.backend.policy.sql_adapter.format_sql(knoxx.backend.policy.sql_adapter.actor_membership_select_query(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"user-id","user-id",-206822291),user_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id], null)));
var seq__65729 = cljs.core.seq(vec__65728);
var first__65730 = cljs.core.first(seq__65729);
var seq__65729__$1 = cljs.core.next(seq__65729);
var sql_str = first__65730;
var params = seq__65729__$1;
return knoxx.backend.policy_db.query_one_BANG_(pool,sql_str,params).then((function (membership){
if(cljs.core.not(membership)){
return Promise.reject((new Error("actor membership not found")));
} else {
var actor_id = knoxx.backend.policy_db.normalize_actor_id((membership["actor_id"]));
return knoxx.backend.policy.protocol.upsert_actor_credential_BANG_(knoxx.backend.policy_db.sql_policy_store(pool,null),actor_id,provider,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"user-id","user-id",-206822291),user_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"kind","kind",-717265803),kind,new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),account_identifier,new cljs.core.Keyword(null,"secretJson","secretJson",1807839704),submitted_secrets,new cljs.core.Keyword(null,"status","status",-1997798413),status], null));
}
})).then((function (credential){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"actor.credential.upsert",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"actor_credential",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(credential),new cljs.core.Keyword(null,"after","after",594996914),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"provider","provider",-302056900),provider,new cljs.core.Keyword(null,"kind","kind",-717265803),kind,new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613),account_identifier,new cljs.core.Keyword(null,"configuredFields","configuredFields",523282029),cljs.core.keys(submitted_secrets)], null)], null)).then((function (_){
return ({"credential": cljs.core.clj__GT_js(knoxx.backend.policy_db.credential_response(credential))});
}));
}));

}
}
}
});
knoxx.backend.policy_db.factory_list_memberships = (function knoxx$backend$policy_db$factory_list_memberships(pool,opts){
var org_id = (opts["orgId"]);
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.org_id = $1::uuid ORDER BY m.created_at ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null)).then((function (r){
return knoxx.backend.policy_db.hydrate_memberships(pool,(r["rows"]));
})).then((function (ms){
return ({"memberships": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(ms)});
}));
}
});
knoxx.backend.policy_db.factory_get_membership = (function knoxx$backend$policy_db$factory_get_membership(pool,membership_id){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.*, o.name AS org_name, o.slug AS org_slug FROM memberships m JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)).then((function (row){
if(cljs.core.truth_(row)){
return knoxx.backend.policy_db.hydrate_memberships(pool,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [row], null)).then((function (h){
return ({"membership": cljs.core.first(h)});
}));
} else {
return ({"membership": null});
}
}));
});
knoxx.backend.policy_db.factory_set_membership_roles = (function knoxx$backend$policy_db$factory_set_membership_roles(pool,uid,mid,membership_id,payload){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM memberships WHERE id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)).then((function (ms){
if(cljs.core.not(ms)){
return Promise.reject((new Error("membership not found")));
} else {
return knoxx.backend.policy_db.set_membership_roles_BANG_(pool,membership_id,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"org-id","org-id",1485182668),(ms["org_id"]),new cljs.core.Keyword(null,"role-ids","role-ids",652985101),(function (){var or__5142__auto__ = (payload["roleIds"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),(function (){var or__5142__auto__ = (payload["roleSlugs"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})(),new cljs.core.Keyword(null,"replace","replace",-786587770),cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((payload["replace"]),false)], null)).then((function (_){
var requested_role_slugs = cljs.core.vec((function (){var or__5142__auto__ = (payload["roleSlugs"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})());
var requested_actor_id = (function (){var or__5142__auto__ = knoxx.backend.policy_db.normalize_actor_id((payload["actorId"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.policy_db.normalize_actor_id((ms["actor_id"]));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.seq(requested_role_slugs)){
return knoxx.backend.policy_db.default_membership_actor_id(requested_role_slugs);
} else {
return "workspace_user";
}
}
}
})();
return knoxx.backend.policy_db.set_membership_actor_id_BANG_(pool,membership_id,requested_actor_id).then((function (___$1){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT m.id, u.email, u.display_name, o.slug AS org_slug FROM memberships m JOIN users u ON u.id = m.user_id JOIN orgs o ON o.id = m.org_id WHERE m.id = $1::uuid",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null));
})).then((function (membership_row){
return knoxx.backend.policy_db.upsert_actor_contract_BANG_(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),requested_actor_id,new cljs.core.Keyword(null,"email","email",1415816706),(membership_row["email"]),new cljs.core.Keyword(null,"display-name","display-name",694513143),(membership_row["display_name"]),new cljs.core.Keyword(null,"org-slug","org-slug",-726595051),(membership_row["org_slug"]),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),requested_role_slugs], null));
}));
}),(function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),(ms["org_id"]),new cljs.core.Keyword(null,"action","action",-811238024),"membership.roles.update",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"membership",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),membership_id], null));
}).then(),(function (_){
return ({"membership": null});
}).then());
}
}));
});
knoxx.backend.policy_db.factory_set_membership_tool_policies = (function knoxx$backend$policy_db$factory_set_membership_tool_policies(pool,uid,mid,membership_id,payload){
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM memberships WHERE id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [membership_id], null)).then((function (ms){
if(cljs.core.not(ms)){
return Promise.reject((new Error("membership not found")));
} else {
return knoxx.backend.policy_db.set_membership_tool_policies_BANG_(pool,membership_id,(function (){var or__5142__auto__ = (payload["toolPolicies"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return [];
}
})()).then((function (_){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),(ms["org_id"]),new cljs.core.Keyword(null,"action","action",-811238024),"membership.tool_policy.update",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"membership",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),membership_id], null));
})).then((function (_){
return ({"membership": null});
}));
}
}));
});
knoxx.backend.policy_db.factory_list_data_lakes = (function knoxx$backend$policy_db$factory_list_data_lakes(pool,opts){
var org_id = (opts["orgId"]);
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM data_lakes WHERE org_id = $1::uuid ORDER BY name ASC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null)).then((function (r){
return ({"dataLakes": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.__GT_js_data_lake,(r["rows"])))});
}));
}
});
knoxx.backend.policy_db.factory_create_data_lake = (function knoxx$backend$policy_db$factory_create_data_lake(pool,uid,mid,payload){
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (payload["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())));
var name = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
if(clojure.string.blank_QMARK_(name)){
return Promise.reject((new Error("name is required")));
} else {
var slug = knoxx.backend.policy_db.slugify((function (){var or__5142__auto__ = (payload["slug"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return name;
}
})(),"lake");
var kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "workspace_docs";
}
})()));
var status = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["status"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "active";
}
})()));
var config = knoxx.backend.policy_db.normalize_lake_config((function (){var or__5142__auto__ = (payload["config"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (payload["config_json"]);
}
})());
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO data_lakes (org_id, name, slug, kind, config_json, status) VALUES ($1, $2, $3, $4, $5::jsonb, $6) RETURNING *",new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id,name,slug,kind,JSON.stringify(cljs.core.clj__GT_js(config)),status], null)).then((function (lake){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"data_lake.create",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"data_lake",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),(lake["id"])], null)).then((function (_){
return ({"dataLake": knoxx.backend.policy_db.__GT_js_data_lake(lake)});
}));
}));

}
}
});
knoxx.backend.policy_db.hash_token_with_salt = (function knoxx$backend$policy_db$hash_token_with_salt(token,salt){
var h = shadow.esm.esm_import$node_crypto.createHash("sha256");
h.update((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(salt)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)),"utf8");

return h.digest("hex");
});
knoxx.backend.policy_db.token_prefix = (function knoxx$backend$policy_db$token_prefix(token){
var h = shadow.esm.esm_import$node_crypto.createHash("sha256");
h.update((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(token)),"utf8");

return cljs.core.subs.cljs$core$IFn$_invoke$arity$3(h.digest("hex"),(0),(12));
});
knoxx.backend.policy_db.generate_salt = (function knoxx$backend$policy_db$generate_salt(){
return shadow.esm.esm_import$node_crypto.randomBytes((16)).toString("hex");
});
knoxx.backend.policy_db.factory_create_session = (function knoxx$backend$policy_db$factory_create_session(pool,session_data){
var token = (function (){var or__5142__auto__ = (session_data["token"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var ttl_secs = parseInt((function (){var or__5142__auto__ = (process.env["KNOXX_SESSION_TTL_SECONDS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "86400";
}
})(),(10));
var salt = knoxx.backend.policy_db.generate_salt();
var token_hash = knoxx.backend.policy_db.hash_token_with_salt(token,salt);
var prefix = knoxx.backend.policy_db.token_prefix(token);
var expires_at = (new Date((Date.now() + (ttl_secs * (1000)))));
if(clojure.string.blank_QMARK_(token)){
return Promise.reject((new Error("token is required for session creation")));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO sessions (user_id, membership_id, org_id, token_hash, token_prefix, salt, email, display_name, auth_provider, external_subject, ip_address, user_agent, expires_at) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13) RETURNING *",new cljs.core.PersistentVector(null, 13, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (){var or__5142__auto__ = (session_data["userId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (session_data["membershipId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (session_data["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),token_hash,prefix,salt,(function (){var or__5142__auto__ = (session_data["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (session_data["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(function (){var or__5142__auto__ = (session_data["authProvider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "github";
}
})(),(function (){var or__5142__auto__ = (session_data["externalSubject"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),(function (){var or__5142__auto__ = (session_data["ipAddress"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),(function (){var or__5142__auto__ = (session_data["userAgent"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(),expires_at.toISOString()], null)).then((function (row){
return ({"session": ({"email": (row["email"]), "orgId": (row["org_id"]), "expiresAt": (row["expires_at"]), "displayName": (row["display_name"]), "createdAt": (row["created_at"]), "authProvider": (row["auth_provider"]), "id": (row["id"]), "membershipId": (row["membership_id"]), "userId": (row["user_id"])})});
}));
}
});
knoxx.backend.policy_db.find_session_in_rows = (function knoxx$backend$policy_db$find_session_in_rows(pool,token,rows){
var i = (0);
while(true){
if((i >= rows.length)){
return null;
} else {
var row = (rows[i]);
var h = (row["token_hash"]);
var s = (row["salt"]);
var c = knoxx.backend.policy_db.hash_token_with_salt(token,s);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(h,c)){
knoxx.backend.policy_db.query_BANG_(pool,"UPDATE sessions SET last_seen_at = NOW() WHERE id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(row["id"])], null)).catch(((function (i,row,h,s,c){
return (function (_err){
return null;
});})(i,row,h,s,c))
);

return ({"session": ({"email": (row["email"]), "orgId": (row["org_id"]), "expiresAt": (row["expires_at"]), "displayName": (row["display_name"]), "createdAt": (row["created_at"]), "authProvider": (row["auth_provider"]), "id": (row["id"]), "membershipId": (row["membership_id"]), "userId": (row["user_id"])})});
} else {
var G__65956 = (i + (1));
i = G__65956;
continue;
}
}
break;
}
});
knoxx.backend.policy_db.factory_get_session_by_token = (function knoxx$backend$policy_db$factory_get_session_by_token(pool,token){
if(clojure.string.blank_QMARK_(token)){
return Promise.resolve(null);
} else {
var prefix = knoxx.backend.policy_db.token_prefix(token);
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM sessions WHERE token_prefix = $1 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 50",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [prefix], null)).then((function (result){
var rows = (result["rows"]);
var found = knoxx.backend.policy_db.find_session_in_rows(pool,token,rows);
if(cljs.core.truth_(found)){
return found;
} else {
return knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM sessions WHERE expires_at > NOW() ORDER BY created_at DESC LIMIT 200",cljs.core.PersistentVector.EMPTY).then((function (fallback_result){
return knoxx.backend.policy_db.find_session_in_rows(pool,token,(fallback_result["rows"]));
}));
}
})).catch((function (_err){
return null;
}));
}
});
knoxx.backend.policy_db.factory_delete_session_by_token = (function knoxx$backend$policy_db$factory_delete_session_by_token(pool,token){
return knoxx.backend.policy_db.factory_get_session_by_token(pool,token).then((function (result){
if(cljs.core.truth_((function (){var and__5140__auto__ = result;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = (result["session"]);
if(cljs.core.truth_(and__5140__auto____$1)){
return (result["session"]["id"]);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM sessions WHERE id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(result["session"]["id"])], null)).catch((function (_){
return null;
}));
} else {
}

return result;
}));
});
knoxx.backend.policy_db.factory_cleanup_expired_sessions = (function knoxx$backend$policy_db$factory_cleanup_expired_sessions(pool){
return knoxx.backend.policy_db.query_BANG_(pool,"DELETE FROM sessions WHERE expires_at < NOW()",cljs.core.PersistentVector.EMPTY).then((function (result){
var count = (function (){var or__5142__auto__ = (result["rowCount"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
if((count > (0))){
console.log((""+"[knoxx-policy-db] Cleaned up "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(count)+" expired sessions"));
} else {
}

return count;
})).catch((function (_err){
return (0);
}));
});
knoxx.backend.policy_db.factory_create_invite = (function knoxx$backend$policy_db$factory_create_invite(pool,uid,mid,payload){
var org_id = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var email = clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (payload["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
var role_slugs = (function (){var or__5142__auto__ = (payload["roleSlugs"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ["knowledge_worker"];
}
})();
var role_slugs_array = (cljs.core.truth_(Array.isArray(role_slugs))?role_slugs:((cljs.core.sequential_QMARK_(role_slugs))?cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_slugs):["knowledge_worker"]
));
var inviter_mid = (function (){var or__5142__auto__ = (payload["inviterMembershipId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return mid;
}
})();
var code = shadow.esm.esm_import$node_crypto.randomBytes((8)).toString("hex");
var ttl_secs = (((7) * (24)) * (3600));
var expires_at = (new Date((Date.now() + (ttl_secs * (1000)))));
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
if(clojure.string.blank_QMARK_(email)){
return Promise.reject((new Error("email is required")));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"INSERT INTO invites (org_id, code, email, inviter_membership_id, role_slugs, status, expires_at) VALUES ($1, $2, $3, $4, $5::jsonb, 'pending', $6) RETURNING *",new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id,code,email,inviter_mid,JSON.stringify(role_slugs_array),expires_at.toISOString()], null)).then((function (row){
return knoxx.backend.policy_db.append_audit_BANG_(pool,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"actor-user-id","actor-user-id",584299995),uid,new cljs.core.Keyword(null,"actor-membership-id","actor-membership-id",291606239),mid,new cljs.core.Keyword(null,"org-id","org-id",1485182668),org_id,new cljs.core.Keyword(null,"action","action",-811238024),"invite.create",new cljs.core.Keyword(null,"resource-kind","resource-kind",-2119603299),"invite",new cljs.core.Keyword(null,"resource-id","resource-id",-1308422582),(row["id"])], null)).then((function (_){
return ({"invite": ({"id": (row["id"]), "orgId": (row["org_id"]), "code": code, "email": email, "status": (row["status"]), "expiresAt": (row["expires_at"]), "createdAt": (row["created_at"])})});
}));
}));

}
}
});
knoxx.backend.policy_db.factory_redeem_invite = (function knoxx$backend$policy_db$factory_redeem_invite(pool,code,email){
if(((clojure.string.blank_QMARK_(code)) || (clojure.string.blank_QMARK_(email)))){
return Promise.reject((new Error("code and email are required")));
} else {
return knoxx.backend.policy_db.query_one_BANG_(pool,"SELECT * FROM invites WHERE code = $1 AND status = 'pending' AND expires_at > NOW()",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [code], null)).then((function (invite){
if(cljs.core.not(invite)){
return Promise.reject((function (){var err = (new Error("Invalid or expired invite code"));
(err.status = (400));

return err;
})());
} else {
var invite_id = (invite["id"]);
var invite_email = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((invite["email"]))));
var normalized_email = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email)));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(invite_email,normalized_email)){
} else {
throw (function (){var err = (new Error("Invite email does not match"));
(err.status = (403));

return err;
})();
}

return knoxx.backend.policy_db.query_one_BANG_(pool,"UPDATE invites SET status = 'redeemed', redeemed_at = NOW() WHERE id = $1 RETURNING *",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [invite_id], null)).then((function (updated){
return ({"invite": ({"id": (updated["id"]), "orgId": (updated["org_id"]), "code": code, "email": (updated["email"]), "status": (updated["status"]), "redeemedAt": (updated["redeemed_at"]), "createdAt": (updated["created_at"])}), "user": null});
}));
}
}));
}
});
knoxx.backend.policy_db.factory_list_invites = (function knoxx$backend$policy_db$factory_list_invites(pool,opts){
var org_id = (opts["orgId"]);
var status_filter = (opts["status"]);
if(clojure.string.blank_QMARK_(org_id)){
return Promise.reject((new Error("orgId is required")));
} else {
return (cljs.core.truth_(status_filter)?knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM invites WHERE org_id = $1::uuid AND status = $2 ORDER BY created_at DESC",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id,status_filter], null)):knoxx.backend.policy_db.query_BANG_(pool,"SELECT * FROM invites WHERE org_id = $1::uuid ORDER BY created_at DESC",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [org_id], null))).then((function (result){
var rows = (result["rows"]);
var invites = [];
var n__5741__auto___65957 = rows.length;
var i_65958 = (0);
while(true){
if((i_65958 < n__5741__auto___65957)){
var row_65959 = (rows[i_65958]);
var role_slugs_65960 = (function (){try{var v = (row_65959["role_slugs"]);
if((v == null)){
return cljs.core.PersistentVector.EMPTY;
} else {
if(typeof v === 'string'){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(JSON.parse(v));
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(v);

}
}
}catch (e65731){var _ = e65731;
return cljs.core.PersistentVector.EMPTY;
}})();
invites.push(({"email": (row_65959["email"]), "orgId": (row_65959["org_id"]), "expiresAt": (row_65959["expires_at"]), "createdAt": (row_65959["created_at"]), "redeemedAt": (row_65959["redeemed_at"]), "status": (row_65959["status"]), "id": (row_65959["id"]), "code": (row_65959["code"]), "roleSlugs": cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(role_slugs_65960)}));

var G__65961 = (i_65958 + (1));
i_65958 = G__65961;
continue;
} else {
}
break;
}

return ({"invites": invites});
}));
}
});
knoxx.backend.policy_db.create_policy_db = (function knoxx$backend$policy_db$create_policy_db(options){
var conn_str = (function (){var or__5142__auto__ = (options["connectionString"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"connectionString","connectionString",631814971).cljs$core$IFn$_invoke$arity$1(options);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
if(clojure.string.blank_QMARK_(conn_str)){
return Promise.resolve(null);
} else {
return (new Promise((function (resolve,reject){
var pool_config = cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"connectionString","connectionString",631814971),conn_str,new cljs.core.Keyword(null,"max","max",61366548),(20),new cljs.core.Keyword(null,"idleTimeoutMillis","idleTimeoutMillis",-229649233),(30000),new cljs.core.Keyword(null,"connectionTimeoutMillis","connectionTimeoutMillis",-1543900585),(5000),new cljs.core.Keyword(null,"allowExitOnIdle","allowExitOnIdle",-916282161),true], null));
var pool = (new shadow.esm.esm_import$pg.Pool(pool_config));
pool.on("error",(function (err,_client){
return console.error("[policy-db] Unexpected PG pool error:",err.message);
}));

pool.on("connect",(function (_client){
return console.log("[policy-db] New PG client connected");
}));

return knoxx.backend.policy_db.ensure_schema_BANG_(pool).then((function (_){
return knoxx.backend.policy_db.insert_permission_seeds_BANG_(pool);
})).then((function (_){
return knoxx.backend.policy_db.insert_tool_seeds_BANG_(pool);
})).then((function (_){
return knoxx.backend.policy_db.ensure_primary_org_BANG_(pool,options);
})).then((function (primary_org){
return knoxx.backend.policy_db.sync_contract_role_projections_BANG_(pool).then((function (_){
return knoxx.backend.policy_db.ensure_bootstrap_user_BANG_(pool,primary_org,options);
})).then((function (bootstrap){
return knoxx.backend.policy_db.ensure_bootstrap_allowlist_users_BANG_(pool,primary_org,options).catch((function (err){
console.warn("[policy-db] bootstrap allowlist failed:",err.message);

return null;
})).then((function (_){
return bootstrap;
}));
})).then((function (bootstrap){
return knoxx.backend.policy_db.sync_user_actors_from_contracts_BANG_(pool,primary_org).catch((function (err){
console.warn("[policy-db] user actor sync failed:",err.message);

return null;
})).then((function (_){
return bootstrap;
}));
})).then((function (bootstrap){
return knoxx.backend.policy_db.backfill_membership_actors_BANG_(pool).then((function (_){
return bootstrap;
}));
})).then((function (bootstrap){
knoxx.backend.policy_db.factory_cleanup_expired_sessions(pool).catch((function (_){
return null;
}));

var uid = (bootstrap["user"]["id"]);
var mid = (bootstrap["membership"]["id"]);
var G__65732 = ({"listMemberships": (function (opts){
return knoxx.backend.policy_db.factory_list_memberships(pool,opts);
}), "evaluateToolAccess": (function (headers_like,tool_id){
return knoxx.backend.policy_db.factory_evaluate_tool_access(pool,headers_like,tool_id);
}), "setMembershipToolPolicies": (function (membership_id,payload){
return knoxx.backend.policy_db.factory_set_membership_tool_policies(pool,uid,mid,membership_id,payload);
}), "redeemInvite": (function (code,email){
return knoxx.backend.policy_db.factory_redeem_invite(pool,code,email);
}), "listDataLakes": (function (opts){
return knoxx.backend.policy_db.factory_list_data_lakes(pool,opts);
}), "listTools": (function (){
return knoxx.backend.policy_db.factory_list_tools(pool);
}), "createInvite": (function (payload){
return knoxx.backend.policy_db.factory_create_invite(pool,uid,mid,payload);
}), "syncUserFromActorContract": (function (payload){
return knoxx.backend.policy_db.sync_user_from_actor_contract_BANG_(pool,primary_org,payload);
}), "getMembership": (function (membership_id){
return knoxx.backend.policy_db.factory_get_membership(pool,membership_id);
}), "listUsers": (function (opts){
return knoxx.backend.policy_db.factory_list_users(pool,opts);
}), "listOrgs": (function (){
return knoxx.backend.policy_db.factory_list_orgs(pool);
}), "createDataLake": (function (payload){
return knoxx.backend.policy_db.factory_create_data_lake(pool,uid,mid,payload);
}), "getBootstrapContext": (function (){
return knoxx.backend.policy_db.factory_get_bootstrap_context(pool,primary_org,bootstrap);
}), "syncActorContracts": (function (){
return knoxx.backend.policy_db.sync_user_actors_from_contracts_BANG_(pool,primary_org);
}), "upsertActorCredential": (function (user_id,payload){
return knoxx.backend.policy_db.factory_upsert_actor_credential(pool,uid,mid,user_id,payload);
}), "getRole": (function (role_id){
return knoxx.backend.policy_db.factory_get_role(pool,role_id);
}), "getSessionByToken": (function (token){
return knoxx.backend.policy_db.factory_get_session_by_token(pool,token);
}), "resolveRequestContext": (function (headers_like){
return knoxx.backend.policy_db.factory_resolve_request_context(pool,headers_like);
}), "close": (function (){
return pool.end();
}), "listRoles": (function (opts){
return knoxx.backend.policy_db.factory_list_roles(pool,opts);
}), "createRole": (function (payload){
return knoxx.backend.policy_db.factory_create_role(pool,uid,mid,payload);
}), "cleanupExpiredSessions": (function (){
return knoxx.backend.policy_db.factory_cleanup_expired_sessions(pool);
}), "getActorCredential": (function (actor_id,provider){
return knoxx.backend.policy_db.factory_get_actor_credential(pool,actor_id,provider);
}), "setRoleToolPolicies": (function (role_id,payload){
return knoxx.backend.policy_db.factory_set_role_tool_policies(pool,uid,mid,role_id,payload);
}), "updateUserActor": (function (user_id,payload){
return knoxx.backend.policy_db.factory_update_user_actor(pool,uid,mid,user_id,payload);
}), "listInvites": (function (opts){
return knoxx.backend.policy_db.factory_list_invites(pool,opts);
}), "createUser": (function (payload){
return knoxx.backend.policy_db.factory_create_user(pool,uid,mid,payload);
}), "setMembershipRoles": (function (membership_id,payload){
return knoxx.backend.policy_db.factory_set_membership_roles(pool,uid,mid,membership_id,payload);
}), "listPermissions": (function (){
return knoxx.backend.policy_db.factory_list_permissions(pool);
}), "listActorCredentials": (function (provider){
return knoxx.backend.policy_db.factory_list_actor_credentials(pool,provider);
}), "deleteSessionByToken": (function (token){
return knoxx.backend.policy_db.factory_delete_session_by_token(pool,token);
}), "query": (function (sql,params){
return knoxx.backend.policy_db.query_BANG_(pool,sql,params);
}), "createSession": (function (session_data){
return knoxx.backend.policy_db.factory_create_session(pool,session_data);
}), "createOrg": (function (payload){
return knoxx.backend.policy_db.factory_create_org(pool,uid,mid,payload);
})});
return (resolve.cljs$core$IFn$_invoke$arity$1 ? resolve.cljs$core$IFn$_invoke$arity$1(G__65732) : resolve.call(null,G__65732));
}));
})).catch(reject);
})));
}
});

//# sourceMappingURL=knoxx.backend.policy_db.js.map
