(ns knoxx.backend.domain.resources.namespace-file
  "Pure parsing for namespace resource files — the resource manifest grammar.

   A namespace file groups resources under one :namespace:

     {:namespace :ussyverse :resources [{...} ...]}

   The grammar (see docs/design/resource-architecture.md):

   - REGISTRATION — `:K/id` registers a resource of kind K. Identity is the
     namespace plus the local id: :namespace :ussyverse + :trigger/id
     :social-replies -> :ussyverse/social-replies. Every kind can be
     registered this way; no kind has to be.

   - COMPOSITE — one entry may register several kinds at once (trigger +
     action + store ...). Each interpreter reads only the keys in its own
     namespace (`:trigger/*`, `:action/*`, `:store/*`, ...).

   - ANONYMOUS FACETS (ownership) — `:K/<field>` keys without `:K/id` declare
     an anonymous resource of kind K, owned by whatever kinds the entry does
     register. Anonymous facets are never registered and never discoverable;
     they are read in place by kind K's interpreter (`:action/fn` on a trigger
     is the canonical example).

   - REFERENCES — references to other resources live under the OWNING kind's
     namespace (`:trigger/action`, `:role/capabilities`, `:model/family`).
     A bare `:K/id` always means identity, never a reference."
  (:require [clojure.string :as str]))

(def kind-id-keys
  "Ordered [kind id-key] pairs: the id key whose presence registers an entry
   as that kind. Order fixes the expansion order of composite entries."
  [[:trigger :trigger/id]
   [:action :action/id]
   [:store :store/id]
   [:agent :agent/id]
   [:actor :actor/id]
   [:role :role/id]
   [:capability :cap/id]
   [:policy :policy/id]
   [:schedule :schedule/id]
   [:generator :generator/id]
   [:source :source/id]
   [:source-mode :source-mode/id]
   [:ingest-source :ingest-source/id]
   [:model :model/id]
   [:model-family :model-family/id]
   [:runtime-feature :runtime-feature/id]
   [:sub-agent :sub-agent/id]])

(def kind->id-key
  (into {} kind-id-keys))

(def ^:private key-ns->kind
  "Key namespace string -> resource kind (e.g. \"cap\" -> :capability)."
  (into {}
        (map (fn [[kind id-key]] [(namespace id-key) kind]))
        kind-id-keys))

(defn- id-name
  [value]
  (cond
    (keyword? value) (name value)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn namespace-file?
  "True when a parsed EDN map is a namespace resource file."
  [raw]
  (and (map? raw)
       (some? (:namespace raw))
       (sequential? (:resources raw))))

(defn qualified-id
  "Qualified keyword identity for a namespace-local resource id."
  [namespace-value local-id]
  (when-let [local (id-name local-id)]
    (keyword (id-name namespace-value) local)))

(defn qualified-id-str
  "Qualified id as the \"namespace/local-id\" string used for record indexing."
  [namespace-value local-id]
  (some-> (qualified-id namespace-value local-id)
          str
          (subs 1)))

(defn entry-kinds
  "Kinds an entry REGISTERS, by presence of their id keys, in grammar order."
  [entry]
  (->> kind-id-keys
       (keep (fn [[kind id-key]]
               (when (some? (get entry id-key)) kind)))
       vec))

(defn facet-kinds
  "All kinds an entry speaks about: kinds with any key in their namespace."
  [entry]
  (->> (keys entry)
       (keep (fn [k]
               (when (keyword? k)
                 (key-ns->kind (namespace k)))))
       distinct
       vec))

(defn anonymous-facets
  "Facet kinds the entry does NOT register: anonymous resources owned by the
   entry's registered kinds (e.g. :action for a trigger carrying :action/fn)."
  [entry]
  (let [registered (set (entry-kinds entry))]
    (->> (facet-kinds entry)
         (remove registered)
         vec)))

(defn- entry-definition
  "Project one registered kind of a composite entry into a full resource
   definition carrying legacy contract identity keys."
  [namespace-value entry kind]
  (let [local-id (get entry (kind->id-key kind))
        qid (qualified-id namespace-value local-id)
        anonymous (anonymous-facets entry)]
    (cond-> (assoc entry
                   :namespace (keyword (id-name namespace-value))
                   :resource/qualified-id qid
                   :contract/kind kind
                   :contract/id (qualified-id-str namespace-value local-id))
      (seq anonymous) (assoc :resource/anonymous-facets anonymous)
      (and (= kind :trigger)
           (nil? (:trigger/kind entry))) (assoc :trigger/kind :event))))

(defn namespace-file-definitions
  "Expand a namespace file into resource definitions, one per registered kind
   per entry. Returns [{:resource/kind kind :resource/definition map} ...].
   Anonymous facets stay on every projected definition — interpreters read
   their own keys in place."
  [raw]
  (->> (:resources raw)
       (filter map?)
       (mapcat (fn [entry]
                 (map (fn [kind]
                        {:resource/kind kind
                         :resource/definition (entry-definition (:namespace raw) entry kind)})
                      (entry-kinds entry))))
       vec))
