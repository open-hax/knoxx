(ns knoxx.backend.infra.stores.mongo-translation-split-test
  "Mongo adapter contracts over a small fake driver boundary."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.stores.mongo-translation-split :as mongo-store]
            [knoxx.backend.infra.translation-split-store :as store]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(def ^:private execution-input
  "Provider policy shared by turn fixtures."
  {:agent-id "publication_translator"
   :model "gemma4:31b"
   :thinking :medium
   :system-prompt "Translate only the admitted source members."
   :tool-ids ["save_translation"]})

(defn- duplicate-key-error
  "Return the native error shape recognized by the Mongo extern adapter."
  []
  (let [error (js/Error. "E11000 duplicate key error")]
    (aset error "code" 11000)
    error))

(defn- matching-row?
  "Whether every exact query selector agrees with one stored row."
  [query row]
  (every? (fn [[key value]] (= value (get row key))) query))

(defn- fake-cursor
  "Create the cursor surface used by `extern.mongo/find-docs!`."
  [rows]
  (let [limit* (atom nil)
        cursor (js-obj)]
    (aset cursor "limit"
          (fn [limit]
            (reset! limit* limit)
            cursor))
    (aset cursor "toArray"
          (fn []
            (let [selected (if-let [limit @limit*]
                             (take limit rows)
                             rows)]
              (js/Promise.resolve (clj->js (vec selected))))))
    cursor))

