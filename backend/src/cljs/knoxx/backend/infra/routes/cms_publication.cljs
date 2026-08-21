(ns knoxx.backend.infra.routes.cms-publication
  "CMS publication facade: the resource-backed replacement for the legacy
  garden-metadata surface.

  Returns and accepts CLJS data only. Fastify handles stay in
  `knoxx.backend.extern.fastify.cms-publication`.

  A state edit writes ONLY `:publication/state`. Publication identity —
  document, garden, locale, revision — is immutable through this surface; a
  re-key is a separate, conflict-checked operation and never a side effect of
  publishing."
  (:require [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.resources.namespace-file :as ns-file]
            [knoxx.backend.infra.routes.publications :as publications]))

(defn ^:async resource-index!
  [config]
  (await (publications/publication-index! config)))

(defn evidence
  "Runtime evidence keyed by publication id. Receipts and blockers are supplied
   by the caller so this facade stays free of any receipt store; with none
   available the CMS still renders desired state, with observed simply absent."
  [{:keys [receipts blockers]}]
  {:receipts (cms/receipts->observed (or receipts []))
   :blockers (or blockers {})})

(defn ^:async list-documents!
  ([config] (list-documents! config {}))
  ([config runtime-evidence]
   (cms/list-view->wire (evidence runtime-evidence)
                        (resolver/list-document-views (await (resource-index! config))))))

(defn ^:async document-view!
  ([config document-id] (document-view! config document-id {}))
  ([config document-id runtime-evidence]
   (cms/document-view->wire (evidence runtime-evidence)
                            (resolver/document-view (await (resource-index! config))
                                                    document-id))))

(defn- declares-publication?
  "Whether an authored map declares `publication-id`, comparing through the same
   canonical rule the resolver indexes by — an authored entry may carry a
   namespace-local id."
  [namespace-value publication-id authored]
  (boolean
   (and (:publication/id authored)
        (= publication-id
           (resolver/canonical-id namespace-value (:publication/id authored))))))

(defn authored-with-state
  "The authored file structure with ONLY the target publication's state changed.

   The whole point is that the file is edited in the shape a human wrote it. The
   previous version serialized the *projected* intent over the entire file, which
   destroyed everything else the file contained: a namespace manifest lost its
   `:namespace`/`:resources` wrapper, every sibling resource in it, and any
   document or garden facet sharing the entry — so one state toggle in the CMS
   could delete resources the next topology load then reported as missing.

   It was lossy even for a standalone file: the projection runs `select-keys`
   over the declared publication keys, so `:namespace` and anything else the
   author wrote were dropped on the first edit."
  [authored publication-id next-state]
  ;; Both conditions deliberately. A standalone resource file may also carry a
  ;; `:namespace`, so the presence of `:resources` is what actually distinguishes
  ;; a manifest to be walked from a single authored resource to be updated.
  (if (and (ns-file/namespace-file? authored)
           (sequential? (:resources authored)))
    (let [namespace-value (:namespace authored)
          entries (vec (:resources authored))
          matches? (partial declares-publication? namespace-value publication-id)]
      (when-not (some matches? entries)
        (throw (ex-info "owning manifest does not declare this publication"
                        {:publication/id publication-id})))
      (assoc authored :resources
             (mapv (fn [entry]
                     (cond-> entry
                       (matches? entry) (assoc :publication/state next-state)))
                   entries)))
    (do
      (when-not (declares-publication? (:namespace authored) publication-id authored)
        (throw (ex-info "owning file does not declare this publication"
                        {:publication/id publication-id})))
      (assoc authored :publication/state next-state))))

(defn ^:async set-publication-state!
  "Apply a decoded domain patch to one publication resource.

   The current resource is read from the graph and only its state is replaced,
   so identity cannot move even if a caller contrived to send identity fields.
   The *file* is then edited in its authored shape, so the edit cannot disturb
   anything the author wrote alongside the publication."
  [config publication-id domain-patch]
  (let [index (await (resource-index! config))
        current (->> (:publications index)
                     (filter #(= publication-id (:publication/id %)))
                     first)]
    (when-not current
      (throw (ex-info "unknown publication" {:publication/id publication-id})))
    (let [next-intent (cms/apply-state-patch current domain-patch)
          record (->> (await (resources/load-all-resource-records! config))
                      (filter #(= publication-id
                                  (get-in % [:resource/definition :publication/id])))
                      first)
          file-path (:resource/file-path record)]
      (when-not file-path
        (throw (ex-info "publication resource has no file on disk"
                        {:publication/id publication-id})))
      (let [authored (await (resources/read-edn-file! file-path))
            updated (authored-with-state authored
                                         publication-id
                                         (:publication/state next-intent))]
        (await (resources/write-edn-file! file-path (str (pr-str updated) "\n"))))
      (resources/invalidate-sync-resource-cache!)
      (cms/publication->wire {:observed nil :blockers []} next-intent))))
