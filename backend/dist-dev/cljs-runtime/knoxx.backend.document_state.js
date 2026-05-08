import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.util.time.js";
import "./knoxx.backend.openplanner_memory.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$node_fs$promises.js";
import "./shadow.esm.esm_import$node_path.js";
goog.provide('knoxx.backend.document_state');
if((typeof knoxx !== 'undefined') && (typeof knoxx.backend !== 'undefined') && (typeof knoxx.backend.document_state !== 'undefined') && (typeof knoxx.backend.document_state.database_state_STAR_ !== 'undefined')){
} else {
knoxx.backend.document_state.database_state_STAR_ = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
}
knoxx.backend.document_state.js_array_seq = (function knoxx$backend$document_state$js_array_seq(arr){
if((!((arr == null)))){
var iter__5628__auto__ = (function knoxx$backend$document_state$js_array_seq_$_iter__58771(s__58772){
return (new cljs.core.LazySeq(null,(function (){
var s__58772__$1 = s__58772;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__58772__$1);
if(temp__5825__auto__){
var s__58772__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__58772__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__58772__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__58775 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__58774 = (0);
while(true){
if((i__58774 < size__5627__auto__)){
var i = cljs.core._nth(c__5626__auto__,i__58774);
cljs.core.chunk_append(b__58775,(arr[i]));

var G__59150 = (i__58774 + (1));
i__58774 = G__59150;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__58775),knoxx$backend$document_state$js_array_seq_$_iter__58771(cljs.core.chunk_rest(s__58772__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__58775),null);
}
} else {
var i = cljs.core.first(s__58772__$2);
return cljs.core.cons((arr[i]),knoxx$backend$document_state$js_array_seq_$_iter__58771(cljs.core.rest(s__58772__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(cljs.core.range.cljs$core$IFn$_invoke$arity$1(arr.length));
} else {
return null;
}
});
knoxx.backend.document_state.request_session_id = (function knoxx$backend$document_state$request_session_id(request){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (request["headers"]["x-knoxx-session-id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
});
knoxx.backend.document_state.database_root_dir = (function knoxx$backend$document_state$database_root_dir(_runtime,config){
return shadow.esm.esm_import$node_path.resolve(new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config),".knoxx","databases");
});
knoxx.backend.document_state.database_docs_dir = (function knoxx$backend$document_state$database_docs_dir(runtime,config,db_id){
return shadow.esm.esm_import$node_path.join(knoxx.backend.document_state.database_root_dir(runtime,config),db_id,"docs");
});
knoxx.backend.document_state.database_owner_key = (function knoxx$backend$document_state$database_owner_key(auth_context){
var or__5142__auto__ = (function (){var G__58835 = knoxx.backend.authz.ctx_org_id(auth_context);
var G__58835__$1 = (((G__58835 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58835)));
if((G__58835__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__58835__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "__global__";
}
});
knoxx.backend.document_state.default_database_id = (function knoxx$backend$document_state$default_database_id(auth_context){
var temp__5823__auto__ = (function (){var G__58838 = knoxx.backend.authz.ctx_org_id(auth_context);
var G__58838__$1 = (((G__58838 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58838)));
if((G__58838__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__58838__$1);
}
})();
if(cljs.core.truth_(temp__5823__auto__)){
var org_id = temp__5823__auto__;
return (""+"default:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id));
} else {
return "default";
}
});
knoxx.backend.document_state.default_database_profile = (function knoxx$backend$document_state$default_database_profile(var_args){
var G__58842 = arguments.length;
switch (G__58842) {
case 2:
return knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var db_id = knoxx.backend.document_state.default_database_id(auth_context);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"privateToSession","privateToSession",-73446717),new cljs.core.Keyword(null,"orgId","orgId",-73585595),new cljs.core.Keyword(null,"forumMode","forumMode",2078997894),new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882),new cljs.core.Keyword(null,"ownerSessionId","ownerSessionId",1073095462),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"orgSlug","orgSlug",-138550998),new cljs.core.Keyword(null,"useLocalDocsBaseUrl","useLocalDocsBaseUrl",-1109521974),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),new cljs.core.Keyword(null,"ownerUserId","ownerUserId",-1250504308),new cljs.core.Keyword(null,"qdrantCollection","qdrantCollection",226372371),new cljs.core.Keyword(null,"ownerMembershipId","ownerMembershipId",2136804692),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"publicDocsBaseUrl","publicDocsBaseUrl",-1708554755)],[false,knoxx.backend.authz.ctx_org_id(auth_context),false,knoxx.backend.document_state.database_docs_dir(runtime,config,db_id),null,"Workspace Docs",knoxx.backend.authz.ctx_org_slug(auth_context),true,knoxx.backend.util.time.now_iso(),knoxx.backend.authz.ctx_user_id(auth_context),new cljs.core.Keyword(null,"collection-name","collection-name",600435477).cljs$core$IFn$_invoke$arity$1(config),knoxx.backend.authz.ctx_membership_id(auth_context),db_id,""]);
}));

(knoxx.backend.document_state.default_database_profile.cljs$lang$maxFixedArity = 3);

knoxx.backend.document_state.default_database_record = (function knoxx$backend$document_state$default_database_record(){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"history","history",-247395220),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"progress","progress",244323547),null,new cljs.core.Keyword(null,"lastRequest","lastRequest",-738015741),null], null);
});
knoxx.backend.document_state.ensure_database_state_BANG_ = (function knoxx$backend$document_state$ensure_database_state_BANG_(var_args){
var G__58869 = arguments.length;
switch (G__58869) {
case 2:
return knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.document_state.database_state_STAR_))){
} else {
cljs.core.reset_BANG_(knoxx.backend.document_state.database_state_STAR_,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"active-id","active-id",-59238656),"default",new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"profiles","profiles",507634713),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"records","records",1326822832),cljs.core.PersistentArrayMap.EMPTY], null));
}

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.database_state_STAR_,(function (state){
var state__$1 = cljs.core.merge.cljs$core$IFn$_invoke$arity$variadic(cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"active-id","active-id",-59238656),"default",new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"profiles","profiles",507634713),cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"records","records",1326822832),cljs.core.PersistentArrayMap.EMPTY], null),state], 0));
var global_default = knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
var state__$2 = cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(state__$1,new cljs.core.Keyword(null,"profiles","profiles",507634713),(function (p1__58854_SHARP_){
if(cljs.core.contains_QMARK_(p1__58854_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(global_default))){
return p1__58854_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__58854_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(global_default),global_default);
}
})),new cljs.core.Keyword(null,"records","records",1326822832),(function (p1__58856_SHARP_){
if(cljs.core.contains_QMARK_(p1__58856_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(global_default))){
return p1__58856_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__58856_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(global_default),knoxx.backend.document_state.default_database_record());
}
}));
var owner_key = knoxx.backend.document_state.database_owner_key(auth_context);
var scoped_default = knoxx.backend.document_state.default_database_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var G__58889 = state__$2;
var G__58889__$1 = (cljs.core.truth_(auth_context)?cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(cljs.core.update.cljs$core$IFn$_invoke$arity$3(G__58889,new cljs.core.Keyword(null,"profiles","profiles",507634713),(function (p1__58859_SHARP_){
if(cljs.core.contains_QMARK_(p1__58859_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(scoped_default))){
return p1__58859_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__58859_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(scoped_default),scoped_default);
}
})),new cljs.core.Keyword(null,"records","records",1326822832),(function (p1__58860_SHARP_){
if(cljs.core.contains_QMARK_(p1__58860_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(scoped_default))){
return p1__58860_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__58860_SHARP_,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(scoped_default),knoxx.backend.document_state.default_database_record());
}
})),new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),(function (p1__58861_SHARP_){
if(cljs.core.contains_QMARK_(p1__58861_SHARP_,owner_key)){
return p1__58861_SHARP_;
} else {
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(p1__58861_SHARP_,owner_key,new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(scoped_default));
}
})):G__58889);
if((auth_context == null)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__58889__$1,new cljs.core.Keyword(null,"active-id","active-id",-59238656),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"active-id","active-id",-59238656).cljs$core$IFn$_invoke$arity$1(state__$2);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(global_default);
}
})());
} else {
return G__58889__$1;
}
}));

