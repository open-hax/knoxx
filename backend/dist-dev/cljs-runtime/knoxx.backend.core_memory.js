import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.document_state.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.runtime.actor_scope.js";
import "./knoxx.backend.runtime.config.js";
import "./knoxx.backend.tooling.js";
goog.provide('knoxx.backend.core_memory');
knoxx.backend.core_memory.parse_json_object = (function knoxx$backend$core_memory$parse_json_object(value){
if(cljs.core.map_QMARK_(value)){
return value;
} else {
if(typeof value === 'string'){
try{return cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(JSON.parse(value),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
}catch (e59142){var _ = e59142;
return null;
}} else {
return null;

}
}
});
knoxx.backend.core_memory.row_extra_map = (function knoxx$backend$core_memory$row_extra_map(row){
var or__5142__auto__ = knoxx.backend.core_memory.parse_json_object(new cljs.core.Keyword(null,"extra","extra",1612569067).cljs$core$IFn$_invoke$arity$1(row));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
});
knoxx.backend.core_memory.devel_path_pattern = /((?:orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)\/[A-Za-z0-9._~:\/+-]+)/;
knoxx.backend.core_memory.url_pattern = /https?:\/\/[A-Za-z0-9._~:\/?#\[\]@!$&'()*+,;=%-]+/;
knoxx.backend.core_memory.trim_mention_token = (function knoxx$backend$core_memory$trim_mention_token(value){
return clojure.string.replace(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)),/^[\s`'\"\(\[\{<]+/,""),/[\s`'\"\)\]\}>:;,.!?]+$/,"");
});
knoxx.backend.core_memory.normalize_web_url = (function knoxx$backend$core_memory$normalize_web_url(value){
var raw = knoxx.backend.core_memory.trim_mention_token(value);
if(clojure.string.blank_QMARK_(raw)){
return null;
} else {
try{var parsed = (new URL(raw));
(parsed.hash = "");

if(clojure.string.blank_QMARK_(parsed.pathname)){
(parsed.pathname = "/");
} else {
}

return parsed.toString();
}catch (e59149){var _ = e59149;
return null;
}}
});
knoxx.backend.core_memory.normalize_devel_path = (function knoxx$backend$core_memory$normalize_devel_path(value){
var trimmed = knoxx.backend.core_memory.trim_mention_token(value);
var no_prefix = ((clojure.string.starts_with_QMARK_(trimmed,"/app/workspace/devel/"))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(trimmed,(("/app/workspace/devel/").length)):((clojure.string.starts_with_QMARK_(trimmed,new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(knoxx.backend.runtime.config.cfg())))?cljs.core.subs.cljs$core$IFn$_invoke$arity$2(trimmed,(cljs.core.count(new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(knoxx.backend.runtime.config.cfg())) + (1))):trimmed
));
var normalized = knoxx.backend.document_state.normalize_relative_path(no_prefix);
if(cljs.core.truth_((function (){var and__5140__auto__ = (!(clojure.string.blank_QMARK_(normalized)));
if(and__5140__auto__){
return cljs.core.re_find(/^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)\//,normalized);
} else {
return and__5140__auto__;
}
})())){
return normalized;
} else {
return null;
}
});
knoxx.backend.core_memory.extract_mentioned_urls = (function knoxx$backend$core_memory$extract_mentioned_urls(text){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.core_memory.normalize_web_url,cljs.core.re_seq(knoxx.backend.core_memory.url_pattern,(function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))));
});
knoxx.backend.core_memory.basename = (function knoxx$backend$core_memory$basename(path){
var s = clojure.string.replace(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path)),/\\\\/,"/"),/\/+/,"/");
var parts = cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,clojure.string.split.cljs$core$IFn$_invoke$arity$2(s,/\//));
var or__5142__auto__ = cljs.core.last(parts);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return s;
}
});
knoxx.backend.core_memory.known_extensionless_files = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 6, ["Justfile",null,"Dockerfile",null,"Caddyfile",null,"Makefile",null,"Brewfile",null,"Procfile",null], null), null);
/**
 * Heuristic: treat devel mentions as file nodes when the token looks like a file.
 * 
 *   Everything else is treated as a directory structural node (devel:dir:*).
 */
