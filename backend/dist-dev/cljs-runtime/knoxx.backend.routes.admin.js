import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.routes.users.admin.js";
goog.provide('knoxx.backend.routes.admin');
knoxx.backend.routes.admin.register_admin_routes_BANG_ = (function knoxx$backend$routes$admin$register_admin_routes_BANG_(app,runtime,p__51971){
var map__51974 = p__51971;
var map__51974__$1 = cljs.core.__destructure_map(map__51974);
var deps = map__51974__$1;
var ensure_org_scope_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"ensure-org-scope!","ensure-org-scope!",-1115734566));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var ensure_any_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"ensure-any-permission!","ensure-any-permission!",1999271593));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var http_error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"http-error","http-error",-1040049553));
var policy_db_promise = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51974__$1,new cljs.core.Keyword(null,"policy-db-promise","policy-db-promise",-584929935));
var G__51990_52257 = app;
var G__51991_52258 = "GET";
var G__51992_52259 = "/api/admin/bootstrap";
var G__51993_52260 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var G__51994 = runtime;
var G__51995 = request;
var G__51996 = reply;
var G__51997 = (function (ctx){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.read") : ensure_permission_BANG_.call(null,ctx,"platform.org.read"));

var G__52002 = runtime;
var G__52003 = reply;
var G__52004 = (200);
var G__52005 = db.getBootstrapContext();
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52002,G__52003,G__52004,G__52005) : policy_db_promise.call(null,G__52002,G__52003,G__52004,G__52005));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51994,G__51995,G__51996,G__51997) : with_request_context_BANG_.call(null,G__51994,G__51995,G__51996,G__51997));
} else {
var G__52009 = reply;
var G__52010 = (503);
var G__52011 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52009,G__52010,G__52011) : json_response_BANG_.call(null,G__52009,G__52010,G__52011));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51990_52257,G__51991_52258,G__51992_52259,G__51993_52260) : route_BANG_.call(null,G__51990_52257,G__51991_52258,G__51992_52259,G__51993_52260));

var G__52012_52263 = app;
var G__52013_52264 = "GET";
var G__52014_52265 = "/api/admin/permissions";
var G__52015_52266 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var G__52016 = runtime;
var G__52017 = request;
var G__52018 = reply;
var G__52019 = (function (ctx){
var G__52020_52267 = ctx;
var G__52021_52268 = new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["platform.roles.manage","org.roles.read"], null);
var G__52022_52269 = "permission_denied";
var G__52023_52270 = "Role permission metadata is outside the current Knoxx scope";
(ensure_any_permission_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_any_permission_BANG_.cljs$core$IFn$_invoke$arity$4(G__52020_52267,G__52021_52268,G__52022_52269,G__52023_52270) : ensure_any_permission_BANG_.call(null,G__52020_52267,G__52021_52268,G__52022_52269,G__52023_52270));

var G__52034 = runtime;
var G__52035 = reply;
var G__52036 = (200);
var G__52037 = db.listPermissions();
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52034,G__52035,G__52036,G__52037) : policy_db_promise.call(null,G__52034,G__52035,G__52036,G__52037));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52016,G__52017,G__52018,G__52019) : with_request_context_BANG_.call(null,G__52016,G__52017,G__52018,G__52019));
} else {
var G__52039 = reply;
var G__52040 = (503);
var G__52041 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52039,G__52040,G__52041) : json_response_BANG_.call(null,G__52039,G__52040,G__52041));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52012_52263,G__52013_52264,G__52014_52265,G__52015_52266) : route_BANG_.call(null,G__52012_52263,G__52013_52264,G__52014_52265,G__52015_52266));

var G__52049_52272 = app;
var G__52050_52273 = "GET";
var G__52051_52274 = "/api/admin/tools";
var G__52052_52275 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var G__52059 = runtime;
var G__52060 = request;
var G__52062 = reply;
var G__52063 = (function (ctx){
var G__52073_52276 = ctx;
var G__52074_52277 = new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, ["platform.roles.manage","org.tool_policy.read","org.user_policy.read"], null);
var G__52075_52278 = "permission_denied";
var G__52076_52279 = "Tool policy metadata is outside the current Knoxx scope";
(ensure_any_permission_BANG_.cljs$core$IFn$_invoke$arity$4 ? ensure_any_permission_BANG_.cljs$core$IFn$_invoke$arity$4(G__52073_52276,G__52074_52277,G__52075_52278,G__52076_52279) : ensure_any_permission_BANG_.call(null,G__52073_52276,G__52074_52277,G__52075_52278,G__52076_52279));

var G__52081 = runtime;
var G__52082 = reply;
var G__52083 = (200);
var G__52084 = db.listTools();
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52081,G__52082,G__52083,G__52084) : policy_db_promise.call(null,G__52081,G__52082,G__52083,G__52084));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52059,G__52060,G__52062,G__52063) : with_request_context_BANG_.call(null,G__52059,G__52060,G__52062,G__52063));
} else {
var G__52091 = reply;
var G__52092 = (503);
var G__52093 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52091,G__52092,G__52093) : json_response_BANG_.call(null,G__52091,G__52092,G__52093));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52049_52272,G__52050_52273,G__52051_52274,G__52052_52275) : route_BANG_.call(null,G__52049_52272,G__52050_52273,G__52051_52274,G__52052_52275));

