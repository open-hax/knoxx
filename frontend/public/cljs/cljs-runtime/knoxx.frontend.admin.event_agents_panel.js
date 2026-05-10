goog.provide('knoxx.frontend.admin.event_agents_panel');
var module$dist$bridge$knoxx_frontend_bridge_es=shadow.js.require("module$dist$bridge$knoxx_frontend_bridge_es", {});
/**
 * Single-binding helper: (await [x (foo)] (bar x))
 */
knoxx.frontend.admin.event_agents_panel.await$ = (function knoxx$frontend$admin$event_agents_panel$await(var_args){
var args__5882__auto__ = [];
var len__5876__auto___13778 = arguments.length;
var i__5877__auto___13779 = (0);
while(true){
if((i__5877__auto___13779 < len__5876__auto___13778)){
args__5882__auto__.push((arguments[i__5877__auto___13779]));

var G__13780 = (i__5877__auto___13779 + (1));
i__5877__auto___13779 = G__13780;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.frontend.admin.event_agents_panel.await$.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.frontend.admin.event_agents_panel.await$.cljs$core$IFn$_invoke$arity$variadic = (function (p__13425,body){
var vec__13426 = p__13425;
var sym = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13426,(0),null);
var expr = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13426,(1),null);
return cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","->","cljs.core/->",1488366311,null),null,(1),null)),(new cljs.core.List(null,expr,null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((new cljs.core.List(null,new cljs.core.Symbol(null,".then",".then",224668574,null),null,(1),null)),(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","fn","cljs.core/fn",-1065745098,null),null,(1),null)),(new cljs.core.List(null,cljs.core.vec(cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$1((new cljs.core.List(null,sym,null,(1),null)))))),null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([body], 0)))),null,(1),null))))),null,(1),null))], 0))));
}));

(knoxx.frontend.admin.event_agents_panel.await$.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.frontend.admin.event_agents_panel.await$.cljs$lang$applyTo = (function (seq13423){
var G__13424 = cljs.core.first(seq13423);
var seq13423__$1 = cljs.core.next(seq13423);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__13424,seq13423__$1);
}));

/**
 * Parallel await for multiple promises.
 */
knoxx.frontend.admin.event_agents_panel.await_all = (function knoxx$frontend$admin$event_agents_panel$await_all(var_args){
var args__5882__auto__ = [];
var len__5876__auto___13781 = arguments.length;
var i__5877__auto___13782 = (0);
while(true){
if((i__5877__auto___13782 < len__5876__auto___13781)){
args__5882__auto__.push((arguments[i__5877__auto___13782]));

var G__13783 = (i__5877__auto___13782 + (1));
i__5877__auto___13782 = G__13783;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.frontend.admin.event_agents_panel.await_all.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.frontend.admin.event_agents_panel.await_all.cljs$core$IFn$_invoke$arity$variadic = (function (bindings,body){
var syms = cljs.core.take_nth.cljs$core$IFn$_invoke$arity$2((2),bindings);
var exprs = cljs.core.take_nth.cljs$core$IFn$_invoke$arity$2((2),cljs.core.rest(bindings));
return cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","->","cljs.core/->",1488366311,null),null,(1),null)),(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((new cljs.core.List(null,new cljs.core.Symbol("js","Promise.all","js/Promise.all",1722974586,null),null,(1),null)),(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","array","cljs.core/array",486685886,null),null,(1),null)),exprs))),null,(1),null))))),null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$2((new cljs.core.List(null,new cljs.core.Symbol(null,".then",".then",224668574,null),null,(1),null)),(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","fn","cljs.core/fn",-1065745098,null),null,(1),null)),(new cljs.core.List(null,cljs.core.vec(cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$1((new cljs.core.List(null,new cljs.core.Symbol(null,"res__13430__auto__","res__13430__auto__",-1757680352,null),null,(1),null)))))),null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(new cljs.core.List(null,cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","let","cljs.core/let",-308701135,null),null,(1),null)),(new cljs.core.List(null,cljs.core.vec(cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$1(cljs.core.interleave.cljs$core$IFn$_invoke$arity$2(syms,cljs.core.map_indexed.cljs$core$IFn$_invoke$arity$2((function (i,_){
return cljs.core.sequence.cljs$core$IFn$_invoke$arity$1(cljs.core.seq(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic((new cljs.core.List(null,new cljs.core.Symbol("cljs.core","aget","cljs.core/aget",6345791,null),null,(1),null)),(new cljs.core.List(null,new cljs.core.Symbol(null,"res__13429__auto__","res__13429__auto__",1535429519,null),null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(new cljs.core.List(null,i,null,(1),null))], 0))));
}),exprs)))))),null,(1),null)),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([body], 0)))),null,(1),null))], 0)))),null,(1),null))))),null,(1),null))], 0))));
}));

(knoxx.frontend.admin.event_agents_panel.await_all.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.frontend.admin.event_agents_panel.await_all.cljs$lang$applyTo = (function (seq13431){
var G__13432 = cljs.core.first(seq13431);
var seq13431__$1 = cljs.core.next(seq13431);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__13432,seq13431__$1);
}));


/**
 * Control buttons above the job list.
 */
knoxx.frontend.admin.event_agents_panel.sidebar_controls = (function (){var G__13434 = (function knoxx$frontend$admin$event_agents_panel$sidebar_controls_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13435 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13438 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13435,(0),null);
var map__13438__$1 = cljs.core.__destructure_map(map__13438);
var on_start = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"on-start","on-start",-1839785985));
var toggling_runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"toggling-runtime","toggling-runtime",1016155616));
var saving_control = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"saving-control","saving-control",1969586949));
var on_save = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"on-save","on-save",1618176266));
var on_refresh = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"on-refresh","on-refresh",-1724176245));
var on_reset = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"on-reset","on-reset",1057446829));
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var draft = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"draft","draft",1421831058));
var on_stop = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"on-stop","on-stop",1520114515));
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var loading = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"loading","loading",-737050189));
var resetting_runtime = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13438__$1,new cljs.core.Keyword(null,"resetting-runtime","resetting-runtime",-34086793));

