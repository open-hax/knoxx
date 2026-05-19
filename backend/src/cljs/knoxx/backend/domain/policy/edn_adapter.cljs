(ns knoxx.backend.domain.policy.edn-adapter
  "EDN policy adapter. This is the canonical adapter for contract identity.

   It resolves contracts through the shared contract loader by contract identity
   rather than trusting directory placement. Actor maps are validated through
   open-hax.contracts.schema via the shared policy protocol."
  (:require [clojure.string :as str]
            [knoxx.backend.contracts.loader :as contracts-loader]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.tools.registry :as tool-registry]
            ["node:fs" :as fs]
            ["node:fs/promises" :as fs-promises]
            ["node:path" :as path]))

(defrecord EdnPolicyStore [contracts-dir])

(defn create-store
  [contracts-dir]
  (->EdnPolicyStore contracts-dir))

(defn- safe-path-segment!
  [segment kind]
  (let [s (str segment)]
    (when (or (str/blank? s)
              (not (re-matches #"[A-Za-z0-9._-]+" s)))
      (throw (js/Error. (str "Invalid " kind " segment: " segment))))
    s))

(defn- loader-config
  [store]
  {:contracts-dir (:contracts-dir store)})

(defn- normalized-class
  [contract-class]
  (contracts-loader/normalize-contract-class contract-class))

(defn- contract-path
  [store contract-class contract-id]
  (contracts-loader/contract-file-path (loader-config store)
                                       (normalized-class contract-class)
                                       (safe-path-segment! contract-id "contract id")))

(defn- contract-id
  [contract-class contract]
  (case contract-class
    :actors (:actor/id contract)
    :actor (:actor/id contract)
    (or (:contract/id contract)
        (:id contract)
        (:role/id contract)
        (:cap/id contract)
        (:model/id contract)
        (:model-family/id contract))))

(defn actor-contract-file-path
  [store actor-id]
  (contract-path store :actors actor-id))

(defn- normalize-actor-contract
  [actor]
  (cond-> actor
    (and (map? actor) (not (:actor/kind actor)))
    (assoc :actor/kind :agent)))

(defn- edn-file-paths-under-root
  [root]
  (try
    (->> (.readdirSync fs root #js {:withFileTypes true :recursive true})
         array-seq
         (keep (fn [ent]
                 (when (and (.isFile ent)
                            (string? (.-name ent))
                            (str/ends-with? (.-name ent) ".edn")
                            (not (str/starts-with? (.-name ent) ".")))
                   (.join path (.-parentPath ent) (.-name ent))))))
    (catch :default _
      [])))

(defn- load-all-contract-records-sync
  [store]
  (->> (contracts-loader/contract-root-paths (loader-config store))
       (mapcat edn-file-paths-under-root)
       distinct
       (keep (fn [file-path]
               (try
                 (contracts-loader/parse-contract-file!
                  file-path
                  (.readFileSync fs file-path "utf8"))
                 (catch :default _
                   nil))))
       contracts-loader/dedup-contracts))

(defn- validate-contract-for-class!
  [contract-class contract]
  (if (= contract-class :actors)
    (policy/validate-actor! (normalize-actor-contract contract))
    (policy/validate-contract! contract-class contract)))

(defn contract-tool-ids
  [store]
  (->> (load-all-contract-records-sync store)
       (filter #(= "capabilities" (:contractClass %)))
       (mapcat #(or (:cap/tools (:contract %)) []))
       (keep tool-registry/normalize-tool-id)
       set))

(extend-type EdnPolicyStore
  policy/PolicyStore
  (list-contracts [store contract-class]
    (let [klass (normalized-class contract-class)]
      (->> (load-all-contract-records-sync store)
           (filter #(= klass (:contractClass %)))
           (mapv #(validate-contract-for-class! contract-class (:contract %))))))

  (get-contract [store contract-class contract-id]
    (let [klass (normalized-class contract-class)
          wanted-id (some-> contract-id str str/trim not-empty)]
      (some->> (load-all-contract-records-sync store)
               (filter #(and (= klass (:contractClass %))
                             (= wanted-id (:id %))))
               first
               :contract
               (validate-contract-for-class! contract-class))))

  (upsert-contract! [store contract-class contract]
    (let [validated (validate-contract-for-class! contract-class contract)
          id (contract-id contract-class validated)
          file-path (contract-path store contract-class id)]
      (.mkdirSync fs (.dirname path file-path) #js {:recursive true})
      (.writeFileSync fs file-path (str (pr-str validated) "\n") "utf8")
      (js/Promise.resolve {:contract validated :path file-path})))

  (list-actors [store]
    (policy/list-contracts store :actors))

  (get-actor [store actor-id]
    (policy/get-contract store :actors actor-id))

  (upsert-actor! [store actor]
    (policy/upsert-contract! store :actors actor)))