var G__52097_52280 = app;
var G__52098_52281 = "GET";
var G__52099_52282 = "/api/admin/orgs";
var G__52100_52283 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var G__52101 = runtime;
var G__52102 = request;
var G__52103 = reply;
var G__52104 = (function (ctx){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.read") : ensure_permission_BANG_.call(null,ctx,"platform.org.read"));

var G__52105 = runtime;
var G__52106 = reply;
var G__52107 = (200);
var G__52108 = db.listOrgs();
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52105,G__52106,G__52107,G__52108) : policy_db_promise.call(null,G__52105,G__52106,G__52107,G__52108));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52101,G__52102,G__52103,G__52104) : with_request_context_BANG_.call(null,G__52101,G__52102,G__52103,G__52104));
} else {
var G__52109 = reply;
var G__52110 = (503);
var G__52111 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52109,G__52110,G__52111) : json_response_BANG_.call(null,G__52109,G__52110,G__52111));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52097_52280,G__52098_52281,G__52099_52282,G__52100_52283) : route_BANG_.call(null,G__52097_52280,G__52098_52281,G__52099_52282,G__52100_52283));

var G__52117_52287 = app;
var G__52118_52288 = "POST";
var G__52119_52289 = "/api/admin/orgs";
var G__52120_52290 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var G__52121 = runtime;
var G__52122 = request;
var G__52123 = reply;
var G__52124 = (function (ctx){
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.create") : ensure_permission_BANG_.call(null,ctx,"platform.org.create"));

var G__52126 = runtime;
var G__52127 = reply;
var G__52128 = (201);
var G__52129 = db.createOrg((function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52126,G__52127,G__52128,G__52129) : policy_db_promise.call(null,G__52126,G__52127,G__52128,G__52129));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52121,G__52122,G__52123,G__52124) : with_request_context_BANG_.call(null,G__52121,G__52122,G__52123,G__52124));
} else {
var G__52131 = reply;
var G__52132 = (503);
var G__52133 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52131,G__52132,G__52133) : json_response_BANG_.call(null,G__52131,G__52132,G__52133));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52117_52287,G__52118_52288,G__52119_52289,G__52120_52290) : route_BANG_.call(null,G__52117_52287,G__52118_52288,G__52119_52289,G__52120_52290));

knoxx.backend.routes.users.admin.register_user_admin_routes_BANG_(app,runtime,deps);

var G__52139_52292 = app;
var G__52140_52293 = "GET";
var G__52141_52294 = "/api/admin/orgs/:orgId/roles";
var G__52142_52295 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var org_id = (function (){var or__5142__auto__ = (request["params"]["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__52144 = runtime;
var G__52145 = request;
var G__52146 = reply;
var G__52147 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.roles.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.roles.read"));

var G__52152 = runtime;
var G__52153 = reply;
var G__52154 = (200);
var G__52155 = db.listRoles(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52152,G__52153,G__52154,G__52155) : policy_db_promise.call(null,G__52152,G__52153,G__52154,G__52155));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52144,G__52145,G__52146,G__52147) : with_request_context_BANG_.call(null,G__52144,G__52145,G__52146,G__52147));
} else {
var G__52157 = reply;
var G__52158 = (503);
var G__52159 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52157,G__52158,G__52159) : json_response_BANG_.call(null,G__52157,G__52158,G__52159));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52139_52292,G__52140_52293,G__52141_52294,G__52142_52295) : route_BANG_.call(null,G__52139_52292,G__52140_52293,G__52141_52294,G__52142_52295));

var G__52160_52296 = app;
var G__52161_52297 = "POST";
var G__52162_52298 = "/api/admin/orgs/:orgId/roles";
var G__52163_52299 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var org_id = (function (){var or__5142__auto__ = (request["params"]["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var payload = Object.assign(({}),body,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
var G__52177 = runtime;
var G__52178 = request;
var G__52179 = reply;
var G__52180 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.roles.create") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.roles.create"));

var G__52185 = runtime;
var G__52186 = reply;
var G__52187 = (201);
var G__52188 = db.createRole(payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52185,G__52186,G__52187,G__52188) : policy_db_promise.call(null,G__52185,G__52186,G__52187,G__52188));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52177,G__52178,G__52179,G__52180) : with_request_context_BANG_.call(null,G__52177,G__52178,G__52179,G__52180));
} else {
var G__52191 = reply;
var G__52192 = (503);
var G__52193 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52191,G__52192,G__52193) : json_response_BANG_.call(null,G__52191,G__52192,G__52193));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52160_52296,G__52161_52297,G__52162_52298,G__52163_52299) : route_BANG_.call(null,G__52160_52296,G__52161_52297,G__52162_52298,G__52163_52299));

