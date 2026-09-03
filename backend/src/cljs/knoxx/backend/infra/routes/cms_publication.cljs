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
            [knoxx.backend.domain.document-admission :as document-admission]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-manifest :as manifest]))

(defn ^:async resource-index!
  [config scope]
  (document-admission/visible-publication-index
   (await (publications/publication-index! config)) scope))

(defn evidence
  "Runtime evidence keyed by publication id. Receipts and blockers are supplied
   by the caller so this facade stays free of any receipt store; with none
   available the CMS still renders desired state, with observed simply absent."
  [{:keys [receipts blockers]}]
  {:receipts (cms/receipts->observed (or receipts []))
   :blockers (or blockers {})})

(defn ^:async list-documents!
  ([config scope] (list-documents! config scope {}))
  ([config scope runtime-evidence]
   (cms/list-view->wire (evidence runtime-evidence)
                        (resolver/list-document-views
                         (await (resource-index! config scope))))))

(defn ^:async document-view!
  ([config scope document-id] (document-view! config scope document-id {}))
  ([config scope document-id runtime-evidence]
   (cms/document-view->wire (evidence runtime-evidence)
                            (resolver/document-view (await (resource-index! config scope))
                                                    document-id))))

(defn- assert-unique-target!
  "Refuse unless exactly one entry in the file claims this publication id.

   Zero and many are different failures and carry different data. The many case
   deliberately omits `:publication/id`, because the adapter's `error-status`
   checks that key first and would report 404 for a file that plainly contains
   the resource twice; leaving only `:conflicts` maps it to 409.

   Duplicate canonical publication ids are not rejected upstream today
   (`knoxx-publication-duplicate-identity`), so a file really can hold two
   entries claiming this id — and a request naming one resource must not
   rewrite two. `manifest/assoc-entry-field` refuses as well; this is the layer
   that gives the refusal an HTTP meaning."
  [edn file-path publication-id]
  (let [matches (manifest/matching-entry-count edn :publication/id publication-id)]
    (when (zero? matches)
      (throw (ex-info "publication resource is not present in its own file"
                      {:publication/id publication-id
                       :resource/file-path file-path})))
    (when (> matches 1)
      (throw (ex-info "more than one entry in this file claims that publication id"
                      {:resource/file-path file-path
                       :conflicts [{:publication/id publication-id
                                    :matches matches}]})))))

(defn ^:async write-publication-state!
  "Set `:publication/state` on one entry of the file that declares it.

   Edits the one field in place rather than writing the resource over the file.
   A manifest routinely declares a document, its garden, and its publications
   together; `pr-str`-ing the patched intent over that file DELETED the document
   and garden, and the very next projection failed with unresolved references.
   Publishing must not destroy the thing being published."
  [file-path publication-id next-state]
  ;; Validated before the file is even read. This is a public function reachable
  ;; with any value, and the read/patch/write below is the last boundary before
  ;; the filesystem — persisting `:banana` as a publication state would leave the
  ;; projection failing closed on a file nobody remembers editing.
  ;;
  ;; A state assertion is the whole of what this write needs. `unchanged-except?`
  ;; below proves nothing else about the file moved, so a file that validated
  ;; before the edit still validates after it exactly when the new state is
  ;; lawful. Re-validating the whole manifest here would mean restating the
  ;; loader's canonicalization — entries are written with namespace-local ids —
  ;; and would prove nothing further.
  (law/assert-valid! :publication/state law/PublicationState next-state)
  (let [edn (await (resources/read-edn-file! file-path))]
    (assert-unique-target! edn file-path publication-id)
    (let [next-edn (manifest/assoc-entry-field edn :publication/id publication-id
                                               :publication/state next-state)]
      ;; Check the bytes about to be persisted, not the intent behind them. The
      ;; transform is supposed to touch exactly one field; asserting that against
      ;; the actual result is what keeps a future edit to it from quietly
      ;; widening into the whole-file replacement this function exists to undo.
      (when-not (manifest/unchanged-except? edn next-edn :publication/id
                                            publication-id :publication/state)
        (throw (ex-info "refusing to write: the edit changed more than publication state"
                        {:publication/id publication-id
                         :resource/file-path file-path})))
      (await (resources/write-edn-file! file-path (str (pr-str next-edn) "\n"))))))

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
  [config scope publication-id domain-patch]
  (let [index (await (resource-index! config scope))
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
