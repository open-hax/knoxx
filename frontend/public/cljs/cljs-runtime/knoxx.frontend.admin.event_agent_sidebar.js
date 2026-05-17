goog.provide('knoxx.frontend.admin.event_agent_sidebar');

/**
 * Single job button in the sidebar list.
 */
knoxx.frontend.admin.event_agent_sidebar.job_button = (function (){var G__12858 = (function knoxx$frontend$admin$event_agent_sidebar$job_button_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__12867 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__12870 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__12867,(0),null);
var map__12870__$1 = cljs.core.__destructure_map(map__12870);
var job = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12870__$1,new cljs.core.Keyword(null,"job","job",850873087));
var runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12870__$1,new cljs.core.Keyword(null,"runtime","runtime",-1331573996));
var active = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12870__$1,new cljs.core.Keyword(null,"active","active",1895962068));
var on_select = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__12870__$1,new cljs.core.Keyword(null,"on-select","on-select",-192407950));

var meta = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [job.source.kind,job.trigger.kind,(cljs.core.truth_(job.contractSourceId)?"contract":"custom")], null);
var runtime_label = (cljs.core.truth_(runtime.running)?"running":(function (){var or__5142__auto__ = runtime.lastStatus;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "idle";
}
})());
var G__12887 = "button";
var G__12888 = (function (){var obj12892 = ({"type":"button","onClick":on_select,"aria-pressed":active,"className":(""+"w-full rounded-lg border px-2.5 py-2 text-left transition "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(active)?"border-sky-500/60 bg-sky-500/10 shadow-[inset_2px_0_0_0_rgba(56,189,248,0.9)]":"border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70"))),"children":[(function (){var G__12899 = "div";
var G__12900 = (function (){var obj12902 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__12904 = "div";
var G__12905 = (function (){var obj12908 = ({"className":"min-w-0 truncate text-sm font-medium text-slate-100","children":job.name});
return obj12908;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12904,G__12905) : helix.core.jsx.call(null,G__12904,G__12905));
})(),(function (){var G__12911 = "span";
var G__12912 = (function (){var obj12915 = ({"className":(""+"h-2 w-2 shrink-0 rounded-full "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(job.enabled)?"bg-emerald-400":"bg-amber-400"))),"aria-hidden":"true"});
return obj12915;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12911,G__12912) : helix.core.jsx.call(null,G__12911,G__12912));
})()]});
return obj12902;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12899,G__12900) : helix.core.jsxs.call(null,G__12899,G__12900));
})(),(function (){var G__12945 = "div";
var G__12946 = (function (){var obj12954 = ({"className":"mt-1 flex items-center justify-between gap-2 text-[11px] leading-4 text-slate-500","children":[(function (){var G__12957 = "span";
var G__12958 = (function (){var obj12962 = ({"className":"min-w-0 truncate","children":clojure.string.join.cljs$core$IFn$_invoke$arity$2(" \u00B7 ",meta)});
return obj12962;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12957,G__12958) : helix.core.jsx.call(null,G__12957,G__12958));
})(),(function (){var G__12965 = "span";
var G__12966 = (function (){var obj12968 = ({"className":"shrink-0","children":(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = runtime.runCount;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})())+"r")});
return obj12968;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12965,G__12966) : helix.core.jsx.call(null,G__12965,G__12966));
})()]});
return obj12954;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12945,G__12946) : helix.core.jsxs.call(null,G__12945,G__12946));
})(),(function (){var G__12985 = "div";
var G__12986 = (function (){var obj12992 = ({"className":"mt-1 flex items-center justify-between gap-2 text-[11px] leading-4","children":[(function (){var G__12993 = "span";
var G__12994 = (function (){var obj12996 = ({"className":"min-w-0 truncate font-mono text-slate-400","children":knoxx.frontend.admin.event_agent_utils.compact_text.cljs$core$IFn$_invoke$arity$2(job.id,(28))});
return obj12996;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12993,G__12994) : helix.core.jsx.call(null,G__12993,G__12994));
})(),(function (){var G__12998 = "span";
var G__12999 = (function (){var obj13001 = ({"className":(""+"shrink-0 uppercase tracking-wide "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(runtime.lastStatus,"ok"))?"text-emerald-300":((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(runtime.lastStatus,"error"))?"text-rose-300":(cljs.core.truth_(runtime.running)?"text-sky-300":"text-slate-500"
))))),"children":runtime_label});
return obj13001;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__12998,G__12999) : helix.core.jsx.call(null,G__12998,G__12999));
})()]});
return obj12992;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12985,G__12986) : helix.core.jsxs.call(null,G__12985,G__12986));
})()]});
return obj12892;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__12887,G__12888) : helix.core.jsxs.call(null,G__12887,G__12888));
});
if(goog.DEBUG === true){
var G__13007 = G__12858;
(G__13007.displayName = "knoxx.frontend.admin.event-agent-sidebar/job-button");

return G__13007;
} else {
return G__12858;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agent_sidebar.js.map
