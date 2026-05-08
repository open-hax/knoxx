import "./cljs_env.js";
import "./cljs.core.js";
import "./open_hax.contracts.schema.js";
goog.provide('knoxx.backend.policy.protocol');
knoxx.backend.policy.protocol.validate_contract_BANG_ = (function knoxx$backend$policy$protocol$validate_contract_BANG_(contract_class,value){
return open_hax.contracts.schema.assert_BANG_(contract_class,value);
});
knoxx.backend.policy.protocol.validate_actor_BANG_ = (function knoxx$backend$policy$protocol$validate_actor_BANG_(actor){
return knoxx.backend.policy.protocol.validate_contract_BANG_(new cljs.core.Keyword(null,"actor","actor",-1830560481),actor);
});

/**
 * @interface
 */
knoxx.backend.policy.protocol.PolicyStore = function(){};

var knoxx$backend$policy$protocol$PolicyStore$list_contracts$dyn_525209 = (function (store,contract_class){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.list_contracts[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(store,contract_class) : m__5499__auto__.call(null,store,contract_class));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.list_contracts["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(store,contract_class) : m__5497__auto__.call(null,store,contract_class));
} else {
throw cljs.core.missing_protocol("PolicyStore.list-contracts",store);
}
}
});
/**
 * Return a promise or value containing contract-shaped maps for contract-class.
 */
knoxx.backend.policy.protocol.list_contracts = (function knoxx$backend$policy$protocol$list_contracts(store,contract_class){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$list_contracts$arity$2 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$list_contracts$arity$2(store,contract_class);
} else {
return knoxx$backend$policy$protocol$PolicyStore$list_contracts$dyn_525209(store,contract_class);
}
});

var knoxx$backend$policy$protocol$PolicyStore$get_contract$dyn_525210 = (function (store,contract_class,contract_id){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.get_contract[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(store,contract_class,contract_id) : m__5499__auto__.call(null,store,contract_class,contract_id));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.get_contract["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(store,contract_class,contract_id) : m__5497__auto__.call(null,store,contract_class,contract_id));
} else {
throw cljs.core.missing_protocol("PolicyStore.get-contract",store);
}
}
});
/**
 * Return one contract-shaped map by class/id, or nil.
 */
knoxx.backend.policy.protocol.get_contract = (function knoxx$backend$policy$protocol$get_contract(store,contract_class,contract_id){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$get_contract$arity$3 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$get_contract$arity$3(store,contract_class,contract_id);
} else {
return knoxx$backend$policy$protocol$PolicyStore$get_contract$dyn_525210(store,contract_class,contract_id);
}
});

var knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$dyn_525212 = (function (store,contract_class,contract){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.upsert_contract_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(store,contract_class,contract) : m__5499__auto__.call(null,store,contract_class,contract));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.upsert_contract_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(store,contract_class,contract) : m__5497__auto__.call(null,store,contract_class,contract));
} else {
throw cljs.core.missing_protocol("PolicyStore.upsert-contract!",store);
}
}
});
/**
 * Validate and persist a contract-shaped map.
 */
knoxx.backend.policy.protocol.upsert_contract_BANG_ = (function knoxx$backend$policy$protocol$upsert_contract_BANG_(store,contract_class,contract){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$arity$3 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$arity$3(store,contract_class,contract);
} else {
return knoxx$backend$policy$protocol$PolicyStore$upsert_contract_BANG_$dyn_525212(store,contract_class,contract);
}
});

var knoxx$backend$policy$protocol$PolicyStore$list_actors$dyn_525229 = (function (store){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.list_actors[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$1(store) : m__5499__auto__.call(null,store));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.list_actors["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$1 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$1(store) : m__5497__auto__.call(null,store));
} else {
throw cljs.core.missing_protocol("PolicyStore.list-actors",store);
}
}
});
/**
 * Return actor contract maps validated by the shared contract schema.
 */
knoxx.backend.policy.protocol.list_actors = (function knoxx$backend$policy$protocol$list_actors(store){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$list_actors$arity$1 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$list_actors$arity$1(store);
} else {
return knoxx$backend$policy$protocol$PolicyStore$list_actors$dyn_525229(store);
}
});

var knoxx$backend$policy$protocol$PolicyStore$get_actor$dyn_525234 = (function (store,actor_id){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.get_actor[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(store,actor_id) : m__5499__auto__.call(null,store,actor_id));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.get_actor["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(store,actor_id) : m__5497__auto__.call(null,store,actor_id));
} else {
throw cljs.core.missing_protocol("PolicyStore.get-actor",store);
}
}
});
/**
 * Return one actor contract map validated by the shared contract schema, or nil.
 */
knoxx.backend.policy.protocol.get_actor = (function knoxx$backend$policy$protocol$get_actor(store,actor_id){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$get_actor$arity$2 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$get_actor$arity$2(store,actor_id);
} else {
return knoxx$backend$policy$protocol$PolicyStore$get_actor$dyn_525234(store,actor_id);
}
});

var knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$dyn_525239 = (function (store,actor){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.upsert_actor_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(store,actor) : m__5499__auto__.call(null,store,actor));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.upsert_actor_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(store,actor) : m__5497__auto__.call(null,store,actor));
} else {
throw cljs.core.missing_protocol("PolicyStore.upsert-actor!",store);
}
}
});
/**
 * Validate and persist one actor contract map.
 */
knoxx.backend.policy.protocol.upsert_actor_BANG_ = (function knoxx$backend$policy$protocol$upsert_actor_BANG_(store,actor){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$arity$2 == null)))))){
return store.knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$arity$2(store,actor);
} else {
return knoxx$backend$policy$protocol$PolicyStore$upsert_actor_BANG_$dyn_525239(store,actor);
}
});


