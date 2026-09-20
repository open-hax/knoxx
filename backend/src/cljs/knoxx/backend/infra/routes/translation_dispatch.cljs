(ns knoxx.backend.infra.routes.translation-dispatch
  "Translation dispatch facade.

  Loads desired state, reads the facts the gate needs, and hands both to
  `infra.translation-dispatch`. Everything in and out is CLJS data — no Fastify
  handle enters or leaves this namespace. The owning extern adapter is
  `knoxx.backend.extern.fastify.translation-dispatch`.

  The order of the four loads is the point. Resources, then source revisions,
  then translation evidence, and only then the gate — each read once, up front,
  because `domain.publication-gate` computes evidence once so a revision
  selector cannot resolve differently between the decision to translate and the
  work actually queued. A facade that let the gate's predicates go read a file
  or a collection per call would put that drift straight back."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.document-admission :as document-admission]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-review-inventory :as review-inventory]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-gate-evidence :as gate-evidence]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-agent-dispatch :as agent-dispatch]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.law.translation-dispatch :as law]))

(defn hydrated-intents
  "Hydrated publication intents, for one document or for every document.

   Walks documents rather than the raw `:publications` list on purpose.
   `hydrate-publication-intent` throws on a dangling document reference, so
   hydrating the raw list would let one broken intent abort an entire sweep. An
   intent whose document does not resolve is instead simply absent: its source
   locale is unknowable, so there is no lawful translation to derive from it,
   and the resolver already reports it as an unresolved reference elsewhere."
  [index document-id]
  (if (some? document-id)
    (resolver/desired-publications index document-id)
    (into []
          (mapcat #(resolver/desired-publications index %))
          (keys (:documents index)))))

(defn- normalized-selection
  "Validate reusable facade input before absence can mean corpus-wide work."
  [selection]
  (cond
    (map? selection)
    (let [selector-keys (set (keys selection))
          allowed-keys #{:document :publication}]
      (when-not (and (every? allowed-keys selector-keys)
                     (<= (count selector-keys) 1)
                     (every? qualified-keyword? (vals selection)))
        (throw (ex-info "invalid translation dispatch selection"
                        {:selection selection})))
      selection)

    (qualified-keyword? selection) {:document selection}
    (nil? selection) {}
    :else (throw (ex-info "invalid translation dispatch selection"
                          {:selection selection}))))

(defn- select-publication
  "Narrow `intents` to one exact publication, refusing an absent id."
  [intents publication]
  (let [selected (filterv #(= publication (:publication/id %)) intents)]
    (when (and publication (empty? selected))
      (throw (ex-info "translation publication selection was not found"
                      {:status 404
                       :code "translation_publication_not_found"
                       :publication publication})))
    selected))

(defn selected-hydrated-intents
  "Hydrated intents selected by exact publication, document, or whole corpus.

   A qualified keyword remains the legacy document form. Map input is closed
   here as well as at HTTP, so a misspelling can never become a corpus sweep.
   Only explicit public or exact-owner documents enter the view: generated
   drafts share one filesystem index across organizations, and manual dispatch
   must not bypass document admission's tenant boundary."
  [index scope selection]
  (let [{:keys [document publication]} (normalized-selection selection)
        visible-index (document-admission/visible-publication-index index scope)
        visible-documents (:documents visible-index)
        _ (when (and document (not (contains? visible-documents document)))
            (throw (ex-info "translation document selection was not found"
                            {:status 404
                             :code "translation_document_not_found"
                             :document document})))
        intents (hydrated-intents visible-index document)]
    (if publication (select-publication intents publication) intents)))

(defn admissible-intents
  "The intents that may derive translation work.

   `domain.publication-gate` states outright that it decides only the
   *evidential* half of admissibility and assumes the structural half holds
   upstream — and until now nothing upstream of dispatch checked it. So an intent
   targeting an archived garden, or a locale its garden does not accept, reached
   the gate, derived translation work, and was enqueued on a shared worker for
   content that can never be published. `translation-work-eligible?` cannot catch
   it: it asks only whether the intent publishes and needs translating, which
   both remain true.

   `law.publication/translatable-publication?` owns this question. It admits
   review-bound drafts without making them reconcilable, and refuses withheld,
   archived, dangling, inactive, or unsupported-locale relations. Filtering
   here rather than inside the gate keeps structural and evidential decisions
   at their respective boundaries."
  [index intents]
  (filterv #(publication-law/translatable-publication? index %) intents))

(defn- referenced-documents
  "The document records the given intents point at.

   The lookup keys off the id directly, because `distinct` is already fed the
   *ids* — the `map` extracted them. Extracting again inside `keep`, as this did
   when it shipped, asked a keyword for its `:publication/document`, got nil for
   every entry, and returned an empty vector every single time.

   Nothing reported it. An empty document list makes
   `source-revision/source-revisions!` produce an empty map, so
   `:current-source-revision` answers nil for every document, so every intent
   short-circuits on `:publication-revision-unresolved` and derives no
   translation work — which `dispatch-translations!` reports as
   `{:considered 5 :admissible 5 :dispatched []}`. That reads exactly like
   \"nothing needed translating\", and it is why four localized intents sat
   blocked with no surface saying why.

   Introduced in `knoxx-translation-work-dispatch` (#253) and load-bearing for
   both runners: the worker path was equally dead."
  [index intents]
  (into []
        (keep #(get-in index [:documents %]))
        (distinct (map :publication/document intents))))

(defn document-source-roots
  "Checkout root per canonical document id, derived from resource provenance."
  [config records]
  (into {}
        (keep (fn [record]
                (when (and (:ok? record) (= :document (:resource/kind record)))
                  (let [document (-> record
                                     publications/single-kind-definition
                                     resolver/canonicalize-document)
                        root (source-revision/resource-source-root
                              config (:resource/file-path record))]
                    (when root [(:document/id document) root])))))
        records))

(defn current-source-locale-receipts
  "Keep receipts translated from the document's currently declared source locale."
  [documents receipts]
  (gate-evidence/current-source-locale-receipts documents receipts))

(defn project-receipts
  "Keep only receipts from the exact active project, including explicit absence."
  [project receipts]
  (gate-evidence/project-receipts project receipts))

(defn tenant-receipts
  "Keep only receipts belonging to the exact active organization."
  [org-id receipts]
  (gate-evidence/tenant-receipts org-id receipts))

(defn gate-facts!
  "Read every gate fact once, scoped to one tenant and project."
  ([config evidence-store scope documents]
   (gate-evidence/gate-facts! config evidence-store scope documents))
  ([config evidence-store scope documents document-roots]
   (gate-evidence/gate-facts! config evidence-store scope documents document-roots)))

(defn gate-evidence!
  "Authenticate receipts, approvals and source acceptance from one snapshot."
  ([config evidence-store scope documents document-roots]
   (gate-evidence/gate-evidence! config evidence-store scope documents document-roots))
  ([config evidence-store scope documents document-roots options]
   (gate-evidence/gate-evidence! config evidence-store scope documents document-roots options)))

(def runner-kinds
  "The producers a deployment may ask translations from.

   `:agent` runs `contracts/agents/publication_translator.edn` through the
   event/trigger runtime — no external service, and the bytes travel with the
   request. `:worker` posts a batch to the OpenPlanner ingestion worker, which is
   the original path and still the right one where that worker is deployed and
   owns the document."
  #{:agent :worker})

(def default-runner
  "The producer used when a deployment names none.

   `:agent`, because a deployment that has not been told otherwise does not have
   the ingestion worker: `knowledge-ops-translation-mt-pipeline` runs it out of
   `ingestion/`, and the production compose stack for this site does not include
   it. Defaulting to `:worker` there meant every dispatch posted a batch nothing
   would ever pick up, and the four localized intents stayed blocked with no
   visible reason."
  :agent)

(defn runner
  "Which producer this deployment asks for translations from.

   An unrecognized value falls back to the default rather than throwing, and says
   nothing about it here — the config layer is where a bad env var should be
   reported, and a reconcile request failing on it would take the whole
   publication path down for a typo."
  [config]
  (let [named (some-> (:translation-runner config) str str/trim str/lower-case
                      not-empty keyword)]
    (if (contains? runner-kinds named) named default-runner)))

(defn- ^:async document-source!
  "The bytes of one intent's source document, or nil when unreadable."
  [index roots intent]
  (when-let [document (get-in index [:documents (:publication/document intent)])]
    (await (contract-content/source-content!
            (get roots (:document/id document)) document))))

(defn- ^:async dispatch-agent-intent!
  "Send one derived work item with its source bytes and candidate recovery context."
  [deps index intent facts scope roots work]
  (let [digest ((:current-source-revision facts) (:publication/document intent))
        source (await (document-source! index roots intent))
        checked-work (:action/with work)
        context (await (dispatch/candidate-recovery-context!
                        (:evidence-store deps) checked-work
                        (dispatch/dispatch-context intent scope digest)))]
    (assoc (if (str/blank? (str source))
             {:dispatch/outcome :dispatch/failed
              :dispatch/detail (str "the document's source could not be read, so"
                                    " there are no bytes to translate")}
             (await (agent-dispatch/dispatch-work! deps checked-work context source)))
           :publication/id (:publication/id intent))))

(defn ^:async dispatch-intents-to-agent!
  "Sequentially announce derived work; report unreadable source instead of skipping it."
  [deps index intents facts scope roots]
  (let [results (atom [])]
    (doseq [intent intents]
      (when-let [work (dispatch/derived-work intent facts)]
        (swap! results conj (await (dispatch-agent-intent!
                                   deps index intent facts scope roots work)))))
    @results))

(defn- ^:async dispatch-selection!
  "Load one resource index and resolve the scoped structurally admissible selection."
  [config deps scope selection]
  (let [load-records! (or (:resource-records! deps) publications/resource-records!)
        build-index (or (:publication-index deps) publications/publication-index)
        records (await (load-records! config))
        index (build-index records)
        hydrated (selected-hydrated-intents index scope selection)]
    {:index index :hydrated hydrated :intents (admissible-intents index hydrated)
     :roots (document-source-roots config records)}))

(defn- selected-desired-work
  "Attach exact tenant/project scope to work for the selected publication IDs."
  [index revisions intents scope]
  (let [selected-publications (set (map :publication/id intents))]
    (->> (review-inventory/desired-work index revisions)
         (filterv #(contains? selected-publications (:publication/id %)))
         (mapv #(assoc % :translation/org-id (:org-id scope)
                         :translation/project (:project scope))))))

(defn- ^:async dispatch-facts!
  "Read source revisions once before authenticating authored receipts and gate evidence."
  [config {:keys [evidence-store] :as deps} scope {:keys [index intents roots]}]
  (let [load-revisions! (or (:source-revisions! deps) source-revision/source-revisions!)
        ensure-receipts! (or (:ensure-contract-receipts! deps) contract-content/ensure-receipts!)
        documents (referenced-documents index intents)
        revisions (await (load-revisions! config documents roots))
        desired-work (selected-desired-work index revisions intents scope)
        authored (await (ensure-receipts! evidence-store index roots scope revisions))]
    (:facts (await (gate-evidence! config evidence-store scope documents roots
                                  {:source-revisions revisions :current-authored authored
                                   :desired-work desired-work :authenticate-content? true})))))

(defn ^:async dispatch-translations!
  "Dispatch one publication, document or corpus; report considered and admissible counts."
  [config deps scope selection]
  (let [{:keys [index hydrated intents roots] :as selected}
        (await (dispatch-selection! config deps scope selection))
        facts (await (dispatch-facts! config deps scope selected))
        selected-runner (runner config)
        dispatch-agent! (or (:dispatch-agent-intents! deps) dispatch-intents-to-agent!)
        dispatch-worker! (or (:dispatch-worker-intents! deps) dispatch/dispatch-intents!)]
    {:considered (count hydrated)
     :admissible (count intents)
     :runner selected-runner
     :dispatched (if (= :agent selected-runner)
                   (await (dispatch-agent! deps index intents facts scope roots))
                   (await (dispatch-worker! deps intents facts scope)))}))

(def worker-report-vocabulary
  "What the ingestion worker actually sends, read from
  `ingestion/src/kms_ingestion/translation/worker.clj` rather than assumed —
  because an earlier version of `resolve-batch-status!` assumed, and was wrong in
  a way that made the whole completion path dead code.

    per-document success   status \"processing\", :completed_document <id>
    all documents done     status \"complete\",   no document named
    all documents failed   status \"failed\",     :error <message>, no document
    some failed            status \"partial\",    no document named

  Two consequences. Success arrives as `\"processing\"`, so gating on
  `complete`/`partial` rejected every real success report. And
  `:failed_document` is never sent: the worker accumulates failures in a local
  atom, and its terminal report names no document at all.

  Kept as a var so the observed contract is citable and one edit updates it."
  {:per-document-success {:status "processing" :names-document? true}
   :all-done {:status "complete" :names-document? false}
   :all-failed {:status "failed" :names-document? false}
   :some-failed {:status "partial" :names-document? false}})

(defn- ^:async observe-source-revision!
  "The dispatched document's current source revision, or nil if unreadable."
  [config record]
  (let [records (await (publications/resource-records! config))
        index (publications/publication-index records)
        document (get-in index [:documents (:dispatch/document record)])]
    (when document
      (get (await (source-revision/source-revisions!
                   config [document] (document-source-roots config records)))
           (:dispatch/document record)))))

(defn source-revision-observer!
  "A function that re-reads a dispatched document's current source revision.

   Supplied to `resolve-batch-report!` so it can verify the source has not moved
   between dispatch and completion — see
   `law.translation-dispatch/source-drift-refusal` for why that verification is
   the strongest thing available when the worker is handed an id rather than
   bytes.

   The resource index is loaded per call rather than closed over. A completion
   report arrives long after the dispatch that caused it, and the whole question
   being asked is what the source looks like *now*; a cached index would answer
   with what it looked like when the process last loaded one."
  [config]
  (partial observe-source-revision! config))

(defn- ^:async resolve-bound-batch!
  "A completed document wins; batch failures without a document resolve the batch binding."
  [deps checked batch-id]
  (let [failed-document (law/failed-document-id (:failed_document checked))]
    (cond
      (some? (:completed_document checked))
      (await (dispatch/resolve-batch-report! deps checked))

      (some? failed-document)
      (await (dispatch/fail-batch-document! deps batch-id failed-document
                                            (or (:error checked) "worker reported failure")))

      (= "failed" (:status checked))
      (await (dispatch/fail-batch! deps batch-id
                                  (or (:error checked) "worker reported batch failure")))

      :else
      {:translation/skipped {:reason :no-document-named :status (:status checked)}})))

(defn ^:async resolve-batch-status!
  "Validate a worker report and require its batch identity before any store lookup.

   Per-document success arrives as processing; complete and partial may name no
   document. Report that absence explicitly rather than silently dropping it."
  [deps report]
  (let [checked (law/assert-valid! :translation-dispatch/status-report
                                   law/BatchStatusReport report)
        batch-id (law/report-batch-id checked)]
    (if (nil? batch-id)
      {:translation/refusal {:refusal/type :batch-id-missing
                             :refusal/actual (:batch_id checked)}}
      (await (resolve-bound-batch! deps checked batch-id)))))
