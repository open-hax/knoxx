(ns knoxx.backend.domain.contracts.sources
  "Runtime source contract resolution.

   A runtime source is not an ingestion source. Ingestion sources decide what is
   indexed. Runtime source resources either hydrate turn context or select
   driver events with :source/listens. Source refs compose from actor -> role ->
   agent -> run overrides and dedupe by :source/id."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as loader]))

(defn deep-merge
  "Recursively merge maps; later maps win for scalar/vector values."
  [& maps]
  (letfn [(merge-entry [a b]
            (if (and (map? a) (map? b))
              (merge-with merge-entry a b)
              b))]
    (reduce merge-entry {} (filter map? maps))))

(defn- nonblank-str
  [value]
  (some-> value str str/trim not-empty))

(defn- source-slug
  [value]
  (some-> value
          nonblank-str
          (str/replace #"^:" "")
          (str/replace #"_" "-")
          str/trim
          not-empty))

(defn- namespaced-source-id
  [namespace-part name-part]
  (when-let [source-name (source-slug name-part)]
    (keyword namespace-part source-name)))

(defn normalize-source-id
  "Normalize source refs while preserving an explicit identity namespace.

   Unqualified values remain backward compatible as :source/<slug>. Qualified
   values such as :github/app-events remain qualified so runtime provenance does
   not collapse unrelated providers onto :source/app-events."
  [value]
  (cond
    (keyword? value)
    (if-let [namespace-part (namespace value)]
      (namespaced-source-id namespace-part (name value))
      (namespaced-source-id "source" (name value)))

    (string? value)
    (when-let [source-id (some-> value nonblank-str (str/replace #"^:" ""))]
      (if (str/includes? source-id "/")
        (let [[namespace-part name-part] (str/split source-id #"/" 2)]
          (when (and (not (str/blank? namespace-part))
                     (not (str/blank? name-part)))
            (namespaced-source-id namespace-part name-part)))
        (namespaced-source-id "source" source-id)))

    (nil? value) nil
    :else (normalize-source-id (str value))))

(defn source-contract-id
  "Map a canonical source id to its contract id slug."
  [source-id]
  (some-> (normalize-source-id source-id) name))

(defn source-ref-id
  "Extract the canonical :source/id from a source ref.

   Accepts:
   - :source/openplanner-memory
   - :github/app-events
   - source/openplanner-memory or openplanner-memory
   - {:source/ref :source/openplanner-memory ...}
   - {:source/id :github/app-events ...}"
  [source-ref]
  (cond
    (map? source-ref)
    (normalize-source-id (or (:source/id source-ref)
                             (:source/ref source-ref)
                             (:source source-ref)
                             (:ref source-ref)
                             (:id source-ref)))

    :else
    (normalize-source-id source-ref)))

(defn- source-ref-map
  [source-ref]
  (cond
    (nil? source-ref) {}
    (map? source-ref) source-ref
    :else {:source/ref (normalize-source-id source-ref)}))

(defn- alias-overrides
  [m]
  (let [filters (deep-merge (:source/filters m)
                            (:filters m))
        hydration (deep-merge (:source/hydration m)
                              (:hydration m)
                              (:hydrate m)
                              (:source/config m)
                              (:config m))
        render (deep-merge (:source/render m)
                           (:render m))]
    (cond-> m
      (seq filters) (assoc :source/filters filters)
      (seq hydration) (assoc :source/hydration hydration)
      (seq render) (assoc :source/render render))))

(defn- enabled?
  [source]
  (and (not (false? (:enabled source)))
       (not (false? (:source/enabled? source)))))

(defn- contract-source-spec
  [contract]
  (when (map? contract)
    (let [source-id (normalize-source-id (or (:source/id contract)
                                             (:contract/id contract)))]
      (cond-> (select-keys contract
                           [:contract/id
                            :contract/type
                            :enabled
                            :source/name
                            :source/type
                            :source/enabled?
                            :source/driver
                            :source/actor
                            :source/listens
                            :source/protocol
                            :source/provider
                            :source/hydration
                            :source/render
                            :source/filters
                            :source/tools])
        source-id (assoc :source/id source-id
                         :source/ref source-id)))))

(defn- source-contract-by-source-id
  [config source-id]
  (let [wanted (normalize-source-id source-id)]
    (some (fn [record]
            (let [contract (:contract record)]
              (when (and (= "sources" (:contractClass record))
                         (= wanted (normalize-source-id (:source/id contract))))
                contract)))
          (loader/load-all-contracts-sync config))))

(defn source-contract
  "Resolve a runtime source contract by source id or source ref."
  [config source-ref]
  (when-let [source-id (source-ref-id source-ref)]
    (or (loader/contract-sync config "sources" (source-contract-id source-id))
        (source-contract-by-source-id config source-id))))

(defn- source-ref-overrides
  [source-ref]
  (when-let [source-id (source-ref-id source-ref)]
    (-> (source-ref-map source-ref)
        alias-overrides
        (dissoc :id :ref :source :filters :hydration :hydrate :render :config :source/config)
        (assoc :source/id source-id
               :source/ref source-id))))

(defn resolve-source-spec
  "Resolve one source ref into a normalized runtime source spec.

   Missing source contracts are tolerated so callers can pass run-local refs
   before the contract exists. A disabled contract is authoritative: a later
   actor, agent, or run ref cannot silently re-enable it."
  [config source-ref]
  (when-let [source-id (source-ref-id source-ref)]
    (let [contract (source-contract config source-id)
          base (or (contract-source-spec contract)
                   {:source/id source-id
                    :source/ref source-id})
          overrides (source-ref-overrides source-ref)
          merged (deep-merge base overrides)]
      (when (and (enabled? contract)
                 (enabled? overrides))
        merged))))

(defn- source-group->refs
  [group]
  (cond
    (nil? group) []
    (map? group) [group]
    (or (vector? group) (list? group) (set? group) (seq? group)) (seq group)
    :else [group]))

(defn compose-source-refs
  "Compose source refs in deterministic precedence order.

   Pass groups in low -> high precedence order (actor, roles, agent, run). Later
   refs for the same :source/id deep-merge over earlier refs while preserving the
   first position in the source vector."
  [config & groups]
  (let [refs (mapcat source-group->refs groups)
        indexed (reduce (fn [{:keys [order by-id] :as acc} source-ref]
                          (if-let [source-id (source-ref-id source-ref)]
                            (let [already? (contains? by-id source-id)
                                  spec (if already?
                                         (source-ref-overrides source-ref)
                                         (resolve-source-spec config source-ref))]
                              (if spec
                                (assoc acc
                                       :order (if already? order (conj order source-id))
                                       :by-id (update by-id source-id #(if % (deep-merge % spec) spec)))
                                acc))
                            acc))
                        {:order [] :by-id {}}
                        refs)]
    (mapv #(get-in indexed [:by-id %]) (:order indexed))))

(defn agent-source-refs
  [agent-spec]
  (or (:sources agent-spec)
      (:runtime-sources agent-spec)
      (:runtimeSources agent-spec)))

(defn source-specs-for-agent
  [config agent-spec]
  (compose-source-refs config (agent-source-refs agent-spec)))

(defn find-source
  [source-specs source-id]
  (let [wanted (normalize-source-id source-id)]
    (some (fn [source]
            (when (= wanted (source-ref-id source))
              source))
          (source-group->refs source-specs))))

(defn source-hydration-options
  [source]
  (deep-merge (:source/hydration source)
              (:hydration source)
              (:hydrate source)
              (:source/config source)
              (:config source)))