var G__52195_52300 = app;
var G__52196_52301 = "PATCH";
var G__52197_52302 = "/api/admin/roles/:roleId/tool-policies";
var G__52198_52303 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var role_id = (function (){var or__5142__auto__ = (request["params"]["roleId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__52208_52304 = runtime;
var G__52209_52305 = request;
var G__52210_52306 = reply;
var G__52211_52307 = (function (ctx){
var G__52212 = runtime;
var G__52213 = reply;
var G__52214 = (200);
var G__52215 = db.getRole(role_id).then((function (result){
var role = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((result["role"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_(role)){
} else {
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((404),"role_not_found","role not found") : http_error.call(null,(404),"role_not_found","role not found"));
}

var G__52220_52308 = ctx;
var G__52221_52309 = new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(role);
var G__52222_52310 = "org.tool_policy.update";
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(G__52220_52308,G__52221_52309,G__52222_52310) : ensure_org_scope_BANG_.call(null,G__52220_52308,G__52221_52309,G__52222_52310));

return db.setRoleToolPolicies(role_id,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
}));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52212,G__52213,G__52214,G__52215) : policy_db_promise.call(null,G__52212,G__52213,G__52214,G__52215));
});
(with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52208_52304,G__52209_52305,G__52210_52306,G__52211_52307) : with_request_context_BANG_.call(null,G__52208_52304,G__52209_52305,G__52210_52306,G__52211_52307));

var G__52223 = reply;
var G__52224 = (503);
var G__52225 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52223,G__52224,G__52225) : json_response_BANG_.call(null,G__52223,G__52224,G__52225));
} else {
return null;
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52195_52300,G__52196_52301,G__52197_52302,G__52198_52303) : route_BANG_.call(null,G__52195_52300,G__52196_52301,G__52197_52302,G__52198_52303));

var G__52226_52311 = app;
var G__52227_52312 = "GET";
var G__52228_52313 = "/api/admin/orgs/:orgId/data-lakes";
var G__52229_52314 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var org_id = (function (){var or__5142__auto__ = (request["params"]["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__52230 = runtime;
var G__52231 = request;
var G__52232 = reply;
var G__52233 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.datalakes.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.datalakes.read"));

var G__52235 = runtime;
var G__52236 = reply;
var G__52237 = (200);
var G__52238 = db.listDataLakes(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52235,G__52236,G__52237,G__52238) : policy_db_promise.call(null,G__52235,G__52236,G__52237,G__52238));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52230,G__52231,G__52232,G__52233) : with_request_context_BANG_.call(null,G__52230,G__52231,G__52232,G__52233));
} else {
var G__52239 = reply;
var G__52240 = (503);
var G__52241 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52239,G__52240,G__52241) : json_response_BANG_.call(null,G__52239,G__52240,G__52241));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52226_52311,G__52227_52312,G__52228_52313,G__52229_52314) : route_BANG_.call(null,G__52226_52311,G__52227_52312,G__52228_52313,G__52229_52314));

var G__52242_52315 = app;
var G__52243_52316 = "POST";
var G__52244_52317 = "/api/admin/orgs/:orgId/data-lakes";
var G__52245_52318 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var org_id = (function (){var or__5142__auto__ = (request["params"]["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var payload = Object.assign(({}),body,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
var G__52246 = runtime;
var G__52247 = request;
var G__52248 = reply;
var G__52249 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.datalakes.create") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.datalakes.create"));

var G__52250 = runtime;
var G__52251 = reply;
var G__52252 = (201);
var G__52253 = db.createDataLake(payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__52250,G__52251,G__52252,G__52253) : policy_db_promise.call(null,G__52250,G__52251,G__52252,G__52253));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__52246,G__52247,G__52248,G__52249) : with_request_context_BANG_.call(null,G__52246,G__52247,G__52248,G__52249));
} else {
var G__52254 = reply;
var G__52255 = (503);
var G__52256 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__52254,G__52255,G__52256) : json_response_BANG_.call(null,G__52254,G__52255,G__52256));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__52242_52315,G__52243_52316,G__52244_52317,G__52245_52318) : route_BANG_.call(null,G__52242_52315,G__52243_52316,G__52244_52317,G__52245_52318));

return null;
});

//# sourceMappingURL=knoxx.backend.routes.admin.js.map
