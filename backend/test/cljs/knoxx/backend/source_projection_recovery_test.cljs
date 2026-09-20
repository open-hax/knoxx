(ns knoxx.backend.source-projection-recovery-test
  "Real filesystem recovery preserves durable bytes and current resource authority."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :as test]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-authoring-store :as store]
            [knoxx.backend.infra.source-review :as review]))

(def ^:private scope {:org-id "org-a" :project "wiki" :document :docs/source})
(def ^:private actor {:id "human-a" :kind :human})
(def ^:private document
  {:document/id :docs/source :document/title "Existing source"
   :document/source-locale :en :document/org-id "org-a"
   :document/visibility :private :document/source {:path "source.md"}})

(defn- ^:async attempt [operation]
  (try {:result (await (operation))}
       (catch :default error {:error (ex-data error)})))

(defn- ^:async declare! [contracts resource]
  (await (files/write-text! contracts "namespaces/source.edn"
                           (files/manifest-text {:namespace :docs :resources [resource]}))))

(defn- ^:async fixture! [operation]
  (let [root (disk/temp-directory!) contracts (str root "/contracts")
        directory (str root "/source-ledger")
        config {:contracts-dir contracts}
        dependencies {:source-provider (sources/open! {:directory directory})
                      :provider (reviews/open! {:directory (str root "/review-ledger")})
                      :now! (constantly "2026-09-20T00:00:00Z")}]
    (try
      (fs/ensure-dir! contracts)
      (await (declare! contracts document))
      (await (files/write-text! root "source.md" "Original source."))
      (await (operation {:root root :contracts contracts :directory directory
                         :config config :dependencies dependencies}))
      (finally (fs/remove-tree! root)))))

(defn- ^:async save! [{:keys [config dependencies]} id content]
  (let [current (await (review/read! config scope dependencies))
        command {:operation-id id :expected-revision (:revision current) :content content}]
    {:command command :result (await (authoring/save! config scope actor command dependencies))}))

