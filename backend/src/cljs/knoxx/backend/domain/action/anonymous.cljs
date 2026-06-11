(ns knoxx.backend.domain.action.anonymous
  "Compile :action/fn forms into callable anonymous action handlers.

   Anonymous actions are inline (fn [ctx action] body) forms carried by
   composite resources. They are not registered in the action registry and not
   discoverable as tools — they are local to their containing resource.

   Function values pass through untouched (code-defined resources). EDN list
   forms are interpreted against a whitelisted pure-function set, the same
   fail-closed posture as trigger condition expressions. The supported subset:
   fn, let (with :keys/:as map and positional vector destructuring), do, if,
   when, and, or, not, quote, keyword access, and whitelisted core functions.
   Async composition belongs in registered actions or :actions/run-steps."
  (:require [clojure.string :as str]))

(def ^:private max-eval-depth 64)

(def ^:private safe-fns
  "Whitelisted pure functions callable from :action/fn bodies."
  {'= = 'not= not= '< < '> > '<= <= '>= >=
   '+ + '- - '* * '/ / 'inc inc 'dec dec 'min min 'max max 'abs abs
   'get get 'get-in get-in 'assoc assoc 'assoc-in assoc-in 'dissoc dissoc
   'update update 'merge merge 'select-keys select-keys 'contains? contains?
   'keys keys 'vals vals 'zipmap zipmap
   'str str 'pr-str pr-str 'subs subs
   'str/join str/join 'str/trim str/trim 'str/lower-case str/lower-case
   'str/upper-case str/upper-case 'str/includes? str/includes?
   'str/starts-with? str/starts-with? 'str/ends-with? str/ends-with?
   'str/replace str/replace 'str/split str/split
   'keyword keyword 'name name 'namespace namespace
   'first first 'second second 'rest rest 'next next 'last last 'nth nth
   'take take 'drop drop 'take-last take-last 'drop-last drop-last
   'count count 'empty? empty? 'seq seq 'vec vec 'vector vector 'list list
   'set set 'into into 'conj conj 'cons cons 'concat concat
   'reverse reverse 'distinct distinct 'flatten flatten
   'map map 'mapv mapv 'filter filter 'filterv filterv 'remove remove
   'keep keep 'reduce reduce 'apply apply 'some some 'every? every?
   'sort sort 'sort-by sort-by 'group-by group-by 'frequencies frequencies
   'interpose interpose 'partition partition 'partition-all partition-all
   'identity identity 'constantly constantly 'comp comp 'partial partial
   'fnil fnil 'juxt juxt
   'true? true? 'false? false? 'nil? nil? 'some? some?
   'string? string? 'number? number? 'boolean? boolean? 'keyword? keyword?
   'map? map? 'vector? vector? 'sequential? sequential? 'coll? coll? 'fn? fn?
   'int? int? 'pos? pos? 'neg? neg? 'zero? zero? 'odd? odd? 'even? even?})

(declare eval-form)

(defn- bind-one
  "Extend env with one binding form bound to value. Supports plain symbols,
   {:keys [...] :as sym} map destructuring, and positional vector destructuring."
  [env binding value]
  (cond
    (symbol? binding)
    (assoc env binding value)

    (map? binding)
    (let [env (if-let [as-sym (:as binding)] (assoc env as-sym value) env)]
      (reduce (fn [acc key-sym]
                (assoc acc (symbol (name key-sym)) (get value (keyword key-sym))))
              env
              (:keys binding [])))

    (vector? binding)
    (reduce (fn [acc [idx inner]]
              (bind-one acc inner (nth value idx nil)))
            env
            (map-indexed vector binding))

    :else
    (throw (ex-info "Unsupported binding form in :action/fn" {:binding binding}))))

(defn- eval-body
  [forms env depth]
  (reduce (fn [_ form] (eval-form form env depth)) nil forms))

(defn- make-fn
  "Build a callable closure from interpreted (fn [params] body) parts."
  [params body env depth]
  (fn [& args]
    (eval-body body
               (reduce (fn [acc [binding value]] (bind-one acc binding value))
                       env
                       (map vector params (concat args (repeat nil))))
               depth)))

(defn- eval-let
  [bindings body env depth]
  (when-not (and (vector? bindings) (even? (count bindings)))
    (throw (ex-info "let in :action/fn requires an even binding vector" {:bindings bindings})))
  (eval-body body
             (reduce (fn [acc [binding value-form]]
                       (bind-one acc binding (eval-form value-form acc depth)))
                     env
                     (partition 2 bindings))
             depth))

(defn- eval-call
  [head tail env depth]
  (let [f (if (symbol? head)
            (or (get env head)
                (get safe-fns head)
                (throw (ex-info "Unknown function in :action/fn" {:sym head})))
            (eval-form head env depth))]
    (if (fn? f)
      (apply f (mapv #(eval-form % env depth) tail))
      (throw (ex-info "Non-callable head in :action/fn" {:head head})))))

(defn- eval-list
  [expr env depth]
  (let [head (first expr)
        tail (rest expr)]
    (cond
      (= 'quote head) (first tail)
      (= 'fn head) (make-fn (first tail) (rest tail) env depth)
      (= 'let head) (eval-let (first tail) (rest tail) env depth)
      (= 'do head) (eval-body tail env depth)
      (= 'if head) (if (eval-form (first tail) env depth)
                     (eval-form (second tail) env depth)
                     (eval-form (nth (vec tail) 2 nil) env depth))
      (= 'when head) (when (eval-form (first tail) env depth)
                       (eval-body (rest tail) env depth))
      (= 'and head) (every? #(eval-form % env depth) tail)
      (= 'or head) (some #(eval-form % env depth) tail)
      (= 'not head) (not (eval-form (first tail) env depth))

      ;; Keyword access: (:key m) and (:key m default)
      (keyword? head)
      (get (eval-form (first tail) env depth) head
           (eval-form (second tail) env depth))

      :else (eval-call head tail env depth))))

(defn- eval-form
  [expr env depth]
  (when (neg? depth)
    (throw (ex-info ":action/fn expression too deeply nested" {:expr expr})))
  (let [depth (dec depth)]
    (cond
      (symbol? expr) (if (contains? env expr)
                       (get env expr)
                       (or (get safe-fns expr)
                           (throw (ex-info "Unbound symbol in :action/fn"
                                           {:sym expr :available (keys env)}))))
      (seq? expr) (if (seq expr) (eval-list expr env depth) expr)
      (vector? expr) (mapv #(eval-form % env depth) expr)
      (map? expr) (into {} (map (fn [[k v]]
                                  [(eval-form k env depth)
                                   (eval-form v env depth)]))
                        expr)
      (set? expr) (into #{} (map #(eval-form % env depth)) expr)
      :else expr)))

(defn compile-action-fn
  "Return a callable (fn [ctx action] ...) for an :action/fn value, or nil.
   Function values pass through; (fn [params] body) list forms are interpreted."
  [form]
  (cond
    (fn? form) form

    (and (seq? form) (= 'fn (first form)) (vector? (second form)))
    (make-fn (second form) (drop 2 form) {} max-eval-depth)

    :else nil))
