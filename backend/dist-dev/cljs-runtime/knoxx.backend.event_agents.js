import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.cljs.modern.js";
import "./shadow.esm.esm_import$node_child_process.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./knoxx.backend.events.sources.discord.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.runtime.models.js";
import "./knoxx.backend.runtime.state.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.agents.runner.js";
import "./knoxx.backend.events.cron.js";
import "./knoxx.backend.triggers.control_config.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.agent_templates.js";
import "./knoxx.backend.tools.media.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.util.parse.js";
import "./knoxx.backend.actions.dispatch.js";
import "./knoxx.backend.contracts.actor_scope.js";
goog.provide('knoxx.backend.event_agents');





if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.running_QMARK__STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.running_QMARK__STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(false);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.scheduled_tasks_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.scheduled_tasks_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.job_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.job_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.user_job_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.user_job_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.source_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.source_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),cljs.core.PersistentArrayMap.EMPTY], null)], null));
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.recent_events_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.recent_events_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentVector.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.dispatched_event_ids_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.dispatched_event_ids_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentHashSet.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.job_specs_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.job_specs_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.reload_timer_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.reload_timer_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.reload_lock_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.reload_lock_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.event_agents.event_agent_job_dirty_redis_key = "event-agent:job-dirty";
/**
 * Coalesce rapid reload! calls into one, firing 2 s after the last call.
 * Public so contract watchers and admin routes can use it.
 */
knoxx.backend.event_agents.debounced_reload_BANG_ = (function knoxx$backend$event_agents$debounced_reload_BANG_(){
var temp__5825__auto___70659 = cljs.core.deref(knoxx.backend.event_agents.reload_timer_STAR_);
if(cljs.core.truth_(temp__5825__auto___70659)){
var t_70660 = temp__5825__auto___70659;
clearTimeout(t_70660);
} else {
}

return cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_timer_STAR_,setTimeout((function (){
cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_timer_STAR_,null);

return (knoxx.backend.event_agents.reload_BANG_.cljs$core$IFn$_invoke$arity$0 ? knoxx.backend.event_agents.reload_BANG_.cljs$core$IFn$_invoke$arity$0() : knoxx.backend.event_agents.reload_BANG_.call(null));
}),(2000)));
});
knoxx.backend.event_agents.mark_event_dispatched_BANG_ = (function knoxx$backend$event_agents$mark_event_dispatched_BANG_(event_id){
var vec__70572 = cljs.core.swap_vals_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.event_agents.dispatched_event_ids_STAR_,cljs.core.conj,event_id);
var before = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70572,(0),null);
return (!(cljs.core.contains_QMARK_(before,event_id)));
});
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.event_agents !== 'undefined') && (typeof knoxx.backend.event_agents.enriched_env_config_cache_STAR_ !== 'undefined')){
} else {
knoxx.backend.event_agents.enriched_env_config_cache_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"base","base",185279322),null,new cljs.core.Keyword(null,"enriched","enriched",-750799869),null], null));
}
/**
 * Return an enriched config derived from runtime-config/cfg.
 * 
 * Memoized to avoid repeated env reads / enrich-config recomputation on hot paths.
 * Cache is refreshed automatically when the base env config value changes.
 */
knoxx.backend.event_agents.cached_enriched_env_config = (function knoxx$backend$event_agents$cached_enriched_env_config(){
var base = knoxx.backend.runtime.config.cfg();
var cached = cljs.core.deref(knoxx.backend.event_agents.enriched_env_config_cache_STAR_);
if(cljs.core.truth_((function (){var and__5140__auto__ = new cljs.core.Keyword(null,"enriched","enriched",-750799869).cljs$core$IFn$_invoke$arity$1(cached);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(base,new cljs.core.Keyword(null,"base","base",185279322).cljs$core$IFn$_invoke$arity$1(cached));
} else {
return and__5140__auto__;
}
})())){
return new cljs.core.Keyword(null,"enriched","enriched",-750799869).cljs$core$IFn$_invoke$arity$1(cached);
} else {
var enriched = knoxx.backend.runtime.models.enrich_config(base);
cljs.core.reset_BANG_(knoxx.backend.event_agents.enriched_env_config_cache_STAR_,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"base","base",185279322),base,new cljs.core.Keyword(null,"enriched","enriched",-750799869),enriched], null));

return enriched;
}
});
/**
 * Return the current enriched runtime config.
 * 
 * Prefer runtime-state/config* when available so dynamic overrides (e.g.
 * persisted event-agent-control) are visible to callers.
 */
knoxx.backend.event_agents.cfg = (function knoxx$backend$event_agents$cfg(){
var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.cached_enriched_env_config();
}
});
knoxx.backend.event_agents.control_config = (function knoxx$backend$event_agents$control_config(config){
return knoxx.backend.triggers.control_config.event_agent_control_config(config);
});
knoxx.backend.event_agents.schedule_events_cron_ticker_BANG_ = (function knoxx$backend$event_agents$schedule_events_cron_ticker_BANG_(config){
return knoxx.backend.events.cron.schedule_cron_ticker_BANG_(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"scheduled-tasks*","scheduled-tasks*",461432845),knoxx.backend.event_agents.scheduled_tasks_STAR_,new cljs.core.Keyword(null,"job-state*","job-state*",-2143443400),knoxx.backend.event_agents.job_state_STAR_,new cljs.core.Keyword(null,"running?*","running?*",-1352022575),knoxx.backend.event_agents.running_QMARK__STAR_,new cljs.core.Keyword(null,"control-config-fn","control-config-fn",-413447156),knoxx.backend.event_agents.control_config,new cljs.core.Keyword(null,"run-job!","run-job!",-1560119830),knoxx.backend.event_agents.run_job_BANG_,new cljs.core.Keyword(null,"update-job-state!","update-job-state!",1002448980),knoxx.backend.event_agents.update_job_state_BANG_,new cljs.core.Keyword(null,"normalize-job-state","normalize-job-state",999456147),knoxx.backend.event_agents.normalize_job_state], null),config);
});
knoxx.backend.event_agents.discord_token = (function knoxx$backend$event_agents$discord_token(){
return knoxx.backend.events.sources.discord.bot_token(knoxx.backend.event_agents.cfg());
});
knoxx.backend.event_agents.policy_db = (function knoxx$backend$event_agents$policy_db(){
return knoxx.backend.runtime.state.current_policy_db();
});
knoxx.backend.event_agents.discord_gateway_user_id = (function knoxx$backend$event_agents$discord_gateway_user_id(){
return knoxx.backend.events.sources.discord.gateway_user_id();
});
knoxx.backend.event_agents.fetch_json_BANG_ = (function knoxx$backend$event_agents$fetch_json_BANG_(url,options){
return fetch(url,options).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.event_agents.discord_source_config = (function knoxx$backend$event_agents$discord_source_config(control){
var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(control,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.Keyword(null,"discord","discord",480262077)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.event_agents.nonblank_str = (function knoxx$backend$event_agents$nonblank_str(value){
var G__70575 = value;
var G__70575__$1 = (((G__70575 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70575)));
var G__70575__$2 = (((G__70575__$1 == null))?null:clojure.string.trim(G__70575__$1));
if((G__70575__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70575__$2);
}
});
knoxx.backend.event_agents.filter_string_list = (function knoxx$backend$event_agents$filter_string_list(values){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__70576 = value;
var G__70576__$1 = (((G__70576 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70576)));
var G__70576__$2 = (((G__70576__$1 == null))?null:clojure.string.trim(G__70576__$1));
if((G__70576__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70576__$2);
}
}),(function (){var or__5142__auto__ = values;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.event_agents.job_channels = (function knoxx$backend$event_agents$job_channels(control,job){
var channels = knoxx.backend.event_agents.filter_string_list(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"channels","channels",1132759174)], null)));
if(cljs.core.seq(channels)){
return channels;
} else {
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685).cljs$core$IFn$_invoke$arity$1(knoxx.backend.event_agents.discord_source_config(control));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
}
});
knoxx.backend.event_agents.job_publish_channels = (function knoxx$backend$event_agents$job_publish_channels(job){
return knoxx.backend.event_agents.filter_string_list((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"publish_channels","publish_channels",2144419461)], null));
}
})());
});
knoxx.backend.event_agents.job_guild_ids = (function knoxx$backend$event_agents$job_guild_ids(job){
return knoxx.backend.event_agents.filter_string_list((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"guildIds","guildIds",785308327)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"guild_ids","guild_ids",-681975868)], null));
}
})());
});
knoxx.backend.event_agents.job_author_ids = (function knoxx$backend$event_agents$job_author_ids(job){
return knoxx.backend.event_agents.filter_string_list((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"authorIds","authorIds",1639016820)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"author_ids","author_ids",-1682078222)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"authors","authors",2063018172)], null));
}
}
})());
});
knoxx.backend.event_agents.job_match_all_QMARK_ = (function knoxx$backend$event_agents$job_match_all_QMARK_(job){
return ((cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"matchAll","matchAll",-601915638)], null)) === true) || (cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"match_all","match_all",1325819031)], null)) === true));
});
knoxx.backend.event_agents.job_keywords = (function knoxx$backend$event_agents$job_keywords(control,job){
var keywords = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__70577 = value;
var G__70577__$1 = (((G__70577 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70577)));
var G__70577__$2 = (((G__70577__$1 == null))?null:clojure.string.trim(G__70577__$1));
var G__70577__$3 = (((G__70577__$2 == null))?null:clojure.string.lower_case(G__70577__$2));
if((G__70577__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__70577__$3);
}
}),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"keywords","keywords",1526959054)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.seq(keywords)){
return keywords;
} else {
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"targetKeywords","targetKeywords",-9231449).cljs$core$IFn$_invoke$arity$1(knoxx.backend.event_agents.discord_source_config(control));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
}
});
knoxx.backend.event_agents.job_max_messages = (function knoxx$backend$event_agents$job_max_messages(job,fallback){
var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"maxMessages","maxMessages",1680581379)], null)));
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
});
knoxx.backend.event_agents.discord_last_seen = (function knoxx$backend$event_agents$discord_last_seen(channel_id){
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.event_agents.source_state_STAR_),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),channel_id], null));
});
knoxx.backend.event_agents.remember_discord_latest_BANG_ = (function knoxx$backend$event_agents$remember_discord_latest_BANG_(channel_id,messages){
var temp__5825__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(cljs.core.first(messages));
if(cljs.core.truth_(temp__5825__auto__)){
var latest_id = temp__5825__auto__;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.source_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),channel_id], null),latest_id);

var temp__5825__auto____$1 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto____$1)){
var client = temp__5825__auto____$1;
return knoxx.backend.redis_client.set_key.cljs$core$IFn$_invoke$arity$3(client,(""+"event-agent:discord-last-seen:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)),latest_id);
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.event_agents.unseen_discord_messages = (function knoxx$backend$event_agents$unseen_discord_messages(channel_id,messages){
var known_id = knoxx.backend.event_agents.discord_last_seen(channel_id);
if(clojure.string.blank_QMARK_(known_id)){
return messages;
} else {
return cljs.core.vec(cljs.core.take_while.cljs$core$IFn$_invoke$arity$2((function (message){
return cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(known_id,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message));
}),messages));
}
});
knoxx.backend.event_agents.append_recent_event_BANG_ = (function knoxx$backend$event_agents$append_recent_event_BANG_(event){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.event_agents.recent_events_STAR_,(function (events){
return cljs.core.vec(cljs.core.take_last((30),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(events),event)));
}));
});
knoxx.backend.event_agents.redis_keys_BANG_ = (function knoxx$backend$event_agents$redis_keys_BANG_(client,pattern){
return client.keys(pattern).then((function (keys){
if(cljs.core.truth_(cljs.core.array_QMARK_(keys))){
return cljs.core.vec(cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(keys));
} else {
return cljs.core.PersistentVector.EMPTY;
}
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] Redis KEYS failed for",pattern,":",err.message], 0));

return cljs.core.PersistentVector.EMPTY;
}));
});
knoxx.backend.event_agents.delete_redis_keys_BANG_ = (function knoxx$backend$event_agents$delete_redis_keys_BANG_(client,keys){
var keys_SINGLEQUOTE_ = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70578_SHARP_){
var G__70580 = p1__70578_SHARP_;
var G__70580__$1 = (((G__70580 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70580)));
var G__70580__$2 = (((G__70580__$1 == null))?null:clojure.string.trim(G__70580__$1));
if((G__70580__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70580__$2);
}
}),(function (){var or__5142__auto__ = keys;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()))));
if(cljs.core.seq(keys_SINGLEQUOTE_)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70579_SHARP_){
return knoxx.backend.redis_client.del(client,p1__70579_SHARP_);
}),keys_SINGLEQUOTE_))).then((function (_){
return cljs.core.count(keys_SINGLEQUOTE_);
}));
} else {
return Promise.resolve((0));
}
});
knoxx.backend.event_agents.clear_runtime_state_BANG_ = (function knoxx$backend$event_agents$clear_runtime_state_BANG_(){
cljs.core.reset_BANG_(knoxx.backend.event_agents.job_state_STAR_,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.event_agents.user_job_state_STAR_,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.event_agents.source_state_STAR_,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),cljs.core.PersistentArrayMap.EMPTY], null)], null));

cljs.core.reset_BANG_(knoxx.backend.event_agents.recent_events_STAR_,cljs.core.PersistentVector.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.event_agents.job_specs_STAR_,cljs.core.PersistentArrayMap.EMPTY);

