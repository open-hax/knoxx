import "./cljs_env.js";
import "./cljs.core.js";
import "./clojure.string.js";
goog.provide('knoxx.backend.routes.users.admin');
knoxx.backend.routes.users.admin.register_user_admin_routes_BANG_ = (function knoxx$backend$routes$users$admin$register_user_admin_routes_BANG_(app,runtime,p__51540){
var map__51541 = p__51540;
var map__51541__$1 = cljs.core.__destructure_map(map__51541);
var route_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"route!","route!",-1286958144));
var json_response_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"json-response!","json-response!",103570476));
var with_request_context_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"with-request-context!","with-request-context!",1089168046));
var ensure_permission_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"ensure-permission!","ensure-permission!",1816359163));
var ensure_org_scope_BANG_ = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"ensure-org-scope!","ensure-org-scope!",-1115734566));
var policy_db = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"policy-db","policy-db",-1771109183));
var policy_db_promise = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"policy-db-promise","policy-db-promise",-584929935));
var http_error = cljs.core.get.cljs$core$IFn$_invoke$arity$2(map__51541__$1,new cljs.core.Keyword(null,"http-error","http-error",-1040049553));
var G__51543_51880 = app;
var G__51544_51881 = "GET";
var G__51545_51882 = "/api/admin/users";
var G__51546_51883 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var org_id = (function (){var or__5142__auto__ = (request["query"]["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (request["query"]["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return null;
}
}
})();
var G__51551 = runtime;
var G__51552 = request;
var G__51553 = reply;
var G__51554 = (function (ctx){
if(cljs.core.truth_(org_id)){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.read"));
} else {
(ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2 ? ensure_permission_BANG_.cljs$core$IFn$_invoke$arity$2(ctx,"platform.org.read") : ensure_permission_BANG_.call(null,ctx,"platform.org.read"));
}

var G__51555 = runtime;
var G__51556 = reply;
var G__51557 = (200);
var G__51558 = db.listUsers(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51555,G__51556,G__51557,G__51558) : policy_db_promise.call(null,G__51555,G__51556,G__51557,G__51558));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51551,G__51552,G__51553,G__51554) : with_request_context_BANG_.call(null,G__51551,G__51552,G__51553,G__51554));
} else {
var G__51559 = reply;
var G__51560 = (503);
var G__51561 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51559,G__51560,G__51561) : json_response_BANG_.call(null,G__51559,G__51560,G__51561));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51543_51880,G__51544_51881,G__51545_51882,G__51546_51883) : route_BANG_.call(null,G__51543_51880,G__51544_51881,G__51545_51882,G__51546_51883));

var G__51562_51961 = app;
var G__51563_51962 = "POST";
var G__51564_51963 = "/api/admin/users";
var G__51565_51964 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var body = (function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})();
var org_id = (function (){var or__5142__auto__ = (body["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var G__51566 = runtime;
var G__51567 = request;
var G__51568 = reply;
var G__51569 = (function (ctx){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((400),"org_required","orgId is required") : http_error.call(null,(400),"org_required","orgId is required"));
} else {
}

(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.create") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.create"));

var G__51570 = runtime;
var G__51571 = reply;
var G__51572 = (201);
var G__51573 = db.createUser(body);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51570,G__51571,G__51572,G__51573) : policy_db_promise.call(null,G__51570,G__51571,G__51572,G__51573));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51566,G__51567,G__51568,G__51569) : with_request_context_BANG_.call(null,G__51566,G__51567,G__51568,G__51569));
} else {
var G__51574 = reply;
var G__51575 = (503);
var G__51576 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51574,G__51575,G__51576) : json_response_BANG_.call(null,G__51574,G__51575,G__51576));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51562_51961,G__51563_51962,G__51564_51963,G__51565_51964) : route_BANG_.call(null,G__51562_51961,G__51563_51962,G__51564_51963,G__51565_51964));

