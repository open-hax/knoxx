(ns knoxx.backend.contracts-routes-test
  (:require ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.contracts :as contract-routes]
            [knoxx.backend.infra.routes.resources :as resource-routes]))

(def fixture-config
  {:contracts-dir "test/fixtures/contracts"})

(defn- capture-json
  []
  (let [captured (atom nil)]
    {:captured captured
     :send! (fn [_status body] (reset! captured body))}))

;; ---------------------------------------------------------------------------
;; Invariant: resource index sync never crashes startup; it resolves to a
;; status map. The old sync-contract-index! name remains as a compatibility
;; alias.
;; ---------------------------------------------------------------------------

(deftest ^:async sync-returns-promise-and-status-map
  (testing "sync-resource-index! returns a Promise resolving to a status map"
    (let [p (resource-routes/sync-resource-index! fixture-config)]
      (is (instance? js/Promise p) "must be a Promise")
      (let [result (await p)]
        (is (true? (:ok result)))
        (is (number? (:count result)))))))

(deftest ^:async sync-alias-does-not-throw
  (testing "sync-contract-index! compatibility alias resolves instead of rejecting"
    (try
      (let [result (await (contract-routes/sync-contract-index! fixture-config))]
        (is (map? result) "resolves to a plain map"))
      (catch :default err
        (is false (str "must not reject: " (.-message err)))))))

;; ---------------------------------------------------------------------------
;; Resource route handlers list resources; compatibility contract handlers still
;; return the old {:contracts [...]} shape for existing clients.
;; ---------------------------------------------------------------------------

