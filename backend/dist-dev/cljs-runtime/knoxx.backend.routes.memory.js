import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.http.js";
import "./knoxx.backend.core_memory.js";
import "./knoxx.backend.openplanner_memory.js";
import "./knoxx.backend.realtime.js";
import "./knoxx.backend.redis_client.js";
import "./knoxx.backend.session_store.js";
import "./knoxx.backend.session_titles.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.util.parse.js";
import "./knoxx.backend.util.time.js";
import "./shadow.cljs.modern.js";
import "./shadow.esm.esm_import$node_crypto.js";
goog.provide('knoxx.backend.routes.memory');
knoxx.backend.routes.memory.interactive_session_id_QMARK_ = (function knoxx$backend$routes$memory$interactive_session_id_QMARK_(session_id){
return (!(clojure.string.starts_with_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)),"translation-")));
});
knoxx.backend.routes.memory.normalized_actor_id = (function knoxx$backend$routes$memory$normalized_actor_id(value){
var G__59415 = value;
var G__59415__$1 = (((G__59415 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59415)));
var G__59415__$2 = (((G__59415__$1 == null))?null:clojure.string.trim(G__59415__$1));
if((G__59415__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__59415__$2);
}
});
knoxx.backend.routes.memory.normalized_actor_ids = (function knoxx$backend$routes$memory$normalized_actor_ids(value){
var items = (((value == null))?cljs.core.PersistentVector.EMPTY:((typeof value === 'string')?clojure.string.split.cljs$core$IFn$_invoke$arity$2(value,/,/):(cljs.core.truth_(cljs.core.array_QMARK_(value))?cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1(value):((cljs.core.sequential_QMARK_(value))?value:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [value], null)
))));
return cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.memory.normalized_actor_id,items)));
});
knoxx.backend.routes.memory.filter_page_actor_rows_BANG_ = (function knoxx$backend$routes$memory$filter_page_actor_rows_BANG_(config,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,actor_id,exclude_actor_ids,page_rows){
if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = actor_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))) && (cljs.core.empty_QMARK_(exclude_actor_ids)))){
return Promise.resolve(cljs.core.vec(page_rows));
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (row){
return (function (){var G__59436 = config;
var G__59437 = new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row);
return (fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2(G__59436,G__59437) : fetch_openplanner_session_rows_BANG_.call(null,G__59436,G__59437));
})().then((function (rows){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"row","row",-570139521),row,new cljs.core.Keyword(null,"visible","visible",-1024216805),(session_matches_page_actor_filter_QMARK_.cljs$core$IFn$_invoke$arity$4 ? session_matches_page_actor_filter_QMARK_.cljs$core$IFn$_invoke$arity$4(config,rows,actor_id,exclude_actor_ids) : session_matches_page_actor_filter_QMARK_.call(null,config,rows,actor_id,exclude_actor_ids))], null);
})).catch((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"row","row",-570139521),row,new cljs.core.Keyword(null,"visible","visible",-1024216805),false], null);
}));
}),page_rows))).then((function (results){
return cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"row","row",-570139521),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"visible","visible",-1024216805),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)))));
}));
}
});
knoxx.backend.routes.memory.fetch_authorized_session_pages_BANG_ = (function knoxx$backend$routes$memory$fetch_authorized_session_pages_BANG_(config,ctx,actor_id,exclude_actor_ids,openplanner_request_BANG_,authorized_session_ids_BANG_,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,upstream_page_size,upstream_offset,acc,needed_count){
return (function (){var G__59455 = config;
var G__59456 = "GET";
var G__59457 = (""+"/v1/sessions?project="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encodeURIComponent(new cljs.core.Keyword(null,"session-project-name","session-project-name",-275048900).cljs$core$IFn$_invoke$arity$1(config)))+"&limit="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(upstream_page_size)+"&offset="+cljs.core.str.cljs$core$IFn$_invoke$arity$1(upstream_offset));
return (openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3 ? openplanner_request_BANG_.cljs$core$IFn$_invoke$arity$3(G__59455,G__59456,G__59457) : openplanner_request_BANG_.call(null,G__59455,G__59456,G__59457));
})().then((function (body){
var page_rows = cljs.core.vec((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"rows","rows",850049680).cljs$core$IFn$_invoke$arity$1(body);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})());
var fetched_count = cljs.core.count(page_rows);
var next_offset = (upstream_offset + fetched_count);
var upstream_has_more = cljs.core.boolean$(new cljs.core.Keyword(null,"has_more","has_more",1574898779).cljs$core$IFn$_invoke$arity$1(body));
return (function (){var G__59466 = config;
var G__59467 = ctx;
var G__59468 = cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"session","session",1008279103),page_rows);
return (authorized_session_ids_BANG_.cljs$core$IFn$_invoke$arity$3 ? authorized_session_ids_BANG_.cljs$core$IFn$_invoke$arity$3(G__59466,G__59467,G__59468) : authorized_session_ids_BANG_.call(null,G__59466,G__59467,G__59468));
})().then((function (allowed){
var authorized_rows = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59452_SHARP_){
return knoxx.backend.routes.memory.interactive_session_id_QMARK_(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(p1__59452_SHARP_));
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59451_SHARP_){
return cljs.core.contains_QMARK_(allowed,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(p1__59451_SHARP_))));
}),page_rows)));
return knoxx.backend.routes.memory.filter_page_actor_rows_BANG_(config,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,actor_id,exclude_actor_ids,authorized_rows).then((function (actor_visible_rows){
var next_acc = cljs.core.into.cljs$core$IFn$_invoke$arity$2(acc,actor_visible_rows);
var reached_target_QMARK_ = ((typeof needed_count === 'number') && ((cljs.core.count(next_acc) >= needed_count)));
if(reached_target_QMARK_){
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"rows","rows",850049680),cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(needed_count,next_acc)),new cljs.core.Keyword(null,"has_more","has_more",1574898779),true], null));
} else {
if(((upstream_has_more) && ((fetched_count > (0))))){
return (knoxx.backend.routes.memory.fetch_authorized_session_pages_BANG_.cljs$core$IFn$_invoke$arity$12 ? knoxx.backend.routes.memory.fetch_authorized_session_pages_BANG_.cljs$core$IFn$_invoke$arity$12(config,ctx,actor_id,exclude_actor_ids,openplanner_request_BANG_,authorized_session_ids_BANG_,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,upstream_page_size,next_offset,next_acc,needed_count) : knoxx.backend.routes.memory.fetch_authorized_session_pages_BANG_.call(null,config,ctx,actor_id,exclude_actor_ids,openplanner_request_BANG_,authorized_session_ids_BANG_,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,upstream_page_size,next_offset,next_acc,needed_count));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"rows","rows",850049680),next_acc,new cljs.core.Keyword(null,"has_more","has_more",1574898779),false], null));

}
}
}));
}));
}));
});
knoxx.backend.routes.memory.hit_session_id = (function knoxx$backend$routes$memory$hit_session_id(hit){
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
knoxx.backend.routes.memory.filter_search_hits_by_actor_BANG_ = (function knoxx$backend$routes$memory$filter_search_hits_by_actor_BANG_(config,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,actor_id,exclude_actor_ids,hits){
var actor_id__$1 = knoxx.backend.routes.memory.normalized_actor_id(actor_id);
var exclude_actor_ids__$1 = knoxx.backend.routes.memory.normalized_actor_ids(exclude_actor_ids);
var hits__$1 = cljs.core.vec(hits);
if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = actor_id__$1;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())))) && (cljs.core.empty_QMARK_(exclude_actor_ids__$1)))){
return Promise.resolve(hits__$1);
} else {
var session_ids = cljs.core.vec(cljs.core.distinct.cljs$core$IFn$_invoke$arity$1(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.str,cljs.core.keep.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.memory.hit_session_id,hits__$1)))));
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (session_id){
return (fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2(config,session_id) : fetch_openplanner_session_rows_BANG_.call(null,config,session_id)).then((function (rows){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"visible","visible",-1024216805),(session_matches_page_actor_filter_QMARK_.cljs$core$IFn$_invoke$arity$4 ? session_matches_page_actor_filter_QMARK_.cljs$core$IFn$_invoke$arity$4(config,rows,actor_id__$1,exclude_actor_ids__$1) : session_matches_page_actor_filter_QMARK_.call(null,config,rows,actor_id__$1,exclude_actor_ids__$1))], null);
})).catch((function (_){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"visible","visible",-1024216805),false], null);
}));
}),session_ids))).then((function (results){
var allowed_sessions = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.str,new cljs.core.Keyword(null,"session","session",1008279103)),cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"visible","visible",-1024216805),cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(results,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)))));
return cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (hit){
return cljs.core.contains_QMARK_(allowed_sessions,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = knoxx.backend.routes.memory.hit_session_id(hit);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
}),hits__$1));
}));
}
});
knoxx.backend.routes.memory.warm_title_cache_BANG_ = (function knoxx$backend$routes$memory$warm_title_cache_BANG_(session_id,config,runtime){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = session_id;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if((((!(clojure.string.blank_QMARK_(session_id__$1)))) && ((((!(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_),session_id__$1)))) && ((!(cljs.core.contains_QMARK_(cljs.core.deref(knoxx.backend.session_titles.session_title_promises_STAR_),session_id__$1)))))))){
var title_promise = knoxx.backend.core_memory.fetch_openplanner_session_rows_BANG_(config,session_id__$1).then((function (title_rows){
var seed_text = knoxx.backend.session_titles.session_title_seed_text(title_rows);
var fallback_title = knoxx.backend.session_titles.heuristic_session_title(seed_text);
return knoxx.backend.session_titles.resolve_session_title_BANG_(config,seed_text).then((function (entry){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,(function (){var or__5142__auto__ = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(entry),fallback_title);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return fallback_title;
}
})(),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry));
})).catch((function (_){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,fallback_title,null);
}));
})).catch((function (_){
return knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,"Untitled session",null);
}));
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.session_titles.session_title_promises_STAR_,cljs.core.assoc,session_id__$1,title_promise);

