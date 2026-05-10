goog.provide('knoxx.frontend.admin.event_agent_runtime');

/**
 * A single stat display card.
 */
knoxx.frontend.admin.event_agent_runtime.stat_card = (function (){var G__12856 = (function knoxx$frontend$admin$event_agent_runtime$stat_card_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__12863 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__12866 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12863,(0),null);
var map__12866__$1 = cljs.core.__destructure_map(map__12866);
var label = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12866__$1,new cljs.core.Keyword(null,"label","label",1718410804));
var value = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12866__$1,new cljs.core.Keyword(null,"value","value",305978217));

var G__12871 = "div";
var G__12872 = (function (){var obj12877 = ({"children":[(function (){var G__12879 = "div";
var G__12880 = (function (){var obj12886 = ({"className":"text-[10px] uppercase tracking-wide text-slate-500","children":label});
return obj12886;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12879,G__12880) : helix.core.jsx.call(null,G__12879,G__12880));
})(),(function (){var G__12889 = "div";
var G__12890 = (function (){var obj12894 = ({"className":"mt-0.5 text-xs text-slate-200","children":value});
return obj12894;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12889,G__12890) : helix.core.jsx.call(null,G__12889,G__12890));
})()]});
return obj12877;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12871,G__12872) : helix.core.jsxs.call(null,G__12871,G__12872));
});
if(goog.DEBUG === true){
var G__12903 = G__12856;
(G__12903.displayName = "knoxx.frontend.admin.event-agent-runtime/stat-card");

return G__12903;
} else {
return G__12856;
}
})();




/**
 * Runtime snapshot card showing status, runs, last finished, next run.
 */
knoxx.frontend.admin.event_agent_runtime.runtime_snapshot = (function (){var G__12916 = (function knoxx$frontend$admin$event_agent_runtime$runtime_snapshot_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__12921 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__12924 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12921,(0),null);
var map__12924__$1 = cljs.core.__destructure_map(map__12924);
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12924__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));

var G__12925 = "div";
var G__12926 = (function (){var obj12930 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/60 p-2.5","children":[(function (){var G__12933 = "div";
var G__12934 = (function (){var obj12938 = ({"className":"text-xs font-semibold text-slate-100","children":"Runtime snapshot"});
return obj12938;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12933,G__12934) : helix.core.jsx.call(null,G__12933,G__12934));
})(),(function (){var G__12941 = "div";
var G__12942 = (function (){var obj12948 = ({"className":"mt-2 grid gap-x-3 gap-y-1.5 sm:grid-cols-2 text-xs","children":[(function (){var G__12951 = knoxx.frontend.admin.event_agent_runtime.stat_card;
var G__12952 = (function (){var obj12956 = ({"label":"Status","value":(function (){var G__12959 = knoxx.frontend.admin.event_agent_components.status_badge;
var G__12960 = (function (){var obj12964 = ({"status":runtime.lastStatus,"enabled":true,"running":runtime.running});
return obj12964;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12959,G__12960) : helix.core.jsx.call(null,G__12959,G__12960));
})()});
return obj12956;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12951,G__12952) : helix.core.jsx.call(null,G__12951,G__12952));
})(),(function (){var G__12969 = knoxx.frontend.admin.event_agent_runtime.stat_card;
var G__12970 = (function (){var obj12972 = ({"label":"Runs","value":(function (){var G__12975 = "div";
var G__12976 = (function (){var obj12978 = ({"className":"text-base font-semibold text-slate-100","children":(function (){var or__5142__auto__ = runtime.runCount;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})()});
return obj12978;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12975,G__12976) : helix.core.jsx.call(null,G__12975,G__12976));
})()});
return obj12972;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12969,G__12970) : helix.core.jsx.call(null,G__12969,G__12970));
})(),(function (){var G__12981 = knoxx.frontend.admin.event_agent_runtime.stat_card;
var G__12982 = (function (){var obj12984 = ({"label":"Last finished","value":knoxx.frontend.admin.event_agent_utils.to_local_date_time(runtime.lastFinishedAt)});
return obj12984;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12981,G__12982) : helix.core.jsx.call(null,G__12981,G__12982));
})(),(function (){var G__12987 = knoxx.frontend.admin.event_agent_runtime.stat_card;
var G__12988 = (function (){var obj12990 = ({"label":"Next run","value":knoxx.frontend.admin.event_agent_utils.to_local_date_time(runtime.nextRunAt)});
return obj12990;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12987,G__12988) : helix.core.jsx.call(null,G__12987,G__12988));
})()]});
return obj12948;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12941,G__12942) : helix.core.jsxs.call(null,G__12941,G__12942));
})()]});
return obj12930;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12925,G__12926) : helix.core.jsxs.call(null,G__12925,G__12926));
});
if(goog.DEBUG === true){
var G__12997 = G__12916;
(G__12997.displayName = "knoxx.frontend.admin.event-agent-runtime/runtime-snapshot");

return G__12997;
} else {
return G__12916;
}
})();




