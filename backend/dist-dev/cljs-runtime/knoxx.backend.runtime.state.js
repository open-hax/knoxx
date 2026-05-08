import "./cljs_env.js";
import "./cljs.core.js";
goog.provide('knoxx.backend.runtime.state');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.runtime !== 'undefined') && (typeof knoxx.backend.runtime.state !== 'undefined') && (typeof knoxx.backend.runtime.state.config_STAR_ !== 'undefined')){
} else {
knoxx.backend.runtime.state.config_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.runtime !== 'undefined') && (typeof knoxx.backend.runtime.state !== 'undefined') && (typeof knoxx.backend.runtime.state.runtime_STAR_ !== 'undefined')){
} else {
knoxx.backend.runtime.state.runtime_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.runtime !== 'undefined') && (typeof knoxx.backend.runtime.state !== 'undefined') && (typeof knoxx.backend.runtime.state.policy_db_STAR_ !== 'undefined')){
} else {
knoxx.backend.runtime.state.policy_db_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.runtime.state.remember_context_BANG_ = (function knoxx$backend$runtime$state$remember_context_BANG_(runtime,config,policy_db){
cljs.core.reset_BANG_(knoxx.backend.runtime.state.runtime_STAR_,runtime);

cljs.core.reset_BANG_(knoxx.backend.runtime.state.config_STAR_,config);

cljs.core.reset_BANG_(knoxx.backend.runtime.state.policy_db_STAR_,policy_db);

return true;
});
knoxx.backend.runtime.state.current_policy_db = (function knoxx$backend$runtime$state$current_policy_db(){
return cljs.core.deref(knoxx.backend.runtime.state.policy_db_STAR_);
});

//# sourceMappingURL=knoxx.backend.runtime.state.js.map
