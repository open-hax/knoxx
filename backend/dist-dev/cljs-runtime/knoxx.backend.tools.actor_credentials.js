import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.agent_context.js";
import "./knoxx.backend.authz.js";
goog.provide('knoxx.backend.tools.actor_credentials');
knoxx.backend.tools.actor_credentials.current_actor_id = (function knoxx$backend$tools$actor_credentials$current_actor_id(){
var ctx = (function (){var or__5142__auto__ = knoxx.backend.agent_context.get_context();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var spec = new cljs.core.Keyword(null,"agent-spec","agent-spec",1796895541).cljs$core$IFn$_invoke$arity$1(ctx);
var G__58760 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(spec);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(ctx);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(ctx);
}
}
}
}
}
})();
var G__58760__$1 = (((G__58760 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58760)));
var G__58760__$2 = (((G__58760__$1 == null))?null:clojure.string.trim(G__58760__$1));
if((G__58760__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__58760__$2);
}
});
knoxx.backend.tools.actor_credentials.normalize_credential = (function knoxx$backend$tools$actor_credentials$normalize_credential(payload){
var credential = (payload["credential"]);
if(cljs.core.truth_(credential)){
return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(credential,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
} else {
return null;
}
});
knoxx.backend.tools.actor_credentials.get_credential_BANG_ = (function knoxx$backend$tools$actor_credentials$get_credential_BANG_(runtime,provider){
var actor_id = knoxx.backend.tools.actor_credentials.current_actor_id();
var db = knoxx.backend.authz.policy_db(runtime);
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))){
return Promise.reject((new Error((""+"No current actor_id is available for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider)+" credentials. Start the agent with an actor_id and configure it in Admin \u2192 Actors."))));
} else {
if((db == null)){
return Promise.reject((new Error("Actor credentials require the Knoxx policy database.")));
} else {
return db.getActorCredential(actor_id,provider).then((function (result){
var temp__5823__auto__ = knoxx.backend.tools.actor_credentials.normalize_credential(result);
if(cljs.core.truth_(temp__5823__auto__)){
var credential = temp__5823__auto__;
return credential;
} else {
throw (new Error((""+"No active "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(provider)+" credentials configured for actor "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)+". Configure them in Admin \u2192 Actors.")));
}
}));

}
}
});
knoxx.backend.tools.actor_credentials.secret_value = (function knoxx$backend$tools$actor_credentials$secret_value(var_args){
var args__5882__auto__ = [];
var len__5876__auto___58846 = arguments.length;
var i__5877__auto___58848 = (0);
while(true){
if((i__5877__auto___58848 < len__5876__auto___58846)){
args__5882__auto__.push((arguments[i__5877__auto___58848]));

var G__58857 = (i__5877__auto___58848 + (1));
i__5877__auto___58848 = G__58857;
continue;
} else {
}
break;
}

var argseq__5883__auto__ = ((((1) < args__5882__auto__.length))?(new cljs.core.IndexedSeq(args__5882__auto__.slice((1)),(0),null)):null);
return knoxx.backend.tools.actor_credentials.secret_value.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__5883__auto__);
});

(knoxx.backend.tools.actor_credentials.secret_value.cljs$core$IFn$_invoke$arity$variadic = (function (credential,keys){
var secrets = new cljs.core.Keyword(null,"secretJson","secretJson",1807839704).cljs$core$IFn$_invoke$arity$1(credential);
return cljs.core.some((function (k){
var G__58823 = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(secrets,k);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(secrets,cljs.core.keyword.cljs$core$IFn$_invoke$arity$1(k));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(secrets,cljs.core.name(k));
}
}
})();
var G__58823__$1 = (((G__58823 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58823)));
var G__58823__$2 = (((G__58823__$1 == null))?null:clojure.string.trim(G__58823__$1));
if((G__58823__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__58823__$2);
}
}),keys);
}));

(knoxx.backend.tools.actor_credentials.secret_value.cljs$lang$maxFixedArity = (1));

/** @this {Function} */
(knoxx.backend.tools.actor_credentials.secret_value.cljs$lang$applyTo = (function (seq58788){
var G__58791 = cljs.core.first(seq58788);
var seq58788__$1 = cljs.core.next(seq58788);
var self__5861__auto__ = this;
return self__5861__auto__.cljs$core$IFn$_invoke$arity$variadic(G__58791,seq58788__$1);
}));


//# sourceMappingURL=knoxx.backend.tools.actor_credentials.js.map
