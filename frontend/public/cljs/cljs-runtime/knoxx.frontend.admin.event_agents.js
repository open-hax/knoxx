goog.provide('knoxx.frontend.admin.event_agents');

/**
 * Shown while control plane data is loading.
 */
knoxx.frontend.admin.event_agents.loading_state = (function (){var G__13272 = (function knoxx$frontend$admin$event_agents$loading_state_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13273 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);

var G__13276 = "div";
var G__13277 = (function (){var obj13279 = ({"className":"text-sm text-slate-300","children":"Loading event-agent control plane\u2026"});
return obj13279;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13276,G__13277) : helix.core.jsx.call(null,G__13276,G__13277));
});
if(goog.DEBUG === true){
var G__13281 = G__13272;
(G__13281.displayName = "knoxx.frontend.admin.event-agents/loading-state");

return G__13281;
} else {
return G__13272;
}
})();




/**
 * Shown when no job is selected.
 */
knoxx.frontend.admin.event_agents.empty_state = (function (){var G__13283 = (function knoxx$frontend$admin$event_agents$empty_state_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13284 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);

var G__13287 = "div";
var G__13288 = (function (){var obj13290 = ({"className":"rounded-xl border border-dashed border-slate-800 bg-slate-950/30 px-4 py-8 text-center text-xs text-slate-400","children":"Select an event agent from the sidebar to inspect it."});
return obj13290;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13287,G__13288) : helix.core.jsx.call(null,G__13287,G__13288));
});
if(goog.DEBUG === true){
var G__13291 = G__13283;
(G__13291.displayName = "knoxx.frontend.admin.event-agents/empty-state");

return G__13291;
} else {
return G__13283;
}
})();




/**
 * Header row for the selected job.
 */
knoxx.frontend.admin.event_agents.job_header = (function (){var G__13293 = (function knoxx$frontend$admin$event_agents$job_header_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13294 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13297 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13294,(0),null);
var map__13297__$1 = cljs.core.__destructure_map(map__13297);
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13297__$1,new cljs.core.Keyword(null,"job","job",850873087));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13297__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var on_run = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13297__$1,new cljs.core.Keyword(null,"on-run","on-run",184106332));
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13297__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var running_job_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13297__$1,new cljs.core.Keyword(null,"running-job-id","running-job-id",1690483347));

var G__13298 = "div";
var G__13299 = (function (){var obj13301 = ({"className":"flex flex-col gap-2 border-b border-slate-800 pb-3 md:flex-row md:items-start md:justify-between","children":[(function (){var G__13302 = "div";
var G__13303 = (function (){var obj13305 = ({"className":"min-w-0","children":[(function (){var G__13306 = "div";
var G__13307 = (function (){var obj13309 = ({"className":"flex flex-wrap items-center gap-1.5","children":[(function (){var G__13310 = "h3";
var G__13311 = (function (){var obj13313 = ({"className":"text-base font-semibold text-slate-100","children":job.name});
return obj13313;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13310,G__13311) : helix.core.jsx.call(null,G__13310,G__13311));
})(),(function (){var G__13314 = knoxx.frontend.admin.event_agent_components.badge;
var G__13315 = (function (){var obj13317 = ({"tone":(cljs.core.truth_(job.enabled)?new cljs.core.Keyword(null,"success","success",1890645906):new cljs.core.Keyword(null,"warn","warn",-436710552)),"children":(cljs.core.truth_(job.enabled)?"Enabled":"Disabled")});
return obj13317;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13314,G__13315) : helix.core.jsx.call(null,G__13314,G__13315));
})(),(function (){var G__13318 = "span";
var G__13319 = (function (){var obj13321 = ({"className":"text-[11px] text-slate-500","children":(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.source.kind)+" \u00B7 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.trigger.kind)+" \u00B7 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(job.contractSourceId)?"contract":"custom")))});
return obj13321;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13318,G__13319) : helix.core.jsx.call(null,G__13318,G__13319));
})(),(cljs.core.truth_(runtime.running)?(function (){var G__13322 = knoxx.frontend.admin.event_agent_components.badge;
var G__13323 = (function (){var obj13325 = ({"tone":new cljs.core.Keyword(null,"info","info",-317069002),"children":"Running now"});
return obj13325;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13322,G__13323) : helix.core.jsx.call(null,G__13322,G__13323));
})():null)]});
return obj13309;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13306,G__13307) : helix.core.jsxs.call(null,G__13306,G__13307));
})(),(function (){var G__13326 = "p";
var G__13327 = (function (){var obj13329 = ({"className":"mt-1 text-xs text-slate-400","children":(function (){var or__5142__auto__ = job.description;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "No description provided.";
}
})()});
return obj13329;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13326,G__13327) : helix.core.jsx.call(null,G__13326,G__13327));
})(),(cljs.core.truth_(job.contractSourceId)?(function (){var G__13330 = "div";
var G__13331 = (function (){var obj13333 = ({"className":"mt-1 text-[11px] text-slate-500","children":["Contract-backed from ",(function (){var G__13334 = "code";
var G__13335 = (function (){var obj13337 = ({"className":"font-mono text-slate-300","children":(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = job.contractSourceKind;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "agent";
}
})())+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.contractSourceId))});
return obj13337;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13334,G__13335) : helix.core.jsx.call(null,G__13334,G__13335));
})()]});
return obj13333;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13330,G__13331) : helix.core.jsxs.call(null,G__13330,G__13331));
})():null)]});
return obj13305;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13302,G__13303) : helix.core.jsxs.call(null,G__13302,G__13303));
})(),(function (){var G__13338 = "button";
var G__13339 = (function (){var obj13341 = ({"type":"button","onClick":on_run,"disabled":((cljs.core.not(can_manage)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(running_job_id,job.id))),"className":(""+"inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 "+"px-2.5 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"),"children":((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(running_job_id,job.id))?"Queueing\u2026":"Run now")});
return obj13341;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13338,G__13339) : helix.core.jsx.call(null,G__13338,G__13339));
})()]});
return obj13301;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13298,G__13299) : helix.core.jsxs.call(null,G__13298,G__13299));
});
if(goog.DEBUG === true){
var G__13342 = G__13293;
(G__13342.displayName = "knoxx.frontend.admin.event-agents/job-header");

return G__13342;
} else {
return G__13293;
}
})();




