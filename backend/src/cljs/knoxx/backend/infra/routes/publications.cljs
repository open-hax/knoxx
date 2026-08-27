(ns knoxx.backend.infra.routes.publications
  "Publication projection facade.

  Loads resources and hands them to the pure resolver. Everything returned is
  CLJS data — no Fastify request or reply handle enters or leaves this
  namespace, and no external publication backend appears in its contract. The
  owning extern adapter is `knoxx.backend.extern.fastify.publications`."
  (:require [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.law.publication :as law]))

(def ^:private kind-id-key
  {:document :document/id
   :garden :garden/id
   :publication :publication/id})

(defn single-kind-definition
  "Project an expanded record onto only the facet its own `:resource/kind`
   registers.

   A composite manifest entry expands to one record per registered kind, and
   every expanded definition retains ALL the composite keys. Without this
   projection, one entry registering both a document and a publication would be
   indexed twice: the document collapses harmlessly as a byte-equal duplicate,
   but the publication is appended twice and the projection then reports a false
   duplicate-relation conflict — breaking the whole topology.

   A record of any other kind loses all three identity keys and is ignored."
  [record]
  (apply dissoc
         (:resource/definition record)
         (vals (dissoc kind-id-key (:resource/kind record)))))

(def ^:private publication-kinds
  "The kinds this projection is the authority on."
  (set (keys kind-id-key)))

(defn- publication-relevant-rejection?
  "Whether a rejected record can bear on the publication topology.

   The loader walks the entire contracts tree, so a rejected agent, role or
   policy would otherwise make one unrelated invalid contract 409 every
   publication read. Those are somebody else's blocker.

   A file-level failure carries no kind at all — an unreadable or unparseable
   file cannot be attributed to a kind, and therefore cannot be ruled out as a
   publication either. The desired topology is genuinely unknown in that case,
   so it counts."
  [record]
  (let [kind (:resource/kind record)]
    (or (nil? kind)
        (contains? publication-kinds kind))))

(defn invalid-resource-blockers
  "Resources the loader rejected, as explicit blockers.

   A schema-invalid publication — a path missing its leading slash, say — is
   logged and dropped by the loader, so the projection would otherwise return a
   successful topology with that intent simply absent. Silent omission is the
   failure mode this projection exists to prevent, so a rejected record is
   surfaced rather than swallowed."
  [records]
  (->> records
       (remove :ok?)
       (filter publication-relevant-rejection?)
       (mapv (fn [record]
               {:blocker :invalid-resource
                :resource/kind (:resource/kind record)
                :resource/file-path (:resource/file-path record)}))))

(defn ^:async resource-records!
  "Every parsed record, valid or not.

   Deliberately the UNDEDUPED list. `load-all-resources!` applies first-wins
   `[kind id]` dedup, which would collapse two files declaring the same
   canonical id with different payloads into whichever the filesystem
   enumerated first — making the resolver's deterministic identity-conflict
   detection unreachable and the topology dependent on directory order."
  [config]
  (await (resources/load-all-resource-records! config)))

(defn ^:async resource-definitions
  [config]
  (->> (await (resource-records! config))
       (filter :ok?)
       (mapv single-kind-definition)))

(defn publication-index
  "Build the desired-state index from an already loaded record snapshot."
  [records]
  (let [blockers (invalid-resource-blockers records)]
    (when (seq blockers)
      (throw (ex-info "invalid publication resources" {:blockers blockers})))
    (resolver/publication-index (mapv single-kind-definition (filter :ok? records)))))

(defn ^:async publication-index!
  [config]
  (publication-index (await (resource-records! config))))

(defn ^:async list-publication-documents!
  "The whole desired topology: `{:documents [...] :gardens [...]}`."
  [config]
  (resolver/list-document-views (await (publication-index! config))))

(def GardenDeploymentListView
  "Deployment DTO. The public site address is adapter configuration, while the
   nested Garden and publication values remain the pure domain projection."
  [:map {:closed true}
   [:site-url law/NonBlankString]
   [:gardens [:vector law/PublicationGardenView]]])

(defn ^:async list-publication-gardens!
  "Deployed Garden contracts, publication placements, and their public site."
  [config]
  (law/assert-valid!
   :publication/garden-deployment-list
   GardenDeploymentListView
   (assoc (resolver/list-garden-views (await (publication-index! config)))
          :site-url (:publication-site-url config))))

(defn ^:async publication-document-view!
  "One document's desired topology: `{:document ... :publications [...]}`."
  [config document-id]
  (resolver/document-view (await (publication-index! config)) document-id))
