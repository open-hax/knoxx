import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
import "./knoxx.backend.auth.session.js";
goog.provide('knoxx.backend.routes.auth');
knoxx.backend.routes.auth.register_auth_routes = (function knoxx$backend$routes$auth$register_auth_routes(app,opts){
var public_base_url = (function (){var or__5142__auto__ = (process.env["KNOXX_PUBLIC_BASE_URL"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "http://localhost";
}
})();
var policyDb = (function (){var or__5142__auto__ = ((cljs.core.map_QMARK_(opts))?new cljs.core.Keyword(null,"policyDb","policyDb",1076383858).cljs$core$IFn$_invoke$arity$1(opts):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (opts["policyDb"]);
}
})();
var runtime = (function (){var or__5142__auto__ = ((cljs.core.map_QMARK_(opts))?new cljs.core.Keyword(null,"runtime","runtime",-1331573996).cljs$core$IFn$_invoke$arity$1(opts):null);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (opts["runtime"]);
}
})();
var client_id = (function (){var or__5142__auto__ = (process.env["KNOXX_GITHUB_OAUTH_CLIENT_ID"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var client_secret = (function (){var or__5142__auto__ = (process.env["KNOXX_GITHUB_OAUTH_CLIENT_SECRET"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var github_enabled = (((!(clojure.string.blank_QMARK_(client_id)))) && ((!(clojure.string.blank_QMARK_(client_secret)))));
if(cljs.core.truth_(policyDb)){
knoxx.backend.auth.session.set_db_session_store_BANG_(policyDb);
} else {
}

app.get("/api/auth/config",(function (_req,reply){
return reply.send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"githubEnabled","githubEnabled",1802398475),github_enabled,new cljs.core.Keyword(null,"publicBaseUrl","publicBaseUrl",-1021789883),public_base_url,new cljs.core.Keyword(null,"loginUrl","loginUrl",-889403941),((github_enabled)?"/api/auth/login":null)], null)));
}));

app.post("/api/auth/signup",(function (req,reply){
if(cljs.core.not(policyDb)){
return reply.code((503)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Knoxx policy database is not configured"], null)));
} else {
var body = (function (){var or__5142__auto__ = (req["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var email = clojure.string.lower_case(clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["email"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()))));
var display_name = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (body["displayName"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["display_name"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return email;
}
}
})())));
if(clojure.string.blank_QMARK_(email)){
return reply.code((400)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"email is required"], null)));
} else {
return policyDb.getBootstrapContext().then((function (bootstrap){
var primary_org = (bootstrap["primaryOrg"]);
var org_id = (primary_org["id"]);
var org_slug = (primary_org["slug"]);
return policyDb.createUser(({"email": email, "displayName": (function (){var or__5142__auto__ = display_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return email;
}
})(), "orgId": org_id, "roleSlugs": ["basic_user"], "authProvider": "signup", "status": "active", "membershipStatus": "active", "isDefault": true})).then((function (_){
return policyDb.resolveRequestContext(({"x-knoxx-user-email": email, "x-knoxx-org-slug": org_slug}));
})).then((function (ctx){
return knoxx.backend.auth.session.create_session_from_context_BANG_(reply,public_base_url,ctx,({"email": email, "displayName": (function (){var or__5142__auto__ = display_name;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return email;
}
})(), "authProvider": "signup"}));
}));
}),(function (result){
return reply.send(result);
}).then(),(function (err){
return reply.code((function (){var or__5142__auto__ = err.statusCode;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = err.status;
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return (500);
}
}
})()).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Signup failed";
}
})()], null)));
}).catch());
}
}
}));

app.get("/api/auth/login",(function (req,reply){
if((!(github_enabled))){
return reply.code((503)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"GitHub OAuth not configured"], null)));
} else {
var redirect = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__53110 = req;
var G__53110__$1 = (((G__53110 == null))?null:(G__53110["query"]));
if((G__53110__$1 == null)){
return null;
} else {
return (G__53110__$1["redirect"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "/";
}
})()));
var state = knoxx.backend.auth.session.create_state(redirect);
var callback_url = (new URL("/api/auth/callback/github",public_base_url)).toString();
var authorize_url = (new URL("https://github.com/login/oauth/authorize"));
authorize_url.searchParams.set("client_id",client_id);

authorize_url.searchParams.set("redirect_uri",callback_url);

authorize_url.searchParams.set("state",state);

