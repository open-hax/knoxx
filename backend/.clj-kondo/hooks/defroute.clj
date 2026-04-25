(ns hooks.defroute
  (:require [clj-kondo.hooks-api :as api]))

;; Teaches clj-kondo the shape of:
;;
;;   (defroute fn-name [extra-dep1 extra-dep2]
;;     "METHOD" "/path"
;;     body...)
;;
;; Macro expands to (defn fn-name [app runtime config deps] ...)
;; and destructures deps into: route! json-response! error-response!
;; ensure-permission! with-request-context! clip-text
;; send-fetch-response! bearer-headers fetch-json request-query-string
;; + any extra-deps declared in the vector.
;;
;; In preHandler mode the body receives ctx bound from request.
;; In classic mode with-request-context! wraps the body and provides ctx.
;; Either way, request reply ctx config runtime are all in scope.

(def ^:private injected-bindings
  '[route! json-response! error-response! ensure-permission!
    with-request-context! clip-text send-fetch-response!
    bearer-headers fetch-json request-query-string
    request reply ctx config runtime])

(defn defroute [{:keys [node]}]
  (let [children  (:children node)
        ;; (defroute fn-name [extra-deps] "METHOD" "/path" & body)
        fn-name   (nth children 1 nil)
        extra-vec (nth children 2 nil)
        body      (drop 5 children)   ; skip defroute fn-name extra-deps method route
        extra-syms (when (api/vector-node? extra-vec)
                     (:children extra-vec))
        all-bindings (into injected-bindings (map api/sexpr extra-syms))
        ;; Build: (defn fn-name [app runtime config deps]
        ;;          (let [route! route! json-response! json-response! ... <body>]))
        binding-vec  (api/vector-node
                      (mapcat (fn [sym]
                                [(api/token-node sym)
                                 (api/token-node sym)])
                              all-bindings))
        new-node (api/list-node
                  [(api/token-node 'defn)
                   fn-name
                   (api/vector-node
                    (map api/token-node '[app runtime config deps]))
                   (api/list-node
                    (concat
                     [(api/token-node 'let)
                      binding-vec]
                     body))])]
    {:node new-node}))
