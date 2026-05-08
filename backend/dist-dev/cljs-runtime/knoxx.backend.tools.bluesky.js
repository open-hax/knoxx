import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.authz.js";
import "./knoxx.backend.text.js";
import "./knoxx.backend.tools.actor_credentials.js";
import "./knoxx.backend.tools.shared.js";
goog.provide('knoxx.backend.tools.bluesky');
knoxx.backend.tools.bluesky.publish_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"text","text",-1790561697),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Bluesky post text. Keep it concise and under platform limits."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.bluesky.profile_params = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional Bluesky handle or DID. Defaults to the authenticated account."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.bluesky.search_params = new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"query","query",-1288509510),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Search query for Bluesky posts or actors."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"kind","kind",-717265803),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"posts or actors. Defaults to posts."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum results to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(25)], null)], null)], null)], null);
knoxx.backend.tools.bluesky.feed_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"actor","actor",-1830560481),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"description","description",-1428560544),"Bluesky handle or DID whose feed should be read."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum posts to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(25)], null)], null)], null)], null);
knoxx.backend.tools.bluesky.timeline_params = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"map","map",1371690461),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"limit","limit",-1355822363),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Maximum timeline posts to return."], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"int","int",-1741416922),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"min","min",444991522),(1),new cljs.core.Keyword(null,"max","max",61366548),(25)], null)], null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"cursor","cursor",1011937484),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"optional","optional",2053951509),true,new cljs.core.Keyword(null,"description","description",-1428560544),"Optional pagination cursor from a previous bluesky.timeline call."], null),new cljs.core.Keyword(null,"string","string",-1989541586)], null)], null);
knoxx.backend.tools.bluesky.bluesky_service_base_url = (function knoxx$backend$tools$bluesky$bluesky_service_base_url(){
return "https://bsky.social";
});
knoxx.backend.tools.bluesky.bluesky_public_base_url = (function knoxx$backend$tools$bluesky$bluesky_public_base_url(){
return "https://public.api.bsky.app";
});
knoxx.backend.tools.bluesky.query_url = (function knoxx$backend$tools$bluesky$query_url(base,params){
var search = (new URLSearchParams());
var seq__58909_59152 = cljs.core.seq(params);
var chunk__58911_59153 = null;
var count__58912_59154 = (0);
var i__58913_59155 = (0);
while(true){
if((i__58913_59155 < count__58912_59154)){
var vec__58926_59156 = chunk__58911_59153.cljs$core$IIndexed$_nth$arity$2(null,i__58913_59155);
var k_59157 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58926_59156,(0),null);
var v_59158 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58926_59156,(1),null);
if((((v_59158 == null)) || (((typeof v_59158 === 'string') && (clojure.string.blank_QMARK_(v_59158)))))){
} else {
search.append(cljs.core.name(k_59157),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v_59158)));
}


var G__59159 = seq__58909_59152;
var G__59160 = chunk__58911_59153;
var G__59161 = count__58912_59154;
var G__59162 = (i__58913_59155 + (1));
seq__58909_59152 = G__59159;
chunk__58911_59153 = G__59160;
count__58912_59154 = G__59161;
i__58913_59155 = G__59162;
continue;
} else {
var temp__5825__auto___59163 = cljs.core.seq(seq__58909_59152);
if(temp__5825__auto___59163){
var seq__58909_59164__$1 = temp__5825__auto___59163;
if(cljs.core.chunked_seq_QMARK_(seq__58909_59164__$1)){
var c__5673__auto___59166 = cljs.core.chunk_first(seq__58909_59164__$1);
var G__59167 = cljs.core.chunk_rest(seq__58909_59164__$1);
var G__59168 = c__5673__auto___59166;
var G__59169 = cljs.core.count(c__5673__auto___59166);
var G__59170 = (0);
seq__58909_59152 = G__59167;
chunk__58911_59153 = G__59168;
count__58912_59154 = G__59169;
i__58913_59155 = G__59170;
continue;
} else {
var vec__58951_59174 = cljs.core.first(seq__58909_59164__$1);
var k_59175 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58951_59174,(0),null);
var v_59176 = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__58951_59174,(1),null);
if((((v_59176 == null)) || (((typeof v_59176 === 'string') && (clojure.string.blank_QMARK_(v_59176)))))){
} else {
search.append(cljs.core.name(k_59175),(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(v_59176)));
}


var G__59177 = cljs.core.next(seq__58909_59164__$1);
var G__59178 = null;
var G__59179 = (0);
var G__59180 = (0);
seq__58909_59152 = G__59177;
chunk__58911_59153 = G__59178;
count__58912_59154 = G__59179;
i__58913_59155 = G__59180;
continue;
}
} else {
}
}
break;
}

