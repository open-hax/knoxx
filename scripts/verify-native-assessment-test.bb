#!/usr/bin/env bb
;; SPDX-License-Identifier: GPL-3.0-or-later
;; Resident inert controls. No process, GitHub, model, credential or ref effects.
(ns user
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :as test]))

(declare api app-identity cleaned? cleanup-completion cleanup! options pages ref-readback resources stop-owned-runs! request!)
(load-file "scripts/verify-native-assessment.bb")

(def owned-id "12345678-1234-1234-1234-123456789abc")
(def owned-branch (str "codex/native-assessment-" owned-id))
(def owned-head (apply str (repeat 40 "a")))
(def owned-repo {:id 100 :full_name "open-hax/knoxx"})
(def owned-ref {:ref (str "refs/heads/" owned-branch) :node_id "owned-ref"
                :object {:type "commit" :sha owned-head}})
(def owned-pr {:id 200 :node_id "owned-pr" :number 300 :state "closed"
               :base {:repo owned-repo}
               :head {:repo owned-repo :ref owned-branch :sha owned-head}})
(def owned-resources {:repository "open-hax/knoxx" :repository-id 100
                      :invocation-id owned-id :branch owned-branch :branch-head owned-head
                      :branch-created? true :branch-ref-node-id "owned-ref" :fixture-seeded? true
                      :pr-number 300 :pr-id 200 :pr-node-id "owned-pr" :root-comment-id 601
                      :request-ids [500] :run-ids #{700} :workflow-id 800
                      :started-at "2026-10-12T00:00:00Z"})