return title_promise;
} else {
return null;
}
});
knoxx.backend.routes.memory.inactive_row = (function knoxx$backend$routes$memory$inactive_row(row){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(row,new cljs.core.Keyword(null,"is_active","is_active",-750859351),false,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"active_status","active_status",1372457012),"inactive",new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),false], 0));
});
knoxx.backend.routes.memory.enrich_row = (function knoxx$backend$routes$memory$enrich_row(redis_client,row){
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row)));
var titled_row = (function (){var temp__5823__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_),session_id);
if(cljs.core.truth_(temp__5823__auto__)){
var title_entry = temp__5823__auto__;
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(row,new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"title","title",636505583).cljs$core$IFn$_invoke$arity$1(title_entry),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"title_model","title_model",501758950),new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(title_entry)], 0));
} else {
return row;
}
})();
if(cljs.core.not(redis_client)){
return Promise.resolve(knoxx.backend.routes.memory.inactive_row(titled_row));
} else {
return knoxx.backend.session_store.get_conversation_active_session(redis_client,session_id).then((function (active_session_id){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(active_session_id)))){
return knoxx.backend.routes.memory.inactive_row(titled_row);
} else {
return knoxx.backend.session_store.get_session(redis_client,active_session_id).then((function (active_session){
var status = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(active_session);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "inactive";
}
})();
var is_active = cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["waiting_input",null,"running",null], null), null),status);
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(titled_row,new cljs.core.Keyword(null,"active_session_id","active_session_id",1867780677),active_session_id,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"is_active","is_active",-750859351),is_active,new cljs.core.Keyword(null,"active_status","active_status",1372457012),status,new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),cljs.core.boolean$(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(active_session))], 0));
})).catch((function (_){
return knoxx.backend.routes.memory.inactive_row(titled_row);
}));
}
})).catch((function (_){
return knoxx.backend.routes.memory.inactive_row(titled_row);
}));
}
});
knoxx.backend.routes.memory.memory_sessions_route_BANG_ = (function knoxx$backend$routes$memory$memory_sessions_route_BANG_(app,runtime,config,deps){
var map__59528 = deps;
var map__59528__$1 = cljs.core.__destructure_map(map__59528);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var openplanner_enabled_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"openplanner-enabled?","openplanner-enabled?",-1180234471));
var openplanner_request_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"openplanner-request!","openplanner-request!",-1690277990));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_matches_page_actor_filter_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"session-matches-page-actor-filter?","session-matches-page-actor-filter?",2088135972));
var authorized_session_ids_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"authorized-session-ids!","authorized-session-ids!",999199653));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var fetch_openplanner_session_rows_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"fetch-openplanner-session-rows!","fetch-openplanner-session-rows!",1014940648));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59528__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59538 = app;
var G__59539 = "GET";
var G__59540 = "/api/memory/sessions";
var G__59541 = (function (request,reply){
var G__59543 = runtime;
var G__59544 = request;
var G__59545 = reply;
var G__59546 = (function (ctx){
if(cljs.core.not((openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1 ? openplanner_enabled_QMARK_.cljs$core$IFn$_invoke$arity$1(config) : openplanner_enabled_QMARK_.call(null,config)))){
var G__59547 = reply;
var G__59548 = (503);
var G__59549 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59547,G__59548,G__59549) : json_response_BANG_.call(null,G__59547,G__59548,G__59549));
} else {
var limit_raw = (request["query"]["limit"]);
var limit = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int(limit_raw);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (12);
}
})();
var actor_id = (function (){var G__59552 = (function (){var or__5142__auto__ = (request["query"]["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (request["query"]["actor"]);
}
})();
if((G__59552 == null)){
return null;
} else {
return knoxx.backend.routes.memory.normalized_actor_id(G__59552);
}
})();
var exclude_actor_ids = knoxx.backend.routes.memory.normalized_actor_ids((function (){var or__5142__auto__ = (request["query"]["excludeActorIds"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["excludeActorId"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (request["query"]["excludeActors"]);
}
}
})());
var offset_raw = (request["query"]["offset"]);
var offset_parsed = parseInt((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = offset_raw;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "0";
}
})())),(10));
var offset = (cljs.core.truth_((function (){var and__5140__auto__ = Number.isFinite(offset_parsed);
if(cljs.core.truth_(and__5140__auto__)){
return (offset_parsed >= (0));
} else {
return and__5140__auto__;
}
})())?offset_parsed:(0));
var upstream_page_size = (200);
var needed_count = ((offset + cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),limit)) + (1));
return knoxx.backend.routes.memory.fetch_authorized_session_pages_BANG_(config,ctx,actor_id,exclude_actor_ids,openplanner_request_BANG_,authorized_session_ids_BANG_,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,upstream_page_size,(0),cljs.core.PersistentVector.EMPTY,needed_count).then((function (p__59561){
var map__59562 = p__59561;
var map__59562__$1 = cljs.core.__destructure_map(map__59562);
var rows = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59562__$1,new cljs.core.Keyword(null,"rows","rows",850049680));
var has_more = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59562__$1,new cljs.core.Keyword(null,"has_more","has_more",1574898779));
var all_rows = cljs.core.vec(rows);
var visible_total = cljs.core.count(all_rows);
var page_rows = cljs.core.vec(cljs.core.take.cljs$core$IFn$_invoke$arity$2(cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),limit),cljs.core.drop.cljs$core$IFn$_invoke$arity$2(offset,all_rows)));
var page_has_more = (function (){var or__5142__auto__ = has_more;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (visible_total > (offset + cljs.core.count(page_rows)));
}
})();
var redis_client = knoxx.backend.redis_client.get_client();
var enrich_promises = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.memory.enrich_row,redis_client),page_rows);
if(cljs.core.not(redis_client)){
var seq__59590_59845 = cljs.core.seq(page_rows);
var chunk__59591_59846 = null;
var count__59592_59847 = (0);
var i__59593_59848 = (0);
while(true){
if((i__59593_59848 < count__59592_59847)){
var row_59849 = chunk__59591_59846.cljs$core$IIndexed$_nth$arity$2(null,i__59593_59848);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59849))),config,runtime);


