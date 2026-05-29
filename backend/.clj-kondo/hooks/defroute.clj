(ns hooks.defroute
  (:require [clj-kondo.hooks-api :as api]
            [clojure.string :as str]))

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
;;   (defn fn-name [_app _runtime _config _deps]
;;     (let [<only symbols referenced in body+guards> (fn [& _] nil) ...]
;;       [guards-if-any] body...))
;;
;; Using (fn [& _] nil) as the binding value avoids both
;; "Unresolved symbol" errors (which occur when the right side
;; references a symbol not yet in scope) and "Nil cannot be called"
;; errors (which occur when clj-kondo tracks a nil-typed binding).
;; A variadic fn is callable with any argument count, so all route-body
;; call sites are valid.
;;
;; Only symbols that actually appear in the body are injected, so
;; clj-kondo does not emit spurious unused-binding warnings for the
;; many standard deps that a given route does not reference.
;;
;; request, reply, ctx, and await model the ^:async fn handler context
;; that the macro emits at runtime.  await is included so clj-kondo
;; does not flag it as unresolved inside route bodies.
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

(def ^:private handler-syms
  '[request reply ctx await])

(defn- any-fn-node []
  (api/list-node
   [(api/token-node 'fn)
    (api/vector-node [(api/token-node '&) (api/token-node '_)])
    (api/token-node nil)]))

(defn- atom-node []
  (api/list-node
   [(api/token-node 'atom)
    (api/vector-node [])]))

(defn- binding-value-node [sym]
  (if (str/ends-with? (name sym) "*")
    (atom-node)
    (any-fn-node)))

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
        ;; Only inject let bindings for symbols actually referenced in body+guards.
        needed-std-syms   (filter (fn [s] (contains? body-syms s))
                                  (concat deps-syms handler-syms))
        ;; Extra-deps used as guards or in body are kept; genuinely unused ones are
        ;; filtered so the real unused-dep warning surfaces.
        needed-extra-syms (filter (fn [s] (contains? body-syms s)) extra-syms)
        all-syms          (concat needed-std-syms needed-extra-syms)
        binding-vec       (api/vector-node
                           (mapcat (fn [sym]
                                     [(api/token-node sym) (binding-value-node sym)])
                                   all-syms))
        ;; All four structural params use _ prefix: they're never referenced in body
        ;; text (app/deps are macro-internal; runtime/config are injected via let
        ;; when the body actually uses them).
        new-node          (api/list-node
                           [(api/token-node 'defn)
                            fn-name
                            (api/vector-node
                             (map api/token-node '[_app _runtime _config _deps]))
                            (api/list-node
                             (concat
                              [(api/token-node 'let)
                               binding-vec]
                              effective-body))])]
    {:node new-node}))
