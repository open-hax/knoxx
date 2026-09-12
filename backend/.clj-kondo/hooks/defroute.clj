(ns hooks.defroute
  (:require [clj-kondo.hooks-api :as api]))

;; Teaches clj-kondo the shape of:
;;
;;   Classic mode:
;;     (defroute fn-name [extra-dep1 extra-dep2]
;;       "METHOD" "/path"
;;       body...)
;;
;;   Pre-handler mode:
;;     (defroute fn-name [extra-dep1 extra-dep2]
;;       "METHOD" "/path"
;;       [guard-fn1 guard-fn2]
;;       body...)
;;
;; The hook generates:
;;
;;   (defn fn-name [_app _runtime _config deps]
;;     (let [<only symbols referenced in body+guards> (get deps :symbol) ...]
;;       (^:async fn [ctx]            ; or [_ctx] when ctx is unused
;;         [guards-if-any] body...)))
;;
;; The body is wrapped in an ^:async fn that mirrors the handler the
;; runtime macro emits.  Modeling the async fn boundary means await and
;; raw Promise chains inside route bodies are analyzed in the same
;; structural context they actually run in.
;;
;; An unknown dependency lookup keeps actual types unknown. Inventing a
;; variadic function here falsely makes request/reply and scalar TTL values
;; callable, producing type errors in valid route bodies. The hook preserves
;; checks on real literals/local values rather than disabling type analysis.
;;
;; Only symbols that actually appear in the body are injected, so
;; clj-kondo does not emit spurious unused-binding warnings for the
;; many standard deps that a given route does not reference.
;;
;; request, reply, and await are modeled by unknown values in the same let
;; pool so clj-kondo does not flag them as unresolved inside route bodies.
;; ctx is modeled as the wrapping ^:async fn parameter (the macro binds
;; it from (aget request "ctx")), not via the let pool.
;;
;; In pre-handler mode children[5] is a vector of guard fn symbols;
;; it is detected and skipped so it is not treated as a body form.

;; Structural params (app, deps) are never in body text — generated as _-prefixed.
;; runtime and config are sometimes in body text; they go in the filtered let pool.
(def ^:private deps-syms
  '[runtime config
    route! json-response! error-response! ensure-permission!
    with-request-context! clip-text send-fetch-response!
    bearer-headers fetch-json request-query-string
    session-guard optional-session-guard])

;; request, reply, and await live in the let pool (with unknown types).
;; ctx is modeled as the parameter of the ^:async handler fn that wraps the
;; body — see new-node below — because the runtime macro binds it from
;; (aget request "ctx") inside the emitted handler, not from deps.
(def ^:private handler-syms
  '[request reply await])

(defn- async-handler-node
  "Wrap the route body forms in the ^:async handler fn that the runtime macro
   emits.  When the body references ctx, it becomes the fn parameter (the macro
   binds it from (aget request \"ctx\")); otherwise an ignored _ctx parameter is
   used so clj-kondo does not flag an unused binding."
  [ctx-used? body-forms]
  (let [param (if ctx-used? 'ctx '_ctx)]
    (api/list-node
     (concat
      [(api/token-node (with-meta 'fn {:async true}))
       (api/vector-node [(api/token-node param)])]
      body-forms))))

(defn- binding-value-node [sym]
  (api/list-node
   [(api/token-node 'get)
    (api/token-node 'deps)
    (api/token-node (keyword sym))]))

(defn- collect-body-syms
  "Walk body nodes recursively, returning a set of all symbol sexprs found."
  [nodes]
  (reduce (fn [acc node]
            (if (api/token-node? node)
              (let [v (try (api/sexpr node) (catch Exception _ nil))]
                (if (symbol? v) (conj acc v) acc))
              (into acc (collect-body-syms (or (:children node) [])))))
          #{}
          nodes))

(defn defroute [{:keys [node]}]
  (let [children          (:children node)
        fn-name           (nth children 1 nil)
        extra-vec         (nth children 2 nil)
        ;; children: defroute fn-name extra-deps method path [guards?] body...
        ;; Pre-handler mode: children[5] is a vector of guard fn symbols.
        ;; Classic mode:     children[5] is the first body form.
        maybe-guards      (nth children 5 nil)
        pre-handler-mode? (and maybe-guards (api/vector-node? maybe-guards))
        body              (if pre-handler-mode?
                            (drop 6 children)  ; skip guard vector
                            (drop 5 children))
        extra-syms        (when (api/vector-node? extra-vec)
                            (map api/sexpr (:children extra-vec)))
        ;; In pre-handler mode, include the guard vector in the generated body so:
        ;; (a) guard symbols are visible as references when filtering, and
        ;; (b) clj-kondo sees them as referenced in the let body (no unused-binding).
        effective-body    (if pre-handler-mode?
                            (cons maybe-guards body)
                            body)
        body-syms         (collect-body-syms effective-body)
        ctx-used?         (contains? body-syms 'ctx)
        ;; Only inject let bindings for symbols actually referenced in body+guards.
        needed-std-syms   (filter (fn [s] (contains? body-syms s))
                                  (concat deps-syms handler-syms))
        ;; Extra-deps used as guards or in body are kept; genuinely unused ones are
        ;; filtered so the real unused-dep warning surfaces.
        needed-extra-syms (filter (fn [s] (contains? body-syms s)) extra-syms)
        all-syms          (concat needed-std-syms needed-extra-syms)
        deps-param        (if (seq all-syms) 'deps '_deps)
        binding-vec       (api/vector-node
                           (mapcat (fn [sym]
                                     [(api/token-node sym) (binding-value-node sym)])
                                   all-syms))
        ;; Use a real name for the dependency parameter only when the generated
        ;; lookups read it. This preserves unused/used-underscored diagnostics.
        new-node          (api/list-node
                           [(api/token-node 'defn)
                            fn-name
                            (api/vector-node
                             (map api/token-node ['_app '_runtime '_config deps-param]))
                            (api/list-node
                             [(api/token-node 'let)
                              binding-vec
                              ;; Model the actual ^:async handler context the
                              ;; runtime macro emits, so await and Promise
                              ;; chains in the body are analyzed inside an
                              ;; async function rather than at let-body scope.
                              (async-handler-node ctx-used? effective-body)])])]
    {:node new-node}))