return cljs.core.reset_BANG_(knoxx.backend.event_agents.dispatched_event_ids_STAR_,cljs.core.PersistentHashSet.EMPTY);
});
knoxx.backend.event_agents.disable_cron_jobs = (function knoxx$backend$event_agents$disable_cron_jobs(control){
return cljs.core.update.cljs$core$IFn$_invoke$arity$3(control,new cljs.core.Keyword(null,"jobs","jobs",-313607120),(function (jobs){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (job){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cron",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(job,new cljs.core.Keyword(null,"enabled","enabled",1195909756),false);
} else {
return job;
}
}),(function (){var or__5142__auto__ = jobs;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
}));
});
knoxx.backend.event_agents.recover_runtime_state_BANG_ = (function knoxx$backend$event_agents$recover_runtime_state_BANG_(control){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
var job_ids = cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control));
var channels = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"defaultChannels","defaultChannels",632407685).cljs$core$IFn$_invoke$arity$1(knoxx.backend.event_agents.discord_source_config(control));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var recovery_promises = cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((function (){var iter__5628__auto__ = (function knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70581(s__70582){
return (new cljs.core.LazySeq(null,(function (){
var s__70582__$1 = s__70582;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__70582__$1);
if(temp__5825__auto__){
var s__70582__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__70582__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__70582__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__70584 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__70583 = (0);
while(true){
if((i__70583 < size__5627__auto__)){
var job_id = cljs.core._nth(c__5626__auto__,i__70583);
cljs.core.chunk_append(b__70584,knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-state:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id))).then(((function (i__70583,job_id,c__5626__auto__,size__5627__auto__,b__70584,s__70582__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (state){
if(cljs.core.truth_(state)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_state_STAR_,cljs.core.assoc,job_id,(knoxx.backend.event_agents.normalize_job_state.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.event_agents.normalize_job_state.cljs$core$IFn$_invoke$arity$2(job_id,state) : knoxx.backend.event_agents.normalize_job_state.call(null,job_id,state)));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered state for",job_id], 0));
} else {
}

return null;
});})(i__70583,job_id,c__5626__auto__,size__5627__auto__,b__70584,s__70582__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
));

var G__70661 = (i__70583 + (1));
i__70583 = G__70661;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__70584),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70581(cljs.core.chunk_rest(s__70582__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__70584),null);
}
} else {
var job_id = cljs.core.first(s__70582__$2);
return cljs.core.cons(knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-state:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id))).then(((function (job_id,s__70582__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (state){
if(cljs.core.truth_(state)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_state_STAR_,cljs.core.assoc,job_id,(knoxx.backend.event_agents.normalize_job_state.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.event_agents.normalize_job_state.cljs$core$IFn$_invoke$arity$2(job_id,state) : knoxx.backend.event_agents.normalize_job_state.call(null,job_id,state)));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered state for",job_id], 0));
} else {
}

return null;
});})(job_id,s__70582__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70581(cljs.core.rest(s__70582__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(job_ids);
})(),(function (){var iter__5628__auto__ = (function knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70585(s__70586){
return (new cljs.core.LazySeq(null,(function (){
var s__70586__$1 = s__70586;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__70586__$1);
if(temp__5825__auto__){
var s__70586__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__70586__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__70586__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__70588 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__70587 = (0);
while(true){
if((i__70587 < size__5627__auto__)){
var job_id = cljs.core._nth(c__5626__auto__,i__70587);
cljs.core.chunk_append(b__70588,knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id))).then(((function (i__70587,job_id,c__5626__auto__,size__5627__auto__,b__70588,s__70586__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (redis_spec){
if(cljs.core.truth_(redis_spec)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_specs_STAR_,cljs.core.assoc,job_id,redis_spec);

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered spec for",job_id], 0));
} else {
}

return null;
});})(i__70587,job_id,c__5626__auto__,size__5627__auto__,b__70588,s__70586__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
));

var G__70662 = (i__70587 + (1));
i__70587 = G__70662;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__70588),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70585(cljs.core.chunk_rest(s__70586__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__70588),null);
}
} else {
var job_id = cljs.core.first(s__70586__$2);
return cljs.core.cons(knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id))).then(((function (job_id,s__70586__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (redis_spec){
if(cljs.core.truth_(redis_spec)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_specs_STAR_,cljs.core.assoc,job_id,redis_spec);

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered spec for",job_id], 0));
} else {
}

return null;
});})(job_id,s__70586__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70585(cljs.core.rest(s__70586__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(job_ids);
})(),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(function (){var iter__5628__auto__ = (function knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70589(s__70590){
return (new cljs.core.LazySeq(null,(function (){
var s__70590__$1 = s__70590;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__70590__$1);
if(temp__5825__auto__){
var s__70590__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__70590__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__70590__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__70592 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__70591 = (0);
while(true){
if((i__70591 < size__5627__auto__)){
var channel_id = cljs.core._nth(c__5626__auto__,i__70591);
cljs.core.chunk_append(b__70592,knoxx.backend.redis_client.get_key(client,(""+"event-agent:discord-last-seen:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id))).then(((function (i__70591,channel_id,c__5626__auto__,size__5627__auto__,b__70592,s__70590__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (last_id){
if(cljs.core.truth_(last_id)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.source_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),channel_id], null),last_id);

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered last-seen for channel",channel_id], 0));
} else {
}

return null;
});})(i__70591,channel_id,c__5626__auto__,size__5627__auto__,b__70592,s__70590__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
));

var G__70663 = (i__70591 + (1));
i__70591 = G__70663;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__70592),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70589(cljs.core.chunk_rest(s__70590__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__70592),null);
}
} else {
var channel_id = cljs.core.first(s__70590__$2);
return cljs.core.cons(knoxx.backend.redis_client.get_key(client,(""+"event-agent:discord-last-seen:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id))).then(((function (channel_id,s__70590__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__){
return (function (last_id){
if(cljs.core.truth_(last_id)){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.source_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"last-seen","last-seen",-526432685),channel_id], null),last_id);

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] recovered last-seen for channel",channel_id], 0));
} else {
}

return null;
});})(channel_id,s__70590__$2,temp__5825__auto__,job_ids,channels,client,temp__5823__auto__))
),knoxx$backend$event_agents$recover_runtime_state_BANG__$_iter__70589(cljs.core.rest(s__70590__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(channels);
})()], 0));
return Promise.all(cljs.core.clj__GT_js(recovery_promises)).then((function (_){
return null;
}));
} else {
return Promise.resolve(null);
}
});
/**
 * Update a job spec in Redis and mark it dirty for SQL flush.
 * This is the canonical write path - all job updates go through here.
 */
knoxx.backend.event_agents.update_job_spec_BANG_ = (function knoxx$backend$event_agents$update_job_spec_BANG_(job_id,spec){
var temp__5825__auto___70664 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___70664)){
var client_70665 = temp__5825__auto___70664;
var key_70666 = (""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id));
var dirty_key_70667 = knoxx.backend.event_agents.event_agent_job_dirty_redis_key;
knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3(client_70665,key_70666,spec);

knoxx.backend.redis_client.sadd(client_70665,dirty_key_70667,job_id);

knoxx.backend.redis_client.expire(client_70665,dirty_key_70667,(86400));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] job",job_id,"marked dirty for persistence"], 0));
} else {
}

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_specs_STAR_,cljs.core.assoc,job_id,spec);

return spec;
});
/**
 * Load job spec from Redis (hot), or return default spec.
 * Redis is the source of truth for running configuration.
 * This prevents the 'reasoning reset' bug by ensuring runtime overrides persist.
 */
knoxx.backend.event_agents.get_job_spec = (function knoxx$backend$event_agents$get_job_spec(job_id,default_spec){
var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
var key = (""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id));
return knoxx.backend.redis_client.get_json(client,key).then((function (redis_spec){
if(cljs.core.truth_(redis_spec)){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] loaded spec for",job_id,"from Redis"], 0));

return redis_spec;
} else {
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] using default spec for",job_id], 0));

return default_spec;
}
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] Redis load failed for",job_id,":",err.message], 0));

return default_spec;
}));
} else {
return default_spec;
}
});
/**
 * Write-behind flush: Move dirty job specs from Redis to SQL.
 * Called periodically by the background flush task.
 * 
 * In a future implementation, this would write to a SQL database.
 * For now, it logs the dirty jobs and clears the dirty marker.
 */
knoxx.backend.event_agents.flush_dirty_jobs_to_sql_BANG_ = (function knoxx$backend$event_agents$flush_dirty_jobs_to_sql_BANG_(){
var temp__5825__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto__)){
var client = temp__5825__auto__;
var dirty_key = knoxx.backend.event_agents.event_agent_job_dirty_redis_key;
return knoxx.backend.redis_client.smembers(client,dirty_key).then((function (job_ids){
if(cljs.core.truth_((function (){var and__5140__auto__ = job_ids;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.seq(job_ids);
} else {
return and__5140__auto__;
}
})())){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] flushing",cljs.core.count(job_ids),"dirty jobs to SQL..."], 0));

var seq__70593_70668 = cljs.core.seq(job_ids);
var chunk__70594_70669 = null;
var count__70595_70670 = (0);
var i__70596_70671 = (0);
while(true){
if((i__70596_70671 < count__70595_70670)){
var job_id_70672 = chunk__70594_70669.cljs$core$IIndexed$_nth$arity$2(null,i__70596_70671);
knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id_70672))).then(((function (seq__70593_70668,chunk__70594_70669,count__70595_70670,i__70596_70671,job_id_70672,dirty_key,client,temp__5825__auto__){
return (function (spec){
if(cljs.core.truth_(spec)){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] flushed",job_id_70672,"to SQL"], 0));
} else {
return null;
}
});})(seq__70593_70668,chunk__70594_70669,count__70595_70670,i__70596_70671,job_id_70672,dirty_key,client,temp__5825__auto__))
);


var G__70673 = seq__70593_70668;
var G__70674 = chunk__70594_70669;
var G__70675 = count__70595_70670;
var G__70676 = (i__70596_70671 + (1));
seq__70593_70668 = G__70673;
chunk__70594_70669 = G__70674;
count__70595_70670 = G__70675;
i__70596_70671 = G__70676;
continue;
} else {
var temp__5825__auto___70677__$1 = cljs.core.seq(seq__70593_70668);
if(temp__5825__auto___70677__$1){
var seq__70593_70678__$1 = temp__5825__auto___70677__$1;
if(cljs.core.chunked_seq_QMARK_(seq__70593_70678__$1)){
var c__5673__auto___70679 = cljs.core.chunk_first(seq__70593_70678__$1);
var G__70680 = cljs.core.chunk_rest(seq__70593_70678__$1);
var G__70681 = c__5673__auto___70679;
var G__70682 = cljs.core.count(c__5673__auto___70679);
var G__70683 = (0);
seq__70593_70668 = G__70680;
chunk__70594_70669 = G__70681;
count__70595_70670 = G__70682;
i__70596_70671 = G__70683;
continue;
} else {
var job_id_70684 = cljs.core.first(seq__70593_70678__$1);
knoxx.backend.redis_client.get_json(client,(""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id_70684))).then(((function (seq__70593_70668,chunk__70594_70669,count__70595_70670,i__70596_70671,job_id_70684,seq__70593_70678__$1,temp__5825__auto___70677__$1,dirty_key,client,temp__5825__auto__){
return (function (spec){
if(cljs.core.truth_(spec)){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] flushed",job_id_70684,"to SQL"], 0));
} else {
return null;
}
});})(seq__70593_70668,chunk__70594_70669,count__70595_70670,i__70596_70671,job_id_70684,seq__70593_70678__$1,temp__5825__auto___70677__$1,dirty_key,client,temp__5825__auto__))
);


var G__70685 = cljs.core.next(seq__70593_70678__$1);
var G__70686 = null;
var G__70687 = (0);
var G__70688 = (0);
seq__70593_70668 = G__70685;
chunk__70594_70669 = G__70686;
count__70595_70670 = G__70687;
i__70596_70671 = G__70688;
continue;
}
} else {
}
}
break;
}

knoxx.backend.redis_client.del(client,dirty_key);

return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] dirty queue cleared"], 0));
} else {
return null;
}
})).catch((function (err){
return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] flush failed:",err.message], 0));
}));
} else {
return null;
}
});
/**
 * Schedule background task to flush dirty jobs to SQL.
 * Runs every 5 minutes to batch writes.
 * Idempotent: no-op if the flush task is already registered.
 */
knoxx.backend.event_agents.schedule_flush_task_BANG_ = (function knoxx$backend$event_agents$schedule_flush_task_BANG_(){
if(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.event_agents.scheduled_tasks_STAR_),new cljs.core.Keyword(null,"flush","flush",-1138711199))){
return null;
} else {
var flush_interval_ms = (((5) * (60)) * (1000));
var flush_task = (function (){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.event_agents.running_QMARK__STAR_))){
return knoxx.backend.event_agents.flush_dirty_jobs_to_sql_BANG_();
} else {
return null;
}
});
var flush_id = (function (){var G__70597 = setInterval(flush_task,flush_interval_ms);
G__70597.unref();

return G__70597;
})();
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.scheduled_tasks_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"flush","flush",-1138711199),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"interval","interval",1708495417),new cljs.core.Keyword(null,"id","id",-1388402092),flush_id], null));

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] scheduled SQL flush every 5 minutes"], 0));

