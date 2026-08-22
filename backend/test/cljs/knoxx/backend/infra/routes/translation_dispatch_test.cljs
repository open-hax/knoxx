(ns knoxx.backend.infra.routes.translation-dispatch-test
  "The facade's routing of one worker status report.

  The report is the only thing the worker sends, and it arrives in several
  shapes for several reasons. Which of them resolves a binding, which records a
  failure, and which does neither is decided here — so it is tested here."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.translation-dispatch :as facade]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(def ^:private at "2026-08-22T09:00:00.000Z")

(def ^:private work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-aaa111bbb222"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"})

(defn- ^:async seeded-store!
  "A store holding one in-flight claim bound to `batch-1`."
  []
  (let [evidence-store (store/memory-store)
        record (law/dispatch-record work context :dispatch/accepted at)]
    (await (store/reserve-dispatch! evidence-store record))
    (await (store/bind-dispatch-batch! evidence-store (:dispatch/key record) "batch-1"))
    {:evidence-store evidence-store
     :clock (constantly at)}))

(deftest failed-document-id-reads-both-shapes-the-worker-can-send
  (testing "a bare id string"
    (is (= "knoxx.docs/probe" (law/failed-document-id "knoxx.docs/probe")))
    (is (= "knoxx.docs/probe" (law/failed-document-id "  knoxx.docs/probe  "))))

  (testing "a map naming one, under any of the spellings in use"
    (is (= "a/b" (law/failed-document-id {:document_id "a/b"})))
    (is (= "a/b" (law/failed-document-id {:document_wire_id "a/b"})))
    (is (= "a/b" (law/failed-document-id {:document "a/b"})))
    (is (= "a/b" (law/failed-document-id {:id "a/b"}))))

  (testing "the most specific spelling wins when several are present"
    (is (= "specific" (law/failed-document-id {:document_id "specific"
                                               :document "general"}))))

  (testing "nothing is guessed from a shape that names no id"
    ;; Treating a nested object as an id would produce a lookup that can never
    ;; match, which reads as 'no such dispatch' rather than as a bad report.
    (is (nil? (law/failed-document-id nil)))
    (is (nil? (law/failed-document-id {})))
    (is (nil? (law/failed-document-id {:document {:nested "thing"}})))
    (is (nil? (law/failed-document-id "   ")))))

(deftest ^:async a-completed-document-resolves-its-binding
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (testing "a receipt is minted"
      (is (some? (:translation/receipt result)))
      (is (= "sha256-aaa111bbb222"
             (:translation/source-revision (:translation/receipt result)))))))

(deftest ^:async a-failed-document-is-recorded-against-its-binding
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :failed_document {:document_id "knoxx.docs/probe"}
                             :error "model unavailable"}))]
    (testing "the attempt is failed, carrying the worker's reason"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (= "model unavailable" (:dispatch/detail (:dispatch/record result)))))

    (testing "a failed attempt is not a translation"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))

(deftest ^:async a-report-naming-no-document-is-reported-not-dropped
  (let [deps (await (seeded-store!))]
    (testing "a batch merely going processing resolves nothing, and says so"
      ;; Silence here is the difference between 'nothing to resolve' and
      ;; 'silently ignored', and an operator debugging a missing translation
      ;; needs to be able to tell them apart.
      (is (= :no-document-named
             (:reason (:translation/skipped
                       (await (facade/resolve-batch-status!
                               deps {:status "processing" :batch_id "batch-1"})))))))

    (testing "a completion with no document named resolves nothing"
      (is (= :no-document-named
             (:reason (:translation/skipped
                       (await (facade/resolve-batch-status!
                               deps {:status "complete" :batch_id "batch-1"})))))))

    (testing "nothing was recorded either way"
      (is (empty? (await (store/completed-translations! (:evidence-store deps))))))))
