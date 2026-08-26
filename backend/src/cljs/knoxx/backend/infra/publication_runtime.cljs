(ns knoxx.backend.infra.publication-runtime
  "Production composition for contract publication.

   This is the outer runtime adapter the pure reconciler deliberately does not
   own: it loads revision/review evidence once, renders source or translated
   content into an artifact, and selects the static-site target configured by
   deployment. No desired state is written here."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.infra.publication-target-static-site :as static-site]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.routes.translation-dispatch :as translation]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as identity]))

(def target-id :open-hax.publication/static-site)

(defn- escape-html [value]
  (str/escape (str value)
              {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&#39;"}))

(defn render-fragment
  "Render semantic text blocks. Styling remains the website/view contract's
   concern; this artifact carries no theme or placement decision."
  [blocks]
  (str "<article class=\"published-document\">"
       (str/join "" (map #(str "<p>" (-> % escape-html
                                          (str/replace "\n" "<br>")) "</p>")
                         (remove str/blank? blocks)))
       "</article>"))

(defn- document-root [roots document]
  (get roots (:document/id document)))

(defn- ^:async source-blocks! [roots document]
  (some-> (await (fs/read-file-or-nil!
                  (source-revision/document-path (document-root roots document)
                                                 document)))
          (str/split #"\n\s*\n")))

(defn- ^:async translated-blocks!
  "The localized blocks for one intent, from the strongest source that has them.

  The order is a precedence, not a fallback chain of equals.

  1. **Agent-submitted content**, keyed by the *output* revision named on the
     receipt the gate itself matched. First because it is the only source whose
     bytes are tied to a specific reviewed revision — see
     `infra.translation-agent-content`. Reading it requires a receipt, so
     `nil` here means no agent has translated this revision, never that one has
     and the bytes were skipped.
  2. **Authored locale files**, the deliberately-kept fallback. Real
     translations somebody wrote by hand, stamped with a 1970 authored-at by
     `infra.publication-contract-content` precisely so agent output supersedes
     them. They keep every declared locale renderable while the agent path fills
     in, which is the whole reason they were not deleted.
  3. **OpenPlanner segments**, the legacy transport this cutover is leaving. Last
     because it is the dependency being removed; still present because retiring
     a transport before its content has moved is how a live site loses pages."
  [client scope roots document intent receipt content-root]
  (if-some [submitted (await (agent-content/content-for-receipt! content-root receipt))]
    (str/split submitted #"\n\s*\n")
    (if-some [authored (await
                        (contract-content/localized-content!
                         (document-root roots document)
                         document
                         (:publication/locale intent)))]
      (str/split authored #"\n\s*\n")
      (let [response (await
                      (openplanner-client/translation-document!
                       client
                       (identity/encode-keyword (:publication/document intent))
                       (name (:publication/locale intent))
                       {:org_id (:org-id scope)}))]
        (mapv :translated_text (:segments response))))))

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
      (when (seq blocks)
        (law/assert-valid!
         :publication/runtime-artifact
         law/PublicationArtifact
         {:artifact/content (render-fragment blocks)
          :artifact/media-type "text/html"
          :artifact/encoding "utf-8"
          :artifact/locale (:publication/locale intent)
          :artifact/revision concrete-revision})))))

(defn locale-admissible?
  [index]
  (fn [_declaration intent artifact]
    (let [garden (get-in index [:gardens (:publication/garden intent)])]
      (and (= (:publication/locale intent) (:artifact/locale artifact))
           (contains? (set (:garden/locales garden)) (:artifact/locale artifact))))))

(defn configured?
  [config]
  (boolean (some-> (:publication-content-root config) str not-empty)))

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
        records (await (publications/resource-records! config))
        index (publications/publication-index records)
        documents (vec (vals (:documents index)))
        roots (translation/document-source-roots config records)
        source-revisions (await (source-revision/source-revisions!
                                 config documents roots))
        _ (await (contract-content/ensure-receipts!
                  evidence-store index roots scope source-revisions))
        {:keys [evidence facts]} (await (translation/gate-evidence!
                                         config evidence-store scope documents roots))
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
       :evidence-facts (constantly facts)
       :artifact-source (artifact-source (openplanner-client/client config)
                                         scope index roots evidence root)
       :locale-admissible? (locale-admissible? index)
       :emit-receipt! (:emit! journal)})
     :journal journal}))
