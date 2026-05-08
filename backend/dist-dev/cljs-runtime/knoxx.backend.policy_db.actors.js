import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.policy.edn_adapter.js";
import "./knoxx.backend.policy.protocol.js";
import "./knoxx.backend.policy.sql_adapter.js";
goog.provide('knoxx.backend.policy_db.actors');
knoxx.backend.policy_db.actors.normalize_actor_id = (function knoxx$backend$policy_db$actors$normalize_actor_id(value){
var G__35517 = value;
var G__35517__$1 = (((G__35517 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35517)));
var G__35517__$2 = (((G__35517__$1 == null))?null:clojure.string.trim(G__35517__$1));
if((G__35517__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35517__$2);
}
});
knoxx.backend.policy_db.actors.normalize_email = (function knoxx$backend$policy_db$actors$normalize_email(value){
var G__35520 = value;
var G__35520__$1 = (((G__35520 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35520)));
var G__35520__$2 = (((G__35520__$1 == null))?null:clojure.string.trim(G__35520__$1));
var G__35520__$3 = (((G__35520__$2 == null))?null:clojure.string.lower_case(G__35520__$2));
if((G__35520__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__35520__$3);
}
});
knoxx.backend.policy_db.actors.user_actor_id_from_email = (function knoxx$backend$policy_db$actors$user_actor_id_from_email(email){
var G__35521 = email;
var G__35521__$1 = (((G__35521 == null))?null:knoxx.backend.policy_db.actors.normalize_email(G__35521));
var G__35521__$2 = (((G__35521__$1 == null))?null:clojure.string.replace(G__35521__$1,/[^a-z0-9]+/,"_"));
var G__35521__$3 = (((G__35521__$2 == null))?null:clojure.string.replace(G__35521__$2,/^_+|_+$/,""));
if((G__35521__$3 == null)){
return null;
} else {
return cljs.core.not_empty(G__35521__$3);
}
});
knoxx.backend.policy_db.actors.actor_email_from_id = (function knoxx$backend$policy_db$actors$actor_email_from_id(actor_id){
var slug = clojure.string.replace(clojure.string.replace(clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor_id)))),/[^a-z0-9._+-]+/,"-"),/^[-.]+|[-.]+$/,"");
if(clojure.string.blank_QMARK_(slug)){
return null;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(slug)+"@actors.local");
}
});
knoxx.backend.policy_db.actors.edn_store = (function knoxx$backend$policy_db$actors$edn_store(contracts_dir){
return knoxx.backend.policy.edn_adapter.create_store(contracts_dir);
});
knoxx.backend.policy_db.actors.actor_contract_file_path = (function knoxx$backend$policy_db$actors$actor_contract_file_path(contracts_dir,actor_id){
return knoxx.backend.policy.edn_adapter.actor_contract_file_path(knoxx.backend.policy_db.actors.edn_store(contracts_dir),actor_id);
});
knoxx.backend.policy_db.actors.actor_summary = (function knoxx$backend$policy_db$actors$actor_summary(actor){
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword("actor","id","actor/id",-1462607809).cljs$core$IFn$_invoke$arity$1(actor),new cljs.core.Keyword(null,"kind","kind",-717265803),(function (){var or__5142__auto__ = new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agent","agent",-766455027);
}
})(),new cljs.core.Keyword(null,"email","email",1415816706),knoxx.backend.policy_db.actors.normalize_email((function (){var or__5142__auto__ = new cljs.core.Keyword("actor","email","actor/email",1189986301).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(actor))){
return new cljs.core.Keyword("actor","username","actor/username",2032890997).cljs$core$IFn$_invoke$arity$1(actor);
} else {
return null;
}
}
})()),new cljs.core.Keyword(null,"username","username",1605666410),(function (){var G__35544 = new cljs.core.Keyword("actor","username","actor/username",2032890997).cljs$core$IFn$_invoke$arity$1(actor);
var G__35544__$1 = (((G__35544 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35544)));
var G__35544__$2 = (((G__35544__$1 == null))?null:clojure.string.trim(G__35544__$1));
if((G__35544__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35544__$2);
}
})(),new cljs.core.Keyword(null,"org","org",1495985),(function (){var G__35546 = new cljs.core.Keyword("actor","org","actor/org",175993262).cljs$core$IFn$_invoke$arity$1(actor);
var G__35546__$1 = (((G__35546 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35546)));
var G__35546__$2 = (((G__35546__$1 == null))?null:clojure.string.trim(G__35546__$1));
if((G__35546__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35546__$2);
}
})(),new cljs.core.Keyword(null,"label","label",1718410804),(function (){var G__35550 = new cljs.core.Keyword("actor","label","actor/label",-1796720603).cljs$core$IFn$_invoke$arity$1(actor);
var G__35550__$1 = (((G__35550 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35550)));
var G__35550__$2 = (((G__35550__$1 == null))?null:clojure.string.trim(G__35550__$1));
if((G__35550__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35550__$2);
}
})(),new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158),cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (role){
if((role instanceof cljs.core.Keyword)){
return clojure.string.replace(cljs.core.name(role),/-/,"_");
} else {
if(typeof role === 'string'){
return clojure.string.replace(clojure.string.trim(role),/-/,"_");
} else {
return null;

}
}
}),(function (){var or__5142__auto__ = new cljs.core.Keyword("actor","roles","actor/roles",186081855).cljs$core$IFn$_invoke$arity$1(actor);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())))),new cljs.core.Keyword(null,"actor","actor",-1830560481),actor], null);
});
knoxx.backend.policy_db.actors.list_actor_contracts = (function knoxx$backend$policy_db$actors$list_actor_contracts(contracts_dir){
return cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(knoxx.backend.policy_db.actors.actor_summary,knoxx.backend.policy.protocol.list_actors(knoxx.backend.policy_db.actors.edn_store(contracts_dir)));
});
knoxx.backend.policy_db.actors.find_actor_contract_by_id = (function knoxx$backend$policy_db$actors$find_actor_contract_by_id(contracts_dir,actor_id){
var G__35570 = knoxx.backend.policy.protocol.get_actor(knoxx.backend.policy_db.actors.edn_store(contracts_dir),actor_id);
if((G__35570 == null)){
return null;
} else {
return knoxx.backend.policy_db.actors.actor_summary(G__35570);
}
});
knoxx.backend.policy_db.actors.find_user_actor_contract_by_email = (function knoxx$backend$policy_db$actors$find_user_actor_contract_by_email(contracts_dir,email){
var temp__5825__auto__ = knoxx.backend.policy_db.actors.normalize_email(email);
if(cljs.core.truth_(temp__5825__auto__)){
var normalized_email = temp__5825__auto__;
return cljs.core.first(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__35579_SHARP_){
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"user","user",1532431356),new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(p1__35579_SHARP_))) && (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized_email,new cljs.core.Keyword(null,"email","email",1415816706).cljs$core$IFn$_invoke$arity$1(p1__35579_SHARP_))));
}),knoxx.backend.policy_db.actors.list_actor_contracts(contracts_dir)));
} else {
return null;
}
});
knoxx.backend.policy_db.actors.upsert_actor_contract_BANG_ = (function knoxx$backend$policy_db$actors$upsert_actor_contract_BANG_(contracts_dir,p__35599){
var map__35600 = p__35599;
var map__35600__$1 = cljs.core.__destructure_map(map__35600);
var actor_id = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"actor-id","actor-id",897721067));
var email = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"email","email",1415816706));
var display_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"display-name","display-name",694513143));
var org_slug = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"org-slug","org-slug",-726595051));
var role_slugs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"role-slugs","role-slugs",-1044987158));
var kind = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__35600__$1,new cljs.core.Keyword(null,"kind","kind",-717265803));
var existing = (function (){var G__35611 = knoxx.backend.policy_db.actors.find_actor_contract_by_id(contracts_dir,actor_id);
if((G__35611 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"actor","actor",-1830560481).cljs$core$IFn$_invoke$arity$1(G__35611);
}
})();
var normalized_email = knoxx.backend.policy_db.actors.normalize_email(email);
var actor = (function (){var G__35613 = (function (){var or__5142__auto__ = existing;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var G__35613__$1 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__35613,new cljs.core.Keyword("actor","id","actor/id",-1462607809),(function (){var or__5142__auto__ = knoxx.backend.policy_db.actors.normalize_actor_id(actor_id);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.policy_db.actors.user_actor_id_from_email(email);
}
})())
;
var G__35613__$2 = ((cljs.core.not(new cljs.core.Keyword("actor","kind","actor/kind",-1410102686).cljs$core$IFn$_invoke$arity$1(existing)))?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__35613__$1,new cljs.core.Keyword("actor","kind","actor/kind",-1410102686),(function (){var or__5142__auto__ = kind;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"agent","agent",-766455027);
}
})()):G__35613__$1);
var G__35613__$3 = (cljs.core.truth_(normalized_email)?cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(G__35613__$2,new cljs.core.Keyword("actor","email","actor/email",1189986301),normalized_email,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword("actor","username","actor/username",2032890997),normalized_email], 0)):G__35613__$2);
var G__35613__$4 = (cljs.core.truth_((function (){var G__35626 = org_slug;
var G__35626__$1 = (((G__35626 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35626)));
var G__35626__$2 = (((G__35626__$1 == null))?null:clojure.string.trim(G__35626__$1));
if((G__35626__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35626__$2);
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__35613__$3,new cljs.core.Keyword("actor","org","actor/org",175993262),clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_slug)))):G__35613__$3);
var G__35613__$5 = (cljs.core.truth_((function (){var G__35627 = display_name;
var G__35627__$1 = (((G__35627 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35627)));
var G__35627__$2 = (((G__35627__$1 == null))?null:clojure.string.trim(G__35627__$1));
if((G__35627__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35627__$2);
}
})())?cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__35613__$4,new cljs.core.Keyword("actor","label","actor/label",-1796720603),clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(display_name)))):G__35613__$4);
if(cljs.core.seq(role_slugs)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__35613__$5,new cljs.core.Keyword("actor","roles","actor/roles",186081855),cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (role){
var temp__5825__auto__ = (function (){var G__35637 = role;
var G__35637__$1 = (((G__35637 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__35637)));
var G__35637__$2 = (((G__35637__$1 == null))?null:clojure.string.trim(G__35637__$1));
if((G__35637__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__35637__$2);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var slug = temp__5825__auto__;
return cljs.core.keyword.cljs$core$IFn$_invoke$arity$2("role",clojure.string.replace(slug,/_/,"-"));
} else {
return null;
}
}),role_slugs))));
} else {
return G__35613__$5;
}
})();
return knoxx.backend.policy.protocol.upsert_actor_BANG_(knoxx.backend.policy_db.actors.edn_store(contracts_dir),actor);
});
knoxx.backend.policy_db.actors.credential_select_query = (function knoxx$backend$policy_db$actors$credential_select_query(actor_id,provider){
return knoxx.backend.policy.sql_adapter.actor_credential_select_query(actor_id,provider);
});
knoxx.backend.policy_db.actors.user_memberships_query = (function knoxx$backend$policy_db$actors$user_memberships_query(user_ids,org_id){
return knoxx.backend.policy.sql_adapter.user_memberships_query(user_ids,org_id);
});
knoxx.backend.policy_db.actors.format_sql = (function knoxx$backend$policy_db$actors$format_sql(query_map){
return knoxx.backend.policy.sql_adapter.format_sql(query_map);
});
knoxx.backend.policy_db.actors.contract_tool_ids = (function knoxx$backend$policy_db$actors$contract_tool_ids(contracts_dir){
return knoxx.backend.policy.edn_adapter.contract_tool_ids(knoxx.backend.policy_db.actors.edn_store(contracts_dir));
});

//# sourceMappingURL=knoxx.backend.policy_db.actors.js.map