var sweep_interval_ms = (((10) * (60)) * (1000));
var sweep_fn = (function (){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.event_agents.running_QMARK__STAR_))){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.event_agents.dispatched_event_ids_STAR_,(function (ids){
if((cljs.core.count(ids) > (500))){
return cljs.core.set(cljs.core.take_last((500),cljs.core.vec(ids)));
} else {
return ids;
}
}));
} else {
return null;
}
});
var sweep_id = (function (){var G__70598 = setInterval(sweep_fn,sweep_interval_ms);
G__70598.unref();

return G__70598;
})();
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.scheduled_tasks_STAR_,cljs.core.assoc,new cljs.core.Keyword(null,"dispatch-sweep","dispatch-sweep",569516003),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"interval","interval",1708495417),new cljs.core.Keyword(null,"id","id",-1388402092),sweep_id,new cljs.core.Keyword(null,"everyMs","everyMs",1558845283),sweep_interval_ms], null));
}
});
knoxx.backend.event_agents.update_job_state_BANG_ = (function knoxx$backend$event_agents$update_job_state_BANG_(job_id,f){
var new_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.job_state_STAR_,cljs.core.update,job_id,(function (current){
var G__70599 = (function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
return (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(G__70599) : f.call(null,G__70599));
})),job_id);
var temp__5825__auto___70689 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___70689)){
var client_70690 = temp__5825__auto___70689;
knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3(client_70690,(""+"event-agent:job-state:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)),new_state);
} else {
}

return new_state;
});
knoxx.backend.event_agents.normalize_job_state = (function knoxx$backend$event_agents$normalize_job_state(job_id,state){
var candidate = ((((cljs.core.map_QMARK_(state)) && (cljs.core.map_QMARK_(cljs.core.get.cljs$core$IFn$_invoke$arity$2(state,job_id)))))?cljs.core.get.cljs$core$IFn$_invoke$arity$2(state,job_id):((cljs.core.map_QMARK_(state))?state:cljs.core.PersistentArrayMap.EMPTY
));
if(cljs.core.map_QMARK_(candidate)){
return candidate;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.event_agents.record_job_run_start_BANG_ = (function knoxx$backend$event_agents$record_job_run_start_BANG_(job){
var job_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
var started_at = Date.now();
var cadence_ms = (((60) * (1000)) * cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})()));
knoxx.backend.event_agents.update_job_state_BANG_(job_id,(function (state){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.event_agents.normalize_job_state(job_id,state),new cljs.core.Keyword(null,"id","id",-1388402092),job_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"running","running",1554969103),true,new cljs.core.Keyword(null,"lastStartedAt","lastStartedAt",1357808797),started_at,new cljs.core.Keyword(null,"lastStatus","lastStatus",1522139835),"running",new cljs.core.Keyword(null,"nextRunAt","nextRunAt",-1914512613),(started_at + cadence_ms)], 0));
}));

return started_at;
});
knoxx.backend.event_agents.record_job_run_finish_BANG_ = (function knoxx$backend$event_agents$record_job_run_finish_BANG_(job,started_at,status,error_message){
var finished_at = Date.now();
var job_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
knoxx.backend.event_agents.update_job_state_BANG_(job_id,(function (state){
return (function (next_state){
if(cljs.core.truth_(error_message)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(next_state,new cljs.core.Keyword(null,"lastError","lastError",845794675),error_message);
} else {
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(next_state,new cljs.core.Keyword(null,"lastError","lastError",845794675));
}
})(cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.event_agents.normalize_job_state(job_id,state),new cljs.core.Keyword(null,"id","id",-1388402092),job_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"enabled","enabled",1195909756),new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"running","running",1554969103),false,new cljs.core.Keyword(null,"lastFinishedAt","lastFinishedAt",-1905527657),finished_at,new cljs.core.Keyword(null,"lastDurationMs","lastDurationMs",-1880215352),(finished_at - started_at),new cljs.core.Keyword(null,"lastStatus","lastStatus",1522139835),status], 0)),new cljs.core.Keyword(null,"runCount","runCount",-2118098906),cljs.core.fnil.cljs$core$IFn$_invoke$arity$2(cljs.core.inc,(0))));
}));

var log_line = (""+"ts="+cljs.core.str.cljs$core$IFn$_invoke$arity$1((new Date()).toISOString())+" | kind=:agent-job-result | job="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+" | status="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(status)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(error_message)?(""+" | error="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(error_message)):null)));
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents-telemetry]",log_line], 0));

return null;
});
knoxx.backend.event_agents.direct_start_headers = (function knoxx$backend$event_agents$direct_start_headers(config){
var api_key = new cljs.core.Keyword(null,"knoxx-api-key","knoxx-api-key",-1142749154).cljs$core$IFn$_invoke$arity$1(config);
var headers = ({"Content-Type": "application/json", "x-knoxx-user-email": "system-admin@open-hax.local"});
if(clojure.string.blank_QMARK_(api_key)){
} else {
(headers["X-API-Key"] = api_key);
}

return headers;
});
knoxx.backend.event_agents.tool_policies__GT_js = (function knoxx$backend$event_agents$tool_policies__GT_js(policies){
return cljs.core.clj__GT_js(cljs.core.vec((function (){var iter__5628__auto__ = (function knoxx$backend$event_agents$tool_policies__GT_js_$_iter__70600(s__70601){
return (new cljs.core.LazySeq(null,(function (){
var s__70601__$1 = s__70601;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__70601__$1);
if(temp__5825__auto__){
var s__70601__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__70601__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__70601__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__70603 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__70602 = (0);
while(true){
if((i__70602 < size__5627__auto__)){
var policy = cljs.core._nth(c__5626__auto__,i__70602);
cljs.core.chunk_append(b__70603,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy)], null));

var G__70691 = (i__70602 + (1));
i__70602 = G__70691;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__70603),knoxx$backend$event_agents$tool_policies__GT_js_$_iter__70600(cljs.core.chunk_rest(s__70601__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__70603),null);
}
} else {
var policy = cljs.core.first(s__70601__$2);
return cljs.core.cons(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"toolId","toolId",-1935596543),new cljs.core.Keyword(null,"toolId","toolId",-1935596543).cljs$core$IFn$_invoke$arity$1(policy),new cljs.core.Keyword(null,"effect","effect",347343289),new cljs.core.Keyword(null,"effect","effect",347343289).cljs$core$IFn$_invoke$arity$1(policy)], null),knoxx$backend$event_agents$tool_policies__GT_js_$_iter__70600(cljs.core.rest(s__70601__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__((function (){var or__5142__auto__ = policies;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
})()));
});
knoxx.backend.event_agents.synthesis_source_mode_QMARK_ = (function knoxx$backend$event_agents$synthesis_source_mode_QMARK_(mode){
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["synthesis",null,"synthesize",null], null), null),(function (){var G__70604 = mode;
var G__70604__$1 = (((G__70604 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70604)));
var G__70604__$2 = (((G__70604__$1 == null))?null:clojure.string.trim(G__70604__$1));
if((G__70604__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__70604__$2);
}
})());
});
/**
 * Fetch a URL with an AbortController timeout. Returns a Promise<Response>.
 */
knoxx.backend.event_agents.fetch_with_timeout = (function knoxx$backend$event_agents$fetch_with_timeout(url,opts,timeout_ms){
var controller = (new AbortController());
var timeout_id = setTimeout((function (){
return controller.abort();
}),timeout_ms);
return fetch(url,Object.assign(({"signal": controller.signal}),opts)).finally((function (){
return clearTimeout(timeout_id);
}));
});
/**
 * Read an SVG file, repair corruption if present, and write it back.
 * Returns a Promise resolving to true if repaired, false otherwise.
 */
knoxx.backend.event_agents.sanitize_svg_file_BANG_ = (function knoxx$backend$event_agents$sanitize_svg_file_BANG_(local_path){
if(clojure.string.ends_with_QMARK_(clojure.string.lower_case(local_path),".svg")){
return shadow.esm.esm_import$node_fs$promises.readFile(local_path,"utf8").then((function (content){
var temp__5823__auto__ = knoxx.backend.text.sanitize_svg_content(content);
if(cljs.core.truth_(temp__5823__auto__)){
var repaired = temp__5823__auto__;
return shadow.esm.esm_import$node_fs$promises.writeFile(local_path,repaired,"utf8").then((function (_){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] repaired corrupted SVG:",local_path], 0));

return true;
}));
} else {
return false;
}
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] SVG sanitization failed for",local_path,":",err.message], 0));

return false;
}));
} else {
return Promise.resolve(false);
}
});
/**
 * Download an attachment to /tmp asynchronously using secure fetch.
 * Returns a Promise<string|nil> with the local file path.
 */
knoxx.backend.event_agents.download_attachment_to_tmp_BANG_ = (function knoxx$backend$event_agents$download_attachment_to_tmp_BANG_(p__70605){
var map__70606 = p__70605;
var map__70606__$1 = cljs.core.__destructure_map(map__70606);
var filename = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70606__$1,new cljs.core.Keyword(null,"filename","filename",-1428840783));
var url = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70606__$1,new cljs.core.Keyword(null,"url","url",276297046));
if(cljs.core.truth_((function (){var and__5140__auto__ = filename;
if(cljs.core.truth_(and__5140__auto__)){
return url;
} else {
return and__5140__auto__;
}
})())){
var local_path = (""+"/tmp/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename));
return knoxx.backend.event_agents.fetch_with_timeout(url,(cljs.core.truth_((function (){var and__5140__auto__ = knoxx.backend.tools.media.source_discord_cdn_url_QMARK_(url);
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.event_agents.discord_token();
} else {
return and__5140__auto__;
}
})())?({"headers": ({"Authorization": (""+"Bot "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.event_agents.discord_token()))})}):({})),(10000)).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.arrayBuffer;
} else {
throw (new Error((""+"HTTP "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status))));
}
})).then((function (buffer){
shadow.esm.esm_import$node_fs$promises.writeFile(local_path,(new Buffer(buffer)));

return local_path;
})).then((function (path){
knoxx.backend.event_agents.sanitize_svg_file_BANG_(path);

return path;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] attachment download failed:",filename,err.message], 0));

return null;
}));
} else {
return Promise.resolve(null);
}
});
/**
 * Build an event summary string. Returns a Promise<string> because attachment
 * downloads are async to avoid blocking the event loop.
 */
knoxx.backend.event_agents.event_summary_text = (function knoxx$backend$event_agents$event_summary_text(event){
var payload = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"payload","payload",-383036092).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var attachments = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var publish_channels = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var publish_text = ((cljs.core.seq(publish_channels))?(""+"\nPublish to channels:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70607_SHARP_){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__70607_SHARP_));
}),publish_channels)))+"\n"):null);
var base_text = (""+"Event source: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Event kind: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Event id: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Occurred at: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(event))+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var channel_id = temp__5825__auto__;
return (""+"Channel ID: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var message_id = temp__5825__auto__;
return (""+"Message ID: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var author = temp__5825__auto__;
return (""+"Author: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(author)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"repository","repository",1489835364).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var repository = temp__5825__auto__;
return (""+"Repository: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(repository)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var content = temp__5825__auto__;
return (""+"Content: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"summary","summary",380847952).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var summary = temp__5825__auto__;
return (""+"Summary: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"payloadPreview","payloadPreview",874931409).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var payload_preview = temp__5825__auto__;
return (""+"Payload preview: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(payload_preview)+"\n");
} else {
return null;
}
})()));
if(cljs.core.seq(attachments)){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (attachment){
return knoxx.backend.event_agents.download_attachment_to_tmp_BANG_(attachment).then((function (local_path){
var filename = new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment);
var url = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment);
if(cljs.core.truth_(local_path)){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)+" (saved to "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(local_path)+" \u2014 use the read tool to view it)");
} else {
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(filename)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(url)?(""+" <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+"> (download failed, use url directly)"):null)));
}
}));
}),attachments))).then((function (lines){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_text)+"\nAttachments (downloaded for reading):\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(lines)))+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = publish_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
}));
} else {
return Promise.resolve((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_text)+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = publish_text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
}
});
knoxx.backend.event_agents.event_summary_text_sync = (function knoxx$backend$event_agents$event_summary_text_sync(event){
var payload = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"payload","payload",-383036092).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var attachments = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var attachment_lines = ((cljs.core.seq(attachments))?(""+"Attachments:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (attachment){
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"filename","filename",-1428840783).cljs$core$IFn$_invoke$arity$1(attachment))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment))?(""+" <"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(attachment))+">"):null)));
}),attachments)))+"\n"):null);
var publish_channels = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
return (""+"Event source: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Event kind: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Event id: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(event))+"\n"+"Occurred at: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"timestamp","timestamp",579478971).cljs$core$IFn$_invoke$arity$1(event))+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var channel_id = temp__5825__auto__;
return (""+"Channel ID: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_id)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"messageId","messageId",-260575736).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var message_id = temp__5825__auto__;
return (""+"Message ID: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(message_id)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var author = temp__5825__auto__;
return (""+"Author: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(author)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"repository","repository",1489835364).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var repository = temp__5825__auto__;
return (""+"Repository: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(repository)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var content = temp__5825__auto__;
return (""+"Content: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(content)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(attachment_lines)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core.seq(publish_channels))?(""+"Publish channels: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",publish_channels))+"\n"):null))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"summary","summary",380847952).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var summary = temp__5825__auto__;
return (""+"Summary: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary)+"\n");
} else {
return null;
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var temp__5825__auto__ = new cljs.core.Keyword(null,"payloadPreview","payloadPreview",874931409).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(temp__5825__auto__)){
var payload_preview = temp__5825__auto__;
return (""+"Payload preview: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(payload_preview)+"\n");
} else {
return null;
}
})()));
});
/**
 * Regex to find media URLs in text content.
 */