var G__13439 = "div";
var G__13440 = (function (){var obj13442 = ({"className":"grid gap-2","children":[(function (){var G__13443 = "button";
var G__13444 = (function (){var obj13446 = ({"type":"button","onClick":on_refresh,"disabled":(function (){var or__5142__auto__ = loading;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = saving_control;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = toggling_runtime;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return resetting_runtime;
}
}
}
})(),"className":(""+"inline-flex items-center justify-center rounded-md border border-slate-700 bg-slate-900 "+"px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"),"children":(cljs.core.truth_(loading)?"Loading\u2026":"Refresh")});
return obj13446;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13443,G__13444) : helix.core.jsx.call(null,G__13443,G__13444));
})(),(function (){var G__13447 = "button";
var G__13448 = (function (){var obj13450 = ({"type":"button","onClick":on_save,"disabled":(function (){var or__5142__auto__ = cljs.core.not(can_manage);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.not(draft);
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = saving_control;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = toggling_runtime;
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return resetting_runtime;
}
}
}
}
})(),"className":(""+"inline-flex items-center justify-center rounded-md bg-sky-600 "+"px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"),"children":(cljs.core.truth_(saving_control)?"Saving\u2026":"Save runtime")});
return obj13450;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13447,G__13448) : helix.core.jsx.call(null,G__13447,G__13448));
})(),(cljs.core.truth_((function (){var and__5140__auto__ = status;
if(cljs.core.truth_(and__5140__auto__)){
return status.runtime.running;
} else {
return and__5140__auto__;
}
})())?(function (){var G__13451 = "button";
var G__13452 = (function (){var obj13454 = ({"type":"button","onClick":on_stop,"disabled":(function (){var or__5142__auto__ = cljs.core.not(can_manage);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = toggling_runtime;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return resetting_runtime;
}
}
})(),"className":(""+"inline-flex items-center justify-center rounded-md bg-rose-700 "+"px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-rose-600 disabled:opacity-60"),"title":"Stops cron scheduling + unsubscribes Discord gateway. Does not hard-cancel an in-flight LLM request.","children":(cljs.core.truth_(toggling_runtime)?"Stopping\u2026":"Stop runtime")});
return obj13454;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13451,G__13452) : helix.core.jsx.call(null,G__13451,G__13452));
})():(function (){var G__13455 = "button";
var G__13456 = (function (){var obj13458 = ({"type":"button","onClick":on_start,"disabled":(function (){var or__5142__auto__ = cljs.core.not(can_manage);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = toggling_runtime;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return resetting_runtime;
}
}
})(),"className":(""+"inline-flex items-center justify-center rounded-md bg-emerald-700 "+"px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-emerald-600 disabled:opacity-60"),"children":(cljs.core.truth_(toggling_runtime)?"Starting\u2026":"Start runtime")});
return obj13458;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13455,G__13456) : helix.core.jsx.call(null,G__13455,G__13456));
})()),(function (){var G__13459 = "button";
var G__13460 = (function (){var obj13462 = ({"type":"button","onClick":on_reset,"disabled":(function (){var or__5142__auto__ = cljs.core.not(can_manage);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = toggling_runtime;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = resetting_runtime;
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return saving_control;
}
}
}
})(),"className":(""+"inline-flex items-center justify-center rounded-md border border-amber-700 bg-amber-950/40 "+"px-3 py-2 text-sm font-semibold text-amber-100 hover:bg-amber-900/60 disabled:opacity-60"),"title":"Stop the runtime, clear persisted event-agent state, disable cron jobs, and leave the scheduler stopped for review.","children":(cljs.core.truth_(resetting_runtime)?"Resetting\u2026":"Full reset")});
return obj13462;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13459,G__13460) : helix.core.jsx.call(null,G__13459,G__13460));
})()]});
return obj13442;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13439,G__13440) : helix.core.jsxs.call(null,G__13439,G__13440));
});
if(goog.DEBUG === true){
var G__13463 = G__13434;
(G__13463.displayName = "knoxx.frontend.admin.event-agents-panel/sidebar-controls");

return G__13463;
} else {
return G__13434;
}
})();




/**
 * Stats cards in the sidebar.
 */
knoxx.frontend.admin.event_agents_panel.sidebar_stats = (function (){var G__13465 = (function knoxx$frontend$admin$event_agents_panel$sidebar_stats_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13466 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13469 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13466,(0),null);
var map__13469__$1 = cljs.core.__destructure_map(map__13469);
var status = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13469__$1,new cljs.core.Keyword(null,"status","status",-1997798413));
var draft = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13469__$1,new cljs.core.Keyword(null,"draft","draft",1421831058));
var recent_event_count = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13469__$1,new cljs.core.Keyword(null,"recent-event-count","recent-event-count",-657631028));
var seen_discord_channels = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13469__$1,new cljs.core.Keyword(null,"seen-discord-channels","seen-discord-channels",-972542083));

var G__13470 = "div";
var G__13471 = (function (){var obj13473 = ({"className":"grid gap-2","children":[(function (){var G__13474 = "div";
var G__13475 = (function (){var obj13477 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2","children":[(function (){var G__13478 = "div";
var G__13479 = (function (){var obj13481 = ({"className":"text-[11px] uppercase tracking-wide text-slate-500","children":"Discord token"});
return obj13481;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13478,G__13479) : helix.core.jsx.call(null,G__13478,G__13479));
})(),(function (){var G__13482 = "div";
var G__13483 = (function (){var obj13485 = ({"className":"mt-1 flex items-center gap-2 text-xs text-slate-200","children":[(cljs.core.truth_((function (){var and__5140__auto__ = status;
if(cljs.core.truth_(and__5140__auto__)){
return status.configured;
} else {
return and__5140__auto__;
}
})())?(function (){var G__13486 = knoxx.frontend.admin.event_agent_components.badge;
var G__13487 = (function (){var obj13489 = ({"tone":new cljs.core.Keyword(null,"success","success",1890645906),"children":"Configured"});
return obj13489;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13486,G__13487) : helix.core.jsx.call(null,G__13486,G__13487));
})():(function (){var G__13490 = knoxx.frontend.admin.event_agent_components.badge;
var G__13491 = (function (){var obj13493 = ({"tone":new cljs.core.Keyword(null,"warn","warn",-436710552),"children":"Missing"});
return obj13493;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13490,G__13491) : helix.core.jsx.call(null,G__13490,G__13491));
})()),(cljs.core.truth_((function (){var and__5140__auto__ = status;
if(cljs.core.truth_(and__5140__auto__)){
return status.tokenPreview;
} else {
return and__5140__auto__;
}
})())?(function (){var G__13494 = "span";
var G__13495 = (function (){var obj13497 = ({"className":"font-mono text-[11px] text-slate-400","children":status.tokenPreview});
return obj13497;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13494,G__13495) : helix.core.jsx.call(null,G__13494,G__13495));
})():null)]});
return obj13485;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13482,G__13483) : helix.core.jsxs.call(null,G__13482,G__13483));
})()]});
return obj13477;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13474,G__13475) : helix.core.jsxs.call(null,G__13474,G__13475));
})(),(function (){var G__13498 = "div";
var G__13499 = (function (){var obj13501 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2","children":[(function (){var G__13502 = "div";
var G__13503 = (function (){var obj13505 = ({"className":"text-[11px] uppercase tracking-wide text-slate-500","children":"Runtime"});
return obj13505;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13502,G__13503) : helix.core.jsx.call(null,G__13502,G__13503));
})(),(function (){var G__13506 = "div";
var G__13507 = (function (){var obj13509 = ({"className":"mt-1 flex items-center gap-2 text-xs text-slate-200","children":[(cljs.core.truth_((function (){var and__5140__auto__ = status;
if(cljs.core.truth_(and__5140__auto__)){
return status.runtime.running;
} else {
return and__5140__auto__;
}
})())?(function (){var G__13510 = knoxx.frontend.admin.event_agent_components.badge;
var G__13511 = (function (){var obj13513 = ({"tone":new cljs.core.Keyword(null,"success","success",1890645906),"children":"Running"});
return obj13513;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13510,G__13511) : helix.core.jsx.call(null,G__13510,G__13511));
})():(function (){var G__13514 = knoxx.frontend.admin.event_agent_components.badge;
var G__13515 = (function (){var obj13517 = ({"tone":new cljs.core.Keyword(null,"warn","warn",-436710552),"children":"Stopped"});
return obj13517;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13514,G__13515) : helix.core.jsx.call(null,G__13514,G__13515));
})()),(function (){var G__13518 = "span";
var G__13519 = (function (){var obj13521 = ({"children":(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((cljs.core.truth_(draft)?cljs.core.count(draft.jobs):(0)))+" jobs")});
return obj13521;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13518,G__13519) : helix.core.jsx.call(null,G__13518,G__13519));
})()]});
return obj13509;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13506,G__13507) : helix.core.jsxs.call(null,G__13506,G__13507));
})()]});
return obj13501;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13498,G__13499) : helix.core.jsxs.call(null,G__13498,G__13499));
})(),(function (){var G__13522 = "div";
var G__13523 = (function (){var obj13525 = ({"className":"grid grid-cols-2 gap-2","children":[(function (){var G__13526 = "div";
var G__13527 = (function (){var obj13529 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2","children":[(function (){var G__13530 = "div";
var G__13531 = (function (){var obj13533 = ({"className":"text-[11px] uppercase tracking-wide text-slate-500","children":"Recent events"});
return obj13533;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13530,G__13531) : helix.core.jsx.call(null,G__13530,G__13531));
})(),(function (){var G__13534 = "div";
var G__13535 = (function (){var obj13537 = ({"className":"mt-1 text-lg font-semibold text-slate-100","children":recent_event_count});
return obj13537;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13534,G__13535) : helix.core.jsx.call(null,G__13534,G__13535));
})(),(function (){var G__13538 = "div";
var G__13539 = (function (){var obj13541 = ({"className":"text-[11px] text-slate-500","children":"Buffered"});
return obj13541;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13538,G__13539) : helix.core.jsx.call(null,G__13538,G__13539));
})()]});
return obj13529;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13526,G__13527) : helix.core.jsxs.call(null,G__13526,G__13527));
})(),(function (){var G__13542 = "div";
var G__13543 = (function (){var obj13545 = ({"className":"rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2","children":[(function (){var G__13546 = "div";
var G__13547 = (function (){var obj13549 = ({"className":"text-[11px] uppercase tracking-wide text-slate-500","children":"Freshness"});
return obj13549;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13546,G__13547) : helix.core.jsx.call(null,G__13546,G__13547));
})(),(function (){var G__13550 = "div";
var G__13551 = (function (){var obj13553 = ({"className":"mt-1 text-lg font-semibold text-slate-100","children":seen_discord_channels});
return obj13553;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13550,G__13551) : helix.core.jsx.call(null,G__13550,G__13551));
})(),(function (){var G__13554 = "div";
var G__13555 = (function (){var obj13557 = ({"className":"text-[11px] text-slate-500","children":"Channels"});
return obj13557;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13554,G__13555) : helix.core.jsx.call(null,G__13554,G__13555));
})()]});
return obj13545;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13542,G__13543) : helix.core.jsxs.call(null,G__13542,G__13543));
})()]});
return obj13525;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13522,G__13523) : helix.core.jsxs.call(null,G__13522,G__13523));
})()]});
return obj13473;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13470,G__13471) : helix.core.jsxs.call(null,G__13470,G__13471));
});
if(goog.DEBUG === true){
var G__13558 = G__13465;
(G__13558.displayName = "knoxx.frontend.admin.event-agents-panel/sidebar-stats");

return G__13558;
} else {
return G__13465;
}
})();