authorize_url.searchParams.set("scope","read:user user:email");

return reply.redirect(authorize_url.toString());
}
}));

app.get("/api/auth/callback/github",(function (req,reply){
if((!(github_enabled))){
return reply.code((503)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"GitHub OAuth not configured"], null)));
} else {
var code = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__53116 = req;
var G__53116__$1 = (((G__53116 == null))?null:(G__53116["query"]));
if((G__53116__$1 == null)){
return null;
} else {
return (G__53116__$1["code"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
var state_val = (""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__53122 = req;
var G__53122__$1 = (((G__53122 == null))?null:(G__53122["query"]));
if((G__53122__$1 == null)){
return null;
} else {
return (G__53122__$1["state"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})()));
if(((clojure.string.blank_QMARK_(code)) || (clojure.string.blank_QMARK_(state_val)))){
return reply.code((400)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Missing code or state"], null)));
} else {
var temp__5823__auto__ = knoxx.backend.auth.session.consume_state(state_val);
if(cljs.core.truth_(temp__5823__auto__)){
var state_entry = temp__5823__auto__;
return knoxx.backend.auth.session.handle_github_callback(policyDb,reply,client_id,client_secret,state_entry,code,public_base_url);
} else {
return reply.code((400)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Invalid or expired state parameter"], null)));
}
}
}
}));

app.post("/api/auth/logout",(function (req,reply){
var cookie_token = (function (){var G__53134 = req;
var G__53134__$1 = (((G__53134 == null))?null:(G__53134["cookies"]));
if((G__53134__$1 == null)){
return null;
} else {
return (G__53134__$1[knoxx.backend.auth.session.COOKIE_NAME]);
}
})();
if(cljs.core.truth_(cookie_token)){
var payload_53229 = knoxx.backend.auth.session.verify_token(cookie_token);
if(cljs.core.truth_((function (){var and__5140__auto__ = payload_53229;
if(cljs.core.truth_(and__5140__auto__)){
return (payload_53229["sid"]);
} else {
return and__5140__auto__;
}
})())){
knoxx.backend.auth.session.delete_session((payload_53229["sid"]),cookie_token).catch((function (_){
return null;
}));
} else {
}
} else {
}

knoxx.backend.auth.session.clear_session_cookie(reply,public_base_url);

return reply.send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"ok","ok",967785236),true], null)));
}));

app.post("/api/auth/invite/redeem",(function (req,reply){
var code = clojure.string.trim((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5142__auto__ = (function (){var G__53143 = req;
var G__53143__$1 = (((G__53143 == null))?null:(G__53143["body"]));
if((G__53143__$1 == null)){
return null;
} else {
return (G__53143__$1["code"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})())));
if(clojure.string.blank_QMARK_(code)){
return reply.code((400)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Invite code is required"], null)));
} else {
var email = (function (){var or__5142__auto__ = (function (){var cookie_token = (function (){var G__53152 = req;
var G__53152__$1 = (((G__53152 == null))?null:(G__53152["cookies"]));
if((G__53152__$1 == null)){
return null;
} else {
return (G__53152__$1[knoxx.backend.auth.session.COOKIE_NAME]);
}
})();
if(cljs.core.truth_(cookie_token)){
var payload = knoxx.backend.auth.session.verify_token(cookie_token);
if(cljs.core.truth_((function (){var and__5140__auto__ = payload;
if(cljs.core.truth_(and__5140__auto__)){
return (payload["sid"]);
} else {
return and__5140__auto__;
}
})())){
return null;
} else {
return null;
}
} else {
return null;
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return clojure.string.trim((function (){var or__5142__auto____$1 = (req.headers["x-knoxx-user-email"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
})());
}
})();
if(clojure.string.blank_QMARK_(email)){
return reply.code((401)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"Not authenticated"], null)));
} else {
return policyDb.redeemInvite(code,email).then((function (result){
return reply.send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"invite","invite",126355381),(result["invite"]),new cljs.core.Keyword(null,"user","user",1532431356),(result["user"])], null)));
})).catch((function (err){
return reply.code((function (){var or__5142__auto__ = err.status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (500);
}
})()).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Invite redemption failed";
}
})()], null)));
}));
}
}
}));