knoxx.backend.event_agents.media_url_pattern = (function knoxx$backend$event_agents$media_url_pattern(){
return (new RegExp(/https?:\/\/\S+\.(?:png|jpg|jpeg|gif|webp|mp4|webm|mp3|wav|ogg|m4a|flac|pdf)/,"gi"));
});
/**
 * Extract media URLs from raw text content.
 */
knoxx.backend.event_agents.extract_media_urls_from_text = (function knoxx$backend$event_agents$extract_media_urls_from_text(text){
if(typeof text === 'string'){
var pattern = knoxx.backend.event_agents.media_url_pattern();
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70608_SHARP_){
return cljs.core.re_matches(pattern,p1__70608_SHARP_);
}),clojure.string.split.cljs$core$IFn$_invoke$arity$2(text,/\s+/))));
} else {
return null;
}
});
/**
 * Extract media URLs from Discord embeds.
 */
knoxx.backend.event_agents.extract_media_from_embeds = (function knoxx$backend$event_agents$extract_media_from_embeds(embeds){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (embed){
var url = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(embed);
if(cljs.core.truth_(url)){
var lower = clojure.string.lower_case(url);
if(cljs.core.truth_(cljs.core.some((function (p1__70609_SHARP_){
return clojure.string.includes_QMARK_(lower,p1__70609_SHARP_);
}),new cljs.core.PersistentVector(null, 8, 5, cljs.core.PersistentVector.EMPTY_NODE, [".png",".jpg",".jpeg",".gif",".webp",".mp4",".webm",".pdf"], null)))){
return url;
} else {
return null;
}
} else {
return null;
}
}),(function (){var or__5142__auto__ = embeds;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
/**
 * Download an image content-part's :url, embed as data URI in :data.
 */
knoxx.backend.event_agents.fetch_image_part_BANG_ = (function knoxx$backend$event_agents$fetch_image_part_BANG_(part){
return fetch(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part),({"method": "GET"})).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.arrayBuffer().then((function (buf){
var b64 = Buffer.from((new Uint8Array(buf))).toString("base64");
var mime = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"mimeType","mimeType",-995071690).cljs$core$IFn$_invoke$arity$1(part);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "image/jpeg";
}
})();
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(part,new cljs.core.Keyword(null,"data","data",-232669377),b64);
}));
} else {
console.warn("[event-agents] fetch-image-part! HTTP",resp.status,new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part));

return Promise.resolve(null);
}
})).catch((function (err){
console.warn("[event-agents] fetch-image-part! failed",err.message,new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(part));

return Promise.resolve(null);
}));
});
/**
 * Returns Promise<vec-of-parts> with image :url replaced by data URI :data.
 */
knoxx.backend.event_agents.materialize_content_parts_BANG_ = (function knoxx$backend$event_agents$materialize_content_parts_BANG_(parts){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (part){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("image",new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(part))){
return knoxx.backend.event_agents.fetch_image_part_BANG_(part);
} else {
return Promise.resolve(part);
}
}),parts))).then((function (arr){
return cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(arr)));
}));
});
knoxx.backend.event_agents.event_content_parts = (function knoxx$backend$event_agents$event_content_parts(event){
var payload = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"payload","payload",-383036092).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contentParts","contentParts",1395809695).cljs$core$IFn$_invoke$arity$1(payload);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
}
})());
});
knoxx.backend.event_agents.sticky_session_enabled_QMARK_ = (function knoxx$backend$event_agents$sticky_session_enabled_QMARK_(job){
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"stickySession","stickySession",1252676028)], null)) === true;
});
knoxx.backend.event_agents.sticky_session_max_messages = (function knoxx$backend$event_agents$sticky_session_max_messages(job){
return knoxx.backend.util.parse.parse_positive_int(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"sessionMaxMessages","sessionMaxMessages",-734097286)], null)));
});
knoxx.backend.event_agents.update_user_job_state_BANG_ = (function knoxx$backend$event_agents$update_user_job_state_BANG_(job_id,user_id,f){
var user_key = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_id));
var new_state = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.user_job_state_STAR_,cljs.core.update,user_key,(function (current){
var G__70610 = (function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
return (f.cljs$core$IFn$_invoke$arity$1 ? f.cljs$core$IFn$_invoke$arity$1(G__70610) : f.call(null,G__70610));
})),user_key);
var temp__5825__auto___70692 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___70692)){
var client_70693 = temp__5825__auto___70692;
knoxx.backend.redis_client.set_json.cljs$core$IFn$_invoke$arity$3(client_70693,(""+"event-agent:user-job-state:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(user_key)),new_state);
} else {
}

return new_state;
});
knoxx.backend.event_agents.normalize_user_job_state = (function knoxx$backend$event_agents$normalize_user_job_state(user_key,state){
if(cljs.core.map_QMARK_(state)){
return state;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.event_agents.sticky_session_base_conversation_id = (function knoxx$backend$event_agents$sticky_session_base_conversation_id(job,event){
var source_kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event)));
var author_id = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"authorId","authorId",-1664154012)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-user";
}
})();
var owner_id = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(source_kind,"discord"))?(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"channelId","channelId",2082229448)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return author_id;
}
})():author_id);
return (""+"event-agent-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(owner_id)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.lower_case(source_kind))+"-sticky");
});
knoxx.backend.event_agents.sticky_session_base_session_id = (function knoxx$backend$event_agents$sticky_session_base_session_id(job,event){
var source_kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event)));
var author_id = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"authorId","authorId",-1664154012)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "unknown-user";
}
})();
var owner_id = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(source_kind,"discord"))?(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"channelId","channelId",2082229448)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return author_id;
}
})():author_id);
return (""+"event-agent-session-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(owner_id)+"-sticky");
});
knoxx.backend.event_agents.sticky_session_summary = (function knoxx$backend$event_agents$sticky_session_summary(session){
var messages = cljs.core.vec(cljs.core.take_last((8),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.map_QMARK_,(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
if(cljs.core.seq(messages)){
return (""+"Previous sticky session summary:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (message){
var role = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "message";
}
})();
var content = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(role)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(content,(0),cljs.core.min.cljs$core$IFn$_invoke$arity$2((240),((content).length)))));
}),messages))));
} else {
return null;
}
});
knoxx.backend.event_agents.sticky_session_target = (function knoxx$backend$event_agents$sticky_session_target(job,event){
var job_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job);
if((!(knoxx.backend.event_agents.sticky_session_enabled_QMARK_(job)))){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),(""+"event-agent-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event)))))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())),new cljs.core.Keyword(null,"session-id","session-id",-1147060351),(""+"event-agent-session-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())),new cljs.core.Keyword(null,"summary","summary",380847952),null], null);
} else {
var state = cljs.core.get.cljs$core$IFn$_invoke$arity$3(cljs.core.deref(knoxx.backend.event_agents.job_state_STAR_),job_id,cljs.core.PersistentArrayMap.EMPTY);
var generation = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"stickyGeneration","stickyGeneration",-505534970).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})();
var base_conversation_id = knoxx.backend.event_agents.sticky_session_base_conversation_id(job,event);
var base_session_id = knoxx.backend.event_agents.sticky_session_base_session_id(job);
var current_conversation_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"stickyConversationId","stickyConversationId",1649052702).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if((generation === (0))){
return base_conversation_id;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_conversation_id)+"-r"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(generation));
}
}
})();
var current_session_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"stickySessionId","stickySessionId",1826249034).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if((generation === (0))){
return base_session_id;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_session_id)+"-r"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(generation));
}
}
})();
var existing_session = knoxx.backend.session_store.get_session_sync(current_session_id);
var message_limit = knoxx.backend.event_agents.sticky_session_max_messages(job);
var message_count = cljs.core.count((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"messages","messages",345434482).cljs$core$IFn$_invoke$arity$1(existing_session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var rollover_QMARK_ = (function (){var and__5140__auto__ = existing_session;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = message_limit;
if(cljs.core.truth_(and__5140__auto____$1)){
return (message_count >= message_limit);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})();
if(cljs.core.truth_(rollover_QMARK_)){
var next_generation = (generation + (1));
var next_conversation_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_conversation_id)+"-r"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(next_generation));
var next_session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base_session_id)+"-r"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(next_generation));
var summary = knoxx.backend.event_agents.sticky_session_summary(existing_session);
knoxx.backend.event_agents.update_job_state_BANG_(job_id,(function (current){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword(null,"stickyGeneration","stickyGeneration",-505534970),next_generation,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"stickyConversationId","stickyConversationId",1649052702),next_conversation_id,new cljs.core.Keyword(null,"stickySessionId","stickySessionId",1826249034),next_session_id], 0));
}));

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),next_conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),next_session_id,new cljs.core.Keyword(null,"summary","summary",380847952),summary], null);
} else {
knoxx.backend.event_agents.update_job_state_BANG_(job_id,(function (current){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic((function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),new cljs.core.Keyword(null,"stickyGeneration","stickyGeneration",-505534970),generation,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"stickyConversationId","stickyConversationId",1649052702),current_conversation_id,new cljs.core.Keyword(null,"stickySessionId","stickySessionId",1826249034),current_session_id], 0));
}));

return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913),current_conversation_id,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),current_session_id,new cljs.core.Keyword(null,"summary","summary",380847952),null], null);
}
}
});
/**
 * Build the agent run payload. Returns a Promise<map> because event-summary-text
 * is async (attachment downloads).
 */
knoxx.backend.event_agents.build_agent_run_payload = (function knoxx$backend$event_agents$build_agent_run_payload(config,job,event){
var agent_spec = new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(job);
var now = Date.now();
var run_id = (""+"event-agent-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job))+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(now));
var map__70611 = knoxx.backend.event_agents.sticky_session_target(job,event);
var map__70611__$1 = cljs.core.__destructure_map(map__70611);
var conversation_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70611__$1,new cljs.core.Keyword(null,"conversation-id","conversation-id",1220978913));
var session_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70611__$1,new cljs.core.Keyword(null,"session-id","session-id",-1147060351));
var summary = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70611__$1,new cljs.core.Keyword(null,"summary","summary",380847952));
var contract_id = (function (){var or__5142__auto__ = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"contractId","contractId",710260199).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(job));
}
}
}
})();
var event_actor = (knoxx.backend.event_agents.event_actor_id.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.event_agents.event_actor_id.cljs$core$IFn$_invoke$arity$1(event) : knoxx.backend.event_agents.event_actor_id.call(null,event));
var actor_id = (function (){var or__5142__auto__ = (cljs.core.truth_((function (){var and__5140__auto__ = event_actor;
if(cljs.core.truth_(and__5140__auto__)){
return (knoxx.backend.event_agents.job_permitted_for_event_actor_QMARK_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.event_agents.job_permitted_for_event_actor_QMARK_.cljs$core$IFn$_invoke$arity$2(job,event) : knoxx.backend.event_agents.job_permitted_for_event_actor_QMARK_.call(null,job,event));
} else {
return and__5140__auto__;
}
})())?event_actor:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(agent_spec));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(job));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(job));
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
return knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(job));
}
}
}
}
}
}
})();
var content_parts = knoxx.backend.event_agents.event_content_parts(event);
var model_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "gemma4:31b";
}
})();
var task_prompt = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"taskPrompt","taskPrompt",944614720).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"task-prompt","task-prompt",-349302716).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var system_prompt = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"systemPrompt","systemPrompt",-590399886).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"system-prompt","system-prompt",362593429).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "You are a Knoxx event agent.";
}
}
})();
var thinking_level = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"thinkingLevel","thinkingLevel",1530898429).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"thinking-level","thinking-level",2081595953).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "off";
}
}
})();
var tool_policies = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"toolPolicies","toolPolicies",-136088976).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var memory_hydration = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"memoryHydration","memoryHydration",-226352759).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"memory-hydration","memory-hydration",1956326082).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
})();
var context_policy = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contextPolicy","contextPolicy",683316353).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"context-policy","context-policy",-1770881557).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"context","context",-830191113).cljs$core$IFn$_invoke$arity$1(agent_spec);
}
}
})();
var preamble = (""+"An event matched this job.\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(task_prompt)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(task_prompt))?null:"\n\n"))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((function (){var or__5142__auto__ = summary;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary)+"\n\n"))));
return knoxx.backend.event_agents.event_summary_text(event).then((function (summary_text){
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"run_id","run_id",-556768024),run_id,new cljs.core.Keyword(null,"message","message",-406056002),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(preamble)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary_text)),new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),content_parts,new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),(function (){var G__70612 = new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"role","role",-736691072),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knowledge_worker";
}
})(),new cljs.core.Keyword(null,"system_prompt","system_prompt",-655033954),system_prompt,new cljs.core.Keyword(null,"task_prompt","task_prompt",1276696196),task_prompt,new cljs.core.Keyword(null,"model","model",331153215),model_id,new cljs.core.Keyword(null,"thinking_level","thinking_level",165057069),thinking_level,new cljs.core.Keyword(null,"tool_policies","tool_policies",24080177),knoxx.backend.event_agents.tool_policies__GT_js(tool_policies)], null);
var G__70612__$1 = (cljs.core.truth_(contract_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70612,new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),contract_id):G__70612);
var G__70612__$2 = (cljs.core.truth_(actor_id)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70612__$1,new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),actor_id):G__70612__$1);
var G__70612__$3 = (cljs.core.truth_(memory_hydration)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70612__$2,new cljs.core.Keyword(null,"memory_hydration","memory_hydration",-1458677455),memory_hydration):G__70612__$2);
if(cljs.core.truth_(context_policy)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__70612__$3,new cljs.core.Keyword(null,"context_policy","context_policy",1230169154),context_policy);
} else {
return G__70612__$3;
}
})(),new cljs.core.Keyword(null,"model","model",331153215),model_id], null);
}));
});
knoxx.backend.event_agents.job_step = (function knoxx$backend$event_agents$job_step(job){
var or__5142__auto__ = new cljs.core.Keyword(null,"step","step",1288888124).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"uses","uses",232664692),"run-agent",new cljs.core.Keyword(null,"with","with",-1536296876),new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$2(job,cljs.core.PersistentArrayMap.EMPTY)], null);
}
});
/**
 * Return the streaming behavior for a job when the session is already active.
 * :steer     — send the message as a steering directive to the running session
 * :follow-up — queue the message for when the current turn finishes (default)
 */