/**
 * Success or error notice banner.
 */
knoxx.frontend.admin.event_agents_panel.notice_banner = (function (){var G__13560 = (function knoxx$frontend$admin$event_agents_panel$notice_banner_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13561 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13564 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13561,(0),null);
var map__13564__$1 = cljs.core.__destructure_map(map__13564);
var notice = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13564__$1,new cljs.core.Keyword(null,"notice","notice",-1121239112));

if(cljs.core.truth_(notice)){
var G__13565 = "div";
var G__13566 = (function (){var obj13568 = ({"className":(""+"rounded-lg border px-3 py-2 text-sm "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var G__13570 = new cljs.core.Keyword(null,"tone","tone",-1422788785).cljs$core$IFn$_invoke$arity$1(notice);
var G__13570__$1 = (((G__13570 instanceof cljs.core.Keyword))?G__13570.fqn:null);
switch (G__13570__$1) {
case "success":
return "border-emerald-500/30 bg-emerald-500/10 text-emerald-200";

break;
default:
return "border-rose-500/30 bg-rose-500/10 text-rose-200";

}
})())),"children":new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(notice)});
return obj13568;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13565,G__13566) : helix.core.jsx.call(null,G__13565,G__13566));
} else {
return null;
}
});
if(goog.DEBUG === true){
var G__13571 = G__13560;
(G__13571.displayName = "knoxx.frontend.admin.event-agents-panel/notice-banner");

return G__13571;
} else {
return G__13560;
}
})();




/**
 * Error banner.
 */
knoxx.frontend.admin.event_agents_panel.error_banner = (function (){var G__13573 = (function knoxx$frontend$admin$event_agents_panel$error_banner_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13574 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13577 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13574,(0),null);
var map__13577__$1 = cljs.core.__destructure_map(map__13577);
var error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13577__$1,new cljs.core.Keyword(null,"error","error",-978969032));

if(cljs.core.seq(error)){
var G__13578 = "div";
var G__13579 = (function (){var obj13581 = ({"className":"rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200","children":error});
return obj13581;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13578,G__13579) : helix.core.jsx.call(null,G__13578,G__13579));
} else {
return null;
}
});
if(goog.DEBUG === true){
var G__13582 = G__13573;
(G__13582.displayName = "knoxx.frontend.admin.event-agents-panel/error-banner");

return G__13582;
} else {
return G__13573;
}
})();




/**
 * Main panel component.
 * Props:
 * - can-manage: boolean
 * - tools: vector of AdminToolDefinition
 * - on-selected-job-change: callback fn
 */
knoxx.frontend.admin.event_agents_panel.event_agents_panel = (function (){var G__13588 = (function knoxx$frontend$admin$event_agents_panel$event_agents_panel_render(props__12020__auto__,maybe_ref__12021__auto__){
var vec__13589 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [helix.core.extract_cljs_props(props__12020__auto__),maybe_ref__12021__auto__], null);
var map__13592 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13589,(0),null);
var map__13592__$1 = cljs.core.__destructure_map(map__13592);
var can_manage = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13592__$1,new cljs.core.Keyword(null,"can-manage","can-manage",-509639858));
var tools = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13592__$1,new cljs.core.Keyword(null,"tools","tools",-1241731990));
var on_selected_job_change = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__13592__$1,new cljs.core.Keyword(null,"on-selected-job-change","on-selected-job-change",1483530384));

