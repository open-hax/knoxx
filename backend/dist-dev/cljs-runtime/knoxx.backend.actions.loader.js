import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.actions.loader');
knoxx.backend.actions.loader.well_known_actions = new cljs.core.PersistentArrayMap(null, 2, ["run-agent",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("action","id","action/id",241708030),"run-agent",new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","agent","invoke/agent",467801765),new cljs.core.Keyword("action","label","action/label",827483238),"Run Knoxx Agent"], null),"invoke-sub-agent",new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("action","id","action/id",241708030),"invoke-sub-agent",new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","sub-agent","invoke/sub-agent",-1071333507),new cljs.core.Keyword("action","label","action/label",827483238),"Invoke Sub-Agent"], null)], null);
knoxx.backend.actions.loader.resolve_action_BANG_ = (function knoxx$backend$actions$loader$resolve_action_BANG_(_config,p__523311){
var map__523316 = p__523311;
var map__523316__$1 = cljs.core.__destructure_map(map__523316);
var _step_spec = map__523316__$1;
var uses = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523316__$1,new cljs.core.Keyword(null,"uses","uses",232664692));
var with$ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__523316__$1,new cljs.core.Keyword(null,"with","with",-1536296876));
var base = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(knoxx.backend.actions.loader.well_known_actions,uses);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword("action","id","action/id",241708030),uses,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","noop","invoke/noop",-2130054978),new cljs.core.Keyword("action","label","action/label",827483238),(""+"Unknown action: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(uses))], null);
}
})();
return Promise.resolve((cljs.core.truth_(with$)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(base,new cljs.core.Keyword("action","with","action/with",-243371526),cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword("action","with","action/with",-243371526).cljs$core$IFn$_invoke$arity$2(base,cljs.core.PersistentArrayMap.EMPTY),with$], 0))):base));
});

//# sourceMappingURL=knoxx.backend.actions.loader.js.map
