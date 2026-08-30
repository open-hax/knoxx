(ns knoxx.backend.infra.translation-split-store-test
  "Atomic-turn, immutable-child, and future-memory contracts for split storage."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.translation-split-store :as store]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]))

(def execution-input
  {:agent-id "publication_translator"
   :model "gemma4:31b"
   :thinking :medium
   :system-prompt "Translate only the admitted source members."
   :tool-ids ["save_translation"]})

(defn- turn-context
  "Build one complete split workflow without persisting any of it."
  ([] (turn-context {}))
  ([{:keys [dispatch-key run-id admitted-at execution memory
            candidate-revision]
     :or {dispatch-key "dispatch-key-1"
          run-id "translation-run-1"
          admitted-at "2026-08-30T11:59:00.000Z"
          candidate-revision "candidate-revision-1"}}]
   (let [manifest (fixture/manifest)
         claim (split/candidate-claim fixture/digest manifest
                                      candidate-revision)
         candidates (fixture/candidates claim)
         candidate-set (split/complete-candidate-set
                        fixture/digest manifest claim candidates)
         execution (or execution
                       (split/execution-snapshot fixture/digest execution-input))
         memory (or memory (split/memory-snapshot {:status :empty :examples []}))
         turn (split/translation-turn-admission
               fixture/digest
               {:dispatch-key dispatch-key
                :run-id run-id
                :admitted-at admitted-at
                :manifest manifest
                :candidate-claim claim
                :execution execution
                :memory memory})]
     {:manifest manifest
      :claim claim
      :candidates candidates
      :candidate-set candidate-set
      :turn turn})))

(defn- persist-through-candidates!
  "Persist the atomic turn and each separately durable candidate member."
  [translation-store {:keys [turn candidates]}]
  (store/admit-turn! translation-store turn)
  (doseq [candidate candidates]
    (store/append-candidate-split! translation-store
                                   (:translation-turn/id turn)
                                   candidate)))

(defn- persist-through-candidate-set!
  "Persist a complete workflow through its raw candidate-set evidence."
  [translation-store {:keys [turn candidate-set] :as context}]
  (persist-through-candidates! translation-store context)
  (store/complete-candidate-set! translation-store
                                 (:translation-turn/id turn)
                                 candidate-set))

(defn- review-receipt
  "Build a receipt for one exact split with optional request changes."
  [manifest candidate-set split-id recorded-at overrides]
  (fixture/review-receipt
   manifest candidate-set split-id fixture/principal recorded-at
   (merge (fixture/review-request) overrides)))

(deftest turn-admission-atomically-claims-id-and-run
  (let [{:keys [turn]} (turn-context)
        translation-store (store/memory-store fixture/digest)]
    (testing "equal aggregate retries return the first immutable turn"
      (is (= turn (store/admit-turn! translation-store turn)))
      (is (= turn (store/admit-turn! translation-store turn))))

    (testing "both exact point selectors resolve the same authenticated fact"
      (is (= turn
             (store/turn-by-id! translation-store
                                (:translation-turn/id turn))))
      (is (= turn
             (store/turn-for-run! translation-store
                                  (:translation-turn/run-id turn))))
      (is (nil? (store/turn-for-run! translation-store "missing-run"))))

    (testing "one run cannot be reused with changed execution or memory"
      (let [changed (-> (turn-context
                         {:run-id (:translation-turn/run-id turn)
                          :dispatch-key (:translation-turn/dispatch-key turn)
                          :execution (split/execution-snapshot
                                      fixture/digest
                                      (assoc execution-input :model "another-model"))})
                        :turn)]
        (is (thrown-with-msg?
             js/Error
             #"immutable translation turn identity conflicts"
             (store/admit-turn! translation-store changed)))))))

(deftest candidate-members-are-turn-bound-and-read-in-admitted-order
  (let [{:keys [turn manifest claim candidates]} (turn-context)
        translation-store (store/memory-store fixture/digest)]
    (store/admit-turn! translation-store turn)

    (testing "provider completion order never becomes composition order"
      (doseq [candidate (reverse candidates)]
        (store/append-candidate-split! translation-store
                                       (:translation-turn/id turn)
                                       candidate))
      (is (= [0 1 2]
             (mapv :candidate/split-index
                   (store/candidate-splits-for-turn!
                    translation-store (:translation-turn/id turn))))))

    (testing "a candidate from another claim cannot cross the turn boundary"
      (let [other-claim (split/candidate-claim fixture/digest manifest
                                               "candidate-revision-2")
            foreign (split/candidate-split
                     fixture/digest
                     (first (:candidate-claim/members other-claim))
                     "foreign")]
        (is (= (:candidate-claim/id claim)
               (get-in turn [:translation-turn/candidate-claim
                             :candidate-claim/id])))
        (is (thrown-with-msg?
             js/Error
             #"does not match its admitted turn"
             (store/append-candidate-split! translation-store
                                            (:translation-turn/id turn)
                                            foreign)))))))

