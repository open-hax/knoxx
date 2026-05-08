import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.contracts.actor_scope');
knoxx.backend.contracts.actor_scope.wildcard_actor = new cljs.core.Keyword(null,"*","*",-1294732318);
knoxx.backend.contracts.actor_scope.legacy_chat_actor_id = "chat_primary";
knoxx.backend.contracts.actor_scope.normalize_actor_claim = (function knoxx$backend$contracts$actor_scope$normalize_actor_claim(value){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(value,knoxx.backend.contracts.actor_scope.wildcard_actor)){
return knoxx.backend.contracts.actor_scope.wildcard_actor;
} else {
if((value instanceof cljs.core.Keyword)){
var normalized = (function (){var G__516867 = value;
var G__516867__$1 = (((G__516867 == null))?null:cljs.core.name(G__516867));
var G__516867__$2 = (((G__516867__$1 == null))?null:clojure.string.trim(G__516867__$1));
if((G__516867__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__516867__$2);
}
})();
if(cljs.core.truth_(normalized)){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized,"*")){
return knoxx.backend.contracts.actor_scope.wildcard_actor;
} else {
return normalized;
}
} else {
return null;
}
} else {
if(typeof value === 'string'){
var normalized = (function (){var G__516874 = value;
var G__516874__$1 = (((G__516874 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__516874)));
var G__516874__$2 = (((G__516874__$1 == null))?null:clojure.string.trim(G__516874__$1));
if((G__516874__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__516874__$2);
}
})();
if(cljs.core.truth_(normalized)){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized,"*")){
return knoxx.backend.contracts.actor_scope.wildcard_actor;
} else {
return normalized;
}
} else {
return null;
}
} else {
if((value == null)){
return null;
} else {
var G__516875 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value));
return (knoxx.backend.contracts.actor_scope.normalize_actor_claim.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.contracts.actor_scope.normalize_actor_claim.cljs$core$IFn$_invoke$arity$1(G__516875) : knoxx.backend.contracts.actor_scope.normalize_actor_claim.call(null,G__516875));

}
}
}
}
});
knoxx.backend.contracts.actor_scope.normalize_actor_claims = (function knoxx$backend$contracts$actor_scope$normalize_actor_claims(value){
var items = ((cljs.core.set_QMARK_(value))?value:((cljs.core.sequential_QMARK_(value))?value:(((value == null))?cljs.core.PersistentVector.EMPTY:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null)
)));
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1(knoxx.backend.contracts.actor_scope.normalize_actor_claim),items);
});
knoxx.backend.contracts.actor_scope.normalized_contract_actors = (function knoxx$backend$contracts$actor_scope$normalized_contract_actors(var_args){
var G__516884 = arguments.length;
switch (G__516884) {
case 1:
return knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$1 = (function (contract){
return knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$2(contract,null);
}));

(knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$2 = (function (contract,missing_fallback_actor_id){
var declared = knoxx.backend.contracts.actor_scope.normalize_actor_claims(new cljs.core.Keyword("contract","actors","contract/actors",-1138019932).cljs$core$IFn$_invoke$arity$1(contract));
var legacy = knoxx.backend.contracts.actor_scope.normalize_actor_claim(new cljs.core.Keyword("contract","actor","contract/actor",1959324173).cljs$core$IFn$_invoke$arity$1(contract));
var merged = (function (){var G__516887 = declared;
var G__516887__$1 = (cljs.core.truth_(legacy)?cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__516887,legacy):G__516887);
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2("knoxx_default",(function (){var G__516889 = new cljs.core.Keyword("contract","id","contract/id",-872298206).cljs$core$IFn$_invoke$arity$1(contract);
var G__516889__$1 = (((G__516889 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__516889)));
if((G__516889__$1 == null)){
return null;
} else {
return clojure.string.trim(G__516889__$1);
}
})())){
return cljs.core.conj.cljs$core$IFn$_invoke$arity$2(G__516887__$1,knoxx.backend.contracts.actor_scope.wildcard_actor);
} else {
return G__516887__$1;
}
})();
if(cljs.core.seq(merged)){
return merged;
} else {
var fallback = knoxx.backend.contracts.actor_scope.normalize_actor_claim(missing_fallback_actor_id);
if(cljs.core.truth_(fallback)){
return cljs.core.PersistentHashSet.createAsIfByAssoc([fallback]);
} else {
return cljs.core.PersistentHashSet.EMPTY;
}

}
}));

(knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$lang$maxFixedArity = 2);

knoxx.backend.contracts.actor_scope.agent_role_claims = (function knoxx$backend$contracts$actor_scope$agent_role_claims(contract){
var legacy_roles = (function (){var or__5142__auto__ = new cljs.core.Keyword("actor","roles","actor/roles",186081855).cljs$core$IFn$_invoke$arity$1(contract);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var agent_roles = (function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"roles","roles",143379530)], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})();
var agent_role = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent","agent",-766455027),new cljs.core.Keyword(null,"role","role",-736691072)], null));
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(legacy_roles,agent_roles,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([((cljs.core.sequential_QMARK_(agent_role))?agent_role:(cljs.core.truth_(agent_role)?new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [agent_role], null):cljs.core.PersistentVector.EMPTY
))], 0))));
});
knoxx.backend.contracts.actor_scope.normalize_agent_contract = (function knoxx$backend$contracts$actor_scope$normalize_agent_contract(contract){
if((!(cljs.core.map_QMARK_(contract)))){
return contract;
} else {
var actors = knoxx.backend.contracts.actor_scope.normalized_contract_actors.cljs$core$IFn$_invoke$arity$1(contract);
var G__516904 = cljs.core.dissoc.cljs$core$IFn$_invoke$arity$2(contract,new cljs.core.Keyword("contract","actor","contract/actor",1959324173));
if(cljs.core.seq(actors)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__516904,new cljs.core.Keyword("contract","actors","contract/actors",-1138019932),actors);
} else {
return G__516904;
}
}
});
knoxx.backend.contracts.actor_scope.actor_allowed_QMARK_ = (function knoxx$backend$contracts$actor_scope$actor_allowed_QMARK_(actors,actor_id){
var claims = knoxx.backend.contracts.actor_scope.normalize_actor_claims(actors);
var wanted = knoxx.backend.contracts.actor_scope.normalize_actor_claim(actor_id);
var or__5142__auto__ = cljs.core.contains_QMARK_(claims,knoxx.backend.contracts.actor_scope.wildcard_actor);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var and__5140__auto__ = wanted;
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.contains_QMARK_(claims,wanted);
} else {
return and__5140__auto__;
}
}
});
knoxx.backend.contracts.actor_scope.effective_actor_id = (function knoxx$backend$contracts$actor_scope$effective_actor_id(actors,requested_actor_id,default_actor_id){
var claims = knoxx.backend.contracts.actor_scope.normalize_actor_claims(actors);
var requested = knoxx.backend.contracts.actor_scope.normalize_actor_claim(requested_actor_id);
var fallback = knoxx.backend.contracts.actor_scope.normalize_actor_claim(default_actor_id);
var concrete_claims = cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.PersistentHashSet.createAsIfByAssoc([knoxx.backend.contracts.actor_scope.wildcard_actor]),claims)));
if(cljs.core.truth_((function (){var and__5140__auto__ = requested;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.contracts.actor_scope.actor_allowed_QMARK_(claims,requested);
} else {
return and__5140__auto__;
}
})())){
return requested;
} else {
if(cljs.core.truth_((function (){var and__5140__auto__ = fallback;
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.contracts.actor_scope.actor_allowed_QMARK_(claims,fallback);
} else {
return and__5140__auto__;
}
})())){
return fallback;
} else {
if(cljs.core.seq(concrete_claims)){
return cljs.core.first(concrete_claims);
} else {
return fallback;

}
}
}
});
knoxx.backend.contracts.actor_scope.actor_claims__GT_wire = (function knoxx$backend$contracts$actor_scope$actor_claims__GT_wire(actors){
return cljs.core.vec(cljs.core.sort.cljs$core$IFn$_invoke$arity$1(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (claim){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(claim,knoxx.backend.contracts.actor_scope.wildcard_actor)){
return "*";
} else {
return claim;
}
}),knoxx.backend.contracts.actor_scope.normalize_actor_claims(actors))));
});

//# sourceMappingURL=knoxx.backend.contracts.actor_scope.js.map
