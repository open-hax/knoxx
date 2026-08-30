(ns knoxx.backend.law.translation-split-turn-test
  (:require [cljs.test :as t]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as fixture]
            [knoxx.backend.law.translation-split-turn :as turn-law]))

(def execution-input
  {:agent-id "publication_translator"
   :model "gemma4:31b"
   :thinking :medium
   :system-prompt "Translate the exact admitted members."
   :tool-ids ["save_translation"]})

(defn- turn
  "Build one authenticated turn with optional memory replacement."
  ([] (turn (split/memory-snapshot {:status :empty :examples []})))
  ([memory]
   (let [manifest (fixture/manifest)
         claim (fixture/claim manifest)]
     (split/translation-turn-admission
      fixture/digest
      {:dispatch-key "dispatch-key-1"
       :run-id "translation-run-1"
       :admitted-at "2026-08-30T11:59:00.000Z"
       :manifest manifest
       :candidate-claim claim
       :execution (split/execution-snapshot fixture/digest execution-input)
       :memory memory}))))

(t/deftest execution-policy-is-content-addressed
  (let [snapshot (split/execution-snapshot fixture/digest execution-input)]
    (t/is (= snapshot
             (split/assert-execution-integrity! fixture/digest snapshot)))
    (t/is (thrown-with-msg?
           js/Error
           #"execution snapshot digest"
           (split/assert-execution-integrity!
            fixture/digest
            (assoc snapshot :translation-execution/model "another-model"))))))

(t/deftest memory-outcomes-are-truthful-and-closed
  (t/is (= :empty
           (:translation-memory-snapshot/status
            (split/memory-snapshot {:status :empty :examples []}))))
  (t/is (= "memory store unavailable"
           (:translation-memory-snapshot/error
            (split/memory-snapshot {:status :failed
                                    :examples []
                                    :error "memory store unavailable"}))))
  (doseq [invalid [{:status :found :examples []}
                   {:status :empty :examples [] :error "not empty"}
                   {:status :failed :examples []}
                   {:status :unknown :examples []}]]
    (t/is (thrown-with-msg?
           js/Error
           #"invalid memory-snapshot"
           (split/memory-snapshot invalid)))))

(t/deftest one-turn-id-binds-every-provider-visible-fact
  (let [value (turn)]
    (t/is (= value (split/assert-turn-integrity! fixture/digest value)))
    (doseq [tampered [(assoc value :translation-turn/dispatch-key "other-key")
                      (assoc-in value [:translation-turn/execution
                                       :translation-execution/model]
                                "other-model")
                      (assoc-in value [:translation-turn/manifest
                                       :split-manifest/source-digest]
                                "other-source")]]
      (t/is (thrown-with-msg?
             js/Error
             #"(turn admission identity|execution snapshot digest|manifest identity)"
             (split/assert-turn-integrity! fixture/digest tampered))))))

(t/deftest review-dispatch-binding-has-one-shared-authority
  (let [value (turn)
        manifest (:translation-turn/manifest value)
        record {:dispatch/key (:translation-turn/dispatch-key value)
                :dispatch/batch-id (:translation-turn/run-id value)
                :dispatch/org-id (:split-manifest/org-id manifest)
                :dispatch/project (:split-manifest/project manifest)
                :dispatch/garden (:split-manifest/garden manifest)
                :dispatch/document (:split-manifest/document manifest)
                :dispatch/source-locale (:split-manifest/source-locale manifest)
                :dispatch/locale (:split-manifest/target-locale manifest)
                :dispatch/revision (:split-manifest/source-revision manifest)}]
    (t/is (= (turn-law/dispatch-review-binding record)
             (turn-law/turn-review-binding value)))
    (t/is (turn-law/review-binding-matches? record value))

    (t/testing "every shared review coordinate is load-bearing"
      (doseq [dispatch-key [:dispatch/key :dispatch/batch-id :dispatch/org-id
                            :dispatch/project :dispatch/garden :dispatch/document
                            :dispatch/source-locale :dispatch/locale
                            :dispatch/revision]]
        (t/is (not (turn-law/review-binding-matches?
                    (assoc record dispatch-key "mismatch") value)))))

    (t/testing "completion-only facts stay outside the common review binding"
      (t/is (turn-law/review-binding-matches?
             (assoc record
                    :dispatch/at "2026-08-30T12:00:00.000Z"
                    :dispatch/outcome :dispatch/accepted)
             value)))))