return cljs.core.deref(knoxx.backend.document_state.database_state_STAR_);
}));

(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$lang$maxFixedArity = 3);

knoxx.backend.document_state.ensure_dir_BANG_ = (function knoxx$backend$document_state$ensure_dir_BANG_(_runtime,dir_path){
return shadow.esm.esm_import$node_fs$promises.mkdir(dir_path,({"recursive": true}));
});
knoxx.backend.document_state.profile_can_access_QMARK_ = (function knoxx$backend$document_state$profile_can_access_QMARK_(var_args){
var G__58902 = arguments.length;
switch (G__58902) {
case 2:
return knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (profile,session_id){
return knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,null,session_id);
}));

(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3 = (function (profile,auth_context,session_id){
var org_id = (function (){var G__58904 = new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(profile);
var G__58904__$1 = (((G__58904 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58904)));
if((G__58904__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__58904__$1);
}
})();
var org_allowed_QMARK_ = (cljs.core.truth_(org_id)?(((auth_context == null)) || (((knoxx.backend.authz.system_admin_QMARK_(auth_context)) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(org_id,(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.authz.ctx_org_id(auth_context)))))))):(((auth_context == null)) || (knoxx.backend.authz.system_admin_QMARK_(auth_context))));
var session_allowed_QMARK_ = ((cljs.core.not(new cljs.core.Keyword(null,"privateToSession","privateToSession",-73446717).cljs$core$IFn$_invoke$arity$1(profile))) || (((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"ownerSessionId","ownerSessionId",1073095462).cljs$core$IFn$_invoke$arity$1(profile))))) || (cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"ownerSessionId","ownerSessionId",1073095462).cljs$core$IFn$_invoke$arity$1(profile))),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)))))));
return ((org_allowed_QMARK_) && (session_allowed_QMARK_));
}));

(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$lang$maxFixedArity = 3);

knoxx.backend.document_state.effective_active_database_id = (function knoxx$backend$document_state$effective_active_database_id(var_args){
var G__58922 = arguments.length;
switch (G__58922) {
case 3:
return knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,request){
return knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4(runtime,config,request,null);
}));

(knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,request,auth_context){
var state = knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var session_id = knoxx.backend.document_state.request_session_id(request);
var owner_key = knoxx.backend.document_state.database_owner_key(auth_context);
var default_id = knoxx.backend.document_state.default_database_id(auth_context);
var active_id = (cljs.core.truth_(auth_context)?(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),owner_key], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default_id;
}
})():(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"active-id","active-id",-59238656).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default_id;
}
})());
var active_profile = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),active_id], null));
if(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(active_profile,auth_context,session_id)){
return active_id;
} else {
var or__5142__auto__ = cljs.core.some((function (p__58931){
var vec__58933 = p__58931;
var db_id = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58933,(0),null);
var profile = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58933,(1),null);
if(knoxx.backend.document_state.profile_can_access_QMARK_.cljs$core$IFn$_invoke$arity$3(profile,auth_context,session_id)){
return db_id;
} else {
return null;
}
}),new cljs.core.Keyword(null,"profiles","profiles",507634713).cljs$core$IFn$_invoke$arity$1(state));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return default_id;
}
}
}));

(knoxx.backend.document_state.effective_active_database_id.cljs$lang$maxFixedArity = 4);

knoxx.backend.document_state.active_database_profile = (function knoxx$backend$document_state$active_database_profile(var_args){
var G__58957 = arguments.length;
switch (G__58957) {
case 3:
return knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,request){
return knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,null);
}));

(knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,request,auth_context){
var state = knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var db_id = knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4(runtime,config,request,auth_context);
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),db_id], null));
}));

(knoxx.backend.document_state.active_database_profile.cljs$lang$maxFixedArity = 4);

knoxx.backend.document_state.normalize_relative_path = (function knoxx$backend$document_state$normalize_relative_path(value){
return clojure.string.replace(clojure.string.replace((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)),/\\/,"/"),/^\/+/,"");
});
knoxx.backend.document_state.sanitize_upload_name = (function knoxx$backend$document_state$sanitize_upload_name(name){
var trimmed = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name)));
var cleaned = clojure.string.replace(clojure.string.replace(clojure.string.replace(trimmed,/[\\\/]+/,"-"),/[^A-Za-z0-9._ -]/,"-"),/\s+/," ");
if(clojure.string.blank_QMARK_(cleaned)){
return "upload.bin";
} else {
return cleaned;
}
});
knoxx.backend.document_state.create_db_id = (function knoxx$backend$document_state$create_db_id(_runtime,name){
var base = clojure.string.replace(clojure.string.replace(clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(name))),/[^a-z0-9]+/,"-"),/^-+|-+$/,"");
var prefix = ((clojure.string.blank_QMARK_(base))?"db":base);
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+"-"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(shadow.esm.esm_import$node_crypto.randomUUID().slice((0),(8))));
});
knoxx.backend.document_state.list_files_recursive_BANG_ = (function knoxx$backend$document_state$list_files_recursive_BANG_(runtime,dir_path){
var read_promise = shadow.esm.esm_import$node_fs$promises.readdir(dir_path,({"withFileTypes": true}));
return read_promise.then((function (entries){
return Promise.all(cljs.core.clj__GT_js((function (){var iter__5628__auto__ = (function knoxx$backend$document_state$list_files_recursive_BANG__$_iter__58979(s__58980){
return (new cljs.core.LazySeq(null,(function (){
var s__58980__$1 = s__58980;
while(true){
var temp__5825__auto__ = cljs.core.seq(s__58980__$1);
if(temp__5825__auto__){
var s__58980__$2 = temp__5825__auto__;
if(cljs.core.chunked_seq_QMARK_(s__58980__$2)){
var c__5626__auto__ = cljs.core.chunk_first(s__58980__$2);
var size__5627__auto__ = cljs.core.count(c__5626__auto__);
var b__58982 = cljs.core.chunk_buffer(size__5627__auto__);
if((function (){var i__58981 = (0);
while(true){
if((i__58981 < size__5627__auto__)){
var entry = cljs.core._nth(c__5626__auto__,i__58981);
cljs.core.chunk_append(b__58982,(function (){var full_path = shadow.esm.esm_import$node_path.join(dir_path,entry.name);
if(cljs.core.truth_(entry.isDirectory())){
return (knoxx.backend.document_state.list_files_recursive_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.document_state.list_files_recursive_BANG_.cljs$core$IFn$_invoke$arity$2(runtime,full_path) : knoxx.backend.document_state.list_files_recursive_BANG_.call(null,runtime,full_path));
} else {
return Promise.resolve([full_path]);
}
})());

var G__59230 = (i__58981 + (1));
i__58981 = G__59230;
continue;
} else {
return true;
}
break;
}
})()){
return cljs.core.chunk_cons(cljs.core.chunk(b__58982),knoxx$backend$document_state$list_files_recursive_BANG__$_iter__58979(cljs.core.chunk_rest(s__58980__$2)));
} else {
return cljs.core.chunk_cons(cljs.core.chunk(b__58982),null);
}
} else {
var entry = cljs.core.first(s__58980__$2);
return cljs.core.cons((function (){var full_path = shadow.esm.esm_import$node_path.join(dir_path,entry.name);
if(cljs.core.truth_(entry.isDirectory())){
return (knoxx.backend.document_state.list_files_recursive_BANG_.cljs$core$IFn$_invoke$arity$2 ? knoxx.backend.document_state.list_files_recursive_BANG_.cljs$core$IFn$_invoke$arity$2(runtime,full_path) : knoxx.backend.document_state.list_files_recursive_BANG_.call(null,runtime,full_path));
} else {
return Promise.resolve([full_path]);
}
})(),knoxx$backend$document_state$list_files_recursive_BANG__$_iter__58979(cljs.core.rest(s__58980__$2)));
}
} else {
return null;
}
break;
}
}),null,null));
});
return iter__5628__auto__(knoxx.backend.document_state.js_array_seq(entries));
})())).then((function (nested){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentVector.EMPTY,cljs.core.mapcat.cljs$core$IFn$_invoke$arity$1(knoxx.backend.document_state.js_array_seq),knoxx.backend.document_state.js_array_seq(nested));
}));
})).catch((function (err){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((err["code"]),"ENOENT")){
return Promise.resolve(cljs.core.PersistentVector.EMPTY);
} else {
return Promise.reject(err);
}
}));
});
knoxx.backend.document_state.file_chunk_count = (function knoxx$backend$document_state$file_chunk_count(text){
return cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),Math.ceil((cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),(((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))).length)) / (1800))));
});
knoxx.backend.document_state.indexed_meta = (function knoxx$backend$document_state$indexed_meta(runtime,config,db_id,rel_path){
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$2(runtime,config),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"indexed","indexed",390758624),rel_path], null));
});
knoxx.backend.document_state.document_entry_BANG_ = (function knoxx$backend$document_state$document_entry_BANG_(runtime,config,profile,db_id,abs_path){
var docs_path = new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile);
return shadow.esm.esm_import$node_fs$promises.stat(abs_path).then((function (stats){
var rel_path = knoxx.backend.document_state.normalize_relative_path(shadow.esm.esm_import$node_path.relative(docs_path,abs_path));
var meta = knoxx.backend.document_state.indexed_meta(runtime,config,db_id,rel_path);
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"name","name",1843675177),shadow.esm.esm_import$node_path.basename(abs_path),new cljs.core.Keyword(null,"relativePath","relativePath",-608773918),rel_path,new cljs.core.Keyword(null,"size","size",1098693007),(function (){var or__5142__auto__ = (stats["size"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.boolean$(meta),new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666).cljs$core$IFn$_invoke$arity$1(meta);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"indexedAt","indexedAt",323997319),new cljs.core.Keyword(null,"indexedAt","indexedAt",323997319).cljs$core$IFn$_invoke$arity$1(meta)], null);
}));
});
knoxx.backend.document_state.list_documents_BANG_ = (function knoxx$backend$document_state$list_documents_BANG_(var_args){
var G__59029 = arguments.length;
switch (G__59029) {
case 3:
return knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,request){
return knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$4(runtime,config,request,null);
}));

