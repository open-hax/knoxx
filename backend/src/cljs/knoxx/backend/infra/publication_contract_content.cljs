(ns knoxx.backend.infra.publication-contract-content
  "Localized content declared by Document contracts — the authored fallback.

   A Document owns content sources. Publication intents own placement and review
   policy; view contracts own presentation. This adapter turns an authored
   localized source into the same immutable translation evidence a producing run
   yields.

   ## It is a fallback, deliberately kept

   The primary producer is now an agent actor: `contracts/namespaces/publication.edn`
   runs `contracts/agents/publication_translator.edn` for a claimed dispatch, and
   its submissions become evidence through `infra.translation-agent-sink`. This
   namespace predates that path and was written when no producer existed at all.

   It was kept rather than deleted for one reason: it keeps every locale a
   garden declares renderable while the agent path fills in, which is what lets
   a reviewer see the localized site at all. `authored-at` is fixed at the epoch
   precisely so that agent output always supersedes an authored file for the same
   relation — see `law.translation-evidence/supersedes?`. The precedence is
   restated at the point of use in `infra.publication-runtime/translated-blocks!`.

   Nothing here should grow. An authored locale file is a human writing a
   translation by hand; if that becomes the normal way translations are produced,
   the honest change is to say so in the Document contract, not to widen this."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [promesa.core :as p]))

(def authored-at
  "Authored content has no discovery-time authority. A fixed early instant keeps
   worker-produced evidence newer when both exist for the same relation."
  "1970-01-01T00:00:00.000Z")

(defn localized-source
  [document locale]
  (get-in document [:document/translations locale]))

(defn content-path
  [root source]
  (let [path (:path source)]
    (cond
      (nil? path) nil
      (str/starts-with? path "/") path
      (nil? root) path
      :else (fs/join root path))))

(defn ^:async localized-content!
  [root document locale]
  (when-let [source (localized-source document locale)]
    (await (fs/read-file-or-nil! (content-path root source)))))

(defn ^:async source-content!
  [root document]
  (await (fs/read-file-or-nil!
          (source-revision/document-path root document))))

(defn- receipt
  [scope intent source-revision content]
  (let [output-revision (source-revision/content-revision content)]
    (evidence-law/assert-receipt!
     (cond->
      {:receipt/type :translation/completed
       :translation/document (:publication/document intent)
       :translation/garden (:publication/garden intent)
       :translation/source-locale (:document/source-locale intent)
       :translation/locale (:publication/locale intent)
       :translation/source-revision source-revision
       :translation/revision output-revision
       :translation/content-digest output-revision
       :translation/dispatch-key
       (str "authored-content:"
            (:org-id scope) ":" (or (:project scope) "_") ":"
            (:publication/document intent) ":"
            (:publication/garden intent) ":"
            (:publication/locale intent) ":"
            source-revision ":" output-revision)
       :translation/org-id (:org-id scope)
       :translation/at authored-at}
      (some? (:project scope))
      (assoc :translation/project (:project scope))))))

(defn ^:async receipts!
  "Translation evidence for every readable authored locale in the index."
  [index roots scope source-revisions]
  (let [pending
        (->> (:publications index)
             (keep (fn [intent]
                     (let [document (get-in index [:documents (:publication/document intent)])
                           locale (:publication/locale intent)
                           source-revision (get source-revisions (:document/id document))]
                       (when (and document
                                  source-revision
                                  (not= locale (:document/source-locale document))
                                  (localized-source document locale))
                         (p/let [content (localized-content!
                                          (get roots (:document/id document))
                                          document locale)]
                           (when (some? content)
                             (receipt scope
                                      (assoc intent
                                             :document/source-locale
                                             (:document/source-locale document))
                                      source-revision
                                      content)))))))
             vec)]
    (->> (await (p/all pending))
         (remove nil?)
         vec)))

(defn- same-output?
  [left right]
  (= (select-keys left [:translation/document
                        :translation/garden
                        :translation/source-locale
                        :translation/locale
                        :translation/source-revision
                        :translation/revision
                        :translation/org-id
                        :translation/project])
     (select-keys right [:translation/document
                         :translation/garden
                         :translation/source-locale
                         :translation/locale
                         :translation/source-revision
                         :translation/revision
                         :translation/org-id
                         :translation/project])))

(defn- authored-receipt?
  "Whether a receipt came from this authored-content adapter."
  [receipt]
  (str/starts-with? (:translation/dispatch-key receipt) "authored-content:"))

(defn authored-relation
  "The exact authored work relation, excluding the output that may change."
  [receipt]
  ((juxt :translation/org-id
         :translation/project
         :translation/document
         :translation/garden
         :translation/source-locale
         :translation/locale
         :translation/source-revision)
   receipt))

(defn current-authored-receipts
  "Replace stale authored history with the files observed in this snapshot.

   Agent/worker receipts remain untouched and can still supersede authored
   fallback by timestamp. Without this normalization, every authored receipt
   shares the epoch timestamp and the lexicographic tie-break can select an old
   digest after the localized file changes.

   `desired-work` is the resource-owned outer relation. Its authored rows are
   retired even when the locale file disappeared or its declaration was
   removed, because absence from `current-authored` is meaningful in precisely
   those cases. The two-argument form remains for callers that only need to
   replace files they actually observed."
  ([receipts current-authored]
   (current-authored-receipts receipts current-authored current-authored))
  ([receipts current-authored desired-work]
   (let [retired-relations (into (set (map authored-relation current-authored))
                                 (map authored-relation)
                                 desired-work)]
     (into (vec (remove #(and (authored-receipt? %)
                              (contains? retired-relations
                                         (authored-relation %)))
                        receipts))
           current-authored))))

(defn ^:async ensure-receipts!
  "Discover authored translations into the durable evidence ledger.

   Exact output facts are idempotent. A changed source or localized file has a
   different digest and therefore appends new evidence instead of rewriting the
   old receipt."
  [evidence-store index roots scope source-revisions]
  (let [authored (await (receipts! index roots scope source-revisions))
        existing (await (store/completed-translations!
                         evidence-store
                         (select-keys scope [:org-id :project])))]
    (doseq [candidate authored
            :when (not-any? #(same-output? candidate %) existing)]
      (await (store/record-translation! evidence-store candidate)))
    authored))
