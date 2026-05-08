import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./shadow.esm.esm_import$node_crypto.js";
import "./shadow.esm.esm_import$nodemailer.js";
import "./shadow.esm.esm_import$redis.js";
goog.provide('knoxx.backend.auth.session');
knoxx.backend.auth.session.session_secret_mem = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
knoxx.backend.auth.session.session_secret = (function knoxx$backend$auth$session$session_secret(){
var or__5142__auto__ = cljs.core.deref(knoxx.backend.auth.session.session_secret_mem);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var env_secret = (process.env["KNOXX_SESSION_SECRET"]);
var secret = (function (){var or__5142__auto____$1 = (((!(clojure.string.blank_QMARK_(env_secret))))?env_secret:null);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return shadow.esm.esm_import$node_crypto.randomBytes((32)).toString("hex");
}
})();
cljs.core.reset_BANG_(knoxx.backend.auth.session.session_secret_mem,secret);

return secret;
}
});
knoxx.backend.auth.session.sign_token = (function knoxx$backend$auth$session$sign_token(payload){
var key = knoxx.backend.auth.session.session_secret();
var iv = shadow.esm.esm_import$node_crypto.randomBytes((12));
var data = JSON.stringify(cljs.core.clj__GT_js(payload));
var key_buf = Buffer.from(key,"hex").subarray((0),(32));
var cipher = shadow.esm.esm_import$node_crypto.createCipheriv("aes-256-gcm",key_buf,iv);
var encrypted = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cipher.update(data,"utf8","base64url"))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(cipher.final("base64url")));
var tag = cipher.getAuthTag();
return (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(iv.toString("base64url"))+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(encrypted)+":"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(tag.toString("base64url")));
});
knoxx.backend.auth.session.verify_token = (function knoxx$backend$auth$session$verify_token(token){
try{var key = knoxx.backend.auth.session.session_secret();
var parts = token.split(":");
if((parts.length >= (3))){
var iv_b64 = (parts[(0)]);
var encrypted = (parts[(1)]);
var tag_b64 = (parts[(2)]);
var iv = Buffer.from(iv_b64,"base64url");
var tag = Buffer.from(tag_b64,"base64url");
var key_buf = Buffer.from(key,"hex").subarray((0),(32));
var decipher = shadow.esm.esm_import$node_crypto.createDecipheriv("aes-256-gcm",key_buf,iv);
decipher.setAuthTag(tag);

var decrypted = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(decipher.update(encrypted,"base64url","utf8"))+cljs.core.str.cljs$core$IFn$_invoke$arity$1(decipher.final("utf8")));
return JSON.parse(decrypted);
} else {
return null;
}
}catch (e50669){var _ = e50669;
return null;
}});
knoxx.backend.auth.session.redis_client = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
knoxx.backend.auth.session.redis_connect_promise = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
knoxx.backend.auth.session.db_session_store = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(null);
knoxx.backend.auth.session.set_db_session_store_BANG_ = (function knoxx$backend$auth$session$set_db_session_store_BANG_(policyDb){
cljs.core.reset_BANG_(knoxx.backend.auth.session.db_session_store,policyDb);

return (knoxx.backend.auth.session.recover_or_persist_session_secret_BANG_.cljs$core$IFn$_invoke$arity$1 ? knoxx.backend.auth.session.recover_or_persist_session_secret_BANG_.cljs$core$IFn$_invoke$arity$1(policyDb) : knoxx.backend.auth.session.recover_or_persist_session_secret_BANG_.call(null,policyDb));
});
/**
 * If KNOXX_SESSION_SECRET env is set, use it. Otherwise, try to load from DB
 * (table: knoxx_config, key: session_secret). If none exists, generate, store, and use.
 */