/**
 * Live runtime details card.
 */
knoxx.frontend.admin.event_agent_runtime.live_runtime = (function (){var G__13021 = (function knoxx$frontend$admin$event_agent_runtime$live_runtime_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13026 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13029 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13026,(0),null);
var map__13029__$1 = cljs.core.__destructure_map(map__13029);
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13029__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));

var G__13040 = "div";
var G__13041 = (function (){var obj13045 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/60 p-2.5","children":[(function (){var G__13046 = "div";
var G__13047 = (function (){var obj13050 = ({"className":"text-xs font-semibold text-slate-100","children":"Live runtime"});
return obj13050;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13046,G__13047) : helix.core.jsx.call(null,G__13046,G__13047));
})(),(function (){var G__13053 = "div";
var G__13054 = (function (){var obj13064 = ({"className":"mt-2 grid gap-x-3 gap-y-1 text-xs text-slate-300","children":[(function (){var G__13067 = "div";
var G__13068 = (function (){var obj13070 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__13071 = "span";
var G__13072 = (function (){var obj13074 = ({"className":"text-slate-500","children":"Schedule"});
return obj13074;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13071,G__13072) : helix.core.jsx.call(null,G__13071,G__13072));
})(),(function (){var G__13075 = "span";
var G__13076 = (function (){var obj13078 = ({"className":"text-right text-slate-200","children":(function (){var or__5142__auto__ = runtime.scheduleLabel;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "\u2014";
}
})()});
return obj13078;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13075,G__13076) : helix.core.jsx.call(null,G__13075,G__13076));
})()]});
return obj13070;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13067,G__13068) : helix.core.jsxs.call(null,G__13067,G__13068));
})(),(function (){var G__13084 = "div";
var G__13085 = (function (){var obj13087 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__13088 = "span";
var G__13089 = (function (){var obj13091 = ({"className":"text-slate-500","children":"Last started"});
return obj13091;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13088,G__13089) : helix.core.jsx.call(null,G__13088,G__13089));
})(),(function (){var G__13094 = "span";
var G__13095 = (function (){var obj13103 = ({"className":"text-right text-slate-200","children":knoxx.frontend.admin.event_agent_utils.to_local_date_time(runtime.lastStartedAt)});
return obj13103;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13094,G__13095) : helix.core.jsx.call(null,G__13094,G__13095));
})()]});
return obj13087;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13084,G__13085) : helix.core.jsxs.call(null,G__13084,G__13085));
})(),(function (){var G__13112 = "div";
var G__13113 = (function (){var obj13117 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__13120 = "span";
var G__13121 = (function (){var obj13123 = ({"className":"text-slate-500","children":"Last finished"});
return obj13123;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13120,G__13121) : helix.core.jsx.call(null,G__13120,G__13121));
})(),(function (){var G__13126 = "span";
var G__13127 = (function (){var obj13129 = ({"className":"text-right text-slate-200","children":knoxx.frontend.admin.event_agent_utils.to_local_date_time(runtime.lastFinishedAt)});
return obj13129;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13126,G__13127) : helix.core.jsx.call(null,G__13126,G__13127));
})()]});
return obj13117;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13112,G__13113) : helix.core.jsxs.call(null,G__13112,G__13113));
})(),(function (){var G__13135 = "div";
var G__13136 = (function (){var obj13139 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__13142 = "span";
var G__13143 = (function (){var obj13147 = ({"className":"text-slate-500","children":"Duration"});
return obj13147;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13142,G__13143) : helix.core.jsx.call(null,G__13142,G__13143));
})(),(function (){var G__13150 = "span";
var G__13151 = (function (){var obj13155 = ({"className":"text-right text-slate-200","children":(cljs.core.truth_(runtime.lastDurationMs)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(runtime.lastDurationMs)+" ms"):"\u2014")});
return obj13155;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13150,G__13151) : helix.core.jsx.call(null,G__13150,G__13151));
})()]});
return obj13139;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13135,G__13136) : helix.core.jsxs.call(null,G__13135,G__13136));
})()]});
return obj13064;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13053,G__13054) : helix.core.jsxs.call(null,G__13053,G__13054));
})(),(cljs.core.truth_(runtime.lastError)?(function (){var G__13166 = "div";
var G__13167 = (function (){var obj13169 = ({"className":"mt-2 rounded border border-rose-500/30 bg-rose-500/10 px-2 py-1 text-[11px] text-rose-200","children":runtime.lastError});
return obj13169;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13166,G__13167) : helix.core.jsx.call(null,G__13166,G__13167));
})():null)]});
return obj13045;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13040,G__13041) : helix.core.jsxs.call(null,G__13040,G__13041));
});
if(goog.DEBUG === true){
var G__13172 = G__13021;
(G__13172.displayName = "knoxx.frontend.admin.event-agent-runtime/live-runtime");

return G__13172;
} else {
return G__13021;
}
})();