var query = search.toString();
if(clojure.string.blank_QMARK_(query)){
return base;
} else {
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(base)+"?"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(query));
}
});
knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_json_fetch_BANG_(url,options,label){
return fetch(url,options).then((function (resp){
if(cljs.core.truth_(resp.ok)){
return resp.json();
} else {
return resp.text().then((function (text){
throw (new Error((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(label)+" error "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status)+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(text))));
}));
}
}));
});
knoxx.backend.tools.bluesky.bluesky_auth_config_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_auth_config_BANG_(runtime){
return knoxx.backend.tools.actor_credentials.get_credential_BANG_(runtime,"bluesky").then((function (credential){
var identifier = (function (){var or__5142__auto__ = knoxx.backend.tools.actor_credentials.secret_value.cljs$core$IFn$_invoke$arity$variadic(credential,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"identifier","identifier",-805503498),new cljs.core.Keyword(null,"handle","handle",1538948854),new cljs.core.Keyword(null,"username","username",1605666410)], 0));
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return new cljs.core.Keyword(null,"accountIdentifier","accountIdentifier",-2043083613).cljs$core$IFn$_invoke$arity$1(credential);
}
})();
var password = knoxx.backend.tools.actor_credentials.secret_value.cljs$core$IFn$_invoke$arity$variadic(credential,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"appPassword","appPassword",-387211090),new cljs.core.Keyword(null,"app-password","app-password",-2097591655),new cljs.core.Keyword(null,"password","password",417022471)], 0));
if(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(identifier)))) || (clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(password)))))){
throw (new Error("Bluesky actor credential must include identifier and appPassword."));
} else {
}

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"identifier","identifier",-805503498),identifier,new cljs.core.Keyword(null,"password","password",417022471),password], null);
}));
});
knoxx.backend.tools.bluesky.bluesky_create_session_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_create_session_BANG_(runtime){
return knoxx.backend.tools.bluesky.bluesky_auth_config_BANG_(runtime).then((function (p__58977){
var map__58978 = p__58977;
var map__58978__$1 = cljs.core.__destructure_map(map__58978);
var identifier = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58978__$1,new cljs.core.Keyword(null,"identifier","identifier",-805503498));
var password = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58978__$1,new cljs.core.Keyword(null,"password","password",417022471));
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_service_base_url())+"/xrpc/com.atproto.server.createSession"),({"method": "POST", "headers": ({"Content-Type": "application/json", "User-Agent": "Knoxx-Agent/1.0"}), "body": JSON.stringify(({"identifier": identifier, "password": password}))}),"Bluesky auth");
}));
});
knoxx.backend.tools.bluesky.bluesky_post_url = (function knoxx$backend$tools$bluesky$bluesky_post_url(handle,uri){
var post_id = (function (){var G__58985 = uri;
var G__58985__$1 = (((G__58985 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58985)));
var G__58985__$2 = (((G__58985__$1 == null))?null:clojure.string.split.cljs$core$IFn$_invoke$arity$2(G__58985__$1,/\//));
if((G__58985__$2 == null)){
return null;
} else {
return cljs.core.last(G__58985__$2);
}
})();
if((((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle)))))) && ((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(post_id)))))))){
return (""+"https://bsky.app/profile/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle)+"/post/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(post_id));
} else {
return null;
}
});
knoxx.backend.tools.bluesky.format_posts = (function knoxx$backend$tools$bluesky$format_posts(prefix,rows){
var lines = clojure.string.join.cljs$core$IFn$_invoke$arity$2("\n",cljs.core.map.cljs$core$IFn$_invoke$arity$2((function (p__58987){
var map__58988 = p__58987;
var map__58988__$1 = cljs.core.__destructure_map(map__58988);
var displayName = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58988__$1,new cljs.core.Keyword(null,"displayName","displayName",-809144601));
var handle = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58988__$1,new cljs.core.Keyword(null,"handle","handle",1538948854));
var text = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58988__$1,new cljs.core.Keyword(null,"text","text",-1790561697));
var url = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__58988__$1,new cljs.core.Keyword(null,"url","url",276297046));
return (""+"- "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = cljs.core.not_empty(displayName);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = handle;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown";
}
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle))))))?(""+" (@"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle)+")"):null))+": "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.text.clip_text.cljs$core$IFn$_invoke$arity$2((function (){var or__5142__auto__ = text;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),(220)))+cljs.core.str.cljs$core$IFn$_invoke$arity$1((((!(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url))))))?(""+"\n  "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)):null)));
}),rows));
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(prefix)+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_(lines))?null:(""+"\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(lines)))));
});
knoxx.backend.tools.bluesky.bluesky_search_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_search_BANG_(query,kind,limit){
var kind__$1 = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind,"actors"))?"actors":"posts");
var endpoint = ((cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind__$1,"actors"))?"app.bsky.actor.searchActors":"app.bsky.feed.searchPosts");
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_(knoxx.backend.tools.bluesky.query_url((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_public_base_url())+"/xrpc/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(endpoint)),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"q","q",689001697),query,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit], null)),({"method": "GET", "headers": ({"Accept": "application/json", "User-Agent": "Knoxx-Agent/1.0"})}),"Bluesky search").then((function (payload){
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(kind__$1,"actors")){
var results = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (actor){
return new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"handle","handle",1538948854),(function (){var or__5142__auto__ = (actor["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"displayName","displayName",-809144601),(function (){var or__5142__auto__ = (actor["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = (actor["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"did","did",593382517),(function (){var or__5142__auto__ = (actor["did"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"url","url",276297046),(function (){var temp__5825__auto__ = (function (){var G__58995 = (actor["handle"]);
var G__58995__$1 = (((G__58995 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__58995)));
if((G__58995__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__58995__$1);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var handle = temp__5825__auto__;
return (""+"https://bsky.app/profile/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle));
} else {
return null;
}
})()], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((payload["actors"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((payload["actors"])):cljs.core.PersistentVector.EMPTY));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"kind","kind",-717265803),kind__$1,new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
} else {
var results = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (post){
var author = (post["author"]);
var record = (post["record"]);
var handle = (function (){var or__5142__auto__ = (author["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"handle","handle",1538948854),handle,new cljs.core.Keyword(null,"displayName","displayName",-809144601),(function (){var or__5142__auto__ = (author["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"text","text",-1790561697),(function (){var or__5142__auto__ = (record["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),(function (){var or__5142__auto__ = (record["createdAt"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"uri","uri",-774711847),(function (){var or__5142__auto__ = (post["uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"url","url",276297046),knoxx.backend.tools.bluesky.bluesky_post_url(handle,(post["uri"]))], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((payload["posts"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((payload["posts"])):cljs.core.PersistentVector.EMPTY));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"kind","kind",-717265803),kind__$1,new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
}
}));
});
knoxx.backend.tools.bluesky.bluesky_profile_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_profile_BANG_(runtime,actor){
var actor__$1 = (function (){var G__59019 = actor;
var G__59019__$1 = (((G__59019 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59019)));
if((G__59019__$1 == null)){
return null;
} else {
return clojure.string.trim(G__59019__$1);
}
})();
var actor_promise = ((clojure.string.blank_QMARK_(actor__$1))?knoxx.backend.tools.bluesky.bluesky_create_session_BANG_(runtime).then((function (session){
var or__5142__auto__ = (session["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (session["did"]);
}
})):Promise.resolve(actor__$1));
return actor_promise.then((function (resolved_actor){
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_(knoxx.backend.tools.bluesky.query_url((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_public_base_url())+"/xrpc/app.bsky.actor.getProfile"),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"actor","actor",-1830560481),resolved_actor], null)),({"method": "GET", "headers": ({"Accept": "application/json", "User-Agent": "Knoxx-Agent/1.0"})}),"Bluesky profile");
})).then((function (profile){
return new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"did","did",593382517),(function (){var or__5142__auto__ = (profile["did"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"handle","handle",1538948854),(function (){var or__5142__auto__ = (profile["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"displayName","displayName",-809144601),(function (){var or__5142__auto__ = (profile["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"description","description",-1428560544),(function (){var or__5142__auto__ = (profile["description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"followersCount","followersCount",635176802),(function (){var or__5142__auto__ = (profile["followersCount"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"followsCount","followsCount",-1543777013),(function (){var or__5142__auto__ = (profile["followsCount"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"postsCount","postsCount",1128073488),(function (){var or__5142__auto__ = (profile["postsCount"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (0);
}
})(),new cljs.core.Keyword(null,"url","url",276297046),(function (){var temp__5825__auto__ = (function (){var G__59031 = (profile["handle"]);
var G__59031__$1 = (((G__59031 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__59031)));
if((G__59031__$1 == null)){
return null;
} else {
return cljs.core.not_empty(G__59031__$1);
}
})();
if(cljs.core.truth_(temp__5825__auto__)){
var handle = temp__5825__auto__;
return (""+"https://bsky.app/profile/"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(handle));
} else {
return null;
}
})()], null);
}));
});
knoxx.backend.tools.bluesky.bluesky_author_feed_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_author_feed_BANG_(actor,limit){
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_(knoxx.backend.tools.bluesky.query_url((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_public_base_url())+"/xrpc/app.bsky.feed.getAuthorFeed"),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actor","actor",-1830560481),actor,new cljs.core.Keyword(null,"limit","limit",-1355822363),limit], null)),({"method": "GET", "headers": ({"Accept": "application/json", "User-Agent": "Knoxx-Agent/1.0"})}),"Bluesky author feed").then((function (payload){
var results = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (entry){
var post = (entry["post"]);
var author = (post["author"]);
var record = (post["record"]);
var handle = (function (){var or__5142__auto__ = (author["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"handle","handle",1538948854),handle,new cljs.core.Keyword(null,"displayName","displayName",-809144601),(function (){var or__5142__auto__ = (author["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"text","text",-1790561697),(function (){var or__5142__auto__ = (record["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),(function (){var or__5142__auto__ = (record["createdAt"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"uri","uri",-774711847),(function (){var or__5142__auto__ = (post["uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"url","url",276297046),knoxx.backend.tools.bluesky.bluesky_post_url(handle,(post["uri"]))], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((payload["feed"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((payload["feed"])):cljs.core.PersistentVector.EMPTY));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"actor","actor",-1830560481),actor,new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
}));
});
knoxx.backend.tools.bluesky.bluesky_timeline_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_timeline_BANG_(runtime,limit,cursor){
return knoxx.backend.tools.bluesky.bluesky_create_session_BANG_(runtime).then((function (session){
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_(knoxx.backend.tools.bluesky.query_url((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_service_base_url())+"/xrpc/app.bsky.feed.getTimeline"),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"limit","limit",-1355822363),limit,new cljs.core.Keyword(null,"cursor","cursor",1011937484),cursor], null)),({"method": "GET", "headers": ({"Accept": "application/json", "User-Agent": "Knoxx-Agent/1.0", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((session["accessJwt"])))})}),"Bluesky timeline");
})).then((function (payload){
var results = cljs.core.mapv.cljs$core$IFn$_invoke$arity$2((function (entry){
var post = (entry["post"]);
var author = (post["author"]);
var record = (post["record"]);
var handle = (function (){var or__5142__auto__ = (author["handle"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"handle","handle",1538948854),handle,new cljs.core.Keyword(null,"displayName","displayName",-809144601),(function (){var or__5142__auto__ = (author["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"text","text",-1790561697),(function (){var or__5142__auto__ = (record["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),(function (){var or__5142__auto__ = (record["createdAt"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"uri","uri",-774711847),(function (){var or__5142__auto__ = (post["uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"url","url",276297046),knoxx.backend.tools.bluesky.bluesky_post_url(handle,(post["uri"]))], null);
}),(cljs.core.truth_(cljs.core.array_QMARK_((payload["feed"])))?cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1((payload["feed"])):cljs.core.PersistentVector.EMPTY));
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"cursor","cursor",1011937484),(payload["cursor"]),new cljs.core.Keyword(null,"results","results",-1134170113),results], null);
}));
});
knoxx.backend.tools.bluesky.bluesky_publish_BANG_ = (function knoxx$backend$tools$bluesky$bluesky_publish_BANG_(runtime,text){
return knoxx.backend.tools.bluesky.bluesky_create_session_BANG_(runtime).then((function (session){
return knoxx.backend.tools.bluesky.bluesky_json_fetch_BANG_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(knoxx.backend.tools.bluesky.bluesky_service_base_url())+"/xrpc/com.atproto.repo.createRecord"),({"method": "POST", "headers": ({"Content-Type": "application/json", "User-Agent": "Knoxx-Agent/1.0", "Authorization": (""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((session["accessJwt"])))}), "body": JSON.stringify(({"repo": (session["did"]), "collection": "app.bsky.feed.post", "record": ({"$type": "app.bsky.feed.post", "text": text, "createdAt": (new Date()).toISOString()})}))}),"Bluesky publish").then((function (result){
var uri = (function (){var or__5142__auto__ = (result["uri"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"uri","uri",-774711847),uri,new cljs.core.Keyword(null,"cid","cid",-1940591320),(function (){var or__5142__auto__ = (result["cid"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(),new cljs.core.Keyword(null,"url","url",276297046),(function (){var or__5142__auto__ = knoxx.backend.tools.bluesky.bluesky_post_url((session["handle"]),uri);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()], null);
}));
}));
});
knoxx.backend.tools.bluesky.publish_execute = (function knoxx$backend$tools$bluesky$publish_execute(runtime,_config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var text = (function (){var or__5142__auto__ = (params["text"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
if(clojure.string.blank_QMARK_(clojure.string.trim(text))){
throw (new Error("text is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Publishing to Bluesky\u2026");

return knoxx.backend.tools.bluesky.bluesky_publish_BANG_(runtime,text).then((function (result){
return knoxx.backend.text.tool_text_result((""+"Published Bluesky post\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"uri","uri",-774711847).cljs$core$IFn$_invoke$arity$1(result);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})())),result);
}));
});
knoxx.backend.tools.bluesky.profile_execute = (function knoxx$backend$tools$bluesky$profile_execute(runtime,_config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var actor = (function (){var or__5142__auto__ = (params["actor"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Reading Bluesky profile\u2026");

return knoxx.backend.tools.bluesky.bluesky_profile_BANG_(runtime,actor).then((function (profile){
return knoxx.backend.text.tool_text_result((""+"Bluesky profile: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = new cljs.core.Keyword(null,"displayName","displayName",-809144601).cljs$core$IFn$_invoke$arity$1(profile);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = new cljs.core.Keyword(null,"handle","handle",1538948854).cljs$core$IFn$_invoke$arity$1(profile);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "unknown";
}
}
})())+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"handle","handle",1538948854).cljs$core$IFn$_invoke$arity$1(profile)))))?null:(""+" (@"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"handle","handle",1538948854).cljs$core$IFn$_invoke$arity$1(profile))+")")))+"\nFollowers: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"followersCount","followersCount",635176802).cljs$core$IFn$_invoke$arity$1(profile))+" | Following: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"followsCount","followsCount",-1543777013).cljs$core$IFn$_invoke$arity$1(profile))+" | Posts: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"postsCount","postsCount",1128073488).cljs$core$IFn$_invoke$arity$1(profile))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(profile)))))?null:(""+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"description","description",-1428560544).cljs$core$IFn$_invoke$arity$1(profile)))))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(profile)))))?null:(""+"\n\n"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(profile)))))),profile);
}));
});
knoxx.backend.tools.bluesky.search_execute = (function knoxx$backend$tools$bluesky$search_execute(_runtime,_config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var query = (function (){var or__5142__auto__ = (params["query"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var kind = (function (){var or__5142__auto__ = (params["kind"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "posts";
}
})();
var limit = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((25),(function (){var or__5142__auto__ = (params["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (5);
}
})()));
if(clojure.string.blank_QMARK_(clojure.string.trim(query))){
throw (new Error("query is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Searching Bluesky\u2026");

return knoxx.backend.tools.bluesky.bluesky_search_BANG_(query,kind,limit).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.bluesky.format_posts((""+"Bluesky search ("+cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"kind","kind",-717265803).cljs$core$IFn$_invoke$arity$1(result))+")"),new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(result)),result);
}));
});
knoxx.backend.tools.bluesky.author_feed_execute = (function knoxx$backend$tools$bluesky$author_feed_execute(_runtime,_config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var actor = (function (){var or__5142__auto__ = (params["actor"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var limit = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((25),(function (){var or__5142__auto__ = (params["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8);
}
})()));
if(clojure.string.blank_QMARK_(clojure.string.trim(actor))){
throw (new Error("actor is required"));
} else {
}

knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,(""+"Reading Bluesky feed for "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor)+"\u2026"));

return knoxx.backend.tools.bluesky.bluesky_author_feed_BANG_(actor,limit).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.bluesky.format_posts((""+"Bluesky author feed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(actor)),new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(result)),result);
}));
});
knoxx.backend.tools.bluesky.timeline_execute = (function knoxx$backend$tools$bluesky$timeline_execute(runtime,_config,_tool_call_id,params,a,b,c){
var on_update = (function (){var or__5142__auto__ = ((cljs.core.fn_QMARK_(a))?a:null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = ((cljs.core.fn_QMARK_(b))?b:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
if(cljs.core.fn_QMARK_(c)){
return c;
} else {
return null;
}
}
}
})();
var limit = cljs.core.max.cljs$core$IFn$_invoke$arity$2((1),cljs.core.min.cljs$core$IFn$_invoke$arity$2((25),(function (){var or__5142__auto__ = (params["limit"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (8);
}
})()));
var cursor = (function (){var or__5142__auto__ = (params["cursor"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
knoxx.backend.tools.shared.maybe_tool_update_BANG_(on_update,"Reading authenticated Bluesky timeline\u2026");

return knoxx.backend.tools.bluesky.bluesky_timeline_BANG_(runtime,limit,cursor).then((function (result){
return knoxx.backend.text.tool_text_result(knoxx.backend.tools.bluesky.format_posts("Bluesky timeline",new cljs.core.Keyword(null,"results","results",-1134170113).cljs$core$IFn$_invoke$arity$1(result)),result);
}));
});
knoxx.backend.tools.bluesky.publish_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"bluesky.publish","Bluesky Publish","Publish a post to Bluesky using the configured account.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Post a concise update to Bluesky when public social publishing is useful.",cljs.core.PersistentVector.EMPTY,knoxx.backend.tools.bluesky.publish_params,knoxx.backend.tools.bluesky.publish_execute], 0));
knoxx.backend.tools.bluesky.profile_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"bluesky.profile","Bluesky Profile","Read a Bluesky profile by handle or DID, or default to the authenticated account.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read a Bluesky profile by handle or DID, or default to the authenticated account.",cljs.core.PersistentVector.EMPTY,knoxx.backend.tools.bluesky.profile_params,knoxx.backend.tools.bluesky.profile_execute], 0));
knoxx.backend.tools.bluesky.search_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"bluesky.search","Bluesky Search","Search public Bluesky posts or actors.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Search public Bluesky posts or actors.",cljs.core.PersistentVector.EMPTY,knoxx.backend.tools.bluesky.search_params,knoxx.backend.tools.bluesky.search_execute], 0));
knoxx.backend.tools.bluesky.author_feed_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"bluesky.author.feed","Bluesky Author Feed","Read recent posts from a specific Bluesky author.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read recent posts from a specific Bluesky author.",cljs.core.PersistentVector.EMPTY,knoxx.backend.tools.bluesky.feed_params,knoxx.backend.tools.bluesky.author_feed_execute], 0));
knoxx.backend.tools.bluesky.timeline_tool = cljs.core.partial.cljs$core$IFn$_invoke$arity$variadic(knoxx.backend.tools.shared.create_tool_obj,"bluesky.timeline","Bluesky Timeline","Read the authenticated account's Bluesky timeline.",cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Read the authenticated account's Bluesky timeline.",cljs.core.PersistentVector.EMPTY,knoxx.backend.tools.bluesky.timeline_params,knoxx.backend.tools.bluesky.timeline_execute], 0));
knoxx.backend.tools.bluesky.create_bluesky_custom_tools = (function knoxx$backend$tools$bluesky$create_bluesky_custom_tools(var_args){
var G__59134 = arguments.length;
switch (G__59134) {
case 2:
return knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error(["Invalid arity: ",arguments.length].join("")));

}
});

(knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$2 = (function (runtime,config){
return knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$3(runtime,config,null);
}));

(knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$core$IFn$_invoke$arity$3 = (function (runtime,config,auth_context){
var allowed_QMARK_ = (function (tool_id){
return (((auth_context == null)) || (knoxx.backend.authz.ctx_tool_allowed_QMARK_(auth_context,tool_id)));
});
return cljs.core.clj__GT_js(cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(cljs.core.nil_QMARK_,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [((allowed_QMARK_("bluesky.publish"))?knoxx.backend.tools.bluesky.publish_tool(runtime,config):null),((allowed_QMARK_("bluesky.profile"))?knoxx.backend.tools.bluesky.profile_tool(runtime,config):null),((allowed_QMARK_("bluesky.search"))?knoxx.backend.tools.bluesky.search_tool(runtime,config):null),((allowed_QMARK_("bluesky.author.feed"))?knoxx.backend.tools.bluesky.author_feed_tool(runtime,config):null),((allowed_QMARK_("bluesky.timeline"))?knoxx.backend.tools.bluesky.timeline_tool(runtime,config):null)], null))));
}));

(knoxx.backend.tools.bluesky.create_bluesky_custom_tools.cljs$lang$maxFixedArity = 3);


//# sourceMappingURL=knoxx.backend.tools.bluesky.js.map