knoxx.backend.auth.session.recover_or_persist_session_secret_BANG_ = (function knoxx$backend$auth$session$recover_or_persist_session_secret_BANG_(policyDb){
var env_secret = (process.env["KNOXX_SESSION_SECRET"]);
if((!(clojure.string.blank_QMARK_(env_secret)))){
cljs.core.reset_BANG_(knoxx.backend.auth.session.session_secret_mem,env_secret);

return console.log("[knoxx-session] Using session secret from KNOXX_SESSION_SECRET env");
} else {
return policyDb.query("SELECT value FROM knoxx_config WHERE key = 'session_secret'",cljs.core.PersistentVector.EMPTY).then((function (result){
var rows = (result["rows"]);
if((rows.length > (0))){
var stored = (rows[(0)]["value"]);
cljs.core.reset_BANG_(knoxx.backend.auth.session.session_secret_mem,stored);

return console.log("[knoxx-session] Recovered session secret from database");
} else {
var new_secret = shadow.esm.esm_import$node_crypto.randomBytes((32)).toString("hex");
cljs.core.reset_BANG_(knoxx.backend.auth.session.session_secret_mem,new_secret);

return policyDb.query("INSERT INTO knoxx_config (key, value) VALUES ('session_secret', $1)\n                                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new_secret], null)).then((function (_){
return console.log("[knoxx-session] Generated and persisted session secret to database");
})).catch((function (err2){
return console.log("[knoxx-session] ERROR persisting secret:",err2.message);
}));
}
})).catch((function (err){
return console.log("[knoxx-session] ERROR loading session secret from DB:",err.message);
}));
}
});
knoxx.backend.auth.session.db_store_session = (function knoxx$backend$auth$session$db_store_session(token,session_data){
if(cljs.core.not(cljs.core.deref(knoxx.backend.auth.session.db_session_store))){
return Promise.resolve(null);
} else {
var db = cljs.core.deref(knoxx.backend.auth.session.db_session_store);
var payload = ({"email": (function (){var or__5142__auto__ = (session_data["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "orgId": (function (){var or__5142__auto__ = (session_data["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "displayName": (function (){var or__5142__auto__ = (session_data["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "authProvider": (function (){var or__5142__auto__ = (session_data["authProvider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "github";
}
})(), "token": token, "userAgent": (function (){var or__5142__auto__ = (session_data["userAgent"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "membershipId": (function (){var or__5142__auto__ = (session_data["membershipId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "userId": (function (){var or__5142__auto__ = (session_data["userId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})(), "ipAddress": (function (){var or__5142__auto__ = (session_data["ipAddress"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})(), "externalSubject": (function (){var or__5142__auto__ = (session_data["externalId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return null;
}
})()});
return db.createSession(payload).catch((function (_){
return null;
}));
}
});
knoxx.backend.auth.session.db_load_session = (function knoxx$backend$auth$session$db_load_session(token){
if(cljs.core.not(cljs.core.deref(knoxx.backend.auth.session.db_session_store))){
return Promise.resolve(null);
} else {
return cljs.core.deref(knoxx.backend.auth.session.db_session_store).getSessionByToken(token).then((function (result){
if(cljs.core.truth_((function (){var and__5140__auto__ = result;
if(cljs.core.truth_(and__5140__auto__)){
return (result["session"]);
} else {
return and__5140__auto__;
}
})())){
var s = (result["session"]);
return ({"email": (s["email"]), "githubLogin": null, "orgId": (s["orgId"]), "displayName": (s["displayName"]), "orgSlug": null, "createdAt": (s["createdAt"]), "authProvider": (s["authProvider"]), "id": (s["id"]), "membershipId": (s["membershipId"]), "githubId": null, "userId": (s["userId"])});
} else {
return null;
}
})).catch((function (_){
return null;
}));
}
});
knoxx.backend.auth.session.get_redis = (function knoxx$backend$auth$session$get_redis(){
if(cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.deref(knoxx.backend.auth.session.redis_client);
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.deref(knoxx.backend.auth.session.redis_client).isOpen;
} else {
return and__5140__auto__;
}
})())){
return Promise.resolve(cljs.core.deref(knoxx.backend.auth.session.redis_client));
} else {
if(cljs.core.truth_(cljs.core.deref(knoxx.backend.auth.session.redis_connect_promise))){
return cljs.core.deref(knoxx.backend.auth.session.redis_connect_promise);
} else {
var promise = (function (){var url = (function (){var or__5142__auto__ = (process.env["REDIS_URL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "redis://127.0.0.1:6379";
}
})();
var client = shadow.esm.esm_import$redis.createClient(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"url","url",276297046),url], null)));
client.on("error",(function (err){
return console.error("[knoxx-session] Redis error:",err.message);
}));

return client.connect().then((function (_){
console.log("[knoxx-session] Redis connected for session store");

cljs.core.reset_BANG_(knoxx.backend.auth.session.redis_client,client);

cljs.core.reset_BANG_(knoxx.backend.auth.session.redis_connect_promise,null);

return client;
}));
})().catch((function (err){
cljs.core.reset_BANG_(knoxx.backend.auth.session.redis_connect_promise,null);

return Promise.reject(err);
}));
cljs.core.reset_BANG_(knoxx.backend.auth.session.redis_connect_promise,promise);

return promise;
}
}
});
knoxx.backend.auth.session.store_session = (function knoxx$backend$auth$session$store_session(session_id,data){
var token = (function (){var or__5142__auto__ = (data["_rawToken"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
return knoxx.backend.auth.session.db_store_session(token,data).catch((function (err){
return console.log("[knoxx-session] WARN: DB store failed:",err.message);
})).then((function (_){
return knoxx.backend.auth.session.get_redis().then((function (redis){
var ttl = parseInt((function (){var or__5142__auto__ = (process.env["KNOXX_SESSION_TTL_SECONDS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "86400";
}
})(),(10));
return redis.set((""+"knoxx:session:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)),JSON.stringify(cljs.core.clj__GT_js(data)),cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"EX","EX",1763139679),ttl], null)));
}));
}));
});
knoxx.backend.auth.session.load_session = (function knoxx$backend$auth$session$load_session(session_id,token){
return knoxx.backend.auth.session.get_redis().then((function (redis){
return redis.get((""+"knoxx:session:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)));
})).then((function (raw){
if(cljs.core.truth_(raw)){
try{return JSON.parse(raw);
}catch (e50684){var _err1 = e50684;
return null;
}} else {
return knoxx.backend.auth.session.db_load_session((function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
}
})).catch((function (_err2){
return knoxx.backend.auth.session.db_load_session((function (){var or__5142__auto__ = token;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
}));
});
knoxx.backend.auth.session.delete_session = (function knoxx$backend$auth$session$delete_session(session_id,token){
return (cljs.core.truth_((function (){var and__5140__auto__ = cljs.core.deref(knoxx.backend.auth.session.db_session_store);
if(cljs.core.truth_(and__5140__auto__)){
return (!(clojure.string.blank_QMARK_(token)));
} else {
return and__5140__auto__;
}
})())?cljs.core.deref(knoxx.backend.auth.session.db_session_store).deleteSessionByToken(token).catch((function (_){
return null;
})):Promise.resolve(null)).then((function (_){
return knoxx.backend.auth.session.get_redis().then((function (redis){
return redis.del((""+"knoxx:session:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(session_id)));
})).catch((function (___$1){
return null;
}));
})).then((function (_){
return null;
}));
});
knoxx.backend.auth.session.exchange_github_code = (function knoxx$backend$auth$session$exchange_github_code(client_id,client_secret,code){
return fetch("https://github.com/login/oauth/access_token",cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"Content-Type","Content-Type",-692731875),"application/json",new cljs.core.Keyword(null,"Accept","Accept",791341676),"application/json"], null),new cljs.core.Keyword(null,"body","body",-2049205669),JSON.stringify(({"client_id": client_id, "client_secret": client_secret, "code": code}))], null))).then((function (resp){
if(cljs.core.not(resp.ok)){
throw (new Error((""+"GitHub token exchange failed: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status))));
} else {
return resp.json();
}
})).then((function (data){
if(cljs.core.truth_((data["error"]))){
throw (new Error((""+"GitHub OAuth error: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (data["error_description"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (data["error"]);
}
})()))));
} else {
return (data["access_token"]);
}
}));
});
knoxx.backend.auth.session.gh_json = (function knoxx$backend$auth$session$gh_json(url,access_token){
return fetch(url,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"Authorization","Authorization",-1017527462),(""+"Bearer "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(access_token)),new cljs.core.Keyword(null,"Accept","Accept",791341676),"application/json"], null)], null))).then((function (resp){
if(cljs.core.not(resp.ok)){
throw (new Error((""+"GitHub API "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(url)+" returned "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(resp.status))));
} else {
return resp.json();
}
}));
});
knoxx.backend.auth.session.get_github_user_emails = (function knoxx$backend$auth$session$get_github_user_emails(access_token){
return knoxx.backend.auth.session.gh_json("https://api.github.com/user/emails",access_token).then((function (emails){
var primary = cljs.core.some((function (e){
if(cljs.core.truth_((e["primary"]))){
return e;
} else {
return null;
}
}),emails);
var or__5142__auto__ = (function (){var G__50701 = primary;
if((G__50701 == null)){
return null;
} else {
return (G__50701["email"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__50702 = cljs.core.first(emails);
if((G__50702 == null)){
return null;
} else {
return (G__50702["email"]);
}
}
})).catch((function (_){
return null;
}));
});
knoxx.backend.auth.session.COOKIE_NAME = "knoxx_session";
knoxx.backend.auth.session.secure_origin_QMARK_ = (function knoxx$backend$auth$session$secure_origin_QMARK_(base_url){
try{return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((new URL(base_url)).protocol,"https:");
}catch (e50703){var _ = e50703;
return false;
}});
knoxx.backend.auth.session.set_session_cookie = (function knoxx$backend$auth$session$set_session_cookie(reply,token,base_url){
var secure = knoxx.backend.auth.session.secure_origin_QMARK_(base_url);
var ttl = parseInt((function (){var or__5142__auto__ = (process.env["KNOXX_SESSION_TTL_SECONDS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "86400";
}
})(),(10));
return reply.setCookie(knoxx.backend.auth.session.COOKIE_NAME,token,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"path","path",-188191168),"/",new cljs.core.Keyword(null,"httpOnly","httpOnly",-1786097473),true,new cljs.core.Keyword(null,"secure","secure",176883900),secure,new cljs.core.Keyword(null,"sameSite","sameSite",2079352839),"Lax",new cljs.core.Keyword(null,"maxAge","maxAge",868089807),ttl], null)));
});
knoxx.backend.auth.session.clear_session_cookie = (function knoxx$backend$auth$session$clear_session_cookie(reply,base_url){
var secure = knoxx.backend.auth.session.secure_origin_QMARK_(base_url);
return reply.clearCookie(knoxx.backend.auth.session.COOKIE_NAME,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"path","path",-188191168),"/",new cljs.core.Keyword(null,"httpOnly","httpOnly",-1786097473),true,new cljs.core.Keyword(null,"secure","secure",176883900),secure,new cljs.core.Keyword(null,"sameSite","sameSite",2079352839),"Lax"], null)));
});
knoxx.backend.auth.session.STATE_TTL = (600);
knoxx.backend.auth.session.pending_states = cljs.core.atom.cljs$core$IFn$_invoke$arity$1(cljs.core.PersistentArrayMap.EMPTY);
knoxx.backend.auth.session.create_state = (function knoxx$backend$auth$session$create_state(redirect){
var state = shadow.esm.esm_import$node_crypto.randomBytes((16)).toString("hex");
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$4(knoxx.backend.auth.session.pending_states,cljs.core.assoc,state,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"redirect","redirect",-1975673286),(function (){var or__5142__auto__ = redirect;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/";
}
})(),new cljs.core.Keyword(null,"createdAt","createdAt",-936788),Date.now()], null));

cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$2(knoxx.backend.auth.session.pending_states,(function (states){
return cljs.core.into.cljs$core$IFn$_invoke$arity$3(cljs.core.PersistentArrayMap.EMPTY,cljs.core.filter.cljs$core$IFn$_invoke$arity$1((function (p__50715){
var vec__50716 = p__50715;
var _ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50716,(0),null);
var v = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__50716,(1),null);
return ((Date.now() - new cljs.core.Keyword(null,"createdAt","createdAt",-936788).cljs$core$IFn$_invoke$arity$1(v)) < (knoxx.backend.auth.session.STATE_TTL * (1000)));
})),states);
}));

return state;
});
knoxx.backend.auth.session.consume_state = (function knoxx$backend$auth$session$consume_state(state){
var temp__5825__auto__ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(cljs.core.deref(knoxx.backend.auth.session.pending_states),state);
if(cljs.core.truth_(temp__5825__auto__)){
var entry = temp__5825__auto__;
cljs.core.swap_BANG_.cljs$core$IFn$_invoke$arity$3(knoxx.backend.auth.session.pending_states,cljs.core.dissoc,state);

if(((Date.now() - new cljs.core.Keyword(null,"createdAt","createdAt",-936788).cljs$core$IFn$_invoke$arity$1(entry)) < (knoxx.backend.auth.session.STATE_TTL * (1000)))){
return entry;
} else {
return null;
}
} else {
return null;
}
});
knoxx.backend.auth.session.http_error = (function knoxx$backend$auth$session$http_error(status,message,code){
var err = (new Error(message));
(err.status = status);

(err.code = code);

return err;
});
knoxx.backend.auth.session.bootstrap_role_slugs_for_email = (function knoxx$backend$auth$session$bootstrap_role_slugs_for_email(email){
var normalized_email = clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = email;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
var bootstrap_admin_email = (function (){var G__50735 = (function (){var or__5142__auto__ = (process.env["KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "system-admin@open-hax.local";
}
})();
var G__50735__$1 = (((G__50735 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50735)));
var G__50735__$2 = (((G__50735__$1 == null))?null:clojure.string.trim(G__50735__$1));
if((G__50735__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__50735__$2);
}
})();
var allowlisted_emails = cljs.core.set(cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.lower_case,cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/[\s,]+/)))));
var allowlist_role_slugs = cljs.core.vec(cljs.core.remove.cljs$core$IFn$_invoke$arity$2(clojure.string.blank_QMARK_,cljs.core.map.cljs$core$IFn$_invoke$arity$2(clojure.string.trim,clojure.string.split.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())),/[\s,]+/))));
if(cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized_email,bootstrap_admin_email)){
return ["system_admin"];
} else {
if(cljs.core.contains_QMARK_(allowlisted_emails,normalized_email)){
return cljs.core.clj__GT_js(((cljs.core.seq(allowlist_role_slugs))?allowlist_role_slugs:new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["knowledge_worker"], null)));
} else {
return ["knowledge_worker"];

}
}
});
knoxx.backend.auth.session.bootstrap_admin_email_QMARK_ = (function knoxx$backend$auth$session$bootstrap_admin_email_QMARK_(email){
var normalized_email = clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = email;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
var bootstrap_admin_email = (function (){var G__50742 = (function (){var or__5142__auto__ = (process.env["KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "system-admin@open-hax.local";
}
})();
var G__50742__$1 = (((G__50742 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50742)));
var G__50742__$2 = (((G__50742__$1 == null))?null:clojure.string.trim(G__50742__$1));
if((G__50742__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__50742__$2);
}
})();
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(normalized_email,bootstrap_admin_email);
});
knoxx.backend.auth.session.has_system_admin_role_QMARK_ = (function knoxx$backend$auth$session$has_system_admin_role_QMARK_(ctx){
return cljs.core.boolean$(cljs.core.some((function (p1__50743_SHARP_){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(p1__50743_SHARP_)),"system_admin");
}),(function (){var or__5142__auto__ = (function (){var G__50745 = ctx;
var G__50745__$1 = (((G__50745 == null))?null:(G__50745["roleSlugs"]));
if((G__50745__$1 == null)){
return null;
} else {
return cljs.core.array_seq.cljs$core$IFn$_invoke$arity$1(G__50745__$1);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return cljs.core.PersistentVector.EMPTY;
}
})()));
});
/**
 * Repair an existing bootstrap admin account that was created before the
 * bootstrap email config was set correctly. If the authenticating GitHub email
 * now matches the configured bootstrap admin email, guarantee the membership
 * carries the global system_admin role before building the session context.
 */
knoxx.backend.auth.session.ensure_bootstrap_admin_role_BANG_ = (function knoxx$backend$auth$session$ensure_bootstrap_admin_role_BANG_(policyDb,ctx,email){
var membership_id = (function (){var G__50754 = ctx;
var G__50754__$1 = (((G__50754 == null))?null:(G__50754["membership"]));
if((G__50754__$1 == null)){
return null;
} else {
return (G__50754__$1["id"]);
}
})();
var org_id = (function (){var G__50758 = ctx;
var G__50758__$1 = (((G__50758 == null))?null:(G__50758["org"]));
if((G__50758__$1 == null)){
return null;
} else {
return (G__50758__$1["id"]);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = membership_id;
if(cljs.core.truth_(and__5140__auto__)){
return ((knoxx.backend.auth.session.bootstrap_admin_email_QMARK_(email)) && ((!(knoxx.backend.auth.session.has_system_admin_role_QMARK_(ctx)))));
} else {
return and__5140__auto__;
}
})())){
return policyDb.setMembershipRoles(membership_id,({"orgId": org_id, "roleSlugs": ["system_admin"]})).then((function (_){
return policyDb.resolveRequestContext(({"x-knoxx-membership-id": membership_id}));
}));
} else {
return Promise.resolve(ctx);
}
});
knoxx.backend.auth.session.ensure_email_membership_BANG_ = (function knoxx$backend$auth$session$ensure_email_membership_BANG_(policyDb,p__50769){
var map__50770 = p__50769;
var map__50770__$1 = cljs.core.__destructure_map(map__50770);
var email = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50770__$1,new cljs.core.Keyword(null,"email","email",1415816706));
var display_name = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50770__$1,new cljs.core.Keyword(null,"display-name","display-name",694513143));
var auth_provider = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50770__$1,new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231));
var external_subject = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__50770__$1,new cljs.core.Keyword(null,"external-subject","external-subject",-265707402));
var normalized_email = (function (){var G__50771 = email;
var G__50771__$1 = (((G__50771 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50771)));
var G__50771__$2 = (((G__50771__$1 == null))?null:clojure.string.trim(G__50771__$1));
if((G__50771__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50771__$2);
}
})();
var headers_like = (cljs.core.truth_(normalized_email)?({"x-knoxx-user-email": normalized_email}):null);
var sync_user_from_actor_contract = (policyDb["syncUserFromActorContract"]);
if(cljs.core.not(normalized_email)){
return Promise.reject(knoxx.backend.auth.session.http_error((401),"Not authenticated","no_email"));
} else {
return (cljs.core.truth_(sync_user_from_actor_contract)?(function (){var G__50772 = ({"email": normalized_email, "displayName": (function (){var or__5142__auto__ = display_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return normalized_email;
}
})(), "authProvider": (function (){var or__5142__auto__ = auth_provider;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "github";
}
})(), "externalSubject": external_subject});
return (sync_user_from_actor_contract.cljs$core$IFn$_invoke$arity$1 ? sync_user_from_actor_contract.cljs$core$IFn$_invoke$arity$1(G__50772) : sync_user_from_actor_contract.call(null,G__50772));
})():Promise.resolve(null)).then((function (_){
return policyDb.resolveRequestContext(headers_like);
}));
}
});
/**
 * Resolve the canonical Knoxx user context by GitHub email.
 * 
 * Email is the canonical username. Actor and role assignment now come from the
 * persisted Knoxx user/membership records rather than being inferred from the
 * OAuth callback environment.
 */