var vec__13593 = helix.hooks.use_state(true);
var loading = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13593,(0),null);
var set_loading = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13593,(1),null);
var vec__13596 = helix.hooks.use_state(false);
var saving_token = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13596,(0),null);
var set_saving_token = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13596,(1),null);
var vec__13599 = helix.hooks.use_state(false);
var saving_control = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13599,(0),null);
var set_saving_control = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13599,(1),null);
var vec__13602 = helix.hooks.use_state(null);
var running_job_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13602,(0),null);
var set_running_job_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13602,(1),null);
var vec__13605 = helix.hooks.use_state(false);
var dispatching_event = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13605,(0),null);
var set_dispatching_event = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13605,(1),null);
var vec__13608 = helix.hooks.use_state(false);
var toggling_runtime = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13608,(0),null);
var set_toggling_runtime = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13608,(1),null);
var vec__13611 = helix.hooks.use_state(false);
var resetting_runtime = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13611,(0),null);
var set_resetting_runtime = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13611,(1),null);
var vec__13614 = helix.hooks.use_state(null);
var notice = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13614,(0),null);
var set_notice = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13614,(1),null);
var vec__13617 = helix.hooks.use_state("");
var error = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13617,(0),null);
var set_error = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13617,(1),null);
var vec__13620 = helix.hooks.use_state(null);
var status = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13620,(0),null);
var set_status = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13620,(1),null);
var vec__13623 = helix.hooks.use_state(null);
var draft = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13623,(0),null);
var set_draft = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13623,(1),null);
var vec__13626 = helix.hooks.use_state(cljs.core.PersistentArrayMap.EMPTY);
var json_drafts = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13626,(0),null);
var set_json_drafts = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13626,(1),null);
var vec__13629 = helix.hooks.use_state(null);
var selected_job_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13629,(0),null);
var set_selected_job_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13629,(1),null);
var vec__13632 = helix.hooks.use_state("");
var job_search = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13632,(0),null);
var set_job_search = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13632,(1),null);
var vec__13635 = helix.hooks.use_state("");
var draft_token = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13635,(0),null);
var set_draft_token = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13635,(1),null);
var vec__13638 = helix.hooks.use_state("github");
var event_source_kind = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13638,(0),null);
var set_event_source_kind = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13638,(1),null);
var vec__13641 = helix.hooks.use_state("issues.opened");
var event_kind = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13641,(0),null);
var set_event_kind = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13641,(1),null);
var vec__13644 = helix.hooks.use_state("{\"repository\": \"open-hax/openplanner\", \"title\": \"Example event\", \"content\": \"Investigate this issue\"}");
var event_payload = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13644,(0),null);
var set_event_payload = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__13644,(1),null);
var runtime_jobs = (cljs.core.truth_(status)?status.runtime.jobs:[]);
var discord_source = (cljs.core.truth_(draft)?draft.sources.discord:({}));
var recent_event_count = (cljs.core.truth_((function (){var and__5140__auto__ = status;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = status.runtime.sources.recentEvents;
if(cljs.core.truth_(and__5140__auto____$1)){
return Array.isArray(status.runtime.sources.recentEvents);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())?status.runtime.sources.recentEvents.length:(0));
var discord_runtime = (cljs.core.truth_(status)?status.runtime.sources.discord:null);
var seen_discord_channels = (cljs.core.truth_((function (){var and__5140__auto__ = discord_runtime;
if(cljs.core.truth_(and__5140__auto__)){
return Array.isArray(discord_runtime.lastSeenChannels);
} else {
return and__5140__auto__;
}
})())?discord_runtime.lastSeenChannels.length:(0));
var filtered_jobs = (cljs.core.truth_(draft)?(function (){var query = knoxx.frontend.admin.event_agent_utils.normalize_search(job_search);
if(cljs.core.empty_QMARK_(query)){
return draft.jobs;
} else {
return cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__13583_SHARP_){
return clojure.string.includes_QMARK_(knoxx.frontend.admin.event_agent_utils.job_search_text(p1__13583_SHARP_),query);
}),draft.jobs);
}
})():cljs.core.PersistentVector.EMPTY);
var selected_job = ((cljs.core.seq(filtered_jobs))?(function (){var or__5142__auto__ = cljs.core.some((function (p1__13584_SHARP_){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__13584_SHARP_.id,selected_job_id)){
return p1__13584_SHARP_;
} else {
return null;
}
}),filtered_jobs);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.first(filtered_jobs);
}
})():null);
var selected_runtime = (cljs.core.truth_(selected_job)?knoxx.frontend.admin.event_agent_utils.runtime_for_job(runtime_jobs,selected_job.id):null);
var selected_job_json_draft = (cljs.core.truth_(selected_job)?(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(json_drafts,selected_job.id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"source-config","source-config",305510816),"{}",new cljs.core.Keyword(null,"filters","filters",974726919),"{}",new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),"[]"], null);
}
})():null);
var load_data = (function (){
(set_loading.cljs$core$IFn$_invoke$arity$1 ? set_loading.cljs$core$IFn$_invoke$arity$1(true) : set_loading.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return Promise.all([module$dist$bridge$knoxx_frontend_bridge_es.getDiscordConfig(),module$dist$bridge$knoxx_frontend_bridge_es.getEventAgentControl()]).then((function (res){
var token_status = (res[(0)]);
var control_status = (res[(1)]);
var merged = Object.assign(({}),control_status,({"configured": token_status.configured, "tokenPreview": token_status.tokenPreview}));
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(merged) : set_status.call(null,merged));

var G__13647_13785 = merged.control;
(set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13647_13785) : set_draft.call(null,G__13647_13785));

(set_draft_token.cljs$core$IFn$_invoke$arity$1 ? set_draft_token.cljs$core$IFn$_invoke$arity$1("") : set_draft_token.call(null,""));

var G__13648_13786 = knoxx.frontend.admin.event_agent_utils.seed_json_drafts(merged.control.jobs);
(set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13648_13786) : set_json_drafts.call(null,G__13648_13786));

var G__13649_13787 = (cljs.core.truth_(merged.availableSourceKinds.includes("github"))?"github":(function (){var or__5142__auto__ = cljs.core.first(merged.availableSourceKinds);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "manual";
}
})());
(set_event_source_kind.cljs$core$IFn$_invoke$arity$1 ? set_event_source_kind.cljs$core$IFn$_invoke$arity$1(G__13649_13787) : set_event_source_kind.call(null,G__13649_13787));

return (set_loading.cljs$core$IFn$_invoke$arity$1 ? set_loading.cljs$core$IFn$_invoke$arity$1(false) : set_loading.call(null,false));
})).catch((function (err){
var G__13650_13788 = err.message;
(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1(G__13650_13788) : set_error.call(null,G__13650_13788));

return (set_loading.cljs$core$IFn$_invoke$arity$1 ? set_loading.cljs$core$IFn$_invoke$arity$1(false) : set_loading.call(null,false));
}));
});
var update_job = (function (job_id,patch){
if(cljs.core.truth_(draft)){
var G__13651 = cljs.core.update.cljs$core$IFn$_invoke$arity$3(draft,new cljs.core.Keyword(null,"jobs","jobs",-313607120),(function (jobs){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (job){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(job.id,job_id)){
return Object.assign(({}),job,patch);
} else {
return job;
}
}),jobs);
}));
return (set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13651) : set_draft.call(null,G__13651));
} else {
return null;
}
});
var update_json_draft = (function (job_id,field,value){
var G__13652 = cljs.core.update.cljs$core$IFn$_invoke$arity$3(json_drafts,job_id,(function (d){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3((function (){var or__5142__auto__ = d;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"source-config","source-config",305510816),"{}",new cljs.core.Keyword(null,"filters","filters",974726919),"{}",new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),"[]"], null);
}
})(),field,value);
}));
return (set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13652) : set_json_drafts.call(null,G__13652));
});
var parse_control_for_save = (function (){
if(cljs.core.truth_(draft)){
} else {
throw (new Error("No draft control loaded"));
}

var jobs = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (job){
var drafts = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(json_drafts,job.id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"source-config","source-config",305510816),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto____$1 = job.source.config;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword(null,"filters","filters",974726919),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto____$1 = job.filters;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})()),new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557),knoxx.frontend.admin.event_agent_utils.pretty_json((function (){var or__5142__auto____$1 = job.agentSpec.toolPolicies;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())], null);
}
})();
var source_config = (function (){try{return JSON.parse(new cljs.core.Keyword(null,"source-config","source-config",305510816).cljs$core$IFn$_invoke$arity$1(drafts));
}catch (e13653){if((e13653 instanceof Error)){
var err = e13653;
throw (new Error((""+"Invalid source config JSON for job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))));
} else {
throw e13653;

}
}})();
var filters = (function (){try{return JSON.parse(new cljs.core.Keyword(null,"filters","filters",974726919).cljs$core$IFn$_invoke$arity$1(drafts));
}catch (e13654){if((e13654 instanceof Error)){
var err = e13654;
throw (new Error((""+"Invalid filters JSON for job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))));
} else {
throw e13654;

}
}})();
var tool_policies = (function (){try{return JSON.parse(new cljs.core.Keyword(null,"tool-policies","tool-policies",-244759557).cljs$core$IFn$_invoke$arity$1(drafts));
}catch (e13655){if((e13655 instanceof Error)){
var err = e13655;
throw (new Error((""+"Invalid tool policy JSON for job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job.name)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err.message))));
} else {
throw e13655;

}
}})();
return Object.assign(({}),job,({"source": Object.assign(({}),job.source,({"config": source_config})), "filters": filters, "agentSpec": Object.assign(({}),job.agentSpec,({"toolPolicies": tool_policies}))}));
}),draft.jobs);
return Object.assign(({}),draft,({"jobs": jobs}));
});
var handle_save_token = (function (){
if(cljs.core.truth_(can_manage)){
var normalized = clojure.string.trim(draft_token);
if(cljs.core.empty_QMARK_(normalized)){
return (set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("Bot token must not be blank") : set_error.call(null,"Bot token must not be blank"));
} else {
(set_saving_token.cljs$core$IFn$_invoke$arity$1 ? set_saving_token.cljs$core$IFn$_invoke$arity$1(true) : set_saving_token.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return module$dist$bridge$knoxx_frontend_bridge_es.updateDiscordConfig(normalized).then((function (updated){
var G__13656_13789 = (function (current){
if(cljs.core.truth_(current)){
return Object.assign(({}),current,({"configured": updated.configured, "tokenPreview": updated.tokenPreview}));
} else {
return null;
}
});
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(G__13656_13789) : set_status.call(null,G__13656_13789));

(set_draft_token.cljs$core$IFn$_invoke$arity$1 ? set_draft_token.cljs$core$IFn$_invoke$arity$1("") : set_draft_token.call(null,""));

var G__13657_13790 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Discord bot token saved. Preview: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(updated.tokenPreview))], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13657_13790) : set_notice.call(null,G__13657_13790));

return load_data();
})).catch((function (err){
var G__13658 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13658) : set_notice.call(null,G__13658));
})).finally((function (){
return (set_saving_token.cljs$core$IFn$_invoke$arity$1 ? set_saving_token.cljs$core$IFn$_invoke$arity$1(false) : set_saving_token.call(null,false));
}));
}
} else {
return null;
}
});
var handle_save_control = (function (){
if(cljs.core.truth_((function (){var and__5140__auto__ = can_manage;
if(cljs.core.truth_(and__5140__auto__)){
return draft;
} else {
return and__5140__auto__;
}
})())){
(set_saving_control.cljs$core$IFn$_invoke$arity$1 ? set_saving_control.cljs$core$IFn$_invoke$arity$1(true) : set_saving_control.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

try{var next = parse_control_for_save();
return module$dist$bridge$knoxx_frontend_bridge_es.updateEventAgentControl(next).then((function (updated){
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(updated) : set_status.call(null,updated));

var G__13661_13791 = updated.control;
(set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13661_13791) : set_draft.call(null,G__13661_13791));

var G__13662_13792 = knoxx.frontend.admin.event_agent_utils.seed_json_drafts(updated.control.jobs);
(set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13662_13792) : set_json_drafts.call(null,G__13662_13792));

var G__13663 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),"Event-agent control plane updated and runtime reloaded."], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13663) : set_notice.call(null,G__13663));
})).catch((function (err){
var G__13664 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13664) : set_notice.call(null,G__13664));
})).finally((function (){
return (set_saving_control.cljs$core$IFn$_invoke$arity$1 ? set_saving_control.cljs$core$IFn$_invoke$arity$1(false) : set_saving_control.call(null,false));
}));
}catch (e13659){if((e13659 instanceof Error)){
var err = e13659;
var G__13660_13793 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13660_13793) : set_notice.call(null,G__13660_13793));

