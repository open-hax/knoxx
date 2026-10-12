#!/usr/bin/env bb
;; SPDX-License-Identifier: GPL-3.0-or-later
;; Resident inert controls. No process, GitHub, model, credential or ref effects.
(ns user
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :as test]))

(declare api app-identity app-responses checks cleaned? cleanup-completion cleanup! disclosure options pages positive! protected-source! ref-readback resources seed! stop-owned-runs! request! wait-run!)
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

(def protected-helper-text (slurp ".github/scripts/native_public_fetch.js"))

(defn protected-helper-case "Exercise actual protected-source! with immutable native files and no native writes." [controls]
  (let [calls (atom []) workflow "inert exact workflow bytes"
        helper (str protected-helper-text (when (:local-changed controls) "\nchanged local"))
        encoded (fn [text] (.encodeToString (java.util.Base64/getEncoder)
                                           (.getBytes text java.nio.charset.StandardCharsets/UTF_8)))
        native (fn [method endpoint & [_payload]]
                 (swap! calls conj [method endpoint])
                 (cond
                   (= endpoint "repos/open-hax/knoxx") (assoc owned-repo :default_branch "main")
                   (= endpoint "repos/open-hax/knoxx/branches/main") {:protected true :commit {:sha owned-head}}
                   (= endpoint (str "repos/open-hax/knoxx/contents/.github/workflows/opencode-native-assessment.yml?ref=" owned-head))
                   {:type "file" :encoding "base64" :content (encoded workflow)}
                   (= endpoint (str "repos/open-hax/knoxx/contents/.github/scripts/native_public_fetch.js?ref=" owned-head))
                   (if (:remote-missing controls) (throw (ex-info "Inert helper unavailable" {}))
                       (merge {:type "file" :encoding "base64"
                               :content (encoded (str protected-helper-text (when (:remote-changed controls) "\nchanged remote")))}
                              (:helper-record controls)))
                   :else (throw (ex-info (str "Unexpected inert protected read " endpoint) {}))))
        local (fn [path]
                (case path
                  ".github/workflows/opencode-native-assessment.yml" workflow
                  ".github/scripts/native_public_fetch.js" (if (:local-missing controls)
                                                               (throw (ex-info "Inert local helper unavailable" {})) helper)
                  (throw (ex-info "Unexpected local protected read" {}))))
        outcome (with-redefs [api native slurp local checks (atom 0)]
                  (try {:value (protected-source! "open-hax/knoxx")}
                       (catch Exception error {:error (.getMessage error)})))]
    {:outcome outcome :calls @calls}))