(knoxx.backend.document_state.list_documents_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,request,auth_context){
var profile = knoxx.backend.document_state.active_database_profile.cljs$core$IFn$_invoke$arity$4(runtime,config,request,auth_context);
var db_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile);
return knoxx.backend.document_state.ensure_dir_BANG_(runtime,new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile)).then((function (){
return knoxx.backend.document_state.list_files_recursive_BANG_(runtime,new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile));
})).then((function (paths){
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p1__59026_SHARP_){
return knoxx.backend.document_state.document_entry_BANG_(runtime,config,profile,db_id,p1__59026_SHARP_);
}),paths))).then((function (items){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"documents","documents",-1582333455),cljs.core.vec(cljs.core.sort_by.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"relativePath","relativePath",-608773918),knoxx.backend.document_state.js_array_seq(items)))], null);
}));
}));
}));

(knoxx.backend.document_state.list_documents_BANG_.cljs$lang$maxFixedArity = 4);

knoxx.backend.document_state.active_record = (function knoxx$backend$document_state$active_record(var_args){
var G__59048 = arguments.length;
switch (G__59048) {
case 3:
return knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,request){
return knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4(runtime,config,request,null);
}));

(knoxx.backend.document_state.active_record.cljs$core$IFn$_invoke$arity$4 = (function (runtime,config,request,auth_context){
var db_id = knoxx.backend.document_state.effective_active_database_id.cljs$core$IFn$_invoke$arity$4(runtime,config,request,auth_context);
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id], null));
}));

(knoxx.backend.document_state.active_record.cljs$lang$maxFixedArity = 4);

knoxx.backend.document_state.active_agent_profile = (function knoxx$backend$document_state$active_agent_profile(var_args){
var G__59062 = arguments.length;
switch (G__59062) {
case 2:
return knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.document_state.active_agent_profile.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var state = knoxx.backend.document_state.ensure_database_state_BANG_.cljs$core$IFn$_invoke$arity$3(runtime,config,auth_context);
var owner_key = knoxx.backend.document_state.database_owner_key(auth_context);
var active_id = (cljs.core.truth_(auth_context)?(function (){var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"active-ids","active-ids",-958452414),owner_key], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return knoxx.backend.document_state.default_database_id(auth_context);
}
})():(function (){var or__5142__auto__ = new cljs.core.Keyword(null,"active-id","active-id",-59238656).cljs$core$IFn$_invoke$arity$1(state);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "default";
}
})());
var or__5142__auto__ = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),active_id], null));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),knoxx.backend.document_state.default_database_id(auth_context)], null));
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return cljs.core.get_in.cljs$core$IFn$_invoke$arity$2(state,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"profiles","profiles",507634713),"default"], null));
}
}
}));