/**
 * @interface
 */
knoxx.backend.policy.protocol.ActorCredentialStore = function(){};

var knoxx$backend$policy$protocol$ActorCredentialStore$list_actor_credentials$dyn_525244 = (function (store,provider){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.list_actor_credentials[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(store,provider) : m__5499__auto__.call(null,store,provider));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.list_actor_credentials["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(store,provider) : m__5497__auto__.call(null,store,provider));
} else {
throw cljs.core.missing_protocol("ActorCredentialStore.list-actor-credentials",store);
}
}
});
/**
 * Return active actor credential state rows for provider. Implementations must not expose this through public contract APIs.
 */
knoxx.backend.policy.protocol.list_actor_credentials = (function knoxx$backend$policy$protocol$list_actor_credentials(store,provider){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$ActorCredentialStore$list_actor_credentials$arity$2 == null)))))){
return store.knoxx$backend$policy$protocol$ActorCredentialStore$list_actor_credentials$arity$2(store,provider);
} else {
return knoxx$backend$policy$protocol$ActorCredentialStore$list_actor_credentials$dyn_525244(store,provider);
}
});

var knoxx$backend$policy$protocol$ActorCredentialStore$get_actor_credential$dyn_525248 = (function (store,actor_id,provider){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.get_actor_credential[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$3(store,actor_id,provider) : m__5499__auto__.call(null,store,actor_id,provider));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.get_actor_credential["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$3 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$3(store,actor_id,provider) : m__5497__auto__.call(null,store,actor_id,provider));
} else {
throw cljs.core.missing_protocol("ActorCredentialStore.get-actor-credential",store);
}
}
});
/**
 * Return actor credential state for provider. Implementations must not expose this through public contract APIs.
 */
knoxx.backend.policy.protocol.get_actor_credential = (function knoxx$backend$policy$protocol$get_actor_credential(store,actor_id,provider){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$ActorCredentialStore$get_actor_credential$arity$3 == null)))))){
return store.knoxx$backend$policy$protocol$ActorCredentialStore$get_actor_credential$arity$3(store,actor_id,provider);
} else {
return knoxx$backend$policy$protocol$ActorCredentialStore$get_actor_credential$dyn_525248(store,actor_id,provider);
}
});

var knoxx$backend$policy$protocol$ActorCredentialStore$upsert_actor_credential_BANG_$dyn_525264 = (function (store,actor_id,provider,credential){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.upsert_actor_credential_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$4 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$4(store,actor_id,provider,credential) : m__5499__auto__.call(null,store,actor_id,provider,credential));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.upsert_actor_credential_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$4 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$4(store,actor_id,provider,credential) : m__5497__auto__.call(null,store,actor_id,provider,credential));
} else {
throw cljs.core.missing_protocol("ActorCredentialStore.upsert-actor-credential!",store);
}
}
});
/**
 * Persist mutable actor credential state.
 */
knoxx.backend.policy.protocol.upsert_actor_credential_BANG_ = (function knoxx$backend$policy$protocol$upsert_actor_credential_BANG_(store,actor_id,provider,credential){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$ActorCredentialStore$upsert_actor_credential_BANG_$arity$4 == null)))))){
return store.knoxx$backend$policy$protocol$ActorCredentialStore$upsert_actor_credential_BANG_$arity$4(store,actor_id,provider,credential);
} else {
return knoxx$backend$policy$protocol$ActorCredentialStore$upsert_actor_credential_BANG_$dyn_525264(store,actor_id,provider,credential);
}
});


/**
 * @interface
 */
knoxx.backend.policy.protocol.ActorProjectionStore = function(){};

var knoxx$backend$policy$protocol$ActorProjectionStore$sync_actor_projections_BANG_$dyn_525278 = (function (store,actors){
var x__5498__auto__ = (((store == null))?null:store);
var m__5499__auto__ = (knoxx.backend.policy.protocol.sync_actor_projections_BANG_[goog.typeOf(x__5498__auto__)]);
if((!((m__5499__auto__ == null)))){
return (m__5499__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5499__auto__.cljs$core$IFn$_invoke$arity$2(store,actors) : m__5499__auto__.call(null,store,actors));
} else {
var m__5497__auto__ = (knoxx.backend.policy.protocol.sync_actor_projections_BANG_["_"]);
if((!((m__5497__auto__ == null)))){
return (m__5497__auto__.cljs$core$IFn$_invoke$arity$2 ? m__5497__auto__.cljs$core$IFn$_invoke$arity$2(store,actors) : m__5497__auto__.call(null,store,actors));
} else {
throw cljs.core.missing_protocol("ActorProjectionStore.sync-actor-projections!",store);
}
}
});
/**
 * Sync canonical actor contracts into projection storage such as SQL memberships/users.
 */
knoxx.backend.policy.protocol.sync_actor_projections_BANG_ = (function knoxx$backend$policy$protocol$sync_actor_projections_BANG_(store,actors){
if((((!((store == null)))) && ((!((store.knoxx$backend$policy$protocol$ActorProjectionStore$sync_actor_projections_BANG_$arity$2 == null)))))){
return store.knoxx$backend$policy$protocol$ActorProjectionStore$sync_actor_projections_BANG_$arity$2(store,actors);
} else {
return knoxx$backend$policy$protocol$ActorProjectionStore$sync_actor_projections_BANG_$dyn_525278(store,actors);
}
});


//# sourceMappingURL=knoxx.backend.policy.protocol.js.map
