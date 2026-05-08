import "./cljs_env.js";
import "./cljs.core.js";
import "./shadow.cljs.modern.js";
import "./knoxx.backend.actions.loader.js";
import "./knoxx.backend.actions.contract.js";
import "./knoxx.backend.actions.registry.js";
import "./knoxx.backend.actions.invoke_agent.js";
import "./knoxx.backend.actions.invoke_sub_agent.js";
goog.provide('knoxx.backend.actions.dispatch');
knoxx.backend.actions.dispatch.dispatch_BANG_ = (function knoxx$backend$actions$dispatch$dispatch_BANG_(ctx,step_spec){
return knoxx.backend.actions.loader.resolve_action_BANG_(new cljs.core.Keyword(null,"config","config",994861415).cljs$core$IFn$_invoke$arity$1(ctx),step_spec).then((function (action){
if(cljs.core.truth_((knoxx.backend.actions.contract.validate_action.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.actions.contract.validate_action.cljs$core$IFn$_invoke$arity$1(action) : knoxx.backend.actions.contract.validate_action.call(null,action)))){
} else {
throw cljs.core.ex_info.cljs$core$IFn$_invoke$arity$2("Invalid ActionContract",new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"explain","explain",484226146),(knoxx.backend.actions.contract.explain_action.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.actions.contract.explain_action.cljs$core$IFn$_invoke$arity$1(action) : knoxx.backend.actions.contract.explain_action.call(null,action)),new cljs.core.Keyword(null,"value","value",305978217),action], null));
}

return knoxx.backend.actions.registry.run_action_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,action);
}));
});

//# sourceMappingURL=knoxx.backend.actions.dispatch.js.map
