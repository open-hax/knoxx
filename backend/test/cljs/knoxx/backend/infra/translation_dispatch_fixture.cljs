(ns knoxx.backend.infra.translation-dispatch-fixture
  "Translation dispatch fixture contracts and finite fixtures."
  (:require [knoxx.backend.infra.openplanner-fixture :as planner-fixture]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]))

(def intent
  "Translation dispatch fixture: intent."
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision "sha256-aaa111bbb222"
   :publication/state :published
   :publication/path "/probe"
   :translation/review :none
   :document/source-locale :en})

(def scope
  "Translation dispatch fixture: scope." {:org-id "org-1" :membership-id "member-1"})

(def dispatched-revision
  "The concrete source digest used by the dispatch fixture."
  "sha256-aaa111bbb222")

(def evidence-scope
  "The tenant and project this fixture dispatches under. Reads are scoped in the
   query now, so a test that read unscoped would see nothing."
  {:org-id "org-1" :project nil})

(def facts
  "Translation dispatch fixture: facts."
  {:current-source-revision (constantly "sha256-aaa111bbb222")
   :translated-revision? (constantly false)
   :approved? (constantly false)
   :source-revision-superseded? (constantly false)})

(def clock
  "Translation dispatch fixture: clock." (constantly "2026-08-22T09:00:00.000Z"))

(defn- batch-response
  "What `recover-settled-batch!` sees when it re-reads the batch.

   An explicit `batch-view` is used as given. A bare `batch-status` is expanded
   into the shape the real projection carries — the status route pushes a
   completed document onto `completed_documents`, so a `complete` batch names it.
   Neither configured means the batch is unreadable, which recovery treats as
   still running: the honest default for a test not exercising recovery."
  [batch-view batch-status]
  (cond
    batch-view (js/Promise.resolve batch-view)
    batch-status (js/Promise.resolve
                  {:status batch-status
                   :completed_documents (if (= "complete" batch-status)
                                          ["knoxx.docs/probe"]
                                          [])
                   :failed_documents []})
    :else (throw (ex-info "unexpected call to translation-batch!" {}))))

(defn fake-client
  "Record batch requests and answer only explicitly seeded protocol operations."
  [{:keys [batches answer observed batch-status batch-view]}]
  (let [respond (or answer (fn [_request n] {:batch_id (str "batch-" n)}))
        list-batches (or observed (fn [_opts] {:batches []}))]
    (planner-fixture/client
     {:enabled? (constantly true)
      :create-translation-batch!
      (fn [_ payload]
        (swap! batches conj payload)
        (js/Promise.resolve (respond payload (count @batches))))
      :translation-batches!
      (fn [_ opts] (js/Promise.resolve (list-batches opts)))
      :translation-batch!
      (fn [_ _ _] (batch-response batch-view batch-status))})))

(defn fixture
  "Fresh store, client and recorded batch list for one test."
  [& {:keys [answer observed source-revision batch-status batch-view]}]
  (let [batches (atom [])]
    {:batches batches
     :deps {:evidence-store (store/memory-store)
            :client (fake-client {:batches batches
                                  :answer answer
                                  :observed observed
                                  :batch-status batch-status
                                  :batch-view batch-view})
            :clock clock
            ;; Defaults to agreeing with the dispatched revision, so completion
            ;; is not refused for source drift. The drift path overrides it.
            :observe-source-revision
            (constantly (js/Promise.resolve
                         (or source-revision dispatched-revision)))}}))

(defn work
  "Translation dispatch fixture: work." []
  (:action/with (dispatch/derived-work intent facts)))

(defn context
  "Translation dispatch fixture: context." []
  ;; The digest observed at dispatch time. Equal to the intent's revision here
  ;; because the fixture's revision IS a content digest; a pinned opaque revision
  ;; would differ, which is why the two are recorded separately.
  (dispatch/dispatch-context intent scope dispatched-revision))