(knoxx.backend.document_state.active_agent_profile.cljs$lang$maxFixedArity = 3);

/**
 * Ingest documents into OpenPlanner for embedding and vector storage.
 * Replaces previous metadata-only tracking with OpenPlanner /v1/documents indexing.
 */
knoxx.backend.document_state.start_document_ingestion_BANG_ = (function knoxx$backend$document_state$start_document_ingestion_BANG_(runtime,config,profile,p__59077){
var map__59078 = p__59077;
var map__59078__$1 = cljs.core.__destructure_map(map__59078);
var full = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59078__$1,new cljs.core.Keyword(null,"full","full",436801220));
var selected_files = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59078__$1,new cljs.core.Keyword(null,"selected-files","selected-files",1045525459));
var db_id = new cljs.core.Keyword(null,"id","id",-1388402092).cljs$core$IFn$_invoke$arity$1(profile);
var docs_path = new cljs.core.Keyword(null,"docsPath","docsPath",-1515329882).cljs$core$IFn$_invoke$arity$1(profile);
var project = (function (){var or__5142__auto__ = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "devel";
}
})();
return knoxx.backend.document_state.list_files_recursive_BANG_(runtime,docs_path).then((function (all_abs){
var wanted = (cljs.core.truth_(full)?null:cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentHashSet.EMPTY,cljs.core.map.cljs$core$IFn$_invoke$arity$1(knoxx.backend.document_state.normalize_relative_path),selected_files));
var queue = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (p__59081){
var map__59082 = p__59081;
var map__59082__$1 = cljs.core.__destructure_map(map__59082);
var rel = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59082__$1,new cljs.core.Keyword(null,"rel","rel",1378823488));
var or__5142__auto__ = full;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.contains_QMARK_(wanted,rel);
}
}),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (abs){
var rel = knoxx.backend.document_state.normalize_relative_path(shadow.esm.esm_import$node_path.relative(docs_path,abs));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"abs","abs",-246026477),abs,new cljs.core.Keyword(null,"rel","rel",1378823488),rel], null);
}),all_abs)));
var started_at = knoxx.backend.util.time.now_iso();
var total = cljs.core.count(queue);
var mode = (cljs.core.truth_(full)?"full":"selected");
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.document_state.database_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"progress","progress",244323547)], null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"startedAt","startedAt",-390697471),new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"percentPrecise","percentPrecise",-1015886804),new cljs.core.Keyword(null,"stale","stale",395586896),new cljs.core.Keyword(null,"active","active",1895962068),new cljs.core.Keyword(null,"currentFile","currentFile",-711998603),new cljs.core.Keyword(null,"filesUpdated","filesUpdated",46752279),new cljs.core.Keyword(null,"totalChunks","totalChunks",-2015469577),new cljs.core.Keyword(null,"percent","percent",2031453817),new cljs.core.Keyword(null,"processedChunks","processedChunks",516451037)],[started_at,(0),mode,(0),false,true,(function (){var G__59084 = queue;
var G__59084__$1 = (((G__59084 == null))?null:cljs.core.first(G__59084));
if((G__59084__$1 == null)){
return null;
} else {
return new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(G__59084__$1);
}
})(),(0),total,(0),(0)]));

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.document_state.database_state_STAR_,cljs.core.assoc_in,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"lastRequest","lastRequest",-738015741)], null),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"full","full",436801220),cljs.core.boolean$(full),new cljs.core.Keyword(null,"selectedFiles","selectedFiles",-2058493306),cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"rel","rel",1378823488),queue))], null));

if((total === (0))){
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.database_state_STAR_,(function (state){
return cljs.core.update_in.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc_in(state,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"progress","progress",244323547)], null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"startedAt","startedAt",-390697471),new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"percentPrecise","percentPrecise",-1015886804),new cljs.core.Keyword(null,"stale","stale",395586896),new cljs.core.Keyword(null,"active","active",1895962068),new cljs.core.Keyword(null,"currentFile","currentFile",-711998603),new cljs.core.Keyword(null,"filesUpdated","filesUpdated",46752279),new cljs.core.Keyword(null,"totalChunks","totalChunks",-2015469577),new cljs.core.Keyword(null,"percent","percent",2031453817),new cljs.core.Keyword(null,"processedChunks","processedChunks",516451037)],[started_at,(0),mode,(100),false,false,null,(0),(0),(100),(0)])),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"history","history",-247395220)], null),(function (history){
return cljs.core.vec(cljs.core.take_last((50),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(history),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),shadow.esm.esm_import$node_crypto.randomUUID(),new cljs.core.Keyword(null,"completedAt","completedAt",-526048405),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"chunksUpserted","chunksUpserted",-873003328),(0),new cljs.core.Keyword(null,"processedChunks","processedChunks",516451037),(0),new cljs.core.Keyword(null,"filesUpdated","filesUpdated",46752279),(0),new cljs.core.Keyword(null,"durationSeconds","durationSeconds",-316133524),(0),new cljs.core.Keyword(null,"errors","errors",-908790718),(0)], null))));
}));
}));