return app.post("/api/auth/invite",(function (req,reply){
return knoxx.backend.auth.session.resolve_auth_context(req,policyDb).then((function (ctx){
var org_id = (function (){var or__5142__auto__ = (function (){var G__53165 = req;
var G__53165__$1 = (((G__53165 == null))?null:(G__53165["body"]));
if((G__53165__$1 == null)){
return null;
} else {
return (G__53165__$1["orgId"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__53166 = ctx;
var G__53166__$1 = (((G__53166 == null))?null:(G__53166["org"]));
if((G__53166__$1 == null)){
return null;
} else {
return (G__53166__$1["id"]);
}
}
})();
var email = (function (){var G__53167 = req;
var G__53167__$1 = (((G__53167 == null))?null:(G__53167["body"]));
if((G__53167__$1 == null)){
return null;
} else {
return (G__53167__$1["email"]);
}
})();
var role_slugs = (function (){var or__5142__auto__ = (function (){var G__53172 = req;
var G__53172__$1 = (((G__53172 == null))?null:(G__53172["body"]));
if((G__53172__$1 == null)){
return null;
} else {
return (G__53172__$1["roleSlugs"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ["knowledge_worker"];
}
})();
if(clojure.string.blank_QMARK_(email)){
return reply.code((400)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),"email is required"], null)));
} else {
return policyDb.createInvite(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id,new cljs.core.Keyword(null,"email","email",1415816706),email,new cljs.core.Keyword(null,"roleSlugs","roleSlugs",988302270),role_slugs,new cljs.core.Keyword(null,"inviterMembershipId","inviterMembershipId",5463144),(function (){var G__53173 = ctx;
var G__53173__$1 = (((G__53173 == null))?null:(G__53173["membership"]));
if((G__53173__$1 == null)){
return null;
} else {
return (G__53173__$1["id"]);
}
})()], null))).then((function (result){
if(cljs.core.not_EQ_.cljs$core$IFn$_invoke$arity$2((function (){var G__53175 = req;
var G__53175__$1 = (((G__53175 == null))?null:(G__53175["body"]));
if((G__53175__$1 == null)){
return null;
} else {
return (G__53175__$1["sendEmail"]);
}
})(),false)){
knoxx.backend.auth.session.send_invite_email(runtime,(result["invite"]),email,public_base_url).catch((function (err){
return console.error("[knoxx-session] Failed to send invite email:",err.message);
}));
} else {
}

return reply.send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"ok","ok",967785236),true,new cljs.core.Keyword(null,"invite","invite",126355381),(result["invite"])], null)));
})).catch((function (err){
return reply.code((function (){var or__5142__auto__ = err.status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (500);
}
})()).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Invite creation failed";
}
})()], null)));
}));
}
}),(function (err){
return reply.code((function (){var or__5142__auto__ = err.status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (401);
}
})()).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Unauthorized";
}
})()], null)));
}).catch());
}),app.get("/api/auth/invites",(function (req,reply){
return knoxx.backend.auth.session.resolve_auth_context(req,policyDb).then((function (ctx){
var org_id = (function (){var or__5142__auto__ = (function (){var G__53187 = req;
var G__53187__$1 = (((G__53187 == null))?null:(G__53187["query"]));
if((G__53187__$1 == null)){
return null;
} else {
return (G__53187__$1["orgId"]);
}
})();
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var G__53193 = ctx;
var G__53193__$1 = (((G__53193 == null))?null:(G__53193["org"]));
if((G__53193__$1 == null)){
return null;
} else {
return (G__53193__$1["id"]);
}
}
})();
var status = (function (){var G__53196 = req;
var G__53196__$1 = (((G__53196 == null))?null:(G__53196["query"]));
if((G__53196__$1 == null)){
return null;
} else {
return (G__53196__$1["status"]);
}
})();
return policyDb.listInvites(cljs.core.clj__GT_js((function (){var G__53200 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null);
if(cljs.core.truth_(status)){
return cljs.core.assoc.cljs$core$IFn$_invoke$arity$3(G__53200,new cljs.core.Keyword(null,"status","status",-1997798413),status);
} else {
return G__53200;
}
})())).then((function (result){
return reply.send(result);
})).catch((function (err){
return reply.code((500)).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),err.message], null)));
}));
})).catch((function (err){
return reply.code((function (){var or__5142__auto__ = err.status;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return (401);
}
})()).send(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"error","error",-978969032),(function (){var or__5142__auto__ = err.message;
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "Unauthorized";
}
})()], null)));
}));
})));
});

//# sourceMappingURL=knoxx.backend.routes.auth.js.map
