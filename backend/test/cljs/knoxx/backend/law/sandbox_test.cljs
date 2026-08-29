(ns knoxx.backend.law.sandbox-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.sandbox :as sandbox-law]))

(deftest exact-git-safe-directory-law-test
  (testing "one exact absolute workdir is accepted and normalized"
    (is (= "/workspace"
           (sandbox-law/exact-git-safe-directory "  /workspace  "))))

  (testing "broad or ambiguous trust scopes fail closed"
    (doseq [workdir [nil
                     ""
                     "workspace"
                     "/"
                     "/workspace/*"
                     "*"
                     "/workspace/../repo"
                     "/workspace/./repo"
                     "/workspace//repo"
                     "/workspace/"]]
      (is (thrown? js/Error
                   (sandbox-law/exact-git-safe-directory workdir))
          (str "expected rejection for " (pr-str workdir))))))

(deftest internal-command-failure-law-test
  (testing "successful structured results pass through the classifier"
    (is (nil? (sandbox-law/internal-command-failure
               "sandbox read"
               {:ok true :exitCode 0 :stdout "content"}))))

  (testing "nonzero commands retain bounded exit and stderr evidence"
    (is (= {:operation "sandbox write"
            :exit-code 17
            :detail "permission denied"
            :message "sandbox write failed (exit 17): permission denied"}
           (sandbox-law/internal-command-failure
            "sandbox write"
            {:ok false :exitCode 17 :stderr "permission denied"}))))

  (testing "spawn errors are evidence when stderr is absent"
    (is (= "sandbox commit failed (exit ENOENT): docker was not found"
           (:message
            (sandbox-law/internal-command-failure
             "sandbox commit"
             {:ok false :exitCode "ENOENT" :error "docker was not found"})))))

  (testing "diagnostics cannot grow an unbounded tool error"
    (let [failure (sandbox-law/internal-command-failure
                   "sandbox read"
                   {:ok false :exitCode 1 :stderr (apply str (repeat 3000 "x"))})]
      (is (= sandbox-law/max-internal-command-detail-chars
             (count (:detail failure)))))))