var G__51577_51967 = app;
var G__51578_51968 = "GET";
var G__51579_51969 = "/api/admin/orgs/:orgId/users";
var G__51580_51970 = (function (request,reply){
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
var G__51581 = runtime;
var G__51582 = request;
var G__51583 = reply;
var G__51584 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.read"));

var G__51587 = runtime;
var G__51588 = reply;
var G__51589 = (200);
var G__51590 = db.listUsers(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51587,G__51588,G__51589,G__51590) : policy_db_promise.call(null,G__51587,G__51588,G__51589,G__51590));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51581,G__51582,G__51583,G__51584) : with_request_context_BANG_.call(null,G__51581,G__51582,G__51583,G__51584));
} else {
var G__51598 = reply;
var G__51599 = (503);
var G__51600 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51598,G__51599,G__51600) : json_response_BANG_.call(null,G__51598,G__51599,G__51600));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51577_51967,G__51578_51968,G__51579_51969,G__51580_51970) : route_BANG_.call(null,G__51577_51967,G__51578_51968,G__51579_51969,G__51580_51970));

var G__51613_51975 = app;
var G__51614_51976 = "GET";
var G__51615_51977 = "/api/admin/orgs/:orgId/actors";
var G__51616_51978 = (function (request,reply){
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
var G__51617 = runtime;
var G__51618 = request;
var G__51619 = reply;
var G__51620 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.read"));

var G__51621 = runtime;
var G__51622 = reply;
var G__51623 = (200);
var G__51624 = db.syncActorContracts().then((function (_){
return db.listUsers(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
}));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51621,G__51622,G__51623,G__51624) : policy_db_promise.call(null,G__51621,G__51622,G__51623,G__51624));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51617,G__51618,G__51619,G__51620) : with_request_context_BANG_.call(null,G__51617,G__51618,G__51619,G__51620));
} else {
var G__51625 = reply;
var G__51626 = (503);
var G__51627 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51625,G__51626,G__51627) : json_response_BANG_.call(null,G__51625,G__51626,G__51627));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51613_51975,G__51614_51976,G__51615_51977,G__51616_51978) : route_BANG_.call(null,G__51613_51975,G__51614_51976,G__51615_51977,G__51616_51978));

var G__51630_51984 = app;
var G__51631_51985 = "POST";
var G__51632_51986 = "/api/admin/orgs/:orgId/users";
var G__51633_51987 = (function (request,reply){
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
var G__51634 = runtime;
var G__51635 = request;
var G__51636 = reply;
var G__51637 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.create") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.create"));

var G__51638 = runtime;
var G__51639 = reply;
var G__51640 = (201);
var G__51641 = db.createUser(payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51638,G__51639,G__51640,G__51641) : policy_db_promise.call(null,G__51638,G__51639,G__51640,G__51641));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51634,G__51635,G__51636,G__51637) : with_request_context_BANG_.call(null,G__51634,G__51635,G__51636,G__51637));
} else {
var G__51646 = reply;
var G__51647 = (503);
var G__51648 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51646,G__51647,G__51648) : json_response_BANG_.call(null,G__51646,G__51647,G__51648));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51630_51984,G__51631_51985,G__51632_51986,G__51633_51987) : route_BANG_.call(null,G__51630_51984,G__51631_51985,G__51632_51986,G__51633_51987));

var G__51655_51998 = app;
var G__51656_51999 = "POST";
var G__51657_52000 = "/api/admin/orgs/:orgId/actors";
var G__51658_52001 = (function (request,reply){
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
var G__51659 = runtime;
var G__51660 = request;
var G__51661 = reply;
var G__51662 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.users.create") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.users.create"));

var G__51663 = runtime;
var G__51664 = reply;
var G__51665 = (201);
var G__51666 = db.createUser(payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51663,G__51664,G__51665,G__51666) : policy_db_promise.call(null,G__51663,G__51664,G__51665,G__51666));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51659,G__51660,G__51661,G__51662) : with_request_context_BANG_.call(null,G__51659,G__51660,G__51661,G__51662));
} else {
var G__51669 = reply;
var G__51670 = (503);
var G__51671 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51669,G__51670,G__51671) : json_response_BANG_.call(null,G__51669,G__51670,G__51671));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51655_51998,G__51656_51999,G__51657_52000,G__51658_52001) : route_BANG_.call(null,G__51655_51998,G__51656_51999,G__51657_52000,G__51658_52001));