(defn- fake-collection
  "Create one fake collection with declared unique scalar fields."
  [collection-name rows unique-fields queries indexes]
  #js {:insertOne
       (fn [native-doc]
         (let [doc (js->clj native-doc :keywordize-keys true)
               collision?
               (some (fn [field]
                       (some #(= (get doc field) (get % field)) @rows))
                     unique-fields)]
           (if collision?
             (js/Promise.reject (duplicate-key-error))
             (do
               (swap! rows conj doc)
               (js/Promise.resolve #js {})))))
       :find
       (fn [native-query]
         (let [query (js->clj native-query :keywordize-keys true)]
           (swap! queries conj {:collection collection-name :query query})
           (fake-cursor (filterv #(matching-row? query %) @rows))))
       :createIndex
       (fn [native-spec native-options]
         (swap! indexes conj
                {:collection collection-name
                 :spec (js->clj native-spec :keywordize-keys true)
                 :options (js->clj native-options :keywordize-keys true)})
         (js/Promise.resolve "index-name"))})

(defn- fake-collections
  "Build every named fake collection over the supplied observation atoms."
  [{:keys [turns candidates candidate-sets reviews queries indexes]}]
  {mongo-store/TURNS_COLLECTION
   (fake-collection mongo-store/TURNS_COLLECTION turns
                    [:turn_id :run_id] queries indexes)
   mongo-store/CANDIDATE_SPLITS_COLLECTION
   (fake-collection mongo-store/CANDIDATE_SPLITS_COLLECTION candidates
                    [:attempt_id] queries indexes)
   mongo-store/CANDIDATE_SETS_COLLECTION
   (fake-collection mongo-store/CANDIDATE_SETS_COLLECTION candidate-sets
                    [:candidate_set_id :turn_id] queries indexes)
   mongo-store/REVIEW_RECEIPTS_COLLECTION
   (fake-collection mongo-store/REVIEW_RECEIPTS_COLLECTION reviews
                    [:review_id] queries indexes)})

(defn- mongo-fixture
  "Create isolated rows, query observations, and one production adapter."
  []
  (let [turns (atom [])
        candidates (atom [])
        candidate-sets (atom [])
        reviews (atom [])
        queries (atom [])
        indexes (atom [])
        collections (fake-collections
                     {:turns turns :candidates candidates
                      :candidate-sets candidate-sets :reviews reviews
                      :queries queries :indexes indexes})
        db #js {:collection (fn [collection-name]
                              (get collections collection-name))}]
    {:db db
     :turns turns
     :candidates candidates
     :candidate-sets candidate-sets
     :reviews reviews
     :queries queries
     :indexes indexes
     :store (mongo-store/create-store db fixture/digest)}))

(defn- turn-context
  "Build one atomic turn and its complete child workflow."
  ([] (turn-context {}))
  ([{:keys [dispatch-key run-id admitted-at execution candidate-revision]
     :or {dispatch-key "dispatch-key-1"
          run-id "translation-run-1"
          admitted-at "2026-08-30T11:59:00.000Z"
          candidate-revision "candidate-revision-1"}}]
   (let [manifest (fixture/manifest)
         claim (split/candidate-claim fixture/digest manifest candidate-revision)
         candidates (fixture/candidates claim)
         candidate-set (split/complete-candidate-set fixture/digest manifest
                                                     claim candidates)
         turn (split/translation-turn-admission
          fixture/digest
          {:dispatch-key dispatch-key
           :run-id run-id
           :admitted-at admitted-at
           :manifest manifest
           :candidate-claim claim
           :execution (or execution
                          (split/execution-snapshot
                           fixture/digest execution-input))
           :memory (split/memory-snapshot {:status :empty :examples []})})]
     {:manifest manifest
      :claim claim
      :candidates candidates
      :candidate-set candidate-set
      :turn turn})))

(defn- ^:async persist-through-set!
  "Persist one turn, every candidate, and its complete candidate set."
  [translation-store {:keys [turn candidates candidate-set]}]
  (let [turn-id (:translation-turn/id turn)]
    (await (store/admit-turn! translation-store turn))
    (doseq [candidate candidates]
      (await (store/append-candidate-split!
              translation-store turn-id candidate)))
    (await (store/complete-candidate-set!
            translation-store turn-id candidate-set))))

(defn- review-receipt
  "Build one candidate-bound receipt with optional request overrides."
  [manifest candidate-set split-id recorded-at overrides]
  (fixture/review-receipt
   manifest candidate-set split-id fixture/principal recorded-at
   (merge (fixture/review-request) overrides)))

(defn- ^:async rejection-of
  "Return the rejection reason from one expected-to-fail promise."
  [promise]
  (try
    (await promise)
    nil
    (catch :default error error)))

(deftest ^:async setup-declares-atomic-and-exact-selector-indexes
  (let [{:keys [db indexes]} (mongo-fixture)]
    (is (true? (await (mongo-store/setup-indexes! db))))
    (testing "turn id and provider run id are independent unique claims"
      (is (some #(= {:collection mongo-store/TURNS_COLLECTION
                     :spec {:turn_id 1}
                     :options {:unique true}}
                   %)
                @indexes))
      (is (some #(= {:collection mongo-store/TURNS_COLLECTION
                     :spec {:run_id 1}
                     :options {:unique true}}
                   %)
                @indexes)))
    (testing "future memory uses its complete tenant and locale selector"
      (is (some #(= {:org_id 1 :project 1
                     :source_locale 1 :target_locale 1}
                   (:spec %))
                @indexes)))
    (testing "review history index matches its exact total order"
      (is (some #(= {:collection mongo-store/REVIEW_RECEIPTS_COLLECTION
                     :spec {:candidate_set_id 1 :split_id 1 :recorded_at 1
                            :operation_id 1 :review_id 1}
                     :options {}}
                   %)
                @indexes)))
    (testing "every immutable child identity is unique"
      (is (= 6 (count (filter #(true? (get-in % [:options :unique]))
                             @indexes)))))))