return Promise.resolve(new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"started","started",585705024),true,new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"selectedFiles","selectedFiles",-2058493306),cljs.core.PersistentVector.EMPTY,new cljs.core.Keyword(null,"indexedCount","indexedCount",1873958532),(0),new cljs.core.Keyword(null,"failedCount","failedCount",-487850042),(0),new cljs.core.Keyword(null,"openplanner","openplanner",-175854128),true], null));
} else {
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__59089){
var map__59090 = p__59089;
var map__59090__$1 = cljs.core.__destructure_map(map__59090);
var abs = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59090__$1,new cljs.core.Keyword(null,"abs","abs",-246026477));
var rel = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59090__$1,new cljs.core.Keyword(null,"rel","rel",1378823488));
return shadow.esm.esm_import$node_fs$promises.readFile(abs,"utf8").then((function (content){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel,new cljs.core.Keyword(null,"content","content",15833224),content,new cljs.core.Keyword(null,"error","error",-978969032),false], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel,new cljs.core.Keyword(null,"content","content",15833224),null,new cljs.core.Keyword(null,"error","error",-978969032),true,new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
}),queue))).then((function (read_results){
var items = cljs.core.vec(knoxx.backend.document_state.js_array_seq(read_results));
var read_failed = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"error","error",-978969032),items));
var valid_items = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"error","error",-978969032),items));
var documents = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (item){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"id","id",-1388402092),(""+"knoxx:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(db_id)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item))),new cljs.core.Keyword(null,"rel-path","rel-path",593215642),new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item),new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(item),new cljs.core.Keyword(null,"source-path","source-path",-1955873712),new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item),new cljs.core.Keyword(null,"project","project",1124394579),project,new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"database-id","database-id",1883826326),db_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(profile)], null)], null);
}),valid_items);
return knoxx.backend.openplanner_memory.batch_upsert_openplanner_documents_BANG_(config,documents,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"concurrency","concurrency",595096228),(3),new cljs.core.Keyword(null,"project","project",1124394579),project,new cljs.core.Keyword(null,"visibility","visibility",1338380893),"internal",new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"database-id","database-id",1883826326),db_id,new cljs.core.Keyword(null,"org-id","org-id",1485182668),new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(profile)], null)], null)).then((function (index_result){
var successful_rels = cljs.core.set(cljs.core.keep.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"rel-path","rel-path",593215642),new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(index_result)));
var indexed_items = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2((function (item){
return cljs.core.contains_QMARK_(successful_rels,new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item));
}),valid_items));
var indexed_count = cljs.core.count(indexed_items);
var failed_count = (cljs.core.count(read_failed) + new cljs.core.Keyword(null,"failed-count","failed-count",-366647954).cljs$core$IFn$_invoke$arity$2(index_result,(0)));
var chunk_count = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3(cljs.core._PLUS_,(0),cljs.core.map.cljs$core$IFn$_invoke$arity$2(cljs.core.comp.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.file_chunk_count,new cljs.core.Keyword(null,"content","content",15833224)),indexed_items));
var started_ms = (new Date(started_at)).getTime();
var duration_seconds = cljs.core.max.cljs$core$IFn$_invoke$arity$2((0),Math.round(((Date.now() - started_ms) / (1000))));
var history_item = new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"id","id",-1388402092),shadow.esm.esm_import$node_crypto.randomUUID(),new cljs.core.Keyword(null,"completedAt","completedAt",-526048405),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"chunksUpserted","chunksUpserted",-873003328),chunk_count,new cljs.core.Keyword(null,"processedChunks","processedChunks",516451037),total,new cljs.core.Keyword(null,"filesUpdated","filesUpdated",46752279),indexed_count,new cljs.core.Keyword(null,"durationSeconds","durationSeconds",-316133524),duration_seconds,new cljs.core.Keyword(null,"errors","errors",-908790718),failed_count], null);
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.document_state.database_state_STAR_,(function (state){
var state_with_index = cljs.core.reduce.cljs$core$IFn$_invoke$arity$3((function (acc,item){
return cljs.core.assoc_in(acc,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"indexed","indexed",390758624),new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item)], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"chunkCount","chunkCount",1427535666),knoxx.backend.document_state.file_chunk_count(new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(item)),new cljs.core.Keyword(null,"indexedAt","indexedAt",323997319),knoxx.backend.util.time.now_iso(),new cljs.core.Keyword(null,"openplanner","openplanner",-175854128),true], null));
}),state,indexed_items);
return cljs.core.update_in.cljs$core$IFn$_invoke$arity$3(cljs.core.assoc_in(state_with_index,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"progress","progress",244323547)], null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"startedAt","startedAt",-390697471),new cljs.core.Keyword(null,"errors","errors",-908790718),new cljs.core.Keyword(null,"mode","mode",654403691),new cljs.core.Keyword(null,"percentPrecise","percentPrecise",-1015886804),new cljs.core.Keyword(null,"stale","stale",395586896),new cljs.core.Keyword(null,"active","active",1895962068),new cljs.core.Keyword(null,"currentFile","currentFile",-711998603),new cljs.core.Keyword(null,"filesUpdated","filesUpdated",46752279),new cljs.core.Keyword(null,"totalChunks","totalChunks",-2015469577),new cljs.core.Keyword(null,"percent","percent",2031453817),new cljs.core.Keyword(null,"processedChunks","processedChunks",516451037)],[started_at,failed_count,mode,(100),false,false,null,indexed_count,total,(100),total])),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"records","records",1326822832),db_id,new cljs.core.Keyword(null,"history","history",-247395220)], null),(function (history){
return cljs.core.vec(cljs.core.take_last((50),cljs.core.conj.cljs$core$IFn$_invoke$arity$2(cljs.core.vec(history),history_item)));
}));
}));

