(ns knoxx.backend.macros)

;; defroute — unified route definition macro.
;;
;; Signature:
;;   (defroute fn-name [extra-dep-syms] method path [& guard-fns] & body)
;;   (defroute fn-name [extra-dep-syms] method path & body)   ; classic
;;
;; Expands to:
;;   (defn fn-name [app runtime config deps] ...)
;;
;; preHandler mode:
;;   [guard-fns] is a vector of symbols bound from deps, each a
;;   standard Fastify hook fn: (fn [req reply done] ...).
;;   They run in declaration order; the composed preHandler stamps
;;   request.ctx so the handler body can read it via `ctx`.
;;
;; Classic mode (backward compat):
;;   No guard vector; with-request-context! is called inline.

(defmacro defroute
  [fn-name extra-deps method-name route-string & rest]
  {:clj-kondo/lint-as 'clojure.core/defn
   :clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (let [pre-handler-mode? (and (seq rest) (vector? (first rest)))
        pre-handlers      (when pre-handler-mode? (first rest))
        body              (if pre-handler-mode? (next rest) rest)]
    `(defn ~fn-name [~'app ~'runtime ~'config ~'deps]
       (let [{:keys [~'route!
                     ~'json-response!
                     ~'error-response!
                     ~'ensure-permission!
                     ~'clip-text
                     ~'with-request-context!
                     ~'send-fetch-response!
                     ~'bearer-headers
                     ~'fetch-json
                     ~'request-query-string
                     ~'session-guard
                     ~'optional-session-guard
                     ~@extra-deps]} ~'deps]
         ~(if pre-handler-mode?
            `(~'route! ~'app ~method-name ~route-string
              (cljs.core/js-obj
               "preHandler" (fn [~'request ~'reply ~'done]
                               (let [~'guards [~@pre-handlers]
                                     ~'chain  (reduce
                                               (fn [~'next ~'guard]
                                                 (fn [] (~'guard ~'request ~'reply ~'next)))
                                               ~'done
                                               (cljs.core/reverse ~'guards))]
                                 (~'chain)))
               "handler"    (^:async fn  [~'request ~'reply]
                               (let [~'ctx (aget ~'request "ctx")]
                                 ~@body))))
            `(~'route! ~'app ~method-name ~route-string
              ( fn  [~'request ~'reply]
                (~'with-request-context! ~'runtime ~'request ~'reply
                 (^:async fn  [~'ctx]
                   ~@body)))))))))

(defmacro then [target & body]
  `(.then ~target (fn [rseult] ~@body)))
(defmacro catch [target & body]
  `(.catch ~target (fn [rseult] ~@body)))

(defmacro with-redis-nil
  "Evaluate body forms with redis/redis-client* set to nil.
   Restores the original value after body completes (even on throw).
   Use in tests to prevent any Redis I/O."
  [& body]
  `(let [orig# @knoxx.backend.redis-client/redis-client*]
     (reset! knoxx.backend.redis-client/redis-client* nil)
     (try
       ~@body
       (finally
         (reset! knoxx.backend.redis-client/redis-client* orig#)))))
