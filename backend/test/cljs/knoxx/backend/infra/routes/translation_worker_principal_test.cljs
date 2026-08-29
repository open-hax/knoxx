(ns knoxx.backend.infra.routes.translation-worker-principal-test
  "Who may make Knoxx believe a translation exists.

  The batch-status route is guarded by `org.translations.manage`, which org
  admins hold as well as system admins. That is correct for the route's original
  job — updating a batch's status in the worker queue — but it is not sufficient
  for the evidence-minting step added alongside it: an org admin could dispatch
  work and immediately report `completed_document` for it, producing a
  completed-translation receipt for a translation that never ran. That receipt
  is exactly what a publication gate waits on.

  `infra.routes.translation/next-batch-op` already closed the same gap for batch
  claiming by requiring a system-admin worker principal. These tests pin that the
  evidence step does the same."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.translation :as translation]))

(deftest only-a-system-admin-worker-principal-may-produce-evidence
  (testing "the worker identity is accepted"
    (is (translation/worker-principal? {:role-slugs ["system_admin"]}))
    (is (translation/worker-principal? {:role-slugs ["system-admin"]})))

  (testing "an org admin holding org.translations.manage is not the worker"
    ;; The whole finding: this principal can legitimately call the route, and
    ;; must still not be able to mint evidence.
    (is (not (translation/worker-principal?
              {:role-slugs ["org_admin"]
               :permissions ["org.translations.manage"]}))))

  (testing "a translator is not the worker either"
    (is (not (translation/worker-principal?
              {:role-slugs ["translator"]
               :permissions ["org.translations.review"]}))))

  (testing "an absent context is not the worker"
    ;; `with-request-context!` hands down nil when the policy database is
    ;; disabled. Read as permission, that would let an anonymous caller
    ;; manufacture the evidence a gate is waiting on.
    (is (not (translation/worker-principal? nil)))
    (is (not (translation/worker-principal? {})))))

(deftest ^:async a-non-worker-report-produces-no-evidence
  (testing "the report is skipped, and says why"
    (let [result (await (translation/resolve-translation-evidence!
                         {} {:role-slugs ["org_admin"]
                          :permissions ["org.translations.manage"]}
                         {:status "complete"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"}))]
      ;; Reported rather than silently dropped: an operator debugging a
      ;; translation that produced no receipt needs to see the reason.
      (is (= :worker-principal-required
             (:reason (:translation/skipped result))))))

  (testing "an anonymous report produces no evidence"
    (let [result (await (translation/resolve-translation-evidence!
                         {} nil
                         {:status "complete"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"}))]
      (is (= :worker-principal-required
             (:reason (:translation/skipped result))))))

  (testing "a worker principal gets past the check and finds no store configured"
    ;; Proves the refusal above is the principal check rather than the store
    ;; being absent: the same call with a worker principal reaches the next gate.
    (let [result (await (translation/resolve-translation-evidence!
                         {} {:role-slugs ["system_admin"]}
                         {:status "complete"
                          :batch_id "batch-1"
                          :completed_document "knoxx.docs/probe"}))]
      (is (= :translation-evidence-unavailable
             (:reason (:translation/skipped result)))))))
