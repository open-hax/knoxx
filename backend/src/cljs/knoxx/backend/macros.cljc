(ns knoxx.backend.macros)

(defmacro defroute
  [fn-name extra-deps method-name route-string & body]
  {:clj-kondo/lint-as 'clojure.core/defn  ;; Treat as defn for arity/locals
   :clj-kondo/ignore [:unresolved-symbol :unused-binding]}
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
             ~@body)))))))

(defmacro defn-async
  "Compatibility macro for async workflow fns.

   Today it expands to a plain defn so callers can return either nil or a
   Promise. Keeping the macro lets us evolve toward a lowered js-await DSL
   without rewriting call sites."
  [name args & body]
  `(defn ~name ~args ~@body))