var G__51672_52024 = app;
var G__51673_52025 = "PATCH";
var G__51674_52026 = "/api/admin/users/:userId";
var G__51675_52027 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var user_id = (function (){var or__5142__auto__ = (request["params"]["userId"]);
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
var org_id = (function (){var or__5142__auto__ = (body["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var G__51688 = runtime;
var G__51689 = request;
var G__51690 = reply;
var G__51691 = (function (ctx){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((400),"org_required","orgId is required") : http_error.call(null,(400),"org_required","orgId is required"));
} else {
}

(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.members.update") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.members.update"));

var G__51699 = runtime;
var G__51700 = reply;
var G__51701 = (200);
var G__51702 = db.updateUserActor(user_id,body);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51699,G__51700,G__51701,G__51702) : policy_db_promise.call(null,G__51699,G__51700,G__51701,G__51702));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51688,G__51689,G__51690,G__51691) : with_request_context_BANG_.call(null,G__51688,G__51689,G__51690,G__51691));
} else {
var G__51711 = reply;
var G__51712 = (503);
var G__51713 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51711,G__51712,G__51713) : json_response_BANG_.call(null,G__51711,G__51712,G__51713));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51672_52024,G__51673_52025,G__51674_52026,G__51675_52027) : route_BANG_.call(null,G__51672_52024,G__51673_52025,G__51674_52026,G__51675_52027));

var G__51718_52029 = app;
var G__51719_52030 = "PATCH";
var G__51720_52031 = "/api/admin/actors/:userId";
var G__51721_52032 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var user_id = (function (){var or__5142__auto__ = (request["params"]["userId"]);
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
var org_id = (function (){var or__5142__auto__ = (body["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var G__51744 = runtime;
var G__51745 = request;
var G__51746 = reply;
var G__51747 = (function (ctx){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((400),"org_required","orgId is required") : http_error.call(null,(400),"org_required","orgId is required"));
} else {
}

(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.members.update") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.members.update"));

var G__51748 = runtime;
var G__51749 = reply;
var G__51750 = (200);
var G__51751 = db.updateUserActor(user_id,body);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51748,G__51749,G__51750,G__51751) : policy_db_promise.call(null,G__51748,G__51749,G__51750,G__51751));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51744,G__51745,G__51746,G__51747) : with_request_context_BANG_.call(null,G__51744,G__51745,G__51746,G__51747));
} else {
var G__51752 = reply;
var G__51753 = (503);
var G__51754 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51752,G__51753,G__51754) : json_response_BANG_.call(null,G__51752,G__51753,G__51754));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51718_52029,G__51719_52030,G__51720_52031,G__51721_52032) : route_BANG_.call(null,G__51718_52029,G__51719_52030,G__51720_52031,G__51721_52032));

var G__51756_52061 = app;
var G__51757_52064 = "PUT";
var G__51758_52065 = "/api/admin/users/:userId/credentials/:provider";
var G__51759_52066 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var user_id = (function (){var or__5142__auto__ = (request["params"]["userId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var provider = (function (){var or__5142__auto__ = (request["params"]["provider"]);
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
var org_id = (function (){var or__5142__auto__ = (body["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var payload = Object.assign(({}),body,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"provider","provider",-302056900),provider], null)));
var G__51762 = runtime;
var G__51763 = request;
var G__51764 = reply;
var G__51765 = (function (ctx){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((400),"org_required","orgId is required") : http_error.call(null,(400),"org_required","orgId is required"));
} else {
}

(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.user_policy.update") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.user_policy.update"));

var G__51767 = runtime;
var G__51768 = reply;
var G__51769 = (200);
var G__51770 = db.upsertActorCredential(user_id,payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51767,G__51768,G__51769,G__51770) : policy_db_promise.call(null,G__51767,G__51768,G__51769,G__51770));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51762,G__51763,G__51764,G__51765) : with_request_context_BANG_.call(null,G__51762,G__51763,G__51764,G__51765));
} else {
var G__51771 = reply;
var G__51772 = (503);
var G__51773 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51771,G__51772,G__51773) : json_response_BANG_.call(null,G__51771,G__51772,G__51773));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51756_52061,G__51757_52064,G__51758_52065,G__51759_52066) : route_BANG_.call(null,G__51756_52061,G__51757_52064,G__51758_52065,G__51759_52066));