return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"started","started",585705024),true,new cljs.core.Keyword(null,"mode","mode",654403691),mode,new cljs.core.Keyword(null,"selectedFiles","selectedFiles",-2058493306),cljs.core.vec(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"rel","rel",1378823488),queue)),new cljs.core.Keyword(null,"indexedCount","indexedCount",1873958532),indexed_count,new cljs.core.Keyword(null,"failedCount","failedCount",-487850042),failed_count,new cljs.core.Keyword(null,"openplanner","openplanner",-175854128),true], null);
}));
}));
}
}));
});
/**
 * Check if a file extension is a text-like format suitable for ingestion.
 */
knoxx.backend.document_state.text_like_path_QMARK_ = (function knoxx$backend$document_state$text_like_path_QMARK_(path_str){
var lower = clojure.string.lower_case((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(path_str)));
var idx = lower.lastIndexOf(".");
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(idx,(-1))){
return true;
} else {
return cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 44, [".ini",null,".hcl",null,".json",null,".cljc",null,".sh",null,".yaml",null,".py",null,".tsx",null,".go",null,".zsh",null,".md",null,".log",null,".rs",null,".jsx",null,".js",null,".tf",null,".env",null,".toml",null,".php",null,".rb",null,".txt",null,".conf",null,".java",null,".graphql",null,".proto",null,".csv",null,".gql",null,".mdx",null,".ts",null,".htm",null,".less",null,".html",null,".css",null,".cljs",null,".scss",null,".clj",null,".org",null,".mjs",null,".cjs",null,".sql",null,".yml",null,".bash",null,".edn",null,".xml",null], null), null),lower.slice(idx));
}
});
/**
 * Immediately ingest specific workspace files into OpenPlanner, bypassing queues.
 * Takes workspace-relative paths, reads them from disk, and sends to /v1/documents.
 *   Returns {:ok true, :indexed N, :failed M, :files [...]} summary.
 */