knoxx.backend.event_agents.streaming_behavior = (function knoxx$backend$event_agents$streaming_behavior(job){
var raw = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"data","data",-232669377),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"config","config",994861415),new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748)], null));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"streamingBehavior","streamingBehavior",-821454748)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return "follow-up";
}
}
}
}
})();
var G__70613 = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(raw)));
switch (G__70613) {
case "steer":
return new cljs.core.Keyword(null,"steer","steer",1705380684);

break;
case "follow-up":
return new cljs.core.Keyword(null,"follow-up","follow-up",-533605152);

break;
case "followup":
return new cljs.core.Keyword(null,"follow-up","follow-up",-533605152);

break;
case "queue":
return new cljs.core.Keyword(null,"follow-up","follow-up",-533605152);

break;
case "idle-only":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
case "idle_only":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
case "drop-if-busy":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
case "drop_if_busy":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
case "ignore-if-busy":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
case "ignore_if_busy":
return new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598);

break;
default:
return new cljs.core.Keyword(null,"follow-up","follow-up",-533605152);

}
});
/**
 * Send a steering message to a running agent session via /api/knoxx/steer.
 */
knoxx.backend.event_agents.steer_running_session_BANG_ = (function knoxx$backend$event_agents$steer_running_session_BANG_(config,job,event,session_id,conversation_id){
return knoxx.backend.event_agents.event_summary_text(event).then((function (summary_text){
var message = (""+"[Steer] New event for this agent:\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary_text));
var body = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"message","message",-406056002),message,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null);
return knoxx.backend.event_agents.fetch_json_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config))+"/api/knoxx/steer"),({"method": "POST", "headers": knoxx.backend.event_agents.direct_start_headers(config), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (result){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] steered session",session_id,"for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job)], 0));

return result;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] failed to steer session for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),":",err.message], 0));

return Promise.reject(err);
}));
}));
});
/**
 * Send a follow-up message to a running agent session via /api/knoxx/follow-up.
 */
knoxx.backend.event_agents.follow_up_running_session_BANG_ = (function knoxx$backend$event_agents$follow_up_running_session_BANG_(config,job,event,session_id,conversation_id){
return knoxx.backend.event_agents.event_summary_text(event).then((function (summary_text){
var message = (""+"[Follow-up] New event for this agent:\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(summary_text));
var body = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"message","message",-406056002),message,new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980),conversation_id,new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id], null);
return knoxx.backend.event_agents.fetch_json_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"knoxx-base-url","knoxx-base-url",-158933143).cljs$core$IFn$_invoke$arity$1(config))+"/api/knoxx/follow-up"),({"method": "POST", "headers": knoxx.backend.event_agents.direct_start_headers(config), "body": JSON.stringify(cljs.core.clj__GT_js(body))})).then((function (result){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] follow-up queued",session_id,"for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job)], 0));

return result;
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] failed to follow-up for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),":",err.message], 0));

return Promise.reject(err);
}));
}));
});
knoxx.backend.event_agents.start_agent_run_BANG_ = (function knoxx$backend$event_agents$start_agent_run_BANG_(config,job,event){
return knoxx.backend.event_agents.build_agent_run_payload(config,job,event).then((function (raw_body){
return knoxx.backend.event_agents.materialize_content_parts_BANG_(new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667).cljs$core$IFn$_invoke$arity$1(raw_body)).then((function (materialized_parts){
var body = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(raw_body,new cljs.core.Keyword(null,"content_parts","content_parts",-2046424667),materialized_parts);
return knoxx.backend.agents.runner.spawn_direct_BANG_.cljs$core$IFn$_invoke$arity$2(config,body).then((function (result){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] queued run",new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(body),"for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),"event",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(event)], 0));

return result;
})).catch((function (err){
var msg = err.message;
var session_id = new cljs.core.Keyword(null,"session_id","session_id",1584799627).cljs$core$IFn$_invoke$arity$1(body);
var conversation_id = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(body);
var behavior = knoxx.backend.event_agents.streaming_behavior(job);
if(((clojure.string.includes_QMARK_(msg,"agent_already_processing")) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(behavior,new cljs.core.Keyword(null,"steer","steer",1705380684))))){
return knoxx.backend.event_agents.steer_running_session_BANG_(config,job,event,session_id,conversation_id);
} else {
if(((clojure.string.includes_QMARK_(msg,"agent_already_processing")) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(behavior,new cljs.core.Keyword(null,"follow-up","follow-up",-533605152))))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] session active, queuing follow-up for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job)], 0));

return knoxx.backend.event_agents.follow_up_running_session_BANG_(config,job,event,session_id,conversation_id);
} else {
if(((clojure.string.includes_QMARK_(msg,"agent_already_processing")) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(behavior,new cljs.core.Keyword(null,"idle-only","idle-only",-1364594598))))){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] session busy, dropping idle-only event for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job)], 0));

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"skipped","skipped",-1144887090),true,new cljs.core.Keyword(null,"reason","reason",-2070751759),"session_busy"], null));
} else {
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] failed to queue run for job",new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),":",msg], 0));

return Promise.reject(err);
}
}
}
}));
}));
}));
});
/**
 * Check if event kinds match the job's configured events.
 * 
 * LEGACY MODE (default): :always and :maybe are treated as a simple union.
 * Any matching event kind triggers the job.
 * 
 * SCORING MODE (opt-in): When :threshold or :weights are present, :always kinds
 * are required (all must match) and :maybe kinds contribute to a score that
 * must meet or exceed :eventThreshold.
 */
knoxx.backend.event_agents.matches_event_kind_QMARK_ = (function knoxx$backend$event_agents$matches_event_kind_QMARK_(job,event_kinds){
var configured = cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var kinds = ((typeof event_kinds === 'string')?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [event_kinds], null):event_kinds);
var always_kinds = cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"alwaysKinds","alwaysKinds",-464748381)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var maybe_kinds = cljs.core.vec((function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"maybeKinds","maybeKinds",-549657535)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var weights = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"eventWeights","eventWeights",2010194476)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var threshold = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"eventThreshold","eventThreshold",-167593365)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (1);
}
})();
var kind_strs = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,kinds));
var scoring_mode_QMARK_ = ((cljs.core.seq(weights)) || ((threshold > (1))));
if(scoring_mode_QMARK_){
var always_strs = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,always_kinds));
var maybe_strs = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,maybe_kinds));
var always_met_QMARK_ = ((cljs.core.empty_QMARK_(always_strs)) || (cljs.core.every_QMARK_((function (p1__70614_SHARP_){
return cljs.core.contains_QMARK_(kind_strs,p1__70614_SHARP_);
}),always_strs)));
var score = ((cljs.core.empty_QMARK_(maybe_strs))?(0):cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,mk){
if(cljs.core.contains_QMARK_(kind_strs,mk)){
return (acc + cljs.core.get.cljs$core$IFn$_invoke$arity$3(weights,mk,(1)));
} else {
return acc;
}
}),(0),maybe_strs));
var score_met_QMARK_ = (score >= threshold);
return ((always_met_QMARK_) && (score_met_QMARK_));
} else {
var or__5142__auto__ = cljs.core.empty_QMARK_(configured);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return cljs.core.some((function (ek){
return cljs.core.some((function (conf){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(ek)),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(conf)));
}),configured);
}),kinds);
}
}
});
knoxx.backend.event_agents.matches_repository_QMARK_ = (function knoxx$backend$event_agents$matches_repository_QMARK_(job,repository){
var allowlist = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (value){
var G__70616 = value;
var G__70616__$1 = (((G__70616 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70616)));
var G__70616__$2 = (((G__70616__$1 == null))?null:clojure.string.trim(G__70616__$1));
if((G__70616__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70616__$2);
}
}),(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"filters","filters",974726919),new cljs.core.Keyword(null,"repositories","repositories",1367837581)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
var or__5142__auto__ = cljs.core.empty_QMARK_(allowlist);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return cljs.core.some((function (p1__70615_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__70615_SHARP_,repository);
}),allowlist);
}
});
knoxx.backend.event_agents.matches_channel_QMARK_ = (function knoxx$backend$event_agents$matches_channel_QMARK_(control,job,channel_id){
var channels = knoxx.backend.event_agents.job_channels(control,job);
var or__5142__auto__ = cljs.core.empty_QMARK_(channels);
if(or__5142__auto__){
return or__5142__auto__;
} else {
return cljs.core.some((function (p1__70617_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__70617_SHARP_,channel_id);
}),channels);
}
});
knoxx.backend.event_agents.matches_keywords_QMARK_ = (function knoxx$backend$event_agents$matches_keywords_QMARK_(control,job,content){
var keywords = knoxx.backend.event_agents.job_keywords(control,job);
var lowered = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = content;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
var or__5142__auto__ = knoxx.backend.event_agents.job_match_all_QMARK_(job);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.empty_QMARK_(keywords);
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
return cljs.core.some((function (p1__70618_SHARP_){
return clojure.string.includes_QMARK_(lowered,p1__70618_SHARP_);
}),keywords);
}
}
});
knoxx.backend.event_agents.matches_author_QMARK_ = (function knoxx$backend$event_agents$matches_author_QMARK_(job,author_id){
var author_ids = knoxx.backend.event_agents.job_author_ids(job);
var normalized_author_id = (function (){var G__70620 = author_id;
var G__70620__$1 = (((G__70620 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70620)));
var G__70620__$2 = (((G__70620__$1 == null))?null:clojure.string.trim(G__70620__$1));
if((G__70620__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70620__$2);
}
})();
var or__5142__auto__ = cljs.core.empty_QMARK_(author_ids);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var and__5140__auto__ = normalized_author_id;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.some((function (p1__70619_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__70619_SHARP_,normalized_author_id);
}),author_ids);
} else {
return and__5140__auto__;
}
}
});
knoxx.backend.event_agents.mention_event_QMARK_ = (function knoxx$backend$event_agents$mention_event_QMARK_(event_kind){
return cljs.core.some((function (p1__70621_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)),p1__70621_SHARP_);
}),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord.message.mention"], null));
});
knoxx.backend.event_agents.event_actor_id = (function knoxx$backend$event_agents$event_actor_id(event){
var payload = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"payload","payload",-383036092).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var or__5142__auto__ = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"sourceActorId","sourceActorId",968454262).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
var or__5142__auto____$5 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533).cljs$core$IFn$_invoke$arity$1(event));
if(cljs.core.truth_(or__5142__auto____$5)){
return or__5142__auto____$5;
} else {
var or__5142__auto____$6 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"sourceActorId","sourceActorId",968454262).cljs$core$IFn$_invoke$arity$1(event));
if(cljs.core.truth_(or__5142__auto____$6)){
return or__5142__auto____$6;
} else {
var or__5142__auto____$7 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(event));
if(cljs.core.truth_(or__5142__auto____$7)){
return or__5142__auto____$7;
} else {
var or__5142__auto____$8 = knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(event));
if(cljs.core.truth_(or__5142__auto____$8)){
return or__5142__auto____$8;
} else {
return knoxx.backend.event_agents.nonblank_str(new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(event));
}
}
}
}
}
}
}
}
}
});
knoxx.backend.event_agents.job_actor_claims = (function knoxx$backend$event_agents$job_actor_claims(job){
var agent_spec = new cljs.core.Keyword(null,"agentSpec","agentSpec",933621050).cljs$core$IFn$_invoke$arity$1(job);
var claims = knoxx.backend.contracts.actor_scope.normalize_actor_claims((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contractActors","contractActors",47284059).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"contract_actor_ids","contract_actor_ids",1798190864).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"contractActorIds","contractActorIds",773188603).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"contract-actor-ids","contract-actor-ids",1506474817).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"contractActors","contractActors",47284059).cljs$core$IFn$_invoke$arity$1(job);
}
}
}
}
}
})());
if(cljs.core.seq(claims)){
return claims;
} else {
return knoxx.backend.contracts.actor_scope.normalize_actor_claims((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(agent_spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(job);
}
}
}
}
}
})());
}
});
knoxx.backend.event_agents.job_permitted_for_event_actor_QMARK_ = (function knoxx$backend$event_agents$job_permitted_for_event_actor_QMARK_(job,event){
var temp__5823__auto__ = knoxx.backend.event_agents.event_actor_id(event);
if(cljs.core.truth_(temp__5823__auto__)){
var actor_id = temp__5823__auto__;
return knoxx.backend.contracts.actor_scope.actor_allowed_QMARK_(knoxx.backend.event_agents.job_actor_claims(job),actor_id);
} else {
return true;
}
});
knoxx.backend.event_agents.job_matches_event_QMARK_ = (function knoxx$backend$event_agents$job_matches_event_QMARK_(control,job,event){
var payload = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"payload","payload",-383036092).cljs$core$IFn$_invoke$arity$1(event);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var event_kind = new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(event);
var event_kinds = (function (){var or__5142__auto__ = cljs.core.seq(new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289).cljs$core$IFn$_invoke$arity$1(event));
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [event_kind], null);
}
})();
var and__5140__auto__ = new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)));
if(and__5140__auto____$1){
var and__5140__auto____$2 = cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889).cljs$core$IFn$_invoke$arity$1(event))));
if(and__5140__auto____$2){
var and__5140__auto____$3 = knoxx.backend.event_agents.job_permitted_for_event_actor_QMARK_(job,event);
if(cljs.core.truth_(and__5140__auto____$3)){
var and__5140__auto____$4 = knoxx.backend.event_agents.matches_event_kind_QMARK_(job,event_kinds);
if(cljs.core.truth_(and__5140__auto____$4)){
var and__5140__auto____$5 = (function (){var or__5142__auto__ = clojure.string.starts_with_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)),"discord.voice");
if(or__5142__auto__){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.matches_channel_QMARK_(control,job,new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(payload));
}
})();
if(cljs.core.truth_(and__5140__auto____$5)){
var and__5140__auto____$6 = knoxx.backend.event_agents.matches_author_QMARK_(job,new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(payload));
if(cljs.core.truth_(and__5140__auto____$6)){
var and__5140__auto____$7 = (function (){var or__5142__auto__ = knoxx.backend.event_agents.mention_event_QMARK_(event_kind);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.matches_keywords_QMARK_(control,job,new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(payload));
}
})();
if(cljs.core.truth_(and__5140__auto____$7)){
return knoxx.backend.event_agents.matches_repository_QMARK_(job,new cljs.core.Keyword(null,"repository","repository",1489835364).cljs$core$IFn$_invoke$arity$1(payload));
} else {
return and__5140__auto____$7;
}
} else {
return and__5140__auto____$6;
}
} else {
return and__5140__auto____$5;
}
} else {
return and__5140__auto____$4;
}
} else {
return and__5140__auto____$3;
}
} else {
return and__5140__auto____$2;
}
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
knoxx.backend.event_agents.dispatch_event_BANG_ = (function knoxx$backend$event_agents$dispatch_event_BANG_(event){
var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
var normalized_base = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+"event-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(Date.now())),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(new Date()).toISOString(),new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"manual",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"manual.event",new cljs.core.Keyword(null,"payload","payload",-383036092),cljs.core.PersistentArrayMap.EMPTY], null),(function (){var or__5142__auto__ = event;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()], 0));
var all_kinds = (function (){var or__5142__auto__ = cljs.core.seq(new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289).cljs$core$IFn$_invoke$arity$1(normalized_base));
if(or__5142__auto__){
return or__5142__auto__;
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"eventKind","eventKind",2138897648).cljs$core$IFn$_invoke$arity$1(normalized_base)], null);
}
})();
var matching_jobs = cljs.core.vec(cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,job){
if(cljs.core.truth_(cljs.core.some((function (p1__70624_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__70624_SHARP_),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job));
}),acc))){
return acc;
} else {
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(acc,job);
}
}),cljs.core.PersistentVector.EMPTY,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (kind){
var e = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(normalized_base,new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),kind);
return cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70623_SHARP_){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__70623_SHARP_,new cljs.core.Keyword("knoxx.backend.event-agents","matched-kind","knoxx.backend.event-agents/matched-kind",11820205),kind);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70622_SHARP_){
return knoxx.backend.event_agents.job_matches_event_QMARK_(control,p1__70622_SHARP_,e);
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control)));
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([all_kinds], 0))));
var normalized_event = normalized_base;
knoxx.backend.event_agents.append_recent_event_BANG_(normalized_event);

