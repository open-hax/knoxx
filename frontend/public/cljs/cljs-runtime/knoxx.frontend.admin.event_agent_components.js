goog.provide('knoxx.frontend.admin.event_agent_components');

/**
 * A details/summary panel that can be toggled open/closed.
 */
knoxx.frontend.admin.event_agent_components.collapsible_panel = (function (){var G__34381 = (function knoxx$frontend$admin$event_agent_components$collapsible_panel_render(props__11972__auto__,maybe_ref__11973__auto__){
var vec__34382 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__11972__auto__),maybe_ref__11973__auto__], null);
var map__34385 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__34382,(0),null);
var map__34385__$1 = cljs.core.__destructure_map(map__34385);
var title = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34385__$1,new cljs.core.Keyword(null,"title","title",636505583));
var description = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34385__$1,new cljs.core.Keyword(null,"description","description",-1428560544));
var default_open = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34385__$1,new cljs.core.Keyword(null,"default-open","default-open",936459665));
var children = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34385__$1,new cljs.core.Keyword(null,"children","children",-940561982));

var G__34395 = "details";
var G__34396 = (function (){var obj34403 = ({"open":default_open,"className":"rounded-xl border border-slate-800 bg-slate-950/40 p-4 open:bg-slate-950/55","children":[(function (){var G__34406 = "summary";
var G__34407 = (function (){var obj34411 = ({"className":"cursor-pointer list-none","children":(function (){var G__34414 = "div";
var G__34415 = (function (){var obj34419 = ({"className":"flex items-start justify-between gap-4","children":[(function (){var G__34420 = "div";
var G__34421 = (function (){var obj34425 = ({"children":[(function (){var G__34428 = "div";
var G__34429 = (function (){var obj34431 = ({"className":"text-sm font-semibold text-slate-100","children":title});
return obj34431;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34428,G__34429) : helix.core.jsx.call(null,G__34428,G__34429));
})(),(cljs.core.truth_(description)?(function (){var G__34432 = "div";
var G__34433 = (function (){var obj34437 = ({"className":"mt-1 text-xs text-slate-500","children":description});
return obj34437;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34432,G__34433) : helix.core.jsx.call(null,G__34432,G__34433));
})():null)]});
return obj34425;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__34420,G__34421) : helix.core.jsxs.call(null,G__34420,G__34421));
})(),(function (){var G__34444 = "span";
var G__34445 = (function (){var obj34447 = ({"className":"text-xs uppercase tracking-wide text-slate-500","children":"toggle"});
return obj34447;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34444,G__34445) : helix.core.jsx.call(null,G__34444,G__34445));
})()]});
return obj34419;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__34414,G__34415) : helix.core.jsxs.call(null,G__34414,G__34415));
})()});
return obj34411;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34406,G__34407) : helix.core.jsx.call(null,G__34406,G__34407));
})(),(function (){var G__34453 = "div";
var G__34454 = (function (){var obj34456 = ({"className":"mt-4","children":children});
return obj34456;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34453,G__34454) : helix.core.jsx.call(null,G__34453,G__34454));
})()]});
return obj34403;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__34395,G__34396) : helix.core.jsxs.call(null,G__34395,G__34396));
});
if(goog.DEBUG === true){
var G__34459 = G__34381;
(G__34459.displayName = "knoxx.frontend.admin.event-agent-components/collapsible-panel");

return G__34459;
} else {
return G__34381;
}
})();




/**
 * Simple status badge.
 */
knoxx.frontend.admin.event_agent_components.badge = (function (){var G__34471 = (function knoxx$frontend$admin$event_agent_components$badge_render(props__11972__auto__,maybe_ref__11973__auto__){
var vec__34473 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__11972__auto__),maybe_ref__11973__auto__], null);
var map__34476 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__34473,(0),null);
var map__34476__$1 = cljs.core.__destructure_map(map__34476);
var tone = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34476__$1,new cljs.core.Keyword(null,"tone","tone",-1422788785));
var children = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34476__$1,new cljs.core.Keyword(null,"children","children",-940561982));

var tone_class = (function (){var G__34478 = tone;
var G__34478__$1 = (((G__34478 instanceof cljs.core.Keyword))?G__34478.fqn:null);
switch (G__34478__$1) {
case "success":
return "border-emerald-500/30 bg-emerald-500/10 text-emerald-200";

break;
case "warn":
return "border-amber-500/30 bg-amber-500/10 text-amber-200";

break;
case "danger":
return "border-rose-500/30 bg-rose-500/10 text-rose-200";

break;
case "info":
return "border-cyan-500/30 bg-cyan-500/10 text-cyan-200";

break;
default:
return "border-slate-700 bg-slate-800 text-slate-200";

}
})();
var G__34484 = "span";
var G__34485 = (function (){var obj34487 = ({"className":(""+"inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tone_class)),"children":children});
return obj34487;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34484,G__34485) : helix.core.jsx.call(null,G__34484,G__34485));
});
if(goog.DEBUG === true){
var G__34488 = G__34471;
(G__34488.displayName = "knoxx.frontend.admin.event-agent-components/badge");

return G__34488;
} else {
return G__34471;
}
})();




/**
 * Badge that shows a runtime status.
 */
knoxx.frontend.admin.event_agent_components.status_badge = (function (){var G__34492 = (function knoxx$frontend$admin$event_agent_components$status_badge_render(props__11972__auto__,maybe_ref__11973__auto__){
var vec__34493 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__11972__auto__),maybe_ref__11973__auto__], null);
var map__34496 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__34493,(0),null);
var map__34496__$1 = cljs.core.__destructure_map(map__34496);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34496__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var enabled = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34496__$1,new cljs.core.Keyword(null,"enabled","enabled",1195909756));
var running = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__34496__$1,new cljs.core.Keyword(null,"running","running",1554969103));

var tone = ((cljs.core.not(enabled))?new cljs.core.Keyword(null,"warn","warn",-436710552):(cljs.core.truth_(running)?new cljs.core.Keyword(null,"info","info",-317069002):((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(status,"ok"))?new cljs.core.Keyword(null,"success","success",1890645906):((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(status,"error"))?new cljs.core.Keyword(null,"danger","danger",-624338030):new cljs.core.Keyword(null,"default","default",-1987822328)
))));
var label = ((cljs.core.not(enabled))?"disabled":(cljs.core.truth_(running)?"running":(cljs.core.truth_(status)?(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(status)):"idle"
)));
var G__34497 = knoxx.frontend.admin.event_agent_components.badge;
var G__34498 = (function (){var obj34500 = ({"tone":tone,"children":label});
return obj34500;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__34497,G__34498) : helix.core.jsx.call(null,G__34497,G__34498));
});
if(goog.DEBUG === true){
var G__34501 = G__34492;
(G__34501.displayName = "knoxx.frontend.admin.event-agent-components/status-badge");

return G__34501;
} else {
return G__34492;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agent_components.js.map