var G__59850 = seq__59590_59845;
var G__59851 = chunk__59591_59846;
var G__59852 = count__59592_59847;
var G__59853 = (i__59593_59848 + (1));
seq__59590_59845 = G__59850;
chunk__59591_59846 = G__59851;
count__59592_59847 = G__59852;
i__59593_59848 = G__59853;
continue;
} else {
var temp__5825__auto___59854 = cljs.core.seq(seq__59590_59845);
if(temp__5825__auto___59854){
var seq__59590_59855__$1 = temp__5825__auto___59854;
if(cljs.core.chunked_seq_QMARK_(seq__59590_59855__$1)){
var c__5673__auto___59856 = cljs.core.chunk_first(seq__59590_59855__$1);
var G__59857 = cljs.core.chunk_rest(seq__59590_59855__$1);
var G__59858 = c__5673__auto___59856;
var G__59859 = cljs.core.count(c__5673__auto___59856);
var G__59860 = (0);
seq__59590_59845 = G__59857;
chunk__59591_59846 = G__59858;
count__59592_59847 = G__59859;
i__59593_59848 = G__59860;
continue;
} else {
var row_59861 = cljs.core.first(seq__59590_59855__$1);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59861))),config,runtime);


var G__59862 = cljs.core.next(seq__59590_59855__$1);
var G__59863 = null;
var G__59864 = (0);
var G__59865 = (0);
seq__59590_59845 = G__59862;
chunk__59591_59846 = G__59863;
count__59592_59847 = G__59864;
i__59593_59848 = G__59865;
continue;
}
} else {
}
}
break;
}

