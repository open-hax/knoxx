goog.provide('knoxx.frontend.admin.event_agent_utils');
/**
 * Split a comma-separated string into trimmed, non-empty tokens.
 */
knoxx.frontend.admin.event_agent_utils.split_csv = (function knoxx$frontend$admin$event_agent_utils$split_csv(value){
return cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.seq,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)),/,/)));
});
/**
 * Join a collection into a comma-separated string.
 */
knoxx.frontend.admin.event_agent_utils.join_csv = (function knoxx$frontend$admin$event_agent_utils$join_csv(values){
return clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",(function (){var or__5142__auto__ = values;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
});
/**
 * Pretty-print a value as JSON.
 */
knoxx.frontend.admin.event_agent_utils.pretty_json = (function knoxx$frontend$admin$event_agent_utils$pretty_json(value){
return JSON.stringify(cljs.core.clj__GT_js((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),null,(2));
});
/**
 * Format a timestamp as a locale string, or '—' if invalid.
 */
knoxx.frontend.admin.event_agent_utils.to_local_date_time = (function knoxx$frontend$admin$event_agent_utils$to_local_date_time(value){
if(cljs.core.truth_((function (){var and__5140__auto__ = value;
if(cljs.core.truth_(and__5140__auto__)){
return Number.isFinite(value);
} else {
return and__5140__auto__;
}
})())){
try{return (new Date(value)).toLocaleString();
}catch (e12066){if((e12066 instanceof Error)){
var _ = e12066;
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
} else {
throw e12066;

}
}} else {
return "\u2014";
}
});
/**
 * Find the runtime job matching a given job id.
 */
knoxx.frontend.admin.event_agent_utils.runtime_for_job = (function knoxx$frontend$admin$event_agent_utils$runtime_for_job(runtime_jobs,job_id){
return cljs.core.some((function (p1__12088_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__12088_SHARP_.id,job_id)){
return p1__12088_SHARP_;
} else {
return null;
}
}),runtime_jobs);
});
/**
 * Build a map of job-id -> JSON draft strings from loaded jobs.
 */
knoxx.frontend.admin.event_agent_utils.seed_json_drafts = (function knoxx$frontend$admin$event_agent_utils$seed_json_drafts(jobs){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1((function (job){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [job.id,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"source-config","source-config",305510816),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto__ = job.source.config;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword(null,"filters","filters",974726919),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto__ = job.filters;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto__ = job.agentSpec.toolPolicies;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null)], null);
})),jobs);
});
/**
 * Compact text to a max length, appending ellipsis if truncated.
 */
knoxx.frontend.admin.event_agent_utils.compact_text = (function knoxx$frontend$admin$event_agent_utils$compact_text(var_args){
var G__12131 = arguments.length;
switch (G__12131) {
case 1:
return knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$1 = (function (value){
return knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$2(value,(120));
}));

(knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$2 = (function (value,max){
var normalized = clojure.string.trim(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = value;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/\s+/," "));
if(cljs.core.empty_QMARK_(normalized)){
return "No description";
} else {
if((((normalized).length) <= max)){
return normalized;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.subs.cljs$core$IFn$_invoke$arity$3(normalized,(0),(max - (1))))+"\u2026");

}
}
}));

(knoxx.frontend.admin.event_agent_utils.compact_text.cljs$lang$maxFixedArity = 2);

/**
 * Lowercase and trim a search string.
 */
knoxx.frontend.admin.event_agent_utils.normalize_search = (function knoxx$frontend$admin$event_agent_utils$normalize_search(value){
return clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value))));
});
/**
 * Build a searchable text blob from a job definition.
 */
knoxx.frontend.admin.event_agent_utils.job_search_text = (function knoxx$frontend$admin$event_agent_utils$job_search_text(job){
return clojure.string.lower_case(clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",cljs.core.filter.cljs$core$IFn$_invoke$arity$2(cljs.core.identity,new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, [job.id,job.name,job.description,job.source.kind,job.source.mode,job.trigger.kind,clojure.string.join.cljs$core$IFn$_invoke$arity$2(" ",job.trigger.eventKinds),job.agentSpec.role,job.agentSpec.model,job.contractSourceId], null))));
});
/**
 * Map a runtime status string to a UI tone keyword.
 */
knoxx.frontend.admin.event_agent_utils.runtime_status_tone = (function knoxx$frontend$admin$event_agent_utils$runtime_status_tone(status){
var G__12157 = status;
switch (G__12157) {
case "ok":
return new cljs.core.Keyword(null,"success","success",1890645906);

break;
case "error":
return new cljs.core.Keyword(null,"danger","danger",-624338030);

break;
case "running":
return new cljs.core.Keyword(null,"info","info",-317069002);

break;
default:
return new cljs.core.Keyword(null,"default","default",-1987822328);

}
});

//# sourceMappingURL=knoxx.frontend.admin.event_agent_utils.js.map
