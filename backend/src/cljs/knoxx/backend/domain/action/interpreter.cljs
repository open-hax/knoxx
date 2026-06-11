(ns knoxx.backend.domain.action.interpreter
  "Action interpreter: executes the :action/* facet of a resource.

   Resolution order for an action map:
   1. :action/fn — inline anonymous action, executed directly (never registered)
   2. registered action kinds (registry handlers and run-action! defmethods)
   3. EDN action resources matched by :action/id — expanded into their
      :action/kind + :action/with and re-executed

   Before execution the action's :action/scope declaration (or the registered
   scope metadata for its kind) is resolved into a flat map of bound action
   fns, filter fns, and store instances, injected into ctx as :scope."
  (:require [knoxx.backend.domain.action.anonymous :as anonymous]
            [knoxx.backend.domain.action.registry :as registry]
            [knoxx.backend.domain.filter.registry :as filter-registry]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.store.registry :as store-registry]))

(defn resolve-scope-decl
  "Resolve an :action/scope declaration {:actions [...] :filters [...]
   :stores [...]} into a flat scope map keyed by the declared ids."
  [config scope-decl]
  (merge
   (into {}
         (map (fn [action-key]
                [action-key (fn [ctx action] (registry/run-action! ctx action))]))
         (or (:actions scope-decl) []))
   (into {}
         (keep (fn [filter-id]
                 (when-let [f (filter-registry/filter-fn filter-id)]
                   [filter-id f])))
         (or (:filters scope-decl) []))
   (into {}
         (keep (fn [store-id]
                 (when-let [store (store-registry/get-store! config store-id)]
                   [store-id store])))
         (or (:stores scope-decl) []))))

(defn- with-scope
  [ctx action]
  (let [scope-decl (or (:action/scope action)
                       (registry/get-scope-declaration (:action/kind action)))]
    (assoc ctx :scope (resolve-scope-decl (:config ctx) scope-decl))))

(defn- known-kind?
  [kind]
  (boolean (or (registry/get-action kind)
               (contains? (dissoc (methods registry/run-action!) :default) kind))))

(defn- action-resource
  "Find an enabled action resource definition whose id matches an action kind."
  [config kind]
  (when (and config (keyword? kind))
    (->> (resources/load-all-resources-sync config)
         (filter #(= :action (:resource/kind %)))
         (map :resource/definition)
         (remove #(false? (:enabled %)))
         (some (fn [definition]
                 (when (or (= kind (:action/id definition))
                           (= kind (:resource/qualified-id definition)))
                   definition))))))

(defn- expand-action-resource
  [action definition]
  (cond-> (assoc action
                 :action/kind (:action/kind definition)
                 :action/with (merge (:action/with definition)
                                     (:action/with action)))
    (:action/scope definition) (assoc :action/scope (:action/scope definition))
    (:action/fn definition) (assoc :action/fn (:action/fn definition))))

(defn execute!
  "Execute the action facet of a resource with scope injected into ctx.
   Returns a Promise of the action result."
  ([ctx action]
   (execute! ctx action 1))
  ([ctx action redirects]
   (if-let [inline (some-> (:action/fn action) anonymous/compile-action-fn)]
     (js/Promise.resolve (inline (with-scope ctx action) action))
     (let [kind (:action/kind action)]
       (cond
         (known-kind? kind)
         (registry/run-action! (with-scope ctx action) action)

         (pos? redirects)
         (if-let [definition (action-resource (:config ctx) kind)]
           (execute! ctx (expand-action-resource action definition) (dec redirects))
           (registry/run-action! ctx action))

         :else
         (registry/run-action! ctx action))))))