return Promise.all(cljs.core.clj__GT_js(enrich_promises)).then((function (enriched_rows){
var G__59617_59866 = reply;
var G__59618_59867 = (200);
var G__59619_59868 = (function (){var G__59620 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"rows","rows",850049680),cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(enriched_rows,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))),new cljs.core.Keyword(null,"offset","offset",296498311),offset,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"has_more","has_more",1574898779),page_has_more], null);
if(cljs.core.not(page_has_more)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59620,new cljs.core.Keyword(null,"total","total",1916810418),visible_total);
} else {
return G__59620;
}
})();
(json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59617_59866,G__59618_59867,G__59619_59868) : json_response_BANG_.call(null,G__59617_59866,G__59618_59867,G__59619_59868));

return null;
})).catch((function (err){
(error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));

return null;
}));
} else {
return knoxx.backend.session_store.list_active_sessions(redis_client).then((function (live_ids){
return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (p1__59523_SHARP_){
return knoxx.backend.session_store.get_session(redis_client,p1__59523_SHARP_);
}),cljs.core.vec(live_ids)))).then((function (live_js){
var live = cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(live_js,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0)));
var op_ids = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__59524_SHARP_){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(p1__59524_SHARP_)));
}),page_rows));
var synthetic = cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (s){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193),new cljs.core.Keyword(null,"last_ts","last_ts",1415613512),new cljs.core.Keyword(null,"is_active","is_active",-750859351),new cljs.core.Keyword(null,"actor_id","actor_id",2086217260),new cljs.core.Keyword(null,"title","title",636505583),new cljs.core.Keyword(null,"active_status","active_status",1372457012),new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106),new cljs.core.Keyword(null,"event_count","event_count",-1889732422),new cljs.core.Keyword(null,"session","session",1008279103)],[cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(s,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"agent_spec","agent_spec",788920365),new cljs.core.Keyword(null,"contract_id","contract_id",-1829507193)], null)),new cljs.core.Keyword(null,"updated_at","updated_at",-460224592).cljs$core$IFn$_invoke$arity$1(s),true,new cljs.core.Keyword(null,"actor_id","actor_id",2086217260).cljs$core$IFn$_invoke$arity$1(s),(""+"Running \u00B7 "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"run_id","run_id",-556768024).cljs$core$IFn$_invoke$arity$1(s);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(s);
}
})())),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(s),cljs.core.boolean$(new cljs.core.Keyword(null,"has_active_stream","has_active_stream",-1354700106).cljs$core$IFn$_invoke$arity$1(s)),(0),new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(s)]);
}),cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p1__59526_SHARP_){
var and__5140__auto__ = new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(p1__59526_SHARP_);
if(cljs.core.truth_(and__5140__auto__)){
return ((cljs.core.not((function (){var G__59625 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"conversation_id","conversation_id",-172324980).cljs$core$IFn$_invoke$arity$1(p1__59526_SHARP_)));
return (op_ids.cljs$core$IFn$_invoke$arity$1 ? op_ids.cljs$core$IFn$_invoke$arity$1(G__59625) : op_ids.call(null,G__59625));
})())) && (cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, ["waiting_input",null,"running",null], null), null),new cljs.core.Keyword(null,"status","status",-1997798413).cljs$core$IFn$_invoke$arity$1(p1__59526_SHARP_))));
} else {
return and__5140__auto__;
}
}),live));
var all_rows__$1 = cljs.core.vec(cljs.core.concat.cljs$core$IFn$_invoke$arity$2(synthetic,page_rows));
var seq__59627_59873 = cljs.core.seq(all_rows__$1);
var chunk__59628_59874 = null;
var count__59629_59875 = (0);
var i__59630_59876 = (0);
while(true){
if((i__59630_59876 < count__59629_59875)){
var row_59877 = chunk__59628_59874.cljs$core$IIndexed$_nth$arity$2(null,i__59630_59876);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59877))),config,runtime);


var G__59879 = seq__59627_59873;
var G__59880 = chunk__59628_59874;
var G__59881 = count__59629_59875;
var G__59882 = (i__59630_59876 + (1));
seq__59627_59873 = G__59879;
chunk__59628_59874 = G__59880;
count__59629_59875 = G__59881;
i__59630_59876 = G__59882;
continue;
} else {
var temp__5825__auto___59883 = cljs.core.seq(seq__59627_59873);
if(temp__5825__auto___59883){
var seq__59627_59884__$1 = temp__5825__auto___59883;
if(cljs.core.chunked_seq_QMARK_(seq__59627_59884__$1)){
var c__5673__auto___59885 = cljs.core.chunk_first(seq__59627_59884__$1);
var G__59886 = cljs.core.chunk_rest(seq__59627_59884__$1);
var G__59887 = c__5673__auto___59885;
var G__59888 = cljs.core.count(c__5673__auto___59885);
var G__59889 = (0);
seq__59627_59873 = G__59886;
chunk__59628_59874 = G__59887;
count__59629_59875 = G__59888;
i__59630_59876 = G__59889;
continue;
} else {
var row_59890 = cljs.core.first(seq__59627_59884__$1);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59890))),config,runtime);


