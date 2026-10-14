(ns knoxx.backend.infra.publication-runtime
  "Production composition for contract publication.

   This is the outer runtime adapter the pure reconciler deliberately does not
   own: it loads revision/review evidence once, renders source or translated
   content into an artifact, and selects the static-site target configured by
   deployment. No desired state is written here."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.document-admission :as document-admission]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.domain.publication-content :as publication-content]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.domain.translation-review-inventory :as review-inventory]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.infra.publication-target-static-site :as static-site]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as content-integrity]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.routes.translation-dispatch :as translation]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as split-registry]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as identity]))

(def target-id :open-hax.publication/static-site)

(defn render-fragment
  "Render approved semantic text and media references through the pure contract.
   Styling remains the website/view contract's concern."
  [blocks]
  (publication-content/render-fragment blocks))

(defn- document-root [roots document]
  (get roots (:document/id document)))

(defn- ^:async source-blocks! [roots document]
  (some-> (await (fs/read-file-or-nil!
                  (source-revision/document-path (document-root roots document)
                                                 document)))
          (str/split #"\n\s*\n")))

(defn- authenticated-blocks
  "Split only bytes authenticated by the exact receipt the gate admitted."
  [receipt content]
  (when (content-integrity/authenticated-content? receipt content)
    (str/split content #"\n\s*\n")))

(defn- ^:async translated-blocks!
  "Read receipt-bound agent output, then authored files, then legacy segments.
   Every source must match the admitted content digest; a mismatching authored
   file stops the lookup. Authored receipts predate agent output so it wins."
  [client scope roots document intent receipt content-root]
  (if-some [submitted (await (agent-content/content-for-receipt! content-root receipt))]
    (authenticated-blocks receipt submitted)
    (if-some [authored (await
                        (contract-content/localized-content!
                         (document-root roots document)
                         document
                         (:publication/locale intent)))]
      (if-some [blocks (authenticated-blocks receipt authored)]
        blocks
        ;; A localized file exists but is not the candidate the receipt names.
        ;; Do not fall through and serve a weaker source under that approval.
        nil)
      (let [response (await
                      (openplanner-client/translation-document!
                       client
                       (identity/encode-keyword (:publication/document intent))
                       (name (:publication/locale intent))
                       {:org_id (:org-id scope)
                        :project (:project scope)
                        :garden_id (identity/encode-keyword
                                    (:publication/garden intent))}))]
        (authenticated-blocks receipt
                              (str/join "\n\n"
                                        (map :translated_text
                                             (:segments response))))))))

(defn- rendered-artifact [intent concrete-revision receipt blocks]
  (when (seq blocks)
    (law/assert-valid!
     :publication/runtime-artifact law/PublicationArtifact
     (cond-> {:artifact/content (render-fragment blocks)
              :artifact/media-type "text/html"
              :artifact/encoding "utf-8"
              :artifact/locale (:publication/locale intent)
              :artifact/revision concrete-revision}
       receipt (assoc :artifact/content-revision (:translation/revision receipt))))))

(defn artifact-source
  "The artifact one admitted intent renders to, at one concrete revision.

  `receipt-for` is consulted rather than `translated-revision?` because the
  content lookup needs the *output* revision, and only the receipt carries it.
  The evidence handed in is the same evidence the gate decided with, so the
  bytes rendered here are the bytes the approval that admitted this intent was
  granted for."
  [client scope index roots evidence content-root]
  (^:async fn [intent concrete-revision]
    (let [document (get-in index [:documents (:publication/document intent)])
          source? (= (:document/source-locale intent) (:publication/locale intent))
          receipt (when-not source?
                    (evidence-domain/receipt-for evidence
                                                 (:publication/document intent)
                                                 (:publication/garden intent)
                                                 (:publication/locale intent)
                                                 concrete-revision))
          blocks (if source?
                   (await (source-blocks! roots document))
                   (await (translated-blocks! client scope roots document intent
                                              receipt content-root)))]
      (rendered-artifact intent concrete-revision receipt blocks))))

(defn locale-admissible?
  [index]
  (fn [_declaration intent artifact]
    (let [garden (get-in index [:gardens (:publication/garden intent)])]
      (and (= (:publication/locale intent) (:artifact/locale artifact))
           (contains? (set (:garden/locales garden)) (:artifact/locale artifact))))))

(defn configured?
  [config]
  (boolean (some-> (:publication-content-root config) str not-empty)))

(defn- ^:async publication-context! [config scope evidence-store]
  (let [records (await (publications/resource-records! config))
        index (document-admission/visible-publication-index
               (publications/publication-index records) scope)
        documents (vec (vals (:documents index)))
        roots (translation/document-source-roots config records)
        source-revisions (await (source-revision/source-revisions!
                                 config documents roots))
        authored (await (contract-content/ensure-receipts!
                         evidence-store index roots scope source-revisions))
        desired-work (mapv #(assoc %
                                   :translation/org-id (:org-id scope)
                                   :translation/project (:project scope))
                           (review-inventory/desired-work index source-revisions))]
    {:index index :documents documents :roots roots
     :source-revisions source-revisions :authored authored :desired-work desired-work}))

(defn- ^:async reviewed-evidence! [config scope evidence-store context]
  (let [{:keys [documents roots source-revisions authored desired-work]} context]
    (await (translation/gate-evidence!
            config evidence-store scope documents roots
            {:source-revisions source-revisions
             :current-authored authored
             :desired-work desired-work
             :authenticate-content? true
             ;; Publication joins current split history, including a durable
             ;; rejection whose projection has not yet completed.
             :enforce-split-review-readiness? true
             :split-store (split-registry/current)
             :digest-hex crypto/sha256-hex}))))

(defn- publication-facts [evidence facts]
  (assoc facts :publication-content-revision
         (fn [document garden locale revision]
           (:translation/revision
            (evidence-domain/receipt-for evidence document garden locale revision)))))

(defn- runtime-reconciler [config scope journal context reviewed]
  (let [{:keys [index roots]} context
        {:keys [evidence facts]} reviewed
        root (:publication-content-root config)
        target {:publication-target/id target-id
                :publication-target/kind :publication-target/static-site
                :publication-target/config {:content-root root}
                :publication-target/enabled? true}]
    {:reconciler
     (reconciler/make-reconciler
      {:registry (registry/make-registry [target]
                                         {:publication-target/static-site
                                          static-site/static-site-target})
       :store (static-site/static-site-store root)
       :load-index! (constantly (js/Promise.resolve index))
       :evidence-facts (constantly (publication-facts evidence facts))
       :artifact-source (artifact-source (openplanner-client/client config)
                                         scope index roots evidence root)
       :locale-admissible? (locale-admissible? index)
       :emit-receipt! (:emit! journal)})
     :journal journal}))

(defn ^:async make-runtime!
  "Build a request-scoped reconciler from fresh desired state and evidence.

   The idempotency store is filesystem-backed and therefore survives this
   request-scoped composition. `journal` is supplied by the route and retained
   across requests for receipt review."
  [config scope journal]
  (when-not (configured? config)
    (throw (ex-info "publication content root is not configured"
                    {:status 503 :code "publication_reconciliation_unavailable"})))
  (let [evidence-store (or (evidence-registry/current)
                           (throw (ex-info "translation evidence persistence is not configured"
                                           {:status 503
                                            :code "translation_evidence_unavailable"})))
        context (await (publication-context! config scope evidence-store))
        reviewed (await (reviewed-evidence! config scope evidence-store context))]
    (runtime-reconciler config scope journal context reviewed)))