if((((!(clojure.string.blank_QMARK_(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(normalized_event))))) && ((!(knoxx.backend.event_agents.mark_event_dispatched_BANG_(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(normalized_event))))))){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"event","event",301435442),normalized_event,new cljs.core.Keyword(null,"skipped","skipped",-1144887090),true], null));
} else {
if(cljs.core.empty_QMARK_(matching_jobs)){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"event","event",301435442),normalized_event], null));
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (job){
var started_at = knoxx.backend.event_agents.record_job_run_start_BANG_(job);
var step = knoxx.backend.event_agents.job_step(job);
return ((((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("discord",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))) && (knoxx.backend.event_agents.synthesis_source_mode_QMARK_(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"mode","mode",654403691)], null))))))?(knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$4 ? knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$4(config,control,job,normalized_event) : knoxx.backend.event_agents.execute_discord_synthesis_BANG_.call(null,config,control,job,normalized_event)):knoxx.backend.actions.dispatch.dispatch_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"event","event",301435442),normalized_event,new cljs.core.Keyword(null,"job","job",850873087),job,new cljs.core.Keyword(null,"run-agent!","run-agent!",-1490275853),knoxx.backend.event_agents.start_agent_run_BANG_], null),step)).then((function (result){
knoxx.backend.event_agents.record_job_run_finish_BANG_(job,started_at,"ok",null);

return result;
})).catch((function (err){
knoxx.backend.event_agents.record_job_run_finish_BANG_(job,started_at,"error",err.message);

return null;
}));
}),matching_jobs))).then((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"matchedJobs","matchedJobs",-1838413822),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092),matching_jobs),new cljs.core.Keyword(null,"event","event",301435442),normalized_event], null);
}));
}
}
});
knoxx.backend.event_agents.discord_bot_user_id = (function knoxx$backend$event_agents$discord_bot_user_id(control){
var or__5142__auto__ = (function (){var G__70625 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(control,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"botUserId","botUserId",971606494)], null));
var G__70625__$1 = (((G__70625 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70625)));
var G__70625__$2 = (((G__70625__$1 == null))?null:clojure.string.trim(G__70625__$1));
if((G__70625__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70625__$2);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.discord_gateway_user_id();
}
});
knoxx.backend.event_agents.discord_event_jobs = (function knoxx$backend$event_agents$discord_event_jobs(control){
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (job){
var and__5140__auto__ = new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job);
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("event",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("discord",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)))));
} else {
return and__5140__auto__;
}
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control)));
});
knoxx.backend.event_agents.discord_union_keywords = (function knoxx$backend$event_agents$discord_union_keywords(control){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (p1__70626_SHARP_){
return knoxx.backend.event_agents.job_keywords(control,p1__70626_SHARP_);
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([knoxx.backend.event_agents.discord_event_jobs(control)], 0))));
});
knoxx.backend.event_agents.dispatch_discord_gateway_message_BANG_ = (function knoxx$backend$event_agents$dispatch_discord_gateway_message_BANG_(message){
var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
var content = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message))+""));
var bot_user_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.discord_bot_user_id(control);
}
})();
var mention_QMARK_ = (function (){var and__5140__auto__ = bot_user_id;
if(cljs.core.truth_(and__5140__auto__)){
return ((clojure.string.includes_QMARK_(content,(""+"<@"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bot_user_id)+">"))) || (clojure.string.includes_QMARK_(content,(""+"<@!"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(bot_user_id)+">"))));
} else {
return and__5140__auto__;
}
})();
var keyword_QMARK_ = cljs.core.some((function (p1__70627_SHARP_){
return clojure.string.includes_QMARK_(content,p1__70627_SHARP_);
}),knoxx.backend.event_agents.discord_union_keywords(control));
var guild_id = new cljs.core.Keyword(null,"guildId","guildId",-559818490).cljs$core$IFn$_invoke$arity$1(message);
var author_id = new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message);
var author_voice_channel = ((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guild_id)+"")))?null:cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.event_agents.source_state_STAR_),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"voice-states","voice-states",-1192023890),guild_id,author_id], null)));
var payload = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696),new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"messageId","messageId",-260575736),new cljs.core.Keyword(null,"attachments","attachments",-1535547830),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533),new cljs.core.Keyword(null,"authorVoiceChannelId","authorVoiceChannelId",-2011707185),new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121),new cljs.core.Keyword(null,"embeds","embeds",833349080)],[new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696).cljs$core$IFn$_invoke$arity$1(message),author_id,guild_id,new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"attachments","attachments",-1535547830).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533).cljs$core$IFn$_invoke$arity$1(message),author_voice_channel,new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121).cljs$core$IFn$_invoke$arity$1(message),cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"embeds","embeds",833349080).cljs$core$IFn$_invoke$arity$1(message);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())]);
if(cljs.core.truth_(new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121).cljs$core$IFn$_invoke$arity$1(message))){
return null;
} else {
knoxx.backend.event_agents.remember_discord_latest_BANG_(new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [message], null));

return knoxx.backend.event_agents.dispatch_event_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"discord",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"discord.message.created",new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),(function (){var G__70628 = new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord.message.created"], null);
var G__70628__$1 = (cljs.core.truth_(mention_QMARK_)?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__70628,"discord.message.mention"):G__70628);
if(cljs.core.truth_(keyword_QMARK_)){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__70628__$1,"discord.message.keyword");
} else {
return G__70628__$1;
}
})(),new cljs.core.Keyword(null,"payload","payload",-383036092),payload], null));
}
});
knoxx.backend.event_agents.bind_discord_gateway_BANG_ = (function knoxx$backend$event_agents$bind_discord_gateway_BANG_(_config){
return knoxx.backend.events.sources.discord.bind_gateways_BANG_(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183),knoxx.backend.event_agents.policy_db(),new cljs.core.Keyword(null,"on-message!","on-message!",-607128138),knoxx.backend.event_agents.dispatch_discord_gateway_message_BANG_,new cljs.core.Keyword(null,"on-voice-state!","on-voice-state!",2001605581),knoxx.backend.event_agents.dispatch_voice_state_update_BANG_], null));
});
knoxx.backend.event_agents.dispatch_voice_state_update_BANG_ = (function knoxx$backend$event_agents$dispatch_voice_state_update_BANG_(p__70629){
var map__70630 = p__70629;
var map__70630__$1 = cljs.core.__destructure_map(map__70630);
var data = map__70630__$1;
var action = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"action","action",-811238024));
var userId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"userId","userId",575594135));
var username = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"username","username",1605666410));
var guildId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"guildId","guildId",-559818490));
var channelId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"channelId","channelId",2082229448));
var oldChannelId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"oldChannelId","oldChannelId",917461643));
var newChannelId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70630__$1,new cljs.core.Keyword(null,"newChannelId","newChannelId",-1471071875));
if((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(userId)+""))))){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(guildId)+""))){
} else {
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(newChannelId)+""))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.event_agents.source_state_STAR_,(function (state){
var updated = cljs.core.update_in.cljs$core$IFn$_invoke$arity$4(state,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"voice-states","voice-states",-1192023890),guildId], null),cljs.core.dissoc,userId);
if(cljs.core.empty_QMARK_(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(updated,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"voice-states","voice-states",-1192023890),guildId], null)))){
return cljs.core.update_in.cljs$core$IFn$_invoke$arity$4(updated,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"voice-states","voice-states",-1192023890)], null),cljs.core.dissoc,guildId);
} else {
return updated;
}
}));
} else {
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.event_agents.source_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.Keyword(null,"voice-states","voice-states",-1192023890),guildId,userId], null),newChannelId);
}
}