return (set_saving_control.cljs$core$IFn$_invoke$arity$1 ? set_saving_control.cljs$core$IFn$_invoke$arity$1(false) : set_saving_control.call(null,false));
} else {
throw e13659;

}
}} else {
return null;
}
});
var handle_run_job = (function (job_id){
if(cljs.core.truth_(can_manage)){
(set_running_job_id.cljs$core$IFn$_invoke$arity$1 ? set_running_job_id.cljs$core$IFn$_invoke$arity$1(job_id) : set_running_job_id.call(null,job_id));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return module$dist$bridge$knoxx_frontend_bridge_es.runEventAgentJob(job_id).then((function (_){
var G__13665_13794 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Queued job "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(job_id)+".")], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13665_13794) : set_notice.call(null,G__13665_13794));

return load_data();
})).catch((function (err){
var G__13666 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13666) : set_notice.call(null,G__13666));
})).finally((function (){
return (set_running_job_id.cljs$core$IFn$_invoke$arity$1 ? set_running_job_id.cljs$core$IFn$_invoke$arity$1(null) : set_running_job_id.call(null,null));
}));
} else {
return null;
}
});
var handle_dispatch_event = (function (){
if(cljs.core.truth_(can_manage)){
(set_dispatching_event.cljs$core$IFn$_invoke$arity$1 ? set_dispatching_event.cljs$core$IFn$_invoke$arity$1(true) : set_dispatching_event.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

try{var payload = JSON.parse((function (){var or__5142__auto__ = event_payload;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "{}";
}
})());
return module$dist$bridge$knoxx_frontend_bridge_es.dispatchEventAgentEvent(({"sourceKind": event_source_kind, "eventKind": event_kind, "payload": payload})).then((function (result){
var G__13669_13795 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Dispatched "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_source_kind)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(event_kind)+". Matched jobs: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = clojure.string.join.cljs$core$IFn$_invoke$arity$2(", ",result.matchedJobs);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "none";
}
})())+".")], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13669_13795) : set_notice.call(null,G__13669_13795));

return load_data();
})).catch((function (err){
var G__13670 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13670) : set_notice.call(null,G__13670));
})).finally((function (){
return (set_dispatching_event.cljs$core$IFn$_invoke$arity$1 ? set_dispatching_event.cljs$core$IFn$_invoke$arity$1(false) : set_dispatching_event.call(null,false));
}));
}catch (e13667){if((e13667 instanceof Error)){
var err = e13667;
var G__13668_13796 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13668_13796) : set_notice.call(null,G__13668_13796));