knoxx.backend.auth.session.ensure_user_membership_BANG_ = (function knoxx$backend$auth$session$ensure_user_membership_BANG_(policyDb,gh_user,email){
return knoxx.backend.auth.session.ensure_email_membership_BANG_(policyDb,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"display-name","display-name",694513143),(function (){var or__5142__auto__ = (gh_user["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (gh_user["login"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return email;
}
}
})(),new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231),"github",new cljs.core.Keyword(null,"external-subject","external-subject",-265707402),(cljs.core.truth_((gh_user["id"]))?(""+"github:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1((gh_user["id"]))):null)], null));
});
knoxx.backend.auth.session.configured_api_key = (function knoxx$backend$auth$session$configured_api_key(){
var G__50782 = (process.env["KNOXX_API_KEY"]);
var G__50782__$1 = (((G__50782 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50782)));
var G__50782__$2 = (((G__50782__$1 == null))?null:clojure.string.trim(G__50782__$1));
if((G__50782__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50782__$2);
}
});
knoxx.backend.auth.session.request_api_key = (function knoxx$backend$auth$session$request_api_key(req){
var G__50784 = (function (){var or__5142__auto__ = (req.headers["x-api-key"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (req.headers["X-API-Key"]);
}
})();
var G__50784__$1 = (((G__50784 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50784)));
var G__50784__$2 = (((G__50784__$1 == null))?null:clojure.string.trim(G__50784__$1));
if((G__50784__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50784__$2);
}
});
knoxx.backend.auth.session.api_key_auth_email = (function knoxx$backend$auth$session$api_key_auth_email(){
var node_env = (function (){var G__50786 = (process.env["NODE_ENV"]);
var G__50786__$1 = (((G__50786 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50786)));
var G__50786__$2 = (((G__50786__$1 == null))?null:clojure.string.trim(G__50786__$1));
if((G__50786__$2 == null)){
return null;
} else {
return clojure.string.lower_case(G__50786__$2);
}
})();
var G__50787 = (function (){var or__5142__auto__ = (process.env["KNOXX_API_KEY_USER_EMAIL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2(node_env,"production")){
return "pi@open-hax.local";
} else {
return null;
}
}
})();
var G__50787__$1 = (((G__50787 == null))?null:(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(G__50787)));
var G__50787__$2 = (((G__50787__$1 == null))?null:clojure.string.trim(G__50787__$1));
if((G__50787__$2 == null)){
return null;
} else {
return cljs.core.not_empty(G__50787__$2);
}
});
knoxx.backend.auth.session.valid_api_key_request_QMARK_ = (function knoxx$backend$auth$session$valid_api_key_request_QMARK_(req){
var expected = knoxx.backend.auth.session.configured_api_key();
var provided = knoxx.backend.auth.session.request_api_key(req);
var and__5140__auto__ = expected;
if(cljs.core.truth_(and__5140__auto__)){
var and__5140__auto____$1 = provided;
if(cljs.core.truth_(and__5140__auto____$1)){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(expected,provided);
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
});
knoxx.backend.auth.session.ensure_api_key_membership_BANG_ = (function knoxx$backend$auth$session$ensure_api_key_membership_BANG_(policyDb){
var temp__5823__auto__ = knoxx.backend.auth.session.api_key_auth_email();
if(cljs.core.truth_(temp__5823__auto__)){
var email = temp__5823__auto__;
return knoxx.backend.auth.session.ensure_email_membership_BANG_(policyDb,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"display-name","display-name",694513143),"Pi",new cljs.core.Keyword(null,"auth-provider","auth-provider",4882231),"api-key",new cljs.core.Keyword(null,"external-subject","external-subject",-265707402),(""+"api-key:"+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email))], null));
} else {
return Promise.reject(knoxx.backend.auth.session.http_error((401),"Knoxx API key user email is not configured","api_key_identity_missing"));
}
});
knoxx.backend.auth.session.resolve_cookie_auth_context = (function knoxx$backend$auth$session$resolve_cookie_auth_context(req,policyDb){
var cookie_token = (function (){var G__50796 = req;
var G__50796__$1 = (((G__50796 == null))?null:(G__50796["cookies"]));
if((G__50796__$1 == null)){
return null;
} else {
return (G__50796__$1[knoxx.backend.auth.session.COOKIE_NAME]);
}
})();
if(cljs.core.not(cookie_token)){
return Promise.reject(knoxx.backend.auth.session.http_error((401),"Not authenticated","no_session"));
} else {
var payload = knoxx.backend.auth.session.verify_token(cookie_token);
if(cljs.core.not((function (){var and__5140__auto__ = payload;
if(cljs.core.truth_(and__5140__auto__)){
return (payload["sid"]);
} else {
return and__5140__auto__;
}
})())){
return Promise.reject(knoxx.backend.auth.session.http_error((401),"Invalid session token","invalid_token"));
} else {
return knoxx.backend.auth.session.load_session((payload["sid"]),cookie_token).then((function (session_data){
if(cljs.core.not(session_data)){
return Promise.reject(knoxx.backend.auth.session.http_error((401),"Session expired","session_expired"));
} else {
var headers = ({"x-knoxx-user-email": (session_data["email"]), "x-knoxx-org-slug": (session_data["orgSlug"])});
if(cljs.core.truth_((session_data["membershipId"]))){
(headers["x-knoxx-membership-id"] = (session_data["membershipId"]));
} else {
}

return policyDb.resolveRequestContext(headers);
}
}));
}
}
});
/**
 * Create session from resolved context, set cookie, and redirect.
 */