/**
 * Right panel showing the selected job details and editor.
 */
knoxx.frontend.admin.event_agents.selected_job_panel = (function (){var G__13344 = (function knoxx$frontend$admin$event_agents$selected_job_panel_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13345 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13348 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13345,(0),null);
var map__13348__$1 = cljs.core.__destructure_map(map__13348);
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"job","job",850873087));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var on_update = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"on-update","on-update",1680216496));
var on_run = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"on-run","on-run",184106332));
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var saving_control = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"saving-control","saving-control",1969586949));
var running_job_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13348__$1,new cljs.core.Keyword(null,"running-job-id","running-job-id",1690483347));

var G__13349 = "div";
var G__13350 = (function (){var obj13352 = ({"className":"rounded-xl border border-slate-800 bg-slate-950/40 p-3","children":[(function (){var G__13353 = knoxx.frontend.admin.event_agents.job_header;
var G__13354 = (function (){var obj13356 = ({"job":job,"runtime":runtime,"on-run":on_run,"can-manage":can_manage,"running-job-id":running_job_id});
return obj13356;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13353,G__13354) : helix.core.jsx.call(null,G__13353,G__13354));
})(),(function (){var G__13357 = "div";
var G__13358 = (function (){var obj13360 = ({"className":"mt-3 space-y-3","children":[(function (){var G__13361 = "div";
var G__13362 = (function (){var obj13364 = ({"className":"grid gap-2 xl:grid-cols-3","children":[(function (){var G__13365 = knoxx.frontend.admin.event_agent_runtime.runtime_snapshot;
var G__13366 = (function (){var obj13368 = ({"runtime":runtime});
return obj13368;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13365,G__13366) : helix.core.jsx.call(null,G__13365,G__13366));
})(),(function (){var G__13369 = knoxx.frontend.admin.event_agent_runtime.live_runtime;
var G__13370 = (function (){var obj13372 = ({"runtime":runtime});
return obj13372;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13369,G__13370) : helix.core.jsx.call(null,G__13369,G__13370));
})(),(function (){var G__13373 = knoxx.frontend.admin.event_agent_runtime.quick_reference;
var G__13374 = (function (){var obj13376 = ({"job":job});
return obj13376;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13373,G__13374) : helix.core.jsx.call(null,G__13373,G__13374));
})()]});
return obj13364;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13361,G__13362) : helix.core.jsxs.call(null,G__13361,G__13362));
})(),(function (){var G__13377 = knoxx.frontend.admin.event_agent_editor.job_form;
var G__13378 = (function (){var obj13380 = ({"job":job,"on-update":on_update,"can-manage":can_manage,"saving-control":saving_control});
return obj13380;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13377,G__13378) : helix.core.jsx.call(null,G__13377,G__13378));
})()]});
return obj13360;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13357,G__13358) : helix.core.jsxs.call(null,G__13357,G__13358));
})()]});
return obj13352;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13349,G__13350) : helix.core.jsxs.call(null,G__13349,G__13350));
});
if(goog.DEBUG === true){
var G__13381 = G__13344;
(G__13381.displayName = "knoxx.frontend.admin.event-agents/selected-job-panel");

return G__13381;
} else {
return G__13344;
}
})();