var G__59891 = cljs.core.next(seq__59627_59884__$1);
var G__59892 = null;
var G__59893 = (0);
var G__59894 = (0);
seq__59627_59873 = G__59891;
chunk__59628_59874 = G__59892;
count__59629_59875 = G__59893;
i__59630_59876 = G__59894;
continue;
}
} else {
}
}
break;
}

return Promise.all(cljs.core.clj__GT_js(cljs.core.mapv.cljs$core$IFn$_invoke$arity$2(cljs.core.partial.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.memory.enrich_row,redis_client),all_rows__$1))).then((function (enriched_rows){
var G__59636_59895 = reply;
var G__59637_59896 = (200);
var G__59638_59897 = (function (){var G__59639 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"rows","rows",850049680),cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(enriched_rows,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))),new cljs.core.Keyword(null,"offset","offset",296498311),offset,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"has_more","has_more",1574898779),page_has_more], null);
if(cljs.core.not(page_has_more)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59639,new cljs.core.Keyword(null,"total","total",1916810418),visible_total);
} else {
return G__59639;
}
})();
(json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59636_59895,G__59637_59896,G__59638_59897) : json_response_BANG_.call(null,G__59636_59895,G__59637_59896,G__59638_59897));

return null;
})).catch((function (err){
(error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));

return null;
}));
})).catch((function (err){
(error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));

return null;
}));
})).catch((function (_){
var seq__59640_59898 = cljs.core.seq(page_rows);
var chunk__59641_59899 = null;
var count__59642_59900 = (0);
var i__59643_59901 = (0);
while(true){
if((i__59643_59901 < count__59642_59900)){
var row_59902 = chunk__59641_59899.cljs$core$IIndexed$_nth$arity$2(null,i__59643_59901);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59902))),config,runtime);


var G__59903 = seq__59640_59898;
var G__59904 = chunk__59641_59899;
var G__59905 = count__59642_59900;
var G__59906 = (i__59643_59901 + (1));
seq__59640_59898 = G__59903;
chunk__59641_59899 = G__59904;
count__59642_59900 = G__59905;
i__59643_59901 = G__59906;
continue;
} else {
var temp__5825__auto___59907 = cljs.core.seq(seq__59640_59898);
if(temp__5825__auto___59907){
var seq__59640_59908__$1 = temp__5825__auto___59907;
if(cljs.core.chunked_seq_QMARK_(seq__59640_59908__$1)){
var c__5673__auto___59909 = cljs.core.chunk_first(seq__59640_59908__$1);
var G__59910 = cljs.core.chunk_rest(seq__59640_59908__$1);
var G__59911 = c__5673__auto___59909;
var G__59912 = cljs.core.count(c__5673__auto___59909);
var G__59913 = (0);
seq__59640_59898 = G__59910;
chunk__59641_59899 = G__59911;
count__59642_59900 = G__59912;
i__59643_59901 = G__59913;
continue;
} else {
var row_59914 = cljs.core.first(seq__59640_59908__$1);
knoxx.backend.routes.memory.warm_title_cache_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"session","session",1008279103).cljs$core$IFn$_invoke$arity$1(row_59914))),config,runtime);


var G__59915 = cljs.core.next(seq__59640_59908__$1);
var G__59916 = null;
var G__59917 = (0);
var G__59918 = (0);
seq__59640_59898 = G__59915;
chunk__59641_59899 = G__59916;
count__59642_59900 = G__59917;
i__59643_59901 = G__59918;
continue;
}
} else {
}
}
break;
}

return Promise.all(cljs.core.clj__GT_js(enrich_promises)).then((function (enriched_rows){
var G__59660_59920 = reply;
var G__59661_59921 = (200);
var G__59662_59922 = (function (){var G__59671 = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"rows","rows",850049680),cljs.core.vec(cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic(enriched_rows,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0))),new cljs.core.Keyword(null,"offset","offset",296498311),offset,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"has_more","has_more",1574898779),page_has_more], null);
if(cljs.core.not(page_has_more)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__59671,new cljs.core.Keyword(null,"total","total",1916810418),visible_total);
} else {
return G__59671;
}
})();
(json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59660_59920,G__59661_59921,G__59662_59922) : json_response_BANG_.call(null,G__59660_59920,G__59661_59921,G__59662_59922));

