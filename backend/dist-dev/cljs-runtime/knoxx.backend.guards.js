import "./cljs_env.js";
import "./cljs.core.js";
import "./knoxx.backend.authz.js";
goog.provide('knoxx.backend.guards');
/**
 * Returns a Fastify preHandler that resolves the Knoxx auth context and
 * attaches it to request.ctx.  Calls done(err) on failure so Fastify
 * returns 500 automatically.
 * 
 * Use for routes that require an authenticated context.
 */
knoxx.backend.guards.make_session_guard = (function knoxx$backend$guards$make_session_guard(runtime){
return (function (req,_reply,done){
return knoxx.backend.authz.resolve_request_context_BANG_(runtime,req).then((function (ctx){
(req["ctx"] = ctx);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
})).catch(done);
});
});
/**
 * Returns a Fastify preHandler that opportunistically resolves auth context.
 * On any error (unauthenticated, policy-db unavailable, etc.) attaches nil
 * and continues — handler body should check `(when ctx ...)` before using it.
 * 
 * Use for routes where auth is optional (public endpoints that adapt when
 * a valid session is present).
 */
knoxx.backend.guards.make_optional_session_guard = (function knoxx$backend$guards$make_optional_session_guard(runtime){
return (function (req,_reply,done){
return knoxx.backend.authz.resolve_request_context_BANG_(runtime,req).then((function (ctx){
(req["ctx"] = ctx);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
})).catch((function (_){
(req["ctx"] = null);

return (done.cljs$core$IFn$_invoke$arity$0 ? done.cljs$core$IFn$_invoke$arity$0() : done.call(null));
}));
});
});

//# sourceMappingURL=knoxx.backend.guards.js.map