(test/deftest protected-helper-precondition-refuses-before-fixtures
  (let [result (protected-helper-case {})]
    (test/is (= {:repository-id 100 :branch "main" :head owned-head} (get-in result [:outcome :value])))
    (test/is (some #(= ["GET" (str "repos/open-hax/knoxx/contents/.github/scripts/native_public_fetch.js?ref=" owned-head)] %)
                   (:calls result))))
  (doseq [controls [{:remote-missing true} {:local-missing true} {:remote-changed true}
                    {:local-changed true} {:local-changed true :remote-changed true}
                    {:helper-record {:type "dir"}} {:helper-record {:encoding "none"}}
                    {:helper-record {:content nil}}]]
    (let [result (protected-helper-case controls)]
      (test/is (string? (get-in result [:outcome :error])) (str controls))
      (test/is (nil? (get-in result [:outcome :value])))
      (test/is (every? #(= "GET" (first %)) (:calls result))))))

(def positive-fixture {:pr {:number 300} :head owned-head :file "docs/verification/.native-assessment/inert/source.txt"
                       :source-marker "source-marker-not-in-request" :root {:url "https://api.github.com/repos/open-hax/knoxx/pulls/comments/601"}})
(def positive-source-url (str "https://raw.githubusercontent.com/open-hax/knoxx/" owned-head "/" (:file positive-fixture)))

(defn positive-coordinate-case "Observe actual positive! against inert native replies and capture its real request." [reply]
  (let [body (atom nil)
        outcome (with-redefs [checks (atom 0)
                             request! (fn [_repo _number text] (reset! body text) {:id 500})
                             wait-run! (fn [& _] {:run {:id 700 :conclusion "success"}
                                                :job {:steps [{:name "Request a native App assessment" :conclusion "success"}]}})
                             app-responses (fn [& _] [{:body reply :html_url "https://github.com/open-hax/knoxx/pull/300#issuecomment-501"}])]
                  (try (positive! "open-hax/knoxx" {} {} positive-fixture 60) {:success true}
                       (catch Exception error {:error (.getMessage error)})))]
    {:outcome outcome :request @body}))

(test/deftest positive-request-does-not-supply-the-source-pin
  (let [reply (str (:source-marker positive-fixture) "\n" positive-source-url "\n" disclosure)
        result (positive-coordinate-case reply)]
    (test/is (true? (get-in result [:outcome :success])))
    (test/is (str/includes? (:request result) (:file positive-fixture)))
    (test/is (not (str/includes? (:request result) owned-head)))
    (test/is (not (str/includes? (:request result) "https://raw.githubusercontent.com/")))
    (test/is (not (str/includes? (:request result) (:source-marker positive-fixture))))))

(test/deftest positive-observation-requires-exact-immutable-citation
  (doseq [url [nil (str/replace positive-source-url owned-head (apply str (repeat 40 "b")))
               (str/replace positive-source-url "open-hax/knoxx" "other/repo")]]
    (let [result (positive-coordinate-case (str (:source-marker positive-fixture) "\n" url "\n" disclosure))]
      (test/is (string? (get-in result [:outcome :error])) (str url))
      (test/is (not (get-in result [:outcome :success]))))))

(defn fixture-pr-case "Exercise actual seed!/cleanup! with accepted or uncertain PR creation and exact inert native custody." [controls]
  (reset! cleaned? false)
  (reset! resources {:workflow-id 800 :request-user request-user :delete-owned-branch? false})
  (let [calls (atom []) records (atom []) inline (atom []) post-state (atom nil) list-reads (atom 0)
        base {:repository-id 100 :branch "main" :head owned-head}
        native (fn [method endpoint & [payload]]
                 (swap! calls conj [method endpoint])
                 (cond
                   (and (= method "POST") (= endpoint "repos/open-hax/knoxx/git/refs"))
                   {:ref (:ref payload) :node_id "inert-created-ref" :object {:type "commit" :sha owned-head}}
                   (= method "PUT") {:commit {:sha owned-head}}
                   (and (= method "POST") (= endpoint "repos/open-hax/knoxx/pulls"))
                   (let [now (str (java.time.Instant/now))]
                     (reset! post-state @resources)
                     (when-not (:absent controls)
                       (let [record (merge {:id 200 :node_id "owned-pr" :number 300 :state "open" :draft true
                                            :title (:title payload) :body (:body payload) :user request-user
                                            :created_at now :updated_at now
                                            :url "https://api.github.com/repos/open-hax/knoxx/pulls/300"
                                            :html_url "https://github.com/open-hax/knoxx/pull/300"
                                            :head {:repo owned-repo :ref (:head payload) :sha owned-head}
                                            :base {:repo owned-repo :ref "main" :sha owned-head}}
                                           (:record controls))]
                         (reset! records (cond-> [record]
                                           (:duplicate controls) (conj (assoc record :id 201 :number 301))))))
                     (if (:acknowledged controls) (first @records)
                         (throw (ex-info "Inert response lost after accepted PR POST" {}))))
                   (and (= method "POST") (= endpoint "repos/open-hax/knoxx/pulls/300/comments"))
                   (do (when-let [before-inline (:before-inline controls)] (before-inline))
                       (reset! inline [{:id 601} {:id 602}]) {:id 601})
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx")) owned-repo
                   (and (= method "GET") (= endpoint "repos/open-hax/knoxx/pulls/300"))
                   (if (:get-error controls) (throw (ex-info "Inert fixture PR GET failure" {}))
                       (merge (first @records) (:get-record controls)))
                   (and (= method "PATCH") (= endpoint "repos/open-hax/knoxx/pulls/300"))
                   (do (swap! records #(mapv (fn [pr] (assoc pr :state "closed")) %)) {:state "closed"})
                   (and (= method "DELETE") (= endpoint "repos/open-hax/knoxx/pulls/comments/601"))
                   (do (swap! inline #(filterv (fn [comment] (not= 601 (:id comment))) %)) nil)
                   :else (throw (ex-info (str "Unexpected inert fixture API " method " " endpoint) {}))))
        pager (fn [endpoint _key]
                (swap! calls conj ["PAGE" endpoint])
                (cond
                  (str/includes? endpoint "/pulls?state=all&head=")
                  (do (swap! list-reads inc)
                      (cond (:list-error controls) (throw (ex-info "Inert fixture PR listing failure" {}))
                            (and (:initially-absent controls) (= 1 @list-reads)) []
                            :else @records))
                  (str/includes? endpoint "/issues/300/comments") [{:id 910 :user {:login "foreign"}}]
                  (str/includes? endpoint "/pulls/300/comments") @inline
                  :else (throw (ex-info "Unexpected inert fixture page" {}))))
        outcome (with-redefs [cleanup-completion (promise) checks (atom 0) api native pages pager
                             stop-owned-runs! (fn [] (swap! calls conj ["TERMINAL"]) true)]
                  (if-let [run-seed-and-cleanup (:run-seed-and-cleanup controls)]
                    (run-seed-and-cleanup #(seed! "open-hax/knoxx" base) calls)
                    (let [seed-result (try {:value (seed! "open-hax/knoxx" base)}
                                           (catch Exception error {:refusal (:code (ex-data error)) :message (.getMessage error)}))
                          cleanup-output (let [writer (java.io.StringWriter.)]
                                           (binding [*out* writer *err* writer] (cleanup!) (cleanup!))
                                           (str writer))]
                      {:seed-result seed-result :cleanup-output cleanup-output})))]
    (merge outcome {:calls @calls :resources @resources :records @records :inline @inline :post-state @post-state})))

(test/deftest accepted-pr-response-loss-recovers-the-owned-draft
  (doseq [controls [{:acknowledged true} {} {:initially-absent true}]]
    (let [result (fixture-pr-case controls) calls (:calls result)
          intent (get-in result [:post-state :pr-create-intent])
          index-of (fn [wanted] (or (first (keep-indexed #(when (= wanted %2) %1) calls)) -1))
          pr-read (index-of ["GET" "repos/open-hax/knoxx/pulls/300"])
          closure (index-of ["PATCH" "repos/open-hax/knoxx/pulls/300"])]
      (test/is (= 1 (count (filter #(= ["POST" "repos/open-hax/knoxx/pulls"] %) calls))))
      (test/is (= request-user (:user intent)))
      (test/is (and (string? (:marker intent)) (string? (:body intent))
                    (str/ends-with? (:body intent) (:marker intent))
                    (str/includes? (:marker intent) (str "fixture=" (get-in result [:post-state :invocation-id])))))
      (test/is (= :pending (:status intent)))
      (test/is (= 300 (get-in result [:resources :pr-number])))
      (test/is (= 200 (get-in result [:resources :pr-id])))
      (test/is (= "owned-pr" (get-in result [:resources :pr-node-id])))
      (test/is (= :reconciled (get-in result [:resources :pr-create-intent :status])))
      (test/is (= (if (:acknowledged controls) :acknowledged :uncertain)
                  (get-in result [:resources :pr-create-intent :post-outcome])))
      (test/is (and (<= 0 pr-read) (< pr-read closure)))
      (test/is (= 1 (count (filter #(= ["PATCH" "repos/open-hax/knoxx/pulls/300"] %) calls))))
      (test/is (= "closed" (:state (first (:records result)))))
      (test/is (not (get-in result [:resources :cleanup-failed?])))
      (test/is (empty? (filter #(and (string? (second %)) (str/includes? (second %) "/git/refs/heads/")) calls)))
      (test/is (str/includes? (:cleanup-output result) "RETAIN fixture branch:"))
      (if (:initially-absent controls)
        (do (test/is (= :fixture-pr-absent (get-in result [:seed-result :refusal])))
            (test/is (empty? (:inline result))))
        (do (test/is (get-in result [:seed-result :value]))
            (test/is (= [602] (mapv :id (:inline result)))))))))

(test/deftest uncertain-pr-custody-refusals-never-close-a-guessed-pr
  (doseq [[controls expected]
          [[{:absent true} :fixture-pr-absent]
           [{:duplicate true} :fixture-pr-duplicate]
           [{:record {:user (assoc request-user :id 999)}} :fixture-pr-user]
           [{:record {:body "changed after POST"}} :fixture-pr-absent]
           [{:record {:title "changed title"}} :fixture-pr-content-context]
           [{:record {:draft false}} :fixture-pr-content-context]
           [{:record {:head {:repo owned-repo :ref "main" :sha owned-head}}} :fixture-pr-content-context]
           [{:record {:base {:repo (assoc owned-repo :id 999) :ref "main" :sha owned-head}}} :fixture-pr-content-context]
           [{:record {:created_at "2000-01-01T00:00:00Z"}} :fixture-pr-timestamp]
           [{:record {:id 0}} :fixture-pr-id]
           [{:get-record {:title "changed native GET"}} :fixture-pr-readback]
           [{:get-error true} :fixture-pr-native-readback]
           [{:list-error true} :fixture-pr-native-readback]]]
    (let [result (fixture-pr-case controls) calls (:calls result)]
      (test/is (= expected (get-in result [:seed-result :refusal])) (str controls))
      (test/is (= 1 (count (filter #(= ["POST" "repos/open-hax/knoxx/pulls"] %) calls))))
      (test/is (nil? (get-in result [:resources :pr-number])))
      (test/is (true? (get-in result [:resources :cleanup-failed?])))
      (test/is (empty? (filter #(#{"PATCH" "DELETE" "TERMINAL"} (first %)) calls)))
      (test/is (str/includes? (:cleanup-output result) "pending PR marker"))
      (test/is (str/includes? (:cleanup-output result) "RETAIN fixture branch:")))))

(test/deftest cleanup-waits-for-the-last-seed-write
  (let [entered (promise) release (promise) claimed (promise)
        seed-result (promise) cleanup-result (promise)
        result (fixture-pr-case
                {:acknowledged true
                 :before-inline (fn []
                                  (deliver entered true)
                                  (require! (= true (deref release 5000 ::timeout)) "Inert seed release timed out"))
                 :run-seed-and-cleanup
                 (fn [seed calls]
                   (let [seed-thread (Thread. ^Runnable
                                              (fn [] (deliver seed-result
                                                              (try {:value (seed)}
                                                                   (catch Throwable error {:error error})))))
                         cleanup-thread (Thread. ^Runnable
                                                 (fn [] (deliver cleanup-result
                                                                 (try {:value (cleanup!)}
                                                                      (catch Throwable error {:error error})))))
                         watch-key ::seed-cleanup-claim]
                     (add-watch cleaned? watch-key
                                (fn [_key _reference before after]
                                  (when (and (false? before) (true? after)) (deliver claimed true))))
                     (try
                       (.start seed-thread)
                       (test/is (= true (deref entered 5000 ::timeout)))
                       (.start cleanup-thread)
                       (test/is (= true (deref claimed 5000 ::timeout)))
                       (test/is (= ::still-running (deref cleanup-result 100 ::still-running)))
                       (test/is (let [finished (:seed-finished @resources)]
                                  (and finished (not (realized? finished)))))
                       (test/is (empty? (filter #(#{"PATCH" "DELETE" "TERMINAL"} (first %)) @calls)))
                       (finally
                         (deliver release true)
                         (.join seed-thread 5000)
                         (.join cleanup-thread 5000)
                         (remove-watch cleaned? watch-key)))
                     (test/is (not (.isAlive seed-thread)))
                     (test/is (not (.isAlive cleanup-thread)))
                     (test/is (get-in (deref seed-result 1000 ::timeout) [:value :pr]))
                     (test/is (= {:value true} (deref cleanup-result 1000 ::timeout)))
                     {:seed-result (deref seed-result 1000 ::timeout) :cleanup-output ""}))})]
    (test/is (= "closed" (:state (first (:records result)))))
    (test/is (= [602] (mapv :id (:inline result))))
    (test/is (not (get-in result [:resources :cleanup-failed?])))
    (test/is (= 1 (count (filter #(= ["PATCH" "repos/open-hax/knoxx/pulls/300"] %) (:calls result)))))
    (test/is (= 1 (count (filter #(= ["DELETE" "repos/open-hax/knoxx/pulls/comments/601"] %) (:calls result)))))))

(let [{:keys [fail error]} (test/run-tests 'user)]
  (when (pos? (+ fail error)) (System/exit 1)))