(deftest complete-candidate-set-requires-durable-exact-coverage
  (let [{:keys [turn candidates candidate-set] :as context} (turn-context)
        translation-store (store/memory-store fixture/digest)
        turn-id (:translation-turn/id turn)]
    (store/admit-turn! translation-store turn)
    (store/append-candidate-split! translation-store turn-id (first candidates))

    (testing "one persisted member cannot become document completion"
      (is (thrown-with-msg?
           js/Error
           #"persisted candidate members do not equal"
           (store/complete-candidate-set! translation-store turn-id candidate-set))))

    (testing "exact coverage admits one idempotent complete set"
      (doseq [candidate (rest candidates)]
        (store/append-candidate-split! translation-store turn-id candidate))
      (is (= candidate-set
             (store/complete-candidate-set!
              translation-store turn-id candidate-set)))
      (is (= candidate-set
             (store/complete-candidate-set!
              translation-store turn-id candidate-set)))
      (is (= candidate-set
             (store/candidate-set-for-turn! translation-store turn-id)))
      (is (= candidate-set
             (store/candidate-set-by-id!
              translation-store (:candidate-set/id candidate-set)))))

    (testing "the helper persists the same evidence used above"
      (is (= candidate-set
             (persist-through-candidate-set!
              (store/memory-store fixture/digest) context))))))

(deftest review-retry-keeps-first-server-timestamp
  (let [{:keys [manifest candidate-set] :as context} (turn-context)
        translation-store (store/memory-store fixture/digest)
        split-id (:split/id (first (:split-manifest/splits manifest)))
        first-receipt (review-receipt manifest candidate-set split-id
                                      "2026-08-30T12:00:00.000Z" {})
        clocked-retry (review-receipt manifest candidate-set split-id
                                      "2026-08-30T12:05:00.000Z" {})
        changed (review-receipt
                 manifest candidate-set split-id
                 "2026-08-30T12:05:00.000Z"
                 {:review/overall "reject"
                  :review/corrected-text nil})]
    (persist-through-candidate-set! translation-store context)
    (is (= (:review/id first-receipt) (:review/id clocked-retry)))
    (is (= first-receipt
           (store/append-review-receipt! translation-store first-receipt)))
    (is (= first-receipt
           (store/append-review-receipt! translation-store clocked-retry)))
    (is (thrown-with-msg?
         js/Error
         #"immutable review receipt identity conflicts"
         (store/append-review-receipt! translation-store changed)))
    (is (= [first-receipt]
           (store/review-history-for-split!
            translation-store (:candidate-set/id candidate-set) split-id)))))

(deftest approved-corrections-are-applicable-to-future-turns
  (let [{:keys [manifest candidate-set] :as context} (turn-context)
        translation-store (store/memory-store fixture/digest)
        split-id (:split/id (second (:split-manifest/splits manifest)))
        approval (review-receipt manifest candidate-set split-id
                                 "2026-08-30T12:00:00.000Z" {})
        rejection (review-receipt
                   manifest candidate-set split-id
                   "2026-08-30T12:01:00.000Z"
                   {:review/operation-id "review-operation-2"
                    :review/overall "reject"
                    :review/corrected-text nil})
        scope {:org-id (:split-manifest/org-id manifest)
               :project (:split-manifest/project manifest)
               :garden (:split-manifest/garden manifest)
               :source-locale (:split-manifest/source-locale manifest)
               :target-locale (:split-manifest/target-locale manifest)
               :current-candidate-set-ids #{(:candidate-set/id candidate-set)}}]
    (persist-through-candidate-set! translation-store context)
    (store/append-review-receipt! translation-store approval)

    (testing "a later translation can retrieve exact approved corrected bytes"
      (is (= ["Párrafo inicial corregido.\n\n"]
             (mapv :translation-memory/target-text
                   (store/applicable-memory! translation-store scope))))
      (is (empty? (store/applicable-memory!
                   translation-store (assoc scope :org-id "another-org")))))

    (testing "a later rejection suppresses, rather than deletes, prior memory"
      (store/append-review-receipt! translation-store rejection)
      (is (empty? (store/applicable-memory! translation-store scope)))
      (is (= [approval rejection]
             (store/review-history-for-split!
              translation-store (:candidate-set/id candidate-set) split-id))))))

(deftest applicable-memory-requires-the-server-current-candidate-allowset
  (let [context-a (turn-context
                   {:dispatch-key "shared-dispatch-key"
                    :run-id "translation-run-a"
                    :admitted-at "2026-08-30T11:00:00.000Z"
                    :candidate-revision "candidate-revision-a"})
        context-b (turn-context
                   {:dispatch-key "shared-dispatch-key"
                    :run-id "translation-run-b"
                    :admitted-at "2026-08-30T11:30:00.000Z"
                    :candidate-revision "candidate-revision-b"})
        translation-store (store/memory-store fixture/digest)
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
    (persist-through-candidate-set! translation-store context-a)
    (persist-through-candidate-set! translation-store context-b)
    (store/append-review-receipt! translation-store approval-a)
    (store/append-review-receipt! translation-store approval-b)

    (testing "an explicit current allowset independently excludes superseded A"
      (let [examples (store/applicable-memory!
                      translation-store
                      (assoc base-scope :current-candidate-set-ids #{set-id-b}))]
        (is (= [set-id-b]
               (mapv :translation-memory/candidate-set-id examples)))
        (is (= ["Correccion vigente B.\n\n"]
               (mapv :translation-memory/target-text examples)))
        (is (not-any? #(= set-id-a
                          (:translation-memory/candidate-set-id %))
                      examples))))

    (testing "the internal memory boundary refuses a caller without authority"
      (is (thrown-with-msg?
           js/Error
           #"invalid translation memory scope"
           (store/applicable-memory! translation-store base-scope))))))

(deftest child-selectors-refuse-missing-parent-authority
  (let [translation-store (store/memory-store fixture/digest)]
    (is (thrown-with-msg?
         js/Error
         #"translation turn is not persisted"
         (store/candidate-splits-for-turn! translation-store "missing-turn")))
    (is (thrown-with-msg?
         js/Error
         #"candidate set is not persisted"
         (store/review-history-for-split!
          translation-store "missing-set" "missing-split")))))
