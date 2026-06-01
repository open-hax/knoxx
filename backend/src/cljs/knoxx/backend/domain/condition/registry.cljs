(ns knoxx.backend.domain.condition.registry
  "Registry for trigger condition functions.
   Conditions are pure predicates: (condition-fn event actor trigger config) -> boolean.
   Condition expressions in trigger contracts are evaluated safely against this registry."
  (:require [clojure.string :as str]))

(defonce ^:private registry* (atom {}))

(defn register-condition!
  "Register a condition function under a namespaced keyword id.
   Signature: (fn [event actor trigger config] -> boolean)"
  [id f]
  (swap! registry* assoc id f))

(defn condition-fn
  "Look up a condition function by id. Returns nil if not found."
  [id]
  (get @registry* id))

(defn condition-ids
  "Return all registered condition ids."
  []
  (keys @registry*))

;; ─── Safe expression evaluator ────────────────────────────────────────

(def ^:private safe-fns
  "Map of whitelisted symbols to their function values for condition evaluation."
  {'not not
   '= =
   'not= not=
   '< <
   '> >
   '<= <=
   '>= >=
   'get get
   'get-in get-in
   'str str
   'str/lower-case str/lower-case
   'str/includes? str/includes?
   'str/starts-with? str/starts-with?
   'str/ends-with? str/ends-with?
   'some some
   'every? every?
   'filter filter
   'remove remove
   'map map
   'count count
   'empty? empty?
   'seq seq
   'first first
   'rest rest
   'contains? contains?
   'keyword keyword
   'name name
   'keyword? keyword?
   'string? string?
   'number? number?
   'boolean? boolean?
   'true? true?
   'false? false?
   'nil? nil?
   'some? some?})

(defn- safe-fn?
  "Return true if the symbol is a known safe function for conditions."
  [sym]
  (contains? safe-fns sym))

(declare safe-eval)

(defn- eval-list
  "Evaluate a list expression safely."
  [expr bindings]
  (when (seq expr)
    (let [head (first expr)
          tail (rest expr)]
      (cond
        ;; Boolean combinators
        (= 'and head) (every? #(safe-eval % bindings) tail)
        (= 'or head) (some #(safe-eval % bindings) tail)
        (= 'not head) (not (safe-eval (first tail) bindings))

        ;; Comparison
        (= '= head) (apply = (map #(safe-eval % bindings) tail))
        (= 'not= head) (apply not= (map #(safe-eval % bindings) tail))
        (= '< head) (apply < (map #(safe-eval % bindings) tail))
        (= '> head) (apply > (map #(safe-eval % bindings) tail))
        (= '<= head) (apply <= (map #(safe-eval % bindings) tail))
        (= '>= head) (apply >= (map #(safe-eval % bindings) tail))

        ;; Condition registry call: (:conditions/foo event ...)
        (keyword? head)
        (if-let [f (condition-fn head)]
          (apply f (map #(safe-eval % bindings) tail))
          (throw (ex-info "Unknown condition keyword" {:ref head})))

        ;; Condition registry call: (conditions/foo event ...) — symbol form
        (symbol? head)
        (if-let [f (or (condition-fn (keyword head))
                       (get safe-fns head))]
          (apply f (map #(safe-eval % bindings) tail))
          (throw (ex-info "Unknown function in condition expression"
                          {:sym head :allowed (safe-fn? head)})))

        :else
        (throw (ex-info "Invalid condition expression head"
                        {:head head :expr expr}))))))

(defn safe-eval
  "Safely evaluate a condition expression with bindings.
   Bindings map symbols to values (e.g. {'event evt 'actor act}).
   Only condition registry fns and whitelisted core fns are callable."
  ([expr bindings]
   (safe-eval expr bindings 8))
  ([expr bindings max-depth]
   (when (neg? max-depth)
     (throw (ex-info "Condition expression too deeply nested" {:expr expr})))
   (cond
     (symbol? expr) (or (get bindings expr)
                        (throw (ex-info "Unbound symbol in condition"
                                        {:sym expr :available (keys bindings)})))
     (seq? expr) (eval-list expr bindings)
     (vector? expr) (vec (map #(safe-eval % bindings (dec max-depth)) expr))
     (map? expr) (into {} (map (fn [[k v]]
                                 [(safe-eval k bindings (dec max-depth))
                                  (safe-eval v bindings (dec max-depth))])
                               expr))
     (set? expr) (set (map #(safe-eval % bindings (dec max-depth)) expr))
     :else expr)))

(defn evaluate
  "Evaluate a trigger condition expression against an event, actor, trigger, and config.
   Returns boolean. Any exception becomes false (condition fails closed)."
  [expr event actor trigger config]
  (if (nil? expr)
    true ;; No condition means always match
    (try
      (boolean (safe-eval expr {'event event
                                'actor actor
                                'trigger trigger
                                'config config}))
      (catch :default e
        (js/console.warn "[condition] failed to evaluate" expr ":" (.-message e))
        false))))
