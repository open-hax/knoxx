import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.actions.registry');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.actions !== 'undefined') && (typeof knoxx.backend.actions.registry !== 'undefined') && (typeof knoxx.backend.actions.registry.run_action_BANG_ !== 'undefined')){
} else {
/**
 * Dispatch an action map by :action/kind.
 */
knoxx.backend.actions.registry.run_action_BANG_ = (function (){var method_table__5747__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__5748__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var method_cache__5749__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__5750__auto__ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__5751__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"hierarchy","hierarchy",-1053470341),(function (){var fexpr__523422 = cljs.core.get_global_hierarchy;
return (fexpr__523422.cljs$core$IFn$_invoke$arity$0 ? fexpr__523422.cljs$core$IFn$_invoke$arity$0() : fexpr__523422.call(null));
})());
return (new cljs.core.MultiFn(cljs.core.symbol.cljs$core$IFn$_invoke$arity$2("knoxx.backend.actions.registry","run-action!"),(function (_ctx,action){
return new cljs.core.Keyword("action","kind","action/kind",-2113018193).cljs$core$IFn$_invoke$arity$1(action);
}),new cljs.core.Keyword(null,"default","default",-1987822328),hierarchy__5751__auto__,method_table__5747__auto__,prefer_table__5748__auto__,method_cache__5749__auto__,cached_hierarchy__5750__auto__));
})();
}
knoxx.backend.actions.registry.run_action_BANG_.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword("invoke","noop","invoke/noop",-2130054978),(function (_,___$1){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword("action","kind","action/kind",-2113018193),new cljs.core.Keyword("invoke","noop","invoke/noop",-2130054978)], null));
}));
knoxx.backend.actions.registry.run_action_BANG_.cljs$core$IMultiFn$_add_method$arity$3(null,new cljs.core.Keyword(null,"default","default",-1987822328),(function (_ctx,action){
console.warn("[knoxx/actions] unknown action/kind",cljs.core.pr_str.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword("action","kind","action/kind",-2113018193).cljs$core$IFn$_invoke$arity$1(action)], 0)));

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),false,new cljs.core.Keyword(null,"error","error",-978969032),"unknown action/kind"], null));
}));

//# sourceMappingURL=knoxx.backend.actions.registry.js.map