(deftest ^:async list-resources-handler-returns-resource-shape
  (let [{:keys [captured send!]} (capture-json)]
    (await (resource-routes/handle-list-resources send! fixture-config "agents"))
    (is (seq (:resources @captured)) "expected resources")
    (is (every? #(= "agents" (:resourceClass %)) (:resources @captured))
        "all returned resources must be agents")))

(deftest list-contracts-handler-returns-promise
  (testing "/api/admin/contracts handler produces a Promise"
    (let [{:keys [send!]} (capture-json)
          p (contract-routes/handle-list-contracts send! fixture-config nil)]
      (is (instance? js/Promise p)))))

(deftest ^:async list-contracts-handler-returns-non-empty
  (testing "/api/admin/contracts body has at least one contract"
    (let [{:keys [captured send!]} (capture-json)]
      (await (contract-routes/handle-list-contracts send! fixture-config nil))
      (is (seq (:contracts @captured))
          (str "expected contracts, got: " (pr-str @captured))))))

(deftest ^:async list-contracts-handler-all-have-id-and-class
  (let [{:keys [captured send!]} (capture-json)]
    (await (contract-routes/handle-list-contracts send! fixture-config nil))
    (doseq [c (:contracts @captured)]
      (is (string? (:id c)) (str "missing :id " (pr-str c)))
      (is (string? (:contractClass c))
          (str "missing :contractClass " (pr-str c))))))

(deftest ^:async list-contracts-handler-class-filter
  (let [{:keys [captured send!]} (capture-json)]
    (await (contract-routes/handle-list-contracts send! fixture-config "agents"))
    (is (seq (:contracts @captured)) "expected agents")
    (is (every? #(= "agents" (:contractClass %)) (:contracts @captured))
        "all returned contracts must be class agents")))

(deftest validate-contract-edn-surfaces-agent-shape-warnings
  (let [result (contract-routes/validate-contract-edn
                "agents"
                "{:contract/id \"warny\"
                  :contract/kind :agent
                  :trigger-kind :cron
                  :source {:max-messages 2000}
                  :agent {:role :contract_writer}
                  :prompts {:task \"update :data/world_state\"}
                  :data {:filter {:publishChannels [\"123\"]}
                         :source {:max-messages 2000}
                         :plot_log []}}")
        messages (set (map :message (:warnings result)))]
    (is (:ok result))
    (is (some #(str/includes? % ":data/:filter") messages))
    (is (some #(str/includes? % "top-level :source") messages))
    (is (some #(str/includes? % "clamped to 100") messages))
    (is (some #(str/includes? % "mutable runtime state") messages))
    (is (some #(str/includes? % "Role refs should use") messages))
    (is (some #(str/includes? % "Prompt references mutable :data") messages))))

(deftest saved-publication-resources-select-the-affected-document
  (is (= :knoxx.docs/entered
         (resource-routes/admission-document-id
          "documents" {:document/id :knoxx.docs/entered})))
  (is (= :knoxx.docs/entered
         (resource-routes/admission-document-id
          "publications" {:publication/document :knoxx.docs/entered})))
  (is (nil? (resource-routes/admission-document-id
             "agents" {:contract/id "unrelated"}))))

(deftest publication-resource-copy-rewrites-the-semantic-identity
  (is (str/includes?
       (resource-routes/update-resource-id-in-edn-text
        "documents"
        "{:document/id :open-hax.documents/source-doc\n :document/title \"Source\"}"
        "copied_doc")
       ":document/id :open-hax.documents/copied-doc"))
  (is (str/includes?
       (resource-routes/update-resource-id-in-edn-text
        "gardens"
        "{:garden/id :open-hax.gardens/source-garden\n :garden/title \"Source\"}"
        "copied_garden")
       ":garden/id :open-hax.gardens/copied-garden"))
  (is (str/includes?
       (resource-routes/update-resource-id-in-edn-text
        "publications"
        "{:publication/id :open-hax.publications/source-es\n :publication/document :open-hax.documents/source}"
        "copied_fr")
       ":publication/id :open-hax.publications/copied-fr")))

(deftest ^:async saved-document-and-publication-trigger-exact-admission
  (let [calls (atom [])
        config {:session-project-name "knoxx-session"}
        ctx {:orgId "org-1"
             :membershipId "membership-1"
             :permissions ["org.translations.manage"
                           "org.publications.manage"]}
        admit! (fn [scope selection]
                 (swap! calls conj [scope selection])
                 (js/Promise.resolve {:ok true :admitted 1 :failed 0}))]
    (await (resource-routes/admit-saved-publication-resource!
            config ctx "documents" {:document/id :knoxx.docs/entered} admit!))
    (await (resource-routes/admit-saved-publication-resource!
            config ctx "publications"
            {:publication/document :knoxx.docs/entered} admit!))
    (await (resource-routes/admit-saved-publication-resource!
            config ctx "agents" {:contract/id "unrelated"} admit!))
    (is (= [[{:org-id "org-1"
              :membership-id "membership-1"
              :project "knoxx-session"}
             {:document :knoxx.docs/entered}]
            [{:org-id "org-1"
              :membership-id "membership-1"
              :project "knoxx-session"}
             {:document :knoxx.docs/entered}]]
           @calls))))

(deftest ^:async compatibility-put-refuses-document-resource-mutation
  (let [root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-resource-put-")))
        response (atom nil)
        calls (atom [])
        config {:contracts-dir root :session-project-name "knoxx-session"}
        ctx {:orgId "org-1"
             :membershipId "membership-1"
             :permissions ["agent.chat.use"]}
        edn-text (str "{:document/id :knoxx.docs/entered\n"
                      " :document/title \"Entered\"\n"
                      " :document/source-locale :en\n"
                      " :document/source {:path \"source.md\"}\n"
                      " :document/anchor? true}")]
    (try
      (await
       (resource-routes/handle-agent-put-contract-edn
        (fn [status body] (reset! response [status body]))
        config "documents" "entered" edn-text ctx
        (fn [scope selection]
          (swap! calls conj [scope selection])
          (js/Promise.resolve {:ok true :admitted 1 :failed 0}))))
      (is (= 400 (first @response)))
      (is (str/includes? (second @response)
                         "compatibility_contract_class_not_writable"))
      (is (empty? @calls))
      (try
        (await (.readFile fs (.join path root "documents" "entered.edn")
                          "utf8"))
        (is false "rejected compatibility mutation must not write a document")
        (catch :default err
          (is (= "ENOENT" (.-code err)))))
      (finally
        (await (.rm fs root #js {:recursive true :force true}))))))

(deftest ^:async compatibility-put-still-writes-agent-resources
  (let [root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-agent-put-")))
        response (atom nil)
        edn-text (str "{:contract/id \"entered\"\n"
                      " :contract/kind :agent\n"
                      " :enabled true\n"
                      " :trigger-kind :manual\n"
                      " :agent {:role :role/developer :model \"test-model\"}\n"
                      " :prompts {:task \"Test compatibility mutation.\"}}")]
    (try
      (await
       (resource-routes/handle-agent-put-contract-edn
        (fn [status body] (reset! response [status body]))
        {:contracts-dir root} "agents" "entered" edn-text))
      (is (= 200 (first @response)))
      (is (= edn-text
             (await (.readFile fs (.join path root "agents" "entered.edn")
                               "utf8"))))
      (finally
        (await (.rm fs root #js {:recursive true :force true}))))))

(deftest automatic-admission-refuses-unattributed-resource-writes
  (try
    (resource-routes/admission-scope
     {:session-project-name "knoxx-session"}
     {:orgId "org-1"})
    (is false "missing membership must fail closed")
    (catch :default err
      (is (= 403 (:status (ex-data err))))
      (is (= "document_admission_context_required"
             (:code (ex-data err)))))))

(deftest ^:async automatic-admission-rejects-a-resolved-failure-result
  (try
    (await
     (resource-routes/admit-saved-publication-resource!
      {:session-project-name "knoxx-session"}
      {:orgId "org-1"
       :membershipId "membership-1"
       :permissions ["org.translations.manage"
                     "org.publications.manage"]}
      "documents"
      {:document/id :knoxx.docs/entered}
      (fn [_scope _selection]
        (js/Promise.resolve {:ok false :admitted 1 :failed 1}))))
    (is false "a resolved failed admission must still fail the resource write")
    (catch :default err
      (is (= 503 (:status (ex-data err))))
      (is (= "document_admission_failed" (:code (ex-data err))))
      (is (= :knoxx.docs/entered (:document/id (ex-data err)))))))

(deftest ^:async automatic-admission-requires-both-publication-permissions
  (doseq [[permissions missing]
          [[[] "org.translations.manage"]
           [["org.translations.manage"] "org.publications.manage"]]]
    (let [calls (atom [])
          err (try
                (await
                 (resource-routes/admit-saved-publication-resource!
                  {:session-project-name "knoxx-session"}
                  {:orgId "org-1"
                   :membershipId "membership-1"
                   :permissions permissions}
                  "documents"
                  {:document/id :knoxx.docs/entered}
                  (fn [_scope _selection]
                    (swap! calls conj :admit)
                    (js/Promise.resolve {:ok true :admitted 1 :failed 0}))))
                nil
                (catch :default error error))]
      (is (= 403 (:status (ex-data err))))
      (is (= "permission_denied" (:code (ex-data err))))
      (is (str/includes? (ex-message err) missing))
      (is (empty? @calls)
          "permission failure must happen before the admission hook"))))

(defn- missing-file-rejection
  []
  (let [err (js/Error. "missing")]
    (aset err "code" "ENOENT")
    (js/Promise.reject err)))

(defn- deferred
  []
  (let [resolve* (atom nil)
        reject* (atom nil)
        promise (js/Promise.
                 (fn [resolve reject]
                   (reset! resolve* resolve)
                   (reset! reject* reject)))]
    {:promise promise
     :resolve! (fn [value] (@resolve* value))
     :reject! (fn [error] (@reject* error))}))

(defn- ^:async flush-promises!
  []
  (dotimes [_ 8]
    (await (js/Promise.resolve nil))))

(deftest ^:async entered-resource-preserves-new-bytes-when-admission-fails
  (let [calls (atom [])
        failure (ex-info "index admission rejected"
                         {:status 503 :code "document_admission_failed"})]
    (try
      (await
       (resource-routes/write-resource-and-admit!
        fixture-config "/resources/doc.edn" "new-edn"
        (fn []
          (swap! calls conj [:admit])
          (js/Promise.reject failure))
        {:read-file! (fn [path]
                       (swap! calls conj [:read path])
                       (js/Promise.resolve "old-edn"))
         :write-file! (fn [path text]
                        (swap! calls conj [:write path text])
                        (js/Promise.resolve nil))
         :delete-file! (fn [path]
                         (swap! calls conj [:delete path])
                         (js/Promise.resolve nil))
         :sync-index! (fn [_config]
                        (swap! calls conj [:sync])
                        (js/Promise.resolve {:ok true}))}))
      (is false "admission rejection must propagate")
      (catch :default err
        (is (identical? failure err))))
    (is (= [[:read "/resources/doc.edn"]
            [:write "/resources/doc.edn" "new-edn"]
            [:sync]
            [:admit]]
           @calls)
        "admission may have durable effects, so its source bytes must remain")))

(deftest ^:async entered-resource-preserves-new-file-when-admission-fails
  (let [calls (atom [])]
    (try
      (await
       (resource-routes/write-resource-and-admit!
        fixture-config "/resources/new-doc.edn" "new-edn"
        (fn [] (js/Promise.reject (js/Error. "index admission rejected")))
        {:read-file! (fn [path]
                       (swap! calls conj [:read path])
                       (missing-file-rejection))
         :write-file! (fn [path text]
                        (swap! calls conj [:write path text])
                        (js/Promise.resolve nil))
         :delete-file! (fn [path]
                         (swap! calls conj [:delete path])
                         (js/Promise.resolve nil))
         :sync-index! (fn [_config]
                        (swap! calls conj [:sync])
                        (js/Promise.resolve {:ok true}))}))
      (is false "admission rejection must propagate")
      (catch :default err
        (is (= "index admission rejected" (.-message err)))))
    (is (= [[:read "/resources/new-doc.edn"]
            [:write "/resources/new-doc.edn" "new-edn"]
            [:sync]]
           @calls)
        "a newly entered source remains available for retry/reconciliation")))

(deftest ^:async entered-resource-returns-admission-without-rollback-on-success
  (let [calls (atom [])
        admission {:ok true :admitted 1 :failed 0}
        result
        (await
         (resource-routes/write-resource-and-admit!
          fixture-config "/resources/doc.edn" "new-edn"
          (fn []
            (swap! calls conj [:admit])
            (js/Promise.resolve admission))
          {:read-file! (fn [path]
                         (swap! calls conj [:read path])
                         (js/Promise.resolve "old-edn"))
           :write-file! (fn [path text]
                          (swap! calls conj [:write path text])
                          (js/Promise.resolve nil))
           :delete-file! (fn [path]
                           (swap! calls conj [:delete path])
                           (js/Promise.resolve nil))
           :sync-index! (fn [_config]
                          (swap! calls conj [:sync])
                          (js/Promise.resolve {:ok true}))}))]
    (is (= admission result))
    (is (= [[:read "/resources/doc.edn"]
            [:write "/resources/doc.edn" "new-edn"]
            [:sync]
            [:admit]]
           @calls))))

(deftest ^:async entered-resource-treats-a-resolved-index-failure-as-a-write-failure
  (let [calls (atom [])
        sync-results (atom [{:ok false :error "loader rejected the new file"}
                            {:ok true}])]
    (try
      (await
       (resource-routes/write-resource-and-admit!
        fixture-config "/resources/doc.edn" "new-edn"
        (fn []
          (swap! calls conj [:admit])
          (js/Promise.resolve {:ok true :admitted 1 :failed 0}))
        {:read-file! (fn [path]
                       (swap! calls conj [:read path])
                       (js/Promise.resolve "old-edn"))
         :write-file! (fn [path text]
                        (swap! calls conj [:write path text])
                        (js/Promise.resolve nil))
         :delete-file! (fn [path]
                         (swap! calls conj [:delete path])
                         (js/Promise.resolve nil))
         :sync-index! (fn [_config]
                        (swap! calls conj [:sync])
                        (let [result (first @sync-results)]
                          (swap! sync-results subvec 1)
                          (js/Promise.resolve result)))}))
      (is false "a resolved {:ok false} sync must reject the write")
      (catch :default err
        (is (= "resource_index_sync_failed" (:code (ex-data err))))
        (is (= :forward (:resource/index-sync-phase (ex-data err))))))
    (is (= [[:read "/resources/doc.edn"]
            [:write "/resources/doc.edn" "new-edn"]
            [:sync]
            [:write "/resources/doc.edn" "old-edn"]
            [:sync]]
           @calls)
        "the old bytes and index are restored before the sync error escapes")))

(deftest ^:async concurrent-put-admission-failure-preserves-ordered-source-history
  (let [file-text (atom "original-edn")
        calls (atom [])
        first-admission (deferred)
        failure (ex-info "first admission rejected"
                         {:status 503 :code "document_admission_failed"})
        io-deps
        {:read-file! (fn [_path]
                       (swap! calls conj [:read @file-text])
                       (js/Promise.resolve @file-text))
         :write-file! (fn [_path text]
                        (reset! file-text text)
                        (swap! calls conj [:write text])
                        (js/Promise.resolve nil))
         :delete-file! (fn [_path]
                         (reset! file-text nil)
                         (swap! calls conj [:delete])
                         (js/Promise.resolve nil))
         :sync-index! (fn [_config]
                        (swap! calls conj [:sync])
                        (js/Promise.resolve {:ok true}))}
        rejected-write
        (resource-routes/write-resource-and-admit!
         fixture-config "/resources/shared.edn" "rejected-edn"
         (fn []
           (swap! calls conj [:admit "rejected-edn"])
           (:promise first-admission))
         io-deps)
        successful-write
        (resource-routes/write-resource-and-admit!
         fixture-config "/resources/shared.edn" "successful-edn"
         (fn []
           (swap! calls conj [:admit "successful-edn"])
           (js/Promise.resolve {:ok true :admitted 1 :failed 0}))
         io-deps)]
    (await (flush-promises!))
    (testing "the newer PUT cannot snapshot or write while the older PUT is admitting"
      (is (= "rejected-edn" @file-text))
      (is (= [[:read "original-edn"]
              [:write "rejected-edn"]
              [:sync]
              [:admit "rejected-edn"]]
             @calls)))
    ((:reject! first-admission) failure)
    (let [rejected-error (try
                           (await rejected-write)
                           nil
                           (catch :default err err))
          successful-result (await successful-write)]
      (is (identical? failure rejected-error))
      (is (= {:ok true :admitted 1 :failed 0} successful-result))
      (is (= "successful-edn" @file-text)
          "the later successful PUT remains authoritative")
      (is (= [[:read "original-edn"]
              [:write "rejected-edn"]
              [:sync]
              [:admit "rejected-edn"]
              [:read "rejected-edn"]
              [:write "successful-edn"]
              [:sync]
              [:admit "successful-edn"]]
             @calls)
          "the later PUT snapshots the exact bytes retained after admission failure"))))
