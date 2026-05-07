(ns open-hax.contracts.policy.eval
  "Unified policy form evaluator.

   Heritage: proxx.policy.eval
   Evaluates quoted Clojure forms against a context map.
   Used by tree-shaped :policy contracts for conditions, filters, and projections.

   Symbols resolve:
     ctx  → full context map
     it   → current item (when iterating :eval/target)

   Injected functions (via contract/apply) are looked up from a registry."
  (:require [clojure.string :as str]))

;; ── Symbol resolution ─────────────────────────────────────────────────────────

(defn- resolve-symbol
  [sym ctx _injected-fns]
  (case sym
    ctx ctx
    it  (:it ctx)
    nil))

;; ── Core eval ─────────────────────────────────────────────────────────────────

(declare eval-form*)

(defn- eval-args
  [args ctx injected-fns]
  (mapv #(eval-form* % ctx injected-fns) args))

(defn- eval-core-form
  [op args ctx injected-fns]
  (case op
    ;; Equality / comparison
    =    (apply = (eval-args args ctx injected-fns))
    not= (apply not= (eval-args args ctx injected-fns))
    <    (apply < (eval-args args ctx injected-fns))
    >    (apply > (eval-args args ctx injected-fns))
    <=   (apply <= (eval-args args ctx injected-fns))
    >=   (apply >= (eval-args args ctx injected-fns))

    ;; Logic
    not  (not (eval-form* (first args) ctx injected-fns))
    and  (reduce (fn [_ arg]
                   (let [result (eval-form* arg ctx injected-fns)]
                     (if result result (reduced nil))))
                 true
                 args)
    or   (some #(eval-form* % ctx injected-fns) args)

    ;; Collection access
    get    (let [[m k not-found] (eval-args args ctx injected-fns)]
             (get m k not-found))
    get-in (let [[m ks not-found] (eval-args args ctx injected-fns)]
             (get-in m ks not-found))
    first  (first (eval-form* (first args) ctx injected-fns))
    second (second (eval-form* (first args) ctx injected-fns))
    count  (count (eval-form* (first args) ctx injected-fns))

    ;; Type coercion
    keyword (keyword (eval-form* (first args) ctx injected-fns))
    str     (apply str (eval-args args ctx injected-fns))
    name    (name (eval-form* (first args) ctx injected-fns))

    ;; Predicates
    some?  (some? (eval-form* (first args) ctx injected-fns))
    nil?   (nil? (eval-form* (first args) ctx injected-fns))
    empty? (empty? (eval-form* (first args) ctx injected-fns))
    string? (string? (eval-form* (first args) ctx injected-fns))

    ;; String operations
    clojure.string/includes?    (let [[s substr] (eval-args args ctx injected-fns)]
                                  (str/includes? s substr))
    clojure.string/split        (let [[s re] (eval-args args ctx injected-fns)]
                                  (str/split s re))
    clojure.string/starts-with? (let [[s prefix] (eval-args args ctx injected-fns)]
                                  (str/starts-with? s prefix))
    clojure.string/ends-with?   (let [[s suffix] (eval-args args ctx injected-fns)]
                                  (str/ends-with? s suffix))
    clojure.string/lower-case   (let [[s] (eval-args args ctx injected-fns)]
                                  (str/lower-case s))
    clojure.string/trim         (let [[s] (eval-args args ctx injected-fns)]
                                  (str/trim s))

    ;; Unknown op
    ::unknown))

(defn- eval-form*
  [form ctx injected-fns]
  (cond
    (symbol? form)
    (resolve-symbol form ctx injected-fns)

    (seq? form)
    (let [[op & args] form]
      (if (= op 'contract/apply)
        ;; Injected function call: (contract/apply [:fn-key :arg] ctx)
        (let [[fn-key value] (eval-args args ctx injected-fns)]
          (when-let [f (get injected-fns fn-key)]
            (f value)))
        ;; Core form evaluation
        (let [result (eval-core-form op args ctx injected-fns)]
          (when-not (= ::unknown result) result))))

    (map? form)
    ;; Evaluate map values, preserve keys
    (into {} (map (fn [[k v]] [k (eval-form* v ctx injected-fns)]) form))

    (vector? form)
    (mapv #(eval-form* % ctx injected-fns) form)

    :else
    ;; Literals: strings, numbers, keywords, booleans, nil
    form))

;; ── Public API ────────────────────────────────────────────────────────────────

(defn eval-form
  "Evaluate a single quoted form against a context map.

   Options:
     :trace      — atom to record evaluation steps (currently unused)
     :injected   — map of keyword→fn for contract/apply calls

   Returns the evaluation result, or nil on any error (never throws)."
  ([form ctx]
   (eval-form form ctx {}))
  ([form ctx {:keys [_trace injected]}]
   (try
     (let [result (eval-form* form ctx (or injected {}))]
       (when result result))
     (catch :default _ nil))))

(defn eval-forms
  "Evaluate multiple forms under a logical operator.

   op — :all (every truthy), :some (any truthy), :none (none truthy),
        :not (first form is falsy), :assert (same as :all)

   Returns the combined result or nil."
  [op forms ctx injected-fns]
  (case op
    :all   (reduce (fn [_ form]
                     (let [result (eval-form* form ctx injected-fns)]
                       (if (nil? result) (reduced nil) result)))
                   true
                   forms)
    :some  (some #(eval-form* % ctx injected-fns) forms)
    :none  (when-not (some #(eval-form* % ctx injected-fns) forms) true)
    :not   (when-not (eval-form* (first forms) ctx injected-fns) true)
    :assert (eval-forms :all forms ctx injected-fns)
    nil))