(defn cleanup-case "Exercise actual cleanup with a native-shaped inert API and ordered operation custody." [controls]
  (reset! cleaned? false)
  (reset! resources (merge owned-resources {:delete-owned-branch? true} (:resources controls)))
  (let [calls (atom []) ref-present? (atom true)
        comments (atom [{:id 500 :user {:login "operator"}}
                        {:id 501 :user app-identity :body "Reply [github run](/open-hax/knoxx/actions/runs/700)"}
                        {:id 502 :user {:login "unrelated"}}
                        {:id 503 :user app-identity :body "Other request [github run](/open-hax/knoxx/actions/runs/701)"}])
        inline (atom [{:id 601} {:id 602}])
        native (fn [method endpoint & [_payload]]
                 (swap! calls conj [method endpoint])
                 (cond
                   (= method "PATCH") (do (when-let [before-closure (:before-closure controls)] (before-closure))
                                          (if (:closure-error controls) (throw (ex-info "inert closure refusal" {})) {:state "closed"}))
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx")) (or (:repo controls) owned-repo)
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx/pulls/300")) (or (:pr controls) owned-pr)
                   (and (= method "DELETE") (str/includes? endpoint "/issues/comments/"))
                   (do (when (:comment-error controls) (throw (ex-info "inert comment refusal" {})))
                       (when-not (:comments-remain controls)
                         (swap! comments (fn [rows] (filterv #(not= (:id %) (parse-long (last (str/split endpoint #"/")))) rows)))) nil)
                   (and (= method "DELETE") (str/includes? endpoint "/pulls/comments/"))
                   (do (swap! inline (fn [rows] (filterv #(not= 601 (:id %)) rows))) nil)
                   (and (= method "DELETE") (str/includes? endpoint "/git/refs/heads/"))
                   (do (when-not (:delete-leaves-ref controls) (reset! ref-present? false))
                       (when (:delete-throws controls) (throw (ex-info "inert lost DELETE response" {}))) nil)
                   :else (throw (ex-info (str "Unexpected inert API: " method " " endpoint) {}))))
        read-ref (fn [repository branch]
                   (swap! calls conj ["REF" repository branch])
                   (when (and (not @ref-present?) (:readback-error controls)) (throw (ex-info "inert failed readback" {})))
                   (if @ref-present? {:status 200 :record (or (:ref controls) owned-ref)} {:status 404 :record {:message "Not Found" :status "404"}}))
        output (with-redefs [cleanup-completion (promise)
                  api native
                  pages (fn [endpoint _key]
                          (swap! calls conj ["PAGE" endpoint])
                          (cond (str/includes? endpoint "/issues/") @comments
                                (str/includes? endpoint "/pulls/") @inline
                                :else (throw (ex-info "Unexpected inert page" {}))))
                  stop-owned-runs! (fn [] (swap! calls conj ["TERMINAL"])
                                     (when (:terminal-error controls) (throw (ex-info "inert active run" {}))) true)
                  ref-readback read-ref]
                  (with-out-str
                    (if-let [run-cleanup (:run-cleanup controls)]
                      (run-cleanup)
                      (do
                        (cleanup!)
                        ;; Shutdown/finally re-entry cannot retry a destructive operation.
                        (cleanup!)))))]
    {:calls @calls :resources @resources :comments @comments :inline @inline
     :ref-present? @ref-present? :output output}))

(defn ref-deletes "Find only exact fixture-ref DELETE intents." [result]
  (filterv #(and (= "DELETE" (first %)) (str/includes? (second %) "/git/refs/heads/")) (:calls result)))

(test/deftest retain-by-default-and-explicit-option
  (test/is (false? (:delete-owned-branch? (options []))))
  (test/is (true? (:delete-owned-branch? (options ["--delete-owned-branch" "--timeout-seconds" "60"]))))
  (test/is (thrown? Exception (options ["--delete-owned-branch" "--delete-owned-branch"])))
  (test/is (thrown? Exception (options ["--delete-any-branch"])))
  (let [result (cleanup-case {:resources {:delete-owned-branch? false}})]
    (test/is (empty? (ref-deletes result)))
    (test/is (:ref-present? result))
    (test/is (str/includes? (:output result) "RETAIN fixture branch:"))
    (test/is (not (:cleanup-failed? (:resources result))))
    (test/is (= [502 503] (mapv :id (:comments result))))
    (test/is (= [602] (mapv :id (:inline result))))))

(test/deftest guarded-delete-after-complete-cleanup
  (let [result (cleanup-case {}) calls (:calls result)
        deletion (.indexOf calls ["DELETE" (str "repos/open-hax/knoxx/git/refs/heads/" owned-branch)])
        terminal (.indexOf calls ["TERMINAL"])
        repo-guard (.indexOf calls ["GET" "repos/open-hax/knoxx"])
        pr-guard (.indexOf calls ["GET" "repos/open-hax/knoxx/pulls/300"])]
    (test/is (= 1 (count (ref-deletes result))))
    (test/is (< terminal repo-guard pr-guard deletion))
    (test/is (= ["REF" "open-hax/knoxx" owned-branch] (last calls)))
    (test/is (= :deleted (get-in result [:resources :branch-deletion-status])))
    (test/is (str/includes? (:output result) "DELETED fixture branch:"))
    (test/is (false? (:ref-present? result)))
    (test/is (not (get-in result [:resources :cleanup-failed?])))
    (test/is (= [502 503] (mapv :id (:comments result))))
    (test/is (= [602] (mapv :id (:inline result))))))

(test/deftest incomplete-cleanup-retains-with-zero-ref-deletes
  (doseq [controls [{:closure-error true} {:terminal-error true} {:comment-error true}
                    {:comments-remain true} {:resources {:fixture-seeded? false}}
                    {:resources {:pr-number nil}}]]
    (let [result (cleanup-case controls)]
      (test/is (empty? (ref-deletes result)) (str controls))
      (test/is (:ref-present? result) (str controls))
      (test/is (true? (get-in result [:resources :cleanup-failed?])) (str controls)))))

(test/deftest fresh-ownership-refusals-retain-with-zero-ref-deletes
  (doseq [controls [{:resources {:branch-created? false}} {:resources {:invocation-id "other"}}
                    {:repo (assoc owned-repo :id 999)} {:repo (assoc owned-repo :full_name "other/repo")}
                    {:pr (assoc owned-pr :state "open")} {:pr (assoc owned-pr :id 999)}
                    {:pr (assoc-in owned-pr [:head :repo :id] 999)}
                    {:pr (assoc-in owned-pr [:head :ref] "main")}
                    {:pr (assoc-in owned-pr [:head :sha] (apply str (repeat 40 "b")))}
                    {:ref (assoc owned-ref :ref "refs/heads/main")}
                    {:ref (assoc owned-ref :node_id "replaced-ref")}
                    {:ref (assoc-in owned-ref [:object :sha] (apply str (repeat 40 "b")))}]]
    (let [result (cleanup-case controls)]
      (test/is (empty? (ref-deletes result)) (str controls))
      (test/is (:ref-present? result) (str controls))
      (test/is (true? (get-in result [:resources :cleanup-failed?])) (str controls)))))

(test/deftest uncertain-deletion-never-retries-or-claims-retention
  (doseq [[controls expected-status expected-presence]
          [[{:delete-throws true} :absent-after-uncertain-delete false]
           [{:delete-leaves-ref true} :retained true]
           [{:readback-error true} :uncertain false]]]
    (let [result (cleanup-case controls)]
      (test/is (= 1 (count (ref-deletes result))))
      (test/is (= expected-status (get-in result [:resources :branch-deletion-status])))
      (test/is (= expected-presence (:ref-present? result)))
      (test/is (= (= expected-status :retained)
                  (str/includes? (:output result) "RETAIN fixture branch:")))
      (test/is (true? (get-in result [:resources :cleanup-failed?]))))))

(test/deftest native-ref-http-absence-contract
  (doseq [[response expected-status]
          [[{:exit 0 :out (str "HTTP/2.0 200 OK\r\nX-Test: inert\r\n\r\n" (json/generate-string owned-ref))} 200]
           [{:exit 1 :out "HTTP/2.0 404 Not Found\r\nX-Test: inert\r\n\r\n{\"message\":\"Not Found\",\"status\":\"404\"}"} 404]]]
    (with-redefs [process/process (fn [& _] (delay response))]
      (test/is (= expected-status (:status (ref-readback "open-hax/knoxx" owned-branch))))))
  (doseq [response [{:exit 1 :out ""} {:exit 1 :out "HTTP/2.0 403 Forbidden\r\n\r\n{}"}
                    {:exit 1 :out "HTTP/2.0 500 Failure\r\n\r\n{}"}
                    {:exit 0 :out "HTTP/2.0 404 Not Found\r\n\r\n{\"message\":\"Not Found\",\"status\":\"404\"}"}
                    {:exit 1 :out "HTTP/2.0 404 Not Found\r\n\r\n{\"message\":\"unknown\",\"status\":\"404\"}"}]]
    (with-redefs [process/process (fn [& _] (delay response))]
      (test/is (thrown? Exception (ref-readback "open-hax/knoxx" owned-branch))))))

(test/deftest concurrent-cleanup-waits-for-success-and-failure
  ;; Block the winning caller inside the actual closure effect. Observe the
  ;; other caller's real losing CAS before checking that it has not returned.
  (doseq [closure-error? [false true]]
    (let [entered (promise) release (promise) lost-claim (promise)
          winner-result (promise) loser-result (promise)
          invoke (fn [result]
                   (deliver result (try {:value (cleanup!)}
                                        (catch Throwable error {:error error}))))
          result (cleanup-case
                  {:closure-error closure-error?
                   :before-closure (fn []
                                     (deliver entered true)
                                     (require! (= true (deref release 5000 ::timeout))
                                               "Inert cleanup release timed out"))
                   :run-cleanup
                   (fn []
                     (let [actual-cas compare-and-set!
                           winner (Thread. ^Runnable #(invoke winner-result) "inert-cleanup-winner")
                           loser (Thread. ^Runnable #(invoke loser-result) "inert-cleanup-loser")]
                       ;; These are private test threads, never live shutdown hooks.
                       (.setDaemon winner true)
                       (.setDaemon loser true)
                       (with-redefs [compare-and-set!
                                     (fn [reference old-value new-value]
                                       (let [claimed? (actual-cas reference old-value new-value)]
                                         (when (and (identical? reference cleaned?) (not claimed?))
                                           (deliver lost-claim true))
                                         claimed?))]
                         (try
                           (.start winner)
                           (test/is (= true (deref entered 5000 ::timeout)))
                           (.start loser)
                           (test/is (= true (deref lost-claim 5000 ::timeout)))
                           (test/is (= ::still-running (deref winner-result 100 ::still-running)))
                           (test/is (= ::still-running (deref loser-result 100 ::still-running)))
                           (finally
                             (deliver release true)
                             (.join winner 5000)
                             (.join loser 5000)))
                         (test/is (not (.isAlive winner)))
                         (test/is (not (.isAlive loser)))
                         (test/is (= {:value (not closure-error?)} (deref winner-result 1000 ::timeout)))
                         (test/is (= (deref winner-result 1000 ::timeout) (deref loser-result 1000 ::timeout)))
                         ;; A later re-entry sees the same completed result without
                         ;; repeating the existing destructive cleanup operations.
                         (test/is (= {:value (cleanup!)} (deref winner-result 1000 ::timeout))))))})
          calls (:calls result)
          deletes (filter #(= "DELETE" (first %)) calls)]
      (test/is (= 1 (count (filter #(= "PATCH" (first %)) calls))))
      (test/is (= closure-error? (true? (get-in result [:resources :cleanup-failed?]))))
      (test/is (every? #(= 1 %) (vals (frequencies deletes))))
      (test/is (= (if closure-error? 0 1) (count (ref-deletes result)))))))

(def request-user {:login "inert-operator" :id 400 :node_id "inert-user" :type "User"})

(defn request-cleanup-case "Run actual request!/cleanup! with inert accepted/lost-response native records." [controls]
  (reset! cleaned? false)
  (reset! resources (merge owned-resources {:request-ids [] :request-intents [] :run-ids #{}
                                           :request-user request-user :delete-owned-branch? false}
                          (:resources controls)))
  (let [calls (atom []) comments (atom []) inline (atom [{:id 601} {:id 602}])
        closed? (atom false) ref-present? (atom true) post-state (atom nil) issue-reads (atom 0)
        run (atom {:id 700 :display_title "Native assessment 900" :status "in_progress"})
        native (fn [method endpoint & [payload]]
                 (swap! calls conj [method endpoint])
                 (cond
                   (and (= method "POST") (= endpoint "repos/open-hax/knoxx/issues/300/comments"))
                   (do
                     (reset! post-state @resources)
                     (when-not (:absent controls)
                       (let [now (str (java.time.Instant/now))
                             original-record (merge {:id 900 :node_id "inert-request" :body (:body payload) :user request-user
                                                     :created_at now :updated_at now
                                                     :issue_url "https://api.github.com/repos/open-hax/knoxx/issues/300"
                                                     :url "https://api.github.com/repos/open-hax/knoxx/issues/comments/900"
                                                     :html_url "https://github.com/open-hax/knoxx/pull/300#issuecomment-900"}
                                                    (:record controls))
                             record (if (:edited controls) (update original-record :body str "\nchanged after POST") original-record)]
                         (reset! comments (cond-> [record
                                                 {:id 901 :user app-identity :body "Reply [github run](/open-hax/knoxx/actions/runs/700)"}
                                                 {:id 910 :user {:login "foreign"} :body "Unrelated"}
                                                 {:id 911 :user app-identity :body "Foreign [github run](/open-hax/knoxx/actions/runs/701)"}]
                                          (:duplicate controls) (conj (assoc record :id 902))))))
                     (if (:acknowledged controls) (first @comments)
                         (throw (ex-info "Inert response lost after the sole POST" {}))))
                   (= method "PATCH") (do (reset! closed? true) {:state "closed"})
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx")) owned-repo
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx/pulls/300"))
                   (assoc owned-pr :state (if @closed? "closed" "open"))
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx/issues/comments/900"))
                   (if (:get-error controls) (throw (ex-info "Inert native GET failure" {}))
                       (merge (first @comments) (:get-record controls)))
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx/actions/runs/700")) @run
                   (and (= method "POST") (= endpoint "repos/open-hax/knoxx/actions/runs/700/cancel"))
                   (do (swap! run assoc :status "completed") nil)
                   (and (= method "DELETE") (str/includes? endpoint "/issues/comments/"))
                   (do (swap! comments (fn [rows] (filterv #(not= (:id %) (parse-long (last (str/split endpoint #"/")))) rows))) nil)
                   (and (= method "DELETE") (= endpoint "repos/open-hax/knoxx/pulls/comments/601"))
                   (do (swap! inline (fn [rows] (filterv #(not= 601 (:id %)) rows))) nil)
                   (and (= method "DELETE") (str/includes? endpoint "/git/refs/heads/"))
                   (do (reset! ref-present? false) nil)
                   :else (throw (ex-info (str "Unexpected inert request API " method " " endpoint) {}))))
        pager (fn [endpoint key]
                (swap! calls conj ["PAGE" endpoint])
                (cond
                  (str/includes? endpoint "/issues/300/comments")
                  (do (swap! issue-reads inc)
                      (cond (:list-error controls) (throw (ex-info "Inert native list failure" {}))
                            (and (:initially-absent controls) (= 1 @issue-reads)) []
                            :else @comments))
                  (= key :workflow_runs) [@run {:id 701 :display_title "Native assessment 910" :status "in_progress"}]
                  (str/includes? endpoint "/pulls/300/comments") @inline
                  :else (throw (ex-info "Unexpected inert request page" {}))))
        observation (with-redefs [cleanup-completion (promise) api native pages pager
                                 ref-readback (fn [& _] (if @ref-present? {:status 200 :record owned-ref}
                                                             {:status 404 :record {:message "Not Found" :status "404"}}))]
                      (let [request-result (try {:value (request! "open-hax/knoxx" 300 "/opencode assess the inert source")}
                                                (catch Exception error {:refusal (:code (ex-data error))
                                                                        :message (.getMessage error)}))
                            cleanup-results (with-out-str
                                              (cleanup!)
                                              (cleanup!))]
                        {:request-result request-result :output cleanup-results}))]
    (merge observation {:calls @calls :resources @resources :comments @comments :inline @inline
                        :post-state @post-state :ref-present? @ref-present?})))

(test/deftest lost-request-response-reconciles-before-owned-cleanup
  (doseq [controls [{:acknowledged true} {} {:initially-absent true}]]
    (let [result (request-cleanup-case controls) calls (:calls result)
          intents (get-in result [:post-state :request-intents])
          intent (first intents)
          request-posts (filter #(= ["POST" "repos/open-hax/knoxx/issues/300/comments"] %) calls)
          cancels (filter #(= ["POST" "repos/open-hax/knoxx/actions/runs/700/cancel"] %) calls)
          deletes (filter #(= "DELETE" (first %)) calls)]
      (test/is (= 1 (count request-posts)))
      (test/is (= 1 (count intents)))
      (test/is (= request-user (:user intent)))
      (test/is (and (string? (:body intent))
                    (str/includes? (:body intent) (str "invocation=" owned-id " request="))))
      (test/is (and (string? (:body intent)) (string? (:marker intent))
                    (str/ends-with? (:body intent) (:marker intent))))
      (test/is (and (string? (:marker intent))
                    (boolean (re-find #"request=[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12} -->$" (:marker intent)))))
      (if (:initially-absent controls)
        (test/is (= :request-comment-absent (get-in result [:request-result :refusal])))
        (test/is (= 900 (get-in result [:request-result :value :id]))))
      (test/is (= [900] (get-in result [:resources :request-ids])))
      (test/is (= :reconciled (get-in result [:resources :request-intents 0 :status])))
      (test/is (= #{700} (get-in result [:resources :run-ids])))
      (test/is (= 1 (count cancels)))
      (test/is (= [["DELETE" "repos/open-hax/knoxx/issues/comments/900"]
                   ["DELETE" "repos/open-hax/knoxx/issues/comments/901"]
                   ["DELETE" "repos/open-hax/knoxx/pulls/comments/601"]] (vec deletes)))
      (test/is (not (get-in result [:resources :cleanup-failed?])))
      (test/is (:ref-present? result))
      (test/is (= [910 911] (mapv :id (:comments result))))
      (test/is (= [602] (mapv :id (:inline result))))
      (when (:initially-absent controls)
        (let [index-after (fn [wanted after-index]
                            (or (first (keep-indexed (fn [index call]
                                                       (when (and (< after-index index) (= wanted call)) index))
                                                     calls))
                                -1))
              closure (index-after ["PATCH" "repos/open-hax/knoxx/pulls/300"] -1)
              reconciliation (index-after ["GET" "repos/open-hax/knoxx/issues/comments/900"] closure)
              discovery (or (first (keep-indexed #(when (and (= "PAGE" (first %2))
                                                           (str/includes? (second %2) "/actions/workflows/")) %1) calls))
                            -1)
              found? (fn [index] (and (integer? index) (<= 0 index)))]
          (test/is (found? closure))
          (test/is (found? reconciliation))
          (test/is (found? discovery))
          (test/is (and (found? closure) (found? reconciliation) (< closure reconciliation)))
          (test/is (and (found? reconciliation) (found? discovery) (< reconciliation discovery))))))))

(test/deftest request-reconciliation-refusals-retain-with-zero-deletes
  (doseq [[controls expected]
          [[{:absent true} :request-comment-absent]
           [{:duplicate true} :request-comment-duplicate]
           [{:record {:user (assoc request-user :id 999)}} :request-comment-user]
           [{:record {:issue_url "https://api.github.com/repos/other/repo/issues/300"}} :request-comment-body-context]
           [{:edited true} :request-comment-body-context]
           [{:record {:created_at "2000-01-01T00:00:00Z"}} :request-comment-timestamp]
           [{:record {:id 0}} :request-comment-id]
           [{:get-record {:body "changed native GET"}} :request-comment-readback]
           [{:get-error true} :request-native-readback]
           [{:list-error true} :request-native-readback]]]
    (let [result (request-cleanup-case (assoc controls :resources {:delete-owned-branch? true}))
          calls (:calls result)]
      (test/is (= expected (get-in result [:request-result :refusal])) (str controls))
      (test/is (= 1 (count (filter #(= ["POST" "repos/open-hax/knoxx/issues/300/comments"] %) calls))))
      (test/is (empty? (filter #(= "DELETE" (first %)) calls)))
      (test/is (empty? (filter #(str/includes? (second %) "/actions/") calls)))
      (test/is (empty? (get-in result [:resources :request-ids])))
      (test/is (true? (get-in result [:resources :cleanup-failed?])))
      (test/is (:ref-present? result)))))

(test/deftest malformed-request-ownership-refuses-before-post
  (doseq [invalid [{:request-user (assoc request-user :type "Bot")}
                   {:request-user (assoc request-user :node_id "")}
                   {:invocation-id "not-the-owned-invocation"}
                   {:pr-id 0} {:repository "other/repo"}]]
    (let [result (request-cleanup-case {:resources invalid})]
      (test/is (= :request-owned-intent (get-in result [:request-result :refusal])))
      (test/is (nil? (:post-state result)))
      (test/is (empty? (filter #(= "POST" (first %)) (:calls result)))))))

(let [{:keys [fail error]} (test/run-tests 'user)]
  (when (pos? (+ fail error)) (System/exit 1)))
