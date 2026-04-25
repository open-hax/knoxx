(ns knoxx.backend.macros)

;; defroute — unified route definition macro.
;;
;; Signature:
;;   (defroute fn-name [extra-dep-syms] method path [& pre-handlers] & body)
;;   (defroute fn-name [extra-dep-syms] method path & body)           ; classic, no preHandlers
;;
;; Expands to:
;;   (defn fn-name [app runtime config deps] ...)
;;
;; preHandler mode (preferred for new routes):
;;   [pre-handlers] is a vector of Fastify preHandler hook fns, looked up from
;;   deps by keyword or passed directly. Fastify runs them in order before the
;;   handler; each hook attaches its payload to request.* and calls done().
;;   The handler body receives `ctx` bound from (aget request "ctx").
;;
;;   Example guard in deps:
;;     (defn make-session-guard [runtime]
;;       (fn [req _reply done]
;;         (-> (with-request-context! runtime req)
;;             (.then (fn [ctx] (aset req "ctx" ctx) (done)))
;;             (.catch done))))
;;
;;   Usage:
;;     (defroute my-route! [ensure-role-can-use!]
;;       "GET" "/api/foo"
;;       [session-guard]
;;       (json-response! reply 200 {:ctx ctx}))
;;
;; Classic mode (backward compat — migrating routes incrementally):
;;   No preHandlers vector; with-request-context! is called inside the handler
;;   directly, as before. body still receives ctx.

(defmacro defroute
  [fn-name extra-deps method-name route-string & rest]
  {:clj-kondo/lint-as 'clojure.core/defn
   :clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (let [pre-handler-mode? (and (seq rest) (vector? (first rest)))
        pre-handlers      (when pre-handler-mode? (first rest))
        body              (if pre-handler-mode? (next rest) rest)]
    (if pre-handler-mode?
      ;; ── preHandler mode ──────────────────────────────────────────────
      `(defn ~fn-name [~'app ~'runtime ~'config ~'deps]
         (let [{:keys [~'route!
                       ~'json-response!
                       ~'error-response!
                       ~'ensure-permission!
                       ~'clip-text
                       ~'send-fetch-response!
                       ~'bearer-headers
                       ~'fetch-json
                       ~'request-query-string
                       ~@extra-deps]} ~'deps]
           (~'route! ~'app ~method-name ~route-string
            (cljs.core/js-obj
              "preHandler" (cljs.core/clj->js ~pre-handlers)
              "handler"    (fn [~'request ~'reply]
                             (let [~'ctx (aget ~'request "ctx")]
                               ~@body))))))
      ;; ── Classic mode (with-request-context! inline) ──────────────────
      `(defn ~fn-name [~'app ~'runtime ~'config ~'deps]
         (let [{:keys [~'route!
                       ~'json-response!
                       ~'error-response!
                       ~'with-request-context!
                       ~'ensure-permission!
                       ~'clip-text
                       ~'send-fetch-response!
                       ~'bearer-headers
                       ~'fetch-json
                       ~'request-query-string
                       ~@extra-deps]} ~'deps]
           (~'route! ~'app ~method-name ~route-string
            (fn [~'request ~'reply]
              (~'with-request-context! ~'runtime ~'request ~'reply
               (fn [~'ctx]
                 ~@body)))))))))
;; (defmacro then [target  & body]
;;   `(.then ~target (fn [rseult] ~@body)))
;; (defmacro catch [target  & body]
;;   `(.catch ~target (fn [rseult] ~@body)))