return (set_dispatching_event.cljs$core$IFn$_invoke$arity$1 ? set_dispatching_event.cljs$core$IFn$_invoke$arity$1(false) : set_dispatching_event.call(null,false));
} else {
throw e13667;

}
}} else {
return null;
}
});
var handle_stop_runtime = (function (){
if(cljs.core.truth_(can_manage)){
(set_toggling_runtime.cljs$core$IFn$_invoke$arity$1 ? set_toggling_runtime.cljs$core$IFn$_invoke$arity$1(true) : set_toggling_runtime.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return module$dist$bridge$knoxx_frontend_bridge_es.stopEventAgentRuntime().then((function (updated){
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(updated) : set_status.call(null,updated));

var G__13671_13797 = updated.control;
(set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13671_13797) : set_draft.call(null,G__13671_13797));

var G__13672_13798 = knoxx.frontend.admin.event_agent_utils.seed_json_drafts(updated.control.jobs);
(set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13672_13798) : set_json_drafts.call(null,G__13672_13798));

var G__13673 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),"Event-agent runtime stopped (schedulers cleared)."], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13673) : set_notice.call(null,G__13673));
})).catch((function (err){
var G__13674 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13674) : set_notice.call(null,G__13674));
})).finally((function (){
return (set_toggling_runtime.cljs$core$IFn$_invoke$arity$1 ? set_toggling_runtime.cljs$core$IFn$_invoke$arity$1(false) : set_toggling_runtime.call(null,false));
}));
} else {
return null;
}
});
var handle_start_runtime = (function (){
if(cljs.core.truth_(can_manage)){
(set_toggling_runtime.cljs$core$IFn$_invoke$arity$1 ? set_toggling_runtime.cljs$core$IFn$_invoke$arity$1(true) : set_toggling_runtime.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return module$dist$bridge$knoxx_frontend_bridge_es.startEventAgentRuntime().then((function (updated){
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(updated) : set_status.call(null,updated));

var G__13675_13799 = updated.control;
(set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13675_13799) : set_draft.call(null,G__13675_13799));

var G__13676_13800 = knoxx.frontend.admin.event_agent_utils.seed_json_drafts(updated.control.jobs);
(set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13676_13800) : set_json_drafts.call(null,G__13676_13800));

var G__13677_13801 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),"Event-agent runtime started."], null);
(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13677_13801) : set_notice.call(null,G__13677_13801));

return load_data();
})).catch((function (err){
var G__13678 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13678) : set_notice.call(null,G__13678));
})).finally((function (){
return (set_toggling_runtime.cljs$core$IFn$_invoke$arity$1 ? set_toggling_runtime.cljs$core$IFn$_invoke$arity$1(false) : set_toggling_runtime.call(null,false));
}));
} else {
return null;
}
});
var handle_reset_runtime = (function (){
if(cljs.core.truth_(can_manage)){
(set_resetting_runtime.cljs$core$IFn$_invoke$arity$1 ? set_resetting_runtime.cljs$core$IFn$_invoke$arity$1(true) : set_resetting_runtime.call(null,true));

(set_error.cljs$core$IFn$_invoke$arity$1 ? set_error.cljs$core$IFn$_invoke$arity$1("") : set_error.call(null,""));

(set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(null) : set_notice.call(null,null));

return module$dist$bridge$knoxx_frontend_bridge_es.resetEventAgentRuntime().then((function (updated){
(set_status.cljs$core$IFn$_invoke$arity$1 ? set_status.cljs$core$IFn$_invoke$arity$1(updated) : set_status.call(null,updated));

var G__13679_13802 = updated.control;
(set_draft.cljs$core$IFn$_invoke$arity$1 ? set_draft.cljs$core$IFn$_invoke$arity$1(G__13679_13802) : set_draft.call(null,G__13679_13802));

var G__13680_13803 = knoxx.frontend.admin.event_agent_utils.seed_json_drafts(updated.control.jobs);
(set_json_drafts.cljs$core$IFn$_invoke$arity$1 ? set_json_drafts.cljs$core$IFn$_invoke$arity$1(G__13680_13803) : set_json_drafts.call(null,G__13680_13803));

var G__13681 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"text","text",-1790561697),(""+"Event-agent runtime reset. Cleared "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(updated.reset.deletedCount)+" persisted state key(s) and disabled "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(updated.reset.disabledCronJobCount)+" cron job(s). Review schedules before restarting.")], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13681) : set_notice.call(null,G__13681));
})).catch((function (err){
var G__13682 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"tone","tone",-1422788785),new cljs.core.Keyword(null,"error","error",-978969032),new cljs.core.Keyword(null,"text","text",-1790561697),err.message], null);
return (set_notice.cljs$core$IFn$_invoke$arity$1 ? set_notice.cljs$core$IFn$_invoke$arity$1(G__13682) : set_notice.call(null,G__13682));
})).finally((function (){
return (set_resetting_runtime.cljs$core$IFn$_invoke$arity$1 ? set_resetting_runtime.cljs$core$IFn$_invoke$arity$1(false) : set_resetting_runtime.call(null,false));
}));
} else {
return null;
}
});
var G__13683_13804 = helix.hooks.wrap_fx((function (){
return load_data();
}));
var G__13684_13805 = [];
(helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2 ? helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2(G__13683_13804,G__13684_13805) : helix.hooks.raw_use_effect.call(null,G__13683_13804,G__13684_13805));

var G__13685_13806 = helix.hooks.wrap_fx((function (){
if(cljs.core.seq(filtered_jobs)){
if(cljs.core.truth_(cljs.core.some((function (p1__13585_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(p1__13585_SHARP_.id,selected_job_id);
}),filtered_jobs))){
return null;
} else {
var G__13687 = cljs.core.first(filtered_jobs).id;
return (set_selected_job_id.cljs$core$IFn$_invoke$arity$1 ? set_selected_job_id.cljs$core$IFn$_invoke$arity$1(G__13687) : set_selected_job_id.call(null,G__13687));
}
} else {
return null;
}
}));
var G__13686_13807 = [filtered_jobs];
(helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2 ? helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2(G__13685_13806,G__13686_13807) : helix.hooks.raw_use_effect.call(null,G__13685_13806,G__13686_13807));