knoxx.backend.core_memory.likely_file_path_QMARK_ = (function knoxx$backend$core_memory$likely_file_path_QMARK_(path){
var b = knoxx.backend.core_memory.basename(path);
var or__5142__auto__ = cljs.core.contains_QMARK_(knoxx.backend.core_memory.known_extensionless_files,b);
if(or__5142__auto__){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = clojure.string.starts_with_QMARK_(b,".");
if(or__5142__auto____$1){
return or__5142__auto____$1;
} else {
return cljs.core.re_find(/\\./,b);
}
}
});
knoxx.backend.core_memory.devel_target_node = (function knoxx$backend$core_memory$devel_target_node(path){
var path__$1 = knoxx.backend.core_memory.normalize_devel_path(path);
if(clojure.string.blank_QMARK_(path__$1)){
return null;
} else {
if(cljs.core.truth_(knoxx.backend.core_memory.likely_file_path_QMARK_(path__$1))){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"path","path",-188191168),path__$1,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164),"file",new cljs.core.Keyword(null,"target_node_id","target_node_id",-988690835),(""+"devel:file:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path__$1))], null);
} else {
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"path","path",-188191168),path__$1,new cljs.core.Keyword(null,"target_kind","target_kind",-78093164),"dir",new cljs.core.Keyword(null,"target_node_id","target_node_id",-988690835),(""+"devel:dir:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path__$1))], null);
}
}
});
knoxx.backend.core_memory.extract_mentioned_devel_paths = (function knoxx$backend$core_memory$extract_mentioned_devel_paths(text){
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.core_memory.devel_target_node,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.second,cljs.core.re_seq(knoxx.backend.core_memory.devel_path_pattern,(function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))))));
});
knoxx.backend.core_memory.session_visible_QMARK_ = (function knoxx$backend$core_memory$session_visible_QMARK_(ctx,rows){
if((ctx == null)){
return true;
} else {
if(knoxx.backend.authz.system_admin_QMARK_(ctx)){
return true;
} else {
var extras = cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.core_memory.row_extra_map,rows);
var org_ids = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (p1__59202_SHARP_){
var G__59210 = p1__59202_SHARP_;
var G__59210__$1 = (((G__59210 == null))?null:new cljs.core.Keyword(null,"org_id","org_id",1380185385).cljs$core$IFn$_invoke$arity$1(G__59210));
var G__59210__$2 = (((G__59210__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59210__$1)));
if((G__59210__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59210__$2);
}
})),extras);
var membership_ids = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (p1__59203_SHARP_){
var G__59214 = p1__59203_SHARP_;
var G__59214__$1 = (((G__59214 == null))?null:new cljs.core.Keyword(null,"membership_id","membership_id",-171302674).cljs$core$IFn$_invoke$arity$1(G__59214));
var G__59214__$2 = (((G__59214__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59214__$1)));
if((G__59214__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59214__$2);
}
})),extras);
var user_ids = cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.keep.cljs$core$IFn$_invoke$arity$1((function (p1__59204_SHARP_){
var G__59217 = p1__59204_SHARP_;
var G__59217__$1 = (((G__59217 == null))?null:new cljs.core.Keyword(null,"user_id","user_id",993497112).cljs$core$IFn$_invoke$arity$1(G__59217));
var G__59217__$2 = (((G__59217__$1 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59217__$1)));
if((G__59217__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59217__$2);
}
})),extras);
var same_org_QMARK_ = cljs.core.contains_QMARK_(org_ids,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_org_id(ctx))));
if(cljs.core.empty_QMARK_(org_ids)){
return knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.memory.cross_session");
} else {
if((!(same_org_QMARK_))){
return false;
} else {
if(knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.memory.cross_session")){
return true;
} else {
return ((cljs.core.contains_QMARK_(membership_ids,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_membership_id(ctx))))) || (cljs.core.contains_QMARK_(user_ids,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_user_id(ctx))))));

}
}
}

}
}
});
knoxx.backend.core_memory.session_contract_id_from_rows = (function knoxx$backend$core_memory$session_contract_id_from_rows(rows){
return cljs.core.some((function (row){
var G__59232 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193).cljs$core$IFn$_invoke$arity$1(knoxx.backend.core_memory.row_extra_map(row));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contract-id","contract-id",-855048622).cljs$core$IFn$_invoke$arity$1(knoxx.backend.core_memory.row_extra_map(row));
}
})();
var G__59232__$1 = (((G__59232 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59232)));
var G__59232__$2 = (((G__59232__$1 == null))?null:clojure.string.trim(G__59232__$1));
if((G__59232__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59232__$2);
}
}),cljs.core.reverse(cljs.core.vec((function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.core_memory.session_contract_actors_from_rows = (function knoxx$backend$core_memory$session_contract_actors_from_rows(rows){
return cljs.core.some((function (row){
var extra = knoxx.backend.core_memory.row_extra_map(row);
var actors = (function (){var G__59240 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"contract_actors","contract_actors",-1493360705).cljs$core$IFn$_invoke$arity$1(extra);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(extra);
}
})();
return (knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1(G__59240) : knoxx.backend.runtime.actor_scope.normalize_actor_claims.call(null,G__59240));
})();
if(cljs.core.seq(actors)){
return actors;
} else {
return null;
}
}),cljs.core.reverse(cljs.core.vec((function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.core_memory.session_actor_id_from_rows = (function knoxx$backend$core_memory$session_actor_id_from_rows(rows){
return cljs.core.some((function (row){
var G__59251 = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(knoxx.backend.core_memory.row_extra_map(row));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"actor-id","actor-id",897721067).cljs$core$IFn$_invoke$arity$1(knoxx.backend.core_memory.row_extra_map(row));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return new cljs.core.Keyword(null,"actorId","actorId",989542370).cljs$core$IFn$_invoke$arity$1(knoxx.backend.core_memory.row_extra_map(row));
}
}
})();
var G__59251__$1 = (((G__59251 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59251)));
var G__59251__$2 = (((G__59251__$1 == null))?null:clojure.string.trim(G__59251__$1));
if((G__59251__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59251__$2);
}
}),cljs.core.reverse(cljs.core.vec((function (){var or__5142__auto__ = rows;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
});
knoxx.backend.core_memory.session_actor_claims_from_rows = (function knoxx$backend$core_memory$session_actor_claims_from_rows(config,rows){
var legacy_fallback = cljs.core.PersistentHashSet.createAsIfByAssoc([knoxx.backend.runtime.actor_scope.legacy_chat_actor_id]);
var or__5142__auto__ = (function (){var G__59266 = knoxx.backend.core_memory.session_actor_id_from_rows(rows);
var G__59266__$1 = (((G__59266 == null))?null:(new cljs.core.PersistentVector(null,1,(5),cljs.core.PersistentVector.EMPTY_NODE,[G__59266],null)));
if((G__59266__$1 == null)){
return null;
} else {
return (knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1(G__59266__$1) : knoxx.backend.runtime.actor_scope.normalize_actor_claims.call(null,G__59266__$1));
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = knoxx.backend.core_memory.session_contract_actors_from_rows(rows);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__59276 = knoxx.backend.core_memory.session_contract_id_from_rows(rows);
var G__59276__$1 = (((G__59276 == null))?null:knoxx.backend.tooling.resolve_agent_contract.cljs$core$IFn$_invoke$arity$2(G__59276,config));
var G__59276__$2 = (((G__59276__$1 == null))?null:new cljs.core.Keyword(null,"contract-actors","contract-actors",-173888049).cljs$core$IFn$_invoke$arity$1(G__59276__$1));
if((G__59276__$2 == null)){
return null;
} else {
return (knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.runtime.actor_scope.normalize_actor_claims.cljs$core$IFn$_invoke$arity$1(G__59276__$2) : knoxx.backend.runtime.actor_scope.normalize_actor_claims.call(null,G__59276__$2));
}
})();
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return legacy_fallback;
}
}
}
});
knoxx.backend.core_memory.session_matches_page_actor_filter_QMARK_ = (function knoxx$backend$core_memory$session_matches_page_actor_filter_QMARK_(config,rows,include_actor_id,exclude_actor_ids){
var include_actor_id__$1 = (function (){var G__59294 = include_actor_id;
var G__59294__$1 = (((G__59294 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59294)));
var G__59294__$2 = (((G__59294__$1 == null))?null:clojure.string.trim(G__59294__$1));
if((G__59294__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59294__$2);
}
})();
var exclude_actor_ids__$1 = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2((function (p1__59284_SHARP_){
var G__59303 = p1__59284_SHARP_;
var G__59303__$1 = (((G__59303 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59303)));
var G__59303__$2 = (((G__59303__$1 == null))?null:clojure.string.trim(G__59303__$1));
if((G__59303__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59303__$2);
}
}),(function (){var or__5142__auto__ = exclude_actor_ids;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})())));
var actors = knoxx.backend.core_memory.session_actor_claims_from_rows(config,rows);
var and__5140__auto__ = (function (){var or__5142__auto__ = clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = include_actor_id__$1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(or__5142__auto__){
return or__5142__auto__;
} else {
return (knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.cljs$core$IFn$_invoke$arity$2(actors,include_actor_id__$1) : knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.call(null,actors,include_actor_id__$1));
}
})();
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not_any_QMARK_((function (p1__59285_SHARP_){
return (knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.cljs$core$IFn$_invoke$arity$2(actors,p1__59285_SHARP_) : knoxx.backend.runtime.actor_scope.actor_allowed_QMARK_.call(null,actors,p1__59285_SHARP_));
}),exclude_actor_ids__$1);
} else {
return and__5140__auto__;
}
});
knoxx.backend.core_memory.session_visible_for_page_actor_QMARK_ = (function knoxx$backend$core_memory$session_visible_for_page_actor_QMARK_(config,rows,page_actor_id){
return knoxx.backend.core_memory.session_matches_page_actor_filter_QMARK_(config,rows,page_actor_id,cljs.core.PersistentVector.EMPTY);
});
knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_ = (function knoxx$backend$core_memory$fetch_openplanner_session_rows_BANG_(config,session_id){
return knoxx.backend.http.openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(config,"GET",(""+"/v1/sessions/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id))))+"?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config)))+"&mode=full")).then((function (body){
return cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
}));
});
knoxx.backend.core_memory.authorized_session_ids_BANG_ = (function knoxx$backend$core_memory$authorized_session_ids_BANG_(config,ctx,session_ids){
var session_ids__$1 = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,session_ids))));
if((((ctx == null)) || (((knoxx.backend.authz.system_admin_QMARK_(ctx)) || (cljs.core.empty_QMARK_(session_ids__$1)))))){
return Promise.resolve(cljs.core.set(session_ids__$1));
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (session_id){
return knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_(config,session_id).then((function (rows){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"allowed","allowed",1436019743),knoxx.backend.core_memory.session_visible_QMARK_(ctx,rows)], null);
}),(function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"allowed","allowed",1436019743),false], null);
}));
}),session_ids__$1))).then((function (results){
return cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session","session",1008279103),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"allowed","allowed",1436019743),knoxx.backend.http.js_array_seq(results))));
}));
}
});
knoxx.backend.core_memory.hit_session_id = (function knoxx$backend$core_memory$hit_session_id(hit){
var or__5142__auto__ = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(hit,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"session","session",1008279103)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(hit,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"session","session",1008279103)], null));
}
}
});
knoxx.backend.core_memory.hit_text = (function knoxx$backend$core_memory$hit_text(hit){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"snippet","snippet",953581994).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"document","document",-1329188687).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"text","text",-1790561697).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(hit,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"metadata","metadata",1799301597),new cljs.core.Keyword(null,"text","text",-1790561697)], null));
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
return "";
}
}
}
}
})()));
});
knoxx.backend.core_memory.reasoning_hit_QMARK_ = (function knoxx$backend$core_memory$reasoning_hit_QMARK_(hit){
var metadata = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"metadata","metadata",1799301597).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = hit;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
}
})();
var kind = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var role = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"role","role",-736691072).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"parent_id","parent_id",-1999171020).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = new cljs.core.Keyword(null,"parent-id","parent-id",-1400729131).cljs$core$IFn$_invoke$arity$1(metadata);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})()));
return ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"knoxx.reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"node_type","node_type",-1629889898).cljs$core$IFn$_invoke$arity$1(metadata),"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"node-type","node-type",129492462).cljs$core$IFn$_invoke$arity$1(metadata),"reasoning")) || (((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(role,"reasoning")) || (clojure.string.includes_QMARK_(id,":reasoning")))))))))));
});
knoxx.backend.core_memory.operational_failure_hit_QMARK_ = (function knoxx$backend$core_memory$operational_failure_hit_QMARK_(hit){
var text = knoxx.backend.core_memory.hit_text(hit);
return cljs.core.boolean$((function (){var or__5142__auto__ = cljs.core.re_find(/\b403\s+No upstream providers are allowed\b/i,text);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.re_find(/\bNo upstream providers are allowed for this tenant and request\b/i,text);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.re_find(/\bprovider_not_allowed\b/i,text);
}
}
})());
});
knoxx.backend.core_memory.filter_authorized_memory_hits_BANG_ = (function knoxx$backend$core_memory$filter_authorized_memory_hits_BANG_(config,ctx,hits){
var hits__$1 = cljs.core.vec(hits);
var session_ids = cljs.core.map.cljs$core$IFn$_invoke$arity$2(knoxx.backend.core_memory.hit_session_id,hits__$1);
return knoxx.backend.core_memory.authorized_session_ids_BANG_(config,ctx,session_ids).then((function (allowed){
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (hit){
return ((cljs.core.contains_QMARK_(allowed,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.core_memory.hit_session_id(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))) && ((((!(knoxx.backend.core_memory.reasoning_hit_QMARK_(hit)))) && ((!(knoxx.backend.core_memory.operational_failure_hit_QMARK_(hit)))))));
}),hits__$1));
}));
});

//# sourceMappingURL=knoxx.backend.core_memory.js.map