var G__51774_52112 = app;
var G__51775_52113 = "PUT";
var G__51776_52114 = "/api/admin/actors/:userId/credentials/:provider";
var G__51777_52115 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var user_id = (function (){var or__5142__auto__ = (request["params"]["userId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var provider = (function (){var or__5142__auto__ = (request["params"]["provider"]);
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
var org_id = (function (){var or__5142__auto__ = (body["orgId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
var or__5142__auto____$1 = (body["org_id"]);
if(cljs.core.truth_(or__5142__auto____$1)){
return or__5142__auto____$1;
} else {
return "";
}
}
})();
var payload = Object.assign(({}),body,cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"provider","provider",-302056900),provider], null)));
var G__51780 = runtime;
var G__51781 = request;
var G__51782 = reply;
var G__51783 = (function (ctx){
if(clojure.string.blank_QMARK_((""+cljs.core.str.cljs$core$IFn$_invoke$arity$1(org_id)))){
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((400),"org_required","orgId is required") : http_error.call(null,(400),"org_required","orgId is required"));
} else {
}

(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.user_policy.update") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.user_policy.update"));

var G__51784 = runtime;
var G__51785 = reply;
var G__51786 = (200);
var G__51787 = db.upsertActorCredential(user_id,payload);
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51784,G__51785,G__51786,G__51787) : policy_db_promise.call(null,G__51784,G__51785,G__51786,G__51787));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51780,G__51781,G__51782,G__51783) : with_request_context_BANG_.call(null,G__51780,G__51781,G__51782,G__51783));
} else {
var G__51789 = reply;
var G__51790 = (503);
var G__51791 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51789,G__51790,G__51791) : json_response_BANG_.call(null,G__51789,G__51790,G__51791));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51774_52112,G__51775_52113,G__51776_52114,G__51777_52115) : route_BANG_.call(null,G__51774_52112,G__51775_52113,G__51776_52114,G__51777_52115));

var G__51792_52148 = app;
var G__51793_52149 = "GET";
var G__51794_52150 = "/api/admin/orgs/:orgId/memberships";
var G__51795_52151 = (function (request,reply){
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
var G__51798 = runtime;
var G__51799 = request;
var G__51800 = reply;
var G__51801 = (function (ctx){
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(ctx,org_id,"org.members.read") : ensure_org_scope_BANG_.call(null,ctx,org_id,"org.members.read"));

var G__51804 = runtime;
var G__51805 = reply;
var G__51806 = (200);
var G__51807 = db.listMemberships(cljs.core.clj__GT_js(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"orgId","orgId",-73585595),org_id], null)));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51804,G__51805,G__51806,G__51807) : policy_db_promise.call(null,G__51804,G__51805,G__51806,G__51807));
});
return (with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51798,G__51799,G__51800,G__51801) : with_request_context_BANG_.call(null,G__51798,G__51799,G__51800,G__51801));
} else {
var G__51810 = reply;
var G__51811 = (503);
var G__51812 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51810,G__51811,G__51812) : json_response_BANG_.call(null,G__51810,G__51811,G__51812));
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51792_52148,G__51793_52149,G__51794_52150,G__51795_52151) : route_BANG_.call(null,G__51792_52148,G__51793_52149,G__51794_52150,G__51795_52151));

var G__51814_52164 = app;
var G__51815_52165 = "PATCH";
var G__51816_52166 = "/api/admin/memberships/:membershipId/roles";
var G__51817_52167 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var membership_id = (function (){var or__5142__auto__ = (request["params"]["membershipId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__51823_52169 = runtime;
var G__51824_52170 = request;
var G__51825_52171 = reply;
var G__51826_52172 = (function (ctx){
var G__51828 = runtime;
var G__51829 = reply;
var G__51830 = (200);
var G__51831 = db.getMembership(membership_id).then((function (result){
var membership = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((result["membership"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_(membership)){
} else {
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((404),"membership_not_found","membership not found") : http_error.call(null,(404),"membership_not_found","membership not found"));
}

var G__51838_52181 = ctx;
var G__51839_52182 = new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(membership);
var G__51840_52183 = "org.members.update";
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(G__51838_52181,G__51839_52182,G__51840_52183) : ensure_org_scope_BANG_.call(null,G__51838_52181,G__51839_52182,G__51840_52183));

return db.setMembershipRoles(membership_id,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
}));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51828,G__51829,G__51830,G__51831) : policy_db_promise.call(null,G__51828,G__51829,G__51830,G__51831));
});
(with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51823_52169,G__51824_52170,G__51825_52171,G__51826_52172) : with_request_context_BANG_.call(null,G__51823_52169,G__51824_52170,G__51825_52171,G__51826_52172));

var G__51841 = reply;
var G__51842 = (503);
var G__51843 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51841,G__51842,G__51843) : json_response_BANG_.call(null,G__51841,G__51842,G__51843));
} else {
return null;
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51814_52164,G__51815_52165,G__51816_52166,G__51817_52167) : route_BANG_.call(null,G__51814_52164,G__51815_52165,G__51816_52166,G__51817_52167));