/**
 * The two-column main area: schedule review (left) + job editor (right).
 */
knoxx.frontend.admin.event_agents.main_content = (function (){var G__13383 = (function knoxx$frontend$admin$event_agents$main_content_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13384 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13387 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13384,(0),null);
var map__13387__$1 = cljs.core.__destructure_map(map__13387);
var on_select_job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"on-select-job","on-select-job",-761540646));
var on_run = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"on-run","on-run",184106332));
var selected_job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"selected-job","selected-job",-1487510531));
var runtime_jobs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"runtime-jobs","runtime-jobs",2045804578));
var selected_job_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"selected-job-id","selected-job-id",284545636));
var saving_control = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"saving-control","saving-control",1969586949));
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var jobs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"jobs","jobs",-313607120));
var on_update = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"on-update","on-update",1680216496));
var running_job_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"running-job-id","running-job-id",1690483347));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13387__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));

var G__13388 = "div";
var G__13389 = (function (){var obj13391 = ({"className":"grid gap-3 min-w-0 h-full min-h-0 grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]","children":[(function (){var G__13392 = "div";
var G__13393 = (function (){var obj13395 = ({"className":"flex flex-col h-full overflow-hidden rounded-xl border border-slate-800 bg-slate-950/40","children":[(function (){var G__13396 = "div";
var G__13397 = (function (){var obj13399 = ({"className":"shrink-0 border-b border-slate-800 px-3 py-2","children":[(function (){var G__13400 = "div";
var G__13401 = (function (){var obj13403 = ({"className":"text-sm font-semibold text-slate-100","children":"Schedule review"});
return obj13403;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13400,G__13401) : helix.core.jsx.call(null,G__13400,G__13401));
})(),(function (){var G__13404 = "div";
var G__13405 = (function (){var obj13407 = ({"className":"text-[11px] text-slate-500","children":"Review cadence and next-run timing."});
return obj13407;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13404,G__13405) : helix.core.jsx.call(null,G__13404,G__13405));
})()]});
return obj13399;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13396,G__13397) : helix.core.jsxs.call(null,G__13396,G__13397));
})(),(function (){var G__13408 = "div";
var G__13409 = (function (){var obj13411 = ({"className":"flex-1 min-h-0 overflow-y-auto p-2","children":"TODO: EventAgentScheduleReview"});
return obj13411;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13408,G__13409) : helix.core.jsx.call(null,G__13408,G__13409));
})()]});
return obj13395;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13392,G__13393) : helix.core.jsxs.call(null,G__13392,G__13393));
})(),(function (){var G__13412 = "div";
var G__13413 = (function (){var obj13415 = ({"className":"h-full overflow-y-auto min-w-0 space-y-3 pr-1","children":(cljs.core.truth_(selected_job)?(function (){var G__13416 = knoxx.frontend.admin.event_agents.selected_job_panel;
var G__13417 = (function (){var obj13419 = ({"job":selected_job,"runtime":runtime,"on-update":on_update,"on-run":on_run,"can-manage":can_manage,"saving-control":saving_control,"running-job-id":running_job_id});
return obj13419;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13416,G__13417) : helix.core.jsx.call(null,G__13416,G__13417));
})():(function (){var G__13420 = knoxx.frontend.admin.event_agents.empty_state;
var G__13421 = ({});
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13420,G__13421) : helix.core.jsx.call(null,G__13420,G__13421));
})())});
return obj13415;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13412,G__13413) : helix.core.jsx.call(null,G__13412,G__13413));
})()]});
return obj13391;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13388,G__13389) : helix.core.jsxs.call(null,G__13388,G__13389));
});
if(goog.DEBUG === true){
var G__13422 = G__13383;
(G__13422.displayName = "knoxx.frontend.admin.event-agents/main-content");

return G__13422;
} else {
return G__13383;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agents.js.map
