(ns knoxx.backend.infra.routes.publications
  "Publication projection facade.

  Loads resources and hands them to the pure resolver. Everything returned is
  CLJS data — no Fastify request or reply handle enters or leaves this
  namespace, and no external publication backend appears in its contract. The
  owning extern adapter is `knoxx.backend.extern.fastify.publications`."
  (:require [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.resources.loader :as resources]))

(defn ^:async resource-definitions
  "Definitions of every resource that loaded and validated.

   Deliberately the UNDEDUPED record list. `load-all-resources!` applies
   first-wins `[kind id]` dedup, which would collapse two files declaring the
   same canonical id with different payloads into whichever the filesystem
   enumerated first — making the resolver's deterministic identity-conflict
   detection unreachable, and the resulting topology dependent on directory
   order."
  [config]
  (->> (await (resources/load-all-resource-records! config))
       (filter :ok?)
       (mapv :resource/definition)))

(defn ^:async publication-index!
  [config]
  (resolver/publication-index (await (resource-definitions config))))

(defn ^:async list-publication-documents!
  "The whole desired topology: `{:documents [...] :gardens [...]}`."
  [config]
  (resolver/list-document-views (await (publication-index! config))))

(defn ^:async publication-document-view!
  "One document's desired topology: `{:document ... :publications [...]}`."
  [config document-id]
  (resolver/document-view (await (publication-index! config)) document-id))
