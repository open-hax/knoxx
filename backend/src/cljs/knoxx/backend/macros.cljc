;; backend/src/clj/knoxx/backend/macros.clj
(ns knoxx.backend.macros)
{:spec/name      "knoxx/backend/http-async-routes"
 :spec/version   "0.1.0"
 :spec/purpose   "Flatten async nesting in Knoxx MCP handlers to workflow orchestration"
 :spec/requires  ["shadow-cljs 2.28+" "malli 1.0+"]
 :spec/validate? true}

(defmacro defroute
  "Declare a Fastify route with a named workflow fn.
   guards: nil | :redis | [:redis :browser-auth] | [:redis :bearer-token] | [:ctx tool-id]
   workflow-sym: a defn-async fn of [deps req reply]"
  [app method path guards workflow-sym]
  (let [has-redis  (and guards (or (= guards :redis) (contains? (set guards) :redis)))
        auth-kind  (when (vector? guards)
                     (some #{:browser-auth :bearer-token :ctx} guards))]
    `(route! ~app ~method ~path
             (fn [req# reply#]
               ~(cond-> `(~workflow-sym req# reply#)
                  has-redis  (as-> form `(when-let [r# (require-redis! reply#)] ~form))
                  auth-kind  (as-> form `(when-let [ctx# (~(symbol "resolve-auth!") reply# req#)] ~form)))))))
(defmacro defroute-fn
  [fname {:keys [method path]} & body]
  (let [m (symbol (str "." method))]
    `(defn ~fname [app runtime config]
       (defroute app ~m ~path
         ~@body))))


(defmacro GET [path bindings & body]
  `(register-route! app :get ~path
     (fn ~bindings ~@body)))

(defmacro POST [path bindings & body]
  `(register-route! app :post ~path
     (fn ~bindings ~@body)))

(defmacro DELETE [path bindings & body]
  `(register-route! app :delete ~path
     (fn ~bindings ~@body)))
(defn await-form? [x]
  (and (seq? x) (= 'await (first x))))

(defn lower-awaits [body]
  (letfn [(step [forms]
            (when (seq forms)
              (let [f (first forms)
                    more (next forms)]
                (cond
                  (and (seq? f) (= 'let (first f)))
                  (let [[_ bindings & let-body] f
                        pairs (partition 2 bindings)
                        body* (reduce
                               (fn [acc [sym expr]]
                                 (if (await-form? expr)
                                   `(shadow.cljs.modern/js-await [~sym ~(second expr)]
                                      ~acc)
                                   `(let [~sym ~expr] ~acc)))
                               `(do ~@let-body)
                               (reverse pairs))]
                    (if more `(do ~body* ~(step more)) body*))

                  (await-form? f)
                  `(shadow.cljs.modern/js-await ~(second f)
                     ~(if more `(do ~(step more)) nil))

                  more
                  `(do ~f ~(step more))

                  :else
                  f))))]
    (step body))

(defmacro defn-async [name args & body]
  `(defn ~name ~args
     ~(lower-awaits body)))