return null;
})).catch((function (err){
(error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));

return null;
}));
}));
}
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59543,G__59544,G__59545,G__59546) : with_request_context_BANG_.call(null,G__59543,G__59544,G__59545,G__59546));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59538,G__59539,G__59540,G__59541) : route_BANG_.call(null,G__59538,G__59539,G__59540,G__59541));
});
knoxx.backend.routes.memory.memory_session_titles_status_route_BANG_ = (function knoxx$backend$routes$memory$memory_session_titles_status_route_BANG_(app,runtime,config,deps){
var map__59677 = deps;
var map__59677__$1 = cljs.core.__destructure_map(map__59677);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59677__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59678 = app;
var G__59679 = "GET";
var G__59680 = "/api/memory/session-titles/status";
var G__59681 = (function (request,reply){
var G__59682 = runtime;
var G__59683 = request;
var G__59684 = reply;
var G__59685 = (function (ctx){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
var G__59686 = reply;
var G__59687 = (503);
var G__59688 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59686,G__59687,G__59688) : json_response_BANG_.call(null,G__59686,G__59687,G__59688));
} else {
var G__59689 = reply;
var G__59690 = (200);
var G__59691 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"status","status",-1997798413),cljs.core.deref(knoxx.backend.session_titles.session_title_backfill_STAR_),new cljs.core.Keyword(null,"cached_count","cached_count",246241445),cljs.core.count(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59689,G__59690,G__59691) : json_response_BANG_.call(null,G__59689,G__59690,G__59691));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59682,G__59683,G__59684,G__59685) : with_request_context_BANG_.call(null,G__59682,G__59683,G__59684,G__59685));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59678,G__59679,G__59680,G__59681) : route_BANG_.call(null,G__59678,G__59679,G__59680,G__59681));
});
knoxx.backend.routes.memory.memory_backfill_titles_route_BANG_ = (function knoxx$backend$routes$memory$memory_backfill_titles_route_BANG_(app,runtime,config,deps){
var map__59692 = deps;
var map__59692__$1 = cljs.core.__destructure_map(map__59692);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var fetch_openplanner_session_rows_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"fetch-openplanner-session-rows!","fetch-openplanner-session-rows!",1014940648));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59692__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59697 = app;
var G__59698 = "POST";
var G__59699 = "/api/memory/sessions/backfill-titles";
var G__59700 = (function (request,reply){
var G__59701 = runtime;
var G__59702 = request;
var G__59703 = reply;
var G__59704 = (function (ctx){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
var G__59705 = reply;
var G__59706 = (503);
var G__59707 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59705,G__59706,G__59707) : json_response_BANG_.call(null,G__59705,G__59706,G__59707));
} else {
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var limit = (function (){var or__5142__auto__ = knoxx.backend.util.parse.parse_positive_int((body["limit"]));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.util.parse.parse_positive_int((request["query"]["limit"]));
}
})();
var force_QMARK_ = ((knoxx.backend.util.parse.truthy_param_QMARK_((body["force"]))) || (knoxx.backend.util.parse.truthy_param_QMARK_((request["query"]["force"]))));
return knoxx.backend.session_titles.start_session_title_backfill_BANG_(runtime,config,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"force","force",781957286),force_QMARK_,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit], null),fetch_openplanner_session_rows_BANG_).then((function (status){
var G__59708 = reply;
var G__59709 = (202);
var G__59710 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"status","status",-1997798413),status,new cljs.core.Keyword(null,"cached_count","cached_count",246241445),cljs.core.count(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59708,G__59709,G__59710) : json_response_BANG_.call(null,G__59708,G__59709,G__59710));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59701,G__59702,G__59703,G__59704) : with_request_context_BANG_.call(null,G__59701,G__59702,G__59703,G__59704));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59697,G__59698,G__59699,G__59700) : route_BANG_.call(null,G__59697,G__59698,G__59699,G__59700));
});
knoxx.backend.routes.memory.memory_import_titles_route_BANG_ = (function knoxx$backend$routes$memory$memory_import_titles_route_BANG_(app,runtime,config,deps){
var map__59712 = deps;
var map__59712__$1 = cljs.core.__destructure_map(map__59712);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59712__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59717 = app;
var G__59718 = "POST";
var G__59719 = "/api/memory/sessions/import-titles";
var G__59720 = (function (request,reply){
var G__59728 = runtime;
var G__59729 = request;
var G__59730 = reply;
var G__59731 = (function (ctx){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
var G__59746 = reply;
var G__59747 = (503);
var G__59748 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59746,G__59747,G__59748) : json_response_BANG_.call(null,G__59746,G__59747,G__59748));
} else {
var body = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
var titles = (function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(body,"titles");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var updated = cljs.core.reduce_kv((function (total,session_id,entry){
var session_id__$1 = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id));
var raw_title = ((cljs.core.map_QMARK_(entry))?(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(entry,"title");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.get.cljs$core$IFn$_invoke$arity$2(entry,new cljs.core.Keyword(null,"title","title",636505583));
}
})():entry);
var title_model = ((cljs.core.map_QMARK_(entry))?(function (){var or__5142__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(entry,"title_model");
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(entry,"title-model");
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = cljs.core.get.cljs$core$IFn$_invoke$arity$2(entry,"model");
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
var or__5142__auto____$3 = new cljs.core.Keyword(null,"title_model","title_model",501758950).cljs$core$IFn$_invoke$arity$1(entry);
if(cljs.core.truth_(or__5142__auto____$3)){
return or__5142__auto____$3;
} else {
var or__5142__auto____$4 = new cljs.core.Keyword(null,"title-model","title-model",-531930396).cljs$core$IFn$_invoke$arity$1(entry);
if(cljs.core.truth_(or__5142__auto____$4)){
return or__5142__auto____$4;
} else {
return new cljs.core.Keyword(null,"model","model",331153215).cljs$core$IFn$_invoke$arity$1(entry);
}
}
}
}
}
})():null);
var normalized = knoxx.backend.session_titles.normalize_session_title.cljs$core$IFn$_invoke$arity$1(raw_title);
if(((clojure.string.blank_QMARK_(session_id__$1)) || ((normalized == null)))){
return total;
} else {
knoxx.backend.session_titles.cache_session_title_BANG_(runtime,config,session_id__$1,normalized,(function (){var or__5142__auto__ = title_model;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "retro:heuristic";
}
})());

return (total + (1));
}
}),(0),titles);
var G__59754 = reply;
var G__59755 = (200);
var G__59756 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"updated","updated",-1627192056),updated,new cljs.core.Keyword(null,"cached_count","cached_count",246241445),cljs.core.count(cljs.core.deref(knoxx.backend.session_titles.session_titles_STAR_))], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59754,G__59755,G__59756) : json_response_BANG_.call(null,G__59754,G__59755,G__59756));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59728,G__59729,G__59730,G__59731) : with_request_context_BANG_.call(null,G__59728,G__59729,G__59730,G__59731));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59717,G__59718,G__59719,G__59720) : route_BANG_.call(null,G__59717,G__59718,G__59719,G__59720));
});
knoxx.backend.routes.memory.memory_session_by_id_route_BANG_ = (function knoxx$backend$routes$memory$memory_session_by_id_route_BANG_(app,runtime,config,deps){
var map__59761 = deps;
var map__59761__$1 = cljs.core.__destructure_map(map__59761);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var fetch_openplanner_session_rows_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"fetch-openplanner-session-rows!","fetch-openplanner-session-rows!",1014940648));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59761__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59762 = app;
var G__59763 = "GET";
var G__59764 = "/api/memory/sessions/:sessionId";
var G__59765 = (function (request,reply){
var G__59766 = runtime;
var G__59767 = request;
var G__59768 = reply;
var G__59769 = (function (ctx){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
var G__59770 = reply;
var G__59771 = (503);
var G__59772 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59770,G__59771,G__59772) : json_response_BANG_.call(null,G__59770,G__59771,G__59772));
} else {
var session_id = (function (){var or__5142__auto__ = (request["params"]["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(session_id)){
var G__59773 = reply;
var G__59774 = (400);
var G__59775 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"sessionId is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59773,G__59774,G__59775) : json_response_BANG_.call(null,G__59773,G__59774,G__59775));
} else {
return (fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2 ? fetch_openplanner_session_rows_BANG_.cljs$core$IFn$_invoke$arity$2(config,session_id) : fetch_openplanner_session_rows_BANG_.call(null,config,session_id)).then((function (rows){
if(knoxx.backend.core_memory.session_visible_QMARK_(ctx,rows)){
var G__59776 = reply;
var G__59777 = (200);
var G__59778 = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"session","session",1008279103),session_id,new cljs.core.Keyword(null,"rows","rows",850049680),rows], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59776,G__59777,G__59778) : json_response_BANG_.call(null,G__59776,G__59777,G__59778));
} else {
var G__59779 = reply;
var G__59780 = knoxx.backend.http.http_error((403),"memory_scope_denied","Session is outside the current Knoxx scope");
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$2 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$2(G__59779,G__59780) : error_response_BANG_.call(null,G__59779,G__59780));
}
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));
}));
}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59766,G__59767,G__59768,G__59769) : with_request_context_BANG_.call(null,G__59766,G__59767,G__59768,G__59769));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59762,G__59763,G__59764,G__59765) : route_BANG_.call(null,G__59762,G__59763,G__59764,G__59765));
});
knoxx.backend.routes.memory.memory_search_route_BANG_ = (function knoxx$backend$routes$memory$memory_search_route_BANG_(app,runtime,config,deps){
var map__59783 = deps;
var map__59783__$1 = cljs.core.__destructure_map(map__59783);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_matches_page_actor_filter_QMARK_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"session-matches-page-actor-filter?","session-matches-page-actor-filter?",2088135972));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var fetch_openplanner_session_rows_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"fetch-openplanner-session-rows!","fetch-openplanner-session-rows!",1014940648));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59783__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59785 = app;
var G__59786 = "POST";
var G__59787 = "/api/memory/search";
var G__59788 = (function (request,reply){
var G__59789 = runtime;
var G__59790 = request;
var G__59791 = reply;
var G__59792 = (function (ctx){
if((!(knoxx.backend.http.openplanner_enabled_QMARK_(config)))){
var G__59793 = reply;
var G__59794 = (503);
var G__59795 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"OpenPlanner is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59793,G__59794,G__59795) : json_response_BANG_.call(null,G__59793,G__59794,G__59795));
} else {
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var query = (function (){var or__5142__auto__ = (body["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var k = (body["k"]);
var actor_id = knoxx.backend.routes.memory.normalized_actor_id((function (){var or__5142__auto__ = (body["actorId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["actor_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (body["actor"]);
}
}
})());
var exclude_actor_ids = knoxx.backend.routes.memory.normalized_actor_ids((function (){var or__5142__auto__ = (body["excludeActorIds"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["exclude_actor_ids"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (body["excludeActorId"]);
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return (body["exclude_actor_id"]);
}
}
}
})());
var session_id = (function (){var or__5142__auto__ = (body["sessionId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["session_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"agent.memory.read") : ensure_permission_BANG_.call(null,ctx,"agent.memory.read"));

if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))) && ((((!(knoxx.backend.authz.ctx_permitted_QMARK_(ctx,"agent.memory.cross_session")))) && ((!(knoxx.backend.authz.system_admin_QMARK_(ctx)))))))){
throw knoxx.backend.http.http_error((403),"memory_scope_denied","Cross-session memory search is outside the current Knoxx scope");
} else {
}

return knoxx.backend.openplanner_memory.openplanner_memory_search_BANG_(config,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"query","query",-1288509510),query,new cljs.core.Keyword(null,"k","k",-2146297393),k,new cljs.core.Keyword(null,"session-id","session-id",-1147060351),session_id], null)).then((function (result){
return knoxx.backend.core_memory.filter_authorized_memory_hits_BANG_(config,ctx,new cljs.core.Keyword(null,"hits","hits",-2120002930).cljs$core$IFn$_invoke$arity$1(result)).then((function (hits){
return knoxx.backend.routes.memory.filter_search_hits_by_actor_BANG_(config,fetch_openplanner_session_rows_BANG_,session_matches_page_actor_filter_QMARK_,actor_id,exclude_actor_ids,hits).then((function (filtered_hits){
var G__59797 = reply;
var G__59798 = (200);
var G__59799 = cljs.core.assoc.cljs$core$IFn$_invoke$arity$variadic(result,new cljs.core.Keyword(null,"ok","ok",967785236),true,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"hits","hits",-2120002930),filtered_hits], 0));
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59797,G__59798,G__59799) : json_response_BANG_.call(null,G__59797,G__59798,G__59799));
}));
}));
})).catch((function (err){
return (error_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? error_response_BANG_.cljs$core$IFn$_invoke$arity$3(reply,err,(502)) : error_response_BANG_.call(null,reply,err,(502)));
}));
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59789,G__59790,G__59791,G__59792) : with_request_context_BANG_.call(null,G__59789,G__59790,G__59791,G__59792));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59785,G__59786,G__59787,G__59788) : route_BANG_.call(null,G__59785,G__59786,G__59787,G__59788));
});
knoxx.backend.routes.memory.lounge_messages_list_route_BANG_ = (function knoxx$backend$routes$memory$lounge_messages_list_route_BANG_(app,runtime,config,deps){
var map__59800 = deps;
var map__59800__$1 = cljs.core.__destructure_map(map__59800);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59800__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59801 = app;
var G__59802 = "GET";
var G__59803 = "/api/lounge/messages";
var G__59804 = (function (request,reply){
var G__59805 = runtime;
var G__59806 = request;
var G__59807 = reply;
var G__59808 = (function (ctx){
var G__59809 = reply;
var G__59810 = (200);
var G__59811 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"messages","messages",345434482),cljs.core.deref(knoxx.backend.routes.memory.lounge_messages_STAR_)], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59809,G__59810,G__59811) : json_response_BANG_.call(null,G__59809,G__59810,G__59811));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59805,G__59806,G__59807,G__59808) : with_request_context_BANG_.call(null,G__59805,G__59806,G__59807,G__59808));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59801,G__59802,G__59803,G__59804) : route_BANG_.call(null,G__59801,G__59802,G__59803,G__59804));
});
knoxx.backend.routes.memory.lounge_messages_create_route_BANG_ = (function knoxx$backend$routes$memory$lounge_messages_create_route_BANG_(app,runtime,config,deps){
var map__59814 = deps;
var map__59814__$1 = cljs.core.__destructure_map(map__59814);
var clip_text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"clip-text","clip-text",-1457928615));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var fetch_json = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"fetch-json","fetch-json",245934686));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var request_query_string = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"request-query-string","request-query-string",-1321342848));
var session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"session-guard","session-guard",-1338532954));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var send_fetch_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"send-fetch-response!","send-fetch-response!",-1440922000));
var optional_session_guard = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"optional-session-guard","optional-session-guard",-726001966));
var error_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"error-response!","error-response!",-856339341));
var bearer_headers = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59814__$1,new cljs.core.Keyword(null,"bearer-headers","bearer-headers",79504310));
var G__59815 = app;
var G__59816 = "POST";
var G__59817 = "/api/lounge/messages";
var G__59818 = (function (request,reply){
var G__59819 = runtime;
var G__59820 = request;
var G__59821 = reply;
var G__59822 = (function (ctx){
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var session_id = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["session_id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var alias = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["alias"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "anonymous";
}
})())));
var text = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(session_id)){
var G__59824 = reply;
var G__59825 = (400);
var G__59826 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"session_id is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59824,G__59825,G__59826) : json_response_BANG_.call(null,G__59824,G__59825,G__59826));
} else {
if(clojure.string.blank_QMARK_(text)){
var G__59827 = reply;
var G__59828 = (400);
var G__59829 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"text is required"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59827,G__59828,G__59829) : json_response_BANG_.call(null,G__59827,G__59828,G__59829));
} else {
var msg = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(shadow.esm.esm_import$node_crypto.randomUUID())),new cljs.core.Keyword(null,"timestamp","timestamp",579478971),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"session_id","session_id",1584799627),session_id,new cljs.core.Keyword(null,"alias","alias",-2039751630),((clojure.string.blank_QMARK_(alias))?"anonymous":alias),new cljs.core.Keyword(null,"text","text",-1790561697),text], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.routes.memory.lounge_messages_STAR_,(function (p1__59812_SHARP_){
return cljs.core.vec(cljs.core.take_last((100),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(p1__59812_SHARP_),msg)));
}));

