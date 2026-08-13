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
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.shape.resource-manifest :as manifest]))

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

(defn ^:async write-publication-state!
  "Set `:publication/state` on one entry of the file that declares it.

   Edits the one field in place rather than writing the resource over the file.
   A manifest routinely declares a document, its garden, and its publications
   together; `pr-str`-ing the patched intent over that file DELETED the document
   and garden, and the very next projection failed with unresolved references.
   Publishing must not destroy the thing being published."
  [file-path publication-id next-state]
  (let [edn (await (resources/read-edn-file! file-path))]
    (when-not (manifest/contains-entry? edn :publication/id publication-id)
      (throw (ex-info "publication resource is not present in its own file"
                      {:publication/id publication-id
                       :resource/file-path file-path})))
    (await (resources/write-edn-file!
            file-path
            (str (pr-str (manifest/assoc-entry-field
                          edn :publication/id publication-id
                          :publication/state next-state))
                 "\n")))))

(defn ^:async publication-file-path!
  [config publication-id]
  (let [record (->> (await (resources/load-all-resource-records! config))
                    (filter #(= publication-id
                                (get-in % [:resource/definition :publication/id])))
                    first)]
    (or (:resource/file-path record)
        (throw (ex-info "publication resource has no file on disk"
                        {:publication/id publication-id})))))

(defn ^:async set-publication-state!
  "Apply a decoded domain patch to one publication resource.

   The current resource is read from the graph and only its state is replaced,
   so identity cannot move even if a caller contrived to send identity fields."
  [config publication-id domain-patch]
  (let [index (await (resource-index! config))
        current (->> (:publications index)
                     (filter #(= publication-id (:publication/id %)))
                     first)]
    (when-not current
      (throw (ex-info "unknown publication" {:publication/id publication-id})))
    (let [next-intent (cms/apply-state-patch current domain-patch)
          file-path (await (publication-file-path! config publication-id))]
      (await (write-publication-state! file-path publication-id
                                       (:publication/state next-intent)))
      (resources/invalidate-sync-resource-cache!)
      (cms/publication->wire {:observed nil :blockers []} next-intent))))