(test/deftest ^:async caller-save-id-cannot-collide-with-synthetic-observation
  (await (fixture!
    (^:async fn [{:keys [directory config dependencies]}]
      (let [current (await (review/read! config scope dependencies))
            command {:operation-id (str "observe/" (:revision current))
                     :expected-revision (:revision current) :content "Accepted collision-safe save."}
            outcome (await (attempt #(authoring/save! config scope actor command dependencies)))
            reopened (assoc dependencies :source-provider (sources/open! {:directory directory}))]
        (test/is (nil? (:error outcome)))
        (test/is (= :save (get-in outcome [:result :event :source/action])))
        (test/is (= "Accepted collision-safe save." (get-in outcome [:result :review :content])))
        (let [retry (await (attempt #(authoring/save! config scope actor command reopened)))
              history (await (store/source-events! (:source-provider reopened) scope))]
          (test/is (nil? (:error retry)))
          (test/is (true? (get-in retry [:result :existing?])))
          (test/is (= (get-in outcome [:result :event]) (get-in retry [:result :event])))
          (test/is (= [:observe :save] (mapv :source/action history)))
          (test/is (= [(:operation-id command) (:operation-id command)] (mapv :source/id history)))))))))

(test/deftest ^:async exact-save-retry-restores-latest-bytes-after-projection-loss-and-reopen
  (await (fixture!
    (^:async fn [{:keys [root directory config dependencies] :as fixture}]
      (let [first-save (await (save! fixture "first" "First accepted bytes."))
            latest-save (await (save! fixture "latest" "Latest accepted bytes."))
            reopened (assoc dependencies :source-provider (sources/open! {:directory directory}))
            history (await (store/source-events! (:source-provider reopened) scope))]
        (fs/delete-if-exists! (str root "/source.md"))
        (let [outcome (await (attempt #(authoring/save! config scope actor (:command first-save) reopened)))]
          (test/is (nil? (:error outcome)))
          (test/is (true? (get-in outcome [:result :existing?])))
          (test/is (= (get-in first-save [:result :event]) (get-in outcome [:result :event])))
          (test/is (= (get-in latest-save [:result :review]) (get-in outcome [:result :review])))
          (test/is (= "Latest accepted bytes." (await (files/read-text! (str root "/source.md")))))
          (test/is (= history (await (store/source-events! (:source-provider reopened) scope))))))))))

(test/deftest ^:async missing-projection-never-admits-a-new-save-or-conflicting-retry
  (await (fixture!
    (^:async fn [{:keys [root config dependencies] :as fixture}]
      (let [{:keys [command]} (await (save! fixture "accepted" "Accepted bytes."))
            history (await (store/source-events! (:source-provider dependencies) scope))]
        (fs/delete-if-exists! (str root "/source.md"))
        (doseq [[attempt-actor attempt-command expected]
                [[actor (assoc command :operation-id "new") "source_content_not_found"]
                 [actor (assoc command :content "Changed retry.") "source_authoring_operation_conflict"]
                 [(assoc actor :id "another-human") command "source_authoring_operation_conflict"]]]
          (test/is (= expected
                      (get-in (await (attempt #(authoring/save! config scope attempt-actor attempt-command dependencies)))
                              [:error :code])))
          (test/is (not (fs/exists? (str root "/source.md")))))
        (test/is (= history (await (store/source-events! (:source-provider dependencies) scope)))))))))

(test/deftest ^:async retry-preserves-conflicting-existing-source-bytes
  (await (fixture!
    (^:async fn [{:keys [root config dependencies] :as fixture}]
      (let [{:keys [command]} (await (save! fixture "accepted" "Accepted bytes."))]
        (await (files/write-text! root "source.md" "Independent edit; preserve me."))
        (test/is (= "source_projection_conflict"
                    (get-in (await (attempt #(authoring/save! config scope actor command dependencies))) [:error :code])))
        (test/is (= "Independent edit; preserve me." (await (files/read-text! (str root "/source.md"))))))))))

(test/deftest ^:async retry-validates-current-declaration-before-reading-source-history
  (await (fixture!
    (^:async fn [{:keys [root contracts config dependencies] :as fixture}]
      (let [{:keys [command]} (await (save! fixture "accepted" "Accepted bytes."))
            reads (atom 0)]
        (fs/delete-if-exists! (str root "/source.md"))
        (with-redefs [store/source-events! (fn [_ _] (swap! reads inc) [])]
          (doseq [resource [nil (assoc document :document/org-id "org-b")
                           (assoc document :document/org-id "org-b" :document/visibility :public)]]
            (if resource (await (declare! contracts resource))
                (fs/delete-if-exists! (str contracts "/namespaces/source.edn")))
            (test/is (contains? #{"source_document_not_found" "source_authoring_owner_required"}
                                (get-in (await (attempt #(authoring/save! config scope actor command dependencies)))
                                        [:error :code])))
            (test/is (zero? @reads))
            (test/is (not (fs/exists? (str root "/source.md")))))))))))

(test/deftest ^:async retry-refuses-changed-declared-source-identity
  (await (fixture!
    (^:async fn [{:keys [root contracts config dependencies] :as fixture}]
      (let [{:keys [command]} (await (save! fixture "accepted" "Accepted bytes."))]
        (fs/delete-if-exists! (str root "/source.md"))
        (await (declare! contracts (assoc-in document [:document/source :path] "replacement.md")))
        (test/is (= "source_authoring_document_conflict"
                    (get-in (await (attempt #(authoring/save! config scope actor command dependencies))) [:error :code])))
        (test/is (not (fs/exists? (str root "/source.md"))))
        (test/is (not (fs/exists? (str root "/replacement.md")))))))))

(test/deftest ^:async retry-refuses-source-symlink-escape-before-reading-history
  (await (fixture!
    (^:async fn [{:keys [root config dependencies] :as fixture}]
      (let [{:keys [command]} (await (save! fixture "accepted" "Accepted bytes."))
            outside (disk/temp-directory!)
            reads (atom 0)]
        (try
          (await (files/write-text! outside "outside.md" "Outside bytes."))
          (fs/delete-if-exists! (str root "/source.md"))
          (disk/symlink! (str outside "/outside.md") (str root "/source.md"))
          (with-redefs [store/source-events! (fn [_ _] (swap! reads inc) [])]
            (test/is (= "document_source_outside_provenance_root"
                        (get-in (await (attempt #(authoring/save! config scope actor command dependencies))) [:error :code])))
            (test/is (zero? @reads)))
          (test/is (= "Outside bytes." (await (files/read-text! (str outside "/outside.md")))))
          (finally (fs/remove-tree! outside))))))))