return knoxx.backend.event_agents.dispatch_event_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"discord",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"discord.voice.state_update",new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["discord.voice.state_update"], null),new cljs.core.Keyword(null,"payload","payload",-383036092),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696),new cljs.core.Keyword(null,"guildId","guildId",-559818490),new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"username","username",1605666410),new cljs.core.Keyword(null,"oldChannelId","oldChannelId",917461643),new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533),new cljs.core.Keyword(null,"userId","userId",575594135),new cljs.core.Keyword(null,"action","action",-811238024),new cljs.core.Keyword(null,"newChannelId","newChannelId",-1471071875)],[new cljs.core.Keyword(null,"gatewayBotUserId","gatewayBotUserId",-989125696).cljs$core$IFn$_invoke$arity$1(data),guildId,channelId,username,oldChannelId,new cljs.core.Keyword(null,"gatewayActorId","gatewayActorId",1232391533).cljs$core$IFn$_invoke$arity$1(data),userId,action,newChannelId])], null));
} else {
return null;
}
});
knoxx.backend.event_agents.dispatch_discord_message_event_BANG_ = (function knoxx$backend$event_agents$dispatch_discord_message_event_BANG_(control,job,message,match_kind){
return knoxx.backend.event_agents.dispatch_event_BANG_(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"discord",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),match_kind,new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"channelId","channelId",2082229448),new cljs.core.Keyword(null,"channelId","channelId",2082229448).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"authorId","authorId",-1664154012),new cljs.core.Keyword(null,"authorId","authorId",-1664154012).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965),new cljs.core.Keyword(null,"authorUsername","authorUsername",177189965).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121),new cljs.core.Keyword(null,"authorIsBot","authorIsBot",-1582823121).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"messageId","messageId",-260575736),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(message)], null)], null));
});
knoxx.backend.event_agents.discord_message_match_kind = (function knoxx$backend$event_agents$discord_message_match_kind(control,job,message){
return knoxx.backend.events.sources.discord.message_match_kind(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"bot-user-id","bot-user-id",1703682999),knoxx.backend.event_agents.discord_bot_user_id(control),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message),new cljs.core.Keyword(null,"keyword?","keyword?",277265542),(function (){var and__5140__auto__ = (!(knoxx.backend.event_agents.job_match_all_QMARK_(job)));
if(and__5140__auto__){
return knoxx.backend.event_agents.matches_keywords_QMARK_(control,job,new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(message));
} else {
return and__5140__auto__;
}
})(),new cljs.core.Keyword(null,"created?","created?",850508195),knoxx.backend.event_agents.matches_event_kind_QMARK_(job,"discord.message.created"),new cljs.core.Keyword(null,"match-all?","match-all?",-1394896600),knoxx.backend.event_agents.job_match_all_QMARK_(job)], null));
});
knoxx.backend.event_agents.resolve_job_channel_ids_BANG_ = (function knoxx$backend$event_agents$resolve_job_channel_ids_BANG_(control,job){
return knoxx.backend.events.sources.discord.resolve_channel_ids_BANG_(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"explicit-channels","explicit-channels",1984056567),knoxx.backend.event_agents.job_channels(control,job),new cljs.core.Keyword(null,"guild-ids","guild-ids",914840032),knoxx.backend.event_agents.job_guild_ids(job),new cljs.core.Keyword(null,"publish-channels","publish-channels",1648651117),knoxx.backend.event_agents.job_publish_channels(job)], null));
});
knoxx.backend.event_agents.execute_discord_patrol_BANG_ = (function knoxx$backend$event_agents$execute_discord_patrol_BANG_(config,control,job){
var limit = knoxx.backend.event_agents.job_max_messages(job,(25));
return knoxx.backend.event_agents.resolve_job_channel_ids_BANG_(control,job).then((function (channels){
return knoxx.backend.events.sources.discord.execute_patrol_BANG_(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738),channels,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"unseen-messages","unseen-messages",-938745644),knoxx.backend.event_agents.unseen_discord_messages,new cljs.core.Keyword(null,"remember-latest!","remember-latest!",-903750029),knoxx.backend.event_agents.remember_discord_latest_BANG_,new cljs.core.Keyword(null,"match-kind","match-kind",83311455),cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.event_agents.discord_message_match_kind,control,job),new cljs.core.Keyword(null,"dispatch-message!","dispatch-message!",-1354158307),cljs.core.partial.cljs$core$IFn$_invoke$arity$3(knoxx.backend.event_agents.dispatch_discord_message_event_BANG_,control,job)], null));
}));
});
knoxx.backend.event_agents.summarize_discord_channel = (function knoxx$backend$event_agents$summarize_discord_channel(channel_id,messages){
return knoxx.backend.events.sources.discord.summarize_channel(channel_id,messages);
});
knoxx.backend.event_agents.synthesis_channel_ids_BANG_ = (function knoxx$backend$event_agents$synthesis_channel_ids_BANG_(control,job,trigger_event){
return knoxx.backend.event_agents.resolve_job_channel_ids_BANG_(control,job).then((function (channels){
var trigger_channel = (function (){var G__70631 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(trigger_event,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.Keyword(null,"channelId","channelId",2082229448)], null));
var G__70631__$1 = (((G__70631 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__70631)));
var G__70631__$2 = (((G__70631__$1 == null))?null:clojure.string.trim(G__70631__$1));
if((G__70631__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__70631__$2);
}
})();
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(channels,(cljs.core.truth_(trigger_channel)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [trigger_channel], null):null))));
}));
});
knoxx.backend.event_agents.discord_synthesis_trigger_summary_BANG_ = (function knoxx$backend$event_agents$discord_synthesis_trigger_summary_BANG_(trigger_event){
if(cljs.core.truth_(trigger_event)){
return knoxx.backend.event_agents.event_summary_text(trigger_event).then((function (txt){
return (""+"Triggering event:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(txt));
}));
} else {
return Promise.resolve(null);
}
});
knoxx.backend.event_agents.discord_synthesis_image_attachments = (function knoxx$backend$event_agents$discord_synthesis_image_attachments(rows){
return knoxx.backend.events.sources.discord.image_attachments(rows);
});
knoxx.backend.event_agents.discord_synthesis_dispatch_summary_BANG_ = (function knoxx$backend$event_agents$discord_synthesis_dispatch_summary_BANG_(config,job,publish_channels,trigger_event,channels,rows){
var channel_summary = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__70632){
var map__70633 = p__70632;
var map__70633__$1 = cljs.core.__destructure_map(map__70633);
var channelId = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70633__$1,new cljs.core.Keyword(null,"channelId","channelId",2082229448));
var messages = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70633__$1,new cljs.core.Keyword(null,"messages","messages",345434482));
return knoxx.backend.event_agents.summarize_discord_channel(channelId,messages);
}),rows)));
var base_summary = ((clojure.string.blank_QMARK_(channel_summary))?null:(""+"Recent Discord context:\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(channel_summary)));
return knoxx.backend.event_agents.discord_synthesis_trigger_summary_BANG_(trigger_event).then((function (trigger_summary){
var summary = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n\n",cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [trigger_summary,base_summary], null)));
if(clojure.string.blank_QMARK_(summary)){
return Promise.resolve(null);
} else {
return knoxx.backend.actions.dispatch.dispatch_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"event","event",301435442),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),"discord",new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),"discord.snapshot.summary",new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(new Date()).toISOString(),new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"summary","summary",380847952),summary,new cljs.core.Keyword(null,"channelId","channelId",2082229448),cljs.core.first(channels),new cljs.core.Keyword(null,"publishChannels","publishChannels",45677262),publish_channels,new cljs.core.Keyword(null,"attachments","attachments",-1535547830),knoxx.backend.event_agents.discord_synthesis_image_attachments(rows)], null)], null),new cljs.core.Keyword(null,"job","job",850873087),job,new cljs.core.Keyword(null,"run-agent!","run-agent!",-1490275853),knoxx.backend.event_agents.start_agent_run_BANG_], null),knoxx.backend.event_agents.job_step(job));
}
}));
});
knoxx.backend.event_agents.execute_discord_synthesis_BANG_ = (function knoxx$backend$event_agents$execute_discord_synthesis_BANG_(var_args){
var G__70635 = arguments.length;
switch (G__70635) {
case 3:
return knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (config,control,job){
return knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$4(config,control,job,null);
}));

(knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (config,control,job,trigger_event){
var limit = knoxx.backend.event_agents.job_max_messages(job,(12));
var publish_channels = knoxx.backend.event_agents.job_publish_channels(job);
return knoxx.backend.event_agents.synthesis_channel_ids_BANG_(control,job,trigger_event).then((function (channels){
return knoxx.backend.events.sources.discord.execute_synthesis_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"channel-ids","channel-ids",780502738),channels,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"dispatch-summary!","dispatch-summary!",455883021),(function (rows){
return knoxx.backend.event_agents.discord_synthesis_dispatch_summary_BANG_(config,job,publish_channels,trigger_event,channels,rows);
})], null));
}));
}));

(knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.event_agents.execute_direct_job_BANG_ = (function knoxx$backend$event_agents$execute_direct_job_BANG_(config,job,source_kind,event_kind){
return knoxx.backend.actions.dispatch.dispatch_BANG_(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"config","config",994861415),config,new cljs.core.Keyword(null,"event","event",301435442),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"sourceKind","sourceKind",-1570414889),source_kind,new cljs.core.Keyword(null,"eventKind","eventKind",2138897648),event_kind,new cljs.core.Keyword(null,"timestamp","timestamp",579478971),(new Date()).toISOString(),new cljs.core.Keyword(null,"payload","payload",-383036092),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"payloadPreview","payloadPreview",874931409),(""+"Synthetic trigger for job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job)))], null)], null),new cljs.core.Keyword(null,"job","job",850873087),job,new cljs.core.Keyword(null,"run-agent!","run-agent!",-1490275853),knoxx.backend.event_agents.start_agent_run_BANG_], null),knoxx.backend.event_agents.job_step(job));
});
knoxx.backend.event_agents.execute_cron_job_BANG_ = (function knoxx$backend$event_agents$execute_cron_job_BANG_(config,job){
var control = knoxx.backend.event_agents.control_config(config);
var source_kind = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"kind","kind",-717265803)], null));
var mode = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"mode","mode",654403691)], null));
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(source_kind,"discord")) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(mode,"patrol")))){
return knoxx.backend.event_agents.execute_discord_patrol_BANG_(config,control,job);
} else {
if(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(source_kind,"discord")) && (knoxx.backend.event_agents.synthesis_source_mode_QMARK_(mode)))){
return knoxx.backend.event_agents.execute_discord_synthesis_BANG_.cljs$core$IFn$_invoke$arity$3(config,control,job);
} else {
return knoxx.backend.event_agents.execute_direct_job_BANG_(config,job,source_kind,"cron.tick");

}
}
});
knoxx.backend.event_agents.run_job_BANG_ = (function knoxx$backend$event_agents$run_job_BANG_(job_id){
var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
var job = cljs.core.some((function (candidate){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(candidate),job_id)){
return candidate;
} else {
return null;
}
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control));
if(cljs.core.not(job)){
return Promise.reject((new Error((""+"Unknown event-agent job: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)))));
} else {
var started_at = knoxx.backend.event_agents.record_job_run_start_BANG_(job);
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cron",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null))))?knoxx.backend.event_agents.execute_cron_job_BANG_(config,job):knoxx.backend.event_agents.execute_direct_job_BANG_(config,job,cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)),"manual.run")).then((function (result){
knoxx.backend.event_agents.record_job_run_finish_BANG_(job,started_at,"ok",null);

return result;
})).catch((function (err){
knoxx.backend.event_agents.record_job_run_finish_BANG_(job,started_at,"error",err.message);

return null;
}));
}
});
knoxx.backend.event_agents.clear_interval_task_BANG_ = (function knoxx$backend$event_agents$clear_interval_task_BANG_(task){
var temp__5825__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(task);
if(cljs.core.truth_(temp__5825__auto__)){
var id = temp__5825__auto__;
return clearInterval(id);
} else {
return null;
}
});
knoxx.backend.event_agents.stop_BANG_ = (function knoxx$backend$event_agents$stop_BANG_(){
knoxx.backend.events.sources.discord.stop_BANG_();

var seq__70636_70696 = cljs.core.seq(cljs.core.deref(knoxx.backend.event_agents.scheduled_tasks_STAR_));
var chunk__70637_70697 = null;
var count__70638_70698 = (0);
var i__70639_70699 = (0);
while(true){
if((i__70639_70699 < count__70638_70698)){
var vec__70646_70700 = chunk__70637_70697.cljs$core$IIndexed$_nth$arity$2(null,i__70639_70699);
var __70701 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70646_70700,(0),null);
var task_70702 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70646_70700,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = task_70702;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_(task_70702)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"interval","interval",1708495417),new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(task_70702))));
} else {
return and__5140__auto__;
}
})())){
knoxx.backend.event_agents.clear_interval_task_BANG_(task_70702);
} else {
}


var G__70703 = seq__70636_70696;
var G__70704 = chunk__70637_70697;
var G__70705 = count__70638_70698;
var G__70706 = (i__70639_70699 + (1));
seq__70636_70696 = G__70703;
chunk__70637_70697 = G__70704;
count__70638_70698 = G__70705;
i__70639_70699 = G__70706;
continue;
} else {
var temp__5825__auto___70707 = cljs.core.seq(seq__70636_70696);
if(temp__5825__auto___70707){
var seq__70636_70708__$1 = temp__5825__auto___70707;
if(cljs.core.chunked_seq_QMARK_(seq__70636_70708__$1)){
var c__5673__auto___70709 = cljs.core.chunk_first(seq__70636_70708__$1);
var G__70710 = cljs.core.chunk_rest(seq__70636_70708__$1);
var G__70711 = c__5673__auto___70709;
var G__70712 = cljs.core.count(c__5673__auto___70709);
var G__70713 = (0);
seq__70636_70696 = G__70710;
chunk__70637_70697 = G__70711;
count__70638_70698 = G__70712;
i__70639_70699 = G__70713;
continue;
} else {
var vec__70649_70714 = cljs.core.first(seq__70636_70708__$1);
var __70715 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70649_70714,(0),null);
var task_70716 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__70649_70714,(1),null);
if(cljs.core.truth_((function (){var and__5140__auto__ = task_70716;
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.map_QMARK_(task_70716)) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"interval","interval",1708495417),new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(task_70716))));
} else {
return and__5140__auto__;
}
})())){
knoxx.backend.event_agents.clear_interval_task_BANG_(task_70716);
} else {
}


var G__70717 = cljs.core.next(seq__70636_70708__$1);
var G__70718 = null;
var G__70719 = (0);
var G__70720 = (0);
seq__70636_70696 = G__70717;
chunk__70637_70697 = G__70718;
count__70638_70698 = G__70719;
i__70639_70699 = G__70720;
continue;
}
} else {
}
}
break;
}

cljs.core.reset_BANG_(knoxx.backend.event_agents.scheduled_tasks_STAR_,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.event_agents.running_QMARK__STAR_,false);

cljs.core.reset_BANG_(knoxx.backend.event_agents.job_specs_STAR_,cljs.core.PersistentArrayMap.EMPTY);

return cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] stopped"], 0));
});
knoxx.backend.event_agents.reset_runtime_BANG_ = (function knoxx$backend$event_agents$reset_runtime_BANG_(config){
var live_config = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.runtime.state.config_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return config;
}
})();
var pending_reload = (function (){var or__5142__auto__ = cljs.core.deref(knoxx.backend.event_agents.reload_lock_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return Promise.resolve(null);
}
})();
var reset_control = knoxx.backend.event_agents.disable_cron_jobs(knoxx.backend.triggers.control_config.default_event_agent_control(live_config));
var redis_patterns = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["event-agent:job-state:*","event-agent:user-job-state:*","event-agent:job-spec:*","event-agent:discord-last-seen:*"], null);
return pending_reload.then((function (){
var temp__5825__auto___70721 = cljs.core.deref(knoxx.backend.event_agents.reload_timer_STAR_);
if(cljs.core.truth_(temp__5825__auto___70721)){
var t_70722 = temp__5825__auto___70721;
clearTimeout(t_70722);

cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_timer_STAR_,null);
} else {
}

return knoxx.backend.event_agents.stop_BANG_();
})).then((function (){
knoxx.backend.event_agents.clear_runtime_state_BANG_();

var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__70652_SHARP_){
return knoxx.backend.event_agents.redis_keys_BANG_(client,p1__70652_SHARP_);
}),redis_patterns))).then((function (results){
var matched = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.mapcat.cljs$core$IFn$_invoke$arity$variadic((function (result){
if(cljs.core.truth_(cljs.core.array_QMARK_(result))){
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(result);
} else {
return result;
}
}),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([Array.from(results)], 0))));
return knoxx.backend.event_agents.delete_redis_keys_BANG_(client,matched).then((function (deleted_count){
return knoxx.backend.event_agents.delete_redis_keys_BANG_(client,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [knoxx.backend.event_agents.event_agent_job_dirty_redis_key], null)).then((function (_){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"deletedCount","deletedCount",866783123),deleted_count], null);
}));
}));
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"deletedCount","deletedCount",866783123),(0)], null));
}
})).then((function (p__70654){
var map__70655 = p__70654;
var map__70655__$1 = cljs.core.__destructure_map(map__70655);
var deletedCount = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__70655__$1,new cljs.core.Keyword(null,"deletedCount","deletedCount",866783123));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.state.config_STAR_,(function (current){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = current;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return live_config;
}
})(),new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),reset_control);
}));

return knoxx.backend.triggers.control_config.persist_event_agent_control_BANG_(reset_control).then((function (_){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"deletedCount","deletedCount",866783123),deletedCount,new cljs.core.Keyword(null,"disabledCronJobCount","disabledCronJobCount",-629928343),cljs.core.count(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__70653_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("cron",cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(p1__70653_SHARP_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"kind","kind",-717265803)], null)));
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(reset_control)))], null);
}));
})).catch((function (err){
knoxx.backend.event_agents.clear_runtime_state_BANG_();

return Promise.reject(err);
}));
});
knoxx.backend.event_agents.status_snapshot = (function knoxx$backend$event_agents$status_snapshot(config){
var control = knoxx.backend.event_agents.control_config(config);
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"running","running",1554969103),cljs.core.deref(knoxx.backend.event_agents.running_QMARK__STAR_),new cljs.core.Keyword(null,"configured","configured",-884777889),true,new cljs.core.Keyword(null,"sources","sources",-321166424),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"discord","discord",480262077),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"lastSeenChannels","lastSeenChannels",-1976322203),cljs.core.vec(cljs.core.keys(new cljs.core.Keyword(null,"last-seen","last-seen",-526432685).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"discord","discord",480262077).cljs$core$IFn$_invoke$arity$1(cljs.core.deref(knoxx.backend.event_agents.source_state_STAR_)))))], null),new cljs.core.Keyword(null,"recentEvents","recentEvents",-866210172),cljs.core.deref(knoxx.backend.event_agents.recent_events_STAR_)], null),new cljs.core.Keyword(null,"jobs","jobs",-313607120),cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (job){
return cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227),new cljs.core.Keyword(null,"scheduleLabel","scheduleLabel",-1181676060),new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"enabled","enabled",1195909756)],[new cljs.core.Keyword(null,"contractSourceId","contractSourceId",693648227).cljs$core$IFn$_invoke$arity$1(job),knoxx.backend.events.cron.cadence_label(cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(job,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405)], null))),new cljs.core.Keyword(null,"contractSourceKind","contractSourceKind",-180837402).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"contractSourceKey","contractSourceKey",-1158171630).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.Keyword(null,"enabled","enabled",1195909756).cljs$core$IFn$_invoke$arity$1(job)]),cljs.core.get.cljs$core$IFn$_invoke$arity$3(cljs.core.deref(knoxx.backend.event_agents.job_state_STAR_),new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(job),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"runCount","runCount",-2118098906),(0),new cljs.core.Keyword(null,"lastStatus","lastStatus",1522139835),"none"], null))], 0));
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control))], null);
});
/**
 * Stop then start the event-agent runtime. Returns a promise.
 * Prevents concurrent reloads — if a reload is already in progress,
 * returns the existing promise.
 */
knoxx.backend.event_agents.reload_BANG_ = (function knoxx$backend$event_agents$reload_BANG_(){
var temp__5825__auto___70723 = cljs.core.deref(knoxx.backend.event_agents.reload_timer_STAR_);
if(cljs.core.truth_(temp__5825__auto___70723)){
var t_70724 = temp__5825__auto___70723;
clearTimeout(t_70724);

cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_timer_STAR_,null);
} else {
}

var or__5142__auto__ = cljs.core.deref(knoxx.backend.event_agents.reload_lock_STAR_);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var p = Promise.resolve(knoxx.backend.event_agents.stop_BANG_()).then((function (){
cljs.core.reset_BANG_(knoxx.backend.event_agents.job_state_STAR_,cljs.core.PersistentArrayMap.EMPTY);

cljs.core.reset_BANG_(knoxx.backend.event_agents.user_job_state_STAR_,cljs.core.PersistentArrayMap.EMPTY);

return (knoxx.backend.event_agents.start_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.event_agents.start_BANG_.cljs$core$IFn$_invoke$arity$1(null) : knoxx.backend.event_agents.start_BANG_.call(null,null));
})).finally((function (){
return cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_lock_STAR_,null);
}));
cljs.core.reset_BANG_(knoxx.backend.event_agents.reload_lock_STAR_,p);

return p;
}
});
knoxx.backend.event_agents.start_BANG_ = (function knoxx$backend$event_agents$start_BANG_(_config){
if(cljs.core.compare_and_set_BANG_(knoxx.backend.event_agents.running_QMARK__STAR_,false,true)){
var recovery_promise = (function (){var temp__5823__auto__ = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5823__auto__)){
var client = temp__5823__auto__;
return knoxx.backend.triggers.control_config.load_event_agent_control().then((function (saved_control){
if(cljs.core.truth_(saved_control)){
return cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.runtime.state.config_STAR_,(function (current_cfg){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = current_cfg;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.event_agents.cfg();
}
})(),new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),saved_control);
}));
} else {
return null;
}
}));
} else {
return Promise.resolve(null);
}
})();
return recovery_promise.then((function (_){
var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] starting with",cljs.core.count(new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control)),"jobs"], 0));

return knoxx.backend.event_agents.recover_runtime_state_BANG_(control).then((function (___$1){
knoxx.backend.event_agents.schedule_flush_task_BANG_();

knoxx.backend.event_agents.bind_discord_gateway_BANG_(config);

return knoxx.backend.event_agents.schedule_events_cron_ticker_BANG_(config);
}));
})).catch((function (err){
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] failed to recover control config from Redis:",err.message], 0));

var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] starting with",cljs.core.count(new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control)),"jobs (defaults)"], 0));

knoxx.backend.event_agents.schedule_flush_task_BANG_();

knoxx.backend.event_agents.bind_discord_gateway_BANG_(config);

return knoxx.backend.event_agents.schedule_events_cron_ticker_BANG_(config);
}));
} else {
return Promise.resolve(null);
}
});
/**
 * Public API: Create or update an event-agent job.
 * 
 * Args:
 * - job-id: String identifier for the job
 * - job-spec: Complete job specification OR template-based spec with :templateId
 * 
 * If job-spec contains :templateId, instantiates from agent-templates DSL.
 * Otherwise, treats job-spec as a complete job definition.
 * 
 * Returns a promise that resolves to the normalized job spec.
 * 
 * Example (template-based):
 * (upsert-job! "frankie-yap-bot"
 *              {:templateId :yap-bot
 *               :trigger {:kind "event" :cadenceMinutes 1 :eventKinds ["discord.message.mention"]}
 *               :filters {:channels ["123456789"] :keywords ["frankie"]}})
 * 
 * Example (direct spec):
 * (upsert-job! "custom-bot"
 *              {:id "custom-bot"
 *               :enabled true
 *               :trigger {:kind "cron" :cadenceMinutes 10}
 *               :agentSpec {:role "executive" :model "glm-5" :thinkingLevel "off"}})
 */
knoxx.backend.event_agents.upsert_job_BANG_ = (function knoxx$backend$event_agents$upsert_job_BANG_(job_id,job_spec){
var config = knoxx.backend.event_agents.cfg();
var template_id = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"templateId","templateId",613248985).cljs$core$IFn$_invoke$arity$1(job_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"template-id","template-id",1952916477).cljs$core$IFn$_invoke$arity$1(job_spec);
}
})();
var normalized_job = (cljs.core.truth_(template_id)?(function (){var trigger = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"trigger","trigger",103466139).cljs$core$IFn$_invoke$arity$1(job_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"event",new cljs.core.Keyword(null,"cadenceMinutes","cadenceMinutes",-1351259405),(5),new cljs.core.Keyword(null,"eventKinds","eventKinds",360827289),cljs.core.PersistentVector.EMPTY], null);
}
})();
var source = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"source","source",-433931539).cljs$core$IFn$_invoke$arity$1(job_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"kind","kind",-717265803),"manual",new cljs.core.Keyword(null,"mode","mode",654403691),"respond",new cljs.core.Keyword(null,"config","config",994861415),cljs.core.PersistentArrayMap.EMPTY], null);
}
})();
var filters = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"filters","filters",974726919).cljs$core$IFn$_invoke$arity$1(job_spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"channels","channels",1132759174),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"keywords","keywords",1526959054),cljs.core.PersistentVector.EMPTY], null);
}
})();
var overrides = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(job_spec,new cljs.core.Keyword(null,"templateId","templateId",613248985),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"template-id","template-id",1952916477),new cljs.core.Keyword(null,"trigger","trigger",103466139),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"filters","filters",974726919)], 0));
return knoxx.backend.agent_templates.instantiate_job.cljs$core$IFn$_invoke$arity$variadic(template_id,job_id,trigger,source,filters,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([overrides], 0));
})():cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([job_spec,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),job_id], null)], 0)));
var final_job = knoxx.backend.agent_templates.normalize_job_for_persistence(normalized_job);
var prev_spec = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.runtime.state.config_STAR_),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"event-agent-control","event-agent-control",531207392),new cljs.core.Keyword(null,"jobs","jobs",-313607120)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var temp__5825__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.event_agents.job_specs_STAR_),job_id);
if(cljs.core.truth_(temp__5825__auto__)){
var s = temp__5825__auto__;
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [s], null);
} else {
return null;
}
}
})();
var spec_sig = (function (p1__70656_SHARP_){
return cljs.core.dissoc.cljs$core$IFn$_invoke$arity$variadic(p1__70656_SHARP_,new cljs.core.Keyword(null,"updatedAt","updatedAt",1796679523),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"createdAt","createdAt",-936788)], 0));
});
var unchanged_QMARK_ = cljs.core.some((function (p1__70657_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(spec_sig(p1__70657_SHARP_),spec_sig(final_job));
}),(function (){var or__5142__auto__ = prev_spec;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
knoxx.backend.event_agents.update_job_spec_BANG_(job_id,final_job);

if(cljs.core.truth_(unchanged_QMARK_)){
} else {
knoxx.backend.event_agents.debounced_reload_BANG_();
}

return Promise.resolve(final_job);
});
/**
 * Get a job spec by ID.
 * Loads from Redis if available, otherwise returns nil.
 * Returns a promise.
 */
knoxx.backend.event_agents.get_job = (function knoxx$backend$event_agents$get_job(job_id){
var config = knoxx.backend.event_agents.cfg();
var control = knoxx.backend.event_agents.control_config(config);
var default_job = cljs.core.some((function (p1__70658_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(p1__70658_SHARP_),job_id)){
return p1__70658_SHARP_;
} else {
return null;
}
}),new cljs.core.Keyword(null,"jobs","jobs",-313607120).cljs$core$IFn$_invoke$arity$1(control));
return knoxx.backend.event_agents.get_job_spec(job_id,default_job);
});
/**
 * Delete a job from Redis and reload runtime.
 * Note: This only removes the Redis override - the job will revert to config defaults.
 * Returns a promise.
 */
knoxx.backend.event_agents.delete_job_BANG_ = (function knoxx$backend$event_agents$delete_job_BANG_(job_id){
var temp__5825__auto___70725 = knoxx.backend.redis_client.get_client();
if(cljs.core.truth_(temp__5825__auto___70725)){
var client_70726 = temp__5825__auto___70725;
var key_70727 = (""+"event-agent:job-spec:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id));
var dirty_key_70728 = "event-agent:job-dirty";
knoxx.backend.redis_client.del(client_70726,key_70727).then((function (){
knoxx.backend.redis_client.srem(client_70726,dirty_key_70728,job_id);

knoxx.backend.event_agents.reload_BANG_();

cljs.core.println.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["[event-agents] deleted job",job_id,"from Redis"], 0));

return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"deleted","deleted",-510100639),job_id], null);
}));
} else {
}

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"deleted","deleted",-510100639),job_id], null));
});
/**
 * List all available agent templates.
 * Returns vector of template keywords.
 */
knoxx.backend.event_agents.list_templates = (function knoxx$backend$event_agents$list_templates(){
return knoxx.backend.agent_templates.all_templates();
});
/**
 * List all available model profiles.
 * Returns vector of profile keywords.
 */
knoxx.backend.event_agents.list_model_profiles = (function knoxx$backend$event_agents$list_model_profiles(){
return knoxx.backend.agent_templates.all_model_profiles();
});
/**
 * Get a template definition by keyword.
 * Returns the template map or nil.
 */
knoxx.backend.event_agents.get_template = (function knoxx$backend$event_agents$get_template(template_id){
return knoxx.backend.agent_templates.get_template(template_id);
});

//# sourceMappingURL=knoxx.backend.event_agents.js.map
