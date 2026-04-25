(ns knoxx.backend.macros)

;; Two arities of defroute:
;;
;; Classic mode (extra-deps is a vector of symbols, no guards):
;;   (defroute fn-name [extra-dep-syms] method path & body)
;;   → (defn fn-name [app runtime config deps] ...)
;;     body receives ctx via with-request-context!
;;
;; Guard mode (extra-deps is a vector of keywords — mcp guard pipeline):
;;   (defroute fn-name [] method path [:guard1 :guard2] & body)
;;   → (defn fn-name [app runtime config deps] ...)
;;     body receives ctx assembled by apply-guards via make-route-runner
;;
;; Detection: if the element after route-string is a vector, it's guards.
;; Otherwise body starts immediately (classic mode, guards = nil).

(defmacro defroute
  [fn-name extra-deps method-name route-string & rest]
  {:clj-kondo/lint-as 'clojure.core/defn
   :clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (let [guards-mode? (and (seq rest) (vector? (first rest)))
        guards       (when guards-mode? (first rest))
        body         (if guards-mode? (rest rest) rest)]
    (if guards-mode?
      ;; Guard-pipeline mode: deps must supply make-route-runner and apply-guards
      `(defn ~fn-name [~'app ~'runtime ~'config ~'deps]
         (let [~'run! (~'knoxx.backend.mcp-http/make-route-runner ~'deps)]
           (~'knoxx.backend.app-shapes/route! ~'app ~method-name ~route-string
            (fn [~'req ~'reply]
              (-> (~'apply-guards (assoc ~'deps :req ~'req :reply ~'reply) ~guards)
                  (.then (fn [~'ctx]
                           (when ~'ctx
                             ~@body))))))))
      ;; Classic mode: body receives ctx via with-request-context!
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
