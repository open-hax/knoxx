import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.runtime.state.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.actor_mailbox');
knoxx.backend.actor_mailbox.mailbox_statuses = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 6, ["expired",null,"failed",null,"delivered",null,"acknowledged",null,"pending",null,"superseded",null], null), null);
knoxx.backend.actor_mailbox.mailbox_delivery_modes = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 5, ["follow-up",null,"event",null,"direct-run",null,"steer",null,"inbox-only",null], null), null);
knoxx.backend.actor_mailbox.nonblank = (function knoxx$backend$actor_mailbox$nonblank(value){
var G__517224 = value;
var G__517224__$1 = (((G__517224 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__517224)));
var G__517224__$2 = (((G__517224__$1 == null))?null:clojure.string.trim(G__517224__$1));
if((G__517224__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__517224__$2);
}
});
knoxx.backend.actor_mailbox.normalize_status = (function knoxx$backend$actor_mailbox$normalize_status(status){
var status_STAR_ = (function (){var G__517231 = status;
var G__517231__$1 = (((G__517231 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__517231)));
var G__517231__$2 = (((G__517231__$1 == null))?null:clojure.string.trim(G__517231__$1));
var G__517231__$3 = (((G__517231__$2 == null))?null:clojure.string.lower_case(G__517231__$2));
if((G__517231__$3 == null)){
return null;
} else {
return clojure.string.replace(G__517231__$3,/_/,"-");
}
})();
if(cljs.core.contains_QMARK_(knoxx.backend.actor_mailbox.mailbox_statuses,status_STAR_)){
return status_STAR_;
} else {
return "pending";
}
});
knoxx.backend.actor_mailbox.normalize_delivery_mode = (function knoxx$backend$actor_mailbox$normalize_delivery_mode(mode){
var mode_STAR_ = (function (){var G__517239 = mode;
var G__517239__$1 = (((G__517239 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__517239)));
var G__517239__$2 = (((G__517239__$1 == null))?null:clojure.string.trim(G__517239__$1));
var G__517239__$3 = (((G__517239__$2 == null))?null:clojure.string.lower_case(G__517239__$2));
if((G__517239__$3 == null)){
return null;
} else {
return clojure.string.replace(G__517239__$3,/_/,"-");
}
})();
var G__517241 = mode_STAR_;
switch (G__517241) {
case "message":
return "follow-up";

break;
case "followup":
return "follow-up";

break;
default:
if(cljs.core.contains_QMARK_(knoxx.backend.actor_mailbox.mailbox_delivery_modes,mode_STAR_)){
return mode_STAR_;
} else {
return "follow-up";
}

}
});
knoxx.backend.actor_mailbox.preview_text = (function knoxx$backend$actor_mailbox$preview_text(content){
var text = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = content;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if((((text).length) > (240))){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(text,(0),(240)))+"\u2026");
} else {
return text;
}
});
knoxx.backend.actor_mailbox.mailbox_event_id = (function knoxx$backend$actor_mailbox$mailbox_event_id(mailbox_id){
return (""+"actor-mailbox-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(mailbox_id));
});
knoxx.backend.actor_mailbox.new_mailbox_id = (function knoxx$backend$actor_mailbox$new_mailbox_id(){
return shadow.esm.esm_import$node_crypto.randomUUID();
});
knoxx.backend.actor_mailbox.source_from_context = (function knoxx$backend$actor_mailbox$source_from_context(ctx){
var agent_spec = new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(ctx);
var actor_id = (function (){var or__5142__auto__ = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(agent_spec));
}
}
}
})();
var contract_id = (function (){var or__5142__auto__ = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(ctx));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"contractId","contractId",710260199).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193).cljs$core$IFn$_invoke$arity$1(agent_spec));
}
}
}
})();
var G__517257 = cljs.core.PersistentArrayMap.EMPTY;
var G__517257__$1 = (cljs.core.truth_(actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517257,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),actor_id):G__517257);
var G__517257__$2 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(ctx)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517257__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(ctx))):G__517257__$1);
var G__517257__$3 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(ctx)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517257__$2,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(ctx))):G__517257__$2);
var G__517257__$4 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(ctx)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517257__$3,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(ctx))):G__517257__$3);
if(cljs.core.truth_(contract_id)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517257__$4,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),contract_id);
} else {
return G__517257__$4;
}
});
knoxx.backend.actor_mailbox.normalize_target_map = (function knoxx$backend$actor_mailbox$normalize_target_map(target){
var kind = (function (){var or__5142__auto__ = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"target-type","target-type",-1795727181).cljs$core$IFn$_invoke$arity$1(target));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target)))?"actor":(cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target)))?"session":(cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target)))?"conversation":null
)));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(target));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "unknown";
}
}
}
})();
var G__517263 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"kind","kind",-717265803),kind], null);
var G__517263__$1 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517263,new cljs.core.Keyword(null,"actor-id","actor-id",897721067),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target))):G__517263);
var G__517263__$2 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517263__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target))):G__517263__$1);
var G__517263__$3 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517263__$2,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target))):G__517263__$2);
var G__517263__$4 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517263__$3,new cljs.core.Keyword(null,"run-id","run-id",-1745267908),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target))):G__517263__$3);
if(cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(target)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517263__$4,new cljs.core.Keyword(null,"address","address",559499426),knoxx.backend.actor_mailbox.nonblank(new cljs.core.Keyword(null,"target","target",253001721).cljs$core$IFn$_invoke$arity$1(target)));
} else {
return G__517263__$4;
}
});
knoxx.backend.actor_mailbox.mailbox_entry = (function knoxx$backend$actor_mailbox$mailbox_entry(p__517275){
var map__517276 = p__517275;
var map__517276__$1 = cljs.core.__destructure_map(map__517276);
var target = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"target","target",253001721));
var content_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"content-ref","content-ref",1710065788));
var metadata = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"metadata","metadata",1799301597));
var next_at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"next-at","next-at",1830929216));
var expires_at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"expires-at","expires-at",1654982210));
var attempts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"attempts","attempts",1024246729));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"source","source",-433931539));
var delivery_mode = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"delivery-mode","delivery-mode",2042238834));
var preview = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"preview","preview",451279890));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"id","id",-1388402092));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517276__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
var entry_id = (function (){var or__5142__auto__ = knoxx.backend.actor_mailbox.nonblank(id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.actor_mailbox.new_mailbox_id();
}
})();
var G__517279 = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344),entry_id,new cljs.core.Keyword("mailbox","kind","mailbox/kind",401992993),(function (){var or__5142__auto__ = knoxx.backend.actor_mailbox.nonblank(kind);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "actor-message";
}
})(),new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881),knoxx.backend.actor_mailbox.normalize_status(status),new cljs.core.Keyword("mailbox","source","mailbox/source",-1264954567),(function (){var or__5142__auto__ = source;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword("mailbox","target","mailbox/target",1100093613),knoxx.backend.actor_mailbox.normalize_target_map((function (){var or__5142__auto__ = target;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"mode","mode",654403691),knoxx.backend.actor_mailbox.normalize_delivery_mode(delivery_mode),new cljs.core.Keyword(null,"attempts","attempts",1024246729),(function (){var or__5142__auto__ = attempts;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()], null),new cljs.core.Keyword("mailbox","content-ref","mailbox/content-ref",877031624),(function (){var or__5142__auto__ = content_ref;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword("mailbox","metadata","mailbox/metadata",-1698257615),(function (){var or__5142__auto__ = metadata;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], null);
var G__517279__$1 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(preview))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517279,new cljs.core.Keyword("mailbox","preview","mailbox/preview",-512838338),knoxx.backend.actor_mailbox.preview_text(preview)):G__517279);
var G__517279__$2 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(next_at))?cljs.core.assoc_in(G__517279__$1,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392),new cljs.core.Keyword(null,"next-at","next-at",1830929216)], null),knoxx.backend.actor_mailbox.nonblank(next_at)):G__517279__$1);
if(cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank(expires_at))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517279__$2,new cljs.core.Keyword("mailbox","expires-at","mailbox/expires-at",-1256489474),knoxx.backend.actor_mailbox.nonblank(expires_at));
} else {
return G__517279__$2;
}
});
knoxx.backend.actor_mailbox.policy_db = (function knoxx$backend$actor_mailbox$policy_db(_runtime){
return knoxx.backend.runtime.state.current_policy_db();
});
knoxx.backend.actor_mailbox.database_enabled_QMARK_ = (function knoxx$backend$actor_mailbox$database_enabled_QMARK_(runtime){
return cljs.core.boolean$((function (){var and__5140__auto__ = knoxx.backend.actor_mailbox.policy_db(runtime);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.fn_QMARK_((knoxx.backend.actor_mailbox.policy_db(runtime)["query"]));
} else {
return and__5140__auto__;
}
})());
});
knoxx.backend.actor_mailbox.query_BANG_ = (function knoxx$backend$actor_mailbox$query_BANG_(runtime,sql,params){
var temp__5823__auto__ = knoxx.backend.actor_mailbox.policy_db(runtime);
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var temp__5823__auto____$1 = (db["query"]);
if(cljs.core.truth_(temp__5823__auto____$1)){
var query_fn = temp__5823__auto____$1;
var G__517310 = sql;
var G__517311 = cljs.core.clj__GT_js(params);
return (query_fn.cljs$core$IFn$_invoke$arity$2 ? query_fn.cljs$core$IFn$_invoke$arity$2(G__517310,G__517311) : query_fn.call(null,G__517310,G__517311));
} else {
return Promise.resolve(null);
}
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actor_mailbox.json_param = (function knoxx$backend$actor_mailbox$json_param(value){
return JSON.stringify(cljs.core.clj__GT_js((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()));
});
knoxx.backend.actor_mailbox.rows = (function knoxx$backend$actor_mailbox$rows(result){
var rows_STAR_ = (result["rows"]);
if(cljs.core.truth_(cljs.core.array_QMARK_(rows_STAR_))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(rows_STAR_);
} else {
return cljs.core.PersistentVector.EMPTY;
}
});
knoxx.backend.actor_mailbox.row__GT_route = (function knoxx$backend$actor_mailbox$row__GT_route(row){
if(cljs.core.truth_(row)){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"actor-id","actor-id",897721067),(row["actor_id"]),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(row["conversation_id"]),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(row["session_id"]),new cljs.core.Keyword(null,"run-id","run-id",-1745267908),(row["run_id"]),new cljs.core.Keyword(null,"contract-id","contract-id",-855048622),(row["contract_id"]),new cljs.core.Keyword(null,"status","status",-1997798413),(row["status"]),new cljs.core.Keyword(null,"last-seen-at","last-seen-at",1929467667),(row["last_seen_at"])], null);
} else {
return null;
}
});
knoxx.backend.actor_mailbox.register_live_session_BANG_ = (function knoxx$backend$actor_mailbox$register_live_session_BANG_(runtime,p__517317){
var map__517318 = p__517317;
var map__517318__$1 = cljs.core.__destructure_map(map__517318);
var actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"run-id","run-id",-1745267908));
var contract_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"contract-id","contract-id",-855048622));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"source","source",-433931539));
var expires_at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517318__$1,new cljs.core.Keyword(null,"expires-at","expires-at",1654982210));
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(actor_id);
if(cljs.core.truth_(temp__5823__auto__)){
var actor_id_STAR_ = temp__5823__auto__;
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"INSERT INTO actor_mailbox_routes (actor_id, conversation_id, session_id, run_id, contract_id, status, source_json, expires_at, last_seen_at)\n             VALUES ($1, $2, $3, $4, $5, 'active', $6::jsonb, COALESCE($7::timestamptz, NOW() + interval '30 minutes'), NOW())\n             ON CONFLICT (actor_id) DO UPDATE SET\n               conversation_id = EXCLUDED.conversation_id,\n               session_id = EXCLUDED.session_id,\n               run_id = EXCLUDED.run_id,\n               contract_id = EXCLUDED.contract_id,\n               status = 'active',\n               source_json = EXCLUDED.source_json,\n               expires_at = EXCLUDED.expires_at,\n               last_seen_at = NOW()\n             RETURNING *",new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [actor_id_STAR_,knoxx.backend.actor_mailbox.nonblank(conversation_id),knoxx.backend.actor_mailbox.nonblank(session_id),knoxx.backend.actor_mailbox.nonblank(run_id),knoxx.backend.actor_mailbox.nonblank(contract_id),knoxx.backend.actor_mailbox.json_param(source),knoxx.backend.actor_mailbox.nonblank(expires_at)], null));
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actor_mailbox.unregister_live_session_BANG_ = (function knoxx$backend$actor_mailbox$unregister_live_session_BANG_(runtime,conversation_id){
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(conversation_id);
if(cljs.core.truth_(temp__5823__auto__)){
var conversation_id_STAR_ = temp__5823__auto__;
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"UPDATE actor_mailbox_routes\n             SET status = 'inactive', last_seen_at = NOW()\n             WHERE conversation_id = $1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [conversation_id_STAR_], null));
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actor_mailbox.resolve_actor_session_BANG_ = (function knoxx$backend$actor_mailbox$resolve_actor_session_BANG_(runtime,actor_id){
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(actor_id);
if(cljs.core.truth_(temp__5823__auto__)){
var actor_id_STAR_ = temp__5823__auto__;
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"SELECT * FROM actor_mailbox_routes\n                 WHERE actor_id = $1\n                   AND status = 'active'\n                   AND (expires_at IS NULL OR expires_at > NOW())\n                 ORDER BY last_seen_at DESC\n                 LIMIT 1",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [actor_id_STAR_], null)).then((function (result){
var G__517325 = cljs.core.first(knoxx.backend.actor_mailbox.rows(result));
if((G__517325 == null)){
return null;
} else {
return knoxx.backend.actor_mailbox.row__GT_route(G__517325);
}
}));
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actor_mailbox.create_entry_BANG_ = (function knoxx$backend$actor_mailbox$create_entry_BANG_(runtime,raw_entry){
var entry = knoxx.backend.actor_mailbox.mailbox_entry(raw_entry);
var target = new cljs.core.Keyword("mailbox","target","mailbox/target",1100093613).cljs$core$IFn$_invoke$arity$1(entry);
var source = new cljs.core.Keyword("mailbox","source","mailbox/source",-1264954567).cljs$core$IFn$_invoke$arity$1(entry);
var delivery = new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392).cljs$core$IFn$_invoke$arity$1(entry);
if((!(knoxx.backend.actor_mailbox.database_enabled_QMARK_(runtime)))){
return Promise.resolve(cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(entry,new cljs.core.Keyword("mailbox","durable?","mailbox/durable?",771149223),false));
} else {
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"INSERT INTO actor_mailbox_entries\n                   (id, kind, status,\n                    source_actor_id, source_session_id, source_conversation_id, source_run_id, source_json,\n                    target_kind, target_actor_id, target_session_id, target_conversation_id, target_run_id, target_json,\n                    delivery_mode, attempts, next_at, expires_at,\n                    content_ref_json, metadata_json, preview)\n                   VALUES\n                   ($1::uuid, $2, $3,\n                    $4, $5, $6, $7, $8::jsonb,\n                    $9, $10, $11, $12, $13, $14::jsonb,\n                    $15, $16, $17::timestamptz, $18::timestamptz,\n                    $19::jsonb, $20::jsonb, $21)\n                   RETURNING *",new cljs.core.PersistentVector(null, 21, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","kind","mailbox/kind",401992993).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(source),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(source),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(source),new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(source),knoxx.backend.actor_mailbox.json_param(source),new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"session-id","session-id",-1147060351).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913).cljs$core$IFn$_invoke$arity$1(target),new cljs.core.Keyword(null,"run-id","run-id",-1745267908).cljs$core$IFn$_invoke$arity$1(target),knoxx.backend.actor_mailbox.json_param(target),new cljs.core.Keyword(null,"mode","mode",654403691).cljs$core$IFn$_invoke$arity$1(delivery),new cljs.core.Keyword(null,"attempts","attempts",1024246729).cljs$core$IFn$_invoke$arity$1(delivery),new cljs.core.Keyword(null,"next-at","next-at",1830929216).cljs$core$IFn$_invoke$arity$1(delivery),new cljs.core.Keyword("mailbox","expires-at","mailbox/expires-at",-1256489474).cljs$core$IFn$_invoke$arity$1(entry),knoxx.backend.actor_mailbox.json_param(new cljs.core.Keyword("mailbox","content-ref","mailbox/content-ref",877031624).cljs$core$IFn$_invoke$arity$1(entry)),knoxx.backend.actor_mailbox.json_param(new cljs.core.Keyword("mailbox","metadata","mailbox/metadata",-1698257615).cljs$core$IFn$_invoke$arity$1(entry)),new cljs.core.Keyword("mailbox","preview","mailbox/preview",-512838338).cljs$core$IFn$_invoke$arity$1(entry)], null)).then((function (_){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(entry,new cljs.core.Keyword("mailbox","durable?","mailbox/durable?",771149223),true);
}));
}
});
knoxx.backend.actor_mailbox.mark_delivery_BANG_ = (function knoxx$backend$actor_mailbox$mark_delivery_BANG_(runtime,mailbox_id,status,p__517343){
var map__517346 = p__517343;
var map__517346__$1 = cljs.core.__destructure_map(map__517346);
var content_ref = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517346__$1,new cljs.core.Keyword(null,"content-ref","content-ref",1710065788));
var error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517346__$1,new cljs.core.Keyword(null,"error","error",-978969032));
var attempts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517346__$1,new cljs.core.Keyword(null,"attempts","attempts",1024246729));
var next_at = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517346__$1,new cljs.core.Keyword(null,"next-at","next-at",1830929216));
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(mailbox_id);
if(cljs.core.truth_(temp__5823__auto__)){
var mailbox_id_STAR_ = temp__5823__auto__;
if((!(knoxx.backend.actor_mailbox.database_enabled_QMARK_(runtime)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344),mailbox_id_STAR_,new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881),knoxx.backend.actor_mailbox.normalize_status(status),new cljs.core.Keyword("mailbox","durable?","mailbox/durable?",771149223),false], null));
} else {
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"UPDATE actor_mailbox_entries\n               SET status = $2,\n                   attempts = COALESCE($3, attempts),\n                   next_at = COALESCE($4::timestamptz, next_at),\n                   delivered_at = CASE WHEN $2 = 'delivered' THEN NOW() ELSE delivered_at END,\n                   acknowledged_at = CASE WHEN $2 = 'acknowledged' THEN NOW() ELSE acknowledged_at END,\n                   content_ref_json = COALESCE($5::jsonb, content_ref_json),\n                   last_error = $6,\n                   updated_at = NOW()\n               WHERE id = $1::uuid",new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [mailbox_id_STAR_,knoxx.backend.actor_mailbox.normalize_status(status),attempts,knoxx.backend.actor_mailbox.nonblank(next_at),(cljs.core.truth_(content_ref)?knoxx.backend.actor_mailbox.json_param(content_ref):null),knoxx.backend.actor_mailbox.nonblank(error)], null));
}
} else {
return Promise.resolve(null);
}
});
knoxx.backend.actor_mailbox.mark_delivered_BANG_ = (function knoxx$backend$actor_mailbox$mark_delivered_BANG_(runtime,mailbox_id,content_ref){
return knoxx.backend.actor_mailbox.mark_delivery_BANG_(runtime,mailbox_id,"delivered",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"content-ref","content-ref",1710065788),content_ref], null));
});
knoxx.backend.actor_mailbox.mark_failed_BANG_ = (function knoxx$backend$actor_mailbox$mark_failed_BANG_(runtime,mailbox_id,error){
return knoxx.backend.actor_mailbox.mark_delivery_BANG_(runtime,mailbox_id,"failed",new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error))], null));
});
knoxx.backend.actor_mailbox.json_value = (function knoxx$backend$actor_mailbox$json_value(value){
if((value == null)){
return cljs.core.PersistentArrayMap.EMPTY;
} else {
if(typeof value === 'string'){
try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(value),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e517391){var _ = e517391;
return cljs.core.PersistentArrayMap.EMPTY;
}} else {
if(cljs.core.truth_(cljs.core.array_QMARK_(value))){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));

}
}
}
});
knoxx.backend.actor_mailbox.row__GT_entry = (function knoxx$backend$actor_mailbox$row__GT_entry(row){
if(cljs.core.truth_(row)){
var source = knoxx.backend.actor_mailbox.json_value((row["source_json"]));
var target = knoxx.backend.actor_mailbox.json_value((row["target_json"]));
var delivery = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"mode","mode",654403691),(row["delivery_mode"]),new cljs.core.Keyword(null,"attempts","attempts",1024246729),(row["attempts"]),new cljs.core.Keyword(null,"next-at","next-at",1830929216),(row["next_at"])], null);
var content_ref = knoxx.backend.actor_mailbox.json_value((row["content_ref_json"]));
var metadata = knoxx.backend.actor_mailbox.json_value((row["metadata_json"]));
var G__517406 = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344),new cljs.core.Keyword("mailbox","kind","mailbox/kind",401992993),new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881),new cljs.core.Keyword("mailbox","created-at","mailbox/created-at",-1406815032),new cljs.core.Keyword("mailbox","content-ref","mailbox/content-ref",877031624),new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392),new cljs.core.Keyword("mailbox","target","mailbox/target",1100093613),new cljs.core.Keyword("mailbox","metadata","mailbox/metadata",-1698257615),new cljs.core.Keyword("mailbox","updated-at","mailbox/updated-at",-779421100),new cljs.core.Keyword("mailbox","source","mailbox/source",-1264954567)],[(row["id"]),(row["kind"]),(row["status"]),(row["created_at"]),content_ref,delivery,target,metadata,(row["updated_at"]),source]);
var G__517406__$1 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank((row["preview"])))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517406,new cljs.core.Keyword("mailbox","preview","mailbox/preview",-512838338),(row["preview"])):G__517406);
var G__517406__$2 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank((row["last_error"])))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517406__$1,new cljs.core.Keyword("mailbox","last-error","mailbox/last-error",997868945),(row["last_error"])):G__517406__$1);
var G__517406__$3 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank((row["delivered_at"])))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517406__$2,new cljs.core.Keyword("mailbox","delivered-at","mailbox/delivered-at",-1353109945),(row["delivered_at"])):G__517406__$2);
var G__517406__$4 = (cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank((row["acknowledged_at"])))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517406__$3,new cljs.core.Keyword("mailbox","acknowledged-at","mailbox/acknowledged-at",1312417597),(row["acknowledged_at"])):G__517406__$3);
if(cljs.core.truth_(knoxx.backend.actor_mailbox.nonblank((row["expires_at"])))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__517406__$4,new cljs.core.Keyword("mailbox","expires-at","mailbox/expires-at",-1256489474),(row["expires_at"]));
} else {
return G__517406__$4;
}
} else {
return null;
}
});
knoxx.backend.actor_mailbox.add_filter = (function knoxx$backend$actor_mailbox$add_filter(p__517422,p__517423){
var map__517426 = p__517422;
var map__517426__$1 = cljs.core.__destructure_map(map__517426);
var clauses = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517426__$1,new cljs.core.Keyword(null,"clauses","clauses",1454841241));
var params = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517426__$1,new cljs.core.Keyword(null,"params","params",710516235));
var vec__517427 = p__517423;
var clause_fn = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517427,(0),null);
var value = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__517427,(1),null);
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(value);
if(cljs.core.truth_(temp__5823__auto__)){
var value_STAR_ = temp__5823__auto__;
var idx = (cljs.core.count(params) + (1));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"clauses","clauses",1454841241),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(clauses,(clause_fn.cljs$core$IFn$_invoke$arity$1 ? clause_fn.cljs$core$IFn$_invoke$arity$1(idx) : clause_fn.call(null,idx))),new cljs.core.Keyword(null,"params","params",710516235),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(params,value_STAR_)], null);
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"clauses","clauses",1454841241),clauses,new cljs.core.Keyword(null,"params","params",710516235),params], null);
}
});
knoxx.backend.actor_mailbox.positive_int = (function knoxx$backend$actor_mailbox$positive_int(value,fallback,max_value){
var parsed = parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),(10));
if(cljs.core.truth_(isNaN(parsed))){
return fallback;
} else {
return cljs.core.min.cljs$core$IFn$_invoke$arity$2(max_value,cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),parsed));
}
});
knoxx.backend.actor_mailbox.list_entries_BANG_ = (function knoxx$backend$actor_mailbox$list_entries_BANG_(runtime,p__517478){
var map__517483 = p__517478;
var map__517483__$1 = cljs.core.__destructure_map(map__517483);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var target_actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"target-actor-id","target-actor-id",1128799845));
var target_session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"target-session-id","target-session-id",-1929186990));
var source_actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"source-actor-id","source-actor-id",-1224551760));
var source_run_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"source-run-id","source-run-id",-2000058256));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517483__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
if((!(knoxx.backend.actor_mailbox.database_enabled_QMARK_(runtime)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entries","entries",-86943161),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"durable?","durable?",2084525683),false], null));
} else {
var map__517499 = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(knoxx.backend.actor_mailbox.add_filter,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"clauses","clauses",1454841241),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"params","params",710516235),cljs.core.PersistentVector.EMPTY], null),new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (idx){
return (""+"status = $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx));
}),status], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (idx){
return (""+"target_actor_id = $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx));
}),target_actor_id], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (idx){
return (""+"target_session_id = $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx));
}),target_session_id], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (idx){
return (""+"source_actor_id = $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx));
}),source_actor_id], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(function (idx){
return (""+"source_run_id = $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(idx));
}),source_run_id], null)], null));
var map__517499__$1 = cljs.core.__destructure_map(map__517499);
var clauses = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517499__$1,new cljs.core.Keyword(null,"clauses","clauses",1454841241));
var params = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517499__$1,new cljs.core.Keyword(null,"params","params",710516235));
var limit_STAR_ = knoxx.backend.actor_mailbox.positive_int(limit,(50),(500));
var limit_idx = (cljs.core.count(params) + (1));
var where_sql = ((cljs.core.seq(clauses))?(""+" WHERE "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(" AND ",clauses))):"");
return knoxx.backend.actor_mailbox.query_BANG_(runtime,(""+"SELECT * FROM actor_mailbox_entries"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(where_sql)+" ORDER BY created_at DESC LIMIT $"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(limit_idx)),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(params,limit_STAR_)).then((function (result){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entries","entries",-86943161),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.actor_mailbox.row__GT_entry,knoxx.backend.actor_mailbox.rows(result)),new cljs.core.Keyword(null,"durable?","durable?",2084525683),true], null);
}));
}
});
knoxx.backend.actor_mailbox.acknowledge_entry_BANG_ = (function knoxx$backend$actor_mailbox$acknowledge_entry_BANG_(var_args){
var G__517511 = arguments.length;
switch (G__517511) {
case 2:
return knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (runtime,mailbox_id){
return knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,mailbox_id,null);
}));

(knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,mailbox_id,target_actor_id){
var temp__5823__auto__ = knoxx.backend.actor_mailbox.nonblank(mailbox_id);
if(cljs.core.truth_(temp__5823__auto__)){
var mailbox_id_STAR_ = temp__5823__auto__;
if((!(knoxx.backend.actor_mailbox.database_enabled_QMARK_(runtime)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344),mailbox_id_STAR_,new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881),"acknowledged",new cljs.core.Keyword("mailbox","durable?","mailbox/durable?",771149223),false], null));
} else {
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"UPDATE actor_mailbox_entries\n                    SET status = 'acknowledged', acknowledged_at = NOW(), updated_at = NOW()\n                    WHERE id = $1::uuid\n                      AND ($2::text IS NULL OR target_actor_id = $2)\n                    RETURNING *",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [mailbox_id_STAR_,knoxx.backend.actor_mailbox.nonblank(target_actor_id)], null)).then((function (result){
var G__517512 = cljs.core.first(knoxx.backend.actor_mailbox.rows(result));
if((G__517512 == null)){
return null;
} else {
return knoxx.backend.actor_mailbox.row__GT_entry(G__517512);
}
}));
}
} else {
return Promise.reject((new Error("mailbox id is required")));
}
}));

(knoxx.backend.actor_mailbox.acknowledge_entry_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.actor_mailbox.retry_request_event = (function knoxx$backend$actor_mailbox$retry_request_event(entry){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.actor_mailbox.mailbox_event_id(new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344).cljs$core$IFn$_invoke$arity$1(entry)))+"-retry-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())),new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"actor",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"actors.mailbox.retry-requested",new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"mailboxId","mailboxId",-395830287),new cljs.core.Keyword("mailbox","id","mailbox/id",-1664073344).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword("mailbox","status","mailbox/status",-754673881).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"target","target",253001721),new cljs.core.Keyword("mailbox","target","mailbox/target",1100093613).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword("mailbox","source","mailbox/source",-1264954567).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"delivery","delivery",-1844470516),new cljs.core.Keyword("mailbox","delivery","mailbox/delivery",1585980392).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"contentRef","contentRef",625680927),new cljs.core.Keyword("mailbox","content-ref","mailbox/content-ref",877031624).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword("mailbox","metadata","mailbox/metadata",-1698257615).cljs$core$IFn$_invoke$arity$1(entry),new cljs.core.Keyword(null,"preview","preview",451279890),new cljs.core.Keyword("mailbox","preview","mailbox/preview",-512838338).cljs$core$IFn$_invoke$arity$1(entry)], null)], null);
});
knoxx.backend.actor_mailbox.retry_eligible_BANG_ = (function knoxx$backend$actor_mailbox$retry_eligible_BANG_(runtime,p__517519){
var map__517520 = p__517519;
var map__517520__$1 = cljs.core.__destructure_map(map__517520);
var mailbox_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517520__$1,new cljs.core.Keyword(null,"mailbox-id","mailbox-id",796861681));
var statuses = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517520__$1,new cljs.core.Keyword(null,"statuses","statuses",710922046));
var max_attempts = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517520__$1,new cljs.core.Keyword(null,"max-attempts","max-attempts",1686564297));
var limit = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517520__$1,new cljs.core.Keyword(null,"limit","limit",-1355822363));
var delay_seconds = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__517520__$1,new cljs.core.Keyword(null,"delay-seconds","delay-seconds",-1391031133));
if((!(knoxx.backend.actor_mailbox.database_enabled_QMARK_(runtime)))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entries","entries",-86943161),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"durable?","durable?",2084525683),false], null));
} else {
var statuses_STAR_ = (function (){var or__5142__auto__ = cljs.core.seq(statuses);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["pending","failed"], null);
}
})();
var max_attempts_STAR_ = knoxx.backend.actor_mailbox.positive_int(max_attempts,(5),(100));
var limit_STAR_ = knoxx.backend.actor_mailbox.positive_int(limit,(25),(200));
var delay_seconds_STAR_ = (function (){var parsed = parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = delay_seconds;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})())),(10));
if(cljs.core.truth_(isNaN(parsed))){
return (0);
} else {
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),parsed);
}
})();
return knoxx.backend.actor_mailbox.query_BANG_(runtime,"WITH candidates AS (\n                     SELECT id\n                     FROM actor_mailbox_entries\n                     WHERE status = ANY($1::text[])\n                       AND attempts < $2\n                       AND (next_at IS NULL OR next_at <= NOW())\n                       AND (expires_at IS NULL OR expires_at > NOW())\n                       AND ($5::uuid IS NULL OR id = $5::uuid)\n                     ORDER BY created_at ASC\n                     LIMIT $3\n                     FOR UPDATE SKIP LOCKED\n                   )\n                   UPDATE actor_mailbox_entries m\n                   SET status = 'pending',\n                       attempts = m.attempts + 1,\n                       next_at = CASE WHEN $4::int > 0 THEN NOW() + ($4::text || ' seconds')::interval ELSE NULL END,\n                       last_error = NULL,\n                       updated_at = NOW()\n                   FROM candidates\n                   WHERE m.id = candidates.id\n                   RETURNING m.*",new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [statuses_STAR_,max_attempts_STAR_,limit_STAR_,delay_seconds_STAR_,knoxx.backend.actor_mailbox.nonblank(mailbox_id)], null)).then((function (result){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"entries","entries",-86943161),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.actor_mailbox.row__GT_entry,knoxx.backend.actor_mailbox.rows(result)),new cljs.core.Keyword(null,"durable?","durable?",2084525683),true], null);
}));
}
});

//# sourceMappingURL=knoxx.backend.actor_mailbox.js.map