var G__13688 = helix.hooks.wrap_fx((function (){
if(cljs.core.truth_(on_selected_job_change)){
(on_selected_job_change.cljs$core$IFn$_invoke$arity$1 ? on_selected_job_change.cljs$core$IFn$_invoke$arity$1(selected_job) : on_selected_job_change.call(null,selected_job));
} else {
}

if(cljs.core.truth_((function (){var or__5142__auto__ = loading;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ((cljs.core.not(draft)) || (cljs.core.not(status)));
}
})())){
var G__13690 = knoxx.frontend.admin.event_agents.loading_state;
var G__13691 = ({});
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13690,G__13691) : helix.core.jsx.call(null,G__13690,G__13691));
} else {
var G__13692 = "div";
var G__13693 = (function (){var obj13695 = ({"className":"flex flex-col h-full min-h-0","children":[(function (){var G__13696 = "div";
var G__13697 = (function (){var obj13699 = ({"className":"min-h-0 flex-1 overflow-hidden","children":(function (){var G__13700 = "div";
var G__13701 = (function (){var obj13703 = ({"className":"grid h-full min-h-0 min-w-[44rem] gap-3 grid-cols-[12rem_minmax(0,1fr)] xl:grid-cols-[13rem_minmax(0,1fr)]","children":[(function (){var G__13704 = "aside";
var G__13705 = (function (){var obj13707 = ({"className":"flex flex-col overflow-hidden h-full space-y-2 rounded-xl border border-slate-800 bg-slate-950/50 p-2.5","children":[(function (){var G__13708 = "div";
var G__13709 = (function (){var obj13711 = ({"className":"flex items-center justify-between gap-2","children":[(function (){var G__13712 = "div";
var G__13713 = (function (){var obj13715 = ({"className":"text-sm font-semibold text-slate-100","children":"Agents"});
return obj13715;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13712,G__13713) : helix.core.jsx.call(null,G__13712,G__13713));
})(),(function (){var G__13716 = "div";
var G__13717 = (function (){var obj13719 = ({"className":"text-[11px] text-slate-500","children":(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(filtered_jobs))+"/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.count(draft.jobs)))});
return obj13719;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13716,G__13717) : helix.core.jsx.call(null,G__13716,G__13717));
})()]});
return obj13711;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13708,G__13709) : helix.core.jsxs.call(null,G__13708,G__13709));
})(),(function (){var G__13720 = knoxx.frontend.admin.event_agents_panel.sidebar_controls;
var G__13721 = (function (){var obj13723 = ({"toggling-runtime":toggling_runtime,"on-start":handle_start_runtime,"on-stop":handle_stop_runtime,"resetting-runtime":resetting_runtime,"loading":loading,"saving-control":saving_control,"draft":draft,"on-save":handle_save_control,"status":status,"can-manage":can_manage,"on-refresh":load_data,"on-reset":handle_reset_runtime});
return obj13723;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13720,G__13721) : helix.core.jsx.call(null,G__13720,G__13721));
})(),(function (){var G__13724 = knoxx.frontend.admin.event_agents_panel.sidebar_stats;
var G__13725 = (function (){var obj13727 = ({"status":status,"draft":draft,"recent-event-count":recent_event_count,"seen-discord-channels":seen_discord_channels});
return obj13727;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13724,G__13725) : helix.core.jsx.call(null,G__13724,G__13725));
})(),(function (){var G__13728 = "label";
var G__13729 = (function (){var obj13731 = ({"className":"space-y-1 block","children":[(function (){var G__13732 = "div";
var G__13733 = (function (){var obj13735 = ({"className":"sr-only","children":"Search"});
return obj13735;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13732,G__13733) : helix.core.jsx.call(null,G__13732,G__13733));
})(),(function (){var G__13736 = "input";
var G__13737 = (function (){var obj13739 = ({"aria-label":"Search","value":helix.impl.props.or_undefined(job_search),"onChange":(function (p1__13586_SHARP_){
var G__13740 = p1__13586_SHARP_.target.value;
return (set_job_search.cljs$core$IFn$_invoke$arity$1 ? set_job_search.cljs$core$IFn$_invoke$arity$1(G__13740) : set_job_search.call(null,G__13740));
}),"placeholder":"Search\u2026","className":(""+"w-full rounded-md border border-slate-800 bg-slate-950/80 "+"px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500")});
return obj13739;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13736,G__13737) : helix.core.jsx.call(null,G__13736,G__13737));
})()]});
return obj13731;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13728,G__13729) : helix.core.jsxs.call(null,G__13728,G__13729));
})(),(function (){var G__13741 = "div";
var G__13742 = (function (){var obj13744 = ({"className":"flex-1 min-h-0 space-y-1.5 overflow-y-auto pr-1","children":((cljs.core.seq(filtered_jobs))?(function (){var iter__5628__auto__ = (function knoxx$frontend$admin$event_agents_panel$event_agents_panel_render_$_iter__13745(s__13746){
return (new cljs.core.LazySeq(null,(function (){
var s__13746__$1 = s__13746;
while(true){
var temp__5823__auto__ = cljs.core.seq(s__13746__$1);
if(temp__5823__auto__){
var s__13746__$2 = temp__5823__auto__;
if(cljs.core.chunked_seq_QMARK_(s__13746__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__13746__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__13748 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__13747 = (0);
while(true){
if((i__13747 < size__5627__auto__)){
var job = cljs.core._nth(c__5626__auto__,i__13747);
cljs.core.chunk_append(b__13748,(function (){var G__13749 = knoxx.frontend.admin.event_agent_sidebar.job_button;
var G__13750 = (function (){var obj13753 = ({"job":job,"runtime":knoxx.frontend.admin.event_agent_utils.runtime_for_job(runtime_jobs,job.id),"active":cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(job.id,selected_job_id),"on-select":((function (i__13747,G__13749,job,c__5626__auto__,size__5627__auto__,b__13748,s__13746__$2,temp__5823__auto__,G__13741,G__13704,G__13700,G__13696,G__13692,vec__13593,loading,set_loading,vec__13596,saving_token,set_saving_token,vec__13599,saving_control,set_saving_control,vec__13602,running_job_id,set_running_job_id,vec__13605,dispatching_event,set_dispatching_event,vec__13608,toggling_runtime,set_toggling_runtime,vec__13611,resetting_runtime,set_resetting_runtime,vec__13614,notice,set_notice,vec__13617,error,set_error,vec__13620,status,set_status,vec__13623,draft,set_draft,vec__13626,json_drafts,set_json_drafts,vec__13629,selected_job_id,set_selected_job_id,vec__13632,job_search,set_job_search,vec__13635,draft_token,set_draft_token,vec__13638,event_source_kind,set_event_source_kind,vec__13641,event_kind,set_event_kind,vec__13644,event_payload,set_event_payload,runtime_jobs,discord_source,recent_event_count,discord_runtime,seen_discord_channels,filtered_jobs,selected_job,selected_runtime,selected_job_json_draft,load_data,update_job,update_json_draft,parse_control_for_save,handle_save_token,handle_save_control,handle_run_job,handle_dispatch_event,handle_stop_runtime,handle_start_runtime,handle_reset_runtime,vec__13589,map__13592,map__13592__$1,can_manage,tools,on_selected_job_change){
return (function (){
var G__13754 = job.id;
return (set_selected_job_id.cljs$core$IFn$_invoke$arity$1 ? set_selected_job_id.cljs$core$IFn$_invoke$arity$1(G__13754) : set_selected_job_id.call(null,G__13754));
});})(i__13747,G__13749,job,c__5626__auto__,size__5627__auto__,b__13748,s__13746__$2,temp__5823__auto__,G__13741,G__13704,G__13700,G__13696,G__13692,vec__13593,loading,set_loading,vec__13596,saving_token,set_saving_token,vec__13599,saving_control,set_saving_control,vec__13602,running_job_id,set_running_job_id,vec__13605,dispatching_event,set_dispatching_event,vec__13608,toggling_runtime,set_toggling_runtime,vec__13611,resetting_runtime,set_resetting_runtime,vec__13614,notice,set_notice,vec__13617,error,set_error,vec__13620,status,set_status,vec__13623,draft,set_draft,vec__13626,json_drafts,set_json_drafts,vec__13629,selected_job_id,set_selected_job_id,vec__13632,job_search,set_job_search,vec__13635,draft_token,set_draft_token,vec__13638,event_source_kind,set_event_source_kind,vec__13641,event_kind,set_event_kind,vec__13644,event_payload,set_event_payload,runtime_jobs,discord_source,recent_event_count,discord_runtime,seen_discord_channels,filtered_jobs,selected_job,selected_runtime,selected_job_json_draft,load_data,update_job,update_json_draft,parse_control_for_save,handle_save_token,handle_save_control,handle_run_job,handle_dispatch_event,handle_stop_runtime,handle_start_runtime,handle_reset_runtime,vec__13589,map__13592,map__13592__$1,can_manage,tools,on_selected_job_change))
});
return obj13753;
})();
var G__13751 = job.id;
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$3 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$3(G__13749,G__13750,G__13751) : helix.core.jsx.call(null,G__13749,G__13750,G__13751));
})());

var G__13808 = (i__13747 + (1));
i__13747 = G__13808;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__13748),knoxx$frontend$admin$event_agents_panel$event_agents_panel_render_$_iter__13745(cljs.core.chunk_rest(s__13746__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__13748),null);
}
} else {
var job = cljs.core.first(s__13746__$2);
return cljs.core.cons((function (){var G__13755 = knoxx.frontend.admin.event_agent_sidebar.job_button;
var G__13756 = (function (){var obj13759 = ({"job":job,"runtime":knoxx.frontend.admin.event_agent_utils.runtime_for_job(runtime_jobs,job.id),"active":cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(job.id,selected_job_id),"on-select":((function (G__13755,job,s__13746__$2,temp__5823__auto__,G__13741,G__13704,G__13700,G__13696,G__13692,vec__13593,loading,set_loading,vec__13596,saving_token,set_saving_token,vec__13599,saving_control,set_saving_control,vec__13602,running_job_id,set_running_job_id,vec__13605,dispatching_event,set_dispatching_event,vec__13608,toggling_runtime,set_toggling_runtime,vec__13611,resetting_runtime,set_resetting_runtime,vec__13614,notice,set_notice,vec__13617,error,set_error,vec__13620,status,set_status,vec__13623,draft,set_draft,vec__13626,json_drafts,set_json_drafts,vec__13629,selected_job_id,set_selected_job_id,vec__13632,job_search,set_job_search,vec__13635,draft_token,set_draft_token,vec__13638,event_source_kind,set_event_source_kind,vec__13641,event_kind,set_event_kind,vec__13644,event_payload,set_event_payload,runtime_jobs,discord_source,recent_event_count,discord_runtime,seen_discord_channels,filtered_jobs,selected_job,selected_runtime,selected_job_json_draft,load_data,update_job,update_json_draft,parse_control_for_save,handle_save_token,handle_save_control,handle_run_job,handle_dispatch_event,handle_stop_runtime,handle_start_runtime,handle_reset_runtime,vec__13589,map__13592,map__13592__$1,can_manage,tools,on_selected_job_change){
return (function (){
var G__13760 = job.id;
return (set_selected_job_id.cljs$core$IFn$_invoke$arity$1 ? set_selected_job_id.cljs$core$IFn$_invoke$arity$1(G__13760) : set_selected_job_id.call(null,G__13760));
});})(G__13755,job,s__13746__$2,temp__5823__auto__,G__13741,G__13704,G__13700,G__13696,G__13692,vec__13593,loading,set_loading,vec__13596,saving_token,set_saving_token,vec__13599,saving_control,set_saving_control,vec__13602,running_job_id,set_running_job_id,vec__13605,dispatching_event,set_dispatching_event,vec__13608,toggling_runtime,set_toggling_runtime,vec__13611,resetting_runtime,set_resetting_runtime,vec__13614,notice,set_notice,vec__13617,error,set_error,vec__13620,status,set_status,vec__13623,draft,set_draft,vec__13626,json_drafts,set_json_drafts,vec__13629,selected_job_id,set_selected_job_id,vec__13632,job_search,set_job_search,vec__13635,draft_token,set_draft_token,vec__13638,event_source_kind,set_event_source_kind,vec__13641,event_kind,set_event_kind,vec__13644,event_payload,set_event_payload,runtime_jobs,discord_source,recent_event_count,discord_runtime,seen_discord_channels,filtered_jobs,selected_job,selected_runtime,selected_job_json_draft,load_data,update_job,update_json_draft,parse_control_for_save,handle_save_token,handle_save_control,handle_run_job,handle_dispatch_event,handle_stop_runtime,handle_start_runtime,handle_reset_runtime,vec__13589,map__13592,map__13592__$1,can_manage,tools,on_selected_job_change))
});
return obj13759;
})();
var G__13757 = job.id;
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$3 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$3(G__13755,G__13756,G__13757) : helix.core.jsx.call(null,G__13755,G__13756,G__13757));
})(),knoxx$frontend$admin$event_agents_panel$event_agents_panel_render_$_iter__13745(cljs.core.rest(s__13746__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(filtered_jobs);
})():(function (){var G__13761 = "div";
var G__13762 = (function (){var obj13764 = ({"className":"rounded-xl border border-dashed border-slate-800 px-3 py-6 text-center text-sm text-slate-500","children":"No event agents match this search."});
return obj13764;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13761,G__13762) : helix.core.jsx.call(null,G__13761,G__13762));
})())});
return obj13744;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13741,G__13742) : helix.core.jsx.call(null,G__13741,G__13742));
})()]});
return obj13707;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13704,G__13705) : helix.core.jsxs.call(null,G__13704,G__13705));
})(),(function (){var G__13765 = knoxx.frontend.admin.event_agents.main_content;
var G__13766 = (function (){var obj13768 = ({"on-run":handle_run_job,"selected-job-id":selected_job_id,"on-select-job":set_selected_job_id,"runtime":selected_runtime,"saving-control":saving_control,"on-update":update_job,"can-manage":can_manage,"jobs":filtered_jobs,"selected-job":selected_job,"runtime-jobs":runtime_jobs,"running-job-id":running_job_id});
return obj13768;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13765,G__13766) : helix.core.jsx.call(null,G__13765,G__13766));
})()]});
return obj13703;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13700,G__13701) : helix.core.jsxs.call(null,G__13700,G__13701));
})()});
return obj13699;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13696,G__13697) : helix.core.jsx.call(null,G__13696,G__13697));
})(),(function (){var G__13769 = knoxx.frontend.admin.event_agents_panel.error_banner;
var G__13770 = (function (){var obj13772 = ({"error":error});
return obj13772;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13769,G__13770) : helix.core.jsx.call(null,G__13769,G__13770));
})(),(function (){var G__13773 = knoxx.frontend.admin.event_agents_panel.notice_banner;
var G__13774 = (function (){var obj13776 = ({"notice":notice});
return obj13776;
})();
return (helix.core.jsx.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsx.cljs$core$IFn$_invoke$arity$2(G__13773,G__13774) : helix.core.jsx.call(null,G__13773,G__13774));
})()]});
return obj13695;
})();
return (helix.core.jsxs.cljs$core$IFn$_invoke$arity$2 ? helix.core.jsxs.cljs$core$IFn$_invoke$arity$2(G__13692,G__13693) : helix.core.jsxs.call(null,G__13692,G__13693));
}
}));
var G__13689 = [selected_job,on_selected_job_change];
return (helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2 ? helix.hooks.raw_use_effect.cljs$core$IFn$_invoke$arity$2(G__13688,G__13689) : helix.hooks.raw_use_effect.call(null,G__13688,G__13689));
});
if(goog.DEBUG === true){
var G__13777 = G__13588;
(G__13777.displayName = "knoxx.frontend.admin.event-agents-panel/event-agents-panel");

return G__13777;
} else {
return G__13588;
}
})();




//# sourceMappingURL=knoxx.frontend.admin.event_agents_panel.js.map