/**
 * Quick reference card with job metadata.
 */
knoxx.frontend.admin.event_agent_runtime.quick_reference = (function (){var G__13176 = (function knoxx$frontend$admin$event_agent_runtime$quick_reference_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13179 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13182 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13179,(0),null);
var map__13182__$1 = cljs.core.__destructure_map(map__13182);
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13182__$1,new cljs.core.Keyword(null,"job","job",850873087));

var G__13187 = "div";
var G__13188 = (function (){var obj13192 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/60 p-2.5","children":[(function (){var G__13193 = "div";
var G__13194 = (function (){var obj13196 = ({"className":"text-xs font-semibold text-slate-100","children":"Quick reference"});
return obj13196;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13193,G__13194) : helix.core.jsx.call(null,G__13193,G__13194));
})(),(function (){var G__13198 = "div";
var G__13199 = (function (){var obj13204 = ({"className":"mt-2 space-y-1 text-[11px] text-slate-400","children":[(function (){var G__13207 = "div";
var G__13208 = (function (){var obj13210 = ({"children":[(function (){var G__13211 = "span";
var G__13212 = (function (){var obj13216 = ({"className":"text-slate-500","children":"Job id: "});
return obj13216;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13211,G__13212) : helix.core.jsx.call(null,G__13211,G__13212));
})(),(function (){var G__13221 = "code";
var G__13222 = (function (){var obj13224 = ({"className":"font-mono text-slate-200","children":job.id});
return obj13224;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13221,G__13222) : helix.core.jsx.call(null,G__13221,G__13222));
})()]});
return obj13210;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13207,G__13208) : helix.core.jsxs.call(null,G__13207,G__13208));
})(),(function (){var G__13225 = "div";
var G__13226 = (function (){var obj13230 = ({"children":[(function (){var G__13233 = "span";
var G__13234 = (function (){var obj13236 = ({"className":"text-slate-500","children":"Source mode: "});
return obj13236;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13233,G__13234) : helix.core.jsx.call(null,G__13233,G__13234));
})(),job.source.mode]});
return obj13230;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13225,G__13226) : helix.core.jsxs.call(null,G__13225,G__13226));
})(),(function (){var G__13239 = "div";
var G__13240 = (function (){var obj13242 = ({"children":[(function (){var G__13245 = "span";
var G__13246 = (function (){var obj13248 = ({"className":"text-slate-500","children":"Trigger cadence: "});
return obj13248;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13245,G__13246) : helix.core.jsx.call(null,G__13245,G__13246));
})(),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.trigger.cadenceMinutes)+" min")]});
return obj13242;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13239,G__13240) : helix.core.jsxs.call(null,G__13239,G__13240));
})(),(function (){var G__13259 = "div";
var G__13260 = (function (){var obj13262 = ({"children":[(function (){var G__13263 = "span";
var G__13264 = (function (){var obj13268 = ({"className":"text-slate-500","children":"Event kinds: "});
return obj13268;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13263,G__13264) : helix.core.jsx.call(null,G__13263,G__13264));
})(),((cljs.core.seq(job.trigger.eventKinds))?clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",job.trigger.eventKinds):"none")]});
return obj13262;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13259,G__13260) : helix.core.jsxs.call(null,G__13259,G__13260));
})()]});
return obj13204;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13198,G__13199) : helix.core.jsxs.call(null,G__13198,G__13199));
})()]});
return obj13192;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13187,G__13188) : helix.core.jsxs.call(null,G__13187,G__13188));
});
if(goog.DEBUG === true){
var G__13270 = G__13176;
(G__13270.displayName = "knoxx.frontend.admin.event-agent-runtime/quick-reference");

return G__13270;
} else {
return G__13176;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agent_runtime.js.map