(deftest ^:async one-row-atomically-admits-the-full-provider-turn
  (let [{translation-store :store turns :turns queries :queries}
        (mongo-fixture)
        {:keys [turn]} (turn-context)
        turn-id (:translation-turn/id turn)
        run-id (:translation-turn/run-id turn)]
    (testing "equal retries and both point selectors return one fact"
      (is (= turn (await (store/admit-turn! translation-store turn))))
      (is (= turn (await (store/admit-turn! translation-store turn))))
      (is (= turn (await (store/turn-by-id! translation-store turn-id))))
      (is (= turn (await (store/turn-for-run! translation-store run-id))))
      (is (= 1 (count @turns))))

    (testing "the authoritative EDN preserves namespaced resource identities"
      (let [row (first @turns)]
        (is (str/includes? (:turn_edn row) ":open-hax.gardens/promethean"))
        (is (= (pr-str :open-hax.documents/start-here) (:document row)))
        (is (= turn-id (:turn_id row)))
        (is (= run-id (:run_id row)))))

    (testing "point reads issue exact scalar queries"
      (is (some #(= {:turn_id turn-id} (:query %)) @queries))
      (is (some #(= {:run_id run-id} (:query %)) @queries)))

    (testing "changed execution behind the same run is a conflict"
      (let [changed
            (:turn
             (turn-context
              {:run-id run-id
               :execution
               (split/execution-snapshot
                fixture/digest (assoc execution-input :model "another-model"))}))
            error (await (rejection-of
                          (store/admit-turn! translation-store changed)))]
        (is (re-find #"immutable translation turn identity conflicts"
                     (ex-message error)))
        (is (= 1 (count @turns)))))))

(deftest ^:async candidates-and-complete-sets-are-monotonic-turn-children
  (let [{translation-store :store candidates-rows :candidates
         set-rows :candidate-sets}
        (mongo-fixture)
        {:keys [turn candidates candidate-set]} (turn-context)
        turn-id (:translation-turn/id turn)]
    (await (store/admit-turn! translation-store turn))
    (await (store/append-candidate-split!
            translation-store turn-id (first candidates)))

    (testing "partial provider output cannot be promoted to completion"
      (let [error (await (rejection-of
                          (store/complete-candidate-set!
                           translation-store turn-id candidate-set)))]
        (is (re-find #"persisted candidate members do not equal"
                     (ex-message error)))))

    (testing "provider arrival order never becomes composition order"
      (doseq [candidate (rest candidates)]
        (await (store/append-candidate-split!
                translation-store turn-id candidate)))
      (is (= [0 1 2]
             (mapv :candidate/split-index
                   (await (store/candidate-splits-for-turn!
                           translation-store turn-id)))))
      (is (= 3 (count @candidates-rows))))

    (testing "exact durable coverage admits one idempotent complete set"
      (is (= candidate-set
             (await (store/complete-candidate-set!
                     translation-store turn-id candidate-set))))
      (is (= candidate-set
             (await (store/complete-candidate-set!
                     translation-store turn-id candidate-set))))
      (is (= candidate-set
             (await (store/candidate-set-for-turn!
                     translation-store turn-id))))
      (is (= candidate-set
             (await (store/candidate-set-by-id!
                     translation-store (:candidate-set/id candidate-set)))))
      (is (= 1 (count @set-rows))))))

(deftest ^:async review-retries-keep-first-time-and-drive-future-memory
  (let [{translation-store :store reviews :reviews queries :queries}
        (mongo-fixture)
        {:keys [manifest candidate-set] :as context} (turn-context)
        split-id (:split/id (second (:split-manifest/splits manifest)))
        first-receipt (review-receipt
                       manifest candidate-set split-id
                       "2026-08-30T12:00:00.000Z" {})
        clocked-retry (review-receipt
                       manifest candidate-set split-id
                       "2026-08-30T12:05:00.000Z" {})
        changed (review-receipt
                 manifest candidate-set split-id
                 "2026-08-30T12:05:00.000Z"
                 {:review/overall "reject" :review/corrected-text nil})
        later-rejection
        (review-receipt
         manifest candidate-set split-id
         "2026-08-30T12:00:00.000Z"
         {:review/operation-id "review-operation-2"
          :review/overall "reject" :review/corrected-text nil})
        scope {:org-id (:split-manifest/org-id manifest)
               :project (:split-manifest/project manifest)
               :garden (:split-manifest/garden manifest)
               :source-locale (:split-manifest/source-locale manifest)
               :target-locale (:split-manifest/target-locale manifest)
               :current-candidate-set-ids #{(:candidate-set/id candidate-set)}}]
    (await (persist-through-set! translation-store context))

    (testing "one operation id owns the first server timestamp"
      (is (= first-receipt
             (await (store/append-review-receipt!
                     translation-store first-receipt))))
      (is (= first-receipt
             (await (store/append-review-receipt!
                     translation-store clocked-retry))))
      (is (= 1 (count @reviews)))
      (is (= (:review/operation-id first-receipt)
             (:operation_id (first @reviews))))
      (is (= [first-receipt]
             (await (store/review-history-for-split!
                     translation-store
                     (:candidate-set/id candidate-set) split-id)))))

    (testing "changed judgment behind the operation conflicts"
      (let [error (await (rejection-of
                          (store/append-review-receipt!
                           translation-store changed)))]
        (is (re-find #"immutable review receipt identity conflicts"
                     (ex-message error)))))

    (testing "approved corrected bytes are available in the exact future scope"
      (is (= ["Párrafo inicial corregido.\n\n"]
             (mapv :translation-memory/target-text
                   (await (store/applicable-memory!
                           translation-store scope)))))
      (is (empty? (await (store/applicable-memory!
                          translation-store
                          (assoc scope :org-id "another-org")))))
      (is (some #(= {:org_id "open-hax"
                     :project "promethean"
                     :source_locale (pr-str :en)
                     :target_locale (pr-str :es)}
                   (:query %))
                @queries)))

    (testing "same-time operation order suppresses memory and orders history"
      (await (store/append-review-receipt!
              translation-store later-rejection))
      (is (empty? (await (store/applicable-memory!
                          translation-store scope))))
      (is (= [first-receipt later-rejection]
             (await (store/review-history-for-split!
                     translation-store
                     (:candidate-set/id candidate-set) split-id)))))))

(deftest ^:async mongo-memory-requires-the-server-current-candidate-allowset
  (let [{translation-store :store} (mongo-fixture)
        context-a (turn-context
                   {:dispatch-key "shared-dispatch-key"
                    :run-id "translation-run-a"
                    :admitted-at "2026-08-30T11:00:00.000Z"
                    :candidate-revision "candidate-revision-a"})
        context-b (turn-context
                   {:dispatch-key "shared-dispatch-key"
                    :run-id "translation-run-b"
                    :admitted-at "2026-08-30T11:30:00.000Z"
                    :candidate-revision "candidate-revision-b"})
        split-id-a (get-in context-a [:manifest :split-manifest/splits 1
                                      :split/id])
        split-id-b (get-in context-b [:manifest :split-manifest/splits 1
                                      :split/id])
        set-id-a (get-in context-a [:candidate-set :candidate-set/id])
        set-id-b (get-in context-b [:candidate-set :candidate-set/id])
        approval-a (review-receipt
                    (:manifest context-a) (:candidate-set context-a) split-id-a
                    "2026-08-30T12:00:00.000Z"
                    {:review/operation-id "review-operation-a"
                     :review/corrected-text "Correccion obsoleta A.\n\n"})
        approval-b (review-receipt
                    (:manifest context-b) (:candidate-set context-b) split-id-b
                    "2026-08-30T12:01:00.000Z"
                    {:review/operation-id "review-operation-b"
                     :review/corrected-text "Correccion vigente B.\n\n"})
        base-scope {:org-id (get-in context-a [:manifest
                                               :split-manifest/org-id])
                    :project (get-in context-a [:manifest
                                                :split-manifest/project])
                    :garden (get-in context-a [:manifest
                                               :split-manifest/garden])
                    :source-locale (get-in context-a [:manifest
                                                      :split-manifest/source-locale])
                    :target-locale (get-in context-a [:manifest
                                                      :split-manifest/target-locale])}]
    (await (persist-through-set! translation-store context-a))
    (await (persist-through-set! translation-store context-b))
    (await (store/append-review-receipt! translation-store approval-a))
    (await (store/append-review-receipt! translation-store approval-b))

    (testing "Mongo independently filters superseded A by the explicit allowset"
      (let [examples (await (store/applicable-memory!
                             translation-store
                             (assoc base-scope
                                    :current-candidate-set-ids #{set-id-b})))]
        (is (= [set-id-b]
               (mapv :translation-memory/candidate-set-id examples)))
        (is (= ["Correccion vigente B.\n\n"]
               (mapv :translation-memory/target-text examples)))
        (is (not-any? #(= set-id-a
                          (:translation-memory/candidate-set-id %))
                      examples))))

    (testing "Mongo refuses an unbounded internal memory query"
      (let [error (await (rejection-of
                          (store/applicable-memory!
                           translation-store base-scope)))]
        (is (re-find #"invalid translation memory scope"
                     (ex-message error)))))))

(deftest ^:async corrupted-flat-selectors-never-authorize-edn
  (let [{translation-store :store turns :turns} (mongo-fixture)
        {:keys [turn]} (turn-context)
        turn-id (:translation-turn/id turn)]
    (await (store/admit-turn! translation-store turn))
    (swap! turns update 0 assoc :manifest_id "another-manifest")
    (let [error (await (rejection-of
                        (store/turn-by-id! translation-store turn-id)))]
      (is (re-find #"selectors disagree with its EDN record"
                   (ex-message error))))))