var G__51844_52200 = app;
var G__51845_52201 = "PATCH";
var G__51846_52202 = "/api/admin/memberships/:membershipId/tool-policies";
var G__51847_52203 = (function (request,reply){
var temp__5823__auto__ = (policy_db.cljs$core$IFn$_invoke$arity$1 ? policy_db.cljs$core$IFn$_invoke$arity$1(runtime) : policy_db.call(null,runtime));
if(cljs.core.truth_(temp__5823__auto__)){
var db = temp__5823__auto__;
var membership_id = (function (){var or__5142__auto__ = (request["params"]["membershipId"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return "";
}
})();
var G__51848_52204 = runtime;
var G__51849_52205 = request;
var G__51850_52206 = reply;
var G__51851_52207 = (function (ctx){
var G__51854 = runtime;
var G__51855 = reply;
var G__51856 = (200);
var G__51857 = db.getMembership(membership_id).then((function (result){
var membership = cljs.core.js__GT_clj.cljs$core$IFn$_invoke$arity$variadic((result["membership"]),cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true], 0));
if(cljs.core.truth_(membership)){
} else {
throw (http_error.cljs$core$IFn$_invoke$arity$3 ? http_error.cljs$core$IFn$_invoke$arity$3((404),"membership_not_found","membership not found") : http_error.call(null,(404),"membership_not_found","membership not found"));
}

var G__51858_52216 = ctx;
var G__51859_52217 = new cljs.core.Keyword(null,"orgId","orgId",-73585595).cljs$core$IFn$_invoke$arity$1(membership);
var G__51860_52218 = "org.user_policy.update";
(ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3 ? ensure_org_scope_BANG_.cljs$core$IFn$_invoke$arity$3(G__51858_52216,G__51859_52217,G__51860_52218) : ensure_org_scope_BANG_.call(null,G__51858_52216,G__51859_52217,G__51860_52218));

return db.setMembershipToolPolicies(membership_id,(function (){var or__5142__auto__ = (request["body"]);
if(cljs.core.truth_(or__5142__auto__)){
return or__5142__auto__;
} else {
return ({});
}
})());
}));
return (policy_db_promise.cljs$core$IFn$_invoke$arity$4 ? policy_db_promise.cljs$core$IFn$_invoke$arity$4(G__51854,G__51855,G__51856,G__51857) : policy_db_promise.call(null,G__51854,G__51855,G__51856,G__51857));
});
(with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4 ? with_request_context_BANG_.cljs$core$IFn$_invoke$arity$4(G__51848_52204,G__51849_52205,G__51850_52206,G__51851_52207) : with_request_context_BANG_.call(null,G__51848_52204,G__51849_52205,G__51850_52206,G__51851_52207));

var G__51863 = reply;
var G__51864 = (503);
var G__51865 = new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"detail","detail",-1545345025),"Knoxx policy database is not configured"], null);
return (json_response_BANG_.cljs$core$IFn$_invoke$arity$3 ? json_response_BANG_.cljs$core$IFn$_invoke$arity$3(G__51863,G__51864,G__51865) : json_response_BANG_.call(null,G__51863,G__51864,G__51865));
} else {
return null;
}
});
(route_BANG_.cljs$core$IFn$_invoke$arity$4 ? route_BANG_.cljs$core$IFn$_invoke$arity$4(G__51844_52200,G__51845_52201,G__51846_52202,G__51847_52203) : route_BANG_.call(null,G__51844_52200,G__51845_52201,G__51846_52202,G__51847_52203));

return null;
});

//# sourceMappingURL=knoxx.backend.routes.users.admin.js.map
