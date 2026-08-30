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
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.translation-review-inventory :as review-inventory]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-agent-dispatch :as agent-dispatch]
            [knoxx.backend.infra.translation-candidate-content :as candidate-content]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.infra.translation-split-projection :as split-projection]
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
   here as well as at HTTP, so a misspelling can never become a corpus sweep."
  [index selection]
  (let [{:keys [document publication]} (normalized-selection selection)
        intents (hydrated-intents index document)]
    (if publication (select-publication intents publication) intents)))

(defn admissible-intents
  "The intents that could actually reconcile to a public materialization.

   `domain.publication-gate` states outright that it decides only the
   *evidential* half of admissibility and assumes the structural half holds
   upstream — and until now nothing upstream of dispatch checked it. So an intent
   targeting an archived garden, or a locale its garden does not accept, reached
   the gate, derived translation work, and was enqueued on a shared worker for
   content that can never be published. `translation-work-eligible?` cannot catch
   it: it asks only whether the intent publishes and needs translating, which
   both remain true.

   `law.publication/admissible-publication?` is the contract that owns this
   question, so it is called rather than re-derived. Filtering here rather than
   inside the gate keeps the two halves where their own docstrings put them."
  [index intents]
  (filterv #(publication-law/admissible-publication? index %) intents))

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
  "Only the receipts whose source locale is still the document's declared one.

   The gate's evidential key is `[document target-locale revision]` — it cannot
   carry a source locale, because a publication intent does not name one. But a
   translation *from* a different source locale is a different translation, and
   the dispatch identity says so by including `:source-locale`.

   Without this filter, changing a document's declared source locale while its
   bytes stay identical left the content digest, the document and the target
   locale all unchanged — so the old receipt satisfied the new intent and the
   retranslation that was genuinely required never happened.

   Filtered here rather than keyed in the domain because the gate's fact
   signature is fixed and the *current* source locale is a property of the
   document, which this layer has and the domain does not."
  [documents receipts]
  (let [declared (into {} (map (juxt :document/id :document/source-locale)) documents)]
    (filterv (fn [receipt]
               (= (:translation/source-locale receipt)
                  (get declared (:translation/document receipt))))
             receipts)))

(defn project-receipts
  "Only the receipts belonging to `project`.

   Translation output is project-scoped — every existing segment, document and
   export route filters by it — so output produced under one project does not
   exist under another. With the project ignored, changing
   `KNOXX_SESSION_PROJECT_NAME` left the durable evidence in place and the new
   project read the old project's receipts as its own.

   A nil active project matches only receipts that also name none, so an unset
   project is its own scope rather than a wildcard over every other."
  [project receipts]
  (filterv #(= project (:translation/project %)) receipts))

(defn tenant-receipts
  "Only the receipts belonging to `org-id`.

   Translation is tenant-scoped: the worker keys segments by organization and
   every document read requires one, so a translation produced for org A does not
   exist for org B. Loading receipts unfiltered made org B's gate report a
   document translated when the segments lived only in org A's tenant — the
   evidence half of the same leak `law.translation-dispatch/dispatch-key` closes
   on the identity half.

   A receipt naming no organization is excluded rather than treated as global.
   Admitting it into every tenant is exactly the failure being fixed."
  [org-id receipts]
  (filterv #(= org-id (:translation/org-id %)) receipts))

(defn- ^:async approvals-for-gate!
  "Load scoped approvals and optionally join current durable split history."
  [evidence-store {:keys [org-id project] :as scope} receipts
   {:keys [enforce-split-review-readiness? split-store digest-hex]}]
  (let [stored (->> (await (store/approvals!
                            evidence-store
                            (select-keys scope [:org-id :project])))
                    (filterv #(and (= org-id (:review/org-id %))
                                   (= project (:review/project %)))))]
    (if enforce-split-review-readiness?
      (await (split-projection/current-review-approvals!
              {:split-store split-store :digest-hex digest-hex}
              receipts stored))
      stored)))

(declare gate-evidence!)

(defn ^:async gate-facts!
  "Every fact `domain.publication-gate` needs, read once, scoped to one tenant.

   All four now come from real providers. `:approved?` reads recorded approvals
   rather than the `(constantly false)` this function used while no approval
   surface existed.

   Approvals are filtered by the same tenant and project as receipts, and for the
   same reason: review evidence attests to a translation that exists in one
   scope. `:approved?` remains inert for dispatch specifically —
   `translation-work` derives from the `:translation-missing` and
   `:translation-stale` blockers, never from the review blocker, so a review
   requirement does not suppress the translation that would satisfy it — but it
   is loaded here because the same facts answer whether the resulting
   publication is admissible, and computing them twice in two places is how the
   two answers drift."
  ([config evidence-store scope documents]
   (gate-facts! config evidence-store scope documents {}))
  ([config evidence-store scope documents document-roots]
   (:facts (await (gate-evidence! config evidence-store scope documents
                                  document-roots)))))

(defn ^:async gate-evidence!
  "The loaded translation evidence *and* the facts derived from it, read once.

   Split out from `gate-facts!` because two callers need different halves of one
   read. The gate needs only the closures; `infra.publication-runtime`
   additionally needs the evidence value itself, to look up the *output* revision
   an intent's receipt names so it can read the bytes that revision identifies.

   Returned together rather than exposed as two functions on purpose. Loading
   the receipts twice would let the facts the gate decided with and the receipt
   the content was read from come from two different reads — and a
   re-translation landing between them would publish the new bytes under the old
   approval, which is precisely the transplant the two-revision design prevents."
  ([config evidence-store scope documents document-roots]
   (gate-evidence! config evidence-store scope documents document-roots {}))
  ([config evidence-store {:keys [org-id project] :as scope} documents document-roots
    {:keys [source-revisions current-authored desired-work authenticate-content?
            enforce-split-review-readiness? split-store digest-hex]
     :or {current-authored []
          desired-work []
          authenticate-content? false
          enforce-split-review-readiness? false}}]
   (let [revisions (or source-revisions
                       (await (source-revision/source-revisions!
                               config documents document-roots)))
        ;; Scoped in the *query*. Reading every receipt ever recorded and
        ;; narrowing afterwards made each dispatch pass grow with the global
        ;; history of every tenant, and left the collection's own indexes unused.
        ;; The in-memory filters below stay as a second check — a store is
        ;; replaceable, and one that ignored the scope must not be able to widen
        ;; what the gate sees.
        scoped-receipts (->> (await (store/completed-translations!
                                     evidence-store
                                     (select-keys scope [:org-id :project])))
                             (#(contract-content/current-authored-receipts
                                % current-authored desired-work))
                             (tenant-receipts org-id)
                             (project-receipts project)
                             (current-source-locale-receipts documents))
        receipts (if authenticate-content?
                   (await (candidate-content/authenticated-receipts!
                           (:publication-content-root config)
                           document-roots
                           (into {} (map (juxt :document/id identity)) documents)
                           current-authored
                           scoped-receipts))
                   scoped-receipts)
        approvals (await (approvals-for-gate!
                          evidence-store scope receipts
                          {:enforce-split-review-readiness?
                           enforce-split-review-readiness?
                           :split-store split-store
                           :digest-hex digest-hex}))
        evidence (evidence-domain/evidence {:receipts receipts
                                            :approvals approvals})]
     {:evidence evidence
      :facts (merge (source-revision/revision-facts revisions)
                    (evidence-domain/gate-facts evidence))})))

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

(defn ^:async dispatch-intents-to-agent!
  "Announce the derived translation work of every intent to an agent actor.

   The agent-path counterpart of `infra.translation-dispatch/dispatch-intents!`,
   and sequential for the same reason that one is: fanning an entire garden's
   backlog out in one pass is how a reconciliation run becomes an incident, and
   here each item starts a model session rather than merely queueing a row.

   An intent whose source cannot be read is reported rather than dispatched. The
   bytes are what the agent translates, so there is no lawful dispatch without
   them — and silently skipping would look identical to 'nothing needed doing'."
  [deps index intents facts scope roots]
  (let [results (atom [])]
    (doseq [intent intents]
      (when-let [work (dispatch/derived-work intent facts)]
        (let [digest ((:current-source-revision facts) (:publication/document intent))
              source (await (document-source! index roots intent))
              checked-work (:action/with work)
              context (await (dispatch/candidate-recovery-context!
                              (:evidence-store deps)
                              checked-work
                              (dispatch/dispatch-context intent scope digest)))]
          (swap! results conj
                 (assoc (if (str/blank? (str source))
                          {:dispatch/outcome :dispatch/failed
                           :dispatch/detail
                           (str "the document's source could not be read, so"
                                " there are no bytes to translate")}
                          (await (agent-dispatch/dispatch-work!
                                  deps
                                  checked-work
                                  context
                                  source)))
                        :publication/id (:publication/id intent))))))
    @results))

(defn ^:async dispatch-translations!
  "Dispatch derived work for one publication, one document, or the corpus.

   Returns `{:considered n :admissible n :dispatched [...]}`. The counts are
   reported separately because an empty dispatch list is ambiguous on its own:
   nothing needed translating, nothing was looked at, and everything looked at
   was structurally inadmissible all read identically. An operator running this
   against the wrong scope — or against a garden that has been archived —
   deserves to be able to tell which."
  [config {:keys [evidence-store] :as deps} scope selection]
  (let [load-records! (or (:resource-records! deps)
                          publications/resource-records!)
        build-index (or (:publication-index deps)
                        publications/publication-index)
        load-revisions! (or (:source-revisions! deps)
                            source-revision/source-revisions!)
        ensure-receipts! (or (:ensure-contract-receipts! deps)
                             contract-content/ensure-receipts!)
        dispatch-agent! (or (:dispatch-agent-intents! deps)
                            dispatch-intents-to-agent!)
        dispatch-worker! (or (:dispatch-worker-intents! deps)
                             dispatch/dispatch-intents!)
        records (await (load-records! config))
        index (build-index records)
        hydrated (selected-hydrated-intents index selection)
        intents (admissible-intents index hydrated)
        documents (referenced-documents index intents)
        roots (document-source-roots config records)
        revisions (await (load-revisions! config documents roots))
        selected-publications (set (map :publication/id intents))
        desired-work (->> (review-inventory/desired-work index revisions)
                          (filterv #(contains? selected-publications
                                               (:publication/id %)))
                          (mapv #(assoc %
                                        :translation/org-id (:org-id scope)
                                        :translation/project (:project scope))))
        authored (await (ensure-receipts! evidence-store index roots scope revisions))
        facts (:facts
               (await (gate-evidence!
                       config evidence-store scope documents roots
                       {:source-revisions revisions
                        :current-authored authored
                        :desired-work desired-work
                        :authenticate-content? true})))
        selected-runner (runner config)]
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

(defn ^:async resolve-batch-status!
  "Turn one worker batch-status report into translation evidence.

   The *document*, not the status, decides whether a per-document binding can be
   resolved; a batch-level failure is resolved by batch id alone, which is sound
   because a Knoxx-created batch carries exactly one document. See
   `worker-report-vocabulary` for what the worker actually sends and why that
   matters. `failed-document-id` is still read because the field exists in the
   contract and a different worker may populate it.

   The report is validated and its batch id resolved *before* any store lookup,
   and both checks are load-bearing. Every branch below joins on that id, and a
   nil one is not a wildcard the stores decline to match — it is the value they
   match unbound records by. See `law/report-batch-id`."
  [deps report]
  (let [checked (law/assert-valid! :translation-dispatch/status-report
                                   law/BatchStatusReport
                                   report)
        failed-document (law/failed-document-id (:failed_document checked))
        batch-id (law/report-batch-id checked)]
    (cond
      (nil? batch-id)
      {:translation/refusal {:refusal/type :batch-id-missing
                             :refusal/actual (:batch_id checked)}}

      ;; Any status naming a completed document resolves that binding. The
      ;; worker sends "processing" here; the others are accepted so a worker
      ;; that reports differently still works.
      (some? (:completed_document checked))
      (await (dispatch/resolve-batch-report! deps checked))

      (some? failed-document)
      (await (dispatch/fail-batch-document! deps batch-id failed-document
                                            (or (:error checked)
                                                "worker reported failure")))

      ;; A batch-level failure names nothing, so the batch id is the binding.
      ;; Without this the claim would sit in flight forever and never be retried.
      (= "failed" (:status checked))
      (await (dispatch/fail-batch! deps batch-id
                                   (or (:error checked)
                                       "worker reported batch failure")))

      :else
      ;; Reported rather than dropped. `complete` and `partial` legitimately name
      ;; nothing — the per-document reports already resolved each binding — and
      ;; an operator debugging a translation that never appeared needs to see the
      ;; difference between 'nothing to resolve' and 'silently ignored'.
      {:translation/skipped {:reason :no-document-named
                             :status (:status checked)}})))