knoxx.backend.auth.session.create_session_and_redirect_BANG_ = (function knoxx$backend$auth$session$create_session_and_redirect_BANG_(policyDb,reply,gh_user,email,state_entry,public_base_url){
return knoxx.backend.auth.session.ensure_user_membership_BANG_(policyDb,gh_user,email).then((function (fresh_ctx){
var session_id = shadow.esm.esm_import$node_crypto.randomUUID();
var raw_token = knoxx.backend.auth.session.sign_token(({"sid": session_id}));
var session_data = ({"email": email, "actorId": (function (){var or__5142__auto__ = (function (){var G__50818 = fresh_ctx;
var G__50818__$1 = (((G__50818 == null))?null:(G__50818["membership"]));
if((G__50818__$1 == null)){
return null;
} else {
return (G__50818__$1["actorId"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__50831 = fresh_ctx;
var G__50831__$1 = (((G__50831 == null))?null:(G__50831["actor"]));
if((G__50831__$1 == null)){
return null;
} else {
return (G__50831__$1["id"]);
}
}
})(), "githubLogin": (gh_user["login"]), "orgId": (function (){var G__50843 = fresh_ctx;
var G__50843__$1 = (((G__50843 == null))?null:(G__50843["org"]));
if((G__50843__$1 == null)){
return null;
} else {
return (G__50843__$1["id"]);
}
})(), "_rawToken": raw_token, "displayName": (function (){var or__5142__auto__ = (gh_user["name"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (gh_user["login"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return email;
}
}
})(), "orgSlug": (function (){var G__50860 = fresh_ctx;
var G__50860__$1 = (((G__50860 == null))?null:(G__50860["org"]));
if((G__50860__$1 == null)){
return null;
} else {
return (G__50860__$1["slug"]);
}
})(), "createdAt": (new Date()).toISOString(), "authProvider": "github", "membershipId": (function (){var G__50867 = fresh_ctx;
var G__50867__$1 = (((G__50867 == null))?null:(G__50867["membership"]));
if((G__50867__$1 == null)){
return null;
} else {
return (G__50867__$1["id"]);
}
})(), "githubId": (gh_user["id"]), "userId": (function (){var G__50879 = fresh_ctx;
var G__50879__$1 = (((G__50879 == null))?null:(G__50879["user"]));
if((G__50879__$1 == null)){
return null;
} else {
return (G__50879__$1["id"]);
}
})()});
return knoxx.backend.auth.session.store_session(session_id,session_data).then((function (_){
return raw_token;
})).then((function (token){
knoxx.backend.auth.session.set_session_cookie(reply,token,public_base_url);

console.log((""+"[knoxx-session] GitHub login: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email)));

return reply.redirect((new URL(new cljs.core.Keyword(null,"redirect","redirect",-1975673286).cljs$core$IFn$_invoke$arity$1(state_entry),public_base_url)).toString());
}));
}));
});
knoxx.backend.auth.session.create_session_from_context_BANG_ = (function knoxx$backend$auth$session$create_session_from_context_BANG_(reply,public_base_url,ctx,session_options){
var session_id = shadow.esm.esm_import$node_crypto.randomUUID();
var raw_token = knoxx.backend.auth.session.sign_token(({"sid": session_id}));
var session_data = ({"email": (function (){var or__5142__auto__ = (session_options["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__50907 = ctx;
var G__50907__$1 = (((G__50907 == null))?null:(G__50907["user"]));
if((G__50907__$1 == null)){
return null;
} else {
return (G__50907__$1["email"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})(), "actorId": (function (){var or__5142__auto__ = (function (){var G__50920 = ctx;
var G__50920__$1 = (((G__50920 == null))?null:(G__50920["membership"]));
if((G__50920__$1 == null)){
return null;
} else {
return (G__50920__$1["actorId"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__50922 = ctx;
var G__50922__$1 = (((G__50922 == null))?null:(G__50922["actor"]));
if((G__50922__$1 == null)){
return null;
} else {
return (G__50922__$1["id"]);
}
}
})(), "orgId": (function (){var G__50928 = ctx;
var G__50928__$1 = (((G__50928 == null))?null:(G__50928["org"]));
if((G__50928__$1 == null)){
return null;
} else {
return (G__50928__$1["id"]);
}
})(), "_rawToken": raw_token, "displayName": (function (){var or__5142__auto__ = (session_options["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (function (){var G__50938 = ctx;
var G__50938__$1 = (((G__50938 == null))?null:(G__50938["user"]));
if((G__50938__$1 == null)){
return null;
} else {
return (G__50938__$1["displayName"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
var or__5142__auto____$2 = (function (){var G__50943 = ctx;
var G__50943__$1 = (((G__50943 == null))?null:(G__50943["user"]));
if((G__50943__$1 == null)){
return null;
} else {
return (G__50943__$1["email"]);
}
})();
if(cljs.core.truth_(or__5142__auto____$2)){
return or__5142__auto____$2;
} else {
return "";
}
}
}
})(), "orgSlug": (function (){var G__50944 = ctx;
var G__50944__$1 = (((G__50944 == null))?null:(G__50944["org"]));
if((G__50944__$1 == null)){
return null;
} else {
return (G__50944__$1["slug"]);
}
})(), "createdAt": (new Date()).toISOString(), "authProvider": (function (){var or__5142__auto__ = (session_options["authProvider"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "local";
}
})(), "membershipId": (function (){var G__50945 = ctx;
var G__50945__$1 = (((G__50945 == null))?null:(G__50945["membership"]));
if((G__50945__$1 == null)){
return null;
} else {
return (G__50945__$1["id"]);
}
})(), "userId": (function (){var G__50946 = ctx;
var G__50946__$1 = (((G__50946 == null))?null:(G__50946["user"]));
if((G__50946__$1 == null)){
return null;
} else {
return (G__50946__$1["id"]);
}
})()});
return knoxx.backend.auth.session.store_session(session_id,session_data).then((function (_){
knoxx.backend.auth.session.set_session_cookie(reply,raw_token,public_base_url);

return ({"ok": true, "sessionId": session_id, "user": (ctx["user"]), "actor": (ctx["actor"]), "org": (ctx["org"]), "membership": (ctx["membership"])});
}));
});
/**
 * Check if email is whitelisted; if so, create session and redirect, otherwise redirect to invite page.
 */
knoxx.backend.auth.session.check_whitelist_and_session_BANG_ = (function knoxx$backend$auth$session$check_whitelist_and_session_BANG_(policyDb,reply,gh_user,email,state_entry,public_base_url){
return knoxx.backend.auth.session.ensure_user_membership_BANG_(policyDb,gh_user,email).then((function (_){
return true;
})).catch((function (_){
return false;
})).then((function (whitelisted){
if(cljs.core.not(whitelisted)){
var invite_url = (new URL("/login",public_base_url));
invite_url.searchParams.set("error","not_whitelisted");

invite_url.searchParams.set("email",email);

invite_url.searchParams.set("github_login",(function (){var or__5142__auto__ = (gh_user["login"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());

return reply.redirect(invite_url.toString());
} else {
return knoxx.backend.auth.session.create_session_and_redirect_BANG_(policyDb,reply,gh_user,email,state_entry,public_base_url);
}
}));
});
knoxx.backend.auth.session.handle_github_callback = (function knoxx$backend$auth$session$handle_github_callback(policyDb,reply,client_id,client_secret,state_entry,code,public_base_url){
return knoxx.backend.auth.session.exchange_github_code(client_id,client_secret,code).then((function (access_token){
return knoxx.backend.auth.session.gh_json("https://api.github.com/user",access_token).then((function (gh_user){
if(cljs.core.not((gh_user["id"]))){
throw (new Error("GitHub user lookup failed"));
} else {
return knoxx.backend.auth.session.get_github_user_emails(access_token).then((function (email){
if(cljs.core.not(email)){
throw (new Error("Could not retrieve GitHub email"));
} else {
return knoxx.backend.auth.session.check_whitelist_and_session_BANG_(policyDb,reply,gh_user,email,state_entry,public_base_url);
}
}));
}
}));
})).catch((function (err){
console.error("[knoxx-session] GitHub OAuth callback error:",err.message);

var error_url = (new URL("/login",public_base_url));
error_url.searchParams.set("error","oauth_failed");

error_url.searchParams.set("message",err.message);

return reply.redirect(error_url.toString());
}));
});
/**
 * Best-effort invite email sender.
 * 
 * IMPORTANT: This function MUST always return a Promise, so callers can safely
 * attach .catch even when email sending is disabled/unconfigured.
 */
knoxx.backend.auth.session.send_invite_email = (function knoxx$backend$auth$session$send_invite_email(_runtime,invite,email,public_base_url){
try{var smtp_host = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_SMTP_HOST"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var smtp_port = parseInt((function (){var or__5142__auto__ = (process.env["KNOXX_SMTP_PORT"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "587";
}
})(),(10));
var smtp_user = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_SMTP_USER"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var smtp_pass = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_SMTP_PASS"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var from = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (process.env["KNOXX_EMAIL_FROM"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = smtp_user;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})()));
var invite_code = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (invite["code"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var invite_url = (function (){try{var u = (new URL("/login",public_base_url));
u.searchParams.set("invite",invite_code);

u.searchParams.set("email",(""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email)));

return u.toString();
}catch (e50963){var _ = e50963;
return "";
}})();
if(((clojure.string.blank_QMARK_(smtp_host)) || (((clojure.string.blank_QMARK_(from)) || (((clojure.string.blank_QMARK_(smtp_user)) || (((clojure.string.blank_QMARK_(smtp_pass)) || (((clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email)))) || (((clojure.string.blank_QMARK_(invite_code)) || (clojure.string.blank_QMARK_(invite_url)))))))))))))){
return Promise.resolve(null);
} else {
var transporter = shadow.esm.esm_import$nodemailer.default.createTransport(({"host": smtp_host, "port": smtp_port, "secure": false, "auth": ({"user": smtp_user, "pass": smtp_pass})}));
var subject = "Knoxx invite";
var text = (""+"You have been invited to Knoxx.\n\n"+"Invite link: "+cljs.core.str.cljs$core$IFn$_invoke$arity$1(invite_url)+"\n");
return transporter.sendMail(({"from": from, "to": (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(email)), "subject": subject, "text": text}));
}
}catch (e50954){var err = e50954;
console.warn("[knoxx-session] send-invite-email error:",err.message);

return Promise.resolve(null);
}});
knoxx.backend.auth.session.create_session_hook = (function knoxx$backend$auth$session$create_session_hook(_policyDb){
return (function knoxx$backend$auth$session$create_session_hook_$_session_hook(req,reply){
if(cljs.core.not((function (){var and__5140__auto__ = req.url.startsWith("/api/auth/");
if(cljs.core.truth_(and__5140__auto__)){
return cljs.core.not(req.url.startsWith("/api/auth/context"));
} else {
return and__5140__auto__;
}
})())){
var headers = req.headers;
var header_email = clojure.string.trim((function (){var or__5142__auto__ = (headers["x-knoxx-user-email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var header_mid = clojure.string.trim((function (){var or__5142__auto__ = (headers["x-knoxx-membership-id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var cookie_token = (function (){var G__50979 = req;
var G__50979__$1 = (((G__50979 == null))?null:(G__50979["cookies"]));
if((G__50979__$1 == null)){
return null;
} else {
return (G__50979__$1[knoxx.backend.auth.session.COOKIE_NAME]);
}
})();
if(cljs.core.truth_((function (){var and__5140__auto__ = clojure.string.blank_QMARK_(header_email);
if(and__5140__auto__){
var and__5140__auto____$1 = clojure.string.blank_QMARK_(header_mid);
if(and__5140__auto____$1){
return cookie_token;
} else {
return and__5140__auto____$1;
}
} else {
return and__5140__auto__;
}
})())){
var payload = knoxx.backend.auth.session.verify_token(cookie_token);
var session_id = (function (){var G__50983 = payload;
if((G__50983 == null)){
return null;
} else {
return (G__50983["sid"]);
}
})();
if(cljs.core.truth_(session_id)){
return knoxx.backend.auth.session.load_session(session_id,cookie_token).then((function (session_data){
if(cljs.core.not(session_data)){
return knoxx.backend.auth.session.clear_session_cookie(reply,(function (){var or__5142__auto__ = (process.env["KNOXX_PUBLIC_BASE_URL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost";
}
})());
} else {
(headers["x-knoxx-user-email"] = (session_data["email"]));

if(cljs.core.truth_((session_data["orgSlug"]))){
(headers["x-knoxx-org-slug"] = (session_data["orgSlug"]));
} else {
}

if(cljs.core.truth_((session_data["membershipId"]))){
return (headers["x-knoxx-membership-id"] = (session_data["membershipId"]));
} else {
return null;
}
}
}),(function (_){
return null;
}).catch());
} else {
return null;
}
} else {
return null;
}
} else {
return null;
}
});
});
knoxx.backend.auth.session.resolve_auth_context = (function knoxx$backend$auth$session$resolve_auth_context(req,policyDb){
var header_email = clojure.string.trim((function (){var or__5142__auto__ = (req.headers["x-knoxx-user-email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
var header_mid = clojure.string.trim((function (){var or__5142__auto__ = (req.headers["x-knoxx-membership-id"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})());
if((((!(clojure.string.blank_QMARK_(header_email)))) || ((!(clojure.string.blank_QMARK_(header_mid)))))){
return policyDb.resolveRequestContext(req.headers);
} else {
if(cljs.core.truth_(knoxx.backend.auth.session.valid_api_key_request_QMARK_(req))){
return knoxx.backend.auth.session.ensure_api_key_membership_BANG_(policyDb);
} else {
return knoxx.backend.auth.session.resolve_cookie_auth_context(req,policyDb);

}
}
});

//# sourceMappingURL=knoxx.backend.auth.session.js.map