knoxx.backend.document_state.priority_ingest_workspace_files_BANG_ = (function knoxx$backend$document_state$priority_ingest_workspace_files_BANG_(_runtime,config,p__59112){
var map__59114 = p__59112;
var map__59114__$1 = cljs.core.__destructure_map(map__59114);
var paths = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59114__$1,new cljs.core.Keyword(null,"paths","paths",-1807389588));
var project = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59114__$1,new cljs.core.Keyword(null,"project","project",1124394579));
var source = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__59114__$1,new cljs.core.Keyword(null,"source","source",-433931539));
var workspace_root = new cljs.core.Keyword(null,"workspace-root","workspace-root",-488911547).cljs$core$IFn$_invoke$arity$1(config);
var project__$1 = (function (){var or__5142__auto__ = project;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"project-name","project-name",1486861539).cljs$core$IFn$_invoke$arity$1(config);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "devel";
}
}
})();
var source__$1 = (function (){var or__5142__auto__ = source;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "knoxx-priority-ingest";
}
})();
return Promise.all(cljs.core.clj__GT_js(cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (rel_path){
var abs_path = shadow.esm.esm_import$node_path.resolve(workspace_root,rel_path);
return shadow.esm.esm_import$node_fs$promises.stat(abs_path).then((function (stat){
if(cljs.core.truth_((function (){var and__5140__auto__ = stat.isFile();
if(cljs.core.truth_(and__5140__auto__)){
return knoxx.backend.document_state.text_like_path_QMARK_(abs_path);
} else {
return and__5140__auto__;
}
})())){
return shadow.esm.esm_import$node_fs$promises.readFile(abs_path,"utf8").then((function (content){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel_path,new cljs.core.Keyword(null,"abs","abs",-246026477),abs_path,new cljs.core.Keyword(null,"content","content",15833224),content,new cljs.core.Keyword(null,"size","size",1098693007),(function (){var or__5142__auto__ = stat.size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"error","error",-978969032),false], null);
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel_path,new cljs.core.Keyword(null,"abs","abs",-246026477),abs_path,new cljs.core.Keyword(null,"content","content",15833224),null,new cljs.core.Keyword(null,"size","size",1098693007),(0),new cljs.core.Keyword(null,"error","error",-978969032),true,new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
} else {
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel_path,new cljs.core.Keyword(null,"abs","abs",-246026477),abs_path,new cljs.core.Keyword(null,"content","content",15833224),null,new cljs.core.Keyword(null,"size","size",1098693007),(function (){var or__5142__auto__ = stat.size;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"error","error",-978969032),true,new cljs.core.Keyword(null,"detail","detail",-1545345025),"binary or unsupported file type"], null);
}
})).catch((function (err){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"rel","rel",1378823488),rel_path,new cljs.core.Keyword(null,"abs","abs",-246026477),abs_path,new cljs.core.Keyword(null,"content","content",15833224),null,new cljs.core.Keyword(null,"size","size",1098693007),(0),new cljs.core.Keyword(null,"error","error",-978969032),true,new cljs.core.Keyword(null,"detail","detail",-1545345025),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(err))], null);
}));
}),paths))).then((function (read_results){
var items = cljs.core.vec(knoxx.backend.document_state.js_array_seq(read_results));
var valid = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"error","error",-978969032),items));
var failed_reads = cljs.core.vec(cljs.core.filter.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"error","error",-978969032),items));
var docs = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (item){
var ext = (function (){var G__59131 = clojure.string.lower_case(new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item));
var G__59131__$1 = (((G__59131 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__59131,/\./));
if((G__59131__$1 == null)){
return null;
} else {
return cljs.core.last(G__59131__$1);
}
})();
var kind = ((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 15, ["js",null,"cljc",null,"rs",null,"ts",null,"mjs",null,"py",null,"cljs",null,"cjs",null,"clj",null,"java",null,"jsx",null,"php",null,"go",null,"tsx",null,"rb",null], null), null),ext))?"code":((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 6, ["md",null,"rst",null,"txt",null,"mdx",null,".org",null,"adoc",null], null), null),ext))?"docs":((cljs.core.contains_QMARK_(new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 7, ["json",null,"toml",null,"yml",null,"yaml",null,"conf",null,"env",null,"ini",null], null), null),ext))?"config":"docs"
)));
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"content","content",15833224),new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.Keyword(null,"source","source",-433931539),new cljs.core.Keyword(null,"source-path","source-path",-1955873712),new cljs.core.Keyword(null,"project","project",1124394579),new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.Keyword(null,"rel-path","rel-path",593215642),new cljs.core.Keyword(null,"visibility","visibility",1338380893)],[new cljs.core.Keyword(null,"content","content",15833224).cljs$core$IFn$_invoke$arity$1(item),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"priority-ingest","priority-ingest",869114081),true,new cljs.core.Keyword(null,"size","size",1098693007),new cljs.core.Keyword(null,"size","size",1098693007).cljs$core$IFn$_invoke$arity$1(item)], null),source__$1,new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item),project__$1,(""+"knoxx-priority:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item))),kind,new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(item),"internal"]);
}),valid);
if(cljs.core.seq(docs)){
return knoxx.backend.openplanner_memory.batch_upsert_openplanner_documents_BANG_(config,docs,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"concurrency","concurrency",595096228),(5),new cljs.core.Keyword(null,"project","project",1124394579),project__$1,new cljs.core.Keyword(null,"visibility","visibility",1338380893),"internal",new cljs.core.Keyword(null,"extra","extra",1612569067),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"source","source",-433931539),source__$1], null)], null)).then((function (index_result){
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"indexed","indexed",390758624),cljs.core.count(new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(index_result)),new cljs.core.Keyword(null,"failed","failed",-1397425762),(cljs.core.count(failed_reads) + new cljs.core.Keyword(null,"failed-count","failed-count",-366647954).cljs$core$IFn$_invoke$arity$2(index_result,(0))),new cljs.core.Keyword(null,"total","total",1916810418),cljs.core.count(paths),new cljs.core.Keyword(null,"files","files",-472457450),cljs.core.concat.cljs$core$IFn$_invoke$arity$variadic(cljs.core.map.cljs$core$IFn$_invoke$arity$2(new cljs.core.Keyword(null,"rel","rel",1378823488),new cljs.core.Keyword(null,"indexed","indexed",390758624).cljs$core$IFn$_invoke$arity$1(index_result)),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (f){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(f))+" (read error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"detail","detail",-1545345025).cljs$core$IFn$_invoke$arity$1(f))+")");
}),failed_reads),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (f){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"rel-path","rel-path",593215642).cljs$core$IFn$_invoke$arity$1(f))+" (index error)");
}),new cljs.core.Keyword(null,"failed","failed",-1397425762).cljs$core$IFn$_invoke$arity$1(index_result))], 0)),new cljs.core.Keyword(null,"source","source",-433931539),source__$1], null);
}));
} else {
return Promise.resolve(new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"indexed","indexed",390758624),(0),new cljs.core.Keyword(null,"failed","failed",-1397425762),cljs.core.count(failed_reads),new cljs.core.Keyword(null,"total","total",1916810418),cljs.core.count(paths),new cljs.core.Keyword(null,"files","files",-472457450),cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (f){
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"rel","rel",1378823488).cljs$core$IFn$_invoke$arity$1(f))+" ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"detail","detail",-1545345025).cljs$core$IFn$_invoke$arity$1(f))+")");
}),failed_reads),new cljs.core.Keyword(null,"source","source",-433931539),source__$1], null));
}
}));
});

//# sourceMappingURL=knoxx.backend.document_state.js.map
