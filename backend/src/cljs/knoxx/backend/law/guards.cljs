(ns knoxx.backend.guards
  (:require [knoxx.backend.authz :as authz]))

;; ── Fastify preHandler factories ─────────────────────────────────────────────
;;
;; Each make-* fn returns a (fn [request reply done]) suitable for use in the
;; defroute preHandlers vector:
;;
;;   (defroute my-route! [ensure-role-can-use!]
;;     "GET" "/api/foo"
;;     [session-guard]
;;     (json-response! reply 200 {:user (ctx-user-id ctx)}))
;;
;; Guards attach their payload to request.* slots:
;;   session-guard          → request.ctx
;;   optional-session-guard → request.ctx (nil if unauthenticated)

(defn make-session-guard
  "Returns a Fastify preHandler that resolves the Knoxx auth context and
   attaches it to request.ctx.  Calls done(err) on failure so Fastify
   returns 500 automatically.

   Use for routes that require an authenticated context."
  [runtime]
  (fn [req _reply done]
    (-> (authz/resolve-request-context! runtime req)
        (.then (fn [ctx]
                 (aset req "ctx" ctx)
                 (done)))
        (.catch done))))

(defn make-optional-session-guard
  "Returns a Fastify preHandler that opportunistically resolves auth context.
   On any error (unauthenticated, policy-db unavailable, etc.) attaches nil
   and continues — handler body should check `(when ctx ...)` before using it.

   Use for routes where auth is optional (public endpoints that adapt when
   a valid session is present)."
  [runtime]
  (fn [req _reply done]
    (-> (authz/resolve-request-context! runtime req)
        (.then (fn [ctx]
                 (aset req "ctx" ctx)
                 (done)))
        (.catch (fn [_]
                  (aset req "ctx" nil)
                  (done))))))