knoxx.backend.realtime.broadcast_ws_BANG_("lounge",msg);

var G__59830 = reply;
var G__59831 = (200);
var G__59832 = new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"message","message",-406056002),msg], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__59830,G__59831,G__59832) : json_response_BANG_.call(null,G__59830,G__59831,G__59832));

}
}
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__59819,G__59820,G__59821,G__59822) : with_request_context_BANG_.call(null,G__59819,G__59820,G__59821,G__59822));
});
return (route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__59815,G__59816,G__59817,G__59818) : route_BANG_.call(null,G__59815,G__59816,G__59817,G__59818));
});
knoxx.backend.routes.memory.register_memory_routes_BANG_ = (function knoxx$backend$routes$memory$register_memory_routes_BANG_(app,runtime,config,deps){
console.log("memory-sessions-route! =",knoxx.backend.routes.memory.memory_sessions_route_BANG_.name);

knoxx.backend.routes.memory.memory_sessions_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.memory_session_titles_status_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.memory_backfill_titles_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.memory_import_titles_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.memory_session_by_id_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.memory_search_route_BANG_(app,runtime,config,deps);

knoxx.backend.routes.memory.lounge_messages_list_route_BANG_(app,runtime,config,deps);

return knoxx.backend.routes.memory.lounge_messages_create_route_BANG_(app,runtime,config,deps);
});

//# sourceMappingURL=knoxx.backend.routes.memory.js.map
