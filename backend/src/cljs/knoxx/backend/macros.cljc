(ns knoxx.backend.macros)

;; Two modes of defroute, dispatched on whether the arg after route-string
;; is a vector (guards) or starts the body directly (classic).
;;
;; Classic mode:
;;   (defroute fn-name [extra-dep-syms] method path & body)
;;   Expands to (defn fn-name [app runtime config deps] ...)
;;   body receives ctx from with-request-context!
;;
;; Guard-pipeline mode:
;;   (defroute fn-name [] method path [:guard ...] & body)
;;   Expands to (defn fn-name [app runtime config deps] ...)
;;   deps must contain :run! (a (make-route-runner deps) value);
;;   body receives ctx assembled by apply-guards.
;;   Called as: (fn-name app runtime config (assoc deps :run! run!))

(defmacro defroute
  [fn-name extra-deps method-name route-string & rest]
  {:clj-kondo/lint-as 'clojure.core/defn
   :clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (let [guards-mode? (and (seq rest) (vector? (first rest)))
        guards       (when guards-mode? (first rest))
        body         (if guards-mode? (next rest) rest)]
    (if guards-mode?
      `(defn ~fn-name [~'app ~'runtime ~'config ~'deps]
         (let [{:keys [~'route! ~'run!]} ~'deps]
           (~'route! ~'app ~method-name ~route-string
            (fn [~'req ~'reply]
              (-> (~'run! ~guards ~'req ~'reply)
                  (.then (fn [~'ctx]
                           (when ~'ctx
                             ~@body))))))))
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

(defmacro defn-async
  "Compatibility macro for async workflow fns.

   Today it expands to a plain defn so callers can return either nil or a
   Promise. Keeping the macro lets us evolve toward a lowered js-await DSL
   without rewriting call sites."
  [name args & body]
  `(defn ~name ~args ~@body))
